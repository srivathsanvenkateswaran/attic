package app.attic.player.ui.comfort

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.crossfadeLengthBody
import app.attic.player.ui.crossfadeLengthLabel
import app.attic.player.ui.transitionBody
import app.attic.player.ui.transitionLabel
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.BackupUiState
import app.attic.player.ui.model.SettingsActions
import app.attic.player.ui.model.restoreConfirmBody
import app.attic.player.ui.model.restoreProblemBody
import app.attic.player.ui.model.restoreResultBody
import app.attic.player.ui.model.SettingsState
import app.attic.player.ui.model.equaliserOwnSummary
import app.attic.player.ui.model.equaliserRowValue
import java.text.NumberFormat

/**
 * Settings (oneui/08-settings-states.html A, B, G, H, I): "Settings" as the large title with the back
 * arrow in the row that closes the viewing area, then one labelled card per section in today's order.
 * Appearance's themes are radios that lead their rows, with the style (Expressive or Comfort) as a
 * second card of radios under it. Lyrics and Song radio are paragraphs, a 10dp progress capsule, a
 * status line and a text button inside their cards; Backup is two rows and a note, with the restore
 * question and its result as dialogs; About sits beside the mark.
 */
@Composable
fun ComfortSettings(state: SettingsState, actions: SettingsActions) {
    val listState = rememberLazyListState()
    val format = NumberFormat.getIntegerInstance()
    val title = stringResource(R.string.settings_title)
    RootPage(title = title, listState = listState, onBack = actions.back) {
        item(key = "hero") { Hero(title) }
        item(key = "tools") {
            LeadTools { ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back) }
        }

        item(key = "appearance") {
            Label(stringResource(R.string.settings_appearance))
            Group {
                listOf(
                    ThemeMode.System to R.string.settings_theme_system,
                    ThemeMode.Dark to R.string.settings_theme_dark,
                    ThemeMode.Light to R.string.settings_theme_light,
                ).forEach { (mode, label) ->
                    item { SetRadioRow(stringResource(label), state.themeMode == mode, { actions.setThemeMode(mode) }) }
                }
            }
        }
        item(key = "style") {
            Label(stringResource(R.string.settings_style))
            Group {
                UiStyle.choices.forEach { style ->
                    item {
                        SetRadioRow(
                            stringResource(if (style == UiStyle.Comfort) R.string.settings_style_comfort else R.string.settings_style_expressive),
                            state.uiStyle == style,
                            { actions.setUiStyle(style) },
                        )
                    }
                }
            }
        }

        item(key = "playback") {
            Label(stringResource(R.string.settings_playback))
            Group {
                item {
                    SetSwitchRow(
                        stringResource(R.string.settings_volume_levelling),
                        state.volumeLevelling,
                        actions.setVolumeLevelling,
                        summary = stringResource(R.string.settings_volume_levelling_body),
                    )
                }
                item {
                    PickerRow(
                        title = stringResource(R.string.settings_transitions),
                        summary = transitionBody(state.transitions),
                        labels = state.transitionChoices.map { transitionLabel(it) },
                        selected = state.transitionChoices.indexOf(state.transitions),
                        onPick = { actions.setTransitions(state.transitionChoices[it]) },
                    )
                }
                if (state.showsCrossfadeLength) {
                    item {
                        PickerRow(
                            title = stringResource(R.string.settings_crossfade_length),
                            summary = crossfadeLengthBody(state.transitions),
                            labels = state.crossfadeChoices.map { crossfadeLengthLabel(it) },
                            selected = state.crossfadeChoices.indexOf(state.crossfadeSeconds),
                            onPick = { actions.setCrossfadeSeconds(state.crossfadeChoices[it]) },
                        )
                    }
                }
                item { EqualiserRow(state, actions) }
            }
        }

        item(key = "lyrics") {
            val bulk = state.bulkLyrics
            Label(stringResource(R.string.settings_lyrics))
            Group {
                item {
                    SetBlock {
                        val saved = format.format(state.savedLyricsCount)
                        val total = format.format(state.songCount)
                        BlockText(emphasiseCount(stringResource(R.string.settings_lyrics_saved, saved, total), saved, total))
                        BlockSmall(stringResource(R.string.settings_lyrics_privacy), Modifier.padding(top = 10.dp))
                        if (bulk.running) {
                            ComfortProgress(
                                progress = { if (bulk.total == 0) 0f else bulk.done.toFloat() / bulk.total },
                                modifier = SetBlockProgressPadding,
                            )
                            BlockSmall(
                                stringResource(
                                    R.string.settings_lyrics_progress,
                                    format.format(bulk.done),
                                    format.format(bulk.total),
                                    format.format(bulk.found),
                                ),
                            )
                        } else {
                            if (bulk.failed > 0) {
                                Warning(
                                    if (bulk.gaveUp) {
                                        stringResource(R.string.settings_lyrics_gave_up)
                                    } else {
                                        pluralStringResource(R.plurals.settings_lyrics_failed, bulk.failed, format.format(bulk.failed))
                                    },
                                )
                            }
                            BlockButton(stringResource(R.string.settings_lyrics_download_all), actions.downloadAllLyrics)
                        }
                    }
                }
            }
        }

        item(key = "radio") {
            val analysis = state.analysis
            val done = format.format(analysis.done)
            val total = format.format(analysis.total)
            Label(stringResource(R.string.settings_moods))
            Group {
                item {
                    SetRow(
                        stringResource(R.string.settings_interests),
                        summary = state.interestsSummary,
                        onClick = actions.openInterests,
                        trailing = { ComfortIcon(Symbol.ChevronRight, tint = Comfort.colors.on3) },
                    )
                }
            }
            Label(stringResource(R.string.settings_song_radio))
            Group {
                item {
                    SetSwitchRow(
                        stringResource(R.string.settings_autoplay),
                        state.autoplay,
                        actions.setAutoplay,
                        summary = stringResource(R.string.settings_autoplay_body),
                    )
                }
                item {
                    SetBlock(afterRow = true) {
                        BlockText(AnnotatedString(stringResource(R.string.settings_radio_privacy)))
                        ComfortProgress(progress = { analysis.fraction }, modifier = SetBlockProgressPadding)
                        BlockSmall(
                            when {
                                analysis.running -> stringResource(R.string.settings_analysis_running, done, total)
                                analysis.done >= analysis.total -> stringResource(R.string.settings_analysis_done, total)
                                else -> stringResource(R.string.settings_analysis_partial, done, total)
                            },
                        )
                        if (analysis.canStart) BlockButton(stringResource(R.string.settings_analyse_now), actions.startAnalysis)
                    }
                }
            }
        }

        item(key = "backup") {
            val backup = state.backup
            Label(stringResource(R.string.settings_backup))
            Group {
                item {
                    SetRow(
                        stringResource(R.string.settings_backup_save),
                        summary = stringResource(if (backup.saving) R.string.settings_backup_saving else R.string.settings_backup_save_body),
                        onClick = actions.backUp.takeUnless { backup.busy },
                    )
                }
                item {
                    SetRow(
                        stringResource(R.string.settings_backup_restore),
                        summary = stringResource(if (backup.restoring) R.string.settings_backup_restoring else R.string.settings_backup_restore_body),
                        onClick = actions.restore.takeUnless { backup.busy },
                    )
                }
                item {
                    SetBlock(afterRow = true) { BlockSmall(stringResource(R.string.settings_backup_note), Modifier) }
                }
            }
        }

        item(key = "about") {
            Label(stringResource(R.string.settings_about))
            Group {
                item {
                    // `.set-block.about`: the mark in its box beside the sentence, the song count in bold.
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MarkBox(size = 52.dp)
                        val count = pluralStringResource(R.plurals.song_count, state.songCount, format.format(state.songCount))
                        val body = stringResource(R.string.settings_about_body, count)
                        val start = body.indexOf(count)
                        BlockText(
                            buildAnnotatedString {
                                append(body)
                                if (start >= 0) addStyle(Bold, start, body.length)
                            },
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SetRow(
                        stringResource(R.string.settings_licences),
                        onClick = actions.openLicences,
                        trailing = { ComfortIcon(Symbol.ChevronRight, tint = Comfort.colors.on3) },
                    )
                }
            }
        }
    }
    BackupDialogs(state.backup, actions)
}

private val Bold = SpanStyle(fontWeight = FontWeight.SemiBold)

/**
 * A row whose value is one of a few choices, shown in accent ink, opening the kit's pop-over over the
 * row toward the value, the way the track menu's sleep timer does: [title] as a caption, then the
 * choices with the current one ticked. "Transitions" and "Crossfade length" use it.
 *
 * The menu hangs from a zero-height anchor on the row's bottom edge. The rows sit low in Settings, so
 * the menu usually has no room below and opens upward, its bottom on the row's bottom, covering the
 * row. Material applies a y offset the same way in both directions, so an offset that covers the row
 * opening downward leaves a row-high gap opening upward; the anchor needs none.
 */
@Composable
private fun PickerRow(title: String, summary: String?, labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    BoxWithConstraints {
        val menuX = maxWidth - 214.dp
        SetRow(
            title,
            summary = summary,
            value = labels.getOrNull(selected),
            onClick = { picking = true },
        )
        Box(Modifier.align(Alignment.BottomStart)) {
            ComfortMenu(
                expanded = picking,
                onDismiss = { picking = false },
                offset = DpOffset(menuX, 0.dp),
            ) {
                MenuCaption(title)
                labels.forEachIndexed { index, label ->
                    MenuRow(
                        null,
                        label,
                        {
                            picking = false
                            onPick(index)
                        },
                        selected = index == selected,
                        check = true,
                    )
                }
            }
        }
    }
}

/**
 * "Equaliser", which opens its page. Its value is its state, as "Transitions" shows its choice: "Off" or
 * the default's preset. The line under the title counts what has a setting of its own, or describes
 * the feature while nothing has.
 */
@Composable
private fun EqualiserRow(state: SettingsState, actions: SettingsActions) {
    SetRow(
        stringResource(R.string.equaliser),
        summary = equaliserOwnSummary(state.equaliser) ?: stringResource(R.string.settings_equaliser_summary),
        value = equaliserRowValue(state.equaliser),
        onClick = actions.openEqualiserPage,
        trailing = {
            // `.set-row .val + .ms{margin-left:-6px}`: the chevron keeps its place and the value moves 6dp towards it.
            ComfortIcon(
                Symbol.ChevronRight,
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val pull = 6.dp.roundToPx()
                    layout(placeable.width - pull, placeable.height) { placeable.placeRelative(-pull, 0) }
                },
                tint = Comfort.colors.on3,
            )
        },
    )
}

/** The question before a restore, then its result or why the file couldn't be used. */
@Composable
private fun BackupDialogs(backup: BackupUiState, actions: SettingsActions) {
    backup.confirm?.let { summary ->
        ComfortDialog(
            title = stringResource(R.string.restore_confirm_title),
            onDismiss = actions.dismissBackupDialog,
            confirmText = stringResource(R.string.restore_confirm),
            onConfirm = actions.confirmRestore,
            dismissText = stringResource(R.string.cancel),
            body = restoreConfirmBody(summary),
        )
    }
    backup.restored?.let { report ->
        ComfortDialog(
            title = stringResource(R.string.restore_done_title),
            onDismiss = actions.dismissBackupDialog,
            confirmText = stringResource(R.string.close),
            onConfirm = actions.dismissBackupDialog,
            dismissText = null,
            body = restoreResultBody(report),
        )
    }
    backup.problem?.let { problem ->
        ComfortDialog(
            title = stringResource(R.string.restore_failed_title),
            onDismiss = actions.dismissBackupDialog,
            confirmText = stringResource(R.string.close),
            onConfirm = actions.dismissBackupDialog,
            dismissText = null,
            body = restoreProblemBody(problem),
        )
    }
}

/** "{saved} of {total} songs have lyrics saved…", the span from the saved count through the word after the total in bold. */
@Composable
private fun emphasiseCount(text: String, saved: String, total: String): AnnotatedString {
    val on = Comfort.colors.on
    val start = text.indexOf(saved)
    val totalAt = if (start >= 0) text.indexOf(total, start + saved.length) else -1
    return buildAnnotatedString {
        append(text)
        if (totalAt >= 0) {
            var end = totalAt + total.length
            if (end < text.length && text[end] == ' ') {
                end++
                while (end < text.length && text[end].isLetter()) end++
            }
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = on), start, end)
        }
    }
}

/** A paragraph in a settings card: 14/20 in `--on-2`, its bold words in `--on`. */
@Composable
private fun BlockText(text: AnnotatedString, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val withInk = buildAnnotatedString {
        append(text)
        text.spanStyles.forEach { addStyle(it.item.copy(color = colors.on), it.start, it.end) }
    }
    Text(withInk, style = onest(14f, 20f), color = colors.on2, modifier = modifier)
}

/** Status and small print: the sheet sets them in the second ink, since they are strings to read. */
@Composable
private fun BlockSmall(text: String, modifier: Modifier = Modifier.padding(top = 6.dp)) {
    Text(text, style = onest(12.5f, 17f), color = Comfort.colors.on2, modifier = modifier)
}

/** A failed lyrics run (`.warn`): a red glyph beside a sentence in ink. */
@Composable
private fun Warning(text: String) {
    val colors = Comfort.colors
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ComfortIcon(Symbol.Error, size = 20.dp, tint = colors.danger, modifier = Modifier.offset(y = (-1).dp))
        Text(text, style = onest(13.5f, 19f), color = colors.on)
    }
}

@Composable
private fun BlockButton(text: String, onClick: () -> Unit) {
    ComfortButton(text, onClick, Modifier.padding(top = 12.dp).offset(x = (-12).dp), style = ButtonStyle.Text)
}
