package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevellingTest {
    @Test
    fun loudSongIsTurnedDownToTheTarget() {
        assertEquals(-6f, Levelling.forLoudness(-8.0, 1.0), 1e-4f)
    }

    @Test
    fun quietSongIsTurnedUpWhenItsPeakAllows() {
        // -20 LUFS wants +6 dB; a peak at -10 dBFS leaves 9 dB below the -1 dB ceiling.
        assertEquals(6f, Levelling.forLoudness(-20.0, 0.316), 1e-3f)
    }

    @Test
    fun boostStopsWhereThePeakWouldPassTheCeiling() {
        // A peak at -3 dBFS leaves 2 dB, so the +6 dB the loudness asks for is held to +2.
        assertEquals(2f, Levelling.forLoudness(-20.0, 0.7079), 1e-3f)
    }

    @Test
    fun aSongThatAlreadyPeaksAtFullScaleIsNeverRaised() {
        assertEquals(-1f, Levelling.clamp(5f, 1f), 1e-4f)
    }

    @Test
    fun withoutAPeakOnlyCutsAreAllowed() {
        assertEquals(0f, Levelling.clamp(4f, null), 0f)
        assertEquals(-3f, Levelling.clamp(-3f, null), 0f)
    }

    @Test
    fun gainStaysInsideItsRange() {
        assertEquals(Levelling.MAX_BOOST_DB, Levelling.clamp(40f, 0.001f), 0f)
        assertEquals(-Levelling.MAX_CUT_DB, Levelling.clamp(-40f, 1f), 0f)
    }

    @Test
    fun replayGainIsMovedFromMinus18ToTheTarget() {
        // A ReplayGain of -6 dB brings a song to -18 LUFS; Attic plays 4 dB louder than that.
        assertEquals(-2f, Levelling.forReplayGain(-6f, 0.9f), 1e-4f)
    }

    @Test
    fun albumLevelIsAPowerAverageWeightedByLength() {
        val level = Levelling.albumLevel(listOf(MeasuredLevel(-10.0, 0.9) to 60_000L, MeasuredLevel(-20.0, 0.5) to 60_000L))!!
        // Mean of 10^-1 and 10^-2 is 0.055, which is -12.6 LUFS: loud songs dominate, as they do to the ear.
        assertEquals(-12.596, level.lufs, 1e-3)
        assertEquals(0.9, level.peak!!, 0.0)
    }

    @Test
    fun albumRunNeedsTheSameAlbumAndTheNextNumber() {
        val one = LevelTrack("a/1", "album", 1)
        assertTrue(AlbumRun.follows(one, LevelTrack("a/2", "album", 2)))
        assertFalse(AlbumRun.follows(one, LevelTrack("a/3", "album", 3)))
        assertFalse(AlbumRun.follows(one, LevelTrack("b/2", "other", 2)))
        assertFalse(AlbumRun.follows(LevelTrack("x", null, 1), LevelTrack("y", null, 2)))
        assertFalse(AlbumRun.follows(LevelTrack("x", "album", null), LevelTrack("y", "album", 2)))
        assertFalse(AlbumRun.follows(one, null))
    }

    private val measured = mapOf("a/1" to MeasuredLevel(-8.0, 1.0), "a/2" to MeasuredLevel(-12.0, 1.0))
    private val albums = mapOf("album" to MeasuredLevel(-10.0, 1.0))

    @Test
    fun songOnItsOwnUsesItsOwnLevel() {
        val gain = Levelling.gainDb(null, LevelTrack("a/2", "album", 2), LevelTrack("z", "zz", 9), null, measured::get, albums::get)
        assertEquals(-2f, gain!!, 1e-4f)
    }

    @Test
    fun songInAnAlbumRunUsesTheAlbumLevel() {
        val gain = Levelling.gainDb(LevelTrack("a/1", "album", 1), LevelTrack("a/2", "album", 2), null, null, measured::get, albums::get)
        assertEquals(-4f, gain!!, 1e-4f)
    }

    @Test
    fun tagsWinOverMeasurement() {
        val tags = TagGain(trackGainDb = -3f, trackPeak = 0.5f, albumGainDb = -5f, albumPeak = 0.6f)
        assertEquals(1f, Levelling.gainDb(null, LevelTrack("a/2", "album", 2), null, tags, measured::get, albums::get)!!, 1e-4f)
        // In an album run the album's tag is used.
        assertEquals(-1f, Levelling.gainDb(LevelTrack("a/1", "album", 1), LevelTrack("a/2", "album", 2), null, tags, measured::get, albums::get)!!, 1e-4f)
    }

    @Test
    fun unmeasuredSongPlaysAtUnity() {
        assertNull(Levelling.gainDb(null, LevelTrack("new", null, null), null, null, measured::get, albums::get))
    }
}
