package app.attic.player.ui.comfort

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.LruCache
import java.nio.ByteBuffer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import coil3.size.pxOrElse
import coil3.transform.Transformation
import kotlin.math.max
import kotlin.math.roundToInt
import coil3.size.Size as CoilSize

/**
 * Bakes a cover's superellipse into its bitmap, so the cover draws as a plain image with no clip.
 *
 * A clip to a superellipse is a path clip: the RenderThread stencils it again on every frame the
 * cover moves, for every draw inside it. Comfort's lists scrolled behind Expressive's and the old
 * UI's because of it (3.1% janky frames in Library > Songs against 2.2%, about 3ms more GPU time a
 * frame). Here the shape is applied once, when the picture is loaded, and Coil's memory cache keeps
 * the result, keyed by this mask and the size.
 *
 * The output is exactly the size the picture is drawn at, cropped and centred the way Coil draws a
 * bitmap with [androidx.compose.ui.layout.ContentScale.Crop] ([cropRect]), so it lands on the same
 * pixels as before. Its edge is [superellipseCoverage], the clip's own coverage, so the edge pixels
 * are those of the clip too. It is then made a hardware bitmap, so it is uploaded once off the
 * RenderThread and holds no copy in the app's heap.
 *
 * [tile] is one quarter of a mosaic: the bitmap is that quarter, and the outline is the whole box's.
 */
internal class SuperellipseMask(
    private val shape: Shape,
    private val density: Float,
    private val tile: MosaicTile? = null,
) : Transformation() {
    override val cacheKey: String = "attic.squircle(${shapeKey(shape)},$density,$tile)"

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        val width = size.width.pxOrElse { input.width }
        val height = size.height.pxOrElse { input.height }
        val boxWidth = tile?.boxWidth ?: width
        val boxHeight = tile?.boxHeight ?: height
        val left = tile?.let { if (it.column == 0) 0 else boxWidth - width } ?: 0
        val top = tile?.let { if (it.row == 0) 0 else boxHeight - height } ?: 0

        val masked = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(masked)
        val coverage = coverageBitmap(resolve(shape, boxWidth, boxHeight, density), boxWidth, boxHeight)
        canvas.drawBitmap(coverage, -left.toFloat(), -top.toFloat(), null)
        val picture = if (input.config == Bitmap.Config.HARDWARE) input.copy(Bitmap.Config.ARGB_8888, false) else input
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
        val crop = cropRect(picture.width, picture.height, width, height)
        canvas.drawBitmap(picture, null, RectF(crop.left.toFloat(), crop.top.toFloat(), crop.right.toFloat(), crop.bottom.toFloat()), paint)
        // The thumbnail is ArtFetcher's own and nothing else holds it (the memory cache keeps the
        // result), so its pixels go now rather than at the next garbage collection.
        if (picture !== input) picture.recycle()
        if (input.config != Bitmap.Config.HARDWARE) input.recycle()
        return masked.copy(Bitmap.Config.HARDWARE, false)?.also { masked.recycle() } ?: masked
    }

    override fun equals(other: Any?) =
        other is SuperellipseMask && other.shape == shape && other.density == density && other.tile == tile

    override fun hashCode() = (shape.hashCode() * 31 + density.hashCode()) * 31 + tile.hashCode()

    internal companion object {
        /** True for the shapes this can bake: [ArtShape] and the superellipses. */
        fun canBake(shape: Shape) = shape == ArtShape || shape is SuperellipseShape

        /** The superellipse [shape] is in a box [width] by [height] pixels. */
        fun resolve(shape: Shape, width: Int, height: Int, density: Float): SuperellipseShape = when (shape) {
            ArtShape -> ArtShape.superellipseFor(Size(width.toFloat(), height.toFloat()), Density(density))
            is SuperellipseShape -> shape
            else -> error("SuperellipseMask cannot bake $shape")
        }

        private fun shapeKey(shape: Shape) = when (shape) {
            ArtShape -> "art"
            is SuperellipseShape -> "n${shape.exponent}"
            else -> error("SuperellipseMask cannot bake $shape")
        }

        /**
         * Where Coil draws a [sourceWidth] by [sourceHeight] bitmap in a [width] by [height] box with
         * `ContentScale.Crop` and centred: scaled to fill, its size rounded, its offset rounded
         * (AsyncImage's content painter and Compose's `BitmapPainter`), the part outside cut off.
         */
        fun cropRect(sourceWidth: Int, sourceHeight: Int, width: Int, height: Int): IntRect {
            val scale = max(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
            val scaledWidth = (sourceWidth * scale).roundToInt()
            val scaledHeight = (sourceHeight * scale).roundToInt()
            val left = ((width - scaledWidth).toFloat() / 2f).roundToInt()
            val top = ((height - scaledHeight).toFloat() / 2f).roundToInt()
            return IntRect(left, top, left + scaledWidth, top + scaledHeight)
        }
    }
}

/**
 * One quarter of a [Mosaic]: [column] and [row] (0 or 1) in a box [boxWidth] by [boxHeight] pixels.
 * The quarters fill the box, so the right column starts at the box width less its own width.
 */
internal data class MosaicTile(val boxWidth: Int, val boxHeight: Int, val column: Int, val row: Int)

/**
 * How much of each pixel of a [width] by [height] box [shape] covers, as the GPU covers it when it
 * clips to the shape: four samples a pixel, at the standard 4x multisample positions, each in or out
 * of the outline's polygon. So 0, 1/4, 1/2, 3/4 or all, as alpha 0..255, row by row.
 *
 * On this phone a clip to a superellipse is drawn that way (every edge pixel of a clipped cover in a
 * screenshot is a quarter step, and these samples predict each one). Drawing the same coverage keeps
 * the edges of a baked cover or a filled icon box identical to the clipped ones they replace.
 */
internal fun superellipseCoverage(shape: SuperellipseShape, width: Int, height: Int): ByteArray {
    val points = shape.outlinePoints(Size(width.toFloat(), height.toFloat()))
    val n = points.size / 2
    val counts = ByteArray(width * height)
    for (row in 0 until height) {
        for (s in 0 until 4) {
            val y = row + SAMPLE_Y[s].toDouble()
            var low = Double.POSITIVE_INFINITY
            var high = Double.NEGATIVE_INFINITY
            for (i in 0 until n) {
                val x1 = points[i * 2].toDouble()
                val y1 = points[i * 2 + 1].toDouble()
                val j = (i + 1) % n
                val x2 = points[j * 2].toDouble()
                val y2 = points[j * 2 + 1].toDouble()
                if ((y1 > y) != (y2 > y)) {
                    val x = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
                    if (x < low) low = x
                    if (x > high) high = x
                }
            }
            if (low > high) continue
            // A sample is in when it lies on or right of the left crossing and left of the right one.
            val dx = SAMPLE_X[s].toDouble()
            val first = maxOf(0, kotlin.math.ceil(low - dx).toInt())
            val last = minOf(width - 1, kotlin.math.ceil(high - dx).toInt() - 1)
            for (column in first..last) counts[row * width + column]++
        }
    }
    for (i in counts.indices) counts[i] = COVERAGE_ALPHA[counts[i].toInt()]
    return counts
}

/** Standard 4x multisample positions: sample s sits at (SAMPLE_X[s], SAMPLE_Y[s]) in its pixel. */
private val SAMPLE_X = floatArrayOf(0.375f, 0.875f, 0.125f, 0.625f)
private val SAMPLE_Y = floatArrayOf(0.125f, 0.375f, 0.625f, 0.875f)
private val COVERAGE_ALPHA = byteArrayOf(0, 64, 128.toByte(), 191.toByte(), 255.toByte())

private val coverageBitmaps = LruCache<String, Bitmap>(48)

/**
 * [superellipseCoverage] as an alpha-only bitmap, kept for reuse: a fill draws it tinted, a cover is
 * cut to it.
 */
internal fun coverageBitmap(shape: SuperellipseShape, width: Int, height: Int): Bitmap {
    val key = "${shape.exponent}:$width:$height"
    coverageBitmaps.get(key)?.let { return it }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    val alpha = superellipseCoverage(shape, width, height)
    val rows = ByteArray(bitmap.rowBytes * height)
    for (row in 0 until height) System.arraycopy(alpha, row * width, rows, row * bitmap.rowBytes, width)
    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rows))
    coverageBitmaps.put(key, bitmap)
    return bitmap
}
