package app.attic.player.ui.expressive

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.data.Album
import app.attic.player.data.Track
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.model.AlbumActions
import app.attic.player.ui.model.AlbumState
import app.attic.player.ui.model.ArtistActions
import app.attic.player.ui.model.ArtistState
import app.attic.player.ui.model.FolderActions
import app.attic.player.ui.model.FolderState
import app.attic.player.ui.model.GenreActions
import app.attic.player.ui.model.GenreState
import app.attic.player.ui.model.LikedActions
import app.attic.player.ui.model.LikedState
import app.attic.player.ui.model.PlayControlsActions
import app.attic.player.ui.model.PlayControlsState
import app.attic.player.ui.model.PlaylistActions
import app.attic.player.ui.model.PlaylistState
import app.attic.player.ui.rememberArtColor
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.NumberFormat

/*
 * The five collection screens (pixel/04-collections.html; the missing item is sheet 08 frame E). One
 * frame: a header block on the cover's scheme with the title first, then the cover with the play
 * cluster beside it; the list under it on the app scheme; and a bar that takes the header's container
 * colour, the title and a play control once the title has scrolled under it.
 */

// ── Album ────────────────────────────────────────────────────────────────────

/** An album (frames A, F, G): track numbers, the wave on the current track, the count under the last one. */
@Composable
fun ExpressiveAlbum(state: AlbumState, actions: AlbumActions) {
    val album = state.album ?: return GoneState(R.string.collection_album_gone, Symbol.Album, cookie = false, actions.back)
    val tracks = album.tracks
    val seed = rememberArtColor(tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    val summary = songsAndDuration(tracks)
    CollectionFrame(
        title = album.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        header = { titleModifier ->
            CollectionHeader(
                title = album.name,
                titleModifier = titleModifier,
                byline = { Byline(album.artist, state.artistMediaId, actions.openArtist) },
                meta = listOf(albumKindAndYear(album), summary),
                art = { Cover(Art.of(tracks.firstOrNull()?.mediaId), it) },
                name = album.name,
                controls = state.controls,
                controlActions = actions.controls,
            )
        },
    ) {
        item(key = "gap") { Spacer(Modifier.height(12.dp)) }
        groupItems(tracks.withIndex().toList(), key = { it.value.key }) { _, (index, track) ->
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = RowLead.Number(trackNumber(track, index)),
                current = state.currentKey == track.key,
                playing = state.isPlaying,
                onClick = { actions.play(index) },
                onLongClick = { actions.showMenu(track) },
                onSwipeToQueue = { actions.queue(track) },
                trailing = { RowMoreButton(track.title) { actions.showMenu(track) } },
            )
        }
        item(key = "footer") {
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 14.dp),
            )
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

/** The artist under an album's title: their cookie and name, in primary because it opens the artist. */
@Composable
private fun Byline(name: String, mediaId: Long?, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .padding(top = 10.dp)
            .clip(ExpressiveShapes.Pill)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistCover(Art.of(mediaId), size = 28.dp)
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (onClick != null) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Artist ───────────────────────────────────────────────────────────────────

/** An artist (frames B, L): the cookie beside the cluster, "Top songs" ranked, "Albums" as a shelf, "All songs". */
@Composable
fun ExpressiveArtist(state: ArtistState, actions: ArtistActions) {
    val artist = state.artist ?: return GoneState(R.string.collection_artist_gone, Symbol.Person, cookie = true, actions.back)
    val first = artist.tracks.firstOrNull()?.mediaId
    val seed = rememberArtColor(first, fallback = Color.Unspecified)
    CollectionFrame(
        title = artist.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        header = { titleModifier ->
            CollectionHeader(
                title = artist.name,
                titleModifier = titleModifier,
                artist = true,
                meta = listOf(songsAndDuration(artist.tracks)),
                art = { ArtistCover(Art.of(first), it) },
                name = artist.name,
                controls = state.controls,
                controlActions = actions.controls,
            )
        },
    ) {
        if (state.popular.isNotEmpty()) {
            item(key = "topHeader") { SectionHeader(stringResource(R.string.artist_top_songs)) }
            groupItems(state.popular, key = { "top:${it.key}" }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = RowLead.RankedCover(index + 1, Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index) },
                    onLongClick = { actions.showMenu(track) },
                    onSwipeToQueue = { actions.queue(track) },
                    trailing = { RowMoreButton(track.title) { actions.showMenu(track) } },
                )
            }
        }
        if (artist.albums.isNotEmpty()) {
            item(key = "albums") {
                SectionHeader(stringResource(R.string.artist_albums))
                Shelf {
                    items(artist.albums, key = { it.key }) { album ->
                        Tile(
                            title = album.name,
                            onClick = { actions.openAlbum(album) },
                            art = { Cover(Art.of(album.tracks.firstOrNull()?.mediaId), it) },
                            subtitle = yearAndKind(album),
                        )
                    }
                }
            }
        }
        if (state.others.isNotEmpty()) {
            item(key = "allHeader") { SectionHeader(stringResource(R.string.artist_all_songs)) }
            val offset = state.popular.size
            groupItems(state.others, key = { it.key }) { index, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = RowLead.Cover(Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(index + offset) },
                    onLongClick = { actions.showMenu(track) },
                    onSwipeToQueue = { actions.queue(track) },
                    trailing = { RowMoreButton(track.title) { actions.showMenu(track) } },
                )
            }
        }
    }
}

/** An album under its artist: "2013 • Album", or the kind alone. */
@Composable
private fun yearAndKind(album: Album): String {
    val kind = stringResource(if (album.tracks.size == 1) R.string.album_kind_single else R.string.album_kind_album)
    return album.year?.let { stringResource(R.string.artist_album_year_kind, it.toString(), kind) } ?: kind
}

// ── Folder and Favourites ────────────────────────────────────────────────────

/** A folder (frame H): a playlist that cannot be edited, with "Playlist from a folder on this phone". */
@Composable
fun ExpressiveFolder(state: FolderState, actions: FolderActions) {
    val folder = state.folder ?: return GoneState(R.string.collection_playlist_gone, Symbol.QueueMusic, cookie = false, actions.back)
    val tracks = folder.tracks
    val seed = rememberArtColor(tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    CollectionFrame(
        title = folder.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        header = { titleModifier ->
            CollectionHeader(
                title = folder.name,
                titleModifier = titleModifier,
                meta = listOf(stringResource(R.string.folder_subtitle), songsAndDuration(tracks)),
                art = { Mosaic(mosaicIds(tracks).map(Art::Song), it) },
                name = folder.name,
                controls = state.controls,
                controlActions = actions.controls,
            )
        },
    ) {
        item(key = "gap") { Spacer(Modifier.height(12.dp)) }
        songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu, onQueue = actions.queue)
    }
}

/**
 * A genre: the folder's layout, since it too is a set of songs gathered from across the library. The
 * meta line says where the genre comes from. Albums most of whose songs are in it come first as a
 * shelf, then every song. Radio sits in the cluster; add to playlist moves to the bar.
 */
@Composable
fun ExpressiveGenre(state: GenreState, actions: GenreActions) {
    val genre = state.genre
    if (genre == null) {
        // While the list is first built there is nothing to say yet; after that, the genre has gone.
        if (!state.loading) GoneState(R.string.collection_genre_gone, Symbol.QueueMusic, cookie = false, actions.back)
        return
    }
    val tracks = genre.tracks
    val seed = rememberArtColor(tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    CollectionFrame(
        title = genre.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        barActions = {
            ExpressiveIconButton(Symbol.PlaylistAdd, stringResource(R.string.collection_add_to_playlist_cd, genre.name), actions.controls.addToPlaylist)
        },
        header = { titleModifier ->
            CollectionHeader(
                title = genre.name,
                titleModifier = titleModifier,
                meta = listOf(stringResource(R.string.genre_subtitle), songsAndDuration(tracks)),
                art = { Mosaic(mosaicIds(tracks).map(Art::Song), it) },
                name = genre.name,
                controls = state.controls,
                controlActions = actions.controls,
                radio = actions.startRadio,
            )
        },
    ) {
        if (genre.albums.isNotEmpty()) {
            item(key = "albums") {
                SectionHeader(stringResource(R.string.artist_albums))
                Shelf {
                    items(genre.albums, key = { it.key }) { album ->
                        Tile(
                            title = album.name,
                            onClick = { actions.openAlbum(album) },
                            art = { Cover(Art.of(album.tracks.firstOrNull()?.mediaId), it) },
                            subtitle = album.artist,
                        )
                    }
                }
            }
            item(key = "songsHeader") { SectionHeader(stringResource(R.string.library_filter_songs)) }
        } else {
            item(key = "gap") { Spacer(Modifier.height(12.dp)) }
        }
        songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu, onQueue = actions.queue)
    }
}

/**
 * Favourites (frames E, J): no cover of its own, so the header stays on the app scheme with the heart
 * on the tertiary container. Newest first; with none, the cluster is disabled and the empty state shows.
 */
@Composable
fun ExpressiveFavourites(state: LikedState, actions: LikedActions) {
    val title = stringResource(R.string.favourites)
    val tracks = state.tracks
    CollectionFrame(
        title = title,
        seed = Color.Unspecified,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        barActions = {
            // The mock has no place for smart shuffle, so it sits behind a more button as on a playlist.
            if (tracks.isNotEmpty()) {
                Box {
                    ExpressiveIconButton(Symbol.MoreVert, stringResource(R.string.favourites_options_cd), actions.openOverflow)
                    ExpressiveMenu(expanded = state.overflowOpen, onDismiss = actions.dismissOverflow) {
                        SmartShuffleItem(state.controls, actions.toggleSmartShuffle)
                    }
                }
            }
        },
        header = { titleModifier ->
            CollectionHeader(
                title = title,
                titleModifier = titleModifier,
                meta = listOf(songsAndDuration(tracks)),
                art = { FavouritesArt(it) },
                name = title,
                controls = state.controls,
                controlActions = actions.controls,
            )
        },
    ) {
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.favourites_empty_title), stringResource(R.string.favourites_empty_body))
            }
        } else {
            item(key = "gap") { Spacer(Modifier.height(12.dp)) }
            songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu, onQueue = actions.queue)
        }
    }
}

/** "Smart shuffle" in a collection's menu: ticked while on for this collection. */
@Composable
private fun SmartShuffleItem(controls: PlayControlsState, onToggle: () -> Unit) {
    ExpressiveMenuItem(Symbol.ShuffleOn, stringResource(R.string.collection_smart_shuffle), onToggle, checked = controls.smartShuffle == true)
}

private fun LazyListScope.songRows(
    tracks: List<Track>,
    currentKey: String?,
    isPlaying: Boolean,
    onPlay: (Int) -> Unit,
    onMenu: (Track) -> Unit,
    onQueue: (Track) -> Unit,
) {
    groupItems(tracks, key = { it.key }) { index, track ->
        SongRow(
            title = track.title,
            subtitle = track.artist,
            lead = RowLead.Cover(Art.of(track.mediaId)),
            current = currentKey == track.key,
            playing = isPlaying,
            onClick = { onPlay(index) },
            onLongClick = { onMenu(track) },
            onSwipeToQueue = { onQueue(track) },
            trailing = { RowMoreButton(track.title) { onMenu(track) } },
        )
    }
}

// ── Playlist ─────────────────────────────────────────────────────────────────

/**
 * A playlist (frames C, D, I): rename and delete in the bar's overflow, "Add songs" and "Reorder songs"
 * as labelled buttons above the list. Reorder is a mode: the header gives way to a contextual bar and
 * each row's menu button becomes a drag handle. The dialogs and the Add songs sheet are drawn by
 * PlaylistScreen around this.
 */
@Composable
fun ExpressivePlaylist(state: PlaylistState, actions: PlaylistActions) {
    val playlist = state.playlist ?: return GoneState(R.string.collection_playlist_gone, Symbol.QueueMusic, cookie = false, actions.back)
    val tracks = state.tracks
    val seed = rememberArtColor(tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    if (state.reordering) {
        ReorderPlaylist(state, actions, playlist.name, seed)
        return
    }
    CollectionFrame(
        title = playlist.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        barActions = {
            Box {
                ExpressiveIconButton(Symbol.MoreVert, stringResource(R.string.collection_options_cd), actions.openOverflow)
                ExpressiveMenu(expanded = state.overflowOpen, onDismiss = actions.dismissOverflow) {
                    if (tracks.isNotEmpty()) SmartShuffleItem(state.controls, actions.toggleSmartShuffle)
                    ExpressiveMenuItem(Symbol.Edit, stringResource(R.string.playlist_rename), actions.startRenaming)
                    ExpressiveMenuItem(Symbol.Delete, stringResource(R.string.playlist_delete), actions.startDeleting, danger = true)
                }
            }
        },
        header = { titleModifier ->
            CollectionHeader(
                title = playlist.name,
                titleModifier = titleModifier,
                meta = listOf(songsAndDuration(tracks)),
                art = { Mosaic(mosaicIds(tracks).map(Art::Song), it) },
                name = playlist.name,
                controls = state.controls,
                controlActions = actions.controls,
            )
        },
    ) {
        item(key = "tools") {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpressiveButton(
                    stringResource(R.string.playlist_add_songs_cd),
                    onClick = actions.startAdding,
                    // The way out of an empty playlist is the one filled button on the page.
                    variant = if (tracks.isEmpty()) ButtonVariant.Filled else ButtonVariant.Tonal,
                    leading = Symbol.Add,
                    leadingFilled = false,
                )
                ExpressiveButton(
                    stringResource(R.string.playlist_reorder_cd),
                    onClick = actions.toggleReordering,
                    variant = ButtonVariant.Outlined,
                    leading = Symbol.SwapVert,
                    leadingFilled = false,
                    enabled = tracks.size > 1,
                )
            }
        }
        if (tracks.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.playlist_empty_title), stringResource(R.string.playlist_empty_body))
            }
        } else {
            groupItems(tracks, key = { it.key }) { _, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = RowLead.Cover(Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(track) },
                    onLongClick = { actions.showMenu(track) },
                    onSwipeToQueue = { actions.queue(track) },
                    trailing = { RowMoreButton(track.title) { actions.showMenu(track) } },
                )
            }
        }
    }
}

/** Reorder mode (frame D): a contextual bar on the cover's container, then the songs with drag handles. */
@Composable
private fun ReorderPlaylist(state: PlaylistState, actions: PlaylistActions, name: String, seed: Color) {
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
    Column(Modifier.fillMaxSize()) {
        FromArt(seed) {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveIconButton(Symbol.Close, stringResource(R.string.close), actions.toggleReordering)
                    Text(
                        stringResource(R.string.playlist_reorder_cd),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                    ExpressiveIconButton(
                        Symbol.Check,
                        stringResource(R.string.playlist_reorder_done_cd),
                        actions.toggleReordering,
                        variant = IconButtonVariant.Filled,
                        width = IconButtonWidth.Wide,
                    )
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalBottomPadding.current),
        ) {
            item(key = "label") {
                // "Late drive, 12 songs", as frame D labels the list and the Add songs sheet heads itself.
                val n = tracks.size
                val count = pluralStringResource(R.plurals.song_count, n, NumberFormat.getIntegerInstance().format(n))
                ListLabel(stringResource(R.string.add_songs_subtitle, name, count))
            }
            itemsIndexed(rows, key = { _, track -> track.key }) { index, track ->
                ReorderableItem(reorderState, key = track.key) { isDragging ->
                    SongRow(
                        title = track.title,
                        subtitle = track.artist,
                        lead = RowLead.Cover(Art.of(track.mediaId)),
                        modifier = Modifier.liftableCell(index, rows.size, isDragging),
                        current = state.currentKey == track.key,
                        playing = state.isPlaying,
                        lifted = isDragging,
                        onClick = { actions.play(track) },
                        onLongClick = { actions.showMenu(track) },
                        trailing = {
                            RowDragHandle(
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
}

// ── The shared frame ─────────────────────────────────────────────────────────

/**
 * The frame every collection shares: the header as the list's first item, on [seed]'s scheme (the app
 * scheme when [seed] is unspecified), the rest of the list on the app scheme, and the bar over it.
 * [header] gets the modifier to put on its title, which tells the bar when the title has gone under it.
 */
@Composable
private fun CollectionFrame(
    title: String,
    seed: Color,
    controls: PlayControlsState,
    controlActions: PlayControlsActions,
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    barActions: @Composable RowScope.() -> Unit = {},
    header: @Composable (titleModifier: Modifier) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    // The title starts 4dp under the bar, so it has gone under it once the list has moved its height and 4.
    var titleHeight by remember { mutableIntStateOf(Int.MAX_VALUE / 2) }
    val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
    val collapsed by remember(listState, gap) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleHeight + gap }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalBottomPadding.current),
        ) {
            item(key = "header") {
                FromArt(seed) { header(Modifier.onSizeChanged { titleHeight = it.height }) }
            }
            content()
        }
        FromArt(seed) {
            CollectionBar(title, collapsed, controls, controlActions, onBack, barActions)
        }
    }
}

/**
 * The bar over a collection (`.appbar`, `.appbar.scrolled`): back and the actions over the header, then,
 * once the title has scrolled under it, the header's container colour, the title, and play or pause
 * for the collection (frame F), squared off while it plays.
 */
@Composable
private fun CollectionBar(
    title: String,
    collapsed: Boolean,
    controls: PlayControlsState,
    controlActions: PlayControlsActions,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (collapsed) colors.surfaceContainer else colors.surfaceContainer.copy(alpha = 0f),
        ExpressiveMotion.defaultEffects(),
        label = "collectionBar",
    )
    Box(Modifier.fillMaxWidth().background(container).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveIconButton(Symbol.ArrowBack, stringResource(R.string.back), onBack)
            Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                androidx.compose.animation.AnimatedVisibility(collapsed, enter = fadeIn(ExpressiveMotion.defaultEffects()), exit = fadeOut(ExpressiveMotion.defaultEffects())) {
                    NarrowingText(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium))
                }
            }
            AnimatedVisibility(
                collapsed && controls.enabled,
                enter = fadeIn(ExpressiveMotion.defaultEffects()) + scaleIn(ExpressiveMotion.fastSpatial(), initialScale = 0.6f),
                exit = fadeOut(ExpressiveMotion.defaultEffects()) + scaleOut(ExpressiveMotion.defaultSpatial(), targetScale = 0.6f),
            ) {
                PlayButton(controls.isPlaying, controlActions.playOrPause)
            }
            actions()
        }
    }
}

/**
 * The header block (`.chead.tf`): the cover scheme's container with 32dp bottom corners, running under
 * the status bar. The title leads in the emphasised style (45sp for an artist), then an optional
 * byline, the meta lines, and the 208dp cover with the play cluster beside it.
 */
@Composable
private fun CollectionHeader(
    title: String,
    titleModifier: Modifier,
    meta: List<String>,
    art: @Composable (Modifier) -> Unit,
    name: String,
    controls: PlayControlsState,
    controlActions: PlayControlsActions,
    artist: Boolean = false,
    byline: (@Composable () -> Unit)? = null,
    radio: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = ExpressiveShapes.ExtraLargeIncreased, bottomEnd = ExpressiveShapes.ExtraLargeIncreased))
            .background(colors.surfaceContainer)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 68.dp, bottom = 20.dp),
    ) {
        HeaderTitle(title, artist, titleModifier)
        byline?.invoke()
        for (line in meta) {
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp).height(CoverSize),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            art(Modifier.size(CoverSize))
            PlayCluster(name, controls, controlActions, Modifier.weight(1f).fillMaxHeight(), radio)
        }
    }
}

private val CoverSize: Dp = 208.dp

/**
 * The title: one line at full width if it fits, else narrowed to 75 (`.w75`), wrapping to three lines
 * only if even that is too wide. 32/36 emphasised; an artist's name 45/48.
 */
@Composable
private fun HeaderTitle(text: String, artist: Boolean, modifier: Modifier) {
    val type = Expressive.type
    val style = if (artist) {
        type.displayMediumEmphasized.copy(lineHeight = 48.sp, letterSpacing = (-0.8).sp)
    } else {
        type.headlineLargeEmphasized.copy(lineHeight = 36.sp, letterSpacing = (-0.4).sp)
    }
    val measurer = rememberTextMeasurer(cacheSize = 4)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxWidth = constraints.maxWidth
        val width = remember(text, style, maxWidth) {
            listOf(100, 75).firstOrNull { w ->
                !measurer.measure(text, style.narrowed(w), maxLines = 1, constraints = Constraints(maxWidth = maxWidth)).hasVisualOverflow
            } ?: 75
        }
        Text(text, style = style.narrowed(width), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The cluster beside the cover (`.ccluster`, frame K): Play as the large filled button, round until
 * this collection is the one playing, then squared off and reading Pause; under it shuffle, which
 * squares off and fills while it is on for this collection, and add to playlist. All three are
 * disabled for an empty collection. With [radio] (a genre) radio takes add to playlist's place, which
 * moves to the bar.
 */
@Composable
private fun PlayCluster(name: String, state: PlayControlsState, actions: PlayControlsActions, modifier: Modifier, radio: (() -> Unit)? = null) {
    val enabled = state.enabled
    Column(modifier.alpha(if (enabled) 1f else 0.38f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollectionPlayButton(state.isPlaying, actions.playOrPause, enabled, Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpressiveToggleButton(
                Symbol.Shuffle,
                checked = state.shuffleOn,
                onCheckedChange = { actions.shuffle() },
                contentDescription = stringResource(R.string.collection_shuffle_cd, name),
                modifier = Modifier.weight(1f),
                medium = true,
                tonalWhenOff = true,
                enabled = enabled,
            )
            ExpressiveIconButton(
                if (radio != null) Symbol.Sensors else Symbol.PlaylistAdd,
                stringResource(if (radio != null) R.string.collection_radio_cd else R.string.collection_add_to_playlist_cd, name),
                radio ?: actions.addToPlaylist,
                modifier = Modifier.weight(1f),
                variant = IconButtonVariant.Tonal,
                medium = true,
                enabled = enabled,
            )
        }
    }
}

/** `.cplay`: glyph over label, 48dp corners while idle and 28 (the cover's) while this collection plays. */
@Composable
private fun CollectionPlayButton(playing: Boolean, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        if (playing) ExpressiveShapes.ExtraLarge else ExpressiveShapes.ExtraExtraLarge,
        ExpressiveMotion.fastSpatial(),
        label = "cplayCorner",
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        // The cluster fades as a whole when disabled, so the button keeps its colours.
        colors = ButtonDefaults.buttonColors(disabledContainerColor = colors.primary, disabledContentColor = colors.onPrimary),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Crossfade(playing, animationSpec = ExpressiveMotion.fastEffects(), label = "cplayGlyph") { isPlaying ->
                SymbolIcon(if (isPlaying) Symbol.Pause else Symbol.PlayArrow, size = 40.dp, filled = true)
            }
            Text(
                stringResource(if (playing) R.string.pause else R.string.play),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

// ── Missing ──────────────────────────────────────────────────────────────────

/**
 * An album, artist or playlist that has gone (sheet 08 frame E): a placeholder in the shape the item
 * had, the two lines, and Back where the thumb is.
 */
@Composable
private fun GoneState(@StringRes title: Int, symbol: Symbol, cookie: Boolean, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        DetailAppBar(title = "", onBack = onBack)
        Column(
            Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 72.dp + 48.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(if (cookie) CookieShape else RoundedCornerShape(ExpressiveShapes.Large))
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(symbol, size = 48.dp, tint = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(title), style = Expressive.type.headlineSmallEmphasized, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.collection_gone_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            ExpressiveButton(
                stringResource(R.string.back),
                onClick = onBack,
                variant = ButtonVariant.Tonal,
                leading = Symbol.ArrowBack,
                leadingFilled = false,
            )
        }
    }
}

// ── Copy ─────────────────────────────────────────────────────────────────────

/** "6 songs, 26 min", "14 songs, 1 hr 13 min". */
@Composable
internal fun songsAndDuration(tracks: List<Track>): String {
    val n = tracks.size
    val count = pluralStringResource(R.plurals.song_count, n, NumberFormat.getIntegerInstance().format(n))
    val minutes = (tracks.sumOf { it.durationMs } / 60_000).toInt()
    val length = if (minutes >= 60) {
        stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
    return stringResource(R.string.songs_and_duration, count, length)
}
