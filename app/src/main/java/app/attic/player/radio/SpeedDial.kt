package app.attic.player.radio

import kotlin.math.sqrt
import kotlin.random.Random

/** Chooses the Home grid: your favourites, then unplayed songs that sound like what you play. */
object SpeedDial {
    data class Song(
        val key: String,
        /** Null until the song has been analysed. */
        val vector: FloatArray?,
        val plays: Int,
        val skips: Int,
        val liked: Boolean,
        val artistKey: String?,
        val albumKey: String?,
    ) {
        val score: Int get() = plays + if (liked) LIKE_WORTH else 0
    }

    private const val LIKE_WORTH = 3
    private const val MAX_FAVOURITES = 4
    private const val MAX_PER_ARTIST = 2
    private const val COPY_SIMILARITY = 0.985f
    private const val VERSION_MIN_SIMILARITY = 0.80f
    private const val DISCOVERY_POOL = 40

    /** The same [day] gives the same grid, so it only changes overnight. */
    fun pick(songs: List<Song>, day: Long, size: Int, isSameSong: (Song, Song) -> Boolean = { _, _ -> false }): List<Song> {
        val fits = { song: Song, chosen: List<Song> -> fits(song, chosen, isSameSong) }
        val usable = songs.filter { it.skips <= it.plays / 2 || it.skips == 0 }
        val chosen = ArrayList<Song>(size)

        usable.filter { it.score > 0 }
            .sortedWith(compareByDescending<Song> { it.score }.thenBy { it.key })
            .forEach { if (chosen.size < MAX_FAVOURITES.coerceAtMost(size) && fits(it, chosen)) chosen += it }

        val random = Random(day)
        val taste = taste(usable)
        val unplayed = usable.filter { it !in chosen && it.score == 0 }
        val discoveries = if (taste != null) {
            // Shuffling only the nearest songs keeps every pick relevant while still changing daily.
            unplayed.filter { it.vector != null }
                .sortedByDescending { dot(it.vector!!, taste) }
                .take(((unplayed.size + 1) / 2).coerceAtMost(DISCOVERY_POOL))
                .shuffled(random)
        } else {
            unplayed.shuffled(random)
        }
        val leftovers = (usable - chosen.toSet() - discoveries.toSet()).shuffled(random)
        (discoveries + leftovers).forEach { if (chosen.size < size && fits(it, chosen)) chosen += it }
        return chosen
    }

    private fun fits(song: Song, chosen: List<Song>, isSameSong: (Song, Song) -> Boolean): Boolean {
        if (song.albumKey != null && chosen.any { it.albumKey == song.albumKey }) return false
        if (song.artistKey != null && chosen.count { it.artistKey == song.artistKey } >= MAX_PER_ARTIST) return false
        val vector = song.vector ?: return true
        return chosen.none { other ->
            val similarity = other.vector?.let { dot(it, vector) } ?: return@none false
            similarity >= COPY_SIMILARITY || (similarity >= VERSION_MIN_SIMILARITY && isSameSong(other, song))
        }
    }

    /** The average sound of what you've played, with favourites counting more. Null with no history. */
    private fun taste(songs: List<Song>): FloatArray? {
        val played = songs.filter { it.score > 0 && it.vector != null }
        if (played.isEmpty()) return null
        val out = FloatArray(played.first().vector!!.size)
        for (song in played) {
            val vector = song.vector!!
            for (i in out.indices) out[i] += vector[i] * song.score
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
}
