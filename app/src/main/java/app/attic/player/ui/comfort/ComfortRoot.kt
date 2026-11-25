package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.LocalBottomPadding

/** The collapsed bar's height under the status bar (`.bar`). */
internal val BarHeight = 56.dp

/** The expanded title area's height under the status bar (`.hero`), and the short one (`.hero.short`). */
internal val HeroHeight = 228.dp

/** The short hero (`.hero.short`): a tablet window under 840dp tall. */
internal val ShortHeroHeight = 150.dp

/**
 * True once the hero (the list's first item, [heroHeight] tall under the status bar) has scrolled up
 * far enough that its bottom meets the bottom of the collapsed bar: the moment the bar takes over.
 */
@Composable
internal fun rememberCollapsed(listState: LazyListState, heroHeight: Dp = HeroHeight): State<Boolean> {
    val threshold = with(LocalDensity.current) { (heroHeight - BarHeight).toPx() }
    return remember(listState, threshold) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold }
    }
}

/**
 * True once the item at [index] has scrolled under the collapsed bar (its top above the bar's bottom),
 * so a copy of it should pin there: Home's sub-tabs, Library's chips.
 */
@Composable
internal fun rememberPinned(listState: LazyListState, index: Int): State<Boolean> {
    val density = LocalDensity.current
    val barBottom = with(density) { (WindowInsets.statusBars.getTop(this) + BarHeight.roundToPx()) }
    return remember(listState, index, barBottom) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > index) {
                true
            } else {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                item != null && item.offset < barBottom
            }
        }
    }
}

/**
 * A root screen (Home, Library, Settings): the list opens with the [Hero] and its tools row, and once
 * the hero has scrolled away the title becomes the 56dp [Bar] across the top, holding [barActions]
 * (and the back arrow, with [onBack]). [pinned] is drawn under the bar once the list item at
 * [pinIndex] reaches it (`.subtabs.lift`, `.chips.lift`); the lowest pinned piece carries the hairline.
 *
 * [content] must start with the hero item. The list pads its end for the dock ([LocalBottomPadding]).
 */
@Composable
internal fun RootPage(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    heroHeight: Dp = HeroHeight,
    onBack: (() -> Unit)? = null,
    /** On a tablet, the side insets that centre the list in a readable column. */
    inset: Dp = 0.dp,
    pinIndex: Int = -1,
    pinned: (@Composable () -> Unit)? = null,
    barActions: @Composable RowScope.() -> Unit = {},
    overlay: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val collapsed by rememberCollapsed(listState, heroHeight)
    val pin by rememberPinned(listState, pinIndex)
    val showPinned = pinned != null && pinIndex >= 0 && collapsed && pin
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = inset, end = inset, bottom = LocalBottomPadding.current + 8.dp),
            content = content,
        )
        overlay()
        AnimatedVisibility(collapsed, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Column(Modifier.fillMaxWidth().background(Comfort.colors.ground)) {
                Bar(title, onBack = onBack, lift = !showPinned, actions = barActions)
                if (showPinned) pinned?.invoke()
            }
        }
    }
}

/**
 * `.tools.lead`: the row that closes the viewing area holding only a back arrow at its start
 * (Settings, Recap).
 */
@Composable
internal fun LeadTools(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().height(50.dp).padding(start = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A pinned row under the collapsed bar: the ground, with the hairline under it (`.lift`). */
internal fun Modifier.liftedUnderBar(ground: androidx.compose.ui.graphics.Color, line: androidx.compose.ui.graphics.Color): Modifier =
    this.fillMaxWidth().background(ground).drawBehind {
        drawRect(line, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
    }
