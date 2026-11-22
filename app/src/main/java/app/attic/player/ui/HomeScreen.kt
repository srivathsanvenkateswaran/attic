package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortHome
import app.attic.player.ui.expressive.ExpressiveHome
import app.attic.player.ui.model.rememberHomeModel

@Composable
fun HomeScreen(library: Library) {
    val (state, actions) = rememberHomeModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveHome(state, actions)
        UiStyle.Comfort -> ComfortHome(state, actions)
    }
}
