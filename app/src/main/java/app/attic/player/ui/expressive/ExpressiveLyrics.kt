package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.data.LrcParser
import app.attic.player.data.Track
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.rememberPosition
import kotlin.math.roundToInt

/**
 * Full-screen lyrics (pixel/06-queue-lyrics.html, frames D, G and H) on the cover's scheme, light or
 * dark with the app. A bar at each end on the container: the song at the top, and at the bottom the
 * elapsed time, the progress line (wavy while playing), the length and play or pause. Synced lines
 * follow playback with the line being sung large and in primary, about a third of the way down; tap a
 * line to seek to it. Plain text is one block; with nothing saved, the download action.
 */
@Composable
fun ExpressiveLyrics(state: LyricsScreenState, actions: LyricsScreenActions) {
    val track = state.track ?: return
    FromCover(track.mediaId) {
        LyricsPage(state, actions, track)
    }
}

/** Height of the bottom bar above the gesture area, and of the top bar under the status bar. */
private val BottomBarHeight = 88.dp
private val TopBarHeight = 64.dp

@Composable
private fun LyricsPage(state: LyricsScreenState, actions: LyricsScreenActions, track: Track) {
    val colors = MaterialTheme.colorScheme
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + BottomBarHeight
    val lyrics = state.lyrics
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            // Swallows taps so they don't reach the player underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        when {
            lyrics.hasLyrics && lyrics.lines.isNotEmpty() -> SyncedLyrics(state, actions, track, top, bottom)
            lyrics.hasLyrics -> PlainLyrics(lyrics.plain.orEmpty(), top, bottom)
            else -> NoLyrics(state, actions, top)
        }
        TopBar(track, actions.close, Modifier.align(Alignment.TopCenter))
        BottomBar(state, actions, track, Modifier.align(Alignment.BottomCenter))
    }
}

// ── Bars ─────────────────────────────────────────────────────────────────────

/** Close, then the title over the artist, start-aligned, on the container with rounded bottom corners. */
@Composable
private fun TopBar(track: Track, onClose: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = ExpressiveShapes.ExtraLarge, bottomEnd = ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(TopBarHeight).padding(start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveIconButton(Symbol.KeyboardArrowDown, stringResource(R.string.lyrics_close_cd), onClose)
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Elapsed time, the progress line, the length, and the 56dp play control (`.lyr-bar`). The line shows
 * where the song is and does not seek; seeking is by tapping a line, as today. Paused, it goes flat.
 */
@Composable
private fun BottomBar(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 250)
    val duration = track.durationMs.coerceAtLeast(1)
    val time = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = ExpressiveShapes.ExtraLarge, topEnd = ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .pointerInput(Unit) { detectTapGestures { } }
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(BottomBarHeight).padding(start = 24.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatDuration(position), style = time, color = colors.onSurfaceVariant)
            WavyLinearProgress(
                { (position.toFloat() / duration).coerceIn(0f, 1f) },
                Modifier.weight(1f),
                running = state.isPlaying,
            )
            Text(formatDuration(track.durationMs), style = time, color = colors.onSurfaceVariant)
            PlayButton(state.isPlaying, actions.togglePlayPause, Modifier.padding(start = 8.dp), size = PlaySize.Large)
        }
    }
}

// ── Bodies ───────────────────────────────────────────────────────────────────

private enum class LineState { Sung, Current, Upcoming }

/**
 * Timed lines. The one being sung sits about a third of the way down; as it changes the list scrolls
 * on the default spatial spring and the new line grows while the old one settles back.
 */
@Composable
private fun SyncedLyrics(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, top: Dp, bottom: Dp) {
    val lines = state.lyrics.lines
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 200)
    val current = LrcParser.lineAt(lines, position)
    val listState = remember(track.key) { LazyListState() }
    val blank = stringResource(R.string.lyric_blank_line)
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val anchor = constraints.maxHeight / 3
        LaunchedEffect(listState, current, anchor) {
            if (current < 0) return@LaunchedEffect
            val shown = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == current }
            if (shown == null) {
                // Far off screen (or just opened): jump there, then settle into place.
                listState.scrollToItem(current)
                listState.scrollBy(-(anchor - with(density) { top.roundToPx() }).toFloat())
            } else {
                val y = shown.offset - listState.layoutInfo.viewportStartOffset
                listState.animateScrollBy((y - anchor).toFloat(), ExpressiveMotion.defaultSpatial())
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            // Room at the end so the last lines can still reach the anchor.
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = top + 22.dp, bottom = bottom + maxHeight / 2),
        ) {
            itemsIndexed(lines) { index, line ->
                val lineState = when {
                    index == current -> LineState.Current
                    index < current -> LineState.Sung
                    else -> LineState.Upcoming
                }
                SyncedLine(line.text.ifBlank { blank }, lineState) { actions.seekTo(line.timeMs) }
            }
        }
    }
}

/**
 * One timed line: 24/32 medium in on-surface-variant to come, outline once sung; the line being sung
 * 34/40, emphasised, in primary. Size, weight and spacing move on the default spatial spring, colour
 * on default effects.
 */
@Composable
private fun SyncedLine(text: String, state: LineState, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = Expressive.type
    val now by animateFloatAsState(if (state == LineState.Current) 1f else 0f, ExpressiveMotion.defaultSpatial(), label = "lyricSize")
    val color by animateColorAsState(
        when (state) {
            LineState.Current -> colors.primary
            LineState.Sung -> colors.outline
            LineState.Upcoming -> colors.onSurfaceVariant
        },
        ExpressiveMotion.defaultEffects(),
        label = "lyricColor",
    )
    val f = now.coerceIn(0f, 1f)
    val style = TextStyle(
        // Rounded and at the larger optical size once it is more than half way to being the current line.
        fontFamily = if (f > 0.5f) type.displaySmallEmphasized.fontFamily else type.headlineSmall.fontFamily,
        fontWeight = FontWeight((500 + 200 * f).roundToInt()),
        fontSize = lerp(24.sp, 34.sp, now),
        lineHeight = lerp(32.sp, 40.sp, now),
        letterSpacing = lerp(0.sp, (-0.3).sp, f),
        lineBreak = LineBreak.Heading,
    )
    Text(
        text,
        style = style,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = lerp(0.dp, 4.dp, f), bottom = lerp(14.dp, 20.dp, f))
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/** Lyrics saved without timing: the paragraphs in on-surface, nothing highlighted, no auto-scroll. */
@Composable
private fun PlainLyrics(text: String, top: Dp, bottom: Dp) {
    val paragraphs = remember(text) { text.trim().split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() } }
    val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, lineBreak = LineBreak.Heading)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = top + 22.dp, bottom = bottom + 24.dp),
    ) {
        for (paragraph in paragraphs) {
            Text(paragraph, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 28.dp))
        }
    }
}

/**
 * Nothing to show (frame H): the clover, the card's sentence for this state, and the download action:
 * filled the first time, outlined as a retry, the loading indicator while it runs.
 */
@Composable
private fun NoLyrics(state: LyricsScreenState, actions: LyricsScreenActions, top: Dp) {
    val colors = MaterialTheme.colorScheme
    val lyrics = state.lyrics
    val retry = lyrics.checked
    val note = when {
        lyrics.instrumental -> R.string.lyrics_instrumental
        lyrics.checked -> R.string.lyrics_not_found
        else -> R.string.lyrics_not_downloaded
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = top + 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(128.dp).clip(CloverShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
            SymbolIcon(Symbol.Lyrics, size = 48.dp, tint = colors.onSecondaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(note),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (lyrics.downloading) {
            LoadingIndicator()
        } else {
            ExpressiveButton(
                stringResource(R.string.lyrics_download),
                onClick = actions.lyrics.download,
                variant = if (retry) ButtonVariant.Outlined else ButtonVariant.Filled,
                leading = if (retry) Symbol.Refresh else Symbol.Download,
                leadingFilled = false,
            )
        }
    }
}
