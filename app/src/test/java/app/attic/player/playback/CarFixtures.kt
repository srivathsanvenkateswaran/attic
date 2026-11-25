package app.attic.player.playback

import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.UserPlaylist

/** A small library for the car tests: two artists, three albums, one folder playlist. */
internal object CarFixtures {
    private var nextId = 1L

    private fun track(key: String, title: String, artist: String, album: String?, albumKey: String?, artistKey: String, genre: String? = null, folder: String? = null) =
        Track(
            key = key,
            mediaId = nextId++,
            title = title,
            artist = artist,
            artistKeys = listOf(artistKey),
            album = album,
            albumKey = albumKey,
            folder = folder,
            trackNumber = null,
            durationMs = 180_000,
            year = null,
            dateAddedSec = 0,
            genre = genre,
        )

    val yellow = track("Music/Coldplay/Parachutes/Yellow.mp3", "Yellow", "Coldplay", "Parachutes", "parachutes|coldplay", "coldplay", genre = "Rock")
    val trouble = track("Music/Coldplay/Parachutes/Trouble.mp3", "Trouble", "Coldplay", "Parachutes", "parachutes|coldplay", "coldplay", genre = "Rock")
    val clocks = track("Music/Coldplay/A Rush of Blood/Clocks.mp3", "Clocks", "Coldplay", "A Rush of Blood to the Head", "arushofbloodtothehead|coldplay", "coldplay", genre = "Rock")
    val kesariya = track("Music/Arijit Singh/Brahmastra/Kesariya.mp3", "Kesariya", "Arijit Singh", "Brahmastra", "brahmastra|arijitsingh", "arijitsingh", genre = "Filmi")
    val tumHi = track("Music/Bollywood/Tum Hi Ho.mp3", "Tum Hi Ho", "Arijit Singh", null, null, "arijitsingh", genre = "Filmi", folder = "Bollywood")
    /** A path with the characters a media id must survive: '|', '%', '+', '/' and non-Latin text. */
    val odd = track("Music/Odd %20 + | Folder/ஒரு பாடல்.mp3", "ஒரு பாடல்", "Arijit Singh", "Brahmastra", "brahmastra|arijitsingh", "arijitsingh")

    private val parachutes = Album("parachutes|coldplay", "Parachutes", "Coldplay", "coldplay", listOf(yellow, trouble), 2000)
    private val rush = Album("arushofbloodtothehead|coldplay", "A Rush of Blood to the Head", "Coldplay", "coldplay", listOf(clocks), 2002)
    private val brahmastra = Album("brahmastra|arijitsingh", "Brahmastra", "Arijit Singh", "arijitsingh", listOf(kesariya, odd), 2022)
    private val coldplay = Artist("coldplay", "Coldplay", listOf(yellow, trouble, clocks), listOf(parachutes, rush))
    private val arijit = Artist("arijitsingh", "Arijit Singh", listOf(kesariya, odd, tumHi), listOf(brahmastra))
    private val bollywood = FolderPlaylist("Bollywood", listOf(tumHi))

    val library = Library(
        tracks = listOf(yellow, trouble, clocks, kesariya, tumHi, odd),
        albums = listOf(parachutes, rush, brahmastra),
        artists = listOf(coldplay, arijit),
        folders = listOf(bollywood),
    )

    val roadTrip = UserPlaylist(7, "Road trip", listOf(clocks.key, kesariya.key, "Music/Gone.mp3"))
    val empty = UserPlaylist(8, "Empty one", emptyList())

    fun snapshot(
        liked: List<Track> = listOf(kesariya, yellow),
        recent: List<Track> = listOf(clocks, yellow),
        mixes: List<Track> = listOf(trouble),
    ) = BrowseSnapshot(
        library = library,
        likedKeys = liked.map { it.key },
        playlists = listOf(roadTrip, empty),
        recentKeys = recent.map { it.key },
        mixes = mixes,
    )
}
