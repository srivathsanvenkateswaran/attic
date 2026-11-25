package app.attic.player.ui.expressive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.NavPlacement
import app.attic.player.ui.adaptive.WideMarginDp
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.adaptive.centredInsetDp
import app.attic.player.ui.adaptive.ReadableWidthDp

/*
 * The Expressive style's share of docs/design/2026-11-25-tablet/SPEC.md: the sizes the wide layouts
 * have in common, and the helpers that lay a list out in columns. Phones never reach any of it; each
 * screen branches once on LocalWindowLayout and keeps its phone path as it was.
 */

/** The navigation rail's width (`ExpressiveNavigationRail`). */
val RailWidth: Dp = 96.dp

/**
 * What a destination adds to the 16dp its components carry, so the page has the 24dp margin of a
 * medium or expanded window.
 */
val WideExtraMargin: Dp = (WideMarginDp - 16f).dp

/** Rows of songs and collections take columns at least this wide. */
const val RowColumnMinDp = 360f

/** Grids of tiles take columns at least this wide. */
const val TileColumnMinDp = 160f

/** True on a medium or expanded window: a tablet, an unfolded foldable, a wide floating window. */
val WindowLayout.wide: Boolean get() = widthClass != WidthClass.Compact

/** True on a medium or expanded window. Read it once at the top of a screen. */
@Composable
@ReadOnlyComposable
fun isWideWindow(): Boolean = LocalWindowLayout.current.wide

/** The width the destinations get: the window less the rail, when there is one. */
val WindowLayout.contentWidthDp: Float
    get() = if (nav == NavPlacement.Rail) widthDp - RailWidth.value else widthDp

/**
 * How many columns of rows fit in a destination [paneDp] wide, with the 24dp margins and the 12dp
 * between columns. A group's cells carry 16dp each side, so the rows' own width is the pane less 48.
 */
fun rowColumns(paneDp: Float): Int = adaptiveColumns(paneDp - 2 * WideMarginDp, RowColumnMinDp, 12f)

/** How many columns of tiles fit in a destination [paneDp] wide. */
fun tileColumns(paneDp: Float): Int = adaptiveColumns(paneDp - 2 * WideMarginDp, TileColumnMinDp, 12f)

/**
 * [groupItems] in [columns] columns, each column its own group, filled top to bottom (SPEC.md,
 * Library and Search). One lazy item holds a line of cells, one from each column, so the list stays
 * lazy. With one column it is [groupItems] itself.
 */
fun <T> LazyListScope.columnGroupItems(
    items: List<T>,
    columns: Int,
    key: (T) -> Any,
    contentType: (T) -> Any? = { "groupCell" },
    cell: @Composable (index: Int, item: T) -> Unit,
) {
    if (columns <= 1 || items.size <= 1) {
        groupItems(items, key, contentType, cell)
        return
    }
    val perColumn = (items.size + columns - 1) / columns
    items(
        count = perColumn,
        key = { line -> "line:${key(items[line])}" },
        contentType = { "groupLine$columns" },
    ) { line ->
        // The cells' own 16dp margins less 6 each side leave 12dp between the columns.
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            for (column in 0 until columns) {
                val start = column * perColumn
                val count = (items.size - start).coerceIn(0, perColumn)
                val index = start + line
                Box(Modifier.weight(1f)) {
                    if (line < count) {
                        Box(Modifier.fillMaxWidth().groupCell(line, count, groupCellColor(), horizontalMargin = 6.dp)) {
                            cell(index, items[index])
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lets the content run [start] past the start edge and [end] past the end edge of the space it is
 * given. A shelf runs on to the window's end edge through the page's end margin; the detail beside the
 * Library's list lines its rows up with the list's, inside the list pane's margin.
 */
fun Modifier.bleed(start: Dp = 0.dp, end: Dp = 0.dp): Modifier = layout { measurable, constraints ->
    val s = start.roundToPx()
    val extra = s + end.roundToPx()
    val wider = if (constraints.hasBoundedWidth) {
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    } else {
        constraints
    }
    val placeable = measurable.measure(wider)
    layout(constraints.constrainWidth(placeable.width - extra), placeable.height) {
        placeable.place(-s, 0)
    }
}

/** Measures the content as a square as long as the slot's longer side and centres it, so it covers the slot. */
fun Modifier.coverSlot(): Modifier = layout { measurable, constraints ->
    val side = maxOf(constraints.maxWidth, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(side, side))
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    layout(width, height) { placeable.place((width - side) / 2, (height - side) / 2) }
}

/** Fades the content out over [edge] at its top and bottom, so a line cut by the edge doesn't show whole. */
fun Modifier.fadeEdges(edge: Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = edge.toPx() / size.height
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f - fade to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

/** Takes [top] off the top of the content: a section header that opens a pane beside a header card sits higher. */
fun Modifier.trimTop(top: Dp): Modifier = layout { measurable, constraints ->
    val trim = top.roundToPx()
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height - trim).coerceAtLeast(0)) { placeable.place(0, -trim) }
}

/**
 * The inset at each side that keeps a page's column at most [maxDp] wide (its rows' 16dp margins
 * extra) and centred in the content area, and never under the 8dp a destination adds on a tablet.
 * Lists take it as horizontal content padding, so they still scroll from anywhere across the window.
 */
fun WindowLayout.readableInset(maxDp: Float = ReadableWidthDp): Dp =
    centredInsetDp(contentWidthDp, maxDp + 32f, minDp = WideExtraMargin.value).dp
