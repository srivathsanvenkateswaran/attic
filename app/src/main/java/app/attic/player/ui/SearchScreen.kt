package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSearch
import app.attic.player.ui.model.rememberSearchModel

@Composable
fun SearchScreen(library: Library) {
    val (state, actions) = rememberSearchModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicSearch(state, actions)
        UiStyle.Comfort -> ClassicSearch(state, actions)
        UiStyle.Classic -> ClassicSearch(state, actions)
    }
}
