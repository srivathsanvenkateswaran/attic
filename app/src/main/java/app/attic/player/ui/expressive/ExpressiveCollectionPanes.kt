package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.adaptive.CollectionLayout
import app.attic.player.ui.adaptive.LocalDetailPane
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.adaptive.collectionHeaderPaneDp
import app.attic.player.ui.adaptive.collectionLayout
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlayControlsState

/**
 * A collection on a medium or expanded window (SPEC.md, Album, artist, playlist, folder, Favourites,
 * genre), laid out for the width of the pane it is given rather than the window's:
 *
 * - 760dp or wider: two panes. The start one (360dp, 320 below 1000) holds the header as a card on
 *   the cover's scheme, 32dp corners, hugging its content: the cover as wide as the card, the title,
 *   the byline and details, then play, shuffle and add in one row. It scrolls only if it has to. The
 *   end pane holds back and the menu, then the songs in columns at least 360dp wide, filled top to
 *   bottom, and an artist's or a genre's albums as a grid.
 * - Narrower than that: one column, the header a row (cover 200dp, the titles and buttons beside it)
 *   under the bar, then the songs.
 * - Under 600dp (a narrow pane, never the whole of a tablet): the phone's layout, [phone].
 *
 * Beside the Library's list ([LocalDetailPane]) there is no bar, since the list is beside it; the
 * menu a playlist or Favourites keeps in its bar moves to the end of the header's buttons, and the
 * rows line up with the list's, 8dp into its pane's margin.
 */
@Composable
internal fun CollectionPanes(
    title: String,
    seed: Color,
    controls: PlayControlsState,
    controlActions: PlayControlsActions,
    onBack: () -> Unit,
    listState: LazyListState,
    barActions: @Composable RowScope.() -> Unit,
    header: HeaderParts,
    content: LazyListScope.(CollectionColumns) -> Unit,
    phone: @Composable () -> Unit,
) {
    val detail = LocalDetailPane.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pane = maxWidth.value
        when (collectionLayout(pane)) {
            CollectionLayout.Stacked -> phone()
            CollectionLayout.TwoPane -> TwoPanes(pane, seed, onBack, listState, barActions, header, content, detail)
            CollectionLayout.HeaderRow ->
                HeaderRowColumn(pane, title, seed, controls, controlActions, onBack, listState, barActions, header, content, detail)
        }
    }
}

/** The start of the header beside the Library's list, and as its own screen: 8 and 24dp in. */
private fun lead(detail: Boolean): Dp = if (detail) 8.dp else 24.dp

private fun columnsFor(listWidthDp: Float, gap: Dp = 12.dp) = CollectionColumns(
    rows = adaptiveColumns(listWidthDp - 32f, RowColumnMinDp, 12f),
    tiles = adaptiveColumns(listWidthDp - 32f, TileColumnMinDp, 12f),
    gap = gap,
)

@Composable
private fun TwoPanes(
    pane: Float,
    seed: Color,
    onBack: () -> Unit,
    listState: LazyListState,
    barActions: @Composable RowScope.() -> Unit,
    header: HeaderParts,
    content: LazyListScope.(CollectionColumns) -> Unit,
    detail: Boolean,
) {
    val start = collectionHeaderPaneDp(pane)
    val lead = lead(detail)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
    val bottom = LocalBottomPadding.current
    // The songs run from 8dp before the start pane's end to 8dp before the pane's own.
    // Under the bar the songs start 8dp down; beside the Library's list, level with the card.
    val columns = remember(pane, start, detail) { columnsFor(pane - start, gap = if (detail) 0.dp else 8.dp) }
    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .width(start.dp)
                .fillMaxHeight()
                // The card stops 8dp above the player bar or mini player.
                .padding(start = lead, end = 16.dp, top = top, bottom = (bottom - 16.dp).coerceAtLeast(8.dp)),
        ) {
            FromArt(seed) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = androidx.compose.ui.Alignment.Top)
                        .clip(RoundedCornerShape(ExpressiveShapes.ExtraLargeIncreased))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    header.art(Modifier.fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(16.dp))
                    HeaderTexts(header, Modifier)
                    Spacer(Modifier.height(16.dp))
                    HeaderActions(header, barActions.takeIf { detail }, fill = true)
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp)
                .bleed(start = 8.dp),
        ) {
            if (!detail) DetailAppBar(title = "", onBack = onBack, actions = barActions)
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = if (detail) top else 0.dp, bottom = bottom),
            ) {
                content(columns)
            }
        }
    }
}

@Composable
private fun HeaderRowColumn(
    pane: Float,
    title: String,
    seed: Color,
    controls: PlayControlsState,
    controlActions: PlayControlsActions,
    onBack: () -> Unit,
    listState: LazyListState,
    barActions: @Composable RowScope.() -> Unit,
    header: HeaderParts,
    content: LazyListScope.(CollectionColumns) -> Unit,
    detail: Boolean,
) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // As its own screen the column keeps 8dp from each side; beside the list it starts 8dp into the list's margin.
    val listWidth = if (detail) pane else pane - 16f
    val columns = remember(listWidth) { columnsFor(listWidth) }
    var titleHeight by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }
    val gap = with(LocalDensity.current) { 16.dp.roundToPx() }
    val collapsed by remember(listState, gap) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleHeight + gap }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(start = if (detail) 0.dp else 8.dp, end = 8.dp)
                .then(if (detail) Modifier.bleed(start = 8.dp) else Modifier),
            state = listState,
            contentPadding = PaddingValues(top = status + if (detail) 8.dp else 64.dp, bottom = LocalBottomPadding.current),
        ) {
            item(key = "header") {
                FromArt(seed) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLargeIncreased))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        header.art(Modifier.size(HeaderRowCover))
                        Column(Modifier.weight(1f).height(HeaderRowCover), verticalArrangement = Arrangement.SpaceBetween) {
                            HeaderTexts(header, Modifier.onSizeChanged { titleHeight = it.height })
                            HeaderActions(header, barActions.takeIf { detail }, fill = false)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            content(columns)
        }
        if (!detail) {
            FromArt(seed) {
                CollectionBar(title, collapsed, controls, controlActions, onBack, barActions)
            }
        }
    }
}

private val HeaderRowCover = 200.dp

/** The title, the byline and the details, spaced as the phone's header spaces them. */
@Composable
private fun HeaderTexts(header: HeaderParts, titleModifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column {
        HeaderTitle(header.title, header.artist, titleModifier)
        header.byline?.invoke()
        for (line in header.meta) {
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Play, shuffle and add to playlist in one row (radio in place of add for a genre). Play is the
 * medium filled button, squared off and reading Pause while this collection plays; with [fill] it
 * takes the room the other two leave. [menu] is the bar's menu, for a header with no bar over it.
 */
@Composable
private fun HeaderActions(header: HeaderParts, menu: (@Composable RowScope.() -> Unit)?, fill: Boolean) {
    val state = header.controls
    val actions = header.controlActions
    val enabled = state.enabled
    val radio = header.radio
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Row(
            if (fill) Modifier.weight(1f).alpha(if (enabled) 1f else 0.38f) else Modifier.alpha(if (enabled) 1f else 0.38f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            ExpressiveButton(
                stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                onClick = actions.playOrPause,
                modifier = if (fill) Modifier.weight(1f) else Modifier,
                size = ButtonSize.Medium,
                leading = if (state.isPlaying) Symbol.Pause else Symbol.PlayArrow,
                squared = state.isPlaying,
                enabled = enabled,
            )
            ExpressiveToggleButton(
                Symbol.Shuffle,
                checked = state.shuffleOn,
                onCheckedChange = { actions.shuffle() },
                contentDescription = stringResource(R.string.collection_shuffle_cd, header.name),
                medium = true,
                tonalWhenOff = true,
                enabled = enabled,
            )
            ExpressiveIconButton(
                if (radio != null) Symbol.Sensors else Symbol.PlaylistAdd,
                stringResource(if (radio != null) R.string.collection_radio_cd else R.string.collection_add_to_playlist_cd, header.name),
                radio ?: actions.addToPlaylist,
                variant = IconButtonVariant.Tonal,
                medium = true,
                enabled = enabled,
            )
        }
        if (menu != null) {
            if (!fill) Spacer(Modifier.weight(1f))
            menu()
        }
    }
}
