package app.attic.player.analysis

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

/** What a whole song measured: integrated loudness in LUFS (null when it never rose above silence) and its sample peak, 1.0 = full scale. */
data class SongLoudness(val integratedLufs: Double?, val samplePeak: Double)

/**
 * Integrated loudness after ITU-R BS.1770-4 / EBU R128: K-weighting, 400 ms blocks every 100 ms, an
 * absolute gate at -70 LUFS and a relative gate 10 LU under the ungated level. Fed the decoder's own
 * interleaved samples at their own rate, before analysis downmixes them, because a mono downmix reads
 * 3 to 6 dB low depending on how alike the channels are.
 *
 * Costs two biquads per channel per sample, a few percent of the decode it rides on.
 */
class LoudnessMeter(sampleRate: Int, private val channels: Int) {
    private val filters = Array(channels) { KWeighting(sampleRate.toDouble()) }
    private val weights = DoubleArray(channels) { channelWeight(it, channels) }
    private val stepFrames = sampleRate / 10
    private var stepFilled = 0
    private val stepSum = DoubleArray(channels)

    /** Weighted mean square of each 100 ms step; a 400 ms block is four in a row. */
    private var steps = DoubleArray(64 * 60 * 10)
    private var stepCount = 0
    private var peak = 0.0

    /** [interleaved] holds whole frames, samples in -1..1. */
    fun add(interleaved: FloatArray) {
        var i = 0
        val frames = interleaved.size / channels
        for (frame in 0 until frames) {
            for (c in 0 until channels) {
                val x = interleaved[i++].toDouble()
                val a = abs(x)
                if (a > peak) peak = a
                val y = filters[c].process(x)
                stepSum[c] += y * y
            }
            if (++stepFilled == stepFrames) closeStep()
        }
    }

    private fun closeStep() {
        var power = 0.0
        for (c in 0 until channels) {
            power += weights[c] * stepSum[c] / stepFrames
            stepSum[c] = 0.0
        }
        if (stepCount == steps.size) steps = steps.copyOf(steps.size * 2)
        steps[stepCount++] = power
        stepFilled = 0
    }

    fun result(): SongLoudness = SongLoudness(integrated(), peak)

    private fun integrated(): Double? {
        val blocks = stepCount - STEPS_PER_BLOCK + 1
        if (blocks <= 0) return null
        val blockPower = DoubleArray(blocks) { b ->
            var sum = 0.0
            for (s in b until b + STEPS_PER_BLOCK) sum += steps[s]
            sum / STEPS_PER_BLOCK
        }
        val absolute = powerOf(ABSOLUTE_GATE_LUFS)
        val aboveAbsolute = blockPower.filter { it > absolute }
        if (aboveAbsolute.isEmpty()) return null
        val relative = powerOf(loudnessOf(aboveAbsolute.average()) + RELATIVE_GATE_LU)
        val gated = aboveAbsolute.filter { it > relative }
        if (gated.isEmpty()) return null
        return loudnessOf(gated.average())
    }

    companion object {
        private const val STEPS_PER_BLOCK = 4
        private const val ABSOLUTE_GATE_LUFS = -70.0
        private const val RELATIVE_GATE_LU = -10.0

        fun loudnessOf(power: Double): Double = -0.691 + 10 * log10(power)
        private fun powerOf(lufs: Double): Double = 10.0.pow((lufs + 0.691) / 10)

        /** BS.1770's channel weights: 1 for left, right and centre, 0 for LFE, 1.41 for the surrounds of a 5.1 file. */
        private fun channelWeight(index: Int, channels: Int): Double = when {
            channels != 6 -> 1.0
            index == 3 -> 0.0
            index >= 4 -> 1.41
            else -> 1.0
        }
    }
}

/**
 * BS.1770's two-stage K filter (a high shelf, then the RLB high-pass), with its coefficients worked out
 * for any sample rate from the filters' analogue parameters the way libebur128 does, so 44.1 kHz files
 * aren't measured with 48 kHz coefficients.
 */
internal class KWeighting(sampleRate: Double) {
    private val shelf: Biquad
    private val highPass: Biquad

    init {
        var k = tan(PI * SHELF_HZ / sampleRate)
        val vh = 10.0.pow(SHELF_GAIN_DB / 20)
        val vb = vh.pow(0.4996667741545416)
        var a0 = 1 + k / SHELF_Q + k * k
        shelf = Biquad(
            b0 = (vh + vb * k / SHELF_Q + k * k) / a0,
            b1 = 2 * (k * k - vh) / a0,
            b2 = (vh - vb * k / SHELF_Q + k * k) / a0,
            a1 = 2 * (k * k - 1) / a0,
            a2 = (1 - k / SHELF_Q + k * k) / a0,
        )
        k = tan(PI * HIGH_PASS_HZ / sampleRate)
        a0 = 1 + k / HIGH_PASS_Q + k * k
        highPass = Biquad(
            b0 = 1.0,
            b1 = -2.0,
            b2 = 1.0,
            a1 = 2 * (k * k - 1) / a0,
            a2 = (1 - k / HIGH_PASS_Q + k * k) / a0,
        )
    }

    fun process(x: Double): Double = highPass.process(shelf.process(x))

    private companion object {
        const val SHELF_HZ = 1681.974450955533
        const val SHELF_GAIN_DB = 3.999843853973347
        const val SHELF_Q = 0.7071752369554196
        const val HIGH_PASS_HZ = 38.13547087602444
        const val HIGH_PASS_Q = 0.5003270373238773
    }
}

/** A direct form I biquad, a0 already divided out. */
internal class Biquad(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }
}
