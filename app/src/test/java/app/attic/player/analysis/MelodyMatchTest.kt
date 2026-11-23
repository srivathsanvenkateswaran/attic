package app.attic.player.analysis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class MelodyMatchTest {
    /** A random walk through pitch classes, held for a few frames per note, like a melody over time. */
    private fun melody(seed: Long, notes: Int = 200): List<Int> {
        val r = Random(seed)
        var pitch = r.nextInt(12)
        return List(notes) {
            pitch = (pitch + listOf(-2, -1, 1, 2, 5, 7)[r.nextInt(6)] + 12) % 12
            pitch
        }.flatMap { p -> List(3 + r.nextInt(3)) { p } }
    }

    private fun chroma(pitches: List<Int>, seed: Long = 1): ByteArray {
        val r = Random(seed)
        val out = ByteArray(pitches.size * 12)
        pitches.forEachIndexed { frame, p ->
            for (c in 0 until 12) {
                val level = if (c == p) 0.95 else if (c == (p + 7) % 12) 0.25 else 0.06 * r.nextDouble()
                out[frame * 12 + c] = (level * 255).toInt().toByte()
            }
        }
        return out
    }

    @Test
    fun identicalMelodiesMatch() {
        val a = chroma(melody(1))
        assertTrue(MelodyMatch.score(a, a) > MelodyMatch.SAME_SONG)
    }

    @Test
    fun transposedMelodyMatches() {
        val notes = melody(2)
        assertTrue(MelodyMatch.score(chroma(notes), chroma(notes.map { (it + 3) % 12 }, 9)) > MelodyMatch.SAME_SONG)
    }

    @Test
    fun aSlightlyFasterEditStillMatches() {
        // Drop every 20th frame, as a video edit running a little quicker would.
        val notes = melody(3)
        val faster = notes.filterIndexed { i, _ -> i % 20 != 0 }
        assertTrue(MelodyMatch.score(chroma(notes), chroma(faster, 5)) > MelodyMatch.SAME_SONG)
    }

    @Test
    fun unrelatedMelodiesDoNotMatch() {
        assertFalse(MelodyMatch.score(chroma(melody(4)), chroma(melody(5))) > MelodyMatch.SAME_SONG)
    }

    @Test
    fun tooShortAudioNeverMatches() {
        assertFalse(MelodyMatch.score(ByteArray(0), chroma(melody(6))) > MelodyMatch.SAME_SONG)
    }
}
