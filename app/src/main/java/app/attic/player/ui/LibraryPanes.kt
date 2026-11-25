package app.attic.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.attic.player.ui.adaptive.LibraryListPaneDp
import app.attic.player.ui.adaptive.ListDetailSelection
import app.attic.player.ui.adaptive.LocalDetailPane
import app.attic.player.ui.adaptive.LocalListDetailSelection
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryItem
import app.attic.player.ui.model.LibraryState

/**
 * The Library as list and detail (docs/design/2026-11-25-tablet/SPEC.md, Library): [list] in a
 * [LibraryListPaneDp] pane at the start, and the album, artist, playlist, folder or Favourites it
 * opened in the pane beside it, drawn by [detail]. The first item is open on arrival. Opening an item
 * from the list selects it here instead of navigating; anything opened from the detail (an artist
 * from an album, say) navigates as usual. The Songs filter has nothing to open, so it keeps the
 * whole width. [libraryShowsListDetail] decides whether the Library is drawn this way at all.
 *
 * The list knows which row is open through [LocalListDetailSelection]; the detail knows it is beside
 * a list through [LocalDetailPane], and leaves out its back button.
 */
@Composable
fun LibraryListDetail(
    state: LibraryState,
    actions: LibraryActions,
    list: @Composable (LibraryState, LibraryActions) -> Unit,
    detail: @Composable (Destination) -> Unit,
) {
    // Per filter, so going back to a filter shows what was open there; saved across rotation and recreation.
    var selectedId by rememberSaveable(state.filter) { mutableStateOf<String?>(null) }
    val selected = openable(state).let { items -> items.firstOrNull { it.id == selectedId } ?: items.firstOrNull() }
    if (selected == null) {
        list(state, actions)
        return
    }
    val selecting = actions.withOpen { item -> selectedId = item.id }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(LibraryListPaneDp.dp).fillMaxHeight()) {
            CompositionLocalProvider(LocalListDetailSelection provides ListDetailSelection(selected.id)) {
                list(state, selecting)
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            CompositionLocalProvider(LocalDetailPane provides true) {
                // A fresh screen per item, so its scroll and header state don't carry over to the next.
                key(selected.id) { detail(selected.destination) }
            }
        }
    }
}

/** The items that open a detail: everything the filter lists, except songs. */
private fun openable(state: LibraryState): List<LibraryItem> =
    if (state.filter == LibraryFilter.Downloaded) emptyList() else state.items

/** These actions with [open] replaced, so the list selects instead of navigating. */
fun LibraryActions.withOpen(open: (LibraryItem) -> Unit) = LibraryActions(
    toggleFilter = toggleFilter,
    cycleSort = cycleSort,
    setSort = setSort,
    toggleGrid = toggleGrid,
    openSettings = openSettings,
    openSearch = openSearch,
    open = open,
    startCreating = startCreating,
    dismissCreating = dismissCreating,
    create = create,
    shuffleAll = shuffleAll,
    play = play,
    showMenu = showMenu,
    queue = queue,
)
