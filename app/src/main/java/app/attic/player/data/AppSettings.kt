package app.attic.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    System("Match phone"),
    Dark("Dark"),
    Light("Light"),
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

    private val _autoplay = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY, true))

    /** Whether radio carries on from the last song of a queue instead of letting the music stop. */
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    fun setAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
        _autoplay.value = enabled
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_AUTOPLAY = "autoplay"
    }
}
