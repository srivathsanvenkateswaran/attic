package app.attic.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortShapes
import app.attic.player.ui.comfort.DefaultComfortType
import app.attic.player.ui.comfort.LocalComfortColors
import app.attic.player.ui.comfort.LocalComfortType
import app.attic.player.ui.comfort.comfortColors
import app.attic.player.ui.comfort.toColorScheme
import app.attic.player.ui.expressive.DefaultExpressiveType
import app.attic.player.ui.expressive.ExpressiveShapes
import app.attic.player.ui.expressive.LocalExpressiveType
import app.attic.player.ui.expressive.expressiveColorScheme

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
    val colors: ColorScheme
    val typography: Typography
    val shapes: Shapes
    when (style) {
        UiStyle.Expressive -> {
            colors = expressiveColorScheme(dark)
            typography = DefaultExpressiveType.material
            shapes = ExpressiveShapes.material
        }
        UiStyle.Comfort -> {
            colors = comfortColors(dark).toColorScheme()
            typography = DefaultComfortType.material
            shapes = ComfortShapes.material
        }
    }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalIsDark provides dark,
            LocalExpressiveType provides DefaultExpressiveType,
            LocalComfortColors provides comfortColors(dark),
            LocalComfortType provides DefaultComfortType,
            content = content,
        )
    }
}
