@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.playback.eq.EqCurve
import app.attic.player.playback.eq.EqScope
import app.attic.player.ui.Haptic
import app.attic.player.ui.Haptics
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.EqOwnRow
import app.attic.player.ui.model.EqPresetChip
import app.attic.player.ui.model.EqPresetDialog
import app.attic.player.ui.model.EqRemoval
import app.attic.player.ui.model.EqualiserBands
import app.attic.player.ui.model.EqualiserEditorActions
import app.attic.player.ui.model.EqualiserEditorState
import app.attic.player.ui.model.EqualiserPageActions
import app.attic.player.ui.model.EqualiserPageState
import app.attic.player.ui.model.EqualiserSheetActions
import app.attic.player.ui.model.EqualiserSheetState
import app.attic.player.ui.rememberHaptics
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * The equaliser in the Comfort style (oneui/11-equaliser.html): the sheet from Now Playing, the page
 * under Settings > Playback, and the capsule in Now Playing's header. The sheet edits the setting the
 * song is playing with; its head says which one that is and its last rows change it. The page only
 * ever edits the default.
 *
 * Moving a band is the one thing here that happens every frame, so the gains are kept where only the
 * plot's drawing reads them ([BandsState]). The model does hand over a new state for each change, which
 * is why the sheet and the page pass their state down in State holders and plain values: the pieces
 * whose inputs did not change are skipped, the chips, the scope head and the action rows among them.
 */

// ── The sheet ────────────────────────────────────────────────────────────────

/**
 * The equaliser for the song that is playing (frames B to F2 and I). Top to bottom: the title with the
 * switch, the scope head, the presets, the plot with the response behind the ten bands, and the rows
 * that give the song or album its own setting or take it away. Over the player the caller washes it with the
 * cover, as it does the track menu.
 */
@Composable
fun ComfortEqualiserSheet(state: EqualiserSheetState, actions: EqualiserSheetActions) {
    val current = rememberUpdatedState(state)
    val calls = rememberUpdatedState(actions)
    ComfortSheet(onDismiss = { calls.value.dismiss() }) { EqualiserSheetContent(current, calls) }
    state.editor.dialog?.let { PresetDialog(it, state.editor, actions.editor) }
}

@Composable
private fun EqualiserSheetContent(current: State<EqualiserSheetState>, calls: State<EqualiserSheetActions>) {
    val state = current.value
    val editorCalls = rememberUpdatedState(calls.value.editor)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        EqualiserTitleRow(state.editor.on) { editorCalls.value.setOn(it) }
        AnimatedVisibility(
            visible = !state.editor.on,
            enter = expandVertically(tween(Settle)) + fadeIn(tween(Settle)),
            exit = shrinkVertically(tween(Settle)) + fadeOut(tween(Settle)),
        ) {
            // `.eq-off`: pulled 4dp up under the title, 8dp clear of the scope head.
            Text(
                stringResource(R.string.equaliser_off_note),
                style = Comfort.type.sub,
                color = Comfort.colors.on2,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp).offset(y = (-4).dp),
            )
        }
        EqualiserScopeHead(state.scope, state.songTitle, state.albumName, state.mediaId)
        EqualiserEditor(state.editor, editorCalls, chipPadding = 16.dp, plotMargin = 16.dp)
        EqualiserActionRows(state.canSaveForSong, state.canSaveForAlbum, state.removal, calls)
    }
}

/** "Equaliser" with the master switch at its end; the whole row is the switch. */
@Composable
private fun EqualiserTitleRow(on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(on, role = Role.Switch, onValueChange = onToggle)
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.equaliser), style = Comfort.type.barTitle, color = Comfort.colors.on, modifier = Modifier.weight(1f))
        ComfortSwitch(on, null)
    }
}

/**
 * `.eq-scope`: what the bands below change. The default is a neutral icon box and "For all songs". A
 * song or album with its own setting shows its cover and name on the tonal bed the kit gives the
 * current song, so the head changes form, not only words, when a setting is saved or removed. TalkBack
 * reads it as one piece and announces the change.
 */
@Composable
private fun EqualiserScopeHead(scope: EqScope, songTitle: String, albumName: String?, mediaId: Long) {
    val colors = Comfort.colors
    val own = scope != EqScope.Default
    val bed = animateFloatAsState(if (own) 1f else 0f, tween(Settle), label = "scopeBed")
    val secondInk by animateColorAsState(if (own) colors.onAccentSoft else colors.on2, tween(Settle), label = "scopeInk")
    val title = stringResource(
        when (scope) {
            EqScope.Default -> R.string.equaliser_scope_default
            EqScope.Album -> R.string.equaliser_scope_album
            EqScope.Song -> R.string.equaliser_scope_song
        },
    )
    val second = when (scope) {
        EqScope.Default -> stringResource(R.string.equaliser_scope_default_body)
        EqScope.Album -> albumName.orEmpty()
        EqScope.Song -> songTitle
    }
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            .fillMaxWidth()
            .drawBehind {
                val shown = bed.value
                if (shown > 0f) drawRoundRect(colors.accentSoft, cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()), alpha = shown)
            }
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .heightIn(min = 64.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Crossfade(own, animationSpec = tween(Settle), label = "scopeLead") { cover ->
            if (cover) Cover(Art.of(mediaId), size = 48.dp) else IconBox(Symbol.LibraryMusic, size = 48.dp, style = IconBoxStyle.Neutral)
        }
        RowText(title, Modifier.weight(1f), subtitle = second, titleStyle = onest(17f, 22f, 700, -0.01f), subtitleColor = secondInk)
    }
}

/**
 * `.eq-rows`: the rows that change which setting the song plays with, 4dp under the plot's card,
 * whose edge parts them from the editor. Saving is offered while the song has no setting of its own; removing says what it removes on
 * a second line, since it cannot be undone. The note belongs to the default, where saving is the only
 * thing on offer.
 */
@Composable
private fun EqualiserActionRows(canSaveForSong: Boolean, canSaveForAlbum: Boolean, removal: EqRemoval?, calls: State<EqualiserSheetActions>) {
    // A song opened from another app can keep no setting: nothing to offer.
    if (!canSaveForSong && !canSaveForAlbum && removal == null) return
    val colors = Comfort.colors
    Column(Modifier.padding(top = 4.dp)) {
        Hairlined {
            if (canSaveForSong) {
                item { SheetRow(Symbol.MusicNote, stringResource(R.string.equaliser_save_song), { calls.value.saveForSong() }) }
            }
            if (canSaveForAlbum) {
                item { SheetRow(Symbol.Album, stringResource(R.string.equaliser_save_album), { calls.value.saveForAlbum() }) }
            }
            if (removal != null) {
                item {
                    SheetRow(
                        if (removal == EqRemoval.SongToAlbum) Symbol.Album else Symbol.LibraryMusic,
                        stringResource(if (removal == EqRemoval.SongToAlbum) R.string.equaliser_use_album else R.string.equaliser_use_default),
                        { calls.value.remove() },
                        summary = stringResource(
                            if (removal == EqRemoval.AlbumToDefault) R.string.equaliser_removes_album else R.string.equaliser_removes_song,
                        ),
                    )
                }
            }
        }
        if (removal == null) {
            // The kit's `Note` is `--on-3`, too faint for a sentence that has to be read.
            Text(
                stringResource(R.string.equaliser_saved_note),
                style = onest(13f, 18f),
                color = colors.on2,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            )
        }
    }
}

// ── Settings > Playback > Equaliser ──────────────────────────────────────────

/**
 * The page (frames H, H-light, K5), laid out like the licence page: the switch, the default in one
 * card, then the albums and the songs that have their own setting, each with the preset it holds and a
 * button that removes it, and the phone's own equaliser last, where the phone has one.
 */
@Composable
fun ComfortEqualiserPage(state: EqualiserPageState, actions: EqualiserPageActions) {
    val editor = rememberUpdatedState(state.editor)
    val calls = rememberUpdatedState(actions)
    val editorCalls = rememberUpdatedState(actions.editor)
    // The same lists by value are the same lists to the page, so a band change leaves their rows alone.
    val albums = remember(state.albums) { state.albums }
    val songs = remember(state.songs) { state.songs }
    val phone = state.phoneEqualiserAvailable
    val title = stringResource(R.string.equaliser)
    RootPage(title = title, listState = rememberLazyListState(), heroHeight = ShortHeroHeight, onBack = { calls.value.back() }) {
        item(key = "hero") { Hero(title, short = true) }
        item(key = "tools") {
            LeadTools { ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), { calls.value.back() }) }
        }
        item(key = "switch") { UseEqualiserCard(editor, editorCalls) }
        item(key = "default") { DefaultCard(editor, editorCalls) }
        if (albums.isNotEmpty()) {
            item(key = "albums") { Label(stringResource(R.string.equaliser_own_albums)) }
            itemsIndexed(albums, key = { _, row -> row.target.toString() }, contentType = { _, _ -> "album" }) { index, row ->
                GroupCell(index, albums.size, Modifier.animateItem()) { OwnAlbumRow(row, calls) }
            }
        }
        if (songs.isNotEmpty()) {
            item(key = "songs") { Label(stringResource(R.string.equaliser_own_songs)) }
            itemsIndexed(songs, key = { _, row -> row.target.toString() }, contentType = { _, _ -> "song" }) { index, row ->
                GroupCell(index, songs.size, Modifier.animateItem()) { OwnSongRow(row, calls) }
            }
        }
        if (albums.isEmpty() && songs.isEmpty()) {
            item(key = "none") {
                Label(stringResource(R.string.equaliser_own_none_title))
                Group { item { SetBlock { SetBlockText(stringResource(R.string.equaliser_own_none)) } } }
            }
        }
        if (phone) {
            item(key = "phone") {
                Label(stringResource(R.string.equaliser_phone_section))
                Group {
                    item {
                        SetRow(
                            stringResource(R.string.equaliser_phone),
                            summary = stringResource(R.string.settings_equaliser_body),
                            onClick = { calls.value.openPhoneEqualiser() },
                            trailing = { ComfortIcon(Symbol.OpenInNew, tint = Comfort.colors.on3) },
                        )
                    }
                }
            }
        }
    }
    state.editor.dialog?.let { PresetDialog(it, state.editor, actions.editor) }
}


@Composable
private fun UseEqualiserCard(editor: State<EqualiserEditorState>, calls: State<EqualiserEditorActions>) {
    val on = editor.value.on
    Group {
        item {
            SetSwitchRow(
                stringResource(R.string.equaliser_use),
                on,
                { calls.value.setOn(it) },
                summary = if (on) null else stringResource(R.string.equaliser_off_note),
            )
        }
    }
}

/**
 * "Default": the editor in one card, with no scope head, since the page edits nothing else and the
 * label and the note under the card say so.
 */
@Composable
private fun DefaultCard(editor: State<EqualiserEditorState>, calls: State<EqualiserEditorActions>) {
    Label(stringResource(R.string.equaliser_default))
    Group {
        item {
            // The plot's card has 8dp of this card on three sides.
            Column(Modifier.padding(top = 10.dp, bottom = 8.dp)) {
                EqualiserEditor(editor.value, calls, chipPadding = 12.dp, plotMargin = 8.dp)
            }
        }
    }
    Note(stringResource(R.string.equaliser_default_note))
}

/** An album with its own setting: the preset it holds, then the button that removes it. The row itself does nothing. */
@Composable
private fun OwnAlbumRow(row: EqOwnRow, calls: State<EqualiserPageActions>) {
    CollectionRow(
        title = row.title,
        subtitle = row.subtitle,
        lead = { m -> Cover(Art.of(row.coverId), m) },
        onClick = null,
        trailing = {
            // `.row .val + .ib`: the value 12dp from the button.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OwnPreset(row.preset, onest(13.5f, 18f))
                ComfortIconButton(
                    Symbol.Close,
                    stringResource(R.string.equaliser_own_remove_cd, row.title),
                    { calls.value.remove(row.target) },
                    dim = true,
                )
            }
        },
    )
}

/** A song with its own setting, as [OwnAlbumRow]. */
@Composable
private fun OwnSongRow(row: EqOwnRow, calls: State<EqualiserPageActions>) {
    SongRow(
        title = row.title,
        subtitle = row.subtitle,
        lead = SongLead.Cover(Art.of(row.coverId)),
        trailing = {
            // `.song .meta + .ib`: the preset 2dp from the button.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                OwnPreset(row.preset, onest(13f, 18f))
                ComfortIconButton(
                    Symbol.Close,
                    stringResource(R.string.equaliser_own_remove_cd, row.title),
                    { calls.value.remove(row.target) },
                    dim = true,
                )
            }
        },
    )
}

/** The preset a row's setting is. A long name of the listener's own gives way to the title before it. */
@Composable
private fun OwnPreset(name: String, style: TextStyle) {
    Text(name, style = style, color = Comfort.colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 124.dp))
}

// ── The editor: presets and the plot ──────────────────────────────────

/** How long a band, the scope head and the off state take to settle (the kit's 200 ms tween). */
private const val Settle = 200

/** How long the knob takes to grow under the finger and shrink again. */
private const val Press = 100

private const val Bands = EqualiserBands.COUNT

/** The plot's gutters inside its card: the dB labels at the start, breathing room at the end. */
private val PlotGutterStart = 30.dp
private val PlotGutterEnd = 10.dp

/**
 * The presets, then the plot: the response drawn behind the ten bands in one card, the same in the
 * sheet and on the page's card. Emits into the column it is called in. [chipPadding] is the chip row's
 * side padding and [plotMargin] the plot card's side margin.
 */
@Composable
private fun EqualiserEditor(editor: EqualiserEditorState, actions: State<EqualiserEditorActions>, chipPadding: Dp, plotMargin: Dp) {
    val bands = rememberBands(editor.curve, actions)
    val curve = rememberUpdatedState(editor.curve)
    // 1 with the equaliser on, 0 off. Read only where it is drawn.
    val lit = animateFloatAsState(if (editor.on) 1f else 0f, tween(Settle), label = "equaliserOn")
    val presets = remember(editor.presets) { editor.presets }
    PresetChips(presets, editor.selected, editor.on, lit, actions, chipPadding)
    // A graph keeps low frequencies on the left in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BandSliders(bands, curve, lit, actions, Modifier.padding(start = plotMargin, end = plotMargin, top = 4.dp))
    }
}

/**
 * The gains as the plot shows them, apart from the model's curve so that moving a band redraws
 * and nothing recomposes. A drag writes here at once and tells the model at most once a frame; a
 * curve from the model (a preset, another song, a removal) is reached over 200 ms.
 */
@Stable
private class BandsState(curve: EqCurve, private val scope: CoroutineScope, private val haptics: Haptics, private val send: (Int, Float) -> Unit) {
    private val shown = FloatArray(Bands) { curve[it] }
    private val from = shown.copyOf()
    private val target = shown.copyOf()
    private val progress = Animatable(1f)
    private var move: Job? = null

    /** One per band, in dB. Read it in a draw block only: it changes on every frame of a drag. */
    var gains: FloatArray by mutableStateOf(shown, neverEqualPolicy())
        private set

    /** Goes up with every change to [gains], so a drawing can tell whether a path it kept is stale. */
    var version = 0
        private set

    /** The band under the finger, or -1. */
    var dragging by mutableIntStateOf(-1)
        private set

    /** The band that carries the grown knob and the value bubble: the one dragged last. */
    var held = -1
        private set

    /** 0 at rest, 1 while a band is dragged: the knob's growth and the bubble's opacity. */
    val grow = Animatable(0f)

    /** Changes whenever a dragged value is waiting to go to the model. */
    var unsent by mutableIntStateOf(0)
        private set
    private var dirty = false
    private var released = false

    private fun publish() {
        version++
        gains = shown
    }

    /** Whether the bands are at, or on their way to, [curve]. */
    fun shows(curve: EqCurve): Boolean {
        for (i in 0 until Bands) if (target[i] != curve[i]) return false
        return true
    }

    /** Moves every band to [curve] over 200 ms. A band under the finger stays with the finger. */
    fun moveTo(curve: EqCurve) {
        move?.cancel()
        for (i in 0 until Bands) {
            from[i] = shown[i]
            if (i != dragging) target[i] = curve[i]
        }
        move = scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(Settle)) {
                for (i in 0 until Bands) if (i != dragging) shown[i] = from[i] + (target[i] - from[i]) * value
                publish()
            }
        }
    }

    /** Starts a drag on [band] and returns the gain it starts from, so the value follows the finger from where it is. */
    fun beginDrag(band: Int): Float {
        dragging = band
        held = band
        from[band] = shown[band]
        target[band] = shown[band]
        scope.launch { grow.animateTo(1f, tween(Press)) }
        return shown[band]
    }

    /** The dragged band at [gainDb], snapped to 1 dB. Gives a tick each time it reaches or passes 0 dB. */
    fun dragTo(gainDb: Float) {
        val band = dragging
        if (band < 0) return
        val snapped = snapGain(gainDb)
        val before = shown[band]
        if (snapped == before) return
        shown[band] = snapped
        from[band] = snapped
        target[band] = snapped
        publish()
        if (reachesZero(before, snapped)) haptics.perform(Haptic.Tick)
        dirty = true
        unsent++
    }

    /** Hands the dragged band's value to the model, if it has changed since the last time. */
    fun flush() {
        val band = dragging
        if (!dirty || band < 0) return
        dirty = false
        send(band, shown[band])
    }

    fun endDrag() {
        if (dragging < 0) return
        // The last value goes now, not with the next frame, so the model is right before anything reads it.
        flush()
        dragging = -1
        released = true
        scope.launch { grow.animateTo(0f, tween(Press)) }
    }

    /** True once after a drag has ended. */
    fun takeReleased(): Boolean = released.also { released = false }

    /** A double tap: [band] back to 0 dB. The model's answer moves the knob. */
    fun reset(band: Int) {
        if (shown[band] != 0f) haptics.perform(Haptic.Tick)
        send(band, 0f)
    }
}

/** A dragged gain as its band takes it: inside the band's range and a whole decibel. */
internal fun snapGain(gainDb: Float): Float = gainDb.coerceIn(EqualiserBands.MIN_DB, EqualiserBands.MAX_DB).roundToInt().toFloat()

/** Whether a band going from [before] to [after] lands on 0 dB or passes it, which the finger feels as a tick. Leaving 0 dB is not one. */
internal fun reachesZero(before: Float, after: Float): Boolean = before != 0f && (after == 0f || (after > 0f) != (before > 0f))

@Composable
private fun rememberBands(curve: EqCurve, actions: State<EqualiserEditorActions>): BandsState {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val bands = remember { BandsState(curve, scope, haptics) { band, gain -> actions.value.setBand(band, gain) } }
    val model = rememberUpdatedState(curve)
    LaunchedEffect(bands) {
        // The engine hears a drag once a frame at most, however often the finger reports.
        launch {
            snapshotFlow { bands.unsent }.conflate().collect {
                bands.flush()
                withFrameNanos { }
            }
        }
        snapshotFlow { model.value to (bands.dragging >= 0) }.collectLatest { (wanted, dragging) ->
            val released = bands.takeReleased()
            if (dragging || bands.shows(wanted)) return@collectLatest
            if (released) {
                // A drag has just ended and its last value may not be back from the model yet. Two frames
                // settle that before the curve is taken for a change made elsewhere.
                repeat(2) { withFrameNanos { } }
                if (bands.shows(model.value)) return@collectLatest
            }
            bands.moveTo(model.value)
        }
    }
    return bands
}

/** Where each chip sits in the row, kept from the last layout so the row can scroll one into view. */
private class ChipSpots {
    var start = IntArray(0)
    var width = IntArray(0)

    /** True until the row has been scrolled to its selection once, which happens without animation. */
    var opening = true

    fun resize(count: Int) {
        if (start.size != count) {
            start = IntArray(count)
            width = IntArray(count)
        }
    }

    fun scrollFor(index: Int, scroll: Int, viewport: Int, side: Int): Int =
        if (index in start.indices) chipScroll(start[index], width[index], scroll, viewport, side) else scroll
}

/**
 * The scroll that shows a chip whole with [side] clear on both sides of it, moving as little as possible
 * from [scroll]. [start] is the chip's place in the row after the row's own [side] padding. A chip wider
 * than the [viewport] shows its start.
 */
internal fun chipScroll(start: Int, width: Int, scroll: Int, viewport: Int, side: Int): Int {
    val fromEnd = start + width + 2 * side - viewport
    return when {
        scroll > start -> start
        scroll < fromEnd -> minOf(fromEnd, start)
        else -> scroll
    }
}

/**
 * `.chips`: the presets in one scrolling row, one of them chosen. "Custom" and "Save as preset" lead
 * while the bands match no preset; then the listener's own presets, then the built-in ones. A chip
 * applies its preset at once. The chosen one of the listener's own ends in `more_vert`: tapping it
 * again, or holding any of them, opens Rename and Delete preset over the chip. The row scrolls its
 * selection into view, and back to the start when the selection becomes "Custom".
 *
 * With the equaliser off the row is drawn at half strength and still works: a tap turns it on.
 */
@Composable
private fun PresetChips(
    presets: List<EqPresetChip>,
    selected: EqPresetChip?,
    on: Boolean,
    lit: State<Float>,
    actions: State<EqualiserEditorActions>,
    sidePadding: Dp,
) {
    val scroll = rememberScrollState()
    val spots = remember { ChipSpots() }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val onSheet = LocalComfortContainer.current == ComfortContainer.Sheet
    val selectedIndex = if (selected == null) 0 else presets.indexOfFirst { it.key == selected.key }
    val side = with(LocalDensity.current) { sidePadding.roundToPx() }
    LaunchedEffect(selected?.key) {
        val opening = spots.opening
        if (opening) {
            snapshotFlow { scroll.maxValue }.first { it != Int.MAX_VALUE }
            spots.opening = false
        } else {
            // The chips are laid out again after this composition; their places are right a frame later.
            withFrameNanos { }
        }
        val wanted = if (selected == null) 0 else spots.scrollFor(selectedIndex, scroll.value, scroll.viewportSize, side)
        if (opening) scroll.scrollTo(wanted) else scroll.animateScrollTo(wanted, tween(Settle))
    }
    val rename = stringResource(R.string.rename)
    val delete = stringResource(R.string.equaliser_delete_preset)
    Layout(
        content = {
            if (selected == null) {
                PresetChip(stringResource(R.string.equaliser_preset_custom), selected = true, onClick = { if (!on) actions.value.setOn(true) })
                PresetChip(stringResource(R.string.equaliser_save_preset), selected = false, onClick = { actions.value.askSavePreset() }, save = true)
            }
            for (chip in presets) {
                key(chip.key) {
                    val chosen = chip.key == selected?.key
                    Box {
                        PresetChip(
                            chip.name,
                            selected = chosen,
                            onClick = {
                                // Off, a tap on the chosen chip turns the equaliser on, as any chip does.
                                if (!chosen || !on) actions.value.choosePreset(chip) else if (chip.isUser) menuFor = chip.key
                            },
                            onLongClick = if (chip.isUser) { { menuFor = chip.key } } else null,
                            more = chosen && chip.isUser,
                            extraActions = if (chip.isUser) {
                                listOf(
                                    CustomAccessibilityAction(rename) { actions.value.askRenamePreset(chip); true },
                                    CustomAccessibilityAction(delete) { actions.value.askDeletePreset(chip); true },
                                )
                            } else {
                                null
                            },
                        )
                        if (chip.isUser) {
                            ComfortMenu(expanded = menuFor == chip.key, onDismiss = { menuFor = null }, onSheet = onSheet) {
                                MenuRow(Symbol.Edit, rename, {
                                    menuFor = null
                                    actions.value.askRenamePreset(chip)
                                })
                                MenuRow(Symbol.Delete, delete, {
                                    menuFor = null
                                    actions.value.askDeletePreset(chip)
                                }, danger = true)
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 0.5f + 0.5f * lit.value }
            .selectableGroup()
            .horizontalScroll(scroll)
            .padding(horizontal = sidePadding, vertical = 6.dp),
    ) { measurables, constraints ->
        // A row, 8dp between chips, that also notes where each chip landed.
        val gap = 8.dp.roundToPx()
        val placeables = measurables.map { it.measure(Constraints(maxHeight = constraints.maxHeight)) }
        spots.resize(placeables.size)
        var x = 0
        var height = 0
        placeables.forEachIndexed { index, placeable ->
            spots.start[index] = x
            spots.width[index] = placeable.width
            x += placeable.width + gap
            height = max(height, placeable.height)
        }
        layout(max(x - gap, constraints.minWidth), height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(spots.start[index], (height - placeable.height) / 2) }
        }
    }
}

/**
 * `.chip` as a single choice: the kit's 36dp pill ([FilterChip]'s metrics and colours), a radio button
 * to TalkBack. [more] ends it in `more_vert` (a chosen preset of the listener's own); [save] is the
 * "Save as preset" action, a button with `add` and its label in accent ink.
 */
@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    more: Boolean = false,
    save: Boolean = false,
    extraActions: List<CustomAccessibilityAction>? = null,
) {
    val colors = Comfort.colors
    val onCard = LocalComfortContainer.current != ComfortContainer.Ground
    val ink = when {
        selected -> colors.onAccentSoft
        save -> colors.accentInk
        else -> colors.on
    }
    val lead = when {
        selected -> Symbol.Check
        save -> Symbol.Add
        else -> null
    }
    Row(
        Modifier
            .height(36.dp)
            .clip(ComfortShapes.Pill)
            .background(if (selected) colors.accentSoft else if (onCard) colors.fill else colors.card)
            .then(
                if (save) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                        .combinedClickable(role = Role.RadioButton, onLongClick = onLongClick, onClick = onClick)
                        .semantics {
                            this.selected = selected
                            if (extraActions != null) customActions = extraActions
                        }
                },
            )
            // `.chip .ms{margin-left:-4px}`, `.chip .ms.more{margin-right:-8px}`.
            .padding(start = if (lead != null) 12.dp else 16.dp, end = if (more) 8.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (lead != null) ComfortIcon(lead, size = 18.dp, tint = ink)
        Text(label, style = onest(14f, 18f, if (selected) 600 else 500), color = ink, maxLines = 1)
        if (more) ComfortIcon(Symbol.MoreVert, size = 18.dp, tint = ink)
    }
}

/** Samples of the response across the plot. Enough for a smooth line at this width. */
private const val ResponsePoints = 96

/** The response is drawn to 14 dB either way, so neighbouring boosts that sum past +12 still show. */
private const val ResponseRangeDb = 14f

/** The bands' drawing is 184dp tall, with 0 dB in the middle and 6.5dp to a decibel. */
private val BandsHeight = 184.dp
private val BandsZero = 92.dp
private val BandStep = 6.5.dp

/**
 * `.eq-bands`: the plot on a `--fill` card. Back to front: dashed rules at +12 and -12 dB and a line at
 * 0, with their labels in the card's start padding; ten capsule tracks; the equaliser's real response,
 * the line in the accent with a soft fill down to 0 dB; the switch's knob on each track; the frequency
 * under each. The response is drawn at the knobs' own 6.5dp to a decibel, and the engine solves its
 * filters to pass through the bands, so the line runs through every knob. Off, the line is the flat one
 * that is playing and the knobs' rings lose the accent.
 *
 * Drag up or down anywhere in a band's column and its value follows the finger from where it is, a
 * decibel at a time; a tap alone does nothing, so resting a thumb or scrolling past cannot change the
 * sound; a double tap puts the band back to 0 dB. While a band is dragged its knob grows and its value
 * rides above it.
 *
 * One drawing, which reads the gains only when it draws; the response path is rebuilt only when the
 * gains or the switch's fade changed. TalkBack gets each band as a slider of its own ([BandSemantics]),
 * laid over its column, and nothing of the drawing.
 */
@Composable
private fun BandSliders(
    bands: BandsState,
    curve: State<EqCurve>,
    lit: State<Float>,
    actions: State<EqualiserEditorActions>,
    modifier: Modifier = Modifier,
) {
    val colors = rememberUpdatedState(Comfort.colors)
    val shadow = Comfort.colors.shadowSmall
    val resources = LocalResources.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    // The bubble's words for each half decibel from -12 to +12, made as they are first needed.
    val values = remember(resources) { arrayOfNulls<String>(49) }
    // The start padding holds the dB labels, the end padding lets the last knob breathe.
    Box(
        modifier
            .fillMaxWidth()
            .background(Comfort.colors.fill, ComfortShapes.Control)
            .padding(start = PlotGutterStart, top = 6.dp, end = PlotGutterEnd, bottom = 10.dp),
    ) {
        Box(Modifier.fillMaxWidth().pointerInput(bands) { bandGestures(bands) }) {
            Column {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(BandsHeight)
                        .drawWithCache {
                            val column = size.width / Bands
                            val zero = BandsZero.toPx()
                            val perDb = BandStep.toPx()
                            val trackTop = 14.dp.toPx()
                            val trackBottom = 170.dp.toPx()
                            val trackWidth = 6.dp.toPx()
                            val hair = 1.dp.toPx()
                            val dashes = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
                            val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            // The labels end 6dp before the plot, in the card's padding, so the drawing is never clipped.
                            val axisStyle = onest(11f, 16f).tabular()
                            val marks = floatArrayOf(EqualiserBands.MAX_DB, 0f, EqualiserBands.MIN_DB)
                            val labels = marks.map { measurer.measure(EqualiserBands.formatDb(it), axisStyle) }
                            val labelEnd = -6.dp.toPx()
                            val line = Path()
                            val area = Path()
                            val scaled = FloatArray(Bands)
                            var drawnVersion = -1
                            var drawnLit = -1f
                            val knob = 10.dp.toPx()
                            val knobHeld = 13.dp.toPx()
                            val ring = 2.dp.toPx()
                            // The knob's shadow is one soft disc, drawn a little low under each knob: no blur, no layer.
                            val shadowReach = knob + 4.dp.toPx()
                            val shadowDrop = 1.5.dp.toPx()
                            val shadowBrush = Brush.radialGradient(
                                0f to shadow,
                                (knob - ring) / shadowReach to shadow,
                                1f to shadow.copy(alpha = 0f),
                                center = Offset.Zero,
                                radius = shadowReach,
                            )
                            val bubble = Size(62.dp.toPx(), 28.dp.toPx())
                            val bubbleCorner = CornerRadius(bubble.height / 2)
                            val bubbleGap = 10.dp.toPx()
                            val bubbleStyle = onest(12.5f, 16f, 600).tabular()
                            onDrawBehind {
                                val ink = colors.value
                                val gains = bands.gains
                                val on = lit.value
                                val accent = lerp(ink.on3, ink.accent, on)
                                val grown = bands.grow.value
                                val held = bands.held
                                if (bands.version != drawnVersion || on != drawnLit) {
                                    drawnVersion = bands.version
                                    drawnLit = on
                                    line.rewind()
                                    area.rewind()
                                    if (on <= 0f) {
                                        line.moveTo(0f, zero)
                                        line.lineTo(size.width, zero)
                                    } else {
                                        for (i in 0 until Bands) scaled[i] = gains[i] * on
                                        val response = EqualiserBands.response(scaled, ResponsePoints)
                                        val step = size.width / (response.size - 1)
                                        for (i in response.indices) {
                                            val x = step * i
                                            val y = zero - response[i].coerceIn(-ResponseRangeDb, ResponseRangeDb) * perDb
                                            if (i == 0) {
                                                line.moveTo(x, y)
                                                area.moveTo(x, y)
                                            } else {
                                                line.lineTo(x, y)
                                                area.lineTo(x, y)
                                            }
                                        }
                                        area.lineTo(size.width, zero)
                                        area.lineTo(0f, zero)
                                        area.close()
                                    }
                                }
                                for (i in marks.indices) {
                                    val y = zero - marks[i] * perDb
                                    drawLine(ink.fill2, Offset(0f, y), Offset(size.width, y), hair, pathEffect = if (marks[i] == 0f) null else dashes)
                                    val label = labels[i]
                                    drawText(label, color = ink.on2, topLeft = Offset(labelEnd - label.size.width, y - label.size.height / 2f))
                                }
                                for (i in 0 until Bands) {
                                    val x = column * (i + 0.5f)
                                    drawLine(ink.fill2, Offset(x, trackTop), Offset(x, trackBottom), trackWidth, StrokeCap.Round)
                                }
                                if (on > 0f) drawPath(area, ink.accent, alpha = 0.2f * on)
                                drawPath(line, accent, style = stroke)
                                for (i in 0 until Bands) {
                                    val x = column * (i + 0.5f)
                                    val y = zero - gains[i] * perDb
                                    val radius = if (i == held) knob + (knobHeld - knob) * grown else knob
                                    translate(x, y + shadowDrop) {
                                        scale(radius / knob, pivot = Offset.Zero) { drawCircle(shadowBrush, shadowReach, Offset.Zero) }
                                    }
                                    // The ring keeps the white knob apart from the line and the card.
                                    drawCircle(accent, radius, Offset(x, y))
                                    drawCircle(ink.knob, radius - ring, Offset(x, y))
                                }
                                if (held >= 0 && grown > 0f) {
                                    val gain = gains[held]
                                    val x = column * (held + 0.5f)
                                    val top = zero - gain * perDb - (knob + (knobHeld - knob) * grown) - bubbleGap - bubble.height
                                    drawRoundRect(ink.toast, Offset(x - bubble.width / 2, top), bubble, bubbleCorner, alpha = grown)
                                    val slot = ((gain - EqualiserBands.MIN_DB) * 2f).roundToInt().coerceIn(0, values.lastIndex)
                                    val words = values[slot]
                                        ?: resources.getString(R.string.equaliser_db, EqualiserBands.formatDb(gain)).also { values[slot] = it }
                                    val text = measurer.measure(words, bubbleStyle)
                                    drawText(
                                        text,
                                        color = ink.onToast,
                                        topLeft = Offset(x - text.size.width / 2f, top + (bubble.height - text.size.height) / 2f),
                                        alpha = grown,
                                    )
                                }
                            }
                        },
                )
                // The axis is read out band by band below, not as ten loose labels.
                Row(Modifier.fillMaxWidth().padding(top = 2.dp).clearAndSetSemantics { }) {
                    val ink = Comfort.colors.on2
                    val style = onest(12f, 16f)
                    for (label in EqualiserBands.labels) {
                        Text(label, style = style, color = ink, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                    }
                }
            }
            BandSemanticsRow(curve, actions, Modifier.matchParentSize())
        }
    }
}

/**
 * The drag and the double tap on the bands. The band is the column the finger came down in; the value
 * moves with the finger from where it was, never jumping to the touch point. The drag takes the
 * gesture once it is past the touch slop, so a sheet or a page scrolls only when the finger starts
 * outside the bands.
 */
private suspend fun PointerInputScope.bandGestures(bands: BandsState) {
    val perDb = BandStep.toPx()
    var tapped = -1
    var tappedAt = 0L
    awaitEachGesture {
        val down = awaitFirstDown()
        val band = (down.position.x / (size.width.toFloat() / Bands)).toInt().coerceIn(0, Bands - 1)
        var travel = 0f
        val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
            change.consume()
            travel = overSlop
        }
        if (drag != null) {
            tapped = -1
            val origin = bands.beginDrag(band)
            try {
                bands.dragTo(origin - travel / perDb)
                verticalDrag(drag.id) { change ->
                    travel += change.positionChange().y
                    change.consume()
                    bands.dragTo(origin - travel / perDb)
                }
            } finally {
                bands.endDrag()
            }
        } else {
            val lifted = currentEvent.changes.firstOrNull { it.id == down.id }?.takeIf { !it.pressed && !it.isConsumed }
            when {
                lifted == null -> tapped = -1
                band == tapped && lifted.uptimeMillis - tappedAt <= viewConfiguration.doubleTapTimeoutMillis -> {
                    tapped = -1
                    bands.reset(band)
                }
                else -> {
                    tapped = band
                    tappedAt = lifted.uptimeMillis
                }
            }
        }
    }
}

/** The ten sliders TalkBack sees, one over each band's column, reading the model's curve. */
@Composable
private fun BandSemanticsRow(curve: State<EqCurve>, actions: State<EqualiserEditorActions>, modifier: Modifier = Modifier) {
    val gains = curve.value
    Row(modifier) {
        for (band in 0 until Bands) BandSemantics(band, gains[band], actions, Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * One band for TalkBack: "31 Hz", "+4 dB", a slider from -12 to +12 that its volume keys and swipes
 * step a decibel at a time, with "Set to 0 dB" as an action.
 */
@Composable
private fun BandSemantics(band: Int, gain: Float, actions: State<EqualiserEditorActions>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val name = remember(context, band) { EqualiserBands.describe(context, band) }
    val value = stringResource(R.string.equaliser_db, EqualiserBands.formatDb(gain))
    val reset = stringResource(R.string.equaliser_band_reset)
    Box(
        modifier.semantics {
            contentDescription = name
            stateDescription = value
            progressBarRangeInfo = ProgressBarRangeInfo(gain, EqualiserBands.MIN_DB..EqualiserBands.MAX_DB, steps = 23)
            setProgress { wanted ->
                val snapped = wanted.roundToInt().toFloat().coerceIn(EqualiserBands.MIN_DB, EqualiserBands.MAX_DB)
                if (snapped == gain) {
                    false
                } else {
                    actions.value.setBand(band, snapped)
                    true
                }
            }
            customActions = listOf(
                CustomAccessibilityAction(reset) {
                    actions.value.setBand(band, 0f)
                    true
                },
            )
        },
    )
}

// ── Preset dialogs ───────────────────────────────────────────────────────────

/** The preset dialog the editor has open: naming a new preset, renaming one, or the question before deleting one (frames F1, K4). */
@Composable
private fun PresetDialog(dialog: EqPresetDialog, editor: EqualiserEditorState, actions: EqualiserEditorActions) {
    when (dialog) {
        EqPresetDialog.Save -> PresetNameDialog(
            title = stringResource(R.string.equaliser_save_preset),
            initial = "",
            confirmText = stringResource(R.string.save),
            taken = { editor.nameTaken(it) },
            onConfirm = actions.savePreset,
            onDismiss = actions.dismissDialog,
        )
        is EqPresetDialog.Rename -> key(dialog.id) {
            PresetNameDialog(
                title = stringResource(R.string.equaliser_rename_preset_title),
                initial = dialog.name,
                confirmText = stringResource(R.string.rename),
                taken = { editor.nameTaken(it, exceptId = dialog.id) },
                onConfirm = { actions.renamePreset(dialog.id, it) },
                onDismiss = actions.dismissDialog,
            )
        }
        is EqPresetDialog.Delete -> ComfortDialog(
            title = stringResource(R.string.equaliser_delete_preset_title, dialog.name),
            onDismiss = actions.dismissDialog,
            confirmText = stringResource(R.string.equaliser_delete_preset),
            onConfirm = { actions.deletePreset(dialog.id) },
            dismissText = stringResource(R.string.cancel),
            body = stringResource(R.string.equaliser_delete_preset_body),
            danger = true,
        )
    }
}

/**
 * The kit's bottom dialog with the "Preset name" field focused. The confirm button waits for a name no
 * other preset has; a name that is taken says so under the field.
 */
@Composable
private fun PresetNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    taken: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val isTaken = name.isNotBlank() && taken(name)
    val canConfirm = name.isNotBlank() && !isTaken
    val confirm = { if (canConfirm) onConfirm(name.trim()) }
    ComfortDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = confirm,
        dismissText = stringResource(R.string.cancel),
        confirmEnabled = canConfirm,
    ) {
        DialogField(name, { name = it }, stringResource(R.string.equaliser_preset_name_label), focusRequester = focus, onDone = confirm)
        if (isTaken) {
            Text(
                stringResource(R.string.equaliser_preset_name_taken),
                style = onest(12.5f, 16f),
                color = Comfort.colors.danger,
                modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

// ── Now Playing ──────────────────────────────────────────────────────────────

/**
 * `.eq-on`: the capsule in Now Playing's header while the song plays with its own or its album's
 * setting: the equaliser glyph in accent ink and "This song" or "This album". Tapping it opens the
 * sheet. It fades in and out and nothing else moves; with [scope] null it draws nothing.
 */
@Composable
internal fun EqualiserIndicator(scope: EqScope?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // What it last said, so the words stay while it fades out.
    val last = remember { arrayOf(scope) }
    if (scope != null) last[0] = scope
    AnimatedVisibility(visible = scope != null, modifier = modifier, enter = fadeIn(tween(Settle)), exit = fadeOut(tween(Settle))) {
        val colors = Comfort.colors
        val album = last[0] == EqScope.Album
        val description = stringResource(if (album) R.string.equaliser_indicator_album_cd else R.string.equaliser_indicator_song_cd)
        val source = remember { MutableInteractionSource() }
        // A 48dp target around a 32dp capsule, 2dp clear of the menu button.
        Box(
            Modifier
                .padding(end = 2.dp)
                .height(48.dp)
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .height(32.dp)
                    .clip(ComfortShapes.Pill)
                    .background(colors.fill)
                    .indication(source, ripple())
                    .padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ComfortIcon(Symbol.Equalizer, size = 18.dp, tint = colors.accentInk)
                Text(
                    stringResource(if (album) R.string.equaliser_indicator_album else R.string.equaliser_indicator_song),
                    style = onest(13f, 18f, 500),
                    color = colors.on2,
                    maxLines = 1,
                    // The description above says it in full.
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}
