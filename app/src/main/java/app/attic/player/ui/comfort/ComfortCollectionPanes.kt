package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.attic.player.R
import app.attic.player.data.Album
import app.attic.player.data.Track
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.adaptive.CollectionLayout
import app.attic.player.ui.adaptive.LocalDetailPane
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.adaptive.collectionHeaderPaneDp
import app.attic.player.ui.adaptive.collectionLayout
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.AlbumActions
import app.attic.player.ui.model.AlbumState
import app.attic.player.ui.model.ArtistActions
import app.attic.player.ui.model.ArtistState
import app.attic.player.ui.model.FolderActions
import app.attic.player.ui.model.FolderState
import app.attic.player.ui.model.GenreActions
import app.attic.player.ui.model.GenreState
import app.attic.player.ui.model.MoodActions
import app.attic.player.ui.model.MoodState
import app.attic.player.ui.model.LikedActions
import app.attic.player.ui.model.LikedState
import app.attic.player.ui.model.PlaylistActions
import app.attic.player.ui.model.PlaylistState
import app.attic.player.ui.mosaicIds

/*
 * The collection screens on medium and expanded windows (docs/design/2026-11-25-tablet/SPEC.md,
 * Album, artist, playlist...). Each lays itself out for the width of the pane it is given:
 * - 760dp or wider, two panes: the header (cover up to 296dp, name, details, the Play row) in a start
 *   pane of 360dp (320 under 1000) that scrolls only if it must, and the songs beside it, in cards
 *   side by side at least 360dp wide, filled top to bottom;
 * - 600 to 760dp, one column with the header as a row: the cover at 200dp, the name, details and the
 *   Play row beside it, then the songs;
 * - narrower, the phone's screen ([LocalStackedCollection]).
 * The bar spans the pane, back at its start. Beside the Library's list ([LocalDetailPane]) there is
 * no back, since the list is right there, and the content starts 16dp under the status bar.
 */

/** True while a collection draws its phone layout from inside [CollectionPanes], so it doesn't branch again. */
internal val LocalStackedCollection = staticCompositionLocalOf { false }

/** The parts of a collection's header, laid out stacked in the start pane or as a row. */
private class HeaderParts(
    val title: String,
    val art: @Composable (Modifier) -> Unit,
    val byline: String? = null,
    val onBylineClick: (() -> Unit)? = null,
    val meta: String? = null,
    val person: Boolean = false,
)

/**
 * The frame every wide collection shares. [stacked] draws the phone screen, for a pane under 600dp.
 * [playRow] is the collection's Play row, given its padding. [body] adds the songs for a column
 * [Dp] wide (the end pane, or the whole pane under the header row).
 */
@Composable
private fun CollectionPanes(
    header: HeaderParts,
    onBack: () -> Unit,
    stacked: @Composable () -> Unit,
    playRow: @Composable (PaddingValues) -> Unit,
    barActions: (@Composable RowScope.() -> Unit)? = null,
    body: LazyListScope.(width: Dp) -> Unit,
) {
    val colors = Comfort.colors
    val detail = LocalDetailPane.current
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.ground)) {
        val width = maxWidth
        val layout = collectionLayout(width.value)
        if (layout == CollectionLayout.Stacked) {
            CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
            return@BoxWithConstraints
        }
        val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val top = status + if (detail) 16.dp else BarHeight
        val bottom = LocalBottomPadding.current + 16.dp
        val listState = rememberLazyListState()
        val scrolled by remember(listState) {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
        }
        if (layout == CollectionLayout.TwoPane) {
            val pane = collectionHeaderPaneDp(width.value).dp
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(pane)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = top, bottom = bottom),
                ) {
                    CollectionHeader(
                        title = header.title,
                        art = header.art,
                        byline = header.byline,
                        onBylineClick = header.onBylineClick,
                        meta = header.meta,
                        person = header.person,
                        artSize = if (header.person) 200.dp else min(296.dp, pane - 64.dp),
                    )
                    playRow(CollectionActionsPadding)
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxHeight(),
                    state = listState,
                    contentPadding = PaddingValues(top = top + 6.dp, bottom = bottom),
                ) {
                    body(width - pane)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = top, bottom = bottom)) {
                item(key = "headerRow") { HeaderRow(header, playRow) }
                item(key = "headerGap") { Spacer(Modifier.height(20.dp)) }
                body(width)
            }
        }
        // The bar spans the pane on the ground: back at the start, the menu at the end. Beside the
        // Library's list there is no back; a menu, if any, keeps its place.
        if (!detail) {
            Box(Modifier.fillMaxWidth().background(colors.ground)) {
                Bar("", onBack = onBack, lift = scrolled && layout == CollectionLayout.HeaderRow, actions = barActions ?: {})
            }
        } else if (barActions != null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = status, end = 8.dp).height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = barActions,
            )
        }
    }
}

/**
 * The header as a row, for a pane 600 to 760dp wide: the cover at 200dp, and beside it, 24dp on, the
 * name (two lines at most), the byline and the details line at the top and the Play row at the bottom.
 */
@Composable
private fun HeaderRow(header: HeaderParts, playRow: @Composable (PaddingValues) -> Unit) {
    val colors = Comfort.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp).height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        header.art(Modifier.size(200.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    header.title,
                    style = if (header.person) Comfort.type.personTitle else Comfort.type.title,
                    color = colors.on,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (header.byline != null) {
                    Row(
                        Modifier.clip(ComfortShapes.Pill).rowClick(header.onBylineClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(header.byline, style = onest(15f, 20f, 600), color = colors.accentInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (header.onBylineClick != null) ComfortIcon(Symbol.ChevronRight, size = 18.dp, tint = colors.accentInk)
                    }
                }
                if (header.meta != null) Text(header.meta, style = onest(13.5f, 19f), color = colors.on2)
            }
            playRow(PaddingValues(top = 20.dp))
        }
    }
}

// ── The screens ──────────────────────────────────────────────────────────────

@Composable
internal fun ComfortAlbumPanes(state: AlbumState, actions: AlbumActions) {
    val stacked = @Composable { ComfortAlbum(state, actions) }
    val album = state.album ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val tracks = album.tracks
    CollectionPanes(
        header = HeaderParts(
            title = album.name,
            art = { m -> Cover(Art.of(tracks.firstOrNull()?.mediaId), m) },
            byline = album.artist,
            onBylineClick = actions.openArtist,
            meta = albumKindAndYear(album),
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(album.name, state.controls, actions.controls, padding = it) },
    ) { width ->
        columnGroupItems(tracks.withIndex().toList(), rowColumns(width.value), "songs", key = { it.value.key }) { _, (index, track) ->
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = SongLead.Number(trackNumber(track, index)),
                current = state.currentKey == track.key,
                playing = state.isPlaying,
                meta = formatDuration(track.durationMs),
                onClick = { actions.play(index) },
                onLongClick = { actions.showMenu(track) },
                onSwipeToQueue = { actions.queue(track) },
                trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
            )
        }
        item(key = "footer") {
            Note(stringResource(R.string.album_footer, songCount(tracks.size), totalLength(tracks)))
        }
    }
}

@Composable
internal fun ComfortArtistPanes(state: ArtistState, actions: ArtistActions) {
    val stacked = @Composable { ComfortArtist(state, actions) }
    val artist = state.artist ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val first = artist.tracks.firstOrNull()?.mediaId
    CollectionPanes(
        header = HeaderParts(
            title = artist.name,
            art = { m -> ArtistCover(Art.of(first), m) },
            meta = songsAndLength(artist.tracks),
            person = true,
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(artist.name, state.controls, actions.controls, padding = it) },
    ) { width ->
        val columns = rowColumns(width.value)
        if (state.popular.isNotEmpty()) {
            item(key = "topLabel") { Label(stringResource(R.string.artist_top_songs), tight = true) }
            columnGroupItems(state.popular, columns, "top", key = { it.key }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = SongLead.Cover(Art.of(track.mediaId)),
                    rank = index + 1,
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index) },
                    onLongClick = { actions.showMenu(track) },
                    onSwipeToQueue = { actions.queue(track) },
                    trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                )
            }
        }
        if (artist.albums.isNotEmpty()) {
            item(key = "albumsLabel") { Label(stringResource(R.string.artist_albums)) }
            albumGrid(artist.albums, width, actions.openAlbum) { album ->
                album.year?.toString() ?: stringResource(if (album.tracks.size == 1) R.string.album_kind_single else R.string.album_kind_album)
            }
        }
        if (state.others.isNotEmpty()) {
            item(key = "allLabel") { Label(stringResource(R.string.artist_all_songs)) }
            val offset = state.popular.size
            columnGroupItems(state.others, columns, "all", key = { it.key }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = SongLead.Cover(Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index + offset) },
                    onLongClick = { actions.showMenu(track) },
                    onSwipeToQueue = { actions.queue(track) },
                    trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                )
            }
        }
    }
}

@Composable
internal fun ComfortFolderPanes(state: FolderState, actions: FolderActions) {
    val stacked = @Composable { ComfortFolder(state, actions) }
    val folder = state.folder ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val tracks = folder.tracks
    CollectionPanes(
        header = HeaderParts(
            title = folder.name,
            art = { m -> Mosaic(mosaicIds(tracks).map(Art::Song), m) },
            meta = stringResource(R.string.folder_subtitle) + "\n" + songsAndLength(tracks),
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(folder.name, state.controls, actions.controls, padding = it) },
    ) { width ->
        songColumns(tracks, rowColumns(width.value), state.currentKey, state.isPlaying, { actions.play(it) }, actions.showMenu, actions.queue)
    }
}

@Composable
internal fun ComfortGenrePanes(state: GenreState, actions: GenreActions) {
    val stacked = @Composable { ComfortGenre(state, actions) }
    val genre = state.genre ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val tracks = genre.tracks
    CollectionPanes(
        header = HeaderParts(
            title = genre.name,
            art = { m -> Mosaic(mosaicIds(tracks).map(Art::Song), m) },
            meta = stringResource(R.string.genre_subtitle) + "\n" + songsAndLength(tracks),
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(genre.name, state.controls, actions.controls, radio = actions.startRadio, padding = it) },
    ) { width ->
        if (genre.albums.isNotEmpty()) {
            item(key = "albumsLabel") { Label(stringResource(R.string.artist_albums), tight = true) }
            albumGrid(genre.albums, width, actions.openAlbum) { it.artist }
            item(key = "songsLabel") { Label(stringResource(R.string.library_filter_songs)) }
        }
        songColumns(tracks, rowColumns(width.value), state.currentKey, state.isPlaying, { actions.play(it) }, actions.showMenu, actions.queue)
    }
}

/** A mood's page on a tablet: the genre page's panes, with radio and no add to playlist (the mix changes daily). */
@Composable
internal fun ComfortMoodPanes(state: MoodState, actions: MoodActions) {
    val stacked = @Composable { ComfortMood(state, actions) }
    val tile = state.tile ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val tracks = tile.tracks
    CollectionPanes(
        header = HeaderParts(
            title = tile.name,
            art = { m -> Mosaic(tile.mosaicIds.map(Art::Song), m) },
            meta = stringResource(R.string.mood_kind, songsAndLength(tracks)) + "\n" + stringResource(R.string.mood_note),
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = {
            PlayRow(tile.name, state.controls, actions.controls, radio = actions.startRadio, radioLabel = stringResource(R.string.mood_radio), addToPlaylist = false, padding = it)
        },
    ) { width ->
        songColumns(tracks, rowColumns(width.value), state.currentKey, state.isPlaying, { actions.play(it) }, actions.showMenu, actions.queue)
    }
}

@Composable
internal fun ComfortFavouritesPanes(state: LikedState, actions: LikedActions) {
    val stacked = @Composable { ComfortFavourites(state, actions) }
    val title = stringResource(R.string.favourites)
    val tracks = state.tracks
    CollectionPanes(
        header = HeaderParts(title = title, art = { m -> FavouritesArt(m) }, meta = songsAndLength(tracks)),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(title, state.controls, actions.controls, padding = it) },
        barActions = if (tracks.isEmpty()) null else {
            {
                Box {
                    ComfortIconButton(Symbol.MoreVert, stringResource(R.string.favourites_options_cd), actions.openOverflow)
                    ComfortMenu(expanded = state.overflowOpen, onDismiss = actions.dismissOverflow) {
                        SmartShuffleRow(state.controls, actions.toggleSmartShuffle)
                    }
                }
            }
        },
    ) { width ->
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.favourites_empty_title), body = stringResource(R.string.favourites_empty_body), art = {})
            }
        } else {
            songColumns(tracks, rowColumns(width.value), state.currentKey, state.isPlaying, { actions.play(it) }, actions.showMenu, actions.queue)
        }
    }
}

@Composable
internal fun ComfortPlaylistPanes(state: PlaylistState, actions: PlaylistActions) {
    val stacked = @Composable { ComfortPlaylist(state, actions) }
    val playlist = state.playlist ?: return CompositionLocalProvider(LocalStackedCollection provides true) { stacked() }
    val tracks = state.tracks
    CollectionPanes(
        header = HeaderParts(
            title = playlist.name,
            art = { m -> Mosaic(mosaicIds(tracks).map(Art::Song), m) },
            meta = songsAndLength(tracks),
        ),
        onBack = actions.back,
        stacked = stacked,
        playRow = { PlayRow(playlist.name, state.controls, actions.controls, padding = it) },
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
    ) { width ->
        val columns = rowColumns(width.value)
        val row: @Composable (Track) -> Unit = { track ->
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = SongLead.Cover(Art.of(track.mediaId)),
                current = state.currentKey == track.key,
                playing = state.isPlaying,
                onClick = { actions.play(track) },
                onLongClick = { actions.showMenu(track) },
                onSwipeToQueue = { actions.queue(track) },
                trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
            )
        }
        val tools = @Composable {
            PlaylistTools(onAdd = actions.startAdding, onReorder = actions.toggleReordering, canReorder = tracks.size > 1)
        }
        if (columns <= 1 || tracks.isEmpty()) {
            // One card, as on the phone: the tools are its first row.
            val count = tracks.size + 1
            item(key = "tools") { GroupCell(0, count) { tools() } }
            if (tracks.isEmpty()) {
                item(key = "empty") {
                    EmptyState(stringResource(R.string.playlist_empty_title), body = stringResource(R.string.playlist_empty_body), art = {})
                }
            } else {
                itemsIndexed(tracks, key = { _, track -> track.key }) { index, track ->
                    GroupCell(index + 1, count) {
                        CompositionLocalProvider(LocalHairlineAbove provides (index > 0)) { row(track) }
                    }
                }
            }
        } else {
            // The tools in a card of their own across the columns, then the songs.
            item(key = "tools") {
                Box(Modifier.padding(horizontal = GroupMargin)) { ColumnCell(0, 1) { tools() } }
            }
            item(key = "toolsGap") { Spacer(Modifier.height(ColumnGap)) }
            columnGroupItems(tracks, columns, "songs", key = { it.key }) { _, track -> row(track) }
        }
    }
    if (state.reordering) ReorderMode(state, actions)
}

/** Every song of a folder, genre or Favourites, in [columns] cards. */
private fun LazyListScope.songColumns(
    tracks: List<Track>,
    columns: Int,
    currentKey: String?,
    isPlaying: Boolean,
    onPlay: (Int) -> Unit,
    onMenu: (Track) -> Unit,
    onQueue: (Track) -> Unit,
) {
    columnGroupItems(tracks, columns, "songs", key = { it.key }) { index, track ->
        SongRow(
            title = track.title,
            subtitle = track.artist,
            lead = SongLead.Cover(Art.of(track.mediaId)),
            current = currentKey == track.key,
            playing = isPlaying,
            onClick = { onPlay(index) },
            onLongClick = { onMenu(track) },
            onSwipeToQueue = { onQueue(track) },
            trailing = { SongMoreButton(track.title, { onMenu(track) }) },
        )
    }
}

/**
 * Albums as a grid where the phone has a shelf: tiles in columns at least 160dp wide, 14 apart, 24dp
 * in from the pane's sides, one lazy item per line.
 */
private fun LazyListScope.albumGrid(albums: List<Album>, width: Dp, onOpen: (Album) -> Unit, subtitle: @Composable (Album) -> String) {
    val columns = adaptiveColumns(width.value - 48f, 160f, 14f)
    val lines = albums.chunked(columns)
    lines.forEachIndexed { line, part ->
        item(key = "albumLine:${part.first().key}") {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = if (line < lines.size - 1) 20.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (column in 0 until columns) {
                    Box(Modifier.weight(1f)) {
                        part.getOrNull(column)?.let { album ->
                            Tile(
                                title = album.name,
                                onClick = { onOpen(album) },
                                art = { m -> Cover(Art.of(album.tracks.firstOrNull()?.mediaId), m) },
                                subtitle = subtitle(album),
                                width = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
