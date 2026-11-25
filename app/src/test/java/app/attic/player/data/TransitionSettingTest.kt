package app.attic.player.data

import app.attic.player.playback.TransitionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TransitionSettingTest {
    @Test
    fun aNewInstallIsOffWithSixSeconds() =
        assertEquals(TransitionSetting(TransitionMode.Off, 6), TransitionSetting.read(null, 0))

    @Test
    fun anOldCrossfadeLengthBecomesCrossfadeAtThatLength() =
        assertEquals(TransitionSetting(TransitionMode.Crossfade, 8), TransitionSetting.read(null, 8))

    @Test
    fun aSavedModeWins() {
        assertEquals(TransitionSetting(TransitionMode.Smart, 4), TransitionSetting.read("Smart", 4))
        assertEquals(TransitionSetting(TransitionMode.Off, 10), TransitionSetting.read("Off", 10))
    }

    @Test
    fun anUnknownModeOrLengthFallsBack() {
        assertEquals(TransitionSetting(TransitionMode.Crossfade, 6), TransitionSetting.read("Blend", 7))
        assertEquals(TransitionSetting(TransitionMode.Off, 6), TransitionSetting.read("Gone", 0))
    }
}
