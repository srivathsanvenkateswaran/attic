package app.attic.player.radio

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** What the engine knows about one analysed song. [vector] must have length 1. */
class RadioSong(
    val key: String,
    val vector: FloatArray,
    val language: String?,
    val artistKey: String?,
    val albumKey: String?,
    val affinity: Float = 0f,
    /** Normalized names from the composer tag. Empty when the file has none. */
    val composerKeys: Set<String> = emptySet(),
) {
    companion object {
        const val NO_VOCALS = "none"
    }
}

/**
 * One radio session. [crossEvery] is how often a song from another language may appear. [seedVector],
 * when set, is what the session steers toward in place of the seed song's own sound: a genre's centre
 * (whose [seedKey] is the real song the session opened with) or a collection's average (whose
 * [seedKey] is [COLLECTION_SEED], no song at all).
 */
class RadioState(
    val seedKey: String,
    val crossEvery: Int,
    val recent: Set<String> = emptySet(),
    /**
     * A sound to steer toward in place of the seed song's: a genre's centre ([seedKey] is still the song
     * the session opened with) or a collection's average (see [forCollection]). Null for song radio.
     */
    val seedVector: FloatArray? = null,
    /** Songs never to pick, with every other version of them: the collection the seed came from. */
    val exclude: Set<String> = emptySet(),
    /** When set, picks keep to these languages instead of following the session's, and none crosses over. */
    val languages: Set<String>? = null,
) {
    // A collection's seed key names no song; a genre's names the song already playing, never to repeat.
    internal val playedKeys = ArrayList<String>().apply { if (seedKey != COLLECTION_SEED) add(seedKey) }
    internal val skippedKeys = HashSet<String>()
    internal var sinceCross = 0
    internal var pendingCross: String? = null

    /** Records a song as queued, so it can't be picked again and steers what comes next. */
    fun played(key: String) {
        playedKeys += key
        if (key == pendingCross) sinceCross = 0 else sinceCross++
        pendingCross = null
    }

    /** A skipped or removed song is never queued again, and the session moves away from its sound. */
    fun skipped(key: String) {
        skippedKeys += key
    }

    companion object {
        /**
         * A session seeded by a collection (a playlist, Favourites) rather than a song: its seed is the
         * normalised average of the collection's sounds, its songs are never picked, and picks keep to
         * the collection's languages. [members] must hold at least one vector.
         */
        fun forCollection(members: List<RadioSong>, recent: Set<String> = emptySet()): RadioState {
            val languages = members.mapNotNull { it.language?.takeUnless { l -> l == RadioSong.NO_VOCALS } }.toSet()
            return RadioState(
                seedKey = COLLECTION_SEED,
                crossEvery = Int.MAX_VALUE,
                recent = recent,
                seedVector = RadioEngine.centroid(members.map { it.vector }),
                exclude = members.map { it.key }.toSet(),
                languages = languages.ifEmpty { null },
            )
        }

        /** The seed key of a collection session. No song has it. */
        const val COLLECTION_SEED = "collection:"
    }
}

/**
 * Picks the next song: nearest in sound to the session so far, then adjusted for composer and taste,
 * filtered to the session's language and to one version of each song.
 */
class RadioEngine(
    private val songs: List<RadioSong>,
    /** Whether two close-sounding songs are really one song. Slow, so it runs on as few pairs as possible. */
    private val isSameSong: (RadioSong, RadioSong) -> Boolean,
) {
    private val byKey = songs.associateBy { it.key }
    private val sameSongAnswers = HashMap<Pair<String, String>, Boolean>()

    /** Nothing, if no song is close enough. [queueWouldEnd] accepts the closest left, so music keeps playing. */
    fun next(state: RadioState, queueWouldEnd: Boolean = false): RadioSong? {
        // Genre radio keeps its opening song's details (language, artist) and steers by the genre's centre;
        // a collection's seed is no song, so it is only the vector.
        val seed = byKey[state.seedKey]
            ?: state.seedVector?.let { RadioSong(state.seedKey, it, language = null, artistKey = null, albumKey = null) }
            ?: return null
        val played = state.playedKeys.mapNotNull(byKey::get)
        val sessionLanguage = sessionLanguage(played) ?: seed.language
        val query = query(state.seedVector ?: seed.vector, played, state.skippedKeys)
        // A collection's own songs count as played for spotting other versions of them.
        val heard = if (state.exclude.isEmpty()) played else played + state.exclude.mapNotNull(byKey::get)

        val ranked = songs
            .filter { it.key !in state.playedKeys && it.key !in state.skippedKeys && it.key !in state.exclude }
            .map { Candidate(it, dot(query, it.vector)) }
            .sortedByDescending { it.scoreFor(seed) }

        if (state.sinceCross + 1 >= state.crossEvery) {
            val cross = firstUsable(ranked, heard) { c ->
                isOtherLanguage(c.song, sessionLanguage) && c.similarity >= CROSS_MIN_SIMILARITY && fitsCaps(c.song, played) && c.song.key !in state.recent
            }
            if (cross != null) {
                state.pendingCross = cross.key
                return cross
            }
        }

        val close = { c: Candidate -> c.similarity >= MIN_SIMILARITY }
        val inLanguage = { c: Candidate ->
            when {
                c.song.language == RadioSong.NO_VOCALS -> c.similarity >= CROSS_MIN_SIMILARITY
                state.languages != null -> c.song.language == null || c.song.language in state.languages
                else -> isSessionLanguage(c.song, sessionLanguage)
            }
        }
        return firstUsable(ranked, heard) { close(it) && inLanguage(it) && fitsCaps(it.song, played) && it.song.key !in state.recent }
            ?: firstUsable(ranked, heard) { close(it) && inLanguage(it) && fitsCaps(it.song, played) }
            ?: firstUsable(ranked, heard) { close(it) && inLanguage(it) }
            ?: if (queueWouldEnd) firstUsable(ranked, heard) { close(it) } ?: firstUsable(ranked, heard) { true } else null
    }

    private inner class Candidate(val song: RadioSong, val similarity: Float) {
        fun scoreFor(seed: RadioSong): Float {
            var score = similarity + song.affinity
            if (sameComposer(song, seed)) score += COMPOSER_BOOST
            if (song.language == null) score -= UNLABELLED_PENALTY
            return score
        }
    }

    /**
     * Shares a composer with the seed. With a composer tag on both songs that tag decides; otherwise
     * the lead artist stands in, which for a film soundtrack filed by music director is the composer.
     */
    private fun sameComposer(song: RadioSong, seed: RadioSong): Boolean =
        if (song.composerKeys.isNotEmpty() && seed.composerKeys.isNotEmpty()) {
            song.composerKeys.any { it in seed.composerKeys }
        } else {
            song.artistKey != null && song.artistKey == seed.artistKey
        }

    private fun firstUsable(ranked: List<Candidate>, played: List<RadioSong>, allowed: (Candidate) -> Boolean): RadioSong? =
        ranked.firstOrNull { allowed(it) && played.none { p -> same(p, it.song) } }?.song

    private fun same(played: RadioSong, candidate: RadioSong): Boolean {
        val similarity = dot(played.vector, candidate.vector)
        if (similarity >= DUPLICATE_SIMILARITY) return true
        if (similarity < VERSION_MIN_SIMILARITY) return false
        val pair = if (played.key < candidate.key) played.key to candidate.key else candidate.key to played.key
        return sameSongAnswers.getOrPut(pair) { isSameSong(played, candidate) }
    }

    private fun isSessionLanguage(song: RadioSong, session: String?): Boolean =
        song.language == null || session == null || song.language == session

    private fun isOtherLanguage(song: RadioSong, session: String?): Boolean =
        song.language != null && song.language != RadioSong.NO_VOCALS && song.language != session

    private fun fitsCaps(song: RadioSong, played: List<RadioSong>): Boolean {
        if (song.albumKey != null && played.count { it.albumKey == song.albumKey } >= MAX_PER_ALBUM) return false
        if (song.artistKey != null && played.size >= MAX_IN_A_ROW &&
            played.takeLast(MAX_IN_A_ROW).all { it.artistKey == song.artistKey }
        ) return false
        return true
    }

    /** The most common known language in the last few songs. A tie goes to the most recent. */
    private fun sessionLanguage(played: List<RadioSong>): String? =
        played.takeLast(LANGUAGE_WINDOW)
            .mapNotNull { it.language?.takeUnless { l -> l == RadioSong.NO_VOCALS } }
            .asReversed()
            .let { recentFirst ->
                recentFirst.distinct().maxByOrNull { language -> recentFirst.count { it == language } }
            }

    /** Half the seed, half recent songs with newer ones counting more. Skipped songs count against. */
    private fun query(seed: FloatArray, played: List<RadioSong>, skipped: Set<String>): FloatArray {
        val out = seed.copyOf()
        for (i in out.indices) out[i] *= SEED_WEIGHT
        var weight = (1 - SEED_WEIGHT) / 2
        for (song in played.asReversed()) {
            val signed = if (song.key in skipped) -weight else weight
            for (i in out.indices) out[i] += signed * song.vector[i]
            weight /= 2
        }
        var norm = 0f
        for (value in out) norm += value * value
        norm = sqrt(norm)
        if (norm > 0) for (i in out.indices) out[i] /= norm
        return out
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    companion object {
        /** The normalised mean of unit vectors: the sound a set of songs shares. */
        fun centroid(vectors: List<FloatArray>): FloatArray =
            Centroid.of(vectors) ?: FloatArray(vectors.first().size)

        private const val SEED_WEIGHT = 0.5f
        private const val COMPOSER_BOOST = 0.03f
        private const val UNLABELLED_PENALTY = 0.03f
        private const val CROSS_MIN_SIMILARITY = 0.72f

        /** A typical pair of songs in the test library scored 0.54, so this keeps out unrelated music. */
        private const val MIN_SIMILARITY = 0.62f
        private const val VERSION_MIN_SIMILARITY = 0.80f
        private const val DUPLICATE_SIMILARITY = 0.985f
        private const val MAX_PER_ALBUM = 2
        private const val MAX_IN_A_ROW = 2
        private const val LANGUAGE_WINDOW = 5
    }
}

/** The middle of a group of songs' sound, for radio that starts from a genre rather than one song. */
object Centroid {
    /** The normalised mean of unit [vectors], or null for none (or vectors that cancel out). */
    fun of(vectors: List<FloatArray>): FloatArray? {
        val first = vectors.firstOrNull() ?: return null
        val sum = FloatArray(first.size)
        for (vector in vectors) for (i in sum.indices) sum[i] += vector[i]
        var norm = 0f
        for (value in sum) norm += value * value
        norm = sqrt(norm)
        if (norm == 0f) return null
        for (i in sum.indices) sum[i] /= norm
        return sum
    }

    /** The key whose vector is closest to [centre], the song a genre's radio opens with. */
    fun nearest(centre: FloatArray, vectors: Map<String, FloatArray>): String? =
        vectors.maxByOrNull { (_, vector) -> centre.indices.sumOf { (centre[it] * vector[it]).toDouble() } }?.key
}

/** Turns the listening log into the numbers radio uses. */
object RadioTaste {
    private const val BASE_SHARE = 0.1
    private const val SMOOTHING = 4.0

    /** One song in this many comes from another language, adjusted by how those picks went. */
    fun crossEvery(finished: Int, skipped: Int): Int {
        val share = BASE_SHARE * (finished + SMOOTHING) / (skipped + SMOOTHING)
        return (1 / share).roundToInt().coerceIn(3, 30)
    }

    /** A small nudge up or down, kept below the differences that sound similarity makes. */
    fun affinity(liked: Boolean, finished: Int, skipped: Int): Float {
        var score = if (liked) 0.03f else 0f
        score += 0.01f * min(finished, 3)
        score -= 0.03f * min(skipped, 3)
        return score
    }
}
