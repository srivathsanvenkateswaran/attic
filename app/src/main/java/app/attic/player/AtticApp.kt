package app.attic.player

import android.app.Application
import android.content.Context
import app.attic.player.data.AppSettings
import app.attic.player.data.LibraryRepository
import app.attic.player.data.LyricsRepository
import app.attic.player.data.AtticDatabase
import app.attic.player.data.UserData
import app.attic.player.playback.PlayerClient
import app.attic.player.ui.ArtFetcher
import app.attic.player.ui.ArtKeyer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AtticApp : Application(), SingletonImageLoader.Factory {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database by lazy { AtticDatabase.create(this) }
    val library by lazy { LibraryRepository(this, scope) }
    val userData by lazy { UserData(database.dao(), scope) }
    val lyrics by lazy { LyricsRepository(database.dao(), scope) }
    val settings by lazy { AppSettings(this) }
    val player by lazy { PlayerClient(this, library, scope) }

    /** Short confirmations and playback errors, shown as a snackbar. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override fun onCreate() {
        super.onCreate()
        scope.launch { library.refresh() }
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
