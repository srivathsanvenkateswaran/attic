package app.attic.player.playback

import app.attic.player.playback.CarFixtures.clocks
import app.attic.player.playback.CarFixtures.kesariya
import app.attic.player.playback.CarFixtures.odd
import app.attic.player.playback.CarFixtures.trouble
import app.attic.player.playback.CarFixtures.tumHi
import app.attic.player.playback.CarFixtures.yellow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseTreeTest {
    private val labels = BrowseLabels()
    private val snapshot = CarFixtures.snapshot()

    private fun children(id: String, limit: Int = BrowseTree.DEFAULT_TAB_LIMIT) = BrowseTree.children(id, snapshot, labels, limit)!!

    private fun tab(tab: BrowseNode.Tab) = BrowseId.tab(tab)

    @Test
    fun `a car with room for five tabs gets every tab`() {
        val tabs = children(BrowseId.ROOT, limit = 5)
        assertEquals(listOf("Home", "Favourites", "Playlists", "Albums", "Artists"), tabs.map { it.title })
        assertTrue(tabs.all { it.browsable && !it.playable && it.tab != null })
    }

    @Test
    fun `with four tabs Favourites moves to the top of Playlists`() {
        val tabs = children(BrowseId.ROOT, limit = 4)
        assertEquals(listOf("Home", "Playlists", "Albums", "Artists"), tabs.map { it.title })
        val playlists = children(tab(BrowseNode.Tab.Playlists), limit = 4)
        assertEquals(tab(BrowseNode.Tab.Favourites), playlists.first().id)
        assertEquals("2 songs", playlists.first().subtitle)
    }

    @Test
    fun `cars that send no limit are treated as four tabs`() {
        assertEquals(4, BrowseTree.children(BrowseId.ROOT, snapshot, labels)!!.size)
    }

    @Test
    fun `Playlists lists your playlists with songs, then folder playlists`() {
        val playlists = children(tab(BrowseNode.Tab.Playlists), limit = 5)
        assertEquals(listOf("Road trip", "Bollywood"), playlists.map { it.title })
        // The playlist's missing file is left out of the count.
        assertEquals("2 songs", playlists.first().subtitle)
    }

    @Test
    fun `Home is shuffle all, then Instant mixes, then Recently played, under their headings`() {
        val home = children(tab(BrowseNode.Tab.Home))
        assertEquals(BrowseId.SHUFFLE_ALL, home[0].id)
        assertNull(home[0].groupTitle)
        assertEquals(BrowseNode.Kind.Mix, home[1].kind)
        assertEquals(trouble.title, home[1].title)
        assertEquals("Instant mixes", home[1].groupTitle)
        assertEquals(listOf(clocks.title, yellow.title), home.drop(2).map { it.title })
        assertTrue(home.drop(2).all { it.groupTitle == "Recently played" && it.playable })
    }

    @Test
    fun `an empty library has nothing on Home`() {
        val empty = BrowseSnapshot(app.attic.player.data.Library.Empty, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(emptyList<BrowseNode>(), BrowseTree.children(tab(BrowseNode.Tab.Home), empty, labels))
    }

    @Test
    fun `albums are a grid sorted by name whose songs are a list`() {
        val albums = children(tab(BrowseNode.Tab.Albums))
        assertEquals(listOf("A Rush of Blood to the Head", "Brahmastra", "Parachutes"), albums.map { it.title })
        assertEquals(BrowseNode.Style.Grid, BrowseTree.tabs(labels, 5).first { it.tab == BrowseNode.Tab.Albums }.childStyle)
        assertEquals(BrowseNode.Style.List, albums.first().childStyle)
        assertEquals(clocks.mediaId, albums.first().coverId)
    }

    @Test
    fun `album songs carry covers and play the album from the tapped song`() {
        val songs = children(BrowseId.album("parachutes|coldplay"))
        assertEquals(listOf(yellow.title, trouble.title), songs.map { it.title })
        assertEquals(trouble.mediaId, songs[1].coverId)
        val request = BrowseTree.playRequest(songs[1].id, snapshot, labels)!!
        assertEquals(listOf(yellow, trouble), request.tracks)
        assertEquals(1, request.startIndex)
        assertEquals("Parachutes", request.contextName)
    }

    @Test
    fun `an artist's songs play most recently played first, under the artist's name`() {
        val songs = children(BrowseId.artist("coldplay"))
        assertEquals(listOf(clocks, yellow, trouble).map { it.title }, songs.map { it.title })
        val request = BrowseTree.playRequest(songs[2].id, snapshot, labels)!!
        assertEquals(listOf(clocks, yellow, trouble), request.tracks)
        assertEquals(2, request.startIndex)
        assertEquals("Coldplay", request.contextName)
    }

    @Test
    fun `favourites, playlists, folders and recents play under the app's names`() {
        fun first(parent: String) = BrowseTree.playRequest(children(parent, limit = 5).first().id, snapshot, labels)!!
        assertEquals(BrowseTree.FAVOURITES, first(tab(BrowseNode.Tab.Favourites)).contextName)
        assertEquals(listOf(kesariya, yellow), first(tab(BrowseNode.Tab.Favourites)).tracks)
        assertEquals("Road trip", first(BrowseId.playlist(7)).contextName)
        assertEquals(listOf(clocks, kesariya), first(BrowseId.playlist(7)).tracks)
        assertEquals("Bollywood", first(BrowseId.folder("Bollywood")).contextName)
        val recent = BrowseTree.playRequest(children(tab(BrowseNode.Tab.Home)).last().id, snapshot, labels)!!
        assertEquals(BrowseTree.RECENTLY_PLAYED, recent.contextName)
        assertEquals(1, recent.startIndex)
    }

    @Test
    fun `an Instant mix starts radio from its song`() {
        val mix = children(tab(BrowseNode.Tab.Home))[1]
        val request = BrowseTree.playRequest(mix.id, snapshot, labels)!!
        assertEquals(listOf(trouble), request.tracks)
        assertEquals(trouble, request.radioFrom)
        assertEquals(false, request.shuffled)
    }

    @Test
    fun `shuffle all plays every song shuffled`() {
        val request = BrowseTree.playRequest(BrowseId.SHUFFLE_ALL, snapshot, labels)!!
        assertEquals(CarFixtures.library.tracks, request.tracks)
        assertEquals(true, request.shuffled)
        assertEquals(BrowseTree.ALL_SONGS, request.contextName)
    }

    @Test
    fun `a song whose list changed since the car drew it plays on its own`() {
        val stale = BrowseId.song(BrowseId.IN_ALBUM, "parachutes|coldplay", kesariya.key)
        val request = BrowseTree.playRequest(stale, snapshot, labels)!!
        assertEquals(listOf(kesariya), request.tracks)
        assertNull(request.contextName)
    }

    @Test
    fun `ids survive paths with separators, percent signs and other scripts`() {
        val id = BrowseId.song(BrowseId.IN_ALBUM, "brahmastra|arijitsingh", odd.key)
        assertEquals(BrowseId.Parsed.Song(BrowseId.IN_ALBUM, "brahmastra|arijitsingh", odd.key), BrowseId.parse(id))
        assertEquals(BrowseId.Parsed.Folder("Bollywood / Old"), BrowseId.parse(BrowseId.folder("Bollywood / Old")))
        val request = BrowseTree.playRequest(id, snapshot, labels)!!
        assertEquals(odd, request.tracks[request.startIndex])
    }

    @Test
    fun `unknown and malformed ids find nothing`() {
        assertNull(BrowseTree.children("nope", snapshot, labels))
        assertNull(BrowseTree.children(BrowseId.album("missing"), snapshot, labels))
        assertNull(BrowseTree.children("song/a", snapshot, labels))
        assertNull(BrowseTree.playRequest(yellow.key, snapshot, labels))
        assertNull(BrowseTree.playRequest(BrowseId.album("parachutes|coldplay"), snapshot, labels))
        assertNull(BrowseId.parse("tab/%zz"))
    }

    @Test
    fun `items describe themselves the same way their parent lists them`() {
        val album = children(tab(BrowseNode.Tab.Albums)).last()
        assertEquals(album, BrowseTree.item(album.id, snapshot, labels))
        val song = children(BrowseId.album("parachutes|coldplay")).first()
        assertEquals(song, BrowseTree.item(song.id, snapshot, labels))
        assertEquals(BrowseNode.Kind.Tab, BrowseTree.item(tab(BrowseNode.Tab.Favourites), snapshot, labels)!!.kind)
    }

    @Test
    fun `search groups songs, artists, albums and playlists`() {
        val results = BrowseTree.search("arijit", snapshot, labels)
        assertEquals(listOf("Songs", "Songs", "Songs", "Artists", "Albums"), results.map { it.groupTitle })
        val brahma = BrowseTree.search("brahma", snapshot, labels)
        assertTrue(brahma.any { it.kind == BrowseNode.Kind.Album && it.groupTitle == "Albums" })
        val road = BrowseTree.search("road", snapshot, labels)
        assertEquals(listOf("Road trip"), road.map { it.title })
        assertEquals(emptyList<BrowseNode>(), BrowseTree.search("  ", snapshot, labels))
    }

    @Test
    fun `a song found by search plays the search results from it`() {
        val hit = BrowseTree.search("yellow", snapshot, labels).first()
        val request = BrowseTree.playRequest(hit.id, snapshot, labels)!!
        assertEquals(BrowseTree.SEARCH_RESULTS, request.contextName)
        assertEquals(yellow, request.tracks[request.startIndex])
    }

    @Test
    fun `paging cuts the list and tolerates pages past the end`() {
        val items = (1..5).toList()
        assertEquals(listOf(3, 4), BrowseTree.page(items, 1, 2))
        assertEquals(listOf(5), BrowseTree.page(items, 2, 2))
        assertEquals(emptyList<Int>(), BrowseTree.page(items, 3, 2))
        assertEquals(items, BrowseTree.page(items, 0, Int.MAX_VALUE))
        assertEquals(items, BrowseTree.page(items, 0, 0))
    }

    @Test
    fun `folder songs only list the folder`() {
        assertEquals(listOf(tumHi.title), children(BrowseId.folder("Bollywood")).map { it.title })
    }
}
