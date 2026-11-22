package app.attic.player.ui.expressive

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.model.WrappedCard
import app.attic.player.ui.model.WrappedData
import app.attic.player.ui.rememberArtColor
import app.attic.player.wrapped.ListenerType
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Recap's ten cards in the Expressive style (pixel/09-recap.html, frames C1 to C10). Each is drawn on
 * a fixed 360 by 640 canvas and scaled, so the story, the thumbnails and the shared 1080 by 1920 image
 * are one drawing. A card's colours are the dark art scheme of one of the period's covers, in both
 * themes: never the wallpaper, so the image someone receives is the one the sender saw.
 */

private val CardWidth = 360.dp
private val CardHeight = 640.dp

/** The seed for a card whose cover has no art: Material's baseline purple, a fixed colour of Attic's own. */
private val FallbackSeed = Color(0xFF6750A4)

private fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

/**
 * The song whose cover colours [card]: a card about one song or one artist takes that cover; the rest
 * are dealt from the period's top songs so that neighbouring cards differ.
 */
fun recapCardCover(card: WrappedCard, data: WrappedData): Long? {
    val songs = data.songs.map { it.first.mediaId }
    fun song(rank: Int): Long? = if (songs.isEmpty()) null else songs[rank % songs.size]
    return when (card) {
        WrappedCard.Intro -> song(0)
        WrappedCard.Minutes -> song(3)
        WrappedCard.TopSong -> song(0)
        WrappedCard.TopSongs -> song(1)
        WrappedCard.TopArtist -> data.artists.firstOrNull()?.let { data.artistCovers[it.first.key] } ?: song(0)
        WrappedCard.TopArtists -> song(4)
        WrappedCard.Languages -> song(2)
        WrappedCard.Clock -> song(3)
        WrappedCard.Highlights -> song(5)
        WrappedCard.Summary -> song(0)
    }
}

/** The seed colour [card] is built from: its cover's tint, or the fallback seed. */
@Composable
fun rememberRecapCardSeed(card: WrappedCard, data: WrappedData): Color {
    val tint = rememberArtColor(recapCardCover(card, data), fallback = Color.Unspecified)
    return if (tint.isSpecified) tint else FallbackSeed
}

/**
 * One card at any size: the 360 by 640 drawing scaled to the width it is given. When [layer] is set the
 * card also records into it, unclipped and with square corners, for Save and Share. [animate] counts
 * the minutes up, as in the story.
 */
@Composable
fun ExpressiveRecapCard(card: WrappedCard, data: WrappedData, modifier: Modifier = Modifier, layer: GraphicsLayer? = null, animate: Boolean = false) {
    val seed = rememberRecapCardSeed(card, data)
    val scheme = remember(seed) { ArtScheme.colorScheme(seed.toArgb(), dark = true, fallback = darkColorScheme()) }
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
                MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
                    CompositionLocalProvider(LocalTextStyle provides cardText(16f, 23f, 500, round = false)) {
                        CardContent(card, data, animate)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardContent(card: WrappedCard, data: WrappedData, animate: Boolean) {
    val period = recapPeriodLabel(data)
    when (card) {
        WrappedCard.Intro -> IntroCard(data, period)
        WrappedCard.Minutes -> MinutesCard(data, period, animate)
        WrappedCard.TopSong -> TopSongCard(data, period)
        WrappedCard.TopSongs -> TopSongsCard(data, period)
        WrappedCard.TopArtist -> TopArtistCard(data, period)
        WrappedCard.TopArtists -> TopArtistsCard(data, period)
        WrappedCard.Languages -> LanguagesCard(data, period)
        WrappedCard.Clock -> ClockCard(data, period)
        WrappedCard.Highlights -> HighlightsCard(data, period)
        WrappedCard.Summary -> SummaryCard(data, period)
    }
}

// ── Wording shared by the page, the story and the cards ───────────────────────

/** True from 24 December, when "so far" drops from the year's wording. */
private fun WrappedData.yearEnded(): Boolean = today.monthValue == 12 && today.dayOfMonth >= 24

/** The period on every card's footer and the story's bar: "2026 so far", "November 2026", "Week of 16 Nov", "All time". */
@Composable
fun recapPeriodLabel(data: WrappedData): String {
    val today = data.today
    return when (data.period) {
        RecapPeriod.Week -> stringResource(
            R.string.card_period_week,
            today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
        )
        RecapPeriod.Month -> stringResource(R.string.card_period_month, today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()), today.year.toString())
        RecapPeriod.Year -> if (data.yearEnded()) today.year.toString() else stringResource(R.string.card_period_year_so_far, today.year.toString())
        RecapPeriod.AllTime -> stringResource(R.string.card_period_all_time)
    }
}

/** The headline on the page, the Home card and the summary card: "Your 2026 so far", from 24 December "Your 2026 Recap". */
@Composable
fun recapHeadline(data: WrappedData): String {
    val today = data.today
    return when (data.period) {
        RecapPeriod.Week -> stringResource(R.string.recap_headline_week)
        RecapPeriod.Month -> stringResource(R.string.recap_headline_month, today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault()))
        RecapPeriod.Year -> stringResource(if (data.yearEnded()) R.string.recap_headline_year else R.string.recap_headline_year_so_far, today.year.toString())
        RecapPeriod.AllTime -> stringResource(R.string.recap_headline_all_time)
    }
}

@Composable
fun listenerTitle(type: ListenerType): String = stringResource(
    when (type) {
        ListenerType.NightOwl -> R.string.listener_night_owl
        ListenerType.EarlyRiser -> R.string.listener_early_riser
        ListenerType.Daytime -> R.string.listener_daytime
        ListenerType.Evening -> R.string.listener_evening
    },
)

@Composable
private fun listenerLine(type: ListenerType): String = stringResource(
    when (type) {
        ListenerType.NightOwl -> R.string.listener_night_owl_line
        ListenerType.EarlyRiser -> R.string.listener_early_riser_line
        ListenerType.Daytime -> R.string.listener_daytime_line
        ListenerType.Evening -> R.string.listener_evening_line
    },
)

/** The glyph at the clock's hub for each listener type. */
fun listenerSymbol(type: ListenerType): Symbol = when (type) {
    ListenerType.NightOwl -> Symbol.Bedtime
    ListenerType.EarlyRiser -> Symbol.WbTwilight
    ListenerType.Daytime -> Symbol.WbSunny
    ListenerType.Evening -> Symbol.NightsStay
}

/** The hour listening peaks in, midnight first. */
fun busiestHour(hours: LongArray): Int = (0 until 24).maxByOrNull { hours[it] } ?: 0

/** "{type line} Your busiest hour starts at {hour}.", the hour as "midnight", "noon", "7 am" or "9 pm". */
@Composable
fun recapClockLine(type: ListenerType, hours: LongArray): String {
    val peak = busiestHour(hours)
    val hour = when {
        peak == 0 -> stringResource(R.string.hour_midnight)
        peak == 12 -> stringResource(R.string.hour_noon)
        peak < 12 -> stringResource(R.string.hour_am, peak)
        else -> stringResource(R.string.hour_pm, peak - 12)
    }
    return stringResource(R.string.recap_busiest_hour, listenerLine(type), hour)
}

/** An hour on the dial: "12 am", "6 am", "10 pm". */
@Composable
private fun dialHour(hour: Int): String {
    val twelve = if (hour % 12 == 0) 12 else hour % 12
    return stringResource(if (hour < 12) R.string.hour_am else R.string.hour_pm, twelve)
}

/** A plural count split into its figure and its word, so the word can sit smaller under the figure ("214" / "plays"). */
fun splitCount(text: String, figure: String): Pair<String, String> {
    val at = text.indexOf(figure)
    if (at < 0) return figure to text
    return figure to (text.removeRange(at, at + figure.length)).trim()
}

// ── Type ─────────────────────────────────────────────────────────────────────

/** Google Sans Flex at a card size: [width] on the width axis (50 to 100), [round] the ROND axis at 100. */
internal fun cardText(size: Float, line: Float, weight: Int, width: Int = 100, round: Boolean = true, tracking: Float = 0f) = TextStyle(
    fontFamily = GoogleSansFlex.family(GoogleSansFlex.Axes(GoogleSansFlex.opticalSizeFor(size), round, width.coerceIn(50, 100))),
    fontWeight = FontWeight(weight.coerceIn(300, 800)),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** A card's display line (`.rc h2`): heavy, rounded, tracked in a little. */
private fun headline(size: Float, line: Float, weight: Int = 800, width: Int = 100) = cardText(size, line, weight, width, round = true, tracking = -0.02f)

/** A card's label (`.rc-lbl`): "Your top song". */
private val CardLabel = cardText(18f, 24f, 600, round = false)

/**
 * One line that shrinks to fit [maxWidth] rather than wrap or cut: big figures on the cards ("18,460").
 */
@Composable
internal fun FitText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified, textAlign: TextAlign? = null) {
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
        Text(text, style = fitted, color = color, maxLines = 1, softWrap = false, textAlign = textAlign)
    }
}

// ── Frame ────────────────────────────────────────────────────────────────────

/** The card's ground and ink, with the mark, "Attic" and the period at its foot (`.rc-foot`). */
@Composable
private fun CardFrame(background: Color, ink: Color, period: String, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(background)) {
        CompositionLocalProvider(LocalContentColor provides ink) {
            content()
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(ink), contentAlignment = Alignment.Center) {
                    AtticMarkIcon(size = 16.dp, tint = background)
                }
                Text(stringResource(R.string.card_footer_name), style = cardText(16f, 20f, 700), modifier = Modifier.weight(1f))
                Box(Modifier.clip(ExpressiveShapes.Pill).background(ink.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(period, style = cardText(13f, 16f, 600, round = false), maxLines = 1)
                }
            }
        }
    }
}

// ── 1 Intro ──────────────────────────────────────────────────────────────────

@Composable
private fun IntroCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val line = stringResource(
        when (data.period) {
            RecapPeriod.Week -> R.string.card_intro_week
            RecapPeriod.Month -> R.string.card_intro_month
            RecapPeriod.Year -> R.string.card_intro_year
            RecapPeriod.AllTime -> R.string.card_intro_all_time
        },
    )
    // The period as one shape per character: the year, three letters of the month, "Week", "All".
    val word = when (data.period) {
        RecapPeriod.Year -> data.today.year.toString()
        RecapPeriod.Month -> data.today.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).trimEnd('.').take(3)
        RecapPeriod.Week -> stringResource(R.string.card_intro_word_week)
        RecapPeriod.AllTime -> stringResource(R.string.card_intro_word_all)
    }.take(4)
    CardFrame(colors.surfaceContainer, colors.onSurface, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 40.dp)) {
            Text(line, style = headline(54f, 52f))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.card_intro_body, count(data.recap.minutes), count(data.recap.songCount.toLong())),
                style = cardText(17f, 24f, 500, round = false),
                color = colors.onSurfaceVariant,
            )
        }
        val shapes: List<Triple<Shape, Color, Color>> = listOf(
            Triple(CircleShape, colors.primary, colors.onPrimary),
            Triple(CookieShape, colors.primaryContainer, colors.onPrimaryContainer),
            Triple(RoundedCornerShape(74.dp, 74.dp, 24.dp, 24.dp), colors.surfaceContainerHighest, colors.primary),
            Triple(PuffShape, colors.tertiaryContainer, colors.onTertiaryContainer),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 74.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            word.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 2) {
                        val index = row * 2 + i
                        val char = pair.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (char != null) {
                                val (shape, fill, ink) = shapes[index]
                                Box(Modifier.fillMaxSize().clip(shape).background(fill))
                                Text(char.toString(), style = cardText(104f, 104f, 800), color = ink, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 2 Minutes ────────────────────────────────────────────────────────────────

/** How far through its period today is: 0.89 on 21 November for the year. All time is complete. */
private fun WrappedData.elapsed(): Float = when (period) {
    RecapPeriod.Week -> today.dayOfWeek.value / 7f
    RecapPeriod.Month -> today.dayOfMonth / today.lengthOfMonth().toFloat()
    RecapPeriod.Year -> today.dayOfYear / today.lengthOfYear().toFloat()
    RecapPeriod.AllTime -> 1f
}

@Composable
private fun MinutesCard(data: WrappedData, period: String, animate: Boolean) {
    val colors = MaterialTheme.colorScheme
    val target = data.recap.minutes
    val shown = remember { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) shown.animateTo(target.toFloat(), tween(1600, easing = FastOutSlowInEasing)) else shown.snapTo(target.toFloat())
    }
    val ink = colors.onPrimary
    val off = colors.inversePrimary
    val progress = data.elapsed()
    CardFrame(colors.primary, ink, period) {
        // The player's seek line at poster size, stopped where today falls in the period.
        Canvas(Modifier.offset(y = 72.dp).fillMaxWidth().height(112.dp)) {
            val u = size.width / 360f
            val thumb = 348f * u * progress.coerceIn(0f, 1f)
            drawWave(ink, startX = -48f * u, endX = thumb - 22f * u, centerY = 56f * u, wavelength = 96f * u, amplitude = 26f * u, phase = 0.5f, stroke = 18f * u)
            drawRoundRect(ink, Offset(thumb, 0f), Size(12f * u, 112f * u), CornerRadius(6f * u))
            val offStart = thumb + 26f * u
            if (offStart < size.width) {
                drawRoundRect(off, Offset(offStart, 47f * u), Size(372f * u - offStart, 18f * u), CornerRadius(9f * u))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 80.dp)) {
            Text(stringResource(R.string.card_minutes_lead), style = CardLabel)
            FitText(
                count(shown.value.toLong()),
                style = cardText(156f, 128f, 800, width = 50, tracking = -0.01f),
                modifier = Modifier.padding(top = 4.dp).offset(x = (-3).dp).fillMaxWidth(),
            )
            Text(stringResource(R.string.card_minutes_unit), style = cardText(49f, 52f, 800, width = 100), modifier = Modifier.offset(x = (-2).dp))
            Spacer(Modifier.height(14.dp))
            val hours = target / 60
            val days = pluralStringResource(R.plurals.recap_days, data.recap.daysListened, count(data.recap.daysListened.toLong()))
            Text(
                if (hours >= 1) {
                    stringResource(R.string.card_minutes_hours_days, pluralStringResource(R.plurals.card_hours, hours.toInt(), count(hours)), days)
                } else {
                    stringResource(R.string.card_minutes_starting, days)
                },
                style = cardText(18f, 26f, 500, round = false),
            )
        }
    }
}

// ── 3 Top song ───────────────────────────────────────────────────────────────

@Composable
private fun TopSongCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val (track, ranked) = data.songs.first()
    CardFrame(colors.primaryContainer, colors.onPrimaryContainer, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            Cover(Art.of(track.mediaId), size = 312.dp, shape = RoundedCornerShape(ExpressiveShapes.ExtraLarge))
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.card_top_song), style = CardLabel)
                    Spacer(Modifier.height(4.dp))
                    NarrowingText(track.title, style = headline(34f, 36f), maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Text(track.artist, style = cardText(15f, 20f, 500, round = false), color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val figure = count(ranked.plays.toLong())
                val (number, word) = splitCount(pluralStringResource(R.plurals.recap_plays, ranked.plays, figure), figure)
                Box(Modifier.size(112.dp).clip(SunnyShape).background(colors.primary), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FitText(number, cardText(36f, 34f, 800), Modifier.padding(horizontal = 14.dp), color = colors.onPrimary)
                        Text(word, style = cardText(13f, 16f, 700), color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

// ── 4 Top songs ──────────────────────────────────────────────────────────────

@Composable
private fun TopSongsCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val rows = data.songs.take(5)
    CardFrame(colors.primary, colors.onPrimary, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
            Text(stringResource(R.string.card_top_songs_headline).replace('\n', ' '), style = headline(36f, 38f))
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                rows.forEachIndexed { index, (track, ranked) ->
                    val first = index == 0
                    val shape = RoundedCornerShape(
                        topStart = if (first) 28.dp else 6.dp,
                        topEnd = if (first) 28.dp else 6.dp,
                        bottomStart = if (index == rows.lastIndex) 28.dp else 6.dp,
                        bottomEnd = if (index == rows.lastIndex) 28.dp else 6.dp,
                    )
                    val fill = if (first) colors.onPrimary else colors.inverseSurface
                    val ink = if (first) colors.primary else colors.onPrimary
                    CompositionLocalProvider(LocalContentColor provides ink) {
                        Row(
                            Modifier.fillMaxWidth().height(if (first) 104.dp else 72.dp).clip(shape).background(fill).padding(start = 12.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("${index + 1}", style = cardText(22f, 22f, 800), textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
                            Cover(Art.of(track.mediaId), size = if (first) 72.dp else 48.dp, shape = RoundedCornerShape(if (first) 16.dp else 12.dp))
                            Column(Modifier.weight(1f)) {
                                if (first) {
                                    NarrowingText(track.title, style = cardText(20f, 23f, 800), maxLines = 2)
                                    Spacer(Modifier.height(2.dp))
                                } else {
                                    Text(track.title, style = cardText(16f, 21f, 700, round = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(track.artist, style = cardText(13f, 17f, 500, round = false), color = ink.copy(alpha = 0.76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val figure = count(ranked.plays.toLong())
                            val (number, word) = splitCount(pluralStringResource(R.plurals.recap_plays, ranked.plays, figure), figure)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(number, style = cardText(19f, 20f, 800, round = false).copy(fontFeatureSettings = "tnum"))
                                Text(word, style = cardText(10f, 12f, 600, round = false), color = ink.copy(alpha = 0.76f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 5 Top artist ─────────────────────────────────────────────────────────────

@Composable
private fun TopArtistCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val (artist, ranked) = data.artists.first()
    CardFrame(colors.primaryContainer, colors.onPrimaryContainer, period) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Box(Modifier.padding(top = 36.dp).size(300.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                // A second cookie, turned 20°, behind the artist's.
                Box(Modifier.requiredSize(316.dp).rotate(20f).clip(CookieShape).background(colors.primary))
                ArtistCover(Art.of(data.artistCovers[artist.key]), size = 300.dp)
            }
            Spacer(Modifier.height(34.dp))
            Text(stringResource(R.string.card_top_artist), style = CardLabel)
            NarrowingText(artist.name, style = headline(52f, 60f).narrowed(82), maxLines = 1, widths = listOf(82, 66, 50))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.card_top_artist_body,
                    count(ranked.listenedMs / 60_000),
                    pluralStringResource(R.plurals.recap_plays, ranked.plays, count(ranked.plays.toLong())),
                    count(data.recap.artistCount.toLong()),
                ),
                style = cardText(16f, 23f, 500, round = false),
            )
        }
    }
}

// ── 6 Top artists ────────────────────────────────────────────────────────────

@Composable
private fun TopArtistsCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val artists = data.artists.take(5)
    val minutes: @Composable (Long) -> String = { ms -> stringResource(R.string.card_minutes, count(ms / 60_000)) }
    CardFrame(colors.surfaceContainer, colors.onSurface, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
            Text(stringResource(R.string.card_top_artists_headline), style = headline(36f, 38f), color = colors.primary)
            Spacer(Modifier.height(20.dp))
            val (top, topRank) = artists.first()
            val rest = artists.drop(1).chunked(2)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(28.dp, 28.dp, if (rest.isEmpty()) 28.dp else 6.dp, if (rest.isEmpty()) 28.dp else 6.dp))
                    .background(colors.primary)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ArtistCover(Art.of(data.artistCovers[top.key]), size = 104.dp)
                CompositionLocalProvider(LocalContentColor provides colors.onPrimary) {
                    Column(Modifier.weight(1f)) {
                        Text("1", style = cardText(22f, 22f, 800))
                        NarrowingText(top.name, style = cardText(24f, 28f, 800), modifier = Modifier.padding(top = 2.dp))
                        Text(minutes(topRank.listenedMs), style = cardText(14f, 20f, 500, round = false))
                    }
                }
            }
            rest.forEachIndexed { row, pair ->
                val last = row == rest.lastIndex
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    pair.forEachIndexed { column, (artist, ranked) ->
                        val alone = pair.size == 1
                        val shape = RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 6.dp,
                            bottomStart = if (last && column == 0) 28.dp else 6.dp,
                            bottomEnd = if (last && (column == 1 || alone)) 28.dp else 6.dp,
                        )
                        Box(Modifier.weight(1f).clip(shape).background(colors.surfaceContainerHighest).padding(14.dp)) {
                            Column {
                                ArtistCover(Art.of(data.artistCovers[artist.key]), size = 60.dp)
                                NarrowingText(artist.name, style = cardText(16f, 21f, 700, round = false), modifier = Modifier.padding(top = 10.dp))
                                Text(minutes(ranked.listenedMs), style = cardText(13f, 17f, 500, round = false), color = colors.onSurfaceVariant, maxLines = 1)
                            }
                            Text(
                                "${row * 2 + column + 2}",
                                style = cardText(22f, 22f, 800),
                                color = colors.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 7 Languages ──────────────────────────────────────────────────────────────

/** A tile of the languages treemap, in card dp. */
private class Tile(val x: Float, val y: Float, val w: Float, val h: Float)

/**
 * Lays [shares] out as a treemap in a [width] by [height] box with [gap] between tiles: the first
 * takes a full-height column, each next one cuts the rest across its longer side, as sheet 09 draws it.
 */
private fun treemap(shares: List<Float>, width: Float, height: Float, gap: Float): List<Tile> {
    val tiles = ArrayList<Tile>()
    var x = 0f
    var y = 0f
    var w = width
    var h = height
    var remaining = shares.sum()
    shares.forEachIndexed { i, share ->
        if (i == shares.lastIndex || remaining <= 0f) {
            tiles += Tile(x, y, w, h)
            return@forEachIndexed
        }
        val f = share / remaining
        val across = i == 0 || w > h
        if (across) {
            val a = (w - gap) * f
            tiles += Tile(x, y, a, h)
            x += a + gap
            w -= a + gap
        } else {
            val a = (h - gap) * f
            tiles += Tile(x, y, w, a)
            y += a + gap
            h -= a + gap
        }
        remaining -= share
    }
    return tiles
}

@Composable
private fun LanguagesCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val languages = data.recap.languages.take(5)
    val tones = listOf(1f, 0.68f, 0.34f, 0.21f, 0.11f)
    val figures = listOf(68f to 66f, 60f to 56f, 44f to 42f, 24f to 26f, 24f to 26f)
    val names = listOf(28f to 34f, 20f to 26f, 16f to 22f, 12f to 16f, 10.5f to 16f)
    CardFrame(colors.surfaceContainer, colors.onSurface, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
            Text(stringResource(R.string.card_languages_label), style = CardLabel)
            Spacer(Modifier.height(18.dp))
            val boxW = 312f
            val boxH = 444f
            val tiles = treemap(languages.map { it.share.coerceAtLeast(0.005f) }, boxW, boxH, 3f)
            Box(Modifier.fillMaxWidth().height(boxH.dp)) {
                tiles.forEachIndexed { i, tile ->
                    val share = languages[i]
                    fun corner(atX: Boolean, atY: Boolean) = if (atX && atY) 28.dp else 6.dp
                    val left = tile.x < 0.5f
                    val top = tile.y < 0.5f
                    val right = abs(tile.x + tile.w - boxW) < 0.5f
                    val bottom = abs(tile.y + tile.h - boxH) < 0.5f
                    val shape = RoundedCornerShape(corner(left, top), corner(right, top), corner(right, bottom), corner(left, bottom))
                    val fill = lerp(colors.surfaceContainer, colors.primary, tones.getOrElse(i) { 0.08f })
                    val ink = if (i < 2) colors.onPrimary else colors.onSurface
                    Box(
                        Modifier
                            .offset(tile.x.dp, tile.y.dp)
                            .size(tile.w.dp, tile.h.dp)
                            .clip(shape)
                            .background(fill)
                            .padding(if (i < 3) PaddingTile else PaddingSmallTile),
                    ) {
                        if (tile.w >= 40f && tile.h >= 40f) {
                            CompositionLocalProvider(LocalContentColor provides ink) {
                                Column {
                                    val (fs, fl) = figures.getOrElse(i) { figures.last() }
                                    val (ns, nl) = names.getOrElse(i) { names.last() }
                                    FitText(
                                        stringResource(R.string.recap_percent, (share.share * 100).toInt()),
                                        cardText(fs, fl, 800, width = if (i == 0) 50 else 72),
                                    )
                                    FitText(languageName(share.language), cardText(ns, nl, if (i == 0) 800 else if (i < 3) 700 else 600, width = if (i == 4) 50 else 100))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PaddingTile = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 16.dp)
private val PaddingSmallTile = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp)

// ── 8 Listening clock ────────────────────────────────────────────────────────

/**
 * The listening clock (`.clk`): twenty-four round-capped bars from a hub, midnight at the top, each
 * as long as that hour's listening against the busiest. Tracks in surface container highest, bars
 * in primary mixed into [ground], the busiest hour in primary with its time beside it, and the
 * listener type's glyph in the hub. [labelSize] is the hour labels' size in the drawing's 288 units.
 */
@Composable
fun ListeningDial(hours: LongArray, type: ListenerType, ground: Color, modifier: Modifier = Modifier, labelSize: Float = 17f, glyph: Dp = 20.dp) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val peak = busiestHour(hours)
    val fixed = listOf(0, 6, 12, 18)
    val labels = fixed.associateWith { dialHour(it) }
    val peakLabel = dialHour(peak)
    val bar = lerp(ground, colors.primary, 0.58f)
    val density = LocalDensity.current
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 288f
            val c = Offset(144f * u, 144f * u)
            val inner = 34f * u
            val outer = 100f * u
            val stroke = 8.5f * u
            val most = (hours.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
            for (h in 0 until 24) {
                val angle = (h / 24f) * 2f * PI.toFloat() - PI.toFloat() / 2f
                val d = Offset(cos(angle), sin(angle))
                drawLine(colors.surfaceContainerHighest, c + d * inner, c + d * outer, stroke, StrokeCap.Round)
                if (hours[h] > 0) {
                    val length = inner + (outer - inner) * (hours[h] / most)
                    drawLine(if (h == peak) colors.primary else bar, c + d * inner, c + d * length, stroke, StrokeCap.Round)
                }
            }
            val fontSize = with(density) { (labelSize * u).toSp() }
            fun label(text: String, at: Offset, anchor: Int, strong: Boolean) {
                val style = cardText(13f, 13f, if (strong) 800 else 600, round = false).copy(
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    color = if (strong) colors.primary else colors.onSurfaceVariant,
                )
                val layout = measurer.measure(text, style, softWrap = false, maxLines = 1)
                val x = when (anchor) {
                    -1 -> at.x - layout.size.width
                    0 -> at.x - layout.size.width / 2f
                    else -> at.x
                }
                drawText(layout, topLeft = Offset(x, at.y - layout.size.height / 2f))
            }
            label(labels.getValue(0), Offset(144f * u, 30f * u), 0, peak == 0)
            label(labels.getValue(6), Offset(254f * u, 144f * u), 1, peak == 6)
            label(labels.getValue(12), Offset(144f * u, 260f * u), 0, peak == 12)
            label(labels.getValue(18), Offset(34f * u, 144f * u), -1, peak == 18)
            if (peak !in fixed && hours[peak] > 0) {
                val angle = (peak / 24f) * 2f * PI.toFloat() - PI.toFloat() / 2f
                val at = c + Offset(cos(angle), sin(angle)) * (115f * u)
                val anchor = when {
                    at.x < c.x - 8f * u -> -1
                    at.x > c.x + 8f * u -> 1
                    else -> 0
                }
                label(peakLabel, at, anchor, true)
            }
        }
        Box(
            Modifier.fillMaxSize(0.23f).clip(CircleShape).background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(listenerSymbol(type), size = glyph, filled = true, tint = colors.onSecondaryContainer)
        }
    }
}

@Composable
private fun ClockCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val type = data.recap.listenerType
    CardFrame(colors.surfaceContainer, colors.onSurface, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
            Text(stringResource(R.string.card_clock_label), style = CardLabel, color = colors.primary)
            ListeningDial(
                data.recap.hours,
                type,
                ground = colors.surfaceContainer,
                // 324dp in a 312dp column, so 6dp past each side.
                modifier = Modifier.requiredSize(324.dp),
                labelSize = 11f,
            )
            NarrowingText(listenerTitle(type), style = headline(56f, 58f, weight = 800), maxLines = 2, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(8.dp))
            Text(recapClockLine(type, data.recap.hours), style = cardText(16f, 23f, 500, round = false), color = colors.onSurfaceVariant)
        }
    }
}

// ── 9 Highlights ─────────────────────────────────────────────────────────────

@Composable
private fun HighlightsCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val recap = data.recap
    val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    val items = buildList {
        recap.biggestDay?.let { day ->
            add(stringResource(R.string.card_highlight_biggest_day_value, count(day.listenedMs / 60_000)) to stringResource(R.string.card_highlight_biggest_day, day.date.format(dayFormat)))
        }
        recap.onRepeat?.let { repeat ->
            data.songs.firstOrNull { it.first.key == repeat.key }?.first?.title?.let { title ->
                add(stringResource(R.string.card_times, count(repeat.plays.toLong())) to stringResource(R.string.card_highlight_repeat, title))
            }
        }
        if (recap.longestStreak >= 2) {
            add(pluralStringResource(R.plurals.recap_days, recap.longestStreak, count(recap.longestStreak.toLong())) to stringResource(R.string.card_highlight_streak))
        }
        if (recap.discoveries > 0) {
            add(count(recap.discoveries.toLong()) to pluralStringResource(R.plurals.card_highlight_first_plays, recap.discoveries))
        }
        if (recap.radioShare >= 0.05f) {
            add(stringResource(R.string.recap_percent, (recap.radioShare * 100).toInt()) to stringResource(R.string.card_highlight_radio))
        }
    }.take(4)
    CardFrame(colors.tertiaryContainer, colors.onTertiaryContainer, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 34.dp)) {
            Text(stringResource(R.string.card_highlights_headline), style = headline(48f, 48f))
            Spacer(Modifier.height(22.dp))
            val rows = items.chunked(2)
            rows.forEachIndexed { row, pair ->
                if (row > 0) Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    pair.forEachIndexed { column, (value, line) ->
                        val index = row * 2 + column
                        val alone = pair.size == 1
                        val firstRow = row == 0
                        val lastRow = row == rows.lastIndex
                        val shape = RoundedCornerShape(
                            topStart = if (firstRow && column == 0) 64.dp else 8.dp,
                            topEnd = if (firstRow && (column == 1 || alone)) 64.dp else 8.dp,
                            bottomEnd = if (lastRow && (column == 1 || alone)) 64.dp else 8.dp,
                            bottomStart = if (lastRow && column == 0) 64.dp else 8.dp,
                        )
                        val tonal = index == 1 || index == 2
                        val fill = if (tonal) colors.primaryContainer else colors.onTertiaryContainer
                        val ink = if (tonal) colors.onPrimaryContainer else colors.tertiaryContainer
                        Column(
                            Modifier
                                .weight(1f)
                                .height(204.dp)
                                .clip(shape)
                                .background(fill)
                                .padding(start = if (column == 0) 26.dp else 20.dp, end = 14.dp, top = 30.dp),
                        ) {
                            FitText(value, cardText(40f, 42f, 800, width = 80), color = ink)
                            Text(line, style = cardText(14f, 19f, 500, round = false), color = ink, modifier = Modifier.padding(top = 8.dp), maxLines = 5, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ── 10 Summary ───────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(data: WrappedData, period: String) {
    val colors = MaterialTheme.colorScheme
    val recap = data.recap
    val ink = colors.onSurface
    CardFrame(colors.surfaceContainer, ink, period) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Text(recapHeadline(data), style = headline(33f, 38f), color = colors.primary)
            Spacer(Modifier.height(16.dp))
            val blocks = buildList<@Composable (Shape) -> Unit> {
                data.songs.firstOrNull()?.let { (track, _) ->
                    add { shape ->
                        SummaryBlock(shape, stringResource(R.string.card_summary_top_songs), track.title, lead = {
                            Cover(Art.of(track.mediaId), size = 92.dp, shape = RoundedCornerShape(16.dp))
                        }) {
                            data.songs.drop(1).take(4).forEach { (other, _) -> Cover(Art.of(other.mediaId), size = 38.dp, shape = RoundedCornerShape(10.dp)) }
                        }
                    }
                }
                data.artists.firstOrNull()?.let { (artist, _) ->
                    add { shape ->
                        SummaryBlock(shape, stringResource(R.string.card_summary_top_artists), artist.name, lead = {
                            ArtistCover(Art.of(data.artistCovers[artist.key]), size = 92.dp)
                        }) {
                            data.artists.drop(1).take(4).forEach { (other, _) -> ArtistCover(Art.of(data.artistCovers[other.key]), size = 38.dp) }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                blocks.forEachIndexed { index, block ->
                    block(RoundedCornerShape(if (index == 0) 28.dp else 6.dp, if (index == 0) 28.dp else 6.dp, 6.dp, 6.dp))
                }
                Row(Modifier.fillMaxWidth().height(124.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val top = if (blocks.isEmpty()) 28.dp else 6.dp
                    Column(
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(top, 6.dp, 6.dp, 28.dp)).background(colors.primary).padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.card_summary_minutes), style = cardText(13f, 16f, 600, round = false), color = colors.onPrimary)
                        FitText(count(recap.minutes), cardText(56f, 58f, 800, width = 66), color = colors.onPrimary)
                    }
                    Column(
                        Modifier.width(112.dp).fillMaxSize().clip(RoundedCornerShape(6.dp, top, 28.dp, 6.dp)).background(colors.primaryContainer).padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.card_summary_mostly_in), style = cardText(13f, 16f, 600, round = false), color = colors.onPrimaryContainer)
                        val mostly = recap.languages.firstOrNull()?.let { languageName(it.language) } ?: listenerTitle(recap.listenerType)
                        FitText(mostly, cardText(30f, 34f, 800), Modifier.padding(top = 4.dp), color = colors.onPrimaryContainer)
                    }
                }
            }
        }
        val wave = colors.primaryContainer
        Canvas(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 72.dp).size(312.dp, 24.dp)) {
            val u = size.width / 312f
            drawWave(wave, startX = 6f * u, endX = 306f * u, centerY = 12f * u, wavelength = 48f * u, amplitude = 7.5f * u, phase = 0.5f, stroke = 8f * u)
        }
    }
}

@Composable
private fun SummaryBlock(shape: Shape, label: String, name: String, lead: @Composable () -> Unit, more: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(124.dp).clip(shape).background(colors.surfaceContainerHighest).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        lead()
        Column(Modifier.weight(1f)) {
            Text(label, style = cardText(13f, 16f, 600, round = false), color = colors.onSurfaceVariant)
            NarrowingText(name, style = cardText(19f, 24f, 800), modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { more() }
        }
    }
}

/** The scheme a card is drawn in, for a surface that sits with it (the story screen takes its hue). */
@Composable
fun rememberRecapCardScheme(card: WrappedCard, data: WrappedData): ColorScheme {
    val seed = rememberRecapCardSeed(card, data)
    return remember(seed) { ArtScheme.colorScheme(seed.toArgb(), dark = true, fallback = darkColorScheme()) }
}
