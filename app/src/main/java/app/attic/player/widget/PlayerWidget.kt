package app.attic.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Size
import android.util.SizeF
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.attic.player.MainActivity
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.LibraryRepository
import app.attic.player.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WidgetState(
    val title: String?,
    val artist: String?,
    val mediaId: Long?,
    val isPlaying: Boolean,
)

/**
 * Home-screen player. One widget that reshapes as it's resized: a cover with a play button when
 * small, a single row at 4x1, and a large cover with controls at 4x2 and up.
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

    companion object {
        private const val ACTION_NEXT = "app.attic.player.widget.NEXT"
        private const val ACTION_PREVIOUS = "app.attic.player.widget.PREVIOUS"
        private const val PREFS = "player_widget"
        private const val COVER_PX = 360
        private var lastCoverId: Long? = null
        private var lastCover: Bitmap? = null

        /** Saves the state so a widget added later, or after a reboot, still shows the last song. */
        fun publish(context: Context, state: WidgetState) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", state.title)
                .putString("artist", state.artist)
                .putLong("mediaId", state.mediaId ?: -1L)
                .putBoolean("playing", state.isPlaying)
                .apply()
            context.attic.scope.launch { render(context, state) }
        }

        private fun read(context: Context): WidgetState {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return WidgetState(
                title = prefs.getString("title", null),
                artist = prefs.getString("artist", null),
                mediaId = prefs.getLong("mediaId", -1L).takeIf { it >= 0 },
                // The process may have died since this was saved, so nothing can be playing now.
                isPlaying = false,
            )
        }

        private suspend fun render(context: Context, state: WidgetState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            val cover = cover(context, state.mediaId)
            val views = RemoteViews(
                mapOf(
                    SizeF(110f, 110f) to build(context, R.layout.widget_small, state, cover),
                    SizeF(180f, 50f) to build(context, R.layout.widget_wide, state, cover),
                    SizeF(250f, 110f) to build(context, R.layout.widget_large, state, cover),
                ),
            )
            manager.updateAppWidget(ids, views)
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
            return bitmap
        }

        private fun build(context: Context, layout: Int, state: WidgetState, cover: Bitmap?): RemoteViews {
            val views = RemoteViews(context.packageName, layout)
            if (layout != R.layout.widget_small) {
                views.setTextViewText(R.id.widget_title, state.title ?: context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_artist, state.artist ?: context.getString(R.string.widget_empty))
                views.setOnClickPendingIntent(R.id.widget_prev, skip(context, ACTION_PREVIOUS))
                views.setOnClickPendingIntent(R.id.widget_next, skip(context, ACTION_NEXT))
            }
            if (cover != null) views.setImageViewBitmap(R.id.widget_art, cover) else views.setImageViewResource(R.id.widget_art, R.drawable.widget_placeholder)
            views.setImageViewResource(R.id.widget_play, if (state.isPlaying) R.drawable.widget_pause else R.drawable.widget_play)
            views.setOnClickPendingIntent(R.id.widget_play, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            return views
        }

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
