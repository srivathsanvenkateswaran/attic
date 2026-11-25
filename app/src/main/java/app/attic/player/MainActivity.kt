package app.attic.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
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
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.currentWindowLayout
import app.attic.player.ui.adaptive.locksToPortrait

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        if (attic.settings.uiStyle.value == UiStyle.Comfort) setTheme(R.style.Theme_Attic_Comfort)
        super.onCreate(savedInstanceState)
        lockPhonesToPortrait()
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
            CompositionLocalProvider(LocalUiStyle provides uiStyle, LocalWindowLayout provides currentWindowLayout()) {
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

    /**
     * Space plays and pauses from a keyboard, wherever focus is, unless a text field is taking the
     * typing (the search field gets its space). It is taken before the screen sees it: once a key
     * press has left touch mode, Compose focuses the first control and would treat Space as a click
     * on it. Enter still activates the focused control.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && !event.isCtrlPressed && !event.isAltPressed && !typing()) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) attic.player.togglePlayPause()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** A text field has the input method's attention, so keys are text. */
    private fun typing(): Boolean = getSystemService(InputMethodManager::class.java)?.isAcceptingText == true

    /**
     * Esc does what Back does (closes the player, the queue or lyrics, a sheet, or leaves a screen)
     * when there is something to close; on a tab's root it does nothing rather than leave the app.
     * It only arrives here when nothing on screen took it.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && event.repeatCount == 0 && onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A foldable opened or closed changes the display without recreating the activity.
        lockPhonesToPortrait()
    }

    /**
     * Phones stay in portrait, which every phone screen is drawn for; tablets and unfolded foldables
     * turn freely. Judged on the whole display, not the window, so a small floating window on a tablet
     * doesn't lock it. Android ignores the request in multi-window and, from Android 16, on large screens.
     */
    private fun lockPhonesToPortrait() {
        val bounds = windowManager.maximumWindowMetrics.bounds
        val smallestSideDp = minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density
        val wanted = if (locksToPortrait(smallestSideDp)) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (requestedOrientation != wanted) requestedOrientation = wanted
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
