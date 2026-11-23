package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenTrackerTest {
    private var now = 1_000L
    private val tracker = ListenTracker { now }

    @Test
    fun onlyPlayingTimeCounts() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 10_000
        tracker.playing(false)
        now += 50_000
        tracker.playing(true)
        now += 5_000
        val event = tracker.finish(automatic = false)!!
        assertEquals(15_000L, event.listenedMs)
        assertEquals("a", event.key)
        assertEquals(1_000L, event.startedAt)
    }

    @Test
    fun endingByItselfIsFinished() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 1_000
        assertEquals(ListenTracker.FINISHED, tracker.finish(automatic = true)!!.outcome)
    }

    @Test
    fun movingOnNearTheEndIsFinished() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 185_000
        assertEquals(ListenTracker.FINISHED, tracker.finish(automatic = false)!!.outcome)
    }

    @Test
    fun movingOnEarlyIsSkipped() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 20_000
        assertEquals(ListenTracker.SKIPPED, tracker.finish(automatic = false)!!.outcome)
    }

    @Test
    fun movingOnMidwayIsPartial() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 130_000
        assertEquals(ListenTracker.PARTIAL, tracker.finish(automatic = false)!!.outcome)
    }

    @Test
    fun aSongNeverHeardIsNotLogged() {
        tracker.start("a", 200_000)
        assertNull(tracker.finish(automatic = false))
    }

    @Test
    fun finishingTwiceLogsOnce() {
        tracker.start("a", 200_000)
        tracker.playing(true)
        now += 1_000
        tracker.finish(automatic = false)
        assertNull(tracker.finish(automatic = false))
    }
}
