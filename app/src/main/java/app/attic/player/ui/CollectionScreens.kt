package app.attic.player.ui

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Library
import app.attic.player.data.Track
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
private fun CollapsingBar(title: String, tint: Color, list: LazyListState, actions: @Composable () -> Unit = {}) {
    val ui = LocalUi.current
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
        BackButton({ ui.back() }, scrim = if (collapsed) Color.Transparent else Color.Black.copy(alpha = 0.35f))
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
        CollapsingBar(title, tint, state, actions)
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
private fun PlayControls(tracks: List<Track>, contextName: String, leading: @Composable () -> Unit = {}) {
    val ui = LocalUi.current
    val app = LocalContext.current.attic
    val player by app.player.state.collectAsStateWithLifecycle()
    val isThisContext = player.contextName == contextName && player.current?.key in tracks.map { it.key }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        CoverArt(tracks.firstOrNull()?.mediaId, CoverShape, Modifier.size(width = 34.dp, height = 44.dp), iconSize = 16.dp)
        IconButton(onClick = { ui.addToPlaylist(tracks.map { it.key }) }, enabled = tracks.isNotEmpty()) {
            Icon(
                Icons.Rounded.AddCircleOutline,
                contentDescription = "Add $contextName to a playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        DownloadedBadge(size = 24.dp)
        leading()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { app.player.shufflePlay(tracks, contextName) }, enabled = tracks.isNotEmpty()) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle $contextName",
                tint = if (player.shuffle && isThisContext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
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
    CoverArt(mediaId, CoverShape, Modifier.fillMaxWidth(0.62f).aspectRatio(1f), iconSize = 64.dp)
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
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val album = library.albumByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    if (album == null) return NotFound("This album")
    val mediaId = album.tracks.first().mediaId

    CollectionFrame(album.name, mediaId) {
        item {
            CollectionHeader(
                title = album.name,
                tintFrom = mediaId,
                cover = { LargeCover(mediaId) },
                subtitle = {
                    ArtistLine(
                        album.artist,
                        album.artistKey?.let { library.artistByKey[it]?.tracks?.first()?.mediaId } ?: mediaId,
                        album.artistKey?.let { k -> { ui.openArtist(k) } },
                    )
                },
                detail = listOfNotNull(if (album.tracks.size == 1) "Single" else "Album", album.year?.toString()).joinToString(" • "),
            )
            PlayControls(album.tracks, album.name)
        }
        itemsIndexed(album.tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                showArt = false,
                onClick = { app.player.play(album.tracks, index, album.name) },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
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
fun ArtistScreen(library: Library, key: String) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val artist = library.artistByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    if (artist == null) return NotFound("This artist")
    val mediaId = artist.tracks.first().mediaId
    // No play counts from a streaming service here, so "Popular" means what you've played most recently.
    val popular = remember(artist, recentKeys) {
        val rank = recentKeys.withIndex().associate { it.value to it.index }
        artist.tracks.sortedBy { rank[it.key] ?: Int.MAX_VALUE }
    }

    CollectionFrame(artist.name, mediaId) {
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
            PlayControls(artist.tracks, artist.name)
            SectionTitle("Popular")
        }
        itemsIndexed(popular.take(POPULAR_COUNT), key = { _, t -> "pop:${t.key}" }) { index, track ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp).width(16.dp),
                )
                TrackRow(
                    track = track,
                    isCurrent = player.current?.key == track.key,
                    isPlaying = player.isPlaying,
                    onClick = { app.player.play(popular, index, artist.name) },
                    onMenu = { ui.showMenu(TrackMenuRequest(track)) },
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
                ) { ui.openAlbum(album.key) }
            }
        }
        if (popular.size > POPULAR_COUNT) {
            item { SectionTitle("All songs") }
            itemsIndexed(popular.drop(POPULAR_COUNT), key = { _, t -> t.key }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = player.current?.key == track.key,
                    isPlaying = player.isPlaying,
                    onClick = { app.player.play(popular, index + POPULAR_COUNT, artist.name) },
                    onMenu = { ui.showMenu(TrackMenuRequest(track)) },
                )
            }
        }
    }
}

private const val POPULAR_COUNT = 5

@Composable
fun FolderScreen(library: Library, name: String) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val folder = library.folders.firstOrNull { it.name == name }
    val player by app.player.state.collectAsStateWithLifecycle()
    if (folder == null) return NotFound("This playlist")
    val first = folder.tracks.first().mediaId

    CollectionFrame(folder.name, first) {
        item {
            CollectionHeader(
                title = folder.name,
                tintFrom = first,
                cover = { MosaicCover(mosaicIds(folder.tracks), Modifier.fillMaxWidth(0.62f)) },
                subtitle = { PlainSubtitle("Playlist from your Music/Offline folder") },
                detail = summarize(folder.tracks),
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
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = remember(likedKeys, library) { likedKeys.mapNotNull { library.trackByKey[it] } }

    CollectionFrame("Liked Songs", tracks.firstOrNull()?.mediaId) {
        item {
            CollectionHeader(
                title = "Liked Songs",
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { LikedTile(Modifier.fillMaxWidth(0.5f).aspectRatio(1f).clip(CoverShape)) },
                subtitle = { PlainSubtitle(summarize(tracks)) },
                detail = null,
            )
            PlayControls(tracks, "Liked Songs")
        }
        if (tracks.isEmpty()) {
            item { EmptyMessage("Songs you like will appear here", "Tap the plus on a song to save it.") }
        }
        itemsIndexed(tracks, key = { _, t -> t.key }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = player.current?.key == track.key,
                isPlaying = player.isPlaying,
                onClick = { app.player.play(tracks, index, "Liked Songs") },
                onMenu = { ui.showMenu(TrackMenuRequest(track)) },
            )
        }
    }
}

@Composable
fun PlaylistScreen(library: Library, id: Long) {
    val app = LocalContext.current.attic
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

    CollectionFrame(
        title = playlist.name,
        tintFrom = tracks.firstOrNull()?.mediaId,
        state = listState,
        actions = {
            Box {
                IconButton(onClick = { overflow = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
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
    ) {
        item(key = "header") {
            CollectionHeader(
                title = playlist.name,
                tintFrom = tracks.firstOrNull()?.mediaId,
                cover = { MosaicCover(mosaicIds(tracks), Modifier.fillMaxWidth(0.62f)) },
                subtitle = { PlainSubtitle(summarize(tracks)) },
                detail = null,
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
                EmptyMessage("Let's find something for your playlist", "Tap the plus above to add songs from your library.")
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
