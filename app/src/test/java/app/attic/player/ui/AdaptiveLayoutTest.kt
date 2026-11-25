package app.attic.player.ui

import app.attic.player.ui.adaptive.CollectionLayout
import app.attic.player.ui.adaptive.NavPlacement
import app.attic.player.ui.adaptive.PlayerPlacement
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.adaptive.centredInsetDp
import app.attic.player.ui.adaptive.collectionHeaderPaneDp
import app.attic.player.ui.adaptive.collectionLayout
import app.attic.player.ui.adaptive.libraryDetailFits
import app.attic.player.ui.adaptive.locksToPortrait
import app.attic.player.ui.adaptive.widthClassOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    private val phone = WindowLayout(411f, 914f)

    // The Xiaomi Pad 6 at its stock 400 dpi, and at a smaller display size.
    private val padLandscape = WindowLayout(1152f, 720f)
    private val padPortrait = WindowLayout(720f, 1152f)
    private val padHalf = WindowLayout(570f, 720f)
    private val largeLandscape = WindowLayout(1440f, 900f)
    private val largePortrait = WindowLayout(900f, 1440f)

    @Test
    fun widthClassesFollowMaterialBreakpoints() {
        assertEquals(WidthClass.Compact, widthClassOf(599.9f))
        assertEquals(WidthClass.Medium, widthClassOf(600f))
        assertEquals(WidthClass.Medium, widthClassOf(839f))
        assertEquals(WidthClass.Expanded, widthClassOf(840f))
        assertEquals(WidthClass.Expanded, widthClassOf(1440f))
    }

    @Test
    fun phoneKeepsTheBottomBarAndDockedMiniPlayer() {
        assertEquals(NavPlacement.BottomBar, phone.nav)
        assertEquals(PlayerPlacement.Docked, phone.player)
        assertFalse(phone.playerSideBySide)
        assertFalse(phone.libraryListDetail)
    }

    @Test
    fun padLandscapeGetsRailPlayerBarSideBySidePlayerAndListDetail() {
        assertEquals(NavPlacement.Rail, padLandscape.nav)
        assertEquals(PlayerPlacement.Bar, padLandscape.player)
        assertTrue(padLandscape.playerSideBySide)
        assertTrue(padLandscape.libraryListDetail)
    }

    @Test
    fun padPortraitIsMediumWithRailFloatingMiniPlayerAndStackedPlayer() {
        assertEquals(WidthClass.Medium, padPortrait.widthClass)
        assertEquals(NavPlacement.Rail, padPortrait.nav)
        assertEquals(PlayerPlacement.Floating, padPortrait.player)
        assertFalse(padPortrait.playerSideBySide)
        assertFalse(padPortrait.libraryListDetail)
    }

    @Test
    fun splitScreenHalfOfThePadIsAPhone() {
        assertEquals(WidthClass.Compact, padHalf.widthClass)
        assertEquals(NavPlacement.BottomBar, padHalf.nav)
        assertEquals(PlayerPlacement.Docked, padHalf.player)
    }

    @Test
    fun largerTabletsFollowOrientation() {
        assertTrue(largeLandscape.libraryListDetail)
        assertTrue(largeLandscape.playerSideBySide)
        assertEquals(PlayerPlacement.Bar, largePortrait.player)
        assertFalse(largePortrait.libraryListDetail)
        assertFalse(largePortrait.playerSideBySide)
    }

    @Test
    fun libraryDetailNeedsRoomForItsOwnLayout() {
        // The Pad sideways: 1152 less Expressive's 96 dp rail, or Comfort's dock and margins.
        assertTrue(libraryDetailFits(1056f))
        assertTrue(libraryDetailFits(1036f))
        assertTrue(libraryDetailFits(1000f))
        // A 7-inch tablet sideways: the detail would be under 600 dp, the phone's screen with its back button.
        assertFalse(libraryDetailFits(999f))
        assertFalse(libraryDetailFits(864f))
        assertFalse(libraryDetailFits(928f))
    }

    @Test
    fun shortWideWindowKeepsThePhoneShell() {
        // A phone held sideways in a floating window, or a short strip of split screen.
        val short = WindowLayout(900f, 400f)
        assertEquals(NavPlacement.BottomBar, short.nav)
        assertEquals(PlayerPlacement.Docked, short.player)
        assertFalse(short.libraryListDetail)
    }

    @Test
    fun widthClassFromTheAdaptiveLibraryWins() {
        // currentWindowLayout passes Material's own class; the derived placements follow it.
        val layout = WindowLayout(845f, 900f, WidthClass.Medium)
        assertEquals(PlayerPlacement.Floating, layout.player)
        assertFalse(layout.playerSideBySide)
    }

    @Test
    fun collectionsPickTheirLayoutFromTheirPaneWidth() {
        assertEquals(CollectionLayout.Stacked, collectionLayout(411f))
        assertEquals(CollectionLayout.HeaderRow, collectionLayout(604f))
        assertEquals(CollectionLayout.HeaderRow, collectionLayout(759f))
        // The Pad upright after the rail, and the detail pane beside the Library list on the Pad sideways.
        assertEquals(CollectionLayout.HeaderRow, collectionLayout(624f))
        assertEquals(CollectionLayout.HeaderRow, collectionLayout(696f))
        // The Pad sideways, full width after the rail; the detail pane at 1440 dp.
        assertEquals(CollectionLayout.TwoPane, collectionLayout(1056f))
        assertEquals(CollectionLayout.TwoPane, collectionLayout(984f))
        assertEquals(320f, collectionHeaderPaneDp(984f))
        assertEquals(360f, collectionHeaderPaneDp(1056f))
    }

    @Test
    fun adaptiveColumnsMatchComposeGridCells() {
        assertEquals(1, adaptiveColumns(100f, 160f))
        assertEquals(2, adaptiveColumns(379f, 160f, 16f))
        assertEquals(2, adaptiveColumns(352f, 160f, 16f))
        assertEquals(3, adaptiveColumns(528f, 160f, 16f))
        assertEquals(7, adaptiveColumns(1296f, 160f, 16f))
    }

    @Test
    fun onlyPhonesLockToPortrait() {
        assertTrue(locksToPortrait(411f))
        assertTrue(locksToPortrait(599f))
        assertFalse(locksToPortrait(600f))
        assertFalse(locksToPortrait(900f))
    }

    @Test
    fun columnsCentreAtTheReadableWidth() {
        // Settings on the Pad sideways: 1056 dp beside the rail leaves 168 dp each side of a 720 dp column.
        assertEquals(168f, centredInsetDp(1056f))
        // Narrower than the column: no inset beyond the margin asked for.
        assertEquals(24f, centredInsetDp(624f, minDp = 24f))
        assertEquals(0f, centredInsetDp(411f))
    }
}
