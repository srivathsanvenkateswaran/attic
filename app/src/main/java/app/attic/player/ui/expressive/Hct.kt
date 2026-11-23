package app.attic.player.ui.expressive

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hue, chroma and tone: the colour space Material's tonal palettes are built in. Hue and chroma are
 * CAM16's, under the default sRGB viewing conditions; tone is CIE L*, so two colours' contrast ratio
 * depends on their tones alone. This follows material-color-utilities (Apache 2.0), with a simpler
 * iterative solver in place of its closed-form one.
 */
data class Hct(val hue: Double, val chroma: Double, val tone: Double) {

    /** The sRGB colour closest to this one, as opaque ARGB. Chroma drops until the colour fits in sRGB; tone is kept. */
    fun toArgb(): Int = HctSolver.solve(hue, chroma, tone)

    companion object {
        fun fromArgb(argb: Int): Hct {
            val xyz = ColorMath.xyzFromArgb(argb)
            val cam = Cam16.fromXyz(xyz[0], xyz[1], xyz[2])
            return Hct(cam.hue, cam.chroma, ColorMath.lstarFromY(xyz[1]))
        }
    }
}

/** WCAG contrast ratio between two opaque colours, from 1 to 21. */
fun contrastRatio(a: Int, b: Int): Double {
    val ya = ColorMath.yFromArgb(a)
    val yb = ColorMath.yFromArgb(b)
    val lighter = max(ya, yb)
    val darker = minOf(ya, yb)
    return (lighter + 5.0) / (darker + 5.0)
}

internal object ColorMath {
    private const val E = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    val WHITE_POINT_D65 = doubleArrayOf(95.047, 100.0, 108.883)

    private val SRGB_TO_XYZ = arrayOf(
        doubleArrayOf(0.41233895, 0.35762064, 0.18051042),
        doubleArrayOf(0.2126, 0.7152, 0.0722),
        doubleArrayOf(0.01932141, 0.11916382, 0.95034478),
    )
    private val XYZ_TO_SRGB = arrayOf(
        doubleArrayOf(3.2413774792388685, -1.5376652402851851, -0.49885366846268053),
        doubleArrayOf(-0.9691452513005321, 1.8758853451067872, 0.04156585616912061),
        doubleArrayOf(0.05562093689691305, -0.20395524564742123, 1.0571799111220335),
    )

    /** 0–255 channel to linear RGB on a 0–100 scale. */
    fun linearized(channel: Int): Double {
        val n = channel / 255.0
        return if (n <= 0.040449936) n / 12.92 * 100.0 else ((n + 0.055) / 1.055).pow(2.4) * 100.0
    }

    /** Linear RGB on a 0–100 scale to a 0–255 channel. */
    fun delinearized(linear: Double): Int {
        val n = linear / 100.0
        val d = if (n <= 0.0031308) n * 12.92 else 1.055 * n.pow(1.0 / 2.4) - 0.055
        return (d * 255.0).roundToInt().coerceIn(0, 255)
    }

    fun xyzFromArgb(argb: Int): DoubleArray {
        val r = linearized(argb shr 16 and 0xFF)
        val g = linearized(argb shr 8 and 0xFF)
        val b = linearized(argb and 0xFF)
        return DoubleArray(3) { SRGB_TO_XYZ[it][0] * r + SRGB_TO_XYZ[it][1] * g + SRGB_TO_XYZ[it][2] * b }
    }

    fun linearRgbFromXyz(x: Double, y: Double, z: Double): DoubleArray =
        DoubleArray(3) { XYZ_TO_SRGB[it][0] * x + XYZ_TO_SRGB[it][1] * y + XYZ_TO_SRGB[it][2] * z }

    fun argbFromLinearRgb(rgb: DoubleArray): Int =
        (0xFF shl 24) or (delinearized(rgb[0]) shl 16) or (delinearized(rgb[1]) shl 8) or delinearized(rgb[2])

    fun yFromArgb(argb: Int): Double = xyzFromArgb(argb)[1]

    fun yFromLstar(lstar: Double): Double = 100.0 * labInvf((lstar + 16.0) / 116.0)

    fun lstarFromY(y: Double): Double = 116.0 * labF(y / 100.0) - 16.0

    fun argbFromLstar(lstar: Double): Int {
        val y = yFromLstar(lstar)
        val c = delinearized(y)
        return (0xFF shl 24) or (c shl 16) or (c shl 8) or c
    }

    private fun labF(t: Double): Double = if (t > E) cbrt(t) else (KAPPA * t + 16.0) / 116.0

    private fun labInvf(ft: Double): Double {
        val cube = ft * ft * ft
        return if (cube > E) cube else (116.0 * ft - 16.0) / KAPPA
    }
}

/** The default viewing conditions: sRGB white, 200/π lux adapting luminance, a mid-grey background, average surround. */
internal object ViewingConditions {
    val n: Double
    val aw: Double
    val nbb: Double
    val ncb: Double
    val c: Double
    val nc: Double
    val rgbD: DoubleArray
    val fl: Double
    val z: Double

    init {
        val white = ColorMath.WHITE_POINT_D65
        val adaptingLuminance = 200.0 / PI * ColorMath.yFromLstar(50.0) / 100.0
        val rW = white[0] * 0.401288 + white[1] * 0.650173 + white[2] * -0.051461
        val gW = white[0] * -0.250268 + white[1] * 1.204414 + white[2] * 0.045854
        val bW = white[0] * -0.002079 + white[1] * 0.048952 + white[2] * 0.953127
        val f = 1.0 // average surround: 0.8 + 2.0 / 10
        c = 0.69
        nc = f
        val d = (f * (1.0 - (1.0 / 3.6) * exp((-adaptingLuminance - 42.0) / 92.0))).coerceIn(0.0, 1.0)
        rgbD = doubleArrayOf(d * (100.0 / rW) + 1.0 - d, d * (100.0 / gW) + 1.0 - d, d * (100.0 / bW) + 1.0 - d)
        val k = 1.0 / (5.0 * adaptingLuminance + 1.0)
        val k4 = k * k * k * k
        val k4F = 1.0 - k4
        fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * cbrt(5.0 * adaptingLuminance)
        n = ColorMath.yFromLstar(50.0) / white[1]
        z = 1.48 + sqrt(n)
        nbb = 0.725 / n.pow(0.2)
        ncb = nbb
        val factors = doubleArrayOf(
            (fl * rgbD[0] * rW / 100.0).pow(0.42),
            (fl * rgbD[1] * gW / 100.0).pow(0.42),
            (fl * rgbD[2] * bW / 100.0).pow(0.42),
        )
        val rgbA = DoubleArray(3) { 400.0 * factors[it] / (factors[it] + 27.13) }
        aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb
    }
}

internal class Cam16(val hue: Double, val chroma: Double, val j: Double) {
    companion object {
        fun fromXyz(x: Double, y: Double, z: Double): Cam16 {
            val vc = ViewingConditions
            val rC = 0.401288 * x + 0.650173 * y - 0.051461 * z
            val gC = -0.250268 * x + 1.204414 * y + 0.045854 * z
            val bC = -0.002079 * x + 0.048952 * y + 0.953127 * z
            val rA = adapt(vc.rgbD[0] * rC)
            val gA = adapt(vc.rgbD[1] * gC)
            val bA = adapt(vc.rgbD[2] * bC)
            val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
            val b = (rA + gA - 2.0 * bA) / 9.0
            val u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0
            val p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0
            val hue = Math.toDegrees(atan2(b, a)).let { if (it < 0) it + 360.0 else if (it >= 360.0) it - 360.0 else it }
            val ac = p2 * vc.nbb
            val j = 100.0 * (ac / vc.aw).pow(vc.c * vc.z)
            val huePrime = if (hue < 20.14) hue + 360.0 else hue
            val eHue = 0.25 * (cos(Math.toRadians(huePrime) + 2.0) + 3.8)
            val p1 = 50000.0 / 13.0 * eHue * vc.nc * vc.ncb
            val t = p1 * hypot(a, b) / (u + 0.305)
            val alpha = t.pow(0.9) * (1.64 - 0.29.pow(vc.n)).pow(0.73)
            val chroma = alpha * sqrt(j / 100.0)
            return Cam16(hue, chroma, j)
        }

        private fun adapt(component: Double): Double {
            val af = (ViewingConditions.fl * abs(component) / 100.0).pow(0.42)
            return sign(component) * 400.0 * af / (af + 27.13)
        }

        /** The XYZ colour with this lightness J, chroma and hue. */
        fun xyzFromJch(j: Double, chroma: Double, hue: Double): DoubleArray {
            val vc = ViewingConditions
            val alpha = if (chroma == 0.0 || j == 0.0) 0.0 else chroma / sqrt(j / 100.0)
            val t = (alpha / (1.64 - 0.29.pow(vc.n)).pow(0.73)).pow(1.0 / 0.9)
            val hRad = Math.toRadians(hue)
            val eHue = 0.25 * (cos(hRad + 2.0) + 3.8)
            val ac = vc.aw * (j / 100.0).pow(1.0 / vc.c / vc.z)
            val p1 = eHue * (50000.0 / 13.0) * vc.nc * vc.ncb
            val p2 = ac / vc.nbb
            val hSin = sin(hRad)
            val hCos = cos(hRad)
            val gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11.0 * t * hCos + 108.0 * t * hSin)
            val a = gamma * hCos
            val b = gamma * hSin
            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0
            val rF = unadapt(rA) / vc.rgbD[0]
            val gF = unadapt(gA) / vc.rgbD[1]
            val bF = unadapt(bA) / vc.rgbD[2]
            return doubleArrayOf(
                1.86206786 * rF - 1.01125463 * gF + 0.14918677 * bF,
                0.38752654 * rF + 0.62144744 * gF - 0.00897398 * bF,
                -0.01584150 * rF - 0.03412294 * gF + 1.04996444 * bF,
            )
        }

        private fun unadapt(adapted: Double): Double {
            val base = max(0.0, 27.13 * abs(adapted) / (400.0 - abs(adapted)))
            return sign(adapted) * (100.0 / ViewingConditions.fl) * base.pow(1.0 / 0.42)
        }
    }
}

/**
 * Finds the sRGB colour for a hue, chroma and tone. For a given chroma it bisects CAM16 lightness J
 * until the colour's luminance matches the tone; if that colour falls outside sRGB it bisects chroma
 * down to the most that fits. Tone is always met exactly (up to 8-bit rounding).
 */
internal object HctSolver {
    private const val GAMUT_SLACK = 0.01

    fun solve(hue: Double, chroma: Double, tone: Double): Int {
        if (chroma < 0.0001 || tone < 0.0001 || tone > 99.9999) return ColorMath.argbFromLstar(tone)
        val h = ((hue % 360.0) + 360.0) % 360.0
        val targetY = ColorMath.yFromLstar(tone)
        inGamut(h, chroma, targetY)?.let { return ColorMath.argbFromLinearRgb(it) }
        var low = 0.0
        var high = chroma
        var best: DoubleArray? = null
        repeat(22) {
            val mid = (low + high) / 2.0
            val rgb = inGamut(h, mid, targetY)
            if (rgb != null) {
                best = rgb
                low = mid
            } else {
                high = mid
            }
        }
        return best?.let { ColorMath.argbFromLinearRgb(it) } ?: ColorMath.argbFromLstar(tone)
    }

    /** Linear RGB for the hue and chroma at luminance [targetY], or null if it is outside sRGB. */
    private fun inGamut(hue: Double, chroma: Double, targetY: Double): DoubleArray? {
        var low = 0.0
        var high = 100.0
        var xyz = Cam16.xyzFromJch(50.0, chroma, hue)
        repeat(36) {
            val j = (low + high) / 2.0
            xyz = Cam16.xyzFromJch(j, chroma, hue)
            if (xyz[1] < targetY) low = j else high = j
        }
        if (abs(xyz[1] - targetY) > 0.05 * max(1.0, targetY)) return null
        val rgb = ColorMath.linearRgbFromXyz(xyz[0], xyz[1], xyz[2])
        return if (rgb.all { it >= -GAMUT_SLACK && it <= 100.0 + GAMUT_SLACK }) rgb else null
    }
}
