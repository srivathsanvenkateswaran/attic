package app.attic.player.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { System, Dark, Light }

/** How the app is drawn. Expressive is Pixel-like, Comfort is One UI-like. */
enum class UiStyle {
    Expressive,
    Comfort;

    companion object {
        /** The styles a user can pick, in the order they are offered. */
        val choices = listOf(Expressive, Comfort)

        /** A Samsung phone gets the style that matches One UI. Everything else gets Expressive. */
        fun defaultFor(manufacturer: String): UiStyle =
            if (manufacturer.equals("samsung", ignoreCase = true)) Comfort else Expressive

        /**
         * The saved choice, or the phone's default when there is none or it names a style that is
         * gone (such as "Classic").
         */
        fun resolve(saved: String?, manufacturer: String): UiStyle =
            entries.firstOrNull { it.name == saved } ?: defaultFor(manufacturer)
    }
}

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /**
     * True when settings.xml held any value other than the first-run flag when the app started, read
     * before anything could write to it. An older install that changed any setting has this.
     */
    val hadValuesAtStart: Boolean = prefs.all.keys.any { it != KEY_ONBOARDING_DONE }

    /** The saved first-run decision: true once onboarded, false while the first run is unfinished, null if never decided. */
    val onboardingDone: Boolean?
        get() = if (prefs.contains(KEY_ONBOARDING_DONE)) prefs.getBoolean(KEY_ONBOARDING_DONE, false) else null

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

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

    private val _volumeLevelling = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_LEVELLING, true))

    /** Whether songs play at a matched loudness, from their tags or the level analysis measured. */
    val volumeLevelling: StateFlow<Boolean> = _volumeLevelling.asStateFlow()

    fun setVolumeLevelling(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOLUME_LEVELLING, enabled).apply()
        _volumeLevelling.value = enabled
    }

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt(KEY_CROSSFADE_SECONDS, 0))

    /** How long one song fades into the next, in seconds. 0 is off, the default. */
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    fun setCrossfadeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE_SECONDS, seconds).apply()
        _crossfadeSeconds.value = seconds
    }

    private val _equaliserOn = MutableStateFlow(prefs.getBoolean(KEY_EQUALISER_ON, false))

    /** The equaliser's own switch. Off, nothing it holds is applied, the default or any song's. */
    val equaliserOn: StateFlow<Boolean> = _equaliserOn.asStateFlow()

    fun setEqualiserOn(on: Boolean) {
        if (on == _equaliserOn.value) return
        prefs.edit().putBoolean(KEY_EQUALISER_ON, on).apply()
        _equaliserOn.value = on
    }

    private val _equaliserDefault = MutableStateFlow(prefs.getString(KEY_EQUALISER_DEFAULT, null).orEmpty())

    /**
     * The curve songs without one of their own play with, as [app.attic.player.playback.eq.EqCurve.encode]
     * writes it. Empty is flat.
     */
    val equaliserDefault: StateFlow<String> = _equaliserDefault.asStateFlow()

    fun setEqualiserDefault(gains: String) {
        if (gains == _equaliserDefault.value) return
        prefs.edit().putString(KEY_EQUALISER_DEFAULT, gains).apply()
        _equaliserDefault.value = gains
    }

    private companion object {
        const val KEY_EQUALISER_ON = "equaliser_on"
        const val KEY_EQUALISER_DEFAULT = "equaliser_default"
        const val KEY_THEME = "theme"
        const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        const val KEY_VOLUME_LEVELLING = "volume_levelling"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_AUTOPLAY = "autoplay"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
