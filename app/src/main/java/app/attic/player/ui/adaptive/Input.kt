package app.attic.player.ui.adaptive

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A right click (a mouse's secondary button, or a two-finger trackpad click) runs [onClick] instead of
 * reaching the clickable underneath: a song row opens its menu, as a long press does. Pass
 * `enabled = false` where no mouse is expected, so phones add nothing to each row.
 */
fun Modifier.onSecondaryClick(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(onClick) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        onClick()
                    }
                }
            }
        }
    }
