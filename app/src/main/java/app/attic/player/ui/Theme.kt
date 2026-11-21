package app.attic.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.classic.ClassicDarkColors
import app.attic.player.ui.classic.ClassicLightColors
import app.attic.player.ui.classic.ClassicType
import app.attic.player.ui.expressive.DefaultExpressiveType
import app.attic.player.ui.expressive.ExpressiveShapes
import app.attic.player.ui.expressive.LocalExpressiveType
import app.attic.player.ui.expressive.expressiveColorScheme

/** True when the resolved theme is dark. Art-tinted surfaces and scrims read this. */
val LocalIsDark = staticCompositionLocalOf { true }

/** The style every surface draws in. Provided once, at the root, from the user's setting. */
val LocalUiStyle = staticCompositionLocalOf<UiStyle> { error("No UiStyle") }

/** The Material theme the current style set up at the root, so a nested surface can return to it. */
@Immutable
class StyleTheme(val colorScheme: ColorScheme, val typography: Typography, val shapes: Shapes)

val LocalStyleTheme = staticCompositionLocalOf<StyleTheme?> { null }

@Composable
fun AtticTheme(mode: ThemeMode, style: UiStyle, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val colors: ColorScheme
    val typography: Typography
    val shapes: Shapes
    when (style) {
        UiStyle.Expressive -> {
            colors = expressiveColorScheme(dark)
            typography = DefaultExpressiveType.material
            shapes = ExpressiveShapes.material
        }
        // Comfort has no theme of its own yet, so it keeps Classic's.
        UiStyle.Comfort, UiStyle.Classic -> {
            colors = if (dark) ClassicDarkColors else ClassicLightColors
            typography = ClassicType
            shapes = Shapes()
        }
    }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalIsDark provides dark,
            LocalStyleTheme provides StyleTheme(colors, typography, shapes),
            LocalExpressiveType provides DefaultExpressiveType,
            content = content,
        )
    }
}

/**
 * Returns to the style's own theme inside a [app.attic.player.ui.classic.ClassicSurface]. The shell
 * wraps its slots in this while the shell itself is still drawn by Classic, so a screen that already
 * has its own drawing gets its own theme back.
 */
@Composable
fun StyleSurface(content: @Composable () -> Unit) {
    val theme = LocalStyleTheme.current ?: return content()
    MaterialTheme(colorScheme = theme.colorScheme, typography = theme.typography, shapes = theme.shapes) {
        CompositionLocalProvider(LocalContentColor provides theme.colorScheme.onBackground, content = content)
    }
}
