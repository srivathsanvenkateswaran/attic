package app.attic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.attic.player.ui.AppRoot
import app.attic.player.ui.AtticTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val clear = androidx.compose.ui.graphics.Color.Transparent.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(clear),
            navigationBarStyle = SystemBarStyle.dark(clear),
        )
        setContent {
            AtticTheme {
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
