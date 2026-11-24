package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.languageName
import app.attic.player.ui.expressive.listenerTitle
import app.attic.player.ui.expressive.recapClockLine
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.model.WrappedPageActions
import app.attic.player.ui.model.WrappedPageState
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/**
 * The Recap page in the Comfort style (oneui/09-recap.html, frames A, B and F to I): an ordinary
 * screen from the kit. The title and the period's headline in the viewing area, the back arrow in the
 * row that closes it, the periods as sub-tabs (one always selected), then grouped cards: "Play recap"
 * ending in the kit's play button (the one amber fill), the six stats, the cards to share, the top
 * songs and artists ranked, the languages in steps of the accent, the listening clock, the footnote.
 * Scrolled, the title joins the back arrow in the bar and the periods pin under it.
 */
@Composable
fun ComfortRecap(state: WrappedPageState, actions: WrappedPageActions) {
    val colors = Comfort.colors
    val list = rememberLazyListState()
    val periods = RecapPeriod.entries
    val density = LocalDensity.current
    val barPx = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp).roundToPx() }
    // The periods pin under the bar once their own row reaches it.
    val pinned by remember(list, barPx) {
        derivedStateOf {
            val tabs = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "periods" }
            if (tabs == null) list.firstVisibleItemIndex > 1 else tabs.offset < barPx
        }
    }
    val tabs: @Composable (Boolean) -> Unit = { lift ->
        SubTabs(
            options = periods.map { periodName(it) },
            selectedIndex = periods.indexOf(state.period),
            onSelect = { actions.setPeriod(periods[it]) },
            lift = lift,
        )
    }
    Box(Modifier.fillMaxSize().background(colors.ground)) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = LocalBottomPadding.current + 16.dp)) {
            item(key = "hero") {
                Box {
                    Hero(stringResource(R.string.recap_title), subtitle = periodHeadline(state.period, state.data?.today ?: LocalDate.now()))
                    // `.tools.lead`: the back arrow in the row that closes the viewing area, pulled up into the hero's empty foot.
                    Box(Modifier.align(Alignment.BottomStart).height(50.dp).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                        ComfortIconButton(Symbol.ArrowBack, stringResource(R.string.back), actions.back)
                    }
                }
            }
            item(key = "periods") { Tools(leading = { tabs(false) }) {} }
            val data = state.data
            when {
                data == null || state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Spinner() }
                }
                data.recap.isEmpty -> item(key = "empty") {
                    EmptyState(
                        title = stringResource(
                            when (state.period) {
                                RecapPeriod.Week -> R.string.recap_empty_week
                                RecapPeriod.Month -> R.string.recap_empty_month
                                RecapPeriod.Year -> R.string.recap_empty_year
                                RecapPeriod.AllTime -> R.string.recap_empty_all_time
                            },
                        ),
                        body = stringResource(R.string.recap_empty_body),
                        symbol = Symbol.GraphicEq,
                    )
                }
                else -> recapContent(state, actions, data)
            }
        }
        if (pinned) {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().background(colors.ground)) {
                    Bar(stringResource(R.string.recap_title), onBack = actions.back)
                }
                tabs(true)
            }
        }
    }
}

@Composable
private fun periodName(period: RecapPeriod): String = stringResource(
    when (period) {
        RecapPeriod.Week -> R.string.recap_period_week
        RecapPeriod.Month -> R.string.recap_period_month
        RecapPeriod.Year -> R.string.recap_period_year
        RecapPeriod.AllTime -> R.string.recap_period_all_time
    },
)

/**
 * The headline under the title for [period], worded before its recap has loaded so it changes with the
 * tab (frame H): "Your 2026 so far", from 24 December "Your 2026 Recap".
 */
@Composable
private fun periodHeadline(period: RecapPeriod, today: LocalDate): String = when (period) {
    RecapPeriod.Week -> stringResource(R.string.recap_headline_week)
    RecapPeriod.Month -> stringResource(R.string.recap_headline_month, today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()))
    RecapPeriod.Year -> stringResource(
        if (today.monthValue == 12 && today.dayOfMonth >= 24) R.string.recap_headline_year else R.string.recap_headline_year_so_far,
        today.year.toString(),
    )
    RecapPeriod.AllTime -> stringResource(R.string.recap_headline_all_time)
}

private fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun LazyListScope.recapContent(state: WrappedPageState, actions: WrappedPageActions, data: WrappedData) {
    val recap = data.recap
    item(key = "play") { PlayRecap(data, state.cards.size, onPlay = { actions.openStory(0) }) }
    item(key = "stats") {
        Group(Modifier.padding(top = GroupSpacing)) {
            item {
                Stats(
                    listOf(
                        count(recap.minutes) to stringResource(R.string.recap_stat_minutes),
                        count(recap.plays.toLong()) to stringResource(R.string.recap_stat_plays),
                        count(recap.songCount.toLong()) to stringResource(R.string.recap_stat_songs),
                        count(recap.artistCount.toLong()) to stringResource(R.string.recap_stat_artists),
                        count(recap.daysListened.toLong()) to stringResource(R.string.recap_stat_days),
                        pluralStringResource(R.plurals.recap_days, recap.longestStreak, count(recap.longestStreak.toLong())) to
                            stringResource(R.string.recap_stat_longest_streak),
                    ),
                )
            }
        }
    }
    item(key = "share") {
        Column {
            Label(stringResource(R.string.recap_share_a_card))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 24.dp)) {
                itemsIndexed(state.cards, key = { _, card -> card.name }) { index, card ->
                    ComfortRecapCard(
                        card,
                        data,
                        Modifier.width(118.dp).clip(RoundedCornerShape(14.dp)).clickable { actions.openStory(index) },
                    )
                }
            }
        }
    }
    if (data.songs.isNotEmpty()) {
        item(key = "songsLabel") {
            LabelWithButton(stringResource(R.string.recap_top_songs)) {
                ComfortButton(
                    stringResource(if (state.topSongsPlaying) R.string.pause else R.string.play),
                    actions.playTopSongs,
                    style = ButtonStyle.Tonal,
                    size = ButtonSize.Small,
                    leading = if (state.topSongsPlaying) Symbol.Pause else Symbol.PlayArrow,
                    leadingFilled = true,
                )
            }
        }
        groupItems(data.songs, key = { "song:" + it.first.key }) { index, (track, ranked) ->
            val current = track.key == state.currentKey
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = SongLead.Cover(Art.of(track.mediaId)),
                rank = index + 1,
                meta = pluralStringResource(R.plurals.recap_plays, ranked.plays, count(ranked.plays.toLong())),
                current = current,
                playing = current && state.playing,
                onClick = { actions.playTopSong(index) },
                onLongClick = { actions.showMenu(track) },
            )
        }
    }
    if (data.artists.isNotEmpty()) {
        item(key = "artistsLabel") { Label(stringResource(R.string.recap_top_artists)) }
        groupItems(data.artists, key = { "artist:" + it.first.key }) { index, (artist, ranked) ->
            SongRow(
                title = artist.name,
                subtitle = null,
                lead = SongLead.Artist(Art.of(data.artistCovers[artist.key])),
                rank = index + 1,
                meta = stringResource(R.string.recap_minutes_short, count(ranked.listenedMs / 60_000)),
                onClick = { actions.openArtist(artist) },
            )
        }
    }
    if (recap.languages.isNotEmpty()) {
        item(key = "languages") {
            Column {
                Label(stringResource(R.string.recap_languages))
                Languages(data)
            }
        }
    }
    item(key = "clock") {
        Column {
            Label(stringResource(R.string.recap_when_you_listen))
            Group {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 18.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        ListeningClock(recap.hours, Modifier.size(132.dp), labels = false)
                        Column(Modifier.weight(1f)) {
                            Text(listenerTitle(recap.listenerType), style = Comfort.type.barTitle, color = Comfort.colors.on)
                            Text(
                                recapClockLine(recap.listenerType, recap.hours),
                                style = onest(13.5f, 19f),
                                color = Comfort.colors.on2,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    recap.loggedSince?.let { since ->
        item(key = "footnote") {
            val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
            Note(stringResource(R.string.recap_footnote, date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))))
        }
    }
}

/**
 * `.playrow`: the story as the page's first card, "Play recap" over the minutes sentence, ending in the
 * kit's play button. The whole row opens the story at the first card.
 */
@Composable
private fun PlayRecap(data: WrappedData, cards: Int, onPlay: () -> Unit) {
    Group {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlay)
                    .heightIn(min = 92.dp)
                    .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.recap_play), style = onest(18f, 24f, 700, tracking = -0.01f), color = Comfort.colors.on)
                    Text(
                        stringResource(R.string.recap_banner_body, count(data.recap.minutes), cards),
                        style = onest(13.5f, 19f),
                        color = Comfort.colors.on2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                PlayButton(playing = false, onClick = onPlay, size = PlaySize.Medium)
            }
        }
    }
}

/** `.label.has-btn`: a group's label with a small button on its line. */
@Composable
private fun LabelWithButton(text: String, button: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp, top = 15.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Comfort.type.label, color = Comfort.colors.on2, modifier = Modifier.weight(1f))
        button()
    }
}

/**
 * Languages (`.langs`): one thick capsule split by share, in steps of the accent with the largest share
 * brightest, then a row per language with its step, its name and its share.
 */
@Composable
private fun Languages(data: WrappedData) {
    val colors = Comfort.colors
    val languages = data.recap.languages
    val steps = languageSteps(languages.map { it.language })
    Group {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    languages.forEachIndexed { index, share ->
                        Box(
                            Modifier
                                .weight(share.share.coerceAtLeast(0.001f))
                                .height(28.dp)
                                .background(steps[index]),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                languages.forEachIndexed { index, share ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .drawBehind {
                                if (index > 0) {
                                    val left = 24.dp.toPx()
                                    drawRect(colors.line, topLeft = Offset(left, 0f), size = Size(size.width - left, 1.dp.toPx()))
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(12.dp).background(steps[index], ComfortShapes.Squircle))
                        Text(languageName(share.language), style = onest(16f, 22f, 500), color = colors.on, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.recap_percent, (share.share * 100).toInt()), style = onest(14f, 18f).tabular(), color = colors.on2)
                    }
                }
            }
        }
    }
}
