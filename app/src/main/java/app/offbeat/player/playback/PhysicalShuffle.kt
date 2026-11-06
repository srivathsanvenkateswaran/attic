package app.offbeat.player.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Shuffle rewrites the playlist itself instead of using a separate play order, so the queue screen,
 * drag-to-reorder and "play next" all work on one list that matches what will actually play. The
 * song that is playing is never removed, so toggling shuffle doesn't interrupt it.
 */
@UnstableApi
class PhysicalShuffle(private val player: Player) {
    /** The order to return to when shuffle goes off. Null while shuffle is off. */
    var baseOrder: List<String>? = null
        private set

    class Ordered(val items: List<MediaItem>, val startIndex: Int)

    /** Called when a whole new queue is about to be set. With shuffle on, it starts out shuffled. */
    fun orderNewQueue(items: List<MediaItem>, startIndex: Int): Ordered {
        if (!player.shuffleModeEnabled || items.isEmpty()) {
            baseOrder = null
            return Ordered(items, startIndex)
        }
        baseOrder = items.map { it.mediaId }
        return Ordered(listOf(items[startIndex]) + QueueOrder.shuffledRest(items, startIndex), 0)
    }

    /** Puts saved state back without reshuffling the saved queue. */
    fun restore(enabled: Boolean, base: List<String>?) {
        player.shuffleModeEnabled = enabled
        baseOrder = if (enabled) base else null
    }

    /**
     * The player every controller talks to. The reorder has to happen in the command rather than in
     * a listener, because changing the playlist during a listener callback gives Media3's session a
     * stale timeline alongside a live index, which it treats as a fatal error.
     */
    fun wrappedPlayer(): Player = object : ForwardingPlayer(player) {
        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled == player.shuffleModeEnabled) return
            super.setShuffleModeEnabled(shuffleModeEnabled)
            if (shuffleModeEnabled) turnOn() else turnOff()
        }
    }

    private fun liveItems(): List<MediaItem> = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }

    private fun turnOn() {
        val live = liveItems()
        baseOrder = live.map { it.mediaId }
        if (live.size < 2) return
        replaceAround(emptyList(), QueueOrder.shuffledRest(live, player.currentMediaItemIndex))
    }

    private fun turnOff() {
        val base = baseOrder
        baseOrder = null
        val live = liveItems()
        if (base == null || live.size < 2) return

        val currentIndex = player.currentMediaItemIndex
        val currentId = live[currentIndex].mediaId
        val target = QueueOrder.unshuffled(base, live.map { it.mediaId })

        val pool = HashMap<String, ArrayDeque<MediaItem>>()
        live.filterIndexed { i, _ -> i != currentIndex }
            .forEach { pool.getOrPut(it.mediaId) { ArrayDeque() }.addLast(it) }

        val split = target.indexOf(currentId)
        val before = target.take(split).map { pool.getValue(it).removeFirst() }
        val after = target.drop(split + 1).map { pool.getValue(it).removeFirst() }
        replaceAround(before, after)
    }

    /** Swaps everything around the current item, which keeps playing untouched. */
    private fun replaceAround(before: List<MediaItem>, after: List<MediaItem>) {
        val current = player.currentMediaItemIndex
        player.removeMediaItems(current + 1, player.mediaItemCount)
        player.removeMediaItems(0, current)
        if (before.isNotEmpty()) player.addMediaItems(0, before)
        if (after.isNotEmpty()) player.addMediaItems(player.mediaItemCount, after)
    }
}
