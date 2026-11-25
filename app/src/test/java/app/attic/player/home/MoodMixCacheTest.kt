package app.attic.player.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodMixCacheTest {
    private val picks = MoodMixes.Picks(listOf(Mood.Focus, Mood.Chill), setOf("pop", "folk"), setOf("ta"))
    private val cache = MoodMixCache(
        day = 20_000,
        inputs = MoodMixCache.inputs(picks, 1, 742),
        mixes = listOf(MoodMixes.Mix(Mood.Chill, listOf("Music/a b.mp3", "Music/Tamil/ச.m4a")), MoodMixes.Mix(Mood.Party, emptyList())),
    )

    @Test
    fun roundTrips() = assertEquals(cache, MoodMixCache.decode(cache.encode()))

    @Test
    fun anotherDayOrOtherInputsAreStale() {
        assertTrue(cache.isCurrent(20_000, MoodMixCache.inputs(picks, 1, 742)))
        assertFalse(cache.isCurrent(20_001, cache.inputs))
        assertFalse(cache.isCurrent(20_000, MoodMixCache.inputs(picks, 1, 743)))
        assertFalse(cache.isCurrent(20_000, MoodMixCache.inputs(picks, 2, 742)))
        assertFalse(cache.isCurrent(20_000, MoodMixCache.inputs(picks.copy(languages = emptySet()), 1, 742)))
    }

    @Test
    fun genreAndLanguageOrderDoesNotMatterButMoodOrderDoes() {
        assertEquals(MoodMixCache.inputs(picks, 1, 1), MoodMixCache.inputs(picks.copy(genres = setOf("folk", "pop")), 1, 1))
        assertNotEquals(MoodMixCache.inputs(picks, 1, 1), MoodMixCache.inputs(picks.copy(moods = listOf(Mood.Chill, Mood.Focus)), 1, 1))
    }

    @Test
    fun anythingElseIsRebuilt() {
        assertNull(MoodMixCache.decode("2\u001E20000\u001Ex"))
        assertNull(MoodMixCache.decode("garbage"))
        assertNull(MoodMixCache.decode(cache.encode().replace("chill", "grumpy")))
    }
}
