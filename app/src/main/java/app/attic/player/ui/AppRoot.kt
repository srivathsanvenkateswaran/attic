package app.attic.player.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
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
import app.attic.player.ui.comfort.ComfortLoading
import app.attic.player.ui.comfort.ComfortOnboarding
import app.attic.player.ui.comfort.ComfortPermission
import app.attic.player.ui.comfort.ComfortShell
import app.attic.player.ui.expressive.ExpressiveLoading
import app.attic.player.ui.expressive.ExpressiveOnboarding
import app.attic.player.ui.expressive.ExpressivePermission
import app.attic.player.ui.expressive.ExpressiveShell
import app.attic.player.ui.model.RootState
import app.attic.player.ui.model.rememberOnboardingModel
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
    fun openGenre(key: String) = go("genre/${Uri.encode(key)}")
    fun openLiked() = go("liked")
    fun openSettings() = go("settings")
    fun openWrapped() = go("wrapped")
    fun openLicences() = go("licences")
    fun openLicence(id: String) = go("licences/${Uri.encode(id)}")
    fun openSearch() = selectTab("search")
    fun addToPlaylist(keys: List<String>) = pickPlaylist(keys)
    fun back() = nav.popBackStack()
}

/**
 * [playlistId] is set when the menu opens from a playlist, which adds "Remove from this playlist".
 * [fromNowPlaying] is set when it opens from Now Playing's menu button, which adds "Sleep timer".
 */
data class TrackMenuRequest(val track: Track, val playlistId: Long? = null, val fromNowPlaying: Boolean = false)

val LocalUi = staticCompositionLocalOf<UiActions> { error("No UiActions") }

/** True while Now Playing, the queue or lyrics covers the destinations, so a screen's own back handling stands aside. */
val LocalOverlayCovered = compositionLocalOf { false }

enum class Tab(val route: String) {
    Home("home"),
    Search("search"),
    Library("library"),
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

@Composable
fun AppRoot() {
    val app = LocalContext.current.attic
    val onboarded by app.onboarding.done.collectAsStateWithLifecycle()
    when (onboarded) {
        // Deciding takes one small database read on the first launch only; the bare surface avoids a flash.
        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        false -> FirstRun()
        true -> MainRoot()
    }
}

/** The first run, in whichever style is current; picking a look on its second step redraws it at once. */
@Composable
private fun FirstRun() {
    val (state, actions) = rememberOnboardingModel()
    // Welcome has no previous step, so back there falls through and leaves the app.
    BackHandler(enabled = state.step.previous() != null) { actions.back() }
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveOnboarding(state, actions)
        UiStyle.Comfort -> ComfortOnboarding(state, actions)
    }
}

/** After the first run: the access gate if audio access was taken away, the first scan, then the shell. */
@Composable
private fun MainRoot() {
    val (state, actions) = rememberRootModel()
    when (state) {
        is RootState.NeedsAccess -> when (LocalUiStyle.current) {
            UiStyle.Expressive -> ExpressivePermission(state, actions)
            UiStyle.Comfort -> ComfortPermission(state, actions)
        }
        RootState.Loading -> when (LocalUiStyle.current) {
            UiStyle.Expressive -> ExpressiveLoading()
            UiStyle.Comfort -> ComfortLoading()
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
    CompositionLocalProvider(LocalUi provides shell.ui, LocalOverlayCovered provides state.covered) {
        when (style) {
            UiStyle.Expressive -> ExpressiveShell(state, actions, slots)
            UiStyle.Comfort -> ComfortShell(state, actions, slots)
        }

        // While an overlay is up, back closes it instead of popping the NavHost. The last callback registered
        // wins, and moving the NavHost into another style's shell registers its callback again, so this one is
        // registered again on every style change to stay on top.
        key(style) {
            BackHandler(enabled = state.covered) { actions.closeTopOverlay() }
        }

        state.menu?.let { request ->
            TrackMenuSheet(request, library, onDismiss = actions.dismissMenu, overPlayer = state.covered)
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
        composable("genre/{key}") { entry ->
            GenreScreen(Uri.decode(entry.arguments?.getString("key").orEmpty()))
        }
        composable("liked") { LikedScreen(library) }
        composable("settings") { SettingsScreen(library) }
        composable("wrapped") { WrappedScreen(library) }
        composable("licences") { LicencesScreen() }
        composable("licences/{id}") { entry -> LicenceScreen(Uri.decode(entry.arguments?.getString("id").orEmpty())) }
    }
}
