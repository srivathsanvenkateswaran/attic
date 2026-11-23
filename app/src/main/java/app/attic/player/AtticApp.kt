package app.attic.player

import android.app.Application
import android.content.Context
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.data.AppSettings
import app.attic.player.data.LibraryRepository
import app.attic.player.data.LyricsRepository
import app.attic.player.data.AtticDatabase
import app.attic.player.data.UserData
import app.attic.player.playback.PlayerClient
import app.attic.player.radio.LanguageImport
import app.attic.player.radio.RadioController
import app.attic.player.ui.ArtFetcher
import app.attic.player.ui.ArtKeyer
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class AtticApp : Application(), SingletonImageLoader.Factory {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database by lazy { AtticDatabase.create(this) }
    val library by lazy { LibraryRepository(this, scope) }
    val userData by lazy { UserData(database.dao(), scope) }
    val lyrics by lazy {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        LyricsRepository(database.dao(), scope, userAgent = "Attic/$version (https://github.com/srivathsanvenkateswaran/attic)")
    }
    val settings by lazy { AppSettings(this) }
    val player by lazy { PlayerClient(this, library, scope) }
    val radio by lazy { RadioController(this) }
    val wrapped by lazy { WrappedRepository(this) }

    /** Short confirmations and playback errors, shown as a snackbar. */
    /** Set when a widget tile opens the app on an album, then cleared once navigated. */
    val requestedAlbum = MutableStateFlow<String?>(null)

    /** Set when a file opened from another app starts playing, so Now Playing opens on it. */
    val requestedNowPlaying = MutableStateFlow(false)

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override fun onCreate() {
        super.onCreate()
        scope.launch { library.refresh() }
        scope.launch { LanguageImport.run(this@AtticApp) }
        // New files get analysed the next time the phone charges.
        scope.launch {
            library.library.filterNotNull().collect { if (it.tracks.isNotEmpty()) AnalysisWorker.scheduleWhileCharging(this@AtticApp) }
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
