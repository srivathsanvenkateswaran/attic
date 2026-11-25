package app.attic.player.playback.eq

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqResponseTest {
    @Test
    fun flatIsAStraightLineAtZero() {
        val r = EqResponse.of(EqCurve.FLAT)
        assertEquals(EqResponse.DEFAULT_POINTS, r.gainsDb.size)
        assertTrue(r.gainsDb.all { it == 0f })
        assertEquals(20f, r.frequenciesHz.first(), 1e-3f)
        assertEquals(20_000f, r.frequenciesHz.last(), 1e-1f)
        assertEquals(0f, r.preGainDb, 0f)
    }

    @Test
    fun followsTheSliders() {
        val curve = EqCurve.of(6f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 6f)
        val r = EqResponse.of(curve, sampleRate = 44_100, points = 2048)
        for (b in 0 until EqBands.COUNT) {
            val f = EqBands.frequenciesHz[b]
            val i = r.frequenciesHz.indices.minBy { kotlin.math.abs(r.frequenciesHz[it] - f) }
            assertEquals("band $b", curve[b], r.gainsDb[i], 0.5f)
        }
        assertTrue(r.preGainDb < -5.5f)
    }

    @Test
    fun cheapEnoughForEverySliderMove() {
        val curves = List(64) { i -> EqCurve.of(*FloatArray(EqBands.COUNT) { b -> ((i * 7 + b * 3) % 49 - 24) * 0.5f }) }
        repeat(2_000) { EqResponse.of(curves[it % curves.size]) }
        val runs = 5_000
        val start = System.nanoTime()
        var sink = 0f
        repeat(runs) { sink += EqResponse.of(curves[it % curves.size]).gainsDb[64] }
        val each = (System.nanoTime() - start) / runs
        val design = EqDesign.forRate(48_000)
        val solveStart = System.nanoTime()
        repeat(runs) { sink += design.filterGainsDb(curves[it % curves.size])[0].toFloat() }
        val solve = (System.nanoTime() - solveStart) / runs
        EqBench.report("response", listOf(String.format(Locale.ROOT, "EqResponse.of 128 points: %.1f us per call (compensation solve alone %.1f us) [%s]", each / 1000.0, solve / 1000.0, if (sink.isNaN()) "nan" else "ok")))
        assertTrue("$each ns", each < 2_000_000)
    }
}
