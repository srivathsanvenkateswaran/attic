package app.attic.player.designrefs

import app.attic.player.ui.comfort.ComfortInterests
import app.attic.player.ui.comfort.ComfortMood
import app.attic.player.ui.expressive.ExpressiveInterests
import app.attic.player.ui.expressive.ExpressiveMood
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.UiStyle
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.DemoStates
import app.attic.player.storeshots.EqualiserDemo
import app.attic.player.ui.comfort.ComfortEqualiserPage
import app.attic.player.ui.comfort.ComfortEqualiserSheet
import app.attic.player.ui.expressive.ExpressiveEqualiserPage
import app.attic.player.ui.expressive.ExpressiveEqualiserSheet
import app.attic.player.ui.model.EqualiserBands
import app.attic.player.ui.model.EqualiserSheetState
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.comfort.ComfortAddSongs
import app.attic.player.ui.comfort.ComfortAlbum
import app.attic.player.ui.comfort.ComfortArtist
import app.attic.player.ui.comfort.ComfortDeletePlaylistDialog
import app.attic.player.ui.comfort.ComfortFavourites
import app.attic.player.ui.comfort.ComfortFolder
import app.attic.player.ui.comfort.ComfortHome
import app.attic.player.ui.comfort.ComfortLibrary
import app.attic.player.ui.comfort.ComfortLicence
import app.attic.player.ui.comfort.ComfortLicences
import app.attic.player.ui.comfort.ComfortLoading
import app.attic.player.ui.comfort.ComfortLyrics
import app.attic.player.ui.comfort.ComfortMiniPlayer
import app.attic.player.ui.comfort.ComfortNowPlaying
import app.attic.player.ui.comfort.ComfortOnboarding
import app.attic.player.ui.comfort.ComfortPermission
import app.attic.player.ui.comfort.ComfortPlaylist
import app.attic.player.ui.comfort.ComfortPlaylistNameDialog
import app.attic.player.ui.comfort.ComfortPlaylistPicker
import app.attic.player.ui.comfort.ComfortQueue
import app.attic.player.ui.comfort.ComfortRecap
import app.attic.player.ui.comfort.ComfortRecapCard
import app.attic.player.ui.comfort.ComfortRecapStory
import app.attic.player.ui.comfort.ComfortSearch
import app.attic.player.ui.comfort.ComfortSettings
import app.attic.player.ui.comfort.ComfortShell
import app.attic.player.ui.comfort.ComfortTrackMenu
import app.attic.player.ui.comfort.TintedCover
import app.attic.player.ui.expressive.ExpressiveAddSongs
import app.attic.player.ui.expressive.ExpressiveAlbum
import app.attic.player.ui.expressive.ExpressiveArtist
import app.attic.player.ui.expressive.ExpressiveDeletePlaylistDialog
import app.attic.player.ui.expressive.ExpressiveFavourites
import app.attic.player.ui.expressive.ExpressiveFolder
import app.attic.player.ui.expressive.ExpressiveHome
import app.attic.player.ui.expressive.ExpressiveLanguagePicker
import app.attic.player.ui.expressive.ExpressiveLibrary
import app.attic.player.ui.expressive.ExpressiveLicence
import app.attic.player.ui.expressive.ExpressiveLicences
import app.attic.player.ui.expressive.ExpressiveLoading
import app.attic.player.ui.expressive.ExpressiveLyrics
import app.attic.player.ui.expressive.ExpressiveMiniPlayer
import app.attic.player.ui.expressive.ExpressiveNowPlaying
import app.attic.player.ui.expressive.ExpressiveOnboarding
import app.attic.player.ui.expressive.ExpressivePermission
import app.attic.player.ui.expressive.ExpressivePlaylist
import app.attic.player.ui.expressive.ExpressivePlaylistNameDialog
import app.attic.player.ui.expressive.ExpressivePlaylistPicker
import app.attic.player.ui.expressive.ExpressiveQueue
import app.attic.player.ui.expressive.ExpressiveRecap
import app.attic.player.ui.expressive.ExpressiveRecapCard
import app.attic.player.ui.expressive.ExpressiveRecapStory
import app.attic.player.ui.expressive.ExpressiveSearch
import app.attic.player.ui.expressive.ExpressiveSettings
import app.attic.player.ui.expressive.ExpressiveShell
import app.attic.player.ui.expressive.ExpressiveSleepTimerPicker
import app.attic.player.ui.expressive.ExpressiveTrackMenu
import app.attic.player.ui.expressive.FromCover
import app.attic.player.data.OnboardingStep
import app.attic.player.ui.model.AccessState
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.LibraryScan
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsState
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.OpenSourceNotices
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.RootActions
import app.attic.player.ui.model.RootState
import app.attic.player.ui.model.ShellState
import app.attic.player.ui.model.TrackMenuState
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.languagePickerModel
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel references for the design files: every screen and state of both styles, light and dark, drawn
 * from the demo library as the store shots are. Written to `<ShotsDir>/<style>-<theme>-<key>.png`;
 * run through scripts/store_shots/design_refs.sh. Keys a style cannot draw are skipped (see [Ref.of]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class DesignRefs(private val style: UiStyle, private val dark: Boolean, private val key: String) {
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * The whole-page keys get a display as tall as [Ref.FULL_HEIGHT] (4571dp at 420dpi), set before the
     * activity starts, so a page lays out every row at once; a window taller than its display draws
     * wrongly under Robolectric. design_refs.sh then trims the empty ground between the last row and the dock.
     */
    private val tallDisplay = object : ExternalResource() {
        override fun before() {
            if (key in Ref.TallKeys) RuntimeEnvironment.setQualifiers("w411dp-h4571dp-420dpi")
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(RegisterDesignActivity).around(tallDisplay).around(compose)

    @Test
    fun shot() = render()

    private fun render() {
        val ref = Ref.of(style, key) ?: return
        val name = "${style.name.lowercase()}-${if (dark) "dark" else "light"}-$key"
        compose.shootRef(
            compose.activity, name, style, dark,
            height = ref.height, capture = ref.capture, ime = ref.ime, bars = ref.bars, stitch = ref.stitch, prepare = ref.prepare,
            content = ref.content,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun shots(): List<Array<Any>> = buildList {
            // The tall ones last, so no screen is drawn after the display has been made tall.
            for (keys in listOf(Ref.Keys, Ref.TallKeys)) {
                for (style in UiStyle.entries) for (dark in listOf(true, false)) for (key in keys) add(arrayOf(style, dark, key))
            }
        }
    }
}

/** How to draw one key. */
class Ref(
    val content: @Composable () -> Unit,
    val height: Int = 2400,
    val capture: Capture = Capture.Window,
    val ime: Boolean = false,
    val bars: Boolean = true,
    val stitch: Boolean = false,
    val prepare: (ComposeContentTestRule.() -> Unit)? = null,
) {
    companion object {
        /** Tall enough for any page here; design_refs.sh trims the empty part above the dock. */
        const val FULL_HEIGHT = 12_000

        val RecapCards: List<WrappedCard> get() = WrappedCard.available(DemoStates.wrapped)
        private fun cardKey(card: WrappedCard) = card.fileName.replace('-', '_')

        val Keys: List<String> = listOf(
            "home", "home_albums", "home_artists", "home_folders",
            "search_browse", "search_typing", "search_results", "search_no_results",
            "library_list", "library_grid", "library_songs", "library_playlists", "library_albums", "library_artists",
            "album", "album_collapsed", "artist", "playlist", "playlist_reorder", "folder", "favourites", "item_missing",
            "now_playing", "now_playing_full", "now_playing_paused_radio", "open_with",
            "queue", "queue_radio", "queue_empty",
            "lyrics_synced", "lyrics_plain", "lyrics_none", "lyrics_not_downloaded", "lyrics_instrumental", "lyrics_downloading",
            "sheet_track_menu", "sheet_track_menu_playlist", "sheet_track_menu_now_playing", "sheet_track_menu_open_with",
            "sheet_sleep_timer", "sleep_timer_active",
            "sheet_equaliser", "sheet_equaliser_song", "sheet_equaliser_album", "sheet_equaliser_off", "dialog_equaliser_save_preset",
            "sheet_equaliser_band_dragged", "menu_equaliser_preset", "settings_equaliser_off",
            "now_playing_equaliser", "settings_equaliser", "settings_equaliser_empty", "sheet_playlist_picker", "sheet_language_picker", "sheet_add_songs",
            "dialog_new_playlist", "dialog_name_playlist", "dialog_rename_playlist", "dialog_delete_playlist", "menu_playlist_overflow",
            "snackbar", "snackbar_over_player",
            "settings", "settings_analysis", "settings_lyrics_download", "settings_backup",
            "dialog_restore_confirm", "dialog_restore_result", "dialog_restore_problem", "licences", "licence_text",
            "recap",
            "moods_home", "moods_home_ask", "mood_page", "mood_page_playing", "moods_browse", "moods_results",
            "moods_picker_empty", "moods_picker", "moods_picker_no_genres", "moods_settings",
        ) + RecapCards.map { "recap_story_${cardKey(it)}" } + RecapCards.map { "recap_share_${cardKey(it)}" } + listOf(
            "permission", "permission_refused", "loading", "empty_library",
            "mini_player_paused", "mini_player_open_with",
            "onboarding_welcome", "onboarding_look", "onboarding_access", "onboarding_access_granted", "onboarding_access_refused",
            "onboarding_library_scanning", "onboarding_library_found", "onboarding_library_empty",
        )

        /** Whole pages, drawn on a phone as tall as [FULL_HEIGHT] (see DesignRefs.tallDisplay). */
        val TallKeys: List<String> = listOf("home_full", "settings_full", "recap_full", "settings_equaliser_full")

        private val s = DemoStates
        private val d = DesignStates

        fun of(style: UiStyle, key: String): Ref? {
            val e = style == UiStyle.Expressive
            fun shell(tab: Tab, body: @Composable () -> Unit) = Ref({ InShell(style, tab, content = body) })
            fun overlayRef(
                capture: Capture = Capture.Window,
                nowPlaying: NowPlayingState? = d.nowPlaying,
                queue: QueueState? = null,
                lyrics: LyricsState? = null,
                message: Int? = null,
                sheet: (@Composable () -> Unit)? = null,
            ) = Ref(
                {
                    InShell(style, Tab.Home, nowPlaying = nowPlaying, queue = queue, lyrics = lyrics, message = message) {
                        if (e) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions)
                    }
                    sheet?.invoke()
                },
                capture = capture,
            )
            fun overHome(sheet: @Composable () -> Unit) = Ref(
                {
                    InShell(style, Tab.Home) { if (e) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions) }
                    sheet()
                },
                capture = Capture.Screen,
            )
            fun overPlaylist(sheet: @Composable () -> Unit) = Ref(
                {
                    InShell(style, Tab.Library) {
                        if (e) ExpressivePlaylist(d.playlist(), d.playlistActions) else ComfortPlaylist(d.playlist(), d.playlistActions)
                    }
                    sheet()
                },
                capture = Capture.Screen,
            )
            fun menu(state: TrackMenuState, overPlayer: Boolean = false): @Composable () -> Unit = { TrackMenu(style, state, overPlayer) }
            fun home(filter: HomeFilter) = shell(Tab.Home) {
                if (e) ExpressiveHome(d.home(filter), s.homeActions) else ComfortHome(d.home(filter), s.homeActions)
            }
            fun library(filter: LibraryFilter?, grid: Boolean = false) = shell(Tab.Library) {
                val state = d.library(filter, grid)
                if (e) ExpressiveLibrary(state, s.libraryActions) else ComfortLibrary(state, s.libraryActions)
            }
            fun search(query: String) = shell(Tab.Search) {
                val state = s.search(query)
                if (e) ExpressiveSearch(state, s.searchActions) else ComfortSearch(state, s.searchActions)
            }
            fun settings(
                analysing: Boolean = false,
                lyrics: Boolean = false,
                height: Int = 2400,
                backup: app.attic.player.ui.model.BackupUiState? = null,
                capture: Capture = Capture.Window,
                prepare: (ComposeContentTestRule.() -> Unit)? = null,
            ) = Ref(
                {
                    InShell(style, Tab.Home) {
                        val dark = LocalIsDark.current
                        val state = if (backup == null) d.settings(style, dark, analysing, lyrics) else d.settings(style, dark, backup = backup)
                        if (e) ExpressiveSettings(state, s.settingsActions) else ComfortSettings(state, s.settingsActions)
                    }
                },
                height = height,
                capture = capture,
                prepare = prepare,
            )
            fun onboarding(
                step: app.attic.player.data.OnboardingStep,
                audio: app.attic.player.ui.model.AccessState = app.attic.player.ui.model.AccessState.NotAsked,
                notifications: app.attic.player.ui.model.AccessState = app.attic.player.ui.model.AccessState.NotAsked,
                scan: app.attic.player.ui.model.LibraryScan = app.attic.player.ui.model.LibraryScan.Scanning,
            ) = Ref({
                val state = d.onboarding(step, style, LocalIsDark.current, audio, notifications, scan)
                if (e) ExpressiveOnboarding(state, d.onboardingActions) else ComfortOnboarding(state, d.onboardingActions)
            })
            // Backup is the last section but About, so the page's end shows it whole in both styles.
            val toBackup: ComposeContentTestRule.() -> Unit = { scrollMain(20_000f) }
            fun lyricsOver(lyrics: LyricsState) = overlayRef(lyrics = lyrics)

            // Comfort's Summary card throws while measuring (SummaryCard's Row(height(IntrinsicSize.Min))
            // holds FitText, a BoxWithConstraints, which cannot answer intrinsics), so it is skipped
            // until that is fixed in ui/comfort/ComfortRecapCards.kt.
            if (!e && key.endsWith("_summary") && key.startsWith("recap_")) return null
            val storyCard = RecapCards.firstOrNull { key == "recap_story_${cardKey(it)}" }
            if (storyCard != null) {
                val page = RecapCards.indexOf(storyCard)
                return Ref(
                    {
                        InShell(style, Tab.Home) { if (e) ExpressiveRecap(s.wrappedPage, s.wrappedActions) else ComfortRecap(s.wrappedPage, s.wrappedActions) }
                        if (e) ExpressiveRecapStory(d.story(page), d.storyActions, emptyFlow())
                        else ComfortRecapStory(d.story(page), d.storyActions, emptyFlow())
                    },
                    capture = Capture.Screen,
                )
            }
            val shareCard = RecapCards.firstOrNull { key == "recap_share_${cardKey(it)}" }
            if (shareCard != null) {
                // The image Save and Share write: the card alone at story size, 1080 by 1920.
                return Ref(
                    {
                        val density = LocalDensity.current
                        val size = with(density) { androidx.compose.ui.unit.DpSize(1080.toDp(), 1920.toDp()) }
                        Box(Modifier.requiredSize(size)) {
                            if (e) ExpressiveRecapCard(shareCard, s.wrapped, Modifier.fillMaxWidth())
                            else ComfortRecapCard(shareCard, s.wrapped, Modifier.fillMaxWidth())
                        }
                    },
                    height = 1920,
                    bars = false,
                )
            }

            return when (key) {
                "home" -> home(HomeFilter.All)
                "home_full" -> Ref(home(HomeFilter.All).content, height = FULL_HEIGHT)
                "home_albums" -> home(HomeFilter.Albums)
                "home_artists" -> home(HomeFilter.Artists)
                "home_folders" -> home(HomeFilter.Folders)

                "search_browse" -> shell(Tab.Search) {
                    if (e) ExpressiveSearch(s.searchBrowse, s.searchActions) else ComfortSearch(s.searchBrowse, s.searchActions)
                }
                "search_typing" -> Ref(search("Ka").content, ime = true, prepare = { onAllNodes(hasSetTextAction())[0].requestFocus() })
                "search_results" -> search("ka")
                "search_no_results" -> search("lullaby")

                "library_list" -> library(null)
                "library_grid" -> library(null, grid = true)
                "library_songs" -> library(LibraryFilter.Downloaded)
                "library_playlists" -> library(LibraryFilter.Playlists)
                "library_albums" -> library(LibraryFilter.Albums)
                "library_artists" -> library(LibraryFilter.Artists)

                "album" -> shell(Tab.Home) {
                    val state = s.album("Paper Lanterns")
                    if (e) ExpressiveAlbum(state, s.albumActions) else ComfortAlbum(state, s.albumActions)
                }
                "album_collapsed" -> Ref(of(style, "album")!!.content, prepare = { scrollMain(1400f) })
                "artist" -> shell(Tab.Home) {
                    val state = s.artist("Kavya Raman")
                    if (e) ExpressiveArtist(state, s.artistActions) else ComfortArtist(state, s.artistActions)
                }
                "playlist" -> shell(Tab.Library) {
                    if (e) ExpressivePlaylist(d.playlist(), d.playlistActions) else ComfortPlaylist(d.playlist(), d.playlistActions)
                }
                "playlist_reorder" -> shell(Tab.Library) {
                    val state = d.playlist(reordering = true)
                    if (e) ExpressivePlaylist(state, d.playlistActions) else ComfortPlaylist(state, d.playlistActions)
                }
                "menu_playlist_overflow" -> Ref(
                    {
                        InShell(style, Tab.Library) {
                            val state = d.playlist(overflowOpen = true)
                            if (e) ExpressivePlaylist(state, d.playlistActions) else ComfortPlaylist(state, d.playlistActions)
                        }
                    },
                    capture = Capture.Screen,
                )
                "folder" -> shell(Tab.Library) {
                    val state = d.folder("Tamil")
                    if (e) ExpressiveFolder(state, d.folderActions) else ComfortFolder(state, d.folderActions)
                }
                "favourites" -> shell(Tab.Library) {
                    if (e) ExpressiveFavourites(d.favourites, d.likedActions) else ComfortFavourites(d.favourites, d.likedActions)
                }
                "item_missing" -> shell(Tab.Home) {
                    if (e) ExpressiveAlbum(d.albumMissing, s.albumActions) else ComfortAlbum(d.albumMissing, s.albumActions)
                }

                "now_playing" -> overlayRef()
                "now_playing_full" -> Ref(overlayRef().content, stitch = true)
                "now_playing_paused_radio" -> overlayRef(nowPlaying = d.nowPlayingPausedRadio)
                "open_with" -> overlayRef(nowPlaying = d.nowPlayingOpenWith)

                "queue" -> overlayRef(queue = s.queueState)
                "queue_radio" -> overlayRef(queue = d.queueRadio)
                "queue_empty" -> overlayRef(queue = d.queueEmpty)

                "lyrics_synced" -> lyricsOver(s.lyrics)
                "lyrics_plain" -> lyricsOver(d.lyricsPlain)
                "lyrics_none" -> lyricsOver(d.lyricsNotFound)
                "lyrics_not_downloaded" -> lyricsOver(d.noLyrics)
                "lyrics_instrumental" -> lyricsOver(d.lyricsInstrumental)
                "lyrics_downloading" -> lyricsOver(d.lyricsDownloading)

                "sheet_track_menu" -> overHome(menu(d.trackMenu()))
                "sheet_track_menu_playlist" -> overPlaylist(menu(d.trackMenu(DemoLibrary.track("Iravu Rail"), inPlaylist = true)))
                "sheet_track_menu_now_playing" ->
                    overlayRef(Capture.Screen, sheet = menu(d.trackMenu(DemoLibrary.nowPlaying, fromNowPlaying = true), overPlayer = true))
                "sheet_track_menu_open_with" -> overlayRef(
                    Capture.Screen,
                    nowPlaying = d.nowPlayingOpenWith,
                    sheet = menu(d.trackMenu(d.openWithTrack, fromNowPlaying = true), overPlayer = true),
                )
                "sheet_sleep_timer" -> overlayRef(
                    Capture.Screen,
                    sheet = menu(d.trackMenu(DemoLibrary.nowPlaying, fromNowPlaying = true, pickingSleepTimer = true), overPlayer = true),
                )
                "sleep_timer_active" -> overlayRef(
                    Capture.Screen,
                    sheet = menu(d.trackMenu(DemoLibrary.nowPlaying, fromNowPlaying = true, timer = d.timerRunning), overPlayer = true),
                )
                "sheet_equaliser" -> overlayRef(Capture.Screen, sheet = { EqSheet(style, EqualiserDemo.sheetDefault) })
                // A song with its own or its album's setting shows the header's indicator behind the sheet, as the app does.
                "sheet_equaliser_song" -> overlayRef(Capture.Screen, nowPlaying = d.nowPlayingWithEqualiser, sheet = { EqSheet(style, EqualiserDemo.sheetSong) })
                "sheet_equaliser_album" ->
                    overlayRef(Capture.Screen, nowPlaying = d.nowPlayingWithAlbumEqualiser, sheet = { EqSheet(style, EqualiserDemo.sheetAlbum) })
                "sheet_equaliser_off" -> overlayRef(Capture.Screen, sheet = { EqSheet(style, EqualiserDemo.sheetOff) })
                "dialog_equaliser_save_preset" ->
                    overlayRef(Capture.Screen, nowPlaying = d.nowPlayingWithEqualiser, sheet = { EqSheet(style, EqualiserDemo.sheetSavePreset) })
                // The 4 kHz band held where it is, so the knob is grown and its value rides above it.
                "sheet_equaliser_band_dragged" -> Ref(
                    overlayRef(Capture.Screen, nowPlaying = d.nowPlayingWithEqualiser, sheet = { EqSheet(style, EqualiserDemo.sheetSong) }).content,
                    capture = Capture.Screen,
                    prepare = { holdBand(7) },
                )
                // A chosen preset of the listener's own opens Rename and Delete when it is tapped again.
                "menu_equaliser_preset" -> Ref(
                    overlayRef(Capture.Screen, sheet = { EqSheet(style, EqualiserDemo.sheetSavedPreset) }).content,
                    capture = Capture.Screen,
                    prepare = { onNode(hasText(EqualiserDemo.SAVED_PRESET)).performClick() },
                )
                "now_playing_equaliser" -> overlayRef(nowPlaying = d.nowPlayingWithEqualiser)
                "settings_equaliser" -> shell(Tab.Home) {
                    val page = EqualiserDemo.page()
                    if (e) ExpressiveEqualiserPage(page, EqualiserDemo.pageActions) else ComfortEqualiserPage(page, EqualiserDemo.pageActions)
                }
                "settings_equaliser_off" -> shell(Tab.Home) {
                    val page = EqualiserDemo.page(on = false)
                    if (e) ExpressiveEqualiserPage(page, EqualiserDemo.pageActions) else ComfortEqualiserPage(page, EqualiserDemo.pageActions)
                }
                // The whole page, down to the lists and the phone's own equaliser.
                "settings_equaliser_full" -> Ref(of(style, "settings_equaliser")!!.content, height = FULL_HEIGHT)
                "settings_equaliser_empty" -> shell(Tab.Home) {
                    val page = EqualiserDemo.page(empty = true)
                    if (e) ExpressiveEqualiserPage(page, EqualiserDemo.pageActions) else ComfortEqualiserPage(page, EqualiserDemo.pageActions)
                }
                "sheet_playlist_picker" -> overHome {
                    if (e) ExpressivePlaylistPicker(d.playlistPicker, d.playlistPickerActions) else ComfortPlaylistPicker(d.playlistPicker, d.playlistPickerActions)
                }
                "sheet_language_picker" -> overHome(menu(d.trackMenu(pickingLanguage = true)))
                "sheet_add_songs" -> overPlaylist {
                    if (e) ExpressiveAddSongs(d.addSongs, d.addSongsActions) else ComfortAddSongs(d.addSongs, d.addSongsActions)
                }
                "dialog_new_playlist" -> overHome {
                    // From the playlist picker's "New playlist": the picker stays under the dialog.
                    if (e) ExpressivePlaylistPicker(d.playlistPicker, d.playlistPickerActions) else ComfortPlaylistPicker(d.playlistPicker, d.playlistPickerActions)
                    val state = d.nameDialog(stringResource(R.string.dialog_new_playlist_title), stringResource(R.string.create), "")
                    if (e) ExpressivePlaylistNameDialog(state, d.nameActions) else ComfortPlaylistNameDialog(state, d.nameActions)
                }
                "dialog_name_playlist" -> Ref(
                    {
                        InShell(style, Tab.Library) { if (e) ExpressiveLibrary(s.library, s.libraryActions) else ComfortLibrary(s.library, s.libraryActions) }
                        val state = d.nameDialog(stringResource(R.string.dialog_name_playlist_title), stringResource(R.string.create), "Road trip")
                        if (e) ExpressivePlaylistNameDialog(state, d.nameActions) else ComfortPlaylistNameDialog(state, d.nameActions)
                    },
                    capture = Capture.Screen,
                )
                "dialog_rename_playlist" -> overPlaylist {
                    val state = d.nameDialog(stringResource(R.string.dialog_rename_playlist_title), stringResource(R.string.rename), "Long drive")
                    if (e) ExpressivePlaylistNameDialog(state, d.nameActions) else ComfortPlaylistNameDialog(state, d.nameActions)
                }
                "dialog_delete_playlist" -> overPlaylist {
                    if (e) ExpressiveDeletePlaylistDialog(d.deleteDialog, d.deleteActions) else ComfortDeletePlaylistDialog(d.deleteDialog, d.deleteActions)
                }
                "snackbar" -> Ref(
                    {
                        InShell(style, Tab.Home, message = R.string.message_added_to_queue) {
                            if (e) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions)
                        }
                    },
                )
                "snackbar_over_player" -> overlayRef(message = R.string.message_playing_next)

                // Mood mixes (docs/features/mood-mixes.md), the Moods section of both design files.
                "moods_home" -> Ref(shell(Tab.Home) { MoodHome(e, ask = false) }.content, prepare = { scrollMain(if (e) 1500f else 1200f) })
                "moods_home_ask" -> Ref(shell(Tab.Home) { MoodHome(e, ask = true) }.content, prepare = { scrollMain(if (e) 1500f else 1200f) })
                "mood_page" -> shell(Tab.Home) {
                    if (e) ExpressiveMood(MoodStates.page(false), MoodStates.pageActions) else ComfortMood(MoodStates.page(false), MoodStates.pageActions)
                }
                "mood_page_playing" -> shell(Tab.Home) {
                    if (e) ExpressiveMood(MoodStates.page(true), MoodStates.pageActions) else ComfortMood(MoodStates.page(true), MoodStates.pageActions)
                }
                "moods_browse" -> Ref(
                    shell(Tab.Search) { if (e) ExpressiveSearch(MoodStates.browse, s.searchActions) else ComfortSearch(MoodStates.browse, s.searchActions) }.content,
                    prepare = { scrollMain(if (e) 900f else 1200f) },
                )
                "moods_results" -> shell(Tab.Search) {
                    val state = MoodStates.results("chi")
                    if (e) ExpressiveSearch(state, s.searchActions) else ComfortSearch(state, s.searchActions)
                }
                "moods_picker_empty" -> Ref({ MoodPicker(e, MoodStates.interests(picked = false)) })
                "moods_picker" -> Ref({ MoodPicker(e, MoodStates.interests(picked = true)) })
                "moods_picker_no_genres" -> Ref({ MoodPicker(e, MoodStates.interests(picked = false, genres = false)) })
                "moods_settings" -> Ref(
                    {
                        InShell(style, Tab.Home) {
                            val state = MoodStates.settings(style, LocalIsDark.current, picked = true)
                            if (e) ExpressiveSettings(state, s.settingsActions) else ComfortSettings(state, s.settingsActions)
                        }
                    },
                    prepare = { scrollMain(if (e) 1700f else 2200f) },
                )
                "settings" -> settings()
                "settings_full" -> settings(height = FULL_HEIGHT)
                "settings_analysis" -> Ref(settings(analysing = true).content, prepare = { scrollMain(6000f) })
                "settings_lyrics_download" -> Ref(settings(lyrics = true).content, prepare = { scrollMain(900f) })
                "settings_backup" -> settings(prepare = toBackup)
                "dialog_restore_confirm" -> settings(backup = d.restoreConfirm, capture = Capture.Screen, prepare = toBackup)
                "dialog_restore_result" -> settings(backup = d.restoreResult, capture = Capture.Screen, prepare = toBackup)
                "dialog_restore_problem" -> settings(backup = d.restoreProblem, capture = Capture.Screen, prepare = toBackup)
                "licences" -> shell(Tab.Home) {
                    if (e) ExpressiveLicences(OpenSourceNotices, d.licencesActions) else ComfortLicences(OpenSourceNotices, d.licencesActions)
                }
                "licence_text" -> shell(Tab.Home) {
                    val state = d.licence(LocalContext.current)
                    if (e) ExpressiveLicence(state) {} else ComfortLicence(state) {}
                }

                "recap" -> shell(Tab.Home) {
                    if (e) ExpressiveRecap(s.wrappedPage, s.wrappedActions) else ComfortRecap(s.wrappedPage, s.wrappedActions)
                }
                "recap_full" -> Ref(of(style, "recap")!!.content, height = FULL_HEIGHT)

                "permission" -> Ref({
                    val state = RootState.NeedsAccess(askedBefore = false)
                    if (e) ExpressivePermission(state, RootActions({}, {})) else ComfortPermission(state, RootActions({}, {}))
                })
                "permission_refused" -> Ref({
                    val state = RootState.NeedsAccess(askedBefore = true)
                    if (e) ExpressivePermission(state, RootActions({}, {})) else ComfortPermission(state, RootActions({}, {}))
                })
                "loading" -> Ref({ if (e) ExpressiveLoading() else ComfortLoading() })
                "empty_library" -> Ref({
                    InShell(style, Tab.Home, mini = null) {
                        if (e) ExpressiveHome(d.homeEmpty, s.homeActions) else ComfortHome(d.homeEmpty, s.homeActions)
                    }
                })
                "mini_player_paused" -> Ref({
                    InShell(style, Tab.Home, mini = d.miniPaused) {
                        if (e) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions)
                    }
                })
                "mini_player_open_with" -> Ref({
                    InShell(style, Tab.Home, mini = d.miniOpenWith) {
                        if (e) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions)
                    }
                })
                "onboarding_welcome" -> onboarding(OnboardingStep.Welcome)
                "onboarding_look" -> onboarding(OnboardingStep.Look)
                "onboarding_access" -> onboarding(OnboardingStep.Access)
                "onboarding_access_granted" -> onboarding(OnboardingStep.Access, AccessState.Granted, AccessState.Granted)
                "onboarding_access_refused" -> onboarding(OnboardingStep.Access, AccessState.Refused)
                "onboarding_library_scanning" -> onboarding(OnboardingStep.Library, AccessState.Granted, AccessState.Granted, LibraryScan.Scanning)
                "onboarding_library_found" ->
                    onboarding(OnboardingStep.Library, AccessState.Granted, AccessState.Granted, LibraryScan.Found(DemoLibrary.library.tracks.size))
                "onboarding_library_empty" -> onboarding(OnboardingStep.Library, AccessState.Granted, AccessState.Granted, LibraryScan.Found(0))
                else -> null
            }
        }

    }
}

private val Scrollable = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

/** The index of the largest vertically scrollable node: the page, not a shelf or a list inside it. */
private fun ComposeContentTestRule.mainScrollIndex(): Int? {
    val nodes = onAllNodes(Scrollable).fetchSemanticsNodes()
    val target = nodes.maxByOrNull { it.boundsInRoot.width * it.boundsInRoot.height } ?: return null
    return nodes.indexOf(target)
}

/** Scrolls the page by [dy] pixels, as a finger would. */
fun ComposeContentTestRule.scrollMain(dy: Float) {
    val index = mainScrollIndex() ?: return
    onAllNodes(Scrollable)[index].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
}

/**
 * Puts a finger on equaliser band [index], drags it past the touch slop and back, and keeps it down, so
 * the band is held at the value it had. The band is found by its TalkBack name, since Now Playing's
 * seek bar under the sheet is a slider too.
 */
fun ComposeContentTestRule.holdBand(index: Int) {
    val name = EqualiserBands.describe(RuntimeEnvironment.getApplication(), index)
    onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and hasContentDescription(name)).performTouchInput {
        val past = 30f
        down(center)
        moveBy(Offset(0f, -(viewConfiguration.touchSlop + past)))
        moveBy(Offset(0f, past))
    }
}

/** Where the page is scrolled to and how far it can go, in pixels for a scrolling column. */
fun ComposeContentTestRule.mainScrollRange(): ScrollAxisRange? {
    val index = mainScrollIndex() ?: return null
    return onAllNodes(Scrollable).fetchSemanticsNodes()[index].config[SemanticsProperties.VerticalScrollAxisRange]
}


/**
 * The style's shell around [content], as StoreShots draws it, with the overlays the shell can raise:
 * Now Playing, then the queue or lyrics over it, and a message ([message], a string resource).
 */
@Composable
private fun InShell(
    style: UiStyle,
    tab: Tab,
    mini: MiniPlayerState? = DemoStates.mini,
    nowPlaying: NowPlayingState? = null,
    queue: QueueState? = null,
    lyrics: LyricsState? = null,
    message: Int? = null,
    content: @Composable () -> Unit,
) {
    val s = DemoStates
    val e = style == UiStyle.Expressive
    val snackbar = remember { SnackbarHostState() }
    if (message != null) {
        val text = stringResource(message)
        LaunchedEffect(text) { snackbar.showSnackbar(text) }
    }
    val covered = nowPlaying != null
    val base = s.shell(tab)
    val state = ShellState(
        player = base.player,
        currentTab = tab,
        hasCurrentSong = mini != null,
        nowPlayingVisible = nowPlaying != null,
        queueVisible = queue != null,
        lyricsVisible = lyrics != null,
        covered = covered,
        snackbar = snackbar,
        menu = null,
        picking = null,
    )
    val slots = ShellSlots(
        content = { content() },
        miniPlayer = { if (mini != null) { if (e) ExpressiveMiniPlayer(mini, s.miniActions) else ComfortMiniPlayer(mini, s.miniActions) } },
        nowPlaying = { if (nowPlaying != null) { if (e) ExpressiveNowPlaying(nowPlaying, s.nowPlayingActions) else ComfortNowPlaying(nowPlaying, s.nowPlayingActions) } },
        queue = { if (queue != null) { if (e) ExpressiveQueue(queue, s.queueActions) else ComfortQueue(queue, s.queueActions) } },
        lyrics = {
            if (lyrics != null) {
                val screen = DesignStates.lyricsScreen(lyrics)
                if (e) ExpressiveLyrics(screen, s.lyricsScreenActions) else ComfortLyrics(screen, s.lyricsScreenActions)
            }
        },
    )
    if (e) ExpressiveShell(state, s.shellActions, slots) else ComfortShell(state, s.shellActions, slots)
}

/** The equaliser sheet over Now Playing, in the scheme the track menu takes there. */
@Composable
private fun EqSheet(style: UiStyle, state: EqualiserSheetState) {
    if (style == UiStyle.Expressive) {
        FromCover(DemoLibrary.nowPlaying.mediaId) { ExpressiveEqualiserSheet(state, EqualiserDemo.sheetActions) }
    } else {
        TintedCover(DemoLibrary.nowPlaying.mediaId, wash = true) { ComfortEqualiserSheet(state, EqualiserDemo.sheetActions) }
    }
}

/**
 * The track menu as ui/Sheets.kt TrackMenuSheet routes it: a picker chosen from the menu replaces it
 * (Comfort opens the language and sleep-timer choices as pop-overs on the menu instead), and over the
 * player Expressive takes the cover's scheme and Comfort the playing song's wash.
 */
@Composable
private fun TrackMenu(style: UiStyle, state: TrackMenuState, overPlayer: Boolean) {
    val d = DesignStates
    val e = style == UiStyle.Expressive
    val inScheme: @Composable (@Composable () -> Unit) -> Unit = { body ->
        when {
            e && overPlayer -> FromCover(state.track.mediaId, content = body)
            !e && overPlayer -> TintedCover(DemoLibrary.nowPlaying.mediaId, wash = true, content = body)
            else -> body()
        }
    }
    when {
        state.pickingLanguage && e -> inScheme {
            val (picker, actions) = languagePickerModel(state.language, {}, songTitle = state.track.title)
            ExpressiveLanguagePicker(picker, actions)
        }
        state.pickingSleepTimer && e -> inScheme { ExpressiveSleepTimerPicker(state, d.trackMenuActions) }
        else -> inScheme { if (e) ExpressiveTrackMenu(state, d.trackMenuActions) else ComfortTrackMenu(state, d.trackMenuActions) }
    }
}

@androidx.compose.runtime.Composable
private fun MoodHome(expressive: Boolean, ask: Boolean) {
    val state = MoodStates.home(ask)
    if (expressive) ExpressiveHome(state, DemoStates.homeActions) else ComfortHome(state, DemoStates.homeActions)
}

@androidx.compose.runtime.Composable
private fun MoodPicker(expressive: Boolean, state: app.attic.player.ui.model.InterestsState) {
    if (expressive) ExpressiveInterests(state, MoodStates.interestsActions) else ComfortInterests(state, MoodStates.interestsActions)
}
