package app.attic.player.analysis

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleScoresTest {
    @Test
    fun averagesEachStyleOverPatches() {
        val averager = StyleAverager(size = 3)
        averager.add(floatArrayOf(0.2f, 0.9f, 0.0f))
        averager.add(floatArrayOf(0.4f, 0.1f, 1.0f))
        assertArrayEquals(floatArrayOf(0.3f, 0.5f, 0.5f), averager.mean(), 1e-6f)
    }

    @Test
    fun topIsHighestFirstWithTiesInLabelOrder() {
        val averager = StyleAverager(size = 4)
        averager.add(floatArrayOf(0.1f, 0.5f, 0.5f, 0.7f))
        assertEquals(listOf(3, 1, 2), averager.top(3).map { it.label })
    }

    @Test
    fun topTakesFiveByDefault() {
        val averager = StyleAverager()
        averager.add(FloatArray(SongAnalyzer.STYLE_COUNT) { it / 1000f })
        assertEquals(listOf(399, 398, 397, 396, 395), averager.top().map { it.label })
    }

    @Test
    fun noPatchesGiveNoStyles() {
        assertTrue(StyleAverager().top().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsARowOfTheWrongWidth() {
        StyleAverager(size = 3).add(FloatArray(4))
    }

    @Test
    fun codecKeepsLabelsAndScoresToFourDecimals() {
        val styles = listOf(StyleScore(399, 0.81234f), StyleScore(0, 0.5f), StyleScore(271, 0.00003f), StyleScore(12, 1f), StyleScore(7, 0f))
        val bytes = StyleCodec.encode(styles)
        assertEquals(20, bytes.size)
        val back = StyleCodec.decode(bytes)
        assertEquals(styles.map { it.label }, back.map { it.label })
        styles.zip(back).forEach { (a, b) -> assertEquals(a.score, b.score, 1f / 65_535) }
    }

    @Test
    fun codecClampsScoresOutsideZeroToOne() {
        val back = StyleCodec.decode(StyleCodec.encode(listOf(StyleScore(1, 1.5f), StyleScore(2, -0.2f))))
        assertEquals(1f, back[0].score, 0f)
        assertEquals(0f, back[1].score, 0f)
    }

    @Test
    fun emptyBlobIsNoStyles() {
        assertTrue(StyleCodec.decode(ByteArray(0)).isEmpty())
        assertEquals(1, StyleCodec.decode(ByteArray(6)).size)
    }
}
