package app.attic.player.home

import app.attic.player.home.HomeShelves.Listen
import app.attic.player.home.HomeShelves.Played
import app.attic.player.home.HomeShelves.Song
import app.attic.player.playback.ListenTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeShelvesTest {
    /** The fixed clock: 23 November 2026, 12:00 UTC. */
    private val now = 1_795_435_200_000L
    private val day = 86_400_000L

    private fun daysAgo(days: Int) = now - days * day
    private fun daysAgoSec(days: Int) = daysAgo(days) / 1000

    private fun library(vararg keys: String, addedDaysAgo: Int = 400) = keys.map { Song(it, daysAgoSec(addedDaysAgo)) }

    private fun listens(key: String, times: Int, daysAgo: Int = 1, outcome: String = ListenTracker.FINISHED) =
        List(times) { Listen(key, daysAgo(daysAgo) + it, outcome) }

    // On repeat

    @Test
    fun onRepeatRanksTheMostPlayedOfTheLastThirtyDays() {
        val keys = setOf("a", "b", "c", "d", "e")
        val rows = listens("a", 5) + listens("b", 9) + listens("c", 3) + listens("d", 2) + listens("e", 4)
        assertEquals(listOf("b", "a", "e", "c", "d"), HomeShelves.onRepeat(rows, keys, now))
    }

    @Test
    fun onRepeatIgnoresPlaysOlderThanThirtyDays() {
        val keys = setOf("a", "b", "c", "d", "old")
        val rows = listens("a", 2) + listens("b", 2) + listens("c", 2) + listens("d", 2) + listens("old", 20, daysAgo = 31)
        assertTrue("old" !in HomeShelves.onRepeat(rows, keys, now))
    }

    @Test
    fun skipsAndRemovalsAreNotPlays() {
        val keys = setOf("a", "b", "c", "d", "skipped")
        val rows = listens("a", 2) + listens("b", 2) + listens("c", 2) + listens("d", 2) +
            listens("skipped", 6, outcome = ListenTracker.SKIPPED) + listens("skipped", 6, outcome = ListenTracker.REMOVED)
        assertTrue("skipped" !in HomeShelves.onRepeat(rows, keys, now))
    }

    @Test
    fun oneListenIsNotOnRepeat() {
        val keys = setOf("a", "b", "c", "d", "once")
        val rows = listens("a", 2) + listens("b", 2) + listens("c", 2) + listens("d", 2) + listens("once", 1)
        assertTrue("once" !in HomeShelves.onRepeat(rows, keys, now))
    }

    @Test
    fun aTieGoesToTheSongHeardMostRecently() {
        val keys = setOf("a", "b", "c", "d")
        val rows = listens("a", 2, daysAgo = 5) + listens("b", 2, daysAgo = 1) + listens("c", 2, daysAgo = 3) + listens("d", 2, daysAgo = 2)
        assertEquals(listOf("b", "d", "c", "a"), HomeShelves.onRepeat(rows, keys, now))
    }

    @Test
    fun onRepeatHidesBelowFourSongs() {
        val rows = listens("a", 5) + listens("b", 5) + listens("c", 5)
        assertEquals(emptyList<String>(), HomeShelves.onRepeat(rows, setOf("a", "b", "c"), now))
    }

    @Test
    fun songsThatLeftTheLibraryAreLeftOut() {
        val rows = listens("a", 5) + listens("b", 5) + listens("c", 5) + listens("d", 5) + listens("gone", 50)
        assertEquals(listOf("a", "b", "c", "d"), HomeShelves.onRepeat(rows, setOf("a", "b", "c", "d"), now))
    }

    @Test
    fun onRepeatStopsAtTwenty() {
        val keys = (1..30).map { "s$it" }
        val rows = keys.flatMap { listens(it, 3) }
        assertEquals(HomeShelves.MAX_SONGS, HomeShelves.onRepeat(rows, keys.toSet(), now).size)
    }

    // Haven't heard in a while

    private val oldPlays = listOf(
        Played("often", daysAgo(90), 12),
        Played("sometimes", daysAgo(70), 4),
        Played("rarely", daysAgo(200), 1),
        Played("lately", daysAgo(10), 30),
        Played("justUnder", daysAgo(59), 8),
        Played("justOver", daysAgo(61), 3),
    )

    @Test
    fun notLatelyIsOftenPlayedSongsWithNoPlayForSixtyDays() {
        val played = oldPlays + Played("more", daysAgo(100), 3)
        val keys = played.map { it.key }.toSet()
        // Equal plays: the one unheard for longer comes first.
        assertEquals(listOf("often", "sometimes", "more", "justOver"), HomeShelves.notLately(played, emptyList(), emptyMap(), keys, now))
    }

    @Test
    fun notLatelyHidesBelowFourSongs() {
        val keys = oldPlays.map { it.key }.toSet()
        assertEquals(emptyList<String>(), HomeShelves.notLately(oldPlays, emptyList(), emptyMap(), keys, now))
    }

    @Test
    fun favouritesCountEvenWhenRarelyPlayed() {
        val keys = setOf("often", "sometimes", "rarely", "justOver", "lately")
        val liked = mapOf("rarely" to daysAgo(300), "lately" to daysAgo(300))
        val shelf = HomeShelves.notLately(oldPlays, emptyList(), liked, keys, now)
        assertTrue("rarely" in shelf)
        assertTrue("lately" !in shelf)
    }

    @Test
    fun aFavouriteNeverPlayedCountsFromWhenItWasLiked() {
        val keys = setOf("often", "sometimes", "justOver", "likedLongAgo", "likedLastWeek")
        val liked = mapOf("likedLongAgo" to daysAgo(120), "likedLastWeek" to daysAgo(7))
        val shelf = HomeShelves.notLately(oldPlays, emptyList(), liked, keys, now)
        assertTrue("likedLongAgo" in shelf)
        assertTrue("likedLastWeek" !in shelf)
    }

    @Test
    fun aListenInTheLogKeepsASongOffTheShelf() {
        val played = oldPlays + Played("more", daysAgo(100), 3)
        val keys = setOf("often", "sometimes", "justOver", "rarely", "more")
        val liked = mapOf("rarely" to daysAgo(300))
        val shelf = HomeShelves.notLately(played, listens("often", 1, daysAgo = 2), liked, keys, now)
        assertEquals(listOf("rarely", "sometimes", "more", "justOver"), shelf)
    }

    @Test
    fun notLatelyRanksByPlaysWithAFavouriteWorthThree() {
        val played = listOf(
            Played("a", daysAgo(90), 10),
            Played("b", daysAgo(90), 8),
            Played("c", daysAgo(90), 5),
            Played("d", daysAgo(90), 4),
        )
        val shelf = HomeShelves.notLately(played, emptyList(), mapOf("c" to daysAgo(400)), setOf("a", "b", "c", "d"), now)
        assertEquals(listOf("a", "b", "c", "d"), shelf)
        val boosted = HomeShelves.notLately(played, emptyList(), mapOf("d" to daysAgo(400)), setOf("a", "b", "c", "d"), now)
        assertEquals(listOf("a", "b", "d", "c"), boosted)
    }

    // Newly added

    @Test
    fun newlyAddedIsTheLastThirtyDaysNewestFirst() {
        val songs = library("old1", "old2", addedDaysAgo = 200) +
            listOf(Song("n1", daysAgoSec(1)), Song("n5", daysAgoSec(5)), Song("n29", daysAgoSec(29)), Song("n3", daysAgoSec(3)), Song("n31", daysAgoSec(31)))
        assertEquals(listOf("n1", "n3", "n5", "n29"), HomeShelves.newlyAdded(songs, now))
    }

    @Test
    fun newlyAddedHidesWhenTheWholeLibraryIsNew() {
        val songs = (1..10).map { Song("s$it", daysAgoSec(it)) }
        assertEquals(emptyList<String>(), HomeShelves.newlyAdded(songs, now))
    }

    @Test
    fun newlyAddedHidesBelowFourSongs() {
        val songs = library("old1", "old2") + listOf(Song("n1", daysAgoSec(1)), Song("n2", daysAgoSec(2)))
        assertEquals(emptyList<String>(), HomeShelves.newlyAdded(songs, now))
    }

    // All three

    @Test
    fun buildFillsEachShelfFromItsOwnSignal() {
        val songs = library("a", "b", "c", "d", "often", "sometimes", "justOver", "more") +
            listOf(Song("n1", daysAgoSec(1)), Song("n2", daysAgoSec(2)), Song("n3", daysAgoSec(3)), Song("n4", daysAgoSec(4)))
        val played = oldPlays + Played("more", daysAgo(100), 3)
        val rows = listens("a", 3) + listens("b", 3) + listens("c", 3) + listens("d", 3)
        val result = HomeShelves.build(songs, played, rows, emptyMap(), now)
        assertEquals(listOf("a", "b", "c", "d"), result.onRepeat.sorted())
        assertEquals(listOf("often", "sometimes", "more", "justOver"), result.notLately)
        assertEquals(listOf("n1", "n2", "n3", "n4"), result.newlyAdded)
    }

    @Test
    fun anEmptyLogGivesNoListeningShelves() {
        val result = HomeShelves.build(library("a", "b"), emptyList(), emptyList(), emptyMap(), now)
        assertEquals(HomeShelves.Result(), result)
    }
}
