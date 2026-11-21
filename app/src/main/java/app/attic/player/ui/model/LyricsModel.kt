package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.LrcParser
import app.attic.player.data.LyricLine
import app.attic.player.data.LyricsResult
import app.attic.player.data.Track
import app.attic.player.playback.PlayerUiState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** A song's saved lyrics, and whether a download for it is under way. */
@Immutable
class LyricsState(
    /** Timed lines, empty when the lyrics are plain or missing. */
    val lines: List<LyricLine>,
    val plain: String?,
    /** A lookup has run for this song, whatever it found. */
    val checked: Boolean,
    val found: Boolean,
    val instrumental: Boolean,
    /** Found, and there is something to show: timed lines or plain text. */
    val hasLyrics: Boolean,
    val downloading: Boolean,
)

class LyricsActions(
    /** Looks the song up on lrclib.net. Says so in a message when nothing is found or the phone is offline. */
    val download: () -> Unit,
)

@Composable
fun rememberLyricsModel(track: Track?): UiModel<LyricsState, LyricsActions> {
    val app = LocalContext.current.attic
    val scope = rememberCoroutineScope()
    val saved by remember(track?.key) { track?.let { app.lyrics.observe(it.key) } ?: flowOf(null) }.collectAsStateWithLifecycle(null)
    val lines = remember(saved) { saved?.synced?.let(LrcParser::parse).orEmpty() }
    val plain = saved?.plain
    var busy by remember(track?.key) { mutableStateOf(false) }

    val state = LyricsState(
        lines = lines,
        plain = plain,
        checked = saved != null,
        found = saved?.found == true,
        instrumental = saved?.instrumental == true,
        hasLyrics = saved?.found == true && !(lines.isEmpty() && plain.isNullOrBlank()),
        downloading = busy,
    )
    val actions = LyricsActions(
        download = {
            if (track != null) {
                busy = true
                scope.launch {
                    val message = when (app.lyrics.download(track)) {
                        LyricsResult.Found -> null
                        LyricsResult.NotFound -> "No lyrics found for ${track.title}"
                        LyricsResult.Offline -> "Connect to the internet to download lyrics"
                    }
                    message?.let { app.messages.tryEmit(it) }
                    busy = false
                }
            }
        },
    )
    return UiModel(state, actions)
}

/** Full-screen lyrics for the song that is playing. */
@Immutable
class LyricsScreenState(
    /** Null once nothing is playing, when the screen draws nothing. */
    val track: Track?,
    val isPlaying: Boolean,
    val lyrics: LyricsState,
    val clock: PlaybackClock,
)

class LyricsScreenActions(
    val close: () -> Unit,
    val togglePlayPause: () -> Unit,
    val seekTo: (Long) -> Unit,
    val lyrics: LyricsActions,
)

@Composable
fun rememberLyricsScreenModel(player: PlayerUiState, onClose: () -> Unit): UiModel<LyricsScreenState, LyricsScreenActions> {
    val app = LocalContext.current.attic
    val track = player.current
    val lyrics = rememberLyricsModel(track)
    val clock = remember(app) { PlaybackClock(app.player::positionMs) }
    val state = LyricsScreenState(track, player.isPlaying, lyrics.state, clock)
    val actions = LyricsScreenActions(
        close = onClose,
        togglePlayPause = { app.player.togglePlayPause() },
        seekTo = { app.player.seekTo(it) },
        lyrics = lyrics.actions,
    )
    return UiModel(state, actions)
}
