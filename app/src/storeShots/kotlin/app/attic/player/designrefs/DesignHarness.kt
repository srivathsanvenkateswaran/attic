package app.attic.player.designrefs

import android.graphics.Bitmap
import android.os.Looper
import android.util.LruCache
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.storeshots.DemoCovers
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.ShotsDir
import app.attic.player.ui.ArtKeyer
import app.attic.player.ui.ArtModel
import app.attic.player.ui.AtticTheme
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.LocalUiStyle
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.currentWindowLayout
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.robolectric.Shadows.shadowOf

/*
 * The store-shot harness (storeshots/ShotHarness.kt) with three additions for the design references:
 * a window taller than the phone for whole-page renders, a hook that runs before the capture (to
 * scroll, say), and a capture of every window, so sheets, dialogs and menus (their own windows) are
 * drawn over the screen they open from. The phone, status bar and gesture bar are the same.
 */

private const val STATUS_BAR_DP = 40
private const val GESTURE_BAR_DP = 24
/** A tablet's bars, as the Tablet pages of the design files draw them. */
private const val TABLET_STATUS_BAR_DP = 24
private const val TABLET_GESTURE_BAR_DP = 20
/** A phone keyboard's height with its suggestion strip and the gesture area. */
private const val KEYBOARD_DP = 300

/** Coil, served from [DemoCovers] instead of MediaStore, on the main thread so a capture never races it. */
private class DemoArtFetcher(private val data: ArtModel) : Fetcher {
    override suspend fun fetch(): FetchResult {
        // A song opened from another app has id -1 and no art, as ArtFetcher finds.
        if (data.mediaId < 0) throw java.io.IOException("No embedded art for ${data.mediaId}")
        val cover = DemoLibrary.coverFor(data.mediaId) ?: error("No demo cover for ${data.mediaId}")
        return ImageFetchResult(DemoCovers.get(cover).asImage(), isSampled = false, dataSource = DataSource.MEMORY)
    }

    class Factory : Fetcher.Factory<ArtModel> {
        override fun create(data: ArtModel, options: Options, imageLoader: ImageLoader) = DemoArtFetcher(data)
    }
}

/** ui/Art.kt's cover-colour cache, filled from the generated covers as the store-shot harness does. */
@Suppress("UNCHECKED_CAST")
internal fun primeCoverColours() {
    val field = Class.forName("app.attic.player.ui.ArtKt").getDeclaredField("artColors").apply { isAccessible = true }
    val cache = field.get(null) as LruCache<Long, Int>
    for (track in DemoLibrary.library.tracks) {
        if (cache.get(track.mediaId) != null) continue
        val cover = DemoCovers.get(DemoLibrary.coverFor(track.mediaId)!!)
        val small = Bitmap.createScaledBitmap(cover, 96, 96, true)
        val palette = Palette.from(small).generate()
        val swatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
        cache.put(track.mediaId, swatch!!.rgb)
    }
}

@OptIn(coil3.annotation.DelicateCoilApi::class)
private fun install(activity: ComponentActivity) {
    SingletonImageLoader.setUnsafe(
        ImageLoader.Builder(activity.applicationContext)
            .components {
                add(ArtKeyer())
                add(DemoArtFetcher.Factory())
            }
            .coroutineContext(Dispatchers.Unconfined)
            .mainCoroutineContext(Dispatchers.Unconfined)
            .interceptorCoroutineContext(Dispatchers.Unconfined)
            .fetcherCoroutineContext(Dispatchers.Unconfined)
            .decoderCoroutineContext(Dispatchers.Unconfined)
            .build(),
    )
    primeCoverColours()
}

/** How the capture is taken. */
enum class Capture {
    /** The activity's window only: every screen without a sheet, dialog or menu. */
    Window,
    /** Every window, composited: for sheets, dialogs and drop-down menus, which Compose puts in windows of their own. */
    Screen,
}

/**
 * Draws [content] in the app's theme for [style] and [dark], [height] px tall (2400 is the phone),
 * with a status bar and gesture bar, runs [settleMs] of frames, calls [prepare], runs [afterMs] more,
 * and writes `<name>.png` to [ShotsDir].
 */
@OptIn(coil3.annotation.ExperimentalCoilApi::class, com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
fun ComposeContentTestRule.shootRef(
    activity: ComponentActivity,
    name: String,
    style: UiStyle,
    dark: Boolean,
    height: Int = 2400,
    width: Int = 1080,
    /** A tablet's status bar and gesture handle are shorter than a phone's, and its clock sits apart from the icons. */
    tablet: Boolean = false,
    capture: Capture = Capture.Window,
    settleMs: Long = 3000,
    afterMs: Long = 1500,
    ime: Boolean = false,
    bars: Boolean = true,
    stitch: Boolean = false,
    prepare: (ComposeContentTestRule.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    mainClock.autoAdvance = false
    activity.window.setLayout(width, height)
    val statusDp = if (tablet) TABLET_STATUS_BAR_DP else STATUS_BAR_DP
    val gestureDp = if (tablet) TABLET_GESTURE_BAR_DP else GESTURE_BAR_DP
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    install(activity)
    var composeView: View? = null
    var withInsets by mutableStateOf(false)
    setContent {
        composeView = LocalView.current
        CompositionLocalProvider(LocalUiStyle provides style, LocalWindowLayout provides currentWindowLayout()) {
            AtticTheme(if (dark) ThemeMode.Dark else ThemeMode.Light, style) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.systemBars))
                    key(withInsets) { if (withInsets) content() }
                    if (bars) SystemBars(LocalIsDark.current, keyboard = ime, statusDp = statusDp, gestureDp = gestureDp)
                }
            }
        }
    }
    mainClock.advanceTimeBy(100)
    val density = activity.resources.displayMetrics.density
    val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, (statusDp * density).toInt(), 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, (gestureDp * density).toInt()))
        .apply {
            if (ime) {
                setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, (KEYBOARD_DP * density).toInt()))
                setVisible(WindowInsetsCompat.Type.ime(), true)
            }
        }
        .build()
    ViewCompat.dispatchApplyWindowInsets(composeView!!, insets)
    mainClock.advanceTimeBy(50)
    withInsets = true
    Snapshot.sendApplyNotifications()
    mainClock.advanceTimeBy(settleMs)
    shadowOf(Looper.getMainLooper()).idle()
    if (prepare != null) {
        prepare()
        mainClock.advanceTimeBy(afterMs)
        shadowOf(Looper.getMainLooper()).idle()
    }
    if (capture == Capture.Screen) {
        // Sheets, dialogs and menus live in windows of their own, laid out a frame later; let them settle.
        repeat(8) {
            mainClock.advanceTimeBy(400)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
    ShotsDir.mkdirs()
    if (stitch) {
        stitchFrames(activity, name, height)
        return
    }
    val file = File(ShotsDir, "$name.png")
    when (capture) {
        Capture.Window -> activity.window.decorView.captureRoboImage(file.path)
        Capture.Screen -> captureScreenRoboImage(file.path)
    }
}

/** A phone's status bar and gesture handle, as storeshots/ShotHarness.kt draws them. */
@Composable
internal fun SystemBars(dark: Boolean, keyboard: Boolean = false, statusDp: Int = STATUS_BAR_DP, gestureDp: Int = GESTURE_BAR_DP) {
    val ink = if (dark) Color.White else Color(0xFF1B1B1F)
    Box(Modifier.fillMaxSize()) {
        if (keyboard) Keyboard(dark, Modifier.align(Alignment.BottomCenter))
        Row(
            Modifier.fillMaxWidth().height(statusDp.dp).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("10:24", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = ink)
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.size(width = 62.dp, height = 14.dp)) {
                val h = size.height
                for (i in 0 until 4) {
                    val barH = h * (0.35f + 0.65f * i / 3f)
                    drawRoundRect(ink, Offset(i * 4.dp.toPx(), h - barH), Size(2.8.dp.toPx(), barH), CornerRadius(1.dp.toPx()))
                }
                val cx = 25.dp.toPx()
                val r = h * 1.15f
                drawArc(ink, 225f, 90f, useCenter = true, topLeft = Offset(cx - r, h - r), size = Size(r * 2, r * 2))
                val bx = 40.dp.toPx()
                val bw = 19.dp.toPx()
                val bh = h * 0.72f
                val top = (h - bh) / 2
                drawRoundRect(ink, Offset(bx, top), Size(bw, bh), CornerRadius(2.5.dp.toPx()), style = Stroke(1.2.dp.toPx()))
                drawRoundRect(ink, Offset(bx + 2.dp.toPx(), top + 2.dp.toPx()), Size((bw - 4.dp.toPx()) * 0.8f, bh - 4.dp.toPx()), CornerRadius(1.dp.toPx()))
                drawRoundRect(ink, Offset(bx + bw + 1.dp.toPx(), h / 2 - 2.dp.toPx()), Size(1.6.dp.toPx(), 4.dp.toPx()), CornerRadius(1.dp.toPx()))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (gestureDp < GESTURE_BAR_DP) 8.dp else 10.dp)
                .width(108.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ink.copy(alpha = 0.85f)),
        )
    }
}

/** A plain stand-in for the system keyboard: Attic does not draw it, but the layout makes room for it. */
@Composable
private fun Keyboard(dark: Boolean, modifier: Modifier) {
    val ground = if (dark) Color(0xFF1F1F22) else Color(0xFFE4E5EA)
    val key = if (dark) Color(0xFF3A3A3F) else Color.White
    Canvas(modifier.fillMaxWidth().height(KEYBOARD_DP.dp).background(ground)) {
        val pad = 6.dp.toPx()
        val top = 44.dp.toPx()
        val rowH = 52.dp.toPx()
        val rows = listOf(10, 9, 9, 5)
        rows.forEachIndexed { r, n ->
            val y = top + r * (rowH + 4.dp.toPx())
            val inset = if (r == 1) size.width / 20 else 0f
            val w = (size.width - 2 * inset - pad * (n + 1)) / (if (r == 3) 10 else n)
            var x = inset + pad
            for (i in 0 until n) {
                val kw = if (r == 3 && i == 2) w * 6 + pad * 5 else w
                drawRoundRect(key, Offset(x, y), Size(kw, rowH - 8.dp.toPx()), CornerRadius(6.dp.toPx()))
                x += kw + pad
            }
        }
    }
}

/** Rows at the top of a scrolled frame that a pinned header may cover, and at the bottom for the gesture bar. */
private const val STITCH_TOP_PX = 330
private const val STITCH_BOTTOM_PX = 80

/**
 * For a page that is sized to the screen and scrolls (Now Playing): captures it at scroll 0, then
 * one screen less the pinned bands at a time down to its end, and writes `<name>.frame<k>.png` with
 * `<name>.frames.json` holding each frame's exact scroll. design_refs_post.py stitches them into
 * `<name>.png`, taking each frame's rows between the pinned header and the gesture bar.
 */
private fun ComposeContentTestRule.stitchFrames(activity: ComponentActivity, name: String, height: Int) {
    val step = (height - STITCH_TOP_PX - STITCH_BOTTOM_PX).toFloat()
    val scrolls = mutableListOf<Int>()
    var k = 0
    while (true) {
        val range = mainScrollRange() ?: break
        scrolls += range.value().toInt()
        activity.window.decorView.captureRoboImage(File(ShotsDir, "$name.frame$k.png").path)
        if (range.value() >= range.maxValue() - 0.5f || k >= 20) break
        scrollMain(step)
        mainClock.advanceTimeBy(1500)
        shadowOf(Looper.getMainLooper()).idle()
        k++
    }
    File(ShotsDir, "$name.frames.json").writeText(
        "{\"scrolls\": ${scrolls.joinToString(", ", "[", "]")}, \"top\": $STITCH_TOP_PX, \"bottom\": $STITCH_BOTTOM_PX}\n",
    )
}
