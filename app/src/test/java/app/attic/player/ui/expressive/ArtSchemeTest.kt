package app.attic.player.ui.expressive

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtSchemeTest {

    /** Seeds around the hue circle at the strengths coverTint produces, plus greys, white, black and loud primaries. */
    private val seeds: List<Int> = buildList {
        for (hue in 0 until 360 step 10) {
            for (saturation in listOf(0.15f, 0.4f, 0.62f)) {
                for (lightness in listOf(0.2f, 0.3f)) add(hsl(hue.toFloat(), saturation, lightness))
            }
        }
        addAll(listOf(0xFF333333.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFF00.toInt()))
    }

    @Test
    fun textRolesReachAaContrastOnTheirContainers() {
        for (dark in listOf(true, false)) {
            for (seed in seeds) {
                val roles = ArtScheme.roles(seed, dark)
                for ((content, container) in ArtScheme.textPairs) {
                    val ratio = contrastRatio(roles.getValue(content), roles.getValue(container))
                    assertTrue(
                        "%s on %s is %.2f:1 for seed #%08X, %s".format(content, container, ratio, seed, if (dark) "dark" else "light"),
                        ratio >= 4.5,
                    )
                }
            }
        }
    }

    @Test
    fun everyRoleLandsOnItsTone() {
        for (dark in listOf(true, false)) {
            val table = if (dark) ArtScheme.dark else ArtScheme.light
            for (seed in seeds) {
                val roles = ArtScheme.roles(seed, dark)
                for ((name, spec) in table) {
                    val tone = Hct.fromArgb(roles.getValue(name)).tone
                    assertEquals("$name tone for #%08X".format(seed), spec.second.tone, tone, 0.6)
                }
            }
        }
    }

    @Test
    fun primaryFollowsTheCoverHueAndTertiaryTurnsSixtyDegrees() {
        val seed = hsl(30f, 0.62f, 0.25f)
        val seedHue = Hct.fromArgb(seed).hue
        val roles = ArtScheme.roles(seed, dark = true)
        val primaryHue = Hct.fromArgb(roles.getValue("primaryContainer")).hue
        val tertiaryHue = Hct.fromArgb(roles.getValue("onTertiaryContainer")).hue
        assertTrue("primary hue $primaryHue vs seed $seedHue", hueDistance(primaryHue, seedHue) < 6.0)
        assertTrue("tertiary hue $tertiaryHue vs seed $seedHue", hueDistance(tertiaryHue, seedHue + 60.0) < 8.0)
    }

    @Test
    fun hctRoundTripsSrgbColours() {
        for (argb in listOf(0xFF3F6FD8.toInt(), 0xFFB8552E.toInt(), 0xFF3F8F6B.toInt(), 0xFF8A3A0E.toInt(), 0xFF777777.toInt())) {
            val hct = Hct.fromArgb(argb)
            val back = hct.toArgb()
            for (shift in listOf(16, 8, 0)) {
                val a = argb shr shift and 0xFF
                val b = back shr shift and 0xFF
                assertTrue("#%08X came back as #%08X".format(argb, back), abs(a - b) <= 2)
            }
        }
    }

    @Test
    fun hctMatchesMaterialColorUtilitiesForThePrimaries() {
        // The CAM16 values material-color-utilities' own tests expect for pure red, green and blue.
        val expected = mapOf(
            0xFFFF0000.toInt() to Triple(27.408, 113.357, 53.233),
            0xFF00FF00.toInt() to Triple(142.139, 108.410, 87.737),
            0xFF0000FF.toInt() to Triple(282.788, 87.230, 32.302),
        )
        for ((argb, value) in expected) {
            val hct = Hct.fromArgb(argb)
            assertEquals(value.first, hct.hue, 0.01)
            assertEquals(value.second, hct.chroma, 0.01)
            assertEquals(value.third, hct.tone, 0.01)
        }
    }

    @Test
    fun theSchemeBuildsForBothThemes() {
        val dark = ArtScheme.colorScheme(hsl(200f, 0.5f, 0.25f), dark = true)
        val light = ArtScheme.colorScheme(hsl(200f, 0.5f, 0.25f), dark = false)
        assertTrue(dark.surface != light.surface)
        assertEquals(dark.surface, dark.background)
    }

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(((a - b) % 360.0 + 360.0) % 360.0)
        return minOf(d, 360.0 - d)
    }

    private fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
