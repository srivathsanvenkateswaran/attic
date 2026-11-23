package app.attic.player.ui.comfort

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.variableFont

/**
 * Onest (OFL 1.1), bundled as res/font/onest.ttf: the variable font from google/fonts, instanced to
 * weight 400–800 and subset to Latin (third_party/fonts-NOTICE.txt). One [Font] per weight the kit
 * uses, each pinning the weight axis, so `FontWeight(600)` is a true semibold.
 */
val Onest: FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        variableFont(R.font.onest, FontWeight(weight), FontVariation.Settings(FontVariation.weight(weight)))
    },
)

/** CSS centres text in its line box; this does the same, so a 16/22 row title sits where the mock has it. */
private val CssLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/**
 * A text style in Onest: [size] and [line] in sp, [weight], and [tracking] in em as kit.css writes it
 * (`letter-spacing:-.02em` is `tracking = -0.02`).
 */
fun onest(size: Float, line: Float, weight: Int = 400, tracking: Float = 0f): TextStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (tracking * size).sp,
    lineHeightStyle = CssLineHeight,
)

/** Tabular figures, for times and counts (`font-variant-numeric:tabular-nums`). */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * The kit's type scale (oneui/KIT.md "Type", 00-direction.html's specimen). Component functions set
 * their own styles; reach for these when a screen draws text of its own.
 */
@Immutable
class ComfortTypography(
    /** `.hero h1`: 40/46 bold, the expanded large title. */
    val hero: TextStyle = onest(40f, 46f, 700, -0.025f),
    /** `.c-head h1`, `.np-title h1`: 26/32 bold, song and collection titles. */
    val title: TextStyle = onest(26f, 32f, 700, -0.02f),
    /** `.c-head.person h1`: 30/36 bold, an artist's name. */
    val personTitle: TextStyle = onest(30f, 36f, 700, -0.02f),
    /** `.bar h2`, `.sheet-title`, `.dialog h3`, `.empty h3`: 20/26 bold. */
    val barTitle: TextStyle = onest(20f, 26f, 700, -0.015f),
    /** `.txt b`: 16/22 medium, row titles. */
    val rowTitle: TextStyle = onest(16f, 22f, 500),
    /** `.row.lg .txt b`: 18/24 bold. */
    val rowTitleLarge: TextStyle = onest(18f, 24f, 700, -0.01f),
    /** Dialog and empty-state text: 15/21. */
    val body: TextStyle = onest(15f, 21f),
    /** `.txt span`: 13.5/18, a row's second line. */
    val sub: TextStyle = onest(13.5f, 18f),
    /** `.txt em`, `.kv em`, tile second lines: 12.5/16. */
    val caption: TextStyle = onest(12.5f, 16f),
    /** `.label`, `.lyrics-card .hd`: 14/18 semibold, section labels. */
    val label: TextStyle = onest(14f, 18f, 600),
    /** `.btn`: 15.5/20 semibold. */
    val button: TextStyle = onest(15.5f, 20f, 600),
    /** `.stat b`: 26/32 bold, tabular. */
    val stat: TextStyle = onest(26f, 32f, 700, -0.02f).tabular(),
    /** `.lyrics-card .ln`: 22/28 bold. */
    val lyric: TextStyle = onest(22f, 28f, 700, -0.015f),
) {
    /** The scale as a Material [Typography], for stock components (dialogs, menus, text fields). */
    val material: Typography = Typography(
        displayLarge = hero,
        displayMedium = hero,
        displaySmall = personTitle,
        headlineLarge = personTitle,
        headlineMedium = title,
        headlineSmall = lyric,
        titleLarge = barTitle,
        titleMedium = rowTitle,
        titleSmall = label,
        bodyLarge = body,
        bodyMedium = sub,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = onest(14f, 18f, 500),
        labelSmall = onest(12f, 15f, 500),
    )
}

internal val DefaultComfortType by lazy { ComfortTypography() }

val LocalComfortType = staticCompositionLocalOf { DefaultComfortType }
