package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.model.InterestChoice
import app.attic.player.ui.model.InterestsActions
import app.attic.player.ui.model.InterestsState
import app.attic.player.ui.model.MoodActions
import app.attic.player.ui.model.MoodState
import app.attic.player.ui.model.MoodTile
import app.attic.player.ui.rememberArtColor
import java.text.NumberFormat

/**
 * A mood on Home's Moods shelf (design: `Mood tile`): BrowseTile's tonal container in the mix's first
 * cover's hue, 200 by 108, with the name and count over the mood's one line, and the mosaic at the end.
 */
@Composable
fun MoodTile(tile: MoodTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val seed = rememberArtColor(tile.tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    FromArt(seed) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier
                .width(200.dp)
                .height(108.dp)
                .clip(RoundedCornerShape(ExpressiveShapes.LargeIncreased))
                .background(colors.secondaryContainer)
                .combinedClickableCompat(onClick)
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    NarrowingText(
                        tile.name,
                        style = Expressive.type.titleMediumEmphasized.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = colors.onSecondaryContainer,
                    )
                    Text(
                        moodSongCount(tile.tracks.size),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSecondaryContainer,
                        maxLines = 1,
                    )
                }
                Text(
                    tile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Mosaic(tile.mosaicIds.map(Art::Song), Modifier.size(56.dp), shape = RoundedCornerShape(ExpressiveShapes.Medium))
        }
    }
}

/**
 * The card at the start of the Moods shelf asking for picks (design: `Mood picks card`): a full-width
 * container, its question and line, then Choose and Not now.
 */
@Composable
fun MoodPicksCard(onChoose: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainerHigh)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
    ) {
        Text(stringResource(R.string.home_moods_prompt_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium))
        Text(
            stringResource(R.string.home_moods_prompt_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveButton(stringResource(R.string.home_moods_prompt_choose), onChoose)
            ExpressiveButton(stringResource(R.string.home_moods_prompt_dismiss), onDismiss, variant = ButtonVariant.Text)
        }
    }
}

@Composable
private fun moodSongCount(count: Int): String =
    pluralStringResource(R.plurals.song_count, count, NumberFormat.getIntegerInstance().format(count))

/**
 * A mood's page: the genre page's layout with the mood's two lines under the title and Mood radio in
 * the radio button's place. No add-to-playlist: the mix is new each day.
 */
@Composable
fun ExpressiveMood(state: MoodState, actions: MoodActions) {
    val tile = state.tile
    if (tile == null) {
        // While the mixes are first built there is nothing to say yet; after that, the mood has none today.
        if (!state.loading) GoneState(R.string.mood_gone, Symbol.QueueMusic, cookie = false, actions.back)
        return
    }
    val tracks = tile.tracks
    val seed = rememberArtColor(tracks.firstOrNull()?.mediaId, fallback = Color.Unspecified)
    CollectionFrame(
        title = tile.name,
        seed = seed,
        controls = state.controls,
        controlActions = actions.controls,
        onBack = actions.back,
        header = { titleModifier ->
            CollectionHeader(
                HeaderParts(
                    title = tile.name,
                    meta = listOf(stringResource(R.string.mood_kind, songsAndDuration(tracks)), stringResource(R.string.mood_note)),
                    art = { Mosaic(tile.mosaicIds.map(Art::Song), it) },
                    name = tile.name,
                    controls = state.controls,
                    controlActions = actions.controls,
                    radio = actions.startRadio,
                    radioLabel = stringResource(R.string.mood_radio),
                ),
                titleModifier,
            )
        },
    ) {
        item(key = "gap") { Spacer(Modifier.height(12.dp)) }
        songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu, onQueue = actions.queue)
    }
}

/**
 * Moods and interests: full screen like the first-run pages, with no tabs or mini player, so Save can
 * stay at the bottom. The large title goes into the bar as the list scrolls, as Settings does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressiveInterests(state: InterestsState, actions: InterestsActions) {
    val listState = rememberLazyListState()
    val titleGone = with(LocalDensity.current) { 44.dp.toPx() }
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleGone }
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DetailAppBar(
            title = if (collapsed) stringResource(R.string.interests_title) else "",
            onBack = actions.back,
            surface = if (collapsed) AppBarSurface.Scrolled else AppBarSurface.Plain,
        )
        // On a tablet the page keeps to a readable column, as Settings does there.
        LazyColumn(Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth(), state = listState) {
            item(key = "title") {
                Text(
                    stringResource(R.string.interests_title),
                    style = Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
                Text(
                    stringResource(R.string.interests_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
            item(key = "moods") { ChoiceGroup(stringResource(R.string.interests_moods), state.moods, actions.toggleMood) }
            item(key = "genres") {
                ChoiceGroup(stringResource(R.string.interests_genres), state.genres, actions.toggleGenre, empty = stringResource(R.string.interests_genres_empty))
            }
            item(key = "languages") { ChoiceGroup(stringResource(R.string.interests_languages), state.languages, actions.toggleLanguage) }
            item(key = "end") { Spacer(Modifier.height(24.dp)) }
        }
        ExpressiveButton(
            stringResource(R.string.interests_save),
            actions.save,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            size = ButtonSize.Medium,
        )
    }
}

/** A group's label in the primary colour, as Settings' sections, then its chips wrapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGroup(title: String, choices: List<InterestChoice>, onToggle: (String) -> Unit, empty: String? = null) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
    )
    if (choices.isEmpty() && empty != null) {
        Text(
            empty,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        return
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        for (choice in choices) {
            ExpressiveFilterChip(choice.label, choice.selected, onClick = { onToggle(choice.id) })
        }
    }
}
