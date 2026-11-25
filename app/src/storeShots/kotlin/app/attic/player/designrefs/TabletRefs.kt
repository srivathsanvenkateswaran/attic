package app.attic.player.designrefs

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.attic.player.data.UiStyle
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
 * The tablet windows the design files' Tablet pages draw. All three are the Pad 6's 2880 by 1800 pixels:
 * at its stock 400 dpi it is 1152 by 720 dp, and at a smaller display size (320 dpi) 1440 by 900 dp.
 */
enum class TabletSize(val key: String, val qualifiers: String, val widthPx: Int, val heightPx: Int) {
    Landscape("landscape", "w1152dp-h720dp-land-400dpi", 2880, 1800),

    /** The Pad upright, a medium width. */
    Portrait("portrait", "w720dp-h1152dp-port-400dpi", 1800, 2880),
    Large("large", "w1440dp-h900dp-land-xhdpi", 2880, 1800),
}

/**
 * The tablet renders: the screens docs/design/2026-11-25-tablet/SPEC.md lists, in both styles and
 * themes, at each [TabletSize], drawn from [Ref] as the phone references are. Written to
 * `<ShotsDir>/tablet-<size>-<style>-<theme>-<key>.png`; run through scripts/store_shots/tablet_refs.sh.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1152dp-h720dp-land-400dpi", application = Application::class)
class TabletRefs(private val size: TabletSize, private val style: UiStyle, private val dark: Boolean, private val key: String) {
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Every test sets its own display, before the activity starts, so the order of the tests doesn't matter. */
    private val display = object : ExternalResource() {
        override fun before() = RuntimeEnvironment.setQualifiers(size.qualifiers)
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(RegisterDesignActivity).around(display).around(compose)

    @Test
    fun shot() {
        val ref = Ref.of(style, key) ?: return
        val name = "tablet-${size.key}-${style.name.lowercase()}-${if (dark) "dark" else "light"}-$key"
        compose.shootRef(
            compose.activity, name, style, dark,
            height = size.heightPx, width = size.widthPx, tablet = true,
            capture = ref.capture, ime = ref.ime, bars = ref.bars, prepare = ref.prepare,
            content = ref.content,
        )
    }

    companion object {
        // The design file's Tablet frames, by key: tablet_compare.py pairs them.
        val Landscape = listOf(
            "home", "library_albums", "search_results", "album", "artist", "playlist",
            "now_playing", "now_playing_lyrics", "lyrics_synced", "settings", "sheet_track_menu_album",
            "dialog_new_playlist_library", "onboarding_look",
            // Not drawn on the Tablet page, rendered to check they hold up.
            "library_grid", "search_browse", "queue", "recap", "recap_story_intro",
        )
        val Portrait = listOf("home", "library_list", "search_results", "album", "now_playing", "settings", "onboarding_look")
        val Large = listOf("home", "library_albums", "album", "now_playing")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun shots(): List<Array<Any>> = buildList {
            for ((size, keys) in listOf(TabletSize.Landscape to Landscape, TabletSize.Portrait to Portrait, TabletSize.Large to Large)) {
                for (style in UiStyle.entries) for (dark in listOf(true, false)) for (key in keys) add(arrayOf(size, style, dark, key))
            }
        }
    }
}
