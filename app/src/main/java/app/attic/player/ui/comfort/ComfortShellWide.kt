package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.MiniPlayerMaxWidthDp
import app.attic.player.ui.adaptive.PlayerPlacement
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState

/** How far the content fades out above the player bar or the floating mini player, as above the phone's dock. */
private val PlayerFade = 36.dp

/** The player's card sits this far above the gesture area. */
private val PlayerGap = 12.dp

/**
 * The Comfort shell on medium and expanded windows (docs/design/2026-11-25-tablet/SPEC.md, the
 * shell). [ComfortSideDock] stands at the start edge, 12dp from it and from the status bar and the
 * gesture area; the destinations fill the rest, their column starting 8dp after the dock and
 * ending 8dp before the end edge, so the phone's 16dp card margins put their cards 24dp from both.
 *
 * The song playing floats at the bottom of the content area, over the same fade to the ground the
 * phone's dock has: on an expanded window as [ComfortPlayerBar], spanning the cards' width; on a
 * medium one as the phone's mini player in the dock's card, at most [MiniPlayerMaxWidthDp] wide and
 * centred. Destinations get the room it takes as [LocalBottomPadding]. Now Playing, the queue and
 * lyrics still rise over the whole window, and the toast sits above the player.
 *
 * The slots are the same movable content the phone shell places, so resizing the window between
 * the two keeps the NavHost, its back stack and the overlays as they are.
 */
@Composable
internal fun ComfortWideShell(state: ShellState, actions: ShellActions, slots: ShellSlots) {
    val colors = Comfort.colors
    val density = LocalDensity.current
    val bar = LocalWindowLayout.current.player == PlayerPlacement.Bar
    val systemNav = WindowInsets.navigationBars
    val navigation = systemNav.asPaddingValues().calculateBottomPadding()
    val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    // The card's top over the gesture area, and 8dp more so the last row clears it.
    val playerRoom = (if (bar) PlayerBarHeight else 64.dp) + PlayerGap + navigation
    val bottomPadding = if (state.hasCurrentSong) playerRoom + 8.dp else navigation + 16.dp
    val playerBar = remember(actions) { PlayerBarActions(actions.openQueue, actions.openLyrics) }
    val tabs = Tab.entries

    Box(Modifier.fillMaxSize().background(colors.ground)) {
        val covered = state.covered
        // Hidden from accessibility services while covered, so they only see the top screen.
        Box(Modifier.fillMaxSize().windowInsetsPadding(sides).then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            // First in the tree so keyboard focus moves dock, content, player; drawn last so its shadow
            // falls on the content.
            ComfortSideDock(
                items = tabs.map { TabItem(stringResource(it.labelRes), it.symbol) },
                selectedIndex = tabs.indexOf(state.currentTab),
                onSelect = { actions.selectTab(tabs[it]) },
                modifier = Modifier
                    .zIndex(1f)
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .windowInsetsPadding(systemNav.only(WindowInsetsSides.Bottom))
                    .padding(start = SideDockGap, top = SideDockGap, bottom = SideDockGap)
                    .fillMaxHeight(),
            )
            Box(Modifier.fillMaxSize().padding(start = SideDockContentStart, end = 8.dp)) {
                CompositionLocalProvider(LocalBottomPadding provides bottomPadding) {
                    slots.content(ComfortNavTransitions)
                }
            }
            AnimatedVisibility(
                visible = state.hasCurrentSong,
                modifier = Modifier.align(Alignment.BottomStart),
                enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(180)),
            ) {
                val fade = with(density) { PlayerFade.toPx() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = SideDockGap + SideDockWidth)
                        .background(Brush.verticalGradient(0f to Color.Transparent, 1f to colors.ground, startY = 0f, endY = fade))
                        .padding(top = PlayerFade, bottom = navigation + PlayerGap),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (bar) {
                        Box(Modifier.padding(start = 24.dp, end = 24.dp)) {
                            CompositionLocalProvider(LocalPlayerBar provides playerBar) { slots.miniPlayer() }
                        }
                    } else {
                        Box(
                            Modifier
                                .padding(horizontal = 24.dp)
                                .widthIn(max = MiniPlayerMaxWidthDp.dp)
                                .fillMaxWidth()
                                .floatingShadow(ComfortShapes.Floating)
                                .clip(ComfortShapes.Floating)
                                .background(colors.raised),
                        ) {
                            slots.miniPlayer()
                        }
                    }
                }
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

        // Above the overlays, so messages raised from Now Playing can be seen; above the player otherwise.
        ToastHost(
            state.snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = if (covered) 0.dp else SideDockContentStart)
                .offset {
                    val bottom = if (covered) {
                        systemNav.getBottom(this) + 32.dp.roundToPx()
                    } else {
                        (bottomPadding + 6.dp).roundToPx()
                    }
                    IntOffset(0, -bottom)
                },
        )
    }
}
