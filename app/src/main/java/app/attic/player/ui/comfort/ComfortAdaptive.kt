package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.NavPlacement
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.ReadableWidthDp
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.adaptive.adaptiveColumns

/*
 * Comfort's share of the tablet layouts (docs/design/2026-11-25-tablet/SPEC.md): the arithmetic its
 * screens use to fill a wide column, and the cards that put rows side by side. Nothing here runs on
 * a compact window.
 */

/** The gap between two cards side by side. */
internal val ColumnGap = 12.dp

/**
 * The width a destination gets in [ComfortWideShell]: the window less the side dock and the 8dp
 * after it, and the 8dp before the end edge. On a compact window, the window's width.
 */
internal fun WindowLayout.contentWidthDp(): Float =
    if (nav == NavPlacement.Rail) widthDp - SideDockContentStart.value - 8f else widthDp

/**
 * How many cards of rows fit side by side in a column [widthDp] wide (the cards inset 16dp at each
 * side): each at least 360dp, and one more while a card would be wider than the readable width.
 */
internal fun rowColumns(widthDp: Float): Int {
    val cards = widthDp - 2 * GroupMargin.value
    var n = adaptiveColumns(cards, 360f, ColumnGap.value)
    while ((cards - ColumnGap.value * (n - 1)) / n > ReadableWidthDp) n++
    return n
}

/**
 * [groupItems] in [columns] cards side by side, filled top to bottom: the first card holds the first
 * third of [items] (with three columns), and so on. Each lazy item is one line across the cards, so a
 * long list stays lazy. With one column it is [groupItems]. [keyPrefix] keeps the lines' keys apart
 * from other sections'.
 */
internal fun <T> LazyListScope.columnGroupItems(
    items: List<T>,
    columns: Int,
    keyPrefix: String,
    key: (T) -> Any,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    if (columns <= 1) {
        groupItems(items, key = { "$keyPrefix:${key(it)}" }) { index, item -> itemContent(index, item) }
        return
    }
    val per = (items.size + columns - 1) / columns
    for (line in 0 until per) {
        item(key = "$keyPrefix:line:${key(items[line])}", contentType = "group-columns") {
            Row(Modifier.fillMaxWidth().padding(horizontal = GroupMargin), horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                for (column in 0 until columns) {
                    val index = column * per + line
                    val count = (items.size - column * per).coerceIn(0, per)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                        if (index < items.size) {
                            ColumnCell(line, count) { itemContent(index, items[index]) }
                        }
                    }
                }
            }
        }
    }
}

/** [GroupCell] without its margin: one slice of a card that already sits in a column. */
@Composable
internal fun ColumnCell(index: Int, count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val radius = ComfortShapes.GroupRadius
    val shape = RoundedCornerShape(
        topStart = if (index == 0) radius else 0.dp,
        topEnd = if (index == 0) radius else 0.dp,
        bottomStart = if (index == count - 1) radius else 0.dp,
        bottomEnd = if (index == count - 1) radius else 0.dp,
    )
    Box(modifier.fillMaxWidth().clip(shape).background(Comfort.colors.card)) {
        CompositionLocalProvider(
            LocalComfortContainer provides ComfortContainer.Card,
            LocalHairlineAbove provides (index > 0),
        ) { content() }
    }
}

/** The side insets that keep a column of cards at most [ReadableWidthDp] wide in a pane [widthDp] wide, centred. */
internal fun readableInset(widthDp: Float, maxDp: Float = ReadableWidthDp + 2 * GroupMargin.value): Dp =
    ((widthDp - maxDp) / 2f).coerceAtLeast(0f).dp

/**
 * The side insets that keep a destination's list of cards in a centred readable column on a tablet
 * (Settings, the licences, Recap); none on a phone.
 */
@Composable
internal fun readableColumnInset(): Dp {
    val window = LocalWindowLayout.current
    return if (window.widthClass == WidthClass.Compact) 0.dp else readableInset(window.contentWidthDp())
}

/** True on a tablet window under 840dp tall (the Pad sideways), where root screens take the short hero. */
@Composable
internal fun shortHeroOnTablet(): Boolean {
    val window = LocalWindowLayout.current
    return window.widthClass != WidthClass.Compact && window.heightDp < 840f
}
