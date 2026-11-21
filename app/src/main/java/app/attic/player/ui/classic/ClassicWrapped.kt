package app.attic.player.ui.classic

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.attic.player.data.LanguageLabels
import app.attic.player.ui.*
import app.attic.player.ui.model.*
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClassicWrapped(state: WrappedPageState, actions: WrappedPageActions) {
    val period = state.period

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomPadding.current)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(actions.back)
                Spacer(Modifier.width(4.dp))
                Text("Wrapped", style = MaterialTheme.typography.titleLarge)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(RecapPeriod.entries) { option ->
                    PillChip(option.label, option == period) { actions.setPeriod(option) }
                }
            }
        }

        val loaded = state.data
        when {
            loaded == null || state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            loaded.recap.isEmpty -> item {
                EmptyMessage(
                    "Nothing played ${period.label.lowercase()} yet",
                    "Any song you play for 30 seconds or more counts. Your top songs, artists and listening clock build up from there.",
                )
            }
            else -> wrappedContent(state, actions, loaded)
        }
    }

}

private fun androidx.compose.foundation.lazy.LazyListScope.wrappedContent(state: WrappedPageState, actions: WrappedPageActions, data: WrappedData) {
    val recap = data.recap
    val cards = state.cards
    val onOpenStory = actions.openStory
    val numbers = NumberFormat.getIntegerInstance()

    item {
        StoryBanner(data, onPlay = { onOpenStory(0) })
    }
    item {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Minutes", numbers.format(recap.minutes), Modifier.weight(1f))
                StatTile("Plays", numbers.format(recap.plays), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Songs", numbers.format(recap.songCount), Modifier.weight(1f))
                StatTile("Artists", numbers.format(recap.artistCount), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Days with music", numbers.format(recap.daysListened), Modifier.weight(1f))
                StatTile("Longest streak", if (recap.longestStreak == 1) "1 day" else "${recap.longestStreak} days", Modifier.weight(1f))
            }
        }
    }

    item {
        SectionTitle("Share a card")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            itemsIndexed(cards) { index, card ->
                WrappedCardView(
                    card,
                    data,
                    Modifier
                        .width(118.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pressable { onOpenStory(index) },
                )
            }
        }
    }

    if (data.songs.isNotEmpty()) {
        item { TopSongsSection(state, actions, data) }
    }
    if (data.artists.isNotEmpty()) {
        item { TopArtistsSection(actions, data) }
    }
    if (recap.languages.isNotEmpty()) {
        item { LanguagesSection(data) }
    }
    item { ClockSection(data) }
    recap.loggedSince?.let { since ->
        item {
            val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
            Text(
                "Counted from Attic's listening log, which began on ${date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))}. A song counts as a play after 30 seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp),
            )
        }
    }
}

/** Shares the intro card's colours so the way in looks like the story it opens. */
@Composable
private fun StoryBanner(data: WrappedData, onPlay: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A0E3D))
            .pressable(scale = 0.98f, onClick = onPlay)
            .padding(20.dp),
    ) {
        Text(data.headline, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFFFB627))
        Spacer(Modifier.height(6.dp))
        Text(
            "${NumberFormat.getIntegerInstance().format(data.recap.minutes)} minutes of music, told in ${WrappedCard.available(data).size} cards you can share.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.clip(CircleShape).background(Color(0xFFFF5D8F)).padding(start = 12.dp, end = 18.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF2A0E3D))
            Spacer(Modifier.width(4.dp))
            Text("Play your story", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2A0E3D))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TopSongsSection(state: WrappedPageState, actions: WrappedPageActions, data: WrappedData) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Top songs", Modifier.weight(1f))
            PlayCircle(
                isPlaying = state.topSongsPlaying,
                onClick = actions.playTopSongs,
                modifier = Modifier.padding(end = 16.dp, top = 14.dp),
            )
        }
        data.songs.forEachIndexed { index, (track, ranked) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.985f, onLongClick = { actions.showMenu(track) }) { actions.playTopSong(index) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                CoverArt(track.mediaId, RoundedCornerShape(4.dp), Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.currentKey == track.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (ranked.plays == 1) "1 play" else "${ranked.plays} plays", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TopArtistsSection(actions: WrappedPageActions, data: WrappedData) {
    Column {
        SectionTitle("Top artists")
        data.artists.forEachIndexed { index, (artist, ranked) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.985f) { actions.openArtist(artist) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                CoverArt(data.artistCovers[artist.key], CircleShape, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${NumberFormat.getIntegerInstance().format(ranked.listenedMs / 60_000)} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LanguagesSection(data: WrappedData) {
    val languages = data.recap.languages
    Column {
        SectionTitle("Languages")
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(18.dp)
                .clip(CircleShape),
        ) {
            languages.forEach { share ->
                Box(Modifier.weight(share.share.coerceAtLeast(0.01f)).fillMaxSize().background(languageColor(share.language)))
            }
        }
        Spacer(Modifier.height(12.dp))
        languages.forEach { share ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(languageColor(share.language)))
                Spacer(Modifier.width(10.dp))
                Text(LanguageLabels.displayName(share.language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("${(share.share * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ClockSection(data: WrappedData) {
    val recap = data.recap
    val peak = (0 until 24).maxByOrNull { recap.hours[it] } ?: 0
    Column {
        SectionTitle("When you listen")
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ListeningClock(recap.hours, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceContainerHighest, Modifier.size(132.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(recap.listenerType.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${recap.listenerType.line} Your busiest hour starts at ${hourLabel(peak)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Full-screen cards that advance on their own. Tap the left side to go back, hold to pause. */
@Composable
fun ClassicWrappedStory(state: WrappedStoryState, actions: WrappedStoryActions) {
    val cards = state.cards
    val page = state.page
    val card = cards[page]
    val layer = rememberGraphicsLayer()
    val current by rememberUpdatedState(actions)
    val capture: suspend () -> Bitmap = { layer.toImageBitmap().asAndroidBitmap() }

    Dialog(onDismissRequest = actions.close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = actions.close)
        Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                cards.indices.forEach { index ->
                    val filled = when {
                        index < page -> 1f
                        index == page -> state.progress()
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(filled).fillMaxSize().background(Color.White))
                    }
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val width = min(maxWidth - 24.dp, (maxHeight - 8.dp) * 9f / 16f)
                WrappedCardView(
                    card,
                    state.data,
                    Modifier
                        .width(width)
                        .clip(RoundedCornerShape(10.dp))
                        .pointerInput(cards.size) {
                            detectTapGestures(
                                onPress = {
                                    current.setHeld(true)
                                    tryAwaitRelease()
                                    current.setHeld(false)
                                },
                                onTap = { offset -> current.tap(offset.x < size.width / 3) },
                            )
                        },
                    layer = layer,
                    animate = true,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryButton(Icons.Rounded.Close, "Close", Modifier, onClick = actions.close)
                Spacer(Modifier.weight(1f))
                StoryButton(Icons.Rounded.Download, "Save", Modifier) { actions.save(capture) }
                StoryButton(Icons.Rounded.IosShare, "Share", Modifier, primary = true) { actions.share(capture) }
            }
        }
    }
}

@Composable
private fun StoryButton(icon: ImageVector, label: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier
            .pressable(scale = 0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.14f))
            .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) Color.Black else Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (primary) Color.Black else Color.White)
    }
}
