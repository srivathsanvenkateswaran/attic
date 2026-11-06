package app.offbeat.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.offbeat.player.data.Library
import app.offbeat.player.offbeat

private enum class LibraryFilter(val label: String) {
    Playlists("Playlists"),
    Albums("Albums"),
    Artists("Artists"),
    Songs("Songs"),
}

private enum class SongSort(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Added("Recently added"),
}

@Composable
fun LibraryScreen(library: Library) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.Playlists) }
    var sort by rememberSaveable { mutableStateOf(SongSort.Title) }
    var creating by remember { mutableStateOf(false) }
    val player by app.player.state.collectAsStateWithLifecycle()
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()

    val songs = remember(library, sort) {
        when (sort) {
            SongSort.Title -> library.tracks
            SongSort.Artist -> library.tracks.sortedBy { it.artist.lowercase() }
            SongSort.Added -> library.tracks.sortedByDescending { it.dateAddedSec }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OffbeatMark(26.dp)
            Spacer(Modifier.width(10.dp))
            Text("Your Library", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Create playlist")
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryFilter.entries) { option ->
                OffbeatChip(option.label, option == filter) { filter = option }
            }
        }

        // Each filter keeps its own list, so scrolling Songs doesn't move Albums.
        when (filter) {
            LibraryFilter.Playlists -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                item(key = "liked") {
                    PlaylistRowLiked(likedKeys.size) { ui.openLiked() }
                }
                items(playlists, key = { "p${it.id}" }) { playlist ->
                    val cover = playlist.trackKeys.firstNotNullOfOrNull { library.trackByKey[it] }?.mediaId
                    ResultRow(cover, playlist.name, songCount(playlist.trackKeys.size)) { ui.openPlaylist(playlist.id) }
                }
                items(library.folders, key = { "f${it.name}" }) { folder ->
                    ResultRow(folder.tracks.first().mediaId, folder.name, "Folder, ${songCount(folder.tracks.size)}") {
                        ui.openFolder(folder.name)
                    }
                }
            }
            LibraryFilter.Albums -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(library.albums, key = { it.key }) { album ->
                    ResultRow(album.tracks.first().mediaId, album.name, album.artist) { ui.openAlbum(album.key) }
                }
            }
            LibraryFilter.Artists -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(library.artists, key = { it.key }) { artist ->
                    ResultRow(artist.tracks.first().mediaId, artist.name, songCount(artist.tracks.size), circle = true) {
                        ui.openArtist(artist.key)
                    }
                }
            }
            LibraryFilter.Songs -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                item(key = "songs-header") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            songCount(songs.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { app.player.shufflePlay(songs, "All songs") }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle all songs")
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(SongSort.entries) { option ->
                            OffbeatChip(option.label, option == sort) { sort = option }
                        }
                    }
                }
                items(songs, key = { it.key }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.current?.key == track.key,
                        isPlaying = player.isPlaying,
                        onClick = { app.player.play(songs, songs.indexOf(track), "All songs") },
                        onMenu = { ui.showMenu(TrackMenuRequest(track)) },
                    )
                }
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
                app.scope.launchCreate(app, name) { id -> ui.openPlaylist(id) }
            },
        )
    }
}

@Composable
private fun OffbeatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}

@Composable
private fun PlaylistRowLiked(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LikedTile(Modifier.size(48.dp).clip(CoverShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Liked songs", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(songCount(count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
