package app.attic.player.ui.comfort

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.ArtModel
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.AtticMarkIcon
import app.attic.player.ui.expressive.Symbol
import coil3.compose.AsyncImage

/**
 * An album, playlist or folder cover (`.cv.sq`, `.cv.sq-lg`): a superellipse, `.sq` below 100dp and
 * `.sq-lg` from 100dp ([ArtShape] picks by the drawn size). Give [size] for a fixed cover, or leave
 * it null to fill the parent's width as a square. A file with no art shows the neutral icon box with a
 * note (`.ibox.neutral` + `music_note`). [dimmed] is the current song's `filter:brightness(.4)`, for
 * the playing bars to sit on.
 *
 * [art] is the shared `Art` type from ui/expressive/Cover.kt: `Art.of(mediaId)` or `Art.Picture(bitmap)`.
 */
@Composable
fun Cover(art: Art?, modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = ArtShape, dimmed: Boolean = false) {
    ArtBox(modifier, size, shape, Comfort.colors.fill) {
        NoArtGlyph()
        Picture(art)
        if (dimmed) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
    }
}

/** An artist (`.cv.sq-soft`): the rounder superellipse, at every size. Never a circle. */
@Composable
fun ArtistCover(art: Art?, modifier: Modifier = Modifier, size: Dp? = null) =
    Cover(art, modifier, size, ComfortShapes.SquircleSoft)

/**
 * A folder or playlist (`.mosaic`): four covers in a 2×2 grid when there are four or more, else the
 * first one, else the placeholder. Pass covers from different albums (`mosaicIds` in ui/Components.kt).
 */
@Composable
fun Mosaic(arts: List<Art>, modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = ArtShape) {
    if (arts.size < 4) return Cover(arts.firstOrNull(), modifier, size, shape)
    ArtBox(modifier, size, shape, Comfort.colors.fill) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 2) {
                Row(Modifier.weight(1f)) {
                    for (column in 0 until 2) {
                        Box(Modifier.weight(1f).fillMaxSize()) { Picture(arts[row * 2 + column]) }
                    }
                }
            }
        }
    }
}

/** `.ibox` variants: default tonal amber, `.neutral`, `.solid`. */
enum class IconBoxStyle { Tonal, Neutral, Solid }

/**
 * `.ibox`: a superellipse holding one icon at 46% of its width: Favourites (tonal, filled heart),
 * placeholders (neutral), empty states (neutral, 76dp). [size] null fills the width.
 */
@Composable
fun IconBox(
    symbol: Symbol,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    style: IconBoxStyle = IconBoxStyle.Tonal,
    filled: Boolean = false,
) {
    val (background, ink) = iconBoxColors(style)
    ArtBox(modifier, size, ArtShape, background) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ComfortIcon(symbol, size = maxWidth * 0.46f, filled = filled, tint = ink)
        }
    }
}

/** Favourites' picture: the tonal icon box with a filled heart. */
@Composable
fun FavouritesArt(modifier: Modifier = Modifier, size: Dp? = null) =
    IconBox(app.attic.player.ui.expressive.Symbol.Favorite, modifier, size, IconBoxStyle.Tonal, filled = true)

/** The placeholder for a file with no cover. */
@Composable
fun NoArt(modifier: Modifier = Modifier, size: Dp? = null) =
    IconBox(app.attic.player.ui.expressive.Symbol.MusicNote, modifier, size, IconBoxStyle.Neutral)

/**
 * The Attic mark, "Headroom" (`.mark`): [size] tall, in the accent unless told otherwise (amber, or the
 * cover's colour inside [Tinted]). 24dp by default, 30dp above Home's hero title.
 */
@Composable
fun ComfortMark(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Comfort.colors.accent) =
    AtticMarkIcon(modifier, size, tint)

/** The mark in an icon box (`.ibox` holding `.mark`), at half the box's width. */
@Composable
fun MarkBox(modifier: Modifier = Modifier, size: Dp? = null, style: IconBoxStyle = IconBoxStyle.Tonal) {
    val (background, ink) = iconBoxColors(style)
    ArtBox(modifier, size, ArtShape, background) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AtticMarkIcon(size = maxWidth * 0.5f, tint = ink)
        }
    }
}

@Composable
private fun iconBoxColors(style: IconBoxStyle): Pair<Color, Color> {
    val colors = Comfort.colors
    return when (style) {
        IconBoxStyle.Tonal -> colors.accentSoft to colors.accentInk
        IconBoxStyle.Neutral -> colors.fill to colors.on2
        IconBoxStyle.Solid -> colors.accent to colors.onAccent
    }
}

@Composable
private fun ArtBox(modifier: Modifier, size: Dp?, shape: Shape, background: Color, content: @Composable () -> Unit) {
    Box(
        modifier
            .then(if (size != null) Modifier.size(size) else Modifier.aspectRatio(1f))
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NoArtGlyph() {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ComfortIcon(Symbol.MusicNote, size = maxWidth * 0.46f, tint = Comfort.colors.on2)
    }
}

@Composable
private fun Picture(art: Art?) {
    when (art) {
        is Art.Song -> AsyncImage(
            model = ArtModel(art.mediaId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        is Art.Picture -> Image(art.bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        null -> Unit
    }
}
