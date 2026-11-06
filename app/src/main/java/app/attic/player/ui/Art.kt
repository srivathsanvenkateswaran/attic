package app.attic.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette as ColorPalette
import app.attic.player.data.LibraryRepository
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Coil model for a track's embedded cover. */
data class ArtModel(val mediaId: Long)

class ArtKeyer : Keyer<ArtModel> {
    override fun key(data: ArtModel, options: Options) = "art:${data.mediaId}"
}

class ArtFetcher(
    private val data: ArtModel,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val target = options.size.width.pxOrElse { DEFAULT_PX }
        val bitmap = loadCover(options.context, data.mediaId, target)
            ?: throw IOException("No embedded art for ${data.mediaId}")
        return ImageFetchResult(bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory : Fetcher.Factory<ArtModel> {
        override fun create(data: ArtModel, options: Options, imageLoader: ImageLoader) = ArtFetcher(data, options)
    }

    private companion object {
        const val DEFAULT_PX = 512
    }
}

/** MediaStore keeps its own thumbnail cache, so lists use it. Only large views decode the original. */
private fun loadCover(context: Context, mediaId: Long, targetPx: Int): Bitmap? {
    if (mediaId < 0) return null
    val uri = LibraryRepository.uriFor(mediaId)
    if (targetPx <= THUMBNAIL_MAX_PX) {
        runCatching {
            return context.contentResolver.loadThumbnail(uri, Size(targetPx, targetPx), null)
        }
    }
    return runCatching {
        val bytes = MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture
            } finally {
                retriever.release()
            }
        } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}

private const val THUMBNAIL_MAX_PX = 640
private val artColors = LruCache<Long, Int>(256)
private const val NO_COLOR = 0

/** A dark tint pulled from the cover, for gradients behind album headers and Now Playing. */
@Composable
fun rememberArtColor(mediaId: Long?, fallback: Color = Palette.PlumHigh): Color {
    val context = LocalContext.current
    val color by produceState(
        initialValue = mediaId?.let { artColors.get(it) }?.takeIf { it != NO_COLOR }?.let { Color(it) } ?: fallback,
        mediaId,
    ) {
        if (mediaId == null) {
            value = fallback
            return@produceState
        }
        val cached = artColors.get(mediaId)
        val argb = cached ?: withContext(Dispatchers.IO) {
            val bitmap = loadCover(context, mediaId, 96)
            val swatch = bitmap?.let { bmp ->
                val palette = ColorPalette.from(bmp.copy(Bitmap.Config.ARGB_8888, false)).generate()
                palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
            }
            swatch?.let { deepen(it.rgb) } ?: NO_COLOR
        }.also { artColors.put(mediaId, it) }
        value = if (argb == NO_COLOR) fallback else Color(argb)
    }
    return color
}

/** Keeps the cover's hue but holds lightness low so white text stays readable on top. */
private fun deepen(rgb: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(rgb, hsl)
    hsl[1] = hsl[1].coerceAtMost(0.62f)
    hsl[2] = hsl[2].coerceIn(0.2f, 0.3f)
    return ColorUtils.HSLToColor(hsl)
}

/** Embedded cover, with a note icon behind it that shows through when a file has no art. */
@Composable
fun CoverArt(
    mediaId: Long?,
    shape: Shape,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
        if (mediaId != null) {
            AsyncImage(
                model = ArtModel(mediaId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
