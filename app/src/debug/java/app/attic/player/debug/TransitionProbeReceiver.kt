package app.attic.player.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import app.attic.player.analysis.KeyEstimate
import app.attic.player.analysis.PcmDecoder
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.analysis.SongTransitions
import app.attic.player.analysis.transitionsEntity
import app.attic.player.attic
import app.attic.player.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
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
 *
 * adb shell am broadcast -n app.attic.player/.debug.TransitionProbeReceiver -a measure --es key "Music/…/song.m4a"
 */
class TransitionProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
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
