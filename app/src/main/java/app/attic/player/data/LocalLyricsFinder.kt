package app.attic.player.data

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Finds lyrics on the phone: `.lrc` files beside songs and lyrics in the songs' own tags.
 *
 * READ_MEDIA_AUDIO is enough for both. MediaStore files `.lrc` as `application/lrc`, a subtitle
 * type, and its Files collection shows subtitle files to any app that may read audio. No other
 * storage permission is needed.
 */
class LocalLyricsFinder(private val context: Context) {
    data class AudioFile(val mediaId: Long, val volume: String, val relativePath: String, val displayName: String, val modifiedSec: Long)

    data class Sidecar(val id: Long, val volume: String, val modifiedSec: Long)

    /** Every song file, or only [mediaId] when given. */
    fun audioFiles(mediaId: Long? = null): Map<Long, AudioFile> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.VOLUME_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        val selection = mediaId?.let { "${MediaStore.Audio.Media._ID} = ?" }
        val args = mediaId?.let { arrayOf(it.toString()) }
        val result = HashMap<Long, AudioFile>()
        context.contentResolver.query(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, selection, args, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val file = AudioFile(
                    mediaId = c.getLong(0),
                    volume = c.getString(1) ?: RawTrack.PRIMARY_VOLUME,
                    relativePath = c.getString(2) ?: "",
                    displayName = c.getString(3) ?: continue,
                    modifiedSec = c.getLong(4),
                )
                result[file.mediaId] = file
            }
        }
        return result
    }

    /** `.lrc` files keyed by [sidecarKey], from every folder or only [inFolderOf]'s folder. */
    fun sidecars(inFolderOf: AudioFile? = null): Map<String, Sidecar> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.VOLUME_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        var selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.lrc'"
        var args: Array<String>? = null
        if (inFolderOf != null) {
            selection += " AND ${MediaStore.Files.FileColumns.VOLUME_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
            args = arrayOf(inFolderOf.volume, inFolderOf.relativePath)
        }
        val result = HashMap<String, Sidecar>()
        context.contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, selection, args, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val volume = c.getString(1) ?: RawTrack.PRIMARY_VOLUME
                val name = c.getString(3) ?: continue
                result[key(volume, c.getString(2) ?: "", name.substringBeforeLast('.'))] = Sidecar(c.getLong(0), volume, c.getLong(4))
            }
        }
        return result
    }

    /** The key of the `.lrc` file that belongs to [file]: same volume, folder and name before the extension. */
    fun sidecarKey(file: AudioFile): String = key(file.volume, file.relativePath, file.displayName.substringBeforeLast('.'))

    fun readSidecar(sidecar: Sidecar): String? {
        val uri = MediaStore.Files.getContentUri(sidecar.volume, sidecar.id)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readNBytes(MAX_SIDECAR_BYTES) } ?: return null
        return decode(bytes)
    }

    fun readEmbedded(file: AudioFile): EmbeddedLyrics {
        val pfd = context.contentResolver.openFileDescriptor(LibraryRepository.uriFor(file.mediaId), "r") ?: return EmbeddedLyrics.None
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).channel.use { channel ->
            EmbeddedLyricsReader.from(TagReader.metadata(channel, file.displayName))
        }
    }

    private fun key(volume: String, relativePath: String, baseName: String) = "$volume\u0000$relativePath\u0000${baseName.lowercase()}"

    private companion object {
        /** A long song's LRC is a few kilobytes. Anything past this isn't lyrics. */
        const val MAX_SIDECAR_BYTES = 512 * 1024

        /** Most LRC files are UTF-8; older ones are often Windows-1252. */
        fun decode(bytes: ByteArray): String = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1252"))
        }
    }
}
