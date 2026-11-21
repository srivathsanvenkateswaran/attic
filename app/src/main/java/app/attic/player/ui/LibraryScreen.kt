package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicLibrary
import app.attic.player.ui.model.rememberLibraryModel

@Composable
fun LibraryScreen(library: Library) {
    val (state, actions) = rememberLibraryModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicLibrary(state, actions)
        UiStyle.Comfort -> ClassicLibrary(state, actions)
        UiStyle.Classic -> ClassicLibrary(state, actions)
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
