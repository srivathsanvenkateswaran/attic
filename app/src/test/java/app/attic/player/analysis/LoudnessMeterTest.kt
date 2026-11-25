package app.attic.player.analysis

import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoudnessMeterTest {
    /** EBU Tech 3341 case 1: a 1 kHz stereo sine with -23 dBFS peaks reads -23.0 LUFS (±0.1). */
    @Test
    fun stereoSineAtMinus23ReadsMinus23() {
        val lufs = measure(48_000, 2, listOf(Tone(997.0, dbfs(-23.0), 20.0)))
        assertEquals(-23.0, lufs!!, 0.1)
    }

    /** The same sine at CD rate, so the coefficients worked out for 44.1 kHz are right too. */
    @Test
    fun sineAt44100ReadsTheSame() {
        val lufs = measure(44_100, 2, listOf(Tone(997.0, dbfs(-23.0), 20.0)))
        assertEquals(-23.0, lufs!!, 0.1)
    }

    /** One channel carries half the power of two: BS.1770 puts a mono sine 3.01 LU below the stereo one. */
    @Test
    fun monoSineReadsThreeDecibelsLower() {
        val lufs = measure(48_000, 1, listOf(Tone(997.0, dbfs(-23.0), 20.0)))
        assertEquals(-26.01, lufs!!, 0.1)
    }

    /** EBU Tech 3341 case 3: 10 s at -36, 60 s at -23, 10 s at -36 reads -23.0, because the relative gate drops the quiet parts. */
    @Test
    fun relativeGateIgnoresQuietPassages() {
        val lufs = measure(
            48_000,
            2,
            listOf(Tone(997.0, dbfs(-36.0), 10.0), Tone(997.0, dbfs(-23.0), 60.0), Tone(997.0, dbfs(-36.0), 10.0)),
        )
        assertEquals(-23.0, lufs!!, 0.1)
    }

    /** EBU Tech 3341 case 5: 20 s at -26, 20.1 s at -20, 20 s at -26 reads -23.0. */
    @Test
    fun mixedLevelsAverageByPower() {
        val lufs = measure(
            48_000,
            2,
            listOf(Tone(997.0, dbfs(-26.0), 20.0), Tone(997.0, dbfs(-20.0), 20.1), Tone(997.0, dbfs(-26.0), 20.0)),
        )
        assertEquals(-23.0, lufs!!, 0.1)
    }

    /**
     * Stereo white noise, against the level BS.1770's own published 48 kHz filter predicts for it: the
     * mean of the filter's power response across the band, times the noise power in both channels.
     */
    @Test
    fun whiteNoiseMatchesTheReferenceFilter() {
        val sigma = 0.05
        val rate = 48_000
        val random = Random(7)
        val meter = LoudnessMeter(rate, 2)
        val chunk = FloatArray(rate * 2)
        repeat(30) {
            for (i in chunk.indices) chunk[i] = (random.nextGaussian() * sigma).toFloat()
            meter.add(chunk)
        }
        val expected = -0.691 + 10 * log10(2 * sigma * sigma * referencePowerGain())
        assertEquals(expected, meter.result().integratedLufs!!, 0.1)
    }

    @Test
    fun silenceHasNoLoudness() {
        val meter = LoudnessMeter(48_000, 2)
        meter.add(FloatArray(48_000 * 2 * 5))
        assertNull(meter.result().integratedLufs)
        assertEquals(0.0, meter.result().samplePeak, 0.0)
    }

    @Test
    fun shorterThanOneBlockHasNoLoudness() {
        assertNull(measure(48_000, 2, listOf(Tone(997.0, 0.5, 0.3))))
    }

    @Test
    fun peakIsTheLargestSample() {
        val meter = LoudnessMeter(48_000, 2)
        meter.add(floatArrayOf(0.1f, -0.2f, 0.75f, -0.9f, 0.3f, 0.0f))
        assertEquals(0.9, meter.result().samplePeak, 1e-6)
    }

    private class Tone(val hz: Double, val amplitude: Double, val seconds: Double)

    private fun dbfs(db: Double) = 10.0.pow(db / 20)

    /** Feeds the tones in 1024-frame chunks, the same sine on every channel. */
    private fun measure(rate: Int, channels: Int, tones: List<Tone>): Double? {
        val meter = LoudnessMeter(rate, channels)
        var n = 0L
        for (tone in tones) {
            val frames = (tone.seconds * rate).toLong()
            var done = 0L
            while (done < frames) {
                val count = minOf(1024L, frames - done).toInt()
                val chunk = FloatArray(count * channels)
                for (f in 0 until count) {
                    val v = (tone.amplitude * sin(2 * PI * tone.hz * (n + f) / rate)).toFloat()
                    for (c in 0 until channels) chunk[f * channels + c] = v
                }
                meter.add(chunk)
                done += count
                n += count
            }
        }
        return meter.result().integratedLufs
    }

    /** Mean |H(e^jw)|² of the K filter over 0..π, from the coefficients printed in BS.1770-4 for 48 kHz. */
    private fun referencePowerGain(): Double {
        val shelfB = doubleArrayOf(1.53512485958697, -2.69169618940638, 1.19839281085285)
        val shelfA = doubleArrayOf(1.0, -1.69065929318241, 0.73248077421585)
        val hpB = doubleArrayOf(1.0, -2.0, 1.0)
        val hpA = doubleArrayOf(1.0, -1.99004745483398, 0.99007225036621)
        val points = 200_000
        var sum = 0.0
        for (i in 0 until points) {
            val w = PI * (i + 0.5) / points
            sum += power(shelfB, shelfA, w) * power(hpB, hpA, w)
        }
        return sum / points
    }

    private fun power(b: DoubleArray, a: DoubleArray, w: Double): Double {
        fun magnitude2(c: DoubleArray): Double {
            val re = c[0] + c[1] * cos(w) + c[2] * cos(2 * w)
            val im = -c[1] * sin(w) - c[2] * sin(2 * w)
            return re * re + im * im
        }
        return magnitude2(b) / magnitude2(a)
    }
}
