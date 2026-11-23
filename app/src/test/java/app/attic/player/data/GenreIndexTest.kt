package app.attic.player.data

import app.attic.player.analysis.StyleLabels
import app.attic.player.analysis.StyleScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GenreIndexTest {
    private val labels = StyleLabels.parse(File("src/main/assets/${StyleLabels.ASSET}").readText())

    private fun style(label: String, score: Float) = StyleScore(labels.labels.indexOf(label).also { require(it >= 0) { label } }, score)

    private fun track(key: String, genre: String? = null, album: String? = null) = Track(
        key = key, mediaId = key.hashCode().toLong(), title = key, artist = "A", artistKeys = listOf("a"),
        album = album, albumKey = album, folder = null, trackNumber = null, durationMs = 180_000, year = null,
        dateAddedSec = 0, genre = genre,
    )

    private fun library(tracks: List<Track>): Library {
        val albums = tracks.filter { it.albumKey != null }.groupBy { it.albumKey!! }
            .map { (key, songs) -> Album(key, key, "A", "a", songs, null) }
        return Library(tracks, albums, emptyList(), emptyList())
    }

    // Tags

    @Test
    fun tagSplitsOnSemicolonsAndNullsButNotSlashes() {
        assertEquals(listOf("Rock", "Pop"), GenreIndex.tagGenres("Rock; Pop"))
        assertEquals(listOf("Rock", "Pop"), GenreIndex.tagGenres("Rock\u0000Pop"))
        assertEquals(listOf("Funk / Soul"), GenreIndex.tagGenres("Funk / Soul"))
    }

    @Test
    fun tagDropsYoutubeCategoriesBlanksAndRepeats() {
        assertEquals(emptyList<String>(), GenreIndex.tagGenres("Music"))
        assertEquals(listOf("Hip-Hop"), GenreIndex.tagGenres("Hip-Hop; ; hip hop; People & Blogs"))
        assertEquals(emptyList<String>(), GenreIndex.tagGenres(null))
    }

    // The model

    @Test
    fun modelGenreIsWhereTheStylesAddUpMost() {
        // Two folk styles outweigh one stronger pop style.
        val styles = listOf(
            style("Pop---Bollywood", 0.40f),
            style("Folk, World, & Country---Hindustani", 0.30f),
            style("Folk, World, & Country---Indian Classical", 0.25f),
        )
        assertEquals(listOf("Folk, World, & Country"), GenreIndex.modelGenres(styles, labels).take(1))
    }

    @Test
    fun secondGenreCountsAtSixtyPercentOfTheFirst() {
        val close = listOf(style("Folk, World, & Country---Hindustani", 0.57f), style("Pop---Bollywood", 0.53f), style("Stage & Screen---Soundtrack", 0.20f))
        assertEquals(listOf("Folk, World, & Country", "Pop"), GenreIndex.modelGenres(close, labels))
        val far = listOf(style("Hip Hop---Trap", 0.41f), style("Hip Hop---Cloud Rap", 0.30f), style("Rock---Emo", 0.06f))
        assertEquals(listOf("Hip Hop"), GenreIndex.modelGenres(far, labels))
    }

    @Test
    fun nonMusicIsNeverAGenre() {
        val styles = listOf(style("Non-Music---Poetry", 0.21f), style("Electronic---Breakcore", 0.07f))
        assertEquals(listOf("Electronic"), GenreIndex.modelGenres(styles, labels))
    }

    @Test
    fun aWeakGuessFilesTheSongNowhere() {
        assertTrue(GenreIndex.modelGenres(listOf(style("Pop---Ballad", 0.04f)), labels).isEmpty())
        assertTrue(GenreIndex.modelGenres(emptyList(), labels).isEmpty())
    }

    @Test
    fun theTagWinsOverTheModel() {
        val styles = listOf(style("Electronic---House", 0.5f))
        assertEquals(listOf("Bollywood"), GenreIndex.songGenres(track("a", genre = "Bollywood"), styles, labels))
        assertEquals(listOf("Electronic"), GenreIndex.songGenres(track("b"), styles, labels))
        assertEquals(listOf("Electronic"), GenreIndex.songGenres(track("c", genre = "Music"), styles, labels))
        assertTrue(GenreIndex.songGenres(track("d"), null, labels).isEmpty())
    }

    // The list

    @Test
    fun tagAndModelSongsMeetUnderOneName() {
        val tagged = (1..3).map { track("t$it", genre = "Hip-Hop") }
        val analysed = (1..2).map { track("m$it") }
        val styles = analysed.associate { it.key to listOf(style("Hip Hop---Trap", 0.4f)) }
        val genres = GenreIndex.build(library(tagged + analysed), styles, labels)
        assertEquals(1, genres.size)
        assertEquals("hiphop", genres[0].key)
        // Three songs spell it "Hip-Hop", two "Hip Hop".
        assertEquals("Hip-Hop", genres[0].name)
        assertEquals((tagged + analysed).map { it.key }, genres[0].tracks.map { it.key })
    }

    @Test
    fun genresUnderFiveSongsAreLeftOut() {
        val rock = (1..5).map { track("r$it", genre = "Rock") }
        val jazz = (1..4).map { track("j$it", genre = "Jazz") }
        assertEquals(listOf("Rock"), GenreIndex.build(library(rock + jazz), emptyMap(), labels).map { it.name })
    }

    @Test
    fun biggestGenreFirst() {
        val rock = (1..5).map { track("r$it", genre = "Rock") }
        val pop = (1..7).map { track("p$it", genre = "Pop") }
        assertEquals(listOf("Pop", "Rock"), GenreIndex.build(library(rock + pop), emptyMap(), labels).map { it.name })
    }

    @Test
    fun anAlbumJoinsWithTwoSongsAndAtLeastHalfOfThem() {
        val mostly = listOf(track("m1", "Rock", "Mostly"), track("m2", "Rock", "Mostly"), track("m3", "Jazz", "Mostly"))
        val once = listOf(track("o1", "Rock", "Once"), track("o2", "Jazz", "Once"))
        val few = listOf(track("f1", "Rock", "Few"), track("f2", "Rock", "Few")) + (3..6).map { track("f$it", "Jazz", "Few") }
        val genres = GenreIndex.build(library(mostly + once + few), emptyMap(), labels)
        val rock = genres.single { it.name == "Rock" }
        assertEquals(listOf("Mostly"), rock.albums.map { it.key })
    }
}
