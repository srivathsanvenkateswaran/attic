package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.attic
import app.attic.player.data.BulkProgress
import app.attic.player.data.Library
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.LocalUi

@Immutable
class SettingsState(
    val themeMode: ThemeMode,
    /** How the app is drawn: Expressive or Comfort. */
    val uiStyle: UiStyle,
    val autoplay: Boolean,
    /** The download-all-lyrics run, if one is going or has just finished. */
    val bulkLyrics: BulkProgress,
    /** Songs in the library with lyrics saved. */
    val savedLyricsCount: Int,
    val songCount: Int,
    val analysis: AnalysisProgress,
)

class SettingsActions(
    val back: () -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    /** Switches style at once; the screen and back stack stay where they are. */
    val setUiStyle: (UiStyle) -> Unit,
    val setAutoplay: (Boolean) -> Unit,
    val downloadAllLyrics: () -> Unit,
    /** Analyses songs now instead of waiting for a charger. */
    val startAnalysis: () -> Unit,
    /** The licences of the code, fonts and model inside Attic. */
    val openLicences: () -> Unit,
)

@Composable
fun rememberSettingsModel(library: Library): UiModel<SettingsState, SettingsActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val mode by app.settings.themeMode.collectAsStateWithLifecycle()
    val style by app.settings.uiStyle.collectAsStateWithLifecycle()
    val autoplay by app.settings.autoplay.collectAsStateWithLifecycle()
    val bulk by app.lyrics.bulk.collectAsStateWithLifecycle()
    val saved by app.lyrics.savedKeys().collectAsStateWithLifecycle(emptyList())
    val savedCount = saved.count { it in library.trackByKey }
    val analyzed by app.radio.analyzedKeys().collectAsStateWithLifecycle(emptyList())
    val analyzing by AnalysisWorker.running(app).collectAsStateWithLifecycle(false)
    val analysis = AnalysisProgress(analyzed.count { it in library.trackByKey }, library.tracks.size, analyzing)

    val state = SettingsState(mode, style, autoplay, bulk, savedCount, library.tracks.size, analysis)
    val actions = SettingsActions(
        back = { ui.back() },
        setThemeMode = app.settings::setThemeMode,
        setUiStyle = app.settings::setUiStyle,
        setAutoplay = app.settings::setAutoplay,
        downloadAllLyrics = { app.lyrics.downloadAll(library.tracks) },
        startAnalysis = { AnalysisWorker.startNow(app) },
        openLicences = { ui.openLicences() },
    )
    return UiModel(state, actions)
}
