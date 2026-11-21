package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypeTest {
    @Test
    fun otherFormatsGetTheirOwnLabel() {
        assertEquals("audio/flac", mimeTypeFor("Music/Offline/A/Song.flac"))
        assertEquals("audio/ogg", mimeTypeFor("Music/Offline/A/Song.ogg"))
        assertEquals("audio/wav", mimeTypeFor("Music/Offline/A/Song.wav"))
    }

    @Test
    fun anUnknownFormatGetsNoLabelSoThePlayerDetectsIt() {
        assertEquals(null, mimeTypeFor("Music/Offline/A/Song.xyz"))
    }

    @Test
    fun m4aIsNotLabelledAsMpeg() {
        assertEquals("audio/mp4", mimeTypeFor("Music/Offline/A/Song.m4a"))
        assertEquals("audio/mpeg", mimeTypeFor("Music/Offline/A/Song.mp3"))
        assertEquals("audio/mp4", mimeTypeFor("Music/Offline/A/SONG.M4A"))
    }
}
