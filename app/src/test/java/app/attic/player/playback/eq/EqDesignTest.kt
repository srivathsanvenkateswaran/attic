package app.attic.player.playback.eq

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqDesignTest {
    private val curves = linkedMapOf(
        "flat" to EqCurve.FLAT,
        "all +6" to EqCurve.of(6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f),
        "all +12" to EqCurve.of(12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f),
        "all -12" to EqCurve.of(-12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f),
        "alternating +-12" to EqCurve.of(12f, -12f, 12f, -12f, 12f, -12f, 12f, -12f, 12f, -12f),
        "alternating -+12" to EqCurve.of(-12f, 12f, -12f, 12f, -12f, 12f, -12f, 12f, -12f, 12f),
        "bass boost" to EqCurve.of(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        "smile" to EqCurve.of(6f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 6f),
        "one band +12" to EqCurve.of(0f, 0f, 0f, 0f, 12f, 0f, 0f, 0f, 0f, 0f),
        "31 Hz +12" to EqCurve.of(12f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "16 kHz +12" to EqCurve.of(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 12f),
        "random" to EqCurve.of(3f, -7f, 11f, -2f, 0f, 5f, -12f, 8f, -4f, 9f),
    ) + EqPresets.all.associate { "preset ${it.id}" to it.curve }

    @Test
    fun responseAtEachCentreMatchesTheSliders() {
        val lines = ArrayList<String>()
        for (rate in intArrayOf(44_100, 48_000)) {
            val design = EqDesign.forRate(rate)
            var worstAll = 0.0
            for ((name, curve) in curves) {
                val gains = design.filterGainsDb(curve)
                var worst = 0.0
                for (b in 0 until EqBands.COUNT) {
                    val error = design.responseDb(gains, EqBands.frequenciesHz[b]) - curve[b]
                    worst = max(worst, abs(error))
                }
                // Between centres: how far the real response strays from the straight line joining the sliders.
                var between = 0.0
                for (b in 0 until EqBands.COUNT - 1) {
                    val mid = design.responseDb(gains, EqBands.frequenciesHz[b] * 1.4142135623730951)
                    between = max(between, abs(mid - (curve[b] + curve[b + 1]) / 2.0))
                }
                worstAll = max(worstAll, worst)
                lines += String.format(Locale.ROOT, "%d Hz  %-24s centre max err %.3f dB  midpoint dev %.2f dB  filter gains [%s]  pre-gain %.2f dB", rate, name, worst, between, gains.joinToString(",") { String.format(Locale.ROOT, "%.1f", it) }, design.preGainDb(gains))
                assertTrue("$name at $rate Hz is off by $worst dB", worst < 1.0)
            }
            lines += String.format(Locale.ROOT, "%d Hz  worst centre error over all curves %.3f dB", rate, worstAll)
        }
        EqBench.report("compensation", lines)
    }

    @Test
    fun withoutCompensationAllPlusSixOvershoots() {
        val design = EqDesign.forRate(48_000)
        val naive = DoubleArray(EqBands.COUNT) { 6.0 }
        val at1k = design.responseDb(naive, 1000.0)
        EqBench.report("compensation", listOf(String.format(Locale.ROOT, "naive all +6 (filter gain = slider) gives %.2f dB at 1 kHz", at1k)))
        assertTrue(at1k > 9.0)
    }

    @Test
    fun flatNeedsNoFiltersAndNoPreGain() {
        val design = EqDesign.forRate(44_100)
        val gains = design.filterGainsDb(EqCurve.FLAT)
        assertTrue(gains.all { it == 0.0 })
        assertEquals(0.0, design.preGainDb(gains), 0.0)
    }

    @Test
    fun preGainTakesBackThePeakBoostOnly() {
        val design = EqDesign.forRate(48_000)
        // A pure cut needs no pre-gain beyond the half-dB ripple the compensation leaves next to it.
        val cut = design.filterGainsDb(EqCurve.of(-6f, -6f, -6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        assertTrue(design.preGainDb(cut) > -0.75)
        assertEquals(-design.peakDb(cut), design.preGainDb(cut), 0.0)
        val boost = design.filterGainsDb(EqCurve.of(0f, 0f, 0f, 0f, 0f, 9f, 0f, 0f, 0f, 0f))
        assertEquals(-9.0, design.preGainDb(boost), 0.3)
    }

    @Test
    fun otherSampleRatesStillWork() {
        for (rate in intArrayOf(8_000, 16_000, 22_050, 32_000, 88_200, 96_000, 192_000)) {
            val design = EqDesign.forRate(rate)
            val curve = EqCurve.of(6f, 5f, 4f, 2f, 0f, 0f, -3f, 0f, 0f, 0f)
            val gains = design.filterGainsDb(curve)
            assertTrue("$rate", gains.all { it.isFinite() && abs(it) <= EqDesign.MAX_FILTER_DB })
            // The bass bands are far from Nyquist at every rate, so they still land.
            for (b in 0..3) assertEquals("$rate band $b", curve[b].toDouble(), design.responseDb(gains, EqBands.frequenciesHz[b]), 1.0)
        }
        assertTrue(EqDesign.forRate(96_000).filterGainsDb(EqCurve.of(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 12f)).let { abs(EqDesign.forRate(96_000).responseDb(it, 16_000.0) - 12) < 1.0 })
    }
}
