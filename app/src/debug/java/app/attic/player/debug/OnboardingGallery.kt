package app.attic.player.debug

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.attic.player.data.OnboardingStep
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortOnboarding
import app.attic.player.ui.expressive.ExpressiveOnboarding
import app.attic.player.ui.model.AccessState
import app.attic.player.ui.model.LibraryScan
import app.attic.player.ui.model.OnboardingActions
import app.attic.player.ui.model.OnboardingState

/**
 * Where the first-run gallery starts, from the intent: `step` 0 to 3 (Welcome, look, access, library),
 * `audio` and `notifications` (`notasked`, `granted`, `refused`), and `songs` (-1 still scanning,
 * 0 for the empty library, or a count).
 */
internal data class OnboardingSample(
    val step: OnboardingStep,
    val audio: AccessState,
    val notifications: AccessState,
    val scan: LibraryScan,
) {
    companion object {
        fun from(intent: Intent): OnboardingSample {
            fun access(name: String, default: AccessState) = when (intent.getStringExtra(name)) {
                "granted" -> AccessState.Granted
                "refused" -> AccessState.Refused
                "notasked" -> AccessState.NotAsked
                else -> default
            }
            val songs = intent.getIntExtra("songs", 127)
            return OnboardingSample(
                step = OnboardingStep.entries.getOrElse(intent.getIntExtra("step", 0)) { OnboardingStep.Welcome },
                audio = access("audio", AccessState.NotAsked),
                notifications = access("notifications", AccessState.NotAsked),
                scan = if (songs < 0) LibraryScan.Scanning else LibraryScan.Found(songs),
            )
        }
    }
}

/**
 * The first run with sample state and no app singletons: the steps, back and Continue move, the look
 * and theme switch live, and Allow grants at once. Nothing is saved and no permission is asked for.
 */
@Composable
internal fun OnboardingGallery(
    sample: OnboardingSample,
    uiStyle: UiStyle,
    themeMode: ThemeMode,
    onStyle: (UiStyle) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
) {
    var step by remember { mutableStateOf(sample.step) }
    var audio by remember { mutableStateOf(sample.audio) }
    var notifications by remember { mutableStateOf(sample.notifications) }
    var lyrics by remember { mutableStateOf(false) }
    val state = OnboardingState(step, uiStyle, themeMode, audio, notifications, sample.scan, lyrics)
    val actions = OnboardingActions(
        next = { if (state.canContinue) step.next()?.let { step = it } },
        back = { step.previous()?.let { step = it } },
        setUiStyle = onStyle,
        setThemeMode = onThemeMode,
        allowAudio = { audio = AccessState.Granted },
        openAppPermissions = { audio = AccessState.Granted },
        allowNotifications = { notifications = AccessState.Granted },
        setDownloadLyrics = { lyrics = it },
        openRadioSettings = { },
        finish = { },
    )
    BackHandler(enabled = step.previous() != null) { actions.back() }
    when (uiStyle) {
        UiStyle.Expressive -> ExpressiveOnboarding(state, actions)
        UiStyle.Comfort -> ComfortOnboarding(state, actions)
    }
}
