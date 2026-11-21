package app.attic.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicDarkColors
import app.attic.player.ui.classic.ClassicLightColors
import app.attic.player.ui.classic.ClassicType

/** True when the resolved theme is dark. Art-tinted surfaces and scrims read this. */
val LocalIsDark = staticCompositionLocalOf { true }

/** The style every surface draws in. Provided once, at the root, from the user's setting. */
val LocalUiStyle = staticCompositionLocalOf<UiStyle> { error("No UiStyle") }

@Composable
fun AtticTheme(mode: ThemeMode, style: UiStyle, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    // Until the new styles have their own schemes, every style uses Classic's.
    val colors: ColorScheme
    val typography: Typography
    when (style) {
        UiStyle.Expressive, UiStyle.Comfort, UiStyle.Classic -> {
            colors = if (dark) ClassicDarkColors else ClassicLightColors
            typography = ClassicType
        }
    }
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalIsDark provides dark,
            content = content,
        )
    }
}
