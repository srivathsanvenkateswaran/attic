package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortEqualiserPage
import app.attic.player.ui.comfort.ComfortEqualiserSheet
import app.attic.player.ui.expressive.ExpressiveEqualiserPage
import app.attic.player.ui.expressive.ExpressiveEqualiserSheet
import app.attic.player.ui.model.rememberEqualiserPageModel
import app.attic.player.ui.model.rememberEqualiserSheetModel

/** Settings > Playback > Equaliser: the default, and the songs and albums that have their own. */
@Composable
fun EqualiserScreen(library: Library) {
    val (state, actions) = rememberEqualiserPageModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveEqualiserPage(state, actions)
        UiStyle.Comfort -> ComfortEqualiserPage(state, actions)
    }
}

/** The equaliser for the song that is playing, from Now Playing's menu or its indicator. */
@Composable
fun EqualiserSheet(track: Track, library: Library, onDismiss: () -> Unit) {
    val (state, actions) = rememberEqualiserSheetModel(track, library, onDismiss)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveEqualiserSheet(state, actions)
        UiStyle.Comfort -> ComfortEqualiserSheet(state, actions)
    }
}
