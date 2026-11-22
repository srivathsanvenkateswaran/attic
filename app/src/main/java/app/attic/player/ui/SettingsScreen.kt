package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortSettings
import app.attic.player.ui.expressive.ExpressiveSettings
import app.attic.player.ui.model.rememberSettingsModel

@Composable
fun SettingsScreen(library: Library) {
    val (state, actions) = rememberSettingsModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveSettings(state, actions)
        UiStyle.Comfort -> ComfortSettings(state, actions)
    }
}
