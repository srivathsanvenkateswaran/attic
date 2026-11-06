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
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class BulkProgress(val done: Int, val total: Int, val found: Int, val running: Boolean)

enum class LyricsResult { Found, NotFound, Offline }

/**
 * Lyrics come from LRCLIB, a free service with no account or key. A request sends only the song's
 * title, artist, album and length. Once saved in Room, lyrics work with no connection.
 */
class LyricsRepository(
    private val dao: MusicDao,
    private val scope: CoroutineScope,
) {
    private val _bulk = MutableStateFlow(BulkProgress(0, 0, 0, running = false))
    val bulk: StateFlow<BulkProgress> = _bulk.asStateFlow()
    private var bulkJob: Job? = null

    fun observe(key: String): Flow<LyricsEntity?> = dao.lyrics(key)

    fun savedKeys(): Flow<List<String>> = dao.lyricsFoundKeys()

    suspend fun download(track: Track): LyricsResult = withContext(Dispatchers.IO) {
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

    /** Fetches lyrics for every song without them. Stops at the first connection failure. */
    fun downloadAll(tracks: List<Track>) {
        if (bulkJob?.isActive == true) return
        bulkJob = scope.launch {
            val have = withContext(Dispatchers.IO) { dao.lyricsCheckedKeys().toSet() }
            val todo = tracks.filter { it.key !in have }
            var found = 0
            _bulk.value = BulkProgress(0, todo.size, 0, running = true)
            for ((index, track) in todo.withIndex()) {
                when (download(track)) {
                    LyricsResult.Found -> found++
                    LyricsResult.NotFound -> Unit
                    LyricsResult.Offline -> break
                }
                _bulk.value = BulkProgress(index + 1, todo.size, found, running = true)
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
            connection.setRequestProperty("User-Agent", "Attic/1.0 (personal offline music player)")
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
    }
}
