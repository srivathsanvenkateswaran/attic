package app.attic.player.debug

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import app.attic.player.analysis.AnalysisWorker
import app.attic.player.attic
import app.attic.player.data.UiStyle
import app.attic.player.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Debug builds only: puts the surfaces outside the app into a state worth checking, without playing a
 * sound. "prepare" readies the current queue paused, which is enough for the media notification and
 * the lock-screen card; "like" toggles the current song's favourite through the notification's own
 * command; "style" switches the UI style exactly as Settings does, so every surface restyles live;
 * "analysis" posts the analysis progress notification as the worker builds it (84 of 127) without
 * analysing anything, and "analysis-clear" (or its Stop) removes it. "release" lets go of the
 * controller "prepare" holds. "widget-shot" (--ez night, --es name) saves the widget drawn off screen
 * (see WidgetSnapshot).
 *
 * adb shell am broadcast -n app.attic.player/.debug.SystemSurfacesReceiver -a prepare
 * adb shell am broadcast -n app.attic.player/.debug.SystemSurfacesReceiver -a like
 * adb shell am broadcast -n app.attic.player/.debug.SystemSurfacesReceiver -a style --es style Comfort
 */
class SystemSurfacesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val notifications = app.getSystemService(NotificationManager::class.java)
        when (intent.action) {
            "style" -> {
                UiStyle.entries.firstOrNull { it.name == intent.getStringExtra("style") }?.let(app.attic.settings::setUiStyle)
                return
            }
            // The analysis notification exactly as the worker builds it, without running an analysis.
            "analysis" -> {
                val stop = PendingIntent.getBroadcast(app, 0, Intent("analysis-clear").setClass(app, SystemSurfacesReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
                notifications.notify(PREVIEW_ID, AnalysisWorker.progressNotification(app, 84, 127, stop))
                return
            }
            "analysis-clear" -> {
                notifications.cancel(PREVIEW_ID)
                return
            }
        }
        if (intent.action == "widget-shot") {
            val pending = goAsync()
            app.attic.scope.launch {
                try {
                    WidgetSnapshot.take(app, intent.getBooleanExtra("night", false), intent.getStringExtra("name") ?: "shot")
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (intent.action == "release") {
            held?.release()
            held = null
            return
        }
        val pending = goAsync()
        app.attic.scope.launch {
            try {
                val controller = held ?: MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java)))
                    .buildAsync().await()
                // Held until "release": an unbound, paused service would stop and take its notification with it.
                held = controller
                when (intent.action) {
                    "prepare" -> {
                        // The saved queue is restored once the library has loaded.
                        withTimeoutOrNull(10_000) { while (controller.mediaItemCount == 0) delay(200) }
                        controller.pause()
                        controller.prepare()
                    }
                    "like" -> controller.sendCustomCommand(SessionCommand("app.attic.player.LIKE", Bundle.EMPTY), Bundle.EMPTY).await()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val PREVIEW_ID = 2901
        var held: MediaController? = null
    }
}
