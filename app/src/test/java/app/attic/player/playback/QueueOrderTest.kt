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

    private val ext = app.attic.player.data.ExternalTrack.keyFor("content://downloads/document/raw%3A%2Fsong.mp3")

    @Test
    fun `saving leaves the queue alone when nothing came from outside`() {
        val saved = QueueOrder.withoutExternal(listOf("a", "b", "c"), 1, 5_000)!!
        assertEquals(listOf("a", "b", "c"), saved.keys)
        assertEquals(1, saved.index)
        assertEquals(5_000L, saved.positionMs)
    }

    @Test
    fun `saving drops an outside song and keeps the place in the rest`() {
        val saved = QueueOrder.withoutExternal(listOf("a", ext, "b", "c"), 2, 5_000)!!
        assertEquals(listOf("a", "b", "c"), saved.keys)
        assertEquals(1, saved.index)
        assertEquals(5_000L, saved.positionMs)
    }

    @Test
    fun `while an outside song plays the saved place is the next song from its start`() {
        val saved = QueueOrder.withoutExternal(listOf("a", ext, "b"), 1, 5_000)!!
        assertEquals(listOf("a", "b"), saved.keys)
        assertEquals(1, saved.index)
        assertEquals(0L, saved.positionMs)
    }

    @Test
    fun `an outside song at the end saves the place on the song before it`() {
        val saved = QueueOrder.withoutExternal(listOf("a", "b", ext), 2, 5_000)!!
        assertEquals(1, saved.index)
        assertEquals(0L, saved.positionMs)
    }

    @Test
    fun `a queue of only outside songs is not saved`() {
        assertEquals(null, QueueOrder.withoutExternal(listOf(ext), 0, 0))
        assertEquals(null, QueueOrder.withoutExternal(emptyList(), -1, 0))
    }
}
