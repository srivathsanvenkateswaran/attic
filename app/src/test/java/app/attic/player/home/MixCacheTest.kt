package app.attic.player.home

import app.attic.player.home.TasteMixes.Mix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixCacheTest {
    private val cache = MixCache(
        day = 20_415,
        seeds = 312,
        mixes = listOf(
            Mix(listOf("Music/Tamil/Aaruyire.mp3", "Music/Hindi/Kun Faya Kun.flac"), listOf("a. r. rahman", "anirudh")),
            Mix(listOf("Music/Phonk/GHOST!.mp3"), emptyList()),
        ),
    )

    @Test
    fun whatIsSavedIsReadBack() {
        assertEquals(cache, MixCache.decode(cache.encode()))
    }

    @Test
    fun noMixesIsSavedToo() {
        val empty = MixCache(20_415, 3, emptyList())
        assertEquals(empty, MixCache.decode(empty.encode()))
    }

    @Test
    fun anythingElseIsIgnored() {
        assertNull(MixCache.decode(""))
        assertNull(MixCache.decode("2\u001E20415\u001E3"))
        assertNull(MixCache.decode("1\u001Etoday\u001E3"))
        assertNull(MixCache.decode(cache.encode() + "\nhalf a line"))
    }

    @Test
    fun theMixesLastTheDay() {
        assertTrue(cache.isCurrent(today = 20_415, seedsNow = 400))
        assertFalse(cache.isCurrent(today = 20_416, seedsNow = 312))
    }

    @Test
    fun anEmptyDayIsBuiltAgainOnceMoreSongsAreAnalysed() {
        val empty = MixCache(20_415, 5, emptyList())
        assertTrue(empty.isCurrent(today = 20_415, seedsNow = 5))
        assertFalse(empty.isCurrent(today = 20_415, seedsNow = 14))
    }
}
