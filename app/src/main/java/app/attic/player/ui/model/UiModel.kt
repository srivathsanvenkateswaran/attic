package app.attic.player.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.attic.player.ui.UiActions

/**
 * A surface's state and what it can do, as produced by its `remember…Model` function. The state says
 * what to show; the actions are everything the drawing may ask for. Neither knows how anything looks.
 */
@Immutable
data class UiModel<S, A>(val state: S, val actions: A)

/** Reads where playback is. Position isn't pushed as state, so a drawing polls it at the rate it needs. */
@Stable
fun interface PlaybackClock {
    fun positionMs(): Long
}

/** Somewhere a tile or row leads, so state can describe a link without holding navigation. */
sealed interface Destination {
    data class Album(val key: String) : Destination
    data class Artist(val key: String) : Destination
    data class Folder(val name: String) : Destination
    data class Playlist(val id: Long) : Destination
    data class Genre(val key: String) : Destination
    data object Liked : Destination
}

fun UiActions.open(destination: Destination) {
    when (destination) {
        is Destination.Album -> openAlbum(destination.key)
        is Destination.Artist -> openArtist(destination.key)
        is Destination.Folder -> openFolder(destination.name)
        is Destination.Playlist -> openPlaylist(destination.id)
        is Destination.Genre -> openGenre(destination.key)
        Destination.Liked -> openLiked()
    }
}
