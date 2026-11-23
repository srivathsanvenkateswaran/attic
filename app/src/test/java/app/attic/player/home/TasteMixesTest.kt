package app.attic.player.home

import app.attic.player.home.TasteMixes.Mix
import app.attic.player.home.TasteMixes.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

class TasteMixesTest {
    private val dims = 128

    /**
     * A unit vector on axis [axis], with a wobble of its own on one of the spare axes (8 and up) so no
     * two songs are copies, and [nudge] along axis 7 to set unplayed songs a little apart.
     */
    private fun near(axis: Int, nudge: Float = 0f, jitter: Int = 0): FloatArray {
        val v = FloatArray(dims)
        v[axis] = 1f
        v[7] += nudge
        v[8 + jitter % (dims - 8)] += 0.25f
        val norm = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
        return FloatArray(dims) { v[it] / norm }
    }

    private fun group(name: String, axis: Int, count: Int, plays: Int = 3, artist: (Int) -> String = { "$name-artist" }) =
        (0 until count).map { Song("$name$it", near(axis, jitter = it), plays = plays + it % 4, skips = 0, liked = false, artistKey = artist(it)) }

    private fun unplayed(key: String, axis: Int, jitter: Int, nudge: Float = 0.3f) =
        Song(key, near(axis, nudge, jitter), plays = 0, skips = 0, liked = false, artistKey = "other")

    private fun groupOf(key: String) = key.takeWhile { it.isLetter() }

    @Test
    fun noEmbeddingsMeansNoMixes() {
        val songs = (0 until 40).map { Song("s$it", null, plays = 5, skips = 0, liked = true, artistKey = "a") }
        assertEquals(emptyList<Mix>(), TasteMixes.build(songs, day = 1))
    }

    @Test
    fun tooFewPlayedSongsMeansNoMixes() {
        val songs = group("a", 0, 4) + group("b", 1, 4) + group("c", 2, 3) + (0 until 30).map { unplayed("u$it", it % 3, it) }
        assertEquals(emptyList<Mix>(), TasteMixes.build(songs, day = 1))
    }

    @Test
    fun anEmptyLibraryMeansNoMixes() {
        assertEquals(emptyList<Mix>(), TasteMixes.build(emptyList(), day = 1))
    }

    @Test
    fun songsThatSoundAlikeEndUpInOneMix() {
        val songs = group("a", 0, 8) + group("b", 1, 8) + group("c", 2, 8)
        val mixes = TasteMixes.build(songs, day = 3)
        assertEquals(3, mixes.size)
        for (mix in mixes) {
            assertEquals("one mix, one group: ${mix.keys}", 1, mix.keys.map(::groupOf).toSet().size)
            assertEquals(8, mix.keys.size)
        }
        assertEquals(setOf("a", "b", "c"), mixes.map { groupOf(it.keys.first()) }.toSet())
    }

    @Test
    fun theSameDayGivesTheSameMixesWhateverTheInputOrder() {
        val songs = group("a", 0, 12) + group("b", 1, 12) + group("c", 2, 12) + group("d", 3, 12) +
            (0 until 40).map { unplayed("u$it", it % 4, it) }
        val first = TasteMixes.build(songs, day = 20_415)
        val again = TasteMixes.build(songs.shuffled(Random(7)), day = 20_415)
        assertEquals(first, again)
        assertTrue(first.isNotEmpty())
    }

    @Test
    fun clusteringIsDeterministicForASeed() {
        val vectors = (0 until 30).map { near(it % 3, jitter = it) }
        val weights = List(30) { 1f + it % 3 }
        val a = TasteMixes.cluster(vectors, weights, 3, Random(42))
        val b = TasteMixes.cluster(vectors, weights, 3, Random(42))
        assertTrue(a.contentEquals(b))
        // Every song lands with the others on its axis.
        for (i in vectors.indices) assertEquals(a[i % 3], a[i])
    }

    @Test
    fun moreListeningGivesMoreMixesUpToFive() {
        val songs = (0 until 6).flatMap { axis -> group("g${'a' + axis}", axis, 20) }
        val mixes = TasteMixes.build(songs, day = 1)
        assertEquals(TasteMixes.MAX_MIXES, mixes.size)
    }

    @Test
    fun aMixIsToppedUpWithUnplayedSongsThatSoundLikeIt() {
        val songs = group("a", 0, 6) + group("b", 1, 6) + group("c", 2, 6) +
            (0 until 5).map { unplayed("nearA$it", 0, it) } + (0 until 5).map { unplayed("far$it", 5, it) }
        val mixes = TasteMixes.build(songs, day = 1)
        val mixA = mixes.single { mix -> mix.keys.any { it.startsWith("a") } }
        assertTrue(mixA.keys.any { it.startsWith("nearA") })
        assertTrue(mixes.flatMap { it.keys }.none { it.startsWith("far") })
    }

    @Test
    fun noSongIsInTwoMixesAndNoMixIsLongerThanTwentyFive() {
        val songs = group("a", 0, 20) + group("b", 1, 20) + group("c", 2, 20) + (0 until 90).map { unplayed("u$it", it % 3, it) }
        val mixes = TasteMixes.build(songs, day = 5)
        val all = mixes.flatMap { it.keys }
        assertEquals(all.size, all.toSet().size)
        assertTrue(mixes.all { it.keys.size <= TasteMixes.MIX_SIZE })
        assertTrue(mixes.all { it.keys.size == TasteMixes.MIX_SIZE })
    }

    @Test
    fun aboutOneSongInThreeIsADiscovery() {
        val songs = group("a", 0, 20) + group("b", 1, 20) + group("c", 2, 20) + (0 until 90).map { unplayed("u$it", it % 3, it) }
        val mix = TasteMixes.build(songs, day = 5).first()
        assertEquals(listOf(false, false, true), mix.keys.take(3).map { it.startsWith("u") })
        assertEquals(TasteMixes.MIX_SIZE / 3, mix.keys.count { it.startsWith("u") })
    }

    @Test
    fun songsYouMostlySkipDontShapeTheMixes() {
        val skipped = (0 until 20).map { Song("skip$it", near(4, jitter = it), plays = 2, skips = 9, liked = false, artistKey = "s") }
        val songs = group("a", 0, 6) + group("b", 1, 6) + group("c", 2, 6) + skipped
        val mixes = TasteMixes.build(songs, day = 1)
        assertTrue(mixes.flatMap { it.keys }.none { it.startsWith("skip") })
    }

    @Test
    fun favouritesCountWithoutPlays() {
        val liked = { name: String, axis: Int -> (0 until 5).map { Song("$name$it", near(axis, jitter = it), plays = 0, skips = 0, liked = true, artistKey = name) } }
        val mixes = TasteMixes.build(liked("a", 0) + liked("b", 1) + liked("c", 2), day = 1)
        assertEquals(3, mixes.size)
    }

    @Test
    fun aMixIsNamedAfterItsMostPlayedArtists() {
        val songs = group("a", 0, 9) { if (it < 5) "rahman" else "anirudh" } + group("b", 1, 6) + group("c", 2, 6)
        val mixA = TasteMixes.build(songs, day = 1).single { mix -> mix.keys.any { it.startsWith("a") } }
        assertEquals(listOf("rahman", "anirudh"), mixA.artistKeys.take(2))
    }

    @Test
    fun twoMixesDontOpenWithTheSameArtist() {
        val mixes = listOf(Mix(listOf("x"), listOf("rahman", "anirudh")), Mix(listOf("y"), listOf("rahman", "ilaiyaraaja")), Mix(listOf("z"), listOf("rahman")))
        assertEquals(listOf(listOf("rahman", "anirudh"), listOf("ilaiyaraaja", "rahman"), listOf("rahman")), TasteMixes.nameArtists(mixes))
    }
}
