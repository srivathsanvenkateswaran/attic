package app.attic.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.attic.player.data.Library
import app.attic.player.data.LibraryRepository
import app.attic.player.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class PlayerUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val contextName: String? = null,
) {
    val current: Track? get() = queue.getOrNull(currentIndex)
}

/** The UI's handle on playback: a MediaController plus its state as a flow. */
class PlayerClient(
    private val context: Context,
    private val library: LibraryRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private val pendingActions = ArrayList<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    /** Held only while the UI is visible, so the service can stop once paused in the background. */
    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val built = MediaController.Builder(context, token).buildAsync().await()
            connecting = false
            controller = built
            built.addListener(listener)
            publish()
            pendingActions.forEach { it(built) }
            pendingActions.clear()
        }
    }

    fun disconnect() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    private fun act(action: (MediaController) -> Unit) {
        val active = controller
        if (active != null && active.isConnected) {
            action(active)
        } else {
            controller = null
            pendingActions += action
            connect()
        }
    }

    private fun publish() {
        val c = controller ?: return
        val lib = library.library.value ?: Library.Empty
        _state.value = PlayerUiState(
            queue = List(c.mediaItemCount) { c.getMediaItemAt(it).toTrack(lib) },
            currentIndex = c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            contextName = c.playlistMetadata.title?.toString(),
        )
    }

    /** Plays [tracks] from [startIndex]. [shuffled] overrides the current shuffle setting when given. */
    fun play(tracks: List<Track>, startIndex: Int, contextName: String?, shuffled: Boolean? = null) {
        if (tracks.isEmpty()) return
        act { c ->
            if (shuffled != null) c.shuffleModeEnabled = shuffled
            c.playlistMetadata = MediaMetadata.Builder().setTitle(contextName).build()
            c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun shufflePlay(tracks: List<Track>, contextName: String?) {
        if (tracks.isEmpty()) return
        play(tracks, tracks.indices.random(), contextName, shuffled = true)
    }

    @OptIn(UnstableApi::class)
    fun togglePlayPause() = act { Util.handlePlayPauseButtonAction(it) }

    fun next() = act { it.seekToNextMediaItem() }

    fun previous() = act { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = act { it.seekTo(positionMs) }

    fun jumpTo(index: Int) = act { c ->
        c.seekTo(index, 0L)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun toggleShuffle() = act { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = act { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(tracks: List<Track>) = act { c ->
        val items = tracks.map { it.toMediaItem() }
        c.addMediaItems((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), items)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun addToQueue(tracks: List<Track>) = act { c ->
        c.addMediaItems(tracks.map { it.toMediaItem() })
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun removeFromQueue(index: Int) = act { it.removeMediaItem(index) }

    fun moveInQueue(from: Int, to: Int) = act { it.moveMediaItem(from, to) }

    /** Keeps the current song and drops everything after it. */
    fun clearUpNext() = act { c ->
        val start = c.currentMediaItemIndex + 1
        if (start < c.mediaItemCount) c.removeMediaItems(start, c.mediaItemCount)
    }

    /** Current playback position, read on demand because it changes too fast to publish as state. */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    /** Re-maps queue entries when a rescan changes what the library knows. */
    fun onLibraryChanged() = publish()
}

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(key)
        .setUri(LibraryRepository.uriFor(mediaId))
        .setMimeType("audio/mpeg")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setTrackNumber(trackNumber)
                .setDurationMs(durationMs)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()

/** Falls back to the item's own metadata if the file has left the library since it was queued. */
private fun MediaItem.toTrack(library: Library): Track =
    library.trackByKey[mediaId] ?: Track(
        key = mediaId,
        mediaId = -1L,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        artistKeys = emptyList(),
        album = mediaMetadata.albumTitle?.toString(),
        albumKey = null,
        folder = null,
        trackNumber = mediaMetadata.trackNumber,
        durationMs = mediaMetadata.durationMs ?: 0L,
        year = null,
        dateAddedSec = 0L,
    )
