package app.offbeat.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.offbeat.player.ui.AppRoot
import app.offbeat.player.ui.OffbeatTheme

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
            OffbeatTheme {
                AppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        offbeat.player.connect()
    }

    override fun onStop() {
        offbeat.player.disconnect()
        super.onStop()
    }
}
