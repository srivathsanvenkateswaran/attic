package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSearch
import app.attic.player.ui.comfort.ComfortSearch
import app.attic.player.ui.expressive.ExpressiveSearch
import app.attic.player.ui.model.rememberSearchModel

@Composable
fun SearchScreen(library: Library) {
    val (state, actions) = rememberSearchModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveSearch(state, actions)
        UiStyle.Comfort -> ComfortSearch(state, actions)
        UiStyle.Classic -> ClassicSearch(state, actions)
    }
}
