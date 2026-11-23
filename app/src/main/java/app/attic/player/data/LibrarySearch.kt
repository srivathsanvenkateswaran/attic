package app.attic.player.data

/**
 * Finds a query in song titles, artists, albums and folder playlists, ignoring case and accents. Names
 * that start with the query come first, then names that contain it, then matches in the secondary text
 * (a song's artist and album, an album's artist). The app's Search screen and the car both use it.
 */
object LibrarySearch {
    class Found(
        val tracks: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
        val folders: List<FolderPlaylist>,
    )

    fun find(library: Library, query: String): Found {
        val q = TitleCleaner.searchable(query)
        return Found(
            tracks = library.tracks.matching(q, { it.title }, { "${it.artist} ${it.album.orEmpty()}" }),
            artists = library.artists.matching(q, { it.name }),
            albums = library.albums.matching(q, { it.name }, { it.artist }),
            folders = library.folders.matching(q, { it.name }),
        )
    }

    /** [items] whose name holds [q] (already [TitleCleaner.searchable]), best matches first. */
    fun <T> List<T>.matching(q: String, name: (T) -> String, also: (T) -> String = { "" }): List<T> =
        mapNotNull { item ->
            val n = TitleCleaner.searchable(name(item))
            when {
                n.startsWith(q) -> item to 0
                n.contains(q) -> item to 1
                TitleCleaner.searchable(also(item)).contains(q) -> item to 2
                else -> null
            }
        }.sortedBy { it.second }.map { it.first }
}
