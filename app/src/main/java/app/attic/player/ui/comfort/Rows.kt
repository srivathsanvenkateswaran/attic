package app.attic.player.ui.comfort

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.onSecondaryClick
import app.attic.player.R
import app.attic.player.ui.SwipeToQueue
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol

/**
 * `.txt`: the text block of every row. [overline] is `<em>` (12.5/16 above), [title] `<b>` (16/22
 * medium), [subtitle] `<span>` (13.5/18 in `--on-2`). One line each with an ellipsis unless [wrap].
 */
@Composable
fun RowText(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overline: String? = null,
    titleStyle: TextStyle = Comfort.type.rowTitle,
    subtitleStyle: TextStyle = Comfort.type.sub,
    titleColor: Color = Comfort.colors.on,
    subtitleColor: Color = Comfort.colors.on2,
    wrap: Boolean = false,
) {
    val lines = if (wrap) Int.MAX_VALUE else 1
    Column(modifier, verticalArrangement = Arrangement.Center) {
        if (overline != null) Text(overline, style = Comfort.type.caption, color = Comfort.colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title, style = titleStyle, color = titleColor, maxLines = lines, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, style = subtitleStyle, color = subtitleColor, maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}

/** `.row` sizes: default 72dp with 56dp art, `.sm` 64dp with 48dp art, `.lg` 92dp with 64dp art and a bold 18sp title. */
enum class RowSize(val minHeight: Dp, val art: Dp, val separator: Dp) {
    Small(64.dp, 48.dp, 78.dp),
    Medium(72.dp, 56.dp, 86.dp),
    Large(92.dp, 64.dp, 94.dp),
}

/**
 * `.row`: a collection in a list (album, artist, playlist, folder, Favourites), or the artist row under
 * Now Playing (`.row.lg`). [lead] draws the art at the size it is given (`RowSize.art`):
 * `{ m -> Cover(art, m) }`, `{ m -> ArtistCover(art, m) }`, `{ m -> FavouritesArt(m) }`.
 * [value] is `.val` (a trailing figure), [chevron] the trailing `chevron_right`; [trailing] replaces both.
 */
@Composable
fun CollectionRow(
    title: String,
    subtitle: String?,
    lead: @Composable (Modifier) -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: RowSize = RowSize.Medium,
    overline: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = Comfort.colors
    // In a sheet the row lines up with the sheet's 24dp margin (`.sheet .row`).
    val inSheet = LocalComfortContainer.current == ComfortContainer.Sheet
    Row(
        modifier
            .fillMaxWidth()
            .hairlineAbove(LocalHairlineAbove.current, size.separator + if (inSheet) 8.dp else 0.dp, if (inSheet) 24.dp else 20.dp)
            .rowClick(onClick, onLongClick)
            .heightIn(min = size.minHeight)
            .padding(start = if (inSheet) 24.dp else 16.dp, end = if (inSheet) 18.dp else 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        lead(Modifier.size(size.art))
        RowText(
            title,
            Modifier.weight(1f),
            subtitle = subtitle,
            overline = overline,
            titleStyle = if (size == RowSize.Large) Comfort.type.rowTitleLarge else Comfort.type.rowTitle,
        )
        if (trailing != null) {
            CompositionLocalProvider(LocalContentColor provides colors.on3) { trailing() }
        } else {
            if (value != null) {
                Text(value, style = onest(13.5f, 18f).tabular(), color = colors.on2, maxLines = 1, modifier = Modifier.padding(end = 10.dp))
            }
            if (chevron) ComfortIcon(Symbol.ChevronRight, tint = colors.on3, modifier = Modifier.padding(end = 6.dp))
        }
    }
}

/** `.kv`: a label over a value, for Credits. */
@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .hairlineAbove(LocalHairlineAbove.current, 20.dp)
            .rowClick(onClick)
            .padding(start = 20.dp, end = 20.dp, top = 11.dp, bottom = 12.dp),
    ) {
        Text(label, style = onest(12.5f, 17f), color = Comfort.colors.on2)
        Text(value, style = Comfort.type.rowTitle, color = Comfort.colors.on)
    }
}

/** What leads a song row. */
sealed interface SongLead {
    /** A 48dp cover. */
    data class Cover(val art: Art?) : SongLead

    /** A 48dp artist picture, in the rounder superellipse (Recap's top artists). */
    data class Artist(val art: Art?) : SongLead

    /** `.song.numbered`: the track number, no cover (an album). */
    data class Number(val number: Int) : SongLead

    /** `.song.bare`: nothing. */
    data object None : SongLead
}

/**
 * `.song`: a song in a group. 64dp, a 48dp cover, title and artist, then [trailing] (more, drag
 * handle, add, added or the radio glyph: [SongMoreButton], [SongDragHandle], [SongAddButton],
 * [SongRadioMark]).
 *
 * - [current] is `.song.now`: a tonal bed under the row, a bold title (its colour does not change),
 *   and the bars over the dimmed cover, or in place of the number. [playing] animates the bars; false
 *   is `.bars.still`.
 * - [rank] is `.song.ranked` (Recap): the rank before the cover.
 * - [meta] is `.meta`: a duration or "214 plays" before the trailing button, or last.
 * - [lifted] is `.song.lifted`: the row being dragged, raised with a small shadow.
 *
 * - [onSwipeToQueue] lets the row be swiped towards the end to queue the song ([SwipeToQueueCell]).
 *
 * In a sheet the row indents to the sheet's 24dp margin by itself.
 */
@Composable
fun SongRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    lead: SongLead = SongLead.None,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    current: Boolean = false,
    playing: Boolean = false,
    rank: Int? = null,
    meta: String? = null,
    lifted: Boolean = false,
    onSwipeToQueue: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    // A right click opens the menu a long press opens, where a mouse is likely: tablet windows only.
    val rowModifier = if (onLongClick != null && LocalWindowLayout.current.widthClass != WidthClass.Compact) {
        modifier.onSecondaryClick(true, onLongClick)
    } else {
        modifier
    }
    if (onSwipeToQueue == null || lifted) {
        SongRowBody(title, subtitle, rowModifier, lead, onClick, onLongClick, current, playing, rank, meta, lifted, trailing)
    } else {
        SwipeToQueueCell(onSwipeToQueue, rowModifier) {
            SongRowBody(title, subtitle, Modifier, lead, onClick, onLongClick, current, playing, rank, meta, false, trailing)
        }
    }
}

/**
 * Swipe to queue: `.swipe` turned to an action, towards the end only. It uncovers `--fill` with the
 * queue glyph in brass and "Add to queue", which turn to the amber bed once letting go would queue,
 * since amber marks the thing about to happen. The row settles back without overshoot, as the kit's
 * dragged surfaces do. [onQueue] runs on a commit.
 */
@Composable
fun SwipeToQueueCell(onQueue: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = Comfort.colors
    SwipeToQueue(
        onQueue = onQueue,
        settle = spring(dampingRatio = 1f, stiffness = 500f),
        modifier = modifier,
        background = { _, armed ->
            val fill by animateColorAsState(if (armed) colors.accentSoft else colors.fill, tween(160), label = "swipeQueueFill")
            val ink by animateColorAsState(if (armed) colors.onAccentSoft else colors.on, tween(160), label = "swipeQueueInk")
            val glyph by animateColorAsState(if (armed) colors.onAccentSoft else colors.accentInk, tween(160), label = "swipeQueueGlyph")
            Row(
                Modifier.fillMaxSize().background(fill).padding(start = 20.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ComfortIcon(Symbol.QueueMusic, size = 20.dp, tint = glyph)
                Text(stringResource(R.string.menu_add_to_queue), style = onest(14f, 18f, 600), color = ink)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().background(colors.card)) { content() }
    }
}

@Composable
private fun SongRowBody(
    title: String,
    subtitle: String?,
    modifier: Modifier,
    lead: SongLead,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    current: Boolean,
    playing: Boolean,
    rank: Int?,
    meta: String?,
    lifted: Boolean,
    trailing: (@Composable RowScope.() -> Unit)?,
) {
    val colors = Comfort.colors
    val inSheet = LocalComfortContainer.current == ComfortContainer.Sheet
    val start = when {
        inSheet -> 24.dp
        lead == SongLead.None && rank == null -> 20.dp
        lifted -> 10.dp
        else -> 16.dp
    }
    val separator = when {
        inSheet -> 86.dp
        rank != null -> 112.dp
        lead is SongLead.Number -> 58.dp
        lead == SongLead.None -> 20.dp
        else -> 78.dp
    }
    val shape = ComfortShapes.Control
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (lifted) Modifier.padding(horizontal = 6.dp).floatingShadow(shape, small = true).clip(shape).background(colors.raised)
                else Modifier.hairlineAbove(LocalHairlineAbove.current, separator, if (inSheet) 24.dp else 20.dp),
            )
            .then(
                if (current && !lifted) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            colors.accentSoft,
                            topLeft = Offset(6.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 12.dp.toPx(), size.height - 8.dp.toPx()),
                            cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .rowClick(onClick, onLongClick)
            .heightIn(min = 64.dp)
            .padding(start = start, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (rank != null) {
            Text(
                rank.toString(),
                style = onest(17f, 22f, 700).tabular(),
                color = colors.on2,
                textAlign = TextAlign.Center,
                softWrap = false,
                // The column is 20dp as in kit.css; "10" is wider and spills evenly past both sides, as the CSS lets it.
                modifier = Modifier.width(20.dp).wrapContentWidth(unbounded = true),
            )
        }
        when (lead) {
            is SongLead.Cover -> Box(contentAlignment = Alignment.Center) {
                Cover(lead.art, size = 48.dp, dimmed = current)
                if (current) PlayingBars(playing, color = colors.onArt)
            }
            is SongLead.Artist -> ArtistCover(lead.art, size = 48.dp)
            is SongLead.Number -> Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (current) PlayingBars(playing, color = colors.onAccentSoft)
                else Text(lead.number.toString(), style = onest(15f, 20f).tabular(), color = colors.on2)
            }
            SongLead.None -> Unit
        }
        RowText(
            title,
            Modifier.weight(1f),
            subtitle = subtitle,
            titleStyle = if (current) Comfort.type.rowTitle.copy(fontWeight = FontWeight.Bold) else Comfort.type.rowTitle,
            subtitleColor = if (current) colors.onAccentSoft else colors.on2,
        )
        if (meta != null) {
            Text(
                meta,
                style = onest(13f, 18f).tabular(),
                color = if (current) colors.onAccentSoft else colors.on2,
                maxLines = 1,
                modifier = if (trailing == null) Modifier.padding(end = 16.dp) else Modifier,
            )
        }
        if (trailing != null) {
            CompositionLocalProvider(LocalContentColor provides if (current) colors.onAccentSoft else colors.on3) { trailing() }
        }
    }
}

/** The row's more button: `more_vert`, described "More options for {title}". */
@Composable
fun SongMoreButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    ComfortIconButton(Symbol.MoreVert, stringResource(R.string.more_options_for_cd, title), onClick, modifier)

/**
 * The drag handle (queue, reorder mode). It does not drag by itself: pass the reorder library's
 * handle modifier as [modifier]. Described "Drag {title} to reorder".
 */
@Composable
fun SongDragHandle(title: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.drag_to_reorder_cd, title)
    Box(
        modifier.size(48.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { ComfortIcon(Symbol.DragHandle) }
}

/** Add songs: `.ib.add` (`add`, in `--on`) or `.ib.added` (`check`, in accent ink). Describes itself. */
@Composable
fun SongAddButton(title: String, added: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    ComfortIconButton(
        if (added) Symbol.Check else Symbol.Add,
        stringResource(if (added) R.string.add_songs_remove_cd else R.string.add_songs_add_cd, title),
        onToggle,
        modifier,
        tint = if (added) colors.accentInk else colors.on,
    )
}

/** Instant mixes: the trailing `sensors` glyph that says a tap starts radio. Not a button. */
@Composable
fun SongRadioMark(modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) { ComfortIcon(Symbol.Sensors) }
}

/**
 * Marks a queue row smart shuffle added from outside the collection: the radio glyph at 20dp in the
 * row's trailing ink, described "Added by smart shuffle". Sits before the drag handle.
 */
@Composable
fun SongSmartPickMark(modifier: Modifier = Modifier) {
    ComfortIcon(Symbol.Sensors, modifier, size = 20.dp, contentDescription = stringResource(R.string.queue_smart_pick_cd))
}

/**
 * `.bars`: the "now playing" indicator, three bars at 50, 100 and 68% that bounce while [playing] and
 * rest at 26% (`.bars.still`) when paused. 16dp with 3dp bars by default; tiles use 22dp with 4dp bars.
 */
@Composable
fun PlayingBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    barWidth: Dp = 3.dp,
    gap: Dp = 2.5.dp,
    color: Color = LocalContentColor.current,
) {
    val rest = listOf(0.5f, 1f, 0.68f)
    val heights = if (playing) {
        val transition = rememberInfiniteTransition(label = "bars")
        listOf(420, 560, 340).mapIndexed { index, period ->
            val value by transition.animateFloat(
                initialValue = 0.26f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(index * 120)),
                label = "bar$index",
            )
            value
        }
    } else {
        rest.map { 0.26f }
    }
    Canvas(modifier.size(size)) {
        val w = barWidth.toPx()
        val g = gap.toPx()
        val total = w * 3 + g * 2
        var x = (this.size.width - total) / 2
        heights.forEach { fraction ->
            val h = this.size.height * fraction
            drawRoundRect(color, Offset(x, this.size.height - h), Size(w, h), CornerRadius(2.dp.toPx()))
            x += w + g
        }
    }
}

/**
 * `.swipe`: a queue row that swipes aside, either way, to reveal `do_not_disturb_on` and "Remove" in
 * `--danger` on `--fill` on the side it uncovers (`.swipe`, `.swipe.r`), then calls [onRemove]. The
 * swipe starts over when [resetKey] changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToRemove(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val colors = Comfort.colors
    val remove by rememberUpdatedState(onRemove)
    key(resetKey) {
        val state = rememberSwipeToDismissBoxState()
        LaunchedEffect(state.currentValue) {
            if (state.currentValue != SwipeToDismissBoxValue.Settled) remove()
        }
        val towardsStart = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
        SwipeToDismissBox(
            state = state,
            modifier = modifier.hairlineAbove(LocalHairlineAbove.current, 78.dp),
            backgroundContent = {
                Box(
                    Modifier.fillMaxSize().background(colors.fill),
                    contentAlignment = if (towardsStart) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Row(
                        Modifier.padding(start = 20.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ComfortIcon(Symbol.DoNotDisturbOn, size = 20.dp, tint = colors.danger)
                        Text(stringResource(R.string.queue_remove), style = onest(14f, 18f, 600), color = colors.danger)
                    }
                }
            },
        ) {
            val radius = ComfortShapes.ControlRadius
            val shape = when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> androidx.compose.foundation.shape.RoundedCornerShape(topEnd = radius, bottomEnd = radius)
                SwipeToDismissBoxValue.StartToEnd -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = radius, bottomStart = radius)
                SwipeToDismissBoxValue.Settled -> androidx.compose.ui.graphics.RectangleShape
            }
            Box(Modifier.fillMaxWidth().clip(shape).background(colors.card)) {
                CompositionLocalProvider(LocalHairlineAbove provides false) { content() }
            }
        }
    }
}
