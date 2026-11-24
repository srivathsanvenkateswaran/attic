package app.attic.player.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The first-run steps, in order. */
enum class OnboardingStep {
    Welcome,
    Look,
    Access,
    Library;

    /** The step Continue leads to, or null after the last one, where the first run ends. */
    fun next(): OnboardingStep? = entries.getOrNull(ordinal + 1)

    /** The step back leads to, or null on Welcome, where back leaves the app. */
    fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

    /** Continue from Access needs audio access; every other step can always go on. */
    fun canContinue(audioGranted: Boolean): Boolean = this != Access || audioGranted

    companion object {
        val count = entries.size
    }
}

object OnboardingRules {
    /**
     * Whether the first run is over. A saved decision always wins, so a first run interrupted after
     * the look was picked (which writes settings) still resumes. Without one, any sign of an
     * earlier install counts as onboarded: a value in settings.xml, or anything saved in the database.
     * An install from before onboarding existed may have data and no settings.xml at all.
     */
    fun alreadyOnboarded(saved: Boolean?, settingsHadValues: Boolean, hasUserData: Boolean): Boolean =
        saved ?: (settingsHadValues || hasUserData)
}

/**
 * Decides once per install whether to show the first run, and remembers the answer in settings.
 * [done] is null until the decision is made.
 */
class OnboardingGate(
    private val saved: () -> Boolean?,
    private val settingsHadValues: () -> Boolean,
    private val hasUserData: suspend () -> Boolean,
    private val save: (Boolean) -> Unit,
) {
    private val _done = MutableStateFlow<Boolean?>(null)
    val done: StateFlow<Boolean?> = _done.asStateFlow()

    fun start(scope: CoroutineScope) {
        scope.launch { resolve() }
    }

    /** Reads the saved decision, or makes it and saves it, so the database is only checked on the first launch. */
    suspend fun resolve(): Boolean {
        _done.value?.let { return it }
        val stored = saved()
        val result = stored ?: OnboardingRules.alreadyOnboarded(null, settingsHadValues(), hasUserData()).also(save)
        _done.value = result
        return result
    }

    fun finish() {
        save(true)
        _done.value = true
    }
}
