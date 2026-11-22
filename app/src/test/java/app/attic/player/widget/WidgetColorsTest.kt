package app.attic.player.widget

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class WidgetColorsTest {
    /** Covers from dull to loud, dark to pale, across the hue circle. */
    private val tints = listOf(
        0xFFC6533A, 0xFF3F6FD8, 0xFF1E5E2A, 0xFFF2D21B, 0xFF7A2E8F, 0xFF101010, 0xFFF5F5F5, 0xFF8A8A8A, 0xFF00B3B3,
    ).map { it.toInt() }

    @Test
    fun expressiveTextAndControlsStayReadable() {
        for (tint in tints) for (dark in listOf(false, true)) check(WidgetColors.expressive(tint, dark), "expressive $tint dark=$dark")
    }

    @Test
    fun comfortTextAndControlsStayReadable() {
        for (swatch in tints + listOf<Int?>(null)) for (dark in listOf(false, true)) check(WidgetColors.comfort(swatch, dark), "comfort $swatch dark=$dark")
    }

    @Test
    fun lightAndDarkDiffer() {
        assertTrue(WidgetColors.expressive(tints[0], false).container != WidgetColors.expressive(tints[0], true).container)
        assertTrue(WidgetColors.comfort(tints[0], false).container != WidgetColors.comfort(tints[0], true).container)
    }

    private fun check(p: WidgetPalette, what: String) {
        assertTrue("$what title", contrast(p.title, p.container) >= 4.5)
        assertTrue("$what artist", contrast(p.artist, p.container) >= 4.5)
        assertTrue("$what play glyph", contrast(p.onPlay, p.play) >= 3.0)
        assertTrue("$what skip glyph", contrast(p.onSkip, p.skip ?: p.container) >= 3.0)
        assertTrue("$what empty art mark", contrast(p.onEmpty, p.empty) >= 3.0)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
