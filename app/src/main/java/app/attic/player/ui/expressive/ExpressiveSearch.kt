package app.attic.player.ui.expressive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.attic.player.R
import androidx.compose.foundation.layout.widthIn
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.adaptive.WideMarginDp
import app.attic.player.ui.adaptive.ReadableWidthDp
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Genre
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.LocalOverlayCovered
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.SearchActions
import app.attic.player.ui.model.SearchResults
import app.attic.player.ui.model.SearchState
import app.attic.player.ui.mosaicIds
import app.attic.player.ui.rememberArtColor
import java.text.NumberFormat
import app.attic.player.ui.model.BrowseTile as BrowseItem

/**
 * Search (sheet 02). At rest: the root bar ("Search" and settings), the search bar, then Browse: the
 * folders as tonal tiles in their own covers' hues, "Genres" in the same tiles, and the biggest albums
 * as a shelf of covers. With
 * focus or a query the bar docks at the top behind a back arrow and the page becomes results: what you
 * open (artists, albums, folders) in one group, the songs you play under "Songs" in the next. Back
 * clears the query and returns to Browse.
 */
@Composable
fun ExpressiveSearch(state: SearchState, actions: SearchActions) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val docked = focused || state.query.isNotEmpty()
    val undock = {
        actions.setQuery("")
        focusManager.clearFocus()
    }
    BackHandler(enabled = docked && !LocalOverlayCovered.current) { undock() }
    val wide = searchWide(LocalWindowLayout.current)

    val browseState = rememberLazyListState()
    Column(Modifier.fillMaxSize().imePadding()) {
        AnimatedVisibility(
            visible = !docked,
            enter = expandVertically(ExpressiveMotion.defaultSpatial()) + fadeIn(ExpressiveMotion.defaultEffects()),
            exit = shrinkVertically(ExpressiveMotion.defaultSpatial()) + fadeOut(ExpressiveMotion.fastEffects()),
        ) {
            RootAppBar(stringResource(R.string.search_title), scrolled = browseState.scrolledFraction(), titleInset = if (wide != null) WideExtraMargin else 0.dp) {
                ExpressiveIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
            }
        }
        AnimatedVisibility(
            visible = docked,
            enter = expandVertically(ExpressiveMotion.defaultSpatial()),
            exit = shrinkVertically(ExpressiveMotion.defaultSpatial()),
        ) {
            Spacer(Modifier.statusBarsPadding().height(8.dp))
        }
        SearchField(
            query = state.query,
            onQueryChange = actions.setQuery,
            placeholder = stringResource(R.string.search_placeholder),
            modifier = (if (wide != null) Modifier.padding(start = WideExtraMargin).widthIn(max = ReadableWidthDp.dp + 32.dp) else Modifier)
                .padding(horizontal = 16.dp).onFocusChanged { focused = it.isFocused },
            onSearch = { keyboard?.hide() },
            onBack = if (docked) undock else null,
        )
        val results = state.results
        when {
            !docked -> Browse(state.tiles, state.genres, state.moods, browseState, actions, wide)
            results == null -> Unit
            results.isEmpty -> EmptyState(
                title = stringResource(R.string.search_no_results_title, state.query),
                body = stringResource(R.string.search_no_results_body),
                symbol = Symbol.SearchOff,
            )
            else -> Results(results, state, actions, wide)
        }
    }
}

@Composable
private fun Browse(tiles: List<BrowseItem>, genres: List<BrowseItem>, moods: List<BrowseItem>, listState: LazyListState, actions: SearchActions, wide: SearchWide? = null) {
    val folders = tiles.filter { it.destination is Destination.Folder }
    val albums = tiles.filter { it.destination !is Destination.Folder }
    val side = if (wide != null) WideExtraMargin else 0.dp
    val tileColumns = wide?.browseColumns ?: 2
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = side, end = side, bottom = LocalBottomPadding.current),
    ) {
        item(key = "browse") { SectionHeader(stringResource(R.string.search_browse)) }
        tonalTiles(folders, "folders", actions, tileColumns)
        // Moods and genres are other ways into the same songs, so they share the folders' tile and sit under them.
        if (moods.isNotEmpty()) {
            item(key = "moodsLabel") { SmallLabel(stringResource(R.string.search_moods)) }
            tonalTiles(moods, "moods", actions, tileColumns)
        }
        if (genres.isNotEmpty()) {
            item(key = "genresLabel") { SmallLabel(stringResource(R.string.search_genres)) }
            tonalTiles(genres, "genres", actions, tileColumns)
        }
        if (albums.isNotEmpty()) {
            item(key = "albumsLabel") { SmallLabel(stringResource(R.string.home_albums)) }
            item(key = "albums") {
                Shelf(if (wide != null) Modifier.bleed(end = side) else Modifier) {
                    items(albums, key = { it.destination.toString() }) { tile ->
                        Tile(
                            title = tile.title,
                            onClick = { actions.open(tile.destination) },
                            art = { Cover(Art.of(tile.cover), it) },
                            subtitle = tile.artist,
                            width = 120.dp,
                        )
                    }
                }
            }
        }
    }
}

/** Folder and genre tiles two across: tonal containers in their own covers' hues, with the mosaic. */
private fun LazyListScope.tonalTiles(tiles: List<BrowseItem>, section: String, actions: SearchActions, columns: Int = 2) {
    items(tiles.chunked(columns), key = { row -> "$section:${row.first().destination}" }) { row ->
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (column in 0 until columns) {
                val tile = row.getOrNull(column)
                if (tile == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    BrowseTile(
                        title = tile.title,
                        seed = rememberArtColor(tile.cover, fallback = Color.Unspecified),
                        onClick = { actions.open(tile.destination) },
                        modifier = Modifier.weight(1f),
                        subtitle = songCount(tile.songCount),
                    ) { m -> Mosaic(tile.mosaicIds.map(Art::Song), m, shape = RoundedCornerShape(ExpressiveShapes.Large)) }
                }
            }
        }
    }
}

/** What a result opens rather than plays. Its lead's shape says the kind: cookie, cover, mosaic. */
@Immutable
private sealed interface Openable {
    val key: String

    data class OfArtist(val artist: Artist) : Openable {
        override val key get() = "artist:${artist.key}"
    }

    data class OfAlbum(val album: Album) : Openable {
        override val key get() = "album:${album.key}"
    }

    data class OfFolder(val folder: FolderPlaylist) : Openable {
        override val key get() = "folder:${folder.name}"
    }

    data class OfGenre(val genre: Genre) : Openable {
        override val key get() = "genre:${genre.key}"
    }

    data class OfMood(val tile: BrowseItem) : Openable {
        override val key get() = "mood:${tile.destination}"
    }
}

@Composable
private fun Results(results: SearchResults, state: SearchState, actions: SearchActions, wide: SearchWide? = null) {
    val side = if (wide != null) WideExtraMargin else 0.dp
    val columns = wide?.rowColumns ?: 1
    val openables = remember(results) {
        results.artists.map(Openable::OfArtist) + results.albums.map(Openable::OfAlbum) + results.folders.map(Openable::OfFolder) +
            results.moods.map(Openable::OfMood) + results.genres.map(Openable::OfGenre)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = side, end = side, top = 12.dp, bottom = LocalBottomPadding.current),
    ) {
        columnGroupItems(openables, columns, key = { it.key }) { _, hit ->
            when (hit) {
                is Openable.OfArtist -> CollectionRow(
                    title = hit.artist.name,
                    subtitle = stringResource(R.string.search_result_artist),
                    lead = { ArtistCover(Art.of(hit.artist.tracks.firstOrNull()?.mediaId), size = 56.dp) },
                    onClick = { actions.open(Destination.Artist(hit.artist.key)) },
                )
                is Openable.OfAlbum -> CollectionRow(
                    title = hit.album.name,
                    subtitle = stringResource(R.string.search_result_album, hit.album.artist),
                    lead = { Cover(Art.of(hit.album.tracks.firstOrNull()?.mediaId), size = 56.dp) },
                    onClick = { actions.open(Destination.Album(hit.album.key)) },
                )
                is Openable.OfFolder -> CollectionRow(
                    title = hit.folder.name,
                    subtitle = stringResource(R.string.search_result_playlist),
                    lead = { Mosaic(mosaicIds(hit.folder.tracks).map(Art::Song), size = 56.dp) },
                    onClick = { actions.open(Destination.Folder(hit.folder.name)) },
                )
                is Openable.OfGenre -> CollectionRow(
                    title = hit.genre.name,
                    subtitle = stringResource(R.string.search_result_genre),
                    lead = { Mosaic(mosaicIds(hit.genre.tracks).map(Art::Song), size = 56.dp) },
                    onClick = { actions.open(Destination.Genre(hit.genre.key)) },
                )
                is Openable.OfMood -> CollectionRow(
                    title = hit.tile.title,
                    subtitle = stringResource(R.string.search_result_mood),
                    lead = { Mosaic(hit.tile.mosaicIds.map(Art::Song), size = 56.dp) },
                    onClick = { actions.open(hit.tile.destination) },
                )
            }
        }
        if (results.tracks.isNotEmpty()) {
            item(key = "songsLabel") {
                ListLabel(stringResource(R.string.library_filter_songs))
            }
            columnGroupItems(results.tracks, columns, key = { "track:${it.key}" }) { _, track ->
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

/** The small label over Browse's album shelf (sheet 02 `.blabel`): 20dp in, on-surface-variant. */
@Composable
private fun SmallLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun songCount(count: Int): String =
    pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count))

/**
 * Search on a medium or expanded window (SPEC.md, Search): the 24dp margins, the field at most 720dp
 * wide, Browse's tiles in cells at least 180dp wide, results in columns at least 360dp wide. Null on a phone.
 */
private class SearchWide(val rowColumns: Int, val browseColumns: Int)

private fun searchWide(window: WindowLayout): SearchWide? {
    if (!window.wide) return null
    val width = window.contentWidthDp
    return SearchWide(rowColumns = rowColumns(width), browseColumns = adaptiveColumns(width - 2 * WideMarginDp, 180f, 8f))
}
