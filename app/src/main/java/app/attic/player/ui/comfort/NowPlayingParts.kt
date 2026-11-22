package app.attic.player.ui.comfort

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol

/**
 * `.np-head`: close (`expand_more`), "Playing from" over the [context] name, more options. Pads for
 * the status bar ([insets]).
 */
@Composable
fun NowPlayingHeader(
    context: String,
    onClose: () -> Unit,
    onMore: () -> Unit,
    moreDescription: String,
    modifier: Modifier = Modifier,
    overline: String = stringResource(R.string.now_playing_playing_from),
    insets: WindowInsets = WindowInsets.statusBars,
) {
    Row(
        modifier.fillMaxWidth().windowInsetsPadding(insets).height(60.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComfortIconButton(Symbol.ExpandMore, stringResource(R.string.now_playing_close_cd), onClose)
        RowText(
            context,
            Modifier.weight(1f).padding(start = 4.dp),
            overline = overline,
            titleStyle = onest(15.5f, 20f, 600),
        )
        ComfortIconButton(Symbol.MoreVert, moreDescription, onMore)
    }
}

/**
 * `.np-cover`: the cover as a large superellipse, 30dp in from the edges. [paused] draws it at 86%
 * (`.np-cover.paused`), animated. Put swipe-to-skip on [modifier].
 */
@Composable
fun NowPlayingCover(art: Art?, paused: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (paused) 0.86f else 1f, tween(260), label = "cover")
    Box(modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 4.dp)) {
        Cover(art, Modifier.fillMaxWidth().scale(scale))
    }
}

/** `.np-title`: the song (26/32 bold, one line) and the artist line, which opens the artist. */
@Composable
fun NowPlayingTitle(title: String, artist: String, modifier: Modifier = Modifier, onArtistClick: (() -> Unit)? = null) {
    val colors = Comfort.colors
    Column(modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 18.dp)) {
        Text(title, style = Comfort.type.title, color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            artist,
            style = onest(16f, 22f),
            color = colors.on2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp).then(if (onArtistClick != null) Modifier.clickable(onClick = onArtistClick) else Modifier),
        )
    }
}

/** `.np-lyric`: the line being sung, in accent ink after the lyrics glyph. Tap opens lyrics. */
@Composable
fun NowPlayingLyric(line: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val ink = Comfort.colors.accentInk
    Row(
        modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 10.dp).rowClick(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComfortIcon(Symbol.Lyrics, size = 18.dp, tint = ink)
        Text(line, style = onest(14f, 20f, 500), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** `.deck`: one card (radius 30) holding everything you touch: [SeekBar], [Transport], [Toggles]. */
@Composable
fun Deck(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clip(ComfortShapes.Floating)
            .background(Comfort.colors.card)
            .padding(14.dp),
        content = content,
    )
}

/**
 * `.seek`: a 48dp track whose fill is the played part, with [elapsed] inside the fill and [total] at
 * the far end. The fill never gets narrower than 64dp, so at 0:00 it is a thumb. Tap or drag:
 * [onScrub] follows the finger (null on release) so the caller can show the time under it, and
 * [onSeek] gets the fraction on release.
 */
@Composable
fun SeekBar(
    progress: () -> Float,
    elapsed: String,
    total: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onScrub: (Float?) -> Unit = {},
    contentDescription: String = stringResource(R.string.now_playing_seek_cd),
) {
    val colors = Comfort.colors
    var dragging by remember { mutableFloatStateOf(-1f) }
    val seek by rememberUpdatedState(onSeek)
    val scrub by rememberUpdatedState(onScrub)
    val shown = { if (dragging >= 0f) dragging else progress().coerceIn(0f, 1f) }
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(ComfortShapes.Control)
            .background(colors.fill)
            .drawBehind {
                val thumb = 64.dp.toPx()
                val width = thumb + (size.width - thumb) * shown()
                val radius = CornerRadius(ComfortShapes.ControlRadius.toPx())
                drawRoundRect(colors.accent, size = Size(width, size.height), cornerRadius = radius)
                drawRoundRect(
                    colors.onAccent.copy(alpha = 0.4f),
                    topLeft = Offset(width - 12.dp.toPx(), 16.dp.toPx()),
                    size = Size(3.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            .pointerInput(Unit) {
                fun fraction(x: Float): Float {
                    val thumb = 64.dp.toPx()
                    return ((x - thumb / 2) / (size.width - thumb)).coerceIn(0f, 1f)
                }
                detectTapGestures { offset -> seek(fraction(offset.x)) }
            }
            .pointerInput(Unit) {
                fun fraction(x: Float): Float {
                    val thumb = 64.dp.toPx()
                    return ((x - thumb / 2) / (size.width - thumb)).coerceIn(0f, 1f)
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragging = fraction(offset.x); scrub(dragging) },
                    onDragEnd = { seek(dragging); dragging = -1f; scrub(null) },
                    onDragCancel = { dragging = -1f; scrub(null) },
                    onHorizontalDrag = { change, _ -> dragging = fraction(change.position.x); scrub(dragging) },
                )
            }
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(progress(), 0f..1f)
                setProgress { value -> seek(value.coerceIn(0f, 1f)); true }
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(elapsed, style = onest(14f, 18f, 600).tabular(), color = colors.onAccent)
            Text(total, style = onest(14f, 18f, 600).tabular(), color = colors.on2)
        }
    }
}

/** `.transport`: previous, the 76dp play button, next, 26dp apart. */
@Composable
fun Transport(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkipButton(Symbol.SkipPrevious, stringResource(R.string.previous), onPrevious)
        PlayButton(playing, onPlayPause)
        SkipButton(Symbol.SkipNext, stringResource(R.string.next), onNext)
    }
}

/** `.tp`: a 64dp target with a 42dp filled glyph at weight 400. */
@Composable
fun SkipButton(symbol: Symbol, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(64.dp)
            .clip(ComfortShapes.Squircle)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        ComfortIcon(symbol, size = 42.dp, filled = true, weight = 400, tint = Comfort.colors.on)
    }
}

/**
 * `.toggles`: shuffle, repeat, favourite and queue as `TonalIconButton(large = true)` and
 * `HeartButton(tonal = true)`, spread across the deck.
 */
@Composable
fun Toggles(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * `.lyrics-card`: "Lyrics" over the lines, in the card colour. Fill it with [LyricsLine],
 * [LyricsMessage], a `ComfortButton(size = ButtonSize.Small)` or a small [Spinner].
 */
@Composable
fun LyricsCard(modifier: Modifier = Modifier, title: String = stringResource(R.string.lyrics_title), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ComfortShapes.Group)
            .background(Comfort.colors.card)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
    ) {
        Text(title, style = Comfort.type.label, color = Comfort.colors.on2, modifier = Modifier.padding(bottom = 12.dp))
        OnCard { content() }
    }
}

/**
 * `.ln`: a lyric line, 22/28 bold. [current] is `.ln.cur` (full ink); otherwise a line to come
 * (`--on-3`). [plain] is `.ln.clip`: unsynced text in full ink, clamped to four lines.
 */
@Composable
fun LyricsLine(text: String, modifier: Modifier = Modifier, current: Boolean = false, plain: Boolean = false) {
    val colors = Comfort.colors
    Text(
        text,
        style = Comfort.type.lyric,
        color = if (current || plain) colors.on else colors.on3,
        maxLines = if (plain) 4 else Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** A bare message in the lyrics card ("Not found"), 15/21 in `--on-2`. */
@Composable
fun LyricsMessage(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Comfort.type.body, color = Comfort.colors.on2, modifier = modifier)
}
