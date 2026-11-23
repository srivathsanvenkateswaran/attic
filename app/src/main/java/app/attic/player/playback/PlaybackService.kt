package app.attic.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import app.attic.player.MainActivity
import app.attic.player.R
import app.attic.player.data.UiStyle
import app.attic.player.data.ListeningEventEntity
import app.attic.player.data.SavedQueueEntity
import app.attic.player.attic
import app.attic.player.widget.PlayerWidget
import app.attic.player.widget.WidgetState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    private lateinit var shuffledPlayer: Player
    private var session: MediaSession? = null
    private var lastRecordedKey: String? = null
    private val listens = ListenTracker()
    private var upcomingKeys: List<String> = emptyList()
    private var upcomingAfter: String? = null
    private var radioFill: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sleep = SleepTimer(SystemClock::elapsedRealtime)
    private var sleepJob: Job? = null

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

        shuffledPlayer = shuffle.wrappedPlayer()
        session = MediaSession.Builder(this, shuffledPlayer)
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
        refreshMediaButtons()
        scope.launch {
            attic.userData.likedSet.collect { refreshMediaButtons() }
        }
        scope.launch {
            attic.settings.uiStyle.collect { refreshMediaButtons() }
        }

        scope.launch { restoreQueue() }
        startSaving()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        logListen(automatic = false)
        saveNow()
        scope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }

    /**
     * Shuffle and favourite, shown beside the transport controls in the notification and on the lock
     * screen. Android draws the card; Attic hands it these two glyphs in the current style's set
     * (pixel/ and oneui/ 10-system, frames B and F): shuffle drawn dim when off, a heart outline that
     * fills when the song is in favourites.
     */
    private fun refreshMediaButtons() {
        val key = player.currentMediaItem?.mediaId
        val liked = key != null && key in attic.userData.likedSet.value
        val shuffled = player.shuffleModeEnabled
        val comfort = attic.settings.uiStyle.value == UiStyle.Comfort
        session?.setMediaButtonPreferences(
            listOf(
                CommandButton.Builder(if (shuffled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
                    .setCustomIconResId(
                        when {
                            comfort && shuffled -> R.drawable.ic_comfort_shuffle_on
                            comfort -> R.drawable.ic_comfort_shuffle_off
                            shuffled -> R.drawable.ic_expressive_shuffle_on
                            else -> R.drawable.ic_expressive_shuffle_off
                        },
                    )
                    .setDisplayName(getString(if (shuffled) R.string.notification_shuffle_off else R.string.notification_shuffle_on))
                    .setSessionCommand(SHUFFLE_COMMAND)
                    .setSlots(CommandButton.SLOT_OVERFLOW)
                    .build(),
                CommandButton.Builder(if (liked) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
                    .setCustomIconResId(
                        when {
                            comfort && liked -> R.drawable.ic_comfort_favourite_on
                            comfort -> R.drawable.ic_comfort_favourite_off
                            liked -> R.drawable.ic_expressive_favourite_on
                            else -> R.drawable.ic_expressive_favourite_off
                        },
                    )
                    .setDisplayName(getString(if (liked) R.string.notification_favourite_remove else R.string.notification_favourite_add))
                    .setSessionCommand(LIKE_COMMAND)
                    .setSlots(CommandButton.SLOT_OVERFLOW)
                    .build(),
            ),
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(mediaSession)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SHUFFLE_COMMAND)
                        .add(LIKE_COMMAND)
                        .add(SleepTimerSession.COMMAND)
                        .build(),
                )
                .setSessionExtras(SleepTimerSession.extras(sleep.status))
                .build()

        override fun onCustomCommand(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand) {
                SHUFFLE_COMMAND -> shuffledPlayer.shuffleModeEnabled = !player.shuffleModeEnabled
                LIKE_COMMAND -> player.currentMediaItem?.mediaId?.let(attic.userData::toggleLike)
                SleepTimerSession.COMMAND -> setSleepTimer(SleepTimerSession.choiceFrom(args))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

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
        val library = attic.library.await()
        return items.mapNotNull { item -> library.trackByKey[item.mediaId]?.toMediaItem() }
    }

    private class SavedQueue(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val entity: SavedQueueEntity,
    )

    private suspend fun loadSaved(): SavedQueue? {
        val saved = attic.database.dao().savedQueue() ?: return null
        val library = attic.library.await()
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
        if (entity.radioSeed != null && entity.contextName != null) attic.radio.resume(entity.radioSeed, entity.contextName)
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
            radioSeed = attic.radio.seedFor(player.playlistMetadata.title?.toString()),
        )
        // Uses the app's scope so the write still finishes while this service is being destroyed.
        attic.scope.launch { attic.database.dao().saveQueue(entity) }
    }

    /**
     * Runs in the service so the timer outlives the UI. A countdown fades the last ten seconds out and
     * pauses; "End of this song" fades the song's own last seconds and lets the player stop at its end
     * (pauseAtEndOfMediaItems). Either way the music is paused, not stopped, and the volume put back.
     */
    private fun setSleepTimer(choice: SleepChoice) {
        sleep.set(choice)
        player.pauseAtEndOfMediaItems = choice == SleepChoice.EndOfSong
        runSleepTimer()
        publishSleepTimer()
    }

    private fun runSleepTimer() {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            while (isActive) {
                val duration = player.duration
                val songRemaining = if (duration == C.TIME_UNSET) null else (duration - player.currentPosition).coerceAtLeast(0)
                when (val step = sleep.step(songRemaining)) {
                    is SleepTimer.Step.Play -> {
                        player.volume = step.volume
                        delay(step.nextCheckMs)
                    }
                    SleepTimer.Step.Pause -> {
                        player.pause()
                        endSleepTimer()
                        return@launch
                    }
                    SleepTimer.Step.Idle -> {
                        player.volume = 1f
                        return@launch
                    }
                }
            }
        }
    }

    /** The timer has run out or switched itself off: full volume again for whenever play is pressed. */
    private fun endSleepTimer() {
        player.volume = 1f
        player.pauseAtEndOfMediaItems = false
        publishSleepTimer()
    }

    private fun publishSleepTimer() {
        session?.setSessionExtras(SleepTimerSession.extras(sleep.status))
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && sleep.songEnded()) {
                sleepJob?.cancel()
                endSleepTimer()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            logListen(
                automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            )
            if (mediaItem != null) {
                listens.start(mediaItem.mediaId, mediaItem.mediaMetadata.durationMs ?: 0L)
                listens.playing(player.isPlaying)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listens.playing(isPlaying)
            if (isPlaying && sleep.resumed()) {
                sleepJob?.cancel()
                endSleepTimer()
            }
        }

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
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                refreshMediaButtons()
            }
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_TIMELINE_CHANGED)) {
                publishToWidgets()
            }
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYLIST_METADATA_CHANGED)) {
                noticeRemovals()
                fillRadioQueue()
            }
        }

        // A file deleted or corrupted mid-queue: drop it and carry on rather than stopping the music.
        override fun onPlayerError(error: PlaybackException) {
            val title = player.currentMediaItem?.mediaMetadata?.title ?: getString(R.string.message_skipped_fallback)
            Log.w(TAG, "Playback failed for $title", error)
            attic.messages.tryEmit(getString(R.string.message_skipped, title))
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

    private fun logListen(automatic: Boolean) {
        val listen = listens.finish(automatic) ?: return
        val context = player.playlistMetadata.title?.toString()
        val cross = attic.radio.isCrossPick(listen.key)
        if (listen.outcome == ListenTracker.SKIPPED) attic.radio.skipped(listen.key)
        attic.scope.launch {
            attic.database.dao().insertEvent(
                ListeningEventEntity(
                    trackKey = listen.key,
                    startedAt = listen.startedAt,
                    listenedMs = listen.listenedMs,
                    durationMs = listen.durationMs,
                    outcome = listen.outcome,
                    contextName = context,
                    radioCross = cross,
                ),
            )
        }
    }

    /** One song swiped out of Up next means "less like this". Clearing the list means stop filling it. */
    private fun noticeRemovals() {
        val index = player.currentMediaItemIndex
        val current = player.currentMediaItem?.mediaId
        val upcoming = (index + 1 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        if (current != null && current == upcomingAfter) {
            val removed = upcomingKeys - upcoming.toSet()
            if (removed.size == 1) {
                val key = removed.single()
                attic.radio.skipped(key)
                val context = player.playlistMetadata.title?.toString()
                val now = System.currentTimeMillis()
                attic.scope.launch {
                    attic.database.dao().insertEvent(
                        ListeningEventEntity(
                            trackKey = key,
                            startedAt = now,
                            listenedMs = 0,
                            durationMs = 0,
                            outcome = ListenTracker.REMOVED,
                            contextName = context,
                            radioCross = attic.radio.isCrossPick(key),
                        ),
                    )
                }
            } else if (removed.size > 1 && upcoming.isEmpty()) {
                attic.radio.stop(player.playlistMetadata.title?.toString())
            }
        }
        upcomingAfter = current
        upcomingKeys = upcoming
    }

    /** Keeps ten songs visible in a radio queue. New songs only ever go on the end, so your order stands. */
    private fun fillRadioQueue() {
        val context = player.playlistMetadata.title?.toString()
        if (!attic.radio.isActive(context) || context == null) {
            mainHandler.post(::startAutoplay)
            return
        }
        val upcoming = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (upcoming >= RADIO_UP_NEXT || !player.playWhenReady || radioFill?.isActive == true) return
        val inQueue = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        radioFill = scope.launch {
            val tracks = attic.radio.pick(context, inQueue, RADIO_UP_NEXT - upcoming, queueWouldEnd = upcoming == 0)
            if (tracks.isNotEmpty() && attic.radio.isActive(player.playlistMetadata.title?.toString())) {
                player.addMediaItems(tracks.map { it.toMediaItem() })
                // The last song may have ended while radio was still choosing.
                if (player.playbackState == Player.STATE_ENDED) player.seekToNextMediaItem()
            }
        }
    }

    /**
     * Once an ordinary queue is on its last song, radio takes over from that song so Next has somewhere
     * to go. Posted rather than run inside the listener, so the context change lands as its own event.
     */
    private fun startAutoplay() {
        val current = player.currentMediaItem ?: return
        val context = player.playlistMetadata.title?.toString()
        if (attic.radio.isActive(context)) return
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) return
        if (player.repeatMode != Player.REPEAT_MODE_OFF || !player.playWhenReady || !attic.settings.autoplay.value) return
        val track = attic.library.library.value?.trackByKey?.get(current.mediaId) ?: return
        val name = attic.radio.continueFrom(track, context) ?: return
        player.playlistMetadata = MediaMetadata.Builder().setTitle(name).build()
    }

    private fun publishToWidgets() {
        val item = player.currentMediaItem
        val track = item?.let { attic.library.library.value?.trackByKey?.get(it.mediaId) }
        PlayerWidget.publish(
            this,
            WidgetState(
                title = item?.mediaMetadata?.title?.toString(),
                artist = item?.mediaMetadata?.artist?.toString(),
                mediaId = track?.mediaId,
                isPlaying = player.isPlaying,
            ),
        )
    }

    private fun recordPlayIfNew() {
        if (!player.isPlaying) return
        val key = player.currentMediaItem?.mediaId ?: return
        if (key == lastRecordedKey) return
        lastRecordedKey = key
        val now = System.currentTimeMillis()
        attic.scope.launch { attic.database.dao().recordPlay(key, now) }
    }

    private companion object {
        const val TAG = "PlaybackService"
        val SHUFFLE_COMMAND = SessionCommand("app.attic.player.SHUFFLE", Bundle.EMPTY)
        val LIKE_COMMAND = SessionCommand("app.attic.player.LIKE", Bundle.EMPTY)
        const val SAVE_DEBOUNCE_MS = 400L
        const val SAVE_INTERVAL_MS = 15_000L
        const val RADIO_UP_NEXT = 10
    }
}
