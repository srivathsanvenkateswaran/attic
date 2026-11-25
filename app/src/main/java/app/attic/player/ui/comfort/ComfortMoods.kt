package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.adaptive.WidthClass
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.model.InterestChoice
import app.attic.player.ui.model.InterestsActions
import app.attic.player.ui.model.InterestsState
import app.attic.player.ui.model.MoodActions
import app.attic.player.ui.model.MoodState
import app.attic.player.ui.model.MoodTile

/**
 * A mood on Home's Moods shelf (design: `Mood tile`): Search's browse card at 200 by 133, the mosaic
 * top left and the count top right, then the name and the mood's one line.
 */
@Composable
fun MoodTile(tile: MoodTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Column(
        modifier
            .width(200.dp)
            .height(133.dp)
            .clip(ComfortShapes.Group)
            .background(colors.card)
            .rowClick(onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Mosaic(tile.mosaicIds.map(Art::Song), Modifier.size(56.dp))
            Spacer(Modifier.weight(1f))
            Text(songCount(tile.tracks.size), style = Comfort.type.caption, color = colors.on2, maxLines = 1)
        }
        Column {
            Text(tile.name, style = onest(16f, 21f, 600), color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tile.description, style = Comfort.type.caption, color = colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The card at the start of the Moods shelf asking for picks (design: `Mood picks card`). */
@Composable
fun MoodPicksCard(onChoose: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Column(
        modifier
            .padding(horizontal = GroupMargin)
            .fillMaxWidth()
            .clip(ComfortShapes.Group)
            .background(colors.card)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
    ) {
        Text(stringResource(R.string.home_moods_prompt_title), style = onest(17f, 22f, 600), color = colors.on)
        Text(
            stringResource(R.string.home_moods_prompt_body),
            style = onest(14f, 20f),
            color = colors.on2,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ComfortButton(stringResource(R.string.home_moods_prompt_choose), onChoose)
            ComfortButton(stringResource(R.string.home_moods_prompt_dismiss), onDismiss, style = ButtonStyle.Text)
        }
    }
}

/**
 * A mood's page: the genre page's centred header with the mood's two lines, then Play, shuffle and
 * Mood radio. No add-to-playlist: the mix is new each day.
 */
@Composable
fun ComfortMood(state: MoodState, actions: MoodActions) {
    if (LocalWindowLayout.current.widthClass != WidthClass.Compact && !LocalStackedCollection.current) return ComfortMoodPanes(state, actions)
    val tile = state.tile
    if (tile == null) {
        // While the mixes are first built there is nothing to say yet; after that, the mood has none today.
        if (!state.loading) GoneState(R.string.mood_gone, Symbol.QueueMusic, actions.back)
        return
    }
    val tracks = tile.tracks
    CollectionFrame(title = tile.name, onBack = actions.back, titleTop = HeaderArt) {
        item(key = "header") {
            CollectionHeader(
                title = tile.name,
                art = { m -> Mosaic(tile.mosaicIds.map(Art::Song), m) },
                meta = stringResource(R.string.mood_kind, songsAndLength(tracks)) + "\n" + stringResource(R.string.mood_note),
                artSize = HeaderArt,
            )
        }
        item(key = "actions") {
            PlayRow(tile.name, state.controls, actions.controls, radio = actions.startRadio, radioLabel = stringResource(R.string.mood_radio), addToPlaylist = false)
        }
        item(key = "gap") { Spacer(Modifier.height(16.dp)) }
        songRows(tracks, state.currentKey, state.isPlaying, onPlay = { actions.play(it) }, onMenu = actions.showMenu, onQueue = actions.queue)
    }
}

/**
 * Moods and interests: Settings' large title and back row, each group's chips on a card under its
 * label, and Save pinned at the bottom. Full screen, as the first-run pages are, so the dock doesn't
 * cover Save.
 */
@Composable
fun ComfortInterests(state: InterestsState, actions: InterestsActions) {
    val colors = Comfort.colors
    val listState = rememberLazyListState()
    val title = stringResource(R.string.interests_title)
    Column(Modifier.fillMaxSize().background(colors.ground).navigationBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            RootPage(title = title, listState = listState, modifier = Modifier.widthIn(max = MAX_WIDTH), onBack = actions.back) {
                item(key = "hero") { Hero(title) }
                item(key = "tools") {
                    LeadTools { ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back) }
                }
                item(key = "intro") {
                    Text(
                        stringResource(R.string.interests_intro),
                        style = onest(14.5f, 20f),
                        color = colors.on2,
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 4.dp),
                    )
                }
                item(key = "moods") { ChoiceGroup(stringResource(R.string.interests_moods), state.moods, actions.toggleMood) }
                item(key = "genres") {
                    ChoiceGroup(stringResource(R.string.interests_genres), state.genres, actions.toggleGenre, empty = stringResource(R.string.interests_genres_empty))
                }
                item(key = "languages") { ChoiceGroup(stringResource(R.string.interests_languages), state.languages, actions.toggleLanguage) }
            }
        }
        ComfortButton(
            stringResource(R.string.interests_save),
            actions.save,
            modifier = Modifier.widthIn(max = MAX_WIDTH).align(Alignment.CenterHorizontally).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            block = true,
            size = ButtonSize.Large,
        )
    }
}

/** On a tablet the page keeps to a readable column, as Settings does there. */
private val MAX_WIDTH = 720.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGroup(title: String, choices: List<InterestChoice>, onToggle: (String) -> Unit, empty: String? = null) {
    Label(title)
    Group(pad = true) {
        item {
            if (choices.isEmpty() && empty != null) {
                Text(empty, style = onest(14f, 20f), color = Comfort.colors.on2, modifier = Modifier.padding(horizontal = 6.dp))
            } else {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (choice in choices) FilterChip(choice.label, choice.selected, onClick = { onToggle(choice.id) })
                }
            }
        }
    }
}
