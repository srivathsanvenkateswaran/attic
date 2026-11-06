package app.attic.player.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewList
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
import app.attic.player.attic
import app.attic.player.data.Library
import app.attic.player.data.Track

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

/** One row or grid tile. [circle] marks artists, which are shown round. */
private class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val circle: Boolean = false,
    val mediaIds: List<Long> = emptyList(),
    val liked: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun LibraryScreen(library: Library) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    var filter by rememberSaveable { mutableStateOf<LibraryFilter?>(null) }
    var sort by rememberSaveable { mutableStateOf(SongSort.Title) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val player by app.player.state.collectAsStateWithLifecycle()
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    val bottom = LocalBottomPadding.current

    val songs = remember(library, sort) {
        when (sort) {
            SongSort.Title -> library.tracks
            SongSort.Artist -> library.tracks.sortedBy { it.artist.lowercase() }
            SongSort.Added -> library.tracks.sortedByDescending { it.dateAddedSec }
        }
    }
    val items = remember(library, filter, likedKeys.size, playlists, recentKeys) {
        val liked = LibraryItem("liked", "Liked Songs", "Playlist, ${songCount(likedKeys.size)}", liked = true) { ui.openLiked() }
        val lists = playlists.map { p ->
            val tracks = p.trackKeys.mapNotNull { library.trackByKey[it] }
            LibraryItem("p${p.id}", p.name, "Playlist, ${songCount(tracks.size)}", mediaIds = mosaicIds(tracks)) { ui.openPlaylist(p.id) }
        }
        val folders = library.folders.map { f ->
            LibraryItem("f${f.name}", f.name, "Playlist, ${songCount(f.tracks.size)}", mediaIds = mosaicIds(f.tracks)) { ui.openFolder(f.name) }
        }
        // "Recents" order: whatever you played lately first, then the rest alphabetically.
        val rank = recentKeys.withIndex().associate { it.value to it.index }
        fun recency(tracks: List<Track>) = tracks.minOfOrNull { rank[it.key] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        val albums = library.albums.sortedBy { recency(it.tracks) }.map { a ->
            LibraryItem("a${a.key}", a.name, "Album, ${a.artist}", mediaIds = listOf(a.tracks.first().mediaId)) { ui.openAlbum(a.key) }
        }
        val artists = library.artists.sortedBy { recency(it.tracks) }.map { a ->
            LibraryItem("r${a.key}", a.name, "Artist", circle = true, mediaIds = listOf(a.tracks.first().mediaId)) { ui.openArtist(a.key) }
        }
        when (filter) {
            LibraryFilter.Playlists -> listOf(liked) + lists + folders
            LibraryFilter.Albums -> albums
            LibraryFilter.Artists -> artists
            LibraryFilter.Downloaded -> emptyList()
            null -> listOf(liked) + lists + folders + artists.take(TOP_ARTISTS) + albums
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
            IconButton(onClick = { ui.openSearch() }) {
                Icon(Icons.Rounded.Search, contentDescription = "Search your library", modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Create playlist", modifier = Modifier.size(28.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryFilter.entries) { option ->
                PillChip(option.label, option == filter) { filter = if (filter == option) null else option }
            }
        }

        if (filter == LibraryFilter.Downloaded) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom)) {
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
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recents", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { grid = !grid }) {
                    Icon(
                        if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = if (grid) "Show as list" else "Show as grid",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items, key = { it.id }) { item -> GridTile(item) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom)) {
                    items(items, key = { it.id }) { item ->
                        ResultRow(null, item.title, item.subtitle, cover = { ItemCover(item, Modifier.size(64.dp)) }, onClick = item.onClick)
                    }
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

private const val TOP_ARTISTS = 40

@Composable
private fun ItemCover(item: LibraryItem, modifier: Modifier) {
    when {
        item.liked -> LikedTile(modifier.clip(CoverShape))
        item.circle -> CoverArt(item.mediaIds.firstOrNull(), CircleShape, modifier)
        else -> MosaicCover(item.mediaIds, modifier)
    }
}

@Composable
private fun GridTile(item: LibraryItem) {
    Column(Modifier.pressable(onClick = item.onClick), horizontalAlignment = if (item.circle) Alignment.CenterHorizontally else Alignment.Start) {
        ItemCover(item, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
