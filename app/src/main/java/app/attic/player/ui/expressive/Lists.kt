package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R

/** The colour a group's cells take. Sheets raise it to surface container high. */
val LocalGroupCellColor = staticCompositionLocalOf<Color?> { null }

/**
 * One cell of a group (`.group > *`): clipped to its place in the group (outer corners 20, inner 4),
 * filled with the cell colour, 2dp below the cell above, 16dp in from the screen edges. Use it on any
 * row inside a LazyColumn; [ListGroup] and [groupItems] apply it for you.
 */
fun Modifier.groupCell(index: Int, count: Int, color: Color, horizontalMargin: Dp = 16.dp): Modifier =
    this
        .padding(start = horizontalMargin, end = horizontalMargin, top = if (index == 0) 0.dp else ExpressiveShapes.GroupGap)
        .clip(ExpressiveShapes.groupItemShape(index, count))
        .background(color)

/** The colour a group cell takes here: surface container, or what [LocalGroupCellColor] says. */
@Composable
fun groupCellColor(): Color = LocalGroupCellColor.current ?: MaterialTheme.colorScheme.surfaceContainer

/**
 * A list group (`.group`): [count] cells 2dp apart, the group's outer corners 20 and the inner ones 4.
 * Every list sits in one; two groups in a row are [ExpressiveShapes.GroupSpacing] apart.
 */
@Composable
fun ListGroup(count: Int, modifier: Modifier = Modifier, cell: @Composable (index: Int) -> Unit) {
    val color = groupCellColor()
    Column(modifier.fillMaxWidth()) {
        for (index in 0 until count) {
            Box(Modifier.fillMaxWidth().groupCell(index, count, color)) { cell(index) }
        }
    }
}

/** [ListGroup] for a LazyColumn: each item is one cell. */
fun <T> LazyListScope.groupItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    contentType: (T) -> Any? = { "groupCell" },
    cell: @Composable (index: Int, item: T) -> Unit,
) {
    items(
        count = items.size,
        key = key?.let { k -> { index: Int -> k(items[index]) } },
        contentType = { index -> contentType(items[index]) },
    ) { index ->
        Box(Modifier.fillMaxWidth().groupCell(index, items.size, groupCellColor())) { cell(index, items[index]) }
    }
}

/** What leads a song row. */
@Immutable
sealed interface RowLead {
    /** A 48dp cover. */
    data class Cover(val art: Art?) : RowLead

    /** A track number in place of a cover (album tracks). The wave replaces it on the current song. */
    data class Number(val number: Int) : RowLead

    /** A rank before a cover (artist "Top songs", Recap). */
    data class RankedCover(val rank: Int, val art: Art?) : RowLead

    data object None : RowLead
}

/**
 * A song row (`.row`): lead, title over subtitle, trailing control. 64dp minimum, 12dp start, 4 end.
 * The current song ([current]) gets the secondary container fill, a heavier title, and the wave over
 * its cover or in place of its number, moving while [playing]. [lifted] is the row being dragged:
 * 20dp corners, the highest container and a shadow. Put it in a [ListGroup] or [groupItems].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    lead: RowLead,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    playing: Boolean = false,
    lifted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        when {
            lifted -> colors.surfaceContainerHighest
            current -> colors.secondaryContainer
            else -> Color.Transparent
        },
        ExpressiveMotion.defaultEffects(),
        label = "rowFill",
    )
    val corner by animateDpAsState(if (lifted) ExpressiveShapes.LargeIncreased else 0.dp, ExpressiveMotion.fastSpatial(), label = "rowLift")
    val liftShape: Shape = RoundedCornerShape(corner)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (lifted) Modifier.shadow(6.dp, liftShape) else Modifier)
            .clip(liftShape)
            .background(fill)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = null,
                        indication = ripple(),
                        onClick = onClick ?: {},
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .heightIn(min = 64.dp)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (lead) {
            is RowLead.Cover -> CoverLead(lead.art, current, playing)
            is RowLead.Number -> NumberLead(lead.number, current, playing)
            is RowLead.RankedCover -> {
                Text(
                    lead.rank.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(18.dp),
                )
                CoverLead(lead.art, current, playing)
            }
            RowLead.None -> Unit
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (current) FontWeight.Bold else FontWeight.Medium, lineHeight = 22.sp),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (current) colors.onSecondaryContainer else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun CoverLead(art: Art?, current: Boolean, playing: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Cover(art, size = 48.dp)
        if (current) {
            Box(Modifier.fillMaxSize().clip(CoverShape).background(colors.secondaryContainer.copy(alpha = 0.8f)))
            NowPlayingWave(playing)
        }
    }
}

@Composable
private fun NumberLead(number: Int, current: Boolean, playing: Boolean) {
    Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
        if (current) {
            NowPlayingWave(playing)
        } else {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The kebab at the end of a song row: "More options for {title}". */
@Composable
fun RowMoreButton(title: String, onClick: () -> Unit) {
    ExpressiveIconButton(Symbol.MoreVert, stringResource(R.string.more_options_for_cd, title), onClick)
}

/** A play count or other figure at the end of a row (`.count`): "187 plays". */
@Composable
fun RowCount(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 12.dp),
    )
}

/**
 * The add control in Add songs: a plus while the song is not in the playlist, a filled tick toggle
 * once it is (`.ib.on`). Descriptions "Add {title}" / "Remove {title}".
 */
@Composable
fun RowAddToggle(title: String, added: Boolean, onToggle: () -> Unit) {
    ExpressiveToggleButton(
        if (added) Symbol.Check else Symbol.Add,
        checked = added,
        onCheckedChange = { onToggle() },
        contentDescription = stringResource(if (added) R.string.add_songs_remove_cd else R.string.add_songs_add_cd, title),
    )
}

/**
 * The drag handle that replaces the kebab in reorder mode and the queue. Attach the reorder library's
 * drag modifier through [modifier]. "Drag {title} to reorder".
 */
@Composable
fun RowDragHandle(title: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.drag_to_reorder_cd, title)
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        SymbolIcon(Symbol.DragHandle, tint = MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = description)
    }
}

/**
 * A collection row (`.row.coll`): a 56dp lead whose shape says what it is (cover, mosaic, cookie,
 * Favourites heart), then title and subtitle. 72dp minimum.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionRow(
    title: String,
    subtitle: String,
    lead: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = ripple(), onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 72.dp)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { lead() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing() else Spacer(Modifier.width(8.dp))
    }
}

/**
 * Swipe to remove (`.swipe`): either way. Behind the row is the error container with "Remove" and
 * the delete glyph; [onRemove] runs once the swipe settles. Wrap a [SongRow] in it, inside a group cell.
 */
@Composable
fun SwipeToRemove(onRemove: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        onDismiss = { value -> if (value != SwipeToDismissBoxValue.Settled) onRemove() },
        backgroundContent = {
            val toStart = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Row(
                Modifier.fillMaxSize().background(colors.errorContainer).padding(horizontal = 24.dp),
                horizontalArrangement = if (toStart) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.onErrorContainer) {
                    SymbolIcon(Symbol.Delete, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.queue_remove), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        },
    ) {
        Box(Modifier.background(groupCellColor())) { content() }
    }
}
