package app.attic.player.ui.classic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.data.LrcParser
import app.attic.player.data.LyricLine
import app.attic.player.data.ThemeMode
import app.attic.player.data.Track
import app.attic.player.data.UiStyle
import app.attic.player.ui.*
import app.attic.player.ui.model.*

private val SungColor = Color.White
private val UpcomingColor = Color.Black.copy(alpha = 0.75f)

/** The card and the full screen draw the same download button, with a spinner while it works. */
@Composable
private fun DownloadLyricsButton(state: LyricsState, actions: LyricsActions) {
    if (state.downloading) {
        CircularProgressIndicator(color = Color.White)
        return
    }
    Button(onClick = actions.download) {
        Text("Download lyrics")
    }
}

/** Card below the player. It previews the lines being sung, or offers the download. */
@Composable
fun LyricsCard(state: LyricsState, actions: LyricsActions, clock: PlaybackClock, track: Track, isPlaying: Boolean, tint: Color, onOpen: () -> Unit) {
    val lines = state.lines
    val plain = state.plain
    val position = rememberPosition(clock, track.key, isPlaying && lines.isNotEmpty(), 400)
    val hasLyrics = state.hasLyrics

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f).compositeOver(tint))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Lyrics", style = MaterialTheme.typography.titleSmall)
        when {
            hasLyrics && lines.isNotEmpty() -> {
                val current = LrcParser.lineAt(lines, position).coerceAtLeast(0)
                lines.drop(current).take(PREVIEW_LINES).forEachIndexed { i, line ->
                    Text(
                        line.text.ifBlank { "♪" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        color = if (i == 0) SungColor else Color.White.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            hasLyrics -> Text(
                plain.orEmpty(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                maxLines = PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            state.instrumental -> Text("This song is instrumental.", style = MaterialTheme.typography.bodyLarge)
            else -> {
                Text(
                    if (state.checked) "No lyrics were found for this song." else "Download once and they stay on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                DownloadLyricsButton(state, actions)
            }
        }
        if (hasLyrics) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .pressable(scale = 0.94f, onClick = onOpen)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Show lyrics", style = MaterialTheme.typography.labelLarge, color = Color.Black)
            }
        }
    }
}

/** The line being sung, shown small under the cover once lyrics are saved. */
@Composable
fun CurrentLyricLine(state: LyricsState, clock: PlaybackClock, track: Track, isPlaying: Boolean) {
    val lines = state.lines
    val position = rememberPosition(clock, track.key, isPlaying && lines.isNotEmpty(), 400)
    val line = lines.getOrNull(LrcParser.lineAt(lines, position))?.text?.takeIf { it.isNotBlank() } ?: return
    Text(
        line,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

private const val PREVIEW_LINES = 4

/** Full-screen lyrics on the song's colour. The line being sung stays in view and tapping a line seeks. */
@Composable
fun ClassicLyricsScreen(state: LyricsScreenState, actions: LyricsScreenActions) {
    AtticTheme(ThemeMode.Dark, UiStyle.Classic) {
        val track = state.track ?: return@AtticTheme
        val rawTint = rememberArtColor(track.mediaId)
        val tint by animateColorAsState(rawTint, tween(600), label = "lyricsTint")

        Column(
            Modifier
                .fillMaxSize()
                .background(tint)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.close) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close lyrics")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(48.dp).padding(end = 48.dp))
            }
            LyricsBody(state, actions, track, Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                PlayCircle(state.isPlaying, actions.togglePlayPause, size = 56.dp, container = Color.White, content = Color.Black)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LyricsBody(state: LyricsScreenState, actions: LyricsScreenActions, track: Track, modifier: Modifier) {
    val lines = state.lyrics.lines
    val position = rememberPosition(state.clock, track.key, state.isPlaying && lines.isNotEmpty(), 200)
    val current = LrcParser.lineAt(lines, position)
    val list = rememberLazyListState()

    LaunchedEffect(current) {
        if (current >= 2) list.animateScrollToItem(current - 2)
    }

    when {
        lines.isNotEmpty() -> LazyColumn(modifier.fillMaxWidth(), list, PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(lines) { index, line -> SyncedLine(line, index <= current) { actions.seekTo(line.timeMs) } }
        }
        !state.lyrics.plain.isNullOrBlank() -> LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            item {
                Text(state.lyrics.plain.orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)
            }
        }
        else -> Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DownloadLyricsButton(state.lyrics, actions.lyrics)
        }
    }
}

@Composable
private fun SyncedLine(line: LyricLine, sung: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (sung) SungColor else UpcomingColor, tween(240), label = "line")
    Text(
        line.text.ifBlank { "♪" },
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 33.sp,
        color = color,
        modifier = Modifier.fillMaxWidth().pressable(scale = 0.98f, onClick = onClick),
    )
}
