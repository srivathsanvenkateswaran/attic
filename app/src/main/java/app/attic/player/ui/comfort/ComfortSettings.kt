package app.attic.player.ui.comfort

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.ThemeMode
import app.attic.player.data.UiStyle
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.SettingsActions
import app.attic.player.ui.model.SettingsState
import java.text.NumberFormat

/**
 * Settings (oneui/08-settings-states.html A, B, G, H, I): "Settings" as the large title with the back
 * arrow in the row that closes the viewing area, then one labelled card per section in today's order.
 * Appearance's themes are radios that lead their rows, with the style (Expressive or Comfort) as a
 * second card of radios under it. Lyrics and Song radio are paragraphs, a 10dp progress capsule, a
 * status line and a text button inside their cards; About sits beside the mark.
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
            }
        }
    }
}

private val Bold = SpanStyle(fontWeight = FontWeight.SemiBold)

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
