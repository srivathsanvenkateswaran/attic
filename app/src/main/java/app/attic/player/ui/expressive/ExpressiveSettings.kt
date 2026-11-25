package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.analysis.AnalysisProgress
import app.attic.player.data.BulkProgress
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.BackupUiState
import app.attic.player.ui.model.SettingsActions
import app.attic.player.ui.model.restoreConfirmBody
import app.attic.player.ui.model.restoreProblemBody
import app.attic.player.ui.model.restoreResultBody
import app.attic.player.ui.model.SettingsState
import java.text.NumberFormat

/**
 * Settings (sheet 08 A, B, F), laid out like the phone's own Settings: a 36sp title that collapses into
 * the bar, a primary label and one group per section, and an icon in a tonal circle at the head of
 * each row. The themes and the style are connected button groups; "Download lyrics for all songs" is
 * a row whose supporting text is the lrclib.net note, and shows a progress line while it runs; the
 * analysis row's line is wavy while songs are being analysed. Backup's two rows open the system file
 * picker; the restore question and its result are dialogs.
 */
@Composable
fun ExpressiveSettings(state: SettingsState, actions: SettingsActions) {
    val listState = rememberLazyListState()
    val titleGone = with(LocalDensity.current) { 44.dp.toPx() }
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleGone }
    }
    val format = NumberFormat.getIntegerInstance()
    Column(Modifier.fillMaxSize()) {
        DetailAppBar(
            title = if (collapsed) stringResource(R.string.settings_title) else "",
            onBack = actions.back,
            surface = if (collapsed) AppBarSurface.Scrolled else AppBarSurface.Plain,
        )
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = LocalBottomPadding.current)) {
            item(key = "title") {
                Text(
                    stringResource(R.string.settings_title),
                    style = Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
            item(key = "appearance") {
                SettingsSection(stringResource(R.string.settings_appearance))
                ListGroup(2) { index ->
                    if (index == 0) ThemeCell(state.themeMode, actions) else StyleCell(state.uiStyle, actions)
                }
            }
            item(key = "playback") {
                SettingsSection(stringResource(R.string.settings_playback))
                ListGroup(if (state.equaliserAvailable) 3 else 2) { index ->
                    when (index) {
                        0 -> VolumeLevellingRow(state.volumeLevelling, actions)
                        1 -> CrossfadeRow(state, actions)
                        else -> EqualiserRow(actions)
                    }
                }
            }
            item(key = "lyrics") {
                SettingsSection(stringResource(R.string.settings_lyrics))
                ListGroup(2) { index ->
                    if (index == 0) {
                        SavedLyricsRow(format.format(state.savedLyricsCount), format.format(state.songCount))
                    } else {
                        DownloadLyricsRow(state.bulkLyrics, actions)
                    }
                }
            }
            item(key = "radio") {
                SettingsSection(stringResource(R.string.settings_song_radio))
                ListGroup(2) { index ->
                    if (index == 0) AutoplayRow(state.autoplay, actions) else AnalysisRow(state.analysis, actions)
                }
            }
            item(key = "backup") {
                SettingsSection(stringResource(R.string.settings_backup))
                ListGroup(3) { index ->
                    when (index) {
                        0 -> BackupRow(
                            Symbol.FileSave,
                            stringResource(R.string.settings_backup_save),
                            stringResource(R.string.settings_backup_save_body),
                            status = if (state.backup.saving) stringResource(R.string.settings_backup_saving) else null,
                            enabled = !state.backup.busy,
                            onClick = actions.backUp,
                        )
                        1 -> BackupRow(
                            Symbol.FileOpen,
                            stringResource(R.string.settings_backup_restore),
                            stringResource(R.string.settings_backup_restore_body),
                            status = if (state.backup.restoring) stringResource(R.string.settings_backup_restoring) else null,
                            enabled = !state.backup.busy,
                            onClick = actions.restore,
                        )
                        else -> SetNote(stringResource(R.string.settings_backup_note))
                    }
                }
            }
            item(key = "about") {
                SettingsSection(stringResource(R.string.settings_about))
                ListGroup(2) { index ->
                    if (index == 0) {
                        SetIconRow(icon = { AtticMarkIcon(size = 22.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer) }) {
                            val count = pluralStringResource(R.plurals.song_count, state.songCount, format.format(state.songCount))
                            SupportingText(stringResource(R.string.settings_about_body, count))
                        }
                    } else {
                        SetIconRow(
                            Symbol.Info,
                            modifier = Modifier.clickable(role = Role.Button, onClick = actions.openLicences),
                            center = true,
                        ) {
                            RowTitle(stringResource(R.string.settings_licences))
                        }
                    }
                }
            }
        }
    }
    BackupDialogs(state.backup, actions)
}

/** "Back up to a file" or "Restore from a file", with a status line while it runs. */
@Composable
private fun BackupRow(symbol: Symbol, title: String, body: String, status: String?, enabled: Boolean, onClick: () -> Unit) {
    SetIconRow(symbol, modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)) {
        RowTitle(title)
        SupportingText(body)
        if (status != null) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    }
}

/** The question before a restore, then its result or why the file couldn't be used. */
@Composable
private fun BackupDialogs(backup: BackupUiState, actions: SettingsActions) {
    val close: @Composable () -> Unit = {
        ExpressiveButton(stringResource(R.string.close), actions.dismissBackupDialog, variant = ButtonVariant.Text)
    }
    backup.confirm?.let { summary ->
        ExpressiveDialog(
            title = stringResource(R.string.restore_confirm_title),
            onDismiss = actions.dismissBackupDialog,
            body = restoreConfirmBody(summary),
            confirmButton = { ExpressiveButton(stringResource(R.string.restore_confirm), actions.confirmRestore, variant = ButtonVariant.Text) },
            dismissButton = { ExpressiveButton(stringResource(R.string.cancel), actions.dismissBackupDialog, variant = ButtonVariant.Text) },
        )
    }
    backup.restored?.let { report ->
        ExpressiveDialog(
            title = stringResource(R.string.restore_done_title),
            onDismiss = actions.dismissBackupDialog,
            body = restoreResultBody(report),
            confirmButton = close,
        )
    }
    backup.problem?.let { problem ->
        ExpressiveDialog(
            title = stringResource(R.string.restore_failed_title),
            onDismiss = actions.dismissBackupDialog,
            body = restoreProblemBody(problem),
            confirmButton = close,
        )
    }
}

/**
 * The three themes as one connected group. The sheet heads the cell with a "Theme" row; that label is
 * new copy, so the group stands alone in its cell (the sheet's own alternative).
 */
@Composable
private fun ThemeCell(mode: ThemeMode, actions: SettingsActions) {
    val labels = listOf(
        stringResource(R.string.settings_theme_system),
        stringResource(R.string.settings_theme_dark),
        stringResource(R.string.settings_theme_light),
    )
    val modes = listOf(ThemeMode.System, ThemeMode.Dark, ThemeMode.Light)
    ConnectedButtonGroup(
        labels,
        modes.indexOf(mode),
        { actions.setThemeMode(modes[it]) },
        Modifier.padding(vertical = 12.dp),
        unselectedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

/** The style picker, Expressive or Comfort, laid out as the sheet lays out the theme cell: a row, then the group. */
@Composable
private fun StyleCell(style: UiStyle, actions: SettingsActions) {
    Column {
        SetIconRow(Symbol.Palette, center = true) {
            Text(stringResource(R.string.settings_style), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }
        val choices = UiStyle.choices
        ConnectedButtonGroup(
            choices.map { stringResource(if (it == UiStyle.Comfort) R.string.settings_style_comfort else R.string.settings_style_expressive) },
            choices.indexOf(style).coerceAtLeast(0),
            { actions.setUiStyle(choices[it]) },
            Modifier.padding(bottom = 12.dp),
            unselectedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

/** "{saved} of {total} songs have lyrics saved…", the count picked out in on-surface. */
@Composable
private fun SavedLyricsRow(saved: String, total: String) {
    val text = stringResource(R.string.settings_lyrics_saved, saved, total)
    val colors = MaterialTheme.colorScheme
    val emphasised = remember(text, saved, total) { emphasiseCount(text, saved, total) }
    SetIconRow(Symbol.Lyrics) {
        Text(
            buildAnnotatedString {
                append(text)
                emphasised?.let { addStyle(SpanStyle(color = colors.onSurface, fontWeight = FontWeight.Medium), it.first, it.last + 1) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** The span from the saved count through the word after the total: "112 of 127 songs". Null if the copy is laid out otherwise. */
private fun emphasiseCount(text: String, saved: String, total: String): IntRange? {
    val start = text.indexOf(saved).takeIf { it >= 0 } ?: return null
    val totalAt = text.indexOf(total, start + saved.length).takeIf { it >= 0 } ?: return null
    var end = totalAt + total.length
    if (end < text.length && text[end] == ' ') {
        end++
        while (end < text.length && text[end].isLetter()) end++
    }
    return start until end
}

@Composable
private fun DownloadLyricsRow(bulk: BulkProgress, actions: SettingsActions) {
    val colors = MaterialTheme.colorScheme
    val format = NumberFormat.getIntegerInstance()
    SetIconRow(
        Symbol.Download,
        modifier = Modifier.clickable(enabled = !bulk.running, role = Role.Button, onClick = actions.downloadAllLyrics),
    ) {
        RowTitle(stringResource(R.string.settings_lyrics_download_all))
        SupportingText(stringResource(R.string.settings_lyrics_privacy))
        if (bulk.running) {
            LinearProgress({ if (bulk.total == 0) 0f else bulk.done.toFloat() / bulk.total }, Modifier.padding(top = 14.dp, bottom = 6.dp))
            Status(stringResource(R.string.settings_lyrics_progress, format.format(bulk.done), format.format(bulk.total), format.format(bulk.found)))
        } else if (bulk.failed > 0) {
            Text(
                if (bulk.gaveUp) {
                    stringResource(R.string.settings_lyrics_gave_up)
                } else {
                    pluralStringResource(R.plurals.settings_lyrics_failed, bulk.failed, format.format(bulk.failed))
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AutoplayRow(autoplay: Boolean, actions: SettingsActions) {
    SetIconRow(
        Symbol.Autoplay,
        modifier = Modifier.toggleable(value = autoplay, role = Role.Switch, onValueChange = actions.setAutoplay),
        trailing = { ExpressiveSwitch(autoplay, onCheckedChange = null, modifier = Modifier.padding(top = 4.dp)) },
    ) {
        RowTitle(stringResource(R.string.settings_autoplay))
        SupportingText(stringResource(R.string.settings_autoplay_body))
    }
}

@Composable
private fun VolumeLevellingRow(enabled: Boolean, actions: SettingsActions) {
    SetIconRow(
        Symbol.VolumeUp,
        modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = actions.setVolumeLevelling),
        trailing = { ExpressiveSwitch(enabled, onCheckedChange = null, modifier = Modifier.padding(top = 4.dp)) },
    ) {
        RowTitle(stringResource(R.string.settings_volume_levelling))
        SupportingText(stringResource(R.string.settings_volume_levelling_body))
    }
}

/** "Crossfade" with its length as the supporting line; tapping opens the kit's menu of lengths, the current one ticked. */
@Composable
private fun CrossfadeRow(state: SettingsState, actions: SettingsActions) {
    var picking by remember { mutableStateOf(false) }
    Box {
        SetIconRow(Symbol.TransitionFade, modifier = Modifier.clickable(role = Role.DropdownList) { picking = true }) {
            RowTitle(stringResource(R.string.settings_crossfade))
            SupportingText(crossfadeLabel(state.crossfadeSeconds))
            Status(stringResource(R.string.settings_crossfade_body))
        }
        ExpressiveMenu(expanded = picking, onDismiss = { picking = false }, modifier = Modifier) {
            for (seconds in state.crossfadeChoices) {
                ExpressiveMenuItem(
                    symbol = if (seconds == state.crossfadeSeconds) Symbol.Check else null,
                    label = crossfadeLabel(seconds),
                    onClick = {
                        picking = false
                        actions.setCrossfadeSeconds(seconds)
                    },
                )
            }
        }
    }
}

@Composable
private fun crossfadeLabel(seconds: Int): String =
    if (seconds <= 0) stringResource(R.string.settings_crossfade_off) else pluralStringResource(R.plurals.settings_crossfade_seconds, seconds, seconds)

/** Shown only when something on the phone opens an equaliser; it opens there, on Attic's audio session. */
@Composable
private fun EqualiserRow(actions: SettingsActions) {
    SetIconRow(
        Symbol.Equalizer,
        modifier = Modifier.clickable(role = Role.Button, onClick = actions.openEqualiser),
        trailing = { SymbolIcon(Symbol.OpenInNew, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)) },
    ) {
        RowTitle(stringResource(R.string.settings_equaliser))
        SupportingText(stringResource(R.string.settings_equaliser_body))
    }
}

@Composable
private fun AnalysisRow(analysis: AnalysisProgress, actions: SettingsActions) {
    val format = NumberFormat.getIntegerInstance()
    val done = format.format(analysis.done)
    val total = format.format(analysis.total)
    SetIconRow(Symbol.GraphicEq) {
        SupportingText(stringResource(R.string.settings_radio_privacy))
        val progress = Modifier.padding(top = 14.dp, bottom = 6.dp)
        if (analysis.running) {
            WavyLinearProgress({ analysis.fraction }, progress)
        } else {
            LinearProgress({ analysis.fraction }, progress)
        }
        Status(
            when {
                analysis.running -> stringResource(R.string.settings_analysis_running, done, total)
                analysis.done >= analysis.total -> stringResource(R.string.settings_analysis_done, total)
                else -> stringResource(R.string.settings_analysis_partial, done, total)
            },
        )
        if (analysis.canStart) {
            ExpressiveButton(
                stringResource(R.string.settings_analyse_now),
                actions.startAnalysis,
                Modifier.padding(top = 14.dp),
                variant = ButtonVariant.Tonal,
            )
        }
    }
}

/**
 * A settings row as sheet 08 draws it: a 40dp tonal circle holding the icon, then the row's text, and
 * an optional trailing control. Rows with more than a line start at the top; [center] centres a
 * one-line row.
 */
@Composable
private fun SetIconRow(
    symbol: Symbol,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = SetIconRow(
    icon = { SymbolIcon(symbol, size = 20.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer) },
    modifier = modifier,
    center = center,
    trailing = trailing,
    content = content,
)

@Composable
private fun SetIconRow(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (center) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f), content = content)
        if (trailing != null) trailing()
    }
}

@Composable
private fun RowTitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
}

@Composable
private fun SupportingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Status(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
