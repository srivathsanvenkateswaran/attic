package app.attic.player.ui.model

import app.attic.player.ui.Haptics
import app.attic.player.ui.rememberHaptics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Genre
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.UserPlaylist
import app.attic.player.playback.PlayerUiState
import app.attic.player.playback.SmartShuffle
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.UiActions
import kotlinx.coroutines.launch

/** The row under every collection header: add all to a playlist, shuffle, and play or pause. */
@Immutable
class PlayControlsState(
    /** The collection's name, which is also the "Playing from" name its songs play under. */
    val contextName: String,
    /** False for an empty collection. */
    val enabled: Boolean,
    /** Shuffle is on and this collection is what's playing. */
    val shuffleOn: Boolean,
    /** This collection is what's playing, and it is playing. */
    val isPlaying: Boolean,
    /**
     * Smart shuffle is on for this collection. Null where it isn't offered: only playlists and
     * Favourites have it, behind their more button.
     */
    val smartShuffle: Boolean? = null,
)

class PlayControlsActions(
    val addToPlaylist: () -> Unit,
    val shuffle: () -> Unit,
    /** Pauses or resumes this collection, or starts it from the top if something else is playing. */
    val playOrPause: () -> Unit,
    /**
     * Turns smart shuffle on (asking radio for songs that fit, about one in three) or off (taking them
     * out again). Turned on while this collection plays, the song playing carries on.
     */
    val toggleSmartShuffle: () -> Unit = {},
)

private fun playControls(
    app: AtticApp,
    ui: UiActions,
    haptics: Haptics,
    player: PlayerUiState,
    tracks: List<Track>,
    contextName: String,
    offersSmartShuffle: Boolean = false,
): UiModel<PlayControlsState, PlayControlsActions> {
    val current = player.current?.key
    // A song smart shuffle added still counts as this collection playing.
    val isThisContext = isSameContext(player.contextName, contextName) &&
        (current in tracks.map { it.key } || current in player.smartPicks)
    val smartOn = isThisContext && player.smartPicks.isNotEmpty()
    val state = PlayControlsState(
        contextName = contextName,
        enabled = tracks.isNotEmpty(),
        shuffleOn = player.shuffle && isThisContext,
        isPlaying = isThisContext && player.isPlaying,
        smartShuffle = if (offersSmartShuffle) smartOn else null,
    )
    val actions = PlayControlsActions(
        addToPlaylist = { ui.addToPlaylist(tracks.map { it.key }) },
        shuffle = { app.player.shufflePlay(tracks, contextName) },
        playOrPause = {
            haptics.toggle(!(isThisContext && player.isPlaying))
            if (isThisContext) app.player.togglePlayPause() else app.player.play(tracks, 0, contextName, shuffled = false)
        },
        toggleSmartShuffle = {
            if (smartOn) {
                app.player.stopSmartShuffle()
            } else if (tracks.isNotEmpty()) {
                app.scope.launch { startSmartShuffle(app, tracks, contextName, keepCurrent = isThisContext) }
            }
        },
    )
    return UiModel(state, actions)
}

/** Radio chooses the songs off the main thread; with none to add it says so and shuffles plainly. */
private suspend fun startSmartShuffle(app: AtticApp, tracks: List<Track>, contextName: String, keepCurrent: Boolean) {
    val picks = app.radio.smartPicks(tracks.map { it.key }, SmartShuffle.pickCount(tracks.size))
    if (picks.isNotEmpty()) {
        app.player.smartShuffle(tracks, picks, contextName, keepCurrent)
        return
    }
    app.messages.tryEmit(app.getString(R.string.message_smart_shuffle_nothing))
    if (keepCurrent) app.player.setShuffle(true) else app.player.shufflePlay(tracks, contextName)
}

// Album

@Immutable
class AlbumState(
    /** Null when the album has gone from the library. */
    val album: Album?,
    /** The picture beside the artist's name: the artist's first song, or this album when the artist isn't known. */
    val artistMediaId: Long?,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class AlbumActions(
    val back: () -> Unit,
    /** Null when the album's artist isn't in the library. */
    val openArtist: (() -> Unit)?,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberAlbumModel(library: Library, key: String): UiModel<AlbumState, AlbumActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val album = library.albumByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = album?.tracks.orEmpty()
    val controls = playControls(app, ui, rememberHaptics(), player, tracks, album?.name.orEmpty())
    val mediaId = album?.tracks?.first()?.mediaId
    val state = AlbumState(
        album = album,
        artistMediaId = album?.artistKey?.let { library.artistByKey[it]?.tracks?.first()?.mediaId } ?: mediaId,
        controls = controls.state,
        currentKey = player.current?.key,
        isPlaying = player.isPlaying,
    )
    val actions = AlbumActions(
        back = { ui.back() },
        openArtist = album?.artistKey?.let { k -> { ui.openArtist(k) } },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, album?.name.orEmpty()) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

// Artist

@Immutable
class ArtistState(
    /** Null when the artist has gone from the library. */
    val artist: Artist?,
    /** "Top songs": with no outside play counts, what you've played most recently. */
    val popular: List<Track>,
    /** The artist's other songs, listed under "All songs". */
    val others: List<Track>,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class ArtistActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    /** Plays from [index] in [ArtistState.popular] followed by [ArtistState.others]. */
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    val openAlbum: (Album) -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberArtistModel(library: Library, key: String): UiModel<ArtistState, ArtistActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val artist = library.artistByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    val recentKeys by app.userData.recentKeys.collectAsStateWithLifecycle(emptyList())
    val ranked = remember(artist, recentKeys) {
        val rank = recentKeys.withIndex().associate { it.value to it.index }
        artist?.tracks.orEmpty().sortedBy { rank[it.key] ?: Int.MAX_VALUE }
    }
    val controls = playControls(app, ui, rememberHaptics(), player, artist?.tracks.orEmpty(), artist?.name.orEmpty())
    val state = ArtistState(
        artist = artist,
        popular = ranked.take(POPULAR_COUNT),
        others = ranked.drop(POPULAR_COUNT),
        controls = controls.state,
        currentKey = player.current?.key,
        isPlaying = player.isPlaying,
    )
    val actions = ArtistActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(ranked, index, artist?.name.orEmpty()) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        openAlbum = { ui.openAlbum(it.key) },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

private const val POPULAR_COUNT = 5

// Folder

@Immutable
class FolderState(
    /** Null when the folder has gone from the phone. */
    val folder: FolderPlaylist?,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class FolderActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberFolderModel(library: Library, name: String): UiModel<FolderState, FolderActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val folder = library.folders.firstOrNull { it.name == name }
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = folder?.tracks.orEmpty()
    val controls = playControls(app, ui, rememberHaptics(), player, tracks, folder?.name.orEmpty())
    val state = FolderState(folder, controls.state, player.current?.key, player.isPlaying)
    val actions = FolderActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, folder?.name.orEmpty()) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

// Favourites

@Immutable
class LikedState(
    /** Newest first. */
    val tracks: List<Track>,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
    /** The more button's menu, which holds smart shuffle. */
    val overflowOpen: Boolean = false,
)

class LikedActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    val openOverflow: () -> Unit = {},
    val dismissOverflow: () -> Unit = {},
    /** Closes the menu and turns smart shuffle on or off. */
    val toggleSmartShuffle: () -> Unit = {},
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberLikedModel(library: Library): UiModel<LikedState, LikedActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = remember(likedKeys, library) { likedKeys.mapNotNull { library.trackByKey[it] } }
    var overflow by remember { mutableStateOf(false) }
    val controls = playControls(app, ui, rememberHaptics(), player, tracks, FAVOURITES_CONTEXT, offersSmartShuffle = true)
    val state = LikedState(tracks, controls.state, player.current?.key, player.isPlaying, overflow)
    val actions = LikedActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, FAVOURITES_CONTEXT) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        openOverflow = { overflow = true },
        dismissOverflow = { overflow = false },
        toggleSmartShuffle = {
            overflow = false
            controls.actions.toggleSmartShuffle()
        },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

// Playlist

@Immutable
class PlaylistState(
    /** Null when the playlist has been deleted. */
    val playlist: UserPlaylist?,
    val tracks: List<Track>,
    /** Reorder mode: rows show drag handles and taps don't play. */
    val reordering: Boolean,
    val overflowOpen: Boolean,
    val renaming: Boolean,
    val confirmingDelete: Boolean,
    /** The Add songs sheet is open. */
    val adding: Boolean,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class PlaylistActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val openOverflow: () -> Unit,
    val dismissOverflow: () -> Unit,
    val startRenaming: () -> Unit,
    val dismissRenaming: () -> Unit,
    val rename: (String) -> Unit,
    val startDeleting: () -> Unit,
    val dismissDeleting: () -> Unit,
    /** Leaves the screen, then deletes the playlist. */
    val delete: () -> Unit,
    val startAdding: () -> Unit,
    val dismissAdding: () -> Unit,
    /** Closes the menu and turns smart shuffle on or off. */
    val toggleSmartShuffle: () -> Unit,
    val toggleReordering: () -> Unit,
    /** Saves the order rows were dragged into. */
    val reorder: (keys: List<String>) -> Unit,
    /** Does nothing in reorder mode. */
    val play: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
    /** A swipe on a song row adds it to the queue. Not offered in reorder mode. */
    val queue: (Track) -> Unit = {},
)

@Composable
fun rememberPlaylistModel(library: Library, id: Long): UiModel<PlaylistState, PlaylistActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == id }
    val trackKeys = playlist?.trackKeys.orEmpty()

    val tracks = remember(trackKeys, library) { trackKeys.mapNotNull { library.trackByKey[it] } }
    var reordering by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    val name = playlist?.name.orEmpty()
    val controls = playControls(app, ui, rememberHaptics(), player, tracks, name, offersSmartShuffle = true)
    val state = PlaylistState(
        playlist = playlist,
        tracks = tracks,
        reordering = reordering,
        overflowOpen = overflow,
        renaming = renaming,
        confirmingDelete = confirmingDelete,
        adding = adding,
        controls = controls.state,
        currentKey = player.current?.key,
        isPlaying = player.isPlaying,
    )
    val actions = PlaylistActions(
        back = { ui.back() },
        controls = controls.actions,
        openOverflow = { overflow = true },
        dismissOverflow = { overflow = false },
        startRenaming = {
            overflow = false
            renaming = true
        },
        dismissRenaming = { renaming = false },
        rename = { newName ->
            renaming = false
            app.userData.renamePlaylist(id, newName)
        },
        startDeleting = {
            overflow = false
            confirmingDelete = true
        },
        dismissDeleting = { confirmingDelete = false },
        delete = {
            confirmingDelete = false
            ui.back()
            app.userData.deletePlaylist(id)
        },
        startAdding = { adding = true },
        dismissAdding = { adding = false },
        toggleSmartShuffle = {
            overflow = false
            controls.actions.toggleSmartShuffle()
        },
        toggleReordering = { reordering = !reordering },
        reorder = { keys -> app.userData.reorderPlaylist(id, keys) },
        play = { track ->
            if (!reordering) app.player.play(tracks, tracks.indexOf(track), name)
        },
        showMenu = { ui.showMenu(TrackMenuRequest(it, playlistId = id)) },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}

// Genre

@Immutable
class GenreState(
    /** Null while the list is still being built ([loading]) or when the genre has gone. */
    val genre: Genre?,
    val loading: Boolean,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class GenreActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    val openAlbum: (Album) -> Unit,
    /** Radio from the middle of the genre's sound. */
    val startRadio: () -> Unit,
    /** A swipe on a song row adds it to the queue. */
    val queue: (Track) -> Unit,
)

/** A genre's page. The list of genres is built off the main thread by [app.attic.player.data.GenreRepository]. */
@Composable
fun rememberGenreModel(key: String): UiModel<GenreState, GenreActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val genres by app.genres.genres.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val genre = genres?.firstOrNull { it.key == key }
    val tracks = genre?.tracks.orEmpty()
    val name = genre?.name.orEmpty()
    val controls = playControls(app, ui, rememberHaptics(), player, tracks, name)
    val state = GenreState(genre, genres == null, controls.state, player.current?.key, player.isPlaying)
    val actions = GenreActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, name) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        openAlbum = { ui.openAlbum(it.key) },
        startRadio = { if (genre != null) app.scope.launch { app.radio.startFromGenre(genre.name, genre.tracks) } },
        queue = app::queueSong,
    )
    return UiModel(state, actions)
}
