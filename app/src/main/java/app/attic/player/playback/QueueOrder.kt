package app.attic.player.playback

import kotlin.random.Random

/** The list arithmetic behind shuffle, kept free of Player so it can be unit tested. */
object QueueOrder {
    /** Everything except the item at [currentIndex], shuffled. The current song keeps playing. */
    fun <T> shuffledRest(items: List<T>, currentIndex: Int, random: Random = Random.Default): List<T> =
        items.filterIndexed { index, _ -> index != currentIndex }.shuffled(random)

    /**
     * Rebuilds the pre-shuffle order for turning shuffle off. Songs removed while shuffled stay
     * gone, and songs added while shuffled go at the end. Handles the same song queued twice.
     */
    fun unshuffled(base: List<String>, live: List<String>): List<String> {
        val remaining = HashMap<String, Int>()
        live.forEach { remaining.merge(it, 1, Int::plus) }
        val result = ArrayList<String>(live.size)
        for (id in base) {
            val count = remaining[id] ?: 0
            if (count > 0) {
                result += id
                remaining[id] = count - 1
            }
        }
        for (id in live) {
            val count = remaining[id] ?: 0
            if (count > 0) {
                result += id
                remaining[id] = count - 1
            }
        }
        return result
    }
}
