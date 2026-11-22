package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.AtticApp
import app.attic.player.attic
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.UserPlaylist
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.LocalUi
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.UiActions

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
)

class PlayControlsActions(
    val addToPlaylist: () -> Unit,
    val shuffle: () -> Unit,
    /** Pauses or resumes this collection, or starts it from the top if something else is playing. */
    val playOrPause: () -> Unit,
)

private fun playControls(
    app: AtticApp,
    ui: UiActions,
    player: PlayerUiState,
    tracks: List<Track>,
    contextName: String,
): UiModel<PlayControlsState, PlayControlsActions> {
    val isThisContext = player.contextName == contextName && player.current?.key in tracks.map { it.key }
    val state = PlayControlsState(
        contextName = contextName,
        enabled = tracks.isNotEmpty(),
        shuffleOn = player.shuffle && isThisContext,
        isPlaying = isThisContext && player.isPlaying,
    )
    val actions = PlayControlsActions(
        addToPlaylist = { ui.addToPlaylist(tracks.map { it.key }) },
        shuffle = { app.player.shufflePlay(tracks, contextName) },
        playOrPause = {
            if (isThisContext) app.player.togglePlayPause() else app.player.play(tracks, 0, contextName, shuffled = false)
        },
    )
    return UiModel(state, actions)
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
)

@Composable
fun rememberAlbumModel(library: Library, key: String): UiModel<AlbumState, AlbumActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val album = library.albumByKey[key]
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = album?.tracks.orEmpty()
    val controls = playControls(app, ui, player, tracks, album?.name.orEmpty())
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
    )
    return UiModel(state, actions)
}

// Artist

@Immutable
class ArtistState(
    /** Null when the artist has gone from the library. */
    val artist: Artist?,
    /** No play counts from a streaming service here, so "Popular" means what you've played most recently. */
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
    val controls = playControls(app, ui, player, artist?.tracks.orEmpty(), artist?.name.orEmpty())
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
    )
    return UiModel(state, actions)
}

private const val POPULAR_COUNT = 5

// Folder

@Immutable
class FolderState(
    /** Null when the folder has gone from Music/Offline. */
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
)

@Composable
fun rememberFolderModel(library: Library, name: String): UiModel<FolderState, FolderActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val folder = library.folders.firstOrNull { it.name == name }
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = folder?.tracks.orEmpty()
    val controls = playControls(app, ui, player, tracks, folder?.name.orEmpty())
    val state = FolderState(folder, controls.state, player.current?.key, player.isPlaying)
    val actions = FolderActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, folder?.name.orEmpty()) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
    )
    return UiModel(state, actions)
}

// Liked Songs

@Immutable
class LikedState(
    /** Newest first. */
    val tracks: List<Track>,
    val controls: PlayControlsState,
    val currentKey: String?,
    val isPlaying: Boolean,
)

class LikedActions(
    val back: () -> Unit,
    val controls: PlayControlsActions,
    val play: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
)

@Composable
fun rememberLikedModel(library: Library): UiModel<LikedState, LikedActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val likedKeys by app.userData.likedKeys.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracks = remember(likedKeys, library) { likedKeys.mapNotNull { library.trackByKey[it] } }
    val controls = playControls(app, ui, player, tracks, LIKED_SONGS)
    val state = LikedState(tracks, controls.state, player.current?.key, player.isPlaying)
    val actions = LikedActions(
        back = { ui.back() },
        controls = controls.actions,
        play = { index -> app.player.play(tracks, index, LIKED_SONGS) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
    )
    return UiModel(state, actions)
}

private const val LIKED_SONGS = "Liked Songs"

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
    val toggleReordering: () -> Unit,
    /** Saves the order rows were dragged into. */
    val reorder: (keys: List<String>) -> Unit,
    /** Does nothing in reorder mode. */
    val play: (Track) -> Unit,
    val showMenu: (Track) -> Unit,
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
    val controls = playControls(app, ui, player, tracks, name)
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
        toggleReordering = { reordering = !reordering },
        reorder = { keys -> app.userData.reorderPlaylist(id, keys) },
        play = { track ->
            if (!reordering) app.player.play(tracks, tracks.indexOf(track), name)
        },
        showMenu = { ui.showMenu(TrackMenuRequest(it, playlistId = id)) },
    )
    return UiModel(state, actions)
}
