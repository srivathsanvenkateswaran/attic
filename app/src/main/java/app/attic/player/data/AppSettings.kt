package app.attic.player.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    System("Match phone"),
    Dark("Dark"),
    Light("Light"),
}

/** How the app is drawn. Expressive is Pixel-like, Comfort is One UI-like. */
enum class UiStyle(val label: String) {
    Expressive("Expressive"),
    Comfort("Comfort"),

    /** Today's Spotify-like look, kept only until the new styles replace it. Never offered as a choice. */
    Classic("Classic"),
    ;

    companion object {
        /** The styles a user can pick, in the order they are offered. */
        val choices = listOf(Expressive, Comfort)

        /** A Samsung phone gets the style that matches One UI. Everything else gets Expressive. */
        fun defaultFor(manufacturer: String): UiStyle =
            if (manufacturer.equals("samsung", ignoreCase = true)) Comfort else Expressive

        /** The saved choice, or the phone's default when the user has never chosen one. */
        fun resolve(saved: String?, manufacturer: String): UiStyle =
            entries.firstOrNull { it.name == saved } ?: defaultFor(manufacturer)
    }
}

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.Dark,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    private val _uiStyle = MutableStateFlow(UiStyle.resolve(prefs.getString(KEY_UI_STYLE, null), Build.MANUFACTURER.orEmpty()))
    val uiStyle: StateFlow<UiStyle> = _uiStyle.asStateFlow()

    fun setUiStyle(style: UiStyle) {
        prefs.edit().putString(KEY_UI_STYLE, style.name).apply()
        _uiStyle.value = style
    }

    private val _autoplay = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY, true))

    /** Whether radio carries on from the last song of a queue instead of letting the music stop. */
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    fun setAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
        _autoplay.value = enabled
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_AUTOPLAY = "autoplay"
    }
}
