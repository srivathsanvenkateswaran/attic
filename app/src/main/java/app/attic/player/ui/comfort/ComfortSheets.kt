@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.playback.SleepChoice
import app.attic.player.ui.sleepChoiceLabel
import app.attic.player.ui.sleepTimerSummary
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.languageName
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
import kotlin.math.roundToInt

/*
 * Sheets, dialogs and menus in the Comfort style (oneui/07-sheets.html). Things that need a decision
 * sit at the bottom: the track menu, the playlist picker and Add songs are bottom sheets; naming and
 * deleting a playlist are bottom dialogs. A short choice opens beside whatever asked for it: the
 * language picker is a pop-over on the track menu's "Language" row. Over Now Playing, the queue or
 * lyrics the caller wraps these in the cover's wash, so they take the player's surfaces (frame H).
 */

// ── Track menu ───────────────────────────────────────────────────────────────

/**
 * The track menu (frames A, C, G, H): the song, then radio, favourite, play next and add to queue as
 * a row of the deck's superellipse toggles with their labels (the heart lights while the song is a
 * favourite), then only the rows that go somewhere or open something, "Share" among them. "Remove
 * from this playlist" comes last, in red. The language row opens the picker as a pop-over over the
 * menu. Opened from Now Playing, "Sleep timer" leads the rows and opens its choices the same way.
 */
@Composable
fun ComfortTrackMenu(state: TrackMenuState, actions: TrackMenuActions) {
    val track = state.track
    val colors = Comfort.colors
    ComfortSheet(onDismiss = actions.dismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(track.title, track.artist, art = { m -> Cover(Art.of(track.mediaId), m) })
            // A song opened from another app isn't in the library: only the sleep timer applies to it.
            if (state.external) {
                Hairlined { if (state.sleepTimer != null) item { SleepTimerRow(state, actions) } }
                return@Column
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp)) {
                QuickAction(Symbol.Sensors, stringResource(R.string.menu_start_radio), actions.startRadio)
                QuickAction(
                    Symbol.Favorite,
                    stringResource(if (state.liked) R.string.favourite_remove else R.string.favourite_add),
                    actions.toggleLike,
                    checked = state.liked,
                )
                QuickAction(Symbol.PlaylistPlay, stringResource(R.string.menu_play_next), actions.playNext)
                QuickAction(Symbol.QueueMusic, stringResource(R.string.menu_add_to_queue), actions.addToQueue)
            }
            // `.quick + .sheet-row::before`: the first row's hairline runs the sheet's width.
            Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(colors.line))
            Hairlined {
                if (state.sleepTimer != null) item { SleepTimerRow(state, actions) }
                item { SheetRow(Symbol.PlaylistAdd, stringResource(R.string.menu_add_to_playlist), actions.pickPlaylist, end = Symbol.ChevronRight) }
                item { LanguageRow(state, actions) }
                if (state.canGoToAlbum) item { SheetRow(Symbol.Album, stringResource(R.string.menu_go_to_album), actions.goToAlbum) }
                if (state.canGoToArtist) item { SheetRow(Symbol.Person, stringResource(R.string.menu_go_to_artist), actions.goToArtist) }
                if (state.canShare) item { SheetRow(Symbol.Share, stringResource(R.string.menu_share), actions.share) }
                if (state.inPlaylist) {
                    item { SheetRow(Symbol.DoNotDisturbOn, stringResource(R.string.menu_remove_from_playlist), actions.removeFromPlaylist, danger = true) }
                }
            }
        }
    }
}

/**
 * `.quick .qa`: a 54dp tonal superellipse with the label under it, a quarter of the sheet's width. On a
 * sheet the resting fill is one step up so it shows against the raised surface.
 */
@Composable
private fun RowScope.QuickAction(symbol: Symbol, label: String, onClick: () -> Unit, checked: Boolean = false) {
    val colors = Comfort.colors
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 104.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TonalIconButton(
            symbol,
            label,
            onClick,
            checked = checked,
            large = true,
            container = if (colors.dark) colors.fill2 else colors.fill,
        )
        Text(
            label,
            style = onest(12.5f, 16f, 500),
            color = colors.on,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.widthIn(max = 86.dp),
        )
    }
}

/**
 * "Language: {name}" ending in `unfold_more`, with the picker as a pop-over on the sheet (frame C):
 * "Song language" as a caption, the seven choices, the current one in the accent with a tick, nothing
 * ticked when the language is unknown. A choice saves it and closes the menu; tapping outside closes
 * only the pop-over and changes nothing.
 */
@Composable
private fun LanguageRow(state: TrackMenuState, actions: TrackMenuActions) {
    var rowTop by remember { mutableIntStateOf(0) }
    var rowHeight by remember { mutableIntStateOf(0) }
    // Unknown until the menu has been laid out once; until then it is taken to open upward, as it does on a phone.
    var menuHeight by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }
    val density = LocalDensity.current
    val windowHeight = LocalView.current.rootView.height
    // Material keeps the menu 48dp inside the window; the navigation bar is below that.
    val opensDown = rowTop + menuHeight + with(density) { 96.dp.roundToPx() } <= windowHeight
    BoxWithConstraints(
        Modifier.onGloballyPositioned {
            rowTop = it.positionInWindow().y.roundToInt()
            rowHeight = it.size.height
        },
    ) {
        SheetRow(
            Symbol.Language,
            stringResource(R.string.menu_language, languageName(state.language)),
            actions.pickLanguage,
            end = Symbol.UnfoldMore,
        )
        ComfortMenu(
            expanded = state.pickingLanguage,
            onDismiss = actions.stopPickingLanguage,
            modifier = Modifier.onSizeChanged { menuHeight = it.height },
            onSheet = true,
            // Toward the row's end, where the glyph that opened it is, and over the row itself. Material adds
            // the offset to the position it picks: below the row it starts at the row's foot, so it is lifted
            // by the row's height to start at its top; above the row (no room below, as on a phone) it ends at
            // the row's top, so it is lowered by the row's height to end at its foot.
            offset = DpOffset(maxWidth - 214.dp, with(density) { (if (opensDown) -rowHeight else rowHeight).toDp() }),
        ) {
            MenuCaption(stringResource(R.string.language_picker_title))
            for ((code, _) in app.attic.player.data.LanguageLabels.CHOICES) {
                MenuRow(null, languageName(code), { actions.chooseLanguage(code) }, selected = code == state.language, check = true)
            }
        }
    }
}

/**
 * "Sleep timer", first among the rows when the menu opens from Now Playing, with what the timer is
 * doing at its end ("12 min left") and `unfold_more`. Its choices open as a pop-over on the row, the
 * way the language picker does: "Sleep timer" as a caption, the six choices, the current one in the
 * accent with a tick. A choice sets the timer and closes the menu; tapping outside closes only the
 * pop-over.
 */
@Composable
private fun SleepTimerRow(state: TrackMenuState, actions: TrackMenuActions) {
    val timer = state.sleepTimer ?: return
    var rowHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.onSizeChanged { rowHeight = it.height }) {
        SheetRow(
            Symbol.Bedtime,
            stringResource(R.string.sleep_timer),
            actions.pickSleepTimer,
            value = sleepTimerSummary(timer),
            end = Symbol.UnfoldMore,
        )
        ComfortMenu(
            expanded = state.pickingSleepTimer,
            onDismiss = actions.stopPickingSleepTimer,
            onSheet = true,
            offset = DpOffset(maxWidth - 214.dp, with(density) { -rowHeight.toDp() }),
        ) {
            MenuCaption(stringResource(R.string.sleep_timer))
            for (choice in SleepChoice.entries) {
                MenuRow(null, sleepChoiceLabel(choice), { actions.chooseSleepTimer(choice) }, selected = choice == timer.choice, check = true)
            }
        }
    }
}

/**
 * The language picker as a sheet of its own, for a caller that opens it without the track menu. The
 * track menu draws it as a pop-over instead ([ComfortTrackMenu]).
 */
@Composable
fun ComfortLanguagePicker(state: LanguagePickerState, actions: LanguagePickerActions) {
    ComfortSheet(onDismiss = actions.dismiss) {
        SheetTitle(stringResource(R.string.language_picker_title), subtitle = state.songTitle)
        Hairlined {
            for ((code, _) in state.choices) {
                item { SheetRow(null, languageName(code), { actions.choose(code) }, selected = code == state.current) }
            }
        }
    }
}

// ── Playlist picker and Add songs ────────────────────────────────────────────

/**
 * The playlist picker (frame B): "New playlist" first, then each playlist drawn as the collection it is,
 * its mosaic, name and count, with a tick when it already holds what is being added.
 */
@Composable
fun ComfortPlaylistPicker(state: PlaylistPickerState, actions: PlaylistPickerActions) {
    val colors = Comfort.colors
    ComfortSheet(onDismiss = actions.dismiss) {
        SheetTitle(stringResource(R.string.playlist_picker_title))
        LazyColumn {
            item(key = "new") {
                CollectionRow(
                    title = stringResource(R.string.playlist_picker_new),
                    subtitle = null,
                    lead = { m -> IconBox(Symbol.Add, m, style = IconBoxStyle.Neutral) },
                    onClick = actions.startCreating,
                )
            }
            itemsIndexed(state.playlists, key = { _, it -> it.id }) { _, playlist ->
                CompositionLocalProvider(LocalHairlineAbove provides true) {
                    CollectionRow(
                        title = playlist.name,
                        subtitle = songCount(playlist.songCount),
                        lead = { m -> Mosaic(playlist.coverIds.map(Art::Song), m) },
                        onClick = { actions.addTo(playlist) },
                        trailing = if (playlist.alreadyIn) {
                            { ComfortIcon(Symbol.Check, tint = colors.accentInk, modifier = Modifier.padding(end = 6.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/**
 * Add songs (frame D): the one tall sheet. Under the title, the playlist and its count, which changes
 * as rows are tapped; the capsule search; every matching song with a plus, or a tick in the accent
 * once it is in the playlist. The list fades out at the sheet's foot. The sheet stays open.
 */
@Composable
fun ComfortAddSongs(state: AddSongsState, actions: AddSongsActions) {
    val colors = Comfort.colors
    val subtitle = state.playlistName?.let { stringResource(R.string.add_songs_subtitle, it, songCount(state.existing.size)) }
    ComfortSheet(onDismiss = actions.dismiss) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            SheetTitle(stringResource(R.string.add_songs_title), subtitle = subtitle)
            SearchField(state.query, actions.setQuery, stringResource(R.string.add_songs_placeholder), Modifier.padding(bottom = 6.dp))
            Box(Modifier.weight(1f)) {
                LazyColumn {
                    itemsIndexed(state.matches, key = { _, track -> track.key }) { index, track ->
                        val added = track.key in state.existing
                        CompositionLocalProvider(LocalHairlineAbove provides (index > 0)) {
                            SongRow(
                                title = track.title,
                                subtitle = track.artist,
                                lead = SongLead.Cover(Art.of(track.mediaId)),
                                onClick = { actions.toggle(track) },
                                trailing = { SongAddButton(track.title, added, { actions.toggle(track) }) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Brush.verticalGradient(0f to Color.Transparent, 0.62f to colors.raised)),
                )
            }
        }
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

/**
 * The playlist name dialog (frames E1 and I): a bottom dialog above the keyboard, the "Playlist name"
 * field focused on open with the cursor after the name, Cancel and a confirm that waits for a name.
 */
@Composable
fun ComfortPlaylistNameDialog(state: PlaylistNameState, actions: PlaylistNameActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ComfortDialog(
        title = state.title,
        onDismiss = actions.dismiss,
        confirmText = state.confirm,
        onConfirm = actions.confirm,
        dismissText = stringResource(R.string.cancel),
        confirmEnabled = state.canConfirm,
    ) {
        DialogField(
            state.name,
            actions.setName,
            stringResource(R.string.dialog_playlist_name_label),
            focusRequester = focus,
            onDone = { if (state.canConfirm) actions.confirm() },
        )
    }
}

/** The delete dialog (frame E2): the question, one sentence, Cancel and "Delete playlist" in red under the right thumb. */
@Composable
fun ComfortDeletePlaylistDialog(state: DeletePlaylistState, actions: DeletePlaylistActions) {
    ComfortDialog(
        title = stringResource(R.string.dialog_delete_playlist_title, state.name),
        onDismiss = actions.dismiss,
        confirmText = stringResource(R.string.dialog_delete_playlist_confirm),
        onConfirm = actions.confirm,
        dismissText = stringResource(R.string.cancel),
        body = stringResource(R.string.dialog_delete_playlist_body),
        danger = true,
    )
}
