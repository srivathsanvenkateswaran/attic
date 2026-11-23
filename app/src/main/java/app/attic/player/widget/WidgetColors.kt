package app.attic.player.widget

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import app.attic.player.ui.comfort.ComfortColors
import app.attic.player.ui.comfort.ComfortDark
import app.attic.player.ui.comfort.ComfortLight
import app.attic.player.ui.comfort.ComfortTint
import app.attic.player.ui.expressive.ArtScheme

/**
 * The colours one theme of the widget is drawn in, as ARGB. Both themes are worked out up front and
 * handed to the launcher together, so the widget follows the phone's night mode as it changes.
 */
data class WidgetPalette(
    val container: Int,
    val title: Int,
    val artist: Int,
    val play: Int,
    val onPlay: Int,
    /** The fill behind previous and next, or null where they are bare glyphs (Comfort). */
    val skip: Int?,
    val onSkip: Int,
    val mark: Int,
    /** Comfort's hairline over the album tray. */
    val line: Int,
    /** An art slot with no cover: its fill and the mark drawn in it. */
    val empty: Int,
    val onEmpty: Int,
)

/** Each style's rule for the widget's colours, reusing the app's own cover-colour maths. */
object WidgetColors {
    /**
     * pixel/10-system: one container in the cover's scheme (ArtScheme, the kit's `.from-art`), in the
     * role the mini player uses, `surfaceContainerHigh`. [tint] is the cover tint from `coverTint`,
     * the seed FromArt takes in the app.
     */
    fun expressive(tint: Int, dark: Boolean): WidgetPalette = expressive(ArtScheme.roles(tint, dark))

    /** With no cover, the wallpaper's scheme, as the app has around its player surfaces. */
    fun expressive(scheme: ColorScheme): WidgetPalette = expressive(
        mapOf(
            "surfaceContainerHigh" to scheme.surfaceContainerHigh.toArgb(),
            "onSurface" to scheme.onSurface.toArgb(),
            "onSurfaceVariant" to scheme.onSurfaceVariant.toArgb(),
            "primary" to scheme.primary.toArgb(),
            "onPrimary" to scheme.onPrimary.toArgb(),
            "secondaryContainer" to scheme.secondaryContainer.toArgb(),
            "onSecondaryContainer" to scheme.onSecondaryContainer.toArgb(),
            "outlineVariant" to scheme.outlineVariant.toArgb(),
        ),
    )

    private fun expressive(r: Map<String, Int>) = WidgetPalette(
        container = r.getValue("surfaceContainerHigh"),
        title = r.getValue("onSurface"),
        artist = r.getValue("onSurfaceVariant"),
        play = r.getValue("primary"),
        onPlay = r.getValue("onPrimary"),
        skip = r.getValue("secondaryContainer"),
        onSkip = r.getValue("onSecondaryContainer"),
        mark = r.getValue("onSurfaceVariant"),
        line = r.getValue("outlineVariant"),
        empty = r.getValue("secondaryContainer"),
        onEmpty = r.getValue("onSecondaryContainer"),
    )

    /**
     * oneui/10-system: the kit's card washed toward the cover (`.tinted.wash`, ComfortTint), with the
     * cover's colour on the play button and the mark. [swatch] is the cover's own colour from
     * `coverSwatch`, as Tinted takes it in the app; null keeps the neutral card and the amber accent.
     */
    fun comfort(swatch: Int?, dark: Boolean): WidgetPalette {
        val base = if (dark) ComfortDark else ComfortLight
        return comfort(if (swatch != null) ComfortTint.apply(base, swatch, wash = true) else base)
    }

    private fun comfort(c: ComfortColors) = WidgetPalette(
        container = c.card.toArgb(),
        title = c.on.toArgb(),
        artist = c.on2.toArgb(),
        play = c.accent.toArgb(),
        onPlay = c.onAccent.toArgb(),
        skip = null,
        onSkip = c.on.toArgb(),
        mark = c.accent.toArgb(),
        line = c.line.toArgb(),
        empty = c.fill.toArgb(),
        onEmpty = c.on2.toArgb(),
    )
}
