package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.comfort.ComfortLyrics
import app.attic.player.ui.classic.ClassicLyricsScreen
import app.attic.player.ui.expressive.ExpressiveLyrics
import app.attic.player.ui.model.rememberLyricsScreenModel

/** Full-screen lyrics. The line being sung stays in view and tapping a line seeks. */
@Composable
fun LyricsScreen(state: PlayerUiState, onClose: () -> Unit) {
    val (model, actions) = rememberLyricsScreenModel(state, onClose)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLyrics(model, actions)
        UiStyle.Comfort -> ComfortLyrics(model, actions)
        UiStyle.Classic -> ClassicLyricsScreen(model, actions)
    }
}
