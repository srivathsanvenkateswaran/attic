package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartShuffleTest {
    @Test
    fun everyThirdSongIsAPick() {
        val mixed = SmartShuffle.interleave(listOf("1", "2", "3", "4", "5", "6"), listOf("a", "b", "c"))
        assertEquals(listOf("1", "2", "a", "3", "4", "b", "5", "6", "c"), mixed)
    }

    @Test
    fun theCollectionCarriesOnWhenThePicksRunOut() {
        val mixed = SmartShuffle.interleave(listOf("1", "2", "3", "4", "5", "6"), listOf("a"))
        assertEquals(listOf("1", "2", "a", "3", "4", "5", "6"), mixed)
    }

    @Test
    fun leftoverPicksGoAtTheEnd() {
        assertEquals(listOf("1", "2", "a", "b", "c"), SmartShuffle.interleave(listOf("1", "2"), listOf("a", "b", "c")))
    }

    @Test
    fun withNoPicksTheQueueIsTheCollection() {
        assertEquals(listOf("1", "2", "3"), SmartShuffle.interleave(listOf("1", "2", "3"), emptyList()))
        assertEquals(listOf("a"), SmartShuffle.interleave(emptyList(), listOf("a")))
    }

    @Test
    fun aboutOneInThreeOverALongPlaylist() {
        val base = (1..60).map { "s$it" }
        val picks = (1..SmartShuffle.pickCount(base.size)).map { "p$it" }
        val mixed = SmartShuffle.interleave(base, picks)
        assertEquals(90, mixed.size)
        assertEquals(30, mixed.count { it.startsWith("p") })
        assertTrue(mixed.chunked(3).all { it.last().startsWith("p") && it.dropLast(1).none { k -> k.startsWith("p") } })
        // Nothing lost, nothing doubled, the collection's order kept.
        assertEquals(base, mixed.filter { it.startsWith("s") })
    }

    @Test
    fun picksAreAskedForInProportionAndCapped() {
        assertEquals(0, SmartShuffle.pickCount(0))
        assertEquals(0, SmartShuffle.pickCount(1))
        assertEquals(1, SmartShuffle.pickCount(2))
        assertEquals(5, SmartShuffle.pickCount(11))
        assertEquals(SmartShuffle.MAX_PICKS, SmartShuffle.pickCount(5_000))
    }

    @Test
    fun turningItOnMidSongKeepsThatSongFirst() {
        assertEquals(listOf("now", "1", "2", "a", "3"), SmartShuffle.afterCurrent("now", listOf("1", "2", "3"), listOf("a")))
    }

    @Test
    fun turningItOffRemovesEveryPickButTheOnePlaying() {
        val queue = listOf("a", "x", "b", "y", "c", "x")
        assertEquals(listOf(5, 3), SmartShuffle.removals(queue, currentIndex = 1, picks = setOf("x", "y")))
        assertEquals(listOf(5, 3, 1), SmartShuffle.removals(queue, currentIndex = 0, picks = setOf("x", "y")))
        assertEquals(emptyList<Int>(), SmartShuffle.removals(queue, currentIndex = 0, picks = emptySet()))
    }
}
