package app.attic.player.storeshots

import app.attic.player.playback.eq.EqCurve
import app.attic.player.playback.eq.EqPresets
import app.attic.player.playback.eq.EqScope
import app.attic.player.playback.eq.EqTarget
import app.attic.player.ui.model.EqOwnRow
import app.attic.player.ui.model.EqPresetChip
import app.attic.player.ui.model.EqPresetDialog
import app.attic.player.ui.model.EqRemoval
import app.attic.player.ui.model.EqualiserEditorActions
import app.attic.player.ui.model.EqualiserEditorState
import app.attic.player.ui.model.EqualiserMenuState
import app.attic.player.ui.model.EqualiserPageActions
import app.attic.player.ui.model.EqualiserPageState
import app.attic.player.ui.model.EqualiserSettingsRow
import app.attic.player.ui.model.EqualiserSheetActions
import app.attic.player.ui.model.EqualiserSheetState

/** The equaliser in the demo library: the sheet over Now Playing in each scope, and the Settings page. */
object EqualiserDemo {
    private val noop: () -> Unit = {}

    private val builtInNames = mapOf(
        EqPresets.FLAT to "Flat", EqPresets.BASS_BOOST to "Bass boost", EqPresets.BASS_REDUCER to "Bass reducer",
        EqPresets.TREBLE_BOOST to "Treble boost", EqPresets.VOCAL to "Vocal", EqPresets.LOUDNESS to "Loudness",
        EqPresets.ACOUSTIC to "Acoustic", EqPresets.ELECTRONIC to "Electronic", EqPresets.HIP_HOP to "Hip-hop",
        EqPresets.CLASSICAL to "Classical", EqPresets.SPOKEN_WORD to "Spoken word",
    )

    private val carSpeakers = EqCurve.of(4f, 3.5f, 2f, 0f, -1f, 0f, 1f, 2.5f, 3f, 2f)
    private val nightLow = EqCurve.of(-2f, -1.5f, -1f, 0f, 1f, 1.5f, 1f, -1f, -2.5f, -3f)

    /** A curve of the listener's own, matching no preset. */
    val custom: EqCurve = EqCurve.of(3f, 4f, 2.5f, 0.5f, -1.5f, -2.5f, -0.5f, 2f, 3.5f, 2.5f)

    private val userChips = listOf(EqPresetChip("user:1", "Car speakers", 1), EqPresetChip("user:2", "Night, low", 2))
    private val curves: Map<String, EqCurve> =
        mapOf("user:1" to carSpeakers, "user:2" to nightLow) + EqPresets.all.associate { it.id to it.curve }
    val chips: List<EqPresetChip> = userChips + EqPresets.all.map { EqPresetChip(it.id, builtInNames.getValue(it.id)) }

    fun preset(id: String): EqCurve = curves.getValue(id)

    private fun selected(curve: EqCurve): EqPresetChip? =
        chips.firstOrNull { !it.isUser && curves[it.key] == curve } ?: chips.firstOrNull { curves[it.key] == curve }

    fun editor(curve: EqCurve, on: Boolean = true, dialog: EqPresetDialog? = null) =
        EqualiserEditorState(on = on, curve = curve, presets = chips, selected = selected(curve), dialog = dialog)

    val editorActions = EqualiserEditorActions(
        setOn = {}, setBand = { _, _ -> }, choosePreset = {}, askSavePreset = noop, askRenamePreset = {}, askDeletePreset = {},
        savePreset = {}, renamePreset = { _, _ -> }, deletePreset = {}, dismissDialog = noop,
    )

    /** The sheet for the song playing in the demo, "Lights Over the Water" from Paper Lanterns. */
    fun sheet(scope: EqScope, curve: EqCurve, on: Boolean = true, dialog: EqPresetDialog? = null, overAlbum: Boolean = false): EqualiserSheetState {
        val now = DemoLibrary.nowPlaying
        return EqualiserSheetState(
            editor = editor(curve, on, dialog),
            scope = scope,
            songTitle = now.title,
            albumName = now.album,
            mediaId = now.mediaId,
            canSaveForSong = scope != EqScope.Song,
            canSaveForAlbum = scope == EqScope.Default,
            removal = when (scope) {
                EqScope.Song -> if (overAlbum) EqRemoval.SongToAlbum else EqRemoval.SongToDefault
                EqScope.Album -> EqRemoval.AlbumToDefault
                EqScope.Default -> null
            },
        )
    }

    val sheetDefault get() = sheet(EqScope.Default, preset(EqPresets.BASS_BOOST))
    val sheetSong get() = sheet(EqScope.Song, custom)
    val sheetAlbum get() = sheet(EqScope.Album, preset(EqPresets.VOCAL))
    val sheetOff get() = sheet(EqScope.Default, preset(EqPresets.BASS_BOOST), on = false)
    val sheetSavePreset get() = sheet(EqScope.Song, custom, dialog = EqPresetDialog.Save)

    val sheetActions = EqualiserSheetActions(editor = editorActions, saveForSong = noop, saveForAlbum = noop, remove = noop, dismiss = noop)

    /** The track menu's row, as it reads with the default on Bass boost. */
    val menu = EqualiserMenuState(on = true, scope = EqScope.Default, presetName = "Bass boost")

    private fun row(title: String, song: Boolean, preset: String): EqOwnRow =
        if (song) {
            val track = DemoLibrary.track(title)
            EqOwnRow(EqTarget.Song(track.key), track.title, track.artist, track.mediaId, preset)
        } else {
            val album = DemoLibrary.album(title)
            EqOwnRow(EqTarget.Album(album.key), album.name, album.artist, album.tracks.first().mediaId, preset)
        }

    fun page(empty: Boolean = false, on: Boolean = true) = EqualiserPageState(
        editor = editor(preset(EqPresets.BASS_BOOST), on),
        albums = if (empty) emptyList() else listOf(row("Afterglow Protocol", false, "Electronic"), row("Room Tone", false, "Custom")),
        songs = if (empty) {
            emptyList()
        } else {
            listOf(
                row("Chai Ka Doosra Cup", true, "Vocal"),
                row("Kettle On", true, "Night, low"),
                row("Lights Over the Water", true, "Custom"),
                row("Marea Lenta", true, "Acoustic"),
            )
        },
        phoneEqualiserAvailable = true,
    )

    val pageActions = EqualiserPageActions(editor = editorActions, back = noop, remove = {}, openPhoneEqualiser = noop)

    /** Settings > Playback's row with something saved. */
    val settingsRow = EqualiserSettingsRow(on = true, presetName = "Bass boost", ownSongs = 4, ownAlbums = 2)
}
