package app.attic.player.playback

import app.attic.player.analysis.EnergyCurve
import app.attic.player.analysis.SongTransitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TransitionPlannerTest {
    private val song = LevelTrack("a.mp3", "albumA", 3)
    private val other = LevelTrack("b.mp3", "albumB", 1)
    private val duration = 240_000L
    private val nextDuration = 200_000L

    /**
     * A measured song. [outro] gives the level (dB under the reference) at a number of seconds before
     * the audio's end; [intro] at a number of seconds after its start.
     */
    private fun measured(
        length: Long,
        audioStart: Long = 0,
        audioEnd: Long = length,
        outroStart: Long = audioEnd,
        outro: (Double) -> Double = { 0.0 },
        intro: (Double) -> Double = { 0.0 },
        introBeat: SongTransitions.Beat? = null,
        outroBeat: SongTransitions.Beat? = null,
    ) = SongTransitions(
        decodedMs = length,
        audioStartMs = audioStart,
        audioEndMs = audioEnd,
        outroStartMs = outroStart,
        intro = introBeat,
        outro = outroBeat,
        energy = 0.5f,
        introEnergy = ByteArray(EnergyCurve.BLOCKS) { EnergyCurve.encode(intro(it * 0.5 + 0.25)) },
        outroEnergy = ByteArray(EnergyCurve.BLOCKS) { EnergyCurve.encode(outro(30.0 - it * 0.5 - 0.25)) },
    )

    private fun plan(
        mode: TransitionMode = TransitionMode.Smart,
        outgoing: SongTransitions? = measured(duration),
        incoming: SongTransitions? = measured(nextDuration),
        next: LevelTrack? = other,
        repeatOne: Boolean = false,
        stops: Boolean = false,
        seconds: Int = 6,
    ) = TransitionPlanner.plan(mode, seconds, song, next, duration, nextDuration, repeatOne, stops, outgoing, incoming)

    // ---- the fixed fade and when there's no transition at all ----

    @Test
    fun crossfadeIsTheFixedFadeAtTheEnd() {
        assertEquals(Transition(234_000, 6_000, 0, Transition.Kind.Fixed), plan(mode = TransitionMode.Crossfade))
    }

    @Test
    fun offMeansNoTransition() = assertNull(plan(mode = TransitionMode.Off))

    @Test
    fun smartKeepsAnAlbumInOrderGapless() = assertNull(plan(next = LevelTrack("a4.mp3", "albumA", 4)))

    @Test
    fun smartStopsForRepeatOneTheSleepTimerAndTheEndOfTheQueue() {
        assertNull(plan(repeatOne = true))
        assertNull(plan(stops = true))
        assertNull(plan(next = null))
    }

    @Test
    fun smartWithoutAnalysisUsesTheCrossfadeLength() {
        assertEquals(Transition(232_000, 8_000, 0, Transition.Kind.SmartFallback), plan(outgoing = null, seconds = 8))
    }

    @Test
    fun smartDistrustsAnalysisOfADifferentLength() {
        // The file changed since it was measured: two seconds out.
        val stale = measured(duration - 2_000)
        assertEquals(Transition.Kind.SmartFallback, plan(outgoing = stale)!!.kind)
    }

    // ---- silence ----

    @Test
    fun trailingSilenceIsNeverPlayed() {
        val outgoing = measured(duration, audioEnd = duration - 5_000)
        val t = plan(outgoing = outgoing)!!
        assertTrue("$t", t.fadeStartMs + t.fadeMs <= duration - 5_000)
    }

    @Test
    fun leadingSilenceOfTheNextSongIsSkipped() {
        val incoming = measured(nextDuration, audioStart = 3_000)
        assertEquals(2_980L, plan(incoming = incoming)!!.incomingStartMs)
    }

    @Test
    fun withoutIncomingAnalysisTheNextSongStartsAtZero() = assertEquals(0L, plan(incoming = null)!!.incomingStartMs)

    // ---- length and start from the curves ----

    /** Full level, then falling 4 dB a second over the last [seconds]. */
    private fun fadeOut(seconds: Double): (Double) -> Double = { before -> if (before >= seconds) 0.0 else -4.0 * (seconds - before) }

    /** Rising from -30 dB to full over [seconds]. */
    private fun buildUp(seconds: Double): (Double) -> Double = { after -> if (after >= seconds) 0.0 else -30.0 * (1 - after / seconds) }

    @Test
    fun aColdEndingIntoALoudStartIsShortAndAtTheEnd() {
        val t = plan()!!
        assertTrue("$t", t.fadeMs <= 2_000)
        assertEquals(duration, t.fadeStartMs + t.fadeMs)
    }

    @Test
    fun aLongFadeOutIntoASlowBuildOverlapsLong() {
        val outgoing = measured(duration, outroStart = duration - 10_000, outro = fadeOut(10.0))
        val incoming = measured(nextDuration, intro = buildUp(8.0))
        val t = plan(outgoing = outgoing, incoming = incoming)!!
        assertTrue("$t", t.fadeMs >= 6_000)
        assertTrue("$t", t.fadeStartMs in duration - 12_000..duration - 7_000)
    }

    @Test
    fun aFadeOutIntoALoudStartSkipsTheQuietTail() {
        val outgoing = measured(duration, outroStart = duration - 10_000, outro = fadeOut(10.0))
        val t = plan(outgoing = outgoing)!!
        // The next song comes in before the tail has gone quiet: no seconds of near-silence first.
        assertTrue("$t", t.fadeStartMs + t.fadeMs <= duration - 4_000)
    }

    @Test
    fun aColdEndingIntoAQuietIntroLetsTheIntroPlay() {
        val incoming = measured(nextDuration, intro = buildUp(10.0))
        val t = plan(incoming = incoming)!!
        assertTrue("$t", t.fadeMs <= 2_000)
        assertEquals(duration, t.fadeStartMs + t.fadeMs)
    }

    @Test
    fun anOverlapIsAtMostAThirdOfEitherSong() {
        val outgoing = measured(duration, outroStart = duration - 20_000, outro = fadeOut(20.0))
        val incoming = measured(15_000, intro = buildUp(10.0))
        val t = TransitionPlanner.plan(TransitionMode.Smart, 6, song, other, duration, 15_000, false, false, outgoing, incoming)!!
        assertTrue("$t", t.fadeMs <= 5_000)
    }

    // ---- beats ----

    private fun beat(bpm: Double, at: Long, confidence: Float = 0.9f) = SongTransitions.Beat(bpm, at, confidence)

    private fun onBeat(position: Double, beat: SongTransitions.Beat): Double {
        val period = beat.periodMs
        val raw = ((position - beat.beatMs) % period + period) % period
        return minOf(raw, period - raw)
    }

    /**
     * Two beat songs. By default a 10 s fade-out into an 8 s build, which wants a long blend; with
     * [short], a 3 s fade-out into a punchy start, which wants a blend of a couple of seconds.
     */
    private fun aligned(
        outBpm: Double,
        inBpm: Double,
        inConfidence: Float = 0.9f,
        short: Boolean = false,
    ): Pair<Transition, Pair<SongTransitions.Beat, SongTransitions.Beat>> {
        val outBeat = beat(outBpm, duration - 10_123)
        val inBeat = beat(inBpm, 2_210, inConfidence)
        val outgoing = if (short) {
            measured(duration, outroStart = duration - 3_000, outro = fadeOut(3.0), outroBeat = outBeat)
        } else {
            measured(duration, outroStart = duration - 10_000, outro = fadeOut(10.0), outroBeat = outBeat)
        }
        val incoming = measured(nextDuration, audioStart = 2_000, intro = if (short) buildUp(1.5) else buildUp(8.0), introBeat = inBeat)
        return plan(outgoing = outgoing, incoming = incoming)!! to (outBeat to inBeat)
    }

    @Test
    fun matchingTemposPutABeatOfEachOnTheMiddle() {
        val (t, beats) = aligned(120.0, 120.0)
        assertTrue("$t", t.beatAligned)
        val middle = t.fadeMs / 2.0
        assertEquals(0.0, onBeat(t.fadeStartMs + middle, beats.first), 1.0)
        assertEquals(0.0, onBeat(t.incomingStartMs + middle, beats.second), 1.0)
        assertTrue("$t", t.fadeMs >= 6_000)
        assertEquals(0L, t.expectedDriftMs)
    }

    @Test
    fun aThreePercentGapIsAlignedWithinTheDriftBudget() {
        val (t, beats) = aligned(120.0, 123.6, short = true)
        assertTrue("$t", t.beatAligned)
        assertTrue("$t", t.fadeMs in TransitionPlanner.MIN_ALIGNED_MS..2_667)
        assertTrue("$t", t.expectedDriftMs <= TransitionPlanner.DRIFT_BUDGET_MS)
        assertEquals(0.0, onBeat(t.fadeStartMs + t.fadeMs / 2.0, beats.first), 1.0)
        assertEquals(0.0, onBeat(t.incomingStartMs + t.fadeMs / 2.0, beats.second), 1.0)
    }

    @Test
    fun aLongBlendIsNotGivenUpForBeatsThatWouldDrift() {
        // 3 % apart, the beats could only stay together for 2.7 s; the 8 s blend sounds better, unaligned.
        val (t, _) = aligned(120.0, 123.6)
        assertFalse("$t", t.beatAligned)
        assertTrue("$t", t.fadeMs >= 6_000)
    }

    @Test
    fun halfTimeCountsAsTheSameTempo() {
        val (t, beats) = aligned(128.0, 64.2)
        assertTrue("$t", t.beatAligned)
        assertEquals(0.0, onBeat(t.incomingStartMs + t.fadeMs / 2.0, beats.second), 1.0)
    }

    @Test
    fun temposTooFarApartStayUnaligned() {
        assertFalse(aligned(120.0, 128.4, short = true).first.beatAligned) // 7 %
        // 5 %: the drift budget leaves 1.6 s, room for a 1.5 s overlap; 5.5 % leaves under the 1.5 s floor.
        assertTrue(aligned(120.0, 126.0, short = true).first.beatAligned)
        assertFalse(aligned(120.0, 126.6, short = true).first.beatAligned)
    }

    @Test
    fun anUnsureBeatIsNotLinedUp() = assertFalse(aligned(120.0, 120.0, inConfidence = 0.2f).first.beatAligned)

    @Test
    fun alignmentNeverPlaysTrailingSilenceOrStartsBeforeTheSong() {
        val (t, _) = aligned(120.0, 120.0)
        assertTrue("$t", t.fadeStartMs + t.fadeMs <= duration)
        assertTrue("$t", t.incomingStartMs >= 0)
    }

    // ---- determinism and cost ----

    @Test
    fun theSameSongsGiveTheSamePlan() {
        val first = aligned(124.0, 125.0).first
        repeat(5) { assertEquals(first, aligned(124.0, 125.0).first) }
    }

    @Test
    fun planningTakesWellUnderAMillisecond() {
        val outgoing = measured(duration, outroStart = duration - 20_000, outro = fadeOut(20.0), outroBeat = beat(124.0, duration - 20_000))
        val incoming = measured(nextDuration, intro = buildUp(8.0), introBeat = beat(124.0, 100))
        repeat(200) { plan(outgoing = outgoing, incoming = incoming) }
        val runs = 1_000
        val started = System.nanoTime()
        repeat(runs) { assertNotNull(plan(outgoing = outgoing, incoming = incoming)) }
        val micros = (System.nanoTime() - started) / 1_000.0 / runs
        println("TRANSITION PLANNING: %.0f µs a plan on the JVM".format(micros))
        assertTrue("$micros µs", micros < 1_000)
    }

    /** Prints what each case plans, for the report. */
    @Test
    fun printThePlans() {
        val fadeOutSong = measured(duration, outroStart = duration - 10_000, outro = fadeOut(10.0))
        val cases = listOf(
            "cold ending → loud start" to plan(),
            "10 s fade-out → loud start" to plan(outgoing = fadeOutSong),
            "10 s fade-out → 8 s build" to plan(outgoing = fadeOutSong, incoming = measured(nextDuration, intro = buildUp(8.0))),
            "cold ending → 10 s build" to plan(incoming = measured(nextDuration, intro = buildUp(10.0))),
            "5 s trailing silence → 3 s leading silence" to plan(
                outgoing = measured(duration, audioEnd = duration - 5_000),
                incoming = measured(nextDuration, audioStart = 3_000),
            ),
            "fade-out → build, 120 → 120 BPM" to aligned(120.0, 120.0).first,
            "fade-out → build, 120 → 123.6 BPM" to aligned(120.0, 123.6).first,
            "fade-out → build, 128 → 64.2 BPM" to aligned(128.0, 64.2).first,
            "fade-out → build, 120 → 128.4 BPM" to aligned(120.0, 128.4).first,
            "3 s fade-out → punchy start, 120 → 120 BPM" to aligned(120.0, 120.0, short = true).first,
            "3 s fade-out → punchy start, 120 → 123.6 BPM" to aligned(120.0, 123.6, short = true).first,
            "3 s fade-out → punchy start, 120 → 126 BPM" to aligned(120.0, 126.0, short = true).first,
        )
        println("TRANSITION PLANS (songs 4:00 → 3:20)")
        println("| Case | Fade starts | Length | Enters next at | Beats |")
        println("|---|---|---|---|---|")
        for ((name, planned) in cases) {
            val t = planned!!
            val beats = if (t.beatAligned) "aligned, drift ≤ ${t.expectedDriftMs} ms" else "not aligned"
            println("| $name | ${(duration - t.fadeStartMs) / 1000.0} s before the end | ${t.fadeMs / 1000.0} s | ${t.incomingStartMs / 1000.0} s | $beats |")
        }
    }

    @Test
    fun foldedTempoRatios() {
        assertEquals(1.0, TransitionPlanner.tempoRatio(beat(120.0, 0), beat(60.0, 0))!!, 1e-9)
        assertEquals(1.0, TransitionPlanner.tempoRatio(beat(87.0, 0), beat(174.0, 0))!!, 1e-9)
        assertTrue(abs(TransitionPlanner.tempoRatio(beat(120.0, 0), beat(126.0, 0))!! - 1.05) < 1e-9)
        assertNull(TransitionPlanner.tempoRatio(beat(120.0, 0, 0.1f), beat(120.0, 0)))
    }
}
