package app.attic.player.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisProgressTest {
    @Test
    fun runningShowsHowFarItHasGot() {
        assertEquals("Analysing songs: 120 of 602", AnalysisProgress(120, 602, running = true).status)
    }

    @Test
    fun notRunningWithSongsLeftSaysItIsWaitingForACharger() {
        assertEquals(
            "120 of 602 songs analysed. The rest continue next time the phone charges.",
            AnalysisProgress(120, 602, running = false).status,
        )
    }

    @Test
    fun finishedSaysEverySongIsReady() {
        assertEquals("All 602 songs analysed.", AnalysisProgress(602, 602, running = false).status)
    }

    @Test
    fun theBarFollowsTheShareDone() {
        assertEquals(0.25f, AnalysisProgress(150, 600, running = true).fraction, 1e-4f)
    }

    @Test
    fun anEmptyLibraryDoesNotDivideByZero() {
        assertEquals(0f, AnalysisProgress(0, 0, running = false).fraction, 0f)
    }

    @Test
    fun theButtonOnlyAppearsWhenItWouldDoSomething() {
        assertEquals(true, AnalysisProgress(1, 2, running = false).canStart)
        assertEquals(false, AnalysisProgress(1, 2, running = true).canStart)
        assertEquals(false, AnalysisProgress(2, 2, running = false).canStart)
    }
}
