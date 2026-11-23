package app.attic.player.ui.model

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.Library
import kotlinx.coroutines.launch

/** What the app can show before the main shell: asking for audio access, then waiting for the first scan. */
@Immutable
sealed interface RootState {
    /** [askedBefore] means the system dialog was already declined, so only app settings can grant it. */
    data class NeedsAccess(val askedBefore: Boolean) : RootState
    data object Loading : RootState
    data class Ready(val library: Library) : RootState
}

class RootActions(
    val allowAccess: () -> Unit,
    val openAppPermissions: () -> Unit,
)

@Composable
fun rememberRootModel(): UiModel<RootState, RootActions> {
    val context = LocalContext.current
    val app = context.attic
    val scope = rememberCoroutineScope()
    var hasAccess by remember { mutableStateOf(app.library.hasPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        asked = true
        hasAccess = result[Manifest.permission.READ_MEDIA_AUDIO] == true
        if (hasAccess) scope.launch { app.library.refresh() }
    }

    // Covers the user granting access from system settings and coming back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = app.library.hasPermission()
        if (now != hasAccess) {
            hasAccess = now
            scope.launch { app.library.refresh() }
        }
    }

    val actions = remember(permissions, context) {
        RootActions(
            allowAccess = {
                permissions.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
            },
            openAppPermissions = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
    }

    if (!hasAccess) return UiModel(RootState.NeedsAccess(asked), actions)

    val library by app.library.library.collectAsStateWithLifecycle()
    LaunchedEffect(library) { app.player.onLibraryChanged() }
    val loaded = library ?: return UiModel(RootState.Loading, actions)
    return UiModel(RootState.Ready(loaded), actions)
}
