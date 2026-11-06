package app.offbeat.player

import android.app.Application
import android.content.Context
import app.offbeat.player.data.LibraryRepository
import app.offbeat.player.data.OffbeatDatabase
import app.offbeat.player.data.UserData
import app.offbeat.player.playback.PlayerClient
import app.offbeat.player.ui.ArtFetcher
import app.offbeat.player.ui.ArtKeyer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class OffbeatApp : Application(), SingletonImageLoader.Factory {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database by lazy { OffbeatDatabase.create(this) }
    val library by lazy { LibraryRepository(this, scope) }
    val userData by lazy { UserData(database.dao(), scope) }
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

val Context.offbeat: OffbeatApp get() = applicationContext as OffbeatApp
