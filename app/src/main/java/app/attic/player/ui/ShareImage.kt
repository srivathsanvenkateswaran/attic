package app.attic.player.ui

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sends a rendered card to other apps, or keeps it in the phone's Pictures. */
object ShareImage {
    /** Story size, which Instagram and WhatsApp status show without cropping. */
    private const val WIDTH = 1080
    private const val HEIGHT = 1920

    suspend fun share(context: Context, bitmap: Bitmap, name: String) {
        val file = withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "shared").apply { mkdirs() }
            folder.listFiles()?.forEach { it.delete() }
            File(folder, "$name.png").also { file -> file.outputStream().use { storySized(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** True once the image is in Pictures/Attic. */
    suspend fun save(context: Context, bitmap: Bitmap, name: String): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Attic")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { storySized(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        }.getOrDefault(false)
        if (!written) {
            resolver.delete(uri, null, null)
            return@withContext false
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        true
    }

    private fun storySized(bitmap: Bitmap): Bitmap {
        val software = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        return if (software.width == WIDTH && software.height == HEIGHT) software else Bitmap.createScaledBitmap(software, WIDTH, HEIGHT, true)
    }
}
