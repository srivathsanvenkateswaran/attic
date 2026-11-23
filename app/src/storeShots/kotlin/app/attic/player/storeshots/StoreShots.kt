package app.attic.player.storeshots

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
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.robolectric.Shadows.shadowOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
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
        compose.shoot(compose.activity, name, style, dark) { Screen(style, screen) }
    }

    companion object {
        val Screens = listOf(
            "home", "album", "artist", "now-playing", "lyrics", "queue", "library", "search", "search-browse", "settings", "recap",
        )

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
        "home" -> InShell(style, Tab.Home) { if (expressive) ExpressiveHome(s.home, s.homeActions) else ComfortHome(s.home, s.homeActions) }
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
        "settings" -> {
            val state = s.settings(style, LocalIsDark.current)
            InShell(style, Tab.Home) { if (expressive) ExpressiveSettings(state, s.settingsActions) else ComfortSettings(state, s.settingsActions) }
        }
        "recap" ->
            InShell(style, Tab.Home) { if (expressive) ExpressiveRecap(s.wrappedPage, s.wrappedActions) else ComfortRecap(s.wrappedPage, s.wrappedActions) }
        else -> error("Unknown screen $screen")
    }
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
