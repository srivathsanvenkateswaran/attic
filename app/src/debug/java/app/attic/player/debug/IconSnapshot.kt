package app.attic.player.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import java.io.File

/**
 * Debug builds only: the launcher icon as the system masks it (this phone's mask), at 60dp, 48dp and
 * 24dp, then the themed icon built from the monochrome layer the way Pixel's launcher does it
 * (wallpaper tones: a pale accent ground with a dark glyph in light, the reverse in dark). Saves
 * files/icon.png. Works with the phone locked.
 */
object IconSnapshot {
    fun take(context: Context): File {
        val icon = context.packageManager.getApplicationIcon(context.packageName) as AdaptiveIconDrawable
        val res = context.resources
        val themedLight = AdaptiveIconDrawable(
            ColorDrawable(res.getColor(android.R.color.system_accent1_100, null)),
            icon.monochrome!!.constantState!!.newDrawable().mutate().apply { setTint(res.getColor(android.R.color.system_accent1_700, null)); setTintMode(PorterDuff.Mode.SRC_IN) },
        )
        val themedDark = AdaptiveIconDrawable(
            ColorDrawable(res.getColor(android.R.color.system_neutral1_800, null)),
            icon.monochrome!!.constantState!!.newDrawable().mutate().apply { setTint(res.getColor(android.R.color.system_accent1_100, null)); setTintMode(PorterDuff.Mode.SRC_IN) },
        )
        val sizes = listOf(60, 48, 24)
        val gap = dp(context, 16)
        val width = gap + (sizes.sumOf { dp(context, it) } + gap * sizes.size) * 3
        val height = dp(context, 60) + gap * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(0x5A, 0x5F, 0x6A))
        var x = gap
        for (drawable in listOf(icon, themedLight, themedDark)) {
            for (size in sizes) {
                val px = dp(context, size)
                drawable.setBounds(x, gap, x + px, gap + px)
                drawable.draw(canvas)
                x += px + gap
            }
        }
        val file = File(context.filesDir, "icon.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
