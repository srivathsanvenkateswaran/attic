package app.attic.player.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import app.attic.player.data.LibraryRepository
import app.attic.player.data.Track
import app.attic.player.data.isExternal
import app.attic.player.playback.mimeTypeFor

/**
 * Sends a song's audio file to another app. The file goes as its MediaStore `content://` URI with a
 * read grant, which MediaProvider honours for an app that can read the file itself, so no
 * FileProvider or copy is needed.
 */
object SongShare {
    /** False when the song can't be shared (it isn't in the library) or no app can take it. */
    fun share(context: Context, track: Track): Boolean {
        if (track.isExternal || track.mediaId < 0) return false
        val uri = LibraryRepository.uriFor(track.mediaId)
        return try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mimeTypeFor(track.key) ?: "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                // The share sheet shows this above its targets.
                putExtra(Intent.EXTRA_TITLE, track.title)
                clipData = ClipData.newUri(context.contentResolver, track.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
