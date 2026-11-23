package app.attic.player.home

import android.util.Log
import app.attic.player.AtticApp
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.data.HistoryEntity
import app.attic.player.data.Library
import app.attic.player.playback.ListenTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Home's shelves and taste mixes, worked out on a background thread and held here so Home only reads
 * them. The shelves are cheap and rebuilt on each [refresh]; the mixes are built once a day, kept in
 * memory and on disk, and only rebuilt early while there were too few songs to make any.
 */
class HomeFeed(private val app: AtticApp) {
    /** Song keys; the UI maps them to tracks. Equal values aren't emitted again, so Home doesn't redraw for nothing. */
    data class Feed(
        val shelves: HomeShelves.Result = HomeShelves.Result(),
        val mixes: List<TasteMixes.Mix> = emptyList(),
    )

    private val _feed = MutableStateFlow(Feed())
    val feed: StateFlow<Feed> = _feed.asStateFlow()

    private val lock = Mutex()
    private var mixCache: MixCache? = null
    private val cacheFile: File get() = File(app.noBackupFilesDir, CACHE_FILE)

    /** Reads the log again. Called as Home appears and as you play, like Instant mixes. */
    suspend fun refresh() = withContext(Dispatchers.Default) {
        lock.withLock {
            val dao = app.database.dao()
            val library = app.library.await()
            val now = System.currentTimeMillis()
            val history = dao.allHistory()
            val liked = dao.allLiked().associate { it.trackKey to it.likedAt }
            val shelves = HomeShelves.build(
                songs = library.tracks.map { HomeShelves.Song(it.key, it.dateAddedSec) },
                played = history.map { HomeShelves.Played(it.trackKey, it.lastPlayedAt, it.playCount) },
                recentListens = dao.listensSince(now - HomeShelves.ON_REPEAT_DAYS * DAY_MS)
                    .map { HomeShelves.Listen(it.trackKey, it.startedAt, it.outcome) },
                liked = liked,
                nowMs = now,
            )
            _feed.value = Feed(shelves, mixes(library, history, liked.keys))
        }
    }

    private suspend fun mixes(library: Library, history: List<HistoryEntity>, liked: Set<String>): List<TasteMixes.Mix> {
        val dao = app.database.dao()
        val today = LocalDate.now().toEpochDay()
        val analysed = dao.analyzedKeysNow(SongAnalyzer.MODEL_VERSION).toSet()
        val seedsNow = (history.map { it.trackKey } + liked).toSet().count { it in analysed && it in library.trackByKey }
        val cached = mixCache ?: readCache()
        if (cached != null && cached.isCurrent(today, seedsNow)) {
            mixCache = cached
            return cached.mixes
        }
        // Too few played or liked songs analysed for any mix: say so without reading every embedding.
        if (seedsNow < TasteMixes.MIN_SEEDS) {
            return MixCache(today, seedsNow, emptyList()).also { mixCache = it }.mixes
        }

        val vectors = dao.embeddings(SongAnalyzer.MODEL_VERSION).associate { it.trackKey to SongAnalyzer.toFloats(it.embedding) }
        val plays = history.associate { it.trackKey to it.playCount }
        val skips = dao.outcomeCounts()
            .filter { it.outcome == ListenTracker.SKIPPED || it.outcome == ListenTracker.REMOVED }
            .groupBy { it.trackKey }
            .mapValues { (_, rows) -> rows.sumOf { it.total } }
        val songs = library.tracks.map { track ->
            TasteMixes.Song(
                key = track.key,
                vector = vectors[track.key],
                plays = plays[track.key] ?: 0,
                skips = skips[track.key] ?: 0,
                liked = track.key in liked,
                artistKey = track.artistKeys.firstOrNull(),
            )
        }
        val started = System.nanoTime()
        val built = MixCache(today, seedsNow, TasteMixes.build(songs, today))
        Log.i(TAG, "Built ${built.mixes.size} mixes from $seedsNow songs in ${(System.nanoTime() - started) / 1_000_000} ms")
        mixCache = built
        writeCache(built)
        return built.mixes
    }

    private suspend fun readCache(): MixCache? = withContext(Dispatchers.IO) {
        runCatching { cacheFile.takeIf { it.isFile }?.readText()?.let(MixCache::decode) }.getOrNull()
    }

    private suspend fun writeCache(cache: MixCache) = withContext(Dispatchers.IO) {
        runCatching { cacheFile.writeText(cache.encode()) }.onFailure { Log.w(TAG, "Couldn't save the mixes", it) }
    }

    private companion object {
        const val TAG = "HomeFeed"
        const val CACHE_FILE = "taste-mixes.txt"
        const val DAY_MS = 86_400_000L
    }
}
