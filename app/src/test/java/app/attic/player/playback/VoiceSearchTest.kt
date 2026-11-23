package app.attic.player.playback

import app.attic.player.playback.CarFixtures.clocks
import app.attic.player.playback.CarFixtures.kesariya
import app.attic.player.playback.CarFixtures.trouble
import app.attic.player.playback.CarFixtures.yellow
import app.attic.player.playback.VoiceRequest.Focus
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSearchTest {
    private val snapshot = CarFixtures.snapshot()

    private fun play(query: String, focus: Focus = Focus.Any, artist: String? = null, album: String? = null, title: String? = null, genre: String? = null): PlayRequest {
        val result = VoiceSearch.resolve(VoiceRequest(query, focus, artist, album, title, genre = genre), snapshot)
        return (result as? VoiceResult.Play)?.request ?: throw AssertionError("\"$query\" gave $result")
    }

    @Test
    fun `nothing named resumes`() {
        assertEquals(VoiceResult.Resume, VoiceSearch.resolve(VoiceRequest(""), snapshot))
        assertEquals(VoiceResult.Resume, VoiceSearch.resolve(VoiceRequest("some music"), snapshot))
        assertEquals(VoiceResult.Resume, VoiceSearch.resolve(VoiceRequest("music on Attic"), snapshot))
    }

    @Test
    fun `an artist's name plays the artist`() {
        val request = play("coldplay")
        assertEquals("Coldplay", request.contextName)
        assertEquals(listOf(clocks, yellow, trouble), request.tracks)
    }

    @Test
    fun `an album name plays the album from the start`() {
        val request = play("Parachutes")
        assertEquals("Parachutes", request.contextName)
        assertEquals(listOf(yellow, trouble), request.tracks)
        assertEquals(0, request.startIndex)
    }

    @Test
    fun `a song title plays that song alone, for radio to carry on from`() {
        val request = play("Kesariya")
        assertEquals(listOf(kesariya), request.tracks)
        assertEquals(null, request.contextName)
    }

    @Test
    fun `X by Y finds the song by that artist`() {
        assertEquals(listOf(clocks), play("clocks by coldplay").tracks)
        assertEquals("Parachutes", play("parachutes by Coldplay").contextName)
    }

    @Test
    fun `playlists, folder playlists and Favourites play by name`() {
        assertEquals("Road trip", play("road trip").contextName)
        assertEquals("Bollywood", play("Bollywood").contextName)
        val favourites = play("my favorites")
        assertEquals(BrowseTree.FAVOURITES, favourites.contextName)
        assertEquals(listOf(kesariya, yellow), favourites.tracks)
        assertEquals(BrowseTree.FAVOURITES, play("liked songs").contextName)
    }

    @Test
    fun `focus hints from the assistant look only where they point`() {
        assertEquals("Coldplay", play("coldplay", Focus.Artist, artist = "Coldplay").contextName)
        assertEquals("Brahmastra", play("brahmastra", Focus.Album, album = "Brahmastra", artist = "Arijit Singh").contextName)
        assertEquals(listOf(trouble), play("trouble", Focus.Song, title = "Trouble").tracks)
        val rock = play("rock", Focus.Genre, genre = "Rock")
        assertEquals(true, rock.shuffled)
        assertEquals(setOf(yellow, trouble, clocks), rock.tracks.toSet())
    }

    @Test
    fun `an exact name beats a partial one, and partial names still match`() {
        // "Trouble" is a song; nothing is named exactly "troub", so the song still plays.
        assertEquals(listOf(trouble), play("troub").tracks)
        // "A Rush of Blood to the Head" by a partial name.
        assertEquals("A Rush of Blood to the Head", play("rush of blood").contextName)
    }

    @Test
    fun `accents and case don't matter`() {
        assertEquals("Coldplay", play("CÓLDPLAY").contextName)
    }

    @Test
    fun `nothing in the library matches`() {
        assertEquals(VoiceResult.NotFound, VoiceSearch.resolve(VoiceRequest("the beatles"), snapshot))
    }

    @Test
    fun `filler words around the name are dropped`() {
        assertEquals("coldplay", VoiceSearch.clean("Play songs by Coldplay on Attic"))
        assertEquals("parachutes", VoiceSearch.clean("parachutes"))
        assertEquals("", VoiceSearch.clean("play music"))
    }

    @Test
    fun `focus MIME types map to what was asked for`() {
        assertEquals(Focus.Artist, VoiceRequest.focusOf("vnd.android.cursor.item/artist"))
        assertEquals(Focus.Album, VoiceRequest.focusOf("vnd.android.cursor.item/album"))
        assertEquals(Focus.Song, VoiceRequest.focusOf("vnd.android.cursor.item/audio"))
        assertEquals(Focus.Playlist, VoiceRequest.focusOf("vnd.android.cursor.item/playlist"))
        assertEquals(Focus.Genre, VoiceRequest.focusOf("vnd.android.cursor.item/genre"))
        assertEquals(Focus.Any, VoiceRequest.focusOf("vnd.android.cursor.item/*"))
        assertEquals(Focus.Any, VoiceRequest.focusOf(null))
    }
}
