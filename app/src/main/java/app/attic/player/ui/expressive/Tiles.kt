package app.attic.player.ui.expressive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A tile (`.tile`): a cover with a title and subtitle under it, in a shelf or grid. [art] draws the
 * picture (a [Cover], [Mosaic], [FavouritesArt] or [ArtistCover]) filling the width it is given.
 * [artist] centres the text under a cookie (`.tile.artist`). [now] marks the song that is playing:
 * the title turns primary with the wave before it. [titleLines] lets a long name wrap (a taste mix).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tile(
    title: String,
    onClick: () -> Unit,
    art: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp? = 148.dp,
    artist: Boolean = false,
    now: Boolean = false,
    playing: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    titleLines: Int = 1,
) {
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = if (artist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        art(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (now) {
                NowPlayingWave(playing)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 0.sp),
                color = if (now) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (artist) TextAlign.Center else TextAlign.Start,
            )
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A shelf (`.shelf`): one row of tiles that runs off the right edge, 12dp apart, 16dp in. */
@Composable
fun Shelf(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * A grid of tiles (`.grid`, `.grid.c3`): [columns] across, 12dp between columns and 20 (or 16 for
 * three columns) between rows. Not lazy, so it can sit inside a LazyColumn item.
 */
@Composable
fun <T> TileGrid(items: List<T>, modifier: Modifier = Modifier, columns: Int = 2, tile: @Composable (T) -> Unit) {
    val rowGap = if (columns >= 3) 16.dp else 20.dp
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(rowGap)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 0 until columns) {
                    Box(Modifier.weight(1f)) { row.getOrNull(column)?.let { tile(it) } }
                }
            }
        }
    }
}

/**
 * Favourites pinned beside Home's carousel (`.hero-fav`): a 64dp-wide pill on the tertiary container
 * with the heart and the song count. [description] is read out in place of the count ("Favourites").
 */
@Composable
fun FavouritesPill(count: String, unit: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 200.dp) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .width(64.dp)
            .height(height)
            .clip(RoundedCornerShape(32.dp))
            .background(colors.tertiaryContainer)
            .combinedClickableCompat(onClick)
            .semantics { contentDescription = description }
            .padding(top = 18.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        SymbolIcon(Symbol.Favorite, size = 28.dp, filled = true, tint = colors.onTertiaryContainer)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, style = Expressive.type.titleLargeEmphasized.copy(lineHeight = 24.sp), color = colors.onTertiaryContainer)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier =
    this.combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick)

/**
 * Home's hero carousel (`.hero`): Material's multi-browse carousel, items 200dp tall that change width
 * as they pass through the large, medium and small slots, 8dp apart with 28dp corners. A tablet gives
 * it a [height] and [preferredItemWidth] of its own (SPEC.md, Home). [item] draws
 * item [index] filling the slot; [caption] names the item in the large slot and cross-fades when it changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroCarousel(
    count: Int,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    caption: @Composable (index: Int) -> Unit = {},
    onItemClick: (Int) -> Unit = {},
    height: Dp = 200.dp,
    preferredItemWidth: Dp = 200.dp,
    item: @Composable (index: Int) -> Unit,
) {
    val state = rememberCarouselState { count }
    Row(modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (leading != null) leading()
        Column(Modifier.weight(1f)) {
            HorizontalMultiBrowseCarousel(
                state = state,
                preferredItemWidth = preferredItemWidth,
                itemSpacing = 8.dp,
                minSmallItemWidth = 40.dp,
                maxSmallItemWidth = 56.dp,
                modifier = Modifier.fillMaxWidth().height(height),
            ) { index ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .maskClip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
                        .combinedClickableCompat({ onItemClick(index) }),
                ) { item(index) }
            }
            androidx.compose.animation.Crossfade(state.currentItem, animationSpec = ExpressiveMotion.defaultEffects(), label = "heroCaption") { current ->
                Box(Modifier.width(preferredItemWidth).padding(start = 4.dp, top = 10.dp, end = 4.dp)) { caption(current) }
            }
        }
    }
}

/** A carousel caption (`.hero-cap`): the large item's title over its subtitle. */
@Composable
fun HeroCaption(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * An instant mix (`.mix`): a pill with the song's cover as a disc and its title. A tap starts radio
 * from that song; a long press opens the track menu. [now] is the seed of the radio that is playing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstantMixPill(title: String, art: Art?, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier, now: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val container = if (now) colors.secondaryContainer else colors.surfaceContainerHigh
    Row(
        modifier
            .height(56.dp)
            .clip(ExpressiveShapes.Pill)
            .background(container)
            .combinedClickable(interactionSource = null, indication = ripple(), onClick = onClick, onLongClick = onLongClick)
            .padding(start = 8.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Cover(art, size = 40.dp, shape = CircleShape)
            Box(Modifier.size(8.dp).clip(CircleShape).background(container))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.onSurface, maxLines = 1)
    }
}

/** Instant mixes (`.mixes`): pills in [rows] rows that scroll sideways together, 8dp apart. */
@Composable
fun <T> InstantMixes(items: List<T>, modifier: Modifier = Modifier, rows: Int = 3, pill: @Composable (T) -> Unit) {
    val perRow = (items.size + rows - 1) / rows.coerceAtLeast(1)
    Column(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(perRow.coerceAtLeast(1)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { pill(it) } }
        }
    }
}

/** Home's Recap card (`.recap-card`): headline and body on the primary container, a mosaic at the end. */
@Composable
fun RecapCard(title: String, body: String, onClick: () -> Unit, modifier: Modifier = Modifier, art: @Composable (Modifier) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.primaryContainer)
            .combinedClickableCompat(onClick)
            .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Expressive.type.headlineSmallEmphasized.copy(lineHeight = 28.sp, letterSpacing = (-0.2).sp), color = colors.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        art(Modifier.size(72.dp))
    }
}

/**
 * The "More from" spotlight (`.spot`): a contained block on surface container with its own head
 * (cookie, overline, name, arrow) and a shelf under it.
 */
@Composable
fun Spotlight(
    overline: String,
    title: String,
    art: Art?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .padding(vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().combinedClickableCompat(onOpen).padding(start = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtistCover(art, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(overline, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(title, style = Expressive.type.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ExpressiveIconButton(Symbol.ArrowForward, null, onOpen)
        }
        content()
    }
}

/**
 * A browse tile (`.browse`): title and a 56dp cover on the secondary container of the cover's own
 * scheme. [seed] is the cover tint (see [FromArt]). With a [subtitle] it is Search's folder tile
 * (sheet 02 A): 88dp tall, an 18sp bold title over the subtitle ("24 songs") and a 64dp picture.
 */
@Composable
fun BrowseTile(
    title: String,
    seed: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    art: @Composable (Modifier) -> Unit,
) {
    FromArt(seed) {
        val colors = MaterialTheme.colorScheme
        val large = subtitle != null
        Row(
            modifier
                .height(if (large) 88.dp else 72.dp)
                .clip(RoundedCornerShape(ExpressiveShapes.LargeIncreased))
                .background(colors.secondaryContainer)
                .combinedClickableCompat(onClick)
                .padding(start = 16.dp, end = if (large) 12.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (large) {
                    // One line beside the 64dp mosaic leaves about 85dp: the name narrows before it truncates.
                    NarrowingText(
                        title,
                        style = Expressive.type.titleMediumEmphasized.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = colors.onSecondaryContainer,
                    )
                } else {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp),
                        color = colors.onSecondaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            art(Modifier.size(if (large) 64.dp else 56.dp))
        }
    }
}

/** A stat tile (`.stat`): a figure over its label. [accent] puts it on the primary container. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val container = if (accent) colors.primaryContainer else colors.surfaceContainer
    val content = if (accent) colors.onPrimaryContainer else colors.onSurface
    Column(
        modifier
            .clip(RoundedCornerShape(ExpressiveShapes.LargeIncreased))
            .background(container)
            .padding(16.dp),
    ) {
        Text(value, style = Expressive.type.headlineMediumEmphasized.copy(letterSpacing = (-0.3).sp), color = content, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (accent) content else colors.onSurfaceVariant)
    }
}
