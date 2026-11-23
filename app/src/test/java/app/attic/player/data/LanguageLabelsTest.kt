package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageLabelsTest {
    private fun resolve(
        folder: String? = "Harris Jayaraj",
        album: String? = "Minnale",
        stored: String? = null,
        lyrics: String? = null,
        instrumental: Boolean = false,
    ) = LanguageLabels.resolve(folder, album, stored, lyrics, instrumental)

    @Test
    fun aStoredLabelWinsOverEverything() {
        assertEquals("hi", resolve(folder = "Tamil", stored = "hi"))
    }

    @Test
    fun themedFoldersNameTheLanguage() {
        assertEquals("ta", resolve(folder = "Tamil"))
        assertEquals("hi", resolve(folder = "Hindi"))
        assertEquals("en", resolve(folder = "English"))
    }

    @Test
    fun phonkCountsAsHavingNoLanguage() {
        assertEquals(LanguageLabels.NO_VOCALS, resolve(folder = "Phonk"))
    }

    @Test
    fun anAlbumNameCanNameTheLanguage() {
        assertEquals("ta", resolve(album = "Roja (Tamil)"))
        assertEquals("te", resolve(album = "Roja (Telugu)"))
    }

    @Test
    fun tamilScriptLyricsMeanTamil() {
        assertEquals("ta", resolve(lyrics = "உன்னாலே உன்னாலே உன்னாலே"))
    }

    @Test
    fun devanagariLyricsMeanHindi() {
        assertEquals("hi", resolve(lyrics = "तेरे बिना बेसुवादी बेसुवादी"))
    }

    @Test
    fun romanizedLyricsSayNothing() {
        assertNull(resolve(lyrics = "Tere bina besuadi besuadi ratiyan"))
    }

    @Test
    fun lyricsMarkedInstrumentalMeanNoVocals() {
        assertEquals(LanguageLabels.NO_VOCALS, resolve(instrumental = true))
    }

    @Test
    fun anArtistFolderAloneSaysNothing() {
        assertNull(resolve())
    }

    @Test
    fun theImportFileIsPathTabLanguage() {
        val parsed = LanguageLabels.parseImport("Music/Offline/A/b.m4a\tta\n\nbad line\nMusic/Offline/C/d.mp3\thi\n")
        assertEquals(mapOf("Music/Offline/A/b.m4a" to "ta", "Music/Offline/C/d.mp3" to "hi"), parsed)
    }
}
