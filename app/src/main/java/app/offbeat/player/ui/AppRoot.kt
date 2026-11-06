package app.offbeat.player.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.offbeat.player.data.Library
import app.offbeat.player.data.Track
import app.offbeat.player.offbeat
import kotlinx.coroutines.launch

/** App-wide UI actions, handed down instead of threading a dozen lambdas through every screen. */
class UiActions(
    private val nav: NavHostController,
    private val closeOverlays: () -> Unit,
    val showMenu: (TrackMenuRequest) -> Unit,
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
    fun back() = nav.popBackStack()
}

/** [playlistId] is set when the menu opens from a playlist, which adds "Remove from this playlist". */
data class TrackMenuRequest(val track: Track, val playlistId: Long? = null)

val LocalUi = staticCompositionLocalOf<UiActions> { error("No UiActions") }

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("home", "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Search("search", "Search", Icons.Rounded.Search, Icons.Rounded.Search),
    Library("library", "Your Library", Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Rounded.LibraryBooks),
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.offbeat
    val scope = rememberCoroutineScope()
    var hasAccess by remember { mutableStateOf(app.library.hasPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        asked = true
        hasAccess = result[Manifest.permission.READ_MEDIA_AUDIO] == true
        if (hasAccess) scope.launch { app.library.refresh() }
    }

    // Covers the user granting access from system settings and coming back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = app.library.hasPermission()
        if (now != hasAccess) {
            hasAccess = now
            scope.launch { app.library.refresh() }
        }
    }

    if (!hasAccess) {
        PermissionScreen(
            askedBefore = asked,
            onAllow = {
                permissions.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        return
    }

    val library by app.library.library.collectAsStateWithLifecycle()
    LaunchedEffect(library) { app.player.onLibraryChanged() }
    val loaded = library
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    MainShell(loaded)
}

@Composable
private fun MainShell(library: Library) {
    val app = LocalContext.current.offbeat
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<TrackMenuRequest?>(null) }
    val player by app.player.state.collectAsStateWithLifecycle()

    val ui = remember(nav) {
        UiActions(
            nav = nav,
            closeOverlays = {
                showNowPlaying = false
                showQueue = false
            },
            showMenu = { menu = it },
        )
    }

    LaunchedEffect(Unit) {
        app.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }
    LaunchedEffect(entry) {
        Tab.entries.firstOrNull { it.route == entry?.destination?.route }?.let { currentTab = it }
    }

    CompositionLocalProvider(LocalUi provides ui) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Hidden from accessibility services while covered, so they only see the top screen.
            val covered = showNowPlaying || showQueue
            Column(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
                Box(Modifier.weight(1f)) {
                    AppNavHost(nav, library)
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
                if (player.current != null) {
                    MiniPlayer(player, onOpen = { showNowPlaying = true })
                }
                BottomBar(currentTab) { tab ->
                    if (tab == currentTab) {
                        nav.popBackStack(tab.route, inclusive = false)
                    } else {
                        nav.navigate(tab.route) {
                            popUpTo(Tab.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying && player.current != null,
                enter = slideInVertically(tween(260)) { it },
                exit = slideOutVertically(tween(220)) { it },
            ) {
                NowPlayingScreen(
                    state = player,
                    onClose = { showNowPlaying = false },
                    onOpenQueue = { showQueue = true },
                )
            }
            AnimatedVisibility(
                visible = showQueue,
                enter = slideInVertically(tween(240)) { it },
                exit = slideOutVertically(tween(200)) { it },
            ) {
                QueueScreen(player, onClose = { showQueue = false })
            }

            BackHandler(enabled = showNowPlaying || showQueue) {
                if (showQueue) showQueue = false else showNowPlaying = false
            }

            menu?.let { request ->
                TrackMenuSheet(request, library, onDismiss = { menu = null })
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, library: Library) {
    NavHost(
        navController = nav,
        startDestination = Tab.Home.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
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
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .height(58.dp),
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
                Spacer(Modifier.height(3.dp))
                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}

@Composable
private fun PermissionScreen(askedBefore: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OffbeatMark(72.dp)
        Spacer(Modifier.height(28.dp))
        Text("Offbeat plays the music on this phone", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            if (askedBefore) {
                "Audio access is off, so Offbeat can't see your songs. Turn on Music and audio in the app's permissions."
            } else {
                "Allow audio access so it can read your songs in Music/Offline. Notifications show the playback controls."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (askedBefore) {
            Button(onClick = onOpenSettings) { Text("Open app permissions") }
        } else {
            Button(onClick = onAllow) { Text("Allow access") }
        }
    }
}
