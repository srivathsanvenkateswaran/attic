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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * A superellipse `|x|^n + |y|^n = 1` stretched to the box: kit.css's `--sq` (n = 4), `--sq-lg`
 * (n = 5) and `--sq-soft` (n = 2.6) masks. The CSS samples the curve at 72 or 88 points
 * (`x = cos(t)^(2/n)`, `y = sin(t)^(2/n)`, t in even steps); this traces the same curve at
 * [SAMPLES] points so large covers stay smooth. Every CSS point lies on it (ComfortShapesTest).
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

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        for (i in 0 until SAMPLES) {
            val x = unitPoints[i * 2] * size.width
            val y = unitPoints[i * 2 + 1] * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is SuperellipseShape && other.exponent == exponent
    override fun hashCode() = exponent.hashCode()
    override fun toString() = "SuperellipseShape($exponent)"

    internal companion object {
        const val SAMPLES = 720
    }
}

/**
 * Artwork and icon containers: `.sq` below 100dp and `.sq-lg` from 100dp, chosen by the size the
 * shape is drawn at. Use it on any cover, mosaic, icon box or tonal button and the kit's size rule
 * holds by itself.
 */
@Immutable
object ArtShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val large = with(density) { size.minDimension.toDp() } >= ComfortShapes.LargeArtFrom
        return (if (large) ComfortShapes.SquircleLarge else ComfortShapes.Squircle).createOutline(size, layoutDirection, density)
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
