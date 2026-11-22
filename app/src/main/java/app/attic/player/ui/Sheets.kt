package app.attic.player.ui

import androidx.compose.runtime.Composable
import app.attic.player.AtticApp
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.ui.comfort.ComfortAddSongs
import app.attic.player.ui.comfort.ComfortDeletePlaylistDialog
import app.attic.player.ui.comfort.ComfortLanguagePicker
import app.attic.player.ui.comfort.ComfortPlaylistNameDialog
import app.attic.player.ui.comfort.ComfortPlaylistPicker
import app.attic.player.ui.comfort.ComfortTrackMenu
import app.attic.player.ui.comfort.TintedCover
import app.attic.player.ui.classic.ClassicAddSongs
import app.attic.player.ui.classic.ClassicDeletePlaylistDialog
import app.attic.player.ui.classic.ClassicLanguagePicker
import app.attic.player.ui.classic.ClassicPlaylistNameDialog
import app.attic.player.ui.classic.ClassicPlaylistPicker
import app.attic.player.ui.classic.ClassicTrackMenu
import app.attic.player.ui.expressive.ExpressiveAddSongs
import app.attic.player.ui.expressive.ExpressiveDeletePlaylistDialog
import app.attic.player.ui.expressive.ExpressiveLanguagePicker
import app.attic.player.ui.expressive.ExpressivePlaylistNameDialog
import app.attic.player.ui.expressive.ExpressivePlaylistPicker
import app.attic.player.ui.expressive.ExpressiveTrackMenu
import app.attic.player.ui.expressive.FromCover
import app.attic.player.ui.model.DeletePlaylistActions
import app.attic.player.ui.model.DeletePlaylistState
import app.attic.player.ui.model.languagePickerModel
import app.attic.player.ui.model.rememberAddSongsModel
import app.attic.player.ui.model.rememberPlaylistNameModel
import app.attic.player.ui.model.rememberPlaylistPickerModel
import app.attic.player.ui.model.rememberTrackMenuModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Creates a playlist on the app scope, so it survives the dialog closing. */
fun CoroutineScope.launchCreate(app: AtticApp, name: String, then: (Long) -> Unit) {
    launch { then(app.userData.createPlaylist(name)) }
}

/**
 * [overPlayer] is true while Now Playing, the queue or lyrics is open: Expressive then draws the menu
 * and the pickers it opens in the cover's scheme, along with the screen under them.
 */
@Composable
fun TrackMenuSheet(request: TrackMenuRequest, library: Library, onDismiss: () -> Unit, overPlayer: Boolean = false) {
    val (state, actions) = rememberTrackMenuModel(request, library, onDismiss)
    val style = LocalUiStyle.current
    // Over the player Expressive takes the cover's scheme, and Comfort the wash of the song that is
    // playing, the surfaces of the screen underneath (07 H). Classic ignores it.
    val inScheme: @Composable (@Composable () -> Unit) -> Unit = { content ->
        when {
            style == UiStyle.Expressive && overPlayer -> FromCover(state.track.mediaId, content = content)
            style == UiStyle.Comfort && overPlayer -> {
                val playing by LocalContext.current.attic.player.state.collectAsStateWithLifecycle()
                TintedCover(playing.current?.mediaId, wash = true, content = content)
            }
            else -> content()
        }
    }
    when {
        state.pickingPlaylist -> inScheme { PlaylistPickerSheet(listOf(state.track.key), onDismiss = onDismiss) }
        // Comfort opens the language picker as a pop-over on the menu itself.
        state.pickingLanguage && style != UiStyle.Comfort ->
            inScheme { LanguagePickerSheet(current = state.language, title = state.track.title, onChoose = actions.chooseLanguage) }
        else -> when (style) {
            UiStyle.Expressive -> inScheme { ExpressiveTrackMenu(state, actions) }
            UiStyle.Comfort -> inScheme { ComfortTrackMenu(state, actions) }
            UiStyle.Classic -> ClassicTrackMenu(state, actions)
        }
    }
}

/** Corrects what radio believes about a song. Your choice is never overwritten. */
@Composable
private fun LanguagePickerSheet(current: String?, title: String, onChoose: (String) -> Unit) {
    val (state, actions) = languagePickerModel(current, onChoose, songTitle = title)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveLanguagePicker(state, actions)
        UiStyle.Comfort -> ComfortLanguagePicker(state, actions)
        UiStyle.Classic -> ClassicLanguagePicker(state, actions)
    }
}

@Composable
fun PlaylistPickerSheet(trackKeys: List<String>, onDismiss: () -> Unit) {
    val (state, actions) = rememberPlaylistPickerModel(trackKeys, onDismiss)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressivePlaylistPicker(state, actions)
        UiStyle.Comfort -> ComfortPlaylistPicker(state, actions)
        UiStyle.Classic -> ClassicPlaylistPicker(state, actions)
    }

    if (state.creating) {
        PlaylistNameDialog(
            title = "New playlist",
            confirm = "Create",
            initial = "",
            onDismiss = actions.dismissCreating,
            onConfirm = actions.create,
        )
    }
}

@Composable
fun PlaylistNameDialog(
    title: String,
    confirm: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val (state, actions) = rememberPlaylistNameModel(title, confirm, initial, onDismiss, onConfirm)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressivePlaylistNameDialog(state, actions)
        UiStyle.Comfort -> ComfortPlaylistNameDialog(state, actions)
        UiStyle.Classic -> ClassicPlaylistNameDialog(state, actions)
    }
}

@Composable
fun DeletePlaylistDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val state = DeletePlaylistState(name)
    val actions = DeletePlaylistActions(confirm = onConfirm, dismiss = onDismiss)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveDeletePlaylistDialog(state, actions)
        UiStyle.Comfort -> ComfortDeletePlaylistDialog(state, actions)
        UiStyle.Classic -> ClassicDeletePlaylistDialog(state, actions)
    }
}

@Composable
fun AddSongsSheet(library: Library, playlistId: Long, existing: Set<String>, onDismiss: () -> Unit) {
    val (state, actions) = rememberAddSongsModel(library, playlistId, existing, onDismiss)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveAddSongs(state, actions)
        UiStyle.Comfort -> ComfortAddSongs(state, actions)
        UiStyle.Classic -> ClassicAddSongs(state, actions)
    }
}
