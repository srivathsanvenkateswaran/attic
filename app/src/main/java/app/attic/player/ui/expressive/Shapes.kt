package app.attic.player.ui.expressive

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The kit's shape rules.
 *
 * - Covers (albums, playlists, folders) are rounded squares whose radius follows their size:
 *   [CoverShape] measures itself, so any cover at any size is right.
 * - Artists are the nine-lobed cookie, [CookieShape], at every size. Never a circle.
 * - Buttons, bars and pills are fully round ([Pill]).
 * - Squared off means on: [onCorner] gives the radius a control takes when it is on.
 * - A list is a group: rows 2dp apart, outer corners 20, inner corners 4 ([groupItemShape]).
 */
object ExpressiveShapes {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val LargeIncreased = 20.dp
    val ExtraLarge = 28.dp
    val ExtraLargeIncreased = 32.dp
    val ExtraExtraLarge = 48.dp

    /** The Material shape scale, which matches the kit's corner tokens. */
    val material = Shapes(
        extraSmall = RoundedCornerShape(ExtraSmall),
        small = RoundedCornerShape(Small),
        medium = RoundedCornerShape(Medium),
        large = RoundedCornerShape(Large),
        extraLarge = RoundedCornerShape(ExtraLarge),
    )

    val Pill = CircleShape

    /** The gap between rows of a group. */
    val GroupGap = 2.dp
    /** The space between two groups. */
    val GroupSpacing = 12.dp

    /** A cover's corner radius for its size: 8 up to 48dp, 12 up to 64, 16 up to 128, 20 up to 180, 28 above. */
    fun coverRadius(size: Dp): Dp = when {
        size <= 48.dp -> Small
        size <= 64.dp -> Medium
        size <= 128.dp -> Large
        size <= 180.dp -> LargeIncreased
        else -> ExtraLarge
    }

    /** A cover of a known size. For a cover whose size comes from layout, use [CoverShape]. */
    fun cover(size: Dp): RoundedCornerShape = RoundedCornerShape(coverRadius(size))

    /** The shape of row [index] of [count] in a group: 20 on the group's outer corners, 4 inside. */
    fun groupItemShape(index: Int, count: Int): RoundedCornerShape {
        val top = if (index == 0) LargeIncreased else ExtraSmall
        val bottom = if (index == count - 1) LargeIncreased else ExtraSmall
        return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }

    /**
     * The corner a control takes when it is on ("squared off means on"): a 36dp play control 11,
     * a 56dp one 18, the wide Now Playing one 28, a 40dp toggle 12, a 56dp toggle 16.
     */
    fun onCorner(height: Dp): Dp = when {
        height <= 36.dp -> 11.dp
        height <= 40.dp -> Medium
        height <= 56.dp -> Large
        else -> ExtraLarge
    }
}

/** A rounded square whose radius follows its measured size, by [ExpressiveShapes.coverRadius]. */
object CoverShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val dp = with(density) { size.minDimension.toDp() }
        val radius = with(density) { ExpressiveShapes.coverRadius(dp).toPx() }
        return RoundedCornerShape(radius).createOutline(size, layoutDirection, density)
    }
}

/**
 * A closed shape described by its radius at each angle around the centre, sampled evenly. Shapes in
 * this form can be morphed into each other by blending radii, which is how [LoadingIndicator] works.
 */
class RadialShape internal constructor(internal val radii: FloatArray) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path(size, 0f))

    /** The outline at [size], turned by [rotation] radians. */
    fun path(size: Size, rotation: Float, into: Path = Path()): Path {
        into.reset()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val n = radii.size
        for (i in 0 until n) {
            val angle = (2.0 * PI * i / n).toFloat() + rotation
            val x = cx + cos(angle) * radii[i] * cx
            val y = cy + sin(angle) * radii[i] * cy
            if (i == 0) into.moveTo(x, y) else into.lineTo(x, y)
        }
        into.close()
        return into
    }

    companion object {
        internal const val SAMPLES = 180

        /** Blends two shapes: 0 is [from], 1 is [to]. */
        fun lerp(from: RadialShape, to: RadialShape, fraction: Float, into: FloatArray = FloatArray(SAMPLES)): FloatArray {
            for (i in into.indices) into[i] = from.radii[i] + (to.radii[i] - from.radii[i]) * fraction
            return into
        }

        /** From a CSS `polygon()` list in percent, star-shaped around the centre, as the kit's shapes are. */
        internal fun fromPolygon(percentPoints: String): RadialShape {
            val values = Regex("""(-?[0-9.]+)%""").findAll(percentPoints).map { it.groupValues[1].toFloat() / 50f - 1f }.toList()
            val points = values.chunked(2).map { (x, y) -> x to y }
            val polar = points.map { (x, y) -> atan2(y, x).let { if (it < 0) it + 2f * PI.toFloat() else it } to sqrt(x * x + y * y) }
                .sortedBy { it.first }
            val radii = FloatArray(SAMPLES) { i ->
                val angle = (2.0 * PI * i / SAMPLES).toFloat()
                val after = polar.indexOfFirst { it.first >= angle }.let { if (it < 0) 0 else it }
                val before = (after - 1 + polar.size) % polar.size
                val (a0, r0) = polar[before]
                val (a1, r1) = polar[after]
                val span = ((a1 - a0) + 2f * PI.toFloat()) % (2f * PI.toFloat())
                val into = ((angle - a0) + 2f * PI.toFloat()) % (2f * PI.toFloat())
                if (span == 0f) r0 else r0 + (r1 - r0) * (into / span)
            }
            return RadialShape(radii)
        }

        /** A shape from a function of angle (radians) to radius, scaled so its widest point touches the bounds. */
        internal fun of(radius: (Double) -> Double): RadialShape {
            val raw = DoubleArray(SAMPLES) { radius(2.0 * PI * it / SAMPLES) }
            val max = raw.max()
            return RadialShape(FloatArray(SAMPLES) { (raw[it] / max).toFloat() })
        }
    }
}

/** The nine-lobed cookie for artists: the kit's `--shape-cookie`, which is Material's Cookie9Sided. */
val CookieShape: RadialShape = RadialShape.fromPolygon(
    "50.0% 0.0%,52.6% 0.3%,55.1% 1.1%,57.5% 2.5%,59.8% 4.1%,61.8% 5.9%,63.8% 7.5%,65.9% 8.6%,68.4% 8.6%,71.1% 8.6%,73.9% 8.7%," +
        "76.6% 9.1%,79.2% 9.9%,81.4% 11.2%,83.4% 12.9%,84.9% 15.1%,86.0% 17.6%,86.8% 20.2%,87.3% 22.9%,87.7% 25.5%,88.3% 27.9%," +
        "90.1% 29.6%,92.1% 31.3%,94.2% 33.0%,96.1% 35.0%,97.7% 37.2%,98.8% 39.6%,99.4% 42.2%,99.3% 44.8%,98.8% 47.4%,97.7% 50.0%," +
        "96.4% 52.4%,95.1% 54.7%,93.9% 56.9%,93.7% 59.3%,94.1% 61.8%,94.6% 64.5%,94.9% 67.2%,94.9% 70.0%,94.3% 72.6%,93.3% 75.0%," +
        "91.7% 77.1%,89.7% 78.9%,87.4% 80.3%,84.9% 81.4%,82.3% 82.3%,79.9% 83.2%,77.9% 84.5%,76.6% 86.7%,75.3% 89.0%,73.9% 91.3%," +
        "72.2% 93.5%,70.2% 95.3%,67.9% 96.7%,65.4% 97.5%,62.8% 97.7%,60.1% 97.4%,57.4% 96.8%,54.8% 95.8%,52.4% 94.9%,50.0% 94.2%," +
        "47.6% 94.9%,45.2% 95.8%,42.6% 96.8%,39.9% 97.4%,37.2% 97.7%,34.6% 97.5%,32.1% 96.7%,29.8% 95.3%,27.8% 93.5%,26.1% 91.3%," +
        "24.7% 89.0%,23.4% 86.7%,22.1% 84.5%,20.1% 83.2%,17.7% 82.3%,15.1% 81.4%,12.6% 80.3%,10.3% 78.9%,8.3% 77.1%,6.7% 75.0%," +
        "5.7% 72.6%,5.1% 70.0%,5.1% 67.2%,5.4% 64.5%,5.9% 61.8%,6.3% 59.3%,6.1% 56.9%,4.9% 54.7%,3.6% 52.4%,2.3% 50.0%,1.2% 47.4%," +
        "0.7% 44.8%,0.6% 42.2%,1.2% 39.6%,2.3% 37.2%,3.9% 35.0%,5.8% 33.0%,7.9% 31.3%,9.9% 29.6%,11.7% 27.9%,12.3% 25.5%,12.7% 22.9%," +
        "13.2% 20.2%,14.0% 17.6%,15.1% 15.1%,16.6% 12.9%,18.6% 11.2%,20.8% 9.9%,23.4% 9.1%,26.1% 8.7%,28.9% 8.6%,31.6% 8.6%,34.1% 8.6%," +
        "36.2% 7.5%,38.2% 5.9%,40.2% 4.1%,42.5% 2.5%,44.9% 1.1%,47.4% 0.3%",
)

/**
 * The four-lobed clover for "nothing here yet" (`--shape-clover` on sheets 06 and 08), Material's
 * Clover4Leaf. [EmptyState] draws its picture in it when asked.
 */
val CloverShape: RadialShape = RadialShape.fromPolygon(
    "50.0% 4.4%,53.0% 4.2%,56.1% 3.7%,59.4% 2.9%,62.8% 2.1%,66.6% 1.2%,70.5% 0.4%,74.7% 0.0%,78.8% 0.0%,83.0% 0.6%," +
        "86.9% 1.9%,90.5% 3.8%,93.6% 6.4%,96.2% 9.5%,98.1% 13.1%,99.4% 17.0%,100.0% 21.2%,100.0% 25.3%,99.6% 29.5%,98.8% 33.4%," +
        "97.9% 37.2%,97.1% 40.6%,96.3% 43.9%,95.8% 47.0%,95.6% 50.0%,95.8% 53.0%,96.3% 56.1%,97.1% 59.4%,97.9% 62.8%," +
        "98.8% 66.6%,99.6% 70.5%,100.0% 74.7%,100.0% 78.8%,99.4% 83.0%,98.1% 86.9%,96.2% 90.5%,93.6% 93.6%,90.5% 96.2%," +
        "86.9% 98.1%,83.0% 99.4%,78.8% 100.0%,74.7% 100.0%,70.5% 99.6%,66.6% 98.8%,62.8% 97.9%,59.4% 97.1%,56.1% 96.3%," +
        "53.0% 95.8%,50.0% 95.6%,47.0% 95.8%,43.9% 96.3%,40.6% 97.1%,37.2% 97.9%,33.4% 98.8%,29.5% 99.6%,25.3% 100.0%," +
        "21.2% 100.0%,17.0% 99.4%,13.1% 98.1%,9.5% 96.2%,6.4% 93.6%,3.8% 90.5%,1.9% 86.9%,0.6% 83.0%,0.0% 78.8%,0.0% 74.7%," +
        "0.4% 70.5%,1.2% 66.6%,2.1% 62.8%,2.9% 59.4%,3.7% 56.1%,4.2% 53.0%,4.4% 50.0%,4.2% 47.0%,3.7% 43.9%,2.9% 40.6%," +
        "2.1% 37.2%,1.2% 33.4%,0.4% 29.5%,0.0% 25.3%,0.0% 21.2%,0.6% 17.0%,1.9% 13.1%,3.8% 9.5%,6.4% 6.4%,9.5% 3.8%,13.1% 1.9%," +
        "17.0% 0.6%,21.2% 0.0%,25.3% 0.0%,29.5% 0.4%,33.4% 1.2%,37.2% 2.1%,40.6% 2.9%,43.9% 3.7%,47.0% 4.2%",
)

/** The soft burst the kit's loader is drawn mid-morph in (`--shape-burst`). */
val BurstShape: RadialShape = RadialShape.fromPolygon(
    "50.0% 0.0%,53.2% 0.8%,56.2% 2.9%,58.8% 5.9%,61.0% 8.9%,63.1% 11.5%,65.3% 13.0%,68.0% 13.5%,71.2% 13.2%,75.0% 12.6%,78.9% 12.3%," +
        "82.5% 12.9%,85.4% 14.6%,87.1% 17.5%,87.7% 21.1%,87.4% 25.0%,86.8% 28.7%,86.5% 32.0%,87.0% 34.7%,88.5% 36.9%,91.1% 39.0%," +
        "94.1% 41.2%,97.1% 43.8%,99.2% 46.8%,100.0% 50.0%,99.2% 53.2%,97.1% 56.2%,94.1% 58.8%,91.1% 61.0%,88.5% 63.1%,87.0% 65.3%," +
        "86.5% 68.0%,86.8% 71.2%,87.4% 75.0%,87.7% 78.9%,87.1% 82.5%,85.4% 85.4%,82.5% 87.1%,78.9% 87.7%,75.0% 87.4%,71.2% 86.8%," +
        "68.0% 86.5%,65.3% 87.0%,63.1% 88.5%,61.0% 91.1%,58.8% 94.1%,56.2% 97.1%,53.2% 99.2%,50.0% 100.0%,46.8% 99.2%,43.8% 97.1%," +
        "41.2% 94.1%,39.0% 91.1%,36.9% 88.5%,34.7% 87.0%,32.0% 86.5%,28.8% 86.8%,25.0% 87.4%,21.1% 87.7%,17.5% 87.1%,14.6% 85.4%," +
        "12.9% 82.5%,12.3% 78.9%,12.6% 75.0%,13.2% 71.3%,13.5% 68.0%,13.0% 65.3%,11.5% 63.1%,8.9% 61.0%,5.9% 58.8%,2.9% 56.2%," +
        "0.8% 53.2%,0.0% 50.0%,0.8% 46.8%,2.9% 43.8%,5.9% 41.2%,8.9% 39.0%,11.5% 36.9%,13.0% 34.7%,13.5% 32.0%,13.2% 28.7%,12.6% 25.0%," +
        "12.3% 21.1%,12.9% 17.5%,14.6% 14.6%,17.5% 12.9%,21.1% 12.3%,25.0% 12.6%,28.7% 13.2%,32.0% 13.5%,34.7% 13.0%,36.9% 11.5%," +
        "39.0% 8.9%,41.2% 5.9%,43.8% 2.9%,46.8% 0.8%",
)

/** The shapes the loading indicator morphs through, after Material's set: burst, cookie, pentagon, pill, sunny, four-leaf, oval. */
internal val LoaderShapes: List<RadialShape> by lazy {
    fun lobed(lobes: Int, depth: Double) = RadialShape.of { a -> 1.0 - depth * (1.0 - cos(lobes * a)) / 2.0 }
    fun roundedPolygon(sides: Int, softness: Double) = RadialShape.of { a ->
        val sector = 2.0 * PI / sides
        val local = ((a + PI / 2.0) % sector + sector) % sector - sector / 2.0
        val polygon = cos(sector / 2.0) / cos(local)
        polygon * (1.0 - softness) + softness * cos(sector / 2.0) * 1.08
    }
    val pill = RadialShape.of { a ->
        // A stadium 1 wide and 0.62 tall, set diagonally the way Material's pill sits in the indicator.
        val t = a - PI / 4.0
        val x = cos(t)
        val y = sin(t)
        val half = 0.62
        val straight = 1.0 - half
        var low = 0.0
        var high = 2.0
        repeat(30) {
            val r = (low + high) / 2.0
            val px = kotlin.math.abs(x * r)
            val py = kotlin.math.abs(y * r)
            val dx = (px - straight).coerceAtLeast(0.0)
            val inside = if (px <= straight) py <= half else dx * dx + py * py <= half * half
            if (inside) low = r else high = r
        }
        low
    }
    listOf(
        BurstShape,
        CookieShape,
        roundedPolygon(5, 0.35),
        pill,
        lobed(8, 0.10),
        lobed(4, 0.22),
        RadialShape.of { a -> 1.0 / sqrt((cos(a) / 1.0) * (cos(a) / 1.0) + (sin(a) / 0.82) * (sin(a) / 0.82)) },
    )
}
