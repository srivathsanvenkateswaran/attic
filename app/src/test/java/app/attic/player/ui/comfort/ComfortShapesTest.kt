package app.attic.player.ui.comfort

import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Test

/** The superellipses trace kit.css's mask paths: every point the CSS lists lies on the Compose outline. */
class ComfortShapesTest {

    /** The first quarter of each mask's path in kit.css (viewBox 0 0 100 100), from (100,50) to (50,100). */
    private val sq = listOf(
        100.0 to 50.0, 99.9 to 64.8, 99.6 to 70.8, 99.1 to 75.4, 98.5 to 79.2, 97.6 to 82.5, 96.5 to 85.4, 95.3 to 87.9,
        93.8 to 90.1, 92.0 to 92.0, 90.1 to 93.8, 87.9 to 95.3, 85.4 to 96.5, 82.5 to 97.6, 79.2 to 98.5, 75.4 to 99.1,
        70.8 to 99.6, 64.8 to 99.9, 50.0 to 100.0,
    )
    private val sqSoft = listOf(
        100.0 to 50.0, 99.9 to 57.7, 99.4 to 63.0, 98.7 to 67.7, 97.7 to 71.9, 96.4 to 75.8, 94.8 to 79.3, 92.9 to 82.6,
        90.7 to 85.6, 88.3 to 88.3, 85.6 to 90.7, 82.6 to 92.9, 79.3 to 94.8, 75.8 to 96.4, 71.9 to 97.7, 67.7 to 98.7,
        63.0 to 99.4, 57.7 to 99.9, 50.0 to 100.0,
    )
    private val sqLg = listOf(
        100.0 to 50.0, 99.9 to 67.4, 99.8 to 72.9, 99.5 to 76.9, 99.2 to 80.1, 98.7 to 82.8, 98.1 to 85.2, 97.5 to 87.3,
        96.7 to 89.1, 95.7 to 90.7, 94.7 to 92.2, 93.5 to 93.5, 92.2 to 94.7, 90.7 to 95.7, 89.1 to 96.7, 87.3 to 97.5,
        85.2 to 98.1, 82.8 to 98.7, 80.1 to 99.2, 76.9 to 99.5, 72.9 to 99.8, 67.4 to 99.9, 50.0 to 100.0,
    )

    @Test
    fun squircleMatchesSq() = assertOnCurve(ComfortShapes.Squircle, sq)

    @Test
    fun softSquircleMatchesSqSoft() = assertOnCurve(ComfortShapes.SquircleSoft, sqSoft)

    @Test
    fun largeSquircleMatchesSqLg() = assertOnCurve(ComfortShapes.SquircleLarge, sqLg)

    private fun assertOnCurve(shape: SuperellipseShape, points: List<Pair<Double, Double>>) {
        val outline = shape.unitPoints
        for ((x, y) in points) {
            var best = Double.MAX_VALUE
            val count = outline.size / 2
            for (i in 0 until count) {
                val j = (i + 1) % count
                best = minOf(best, segmentDistance(x, y, outline[i * 2] * 100.0, outline[i * 2 + 1] * 100.0, outline[j * 2] * 100.0, outline[j * 2 + 1] * 100.0))
            }
            // The CSS rounds to 0.1; the outline is a polyline of 720 points.
            assertTrue("($x, $y) is $best from $shape", best < 0.08)
        }
    }

    private fun segmentDistance(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val length = dx * dx + dy * dy
        val t = if (length == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / length).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}
