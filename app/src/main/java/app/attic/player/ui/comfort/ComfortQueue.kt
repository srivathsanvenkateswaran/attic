package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Track
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.contextLabel
import app.attic.player.ui.expressive.contextSymbol
import app.attic.player.ui.model.PlaybackClock
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.UpNextRow
import app.attic.player.ui.rememberPosition
import java.text.NumberFormat
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The queue (oneui/06-queue-lyrics.html, frames A to C and F). It stands on Now Playing's washed
 * ground, light or dark with the app. The current song is the mini player's filled row under "Now
 * playing", whose label ends with where the queue came from; everything after it is one card of rows
 * with handles. Up-next rows jump on tap, move by their handle, go with a swipe either way, and open
 * the track menu on a long press. "Clear" is a pill at the bottom edge, over a fade to the ground.
 */
@Composable
fun ComfortQueue(state: QueueState, actions: QueueActions) {
    TintedCover(state.current?.mediaId, wash = true) {
        QueuePage(state, actions)
    }
}

@Composable
private fun QueuePage(state: QueueState, actions: QueueActions) {
    val colors = Comfort.colors
    val current = state.current

    // The rows follow the queue, except mid-drag, when they hold the order being dragged.
    val rows = remember { mutableStateListOf<UpNextRow>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.upNext, dragging) {
        if (!dragging) {
            rows.clear()
            rows.addAll(state.upNext)
        }
    }
    val listState = rememberLazyListState()
    var dragFrom by remember { mutableIntStateOf(-1) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.id == from.key }
        val toIndex = rows.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) rows.add(toIndex, rows.removeAt(fromIndex))
    }
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            // Swallows taps so they don't reach the player underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = top, bottom = navigation + if (rows.isEmpty()) 24.dp else 118.dp),
        ) {
            if (current != null) {
                item(key = "nowLabel") {
                    Label(
                        stringResource(R.string.queue_now_playing),
                        tight = true,
                        end = {
                            contextSymbol(state.contextKind)?.let { ComfortIcon(it, size = 18.dp) }
                            Text(
                                contextLabel(state.contextName, state.contextKind),
                                style = onest(14f, 18f, 500),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
                item(key = "nowRow") { NowPlayingRow(current, state, actions) }
            }
            if (rows.isEmpty()) {
                item(key = "empty") {
                    EmptyState(stringResource(R.string.queue_nothing_up_next), symbol = Symbol.QueueMusic)
                }
            } else {
                item(key = "nextLabel") {
                    val n = rows.size
                    Label(pluralStringResource(R.plurals.queue_up_next, n, NumberFormat.getIntegerInstance().format(n)))
                }
                itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                    ReorderableItem(reorderState, key = row.id) { isDragging ->
                        GroupCell(index, rows.size) {
                            SwipeToRemove(onRemove = { actions.remove(row.queueIndex) }, resetKey = state.upNext) {
                                SongRow(
                                    title = row.track.title,
                                    subtitle = row.track.artist,
                                    lead = SongLead.Cover(Art.of(row.track.mediaId)),
                                    lifted = isDragging,
                                    onClick = { actions.jumpTo(row.queueIndex) },
                                    onLongClick = { actions.showMenu(row.track) },
                                    trailing = {
                                        SongDragHandle(
                                            row.track.title,
                                            Modifier.draggableHandle(
                                                onDragStarted = {
                                                    dragging = true
                                                    dragFrom = row.queueIndex
                                                },
                                                onDragStopped = {
                                                    actions.move(dragFrom, rows.indexOfFirst { it.id == row.id })
                                                    dragFrom = -1
                                                    dragging = false
                                                },
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().background(colors.ground)) {
            Bar(
                stringResource(R.string.queue_title),
                onBack = actions.close,
                backSymbol = Symbol.ExpandMore,
                backDescription = stringResource(R.string.queue_close_cd),
            )
        }
        if (rows.isNotEmpty()) ClearFoot(actions.clearUpNext, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * `.q-now`: the song that is playing as the mini player's row: the cover dimmed under the bars, the
 * title in ink, and the row filled from the left as far as the song has played. A tap does nothing;
 * the menu is on the button and a long press.
 */
@Composable
private fun NowPlayingRow(track: Track, state: QueueState, actions: QueueActions) {
    val colors = Comfort.colors
    val progress = rememberProgress(state.clock, track, state.isPlaying)
    Box(
        Modifier
            .padding(horizontal = GroupMargin)
            .fillMaxWidth()
            .clip(ComfortShapes.Group)
            .background(colors.miniBase)
            .drawBehind {
                val width = size.width * progress().coerceIn(0f, 1f)
                if (width > 0f) {
                    val r = 22.dp.toPx()
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f, top = 0f, right = width, bottom = size.height,
                                topLeftCornerRadius = CornerRadius.Zero,
                                topRightCornerRadius = CornerRadius(r),
                                bottomRightCornerRadius = CornerRadius(r),
                                bottomLeftCornerRadius = CornerRadius.Zero,
                            ),
                        )
                    }
                    drawPath(path, colors.miniFill)
                }
            },
    ) {
        // The fill takes the place of the current row's tonal bed, and the second line takes the mini player's ink.
        ProvideComfortColors(colors.copy(accentSoft = Color.Transparent, onAccentSoft = colors.miniInk2)) {
            OnCard {
                SongRow(
                    title = track.title,
                    subtitle = track.artist,
                    modifier = Modifier.heightIn(min = 72.dp),
                    lead = SongLead.Cover(Art.of(track.mediaId)),
                    current = true,
                    playing = state.isPlaying,
                    onLongClick = { actions.showMenu(track) },
                    trailing = { SongMoreButton(track.title, { actions.showMenu(track) }) },
                )
            }
        }
    }
}

/** How far [track] has played, from 0 to 1, read where it is drawn. */
@Composable
internal fun rememberProgress(clock: PlaybackClock?, track: Track, playing: Boolean): () -> Float {
    if (clock == null) return { 0f }
    val position = rememberPosition(clock, track.key, playing, 500)
    val duration = track.durationMs.coerceAtLeast(1)
    return { position.toFloat() / duration }
}

/** `.q-foot`: "Clear" as a raised pill at the bottom centre, over a fade to the ground. */
@Composable
private fun ClearFoot(onClear: () -> Unit, modifier: Modifier) {
    val colors = Comfort.colors
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(118.dp + navigation)
            .background(Brush.verticalGradient(0f to Color.Transparent, 62f / 118f to colors.ground))
            .padding(bottom = navigation + 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // `.q-foot .btn`: the plain button on the raised surface, with the small shadow and the edge.
        ProvideComfortColors(colors.copy(fill = colors.raised)) {
            ComfortButton(
                stringResource(R.string.queue_clear),
                onClear,
                Modifier.floatingShadow(ComfortShapes.Pill, small = true, edge = true),
                style = ButtonStyle.Plain,
                leading = Symbol.ClearAll,
            )
        }
    }
}
