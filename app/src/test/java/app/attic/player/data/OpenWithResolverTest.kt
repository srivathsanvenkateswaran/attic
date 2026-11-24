package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenWithResolverTest {
    private fun track(key: String, mediaId: Long) = Track(
        key = key,
        mediaId = mediaId,
        title = key.substringAfterLast('/'),
        artist = "",
        artistKeys = emptyList(),
        album = null,
        albumKey = null,
        folder = null,
        trackNumber = null,
        durationMs = 0,
        year = null,
        dateAddedSec = 0,
    )

    private val song = track("Music/Hindi/Jaan Se Guzarte Hain.mp3", 42)
    private val other = track("Download/Udd Gaye.m4a", 7)
    private val library = Library(listOf(song, other), emptyList(), emptyList(), emptyList())

    private fun content(authority: String, vararg segments: String) = UriParts("content", authority, segments.toList())
    private fun file(path: String) = UriParts("file", "", path.trim('/').split('/'))
    private fun resolve(parts: UriParts) = OpenWithResolver.resolve(parts, library)

    @Test
    fun `a MediaStore audio URI finds the song by id`() {
        assertEquals(OpenTarget.InLibrary(song), resolve(content("media", "external", "audio", "media", "42")))
        assertEquals(OpenTarget.InLibrary(song), resolve(content("media", "external_primary", "audio", "media", "42")))
        assertEquals(OpenTarget.InLibrary(other), resolve(content("media", "1a2b-3c4d", "file", "7")))
    }

    @Test
    fun `a MediaStore id the library doesn't hold plays on its own`() {
        // A ringtone, a voice note, or a song in a folder the library leaves out.
        assertEquals(OpenTarget.NotInLibrary, resolve(content("media", "external", "audio", "media", "999")))
    }

    @Test
    fun `the media documents provider wraps a MediaStore id`() {
        assertEquals(OpenTarget.InLibrary(song), resolve(content("com.android.providers.media.documents", "document", "audio:42")))
        assertEquals(OpenTarget.NotInLibrary, resolve(content("com.android.providers.media.documents", "document", "image:42")))
    }

    @Test
    fun `the downloads list gives a MediaStore id or a raw path`() {
        val downloads = "com.android.providers.downloads.documents"
        assertEquals(OpenTarget.InLibrary(other), resolve(content(downloads, "document", "msf:7")))
        assertEquals(OpenTarget.InLibrary(other), resolve(content(downloads, "document", "raw:/storage/emulated/0/Download/Udd Gaye.m4a")))
        // Its own numeric id names a row in the downloads database, not a song.
        assertEquals(OpenTarget.NotInLibrary, resolve(content(downloads, "document", "7")))
    }

    @Test
    fun `the storage documents provider gives a path on a volume`() {
        val storage = "com.android.externalstorage.documents"
        assertEquals(OpenTarget.InLibrary(song), resolve(content(storage, "document", "primary:Music/Hindi/Jaan Se Guzarte Hain.mp3")))
        assertEquals(OpenTarget.InLibrary(song), resolve(content(storage, "document", "1A2B-3C4D:Music/Hindi/Jaan Se Guzarte Hain.mp3")))
        assertEquals(
            OpenTarget.InLibrary(song),
            resolve(content(storage, "tree", "primary:Music", "document", "primary:Music/Hindi/Jaan Se Guzarte Hain.mp3")),
        )
    }

    @Test
    fun `file URIs match by path on any volume`() {
        assertEquals(OpenTarget.InLibrary(song), resolve(file("/storage/emulated/0/Music/Hindi/Jaan Se Guzarte Hain.mp3")))
        assertEquals(OpenTarget.InLibrary(song), resolve(file("/sdcard/Music/Hindi/Jaan Se Guzarte Hain.mp3")))
        assertEquals(OpenTarget.InLibrary(song), resolve(file("/storage/1A2B-3C4D/Music/Hindi/Jaan Se Guzarte Hain.mp3")))
        assertEquals(OpenTarget.InLibrary(other), resolve(UriParts("FILE", null, listOf("storage", "self", "primary", "Download", "Udd Gaye.m4a"))))
        assertEquals(OpenTarget.NotInLibrary, resolve(file("/storage/emulated/0/Music/Somewhere else.mp3")))
    }

    @Test
    fun `a file manager's own URI is matched when the real path is inside it`() {
        assertEquals(
            OpenTarget.InLibrary(song),
            resolve(content("com.example.files.provider", "root", "storage", "emulated", "0", "Music", "Hindi", "Jaan Se Guzarte Hain.mp3")),
        )
        assertEquals(OpenTarget.NotInLibrary, resolve(content("com.example.files.provider", "external_files", "Music", "a.mp3")))
    }

    @Test
    fun `odd URIs never crash and never match`() {
        val odd = listOf(
            UriParts("content", null, emptyList()),
            content("media"),
            content("media", "external", "audio", "media", "-1"),
            content("media", "external", "audio", "media", "not-a-number"),
            content("media", "external", "audio", "media", "99999999999999999999999"),
            content("com.android.providers.media.documents", "document"),
            content("com.android.providers.media.documents", "document", "audio:"),
            content("com.android.providers.downloads.documents", "document", "msf:abc"),
            content("com.android.providers.downloads.documents", "document", "raw:"),
            content("com.android.providers.downloads.documents", "document", "raw:relative/path.mp3"),
            content("com.android.externalstorage.documents", "document", "primary"),
            content("com.android.externalstorage.documents", "document", "primary:"),
            content("com.android.externalstorage.documents", "document", "primary:Music/../Music/Hindi/Jaan Se Guzarte Hain.mp3"),
            content("com.android.externalstorage.documents", "document", "primary:Music/Hindi/"),
            file("/"),
            file("/storage/emulated/0/"),
            file("/storage/emulated/0/Music/./Hindi/Jaan Se Guzarte Hain.mp3"),
            UriParts("file", null, listOf("", "", "")),
        )
        odd.forEach { assertEquals(it.toString(), OpenTarget.NotInLibrary, resolve(it)) }
    }

    @Test
    fun `schemes other than content and file are not opened`() {
        assertEquals(OpenTarget.Unsupported, resolve(UriParts("https", "example.com", listOf("song.mp3"))))
        assertEquals(OpenTarget.Unsupported, resolve(UriParts(null, null, listOf("song.mp3"))))
        assertEquals(OpenTarget.Unsupported, resolve(UriParts("", null, emptyList())))
    }

    @Test
    fun `paths off a volume give no key`() {
        assertNull(OpenWithResolver.keyForPath("/data/data/app/files/song.mp3"))
        assertNull(OpenWithResolver.keyForPath("Music/song.mp3"))
        assertEquals("Music/song.mp3", OpenWithResolver.keyForPath("/storage/emulated/10/Music/song.mp3"))
    }
}
