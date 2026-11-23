package app.attic.player.analysis

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Recognises the same song in another language, key or edit by following the melody over time. The
 * method follows cover-song detection (Serrà et al., 2009). On the test library every same-song pair
 * scored above 21 and every different pair sampled stayed below 6.4.
 */
object MelodyMatch {
    const val SAME_SONG = 12f

    private const val STACK = 4
    private const val NEAREST_SHARE = 0.1
    private const val GAP = 0.5f

    fun score(first: ByteArray, second: ByteArray): Float {
        val (a, b) = if (first.size <= second.size) first to second else second to first
        val rows = a.size / 12
        val columns = b.size / 12
        if (rows <= STACK || columns <= STACK) return 0f

        val rotation = bestRotation(a, b)
        val similarity = stackedSimilarity(a, b, rows, columns, rotation)
        val height = rows - STACK + 1
        val width = columns - STACK + 1
        val rowCut = cutoffsByRow(similarity, height, width)
        val columnCut = cutoffsByColumn(similarity, height, width)

        var above = FloatArray(width + 2)
        var above2 = FloatArray(width + 2)
        var current = FloatArray(width + 2)
        var best = 0f
        for (i in 0 until height) {
            for (j in 0 until width) {
                val s = similarity[i * width + j]
                val diagonal = above[j + 1]
                val skipRow = above2[j + 1]
                val skipColumn = above[j]
                val value = if (s >= rowCut[i] && s >= columnCut[j]) {
                    max(diagonal, max(skipRow, skipColumn)) + 1f
                } else {
                    max(0f, max(diagonal, max(skipRow, skipColumn)) - GAP)
                }
                current[j + 2] = value
                if (value > best) best = value
            }
            val recycled = above2
            above2 = above
            above = current
            current = recycled
        }
        return best / sqrt(height.toFloat())
    }

    /** The transposition that best lines up the songs' overall pitch profiles. */
    private fun bestRotation(a: ByteArray, b: ByteArray): Int {
        val pa = profile(a)
        val pb = profile(b)
        return (0 until 12).maxByOrNull { k ->
            var dot = 0f
            for (c in 0 until 12) dot += pa[c] * pb[Math.floorMod(c - k, 12)]
            dot
        } ?: 0
    }

    private fun profile(chroma: ByteArray): FloatArray {
        val out = FloatArray(12)
        for (i in chroma.indices) out[i % 12] += (chroma[i].toInt() and 0xFF).toFloat()
        return out
    }

    /** Frame similarity averaged over a short run, so a match means a phrase rather than one chord. */
    private fun stackedSimilarity(a: ByteArray, b: ByteArray, rows: Int, columns: Int, rotation: Int): FloatArray {
        val raw = FloatArray(rows * columns)
        val rotated = FloatArray(columns * 12)
        for (j in 0 until columns) {
            for (c in 0 until 12) {
                rotated[j * 12 + c] = (b[j * 12 + Math.floorMod(c - rotation, 12)].toInt() and 0xFF) / 255f
            }
        }
        val row = FloatArray(12)
        for (i in 0 until rows) {
            for (c in 0 until 12) row[c] = (a[i * 12 + c].toInt() and 0xFF) / 255f
            for (j in 0 until columns) {
                var dot = 0f
                val offset = j * 12
                for (c in 0 until 12) dot += row[c] * rotated[offset + c]
                raw[i * columns + j] = dot
            }
        }
        val height = rows - STACK + 1
        val width = columns - STACK + 1
        val out = FloatArray(height * width)
        for (i in 0 until height) {
            for (j in 0 until width) {
                var sum = 0f
                for (k in 0 until STACK) sum += raw[(i + k) * columns + j + k]
                out[i * width + j] = sum / STACK
            }
        }
        return out
    }

    private fun cutoffsByRow(similarity: FloatArray, height: Int, width: Int): FloatArray {
        val keep = max(1, (width * NEAREST_SHARE).toInt())
        val scratch = FloatArray(width)
        return FloatArray(height) { i ->
            System.arraycopy(similarity, i * width, scratch, 0, width)
            scratch.sort()
            scratch[width - keep]
        }
    }

    private fun cutoffsByColumn(similarity: FloatArray, height: Int, width: Int): FloatArray {
        val keep = max(1, (height * NEAREST_SHARE).toInt())
        val scratch = FloatArray(height)
        return FloatArray(width) { j ->
            for (i in 0 until height) scratch[i] = similarity[i * width + j]
            scratch.sort()
            scratch[height - keep]
        }
    }
}
