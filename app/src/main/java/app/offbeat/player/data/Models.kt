package app.offbeat.player.data

/** One row from MediaStore, before any cleanup. Nullable fields were absent from the tags. */
data class RawTrack(
    val mediaId: Long,
    val displayName: String,
    val relativePath: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val cdTrackNumber: String?,
    val durationMs: Long,
    val year: Int?,
    val dateAddedSec: Long,
)

data class Track(
    /** Survives a MediaStore rebuild, unlike [mediaId], so saved data refers to tracks by this. */
    val key: String,
    val mediaId: Long,
    val title: String,
    val artist: String,
    val artistKeys: List<String>,
    val album: String?,
    val albumKey: String?,
    val folder: String?,
    val trackNumber: Int?,
    val durationMs: Long,
    val year: Int?,
    val dateAddedSec: Long,
)

data class Album(
    val key: String,
    val name: String,
    val artist: String,
    val artistKey: String?,
    val tracks: List<Track>,
    val year: Int?,
)

data class Artist(
    val key: String,
    val name: String,
    val tracks: List<Track>,
    val albums: List<Album>,
)

/** A themed top-level folder (English, Hindi, Tamil, Phonk) shown as a read-only playlist. */
data class FolderPlaylist(
    val name: String,
    val tracks: List<Track>,
)

data class Library(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<FolderPlaylist>,
) {
    val trackByKey: Map<String, Track> = tracks.associateBy { it.key }
    val albumByKey: Map<String, Album> = albums.associateBy { it.key }
    val artistByKey: Map<String, Artist> = artists.associateBy { it.key }

    companion object {
        val Empty = Library(emptyList(), emptyList(), emptyList(), emptyList())
    }
}
