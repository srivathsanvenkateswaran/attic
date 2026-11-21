package app.attic.player.ui.expressive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import app.attic.player.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Google Sans Flex, bundled as one variable font (res/font/google_sans_flex.ttf) with its weight
 * (300–800), width (50–100), roundness (ROND 0–100) and optical size (12–48) axes.
 *
 * Compose picks a font by weight only, so every other axis setting is its own [FontFamily]. [family]
 * builds and caches them. Optical size is set per text style, close to the style's size, because
 * Android does not apply it automatically the way a browser does.
 */
object GoogleSansFlex {
    /** The settings one family pins. Weight is chosen per text style from the family's members. */
    data class Axes(val opticalSize: Int, val round: Boolean, val width: Int = 100)

    private val families = ConcurrentHashMap<Axes, FontFamily>()
    private val axesOf = ConcurrentHashMap<FontFamily, Axes>()
    private val weights = listOf(300, 400, 500, 600, 700, 800)

    fun family(axes: Axes): FontFamily = families.getOrPut(axes) {
        FontFamily(
            weights.map { weight ->
                Font(
                    R.font.google_sans_flex,
                    weight = FontWeight(weight),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(weight),
                        FontVariation.width(axes.width.toFloat()),
                        FontVariation.Setting("ROND", if (axes.round) 100f else 0f),
                        FontVariation.Setting("opsz", axes.opticalSize.toFloat()),
                    ),
                )
            },
        ).also { axesOf[it] = axes }
    }

    /** The axes a family made by [family] pins, or null for any other family. */
    fun axesOf(family: FontFamily?): Axes? = family?.let { axesOf[it] }

    /** Optical sizes in use, so a few families cover every style. */
    fun opticalSizeFor(fontSizeSp: Float): Int = when {
        fontSizeSp <= 12f -> 12
        fontSizeSp <= 14f -> 14
        fontSizeSp <= 18f -> 16
        fontSizeSp <= 24f -> 22
        fontSizeSp <= 30f -> 28
        fontSizeSp <= 40f -> 36
        else -> 45
    }
}

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0, round: Boolean = false) = TextStyle(
    fontFamily = GoogleSansFlex.family(GoogleSansFlex.Axes(GoogleSansFlex.opticalSizeFor(size.toFloat()), round)),
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/**
 * The kit's type scale (`.t-display` 45/52 … `.t-label-s` 11/16) and each style's emphasised partner
 * (`.em`: heavier and fully rounded). Material 3 1.4 keeps its own emphasised styles internal, so they
 * live here: read them with `Expressive.type.titleLargeEmphasized` and the plain ones from
 * `MaterialTheme.typography` as usual.
 */
@Immutable
class ExpressiveTypography(
    val displayLarge: TextStyle = style(57, 64, 400, -0.25),
    val displayMedium: TextStyle = style(45, 52, 400, -0.5),
    val displaySmall: TextStyle = style(36, 44, 400, -0.3),
    val headlineLarge: TextStyle = style(32, 40, 400),
    val headlineMedium: TextStyle = style(28, 36, 400),
    val headlineSmall: TextStyle = style(24, 32, 400),
    val titleLarge: TextStyle = style(22, 28, 400),
    val titleMedium: TextStyle = style(16, 24, 500, 0.1),
    val titleSmall: TextStyle = style(14, 20, 500, 0.1),
    val bodyLarge: TextStyle = style(16, 24, 400),
    val bodyMedium: TextStyle = style(14, 20, 400),
    val bodySmall: TextStyle = style(12, 16, 400),
    val labelLarge: TextStyle = style(14, 20, 500),
    val labelMedium: TextStyle = style(12, 16, 500),
    val labelSmall: TextStyle = style(11, 16, 500),
    val displayLargeEmphasized: TextStyle = style(57, 64, 700, -0.25, round = true),
    val displayMediumEmphasized: TextStyle = style(45, 52, 700, -0.5, round = true),
    val displaySmallEmphasized: TextStyle = style(36, 44, 700, -0.3, round = true),
    val headlineLargeEmphasized: TextStyle = style(32, 40, 700, round = true),
    val headlineMediumEmphasized: TextStyle = style(28, 36, 700, round = true),
    val headlineSmallEmphasized: TextStyle = style(24, 32, 700, round = true),
    val titleLargeEmphasized: TextStyle = style(22, 28, 700, round = true),
    val titleMediumEmphasized: TextStyle = style(16, 24, 600, 0.1, round = true),
    val titleSmallEmphasized: TextStyle = style(14, 20, 600, 0.1, round = true),
    val bodyLargeEmphasized: TextStyle = style(16, 24, 600, round = true),
    val bodyMediumEmphasized: TextStyle = style(14, 20, 600, round = true),
    val bodySmallEmphasized: TextStyle = style(12, 16, 700, round = true),
    val labelLargeEmphasized: TextStyle = style(14, 20, 600, round = true),
    val labelMediumEmphasized: TextStyle = style(12, 16, 600, round = true),
    val labelSmallEmphasized: TextStyle = style(11, 16, 700, round = true),
) {
    /** The plain styles as a Material [Typography], for `MaterialTheme` and every stock component. */
    val material: Typography = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )
}

/** The emphasised partner of any style from this kit: heavier and fully rounded. */
fun TextStyle.emphasized(weight: FontWeight = FontWeight.Bold): TextStyle {
    val axes = GoogleSansFlex.axesOf(fontFamily) ?: return copy(fontWeight = weight)
    return copy(fontFamily = GoogleSansFlex.family(axes.copy(round = true)), fontWeight = weight)
}

/** This style on the width axis: 100 is normal, 75 and 50 are the kit's `.w75` and `.w50`. */
fun TextStyle.narrowed(width: Int): TextStyle {
    val axes = GoogleSansFlex.axesOf(fontFamily) ?: return this
    return copy(fontFamily = GoogleSansFlex.family(axes.copy(width = width)))
}

/**
 * A title that narrows before it truncates: it is set at full width if it fits in [maxLines], else at
 * 75, else at 50, and only then cut with an ellipsis. The kit's `.w75` / `.w50`, decided by measuring.
 */
@Composable
fun NarrowingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
    widths: List<Int> = listOf(100, 75, 50),
) {
    val measurer = rememberTextMeasurer(cacheSize = 8)
    BoxWithConstraints(modifier) {
        val maxWidth = constraints.maxWidth
        val chosen = remember(text, style, maxWidth, maxLines, widths) {
            widths.firstOrNull { width ->
                val result = measurer.measure(
                    text,
                    style.narrowed(width),
                    maxLines = maxLines,
                    constraints = Constraints(maxWidth = maxWidth),
                )
                !result.hasVisualOverflow
            } ?: widths.last()
        }
        Text(
            text = text,
            style = style.narrowed(chosen),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }
}
