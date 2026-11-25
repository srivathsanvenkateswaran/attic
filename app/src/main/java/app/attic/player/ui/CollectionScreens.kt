package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.attic.player.R
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortAlbum
import app.attic.player.ui.comfort.ComfortArtist
import app.attic.player.ui.comfort.ComfortFavourites
import app.attic.player.ui.comfort.ComfortFolder
import app.attic.player.ui.comfort.ComfortGenre
import app.attic.player.ui.comfort.ComfortPlaylist
import app.attic.player.ui.expressive.ExpressiveAlbum
import app.attic.player.ui.expressive.ExpressiveArtist
import app.attic.player.ui.expressive.ExpressiveFavourites
import app.attic.player.ui.expressive.ExpressiveFolder
import app.attic.player.ui.expressive.ExpressiveGenre
import app.attic.player.ui.expressive.ExpressivePlaylist
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.rememberAlbumModel
import app.attic.player.ui.model.rememberArtistModel
import app.attic.player.ui.model.rememberFolderModel
import app.attic.player.ui.model.rememberGenreModel
import app.attic.player.ui.model.rememberLikedModel
import app.attic.player.ui.model.rememberPlaylistModel

/** The screen a [Destination] leads to, for the detail pane beside the Library's list. */
@Composable
fun DestinationScreen(library: Library, destination: Destination) {
    when (destination) {
        is Destination.Album -> AlbumScreen(library, destination.key)
        is Destination.Artist -> ArtistScreen(library, destination.key)
        is Destination.Folder -> FolderScreen(library, destination.name)
        is Destination.Playlist -> PlaylistScreen(library, destination.id)
        is Destination.Genre -> GenreScreen(destination.key)
        Destination.Liked -> LikedScreen(library)
    }
}

@Composable
fun AlbumScreen(library: Library, key: String) {
    val (state, actions) = rememberAlbumModel(library, key)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveAlbum(state, actions)
        UiStyle.Comfort -> ComfortAlbum(state, actions)
    }
}

@Composable
fun ArtistScreen(library: Library, key: String) {
    val (state, actions) = rememberArtistModel(library, key)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveArtist(state, actions)
        UiStyle.Comfort -> ComfortArtist(state, actions)
    }
}

@Composable
fun FolderScreen(library: Library, name: String) {
    val (state, actions) = rememberFolderModel(library, name)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveFolder(state, actions)
        UiStyle.Comfort -> ComfortFolder(state, actions)
    }
}

@Composable
fun GenreScreen(key: String) {
    val (state, actions) = rememberGenreModel(key)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveGenre(state, actions)
        UiStyle.Comfort -> ComfortGenre(state, actions)
    }
}

@Composable
fun LikedScreen(library: Library) {
    val (state, actions) = rememberLikedModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveFavourites(state, actions)
        UiStyle.Comfort -> ComfortFavourites(state, actions)
    }
}

@Composable
fun PlaylistScreen(library: Library, id: Long) {
    val (state, actions) = rememberPlaylistModel(library, id)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressivePlaylist(state, actions)
        UiStyle.Comfort -> ComfortPlaylist(state, actions)
    }

    // The sheet and dialogs sit outside the style's drawing, so what they hold survives a style switch.
    val playlist = state.playlist ?: return
    if (state.adding) {
        AddSongsSheet(library, playlist.id, playlist.trackKeys.toSet(), onDismiss = actions.dismissAdding)
    }
    if (state.renaming) {
        PlaylistNameDialog(
            title = stringResource(R.string.dialog_rename_playlist_title),
            confirm = stringResource(R.string.rename),
            initial = playlist.name,
            onDismiss = actions.dismissRenaming,
            onConfirm = actions.rename,
        )
    }
    if (state.confirmingDelete) {
        DeletePlaylistDialog(
            name = playlist.name,
            onDismiss = actions.dismissDeleting,
            onConfirm = actions.delete,
        )
    }
}
