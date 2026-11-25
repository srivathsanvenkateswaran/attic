package app.attic.player.playback

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.google.common.collect.ImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainTagsTest {
    @Test
    fun readsReplayGainFieldsWhateverTheirCase() {
        val gain = ReplayGainTags.parse(
            listOf(
                "REPLAYGAIN_TRACK_GAIN" to "-6.48 dB",
                "replaygain_track_peak" to "0.988312",
                "ReplayGain_Album_Gain" to "-7.10 dB",
                "REPLAYGAIN_ALBUM_PEAK" to "1.000000",
            ),
        )!!
        assertEquals(-6.48f, gain.trackGainDb!!, 1e-4f)
        assertEquals(0.988312f, gain.trackPeak!!, 1e-6f)
        assertEquals(-7.10f, gain.albumGainDb!!, 1e-4f)
        assertEquals(1f, gain.albumPeak!!, 0f)
    }

    @Test
    fun readsPositiveGainsAndDecimalCommas() {
        assertEquals(1.5f, ReplayGainTags.decibels("+1.5dB")!!, 1e-4f)
        assertEquals(-6.48f, ReplayGainTags.decibels("-6,48 dB")!!, 1e-4f)
        assertEquals(3f, ReplayGainTags.decibels("3 dB")!!, 0f)
    }

    @Test
    fun ignoresGarbage() {
        assertNull(ReplayGainTags.decibels("loud"))
        assertNull(ReplayGainTags.decibels("-900 dB"))
        assertNull(ReplayGainTags.parse(listOf("REPLAYGAIN_TRACK_GAIN" to "n/a")))
        assertNull(ReplayGainTags.parse(emptyList()))
    }

    @Test
    fun readsOpusR128GainAgainstMinus23() {
        // -512 is -2 dB against -23 LUFS, which is +3 dB against ReplayGain's -18.
        val gain = ReplayGainTags.parse(listOf("R128_TRACK_GAIN" to "-512"))!!
        assertEquals(3f, gain.trackGainDb!!, 1e-4f)
    }

    @Test
    fun replayGainWinsOverSoundCheck() {
        val gain = ReplayGainTags.parse(
            listOf(
                "iTunNORM" to " 000003E8 000003E8 00000000 00000000 00000000 00000000 00007FFF 00007FFF 00000000 00000000",
                "replaygain_track_gain" to "-2.00 dB",
            ),
        )!!
        assertEquals(-2f, gain.trackGainDb!!, 1e-4f)
    }

    /** 0x2710 is 10000, ten times the reference 1000, so Sound Check asks for -10 dB; the peak word 0x4000 is half scale. */
    @Test
    fun readsSoundCheck() {
        val gain = ReplayGainTags.parse(
            listOf("iTunNORM" to " 00002710 00001F40 00006590 00004E20 00024CA8 00024CA8 00004000 00003000 00007E54 00007E54"),
        )!!
        assertEquals(-10f, gain.trackGainDb!!, 1e-4f)
        assertEquals(0.5f, gain.trackPeak!!, 1e-6f)
        assertNull(gain.albumGainDb)
    }

    @Test
    fun shortSoundCheckIsIgnored() {
        assertNull(ReplayGainTags.parse(listOf("iTunNORM" to "000003E8 000003E8")))
    }

    @Test
    fun readsId3Txxx() {
        val metadata = Metadata(
            TextInformationFrame("TIT2", null, ImmutableList.of("A song")),
            TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_GAIN", ImmutableList.of("-4.20 dB")),
            TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_PEAK", ImmutableList.of("0.9")),
        )
        val gain = ReplayGainTags.fromMetadata(metadata)!!
        assertEquals(-4.2f, gain.trackGainDb!!, 1e-4f)
        assertEquals(0.9f, gain.trackPeak!!, 1e-6f)
    }

    @Test
    fun readsMp4FreeformAtom() {
        val metadata = Metadata(InternalFrame("com.apple.iTunes", "replaygain_track_gain", "-8.01 dB"))
        assertEquals(-8.01f, ReplayGainTags.fromMetadata(metadata)!!.trackGainDb!!, 1e-4f)
    }

    @Test
    fun readsSoundCheckFromAnId3Comment() {
        val metadata = Metadata(
            CommentFrame("eng", "iTunNORM", " 000003E8 000003E8 00000000 00000000 00000000 00000000 00008000 00008000 00000000 00000000"),
        )
        val gain = ReplayGainTags.fromMetadata(metadata)!!
        assertEquals(0f, gain.trackGainDb!!, 1e-4f)
        assertEquals(1f, gain.trackPeak!!, 1e-6f)
    }

    @Test
    fun readsVorbisComments() {
        val metadata = Metadata(VorbisComment("replaygain_album_gain", "-9.5 dB"), VorbisComment("REPLAYGAIN_TRACK_GAIN", "-9 dB"))
        val gain = ReplayGainTags.fromMetadata(metadata)!!
        assertEquals(-9f, gain.trackGainDb!!, 1e-4f)
        assertEquals(-9.5f, gain.albumGainDb!!, 1e-4f)
    }

    @Test
    fun noTagsMeansNoGain() {
        assertNull(ReplayGainTags.fromMetadata(Metadata(TextInformationFrame("TIT2", null, ImmutableList.of("A song")))))
        assertNull(ReplayGainTags.fromMetadata(null))
    }
}
