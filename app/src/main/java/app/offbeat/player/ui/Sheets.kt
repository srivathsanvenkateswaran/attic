package app.offbeat.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.offbeat.player.OffbeatApp
import app.offbeat.player.data.Library
import app.offbeat.player.data.TitleCleaner
import app.offbeat.player.offbeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Creates a playlist on the app scope, so it survives the dialog closing. */
fun CoroutineScope.launchCreate(app: OffbeatApp, name: String, then: (Long) -> Unit) {
    launch { then(app.userData.createPlaylist(name)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(request: TrackMenuRequest, library: Library, onDismiss: () -> Unit) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val track = request.track
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    var pickingPlaylist by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (pickingPlaylist) {
        PlaylistPickerSheet(trackKey = track.key, onDismiss = onDismiss)
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverArt(track.mediaId, CoverShape, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

            val isLiked = track.key in liked
            MenuRow(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isLiked) "Remove from liked songs" else "Add to liked songs") {
                app.userData.toggleLike(track.key)
                onDismiss()
            }
            MenuRow(Icons.AutoMirrored.Rounded.PlaylistPlay, "Play next") {
                app.player.playNext(listOf(track))
                app.messages.tryEmit("Playing next")
                onDismiss()
            }
            MenuRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue") {
                app.player.addToQueue(listOf(track))
                app.messages.tryEmit("Added to queue")
                onDismiss()
            }
            MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist") { pickingPlaylist = true }
            request.playlistId?.let { id ->
                MenuRow(Icons.Rounded.RemoveCircleOutline, "Remove from this playlist") {
                    app.userData.removeFromPlaylist(id, track.key)
                    onDismiss()
                }
            }
            track.albumKey?.takeIf { it in library.albumByKey }?.let { key ->
                MenuRow(Icons.Rounded.Album, "Go to album") {
                    onDismiss()
                    ui.openAlbum(key)
                }
            }
            track.artistKeys.firstOrNull { it in library.artistByKey }?.let { key ->
                MenuRow(Icons.Rounded.PersonOutline, "Go to artist") {
                    onDismiss()
                    ui.openArtist(key)
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistPickerSheet(trackKey: String, onDismiss: () -> Unit) {
    val app = LocalContext.current.offbeat
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun addTo(id: Long, name: String) {
        app.scope.launch {
            val added = app.userData.addToPlaylist(id, listOf(trackKey))
            app.messages.tryEmit(if (added > 0) "Added to $name" else "Already in $name")
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            "Add to playlist",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            item {
                MenuRow(Icons.Rounded.Add, "New playlist") { creating = true }
            }
            items(playlists, key = { it.id }) { playlist ->
                val alreadyIn = trackKey in playlist.trackKeys
                MenuRow(
                    if (alreadyIn) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.QueueMusic,
                    "${playlist.name} (${songCount(playlist.trackKeys.size)})",
                ) { addTo(playlist.id, playlist.name) }
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = "New playlist",
            confirm = "Create",
            initial = "",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                scope.launch {
                    val id = app.userData.createPlaylist(name)
                    addTo(id, name.trim())
                }
            },
        )
    }
}

@Composable
fun PlaylistNameDialog(
    title: String,
    confirm: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Playlist name") },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun DeletePlaylistDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Delete $name?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("The playlist goes away. The songs stay in your library.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete playlist") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsSheet(library: Library, playlistId: Long, existing: Set<String>, onDismiss: () -> Unit) {
    val app = LocalContext.current.offbeat
    var query by remember { mutableStateOf("") }
    val matches = remember(query, library) {
        val q = TitleCleaner.searchable(query)
        if (q.isEmpty()) {
            library.tracks
        } else {
            library.tracks.filter { TitleCleaner.searchable("${it.title} ${it.artist}").contains(q) }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Text(
                "Add songs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Find a song") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
                items(matches, key = { it.key }) { track ->
                    val added = track.key in existing
                    TrackRow(
                        track = track,
                        isCurrent = false,
                        isPlaying = false,
                        onClick = {
                            if (added) {
                                app.userData.removeFromPlaylist(playlistId, track.key)
                            } else {
                                app.scope.launch { app.userData.addToPlaylist(playlistId, listOf(track.key)) }
                            }
                        },
                        onMenu = {},
                        trailing = {
                            IconButton(
                                onClick = {
                                    if (added) {
                                        app.userData.removeFromPlaylist(playlistId, track.key)
                                    } else {
                                        app.scope.launch { app.userData.addToPlaylist(playlistId, listOf(track.key)) }
                                    }
                                },
                            ) {
                                Icon(
                                    if (added) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = if (added) "Remove ${track.title}" else "Add ${track.title}",
                                    tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
