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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Attic mark (`.mark`): the house with the round window, corners rounded to sit with the type,
 * drawn in one colour taken from the scheme. Never a fixed green.
 */
val AtticMarkVector: ImageVector by lazy {
    ImageVector.Builder("AtticMark", 24.dp, 24.dp, 68f, 68f).apply {
        group(translationX = -20f, translationY = -20f) {
            addPath(
                pathData = PathParser().parsePathString(
                    "M54,25L84,53L84,79Q84,83 80,83L28,83Q24,83 24,79L24,53ZM44,60a10,10 0 1,0 20,0a10,10 0 1,0 -20,0Z",
                ).toNodes(),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 5f,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}

@Composable
fun AtticMarkIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    Icon(AtticMarkVector, contentDescription = null, tint = tint, modifier = modifier.size(size))
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
