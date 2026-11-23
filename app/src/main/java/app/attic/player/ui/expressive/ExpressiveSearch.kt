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
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
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
 * folders as tonal tiles in their own covers' hues and the biggest albums as a shelf of covers. With
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

    val browseState = rememberLazyListState()
    Column(Modifier.fillMaxSize().imePadding()) {
        AnimatedVisibility(
            visible = !docked,
            enter = expandVertically(ExpressiveMotion.defaultSpatial()) + fadeIn(ExpressiveMotion.defaultEffects()),
            exit = shrinkVertically(ExpressiveMotion.defaultSpatial()) + fadeOut(ExpressiveMotion.fastEffects()),
        ) {
            RootAppBar(stringResource(R.string.search_title), scrolled = browseState.scrolledFraction()) {
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
            modifier = Modifier.padding(horizontal = 16.dp).onFocusChanged { focused = it.isFocused },
            onSearch = { keyboard?.hide() },
            onBack = if (docked) undock else null,
        )
        val results = state.results
        when {
            !docked -> Browse(state.tiles, browseState, actions)
            results == null -> Unit
            results.isEmpty -> EmptyState(
                title = stringResource(R.string.search_no_results_title, state.query),
                body = stringResource(R.string.search_no_results_body),
                symbol = Symbol.SearchOff,
            )
            else -> Results(results, state, actions)
        }
    }
}

@Composable
private fun Browse(tiles: List<BrowseItem>, listState: LazyListState, actions: SearchActions) {
    val folders = tiles.filter { it.destination is Destination.Folder }
    val albums = tiles.filter { it.destination !is Destination.Folder }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomPadding.current),
    ) {
        item(key = "browse") { SectionHeader(stringResource(R.string.search_browse)) }
        items(folders.chunked(2), key = { row -> "folders:${row.first().title}" }) { row ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (column in 0 until 2) {
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
        if (albums.isNotEmpty()) {
            item(key = "albumsLabel") { SmallLabel(stringResource(R.string.home_albums)) }
            item(key = "albums") {
                Shelf {
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
}

@Composable
private fun Results(results: SearchResults, state: SearchState, actions: SearchActions) {
    val openables = remember(results) {
        results.artists.map(Openable::OfArtist) + results.albums.map(Openable::OfAlbum) + results.folders.map(Openable::OfFolder)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = LocalBottomPadding.current),
    ) {
        groupItems(openables, key = { it.key }) { _, hit ->
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
            }
        }
        if (results.tracks.isNotEmpty()) {
            item(key = "songsLabel") {
                ListLabel(stringResource(R.string.library_filter_songs))
            }
            groupItems(results.tracks, key = { "track:${it.key}" }) { _, track ->
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    lead = RowLead.Cover(Art.of(track.mediaId)),
                    current = state.currentKey == track.key,
                    playing = state.isPlaying,
                    onClick = { actions.play(track) },
                    onLongClick = { actions.showMenu(track) },
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
