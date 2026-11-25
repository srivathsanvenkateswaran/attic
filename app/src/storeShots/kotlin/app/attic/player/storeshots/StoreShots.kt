package app.attic.player.storeshots

import app.attic.player.ui.comfort.ComfortEqualiserPage
import app.attic.player.ui.comfort.ComfortEqualiserSheet
import app.attic.player.ui.comfort.TintedCover
import app.attic.player.ui.expressive.ExpressiveEqualiserPage
import app.attic.player.ui.expressive.ExpressiveEqualiserSheet
import app.attic.player.ui.expressive.FromCover
import app.attic.player.designrefs.MoodStates
import app.attic.player.playback.TransitionMode
import app.attic.player.ui.comfort.ComfortInterests
import app.attic.player.ui.comfort.ComfortMood
import app.attic.player.ui.expressive.ExpressiveInterests
import app.attic.player.ui.expressive.ExpressiveMood
import android.app.Application
import androidx.compose.runtime.Composable
import app.attic.player.data.UiStyle
import app.attic.player.ui.ShellSlots
import app.attic.player.ui.Tab
import app.attic.player.ui.comfort.ComfortAlbum
import app.attic.player.ui.comfort.ComfortArtist
import app.attic.player.ui.comfort.ComfortHome
import app.attic.player.ui.comfort.ComfortLibrary
import app.attic.player.ui.comfort.ComfortLyrics
import app.attic.player.ui.comfort.ComfortMiniPlayer
import app.attic.player.ui.comfort.ComfortNowPlaying
import app.attic.player.ui.comfort.ComfortQueue
import app.attic.player.ui.comfort.ComfortRecap
import app.attic.player.ui.comfort.ComfortSearch
import app.attic.player.ui.comfort.ComfortSettings
import app.attic.player.ui.comfort.ComfortShell
import app.attic.player.ui.expressive.ExpressiveAlbum
import app.attic.player.ui.expressive.ExpressiveArtist
import app.attic.player.ui.expressive.ExpressiveHome
import app.attic.player.ui.expressive.ExpressiveLibrary
import app.attic.player.ui.expressive.ExpressiveLyrics
import app.attic.player.ui.expressive.ExpressiveMiniPlayer
import app.attic.player.ui.expressive.ExpressiveNowPlaying
import app.attic.player.ui.expressive.ExpressiveQueue
import app.attic.player.ui.expressive.ExpressiveRecap
import app.attic.player.ui.expressive.ExpressiveSearch
import app.attic.player.ui.expressive.ExpressiveSettings
import app.attic.player.ui.expressive.ExpressiveShell
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.attic.player.ui.LocalIsDark
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import app.attic.player.R
import app.attic.player.designrefs.scrollMain
import androidx.compose.ui.unit.dp
import app.attic.player.ui.expressive.AtticMarkIcon
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.robolectric.Shadows.shadowOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The store and README screenshots: every main screen in both styles, dark and light, drawn from
 * [DemoStates], one fresh activity each. Run through scripts/store_shots/run.sh, which also frames
 * them; the raw captures land in /tmp/attic-dev/store-shots/raw as `<style>-<theme>-<screen>.png`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class StoreShots(private val style: UiStyle, private val dark: Boolean, private val screen: String) {
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Unit tests have no manifest entry for the activity the rule launches, so it is added first. */
    @get:Rule val rules: RuleChain = RuleChain.outerRule(RegisterActivity).around(compose)

    @Test
    fun shot() {
        val name = "${style.name.lowercase()}-${if (dark) "dark" else "light"}-$screen"
        // Home from "Mixes" or "Moods" down, and Settings from "Playback" down, each heading just below
        // the bar that stays pinned at the top (Comfort's Home keeps its tabs there too).
        val expressive = style == UiStyle.Expressive
        val heading = when (screen) {
            "home-mixes", "home-mixes-moods" -> R.string.home_mixes to if (expressive) 150f else 175f
            "home-moods" -> R.string.home_moods to if (expressive) 150f else 175f
            "settings-playback" -> R.string.settings_playback to 125f
            else -> null
        }
        val prepare: ComposeContentTestRule.() -> Unit = {
            if (heading != null) bringToTop(context.getString(heading.first), heading.second)
        }
        compose.shoot(compose.activity, name, style, dark, prepare = prepare, allWindows = screen in SheetScreens) { Screen(style, screen) }
    }

    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    companion object {
        val Screens = listOf(
            "home", "home-mixes", "album", "artist", "now-playing", "lyrics", "queue", "library", "search", "search-browse", "settings",
            "settings-playback", "recap", "equaliser", "equaliser-settings", "home-moods", "home-mixes-moods", "mood", "interests",
        )

        /** Screens with a sheet over them, captured with every window. */
        val SheetScreens = setOf("equaliser")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun shots(): List<Array<Any>> = buildList {
            for (style in UiStyle.entries) for (dark in listOf(true, false)) for (screen in Screens) add(arrayOf(style, dark, screen))
        }
    }
}

@Composable
private fun Screen(style: UiStyle, screen: String) {
    val expressive = style == UiStyle.Expressive
    val s = DemoStates
    when (screen) {
        "home", "home-mixes" -> InShell(style, Tab.Home) { if (expressive) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions) }
        "album" -> {
            val state = s.album("Paper Lanterns")
            InShell(style, Tab.Home) { if (expressive) ExpressiveAlbum(state, s.albumActions) else ComfortAlbum(state, s.albumActions) }
        }
        "artist" -> {
            val state = s.artist("Kavya Raman")
            InShell(style, Tab.Home) { if (expressive) ExpressiveArtist(state, s.artistActions) else ComfortArtist(state, s.artistActions) }
        }
        "now-playing" ->
            if (expressive) ExpressiveNowPlaying(s.nowPlaying, s.nowPlayingActions) else ComfortNowPlaying(s.nowPlaying, s.nowPlayingActions)
        "lyrics" ->
            if (expressive) ExpressiveLyrics(s.lyricsScreen, s.lyricsScreenActions) else ComfortLyrics(s.lyricsScreen, s.lyricsScreenActions)
        "queue" -> if (expressive) ExpressiveQueue(s.queueState, s.queueActions) else ComfortQueue(s.queueState, s.queueActions)
        "library" ->
            InShell(style, Tab.Library) { if (expressive) ExpressiveLibrary(s.library, s.libraryActions) else ComfortLibrary(s.library, s.libraryActions) }
        "search" -> {
            val state = s.search("ka")
            InShell(style, Tab.Search) { if (expressive) ExpressiveSearch(state, s.searchActions) else ComfortSearch(state, s.searchActions) }
        }
        "search-browse" ->
            InShell(style, Tab.Search) { if (expressive) ExpressiveSearch(s.searchBrowse, s.searchActions) else ComfortSearch(s.searchBrowse, s.searchActions) }
        "settings", "settings-playback" -> {
            // Playback shows Smart, the 1.3 transition; the full Settings shot keeps the default demo.
            val transitions = if (screen == "settings-playback") TransitionMode.Smart else TransitionMode.Crossfade
            val state = s.settings(style, LocalIsDark.current, transitions)
            InShell(style, Tab.Home) { if (expressive) ExpressiveSettings(state, s.settingsActions) else ComfortSettings(state, s.settingsActions) }
        }
        "recap" ->
            InShell(style, Tab.Home) { if (expressive) ExpressiveRecap(s.wrappedPage, s.wrappedActions) else ComfortRecap(s.wrappedPage, s.wrappedActions) }
        // A song saved with its own curve, the sheet over Now Playing in the scheme the track menu takes there.
        "equaliser" -> {
            if (expressive) ExpressiveNowPlaying(s.nowPlaying, s.nowPlayingActions) else ComfortNowPlaying(s.nowPlaying, s.nowPlayingActions)
            if (expressive) {
                FromCover(DemoLibrary.nowPlaying.mediaId) { ExpressiveEqualiserSheet(EqualiserDemo.sheetSong, EqualiserDemo.sheetActions) }
            } else {
                TintedCover(DemoLibrary.nowPlaying.mediaId, wash = true) { ComfortEqualiserSheet(EqualiserDemo.sheetSong, EqualiserDemo.sheetActions) }
            }
        }
        "equaliser-settings" -> InShell(style, Tab.Home) {
            val page = EqualiserDemo.page()
            if (expressive) ExpressiveEqualiserPage(page, EqualiserDemo.pageActions) else ComfortEqualiserPage(page, EqualiserDemo.pageActions)
        }
        // Mood mixes: Home with the Moods shelf (from Mixes down in "home-mixes-moods", so both rows
        // show), Chill's page, and the picks page with picks saved.
        "home-moods", "home-mixes-moods" -> {
            val state = MoodStates.home(ask = false)
            InShell(style, Tab.Home) { if (expressive) ExpressiveHome(state, s.homeActions) else ComfortHome(state, s.homeActions) }
        }
        "mood" -> {
            val state = MoodStates.page(playing = false)
            InShell(style, Tab.Home) { if (expressive) ExpressiveMood(state, MoodStates.pageActions) else ComfortMood(state, MoodStates.pageActions) }
        }
        "interests" -> {
            val state = MoodStates.interests(picked = true)
            if (expressive) ExpressiveInterests(state, MoodStates.interestsActions) else ComfortInterests(state, MoodStates.interestsActions)
        }
        else -> error("Unknown screen $screen")
    }
}

/** Scrolls the page until the first node showing exactly [text] has its top [topDp] below the screen's top. */
private fun ComposeContentTestRule.bringToTop(text: String, topDp: Float) {
    val top = topDp * ApplicationProvider.getApplicationContext<Application>().resources.displayMetrics.density
    repeat(20) {
        val found = onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes()
        if (found.isNotEmpty()) {
            scrollMain(found.first().boundsInRoot.top - top)
            return
        }
        scrollMain(1200f)
        mainClock.advanceTimeBy(100)
    }
    error("No \"$text\" on the page")
}

private object RegisterActivity : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val info = ActivityInfo().apply {
            name = ComponentActivity::class.java.name
            packageName = context.packageName
            theme = android.R.style.Theme_Material_NoActionBar
        }
        shadowOf(context.packageManager).addOrUpdateActivity(info)
    }
}

/** The style's shell around [content], with the demo song in the mini player and [tab] lit. */
@Composable
private fun InShell(style: UiStyle, tab: Tab, content: @Composable () -> Unit) {
    val s = DemoStates
    val slots = ShellSlots(
        content = { content() },
        miniPlayer = {
            if (style == UiStyle.Expressive) ExpressiveMiniPlayer(s.mini, s.miniActions) else ComfortMiniPlayer(s.mini, s.miniActions)
        },
        nowPlaying = {},
        queue = {},
        lyrics = {},
    )
    if (style == UiStyle.Expressive) ExpressiveShell(s.shell(tab), s.shellActions, slots) else ComfortShell(s.shell(tab), s.shellActions, slots)
}

/** The Headroom mark on each brand colour, for scripts/store_shots/frame.py to place beside captions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class BrandMarks {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(RegisterActivity).around(compose)

    @Test
    fun marks() {
        val marks = listOf("mark-on-cream" to (Cream to Terracotta), "mark-on-terracotta" to (Terracotta to Cream))
        var current by mutableStateOf(marks.first().second)
        compose.setContent {
            val (ground, ink) = current
            Box(Modifier.size(96.dp).background(ground).testTag("mark"), contentAlignment = Alignment.Center) {
                AtticMarkIcon(size = 80.dp, tint = ink)
            }
        }
        for ((name, colours) in marks) {
            current = colours
            compose.waitForIdle()
            ShotsDir.mkdirs()
            compose.onNodeWithTag("mark").captureRoboImage(File(ShotsDir, "$name.png").path)
        }
    }

    private companion object {
        val Cream = Color(0xFFF5EDDD)
        val Terracotta = Color(0xFFC6533A)
    }
}
