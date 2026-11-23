package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.NavTransitions
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.isTabRoute
import app.attic.player.ui.model.MiniPlayerActions
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.rememberPosition

/**
 * Moving between destinations: a tab root fades through; a detail screen comes in a short way from the
 * end while fading, and going back reverses it.
 */
val ComfortNavTransitions = NavTransitions(
    enter = {
        if (isTabRoute(targetState.destination.route)) {
            fadeIn(tween(220, delayMillis = 60))
        } else {
            slideInHorizontally(tween(300)) { it / 8 } + fadeIn(tween(220))
        }
    },
    exit = { fadeOut(tween(120)) },
    popEnter = { fadeIn(tween(220)) },
    popExit = {
        if (isTabRoute(initialState.destination.route)) {
            fadeOut(tween(120))
        } else {
            slideOutHorizontally(tween(260)) { it / 8 } + fadeOut(tween(180))
        }
    },
)

/** Full-screen overlays rise without overshooting, so no gap opens under them. */
private fun <T> overlaySpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)

/**
 * The Comfort shell (the dock on every sheet): destinations fill the screen and scroll under the
 * dock, one floating card whose first row is the mini player and whose second is the tabs. With
 * nothing queued the mini row folds away and the dock is a capsule tab bar. While the keyboard is up
 * the dock stays where it is, under the keyboard. Now Playing, the queue and lyrics rise over
 * everything and have no dock. The toast sits above the dock, or low over an overlay, and draws above
 * the overlays.
 *
 * Destinations get the room the dock takes as [LocalBottomPadding] (the kit's `.pad-dock` and
 * `.pad-tabs`): the dock's measured height, its 28dp fade included.
 */
@Composable
fun ComfortShell(state: ShellState, actions: ShellActions, slots: ShellSlots) {
    val colors = Comfort.colors
    val density = LocalDensity.current
    val systemNav = WindowInsets.navigationBars
    var dockHeightPx by remember { mutableIntStateOf(0) }
    val dockHeight = with(density) {
        if (dockHeightPx > 0) dockHeightPx.toDp() else (if (state.hasCurrentSong) 176.dp else 112.dp)
    }

    Box(Modifier.fillMaxSize().background(colors.ground)) {
        val covered = state.covered
        // Hidden from accessibility services while covered, so they only see the top screen.
        Box(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            CompositionLocalProvider(LocalBottomPadding provides dockHeight) {
                slots.content(ComfortNavTransitions)
            }

            val tabs = Tab.entries
            Dock(
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { dockHeightPx = it.height },
                mini = {
                    AnimatedVisibility(
                        visible = state.hasCurrentSong,
                        enter = expandVertically(tween(240), expandFrom = Alignment.Top) + fadeIn(tween(240)),
                        exit = shrinkVertically(tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(180)),
                    ) {
                        slots.miniPlayer()
                    }
                },
            ) {
                TabBar(
                    items = tabs.map { TabItem(stringResource(it.labelRes), it.symbol) },
                    selectedIndex = tabs.indexOf(state.currentTab),
                    onSelect = { actions.selectTab(tabs[it]) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.nowPlayingVisible,
            enter = slideInVertically(overlaySpring()) { it },
            exit = slideOutVertically(overlaySpring()) { it },
        ) {
            slots.nowPlaying()
        }
        AnimatedVisibility(
            visible = state.queueVisible,
            enter = slideInVertically(overlaySpring()) { it },
            exit = slideOutVertically(overlaySpring()) { it },
        ) {
            slots.queue()
        }
        AnimatedVisibility(
            visible = state.lyricsVisible,
            enter = slideInVertically(overlaySpring()) { it },
            exit = slideOutVertically(overlaySpring()) { it },
        ) {
            slots.lyrics()
        }

        // Above the player overlays, so messages raised from Now Playing can be seen. The kit's `.toast`
        // sits 34dp above the dock's card (180 against a 146dp dock); `.toast.low` sits low over an overlay.
        ToastHost(
            state.snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .offset {
                    val bottom = if (covered) {
                        systemNav.getBottom(this) + 32.dp.roundToPx()
                    } else {
                        dockHeightPx + 6.dp.roundToPx()
                    }
                    IntOffset(0, -bottom)
                },
        )
    }
}

private val Tab.labelRes: Int
    get() = when (this) {
        Tab.Home -> R.string.tab_home
        Tab.Search -> R.string.tab_search
        Tab.Library -> R.string.tab_library
    }

private val Tab.symbol: Symbol
    get() = when (this) {
        Tab.Home -> Symbol.Home
        Tab.Search -> Symbol.Search
        Tab.Library -> Symbol.LibraryMusic
    }

/**
 * The mini player, the dock's first row (sheet 05 frame E): the kit's [MiniPlayer], tinted from the
 * cover and filled as far as the song has played. Tap opens Now Playing; swiping the text sideways
 * past 28% of its width skips.
 */
@Composable
fun ComfortMiniPlayer(state: MiniPlayerState, actions: MiniPlayerActions) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified, deepen = false)
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 500)
    val duration = track.durationMs
    MiniPlayer(
        title = track.title,
        artist = track.artist,
        art = Art.of(track.mediaId),
        progress = { if (duration > 0) position.toFloat() / duration else 0f },
        playing = state.isPlaying,
        favourite = state.liked,
        onToggleFavourite = actions.toggleLike,
        onPlayPause = actions.togglePlayPause,
        onClick = actions.open,
        seed = seed,
        onSwipeNext = actions.next,
        onSwipePrevious = actions.previous,
    )
}
