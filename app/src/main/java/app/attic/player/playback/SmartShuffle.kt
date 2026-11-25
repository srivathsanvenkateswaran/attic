package app.attic.player.playback

/**
 * Smart shuffle: a playlist or Favourites played shuffled, with about one song in three from the rest
 * of the library, chosen by radio to fit the collection's sound. The collection itself is unchanged.
 * The list arithmetic is here; [SmartShuffleSession] carries it across the session.
 */
object SmartShuffle {
    /** Two of the collection's songs, then one pick. */
    const val EVERY = 3

    /** The most picks asked of radio at once, so a long playlist doesn't hold up the start. */
    const val MAX_PICKS = 100

    /** How many picks make about one in three for a collection of [size] songs. */
    fun pickCount(size: Int): Int = (size / (EVERY - 1)).coerceIn(0, MAX_PICKS)

    /**
     * [base] with [extra] spread through it: after every two from [base], one from [extra], starting
     * with [base]. Picks still left once [base] runs out follow at the end.
     */
    fun <T> interleave(base: List<T>, extra: List<T>, every: Int = EVERY): List<T> {
        val out = ArrayList<T>(base.size + extra.size)
        val picks = extra.iterator()
        base.forEachIndexed { index, item ->
            out += item
            if ((index + 1) % (every - 1) == 0 && picks.hasNext()) out += picks.next()
        }
        picks.forEachRemaining { out += it }
        return out
    }

    /**
     * Keeps the current song where it is and smart-shuffles the rest of the collection after it: what
     * turning smart shuffle on does while that collection is already playing.
     */
    fun <T> afterCurrent(current: T, shuffledRest: List<T>, picks: List<T>): List<T> =
        listOf(current) + interleave(shuffledRest, picks)

    /**
     * The queue once smart shuffle goes off: the picks taken out, except the one playing, which
     * finishes. Returns the indices to remove, highest first, so removing them in turn stays valid.
     */
    fun removals(queue: List<String>, currentIndex: Int, picks: Set<String>): List<Int> =
        queue.indices.filter { it != currentIndex && queue[it] in picks }.sortedDescending()
}
