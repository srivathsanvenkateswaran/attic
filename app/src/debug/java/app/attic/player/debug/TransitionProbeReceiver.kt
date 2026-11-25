package app.attic.player.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.analysis.KeyEstimate
import app.attic.player.analysis.LoudnessMeter
import app.attic.player.analysis.TRANSITIONS_VERSION
import app.attic.player.analysis.PcmDecoder
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.analysis.SongTransitions
import app.attic.player.analysis.transitionsEntity
import app.attic.player.attic
import app.attic.player.data.LibraryRepository
import app.attic.player.playback.AlbumRun
import app.attic.player.playback.LevelTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug builds only: measures one song's transition points on its own, so a phone test can try smart
 * transitions on a song or two without an analysis run, and puts things back afterwards.
 *
 * - "measure" --es key <track key>: a decode with no model run; saves only the song's transitions row
 *   (not its loudness) and logs what it found and what it cost, wall clock and thread CPU.
 * - "forget" --es key <track key>: deletes that row again.
 * - "cost" --es key <track key>: a full analysis of the song (model included), saving nothing, and logs
 *   how long it took and the transition meter's share.
 * - "loudness-only" --es key <track key>: the decode the backfill did before transitions existed (loudness
 *   alone), saving nothing, timed like "measure", to price the new backfill against the old one.
 * - "queue-report": logs the saved queue's next songs, which have transitions measured, and every album
 *   run in it.
 * - "hold-analysis": drops the analysis run the app queues while charging, until the app next starts, so
 *   a test on a phone that is charging doesn't analyse the library.
 *
 * adb shell am broadcast -n app.attic.player/.debug.TransitionProbeReceiver -a measure --es key "Music/…/song.m4a"
 */
class TransitionProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (intent.action == "hold-analysis") {
            // The library's first load queues a run while charging; this drops it once that has happened.
            val pending = goAsync()
            app.attic.scope.launch {
                try {
                    AnalysisWorker.cancel(app)
                    app.attic.library.await()
                    delay(3_000)
                    AnalysisWorker.cancel(app)
                    Log.i(TAG, "Analysis held until the app next starts")
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action == "queue-report") {
            val pending = goAsync()
            app.attic.scope.launch {
                try {
                    withContext(Dispatchers.Default) { queueReport(app) }
                } finally {
                    pending.finish()
                }
            }
            return
        }
        val key = intent.getStringExtra("key") ?: return
        val pending = goAsync()
        app.attic.scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    val dao = app.attic.database.dao()
                    val track = app.attic.library.await().trackByKey[key]
                    if (track == null) {
                        Log.w(TAG, "No song with key $key")
                        return@withContext
                    }
                    val uri = LibraryRepository.uriFor(track.mediaId)
                    when (intent.action) {
                        "forget" -> {
                            dao.forgetTransitions(key)
                            Log.i(TAG, "Forgot transitions: $key")
                        }
                        "measure" -> {
                            val wall = SystemClock.elapsedRealtime()
                            val cpu = SystemClock.currentThreadTimeMillis()
                            val measured = PcmDecoder.measure(app, uri)
                            val cpuMs = SystemClock.currentThreadTimeMillis() - cpu
                            val wallMs = SystemClock.elapsedRealtime() - wall
                            val songKey = KeyEstimate.of(dao.chroma(key))
                            dao.saveTransitions(transitionsEntity(key, measured?.transitions, songKey, System.currentTimeMillis()))
                            Log.i(
                                TAG,
                                "Measured in $wallMs ms wall, $cpuMs ms CPU, meter ${(measured?.transitionNanos ?: 0) / 1_000_000} ms " +
                                    "(song ${track.durationMs / 1000} s): ${describe(measured?.transitions)}, key ${songKey?.key} " +
                                    "conf ${songKey?.confidence}: $key",
                            )
                        }
                        "loudness-only" -> {
                            // What the backfill decode did before transitions existed: loudness alone.
                            val wall = SystemClock.elapsedRealtime()
                            val cpu = SystemClock.currentThreadTimeMillis()
                            var meter: LoudnessMeter? = null
                            PcmDecoder.decode(
                                app,
                                uri,
                                onFormat = { rate, channels -> meter = LoudnessMeter(rate, channels) },
                                onSamples = { samples ->
                                    meter?.add(samples)
                                    true
                                },
                            )
                            meter?.result()
                            Log.i(
                                TAG,
                                "Loudness-only decode, not saved: ${SystemClock.elapsedRealtime() - wall} ms wall, " +
                                    "${SystemClock.currentThreadTimeMillis() - cpu} ms CPU (song ${track.durationMs / 1000} s): $key",
                            )
                        }
                        "cost" -> {
                            val wall = SystemClock.elapsedRealtime()
                            val cpu = SystemClock.currentThreadTimeMillis()
                            val analysis = SongAnalyzer(app).use { it.analyze(uri) }
                            Log.i(
                                TAG,
                                "Full analysis, not saved: ${SystemClock.elapsedRealtime() - wall} ms wall, " +
                                    "${SystemClock.currentThreadTimeMillis() - cpu} ms CPU on this thread (the model runs on two more), " +
                                    "meter ${analysis.transitionNanos / 1_000_000} ms (song ${track.durationMs / 1000} s): $key",
                            )
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * The saved queue as the planner sees it: each song with whether its transitions are measured, and
     * each pair that is an album run (stays gapless). Logs the current song and the next 60.
     */
    private suspend fun queueReport(app: Context) {
        val dao = app.attic.database.dao()
        val queue = dao.savedQueue() ?: return
        val library = app.attic.library.await()
        val measured = dao.transitionedKeysNow(TRANSITIONS_VERSION).toSet()
        val keys = queue.queueKeys.split('\n').filter { it.isNotEmpty() }
        val tracks = keys.map { library.trackByKey[it] }
        Log.i(TAG, "Queue: ${keys.size} songs, at ${queue.index}, ${measured.size} songs measured in the library")
        for (i in queue.index until minOf(keys.size, queue.index + 61)) {
            val t = tracks[i]
            val level = t?.let { LevelTrack(it.key, it.albumKey, it.trackNumber) }
            val next = tracks.getOrNull(i + 1)?.let { LevelTrack(it.key, it.albumKey, it.trackNumber) }
            val run = if (AlbumRun.follows(level, next)) " ALBUM RUN into the next" else ""
            Log.i(TAG, "Queue $i: ${t?.title} (${t?.album} #${t?.trackNumber})${if (keys[i] in measured) " measured" else ""}$run")
        }
        // Album runs anywhere in the queue, for a gapless check.
        val runs = (0 until keys.size - 1).filter { i ->
            val a = tracks[i] ?: return@filter false
            val b = tracks[i + 1] ?: return@filter false
            AlbumRun.follows(LevelTrack(a.key, a.albumKey, a.trackNumber), LevelTrack(b.key, b.albumKey, b.trackNumber))
        }
        Log.i(TAG, "Album runs in the queue at: ${runs.joinToString()}")
    }

    private fun describe(t: SongTransitions?): String = if (t == null) {
        "nothing measured"
    } else {
        "decoded ${t.decodedMs} ms, audio ${t.audioStartMs}–${t.audioEndMs} ms, outro at ${t.outroStartMs} ms, " +
            "intro ${t.intro?.let { "%.2f BPM conf %.2f".format(it.bpm, it.confidence) } ?: "no beat"}, " +
            "outro ${t.outro?.let { "%.2f BPM conf %.2f".format(it.bpm, it.confidence) } ?: "no beat"}, energy %.2f".format(t.energy)
    }

    private companion object {
        const val TAG = "AtticTransition"
    }
}
