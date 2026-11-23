package app.attic.player.ui.model

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.attic.player.attic
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.Tab
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.UiActions

/** The shell around the destinations: which tab is lit, which overlay is up, and the messages to show. */
@Stable
class ShellState(
    val player: PlayerUiState,
    /** The tab root visited last. Detail screens keep it lit. */
    val currentTab: Tab,
    val hasCurrentSong: Boolean,
    val nowPlayingVisible: Boolean,
    val queueVisible: Boolean,
    val lyricsVisible: Boolean,
    /** An overlay covers the destinations, so they are hidden from accessibility services. */
    val covered: Boolean,
    val snackbar: SnackbarHostState,
    val menu: TrackMenuRequest?,
    val picking: List<String>?,
)

class ShellActions(
    /** Re-selecting the current tab returns to its root. Another tab pops to Home, keeping each tab's state. */
    val selectTab: (Tab) -> Unit,
    val openNowPlaying: () -> Unit,
    val closeNowPlaying: () -> Unit,
    val openQueue: () -> Unit,
    val closeQueue: () -> Unit,
    val openLyrics: () -> Unit,
    val closeLyrics: () -> Unit,
    /** System back while covered: Lyrics, then Queue, then Now Playing. */
    val closeTopOverlay: () -> Unit,
    val dismissMenu: () -> Unit,
    val dismissPicker: () -> Unit,
)

/** The shell's model plus the navigation it owns, which every style's shell shares. */
class ShellModel(
    val nav: NavHostController,
    val ui: UiActions,
    val state: ShellState,
    val actions: ShellActions,
)

@Composable
fun rememberShellModel(): ShellModel {
    val app = LocalContext.current.attic
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<TrackMenuRequest?>(null) }
    var picking by remember { mutableStateOf<List<String>?>(null) }
    val player by app.player.state.collectAsStateWithLifecycle()

    val ui = remember(nav) {
        UiActions(
            nav = nav,
            closeOverlays = {
                showNowPlaying = false
                showQueue = false
                showLyrics = false
            },
            showMenu = { menu = it },
            pickPlaylist = { picking = it },
            selectTab = { route ->
                nav.navigate(route) {
                    popUpTo(Tab.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        app.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    // A widget tile asks for an album by its key.
    val requestedAlbum by app.requestedAlbum.collectAsStateWithLifecycle()
    LaunchedEffect(requestedAlbum) {
        requestedAlbum?.let { key ->
            ui.openAlbum(key)
            app.requestedAlbum.value = null
        }
    }

    // A file opened from another app has started playing.
    val requestedNowPlaying by app.requestedNowPlaying.collectAsStateWithLifecycle()
    LaunchedEffect(requestedNowPlaying) {
        if (requestedNowPlaying) {
            showQueue = false
            showLyrics = false
            showNowPlaying = true
            app.requestedNowPlaying.value = false
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }
    LaunchedEffect(entry) {
        Tab.entries.firstOrNull { it.route == entry?.destination?.route }?.let { currentTab = it }
    }

    val actions = remember(nav) {
        ShellActions(
            selectTab = { tab ->
                if (tab == currentTab) {
                    nav.popBackStack(tab.route, inclusive = false)
                } else {
                    nav.navigate(tab.route) {
                        popUpTo(Tab.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            openNowPlaying = { showNowPlaying = true },
            closeNowPlaying = { showNowPlaying = false },
            openQueue = { showQueue = true },
            closeQueue = { showQueue = false },
            openLyrics = { showLyrics = true },
            closeLyrics = { showLyrics = false },
            closeTopOverlay = {
                when {
                    showLyrics -> showLyrics = false
                    showQueue -> showQueue = false
                    else -> showNowPlaying = false
                }
            },
            dismissMenu = { menu = null },
            dismissPicker = { picking = null },
        )
    }

    val hasCurrentSong = player.current != null
    val state = ShellState(
        player = player,
        currentTab = currentTab,
        hasCurrentSong = hasCurrentSong,
        nowPlayingVisible = showNowPlaying && hasCurrentSong,
        queueVisible = showQueue,
        lyricsVisible = showLyrics && hasCurrentSong,
        covered = showNowPlaying || showQueue || showLyrics,
        snackbar = snackbar,
        menu = menu,
        picking = picking,
    )
    return ShellModel(nav, ui, state, actions)
}
