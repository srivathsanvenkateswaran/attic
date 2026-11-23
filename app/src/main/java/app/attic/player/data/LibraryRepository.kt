package app.attic.player.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads every song MediaStore indexes, on all volumes, and keeps the result current as files change. */
class LibraryRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _library = MutableStateFlow<Library?>(null)

    /** Null until the first scan finishes. Empty if audio access has not been granted. */
    val library: StateFlow<Library?> = _library.asStateFlow()

    private var observerRegistered = false
    private var pendingRescan: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            pendingRescan?.cancel()
            pendingRescan = scope.launch {
                delay(RESCAN_DEBOUNCE_MS)
                refresh()
            }
        }
    }

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh() {
        if (!hasPermission()) {
            _library.value = Library.Empty
            return
        }
        if (!observerRegistered) {
            context.contentResolver.registerContentObserver(COLLECTION, true, observer)
            observerRegistered = true
        }
        _library.value = withContext(Dispatchers.IO) { LibraryBuilder.build(query()) }
    }

    /** Suspends until a scan has finished, scanning now if none has. */
    suspend fun await(): Library {
        if (_library.value == null) refresh()
        return _library.filterNotNull().first()
    }

    private fun query(): List<RawTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.CD_TRACK_NUMBER,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.VOLUME_NAME,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.IS_AUDIOBOOK,
            MediaStore.Audio.Media.IS_RECORDING,
        )
        val rows = ArrayList<RawTrack>(300)
        context.contentResolver.query(
            COLLECTION,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val relativePath = c.getString(2).orEmpty()
                val displayName = c.getString(1).orEmpty()
                // The remaining labels are checked here rather than in SQL, so an unset label counts as false.
                val flags = AudioFlags(
                    isRingtone = c.getInt(11) != 0,
                    isNotification = c.getInt(12) != 0,
                    isAlarm = c.getInt(13) != 0,
                    isPodcast = c.getInt(14) != 0,
                    isAudiobook = c.getInt(15) != 0,
                    isRecording = c.getInt(16) != 0,
                )
                if (!LibraryScope.isSong(relativePath, displayName, flags)) continue
                rows += RawTrack(
                    mediaId = c.getLong(0),
                    displayName = displayName,
                    relativePath = relativePath,
                    title = c.getString(3),
                    artist = c.getString(4),
                    album = c.getString(5),
                    cdTrackNumber = c.getString(6),
                    durationMs = c.getLong(7),
                    year = c.getInt(8).takeIf { it > 0 },
                    dateAddedSec = c.getLong(9),
                    volume = c.getString(10) ?: RawTrack.PRIMARY_VOLUME,
                )
            }
        }
        return rows
    }

    companion object {
        private const val RESCAN_DEBOUNCE_MS = 1500L
        private val COLLECTION: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        fun uriFor(mediaId: Long): Uri = ContentUris.withAppendedId(COLLECTION, mediaId)
    }
}
