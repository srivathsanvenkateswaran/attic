package app.attic.player.ui.expressive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Attic mark (`.mark`), "Headroom": a speaker cabinet cut to the slope of a roof, with the woofer
 * (its dust cap left solid) and the tweeter as holes. One even-odd path from
 * docs/design/2026-11-21-ui-directions/logo/marks/sound-2-headroom.svg, cropped to its bounding box
 * (x 35.2 to 71.2, y 27 to 78.5), so the glyph is 36 wide by 51.5 tall. Drawn in one colour taken
 * from the scheme, never a fixed one.
 */
val AtticMarkVector: ImageVector by lazy {
    ImageVector.Builder("AtticMark", MARK_WIDTH.dp, MARK_HEIGHT.dp, MARK_WIDTH, MARK_HEIGHT).apply {
        group(translationX = -35.2f, translationY = -27f) {
            addPath(
                pathData = PathParser().parsePathString(
                    "M35.2,78.5L35.2,47.78L71.2,27L71.2,78.5ZM41.2,60.5a12,12 0 1,0 24,0a12,12 0 1,0 -24,0Z" +
                        "M48.4,60.5a4.8,4.8 0 1,0 9.6,0a4.8,4.8 0 1,0 -9.6,0ZM57.2,42a3.8,3.8 0 1,0 7.6,0a3.8,3.8 0 1,0 -7.6,0Z",
                ).toNodes(),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black),
            )
        }
    }.build()
}

private const val MARK_WIDTH = 36f
private const val MARK_HEIGHT = 51.5f

/**
 * The mark [size] tall and as wide as its proportions give (about 0.7 of [size]), so it hugs the
 * text beside it. [size] is the kit's `.mark` size: 32dp beside Home's title, 24dp once it shrinks.
 */
@Composable
fun AtticMarkIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    Icon(AtticMarkVector, contentDescription = null, tint = tint, modifier = modifier.size(size * (MARK_WIDTH / MARK_HEIGHT), size))
}

/** The wordmark on Home's root bar: the mark in primary, then "Attic" in [style]. */
@Composable
fun AtticWordmark(text: String, style: TextStyle, modifier: Modifier = Modifier, markSize: Dp = 32.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AtticMarkIcon(size = markSize, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(text, style = style, maxLines = 1)
    }
}
