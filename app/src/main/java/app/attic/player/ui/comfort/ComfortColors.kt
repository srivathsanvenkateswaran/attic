package app.attic.player.ui.comfort

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour token in oneui/kit.css section 1, by its CSS name (`--fill-2` is [fill2], `--accent-ink`
 * is [accentInk]). Read them with `Comfort.colors` inside a Comfort theme; [Tinted] swaps some of them
 * for the cover's colour, so a component that reads tokens from here follows the artwork by itself.
 *
 * The one addition to the kit is [miniInk2], the mini player's second line: kit.css draws it in
 * `--on-2`, which falls to 3.6:1 over a cover-coloured fill. Untinted it is `--on-2`.
 */
@Immutable
data class ComfortColors(
    val dark: Boolean,
    /** Night / Plaster: the screen. */
    val ground: Color,
    /** Loft / Paper: groups, cards, the deck. */
    val card: Color,
    /** Rafter: things that float (dock, sheets, dialogs, menus). */
    val raised: Color,
    /** Slate / Putty: control tracks, tonal icon buttons, chips on a card. */
    val fill: Color,
    /** Switch-off track, progress track, grab handle. */
    val fill2: Color,
    /** Seam: inset hairlines. */
    val line: Color,
    /** A 1dp ring around floating pieces, drawn with their shadow. */
    val edge: Color,
    /** Chalk / Ink: text and icons. */
    val on: Color,
    /** Dust: secondary text. */
    val on2: Color,
    /** Ash: hints, placeholders and trailing icons. Below 4.5:1 by design; never body text. */
    val on3: Color,
    /** Lamplight: fills, the one filled button, switches, progress, selection. */
    val accent: Color,
    /** Soot: ink on [accent]. */
    val onAccent: Color,
    /** The accent as text or an icon on a neutral surface (Brass in light). */
    val accentInk: Color,
    /** The tonal container: selected tab, current row, tonal button. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Ember: delete, remove. */
    val danger: Color,
    /** Marks drawn on top of artwork (the playing bars over a dimmed cover). */
    val onArt: Color,
    /** The switch knob. */
    val knob: Color,
    val toast: Color,
    val onToast: Color,
    val scrim: Color,
    /** Shadow colours for `--shadow` (floating pieces) and `--shadow-sm` (a lifted row, the knob, a toast). */
    val shadow: Color,
    val shadowSmall: Color,
    /** The mini player's ground and its played part (`--mini-base`, `--mini-fill`). */
    val miniBase: Color,
    val miniFill: Color,
    /** The mini player's second line. See the class note. */
    val miniInk2: Color,
)

/** kit.css `:is(.phone,.strip)`. */
val ComfortDark = ComfortColors(
    dark = true,
    ground = Color(0xFF000000),
    card = Color(0xFF171716),
    raised = Color(0xFF232221),
    fill = Color(0xFF2C2B29),
    fill2 = Color(0xFF3B3936),
    line = Color(0xFF2A2928),
    edge = Color.White.copy(alpha = 0.07f),
    on = Color(0xFFF5F4F1),
    on2 = Color(0xFFAAA7A2),
    on3 = Color(0xFF73706B),
    accent = Color(0xFFFFB347),
    onAccent = Color(0xFF2A1800),
    accentInk = Color(0xFFFFC063),
    accentSoft = Color(0xFF3A2A13),
    onAccentSoft = Color(0xFFFFCD85),
    danger = Color(0xFFFF7A6B),
    onArt = Color.White,
    knob = Color.White,
    toast = Color(0xFF3B3936),
    onToast = Color(0xFFF5F4F1),
    scrim = Color.Black.copy(alpha = 0.62f),
    shadow = Color.Black.copy(alpha = 0.7f),
    shadowSmall = Color.Black.copy(alpha = 0.45f),
    miniBase = Color(0xFF232221),
    miniFill = Color(0xFF2C2B29),
    miniInk2 = Color(0xFFAAA7A2),
)

/** kit.css `:is(.phone,.strip).light`. */
val ComfortLight = ComfortColors(
    dark = false,
    ground = Color(0xFFF2F1EE),
    card = Color(0xFFFFFFFF),
    raised = Color(0xFFFFFFFF),
    fill = Color(0xFFEEECE8),
    fill2 = Color(0xFFDCD9D3),
    line = Color(0xFFEBE9E4),
    edge = Color(0xFF281E0A).copy(alpha = 0.06f),
    on = Color(0xFF191817),
    on2 = Color(0xFF67645F),
    on3 = Color(0xFF9C9993),
    accent = Color(0xFFF2A024),
    onAccent = Color(0xFF2A1800),
    accentInk = Color(0xFF985600),
    accentSoft = Color(0xFFFBE8C9),
    onAccentSoft = Color(0xFF7A4500),
    danger = Color(0xFFCF3A2A),
    onArt = Color.White,
    knob = Color.White,
    toast = Color(0xFF3D3B38),
    onToast = Color.White,
    scrim = Color(0xFF18140C).copy(alpha = 0.4f),
    shadow = Color(0xFF3C2D14).copy(alpha = 0.14f),
    shadowSmall = Color(0xFF3C2D14).copy(alpha = 0.16f),
    miniBase = Color(0xFFFFFFFF),
    miniFill = Color(0xFFEEECE8),
    miniInk2 = Color(0xFF67645F),
)

/** The tokens in effect. AtticTheme provides the palette; [Tinted] replaces it for its content. */
val LocalComfortColors = staticCompositionLocalOf { ComfortDark }

/**
 * The tokens as a Material [ColorScheme], so stock Material components (sheet, dialog, menu, switch,
 * text field) draw in the kit's colours: the accent is primary, the tonal container is
 * primaryContainer and secondaryContainer, ground is surface, card is surfaceContainer, raised is
 * surfaceContainerHigh, fill is surfaceContainerHighest, on-2 is onSurfaceVariant, on-3 is outline,
 * the hairline is outlineVariant and the toast is inverseSurface. There is no second accent:
 * secondary and tertiary repeat the accent's ink. Surface tint is off, so nothing tints by elevation.
 */
fun ComfortColors.toColorScheme(): ColorScheme {
    val errorContainer = if (dark) Color(0xFF3D1A16) else Color(0xFFFBE0DC)
    val onErrorContainer = if (dark) Color(0xFFFFB4A9) else Color(0xFF8C1D12)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = onAccentSoft,
        inversePrimary = accentInk,
        secondary = accentInk,
        onSecondary = ground,
        secondaryContainer = accentSoft,
        onSecondaryContainer = onAccentSoft,
        tertiary = accentInk,
        onTertiary = ground,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = onAccentSoft,
        background = ground,
        onBackground = on,
        surface = ground,
        onSurface = on,
        surfaceVariant = fill,
        onSurfaceVariant = on2,
        surfaceTint = Color.Transparent,
        inverseSurface = toast,
        inverseOnSurface = onToast,
        error = danger,
        onError = if (dark) Color(0xFF2A0A06) else Color.White,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = on3,
        outlineVariant = line,
        scrim = scrim,
        surfaceBright = raised,
        surfaceDim = ground,
        surfaceContainerLowest = ground,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = raised,
        surfaceContainerHighest = fill,
    )
}
