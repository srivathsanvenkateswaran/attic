package app.attic.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android Auto's brand colour (VD-3) comes from a style the manifest names for the car alone, so the
 * app's own themes, and with them both in-app styles, stay as they were.
 */
class CarThemeTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()
    private val themes = File("src/main/res/values/themes.xml").readText()

    @Test
    fun `the car reads the terracotta accent from its own style`() {
        assertTrue(
            Regex("""android:name="com.google.android.gms.car.application.theme"\s+android:resource="@style/Theme.Attic.Car"""").containsMatchIn(manifest),
        )
        val car = Regex("""<style name="Theme.Attic.Car"[^>]*>(.*?)</style>""", RegexOption.DOT_MATCHES_ALL).find(themes)!!.groupValues[1]
        assertTrue(car.contains("""<item name="android:colorAccent">@color/headroom_ground</item>"""))
        assertTrue(car.contains("""<item name="android:colorPrimary">@color/headroom_ground</item>"""))
        val colors = File("src/main/res/values/colors.xml").readText()
        assertTrue(colors.contains("""<color name="headroom_ground">#C6533A</color>"""))
    }

    @Test
    fun `no activity or the application uses the car style`() {
        assertEquals(1, Regex("Theme.Attic.Car").findAll(manifest).count())
        val appThemes = Regex("""android:theme="@style/([A-Za-z.]+)"""").findAll(manifest).map { it.groupValues[1] }.toList()
        assertFalse(appThemes.any { it.startsWith("Theme.Attic.Car") })
        // The in-app themes set no accent or primary of their own.
        val others = themes.replace(Regex("""<style name="Theme.Attic.Car".*?</style>""", RegexOption.DOT_MATCHES_ALL), "")
        assertFalse(others.contains("colorAccent"))
        assertFalse(others.contains("colorPrimary"))
    }
}
