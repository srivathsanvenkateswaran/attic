package app.attic.player.ui.classic

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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Radio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.ui.*
import app.attic.player.ui.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicTrackMenu(state: TrackMenuState, actions: TrackMenuActions) {
    val track = state.track
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = actions.dismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
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

            MenuRow(Icons.Rounded.Radio, "Start radio", actions.startRadio)
            val isLiked = state.liked
            MenuRow(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isLiked) "Remove from liked songs" else "Add to liked songs", actions.toggleLike)
            MenuRow(Icons.AutoMirrored.Rounded.PlaylistPlay, "Play next", actions.playNext)
            MenuRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue", actions.addToQueue)
            MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist", actions.pickPlaylist)
            MenuRow(Icons.Rounded.Language, "Language: ${state.languageName}", actions.pickLanguage)
            if (state.inPlaylist) {
                MenuRow(Icons.Rounded.RemoveCircleOutline, "Remove from this playlist", actions.removeFromPlaylist)
            }
            if (state.canGoToAlbum) {
                MenuRow(Icons.Rounded.Album, "Go to album", actions.goToAlbum)
            }
            if (state.canGoToArtist) {
                MenuRow(Icons.Rounded.PersonOutline, "Go to artist", actions.goToArtist)
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

/** Corrects what radio believes about a song. Your choice is never overwritten. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicLanguagePicker(state: LanguagePickerState, actions: LanguagePickerActions) {
    ModalBottomSheet(
        onDismissRequest = actions.dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            "Song language",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            items(state.choices, key = { it.first }) { (code, name) ->
                MenuRow(if (code == state.current) Icons.Rounded.Check else Icons.Rounded.Language, name) { actions.choose(code) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicPlaylistPicker(state: PlaylistPickerState, actions: PlaylistPickerActions) {
    ModalBottomSheet(
        onDismissRequest = actions.dismiss,
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
                MenuRow(Icons.Rounded.Add, "New playlist", actions.startCreating)
            }
            items(state.playlists, key = { it.id }) { playlist ->
                MenuRow(
                    if (playlist.alreadyIn) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.QueueMusic,
                    "${playlist.name} (${songCount(playlist.songCount)})",
                ) { actions.addTo(playlist) }
            }
        }
    }

}

@Composable
fun ClassicPlaylistNameDialog(state: PlaylistNameState, actions: PlaylistNameActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = actions.dismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(state.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = state.name,
                onValueChange = actions.setName,
                singleLine = true,
                label = { Text("Playlist name") },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = actions.confirm, enabled = state.canConfirm) { Text(state.confirm) }
        },
        dismissButton = {
            TextButton(onClick = actions.dismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun ClassicDeletePlaylistDialog(state: DeletePlaylistState, actions: DeletePlaylistActions) {
    AlertDialog(
        onDismissRequest = actions.dismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Delete ${state.name}?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("The playlist goes away. The songs stay in your library.") },
        confirmButton = { TextButton(onClick = actions.confirm) { Text("Delete playlist") } },
        dismissButton = { TextButton(onClick = actions.dismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicAddSongs(state: AddSongsState, actions: AddSongsActions) {
    ModalBottomSheet(
        onDismissRequest = actions.dismiss,
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
                value = state.query,
                onValueChange = actions.setQuery,
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
                items(state.matches, key = { it.key }) { track ->
                    val added = track.key in state.existing
                    TrackRow(
                        track = track,
                        isCurrent = false,
                        isPlaying = false,
                        onClick = { actions.toggle(track) },
                        onMenu = {},
                        trailing = {
                            IconButton(onClick = { actions.toggle(track) }) {
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
