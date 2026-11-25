package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryState

/**
 * The Library's list beside the item it opened (docs/design/2026-11-25-tablet/SPEC.md, Library; drawn
 * by ui/LibraryPanes.kt in a 400dp pane). The hero would leave four rows on a 720dp tall window, so
 * the collapsed bar takes its place from the start, holding the title and the three tools; the chips
 * and the labelled cards follow as on the phone, in one column. The open row ([selectedId]) sits on
 * the bed a playing song's row has.
 */
@Composable
internal fun ComfortLibraryListPane(state: LibraryState, actions: LibraryActions, selectedId: String) {
    val colors = Comfort.colors
    val listState = rememberLazyListState()
    val lifted by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + BarHeight
    Box(Modifier.fillMaxSize().background(colors.ground)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = top, bottom = LocalBottomPadding.current + 8.dp),
        ) {
            item(key = "chips") {
                ChipRow {
                    LibraryFilter.entries.forEach { option ->
                        FilterChip(option.title(), option == state.filter, { actions.toggleFilter(option) })
                    }
                }
            }
            when {
                state.filter == LibraryFilter.Downloaded -> songs(state, actions, 0.dp)
                state.grid -> grids(state, actions, selectedId = selectedId)
                else -> lists(state, actions, columns = 1, selectedId = selectedId)
            }
        }
        Box(Modifier.fillMaxWidth().background(colors.ground)) {
            Bar(stringResource(R.string.library_title), lift = lifted) {
                ComfortIconButton(Symbol.Search, stringResource(R.string.library_search_cd), actions.openSearch)
                ComfortIconButton(Symbol.Add, stringResource(R.string.library_create_playlist_cd), actions.startCreating)
                ComfortIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
            }
        }
    }
}

/** The bed under a selected tile in the grid view: accent-soft, reaching 6dp past the tile on every side. */
@Composable
internal fun Modifier.selectedTileBed(): Modifier {
    val bed = Comfort.colors.accentSoft
    return drawBehind {
        val out = 6.dp.toPx()
        drawRoundRect(
            bed,
            topLeft = Offset(-out, -out),
            size = Size(size.width + 2 * out, size.height + 2 * out),
            cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()),
        )
    }
}

/** The bed under a selected row: accent-soft, 6dp in at the sides and 4 above and below, with the control radius, as a current song's. */
@Composable
internal fun Modifier.selectedBed(): Modifier {
    val bed = Comfort.colors.accentSoft
    return drawBehind {
        drawRoundRect(
            bed,
            topLeft = Offset(6.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 12.dp.toPx(), size.height - 8.dp.toPx()),
            cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()),
        )
    }
}
