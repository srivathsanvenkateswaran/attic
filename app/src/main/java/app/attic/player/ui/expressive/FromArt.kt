package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import app.attic.player.ui.LocalIsDark
import app.attic.player.ui.rememberArtColor

/**
 * Draws [content] in the colour scheme of a cover: the kit's `.from-art`. Every Material role inside
 * is rebuilt from [seed]'s hue by [ArtScheme], so stock components and kit components alike take the
 * cover's colours. When the seed changes (a new song) every role animates on the slow effects spring,
 * about 600 ms. It follows light and dark from [dark].
 *
 * Use it on player surfaces only: mini player, Now Playing, queue, full-screen lyrics, collection and
 * artist headers, browse tiles. A list under a collection header stays on the app scheme.
 *
 * [seed] is a cover tint from `rememberArtColor`/`coverTint` in ui/Art.kt. With no seed
 * ([Color.Unspecified]) the content keeps the scheme around it.
 */
@Composable
fun FromArt(
    seed: Color,
    dark: Boolean = LocalIsDark.current,
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.colorScheme
    val target = rememberArtScheme(seed, dark, base)
    val scheme = animateColorScheme(target, ExpressiveMotion.slowEffects())
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}

/** [FromArt] for a song's cover, looked up by its MediaStore id. A song with no art keeps the scheme around it. */
@Composable
fun FromCover(mediaId: Long?, dark: Boolean = LocalIsDark.current, content: @Composable () -> Unit) {
    val seed = rememberArtColor(mediaId, fallback = Color.Unspecified)
    FromArt(seed, dark, content)
}

/** The art scheme for [seed] without providing it, e.g. to colour one surface by hand. Not animated. */
@Composable
fun rememberArtScheme(seed: Color, dark: Boolean = LocalIsDark.current, base: ColorScheme = MaterialTheme.colorScheme): ColorScheme =
    remember(seed, dark, base) {
        if (seed.isSpecified) ArtScheme.colorScheme(seed.toArgb(), dark, base) else base
    }

/** Animates every role of a scheme towards [target]. */
@Composable
fun animateColorScheme(target: ColorScheme, spec: AnimationSpec<Color>): ColorScheme {
    @Composable
    fun animate(color: Color): Color {
        val value by animateColorAsState(color, spec, label = "scheme")
        return value
    }
    return target.copy(
        primary = animate(target.primary),
        onPrimary = animate(target.onPrimary),
        primaryContainer = animate(target.primaryContainer),
        onPrimaryContainer = animate(target.onPrimaryContainer),
        inversePrimary = animate(target.inversePrimary),
        secondary = animate(target.secondary),
        onSecondary = animate(target.onSecondary),
        secondaryContainer = animate(target.secondaryContainer),
        onSecondaryContainer = animate(target.onSecondaryContainer),
        tertiary = animate(target.tertiary),
        onTertiary = animate(target.onTertiary),
        tertiaryContainer = animate(target.tertiaryContainer),
        onTertiaryContainer = animate(target.onTertiaryContainer),
        background = animate(target.background),
        onBackground = animate(target.onBackground),
        surface = animate(target.surface),
        onSurface = animate(target.onSurface),
        surfaceVariant = animate(target.surfaceVariant),
        onSurfaceVariant = animate(target.onSurfaceVariant),
        surfaceTint = animate(target.surfaceTint),
        inverseSurface = animate(target.inverseSurface),
        inverseOnSurface = animate(target.inverseOnSurface),
        outline = animate(target.outline),
        outlineVariant = animate(target.outlineVariant),
        surfaceBright = animate(target.surfaceBright),
        surfaceDim = animate(target.surfaceDim),
        surfaceContainerLowest = animate(target.surfaceContainerLowest),
        surfaceContainerLow = animate(target.surfaceContainerLow),
        surfaceContainer = animate(target.surfaceContainer),
        surfaceContainerHigh = animate(target.surfaceContainerHigh),
        surfaceContainerHighest = animate(target.surfaceContainerHighest),
    )
}
