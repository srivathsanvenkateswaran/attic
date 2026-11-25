package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.attic.player.R
import app.attic.player.ui.adaptive.LocalInitialPlayerPanel
import app.attic.player.ui.adaptive.PlayerPanel
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.rememberArtColor

/**
 * Now Playing beside its queue or lyrics, for an expanded window wider than it is tall
 * (docs/design/2026-11-25-tablet/SPEC.md, Now Playing; the design file's Tablet page, Now Playing).
 *
 * The start half is the player: the phone's header, then the cover as large as the height allows
 * (at most 440dp) with the next cover after it, the title block, the seek bar, the transport and the
 * tools. In a window under 840dp tall (the Pad sideways) the line being sung is left out and the gaps
 * tighten, so the tools end above the gesture area. The end half sits on the page: a switch between
 * Up next and Lyrics, opening on [LocalInitialPlayerPanel] and remembered across rotation, and under
 * it the queue as the queue screen lists it, or the synced lyrics with the button that opens them
 * full screen at the end of the switch's row.
 */
@Composable
fun ExpressiveNowPlayingWide(
    state: NowPlayingState,
    actions: NowPlayingActions,
    queue: QueueState,
    queueActions: QueueActions,
    lyrics: LyricsScreenState,
    lyricsActions: LyricsScreenActions,
) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified)
    val initial = LocalInitialPlayerPanel.current
    var panel by rememberSaveable { mutableStateOf(initial) }
    FromArt(seed) {
        Row(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // Swallows taps so they don't reach the screen underneath.
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding(),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Header(state, actions, track)
                Player(state, actions, track, lyric = panel == PlayerPanel.UpNext)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(end = WideExtraMargin)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val panels = PlayerPanel.entries
                    ConnectedButtonGroup(
                        options = panels.map { stringResource(it.label) },
                        selectedIndex = panels.indexOf(panel),
                        onSelect = { panel = panels[it] },
                        // The group's own 16dp margins either side of the 320dp switch.
                        modifier = Modifier.width(SwitchWidth + 32.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    if (panel == PlayerPanel.Lyrics) {
                        ExpressiveIconButton(Symbol.OpenInFull, stringResource(R.string.lyrics_open), actions.openLyrics)
                    }
                }
                val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                when (panel) {
                    PlayerPanel.UpNext -> QueueList(queue, queueActions, Modifier.fillMaxSize(), PaddingValues(bottom = bottom))
                    PlayerPanel.Lyrics -> {
                        val words = lyrics.track
                        if (words != null) {
                            Box(Modifier.fillMaxSize().padding(bottom = bottom - 24.dp).fadeEdges()) {
                                LyricsBody(lyrics, lyricsActions, words, top = 0.dp, bottom = 0.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The switch between the panels: 320dp, as the design's side panel component draws it. */
private val SwitchWidth = 320.dp

private val PlayerPanel.label: Int
    get() = when (this) {
        PlayerPanel.UpNext -> R.string.player_panel_up_next
        PlayerPanel.Lyrics -> R.string.lyrics_title
    }

/**
 * The player under the header, sized from the height the half has: the cover takes what the rest
 * leaves, on the 8dp grid, at most 440dp (and none under [MinCover]), and the column is the cover plus
 * the next cover's 72dp and the 8 between them, at least the transport's width, centred in the half.
 */
@Composable
private fun Player(state: NowPlayingState, actions: NowPlayingActions, track: app.attic.player.data.Track, lyric: Boolean) {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val compact = maxHeight + 64.dp < 816.dp
        val fixed = if (compact) CompactFixed else RoomyFixed
        val room = maxHeight - nav - fixed
        val fitted: Dp = (room.value / 8f).toInt().times(8f).coerceAtMost(440f).dp
            .coerceAtMost(maxWidth - 128.dp)
        // A window too short for even a small cover (840 by 480dp) leaves it out, so the controls fit.
        val side = if (fitted >= MinCover) fitted else 0.dp
        val pager = side + Peek + 8.dp
        // Never narrower than the transport, so a short window (a 7-inch tablet sideways) shrinks the
        // cover but keeps next in view; the cover then starts at the column's start, as the title does.
        val column = max(pager, TransportWidth)
        Column(Modifier.width(column + 48.dp)) {
            if (side > 0.dp) {
                CoverPager(
                    state,
                    actions,
                    cover = side,
                    modifier = Modifier.width(pager + 48.dp).padding(top = if (compact) 0.dp else 4.dp),
                    inset = 24.dp,
                    sliver = Peek,
                )
            }
            TitleBlock(state, actions, track, top = if (compact) 16.dp else 26.dp, lyric = lyric && !compact)
            SeekSection(state, track, actions.seekTo, top = if (compact) 8.dp else 10.dp)
            Transport(
                playing = state.isPlaying,
                onPrevious = actions.previous,
                onPlayPause = actions.togglePlayPause,
                onNext = actions.next,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = if (compact) 12.dp else 22.dp),
            )
            Tools(
                state,
                actions,
                Modifier.padding(start = 24.dp, end = 24.dp, top = if (compact) 12.dp else 20.dp, bottom = if (compact) 8.dp else 24.dp),
            )
        }
    }
}

/** The transport's width while paused: two 76dp skip buttons, the 152dp play control, 8dp between. */
private val TransportWidth = (76 + 8 + 152 + 8 + 76).dp

/** The smallest cover the player shows; a shorter window leaves the cover out. */
private val MinCover = 96.dp

/** The next cover's width beside the cover. */
private val Peek = 72.dp

/** Everything in the column but the cover, tight (a window under 840dp tall) and with room. */
private val CompactFixed = (16 + 62 + 8 + 44 + 15 + 12 + 80 + 12 + 56 + 8).dp
private val RoomyFixed = (4 + 118 + 10 + 44 + 15 + 22 + 80 + 20 + 56 + 24).dp
