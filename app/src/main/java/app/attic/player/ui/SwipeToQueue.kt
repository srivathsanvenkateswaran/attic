package app.attic.player.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * The numbers behind swiping a song row to queue it. The swipe goes one way only, towards the end of
 * the line (right, in a left-to-right language), and the row springs back either way. The queue's own
 * rows swipe to remove instead and never take this gesture, so the two don't meet.
 */
object SwipeQueue {
    /** Let go past this share of the row's width and the song is queued. */
    const val COMMIT_FRACTION = 0.3f

    /** The row follows the finger to here, then resists, and goes no further than [MAX_FRACTION]. */
    const val MAX_FRACTION = 0.5f

    /** Past the commit point the row moves this much per pixel of finger. */
    const val RESISTANCE = 0.35f

    /** A quick flick queues from a shorter swipe: at least this fast, in dp a second... */
    const val FLING_DP_PER_SECOND = 1_000f

    /** ...and at least this share of the width. */
    const val FLING_MIN_FRACTION = 0.12f

    /** Where the row goes when the finger moves [delta] pixels from [offset]. Never towards the start. */
    fun drag(offset: Float, delta: Float, width: Int): Float {
        if (width <= 0) return 0f
        val commit = width * COMMIT_FRACTION
        val max = width * MAX_FRACTION
        // Below the commit point a pixel of finger is a pixel of row; above it, a fraction.
        val finger = if (offset <= commit) offset else commit + (offset - commit) / RESISTANCE
        val moved = (finger + delta).coerceAtLeast(0f)
        val row = if (moved <= commit) moved else commit + (moved - commit) * RESISTANCE
        return row.coerceAtMost(max)
    }

    /** True once letting go now would queue the song, ignoring speed. The swipe's background shows it. */
    fun armed(offset: Float, width: Int): Boolean = width > 0 && offset >= width * COMMIT_FRACTION

    /** Whether letting go at [offset] with [velocity] (pixels a second, positive towards the end) queues. */
    fun commits(offset: Float, width: Int, velocity: Float, density: Float): Boolean =
        armed(offset, width) || (width > 0 && velocity >= FLING_DP_PER_SECOND * density && offset >= width * FLING_MIN_FRACTION)

    /** 0 at rest to 1 at the commit point, for drawing the background. */
    fun progress(offset: Float, width: Int): Float =
        if (width <= 0) 0f else (offset / (width * COMMIT_FRACTION)).coerceIn(0f, 1f)
}

/**
 * Lets a song row be swiped towards the end to add it to the queue. Behind the row, [background]
 * draws what the swipe uncovers, told how far along it is and whether letting go would queue. On a
 * commit the phone gives [Haptic.Confirm] and [onQueue] runs; the row then settles back with
 * [settle], the kit's own motion. With [onQueue] null the row is drawn as it is, with no gesture.
 */
@Composable
fun SwipeToQueue(
    onQueue: (() -> Unit)?,
    settle: AnimationSpec<Float>,
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.(progress: Float, armed: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    if (onQueue == null) {
        Box(modifier) { content() }
        return
    }
    val queue by rememberUpdatedState(onQueue)
    val haptics = rememberHaptics()
    val density = LocalDensity.current.density
    var width by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    val state = rememberDraggableState { delta -> offset = SwipeQueue.drag(offset, delta, width) }
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .draggable(
                state = state,
                orientation = Orientation.Horizontal,
                // In a right-to-left language the end is on the left, and the swipe goes that way.
                reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                onDragStopped = { velocity ->
                    if (SwipeQueue.commits(offset, width, velocity, density)) {
                        haptics.perform(Haptic.Confirm)
                        queue()
                    }
                    // A new touch takes the row back from the settle rather than waiting for it.
                    state.drag(MutatePriority.Default) {
                        animate(offset, 0f, initialVelocity = velocity, animationSpec = settle) { value, _ -> offset = value }
                    }
                },
            ),
    ) {
        if (offset > 0f) {
            Box(Modifier.matchParentSize()) { background(SwipeQueue.progress(offset, width), SwipeQueue.armed(offset, width)) }
        }
        // offset {} mirrors in right-to-left, so a positive x always moves the row towards the end.
        Box(Modifier.offset { IntOffset(offset.roundToInt(), 0) }) { content() }
    }
}
