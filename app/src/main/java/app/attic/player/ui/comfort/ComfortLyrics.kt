package app.attic.player.ui.comfort

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.data.LrcParser
import app.attic.player.data.Track
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.rememberPosition
import kotlin.math.roundToInt

/**
 * Full-screen lyrics (oneui/06-queue-lyrics.html, frames D, G, H and I) on Now Playing's washed
 * ground, light or dark with the app. A bar that says "Lyrics"; synced lines with the one being sung
 * larger and on a tonal bed, about a third of the way down, sung lines receding and coming lines
 * readable, a tap on a line seeking to it; plain text as one block; with nothing saved, the empty
 * block and "Download lyrics". At the foot, the dock's mini-player row on its own: cover, title, the
 * played part filled, and play or pause.
 */
@Composable
fun ComfortLyrics(state: LyricsScreenState, actions: LyricsScreenActions) {
    val track = state.track ?: return
    val window = LocalWindowLayout.current
    TintedCover(track.mediaId, wash = true) {
        when {
            window.playerSideBySide -> ComfortLyricsWide(state, actions, track)
            // A tablet upright: the lines in a centred column at most 640dp wide.
            window.widthClass != WidthClass.Compact -> LyricsPage(state, actions, track, ((window.widthDp - LyricsColumnMax) / 2f - 16f).coerceAtLeast(0f).dp)
            else -> LyricsPage(state, actions, track)
        }
    }
}

/** The lyrics' column on a tablet, at most this wide. */
internal const val LyricsColumnMax = 640f

/** The deck at the foot: the 64dp row, its card's 22dp (or the gesture area) under it, and 84dp of fade over it. */
private val DeckRoom = 64.dp + 22.dp

@Composable
private fun LyricsPage(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, inset: Dp = 0.dp) {
    val colors = Comfort.colors
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DeckRoom
    val lyrics = state.lyrics
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            // Swallows taps so they don't reach the player underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        when {
            lyrics.hasLyrics && lyrics.lines.isNotEmpty() -> SyncedLyrics(state, actions, track, top, bottom, inset)
            lyrics.hasLyrics -> PlainLyrics(lyrics.plain.orEmpty(), top, bottom, inset)
            else -> NoLyrics(state, actions, top, bottom)
        }
        Box(Modifier.fillMaxWidth().background(colors.ground)) {
            Bar(
                stringResource(R.string.lyrics_title),
                onBack = actions.close,
                backSymbol = Symbol.ExpandMore,
                backDescription = stringResource(R.string.lyrics_close_cd),
            )
        }
        LyricsDeck(state, actions, track, Modifier.align(Alignment.BottomCenter))
    }
}

/** `.dock.solo`: the mini player's row alone in the dock, ending in a small play button. */
@Composable
private fun LyricsDeck(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, modifier: Modifier) {
    val colors = Comfort.colors
    val progress = rememberProgress(state.clock, track, state.isPlaying)
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to colors.ground))
            .padding(top = 56.dp),
    ) {
        Dock(
            mini = {
                MiniPlayer(
                    title = track.title,
                    artist = track.artist,
                    art = Art.of(track.mediaId),
                    progress = progress,
                    playing = state.isPlaying,
                    favourite = false,
                    onToggleFavourite = {},
                    onPlayPause = actions.togglePlayPause,
                    onClick = {},
                    placement = MiniPlayerPlacement.Alone,
                    trailing = { PlayButton(state.isPlaying, actions.togglePlayPause, Modifier.padding(end = 6.dp), size = PlaySize.Small) },
                )
            },
            tabs = {},
        )
    }
}

// ── Bodies ───────────────────────────────────────────────────────────────────

/**
 * Timed lines. The one being sung sits about a third of the way down; as it changes the list scrolls
 * and the line grows onto its bed while the old one settles back.
 */
@Composable
internal fun SyncedLyrics(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, top: Dp, bottom: Dp, inset: Dp = 0.dp) {
    val lines = state.lyrics.lines
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 200)
    val current = LrcParser.lineAt(lines, position)
    val listState = remember(track.key) { LazyListState() }
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
                listState.animateScrollBy((y - anchor).toFloat(), tween(LineMs))
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            // Room at the end so the last lines can still reach the anchor.
            contentPadding = PaddingValues(start = 16.dp + inset, end = 16.dp + inset, top = top + 4.dp, bottom = bottom + maxHeight / 2),
        ) {
            itemsIndexed(lines) { index, line ->
                val lineState = when {
                    index == current -> LineState.Current
                    index < current -> LineState.Sung
                    else -> LineState.Upcoming
                }
                SyncedLine(line.text, lineState) { actions.seekTo(line.timeMs) }
            }
        }
        // `.ly::before`: the lines fade out under the bar.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = top)
                .height(72.dp)
                .background(Brush.verticalGradient(0f to Comfort.colors.ground, 6f / 72f to Comfort.colors.ground, 1f to Color.Transparent)),
        )
    }
}

private enum class LineState { Sung, Current, Upcoming }

private const val LineMs = 240

/**
 * One timed line (`.ly p`): 22/30 semibold in `--on-2` to come; sung, a step fainter (`--on-3` mixed
 * with `--on-2`); the line being sung 27/34 bold in `--on` on an accent-soft bed. A blank line is the
 * note glyph.
 */
@Composable
private fun SyncedLine(text: String, state: LineState, onClick: () -> Unit) {
    val colors = Comfort.colors
    val now by animateFloatAsState(if (state == LineState.Current) 1f else 0f, tween(LineMs), label = "lyricNow")
    val color by animateColorAsState(
        when (state) {
            LineState.Current -> colors.on
            LineState.Sung -> lerp(colors.on2, colors.on3, 0.72f)
            LineState.Upcoming -> colors.on2
        },
        tween(LineMs),
        label = "lyricColor",
    )
    val bed = colors.accentSoft
    val style = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight((600 + 100 * now).roundToInt()),
        fontSize = lerp(22.sp, 27.sp, now),
        lineHeight = lerp(30.sp, 34.sp, now),
        letterSpacing = lerp((-0.012).em, (-0.02).em, now),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = lerp(0.dp, 10.dp, now))
            .drawBehind {
                if (now > 0f) {
                    drawRoundRect(
                        bed.copy(alpha = bed.alpha * now),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()),
                    )
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = lerp(9.dp, 18.dp, now), bottom = lerp(9.dp, 19.dp, now)),
    ) {
        if (text.isBlank()) {
            Box(Modifier.heightIn(min = 30.dp), contentAlignment = Alignment.CenterStart) {
                ComfortIcon(Symbol.MusicNote, tint = color, contentDescription = stringResource(R.string.lyric_blank_line))
            }
        } else {
            Text(text, style = style, color = color)
        }
    }
}

/** Lyrics saved without timing (frame H): the paragraphs in ink, nothing highlighted, no auto-scroll. */
@Composable
internal fun PlainLyrics(text: String, top: Dp, bottom: Dp, inset: Dp = 0.dp) {
    val paragraphs = remember(text) { text.trim().split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() } }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 30.dp + inset, end = 30.dp + inset, top = top + 6.dp, bottom = bottom + 24.dp),
    ) {
        for (paragraph in paragraphs) {
            Text(
                paragraph,
                style = onest(20f, 31f, 500, tracking = -0.005f),
                color = Comfort.colors.on,
                modifier = Modifier.padding(bottom = 22.dp),
            )
        }
    }
}

/**
 * Nothing to show (frame I): the kit's empty block with the card's sentence for this state, and
 * "Download lyrics", or the spinner in its place while it runs.
 */
@Composable
internal fun NoLyrics(state: LyricsScreenState, actions: LyricsScreenActions, top: Dp, bottom: Dp) {
    val colors = Comfort.colors
    val lyrics = state.lyrics
    val note = when {
        lyrics.instrumental -> R.string.lyrics_instrumental
        lyrics.checked -> R.string.lyrics_not_found
        else -> R.string.lyrics_not_downloaded
    }
    Column(
        Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = top, bottom = bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBox(Symbol.Lyrics, size = 76.dp, style = IconBoxStyle.Neutral)
        Text(
            stringResource(note),
            style = Comfort.type.body,
            color = colors.on2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 22.dp),
        )
        Box(Modifier.padding(top = 24.dp).height(48.dp), contentAlignment = Alignment.Center) {
            if (lyrics.downloading) {
                Spinner()
            } else {
                ComfortButton(stringResource(R.string.lyrics_download), actions.lyrics.download)
            }
        }
    }
}
