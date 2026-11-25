package app.attic.player.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.adaptive.MiniPlayerMaxWidthDp
import app.attic.player.ui.adaptive.PlayerPlacement
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState
import kotlin.math.max

private val MiniPlayerHeight = 64.dp
/** Between the floating mini player and the gesture area. */
private val FloatingGap = 12.dp

/**
 * The shell on a medium or expanded window (SPEC.md, the shell): the navigation rail at the start
 * edge and the destinations beside it. The playing song is the player bar along the bottom of the
 * content area on an expanded window, and the phone's mini player floating there on a medium one, at
 * most [MiniPlayerMaxWidthDp] wide and centred, over a band of the page colour behind the gesture
 * handle. Now Playing, the queue and lyrics still cover the whole window, and the snackbar sits above
 * the bar or the mini player.
 *
 * The destinations get the room the bar or mini player takes as [LocalBottomPadding], with the 24dp
 * the phone shell adds. Keyboard focus moves through the rail, then the content, then the bar.
 */
@Composable
internal fun ExpressiveWideShell(state: ShellState, actions: ShellActions, slots: ShellSlots, window: WindowLayout) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val systemNav = WindowInsets.navigationBars
    val bar = window.player == PlayerPlacement.Bar
    val navBottom = with(density) { systemNav.getBottom(this).toDp() }
    val playerSpace = when {
        !state.hasCurrentSong -> navBottom
        bar -> PlayerBarHeight + navBottom
        else -> MiniPlayerHeight + FloatingGap + navBottom
    }
    val links = remember(actions) { PlayerBarLinks(actions.openQueue, actions.openLyrics) }

    Box(Modifier.fillMaxSize().background(colors.surface)) {
        val covered = state.covered
        Row(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            val tabs = Tab.entries
            ExpressiveNavigationRail(
                items = tabs.map { NavItem(stringResource(it.labelRes), it.symbol) },
                selectedIndex = tabs.indexOf(state.currentTab),
                onSelect = { actions.selectTab(tabs[it]) },
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CompositionLocalProvider(LocalBottomPadding provides playerSpace + 24.dp) {
                    slots.content(ExpressiveNavTransitions)
                }
                if (!bar) {
                    // The page colour behind the gesture handle, so the page doesn't show under it.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(systemNav)
                            .background(colors.surface),
                    )
                }
                PlayerSlot(state.hasCurrentSong, bar, navBottom, links, slots)
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

        // Above the player overlays, so messages raised from Now Playing can be seen.
        SnackbarHost(
            state.snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = if (covered) 0.dp else RailWidth)
                .offset {
                    val bottom = if (covered) {
                        systemNav.getBottom(this) + 24.dp.roundToPx()
                    } else {
                        max(playerSpace.roundToPx(), ime.getBottom(this)) + 16.dp.roundToPx()
                    }
                    IntOffset(0, -bottom)
                },
        ) { data ->
            val cover = state.player.current?.mediaId
            if (covered && cover != null) FromCover(cover) { ExpressiveSnackbar(data) } else ExpressiveSnackbar(data)
        }
    }
}

/** The player bar, or the floating mini player, at the bottom of the content area. */
@Composable
private fun BoxScope.PlayerSlot(visible: Boolean, bar: Boolean, navBottom: Dp, links: PlayerBarLinks, slots: ShellSlots) {
    val ime = WindowInsets.ime
    val systemNav = WindowInsets.navigationBars
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset { IntOffset(0, -max(0, ime.getBottom(this) - systemNav.getBottom(this))) },
        enter = slideInVertically(ExpressiveMotion.defaultSpatial()) { it } + fadeIn(ExpressiveMotion.defaultEffects()),
        exit = slideOutVertically(ExpressiveMotion.defaultSpatial()) { it } + fadeOut(ExpressiveMotion.fastEffects()),
    ) {
        if (bar) {
            CompositionLocalProvider(LocalPlayerBarLinks provides links) { slots.miniPlayer() }
        } else {
            Box(
                Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = FloatingGap + navBottom)
                    .widthIn(max = MiniPlayerMaxWidthDp.dp),
            ) { slots.miniPlayer() }
        }
    }
}
