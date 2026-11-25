package app.attic.player.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.LocalBottomPadding
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.model.WrappedPageActions
import app.attic.player.ui.model.WrappedPageState
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Recap page in the Expressive style (pixel/09-recap.html, frames A, B, F, G and H): a large
 * title, the period as a connected group, one tonal block with the headline, "Play recap" and the
 * first three cards as a stack, six stats each in its own shape, the cards to share, the top songs
 * and artists as grouped lists, languages as one connected bar, and the listening clock. The page
 * follows the wallpaper scheme; the cards keep their own.
 */
@Composable
fun ExpressiveRecap(state: WrappedPageState, actions: WrappedPageActions) {
    val list = rememberLazyListState()
    // The bar shows the title once the large one has scrolled away.
    val titleGone by remember(list) { derivedStateOf { list.firstVisibleItemIndex > 1 } }
    val periods = RecapPeriod.entries
    // On a tablet the page is a centred column at most 720dp wide (SPEC.md, Recap).
    val window = LocalWindowLayout.current
    val inset = if (window.wide) window.readableInset() else 0.dp
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = inset, end = inset, bottom = LocalBottomPadding.current),
        ) {
            item(key = "bar") { Column { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)); Spacer(Modifier.height(64.dp)) } }
            item(key = "title") {
                Text(
                    stringResource(R.string.recap_title),
                    style = Expressive.type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                )
            }
            item(key = "periods") {
                ConnectedButtonGroup(
                    options = periods.map { periodName(it) },
                    selectedIndex = periods.indexOf(state.period),
                    onSelect = { actions.setPeriod(periods[it]) },
                    labelWidth = 86,
                    selectedWeight = 1.3f,
                )
            }
            val data = state.data
            when {
                data == null || state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
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
                        symbol = Symbol.CalendarMonth,
                    )
                }
                else -> recapContent(state, actions, data)
            }
        }
        DetailAppBar(
            title = if (titleGone) stringResource(R.string.recap_title) else "",
            onBack = actions.back,
            surface = if (titleGone) AppBarSurface.Scrolled else AppBarSurface.Plain,
            contentInset = if (window.wide) WideExtraMargin else 0.dp,
        )
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

private fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun LazyListScope.recapContent(state: WrappedPageState, actions: WrappedPageActions, data: WrappedData) {
    val recap = data.recap
    item(key = "hero") { Headline(data, state.cards, onPlay = { actions.openStory(0) }) }
    item(key = "stats") { Stats(data) }
    item(key = "share") {
        SectionHeader(stringResource(R.string.recap_share_a_card))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            itemsIndexed(state.cards, key = { _, card -> card.name }) { index, card ->
                CardThumb(card, data, 118.dp, Modifier.clickable { actions.openStory(index) })
            }
        }
    }
    if (data.songs.isNotEmpty()) {
        item(key = "songsHeader") {
            SectionHeader(stringResource(R.string.recap_top_songs)) {
                // `.sec .pp{margin:-10px -8px -10px 0}`: the control reaches past the header's padding.
                PlayButton(
                    playing = state.topSongsPlaying,
                    onClick = actions.playTopSongs,
                    size = PlaySize.Large,
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val v = 10.dp.roundToPx()
                        layout(placeable.width - 8.dp.roundToPx(), placeable.height - 2 * v) { placeable.place(0, -v) }
                    },
                )
            }
        }
        groupItems(data.songs, key = { "song:" + it.first.key }) { index, (track, ranked) ->
            val current = track.key == state.currentKey
            SongRow(
                title = track.title,
                subtitle = track.artist,
                lead = RowLead.RankedCover(index + 1, Art.of(track.mediaId)),
                current = current,
                playing = current && state.playing,
                onClick = { actions.playTopSong(index) },
                onLongClick = { actions.showMenu(track) },
                trailing = { RowCount(pluralStringResource(R.plurals.recap_plays, ranked.plays, count(ranked.plays.toLong()))) },
            )
        }
    }
    if (data.artists.isNotEmpty()) {
        item(key = "artistsHeader") { SectionHeader(stringResource(R.string.recap_top_artists)) }
        groupItems(data.artists, key = { "artist:" + it.first.key }) { index, (artist, ranked) ->
            SongRow(
                title = artist.name,
                subtitle = "",
                lead = RowLead.RankedArtist(index + 1, Art.of(data.artistCovers[artist.key])),
                onClick = { actions.openArtist(artist) },
                trailing = { RowCount(stringResource(R.string.recap_minutes_short, count(ranked.listenedMs / 60_000))) },
            )
        }
    }
    if (recap.languages.isNotEmpty()) {
        item(key = "languages") {
            SectionHeader(stringResource(R.string.recap_languages))
            Languages(data)
        }
    }
    item(key = "clock") {
        SectionHeader(stringResource(R.string.recap_when_you_listen))
        WhenYouListen(data)
    }
    recap.loggedSince?.let { since ->
        item(key = "footnote") {
            val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
            Text(
                stringResource(R.string.recap_footnote, date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp),
            )
        }
    }
}

/** A card at [width], 9:16 with corners scaled from the card's 28. */
@Composable
private fun CardThumb(card: WrappedCard, data: WrappedData, width: Dp, modifier: Modifier = Modifier) {
    val radius = 28.dp * (width / 360.dp)
    ExpressiveRecapCard(card, data, modifier.width(width).clip(RoundedCornerShape(radius)))
}

/**
 * The headline block (`.rhero`): the one primary-container surface on the page, with the headline,
 * the minutes and card count, "Play recap", and the first three cards fanned as a stack. Tapping
 * anywhere on it opens the story at the first card.
 */
@Composable
private fun Headline(data: WrappedData, cards: List<WrappedCard>, onPlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.primaryContainer)
            .clickable(onClick = onPlay)
            .padding(24.dp),
    ) {
        Text(
            recapHeadline(data),
            style = Expressive.type.headlineLargeEmphasized.copy(lineHeight = 36.sp, letterSpacing = (-0.3).sp),
            color = colors.onPrimaryContainer,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.recap_banner_body, count(data.recap.minutes), cards.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ExpressiveButton(
                    stringResource(R.string.recap_play),
                    onPlay,
                    Modifier.padding(top = 20.dp),
                    size = ButtonSize.Medium,
                    leading = Symbol.PlayArrow,
                )
            }
            Deck(data, cards, colors.primaryContainer)
        }
    }
}

/** The first three cards as a stack (`.deck`): Minutes in front, then the top song, then the intro, each raised 8dp. */
@Composable
private fun Deck(data: WrappedData, cards: List<WrappedCard>, ring: Color) {
    val order = listOf(1, 2, 0).mapNotNull { cards.getOrNull(it) }
    if (order.isEmpty()) return
    val width = 61.2.dp
    val radius = 28.dp * (width / 360.dp)
    Box(Modifier.padding(bottom = 2.dp)) {
        // Drawn back to front; each is 34dp under the one before it and 8dp higher.
        order.indices.reversed().forEach { i ->
            val card = order[i]
            val shape: Shape = RoundedCornerShape(radius + 3.dp)
            Box(
                Modifier
                    .offset(x = (width + 6.dp - 34.dp) * i, y = (-8).dp * i)
                    .padding(top = 16.dp)
                    .clip(shape)
                    .background(ring)
                    .padding(3.dp),
            ) {
                ExpressiveRecapCard(card, data, Modifier.width(width).clip(RoundedCornerShape(radius)))
            }
        }
        // Holds the stack's width: three cards overlapping by 34dp, plus the rings.
        Spacer(Modifier.width((width + 6.dp) * order.size - 34.dp * (order.size - 1)).height(1.dp))
    }
}

/** One stat in its own container (`.bt`): figure over label, centred. */
@Composable
private fun StatShape(value: String, label: String, shape: Shape, fill: Color, modifier: Modifier = Modifier, ink: Color = MaterialTheme.colorScheme.onSurface, labelInk: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(
        modifier.height(114.dp).clip(shape).background(fill),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FitText(
            value,
            Expressive.type.headlineMediumEmphasized.copy(lineHeight = 32.sp, letterSpacing = (-0.3).sp, fontFeatureSettings = "tnum"),
            Modifier.padding(horizontal = 10.dp),
            color = ink,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The six stats (`.bento`): Minutes wide, Plays a pill, Songs a disc, Artists the cookie, Days an arch, the streak a full-width pill. */
@Composable
private fun Stats(data: WrappedData) {
    val colors = MaterialTheme.colorScheme
    val recap = data.recap
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.weight(2f, fill = true).height(114.dp).clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge)).background(colors.secondaryContainer).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                FitText(count(recap.minutes), cardText(57f, 60f, 800, width = 88, tracking = -0.0175f).copy(fontFeatureSettings = "tnum"), color = colors.onSurface)
                Text(stringResource(R.string.recap_stat_minutes), style = MaterialTheme.typography.bodyMedium, color = colors.onSecondaryContainer)
            }
            StatShape(count(recap.plays.toLong()), stringResource(R.string.recap_stat_plays), RoundedCornerShape(57.dp), colors.surfaceContainerHigh, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatShape(count(recap.songCount.toLong()), stringResource(R.string.recap_stat_songs), CircleShape, colors.surfaceContainer, Modifier.weight(1f))
            StatShape(
                count(recap.artistCount.toLong()),
                stringResource(R.string.recap_stat_artists),
                CookieShape,
                colors.primaryContainer,
                Modifier.weight(1f),
                ink = colors.onPrimaryContainer,
                labelInk = colors.onPrimaryContainer,
            )
            StatShape(
                count(recap.daysListened.toLong()),
                stringResource(R.string.recap_stat_days),
                RoundedCornerShape(57.dp, 57.dp, ExpressiveShapes.LargeIncreased, ExpressiveShapes.LargeIncreased),
                colors.surfaceContainer,
                Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(36.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                pluralStringResource(R.plurals.recap_days, recap.longestStreak, count(recap.longestStreak.toLong())),
                style = Expressive.type.headlineMediumEmphasized.copy(lineHeight = 32.sp, letterSpacing = (-0.3).sp),
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.recap_stat_longest_streak), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** Languages (`.lang`): one connected bar in tones stepping down from primary, and the legend in the same tones. */
@Composable
private fun Languages(data: WrappedData) {
    val colors = MaterialTheme.colorScheme
    val ground = colors.surfaceContainer
    val tones = listOf(1f, 0.7f, 0.48f, 0.32f, 0.2f)
    val languages = data.recap.languages
    fun tone(i: Int) = lerp(ground, colors.primary, tones.getOrElse(i) { 0.14f })
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge)).background(ground).padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            languages.forEachIndexed { i, share ->
                val first = i == 0
                val last = i == languages.lastIndex
                val shape = RoundedCornerShape(
                    topStart = if (first) 16.dp else 4.dp,
                    bottomStart = if (first) 16.dp else 4.dp,
                    topEnd = if (last) 16.dp else 4.dp,
                    bottomEnd = if (last) 16.dp else 4.dp,
                )
                Box(Modifier.weight(share.share.coerceAtLeast(0.01f)).fillMaxSize().clip(shape).background(tone(i)))
            }
        }
        Spacer(Modifier.height(12.dp))
        languages.forEachIndexed { i, share ->
            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(tone(i)))
                Spacer(Modifier.width(12.dp))
                Text(languageName(share.language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.recap_percent, (share.share * 100).toInt()),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** "When you listen" (`.when`): the clock with its hour labels and hub glyph, the listener type and its line. */
@Composable
private fun WhenYouListen(data: WrappedData) {
    val colors = MaterialTheme.colorScheme
    val recap = data.recap
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            .background(colors.surfaceContainer)
            .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ListeningDial(recap.hours, recap.listenerType, ground = colors.surfaceContainer, modifier = Modifier.size(172.dp), labelSize = 17f)
        Column(Modifier.weight(1f)) {
            Text(listenerTitle(recap.listenerType), style = Expressive.type.headlineSmallEmphasized.copy(lineHeight = 28.sp))
            Text(
                recapClockLine(recap.listenerType, recap.hours),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
