package app.attic.player.playback.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualiserAudioProcessorTest {
    private val bass = EqPresets.byId(EqPresets.BASS_BOOST)!!.curve
    private val oneK = EqCurve.of(0f, 0f, 0f, 0f, 0f, 6f, 0f, 0f, 0f, 0f)
    private val ceiling16 = (EqualiserAudioProcessor.CEILING * 32768).toInt() + 1

    private fun rms(x: ShortArray, from: Int, until: Int): Double {
        var s = 0.0
        for (i in from until until) s += x[i].toDouble() * x[i]
        return sqrt(s / (until - from)) / 32768
    }

    private fun rms(x: FloatArray, from: Int, until: Int): Double {
        var s = 0.0
        for (i in from until until) s += x[i].toDouble() * x[i]
        return sqrt(s / (until - from))
    }

    private fun maxStep(x: ShortArray, channels: Int, from: Int, until: Int): Int {
        var m = 0
        for (i in from + channels until until) m = max(m, abs(x[i] - x[i - channels]))
        return m
    }

    @Test
    fun eachItemGetsItsOwnCurveAtTheBoundary() {
        val source = FakeEqSource("A" to bass, "B" to null, "C" to EqCurve.FLAT)
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        assertEquals(bass, p.appliedCurve)
        assertTrue(p.preGainDb < -4f)
        p.flush(item("B"))
        assertNull(p.appliedCurve)
        assertEquals(0f, p.preGainDb, 0f)
        p.flush(item("A"))
        assertEquals(bass, p.appliedCurve)
        p.flush(item("C"))
        assertNull("A flat curve is treated as no curve", p.appliedCurve)
    }

    /**
     * Plays a 55 Hz and 1 kHz mix through item A, ends the stream and flushes into item B as Media3
     * does at a gapless boundary, and carries the same waveform on. Returns the largest
     * sample-to-sample step at the boundary and the largest anywhere else.
     */
    private fun boundarySteps(a: EqCurve?, b: EqCurve?): Pair<Int, Int> {
        val frames = 48_000
        val bass = sine(55.0, 0.45, frames, 2)
        val tone = sine(1000.0, 0.1, frames, 2)
        val mix = DoubleArray(bass.size) { bass[it] + tone[it] }.toShorts()
        val half = frames // samples: half the frames, both channels
        val p = EqualiserAudioProcessor(FakeEqSource("A" to a, "B" to b)).start(48_000, 2, PCM16, "A")
        val first = p.runShorts(mix.copyOfRange(0, half))
        p.queueEndOfStream()
        p.getOutput()
        p.flush(item("B"))
        val second = p.runShorts(mix.copyOfRange(half, mix.size))
        val out = first + second
        val atBoundary = maxStep(out, 2, half - 4, half + 4)
        val elsewhere = max(maxStep(out, 2, 2_000, half - 4), maxStep(out, 2, half + 4, out.size))
        return atBoundary to elsewhere
    }

    @Test
    fun aGaplessBoundaryWithTheSameCurveDoesNotClick() {
        val (boundary, elsewhere) = boundarySteps(bass, bass)
        assertTrue("step $boundary at the boundary, $elsewhere elsewhere", boundary <= elsewhere + 64)
    }

    @Test
    fun aGaplessBoundaryIntoAnotherCurveGlidesWithoutAClick() {
        for ((a, b) in listOf(bass to oneK, bass to null, null to bass)) {
            val (boundary, elsewhere) = boundarySteps(a, b)
            assertTrue("$a to $b: step $boundary at the boundary, $elsewhere elsewhere", boundary <= elsewhere + 64)
        }
    }

    @Test
    fun theNextSongsCurveIsTheOneAppliedAfterAGaplessBoundary() {
        val p = EqualiserAudioProcessor(FakeEqSource("A" to bass, "B" to oneK)).start(48_000, 2, PCM16, "A")
        p.runShorts(ShortArray(4096))
        p.queueEndOfStream()
        p.getOutput()
        p.flush(item("B"))
        assertEquals(oneK, p.appliedCurve)
        p.runShorts(ShortArray(48_000 * 2))
        // After the glide, B sounds exactly as if it had started on its own.
        val fresh = EqualiserAudioProcessor(FakeEqSource("B" to oneK)).start(48_000, 2, PCM16, "B")
        assertEquals(fresh.preGainDb, p.preGainDb, 1e-4f)
        val input = sine(1000.0, 0.1, 4800, 2).toShorts()
        val warm = p.runShorts(input)
        val cold = fresh.runShorts(input)
        assertTrue(maxStep(warm, 2, 0, warm.size) > 0)
        assertEquals(rms(cold, 2400, cold.size), rms(warm, 2400, warm.size), 1e-3)
    }

    @Test
    fun aSeekStillStartsTheFiltersAtRest() {
        val p = EqualiserAudioProcessor(FakeEqSource("A" to bass, "B" to oneK)).start(48_000, 2, PCM16, "A")
        p.runShorts(sine(55.0, 0.5, 4800, 2).toShorts())
        // No end of stream first: a seek. The new curve applies at once, with no glide.
        p.flush(item("B"))
        assertEquals(oneK, p.appliedCurve)
        val silent = p.runShorts(ShortArray(4096))
        assertTrue("filters rang on after a seek", silent.all { it.toInt() == 0 })
    }

    @Test
    fun appliesTheCurvesGainAtABandCentre() {
        for (channels in intArrayOf(1, 2, 6)) {
            val p = EqualiserAudioProcessor(FakeEqSource("A" to oneK)).start(48_000, channels, PCM_FLOAT, "A")
            val frames = 48_000
            val out = p.runFloats(sine(1000.0, 0.1, frames, channels).toFloats())
            val gainDb = 20 * log10(rms(out, frames * channels / 2, frames * channels) / (0.1 / sqrt(2.0)))
            assertEquals("$channels channels", 6.0 + p.preGainDb, gainDb, 0.1)
        }
    }

    @Test
    fun flatAndMissingCurvesAreBitExact() {
        val random = Random(7)
        val quiet = ShortArray(48_000 * 2) { random.nextInt(-3000, 3000).toShort() }
        for (curve in listOf(null, EqCurve.FLAT)) {
            val p = EqualiserAudioProcessor(FakeEqSource("A" to curve)).start(48_000, 2, PCM16, "A")
            assertArrayEquals(quiet, p.runShorts(quiet))
        }
        val loud = FloatArray(48_000 * 2) { random.nextFloat() * 2 - 1 }
        val p = EqualiserAudioProcessor(FakeEqSource("A" to null)).start(44_100, 2, PCM_FLOAT, "A")
        assertArrayEquals(loud, p.runFloats(loud), 0f)
    }

    @Test
    fun returnsToBitExactAfterGlidingBackToFlat() {
        val source = FakeEqSource("A" to bass)
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        val random = Random(3)
        val quiet = ShortArray(48_000 * 2) { random.nextInt(-3000, 3000).toShort() }
        p.runShorts(quiet)
        source.set("A", null)
        p.runShorts(quiet) // the glide and the filters' ringing
        p.runShorts(ShortArray(48_000 * 2))
        assertArrayEquals(quiet, p.runShorts(quiet))
    }

    @Test
    fun aMidSongChangeGlidesWithoutAClick() {
        val source = FakeEqSource("A" to EqCurve.FLAT)
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        val frames = 48_000
        val input = sine(1000.0, 0.5, frames).toShorts()
        // +12 dB at 250 Hz: the pre-gain then takes about 12 dB off the 1 kHz tone, a large, fast change.
        val boost = EqCurve.of(0f, 0f, 0f, 12f, 0f, 0f, 0f, 0f, 0f, 0f)
        val changeAt = 10 // buffers of 1024 frames
        val out = p.runShorts(input) { if (it == changeAt) source.set("A", boost) }
        assertEquals(boost, p.appliedCurve)
        val at = changeAt * 1024 * 2
        val before = maxStep(out, 2, 0, at)
        val during = maxStep(out, 2, at, at + 2 * 9600)
        val after = maxStep(out, 2, out.size - 2 * 9600, out.size)
        // A sine's largest sample-to-sample step is amplitude · 2π f / fs. The glide only moves between
        // the two levels, so no step may exceed the louder one's by more than a hair; a click would.
        assertTrue("before $before during $during after $after", during <= max(before, after) * 1.02 + 2)
        assertTrue("before $before after $after", after < before / 2)
        // It does finish: the second half is the new curve's response at 1 kHz plus the pre-gain.
        val design = EqDesign.forRate(48_000)
        val expected = design.responseDb(design.filterGainsDb(boost), 1000.0) + p.preGainDb
        val gain = 20 * log10(rms(out, out.size / 2, out.size) / rms(input, out.size / 2, out.size))
        assertEquals(expected, gain, 0.1)
    }

    @Test
    fun draggingASliderIsSmooth() {
        val source = FakeEqSource("A" to EqCurve.FLAT)
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        val input = sine(1000.0, 0.3, 48_000 * 2).toShorts()
        // A drag from -12 to +12 dB at 1 kHz over a second, one step per buffer, then back down in one jump.
        val out = p.runShorts(input) { buffer ->
            val g = when {
                buffer < 48 -> -12f + buffer * 0.5f
                buffer == 60 -> -12f
                else -> null
            }
            if (g != null) source.set("A", EqCurve.FLAT.with(5, g))
        }
        val expected = 0.3 * 32768 * 2 * PI * 1000 / 48_000
        val worst = maxStep(out, 2, 0, out.size)
        // Never more than the steepest a 1 kHz sine at this level can be with the pre-gain at 0 dB.
        assertTrue("worst step $worst, sine slope $expected", worst <= expected * 1.02)
    }

    @Test
    fun boostedFullScaleSineNeverPassesTheCeiling16Bit() {
        val curves = listOf(
            EqCurve.of(12f, 12f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            EqCurve.of(12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f, 12f),
            EqCurve.of(12f, -12f, 12f, -12f, 12f, -12f, 12f, -12f, 12f, -12f),
        )
        for (curve in curves) for (hz in doubleArrayOf(62.5, 125.0, 1000.0)) {
            val p = EqualiserAudioProcessor(FakeEqSource("A" to curve)).start(48_000, 2, PCM16, "A")
            val input = ShortArray(48_000 * 2) { i -> (if (kotlin.math.sin(2 * PI * hz * (i / 2) / 48_000) >= 0) 32767 else -32768).toShort() }
            val sineIn = sine(hz, 1.0, 48_000).toShorts()
            val out = p.runShorts(sineIn)
            assertTrue("$curve $hz Hz sine peak ${out.maxOf { abs(it.toInt()) }}", out.maxOf { abs(it.toInt()) } <= ceiling16)
            // No wraparound: a wrapped sample would jump most of full scale between neighbours, which a
            // sine this low never does.
            val step = maxStep(out, 2, 0, out.size)
            assertTrue("$curve $hz Hz step $step", step < 32768 * 2 * PI * hz / 48_000 * 1.5 + 64)
            // A full-scale square wave rings past full scale in every filter; the limiter still holds it.
            val square = p.runShorts(input)
            assertTrue("$curve $hz Hz square peak ${square.maxOf { abs(it.toInt()) }}", square.maxOf { abs(it.toInt()) } <= ceiling16)
        }
    }

    @Test
    fun boostedFloatNeverPassesTheCeilingEvenFromOverFullScale() {
        val p = EqualiserAudioProcessor(FakeEqSource("A" to EqCurve.of(12f, 12f, 12f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))).start(44_100, 2, PCM_FLOAT, "A")
        val out = p.runFloats(sine(80.0, 1.5, 44_100, rate = 44_100).toFloats())
        val peak = out.maxOf { abs(it) }
        assertTrue("peak $peak", peak <= EqualiserAudioProcessor.CEILING.toFloat() + 1e-6f)
    }

    @Test
    fun quietMusicUnderABoostIsNotLimited() {
        // A -20 dBFS sine at a +6 dB band, after pre-gain, sits far under the ceiling: the limiter leaves it alone.
        val p = EqualiserAudioProcessor(FakeEqSource("A" to oneK)).start(48_000, 2, PCM_FLOAT, "A")
        val out = p.runFloats(sine(1000.0, 0.1, 48_000).toFloats())
        val gain = 20 * log10(rms(out, 48_000, 96_000) / (0.1 / sqrt(2.0)))
        assertEquals(6.0 + p.preGainDb, gain, 0.05)
    }

    @Test
    fun otherEncodingsAreLeftAlone() {
        val p = EqualiserAudioProcessor(FakeEqSource())
        p.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_24BIT))
        assertFalse(p.isActive)
    }

    @Test
    fun aSourceThatThrowsLeavesAudioAlone() {
        val source = object : EqSource {
            override val version = 0L
            override fun curveFor(timeline: androidx.media3.common.Timeline, periodUid: Any?): EqCurve? = error("boom")
        }
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        assertNull(p.appliedCurve)
        val x = ShortArray(512) { it.toShort() }
        assertArrayEquals(x, p.runShorts(x))
    }

    @Test
    fun anUnchangedVersionDoesNotAskAgain() {
        val source = FakeEqSource("A" to bass)
        val p = EqualiserAudioProcessor(source).start(48_000, 2, PCM16, "A")
        val calls = source.calls
        p.runShorts(ShortArray(48_000))
        assertEquals(calls, source.calls)
        source.version++
        p.runShorts(ShortArray(2048))
        assertEquals(calls + 1, source.calls)
    }

    @Test
    fun logsSwitchesAndLevelsWhenAsked() {
        val lines = ArrayList<String>()
        val logger = object : EqLogger {
            override val enabled = true
            override fun log(message: String) {
                lines += message
            }
        }
        val p = EqualiserAudioProcessor(FakeEqSource("A" to bass), logger).start(48_000, 2, PCM16, "A")
        p.runShorts(sine(60.0, 0.1, 48_000 * 5 / 2).toShorts())
        assertTrue(lines.first(), lines.first().startsWith("item A: curve "))
        val rms = lines.filter { it.startsWith("rms ") }
        assertEquals(2, rms.size)
        assertTrue(rms[0], rms[0].contains("out") && rms[0].contains("bass"))
    }

    @Test
    fun theDefaultLoggerIsQuietInUnitTests() {
        assertFalse(EqLogger.Android.enabled)
    }

    /** Why the filters run in double: the 31 Hz band in float drifts far above the 16-bit floor. */
    @Test
    fun doublePrecisionAt31Hz() {
        val rate = 48_000
        val f = Biquad.peaking(31.25, 12.0, EqDesign.PEAKING_Q, rate)
        val random = Random(1)
        val n = rate * 2
        val x = DoubleArray(n) { random.nextDouble(-0.1, 0.1) + 0.3 * kotlin.math.sin(2 * PI * 40 * it / rate) }
        val ref = DoubleArray(n)
        // Reference: the same filter in direct form I with double state, a different structure.
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        for (i in 0 until n) {
            val y = f.b0 * x[i] + f.b1 * x1 + f.b2 * x2 - f.a1 * y1 - f.a2 * y2
            x2 = x1; x1 = x[i]; y2 = y1; y1 = y
            ref[i] = y
        }
        fun tdf2Float(): DoubleArray {
            val b0 = f.b0.toFloat(); val b1 = f.b1.toFloat(); val b2 = f.b2.toFloat(); val a1 = f.a1.toFloat(); val a2 = f.a2.toFloat()
            var s1 = 0f; var s2 = 0f
            return DoubleArray(n) {
                val xi = x[it].toFloat()
                val y = b0 * xi + s1
                s1 = b1 * xi - a1 * y + s2
                s2 = b2 * xi - a2 * y
                y.toDouble()
            }
        }
        fun errorDb(y: DoubleArray): Double {
            var s = 0.0
            for (i in 0 until n) s += (y[i] - ref[i]).pow(2)
            return 10 * log10(max(s / n, 1e-40))
        }
        val tdf2Double = DoubleArray(n).also {
            var s1 = 0.0; var s2 = 0.0
            for (i in 0 until n) {
                val y = f.b0 * x[i] + s1
                s1 = f.b1 * x[i] - f.a1 * y + s2
                s2 = f.b2 * x[i] - f.a2 * y
                it[i] = y
            }
        }
        val floatErr = errorDb(tdf2Float())
        val doubleErr = errorDb(tdf2Double)
        EqBench.report("precision", listOf(String.format(Locale.ROOT, "31.25 Hz +12 dB peaking at 48 kHz, TDF-II error vs double DF-I: float %.1f dBFS RMS, double %.1f dBFS RMS", floatErr, doubleErr)))
        assertTrue(doubleErr < -150)
        assertTrue(floatErr > doubleErr + 60)
    }
}
