package app.attic.player.data

/**
 * The parts of a URI the resolver reads, taken from `android.net.Uri` by the caller so this stays
 * free of Android and can be tested. [pathSegments] are decoded.
 */
data class UriParts(
    val scheme: String?,
    val authority: String?,
    val pathSegments: List<String>,
)

/** What an "Open with" URI turned out to be. */
sealed interface OpenTarget {
    /** A song in the library: it plays there, in its album or among all songs. */
    data class InLibrary(val track: Track) : OpenTarget

    /** A playable file the library doesn't hold: it plays once from the URI and is kept nowhere. */
    data object NotInLibrary : OpenTarget

    /** Not something Attic can open (an http link, a URI with no scheme). */
    data object Unsupported : OpenTarget
}

/**
 * Works out which library song, if any, a URI from a file manager or downloads list points at. A
 * library key is the file's path relative to its volume, so a URI matches when it gives either a
 * MediaStore id or a path on a volume. Anything it can't place is still played, as a file of its own.
 */
object OpenWithResolver {
    private const val MEDIA = "media"
    private const val MEDIA_DOCUMENTS = "com.android.providers.media.documents"
    private const val DOWNLOADS_DOCUMENTS = "com.android.providers.downloads.documents"
    private const val STORAGE_DOCUMENTS = "com.android.externalstorage.documents"

    /** `/storage/emulated/0/…`, `/storage/self/primary/…`, `/sdcard/…`, or an SD card's `/storage/1A2B-3C4D/…`. */
    private val VOLUME_PATH = Regex("""(?:^|/)(?:storage/emulated/\d+|storage/self/primary|sdcard|storage/(?!emulated/|self/)[^/]+)/(.+)$""")

    fun resolve(parts: UriParts, library: Library): OpenTarget {
        val scheme = parts.scheme?.lowercase()
        if (scheme != "content" && scheme != "file") return OpenTarget.Unsupported
        val track = mediaStoreId(parts)?.let { id -> library.tracks.firstOrNull { it.mediaId == id } }
            ?: libraryKey(parts)?.let { library.trackByKey[it] }
        return if (track != null) OpenTarget.InLibrary(track) else OpenTarget.NotInLibrary
    }

    /** The MediaStore row a URI names, from MediaStore itself or from a documents provider that wraps it. */
    fun mediaStoreId(parts: UriParts): Long? {
        if (parts.scheme?.lowercase() != "content") return null
        return when (parts.authority) {
            // content://media/external/audio/media/123, or …/file/123 on another volume.
            MEDIA -> parts.pathSegments.lastOrNull()?.toLongOrNull()?.takeIf { it >= 0 }
            // A document id like "audio:123".
            MEDIA_DOCUMENTS -> documentId(parts)?.let { idAfter(it, "audio:") }
            // "msf:123" is a MediaStore id; a bare number is the downloads list's own id and says nothing here.
            DOWNLOADS_DOCUMENTS -> documentId(parts)?.let { idAfter(it, "msf:") }
            else -> null
        }
    }

    /** The library key (path relative to its volume) a URI names, or null when it gives no path. */
    fun libraryKey(parts: UriParts): String? {
        val scheme = parts.scheme?.lowercase()
        if (scheme == "file") return keyForPath("/" + parts.pathSegments.joinToString("/"))
        if (scheme != "content") return null
        return when (parts.authority) {
            MEDIA, MEDIA_DOCUMENTS -> null
            // "raw:/storage/emulated/0/Download/song.mp3".
            DOWNLOADS_DOCUMENTS -> documentId(parts)?.takeIf { it.startsWith("raw:") }?.let { keyForPath(it.removePrefix("raw:")) }
            // "primary:Music/song.mp3" or "1A2B-3C4D:Music/song.mp3": the part after the colon is the key.
            STORAGE_DOCUMENTS -> documentId(parts)?.substringAfter(':', "")?.let(::cleanKey)
            // Many file managers serve their own URIs with the real path inside them.
            else -> keyForPath("/" + parts.pathSegments.joinToString("/"))
        }
    }

    /** The key for an absolute path on a volume, or null when the path isn't on one. */
    fun keyForPath(path: String): String? = VOLUME_PATH.find(path)?.groupValues?.get(1)?.let(::cleanKey)

    /** Rejects keys that climb out of their folder or end in a folder, which no song's key does. */
    private fun cleanKey(key: String): String? {
        if (key.isEmpty() || key.endsWith('/')) return null
        val segments = key.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
        return key
    }

    /** The document id in `…/document/<id>` or `…/tree/<tree>/document/<id>`. */
    private fun documentId(parts: UriParts): String? {
        val segments = parts.pathSegments
        val at = segments.lastIndexOf("document")
        return segments.getOrNull(at + 1)?.takeIf { at >= 0 && it.isNotEmpty() }
    }

    private fun idAfter(documentId: String, prefix: String): Long? =
        documentId.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toLongOrNull()?.takeIf { it >= 0 }
}
