package app.attic.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Track

val CoverShape = RoundedCornerShape(4.dp)
val BigCoverShape = RoundedCornerShape(8.dp)

/** Space lists leave at the bottom so their last row clears the floating mini player and tab bar. */
val LocalBottomPadding = compositionLocalOf { 0.dp }

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** "17 songs, 1 hr 2 min" */
fun summarize(tracks: List<Track>): String {
    val minutes = tracks.sumOf { it.durationMs } / 60_000
    val length = if (minutes >= 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"
    return "${songCount(tracks.size)}, $length"
}

fun songCount(count: Int) = if (count == 1) "1 song" else "$count songs"

@Composable
fun AtticMark(size: Dp, modifier: Modifier = Modifier) {
    // The roofline spans 60 of the launcher canvas's 108 units, so scale up to drop the margin.
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 108f / 60f),
        )
    }
}

/** Small animated bars marking the song that is playing right now. */
@Composable
fun PlayingBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "bars")
    val phases = listOf(420, 560, 340).map { duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
            label = "bar",
        )
    }
    Canvas(modifier.size(14.dp)) {
        val barWidth = size.width / 4
        phases.forEachIndexed { i, phase ->
            val h = size.height * (if (playing) phase.value else 0.3f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth * 1.5f), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 3),
            )
        }
    }
}

/** The green marker for a saved song. Everything here is on the device, so every row carries it. */
@Composable
fun DownloadedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(10.dp),
        )
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    showArt: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onLongClick = onMenu, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArt) {
            CoverArt(track.mediaId, CoverShape, Modifier.size(50.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    PlayingBars(isPlaying)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DownloadedBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            IconButton(onClick = onMenu) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More options for ${track.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Round play button whose icon morphs between play and pause. */
@Composable
fun PlayCircle(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .pressable(scale = 0.92f, onClick = onClick)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(tween(140), 0.7f) + fadeIn(tween(140))) togetherWith (scaleOut(tween(100), 0.7f) + fadeOut(tween(100)))
            },
            label = "playPause",
        ) { playing ->
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = content,
                modifier = Modifier.size(size * 0.58f),
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp),
    )
}

/** A playlist cover: four covers from its songs, or fewer when there aren't four distinct ones. */
@Composable
fun MosaicCover(mediaIds: List<Long>, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = CoverShape) {
    val ids = mediaIds.take(4)
    if (ids.size < 4) {
        CoverArt(ids.firstOrNull(), shape, modifier.aspectRatio(1f), iconSize = 40.dp)
        return
    }
    Column(modifier.aspectRatio(1f).clip(shape)) {
        ids.chunked(2).forEach { row ->
            Row(Modifier.weight(1f)) {
                row.forEach { id ->
                    CoverArt(id, RoundedCornerShape(0.dp), Modifier.weight(1f).fillMaxWidth().aspectRatio(1f))
                }
            }
        }
    }
}

/** Covers from different albums, so a mosaic doesn't repeat the same picture four times. */
fun mosaicIds(tracks: List<Track>): List<Long> =
    tracks.distinctBy { it.albumKey ?: it.key }.map { it.mediaId }

@Composable
fun EmptyMessage(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}
