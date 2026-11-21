package app.attic.player.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.ui.*
import app.attic.player.ui.model.BrowseTile
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.SearchActions
import app.attic.player.ui.model.SearchState

@Composable
fun ClassicSearch(state: SearchState, actions: SearchActions) {
    val query = state.query
    val results = state.results
    val tiles = state.tiles

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileButton(actions.openSettings)
            Spacer(Modifier.width(12.dp))
            Text("Search", style = MaterialTheme.typography.headlineSmall)
        }
        TextField(
            value = query,
            onValueChange = actions.setQuery,
            placeholder = { Text("What do you want to listen to?", maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { actions.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = searchFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when {
            results == null -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = LocalBottomPadding.current),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Start browsing",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
                    )
                }
                items(tiles.size) { index -> BrowseCard(tiles[index], TileColors[index % TileColors.size]) { actions.open(tiles[index].destination) } }
            }
            results.isEmpty -> EmptyMessage("No results for \"$query\"", "Check the spelling, or try fewer words.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomPadding.current),
            ) {
                items(results.artists, key = { "artist:${it.key}" }) { artist ->
                    ResultRow(artist.tracks.first().mediaId, artist.name, "Artist", circle = true) { actions.open(Destination.Artist(artist.key)) }
                }
                items(results.folders, key = { "folder:${it.name}" }) { folder ->
                    ResultRow(folder.tracks.first().mediaId, folder.name, "Playlist") { actions.open(Destination.Folder(folder.name)) }
                }
                items(results.tracks, key = { "track:${it.key}" }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = state.currentKey == track.key,
                        isPlaying = state.isPlaying,
                        onClick = { actions.play(track) },
                        onMenu = { actions.showMenu(track) },
                    )
                }
                items(results.albums, key = { "album:${it.key}" }) { album ->
                    ResultRow(album.tracks.first().mediaId, album.name, "Album, ${album.artist}") { actions.open(Destination.Album(album.key)) }
                }
            }
        }
    }
}

/** Coloured browse tile with its cover tucked into the corner at an angle. */
private val TileColors = listOf(
    Color(0xFF8D67AB), Color(0xFFE13300), Color(0xFF1E3264), Color(0xFF148A08),
    Color(0xFFBA5D07), Color(0xFF503750), Color(0xFF0D73EC), Color(0xFFAF2896),
)

/** Coloured browse tile with its cover tucked into the corner at an angle. */
@Composable
private fun BrowseCard(tile: BrowseTile, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .height(92.dp)
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(6.dp))
            .background(color),
    ) {
        Text(
            tile.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 44.dp),
        )
        CoverArt(
            tile.cover,
            CoverShape,
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 8.dp)
                .rotate(25f)
                .size(68.dp),
        )
    }
}
