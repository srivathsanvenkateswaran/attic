package app.attic.player.ui.comfort

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Track
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.LibraryActions
import app.attic.player.ui.model.LibraryFilter
import app.attic.player.ui.model.LibraryItem
import app.attic.player.ui.model.LibraryKind
import app.attic.player.ui.model.LibraryState
import app.attic.player.ui.model.SongSort
import java.text.NumberFormat
import kotlinx.coroutines.launch

/**
 * Library (oneui/03-library.html): the bare title; search, create playlist and settings as the tools
 * row; the four chips. With no filter the list runs as three labelled cards, "Playlists" (Favourites,
 * your playlists, folders), "Artists" and "Albums", or as three grids; the view toggle sits at the
 * right of the first label. Under "Songs" the label counts the songs, the sort opens a menu of the
 * three orders, and "Shuffle all songs" is the first row of the card. Scrolled, the title becomes the
 * bar with the three actions and the chips pin under it; sorted by title, an index runs down the right.
 */
@Composable
fun ComfortLibrary(state: LibraryState, actions: LibraryActions) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val songsMode = state.filter == LibraryFilter.Downloaded
    val tools: @Composable () -> Unit = {
        ComfortIconButton(Symbol.Search, stringResource(R.string.library_search_cd), actions.openSearch)
        ComfortIconButton(Symbol.Add, stringResource(R.string.library_create_playlist_cd), actions.startCreating)
        ComfortIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
    }
    val chips: @Composable () -> Unit = {
        ChipRow {
            LibraryFilter.entries.forEach { option ->
                FilterChip(option.title(), option == state.filter, { actions.toggleFilter(option) })
            }
        }
    }

    // The index: only for every song in title order, and only once the title has collapsed.
    val collapsed by rememberCollapsed(listState)
    val showIndex = songsMode && state.sort == SongSort.Title && collapsed && state.songs.size > INDEX_MIN_SONGS
    val letters = remember(state.songs, showIndex) { if (showIndex) IndexLetters else emptyList() }
    val firstOfLetter = remember(state.songs) { firstIndexByLetter(state.songs) }
    val currentLetter by remember(listState, state.songs) {
        derivedStateOf {
            val song = (listState.firstVisibleItemIndex - SONG_START).coerceIn(0, (state.songs.size - 1).coerceAtLeast(0))
            state.songs.getOrNull(song)?.let { IndexLetters.indexOf(letterOf(it.title)) } ?: -1
        }
    }
    val indexSpace by animateDpAsState(if (showIndex) 28.dp else 0.dp, tween(200), label = "indexSpace")
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }

    RootPage(
        title = stringResource(R.string.library_title),
        listState = listState,
        pinIndex = CHIPS_INDEX,
        pinned = {
            Box(Modifier.liftedUnderBar(Comfort.colors.ground, Comfort.colors.line).padding(bottom = 10.dp)) { chips() }
        },
        barActions = { tools() },
        overlay = {
            AnimatedVisibility(
                showIndex,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    IndexScrubberWithPop(
                        letters = letters,
                        current = currentLetter,
                        onSelect = { letter ->
                            firstOfLetter[IndexLetters[letter]]?.let { song ->
                                scope.launch { listState.scrollToItem(SONG_START + song) }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        top = statusTop + BarHeight + 58.dp + 16.dp,
                    )
                }
            }
        },
    ) {
        item(key = "hero") { Hero(stringResource(R.string.library_title)) }
        item(key = "tools") { Tools { tools() } }
        item(key = "chips") { chips() }
        when {
            songsMode -> songs(state, actions, indexSpace)
            state.grid -> grids(state, actions)
            else -> lists(state, actions)
        }
    }
}

/** The list's first three items: hero, tools, chips. The songs follow the label and the shuffle row. */
private const val CHIPS_INDEX = 2
private const val SONG_START = 5
private const val INDEX_MIN_SONGS = 30

private val IndexLetters = ('A'..'Z').map { it.toString() } + "#"

private fun letterOf(title: String): String {
    val first = title.trimStart().firstOrNull()?.uppercaseChar() ?: return "#"
    return if (first in 'A'..'Z') first.toString() else "#"
}

/** For each index letter, the first song (in the list's order) whose title starts with it, or a later letter. */
private fun firstIndexByLetter(songs: List<Track>): Map<String, Int> {
    val first = HashMap<String, Int>()
    songs.forEachIndexed { i, track -> first.putIfAbsent(letterOf(track.title), i) }
    val result = HashMap<String, Int>()
    var next: Int? = null
    for (letter in IndexLetters.reversed()) {
        first[letter]?.let { next = it }
        next?.let { result[letter] = it }
    }
    return result
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

/** A run of items under one label: with no filter, "Playlists", "Artists" and "Albums"; with one, the filter's name. */
private class Section(val label: Int, val items: List<LibraryItem>)

private fun sections(state: LibraryState): List<Section> {
    when (state.filter) {
        LibraryFilter.Playlists -> return listOf(Section(R.string.library_filter_playlists, state.items))
        LibraryFilter.Albums -> return listOf(Section(R.string.library_filter_albums, state.items))
        LibraryFilter.Artists -> return listOf(Section(R.string.library_filter_artists, state.items))
        else -> Unit
    }
    val (artists, rest) = state.items.partition { it.kind == LibraryKind.Artist }
    val (albums, lists) = rest.partition { it.kind == LibraryKind.Album }
    return listOfNotNull(
        Section(R.string.library_filter_playlists, lists).takeIf { lists.isNotEmpty() },
        Section(R.string.library_filter_artists, artists).takeIf { artists.isNotEmpty() },
        Section(R.string.library_filter_albums, albums).takeIf { albums.isNotEmpty() },
    )
}

private fun LazyListScope.lists(state: LibraryState, actions: LibraryActions) {
    sections(state).forEachIndexed { index, section ->
        item(key = "label:${section.label}") {
            SectionLabel(section.label, toggle = if (index == 0) ({ ViewToggle(state.grid, actions.toggleGrid) }) else null)
        }
        groupItems(section.items, key = { it.id }) { _, item ->
            CollectionRow(
                title = item.displayTitle(),
                subtitle = item.subtitle(),
                lead = { m -> ItemArt(item, m) },
                onClick = { actions.open(item) },
            )
        }
    }
}

private fun LazyListScope.grids(state: LibraryState, actions: LibraryActions) {
    sections(state).forEachIndexed { index, section ->
        item(key = "label:${section.label}") {
            SectionLabel(section.label, toggle = if (index == 0) ({ ViewToggle(state.grid, actions.toggleGrid) }) else null)
        }
        // A grid in rows of three, so a long library stays lazy (`.grid3`: 14 across, 20 down, 24 in).
        val rows = section.items.chunked(3)
        itemsIndexed(rows, key = { _, row -> "grid:${row.first().id}" }) { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = if (rowIndex < rows.size - 1) 20.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (column in 0 until 3) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        row.getOrNull(column)?.let { item ->
                            val artist = item.kind == LibraryKind.Artist
                            Tile(
                                title = item.displayTitle(),
                                onClick = { actions.open(item) },
                                art = { m -> ItemArt(item, m) },
                                subtitle = item.subtitle(),
                                width = null,
                                centered = artist,
                                twoLines = artist,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Songs": the count as the label with the sort at its right, then one card that starts with
 * "Shuffle all songs" and holds every song. [indexSpace] makes room for the index.
 */
private fun LazyListScope.songs(state: LibraryState, actions: LibraryActions, indexSpace: androidx.compose.ui.unit.Dp) {
    item(key = "songsLabel") {
        val count = state.songs.size
        Label(
            pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count)),
            end = { LabelControl { SortControl(state.sort, actions) } },
        )
    }
    val count = state.songs.size + 1
    item(key = "shuffleAll") {
        GroupCell(0, count, Modifier.padding(end = indexSpace)) {
            CollectionRow(
                title = stringResource(R.string.library_shuffle_all_cd),
                subtitle = null,
                lead = { m -> IconBox(Symbol.Shuffle, m) },
                onClick = actions.shuffleAll,
                size = RowSize.Small,
            )
        }
    }
    itemsIndexed(state.songs, key = { _, track -> track.key }, contentType = { _, _ -> "group-row" }) { index, track ->
        GroupCell(index + 1, count, Modifier.padding(end = indexSpace)) {
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = SongLead.Cover(Art.of(track.mediaId)),
                current = track.key == state.currentKey,
                playing = state.isPlaying,
                onClick = { actions.play(track) },
                onLongClick = { actions.showMenu(track) },
                onSwipeToQueue = { actions.queue(track) },
                trailing = { SongMoreButton(track.title, onClick = { actions.showMenu(track) }) },
            )
        }
    }
}

@Composable
private fun SectionLabel(label: Int, toggle: (@Composable () -> Unit)?) {
    Label(stringResource(label), end = toggle?.let { { LabelControl(it) } })
}

/**
 * A control at the right end of a label, acting on the list under it (`.lctl`): a 48dp target centred
 * on the label's text line, 14dp from the edge, that does not make the label taller.
 */
@Composable
private fun LabelControl(content: @Composable () -> Unit) {
    Box(
        Modifier.height(18.dp).wrapContentHeight(unbounded = true).offset(x = 14.dp),
        contentAlignment = Alignment.CenterEnd,
    ) { content() }
}

@Composable
private fun ViewToggle(grid: Boolean, onToggle: () -> Unit) {
    ComfortIconButton(
        if (grid) Symbol.ViewList else Symbol.GridView,
        stringResource(if (grid) R.string.library_show_list_cd else R.string.library_show_grid_cd),
        onToggle,
        tint = Comfort.colors.on,
        iconSize = 22.dp,
    )
}

/** The sort (`.lctl.sort`): the current order and `expand_more`; opens a menu of the three with a tick on the current one. */
@Composable
private fun SortControl(sort: SongSort, actions: LibraryActions) {
    val colors = Comfort.colors
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.library_sort_menu_cd)
    Box {
        Row(
            Modifier
                .height(48.dp)
                .clip(ComfortShapes.Pill)
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = description }
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(sort.title(), style = onest(14f, 18f, 600), color = colors.on)
            ComfortIcon(Symbol.ExpandMore, size = 22.dp, tint = colors.on)
        }
        ComfortMenu(expanded = open, onDismiss = { open = false }, offset = DpOffset(0.dp, 4.dp)) {
            SongSort.entries.forEach { option ->
                SortRow(option.title(), option == sort) {
                    open = false
                    actions.setSort(option)
                }
            }
        }
    }
}

/** A menu row with the tick at its end on the current choice (the sheet's `.menu-row.on .ms.end`). */
@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Comfort.colors
    Row(
        Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = onest(16f, 22f, if (selected) 600 else 400),
            color = if (selected) colors.accentInk else colors.on,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.weight(1f))
        if (selected) ComfortIcon(Symbol.Check, tint = colors.accentInk, modifier = Modifier.padding(start = 28.dp))
    }
}

/** The kind's picture: the Favourites heart box, a mosaic for playlists and folders, the cover, the artist's softer shape. */
@Composable
private fun ItemArt(item: LibraryItem, modifier: Modifier) {
    when (item.kind) {
        LibraryKind.Favourites -> FavouritesArt(modifier)
        LibraryKind.Playlist, LibraryKind.Folder -> Mosaic(item.mediaIds.map(Art::Song), modifier)
        LibraryKind.Album -> Cover(Art.of(item.mediaIds.firstOrNull()), modifier)
        LibraryKind.Artist -> ArtistCover(Art.of(item.mediaIds.firstOrNull()), modifier)
    }
}

@Composable
private fun LibraryItem.displayTitle(): String =
    if (kind == LibraryKind.Favourites) stringResource(R.string.favourites) else title

/** The card's label names the kind, so the second line is only the count, or the album's artist; an artist has none. */
@Composable
private fun LibraryItem.subtitle(): String? = when (kind) {
    LibraryKind.Favourites, LibraryKind.Playlist, LibraryKind.Folder -> songCount(songCount)
    LibraryKind.Artist -> null
    LibraryKind.Album -> artist
}
