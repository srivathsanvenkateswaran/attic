package app.attic.player.debug

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import app.attic.player.widget.PlayerWidget
import java.io.File

/**
 * Debug builds only: applies the widget's views at its three sizes (4 x 2, 4 x 1, 2 x 2 as
 * pixel/10-system measures them) the way a launcher does, in light or dark, and saves the picture to
 * files/widget-<name>.png. Nothing is placed on a home screen, so it works with the phone locked.
 */
object WidgetSnapshot {
    private val SIZES = listOf(358 to 201, 358 to 88, 172 to 172)

    suspend fun take(context: Context, night: Boolean, name: String): File {
        val themed = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            },
        )
        val remote = PlayerWidget.views(context)
        val pad = dp(context, 16)
        val width = dp(context, 358) + pad * 2
        val height = SIZES.sumOf { dp(context, it.second) } + pad * (SIZES.size + 1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (night) intArrayOf(0xFF0E1A3A.toInt(), 0xFF3A2C7A.toInt(), 0xFF6A4BB0.toInt()) else intArrayOf(0xFFDCE8F2.toInt(), 0xFFE6F0E4.toInt(), 0xFFE9DFF4.toInt()),
        ).apply { setBounds(0, 0, width, height) }.draw(canvas)
        var y = pad
        for ((w, h) in SIZES) {
            // The layout the launcher would pick: the largest whose smallest size fits.
            val layout = remote.filterKeys { it.width <= w && it.height <= h }.maxBy { it.key.width * it.key.height }.value
            val view = layout.apply(themed, FrameLayout(themed))
            val pw = dp(context, w)
            val ph = dp(context, h)
            view.measure(View.MeasureSpec.makeMeasureSpec(pw, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(ph, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, pw, ph)
            canvas.save()
            canvas.translate(pad.toFloat(), y.toFloat())
            view.draw(canvas)
            canvas.restore()
            y += ph + pad
        }
        val file = File(context.filesDir, "widget-$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
