package app.attic.player.ui.classic

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.ui.*
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryItem
import app.attic.player.ui.model.LibraryState

@Composable
fun ClassicLibrary(state: LibraryState, actions: LibraryActions) {
    val filter = state.filter
    val sort = state.sort
    val grid = state.grid
    val songs = state.songs
    val items = state.items
    val bottom = LocalBottomPadding.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileButton(actions.openSettings)
            Spacer(Modifier.width(12.dp))
            Text("Your Library", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = actions.openSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search your library", modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = actions.startCreating) {
                Icon(Icons.Rounded.Add, contentDescription = "Create playlist", modifier = Modifier.size(28.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryFilter.entries) { option ->
                PillChip(option.label, option == filter) { actions.toggleFilter(option) }
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
                                .pressable { actions.cycleSort() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${sort.label}, ${songCount(songs.size)}", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = actions.shuffleAll) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle all songs")
                        }
                    }
                }
                items(songs, key = { it.key }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = state.currentKey == track.key,
                        isPlaying = state.isPlaying,
                        onClick = { actions.play(track) },
                        onMenu = { actions.showMenu(track) },
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
                IconButton(onClick = actions.toggleGrid) {
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
                    items(items, key = { it.id }) { item -> GridTile(item) { actions.open(item) } }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom)) {
                    items(items, key = { it.id }) { item ->
                        ResultRow(null, item.title, item.subtitle, cover = { ItemCover(item, Modifier.size(64.dp)) }, onClick = { actions.open(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCover(item: LibraryItem, modifier: Modifier) {
    when {
        item.liked -> LikedTile(modifier.clip(CoverShape))
        item.circle -> CoverArt(item.mediaIds.firstOrNull(), CircleShape, modifier)
        else -> MosaicCover(item.mediaIds, modifier)
    }
}

@Composable
private fun GridTile(item: LibraryItem, onClick: () -> Unit) {
    Column(Modifier.pressable(onClick = onClick), horizontalAlignment = if (item.circle) Alignment.CenterHorizontally else Alignment.Start) {
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
