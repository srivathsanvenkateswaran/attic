package app.attic.player.ui.comfort

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import app.attic.player.R
import app.attic.player.data.Album
import app.attic.player.data.Track
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.AlbumActions
import app.attic.player.ui.model.AlbumState
import app.attic.player.ui.model.ArtistActions
import app.attic.player.ui.model.ArtistState
import app.attic.player.ui.model.FolderActions
import app.attic.player.ui.model.FolderState
import app.attic.player.ui.model.LikedActions
import app.attic.player.ui.model.LikedState
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlayControlsState
import app.attic.player.ui.model.PlaylistActions
import app.attic.player.ui.model.PlaylistState
import app.attic.player.ui.mosaicIds
import java.text.NumberFormat
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/*
 * The five collection screens in the Comfort style (oneui/04-collections.html; the missing item is
 * sheet 08 frame E). One frame: a plain bar over the ground, the cover as an object with the name
 * centred under it, one row of buttons (Play, shuffle, add to a playlist), and the songs in a card.
 * Nothing takes its colour from the artwork except the artwork. Once the title has scrolled under the
 * bar, the bar lifts (ground and a hairline) and shows the title.
 */

/** The cover's size in the header: the sheet draws 176 so the third row stays clear of the dock. */
private val HeaderArt = 176.dp

// ── Album ────────────────────────────────────────────────────────────────────

/** An album (frames A, F, G): track numbers and lengths in one card, bars on the playing track, the count under it. */
@Composable
fun ComfortAlbum(state: AlbumState, actions: AlbumActions) {
    val album = state.album ?: return GoneState(R.string.collection_album_gone, Symbol.Album, actions.back)
    val tracks = album.tracks
    CollectionFrame(title = album.name, onBack = actions.back, titleTop = HeaderArt) {
        item(key = "header") {
            CollectionHeader(
                title = album.name,
                art = { m -> Cover(Art.of(tracks.firstOrNull()?.mediaId), m) },
                byline = album.artist,
                onBylineClick = actions.openArtist,
                meta = albumKindAndYear(album),
                artSize = HeaderArt,
            )
        }
        item(key = "actions") { PlayRow(album.name, state.controls, actions.controls) }
        item(key = "gap") { Spacer(Modifier.height(16.dp)) }
        groupItems(tracks.withIndex().toList(), key = { it.value.key }) { _, (index, track) ->
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = SongLead.Number(trackNumber(track, index)),
                current = state.currentKey == track.key,
                playing = state.isPlaying,
                meta = formatDuration(track.durationMs),
                onClick = { actions.play(index) },
                onLongClick = { actions.showMenu(track) },
                trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
            )
        }
        item(key = "footer") {
            Note(stringResource(R.string.album_footer, songCount(tracks.size), totalLength(tracks)))
        }
    }
}

/** The tag's track number, without a disc prefix (1003 is disc 1, track 3), else the position. */
private fun trackNumber(track: Track, index: Int): Int =
    track.trackNumber?.let { it % 1000 }?.takeIf { it > 0 } ?: (index + 1)

/** "Album • 2026", "Single • 2026", or the kind alone when the year isn't known. */
@Composable
private fun albumKindAndYear(album: Album): String {
    val kind = stringResource(if (album.tracks.size == 1) R.string.album_kind_single else R.string.album_kind_album)
    return album.year?.let { stringResource(R.string.album_kind_year, kind, it.toString()) } ?: kind
}

// ── Artist ───────────────────────────────────────────────────────────────────

/**
 * An artist (frames B, H): the picture in the rounder shape with the name under it, "Top songs" as
 * five ranked rows in a card, "Albums" as a shelf with the year, then "All songs" in a card.
 */
@Composable
fun ComfortArtist(state: ArtistState, actions: ArtistActions) {
    val artist = state.artist ?: return GoneState(R.string.collection_artist_gone, Symbol.Person, actions.back)
    val first = artist.tracks.firstOrNull()?.mediaId
    CollectionFrame(title = artist.name, onBack = actions.back, titleTop = 148.dp) {
        item(key = "header") {
            CollectionHeader(
                title = artist.name,
                art = { m -> ArtistCover(Art.of(first), m) },
                meta = songsAndLength(artist.tracks),
                person = true,
            )
        }
        item(key = "actions") { PlayRow(artist.name, state.controls, actions.controls) }
        if (state.popular.isNotEmpty()) {
            item(key = "topLabel") { Label(stringResource(R.string.artist_top_songs)) }
            groupItems(state.popular, key = { "top:${it.key}" }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = SongLead.Cover(Art.of(track.mediaId)),
                    rank = index + 1,
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index) },
                    onLongClick = { actions.showMenu(track) },
                    trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                )
            }
        }
        if (artist.albums.isNotEmpty()) {
            item(key = "albums") {
                Column {
                    Label(stringResource(R.string.artist_albums))
                    Shelf {
                        items(artist.albums, key = { it.key }) { album ->
                            Tile(
                                title = album.name,
                                onClick = { actions.openAlbum(album) },
                                art = { m -> Cover(Art.of(album.tracks.firstOrNull()?.mediaId), m) },
                                subtitle = album.year?.toString()
                                    ?: stringResource(if (album.tracks.size == 1) R.string.album_kind_single else R.string.album_kind_album),
                            )
                        }
                    }
                }
            }
        }
        if (state.others.isNotEmpty()) {
            item(key = "allLabel") { Label(stringResource(R.string.artist_all_songs)) }
            val offset = state.popular.size
            groupItems(state.others, key = { it.key }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = SongLead.Cover(Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index + offset) },
                    onLongClick = { actions.showMenu(track) },
                    trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                )
            }
        }
    }
}

// ── Folder and Favourites ────────────────────────────────────────────────────

/** A folder playlist (frame I): a playlist that cannot be edited, so no more button and no tools. */
@Composable
fun ComfortFolder(state: FolderState, actions: FolderActions) {
    val folder = state.folder ?: return GoneState(R.string.collection_playlist_gone, Symbol.QueueMusic, actions.back)
    val tracks = folder.tracks
    CollectionFrame(title = folder.name, onBack = actions.back, titleTop = HeaderArt) {
        item(key = "header") {
            CollectionHeader(
                title = folder.name,
                art = { m -> Mosaic(mosaicIds(tracks).map(Art::Song), m) },
                meta = stringResource(R.string.folder_subtitle) + "\n" + songsAndLength(tracks),
                artSize = HeaderArt,
            )
        }
        item(key = "actions") { PlayRow(folder.name, state.controls, actions.controls) }
        item(key = "gap") { Spacer(Modifier.height(16.dp)) }
        songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu)
    }
}

/**
 * Favourites (frames E, K): the tonal heart box at header size, newest first. With none the three
 * buttons are dim and the message says where the heart is.
 */
@Composable
fun ComfortFavourites(state: LikedState, actions: LikedActions) {
    val title = stringResource(R.string.favourites)
    val tracks = state.tracks
    CollectionFrame(
        title = title,
        onBack = actions.back,
        titleTop = HeaderArt,
        barActions = {
            // The mock has no place for smart shuffle, so it sits behind a more button as on a playlist.
            if (tracks.isNotEmpty()) {
                Box {
                    ComfortIconButton(Symbol.MoreVert, stringResource(R.string.favourites_options_cd), actions.openOverflow)
                    ComfortMenu(expanded = state.overflowOpen, onDismiss = actions.dismissOverflow) {
                        SmartShuffleRow(state.controls, actions.toggleSmartShuffle)
                    }
                }
            }
        },
    ) {
        item(key = "header") {
            CollectionHeader(
                title = title,
                art = { m -> FavouritesArt(m) },
                meta = songsAndLength(tracks),
                artSize = HeaderArt,
            )
        }
        item(key = "actions") { PlayRow(title, state.controls, actions.controls) }
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    stringResource(R.string.favourites_empty_title),
                    body = stringResource(R.string.favourites_empty_body),
                    art = {},
                )
            }
        } else {
            item(key = "gap") { Spacer(Modifier.height(16.dp)) }
            songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu)
        }
    }
}

/** "Smart shuffle" in a collection's menu: selected, with the tick, while on for this collection. */
@Composable
private fun SmartShuffleRow(controls: PlayControlsState, onToggle: () -> Unit) {
    val on = controls.smartShuffle == true
    MenuRow(
        Symbol.ShuffleOn,
        stringResource(R.string.collection_smart_shuffle),
        onToggle,
        modifier = Modifier.semantics { toggleableState = ToggleableState(on) },
        selected = on,
        check = true,
    )
}

private fun LazyListScope.songRows(
    tracks: List<Track>,
    currentKey: String?,
    isPlaying: Boolean,
    onPlay: (Int) -> Unit,
    onMenu: (Track) -> Unit,
) {
    groupItems(tracks, key = { it.key }) { index, track ->
        SongRow(
            title = track.title,
            subtitle = track.artist,
            lead = SongLead.Cover(Art.of(track.mediaId)),
            current = currentKey == track.key,
            playing = isPlaying,
            onClick = { onPlay(index) },
            onLongClick = { onMenu(track) },
            trailing = { SongMoreButton(track.title, { onMenu(track) }) },
        )
    }
}

// ── Playlist ─────────────────────────────────────────────────────────────────

/**
 * A playlist (frames C, D, J; sheet 07 F1): rename and delete behind the more button in the bar, and
 * "Add songs" and "Reorder songs" as two labelled halves at the head of the song card. Reorder is a
 * mode drawn over everything, dock included: the bar names it in the accent, every row ends in a
 * handle, and an action bar holds "Add songs" and "Done reordering". The dialogs and the Add songs sheet
 * are drawn by PlaylistScreen around this.
 */
@Composable
fun ComfortPlaylist(state: PlaylistState, actions: PlaylistActions) {
    val playlist = state.playlist ?: return GoneState(R.string.collection_playlist_gone, Symbol.QueueMusic, actions.back)
    val tracks = state.tracks
    CollectionFrame(
        title = playlist.name,
        onBack = actions.back,
        titleTop = HeaderArt,
        barActions = {
            Box {
                ComfortIconButton(Symbol.MoreVert, stringResource(R.string.collection_options_cd), actions.openOverflow)
                ComfortMenu(expanded = state.overflowOpen, onDismiss = actions.dismissOverflow) {
                    if (tracks.isNotEmpty()) SmartShuffleRow(state.controls, actions.toggleSmartShuffle)
                    MenuRow(Symbol.Edit, stringResource(R.string.playlist_rename), actions.startRenaming)
                    MenuRow(Symbol.Delete, stringResource(R.string.playlist_delete), actions.startDeleting, danger = true)
                }
            }
        },
    ) {
        item(key = "header") {
            CollectionHeader(
                title = playlist.name,
                art = { m -> Mosaic(mosaicIds(tracks).map(Art::Song), m) },
                meta = songsAndLength(tracks),
                artSize = HeaderArt,
            )
        }
        item(key = "actions") { PlayRow(playlist.name, state.controls, actions.controls) }
        item(key = "gap") { Spacer(Modifier.height(16.dp)) }
        val count = tracks.size + 1
        item(key = "tools") {
            GroupCell(0, count) {
                PlaylistTools(onAdd = actions.startAdding, onReorder = actions.toggleReordering, canReorder = tracks.size > 1)
            }
        }
        if (tracks.isEmpty()) {
            // The sentence points at the plus in "Add songs" just above it.
            item(key = "empty") {
                EmptyState(
                    stringResource(R.string.playlist_empty_title),
                    body = stringResource(R.string.playlist_empty_body),
                    art = {},
                    modifier = Modifier.padding(top = 0.dp),
                )
            }
        } else {
            itemsIndexed(tracks, key = { _, track -> track.key }) { index, track ->
                GroupCell(index + 1, count) {
                    // Under the tools the first song takes the tools' full-width hairline instead of its own.
                    CompositionLocalProvider(LocalHairlineAbove provides (index > 0)) {
                        SongRow(
                            title = track.title,
                            subtitle = track.artist,
                            lead = SongLead.Cover(Art.of(track.mediaId)),
                            current = state.currentKey == track.key,
                            playing = state.isPlaying,
                            onClick = { actions.play(track) },
                            onLongClick = { actions.showMenu(track) },
                            trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                        )
                    }
                }
            }
        }
    }
    if (state.reordering) ReorderMode(state, actions)
}

/**
 * `.pl-tools`: "Add songs" and "Reorder songs" as two halves of the card's first row, split by a short
 * divider, with a hairline under them. Reorder is off with fewer than two songs.
 */
@Composable
private fun PlaylistTools(onAdd: () -> Unit, onReorder: () -> Unit, canReorder: Boolean) {
    val colors = Comfort.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                val x = size.width / 2
                drawRect(colors.fill2, topLeft = Offset(x, 18.dp.toPx()), size = Size(1.dp.toPx(), size.height - 36.dp.toPx()))
                val inset = 20.dp.toPx()
                drawRect(colors.line, topLeft = Offset(inset, size.height - 1.dp.toPx()), size = Size(size.width - inset * 2, 1.dp.toPx()))
            },
    ) {
        ToolHalf(Symbol.Add, stringResource(R.string.playlist_add_songs_cd), onAdd, enabled = true)
        ToolHalf(Symbol.SwapVert, stringResource(R.string.playlist_reorder_cd), onReorder, enabled = canReorder)
    }
}

@Composable
private fun RowScope.ToolHalf(symbol: Symbol, label: String, onClick: () -> Unit, enabled: Boolean) {
    val colors = Comfort.colors
    Row(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComfortIcon(symbol, size = 22.dp, tint = colors.on2)
        Text(label, style = onest(14.5f, 20f, 600), color = colors.on, maxLines = 1)
    }
}

/**
 * Reorder mode (frame D), over everything including the dock: the bar names the mode in the accent,
 * every row ends in a handle and lifts while dragged, and the action bar holds "Add songs" and "Done
 * reordering". Back leaves the mode. Tapping a row does nothing here; a long press still opens the menu.
 */
@Composable
private fun ReorderMode(state: PlaylistState, actions: PlaylistActions) {
    val haptics = rememberHaptics()
    val tracks = state.tracks
    // The rows follow the playlist, except mid-drag, when they hold the order being dragged.
    val rows = remember { mutableStateListOf<Track>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(tracks, dragging) {
        if (!dragging) {
            rows.clear()
            rows.addAll(tracks)
        }
    }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        if (fromIndex >= 0 && toIndex >= 0) rows.add(toIndex, rows.removeAt(fromIndex))
    }
    Dialog(onDismissRequest = actions.toggleReordering, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SystemBarsFor(Comfort.colors.dark, dimWindow = false)
        val colors = Comfort.colors
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.fillMaxSize().background(colors.ground)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = top + 16.dp, bottom = bottom + 130.dp),
            ) {
                itemsIndexed(rows, key = { _, track -> track.key }) { index, track ->
                    ReorderableItem(reorderState, key = track.key) { isDragging ->
                        GroupCell(index, rows.size) {
                            SongRow(
                                title = track.title,
                                subtitle = track.artist,
                                lead = SongLead.Cover(Art.of(track.mediaId)),
                                current = state.currentKey == track.key,
                                playing = state.isPlaying,
                                lifted = isDragging,
                                onClick = { actions.play(track) },
                                onLongClick = { actions.showMenu(track) },
                                trailing = {
                                    SongDragHandle(
                                        track.title,
                                        Modifier.draggableHandle(
                                            onDragStarted = {
                                                haptics.perform(Haptic.PickUp)
                                                dragging = true
                                            },
                                            onDragStopped = {
                                                haptics.perform(Haptic.Drop)
                                                actions.reorder(rows.map { it.key })
                                                dragging = false
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            Bar(stringResource(R.string.playlist_reorder_cd), lift = true, accentTitle = true)
            // The list fades out behind the action bar, as it does behind the dock.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(118.dp + bottom)
                    .background(Brush.verticalGradient(0f to Color.Transparent, 36f / 118f to colors.ground)),
            )
            ActionBar(Modifier.align(Alignment.BottomCenter)) {
                ActionBarItem(Symbol.Add, stringResource(R.string.playlist_add_songs_cd), actions.startAdding)
                ActionBarItem(Symbol.Check, stringResource(R.string.playlist_reorder_done_cd), actions.toggleReordering, go = true)
            }
        }
    }
}

// ── The shared frame ─────────────────────────────────────────────────────────

/**
 * The frame every collection shares: the list on the ground, and the bar over it. The bar is plain
 * (back, and the playlist's more button) until the title, [titleTop] below the top of the first item,
 * has scrolled under it; then it lifts and shows [title].
 */
@Composable
private fun CollectionFrame(
    title: String,
    onBack: () -> Unit,
    titleTop: Dp,
    listState: LazyListState = rememberLazyListState(),
    barActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val colors = Comfort.colors
    val density = LocalDensity.current
    // The title starts 6 + art + 18dp into the header.
    val titlePx = with(density) { (titleTop + 24.dp).roundToPx() }
    val collapsed by remember(listState, titlePx) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titlePx }
    }
    val barHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    Box(Modifier.fillMaxSize().background(colors.ground)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = barHeight, bottom = LocalBottomPadding.current + 16.dp),
            content = content,
        )
        // The ground behind the bar even before it lifts, so the cover never shows through the back button.
        Box(Modifier.fillMaxWidth().background(colors.ground)) {
            Bar(if (collapsed) title else "", onBack = onBack, lift = collapsed, actions = barActions)
        }
    }
}

/**
 * `.c-actions`: Play (Pause while this collection plays), then shuffle, lit while it is on for this
 * collection, and add to a playlist. All three are off for an empty collection. In light the resting
 * toggles take the card colour, since the putty fill vanishes on the plaster ground.
 */
@Composable
private fun PlayRow(name: String, state: PlayControlsState, actions: PlayControlsActions) {
    val colors = Comfort.colors
    val container = if (colors.dark) colors.fill else colors.card
    CollectionActions {
        CollectionPlayButton(state.isPlaying, actions.playOrPause, Modifier.weight(1f), enabled = state.enabled)
        TonalIconButton(
            Symbol.Shuffle,
            stringResource(R.string.collection_shuffle_cd, name),
            actions.shuffle,
            checked = state.shuffleOn,
            large = true,
            enabled = state.enabled,
            container = container,
        )
        TonalIconButton(
            Symbol.PlaylistAdd,
            stringResource(R.string.collection_add_to_playlist_cd, name),
            actions.addToPlaylist,
            large = true,
            enabled = state.enabled,
            container = container,
        )
    }
}

// ── Missing ──────────────────────────────────────────────────────────────────

/** An album, artist or playlist that has gone (sheet 08 frame E): the message centred, and a plain Back under it. */
@Composable
private fun GoneState(@StringRes title: Int, symbol: Symbol, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Comfort.colors.ground)) {
        Bar("", onBack = onBack)
        EmptyState(
            stringResource(title),
            modifier = Modifier.weight(1f).padding(bottom = LocalBottomPadding.current),
            body = stringResource(R.string.collection_gone_body),
            symbol = symbol,
            fill = true,
            action = { ComfortButton(stringResource(R.string.back), onBack, style = ButtonStyle.Plain) },
        )
    }
}

// ── Copy ─────────────────────────────────────────────────────────────────────

@Composable
internal fun songCount(n: Int): String = pluralStringResource(R.plurals.song_count, n, NumberFormat.getIntegerInstance().format(n))

/** "26 min", "1 hr 13 min". */
@Composable
internal fun totalLength(tracks: List<Track>): String {
    val minutes = (tracks.sumOf { it.durationMs } / 60_000).toInt()
    return if (minutes >= 60) {
        stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

/** "6 songs, 26 min". */
@Composable
internal fun songsAndLength(tracks: List<Track>): String =
    stringResource(R.string.songs_and_duration, songCount(tracks.size), totalLength(tracks))

/**
 * For a full-screen layer in its own window (a mode, the Recap story): no window dim, and status and
 * navigation bar icons that read on [dark] or light ground.
 */
@Composable
internal fun SystemBarsFor(dark: Boolean, dimWindow: Boolean = false) {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            if (!dimWindow) window.setDimAmount(0f)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
