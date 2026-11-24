package app.attic.player.ui.model

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.OnboardingStep
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** Where a permission stands. [Refused] means the system prompt was answered no. */
enum class AccessState { NotAsked, Granted, Refused }

/** The first library scan, as the last step shows it. */
@Immutable
sealed interface LibraryScan {
    data object Scanning : LibraryScan
    data class Found(val count: Int) : LibraryScan
}

@Immutable
data class OnboardingState(
    val step: OnboardingStep,
    val uiStyle: UiStyle,
    val themeMode: ThemeMode,
    val audio: AccessState,
    val notifications: AccessState,
    val scan: LibraryScan,
    /** Whether "Start listening" also downloads lyrics for every song. Off until the user turns it on. */
    val downloadLyrics: Boolean,
) {
    /** 1-based, for "2 of 4". */
    val position: Int get() = step.ordinal + 1
    val canContinue: Boolean get() = step.canContinue(audio == AccessState.Granted)
}

class OnboardingActions(
    /** Continue to the next step. Does nothing on Access until audio access is granted. */
    val next: () -> Unit,
    /** Back to the previous step. Welcome has none; system back leaves the app there. */
    val back: () -> Unit,
    /** Switches style at once, as Settings does, so the first run redraws in it. */
    val setUiStyle: (UiStyle) -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    /** The system prompt for Music and audio. */
    val allowAudio: () -> Unit,
    /** The app's page in system settings, once the prompt was refused. */
    val openAppPermissions: () -> Unit,
    /** The system prompt for notifications, asked on its own so a no blocks nothing. */
    val allowNotifications: () -> Unit,
    val setDownloadLyrics: (Boolean) -> Unit,
    /** Ends the first run and opens Settings at the song radio rows. */
    val openRadioSettings: () -> Unit,
    /** "Start listening": ends the first run and goes Home. */
    val finish: () -> Unit,
)

@Composable
fun rememberOnboardingModel(): UiModel<OnboardingState, OnboardingActions> {
    val context = LocalContext.current
    val app = context.attic
    val scope = rememberCoroutineScope()

    // Saved, so rotation and process death come back to the same step and answers.
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
    var audioAsked by rememberSaveable { mutableStateOf(false) }
    var notificationsAsked by rememberSaveable { mutableStateOf(false) }
    var downloadLyrics by rememberSaveable { mutableStateOf(false) }

    fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    var hasAudio by remember { mutableStateOf(app.library.hasPermission()) }
    var hasNotifications by remember { mutableStateOf(granted(Manifest.permission.POST_NOTIFICATIONS)) }

    // Started undispatched so the repository reports the scan before the next frame, never "found 0".
    fun scan() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { app.library.refresh() }
    }

    val audioPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        audioAsked = true
        hasAudio = ok
        if (ok) scan()
    }
    val notificationsPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notificationsAsked = true
        hasNotifications = ok
    }

    // Covers access granted from system settings, and coming back after it.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val audio = app.library.hasPermission()
        if (audio != hasAudio) {
            hasAudio = audio
            if (audio) scan()
        }
        hasNotifications = granted(Manifest.permission.POST_NOTIFICATIONS)
    }

    val style by app.settings.uiStyle.collectAsStateWithLifecycle()
    val theme by app.settings.themeMode.collectAsStateWithLifecycle()
    val library by app.library.library.collectAsStateWithLifecycle()
    val scanning by app.library.scanning.collectAsStateWithLifecycle()

    val audio = when {
        hasAudio -> AccessState.Granted
        audioAsked -> AccessState.Refused
        else -> AccessState.NotAsked
    }
    val notifications = when {
        hasNotifications -> AccessState.Granted
        notificationsAsked -> AccessState.Refused
        else -> AccessState.NotAsked
    }
    val found = library
    val scan = if (!hasAudio || scanning || found == null) LibraryScan.Scanning else LibraryScan.Found(found.tracks.size)

    val state = OnboardingState(step, style, theme, audio, notifications, scan, downloadLyrics)
    val current by rememberUpdatedState(state)

    val actions = remember(audioPrompt, notificationsPrompt, context) {
        fun end(toSettings: Boolean) {
            val tracks = app.library.library.value?.tracks.orEmpty()
            if (current.downloadLyrics && tracks.isNotEmpty()) app.lyrics.downloadAll(tracks)
            if (toSettings) app.requestedSettings.value = true
            app.onboarding.finish()
        }
        OnboardingActions(
            next = {
                if (current.canContinue) {
                    val next = step.next()
                    if (next != null) step = next else end(toSettings = false)
                }
            },
            back = { step.previous()?.let { step = it } },
            setUiStyle = app.settings::setUiStyle,
            setThemeMode = app.settings::setThemeMode,
            allowAudio = { audioPrompt.launch(Manifest.permission.READ_MEDIA_AUDIO) },
            openAppPermissions = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            allowNotifications = { notificationsPrompt.launch(Manifest.permission.POST_NOTIFICATIONS) },
            setDownloadLyrics = { downloadLyrics = it },
            openRadioSettings = { end(toSettings = true) },
            finish = { end(toSettings = false) },
        )
    }
    return UiModel(state, actions)
}
