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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.attic.player.R
import app.attic.player.data.Track
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.rememberPosition

/** The start pane of full-screen lyrics beside the lines. */
private val LyricsPane = 360.dp

/**
 * Full-screen lyrics on an expanded window wider than it is tall (docs/design/2026-11-25-tablet/SPEC.md,
 * Full-screen lyrics). A 360dp start pane holds the bar, the cover (264dp), the title and artist, and
 * at its foot the seek bar and the play button in one card, so the phone's mini player row is not
 * needed. The lines take the rest, in a column at most 640dp wide in its middle.
 */
@Composable
internal fun ComfortLyricsWide(state: LyricsScreenState, actions: LyricsScreenActions, track: Track) {
    val colors = Comfort.colors
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            // Swallows taps so they don't reach the player underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(Modifier.width(LyricsPane).fillMaxHeight()) {
            Bar(
                stringResource(R.string.lyrics_title),
                onBack = actions.close,
                backSymbol = Symbol.ExpandMore,
                backDescription = stringResource(R.string.lyrics_close_cd),
            )
            Cover(Art.of(track.mediaId), Modifier.padding(start = 30.dp, top = 8.dp), size = 264.dp)
            NowPlayingTitle(track.title, track.artist)
            Spacer(Modifier.weight(1f))
            LyricsControls(state, actions, track)
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val column = min(LyricsColumnMax.dp, maxWidth - 48.dp)
            // The list pads 16 itself; the lines keep 14 of their own inside the column.
            val inset = ((maxWidth - column) / 2 - 16.dp).coerceAtLeast(0.dp)
            val lyrics = state.lyrics
            val top = status + 32.dp
            val bottom = navigation + 24.dp
            when {
                lyrics.hasLyrics && lyrics.lines.isNotEmpty() -> SyncedLyrics(state, actions, track, top, bottom, inset)
                lyrics.hasLyrics -> PlainLyrics(lyrics.plain.orEmpty(), top, bottom, inset)
                else -> NoLyrics(state, actions, top, bottom)
            }
            // The ground over the lines that have scrolled up past the fade, as the phone's bar is.
            Box(Modifier.fillMaxWidth().height(top).background(colors.ground))
        }
    }
}

/**
 * The seek bar over the play button, in one card (the deck without its toggles). Skipping needs
 * actions [LyricsScreenActions] does not have yet, so only play and pause are here.
 */
@Composable
private fun LyricsControls(state: LyricsScreenState, actions: LyricsScreenActions, track: Track) {
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 250)
    val duration = track.durationMs.coerceAtLeast(1)
    var scrub by remember { mutableStateOf<Float?>(null) }
    val shown = scrub?.let { (it * duration).toLong() } ?: position
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ComfortShapes.Floating)
            .background(Comfort.colors.card)
            .padding(14.dp),
    ) {
        SeekBar(
            progress = { (shown.toFloat() / duration).coerceIn(0f, 1f) },
            elapsed = formatDuration(shown),
            total = formatDuration(track.durationMs),
            onSeek = { actions.seekTo((it * duration).toLong()) },
            onScrub = { scrub = it },
            contentDescription = stringResource(R.string.now_playing_seek_cd, track.title),
        )
        Transport(state.isPlaying, actions.previous, actions.togglePlayPause, actions.next)
    }
}
