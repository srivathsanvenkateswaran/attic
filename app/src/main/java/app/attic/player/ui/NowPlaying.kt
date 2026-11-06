package app.attic.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.attic.player.attic
import app.attic.player.data.ThemeMode
import app.attic.player.data.Track
import app.attic.player.playback.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Polls the player while [active], since position isn't pushed as state. */
@Composable
fun rememberPosition(trackKey: String?, active: Boolean, intervalMs: Long): Long {
    val client = LocalContext.current.attic.player
    var position by remember { mutableLongStateOf(client.positionMs()) }
    LaunchedEffect(trackKey, active) {
        position = client.positionMs()
        while (active) {
            delay(intervalMs)
            position = client.positionMs()
        }
    }
    return position
}

@Composable
fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit) {
    val app = LocalContext.current.attic
    val track = state.current ?: return
    val position = rememberPosition(track.key, state.isPlaying, 500)
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val rawTint = rememberArtColor(track.mediaId)
    val tint by animateColorAsState(rawTint, tween(400), label = "miniTint")
    val progress = if (track.durationMs > 0) (position.toFloat() / track.durationMs).coerceIn(0f, 1f) else 0f
    val swipe = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .pressable(scale = 0.985f, onClick = onOpen)
            .clip(RoundedCornerShape(6.dp))
            .background(tint),
    ) {
        Row(
            Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(track.mediaId, CoverShape, Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            // Swiping the title skips, with the text following the finger.
            Column(
                Modifier
                    .weight(1f)
                    .offset { IntOffset(swipe.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val moved = swipe.value
                                    if (abs(moved) > size.width * 0.28f) {
                                        if (moved < 0) app.player.next() else app.player.previous()
                                        swipe.snapTo(-moved / 2)
                                    }
                                    swipe.animateTo(0f, tween(220))
                                }
                            },
                        ) { change, amount ->
                            change.consume()
                            scope.launch { swipe.snapTo(swipe.value + amount) }
                        }
                    },
            ) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadedBadge()
                    Spacer(Modifier.width(4.dp))
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(initialDelayMillis = MARQUEE_DELAY_MS),
                    )
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) {
                LikeButton(track.key in liked) { app.userData.toggleLike(track.key) }
                IconButton(onClick = { app.player.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color.White),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

private const val MARQUEE_DELAY_MS = 2500

/** Always dark, because it sits on the song's colour rather than the app background. */
@Composable
fun NowPlayingScreen(state: PlayerUiState, onClose: () -> Unit, onOpenQueue: () -> Unit, onOpenLyrics: () -> Unit) {
    AtticTheme(ThemeMode.Dark) {
        NowPlayingContent(state, onClose, onOpenQueue, onOpenLyrics)
    }
}

@Composable
private fun NowPlayingContent(state: PlayerUiState, onClose: () -> Unit, onOpenQueue: () -> Unit, onOpenLyrics: () -> Unit) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val scope = rememberCoroutineScope()
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val track = state.current ?: return
    val rawTint = rememberArtColor(track.mediaId)
    val tint by animateColorAsState(rawTint, tween(600), label = "nowPlayingTint")

    val dragOffset = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    val dismissPx = with(LocalDensity.current) { 160.dp.toPx() }
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
                    dragOffset.animateTo(0f)
                }
                return available
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .background(MaterialTheme.colorScheme.background)
            // Swallows taps so they don't reach the screen underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .nestedScroll(dismissDrag),
    ) {
        val pageHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(
                Modifier
                    .height(pageHeight)
                    .background(Brush.verticalGradient(0f to tint, 0.72f to Color.Black.copy(alpha = 0.72f).compositeOver(tint), 1f to MaterialTheme.colorScheme.background))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                Header(state, track, onClose)
                Spacer(Modifier.weight(1f))
                CoverPager(state)
                Spacer(Modifier.weight(1f))
                CurrentLyricLine(track, state.isPlaying)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = (track.artistKeys.firstOrNull()?.let { key -> Modifier.pressable { ui.openArtist(key) } } ?: Modifier)
                                .basicMarquee(initialDelayMillis = MARQUEE_DELAY_MS),
                        )
                    }
                    LikeButton(track.key in liked, Modifier.offset(x = 10.dp), size = 28.dp) { app.userData.toggleLike(track.key) }
                }

                Spacer(Modifier.height(12.dp))
                SeekSection(track, state.isPlaying)
                Spacer(Modifier.height(4.dp))
                Transport(state)

                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DownloadedBadge()
                        Spacer(Modifier.width(6.dp))
                        Text("Downloaded", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenQueue, modifier = Modifier.offset(x = 10.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Open queue")
                    }
                }
            }

            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LyricsCard(track, state.isPlaying, tint, onOpenLyrics)
                AboutArtistCard(track)
                CreditsCard(track)
            }
        }
        StickyMiniHeader(state, track, scroll.value > with(LocalDensity.current) { (pageHeight * 0.55f).toPx() }, tint)
    }
}

@Composable
private fun Header(state: PlayerUiState, track: Track, onClose: () -> Unit) {
    val ui = LocalUi.current
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player", modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Playing from", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(
                state.contextName ?: "Your library",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { ui.showMenu(TrackMenuRequest(track)) }, modifier = Modifier.offset(x = 12.dp)) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${track.title}")
        }
    }
}

/** Swipe the cover sideways to change song, and the cover shrinks a little while paused. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverPager(state: PlayerUiState) {
    val client = LocalContext.current.attic.player
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
            if (page != index) client.skipTo(page)
        }
    }

    val inset by animateDpAsState(if (state.isPlaying) 0.dp else 14.dp, tween(260), label = "coverInset")
    HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(1f), pageSpacing = 16.dp, key = { "$it:${state.queue[it].key}" }) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CoverArt(
                state.queue[page].mediaId,
                BigCoverShape,
                Modifier.fillMaxSize().padding(if (page == state.currentIndex) inset else 0.dp),
                iconSize = 96.dp,
            )
        }
    }
}

@Composable
private fun Transport(state: PlayerUiState) {
    val app = LocalContext.current.attic
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        ToggleIcon(
            icon = Icons.Rounded.Shuffle,
            active = state.shuffle,
            description = if (state.shuffle) "Turn shuffle off" else "Turn shuffle on",
            onClick = { app.player.toggleShuffle() },
        )
        IconButton(onClick = { app.player.previous() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp))
        }
        PlayCircle(state.isPlaying, { app.player.togglePlayPause() }, size = 66.dp, container = Color.White, content = Color.Black)
        IconButton(onClick = { app.player.next() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp))
        }
        val (icon, label) = when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne to "Repeat one, tap to turn off"
            Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat to "Repeat all, tap to repeat one"
            else -> Icons.Rounded.Repeat to "Repeat off, tap to repeat all"
        }
        ToggleIcon(icon, state.repeatMode != Player.REPEAT_MODE_OFF, label) { app.player.cycleRepeat() }
    }
}

/** Icon that turns green with a small dot beneath it while on. */
@Composable
private fun ToggleIcon(icon: ImageVector, active: Boolean, description: String, onClick: () -> Unit) {
    val tint by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else Color.White, tween(160), label = "toggle")
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint)
            if (active) {
                Box(
                    Modifier
                        .offset(y = 16.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
        }
    }
}

@Composable
private fun SeekSection(track: Track, isPlaying: Boolean) {
    val client = LocalContext.current.attic.player
    val position = rememberPosition(track.key, isPlaying, 250)
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val duration = track.durationMs.coerceAtLeast(1)
    val fraction = if (scrubbing) scrubFraction else (position.toFloat() / duration).coerceIn(0f, 1f)
    val shownPosition = if (scrubbing) (scrubFraction * duration).toLong() else position

    SeekBar(
        fraction = fraction,
        scrubbing = scrubbing,
        onScrub = {
            scrubbing = true
            scrubFraction = it
        },
        onCommit = {
            client.seekTo((scrubFraction * duration).toLong())
            scrubbing = false
        },
        label = "Seek in ${track.title}",
    )
    Row(Modifier.fillMaxWidth()) {
        Text(formatDuration(shownPosition), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.weight(1f))
        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
    }
}

/** White line whose handle grows, and whose fill turns green, while being dragged. */
@Composable
fun SeekBar(fraction: Float, scrubbing: Boolean, onScrub: (Float) -> Unit, onCommit: () -> Unit, label: String) {
    val active = if (scrubbing) MaterialTheme.colorScheme.primary else Color.White
    val rest = Color.White.copy(alpha = 0.25f)
    val handle by animateDpAsState(if (scrubbing) 8.dp else 6.dp, tween(120), label = "handle")
    val scrub by rememberUpdatedState(onScrub)
    val commit by rememberUpdatedState(onCommit)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { value ->
                    scrub(value.coerceIn(0f, 1f))
                    commit()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    scrub((offset.x / size.width).coerceIn(0f, 1f))
                    commit()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { scrub((it.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { commit() },
                    onDragCancel = { commit() },
                ) { change, _ ->
                    change.consume()
                    scrub((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val y = size.height / 2
        val x = size.width * fraction
        val stroke = 3.dp.toPx()
        drawLine(rest, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(active, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
        drawCircle(active, handle.toPx(), Offset(x, y))
    }
}

/** Once the controls scroll away, the song and a pause button stay pinned at the top. */
@Composable
private fun StickyMiniHeader(state: PlayerUiState, track: Track, visible: Boolean, tint: Color) {
    val app = LocalContext.current.attic
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val position = rememberPosition(track.key, state.isPlaying && visible, 500)
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f).compositeOver(tint)).statusBarsPadding()) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                LikeButton(track.key in liked) { app.userData.toggleLike(track.key) }
                IconButton(onClick = { app.player.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((position.toFloat() / track.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)).background(Color.White))
            }
        }
    }
}

private val CardColor = Color(0xFF282828)

@Composable
private fun AboutArtistCard(track: Track) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val library by app.library.library.collectAsStateWithLifecycle()
    val artist = track.artistKeys.firstNotNullOfOrNull { library?.artistByKey?.get(it) } ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .pressable(scale = 0.985f) { ui.openArtist(artist.key) },
    ) {
        Box {
            CoverArt(artist.tracks.first().mediaId, RoundedCornerShape(0.dp), Modifier.fillMaxWidth().aspectRatio(1.35f), iconSize = 64.dp)
            Text("About the artist", style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.padding(16.dp))
        }
        Column(Modifier.padding(16.dp)) {
            Text(artist.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${songCount(artist.tracks.size)} in your library",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CreditsCard(track: Track) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Credits", style = MaterialTheme.typography.titleMedium)
        Credit("Performed by", track.artist)
        track.album?.let { Credit("Album", it) }
        track.year?.let { Credit("Year", it.toString()) }
    }
}

@Composable
private fun Credit(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

