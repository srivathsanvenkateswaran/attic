package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class QueueOrderTest {
    @Test
    fun `shuffled rest leaves out the current song and keeps every other`() {
        val items = (0 until 50).map { "t$it" }
        val rest = QueueOrder.shuffledRest(items, 7, Random(1))
        assertFalse("t7" in rest)
        assertEquals(items.toSet() - "t7", rest.toSet())
        assertEquals(49, rest.size)
    }

    @Test
    fun `unshuffle restores base order`() {
        assertEquals(listOf("a", "b", "c"), QueueOrder.unshuffled(listOf("a", "b", "c"), listOf("c", "a", "b")))
    }

    @Test
    fun `unshuffle drops songs removed while shuffled and appends songs added`() {
        assertEquals(
            listOf("a", "c", "new"),
            QueueOrder.unshuffled(listOf("a", "b", "c"), listOf("new", "c", "a")),
        )
    }

    @Test
    fun `unshuffle keeps duplicates`() {
        assertEquals(
            listOf("a", "b", "a"),
            QueueOrder.unshuffled(listOf("a", "b"), listOf("b", "a", "a")),
        )
    }
}
