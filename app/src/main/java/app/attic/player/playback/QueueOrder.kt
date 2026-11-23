package app.attic.player.playback

import app.attic.player.data.ExternalTrack
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

    class Saved(val keys: List<String>, val index: Int, val positionMs: Long)

    /**
     * The queue as it is saved: without songs opened from another app, whose URI grant won't outlast
     * the app, so the saved queue holds only what was there before. When one of those is playing,
     * the saved place is the next song after it (from its start), or the one before when it was last.
     * Null when nothing would be left, so the previous save stands.
     */
    fun withoutExternal(keys: List<String>, index: Int, positionMs: Long): Saved? {
        val kept = keys.filterNot(ExternalTrack::isExternal)
        if (kept.isEmpty()) return null
        val current = keys.getOrNull(index)
        val keptBefore = keys.take(index.coerceIn(0, keys.size)).count { !ExternalTrack.isExternal(it) }
        return if (current != null && !ExternalTrack.isExternal(current)) {
            Saved(kept, keptBefore, positionMs)
        } else {
            Saved(kept, keptBefore.coerceAtMost(kept.size - 1), 0L)
        }
    }
}
