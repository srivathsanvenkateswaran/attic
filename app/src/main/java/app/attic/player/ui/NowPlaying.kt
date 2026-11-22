package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.classic.ClassicMiniPlayer
import app.attic.player.ui.classic.ClassicNowPlaying
import app.attic.player.ui.comfort.ComfortMiniPlayer
import app.attic.player.ui.comfort.ComfortNowPlaying
import app.attic.player.ui.expressive.ExpressiveMiniPlayer
import app.attic.player.ui.expressive.ExpressiveNowPlaying
import app.attic.player.ui.model.rememberMiniPlayerModel
import app.attic.player.ui.model.rememberNowPlayingModel

@Composable
fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit) {
    val (model, actions) = rememberMiniPlayerModel(state, onOpen)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveMiniPlayer(model, actions)
        UiStyle.Comfort -> ComfortMiniPlayer(model, actions)
        UiStyle.Classic -> ClassicMiniPlayer(model, actions)
    }
}

@Composable
fun NowPlayingScreen(state: PlayerUiState, onClose: () -> Unit, onOpenQueue: () -> Unit, onOpenLyrics: () -> Unit) {
    val (model, actions) = rememberNowPlayingModel(state, onClose, onOpenQueue, onOpenLyrics)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveNowPlaying(model, actions)
        UiStyle.Comfort -> ComfortNowPlaying(model, actions)
        UiStyle.Classic -> ClassicNowPlaying(model, actions)
    }
}
