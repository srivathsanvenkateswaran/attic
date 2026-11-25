package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.media3.common.Player
import app.attic.player.R
import app.attic.player.data.Artist
import app.attic.player.data.LrcParser
import app.attic.player.data.Track
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.contextLabel
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.LyricsActions
import app.attic.player.ui.model.LyricsState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.PlaybackClock
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.MiniPlayerMaxWidthDp
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.rememberPosition
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Now Playing (oneui/05-now-playing.html): the whole screen washed with the cover's colour and
 * following the app's theme. The first page is one screen tall: the header, the cover (it pages
 * sideways through the queue, and steps back to 86% while paused), the title and artist, the line
 * being sung, and the deck holding seek, transport and the four toggles. Below it the lyrics card,
 * "About the artist" and "Credits" as labelled groups; once the deck has scrolled away the title,
 * heart and play button pin to the top as the dock's row. Pulling down at the top closes it.
 */
@Composable
fun ComfortNowPlaying(state: NowPlayingState, actions: NowPlayingActions) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified, deepen = false)
    // A tablet upright (or any window too narrow for the side-by-side layout) keeps this page in a
    // centred column, the cover at most 480dp (docs/design/2026-11-25-tablet/SPEC.md, Now Playing).
    val column = if (LocalWindowLayout.current.widthClass == WidthClass.Compact) null else MiniPlayerMaxWidthDp.dp
    Tinted(seed, wash = true) {
        NowPlayingPage(state, actions, track, column)
    }
}

/** The cover's largest side in a tablet's column. */
private val ColumnCoverMax = 480.dp

/**
 * [column], on a tablet, is the widest the page's content may be: it is centred, the cover at most
 * [ColumnCoverMax], with the cover and title halfway between the header and the deck.
 */
@Composable
private fun NowPlayingPage(state: NowPlayingState, actions: NowPlayingActions, track: Track, column: Dp? = null) {
    val colors = Comfort.colors
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
                    dragOffset.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f))
                }
                return available
            }
        }
    }
    // Where the play button sits on the first page: the compact header takes over once it has scrolled
    // under the header's place. (What follows the page is often too short to scroll the whole deck away.)
    var playCentre by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .background(colors.ground)
            // Swallows taps so they don't reach the screen underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .nestedScroll(dismissDrag),
    ) {
        val pageHeight = maxHeight
        val insets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // The cover takes the kit's width (30dp in from each edge) unless the page is too short for it;
        // whatever height is left over goes above the deck, keeping the controls low.
        val hasLine = state.lyrics.hasLyrics && state.lyrics.lines.isNotEmpty()
        val fixed = insets + HeaderHeight + 4.dp + TitleHeight + (if (hasLine) 30.dp else 0.dp) + DeckHeight + 16.dp
        val side = if (column == null) {
            min(maxWidth - 60.dp, pageHeight - fixed).coerceAtLeast(120.dp)
        } else {
            min(min(min(maxWidth, column) - 60.dp, pageHeight - fixed), ColumnCoverMax).coerceAtLeast(120.dp)
        }
        // Taken out of the state here so the header's slot changes only when the indicator does.
        val equaliser = state.equaliser
        val openEqualiser = actions.openEqualiser
        val columnWidth = if (column == null) Modifier.fillMaxWidth() else Modifier.width(side + 60.dp)
        Column(Modifier.fillMaxSize().verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(columnWidth.height(pageHeight)) {
                NowPlayingHeader(
                    context = contextLabel(state.contextName, state.contextKind),
                    onClose = actions.close,
                    onMore = actions.showMenu,
                    moreDescription = stringResource(R.string.more_options_for_cd, track.title),
                    trailing = { EqualiserIndicator(equaliser, openEqualiser) },
                )
                if (column != null) Spacer(Modifier.weight(1f))
                CoverPager(state, actions, side)
                NowPlayingTitle(track.title, track.artist, onArtistClick = actions.openTrackArtist)
                InlineLyric(state.lyrics, state.clock, track, state.isPlaying)
                Spacer(Modifier.weight(1f))
                Deck(Modifier.onPlaced { playCentre = it.positionInParent().y.roundToInt() + with(density) { PlayCentreInDeck.roundToPx() } }) {
                    Seek(state, track, actions.seekTo)
                    Transport(state.isPlaying, actions.previous, actions.togglePlayPause, actions.next)
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
                        TonalIconButton(Symbol.QueueMusic, stringResource(R.string.now_playing_open_queue_cd), actions.openQueue, large = true)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            Column(columnWidth.navigationBarsPadding().padding(top = 12.dp, bottom = 28.dp)) {
                LyricsSection(state.lyrics, actions.lyrics, state.clock, track, state.isPlaying, actions.openLyrics)
                state.artist?.let { AboutArtist(it, actions.openArtist) }
                Credits(track)
            }
        }

        val headerHeight = with(density) { WindowInsets.statusBars.getTop(this) + 60.dp.roundToPx() }
        // Once the page moves, the status bar keeps the ground under it, so nothing scrolls through it.
        if (scroll.value > 0) {
            Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(colors.ground))
        }
        CompactHeader(
            state,
            actions,
            track,
            visible = scroll.value > playCentre - headerHeight,
            onTop = { scope.launch { scroll.animateScrollTo(0) } },
        )
    }
}

/** `.np-head` (60), `.np-title` (18 + 32 + 22 + 2) and `.deck` (16 above; 14 + 48 + 14 + 76 + 12 + 54 + 14). */
private val HeaderHeight = 60.dp
private val TitleHeight = 74.dp
private val DeckHeight = 248.dp

/** From the deck's top (its 16dp margin included) to the middle of the play button. */
private val PlayCentreInDeck = 16.dp + 14.dp + 48.dp + 14.dp + 38.dp

/**
 * The cover (`.np-cover`) as a pager over the queue: swiping sideways changes song, and settling on a
 * page jumps to that song without changing whether it plays. Paused, the cover steps back to 86%.
 */
@Composable
internal fun CoverPager(state: NowPlayingState, actions: NowPlayingActions, side: androidx.compose.ui.unit.Dp) {
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

    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth().height(side + 4.dp),
        beyondViewportPageCount = 1,
        key = { "$it:${state.queue[it].key}" },
    ) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            NowPlayingCover(
                Art.of(state.queue[page].mediaId),
                paused = !state.isPlaying,
                // The kit's cover is 30dp in from the edges; on a short screen it narrows to fit.
                modifier = Modifier.width(side + 60.dp),
            )
        }
    }
}

/**
 * The line being sung (`.np-lyric`), only with synced lyrics. The row keeps its place through blank
 * lines so the title does not jump.
 */
@Composable
private fun InlineLyric(lyrics: LyricsState, clock: PlaybackClock, track: Track, isPlaying: Boolean) {
    val lines = lyrics.lines
    if (!lyrics.hasLyrics || lines.isEmpty()) return
    val position = rememberPosition(clock, track.key, isPlaying, 400)
    val line = lines.getOrNull(LrcParser.lineAt(lines, position))?.text?.takeIf { it.isNotBlank() }
    Box(Modifier.height(30.dp)) {
        if (line != null) NowPlayingLyric(line)
    }
}

/** The seek bar; the elapsed time inside its fill follows a drag. */
@Composable
internal fun Seek(state: NowPlayingState, track: Track, seekTo: (Long) -> Unit) {
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 250)
    val duration = track.durationMs.coerceAtLeast(1)
    var scrub by remember { mutableStateOf<Float?>(null) }
    // After a seek, hold the new position until the next poll reports it, so the fill doesn't jump back.
    var held by remember(track.key) { mutableStateOf<Pair<Long, Long>?>(null) }
    val shown = scrub?.let { (it * duration).toLong() }
        ?: held?.takeIf { (from, _) -> from == position }?.second
        ?: position
    SeekBar(
        progress = { (shown.toFloat() / duration).coerceIn(0f, 1f) },
        elapsed = formatDuration(shown),
        total = formatDuration(track.durationMs),
        onSeek = { fraction ->
            val target = (fraction * duration).toLong()
            held = position to target
            seekTo(target)
        },
        onScrub = { scrub = it },
        contentDescription = stringResource(R.string.now_playing_seek_cd, track.title),
    )
}

private const val PreviewLines = 4

/**
 * The lyrics card (`.lyrics-card`, sheet 06 E): four lines from the one being sung, the plain text
 * clipped to four lines, or one sentence and at most one action. The spinner takes the button's place.
 */
@Composable
private fun LyricsSection(lyrics: LyricsState, actions: LyricsActions, clock: PlaybackClock, track: Track, isPlaying: Boolean, onOpen: () -> Unit) {
    LyricsCard {
        val lines = lyrics.lines
        when {
            lyrics.hasLyrics && lines.isNotEmpty() -> {
                val position = rememberPosition(clock, track.key, isPlaying, 400)
                val current = LrcParser.lineAt(lines, position).coerceAtLeast(0)
                val blank = stringResource(R.string.lyric_blank_line)
                lines.drop(current).take(PreviewLines).forEachIndexed { i, line ->
                    LyricsLine(line.text.ifBlank { blank }, current = i == 0)
                }
            }
            lyrics.hasLyrics -> LyricsLine(lyrics.plain.orEmpty().trim(), plain = true)
            lyrics.instrumental -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBox(Symbol.MusicNote, size = 44.dp, style = IconBoxStyle.Neutral)
                LyricsMessage(stringResource(R.string.lyrics_instrumental))
            }
            else -> {
                LyricsMessage(stringResource(if (lyrics.checked) R.string.lyrics_not_found else R.string.lyrics_not_downloaded))
                if (lyrics.downloading) {
                    Box(Modifier.padding(top = 14.dp).height(36.dp), contentAlignment = Alignment.CenterStart) { Spinner(small = true) }
                } else {
                    ComfortButton(
                        stringResource(R.string.lyrics_download),
                        actions.download,
                        Modifier.padding(top = 14.dp),
                        size = ButtonSize.Small,
                    )
                }
            }
        }
        if (lyrics.hasLyrics) {
            // Lines end in 8dp of their own; the kit puts 14 above the button.
            ComfortButton(
                stringResource(R.string.lyrics_open),
                onOpen,
                Modifier.padding(top = 6.dp),
                style = ButtonStyle.Tonal,
                size = ButtonSize.Small,
            )
        }
    }
}

/** "About the artist": one large row with the artist's picture, name and song count. Opens the artist. */
@Composable
private fun AboutArtist(artist: Artist, onOpen: () -> Unit) {
    val count = artist.tracks.size
    Label(stringResource(R.string.now_playing_about_artist))
    Group {
        item {
            CollectionRow(
                title = artist.name,
                subtitle = pluralStringResource(R.plurals.now_playing_artist_songs, count, NumberFormat.getIntegerInstance().format(count)),
                lead = { m -> ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), m) },
                onClick = onOpen,
                size = RowSize.Large,
                chevron = true,
            )
        }
    }
}

/** "Credits": performed by, and the composer, album, year and genre when the tags have them. */
@Composable
private fun Credits(track: Track) {
    Label(stringResource(R.string.now_playing_credits))
    Group {
        item { KeyValue(stringResource(R.string.now_playing_performed_by), track.artist) }
        track.composer?.let { composer -> item { KeyValue(stringResource(R.string.now_playing_composer), composer) } }
        track.album?.let { album -> item { KeyValue(stringResource(R.string.now_playing_album), album) } }
        track.year?.let { year -> item { KeyValue(stringResource(R.string.now_playing_year), year.toString()) } }
        track.genre?.let { genre -> item { KeyValue(stringResource(R.string.now_playing_genre), genre) } }
    }
}

/**
 * The compact header (`.mini.top`, frame F): once the deck has scrolled away, the title, heart and
 * play button pin to the top as the dock's row, filled as far as the song has played. Tapping it
 * scrolls back to the player.
 */
@Composable
private fun CompactHeader(state: NowPlayingState, actions: NowPlayingActions, track: Track, visible: Boolean, onTop: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
        val position = rememberPosition(state.clock, track.key, state.isPlaying && visible, 500)
        val duration = track.durationMs.coerceAtLeast(1)
        Box(
            Modifier
                .fillMaxWidth()
                .background(Comfort.colors.ground)
                .pointerInput(Unit) { detectTapGestures { } }
                .statusBarsPadding(),
        ) {
            MiniPlayer(
                title = track.title,
                artist = track.artist,
                art = null,
                progress = { position.toFloat() / duration },
                playing = state.isPlaying,
                favourite = state.liked,
                onToggleFavourite = actions.toggleLike,
                onPlayPause = actions.togglePlayPause,
                onClick = onTop,
                placement = MiniPlayerPlacement.Top,
            )
        }
    }
}
