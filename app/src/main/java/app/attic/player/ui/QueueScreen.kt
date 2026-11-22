package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.classic.ClassicSurface
import app.attic.player.ui.classic.ClassicQueue
import app.attic.player.ui.expressive.ExpressiveQueue
import app.attic.player.ui.model.rememberQueueModel

@Composable
fun QueueScreen(state: PlayerUiState, onClose: () -> Unit) {
    val (model, actions) = rememberQueueModel(state, onClose)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveQueue(model, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicQueue(model, actions) }
        UiStyle.Classic -> ClassicQueue(model, actions)
    }
}
