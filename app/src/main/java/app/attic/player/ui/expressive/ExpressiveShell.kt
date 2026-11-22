package app.attic.player.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.NavTransitions
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.isTabRoute
import app.attic.player.ui.model.MiniPlayerActions
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.rememberPosition
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Moving between destinations, after Material's patterns: a tab root fades through (fades in while
 * growing from 92%), a detail screen comes in on the shared X axis (slides a short way in from the end
 * while fading), and going back reverses it.
 */
val ExpressiveNavTransitions = NavTransitions(
    enter = {
        if (isTabRoute(targetState.destination.route)) {
            fadeIn(ExpressiveMotion.defaultEffects()) + scaleIn(ExpressiveMotion.defaultSpatial(), initialScale = 0.92f)
        } else {
            slideInHorizontally(ExpressiveMotion.defaultSpatial()) { it / 10 } + fadeIn(ExpressiveMotion.defaultEffects())
        }
    },
    exit = { fadeOut(ExpressiveMotion.fastEffects()) },
    popEnter = { fadeIn(ExpressiveMotion.defaultEffects()) },
    popExit = {
        if (isTabRoute(initialState.destination.route)) {
            fadeOut(ExpressiveMotion.fastEffects())
        } else {
            slideOutHorizontally(ExpressiveMotion.defaultSpatial()) { it / 10 } + fadeOut(ExpressiveMotion.fastEffects())
        }
    },
)

/** Full-screen overlays rise without overshooting, so no gap opens under them. */
private fun <T> overlaySpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)

private val MiniPlayerHeight = 64.dp
/** Between the mini player and the navigation bar, or the keyboard. */
private val MiniPlayerGap = 8.dp
/** The navigation bar's own height, before the gesture area under it. Material's short bar is 64dp. */
private val NavBarHeight = 64.dp

/**
 * The Expressive shell (sheet 01 and the dock on every sheet): destinations fill the screen and scroll
 * under the dock, which is the navigation bar on surface container with the mini player floating 8dp
 * above it, 12dp in from the edges. While the keyboard is up the mini player rides above it and the
 * bar stays under it. Now Playing, the queue and lyrics rise over everything; the snackbar sits 16dp
 * above the dock, or low while an overlay is open, and draws above the overlays.
 *
 * Destinations get the room the dock takes as [LocalBottomPadding]: the dock's height plus 24dp, the
 * kit's `.page.clear-dock`. A FAB sits 12dp above the dock, at `LocalBottomPadding.current - 12.dp`.
 */
@Composable
fun ExpressiveShell(state: ShellState, actions: ShellActions, slots: ShellSlots) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val systemNav = WindowInsets.navigationBars
    // The bar's measured height, gesture area included. Until it has been laid out, Material's height plus the inset.
    var navHeightPx by remember { mutableIntStateOf(0) }
    val navHeight: Dp = with(density) {
        if (navHeightPx > 0) navHeightPx.toDp() else NavBarHeight + systemNav.getBottom(this).toDp()
    }
    val miniSpace = if (state.hasCurrentSong) MiniPlayerHeight + MiniPlayerGap else 0.dp

    Box(Modifier.fillMaxSize().background(colors.surface)) {
        val covered = state.covered
        // Hidden from accessibility services while covered, so they only see the top screen.
        Box(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
            CompositionLocalProvider(LocalBottomPadding provides navHeight + miniSpace + 24.dp) {
                slots.content(ExpressiveNavTransitions)
            }

            AnimatedVisibility(
                visible = state.hasCurrentSong,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, -max(navHeightPx, ime.getBottom(this))) },
                enter = slideInVertically(ExpressiveMotion.defaultSpatial()) { it } + fadeIn(ExpressiveMotion.defaultEffects()),
                exit = slideOutVertically(ExpressiveMotion.defaultSpatial()) { it } + fadeOut(ExpressiveMotion.fastEffects()),
            ) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = MiniPlayerGap)) { slots.miniPlayer() }
            }

            val tabs = Tab.entries
            ExpressiveNavigationBar(
                items = tabs.map { NavItem(stringResource(it.labelRes), it.symbol) },
                selectedIndex = tabs.indexOf(state.currentTab),
                onSelect = { actions.selectTab(tabs[it]) },
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { navHeightPx = it.height },
            )
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
        val miniSpacePx = with(density) { miniSpace.roundToPx() }
        SnackbarHost(
            state.snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .offset {
                    val bottom = if (covered) {
                        systemNav.getBottom(this) + 24.dp.roundToPx()
                    } else {
                        max(navHeightPx, ime.getBottom(this)) + miniSpacePx + 16.dp.roundToPx()
                    }
                    IntOffset(0, -bottom)
                },
        ) { data ->
            // Over the player, queue or lyrics the message takes the inverse roles of the cover's scheme (07 J).
            val cover = state.player.current?.mediaId
            if (covered && cover != null) FromCover(cover) { ExpressiveSnackbar(data) } else ExpressiveSnackbar(data)
        }
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
 * The mini player (`.mini`, sheet 05 frame E): a 64dp pill on the cover's surface container high,
 * holding the cover, title over artist, the heart and the play control with its progress ring. Tap
 * opens Now Playing. Swiping the title sideways follows the finger; past 28% of its width, left skips
 * to the next song and right to the previous. A long artist line scrolls after 2.5 s.
 */
@Composable
fun ExpressiveMiniPlayer(state: MiniPlayerState, actions: MiniPlayerActions) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified)
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 500)
    val progress = if (track.durationMs > 0) (position.toFloat() / track.durationMs).coerceIn(0f, 1f) else 0f
    val swipe = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    FromArt(seed) {
        val colors = MaterialTheme.colorScheme
        Row(
            Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .clip(ExpressiveShapes.Pill)
                .background(colors.surfaceContainerHigh)
                .clickable(interactionSource = null, indication = ripple(), onClick = actions.open)
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(Art.of(track.mediaId), size = 48.dp, shape = RoundedCornerShape(ExpressiveShapes.Large))
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .offset { IntOffset(swipe.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val moved = swipe.value
                                    if (abs(moved) > size.width * SWIPE_THRESHOLD) {
                                        if (moved < 0) actions.next() else actions.previous()
                                        swipe.snapTo(-moved / 2)
                                    }
                                    swipe.animateTo(0f, ExpressiveMotion.defaultSpatial())
                                }
                            },
                            onDragCancel = { scope.launch { swipe.animateTo(0f, ExpressiveMotion.defaultSpatial()) } },
                        ) { change, amount ->
                            change.consume()
                            scope.launch { swipe.snapTo(swipe.value + amount) }
                        }
                    },
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(initialDelayMillis = MARQUEE_DELAY_MS),
                )
            }
            HeartButton(state.liked, actions.toggleLike)
            PlayButton(state.isPlaying, actions.togglePlayPause, progress = { progress })
        }
    }
}

private const val SWIPE_THRESHOLD = 0.28f
private const val MARQUEE_DELAY_MS = 2500
