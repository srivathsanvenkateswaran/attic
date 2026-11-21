package app.attic.player.ui.classic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.attic.player.ui.AtticMark
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.NavTransitions
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.isTabRoute
import app.attic.player.ui.model.RootActions
import app.attic.player.ui.model.RootState
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState
import app.attic.player.ui.pressable

/** A detail screen slides in over its parent. Tab switches are just a quick fade. */
val ClassicNavTransitions = NavTransitions(
    enter = {
        if (isTabRoute(targetState.destination.route)) fadeIn(tween(120)) else slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(180))
    },
    exit = { fadeOut(tween(120)) },
    popEnter = { fadeIn(tween(160)) },
    popExit = {
        if (isTabRoute(initialState.destination.route)) fadeOut(tween(120)) else slideOutHorizontally(tween(200)) { it / 6 } + fadeOut(tween(160))
    },
)

@Composable
fun ClassicShell(state: ShellState, actions: ShellActions, slots: ShellSlots) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Hidden from accessibility services while covered, so they only see the top screen.
        val covered = state.covered
        val bottomBars = (if (state.hasCurrentSong) MINI_PLAYER_HEIGHT else 0.dp) + TAB_BAR_HEIGHT +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            CompositionLocalProvider(LocalBottomPadding provides bottomBars + 12.dp) {
                slots.content(ClassicNavTransitions)
            }
            Column(Modifier.align(Alignment.BottomCenter)) {
                AnimatedVisibility(
                    visible = state.hasCurrentSong,
                    enter = slideInVertically(tween(240)) { it } + fadeIn(tween(240)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                ) {
                    slots.miniPlayer()
                }
                BottomBar(state.currentTab, actions.selectTab)
            }
        }

        AnimatedVisibility(
            visible = state.nowPlayingVisible,
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            slots.nowPlaying()
        }
        AnimatedVisibility(
            visible = state.queueVisible,
            enter = slideInVertically(tween(240)) { it },
            exit = slideOutVertically(tween(200)) { it },
        ) {
            slots.queue()
        }

        AnimatedVisibility(
            visible = state.lyricsVisible,
            enter = slideInVertically(tween(260)) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            slots.lyrics()
        }

        // Above the player overlays, so messages raised from Now Playing can be seen.
        SnackbarHost(
            state.snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (covered) 48.dp else bottomBars),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

private val MINI_PLAYER_HEIGHT = 64.dp
private val TAB_BAR_HEIGHT = 58.dp

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.Home -> Icons.Outlined.Home
        Tab.Search -> Icons.Rounded.Search
        Tab.Library -> Icons.AutoMirrored.Outlined.LibraryBooks
    }

private val Tab.selectedIcon: ImageVector
    get() = when (this) {
        Tab.Home -> Icons.Rounded.Home
        Tab.Search -> Icons.Rounded.Search
        Tab.Library -> Icons.AutoMirrored.Rounded.LibraryBooks
    }

/** Floats over the content and fades it out, rather than sitting on a solid block. */
@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to background.copy(alpha = 0.72f), 0.55f to background.copy(alpha = 0.97f), 1f to background))
            .navigationBarsPadding()
            .height(TAB_BAR_HEIGHT),
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val tint by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(140),
                label = "tabTint",
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .semantics { this.selected = selected }
                    .pressable(scale = 0.9f) { onSelect(tab) },
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
fun ClassicLoading() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ClassicPermissionScreen(state: RootState.NeedsAccess, actions: RootActions) {
    val askedBefore = state.askedBefore
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
        AtticMark(72.dp)
        Spacer(Modifier.height(28.dp))
        Text("Attic plays the music on this phone", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            if (askedBefore) {
                "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions."
            } else {
                "Allow audio access so it can read your songs in Music/Offline. Notifications show the playback controls."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (askedBefore) {
            Button(onClick = actions.openAppPermissions) { Text("Open app permissions") }
        } else {
            Button(onClick = actions.allowAccess) { Text("Allow access") }
        }
    }
}
