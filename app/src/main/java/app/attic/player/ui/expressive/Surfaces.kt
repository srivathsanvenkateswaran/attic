package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A bottom sheet (`.sheet`): Material's modal sheet on surface container low, 28dp top corners, the
 * kit's 32×4 handle, opened fully. Rows inside are [SheetRow]s; a group inside takes surface
 * container high cells. [bottomPadding] is the kit's 32dp under the last row; a sheet whose list
 * scrolls to its foot (Add songs) passes 0 and pads the list instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = state,
        shape = RoundedCornerShape(topStart = ExpressiveShapes.ExtraLarge, topEnd = ExpressiveShapes.ExtraLarge),
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        dragHandle = { SheetHandle() },
    ) {
        CompositionLocalProvider(LocalGroupCellColor provides colors.surfaceContainerHigh) {
            Column(Modifier.padding(bottom = bottomPadding), content = content)
        }
    }
}

/** The sheet's drag handle (`.handle`): 32×4 in outline, 16dp above and 12 below. */
@Composable
fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 16.dp, bottom = 12.dp)
            .size(32.dp, 4.dp)
            .clip(ExpressiveShapes.Pill)
            .background(MaterialTheme.colorScheme.outline),
    )
}

/** A sheet's song header (`.sheet-head`): 56dp cover, title over artist. */
@Composable
fun SheetHeader(title: String, subtitle: String, art: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        art(Modifier.size(56.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A sheet's title when it is not about one song (`.sheet-title`): "Add to playlist". */
@Composable
fun SheetTitle(text: String) {
    Text(text, style = Expressive.type.headlineSmallEmphasized, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp))
}

/** The rule under a sheet header (`.divider`). */
@Composable
fun SheetDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * A sheet row (`.sheet-row`): a 24dp symbol and a label, 56dp tall. [selected] is the current choice
 * in a picker (`.sheet-row.on`): primary and heavier, usually with [Symbol.Check].
 */
@Composable
fun SheetRow(symbol: Symbol, label: String, onClick: () -> Unit, selected: Boolean = false, filled: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(symbol, tint = tint, filled = filled)
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) colors.primary else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A dialog (`.dialog`): Material's alert dialog on surface container high with 28dp corners and an
 * emphasised headline. Put a [DialogTextField] in [content] for the playlist name.
 */
@Composable
fun ExpressiveDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    body: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = { Text(title, style = Expressive.type.headlineSmallEmphasized) },
        text = if (body != null || content != null) {
            {
                Column {
                    if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    if (content != null) {
                        if (body != null) Spacer(Modifier.height(16.dp))
                        content()
                    }
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(ExpressiveShapes.ExtraLarge),
        containerColor = colors.surfaceContainerHigh,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
    )
}

/** The outlined text field in a dialog (`.field`): "Playlist name". */
@Composable
fun DialogTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ExpressiveShapes.ExtraSmall),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
    )
}

/**
 * A menu (`.menu`): Material's dropdown on surface container with 16dp corners and the kit's shadow,
 * 8dp of padding round its items and at least 196dp wide.
 */
@Composable
fun ExpressiveMenu(expanded: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // Applies inside the surface. Material already pads 8dp above and below; this adds the sides.
        modifier = modifier.padding(horizontal = 8.dp).widthIn(min = 180.dp),
        shape = RoundedCornerShape(ExpressiveShapes.Large),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        content = content,
    )
}

/**
 * A menu item (`.menu-item`): a symbol and a label. [danger] turns both to the error colour. A null
 * [symbol] keeps its space, so a menu of choices can tick only the current one (sheet 03 H).
 */
@Composable
fun ExpressiveMenuItem(symbol: Symbol?, label: String, onClick: () -> Unit, danger: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        onClick = onClick,
        leadingIcon = { if (symbol != null) SymbolIcon(symbol) else Spacer(Modifier.size(24.dp)) },
        colors = MenuDefaults.itemColors(
            textColor = if (danger) colors.error else colors.onSurface,
            leadingIconColor = if (danger) colors.error else colors.onSurfaceVariant,
        ),
    )
}

/** The snackbar (`.snack`): inverse surface, 8dp corners, text only. Use it in a SnackbarHost. */
@Composable
fun ExpressiveSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    Snackbar(
        snackbarData = data,
        modifier = modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(ExpressiveShapes.Small),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
}

/** A snackbar drawn without a host, for previews and the gallery. */
@Composable
fun StaticSnackbar(text: String, modifier: Modifier = Modifier) {
    Snackbar(
        modifier = modifier,
        shape = RoundedCornerShape(ExpressiveShapes.Small),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) { Text(text) }
}

/**
 * An empty state (`.empty`): a 112dp rounded square on the secondary container holding a symbol (or
 * [art], e.g. the Attic mark), the title in the emphasised headline, the body, and an optional action.
 * [clover] draws the picture as the 128dp clover instead (`.empty-art.clover`), for "nothing here yet":
 * the empty library. A null [body] leaves the title alone ("Nothing up next").
 */
@Composable
fun EmptyState(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    symbol: Symbol? = null,
    art: (@Composable () -> Unit)? = null,
    clover: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (symbol != null || art != null) {
            Box(
                Modifier
                    .size(if (clover) 128.dp else 112.dp)
                    .clip(if (clover) CloverShape else RoundedCornerShape(36.dp))
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.onSecondaryContainer) {
                    if (art != null) art() else if (symbol != null) SymbolIcon(symbol, size = 48.dp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Text(title, style = Expressive.type.headlineSmallEmphasized, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** A settings section heading (`.set-sec`): primary, 24dp above, 32 in from the edge. */
@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
    )
}

/**
 * A settings row (`.set-row`): an optional leading symbol, a title over an optional summary, and a
 * trailing control. 64dp minimum. Use [SetSwitchRow] and [SetRadioRow] for the usual controls; put it
 * in a [ListGroup] cell.
 */
@Composable
fun SetRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: Symbol? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (leading != null) SymbolIcon(leading, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}

/** A settings row with a switch; tapping the row toggles it. */
@Composable
fun SetSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, summary: String? = null, leading: Symbol? = null) {
    SetRow(
        title,
        Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        summary,
        leading,
    ) { ExpressiveSwitch(checked, onCheckedChange = null) }
}

/** A settings row with a radio button, one of a set; tapping the row selects it. */
@Composable
fun SetRadioRow(title: String, selected: Boolean, onSelect: () -> Unit, summary: String? = null) {
    SetRow(
        title,
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        summary,
    ) { ExpressiveRadio(selected, onClick = null) }
}

/** A paragraph in a settings group (`.set-note`), with room for progress, a status line and a text button. */
@Composable
fun SetNote(text: String, modifier: Modifier = Modifier, extra: @Composable ColumnScope.() -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        extra()
    }
}

/** The small status line under progress in a [SetNote] (`.set-note small`). */
@Composable
fun SetNoteStatus(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

/** The switch (`.switch`): Material's, with a tick in the thumb when on and a cross when off. */
@Composable
fun ExpressiveSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = { SymbolIcon(if (checked) Symbol.Check else Symbol.Close, size = 16.dp) },
    )
}

/** The radio button (`.radio`): Material's. */
@Composable
fun ExpressiveRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    RadioButton(selected = selected, onClick = onClick, modifier = modifier, enabled = enabled)
}

/**
 * A card under the player (`.np-card`): surface container, 28dp corners, 20dp padding, an optional
 * small heading with a symbol ("Lyrics", "Credits").
 */
@Composable
fun InfoCard(modifier: Modifier = Modifier, title: String? = null, symbol: Symbol? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .padding(20.dp),
    ) {
        if (title != null) {
            Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (symbol != null) {
                    SymbolIcon(symbol, size = 20.dp, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.onSurfaceVariant)
            }
        }
        content()
    }
}

/** A label over its value (`.kv`), as in Credits: "Performed by" / "Khan Saab, Shashwat Sachdev". */
@Composable
fun KeyValue(label: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Where a lyric line stands against playback. */
enum class LyricState { Sung, Current, Upcoming, Plain }

/**
 * A lyric line (`.lyrics p`): 22/28 emphasised in the card, 28/36 full screen ([large]). Sung lines are
 * on-surface-variant, the current one on-surface, those to come outline; plain (unsynced) text is on-surface.
 */
@Composable
fun LyricLine(text: String, state: LyricState, modifier: Modifier = Modifier, large: Boolean = false, maxLines: Int = Int.MAX_VALUE) {
    val colors = MaterialTheme.colorScheme
    val color = when (state) {
        LyricState.Sung -> colors.onSurfaceVariant
        LyricState.Current, LyricState.Plain -> colors.onSurface
        LyricState.Upcoming -> colors.outline
    }
    val base = Expressive.type.titleLargeEmphasized
    val style = if (large) base.copy(fontSize = 28.sp, lineHeight = 36.sp) else base
    Text(
        text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = if (large) 16.dp else 10.dp),
    )
}
