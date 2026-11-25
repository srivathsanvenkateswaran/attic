package app.attic.player.home

import android.util.Log
import app.attic.player.AtticApp
import app.attic.player.analysis.MoodScorer
import app.attic.player.analysis.MoodSignals
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.analysis.StyleCodec
import app.attic.player.data.GenreIndex
import app.attic.player.data.HistoryEntity
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Library
import app.attic.player.data.TitleCleaner
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
 * Home's shelves, taste mixes and mood mixes, worked out on a background thread and held here so Home
 * only reads them. The shelves are cheap and rebuilt on each [refresh]; the taste mixes are built once
 * a day, kept in memory and on disk, and only rebuilt early while there were too few songs to make
 * any. The mood mixes are built once a day too, and again the same day when your picks change or
 * more songs get mood scores (docs/features/mood-mixes.md).
 */
class HomeFeed(private val app: AtticApp) {
    /** Song keys; the UI maps them to tracks. Equal values aren't emitted again, so Home doesn't redraw for nothing. */
    data class Feed(
        val shelves: HomeShelves.Result = HomeShelves.Result(),
        val mixes: List<TasteMixes.Mix> = emptyList(),
        /** Every mood that has a mix today, in [Mood] order. Home narrows it to your picks; Search shows all. */
        val moodMixes: List<MoodMixes.Mix> = emptyList(),
        /** False until the first refresh has worked out the mood mixes, so "none today" and "not yet" differ. */
        val moodsBuilt: Boolean = false,
    )

    private val _feed = MutableStateFlow(Feed())
    val feed: StateFlow<Feed> = _feed.asStateFlow()

    private val lock = Mutex()
    private var mixCache: MixCache? = null
    private val cacheFile: File get() = File(app.noBackupFilesDir, CACHE_FILE)
    private var moodCache: MoodMixCache? = null
    private val moodCacheFile: File get() = File(app.noBackupFilesDir, MOOD_CACHE_FILE)

    /** The songs that fit each mood, best first, from the last build. Mood radio picks only from these. */
    private var moodFits: Map<Mood, List<String>>? = null

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
            val tasteMixes = mixes(library, history, liked.keys)
            // Songs analysed since the last refresh, or before mood scores existed, get them now: no decode.
            app.moodScorer.scoreMissing()
            _feed.value = Feed(shelves, tasteMixes, moodMixes(library, liked.keys, now), moodsBuilt = true)
        }
    }

    /**
     * The songs that fit [mood], best first, as the day's mixes were built from. Empty before anything
     * has mood scores.
     */
    suspend fun moodFits(mood: Mood): List<String> = withContext(Dispatchers.Default) {
        lock.withLock {
            moodFits?.let { return@withLock it[mood].orEmpty() }
            val library = app.library.await()
            val liked = app.database.dao().allLiked().mapTo(HashSet()) { it.trackKey }
            val candidates = moodCandidates(library, liked, System.currentTimeMillis()) ?: return@withLock emptyList()
            moodFits = candidates.mapValues { (_, list) -> list.map { it.song.key } }
            moodFits!![mood].orEmpty()
        }
    }

    private fun picks(): MoodMixes.Picks {
        val interests = app.settings.interests.value
        return MoodMixes.Picks(
            moods = interests.moods.mapNotNull(Mood::fromId),
            genres = interests.genres.toSet(),
            languages = interests.languages.toSet(),
        )
    }

    private suspend fun moodMixes(library: Library, liked: Set<String>, now: Long): List<MoodMixes.Mix> {
        val dao = app.database.dao()
        val today = LocalDate.now().toEpochDay()
        val scored = dao.moodCount(MoodScorer.VERSION)
        val inputs = MoodMixCache.inputs(picks(), MoodScorer.VERSION, scored)
        val cached = moodCache ?: readMoodCache()
        if (cached != null && cached.isCurrent(today, inputs)) {
            moodCache = cached
            return cached.mixes
        }
        if (scored < MoodMixes.MIN_SONGS) return MoodMixCache(today, inputs, emptyList()).also { moodCache = it }.mixes

        val started = System.nanoTime()
        val candidates = moodCandidates(library, liked, now).orEmpty()
        moodFits = candidates.mapValues { (_, list) -> list.map { it.song.key } }
        val pools = candidates.mapValues { (_, list) -> MoodMixes.pool(list) }
        // Only the pools' embeddings are read, to keep one version of each recording.
        val poolKeys = pools.values.flatten().map { it.song.key }.distinct()
        val vectors = poolKeys.chunked(SQL_CHUNK).flatMap { dao.embeddingsFor(SongAnalyzer.MODEL_VERSION, it) }
            .associate { it.trackKey to SongAnalyzer.toFloats(it.embedding) }
        val mixes = pools.mapNotNull { (mood, pool) -> MoodMixes.mix(mood, pool, today, vectors) }
        Log.i(TAG, "Built ${mixes.size} mood mixes from $scored songs in ${(System.nanoTime() - started) / 1_000_000} ms")
        val built = MoodMixCache(today, inputs, mixes)
        moodCache = built
        writeMoodCache(built)
        return mixes
    }

    /**
     * Every mood's fitting songs, scored. Genres and languages are only worked out when some are
     * picked, since otherwise they score nothing. Null when no song has mood scores.
     */
    private suspend fun moodCandidates(library: Library, liked: Set<String>, now: Long): Map<Mood, List<MoodMixes.Candidate>>? {
        val dao = app.database.dao()
        val rows = dao.moods(MoodScorer.VERSION)
        if (rows.isEmpty()) return null
        val picks = picks()
        val genres: (String) -> Set<String> = if (picks.genres.isEmpty()) {
            { emptySet() }
        } else {
            val styles = dao.stylesNow(SongAnalyzer.MODEL_VERSION).associate { it.trackKey to StyleCodec.decode(it.top) }
            val labels = app.genres.labels
            val genresOf: (String) -> Set<String> = { key ->
                library.trackByKey[key]?.let { track ->
                    GenreIndex.songGenres(track, styles[key], labels).map(TitleCleaner::normalize).toSet()
                }.orEmpty()
            }
            genresOf
        }
        val language: (String) -> String? = if (picks.languages.isEmpty()) {
            { null }
        } else {
            val stored = dao.storedLanguages().associate { it.trackKey to it.language }
            val lyrics = dao.lyricsTexts().associateBy { it.trackKey }
            val languageOf: (String) -> String? = { key ->
                library.trackByKey[key]?.let { track ->
                    val text = lyrics[key]
                    LanguageLabels.resolve(track.folder, track.album, stored[key], text?.plain ?: text?.synced, text?.instrumental == true)
                }
            }
            languageOf
        }
        val songs = rows.mapNotNull { row ->
            val track = library.trackByKey[row.trackKey] ?: return@mapNotNull null
            MoodMixes.Song(
                key = track.key,
                signals = MoodSignals.decode(row.scores),
                language = language(track.key),
                genres = genres(track.key),
                artistKey = track.artistKeys.firstOrNull(),
                albumKey = track.albumKey,
                liked = track.key in liked,
            )
        }
        val listens = dao.listens().map { MoodMixes.Listen(it.trackKey, it.startedAt, it.outcome) }
        return MoodMixes.candidates(songs, picks, listens, now)
    }

    private suspend fun readMoodCache(): MoodMixCache? = withContext(Dispatchers.IO) {
        runCatching { moodCacheFile.takeIf { it.isFile }?.readText()?.let(MoodMixCache::decode) }.getOrNull()
    }

    private suspend fun writeMoodCache(cache: MoodMixCache) = withContext(Dispatchers.IO) {
        runCatching { moodCacheFile.writeText(cache.encode()) }.onFailure { Log.w(TAG, "Couldn't save the mood mixes", it) }
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
        const val MOOD_CACHE_FILE = "mood-mixes.txt"

        /** Keys per IN query, under SQLite's limit on bound values. */
        const val SQL_CHUNK = 500
        const val DAY_MS = 86_400_000L
    }
}
