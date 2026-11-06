package app.offbeat.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.offbeat.player.data.Library
import app.offbeat.player.data.Track
import app.offbeat.player.offbeat
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Art-tinted header shared by album, artist, folder and playlist screens. */
@Composable
private fun CollectionHeader(
    title: String,
    subtitle: String,
    detail: String?,
    tintFrom: Long?,
    cover: @Composable () -> Unit,
    onSubtitleClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val ui = LocalUi.current
    val tint = rememberArtColor(tintFrom)
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(tint, MaterialTheme.colorScheme.background))),
    ) {
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton({ ui.back() })
                Spacer(Modifier.weight(1f))
                actions()
            }
            Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp), contentAlignment = Alignment.Center) {
                cover()
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    .then(if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier),
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
                )
            }
        }
    }
}

/** Shuffle plus a play button that pauses instead if this collection is the one already playing. */
@Composable
private fun PlayControls(tracks: List<Track>, contextName: String, extra: @Composable () -> Unit = {}) {
    val app = LocalContext.current.offbeat
    val player by app.player.state.collectAsStateWithLifecycle()
    val isThisContext = player.contextName == contextName && player.current?.key in tracks.map { it.key }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        extra()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { app.player.shufflePlay(tracks, contextName) }, enabled = tracks.isNotEmpty()) {
            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle $contextName")
        }
        Spacer(Modifier.width(8.dp))
        PlayCircle(
            isPlaying = isThisContext && player.isPlaying,
            onClick = {
                if (isThisContext) app.player.togglePlayPause() else app.player.play(tracks, 0, contextName, shuffled = false)
            },
        )
    }
}

@Composable
private fun LargeCover(mediaId: Long?) {
    CoverArt(mediaId, BigCoverShape, Modifier.fillMaxWidth(0.58f).aspectRatio(1f), iconSize = 64.dp)
}

@Composable
private fun NotFound(what: String) {
    val ui = LocalUi.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackButton({ ui.back() })
        EmptyMessage("$what is gone", "It may have been removed from Music/Offline.")
    }
}

@Composable
fun AlbumScreen(library: Library, key: String) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val album = library.albumByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    if (album == null) return NotFound("This album")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            CollectionHeader(
                title = album.name,
                subtitle = album.artist,
                detail = listOfNotNull(album.year?.toString(), summarize(album.tracks)).joinToString(", "),
                tintFrom = album.tracks.first().mediaId,
                cover = { LargeCover(album.tracks.first().mediaId) },
                onSubtitleClick = album.artistKey?.let { k -> { ui.openArtist(k) } },
            )
            PlayControls(album.tracks, album.name)
        }
        itemsIndexed(album.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                number = track.trackNumber ?: (index + 1),
                showArt = false,
                onClick = { app.player.play(album.tracks, index, album.name) },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
            )
        }
    }
}

@Composable
fun ArtistScreen(library: Library, key: String) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val artist = library.artistByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    if (artist == null) return NotFound("This artist")
    val mediaId = artist.tracks.first().mediaId

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            CollectionHeader(
                title = artist.name,
                subtitle = summarize(artist.tracks),
                detail = null,
                tintFrom = mediaId,
                cover = { CoverArt(mediaId, CircleShape, Modifier.size(180.dp), iconSize = 64.dp) },
            )
            PlayControls(artist.tracks, artist.name)
        }
        if (artist.albums.isNotEmpty()) {
            item {
                SectionTitle("Albums")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(artist.albums, key = { it.key }) { album ->
                        ShelfCard(
                            mediaId = album.tracks.first().mediaId,
                            title = album.name,
                            subtitle = album.year?.toString() ?: songCount(album.tracks.size),
                            onClick = { ui.openAlbum(album.key) },
                        )
                    }
                }
            }
        }
        item { SectionTitle("Songs") }
        itemsIndexed(artist.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                onClick = { app.player.play(artist.tracks, index, artist.name) },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
            )
        }
    }
}

@Composable
fun FolderScreen(library: Library, name: String) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val folder = library.folders.firstOrNull { it.name == name }
    val player by app.player.state.collectAsStateWithLifecycle()
    if (folder == null) return NotFound("This folder")
    val step = (folder.tracks.size / 3).coerceAtLeast(1)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            CollectionHeader(
                title = folder.name,
                subtitle = summarize(folder.tracks),
                detail = "Folder in Music/Offline",
                tintFrom = folder.tracks.first().mediaId,
                cover = {
                    FannedCovers(
                        listOf(step * 2, step, 0).mapNotNull { folder.tracks.getOrNull(it)?.mediaId },
                        coverSize = 150.dp,
                    )
                },
            )
            PlayControls(folder.tracks, folder.name)
        }
        itemsIndexed(folder.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                onClick = { app.player.play(folder.tracks, index, folder.name) },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
            )
        }
    }
}

@Composable
fun LikedScreen(library: Library) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = remember(likedKeys, library) { likedKeys.mapNotNull { library.trackByKey[it] } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            CollectionHeader(
                title = "Liked songs",
                subtitle = summarize(tracks),
                detail = null,
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { LikedTile(Modifier.fillMaxWidth(0.42f).aspectRatio(1f).clip(BigCoverShape)) },
            )
            PlayControls(tracks, "Liked songs")
        }
        if (tracks.isEmpty()) {
            item { EmptyMessage("No liked songs yet", "Tap the heart on a song and it will be saved here.") }
        }
        itemsIndexed(tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                onClick = { app.player.play(tracks, index, "Liked songs") },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
            )
        }
    }
}

@Composable
fun PlaylistScreen(library: Library, id: Long) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == id }
    if (playlist == null) return NotFound("This playlist")

    val tracks = remember(playlist.trackKeys, library) { playlist.trackKeys.mapNotNull { library.trackByKey[it] } }
    var reordering by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

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

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "header") {
            CollectionHeader(
                title = playlist.name,
                subtitle = summarize(tracks),
                detail = null,
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { LargeCover(tracks.firstOrNull()?.mediaId) },
                actions = {
                    Box {
                        IconButton(onClick = { overflow = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit playlist")
                        }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = {
                                    overflow = false
                                    renaming = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete playlist") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = {
                                    overflow = false
                                    confirmingDelete = true
                                },
                            )
                        }
                    }
                },
            )
            PlayControls(tracks, playlist.name) {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add songs")
                }
                IconButton(onClick = { reordering = !reordering }, enabled = tracks.size > 1) {
                    Icon(
                        if (reordering) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                        contentDescription = if (reordering) "Done reordering" else "Reorder songs",
                    )
                }
            }
        }
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyMessage("This playlist is empty", "Tap + above to add songs from your library.")
            }
        }
        items(rows, key = { it.key }) { track ->
            ReorderableItem(reorderState, key = track.key, enabled = reordering) { isDragging ->
                TrackRow(
                    track = track,
                    isCurrent = player.current?.key == track.key,
                    isPlaying = player.isPlaying,
                    modifier = if (isDragging) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier,
                    onClick = {
                        if (!reordering) app.player.play(tracks, tracks.indexOf(track), playlist.name)
                    },
                    onMenu = { ui.showMenu(TrackMenuRequest(track, playlistId = id)) },
                    trailing = if (reordering) {
                        {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        app.userData.reorderPlaylist(id, rows.map { it.key })
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

    if (adding) {
        AddSongsSheet(library, playlist.id, playlist.trackKeys.toSet(), onDismiss = { adding = false })
    }
    if (renaming) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirm = "Rename",
            initial = playlist.name,
            onDismiss = { renaming = false },
            onConfirm = { name ->
                renaming = false
                app.userData.renamePlaylist(id, name)
            },
        )
    }
    if (confirmingDelete) {
        DeletePlaylistDialog(
            name = playlist.name,
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                ui.back()
                app.userData.deletePlaylist(id)
            },
        )
    }
}
