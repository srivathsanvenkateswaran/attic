package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.data.LanguageLabels
import app.attic.player.playback.SleepChoice
import app.attic.player.ui.sleepChoiceLabel
import app.attic.player.ui.sleepTimerSummary
import app.attic.player.ui.model.AddSongsActions
import app.attic.player.ui.model.AddSongsState
import app.attic.player.ui.model.DeletePlaylistActions
import app.attic.player.ui.model.DeletePlaylistState
import app.attic.player.ui.model.LanguagePickerActions
import app.attic.player.ui.model.LanguagePickerState
import app.attic.player.ui.model.PlaylistNameActions
import app.attic.player.ui.model.PlaylistNameState
import app.attic.player.ui.model.PlaylistPickerActions
import app.attic.player.ui.model.PlaylistPickerState
import app.attic.player.ui.model.TrackMenuActions
import app.attic.player.ui.model.TrackMenuState
import java.text.NumberFormat

/*
 * The sheets and dialogs in the Expressive style (pixel/07-sheets.html, frames A to E). Each is
 * Material's own modal sheet or alert dialog through the kit; the work is what goes inside.
 */

/**
 * The track menu (frame A, G, H): the song, the three ways to play it as a button group, then two
 * grouped lists: filing it (favourites, a playlist, its language, out of this playlist) and going
 * somewhere else (its album and artist, each shown by its cover and name, and the share sheet).
 * Opened from Now Playing, "Sleep timer" sits in a group of its own under the song.
 */
@Composable
fun ExpressiveTrackMenu(state: TrackMenuState, actions: TrackMenuActions) {
    val track = state.track
    ExpressiveSheet(onDismiss = actions.dismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(track.title, track.artist) { m -> Cover(Art.of(track.mediaId), m) }
            state.sleepTimer?.let { timer ->
                ListGroup(1) {
                    MenuCell(
                        stringResource(R.string.sleep_timer),
                        actions.pickSleepTimer,
                        supporting = sleepTimerSummary(timer),
                        lead = { CellSymbol(Symbol.Bedtime, filled = timer.choice != SleepChoice.Off, on = timer.choice != SleepChoice.Off) },
                    )
                }
                Spacer(Modifier.height(ExpressiveShapes.GroupSpacing))
            }
            // A song opened from another app isn't in the library, so nothing below applies to it.
            if (state.external) return@Column
            ButtonGroup(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp), fill = true) {
                QuickAction(Symbol.Sensors, stringResource(R.string.menu_start_radio), actions.startRadio)
                QuickAction(Symbol.PlaylistPlay, stringResource(R.string.menu_play_next), actions.playNext)
                QuickAction(Symbol.QueueMusic, stringResource(R.string.menu_add_to_queue), actions.addToQueue)
            }
            val filing = buildList<@Composable () -> Unit> {
                add {
                    MenuCell(
                        label = stringResource(if (state.liked) R.string.favourite_remove else R.string.favourite_add),
                        onClick = actions.toggleLike,
                        lead = { CellSymbol(Symbol.Favorite, filled = state.liked, on = state.liked) },
                    )
                }
                add { MenuCell(stringResource(R.string.menu_add_to_playlist), actions.pickPlaylist, lead = { CellSymbol(Symbol.PlaylistAdd) }) }
                add {
                    MenuCell(
                        stringResource(R.string.menu_language, languageName(state.language)),
                        actions.pickLanguage,
                        lead = { CellSymbol(Symbol.Language) },
                    )
                }
                if (state.inPlaylist) {
                    add { MenuCell(stringResource(R.string.menu_remove_from_playlist), actions.removeFromPlaylist, lead = { CellSymbol(Symbol.DoNotDisturbOn) }) }
                }
            }
            ListGroup(filing.size) { filing[it]() }

            val going = buildList<@Composable () -> Unit> {
                if (state.canGoToAlbum) {
                    add {
                        MenuCell(
                            stringResource(R.string.menu_go_to_album),
                            actions.goToAlbum,
                            supporting = state.albumName,
                            lead = { Cover(Art.of(track.mediaId), size = 40.dp) },
                        )
                    }
                }
                if (state.canGoToArtist) {
                    add {
                        MenuCell(
                            stringResource(R.string.menu_go_to_artist),
                            actions.goToArtist,
                            supporting = state.artistName,
                            lead = { ArtistCover(Art.of(state.artistCover ?: track.mediaId), size = 40.dp) },
                        )
                    }
                }
                if (state.canShare) {
                    add { MenuCell(stringResource(R.string.menu_share), actions.share, lead = { CellSymbol(Symbol.Share) }) }
                }
            }
            if (going.isNotEmpty()) {
                Spacer(Modifier.height(ExpressiveShapes.GroupSpacing))
                ListGroup(going.size) { going[it]() }
            }
        }
    }
}

/** One of the menu's three play actions (`.qa a`): an 80dp tonal block, the symbol over the label. */
@Composable
private fun ButtonGroupScope.QuickAction(symbol: Symbol, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .groupItem(interaction)
            .height(80.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.secondaryContainer)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onSecondaryContainer) {
            SymbolIcon(symbol)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A row's leading symbol in a sheet group: a 40dp slot, so every label starts on the 72dp keyline. */
@Composable
private fun CellSymbol(symbol: Symbol, filled: Boolean = false, on: Boolean = false) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        SymbolIcon(symbol, filled = filled, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A row in a sheet's group (`.sheet .group .sheet-row`): a 40dp lead, the label, and an optional
 * line under it (`.two`, 64dp) naming where the row leads. [selected] is a picker's chosen row: it
 * takes the tonal fill. [trailing] holds a radio button.
 */
@Composable
private fun MenuCell(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    selected: Boolean = false,
    lead: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    role: Role = Role.Button,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.background(colors.secondaryContainer) else Modifier)
            .then(
                if (role == Role.RadioButton) {
                    Modifier.selectable(selected = selected, role = role, onClick = onClick)
                } else {
                    Modifier.clickable(role = role, onClick = onClick)
                },
            )
            .heightIn(min = if (supporting != null) 64.dp else 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (lead != null) lead()
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing()
    }
}

/** A sheet's title with the line under it naming what the sheet acts on (`.sheet-title` + `.sheet-sub`). */
@Composable
private fun TitleBlock(title: String, subtitle: String?) {
    SheetTitle(title)
    if (subtitle != null) SheetSubtitle(subtitle)
}

/**
 * The line under a sheet's title (`.sheet-sub`), pulled 10dp up to it. One line for a name; the
 * equaliser's "Off" sentence passes more [maxLines] so it can wrap at a large font size.
 */
@Composable
internal fun SheetSubtitle(text: String, maxLines: Int = 1) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.pullUp(10.dp).padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    )
}

/** A negative top margin (`margin-top:-10px`): the line sits closer under the title. */
internal fun Modifier.pullUp(by: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val shift = by.roundToPx()
        layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
    }

/** A song's language by its code, from the shared copy. "Unknown" when the app cannot tell. */
@Composable
fun languageName(code: String?): String = stringResource(
    when (code) {
        "ta" -> R.string.language_tamil
        "hi" -> R.string.language_hindi
        "te" -> R.string.language_telugu
        "ml" -> R.string.language_malayalam
        "kn" -> R.string.language_kannada
        "en" -> R.string.language_english
        LanguageLabels.NO_VOCALS -> R.string.language_no_vocals
        else -> R.string.language_unknown
    },
)

/**
 * The language picker (frames C and I): the seven languages as one single-choice group. The chosen
 * row takes the tonal fill and its radio is on; an unset language ("Unknown") has nothing chosen.
 */
@Composable
fun ExpressiveLanguagePicker(state: LanguagePickerState, actions: LanguagePickerActions) {
    ExpressiveSheet(onDismiss = actions.dismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TitleBlock(stringResource(R.string.language_picker_title), state.songTitle)
            ListGroup(state.choices.size) { index ->
                val code = state.choices[index].first
                val selected = code == state.current
                MenuCell(
                    label = languageName(code),
                    onClick = { actions.choose(code) },
                    selected = selected,
                    role = Role.RadioButton,
                    trailing = { ExpressiveRadio(selected, onClick = null) },
                )
            }
        }
    }
}

/**
 * The sleep timer's choices, opened from "Sleep timer" in the track menu over Now Playing. Built like
 * the language picker: a title with what the timer is doing under it, then one single-choice group.
 * A choice sets the timer and closes the sheet.
 */
@Composable
fun ExpressiveSleepTimerPicker(state: TrackMenuState, actions: TrackMenuActions) {
    val timer = state.sleepTimer ?: return
    ExpressiveSheet(onDismiss = actions.dismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TitleBlock(stringResource(R.string.sleep_timer), sleepTimerSummary(timer))
            val choices = SleepChoice.entries
            ListGroup(choices.size) { index ->
                val choice = choices[index]
                val selected = choice == timer.choice
                MenuCell(
                    label = sleepChoiceLabel(choice),
                    onClick = { actions.chooseSleepTimer(choice) },
                    selected = selected,
                    role = Role.RadioButton,
                    trailing = { ExpressiveRadio(selected, onClick = null) },
                )
            }
        }
    }
}

/**
 * The playlist picker (frame B): "New playlist" as a cell of its own, then each playlist with its
 * mosaic, its name and its count, and a check when it already holds what is being added.
 */
@Composable
fun ExpressivePlaylistPicker(state: PlaylistPickerState, actions: PlaylistPickerActions) {
    val subtitle = state.songTitle ?: if (state.songCount > 1) songCountText(state.songCount) else null
    ExpressiveSheet(onDismiss = actions.dismiss) {
        TitleBlock(stringResource(R.string.playlist_picker_title), subtitle)
        LazyColumn {
            item(key = "new") {
                Box(Modifier.fillMaxWidth().groupCell(0, 1, groupCellColor())) {
                    PickerRow(stringResource(R.string.playlist_picker_new), null, actions.startCreating, lead = {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(ExpressiveShapes.Medium)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { SymbolIcon(Symbol.Add, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                    })
                }
            }
            if (state.playlists.isNotEmpty()) item(key = "gap") { Spacer(Modifier.height(ExpressiveShapes.GroupSpacing)) }
            groupItems(state.playlists, key = { it.id }) { _, playlist ->
                PickerRow(
                    playlist.name,
                    songCountText(playlist.songCount),
                    onClick = { actions.addTo(playlist) },
                    lead = { Mosaic(playlist.coverIds.map(Art::Song), size = 56.dp) },
                    trailing = if (playlist.alreadyIn) {
                        { SymbolIcon(Symbol.Check, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun songCountText(count: Int): String =
    pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count))

/** A collection row in the picker (`.row.coll`), whose subtitle may be absent ("New playlist"). */
@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    lead: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { lead() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * Add songs (frame D): 92% of the screen, the pill search bar, then every matching song with an add
 * toggle that squares off and fills once the song is in the playlist. The sheet stays open.
 */
@Composable
fun ExpressiveAddSongs(state: AddSongsState, actions: AddSongsActions) {
    val count = state.existing.size
    val subtitle = state.playlistName?.let { stringResource(R.string.add_songs_subtitle, it, songCountText(count)) }
    ExpressiveSheet(onDismiss = actions.dismiss, bottomPadding = 0.dp) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            TitleBlock(stringResource(R.string.add_songs_title), subtitle)
            SearchField(
                state.query,
                actions.setQuery,
                stringResource(R.string.add_songs_placeholder),
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                groupItems(state.matches, key = { it.key }) { _, track ->
                    val added = track.key in state.existing
                    SongRow(
                        title = track.title,
                        subtitle = track.artist,
                        lead = RowLead.Cover(Art.of(track.mediaId)),
                        onClick = { actions.toggle(track) },
                        trailing = { RowAddToggle(track.title, added) { actions.toggle(track) } },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/**
 * The playlist name dialog (frame E1 and its two other titles): the emphasised title, the outlined
 * "Playlist name" field focused on open, Cancel and a confirm that waits for a name.
 */
@Composable
fun ExpressivePlaylistNameDialog(state: PlaylistNameState, actions: PlaylistNameActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ExpressiveDialog(
        title = state.title,
        onDismiss = actions.dismiss,
        confirmButton = {
            ExpressiveButton(state.confirm, actions.confirm, variant = ButtonVariant.Text, enabled = state.canConfirm)
        },
        dismissButton = { ExpressiveButton(stringResource(R.string.cancel), actions.dismiss, variant = ButtonVariant.Text) },
    ) {
        DialogTextField(state.name, actions.setName, stringResource(R.string.dialog_playlist_name_label), Modifier.focusRequester(focus))
    }
}

/** The delete dialog (frame E2): title, one sentence, Cancel and "Delete playlist" in error. */
@Composable
fun ExpressiveDeletePlaylistDialog(state: DeletePlaylistState, actions: DeletePlaylistActions) {
    ExpressiveDialog(
        title = stringResource(R.string.dialog_delete_playlist_title, state.name),
        onDismiss = actions.dismiss,
        body = stringResource(R.string.dialog_delete_playlist_body),
        confirmButton = {
            ExpressiveButton(
                stringResource(R.string.dialog_delete_playlist_confirm),
                actions.confirm,
                variant = ButtonVariant.Text,
                contentColor = MaterialTheme.colorScheme.error,
            )
        },
        dismissButton = { ExpressiveButton(stringResource(R.string.cancel), actions.dismiss, variant = ButtonVariant.Text) },
    )
}
