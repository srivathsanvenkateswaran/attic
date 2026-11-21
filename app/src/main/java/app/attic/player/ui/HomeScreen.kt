package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicHome
import app.attic.player.ui.model.rememberHomeModel

@Composable
fun HomeScreen(library: Library) {
    val (state, actions) = rememberHomeModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicHome(state, actions)
        UiStyle.Comfort -> ClassicHome(state, actions)
        UiStyle.Classic -> ClassicHome(state, actions)
    }
}
