package app.attic.player.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisPlanTest {
    @Test
    fun aSongIsDoneOnlyOnceItsModelWorkAndLoudnessAreBothSaved() {
        assertEquals(AnalysisNeed.Full, AnalysisPlan.need("a", finished = emptySet(), measured = emptySet()))
        assertEquals(AnalysisNeed.Full, AnalysisPlan.need("a", finished = emptySet(), measured = setOf("a")))
        assertEquals(AnalysisNeed.Loudness, AnalysisPlan.need("a", finished = setOf("a"), measured = emptySet()))
        assertNull(AnalysisPlan.need("a", finished = setOf("a"), measured = setOf("a")))
    }

    @Test
    fun aSongMissingStylesAndLoudnessGetsOneFullDecodeNotTwo() {
        // Analysed before both styles and loudness were stored: the one full decode measures loudness too.
        val todo = AnalysisPlan.todo(listOf("old"), finished = emptySet(), analyzed = setOf("old"), measured = emptySet(), recent = emptyMap())
        assertEquals(listOf("old" to AnalysisNeed.Full), todo)
    }

    @Test
    fun loudnessOnlyGoesFirstThenNewSongsThenStylesBackfill() {
        val todo = AnalysisPlan.todo(
            keys = listOf("backfill", "new", "loudness", "done"),
            finished = setOf("loudness", "done"),
            analyzed = setOf("backfill", "loudness", "done"),
            measured = setOf("done"),
            recent = emptyMap(),
        )
        assertEquals(
            listOf("loudness" to AnalysisNeed.Loudness, "new" to AnalysisNeed.Full, "backfill" to AnalysisNeed.Full),
            todo,
        )
    }

    @Test
    fun withinAGroupRecentlyPlayedSongsComeFirst() {
        val todo = AnalysisPlan.todo(
            keys = listOf("a", "b", "c"),
            finished = emptySet(),
            analyzed = emptySet(),
            measured = emptySet(),
            recent = mapOf("c" to 0, "b" to 1),
        )
        assertEquals(listOf("c", "b", "a"), todo.map { it.first })
    }
}
