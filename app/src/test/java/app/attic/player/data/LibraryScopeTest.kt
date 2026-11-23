package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScopeTest {
    private var nextId = 1L

    private fun raw(
        path: String,
        file: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        volume: String = RawTrack.PRIMARY_VOLUME,
    ) = RawTrack(nextId++, file, path, title, artist, album, null, 200_000, null, 0, volume)

    /** The shape of the owner's library: artist folders, themed folders and Various Artists under Music/Offline. */
    private fun ownerLibrary(volume: String = RawTrack.PRIMARY_VOLUME) = listOf(
        raw("Music/Offline/A. R. Rahman/Roja/", "01 Kadhal Rojave.m4a", "Kadhal Rojave", "A. R. Rahman", "Roja", volume),
        raw("Music/Offline/A. R. Rahman/Roja/", "02 Chinna Chinna Aasai.m4a", "Chinna Chinna Aasai", "A. R. Rahman", "Roja", volume),
        raw("Music/Offline/Coldplay/Parachutes/", "05 Yellow.mp3", "Yellow", "Coldplay", "Parachutes", volume),
        raw("Music/Offline/Coldplay/Parachutes/", "06 Trouble.mp3", "Trouble", "Coldplay", "Parachutes", volume),
        raw("Music/Offline/Various Artists/Tamasha/", "a.mp3", "Agar Tum Saath Ho", "Alka Yagnik & Arijit Singh", "Tamasha", volume),
        raw("Music/Offline/English/", "The Nights [kTpyO8iwlSY].mp3", "Avicii - The Nights", "Avicii", "English", volume),
        raw("Music/Offline/English/", "Wake Me Up [kTpyO8iwlSY].mp3", "Avicii - Wake Me Up", "Avicii", "English", volume),
        raw("Music/Offline/Hindi/", "Gulabi [kTpyO8iwlSY].mp3", null, null, "Hindi", volume),
        raw("Music/Offline/Tamil/", "Song [kTpyO8iwlSY].mp3", "Song", "Anirudh", "Tamil", volume),
        raw("Music/Offline/Phonk/", "Soul [kTpyO8iwlSY].mp3", "KORDHELL - SOUL", "KORDHELL", "Phonk", volume),
    )

    @Test
    fun `owner library detects music offline as the root and builds the same library as before`() {
        val rows = ownerLibrary()
        val roots = LibraryScope.roots(rows)
        assertTrue(roots.values.all { it == "Music/Offline/" })
        assertEquals(LibraryBuilder.build(rows, root = "Music/Offline/"), LibraryBuilder.build(rows))

        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("English", "Hindi", "Phonk", "Tamil"), lib.folders.map { it.name })
        assertEquals(
            listOf("A. R. Rahman", "Alka Yagnik", "Arijit Singh", "Avicii", "Coldplay", "KORDHELL"),
            lib.artists.map { it.name },
        )
        assertEquals("Music/Offline/Coldplay/Parachutes/05 Yellow.mp3", lib.tracks.single { it.title == "Yellow" }.key)
    }

    @Test
    fun `a stray file next to the owner's folder leaves the owner's root alone`() {
        val stray = raw("Music/", "voice memo.mp3", "Memo", null, "Music")
        val rows = ownerLibrary() + stray
        val roots = LibraryScope.roots(rows)
        assertEquals("Music/Offline/", roots.getValue(rows.first()))

        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("English", "Hindi", "Phonk", "Tamil"), lib.folders.map { it.name })
        assertFalse("Offline" in lib.artists.map { it.name })
        assertEquals(null, lib.tracks.single { it.title == "Memo" }.folder)
    }

    @Test
    fun `a flat music folder is the root and tags supply the artists`() {
        val rows = listOf(
            raw("Music/", "Yellow.mp3", "Yellow", "Coldplay", "Parachutes"),
            raw("Music/", "Hello.mp3", "Hello", "Adele", "25"),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Music/" })
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Adele", "Coldplay"), lib.artists.map { it.name })
        assertTrue(lib.folders.isEmpty())
        assertTrue(lib.tracks.all { it.folder == null })
    }

    @Test
    fun `artist folders directly in music make music the root`() {
        val rows = listOf(
            raw("Music/Coldplay/Parachutes/", "Yellow.mp3", "Yellow", null, "Parachutes"),
            raw("Music/Adele/25/", "Hello.mp3", "Hello", null, "25"),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Music/" })
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Adele", "Coldplay"), lib.artists.map { it.name })
        assertEquals("Coldplay", lib.tracks.single { it.title == "Yellow" }.artist)
    }

    @Test
    fun `a one-artist library keeps its artist folder`() {
        val rows = listOf(
            raw("Music/Coldplay/Parachutes/", "Yellow.mp3", "Yellow", "Coldplay", "Parachutes"),
            raw("Music/Coldplay/X&Y/", "Fix You.mp3", "Fix You", "Coldplay", "X&Y"),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Music/" })
        val lib = LibraryBuilder.build(rows)
        assertEquals(listOf("Coldplay"), lib.artists.map { it.name })
        assertEquals(listOf("Parachutes", "X&Y"), lib.albums.map { it.name })
    }

    @Test
    fun `a wrapper folder around artist folders becomes the root`() {
        val rows = listOf(
            raw("Music/My Songs/Coldplay/", "Yellow.mp3", "Yellow", "Coldplay", null),
            raw("Music/My Songs/Adele/", "Hello.mp3", "Hello", "Adele", null),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "Music/My Songs/" })
        assertFalse("My Songs" in LibraryBuilder.build(rows).artists.map { it.name })
    }

    @Test
    fun `download is its own root and never becomes an artist`() {
        val rows = listOf(
            raw("Music/Coldplay/Parachutes/", "Yellow.mp3", "Yellow", "Coldplay", "Parachutes"),
            raw("Music/Adele/25/", "Hello.mp3", "Hello", "Adele", "25"),
            raw("Download/", "Levitating.mp3", "Levitating", "Dua Lipa", "Download"),
            raw("Download/", "Believer.mp3", "Believer", "Imagine Dragons", "Download"),
        )
        val roots = LibraryScope.roots(rows)
        assertEquals("Music/", roots.getValue(rows[0]))
        assertEquals("Download/", roots.getValue(rows[2]))

        val lib = LibraryBuilder.build(rows)
        val names = lib.artists.map { it.name }
        assertFalse("Music" in names)
        assertFalse("Download" in names)
        assertTrue("Dua Lipa" in names)
        assertTrue(lib.albums.none { it.name == "Download" })
    }

    @Test
    fun `an sd card gets its own root and does not move the phone's`() {
        val card = "64e4-1ef6"
        val rows = ownerLibrary() + listOf(
            raw("Music/Eminem/Recovery/", "Not Afraid.mp3", "Not Afraid", "Eminem", "Recovery", card),
            raw("Music/Adele/25/", "Hello.mp3", "Hello", "Adele", "25", card),
        )
        val roots = LibraryScope.roots(rows)
        assertEquals("Music/Offline/", roots.getValue(rows.first()))
        assertEquals("Music/", roots.getValue(rows.last()))
        assertTrue("Eminem" in LibraryBuilder.build(rows).artists.map { it.name })
    }

    @Test
    fun `artist folders at the top of an sd card use the card itself as the root`() {
        val card = "64e4-1ef6"
        val rows = listOf(
            raw("Coldplay/Parachutes/", "Yellow.mp3", "Yellow", null, "Parachutes", card),
            raw("Adele/25/", "Hello.mp3", "Hello", null, "25", card),
        )
        assertTrue(LibraryScope.roots(rows).values.all { it == "" })
        assertEquals(listOf("Adele", "Coldplay"), LibraryBuilder.build(rows).artists.map { it.name })
    }

    @Test
    fun `the same path on two volumes is one song and the phone's copy wins`() {
        val onCard = raw("Music/Offline/English/", "a.mp3", "A", "Avicii", "English", "64e4-1ef6")
        val onPhone = raw("Music/Offline/English/", "a.mp3", "A", "Avicii", "English")
        val lib = LibraryBuilder.build(listOf(onCard, onPhone))
        assertEquals(onPhone.mediaId, lib.tracks.single().mediaId)
    }

    @Test
    fun `mediastore labels for ringtones, alerts, recordings and podcasts exclude a file`() {
        val path = "Music/"
        val file = "a.mp3"
        assertTrue(LibraryScope.isSong(path, file, AudioFlags()))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isMusic = false)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isRingtone = true)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isNotification = true)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isAlarm = true)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isRecording = true)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isPodcast = true)))
        assertFalse(LibraryScope.isSong(path, file, AudioFlags(isAudiobook = true)))
    }

    @Test
    fun `standard ringtone and recording folders are excluded even without labels`() {
        assertFalse(LibraryScope.isSong("Ringtones/", "Bell.ogg", AudioFlags()))
        assertFalse(LibraryScope.isSong("Notifications/", "Ping.ogg", AudioFlags()))
        assertFalse(LibraryScope.isSong("Alarms/", "Wake.mp3", AudioFlags()))
        assertFalse(LibraryScope.isSong("Recordings/Voice Recorder/", "Lecture.m4a", AudioFlags()))
        assertTrue(LibraryScope.isSong("Music/Live Recordings/", "Yellow (Live).mp3", AudioFlags()))
    }

    @Test
    fun `voice notes are excluded and songs shared in chats are kept`() {
        val whatsapp = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/202347/"
        assertFalse(LibraryScope.isSong(whatsapp, "PTT-20261120-WA0003.opus", AudioFlags()))
        val business = "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Voice Notes/202347/"
        assertFalse(LibraryScope.isSong(business, "PTT-20261120-WA0001.opus", AudioFlags()))
        assertTrue(
            LibraryScope.isSong("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/", "AUD-20261120-WA0002.mp3", AudioFlags()),
        )
        assertFalse(LibraryScope.isSong("Telegram/Telegram Audio/", "4_5823.ogg", AudioFlags()))
        assertTrue(LibraryScope.isSong("Telegram/Telegram Audio/", "Kesariya.mp3", AudioFlags()))
        assertFalse(LibraryScope.isSong("Recordings/Call recordings/", "Call with Amma.m4a", AudioFlags()))
    }

    @Test
    fun `a path outside its root has no folder parts`() {
        assertEquals(listOf("English"), LibraryScope.pathInRoot("Music/Offline/English/", "Music/Offline/"))
        assertEquals(emptyList<String>(), LibraryScope.pathInRoot("Music/", "Music/Offline/"))
        assertEquals(emptyList<String>(), LibraryScope.pathInRoot("Music/Other/", "Music/Offline/"))
        assertEquals(listOf("Coldplay", "Parachutes"), LibraryScope.pathInRoot("Coldplay/Parachutes/", ""))
    }
}
