package app.attic.player.ui.expressive

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.attic.player.R
import app.attic.player.data.Album
import app.attic.player.data.Artist
import app.attic.player.data.FolderPlaylist
import app.attic.player.data.Track
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.HomeActions
import app.attic.player.ui.model.HomeFilter
import app.attic.player.ui.model.HomeShelf
import app.attic.player.ui.model.HomeState
import app.attic.player.ui.model.Shortcut
import app.attic.player.ui.model.WrappedTeaser
import app.attic.player.ui.mosaicIds
import java.text.NumberFormat

/**
 * Home (sheet 01): the root bar with the mark, "Attic" and settings; the connected filter; then, under
 * "All", the carousel of places with Favourites pinned as the pill, the Recap card, Instant mixes,
 * the taste mixes, Recently played, the shelves from your listening (On repeat, Haven't heard in a
 * while, Newly added), Start listening as a grouped list, folders, albums, the "More from" block and
 * top artists. Another filter gives its one kind of content the page as a grid.
 */
@Composable
fun ExpressiveHome(state: HomeState, actions: HomeActions) {
    val listState = rememberLazyListState()
    val scrolled = if (state.isEmpty) 0f else listState.scrolledFraction()
    val markFraction by animateFloatAsState(scrolled, ExpressiveMotion.defaultEffects(), label = "homeMark")
    Column(Modifier.fillMaxSize()) {
        RootAppBar(
            title = stringResource(R.string.app_name),
            scrolled = scrolled,
            titleLeading = {
                AtticMarkIcon(size = lerp(32.dp, 24.dp, markFraction), tint = MaterialTheme.colorScheme.primary)
            },
        ) {
            ExpressiveIconButton(Symbol.Settings, stringResource(R.string.settings), actions.openSettings)
        }
        if (state.isEmpty) {
            // No filter: there is nothing to filter. No action: songs arrive by copying files.
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                body = stringResource(R.string.home_empty_body),
                modifier = Modifier.padding(top = 64.dp),
                symbol = Symbol.FolderOpen,
                clover = true,
            )
            return
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomPadding.current),
        ) {
            item(key = "filter") {
                val labels = listOf(
                    stringResource(R.string.home_filter_all),
                    stringResource(R.string.home_filter_albums),
                    stringResource(R.string.home_filter_artists),
                    stringResource(R.string.home_filter_folders),
                )
                ConnectedButtonGroup(labels, state.filter.ordinal, { actions.setFilter(HomeFilter.entries[it]) })
            }
            when (state.filter) {
                HomeFilter.All -> allSections(state, actions)
                HomeFilter.Albums -> item(key = "albumGrid") {
                    FilterGrid(state.biggestAlbums) { AlbumTile(it, actions, width = null) }
                }
                HomeFilter.Artists -> item(key = "artistGrid") {
                    FilterGrid(state.biggestArtists, columns = 3) { ArtistTile(it, actions, width = null) }
                }
                HomeFilter.Folders -> item(key = "folderGrid") {
                    FilterGrid(state.folders) { FolderTile(it, actions, width = null) }
                }
            }
        }
    }
}

private fun LazyListScope.allSections(state: HomeState, actions: HomeActions) {
    if (state.shortcuts.isNotEmpty()) {
        item(key = "hero") {
            Spacer(Modifier.height(12.dp))
            Places(state, actions)
        }
    }
    state.wrapped?.let { teaser ->
        item(key = "recap") { Recap(teaser, actions.openWrapped) }
    }
    if (state.speedDial.isNotEmpty()) {
        item(key = "mixes") {
            SectionHeader(stringResource(R.string.home_instant_mixes))
            InstantMixes(state.speedDial, rows = 3) { track ->
                InstantMixPill(
                    title = track.title,
                    art = Art.of(track.mediaId),
                    onClick = { actions.startRadio(track) },
                    onLongClick = { actions.showMenu(track) },
                    now = track.key == state.radioSeedKey,
                )
            }
        }
    }
    if (state.tasteMixes.isNotEmpty()) {
        item(key = "tasteMixes") {
            SectionHeader(stringResource(R.string.home_mixes), overline = stringResource(R.string.home_mixes_overline))
            Shelf {
                // Mixes never share a song, so the first one names the mix.
                items(state.tasteMixes, key = { it.tracks.first().key }) { mix ->
                    Tile(
                        title = mix.name,
                        onClick = { actions.playMix(mix) },
                        art = { Mosaic(mix.mosaicIds.map(Art::Song), it) },
                        subtitle = songCount(mix.tracks.size),
                        titleLines = 2,
                    )
                }
            }
        }
    }
    if (state.moods.isNotEmpty()) {
        item(key = "moods") {
            SectionHeader(stringResource(R.string.home_moods), overline = stringResource(R.string.home_moods_overline))
            if (state.askForMoods) {
                MoodPicksCard(onChoose = actions.openInterests, onDismiss = actions.dismissMoodPrompt, modifier = Modifier.padding(bottom = 12.dp))
            }
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
        item(key = "startHeader") {
            SectionHeader(
                stringResource(R.string.home_start_listening),
                overline = stringResource(R.string.home_start_listening_overline),
            )
        }
        groupItems(state.startListening, key = { "start:${it.key}" }) { _, track ->
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = RowLead.Cover(Art.of(track.mediaId)),
                current = state.currentKey == track.key,
                playing = state.isPlaying,
                onClick = { actions.playStartListening(track) },
                onLongClick = { actions.showMenu(track) },
                onSwipeToQueue = { actions.queue(track) },
                trailing = { RowMoreButton(track.title) { actions.showMenu(track) } },
            )
        }
    }
    if (state.folders.isNotEmpty()) {
        item(key = "folders") {
            SectionHeader(stringResource(R.string.home_your_folders))
            Shelf { items(state.folders, key = { it.name }) { FolderTile(it, actions) } }
        }
    }
    if (state.biggestAlbums.isNotEmpty()) {
        item(key = "albums") {
            SectionHeader(stringResource(R.string.home_albums))
            Shelf { items(state.biggestAlbums, key = { it.key }) { AlbumTile(it, actions) } }
        }
    }
    state.spotlight?.let { artist ->
        item(key = "spotlight") {
            Spotlight(
                overline = stringResource(R.string.home_more_from),
                title = artist.name,
                art = Art.of(artist.tracks.firstOrNull()?.mediaId),
                onOpen = { actions.openArtist(artist) },
            ) {
                Shelf {
                    items(artist.albums, key = { it.key }) { album ->
                        Tile(
                            title = album.name,
                            onClick = { actions.openAlbum(album) },
                            art = { Cover(Art.of(album.tracks.firstOrNull()?.mediaId), it) },
                            subtitle = album.year?.toString() ?: stringResource(R.string.home_album_no_year),
                            width = 120.dp,
                        )
                    }
                }
            }
        }
    }
    if (state.biggestArtists.isNotEmpty()) {
        item(key = "artists") {
            SectionHeader(stringResource(R.string.home_top_artists))
            Shelf { items(state.biggestArtists, key = { it.key }) { ArtistTile(it, actions) } }
        }
    }
}

/** A shelf of songs (Recently played and the shelves after it): cover, title, artist; tap plays the shelf from that song. */
private fun LazyListScope.songShelf(key: String, @StringRes title: Int, tracks: List<Track>, state: HomeState, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    item(key = key) {
        SectionHeader(stringResource(title))
        Shelf {
            items(tracks, key = { it.key }) { track ->
                Tile(
                    title = track.title,
                    onClick = { onPlay(track) },
                    art = { Cover(Art.of(track.mediaId), it) },
                    subtitle = track.artist,
                    width = 112.dp,
                    now = state.currentKey == track.key,
                    playing = state.isPlaying,
                )
            }
        }
    }
}

/** The six shortcuts as one carousel: Favourites pinned as the pill, then the recent places, the first one large. */
@Composable
private fun Places(state: HomeState, actions: HomeActions) {
    val places = state.shortcuts.filter { it != Shortcut.Liked }
    val count = state.favouriteCount
    val pill = @Composable {
        FavouritesPill(
            count = NumberFormat.getIntegerInstance().format(count),
            unit = pluralStringResource(R.plurals.home_favourites_unit, count),
            description = stringResource(R.string.favourites),
            onClick = { actions.openShortcut(Shortcut.Liked) },
            // As tall as the covers and their caption, as `.hero-fav` stretches in its row.
            height = if (places.isEmpty()) 200.dp else 248.dp,
        )
    }
    if (places.isEmpty()) {
        Row(Modifier.padding(start = 16.dp)) { pill() }
        return
    }
    HeroCarousel(
        count = places.size,
        leading = pill,
        caption = { index ->
            when (val place = places[index]) {
                is Shortcut.ForAlbum -> HeroCaption(place.album.name, albumCaption(place.album))
                is Shortcut.ForFolder -> HeroCaption(place.folder.name, songCount(place.folder.tracks.size))
                Shortcut.Liked -> Unit
            }
        },
        onItemClick = { actions.openShortcut(places[it]) },
    ) { index ->
        val place = places[index]
        val name = when (place) {
            is Shortcut.ForAlbum -> place.album.name
            is Shortcut.ForFolder -> place.folder.name
            Shortcut.Liked -> ""
        }
        // Every slot is as tall as the carousel; the picture fills that height and the slot crops its sides.
        val art = Modifier.requiredSize(HERO_HEIGHT).semantics { contentDescription = name }
        when (place) {
            is Shortcut.ForAlbum -> Cover(Art.of(place.album.tracks.firstOrNull()?.mediaId), art, shape = RoundedCornerShape(0.dp))
            is Shortcut.ForFolder -> Mosaic(mosaicIds(place.folder.tracks).map(Art::Song), art, shape = RoundedCornerShape(0.dp))
            Shortcut.Liked -> Unit
        }
    }
}

private val HERO_HEIGHT = 200.dp

@Composable
private fun albumCaption(album: Album): String =
    album.year?.let { stringResource(R.string.home_hero_album_subtitle, album.artist, it.toString()) } ?: album.artist

@Composable
private fun songCount(count: Int): String =
    pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count))

@Composable
private fun Recap(teaser: WrappedTeaser, onOpen: () -> Unit) {
    val year = teaser.year.toString()
    RecapCard(
        title = stringResource(if (teaser.yearEnded) R.string.home_recap_title_year else R.string.home_recap_title_so_far, year),
        body = stringResource(R.string.home_recap_body, NumberFormat.getIntegerInstance().format(teaser.minutes)),
        onClick = onOpen,
    ) { m -> Mosaic(teaser.mosaicIds.map(Art::Song), m) }
}

@Composable
private fun AlbumTile(album: Album, actions: HomeActions, width: Dp? = 148.dp) {
    Tile(
        title = album.name,
        onClick = { actions.openAlbum(album) },
        art = { Cover(Art.of(album.tracks.firstOrNull()?.mediaId), it) },
        subtitle = album.artist,
        width = width,
    )
}

@Composable
private fun FolderTile(folder: FolderPlaylist, actions: HomeActions, width: Dp? = 148.dp) {
    Tile(
        title = folder.name,
        onClick = { actions.openFolder(folder) },
        art = { Mosaic(mosaicIds(folder.tracks).map(Art::Song), it) },
        subtitle = songCount(folder.tracks.size),
        width = width,
    )
}

@Composable
private fun ArtistTile(artist: Artist, actions: HomeActions, width: Dp? = 112.dp) {
    Tile(
        title = artist.name,
        onClick = { actions.openArtist(artist) },
        art = { ArtistCover(Art.of(artist.tracks.firstOrNull()?.mediaId), it) },
        width = width,
        artist = true,
    )
}

/** A filter's one kind of content, given the page (frame C): a grid instead of a shelf. */
@Composable
private fun <T> FilterGrid(items: List<T>, columns: Int = 2, tile: @Composable (T) -> Unit) {
    Box(Modifier.padding(top = 16.dp)) { TileGrid(items, columns = columns, tile = tile) }
}
