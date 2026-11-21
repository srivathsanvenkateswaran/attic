package app.attic.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.Track

/** Space lists leave at the bottom so their last row clears the floating mini player and tab bar. */
val LocalBottomPadding = compositionLocalOf { 0.dp }

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** "17 songs, 1 hr 2 min" */
fun summarize(tracks: List<Track>): String {
    val minutes = tracks.sumOf { it.durationMs } / 60_000
    val length = if (minutes >= 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"
    return "${songCount(tracks.size)}, $length"
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
