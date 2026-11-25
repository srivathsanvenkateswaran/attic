package app.attic.player.analysis

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable

/**
 * Essentia's classifier heads for Discogs-EffNet, run on stored embeddings. Each head is a small
 * network from a 1,280-number embedding to a few scores; the files are bundled unmodified under
 * assets/[DIR]. No Android here, so the JVM tests run the real heads.
 *
 * The heads were trained on the raw embeddings of single patches. Attic stores a song's mean
 * embedding at unit length, so [score] multiplies it back by its length when that is known. Without
 * the length, only the single heads that rank songs well at a fixed scale ([APPROXIMATE_SCALE]) run;
 * the moodtheme tags and engagement are left missing, because at a fixed scale they come out
 * unrelated to what the heads say about the real audio (docs/features/mood-mixes.md, "Input scale").
 */
class MoodHeads(
    private val environment: OrtEnvironment,
    read: (String) -> ByteArray,
) : Closeable {
    /** One head: which of its outputs go to which [MoodSignals] index. */
    class Head(val file: String, val outputs: List<Pair<Int, Int>>, val needsLength: Boolean)

    private val sessions: List<Pair<Head, OrtSession>> = HEADS.map { head ->
        val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(1) }
        head to environment.createSession(read("$DIR/${head.file}"), options)
    }

    /**
     * Scores for each of [embeddings] (unit length), [norms] giving each one's length, or null or 0
     * when unknown. Returns [MoodSignals.SIZE] values per song, NaN where missing.
     */
    fun score(embeddings: List<FloatArray>, norms: List<Float?>): List<FloatArray> {
        require(embeddings.size == norms.size)
        val out = List(embeddings.size) { FloatArray(MoodSignals.SIZE) { Float.NaN } }
        if (embeddings.isEmpty()) return out
        val exact = norms.map { it != null && it > 0f }
        val scaled = embeddings.mapIndexed { i, vector ->
            val scale = if (exact[i]) norms[i]!! else APPROXIMATE_SCALE
            FloatArray(vector.size) { vector[it] * scale }
        }
        val known = scaled.indices.filter { exact[it] }
        for ((head, session) in sessions) {
            val rows = if (head.needsLength) known else scaled.indices.toList()
            if (rows.isEmpty()) continue
            val results = run(session, rows.map(scaled::get))
            rows.forEachIndexed { r, song ->
                for ((column, signal) in head.outputs) out[song][signal] = results[r][column].coerceIn(0f, 1f)
            }
        }
        return out
    }

    private fun run(session: OrtSession, batch: List<FloatArray>): Array<FloatArray> =
        OnnxTensor.createTensor(environment, batch.toTypedArray()).use { tensor ->
            session.run(mapOf(INPUT to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                (result.get(0).value as Array<FloatArray>)
            }
        }

    override fun close() = sessions.forEach { it.second.close() }

    companion object {
        const val DIR = "moods"
        private const val INPUT = "embeddings"

        /**
         * The length a unit embedding is given when its own is unknown. Over 169 of the owner's songs
         * the lengths ran 2.4 to 6.4 (10th to 90th percentile); 3.0 kept the single heads' ranking
         * closest to the reference.
         */
        const val APPROXIMATE_SCALE = 3.0f

        /** The heads, with the column of each output Attic keeps. Classes are in each file's metadata. */
        val HEADS = listOf(
            Head("mood_happy-discogs-effnet-1.onnx", listOf(0 to MoodSignals.HAPPY), needsLength = false),
            Head("mood_sad-discogs-effnet-1.onnx", listOf(1 to MoodSignals.SAD), needsLength = false),
            Head("mood_relaxed-discogs-effnet-1.onnx", listOf(1 to MoodSignals.RELAXED), needsLength = false),
            Head("mood_aggressive-discogs-effnet-1.onnx", listOf(0 to MoodSignals.AGGRESSIVE), needsLength = false),
            Head("mood_party-discogs-effnet-1.onnx", listOf(1 to MoodSignals.PARTY), needsLength = false),
            Head("danceability-discogs-effnet-1.onnx", listOf(0 to MoodSignals.DANCEABLE), needsLength = false),
            Head("voice_instrumental-discogs-effnet-1.onnx", listOf(0 to MoodSignals.INSTRUMENTAL), needsLength = false),
            Head("engagement_regression-discogs-effnet-1.onnx", listOf(0 to MoodSignals.ENGAGEMENT), needsLength = true),
            Head(
                "mtg_jamendo_moodtheme-discogs-effnet-1.onnx",
                MoodSignals.THEME_TAGS.indices.map { it to MoodSignals.THEME_START + it },
                needsLength = true,
            ),
        )

        /** The class each kept output names, as the metadata files spell it, for the test that checks the columns. */
        val KEPT_CLASSES = mapOf(
            "mood_happy" to "happy",
            "mood_sad" to "sad",
            "mood_relaxed" to "relaxed",
            "mood_aggressive" to "aggressive",
            "mood_party" to "party",
            "danceability" to "danceable",
            "voice_instrumental" to "instrumental",
            "engagement_regression" to "engagement",
        )
    }
}
