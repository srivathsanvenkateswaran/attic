package app.attic.player.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.attic.player.data.Track
import app.attic.player.attic
import app.attic.player.playback.PlayerUiState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** [queueIndex] is the position in the player, which stays valid while rows are dragged around it. */
private data class UpNextRow(val id: String, val track: Track, val queueIndex: Int)

/** The same song can be queued twice, so a row's identity is its key plus which occurrence it is. */
private fun buildRows(state: PlayerUiState): List<UpNextRow> {
    val seen = HashMap<String, Int>()
    return state.queue.mapIndexedNotNull { index, track ->
        val occurrence = seen.merge(track.key, 1, Int::plus)!!
        if (index > state.currentIndex) UpNextRow("${track.key}#$occurrence", track, index) else null
    }
}

@Composable
fun QueueScreen(state: PlayerUiState, onClose: () -> Unit) {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    val current = state.current

    val rows = remember { mutableStateListOf<UpNextRow>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.queue, state.currentIndex, dragging) {
        if (!dragging) {
            rows.clear()
            rows.addAll(buildRows(state))
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
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close queue")
            }
            Text("Queue", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (rows.isNotEmpty()) {
                TextButton(onClick = { app.player.clearUpNext() }) { Text("Clear") }
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
                        onMenu = { ui.showMenu(TrackMenuRequest(current)) },
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
                            app.player.removeFromQueue(row.queueIndex)
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
                            onClick = { app.player.jumpTo(row.queueIndex) },
                            onMenu = { ui.showMenu(TrackMenuRequest(row.track)) },
                            trailing = {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            dragging = true
                                            dragFrom = row.queueIndex
                                        },
                                        onDragStopped = {
                                            val target = state.currentIndex + 1 + rows.indexOfFirst { it.id == row.id }
                                            if (dragFrom >= 0 && dragFrom != target) app.player.moveInQueue(dragFrom, target)
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
