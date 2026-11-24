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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.ArtModel
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.AtticMarkIcon
import app.attic.player.ui.expressive.Symbol
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.transformations
import kotlinx.coroutines.delay

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
    if (art is Art.Song && !dimmed && LocalBakedCovers.current && SuperellipseMask.canBake(shape)) {
        BakedCover(art.mediaId, modifier, size, shape)
    } else {
        ArtBox(modifier, size, shape, Comfort.colors.fill) {
            NoArtGlyph(size)
            Picture(art)
            if (dimmed) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }
    }
}

/**
 * Whether [Cover] and [Mosaic] bake their superellipse into the picture ([SuperellipseMask]) rather
 * than clip to it. A baked edge is drawn at the cover's own size, so anything that scales covers as
 * a layer (Now Playing's paused cover, the Recap cards) turns this off and keeps the clip, which
 * stays sharp at any scale.
 */
internal val LocalBakedCovers = staticCompositionLocalOf { true }

/**
 * A cover drawn with no clip: the picture comes with its shape baked in, and until it has faded in
 * (or when there is none) the shape is filled under it and the note glyph drawn, as [ArtBox] would.
 * Once the picture covers them they are no longer drawn, so a scrolled list draws one image a cover.
 */
@Composable
private fun BakedCover(mediaId: Long, modifier: Modifier, size: Dp?, shape: Shape) {
    val fill = Comfort.colors.fill
    val density = LocalDensity.current.density
    val context = LocalPlatformContext.current
    val request = remember(context, mediaId, shape, density) {
        ImageRequest.Builder(context).data(ArtModel(mediaId)).transformations(SuperellipseMask(shape, density)).build()
    }
    var shown by remember(mediaId) { mutableStateOf<DataSource?>(null) }
    var covered by remember(mediaId) { mutableStateOf(false) }
    Box(
        modifier
            .then(if (size != null) Modifier.size(size) else Modifier.aspectRatio(1f))
            .drawWithCache {
                val mask = shapeMask(shape, this.size)
                val tint = ColorFilter.tint(fill)
                onDrawBehind { if (!covered) drawImage(mask, colorFilter = tint) }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!covered) NoArtGlyph(size)
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state -> if (state is AsyncImagePainter.State.Success) shown = state.result.dataSource },
            clipToBounds = false,
        )
    }
    shown?.let { source ->
        LaunchedEffect(source) {
            // From the memory cache it is drawn at once; otherwise it cross-fades in first.
            if (source != DataSource.MEMORY_CACHE) delay(COVER_FADE_MS)
            covered = true
        }
    }
}

/** A little longer than the image loader's cross-fade (AtticApp). */
private const val COVER_FADE_MS = 200L

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
    val pictures = arts.take(4)
    if (LocalBakedCovers.current && SuperellipseMask.canBake(shape) && pictures.all { it is Art.Song }) {
        BakedMosaic(pictures.map { (it as Art.Song).mediaId }, modifier, size, shape)
        return
    }
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

/**
 * A [Mosaic] drawn with no clip: each quarter's picture has its part of the box's outline baked in,
 * and each quarter fills its part of the shape until its picture has faded in (or for good when it
 * has none).
 */
@Composable
private fun BakedMosaic(mediaIds: List<Long>, modifier: Modifier, size: Dp?, shape: Shape) {
    val fill = Comfort.colors.fill
    val density = LocalDensity.current.density
    val context = LocalPlatformContext.current
    BoxWithConstraints(modifier.then(if (size != null) Modifier.size(size) else Modifier.aspectRatio(1f))) {
        val boxWidth = constraints.maxWidth
        val boxHeight = constraints.maxHeight
        val boxSize = Size(boxWidth.toFloat(), boxHeight.toFloat())
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 2) {
                Row(Modifier.weight(1f)) {
                    for (column in 0 until 2) {
                        val mediaId = mediaIds[row * 2 + column]
                        val tile = MosaicTile(boxWidth, boxHeight, column, row)
                        val request = remember(context, mediaId, shape, density, tile) {
                            ImageRequest.Builder(context).data(ArtModel(mediaId))
                                .transformations(SuperellipseMask(shape, density, tile)).build()
                        }
                        var shown by remember(mediaId) { mutableStateOf<DataSource?>(null) }
                        var covered by remember(mediaId) { mutableStateOf(false) }
                        Box(
                            Modifier.weight(1f).fillMaxSize().drawWithCache {
                                val mask = shapeMask(shape, boxSize)
                                val tint = ColorFilter.tint(fill)
                                val left = if (column == 0) 0 else boxWidth - this.size.width.roundToInt()
                                val top = if (row == 0) 0 else boxHeight - this.size.height.roundToInt()
                                val part = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt())
                                onDrawBehind {
                                    if (!covered) drawImage(mask, srcOffset = IntOffset(left, top), srcSize = part, dstSize = part, colorFilter = tint)
                                }
                            },
                        ) {
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onState = { state -> if (state is AsyncImagePainter.State.Success) shown = state.result.dataSource },
                                clipToBounds = false,
                            )
                        }
                        shown?.let { source ->
                            LaunchedEffect(source) {
                                if (source != DataSource.MEMORY_CACHE) delay(COVER_FADE_MS)
                                covered = true
                            }
                        }
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
    FilledArtBox(modifier, size, background) { width -> ComfortIcon(symbol, size = width * 0.46f, filled = filled, tint = ink) }
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
    FilledArtBox(modifier, size, background) { width -> AtticMarkIcon(size = width * 0.5f, tint = ink) }
}

@Composable
private fun iconBoxColors(style: IconBoxStyle): Pair<Color, Color> {
    val colors = Comfort.colors
    // `.sheet .ibox.neutral`: on a dark sheet the neutral box is one step up, so it shows on the raised surface.
    val neutral = if (colors.dark && LocalComfortContainer.current == ComfortContainer.Sheet) colors.fill2 else colors.fill
    return when (style) {
        IconBoxStyle.Tonal -> colors.accentSoft to colors.accentInk
        IconBoxStyle.Neutral -> neutral to colors.on2
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

/**
 * An [ArtShape] filled with [background], holding [content] centred: an icon box. The content sits
 * well inside the shape, so the shape is drawn as a fill rather than clipped to. [content] gets the
 * box's width.
 */
@Composable
private fun FilledArtBox(modifier: Modifier, size: Dp?, background: Color, content: @Composable (Dp) -> Unit) {
    Box(
        modifier
            .then(if (size != null) Modifier.size(size) else Modifier.aspectRatio(1f))
            .drawWithCache {
                val mask = shapeMask(ArtShape, this.size)
                val tint = ColorFilter.tint(background)
                onDrawBehind { drawImage(mask, colorFilter = tint) }
            },
        contentAlignment = Alignment.Center,
    ) { WithWidth(size, content) }
}

/**
 * [shape] at [size] as an alpha mask with the clip's own edge coverage ([superellipseCoverage]), for
 * filling the shape: drawn tinted, it lands on exactly the pixels a clipped fill did.
 */
private fun CacheDrawScope.shapeMask(shape: Shape, size: Size): ImageBitmap {
    val width = size.width.roundToInt()
    val height = size.height.roundToInt()
    return coverageBitmap(SuperellipseMask.resolve(shape, width, height, density), width, height).asImageBitmap()
}

/**
 * Runs [content] with the width a box of [size] is laid out at: [size] rounded to whole pixels, as
 * the layout rounds it, so it matches what [BoxWithConstraints] would give without subcomposing.
 * With no [size] the box takes its width from its parent and only [BoxWithConstraints] knows it.
 */
@Composable
private fun WithWidth(size: Dp?, content: @Composable (Dp) -> Unit) {
    if (size != null) {
        val density = LocalDensity.current
        content(with(density) { size.roundToPx().toDp() })
    } else {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content(maxWidth) }
    }
}

@Composable
private fun NoArtGlyph(size: Dp?) {
    WithWidth(size) { width -> ComfortIcon(Symbol.MusicNote, size = width * 0.46f, tint = Comfort.colors.on2) }
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
