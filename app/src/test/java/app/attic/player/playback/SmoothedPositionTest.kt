package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothedPositionTest {
    @Test
    fun aHeldReadingIsCarriedForwardByTheClock() {
        val p = SmoothedPosition()
        assertEquals(10_000L, p.add(0, 10_000))
        // The player still reports 10 000 a quarter of a second later: it is really at 10 250.
        assertEquals(10_250L, p.add(250, 10_000))
        assertEquals(10_300L, p.add(300, 10_300))
    }

    @Test
    fun oldReadingsLeaveTheWindow() {
        val p = SmoothedPosition(windowMs = 600)
        p.add(0, 10_500) // a reading that ran ahead
        assertEquals(11_000L, p.add(500, 10_900))
        // At 1.2 s both earlier readings are more than 600 ms old.
        assertEquals(11_200L, p.add(1_200, 11_200))
    }

    @Test
    fun resetForgetsReadingsFromBeforeASeek() {
        val p = SmoothedPosition()
        p.add(0, 50_000)
        p.reset()
        assertEquals(1_100L, p.add(100, 1_100))
    }

    private class Row(val clockMs: Long, val tailMinusClock: Long, val mainMinusClock: Long)

    private fun row(clockMs: Int, tailMinusClock: Int, mainMinusClock: Int) = Row(clockMs.toLong(), tailMinusClock.toLong(), mainMinusClock.toLong())

    /**
     * Readings from the owner's phone, 1.5 s of a tail being lined up, as position minus clock for each
     * player. Each player's position held still for about 250 ms and then jumped, so the raw difference
     * swung by more than 200 ms; the smoothed one stays at the tail's real offset, about 371 ms behind.
     */
    @Test
    fun phoneReadingsGiveOneSteadyDifference() {
        val rows = listOf(
        row(13, -235, -20),
        row(64, -275, -71),
        row(127, -269, -135),
        row(184, -300, 61),
        row(243, -360, 50),
        row(297, -414, -4),
        row(363, -480, -70),
        row(413, -294, -120),
        row(464, -295, 80),
        row(527, -328, 64),
        row(577, -378, 14),
        row(628, -429, -37),
        row(681, -482, -90),
        row(745, -295, -154),
        row(804, -326, 70),
        row(860, -382, 14),
        row(910, -433, -37),
        row(961, -483, -87),
        row(1011, -533, -137),
        row(1061, -303, 83),
        row(1111, -353, 33),
        row(1164, -406, -20),
        row(1231, -473, -87),
        row(1297, -288, -153),
        row(1357, -302, 77),
        row(1413, -358, 21),
        row(1466, -411, -32),
        )
        val tail = SmoothedPosition()
        val main = SmoothedPosition()
        val raw = ArrayList<Long>()
        for (r in rows) {
            val clock = 1_000_000 + r.clockMs
            val tailAt = clock + r.tailMinusClock
            val mainAt = clock + r.mainMinusClock
            raw += tailAt - mainAt
            val difference = tail.add(clock, tailAt) - main.add(clock, mainAt)
            if (r.clockMs >= 700) assertTrue("difference $difference at ${r.clockMs}", difference in -385L..-360L)
        }
        assertTrue("raw spread ${raw.max() - raw.min()}", raw.max() - raw.min() > 200)
    }
}
