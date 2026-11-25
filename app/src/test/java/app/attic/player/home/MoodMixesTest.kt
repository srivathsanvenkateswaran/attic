package app.attic.player.home

import app.attic.player.analysis.MoodSignals
import app.attic.player.data.LanguageLabels
import app.attic.player.playback.ListenTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/** Fixtures with hand-set mood signals, checked against the formula in docs/features/mood-mixes.md. */
class MoodMixesTest {
    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val day = 20_000L

    /** Signals for a song: everything at 0.5 and every tag at 0.1 unless given. */
    private fun signals(exact: Boolean = true, vararg set: Pair<Int, Float>): FloatArray {
        val out = FloatArray(MoodSignals.SIZE) { if (it < MoodSignals.THEME_START) 0.5f else 0.1f }
        if (!exact) {
            out[MoodSignals.ENGAGEMENT] = Float.NaN
            for (i in MoodSignals.THEME_START until MoodSignals.SIZE) out[i] = Float.NaN
        }
        for ((i, v) in set) out[i] = v
        return out
    }

    private fun song(
        key: String,
        signals: FloatArray,
        language: String? = null,
        genres: Set<String> = emptySet(),
        artist: String? = "artist:$key",
        album: String? = "album:$key",
        liked: Boolean = false,
    ) = MoodMixes.Song(key, signals, language, genres, artist, album, liked)

    /** A chill song: relaxed by [relax], calm tags, not aggressive, not engaging. */
    private fun chill(key: String, relax: Float, exact: Boolean = true, language: String? = null, artist: String? = "artist:$key", album: String? = "album:$key", genres: Set<String> = emptySet(), liked: Boolean = false) =
        song(
            key,
            signals(
                exact,
                MoodSignals.RELAXED to relax,
                MoodSignals.AGGRESSIVE to 0.05f,
                MoodSignals.HAPPY to 0.3f,
                MoodSignals.PARTY to 0.2f,
                MoodSignals.ENGAGEMENT to (if (exact) 1f - relax else Float.NaN),
                MoodSignals.tag("calm") to (if (exact) relax / 2 else Float.NaN),
                MoodSignals.tag("relaxing") to (if (exact) relax / 2 else Float.NaN),
                MoodSignals.tag("soft") to (if (exact) relax / 2 else Float.NaN),
            ),
            language = language,
            artist = artist,
            album = album,
            genres = genres,
            liked = liked,
        )

    /** A loud song that fits no calm mood. */
    private fun loud(key: String) = song(
        key,
        signals(true, MoodSignals.RELAXED to 0.1f, MoodSignals.AGGRESSIVE to 0.9f, MoodSignals.PARTY to 0.9f, MoodSignals.DANCEABLE to 0.9f, MoodSignals.ENGAGEMENT to 0.9f),
    )

    private fun library(chillCount: Int = 30, loudCount: Int = 30): List<MoodMixes.Song> =
        (0 until chillCount).map { chill("chill%02d".format(it), 0.55f + 0.4f * it / chillCount) } +
            (0 until loudCount).map { loud("loud%02d".format(it)) }

    /** Loud songs alongside, so the calm ones rank high on every Chill term instead of tying with each other. */
    private fun filled(songs: List<MoodMixes.Song>) = songs + (0 until 20).map { loud("zz%02d".format(it)) }

    private fun keysOf(candidates: List<MoodMixes.Candidate>?) = candidates.orEmpty().map { it.song.key }

    // ---------------------------------------------------------------- percentiles and fit

    @Test
    fun percentilesRunFromZeroToOneAndTiesShareTheirMeanRank() {
        val p = MoodMixes.percentiles(floatArrayOf(0.2f, 0.9f, 0.5f, 0.5f, Float.NaN))
        assertEquals(0f, p[0], 1e-6f)
        assertEquals(1f, p[1], 1e-6f)
        // The two 0.5s hold ranks 1 and 2 of 0..3, so both get 1.5 / 3.
        assertEquals(0.5f, p[2], 1e-6f)
        assertEquals(0.5f, p[3], 1e-6f)
        assertTrue(p[4].isNaN())
        assertEquals(0.5f, MoodMixes.percentiles(floatArrayOf(Float.NaN, 0.3f))[1], 1e-6f)
    }

    @Test
    fun fitIsTheWeightedMeanOfPercentiles() {
        // Two songs: the first highest on every Chill term, the second lowest.
        val songs = listOf(chill("a", 0.9f), chill("b", 0.6f))
        val ranks = MoodMixes.ranks(songs)
        val fit = MoodMixes.fits(Mood.Chill, songs, ranks)
        // a: relaxed 1, tags 1, (1 − aggressive) 0.5 (a tie), (1 − engagement) 1 → (3 + 2 + 0.5 + 1) / 7.
        assertEquals(6.5f / 7f, fit[0], 1e-5f)
        // b: 0, 0, 0.5, 0 → 0.5 / 7, under the floor.
        assertTrue(fit[1].isNaN())
    }

    @Test
    fun aGateKeepsOutSongsThatRankWellButAreNotThatMood() {
        // Every song is tense: the most relaxed of them still isn't relaxed, so none is Chill.
        val songs = (0 until 20).map { chill("s$it", 0.1f + it * 0.01f) }
        val candidates = MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now)
        assertNull(candidates[Mood.Chill])
    }

    @Test
    fun aMissingTermDropsOutAndTheRestAreRescaled() {
        val exact = chill("a", 0.9f)
        val approximate = chill("b", 0.9f, exact = false)
        val songs = listOf(exact, approximate, chill("c", 0.6f), chill("d", 0.6f, exact = false))
        val fit = MoodMixes.fits(Mood.Chill, songs, MoodMixes.ranks(songs))
        // b has only relaxed (tied top with a: ranks 2 and 3 of 0..3, so 2.5 / 3) and aggressive (a
        // four-way tie, 0.5): (3 · 2.5 / 3 + 1 · 0.5) / 4.
        assertEquals(0.75f, fit[1], 1e-5f)
        assertFalse(fit[0].isNaN())
    }

    @Test
    fun romanticNeedsTheTagsSoApproximateScoresNeverFitIt() {
        val tender = { key: String, exact: Boolean ->
            song(
                key,
                signals(
                    exact,
                    MoodSignals.AGGRESSIVE to 0.02f,
                    MoodSignals.INSTRUMENTAL to 0.05f,
                    MoodSignals.PARTY to 0.1f,
                    MoodSignals.RELAXED to 0.9f,
                    MoodSignals.tag("romantic") to (if (exact) 0.4f else Float.NaN),
                    MoodSignals.tag("love") to (if (exact) 0.4f else Float.NaN),
                ),
            )
        }
        val songs = (0 until 10).map { tender("exact$it", true) } + (0 until 10).map { tender("approx$it", false) } +
            (0 until 10).map { loud("loud$it") }
        val romantic = keysOf(MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now)[Mood.Romantic])
        assertEquals((0 until 10).map { "exact$it" }.toSet(), romantic.toSet())
    }

    @Test
    fun everyMoodHasARecipeAndEachRecipeReadsRealTags() {
        assertEquals(Mood.entries.toSet(), MoodMixes.RECIPES.keys)
        // MoodSignals.tag throws for a name the head doesn't have, so building the recipes proves the names.
        assertTrue(MoodMixes.LOVE_TAGS.indices.all { it >= MoodSignals.THEME_START })
    }

    // ---------------------------------------------------------------- score

    @Test
    fun tasteAndFreshnessFollowTheFormula() {
        assertEquals(0f, MoodMixes.taste(false, 0.0, 0.0), 0f)
        assertEquals(0.5f + 0.25f, MoodMixes.taste(true, 2.0, 0.0), 1e-6f)
        assertEquals(-0.4f, MoodMixes.taste(false, 0.0, 1.0), 1e-6f)
        // Endless skips and nothing else bottom out at −0.8, inside the clamp.
        assertEquals(-0.8f, MoodMixes.taste(false, 0.0, 1e9), 1e-6f)
        assertEquals(1f, MoodMixes.fresh(now, now), 0f)
        assertEquals(0.5f, MoodMixes.fresh(now - 12 * hour, now), 1e-6f)
        assertEquals(0f, MoodMixes.fresh(null, now), 0f)
        // A listen from the future (a clock change) counts as just heard, never above 1.
        assertEquals(1f, MoodMixes.fresh(now + hour, now), 0f)
    }

    @Test
    fun aListenCountsHalfAfterThirtyDays() {
        val history = MoodMixes.History.of(
            listOf(
                MoodMixes.Listen("a", now, ListenTracker.FINISHED),
                MoodMixes.Listen("a", now - 30 * 24 * hour, ListenTracker.PARTIAL),
                MoodMixes.Listen("b", now - 60 * 24 * hour, ListenTracker.SKIPPED),
            ),
            now,
        )
        assertEquals(1.5, history.played.getValue("a"), 1e-9)
        assertEquals(0.25, history.skipped.getValue("b"), 1e-9)
        assertEquals(now, history.lastHeard.getValue("a"))
        assertEquals(1, history.finishes.getValue("a"))
    }

    @Test
    fun picksAndLikesRaiseASongAndAJustHeardOneFalls() {
        val songs = filled((0 until 12).map { chill("s%02d".format(it), 0.8f, artist = "a$it") })
        val plain = MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now).getValue(Mood.Chill)
        val base = plain.first { it.song.key == "s05" }.score

        val liked = songs.map { if (it.key == "s05") chill("s05", 0.8f, artist = "a5", liked = true) else it }
        val likedScore = MoodMixes.candidates(liked, MoodMixes.Picks(), emptyList(), now).getValue(Mood.Chill).first { it.song.key == "s05" }.score
        assertEquals(base + MoodMixes.TASTE_WEIGHT * 0.5f, likedScore, 1e-5f)

        val heard = listOf(MoodMixes.Listen("s05", now - hour, ListenTracker.PARTIAL))
        val heardScore = MoodMixes.candidates(songs, MoodMixes.Picks(), heard, now).getValue(Mood.Chill).first { it.song.key == "s05" }.score
        val expected = base + MoodMixes.TASTE_WEIGHT * MoodMixes.taste(false, 0.5.pow(hour / MoodMixes.TASTE_HALF_LIFE_MS.toDouble()), 0.0) -
            MoodMixes.FRESH_WEIGHT * MoodMixes.fresh(now - hour, now)
        assertEquals(expected, heardScore, 1e-5f)
        assertTrue(heardScore < base)
    }

    private fun Double.pow(x: Double) = Math.pow(this, x)

    @Test
    fun aPickedGenreAddsItsWeight() {
        val songs = filled((0 until 12).map { chill("s%02d".format(it), 0.8f, genres = if (it == 3) setOf("folk") else setOf("pop")) })
        val none = MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now).getValue(Mood.Chill)
        val folk = MoodMixes.candidates(songs, MoodMixes.Picks(genres = setOf("folk")), emptyList(), now).getValue(Mood.Chill)
        assertEquals("s03", folk.first().song.key)
        assertEquals(none.first { it.song.key == "s03" }.score + MoodMixes.GENRE_WEIGHT, folk.first().score, 1e-5f)
    }

    @Test
    fun picksLanguagesLeavesOthersOutButKeepsUnknownAndNoVocals() {
        val songs = filled(
            (0 until 10).map { chill("ta$it", 0.8f, language = "ta") } +
                (0 until 5).map { chill("hi$it", 0.8f, language = "hi") } +
                listOf(chill("unknown", 0.8f), chill("instrumental", 0.8f, language = LanguageLabels.NO_VOCALS)),
        )
        val chill = keysOf(MoodMixes.candidates(songs, MoodMixes.Picks(languages = setOf("ta")), emptyList(), now)[Mood.Chill])
        assertTrue(chill.none { it.startsWith("hi") })
        assertTrue("unknown" in chill && "instrumental" in chill)
        assertEquals(12, chill.size)
    }

    @Test
    fun theLanguageFilterIsDroppedWhenTooFewSongsAreLeft() {
        val songs = filled((0 until 3).map { chill("ta$it", 0.8f, language = "ta") } + (0 until 10).map { chill("hi$it", 0.8f, language = "hi") })
        val chill = MoodMixes.candidates(songs, MoodMixes.Picks(languages = setOf("ta")), emptyList(), now).getValue(Mood.Chill)
        assertEquals(13, chill.size)
        // The picked language still ranks first through its term.
        assertEquals(setOf("ta0", "ta1", "ta2"), chill.take(3).map { it.song.key }.toSet())
    }

    @Test
    fun aSongYouSkipIsLeftOut() {
        val songs = filled((0 until 12).map { chill("s%02d".format(it), 0.8f) })
        val skips = List(3) { MoodMixes.Listen("s04", now - (it + 1) * 24 * hour, ListenTracker.SKIPPED) } +
            MoodMixes.Listen("s04", now - 10 * 24 * hour, ListenTracker.FINISHED)
        assertFalse("s04" in keysOf(MoodMixes.candidates(songs, MoodMixes.Picks(), skips, now)[Mood.Chill]))
        // Skipped three times but finished as often: kept.
        val even = skips + List(2) { MoodMixes.Listen("s04", now - 20 * 24 * hour, ListenTracker.FINISHED) }
        assertTrue("s04" in keysOf(MoodMixes.candidates(songs, MoodMixes.Picks(), even, now)[Mood.Chill]))
    }

    // ---------------------------------------------------------------- the day's mix

    private fun buildChill(songs: List<MoodMixes.Song>, day: Long = this.day, picks: MoodMixes.Picks = MoodMixes.Picks(), vectors: Map<String, FloatArray> = emptyMap()) =
        MoodMixes.build(songs, picks, emptyList(), now, day, vectors).firstOrNull { it.mood == Mood.Chill }

    @Test
    fun theSameInputsGiveTheSameMix() {
        val songs = library(chillCount = 100)
        val first = buildChill(songs)!!
        assertEquals(first, buildChill(songs))
        assertEquals(first, buildChill(songs.shuffled(kotlin.random.Random(7))))
        assertEquals(MoodMixes.MIX_SIZE, first.keys.size)
        assertEquals(first.keys.size, first.keys.distinct().size)
    }

    @Test
    fun anotherDayDrawsAnotherMixFromTheSamePool() {
        val songs = library(chillCount = 100)
        val today = buildChill(songs, day)!!
        val tomorrow = buildChill(songs, day + 1)!!
        assertNotEquals(today.keys, tomorrow.keys)
        val pool = MoodMixes.pool(MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now).getValue(Mood.Chill)).map { it.song.key }.toSet()
        assertTrue(pool.containsAll(today.keys) && pool.containsAll(tomorrow.keys))
        assertEquals(MoodMixes.POOL_SIZE, pool.size)
    }

    @Test
    fun higherScoresAreLikelierToMakeTheCut() {
        val songs = library(chillCount = 60)
        val pool = MoodMixes.pool(MoodMixes.candidates(songs, MoodMixes.Picks(), emptyList(), now).getValue(Mood.Chill))
        val top = pool.take(10).map { it.song.key }.toSet()
        val bottom = pool.takeLast(10).map { it.song.key }.toSet()
        var topCount = 0
        var bottomCount = 0
        for (d in 0 until 60) {
            val keys = MoodMixes.mix(Mood.Chill, pool, day + d, emptyMap())!!.keys
            topCount += keys.count { it in top }
            bottomCount += keys.count { it in bottom }
        }
        assertTrue("top $topCount, bottom $bottomCount", topCount > bottomCount)
    }

    @Test
    fun aChangeInPicksChangesTheMix() {
        val songs = filled((0 until 60).map { chill("s%02d".format(it), 0.6f + 0.3f * (it % 7) / 7, genres = if (it % 5 == 0) setOf("folk") else emptySet()) })
        assertNotEquals(buildChill(songs), buildChill(songs, picks = MoodMixes.Picks(genres = setOf("folk"))))
    }

    @Test
    fun oneVersionOfEachRecording() {
        val songs = filled((0 until 12).map { chill("s%02d".format(it), 0.8f) })
        val vectors = songs.filter { it.key.startsWith("s") }.associate { it.key to unit(it.key.hashCode()) }.toMutableMap()
        vectors["s01"] = vectors.getValue("s00").copyOf()
        val mix = buildChill(songs, vectors = vectors)!!
        assertFalse("s00" in mix.keys && "s01" in mix.keys)
        assertEquals(11, mix.keys.size)
    }

    @Test
    fun artistAndAlbumCapsHold() {
        val songs = filled((0 until 40).map { chill("s%02d".format(it), 0.8f, artist = "artist${it % 4}", album = "album${it % 10}") })
        val mix = buildChill(songs)!!
        val artists = mix.keys.map { k -> songs.first { it.key == k }.artistKey }.groupingBy { it }.eachCount()
        val albums = mix.keys.map { k -> songs.first { it.key == k }.albumKey }.groupingBy { it }.eachCount()
        assertTrue(artists.values.all { it <= MoodMixes.MAX_PER_ARTIST })
        assertTrue(albums.values.all { it <= MoodMixes.MAX_PER_ALBUM })
        assertEquals(4 * MoodMixes.MAX_PER_ARTIST, mix.keys.size)
    }

    @Test
    fun fewerThanEightSongsMakeNoMix() {
        assertNull(buildChill(library(chillCount = 7)))
        assertEquals(8, buildChill(library(chillCount = 8))!!.keys.size)
    }

    @Test
    fun noArtistTwiceInARowWhereThatCanBeHelped() {
        fun s(key: String, artist: String) = chill(key, 0.8f, artist = artist)
        val spread = MoodMixes.spreadArtists(listOf(s("1", "a"), s("2", "a"), s("3", "b"), s("4", "a"), s("5", "c")))
        assertEquals(listOf("1", "3", "2", "5", "4"), spread.map { it.key })
        // Nothing else to put between them: left as it is.
        assertEquals(listOf("1", "2"), MoodMixes.spreadArtists(listOf(s("1", "a"), s("2", "a"))).map { it.key })
    }

    @Test
    fun homeShowsPickedMoodsInTheirOrderOrEveryMix() {
        val mixes = listOf(MoodMixes.Mix(Mood.Chill, listOf("a")), MoodMixes.Mix(Mood.Party, listOf("b")), MoodMixes.Mix(Mood.Focus, listOf("c")))
        assertEquals(mixes, MoodMixes.forHome(mixes, MoodMixes.Picks()))
        assertEquals(
            listOf(Mood.Focus, Mood.Chill),
            MoodMixes.forHome(mixes, MoodMixes.Picks(moods = listOf(Mood.Focus, Mood.Romantic, Mood.Chill))).map { it.mood },
        )
    }

    @Test
    fun moodIdsRoundTrip() {
        Mood.entries.forEach { assertEquals(it, Mood.fromId(it.id)) }
        assertNull(Mood.fromId("grumpy"))
    }

    private fun unit(seed: Int): FloatArray {
        val random = kotlin.random.Random(seed)
        val v = FloatArray(16) { random.nextFloat() - 0.5f }
        val n = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
        return FloatArray(16) { v[it] / n }.also { check(abs(it.sumOf { x -> (x * x).toDouble() } - 1) < 1e-4) }
    }
}
