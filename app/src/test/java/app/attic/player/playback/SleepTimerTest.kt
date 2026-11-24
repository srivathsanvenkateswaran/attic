package app.attic.player.playback

import app.attic.player.playback.SleepTimer.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    private var now = 1_000_000L
    private val timer = SleepTimer { now }

    @Test
    fun `off does nothing`() {
        assertEquals(Step.Idle, timer.step(songRemainingMs = 5_000))
        assertEquals(SleepChoice.Off, timer.status.choice)
    }

    @Test
    fun `a countdown plays at full volume and wakes when the fade is due`() {
        timer.set(SleepChoice.Minutes15)
        assertEquals(now + 15 * 60_000L, timer.status.endsAtElapsedMs)

        now += 15 * 60_000L - 25_000L
        // 25 s left: full volume, and the next check is when 10 s remain.
        assertEquals(Step.Play(1f, 15_000L), timer.step(null))
    }

    @Test
    fun `a long wait is capped so a late wake-up corrects itself`() {
        timer.set(SleepChoice.Minutes60)
        val step = timer.step(null) as Step.Play
        assertEquals(1f, step.volume)
        assertEquals(SleepTimer.MAX_WAIT_MS, step.nextCheckMs)
    }

    @Test
    fun `the last ten seconds fade the volume down`() {
        timer.set(SleepChoice.Minutes30)
        now += 30 * 60_000L - 10_000L
        assertEquals(Step.Play(1f, SleepTimer.FADE_TICK_MS), timer.step(null))
        now += 5_000L
        assertEquals(Step.Play(0.5f, SleepTimer.FADE_TICK_MS), timer.step(null))
        now += 4_000L
        assertEquals(0.1f, (timer.step(null) as Step.Play).volume, 0.0001f)
    }

    @Test
    fun `time up pauses once and switches the timer off`() {
        timer.set(SleepChoice.Minutes45)
        now += 45 * 60_000L
        assertEquals(Step.Pause, timer.step(null))
        assertEquals(SleepChoice.Off, timer.status.choice)
        assertNull(timer.status.endsAtElapsedMs)
        assertEquals(Step.Idle, timer.step(null))
    }

    @Test
    fun `a check that wakes late still pauses`() {
        timer.set(SleepChoice.Minutes15)
        now += 20 * 60_000L
        assertEquals(Step.Pause, timer.step(null))
    }

    @Test
    fun `remaining time counts down and never goes negative`() {
        timer.set(SleepChoice.Minutes15)
        now += 60_000L
        assertEquals(14 * 60_000L, timer.status.remainingMs(now))
        assertEquals(0L, timer.status.remainingMs(now + 60 * 60_000L))
    }

    @Test
    fun `turning it off clears the end time`() {
        timer.set(SleepChoice.Minutes15)
        timer.set(SleepChoice.Off)
        assertEquals(Step.Idle, timer.step(null))
        assertNull(timer.status.endsAtElapsedMs)
    }

    @Test
    fun `setting a new time replaces the old one`() {
        timer.set(SleepChoice.Minutes60)
        now += 50 * 60_000L
        timer.set(SleepChoice.Minutes15)
        assertEquals(now + 15 * 60_000L, timer.status.endsAtElapsedMs)
    }

    @Test
    fun `end of song fades on the song's last seconds`() {
        timer.set(SleepChoice.EndOfSong)
        assertNull(timer.status.endsAtElapsedMs)
        assertEquals(Step.Play(1f, SleepTimer.SONG_CHECK_MS), timer.step(songRemainingMs = 120_000))
        assertEquals(Step.Play(0.25f, SleepTimer.FADE_TICK_MS), timer.step(songRemainingMs = 2_500))
        assertEquals(Step.Play(0f, SleepTimer.FADE_TICK_MS), timer.step(songRemainingMs = 0))
    }

    @Test
    fun `end of song with an unknown length keeps full volume`() {
        timer.set(SleepChoice.EndOfSong)
        assertEquals(Step.Play(1f, SleepTimer.SONG_CHECK_MS), timer.step(songRemainingMs = null))
    }

    @Test
    fun `end of song pauses when the song ends and only then`() {
        assertFalse(timer.songEnded())
        timer.set(SleepChoice.Minutes15)
        assertFalse(timer.songEnded())
        timer.set(SleepChoice.EndOfSong)
        assertTrue(timer.songEnded())
        assertEquals(SleepChoice.Off, timer.status.choice)
    }

    @Test
    fun `resuming after the countdown ran out switches it off instead of pausing`() {
        timer.set(SleepChoice.Minutes15)
        now += 10 * 60_000L
        assertFalse(timer.resumed())
        now += 10 * 60_000L
        assertTrue(timer.resumed())
        assertEquals(Step.Idle, timer.step(null))
    }

    @Test
    fun `resuming leaves end of song alone`() {
        timer.set(SleepChoice.EndOfSong)
        assertFalse(timer.resumed())
        assertEquals(SleepChoice.EndOfSong, timer.status.choice)
    }

    @Test
    fun `choices map from their minutes`() {
        SleepChoice.entries.forEach { assertEquals(it, SleepChoice.of(it.minutes)) }
        assertEquals(SleepChoice.Off, SleepChoice.of(7))
    }
}
