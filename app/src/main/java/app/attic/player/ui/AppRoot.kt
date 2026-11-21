package app.attic.player.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.classic.ClassicSurface
import app.attic.player.ui.classic.ClassicLoading
import app.attic.player.ui.classic.ClassicPermissionScreen
import app.attic.player.ui.classic.ClassicShell
import app.attic.player.ui.model.RootState
import app.attic.player.ui.model.rememberRootModel
import app.attic.player.ui.model.rememberShellModel

/** App-wide UI actions, handed down instead of threading a dozen lambdas through every screen. */
class UiActions(
    private val nav: NavHostController,
    private val closeOverlays: () -> Unit,
    val showMenu: (TrackMenuRequest) -> Unit,
    private val pickPlaylist: (List<String>) -> Unit,
    private val selectTab: (String) -> Unit,
) {
    private fun go(route: String) {
        closeOverlays()
        nav.navigate(route) { launchSingleTop = true }
    }

    fun openAlbum(key: String) = go("album/${Uri.encode(key)}")
    fun openArtist(key: String) = go("artist/${Uri.encode(key)}")
    fun openFolder(name: String) = go("folder/${Uri.encode(name)}")
    fun openPlaylist(id: Long) = go("playlist/$id")
    fun openLiked() = go("liked")
    fun openSettings() = go("settings")
    fun openWrapped() = go("wrapped")
    fun openSearch() = selectTab("search")
    fun addToPlaylist(keys: List<String>) = pickPlaylist(keys)
    fun back() = nav.popBackStack()
}

/** [playlistId] is set when the menu opens from a playlist, which adds "Remove from this playlist". */
data class TrackMenuRequest(val track: Track, val playlistId: Long? = null)

val LocalUi = staticCompositionLocalOf<UiActions> { error("No UiActions") }

enum class Tab(val route: String, val label: String) {
    Home("home", "Home"),
    Search("search", "Search"),
    Library("library", "Your Library"),
}

private val TabRoutes = Tab.entries.map { it.route }.toSet()

/** True for the three tab roots, which styles usually animate differently from detail screens. */
fun isTabRoute(route: String?): Boolean = route in TabRoutes

/** How a style animates moving between destinations. */
@Immutable
class NavTransitions(
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
)

/**
 * The pieces a style's shell arranges. Each is movable content created once in [MainShell], so a style
 * can put it anywhere and a live style switch moves it, with its state, instead of recreating it.
 */
class ShellSlots(
    /** The destinations. The NavHost lives here, so its back stack and saved screen state survive a switch. */
    val content: @Composable (NavTransitions) -> Unit,
    val miniPlayer: @Composable () -> Unit,
    val nowPlaying: @Composable () -> Unit,
    val queue: @Composable () -> Unit,
    val lyrics: @Composable () -> Unit,
)

/**
 * The same slots, each drawn in the style's own theme. A shell still drawn by Classic sits in a
 * ClassicSurface; this hands its slots back to the style, so a screen with drawing of its own looks right.
 */
fun ShellSlots.inStyleSurface() = ShellSlots(
    content = { transitions -> StyleSurface { content(transitions) } },
    miniPlayer = { StyleSurface { miniPlayer() } },
    nowPlaying = { StyleSurface { nowPlaying() } },
    queue = { StyleSurface { queue() } },
    lyrics = { StyleSurface { lyrics() } },
)

@Composable
fun AppRoot() {
    val (state, actions) = rememberRootModel()
    when (state) {
        is RootState.NeedsAccess -> when (LocalUiStyle.current) {
            UiStyle.Expressive -> ClassicSurface { ClassicPermissionScreen(state, actions) }
            UiStyle.Comfort -> ClassicSurface { ClassicPermissionScreen(state, actions) }
            UiStyle.Classic -> ClassicPermissionScreen(state, actions)
        }
        RootState.Loading -> when (LocalUiStyle.current) {
            UiStyle.Expressive -> ClassicSurface { ClassicLoading() }
            UiStyle.Comfort -> ClassicSurface { ClassicLoading() }
            UiStyle.Classic -> ClassicLoading()
        }
        is RootState.Ready -> MainShell(state.library)
    }
}

@Composable
private fun MainShell(library: Library) {
    val shell = rememberShellModel()
    val state = shell.state
    val actions = shell.actions

    // Created once and only ever moved, never recreated, whichever style draws the shell around them.
    val navHost = remember(shell.nav) {
        movableContentOf { lib: Library, transitions: NavTransitions -> AppNavHost(shell.nav, lib, transitions) }
    }
    val miniPlayer = remember(actions) {
        movableContentOf { player: PlayerUiState -> MiniPlayer(player, onOpen = actions.openNowPlaying) }
    }
    val nowPlaying = remember(actions) {
        movableContentOf { player: PlayerUiState ->
            NowPlayingScreen(
                state = player,
                onClose = actions.closeNowPlaying,
                onOpenQueue = actions.openQueue,
                onOpenLyrics = actions.openLyrics,
            )
        }
    }
    val queue = remember(actions) {
        movableContentOf { player: PlayerUiState -> QueueScreen(player, onClose = actions.closeQueue) }
    }
    val lyrics = remember(actions) {
        movableContentOf { player: PlayerUiState -> LyricsScreen(player, onClose = actions.closeLyrics) }
    }
    val player = state.player
    val slots = ShellSlots(
        content = { transitions -> navHost(library, transitions) },
        miniPlayer = { miniPlayer(player) },
        nowPlaying = { nowPlaying(player) },
        queue = { queue(player) },
        lyrics = { lyrics(player) },
    )

    val style = LocalUiStyle.current
    CompositionLocalProvider(LocalUi provides shell.ui) {
        when (style) {
            UiStyle.Expressive -> ClassicSurface { ClassicShell(state, actions, slots.inStyleSurface()) }
            UiStyle.Comfort -> ClassicSurface { ClassicShell(state, actions, slots.inStyleSurface()) }
            UiStyle.Classic -> ClassicShell(state, actions, slots)
        }

        // While an overlay is up, back closes it instead of popping the NavHost. The last callback registered
        // wins, and moving the NavHost into another style's shell registers its callback again, so this one is
        // registered again on every style change to stay on top.
        key(style) {
            BackHandler(enabled = state.covered) { actions.closeTopOverlay() }
        }

        state.menu?.let { request ->
            TrackMenuSheet(request, library, onDismiss = actions.dismissMenu)
        }
        state.picking?.let { keys ->
            PlaylistPickerSheet(keys, onDismiss = actions.dismissPicker)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, library: Library, transitions: NavTransitions) {
    NavHost(
        navController = nav,
        startDestination = Tab.Home.route,
        enterTransition = transitions.enter,
        exitTransition = transitions.exit,
        popEnterTransition = transitions.popEnter,
        popExitTransition = transitions.popExit,
    ) {
        composable(Tab.Home.route) { HomeScreen(library) }
        composable(Tab.Search.route) { SearchScreen(library) }
        composable(Tab.Library.route) { LibraryScreen(library) }
        composable("album/{key}") { entry ->
            AlbumScreen(library, Uri.decode(entry.arguments?.getString("key").orEmpty()))
        }
        composable("artist/{key}") { entry ->
            ArtistScreen(library, Uri.decode(entry.arguments?.getString("key").orEmpty()))
        }
        composable("folder/{name}") { entry ->
            FolderScreen(library, Uri.decode(entry.arguments?.getString("name").orEmpty()))
        }
        composable("playlist/{id}") { entry ->
            PlaylistScreen(library, entry.arguments?.getString("id")?.toLongOrNull() ?: -1L)
        }
        composable("liked") { LikedScreen(library) }
        composable("settings") { SettingsScreen(library) }
        composable("wrapped") { WrappedScreen(library) }
    }
}
