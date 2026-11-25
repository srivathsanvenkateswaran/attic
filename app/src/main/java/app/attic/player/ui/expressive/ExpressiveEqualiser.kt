package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.playback.eq.EqScope
import app.attic.player.playback.eq.EqTarget
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.EqOwnRow
import app.attic.player.ui.model.EqPresetChip
import app.attic.player.ui.model.EqPresetDialog
import app.attic.player.ui.model.EqRemoval
import app.attic.player.ui.model.EqualiserEditorActions
import app.attic.player.ui.model.EqualiserEditorState
import app.attic.player.ui.model.EqualiserPageActions
import app.attic.player.ui.model.EqualiserPageState
import app.attic.player.ui.model.EqualiserSheetActions
import app.attic.player.ui.model.EqualiserSheetState
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/*
 * The equaliser in the Expressive style (pixel/11-equaliser.html): the sheet over Now Playing and
 * Settings > Playback > Equaliser. Both are the same two group cells: the scope, which says what the
 * bands change, and the editor, which is the preset chips over the band graph.
 */

/**
 * The equaliser for the song that is playing (frames B to F2, I). The title row carries the switch.
 * Under the group, the actions that change the scope: saving for the song or its album as a button
 * group, and the outlined button that removes an own setting with what it removes beside it. Saving or
 * removing changes the scope cell in place; the sheet stays open.
 */
@Composable
fun ExpressiveEqualiserSheet(state: EqualiserSheetState, actions: EqualiserSheetActions) {
    val editor = state.editor
    ExpressiveSheet(onDismiss = actions.dismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            EqualiserTitle(editor.on, actions.editor.setOn)
            if (!editor.on) SheetSubtitle(stringResource(R.string.equaliser_off_note), maxLines = 3)
            ListGroup(2) { index ->
                if (index == 0) {
                    EqualiserScopeCell(
                        state.scope,
                        when (state.scope) {
                            EqScope.Song -> state.songTitle
                            EqScope.Album -> state.albumName.orEmpty()
                            EqScope.Default -> stringResource(R.string.equaliser_scope_default_body)
                        },
                    )
                } else {
                    EqualiserEditor(editor, actions.editor)
                }
            }
            val saving = state.canSaveForSong || state.canSaveForAlbum
            if (saving) SaveActions(state.canSaveForSong, state.canSaveForAlbum, actions.saveForSong, actions.saveForAlbum)
            // The note explains saving to someone who has not saved yet; once the song plays with a
            // setting of its own or its album's, the scope cell already shows what saving does.
            if (saving && state.scope == EqScope.Default) {
                Text(
                    stringResource(R.string.equaliser_saved_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp),
                )
            }
            state.removal?.let { RemovalRow(it, actions.remove, afterSave = saving) }
        }
    }
}

/** The sheet's title with the master switch at its end; the whole row is the switch. */
@Composable
private fun EqualiserTitle(on: Boolean, setOn: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = on, role = Role.Switch, onValueChange = setOn)
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.equaliser), style = Expressive.type.headlineSmallEmphasized, modifier = Modifier.weight(1f))
        ExpressiveSwitch(on, onCheckedChange = null)
    }
}

/** "Save for this song" (the one filled button) and "Save for this album" (tonal). One alone fills the width. */
@Composable
private fun SaveActions(song: Boolean, album: Boolean, saveForSong: () -> Unit, saveForAlbum: () -> Unit) {
    ButtonGroup(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp), fill = true) {
        if (song) {
            val interaction = remember { MutableInteractionSource() }
            ExpressiveButton(stringResource(R.string.equaliser_save_song), saveForSong, Modifier.groupItem(interaction), interactionSource = interaction)
        }
        if (album) {
            val interaction = remember { MutableInteractionSource() }
            ExpressiveButton(
                stringResource(R.string.equaliser_save_album),
                saveForAlbum,
                Modifier.groupItem(interaction),
                variant = ButtonVariant.Tonal,
                interactionSource = interaction,
            )
        }
    }
}

/**
 * "Use default" or "Use album setting", with what it removes beside it. A screen reader gets the two
 * as one button ("Use default. Removes this song's setting"), so the consequence is heard before the tap.
 */
@Composable
private fun RemovalRow(removal: EqRemoval, onRemove: () -> Unit, afterSave: Boolean) {
    val label = stringResource(if (removal == EqRemoval.SongToAlbum) R.string.equaliser_use_album else R.string.equaliser_use_default)
    val consequence = stringResource(if (removal == EqRemoval.AlbumToDefault) R.string.equaliser_removes_album else R.string.equaliser_removes_song)
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (afterSave) 8.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExpressiveButton(
            label,
            onRemove,
            Modifier.semantics { contentDescription = "$label. $consequence" },
            variant = ButtonVariant.Outlined,
        )
        Text(
            consequence,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).clearAndSetSemantics { },
        )
    }
}

/**
 * The scope cell: which setting the bands below change. The default is the kit's round tonal glyph on
 * the group's cell colour. A song or album of its own squares the glyph off and fills it with primary,
 * the kit's "squared off means on", and fills the whole cell with primary container: someone dragging a
 * band has their eye on the graph, and a cell that changes colour is seen from there where a 40dp glyph
 * is not. The corner moves on the fast spatial spring and the colours on default effects, so a save or
 * a removal is seen in place; nothing animates until the scope changes. Read as one node, politely
 * announced when it changes.
 */
@Composable
fun EqualiserScopeCell(scope: EqScope, detail: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val own = scope != EqScope.Default
    val corner by animateDpAsState(if (own) ExpressiveShapes.Medium else 20.dp, ExpressiveMotion.fastSpatial(), label = "scopeCorner")
    val fill by animateColorAsState(if (own) colors.primary else colors.secondaryContainer, ExpressiveMotion.defaultEffects(), label = "scopeFill")
    val ink by animateColorAsState(if (own) colors.onPrimary else colors.onSecondaryContainer, ExpressiveMotion.defaultEffects(), label = "scopeInk")
    val cell = groupCellColor()
    val cellFill by animateColorAsState(if (own) colors.primaryContainer else cell, ExpressiveMotion.defaultEffects(), label = "scopeCell")
    val title by animateColorAsState(if (own) colors.onPrimaryContainer else colors.onSurface, ExpressiveMotion.defaultEffects(), label = "scopeTitle")
    val body by animateColorAsState(if (own) colors.onPrimaryContainer else colors.onSurfaceVariant, ExpressiveMotion.defaultEffects(), label = "scopeBody")
    Row(
        modifier
            .fillMaxWidth()
            // Over the group's own fill, which the cell's shape already clips; read while drawing, and
            // not drawn at all while it matches the group's colour.
            .drawBehind { if (cellFill != cell) drawRect(cellFill) }
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(corner)).background(fill), contentAlignment = Alignment.Center) {
            SymbolIcon(scopeSymbol(scope), filled = own, tint = ink)
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when (scope) {
                        EqScope.Song -> R.string.equaliser_scope_song
                        EqScope.Album -> R.string.equaliser_scope_album
                        EqScope.Default -> R.string.equaliser_scope_default
                    },
                ),
                style = Expressive.type.titleLargeEmphasized,
                color = title,
            )
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The one glyph each scope goes by, wherever it is shown. */
internal fun scopeSymbol(scope: EqScope): Symbol = when (scope) {
    EqScope.Song -> Symbol.MusicNote
    EqScope.Album -> Symbol.Album
    EqScope.Default -> Symbol.LibraryMusic
}

/**
 * The editor cell: the preset chips in one scrolling row, then the band graph. The chips sit in 48dp
 * touch targets, so the cell pads 4dp above them and the graph is pulled 8dp up under them, which puts
 * the chips 12dp from the cell's top and the plot 20dp under the chips as the sheet draws them. It also
 * owns the preset dialogs, so the sheet and the Settings page get them alike.
 */
@Composable
fun EqualiserEditor(state: EqualiserEditorState, actions: EqualiserEditorActions, modifier: Modifier = Modifier) {
    // The model builds the list anew each time it emits; handing the chips an equal list by the same
    // instance lets them skip while a band is dragged.
    val presets = remember(state.presets) { state.presets }
    Column(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
        PresetChips(presets, state.selected, state.on, actions)
        BandGraph(state.curve, state.on, actions.setBand, Modifier.pullUp(8.dp))
    }
    PresetDialogs(state, actions)
}

private const val CustomKey = "#custom"
private const val SaveKey = "#save"

/** The kit's filter chip reads as a checkbox; in this row exactly one is chosen, so it reads as a radio button. */
private val RadioChip = Modifier.semantics { role = Role.RadioButton }

/**
 * The presets, single choice: "Custom" and "Save as preset" while the bands match none, then the saved
 * ones, then the built-in ones. It opens with the selected chip in view, one chip of context before it,
 * and scrolls to keep it in view when the selection changes. The selected chip is filled with primary,
 * the look a connected button group gives its one choice: the kit's tonal fill is too close to a tonal
 * cell's under some schemes to be seen. Off, it goes back to tonal and the row draws at half strength,
 * so nothing in the editor keeps the accent while nothing is applied; every chip still works, and
 * choosing one turns the equaliser on.
 */
@Composable
private fun PresetChips(presets: List<EqPresetChip>, selected: EqPresetChip?, on: Boolean, actions: EqualiserEditorActions) {
    val first = (presets.indexOfFirst { it.key == selected?.key } - 1).coerceAtLeast(0)
    // Past the start, the row opens 4dp further on, so the chip before the one of context is wholly out
    // of the cell rather than a sliver at its edge.
    val firstOffset = if (first > 0) with(LocalDensity.current) { 4.dp.roundToPx() } else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = first, initialFirstVisibleItemScrollOffset = firstOffset)
    val selectedKey by rememberUpdatedState(selected?.key)
    val shown by rememberUpdatedState(presets)
    LaunchedEffect(listState) {
        snapshotFlow { selectedKey }.drop(1).collectLatest { key ->
            // A frame, so the row has been laid out with the chips the new selection brought or took away.
            withFrameNanos { }
            if (key == null) listState.animateScrollToItem(0) else listState.reveal(shown.indexOfFirst { it.key == key })
        }
    }
    val strength by animateFloatAsState(if (on) 1f else 0.5f, ExpressiveMotion.defaultEffects(), label = "chipsStrength")
    val colors = MaterialTheme.colorScheme
    val chosenFill by animateColorAsState(if (on) colors.primary else colors.secondaryContainer, ExpressiveMotion.defaultEffects(), label = "chipFill")
    val chosenInk by animateColorAsState(if (on) colors.onPrimary else colors.onSecondaryContainer, ExpressiveMotion.defaultEffects(), label = "chipInk")
    var menuFor by remember { mutableStateOf<String?>(null) }
    LazyRow(
        Modifier.fillMaxWidth().selectableGroup().graphicsLayer { alpha = strength },
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected == null) {
            item(key = CustomKey, contentType = "chip") {
                ExpressiveFilterChip(
                    stringResource(R.string.equaliser_preset_custom),
                    true,
                    { if (!on) actions.setOn(true) },
                    RadioChip,
                    selectedContainerColor = chosenFill,
                    selectedContentColor = chosenInk,
                )
            }
            item(key = SaveKey, contentType = "save") {
                PresetChip(stringResource(R.string.equaliser_save_preset), selected = false, onClick = actions.askSavePreset, action = true)
            }
        }
        items(presets, key = { it.key }, contentType = { if (it.isUser) "saved" else "chip" }) { chip ->
            val current = chip.key == selected?.key
            if (!chip.isUser) {
                // Choosing the chip that is already selected only matters while off, when it turns the equaliser on.
                ExpressiveFilterChip(
                    chip.name,
                    current,
                    { if (!current || !on) actions.choosePreset(chip) },
                    RadioChip,
                    selectedContainerColor = chosenFill,
                    selectedContentColor = chosenInk,
                )
            } else {
                val rename = stringResource(R.string.rename)
                val delete = stringResource(R.string.equaliser_delete_preset)
                Box {
                    PresetChip(
                        chip.name,
                        selected = current,
                        // The selected chip's second tap opens its menu, which is what its "more" glyph promises.
                        onClick = { if (current && on) menuFor = chip.key else actions.choosePreset(chip) },
                        onLongClick = { menuFor = chip.key },
                        more = current,
                        selectedFill = chosenFill,
                        selectedInk = chosenInk,
                        menuActions = listOf(
                            CustomAccessibilityAction(rename) { actions.askRenamePreset(chip); true },
                            CustomAccessibilityAction(delete) { actions.askDeletePreset(chip); true },
                        ),
                    )
                    ExpressiveMenu(expanded = menuFor == chip.key, onDismiss = { menuFor = null }, offset = DpOffset(0.dp, (-4).dp)) {
                        ExpressiveMenuItem(Symbol.Edit, rename, onClick = {
                            menuFor = null
                            actions.askRenamePreset(chip)
                        })
                        ExpressiveMenuItem(Symbol.Delete, delete, danger = true, onClick = {
                            menuFor = null
                            actions.askDeletePreset(chip)
                        })
                    }
                }
            }
        }
    }
}

/** Scrolls just far enough that item [index] is whole inside the row's padding; an item out of sight comes to the start. */
private suspend fun LazyListState.reveal(index: Int) {
    if (index < 0) return
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val end = info.viewportEndOffset - info.afterContentPadding
    val by = when {
        item.offset < 0 -> item.offset
        item.offset + item.size > end -> item.offset + item.size - end
        else -> 0
    }
    if (by != 0) animateScrollBy(by.toFloat())
}

/**
 * A chip the kit's [ExpressiveFilterChip] can't draw, to the same measure (32dp in a 48dp target, 8dp
 * corners, 18dp glyphs): a saved preset, which takes a long press and ends in the "more" glyph while it
 * is selected ([more]); and the "Save as preset" action ([action]), outlined with a plus and in primary.
 * Selected, it fills with [selectedFill] and draws in [selectedInk], as the row's other chips do.
 * [menuActions] offers a saved preset's menu to a screen reader.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    more: Boolean = false,
    action: Boolean = false,
    selectedFill: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedInk: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    menuActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(ExpressiveShapes.Small)
    val ink = when {
        action -> colors.primary
        selected -> selectedInk
        else -> colors.onSurfaceVariant
    }
    val leading = if (action) Symbol.Add else if (selected) Symbol.Check else null
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .height(32.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(selectedFill) else Modifier.border(1.dp, colors.outlineVariant, shape))
            .combinedClickable(role = if (action) Role.Button else Role.RadioButton, onClick = onClick, onLongClick = onLongClick)
            .semantics {
                if (!action) this.selected = selected
                if (menuActions.isNotEmpty()) customActions = menuActions
            }
            .padding(start = if (leading != null) 8.dp else 16.dp, end = if (more) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            SymbolIcon(leading, size = 18.dp, tint = ink)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1)
        if (more) {
            Spacer(Modifier.width(6.dp))
            SymbolIcon(Symbol.MoreVert, size = 18.dp, tint = selectedInk)
        }
    }
}

/** The preset dialog the model says is open: naming a new preset, renaming one, or confirming a delete. */
@Composable
private fun PresetDialogs(state: EqualiserEditorState, actions: EqualiserEditorActions) {
    when (val dialog = state.dialog) {
        EqPresetDialog.Save -> PresetNameDialog(
            title = stringResource(R.string.equaliser_save_preset),
            initial = "",
            confirm = stringResource(R.string.save),
            taken = { state.nameTaken(it) },
            onConfirm = actions.savePreset,
            onDismiss = actions.dismissDialog,
        )
        is EqPresetDialog.Rename -> PresetNameDialog(
            title = stringResource(R.string.equaliser_rename_preset_title),
            initial = dialog.name,
            confirm = stringResource(R.string.rename),
            taken = { state.nameTaken(it, exceptId = dialog.id) },
            onConfirm = { actions.renamePreset(dialog.id, it) },
            onDismiss = actions.dismissDialog,
        )
        is EqPresetDialog.Delete -> ExpressiveDialog(
            title = stringResource(R.string.equaliser_delete_preset_title, dialog.name),
            onDismiss = actions.dismissDialog,
            body = stringResource(R.string.equaliser_delete_preset_body),
            confirmButton = {
                ExpressiveButton(
                    stringResource(R.string.equaliser_delete_preset),
                    { actions.deletePreset(dialog.id) },
                    variant = ButtonVariant.Text,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            },
            dismissButton = { ExpressiveButton(stringResource(R.string.cancel), actions.dismissDialog, variant = ButtonVariant.Text) },
        )
        null -> Unit
    }
}

/**
 * The kit's name dialog for a preset: the field focused on open with the cursor after [initial]. The
 * confirm waits for a name that is not blank and that no other preset has; a taken name says so under
 * the field.
 */
@Composable
private fun PresetNameDialog(
    title: String,
    initial: String,
    confirm: String,
    taken: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val clash = name.text.isNotBlank() && taken(name.text)
    val valid = name.text.isNotBlank() && !clash
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ExpressiveDialog(
        title = title,
        onDismiss = onDismiss,
        confirmButton = { ExpressiveButton(confirm, { onConfirm(name.text.trim()) }, variant = ButtonVariant.Text, enabled = valid) },
        dismissButton = { ExpressiveButton(stringResource(R.string.cancel), onDismiss, variant = ButtonVariant.Text) },
    ) {
        DialogTextField(
            name,
            { name = it },
            stringResource(R.string.equaliser_preset_name_label),
            Modifier.focusRequester(focus),
            error = if (clash) stringResource(R.string.equaliser_preset_name_taken) else null,
            onDone = { if (valid) onConfirm(name.text.trim()) },
        )
    }
}

// Settings > Playback > Equaliser.

/**
 * The page (frames H, K5), laid out like Settings: the switch, the default in the sheet's two cells on
 * the app scheme, then every album and song with its own setting, each naming its preset and ending in
 * the button that removes it. A removed row slides away; with none left, one note stands in for both
 * lists. The phone's own equaliser, when it has one, is the last row.
 */
@Composable
fun ExpressiveEqualiserPage(state: EqualiserPageState, actions: EqualiserPageActions) {
    val listState = rememberLazyListState()
    val title = stringResource(R.string.equaliser)
    val editor = state.editor
    LargeTitlePage(title, listState, actions.back) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = LocalBottomPadding.current)) {
            item(key = "title") { LargeTitle(title, bottom = 20.dp) }
            item(key = "switch") { ListGroup(1) { UseEqualiserRow(editor.on, actions.editor.setOn) } }
            item(key = "default") {
                SettingsSection(stringResource(R.string.equaliser_default))
                ListGroup(2) { index ->
                    if (index == 0) {
                        EqualiserScopeCell(EqScope.Default, stringResource(R.string.equaliser_scope_default_body))
                    } else {
                        EqualiserEditor(editor, actions.editor)
                    }
                }
            }
            if (state.albums.isEmpty() && state.songs.isEmpty()) {
                item(key = "none") {
                    Column(Modifier.animateItem()) {
                        SettingsSection(stringResource(R.string.equaliser_own_none_title))
                        ListGroup(1) { SetNote(stringResource(R.string.equaliser_own_none)) }
                    }
                }
            }
            ownRows("albums", R.string.equaliser_own_albums, state.albums, album = true, actions.remove)
            ownRows("songs", R.string.equaliser_own_songs, state.songs, album = false, actions.remove)
            if (state.phoneEqualiserAvailable) {
                item(key = "phone") {
                    Column(Modifier.animateItem()) {
                        Spacer(Modifier.height(24.dp))
                        ListGroup(1) { PhoneEqualiserRow(actions.openPhoneEqualiser) }
                    }
                }
            }
        }
    }
}

/** "Use equaliser" with the switch. Off, it says under the title how the equaliser comes back on. */
@Composable
private fun UseEqualiserRow(on: Boolean, setOn: (Boolean) -> Unit) {
    SetIconRow(
        Symbol.Equalizer,
        modifier = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = setOn),
        center = on,
        trailing = { ExpressiveSwitch(on, onCheckedChange = null, modifier = if (on) Modifier else Modifier.padding(top = 4.dp)) },
    ) {
        RowTitle(stringResource(R.string.equaliser_use))
        if (!on) SupportingText(stringResource(R.string.equaliser_off_note))
    }
}

/** A section of the page's own-setting lists: its heading, then one group cell a row. Nothing when the list is empty. */
private fun LazyListScope.ownRows(section: String, title: Int, rows: List<EqOwnRow>, album: Boolean, remove: (EqTarget) -> Unit) {
    if (rows.isEmpty()) return
    item(key = section, contentType = "section") { SettingsSection(stringResource(title), Modifier.animateItem()) }
    items(rows.size, key = { ownKey(rows[it].target) }, contentType = { section }) { index ->
        val row = rows[index]
        Box(Modifier.animateItem().fillMaxWidth().groupCell(index, rows.size, groupCellColor())) {
            val trailing: @Composable () -> Unit = { OwnSetting(row.preset, row.title) { remove(row.target) } }
            if (album) {
                CollectionRow(row.title, row.subtitle, lead = { Cover(Art.of(row.coverId), size = 56.dp) }, onClick = null, trailing = trailing)
            } else {
                SongRow(row.title, row.subtitle, RowLead.Cover(Art.of(row.coverId)), trailing = trailing)
            }
        }
    }
}

private fun ownKey(target: EqTarget): String = when (target) {
    is EqTarget.Album -> "album:${target.key}"
    is EqTarget.Song -> "song:${target.key}"
    EqTarget.Default -> "default"
}

/** The end of an own-setting row: the preset it holds, then the only thing on the row that can be pressed. */
@Composable
private fun OwnSetting(preset: String, title: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // A long name of the listener's own gives way to the song's title.
        Box(Modifier.widthIn(max = 128.dp)) { RowCount(preset) }
        ExpressiveIconButton(Symbol.Close, stringResource(R.string.equaliser_own_remove_cd, title), onRemove)
    }
}

/** Opens the phone's own equaliser panel, which is outside Attic: hence the "opens elsewhere" glyph. */
@Composable
private fun PhoneEqualiserRow(onClick: () -> Unit) {
    SetIconRow(
        Symbol.Smartphone,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        trailing = { SymbolIcon(Symbol.OpenInNew, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)) },
    ) {
        RowTitle(stringResource(R.string.equaliser_phone))
        SupportingText(stringResource(R.string.settings_equaliser_body))
    }
}
