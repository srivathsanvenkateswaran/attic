package app.attic.player.playback.eq

import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Measures how much work the limiter is left with when the pre-gain takes back all, three quarters,
 * half or none of a curve's peak boost, on two music-like signals. This is the evidence behind
 * [EqDesign.HEADROOM_COMPENSATION]; it uses the processor's filters and the same limiter law.
 */
class EqHeadroomTest {
    private val rate = 48_000

    /** Pink-ish noise (Paul Kellet's filter), mono, [seconds] long. */
    private fun pink(seconds: Int, seed: Int): DoubleArray {
        val r = Random(seed)
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
        return DoubleArray(rate * seconds) {
            val w = r.nextDouble(-1.0, 1.0)
            b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.96900 * b2 + w * 0.1538520
            b3 = 0.86650 * b3 + w * 0.3104856; b4 = 0.55000 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.0168980
            val y = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
            b6 = w * 0.115926
            y
        }
    }

    private fun normalisePeak(x: DoubleArray, peakDb: Double): DoubleArray {
        val k = 10.0.pow(peakDb / 20) / x.maxOf { abs(it) }
        return DoubleArray(x.size) { x[it] * k }
    }

    /** A dynamic track after levelling: peaks at -1 dBFS, RMS around -14 dBFS. */
    private fun levelled() = normalisePeak(pink(20, 1), -1.0)

    /** A loud modern master played without levelling: saturated to about 8 dB crest, peaks at -0.1 dBFS. */
    private fun loudMaster(): DoubleArray {
        val p = pink(20, 2)
        val rms = sqrt(p.sumOf { it * it } / p.size)
        return normalisePeak(DoubleArray(p.size) { tanh(p[it] / rms * 0.45) }, -0.1)
    }

    private class Result(val limitedPercent: Double, val maxReductionDb: Double, val levelChangeDb: Double)

    private fun run(input: DoubleArray, curve: EqCurve, fraction: Double): Result {
        val design = EqDesign.forRate(rate)
        val gains = design.filterGainsDb(curve)
        val filters = (0 until EqBands.COUNT).filter { gains[it] != 0.0 }.map { design.filter(it, gains[it]) }
        val pre = 10.0.pow(-fraction * max(0.0, design.peakDb(gains)) / 20)
        val ceiling = EqualiserAudioProcessor.CEILING
        val release = exp(-1.0 / (EqualiserAudioProcessor.RELEASE_SECONDS * rate))
        val s1 = DoubleArray(filters.size)
        val s2 = DoubleArray(filters.size)
        var g = 1.0
        var limited = 0
        var minGain = 1.0
        var inPower = 0.0
        var outPower = 0.0
        for (x in input) {
            var y = x
            for (i in filters.indices) {
                val f = filters[i]
                val out = f.b0 * y + s1[i]
                s1[i] = f.b1 * y - f.a1 * out + s2[i]
                s2[i] = f.b2 * y - f.a2 * out
                y = out
            }
            y *= pre
            g = 1.0 - (1.0 - g) * release
            if (abs(y) * g > ceiling) g = ceiling / abs(y)
            if (g < 0.9988) limited++ // more than 0.01 dB of reduction
            minGain = minOf(minGain, g)
            inPower += x * x
            outPower += (y * g).pow(2)
        }
        return Result(100.0 * limited / input.size, -20 * log10(minGain) + 0.0, 10 * log10(outPower / inPower))
    }

    @Test
    fun fullCompensationLeavesTheLimiterAlmostIdle() {
        val curves = listOf("bass_boost", "loudness", "treble_boost", "hip_hop").map { EqPresets.byId(it)!! } +
            EqPreset("+6 at 62 Hz", EqCurve.of(0f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        val lines = ArrayList<String>()
        val signals = listOf("levelled, peaks -1 dBFS" to levelled(), "loud master, peaks -0.1 dBFS" to loudMaster())
        var fullWorst = 0.0
        for ((signalName, signal) in signals) for (preset in curves) for (fraction in doubleArrayOf(1.0, 0.75, 0.5, 0.0)) {
            val r = run(signal, preset.curve, fraction)
            if (fraction == 1.0) fullWorst = max(fullWorst, r.limitedPercent)
            lines += String.format(Locale.ROOT, "%-28s %-12s pre-gain %3.0f%% of peak: limiting %6.2f%% of samples, max reduction %5.2f dB, level change %+5.2f dB", signalName, preset.id, fraction * 100, r.limitedPercent, r.maxReductionDb, r.levelChangeDb)
        }
        EqBench.report("headroom", lines)
        assertTrue("limiter busy on $fullWorst% of samples with full compensation", fullWorst < 1.0)
    }
}
