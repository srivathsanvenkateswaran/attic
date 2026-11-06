package app.attic.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.attic.player.attic
import app.attic.player.data.LrcParser
import app.attic.player.data.LyricLine
import app.attic.player.data.LyricsEntity
import app.attic.player.data.LyricsResult
import app.attic.player.data.ThemeMode
import app.attic.player.data.Track
import app.attic.player.playback.PlayerUiState
import kotlinx.coroutines.launch

private val SungColor = Color.White
private val UpcomingColor = Color.Black.copy(alpha = 0.75f)

/** Lets the card and the full screen share one download button and its feedback. */
@Composable
private fun DownloadLyricsButton(track: Track) {
    val app = LocalContext.current.attic
    val scope = rememberCoroutineScope()
    var busy by remember(track.key) { mutableStateOf(false) }
    if (busy) {
        CircularProgressIndicator(color = Color.White)
        return
    }
    Button(
        onClick = {
            busy = true
            scope.launch {
                val message = when (app.lyrics.download(track)) {
                    LyricsResult.Found -> null
                    LyricsResult.NotFound -> "No lyrics found for ${track.title}"
                    LyricsResult.Offline -> "Connect to the internet to download lyrics"
                }
                message?.let { app.messages.tryEmit(it) }
                busy = false
            }
        },
    ) {
        Text("Download lyrics")
    }
}

/** Card below the player. It previews the lines being sung, or offers the download. */
@Composable
fun LyricsCard(track: Track, isPlaying: Boolean, tint: Color, onOpen: () -> Unit) {
    val app = LocalContext.current.attic
    val saved by remember(track.key) { app.lyrics.observe(track.key) }.collectAsStateWithLifecycle(null)
    val lines = remember(saved) { saved?.synced?.let(LrcParser::parse).orEmpty() }
    val plain = saved?.plain
    val position = rememberPosition(track.key, isPlaying && lines.isNotEmpty(), 400)
    val hasLyrics = saved?.found == true && !(lines.isEmpty() && plain.isNullOrBlank())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .background(tint.copy(alpha = 0.55f))
            .then(if (hasLyrics) Modifier.pressable(scale = 0.985f, onClick = onOpen) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Lyrics", style = MaterialTheme.typography.titleMedium)
        when {
            hasLyrics && lines.isNotEmpty() -> {
                val current = LrcParser.lineAt(lines, position).coerceAtLeast(0)
                lines.drop(current).take(PREVIEW_LINES).forEachIndexed { i, line ->
                    Text(
                        line.text.ifBlank { "♪" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        color = if (i == 0) SungColor else UpcomingColor,
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
            saved?.instrumental == true -> Text("This song is instrumental.", style = MaterialTheme.typography.bodyLarge)
            else -> {
                Text(
                    if (saved != null) "No lyrics were found for this song." else "Download once and they stay on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                DownloadLyricsButton(track)
            }
        }
    }
}

private const val PREVIEW_LINES = 4

/** Full-screen lyrics on the song's colour. The line being sung stays in view and tapping a line seeks. */
@Composable
fun LyricsScreen(state: PlayerUiState, onClose: () -> Unit) {
    AtticTheme(ThemeMode.Dark) {
        val track = state.current ?: return@AtticTheme
        val app = LocalContext.current.attic
        val rawTint = rememberArtColor(track.mediaId)
        val tint by animateColorAsState(rawTint, tween(600), label = "lyricsTint")
        val saved by remember(track.key) { app.lyrics.observe(track.key) }.collectAsStateWithLifecycle(null)

        Column(
            Modifier
                .fillMaxSize()
                .background(tint)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close lyrics")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(48.dp).padding(end = 48.dp))
            }
            LyricsBody(track, state.isPlaying, saved, Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                PlayCircle(state.isPlaying, { app.player.togglePlayPause() }, size = 56.dp, container = Color.White, content = Color.Black)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LyricsBody(track: Track, isPlaying: Boolean, saved: LyricsEntity?, modifier: Modifier) {
    val client = LocalContext.current.attic.player
    val lines = remember(saved) { saved?.synced?.let(LrcParser::parse).orEmpty() }
    val position = rememberPosition(track.key, isPlaying && lines.isNotEmpty(), 200)
    val current = LrcParser.lineAt(lines, position)
    val list = rememberLazyListState()

    LaunchedEffect(current) {
        if (current >= 2) list.animateScrollToItem(current - 2)
    }

    when {
        lines.isNotEmpty() -> LazyColumn(modifier.fillMaxWidth(), list, PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(lines) { index, line -> SyncedLine(line, index <= current) { client.seekTo(line.timeMs) } }
        }
        !saved?.plain.isNullOrBlank() -> LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            item {
                Text(saved?.plain.orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)
            }
        }
        else -> Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DownloadLyricsButton(track)
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
