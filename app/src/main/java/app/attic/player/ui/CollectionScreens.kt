package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicSurface
import app.attic.player.ui.classic.ClassicAlbum
import app.attic.player.ui.classic.ClassicArtist
import app.attic.player.ui.classic.ClassicFolder
import app.attic.player.ui.classic.ClassicLiked
import app.attic.player.ui.classic.ClassicPlaylist
import app.attic.player.ui.expressive.ExpressiveAlbum
import app.attic.player.ui.expressive.ExpressiveArtist
import app.attic.player.ui.expressive.ExpressiveFavourites
import app.attic.player.ui.expressive.ExpressiveFolder
import app.attic.player.ui.expressive.ExpressivePlaylist
import app.attic.player.ui.model.rememberAlbumModel
import app.attic.player.ui.model.rememberArtistModel
import app.attic.player.ui.model.rememberFolderModel
import app.attic.player.ui.model.rememberLikedModel
import app.attic.player.ui.model.rememberPlaylistModel

@Composable
fun AlbumScreen(library: Library, key: String) {
    val (state, actions) = rememberAlbumModel(library, key)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveAlbum(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicAlbum(state, actions) }
        UiStyle.Classic -> ClassicAlbum(state, actions)
    }
}

@Composable
fun ArtistScreen(library: Library, key: String) {
    val (state, actions) = rememberArtistModel(library, key)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveArtist(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicArtist(state, actions) }
        UiStyle.Classic -> ClassicArtist(state, actions)
    }
}

@Composable
fun FolderScreen(library: Library, name: String) {
    val (state, actions) = rememberFolderModel(library, name)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveFolder(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicFolder(state, actions) }
        UiStyle.Classic -> ClassicFolder(state, actions)
    }
}

@Composable
fun LikedScreen(library: Library) {
    val (state, actions) = rememberLikedModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveFavourites(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicLiked(state, actions) }
        UiStyle.Classic -> ClassicLiked(state, actions)
    }
}

@Composable
fun PlaylistScreen(library: Library, id: Long) {
    val (state, actions) = rememberPlaylistModel(library, id)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressivePlaylist(state, actions)
        UiStyle.Comfort -> ClassicSurface { ClassicPlaylist(state, actions) }
        UiStyle.Classic -> ClassicPlaylist(state, actions)
    }

    // The sheet and dialogs sit outside the style's drawing, so what they hold survives a style switch.
    val playlist = state.playlist ?: return
    if (state.adding) {
        AddSongsSheet(library, playlist.id, playlist.trackKeys.toSet(), onDismiss = actions.dismissAdding)
    }
    if (state.renaming) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirm = "Rename",
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
