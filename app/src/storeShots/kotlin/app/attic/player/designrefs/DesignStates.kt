package app.attic.player.designrefs

import android.content.Context
import androidx.media3.common.Player
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.data.BulkProgress
import app.attic.player.data.OnboardingStep
import app.attic.player.data.ThemeMode
import app.attic.player.data.backup.RestoreReport
import app.attic.player.ui.model.AccessState
import app.attic.player.ui.model.BackupSummary
import app.attic.player.ui.model.BackupUiState
import app.attic.player.ui.model.LibraryScan
import app.attic.player.ui.model.OnboardingActions
import app.attic.player.ui.model.OnboardingState
import app.attic.player.ui.model.RestoreProblem
import app.attic.player.data.ExternalTrack
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Track
import app.attic.player.data.UiStyle
import app.attic.player.data.UserPlaylist
import app.attic.player.playback.SleepChoice
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.DemoStates
import app.attic.player.storeshots.EqualiserDemo
import app.attic.player.playback.eq.EqScope
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.model.AddSongsActions
import app.attic.player.ui.model.AddSongsState
import app.attic.player.ui.model.AlbumState
import app.attic.player.ui.model.ContextKind
import app.attic.player.ui.model.DeletePlaylistActions
import app.attic.player.ui.model.DeletePlaylistState
import app.attic.player.ui.model.FolderActions
import app.attic.player.ui.model.FolderState
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryKind
import app.attic.player.ui.model.LibraryState
import app.attic.player.ui.model.LicenceState
import app.attic.player.ui.model.LicencesActions
import app.attic.player.ui.model.LikedActions
import app.attic.player.ui.model.LikedState
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.model.LyricsState
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.OpenSourceNotices
import app.attic.player.ui.model.PickerPlaylist
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlaybackClock
import app.attic.player.ui.model.PlayControlsState
import app.attic.player.ui.model.PlaylistActions
import app.attic.player.ui.model.PlaylistNameActions
import app.attic.player.ui.model.PlaylistNameState
import app.attic.player.ui.model.PlaylistPickerActions
import app.attic.player.ui.model.PlaylistPickerState
import app.attic.player.ui.model.PlaylistState
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.SettingsState
import app.attic.player.ui.model.TrackMenuActions
import app.attic.player.ui.model.TrackMenuState
import app.attic.player.ui.model.SleepTimerMenuState
import app.attic.player.ui.model.UpNextRow
import app.attic.player.ui.model.WrappedPageState
import app.attic.player.ui.model.WrappedStoryActions
import app.attic.player.ui.model.WrappedStoryState
import app.attic.player.ui.model.licenceParagraphs
import java.io.File

/**
 * The states the design references need beyond the store shots' [DemoStates]: every filter, sheet,
 * dialog and edge state, built from the same demo library. Actions do nothing.
 */
object DesignStates {
    private val s = DemoStates
    private val lib get() = DemoLibrary.library
    private val now: Track get() = DemoLibrary.nowPlaying
    private val noop: () -> Unit = {}

    // Home

    fun home(filter: HomeFilter): HomeState {
        val h = s.home
        return HomeState(
            isEmpty = false, filter = filter, shortcuts = h.shortcuts, wrapped = h.wrapped, speedDial = h.speedDial, tasteMixes = h.tasteMixes,
            recentTracks = h.recentTracks, onRepeat = h.onRepeat, notLately = h.notLately, newlyAdded = h.newlyAdded, startListening = h.startListening, folders = h.folders,
            biggestAlbums = h.biggestAlbums, biggestArtists = h.biggestArtists, spotlight = h.spotlight,
            currentKey = h.currentKey, isPlaying = h.isPlaying, favouriteCount = h.favouriteCount, radioSeedKey = h.radioSeedKey,
        )
    }

    /** A phone with access granted and no music on it. */
    val homeEmpty = HomeState(
        isEmpty = true, filter = HomeFilter.All, shortcuts = emptyList(), wrapped = null, speedDial = emptyList(), tasteMixes = emptyList(),
        recentTracks = emptyList(), onRepeat = emptyList(), notLately = emptyList(), newlyAdded = emptyList(), startListening = emptyList(), folders = emptyList(), biggestAlbums = emptyList(),
        biggestArtists = emptyList(), spotlight = null, currentKey = null, isPlaying = false, favouriteCount = 0, radioSeedKey = null,
    )

    // Library

    fun library(filter: LibraryFilter?, grid: Boolean = false): LibraryState {
        val all = s.library
        val items = when (filter) {
            null -> all.items
            LibraryFilter.Playlists -> all.items.filter { it.kind in setOf(LibraryKind.Favourites, LibraryKind.Playlist, LibraryKind.Folder) }
            LibraryFilter.Albums -> all.items.filter { it.kind == LibraryKind.Album }
            LibraryFilter.Artists -> lib.artists.map { a ->
                app.attic.player.ui.model.LibraryItem(
                    "r${a.key}", a.name, listOf(a.tracks.first().mediaId),
                    app.attic.player.ui.model.Destination.Artist(a.key), LibraryKind.Artist, songCount = a.tracks.size,
                )
            }
            LibraryFilter.Downloaded -> emptyList()
        }
        return LibraryState(filter, all.sort, grid, creating = false, songs = all.songs, items = items, currentKey = all.currentKey, isPlaying = true)
    }

    // Collections

    private fun controls(name: String, playing: Boolean = false, enabled: Boolean = true) =
        PlayControlsState(name, enabled = enabled, shuffleOn = false, isPlaying = playing)
    private val controlsActions = PlayControlsActions(noop, noop, noop)

    val albumMissing = AlbumState(album = null, artistMediaId = null, controls = controls("", enabled = false), currentKey = now.key, isPlaying = true)

    fun folder(name: String) = FolderState(lib.folders.first { it.name == name }, controls(name), now.key, isPlaying = true)
    val folderActions = FolderActions(noop, controlsActions, {}, {})

    private val likedTracks: List<Track> get() = listOf(
        "Lights Over the Water", "Iravu Rail", "Chai Ka Doosra Cup", "Neon Peaches", "Marea Lenta", "Night Drive Firmware",
        "Kettle On", "Filter Coffee Funk", "Platform Ettu", "Patang", "Room Tone", "Bheegi Sadak",
    ).map(DemoLibrary::track)

    val favourites get() = LikedState(likedTracks, controls("Favourites"), now.key, isPlaying = true)
    val likedActions = LikedActions(noop, controlsActions, {}, {})

    private val longDrive: List<Track> get() = listOf(
        "Night Drive Firmware", "Iravu Rail", "Neon Peaches", "Bheegi Sadak", "Kadaisi Vandi", "Drift Mode",
        "Sheher Ki Shaam", "Swimming Pool Static", "Faro Apagado", "Midnight Checksum", "Auto Rickshaw Waltz", "Second Bus Home",
    ).map(DemoLibrary::track)

    val playlistModel get() = UserPlaylist(1L, "Long drive", longDrive.map { it.key })

    fun playlist(reordering: Boolean = false, overflowOpen: Boolean = false) = PlaylistState(
        playlist = playlistModel,
        tracks = longDrive,
        reordering = reordering,
        overflowOpen = overflowOpen,
        renaming = false,
        confirmingDelete = false,
        adding = false,
        controls = controls("Long drive"),
        currentKey = now.key,
        isPlaying = true,
    )

    val playlistActions = PlaylistActions(
        back = noop, controls = controlsActions, openOverflow = noop, dismissOverflow = noop, startRenaming = noop,
        dismissRenaming = noop, rename = {}, startDeleting = noop, dismissDeleting = noop, delete = noop, startAdding = noop,
        dismissAdding = noop, toggleReordering = noop, reorder = {}, play = {}, showMenu = {}, toggleSmartShuffle = noop,
    )

    // Sheets and dialogs

    fun trackMenu(
        track: Track = DemoLibrary.track("Porch Light"),
        fromNowPlaying: Boolean = false,
        timer: SleepTimerMenuState? = null,
        pickingLanguage: Boolean = false,
        pickingSleepTimer: Boolean = false,
        inPlaylist: Boolean = false,
    ): TrackMenuState {
        val external = ExternalTrack.isExternal(track.key)
        val artist = lib.artists.firstOrNull { it.name == track.artist }
        return TrackMenuState(
            track = track,
            liked = !external && track.key in likedTracks.map { it.key }.toSet(),
            languageName = "English",
            inPlaylist = inPlaylist,
            canGoToAlbum = !external,
            canGoToArtist = !external && artist != null,
            pickingPlaylist = false,
            pickingLanguage = pickingLanguage,
            language = if (external) null else "en",
            albumName = track.album.takeIf { !external },
            artistName = artist?.name,
            artistCover = artist?.tracks?.first()?.mediaId,
            external = external,
            canShare = !external && track.mediaId >= 0,
            sleepTimer = if (fromNowPlaying) (timer ?: SleepTimerMenuState(SleepChoice.Off, null)) else null,
            pickingSleepTimer = pickingSleepTimer,
            equaliser = if (fromNowPlaying) EqualiserDemo.menu else null,
        )
    }

    val trackMenuActions = TrackMenuActions(
        startRadio = noop, toggleLike = noop, playNext = noop, addToQueue = noop, pickPlaylist = noop, pickLanguage = noop,
        removeFromPlaylist = noop, goToAlbum = noop, goToArtist = noop, chooseLanguage = {}, dismiss = noop,
    )

    /** A countdown with 23 minutes and a bit left. */
    val timerRunning = SleepTimerMenuState(SleepChoice.Minutes30, 22 * 60_000L + 41_000L)

    val playlistPicker: PlaylistPickerState get() = PlaylistPickerState(
        playlists = listOf(
            Triple(1L, "Long drive", longDrive),
            Triple(2L, "Sunday cleaning", listOf("Glasshouse", "Patang", "Azulejo", "Filter Coffee Funk", "Porch Light").map(DemoLibrary::track)),
            Triple(3L, "Focus", listOf("Kettle On", "Room Tone", "Brushes", "Unsent Drafts").map(DemoLibrary::track)),
        ).map { (id, name, tracks) ->
            PickerPlaylist(id, name, tracks.size, alreadyIn = tracks.any { it.title == "Porch Light" }, coverIds = mosaicIds(tracks))
        },
        creating = false,
        songTitle = "Porch Light",
        songCount = 1,
    )
    val playlistPickerActions = PlaylistPickerActions({}, noop, noop, {}, noop)

    val addSongs: AddSongsState get() = AddSongsState(
        query = "",
        matches = lib.tracks,
        existing = longDrive.map { it.key }.toSet(),
        playlistName = "Long drive",
    )
    val addSongsActions = AddSongsActions({}, {}, noop)

    fun nameDialog(title: String, confirm: String, name: String) = PlaylistNameState(title, confirm, name)
    val nameActions = PlaylistNameActions({}, noop, noop)
    val deleteDialog = DeletePlaylistState("Long drive")
    val deleteActions = DeletePlaylistActions(noop, noop)

    // Now Playing, queue and lyrics

    /** The demo song with the tags the Credits card reads beyond artist, album and year. */
    val nowTagged: Track get() = now.copy(composer = "Mira Okafor, Idris Bello", genre = "Indie folk")

    /** DemoStates' Now Playing, on [nowTagged]. */
    val nowPlaying: NowPlayingState get() {
        val n = s.nowPlaying
        return NowPlayingState(
            track = nowTagged, contextName = n.contextName, contextKind = n.contextKind, isPlaying = n.isPlaying, shuffle = n.shuffle,
            repeatMode = n.repeatMode, queue = listOf(nowTagged) + n.queue.drop(1), currentIndex = 0, liked = n.liked, artist = n.artist,
            lyrics = n.lyrics, clock = n.clock,
        )
    }

    private val radioQueue: List<Track> get() = listOf(now) + listOf(
        "Porch Light", "Clear by Morning", "Glasshouse", "Late Bloom", "Moth Letters", "Low Tide Hymn", "Umbrella Song",
        "Sunburn Choir", "Coming Back Slow", "Fog on Garden Road",
    ).map(DemoLibrary::track)

    val radioContext = "Radio · ${now.title}"

    /** Now Playing for a song with its own equaliser setting: the header shows the indicator. */
    val nowPlayingWithEqualiser: NowPlayingState get() = nowPlayingWithEqualiser(EqScope.Song)

    /** Now Playing for a song that plays with its album's setting: the indicator reads "This album". */
    val nowPlayingWithAlbumEqualiser: NowPlayingState get() = nowPlayingWithEqualiser(EqScope.Album)

    private fun nowPlayingWithEqualiser(scope: EqScope): NowPlayingState {
        val n = nowPlaying
        return NowPlayingState(
            track = n.track, contextName = n.contextName, contextKind = n.contextKind, isPlaying = n.isPlaying, shuffle = n.shuffle,
            repeatMode = n.repeatMode, queue = n.queue, currentIndex = n.currentIndex, liked = n.liked, artist = n.artist,
            lyrics = n.lyrics, clock = n.clock, equaliser = scope,
        )
    }

    val nowPlayingPausedRadio: NowPlayingState get() {
        val n = s.nowPlaying
        return NowPlayingState(
            track = nowTagged, contextName = radioContext, contextKind = ContextKind.Radio, isPlaying = false, shuffle = true,
            repeatMode = Player.REPEAT_MODE_ONE, queue = radioQueue, currentIndex = 0, liked = true, artist = n.artist,
            lyrics = n.lyrics, clock = n.clock,
        )
    }

    /** A voice note opened from a file manager: not in the library, so no cover, no artist page, no lyrics. */
    val openWithTrack = Track(
        key = ExternalTrack.keyFor("content://com.android.externalstorage.documents/document/primary%3ARecordings%2Fband-practice.m4a"),
        mediaId = -1L,
        title = "band-practice",
        artist = "",
        artistKeys = emptyList(),
        album = null,
        albumKey = null,
        folder = null,
        trackNumber = null,
        durationMs = 3 * 60_000L + 7_000L,
        year = null,
        dateAddedSec = 0L,
    )

    val noLyrics = LyricsState(emptyList(), null, checked = false, found = false, instrumental = false, hasLyrics = false, downloading = false)

    val nowPlayingOpenWith: NowPlayingState get() {
        val n = s.nowPlaying
        // It goes in right after the current song, and "Playing from" stays what it was.
        val queue = listOf(now, openWithTrack) + n.queue.drop(1)
        return NowPlayingState(
            track = openWithTrack, contextName = n.contextName, contextKind = n.contextKind, isPlaying = true, shuffle = false,
            repeatMode = Player.REPEAT_MODE_OFF, queue = queue, currentIndex = 1, liked = false, artist = null,
            lyrics = noLyrics, clock = PlaybackClock { 41_000L },
        )
    }

    val miniPaused get() = MiniPlayerState(now, isPlaying = false, liked = true, clock = s.clock)
    val miniOpenWith get() = MiniPlayerState(openWithTrack, isPlaying = true, liked = false, clock = PlaybackClock { 41_000L })

    val queueRadio: QueueState get() = QueueState(
        current = now, isPlaying = true, contextName = radioContext, contextKind = ContextKind.Radio,
        upNext = radioQueue.mapIndexedNotNull { i, t -> if (i > 0) UpNextRow("${t.key}#1", t, i) else null }, clock = s.clock,
    )

    val queueEmpty: QueueState get() = QueueState(
        current = now, isPlaying = true, contextName = "Paper Lanterns", contextKind = ContextKind.Album, upNext = emptyList(), clock = s.clock,
    )

    private val plainText = DemoLibrary.lyrics.joinToString("\n") { it.text }.replace(Regex("\n{2,}"), "\n\n").trim()

    fun lyricsScreen(lyrics: LyricsState) = LyricsScreenState(now, isPlaying = true, lyrics = lyrics, clock = s.clock)

    val lyricsPlain = LyricsState(emptyList(), plainText, checked = true, found = true, instrumental = false, hasLyrics = true, downloading = false)
    val lyricsNotFound = LyricsState(emptyList(), null, checked = true, found = false, instrumental = false, hasLyrics = false, downloading = false)
    val lyricsInstrumental = LyricsState(emptyList(), null, checked = true, found = true, instrumental = true, hasLyrics = false, downloading = false)
    val lyricsDownloading = LyricsState(emptyList(), null, checked = false, found = false, instrumental = false, hasLyrics = false, downloading = true)

    // Settings and licences

    fun settings(
        style: UiStyle,
        dark: Boolean,
        analysing: Boolean = false,
        downloadingLyrics: Boolean = false,
        backup: BackupUiState = base(style, dark).backup,
    ): SettingsState {
        val base = s.settings(style, dark)
        val total = lib.tracks.size
        return SettingsState(
            themeMode = base.themeMode,
            uiStyle = base.uiStyle,
            autoplay = base.autoplay,
            bulkLyrics = if (downloadingLyrics) BulkProgress(done = 23, total = total, found = 17, running = true) else base.bulkLyrics,
            savedLyricsCount = base.savedLyricsCount,
            songCount = total,
            analysis = if (analysing) AnalysisProgress(done = 41, total = total, running = true) else base.analysis,
            backup = backup,
            volumeLevelling = base.volumeLevelling,
            crossfadeSeconds = base.crossfadeSeconds,
            equaliser = base.equaliser,
        )
    }

    private fun base(style: UiStyle, dark: Boolean) = s.settings(style, dark)

    /** "Restore this backup?" for a backup made on 2 November 2026 on another phone. */
    val restoreConfirm = BackupUiState(
        saving = false, restoring = false,
        confirm = BackupSummary(createdAt = 1_793_620_800_000L, likes = 42, playlists = 3, plays = 4_870, lyrics = 51),
        restored = null, problem = null,
    )

    /** What a restore onto a phone with part of the library reports. */
    val restoreResult = BackupUiState(
        saving = false, restoring = false, confirm = null,
        restored = RestoreReport(
            likesAdded = 38, playlistsAdded = 2, playlistsMerged = 1, listensAdded = 4_312, lyricsAdded = 47,
            songsMatched = 64, songsMissing = 5,
        ),
        problem = null,
    )

    val restoreProblem = BackupUiState(saving = false, restoring = false, confirm = null, restored = null, problem = RestoreProblem.NotABackup)

    // First run

    fun onboarding(
        step: OnboardingStep,
        style: UiStyle,
        dark: Boolean,
        audio: AccessState = AccessState.NotAsked,
        notifications: AccessState = AccessState.NotAsked,
        scan: LibraryScan = LibraryScan.Scanning,
    ) = OnboardingState(step, style, if (dark) ThemeMode.Dark else ThemeMode.Light, audio, notifications, scan, downloadLyrics = false)

    val onboardingActions = OnboardingActions(
        next = noop, back = noop, setUiStyle = {}, setThemeMode = {}, allowAudio = noop, openAppPermissions = noop,
        allowNotifications = noop, setDownloadLyrics = {}, openRadioSettings = noop, finish = noop,
    )

    val licencesActions = LicencesActions(noop, {})

    /** Attic's own licence, as Settings, Open-source licences, Attic shows it. */
    fun licence(context: Context): LicenceState {
        val notice = OpenSourceNotices.first()
        val text = runCatching { context.assets.open("licences/${notice.files.first()}").bufferedReader().use { it.readText() } }
            .getOrElse { listOf(File("../LICENSE"), File("LICENSE")).first { it.exists() }.readText() }
        return LicenceState(notice, licenceParagraphs(text))
    }

    // Recap

    fun story(page: Int): WrappedStoryState {
        val wrapped: WrappedPageState = s.wrappedPage
        return WrappedStoryState(wrapped.data!!, wrapped.cards, page, progress = { 0.45f })
    }

    val storyActions = WrappedStoryActions(setHeld = {}, tap = {}, close = noop, save = {}, share = {})

    val languageChoices = LanguageLabels.CHOICES
}
