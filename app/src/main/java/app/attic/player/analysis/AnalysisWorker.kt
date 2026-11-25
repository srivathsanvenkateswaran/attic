package app.attic.player.analysis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.attic.player.MainActivity
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortDark
import app.attic.player.ui.comfort.ComfortLight
import androidx.compose.ui.graphics.toArgb
import java.text.NumberFormat
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import app.attic.player.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Works through songs missing their loudness, unanalysed songs, songs missing only their transitions,
 * then songs analysed before their styles were stored ([AnalysisPlan]). Progress is saved per song, so being stopped loses nothing.
 */
class AnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var foreground = false
    private var shownDone = 0
    private var shownTotal = 0

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        val app = applicationContext.attic
        val dao = app.database.dao()
        val library = app.library.await()
        val finished = dao.finishedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        val analyzed = dao.analyzedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        val measured = dao.measuredKeysNow(LOUDNESS_VERSION).toSet()
        val transitioned = dao.transitionedKeysNow(TRANSITIONS_VERSION).toSet()
        val recent = dao.recentKeys(RECENT_FIRST).withIndex().associate { it.value to it.index }
        val todo = AnalysisPlan.todo(library.tracks.map { it.key }, finished, analyzed, measured, transitioned, recent)
            .mapNotNull { (key, need) -> library.trackByKey[key]?.let { it to need } }
        if (todo.isEmpty()) return@withContext Result.success()

        val total = library.tracks.size
        var done = total - todo.size
        showProgress(done, total)
        // A style change in Settings redraws the notification at once rather than at the next song.
        val restyle = launch {
            app.settings.uiStyle.drop(1).collect { notifications.notify(NOTIFICATION_ID, buildNotification(shownDone, shownTotal)) }
        }
        try {
            // The model loads only if a song needs it; songs missing only their loudness just get decoded.
            val analyzer = lazy { SongAnalyzer(applicationContext) }
            try {
                val started = System.currentTimeMillis()
                var sinceMoods = 0
                for ((track, need) in todo) {
                    coroutineContext.ensureActive()
                    // A background job gets stopped and retried ever more slowly, so it hands over first.
                    if (!foreground && System.currentTimeMillis() - started > CHUNK_MS) {
                        enqueueNextChunk()
                        return@withContext Result.success()
                    }
                    val songStarted = System.currentTimeMillis()
                    val uri = LibraryRepository.uriFor(track.mediaId)
                    when (need) {
                        AnalysisNeed.Full -> {
                            val analysis = analyzer.value.analyze(uri)
                            if (analysis.sound == null) Log.w(TAG, "Couldn't analyse ${track.key}")
                            dao.saveAnalysis(track.key, analysis, hadEmbedding = track.key in analyzed)
                            Log.i(
                                TAG,
                                "Analysed in ${System.currentTimeMillis() - songStarted} ms " +
                                    "(transitions ${analysis.transitionNanos / 1_000_000} ms): ${track.key}",
                            )
                        }
                        AnalysisNeed.Measure -> {
                            val measured = PcmDecoder.measure(applicationContext, uri)
                            dao.saveMeasured(track.key, measured)
                            Log.i(
                                TAG,
                                "Measured in ${System.currentTimeMillis() - songStarted} ms " +
                                    "(transitions ${(measured?.transitionNanos ?: 0) / 1_000_000} ms): ${track.key}",
                            )
                        }
                    }
                    done++
                    showProgress(done, total)
                    // Mood scores come from the saved embeddings, a batch at a time, so mixes can use new songs soon.
                    if (++sinceMoods >= MOOD_EVERY) {
                        app.moodScorer.scoreMissing()
                        sinceMoods = 0
                    }
                }
            } finally {
                if (analyzer.isInitialized()) analyzer.value.close()
            }
        } finally {
            restyle.cancel()
            notifications.cancel(NOTIFICATION_ID)
        }
        app.moodScorer.scoreMissing()
        // Files added during this run didn't get a job of their own, because this one was already going.
        val finishedNow = dao.finishedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        val measuredNow = dao.measuredKeysNow(LOUDNESS_VERSION).toSet()
        val transitionedNow = dao.transitionedKeysNow(TRANSITIONS_VERSION).toSet()
        if (app.library.await().tracks.any { AnalysisPlan.need(it.key, finishedNow, measuredNow, transitionedNow) != null }) enqueueNextChunk()
        Result.success()
    }

    /**
     * A foreground service only for a run you started with "Analyse now", and only while Android allows one,
     * which needs the app open. Play's dataSync declaration ("Local processing") covers user-started work
     * only, so a run that waited for the charger stays an ordinary job with an ordinary notification.
     */
    private suspend fun showProgress(done: Int, total: Int) {
        shownDone = done
        shownTotal = total
        val notification = buildNotification(done, total)
        foreground = withoutCharger && try {
            setForeground(ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            true
        } catch (_: IllegalStateException) {
            false
        }
        if (!foreground) notifications.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(done: Int, total: Int): Notification =
        progressNotification(applicationContext, done, total, WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))

    private fun enqueueNextChunk() {
        val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_SECONDS, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiresCharging(!withoutCharger).build())
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private val withoutCharger: Boolean get() = inputData.getBoolean(KEY_WITHOUT_CHARGER, false)

    companion object {
        private const val TAG = "AnalysisWorker"
        private const val KEY_WITHOUT_CHARGER = "withoutCharger"
        private const val WORK_NAME = "song-analysis"
        private const val CHANNEL_ID = "song-analysis"
        private const val NOTIFICATION_ID = 2001
        private const val RECENT_FIRST = 200
        private const val CHUNK_MS = 8 * 60 * 1000L
        private const val MOOD_EVERY = 50

        /**
         * Android's standard progress notification, as both 10-system sheets draw frame C. The style
         * picks the glyph (graphic_eq at each kit's weight) and, for Comfort only, the accent: its amber
         * fills the icon's circle. Expressive leaves the colour to the system, which takes it from the
         * wallpaper. [stop] is the "Stop" action.
         */
        fun progressNotification(context: Context, done: Int, total: Int, stop: PendingIntent): Notification {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.analysis_channel), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.analysis_channel_description)
                },
            )
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val comfort = context.attic.settings.uiStyle.value == UiStyle.Comfort
            val number = NumberFormat.getIntegerInstance()
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (comfort) R.drawable.ic_comfort_analysis else R.drawable.ic_expressive_analysis)
                .apply { if (comfort) setColor(comfortAccent(context)) }
                .setContentTitle(context.getString(R.string.analysis_notification_title))
                .setContentText(context.getString(R.string.analysis_notification_progress, number.format(done), number.format(total)))
                .setProgress(total, done, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(open)
                .addAction(0, context.getString(R.string.analysis_notification_stop), stop)
                .build()
        }

        /** Comfort's Lamplight, in the phone's current theme: oneui/kit.css `--accent`. */
        private fun comfortAccent(context: Context): Int =
            (if (context.resources.configuration.isNightModeActive) ComfortDark else ComfortLight).accent.toArgb()

        /** Being interrupted is normal here, so the retry stays short instead of growing to hours. */
        private const val RETRY_SECONDS = 30L

        /** Whether analysis is working right now, rather than waiting for a charger. */
        fun running(context: Context): Flow<Boolean> =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

        /** Leaves running work alone, so calling this on every library change is cheap. */
        fun scheduleWhileCharging(context: Context) {
            val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_SECONDS, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        /** Stops a run and drops the queued one, as the notification's Stop does. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Asked for from Settings, so it doesn't wait for a charger. */
        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_WITHOUT_CHARGER to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
