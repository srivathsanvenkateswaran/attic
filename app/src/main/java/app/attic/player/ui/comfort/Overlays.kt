package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.adaptive.DialogMaxWidthDp
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.WidthClass

/**
 * `.sheet`: Material's ModalBottomSheet in the kit's dress: raised, 30dp top corners, a 40×4 grab
 * handle in `--fill-2`, the kit's scrim. It opens fully expanded. Fill it with [SheetHeader] or
 * [SheetTitle], then rows ([SheetRow] in a [Hairlined], or [SongRow]s, which indent to the sheet's
 * margin by themselves). A [SearchField] inside takes `--fill`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfortSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Comfort.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = ComfortShapes.FloatingTop,
        containerColor = colors.raised,
        contentColor = colors.on,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = { SheetGrab() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        CompositionLocalProvider(LocalComfortContainer provides ComfortContainer.Sheet) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 30.dp)) { content() }
        }
    }
}

@Composable
private fun SheetGrab() {
    Box(
        Modifier
            .padding(top = 12.dp, bottom = 12.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(ComfortShapes.Pill)
            .background(Comfort.colors.fill2),
    )
}

/**
 * The sheet drawn in place, not modal: for previews and the gallery (`.strip .sheet`). Screens use
 * [ComfortSheet].
 */
@Composable
fun StaticSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Comfort.colors
    Column(
        modifier
            .fillMaxWidth()
            .floatingShadow(ComfortShapes.FloatingTop, edge = false)
            .clip(ComfortShapes.FloatingTop)
            .background(colors.raised)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetGrab()
        CompositionLocalProvider(LocalComfortContainer provides ComfortContainer.Sheet, LocalContentColor provides colors.on) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

/** `.sheet-head`: a song's 48dp cover, title (17 semibold) and artist, at the top of the track menu. */
@Composable
fun SheetHeader(title: String, subtitle: String?, art: @Composable (Modifier) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        art(Modifier.size(48.dp))
        RowText(title, Modifier.weight(1f), subtitle = subtitle, titleStyle = onest(17f, 22f, 600))
    }
}

/**
 * `.sheet-title`: 20/26 bold ("Add to playlist"). [subtitle] is its `<small>`: 13.5/18 in `--on-2`, 2dp
 * under it, naming what the sheet acts on ("Late drive, 12 songs").
 */
@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 12.dp)) {
        Text(text, style = Comfort.type.barTitle, color = Comfort.colors.on)
        if (subtitle != null) {
            Text(
                subtitle,
                style = onest(13.5f, 18f),
                color = Comfort.colors.on2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * `.sheet-row`: an icon in `--on-2`, the label, and at the end a [value] ("Change") or, when
 * [selected], a tick in accent ink (and a semibold label). [danger] colours it `--danger`. [end] is a
 * quiet trailing glyph in `--on-3` for a row that opens something in place (`.end.go`: `chevron_right`
 * to a picker, `unfold_more` to a pop-over). [summary] is `.sheet-row.two`: a second line under the
 * label (13.5/18 in `--on-2`) saying what the row leads to, in a 64dp row. Split by hairlines inset to
 * the label when placed in a [Hairlined].
 */
@Composable
fun SheetRow(
    symbol: Symbol?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    selected: Boolean = false,
    danger: Boolean = false,
    end: Symbol? = null,
    summary: String? = null,
) {
    val colors = Comfort.colors
    val ink = if (danger) colors.danger else colors.on
    Row(
        modifier
            .fillMaxWidth()
            .hairlineAbove(LocalHairlineAbove.current, 66.dp, 24.dp)
            .clickable(onClick = onClick)
            .heightIn(min = if (summary != null) 64.dp else 52.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (symbol != null) ComfortIcon(symbol, tint = if (danger) colors.danger else colors.on2) else Box(Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = onest(16f, 22f, if (selected) 600 else 400),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) Text(summary, style = Comfort.type.sub, color = colors.on2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = onest(14f, 18f), color = colors.on2, maxLines = 1)
        if (selected) ComfortIcon(Symbol.Check, tint = colors.accentInk)
        if (end != null) ComfortIcon(end, tint = colors.on3)
    }
}

/**
 * `.dialog`: a bottom dialog floating 12dp in from the edges and 22dp above the bottom (it rides above
 * the keyboard: `.above-kbd`), radius 30, raised, with the title (20/26 bold), an optional [body]
 * (15/21), any [content] (a [DialogField]), and two text buttons split by a thin divider. [confirmEnabled]
 * false is `.btn.text.off`; [danger] colours the confirm button `--danger` ("Delete playlist").
 *
 * Drawn in a platform Dialog window, with the kit's scrim instead of the window's dim. Material's
 * AlertDialog centres itself and stacks its buttons at the end, which the kit does not.
 */
@Composable
fun ComfortDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    /** Null for a dialog with one button, such as a result to acknowledge. */
    dismissText: String?,
    modifier: Modifier = Modifier,
    body: String? = null,
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // On a tablet the dialog sits in the middle of the window, at most 560dp wide, or 400 with a field
    // (docs/design/2026-11-25-tablet/SPEC.md, Sheets and dialogs); on a phone it floats at the bottom.
    val tablet = LocalWindowLayout.current.widthClass != WidthClass.Compact
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val colors = Comfort.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = if (tablet) Alignment.Center else Alignment.BottomCenter,
        ) {
            DialogCard(
                title, body, confirmText, onConfirm, dismissText, onDismiss, confirmEnabled, danger,
                if (tablet) {
                    modifier.imePadding().padding(24.dp).widthIn(max = if (content != null) OneFieldDialogWidth else DialogMaxWidthDp.dp)
                } else {
                    modifier.navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 22.dp)
                },
                content,
            )
        }
    }
}

/** A dialog holding one field (New playlist, Rename) on a tablet. */
private val OneFieldDialogWidth = 400.dp

/**
 * The dialog's card on its own, for a preview or the gallery. [ComfortDialog] places it in a window,
 * 12dp in from the edges and 22dp above the bottom.
 */
@Composable
fun DialogCard(
    title: String,
    body: String?,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String?,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = Comfort.colors
    Column(
        modifier
            .fillMaxWidth()
            // kit.css gives the dialog the shadow only; the 1dp edge is the menu's, the dock's and the action bar's.
            .floatingShadow(ComfortShapes.Floating, edge = false)
            .clip(ComfortShapes.Floating)
            .background(colors.raised)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp),
    ) {
        CompositionLocalProvider(LocalComfortContainer provides ComfortContainer.Dialog, LocalContentColor provides colors.on) {
            Text(title, style = Comfort.type.barTitle, color = colors.on)
            if (body != null) Text(body, style = Comfort.type.body, color = colors.on2, modifier = Modifier.padding(top = 8.dp))
            content?.invoke(this)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .padding(horizontal = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissText != null) {
                    DialogButton(dismissText, onDismiss, Modifier.weight(1f))
                    Box(Modifier.size(width = 1.dp, height = 18.dp).background(colors.fill2))
                }
                DialogButton(confirmText, onConfirm, Modifier.weight(1f), enabled = confirmEnabled, danger = danger)
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val colors = Comfort.colors
    Box(
        modifier
            .height(52.dp)
            .clip(ComfortShapes.Pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Comfort.type.button,
            color = (if (danger) colors.danger else colors.accentInk).copy(alpha = if (enabled) 1f else 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * `.field`: a label in accent ink over the text (17/24), underlined 2dp in the accent. [placeholder]
 * shows in `--on-3` while empty. A prefilled [value] opens with the cursor at its end.
 */
@Composable
fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    focusRequester: FocusRequester? = null,
    onDone: (() -> Unit)? = null,
) {
    val colors = Comfort.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(colors.accent, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
            }
            .padding(bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = onest(12.5f, 16f, 500), color = colors.accentInk)
        Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty() && placeholder != null) Text(placeholder, style = onest(17f, 24f), color = colors.on3)
            // A prefilled name ("Rename playlist") opens with the cursor after it, as `.field .caret` draws.
            var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
            val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
            BasicTextField(
                value = shown,
                onValueChange = {
                    field = it
                    if (it.text != value) onValueChange(it.text)
                },
                singleLine = true,
                textStyle = onest(17f, 24f).copy(color = colors.on),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
        }
    }
}

/**
 * `.menu`: Material's DropdownMenu, raised, radius 22, with the floating shadow and edge. It opens
 * beside the anchor it is placed in (a Box around the trigger). Fill it with [MenuRow]s, and a
 * [MenuCaption] first if it needs one. [onSheet] is `.menu.on-sheet`: opened from a sheet's row, it
 * sits one step above the sheet on `--fill`.
 */
@Composable
fun ComfortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onSheet: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Comfort.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 200.dp),
        offset = offset,
        shape = ComfortShapes.Menu,
        containerColor = if (onSheet) colors.fill else colors.raised,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.edge),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.on) { content() }
    }
}

/** The menu drawn in place, for previews and the gallery. Screens use [ComfortMenu]. */
@Composable
fun StaticMenu(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Comfort.colors
    Column(
        modifier
            .widthIn(min = 200.dp)
            .floatingShadow(ComfortShapes.Menu)
            .clip(ComfortShapes.Menu)
            .background(colors.raised)
            .padding(vertical = 6.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.on) { content() }
    }
}

/** `.menu-cap`: a small caption at the top of a pop-over ("Song language"). */
@Composable
fun MenuCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = onest(13f, 18f, 600),
        color = Comfort.colors.on2,
        modifier = modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 6.dp),
    )
}

/**
 * `.menu-row`: 50dp, icon in `--on-2` then the label. [selected] is `.on` (accent ink, semibold) and,
 * in a single-choice pop-over, ends in a tick ([check]); [danger] is `.danger`. A null [symbol] keeps
 * no space.
 */
@Composable
fun MenuRow(
    symbol: Symbol?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    danger: Boolean = false,
    check: Boolean = false,
) {
    val colors = Comfort.colors
    val ink = when {
        danger -> colors.danger
        selected -> colors.accentInk
        else -> colors.on
    }
    Row(
        modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (symbol != null) ComfortIcon(symbol, tint = if (danger || selected) ink else colors.on2)
        Text(label, style = onest(16f, 22f, if (selected) 600 else 400), color = ink, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
        if (check && selected) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { ComfortIcon(Symbol.Check, tint = ink, modifier = Modifier.padding(start = 18.dp)) }
        }
    }
}

/** `.toast`: one line of text in a capsule (no action), on `--toast`, with the small shadow. */
@Composable
fun Toast(text: String, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Box(
        modifier
            .widthIn(max = 330.dp)
            .floatingShadow(ComfortShapes.Pill, small = true)
            .clip(ComfortShapes.Pill)
            .background(colors.toast)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(text, style = onest(14.5f, 20f), color = colors.onToast, textAlign = TextAlign.Center)
    }
}

/**
 * A SnackbarHost that shows each message as a [Toast], centred. Place it above the dock (the kit's
 * `bottom:180px`; `.over-tabs` 116 with tabs alone; `.low` 56 over an overlay).
 */
@Composable
fun ToastHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Toast(data.visuals.message) }
    }
}
