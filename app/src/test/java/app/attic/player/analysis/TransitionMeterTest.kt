package app.attic.player.analysis

import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TransitionMeter] on generated fixtures at 16 kHz: click tracks and a drum loop for tempo and beat
 * phase, silence for the audio's real start and end, fades and a cold ending for the outro. Each case
 * adds a row to a table printed at the end, which is the accuracy the report quotes.
 */
class TransitionMeterTest {
    private val rate = TransitionMeter.SAMPLE_RATE

    // ---- tempo and beats ----

    @Test
    fun clickTracksReadTheirTempoAndPhase() {
        for (bpm in listOf(80.0, 100.0, 120.0, 128.0, 150.0, 174.0)) {
            val firstBeat = 0.137
            val audio = clicks(bpm, seconds = 60.0, firstBeatS = firstBeat, noiseDb = -50.0)
            val result = measure(audio)!!
            checkBeat("clicks $bpm BPM, intro", bpm, firstBeat, result.intro)
            checkBeat("clicks $bpm BPM, outro", bpm, firstBeat, result.outro)
        }
    }

    @Test
    fun aDrumLoopUnderAPadReadsItsTempo() {
        for (bpm in listOf(92.0, 118.0, 126.0, 140.0)) {
            val audio = drumLoop(bpm, seconds = 60.0, firstBeatS = 0.25, seed = bpm.toLong())
            val result = measure(audio)!!
            checkBeat("drum loop $bpm BPM, intro", bpm, 0.25, result.intro)
            checkBeat("drum loop $bpm BPM, outro", bpm, 0.25, result.outro)
        }
    }

    @Test
    fun aLooselyPlayedBeatInNoiseStillReads() {
        // Every hit up to 12 ms early or late, as a drummer plays, over noise at -30 dB.
        val audio = drumLoop(124.0, seconds = 60.0, firstBeatS = 0.4, seed = 11, jitterS = 0.012)
        val noisy = noise(60.0, -30.0, 12)
        for (i in audio.indices) audio[i] += noisy[i]
        val result = measure(audio)!!
        checkBeat("loose drums 124 BPM in noise, intro", 124.0, 0.4, result.intro)
        checkBeat("loose drums 124 BPM in noise, outro", 124.0, 0.4, result.outro)
    }

    @Test
    fun aPadWithNoBeatHasNoConfidentGrid() {
        val audio = pad(seconds = 40.0, seed = 7)
        val result = measure(audio)!!
        val intro = result.intro
        row("pad, no beat", "no grid", intro?.let { "%.1f BPM conf %.2f".format(it.bpm, it.confidence) } ?: "no grid")
        assertTrue("pad confidence ${intro?.confidence}", intro == null || intro.confidence < BeatTracker.MIN_CONFIDENCE)
    }

    // ---- silence and the outro ----

    @Test
    fun leadingAndTrailingSilenceAreFound() {
        val body = drumLoop(120.0, seconds = 40.0, firstBeatS = 0.0, seed = 3)
        val audio = silence(2.5) + body + silence(4.0)
        val result = measure(audio)!!
        row("silence 2.5 s before", "2500 ms", "${result.audioStartMs} ms")
        row("silence 4 s after", "${(2.5 + 40.0) * 1000} ms end", "${result.audioEndMs} ms end")
        assertEquals(2_500.0, result.audioStartMs.toDouble(), 50.0)
        assertEquals(42_500.0, result.audioEndMs.toDouble(), 50.0)
        assertEquals(46_500.0, result.decodedMs.toDouble(), 20.0)
    }

    @Test
    fun aColdEndingHasItsOutroAtTheEnd() {
        val audio = drumLoop(120.0, seconds = 40.0, firstBeatS = 0.0, seed = 4)
        val result = measure(audio)!!
        row("cold ending", "outro at ${result.audioEndMs} ms", "${result.outroStartMs} ms")
        assertTrue("outro ${result.outroStartMs} end ${result.audioEndMs}", result.audioEndMs - result.outroStartMs <= 1_000)
    }

    @Test
    fun aFadeOutStartsTheOutro() {
        // 40 s of music, the last 10 s fading out linearly in amplitude.
        val audio = drumLoop(120.0, seconds = 40.0, firstBeatS = 0.0, seed = 5)
        applyFade(audio, fromS = 30.0, toS = 40.0, fadeIn = false)
        val result = measure(audio)!!
        row("linear fade-out from 30 s", "about 30000 ms", "${result.outroStartMs} ms")
        assertEquals(30_000.0, result.outroStartMs.toDouble(), 2_000.0)
        // The curve near the end falls away.
        val last = EnergyCurve.decode(result.outroEnergy.last())
        val early = EnergyCurve.decode(result.outroEnergy[0])
        assertTrue("curve $early → $last", last < early - 15)
    }

    @Test
    fun aFadeInShowsInTheIntroCurve() {
        val audio = drumLoop(120.0, seconds = 40.0, firstBeatS = 0.0, seed = 6)
        applyFade(audio, fromS = 0.0, toS = 8.0, fadeIn = true)
        val result = measure(audio)!!
        val curve = result.introEnergy.map(EnergyCurve::decode)
        // Fully up by 9 s, well down in the first second.
        row("linear fade-in over 8 s", "rises to 0 dB by 8 s", "%.1f dB at 0.5 s, %.1f dB at 9 s".format(curve[1], curve[18]))
        assertTrue("start ${curve[1]}", curve[1] < -12)
        assertTrue("at 9 s ${curve[18]}", curve[18] > -3)
        assertTrue("start ms ${result.audioStartMs}", result.audioStartMs < 1_500)
    }

    @Test
    fun silenceAloneMeasuresNothing() {
        assertNull(measure(silence(10.0)))
    }

    @Test
    fun energyIsHigherForBusierMusic() {
        val busy = measure(drumLoop(140.0, seconds = 30.0, firstBeatS = 0.0, seed = 8))!!
        val calm = measure(pad(seconds = 30.0, seed = 8))!!
        row("energy, drum loop vs pad", "loop higher", "%.2f vs %.2f".format(busy.energy, calm.energy))
        assertTrue("busy ${busy.energy} calm ${calm.energy}", busy.energy > calm.energy + 0.1)
    }

    @Test
    fun aLongFileKeepsOnlyItsEnds() {
        // Six minutes: the meter keeps the first and last 90 s, and still finds both ends' beats.
        val audio = clicks(128.0, seconds = 360.0, firstBeatS = 0.05, noiseDb = -50.0)
        val result = measure(audio)!!
        checkBeat("six-minute clicks, outro", 128.0, 0.05, result.outro)
        assertEquals(360_000.0, result.decodedMs.toDouble(), 20.0)
    }

    @Test
    fun costPerMinuteOfAudio() {
        val audio = drumLoop(120.0, seconds = 240.0, firstBeatS = 0.0, seed = 9)
        repeat(2) { measure(audio) } // warm up
        val started = System.nanoTime()
        val runs = 3
        repeat(runs) { measure(audio) }
        val perMinuteMs = (System.nanoTime() - started) / 1e6 / runs / 4
        row("JVM cost", "per minute of audio", "%.1f ms".format(perMinuteMs))
        assertTrue(perMinuteMs < 500)
    }

    // ---- key ----

    @Test
    fun keysFromTriadChroma() {
        fun chroma(vararg classes: Int): ByteArray = ByteArray(12 * 40) { i -> if (i % 12 in classes) 200.toByte() else 20 }
        assertEquals(0, KeyEstimate.of(chroma(0, 4, 7))!!.key) // C major
        assertEquals(21, KeyEstimate.of(chroma(9, 0, 4, 2, 11, 5, 7).let { scale -> chromaWeighted(scale, tonic = 9, third = 0) })!!.key) // A minor
        assertEquals(6, KeyEstimate.of(chroma(6, 10, 1))!!.key) // F# major
        assertNull(KeyEstimate.of(ByteArray(0)))
    }

    @Test
    fun camelotNeighbours() {
        assertEquals(0, KeyEstimate.camelotDistance(0, 0))
        assertEquals(1, KeyEstimate.camelotDistance(0, 7)) // C → G
        assertEquals(1, KeyEstimate.camelotDistance(0, 5)) // C → F
        assertEquals(1, KeyEstimate.camelotDistance(0, 21)) // C major → A minor
        assertEquals(6, KeyEstimate.camelotDistance(0, 6)) // C → F#
    }

    /** A minor scale's chroma with its tonic and third stressed, so the minor profile wins. */
    private fun chromaWeighted(scale: ByteArray, tonic: Int, third: Int): ByteArray = ByteArray(scale.size) { i ->
        when (i % 12) {
            tonic -> 255.toByte()
            third -> 230.toByte()
            else -> scale[i]
        }
    }

    // ---- helpers ----

    private fun checkBeat(label: String, bpm: Double, firstBeatS: Double, beat: SongTransitions.Beat?) {
        assertNotNull("$label: no beat", beat)
        beat!!
        // Half or double time is the same grid for lining beats up (TransitionPlanner folds it), so it passes and is reported.
        val heard = listOf(bpm, bpm / 2, bpm * 2).minBy { abs(beat.bpm - it) }
        val tempoError = abs(beat.bpm - heard) / heard * 100
        val period = 60_000.0 / bpm
        val offset = beat.beatMs - firstBeatS * 1000
        val raw = offset - Math.floor(offset / period) * period
        val phaseError = if (raw > period / 2) raw - period else raw
        val octave = if (heard != bpm) " (read at ${if (heard < bpm) "half" else "double"} time)" else ""
        row(label, "%.1f BPM".format(bpm), "%.2f BPM%s, error %.2f %%, phase %+.0f ms, conf %.2f".format(beat.bpm, octave, tempoError, phaseError, beat.confidence))
        assertTrue("$label tempo ${beat.bpm}", tempoError < 1.0)
        assertTrue("$label phase $phaseError", abs(phaseError) <= 20)
        assertTrue("$label confidence ${beat.confidence}", beat.confidence >= BeatTracker.MIN_CONFIDENCE)
    }

    private fun measure(audio: FloatArray): SongTransitions? {
        val meter = TransitionMeter()
        var i = 0
        while (i < audio.size) {
            val end = minOf(audio.size, i + 4_096)
            meter.add(audio.copyOfRange(i, end))
            i = end
        }
        return meter.result()
    }

    private fun silence(seconds: Double) = FloatArray((seconds * rate).toInt())

    private fun noise(seconds: Double, db: Double, seed: Long): FloatArray {
        val random = Random(seed)
        val amp = 10.0.pow(db / 20).toFloat()
        return FloatArray((seconds * rate).toInt()) { (random.nextGaussian() * amp).toFloat() }
    }

    private fun clicks(bpm: Double, seconds: Double, firstBeatS: Double, noiseDb: Double): FloatArray {
        val out = noise(seconds, noiseDb, 1)
        var t = firstBeatS
        while (t < seconds) {
            val at = (t * rate).roundToInt()
            for (i in 0 until 80) if (at + i < out.size) out[at + i] += (0.8 * exp(-i / 20.0) * sin(2 * PI * 1_000 * i / rate)).toFloat()
            t += 60.0 / bpm
        }
        return out
    }

    /**
     * A rock beat over a sustained chord: kick on beats 1 and 3, snare on 2 and 4, a quieter hat on every
     * eighth, and a little noise.
     */
    private fun drumLoop(bpm: Double, seconds: Double, firstBeatS: Double, seed: Long, jitterS: Double = 0.0): FloatArray {
        val out = pad(seconds, seed, level = 0.12)
        val random = Random(seed)
        val beat = 60.0 / bpm
        var t = firstBeatS
        var n = 0
        while (t < seconds) {
            val at = ((t + if (jitterS > 0) (random.nextDouble() * 2 - 1) * jitterS else 0.0) * rate).roundToInt().coerceAtLeast(0)
            if (n % 2 == 0) {
                for (i in 0 until 2_400) {
                    if (at + i >= out.size) break
                    val f = 50 + 70 * exp(-i / 300.0)
                    out[at + i] += (0.7 * exp(-i / 900.0) * sin(2 * PI * f * i / rate)).toFloat()
                }
            } else {
                for (i in 0 until 2_000) {
                    if (at + i >= out.size) break
                    val body = 0.25 * exp(-i / 500.0) * sin(2 * PI * 190 * i / rate)
                    out[at + i] += (body + 0.3 * exp(-i / 400.0) * random.nextGaussian()).toFloat()
                }
            }
            for (half in 0..1) {
                val hat = ((t + half * beat / 2) * rate).roundToInt()
                for (i in 0 until 500) if (hat + i < out.size) out[hat + i] += (0.04 * exp(-i / 100.0) * random.nextGaussian()).toFloat()
            }
            t += beat
            n++
        }
        return out
    }

    /** A slow-moving A minor chord with no attack in it. */
    private fun pad(seconds: Double, seed: Long, level: Double = 0.2): FloatArray {
        val base = noise(seconds, -55.0, seed)
        val notes = listOf(220.0, 261.63, 329.63)
        for (i in base.indices) {
            val s = i.toDouble() / rate
            val swell = 0.8 + 0.2 * sin(2 * PI * 0.05 * s)
            var v = 0.0
            for (f in notes) v += sin(2 * PI * f * s)
            base[i] += (level * swell * v / notes.size).toFloat()
        }
        return base
    }

    private fun applyFade(audio: FloatArray, fromS: Double, toS: Double, fadeIn: Boolean) {
        val from = (fromS * rate).toInt()
        val to = minOf(audio.size, (toS * rate).toInt())
        for (i in from until to) {
            val p = (i - from).toDouble() / (to - from)
            audio[i] *= (if (fadeIn) p else 1 - p).toFloat()
        }
        if (!fadeIn) for (i in to until audio.size) audio[i] = 0f
    }

    private fun row(case: String, expected: String, measured: String) {
        synchronized(rows) { rows += "| $case | $expected | $measured |" }
    }

    companion object {
        private val rows = ArrayList<String>()

        @JvmStatic
        @AfterClass
        fun printTable() {
            println("TRANSITION FIXTURES")
            println("| Fixture | Expected | Measured |")
            println("|---|---|---|")
            rows.forEach(::println)
        }
    }
}
