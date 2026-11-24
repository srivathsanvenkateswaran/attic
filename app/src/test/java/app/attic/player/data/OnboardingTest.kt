package app.attic.player.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRulesTest {
    @Test
    fun freshInstallIsNotOnboarded() {
        assertFalse(OnboardingRules.alreadyOnboarded(saved = null, settingsHadValues = false, hasUserData = false))
    }

    @Test
    fun dataInTheDatabaseCountsWithoutSettings() {
        // The owner's phone: years of plays and likes, no settings.xml.
        assertTrue(OnboardingRules.alreadyOnboarded(saved = null, settingsHadValues = false, hasUserData = true))
    }

    @Test
    fun anySettingCountsWithoutData() {
        assertTrue(OnboardingRules.alreadyOnboarded(saved = null, settingsHadValues = true, hasUserData = false))
    }

    @Test
    fun savedDecisionWins() {
        // Picking a look mid-way writes settings; an unfinished first run must still resume.
        assertFalse(OnboardingRules.alreadyOnboarded(saved = false, settingsHadValues = true, hasUserData = true))
        assertTrue(OnboardingRules.alreadyOnboarded(saved = true, settingsHadValues = false, hasUserData = false))
    }
}

class OnboardingGateTest {
    private class Store(var value: Boolean? = null) {
        var dataChecks = 0
    }

    private fun gate(store: Store, settingsHadValues: Boolean = false, hasUserData: Boolean = false) = OnboardingGate(
        saved = { store.value },
        settingsHadValues = { settingsHadValues },
        hasUserData = {
            store.dataChecks++
            hasUserData
        },
        save = { store.value = it },
    )

    @Test
    fun freshInstallSavesThatTheFirstRunIsDue() = runBlocking {
        val store = Store()
        val gate = gate(store)
        assertNull(gate.done.value)
        assertFalse(gate.resolve())
        assertEquals(false, store.value)
        assertEquals(false, gate.done.value)
    }

    @Test
    fun existingInstallIsMarkedDoneAndSkips() = runBlocking {
        val store = Store()
        assertTrue(gate(store, hasUserData = true).resolve())
        assertEquals(true, store.value)
    }

    @Test
    fun interruptedFirstRunResumesAfterSettingsWereWritten() = runBlocking {
        val store = Store(value = false)
        assertFalse(gate(store, settingsHadValues = true, hasUserData = true).resolve())
        assertEquals(0, store.dataChecks)
    }

    @Test
    fun finishSavesDone() = runBlocking {
        val store = Store()
        val gate = gate(store)
        gate.resolve()
        gate.finish()
        assertEquals(true, store.value)
        assertEquals(true, gate.done.value)
        // The next launch reads the saved answer and never looks at the database.
        val next = gate(store)
        assertTrue(next.resolve())
        assertEquals(1, store.dataChecks)
    }
}

class OnboardingStepTest {
    @Test
    fun stepsRunInOrder() {
        assertEquals(OnboardingStep.Look, OnboardingStep.Welcome.next())
        assertEquals(OnboardingStep.Access, OnboardingStep.Look.next())
        assertEquals(OnboardingStep.Library, OnboardingStep.Access.next())
        assertNull(OnboardingStep.Library.next())
        assertEquals(4, OnboardingStep.count)
    }

    @Test
    fun backGoesToThePreviousStepAndLeavesFromWelcome() {
        assertNull(OnboardingStep.Welcome.previous())
        assertEquals(OnboardingStep.Welcome, OnboardingStep.Look.previous())
        assertEquals(OnboardingStep.Look, OnboardingStep.Access.previous())
        assertEquals(OnboardingStep.Access, OnboardingStep.Library.previous())
    }

    @Test
    fun onlyAccessWaitsForAudio() {
        assertFalse(OnboardingStep.Access.canContinue(audioGranted = false))
        assertTrue(OnboardingStep.Access.canContinue(audioGranted = true))
        assertTrue(OnboardingStep.Welcome.canContinue(audioGranted = false))
        assertTrue(OnboardingStep.Look.canContinue(audioGranted = false))
        assertTrue(OnboardingStep.Library.canContinue(audioGranted = false))
    }
}
