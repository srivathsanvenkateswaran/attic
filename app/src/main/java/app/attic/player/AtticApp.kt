package app.attic.player

import android.app.Application
import android.content.Context
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.data.AppSettings
import app.attic.player.data.GenreRepository
import app.attic.player.data.LibraryRepository
import app.attic.player.data.LocalLyricsFinder
import app.attic.player.data.LyricsRepository
import app.attic.player.data.AtticDatabase
import app.attic.player.data.OnboardingGate
import app.attic.player.data.UserData
import app.attic.player.home.HomeFeed
import app.attic.player.data.backup.BackupRepository
import app.attic.player.playback.PlayerClient
import app.attic.player.playback.eq.EqualiserStore
import app.attic.player.radio.LanguageImport
import app.attic.player.radio.RadioController
import app.attic.player.ui.ArtFetcher
import app.attic.player.ui.ArtKeyer
import app.attic.player.ui.model.BackupController
import app.attic.player.ui.warmStyleTypefaces
import app.attic.player.widget.PlayerWidget
import app.attic.player.wrapped.WrappedRepository
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AtticApp : Application(), SingletonImageLoader.Factory {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database by lazy { AtticDatabase.create(this) }
    val library by lazy { LibraryRepository(this, scope) }
    val userData by lazy { UserData(database.dao(), scope) }
    val lyrics by lazy {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        LyricsRepository(
            database.dao(),
            scope,
            userAgent = "Attic/$version (https://github.com/srivathsanvenkateswaran/attic)",
            finder = LocalLyricsFinder(this),
        )
    }
    val settings by lazy { AppSettings(this) }

    /** Whether to show the first run. Decided once per install, on first use. */
    val onboarding by lazy {
        OnboardingGate(
            saved = { settings.onboardingDone },
            settingsHadValues = { settings.hadValuesAtStart },
            hasUserData = { withContext(Dispatchers.IO) { database.dao().hasUserData() } },
            save = settings::setOnboardingDone,
        ).also { it.start(scope) }
    }
    /** The equaliser's settings, read by the playback thread and changed from Now Playing and Settings. */
    val equaliser by lazy { EqualiserStore(database.dao(), settings, library.library, scope) }
    val player by lazy { PlayerClient(this, library, scope) { messages.tryEmit(it) } }
    val radio by lazy { RadioController(this) }
    val homeFeed by lazy { HomeFeed(this) }
    val genres by lazy { GenreRepository(this, database.dao(), library.library, scope) }
    val wrapped by lazy { WrappedRepository(this) }
    val backup by lazy {
        BackupRepository(database, settings, library, packageManager.getPackageInfo(packageName, 0).versionName, filesDir, noBackupFilesDir)
    }
    val backupSession by lazy { BackupController(this) }

    /** Set when a widget tile opens the app on an album, then cleared once navigated. */
    val requestedAlbum = MutableStateFlow<String?>(null)

    /** Set when a file opened from another app starts playing, so Now Playing opens on it. */
    val requestedNowPlaying = MutableStateFlow(false)

    /** Set when the first run ends on its radio link, so the shell opens Settings; cleared once navigated. */
    val requestedSettings = MutableStateFlow(false)

    /** The player's audio session, for Settings' Equaliser row. 0 until the playback service has started. */
    val audioSessionId = MutableStateFlow(0)

    /** Short confirmations and playback errors, shown as a snackbar. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override fun onCreate() {
        super.onCreate()
        // Settings and the database are read before anything this launch writes to them.
        onboarding
        scope.launch { library.refresh() }
        // The style's typefaces take a few milliseconds each; make them now, off the main thread, so
        // the first screen finds them ready (ui/VariableFonts.kt).
        scope.launch(Dispatchers.Default) { warmStyleTypefaces(this@AtticApp, settings.uiStyle.value) }
        scope.launch { LanguageImport.run(this@AtticApp) }
        // A new install that Android restored from a backup merges the copy it came with.
        scope.launch { backup.restoreSnapshotOnce() }
        // New files get analysed the next time the phone charges.
        scope.launch {
            library.library.filterNotNull().collect { if (it.tracks.isNotEmpty()) AnalysisWorker.scheduleWhileCharging(this@AtticApp) }
        }
        // Lyrics in the songs' files and in .lrc files beside them, read again whenever the library changes.
        scope.launch {
            library.library.filterNotNull().collectLatest { lyrics.syncLocal(it.tracks) }
        }
        // The widget is drawn in the app's style, so a new style redraws it at once.
        scope.launch {
            settings.uiStyle.drop(1).collect { PlayerWidget.refresh(this@AtticApp) }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ArtKeyer())
                add(ArtFetcher.Factory())
            }
            .crossfade(120)
            .build()
}

val Context.attic: AtticApp get() = applicationContext as AtticApp
