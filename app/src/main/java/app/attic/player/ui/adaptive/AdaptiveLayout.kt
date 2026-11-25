package app.attic.player.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.core.layout.WindowSizeClass

/*
 * Which layout each window and pane size gets, as docs/design/2026-11-25-tablet/SPEC.md sets out.
 * Everything here is plain arithmetic on dp, so it is unit tested without Compose; the composables
 * only read the window's size and hand it in.
 */

/** Material's width classes. Phones are compact; the tablet is expanded in either orientation. */
enum class WidthClass { Compact, Medium, Expanded }

/** Where the destinations' navigation sits. */
enum class NavPlacement { BottomBar, Rail }

/** How the playing song is shown while the destinations are on screen. */
enum class PlayerPlacement {
    /** The phone mini player above the bottom bar or inside the dock. */
    Docked,

    /** The mini player floating at the bottom of the content area, at most [MiniPlayerMaxWidthDp] wide. */
    Floating,

    /** The player bar along the bottom of the content area. */
    Bar,
}

/** The window's size and what follows from it. Read it with [LocalWindowLayout]. */
@Immutable
data class WindowLayout(
    val widthDp: Float,
    val heightDp: Float,
    /** Material's class for [widthDp]; [currentWindowLayout] takes it from the adaptive library. */
    val widthClass: WidthClass = widthClassOf(widthDp),
) {

    /** A window too short for a rail of tabs and a player bar keeps the phone shell, whatever its width. */
    private val wideShell: Boolean = widthClass != WidthClass.Compact && heightDp >= MinWideShellHeightDp

    val nav: NavPlacement = if (wideShell) NavPlacement.Rail else NavPlacement.BottomBar

    val player: PlayerPlacement = when {
        !wideShell -> PlayerPlacement.Docked
        widthClass == WidthClass.Expanded -> PlayerPlacement.Bar
        else -> PlayerPlacement.Floating
    }

    val landscape: Boolean = widthDp > heightDp

    /** Now Playing and full-screen lyrics put the player beside the queue or the lyrics. */
    val playerSideBySide: Boolean = widthClass == WidthClass.Expanded && landscape

    /** The Library shows the item it opened beside its list: the Pad 6 sideways (1152 dp) and wider. */
    val libraryListDetail: Boolean = wideShell && widthClass == WidthClass.Expanded && landscape

    companion object {
        /** A phone-sized window, the default before the window has been measured. */
        val Phone = WindowLayout(411f, 914f)
    }
}

fun widthClassOf(widthDp: Float): WidthClass = when {
    widthDp >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND -> WidthClass.Expanded
    widthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND -> WidthClass.Medium
    else -> WidthClass.Compact
}

/** How an album, artist, playlist, folder, Favourites or genre lays itself out in the width it is given. */
enum class CollectionLayout {
    /** The phone layout: header stacked over the songs. */
    Stacked,

    /** One column, with the header as a row: cover at the start, title and buttons beside it. */
    HeaderRow,

    /** The header in a fixed start pane, the songs scrolling in the end pane. */
    TwoPane,
}

fun collectionLayout(paneWidthDp: Float): CollectionLayout = when {
    paneWidthDp >= CollectionTwoPaneMinDp -> CollectionLayout.TwoPane
    paneWidthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND -> CollectionLayout.HeaderRow
    else -> CollectionLayout.Stacked
}

/** The width of a collection's header pane in [CollectionLayout.TwoPane]. */
fun collectionHeaderPaneDp(paneWidthDp: Float): Float = if (paneWidthDp >= 1000f) 360f else 320f

/**
 * How many columns of cells at least [minCellDp] wide, [gapDp] apart, fit in [availableDp]. At least
 * one. Matches Compose's `GridCells.Adaptive`, so a grid and a hand-built row of cells agree.
 */
fun adaptiveColumns(availableDp: Float, minCellDp: Float, gapDp: Float = 0f): Int =
    maxOf(1, ((availableDp + gapDp) / (minCellDp + gapDp)).toInt())

/** Phones stay in portrait; anything whose smaller side is 600 dp or more turns freely. */
fun locksToPortrait(smallestDisplaySideDp: Float): Boolean = smallestDisplaySideDp < WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND

/** A column of text, rows or settings is never wider than this. */
const val ReadableWidthDp = 720f

/** Material's bottom sheet maximum width. */
const val SheetMaxWidthDp = 640f

/** Material's dialog maximum width. */
const val DialogMaxWidthDp = 560f

/** The floating mini player's widest, and Now Playing's column when it isn't side by side. */
const val MiniPlayerMaxWidthDp = 560f

/** Below this height the phone shell stays, so a short landscape window keeps its bottom bar. */
const val MinWideShellHeightDp = 480f

/** The Library's list pane when it shows list and detail. */
const val LibraryListPaneDp = 360f

/** A collection pane this wide puts its header beside the songs. */
const val CollectionTwoPaneMinDp = 760f

/** The window's [WindowLayout], provided once at the root. Phones get [WindowLayout.Phone]-like values. */
val LocalWindowLayout = compositionLocalOf { WindowLayout.Phone }

/**
 * The current window's layout. The width class comes from Material's adaptive info; the exact size
 * (for orientation and the list-and-detail width) from the window itself. Both change only when the
 * window does (rotation, split screen, a floating window resized), so a phone composes this once.
 */
@Composable
fun currentWindowLayout(): WindowLayout {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(sizeClass, size, density) {
        val widthClass = when {
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WidthClass.Expanded
            sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WidthClass.Medium
            else -> WidthClass.Compact
        }
        if (size.width == 0 || size.height == 0) {
            WindowLayout.Phone
        } else {
            WindowLayout(size.width / density.density, size.height / density.density, widthClass)
        }
    }
}
