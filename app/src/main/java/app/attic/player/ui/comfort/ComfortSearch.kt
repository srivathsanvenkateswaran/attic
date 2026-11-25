package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.BrowseTile as BrowseItem
import app.attic.player.ui.model.Destination
import app.attic.player.ui.model.SearchActions
import app.attic.player.ui.model.SearchResults
import app.attic.player.ui.model.SearchState
import app.attic.player.ui.mosaicIds

/** The field's row under the hero (`.findrow`) and the pinned field under the bar (`.pin`): 2 + 50 + 12. */
private val FieldRow = 50.dp
private val PinHeight = 64.dp

/** The panel the field rides in on the keyboard (`.typebar`): 12 above the field, 12 below it. */
private val TypebarHeight = 74.dp

/**
 * Search (oneui/02-search.html). At rest, with an empty field: the bare title, then the field and the
 * settings gear as the row that closes the viewing area, then Browse as neutral cards. With a query the
 * title is the bar and the field pins under it; matching artists, folders and albums come first as a
 * shelf, then "Songs" as one card. While the keyboard is open the field leaves the top and rides on
 * the keyboard in a panel; the bar stays where it is.
 *
 * The field is one composable throughout, moved by its modifiers, so it keeps focus and the keyboard
 * as it travels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComfortSearch(state: SearchState, actions: SearchActions) {
    val colors = Comfort.colors
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val typing = WindowInsets.isImeVisible
    val browsing = state.query.isEmpty() && !typing
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val barBottom = statusTop + BarHeight
    val dock = LocalBottomPadding.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val bottom = if (typing) max(dock, imeBottom + TypebarHeight) else dock

    val browseState = rememberLazyListState()
    val resultsState = rememberLazyListState()
    val collapsed by rememberCollapsed(browseState)
    // Where the field's row is in the browse list, while it is on screen.
    val rowTop by remember(browseState) {
        derivedStateOf { browseState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == FIND_ROW }?.offset }
    }
    val pinnedTop = with(density) { (barBottom + 2.dp).roundToPx() }
    // Before the list's first layout the row is where the hero leaves it.
    val restingTop = with(density) { (statusTop + HeroHeight).roundToPx() }
    val fieldTop = rowTop ?: if (browseState.firstVisibleItemIndex <= 1) restingTop else pinnedTop
    val fieldPinned = !browsing || fieldTop < pinnedTop
    val showBar = !browsing || collapsed || fieldPinned

    Box(Modifier.fillMaxSize()) {
        when {
            browsing -> Browse(state, actions, browseState, bottom)
            else -> {
                val top = barBottom + if (typing) 0.dp else PinHeight
                val results = state.results
                when {
                    results == null -> Browse(state, actions, resultsState, bottom, top = top)
                    results.isEmpty -> EmptyState(
                        title = stringResource(R.string.search_no_results_title, state.query),
                        body = stringResource(R.string.search_no_results_body),
                        symbol = Symbol.Search,
                        fill = true,
                        modifier = Modifier.padding(top = top, bottom = if (typing) bottom else dock),
                    )
                    else -> Results(results, state, actions, resultsState, top, bottom)
                }
            }
        }

        // The bar, and under it the ground the pinned field sits on.
        if (showBar) {
            Column(Modifier.fillMaxWidth().background(colors.ground)) {
                Bar(stringResource(R.string.search_title), lift = typing) {
                    ComfortIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
                }
                if (!typing && fieldPinned) {
                    Spacer(Modifier.height(PinHeight).liftedUnderBar(colors.ground, colors.line))
                }
            }
        }

        // The field: in the browse list's row, pinned under the bar, or on the keyboard.
        val fieldModifier = when {
            typing -> Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime)
                .fillMaxWidth()
                .floatingShadow(ComfortShapes.FloatingTop)
                .clip(ComfortShapes.FloatingTop)
                .background(colors.card)
                .padding(top = 12.dp, bottom = 12.dp)
            fieldPinned -> Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(0, pinnedTop) }
                .fillMaxWidth()
            else -> Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(0, fieldTop) }
                .fillMaxWidth()
                .padding(end = 40.dp)
        }
        Box(fieldModifier) {
            CompositionLocalProvider(LocalComfortContainer provides if (typing) ComfortContainer.Card else ComfortContainer.Ground) {
                SearchField(
                    query = state.query,
                    onQueryChange = actions.setQuery,
                    placeholder = stringResource(R.string.search_placeholder),
                    onSearch = { keyboard?.hide() },
                )
            }
        }
    }
}

private const val FIND_ROW = "findRow"

/**
 * Browse: every folder, then the biggest albums, as neutral cards two across, with the mosaic or the
 * cover upright in the corner; then "Genres" in the same cards. At rest it opens with the hero and the field's row; [top] is set when it
 * shows under the bar instead (the keyboard is up with nothing typed).
 */
@Composable
private fun Browse(state: SearchState, actions: SearchActions, listState: LazyListState, bottom: Dp, top: Dp? = null) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top ?: 0.dp, bottom = bottom + 8.dp),
    ) {
        if (top == null) {
            item(key = "hero") { Hero(stringResource(R.string.search_title)) }
            item(key = FIND_ROW) {
                // The field itself is drawn over this row by the screen; the row holds the gear.
                Row(Modifier.fillMaxWidth().height(FieldRow).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    ComfortIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
                }
            }
        }
        item(key = "browseLabel") { Label(stringResource(R.string.search_browse)) }
        browseCards(state.tiles, "browse", actions)
        if (state.moods.isNotEmpty()) {
            item(key = "moodsLabel") { Label(stringResource(R.string.search_moods)) }
            browseCards(state.moods, "moods", actions)
        }
        if (state.genres.isNotEmpty()) {
            item(key = "genresLabel") { Label(stringResource(R.string.search_genres)) }
            browseCards(state.genres, "genres", actions)
        }
    }
}

private fun LazyListScope.browseCards(tiles: List<BrowseItem>, section: String, actions: SearchActions) {
    items(tiles.chunked(2), key = { row -> "$section:${row.first().destination}" }) { row ->
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (column in 0 until 2) {
                val tile = row.getOrNull(column)
                if (tile == null) Spacer(Modifier.weight(1f)) else BrowseCard(tile, actions, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BrowseCard(tile: BrowseItem, actions: SearchActions, modifier: Modifier) {
    // Folders and genres are gatherings of songs: a mosaic and a count. An album is its cover and artist.
    val gathered = tile.destination is Destination.Folder || tile.destination is Destination.Genre || tile.destination is Destination.Mood
    BrowseTile(
        title = tile.title,
        onClick = { actions.open(tile.destination) },
        art = { m -> if (gathered) Mosaic(tile.mosaicIds.map(Art::Song), m) else Cover(Art.of(tile.cover), m) },
        modifier = modifier,
        subtitle = if (gathered) songCount(tile.songCount) else tile.artist,
    )
}

/** Matching artists, folders and albums as a shelf of objects with their type under each name, then the songs. */
@Composable
private fun Results(results: SearchResults, state: SearchState, actions: SearchActions, listState: LazyListState, top: Dp, bottom: Dp) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top, bottom = bottom + 8.dp),
    ) {
        if (results.artists.isNotEmpty() || results.folders.isNotEmpty() || results.albums.isNotEmpty() || results.genres.isNotEmpty() || results.moods.isNotEmpty()) {
            item(key = "found") {
                Shelf(Modifier.padding(top = 16.dp)) {
                    items(results.artists, key = { "artist:${it.key}" }) { artist ->
                        ResultTile(
                            artist.name,
                            stringResource(R.string.search_result_artist),
                            { actions.open(Destination.Artist(artist.key)) },
                        ) { m -> ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), m) }
                    }
                    items(results.folders, key = { "folder:${it.name}" }) { folder ->
                        ResultTile(
                            folder.name,
                            stringResource(R.string.search_result_playlist),
                            { actions.open(Destination.Folder(folder.name)) },
                        ) { m -> Mosaic(mosaicIds(folder.tracks).map(Art::Song), m) }
                    }
                    items(results.albums, key = { "album:${it.key}" }) { album ->
                        ResultTile(
                            album.name,
                            stringResource(R.string.search_result_album, album.artist),
                            { actions.open(Destination.Album(album.key)) },
                        ) { m -> Cover(Art.of(album.tracks.firstOrNull()?.mediaId), m) }
                    }
                    items(results.moods, key = { "mood:${it.destination}" }) { mood ->
                        ResultTile(
                            mood.title,
                            stringResource(R.string.search_result_mood),
                            { actions.open(mood.destination) },
                        ) { m -> Mosaic(mood.mosaicIds.map(Art::Song), m) }
                    }
                    items(results.genres, key = { "genre:${it.key}" }) { genre ->
                        ResultTile(
                            genre.name,
                            stringResource(R.string.search_result_genre),
                            { actions.open(Destination.Genre(genre.key)) },
                        ) { m -> Mosaic(mosaicIds(genre.tracks).map(Art::Song), m) }
                    }
                }
            }
        }
        if (results.tracks.isNotEmpty()) {
            // "Songs" is the library filter's word, shared.
            item(key = "songsLabel") { Label(stringResource(R.string.library_filter_songs)) }
            groupItems(results.tracks, key = { "track:${it.key}" }) { _, track ->
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
}

@Composable
private fun ResultTile(title: String, type: String, onClick: () -> Unit, art: @Composable (Modifier) -> Unit) {
    Tile(
        title = title,
        onClick = onClick,
        art = art,
        subtitle = type,
        width = 104.dp,
        twoLines = true,
        subtitleLines = 2,
    )
}
