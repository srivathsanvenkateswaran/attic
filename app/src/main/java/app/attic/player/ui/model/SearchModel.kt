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
import app.attic.player.data.Genre
import app.attic.player.data.Library
import app.attic.player.data.LibrarySearch
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.mosaicIds

/** What a query found, already cut to what is shown: up to five artists and ten albums. */
@Immutable
class SearchResults(
    val tracks: List<Track>,
    val artists: List<Artist>,
    val albums: List<Album>,
    val folders: List<FolderPlaylist>,
    val genres: List<Genre> = emptyList(),
) {
    val isEmpty get() = tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && folders.isEmpty() && genres.isEmpty()
}

/** Accent and case insensitive, with name-prefix matches ranked first. */
private fun search(library: Library, genres: List<Genre>, query: String): SearchResults {
    val found = LibrarySearch.find(library, query)
    return SearchResults(
        tracks = found.tracks,
        artists = found.artists.take(5),
        albums = found.albums.take(10),
        folders = found.folders,
        genres = with(LibrarySearch) { genres.matching(TitleCleaner.searchable(query), { it.name }) }.take(3),
    )
}

/**
 * A browse tile: every folder, then the eight biggest albums; or a genre, in [SearchState.genres].
 * [artist] is the album's artist (null for a folder or genre); [mosaicIds] are a folder's or genre's
 * covers from different albums.
 */
@Immutable
class BrowseTile(
    val title: String,
    val cover: Long?,
    val destination: Destination,
    val songCount: Int = 0,
    val artist: String? = null,
    val mosaicIds: List<Long> = emptyList(),
)

@Immutable
class SearchState(
    val query: String,
    /** Null while the query is blank, when the browse tiles show instead. */
    val results: SearchResults?,
    val tiles: List<BrowseTile>,
    val currentKey: String?,
    val isPlaying: Boolean,
    /** Browse's "Genres": one tile per genre, biggest first. Empty until there are genres to show. */
    val genres: List<BrowseTile> = emptyList(),
)

class SearchActions(
    val setQuery: (String) -> Unit,
    val openSettings: () -> Unit,
    val open: (Destination) -> Unit,
    val play: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberSearchModel(library: Library): UiModel<SearchState, SearchActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val genres by app.genres.genres.collectAsStateWithLifecycle()
    val genreList = genres.orEmpty()
    val results = remember(query, library, genreList) { query.takeIf { it.isNotBlank() }?.let { search(library, genreList, it) } }
    val tiles = remember(library) {
        library.folders.map { f ->
            BrowseTile(f.name, f.tracks.first().mediaId, Destination.Folder(f.name), f.tracks.size, mosaicIds = mosaicIds(f.tracks))
        } +
            library.albums.sortedByDescending { it.tracks.size }.take(8).map { a ->
                BrowseTile(a.name, a.tracks.first().mediaId, Destination.Album(a.key), a.tracks.size, a.artist)
            }
    }

    val genreTiles = remember(genreList) {
        genreList.map { g ->
            BrowseTile(g.name, g.tracks.first().mediaId, Destination.Genre(g.key), g.tracks.size, mosaicIds = mosaicIds(g.tracks))
        }
    }

    val state = SearchState(query, results, tiles, player.current?.key, player.isPlaying, genreTiles)
    val actions = SearchActions(
        setQuery = { query = it },
        openSettings = ui::openSettings,
        open = { ui.open(it) },
        play = { track ->
            val tracks = results?.tracks.orEmpty()
            app.player.play(tracks, tracks.indexOf(track), "Search results")
        },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}
