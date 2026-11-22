package app.attic.player.ui.comfort

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComfortTintTest {

    /** The sixteen fixture tints from shared/covers.css. */
    private val fixtureTints = listOf(
        0xFFA04E16, 0xFF5D6470, 0xFFC9552E, 0xFFC8553D, 0xFFA62A2A, 0xFF284A70, 0xFF7C2F2F, 0xFFC98A1F,
        0xFF7FA32B, 0xFFA02C7A, 0xFF2C5F93, 0xFF1F8AA8, 0xFF7A1616, 0xFF5D43A6, 0xFF6A7D2E, 0xFF3F8A4F,
    ).map { it.toInt() }

    /** Palette swatches can be anything: fixtures, a hue sweep at several strengths, loud primaries, greys and random colours. */
    private val seeds: List<Int> = buildList {
        addAll(fixtureTints)
        for (hue in 0 until 360 step 10) {
            for (saturation in listOf(0.2f, 0.5f, 0.8f, 1f)) {
                for (lightness in listOf(0.15f, 0.35f, 0.5f, 0.7f, 0.9f)) add(hsl(hue.toFloat(), saturation, lightness))
            }
        }
        addAll(listOf(0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF).map { it.toInt() })
        val random = Random(7)
        repeat(400) { add(0xFF000000.toInt() or random.nextInt(0x1000000)) }
    }

    private val themes = listOf(ComfortDark, ComfortLight)

    @Test
    fun theUntintedPaletteKeepsTextAtAa() {
        for (palette in themes) {
            for ((content, background) in ComfortTint.textPairs) {
                val ratio = Oklch.contrast(content(palette).toArgb(), background(palette).toArgb())
                assertTrue("untinted ${name(palette)}: %.2f:1".format(ratio), ratio >= 4.5)
            }
        }
    }

    @Test
    fun textRolesHoldAaForEveryCoverInBothThemes() {
        for (palette in themes) {
            for (wash in listOf(false, true)) {
                for (seed in seeds) {
                    val tinted = ComfortTint.apply(palette, seed, wash)
                    ComfortTint.textPairs.forEachIndexed { index, (content, background) ->
                        val ratio = Oklch.contrast(content(tinted).toArgb(), background(tinted).toArgb())
                        assertTrue(
                            "pair %d is %.2f:1 for seed #%08X, %s%s".format(index, ratio, seed, name(palette), if (wash) " wash" else ""),
                            ratio >= 4.5,
                        )
                    }
                }
            }
        }
    }

    /**
     * `--on-3` (hints, placeholders, trailing icons) is below 4.5:1 in the kit's own palette (4.3:1 on
     * black, 2.8:1 on white). Tinting must not make it worse than that.
     */
    @Test
    fun quietInkIsNeverWeakerThanTheKitsOwn() {
        for (palette in themes) {
            val surfaces = listOf<(ComfortColors) -> Color>({ it.ground }, { it.card }, { it.raised })
            for (seed in seeds) {
                val tinted = ComfortTint.apply(palette, seed, wash = true)
                for (surface in surfaces) {
                    val kit = Oklch.contrast(palette.on3.toArgb(), surface(palette).toArgb())
                    val ratio = Oklch.contrast(tinted.on3.toArgb(), surface(tinted).toArgb())
                    assertTrue("on-3 %.2f vs kit %.2f for #%08X %s".format(ratio, kit, seed, name(palette)), ratio >= kit - 0.05)
                }
            }
        }
    }

    @Test
    fun tokensKeepTheCoverHueAndTheKitsLightness() {
        for (seed in fixtureTints) {
            val tint = Oklch.fromArgb(seed)
            val dark = ComfortTint.apply(ComfortDark, seed, wash = true)
            val accent = Oklch.fromArgb(dark.accent.toArgb())
            assertEquals("accent lightness", 0.76, accent.l, 0.006)
            assertTrue("accent hue ${accent.h} vs ${tint.h}", hueDistance(accent.h, tint.h) < 5.0)
            assertEquals("ground lightness", 0.115, Oklch.fromArgb(dark.ground.toArgb()).l, 0.006)
            val light = ComfortTint.apply(ComfortLight, seed, wash = false)
            assertEquals("light accent lightness", 0.47, Oklch.fromArgb(light.accent.toArgb()).l, 0.006)
            assertEquals("light on-accent", Color.White, light.onAccent)
        }
    }

    @Test
    fun tintedLeavesSurfacesAloneAndWashMovesThem() {
        val seed = fixtureTints.first()
        for (palette in themes) {
            val tinted = ComfortTint.apply(palette, seed, wash = false)
            assertEquals(palette.ground, tinted.ground)
            assertEquals(palette.card, tinted.card)
            assertEquals(palette.on2, tinted.on2)
            assertTrue(palette.accent != tinted.accent)
            val washed = ComfortTint.apply(palette, seed, wash = true)
            assertTrue(palette.ground != washed.ground)
            assertEquals("--on never changes", palette.on, washed.on)
        }
    }

    @Test
    fun aGreyCoverGivesAGreyPlayer() {
        val accent = Oklch.fromArgb(ComfortTint.apply(ComfortDark, 0xFF808080.toInt(), wash = true).accent.toArgb())
        assertTrue("chroma ${accent.c}", accent.c < 0.005)
    }

    @Test
    fun oklchRoundTrips() {
        for (seed in seeds.take(200)) {
            val lch = Oklch.fromArgb(seed)
            val back = Oklch.toArgb(lch.l, lch.c, lch.h)
            for (shift in listOf(16, 8, 0)) {
                val a = (seed shr shift) and 0xFF
                val b = (back shr shift) and 0xFF
                assertTrue("#%08X -> #%08X".format(seed, back), abs(a - b) <= 1)
            }
        }
    }

    private fun name(palette: ComfortColors) = if (palette.dark) "dark" else "light"

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(((a - b) % 360 + 360) % 360)
        return minOf(d, 360 - d)
    }

    private fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when ((h / 60f).toInt() % 6) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun channel(v: Float) = ((v + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
