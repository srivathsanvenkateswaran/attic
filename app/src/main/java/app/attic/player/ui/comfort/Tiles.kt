package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * `.tile`: a collection as an object, superellipse art with its name under it. [art] draws the picture
 * into the modifier it is given (square, full tile width or [artSize]): `{ m -> Cover(art, m) }`,
 * `{ m -> ArtistCover(art, m) }`, `{ m -> Mosaic(arts, m) }`.
 *
 * - [width] is `--w` on a shelf (default 132); pass null inside a grid.
 * - [centered] is `.tile.center` (artists, trays): centred medium names. [twoLines] is `b.two`.
 * - [artSize] fixes the art and centres it (a tray's 80 or 64dp).
 * - [now] is `.tile.now`: the cover dimmed with the bars over it; [playing] animates them.
 * - [subtitleLines] lets the second line wrap (Search's result shelf: "Album, {artist}" on two lines).
 */
@Composable
fun Tile(
    title: String,
    onClick: (() -> Unit)?,
    art: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp? = 132.dp,
    centered: Boolean = false,
    twoLines: Boolean = false,
    artSize: Dp? = null,
    now: Boolean = false,
    playing: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    subtitleLines: Int = 1,
) {
    val colors = Comfort.colors
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .rowClick(onClick, onLongClick),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            if (artSize != null) Modifier.size(artSize) else Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            art(if (artSize != null) Modifier.size(artSize) else Modifier.fillMaxWidth())
            if (now) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f), ArtShape))
                PlayingBars(playing, size = 22.dp, barWidth = 4.dp, gap = 3.5.dp, color = colors.onArt)
            }
        }
        Text(
            title,
            style = if (centered) onest(13.5f, 18f, 500) else onest(14f, 18f, 600),
            color = colors.on,
            maxLines = if (twoLines) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = Comfort.type.caption,
                color = colors.on2,
                maxLines = subtitleLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
            )
        }
    }
}

/**
 * `.shelf`: tiles that scroll sideways, 14dp apart, 24dp in from the screen edges (16 inside a group).
 *
 * ```
 * Shelf { items(albums) { Tile(…) } }
 * ```
 */
@Composable
fun Shelf(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    val inGroup = LocalComfortContainer.current != ComfortContainer.Ground
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = if (inGroup) 16.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/**
 * `.grid2`, `.grid3`, `.grid4`: tiles in equal columns (not lazy). [tray] is `.tray` inside a
 * `Group(pad = true)`: fixed art (80dp in three columns, 64 in four) with centred names, like folders on
 * a home screen. Give each [Tile] `width = null`; in a tray also `artSize = TrayArt(columns)` and
 * `centered = true`.
 */
@Composable
fun TileGrid(
    count: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    tray: Boolean = false,
    /** A tablet's six-across tray keeps the three-across tray's gaps. */
    trayGaps: Boolean = tray && columns == 3,
    item: @Composable (index: Int) -> Unit,
) {
    val inGroup = LocalComfortContainer.current != ComfortContainer.Ground
    val (rowGap, columnGap) = when {
        trayGaps -> 14.dp to 6.dp
        columns == 2 -> 12.dp to 12.dp
        columns == 3 -> 20.dp to 14.dp
        else -> 16.dp to 10.dp
    }
    val side = when {
        inGroup -> 0.dp
        columns == 2 -> 16.dp
        else -> 24.dp
    }
    Column(modifier.fillMaxWidth().padding(horizontal = side), verticalArrangement = Arrangement.spacedBy(rowGap)) {
        for (start in 0 until count step columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                for (column in 0 until columns) {
                    val index = start + column
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (index < count) item(index)
                    }
                }
            }
        }
    }
}

/** A tray's art size: 80dp in three columns, 64 in four. */
fun trayArt(columns: Int): Dp = if (columns >= 4) 64.dp else 80.dp

/**
 * `.pager`: cards that page sideways (Instant mixes), 312dp wide and 10dp apart, so the next one shows
 * at the right edge. Each page is a group: fill it like [Group]'s scope.
 */
@Composable
fun Pager(pageCount: Int, modifier: Modifier = Modifier, page: GroupScope.(index: Int) -> Unit) {
    val state = rememberPagerState { pageCount }
    HorizontalPager(
        state = state,
        modifier = modifier.fillMaxWidth(),
        pageSize = PageSize.Fixed(312.dp),
        pageSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) { index ->
        Group(margin = 0.dp) { page(index) }
    }
}

/**
 * `.browse`: Search's small card with the art top-left and the name at the bottom. With a [seed] it is
 * `.browse.tinted.wash`, taking a trace of its cover.
 */
@Composable
fun BrowseTile(
    title: String,
    onClick: () -> Unit,
    art: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    seed: Color = Color.Unspecified,
) {
    val body: @Composable () -> Unit = {
        val colors = Comfort.colors
        Column(
            modifier
                .heightIn(min = 128.dp)
                .clip(ComfortShapes.Group)
                .background(colors.card)
                .rowClick(onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            art(Modifier.size(56.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = onest(16f, 21f, 600), color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = Comfort.type.caption, color = colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (seed.isSpecified) Tinted(seed, wash = true, content = body) else body()
}

/**
 * `.c-head`: a collection's header, centred in the viewing area: [art] (196dp for an album or playlist,
 * 148 for a [person]), the title (26/32 bold, up to three lines; 30/36 for a person), the artist link
 * [byline] in accent ink with a chevron (album only), and [meta] ("Album • 2026", "14 songs, 1 hr 13 min").
 */
@Composable
fun CollectionHeader(
    title: String,
    art: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    byline: String? = null,
    onBylineClick: (() -> Unit)? = null,
    meta: String? = null,
    person: Boolean = false,
    artSize: Dp = if (person) 148.dp else 196.dp,
) {
    val colors = Comfort.colors
    Column(
        modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art(Modifier.size(artSize))
        Text(
            title,
            style = if (person) Comfort.type.personTitle else Comfort.type.title,
            color = colors.on,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 18.dp),
        )
        if (byline != null) {
            Row(
                Modifier.padding(top = 6.dp).clip(ComfortShapes.Pill).rowClick(onBylineClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(byline, style = onest(15f, 20f, 600), color = colors.accentInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (onBylineClick != null) ComfortIcon(app.attic.player.ui.expressive.Symbol.ChevronRight, size = 18.dp, tint = colors.accentInk)
            }
        }
        if (meta != null) {
            Text(meta, style = onest(13.5f, 19f), color = colors.on2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Provides the card container for a hand-made card (`.bg-card` holding kit parts). */
@Composable
fun OnCard(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalComfortContainer provides ComfortContainer.Card, content = content)
