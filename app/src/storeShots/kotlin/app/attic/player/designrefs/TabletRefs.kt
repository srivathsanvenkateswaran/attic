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

    /** Half the Pad sideways in split screen: a compact width, so the phone layouts. */
    Split("split", "w570dp-h720dp-port-400dpi", 1425, 1800),

    /** A floating window: a medium width held sideways. */
    Floating("floating", "w800dp-h600dp-land-400dpi", 2000, 1500),

    /** A bigger tablet upright: an expanded width taller than it is wide. */
    LargePortrait("large-portrait", "w900dp-h1440dp-port-xhdpi", 1800, 2880),

    /** The store listing's 10-inch tablet, sideways: 2560 by 1600 pixels at 320 dpi. */
    TenInch("ten-inch", "w1280dp-h800dp-land-xhdpi", 2560, 1600),

    /**
     * The store listing's smaller tablet, sideways: 2048 by 1280 pixels at 320 dpi. At 960 by 600 dp, the
     * usual 7-inch window, Comfort's Now Playing shortens the title and both players squeeze their buttons.
     */
    SevenInch("seven-inch", "w1024dp-h640dp-land-xhdpi", 2048, 1280),
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
        /** Mood mixes and the equaliser, merged after the Tablet page was drawn. */
        val Moods = listOf("moods_home", "mood_page", "moods_browse", "moods_picker", "moods_settings")
        val Equaliser = listOf("sheet_equaliser", "settings_equaliser", "now_playing_equaliser", "dialog_equaliser_save_preset", "menu_transitions")

        // The design file's Tablet frames, by key: tablet_compare.py pairs them.
        val Landscape = listOf(
            "home", "library_albums", "search_results", "album", "artist", "playlist",
            "now_playing", "now_playing_lyrics", "lyrics_synced", "settings", "sheet_track_menu_album",
            "dialog_new_playlist_library", "onboarding_look",
            // Not drawn on the Tablet page, rendered to check they hold up.
            "library_grid", "search_browse", "queue", "recap", "recap_story_intro",
        ) + Moods + Equaliser
        val Portrait = listOf("home", "library_list", "search_results", "album", "now_playing", "settings", "onboarding_look") + Moods + Equaliser
        val Large = listOf("home", "library_albums", "album", "now_playing")

        /** Every size class and orientation beyond the Pad's own: the main screens, moods and the equaliser. */
        val OtherSizes = listOf(
            "home", "library_albums", "search_results", "album", "now_playing", "settings",
            "moods_home", "mood_page", "sheet_equaliser", "settings_equaliser",
        )

        /** The screens the store listing's tablet screenshots and the website's tablet section show. */
        val Store = listOf("home", "library_albums", "album", "search_results", "now_playing", "now_playing_lyrics")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun shots(): List<Array<Any>> = buildList {
            for ((size, keys) in listOf(
                TabletSize.Landscape to Landscape, TabletSize.Portrait to Portrait, TabletSize.Large to Large,
                TabletSize.Split to OtherSizes, TabletSize.Floating to OtherSizes, TabletSize.LargePortrait to OtherSizes,
                TabletSize.TenInch to Store, TabletSize.SevenInch to Store,
            )) {
                for (style in UiStyle.entries) for (dark in listOf(true, false)) for (key in keys) add(arrayOf(size, style, dark, key))
            }
        }
    }
}
