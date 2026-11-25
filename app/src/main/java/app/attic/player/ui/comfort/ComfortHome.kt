package app.attic.player.ui.comfort

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Track
import app.attic.player.ui.adaptive.WindowLayout
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.adaptiveColumns
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.HomeActions
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeShelf
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.Shortcut
import app.attic.player.ui.model.WrappedTeaser
import app.attic.player.ui.mosaicIds
import java.text.NumberFormat

/**
 * Home (oneui/01-home.html): the centred title with the mark in an empty upper third, then the
 * sub-tabs and settings closing the viewing area. Under "All": the six shortcuts as a tray in one
 * card, the Recap entry row, Instant mixes paging sideways three songs to a card, the taste mixes,
 * Recently played, the shelves from your listening (On repeat, Haven't heard in a while, Newly
 * added), Start listening as a card of rows, folders as a four-across tray, Albums, the "More from" card and
 * Top artists. Another sub-tab leaves its one section as a three-column grid. Scrolled, the title
 * becomes a bar with settings and the sub-tabs pin under it.
 */
@Composable
fun ComfortHome(state: HomeState, actions: HomeActions) {
    val window = LocalWindowLayout.current
    // On a tablet the side dock carries the mark, and a window under 840dp tall takes the short hero.
    val wide = if (window.widthClass == WidthClass.Compact) null else HomeColumns.of(window)
    val listState = rememberLazyListState()
    val title = stringResource(R.string.tab_home)
    val settings = @Composable {
        ComfortIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
    }
    val filters = listOf(
        stringResource(R.string.home_filter_all),
        stringResource(R.string.home_filter_albums),
        stringResource(R.string.home_filter_artists),
        stringResource(R.string.home_filter_folders),
    )
    RootPage(
        title = title,
        listState = listState,
        heroHeight = if (wide?.shortHero == true) ShortHeroHeight else HeroHeight,
        pinIndex = if (state.isEmpty) -1 else 1,
        pinned = {
            SubTabs(filters, state.filter.ordinal, { actions.setFilter(HomeFilter.entries[it]) }, lift = true)
        },
        barActions = { settings() },
    ) {
        item(key = "hero") { Hero(title, mark = wide == null, short = wide?.shortHero == true) }
        if (state.isEmpty) {
            // Settings stays (it is on every tab root); there is nothing to filter.
            item(key = "tools") { Tools { settings() } }
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    symbol = Symbol.FolderOpen,
                )
            }
            return@RootPage
        }
        item(key = "tools") {
            Tools(leading = { SubTabs(filters, state.filter.ordinal, { actions.setFilter(HomeFilter.entries[it]) }) }) {
                settings()
            }
        }
        when (state.filter) {
            HomeFilter.All -> allSections(state, actions, wide)
            HomeFilter.Albums -> item(key = "albumGrid") {
                FilterGrid(state.biggestAlbums.size, wide?.grid ?: 3) { AlbumTile(state.biggestAlbums[it], actions, width = null) }
            }
            HomeFilter.Artists -> item(key = "artistGrid") {
                FilterGrid(state.biggestArtists.size, wide?.grid ?: 3) { ArtistTile(state.biggestArtists[it], actions, width = null) }
            }
            HomeFilter.Folders -> item(key = "folderGrid") {
                FilterGrid(state.folders.size, wide?.grid ?: 3) { FolderTile(state.folders[it], actions, width = null) }
            }
        }
    }
}

/**
 * How Home fills a tablet's column (docs/design/2026-11-25-tablet/SPEC.md, Home): the shortcut and
 * folder trays six across (art 112dp on an expanded window, 72 on a medium one), Instant mixes' pages
 * side by side (three on expanded, two on medium), Start listening in two cards on expanded, and the
 * filter grids in columns at least 160dp wide.
 */
private class HomeColumns(val trayArt: Dp, val mixes: Int, val start: Int, val grid: Int, val shortHero: Boolean) {
    companion object {
        fun of(window: WindowLayout): HomeColumns {
            val expanded = window.widthClass == WidthClass.Expanded
            return HomeColumns(
                trayArt = if (expanded) 112.dp else 72.dp,
                mixes = if (expanded) 3 else 2,
                start = if (expanded) 2 else 1,
                grid = adaptiveColumns(window.contentWidthDp() - 48f, 160f, 14f),
                shortHero = window.heightDp < 840f,
            )
        }
    }
}

/** The tray's columns on a tablet. Four would leave a second row of two shortcuts. */
private const val WideTrayColumns = 6

private fun LazyListScope.allSections(state: HomeState, actions: HomeActions, wide: HomeColumns? = null) {
    if (state.shortcuts.isNotEmpty()) {
        item(key = "shortcuts") { Shortcuts(state.shortcuts, actions, wide) }
    }
    state.wrapped?.let { teaser ->
        item(key = "recap") { RecapEntry(teaser, actions.openWrapped, Modifier.padding(top = GroupSpacing)) }
    }
    if (state.speedDial.isNotEmpty() && wide != null) {
        item(key = "mixes") { InstantMixColumns(state, actions, wide.mixes) }
    } else if (state.speedDial.isNotEmpty()) {
        item(key = "mixes") {
            Column {
                Label(stringResource(R.string.home_instant_mixes))
                val pages = state.speedDial.chunked(3)
                Pager(pages.size) { index ->
                    items(pages[index]) { track ->
                        SongRow(
                            title = track.title,
                            subtitle = track.artist,
                            lead = SongLead.Cover(Art.of(track.mediaId)),
                            current = track.key == state.currentKey,
                            playing = state.isPlaying,
                            onClick = { actions.startRadio(track) },
                            onLongClick = { actions.showMenu(track) },
                            trailing = { SongRadioMark() },
                        )
                    }
                }
            }
        }
    }
    if (state.tasteMixes.isNotEmpty()) {
        item(key = "tasteMixesLabel") {
            Label(stringResource(R.string.home_mixes), small = stringResource(R.string.home_mixes_overline))
        }
        item(key = "tasteMixes") {
            Shelf {
                // Mixes never share a song, so the first one names the mix.
                items(state.tasteMixes, key = { it.tracks.first().key }) { mix ->
                    Tile(
                        title = mix.name,
                        onClick = { actions.playMix(mix) },
                        art = { m -> Mosaic(mix.mosaicIds.map(Art::Song), m) },
                        subtitle = songCount(mix.tracks.size),
                        twoLines = true,
                    )
                }
            }
        }
    }
    if (state.moods.isNotEmpty()) {
        item(key = "moodsLabel") {
            Label(stringResource(R.string.home_moods), small = stringResource(R.string.home_moods_overline))
        }
        if (state.askForMoods) {
            item(key = "moodsAsk") {
                MoodPicksCard(onChoose = actions.openInterests, onDismiss = actions.dismissMoodPrompt, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        item(key = "moods") {
            Shelf {
                items(state.moods, key = { it.mood.id }) { tile -> MoodTile(tile, onClick = { actions.openMood(tile) }) }
            }
        }
    }
    songShelf("recent", R.string.home_recently_played, state.recentTracks, state) { actions.playRecent(it) }
    songShelf("onRepeat", R.string.home_on_repeat, state.onRepeat, state) { actions.playShelf(HomeShelf.OnRepeat, it) }
    songShelf("notLately", R.string.home_not_lately, state.notLately, state) { actions.playShelf(HomeShelf.NotLately, it) }
    songShelf("newlyAdded", R.string.home_newly_added, state.newlyAdded, state) { actions.playShelf(HomeShelf.NewlyAdded, it) }
    if (state.startListening.isNotEmpty()) {
        item(key = "startLabel") {
            Label(stringResource(R.string.home_start_listening), small = stringResource(R.string.home_start_listening_overline))
        }
        if (wide != null) {
            columnGroupItems(state.startListening, wide.start, "start", key = { it.key }) { _, track ->
                StartListeningRow(track, state, actions)
            }
        } else {
            groupItems(state.startListening, key = { "start:${it.key}" }) { _, track ->
                StartListeningRow(track, state, actions)
            }
        }
    }
    if (state.folders.isNotEmpty()) {
        item(key = "foldersLabel") { Label(stringResource(R.string.home_your_folders)) }
        item(key = "folders") { if (wide != null) WideFolderTray(state.folders, actions, wide.trayArt) else FolderTray(state.folders, actions) }
    }
    if (state.biggestAlbums.isNotEmpty()) {
        item(key = "albumsLabel") { Label(stringResource(R.string.home_albums)) }
        item(key = "albums") {
            Shelf { items(state.biggestAlbums, key = { it.key }) { AlbumTile(it, actions) } }
        }
    }
    state.spotlight?.let { artist ->
        item(key = "spotlight") { Spotlight(artist, actions, Modifier.padding(top = 28.dp)) }
    }
    if (state.biggestArtists.isNotEmpty()) {
        item(key = "artistsLabel") { Label(stringResource(R.string.home_top_artists)) }
        item(key = "artists") {
            Shelf { items(state.biggestArtists, key = { it.key }) { ArtistTile(it, actions) } }
        }
    }
}

/** A shelf of songs (Recently played and the shelves after it): cover, title, artist; tap plays the shelf from that song. */
private fun LazyListScope.songShelf(key: String, @StringRes title: Int, tracks: List<Track>, state: HomeState, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    item(key = "${key}Label") { Label(stringResource(title)) }
    item(key = key) {
        Shelf {
            items(tracks, key = { it.key }) { track ->
                Tile(
                    title = track.title,
                    onClick = { onPlay(track) },
                    art = { m -> Cover(Art.of(track.mediaId), m) },
                    subtitle = track.artist,
                    width = 112.dp,
                    now = track.key == state.currentKey,
                    playing = state.isPlaying,
                )
            }
        }
    }
}

@Composable
private fun StartListeningRow(track: Track, state: HomeState, actions: HomeActions) {
    SongRow(
        title = track.title,
        subtitle = track.artist,
        lead = SongLead.Cover(Art.of(track.mediaId)),
        current = track.key == state.currentKey,
        playing = state.isPlaying,
        onClick = { actions.playStartListening(track) },
        onLongClick = { actions.showMenu(track) },
        onSwipeToQueue = { actions.queue(track) },
        trailing = { SongMoreButton(track.title, onClick = { actions.showMenu(track) }) },
    )
}

/** The shortcuts as a 3 × 2 tray in one card (`.group.pad > .grid3.tray`): Favourites first. */
@Composable
private fun Shortcuts(shortcuts: List<Shortcut>, actions: HomeActions, wide: HomeColumns? = null) {
    Group(pad = true) {
        item {
            TileGrid(shortcuts.size, columns = if (wide != null) WideTrayColumns else 3, tray = true, trayGaps = true) { index ->
                val shortcut = shortcuts[index]
                Tile(
                    title = when (shortcut) {
                        Shortcut.Liked -> stringResource(R.string.favourites)
                        is Shortcut.ForAlbum -> shortcut.album.name
                        is Shortcut.ForFolder -> shortcut.folder.name
                    },
                    onClick = { actions.openShortcut(shortcut) },
                    art = { m ->
                        when (shortcut) {
                            Shortcut.Liked -> FavouritesArt(m)
                            is Shortcut.ForAlbum -> Cover(Art.of(shortcut.album.tracks.firstOrNull()?.mediaId), m)
                            is Shortcut.ForFolder -> Mosaic(mosaicIds(shortcut.folder.tracks).map(Art::Song), m)
                        }
                    },
                    width = null,
                    centered = true,
                    twoLines = true,
                    artSize = wide?.trayArt ?: trayArt(3),
                )
            }
        }
    }
}

/**
 * Instant mixes on a tablet: where the phone pages sideways, the songs sit in [columns] equal cards
 * side by side, filled top to bottom.
 */
@Composable
private fun InstantMixColumns(state: HomeState, actions: HomeActions, columns: Int) {
    Column {
        Label(stringResource(R.string.home_instant_mixes))
        val per = ((state.speedDial.size + columns - 1) / columns).coerceAtLeast(1)
        val parts = state.speedDial.chunked(per)
        Row(Modifier.padding(horizontal = GroupMargin), horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
            for (part in parts) {
                Box(Modifier.weight(1f)) {
                    Group(margin = 0.dp) {
                        items(part) { track ->
                            SongRow(
                                title = track.title,
                                subtitle = track.artist,
                                lead = SongLead.Cover(Art.of(track.mediaId)),
                                current = track.key == state.currentKey,
                                playing = state.isPlaying,
                                onClick = { actions.startRadio(track) },
                                onLongClick = { actions.showMenu(track) },
                                trailing = { SongRadioMark() },
                            )
                        }
                    }
                }
            }
            // Fewer songs than columns leave the last cells empty, so the cards keep their width.
            repeat(columns - parts.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** "Your folders" on a tablet: the six-across tray, in rows. */
@Composable
private fun WideFolderTray(folders: List<FolderPlaylist>, actions: HomeActions, art: Dp) {
    Group(pad = true) {
        item {
            TileGrid(folders.size, columns = WideTrayColumns, tray = true, trayGaps = true) { index ->
                Tile(
                    title = folders[index].name,
                    onClick = { actions.openFolder(folders[index]) },
                    art = { m -> Mosaic(mosaicIds(folders[index].tracks).map(Art::Song), m) },
                    subtitle = songCount(folders[index].tracks.size),
                    width = null,
                    centered = true,
                    artSize = art,
                )
            }
        }
    }
}

/**
 * The Recap entry (`.row.recap`, sheet 09 E): a mosaic of the year's top covers, the headline in
 * accent ink ("Your 2026 so far", or "Your 2026 Recap" from 24 December) and the minutes line, which
 * wraps.
 */
@Composable
private fun RecapEntry(teaser: WrappedTeaser, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val year = teaser.year.toString()
    Group(modifier) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .rowClick(onOpen)
                    .heightIn(min = 72.dp)
                    .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Mosaic(teaser.mosaicIds.map(Art::Song), size = 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (teaser.yearEnded) R.string.home_recap_title_year else R.string.home_recap_title_so_far, year),
                        style = onest(17f, 22f, 700),
                        color = colors.accentInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.home_recap_body, NumberFormat.getIntegerInstance().format(teaser.minutes)),
                        style = Comfort.type.sub,
                        color = colors.on2,
                    )
                }
                ComfortIcon(Symbol.ChevronRight, tint = colors.on3, modifier = Modifier.padding(end = 6.dp))
            }
        }
    }
}

/**
 * "Your folders" as a four-across tray in a card (`.group.pad > .grid4.tray`). A fifth folder scrolls
 * the tray sideways.
 */
@Composable
private fun FolderTray(folders: List<FolderPlaylist>, actions: HomeActions) {
    Group(pad = true) {
        item {
            if (folders.size <= 4) {
                TileGrid(folders.size, columns = 4, tray = true) { index -> FolderTrayTile(folders[index], actions, width = null) }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val gap = 10.dp
                    val width = (maxWidth - gap * 3) / 4
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        folders.forEach { FolderTrayTile(it, actions, width) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTrayTile(folder: FolderPlaylist, actions: HomeActions, width: Dp?) {
    Tile(
        title = folder.name,
        onClick = { actions.openFolder(folder) },
        art = { m -> Mosaic(mosaicIds(folder.tracks).map(Art::Song), m) },
        subtitle = songCount(folder.tracks.size),
        width = width,
        centered = true,
        artSize = trayArt(4),
    )
}

/** "More from": one card, the artist as a row that opens them, then their albums on a shelf inside it. */
@Composable
private fun Spotlight(artist: Artist, actions: HomeActions, modifier: Modifier = Modifier) {
    Group(modifier) {
        item {
            CollectionRow(
                title = artist.name,
                subtitle = null,
                overline = stringResource(R.string.home_more_from),
                lead = { m -> ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), m) },
                onClick = { actions.openArtist(artist) },
                size = RowSize.Small,
                chevron = true,
            )
        }
        item {
            Inset {
                Shelf {
                    items(artist.albums, key = { it.key }) { album ->
                        Tile(
                            title = album.name,
                            onClick = { actions.openAlbum(album) },
                            art = { m -> Cover(Art.of(album.tracks.firstOrNull()?.mediaId), m) },
                            subtitle = album.year?.toString() ?: stringResource(R.string.home_album_no_year),
                            width = 104.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumTile(album: Album, actions: HomeActions, width: Dp? = 132.dp) {
    Tile(
        title = album.name,
        onClick = { actions.openAlbum(album) },
        art = { m -> Cover(Art.of(album.tracks.firstOrNull()?.mediaId), m) },
        subtitle = album.artist,
        width = width,
    )
}

@Composable
private fun FolderTile(folder: FolderPlaylist, actions: HomeActions, width: Dp? = 132.dp) {
    Tile(
        title = folder.name,
        onClick = { actions.openFolder(folder) },
        art = { m -> Mosaic(mosaicIds(folder.tracks).map(Art::Song), m) },
        subtitle = songCount(folder.tracks.size),
        width = width,
    )
}

@Composable
private fun ArtistTile(artist: Artist, actions: HomeActions, width: Dp? = 92.dp) {
    Tile(
        title = artist.name,
        onClick = { actions.openArtist(artist) },
        art = { m -> ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), m) },
        width = width,
        centered = true,
        twoLines = width == null,
    )
}

/** A sub-tab's one section (frame C): a three-column grid where "All" has a shelf. */
@Composable
private fun FilterGrid(count: Int, columns: Int = 3, tile: @Composable (Int) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        TileGrid(count, columns = columns) { tile(it) }
        Spacer(Modifier.height(8.dp))
    }
}

