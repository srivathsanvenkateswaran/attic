package app.attic.player.ui.model

import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.data.Track

/**
 * Adds a song to the end of the queue and says so, as the song menu's "Add to queue" does. A swipe on
 * a song row calls this; the row gives the haptic, since only the gesture knows it was a swipe.
 */
internal fun AtticApp.queueSong(track: Track) {
    player.addToQueue(listOf(track))
    messages.tryEmit(getString(R.string.message_added_to_queue))
}
