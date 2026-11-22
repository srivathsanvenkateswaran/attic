package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import app.attic.player.data.Track
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.mosaicIds
import app.attic.player.wrapped.RecapPeriod

sealed interface Shortcut {
    data object Liked : Shortcut
    data class ForAlbum(val album: Album) : Shortcut
    data class ForFolder(val folder: FolderPlaylist) : Shortcut
}

enum class HomeFilter { All, Albums, Artists, Folders }

/**
 * The way into the recap, open all year rather than only in December. [year] and [yearEnded] (from
 * 24 December) let a style word it from its own copy.
 */
@Immutable
class WrappedTeaser(
    val minutes: Long,
    val mosaicIds: List<Long>,
    val year: Int,
    val yearEnded: Boolean,
)

@Immutable
class HomeState(
    val isEmpty: Boolean,
    val filter: HomeFilter,
    /** Liked songs, then whatever you played lately (by album or folder), topped up with the biggest albums. */
    val shortcuts: List<Shortcut>,
    /** Null until the year's recap has loaded, and while there is nothing to show. */
    val wrapped: WrappedTeaser?,
    val speedDial: List<Track>,
    val recentTracks: List<Track>,
    /** Picked at random from your songs. */
    val startListening: List<Track>,
    val folders: List<FolderPlaylist>,
    val biggestAlbums: List<Album>,
    val biggestArtists: List<Artist>,
    /** The biggest artist with at least two albums, shown as "More from". */
    val spotlight: Artist?,
    val currentKey: String?,
    val isPlaying: Boolean,
    /** Liked songs that are in the library, the count Favourites shows. */
    val favouriteCount: Int,
    /** The song the radio that is playing was started from, if radio is playing. */
    val radioSeedKey: String?,
)

class HomeActions(
    val setFilter: (HomeFilter) -> Unit,
    val openSettings: () -> Unit,
    val openShortcut: (Shortcut) -> Unit,
    val openWrapped: () -> Unit,
    val openFolder: (FolderPlaylist) -> Unit,
    val openAlbum: (Album) -> Unit,
    val openArtist: (Artist) -> Unit,
    /** An Instant mixes tile starts radio from its song. */
    val startRadio: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
    val playRecent: (Track) -> Unit,
    val playStartListening: (Track) -> Unit,
)

@Composable
fun rememberHomeModel(library: Library): UiModel<HomeState, HomeActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(HomeFilter.All) }
    val speedDial by app.radio.speedDial.collectAsStateWithLifecycle()
    val listenCount by app.wrapped.listenCount().collectAsStateWithLifecycle(0)
    val analyzed by app.radio.analyzedKeys().collectAsStateWithLifecycle(emptyList())
    // Refreshes as you play and as analysis reaches more songs, in steps so it isn't constantly redone.
    LaunchedEffect(library, recentKeys, analyzed.size / SPEED_DIAL_REFRESH_STEP) { app.radio.refreshSpeedDial() }

    val recentTracks = remember(recentKeys, library) {
        recentKeys.mapNotNull { library.trackByKey[it] }.take(12)
    }
    val shortcuts = remember(recentTracks, library) { buildShortcuts(recentTracks, library) }
    val biggestAlbums = remember(library) { library.albums.sortedByDescending { it.tracks.size }.take(14) }
    val biggestArtists = remember(library) { library.artists.sortedByDescending { it.tracks.size }.take(14) }
    val startListening = remember(library, recentKeys.isEmpty()) { library.tracks.shuffled().take(4) }
    val spotlight = remember(library) { biggestArtists.firstOrNull { it.albums.size >= 2 } }

    // Only loaded while the card would be on screen, as it was when the card loaded its own recap.
    val wrapped = if (library.tracks.isNotEmpty() && filter == HomeFilter.All && listenCount > 0) {
        rememberWrappedTeaser(library, recentKeys)
    } else {
        null
    }

    val state = HomeState(
        isEmpty = library.tracks.isEmpty(),
        filter = filter,
        shortcuts = shortcuts,
        wrapped = wrapped,
        speedDial = speedDial,
        recentTracks = recentTracks,
        startListening = startListening,
        folders = library.folders,
        biggestAlbums = biggestAlbums,
        biggestArtists = biggestArtists,
        spotlight = spotlight,
        currentKey = player.current?.key,
        isPlaying = player.isPlaying,
        favouriteCount = remember(library, liked) { library.tracks.count { it.key in liked } },
        radioSeedKey = app.radio.seedFor(player.contextName),
    )
    val actions = HomeActions(
        setFilter = { filter = it },
        openSettings = ui::openSettings,
        openShortcut = { shortcut ->
            when (shortcut) {
                Shortcut.Liked -> ui.openLiked()
                is Shortcut.ForAlbum -> ui.openAlbum(shortcut.album.key)
                is Shortcut.ForFolder -> ui.openFolder(shortcut.folder.name)
            }
        },
        openWrapped = ui::openWrapped,
        openFolder = { ui.openFolder(it.name) },
        openAlbum = { ui.openAlbum(it.key) },
        openArtist = { ui.openArtist(it.key) },
        startRadio = { app.radio.start(it) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        playRecent = { track -> app.player.play(recentTracks, recentTracks.indexOf(track), "Recently played") },
        playStartListening = { track -> app.player.play(startListening, startListening.indexOf(track), "Start listening") },
    )
    return UiModel(state, actions)
}

@Composable
private fun rememberWrappedTeaser(library: Library, recentKeys: List<String>): WrappedTeaser? {
    val app = LocalContext.current.attic
    val data by produceState<WrappedData?>(null, recentKeys, library) {
        value = wrappedData(app.wrapped.recap(RecapPeriod.Year), RecapPeriod.Year, library)
    }
    val loaded = data ?: return null
    if (loaded.recap.isEmpty) return null
    val today = java.time.LocalDate.now()
    return WrappedTeaser(
        minutes = loaded.recap.minutes,
        mosaicIds = loaded.songs.map { it.first }.let(::mosaicIds),
        year = today.year,
        yearEnded = today.monthValue == 12 && today.dayOfMonth >= 24,
    )
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
private const val SPEED_DIAL_REFRESH_STEP = 25
