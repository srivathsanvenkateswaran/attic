package app.attic.player.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A column holding a cover and what goes with it (a collection's header, the start pane of full-screen
 * lyrics), whose cover takes the height the rest leaves: [content] is
 * measured once with no cover, then drawn with the largest cover, from [minCover] to [maxCover], that
 * keeps the column within [height]. A short window (a 7-inch tablet sideways) gets a smaller cover
 * instead of buttons under the player bar. Where even [minCover] doesn't fit, the column is taller
 * than [height] and its pane scrolls, or with [orNone] the cover is 0dp and [content] leaves it out.
 * The measuring pass is never placed and carries no semantics.
 */
@Composable
fun CoverToFit(
    height: Dp,
    maxCover: Dp,
    minCover: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    orNone: Boolean = false,
    content: @Composable ColumnScope.(cover: Dp) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val rest = subcompose(Probe) {
            Column(Modifier.clearAndSetSemantics { }, Arrangement.Top, horizontalAlignment) { content(0.dp) }
        }.sumOf { it.measure(loose).height }
        val room = (height.roundToPx() - rest).toDp()
        val cover = if (orNone && room < minCover) 0.dp else room.coerceIn(minCover, maxOf(minCover, maxCover))
        val drawn = subcompose(Drawn) {
            Column(Modifier, Arrangement.Top, horizontalAlignment) { content(cover) }
        }.map { it.measure(loose) }
        val width = (drawn.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, drawn.sumOf { it.height }) {
            var y = 0
            for (p in drawn) {
                p.placeRelative(0, y)
                y += p.height
            }
        }
    }
}

private object Probe
private object Drawn
