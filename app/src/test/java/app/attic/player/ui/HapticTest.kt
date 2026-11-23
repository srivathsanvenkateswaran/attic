package app.attic.player.ui

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every haptic is one of the platform's own constants, with an older one where Android 13 lacks it. */
class HapticTest {
    @Test
    fun android14UsesTheNamedConstants() {
        assertEquals(HapticFeedbackConstants.TOGGLE_ON, Haptic.On.constant(34))
        assertEquals(HapticFeedbackConstants.TOGGLE_OFF, Haptic.Off.constant(34))
        assertEquals(HapticFeedbackConstants.DRAG_START, Haptic.PickUp.constant(34))
        assertEquals(HapticFeedbackConstants.GESTURE_END, Haptic.Drop.constant(34))
        assertEquals(HapticFeedbackConstants.CONFIRM, Haptic.Confirm.constant(34))
        assertEquals(HapticFeedbackConstants.SEGMENT_TICK, Haptic.Tick.constant(34))
    }

    @Test
    fun android13FallsBackToOlderConstants() {
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, Haptic.On.constant(33))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, Haptic.Off.constant(33))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, Haptic.PickUp.constant(33))
        assertEquals(HapticFeedbackConstants.GESTURE_END, Haptic.Drop.constant(33))
        assertEquals(HapticFeedbackConstants.CONFIRM, Haptic.Confirm.constant(33))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, Haptic.Tick.constant(33))
    }
}
