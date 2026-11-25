package app.attic.player.playback.eq

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

class BiquadTest {
    /** |H(e^jw)| in dB from the complex transfer function, independent of [Biquad.magnitudeSquared]. */
    private fun complexDb(f: Biquad, hz: Double, rate: Int): Double {
        val w = 2 * PI * hz / rate
        val nr = f.b0 + f.b1 * cos(w) + f.b2 * cos(2 * w)
        val ni = -f.b1 * sin(w) - f.b2 * sin(2 * w)
        val dr = 1 + f.a1 * cos(w) + f.a2 * cos(2 * w)
        val di = -f.a1 * sin(w) - f.a2 * sin(2 * w)
        return 20 * log10(hypot(nr, ni) / hypot(dr, di))
    }

    private fun assertCoefficients(expected: DoubleArray, f: Biquad) {
        val actual = doubleArrayOf(f.b0, f.b1, f.b2, f.a1, f.a2)
        for (i in expected.indices) assertEquals("coefficient $i", expected[i], actual[i], 1e-12)
    }

    @Test
    fun peakingReachesItsGainAtTheCentreAndNothingAtTheEnds() {
        for (rate in intArrayOf(44_100, 48_000)) for (hz in doubleArrayOf(31.25, 1000.0, 8000.0)) for (gain in doubleArrayOf(-12.0, 6.0, 12.0)) {
            val f = Biquad.peaking(hz, gain, 0.8, rate)
            assertEquals(gain, f.magnitudeDb(hz, rate), 1e-4)
            assertEquals(gain, complexDb(f, hz, rate), 1e-4)
            assertEquals(0.0, f.magnitudeDb(0.0, rate), 1e-4)
            assertEquals(0.0, f.magnitudeDb(rate / 2.0, rate), 1e-4)
        }
    }

    @Test
    fun peakingMatchesTheCookbookByHand() {
        // Peaking EQ, fs 48 kHz, f0 1 kHz, Q 1, +6 dB, worked from the cookbook formulas.
        val a = 10.0.pow(6.0 / 40)
        val w0 = 2 * PI * 1000 / 48_000
        val alpha = sin(w0) / 2
        val a0 = 1 + alpha / a
        assertCoefficients(doubleArrayOf((1 + alpha * a) / a0, -2 * cos(w0) / a0, (1 - alpha * a) / a0, -2 * cos(w0) / a0, (1 - alpha / a) / a0), Biquad.peaking(1000.0, 6.0, 1.0, 48_000))
        // The same filter's values as computed separately in double precision (Python, numpy).
        assertCoefficients(doubleArrayOf(1.043953086990335, -1.895320723936596, 0.867722284759857, -1.895320723936596, 0.911675371750192), Biquad.peaking(1000.0, 6.0, 1.0, 48_000))
    }

    @Test
    fun shelvesMatchIndependentValues() {
        assertCoefficients(doubleArrayOf(0.996792425901784, -1.978059141061034, 0.98138669605839, -1.977999434827318, 0.978238828193891), Biquad.lowShelf(100.0, -6.0, 1.0, 48_000))
        assertCoefficients(doubleArrayOf(1.754303442995509, -0.816775671241565, 0.368676776762536, 0.131569898655769, 0.17463464986071), Biquad.highShelf(10_000.0, 9.0, 1.0, 44_100))
    }

    @Test
    fun shelvesReachTheirGainAtTheExtremesAndHalfAtTheCorner() {
        val low = Biquad.lowShelf(200.0, 9.0, 1.0, 48_000)
        assertEquals(9.0, low.magnitudeDb(0.0, 48_000), 1e-6)
        assertEquals(0.0, low.magnitudeDb(24_000.0, 48_000), 1e-6)
        assertEquals(4.5, low.magnitudeDb(200.0, 48_000), 1e-6)
        val high = Biquad.highShelf(EqDesign.TREBLE_SHELF_HZ, -8.0, 1.0, 44_100)
        assertEquals(-8.0, high.magnitudeDb(22_050.0, 44_100), 1e-6)
        assertEquals(0.0, high.magnitudeDb(0.0, 44_100), 1e-6)
        assertEquals(-4.0, high.magnitudeDb(EqDesign.TREBLE_SHELF_HZ, 44_100), 1e-6)
        assertEquals(complexDb(high, 5000.0, 44_100), high.magnitudeDb(5000.0, 44_100), 1e-9)
    }

    @Test
    fun cascadeAddsInDecibels() {
        val a = Biquad.peaking(1000.0, 6.0, 0.8, 48_000)
        val b = Biquad.peaking(2000.0, -3.0, 0.8, 48_000)
        for (hz in doubleArrayOf(50.0, 1000.0, 1414.0, 9000.0)) {
            assertEquals(a.magnitudeDb(hz, 48_000) + b.magnitudeDb(hz, 48_000), Biquad.cascadeDb(listOf(a, b), hz, 48_000), 1e-9)
        }
        assertEquals(0.0, Biquad.cascadeDb(listOf(Biquad.IDENTITY), 440.0, 48_000), 1e-12)
    }

    @Test
    fun zeroGainIsExactlyTransparent() {
        val f = Biquad.peaking(31.25, 0.0, 0.8, 48_000)
        assertEquals(f.b1, f.a1, 0.0)
        assertEquals(f.b2, f.a2, 0.0)
        assertEquals(1.0, f.b0, 0.0)
    }
}
