package app.attic.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.attic.player.data.Track
import app.attic.player.attic
import app.attic.player.playback.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Polls the player while [active], since position isn't pushed as state. */
@Composable
private fun rememberPosition(trackKey: String?, active: Boolean, intervalMs: Long): Long {
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

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint)
            .clickable(onClick = onOpen),
    ) {
        Row(
            Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(track.mediaId, CoverShape, Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LikeButton(liked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs",
            tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun NowPlayingScreen(state: PlayerUiState, onClose: () -> Unit, onOpenQueue: () -> Unit) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val scope = rememberCoroutineScope()
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val track = state.current ?: return

    val rawTint = rememberArtColor(track.mediaId)
    val tint by animateColorAsState(rawTint, tween(500), label = "nowPlayingTint")
    val dragOffset = remember { Animatable(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .background(MaterialTheme.colorScheme.background)
            .background(Brush.verticalGradient(0f to tint, 0.75f to MaterialTheme.colorScheme.background))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dragOffset.value > size.height * DISMISS_FRACTION) {
                                onClose()
                                delay(240)
                                dragOffset.snapTo(0f)
                            } else {
                                dragOffset.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { dragOffset.animateTo(0f) } },
                ) { change, amount ->
                    change.consume()
                    scope.launch { dragOffset.snapTo((dragOffset.value + amount).coerceAtLeast(0f)) }
                }
            }
            // Swallows taps so they don't reach the screen underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player", modifier = Modifier.size(30.dp))
                }
                Text(
                    state.contextName ?: "Now playing",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { ui.showMenu(TrackMenuRequest(track)) },
                    modifier = Modifier.offset(x = 12.dp),
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${track.title}")
                }
            }

            Spacer(Modifier.weight(1f))
            CoverArt(track.mediaId, BigCoverShape, Modifier.fillMaxWidth().aspectRatio(1f), iconSize = 96.dp)
            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickableArtist(track) { key -> ui.openArtist(key) },
                    )
                }
                LikeButton(track.key in liked) { app.userData.toggleLike(track.key) }
            }

            Spacer(Modifier.height(14.dp))
            SeekSection(track, state.isPlaying)
            Spacer(Modifier.height(6.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { app.player.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = if (state.shuffle) "Turn shuffle off" else "Turn shuffle on",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { app.player.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(38.dp))
                }
                PlayCircle(state.isPlaying, { app.player.togglePlayPause() }, size = 68.dp)
                IconButton(onClick = { app.player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { app.player.cycleRepeat() }) {
                    val (icon, label) = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne to "Repeat one, tap to turn off"
                        Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat to "Repeat all, tap to repeat one"
                        else -> Icons.Rounded.Repeat to "Repeat off, tap to repeat all"
                    }
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onOpenQueue, modifier = Modifier.offset(x = 8.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Open queue")
                }
            }
        }
    }
}

private fun Modifier.clickableArtist(track: Track, open: (String) -> Unit): Modifier {
    val key = track.artistKeys.firstOrNull() ?: return this
    return this.clickable { open(key) }
}

private const val DISMISS_FRACTION = 0.18f

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
        Text(formatDuration(shownPosition), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeekBar(fraction: Float, onScrub: (Float) -> Unit, onCommit: () -> Unit, label: String) {
    val active = MaterialTheme.colorScheme.primary
    val rest = Color.White.copy(alpha = 0.24f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { value ->
                    onScrub(value.coerceIn(0f, 1f))
                    onCommit()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onScrub((offset.x / size.width).coerceIn(0f, 1f))
                    onCommit()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onScrub((it.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = onCommit,
                    onDragCancel = onCommit,
                ) { change, _ ->
                    change.consume()
                    onScrub((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val y = size.height / 2
        val x = size.width * fraction
        val stroke = 3.dp.toPx()
        drawLine(rest, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(active, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
        drawCircle(active, 7.dp.toPx(), Offset(x, y))
    }
}
