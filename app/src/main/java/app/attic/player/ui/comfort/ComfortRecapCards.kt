package app.attic.player.ui.comfort

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.data.LanguageLabels
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.expressive.busiestHour
import app.attic.player.ui.expressive.languageName
import app.attic.player.ui.expressive.listenerTitle
import app.attic.player.ui.expressive.recapClockLine
import app.attic.player.ui.expressive.recapHeadline
import app.attic.player.ui.expressive.recapPeriodLabel
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.rememberArtColor
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Recap's ten cards in the Comfort style (oneui/09-recap.html, frame C). Each is built from pieces of
 * the app on a fixed 360 by 640 canvas and scaled, so the story, the thumbnails and the shared
 * 1080 by 1920 image are one drawing: a label or headline, one statement, one deck (a card one step
 * up), then the mark, "Attic" and the period. Cards are always the dark tokens, whatever the phone's
 * theme, so the image someone receives is the one the sender saw. The three cards about one song or
 * one artist take that cover's wash; the ranked rows lie on beds in each row's cover colour.
 */

private val CardWidth = 360.dp
private val CardHeight = 640.dp

private fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

/** The cover whose wash [card] takes: the top song, the top artist, and the summary. The rest stay neutral. */
private fun washCover(card: WrappedCard, data: WrappedData): Long? = when (card) {
    WrappedCard.TopSong, WrappedCard.Summary -> data.songs.firstOrNull()?.first?.mediaId
    WrappedCard.TopArtist -> data.artists.firstOrNull()?.let { data.artistCovers[it.first.key] }
    else -> null
}

/** True for a card drawn on its cover's wash, so the story around it takes the wash too (frame D2). */
internal fun washes(card: WrappedCard): Boolean = card == WrappedCard.TopSong || card == WrappedCard.TopArtist || card == WrappedCard.Summary

/**
 * The card's colours: the kit's dark tokens, washed with [card]'s cover when it has one. The story
 * wraps itself in the same, so its deck and Share follow the card.
 */
@Composable
internal fun RecapCardColors(card: WrappedCard, data: WrappedData, content: @Composable () -> Unit) {
    ProvideComfortColors(ComfortDark) {
        val cover = washCover(card, data)
        if (cover != null) TintedCover(cover, wash = true, content = content) else content()
    }
}

/**
 * One card at any size: the 360 by 640 drawing scaled to the width it is given. When [layer] is set the
 * card also records into it, unclipped and with square corners, for Save and Share. [animate] counts
 * the minutes up and raises the amber through its shape, as in the story.
 */
@Composable
fun ComfortRecapCard(card: WrappedCard, data: WrappedData, modifier: Modifier = Modifier, layer: GraphicsLayer? = null, animate: Boolean = false) {
    BoxWithConstraints(modifier.aspectRatio(9f / 16f).clipToBounds(), contentAlignment = Alignment.Center) {
        val scale = maxWidth / CardWidth
        val density = LocalDensity.current
        Box(
            Modifier
                .requiredSize(CardWidth, CardHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (layer == null) {
                        Modifier
                    } else {
                        Modifier.drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        }
                    },
                ),
        ) {
            // The card is a picture, so it ignores the phone's text size setting.
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                RecapCardColors(card, data) {
                    CardFrame(recapPeriodLabel(data)) { CardContent(card, data, animate) }
                }
            }
        }
    }
}

@Composable
private fun CardContent(card: WrappedCard, data: WrappedData, animate: Boolean) {
    when (card) {
        WrappedCard.Intro -> IntroCard(data)
        WrappedCard.Minutes -> MinutesCard(data, animate)
        WrappedCard.TopSong -> TopSongCard(data)
        WrappedCard.TopSongs -> TopSongsCard(data)
        WrappedCard.TopArtist -> TopArtistCard(data)
        WrappedCard.TopArtists -> TopArtistsCard(data)
        WrappedCard.Languages -> LanguagesCard(data)
        WrappedCard.Clock -> ClockCard(data)
        WrappedCard.Highlights -> HighlightsCard(data)
        WrappedCard.Summary -> SummaryCard(data)
    }
}

// ── Frame and type ───────────────────────────────────────────────────────────

/** `.rc .lb`: "Your top song". */
private val CardLabel = onest(15f, 20f, 600)

/** `.rc .hd` at a size. */
private fun headline(size: Float, line: Float, tracking: Float = -0.03f) = onest(size, line, 800, tracking)

/** `.rc .sub`. */
private val CardSub = onest(17f, 24f)

/**
 * `.rc`: the card colour, 28dp from the top and 24 from the sides, and `.ft` at its foot: the mark in a
 * solid icon box, "Attic", and the period at the end.
 */
@Composable
private fun CardFrame(period: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = Comfort.colors
    Box(Modifier.fillMaxSize().background(colors.card)) {
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 28.dp), content = content)
        Footer(period, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun BoxScope.Footer(period: String, modifier: Modifier) {
    val colors = Comfort.colors
    Row(
        modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        MarkBox(size = 26.dp, style = IconBoxStyle.Solid)
        Text(stringResource(R.string.card_footer_name), style = onest(14f, 18f, 700), color = colors.on, modifier = Modifier.weight(1f))
        Text(period, style = onest(13f, 18f, 500), color = colors.on2, maxLines = 1)
    }
}

/** `.dk`: the card's deck, one step above the card, radius 26. */
@Composable
private fun CardDeck(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(ComfortShapes.Group).background(Comfort.colors.fill), content = content)
}

/** One line that shrinks to fit the width rather than wrap or cut: the big figures ("18,460"). */
@Composable
private fun FitText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer(cacheSize = 4)
    BoxWithConstraints(modifier) {
        val max = constraints.maxWidth
        val fitted = remember(text, style, max) {
            val width = measurer.measure(text, style, softWrap = false, maxLines = 1).size.width
            if (max == Constraints.Infinity || width <= max || width == 0) {
                style
            } else {
                val k = max.toFloat() / width
                style.copy(fontSize = style.fontSize * k, lineHeight = style.lineHeight * k)
            }
        }
        Text(text, style = fitted, color = color, maxLines = 1, softWrap = false)
    }
}

// ── 1 Intro ──────────────────────────────────────────────────────────────────

/** A tray of the nine covers played most, the period's line, and the minutes and songs under it. */
@Composable
private fun IntroCard(data: WrappedData) {
    val colors = Comfort.colors
    val line = stringResource(
        when (data.period) {
            RecapPeriod.Week -> R.string.card_intro_week
            RecapPeriod.Month -> R.string.card_intro_month
            RecapPeriod.Year -> R.string.card_intro_year
            RecapPeriod.AllTime -> R.string.card_intro_all_time
        },
    )
    // One cover per album, most played first, topped up with repeats if there are fewer than nine albums.
    val songs = data.songs.map { it.first }
    val covers = (songs.distinctBy { it.albumKey ?: it.key } + songs).map { it.mediaId }.distinct().take(9)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        covers.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { Cover(Art.of(it), size = 96.dp) }
            }
        }
    }
    Text(line, style = headline(40f, 44f), color = colors.on, modifier = Modifier.padding(top = 24.dp))
    Text(
        stringResource(R.string.card_intro_body, count(data.recap.minutes), count(data.recap.songCount.toLong())),
        style = onest(19f, 27f),
        color = colors.on2,
        modifier = Modifier.padding(top = 12.dp),
    )
}

// ── 2 Minutes ────────────────────────────────────────────────────────────────

/** The number in the one amber shape. In the story the amber rises through it while the number counts up. */
@Composable
private fun MinutesCard(data: WrappedData, animate: Boolean) {
    val colors = Comfort.colors
    val target = data.recap.minutes
    val shown = remember { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) shown.animateTo(target.toFloat(), tween(1600, easing = FastOutSlowInEasing)) else shown.snapTo(target.toFloat())
    }
    val rise = if (target > 0) (shown.value / target).coerceIn(0f, 1f) else 1f
    Text(stringResource(R.string.card_minutes_lead), style = CardLabel, color = colors.on2)
    Box(Modifier.padding(top = 16.dp).size(312.dp).clip(ComfortShapes.SquircleLarge).background(colors.fill)) {
        LampNumber(count(shown.value.toLong()), colors.on)
        // The amber part, and the number in the ink that reads on it, clipped to how far it has risen.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(top = size.height * (1f - rise)) {
                        drawRect(colors.accent)
                        this@drawWithContent.drawContent()
                    }
                },
        ) { LampNumber(count(shown.value.toLong()), colors.onAccent) }
    }
    val hours = target / 60
    val days = pluralStringResource(R.plurals.recap_days, data.recap.daysListened, count(data.recap.daysListened.toLong()))
    Text(
        if (hours >= 1) {
            stringResource(R.string.card_minutes_hours_days, pluralStringResource(R.plurals.card_hours, hours.toInt(), count(hours)), days)
        } else {
            stringResource(R.string.card_minutes_starting, days)
        },
        style = onest(24f, 31f, 700, tracking = -0.015f),
        color = colors.on,
        modifier = Modifier.padding(top = 28.dp),
    )
}

@Composable
private fun LampNumber(figure: String, ink: Color) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        FitText(figure, headline(80f, 80f, tracking = -0.05f), ink)
        Text(stringResource(R.string.card_minutes_unit), style = onest(30f, 36f, 700, tracking = -0.02f), color = ink)
    }
}

// ── 3 Top song ───────────────────────────────────────────────────────────────

/** On the cover's wash: the cover upright and whole, the title, the artist, and the plays in an accent pill. */
@Composable
private fun TopSongCard(data: WrappedData) {
    val colors = Comfort.colors
    val (track, ranked) = data.songs.first()
    Text(stringResource(R.string.card_top_song), style = CardLabel, color = colors.on2)
    Cover(Art.of(track.mediaId), Modifier.padding(top = 16.dp), size = 312.dp)
    Text(track.title, style = headline(32f, 36f), color = colors.on, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 20.dp))
    Text(track.artist, style = CardSub, color = colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    Box(
        Modifier.padding(top = 16.dp).height(40.dp).clip(ComfortShapes.Pill).background(colors.accent).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (ranked.plays == 1) stringResource(R.string.card_played_once) else stringResource(R.string.card_played_times, count(ranked.plays.toLong())),
            style = onest(15f, 20f, 700),
            color = colors.onAccent,
        )
    }
}

// ── 4 Top songs, 6 Top artists ───────────────────────────────────────────────

/** Five ranked rows in a deck; each lies on a bed in its cover's colour, as long as its plays against the first. */
@Composable
private fun TopSongsCard(data: WrappedData) {
    val rows = data.songs.take(5)
    val top = rows.first().second.plays.coerceAtLeast(1)
    Text(stringResource(R.string.card_top_songs_headline).replace('\n', ' '), style = headline(34f, 38f), color = Comfort.colors.on)
    CardDeck(Modifier.padding(top = 20.dp)) {
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { index, (track, ranked) ->
            RankRow(
                rank = index + 1,
                share = ranked.plays.toFloat() / top,
                mediaId = track.mediaId,
                title = track.title,
                subtitle = track.artist,
                value = stringResource(R.string.card_times, count(ranked.plays.toLong())),
                art = { Cover(Art.of(track.mediaId), size = 52.dp) },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** The same beds, sized by minutes. */
@Composable
private fun TopArtistsCard(data: WrappedData) {
    val rows = data.artists.take(5)
    val top = rows.first().second.listenedMs.coerceAtLeast(1)
    Text(stringResource(R.string.card_top_artists_headline).replace('\n', ' '), style = headline(34f, 38f), color = Comfort.colors.on)
    CardDeck(Modifier.padding(top = 20.dp)) {
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { index, (artist, ranked) ->
            val cover = data.artistCovers[artist.key]
            RankRow(
                rank = index + 1,
                share = ranked.listenedMs.toFloat() / top,
                mediaId = cover,
                title = artist.name,
                subtitle = stringResource(R.string.card_minutes, count(ranked.listenedMs / 60_000)),
                value = null,
                art = { ArtistCover(Art.of(cover), size = 52.dp) },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * `.rk`: rank, picture, two lines and an optional count, on a bed in [mediaId]'s cover colour (the mini
 * player's fill) whose length is 88dp plus [share] of the rest, so it is never shorter than the rank
 * and picture.
 */
@Composable
private fun RankRow(rank: Int, share: Float, mediaId: Long?, title: String, subtitle: String, value: String?, art: @Composable () -> Unit) {
    val seed = rememberArtColor(mediaId, Color.Unspecified, deepen = false)
    Tinted(seed) {
        val colors = Comfort.colors
        val bed = colors.miniFill
        Row(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .drawBehind {
                    val width = 88.dp.toPx() + (size.width - 100.dp.toPx()) * share.coerceIn(0f, 1f)
                    drawRoundRect(
                        bed,
                        topLeft = Offset(6.dp.toPx(), 4.dp.toPx()),
                        size = Size(width, size.height - 8.dp.toPx()),
                        cornerRadius = CornerRadius(ComfortShapes.ControlRadius.toPx()),
                    )
                }
                .padding(start = 20.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(rank.toString(), style = onest(20f, 24f, 800), color = colors.on, modifier = Modifier.width(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            art()
            Column(Modifier.weight(1f)) {
                Text(title, style = onest(16f, 19f, 700), color = colors.on, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = onest(13f, 17f), color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            }
            if (value != null) Text(value, style = onest(15f, 20f, 700).tabular(), color = colors.on)
        }
    }
}

// ── 5 Top artist ─────────────────────────────────────────────────────────────

/** On the artist's wash: the picture in the rounder shape, whole and upright, the name, and the minutes. */
@Composable
private fun TopArtistCard(data: WrappedData) {
    val colors = Comfort.colors
    val (artist, ranked) = data.artists.first()
    Text(stringResource(R.string.card_top_artist), style = CardLabel, color = colors.on2)
    ArtistCover(Art.of(data.artistCovers[artist.key]), Modifier.padding(top = 16.dp), size = 312.dp)
    Text(artist.name, style = headline(44f, 48f), color = colors.on, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 22.dp))
    Text(
        stringResource(
            R.string.card_top_artist_body,
            count(ranked.listenedMs / 60_000),
            pluralStringResource(R.plurals.recap_plays, ranked.plays, count(ranked.plays.toLong())),
            count(data.recap.artistCount.toLong()),
        ),
        style = CardSub,
        color = colors.on2,
        modifier = Modifier.padding(top = 10.dp),
    )
}

// ── 7 Languages ──────────────────────────────────────────────────────────────

/** The top share large, its language under it, then up to five thick tracks filled to their true share. */
@Composable
private fun LanguagesCard(data: WrappedData) {
    val colors = Comfort.colors
    val languages = data.recap.languages.take(5)
    val first = languages.first()
    val steps = languageSteps(languages.map { it.language })
    Text(stringResource(R.string.card_languages_label), style = CardLabel, color = colors.on2)
    Text(stringResource(R.string.recap_percent, (first.share * 100).toInt()), style = headline(104f, 98f, tracking = -0.055f), color = colors.on, modifier = Modifier.padding(top = 10.dp))
    Text(languageName(first.language), style = headline(40f, 44f), color = colors.on)
    CardDeck(Modifier.padding(top = 20.dp)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            languages.forEachIndexed { index, share ->
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Text(languageName(share.language), style = onest(15f, 20f, 600), color = colors.on, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.recap_percent, (share.share * 100).toInt()), style = onest(15f, 20f).tabular(), color = colors.on2)
                    }
                    val step = steps[index]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(ComfortShapes.Pill)
                            .background(colors.fill2)
                            .drawBehind {
                                val width = (size.width * share.share.coerceIn(0f, 1f)).coerceAtLeast(16.dp.toPx())
                                drawRoundRect(step, size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2))
                            },
                    )
                }
            }
        }
    }
}

/**
 * Each language's colour: steps of the accent, largest share brightest (`.st0` to `.st3`, lightness
 * down .13, .26, .38 in OKLCH), a fifth language sharing the darkest step; "No vocals" is not a
 * language, so it is neutral (`--on-3`).
 */
@Composable
internal fun languageSteps(codes: List<String>): List<Color> {
    val colors = Comfort.colors
    val accent = colors.accent
    val lch = remember(accent) { Oklch.fromArgb(accent.toArgb()) }
    var step = 0
    return codes.map { code ->
        if (code == LanguageLabels.NO_VOCALS) {
            colors.on3
        } else {
            val drop = when (step.coerceAtMost(3)) {
                0 -> 0.0
                1 -> 0.13
                2 -> 0.26
                else -> 0.38
            }
            step++
            if (drop == 0.0) accent else Color(Oklch.toArgb((lch.l - drop).coerceAtLeast(0.0), lch.c, lch.h))
        }
    }
}

// ── 8 Listening clock ────────────────────────────────────────────────────────

/** Twenty-four thick bars from midnight at the top; the busiest hour at full strength and named in the middle. */
@Composable
private fun ClockCard(data: WrappedData) {
    val colors = Comfort.colors
    val type = data.recap.listenerType
    Text(stringResource(R.string.card_clock_label), style = CardLabel, color = colors.on2)
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        ListeningClock(data.recap.hours, Modifier.size(296.dp), labels = true)
    }
    Text(listenerTitle(type), style = headline(44f, 48f), color = colors.on, modifier = Modifier.padding(top = 12.dp))
    Text(recapClockLine(type, data.recap.hours), style = CardSub, color = colors.on2, modifier = Modifier.padding(top = 8.dp))
}

/**
 * The listening clock (`.clock`, a 200-unit dial): 24 round-capped bars from radius 41 to 84, midnight
 * at the top, clockwise, each on a track in `--fill` and filled in the accent as far as that hour's
 * listening against the busiest hour (at 72%, the busiest at full strength). With [labels], "midnight"
 * and "noon" at the ends and the busiest hour in the middle.
 */
@Composable
internal fun ListeningClock(hours: LongArray, modifier: Modifier, labels: Boolean) {
    val colors = Comfort.colors
    val peak = busiestHour(hours)
    val max = hours.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val midnight = stringResource(R.string.hour_midnight)
    val noon = stringResource(R.string.hour_noon)
    val hourText = when {
        peak == 0 -> midnight
        peak == 12 -> noon
        peak < 12 -> stringResource(R.string.hour_am, peak)
        else -> stringResource(R.string.hour_pm, peak - 12)
    }
    val density = LocalDensity.current
    Canvas(modifier) {
        val u = size.minDimension / 200f
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = 6.6f * u
        for (h in 0 until 24) {
            val angle = Math.toRadians(h * 15.0)
            val dx = kotlin.math.sin(angle).toFloat()
            val dy = -kotlin.math.cos(angle).toFloat()
            fun at(r: Float) = Offset(c.x + dx * r * u, c.y + dy * r * u)
            drawLine(colors.fill, at(41f), at(84f), stroke, StrokeCap.Round)
            val fraction = hours[h].toFloat() / max
            if (fraction > 0f) {
                drawLine(colors.accent.copy(alpha = if (h == peak) 1f else 0.72f), at(41f), at(41f + 43f * fraction), stroke, StrokeCap.Round)
            }
        }
        if (labels) {
            fun label(text: String, size: Float, weight: Int, color: Color, centreY: Float) {
                val style = onest(size, size * 1.2f, weight).copy(fontSize = with(density) { (size * u).toSp() }, lineHeight = with(density) { (size * 1.2f * u).toSp() })
                val layout = measurer.measure(text, style)
                drawText(layout, color, Offset(c.x - layout.size.width / 2f, c.y + centreY * u - layout.size.height / 2f))
            }
            label(midnight, 7.4f, 500, colors.on3, -93.5f)
            label(noon, 7.4f, 500, colors.on3, 94.5f)
            label(hourText, 17f, 800, colors.on, 0f)
        }
    }
}

// ── 9 Highlights ─────────────────────────────────────────────────────────────

/** Up to four highlights as rows in a deck, each with an icon box, the way a settings list is drawn. */
@Composable
private fun HighlightsCard(data: WrappedData) {
    val colors = Comfort.colors
    val recap = data.recap
    val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    val items = buildList {
        recap.biggestDay?.let { day ->
            add(Triple(Symbol.Today, stringResource(R.string.card_highlight_biggest_day_value, count(day.listenedMs / 60_000)), stringResource(R.string.card_highlight_biggest_day, day.date.format(dayFormat))))
        }
        recap.onRepeat?.let { repeat ->
            data.songs.firstOrNull { it.first.key == repeat.key }?.first?.title?.let { title ->
                add(Triple(Symbol.Repeat, stringResource(R.string.card_times, count(repeat.plays.toLong())), stringResource(R.string.card_highlight_repeat, title)))
            }
        }
        if (recap.longestStreak >= 2) {
            add(Triple(Symbol.LocalFireDepartment, pluralStringResource(R.plurals.recap_days, recap.longestStreak, count(recap.longestStreak.toLong())), stringResource(R.string.card_highlight_streak)))
        }
        if (recap.discoveries > 0) {
            add(Triple(Symbol.NewReleases, count(recap.discoveries.toLong()), pluralStringResource(R.plurals.card_highlight_first_plays, recap.discoveries)))
        }
        if (recap.radioShare >= 0.05f) {
            add(Triple(Symbol.Sensors, stringResource(R.string.recap_percent, (recap.radioShare * 100).toInt()), stringResource(R.string.card_highlight_radio)))
        }
    }.take(4)
    Text(stringResource(R.string.card_highlights_headline), style = headline(34f, 38f), color = colors.on)
    CardDeck(Modifier.padding(top = 20.dp)) {
        items.forEachIndexed { index, (symbol, value, line) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp)
                    .drawBehind {
                        if (index > 0) {
                            val left = 80.dp.toPx()
                            drawRect(colors.fill2, topLeft = Offset(left, 0f), size = Size(size.width - left - 20.dp.toPx(), 1.dp.toPx()))
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBox(symbol, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(value, style = headline(30f, 34f), color = colors.on, maxLines = 1)
                    Text(line, style = onest(15f, 20f), color = colors.on2, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// ── 10 Summary ───────────────────────────────────────────────────────────────

/** On the top song's wash: cover and headline, a two-cell stat deck, then the two top-five lists stacked, each name with its picture. */
@Composable
private fun SummaryCard(data: WrappedData) {
    val colors = Comfort.colors
    val recap = data.recap
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Cover(Art.of(data.songs.firstOrNull()?.first?.mediaId), size = 88.dp)
        Text(recapHeadline(data), style = headline(28f, 32f), color = colors.on, maxLines = 3)
    }
    CardDeck(Modifier.padding(top = 16.dp)) {
        // The divider is drawn from the row's own height: the stats fit their figures with FitText, a
        // BoxWithConstraints, which cannot answer the intrinsic measurement an IntrinsicSize row asks for.
        Row(
            Modifier.fillMaxWidth().drawBehind {
                val line = 1.dp.toPx()
                drawRect(colors.fill2, topLeft = Offset((size.width - line) / 2f, 0f), size = Size(line, size.height))
            },
        ) {
            SummaryStat(count(recap.minutes), stringResource(R.string.card_summary_minutes), Modifier.weight(1f))
            Spacer(Modifier.width(1.dp))
            val mostly = recap.languages.firstOrNull()?.let { languageName(it.language) } ?: listenerTitle(recap.listenerType)
            SummaryStat(mostly, stringResource(R.string.card_summary_mostly_in), Modifier.weight(1f))
        }
    }
    if (data.artists.isNotEmpty()) {
        SummaryList(stringResource(R.string.card_summary_top_artists), data.artists.take(5).map { (artist, _) -> artist.name to data.artistCovers[artist.key] }, person = true)
    }
    if (data.songs.isNotEmpty()) {
        SummaryList(stringResource(R.string.card_summary_top_songs), data.songs.take(5).map { (track, _) -> track.title to track.mediaId }, person = false)
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 12.dp)) {
        FitText(value, headline(26f, 30f, tracking = -0.025f), Comfort.colors.on)
        Text(label, style = onest(13f, 17f), color = Comfort.colors.on2)
    }
}

@Composable
private fun SummaryList(label: String, rows: List<Pair<String, Long?>>, person: Boolean) {
    val colors = Comfort.colors
    CardDeck(Modifier.padding(top = 10.dp)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 11.dp)) {
            Text(label, style = onest(13f, 17f, 600), color = colors.on2, modifier = Modifier.padding(bottom = 5.dp))
            rows.forEachIndexed { index, (name, cover) ->
                Row(Modifier.height(25.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text((index + 1).toString(), style = onest(15f, 20f, 600).tabular(), color = colors.on2, modifier = Modifier.width(9.dp))
                    if (person) ArtistCover(Art.of(cover), size = 20.dp) else Cover(Art.of(cover), size = 20.dp)
                    Text(name, style = onest(15f, 20f, 600), color = colors.on, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
