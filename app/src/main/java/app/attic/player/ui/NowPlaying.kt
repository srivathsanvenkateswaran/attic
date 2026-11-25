package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.comfort.ComfortMiniPlayer
import app.attic.player.ui.comfort.ComfortNowPlaying
import app.attic.player.ui.expressive.ExpressiveMiniPlayer
import app.attic.player.ui.expressive.ExpressiveNowPlaying
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.comfort.ComfortNowPlayingWide
import app.attic.player.ui.expressive.ExpressiveNowPlayingWide
import app.attic.player.ui.model.rememberLyricsScreenModel
import app.attic.player.ui.model.rememberMiniPlayerModel
import app.attic.player.ui.model.rememberNowPlayingModel
import app.attic.player.ui.model.rememberQueueModel

@Composable
fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit) {
    val (model, actions) = rememberMiniPlayerModel(state, onOpen)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveMiniPlayer(model, actions)
        UiStyle.Comfort -> ComfortMiniPlayer(model, actions)
    }
}

@Composable
fun NowPlayingScreen(state: PlayerUiState, onClose: () -> Unit, onOpenQueue: () -> Unit, onOpenLyrics: () -> Unit) {
    val (model, actions) = rememberNowPlayingModel(state, onClose, onOpenQueue, onOpenLyrics)
    if (LocalWindowLayout.current.playerSideBySide) {
        // The queue and the lyrics sit beside the player, so their models live here too. Neither has a
        // close button in the panel; closing Now Playing closes both.
        val (queue, queueActions) = rememberQueueModel(state, onClose = {})
        val (lyrics, lyricsActions) = rememberLyricsScreenModel(state, onClose = {})
        when (LocalUiStyle.current) {
            UiStyle.Expressive -> ExpressiveNowPlayingWide(model, actions, queue, queueActions, lyrics, lyricsActions)
            UiStyle.Comfort -> ComfortNowPlayingWide(model, actions, queue, queueActions, lyrics, lyricsActions)
        }
        return
    }
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveNowPlaying(model, actions)
        UiStyle.Comfort -> ComfortNowPlaying(model, actions)
    }
}
