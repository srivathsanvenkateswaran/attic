package app.attic.player.analysis

import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.system.Os
import android.util.Log
import app.attic.player.data.MusicDao
import app.attic.player.data.SongMoodsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Gives every analysed song its mood scores from the stored embedding, with no decode. Safe to call
 * often: it reads nothing but one query when every song is already scored. Songs come back a batch at
 * a time and each batch is written as it finishes, so being stopped loses at most one batch.
 */
class MoodScorer(private val context: Context, private val dao: MusicDao) {
    private val lock = Mutex()

    /** Scores what is missing or stale and returns how many songs it scored. */
    suspend fun scoreMissing(): Int = lock.withLock {
        withContext(Dispatchers.Default) {
            var heads: MoodHeads? = null
            var scored = 0
            val started = System.nanoTime()
            try {
                while (true) {
                    coroutineContext.ensureActive()
                    val rows = dao.moodInputs(SongAnalyzer.MODEL_VERSION, VERSION, BATCH)
                    if (rows.isEmpty()) break
                    val loaded = heads ?: load().also { heads = it }
                    val norms = rows.map { it.embeddingNorm }
                    val signals = loaded.score(rows.map { SongAnalyzer.toFloats(it.embedding) }, norms)
                    dao.saveMoods(
                        rows.mapIndexed { i, row ->
                            SongMoodsEntity(row.trackKey, VERSION, exact = (norms[i] ?: 0f) > 0f, scores = MoodSignals.encode(signals[i]))
                        },
                    )
                    scored += rows.size
                    // A full batch may be followed by more; a short one was the last.
                    if (rows.size < BATCH) break
                }
            } finally {
                heads?.close()
            }
            if (scored > 0) Log.i(TAG, "Scored moods for $scored songs in ${(System.nanoTime() - started) / 1_000_000} ms")
            scored
        }
    }

    private fun load(): MoodHeads {
        // As SongAnalyzer does: ONNX Runtime's usage events stay off before the runtime first loads.
        Os.setenv("ORT_DISABLE_TELEMETRY", "1", true)
        return MoodHeads(OrtEnvironment.getEnvironment()) { path -> context.assets.open(path).use { it.readBytes() } }
    }

    companion object {
        private const val TAG = "MoodScorer"
        private const val BATCH = 128

        /** Raise this if the heads, their input or [MoodSignals]' order change, so every song is scored again. */
        const val VERSION = 1
    }
}
