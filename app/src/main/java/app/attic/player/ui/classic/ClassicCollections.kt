package app.attic.player.ui.classic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.data.Track
import app.attic.player.ui.*
import app.attic.player.ui.model.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Cover tint for gradients. Softer in light mode, where the text on top of it is dark. */
@Composable
private fun headerTint(mediaId: Long?): Color {
    val raw = rememberArtColor(mediaId)
    return if (LocalIsDark.current) raw else raw.copy(alpha = 0.4f).compositeOver(MaterialTheme.colorScheme.background)
}

/** Back button and a title that fades in once the header has scrolled away. */
@Composable
private fun CollapsingBar(title: String, tint: Color, list: LazyListState, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    val collapsed by remember(list) {
        derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > HEADER_COLLAPSE_PX }
    }
    val background by animateColorAsState(if (collapsed) tint else Color.Transparent, tween(180), label = "bar")
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack, scrim = if (collapsed) Color.Transparent else Color.Black.copy(alpha = 0.35f))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = if (collapsed) 1f else 0f },
        )
        actions()
    }
}

private const val HEADER_COLLAPSE_PX = 520

/** Screen frame: a list beneath a bar that sits on top of it. */
@Composable
private fun CollectionFrame(
    title: String,
    tintFrom: Long?,
    onBack: () -> Unit,
    state: LazyListState = rememberLazyListState(),
    actions: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val tint = headerTint(tintFrom)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(bottom = LocalBottomPadding.current),
            content = content,
        )
        CollapsingBar(title, tint, state, onBack, actions)
    }
}

@Composable
private fun CollectionHeader(
    title: String,
    tintFrom: Long?,
    cover: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = {},
    detail: String?,
) {
    val tint = headerTint(tintFrom)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(tint, MaterialTheme.colorScheme.background))),
    ) {
        Spacer(Modifier.statusBarsPadding().height(52.dp))
        Box(Modifier.fillMaxWidth().padding(bottom = 18.dp), contentAlignment = Alignment.Center) { cover() }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(6.dp))
        subtitle()
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
        }
    }
}

/** Artist line under a title: their picture in a small circle, then the name. */
@Composable
private fun ArtistLine(name: String, mediaId: Long?, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(mediaId, CircleShape, Modifier.size(24.dp), iconSize = 14.dp)
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlainSubtitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** Row under every header: the downloaded marker on the left, shuffle and a green play button on the right. */
@Composable
private fun PlayControls(state: PlayControlsState, actions: PlayControlsActions, leading: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        CoverArt(state.coverId, CoverShape, Modifier.size(width = 34.dp, height = 44.dp), iconSize = 16.dp)
        IconButton(onClick = actions.addToPlaylist, enabled = state.enabled) {
            Icon(
                Icons.Rounded.AddCircleOutline,
                contentDescription = "Add ${state.contextName} to a playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        DownloadedBadge(size = 24.dp)
        leading()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = actions.shuffle, enabled = state.enabled) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle ${state.contextName}",
                tint = if (state.shuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        PlayCircle(
            isPlaying = state.isPlaying,
            onClick = actions.playOrPause,
        )
    }
}

@Composable
private fun LargeCover(mediaId: Long?) {
    CoverArt(mediaId, CoverShape, Modifier.fillMaxWidth(0.62f).aspectRatio(1f), iconSize = 64.dp)
}

@Composable
private fun NotFound(what: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackButton(onBack)
        EmptyMessage("$what is gone", "It may have been removed from Music/Offline.")
    }
}

@Composable
fun ClassicAlbum(state: AlbumState, actions: AlbumActions) {
    val album = state.album ?: return NotFound("This album", actions.back)
    val mediaId = album.tracks.first().mediaId

    CollectionFrame(album.name, mediaId, actions.back) {
        item {
            CollectionHeader(
                title = album.name,
                tintFrom = mediaId,
                cover = { LargeCover(mediaId) },
                subtitle = {
                    ArtistLine(album.artist, state.artistMediaId, actions.openArtist)
                },
                detail = listOfNotNull(if (album.tracks.size == 1) "Single" else "Album", album.year?.toString()).joinToString(" • "),
            )
            PlayControls(state.controls, actions.controls)
        }
        itemsIndexed(album.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = state.currentKey == track.key,
                isPlaying = state.isPlaying,
                showArt = false,
                onClick = { actions.play(index) },
                onMenu = { actions.showMenu(track) },
            )
        }
        item {
            Text(
                summarize(album.tracks).replace(", ", " • "),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
fun ClassicArtist(state: ArtistState, actions: ArtistActions) {
    val artist = state.artist ?: return NotFound("This artist", actions.back)
    val mediaId = artist.tracks.first().mediaId

    CollectionFrame(artist.name, mediaId, actions.back) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(1.05f)) {
                CoverArt(mediaId, androidx.compose.ui.graphics.RectangleShape, Modifier.fillMaxSize(), iconSize = 96.dp)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))),
                )
                Text(
                    artist.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 48.sp),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            PlainSubtitle(summarize(artist.tracks))
            PlayControls(state.controls, actions.controls)
            SectionTitle("Popular")
        }
        itemsIndexed(state.popular, key = { _, t -> "pop:${t.key}" }) { index, track ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp).width(16.dp),
                )
                TrackRow(
                    track = track,
                    isCurrent = state.currentKey == track.key,
                    isPlaying = state.isPlaying,
                    onClick = { actions.play(index) },
                    onMenu = { actions.showMenu(track) },
                )
            }
        }
        if (artist.albums.isNotEmpty()) {
            item {
                SectionTitle("Popular releases")
            }
            items(artist.albums, key = { "rel:${it.key}" }) { album ->
                ResultRow(
                    album.tracks.first().mediaId,
                    album.name,
                    listOfNotNull(album.year?.toString(), if (album.tracks.size == 1) "Single" else "Album").joinToString(" • "),
                ) { actions.openAlbum(album) }
            }
        }
        if (state.others.isNotEmpty()) {
            item { SectionTitle("All songs") }
            itemsIndexed(state.others, key = { _, t -> t.key }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = state.currentKey == track.key,
                    isPlaying = state.isPlaying,
                    onClick = { actions.play(index + state.popular.size) },
                    onMenu = { actions.showMenu(track) },
                )
            }
        }
    }
}

@Composable
fun ClassicFolder(state: FolderState, actions: FolderActions) {
    val folder = state.folder ?: return NotFound("This playlist", actions.back)
    val first = folder.tracks.first().mediaId

    CollectionFrame(folder.name, first, actions.back) {
        item {
            CollectionHeader(
                title = folder.name,
                tintFrom = first,
                cover = { MosaicCover(mosaicIds(folder.tracks), Modifier.fillMaxWidth(0.62f)) },
                subtitle = { PlainSubtitle("Playlist from your Music/Offline folder") },
                detail = summarize(folder.tracks),
            )
            PlayControls(state.controls, actions.controls)
        }
        itemsIndexed(folder.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = state.currentKey == track.key,
                isPlaying = state.isPlaying,
                onClick = { actions.play(index) },
                onMenu = { actions.showMenu(track) },
            )
        }
    }
}

@Composable
fun ClassicLiked(state: LikedState, actions: LikedActions) {
    val tracks = state.tracks

    CollectionFrame("Liked Songs", tracks.firstOrNull()?.mediaId, actions.back) {
        item {
            CollectionHeader(
                title = "Liked Songs",
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { LikedTile(Modifier.fillMaxWidth(0.5f).aspectRatio(1f).clip(CoverShape)) },
                subtitle = { PlainSubtitle(summarize(tracks)) },
                detail = null,
            )
            PlayControls(state.controls, actions.controls)
        }
        if (tracks.isEmpty()) {
            item { EmptyMessage("Songs you like will appear here", "Tap the plus on a song to save it.") }
        }
        itemsIndexed(tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = state.currentKey == track.key,
                isPlaying = state.isPlaying,
                onClick = { actions.play(index) },
                onMenu = { actions.showMenu(track) },
            )
        }
    }
}

@Composable
fun ClassicPlaylist(state: PlaylistState, actions: PlaylistActions) {
    val playlist = state.playlist ?: return NotFound("This playlist", actions.back)
    val tracks = state.tracks
    val reordering = state.reordering

    // The rows follow the playlist, except mid-drag, when they hold the order being dragged.
    val rows = remember { mutableStateListOf<Track>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(tracks, dragging) {
        if (!dragging) {
            rows.clear()
            rows.addAll(tracks)
        }
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        if (fromIndex >= 0 && toIndex >= 0) rows.add(toIndex, rows.removeAt(fromIndex))
    }

    CollectionFrame(
        title = playlist.name,
        tintFrom = tracks.firstOrNull()?.mediaId,
        onBack = actions.back,
        state = listState,
        actions = {
            Box {
                IconButton(onClick = actions.openOverflow) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
                }
                DropdownMenu(expanded = state.overflowOpen, onDismissRequest = actions.dismissOverflow) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = actions.startRenaming,
                    )
                    DropdownMenuItem(
                        text = { Text("Delete playlist") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = actions.startDeleting,
                    )
                }
            }
        },
    ) {
        item(key = "header") {
            CollectionHeader(
                title = playlist.name,
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { MosaicCover(mosaicIds(tracks), Modifier.fillMaxWidth(0.62f)) },
                subtitle = { PlainSubtitle(summarize(tracks)) },
                detail = null,
            )
            PlayControls(state.controls, actions.controls) {
                IconButton(onClick = actions.startAdding) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add songs")
                }
                IconButton(onClick = actions.toggleReordering, enabled = tracks.size > 1) {
                    Icon(
                        if (reordering) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                        contentDescription = if (reordering) "Done reordering" else "Reorder songs",
                    )
                }
            }
        }
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyMessage("Let's find something for your playlist", "Tap the plus above to add songs from your library.")
            }
        }
        items(rows, key = { it.key }) { track ->
            ReorderableItem(reorderState, key = track.key, enabled = reordering) { isDragging ->
                TrackRow(
                    track = track,
                    isCurrent = state.currentKey == track.key,
                    isPlaying = state.isPlaying,
                    modifier = if (isDragging) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier,
                    onClick = { actions.play(track) },
                    onMenu = { actions.showMenu(track) },
                    trailing = if (reordering) {
                        {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        actions.reorder(rows.map { it.key })
                                        dragging = false
                                    },
                                ),
                            ) {
                                Icon(Icons.Rounded.DragHandle, contentDescription = "Drag ${track.title} to reorder")
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
