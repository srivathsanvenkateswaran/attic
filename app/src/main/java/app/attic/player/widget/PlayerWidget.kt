package app.attic.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.Size
import android.util.SizeF
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.attic.player.MainActivity
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.LibraryRepository
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlaybackService
import app.attic.player.ui.comfort.ComfortShapes
import app.attic.player.ui.comfort.SuperellipseShape
import app.attic.player.ui.coverSwatch
import app.attic.player.ui.coverTint
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

data class WidgetState(
    val title: String?,
    val artist: String?,
    val mediaId: Long?,
    val isPlaying: Boolean,
)

/**
 * Home-screen player. One widget that reshapes as it's resized: the cover and play when small, a
 * single row at 4x1, and the song with five album shortcuts at 4x2 and up. Each UI style has its
 * own three layouts (pixel/ and oneui/ 10-system, frame A). Colours come
 * from the cover by the style's rule, in light and dark, and the launcher picks by night mode.
 */
class PlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        context.attic.scope.launch {
            try {
                render(context, read(context))
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_NEXT && action != ACTION_PREVIOUS) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        // A receiver's own context isn't allowed to bind to the service.
        val app = context.applicationContext
        context.attic.scope.launch {
            try {
                val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
                val controller = MediaController.Builder(app, token).buildAsync().await()
                if (action == ACTION_NEXT) controller.seekToNextMediaItem() else controller.seekToPrevious()
                controller.release()
            } finally {
                pending.finish()
            }
        }
    }

    private enum class Layout { Small, Wide, Large }

    /** Both themes' colours. */
    private class Look(val light: WidgetPalette, val dark: WidgetPalette)

    /** The art for one render, already cut to the style's shapes. Null where a file has no cover. */
    private class Art(val cover: Bitmap?, val smallCover: Bitmap?, val tiles: List<Tile>)

    private class Tile(val albumKey: String, val art: Bitmap?)

    companion object {
        const val EXTRA_ALBUM_KEY = "app.attic.player.widget.ALBUM"
        private const val TILE_PX = 160
        private const val TILE_REQUEST_BASE = 100
        private val TILE_IDS = listOf(R.id.widget_tile1, R.id.widget_tile2, R.id.widget_tile3, R.id.widget_tile4, R.id.widget_tile5)
        private const val ACTION_NEXT = "app.attic.player.widget.NEXT"
        private const val ACTION_PREVIOUS = "app.attic.player.widget.PREVIOUS"
        private const val PREFS = "player_widget"
        private const val COVER_PX = 360

        /** The smallest size each layout is drawn at; the launcher picks the largest that fits. */
        private val SMALL = SizeF(110f, 110f)
        private val WIDE = SizeF(180f, 50f)
        private val LARGE = SizeF(250f, 180f)

        /**
         * Comfort's 4 x 2 is drawn to the mock's measure, 14 + 88 + 12 over 1 + 12 + 56 + 14 = 197dp, and
         * is clipped below that, so a shorter widget gets the 4 x 1 layout instead.
         */
        private val COMFORT_LARGE = SizeF(250f, 197f)

        /** The real state while the service has published in this process. */
        private var live: WidgetState? = null
        private var lastCoverId: Long? = null
        private var lastCover: Bitmap? = null

        /** What [publish] last drew, in which style, so a repeat of the same state draws nothing. */
        private var lastPublished: Pair<WidgetState, UiStyle>? = null

        /** Comfort's cuts of [lastCover], by shape, so a redraw for the same song cuts nothing. */
        private val coverCuts = ConcurrentHashMap<SuperellipseShape, Bitmap>()

        /** Saves the state so a widget added later, or after a reboot, still shows the last song. */
        fun publish(context: Context, state: WidgetState) {
            live = state
            // The player reports several events for one change (a new queue: timeline, then item,
            // then playing), and each redraw sends every cover across to the launcher again.
            val drawn = state to context.attic.settings.uiStyle.value
            if (drawn == lastPublished) return
            lastPublished = drawn
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", state.title)
                .putString("artist", state.artist)
                .putLong("mediaId", state.mediaId ?: -1L)
                .putBoolean("playing", state.isPlaying)
                .apply()
            context.attic.scope.launch { render(context, state) }
        }

        /** Redraws every widget as things stand, for when the UI style changes in Settings. */
        fun refresh(context: Context) {
            context.attic.scope.launch { render(context, read(context)) }
        }

        private fun read(context: Context): WidgetState {
            live?.let { return it }
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return WidgetState(
                title = prefs.getString("title", null),
                artist = prefs.getString("artist", null),
                mediaId = prefs.getLong("mediaId", -1L).takeIf { it >= 0 },
                // Only reached in a fresh process, so nothing can be playing yet.
                isPlaying = false,
            )
        }

        private suspend fun render(context: Context, state: WidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, RemoteViews(views(context, state)))
        }

        /** What the widget shows for [state] in the current style: a layout per smallest size, the launcher picks. */
        internal suspend fun views(context: Context, state: WidgetState = read(context)): Map<SizeF, RemoteViews> {
            val comfort = context.attic.settings.uiStyle.value == UiStyle.Comfort
            val cover = cover(context, state.mediaId)
            val tiles = recentAlbums(context)
            val (look, art) = withContext(Dispatchers.Default) {
                look(context, comfort, state.mediaId) to art(comfort, cover, tiles)
            }
            return mapOf(
                SMALL to build(context, comfort, Layout.Small, state, art, look),
                WIDE to build(context, comfort, Layout.Wide, state, art, look),
                (if (comfort) COMFORT_LARGE else LARGE) to build(context, comfort, Layout.Large, state, art, look),
            )
        }

        /**
         * Expressive takes the cover's scheme from its tint, as FromArt does, or the wallpaper's scheme
         * with no cover. Comfort washes its card toward the cover's swatch, as Tinted does.
         */
        private fun look(context: Context, comfort: Boolean, mediaId: Long?): Look {
            if (comfort) {
                val swatch = mediaId?.let { coverSwatch(context, it) }
                return Look(WidgetColors.comfort(swatch, dark = false), WidgetColors.comfort(swatch, dark = true))
            }
            val tint = mediaId?.let { coverTint(context, it) }
            return if (tint != null) {
                Look(WidgetColors.expressive(tint, dark = false), WidgetColors.expressive(tint, dark = true))
            } else {
                Look(WidgetColors.expressive(dynamicLightColorScheme(context)), WidgetColors.expressive(dynamicDarkColorScheme(context)))
            }
        }

        /** Expressive clips art with the views' rounded outlines. Comfort's superellipse has to be cut into the bitmap. */
        private fun art(comfort: Boolean, cover: Bitmap?, tiles: List<Tile>): Art =
            if (!comfort) {
                Art(cover, cover, tiles)
            } else {
                Art(
                    cover = cover?.let { cutTo(it, ComfortShapes.Squircle) },
                    // The 2 x 2 cover is drawn at about 148dp, where the kit switches to the larger curve.
                    smallCover = cover?.let { cutTo(it, ComfortShapes.SquircleLarge) },
                    tiles = tiles.map { Tile(it.albumKey, it.art?.let { art -> cutTo(art, ComfortShapes.Squircle) }) },
                )
            }

        /**
         * The centre square of [source] inside [shape], transparent outside it. Immutable: RemoteViews
         * copies a mutable bitmap into shared memory for every layout it is set on, at every redraw
         * (17 to 25MB at launch, held until the next garbage collection), and sends an immutable one as it is.
         */
        private fun cutTo(source: Bitmap, shape: SuperellipseShape): Bitmap {
            if (source === lastCover) coverCuts[shape]?.let { return it }
            return cutOnce(source, shape).also { if (source === lastCover) coverCuts[shape] = it }
        }

        private fun cutOnce(source: Bitmap, shape: SuperellipseShape): Bitmap {
            val size = min(source.width, source.height)
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setTranslate(-(source.width - size) / 2f, -(source.height - size) / 2f) })
            }
            val points = shape.unitPoints
            val path = Path()
            for (i in 0 until points.size / 2) {
                val x = points[i * 2] * size
                val y = points[i * 2 + 1] * size
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            Canvas(out).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
            return out.copy(Bitmap.Config.ARGB_8888, false).also { out.recycle() }
        }

        private suspend fun cover(context: Context, mediaId: Long?): Bitmap? {
            if (mediaId == null) return null
            if (mediaId == lastCoverId) return lastCover
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(LibraryRepository.uriFor(mediaId), Size(COVER_PX, COVER_PX), null)
                }.getOrNull()
            }
            lastCoverId = mediaId
            lastCover = bitmap
            coverCuts.clear()
            return bitmap
        }

        /** The albums you played last, topped up with your biggest ones so the row is never empty. */
        private suspend fun recentAlbums(context: Context): List<Tile> {
            val app = context.attic
            val library = app.library.await()
            val recent = app.userData.recentKeys.first().mapNotNull { library.trackByKey[it]?.albumKey }.distinct()
            val keys = (recent + library.albums.sortedByDescending { it.tracks.size }.map { it.key }).distinct().take(TILE_IDS.size)
            return withContext(Dispatchers.IO) {
                keys.mapNotNull { key ->
                    val album = library.albumByKey[key] ?: return@mapNotNull null
                    val art = runCatching {
                        context.contentResolver.loadThumbnail(LibraryRepository.uriFor(album.tracks.first().mediaId), Size(TILE_PX, TILE_PX), null)
                    }.getOrNull()
                    Tile(key, art)
                }
            }
        }

        private fun layoutFor(comfort: Boolean, layout: Layout): Int = when (layout) {
            Layout.Small -> if (comfort) R.layout.widget_comfort_small else R.layout.widget_expressive_small
            Layout.Wide -> if (comfort) R.layout.widget_comfort_wide else R.layout.widget_expressive_wide
            Layout.Large -> if (comfort) R.layout.widget_comfort_large else R.layout.widget_expressive_large
        }

        private fun build(context: Context, comfort: Boolean, layout: Layout, state: WidgetState, art: Art, look: Look): RemoteViews {
            val views = RemoteViews(context.packageName, layoutFor(comfort, layout))
            views.backgroundTint(android.R.id.background, look) { it.container }
            views.setOnClickPendingIntent(
                android.R.id.background,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            // Expressive's small size has a title; Comfort's, like today's, has none.
            if (layout != Layout.Small || !comfort) {
                views.setTextViewText(R.id.widget_title, state.title ?: context.getString(R.string.app_name))
                views.color(R.id.widget_title, "setTextColor", look) { it.title }
            }
            if (layout != Layout.Small) {
                views.setTextViewText(R.id.widget_artist, state.artist ?: context.getString(R.string.widget_empty))
                views.color(R.id.widget_artist, "setTextColor", look) { it.artist }
                for (id in listOf(R.id.widget_prev, R.id.widget_next)) {
                    views.color(id, "setColorFilter", look) { it.onSkip }
                    if (!comfort) views.backgroundTint(id, look) { it.skip ?: it.container }
                }
                views.setOnClickPendingIntent(R.id.widget_prev, skip(context, ACTION_PREVIOUS))
                views.setOnClickPendingIntent(R.id.widget_next, skip(context, ACTION_NEXT))
            }

            val cover = if (layout == Layout.Small) art.smallCover else art.cover
            views.artOrMark(R.id.widget_art, cover, comfort, if (layout == Layout.Small) R.drawable.widget_squircle_large else R.drawable.widget_squircle, look)

            // The play control: the kit's own in each style. Expressive's squares off while playing.
            views.setImageViewResource(R.id.widget_play, if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            if (!comfort) {
                views.setInt(
                    R.id.widget_play,
                    "setBackgroundResource",
                    if (state.isPlaying) R.drawable.widget_expressive_play_playing else R.drawable.widget_expressive_play_paused,
                )
            }
            views.backgroundTint(R.id.widget_play, look) { it.play }
            views.color(R.id.widget_play, "setColorFilter", look) { it.onPlay }
            views.setOnClickPendingIntent(R.id.widget_play, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            if (comfort && layout == Layout.Small) views.backgroundTint(R.id.widget_play_ring, look) { it.container }

            if (layout == Layout.Large) {
                views.color(R.id.widget_mark, "setColorFilter", look) { it.mark }
                if (comfort) views.color(R.id.widget_line, "setColorFilter", look) { it.line }
                TILE_IDS.forEachIndexed { index, id ->
                    val tile = art.tiles.getOrNull(index)
                    views.artOrMark(id, tile?.art, comfort, R.drawable.widget_squircle, look)
                    if (tile != null) {
                        views.setOnClickPendingIntent(
                            id,
                            PendingIntent.getActivity(
                                context,
                                TILE_REQUEST_BASE + index,
                                Intent(context, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .putExtra(EXTRA_ALBUM_KEY, tile.albumKey),
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                            ),
                        )
                    }
                }
            }
            return views
        }

        /**
         * A cover, or with none the style's empty art box: Expressive's `.no-art` (the view's own rounded
         * fill) and Comfort's neutral `.ibox` (a superellipse [box]), each with the mark in it.
         */
        private fun RemoteViews.artOrMark(id: Int, bitmap: Bitmap?, comfort: Boolean, box: Int, look: Look) {
            if (bitmap != null) {
                setImageViewBitmap(id, bitmap)
                return
            }
            if (comfort) setInt(id, "setBackgroundResource", box)
            backgroundTint(id, look) { it.empty }
            setImageViewResource(id, R.drawable.ic_launcher_monochrome)
            color(id, "setColorFilter", look) { it.onEmpty }
        }

        /** Calls [method] with the light colour, or the dark one when the launcher is in night mode. */
        private fun RemoteViews.color(id: Int, method: String, look: Look, pick: (WidgetPalette) -> Int) =
            setColorInt(id, method, pick(look.light), pick(look.dark))

        private fun RemoteViews.backgroundTint(id: Int, look: Look, pick: (WidgetPalette) -> Int) =
            setColorStateList(id, "setBackgroundTintList", ColorStateList.valueOf(pick(look.light)), ColorStateList.valueOf(pick(look.dark)))

        /** Media3's button receiver only accepts play keys, so skipping uses a short-lived controller instead. */
        private fun skip(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(action).setClass(context, PlayerWidget::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )

        /** Goes through the media button receiver, so it also restarts playback after the service has died. */
        @OptIn(UnstableApi::class)
        private fun mediaKey(context: Context, keyCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                keyCode,
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setClass(context, MediaButtonReceiver::class.java)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
