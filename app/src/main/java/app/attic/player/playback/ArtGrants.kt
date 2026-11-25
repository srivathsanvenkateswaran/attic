package app.attic.player.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import java.util.concurrent.ConcurrentHashMap

/**
 * [ArtProvider] isn't exported, so no app can read covers unless Attic lets it. This lets in only the
 * controllers connected to the session (a car, the system's media controls, a watch), and only for the
 * covers Attic hands them: the rows a car browses or searches, and the songs around the one playing.
 *
 * Attic's own process needs no grant: the app, the widget's bitmaps and Media3's notification all load
 * covers here, and the notification's loader runs in this process too.
 */
class ArtGrants(private val context: Context) {
    /** Package and URI pairs already granted, so a row browsed twice costs no second call to the system. */
    private val granted = ConcurrentHashMap.newKeySet<Pair<String, Uri>>()

    /** Lets [controller] read the covers of [items]. Other artwork, such as the tabs' icons, needs none. */
    fun grant(controller: MediaSession.ControllerInfo, items: Iterable<MediaItem>) {
        val pkg = controller.packageName
        if (!needsGrant(pkg, context.packageName)) return
        for (uri in coverUris(items)) {
            if (!granted.add(pkg to uri)) continue
            try {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: RuntimeException) {
                // A package the system doesn't know (one that has just been removed): it gets no cover.
                granted.remove(pkg to uri)
                Log.w(TAG, "Couldn't grant a cover to $pkg", e)
            }
        }
    }

    fun grant(controller: MediaSession.ControllerInfo, item: MediaItem) = grant(controller, listOf(item))

    companion object {
        private const val TAG = "ArtGrants"

        /**
         * Media3 names a controller it can't identify by this placeholder rather than a real package.
         * Granting to it would fail, so it gets nothing.
         */
        const val LEGACY_CONTROLLER = "android.media.session.MediaController"

        /** Attic itself reads its own provider, and an unidentified controller can't be granted anything. */
        fun needsGrant(controllerPackage: String, ownPackage: String): Boolean =
            controllerPackage.isNotEmpty() && controllerPackage != ownPackage && controllerPackage != LEGACY_CONTROLLER

        /** The [ArtProvider] covers among [items]' artwork, each once. */
        fun coverUris(items: Iterable<MediaItem>): List<Uri> =
            items.mapNotNull { it.mediaMetadata.artworkUri }
                .filter { it.scheme == "content" && it.authority == ArtProvider.AUTHORITY }
                .distinct()

        /**
         * The queue positions whose covers a controller may show next: the song playing and the
         * [ahead] after it. Granting the whole queue would mean thousands of calls for "Shuffle all".
         */
        fun queueWindow(currentIndex: Int, count: Int, ahead: Int): IntRange {
            if (count <= 0 || currentIndex < 0) return IntRange.EMPTY
            return currentIndex..minOf(count - 1, currentIndex + ahead)
        }
    }
}
