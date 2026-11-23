package app.attic.player.radio

import android.util.Log
import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.analysis.MelodyMatch
import app.attic.player.analysis.SongAnalyzer
import app.attic.player.analysis.StyleCodec
import app.attic.player.data.LanguageLabels
import app.attic.player.data.LibraryRepository
import app.attic.player.data.SongAnalysisEntity
import app.attic.player.data.SongLanguageEntity
import app.attic.player.data.SongStylesEntity
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track
import app.attic.player.playback.ListenTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/** Reads the optional labels file an owner can place in the app's folder, `path<TAB>language` per line. */
object LanguageImport {
    private const val FILE_NAME = "language-labels.tsv"
    private const val SOURCE_IMPORTED = "imported"
    const val SOURCE_MANUAL = "manual"

    suspend fun run(app: AtticApp) = withContext(Dispatchers.IO) {
        val file = app.getExternalFilesDir(null)?.let { File(it, FILE_NAME) }?.takeIf { it.isFile } ?: return@withContext
        val dao = app.database.dao()
        val manual = dao.storedLanguages().filter { it.source == SOURCE_MANUAL }.map { it.trackKey }.toSet()
        val rows = LanguageLabels.parseImport(file.readText())
            .filterKeys { it !in manual }
            .map { (key, language) -> SongLanguageEntity(key, language, SOURCE_IMPORTED) }
        dao.saveLanguages(rows)
        Log.i("LanguageImport", "Imported ${rows.size} labels")
    }
}

/** Runs the radio session the playback service asks it to keep filled. */
class RadioController(private val app: AtticApp) {
    private class Session(val seedKey: String, val contextName: String) {
        /** False until the player reports this context, so the old queue's events can't end a new session. */
        @Volatile var seen = false
        var engine: RadioEngine? = null
        var analyzedCount = -1
        var state: RadioState? = null
        val tracks = HashMap<String, Track>()
        val crossPicks = HashSet<String>()
        val pendingSkips = ArrayList<String>()
    }

    @Volatile private var session: Session? = null

    /** A queue you cleared, or a seed radio couldn't read. Autoplay leaves it alone until something else plays. */
    @Volatile private var stoppedContext: String? = null

    private val _speedDial = MutableStateFlow<List<Track>>(emptyList())

    /** The Home grid. Empty until [refreshSpeedDial] has run once. */
    val speedDial: StateFlow<List<Track>> = _speedDial.asStateFlow()

    /** Reads likes, plays and sound profiles again. Cheap enough to run each time Home appears. */
    suspend fun refreshSpeedDial() = withContext(Dispatchers.Default) {
        val dao = app.database.dao()
        val library = app.library.await()
        val vectors = dao.embeddings(SongAnalyzer.MODEL_VERSION).associate { it.trackKey to SongAnalyzer.toFloats(it.embedding) }
        val outcomes = dao.outcomeCounts().groupBy { it.trackKey }
        val played = dao.playCounts().associate { it.trackKey to it.playCount }
        val liked = app.userData.likedSet.value
        val songs = library.tracks.map { track ->
            val counts = outcomes[track.key].orEmpty()
            SpeedDial.Song(
                key = track.key,
                vector = vectors[track.key],
                plays = played[track.key] ?: 0,
                skips = counts.filter { it.outcome == ListenTracker.SKIPPED || it.outcome == ListenTracker.REMOVED }.sumOf { it.total },
                liked = track.key in liked,
                artistKey = track.artistKeys.firstOrNull(),
                albumKey = track.albumKey,
            )
        }
        val today = System.currentTimeMillis() / DAY_MS
        _speedDial.value = SpeedDial.pick(songs, today, SPEED_DIAL_SIZE) { a, b -> sameMelody(a.key, b.key) }.mapNotNull { library.trackByKey[it.key] }
    }

    fun analyzedKeys(): Flow<List<String>> = app.database.dao().analyzedKeys(SongAnalyzer.MODEL_VERSION)

    /** Plays the song straight away. The service then asks for the songs that follow it. */
    fun start(track: Track) {
        val started = Session(track.key, "$CONTEXT_PREFIX${track.title}")
        session = started
        stoppedContext = null
        app.player.play(listOf(track), 0, started.contextName, shuffled = false)
    }

    /**
     * Takes over when an ordinary queue reaches its last song, seeded from that song. Returns the radio's
     * context name for the player to show, or null when autoplay should stay out of this queue.
     */
    fun continueFrom(track: Track, fromContext: String?): String? {
        if (fromContext != null && fromContext == stoppedContext) return null
        val name = "$CONTEXT_PREFIX${track.title}"
        if (name == stoppedContext) return null
        session = Session(track.key, name).apply { seen = true }
        return name
    }

    fun resume(seedKey: String, contextName: String) {
        if (session == null) session = Session(seedKey, contextName).apply { seen = true }
    }

    /** [contextName] is the queue that shouldn't be refilled by autoplay, such as one you just cleared. */
    fun stop(contextName: String? = null) {
        session = null
        stoppedContext = contextName
    }

    fun seedFor(contextName: String?): String? = session?.takeIf { it.contextName == contextName }?.seedKey

    /** Ends the session once the player has moved on to something else. */
    fun isActive(contextName: String?): Boolean {
        if (contextName != stoppedContext) stoppedContext = null
        val current = session ?: return false
        if (contextName == current.contextName) {
            current.seen = true
            return true
        }
        if (current.seen) session = null
        return false
    }

    fun isCrossPick(key: String): Boolean = session?.crossPicks?.contains(key) == true

    fun skipped(key: String) {
        val current = session ?: return
        synchronized(current) {
            current.state?.skipped(key) ?: current.pendingSkips.add(key)
        }
    }

    /** The next [count] songs, leaving out anything already in the queue. */
    suspend fun pick(contextName: String, inQueue: List<String>, count: Int, queueWouldEnd: Boolean): List<Track> = withContext(Dispatchers.Default) {
        val current = session?.takeIf { it.contextName == contextName } ?: return@withContext emptyList()
        if (current.engine == null && !prepare(current)) return@withContext emptyList()
        // Background analysis may have reached more songs since this session began.
        val analyzedNow = app.database.dao().analyzedKeysNow(SongAnalyzer.MODEL_VERSION).size
        if (analyzedNow != current.analyzedCount) {
            val songs = loadSongs { synchronized(current) { current.tracks[it.key] = it } }
            synchronized(current) {
                current.engine = RadioEngine(songs) { a, b -> sameMelody(a.key, b.key) }
                current.analyzedCount = analyzedNow
            }
        }
        val picks = ArrayList<Track>(count)
        synchronized(current) {
            val engine = current.engine!!
            val state = current.state!!
            inQueue.filter { it !in state.playedKeys }.forEach(state::played)
            repeat(count) {
                val next = engine.next(state, queueWouldEnd && picks.isEmpty()) ?: return@repeat
                if (state.pendingCross == next.key) current.crossPicks += next.key
                state.played(next.key)
                current.tracks[next.key]?.let(picks::add)
            }
        }
        picks
    }

    private suspend fun prepare(current: Session): Boolean {
        val dao = app.database.dao()
        val library = app.library.await()
        if (ensureAnalyzed(current.seedKey).not()) {
            app.messages.tryEmit(app.getString(R.string.message_radio_unreadable))
            stop(current.contextName)
            return false
        }
        val analyzedNow = dao.analyzedKeysNow(SongAnalyzer.MODEL_VERSION).size
        val songs = loadSongs { synchronized(current) { current.tracks[it.key] = it } }
        val cross = dao.crossCounts()
        val state = RadioState(
            seedKey = current.seedKey,
            crossEvery = RadioTaste.crossEvery(
                finished = cross.filter { it.outcome == ListenTracker.FINISHED }.sumOf { it.total },
                skipped = cross.filter { it.outcome == ListenTracker.SKIPPED || it.outcome == ListenTracker.REMOVED }.sumOf { it.total },
            ),
            recent = dao.recentKeys(RECENT_TO_AVOID).toSet() - current.seedKey,
        )
        synchronized(current) {
            current.pendingSkips.forEach(state::skipped)
            current.state = state
            current.engine = RadioEngine(songs) { a, b -> sameMelody(a.key, b.key) }
            current.analyzedCount = analyzedNow
        }
        if (songs.size < library.tracks.size) {
            app.messages.tryEmit(app.getString(R.string.message_radio_partial, songs.size.toString(), library.tracks.size.toString()))
        }
        return true
    }

    /**
     * Smart shuffle's picks for a collection: up to [count] songs from the rest of the library that fit
     * its sound, chosen by a radio session seeded with the collection's average sound
     * ([RadioState.forCollection]). Prefers songs you haven't played lately, keeps to the collection's
     * languages and leaves out other versions of its songs. Empty when none of it has been analysed.
     */
    suspend fun smartPicks(collection: List<String>, count: Int): List<Track> = withContext(Dispatchers.Default) {
        if (count <= 0 || collection.isEmpty()) return@withContext emptyList()
        val tracks = HashMap<String, Track>()
        val songs = loadSongs { tracks[it.key] = it }
        val inCollection = collection.toSet()
        val members = songs.filter { it.key in inCollection }
        if (members.isEmpty()) return@withContext emptyList()
        val state = RadioState.forCollection(members, recent = app.database.dao().recentKeys(RECENT_TO_AVOID).toSet())
        val engine = RadioEngine(songs) { a, b -> sameMelody(a.key, b.key) }
        val picks = ArrayList<Track>(count)
        while (picks.size < count) {
            val next = engine.next(state) ?: break
            state.played(next.key)
            tracks[next.key]?.let(picks::add)
        }
        picks
    }

    /** Every analysed song as radio sees it, handing each track to [found]. Run again as analysis reaches more of the library. */
    private suspend fun loadSongs(found: (Track) -> Unit): List<RadioSong> {
        val dao = app.database.dao()
        val library = app.library.await()
        val stored = dao.storedLanguages().associate { it.trackKey to it.language }
        val lyrics = dao.lyricsTexts().associateBy { it.trackKey }
        val liked = app.userData.likedSet.value
        val outcomes = dao.outcomeCounts().groupBy { it.trackKey }
        return dao.embeddings(SongAnalyzer.MODEL_VERSION).mapNotNull { row ->
            val track = library.trackByKey[row.trackKey] ?: return@mapNotNull null
            val text = lyrics[track.key]
            val counts = outcomes[track.key].orEmpty()
            found(track)
            RadioSong(
                key = track.key,
                vector = SongAnalyzer.toFloats(row.embedding),
                language = LanguageLabels.resolve(track.folder, track.album, stored[track.key], text?.plain ?: text?.synced, text?.instrumental == true),
                artistKey = track.artistKeys.firstOrNull(),
                albumKey = track.albumKey,
                composerKeys = track.composer?.let { TitleCleaner.splitCredits(it) }.orEmpty().map(TitleCleaner::normalize).toSet(),
                affinity = RadioTaste.affinity(
                    liked = track.key in liked,
                    finished = counts.filter { it.outcome == ListenTracker.FINISHED }.sumOf { it.total },
                    skipped = counts.filter { it.outcome == ListenTracker.SKIPPED || it.outcome == ListenTracker.REMOVED }.sumOf { it.total },
                ),
            )
        }
    }

    /** Analyses the seed now if the background work hasn't reached it yet. */
    private suspend fun ensureAnalyzed(key: String): Boolean {
        val dao = app.database.dao()
        if (dao.chroma(key) != null) return true
        val track = app.library.await().trackByKey[key] ?: return false
        val analysis = SongAnalyzer(app).use { it.analyze(LibraryRepository.uriFor(track.mediaId)) } ?: return false
        dao.saveAnalysisAndStyles(
            SongAnalysisEntity(key, SongAnalyzer.MODEL_VERSION, SongAnalyzer.toBytes(analysis.embedding), analysis.chroma, System.currentTimeMillis()),
            SongStylesEntity(key, SongAnalyzer.MODEL_VERSION, StyleCodec.encode(analysis.styles)),
        )
        return true
    }

    private val chromaCache = object : LinkedHashMap<String, ByteArray>(CHROMA_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) = size > CHROMA_CACHE
    }

    private fun chroma(key: String): ByteArray = synchronized(chromaCache) {
        chromaCache.getOrPut(key) { runBlocking(Dispatchers.IO) { app.database.dao().chroma(key) } ?: ByteArray(0) }
    }

    private fun sameMelody(a: String, b: String): Boolean =
        MelodyMatch.score(chroma(a), chroma(b)) > MelodyMatch.SAME_SONG

    /** What this song's language currently resolves to, for the song menu. */
    fun languageFlow(track: Track): Flow<String?> {
        val dao = app.database.dao()
        return combine(dao.storedLanguage(track.key), dao.lyrics(track.key)) { stored, lyrics ->
            LanguageLabels.resolve(track.folder, track.album, stored, lyrics?.plain ?: lyrics?.synced, lyrics?.instrumental == true)
        }
    }

    fun setLanguage(key: String, language: String) {
        if (language.isEmpty()) return
        app.scope.launch {
            app.database.dao().saveLanguage(SongLanguageEntity(key, language, LanguageImport.SOURCE_MANUAL))
        }
    }

    private companion object {
        const val CONTEXT_PREFIX = "Radio · "
        const val RECENT_TO_AVOID = 30
        const val CHROMA_CACHE = 64
        const val SPEED_DIAL_SIZE = 9
        const val DAY_MS = 86_400_000L
    }
}
