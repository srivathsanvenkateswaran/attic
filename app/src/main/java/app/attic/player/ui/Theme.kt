package app.attic.player.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.attic.player.R

object Palette {
    val Plum = Color(0xFF16101F)
    val PlumRaised = Color(0xFF231B2F)
    val PlumHigh = Color(0xFF322841)
    val Marigold = Color(0xFFFFB627)
    val Ink = Color(0xFF2A1A00)
    val Paper = Color(0xFFF7F2EC)
    val Mist = Color(0xFFAFA6BC)
}

private fun bricolage(weight: FontWeight) = Font(
    R.font.bricolage,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.width(88f),
        FontVariation.Setting("opsz", 40f),
    ),
)

private fun figtree(weight: FontWeight) = Font(
    R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Display = FontFamily(bricolage(FontWeight.SemiBold), bricolage(FontWeight.Bold), bricolage(FontWeight.ExtraBold))
private val Text = FontFamily(figtree(FontWeight.Normal), figtree(FontWeight.Medium), figtree(FontWeight.SemiBold), figtree(FontWeight.Bold))

private val AtticType = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 39.sp, letterSpacing = (-0.01).em),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Text, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Text, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val AtticColors = darkColorScheme(
    primary = Palette.Marigold,
    onPrimary = Palette.Ink,
    secondary = Palette.Marigold,
    onSecondary = Palette.Ink,
    background = Palette.Plum,
    onBackground = Palette.Paper,
    surface = Palette.Plum,
    onSurface = Palette.Paper,
    surfaceVariant = Palette.PlumRaised,
    onSurfaceVariant = Palette.Mist,
    surfaceContainerLowest = Palette.Plum,
    surfaceContainerLow = Palette.PlumRaised,
    surfaceContainer = Palette.PlumRaised,
    surfaceContainerHigh = Palette.PlumRaised,
    surfaceContainerHighest = Palette.PlumHigh,
    outline = Palette.PlumHigh,
    outlineVariant = Palette.PlumHigh,
    inverseSurface = Palette.Paper,
    inverseOnSurface = Palette.Plum,
)

@Composable
fun AtticTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AtticColors, typography = AtticType) {
        CompositionLocalProvider(LocalContentColor provides AtticColors.onBackground, content = content)
    }
}
