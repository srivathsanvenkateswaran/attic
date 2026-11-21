package app.attic.player.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RadioEngineTest {
    /** Songs on a circle, so an angle sets how similar two songs are: cosine of the angle between them. */
    private fun song(
        key: String,
        degrees: Double,
        language: String? = "ta",
        artist: String? = key,
        album: String? = key,
        affinity: Float = 0f,
    ): RadioSong {
        val r = Math.toRadians(degrees)
        return RadioSong(key, floatArrayOf(cos(r).toFloat(), sin(r).toFloat()), language, artist, album, affinity)
    }

    private fun engine(vararg songs: RadioSong, sameSong: (RadioSong, RadioSong) -> Boolean = { _, _ -> false }) =
        RadioEngine(songs.toList(), sameSong)

    private fun RadioEngine.chain(seed: String, n: Int, crossEvery: Int = 1000): List<String> {
        val state = RadioState(seed, crossEvery = crossEvery)
        return buildList {
            repeat(n) {
                val next = next(state) ?: return@buildList
                add(next.key)
                state.played(next.key)
            }
        }
    }

    @Test
    fun picksTheClosestSongFirst() {
        val e = engine(song("seed", 0.0), song("far", 40.0), song("near", 10.0))
        assertEquals(listOf("near", "far"), e.chain("seed", 2))
    }

    @Test
    fun neverRepeatsASong() {
        val e = engine(song("seed", 0.0), song("a", 15.0))
        assertEquals(listOf("a"), e.chain("seed", 5))
    }

    @Test
    fun staysInTheSessionLanguageOutsideTheCrossSlot() {
        val e = engine(song("seed", 0.0), song("hindi", 15.0, "hi"), song("tamil", 30.0))
        assertEquals(listOf("tamil"), e.chain("seed", 1))
    }

    @Test
    fun theCrossSlotPlaysAStrongMatchFromAnotherLanguage() {
        val e = engine(song("seed", 0.0), song("tamil", 30.0), song("hindi", 15.0, "hi"), song("tamil2", 50.0))
        assertEquals(listOf("tamil", "hindi", "tamil2"), e.chain("seed", 3, crossEvery = 2))
    }

    @Test
    fun aWeakOtherLanguageMatchDoesNotGetTheCrossSlot() {
        val e = engine(song("seed", 0.0), song("tamil", 30.0), song("hindi", 80.0, "hi"), song("tamil2", 50.0))
        assertEquals(listOf("tamil", "tamil2"), e.chain("seed", 2, crossEvery = 2))
    }

    @Test
    fun anUnlabelledSongNeverTakesTheCrossSlot() {
        val e = engine(song("seed", 0.0), song("tamil", 30.0), song("unknown", 15.0, null), song("hindi", 20.0, "hi"))
        val state = RadioState("seed", crossEvery = 2)
        state.played("tamil")
        assertEquals("hindi", e.next(state)?.key)
    }

    @Test
    fun anUnlabelledSongCanPlayInASessionSlotBelowALabelledOne() {
        val e = engine(song("seed", 0.0), song("unknown", 15.0, null), song("tamil", -18.0))
        assertEquals(listOf("tamil", "unknown"), e.chain("seed", 2))
    }

    @Test
    fun songsWithoutVocalsFitAnySession() {
        val e = engine(song("seed", 0.0), song("instrumental", 15.0, RadioSong.NO_VOCALS))
        assertEquals(listOf("instrumental"), e.chain("seed", 1))
    }

    @Test
    fun anotherVersionOfAPlayedSongIsSkipped() {
        val e = engine(song("aaruyire", 0.0), song("tere-bina", 20.0, "ta"), song("other", 50.0)) { a, b ->
            setOf(a.key, b.key) == setOf("aaruyire", "tere-bina")
        }
        assertEquals(listOf("other"), e.chain("aaruyire", 2))
    }

    @Test
    fun theSameSongCheckOnlyRunsOnCloseSoundingPairs() {
        val asked = mutableListOf<String>()
        val e = engine(song("seed", 0.0), song("close", 20.0), song("distant", 60.0)) { _, b ->
            asked += b.key
            false
        }
        e.chain("seed", 2)
        assertEquals(listOf("close"), asked)
    }

    @Test
    fun anAlmostIdenticalVectorIsTreatedAsTheSameSongWithoutAsking() {
        val e = engine(song("seed", 0.0), song("copy", 0.5), song("other", 50.0)) { _, _ -> error("should not ask") }
        assertEquals(listOf("other"), e.chain("seed", 1))
    }

    @Test
    fun theSeedComposerGetsABoost() {
        val e = engine(song("seed", 0.0, artist = "rahman"), song("other", 10.0), song("rahman2", 11.0, artist = "rahman"))
        assertEquals("rahman2", e.chain("seed", 1).single())
    }

    @Test
    fun oneArtistCannotTakeMoreThanTwoSlotsInARow() {
        val e = engine(
            song("seed", 0.0, artist = "x"),
            song("x1", 12.0, artist = "x"),
            song("x2", 24.0, artist = "x"),
            song("x3", 36.0, artist = "x"),
            song("y", 48.0, artist = "y"),
        )
        assertEquals(listOf("x1", "y"), e.chain("seed", 2))
    }

    @Test
    fun anAlbumIsCappedAtTwoSongsInASession() {
        val e = engine(
            song("seed", 0.0, album = "a"),
            song("a1", 12.0, album = "a"),
            song("a2", 24.0, album = "a"),
            song("b", 48.0, album = "b"),
        )
        assertEquals(listOf("a1", "b"), e.chain("seed", 2))
    }

    @Test
    fun affinityMovesASongUp() {
        val e = engine(song("seed", 0.0), song("skipped", 10.0, affinity = -0.2f), song("other", 15.0))
        assertEquals("other", e.chain("seed", 1).single())
    }

    @Test
    fun recentlyPlayedSongsAreAvoidedUntilNothingElseIsLeft() {
        val e = engine(song("seed", 0.0), song("recent", 15.0), song("fresh", 40.0))
        val state = RadioState("seed", crossEvery = 1000, recent = setOf("recent"))
        val first = e.next(state)
        assertEquals("fresh", first?.key)
        state.played(first!!.key)
        assertEquals("recent", e.next(state)?.key)
    }

    @Test
    fun theSessionLanguageFollowsWhatIsBeingPlayed() {
        val e = engine(song("seed", 0.0), song("h1", 20.0, "hi"), song("h2", 35.0, "hi"), song("h3", 50.0, "hi"), song("t", 15.0), song("h4", 65.0, "hi"))
        val state = RadioState("seed", crossEvery = 1000)
        listOf("h1", "h2", "h3").forEach(state::played)
        assertEquals("h4", e.next(state)?.key)
    }

    @Test
    fun aSkippedSongPushesTheQueryAwayFromIt() {
        val e = engine(song("seed", 0.0), song("skipped", 30.0), song("skippedNear", 45.0), song("oppositeSide", -35.0))
        val state = RadioState("seed", crossEvery = 1000)
        state.played("skipped")
        state.skipped("skipped")
        assertEquals("oppositeSide", e.next(state)?.key)
    }

    @Test
    fun aRemovedSongIsNotQueuedAgain() {
        val e = engine(song("seed", 0.0), song("removed", 15.0), song("other", 30.0))
        val state = RadioState("seed", crossEvery = 1000)
        state.skipped("removed")
        assertEquals("other", e.next(state)?.key)
    }

    @Test
    fun aSongThatSoundsNothingAlikeIsNotQueued() {
        val e = engine(song("seed", 0.0), song("phonk", 80.0, RadioSong.NO_VOCALS))
        assertNull(e.next(RadioState("seed", crossEvery = 1000)))
    }

    @Test
    fun whenTheQueueWouldRunOutTheClosestRemainingSongPlays() {
        val e = engine(song("seed", 0.0), song("phonk", 80.0, RadioSong.NO_VOCALS))
        assertEquals("phonk", e.next(RadioState("seed", crossEvery = 1000), queueWouldEnd = true)?.key)
    }

    @Test
    fun swappingInMoreSongsKeepsTheSession() {
        val state = RadioState("seed", crossEvery = 1000)
        state.played("old")
        val e = engine(song("seed", 0.0), song("old", 15.0), song("new", 30.0))
        assertEquals("new", e.next(state)?.key)
    }

    @Test
    fun anUnknownSeedGivesNothing() {
        assertNull(engine(song("a", 0.0)).next(RadioState("missing", crossEvery = 10)))
    }

    @Test
    fun anEmptyPoolGivesNothing() {
        assertNull(engine(song("seed", 0.0)).next(RadioState("seed", crossEvery = 10)))
    }

    @Test
    fun aThinLanguagePoolLeavesTheQueueShortRatherThanSwitchingLanguage() {
        val e = engine(song("seed", 0.0), song("hindi", 10.0, "hi"))
        assertNull(e.next(RadioState("seed", crossEvery = 1000)))
    }

    @Test
    fun whenTheQueueWouldRunOutOtherLanguagesAreAllowed() {
        val e = engine(song("seed", 0.0), song("hindi", 40.0, "hi"), song("tamil", 80.0))
        assertEquals("hindi", e.next(RadioState("seed", crossEvery = 1000), queueWouldEnd = true)?.key)
    }

    @Test
    fun aSongWithoutVocalsNeedsAStrongMatchToJoinASession() {
        val e = engine(song("seed", 0.0), song("phonk", 48.0, RadioSong.NO_VOCALS))
        assertNull(e.next(RadioState("seed", crossEvery = 1000)))
    }

    @Test
    fun theCrossLanguageShareGrowsWhenThosePicksGetFinished() {
        assertTrue(RadioTaste.crossEvery(finished = 8, skipped = 0) < RadioTaste.crossEvery(finished = 0, skipped = 0))
    }

    @Test
    fun theCrossLanguageShareShrinksWhenThosePicksGetSkipped() {
        assertTrue(RadioTaste.crossEvery(finished = 0, skipped = 8) > RadioTaste.crossEvery(finished = 0, skipped = 0))
    }

    @Test
    fun theDefaultIsOneSongInTen() {
        assertEquals(10, RadioTaste.crossEvery(finished = 0, skipped = 0))
    }

    @Test
    fun skipsLowerAffinityAndFinishesRaiseIt() {
        assertTrue(RadioTaste.affinity(liked = false, finished = 0, skipped = 3) < 0f)
        assertTrue(RadioTaste.affinity(liked = true, finished = 2, skipped = 0) > 0f)
        assertFalse(RadioTaste.affinity(liked = false, finished = 0, skipped = 0) != 0f)
    }
}
