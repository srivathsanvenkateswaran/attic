package app.attic.player.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.attic.player.ui.*
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState
import app.attic.player.ui.model.UpNextRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ClassicQueue(state: QueueState, actions: QueueActions) {
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
    var dragFrom by remember { mutableStateOf(-1) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = rows.indexOfFirst { it.id == from.key }
        val toIndex = rows.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) rows.add(toIndex, rows.removeAt(fromIndex))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.close) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close queue")
            }
            Text("Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (rows.isNotEmpty()) {
                TextButton(onClick = actions.clearUpNext) { Text("Clear") }
            }
        }

        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            if (current != null) {
                item(key = "now-title") { SmallHeading("Now playing") }
                item(key = "now-row") {
                    TrackRow(
                        track = current,
                        isCurrent = true,
                        isPlaying = state.isPlaying,
                        onClick = {},
                        onMenu = { actions.showMenu(current) },
                    )
                }
            }
            item(key = "next-title") {
                SmallHeading(if (rows.isEmpty()) "Nothing up next" else "Up next, ${songCount(rows.size)}")
            }
            items(rows, key = { it.id }) { row ->
                ReorderableItem(reorderState, key = row.id) { isDragging ->
                    val dismiss = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismiss.currentValue) {
                        if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) {
                            actions.remove(row.queueIndex)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismiss,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text("Remove", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                    ) {
                        TrackRow(
                            track = row.track,
                            isCurrent = false,
                            isPlaying = false,
                            modifier = Modifier.background(
                                if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.background,
                            ),
                            onClick = { actions.jumpTo(row.queueIndex) },
                            onMenu = { actions.showMenu(row.track) },
                            trailing = {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
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
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Drag ${row.track.title} to reorder")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
