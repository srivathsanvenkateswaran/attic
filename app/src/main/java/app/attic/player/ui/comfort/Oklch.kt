package app.attic.player.ui.comfort

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * OKLab / OKLCH for sRGB colours, the space kit.css writes its `.tinted` rules in
 * (`oklch(from var(--tint) L calc(c*k) h)`). Plain math on ARGB ints so it runs in unit tests.
 */
internal object Oklch {
    /** A colour in OKLCH: lightness 0..1, chroma (about 0..0.37 for sRGB), hue in degrees. */
    data class Lch(val l: Double, val c: Double, val h: Double)

    fun fromArgb(argb: Int): Lch {
        val r = linear(((argb shr 16) and 0xFF) / 255.0)
        val g = linear(((argb shr 8) and 0xFF) / 255.0)
        val b = linear((argb and 0xFF) / 255.0)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        val hue = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
        return Lch(lightness, hypot(a, bb), hue)
    }

    /**
     * The sRGB colour for [l], [c], [h]. Out of gamut, chroma is reduced at the same lightness and
     * hue until it fits (CSS Color 4's gamut mapping, without its just-noticeable clip), so the
     * lightness each kit token sets, and with it the contrast, never moves.
     */
    fun toArgb(l: Double, c: Double, h: Double): Int {
        var rgb = linearRgb(l, c, h)
        if (!inGamut(rgb)) {
            var lo = 0.0
            var hi = c
            repeat(24) {
                val mid = (lo + hi) / 2
                if (inGamut(linearRgb(l, mid, h))) lo = mid else hi = mid
            }
            rgb = linearRgb(l, lo, h)
        }
        val (r, g, b) = rgb.map { (gamma(it.coerceIn(0.0, 1.0)) * 255).roundToInt().coerceIn(0, 255) }
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** WCAG relative luminance of an ARGB colour. */
    fun luminance(argb: Int): Double {
        val r = linear(((argb shr 16) and 0xFF) / 255.0)
        val g = linear(((argb shr 8) and 0xFF) / 255.0)
        val b = linear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two opaque colours. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun linearRgb(l: Double, c: Double, h: Double): List<Double> {
        val rad = Math.toRadians(h)
        val a = c * cos(rad)
        val b = c * sin(rad)
        val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return listOf(
            4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1,
            -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1,
            -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1,
        )
    }

    private fun inGamut(rgb: List<Double>) = rgb.all { it in -1e-7..(1 + 1e-7) }

    private fun linear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun gamma(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
}
