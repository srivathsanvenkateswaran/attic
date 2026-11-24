package app.attic.player.ui.model

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.R
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.analysis.LOUDNESS_VERSION
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.attic
import app.attic.player.data.BulkProgress
import app.attic.player.data.Library
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.data.backup.RestoreReport
import app.attic.player.playback.CrossfadePlan
import app.attic.player.playback.Equaliser
import app.attic.player.ui.LocalUi
import java.time.LocalDate

@Immutable
class SettingsState(
    val themeMode: ThemeMode,
    /** How the app is drawn: Expressive or Comfort. */
    val uiStyle: UiStyle,
    val autoplay: Boolean,
    /** Songs play at a matched loudness. */
    val volumeLevelling: Boolean,
    /** Seconds one song fades into the next; 0 is off. One of [crossfadeChoices]. */
    val crossfadeSeconds: Int,
    /** False when nothing on the phone opens an equaliser panel, and the row is hidden. */
    val equaliserAvailable: Boolean,
    /** The download-all-lyrics run, if one is going or has just finished. */
    val bulkLyrics: BulkProgress,
    /** Songs in the library with lyrics saved. */
    val savedLyricsCount: Int,
    val songCount: Int,
    val analysis: AnalysisProgress,
    val backup: BackupUiState,
    /** The Equaliser row: on or off, the default's preset, and how many songs and albums have their own. */
    val equaliser: EqualiserSettingsRow = EqualiserSettingsRow(on = false, presetName = "", ownSongs = 0, ownAlbums = 0),
) {
    /** The crossfade lengths offered, in seconds, Off first. */
    val crossfadeChoices: List<Int> get() = CrossfadePlan.choices
}

/**
 * Back up and restore. At most one of [confirm], [restored] and [problem] is set, and a style draws
 * it as a dialog: the question before restoring, the result, or why the file couldn't be used.
 */
@Immutable
class BackupUiState(
    val saving: Boolean,
    /** Reading the chosen file or merging it in. */
    val restoring: Boolean,
    val confirm: BackupSummary?,
    val restored: RestoreReport?,
    val problem: RestoreProblem?,
) {
    val busy: Boolean get() = saving || restoring

    companion object {
        fun of(status: BackupStatus) = BackupUiState(
            saving = status == BackupStatus.Saving,
            restoring = status == BackupStatus.Reading || status == BackupStatus.Restoring,
            confirm = (status as? BackupStatus.Confirming)?.summary,
            restored = (status as? BackupStatus.Restored)?.report,
            problem = (status as? BackupStatus.Failed)?.problem,
        )
    }
}

class SettingsActions(
    val back: () -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    /** Switches style at once; the screen and back stack stay where they are. */
    val setUiStyle: (UiStyle) -> Unit,
    val setAutoplay: (Boolean) -> Unit,
    val setVolumeLevelling: (Boolean) -> Unit,
    val setCrossfadeSeconds: (Int) -> Unit,
    /** Opens the phone's equaliser on Attic's audio session. Moved to the Equaliser page's "Phone equaliser" row. */
    val openEqualiser: () -> Unit,
    /** Opens Settings > Playback > Equaliser. */
    val openEqualiserPage: () -> Unit = {},
    val downloadAllLyrics: () -> Unit,
    /** Analyses songs now instead of waiting for a charger. */
    val startAnalysis: () -> Unit,
    /** The licences of the code, fonts and model inside Attic. */
    val openLicences: () -> Unit,
    /** Opens the system's file picker to save a backup. */
    val backUp: () -> Unit,
    /** Opens the system's file picker to choose a backup; restoring waits for [confirmRestore]. */
    val restore: () -> Unit,
    val confirmRestore: () -> Unit,
    /** Closes the restore question, result or problem. */
    val dismissBackupDialog: () -> Unit,
)

@Composable
fun rememberSettingsModel(library: Library): UiModel<SettingsState, SettingsActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val mode by app.settings.themeMode.collectAsStateWithLifecycle()
    val style by app.settings.uiStyle.collectAsStateWithLifecycle()
    val autoplay by app.settings.autoplay.collectAsStateWithLifecycle()
    val levelling by app.settings.volumeLevelling.collectAsStateWithLifecycle()
    val crossfade by app.settings.crossfadeSeconds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Checked each time Settings opens, so an equaliser installed meanwhile shows up.
    val equaliserAvailable = remember { Equaliser.isAvailable(context) }
    val equaliser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val bulk by app.lyrics.bulk.collectAsStateWithLifecycle()
    val saved by app.lyrics.savedKeys().collectAsStateWithLifecycle(emptyList())
    val savedCount = saved.count { it in library.trackByKey }
    // A song counts once the worker has nothing left to do for it: its sound and styles (or found unreadable),
    // and its loudness. Songs analysed before styles or loudness existed show as still to do.
    val analyzed by remember { app.database.dao().completeKeys(SongAnalyzer.MODEL_VERSION, LOUDNESS_VERSION) }
        .collectAsStateWithLifecycle(emptyList())
    val analyzing by AnalysisWorker.running(app).collectAsStateWithLifecycle(false)
    val analysis = AnalysisProgress(analyzed.count { it in library.trackByKey }, library.tracks.size, analyzing)

    val backupStatus by app.backupSession.status.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    // The system picker writes and reads through the Storage Access Framework, so no storage permission is needed.
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        uri?.let(app.backupSession::save)
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(app.backupSession::open)
    }

    val state = SettingsState(
        mode,
        style,
        autoplay,
        levelling,
        crossfade,
        equaliserAvailable,
        bulk,
        savedCount,
        library.tracks.size,
        analysis,
        BackupUiState.of(backupStatus),
        rememberEqualiserSettingsRow(library),
    )
    val actions = SettingsActions(
        back = { ui.back() },
        setThemeMode = app.settings::setThemeMode,
        setUiStyle = { choice ->
            if (choice != style) haptics.perform(Haptic.Tick)
            app.settings.setUiStyle(choice)
        },
        setAutoplay = app.settings::setAutoplay,
        setVolumeLevelling = app.settings::setVolumeLevelling,
        setCrossfadeSeconds = app.settings::setCrossfadeSeconds,
        openEqualiser = {
            try {
                equaliser.launch(Equaliser.panelIntent(context, app.audioSessionId.value))
            } catch (_: ActivityNotFoundException) {
                app.messages.tryEmit(app.getString(R.string.settings_equaliser_failed))
            }
        },
        openEqualiserPage = { ui.openEqualiser() },
        downloadAllLyrics = { app.lyrics.downloadAll(library.tracks) },
        startAnalysis = { AnalysisWorker.startNow(app) },
        openLicences = { ui.openLicences() },
        backUp = { createFile.launch("attic-backup-${LocalDate.now()}.json") },
        // Some file managers label a .json file as plain text or plain bytes, so those can be picked too.
        restore = { openFile.launch(arrayOf(BACKUP_MIME, "application/gzip", "application/x-gzip", "application/octet-stream", "text/plain")) },
        confirmRestore = app.backupSession::confirm,
        dismissBackupDialog = app.backupSession::dismiss,
    )
    return UiModel(state, actions)
}

private const val BACKUP_MIME = "application/json"
