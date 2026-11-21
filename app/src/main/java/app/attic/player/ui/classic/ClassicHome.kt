package app.attic.player.ui.classic

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.data.Album
import app.attic.player.data.Track
import app.attic.player.ui.*
import app.attic.player.ui.model.HomeActions
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.Shortcut
import app.attic.player.ui.model.WrappedTeaser

@Composable
fun ClassicHome(state: HomeState, actions: HomeActions) {
    val filter = state.filter
    val shortcuts = state.shortcuts
    val speedDial = state.speedDial
    val recentTracks = state.recentTracks
    val startListening = state.startListening
    val biggestAlbums = state.biggestAlbums
    val biggestArtists = state.biggestArtists
    val spotlight = state.spotlight

    if (state.isEmpty) {
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
                ProfileButton(actions.openSettings)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
                    items(HomeFilter.entries) { option ->
                        PillChip(option.label, option == filter) { actions.setFilter(option) }
                    }
                }
            }
        }

        if (filter == HomeFilter.All) {
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shortcuts.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ShortcutTile(it, actions, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            state.wrapped?.let { teaser ->
                item { WrappedEntry(teaser, actions.openWrapped) }
            }
            if (speedDial.isNotEmpty()) {
                item {
                    SectionTitle("Speed dial")
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        speedDial.chunked(SPEED_DIAL_COLUMNS).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { track ->
                                    SpeedDialTile(
                                        track,
                                        isCurrent = state.currentKey == track.key,
                                        modifier = Modifier.weight(1f),
                                        onClick = { actions.startRadio(track) },
                                        onLongClick = { actions.showMenu(track) },
                                    )
                                }
                                repeat(SPEED_DIAL_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                            }
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
                                highlighted = state.currentKey == track.key,
                                onClick = { actions.playRecent(track) },
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
                    isCurrent = state.currentKey == track.key,
                    isPlaying = state.isPlaying,
                    onClick = { actions.playStartListening(track) },
                    onMenu = { actions.showMenu(track) },
                )
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Folders) {
            item {
                SectionTitle("Your folders")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.folders, key = { it.name }) { folder ->
                        FolderCard(folder) { actions.openFolder(folder) }
                    }
                }
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Albums) {
            item {
                SectionTitle("Albums")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(biggestAlbums, key = { it.key }) { album ->
                        ShelfCard(album.tracks.first().mediaId, album.name, album.artist, { actions.openAlbum(album) })
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
                        .pressable { actions.openArtist(spotlight) },
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
                        ShelfCard(album.tracks.first().mediaId, album.name, album.year?.toString() ?: "Album", { actions.openAlbum(album) })
                    }
                }
            }
        }

        if (filter == HomeFilter.All || filter == HomeFilter.Artists) {
            item {
                SectionTitle("Your top artists")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(biggestArtists, key = { it.key }) { artist ->
                        ArtistCircle(artist) { actions.openArtist(artist) }
                    }
                }
            }
        }
    }
}

/** The way into Wrapped, open all year rather than only in December. */
@Composable
private fun WrappedEntry(teaser: WrappedTeaser, onOpen: () -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2A0E3D))
            .pressable(scale = 0.98f, onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MosaicCover(teaser.mosaicIds, Modifier.size(64.dp), RoundedCornerShape(4.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(teaser.headline, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFB627), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${java.text.NumberFormat.getIntegerInstance().format(teaser.minutes)} minutes. See your top songs and cards to share.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
            )
        }
    }
}
private const val SPEED_DIAL_COLUMNS = 3

/** A square cover with the title over it. Tapping starts radio from the song; holding opens its menu. */
@Composable
private fun SpeedDialTile(track: Track, isCurrent: Boolean, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(1f)
            .pressable(onLongClick = onLongClick, onClick = onClick)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        CoverArt(track.mediaId, RoundedCornerShape(0.dp), Modifier.fillMaxSize(), iconSize = 32.dp)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))),
        )
        Text(
            track.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
    }
}

@Composable
private fun ShortcutTile(shortcut: Shortcut, actions: HomeActions, modifier: Modifier = Modifier) {
    val title = when (shortcut) {
        Shortcut.Liked -> "Liked Songs"
        is Shortcut.ForAlbum -> shortcut.album.name
        is Shortcut.ForFolder -> shortcut.folder.name
    }
    val onClick = { actions.openShortcut(shortcut) }
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
