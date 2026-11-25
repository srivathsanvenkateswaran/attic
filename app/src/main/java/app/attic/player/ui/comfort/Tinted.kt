package app.attic.player.ui.comfort

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import app.attic.player.ui.rememberArtColor

/**
 * kit.css section 2, `.tinted` and `.tinted.wash`, as a function of the palette around it and the
 * cover's tint. Each token keeps the tint's OKLCH hue, scales its chroma by a fixed factor and sets
 * its own lightness, exactly as the CSS writes `oklch(from var(--tint) L calc(c*k) h)`. So contrast is
 * the same for a pale cover and a dark one. Only the tint's hue and chroma are read; its lightness is
 * ignored.
 */
object ComfortTint {
    /** One token: OKLCH lightness and the factor on the tint's chroma. */
    data class Spec(val lightness: Double, val chroma: Double)

    /** `.tinted`, dark. The mini player's second line ([ComfortColors.miniInk2]) is Attic's addition. */
    val darkTinted = mapOf(
        "accent" to Spec(0.76, 1.0),
        "onAccent" to Spec(0.2, 0.55),
        "accentInk" to Spec(0.82, 0.85),
        "accentSoft" to Spec(0.31, 0.6),
        "onAccentSoft" to Spec(0.89, 0.5),
        "miniBase" to Spec(0.25, 0.5),
        "miniFill" to Spec(0.4, 0.9),
        "miniInk2" to Spec(0.82, 0.3),
    )

    /** `.tinted.wash`, dark: the surfaces and the two quieter inks. */
    val darkWash = mapOf(
        "ground" to Spec(0.115, 0.28),
        "card" to Spec(0.215, 0.34),
        "raised" to Spec(0.26, 0.34),
        "fill" to Spec(0.3, 0.4),
        "fill2" to Spec(0.38, 0.45),
        "line" to Spec(0.28, 0.36),
        "on2" to Spec(0.77, 0.3),
        "on3" to Spec(0.57, 0.26),
    )

    /** `.light .tinted`. On the accent the ink is plain white, as the CSS has it. */
    val lightTinted = mapOf(
        "accent" to Spec(0.47, 1.0),
        "accentInk" to Spec(0.43, 1.0),
        "accentSoft" to Spec(0.9, 0.4),
        "onAccentSoft" to Spec(0.36, 1.0),
        "miniBase" to Spec(0.955, 0.16),
        "miniFill" to Spec(0.87, 0.42),
        "miniInk2" to Spec(0.45, 0.3),
    )

    val lightWash = mapOf(
        "ground" to Spec(0.93, 0.2),
        "card" to Spec(0.978, 0.08),
        "raised" to Spec(0.985, 0.06),
        "fill" to Spec(0.905, 0.26),
        "fill2" to Spec(0.84, 0.34),
        "line" to Spec(0.915, 0.14),
        "on2" to Spec(0.45, 0.3),
        "on3" to Spec(0.62, 0.26),
    )

    /**
     * [base] with the tokens `.tinted` (and with [wash], `.tinted.wash`) moves, computed from [tint]
     * (an ARGB cover colour). Tokens the rule does not name keep [base]'s value, so a `Tinted` inside
     * a washed one keeps the washed surfaces, as nested CSS classes would.
     */
    fun apply(base: ComfortColors, tint: Int, wash: Boolean): ComfortColors {
        val lch = Oklch.fromArgb(tint)
        val specs = buildMap {
            putAll(if (base.dark) darkTinted else lightTinted)
            if (wash) putAll(if (base.dark) darkWash else lightWash)
        }
        fun token(name: String, fallback: Color): Color =
            specs[name]?.let { Color(Oklch.toArgb(it.lightness, lch.c * it.chroma, lch.h)) } ?: fallback
        return base.copy(
            ground = token("ground", base.ground),
            card = token("card", base.card),
            raised = token("raised", base.raised),
            fill = token("fill", base.fill),
            fill2 = token("fill2", base.fill2),
            line = token("line", base.line),
            on2 = token("on2", base.on2),
            on3 = token("on3", base.on3),
            accent = token("accent", base.accent),
            onAccent = if (base.dark) token("onAccent", base.onAccent) else Color.White,
            accentInk = token("accentInk", base.accentInk),
            accentSoft = token("accentSoft", base.accentSoft),
            onAccentSoft = token("onAccentSoft", base.onAccentSoft),
            miniBase = token("miniBase", base.miniBase),
            miniFill = token("miniFill", base.miniFill),
            miniInk2 = token("miniInk2", base.miniInk2),
        )
    }

    /** Text roles and the surfaces they sit on, as the kit pairs them. Each holds 4.5:1 for any tint. */
    val textPairs: List<Pair<(ComfortColors) -> Color, (ComfortColors) -> Color>> = buildList {
        val surfaces = listOf<(ComfortColors) -> Color>({ it.ground }, { it.card }, { it.raised })
        for (surface in surfaces) {
            add({ c: ComfortColors -> c.on } to surface)
            add({ c: ComfortColors -> c.on2 } to surface)
            add({ c: ComfortColors -> c.accentInk } to surface)
        }
        add({ c: ComfortColors -> c.on } to { c: ComfortColors -> c.fill })
        add({ c: ComfortColors -> c.on2 } to { c: ComfortColors -> c.fill })
        add({ c: ComfortColors -> c.on } to { c: ComfortColors -> c.fill2 })
        add({ c: ComfortColors -> c.onAccent } to { c: ComfortColors -> c.accent })
        add({ c: ComfortColors -> c.onAccentSoft } to { c: ComfortColors -> c.accentSoft })
        add({ c: ComfortColors -> c.on } to { c: ComfortColors -> c.accentSoft })
        add({ c: ComfortColors -> c.on } to { c: ComfortColors -> c.miniBase })
        add({ c: ComfortColors -> c.on } to { c: ComfortColors -> c.miniFill })
        add({ c: ComfortColors -> c.miniInk2 } to { c: ComfortColors -> c.miniBase })
        add({ c: ComfortColors -> c.miniInk2 } to { c: ComfortColors -> c.miniFill })
    }
}

/** How long a tinted container takes to change colour when the cover changes. */
internal val TintSpec: AnimationSpec<Color> = tween(durationMillis = 450)

/**
 * Draws [content] in the colours of a cover: kit.css `.tinted`, and with [wash] `.tinted.wash`.
 *
 * - `.tinted` swaps the accent tokens (accent, on-accent, accent-ink, accent-soft, on-accent-soft) and
 *   the mini player's two fills for the cover's colour. Every kit component inside, and every stock
 *   Material component (MaterialTheme.colorScheme is rebuilt from the tokens), follows.
 * - [wash] also leans the surfaces (ground, card, raised, fill, fill-2, line) and the secondary and
 *   tertiary inks toward it. `--on` never changes.
 *
 * Amber does not appear inside: the accent is the cover's. A [seed] of [Color.Unspecified] (a song
 * with no art) keeps the palette around it. When [seed] changes (a new song) every token animates over
 * about 450 ms. Light or dark follows the palette around it.
 *
 * [seed] is a cover colour: `rememberArtColor(mediaId, Color.Unspecified, deepen = false)` from
 * ui/Art.kt, or use [TintedCover], which looks it up.
 */
@Composable
fun Tinted(seed: Color, wash: Boolean = false, content: @Composable () -> Unit) {
    val base = LocalComfortColors.current
    val target = remember(base, seed, wash) {
        if (seed.isSpecified) ComfortTint.apply(base, seed.toArgb(), wash) else base
    }
    val colors = animateComfortColors(target)
    ProvideComfortColors(colors, content)
}

/** [Tinted] for a song's cover, looked up by its MediaStore id. A song with no art keeps the palette. */
@Composable
fun TintedCover(mediaId: Long?, wash: Boolean = false, content: @Composable () -> Unit) {
    val seed = rememberArtColor(mediaId, fallback = Color.Unspecified, deepen = false)
    Tinted(seed, wash, content)
}

/** Provides [colors] as the Comfort tokens and as MaterialTheme's scheme, with `--on` as content colour. */
@Composable
fun ProvideComfortColors(colors: ComfortColors, content: @Composable () -> Unit) {
    val scheme = remember(colors) { colors.toColorScheme() }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(
            LocalComfortColors provides colors,
            LocalContentColor provides colors.on,
            content = content,
        )
    }
}

@Composable
private fun animateComfortColors(target: ComfortColors): ComfortColors {
    @Composable
    fun animate(color: Color): Color {
        val value by animateColorAsState(color, TintSpec, label = "tint")
        return value
    }
    return target.copy(
        ground = animate(target.ground),
        card = animate(target.card),
        raised = animate(target.raised),
        fill = animate(target.fill),
        fill2 = animate(target.fill2),
        line = animate(target.line),
        on2 = animate(target.on2),
        on3 = animate(target.on3),
        accent = animate(target.accent),
        onAccent = animate(target.onAccent),
        accentInk = animate(target.accentInk),
        accentSoft = animate(target.accentSoft),
        onAccentSoft = animate(target.onAccentSoft),
        miniBase = animate(target.miniBase),
        miniFill = animate(target.miniFill),
        miniInk2 = animate(target.miniInk2),
    )
}
