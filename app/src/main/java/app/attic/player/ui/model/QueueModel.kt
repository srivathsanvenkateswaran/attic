package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Track
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest

/**
 * [queueIndex] is the position in the player, which stays valid while rows are dragged around it.
 * [smartPick] marks a song smart shuffle added from outside the collection.
 */
@Immutable
data class UpNextRow(val id: String, val track: Track, val queueIndex: Int, val smartPick: Boolean = false)

/** The same song can be queued twice, so a row's identity is its key plus which occurrence it is. */
private fun buildRows(state: PlayerUiState): List<UpNextRow> {
    val seen = HashMap<String, Int>()
    return state.queue.mapIndexedNotNull { index, track ->
        val occurrence = seen.merge(track.key, 1, Int::plus)!!
        if (index > state.currentIndex) UpNextRow("${track.key}#$occurrence", track, index, track.key in state.smartPicks) else null
    }
}

@Immutable
class QueueState(
    val current: Track?,
    val isPlaying: Boolean,
    /** What the queue plays from, as Now Playing shows it. Null means the whole library. */
    val contextName: String?,
    /** What [contextName] names, for a style that marks it with a glyph. */
    val contextKind: ContextKind,
    /** Everything after the current song, in play order (which is the shuffled order when shuffle is on). */
    val upNext: List<UpNextRow>,
    /** Where the current song is, for a style that shows how far it has played. */
    val clock: PlaybackClock? = null,
    /** The song playing was added by smart shuffle. */
    val currentSmartPick: Boolean = false,
)

class QueueActions(
    val close: () -> Unit,
    /** Also stops radio, which would otherwise refill it. */
    val clearUpNext: () -> Unit,
    val jumpTo: (queueIndex: Int) -> Unit,
    val remove: (queueIndex: Int) -> Unit,
    /** A row that was at [fromQueueIndex] was dropped at [toUpNextPosition] in the up-next list. */
    val move: (fromQueueIndex: Int, toUpNextPosition: Int) -> Unit,
    val showMenu: (Track) -> Unit,
)

@Composable
fun rememberQueueModel(player: PlayerUiState, onClose: () -> Unit): UiModel<QueueState, QueueActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val library by app.library.library.collectAsStateWithLifecycle()
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val upNext = remember(player.queue, player.currentIndex, player.smartPicks) { buildRows(player) }
    val contextKind = remember(player.contextName, player.current, library, playlists) {
        contextKindOf(player.contextName, player.current, library, playlists.map { it.name })
    }
    val clock = remember(app) { PlaybackClock(app.player::positionMs) }
    val state = QueueState(
        player.current, player.isPlaying, player.contextName, contextKind, upNext, clock,
        currentSmartPick = player.current?.key in player.smartPicks,
    )
    val actions = QueueActions(
        close = onClose,
        clearUpNext = { app.player.clearUpNext() },
        jumpTo = { app.player.jumpTo(it) },
        remove = { app.player.removeFromQueue(it) },
        move = { from, position ->
            val target = player.currentIndex + 1 + position
            if (from >= 0 && from != target) app.player.moveInQueue(from, target)
        },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
    )
    return UiModel(state, actions)
}
