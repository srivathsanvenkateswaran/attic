package app.attic.player.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryItem
import app.attic.player.ui.model.LibraryKind
import app.attic.player.ui.model.LibraryState
import app.attic.player.ui.model.SongSort
import java.text.NumberFormat

/**
 * Library (sheet 03): the root bar with search, the grid toggle and settings; the four filter chips,
 * pinned under the bar and taking its colour once the page scrolls; then, with no filter, three groups
 * (Favourites, playlists and folders; "Artists"; "Albums") as a list or a three-column grid. Under
 * "Songs" the page is "Shuffle all songs", the count with the sort as a split button, and every song.
 * "New playlist" is an extended FAB above the mini player while playlists are in the list; it shrinks
 * to its icon once the list scrolls.
 */
@Composable
fun ExpressiveLibrary(state: LibraryState, actions: LibraryActions) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val songs = state.filter == LibraryFilter.Downloaded
    val grid = state.grid && !songs
    val scrolled = if (grid) gridState.scrolledFraction() else listState.scrolledFraction()
    val colors = MaterialTheme.colorScheme
    val pinned by animateColorAsState(
        if (scrolled > 0.5f) colors.surfaceContainer else colors.surface.copy(alpha = 0f),
        ExpressiveMotion.defaultEffects(),
        label = "chipsPinned",
    )
    val showFab = state.filter == null || state.filter == LibraryFilter.Playlists
    val bottom = LocalBottomPadding.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            RootAppBar(stringResource(R.string.library_title), scrolled = scrolled) {
                ExpressiveIconButton(Symbol.Search, stringResource(R.string.library_search_cd), actions.openSearch)
                ExpressiveToggleButton(
                    Symbol.GridView,
                    checked = grid,
                    onCheckedChange = { actions.toggleGrid() },
                    contentDescription = stringResource(if (grid) R.string.library_show_list_cd else R.string.library_show_grid_cd),
                    enabled = !songs,
                )
                ExpressiveIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
            }
            Box(Modifier.fillMaxWidth().background(pinned).padding(bottom = 12.dp)) {
                ChipRow(Modifier.horizontalScroll(rememberScrollState())) {
                    LibraryFilter.entries.forEach { option ->
                        ExpressiveFilterChip(option.title(), option == state.filter, { actions.toggleFilter(option) })
                    }
                }
            }
            val end = bottom + if (showFab) 72.dp else 0.dp
            when {
                songs -> SongList(state, actions, listState, end)
                grid -> CollectionGrid(state, actions, gridState, end)
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = end)) {
                    collectionRows(state, actions)
                }
            }
        }
        AnimatedVisibility(
            visible = showFab,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom - 12.dp),
            enter = scaleIn(ExpressiveMotion.fastSpatial()) + fadeIn(ExpressiveMotion.fastEffects()),
            exit = scaleOut(ExpressiveMotion.fastSpatial()) + fadeOut(ExpressiveMotion.fastEffects()),
        ) {
            ExpressiveExtendedFab(
                stringResource(R.string.playlist_picker_new),
                Symbol.Add,
                actions.startCreating,
                expanded = scrolled < 0.5f,
            )
        }
    }
}

@Composable
private fun LibraryFilter.title(): String = stringResource(
    when (this) {
        LibraryFilter.Playlists -> R.string.library_filter_playlists
        LibraryFilter.Albums -> R.string.library_filter_albums
        LibraryFilter.Artists -> R.string.library_filter_artists
        LibraryFilter.Downloaded -> R.string.library_filter_songs
    },
)

@Composable
private fun SongSort.title(): String = stringResource(
    when (this) {
        SongSort.Title -> R.string.library_sort_title
        SongSort.Artist -> R.string.library_sort_artist
        SongSort.Added -> R.string.library_sort_recently_added
    },
)

/** The rows in groups: one group under a filter; with none, playlists and folders, then "Artists", then "Albums". */
private class Section(val label: Int?, val items: List<LibraryItem>)

private fun sections(state: LibraryState): List<Section> {
    if (state.filter != null) return listOf(Section(null, state.items))
    val (artists, rest) = state.items.partition { it.kind == LibraryKind.Artist }
    val (albums, lists) = rest.partition { it.kind == LibraryKind.Album }
    return listOfNotNull(
        Section(null, lists).takeIf { lists.isNotEmpty() },
        Section(R.string.library_filter_artists, artists).takeIf { artists.isNotEmpty() },
        Section(R.string.library_filter_albums, albums).takeIf { albums.isNotEmpty() },
    )
}

private fun LazyListScope.collectionRows(state: LibraryState, actions: LibraryActions) {
    sections(state).forEachIndexed { index, section ->
        if (section.label != null) {
            item(key = "label:${section.label}") { ListLabel(stringResource(section.label)) }
        } else if (index > 0) {
            item(key = "gap:$index") { Box(Modifier.padding(top = ExpressiveShapes.GroupSpacing)) }
        }
        groupItems(section.items, key = { it.id }) { _, item ->
            CollectionRow(
                title = item.displayTitle(),
                subtitle = item.displaySubtitle(),
                lead = { ItemArt(item, size = 56.dp) },
                onClick = { actions.open(item) },
            )
        }
    }
}

@Composable
private fun CollectionGrid(state: LibraryState, actions: LibraryActions, gridState: LazyGridState, bottom: Dp) {
    val sections = sections(state)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        sections.forEach { section ->
            if (section.label != null) {
                item(key = "label:${section.label}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(section.label),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
            }
            items(section.items, key = { it.id }) { item ->
                Tile(
                    title = item.displayTitle(),
                    onClick = { actions.open(item) },
                    art = { m -> ItemArt(item, m) },
                    subtitle = item.displaySubtitle(),
                    width = null,
                    artist = item.kind == LibraryKind.Artist,
                )
            }
        }
    }
}

/** The shape says the kind: the Favourites heart, a mosaic for playlists and folders, a cover, the cookie. */
@Composable
private fun ItemArt(item: LibraryItem, modifier: Modifier = Modifier, size: Dp? = null) {
    when (item.kind) {
        LibraryKind.Favourites -> FavouritesArt(modifier, size)
        LibraryKind.Playlist, LibraryKind.Folder -> Mosaic(item.mediaIds.map(Art::Song), modifier, size)
        LibraryKind.Album -> Cover(Art.of(item.mediaIds.firstOrNull()), modifier, size)
        LibraryKind.Artist -> ArtistCover(Art.of(item.mediaIds.firstOrNull()), modifier, size)
    }
}

@Composable
private fun LibraryItem.displayTitle(): String =
    if (kind == LibraryKind.Favourites) stringResource(R.string.favourites) else title

/** Today's wording, kept (KIT.md): "Playlist, 46 songs", "Artist", "Album, {artist}". */
@Composable
private fun LibraryItem.displaySubtitle(): String = when (kind) {
    LibraryKind.Favourites, LibraryKind.Playlist, LibraryKind.Folder ->
        pluralStringResource(R.plurals.library_playlist_subtitle, songCount, NumberFormat.getIntegerInstance().format(songCount))
    LibraryKind.Artist -> stringResource(R.string.library_artist_subtitle)
    LibraryKind.Album -> stringResource(R.string.library_album_subtitle, artist.orEmpty())
}

@Composable
private fun SongList(
    state: LibraryState,
    actions: LibraryActions,
    listState: LazyListState,
    bottom: Dp,
) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottom)) {
        item(key = "shuffle") {
            ExpressiveButton(
                stringResource(R.string.library_shuffle_all_cd),
                actions.shuffleAll,
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
                size = ButtonSize.Medium,
                leading = Symbol.Shuffle,
            )
        }
        item(key = "sort") {
            Row(
                Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val count = state.songs.size
                Text(
                    pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count)),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                SortButton(state.sort, actions)
            }
        }
        groupItems(state.songs, key = { it.key }) { _, track ->
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

/** The sort as a split button: the label cycles Title, Artist, Recently added; the arrow lists the three. */
@Composable
private fun SortButton(sort: SongSort, actions: LibraryActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        SplitButton(
            text = sort.title(),
            onClick = actions.cycleSort,
            onMenuClick = { open = true },
            menuContentDescription = stringResource(R.string.library_sort_menu_cd),
            leading = Symbol.Sort,
        )
        ExpressiveMenu(expanded = open, onDismiss = { open = false }) {
            SongSort.entries.forEach { option ->
                ExpressiveMenuItem(
                    symbol = if (option == sort) Symbol.Check else null,
                    label = option.title(),
                    onClick = {
                        open = false
                        actions.setSort(option)
                    },
                )
            }
        }
    }
}

/** [scrolledFraction] for a grid. */
@Composable
private fun LazyGridState.scrolledFraction(range: Float = 120f): Float {
    val fraction by remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / range).coerceIn(0f, 1f)
        }
    }
    return fraction
}
