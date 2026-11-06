package app.attic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.attic.player.ui.AppRoot
import app.attic.player.ui.AtticTheme
import app.attic.player.ui.LocalIsDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by attic.settings.themeMode.collectAsStateWithLifecycle()
            AtticTheme(mode) {
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

    override fun onStart() {
        super.onStart()
        attic.player.connect()
    }

    override fun onStop() {
        attic.player.disconnect()
        super.onStop()
    }
}
