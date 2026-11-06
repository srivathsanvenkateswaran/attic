package app.attic.player.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Library

private enum class LibraryFilter(val label: String) {
    Playlists("Playlists"),
    Albums("Albums"),
    Artists("Artists"),
    Downloaded("Downloaded"),
}

private enum class SongSort(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Added("Recently added");

    fun next() = entries[(ordinal + 1) % entries.size]
}

@Composable
fun LibraryScreen(library: Library) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.Playlists) }
    var sort by rememberSaveable { mutableStateOf(SongSort.Title) }
    var creating by remember { mutableStateOf(false) }
    val player by app.player.state.collectAsStateWithLifecycle()
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val bottom = PaddingValues(bottom = LocalBottomPadding.current)

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
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileButton()
            Spacer(Modifier.width(12.dp))
            Text("Your Library", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Create playlist", modifier = Modifier.size(28.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryFilter.entries) { option ->
                PillChip(option.label, option == filter) { filter = option }
            }
        }

        // Each filter keeps its own list, so scrolling Downloaded doesn't move Albums.
        when (filter) {
            LibraryFilter.Playlists -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                item(key = "liked") {
                    ResultRow(
                        null,
                        "Liked Songs",
                        "Playlist, ${songCount(likedKeys.size)}",
                        cover = { LikedTile(Modifier.size(56.dp).clip(CoverShape)) },
                    ) { ui.openLiked() }
                }
                items(playlists, key = { "p${it.id}" }) { playlist ->
                    val tracks = playlist.trackKeys.mapNotNull { library.trackByKey[it] }
                    ResultRow(
                        null,
                        playlist.name,
                        "Playlist, ${songCount(tracks.size)}",
                        cover = { MosaicCover(mosaicIds(tracks), Modifier.size(56.dp)) },
                    ) { ui.openPlaylist(playlist.id) }
                }
                items(library.folders, key = { "f${it.name}" }) { folder ->
                    ResultRow(
                        null,
                        folder.name,
                        "Playlist, ${songCount(folder.tracks.size)}",
                        cover = { MosaicCover(mosaicIds(folder.tracks), Modifier.size(56.dp)) },
                    ) { ui.openFolder(folder.name) }
                }
            }
            LibraryFilter.Albums -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                items(library.albums, key = { it.key }) { album ->
                    ResultRow(album.tracks.first().mediaId, album.name, "Album, ${album.artist}") { ui.openAlbum(album.key) }
                }
            }
            LibraryFilter.Artists -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                items(library.artists, key = { it.key }) { artist ->
                    ResultRow(artist.tracks.first().mediaId, artist.name, "Artist", circle = true) { ui.openArtist(artist.key) }
                }
            }
            LibraryFilter.Downloaded -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                item(key = "songs-header") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .pressable { sort = sort.next() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${sort.label}, ${songCount(songs.size)}", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = { app.player.shufflePlay(songs, "All songs") }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle all songs")
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
            title = "Give your playlist a name",
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
