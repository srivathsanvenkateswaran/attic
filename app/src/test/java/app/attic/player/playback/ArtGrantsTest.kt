package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtGrantsTest {
    @Test
    fun `only other identified packages get grants`() {
        assertTrue(ArtGrants.needsGrant("com.google.android.projection.gearhead", "app.attic.player"))
        assertTrue(ArtGrants.needsGrant("com.android.systemui", "app.attic.player"))
        assertFalse(ArtGrants.needsGrant("app.attic.player", "app.attic.player"))
        assertFalse(ArtGrants.needsGrant(ArtGrants.LEGACY_CONTROLLER, "app.attic.player"))
        assertFalse(ArtGrants.needsGrant("", "app.attic.player"))
    }

    @Test
    fun `the queue window is the playing song and the ones after it, within the queue`() {
        assertEquals(3..13, ArtGrants.queueWindow(currentIndex = 3, count = 100, ahead = 10))
        assertEquals(95..99, ArtGrants.queueWindow(currentIndex = 95, count = 100, ahead = 10))
        assertEquals(0..0, ArtGrants.queueWindow(currentIndex = 0, count = 1, ahead = 10))
        assertTrue(ArtGrants.queueWindow(currentIndex = -1, count = 0, ahead = 10).isEmpty())
    }
}
