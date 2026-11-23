package app.attic.player.playback

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import app.attic.player.MainActivity
import app.attic.player.R
import app.attic.player.attic
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

/**
 * The Android side of the car's browse tree: it reads a [BrowseSnapshot] from the app, turns
 * [BrowseTree] nodes into media items with cover URIs and layout hints, and reads voice requests.
 */
@OptIn(UnstableApi::class)
internal class CarBrowser(private val service: PlaybackService) {
    private val app get() = service.attic

    /** How many tabs each car said it can show, by package. */
    private val tabLimits = ConcurrentHashMap<String, Int>()

    val labels: BrowseLabels by lazy {
        BrowseLabels(
            home = service.getString(R.string.tab_home),
            favourites = service.getString(R.string.favourites),
            playlists = service.getString(R.string.library_filter_playlists),
            albums = service.getString(R.string.library_filter_albums),
            artists = service.getString(R.string.library_filter_artists),
            instantMixes = service.getString(R.string.home_instant_mixes),
            recentlyPlayed = service.getString(R.string.home_recently_played),
            shuffleAll = service.getString(R.string.library_shuffle_all_cd),
            songsGroup = service.getString(R.string.library_filter_songs),
            songCount = { service.resources.getQuantityString(R.plurals.song_count, it, it.toString()) },
        )
    }

    suspend fun snapshot(refreshMixes: Boolean = false): BrowseSnapshot {
        val library = app.library.await()
        if (refreshMixes) runCatching { app.radio.refreshSpeedDial() }
        return BrowseSnapshot(
            library = library,
            likedKeys = app.userData.likedKeys.value,
            playlists = app.userData.playlists.value,
            recentKeys = app.userData.recentKeys.first(),
            mixes = app.radio.speedDial.value,
        )
    }

    fun root(browser: MediaSession.ControllerInfo, params: LibraryParams?): LibraryResult<MediaItem> {
        params?.extras?.getInt(MediaConstants.EXTRAS_KEY_ROOT_CHILDREN_LIMIT, 0)?.takeIf { it > 0 }?.let { tabLimits[browser.packageName] = it }
        val extras = Bundle().apply {
            putBoolean(SEARCH_SUPPORTED, true)
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        }
        val root = MediaItem.Builder()
            .setMediaId(BrowseId.ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build(),
            )
            .build()
        return LibraryResult.ofItem(root, LibraryParams.Builder().setExtras(extras).build())
    }

    suspend fun children(
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
        if (!app.library.hasPermission()) return needsPermission()
        val isHome = parentId == BrowseId.tab(BrowseNode.Tab.Home)
        val limit = tabLimits[browser.packageName] ?: BrowseTree.DEFAULT_TAB_LIMIT
        val nodes = BrowseTree.children(parentId, snapshot(refreshMixes = isHome), labels, limit)
            ?: return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        return LibraryResult.ofItemList(BrowseTree.page(nodes, page, pageSize).map(::toMediaItem), params)
    }

    suspend fun item(id: String): LibraryResult<MediaItem> {
        val node = BrowseTree.item(id, snapshot(), labels) ?: return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        return LibraryResult.ofItem(toMediaItem(node), null)
    }

    suspend fun search(query: String): List<MediaItem> = BrowseTree.search(query, snapshot(), labels).map(::toMediaItem)

    /** What a car row asks to play, or null when [item] is a song key or anything else that isn't a row. */
    suspend fun requestFor(item: MediaItem): PlayRequest? {
        val id = item.mediaId
        // The app's own queues are song keys; only build a snapshot for something that parses as a row.
        if (id.isEmpty() || BrowseId.parse(id) == null || app.library.await().trackByKey.containsKey(id)) return null
        return BrowseTree.playRequest(id, snapshot(), labels)
    }

    /** A spoken "play …", which arrives as an item with a search query and no id. */
    fun voiceRequest(item: MediaItem): VoiceRequest? {
        val query = item.requestMetadata.searchQuery ?: return null
        if (item.mediaId.isNotEmpty()) return null
        val extras = item.requestMetadata.extras
        return VoiceRequest(
            query = query,
            focus = VoiceRequest.focusOf(extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)),
            artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST),
            album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM),
            title = extras?.getString(MediaStore.EXTRA_MEDIA_TITLE),
            playlist = extras?.getString(PLAYLIST_EXTRA),
            genre = extras?.getString(MediaStore.EXTRA_MEDIA_GENRE),
        )
    }

    fun notFound(query: String) = SessionError(SessionError.ERROR_NOT_SUPPORTED, service.getString(R.string.car_not_found, query))

    /**
     * Without audio access there is nothing to show. The car shows this message, and on a car running
     * Android itself a button that opens Attic; on a phone screen the driver is told to look later.
     */
    private fun <T : Any> needsPermission(): LibraryResult<T> {
        val open = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val extras = Bundle().apply {
            putString(MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT, service.getString(R.string.car_open_attic))
            putParcelable(MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT, open)
        }
        // Only this code (and parental control) is copied to the car's playback state, which is where it reads errors.
        return LibraryResult.ofError<T>(SessionError(SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED, service.getString(R.string.car_needs_permission), extras))
    }

    private fun toMediaItem(node: BrowseNode): MediaItem {
        val extras = Bundle()
        node.groupTitle?.let { extras.putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it) }
        node.childStyle?.let { style ->
            val value = if (style == BrowseNode.Style.Grid) MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM else MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, value)
            extras.putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        }
        val artwork = node.tab?.let(::tabIcon) ?: node.coverId?.takeIf { it >= 0 }?.let(ArtProvider::uriFor)
        val metadata = MediaMetadata.Builder()
            .setTitle(node.title)
            .setDisplayTitle(node.title)
            .setSubtitle(node.subtitle)
            .setArtist(node.track?.artist ?: node.subtitle)
            .setAlbumTitle(node.track?.album)
            .setDurationMs(node.track?.durationMs)
            .setArtworkUri(artwork)
            .setIsPlayable(node.playable)
            .setIsBrowsable(node.browsable)
            .setMediaType(mediaType(node.kind))
            .setExtras(extras)
            .build()
        return MediaItem.Builder().setMediaId(node.id).setMediaMetadata(metadata).build()
    }

    /** White vector icons, which the car tints; referenced by id so resource shrinking keeps them. */
    private fun tabIcon(tab: BrowseNode.Tab): Uri {
        val id = when (tab) {
            BrowseNode.Tab.Home -> R.drawable.ic_auto_home
            BrowseNode.Tab.Favourites -> R.drawable.ic_auto_favourites
            BrowseNode.Tab.Playlists -> R.drawable.ic_auto_playlists
            BrowseNode.Tab.Albums -> R.drawable.ic_auto_albums
            BrowseNode.Tab.Artists -> R.drawable.ic_auto_artists
        }
        val res = service.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(res.getResourcePackageName(id))
            .appendPath(res.getResourceTypeName(id))
            .appendPath(res.getResourceEntryName(id))
            .build()
    }

    private fun mediaType(kind: BrowseNode.Kind): Int = when (kind) {
        BrowseNode.Kind.Root, BrowseNode.Kind.Tab -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        BrowseNode.Kind.Album -> MediaMetadata.MEDIA_TYPE_ALBUM
        BrowseNode.Kind.Artist -> MediaMetadata.MEDIA_TYPE_ARTIST
        BrowseNode.Kind.Playlist -> MediaMetadata.MEDIA_TYPE_PLAYLIST
        BrowseNode.Kind.Song, BrowseNode.Kind.Mix, BrowseNode.Kind.ShuffleAll -> MediaMetadata.MEDIA_TYPE_MUSIC
    }

    companion object {
        /** The legacy browser extra that turns on the car's search button. Media3 has no constant for it. */
        private const val SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

        /** MediaStore.EXTRA_MEDIA_PLAYLIST, deprecated in the SDK but still what assistants send. */
        private const val PLAYLIST_EXTRA = "android.intent.extra.playlist"

        /** The lists a car may be showing that a rescan, a favourite or a playlist edit can change. */
        val refreshedIds: List<String> = listOf(BrowseId.ROOT) + BrowseNode.Tab.entries.map(BrowseId::tab)
    }
}
