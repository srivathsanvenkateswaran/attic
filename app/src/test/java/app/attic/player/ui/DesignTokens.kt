package app.attic.player.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.comfort.ComfortColors
import app.attic.player.ui.comfort.ComfortDark
import app.attic.player.ui.comfort.ComfortLight
import app.attic.player.ui.comfort.ComfortShapes
import app.attic.player.ui.comfort.ComfortTypography
import app.attic.player.ui.comfort.TintSpec
import app.attic.player.ui.expressive.ExpressiveMotion
import app.attic.player.ui.expressive.ExpressiveShapes
import app.attic.player.ui.expressive.ExpressiveTypography
import app.attic.player.ui.expressive.GoogleSansFlex
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Reads Attic's design tokens out of the Kotlin that defines them, for docs/design-system/tokens.json
 * and the checks on docs/design-system/tokens.md. Nothing here holds a value of its own: every number
 * comes from the theme, type, shape and motion code, or from res/values*\/colors.xml.
 *
 * The lists of names are written out so the JSON keeps a stable order; [DesignTokensTest] checks by
 * reflection that each list still covers every property, so a new token fails the test until it is
 * added here and documented.
 */
internal object DesignTokens {

    val comfortColorNames = listOf(
        "ground", "card", "raised", "fill", "fill2", "line", "edge", "on", "on2", "on3", "accent", "onAccent",
        "accentInk", "accentSoft", "onAccentSoft", "danger", "onArt", "knob", "toast", "onToast", "scrim",
        "shadow", "shadowSmall", "miniBase", "miniFill", "miniInk2",
    )

    fun comfortColor(colors: ComfortColors, name: String): Color = when (name) {
        "ground" -> colors.ground
        "card" -> colors.card
        "raised" -> colors.raised
        "fill" -> colors.fill
        "fill2" -> colors.fill2
        "line" -> colors.line
        "edge" -> colors.edge
        "on" -> colors.on
        "on2" -> colors.on2
        "on3" -> colors.on3
        "accent" -> colors.accent
        "onAccent" -> colors.onAccent
        "accentInk" -> colors.accentInk
        "accentSoft" -> colors.accentSoft
        "onAccentSoft" -> colors.onAccentSoft
        "danger" -> colors.danger
        "onArt" -> colors.onArt
        "knob" -> colors.knob
        "toast" -> colors.toast
        "onToast" -> colors.onToast
        "scrim" -> colors.scrim
        "shadow" -> colors.shadow
        "shadowSmall" -> colors.shadowSmall
        "miniBase" -> colors.miniBase
        "miniFill" -> colors.miniFill
        "miniInk2" -> colors.miniInk2
        else -> error("Unknown Comfort colour $name")
    }

    fun comfortTypeStyles(t: ComfortTypography = ComfortTypography()): List<Pair<String, TextStyle>> = listOf(
        "hero" to t.hero, "title" to t.title, "personTitle" to t.personTitle, "barTitle" to t.barTitle,
        "rowTitle" to t.rowTitle, "rowTitleLarge" to t.rowTitleLarge, "body" to t.body, "sub" to t.sub,
        "caption" to t.caption, "label" to t.label, "button" to t.button, "stat" to t.stat, "lyric" to t.lyric,
    )

    fun expressiveTypeStyles(t: ExpressiveTypography = ExpressiveTypography()): List<Pair<String, TextStyle>> = listOf(
        "displayLarge" to t.displayLarge, "displayMedium" to t.displayMedium, "displaySmall" to t.displaySmall,
        "headlineLarge" to t.headlineLarge, "headlineMedium" to t.headlineMedium, "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge, "titleMedium" to t.titleMedium, "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge, "bodyMedium" to t.bodyMedium, "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge, "labelMedium" to t.labelMedium, "labelSmall" to t.labelSmall,
        "displayLargeEmphasized" to t.displayLargeEmphasized, "displayMediumEmphasized" to t.displayMediumEmphasized,
        "displaySmallEmphasized" to t.displaySmallEmphasized, "headlineLargeEmphasized" to t.headlineLargeEmphasized,
        "headlineMediumEmphasized" to t.headlineMediumEmphasized, "headlineSmallEmphasized" to t.headlineSmallEmphasized,
        "titleLargeEmphasized" to t.titleLargeEmphasized, "titleMediumEmphasized" to t.titleMediumEmphasized,
        "titleSmallEmphasized" to t.titleSmallEmphasized, "bodyLargeEmphasized" to t.bodyLargeEmphasized,
        "bodyMediumEmphasized" to t.bodyMediumEmphasized, "bodySmallEmphasized" to t.bodySmallEmphasized,
        "labelLargeEmphasized" to t.labelLargeEmphasized, "labelMediumEmphasized" to t.labelMediumEmphasized,
        "labelSmallEmphasized" to t.labelSmallEmphasized,
    )

    val comfortRadii: List<Pair<String, Dp>> = listOf(
        "FloatRadius" to ComfortShapes.FloatRadius,
        "GroupRadius" to ComfortShapes.GroupRadius,
        "ControlRadius" to ComfortShapes.ControlRadius,
        "MenuRadius" to ComfortShapes.MenuRadius,
        "LargeArtFrom" to ComfortShapes.LargeArtFrom,
    )

    val comfortSuperellipses: List<Pair<String, Double>> = listOf(
        "Squircle" to ComfortShapes.Squircle.exponent,
        "SquircleLarge" to ComfortShapes.SquircleLarge.exponent,
        "SquircleSoft" to ComfortShapes.SquircleSoft.exponent,
    )

    val expressiveCorners: List<Pair<String, Dp>> = listOf(
        "ExtraSmall" to ExpressiveShapes.ExtraSmall,
        "Small" to ExpressiveShapes.Small,
        "Medium" to ExpressiveShapes.Medium,
        "Large" to ExpressiveShapes.Large,
        "LargeIncreased" to ExpressiveShapes.LargeIncreased,
        "ExtraLarge" to ExpressiveShapes.ExtraLarge,
        "ExtraLargeIncreased" to ExpressiveShapes.ExtraLargeIncreased,
        "ExtraExtraLarge" to ExpressiveShapes.ExtraExtraLarge,
        "GroupGap" to ExpressiveShapes.GroupGap,
        "GroupSpacing" to ExpressiveShapes.GroupSpacing,
    )

    val expressiveMotion: List<Pair<String, SpringSpec<Float>>> = listOf(
        "fastSpatial" to ExpressiveMotion.fastSpatial<Float>() as SpringSpec<Float>,
        "defaultSpatial" to ExpressiveMotion.defaultSpatial<Float>() as SpringSpec<Float>,
        "slowSpatial" to ExpressiveMotion.slowSpatial<Float>() as SpringSpec<Float>,
        "fastEffects" to ExpressiveMotion.fastEffects<Float>() as SpringSpec<Float>,
        "defaultEffects" to ExpressiveMotion.defaultEffects<Float>() as SpringSpec<Float>,
        "slowEffects" to ExpressiveMotion.slowEffects<Float>() as SpringSpec<Float>,
    )

    val tintMillis: Int get() = (TintSpec as TweenSpec<Color>).durationMillis

    // ---- Formatting, shared with the tokens.md checks ----

    /** `#RRGGBB`, or `#RRGGBBAA` when the colour is not opaque, with the alpha byte Compose packs. */
    fun hex(color: Color): String {
        val argb = color.toArgb()
        val rgb = "%06X".format(argb and 0xFFFFFF)
        val alpha = argb ushr 24
        return if (alpha == 0xFF) "#$rgb" else "#$rgb%02X".format(alpha)
    }

    /** A number as the code writes it: no trailing `.0`, no float noise. */
    fun num(value: Double): String =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().let {
            if (it == "-0") "0" else it
        }

    fun num(value: Float): String = num(value.toDouble())

    /** Comfort writes tracking in em, as kit.css does; the TextStyle holds it in sp (em × size). */
    fun comfortTrackingEm(style: TextStyle): Double =
        if (style.letterSpacing.value == 0f) 0.0 else style.letterSpacing.value.toDouble() / style.fontSize.value

    // ---- The JSON tree ----

    fun tree(resDir: File): Map<String, Any?> = linkedMapOf(
        "_generated" to "From the Kotlin sources and res/values*/colors.xml by DesignTokensTest. " +
            "Do not edit by hand; run scripts/design_tokens.sh.",
        "comfort" to linkedMapOf(
            "color" to linkedMapOf(
                "dark" to comfortColorNames.associateWithTo(linkedMapOf()) { hex(comfortColor(ComfortDark, it)) },
                "light" to comfortColorNames.associateWithTo(linkedMapOf()) { hex(comfortColor(ComfortLight, it)) },
            ),
            "tintAnimationMs" to tintMillis,
            "type" to linkedMapOf(
                "font" to "Onest",
                "styles" to comfortTypeStyles().associateTo(linkedMapOf()) { (name, style) ->
                    name to linkedMapOf(
                        "size" to Raw(num(style.fontSize.value)),
                        "line" to Raw(num(style.lineHeight.value)),
                        "weight" to style.fontWeight!!.weight,
                        "trackingEm" to Raw(num(comfortTrackingEm(style))),
                        "tabular" to (style.fontFeatureSettings == "tnum"),
                    )
                },
            ),
            "shape" to linkedMapOf(
                "radiusDp" to comfortRadii.associateTo(linkedMapOf()) { (name, dp) -> name to Raw(num(dp.value)) },
                "superellipseExponent" to comfortSuperellipses.associateTo(linkedMapOf()) { (name, n) -> name to Raw(num(n)) },
                "materialDp" to materialRadii(ComfortShapes.material),
            ),
        ),
        "expressive" to linkedMapOf(
            "color" to "The wallpaper scheme (dynamic colour) on app surfaces, and a scheme built from the cover " +
                "(FromArt, ArtScheme) on the player; no fixed palette.",
            "type" to linkedMapOf(
                "font" to "Google Sans Flex",
                "styles" to expressiveTypeStyles().associateTo(linkedMapOf()) { (name, style) ->
                    val axes = GoogleSansFlex.axesOf(style.fontFamily)
                    name to linkedMapOf(
                        "size" to Raw(num(style.fontSize.value)),
                        "line" to Raw(num(style.lineHeight.value)),
                        "weight" to style.fontWeight!!.weight,
                        "trackingSp" to Raw(num(style.letterSpacing.value)),
                        "round" to (axes?.round ?: false),
                        "opticalSize" to axes?.opticalSize,
                    )
                },
            ),
            "shape" to linkedMapOf(
                "cornerDp" to expressiveCorners.associateTo(linkedMapOf()) { (name, dp) -> name to Raw(num(dp.value)) },
                "coverRadiusDp" to steps { ExpressiveShapes.coverRadius(it) },
                "onCornerDp" to steps { ExpressiveShapes.onCorner(it) },
                "materialDp" to materialRadii(ExpressiveShapes.material),
            ),
            "motion" to expressiveMotion.associateTo(linkedMapOf()) { (name, spec) ->
                name to linkedMapOf("dampingRatio" to Raw(num(spec.dampingRatio)), "stiffness" to Raw(num(spec.stiffness)))
            },
        ),
        "resources" to linkedMapOf(
            "light" to colorResources(File(resDir, "values/colors.xml")),
            "dark" to colorResources(File(resDir, "values-night/colors.xml")),
        ),
    )

    /** A function of size as steps: each radius with the largest size (dp) it applies up to; the last has none. */
    private fun steps(f: (Dp) -> Dp): List<Map<String, Any?>> {
        val out = mutableListOf<Map<String, Any?>>()
        var current = f(1.dp)
        for (size in 2..1000) {
            val next = f(size.dp)
            if (next != current) {
                out += linkedMapOf("upToDp" to size - 1, "radiusDp" to Raw(num(current.value)))
                current = next
            }
        }
        out += linkedMapOf("upToDp" to null, "radiusDp" to Raw(num(current.value)))
        return out
    }

    private fun materialRadii(shapes: Shapes): Map<String, Any?> = linkedMapOf(
        "extraSmall" to radius(shapes.extraSmall),
        "small" to radius(shapes.small),
        "medium" to radius(shapes.medium),
        "large" to radius(shapes.large),
        "extraLarge" to radius(shapes.extraLarge),
    )

    private fun radius(shape: androidx.compose.ui.graphics.Shape): Raw {
        shape as RoundedCornerShape
        return Raw(num(shape.topStart.toPx(Size(1000f, 1000f), Density(1f))))
    }

    /** Literal `<color>` values (hex) in a colors.xml; references to system colours are kept as written. */
    fun colorResources(file: File): Map<String, String> {
        val pattern = Regex("""<color name="([a-z0-9_]+)">([^<]+)</color>""")
        return pattern.findAll(file.readText()).associateTo(linkedMapOf()) { it.groupValues[1] to it.groupValues[2].trim() }
    }

    // ---- A small JSON writer, so the file's order and number formatting stay put ----

    /** A number already formatted. */
    class Raw(val text: String)

    fun json(value: Any?, indent: String = ""): String = when (value) {
        null -> "null"
        is Raw -> value.text
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Boolean, is Int -> value.toString()
        is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(",\n", "{\n", "\n$indent}") { (k, v) ->
            "$indent  \"$k\": ${json(v, "$indent  ")}"
        }
        is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(",\n", "[\n", "\n$indent]") {
            "$indent  ${json(it, "$indent  ")}"
        }
        else -> error("Can't write ${value::class}")
    }
}
