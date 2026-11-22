package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortLibrary
import app.attic.player.ui.expressive.ExpressiveLibrary
import app.attic.player.ui.model.rememberLibraryModel

@Composable
fun LibraryScreen(library: Library) {
    val (state, actions) = rememberLibraryModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLibrary(state, actions)
        UiStyle.Comfort -> ComfortLibrary(state, actions)
    }

    // Outside the style's drawing, so the name typed so far survives a style switch.
    if (state.creating) {
        PlaylistNameDialog(
            title = "Give your playlist a name",
            confirm = "Create",
            initial = "",
            onDismiss = actions.dismissCreating,
            onConfirm = actions.create,
        )
    }
}
