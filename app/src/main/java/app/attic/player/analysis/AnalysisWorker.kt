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
import app.attic.player.data.LibraryRepository
import app.attic.player.data.SongAnalysisEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Works through unanalysed songs. Progress is saved per song, so being stopped loses nothing. */
class AnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var foreground = false

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        val app = applicationContext.attic
        val dao = app.database.dao()
        val library = app.library.await()
        val done = dao.analyzedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        // Songs you actually play get ready first.
        val recent = dao.recentKeys(RECENT_FIRST).withIndex().associate { it.value to it.index }
        val todo = library.tracks
            .filter { it.key !in done }
            .sortedBy { recent[it.key] ?: Int.MAX_VALUE }
        if (todo.isEmpty()) return@withContext Result.success()

        val total = library.tracks.size
        var finished = total - todo.size
        showProgress(finished, total)
        try {
            SongAnalyzer(applicationContext).use { analyzer ->
                val started = System.currentTimeMillis()
                for (track in todo) {
                    coroutineContext.ensureActive()
                    // A background job gets stopped and retried ever more slowly, so it hands over first.
                    if (!foreground && System.currentTimeMillis() - started > CHUNK_MS) {
                        enqueueNextChunk()
                        return@withContext Result.success()
                    }
                    val songStarted = System.currentTimeMillis()
                    val analysis = analyzer.analyze(LibraryRepository.uriFor(track.mediaId))
                    if (analysis == null) Log.w(TAG, "Couldn't analyse ${track.key}")
                    dao.saveAnalysis(
                        SongAnalysisEntity(
                            trackKey = track.key,
                            modelVersion = SongAnalyzer.MODEL_VERSION,
                            embedding = analysis?.embedding?.let(SongAnalyzer::toBytes),
                            chroma = analysis?.chroma,
                            analyzedAt = System.currentTimeMillis(),
                        ),
                    )
                    Log.i(TAG, "Analysed in ${System.currentTimeMillis() - songStarted} ms: ${track.key}")
                    finished++
                    showProgress(finished, total)
                }
            }
        } finally {
            notifications.cancel(NOTIFICATION_ID)
        }
        // Files added during this run didn't get a job of their own, because this one was already going.
        val analyzed = dao.analyzedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        if (app.library.await().tracks.any { it.key !in analyzed }) enqueueNextChunk()
        Result.success()
    }

    /** A foreground service when Android allows one, which only happens if the app was open to start it. */
    private suspend fun showProgress(done: Int, total: Int) {
        val notification = buildNotification(done, total)
        try {
            setForeground(ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            foreground = true
        } catch (_: IllegalStateException) {
            foreground = false
            notifications.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(done: Int, total: Int): Notification {
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Song analysis", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress while Attic analyses your songs for radio"
            },
        )
        val open = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_analysis)
            .setContentTitle("Analysing your songs")
            .setContentText("$done of $total")
            .setProgress(total, done, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, "Stop", WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
            .build()
    }

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
