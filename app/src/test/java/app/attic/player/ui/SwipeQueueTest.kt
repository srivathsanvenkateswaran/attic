package app.attic.player.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeQueueTest {
    private val width = 1000
    private val density = 2.5f

    @Test
    fun followsTheFingerUpToTheCommitPoint() {
        assertEquals(120f, SwipeQueue.drag(0f, 120f, width), 0.01f)
        assertEquals(300f, SwipeQueue.drag(200f, 100f, width), 0.01f)
    }

    @Test
    fun resistsPastTheCommitPoint() {
        // 100px of finger past 300 moves the row 35px.
        assertEquals(335f, SwipeQueue.drag(300f, 100f, width), 0.01f)
        // Resistance is undone on the way back: from 335, 100px back reaches 300 again.
        assertEquals(300f, SwipeQueue.drag(335f, -100f, width), 0.01f)
    }

    @Test
    fun stopsAtHalfTheWidth() {
        assertEquals(500f, SwipeQueue.drag(0f, 5_000f, width), 0.01f)
    }

    @Test
    fun neverMovesTowardsTheStart() {
        assertEquals(0f, SwipeQueue.drag(0f, -80f, width), 0f)
        assertEquals(0f, SwipeQueue.drag(50f, -80f, width), 0f)
    }

    @Test
    fun aRowNotYetMeasuredDoesNotMove() {
        assertEquals(0f, SwipeQueue.drag(0f, 100f, 0), 0f)
        assertFalse(SwipeQueue.commits(0f, 0, 10_000f, density))
    }

    @Test
    fun lettingGoPastThirtyPercentQueues() {
        assertTrue(SwipeQueue.armed(300f, width))
        assertFalse(SwipeQueue.armed(299f, width))
        assertTrue(SwipeQueue.commits(300f, width, 0f, density))
        assertTrue(SwipeQueue.commits(300f, width, -5_000f, density))
        assertFalse(SwipeQueue.commits(299f, width, 0f, density))
    }

    @Test
    fun aFlickQueuesFromAShorterSwipe() {
        val fast = SwipeQueue.FLING_DP_PER_SECOND * density
        assertTrue(SwipeQueue.commits(120f, width, fast, density))
        assertFalse(SwipeQueue.commits(119f, width, fast, density))
        assertFalse(SwipeQueue.commits(200f, width, fast - 1, density))
        // A flick back towards the start never queues a short swipe.
        assertFalse(SwipeQueue.commits(200f, width, -fast, density))
    }

    @Test
    fun progressRunsToTheCommitPoint() {
        assertEquals(0f, SwipeQueue.progress(0f, width), 0f)
        assertEquals(0.5f, SwipeQueue.progress(150f, width), 0.001f)
        assertEquals(1f, SwipeQueue.progress(450f, width), 0f)
    }
}
