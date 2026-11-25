package app.attic.player.playback

import app.attic.player.data.Library
import app.attic.player.data.LibrarySearch
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track
import app.attic.player.data.UserPlaylist
import java.net.URLDecoder
import java.net.URLEncoder

/** Everything the car's browse tree is drawn from, read once per request so a page is consistent. */
class BrowseSnapshot(
    val library: Library,
    /** Newest first. */
    val likedKeys: List<String>,
    val playlists: List<UserPlaylist>,
    /** Most recent first. */
    val recentKeys: List<String>,
    /** The Instant mixes Home shows in the app. */
    val mixes: List<Track>,
) {
    val liked: List<Track> by lazy { likedKeys.mapNotNull { library.trackByKey[it] } }
    val recent: List<Track> by lazy { recentKeys.mapNotNull { library.trackByKey[it] }.distinctBy { it.key }.take(RECENT_COUNT) }

    /** An artist's songs as the app's artist page plays them: what you played most recently first. */
    fun artistTracks(key: String): List<Track> {
        val artist = library.artistByKey[key] ?: return emptyList()
        val rank = recentKeys.withIndex().associate { it.value to it.index }
        return artist.tracks.sortedBy { rank[it.key] ?: Int.MAX_VALUE }
    }

    fun playlistTracks(playlist: UserPlaylist): List<Track> = playlist.trackKeys.mapNotNull { library.trackByKey[it] }

    companion object {
        /** As many as the app's Recently played row. */
        const val RECENT_COUNT = 12
    }
}

/** Visible words in the tree. The service reads them from string resources; tests use these. */
class BrowseLabels(
    val home: String = "Home",
    val favourites: String = "Favourites",
    val playlists: String = "Playlists",
    val albums: String = "Albums",
    val artists: String = "Artists",
    val instantMixes: String = "Instant mixes",
    val recentlyPlayed: String = "Recently played",
    val shuffleAll: String = "Shuffle all songs",
    val songsGroup: String = "Songs",
    val songCount: (Int) -> String = { if (it == 1) "1 song" else "$it songs" },
)

/** One row or tile in the car. Playable nodes start music; browsable ones open a list. */
data class BrowseNode(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val kind: Kind,
    /** The MediaStore id whose cover the row shows. */
    val coverId: Long? = null,
    /** Set on the root's tabs, which show an icon rather than a cover. */
    val tab: Tab? = null,
    /** Rows that share a group title are drawn under one heading. */
    val groupTitle: String? = null,
    /** How this node's children are laid out, when it is browsable. */
    val childStyle: Style? = null,
    /** The song behind a song or Instant mix row. */
    val track: Track? = null,
) {
    enum class Kind(val playable: Boolean, val browsable: Boolean) {
        Root(false, true),
        Tab(false, true),
        Album(false, true),
        Artist(false, true),
        Playlist(false, true),
        Song(true, false),
        Mix(true, false),
        ShuffleAll(true, false),
    }

    enum class Style { List, Grid }

    enum class Tab { Home, Favourites, Playlists, Albums, Artists }

    val playable: Boolean get() = kind.playable
    val browsable: Boolean get() = kind.browsable
}

/** What to put in the player when a car row, a search result or a voice request is played. */
data class PlayRequest(
    val tracks: List<Track>,
    val startIndex: Int,
    /** The name the queue plays under, as the app's own screens name it. Null for a lone song. */
    val contextName: String?,
    /** Set when the request starts radio from this song (an Instant mix) instead of a fixed list. */
    val radioFrom: Track? = null,
    /** Overrides the shuffle setting when not null. */
    val shuffled: Boolean? = null,
)

/**
 * The media ids the car sees. Each part is URL-encoded and joined with '/', because song keys are file
 * paths and album keys contain '|'. A playable song's id names the list it was found in, so tapping it
 * plays that whole list from there, the way tapping a song in the app does.
 */
object BrowseId {
    const val ROOT = "root"
    const val SHUFFLE_ALL = "shuffle"
    private const val TAB = "tab"
    private const val ALBUM = "album"
    private const val ARTIST = "artist"
    private const val PLAYLIST = "playlist"
    private const val FOLDER = "folder"
    private const val SONG = "song"
    private const val MIX = "mix"
    private const val NONE = "-"

    // The parts of a song id: where it was found, and that list's own key.
    internal const val IN_ALBUM = ALBUM
    internal const val IN_ARTIST = ARTIST
    internal const val IN_PLAYLIST = PLAYLIST
    internal const val IN_FOLDER = FOLDER
    internal const val IN_FAVOURITES = "favourites"
    internal const val IN_RECENT = "recent"
    internal const val IN_SEARCH = "search"

    fun tab(tab: BrowseNode.Tab) = join(TAB, tab.name.lowercase())
    fun album(key: String) = join(ALBUM, key)
    fun artist(key: String) = join(ARTIST, key)
    fun playlist(id: Long) = join(PLAYLIST, id.toString())
    fun folder(name: String) = join(FOLDER, name)
    fun mix(trackKey: String) = join(MIX, trackKey)
    fun song(context: String, contextKey: String?, trackKey: String) = join(SONG, context, contextKey ?: NONE, trackKey)

    sealed interface Parsed {
        data object Root : Parsed
        data class Tab(val tab: BrowseNode.Tab) : Parsed
        data class Album(val key: String) : Parsed
        data class Artist(val key: String) : Parsed
        data class Playlist(val id: Long) : Parsed
        data class Folder(val name: String) : Parsed
        data class Song(val context: String, val contextKey: String?, val trackKey: String) : Parsed
        data class Mix(val trackKey: String) : Parsed
        data object ShuffleAll : Parsed
    }

    fun parse(id: String): Parsed? {
        if (id == ROOT) return Parsed.Root
        if (id == SHUFFLE_ALL) return Parsed.ShuffleAll
        val parts = runCatching { id.split('/').map { URLDecoder.decode(it, Charsets.UTF_8) } }.getOrNull() ?: return null
        return when (parts.first()) {
            TAB -> parts.getOrNull(1)?.let { name -> BrowseNode.Tab.entries.firstOrNull { it.name.lowercase() == name } }?.let(Parsed::Tab)
            ALBUM -> parts.getOrNull(1)?.let(Parsed::Album)
            ARTIST -> parts.getOrNull(1)?.let(Parsed::Artist)
            PLAYLIST -> parts.getOrNull(1)?.toLongOrNull()?.let(Parsed::Playlist)
            FOLDER -> parts.getOrNull(1)?.let(Parsed::Folder)
            MIX -> parts.getOrNull(1)?.let(Parsed::Mix)
            SONG -> if (parts.size == 4) Parsed.Song(parts[1], parts[2].takeIf { it != NONE }, parts[3]) else null
            else -> null
        }
    }

    private fun join(vararg parts: String) = parts.joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8) }
}

/**
 * Builds the car's browse tree from a [BrowseSnapshot]. It holds no state and touches no Android
 * classes, so the tree is tested on the JVM; the service turns nodes into media items.
 *
 * ```
 * root
 * ├── Home        Shuffle all songs, Instant mixes (each starts radio), Recently played
 * ├── Favourites  songs, newest first
 * ├── Playlists   Favourites when the car has room for only four tabs, your playlists, folder playlists
 * ├── Albums      a grid of albums → songs
 * └── Artists     a grid of artists → songs, most recently played first
 * ```
 */
object BrowseTree {
    /** Cars that send no limit are assumed to show four tabs, as the car media docs advise. */
    const val DEFAULT_TAB_LIMIT = 4

    /** Home, Favourites, Playlists, Albums and Artists. */
    const val ALL_TABS = 5
    const val SEARCH_SONG_LIMIT = 50

    /** The root's tabs. With room for only four, Favourites moves to the top of Playlists. */
    fun tabs(labels: BrowseLabels, limit: Int = DEFAULT_TAB_LIMIT): List<BrowseNode> {
        val all = listOf(
            tabNode(BrowseNode.Tab.Home, labels.home, BrowseNode.Style.List),
            tabNode(BrowseNode.Tab.Favourites, labels.favourites, BrowseNode.Style.List),
            tabNode(BrowseNode.Tab.Playlists, labels.playlists, BrowseNode.Style.List),
            tabNode(BrowseNode.Tab.Albums, labels.albums, BrowseNode.Style.Grid),
            tabNode(BrowseNode.Tab.Artists, labels.artists, BrowseNode.Style.Grid),
        )
        return if (limit >= ALL_TABS) all else all.filter { it.tab != BrowseNode.Tab.Favourites }.take(limit.coerceAtLeast(1))
    }

    /** The children of [parentId], or null if no such node exists. */
    fun children(parentId: String, snapshot: BrowseSnapshot, labels: BrowseLabels, tabLimit: Int = DEFAULT_TAB_LIMIT): List<BrowseNode>? {
        val library = snapshot.library
        return when (val parsed = BrowseId.parse(parentId)) {
            BrowseId.Parsed.Root -> tabs(labels, tabLimit)
            is BrowseId.Parsed.Tab -> when (parsed.tab) {
                BrowseNode.Tab.Home -> home(snapshot, labels)
                BrowseNode.Tab.Favourites -> snapshot.liked.map { song(it, BrowseId.IN_FAVOURITES, null, it.artist) }
                BrowseNode.Tab.Playlists -> playlists(snapshot, labels, includeFavourites = tabLimit < ALL_TABS)
                BrowseNode.Tab.Albums -> library.albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).map { album ->
                    BrowseNode(BrowseId.album(album.key), album.name, album.artist, BrowseNode.Kind.Album, album.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
                }
                BrowseNode.Tab.Artists -> library.artists.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).map { artist ->
                    BrowseNode(BrowseId.artist(artist.key), artist.name, labels.songCount(artist.tracks.size), BrowseNode.Kind.Artist, artist.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
                }
            }
            is BrowseId.Parsed.Album -> library.albumByKey[parsed.key]?.tracks?.map { song(it, BrowseId.IN_ALBUM, parsed.key, it.artist) }
            is BrowseId.Parsed.Artist -> library.artistByKey[parsed.key]?.let {
                snapshot.artistTracks(parsed.key).map { song(it, BrowseId.IN_ARTIST, parsed.key, it.album ?: it.artist) }
            }
            is BrowseId.Parsed.Playlist -> snapshot.playlists.firstOrNull { it.id == parsed.id }?.let { playlist ->
                snapshot.playlistTracks(playlist).map { song(it, BrowseId.IN_PLAYLIST, parsed.id.toString(), it.artist) }
            }
            is BrowseId.Parsed.Folder -> library.folders.firstOrNull { it.name == parsed.name }?.tracks?.map {
                song(it, BrowseId.IN_FOLDER, parsed.name, it.artist)
            }
            else -> null
        }
    }

    /** The node [id] names, for a car that asks about one item. */
    fun item(id: String, snapshot: BrowseSnapshot, labels: BrowseLabels): BrowseNode? {
        val library = snapshot.library
        return when (val parsed = BrowseId.parse(id)) {
            null -> null
            BrowseId.Parsed.Root -> BrowseNode(BrowseId.ROOT, "", kind = BrowseNode.Kind.Root)
            is BrowseId.Parsed.Tab -> tabs(labels, ALL_TABS).first { it.tab == parsed.tab }
            is BrowseId.Parsed.Album -> library.albumByKey[parsed.key]?.let {
                BrowseNode(id, it.name, it.artist, BrowseNode.Kind.Album, it.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
            }
            is BrowseId.Parsed.Artist -> library.artistByKey[parsed.key]?.let {
                BrowseNode(id, it.name, labels.songCount(it.tracks.size), BrowseNode.Kind.Artist, it.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
            }
            is BrowseId.Parsed.Playlist -> snapshot.playlists.firstOrNull { it.id == parsed.id }?.let { playlistNode(it, snapshot, labels) }
            is BrowseId.Parsed.Folder -> library.folders.firstOrNull { it.name == parsed.name }?.let {
                BrowseNode(id, it.name, labels.songCount(it.tracks.size), BrowseNode.Kind.Playlist, it.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
            }
            is BrowseId.Parsed.Song -> library.trackByKey[parsed.trackKey]?.let { song(it, parsed.context, parsed.contextKey, it.artist) }
            is BrowseId.Parsed.Mix -> library.trackByKey[parsed.trackKey]?.let { mix(it) }
            BrowseId.Parsed.ShuffleAll -> shuffleAll(snapshot, labels)
        }
    }

    /** Search results for the car: songs, then artists, albums and playlists, each under its own heading. */
    fun search(query: String, snapshot: BrowseSnapshot, labels: BrowseLabels): List<BrowseNode> {
        if (query.isBlank()) return emptyList()
        val found = LibrarySearch.find(snapshot.library, query)
        val songs = found.tracks.take(SEARCH_SONG_LIMIT).map {
            song(it, BrowseId.IN_SEARCH, query, it.artist).copy(groupTitle = labels.songsGroup)
        }
        val artists = found.artists.map {
            BrowseNode(BrowseId.artist(it.key), it.name, labels.songCount(it.tracks.size), BrowseNode.Kind.Artist, it.tracks.firstOrNull()?.mediaId, groupTitle = labels.artists, childStyle = BrowseNode.Style.List)
        }
        val albums = found.albums.map {
            BrowseNode(BrowseId.album(it.key), it.name, it.artist, BrowseNode.Kind.Album, it.tracks.firstOrNull()?.mediaId, groupTitle = labels.albums, childStyle = BrowseNode.Style.List)
        }
        val q = TitleCleaner.searchable(query)
        val playlists = snapshot.playlists.filter { TitleCleaner.searchable(it.name).contains(q) }
            .filter { snapshot.playlistTracks(it).isNotEmpty() }
            .map { playlistNode(it, snapshot, labels).copy(groupTitle = labels.playlists) } +
            found.folders.map {
                BrowseNode(BrowseId.folder(it.name), it.name, labels.songCount(it.tracks.size), BrowseNode.Kind.Playlist, it.tracks.firstOrNull()?.mediaId, groupTitle = labels.playlists, childStyle = BrowseNode.Style.List)
            }
        return songs + artists + albums + playlists
    }

    /**
     * What playing [id] puts in the player, or null if [id] is not a playable node. A song whose list
     * has changed since the car drew it plays on its own rather than failing.
     */
    fun playRequest(id: String, snapshot: BrowseSnapshot, labels: BrowseLabels): PlayRequest? {
        val library = snapshot.library
        return when (val parsed = BrowseId.parse(id)) {
            is BrowseId.Parsed.Song -> {
                val track = library.trackByKey[parsed.trackKey] ?: return null
                val (list, name) = contextOf(parsed, snapshot, labels)
                val index = list.indexOfFirst { it.key == track.key }
                if (index >= 0) PlayRequest(list, index, name) else PlayRequest(listOf(track), 0, null)
            }
            is BrowseId.Parsed.Mix -> library.trackByKey[parsed.trackKey]?.let { PlayRequest(listOf(it), 0, null, radioFrom = it, shuffled = false) }
            BrowseId.Parsed.ShuffleAll -> shuffleAllRequest(snapshot)
            else -> null
        }
    }

    /** Every song, shuffled, under the name the app's Songs list plays as. */
    fun shuffleAllRequest(snapshot: BrowseSnapshot): PlayRequest? {
        val tracks = snapshot.library.tracks
        if (tracks.isEmpty()) return null
        return PlayRequest(tracks, tracks.indices.random(), ALL_SONGS, shuffled = true)
    }

    /** One page of [items]. Cars mostly ask for everything at once, which a negative or zero size also means. */
    fun <T> page(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (page < 0 || pageSize <= 0) return items
        val from = page.toLong() * pageSize
        if (from >= items.size) return emptyList()
        return items.subList(from.toInt(), minOf(items.size.toLong(), from + pageSize).toInt())
    }

    private fun contextOf(song: BrowseId.Parsed.Song, snapshot: BrowseSnapshot, labels: BrowseLabels): Pair<List<Track>, String?> {
        val library = snapshot.library
        val key = song.contextKey
        return when (song.context) {
            BrowseId.IN_ALBUM -> key?.let(library.albumByKey::get)?.let { it.tracks to it.name }
            BrowseId.IN_ARTIST -> key?.let(library.artistByKey::get)?.let { snapshot.artistTracks(it.key) to it.name }
            BrowseId.IN_PLAYLIST -> key?.toLongOrNull()?.let { id -> snapshot.playlists.firstOrNull { it.id == id } }?.let { snapshot.playlistTracks(it) to it.name }
            BrowseId.IN_FOLDER -> library.folders.firstOrNull { it.name == key }?.let { it.tracks to it.name }
            BrowseId.IN_FAVOURITES -> snapshot.liked to FAVOURITES
            BrowseId.IN_RECENT -> snapshot.recent to RECENTLY_PLAYED
            BrowseId.IN_SEARCH -> key?.let { LibrarySearch.find(library, it).tracks.take(SEARCH_SONG_LIMIT) to SEARCH_RESULTS }
            else -> null
        } ?: (emptyList<Track>() to null)
    }

    private fun home(snapshot: BrowseSnapshot, labels: BrowseLabels): List<BrowseNode> {
        val shuffle = listOfNotNull(shuffleAll(snapshot, labels))
        val mixes = snapshot.mixes.map { mix(it).copy(groupTitle = labels.instantMixes) }
        val recent = snapshot.recent.map { song(it, BrowseId.IN_RECENT, null, it.artist).copy(groupTitle = labels.recentlyPlayed) }
        return shuffle + mixes + recent
    }

    private fun playlists(snapshot: BrowseSnapshot, labels: BrowseLabels, includeFavourites: Boolean): List<BrowseNode> {
        val favourites = if (includeFavourites) {
            listOf(
                BrowseNode(
                    BrowseId.tab(BrowseNode.Tab.Favourites), labels.favourites, labels.songCount(snapshot.liked.size),
                    BrowseNode.Kind.Tab, snapshot.liked.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List,
                ),
            )
        } else {
            emptyList()
        }
        val own = snapshot.playlists.filter { snapshot.playlistTracks(it).isNotEmpty() }.map { playlistNode(it, snapshot, labels) }
        val folders = snapshot.library.folders.map {
            BrowseNode(BrowseId.folder(it.name), it.name, labels.songCount(it.tracks.size), BrowseNode.Kind.Playlist, it.tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List)
        }
        return favourites + own + folders
    }

    private fun playlistNode(playlist: UserPlaylist, snapshot: BrowseSnapshot, labels: BrowseLabels): BrowseNode {
        val tracks = snapshot.playlistTracks(playlist)
        return BrowseNode(
            BrowseId.playlist(playlist.id), playlist.name, labels.songCount(tracks.size), BrowseNode.Kind.Playlist,
            tracks.firstOrNull()?.mediaId, childStyle = BrowseNode.Style.List,
        )
    }

    private fun tabNode(tab: BrowseNode.Tab, title: String, childStyle: BrowseNode.Style) =
        BrowseNode(BrowseId.tab(tab), title, kind = BrowseNode.Kind.Tab, tab = tab, childStyle = childStyle)

    private fun shuffleAll(snapshot: BrowseSnapshot, labels: BrowseLabels): BrowseNode? {
        val tracks = snapshot.library.tracks
        if (tracks.isEmpty()) return null
        return BrowseNode(BrowseId.SHUFFLE_ALL, labels.shuffleAll, labels.songCount(tracks.size), BrowseNode.Kind.ShuffleAll)
    }

    private fun mix(track: Track) =
        BrowseNode(BrowseId.mix(track.key), track.title, track.artist, BrowseNode.Kind.Mix, track.mediaId, track = track)

    private fun song(track: Track, context: String, contextKey: String?, subtitle: String) =
        BrowseNode(BrowseId.song(context, contextKey, track.key), track.title, subtitle, BrowseNode.Kind.Song, track.mediaId, track = track)

    // The names the app's own screens play these lists under, so history, Recap and Now Playing match.
    const val FAVOURITES = "Favourites"
    const val RECENTLY_PLAYED = "Recently played"
    const val SEARCH_RESULTS = "Search results"
    const val ALL_SONGS = "All songs"
}
