package app.attic.player.ui.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The colour scheme a player surface takes from its cover: the kit's `.from-art` rule.
 *
 * kit.css builds every role as `oklch(L C h)` with h the cover tint's hue and L, C fixed per role.
 * Here each role keeps that fixed lightness and chroma, converted to HCT tone and chroma (the tone of
 * the kit's OKLCH lightness, and the median chroma over all hues), so a dull cover and a loud one give
 * the same contrast. Primary, secondary and the neutrals take the cover's HCT hue. Tertiary takes the
 * hue turned 60°, as Material's tonal-spot scheme does, which the CSS could not do.
 *
 * The CSS also mixes in a tenth of the tint's own tone and chroma. That is left out: it would make
 * contrast depend on the cover.
 */
object ArtScheme {
    /** A role's place in a tonal palette. */
    data class Role(val tone: Double, val chroma: Double)

    /** Which palette a role draws from: the hue it uses. */
    enum class Palette { Primary, Secondary, Tertiary, Neutral, NeutralVariant }

    private const val TERTIARY_ROTATION = 60.0

    /** Every role the kit defines, per theme. Tone and chroma come from the kit's OKLCH values (see the class doc). */
    internal val dark: Map<String, Pair<Palette, Role>> = mapOf(
        "primary" to (Palette.Primary to Role(83.8, 34.6)),
        "onPrimary" to (Palette.Primary to Role(14.2, 25.0)),
        "primaryContainer" to (Palette.Primary to Role(32.7, 33.8)),
        "onPrimaryContainer" to (Palette.Primary to Role(91.9, 18.1)),
        "inversePrimary" to (Palette.Primary to Role(39.7, 42.1)),
        "secondary" to (Palette.Secondary to Role(81.4, 20.5)),
        "onSecondary" to (Palette.Secondary to Role(15.3, 17.6)),
        "secondaryContainer" to (Palette.Secondary to Role(25.8, 23.1)),
        "onSecondaryContainer" to (Palette.Secondary to Role(89.6, 16.7)),
        "tertiary" to (Palette.Tertiary to Role(91.9, 21.0)),
        "onTertiary" to (Palette.Tertiary to Role(28.1, 32.2)),
        "tertiaryContainer" to (Palette.Tertiary to Role(88.4, 26.1)),
        "onTertiaryContainer" to (Palette.Tertiary to Role(23.4, 32.2)),
        "surface" to (Palette.Neutral to Role(5.7, 13.6)),
        "surfaceDim" to (Palette.Neutral to Role(5.7, 13.6)),
        "surfaceBright" to (Palette.Neutral to Role(25.8, 17.6)),
        "surfaceContainerLowest" to (Palette.Neutral to Role(2.5, 11.4)),
        "surfaceContainerLow" to (Palette.Neutral to Role(8.9, 15.2)),
        "surfaceContainer" to (Palette.Neutral to Role(12.4, 16.8)),
        "surfaceContainerHigh" to (Palette.Neutral to Role(15.9, 18.3)),
        "surfaceContainerHighest" to (Palette.Neutral to Role(20.0, 19.5)),
        "onSurface" to (Palette.Neutral to Role(93.0, 9.2)),
        "inverseSurface" to (Palette.Neutral to Role(93.0, 9.2)),
        "inverseOnSurface" to (Palette.Neutral to Role(18.8, 13.7)),
        "onSurfaceVariant" to (Palette.NeutralVariant to Role(79.1, 15.0)),
        "outline" to (Palette.NeutralVariant to Role(58.2, 15.3)),
        "outlineVariant" to (Palette.NeutralVariant to Role(30.4, 17.5)),
    )

    internal val light: Map<String, Pair<Palette, Role>> = mapOf(
        "primary" to (Palette.Primary to Role(38.5, 43.6)),
        "onPrimary" to (Palette.Primary to Role(98.8, 4.5)),
        "primaryContainer" to (Palette.Primary to Role(84.9, 28.4)),
        "onPrimaryContainer" to (Palette.Primary to Role(22.3, 35.0)),
        "inversePrimary" to (Palette.Primary to Role(83.8, 34.6)),
        "secondary" to (Palette.Secondary to Role(37.4, 23.0)),
        "onSecondary" to (Palette.Secondary to Role(98.8, 4.5)),
        "secondaryContainer" to (Palette.Secondary to Role(87.8, 18.6)),
        "onSecondaryContainer" to (Palette.Secondary to Role(24.6, 23.1)),
        "tertiary" to (Palette.Tertiary to Role(35.0, 35.5)),
        "onTertiary" to (Palette.Tertiary to Role(98.8, 4.5)),
        "tertiaryContainer" to (Palette.Tertiary to Role(88.4, 26.1)),
        "onTertiaryContainer" to (Palette.Tertiary to Role(23.4, 32.2)),
        "surface" to (Palette.Neutral to Role(96.3, 8.5)),
        "surfaceDim" to (Palette.Neutral to Role(87.2, 13.0)),
        "surfaceBright" to (Palette.Neutral to Role(96.3, 8.5)),
        "surfaceContainerLowest" to (Palette.Neutral to Role(99.4, 3.6)),
        "surfaceContainerLow" to (Palette.Neutral to Role(94.2, 10.2)),
        "surfaceContainer" to (Palette.Neutral to Role(92.2, 12.2)),
        "surfaceContainerHigh" to (Palette.Neutral to Role(90.1, 13.7)),
        "surfaceContainerHighest" to (Palette.Neutral to Role(87.8, 15.3)),
        "onSurface" to (Palette.Neutral to Role(9.5, 13.7)),
        "inverseSurface" to (Palette.Neutral to Role(14.2, 13.7)),
        "inverseOnSurface" to (Palette.Neutral to Role(94.2, 9.2)),
        "onSurfaceVariant" to (Palette.NeutralVariant to Role(30.4, 17.5)),
        "outline" to (Palette.NeutralVariant to Role(51.3, 17.3)),
        "outlineVariant" to (Palette.NeutralVariant to Role(79.1, 16.9)),
    )

    /** The hue each palette uses for a cover whose HCT hue is [seedHue]. */
    fun paletteHue(palette: Palette, seedHue: Double): Double = when (palette) {
        Palette.Tertiary -> (seedHue + TERTIARY_ROTATION) % 360.0
        else -> seedHue
    }

    /** Every role as opaque ARGB, keyed by its [ColorScheme] property name. */
    fun roles(seedArgb: Int, dark: Boolean): Map<String, Int> {
        val hue = Hct.fromArgb(seedArgb).hue
        val table = if (dark) this.dark else light
        return table.mapValues { (_, spec) ->
            val (palette, role) = spec
            Hct(paletteHue(palette, hue), role.chroma, role.tone).toArgb()
        }
    }

    /**
     * The full scheme for a cover. Error roles and anything the kit does not define come from
     * [fallback], normally the app scheme, so they match the rest of the app.
     */
    fun colorScheme(seedArgb: Int, dark: Boolean, fallback: ColorScheme? = null): ColorScheme {
        val r = roles(seedArgb, dark).mapValues { Color(it.value) }
        val base = fallback ?: if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = r.getValue("primary"),
            onPrimary = r.getValue("onPrimary"),
            primaryContainer = r.getValue("primaryContainer"),
            onPrimaryContainer = r.getValue("onPrimaryContainer"),
            inversePrimary = r.getValue("inversePrimary"),
            secondary = r.getValue("secondary"),
            onSecondary = r.getValue("onSecondary"),
            secondaryContainer = r.getValue("secondaryContainer"),
            onSecondaryContainer = r.getValue("onSecondaryContainer"),
            tertiary = r.getValue("tertiary"),
            onTertiary = r.getValue("onTertiary"),
            tertiaryContainer = r.getValue("tertiaryContainer"),
            onTertiaryContainer = r.getValue("onTertiaryContainer"),
            background = r.getValue("surface"),
            onBackground = r.getValue("onSurface"),
            surface = r.getValue("surface"),
            onSurface = r.getValue("onSurface"),
            surfaceVariant = r.getValue("surfaceContainerHighest"),
            onSurfaceVariant = r.getValue("onSurfaceVariant"),
            surfaceTint = r.getValue("primary"),
            inverseSurface = r.getValue("inverseSurface"),
            inverseOnSurface = r.getValue("inverseOnSurface"),
            outline = r.getValue("outline"),
            outlineVariant = r.getValue("outlineVariant"),
            surfaceBright = r.getValue("surfaceBright"),
            surfaceDim = r.getValue("surfaceDim"),
            surfaceContainerLowest = r.getValue("surfaceContainerLowest"),
            surfaceContainerLow = r.getValue("surfaceContainerLow"),
            surfaceContainer = r.getValue("surfaceContainer"),
            surfaceContainerHigh = r.getValue("surfaceContainerHigh"),
            surfaceContainerHighest = r.getValue("surfaceContainerHighest"),
        )
    }

    /** Pairs of (content role, container role) that carry text, for the contrast tests. */
    val textPairs: List<Pair<String, String>> = listOf(
        "onPrimary" to "primary",
        "onPrimaryContainer" to "primaryContainer",
        "onSecondary" to "secondary",
        "onSecondaryContainer" to "secondaryContainer",
        "onTertiary" to "tertiary",
        "onTertiaryContainer" to "tertiaryContainer",
        "onSurface" to "surface",
        "onSurface" to "surfaceContainerLowest",
        "onSurface" to "surfaceContainerLow",
        "onSurface" to "surfaceContainer",
        "onSurface" to "surfaceContainerHigh",
        "onSurface" to "surfaceContainerHighest",
        "onSurface" to "surfaceBright",
        "onSurfaceVariant" to "surface",
        "onSurfaceVariant" to "surfaceContainer",
        "onSurfaceVariant" to "surfaceContainerHigh",
        "onSurfaceVariant" to "surfaceContainerHighest",
        "inverseOnSurface" to "inverseSurface",
        "primary" to "surface",
        "primary" to "surfaceContainer",
        "primary" to "surfaceContainerHigh",
        "onSurface" to "secondaryContainer",
    )
}
