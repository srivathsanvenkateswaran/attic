package app.attic.player.ui.comfort

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A superellipse `|x|^n + |y|^n = 1` stretched to the box: kit.css's `--sq` (n = 4), `--sq-lg`
 * (n = 5) and `--sq-soft` (n = 2.6) masks. The CSS samples the curve at 72 or 88 points
 * (`x = cos(t)^(2/n)`, `y = sin(t)^(2/n)`, t in even steps); this traces the same curve at
 * [SAMPLES] points so large covers stay smooth. Every CSS point lies on it (ComfortShapesTest).
 *
 * The outline it draws has only as many points as its size needs to stay within 0.1px of the curve
 * ([samplesFor]): 64 for a 48dp row cover, about 160 for Now Playing's. A clip to this shape is a
 * path clip the RenderThread redoes on every frame it moves, at a cost that grows with the points:
 * at 720 points for every size, scrolling Library > Songs spent a quarter of its RenderThread time in
 * `SkPath::isConvex` and stencil ops, and Comfort missed frames Expressive did not.
 */
@Immutable
class SuperellipseShape(val exponent: Double) : Shape {
    /** The outline on a unit square, (0,0) to (1,1), as x,y pairs. */
    internal val unitPoints: FloatArray = FloatArray(SAMPLES * 2).also { points ->
        val power = 2.0 / exponent
        for (i in 0 until SAMPLES) {
            val t = 2 * Math.PI * i / SAMPLES
            val c = cos(t)
            val s = sin(t)
            points[i * 2] = (0.5 + 0.5 * sign(c) * abs(c).pow(power)).toFloat()
            points[i * 2 + 1] = (0.5 + 0.5 * sign(s) * abs(s).pow(power)).toFloat()
        }
    }

    private val pointsBySamples = ConcurrentHashMap<Int, FloatArray>()

    /** The outline on a unit square at [samples] points, for a shape drawn [sizePx] across. */
    internal fun unitPointsFor(sizePx: Float): FloatArray {
        val samples = samplesFor(sizePx)
        return if (samples == SAMPLES) unitPoints else pointsBySamples.getOrPut(samples) { points(samples) }
    }

    private fun points(samples: Int) = FloatArray(samples * 2).also { points ->
        val power = 2.0 / exponent
        for (i in 0 until samples) {
            val t = 2 * Math.PI * i / samples
            val c = cos(t)
            val s = sin(t)
            points[i * 2] = (0.5 + 0.5 * sign(c) * abs(c).pow(power)).toFloat()
            points[i * 2 + 1] = (0.5 + 0.5 * sign(s) * abs(s).pow(power)).toFloat()
        }
    }

    /** The outline's corners in a box of [size], as x,y pairs: the polygon [createOutline] draws. */
    internal fun outlinePoints(size: Size): FloatArray {
        val unit = unitPointsFor(size.maxDimension)
        return FloatArray(unit.size) { i -> unit[i] * (if (i % 2 == 0) size.width else size.height) }
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = outlinePoints(size)
        val path = Path()
        for (i in 0 until points.size / 2) {
            if (i == 0) path.moveTo(points[0], points[1]) else path.lineTo(points[i * 2], points[i * 2 + 1])
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is SuperellipseShape && other.exponent == exponent
    override fun hashCode() = exponent.hashCode()
    override fun toString() = "SuperellipseShape($exponent)"

    internal companion object {
        const val SAMPLES = 720

        /**
         * Points for an outline [sizePx] across: the chord of a polyline at N even steps of t sits at
         * most about 2.4 / N² of the size off these curves, so N = sqrt(25 * size) keeps it under
         * 0.1px. Rounded up to a multiple of 8 so the corners and the axes stay on the outline.
         */
        fun samplesFor(sizePx: Float): Int {
            if (!(sizePx > 0f)) return SAMPLES
            val samples = ceil(sqrt(25.0 * sizePx) / 8).toInt() * 8
            return samples.coerceIn(32, SAMPLES)
        }
    }
}

/**
 * Artwork and icon containers: `.sq` below 100dp and `.sq-lg` from 100dp, chosen by the size the
 * shape is drawn at. Use it on any cover, mosaic, icon box or tonal button and the kit's size rule
 * holds by itself.
 */
@Immutable
object ArtShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        superellipseFor(size, density).createOutline(size, layoutDirection, density)

    /** The superellipse a box of [size] takes. */
    fun superellipseFor(size: Size, density: Density): SuperellipseShape {
        val large = with(density) { size.minDimension.toDp() } >= ComfortShapes.LargeArtFrom
        return if (large) ComfortShapes.SquircleLarge else ComfortShapes.Squircle
    }
}

/**
 * The kit's shape rules (oneui/KIT.md "Shape").
 *
 * - Every image and icon container is a superellipse: [ArtShape] (`.sq`, `.sq-lg` by size), and
 *   [SquircleSoft] (`.sq-soft`) for artists. Never a rounded rectangle, never a circle.
 * - Rectangles have three radii: [Floating] 30 for things that float (dock, deck, sheet, dialog, action
 *   bar), [Group] 26 for groups, [Control] 18 for a control nested in a card. [Menu] 22 is the pop-over
 *   menu's own radius.
 * - Buttons and chips are pills ([Pill]). The only circles are the radio, the switch knob and the
 *   spinner.
 */
object ComfortShapes {
    val Squircle = SuperellipseShape(4.0)
    val SquircleLarge = SuperellipseShape(5.0)
    val SquircleSoft = SuperellipseShape(2.6)

    /** `.sq-lg` starts here. */
    val LargeArtFrom: Dp = 100.dp

    val FloatRadius = 30.dp
    val GroupRadius = 26.dp
    val ControlRadius = 18.dp
    val MenuRadius = 22.dp

    val Floating = RoundedCornerShape(FloatRadius)
    val FloatingTop = RoundedCornerShape(topStart = FloatRadius, topEnd = FloatRadius)
    val Group = RoundedCornerShape(GroupRadius)
    val Control = RoundedCornerShape(ControlRadius)
    val Menu = RoundedCornerShape(MenuRadius)
    val Pill = RoundedCornerShape(50)

    /** Radio, switch knob, spinner. Nothing else. */
    val Circle = CircleShape

    /** The superellipse for a cover drawn at [size]: `.sq` up to 96dp, `.sq-lg` from 100dp. */
    fun art(size: Dp): Shape = if (size >= LargeArtFrom) SquircleLarge else Squircle

    /** Material's shape slots, for stock components: 8, 18, 18, 26, 30. */
    val material = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = Control,
        medium = Control,
        large = Group,
        extraLarge = Floating,
    )
}
