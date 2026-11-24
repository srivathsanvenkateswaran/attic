package app.attic.player.data.backup

import android.util.Log
import androidx.room.withTransaction
import app.attic.player.data.AppSettings
import app.attic.player.data.AtticDatabase
import app.attic.player.data.LibraryRepository
import app.attic.player.data.PlaylistTrackEntity
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Turns the user's data into a [Backup] and merges one back in. Also keeps the copy that Android's
 * own backup carries: [snapshotFile], the same format gzipped, which `res/xml/data_extraction_rules.xml`
 * names as the only file to send to the cloud. The database itself isn't sent, because song analysis
 * in it can pass the 25 MB that Android allows an app.
 */
class BackupRepository(
    private val database: AtticDatabase,
    private val settings: AppSettings,
    private val library: LibraryRepository,
    private val appVersion: String?,
    filesDir: File,
    noBackupDir: File,
) {
    private val dao get() = database.dao()
    private val lock = Mutex()

    val snapshotFile = File(filesDir, "$SNAPSHOT_DIR/$SNAPSHOT_NAME")

    /** Exists once this install has looked for a snapshot restored from another phone. Never backed up. */
    private val snapshotChecked = File(noBackupDir, "backup-snapshot-checked")
    private var lastSnapshot: Int? = null

    suspend fun export(): Backup = withContext(Dispatchers.IO) {
        val tracks = dao.allPlaylistTracks().groupBy({ it.playlistId }, { it.trackKey })
        Backup(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            settings = BackupSettings(
                theme = settings.themeMode.value.name,
                uiStyle = settings.uiStyle.value.name,
                autoplay = settings.autoplay.value,
            ),
            likes = dao.allLiked().map { BackupLike(it.trackKey, it.likedAt) },
            playlists = dao.allPlaylists().map { BackupPlaylist(it.name, it.createdAt, tracks[it.id].orEmpty()) },
            plays = dao.allHistory().map { BackupPlay(it.trackKey, it.lastPlayedAt, it.playCount) },
            listens = dao.allEvents().map {
                BackupListen(it.trackKey, it.startedAt, it.listenedMs, it.durationMs, it.outcome, it.contextName, it.radioCross)
            },
            lyrics = dao.allFoundLyrics().map { BackupLyrics(it.trackKey, it.synced, it.plain, it.instrumental, it.fetchedAt) },
            languages = dao.storedLanguages().map { BackupLanguage(it.trackKey, it.language, it.source) },
            queue = dao.savedQueue()?.let { q ->
                BackupQueue(
                    songs = q.queueKeys.split('\n').filter { it.isNotEmpty() },
                    index = q.index,
                    positionMs = q.positionMs,
                    repeatMode = q.repeatMode,
                    shuffle = q.shuffle,
                    baseOrder = q.baseKeys?.split('\n')?.filter { it.isNotEmpty() },
                    context = q.contextName,
                    radioSeed = q.radioSeed,
                )
            },
        )
    }

    /** Writes a backup of everything now, as plain JSON. */
    suspend fun writeTo(out: OutputStream) = withContext(Dispatchers.IO) {
        val text = BackupCodec.encode(export())
        out.write(text.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    /** Reads a backup file. Throws [BackupReadException] if it isn't one this version can use. */
    suspend fun readFrom(input: InputStream): Backup = withContext(Dispatchers.IO) {
        BackupCodec.decode(readLimited(input))
    }

    /**
     * Merges [backup] into this phone in one transaction, so a failure changes nothing. Settings
     * are taken from the backup when [applySettings] is true.
     */
    suspend fun restore(backup: Backup, applySettings: Boolean): RestoreReport = lock.withLock {
        withContext(Dispatchers.IO) {
            val libraryKeys = library.await().trackByKey.keys
            val plan = database.withTransaction {
                val playlists = dao.allPlaylists()
                val tracks = dao.allPlaylistTracks().groupBy({ it.playlistId }, { it.trackKey })
                val existing = ExistingData(
                    likedKeys = dao.allLiked().mapTo(HashSet()) { it.trackKey },
                    playlists = playlists.map { ExistingPlaylist(it.id, it.name, tracks[it.id].orEmpty()) },
                    history = dao.allHistory().associateBy { it.trackKey },
                    listenIds = dao.eventStarts().mapTo(HashSet()) { RestorePlanner.listenId(it.trackKey, it.startedAt) },
                    lyricsFoundKeys = dao.allFoundLyrics().mapTo(HashSet()) { it.trackKey },
                    languageSources = dao.storedLanguages().associate { it.trackKey to it.source },
                    hasSavedQueue = dao.savedQueue() != null,
                )
                val plan = RestorePlanner.plan(backup, existing, libraryKeys)
                dao.insertLiked(plan.likes)
                for (playlist in plan.newPlaylists) {
                    val id = dao.insertPlaylist(playlist.entity)
                    dao.insertPlaylistTracksIfAbsent(playlist.keys.mapIndexed { i, key -> PlaylistTrackEntity(id, key, i) })
                }
                for ((id, keys) in plan.playlistAdditions) {
                    val start = dao.maxPosition(id) + 1
                    dao.insertPlaylistTracksIfAbsent(keys.mapIndexed { i, key -> PlaylistTrackEntity(id, key, start + i) })
                }
                dao.saveHistory(plan.history)
                dao.insertEvents(plan.listens)
                dao.saveLyrics(plan.lyrics)
                dao.saveLanguages(plan.languages)
                plan.queue?.let { dao.saveQueue(it) }
                plan
            }
            if (applySettings) backup.settings?.let(::applySettings)
            plan.report
        }
    }

    private fun applySettings(saved: BackupSettings) {
        ThemeMode.entries.firstOrNull { it.name == saved.theme }?.let(settings::setThemeMode)
        UiStyle.entries.firstOrNull { it.name == saved.uiStyle }?.let(settings::setUiStyle)
        saved.autoplay?.let(settings::setAutoplay)
    }

    /**
     * Refreshes the copy Android's backup carries. Skipped while a restored copy is still waiting to
     * be read, so an empty new install can't overwrite the backup it came with.
     */
    suspend fun saveSnapshot() = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!snapshotChecked.exists()) return@withContext
            try {
                val backup = export()
                val unchanged = BackupCodec.encode(backup.copy(createdAt = 0)).hashCode()
                if (unchanged == lastSnapshot && snapshotFile.isFile) return@withContext
                var bytes = BackupCodec.gzip(BackupCodec.encode(backup))
                // Lyrics can be downloaded again; the rest can't. Past the limit Android sends nothing at all.
                if (bytes.size > SNAPSHOT_LIMIT) bytes = BackupCodec.gzip(BackupCodec.encode(backup.copy(lyrics = emptyList())))
                if (bytes.size > SNAPSHOT_LIMIT) {
                    Log.w(TAG, "Backup copy is ${bytes.size} bytes, over Android's limit; not saved")
                    return@withContext
                }
                snapshotFile.parentFile?.mkdirs()
                val partial = File(snapshotFile.parentFile, "$SNAPSHOT_NAME.partial")
                partial.writeBytes(bytes)
                if (!partial.renameTo(snapshotFile)) throw IOException("Couldn't replace ${snapshotFile.name}")
                lastSnapshot = unchanged
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // IO or the database: either way the old copy stays, and the next stop tries again.
                Log.w(TAG, "Couldn't save the backup copy", e)
            }
        }
    }

    /**
     * On the first launch of an install, merges the copy Android restored from another phone, if
     * there is one. It waits for audio access so songs in other folders can be matched by name.
     */
    suspend fun restoreSnapshotOnce() {
        if (withContext(Dispatchers.IO) { snapshotChecked.exists() }) return
        if (withContext(Dispatchers.IO) { snapshotFile.isFile }) {
            library.library.first { it != null && library.hasPermission() }
            val backup = try {
                withContext(Dispatchers.IO) { BackupCodec.decode(snapshotFile.readBytes()) }
            } catch (e: BackupReadException) {
                Log.w(TAG, "Android's backup copy couldn't be read", e)
                null
            } catch (e: IOException) {
                Log.w(TAG, "Android's backup copy couldn't be read", e)
                null
            }
            if (backup != null) {
                try {
                    Log.i(TAG, "Restored Android's backup copy: ${restore(backup, applySettings = false)}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The transaction rolled back. Leave the check undone so the next launch tries again.
                    Log.w(TAG, "Couldn't restore Android's backup copy", e)
                    return
                }
            }
        }
        withContext(Dispatchers.IO) {
            snapshotChecked.parentFile?.mkdirs()
            snapshotChecked.createNewFile()
        }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > BackupCodec.MAX_BYTES) throw BackupReadException(BackupReadException.Reason.TooLarge)
        }
        return out.toByteArray()
    }

    companion object {
        private const val TAG = "Backup"
        const val SNAPSHOT_DIR = "backup"
        const val SNAPSHOT_NAME = "attic-backup.json.gz"

        /** Under the 25 MB Android's cloud backup allows an app, with room for the settings file. */
        private const val SNAPSHOT_LIMIT = 20 * 1024 * 1024
    }
}
