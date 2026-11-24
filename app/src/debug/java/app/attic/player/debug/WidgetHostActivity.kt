package app.attic.player.debug

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import app.attic.player.widget.PlayerWidget

/**
 * Debug builds only: hosts the player widget at its three sizes (4 x 2, 4 x 1, 2 x 2 as
 * pixel/10-system measures them) on a stand-in wallpaper, so it can be checked on a phone whose
 * launcher adb cannot add widgets to. Light or dark comes from the "night" extra, independent of the
 * phone. The widgets exist only while this screen is open; leaving it deletes them.
 *
 * Needs bind permission once: adb shell appwidget grantbind --package app.attic.player
 * Open: adb shell am start -n app.attic.player/.debug.WidgetHostActivity --ez night true
 */
class WidgetHostActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private val ids = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = intent.getBooleanExtra(EXTRA_NIGHT, false)
        val themed = createConfigurationContext(
            Configuration(resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            },
        )
        host = AppWidgetHost(this, HOST_ID)
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, PlayerWidget::class.java)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(64), dp(16), dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (night) intArrayOf(0xFF0E1A3A.toInt(), 0xFF3A2C7A.toInt(), 0xFF6A4BB0.toInt()) else intArrayOf(0xFFDCE8F2.toInt(), 0xFFE6F0E4.toInt(), 0xFFE9DFF4.toInt()),
            )
        }
        for ((width, height) in SIZES) {
            val id = host.allocateAppWidgetId()
            ids += id
            if (!manager.bindAppWidgetIdIfAllowed(id, provider)) {
                column.addView(TextView(this).apply { text = "Not allowed to bind. Run: adb shell appwidget grantbind --package $packageName" })
                break
            }
            val view = host.createView(themed, id, manager.getAppWidgetInfo(id))
            // The platform pads a host view by about 8dp a side; the sizes above are the widget's own, as
            // the mocks measure it (and as Launcher3 lays it out), so the padding goes.
            view.setPadding(0, 0, 0, 0)
            view.updateAppWidgetSize(Bundle(), listOf(SizeF(width.toFloat(), height.toFloat())))
            column.addView(view, LinearLayout.LayoutParams(dp(width), dp(height)).apply { bottomMargin = dp(16) })
        }
        setContentView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        host.startListening()
        sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids.toIntArray()),
        )
    }

    override fun onDestroy() {
        host.stopListening()
        ids.forEach(host::deleteAppWidgetId)
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private companion object {
        const val HOST_ID = 0xA771C
        const val EXTRA_NIGHT = "night"
        val SIZES = listOf(358 to 201, 358 to 88, 172 to 172)
    }
}
