package app.attic.player.data

/** MediaStore's own labels for an audio file. A label MediaStore left empty reads as false. */
data class AudioFlags(
    val isMusic: Boolean = true,
    val isRingtone: Boolean = false,
    val isNotification: Boolean = false,
    val isAlarm: Boolean = false,
    val isPodcast: Boolean = false,
    val isAudiobook: Boolean = false,
    val isRecording: Boolean = false,
)

/** What a folder at the top of a library root stands for. */
enum class FolderKind {
    /** Named after the artist of most of its songs, or holding no artist tags at all. */
    Artist,

    /** `Various Artists`: a compilation shelf whose songs credit their own performers. */
    Various,

    /** Anything else, such as `Bollywood/`, `Workout/` or `Snaptube/`: shown as a read-only playlist. */
    Playlist,
}

/**
 * Decides which audio files are songs and where each song's library folder starts.
 *
 * Attic reads every volume MediaStore indexes, so a library can sit in `Music/`, `Music/Offline/`,
 * `Download/` or on an SD card. Folder-derived meaning (artist folders, folder playlists, "Various
 * Artists") is read from the path below the library root that [roots] picks.
 */
object LibraryScope {
    /** Public folders Android creates on every volume. Each one is the start of its own library root. */
    private val STANDARD_FOLDERS = setOf(
        "Music", "Download", "Downloads", "Documents", "Podcasts", "Audiobooks", "Recordings",
        "Ringtones", "Notifications", "Alarms", "DCIM", "Movies", "Pictures", "Android",
    )

    /** Top-level folders whose files are never songs, for phones whose MediaStore leaves the labels unset. */
    private val NON_MUSIC_TOP = setOf("Ringtones", "Notifications", "Alarms", "Podcasts", "Audiobooks", "Recordings")

    /** Folders, at any depth, that hold voice messages or recorder output. */
    private val VOICE_FOLDERS = setOf(
        "WhatsApp Voice Notes",
        "WhatsApp Business Voice Notes",
        "Call recordings",
        "Voice Recorder",
        "Sound recorder",
        "sound_recorder",
    )

    /** Telegram keeps voice messages and shared songs together here; its voice messages are Opus in Ogg. */
    private const val TELEGRAM_AUDIO = "Telegram Audio"
    private val VOICE_EXTENSIONS = setOf("ogg", "oga", "opus")

    /** Share of a root's songs one subfolder must hold to count as a wrapper around the whole library. */
    private const val WRAPPER_SHARE = 0.9

    private val VARIOUS_KEY = TitleCleaner.normalize("Various Artists")
    private val UNKNOWN_TAGS = setOf("<unknown>", "unknown", "")

    fun isSong(relativePath: String, displayName: String, flags: AudioFlags): Boolean {
        if (!flags.isMusic) return false
        if (flags.isRingtone || flags.isNotification || flags.isAlarm) return false
        if (flags.isPodcast || flags.isAudiobook || flags.isRecording) return false
        val parts = segments(relativePath)
        if (parts.firstOrNull() in NON_MUSIC_TOP) return false
        if (parts.any { it in VOICE_FOLDERS }) return false
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (TELEGRAM_AUDIO in parts && extension in VOICE_EXTENSIONS) return false
        return true
    }

    /**
     * The library root for every song, as a relative path ending in '/' (or "" for the volume root).
     *
     * Songs are grouped by volume and by their standard top-level folder (`Music/`, `Download/` …);
     * songs outside any standard folder form one group at the volume root. Each group starts at that
     * folder and steps into a subfolder while the subfolder is a wrapper: it holds at least 90% of
     * the group's songs, most of them in its own subfolders rather than loose inside it, and it is
     * neither an artist folder nor `Various Artists`. So `Music/Offline/` is the root when nearly
     * everything lives in folders under it, `Music/` when artist folders sit directly in it, a
     * one-artist library in `Music/Coldplay/` keeps Coldplay as its artist folder, and a single
     * folder of loose songs such as `Download/Snaptube/` stays a folder below the root.
     */
    fun roots(rows: List<RawTrack>): Map<RawTrack, String> {
        val result = HashMap<RawTrack, String>(rows.size)
        rows.groupBy { it.volume to groupStart(segments(it.relativePath)) }.forEach { (key, group) ->
            val root = descend(key.second, group)
            group.forEach { result[it] = root.joinToString("") { s -> "$s/" } }
        }
        return result
    }

    /** The folders of [relativePath] below [root], or empty when the song sits outside it. */
    fun pathInRoot(relativePath: String, root: String): List<String> {
        val parts = segments(relativePath)
        val rootParts = segments(root)
        if (parts.size < rootParts.size || parts.subList(0, rootParts.size) != rootParts) return emptyList()
        return parts.drop(rootParts.size)
    }

    private fun groupStart(parts: List<String>): List<String> =
        parts.firstOrNull()?.takeIf { it in STANDARD_FOLDERS }?.let(::listOf).orEmpty()

    private fun descend(start: List<String>, group: List<RawTrack>): List<String> {
        val root = start.toMutableList()
        while (true) {
            val below = group.map { segments(it.relativePath) }
                .filter { it.size > root.size && it.subList(0, root.size) == root }
            val (child, count) = below.groupingBy { it[root.size] }.eachCount().maxByOrNull { it.value } ?: break
            if (count < group.size * WRAPPER_SHARE) break
            val inChild = group.filter { segments(it.relativePath).getOrNull(root.size) == child }
            if (isVariousArtists(child) || isProvenArtistFolder(child, inChild)) break
            val loose = inChild.count { segments(it.relativePath).size == root.size + 1 }
            if (loose * 2 >= inChild.size) break
            root += child
        }
        return root
    }

    /**
     * A folder is an artist folder when most of its tagged songs credit the folder's name as artist,
     * album artist or composer, or when none of its songs has any of those tags. `Various Artists` is
     * its own kind. Any other folder is a playlist.
     */
    fun folderKind(name: String, rows: List<RawTrack>): FolderKind = when {
        isVariousArtists(name) -> FolderKind.Various
        rows.none { creditTags(it).isNotEmpty() } -> FolderKind.Artist
        isProvenArtistFolder(name, rows) -> FolderKind.Artist
        else -> FolderKind.Playlist
    }

    fun isVariousArtists(name: String): Boolean = TitleCleaner.normalize(name) == VARIOUS_KEY

    /** The tag with MediaStore's `<unknown>` placeholder and blanks read as absent. */
    fun tagOrNull(tag: String?): String? = tag?.trim()?.takeUnless { it.lowercase() in UNKNOWN_TAGS }

    private fun creditTags(row: RawTrack): List<String> =
        listOfNotNull(tagOrNull(row.artist), tagOrNull(row.albumArtist), tagOrNull(row.composer))

    /** More than half of the folder's tagged songs credit its name, whole ("Simon & Garfunkel") or as one of several. */
    private fun isProvenArtistFolder(name: String, rows: List<RawTrack>): Boolean {
        val key = TitleCleaner.normalize(name)
        val tagged = rows.map(::creditTags).filter { it.isNotEmpty() }
        if (tagged.isEmpty()) return false
        val matching = tagged.count { tags ->
            (tags + tags.flatMap { TitleCleaner.splitCredits(it) }).any { TitleCleaner.normalize(it) == key }
        }
        return matching * 2 > tagged.size
    }

    private fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }
}
