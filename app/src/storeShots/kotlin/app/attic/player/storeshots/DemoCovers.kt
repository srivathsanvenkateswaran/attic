package app.attic.player.storeshots

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Abstract album covers drawn in code, so the screenshots carry no one's artwork. Each is a 1000px
 * square: a gradient ground, a few shapes, and film grain. Index them 0 until [COUNT]; the colours
 * are strong and far apart, so the cover-tinted screens each come out a different colour.
 */
object DemoCovers {
    const val COUNT = 12
    private const val SIZE = 1000
    private val cache = HashMap<Int, Bitmap>()

    fun get(index: Int): Bitmap = cache.getOrPut(index) { draw(index) }

    private fun draw(index: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = SIZE.toFloat()
        when (index) {
            0 -> sunOverWater(canvas, s)
            1 -> rings(canvas, s)
            2 -> bauhausBands(canvas, s)
            3 -> blobs(canvas, s)
            4 -> dotGrid(canvas, s)
            5 -> arches(canvas, s)
            6 -> waves(canvas, s)
            7 -> overlappingSquares(canvas, s)
            8 -> nightSky(canvas, s)
            9 -> burst(canvas, s)
            10 -> halftone(canvas, s)
            else -> horizon(canvas, s)
        }
        grain(bitmap, seed = index, amount = if (index == 8) 22 else 16)
        return bitmap
    }

    private fun paint(color: Long) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() }

    private fun vertical(canvas: Canvas, s: Float, top: Long, bottom: Long) {
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, s, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, s, s, p)
    }

    /** A low sun over striped water, coral to magenta. */
    private fun sunOverWater(c: Canvas, s: Float) {
        vertical(c, s, 0xFFFF8A5B, 0xFFB0245E)
        val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s * 0.25f, 0f, s * 0.75f, 0xFFFFE27A.toInt(), 0xFFFFA84A.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawCircle(s * 0.5f, s * 0.55f, s * 0.26f, sun)
        val water = paint(0xFF2A1450)
        c.drawRect(0f, s * 0.62f, s, s, water)
        val stripe = paint(0xFFFFC56B)
        for (i in 0 until 7) {
            val y = s * (0.65f + i * 0.045f)
            val half = s * (0.24f - i * 0.028f)
            c.drawRoundRect(RectF(s * 0.5f - half, y, s * 0.5f + half, y + s * 0.014f), 8f, 8f, stripe)
        }
    }

    /** Off-centre rings, teal into lime. */
    private fun rings(c: Canvas, s: Float) {
        vertical(c, s, 0xFF00494F, 0xFF002A33)
        val colors = listOf(0xFFC6F432, 0xFF5EE6B5, 0xFF16B3A6, 0xFF0A7D80, 0xFF06555C)
        for (i in 9 downTo 0) {
            c.drawCircle(s * 0.62f, s * 0.42f, s * (0.08f + i * 0.075f), paint(colors[i % colors.size]))
        }
    }

    /** Diagonal bands and a half disc, mustard, rust, cream and ink. */
    private fun bauhausBands(c: Canvas, s: Float) {
        c.drawColor(0xFFF1E4C8.toInt())
        c.save()
        c.rotate(-28f, s / 2, s / 2)
        val bands = listOf(0xFFE3A51C, 0xFFC2471F, 0xFF1D1B26, 0xFFE3A51C, 0xFF2F5D8C)
        bands.forEachIndexed { i, color -> c.drawRect(-s, s * (0.12f + i * 0.17f), s * 2, s * (0.21f + i * 0.17f), paint(color)) }
        c.restore()
        c.drawArc(RectF(s * 0.18f, s * 0.18f, s * 0.82f, s * 0.82f), 180f, 180f, true, paint(0xFF1D1B26))
        c.drawCircle(s * 0.5f, s * 0.5f, s * 0.07f, paint(0xFFF1E4C8))
    }

    /** Soft colour fields that run into each other, violet, pink and orange. */
    private fun blobs(c: Canvas, s: Float) {
        c.drawColor(0xFF2B0F5C.toInt())
        fun blob(x: Float, y: Float, r: Float, color: Long) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x, y, r, color.toInt(), (color and 0x00FFFFFF).toInt(), Shader.TileMode.CLAMP)
            }
            c.drawCircle(x, y, r, p)
        }
        blob(s * 0.25f, s * 0.3f, s * 0.6f, 0xFFFF3D9A)
        blob(s * 0.8f, s * 0.75f, s * 0.6f, 0xFFFF9C2B)
        blob(s * 0.75f, s * 0.15f, s * 0.45f, 0xFF7B4DFF)
        blob(s * 0.2f, s * 0.9f, s * 0.4f, 0xFF5B1FD1)
    }

    /** A grid of dots, orange on cobalt, one of them missing. */
    private fun dotGrid(c: Canvas, s: Float) {
        vertical(c, s, 0xFF1E46D8, 0xFF0F2A8F)
        val n = 7
        val step = s / (n + 1)
        for (row in 1..n) for (col in 1..n) {
            if (row == 3 && col == 5) continue
            val big = (row + col) % 4 == 0
            c.drawCircle(col * step, row * step, if (big) step * 0.36f else step * 0.22f, paint(if (big) 0xFFFFB13B else 0xFFFF6A2B))
        }
    }

    /** Stacked arches, terracotta and sage on sand. */
    private fun arches(c: Canvas, s: Float) {
        c.drawColor(0xFFF0D9B5.toInt())
        val colors = listOf(0xFFC6533A, 0xFFE58F5B, 0xFF7E9C7A, 0xFF3E5B4F)
        colors.forEachIndexed { i, color ->
            val inset = s * (0.1f + i * 0.09f)
            val path = Path().apply {
                moveTo(inset, s)
                lineTo(inset, s * 0.5f + inset * 0.3f)
                arcTo(RectF(inset, s * 0.18f + inset * 0.3f, s - inset, s * 0.82f + inset * 0.3f), 180f, 180f)
                lineTo(s - inset, s)
                close()
            }
            c.drawPath(path, paint(color))
        }
        c.drawCircle(s * 0.8f, s * 0.18f, s * 0.07f, paint(0xFFC6533A))
    }

    /** Ink waves across an electric green field. */
    private fun waves(c: Canvas, s: Float) {
        vertical(c, s, 0xFF39F58A, 0xFF13B86B)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B2A1E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = s * 0.012f
            strokeCap = Paint.Cap.ROUND
        }
        for (line in 0 until 22) {
            val base = s * (0.06f + line * 0.042f)
            val amp = s * (0.015f + 0.05f * sin(line / 21.0 * PI).toFloat())
            val path = Path()
            var x = 0f
            path.moveTo(0f, base)
            while (x <= s) {
                path.lineTo(x, base + amp * sin(x / s * 4 * PI + line * 0.35).toFloat())
                x += 6f
            }
            c.drawPath(path, p)
        }
    }

    /** Turned squares that overlap and multiply, cyan, magenta and yellow on white. */
    private fun overlappingSquares(c: Canvas, s: Float) {
        c.drawColor(0xFFF7F3EC.toInt())
        fun square(cx: Float, cy: Float, half: Float, angle: Float, color: Long) {
            c.save()
            c.rotate(angle, cx, cy)
            val p = paint(color).apply { blendMode = BlendMode.MULTIPLY }
            c.drawRect(cx - half, cy - half, cx + half, cy + half, p)
            c.restore()
        }
        square(s * 0.4f, s * 0.42f, s * 0.24f, 12f, 0xFF00B5E2)
        square(s * 0.6f, s * 0.48f, s * 0.24f, -18f, 0xFFF0368F)
        square(s * 0.5f, s * 0.64f, s * 0.2f, 40f, 0xFFFFD400)
    }

    /** A crescent and stars over deep blue, for the late-night tapes. */
    private fun nightSky(c: Canvas, s: Float) {
        vertical(c, s, 0xFF0B1238, 0xFF3A2A6E)
        val random = Random(8)
        repeat(90) {
            val r = random.nextFloat() * s * 0.004f + s * 0.0015f
            c.drawCircle(random.nextFloat() * s, random.nextFloat() * s * 0.75f, r, paint(0xFFE9E4FF))
        }
        c.drawCircle(s * 0.66f, s * 0.36f, s * 0.17f, paint(0xFFFFE9A8))
        c.drawCircle(s * 0.73f, s * 0.31f, s * 0.15f, paint(0xFF15194A))
        val hills = Path().apply {
            moveTo(0f, s)
            lineTo(0f, s * 0.8f)
            cubicTo(s * 0.3f, s * 0.68f, s * 0.55f, s * 0.9f, s, s * 0.74f)
            lineTo(s, s)
            close()
        }
        c.drawPath(hills, paint(0xFF07091F))
    }

    /** Rays from a point low on the left, red and cream. */
    private fun burst(c: Canvas, s: Float) {
        c.drawColor(0xFFFFF1DC.toInt())
        val cx = s * 0.3f
        val cy = s * 0.78f
        val rays = 18
        val p = paint(0xFFE0262E)
        for (i in 0 until rays) {
            val a0 = (i * 2 * PI / (rays * 2)) * 2
            val a1 = a0 + PI / rays
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + 2 * s * cos(a0).toFloat(), cy + 2 * s * sin(a0).toFloat())
                lineTo(cx + 2 * s * cos(a1).toFloat(), cy + 2 * s * sin(a1).toFloat())
                close()
            }
            c.drawPath(path, p)
        }
        c.drawCircle(cx, cy, s * 0.16f, paint(0xFFFFF1DC))
        c.drawCircle(cx, cy, s * 0.11f, paint(0xFF1A1A1A))
    }

    /** Halftone dots growing toward one corner, emerald on near-black. */
    private fun halftone(c: Canvas, s: Float) {
        c.drawColor(0xFF07140F.toInt())
        val step = s / 26
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s, s, 0f, 0xFF00E39A.toInt(), 0xFF9BFFDA.toInt(), Shader.TileMode.CLAMP)
        }
        var y = step / 2
        while (y < s) {
            var x = step / 2
            while (x < s) {
                val d = hypot(x - s * 0.85f, y - s * 0.2f) / (s * 1.1f)
                val r = step * 0.48f * (1 - d).coerceIn(0.05f, 1f)
                c.drawCircle(x, y, r, p)
                x += step
            }
            y += step
        }
    }

    /** A wide horizon: peach sky over a violet sea, the line between them torn. */
    private fun horizon(c: Canvas, s: Float) {
        vertical(c, s, 0xFFFFC7A1, 0xFFFF7E79)
        val random = Random(11)
        val sea = Path().apply {
            moveTo(0f, s)
            var x = 0f
            lineTo(0f, s * 0.58f)
            while (x <= s) {
                lineTo(x, s * 0.58f + (random.nextFloat() - 0.5f) * s * 0.02f)
                x += s * 0.02f
            }
            lineTo(s, s)
            close()
        }
        c.drawPath(sea, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s * 0.58f, 0f, s, 0xFF6B3FA0.toInt(), 0xFF2A1248.toInt(), Shader.TileMode.CLAMP)
        })
        c.drawCircle(s * 0.28f, s * 0.36f, s * 0.1f, paint(0xFFFFF4E0))
        val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = SweepGradient(s * 0.28f, s * 0.36f, intArrayOf(0x00FFFFFF, 0x33FFFFFF, 0x00FFFFFF), null)
        }
        c.drawCircle(s * 0.28f, s * 0.36f, s * 0.3f, sweep)
    }

    /** Monochrome noise added to every pixel, the same for a given [seed]. */
    private fun grain(bitmap: Bitmap, seed: Int, amount: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val random = Random(seed * 7919 + 1)
        for (i in pixels.indices) {
            val n = random.nextInt(-amount, amount + 1)
            val p = pixels[i]
            val r = ((p shr 16 and 0xFF) + n).coerceIn(0, 255)
            val g = ((p shr 8 and 0xFF) + n).coerceIn(0, 255)
            val b = ((p and 0xFF) + n).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
