package app.attic.player.home

import app.attic.player.analysis.MoodSignals
import app.attic.player.data.LanguageLabels
import app.attic.player.playback.ListenTracker
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

/** The moods Attic builds mixes for, in the order they are always shown. [id] is what settings and backups store. */
enum class Mood(val id: String) {
    Chill("chill"),
    Happy("happy"),
    Energetic("energetic"),
    Focus("focus"),
    Party("party"),
    Melancholy("melancholy"),
    Romantic("romantic");

    companion object {
        fun fromId(id: String): Mood? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Mood mixes: for each mood, the songs whose mood scores fit it, ranked by fit, your picks and your
 * listening, then a day's 25 drawn from the best 50 by a generator seeded with the day. Pure
 * arithmetic over rows already read, with the time and the day passed in, so the same inputs always
 * give the same mixes. docs/features/mood-mixes.md explains every number here.
 */
object MoodMixes {
    /** What scoring needs to know about one analysed song. */
    class Song(
        val key: String,
        /** [MoodSignals.SIZE] values, NaN where missing. */
        val signals: FloatArray,
        /** As [LanguageLabels.resolve] says; null when unknown. */
        val language: String?,
        /** [app.attic.player.data.Genre.key]s. */
        val genres: Set<String>,
        val artistKey: String?,
        val albumKey: String?,
        val liked: Boolean,
    )

    /** One `listening_events` row. */
    class Listen(val key: String, val startedAt: Long, val outcome: String)

    /** The moods, genres and languages you picked. Any of them may be empty. */
    data class Picks(val moods: List<Mood> = emptyList(), val genres: Set<String> = emptySet(), val languages: Set<String> = emptySet())

    /** A song that fits a mood, with its score for the mix. */
    class Candidate(val song: Song, val fit: Float, val score: Float)

    /** One day's mix: its songs in play order. */
    data class Mix(val mood: Mood, val keys: List<String>)

    const val MIX_SIZE = 25
    const val MIN_SONGS = 8
    const val POOL_SIZE = 50
    const val FIT_FLOOR = 0.6f
    const val MAX_PER_ARTIST = 3
    const val MAX_PER_ALBUM = 2

    const val GENRE_WEIGHT = 0.10f
    const val LANGUAGE_WEIGHT = 0.10f
    const val TASTE_WEIGHT = 0.15f
    const val FRESH_WEIGHT = 0.20f

    /** A listen counts half as much after this long, so what you play now outweighs what you played months ago. */
    const val TASTE_HALF_LIFE_MS = 30 * 86_400_000L

    /** How quickly "you just heard this" wears off: half after twelve hours. */
    const val FRESH_HALF_LIFE_MS = 12 * 3_600_000L

    /** Skips that leave a song out of every mood mix, when they also outnumber its finished listens. */
    const val SKIPS_TO_EXCLUDE = 3

    /** Two files this close are copies of one recording, as in radio and taste mixes. */
    private const val COPY_SIMILARITY = 0.985f

    // ---------------------------------------------------------------- signals and fit

    /** A signal a term reads: one stored value, or the mean of some moodtheme tags. */
    class Signal(val name: String, val indices: IntArray) {
        fun of(signals: FloatArray): Float {
            var sum = 0f
            for (i in indices) {
                val value = signals[i]
                if (value.isNaN()) return Float.NaN
                sum += value
            }
            return sum / indices.size
        }
    }

    private fun single(name: String, index: Int) = Signal(name, intArrayOf(index))
    private fun tags(vararg names: String) = Signal(names.joinToString("+"), names.map(MoodSignals::tag).toIntArray())

    val HAPPY = single("happy", MoodSignals.HAPPY)
    val SAD = single("sad", MoodSignals.SAD)
    val RELAXED = single("relaxed", MoodSignals.RELAXED)
    val AGGRESSIVE = single("aggressive", MoodSignals.AGGRESSIVE)
    val PARTY = single("party", MoodSignals.PARTY)
    val DANCEABLE = single("danceable", MoodSignals.DANCEABLE)
    val INSTRUMENTAL = single("instrumental", MoodSignals.INSTRUMENTAL)
    val ENGAGEMENT = single("engagement", MoodSignals.ENGAGEMENT)
    val CALM_TAGS = tags("calm", "relaxing", "soft")
    val HAPPY_TAGS = tags("happy", "positive", "uplifting", "fun")
    val ENERGY_TAGS = tags("energetic", "upbeat", "powerful", "fast")
    val FOCUS_TAGS = tags("background", "meditative", "calm", "soundscape")
    val PARTY_TAGS = tags("party", "fun", "groovy")
    val SAD_TAGS = tags("melancholic", "sad", "emotional")
    val LOVE_TAGS = tags("romantic", "love")

    /** One part of a mood's fit: a signal's percentile, or one minus it when [inverted]. */
    class Term(val weight: Float, val signal: Signal, val inverted: Boolean = false)

    /** A condition on a raw value, so a library with few such songs doesn't fill the mood with its least unlike ones. */
    class Gate(val signal: Signal, val atLeast: Float? = null, val atMost: Float? = null) {
        fun passes(signals: FloatArray): Boolean {
            val value = signal.of(signals)
            if (value.isNaN()) return false
            return (atLeast == null || value >= atLeast) && (atMost == null || value <= atMost)
        }
    }

    /** How a mood is recognised. A song lacking a [required] signal can't fit it. */
    class Recipe(val terms: List<Term>, val gates: List<Gate>, val required: Signal? = null)

    val RECIPES: Map<Mood, Recipe> = mapOf(
        Mood.Chill to Recipe(
            listOf(Term(3f, RELAXED), Term(2f, CALM_TAGS), Term(1f, AGGRESSIVE, inverted = true), Term(1f, ENGAGEMENT, inverted = true)),
            listOf(Gate(RELAXED, atLeast = 0.5f)),
        ),
        Mood.Happy to Recipe(
            listOf(Term(3f, HAPPY), Term(2f, HAPPY_TAGS), Term(1f, SAD, inverted = true)),
            listOf(Gate(HAPPY, atLeast = 0.5f)),
        ),
        Mood.Energetic to Recipe(
            listOf(Term(2f, ENERGY_TAGS), Term(2f, ENGAGEMENT), Term(1f, DANCEABLE), Term(2f, RELAXED, inverted = true)),
            listOf(Gate(RELAXED, atMost = 0.5f)),
        ),
        Mood.Focus to Recipe(
            listOf(Term(3f, INSTRUMENTAL), Term(2f, FOCUS_TAGS), Term(1f, RELAXED), Term(2f, ENGAGEMENT, inverted = true)),
            listOf(Gate(INSTRUMENTAL, atLeast = 0.3f), Gate(AGGRESSIVE, atMost = 0.5f)),
        ),
        Mood.Party to Recipe(
            listOf(Term(3f, PARTY), Term(2f, DANCEABLE), Term(1f, PARTY_TAGS), Term(1f, ENGAGEMENT)),
            listOf(Gate(PARTY, atLeast = 0.5f)),
        ),
        Mood.Melancholy to Recipe(
            listOf(Term(3f, SAD), Term(2f, SAD_TAGS), Term(1f, HAPPY, inverted = true), Term(1f, PARTY, inverted = true)),
            listOf(Gate(SAD, atLeast = 0.5f)),
        ),
        Mood.Romantic to Recipe(
            listOf(
                Term(3f, LOVE_TAGS),
                Term(1f, RELAXED),
                Term(1f, AGGRESSIVE, inverted = true),
                Term(1f, PARTY, inverted = true),
                Term(1f, INSTRUMENTAL, inverted = true),
            ),
            listOf(Gate(AGGRESSIVE, atMost = 0.3f), Gate(INSTRUMENTAL, atMost = 0.5f)),
            required = LOVE_TAGS,
        ),
    )

    /**
     * Each value's percentile among the non-missing ones: 0 for the lowest, 1 for the highest, equal
     * values sharing the mean of their ranks. NaN stays NaN; a single value is 0.5.
     */
    fun percentiles(values: FloatArray): FloatArray {
        val present = values.indices.filter { !values[it].isNaN() }.sortedWith(compareBy({ values[it] }, { it }))
        val out = FloatArray(values.size) { Float.NaN }
        if (present.isEmpty()) return out
        if (present.size == 1) {
            out[present[0]] = 0.5f
            return out
        }
        var start = 0
        while (start < present.size) {
            var end = start
            while (end + 1 < present.size && values[present[end + 1]] == values[present[start]]) end++
            val rank = (start + end) / 2.0
            for (i in start..end) out[present[i]] = (rank / (present.size - 1)).toFloat()
            start = end + 1
        }
        return out
    }

    /**
     * Every song's fit for [mood], NaN where it doesn't fit: it fails a gate, lacks the required
     * signal, has no term left, or falls under [FIT_FLOOR]. [ranks] holds each signal's percentiles.
     */
    fun fits(mood: Mood, songs: List<Song>, ranks: Map<Signal, FloatArray>): FloatArray {
        val recipe = RECIPES.getValue(mood)
        return FloatArray(songs.size) { i ->
            val signals = songs[i].signals
            if (recipe.required != null && recipe.required.of(signals).isNaN()) return@FloatArray Float.NaN
            if (recipe.gates.any { !it.passes(signals) }) return@FloatArray Float.NaN
            var sum = 0f
            var weights = 0f
            for (term in recipe.terms) {
                val p = ranks.getValue(term.signal)[i]
                if (p.isNaN()) continue // Missing for this song: the term drops out and the rest are rescaled.
                sum += term.weight * (if (term.inverted) 1f - p else p)
                weights += term.weight
            }
            if (weights == 0f) return@FloatArray Float.NaN
            val fit = sum / weights
            if (fit >= FIT_FLOOR) fit else Float.NaN
        }
    }

    /** The percentiles of every signal the recipes read, over [songs]. */
    fun ranks(songs: List<Song>): Map<Signal, FloatArray> =
        RECIPES.values.flatMap { r -> r.terms.map { it.signal } }.distinct()
            .associateWith { signal -> percentiles(FloatArray(songs.size) { signal.of(songs[it].signals) }) }

    // ---------------------------------------------------------------- taste and freshness

    /** Per song: likes up, skips down, recent listens counting more. −1 to 1. */
    fun taste(liked: Boolean, played: Double, skipped: Double): Float =
        (0.5 * (if (liked) 1 else 0) + 0.5 * played / (played + 2) - 0.8 * skipped / (skipped + 1)).toFloat().coerceIn(-1f, 1f)

    /** 1 just after you heard the song, 0.5 twelve hours on, near 0 after two days; 0 if never heard. */
    fun fresh(lastHeardAt: Long?, nowMs: Long): Float {
        if (lastHeardAt == null) return 0f
        val age = (nowMs - lastHeardAt).coerceAtLeast(0)
        return 0.5.pow(age.toDouble() / FRESH_HALF_LIFE_MS).toFloat()
    }

    /** What the listening log says about each song, worked out once for every mood. */
    class History(
        /** Finished and partial listens, each weighted by its age. */
        val played: Map<String, Double>,
        /** Skipped and removed listens, weighted the same way. */
        val skipped: Map<String, Double>,
        val skips: Map<String, Int>,
        val finishes: Map<String, Int>,
        val lastHeard: Map<String, Long>,
    ) {
        /** Skipped often enough, and more often than finished, to be left out. */
        fun isSkipped(key: String): Boolean {
            val count = skips[key] ?: 0
            return count >= SKIPS_TO_EXCLUDE && count > (finishes[key] ?: 0)
        }

        companion object {
            fun of(listens: List<Listen>, nowMs: Long): History {
                val played = HashMap<String, Double>()
                val skipped = HashMap<String, Double>()
                val skips = HashMap<String, Int>()
                val finishes = HashMap<String, Int>()
                val lastHeard = HashMap<String, Long>()
                for (listen in listens) {
                    val weight = 0.5.pow((nowMs - listen.startedAt).coerceAtLeast(0).toDouble() / TASTE_HALF_LIFE_MS)
                    when (listen.outcome) {
                        ListenTracker.SKIPPED, ListenTracker.REMOVED -> {
                            skipped.merge(listen.key, weight, Double::plus)
                            skips.merge(listen.key, 1, Int::plus)
                        }
                        else -> {
                            played.merge(listen.key, weight, Double::plus)
                            if (listen.outcome == ListenTracker.FINISHED) finishes.merge(listen.key, 1, Int::plus)
                        }
                    }
                    lastHeard.merge(listen.key, listen.startedAt, ::maxOf)
                }
                return History(played, skipped, skips, finishes, lastHeard)
            }
        }
    }

    // ---------------------------------------------------------------- building

    /**
     * Every song that fits each mood, best first by score, ties by key. Songs you skip are left out;
     * so, when any language is picked, are songs in a language you didn't pick (unless that leaves
     * fewer than [MIN_SONGS]). Moods with no fitting song are absent.
     */
    fun candidates(songs: List<Song>, picks: Picks, listens: List<Listen>, nowMs: Long): Map<Mood, List<Candidate>> {
        val ordered = songs.sortedBy { it.key }
        val ranks = ranks(ordered)
        val history = History.of(listens, nowMs)
        val extras = FloatArray(ordered.size) { i ->
            val song = ordered[i]
            val genre = if (picks.genres.isNotEmpty() && song.genres.any { it in picks.genres }) 1f else 0f
            val language = when {
                picks.languages.isEmpty() -> 0f
                song.language != null && song.language in picks.languages -> 1f
                song.language == null || song.language == LanguageLabels.NO_VOCALS -> 0.5f
                else -> 0f
            }
            val taste = taste(song.liked, history.played[song.key] ?: 0.0, history.skipped[song.key] ?: 0.0)
            GENRE_WEIGHT * genre + LANGUAGE_WEIGHT * language + TASTE_WEIGHT * taste -
                FRESH_WEIGHT * fresh(history.lastHeard[song.key], nowMs)
        }
        val result = LinkedHashMap<Mood, List<Candidate>>()
        for (mood in Mood.entries) {
            val fit = fits(mood, ordered, ranks)
            val all = ordered.indices
                .filter { !fit[it].isNaN() && !history.isSkipped(ordered[it].key) }
                .map { Candidate(ordered[it], fit[it], fit[it] + extras[it]) }
            val inLanguage = if (picks.languages.isEmpty()) {
                all
            } else {
                all.filter { c ->
                    val language = c.song.language
                    language == null || language == LanguageLabels.NO_VOCALS || language in picks.languages
                }.takeIf { it.size >= MIN_SONGS } ?: all
            }
            if (inLanguage.isNotEmpty()) {
                result[mood] = inLanguage.sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.song.key })
            }
        }
        return result
    }

    /** The first [POOL_SIZE] of each mood's candidates: the only songs a day's mix is drawn from. */
    fun pool(candidates: List<Candidate>): List<Candidate> = candidates.take(POOL_SIZE)

    /**
     * The day's mix for [mood] from its [pool]: a weighted draw seeded by [day] and the mood, then one
     * version of each recording ([vectors], unit length; a song without one is kept), the artist and
     * album caps, [MIX_SIZE] songs, and no artist twice in a row where that can be helped. Null when
     * fewer than [MIN_SONGS] are left.
     */
    fun mix(mood: Mood, pool: List<Candidate>, day: Long, vectors: Map<String, FloatArray>): Mix? {
        if (pool.size < MIN_SONGS) return null
        val random = Random(day * 31 + mood.ordinal)
        val lowest = pool.minOf { it.score }
        // Efraimidis and Spirakis: key = u^(1/w), compared as ln(u) / w. Draws go in key order, so the
        // pool's own order can't change them.
        val drawn = pool.sortedBy { it.song.key }
            .map { c ->
                val u = 1.0 - random.nextDouble() // In (0, 1], so ln is finite.
                val weight = (c.score - lowest + 0.05f).toDouble()
                c to ln(u) / weight
            }
            .sortedWith(compareByDescending<Pair<Candidate, Double>> { it.second }.thenBy { it.first.song.key })
            .map { it.first.song }

        val taken = ArrayList<Song>(MIX_SIZE)
        val perArtist = HashMap<String, Int>()
        val perAlbum = HashMap<String, Int>()
        for (song in drawn) {
            if (taken.size == MIX_SIZE) break
            if (song.artistKey != null && (perArtist[song.artistKey] ?: 0) >= MAX_PER_ARTIST) continue
            if (song.albumKey != null && (perAlbum[song.albumKey] ?: 0) >= MAX_PER_ALBUM) continue
            val vector = vectors[song.key]
            if (vector != null && taken.any { other -> vectors[other.key]?.let { dot(it, vector) >= COPY_SIMILARITY } == true }) continue
            taken += song
            song.artistKey?.let { perArtist.merge(it, 1, Int::plus) }
            song.albumKey?.let { perAlbum.merge(it, 1, Int::plus) }
        }
        if (taken.size < MIN_SONGS) return null
        return Mix(mood, spreadArtists(taken).map { it.key })
    }

    /** Moves a song that would follow its own artist to the next place where it doesn't, keeping the rest in order. */
    fun spreadArtists(songs: List<Song>): List<Song> {
        val out = songs.toMutableList()
        for (i in 1 until out.size) {
            val previous = out[i - 1].artistKey ?: continue
            if (out[i].artistKey != previous) continue
            val j = (i + 1 until out.size).firstOrNull { out[it].artistKey != previous } ?: break
            out.add(i, out.removeAt(j))
        }
        return out
    }

    /** Every mood's mix for the day, in [Mood] order, leaving out moods with too few songs. */
    fun build(songs: List<Song>, picks: Picks, listens: List<Listen>, nowMs: Long, day: Long, vectors: Map<String, FloatArray>): List<Mix> =
        candidates(songs, picks, listens, nowMs).mapNotNull { (mood, list) -> mix(mood, pool(list), day, vectors) }

    /** The mixes Home shows: your picked moods' mixes in your order, or every mix when none is picked. */
    fun forHome(mixes: List<Mix>, picks: Picks): List<Mix> =
        if (picks.moods.isEmpty()) mixes else picks.moods.mapNotNull { mood -> mixes.firstOrNull { it.mood == mood } }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
