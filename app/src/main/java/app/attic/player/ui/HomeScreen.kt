package app.attic.player.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.Track

private sealed interface Shortcut {
    data object Liked : Shortcut
    data class ForAlbum(val album: Album) : Shortcut
    data class ForFolder(val folder: FolderPlaylist) : Shortcut
}

private enum class HomeFilter(val label: String) {
    All("All"),
    Albums("Albums"),
    Artists("Artists"),
    Folders("Folders"),
}

@Composable
fun HomeScreen(library: Library) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    val player by app.player.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(HomeFilter.All) }

    val recentTracks = remember(recentKeys, library) {
        recentKeys.mapNotNull { library.trackByKey[it] }.take(12)
    }
    val shortcuts = remember(recentTracks, library) { buildShortcuts(recentTracks, library) }
    val biggestAlbums = remember(library) { library.albums.sortedByDescending { it.tracks.size }.take(14) }
    val biggestArtists = remember(library) { library.artists.sortedByDescending { it.tracks.size }.take(14) }
    val startListening = remember(library, recentKeys.isEmpty()) { library.tracks.shuffled().take(4) }
    val spotlight = remember(library) { biggestArtists.firstOrNull { it.albums.size >= 2 } }

    if (library.tracks.isEmpty()) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            EmptyMessage(
                "No songs in Music/Offline",
                "Copy MP3s into Music/Offline on this phone and they will show up here.",
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomPadding.current)) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileButton()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
                    items(HomeFilter.entries) { option ->
                        PillChip(option.label, option == filter) { filter = option }
                    }
                }
            }
        }

        if (filter == HomeFilter.All) {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shortcuts.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ShortcutTile(it, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (recentTracks.isNotEmpty()) {
                item {
                    SectionTitle("Recently played")
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recentTracks, key = { it.key }) { track ->
                            ShelfCard(
                                mediaId = track.mediaId,
                                title = track.title,
                                subtitle = track.artist,
                                highlighted = player.current?.key == track.key,
                                onClick = { app.player.play(recentTracks, recentTracks.indexOf(track), "Recently played") },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Picked at random from your songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp),
                )
                Text(
                    "Start listening",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                )
            }
            items(startListening, key = { "start:${it.key}" }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = player.current?.key == track.key,
                    isPlaying = player.isPlaying,
                    onClick = { app.player.play(startListening, startListening.indexOf(track), "Start listening") },
                    onMenu = { ui.showMenu(TrackMenuRequest(track)) },
                )
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Folders) {
            item {
                SectionTitle("Your folders")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(library.folders, key = { it.name }) { folder ->
                        FolderCard(folder) { ui.openFolder(folder.name) }
                    }
                }
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Albums) {
            item {
                SectionTitle("Albums")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(biggestAlbums, key = { it.key }) { album ->
                        ShelfCard(album.tracks.first().mediaId, album.name, album.artist, { ui.openAlbum(album.key) })
                    }
                }
            }
        }

        if (filter == HomeFilter.All && spotlight != null) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp)
                        .pressable { ui.openArtist(spotlight.key) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(spotlight.tracks.first().mediaId, CircleShape, Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("More from", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(spotlight.name, style = MaterialTheme.typography.titleLarge)
                    }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(spotlight.albums, key = { it.key }) { album ->
                        ShelfCard(album.tracks.first().mediaId, album.name, album.year?.toString() ?: "Album", { ui.openAlbum(album.key) })
                    }
                }
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Artists) {
            item {
                SectionTitle("Your top artists")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(biggestArtists, key = { it.key }) { artist ->
                        ArtistCircle(artist) { ui.openArtist(artist.key) }
                    }
                }
            }
        }
    }
}

/** Liked songs, then whatever you played lately (by album or folder), topped up with the biggest albums. */
private fun buildShortcuts(recent: List<Track>, library: Library): List<Shortcut> {
    val result = LinkedHashSet<Shortcut>()
    result += Shortcut.Liked
    for (track in recent) {
        if (result.size >= SHORTCUT_COUNT) break
        val album = track.albumKey?.let { library.albumByKey[it] }
        val folder = library.folders.firstOrNull { it.name == track.folder }
        when {
            album != null && album.tracks.size > 1 -> result += Shortcut.ForAlbum(album)
            folder != null -> result += Shortcut.ForFolder(folder)
            album != null -> result += Shortcut.ForAlbum(album)
        }
    }
    library.albums.sortedByDescending { it.tracks.size }.forEach { album ->
        if (result.size < SHORTCUT_COUNT) result += Shortcut.ForAlbum(album)
    }
    return result.toList()
}

private const val SHORTCUT_COUNT = 6

@Composable
private fun ShortcutTile(shortcut: Shortcut, modifier: Modifier = Modifier) {
    val ui = LocalUi.current
    val title: String
    val onClick: () -> Unit
    when (shortcut) {
        Shortcut.Liked -> {
            title = "Liked Songs"
            onClick = ui::openLiked
        }
        is Shortcut.ForAlbum -> {
            title = shortcut.album.name
            onClick = { ui.openAlbum(shortcut.album.key) }
        }
        is Shortcut.ForFolder -> {
            title = shortcut.folder.name
            onClick = { ui.openFolder(shortcut.folder.name) }
        }
    }
    Row(
        modifier
            .height(56.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (shortcut) {
            Shortcut.Liked -> LikedTile(Modifier.size(56.dp))
            is Shortcut.ForAlbum -> CoverArt(shortcut.album.tracks.first().mediaId, RoundedCornerShape(0.dp), Modifier.size(56.dp))
            is Shortcut.ForFolder -> MosaicCover(mosaicIds(shortcut.folder.tracks), Modifier.size(56.dp), RoundedCornerShape(0.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
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
    Column(Modifier.width(150.dp).pressable(onClick = onClick)) {
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
    Column(Modifier.width(150.dp).pressable(onClick = onClick)) {
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
fun ProfileButton() {
    val ui = LocalUi.current
    Box(
        Modifier
            .size(36.dp)
            .pressable(scale = 0.9f) { ui.openSettings() }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
