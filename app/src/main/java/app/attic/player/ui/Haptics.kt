package app.attic.player.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The moments Attic answers with a tap you can feel. Each maps to one of the platform's predefined
 * feedback constants, never a vibration pattern of Attic's own, so the phone decides how it feels and
 * the system's touch-feedback setting can turn it off.
 */
enum class Haptic {
    /** A like, or playback starting. */
    On,

    /** An unlike, or playback pausing. */
    Off,

    /** A row picked up to reorder. */
    PickUp,

    /** A dragged row let go. */
    Drop,

    /** Something done that has no undo on screen: a swipe that queued a song, a sleep timer set. */
    Confirm,

    /** A choice moved along a set of options: the style switch. */
    Tick,
    ;

    /**
     * The constant for this on [sdk]. Android 14 added the toggle, drag and segment constants; before it
     * the nearest older one stands in. Attic's minimum is 13, so CONFIRM and GESTURE_END (Android 11)
     * are always there.
     */
    fun constant(sdk: Int = Build.VERSION.SDK_INT): Int = when (this) {
        On -> if (sdk >= U) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CLOCK_TICK
        Off -> if (sdk >= U) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CLOCK_TICK
        PickUp -> if (sdk >= U) HapticFeedbackConstants.DRAG_START else HapticFeedbackConstants.LONG_PRESS
        Drop -> HapticFeedbackConstants.GESTURE_END
        Confirm -> HapticFeedbackConstants.CONFIRM
        Tick -> if (sdk >= U) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    }

    private companion object {
        const val U = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}

/** Plays [Haptic]s through the view the screen is in. Without flags, so the system setting applies. */
@Stable
class Haptics(private val view: View) {
    fun perform(haptic: Haptic) {
        view.performHapticFeedback(haptic.constant())
    }

    /** [Haptic.On] or [Haptic.Off] for a toggle that is about to become [on]. */
    fun toggle(on: Boolean) = perform(if (on) Haptic.On else Haptic.Off)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
