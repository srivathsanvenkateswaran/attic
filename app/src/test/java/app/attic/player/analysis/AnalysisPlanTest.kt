package app.attic.player.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisPlanTest {
    @Test
    fun aSongIsDoneOnlyOnceItsModelWorkLoudnessAndTransitionsAreAllSaved() {
        val all = setOf("a")
        val none = emptySet<String>()
        assertEquals(AnalysisNeed.Full, AnalysisPlan.need("a", finished = none, measured = none, transitioned = none))
        assertEquals(AnalysisNeed.Full, AnalysisPlan.need("a", finished = none, measured = all, transitioned = all))
        assertEquals(AnalysisNeed.Measure, AnalysisPlan.need("a", finished = all, measured = none, transitioned = all))
        assertEquals(AnalysisNeed.Measure, AnalysisPlan.need("a", finished = all, measured = all, transitioned = none))
        assertEquals(AnalysisNeed.Measure, AnalysisPlan.need("a", finished = all, measured = none, transitioned = none))
        assertNull(AnalysisPlan.need("a", finished = all, measured = all, transitioned = all))
    }

    @Test
    fun aSongMissingStylesLoudnessAndTransitionsGetsOneFullDecodeNotTwo() {
        // Analysed before styles, loudness and transitions were stored: the one full decode measures them all.
        val todo = AnalysisPlan.todo(
            listOf("old"),
            finished = emptySet(),
            analyzed = setOf("old"),
            measured = emptySet(),
            transitioned = emptySet(),
            recent = emptyMap(),
        )
        assertEquals(listOf("old" to AnalysisNeed.Full), todo)
    }

    @Test
    fun loudnessFirstThenNewSongsThenTheTransitionsBackfillThenStyles() {
        val todo = AnalysisPlan.todo(
            keys = listOf("styles", "transitions", "new", "loudness", "done"),
            finished = setOf("transitions", "loudness", "done"),
            analyzed = setOf("styles", "transitions", "loudness", "done"),
            measured = setOf("transitions", "done"),
            transitioned = setOf("loudness", "done"),
            recent = emptyMap(),
        )
        assertEquals(
            listOf(
                "loudness" to AnalysisNeed.Measure,
                "new" to AnalysisNeed.Full,
                "transitions" to AnalysisNeed.Measure,
                "styles" to AnalysisNeed.Full,
            ),
            todo,
        )
    }

    @Test
    fun aLibraryAnalysedBeforeTransitionsNeedsOnlyQuickDecodes() {
        val keys = listOf("a", "b", "c")
        val todo = AnalysisPlan.todo(keys, keys.toSet(), keys.toSet(), keys.toSet(), transitioned = emptySet(), recent = mapOf("b" to 0))
        assertEquals(listOf("b", "a", "c"), todo.map { it.first })
        assertEquals(setOf(AnalysisNeed.Measure), todo.map { it.second }.toSet())
    }

    @Test
    fun withinAGroupRecentlyPlayedSongsComeFirst() {
        val todo = AnalysisPlan.todo(
            keys = listOf("a", "b", "c"),
            finished = emptySet(),
            analyzed = emptySet(),
            measured = emptySet(),
            transitioned = emptySet(),
            recent = mapOf("c" to 0, "b" to 1),
        )
        assertEquals(listOf("c", "b", "a"), todo.map { it.first })
    }
}
