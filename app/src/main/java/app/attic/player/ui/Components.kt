package app.attic.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Track
import app.attic.player.ui.model.PlaybackClock
import kotlinx.coroutines.delay

/** Space lists leave at the bottom so their last row clears the floating mini player and tab bar. */
val LocalBottomPadding = compositionLocalOf { 0.dp }

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun songCount(count: Int) = if (count == 1) "1 song" else "$count songs"

@Composable
fun AtticMark(size: Dp, modifier: Modifier = Modifier) {
    // The roofline spans 60 of the launcher canvas's 108 units, so scale up to drop the margin.
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 108f / 60f),
        )
    }
}

/** Covers from different albums, so a mosaic doesn't repeat the same picture four times. */
fun mosaicIds(tracks: List<Track>): List<Long> =
    tracks.distinctBy { it.albumKey ?: it.key }.map { it.mediaId }

/** Polls [clock] every [intervalMs] while [active], since position isn't pushed as state. */
@Composable
fun rememberPosition(clock: PlaybackClock, trackKey: String?, active: Boolean, intervalMs: Long): Long {
    var position by remember { mutableLongStateOf(clock.positionMs()) }
    LaunchedEffect(trackKey, active) {
        position = clock.positionMs()
        while (active) {
            delay(intervalMs)
            position = clock.positionMs()
        }
    }
    return position
}

/**
 * Whether a loading screen shows its indicator yet: not for the first 400 ms. A normal start has the
 * library before then, so the first frame is only the ground and Home follows; the indicator would
 * only flash. It cost that first frame too: Expressive's morphing `LoadingIndicator` took about 25 ms
 * to compose and a path stencil to draw, all before the app's first frame reached the screen. A first
 * scan that takes longer still shows it.
 */
@Composable
fun rememberLoadingIndicatorShown(): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_INDICATOR_DELAY_MS)
        shown = true
    }
    return shown
}

private const val LOADING_INDICATOR_DELAY_MS = 400L
