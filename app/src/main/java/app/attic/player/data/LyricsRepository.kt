package app.attic.player.data

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class BulkProgress(
    val done: Int,
    val total: Int,
    val found: Int,
    val running: Boolean,
    /** Songs that failed on a connection error. They have no saved row, so a later run retries them. */
    val failed: Int = 0,
    val gaveUp: Boolean = false,
)

enum class LyricsResult { Found, NotFound, Offline }

/**
 * Lyrics come first from the phone: a `.lrc` file beside the song, then the song's own tags (see
 * [LyricsPriority]). Those are read with no network and saved in Room like downloaded ones.
 * Songs without them can be looked up on LRCLIB, a free service with no account or key. A request
 * sends only the song's title, artist, album and length. Once saved, lyrics work with no connection.
 */
class LyricsRepository(
    private val dao: MusicDao,
    private val scope: CoroutineScope,
    /** LRCLIB asks clients to name themselves with a version and a homepage. */
    private val userAgent: String,
    private val finder: LocalLyricsFinder,
) {
    /** One library-wide read of local lyrics at a time. Single songs don't wait for it. */
    private val sweep = Mutex()

    private val _bulk = MutableStateFlow(BulkProgress(0, 0, 0, running = false))
    val bulk: StateFlow<BulkProgress> = _bulk.asStateFlow()
    private var bulkJob: Job? = null

    fun observe(key: String): Flow<LyricsEntity?> = dao.lyrics(key)

    fun savedKeys(): Flow<List<String>> = dao.lyricsFoundKeys()

    /**
     * Reads lyrics from every song's files and saves what it finds. Files unchanged since the last
     * read, and whose `.lrc` file hasn't come, gone or changed, are skipped.
     */
    suspend fun syncLocal(tracks: List<Track>) = sweep.withLock {
        withContext(Dispatchers.IO) {
            val files = safely { finder.audioFiles() } ?: return@withContext
            val sidecars = safely { finder.sidecars() } ?: return@withContext
            val checks = dao.localLyricsChecks().associateBy { it.trackKey }
            for (track in tracks) {
                val file = files[track.mediaId] ?: continue
                syncOne(track, file, sidecars[finder.sidecarKey(file)], checks[track.key])
            }
        }
    }

    /** [syncLocal] for one song, such as the one that just started playing. */
    suspend fun syncLocal(track: Track) = withContext(Dispatchers.IO) {
        val file = safely { finder.audioFiles(track.mediaId) }?.get(track.mediaId) ?: return@withContext
        val sidecars = safely { finder.sidecars(inFolderOf = file) } ?: return@withContext
        syncOne(track, file, sidecars[finder.sidecarKey(file)], dao.localLyricsCheck(track.key))
    }

    private suspend fun syncOne(track: Track, file: LocalLyricsFinder.AudioFile, sidecar: LocalLyricsFinder.Sidecar?, check: LocalLyricsCheckEntity?) {
        if (check != null && check.audioModifiedSec == file.modifiedSec && check.sidecarModifiedSec == sidecar?.modifiedSec) return
        val sidecarText = sidecar?.let { safely { finder.readSidecar(it) } }
        val embedded = safely { finder.readEmbedded(file) } ?: EmbeddedLyrics.None
        val local = LyricsPriority.local(sidecarText, embedded)
        when (val update = LyricsPriority.update(local, dao.lyricsNow(track.key))) {
            is LyricsUpdate.Save -> dao.saveLyrics(
                LyricsEntity(
                    trackKey = track.key,
                    synced = update.lyrics.synced,
                    plain = update.lyrics.plain,
                    instrumental = false,
                    found = true,
                    fetchedAt = System.currentTimeMillis(),
                    source = update.lyrics.source.id,
                ),
            )
            LyricsUpdate.Remove -> dao.deleteLyrics(track.key)
            LyricsUpdate.Keep -> Unit
        }
        dao.saveLocalLyricsCheck(LocalLyricsCheckEntity(track.key, file.modifiedSec, sidecar?.modifiedSec))
    }

    /** A file that can't be read, or a missing permission, just means no local lyrics. */
    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    /** Lyrics on the phone are used before asking LRCLIB, so a song with them never goes online. */
    suspend fun download(track: Track): LyricsResult = withContext(Dispatchers.IO) {
        syncLocal(track)
        dao.lyricsNow(track.key)?.let { saved ->
            if (saved.found && LyricsSource.of(saved.source) != LyricsSource.Lrclib) return@withContext LyricsResult.Found
        }
        fetch(track)
    }

    private suspend fun fetch(track: Track): LyricsResult = withContext(Dispatchers.IO) {
        val match = try {
            exact(track) ?: searched(track)
        } catch (_: IOException) {
            return@withContext LyricsResult.Offline
        }
        dao.saveLyrics(
            LyricsEntity(
                trackKey = track.key,
                synced = match?.synced?.takeIf { it.isNotBlank() },
                plain = match?.plain?.takeIf { it.isNotBlank() },
                instrumental = match?.instrumental == true,
                found = match != null,
                fetchedAt = System.currentTimeMillis(),
            ),
        )
        if (match != null) LyricsResult.Found else LyricsResult.NotFound
    }

    /**
     * Fetches lyrics for every song without them, giving up only if the connection looks gone.
     * Songs with lyrics in their files are read first and skipped, like songs already checked.
     */
    fun downloadAll(tracks: List<Track>) {
        if (bulkJob?.isActive == true) return
        bulkJob = scope.launch {
            _bulk.value = BulkProgress(0, 0, 0, running = true)
            syncLocal(tracks)
            val have = withContext(Dispatchers.IO) { dao.lyricsCheckedKeys().toSet() }
            val todo = tracks.filter { it.key !in have }
            var found = 0
            var failed = 0
            var failuresInARow = 0
            _bulk.value = BulkProgress(0, todo.size, 0, running = true)
            for ((index, track) in todo.withIndex()) {
                when (fetch(track)) {
                    LyricsResult.Found -> {
                        found++
                        failuresInARow = 0
                    }
                    LyricsResult.NotFound -> failuresInARow = 0
                    LyricsResult.Offline -> {
                        failed++
                        failuresInARow++
                    }
                }
                _bulk.value = BulkProgress(index + 1, todo.size, found, running = true, failed = failed)
                if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
                    _bulk.value = _bulk.value.copy(gaveUp = true)
                    break
                }
            }
            _bulk.value = _bulk.value.copy(running = false)
        }
    }

    private fun exact(track: Track): LyricsCandidate? {
        val query = buildString {
            append("artist_name=").append(Uri.encode(track.artist.substringBefore(',').substringBefore(" & ")))
            append("&track_name=").append(Uri.encode(track.title))
            track.album?.let { append("&album_name=").append(Uri.encode(it)) }
            append("&duration=").append(track.durationMs / 1000)
        }
        val body = get("$BASE/get?$query") ?: return null
        return LyricsMatcher.best(listOf(candidate(JSONObject(body))), track.durationMs)
    }

    private fun searched(track: Track): LyricsCandidate? {
        val query = Uri.encode("${track.title} ${track.artist.substringBefore(',')}")
        val body = get("$BASE/search?q=$query") ?: return null
        val rows = JSONArray(body)
        return LyricsMatcher.best(List(rows.length()) { candidate(rows.getJSONObject(it)) }, track.durationMs)
    }

    private fun candidate(json: JSONObject) = LyricsCandidate(
        trackName = json.optString("trackName"),
        artistName = json.optString("artistName"),
        durationSec = json.optDouble("duration", 0.0),
        synced = json.optString("syncedLyrics").takeUnless { json.isNull("syncedLyrics") },
        plain = json.optString("plainLyrics").takeUnless { json.isNull("plainLyrics") },
        instrumental = json.optBoolean("instrumental"),
    )

    /** Null on 404, meaning the service doesn't have it. Other failures throw, meaning try again later. */
    private fun get(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", userAgent)
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("Lyrics service returned ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://lrclib.net/api"
        const val TIMEOUT_MS = 12_000
        const val MAX_FAILURES_IN_A_ROW = 5
    }
}
