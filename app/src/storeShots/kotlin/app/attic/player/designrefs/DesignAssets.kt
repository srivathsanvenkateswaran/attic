package app.attic.player.designrefs

import android.app.Application
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.attic.player.storeshots.DemoCovers
import app.attic.player.storeshots.DemoLibrary
import app.attic.player.storeshots.ShotsDir
import app.attic.player.ui.expressive.AtticMarkIcon
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Slug of an album or screen name, as the design-refs files are named. */
internal fun slug(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

/** Unit tests have no manifest entry for the activity the compose rule launches, so it is added first. */
internal object RegisterDesignActivity : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val info = ActivityInfo().apply {
            name = ComponentActivity::class.java.name
            packageName = context.packageName
            theme = android.R.style.Theme_Material_NoActionBar
        }
        shadowOf(context.packageManager).addOrUpdateActivity(info)
    }
}

/**
 * The generated demo covers as 720px JPEGs and the Headroom mark, for the design files to embed.
 * Written to `<ShotsDir>/covers` and `<ShotsDir>/brand`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi", application = Application::class)
class DesignAssets {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(RegisterDesignActivity).around(compose)

    @Test
    fun covers() {
        val dir = File(ShotsDir, "covers").apply { mkdirs() }
        for (index in 0 until DemoCovers.COUNT) {
            val album = DemoLibrary.library.albums.first { DemoLibrary.coverFor(it.tracks.first().mediaId) == index }
            val small = Bitmap.createScaledBitmap(DemoCovers.get(index), 720, 720, true)
            File(dir, "$index-${slug(album.name)}.jpg").outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            // The full 1000px original too, lossless, for anyone who needs to resample it.
            File(dir, "full").mkdirs()
            File(dir, "full/$index-${slug(album.name)}.png").outputStream().use { DemoCovers.get(index).compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun marks() {
        val dir = File(ShotsDir, "brand").apply { mkdirs() }
        val marks = listOf(
            "mark-on-cream" to (Cream to Terracotta),
            "mark-on-terracotta" to (Terracotta to Cream),
            "mark-terracotta-transparent" to (Color.Transparent to Terracotta),
            "mark-cream-transparent" to (Color.Transparent to Cream),
            "mark-black-transparent" to (Color.Transparent to Color.Black),
        )
        var current by mutableStateOf(marks.first().second)
        compose.setContent {
            val (ground, ink) = current
            // 195dp at 420dpi is 512px.
            Box(Modifier.size(195.dp).background(ground).testTag("mark"), contentAlignment = Alignment.Center) {
                AtticMarkIcon(size = 162.dp, tint = ink)
            }
        }
        for ((name, colours) in marks) {
            current = colours
            compose.waitForIdle()
            compose.onNodeWithTag("mark").captureRoboImage(File(dir, "$name.png").path)
        }
    }

    private companion object {
        val Cream = Color(0xFFF5EDDD)
        val Terracotta = Color(0xFFC6533A)
    }
}
