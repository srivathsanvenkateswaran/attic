package app.attic.player.playback.eq

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A second-order filter's coefficients, normalised so a0 = 1, from Robert Bristow-Johnson's Audio EQ
 * Cookbook. The equaliser runs them as transposed direct form II in double precision; see
 * [EqualiserAudioProcessor] for why double.
 */
class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
    /** |H|² at the angular frequency whose cosine is [cosW]. Real arithmetic only, so it is cheap. */
    fun magnitudeSquared(cosW: Double): Double {
        val cos2W = 2 * cosW * cosW - 1
        val num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * cosW + 2 * b0 * b2 * cos2W
        val den = 1 + a1 * a1 + a2 * a2 + 2 * (a1 + a1 * a2) * cosW + 2 * a2 * cos2W
        return num / den
    }

    fun magnitudeDb(frequencyHz: Double, sampleRate: Int): Double = 10 * log10(max(magnitudeSquared(cosOf(frequencyHz, sampleRate)), TINY))

    override fun toString(): String = "Biquad(b0=$b0, b1=$b1, b2=$b2, a1=$a1, a2=$a2)"

    companion object {
        val IDENTITY = Biquad(1.0, 0.0, 0.0, 0.0, 0.0)

        /** Peaking EQ: [gainDb] at [frequencyHz], back to 0 dB at DC and Nyquist. [q] is the cookbook's Q. */
        fun peaking(frequencyHz: Double, gainDb: Double, q: Double, sampleRate: Int): Biquad =
            from(DoubleArray(5).also { Rbj.peaking(cosOf(frequencyHz, sampleRate), sinOf(frequencyHz, sampleRate) / (2 * q), gainDb, it, 0) })

        /** Low shelf with the cookbook's shelf slope [slope] (1 is the steepest without overshoot). */
        fun lowShelf(frequencyHz: Double, gainDb: Double, slope: Double, sampleRate: Int): Biquad =
            from(DoubleArray(5).also { Rbj.lowShelf(cosOf(frequencyHz, sampleRate), sinOf(frequencyHz, sampleRate), slope, gainDb, it, 0) })

        fun highShelf(frequencyHz: Double, gainDb: Double, slope: Double, sampleRate: Int): Biquad =
            from(DoubleArray(5).also { Rbj.highShelf(cosOf(frequencyHz, sampleRate), sinOf(frequencyHz, sampleRate), slope, gainDb, it, 0) })

        /** Butterworth-style low-pass, used only to measure bass energy for the debug log. */
        fun lowPass(frequencyHz: Double, q: Double, sampleRate: Int): Biquad {
            val c = cosOf(frequencyHz, sampleRate)
            val alpha = sinOf(frequencyHz, sampleRate) / (2 * q)
            val a0 = 1 + alpha
            return Biquad((1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)
        }

        /** The response in dB of [filters] in series at [frequencyHz]. */
        fun cascadeDb(filters: List<Biquad>, frequencyHz: Double, sampleRate: Int): Double {
            val c = cosOf(frequencyHz, sampleRate)
            var power = 1.0
            for (f in filters) power *= f.magnitudeSquared(c)
            return 10 * log10(max(power, TINY))
        }

        internal fun cosOf(frequencyHz: Double, sampleRate: Int): Double = cos(2 * PI * frequencyHz / sampleRate)

        internal fun sinOf(frequencyHz: Double, sampleRate: Int): Double = sin(2 * PI * frequencyHz / sampleRate)

        private fun from(c: DoubleArray) = Biquad(c[0], c[1], c[2], c[3], c[4])

        private const val TINY = 1e-30
    }
}

/**
 * The cookbook formulas, writing b0, b1, b2, a1, a2 (normalised by a0) into [out] at [at]. They take the
 * cosine and sine of the centre frequency precomputed, so a glide can recompute coefficients every 64
 * frames with one `pow` per band and no allocation.
 */
internal object Rbj {
    fun peaking(cosW0: Double, alpha: Double, gainDb: Double, out: DoubleArray, at: Int) {
        val a = 10.0.pow(gainDb / 40)
        val a0 = 1 + alpha / a
        out[at] = (1 + alpha * a) / a0
        out[at + 1] = -2 * cosW0 / a0
        out[at + 2] = (1 - alpha * a) / a0
        out[at + 3] = -2 * cosW0 / a0
        out[at + 4] = (1 - alpha / a) / a0
    }

    fun lowShelf(cosW0: Double, sinW0: Double, slope: Double, gainDb: Double, out: DoubleArray, at: Int) {
        val a = 10.0.pow(gainDb / 40)
        val alpha = sinW0 / 2 * sqrt((a + 1 / a) * (1 / slope - 1) + 2)
        val k = 2 * sqrt(a) * alpha
        val a0 = (a + 1) + (a - 1) * cosW0 + k
        out[at] = a * ((a + 1) - (a - 1) * cosW0 + k) / a0
        out[at + 1] = 2 * a * ((a - 1) - (a + 1) * cosW0) / a0
        out[at + 2] = a * ((a + 1) - (a - 1) * cosW0 - k) / a0
        out[at + 3] = -2 * ((a - 1) + (a + 1) * cosW0) / a0
        out[at + 4] = ((a + 1) + (a - 1) * cosW0 - k) / a0
    }

    fun highShelf(cosW0: Double, sinW0: Double, slope: Double, gainDb: Double, out: DoubleArray, at: Int) {
        val a = 10.0.pow(gainDb / 40)
        val alpha = sinW0 / 2 * sqrt((a + 1 / a) * (1 / slope - 1) + 2)
        val k = 2 * sqrt(a) * alpha
        val a0 = (a + 1) - (a - 1) * cosW0 + k
        out[at] = a * ((a + 1) + (a - 1) * cosW0 + k) / a0
        out[at + 1] = -2 * a * ((a - 1) + (a + 1) * cosW0) / a0
        out[at + 2] = a * ((a + 1) + (a - 1) * cosW0 - k) / a0
        out[at + 3] = 2 * ((a - 1) - (a + 1) * cosW0) / a0
        out[at + 4] = ((a + 1) - (a - 1) * cosW0 - k) / a0
    }
}
