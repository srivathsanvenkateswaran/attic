package app.attic.player.storeshots

import androidx.compose.material3.SnackbarHostState
import androidx.media3.common.Player
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.data.BulkProgress
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Library
import app.attic.player.data.ThemeMode
import app.attic.player.data.Track
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlayerUiState
import app.attic.player.ui.Tab
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.model.AlbumActions
import app.attic.player.ui.model.BackupUiState
import app.attic.player.ui.model.AlbumState
import app.attic.player.ui.model.ArtistActions
import app.attic.player.ui.model.ArtistState
import app.attic.player.ui.model.ContextKind
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.HomeActions
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryItem
import app.attic.player.ui.model.LibraryKind
import app.attic.player.ui.model.LibraryState
import app.attic.player.ui.model.LyricsActions
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.model.LyricsState
import app.attic.player.ui.model.MiniPlayerActions
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlayControlsState
import app.attic.player.ui.model.PlaybackClock
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.SearchActions
import app.attic.player.ui.model.SearchResults
import app.attic.player.ui.model.SearchState
import app.attic.player.ui.model.SettingsActions
import app.attic.player.ui.model.SettingsState
import app.attic.player.ui.model.ShellActions
import app.attic.player.ui.model.ShellState
import app.attic.player.ui.model.Shortcut
import app.attic.player.ui.model.SongSort
import app.attic.player.ui.model.UpNextRow
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.model.WrappedPageActions
import app.attic.player.ui.model.WrappedPageState
import app.attic.player.ui.model.WrappedTeaser
import app.attic.player.ui.model.BrowseTile
import app.attic.player.ui.model.BackupUiState
import app.attic.player.ui.model.wrappedData
import app.attic.player.wrapped.DayTotal
import app.attic.player.wrapped.LanguageShare
import app.attic.player.wrapped.Listen
import app.attic.player.wrapped.RankedArtist
import app.attic.player.wrapped.RankedSong
import app.attic.player.wrapped.Recap
import app.attic.player.wrapped.RecapPeriod
import app.attic.player.wrapped.RepeatDay
import java.time.LocalDate

/**
 * The state each screen is drawn from, as the real models would build it for [DemoLibrary], and
 * actions that do nothing. Nothing here touches the app's singletons.
 */
object DemoStates {
    private val lib: Library get() = DemoLibrary.library
    private val now: Track get() = DemoLibrary.nowPlaying
    private val noop: () -> Unit = {}

    /** The day the shots are dated, for the recap's wording. */
    val today: LocalDate = LocalDate.of(2026, 11, 23)

    val clock = PlaybackClock { DemoLibrary.POSITION_MS }

    private val queue: List<Track> get() = DemoLibrary.album("Paper Lanterns").tracks +
        DemoLibrary.album("Glasshouse Summer").tracks.take(2) + DemoLibrary.album("Iravu Rail").tracks.take(2)

    val player: PlayerUiState get() = PlayerUiState(
        queue = queue,
        currentIndex = 0,
        isPlaying = true,
        shuffle = false,
        repeatMode = Player.REPEAT_MODE_OFF,
        contextName = "Paper Lanterns",
    )

    fun shell(tab: Tab) = ShellState(
        player = player,
        currentTab = tab,
        hasCurrentSong = true,
        nowPlayingVisible = false,
        queueVisible = false,
        lyricsVisible = false,
        covered = false,
        snackbar = SnackbarHostState(),
        menu = null,
        picking = null,
    )

    val shellActions = ShellActions({}, noop, noop, noop, noop, noop, noop, noop, noop, noop)

    val mini get() = MiniPlayerState(now, isPlaying = true, liked = true, clock = clock)
    val miniActions = MiniPlayerActions(noop, noop, noop, noop, noop)

    // Home

    val home: HomeState get() {
        val albums = lib.albums
        val recent = listOf(
            "Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta",
            "Night Drive Firmware", "Kettle On", "Filter Coffee Funk",
        ).map(DemoLibrary::track)
        return HomeState(
            isEmpty = false,
            filter = HomeFilter.All,
            shortcuts = listOf(Shortcut.Liked) + listOf("Paper Lanterns", "Iravu Rail", "Chhat Pe Dhoop", "Glasshouse Summer", "Afterglow Protocol")
                .map { Shortcut.ForAlbum(DemoLibrary.album(it)) },
            wrapped = WrappedTeaser(
                minutes = 18_420,
                mosaicIds = mosaicIds(recent),
                year = today.year,
                yearEnded = false,
            ),
            speedDial = listOf("Porch Light", "Platform Ettu", "Patang", "Glasshouse", "Azulejo", "Drift Mode").map(DemoLibrary::track),
            recentTracks = recent,
            startListening = listOf("Moth Letters", "Kadalkarai", "Sheher Ki Shaam", "Rain on the Skylight").map(DemoLibrary::track),
            folders = lib.folders,
            biggestAlbums = albums.sortedByDescending { it.tracks.size },
            biggestArtists = lib.artists.sortedByDescending { it.tracks.size },
            spotlight = DemoLibrary.artist("Mira Okafor"),
            currentKey = now.key,
            isPlaying = true,
            favouriteCount = 42,
            radioSeedKey = null,
        )
    }

    val homeActions = HomeActions({}, noop, {}, noop, {}, {}, {}, {}, {}, {}, {})

    // Collections

    private fun controls(name: String, playing: Boolean) = PlayControlsState(name, enabled = true, shuffleOn = false, isPlaying = playing)
    private val controlsActions = PlayControlsActions(noop, noop, noop)

    fun album(name: String): AlbumState {
        val album = DemoLibrary.album(name)
        val playing = album.tracks.any { it.key == now.key }
        return AlbumState(
            album = album,
            artistMediaId = DemoLibrary.artist(album.artist).tracks.first().mediaId,
            controls = controls(album.name, playing),
            currentKey = now.key,
            isPlaying = true,
        )
    }

    val albumActions = AlbumActions(noop, noop, controlsActions, {}, {})

    fun artist(name: String): ArtistState {
        val artist = DemoLibrary.artist(name)
        val ranked = artist.tracks.sortedBy { (it.title.hashCode() and 0xFF) }
        return ArtistState(
            artist = artist,
            popular = ranked.take(5),
            others = ranked.drop(5),
            controls = controls(artist.name, playing = false),
            currentKey = now.key,
            isPlaying = true,
        )
    }

    val artistActions = ArtistActions(noop, controlsActions, {}, {}, {})

    // Now Playing, queue and lyrics

    val lyrics = LyricsState(
        lines = DemoLibrary.lyrics,
        plain = null,
        checked = true,
        found = true,
        instrumental = false,
        hasLyrics = true,
        downloading = false,
    )
    private val lyricsActions = LyricsActions(noop)

    val nowPlaying: NowPlayingState get() = NowPlayingState(
        track = now,
        contextName = "Paper Lanterns",
        contextKind = ContextKind.Album,
        isPlaying = true,
        shuffle = false,
        repeatMode = Player.REPEAT_MODE_OFF,
        queue = queue,
        currentIndex = 0,
        liked = true,
        artist = DemoLibrary.artist(now.artist),
        lyrics = lyrics,
        clock = clock,
    )

    val nowPlayingActions = NowPlayingActions(
        close = noop, openQueue = noop, openLyrics = noop, showMenu = noop, toggleLike = noop,
        openTrackArtist = noop, openArtist = noop, skipTo = {}, togglePlayPause = noop, previous = noop,
        next = noop, toggleShuffle = noop, cycleRepeat = noop, seekTo = {}, lyrics = lyricsActions,
    )

    val lyricsScreen get() = LyricsScreenState(now, isPlaying = true, lyrics = lyrics, clock = clock)
    val lyricsScreenActions = LyricsScreenActions(noop, noop, {}, lyricsActions)

    val queueState: QueueState get() = QueueState(
        current = now,
        isPlaying = true,
        contextName = "Paper Lanterns",
        contextKind = ContextKind.Album,
        upNext = queue.mapIndexedNotNull { i, t -> if (i > 0) UpNextRow("${t.key}#1", t, i) else null },
        clock = clock,
    )
    val queueActions = QueueActions(noop, noop, {}, {}, { _, _ -> }, {})

    // Library

    val library: LibraryState get() {
        val playlists = listOf(
            Triple(1L, "Long drive", listOf("Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak")),
            Triple(2L, "Sunday cleaning", listOf("Glasshouse", "Patang", "Azulejo", "Filter Coffee Funk")),
            Triple(3L, "Focus", listOf("Kettle On", "Room Tone", "Brushes", "Unsent Drafts")),
        ).map { (id, title, songs) ->
            val tracks = songs.map(DemoLibrary::track)
            LibraryItem("p$id", title, mosaicIds(tracks), Destination.Playlist(id), LibraryKind.Playlist, songCount = 18 + id.toInt() * 7)
        }
        val liked = LibraryItem("liked", "Favourites", destination = Destination.Liked, kind = LibraryKind.Favourites, songCount = 42)
        val folders = lib.folders.map { f ->
            LibraryItem("f${f.name}", f.name, mosaicIds(f.tracks), Destination.Folder(f.name), LibraryKind.Folder, songCount = f.tracks.size)
        }
        val artists = lib.artists.map { a ->
            LibraryItem("r${a.key}", a.name, listOf(a.tracks.first().mediaId), Destination.Artist(a.key), LibraryKind.Artist, songCount = a.tracks.size)
        }
        val albums = lib.albums.map { a ->
            LibraryItem("a${a.key}", a.name, listOf(a.tracks.first().mediaId), Destination.Album(a.key), LibraryKind.Album, a.tracks.size, a.artist)
        }
        return LibraryState(
            filter = null,
            sort = SongSort.Title,
            grid = false,
            creating = false,
            songs = lib.tracks,
            items = listOf(liked) + playlists + folders + artists.take(4) + albums,
            currentKey = now.key,
            isPlaying = true,
        )
    }

    val libraryActions = LibraryActions(
        toggleFilter = { _: LibraryFilter -> }, cycleSort = noop, setSort = {}, toggleGrid = noop, openSettings = noop,
        openSearch = noop, open = {}, startCreating = noop, dismissCreating = noop, create = {}, shuffleAll = noop,
        play = {}, showMenu = {},
    )

    // Search

    /** The app's matching, simplified: name starts with the query first, then contains it. */
    fun search(query: String): SearchState {
        val q = query.lowercase()
        fun <T> List<T>.matching(name: (T) -> String): List<T> =
            filter { name(it).lowercase().contains(q) }.sortedBy { if (name(it).lowercase().startsWith(q)) 0 else 1 }
        val results = SearchResults(
            tracks = lib.tracks.matching { it.title },
            artists = lib.artists.matching { it.name }.take(5),
            albums = lib.albums.matching { it.name }.take(10),
            folders = lib.folders.matching { it.name },
        )
        return SearchState(query, results, browseTiles, now.key, isPlaying = true)
    }

    val searchBrowse: SearchState get() = SearchState("", null, browseTiles, now.key, isPlaying = true)

    private val browseTiles: List<BrowseTile> get() =
        lib.folders.map { f ->
            BrowseTile(f.name, f.tracks.first().mediaId, Destination.Folder(f.name), f.tracks.size, mosaicIds = mosaicIds(f.tracks))
        } + lib.albums.sortedByDescending { it.tracks.size }.take(8).map { a ->
            BrowseTile(a.name, a.tracks.first().mediaId, Destination.Album(a.key), a.tracks.size, a.artist)
        }

    val searchActions = SearchActions({}, noop, {}, {}, {})

    // Settings

    fun settings(style: UiStyle, dark: Boolean) = SettingsState(
        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
        uiStyle = style,
        autoplay = true,
        bulkLyrics = BulkProgress(done = 0, total = 0, found = 0, running = false),
        savedLyricsCount = 51,
        songCount = lib.tracks.size,
        analysis = AnalysisProgress(lib.tracks.size, lib.tracks.size, running = false),
        backup = BackupUiState(saving = false, restoring = false, confirm = null, restored = null, problem = null),
    )

    val settingsActions = SettingsActions(noop, {}, {}, {}, noop, noop, noop, noop, noop, noop, noop)

    // Recap

    val wrapped: WrappedData get() {
        fun key(title: String) = DemoLibrary.track(title).key
        val songs = listOf(
            "Lights Over the Water" to 96, "Iravu Rail" to 81, "Chai Ka Doosra Cup" to 74, "Neon Peaches" to 69,
            "Night Drive Firmware" to 63, "Kadalkarai" to 57, "Marea Lenta" to 52, "Kettle On" to 49,
            "Platform Ettu" to 44, "Patang" to 41,
        ).map { (title, plays) -> RankedSong(key(title), plays, plays * 232_000L) }
        val artists = listOf(
            "Mira Okafor" to 412, "Kavya Raman" to 356, "Ruhaan Sethi" to 298, "Juniper Vale" to 201, "Velvet Circuit" to 177,
        ).map { (name, plays) -> RankedArtist(DemoLibrary.artist(name).key, plays, plays * 225_000L) }
        val hours = LongArray(24) { h ->
            val evening = if (h in 19..23) 3.2 else 1.0
            val night = if (h in 0..1) 2.0 else 1.0
            val asleep = if (h in 2..6) 0.05 else 1.0
            ((40 + 30 * kotlin.math.sin(h / 24.0 * Math.PI)) * evening * night * asleep * 60_000 * 9).toLong()
        }
        val listened = 18_420L * 60_000
        val recap = Recap(
            listenedMs = listened,
            plays = 4_870,
            songCount = lib.tracks.size,
            artistCount = lib.artists.size,
            topSongs = songs,
            topArtists = artists,
            languages = listOf(
                LanguageShare("en", (listened * 0.41).toLong(), 0.41f),
                LanguageShare("ta", (listened * 0.29).toLong(), 0.29f),
                LanguageShare("hi", (listened * 0.21).toLong(), 0.21f),
                LanguageShare(LanguageLabels.NO_VOCALS, (listened * 0.09).toLong(), 0.09f),
            ),
            hours = hours,
            daysListened = 301,
            longestStreak = 46,
            biggestDay = DayTotal(LocalDate.of(2026, 8, 15), 5 * 3_600_000L + 42 * 60_000L),
            onRepeat = RepeatDay(key("Iravu Rail"), LocalDate.of(2026, 3, 9), 14),
            discoveries = 38,
            radioShare = 0.22f,
            firstListen = Listen(key("Kettle On"), 1_767_225_600_000L, 136_000L, "complete", "Tea at 2 AM"),
            loggedSince = 1_735_689_600_000L,
        )
        return wrappedData(recap, RecapPeriod.Year, lib, today)
    }

    val wrappedPage: WrappedPageState get() {
        val data = wrapped
        return WrappedPageState(
            period = RecapPeriod.Year,
            data = data,
            loading = false,
            cards = WrappedCard.available(data),
            storyAt = null,
            topSongsPlaying = false,
            currentKey = now.key,
            playing = true,
        )
    }

    val wrappedActions = WrappedPageActions(noop, {}, {}, noop, noop, {}, {}, {})
}
