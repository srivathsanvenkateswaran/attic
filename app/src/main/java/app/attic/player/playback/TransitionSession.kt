package app.attic.player.playback

import android.os.Bundle

/**
 * Where a Smart transition out of the current song will start, published in the session extras beside
 * the sleep timer, so Now Playing can mark it on the seek bar without reading the analysis itself.
 */
object TransitionSession {
    private const val SONG = "transition_song"
    private const val AT = "transition_at"

    /** A point a song plan has, or nothing. */
    data class Mark(val mediaId: String, val atMs: Long)

    fun put(extras: Bundle, mark: Mark?) {
        if (mark == null) return
        extras.putString(SONG, mark.mediaId)
        extras.putLong(AT, mark.atMs)
    }

    fun from(extras: Bundle): Mark? {
        val song = extras.getString(SONG) ?: return null
        if (!extras.containsKey(AT)) return null
        return Mark(song, extras.getLong(AT))
    }
}
