package app.attic.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.attic.player.data.UiStyle
import app.attic.player.playback.OpenWith
import app.attic.player.playback.PlayFromSearch
import kotlinx.coroutines.launch
import app.attic.player.ui.AppRoot
import app.attic.player.widget.PlayerWidget
import app.attic.player.ui.AtticTheme
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.LocalUiStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        if (attic.settings.uiStyle.value == UiStyle.Comfort) setTheme(R.style.Theme_Attic_Comfort)
        super.onCreate(savedInstanceState)
        // The system draws the splash before any app code runs, so each style's splash is handed over
        // now and used from the next cold start. The system keeps the choice.
        lifecycleScope.launch {
            attic.settings.uiStyle.collect { style ->
                splashScreen.setSplashScreenTheme(
                    if (style == UiStyle.Comfort) R.style.Theme_Attic_Splash_Comfort else R.style.Theme_Attic_Splash_Expressive,
                )
            }
        }
        handleWidgetIntent(intent)
        // Only a fresh launch opens the file: a recreated activity, or one reopened from Recents,
        // gets the same intent again and must not play it a second time.
        if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) {
            OpenWith.handle(attic, intent) || PlayFromSearch.handle(attic, intent)
        }
        setContent {
            val mode by attic.settings.themeMode.collectAsStateWithLifecycle()
            val uiStyle by attic.settings.uiStyle.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalUiStyle provides uiStyle) {
                AtticTheme(mode, uiStyle) {
                    val dark = LocalIsDark.current
                    LaunchedEffect(dark) {
                        val clear = Color.Transparent.toArgb()
                        val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
                        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    }
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
        OpenWith.handle(attic, intent) || PlayFromSearch.handle(attic, intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        intent?.getStringExtra(PlayerWidget.EXTRA_ALBUM_KEY)?.let { attic.requestedAlbum.value = it }
    }

    override fun onStart() {
        super.onStart()
        attic.player.connect()
    }

    override fun onStop() {
        attic.player.disconnect()
        // Android's backup runs while the phone is idle, so the copy it takes is the one from the last time the app was left.
        attic.scope.launch { attic.backup.saveSnapshot() }
        super.onStop()
    }
}
