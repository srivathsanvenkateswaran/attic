package app.attic.player.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track

private class SearchResults(
    val tracks: List<Track>,
    val artists: List<Artist>,
    val albums: List<Album>,
    val folders: List<FolderPlaylist>,
) {
    val isEmpty get() = tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && folders.isEmpty()
}

/** Accent and case insensitive, with name-prefix matches ranked first. */
private fun search(library: Library, query: String): SearchResults {
    val q = TitleCleaner.searchable(query)
    fun <T> List<T>.matching(name: (T) -> String, also: (T) -> String = { "" }): List<T> =
        mapNotNull { item ->
            val n = TitleCleaner.searchable(name(item))
            when {
                n.startsWith(q) -> item to 0
                n.contains(q) -> item to 1
                TitleCleaner.searchable(also(item)).contains(q) -> item to 2
                else -> null
            }
        }.sortedBy { it.second }.map { it.first }

    return SearchResults(
        tracks = library.tracks.matching({ it.title }, { "${it.artist} ${it.album.orEmpty()}" }),
        artists = library.artists.matching({ it.name }),
        albums = library.albums.matching({ it.name }, { it.artist }),
        folders = library.folders.matching({ it.name }),
    )
}

private val TileColors = listOf(
    Color(0xFF8D67AB), Color(0xFFE13300), Color(0xFF1E3264), Color(0xFF148A08),
    Color(0xFFBA5D07), Color(0xFF503750), Color(0xFF0D73EC), Color(0xFFAF2896),
)

private class BrowseTile(val title: String, val cover: Long?, val onClick: () -> Unit)

@Composable
fun SearchScreen(library: Library) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, library) { query.takeIf { it.isNotBlank() }?.let { search(library, it) } }
    val tiles = remember(library) {
        library.folders.map { f -> BrowseTile(f.name, f.tracks.first().mediaId) { ui.openFolder(f.name) } } +
            library.albums.sortedByDescending { it.tracks.size }.take(8).map { a ->
                BrowseTile(a.name, a.tracks.first().mediaId) { ui.openAlbum(a.key) }
            }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileButton()
            Spacer(Modifier.width(12.dp))
            Text("Search", style = MaterialTheme.typography.headlineSmall)
        }
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("What do you want to listen to?", maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
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
                items(tiles.size) { index -> BrowseCard(tiles[index], TileColors[index % TileColors.size]) }
            }
            results.isEmpty -> EmptyMessage("No results for \"$query\"", "Check the spelling, or try fewer words.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomPadding.current),
            ) {
                items(results.artists.take(5), key = { "artist:${it.key}" }) { artist ->
                    ResultRow(artist.tracks.first().mediaId, artist.name, "Artist", circle = true) { ui.openArtist(artist.key) }
                }
                items(results.folders, key = { "folder:${it.name}" }) { folder ->
                    ResultRow(folder.tracks.first().mediaId, folder.name, "Playlist") { ui.openFolder(folder.name) }
                }
                items(results.tracks, key = { "track:${it.key}" }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.current?.key == track.key,
                        isPlaying = player.isPlaying,
                        onClick = { app.player.play(results.tracks, results.tracks.indexOf(track), "Search results") },
                        onMenu = { ui.showMenu(TrackMenuRequest(track)) },
                    )
                }
                items(results.albums.take(10), key = { "album:${it.key}" }) { album ->
                    ResultRow(album.tracks.first().mediaId, album.name, "Album, ${album.artist}") { ui.openAlbum(album.key) }
                }
            }
        }
    }
}

@Composable
fun searchFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    unfocusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    focusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    focusedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    focusedTrailingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

/** Coloured browse tile with its cover tucked into the corner at an angle. */
@Composable
private fun BrowseCard(tile: BrowseTile, color: Color) {
    Box(
        Modifier
            .height(100.dp)
            .fillMaxWidth()
            .pressable(onClick = tile.onClick)
            .clip(RoundedCornerShape(6.dp))
            .background(color),
    ) {
        Text(
            tile.title,
            style = MaterialTheme.typography.titleMedium,
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

@Composable
fun ResultRow(
    mediaId: Long?,
    title: String,
    subtitle: String,
    circle: Boolean = false,
    cover: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) cover() else CoverArt(mediaId, if (circle) CircleShape else CoverShape, Modifier.size(56.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
