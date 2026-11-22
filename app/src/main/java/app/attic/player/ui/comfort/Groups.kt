package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * What a component is drawn on. Chips and the search field take `--fill` on anything but the ground
 * (`:is(.group,.sheet,.dialog) .chip`); song rows in a sheet indent to the sheet's 24dp margin.
 * [Group], [ComfortSheet] and [ComfortDialog] set it.
 */
enum class ComfortContainer { Ground, Card, Sheet, Dialog }

val LocalComfortContainer = compositionLocalOf { ComfortContainer.Ground }

/**
 * True for the second and later rows of a hairline-split list. Rows read it and draw the hairline at
 * their own inset (`--sep`), as kit.css's `.row + .row::before` does. [Hairlined], [Group] and
 * [groupItems] set it; nothing else needs to.
 */
val LocalHairlineAbove = compositionLocalOf { false }

/** Collects the items of a hairline-split list. Each [item] is one row (or an [Inset]). */
class GroupScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items += content
    }

    fun <T> items(list: List<T>, content: @Composable (T) -> Unit) {
        list.forEach { value -> items += { content(value) } }
    }
}

/**
 * Rows split by hairlines with no card: the rows of a sheet or a menu. Inside a [Group] use the group.
 *
 * ```
 * Hairlined { item { SheetRow(…) }; item { SheetRow(…) } }
 * ```
 */
@Composable
fun Hairlined(modifier: Modifier = Modifier, content: GroupScope.() -> Unit) {
    val scope = GroupScope().apply(content)
    Column(modifier) {
        scope.items.forEachIndexed { index, item ->
            CompositionLocalProvider(LocalHairlineAbove provides (index > 0)) { item() }
        }
    }
}

/** Horizontal margin of a group on the ground (`.group{margin:0 16px}`). */
val GroupMargin = 16.dp

/** Space between two groups in a column (`.group + .group{margin-top:12px}`). */
val GroupSpacing = 12.dp

/**
 * `.group`: a card on the ground, radius 26, holding rows split by hairlines the kit draws for you.
 * No gaps between rows and no shadow. It insets itself 16dp from the screen edges; pass
 * `margin = 0.dp` where the parent already places it (a pager). [pad] is `.group.pad`, for a group
 * holding a grid instead of rows.
 *
 * ```
 * Label("Start listening")
 * Group {
 *     item { SongRow(…) }
 *     item { SongRow(…) }
 * }
 * ```
 * For a long list in a LazyColumn, use [groupItems] instead.
 */
@Composable
fun Group(
    modifier: Modifier = Modifier,
    pad: Boolean = false,
    margin: Dp = GroupMargin,
    content: GroupScope.() -> Unit,
) {
    val colors = Comfort.colors
    val scope = GroupScope().apply(content)
    Column(
        modifier
            .padding(horizontal = margin)
            .fillMaxWidth()
            .clip(ComfortShapes.Group)
            .background(colors.card)
            .then(if (pad) Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 16.dp) else Modifier),
    ) {
        CompositionLocalProvider(LocalComfortContainer provides ComfortContainer.Card) {
            scope.items.forEachIndexed { index, item ->
                CompositionLocalProvider(LocalHairlineAbove provides (index > 0)) { item() }
            }
        }
    }
}

/**
 * A group in a LazyColumn: one lazy item per row, each drawn as its slice of the card (top corners on
 * the first, bottom corners on the last), with the hairlines between them.
 *
 * ```
 * LazyColumn {
 *     item { Label("Songs") }
 *     groupItems(songs, key = { it.id }) { index, song -> SongRow(…) }
 * }
 * ```
 */
fun <T> LazyListScope.groupItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    contentType: (T) -> Any? = { "group-row" },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    itemsIndexed(items, key = key?.let { k -> { _: Int, item: T -> k(item) } }, contentType = { _, item -> contentType(item) }) { index, item ->
        GroupCell(index, items.size) { itemContent(index, item) }
    }
}

/**
 * One slice of a group, for a lazy list built by hand: [index] of [count] rows. [groupItems] uses it.
 */
@Composable
fun GroupCell(index: Int, count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val radius = ComfortShapes.GroupRadius
    val shape = RoundedCornerShape(
        topStart = if (index == 0) radius else 0.dp,
        topEnd = if (index == 0) radius else 0.dp,
        bottomStart = if (index == count - 1) radius else 0.dp,
        bottomEnd = if (index == count - 1) radius else 0.dp,
    )
    Box(modifier.padding(horizontal = GroupMargin).fillMaxWidth().clip(shape).background(Comfort.colors.card)) {
        CompositionLocalProvider(
            LocalComfortContainer provides ComfortContainer.Card,
            LocalHairlineAbove provides (index > 0),
        ) { content() }
    }
}

/**
 * `.inset`: a block inside a group that is not a row (a shelf, a grid), with a hairline above it when
 * a row comes first.
 */
@Composable
fun Inset(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().hairlineAbove(LocalHairlineAbove.current, 20.dp).padding(top = 14.dp, bottom = 16.dp),
        content = content,
    )
}

/**
 * The hairline over a row that follows another: from [start] (the row's `--sep`, where its text
 * starts) to [end] from the right edge, in `--line`. Rows call this themselves.
 */
@Composable
fun Modifier.hairlineAbove(enabled: Boolean, start: Dp, end: Dp = 20.dp): Modifier {
    if (!enabled) return this
    val color = Comfort.colors.line
    return this.drawWithContent {
        drawContent()
        val rtl = layoutDirection == LayoutDirection.Rtl
        val left = (if (rtl) end else start).toPx()
        val right = size.width - (if (rtl) start else end).toPx()
        drawRect(color, topLeft = Offset(left, 0f), size = androidx.compose.ui.geometry.Size(right - left, 1.dp.toPx()))
    }
}

/**
 * The shadow of a floating piece (`--shadow` and the 1dp `--edge` ring): dock, sheet, dialog, menu,
 * action bar. [small] is `--shadow-sm` (a lifted row, a toast, the switch knob). Cards never take one.
 */
@Composable
fun Modifier.floatingShadow(shape: Shape, small: Boolean = false, edge: Boolean = !small): Modifier {
    val colors = Comfort.colors
    val shadow = if (small) {
        if (colors.dark) Shadow(radius = 14.dp, color = colors.shadowSmall, offset = DpOffset(0.dp, 4.dp))
        else Shadow(radius = 12.dp, color = colors.shadowSmall, offset = DpOffset(0.dp, 3.dp))
    } else {
        if (colors.dark) Shadow(radius = 36.dp, color = colors.shadow, offset = DpOffset(0.dp, 12.dp))
        else Shadow(radius = 28.dp, color = colors.shadow, offset = DpOffset(0.dp, 8.dp))
    }
    return this
        .dropShadow(shape, shadow)
        .then(if (edge) Modifier.border(1.dp, colors.edge, shape) else Modifier)
}

/**
 * `.label`: the name of the group below it, on the ground, outside the card. [small] follows the
 * label on the same line (`<small>`); [end] sits at the right (`<span class="end">`). [tight] is
 * `.label.tight`, for the first label under a bar.
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    small: String? = null,
    tight: Boolean = false,
    end: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = Comfort.colors
    Row(
        modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = if (tight) 14.dp else 24.dp, bottom = 9.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = Comfort.type.label, color = colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (small != null) {
            Text(
                small,
                style = onest(12.5f, 18f),
                color = colors.on3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (end != null) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.on3) { end() }
            }
        }
    }
}

/** `.note`: small print under a group. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, style = onest(13f, 18f), color = Comfort.colors.on3, modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 10.dp))
}

/** A surface that repaints itself with the (washed) ground or card: kit.css `.bg-ground` / `.bg-card`. */
@Composable
fun ComfortSurface(
    modifier: Modifier = Modifier,
    card: Boolean = false,
    shape: Shape = androidx.compose.ui.graphics.RectangleShape,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Comfort.colors
    Box(modifier.clip(shape).background(if (card) colors.card else colors.ground)) {
        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.on) { content() }
    }
}

/** Padding that clears the dock: kit.css `.pad-dock` (176), `.pad-tabs` (112), `.pad-bar` (112). */
object ComfortPadding {
    val Dock = PaddingValues(bottom = 176.dp)
    val Tabs = PaddingValues(bottom = 112.dp)
    val ActionBar = PaddingValues(bottom = 112.dp)
}

