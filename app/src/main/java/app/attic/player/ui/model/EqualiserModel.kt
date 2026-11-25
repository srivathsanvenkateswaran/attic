package app.attic.player.ui.model

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.R
import app.attic.player.attic
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.data.isExternal
import app.attic.player.playback.Equaliser
import app.attic.player.playback.eq.EqBands
import app.attic.player.playback.eq.EqCurve
import app.attic.player.playback.eq.EqPreset
import app.attic.player.playback.eq.EqPresets
import app.attic.player.playback.eq.EqResponse
import app.attic.player.playback.eq.EqScope
import app.attic.player.playback.eq.EqTarget
import app.attic.player.playback.eq.EqualiserState
import app.attic.player.playback.eq.UserPreset
import app.attic.player.ui.Haptic
import app.attic.player.ui.LocalUi
import app.attic.player.ui.rememberHaptics
import kotlin.math.abs
import kotlin.math.sqrt

// The editor: presets, bands and the response, the same on the Now Playing sheet and the Settings page.

/** One preset chip. [key] is a built-in id from [EqPresets], or `user:<id>` for one of the user's own. */
@Immutable
data class EqPresetChip(val key: String, val name: String, val userId: Long? = null) {
    val isUser: Boolean get() = userId != null
}

/** The preset dialog that is open, if any. */
sealed interface EqPresetDialog {
    /** "Save as preset": names the curve now shown. */
    data object Save : EqPresetDialog

    data class Rename(val id: Long, val name: String) : EqPresetDialog

    data class Delete(val id: Long, val name: String) : EqPresetDialog
}

@Immutable
class EqualiserEditorState(
    /** The equaliser's switch. Off, nothing is applied, but every control still works and turns it on. */
    val on: Boolean,
    /** The curve being edited, which is the one playing when [on]. */
    val curve: EqCurve,
    /** The user's presets, oldest first, then the built-in ones in their order. "Custom" and "Save as preset" are not in here. */
    val presets: List<EqPresetChip>,
    /** The preset [curve] is, or null when it is none of them: then the style shows "Custom" and "Save as preset". */
    val selected: EqPresetChip?,
    val dialog: EqPresetDialog?,
) {
    /** The name the curve goes by in lists and the menu: its preset's, or "Custom" ([customName]). */
    fun presetName(customName: String): String = selected?.name ?: customName

    /**
     * Whether [name] is already taken by another preset, built-in or the user's, ignoring case and spaces
     * at the ends. [exceptId] is the preset being renamed, which may keep its own name.
     */
    fun nameTaken(name: String, exceptId: Long? = null): Boolean {
        val wanted = name.trim().lowercase()
        return presets.any { it.userId != exceptId && it.name.trim().lowercase() == wanted }
    }
}

class EqualiserEditorActions(
    val setOn: (Boolean) -> Unit,
    /**
     * Moves band [index] to a gain in dB (clamped to the band range). Heard at once and saved shortly
     * after. Call it at most once a frame while dragging.
     */
    val setBand: (index: Int, gainDb: Float) -> Unit,
    /** Applies a preset at once. */
    val choosePreset: (EqPresetChip) -> Unit,
    val askSavePreset: () -> Unit,
    val askRenamePreset: (EqPresetChip) -> Unit,
    val askDeletePreset: (EqPresetChip) -> Unit,
    /** From the "Save as preset" dialog. The new preset is then the selected one. */
    val savePreset: (name: String) -> Unit,
    val renamePreset: (id: Long, name: String) -> Unit,
    val deletePreset: (id: Long) -> Unit,
    val dismissDialog: () -> Unit,
)

/** What a drawing needs about the bands, so a style doesn't reach into the engine. */
object EqualiserBands {
    const val COUNT = EqBands.COUNT
    const val MIN_DB = EqBands.MIN_DB
    const val MAX_DB = EqBands.MAX_DB

    /** "31", "62" … "16k", under the sliders. */
    val labels: List<String> = EqBands.labels

    /**
     * The drawing's x axis: ten octaves on a log scale, each band's centre in the middle of its tenth,
     * from 31.25 Hz / √2 to 16 kHz × √2.
     */
    val fromHz: Double = EqBands.frequenciesHz.first() / sqrt(2.0)
    val toHz: Double = EqBands.frequenciesHz.last() * sqrt(2.0)

    /**
     * The real response of [gainsDb] (one per band) in dB at [points] frequencies spread evenly over the
     * drawing's width, without the pre-gain, so a flat curve sits on 0 dB. About 20 µs for 96 points on a
     * desktop; cheap enough for every drag frame.
     */
    fun response(gainsDb: FloatArray, points: Int = 96): FloatArray =
        EqResponse.of(EqCurve.of(*gainsDb).coerced(), sampleRate = 48_000, points = points, fromHz = fromHz, toHz = toHz).gainsDb

    /** The x of band [index]'s centre as a fraction of the drawing's width. */
    fun centre(index: Int): Float = (index + 0.5f) / COUNT

    /** The value as the bubble and TalkBack say it, with its sign: "+4", "−2.5", "0". Use with `R.string.equaliser_db`. */
    fun formatDb(gainDb: Float): String {
        val rounded = Math.round(gainDb * 2f) / 2f
        val magnitude = abs(rounded).let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }
        return when {
            rounded > 0f -> "+$magnitude"
            rounded < 0f -> "−$magnitude"
            else -> "0"
        }
    }

    /** A band's name for TalkBack: "31 Hz", "1 kHz". */
    fun describe(context: Context, index: Int): String {
        val hz = EqBands.frequenciesHz[index]
        return if (hz >= 1000) {
            context.getString(R.string.equaliser_khz, (hz / 1000).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() })
        } else {
            context.getString(R.string.equaliser_hz, hz.toInt().toString())
        }
    }
}

/** The names of the built-in presets, by id. */
fun builtInPresetName(context: Context, id: String): String = context.getString(
    when (id) {
        EqPresets.FLAT -> R.string.equaliser_preset_flat
        EqPresets.BASS_BOOST -> R.string.equaliser_preset_bass_boost
        EqPresets.BASS_REDUCER -> R.string.equaliser_preset_bass_reducer
        EqPresets.TREBLE_BOOST -> R.string.equaliser_preset_treble_boost
        EqPresets.VOCAL -> R.string.equaliser_preset_vocal
        EqPresets.LOUDNESS -> R.string.equaliser_preset_loudness
        EqPresets.ACOUSTIC -> R.string.equaliser_preset_acoustic
        EqPresets.ELECTRONIC -> R.string.equaliser_preset_electronic
        EqPresets.HIP_HOP -> R.string.equaliser_preset_hip_hop
        EqPresets.CLASSICAL -> R.string.equaliser_preset_classical
        else -> R.string.equaliser_preset_spoken_word
    },
)

private fun userKey(id: Long) = "user:$id"

/** The chips in order (the user's first, then the built-ins) and the curve each stands for. */
private fun presetChips(context: Context, user: List<UserPreset>): List<Pair<EqPresetChip, EqCurve>> =
    user.map { EqPresetChip(userKey(it.id), it.name, it.id) to it.curve } +
        EqPresets.all.map { EqPresetChip(it.id, builtInPresetName(context, it.id)) to it.curve }

/** The chip [curve] is: a built-in first, then the user's in order, or null for Custom. */
private fun selectedChip(curve: EqCurve, chips: List<Pair<EqPresetChip, EqCurve>>): EqPresetChip? {
    val match = EqPresets.match(curve, chips.filter { it.first.isUser }.map { EqPreset(it.first.key, it.second) }) ?: return null
    return chips.firstOrNull { it.first.key == match.id }?.first
}

/** Which preset a curve shows as, for a list or a menu row. */
fun presetNameOf(context: Context, curve: EqCurve, user: List<UserPreset>): String =
    selectedChip(curve, presetChips(context, user))?.name ?: context.getString(R.string.equaliser_preset_custom)

/**
 * The editor for whatever [target] gives: the setting the sheet is showing, or the default on the
 * Settings page. Every change goes to that setting through the store.
 */
@Composable
private fun rememberEditor(target: () -> EqTarget, state: EqualiserState, curve: EqCurve): UiModel<EqualiserEditorState, EqualiserEditorActions> {
    val app = LocalContext.current.attic
    val context = LocalContext.current
    val store = app.equaliser
    val user by store.presets.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    var dialog by remember { mutableStateOf<EqPresetDialog?>(null) }
    // A preset saved from the dialog is selected as soon as it arrives from the database.
    val chips = remember(user, context) { presetChips(context, user) }
    val selected = remember(curve, chips) { selectedChip(curve, chips) }

    fun current(): EqCurve = store.state.value.let { now -> now.curveOf(target()) ?: now.default }

    val editor = EqualiserEditorState(
        on = state.on,
        curve = curve,
        presets = chips.map { it.first },
        selected = selected,
        dialog = dialog,
    )
    val actions = EqualiserEditorActions(
        setOn = { on ->
            haptics.toggle(on)
            store.setOn(on)
        },
        setBand = { index, gain -> store.set(target(), current().with(index, gain.coerceIn(EqBands.MIN_DB, EqBands.MAX_DB))) },
        choosePreset = { chip ->
            chips.firstOrNull { it.first.key == chip.key }?.let { (_, preset) ->
                haptics.perform(Haptic.Tick)
                store.set(target(), preset)
            }
        },
        askSavePreset = { dialog = EqPresetDialog.Save },
        askRenamePreset = { chip -> chip.userId?.let { dialog = EqPresetDialog.Rename(it, chip.name) } },
        askDeletePreset = { chip -> chip.userId?.let { dialog = EqPresetDialog.Delete(it, chip.name) } },
        savePreset = { name ->
            dialog = null
            if (name.isNotBlank()) {
                haptics.perform(Haptic.Confirm)
                store.savePreset(name, current())
            }
        },
        renamePreset = { id, name ->
            dialog = null
            if (name.isNotBlank()) store.renamePreset(id, name)
        },
        deletePreset = { id ->
            dialog = null
            store.deletePreset(id)
        },
        dismissDialog = { dialog = null },
    )
    return UiModel(editor, actions)
}

// The sheet from Now Playing.

/** What removing the song's or album's own setting leads to, which decides the action's words. */
enum class EqRemoval {
    /** The song's own setting goes and it plays with the default: "Use default", "Removes this song's setting". */
    SongToDefault,

    /** The song's own setting goes and its album's takes over: "Use album setting", "Removes this song's setting". */
    SongToAlbum,

    /** The album's setting goes: "Use default", "Removes this album's setting". */
    AlbumToDefault,
}

@Immutable
class EqualiserSheetState(
    val editor: EqualiserEditorState,
    /** Which setting the song plays with, and so which one the sheet changes. */
    val scope: EqScope,
    val songTitle: String,
    /** The song's album by name, when it has one in the library. */
    val albumName: String?,
    /** For the cover beside an own setting. */
    val mediaId: Long,
    val canSaveForSong: Boolean,
    val canSaveForAlbum: Boolean,
    /** The removal action, when the song plays with its own or its album's setting. */
    val removal: EqRemoval?,
)

class EqualiserSheetActions(
    val editor: EqualiserEditorActions,
    /** Gives the song its own setting from the curve now shown. The sheet stays open and then edits it. */
    val saveForSong: () -> Unit,
    val saveForAlbum: () -> Unit,
    /** Removes the setting [EqualiserSheetState.removal] names. */
    val remove: () -> Unit,
    val dismiss: () -> Unit,
)

/**
 * The equaliser for the song that is playing, falling back to [fallback] (the song the menu opened on)
 * when nothing is. It follows the player: when the next song starts, the sheet shows that song's setting.
 */
@Composable
fun rememberEqualiserSheetModel(fallback: Track, library: Library, onDismiss: () -> Unit): UiModel<EqualiserSheetState, EqualiserSheetActions> {
    val app = LocalContext.current.attic
    val store = app.equaliser
    val haptics = rememberHaptics()
    val player by app.player.state.collectAsStateWithLifecycle()
    val state by store.state.collectAsStateWithLifecycle()
    val track = player.current ?: fallback
    val external = track.isExternal
    val songKey = track.key.takeUnless { external }
    val albumKey = track.albumKey?.takeIf { !external && it in library.albumByKey }
    val resolved = state.resolve(songKey, albumKey)
    val target = when (resolved.scope) {
        EqScope.Song -> EqTarget.Song(songKey!!)
        EqScope.Album -> EqTarget.Album(albumKey!!)
        EqScope.Default -> EqTarget.Default
    }
    // Saving for a song or album that was playing with the default puts the default back as it was when
    // the sheet opened, so changes made on the way to "Save for this song" don't reach every other song.
    val defaultAtOpen = remember { store.state.value.default }
    val editor = rememberEditor({ target }, state, resolved.curve)

    fun saveFor(own: EqTarget) {
        val curve = store.state.value.let { it.curveOf(target) ?: it.default }
        haptics.perform(Haptic.Confirm)
        store.set(own, curve)
        if (target == EqTarget.Default && store.state.value.default != defaultAtOpen) store.set(EqTarget.Default, defaultAtOpen)
    }

    val removal = when (resolved.scope) {
        EqScope.Song -> if (albumKey != null && albumKey in state.albums) EqRemoval.SongToAlbum else EqRemoval.SongToDefault
        EqScope.Album -> EqRemoval.AlbumToDefault
        EqScope.Default -> null
    }
    val sheet = EqualiserSheetState(
        editor = editor.state,
        scope = resolved.scope,
        songTitle = track.title,
        albumName = albumKey?.let { library.albumByKey[it]?.name },
        mediaId = track.mediaId,
        canSaveForSong = songKey != null && resolved.scope != EqScope.Song,
        canSaveForAlbum = albumKey != null && resolved.scope == EqScope.Default,
        removal = removal,
    )
    val actions = EqualiserSheetActions(
        editor = editor.actions,
        saveForSong = { songKey?.let { saveFor(EqTarget.Song(it)) } },
        saveForAlbum = { albumKey?.let { saveFor(EqTarget.Album(it)) } },
        remove = {
            haptics.perform(Haptic.Confirm)
            when (removal) {
                EqRemoval.SongToDefault, EqRemoval.SongToAlbum -> songKey?.let { store.clear(EqTarget.Song(it)) }
                EqRemoval.AlbumToDefault -> albumKey?.let { store.clear(EqTarget.Album(it)) }
                null -> Unit
            }
        },
        dismiss = onDismiss,
    )
    return UiModel(sheet, actions)
}

// The track menu row and the Now Playing indicator.

/** The equaliser as the track menu's row shows it. */
@Immutable
data class EqualiserMenuState(val on: Boolean, val scope: EqScope, val presetName: String)

/** The track menu row's second line: "Off", the default's preset, or "This song · Vocal". */
@Composable
fun equaliserSummary(state: EqualiserMenuState): String = when {
    !state.on -> stringResource(R.string.equaliser_off)
    state.scope == EqScope.Song -> stringResource(R.string.equaliser_state_song, state.presetName)
    state.scope == EqScope.Album -> stringResource(R.string.equaliser_state_album, state.presetName)
    else -> state.presetName
}

/** The menu row's state for [track]. */
@Composable
fun rememberEqualiserMenuState(track: Track, library: Library): EqualiserMenuState {
    val app = LocalContext.current.attic
    val context = LocalContext.current
    val state by app.equaliser.state.collectAsStateWithLifecycle()
    val user by app.equaliser.presets.collectAsStateWithLifecycle()
    val external = track.isExternal
    val resolved = state.resolve(track.key.takeUnless { external }, track.albumKey?.takeIf { !external && it in library.albumByKey })
    return EqualiserMenuState(state.on, resolved.scope, remember(resolved.curve, user) { presetNameOf(context, resolved.curve, user) })
}

/**
 * Song or Album when Now Playing should show its equaliser indicator: the equaliser is on and [track]
 * plays with its own or its album's setting. Null otherwise.
 */
@Composable
fun rememberEqualiserIndicator(track: Track?): EqScope? {
    val app = LocalContext.current.attic
    val state by app.equaliser.state.collectAsStateWithLifecycle()
    if (track == null || track.isExternal || !state.on) return null
    return state.resolve(track.key, track.albumKey).scope.takeUnless { it == EqScope.Default }
}

// Settings > Playback > Equaliser.

/** A song or album with its own setting, as the Settings page lists it. */
@Immutable
data class EqOwnRow(
    val target: EqTarget,
    val title: String,
    /** The artist. */
    val subtitle: String,
    /** A MediaStore ID for the cover, or null for none. */
    val coverId: Long?,
    /** The preset the setting is, or "Custom". */
    val preset: String,
)

@Immutable
class EqualiserPageState(
    /** Always the default. */
    val editor: EqualiserEditorState,
    /** Sorted by title. Albums and songs no longer in the library are left out. */
    val albums: List<EqOwnRow>,
    val songs: List<EqOwnRow>,
    /** Whether the phone has an equaliser panel of its own to open. */
    val phoneEqualiserAvailable: Boolean,
)

class EqualiserPageActions(
    val editor: EqualiserEditorActions,
    val back: () -> Unit,
    /** Removes a song's or album's own setting. No confirmation: the row goes. */
    val remove: (EqTarget) -> Unit,
    /** Opens the phone's own equaliser panel on Attic's audio session. */
    val openPhoneEqualiser: () -> Unit,
)

@Composable
fun rememberEqualiserPageModel(library: Library): UiModel<EqualiserPageState, EqualiserPageActions> {
    val app = LocalContext.current.attic
    val context = LocalContext.current
    val ui = LocalUi.current
    val store = app.equaliser
    val state by store.state.collectAsStateWithLifecycle()
    val user by store.presets.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val editor = rememberEditor({ EqTarget.Default }, state, state.default)
    // Checked each time the page opens, so an equaliser app installed meanwhile shows up.
    val phoneAvailable = remember { Equaliser.isAvailable(context) }
    val panel = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    val albums = remember(state.albums, user, library) {
        state.albums.mapNotNull { (key, curve) ->
            val album = library.albumByKey[key] ?: return@mapNotNull null
            EqOwnRow(EqTarget.Album(key), album.name, album.artist, album.tracks.firstOrNull()?.mediaId, presetNameOf(context, curve, user))
        }.sortedBy { it.title.lowercase() }
    }
    val songs = remember(state.songs, user, library) {
        state.songs.mapNotNull { (key, curve) ->
            val track = library.trackByKey[key] ?: return@mapNotNull null
            EqOwnRow(EqTarget.Song(key), track.title, track.artist, track.mediaId, presetNameOf(context, curve, user))
        }.sortedBy { it.title.lowercase() }
    }
    val page = EqualiserPageState(editor.state, albums, songs, phoneAvailable)
    val actions = EqualiserPageActions(
        editor = editor.actions,
        back = { ui.back() },
        remove = { target ->
            haptics.perform(Haptic.Tick)
            store.clear(target)
        },
        openPhoneEqualiser = {
            try {
                panel.launch(Equaliser.panelIntent(context, app.audioSessionId.value))
            } catch (_: ActivityNotFoundException) {
                app.messages.tryEmit(app.getString(R.string.settings_equaliser_failed))
            }
        },
    )
    return UiModel(page, actions)
}

/** The Settings > Playback row's state. */
@Immutable
data class EqualiserSettingsRow(
    val on: Boolean,
    /** The default's preset name, or "Custom". */
    val presetName: String,
    /** Songs and albums with their own setting that are in the library. */
    val ownSongs: Int,
    val ownAlbums: Int,
)

@Composable
fun rememberEqualiserSettingsRow(library: Library): EqualiserSettingsRow {
    val app = LocalContext.current.attic
    val context = LocalContext.current
    val state by app.equaliser.state.collectAsStateWithLifecycle()
    val user by app.equaliser.presets.collectAsStateWithLifecycle()
    return EqualiserSettingsRow(
        on = state.on,
        presetName = remember(state.default, user) { presetNameOf(context, state.default, user) },
        ownSongs = state.songs.keys.count { it in library.trackByKey },
        ownAlbums = state.albums.keys.count { it in library.albumByKey },
    )
}

/** The row's state line: "Off" or the default's preset name. */
@Composable
fun equaliserRowValue(row: EqualiserSettingsRow): String = if (row.on) row.presetName else stringResource(R.string.equaliser_off)

/** "With their own setting: 4 songs, 2 albums", or null when nothing has its own. */
@Composable
fun equaliserOwnSummary(row: EqualiserSettingsRow): String? {
    val resources = LocalContext.current.resources
    val songs = resources.getQuantityString(R.plurals.song_count, row.ownSongs, row.ownSongs.toString())
    val albums = resources.getQuantityString(R.plurals.album_count, row.ownAlbums, row.ownAlbums.toString())
    val counts = when {
        row.ownSongs > 0 && row.ownAlbums > 0 -> stringResource(R.string.settings_equaliser_own_both, songs, albums)
        row.ownSongs > 0 -> songs
        row.ownAlbums > 0 -> albums
        else -> return null
    }
    return stringResource(R.string.settings_equaliser_own, counts)
}
