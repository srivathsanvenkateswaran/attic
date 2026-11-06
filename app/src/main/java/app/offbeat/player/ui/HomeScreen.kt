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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.offbeat.player.data.Album
import app.offbeat.player.data.Artist
import app.offbeat.player.data.FolderPlaylist
import app.offbeat.player.data.Library
import app.offbeat.player.data.Track
import app.offbeat.player.offbeat
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private sealed interface Shortcut {
    data object Liked : Shortcut
    data class ForAlbum(val album: Album) : Shortcut
    data class ForFolder(val folder: FolderPlaylist) : Shortcut
}

@Composable
fun HomeScreen(library: Library) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()

    val recentTracks = remember(recentKeys, library) {
        recentKeys.mapNotNull { library.trackByKey[it] }.take(12)
    }
    val shortcuts = remember(recentTracks, library) { buildShortcuts(recentTracks, library) }
    val biggestAlbums = remember(library) {
        library.albums.sortedByDescending { it.tracks.size }.take(12)
    }
    val biggestArtists = remember(library) {
        library.artists.sortedByDescending { it.tracks.size }.take(12)
    }

    if (library.tracks.isEmpty()) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            EmptyMessage(
                "No songs in Music/Offline",
                "Copy MP3s into Music/Offline on this phone and they will show up here.",
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OffbeatMark(26.dp)
                Spacer(Modifier.width(10.dp))
                Text(greeting(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { app.player.shufflePlay(library.tracks, "All songs") }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle all", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shortcuts.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { shortcut ->
                            ShortcutTile(shortcut, likedKeys.size, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SectionTitle("Your folders")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(library.folders, key = { it.name }) { folder ->
                    FolderCrate(folder) { ui.openFolder(folder.name) }
                }
            }
        }

        if (recentTracks.isNotEmpty()) {
            item {
                SectionTitle("Recently played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(recentTracks, key = { it.key }) { track ->
                        val index = recentTracks.indexOf(track)
                        ShelfCard(
                            mediaId = track.mediaId,
                            title = track.title,
                            subtitle = track.artist,
                            highlighted = player.current?.key == track.key,
                            onClick = { app.player.play(recentTracks, index, "Recently played") },
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("Albums")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(biggestAlbums, key = { it.key }) { album ->
                    ShelfCard(
                        mediaId = album.tracks.first().mediaId,
                        title = album.name,
                        subtitle = album.artist,
                        onClick = { ui.openAlbum(album.key) },
                    )
                }
            }
        }

        item {
            SectionTitle("Artists")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(biggestArtists, key = { it.key }) { artist ->
                    ArtistCircle(artist) { ui.openArtist(artist.key) }
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
private fun ShortcutTile(shortcut: Shortcut, likedCount: Int, modifier: Modifier = Modifier) {
    val ui = LocalUi.current
    val title: String
    val mediaId: Long?
    val onClick: () -> Unit
    when (shortcut) {
        Shortcut.Liked -> {
            title = "Liked songs"
            mediaId = null
            onClick = ui::openLiked
        }
        is Shortcut.ForAlbum -> {
            title = shortcut.album.name
            mediaId = shortcut.album.tracks.first().mediaId
            onClick = { ui.openAlbum(shortcut.album.key) }
        }
        is Shortcut.ForFolder -> {
            title = shortcut.folder.name
            mediaId = shortcut.folder.tracks.first().mediaId
            onClick = { ui.openFolder(shortcut.folder.name) }
        }
    }
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shortcut == Shortcut.Liked) {
            LikedTile(Modifier.size(56.dp))
        } else {
            CoverArt(mediaId, RoundedCornerShape(0.dp), Modifier.size(56.dp))
        }
        Column(Modifier.padding(horizontal = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (shortcut == Shortcut.Liked) {
                Text(
                    songCount(likedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LikedTile(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxSize(0.46f),
        )
    }
}

@Composable
fun FolderCrate(folder: FolderPlaylist, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Evenly spaced picks, so the three covers aren't all from the same corner of the alphabet.
        val step = (folder.tracks.size / 3).coerceAtLeast(1)
        FannedCovers(listOf(step * 2, step, 0).mapNotNull { folder.tracks.getOrNull(it)?.mediaId })
        Spacer(Modifier.height(6.dp))
        Text(folder.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        Text(
            songCount(folder.tracks.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
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
    Column(
        Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
    ) {
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistCircle(artist: Artist, onClick: () -> Unit) {
    Column(
        Modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(artist.tracks.first().mediaId, CircleShape, Modifier.size(112.dp), iconSize = 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
