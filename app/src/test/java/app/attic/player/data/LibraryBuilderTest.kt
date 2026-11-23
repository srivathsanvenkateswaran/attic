package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBuilderTest {
    private var nextId = 1L

    private fun raw(
        path: String,
        file: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        track: String? = null,
        albumArtist: String? = null,
        composer: String? = null,
        genre: String? = null,
    ) = RawTrack(
        nextId++, file, "Music/Offline/$path", title, artist, album, track, 1000, null, 0,
        albumArtist = albumArtist, composer = composer, genre = genre,
    )

    /** These tests are about tag cleanup, so they fix the owner's root instead of detecting one. */
    private fun build(rows: List<RawTrack>) = LibraryBuilder.build(rows, root = "Music/Offline/")

    private fun rip(path: String, title: String, artist: String) =
        build(listOf(raw("$path/", "x [kTpyO8iwlSY].mp3", title, artist, path))).tracks.single()

    @Test
    fun `strips only eleven character youtube ids`() {
        assertEquals("Gulabi", TitleCleaner.stripYoutubeId("Gulabi [kTpyO8iwlSY]"))
        assertEquals("Sapphire [Remix]", TitleCleaner.stripYoutubeId("Sapphire [Remix]"))
        assertEquals("X [Official Video]", TitleCleaner.stripYoutubeId("X [Official Video]"))
    }

    @Test
    fun `missing tags fall back to the cleaned filename and folder artist`() {
        val t = build(
            listOf(raw("Coldplay/Parachutes/", "05 Yellow [kTpyO8iwlSY].mp3", null, "<unknown>", "Parachutes")),
        ).tracks.single()
        assertEquals("Yellow", t.title)
        assertEquals("Coldplay", t.artist)
        assertEquals(5, t.trackNumber)
    }

    @Test
    fun `title that mediastore copied from the filename is cleaned like a filename`() {
        val t = build(
            listOf(raw("Test Artist/Test Album/", "07 Fallback Song.mp3", "07 Fallback Song", "<unknown>", "Test Album")),
        ).tracks.single()
        assertEquals("Fallback Song", t.title)
        assertEquals("Test Artist", t.artist)
        assertEquals(7, t.trackNumber)
    }

    @Test
    fun `untagged file in a themed folder has unknown artist and no album`() {
        val lib = build(
            listOf(
                raw("Hindi/", "Gulabi [kTpyO8iwlSY].mp3", null, null, "Hindi"),
                raw("Hindi/", "Kesariya [kTpyO8iwlSY].mp3", "Kesariya", "T-Series", "Hindi"),
            ),
        )
        assertEquals(listOf("Hindi"), lib.folders.map { it.name })
        val t = lib.tracks.single { it.title == "Gulabi" }
        assertEquals("Unknown artist", t.artist)
        assertNull(t.album)
        assertTrue(t.artistKeys.isEmpty())
    }

    @Test
    fun `album placeholder from mediastore is not an album`() {
        val lib = build(listOf(raw("English/", "a.mp3", "Avicii - The Nights", "Avicii", "English")))
        assertTrue(lib.albums.isEmpty())
        assertEquals(listOf("English"), lib.folders.map { it.name })
    }

    @Test
    fun `themed folders are playlists not artists`() {
        val lib = build(
            listOf(
                raw("English/", "a.mp3", "Avicii - The Nights", "Avicii", "English"),
                raw("Phonk/", "b.mp3", "KORDHELL - SOUL", "KORDHELL", "Phonk"),
            ),
        )
        assertEquals(listOf("English", "Phonk"), lib.folders.map { it.name })
        val names = lib.artists.map { it.name }
        assertFalse("English" in names)
        assertFalse("Phonk" in names)
        assertTrue("Avicii" in names)
    }

    @Test
    fun `various artists is not an artist but its performers are`() {
        val lib = build(
            listOf(raw("Various Artists/Tamasha/", "x.mp3", "AGAR TUM SAATH HO", "Alka Yagnik & Arijit Singh", "Tamasha")),
        )
        assertEquals(listOf("Alka Yagnik", "Arijit Singh"), lib.artists.map { it.name })
        assertEquals("Various Artists", lib.albums.single().artist)
        assertNull(lib.albums.single().artistKey)
    }

    @Test
    fun `artist spellings merge and folder name wins`() {
        val lib = build(
            listOf(
                raw("A. R. Rahman/Roja/", "a.mp3", "Song", "A. R. Rahman & Naresh Iyer", "Roja"),
                raw("Hindi/", "b.mp3", "Other", "RASHID ALI, A.R. RAHMAN", "Jaane Tu"),
                raw("Tamil/", "c.mp3", "Third", "A.R. Rahman, Sid Sriram", "Pathu Thala"),
            ),
        )
        val rahman = lib.artists.single { it.key == TitleCleaner.normalize("A R Rahman") }
        assertEquals("A. R. Rahman", rahman.name)
        assertEquals(3, rahman.tracks.size)
    }

    @Test
    fun `folder stand-in characters defer to the tagged artist spelling`() {
        val lib = build(
            listOf(raw("Axwell -- Ingrosso/More Than You Know/", "a.mp3", "More Than You Know", """Axwell /\ Ingrosso, Sebastian Ingrosso""", "More Than You Know")),
        )
        assertTrue("""Axwell /\ Ingrosso""" in lib.artists.map { it.name })
        assertEquals("""Axwell /\ Ingrosso""", lib.albums.single().artist)
    }

    @Test
    fun `themed single joins the full album of the same name`() {
        val lib = build(
            listOf(
                raw("Various Artists/Yeh Jawaani Hai Deewani/", "a.mp3", "Balam Pichkari", "Vishal Dadlani", "Yeh Jawaani Hai Deewani"),
                raw("Hindi/", "b [pnJriqNxpcc].mp3", "Subhanallah", "Pritam, Sreeram", "Yeh Jawaani Hai Deewani"),
            ),
        )
        assertEquals(1, lib.albums.size)
        assertEquals(2, lib.albums.single().tracks.size)
        assertEquals("Various Artists", lib.albums.single().artist)
    }

    @Test
    fun `album sorts by track number and ignores a junk number shared by all`() {
        val ordered = build(
            listOf(
                raw("S/D/", "11 Ramba Ho.mp3", "Ramba Ho", "M", "D", "11"),
                raw("S/D/", "05 Run.mp3", "Run", "R", "D", "5"),
            ),
        ).albums.single().tracks.map { it.title }
        assertEquals(listOf("Run", "Ramba Ho"), ordered)

        val junk = build(
            listOf(raw("H/A/", "b.mp3", "B", "H", "A", "7"), raw("H/A/", "a.mp3", "A", "H", "A", "7")),
        ).albums.single().tracks
        assertEquals(listOf("A", "B"), junk.map { it.title })
        assertNull(junk[0].trackNumber)
    }

    @Test
    fun `track number past any real album length is discarded`() {
        val t = build(listOf(raw("H/A/", "a.mp3", "A", "H", "A", "63"))).tracks.single()
        assertNull(t.trackNumber)
    }

    @Test
    fun `feat in an artist tag splits credits and repeated names are dropped`() {
        val lib = build(
            listOf(
                raw("The Weeknd/Starboy/", "a.mp3", "Die For You", "The Weeknd feat. Anitta", "Starboy"),
                raw("Hindi/", "b.mp3", "Ajab Si", "Vishal-Shekhar, K.K., Vishal-Shekhar", "Om Shanti Om"),
            ),
        )
        assertTrue(lib.artists.map { it.name }.containsAll(listOf("The Weeknd", "Anitta")))
        assertEquals("Vishal-Shekhar, K.K.", lib.tracks.single { it.title == "Ajab Si" }.artist)
    }

    @Test
    fun `tagged title drops a redundant artist prefix but keeps real dashes`() {
        val lib = build(
            listOf(
                raw("OneRepublic/Top Gun/", "a.mp3", "OneRepublic - I Ain't Worried", "OneRepublic", "Top Gun"),
                raw("S/D/", "b.mp3", "Run Down the City - Monica", "Reble", "D"),
            ),
        )
        assertTrue("I Ain't Worried" in lib.tracks.map { it.title })
        assertTrue("Run Down the City - Monica" in lib.tracks.map { it.title })
    }

    @Test
    fun `rip title artist replaces uploader channel`() {
        val t = rip("English", "Coldplay - Hymn For The Weekend (Lyrics)", "DopeMusic")
        assertEquals("Hymn For The Weekend", t.title)
        assertEquals("Coldplay", t.artist)
    }

    @Test
    fun `rip removes video noise and keeps meaningful brackets`() {
        assertEquals("The Scientist", rip("English", "Coldplay - The Scientist (Official 4K Video)", "Coldplay").title)
        assertEquals("STEP ON UP", rip("English", "ARIANA GRANDE - STEP ON UP(LYRICS)", "VIBES").title)
        assertEquals(
            "Waka Waka (This Time for Africa) (The Official 2010 FIFA World Cup™ Song)",
            rip("English", "Shakira - Waka Waka (This Time for Africa) (The Official 2010 FIFA World Cup™ Song)", "Shakira").title,
        )
        assertEquals("Celestial (Steerner Remix)", rip("English", "Ed Sheeran - Celestial (Steerner Remix)", "Steerner").title)
    }

    @Test
    fun `rip credits collaborators and featured artists`() {
        val lib = build(
            listOf(
                raw("English/", "a.mp3", "Sean Paul - No Lie ft. Dua Lipa", "Sean Paul", "English"),
                raw("Phonk/", "b.mp3", "KORDHELL x DUDEPLAYA - SOUL", "KORDHELL", "Phonk"),
                raw("English/", "c.mp3", "Charlie Puth - We Don't Talk Anymore (feat. Selena Gomez) [Official Video]", "Charlie Puth", "English"),
            ),
        )
        val names = lib.artists.map { it.name }
        assertTrue(names.containsAll(listOf("Sean Paul", "Dua Lipa", "KORDHELL", "DUDEPLAYA", "Charlie Puth", "Selena Gomez")))
        assertEquals(
            "We Don't Talk Anymore (feat. Selena Gomez)",
            lib.tracks.single { it.artist == "Charlie Puth" }.title,
        )
    }

    @Test
    fun `uploader on the right means song on the left`() {
        val t = rip("Phonk", "Nada Nada - Jmilton", "Jmilton")
        assertEquals("Nada Nada", t.title)
        assertEquals("Jmilton", t.artist)
        val n = rip("English", "DIA DELÍCIA - NAKAMA (Official Music Video)", "Nakama")
        assertEquals("DIA DELÍCIA", n.title)
        assertEquals("Nakama", n.artist)
    }

    @Test
    fun `pipe separated film uploads take the song and keep the tag artist`() {
        val t = rip("Tamil", "Maryan - Sonapareeya Video | Dhanush, Parvathy Menon | Rahman", "SonyMusicSouthVEVO")
        assertEquals("Sonapareeya", t.title)
        assertEquals("SonyMusicSouthVEVO", t.artist)
        assertTrue(t.artistKeys.isEmpty())
        assertEquals("Illuminati", rip("Tamil", "Illuminati|Aavesham|Jithu Madhavan", "Think Music India").title)
        assertEquals("The Life of Ram", rip("Tamil", "The Life of Ram Video Song | 96 | Vijay Sethupathi", "Think Music India").title)
        assertEquals("Maya Nadhi", rip("Tamil", "Kabali Songs | Maya Nadhi Video Song | Rajinikanth", "Think Music India").title)
        assertEquals("K For Krishna", rip("Tamil", "K For Krishna - Video Song | Guruvayoorambala Nadayil", "Saregama Malayalam").title)
    }

    @Test
    fun `uploader channels without a parsed artist never become browsable artists`() {
        val lib = build(listOf(raw("Phonk/", "a.mp3", "miguel phonk", "Adrenaline Rush 🅥 ", "Phonk")))
        assertTrue(lib.artists.isEmpty())
        assertEquals("Adrenaline Rush 🅥", lib.tracks.single().artist)
    }

    @Test
    fun `key is the stable path and not the mediastore id`() {
        val t = build(listOf(raw("Hindi/", "G.mp3", "G", "A", "Hindi"))).tracks.single()
        assertEquals("Music/Offline/Hindi/G.mp3", t.key)
    }

    @Test
    fun `a compilation filed under each performer's folder is one album by its album artist`() {
        val lib = build(
            listOf(
                raw("Arijit Singh/Aashiqui 2/", "a.mp3", "Tum Hi Ho", "Arijit Singh", "Aashiqui 2", albumArtist = "Various Artists"),
                raw("Ankit Tiwari/Aashiqui 2/", "b.mp3", "Sunn Raha Hai", "Ankit Tiwari", "Aashiqui 2", albumArtist = "Various Artists"),
            ),
        )
        val album = lib.albums.single()
        assertEquals(2, album.tracks.size)
        assertEquals("Various Artists", album.artist)
        assertNull(album.artistKey)
        // The folders still make both performers artists.
        assertEquals(listOf("Ankit Tiwari", "Arijit Singh"), lib.artists.map { it.name })
    }

    @Test
    fun `without an album artist the same album name under two artist folders stays two albums`() {
        val lib = build(
            listOf(
                raw("Queen/Greatest Hits/", "a.mp3", "Bohemian Rhapsody", "Queen", "Greatest Hits"),
                raw("ABBA/Greatest Hits/", "b.mp3", "Dancing Queen", "ABBA", "Greatest Hits"),
            ),
        )
        assertEquals(listOf("ABBA", "Queen"), lib.albums.map { it.artist }.sorted())
    }

    @Test
    fun `loose songs group into albums by album artist`() {
        val rows = listOf(
            RawTrack(1, "a.mp3", "Music/", "Bohemian Rhapsody", "Queen", "Greatest Hits", null, 1000, null, 0, albumArtist = "Queen"),
            RawTrack(2, "b.mp3", "Music/", "Dancing Queen", "ABBA", "Greatest Hits", null, 1000, null, 0, albumArtist = "ABBA"),
            RawTrack(3, "c.mp3", "Music/", "Waterloo", "ABBA", "Greatest Hits", null, 1000, null, 0, albumArtist = "ABBA"),
            RawTrack(4, "d.mp3", "Music/", "Chaleya", "Arijit Singh, Shilpa Rao", "Jawan", null, 1000, null, 0, albumArtist = "Anirudh Ravichander"),
        )
        val lib = LibraryBuilder.build(rows)
        val hits = lib.albums.filter { it.name == "Greatest Hits" }.associate { it.artist to it.tracks.size }
        assertEquals(mapOf("ABBA" to 2, "Queen" to 1), hits)
        // An album artist with no song of their own still gets a page the album links to.
        val jawan = lib.albums.single { it.name == "Jawan" }
        assertEquals("Anirudh Ravichander", jawan.artist)
        assertEquals("Anirudh Ravichander", lib.artistByKey.getValue(jawan.artistKey!!).name)
        assertEquals("Arijit Singh, Shilpa Rao", lib.tracks.single { it.title == "Chaleya" }.artist)
    }

    @Test
    fun `an album artist naming several people shows them all and links the first`() {
        val lib = LibraryBuilder.build(
            listOf(RawTrack(1, "a.mp3", "Music/", "Kesariya", "Arijit Singh", "Brahmastra", null, 1000, null, 0, albumArtist = "Pritam, Arijit Singh")),
        )
        val album = lib.albums.single()
        assertEquals("Pritam, Arijit Singh", album.artist)
        assertEquals(TitleCleaner.normalize("Pritam"), album.artistKey)
    }

    @Test
    fun `an album artist that spells an artist folder is not split at its ampersand`() {
        val lib = build(
            listOf(raw("Simon & Garfunkel/Bookends/", "a.mp3", "America", "Simon & Garfunkel", "Bookends", albumArtist = "Simon & Garfunkel")),
        )
        val album = lib.albums.single()
        assertEquals("Simon & Garfunkel", album.artist)
        assertEquals(TitleCleaner.normalize("Simon & Garfunkel"), album.artistKey)
        assertEquals("bookends|simongarfunkel", album.key)
    }

    @Test
    fun `genre and composer come from the tags and youtube categories are dropped`() {
        val lib = build(
            listOf(
                raw("Coldplay/Parachutes/", "a.mp3", "Yellow", "Coldplay", "Parachutes", genre = "Alternative Rock", composer = "Chris Martin, Guy Berryman, Chris Martin"),
                raw("Coldplay/Parachutes/", "b.mp3", "Trouble", "Coldplay", "Parachutes", genre = "Music"),
                raw("Coldplay/Parachutes/", "c.mp3", "Shiver", "Coldplay", "Parachutes", genre = "People & Blogs", composer = "<unknown>"),
            ),
        )
        val yellow = lib.tracks.single { it.title == "Yellow" }
        assertEquals("Alternative Rock", yellow.genre)
        assertEquals("Chris Martin, Guy Berryman", yellow.composer)
        assertNull(lib.tracks.single { it.title == "Trouble" }.genre)
        assertNull(lib.tracks.single { it.title == "Shiver" }.genre)
        assertNull(lib.tracks.single { it.title == "Shiver" }.composer)
    }
}
