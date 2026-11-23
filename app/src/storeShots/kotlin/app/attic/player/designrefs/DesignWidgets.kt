package app.attic.player.designrefs

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import app.attic.player.storeshots.DemoCovers
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.ShotsDir
import app.attic.player.widget.PlayerWidget
import app.attic.player.widget.WidgetState
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The home-screen widget, built by PlayerWidget's own (private) code and applied as the launcher
 * would apply its RemoteViews: each style's three layouts, light and dark, playing the demo song, and
 * the large layout with nothing played yet. The widget is drawn on a plain wallpaper-coloured ground
 * with 16dp around it, at a typical launcher cell size, and written as `<style>-<theme>-widget_<size>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class DesignWidgets {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Launcher sizes, in dp: a 2 x 2, a 4 x 1 and a 4 x 2 cell block on a Pixel. */
    private val sizes = mapOf("Small" to (170 to 170), "Wide" to (352 to 96), "Large" to (352 to 200))

    @Test
    fun widgets() {
        primeCoverColours()
        val companion = PlayerWidget::class.java.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        val cc = companion.javaClass
        fun nested(name: String) = Class.forName("app.attic.player.widget.PlayerWidget\$$name")
        val layoutCls = nested("Layout")
        val lookCls = nested("Look")
        val artCls = nested("Art")
        val tileCtor = nested("Tile").getDeclaredConstructor(String::class.java, Bitmap::class.java).apply { isAccessible = true }
        val look = cc.getDeclaredMethod("look", Context::class.java, Boolean::class.javaPrimitiveType, java.lang.Long::class.java).apply { isAccessible = true }
        val art = cc.getDeclaredMethod("art", Boolean::class.javaPrimitiveType, Bitmap::class.java, List::class.java).apply { isAccessible = true }
        val build = cc.getDeclaredMethod(
            "build", Context::class.java, Boolean::class.javaPrimitiveType, layoutCls, WidgetState::class.java, artCls, lookCls,
        ).apply { isAccessible = true }

        val now = DemoLibrary.nowPlaying
        val cover = Bitmap.createScaledBitmap(DemoCovers.get(DemoLibrary.coverFor(now.mediaId)!!), 360, 360, true)
        // The albums played last, as recentAlbums() would find them.
        val tiles = listOf("Paper Lanterns", "Iravu Rail", "Chhat Pe Dhoop", "Glasshouse Summer", "Afterglow Protocol").map { name ->
            val album = DemoLibrary.album(name)
            tileCtor.newInstance(album.key, Bitmap.createScaledBitmap(DemoCovers.get(DemoLibrary.coverFor(album.tracks.first().mediaId)!!), 160, 160, true))
        }
        val playing = WidgetState(now.title, now.artist, now.mediaId, isPlaying = true)
        val idle = WidgetState(null, null, null, isPlaying = false)

        val dir = ShotsDir.apply { mkdirs() }
        for (comfort in listOf(false, true)) {
            val style = if (comfort) "comfort" else "expressive"
            for (dark in listOf(false, true)) {
                val theme = if (dark) "dark" else "light"
                val config = Configuration(context.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
                }
                val themed = context.createConfigurationContext(config)
                for ((layoutName, size) in sizes) {
                    val layout = layoutCls.enumConstants.first { (it as Enum<*>).name == layoutName }
                    val cases = buildList {
                        add(layoutName.lowercase() to playing)
                        if (layoutName == "Large") add("large_empty" to idle)
                    }
                    for ((key, state) in cases) {
                        val hasSong = state.mediaId != null
                        val lk = look.invoke(companion, context, comfort, state.mediaId)
                        val ar = art.invoke(companion, comfort, if (hasSong) cover else null, if (hasSong) tiles else emptyList<Any>())
                        val views = build.invoke(companion, context, comfort, layout, state, ar, lk) as android.widget.RemoteViews
                        val file = File(dir, "$style-$theme-widget_$key.png")
                        draw(themed, views, size, dark, file)
                    }
                }
            }
        }
    }

    private fun draw(context: Context, views: android.widget.RemoteViews, size: Pair<Int, Int>, dark: Boolean, file: File) {
        val density = context.resources.displayMetrics.density
        val w = (size.first * density).toInt()
        val h = (size.second * density).toInt()
        val pad = (16 * density).toInt()
        val parent = FrameLayout(context)
        val view = views.apply(context, parent)
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (dark) 0xFF2E3138.toInt() else 0xFFC9CED6.toInt())
        canvas.translate(pad.toFloat(), pad.toFloat())
        view.draw(canvas)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
