package app.attic.player.playback

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.data.ExternalTrack
import app.attic.player.data.Library
import app.attic.player.data.OpenTarget
import app.attic.player.data.OpenWithResolver
import app.attic.player.data.Track
import app.attic.player.data.UriParts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An audio file opened from a file manager or downloads list (ACTION_VIEW). A song in the library
 * plays there, in its album or among all songs, as if tapped in the app. Any other file plays once
 * from the URI: it goes in after the current song without changing the rest of the queue, and is
 * kept out of the library, the saved queue, history, favourites and playlists.
 */
object OpenWith {
    private const val TAG = "OpenWith"

    /** True when [intent] was a file to open, so the caller knows it was handled. */
    fun handle(app: AtticApp, intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        app.scope.launch {
            try {
                open(app, uri)
            } catch (e: Exception) {
                // A URI can be malformed or refuse to be read in ways no list covers. Say so; never crash.
                Log.w(TAG, "Couldn't open $uri", e)
                app.messages.tryEmit(app.getString(R.string.message_open_failed))
            }
        }
        return true
    }

    private suspend fun open(app: AtticApp, uri: Uri) {
        val parts = UriParts(uri.scheme, uri.authority, uri.pathSegments.orEmpty())
        val library = app.library.await()
        when (val target = OpenWithResolver.resolve(parts, library)) {
            is OpenTarget.InLibrary -> playInContext(app, library, target.track)
            OpenTarget.NotInLibrary -> app.player.playExternal(withContext(Dispatchers.IO) { readTrack(app, uri) })
            OpenTarget.Unsupported -> {
                app.messages.tryEmit(app.getString(R.string.message_open_failed))
                return
            }
        }
        app.requestedNowPlaying.value = true
    }

    /** The context a tap in the app would give it: its album, or all songs when it has none. */
    private fun playInContext(app: AtticApp, library: Library, track: Track) {
        val album = track.albumKey?.let { library.albumByKey[it] }?.takeIf { a -> a.tracks.any { it.key == track.key } }
        if (album != null) {
            app.player.play(album.tracks, album.tracks.indexOfFirst { it.key == track.key }, album.name)
        } else {
            app.player.play(library.tracks, library.tracks.indexOfFirst { it.key == track.key }, "All songs")
        }
    }

    /** A track for a file outside the library, named from its tags, else its file name. */
    private fun readTrack(app: AtticApp, uri: Uri): Track {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(app, uri)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            // The player may still manage a file the retriever can't read; tags are only for the label.
            Log.w(TAG, "No tags for $uri", e)
        }
        return Track(
            key = ExternalTrack.keyFor(uri.toString()),
            mediaId = -1L,
            title = title?.takeIf { it.isNotBlank() } ?: fileName(app, uri) ?: app.getString(R.string.open_with_untitled),
            artist = artist?.takeIf { it.isNotBlank() }.orEmpty(),
            artistKeys = emptyList(),
            album = album?.takeIf { it.isNotBlank() },
            albumKey = null,
            folder = null,
            trackNumber = null,
            durationMs = durationMs,
            year = null,
            dateAddedSec = 0L,
        )
    }

    private fun fileName(app: AtticApp, uri: Uri): String? {
        val name = try {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        } ?: uri.lastPathSegment?.substringAfterLast('/')
        return name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    }
}
