package app.attic.player.data

import android.content.Context
import android.os.Build
import app.attic.player.playback.TransitionMode
import app.attic.player.playback.TransitionPlanner
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

/**
 * The moods, genres and languages you picked for mood mixes: [Mood] ids, [Genre.key]s and
 * [LanguageLabels] codes, moods in the order you chose them. [updatedAt] is 0 until you first save.
 */
data class Interests(
    val moods: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val updatedAt: Long = 0,
) {
    val isEmpty: Boolean get() = moods.isEmpty() && genres.isEmpty() && languages.isEmpty()

    /** Never saved: a restore may fill these in. */
    val isUnset: Boolean get() = updatedAt == 0L && isEmpty
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

    private val savedTransitions = TransitionSetting.read(prefs.getString(KEY_TRANSITIONS, null), prefs.getInt(KEY_CROSSFADE_SECONDS, 0))

    private val _transitions = MutableStateFlow(savedTransitions.mode)

    /** What happens between songs. Off by default. */
    val transitions: StateFlow<TransitionMode> = _transitions.asStateFlow()

    fun setTransitions(mode: TransitionMode) {
        prefs.edit().putString(KEY_TRANSITIONS, mode.name).putInt(KEY_CROSSFADE_SECONDS, _crossfadeSeconds.value).apply()
        _transitions.value = mode
    }

    private val _crossfadeSeconds = MutableStateFlow(savedTransitions.seconds)

    /** The crossfade length, 2 to 12 seconds: Crossfade's fade, and Smart's for songs not analysed yet. */
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    fun setCrossfadeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE_SECONDS, seconds).putString(KEY_TRANSITIONS, _transitions.value.name).apply()
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

    private val _interests = MutableStateFlow(readInterests())

    /** What mood mixes follow. Stored here rather than in the database: a few short lists that need no migration. */
    val interests: StateFlow<Interests> = _interests.asStateFlow()

    fun setInterests(interests: Interests) {
        prefs.edit()
            .putString(KEY_INTEREST_MOODS, interests.moods.joinToString(LIST_SEPARATOR))
            .putString(KEY_INTEREST_GENRES, interests.genres.joinToString(LIST_SEPARATOR))
            .putString(KEY_INTEREST_LANGUAGES, interests.languages.joinToString(LIST_SEPARATOR))
            .putLong(KEY_INTERESTS_UPDATED, interests.updatedAt)
            .apply()
        _interests.value = interests
    }

    private fun readInterests(): Interests {
        fun list(key: String) = prefs.getString(key, null).orEmpty().split(LIST_SEPARATOR).filter { it.isNotEmpty() }
        return Interests(list(KEY_INTEREST_MOODS), list(KEY_INTEREST_GENRES), list(KEY_INTEREST_LANGUAGES), prefs.getLong(KEY_INTERESTS_UPDATED, 0))
    }

    private val _moodPromptDismissed = MutableStateFlow(prefs.getBoolean(KEY_MOOD_PROMPT_DISMISSED, false))

    /** True once Home's card asking for picks was answered with "Not now" or the picks were saved. */
    val moodPromptDismissed: StateFlow<Boolean> = _moodPromptDismissed.asStateFlow()

    fun dismissMoodPrompt() {
        prefs.edit().putBoolean(KEY_MOOD_PROMPT_DISMISSED, true).apply()
        _moodPromptDismissed.value = true
    }

    private companion object {
        const val KEY_EQUALISER_ON = "equaliser_on"
        const val KEY_EQUALISER_DEFAULT = "equaliser_default"
        const val KEY_INTEREST_MOODS = "interest_moods"
        const val KEY_INTEREST_GENRES = "interest_genres"
        const val KEY_INTEREST_LANGUAGES = "interest_languages"
        const val KEY_INTERESTS_UPDATED = "interests_updated_at"
        const val KEY_MOOD_PROMPT_DISMISSED = "mood_prompt_dismissed"

        /** No genre name or language code holds it. */
        const val LIST_SEPARATOR = "\u001F"
        const val KEY_THEME = "theme"
        const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        const val KEY_TRANSITIONS = "transitions"
        const val KEY_VOLUME_LEVELLING = "volume_levelling"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_AUTOPLAY = "autoplay"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}

/**
 * The saved transition setting. Before Smart existed there was only `crossfade_seconds`, with 0 for off,
 * so an install that has no `transitions` value reads its mode from that: a length means Crossfade at
 * that length, 0 means Off with the default length.
 */
data class TransitionSetting(val mode: TransitionMode, val seconds: Int) {
    companion object {
        fun read(savedMode: String?, savedSeconds: Int): TransitionSetting {
            val seconds = savedSeconds.takeIf { it in TransitionPlanner.lengthChoices } ?: TransitionPlanner.DEFAULT_LENGTH_SECONDS
            val mode = TransitionMode.entries.firstOrNull { it.name == savedMode }
                ?: if (savedSeconds > 0) TransitionMode.Crossfade else TransitionMode.Off
            return TransitionSetting(mode, seconds)
        }
    }
}
