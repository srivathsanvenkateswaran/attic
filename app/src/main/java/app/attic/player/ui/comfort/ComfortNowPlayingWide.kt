package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.media3.common.Player
import app.attic.player.R
import app.attic.player.data.Track
import app.attic.player.ui.adaptive.LocalInitialPlayerPanel
import app.attic.player.ui.adaptive.PlayerPanel
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.contextLabel
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.rememberArtColor

/** The switch row at the top of the side panel, under the status bar. */
private val PanelHead = 60.dp

/** The deck's height: 14 + seek 48 + transport 14 + 76 + toggles 12 + 54 + 14. */
private val WideDeckHeight = 232.dp

/**
 * Now Playing beside its queue or lyrics, for an expanded window wider than it is tall
 * (docs/design/2026-11-25-tablet/SPEC.md, Now Playing; the design file's Tablet page, Now Playing).
 * Washed with the cover's colour as on the phone. The start half is the player: the header across it,
 * the cover as large as the height leaves (280dp at 720dp tall, at most 440), the title, and the deck
 * at the bottom, all in a column the cover's width plus 60. The line being sung is left out, since
 * Lyrics is one tap away. The end half sits on the page: a switch between Up next and Lyrics
 * (opening on [LocalInitialPlayerPanel], kept across rotation), then the queue as the queue screen
 * lays it out or the synced lyrics, with the button that opens them full screen at the end of the
 * switch's row.
 */
@Composable
fun ComfortNowPlayingWide(
    state: NowPlayingState,
    actions: NowPlayingActions,
    queue: QueueState,
    queueActions: QueueActions,
    lyrics: LyricsScreenState,
    lyricsActions: LyricsScreenActions,
) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified, deepen = false)
    val initial = LocalInitialPlayerPanel.current
    var panel by rememberSaveable { mutableStateOf(initial) }
    Tinted(seed, wash = true) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Comfort.colors.ground)
                // Swallows taps so they don't reach the screen underneath.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            WidePlayer(state, actions, track, Modifier.weight(1f).fillMaxHeight()) { panel = PlayerPanel.UpNext }
            SidePanel(panel, { panel = it }, actions, queue, queueActions, lyrics, lyricsActions, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** The start half: header, cover, title and deck in a column the cover's width plus 60, centred. */
@Composable
private fun WidePlayer(state: NowPlayingState, actions: NowPlayingActions, track: Track, modifier: Modifier, onQueue: () -> Unit) {
    BoxWithConstraints(modifier) {
        val insets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // Header 60, the cover's 4 above it, title 74, then the deck 10 under the title (its own 16dp
        // margin less 6 of space) and 16 over the gesture area.
        val room = maxHeight - insets - 60.dp - 4.dp - 74.dp - 10.dp - WideDeckHeight - 16.dp
        val cover = max(120.dp, min(min(440.dp, room), maxWidth - 120.dp))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            NowPlayingHeader(
                context = contextLabel(state.contextName, state.contextKind),
                onClose = actions.close,
                onMore = actions.showMenu,
                moreDescription = stringResource(R.string.more_options_for_cd, track.title),
            )
            Column(Modifier.width(cover + 60.dp).weight(1f)) {
                CoverPager(state, actions, cover)
                NowPlayingTitle(track.title, track.artist, onArtistClick = actions.openTrackArtist)
                Spacer(Modifier.weight(1f))
                Deck {
                    Seek(state, track, actions.seekTo)
                    Transport(state.isPlaying, actions.previous, actions.togglePlayPause, actions.next)
                    WideToggles(state, actions, onQueue)
                }
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

/** Shuffle, repeat, favourite and the queue, as the phone's deck has them; the queue button shows Up next. */
@Composable
private fun WideToggles(state: NowPlayingState, actions: NowPlayingActions, onQueue: () -> Unit) {
    Toggles {
        TonalIconButton(
            Symbol.Shuffle,
            stringResource(if (state.shuffle) R.string.now_playing_shuffle_off_cd else R.string.now_playing_shuffle_on_cd),
            actions.toggleShuffle,
            checked = state.shuffle,
            large = true,
        )
        val (repeatSymbol, repeatDescription) = when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> Symbol.RepeatOne to R.string.now_playing_repeat_one_cd
            Player.REPEAT_MODE_ALL -> Symbol.Repeat to R.string.now_playing_repeat_all_cd
            else -> Symbol.Repeat to R.string.now_playing_repeat_off_cd
        }
        TonalIconButton(
            repeatSymbol,
            stringResource(repeatDescription),
            actions.cycleRepeat,
            checked = state.repeatMode != Player.REPEAT_MODE_OFF,
            large = true,
        )
        HeartButton(state.liked, actions.toggleLike, tonal = true)
        TonalIconButton(Symbol.QueueMusic, stringResource(R.string.now_playing_open_queue_cd), onQueue, large = true)
    }
}

/**
 * The end half: Up next or Lyrics under the switch. The queue is the queue screen's own list (drag to
 * reorder, swipe to remove, Clear at the foot); the lyrics are the full-screen lyrics' lines.
 */
@Composable
private fun SidePanel(
    panel: PlayerPanel,
    onPanel: (PlayerPanel) -> Unit,
    actions: NowPlayingActions,
    queue: QueueState,
    queueActions: QueueActions,
    lyrics: LyricsScreenState,
    lyricsActions: LyricsScreenActions,
    modifier: Modifier,
) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val top = status + PanelHead
    Box(modifier.padding(end = 8.dp)) {
        when (panel) {
            PlayerPanel.UpNext -> QueuePage(queue, queueActions, top = top, showBar = false)
            PlayerPanel.Lyrics -> {
                val track = lyrics.track
                val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                if (track != null) {
                    Box(Modifier.fillMaxSize().background(Comfort.colors.ground)) {
                        when {
                            lyrics.lyrics.hasLyrics && lyrics.lyrics.lines.isNotEmpty() -> SyncedLyrics(lyrics, lyricsActions, track, top, bottom)
                            lyrics.lyrics.hasLyrics -> PlainLyrics(lyrics.lyrics.plain.orEmpty(), top, bottom)
                            else -> NoLyrics(lyrics, lyricsActions, top, bottom)
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Comfort.colors.ground)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(PanelHead)
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val options = listOf(stringResource(R.string.player_panel_up_next), stringResource(R.string.lyrics_title))
            SubTabs(options, PlayerPanel.entries.indexOf(panel), { onPanel(PlayerPanel.entries[it]) }, Modifier.weight(1f))
            if (panel == PlayerPanel.Lyrics) {
                ComfortIconButton(Symbol.OpenInFull, stringResource(R.string.lyrics_open_full_screen_cd), actions.openLyrics)
            }
        }
    }
}
