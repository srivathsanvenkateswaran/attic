package app.attic.player.playback

/**
 * A player's position as it really is now. Read from the app thread, `currentPosition` only changes
 * when the player's own thread reports, and on the owner's phone it held still for 230 to 250 ms at a
 * time and then jumped. Two such readings taken at the same moment could be up to a quarter of a
 * second apart in their staleness, which is far more than a crossfade can tolerate.
 *
 * Each reading is kept as its position minus the clock time it was read. A held reading falls behind
 * as the clock moves on, and a fresh one is the highest of them, so the newest report is the highest
 * value in the last [windowMs]. The estimate is that plus the clock now. Call [reset] after a seek or
 * a move to another song, when the position jumps on purpose.
 */
class SmoothedPosition(private val windowMs: Long = 600) {
    private val clocks = LongArray(CAPACITY)
    private val offsets = LongArray(CAPACITY)
    private var start = 0
    private var size = 0

    fun reset() {
        start = 0
        size = 0
    }

    /** Records [positionMs] read at [clockMs] and returns the estimate of the position at [clockMs]. */
    fun add(clockMs: Long, positionMs: Long): Long {
        // Drop readings older than the window, and the oldest if full.
        while (size > 0 && (clockMs - clocks[start] > windowMs || size == CAPACITY)) {
            start = (start + 1) % CAPACITY
            size--
        }
        val at = (start + size) % CAPACITY
        clocks[at] = clockMs
        offsets[at] = positionMs - clockMs
        size++
        var best = Long.MIN_VALUE
        for (i in 0 until size) {
            val o = offsets[(start + i) % CAPACITY]
            if (o > best) best = o
        }
        return clockMs + best
    }

    private companion object {
        const val CAPACITY = 64
    }
}
