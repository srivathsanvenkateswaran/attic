package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsTest {
    @Test
    fun `parses stamps including centiseconds and repeated lines`() {
        val lines = LrcParser.parse("[00:27.68] Come up\n[01:02][01:30.5]Chorus\nno stamp here\n[ar:Coldplay]")
        assertEquals(listOf(27_680L, 62_000L, 90_500L), lines.map { it.timeMs })
        assertEquals(listOf("Come up", "Chorus", "Chorus"), lines.map { it.text })
    }

    @Test
    fun `finds the line being sung`() {
        val lines = listOf(LyricLine(1000, "a"), LyricLine(2000, "b"), LyricLine(3000, "c"))
        assertEquals(-1, LrcParser.lineAt(lines, 500))
        assertEquals(0, LrcParser.lineAt(lines, 1000))
        assertEquals(1, LrcParser.lineAt(lines, 2999))
        assertEquals(2, LrcParser.lineAt(lines, 99_000))
        assertEquals(-1, LrcParser.lineAt(emptyList(), 100))
    }

    private fun c(duration: Double, synced: String? = null, plain: String? = null) =
        LyricsCandidate("t", "a", duration, synced, plain, false)

    @Test
    fun `matcher rejects other songs by length and prefers synced`() {
        val plainClose = c(200.0, plain = "p")
        val syncedFar = c(208.0, synced = "s")
        val wrongSong = c(400.0, synced = "s")
        assertEquals(syncedFar, LyricsMatcher.best(listOf(plainClose, syncedFar, wrongSong), 200_000))
        assertNull(LyricsMatcher.best(listOf(wrongSong), 200_000))
        assertNull(LyricsMatcher.best(listOf(c(200.0)), 200_000))
    }
}
