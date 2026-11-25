package app.attic.player.playback

import app.attic.player.data.Album
import app.attic.player.data.LibrarySearch.matching
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track

/**
 * A spoken "play …" request. [query] is what was said after "play"; the other fields are the hints the
 * assistant adds when it understood the request (MediaStore's EXTRA_MEDIA_FOCUS, ARTIST, ALBUM and so on).
 */
data class VoiceRequest(
    val query: String,
    val focus: Focus = Focus.Any,
    val artist: String? = null,
    val album: String? = null,
    val title: String? = null,
    val playlist: String? = null,
    val genre: String? = null,
) {
    enum class Focus { Any, Artist, Album, Song, Playlist, Genre }

    companion object {
        /** Maps MediaStore's EXTRA_MEDIA_FOCUS values, the entry MIME types of artists, albums and so on. */
        fun focusOf(mime: String?): Focus = when (mime) {
            "vnd.android.cursor.item/artist" -> Focus.Artist
            "vnd.android.cursor.item/album" -> Focus.Album
            "vnd.android.cursor.item/audio" -> Focus.Song
            "vnd.android.cursor.item/playlist" -> Focus.Playlist
            "vnd.android.cursor.item/genre" -> Focus.Genre
            else -> Focus.Any
        }
    }
}

sealed interface VoiceResult {
    /** "Play music" with nothing named: carry on with the saved queue, or shuffle everything. */
    data object Resume : VoiceResult

    data class Play(val request: PlayRequest) : VoiceResult

    data object NotFound : VoiceResult
}

/**
 * Turns "play X" into a queue. With a focus hint it looks only where the hint points; without one it
 * tries, in order, a playlist name, an artist, an album, then a song title, preferring an exact name to
 * one that only starts with or contains the words. "X by Y" narrows a song or album to that artist.
 */
object VoiceSearch {
    fun resolve(request: VoiceRequest, snapshot: BrowseSnapshot): VoiceResult {
        val query = clean(request.query)
        val named = listOfNotNull(request.artist, request.album, request.title, request.playlist, request.genre).any { it.isNotBlank() }
        if (query.isEmpty() && !named) return VoiceResult.Resume

        val play = when (request.focus) {
            VoiceRequest.Focus.Artist -> artist(request.artist ?: query, snapshot)
            VoiceRequest.Focus.Album -> album(request.album ?: query, request.artist, snapshot)
            VoiceRequest.Focus.Song -> song(request.title ?: query, request.artist, snapshot)
            VoiceRequest.Focus.Playlist -> playlist(request.playlist ?: query, snapshot)
            VoiceRequest.Focus.Genre -> genre(request.genre ?: query, snapshot)
            VoiceRequest.Focus.Any -> anything(query, snapshot)
        } ?: anything(query, snapshot).takeIf { request.focus != VoiceRequest.Focus.Any }
        return play?.let(VoiceResult::Play) ?: VoiceResult.NotFound
    }

    /** Drops the words around a name that an assistant can pass through: "play", "some", "on Attic", "songs". */
    internal fun clean(query: String): String {
        var q = TitleCleaner.searchable(query).trim()
        q = q.removePrefix("play ").trim()
        q = Regex("\\s+(on|in|from|using|with) attic$").replace(q, "")
        q = Regex("^(some|my|the songs of|songs by|music by|songs from|music from)\\s+").replace(q, "")
        q = Regex("\\s+(songs|music)$").replace(q, "")
        return if (q in GENERIC) "" else q
    }

    private fun anything(query: String, snapshot: BrowseSnapshot): PlayRequest? {
        if (query.isEmpty()) return null
        byArtist(query)?.let { (name, artist) ->
            (album(name, artist, snapshot, exactOnly = true) ?: song(name, artist, snapshot, exactOnly = true))?.let { return it }
        }
        return playlist(query, snapshot, exactOnly = true)
            ?: artist(query, snapshot, exactOnly = true)
            ?: album(query, null, snapshot, exactOnly = true)
            ?: song(query, null, snapshot, exactOnly = true)
            ?: playlist(query, snapshot)
            ?: artist(query, snapshot)
            ?: album(query, null, snapshot)
            ?: song(query, null, snapshot)
            ?: byArtist(query)?.let { (name, artist) -> album(name, artist, snapshot) ?: song(name, artist, snapshot) }
    }

    /** "X by Y" split at its last " by ", so a title that itself contains "by" still works. */
    private fun byArtist(query: String): Pair<String, String>? {
        val at = query.lastIndexOf(" by ")
        if (at <= 0) return null
        return query.substring(0, at).trim() to query.substring(at + 4).trim()
    }

    private fun artist(name: String, snapshot: BrowseSnapshot, exactOnly: Boolean = false): PlayRequest? {
        val artist = best(snapshot.library.artists, name, exactOnly) { it.name } ?: return null
        val tracks = snapshot.artistTracks(artist.key)
        return PlayRequest(tracks, 0, artist.name).takeIf { tracks.isNotEmpty() }
    }

    private fun album(name: String, artist: String?, snapshot: BrowseSnapshot, exactOnly: Boolean = false): PlayRequest? {
        val candidates = snapshot.library.albums.filter { artist == null || byArtistNamed(it, artist) }
        val album = best(candidates, name, exactOnly) { it.name } ?: return null
        return PlayRequest(album.tracks, 0, album.name).takeIf { album.tracks.isNotEmpty() }
    }

    /** One song, which radio carries on from when it ends (if autoplay is on). */
    private fun song(name: String, artist: String?, snapshot: BrowseSnapshot, exactOnly: Boolean = false): PlayRequest? {
        val candidates = snapshot.library.tracks.filter { artist == null || isBy(it, artist) }
        val track = best(candidates, name, exactOnly) { it.title } ?: return null
        return PlayRequest(listOf(track), 0, null)
    }

    private fun playlist(name: String, snapshot: BrowseSnapshot, exactOnly: Boolean = false): PlayRequest? {
        val q = TitleCleaner.searchable(name)
        if (q in FAVOURITE_NAMES) return snapshot.liked.takeIf { it.isNotEmpty() }?.let { PlayRequest(it, 0, BrowseTree.FAVOURITES) }
        val own = snapshot.playlists.filter { snapshot.playlistTracks(it).isNotEmpty() }
        best(own, name, exactOnly) { it.name }?.let { return PlayRequest(snapshot.playlistTracks(it), 0, it.name) }
        return best(snapshot.library.folders, name, exactOnly) { it.name }?.let { PlayRequest(it.tracks, 0, it.name) }
    }

    /** Every song tagged with the genre, shuffled. */
    private fun genre(name: String, snapshot: BrowseSnapshot): PlayRequest? {
        val q = TitleCleaner.searchable(name)
        if (q.isEmpty()) return null
        val tracks = snapshot.library.tracks.filter { track -> track.genre?.let { TitleCleaner.searchable(it).contains(q) } == true }
        if (tracks.isEmpty()) return null
        return PlayRequest(tracks, tracks.indices.random(), tracks.first().genre, shuffled = true)
    }

    private fun <T> best(items: List<T>, name: String, exactOnly: Boolean, nameOf: (T) -> String): T? {
        val q = TitleCleaner.searchable(name)
        if (q.isEmpty()) return null
        items.firstOrNull { TitleCleaner.searchable(nameOf(it)) == q }?.let { return it }
        if (exactOnly) return null
        return items.matching(q, nameOf).firstOrNull()
    }

    private fun isBy(track: Track, artist: String): Boolean {
        val q = TitleCleaner.searchable(artist)
        return TitleCleaner.searchable(track.artist).contains(q) || track.artistKeys.any { it == TitleCleaner.normalize(artist) }
    }

    private fun byArtistNamed(album: Album, artist: String): Boolean {
        val q = TitleCleaner.searchable(artist)
        return TitleCleaner.searchable(album.artist).contains(q) || album.tracks.any { isBy(it, artist) }
    }

    private val GENERIC = setOf("", "music", "songs", "something", "anything", "a song", "my music", "some music", "attic")
    private val FAVOURITE_NAMES = setOf("favourites", "favorites", "my favourites", "my favorites", "liked songs", "my liked songs", "liked")
}
