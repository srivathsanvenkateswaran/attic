package app.attic.player.ui.model

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Library
import app.attic.player.data.TitleCleaner
import app.attic.player.data.Track
import app.attic.player.data.isExternal
import app.attic.player.playback.SleepChoice
import app.attic.player.ui.LocalUi
import app.attic.player.ui.SongShare
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.ui.mosaicIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Track menu

@Immutable
class TrackMenuState(
    val track: Track,
    val liked: Boolean,
    /** The language radio believes the song is in, as its display name ("Unknown" when unset). */
    val languageName: String,
    /** Opened from a playlist, so "Remove from this playlist" is offered. */
    val inPlaylist: Boolean,
    /** Offered only when the album is in the library. */
    val canGoToAlbum: Boolean,
    /** Offered only when one of the song's artists is in the library. */
    val canGoToArtist: Boolean,
    /** "Add to playlist" was chosen, so the playlist picker replaces the menu. */
    val pickingPlaylist: Boolean,
    /** "Language" was chosen, so the language picker replaces the menu. */
    val pickingLanguage: Boolean,
    /** The raw language code, for the language picker's check mark. */
    val language: String?,
    /** The album "Go to album" opens, by name, when it is in the library. */
    val albumName: String? = null,
    /** The artist "Go to artist" opens, by name, when one is in the library. */
    val artistName: String? = null,
    /** A cover of that artist's, for drawing the artist. */
    val artistCover: Long? = null,
    /**
     * Opened from another app and not in the library ("Open with"). The menu then offers only what
     * doesn't file the song anywhere: no radio, queue, favourites, playlists, language or sharing.
     */
    val external: Boolean = false,
    /** Offered for any song in the library: it is shared as its MediaStore file. */
    val canShare: Boolean = false,
    /** The sleep timer, offered only when the menu opens from Now Playing. Null elsewhere. */
    val sleepTimer: SleepTimerMenuState? = null,
    /** "Sleep timer" was chosen, so its choices are showing. */
    val pickingSleepTimer: Boolean = false,
)

/**
 * The sleep timer as the menu shows it. [remainingMs] counts down once a second while the menu is
 * open, and is null for "End of this song" and Off.
 */
@Immutable
data class SleepTimerMenuState(val choice: SleepChoice, val remainingMs: Long?)

/** Each choice does its thing and closes the menu, except the two that open a picker. */
class TrackMenuActions(
    val startRadio: () -> Unit,
    val toggleLike: () -> Unit,
    val playNext: () -> Unit,
    val addToQueue: () -> Unit,
    val pickPlaylist: () -> Unit,
    val pickLanguage: () -> Unit,
    val removeFromPlaylist: () -> Unit,
    val goToAlbum: () -> Unit,
    val goToArtist: () -> Unit,
    /** From the language picker. An empty code (the picker dismissed) changes nothing. */
    val chooseLanguage: (String) -> Unit,
    val dismiss: () -> Unit,
    /** Closes the language picker and leaves the menu open, for a style that draws the picker over the menu. */
    val stopPickingLanguage: () -> Unit = {},
    /** Hands the song's file to the share sheet. */
    val share: () -> Unit = {},
    val pickSleepTimer: () -> Unit = {},
    /** Sets the timer (or turns it off) and closes the menu. */
    val chooseSleepTimer: (SleepChoice) -> Unit = {},
    /** Closes the timer's choices and leaves the menu open, for a style that draws them over the menu. */
    val stopPickingSleepTimer: () -> Unit = {},
)

@Composable
fun rememberTrackMenuModel(request: TrackMenuRequest, library: Library, onDismiss: () -> Unit): UiModel<TrackMenuState, TrackMenuActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val context = LocalContext.current
    val track = request.track
    val liked by app.userData.likedSet.collectAsStateWithLifecycle()
    var pickingPlaylist by remember { mutableStateOf(false) }
    var pickingLanguage by remember { mutableStateOf(false) }
    var pickingSleepTimer by remember { mutableStateOf(false) }
    val player by app.player.state.collectAsStateWithLifecycle()
    val sleepRemaining by produceState<Long?>(null, player.sleepTimer) {
        while (true) {
            value = player.sleepTimer.remainingMs(SystemClock.elapsedRealtime())
            if (value == null) break
            delay(1_000)
        }
    }
    val language by remember(track.key) { app.radio.languageFlow(track) }.collectAsStateWithLifecycle(null)
    val haptics = rememberHaptics()
    val albumKey = track.albumKey?.takeIf { it in library.albumByKey }
    val artistKey = track.artistKeys.firstOrNull { it in library.artistByKey }
    val artist = artistKey?.let { library.artistByKey[it] }

    val state = TrackMenuState(
        track = track,
        liked = track.key in liked,
        languageName = LanguageLabels.displayName(language),
        inPlaylist = request.playlistId != null,
        canGoToAlbum = albumKey != null,
        canGoToArtist = artistKey != null,
        pickingPlaylist = pickingPlaylist,
        pickingLanguage = pickingLanguage,
        language = language,
        albumName = albumKey?.let { library.albumByKey[it]?.name },
        artistName = artist?.name,
        artistCover = artist?.let { a -> a.tracks.firstOrNull { it.key == track.key }?.mediaId ?: a.tracks.firstOrNull()?.mediaId },
        external = track.isExternal,
        canShare = !track.isExternal && track.mediaId >= 0,
        sleepTimer = if (request.fromNowPlaying) SleepTimerMenuState(player.sleepTimer.choice, sleepRemaining) else null,
        pickingSleepTimer = pickingSleepTimer,
    )
    val actions = TrackMenuActions(
        startRadio = {
            app.radio.start(track)
            onDismiss()
        },
        toggleLike = {
            haptics.toggle(track.key !in liked)
            app.userData.toggleLike(track.key)
            onDismiss()
        },
        playNext = {
            app.player.playNext(listOf(track))
            app.messages.tryEmit(app.getString(R.string.message_playing_next))
            onDismiss()
        },
        addToQueue = {
            app.player.addToQueue(listOf(track))
            app.messages.tryEmit(app.getString(R.string.message_added_to_queue))
            onDismiss()
        },
        pickPlaylist = { pickingPlaylist = true },
        pickLanguage = { pickingLanguage = true },
        removeFromPlaylist = {
            request.playlistId?.let { id -> app.userData.removeFromPlaylist(id, track.key) }
            onDismiss()
        },
        goToAlbum = {
            onDismiss()
            albumKey?.let { ui.openAlbum(it) }
        },
        goToArtist = {
            onDismiss()
            artistKey?.let { ui.openArtist(it) }
        },
        chooseLanguage = { choice ->
            app.radio.setLanguage(track.key, choice)
            onDismiss()
        },
        dismiss = onDismiss,
        stopPickingLanguage = { pickingLanguage = false },
        share = {
            onDismiss()
            if (!SongShare.share(context, track)) app.messages.tryEmit(app.getString(R.string.message_share_failed))
        },
        pickSleepTimer = { pickingSleepTimer = true },
        chooseSleepTimer = { choice ->
            haptics.perform(Haptic.Confirm)
            app.player.setSleepTimer(choice)
            app.messages.tryEmit(
                when (choice) {
                    SleepChoice.Off -> app.getString(R.string.message_sleep_timer_off)
                    SleepChoice.EndOfSong -> app.getString(R.string.message_sleep_timer_end_of_song)
                    else -> app.getString(R.string.message_sleep_timer_set, choice.minutes)
                },
            )
            onDismiss()
        },
        stopPickingSleepTimer = { pickingSleepTimer = false },
    )
    return UiModel(state, actions)
}

// Language picker

@Immutable
class LanguagePickerState(
    /** The song's language code, which gets the check mark. */
    val current: String?,
    /** Code and display name, in the order offered. */
    val choices: List<Pair<String, String>>,
    /** The song being corrected, by title. */
    val songTitle: String? = null,
)

class LanguagePickerActions(
    val choose: (String) -> Unit,
    /** Dismissing chooses nothing. */
    val dismiss: () -> Unit,
)

fun languagePickerModel(current: String?, onChoose: (String) -> Unit, songTitle: String? = null): UiModel<LanguagePickerState, LanguagePickerActions> =
    UiModel(
        LanguagePickerState(current, LanguageLabels.CHOICES, songTitle),
        LanguagePickerActions(choose = onChoose, dismiss = { onChoose("") }),
    )

// Playlist picker

@Immutable
class PickerPlaylist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val alreadyIn: Boolean,
    /** Covers for the playlist's mosaic, from different albums. */
    val coverIds: List<Long> = emptyList(),
)

@Immutable
class PlaylistPickerState(
    val playlists: List<PickerPlaylist>,
    /** The new-playlist dialog is open. */
    val creating: Boolean,
    /** What is being added: the song's title when it is one song, else null. */
    val songTitle: String? = null,
    /** How many songs are being added. */
    val songCount: Int = 0,
)

class PlaylistPickerActions(
    /** Adds the songs, says so in a message ("Added to …" or "Already in …"), and closes the picker. */
    val addTo: (PickerPlaylist) -> Unit,
    val startCreating: () -> Unit,
    val dismissCreating: () -> Unit,
    /** Creates the playlist, then adds the songs to it. */
    val create: (String) -> Unit,
    val dismiss: () -> Unit,
)

@Composable
fun rememberPlaylistPickerModel(trackKeys: List<String>, onDismiss: () -> Unit): UiModel<PlaylistPickerState, PlaylistPickerActions> {
    val app = LocalContext.current.attic
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    val library by app.library.library.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun addTo(id: Long, name: String) {
        app.scope.launch {
            val added = app.userData.addToPlaylist(id, trackKeys)
            app.messages.tryEmit(app.getString(if (added > 0) R.string.message_added_to_playlist else R.string.message_already_in_playlist, name))
        }
        onDismiss()
    }

    val state = PlaylistPickerState(
        playlists = playlists.map { playlist ->
            val tracks = library?.let { lib -> playlist.trackKeys.mapNotNull { lib.trackByKey[it] } }.orEmpty()
            PickerPlaylist(playlist.id, playlist.name, playlist.trackKeys.size, playlist.trackKeys.containsAll(trackKeys), mosaicIds(tracks))
        },
        creating = creating,
        songTitle = trackKeys.singleOrNull()?.let { key -> library?.trackByKey?.get(key)?.title },
        songCount = trackKeys.size,
    )
    val actions = PlaylistPickerActions(
        addTo = { addTo(it.id, it.name) },
        startCreating = { creating = true },
        dismissCreating = { creating = false },
        create = { name ->
            creating = false
            scope.launch {
                val id = app.userData.createPlaylist(name)
                addTo(id, name.trim())
            }
        },
        dismiss = onDismiss,
    )
    return UiModel(state, actions)
}

// Playlist name dialog

@Immutable
class PlaylistNameState(val title: String, val confirm: String, val name: String) {
    val canConfirm: Boolean get() = name.isNotBlank()
}

class PlaylistNameActions(
    val setName: (String) -> Unit,
    val confirm: () -> Unit,
    val dismiss: () -> Unit,
)

@Composable
fun rememberPlaylistNameModel(
    title: String,
    confirm: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
): UiModel<PlaylistNameState, PlaylistNameActions> {
    var name by remember { mutableStateOf(initial) }
    return UiModel(
        PlaylistNameState(title, confirm, name),
        PlaylistNameActions(setName = { name = it }, confirm = { onConfirm(name) }, dismiss = onDismiss),
    )
}

// Delete playlist dialog

@Immutable
class DeletePlaylistState(val name: String)

class DeletePlaylistActions(val confirm: () -> Unit, val dismiss: () -> Unit)

// Add songs sheet

@Immutable
class AddSongsState(
    val query: String,
    /** Every song when the query is blank, otherwise those whose title or artist match. */
    val matches: List<Track>,
    /** Songs already in the playlist, shown with a check. */
    val existing: Set<String>,
    /** The playlist being filled, by name. */
    val playlistName: String? = null,
)

class AddSongsActions(
    val setQuery: (String) -> Unit,
    /** Adds the song, or removes it if it is already in the playlist. */
    val toggle: (Track) -> Unit,
    val dismiss: () -> Unit,
)

@Composable
fun rememberAddSongsModel(library: Library, playlistId: Long, existing: Set<String>, onDismiss: () -> Unit): UiModel<AddSongsState, AddSongsActions> {
    val app = LocalContext.current.attic
    val playlists by app.userData.playlists.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val matches = remember(query, library) {
        val q = TitleCleaner.searchable(query)
        if (q.isEmpty()) {
            library.tracks
        } else {
            library.tracks.filter { TitleCleaner.searchable("${it.title} ${it.artist}").contains(q) }
        }
    }
    val actions = AddSongsActions(
        setQuery = { query = it },
        toggle = { track ->
            if (track.key in existing) {
                app.userData.removeFromPlaylist(playlistId, track.key)
            } else {
                app.scope.launch { app.userData.addToPlaylist(playlistId, listOf(track.key)) }
            }
        },
        dismiss = onDismiss,
    )
    return UiModel(AddSongsState(query, matches, existing, playlists.firstOrNull { it.id == playlistId }?.name), actions)
}
