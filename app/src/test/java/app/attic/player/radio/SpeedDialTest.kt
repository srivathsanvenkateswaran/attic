package app.attic.player.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SpeedDialTest {
    private fun song(
        key: String,
        degrees: Double? = null,
        plays: Int = 0,
        skips: Int = 0,
        liked: Boolean = false,
        artist: String? = key,
        album: String? = key,
    ) = SpeedDial.Song(
        key = key,
        vector = degrees?.let { floatArrayOf(cos(Math.toRadians(it)).toFloat(), sin(Math.toRadians(it)).toFloat()) },
        plays = plays,
        skips = skips,
        liked = liked,
        artistKey = artist,
        albumKey = album,
    )

    @Test
    fun theFavouritesComeFirst() {
        val songs = listOf(song("rare", plays = 1), song("most", plays = 9), song("some", plays = 4))
        assertEquals(listOf("most", "some", "rare"), SpeedDial.pick(songs, day = 1, size = 3).map { it.key })
    }

    @Test
    fun aLikeCountsTowardsAFavourite() {
        val songs = listOf(song("played", plays = 1), song("liked", liked = true))
        assertEquals("liked", SpeedDial.pick(songs, day = 1, size = 1).single().key)
    }

    @Test
    fun theRestAreUnplayedSongsThatSoundLikeYourTaste() {
        val favourites = listOf(0.0, 12.0, -12.0, 24.0).mapIndexed { i, angle -> song("f$i", degrees = angle, plays = 5) }
        val songs = favourites + listOf(song("near", degrees = 36.0), song("far", degrees = 170.0))
        val picked = SpeedDial.pick(songs, day = 1, size = 5).map { it.key }
        assertTrue("near" in picked)
        assertFalse("far" in picked)
    }

    @Test
    fun aSongYouKeepSkippingIsLeftOut() {
        val songs = listOf(song("skipped", plays = 3, skips = 5), song("other", plays = 1))
        assertEquals(listOf("other"), SpeedDial.pick(songs, day = 1, size = 2).map { it.key })
    }

    @Test
    fun oneAlbumTakesOneTile() {
        val songs = listOf(song("a1", plays = 9, album = "a"), song("a2", plays = 8, album = "a"), song("b", plays = 1))
        assertEquals(listOf("a1", "b"), SpeedDial.pick(songs, day = 1, size = 3).map { it.key })
    }

    @Test
    fun oneArtistTakesAtMostTwoTiles() {
        val songs = listOf(
            song("x1", plays = 9, artist = "x"),
            song("x2", plays = 8, artist = "x"),
            song("x3", plays = 7, artist = "x"),
            song("y", plays = 1),
        )
        assertEquals(listOf("x1", "x2", "y"), SpeedDial.pick(songs, day = 1, size = 4).map { it.key })
    }

    @Test
    fun twoCopiesOfASongTakeOneTile() {
        val songs = listOf(song("copy1", degrees = 40.0, plays = 9), song("copy2", degrees = 40.5, plays = 8), song("other", degrees = 90.0, plays = 1))
        assertEquals(listOf("copy1", "other"), SpeedDial.pick(songs, day = 1, size = 3).map { it.key })
    }

    @Test
    fun twoLanguageVersionsOfASongTakeOneTile() {
        val songs = listOf(song("aaruyire", degrees = 0.0, plays = 9), song("tere-bina", degrees = 25.0, plays = 8), song("other", degrees = 80.0, plays = 1))
        val picked = SpeedDial.pick(songs, day = 1, size = 3) { a, b -> setOf(a.key, b.key) == setOf("aaruyire", "tere-bina") }
        assertEquals(listOf("aaruyire", "other"), picked.map { it.key })
    }

    @Test
    fun itStaysTheSameWithinADay() {
        val songs = List(60) { song("s$it", degrees = it * 3.0) } + song("played", plays = 2)
        assertEquals(SpeedDial.pick(songs, day = 5, size = 9), SpeedDial.pick(songs, day = 5, size = 9))
    }

    @Test
    fun itChangesFromDayToDay() {
        val songs = List(60) { song("s$it", degrees = it * 3.0) } + song("played", plays = 2)
        val days = (1L..5L).map { day -> SpeedDial.pick(songs, day, size = 9).map { it.key } }
        assertTrue(days.distinct().size > 1)
    }

    @Test
    fun itStillFillsUpWithNoHistoryAndNoAnalysis() {
        val songs = List(20) { song("s$it", degrees = null) }
        assertEquals(9, SpeedDial.pick(songs, day = 1, size = 9).size)
    }

    @Test
    fun aSmallLibraryGivesWhatItHas() {
        assertEquals(2, SpeedDial.pick(listOf(song("a"), song("b")), day = 1, size = 9).size)
    }
}
