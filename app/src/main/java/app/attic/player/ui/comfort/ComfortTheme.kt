package app.attic.player.ui.comfort

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.SymbolIcon

/** The Comfort palette for a theme: kit.css's dark tokens or its `.light` ones. */
fun comfortColors(dark: Boolean): ComfortColors = if (dark) ComfortDark else ComfortLight

/** One place to reach the Comfort tokens MaterialTheme has no slot for. */
object Comfort {
    /** The colour tokens in effect: the theme's palette, or a cover's inside [Tinted]. */
    val colors: ComfortColors
        @Composable @ReadOnlyComposable get() = LocalComfortColors.current

    /** The type scale. Its plain styles are also MaterialTheme.typography. */
    val type: ComfortTypography
        @Composable @ReadOnlyComposable get() = LocalComfortType.current

    val shapes = ComfortShapes
}

/** The icon weight the kit draws Material Symbols Rounded at (`'wght' 300`). */
const val ComfortIconWeight = 300

/**
 * A Material Symbols Rounded icon at the kit's weight 300 (`<span class="ms">`). [filled] is
 * `.ms.fill`: the selected tab, a favourite that is on, the play and pause glyphs. Sizes in the kit:
 * 18, 20, 24 (default), 28, 32, 40. Pass [contentDescription] only for an icon that stands alone.
 */
@Composable
fun ComfortIcon(
    symbol: Symbol,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
    weight: Int = ComfortIconWeight,
    contentDescription: String? = null,
) = SymbolIcon(symbol, modifier, size, filled, tint, weight, contentDescription)
