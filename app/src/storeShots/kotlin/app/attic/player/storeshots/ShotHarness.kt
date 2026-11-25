package app.attic.player.storeshots

import android.graphics.Bitmap
import android.os.Looper
import android.util.LruCache
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import app.attic.player.ui.ArtKeyer
import app.attic.player.ui.ArtModel
import app.attic.player.ui.AtticTheme
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.LocalUiStyle
import com.github.takahirom.roborazzi.captureRoboImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.robolectric.Shadows.shadowOf

/** Where the raw captures go. Set by app/build.gradle.kts from -PstoreShotsOut. */
val ShotsDir: File = File(System.getProperty("storeShots.out") ?: "/tmp/attic-dev/store-shots/raw")

/** The phone the shots are taken on: 1080 by 2400 at 420 dpi. Status bar and gesture area in dp. */
private const val STATUS_BAR_DP = 40
private const val GESTURE_BAR_DP = 24

/** Coil, served from [DemoCovers] instead of MediaStore, on the main thread so a capture never races it. */
private class DemoArtFetcher(private val data: ArtModel) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val cover = DemoLibrary.coverFor(data.mediaId) ?: error("No demo cover for ${data.mediaId}")
        // A copy each time: Comfort's SuperellipseMask recycles the bitmap it is handed, as it may with
        // ArtFetcher's thumbnails, and the cached original has to survive for the next cover.
        val bitmap = DemoCovers.get(cover).let { it.copy(it.config ?: Bitmap.Config.ARGB_8888, false) }
        return ImageFetchResult(bitmap.asImage(), isSampled = false, dataSource = DataSource.MEMORY)
    }

    class Factory : Fetcher.Factory<ArtModel> {
        override fun create(data: ArtModel, options: Options, imageLoader: ImageLoader) = DemoArtFetcher(data)
    }
}

/**
 * ui/Art.kt keeps each cover's colour in a private cache and reads it from MediaStore on a miss. The
 * tint the screens take on is filled in here, as Art.kt would compute it, so the first frame already
 * has it and nothing has to load in the background.
 */
@Suppress("UNCHECKED_CAST")
private fun primeCoverColours() {
    val field = Class.forName("app.attic.player.ui.ArtKt").getDeclaredField("artColors").apply { isAccessible = true }
    val cache = field.get(null) as LruCache<Long, Int>
    for (track in DemoLibrary.library.tracks) {
        val cover = DemoCovers.get(DemoLibrary.coverFor(track.mediaId)!!)
        val small = Bitmap.createScaledBitmap(cover, 96, 96, true)
        val palette = Palette.from(small).generate()
        val swatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
        cache.put(track.mediaId, swatch!!.rgb)
    }
}

private var installed = false

private fun install(activity: ComponentActivity) {
    if (installed) return
    installed = true
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

/**
 * Draws [content] in the app's theme for [style] and [dark], full screen with a status bar and a
 * gesture bar, runs [settleMs] of frames on the rule's clock, runs [prepare] (a scroll, say), and writes `<name>.png` to [ShotsDir].
 * The clock only moves when told, so looping animations (a marquee, the playing bars) don't hold it up.
 */
fun ComposeContentTestRule.shoot(
    activity: ComponentActivity,
    name: String,
    style: UiStyle,
    dark: Boolean,
    settleMs: Long = 3000,
    prepare: ComposeContentTestRule.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    mainClock.autoAdvance = false
    // The phone's exact pixels. Qualifiers alone round 411dp at 2.625 down to 1078 by 2399.
    activity.window.setLayout(1080, 2400)
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    install(activity)
    var composeView: View? = null
    // Robolectric's window has no system bars, so the insets are handed in after the first frame. The
    // screen is then drawn afresh, so it sees them from its first layout, as it would on a phone.
    var withInsets by mutableStateOf(false)
    setContent {
        composeView = LocalView.current
        CompositionLocalProvider(LocalUiStyle provides style) {
            AtticTheme(if (dark) ThemeMode.Dark else ThemeMode.Light, style) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Reading the insets keeps Compose listening for them before the screen is drawn.
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.systemBars))
                    key(withInsets) { if (withInsets) content() }
                    SystemBars(LocalIsDark.current)
                }
            }
        }
    }
    mainClock.advanceTimeBy(100)
    val density = activity.resources.displayMetrics.density
    val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, (STATUS_BAR_DP * density).toInt(), 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, (GESTURE_BAR_DP * density).toInt()))
        .build()
    ViewCompat.dispatchApplyWindowInsets(composeView!!, insets)
    mainClock.advanceTimeBy(50)
    withInsets = true
    Snapshot.sendApplyNotifications()
    mainClock.advanceTimeBy(settleMs)
    shadowOf(Looper.getMainLooper()).idle()
    prepare()
    mainClock.advanceTimeBy(1000)
    shadowOf(Looper.getMainLooper()).idle()
    ShotsDir.mkdirs()
    activity.window.decorView.captureRoboImage(File(ShotsDir, "$name.png").path)
}

/** A phone's status bar (time, signal, wifi, battery) and gesture handle, in ink that suits the theme. */
@Composable
private fun SystemBars(dark: Boolean) {
    val ink = if (dark) Color.White else Color(0xFF1B1B1F)
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(STATUS_BAR_DP.dp).padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("10:24", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = ink)
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.size(width = 62.dp, height = 14.dp)) {
                val h = size.height
                // Signal: four rising bars.
                for (i in 0 until 4) {
                    val barH = h * (0.35f + 0.65f * i / 3f)
                    drawRoundRect(ink, Offset(i * 4.dp.toPx(), h - barH), Size(2.8.dp.toPx(), barH), CornerRadius(1.dp.toPx()))
                }
                // Wifi: a quarter-circle fan pointing down.
                val cx = 25.dp.toPx()
                val r = h * 1.15f
                drawArc(ink, 225f, 90f, useCenter = true, topLeft = Offset(cx - r, h - r), size = Size(r * 2, r * 2))
                // Battery: outline, fill at about 80%, and the nub.
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
                .padding(bottom = 10.dp)
                .width(108.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ink.copy(alpha = 0.85f)),
        )
    }
}
