package app.attic.player.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

val CoverShape = RoundedCornerShape(6.dp)
val BigCoverShape = RoundedCornerShape(10.dp)

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** "17 songs, 1 hr 2 min" */
fun summarize(tracks: List<Track>): String {
    val minutes = tracks.sumOf { it.durationMs } / 60_000
    val songs = if (tracks.size == 1) "1 song" else "${tracks.size} songs"
    val length = when {
        minutes >= 60 -> "${minutes / 60} hr ${minutes % 60} min"
        else -> "$minutes min"
    }
    return "$songs, $length"
}

fun songCount(count: Int) = if (count == 1) "1 song" else "$count songs"

@Composable
fun AtticMark(size: Dp, modifier: Modifier = Modifier) {
    // The launcher foreground's disc fills the middle of a 108-unit canvas, so scale up to crop the margin.
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 108f / 56f),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showArt: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMenu)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArt) {
            CoverArt(track.mediaId, CoverShape, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
        } else {
            Box(Modifier.widthIn(min = 28.dp), contentAlignment = Alignment.CenterStart) {
                if (isCurrent) {
                    PlayingBars(isPlaying)
                } else {
                    Text(
                        number?.toString().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent && showArt) {
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
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

@Composable
fun PlayCircle(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    label: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = label ?: if (isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.58f),
        )
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
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
    )
}

/** A folder playlist drawn as a crate of covers slipping out of line. This is the app's signature shape. */
@Composable
fun FannedCovers(mediaIds: List<Long>, modifier: Modifier = Modifier, coverSize: Dp = 92.dp) {
    val tilts = listOf(-11f, 8f, -3f)
    val shifts = listOf(-26.dp, 28.dp, 0.dp)
    val drops = listOf(6.dp, 9.dp, 0.dp)
    Box(modifier.height(coverSize + 24.dp).width(coverSize + 56.dp), contentAlignment = Alignment.Center) {
        // Drawn back to front, so the last cover sits on top of the other two.
        for (i in 0 until 3) {
            val id = mediaIds.getOrNull(i) ?: continue
            CoverArt(
                id,
                CoverShape,
                Modifier
                    .offset(x = shifts[i], y = drops[i])
                    .size(coverSize)
                    .rotate(tilts[i]),
            )
        }
    }
}

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
