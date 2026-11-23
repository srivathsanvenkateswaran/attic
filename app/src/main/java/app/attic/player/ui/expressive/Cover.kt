package app.attic.player.ui.expressive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import app.attic.player.ui.ArtModel
import coil3.compose.AsyncImage

/** Where a cover's picture comes from. */
@Immutable
sealed interface Art {
    /** A song's embedded cover, by its MediaStore id. */
    data class Song(val mediaId: Long) : Art

    /** A picture already in memory: previews and the kit gallery. */
    data class Picture(val bitmap: ImageBitmap) : Art

    companion object {
        fun of(mediaId: Long?): Art? = mediaId?.let(::Song)
    }
}

/**
 * An album, playlist or folder cover (`.cv`): a rounded square whose radius follows its size. Give
 * [size] for a fixed cover; leave it null to fill the width the parent gives, as a square. A file with
 * no art shows the music-note placeholder (`.no-art`).
 */
@Composable
fun Cover(art: Art?, modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = CoverShape) {
    CoverBox(modifier, size, shape) {
        NoArtGlyph()
        CoverPicture(art)
    }
}

/** An artist (`.cv.cookie`): the nine-lobed cookie at every size. Never a circle. */
@Composable
fun ArtistCover(art: Art?, modifier: Modifier = Modifier, size: Dp? = null) =
    Cover(art, modifier, size, CookieShape)

/**
 * A folder or playlist (`.mosaic`): four covers in a 2×2 grid when there are four or more, else the
 * first one, else the placeholder. Pass covers from different albums (see `mosaicIds` in ui/Components.kt).
 */
@Composable
fun Mosaic(arts: List<Art>, modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = CoverShape) {
    if (arts.size < 4) return Cover(arts.firstOrNull(), modifier, size, shape)
    CoverBox(modifier, size, shape) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 2) {
                Row(Modifier.weight(1f)) {
                    for (column in 0 until 2) {
                        Box(Modifier.weight(1f).fillMaxSize()) { CoverPicture(arts[row * 2 + column]) }
                    }
                }
            }
        }
    }
}

/** Favourites (`.fav-art`): a filled heart on the tertiary container, the one place that colour is spent. */
@Composable
fun FavouritesArt(modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = CoverShape) {
    val colors = MaterialTheme.colorScheme
    CoverBox(modifier, size, shape, colors.tertiaryContainer) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SymbolIcon(Symbol.Favorite, size = maxWidth * 0.5f, filled = true, tint = colors.onTertiaryContainer)
        }
    }
}

/** The placeholder for a file with no cover (`.no-art`). */
@Composable
fun NoArt(modifier: Modifier = Modifier, size: Dp? = null, shape: Shape = CoverShape) {
    CoverBox(modifier, size, shape) { NoArtGlyph() }
}

@Composable
private fun CoverBox(
    modifier: Modifier,
    size: Dp?,
    shape: Shape,
    background: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit,
) {
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
        SymbolIcon(Symbol.MusicNote, size = maxWidth * 0.46f, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoverPicture(art: Art?) {
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
