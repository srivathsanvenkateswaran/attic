package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Artist
import app.attic.player.data.Track
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest

@Immutable
class MiniPlayerState(
    /** Null when nothing is queued, when there is no mini player. */
    val track: Track?,
    val isPlaying: Boolean,
    val liked: Boolean,
    val clock: PlaybackClock,
)

class MiniPlayerActions(
    val open: () -> Unit,
    val toggleLike: () -> Unit,
    val togglePlayPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
)

@Composable
fun rememberMiniPlayerModel(player: PlayerUiState, onOpen: () -> Unit): UiModel<MiniPlayerState, MiniPlayerActions> {
    val app = LocalContext.current.attic
    val track = player.current
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val clock = remember(app) { PlaybackClock(app.player::positionMs) }
    val state = MiniPlayerState(track, player.isPlaying, track != null && track.key in liked, clock)
    val actions = MiniPlayerActions(
        open = onOpen,
        toggleLike = { track?.let { app.userData.toggleLike(it.key) } },
        togglePlayPause = { app.player.togglePlayPause() },
        next = { app.player.next() },
        previous = { app.player.previous() },
    )
    return UiModel(state, actions)
}

@Immutable
class NowPlayingState(
    /** Null once nothing is playing, when the screen draws nothing. */
    val track: Track?,
    /** Shown under "Playing from". Null means the whole library. */
    val contextName: String?,
    val isPlaying: Boolean,
    val shuffle: Boolean,
    /** A media3 `Player.REPEAT_MODE_*` value. */
    val repeatMode: Int,
    val queue: List<Track>,
    val currentIndex: Int,
    val liked: Boolean,
    /** The artist behind the artist name and the "About the artist" card, if they're in the library. */
    val artist: Artist?,
    val lyrics: LyricsState,
    val clock: PlaybackClock,
)

class NowPlayingActions(
    val close: () -> Unit,
    val openQueue: () -> Unit,
    val openLyrics: () -> Unit,
    /** The track menu for the song that is playing. */
    val showMenu: () -> Unit,
    val toggleLike: () -> Unit,
    /** Opens the song's first artist. Null when the song has no artist key. */
    val openTrackArtist: (() -> Unit)?,
    /** Opens [NowPlayingState.artist]. */
    val openArtist: () -> Unit,
    /** The cover was swiped to another song in the queue. */
    val skipTo: (Int) -> Unit,
    val togglePlayPause: () -> Unit,
    val previous: () -> Unit,
    val next: () -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeat: () -> Unit,
    val seekTo: (Long) -> Unit,
    val lyrics: LyricsActions,
)

@Composable
fun rememberNowPlayingModel(
    player: PlayerUiState,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
): UiModel<NowPlayingState, NowPlayingActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val library by app.library.library.collectAsStateWithLifecycle()
    val track = player.current
    val lyrics = rememberLyricsModel(track)
    val clock = remember(app) { PlaybackClock(app.player::positionMs) }
    val artist = track?.artistKeys?.firstNotNullOfOrNull { library?.artistByKey?.get(it) }

    val state = NowPlayingState(
        track = track,
        contextName = player.contextName,
        isPlaying = player.isPlaying,
        shuffle = player.shuffle,
        repeatMode = player.repeatMode,
        queue = player.queue,
        currentIndex = player.currentIndex,
        liked = track != null && track.key in liked,
        artist = artist,
        lyrics = lyrics.state,
        clock = clock,
    )
    val actions = NowPlayingActions(
        close = onClose,
        openQueue = onOpenQueue,
        openLyrics = onOpenLyrics,
        showMenu = { track?.let { ui.showMenu(TrackMenuRequest(it)) } },
        toggleLike = { track?.let { app.userData.toggleLike(it.key) } },
        openTrackArtist = track?.artistKeys?.firstOrNull()?.let { key -> { ui.openArtist(key) } },
        openArtist = { artist?.let { ui.openArtist(it.key) } },
        skipTo = { app.player.skipTo(it) },
        togglePlayPause = { app.player.togglePlayPause() },
        previous = { app.player.previous() },
        next = { app.player.next() },
        toggleShuffle = { app.player.toggleShuffle() },
        cycleRepeat = { app.player.cycleRepeat() },
        seekTo = { app.player.seekTo(it) },
        lyrics = lyrics.actions,
    )
    return UiModel(state, actions)
}
