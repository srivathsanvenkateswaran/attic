package app.attic.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.data.ThemeMode

private fun figtree(weight: FontWeight) = Font(
    R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)

private fun heading(size: Int, line: Int) = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (-0.02).em,
)

private fun body(weight: FontWeight, size: Int, line: Int) =
    TextStyle(fontFamily = Figtree, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val AtticType = Typography(
    displaySmall = heading(36, 40),
    headlineSmall = heading(26, 31),
    titleLarge = heading(22, 27),
    titleMedium = body(FontWeight.Medium, 16, 21),
    titleSmall = body(FontWeight.SemiBold, 14, 18),
    bodyLarge = body(FontWeight.Normal, 16, 22),
    bodyMedium = body(FontWeight.Normal, 14, 19),
    bodySmall = body(FontWeight.Normal, 12, 16),
    labelLarge = body(FontWeight.SemiBold, 14, 18),
    labelMedium = body(FontWeight.Medium, 12, 15),
    labelSmall = body(FontWeight.Medium, 11, 14),
)

private val Green = Color(0xFF1ED760)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    secondary = Green,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF282828),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF727272),
    outlineVariant = Color(0xFF333333),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
)

// Spotify has no light mode to copy, so this mirrors the dark roles. Green darkens so it stays legible on white.
private val LightColors = lightColorScheme(
    primary = Color(0xFF12A84A),
    onPrimary = Color.White,
    secondary = Color(0xFF12A84A),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5E5E5E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFE2E2E2),
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color.White,
)

/** True when the resolved theme is dark. Art-tinted surfaces and scrims read this. */
val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun AtticTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AtticType) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalIsDark provides dark,
            content = content,
        )
    }
}
