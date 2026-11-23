package app.attic.player.analysis

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** One of a song's strongest styles: an index into [StyleLabels] and the model's averaged score, 0 to 1. */
data class StyleScore(val label: Int, val score: Float)

/**
 * Averages the model's style scores over a song's patches. Each patch's `activations` row holds one
 * independent sigmoid per style, so the song's score for a style is the mean of that style's column.
 */
class StyleAverager(private val size: Int = SongAnalyzer.STYLE_COUNT) {
    private val sum = DoubleArray(size)
    private var patches = 0

    fun add(row: FloatArray) {
        require(row.size == size) { "Expected $size scores, got ${row.size}" }
        for (i in row.indices) sum[i] += row[i].toDouble()
        patches++
    }

    fun mean(): FloatArray = FloatArray(size) { if (patches == 0) 0f else (sum[it] / patches).toFloat() }

    /** The [count] highest means, highest first. Ties keep the label order. Empty before any patch. */
    fun top(count: Int = StyleCodec.TOP): List<StyleScore> =
        if (patches == 0) emptyList() else StyleCodec.top(mean(), count)
}

/**
 * The stored form of a song's top styles: per style, the label index and the score scaled to 0..65535,
 * both as little-endian unsigned 16-bit numbers. Five styles take 20 bytes.
 */
object StyleCodec {
    const val TOP = 5
    private const val ENTRY_BYTES = 4
    private const val SCALE = 65_535f

    fun top(scores: FloatArray, count: Int = TOP): List<StyleScore> =
        scores.indices
            .sortedWith(compareByDescending<Int> { scores[it] }.thenBy { it })
            .take(count)
            .map { StyleScore(it, scores[it]) }

    fun encode(styles: List<StyleScore>): ByteArray {
        val buffer = ByteBuffer.allocate(styles.size * ENTRY_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        for (style in styles) {
            require(style.label in 0..0xFFFF) { "Label ${style.label} out of range" }
            buffer.putShort(style.label.toShort())
            buffer.putShort((style.score.coerceIn(0f, 1f) * SCALE).roundToInt().toShort())
        }
        return buffer.array()
    }

    /** A blob of the wrong length decodes as far as it goes. */
    fun decode(bytes: ByteArray): List<StyleScore> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return List(bytes.size / ENTRY_BYTES) {
            val label = buffer.short.toInt() and 0xFFFF
            val score = (buffer.short.toInt() and 0xFFFF) / SCALE
            StyleScore(label, score)
        }
    }
}
