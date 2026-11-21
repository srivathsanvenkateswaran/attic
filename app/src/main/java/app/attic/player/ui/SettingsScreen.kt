package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSettings
import app.attic.player.ui.model.rememberSettingsModel

@Composable
fun SettingsScreen(library: Library) {
    val (state, actions) = rememberSettingsModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ClassicSettings(state, actions)
        UiStyle.Comfort -> ClassicSettings(state, actions)
        UiStyle.Classic -> ClassicSettings(state, actions)
    }
}
