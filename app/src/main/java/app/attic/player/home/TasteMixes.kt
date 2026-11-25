package app.attic.player.home

import app.attic.player.playback.SmartShuffle
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Taste mixes: the songs you play and like, grouped by sound into three to five mixes, each topped up
 * with songs you rarely play that sound like the group. Simple k-means on the stored embeddings,
 * seeded by the day, so the same day always gives the same mixes and they change overnight.
 */
object TasteMixes {
    class Song(
        val key: String,
        /** Unit length. Null until the song has been analysed. */
        val vector: FloatArray?,
        val plays: Int,
        val skips: Int,
        val liked: Boolean,
        val artistKey: String?,
    ) {
        /** Favourites count as three plays. */
        val weight: Int get() = plays + if (liked) LIKE_WORTH else 0
    }

    /** One mix: its songs in play order, and the artists it is named after, most played first. */
    data class Mix(val keys: List<String>, val artistKeys: List<String>)

    const val MIN_MIXES = 3
    const val MAX_MIXES = 5
    const val MIX_SIZE = 25

    /** Played or liked songs a mix needs to be built around. Fewer, and the group is dropped. */
    const val MIN_MEMBERS = 4

    /** Below this many played or liked songs there are no mixes at all. */
    const val MIN_SEEDS = MIN_MIXES * MIN_MEMBERS

    /** One more mix per this many played or liked songs, from three up to five. */
    private const val SEEDS_PER_MIX = 20

    /** The most played or liked songs clustered, the heaviest first, which bounds the work. */
    private const val MAX_SEEDS = 800
    private const val LIKE_WORTH = 3
    private const val MAX_ITERATIONS = 30
    private const val RESTARTS = 4

    /** About a third of each mix is songs you rarely play, as with smart shuffle. */
    private const val FAMILIAR = MIX_SIZE - MIX_SIZE / SmartShuffle.EVERY

    /** A discovery must sound at least this close to its mix; radio's own floor. */
    private const val MIN_SIMILARITY = 0.62f

    /** Two files this close are copies of one recording. */
    private const val COPY_SIMILARITY = 0.985f

    /**
     * The day's mixes, the biggest first. Empty with too few played or liked songs that have been
     * analysed: a new library, or analysis that hasn't reached them yet.
     */
    fun build(songs: List<Song>, day: Long): List<Mix> {
        val analysed = songs.filter { it.vector != null }.sortedBy { it.key }
        val seeds = analysed
            .filter { it.weight > 0 && (it.liked || it.skips <= it.plays / 2) }
            .sortedWith(compareByDescending<Song> { it.weight }.thenBy { it.key })
            .take(MAX_SEEDS)
            .sortedBy { it.key }
        if (seeds.size < MIN_SEEDS) return emptyList()

        val random = Random(day)
        val k = (seeds.size / SEEDS_PER_MIX).coerceIn(MIN_MIXES, MAX_MIXES)
        val assignment = cluster(seeds.map { it.vector!! }, seeds.map { it.weight.toFloat() }, k, random)
        val groups = (0 until k)
            .map { c -> seeds.filterIndexed { i, _ -> assignment[i] == c } }
            .filter { it.size >= MIN_MEMBERS }
            .sortedWith(compareByDescending<List<Song>> { group -> group.sumOf { it.weight } }.thenBy { it.first().key })

        val seedKeys = seeds.map { it.key }.toSet()
        val others = analysed.filter { it.key !in seedKeys && (it.skips == 0 || it.skips <= it.plays / 2) }
        val used = HashSet<String>()
        return groups.mapIndexed { index, group ->
            val centre = centroid(group.map { it.vector!! }, group.map { it.weight.toFloat() })
            val mixRandom = Random(day * 31 + index)
            val familiar = group
                .sortedWith(compareByDescending<Song> { it.weight }.thenByDescending { dot(it.vector!!, centre) }.thenBy { it.key })
                .take(FAMILIAR * 2)
                .shuffled(mixRandom)
                .let { distinctCopies(it, emptyList()) }
                .take(FAMILIAR)
            val wanted = MIX_SIZE - familiar.size
            val discoveries = others
                .filter { it.key !in used }
                .map { it to dot(it.vector!!, centre) }
                .filter { it.second >= MIN_SIMILARITY }
                .sortedWith(compareByDescending<Pair<Song, Float>> { it.second }.thenBy { it.first.key })
                .take(wanted * 2)
                .map { it.first }
                .shuffled(mixRandom)
                .let { distinctCopies(it, familiar) }
                .take(wanted)
            used += discoveries.map { it.key }
            Mix(
                keys = SmartShuffle.interleave(familiar, discoveries).map { it.key },
                artistKeys = topArtists(group),
            )
        }
    }

    /**
     * The one or two artists each mix is named after: its most played, except that an artist who
     * already leads an earlier mix gives way to the next, so two mixes don't open with the same name.
     */
    fun nameArtists(mixes: List<Mix>): List<List<String>> {
        val leads = HashSet<String>()
        return mixes.map { mix ->
            val lead = mix.artistKeys.firstOrNull { it !in leads } ?: mix.artistKeys.firstOrNull() ?: return@map emptyList()
            leads += lead
            listOfNotNull(lead, mix.artistKeys.firstOrNull { it != lead })
        }
    }

    private fun topArtists(group: List<Song>): List<String> =
        group.filter { it.artistKey != null }
            .groupBy { it.artistKey!! }
            .mapValues { (_, songs) -> songs.sumOf { it.weight } }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { it.key }

    /** Leaves out any song that is a copy of one already chosen. */
    private fun distinctCopies(songs: List<Song>, chosen: List<Song>): List<Song> {
        val out = ArrayList<Song>()
        for (song in songs) {
            val vector = song.vector!!
            if ((chosen + out).none { dot(it.vector!!, vector) >= COPY_SIMILARITY }) out += song
        }
        return out
    }

    /**
     * Spherical k-means: vectors are unit length, so a song joins the centre it has the largest dot
     * product with, and a centre is the normalised weighted mean of its songs. Starts from k-means++
     * picks drawn with [random], [RESTARTS] times, and keeps the tightest grouping, so one unlucky
     * start can't merge two sounds. One seed always gives one answer. Returns each vector's cluster.
     */
    fun cluster(vectors: List<FloatArray>, weights: List<Float>, k: Int, random: Random): IntArray {
        if (vectors.isEmpty() || k <= 0) return IntArray(vectors.size) { -1 }
        var best: IntArray? = null
        var bestScore = Float.NEGATIVE_INFINITY
        repeat(RESTARTS) {
            val centres = initialCentres(vectors, weights, k.coerceAtMost(vectors.size), random)
            val assignment = refine(vectors, weights, centres)
            // Weighted closeness of every song to its centre: higher is tighter.
            val score = vectors.indices.sumOf { (weights[it] * dot(vectors[it], centres[assignment[it]])).toDouble() }.toFloat()
            if (score > bestScore) {
                bestScore = score
                best = assignment
            }
        }
        return best!!
    }

    private fun refine(vectors: List<FloatArray>, weights: List<Float>, centres: MutableList<FloatArray>): IntArray {
        val n = vectors.size
        val assignment = IntArray(n) { -1 }
        for (iteration in 0 until MAX_ITERATIONS) {
            var changed = false
            for (i in 0 until n) {
                val best = centres.indices.maxByOrNull { dot(vectors[i], centres[it]) }!!
                if (best != assignment[i]) {
                    assignment[i] = best
                    changed = true
                }
            }
            if (!changed) break
            for (c in centres.indices) {
                val members = (0 until n).filter { assignment[it] == c }
                // An emptied centre stays where it was; the mix it would have made is dropped as too small.
                if (members.isNotEmpty()) centres[c] = centroid(members.map(vectors::get), members.map(weights::get))
            }
        }
        return assignment
    }

    /** k-means++: the first centre at random by weight, each next one far from those already chosen. */
    private fun initialCentres(vectors: List<FloatArray>, weights: List<Float>, k: Int, random: Random): MutableList<FloatArray> {
        val centres = ArrayList<FloatArray>(k)
        centres += vectors[weighted(weights, random)].copyOf()
        while (centres.size < k) {
            // 1 - cos is half the squared distance between unit vectors.
            val far = vectors.mapIndexed { i, v -> weights[i] * (1f - centres.maxOf { dot(v, it) }).coerceAtLeast(0f) }
            val index = if (far.sum() > 0f) weighted(far, random) else random.nextInt(vectors.size)
            centres += vectors[index].copyOf()
        }
        return centres
    }

    private fun weighted(weights: List<Float>, random: Random): Int {
        val total = weights.sum()
        if (total <= 0f) return random.nextInt(weights.size)
        var target = random.nextFloat() * total
        for (i in weights.indices) {
            target -= weights[i]
            if (target <= 0f) return i
        }
        return weights.lastIndex
    }

    private fun centroid(vectors: List<FloatArray>, weights: List<Float>): FloatArray {
        val out = FloatArray(vectors.first().size)
        vectors.forEachIndexed { i, v ->
            val w = weights[i]
            for (d in out.indices) out[d] += v[d] * w
        }
        var norm = 0f
        for (value in out) norm += value * value
        norm = sqrt(norm)
        if (norm > 0f) for (d in out.indices) out[d] /= norm
        return out
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
