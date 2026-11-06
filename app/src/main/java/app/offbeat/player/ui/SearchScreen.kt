package app.offbeat.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.offbeat.player.data.Album
import app.offbeat.player.data.Artist
import app.offbeat.player.data.FolderPlaylist
import app.offbeat.player.data.Library
import app.offbeat.player.data.TitleCleaner
import app.offbeat.player.data.Track
import app.offbeat.player.offbeat

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

@Composable
fun SearchScreen(library: Library) {
    val app = LocalContext.current.offbeat
    val ui = LocalUi.current
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, library) { query.takeIf { it.isNotBlank() }?.let { search(library, it) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when {
            results == null -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            ) {
                items(library.folders, key = { it.name }) { folder ->
                    FolderCrate(folder) { ui.openFolder(folder.name) }
                }
            }
            results.isEmpty -> EmptyMessage("No matches", "Nothing in your library matches \"$query\".")
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                if (results.artists.isNotEmpty()) {
                    item { SectionTitle("Artists") }
                    items(results.artists.take(5), key = { "artist:${it.key}" }) { artist ->
                        ResultRow(artist.tracks.first().mediaId, artist.name, songCount(artist.tracks.size), circle = true) {
                            ui.openArtist(artist.key)
                        }
                    }
                }
                if (results.folders.isNotEmpty()) {
                    item { SectionTitle("Folders") }
                    items(results.folders, key = { "folder:${it.name}" }) { folder ->
                        ResultRow(folder.tracks.first().mediaId, folder.name, songCount(folder.tracks.size)) {
                            ui.openFolder(folder.name)
                        }
                    }
                }
                if (results.tracks.isNotEmpty()) {
                    item { SectionTitle("Songs") }
                    items(results.tracks, key = { "track:${it.key}" }) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = player.current?.key == track.key,
                            isPlaying = player.isPlaying,
                            onClick = {
                                app.player.play(results.tracks, results.tracks.indexOf(track), "Search results")
                            },
                            onMenu = { ui.showMenu(TrackMenuRequest(track)) },
                        )
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item { SectionTitle("Albums") }
                    items(results.albums.take(10), key = { "album:${it.key}" }) { album ->
                        ResultRow(album.tracks.first().mediaId, album.name, album.artist) {
                            ui.openAlbum(album.key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(mediaId: Long?, title: String, subtitle: String, circle: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(mediaId, if (circle) CircleShape else CoverShape, Modifier.size(48.dp))
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
