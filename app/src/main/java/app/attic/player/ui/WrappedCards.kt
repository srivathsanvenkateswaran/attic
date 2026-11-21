package app.attic.player.ui

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.attic.player.data.Artist
import app.attic.player.data.LanguageLabels
import app.attic.player.data.Track
import app.attic.player.ui.classic.*
import app.attic.player.wrapped.RankedArtist
import app.attic.player.wrapped.RankedSong
import app.attic.player.wrapped.Recap
import app.attic.player.wrapped.RecapPeriod
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A recap with its songs and artists looked up in the library, ready to draw. */
class WrappedData(
    val recap: Recap,
    val period: RecapPeriod,
    /** "2026 so far", "November 2026". Printed on every card so a shared image says what it covers. */
    val periodLabel: String,
    val headline: String,
    val songs: List<Pair<Track, RankedSong>>,
    val artists: List<Pair<Artist, RankedArtist>>,
    /** The cover shown for each artist: their most played song in this period. */
    val artistCovers: Map<String, Long>,
)

enum class WrappedCard(val fileName: String) {
    Intro("intro"),
    Minutes("minutes"),
    TopSong("top-song"),
    TopSongs("top-songs"),
    TopArtist("top-artist"),
    TopArtists("top-artists"),
    Languages("languages"),
    Clock("listening-clock"),
    Highlights("highlights"),
    Summary("summary"),
    ;

    companion object {
        /** The cards this recap has enough data for, in story order. */
        fun available(data: WrappedData): List<WrappedCard> = entries.filter { card ->
            when (card) {
                TopSong -> data.songs.isNotEmpty()
                TopSongs -> data.songs.size >= 3
                TopArtist -> data.artists.isNotEmpty()
                TopArtists -> data.artists.size >= 3
                Languages -> data.recap.languages.isNotEmpty()
                Highlights -> data.recap.biggestDay != null
                else -> true
            }
        }
    }
}

private val Marigold = Color(0xFFFFB627)
private val Plum = Color(0xFF2A0E3D)
private val Pink = Color(0xFFFF5D8F)
private val Cobalt = Color(0xFF2B59FF)
private val Mint = Color(0xFF9FF0C9)
private val Paper = Color.White
private val Tape = Color.Black

/** One colour per language, so the same language looks the same on every card. */
private val LanguageColors = mapOf(
    "ta" to Marigold,
    "hi" to Pink,
    "en" to Cobalt,
    "te" to Color(0xFF7A3CFF),
    "ml" to Color(0xFF00A86B),
    "kn" to Color(0xFFFF7A1A),
    LanguageLabels.NO_VOCALS to Plum,
)

fun languageColor(code: String): Color = LanguageColors[code] ?: Color(0xFF8C8C8C)

private val CARD_WIDTH = 360.dp
private val CARD_HEIGHT = 640.dp

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun plays(count: Int) = if (count == 1) "1 play" else "$count plays"

/**
 * A card laid out on a fixed 360 by 640 canvas and scaled to fit, so the story, the thumbnails and
 * the shared image all match. When [layer] is set, the card draws into it for sharing.
 */
@Composable
fun ScaledWrappedCard(card: WrappedCard, data: WrappedData, modifier: Modifier = Modifier, layer: GraphicsLayer? = null, animate: Boolean = false) {
    BoxWithConstraints(modifier.aspectRatio(9f / 16f).clipToBounds(), contentAlignment = Alignment.Center) {
        val scale = maxWidth / CARD_WIDTH
        val density = LocalDensity.current
        Box(
            Modifier
                .requiredSize(CARD_WIDTH, CARD_HEIGHT)
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
                CardContent(card, data, animate)
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

private fun display(size: TextUnit, weight: FontWeight = FontWeight.ExtraBold, line: Float = 0.92f) =
    TextStyle(fontWeight = weight, fontSize = size, lineHeight = size * line, letterSpacing = (-0.04).em)

@Composable
private fun cardType(style: TextStyle): TextStyle = MaterialTheme.typography.bodyLarge.merge(style)

/** Full-bleed colour, with the Attic mark and the period at the foot so a shared image says what it covers. */
@Composable
private fun CardFrame(data: WrappedData, background: Color, ink: Color, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(background)) {
        content()
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp).background(Tape, CircleShape), contentAlignment = Alignment.Center) {
                AtticMark(12.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text("Attic", style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)), color = ink)
            Spacer(Modifier.weight(1f))
            Text(data.periodLabel, style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp)), color = ink.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun Label(text: String, ink: Color, modifier: Modifier = Modifier) {
    Text(text, style = cardType(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)), color = ink, modifier = modifier)
}

@Composable
private fun IntroCard(data: WrappedData) {
    CardFrame(data, Plum, Paper) {
        // The year or month, set so big it runs off the bottom edge.
        val big = when (data.period) {
            RecapPeriod.Year -> data.periodLabel.take(4)
            RecapPeriod.Month -> data.periodLabel.substringBefore(' ').take(3)
            RecapPeriod.Week -> "Week"
            RecapPeriod.AllTime -> "All"
        }
        Text(
            big,
            style = cardType(display(if (big.length > 3) 148.sp else 190.sp, line = 0.8f)),
            color = Marigold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset(x = (-8).dp, y = 30.dp)
                .padding(bottom = 40.dp),
        )
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 56.dp)) {
            Text(introLine(data.period), style = cardType(display(54.sp)), color = Paper)
            Spacer(Modifier.height(18.dp))
            Text(
                "${formatCount(data.recap.minutes)} minutes across ${formatCount(data.recap.songCount.toLong())} songs. Here's what you played.",
                style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)),
                color = Paper.copy(alpha = 0.85f),
            )
        }
    }
}

/** The year or month already fills the bottom of the intro card, so the words don't repeat it. */
private fun introLine(period: RecapPeriod) = when (period) {
    RecapPeriod.Week -> "Your week in music"
    RecapPeriod.Month -> "Your month in music"
    RecapPeriod.Year -> "Your year so far"
    RecapPeriod.AllTime -> "Everything you've played"
}

@Composable
private fun MinutesCard(data: WrappedData, animate: Boolean) {
    val target = data.recap.minutes
    val shown = remember { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) shown.animateTo(target.toFloat(), tween(1600, easing = FastOutSlowInEasing)) else shown.snapTo(target.toFloat())
    }
    CardFrame(data, Cobalt, Paper) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Mint, radius = size.width * 0.62f, center = Offset(size.width * 1.02f, size.height * 0.12f))
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 210.dp)) {
            Label("You listened for", Paper)
            Spacer(Modifier.height(6.dp))
            val digits = formatCount(shown.value.toLong())
            Text(
                digits,
                style = cardType(display(if (formatCount(target).length > 5) 92.sp else 116.sp)),
                color = Paper,
                maxLines = 1,
                softWrap = false,
            )
            Text("minutes", style = cardType(display(44.sp)), color = Mint)
            Spacer(Modifier.height(24.dp))
            val hours = target / 60
            Text(
                buildString {
                    append(if (hours >= 1) "That's about ${formatCount(hours)} ${if (hours == 1L) "hour" else "hours"}" else "Just getting started")
                    append(" across ${data.recap.daysListened} ${if (data.recap.daysListened == 1) "day" else "days"}.")
                },
                style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)),
                color = Paper.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun TopSongCard(data: WrappedData) {
    val (track, ranked) = data.songs.first()
    CardFrame(data, Marigold, Plum) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 40.dp)) {
            Label("Your top song", Plum)
            Spacer(Modifier.height(22.dp))
            CoverArt(
                track.mediaId,
                RoundedCornerShape(4.dp),
                Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .rotate(-4f),
                iconSize = 64.dp,
            )
            Spacer(Modifier.height(30.dp))
            Text(track.title, style = cardType(display(40.sp)), color = Plum, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(track.artist, style = cardType(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)), color = Plum.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.background(Plum, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                Text("Played ${if (ranked.plays == 1) "once" else "${ranked.plays} times"}", style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)), color = Marigold)
            }
        }
    }
}

@Composable
private fun RankedRows(rows: List<Triple<Long?, String, String>>, ink: Color, rank: Color, round: Boolean, trailing: List<String> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEachIndexed { index, (cover, title, detail) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}",
                    style = cardType(display(40.sp)),
                    color = rank,
                    modifier = Modifier.width(38.dp),
                )
                CoverArt(cover, if (round) CircleShape else RoundedCornerShape(3.dp), Modifier.size(58.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 21.sp)), color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(detail, style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)), color = ink.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                trailing.getOrNull(index)?.let { value ->
                    Spacer(Modifier.width(10.dp))
                    Text(value, style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)), color = rank)
                }
            }
        }
    }
}

@Composable
private fun TopSongsCard(data: WrappedData) {
    CardFrame(data, Paper, Plum) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp)) {
            Text("Songs you\nkept coming\nback to", style = cardType(display(46.sp)), color = Plum)
            Spacer(Modifier.height(30.dp))
            RankedRows(
                data.songs.take(5).map { (track, _) -> Triple(track.mediaId, track.title, track.artist) },
                ink = Plum,
                rank = Pink,
                round = false,
                trailing = data.songs.take(5).map { "${it.second.plays}×" },
            )
        }
    }
}

@Composable
private fun TopArtistCard(data: WrappedData) {
    val (artist, ranked) = data.artists.first()
    CardFrame(data, Pink, Plum) {
        CoverArt(
            data.artistCovers[artist.key],
            CircleShape,
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 60.dp)
                .size(330.dp),
            iconSize = 72.dp,
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 84.dp)) {
            Label("Your top artist", Plum)
            Spacer(Modifier.height(8.dp))
            Text(artist.name, style = cardType(display(if (artist.name.length > 14) 50.sp else 64.sp)), color = Plum, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Text(
                "${formatCount(ranked.listenedMs / 60_000)} minutes over ${plays(ranked.plays)}. Number one of the ${data.recap.artistCount} artists you played.",
                style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp)),
                color = Plum,
            )
        }
    }
}

@Composable
private fun TopArtistsCard(data: WrappedData) {
    CardFrame(data, Plum, Paper) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp)) {
            Text("The artists\non repeat", style = cardType(display(50.sp)), color = Mint)
            Spacer(Modifier.height(34.dp))
            RankedRows(
                data.artists.take(5).map { (artist, ranked) -> Triple(data.artistCovers[artist.key], artist.name, "${formatCount(ranked.listenedMs / 60_000)} minutes") },
                ink = Paper,
                rank = Mint,
                round = true,
            )
        }
    }
}

@Composable
private fun LanguagesCard(data: WrappedData) {
    val languages = data.recap.languages
    val top = languages.first()
    CardFrame(data, Mint, Plum) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp)) {
            Label("What you listened in", Plum)
            Spacer(Modifier.height(6.dp))
            Text("${(top.share * 100).toInt()}%", style = cardType(display(128.sp, line = 0.85f)), color = Plum)
            Text(LanguageLabels.displayName(top.language), style = cardType(display(48.sp)), color = Plum)
            Spacer(Modifier.height(30.dp))
            // Each language is a thick band whose length is its share.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.take(5).forEach { share ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.12f + 0.62f * share.share)
                                .height(34.dp)
                                .background(languageColor(share.language), RoundedCornerShape(topEnd = 17.dp, bottomEnd = 17.dp)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${LanguageLabels.displayName(share.language)} ${(share.share * 100).toInt()}%",
                            style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)),
                            color = Plum,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Twenty-four bars around a dial, midnight at the top, each as long as that hour's listening. */
@Composable
fun ListeningClock(hours: LongArray, bar: Color, track: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2, size.height / 2)
        val inner = size.minDimension * 0.18f
        val outer = size.minDimension * 0.48f
        val most = (hours.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val stroke = size.minDimension * 0.042f
        for (hour in 0 until 24) {
            val angle = (hour / 24f) * 2 * PI.toFloat() - PI.toFloat() / 2
            val direction = Offset(cos(angle), sin(angle))
            drawLine(track, center + direction * inner, center + direction * outer, strokeWidth = stroke, cap = StrokeCap.Round)
            val length = inner + (outer - inner) * (hours[hour] / most)
            if (hours[hour] > 0) drawLine(bar, center + direction * inner, center + direction * length, strokeWidth = stroke, cap = StrokeCap.Round)
        }
        drawCircle(bar, radius = inner * 0.55f, center = center, style = Stroke(width = stroke * 0.6f))
    }
}

@Composable
private fun ClockCard(data: WrappedData) {
    val type = data.recap.listenerType
    val peak = (0 until 24).maxByOrNull { data.recap.hours[it] } ?: 0
    CardFrame(data, Tape, Paper) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp)) {
            Label("Your listening clock", Marigold)
            Spacer(Modifier.height(20.dp))
            ListeningClock(data.recap.hours, Marigold, Color(0xFF2A2A2A), Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Text(type.title, style = cardType(display(52.sp)), color = Paper)
            Spacer(Modifier.height(10.dp))
            Text(
                "${type.line} Your busiest hour starts at ${hourLabel(peak)}.",
                style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp)),
                color = Paper.copy(alpha = 0.8f),
            )
        }
    }
}

fun hourLabel(hour: Int): String = when {
    hour == 0 -> "midnight"
    hour == 12 -> "noon"
    hour < 12 -> "$hour am"
    else -> "${hour - 12} pm"
}

@Composable
private fun HighlightsCard(data: WrappedData) {
    val recap = data.recap
    val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    CardFrame(data, Cobalt, Paper) {
        val items = buildList {
            recap.biggestDay?.let { day ->
                add("${formatCount(day.listenedMs / 60_000)} min" to "on ${day.date.format(dayFormat)}, your biggest day")
            }
            recap.onRepeat?.let { repeat ->
                data.songs.firstOrNull { it.first.key == repeat.key }?.first?.title?.let { add("${repeat.plays}×" to "$it in a single day") }
            }
            if (recap.longestStreak >= 2) add("${recap.longestStreak} days" to "in a row with music")
            if (recap.discoveries > 0) add("${recap.discoveries}" to if (recap.discoveries == 1) "song played for the first time" else "songs played for the first time")
            if (recap.radioShare >= 0.05f) add("${(recap.radioShare * 100).toInt()}%" to "of your listening picked by radio")
        }.take(4)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
            Text("The good bits", style = cardType(display(52.sp)), color = Marigold)
            items.forEach { (value, line) -> Highlight(value, line) }
        }
    }
}

@Composable
private fun Highlight(value: String, line: String) {
    Column {
        Text(value, style = cardType(display(44.sp)), color = Paper, maxLines = 1)
        Text(line, style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp)), color = Paper.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SummaryCard(data: WrappedData) {
    val recap = data.recap
    CardFrame(data, Plum, Paper) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                CoverArt(data.songs.firstOrNull()?.first?.mediaId, RoundedCornerShape(4.dp), Modifier.size(176.dp), iconSize = 48.dp)
                Spacer(Modifier.width(16.dp))
                Text(data.headline, style = cardType(display(34.sp)), color = Marigold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(34.dp))
            Row {
                SummaryColumn("Top artists", data.artists.take(5).map { it.first.name }, Modifier.weight(1f))
                Spacer(Modifier.width(14.dp))
                SummaryColumn("Top songs", data.songs.take(5).map { it.first.title }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(34.dp))
            Row {
                SummaryStat("Minutes", formatCount(recap.minutes), Modifier.weight(1f))
                Spacer(Modifier.width(14.dp))
                SummaryStat(
                    "Mostly in",
                    recap.languages.firstOrNull()?.let { LanguageLabels.displayName(it.language) } ?: recap.listenerType.title,
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(40.dp))
            // The reels of a cassette, the one ornament on the card everyone shares.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Reel(Pink)
                Box(Modifier.weight(1f).height(6.dp).background(Pink.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
                Reel(Mint)
            }
        }
    }
}

@Composable
private fun Reel(color: Color) {
    Canvas(Modifier.size(34.dp)) {
        drawCircle(color, style = Stroke(width = size.minDimension * 0.14f))
        for (spoke in 0 until 3) {
            val angle = spoke * 2 * PI.toFloat() / 3
            drawLine(color, center, center + Offset(cos(angle), sin(angle)) * (size.minDimension * 0.36f), strokeWidth = size.minDimension * 0.1f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun SummaryColumn(title: String, names: List<String>, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)), color = Paper.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        names.forEachIndexed { index, name ->
            Text(
                "${index + 1}  $name",
                style = cardType(TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 27.sp)),
                color = Paper,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryStat(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = cardType(TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)), color = Paper.copy(alpha = 0.7f))
        Text(value, style = cardType(display(40.sp)), color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The label printed on cards, such as "2026 so far" or "November 2026". */
fun periodLabel(period: RecapPeriod, today: java.time.LocalDate): String = when (period) {
    RecapPeriod.Week -> "Week of ${today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}"
    RecapPeriod.Month -> "${today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())} ${today.year}"
    RecapPeriod.Year -> if (today.monthValue == 12 && today.dayOfMonth >= 24) "${today.year}" else "${today.year} so far"
    RecapPeriod.AllTime -> "All time"
}

fun headline(period: RecapPeriod, today: java.time.LocalDate): String = when (period) {
    RecapPeriod.Week -> "Your week in music"
    RecapPeriod.Month -> "Your ${today.month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())} in music"
    RecapPeriod.Year -> if (today.monthValue == 12 && today.dayOfMonth >= 24) "Your ${today.year} Wrapped" else "Your ${today.year} so far"
    RecapPeriod.AllTime -> "Everything you've played"
}
