package app.attic.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortLookPreview
import app.attic.player.ui.expressive.ExpressiveLookPreview
import kotlin.math.roundToInt

/**
 * A small live picture of [style] for the first run's look cards: that style's own Home filters and
 * tab bar, drawn in its own theme whichever style is around it, at [scale] of their real size. It is
 * a picture: it takes no touches and says nothing to accessibility services.
 */
@Composable
fun LookPreview(style: UiStyle, mode: ThemeMode, modifier: Modifier = Modifier, scale: Float = 0.5f) {
    Box(modifier.clearAndSetSemantics { }) {
        Scaled(scale) {
            AtticTheme(mode, style) {
                when (style) {
                    UiStyle.Expressive -> ExpressiveLookPreview()
                    UiStyle.Comfort -> ComfortLookPreview()
                }
            }
        }
        // Lies over the picture and takes its touches without using them: the buttons drawn below never
        // see one, and the card around it still does.
        Box(Modifier.matchParentSize().pointerInput(Unit) { })
    }
}

/** Lays [content] out at 1/[scale] of the width it is given, then draws it scaled down to fit. */
@Composable
private fun Scaled(scale: Float, content: @Composable () -> Unit) {
    Layout(content, Modifier.clipToBounds()) { measurables, constraints ->
        val width = constraints.maxWidth
        val inner = measurables.first().measure(Constraints(minWidth = (width / scale).roundToInt(), maxWidth = (width / scale).roundToInt()))
        layout(width, (inner.height * scale).roundToInt()) {
            inner.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
