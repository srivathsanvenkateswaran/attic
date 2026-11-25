package app.attic.player.ui.model

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.R
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.analysis.LOUDNESS_VERSION
import app.attic.player.analysis.TRANSITIONS_VERSION
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.attic
import app.attic.player.data.BulkProgress
import app.attic.player.data.Library
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.data.backup.RestoreReport
import app.attic.player.playback.TransitionMode
import app.attic.player.playback.TransitionPlanner
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
    /** What happens between songs: Off, Crossfade or Smart. */
    val transitions: TransitionMode,
    /** The crossfade length in seconds, one of [crossfadeChoices]: Crossfade's fade, and Smart's for songs not analysed yet. */
    val crossfadeSeconds: Int,
    /** The download-all-lyrics run, if one is going or has just finished. */
    val bulkLyrics: BulkProgress,
    /** Songs in the library with lyrics saved. */
    val savedLyricsCount: Int,
    val songCount: Int,
    val analysis: AnalysisProgress,
    val backup: BackupUiState,
    /** The Equaliser row: on or off, the default's preset, and how many songs and albums have their own. */
    val equaliser: EqualiserSettingsRow,
    /** The Moods and interests row's second line: "Chill, Focus · 3 genres · Tamil, English". */
    val interestsSummary: String = "",
) {
    /** The transition choices, in the order the picker lists them. */
    val transitionChoices: List<TransitionMode> get() = TransitionMode.entries

    /** The crossfade lengths offered, in seconds. */
    val crossfadeChoices: List<Int> get() = TransitionPlanner.lengthChoices

    /** The length row shows only when a transition uses it. */
    val showsCrossfadeLength: Boolean get() = transitions != TransitionMode.Off
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
    val setTransitions: (TransitionMode) -> Unit,
    val setCrossfadeSeconds: (Int) -> Unit,
    /** Opens Settings > Playback > Equaliser. */
    val openEqualiserPage: () -> Unit,
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
    /** The Moods and interests page, where the mood mixes' picks are made. */
    val openInterests: () -> Unit = {},
)

@Composable
fun rememberSettingsModel(library: Library): UiModel<SettingsState, SettingsActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val mode by app.settings.themeMode.collectAsStateWithLifecycle()
    val style by app.settings.uiStyle.collectAsStateWithLifecycle()
    val autoplay by app.settings.autoplay.collectAsStateWithLifecycle()
    val levelling by app.settings.volumeLevelling.collectAsStateWithLifecycle()
    val transitions by app.settings.transitions.collectAsStateWithLifecycle()
    val crossfade by app.settings.crossfadeSeconds.collectAsStateWithLifecycle()
    val bulk by app.lyrics.bulk.collectAsStateWithLifecycle()
    val saved by app.lyrics.savedKeys().collectAsStateWithLifecycle(emptyList())
    val savedCount = saved.count { it in library.trackByKey }
    // A song counts once the worker has nothing left to do for it: its sound and styles (or found unreadable),
    // its loudness and its transitions. Songs analysed before any of those existed show as still to do.
    val analyzed by remember { app.database.dao().completeKeys(SongAnalyzer.MODEL_VERSION, LOUDNESS_VERSION, TRANSITIONS_VERSION) }
        .collectAsStateWithLifecycle(emptyList())
    val analyzing by AnalysisWorker.running(app).collectAsStateWithLifecycle(false)
    val analysis = AnalysisProgress(analyzed.count { it in library.trackByKey }, library.tracks.size, analyzing)

    val backupStatus by app.backupSession.status.collectAsStateWithLifecycle()
    val interests by app.settings.interests.collectAsStateWithLifecycle()
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
        transitions,
        crossfade,
        bulk,
        savedCount,
        library.tracks.size,
        analysis,
        BackupUiState.of(backupStatus),
        rememberEqualiserSettingsRow(library),
        interestsSummary(interests, LocalResources.current),
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
        setTransitions = app.settings::setTransitions,
        setCrossfadeSeconds = app.settings::setCrossfadeSeconds,
        openEqualiserPage = { ui.openEqualiser() },
        downloadAllLyrics = { app.lyrics.downloadAll(library.tracks) },
        startAnalysis = { AnalysisWorker.startNow(app) },
        openLicences = { ui.openLicences() },
        backUp = { createFile.launch("attic-backup-${LocalDate.now()}.json") },
        // Some file managers label a .json file as plain text or plain bytes, so those can be picked too.
        restore = { openFile.launch(arrayOf(BACKUP_MIME, "application/gzip", "application/x-gzip", "application/octet-stream", "text/plain")) },
        confirmRestore = app.backupSession::confirm,
        dismissBackupDialog = app.backupSession::dismiss,
        openInterests = { ui.openInterests() },
    )
    return UiModel(state, actions)
}

private const val BACKUP_MIME = "application/json"
