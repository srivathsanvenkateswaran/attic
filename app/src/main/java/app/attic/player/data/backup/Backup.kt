package app.attic.player.data.backup

/**
 * Everything a backup file holds, in the file's own terms so the format can stay put while the
 * database changes. A song is its library key, the file's path relative to its volume
 * (`Music/Hindi/Kabira.mp3`), the same key the database uses.
 *
 * Song analysis is not in here. It is derived from the audio, about 34 KB a song, and runs again on
 * its own while the phone charges (docs/backup.md has the reasoning).
 */
data class Backup(
    val createdAt: Long,
    val appVersion: String?,
    val settings: BackupSettings?,
    val likes: List<BackupLike> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val plays: List<BackupPlay> = emptyList(),
    val listens: List<BackupListen> = emptyList(),
    val lyrics: List<BackupLyrics> = emptyList(),
    val languages: List<BackupLanguage> = emptyList(),
    val queue: BackupQueue? = null,
) {
    /** Every song the backup mentions, once each. */
    fun songs(): Set<String> = buildSet {
        likes.forEach { add(it.song) }
        playlists.forEach { addAll(it.songs) }
        plays.forEach { add(it.song) }
        listens.forEach { add(it.song) }
        lyrics.forEach { add(it.song) }
        languages.forEach { add(it.song) }
    }
}

/** Names as [app.attic.player.data.ThemeMode] and [app.attic.player.data.UiStyle] spell them. Null when absent. */
data class BackupSettings(val theme: String?, val uiStyle: String?, val autoplay: Boolean?)

data class BackupLike(val song: String, val likedAt: Long)

data class BackupPlaylist(val name: String, val createdAt: Long, val songs: List<String>)

/** One row of the play-count summary behind "Recently played". */
data class BackupPlay(val song: String, val lastPlayedAt: Long, val playCount: Int)

/** One entry of the listening log that radio and Recap read. */
data class BackupListen(
    val song: String,
    val startedAt: Long,
    val listenedMs: Long,
    val durationMs: Long,
    val outcome: String,
    val context: String?,
    val radioCross: Boolean,
)

/** Lyrics that were found. Checked misses are not kept. */
data class BackupLyrics(
    val song: String,
    val synced: String?,
    val plain: String?,
    val instrumental: Boolean,
    val fetchedAt: Long,
)

data class BackupLanguage(val song: String, val language: String, val source: String)

data class BackupQueue(
    val songs: List<String>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean,
    val baseOrder: List<String>?,
    val context: String?,
    val radioSeed: String?,
)
