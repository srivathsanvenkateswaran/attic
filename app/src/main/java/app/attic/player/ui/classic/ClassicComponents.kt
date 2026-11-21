package app.attic.player.ui.classic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Track
import app.attic.player.ui.*

val CoverShape = RoundedCornerShape(4.dp)

val BigCoverShape = RoundedCornerShape(8.dp)

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
fun DownloadedBadge(modifier: Modifier = Modifier, size: Dp = 13.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(size * 0.78f),
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
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, scrim: Color = Color.Transparent) {
    // The round scrim keeps the arrow readable over any cover colour.
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(containerColor = scrim, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
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

/** Outlined plus that pops into a filled green check when a song is saved to Liked songs. */
@Composable
fun LikeButton(liked: Boolean, modifier: Modifier = Modifier, size: Dp = 26.dp, onClick: () -> Unit) {
    val pop = remember { Animatable(1f) }
    val initial = remember { liked }
    LaunchedEffect(liked) {
        if (liked != initial || pop.value != 1f) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (liked) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
            contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs",
            tint = if (liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        )
    }
}

/** Violet-to-pale gradient tile with a white heart for Liked Songs. */
@Composable
fun LikedTile(modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.linearGradient(listOf(Color(0xFF4A17E0), Color(0xFFC4EFD9)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.46f))
    }
}

@Composable
fun FolderCard(folder: FolderPlaylist, onClick: () -> Unit) {
    Column(Modifier.width(160.dp).pressable(onClick = onClick)) {
        MosaicCover(mosaicIds(folder.tracks), Modifier.fillMaxWidth(), BigCoverShape)
        Spacer(Modifier.height(8.dp))
        Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            songCount(folder.tracks.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ShelfCard(
    mediaId: Long?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Column(Modifier.width(160.dp).pressable(onClick = onClick)) {
        CoverArt(mediaId, BigCoverShape, Modifier.fillMaxWidth().aspectRatio(1f), iconSize = 40.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistCircle(artist: Artist, onClick: () -> Unit) {
    Column(Modifier.width(130.dp).pressable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        CoverArt(artist.tracks.first().mediaId, CircleShape, Modifier.size(130.dp), iconSize = 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small round button that opens Settings, in the spot where a streaming app shows your profile. */
@Composable
fun ProfileButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .pressable(scale = 0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = "Settings" },
        contentAlignment = Alignment.Center,
    ) {
        AtticMark(18.dp)
    }
}

@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressable(scale = 0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun searchFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    unfocusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    focusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    focusedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    focusedTrailingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
fun ResultRow(
    mediaId: Long?,
    title: String,
    subtitle: String,
    circle: Boolean = false,
    cover: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) cover() else CoverArt(mediaId, if (circle) CircleShape else CoverShape, Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
