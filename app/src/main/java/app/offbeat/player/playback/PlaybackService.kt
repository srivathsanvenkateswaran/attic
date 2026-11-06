package app.offbeat.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import app.offbeat.player.MainActivity
import app.offbeat.player.data.SavedQueueEntity
import app.offbeat.player.offbeat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the player. Media3 provides the notification, lock-screen controls and media-button routing
 * from the session, and ExoPlayer handles audio focus and pausing when headphones are unplugged.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private lateinit var player: ExoPlayer
    private lateinit var shuffle: PhysicalShuffle
    private var session: MediaSession? = null
    private var lastRecordedKey: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        // Shuffle reorders the real list (see PhysicalShuffle), so the player itself walks it in order.
        player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
        shuffle = PhysicalShuffle(player)
        player.addListener(PlayerEvents())

        session = MediaSession.Builder(this, shuffle.wrappedPlayer())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setCallback(SessionCallback())
            .build()

        scope.launch { restoreQueue() }
        startSaving()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        saveNow()
        scope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {
        // A controller can't hand us playable URIs across the session boundary, only ids.
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {
            resolve(mediaItems).toMutableList()
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val resolved = resolve(mediaItems)
            val start = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
            val position = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
            val ordered = shuffle.orderNewQueue(resolved, start)
            MediaItemsWithStartPosition(ordered.items, ordered.startIndex, position)
        }

        // Bluetooth or a headset pressed play while the service wasn't running.
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val saved = loadSaved() ?: throw UnsupportedOperationException("Nothing to resume")
            MediaItemsWithStartPosition(saved.items, saved.index, saved.positionMs)
        }
    }

    private suspend fun resolve(items: List<MediaItem>): List<MediaItem> {
        val library = offbeat.library.await()
        return items.mapNotNull { item -> library.trackByKey[item.mediaId]?.toMediaItem() }
    }

    private class SavedQueue(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val entity: SavedQueueEntity,
    )

    private suspend fun loadSaved(): SavedQueue? {
        val saved = offbeat.database.dao().savedQueue() ?: return null
        val library = offbeat.library.await()
        val keys = saved.queueKeys.split('\n').filter { it.isNotEmpty() }
        val currentKey = keys.getOrNull(saved.index)
        // Files can disappear between runs. Keep what is left, and keep the position only for the same song.
        val items = keys.mapNotNull { library.trackByKey[it]?.toMediaItem() }
        if (items.isEmpty()) return null
        val index = items.indexOfFirst { it.mediaId == currentKey }
        return if (index >= 0) {
            SavedQueue(items, index, saved.positionMs, saved)
        } else {
            SavedQueue(items, 0, 0, saved)
        }
    }

    /** Puts the last queue back without playing or preparing, so no notification appears on launch. */
    private suspend fun restoreQueue() {
        val saved = loadSaved() ?: return
        if (player.mediaItemCount > 0) return
        val entity = saved.entity
        player.repeatMode = entity.repeatMode
        shuffle.restore(entity.shuffle, entity.baseKeys?.split('\n')?.filter { it.isNotEmpty() })
        player.playlistMetadata = MediaMetadata.Builder().setTitle(entity.contextName).build()
        player.setMediaItems(saved.items, saved.index, saved.positionMs)
    }

    @kotlin.OptIn(FlowPreview::class)
    private fun startSaving() {
        scope.launch {
            saveRequests.debounce(SAVE_DEBOUNCE_MS).collect { saveNow() }
        }
        scope.launch {
            while (isActive) {
                delay(SAVE_INTERVAL_MS)
                if (player.isPlaying) saveNow()
            }
        }
    }

    private fun saveNow() {
        if (player.mediaItemCount == 0) return
        val keys = (0 until player.mediaItemCount).joinToString("\n") { player.getMediaItemAt(it).mediaId }
        val entity = SavedQueueEntity(
            queueKeys = keys,
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition,
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
            baseKeys = shuffle.baseOrder?.joinToString("\n"),
            contextName = player.playlistMetadata.title?.toString(),
        )
        // Uses the app's scope so the write still finishes while this service is being destroyed.
        offbeat.scope.launch { offbeat.database.dao().saveQueue(entity) }
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                )
            ) {
                saveRequests.tryEmit(Unit)
            }
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED)) {
                recordPlayIfNew()
            }
        }

        // A file deleted or corrupted mid-queue: drop it and carry on rather than stopping the music.
        override fun onPlayerError(error: PlaybackException) {
            val title = player.currentMediaItem?.mediaMetadata?.title ?: "a song"
            Log.w(TAG, "Playback failed for $title", error)
            offbeat.messages.tryEmit("Skipped $title because it can't be played")
            // Posted, because the playlist must not change during a listener callback.
            mainHandler.post {
                if (player.mediaItemCount > 1) {
                    player.removeMediaItem(player.currentMediaItemIndex)
                    player.prepare()
                    player.play()
                } else {
                    player.clearMediaItems()
                }
            }
        }
    }

    private fun recordPlayIfNew() {
        if (!player.isPlaying) return
        val key = player.currentMediaItem?.mediaId ?: return
        if (key == lastRecordedKey) return
        lastRecordedKey = key
        val now = System.currentTimeMillis()
        offbeat.scope.launch { offbeat.database.dao().recordPlay(key, now) }
    }

    private companion object {
        const val TAG = "PlaybackService"
        const val SAVE_DEBOUNCE_MS = 400L
        const val SAVE_INTERVAL_MS = 15_000L
    }
}
