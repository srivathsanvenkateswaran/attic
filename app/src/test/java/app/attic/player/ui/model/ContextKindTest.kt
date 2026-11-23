package app.attic.player.ui.model

import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextKindTest {
    private val song = Track(
        key = "k1", mediaId = 1, title = "Jaan Se Guzarte Hain", artist = "Khan Saab, Shashwat Sachdev",
        artistKeys = listOf("khan", "shashwat"), album = "Dhurandhar: The Revenge", albumKey = "revenge",
        folder = "Hindi", trackNumber = 1, durationMs = 1000, year = 2026, dateAddedSec = 0,
    )
    private val other = song.copy(key = "k2", artistKeys = listOf("arijit"), album = "Tum Hi Ho", albumKey = "tum", folder = "Tamil")
    private val library = Library(
        tracks = listOf(song, other),
        albums = listOf(Album("revenge", "Dhurandhar: The Revenge", "Khan Saab", "khan", listOf(song), 2026), Album("tum", "Tum Hi Ho", "Arijit Singh", "arijit", listOf(other), null)),
        artists = listOf(
            Artist("khan", "Khan Saab", listOf(song), emptyList()),
            Artist("shashwat", "Shashwat Sachdev", listOf(song), emptyList()),
            Artist("arijit", "Arijit Singh", listOf(other), emptyList()),
        ),
        folders = listOf(FolderPlaylist("Hindi", listOf(song)), FolderPlaylist("Tamil", listOf(other))),
    )

    private fun kind(name: String?, playlists: List<String> = emptyList()) = contextKindOf(name, song, library, playlists)

    @Test
    fun fixedNamesAreRecognised() {
        assertEquals(ContextKind.Radio, kind("Radio · Yellow"))
        assertEquals(ContextKind.Favourites, kind("Favourites"))
        assertEquals(ContextKind.Other, kind("Recently played"))
        assertEquals(ContextKind.Other, kind("All songs"))
        assertEquals(ContextKind.Other, kind(null))
    }

    @Test
    fun aQueueSavedUnderTheOldFavouritesNameIsStillFavourites() {
        assertEquals(ContextKind.Favourites, kind("Liked Songs"))
        assertTrue(isSameContext("Liked Songs", "Favourites"))
        assertTrue(isSameContext("Favourites", "Favourites"))
        assertTrue(isSameContext("Hindi", "Hindi"))
        assertFalse(isSameContext("Liked Songs", "Hindi"))
        assertFalse(isSameContext(null, "Favourites"))
    }

    @Test
    fun theSongsOwnCollectionsComeFirst() {
        assertEquals(ContextKind.Album, kind("Dhurandhar: The Revenge"))
        assertEquals(ContextKind.Artist, kind("Shashwat Sachdev"))
        assertEquals(ContextKind.Folder, kind("Hindi"))
    }

    @Test
    fun playlistsAndTheRestOfTheLibraryFollow() {
        assertEquals(ContextKind.Playlist, kind("Road trip", listOf("Road trip")))
        assertEquals(ContextKind.Album, kind("Tum Hi Ho"))
        assertEquals(ContextKind.Artist, kind("Arijit Singh"))
        assertEquals(ContextKind.Folder, kind("Tamil"))
        assertEquals(ContextKind.Other, kind("Something else"))
    }
}
