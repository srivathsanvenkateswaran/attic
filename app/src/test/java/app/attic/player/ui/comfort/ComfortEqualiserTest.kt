package app.attic.player.ui.comfort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The arithmetic under the Comfort equaliser's bands and its preset row. */
class ComfortEqualiserTest {
    @Test
    fun aDraggedGainSnapsToWholeDecibelsInsideTheRange() {
        assertEquals(3f, snapGain(2.6f), 0f)
        assertEquals(-2f, snapGain(-2.4f), 0f)
        assertEquals(12f, snapGain(40f), 0f)
        assertEquals(-12f, snapGain(-12.7f), 0f)
    }

    @Test
    fun aGainJustUnderZeroSnapsToPlainZero() {
        // Negative zero would pass for 0 dB in a comparison and still differ as a stored value.
        assertEquals(0f.toRawBits(), snapGain(-0.3f).toRawBits())
    }

    @Test
    fun landingOnZeroOrPassingItIsATick() {
        assertTrue(reachesZero(before = 1f, after = 0f))
        assertTrue(reachesZero(before = -2f, after = 0f))
        assertTrue(reachesZero(before = 2f, after = -1f))
        assertTrue(reachesZero(before = -3f, after = 4f))
    }

    @Test
    fun leavingZeroOrStayingOnOneSideIsNotATick() {
        assertFalse(reachesZero(before = 0f, after = 1f))
        assertFalse(reachesZero(before = 0f, after = -1f))
        assertFalse(reachesZero(before = 2f, after = 5f))
        assertFalse(reachesZero(before = -6f, after = -1f))
    }

    @Test
    fun aChipAlreadyInViewLeavesTheRowWhereItIs() {
        assertEquals(40, chipScroll(start = 100, width = 80, scroll = 40, viewport = 300, side = 16))
    }

    @Test
    fun aChipCutOffAtTheEndComesInWithTheMarginClear() {
        // The chip ends at 16 + 400 + 90 in the row; with 16 clear after it the viewport has to reach 522.
        assertEquals(222, chipScroll(start = 400, width = 90, scroll = 0, viewport = 300, side = 16))
    }

    @Test
    fun aChipCutOffAtTheStartComesBackToItsStart() {
        assertEquals(100, chipScroll(start = 100, width = 80, scroll = 260, viewport = 300, side = 16))
    }

    @Test
    fun aChipWiderThanTheRowShowsItsStart() {
        assertEquals(50, chipScroll(start = 50, width = 400, scroll = 0, viewport = 300, side = 16))
    }
}
