package app.attic.player.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import app.attic.player.R
import androidx.compose.ui.unit.Dp
import app.attic.player.ui.adaptive.MiniPlayerMaxWidthDp
import androidx.compose.foundation.layout.widthIn
import app.attic.player.data.Artist
import app.attic.player.data.LrcParser
import app.attic.player.data.Track
import app.attic.player.playback.eq.EqScope
import app.attic.player.ui.formatDuration
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.rememberPosition
import app.attic.player.ui.model.ContextKind
import app.attic.player.ui.model.LyricsActions
import app.attic.player.ui.model.LyricsState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.PlaybackClock
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Now Playing in the Expressive style (pixel/05-now-playing.html, frames A to D and F): one tonal
 * surface in the cover's scheme, light or dark with the app. The first page is the player; the
 * lyrics card, "About the artist" and "Credits" follow, and a compact header takes over once the
 * transport has scrolled away.
 */
@Composable
fun ExpressiveNowPlaying(state: NowPlayingState, actions: NowPlayingActions) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified)
    // On a tablet that isn't side by side, the phone's player in a centred column (SPEC.md, Now Playing).
    val column = if (isWideWindow()) MiniPlayerMaxWidthDp.dp else null
    FromArt(seed) {
        NowPlayingPage(state, actions, track, column)
    }
}

/** How much of the lyrics card shows under the first page, as the cue to scroll. */
private val Peek = 36.dp

private val SideMargin = 24.dp

@Composable
private fun NowPlayingPage(state: NowPlayingState, actions: NowPlayingActions, track: Track, column: Dp? = null) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val onClose by rememberUpdatedState(actions.close)
    val dragOffset = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val dismissPx = with(density) { 160.dp.toPx() }
    // Pulling down at the top of the page drags the whole player away. Anywhere else it scrolls.
    val dismissDrag = remember(dismissPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dragOffset.value > 0 && available.y < 0) {
                    val consumed = available.y.coerceAtLeast(-dragOffset.value)
                    scope.launch { dragOffset.snapTo(dragOffset.value + consumed) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    scope.launch { dragOffset.snapTo(dragOffset.value + available.y) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffset.value == 0f) return Velocity.Zero
                if (dragOffset.value > dismissPx) {
                    onClose()
                    delay(240)
                    dragOffset.snapTo(0f)
                } else {
                    dragOffset.animateTo(0f, ExpressiveMotion.defaultSpatial())
                }
                return available
            }
        }
    }
    // Where the title block ends on the first page, so the compact header can take over once it has gone.
    var controlsTop by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }
    var titleEnd by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .background(colors.surface)
            // Swallows taps so they don't reach the screen underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .nestedScroll(dismissDrag),
    ) {
        val cover = if (column == null) (maxWidth - 88.dp).coerceAtLeast(160.dp) else (minOf(maxWidth, column) - 88.dp).coerceIn(160.dp, 480.dp)
        val pageHeight = maxHeight - Peek
        // A tablet's column: at most [column] wide, centred, the page scrolling from anywhere across it.
        val centred = if (column == null) Modifier else Modifier.widthIn(max = column)
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll),
            horizontalAlignment = if (column == null) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Column(
                centred
                    .fillMaxWidth()
                    // A tablet's tall window would spread the player out; there it keeps its gaps, as drawn.
                    .then(if (column == null) Modifier.heightIn(min = pageHeight) else Modifier)
                    .statusBarsPadding(),
                verticalArrangement = if (column == null) Arrangement.SpaceBetween else Arrangement.Top,
            ) {
                Header(state, actions, track)
                // On a tablet the cover stops at 480dp, so the pager is as wide as one square page, its
                // insets and the next cover's sliver (88dp), not the whole column.
                CoverPager(state, actions, cover, Modifier.padding(top = 4.dp).then(if (column == null) Modifier else Modifier.width(cover + 88.dp)))
                Column(Modifier.onPlaced { controlsTop = it.positionInParent().y.roundToInt() }) {
                    TitleBlock(
                        state,
                        actions,
                        track,
                        Modifier.onPlaced { titleEnd = (it.positionInParent().y + it.size.height).roundToInt() },
                    )
                    SeekSection(state, track, actions.seekTo)
                    Transport(
                        playing = state.isPlaying,
                        onPrevious = actions.previous,
                        onPlayPause = actions.togglePlayPause,
                        onNext = actions.next,
                        modifier = Modifier.padding(start = SideMargin, end = SideMargin, top = 22.dp),
                    )
                    Tools(state, actions, Modifier.padding(start = SideMargin, end = SideMargin, top = 20.dp, bottom = 24.dp))
                }
            }
            Column(
                centred
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LyricsCard(state.lyrics, actions.lyrics, state.clock, track, state.isPlaying, actions.openLyrics)
                state.artist?.let { ArtistCard(it, actions.openArtist) }
                CreditsCard(track)
            }
        }
        // Positions are below the status bar, as the header's 72dp are. The page is too short to scroll the
        // transport away, so the header follows the title: it carries the title once that has gone.
        val handover = with(density) { 72.dp.toPx() }
        CompactHeader(state, actions, track, visible = scroll.value > controlsTop + titleEnd - handover, column = column)
    }
}

// ── The first page ───────────────────────────────────────────────────────────

/** Close, "Playing from" over the context with a glyph for its kind, and the track menu (`.np-top`). */
@Composable
internal fun Header(state: NowPlayingState, actions: NowPlayingActions, track: Track) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveIconButton(Symbol.KeyboardArrowDown, stringResource(R.string.now_playing_close_cd), actions.close)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.now_playing_playing_from),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                contextSymbol(state.contextKind)?.let {
                    SymbolIcon(it, size = 18.dp, tint = colors.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    contextLabel(state.contextName, state.contextKind),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        EqualiserIndicator(state.equaliser, actions.openEqualiser)
        ExpressiveIconButton(Symbol.MoreVert, stringResource(R.string.more_options_for_cd, track.title), actions.showMenu)
    }
}

/**
 * The pill in the header while the song plays with an equaliser setting of its own or its album's
 * (sheet 11 G): the glyph in primary and "This song" or "This album", opening the equaliser sheet. It
 * fades in and out without sliding, so the context name beside it gives up its width once and does not
 * drift. With no such setting nothing is drawn or laid out.
 */
@Composable
private fun EqualiserIndicator(scope: EqScope?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // On the way out [scope] is already null, so the label is still the one that was showing.
    val last = remember { arrayOf(scope) }
    if (scope != null) last[0] = scope
    AnimatedVisibility(
        visible = scope != null,
        enter = fadeIn(ExpressiveMotion.defaultEffects()),
        exit = fadeOut(ExpressiveMotion.defaultEffects()),
    ) {
        val album = last[0] == EqScope.Album
        val description = stringResource(if (album) R.string.equaliser_indicator_album_cd else R.string.equaliser_indicator_song_cd)
        Row(
            Modifier
                .minimumInteractiveComponentSize()
                .height(32.dp)
                .clip(ExpressiveShapes.Pill)
                .background(colors.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(start = 10.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(Symbol.Equalizer, size = 18.dp, tint = colors.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(if (album) R.string.equaliser_indicator_album else R.string.equaliser_indicator_song),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                // The description above says it in full.
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

internal fun contextSymbol(kind: ContextKind): Symbol? = when (kind) {
    ContextKind.Album -> Symbol.Album
    ContextKind.Artist -> Symbol.Person
    ContextKind.Folder -> Symbol.Folder
    ContextKind.Playlist -> Symbol.PlaylistPlay
    ContextKind.Favourites -> Symbol.Favorite
    ContextKind.Radio -> Symbol.Sensors
    ContextKind.Other -> null
}

/** The model hands over the saved context name; this applies the shared naming ("Favourites"). */
@Composable
internal fun contextLabel(name: String?, kind: ContextKind): String = when {
    name == null -> stringResource(R.string.context_your_library)
    kind == ContextKind.Favourites -> stringResource(R.string.context_favourites)
    kind == ContextKind.Radio -> stringResource(R.string.context_radio, name.substringAfter(" · "))
    else -> name
}

/** Width of the next song's sliver, and its corner. */
private val SliverWidth = 40.dp
private val SliverCorner = 20.dp
private const val PausedScale = 0.86f
private val PlayingCorner = 28.dp
private val PausedCorner = 48.dp

/**
 * The cover pager (`.np-pager`): the song's cover, and the next queue entry as a 40dp sliver cut from
 * the middle of its cover, so the swipe shows. Swiping sideways changes song. Paused, every cover
 * steps back to 86% and its corners round from 28 to 48, on the same spring as the play control. As a
 * page slides in, the sliver widens into the cover.
 */
@Composable
internal fun CoverPager(
    state: NowPlayingState,
    actions: NowPlayingActions,
    cover: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    inset: Dp = 20.dp,
    sliver: Dp = SliverWidth,
) {
    val skipTo by rememberUpdatedState(actions.skipTo)
    val pager = rememberPagerState(initialPage = state.currentIndex.coerceAtLeast(0)) { state.queue.size }
    val index by rememberUpdatedState(state.currentIndex)

    LaunchedEffect(state.currentIndex, state.queue.size) {
        if (state.currentIndex in 0 until state.queue.size && pager.currentPage != state.currentIndex) {
            if (abs(pager.currentPage - state.currentIndex) == 1) {
                pager.animateScrollToPage(state.currentIndex)
            } else {
                pager.scrollToPage(state.currentIndex)
            }
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            if (page != index) skipTo(page)
        }
    }

    val playing by animateFloatAsState(if (state.isPlaying) 1f else 0f, ExpressiveMotion.fastSpatial(), label = "coverPlaying")
    val density = LocalDensity.current
    val geometry = remember(density) {
        with(density) { SliverGeometry(sliver.toPx(), SliverCorner.toPx(), PlayingCorner.toPx(), PausedCorner.toPx() * PausedScale) }
    }
    HorizontalPager(
        state = pager,
        // Inset 20 each side and clipped there, so the song before stays out of sight and the next one
        // shows exactly its sliver.
        modifier = modifier.fillMaxWidth().padding(horizontal = inset).height(cover),
        contentPadding = PaddingValues(end = 8.dp + sliver),
        pageSpacing = 8.dp,
        beyondViewportPageCount = 1,
        key = { "$it:${state.queue[it].key}" },
    ) { page ->
        val art = Art.of(state.queue[page].mediaId)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val frame = geometry.frame(pager.slide(page), playing, size)
                    shape = frame.shape
                    clip = true
                },
        ) {
            Cover(
                art,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val frame = geometry.frame(pager.slide(page), playing, size)
                        scaleX = frame.scale
                        scaleY = frame.scale
                        translationX = frame.centerX - size.width / 2
                    },
                shape = RectangleShape,
            )
        }
    }
}

/** 0 for the page in the main slot (or any before it), 1 for the next one, between while sliding. */
private fun PagerState.slide(page: Int): Float = ((page - currentPage) - currentPageOffsetFraction).coerceIn(0f, 1f)

private class SliverGeometry(val sliverWidth: Float, val sliverCorner: Float, val playingCorner: Float, val pausedCorner: Float) {
    class Frame(val shape: Shape, val scale: Float, val centerX: Float)

    /** The visible part of a page: the full cover at [slide] 0, the sliver at 1. [playing] is 1 playing, 0 paused. */
    fun frame(slide: Float, playing: Float, size: Size): Frame {
        val scale = lerp(PausedScale, 1f, playing).coerceAtMost(1f)
        val full = size.width
        val height = size.height * scale
        val width = lerp(full * scale, sliverWidth, slide)
        val left = lerp((full - full * scale) / 2, 0f, slide)
        val top = (size.height - height) / 2
        val corner = lerp(lerp(pausedCorner, playingCorner, playing.coerceIn(0f, 1f)), sliverCorner, slide)
        return Frame(RoundRectShape(Rect(left, top, left + width, top + height), corner), scale, left + width / 2)
    }
}

private class RoundRectShape(private val rect: Rect, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(corner)))
}

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

/** The current lyric line (only with synced lyrics), the title, and the artist, which opens the artist. */
@Composable
internal fun TitleBlock(
    state: NowPlayingState,
    actions: NowPlayingActions,
    track: Track,
    modifier: Modifier = Modifier,
    top: Dp = 26.dp,
    lyric: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(start = SideMargin, end = SideMargin, top = top)) {
        if (lyric) InlineLyric(state.lyrics, state.clock, track, state.isPlaying)
        NarrowingText(
            track.title,
            style = Expressive.type.headlineLargeEmphasized.copy(letterSpacing = (-0.3).sp),
        )
        val openArtist = actions.openTrackArtist
        Text(
            track.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .then(if (openArtist != null) Modifier.clickable(role = Role.Button, onClick = openArtist) else Modifier),
        )
    }
}

/**
 * The line being sung, over the title (`.np-lyric`). Only for synced lyrics; the row keeps its place
 * through blank lines so the title does not jump.
 */
@Composable
private fun InlineLyric(lyrics: LyricsState, clock: PlaybackClock, track: Track, isPlaying: Boolean) {
    val lines = lyrics.lines
    if (!lyrics.hasLyrics || lines.isEmpty()) return
    val position = rememberPosition(clock, track.key, isPlaying, 400)
    val line = lines.getOrNull(LrcParser.lineAt(lines, position))?.text?.takeIf { it.isNotBlank() }
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(bottom = 10.dp).height(20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (line != null) {
            SymbolIcon(Symbol.Lyrics, size = 18.dp, tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                line,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The wavy seek bar and the times under it. The elapsed time follows a drag. */
@Composable
internal fun SeekSection(state: NowPlayingState, track: Track, seekTo: (Long) -> Unit, top: Dp = 10.dp) {
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 250)
    val duration = track.durationMs.coerceAtLeast(1)
    var scrub by remember { mutableStateOf<Float?>(null) }
    // After a seek, hold the new position until the next poll reports it, so the handle doesn't jump back.
    var held by remember(track.key) { mutableStateOf<Pair<Long, Long>?>(null) }
    val shownPosition = scrub?.let { (it * duration).toLong() }
        ?: held?.takeIf { (from, _) -> from == position }?.second
        ?: position
    SeekBar(
        progress = (shownPosition.toFloat() / duration).coerceIn(0f, 1f),
        playing = state.isPlaying,
        onSeek = { fraction ->
            val target = (fraction * duration).toLong()
            held = position to target
            seekTo(target)
        },
        contentDescription = stringResource(R.string.now_playing_seek_cd, track.title),
        modifier = Modifier.padding(start = SideMargin, end = SideMargin, top = top),
        onScrub = { scrub = it },
        mark = state.transitionAtMs?.let { (it.toFloat() / duration).coerceIn(0f, 1f) },
    )
    SeekTimes(formatDuration(shownPosition), formatDuration(track.durationMs), Modifier.padding(horizontal = SideMargin))
}

/** Shuffle, repeat, favourite and queue on one container (`.np-tools`). On toggles square off and fill. */
@Composable
internal fun Tools(state: NowPlayingState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ExpressiveShapes.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveToggleButton(
            Symbol.Shuffle,
            checked = state.shuffle,
            onCheckedChange = { actions.toggleShuffle() },
            contentDescription = stringResource(if (state.shuffle) R.string.now_playing_shuffle_off_cd else R.string.now_playing_shuffle_on_cd),
        )
        val (repeatSymbol, repeatDescription) = when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> Symbol.RepeatOne to R.string.now_playing_repeat_one_cd
            Player.REPEAT_MODE_ALL -> Symbol.Repeat to R.string.now_playing_repeat_all_cd
            else -> Symbol.Repeat to R.string.now_playing_repeat_off_cd
        }
        ExpressiveToggleButton(
            repeatSymbol,
            checked = state.repeatMode != Player.REPEAT_MODE_OFF,
            onCheckedChange = { actions.cycleRepeat() },
            contentDescription = stringResource(repeatDescription),
        )
        HeartButton(state.liked, actions.toggleLike)
        ExpressiveIconButton(Symbol.QueueMusic, stringResource(R.string.now_playing_open_queue_cd), actions.openQueue)
    }
}

// ── Below the player ─────────────────────────────────────────────────────────

private const val PreviewLines = 4

/**
 * The lyrics card (`.np-card`, sheet 06 frame E). The container and its "Lyrics" label stay put; the
 * body is four lines from the one being sung, the plain text, a note, or the download action, and
 * "Open lyrics" shows only when there is text to open.
 */
@Composable
private fun LyricsCard(lyrics: LyricsState, actions: LyricsActions, clock: PlaybackClock, track: Track, isPlaying: Boolean, onOpen: () -> Unit) {
    InfoCard(title = stringResource(R.string.lyrics_title), symbol = Symbol.Lyrics) {
        val lines = lyrics.lines
        when {
            lyrics.hasLyrics && lines.isNotEmpty() -> {
                val position = rememberPosition(clock, track.key, isPlaying, 400)
                val current = LrcParser.lineAt(lines, position).coerceAtLeast(0)
                val blank = stringResource(R.string.lyric_blank_line)
                lines.drop(current).take(PreviewLines).forEachIndexed { i, line ->
                    LyricLine(
                        line.text.ifBlank { blank },
                        if (i == 0) LyricState.Current else LyricState.Upcoming,
                        maxLines = 2,
                    )
                }
            }
            lyrics.hasLyrics -> LyricLine(lyrics.plain.orEmpty().trim(), LyricState.Plain, maxLines = PreviewLines)
            lyrics.instrumental -> Note(stringResource(R.string.lyrics_instrumental))
            else -> {
                Note(stringResource(if (lyrics.checked) R.string.lyrics_not_found else R.string.lyrics_not_downloaded))
                if (lyrics.downloading) {
                    LoadingIndicator(Modifier.padding(top = 12.dp).offset(x = (-4).dp))
                } else {
                    ExpressiveButton(
                        stringResource(R.string.lyrics_download),
                        onClick = actions.download,
                        modifier = Modifier.padding(top = 16.dp),
                        // A retry after nothing was found is outlined; the first download is the page's one filled button.
                        variant = if (lyrics.checked) ButtonVariant.Outlined else ButtonVariant.Filled,
                        leading = if (lyrics.checked) Symbol.Refresh else Symbol.Download,
                        leadingFilled = false,
                    )
                }
            }
        }
        if (lyrics.hasLyrics) {
            ExpressiveButton(
                stringResource(R.string.lyrics_open),
                onClick = onOpen,
                // 16 above the button; synced lines already end in 10.
                modifier = Modifier.padding(top = if (lines.isNotEmpty()) 6.dp else 16.dp),
                variant = ButtonVariant.Tonal,
                trailing = Symbol.OpenInFull,
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "About the artist" (`.artist-card`): the artist in the cookie, beside their name and song count. Opens the artist. */
@Composable
private fun ArtistCard(artist: Artist, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val count = artist.tracks.size
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), size = 88.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.now_playing_about_artist), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(
                artist.name,
                style = Expressive.type.titleLargeEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                pluralStringResource(R.plurals.now_playing_artist_songs, count, NumberFormat.getIntegerInstance().format(count)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        SymbolIcon(Symbol.ArrowForward, tint = colors.onSurface)
    }
}

/** "Credits": performed by, and the composer, album, year and genre when the tags have them. */
@Composable
private fun CreditsCard(track: Track) {
    InfoCard(title = stringResource(R.string.now_playing_credits)) {
        KeyValue(stringResource(R.string.now_playing_performed_by), track.artist)
        track.composer?.let { KeyValue(stringResource(R.string.now_playing_composer), it) }
        track.album?.let { KeyValue(stringResource(R.string.now_playing_album), it) }
        track.year?.let { KeyValue(stringResource(R.string.now_playing_year), it.toString()) }
        track.genre?.let { KeyValue(stringResource(R.string.now_playing_genre), it) }
    }
}

// ── Once scrolled ────────────────────────────────────────────────────────────

/**
 * The compact header (`.np-sticky`, frame F): title, artist, favourite and the small play control with
 * its progress ring, on the high container with rounded bottom corners. It covers the status bar.
 */
@Composable
private fun CompactHeader(state: NowPlayingState, actions: NowPlayingActions, track: Track, visible: Boolean, column: Dp? = null) {
    AnimatedVisibility(
        visible,
        enter = slideInVertically(ExpressiveMotion.defaultSpatial()) { -it } + fadeIn(ExpressiveMotion.defaultEffects()),
        exit = slideOutVertically(ExpressiveMotion.defaultSpatial()) { -it } + fadeOut(ExpressiveMotion.defaultEffects()),
    ) {
        val colors = MaterialTheme.colorScheme
        val position = rememberPosition(state.clock, track.key, state.isPlaying && visible, 500)
        val duration = track.durationMs.coerceAtLeast(1)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = ExpressiveShapes.ExtraLarge, bottomEnd = ExpressiveShapes.ExtraLarge))
                .background(colors.surfaceContainerHigh)
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding(),
        ) {
            Row(
                (if (column == null) Modifier else Modifier.align(Alignment.TopCenter).widthIn(max = column))
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(start = 24.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                HeartButton(state.liked, actions.toggleLike)
                PlayButton(state.isPlaying, actions.togglePlayPause, progress = { (position.toFloat() / duration).coerceIn(0f, 1f) })
            }
        }
    }
}
