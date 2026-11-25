package app.attic.player.ui.expressive

import app.attic.player.ui.Haptic
import app.attic.player.ui.rememberHaptics
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.UpNextRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.NumberFormat

/**
 * The queue (pixel/06-queue-lyrics.html, frames A to C and F). It opens from Now Playing and keeps the
 * cover's scheme, light or dark with the app. The bar names what the queue plays from; "Clear" sits on
 * the "Up next" heading it empties. Up-next rows jump on tap, move by their handle, go with a swipe
 * either way, and open the track menu on a long press.
 */
@Composable
fun ExpressiveQueue(state: QueueState, actions: QueueActions) {
    FromCover(state.current?.mediaId) {
        QueuePage(state, actions)
    }
}

@Composable
private fun QueuePage(state: QueueState, actions: QueueActions) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.colorScheme
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
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            // Swallows taps so they don't reach the player underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding(),
    ) {
        QueueBar(state, actions)
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottom)) {
            if (current != null) {
                item(key = "nowLabel") { ListLabel(stringResource(R.string.queue_now_playing)) }
                item(key = "nowRow") {
                    Box(Modifier.fillMaxWidth().groupCell(0, 1, groupCellColor())) {
                        // The song that is playing does nothing on a tap; its menu is on the button and a long press.
                        SongRow(
                            title = current.title,
                            subtitle = current.artist,
                            lead = RowLead.Cover(Art.of(current.mediaId)),
                            current = true,
                            playing = state.isPlaying,
                            onLongClick = { actions.showMenu(current) },
                            trailing = {
                                if (state.currentSmartPick) RowSmartPickMark()
                                RowMoreButton(current.title) { actions.showMenu(current) }
                            },
                        )
                    }
                }
            }
            if (rows.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.queue_nothing_up_next),
                        body = null,
                        modifier = Modifier.padding(top = 48.dp),
                        symbol = Symbol.QueueMusic,
                        clover = true,
                    )
                }
            } else {
                item(key = "nextHead") {
                    Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.Bottom) {
                        val n = rows.size
                        ListLabel(
                            pluralStringResource(R.plurals.queue_up_next, n, NumberFormat.getIntegerInstance().format(n)),
                            Modifier.weight(1f),
                        )
                        ExpressiveButton(stringResource(R.string.queue_clear), onClick = actions.clearUpNext, variant = ButtonVariant.Text)
                    }
                }
                itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                    ReorderableItem(reorderState, key = row.id) { isDragging ->
                        Box(Modifier.fillMaxWidth().liftableCell(index, rows.size, isDragging)) {
                            SwipeToRemove(onRemove = { actions.remove(row.queueIndex) }, resetKey = state.upNext) {
                                SongRow(
                                    title = row.track.title,
                                    subtitle = row.track.artist,
                                    lead = RowLead.Cover(Art.of(row.track.mediaId)),
                                    lifted = isDragging,
                                    onClick = { actions.jumpTo(row.queueIndex) },
                                    onLongClick = { actions.showMenu(row.track) },
                                    trailing = {
                                        if (row.smartPick) RowSmartPickMark()
                                        RowDragHandle(
                                            row.track.title,
                                            Modifier.draggableHandle(
                                                onDragStarted = {
                                                    haptics.perform(Haptic.PickUp)
                                                    dragging = true
                                                    dragFrom = row.queueIndex
                                                },
                                                onDragStopped = {
                                                    haptics.perform(Haptic.Drop)
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
    }
}

/** Close, "Queue", and under it what the songs come from with the glyph for its kind (`.obar`). */
@Composable
private fun QueueBar(state: QueueState, actions: QueueActions) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveIconButton(Symbol.KeyboardArrowDown, stringResource(R.string.queue_close_cd), actions.close)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.queue_title), style = Expressive.type.titleLargeEmphasized, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                contextSymbol(state.contextKind)?.let {
                    SymbolIcon(it, size = 16.dp, tint = colors.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    contextLabel(state.contextName, state.contextKind),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
