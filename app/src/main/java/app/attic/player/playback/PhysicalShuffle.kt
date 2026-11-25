package app.attic.player.playback

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
class PhysicalShuffle(
    private val player: Player,
    /** Called when the smart-shuffle picks change, so the service can tell the controllers. */
    private val onSmartChanged: () -> Unit = {},
) {
    /** The order to return to when shuffle goes off. Null while shuffle is off. */
    var baseOrder: List<String>? = null
        private set

    /**
     * Songs smart shuffle added from outside the collection, marked in the queue. Empty unless smart
     * shuffle is on. They are not in [baseOrder], so turning shuffle off takes them out again.
     */
    var smartPicks: Set<String> = emptySet()
        private set(value) {
            if (field == value) return
            field = value
            onSmartChanged()
        }

    class Ordered(val items: List<MediaItem>, val startIndex: Int)

    /** Called when a whole new queue is about to be set. With shuffle on, it starts out shuffled. */
    fun orderNewQueue(items: List<MediaItem>, startIndex: Int): Ordered {
        smartPicks = emptySet()
        if (!player.shuffleModeEnabled || items.isEmpty()) {
            baseOrder = null
            return Ordered(items, startIndex)
        }
        baseOrder = items.map { it.mediaId }
        return Ordered(listOf(items[startIndex]) + QueueOrder.shuffledRest(items, startIndex), 0)
    }

    /** Puts saved state back without reshuffling the saved queue. */
    fun restore(enabled: Boolean, base: List<String>?, picks: Set<String> = emptySet()) {
        player.shuffleModeEnabled = enabled
        baseOrder = if (enabled) base else null
        smartPicks = if (enabled) picks else emptySet()
    }

    /**
     * Smart shuffle: [base] (the collection, in its own order) shuffled, with [picks] spread through
     * it one song in three. With [keepCurrent] and the current song in [base], that song plays on and
     * the rest goes after it; otherwise the queue is replaced and starts from its first song. Shuffle
     * is switched on underneath, so the list is already in the order it plays.
     */
    fun startSmart(base: List<MediaItem>, picks: List<MediaItem>, keepCurrent: Boolean) {
        val currentId = player.currentMediaItem?.mediaId
        val keptIndex = if (keepCurrent) base.indexOfFirst { it.mediaId == currentId } else -1
        player.shuffleModeEnabled = true
        baseOrder = base.map { it.mediaId }
        if (keptIndex >= 0 && player.mediaItemCount > 0) {
            replaceAround(emptyList(), SmartShuffle.interleave(QueueOrder.shuffledRest(base, keptIndex), picks))
        } else {
            player.setMediaItems(SmartShuffle.interleave(base.shuffled(), picks), 0, 0L)
        }
        smartPicks = picks.map { it.mediaId }.toSet()
    }

    /** Smart shuffle off, shuffle still on: every pick leaves the queue except one that is playing. */
    fun stopSmart() {
        val live = liveItems().map { it.mediaId }
        SmartShuffle.removals(live, player.currentMediaItemIndex, smartPicks).forEach(player::removeMediaItem)
        smartPicks = emptySet()
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
        // Smart shuffle's picks go with it; a pick that is playing finishes, then the collection plays in order.
        val picks = smartPicks
        if (picks.isNotEmpty()) stopSmart()
        val live = liveItems()
        if (base == null || live.size < 2) return

        val currentIndex = player.currentMediaItemIndex
        val currentId = live[currentIndex].mediaId
        val unshuffled = QueueOrder.unshuffled(base, live.map { it.mediaId })
        val target = if (currentId in picks) listOf(currentId) + (unshuffled - currentId) else unshuffled

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
