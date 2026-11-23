package app.attic.player.data

import app.attic.player.analysis.StyleLabels
import app.attic.player.analysis.StyleScore

/**
 * A genre you can browse: songs whose genre tag names it or, for songs without a tag, whose analysed
 * styles mostly fall under it. [albums] are the albums most of whose songs are in it.
 */
data class Genre(
    /** [TitleCleaner.normalize] of the name, so "Hip-Hop" and "Hip Hop" are one genre. */
    val key: String,
    val name: String,
    val tracks: List<Track>,
    val albums: List<Album>,
)

/** Builds the Genres list from tags and analysis. Pure, so it can run off the main thread and in tests. */
object GenreIndex {
    /** A genre with fewer songs than this is left out, so a stray tag doesn't become a page. */
    const val MIN_SONGS = 5

    /** A second genre counts when its styles add up to at least this share of the first one's. */
    const val SECOND_GENRE_SHARE = 0.6f

    /** Below this, the model's best guess is too weak to file the song anywhere. */
    const val MIN_TOP_SCORE = 0.05f

    /** An album joins a genre when at least two of its songs, and at least half of them, are in it. */
    private const val ALBUM_MIN_SONGS = 2

    /** Discogs' genre for speech and sound effects, which says nothing useful about a music file. */
    private const val NON_MUSIC = "Non-Music"

    private val tagSeparators = Regex("""\s*[;\u0000]\s*""")

    /** The genres a tag names. Several are separated by ";" (or ID3's null), never by "/": "Funk / Soul" is one. */
    fun tagGenres(tag: String?): List<String> =
        tag?.split(tagSeparators).orEmpty().mapNotNull(TitleCleaner::genre).distinctBy(TitleCleaner::normalize)

    /**
     * The Discogs genres a song's top styles fall under. Each genre gets the sum of its styles' scores;
     * the strongest always counts, and another counts at [SECOND_GENRE_SHARE] of it or more. A film song
     * whose styles are Hindustani, Bollywood and Soundtrack lands in "Folk, World, & Country" and "Pop".
     */
    fun modelGenres(styles: List<StyleScore>, labels: StyleLabels): List<String> {
        if ((styles.maxOfOrNull { it.score } ?: 0f) < MIN_TOP_SCORE) return emptyList()
        val totals = LinkedHashMap<String, Float>()
        for (style in styles) {
            val genre = labels.genre(style.label)
            if (genre != NON_MUSIC) totals[genre] = (totals[genre] ?: 0f) + style.score
        }
        val best = totals.values.maxOrNull() ?: return emptyList()
        return totals.entries
            .filter { it.value >= best * SECOND_GENRE_SHARE }
            .sortedByDescending { it.value }
            .map { it.key }
    }

    /** A song's genres: its tag's when it has one, otherwise the model's. */
    fun songGenres(track: Track, styles: List<StyleScore>?, labels: StyleLabels): List<String> =
        tagGenres(track.genre).ifEmpty { styles?.let { modelGenres(it, labels) }.orEmpty() }

    /**
     * Every genre with [MIN_SONGS] or more songs, biggest first. A genre's name is the spelling most of
     * its songs use. Songs keep the library's order; albums go by how many of their songs are in it.
     */
    fun build(library: Library, styles: Map<String, List<StyleScore>>, labels: StyleLabels): List<Genre> {
        val members = LinkedHashMap<String, MutableList<Track>>()
        val spellings = HashMap<String, MutableMap<String, Int>>()
        for (track in library.tracks) {
            for (name in songGenres(track, styles[track.key], labels)) {
                val key = TitleCleaner.normalize(name)
                if (key.isEmpty()) continue
                members.getOrPut(key) { ArrayList() } += track
                spellings.getOrPut(key) { HashMap() }.merge(name, 1, Int::plus)
            }
        }
        return members
            .filterValues { it.size >= MIN_SONGS }
            .map { (key, tracks) ->
                val name = spellings.getValue(key).entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).first().key
                Genre(key, name, tracks, albumsOf(tracks, library))
            }
            .sortedWith(compareByDescending<Genre> { it.tracks.size }.thenBy { it.name.lowercase() })
    }

    private fun albumsOf(tracks: List<Track>, library: Library): List<Album> {
        val counts = tracks.mapNotNull { it.albumKey }.groupingBy { it }.eachCount()
        return counts.entries
            .mapNotNull { (key, count) ->
                val album = library.albumByKey[key] ?: return@mapNotNull null
                if (count >= ALBUM_MIN_SONGS && count * 2 >= album.tracks.size) album to count else null
            }
            .sortedWith(compareByDescending<Pair<Album, Int>> { it.second }.thenBy { it.first.name.lowercase() })
            .map { it.first }
    }
}
