package app.attic.player.data

/**
 * A song opened from another app ("Open with") that is not in the library. It plays from the URI
 * that app handed over, and its key carries that URI, so the player can rebuild it from the key
 * alone. Nothing about it is saved: no listening history, favourites, playlists or saved queue.
 */
object ExternalTrack {
    private const val PREFIX = "external:"

    fun keyFor(uri: String): String = PREFIX + uri

    fun isExternal(key: String): Boolean = key.startsWith(PREFIX)

    /** The URI an external key was made from, or null for a library key. */
    fun uriOf(key: String): String? = if (isExternal(key)) key.removePrefix(PREFIX) else null
}

val Track.isExternal: Boolean get() = ExternalTrack.isExternal(key)
