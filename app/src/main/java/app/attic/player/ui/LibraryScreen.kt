package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.attic.player.R
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.comfort.ComfortLibrary
import app.attic.player.ui.expressive.ExpressiveLibrary
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryState
import app.attic.player.ui.model.rememberLibraryModel

@Composable
fun LibraryScreen(library: Library) {
    val (state, actions) = rememberLibraryModel(library)
    // Movable, so turning a tablet between list and detail and the list alone keeps the list's
    // scroll position: it moves into the list pane with its state instead of starting again.
    val list = remember { movableContentOf { s: LibraryState, a: LibraryActions -> StyledLibrary(s, a) } }
    if (LocalWindowLayout.current.libraryListDetail) {
        LibraryListDetail(state, actions, list = list, detail = { DestinationScreen(library, it) })
    } else {
        list(state, actions)
    }

    // Outside the style's drawing, so the name typed so far survives a style switch.
    if (state.creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.dialog_name_playlist_title),
            confirm = stringResource(R.string.create),
            initial = "",
            onDismiss = actions.dismissCreating,
            onConfirm = actions.create,
        )
    }
}

@Composable
private fun StyledLibrary(state: LibraryState, actions: LibraryActions) {
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLibrary(state, actions)
        UiStyle.Comfort -> ComfortLibrary(state, actions)
    }
}
