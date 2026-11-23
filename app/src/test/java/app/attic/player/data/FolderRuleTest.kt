package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which top folders become artists and which become folder playlists, on layouts other than the owner's. */
class FolderRuleTest {
    private var nextId = 1L

    private fun raw(
        path: String,
        file: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        albumArtist: String? = null,
        composer: String? = null,
    ) = RawTrack(nextId++, file, path, title, artist, album, null, 200_000, null, 0, albumArtist = albumArtist, composer = composer)

    @Test
    fun `a folder named after most of its songs' artist is an artist folder`() {
        val rows = listOf(
            raw("Music/Arijit Singh/", "a.mp3", "Kesariya", "Arijit Singh", "Brahmastra"),
            raw("Music/Arijit Singh/", "b.mp3", "Tum Hi Ho", "Arijit Singh", "Aashiqui 2"),
            raw("Music/Arijit Singh/", "c.mp3", "Apna Bana Le", "Sachin-Jigar", "Bhediya"),
        )
        assertEquals(FolderKind.Artist, LibraryScope.folderKind("Arijit Singh", rows))
    }

    @Test
    fun `a folder whose name only some songs credit is a playlist`() {
        val rows = listOf(
            raw("Music/Arijit Singh/", "a.mp3", "Kesariya", "Arijit Singh", "Brahmastra"),
            raw("Music/Arijit Singh/", "b.mp3", "Believer", "Imagine Dragons", "Evolve"),
            raw("Music/Arijit Singh/", "c.mp3", "Levitating", "Dua Lipa", "Future Nostalgia"),
        )
        assertEquals(FolderKind.Playlist, LibraryScope.folderKind("Arijit Singh", rows))
    }

    @Test
    fun `album artist and composer tags count as crediting the folder`() {
        val byAlbumArtist = listOf(
            raw("Music/Pritam/Jawan/", "a.mp3", "Chaleya", "Arijit Singh, Shilpa Rao", "Jawan", albumArtist = "Pritam"),
            raw("Music/Pritam/Jawan/", "b.mp3", "Zinda Banda", "Anirudh", "Jawan", albumArtist = "Pritam"),
        )
        assertEquals(FolderKind.Artist, LibraryScope.folderKind("Pritam", byAlbumArtist))
        val byComposer = listOf(
            raw("Music/Ilaiyaraaja/", "a.mp3", "Ilaya Nila", "S. P. Balasubrahmanyam", "Payanangal", composer = "Ilaiyaraaja"),
            raw("Music/Ilaiyaraaja/", "b.mp3", "Raja Raja Chozhan", "K. J. Yesudas", "Rettai Vaal", composer = "Ilaiyaraaja"),
        )
        assertEquals(FolderKind.Artist, LibraryScope.folderKind("Ilaiyaraaja", byComposer))
    }

    @Test
    fun `a folder with no artist tags at all is an artist folder`() {
        val rows = listOf(raw("Music/Coldplay/", "Yellow.mp3"), raw("Music/Coldplay/", "Fix You.mp3", "Fix You", "<unknown>"))
        assertEquals(FolderKind.Artist, LibraryScope.folderKind("Coldplay", rows))
    }

    @Test
    fun `various artists is neither an artist nor a playlist`() {
        val rows = listOf(raw("Music/Various Artists/Tamasha/", "a.mp3", "Matargashti", "Mohit Chauhan", "Tamasha"))
        assertEquals(FolderKind.Various, LibraryScope.folderKind("Various Artists", rows))
        val lib = LibraryBuilder.build(rows)
        assertTrue(lib.folders.isEmpty())
        assertEquals(listOf("Mohit Chauhan"), lib.artists.map { it.name })
        assertEquals("Various Artists", lib.albums.single().artist)
    }

    @Test
    fun `themed folders in a stranger's library become playlists, not artists`() {
        val rows = listOf(
            raw("Music/Bollywood/", "Kesariya.mp3", "Kesariya", "Arijit Singh", "Brahmastra"),
            raw("Music/Bollywood/", "Chaleya.mp3", "Chaleya", "Arijit Singh, Shilpa Rao", "Jawan"),
            raw("Music/Bollywood/", "Jhoome Jo Pathaan.mp3", "Jhoome Jo Pathaan", "Vishal-Shekhar", "Pathaan"),
            raw("Music/Workout/", "Believer.mp3", "Believer", "Imagine Dragons", "Evolve"),
            raw("Music/Workout/", "Till I Collapse.mp3", "Till I Collapse", "Eminem", "The Eminem Show"),
            raw("Music/Coldplay/Parachutes/", "Yellow.mp3", "Yellow", "Coldplay", "Parachutes"),
        )
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Bollywood" to 3, "Workout" to 2), lib.folders.map { it.name to it.tracks.size })
        val names = lib.artists.map { it.name }
        assertFalse("Bollywood" in names)
        assertFalse("Workout" in names)
        assertTrue(names.containsAll(listOf("Arijit Singh", "Coldplay", "Eminem", "Imagine Dragons")))
        // A song in a playlist folder keeps its tagged album.
        assertEquals("Brahmastra", lib.tracks.single { it.title == "Kesariya" }.album)
        assertEquals("Arijit Singh", lib.tracks.single { it.title == "Kesariya" }.artist)
    }

    @Test
    fun `a downloader's folder next to other downloads is a playlist`() {
        val rows = listOf(
            raw("Download/", "Levitating.mp3", "Levitating", "Dua Lipa", "Download"),
            raw("Download/Snaptube/", "Believer.mp3", "Believer", "Imagine Dragons", "Snaptube"),
            raw("Download/Snaptube/", "Shape of You.mp3", "Ed Sheeran - Shape of You", "Ed Sheeran", "Snaptube"),
        )
        assertEquals("Download/", LibraryScope.roots(rows).getValue(rows[1]))
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Snaptube"), lib.folders.map { it.name })
        assertFalse("Snaptube" in lib.artists.map { it.name })
        assertTrue(lib.albums.none { it.name == "Snaptube" })
    }

    @Test
    fun `a folder of loose songs holding the whole library stays a playlist instead of becoming the root`() {
        val rows = listOf(
            raw("Download/Snaptube/", "Believer.mp3", "Believer", "Imagine Dragons", "Snaptube"),
            raw("Download/Snaptube/", "Shape of You.mp3", "Ed Sheeran - Shape of You", "Ed Sheeran", "Snaptube"),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Download/" })
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Snaptube"), lib.folders.map { it.name })
        assertEquals(listOf("Ed Sheeran"), lib.artists.map { it.name })
    }

    @Test
    fun `an album-less song in a playlist folder is read as a download, so a bare tag artist gets no page`() {
        // A download's artist tag is often the uploader's channel. Only an artist the title names gets a page.
        val lib = LibraryBuilder.build(
            listOf(
                raw("Music/Gym/", "Believer.mp3", "Believer", "Imagine Dragons", "Gym"),
                raw("Music/Gym/", "Lose Yourself.mp3", "Eminem - Lose Yourself (Official Video)", "EminemVEVO", "Gym"),
            ),
        )
        assertEquals("Imagine Dragons", lib.tracks.single { it.title == "Believer" }.artist)
        assertEquals("Eminem", lib.tracks.single { it.title == "Lose Yourself" }.artist)
        assertEquals(listOf("Eminem"), lib.artists.map { it.name })
    }

    @Test
    fun `a wrapper around artist folders is the root and its folders are read below it`() {
        val rows = listOf(
            raw("Music/Songs/Coldplay/Parachutes/", "Yellow.mp3", "Yellow", "Coldplay", "Parachutes"),
            raw("Music/Songs/Adele/25/", "Hello.mp3", "Hello", "Adele", "25"),
            raw("Music/Songs/Gym/", "Believer.mp3", "Believer", "Imagine Dragons", "Gym"),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Music/Songs/" })
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Gym"), lib.folders.map { it.name })
        assertEquals(listOf("Adele", "Coldplay"), lib.artists.map { it.name })
    }

    @Test
    fun `untagged songs in a playlist folder keep the cleaned filename and no artist page`() {
        val rows = listOf(
            raw("Music/Bollywood/", "Gulabi [kTpyO8iwlSY].mp3", null, null, "Bollywood"),
            raw("Music/Bollywood/", "Kesariya.mp3", "Kesariya", "Arijit Singh", "Brahmastra"),
        )
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Bollywood"), lib.folders.map { it.name })
        val gulabi = lib.tracks.single { it.title == "Gulabi" }
        assertEquals("Unknown artist", gulabi.artist)
        assertNull(gulabi.album)
        assertTrue(gulabi.artistKeys.isEmpty())
    }
}
