package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest

/** What a query found, already cut to what is shown: up to five artists and ten albums. */
@Immutable
class SearchResults(
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
        artists = library.artists.matching({ it.name }).take(5),
        albums = library.albums.matching({ it.name }, { it.artist }).take(10),
        folders = library.folders.matching({ it.name }),
    )
}

/** A browse tile: every folder, then the eight biggest albums. */
@Immutable
class BrowseTile(val title: String, val cover: Long?, val destination: Destination)

@Immutable
class SearchState(
    val query: String,
    /** Null while the query is blank, when the browse tiles show instead. */
    val results: SearchResults?,
    val tiles: List<BrowseTile>,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class SearchActions(
    val setQuery: (String) -> Unit,
    val openSettings: () -> Unit,
    val open: (Destination) -> Unit,
    val play: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
)

@Composable
fun rememberSearchModel(library: Library): UiModel<SearchState, SearchActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, library) { query.takeIf { it.isNotBlank() }?.let { search(library, it) } }
    val tiles = remember(library) {
        library.folders.map { f -> BrowseTile(f.name, f.tracks.first().mediaId, Destination.Folder(f.name)) } +
            library.albums.sortedByDescending { it.tracks.size }.take(8).map { a ->
                BrowseTile(a.name, a.tracks.first().mediaId, Destination.Album(a.key))
            }
    }

    val state = SearchState(query, results, tiles, player.current?.key, player.isPlaying)
    val actions = SearchActions(
        setQuery = { query = it },
        openSettings = ui::openSettings,
        open = { ui.open(it) },
        play = { track ->
            val tracks = results?.tracks.orEmpty()
            app.player.play(tracks, tracks.indexOf(track), "Search results")
        },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
    )
    return UiModel(state, actions)
}
