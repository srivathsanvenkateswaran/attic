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
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.launchCreate
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.songCount

enum class LibraryFilter(val label: String) {
    Playlists("Playlists"),
    Albums("Albums"),
    Artists("Artists"),
    Downloaded("Downloaded"),
}

enum class SongSort(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Added("Recently added");

    fun next() = entries[(ordinal + 1) % entries.size]
}

/** What a library row is, so a style can group rows and word their subtitles itself. */
enum class LibraryKind { Favourites, Playlist, Folder, Artist, Album }

/**
 * One row or grid tile. [circle] marks artists, which are shown round. [title] and [subtitle] are
 * Classic's wording; [kind], [songCount] and [artist] (an album's) let other styles word their own.
 */
@Immutable
class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val circle: Boolean = false,
    val mediaIds: List<Long> = emptyList(),
    val liked: Boolean = false,
    val destination: Destination,
    val kind: LibraryKind,
    val songCount: Int = 0,
    val artist: String? = null,
)

@Immutable
class LibraryState(
    /** Null shows everything. Each chip toggles itself off. */
    val filter: LibraryFilter?,
    val sort: SongSort,
    val grid: Boolean,
    /** The create-playlist dialog is open. */
    val creating: Boolean,
    /** Every song in [sort] order, for the Downloaded filter. */
    val songs: List<Track>,
    /** In "Recents" order for the current filter. */
    val items: List<LibraryItem>,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class LibraryActions(
    val toggleFilter: (LibraryFilter) -> Unit,
    val cycleSort: () -> Unit,
    /** Picks a sort directly, as a menu of the three does. */
    val setSort: (SongSort) -> Unit,
    val toggleGrid: () -> Unit,
    val openSettings: () -> Unit,
    val openSearch: () -> Unit,
    val open: (LibraryItem) -> Unit,
    val startCreating: () -> Unit,
    val dismissCreating: () -> Unit,
    /** Creates the playlist and opens it. */
    val create: (String) -> Unit,
    val shuffleAll: () -> Unit,
    val play: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
)

@Composable
fun rememberLibraryModel(library: Library): UiModel<LibraryState, LibraryActions> {
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

    val songs = remember(library, sort) {
        when (sort) {
            SongSort.Title -> library.tracks
            SongSort.Artist -> library.tracks.sortedBy { it.artist.lowercase() }
            SongSort.Added -> library.tracks.sortedByDescending { it.dateAddedSec }
        }
    }
    val items = remember(library, filter, likedKeys.size, playlists, recentKeys) {
        val liked = LibraryItem(
            "liked", "Liked Songs", "Playlist, ${songCount(likedKeys.size)}", liked = true, destination = Destination.Liked,
            kind = LibraryKind.Favourites, songCount = likedKeys.size,
        )
        val lists = playlists.map { p ->
            val tracks = p.trackKeys.mapNotNull { library.trackByKey[it] }
            LibraryItem(
                "p${p.id}", p.name, "Playlist, ${songCount(tracks.size)}", mediaIds = mosaicIds(tracks), destination = Destination.Playlist(p.id),
                kind = LibraryKind.Playlist, songCount = tracks.size,
            )
        }
        val folders = library.folders.map { f ->
            LibraryItem(
                "f${f.name}", f.name, "Playlist, ${songCount(f.tracks.size)}", mediaIds = mosaicIds(f.tracks), destination = Destination.Folder(f.name),
                kind = LibraryKind.Folder, songCount = f.tracks.size,
            )
        }
        // "Recents" order: whatever you played lately first, then the rest alphabetically.
        val rank = recentKeys.withIndex().associate { it.value to it.index }
        fun recency(tracks: List<Track>) = tracks.minOfOrNull { rank[it.key] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        val albums = library.albums.sortedBy { recency(it.tracks) }.map { a ->
            LibraryItem(
                "a${a.key}", a.name, "Album, ${a.artist}", mediaIds = listOf(a.tracks.first().mediaId), destination = Destination.Album(a.key),
                kind = LibraryKind.Album, songCount = a.tracks.size, artist = a.artist,
            )
        }
        val artists = library.artists.sortedBy { recency(it.tracks) }.map { a ->
            LibraryItem(
                "r${a.key}", a.name, "Artist", circle = true, mediaIds = listOf(a.tracks.first().mediaId), destination = Destination.Artist(a.key),
                kind = LibraryKind.Artist, songCount = a.tracks.size,
            )
        }
        when (filter) {
            LibraryFilter.Playlists -> listOf(liked) + lists + folders
            LibraryFilter.Albums -> albums
            LibraryFilter.Artists -> artists
            LibraryFilter.Downloaded -> emptyList()
            null -> listOf(liked) + lists + folders + artists.take(TOP_ARTISTS) + albums
        }
    }

    val state = LibraryState(filter, sort, grid, creating, songs, items, player.current?.key, player.isPlaying)
    val actions = LibraryActions(
        toggleFilter = { option -> filter = if (filter == option) null else option },
        cycleSort = { sort = sort.next() },
        setSort = { sort = it },
        toggleGrid = { grid = !grid },
        openSettings = ui::openSettings,
        openSearch = { ui.openSearch() },
        open = { ui.open(it.destination) },
        startCreating = { creating = true },
        dismissCreating = { creating = false },
        create = { name ->
            creating = false
            app.scope.launchCreate(app, name) { id -> ui.openPlaylist(id) }
        },
        shuffleAll = { app.player.shufflePlay(songs, "All songs") },
        play = { track -> app.player.play(songs, songs.indexOf(track), "All songs") },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
    )
    return UiModel(state, actions)
}

private const val TOP_ARTISTS = 40
