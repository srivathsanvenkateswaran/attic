package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSurface
import app.attic.player.ui.classic.ClassicSearch
import app.attic.player.ui.expressive.ExpressiveSearch
import app.attic.player.ui.model.rememberSearchModel

@Composable
fun SearchScreen(library: Library) {
    val (state, actions) = rememberSearchModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveSearch(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicSearch(state, actions) }
        UiStyle.Classic -> ClassicSearch(state, actions)
    }
}
