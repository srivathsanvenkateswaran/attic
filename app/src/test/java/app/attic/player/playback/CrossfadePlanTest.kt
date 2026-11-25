package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadePlanTest {
    private val song = LevelTrack("a.mp3", "albumA", 3)
    private val other = LevelTrack("b.mp3", "albumB", 1)

    private fun fade(
        seconds: Int = 6,
        current: LevelTrack? = song,
        next: LevelTrack? = other,
        duration: Long = 240_000,
        nextDuration: Long? = 200_000,
        repeatOne: Boolean = false,
        stops: Boolean = false,
    ) = CrossfadePlan.fadeMs(seconds, current, next, duration, nextDuration, repeatOne, stops)

    @Test
    fun fadesForTheChosenLength() = assertEquals(6_000L, fade())

    @Test
    fun offMeansNoFade() = assertEquals(0L, fade(seconds = 0))

    @Test
    fun noNextSongMeansNoFade() = assertEquals(0L, fade(next = null))

    @Test
    fun repeatOneMeansNoFade() = assertEquals(0L, fade(repeatOne = true))

    @Test
    fun sleepTimerAtEndOfSongMeansNoFade() = assertEquals(0L, fade(stops = true))

    @Test
    fun anAlbumInOrderStaysGapless() = assertEquals(0L, fade(next = LevelTrack("a4.mp3", "albumA", 4)))

    @Test
    fun theSameAlbumOutOfOrderStillFades() = assertEquals(6_000L, fade(next = LevelTrack("a7.mp3", "albumA", 7)))

    @Test
    fun aSongFromOutsideTheLibraryStillFades() = assertEquals(6_000L, fade(next = LevelTrack("content://x", null, null)))

    @Test
    fun fadeIsAtMostAThirdOfEitherSong() {
        assertEquals(10_000L, fade(seconds = 12, duration = 30_000))
        assertEquals(5_000L, fade(seconds = 12, nextDuration = 15_000))
    }

    @Test
    fun songsTooShortForASecondOfFadeDoNotFade() = assertEquals(0L, fade(duration = 2_500))

    @Test
    fun unknownLengthMeansNoFade() = assertEquals(0L, fade(duration = 0))

    @Test
    fun waitsUntilEightSecondsBeforeTheFade() {
        // A 4:00 song with a 6 s fade fades at 3:54, so the tail starts at 3:46.
        assertEquals(CrossfadePlan.Step.Wait(226_000 - 100_000), CrossfadePlan.step(100_000, 234_000))
    }

    @Test
    fun armsInsideTheLead() {
        assertEquals(CrossfadePlan.Step.Arm, CrossfadePlan.step(227_000, 234_000))
        assertEquals(CrossfadePlan.Step.Arm, CrossfadePlan.step(232_500, 234_000))
    }

    /** Seeking to just before the end leaves no time to line up, so that song ends without a fade. */
    @Test
    fun seekingCloseToTheEndSkipsTheFade() {
        assertEquals(CrossfadePlan.Step.TooLate, CrossfadePlan.step(233_000, 234_000))
        assertEquals(CrossfadePlan.Step.TooLate, CrossfadePlan.step(239_000, 234_000))
    }

    @Test
    fun curvesKeepThePowerSteady() {
        for (i in 0..10) {
            val p = i / 10f
            val out = CrossfadePlan.outgoingVolume(p)
            val inc = CrossfadePlan.incomingVolume(p)
            assertEquals(1f, out * out + inc * inc, 1e-5f)
        }
        assertEquals(1f, CrossfadePlan.outgoingVolume(0f), 0f)
        assertEquals(0f, CrossfadePlan.incomingVolume(0f), 0f)
        assertEquals(1f, CrossfadePlan.incomingVolume(1.5f), 1e-6f)
    }

    @Test
    fun lengthsAreTwoToTwelveSecondsWithSixTheDefault() {
        assertEquals(listOf(2, 4, 6, 8, 10, 12), TransitionPlanner.lengthChoices)
        assertTrue(TransitionPlanner.DEFAULT_LENGTH_SECONDS in TransitionPlanner.lengthChoices)
    }
}

class TailAlignmentTest {
    @Test
    fun waitsForFifteenReadings() {
        val a = TailAlignment()
        repeat(14) { assertEquals(TailAlignment.Verdict.NeedMore, a.record(5)) }
        assertEquals(TailAlignment.Verdict.Aligned, a.record(5))
    }

    @Test
    fun oneWildReadingDoesNotDecide() {
        val a = TailAlignment()
        listOf(3L, -4L, 250L, 6L).forEach { a.record(it) }
        repeat(10) { a.record(1) }
        assertEquals(TailAlignment.Verdict.Aligned, a.record(2))
    }

    /** Readings from the owner's phone, seek 4 of a failed line-up: the true offset near 25 ms with a third of them about 270 ms out. */
    @Test
    fun aThirdOfTheReadingsBeingOutliersDoesNotDecide() {
        val a = TailAlignment(toleranceMs = 30)
        val readings = listOf(73L, 14, 9, 9, 240, 318, 35, 25, 25, 25, 329, 61, 26, 26, 26)
        readings.dropLast(1).forEach { assertEquals(TailAlignment.Verdict.NeedMore, a.record(it)) }
        assertEquals(TailAlignment.Verdict.Aligned, a.record(readings.last()))
    }

    @Test
    fun aLateTailSeeksFurtherAheadNextTime() {
        val a = TailAlignment()
        val before = a.leadMs
        repeat(14) { a.record(-60) }
        assertEquals(TailAlignment.Verdict.Reseek, a.record(-60))
        assertEquals(before + 60, a.leadMs)
        assertEquals(1_000 + before + 60, a.seekTarget(1_000))
    }

    @Test
    fun anEarlyTailSeeksLessFarAhead() {
        val a = TailAlignment()
        val before = a.leadMs
        repeat(15) { a.record(45) }
        assertEquals(before - 45, a.leadMs)
    }

    @Test
    fun resetDropsHalfASet() {
        val a = TailAlignment()
        repeat(3) { a.record(500) }
        a.reset()
        repeat(14) { assertEquals(TailAlignment.Verdict.NeedMore, a.record(0)) }
        assertEquals(TailAlignment.Verdict.Aligned, a.record(0))
    }
}
