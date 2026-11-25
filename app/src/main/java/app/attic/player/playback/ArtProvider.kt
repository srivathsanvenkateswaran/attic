package app.attic.player.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import app.attic.player.ui.loadCover
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves song covers as files, for Android Auto and other car screens, which take artwork only as a
 * URI. A cover is cut from the song's file once, at [SIZE_PX], and kept in the cache until the next
 * launch. It isn't exported: the car's process opens these URIs on a read grant the session gives
 * each connected controller for the covers it hands it ([ArtGrants]). Read-only, covers only.
 */
class ArtProvider : ContentProvider() {
    private val launch = System.currentTimeMillis().toString()
    private val root: File by lazy { File(context!!.cacheDir, "car-art") }
    private val dir: File by lazy { File(root, launch).apply { mkdirs() } }
    private val locks = ConcurrentHashMap<Long, Any>()

    override fun onCreate(): Boolean {
        // Covers can change when a file is retagged, so each launch cuts its own and clears the last one's.
        Thread { root.listFiles()?.filter { it.name != launch }?.forEach(File::deleteRecursively) }.start()
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Covers are read-only")
        val mediaId = mediaIdOf(uri) ?: throw FileNotFoundException(uri.toString())
        val file = File(dir, "$mediaId.jpg")
        synchronized(locks.computeIfAbsent(mediaId) { Any() }) {
            if (!file.exists()) cut(mediaId, file)
        }
        // An empty file records that the song has no cover, so it isn't decoded again.
        if (file.length() == 0L) throw FileNotFoundException("No cover for $mediaId")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun cut(mediaId: Long, file: File) {
        val bitmap = loadCover(context!!, mediaId, SIZE_PX)
        val partial = File(dir, "$mediaId.part")
        partial.outputStream().use { out -> bitmap?.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
        partial.renameTo(file)
    }

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        /** Matches the provider's authority in the manifest. */
        const val AUTHORITY = "app.attic.player.art"
        private const val SIZE_PX = 480
        private const val JPEG_QUALITY = 90

        fun uriFor(mediaId: Long): Uri = Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("cover").appendPath(mediaId.toString()).build()

        private fun mediaIdOf(uri: Uri): Long? =
            uri.pathSegments.takeIf { it.size == 2 && it[0] == "cover" }?.get(1)?.toLongOrNull()?.takeIf { it >= 0 }
    }
}
