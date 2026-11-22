package app.attic.player.ui.model

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import app.attic.player.attic
import app.attic.player.data.Artist
import app.attic.player.data.Library
import app.attic.player.data.Track
import app.attic.player.ui.LocalUi
import app.attic.player.ui.ShareImage
import app.attic.player.ui.TrackMenuRequest
import app.attic.player.wrapped.RankedArtist
import app.attic.player.wrapped.RankedSong
import app.attic.player.wrapped.Recap
import app.attic.player.wrapped.RecapPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/** A recap with its songs and artists looked up in the library, ready to draw. */
@Immutable
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
    /** The day the recap was made, which a style words the period and headline from. */
    val today: LocalDate = LocalDate.now(),
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

/** Looks up a recap's songs and artists so the cards can show names and covers. */
fun wrappedData(recap: Recap, period: RecapPeriod, library: Library, today: LocalDate = LocalDate.now()): WrappedData {
    val songs = recap.topSongs.mapNotNull { ranked -> library.trackByKey[ranked.key]?.let { it to ranked } }
    val artists = recap.topArtists.mapNotNull { ranked -> library.artistByKey[ranked.key]?.let { it to ranked } }
    // Each artist gets art no higher-ranked artist already shows. Songs on one album share a cover, so albums count as one.
    val used = HashSet<String>()
    fun art(track: app.attic.player.data.Track) = track.albumKey ?: track.key
    val covers = artists.associate { (artist, _) ->
        val candidates = songs.map { it.first }.filter { artist.key in it.artistKeys } + artist.tracks
        val pick = candidates.firstOrNull { art(it) !in used } ?: candidates.first()
        used += art(pick)
        artist.key to pick.mediaId
    }
    return WrappedData(recap, period, periodLabel(period, today), headline(period, today), songs, artists, covers, today)
}

// Wrapped page

@Immutable
class WrappedPageState(
    val period: RecapPeriod,
    /** The last recap loaded, which may still be for the previous period while the new one loads. */
    val data: WrappedData?,
    /** No recap for [period] yet. */
    val loading: Boolean,
    /** The cards [data] has enough for, in story order. */
    val cards: List<WrappedCard>,
    /** The card the story is open at, or null when it is closed. */
    val storyAt: Int?,
    /** The top songs are what's playing, and they are playing. */
    val topSongsPlaying: Boolean,
    val currentKey: String?,
    /** Something is playing, so the current song's wave moves. */
    val playing: Boolean = false,
)

class WrappedPageActions(
    val back: () -> Unit,
    val setPeriod: (RecapPeriod) -> Unit,
    val openStory: (Int) -> Unit,
    val closeStory: () -> Unit,
    /** Pauses or resumes the top songs, or plays them from the top. */
    val playTopSongs: () -> Unit,
    val playTopSong: (index: Int) -> Unit,
    val showMenu: (Track) -> Unit,
    val openArtist: (Artist) -> Unit,
)

@Composable
fun rememberWrappedModel(library: Library): UiModel<WrappedPageState, WrappedPageActions> {
    val app = LocalContext.current.attic
    val ui = LocalUi.current
    var period by rememberSaveable { mutableStateOf(RecapPeriod.Year) }
    val listenCount by app.wrapped.listenCount().collectAsStateWithLifecycle(0)
    val data by produceState<WrappedData?>(null, period, listenCount, library) {
        value = wrappedData(app.wrapped.recap(period), period, library)
    }
    var storyAt by rememberSaveable { mutableStateOf<Int?>(null) }
    val player by app.player.state.collectAsStateWithLifecycle()

    val loaded = data
    val tracks = loaded?.songs.orEmpty().map { it.first }
    val isTopSongs = player.contextName == TOP_SONGS
    val state = WrappedPageState(
        period = period,
        data = loaded,
        loading = loaded == null || loaded.period != period,
        cards = loaded?.let { WrappedCard.available(it) }.orEmpty(),
        storyAt = storyAt,
        topSongsPlaying = player.isPlaying && isTopSongs,
        currentKey = player.current?.key,
        playing = player.isPlaying,
    )
    val actions = WrappedPageActions(
        back = { ui.back() },
        setPeriod = { period = it },
        openStory = { storyAt = it },
        closeStory = { storyAt = null },
        playTopSongs = { if (isTopSongs) app.player.togglePlayPause() else app.player.play(tracks, 0, TOP_SONGS, shuffled = false) },
        playTopSong = { index -> app.player.play(tracks, index, TOP_SONGS, shuffled = false) },
        showMenu = { ui.showMenu(TrackMenuRequest(it)) },
        openArtist = { ui.openArtist(it.key) },
    )
    return UiModel(state, actions)
}

private const val TOP_SONGS = "Your top songs"

// Wrapped story

@Immutable
class WrappedStoryState(
    val data: WrappedData,
    val cards: List<WrappedCard>,
    /** The card showing now. */
    val page: Int,
    /** How far the current card's timer has run, from 0 to 1. Read it where it is drawn, as it changes every frame. */
    val progress: () -> Float,
)

class WrappedStoryActions(
    /** Holding a card pauses the story. */
    val setHeld: (Boolean) -> Unit,
    /** A tap goes back a card, or forward, closing the story after the last. */
    val tap: (back: Boolean) -> Unit,
    val close: () -> Unit,
    /** Saves the current card to Pictures/Attic. [capture] renders it as the style draws it. */
    val save: (capture: suspend () -> Bitmap) -> Unit,
    /** Shares the current card through the system share sheet. */
    val share: (capture: suspend () -> Bitmap) -> Unit,
)

/** Cards advance every seven seconds, waiting while held, while saving or sharing, and while the app is covered. */
@Composable
fun rememberWrappedStoryModel(data: WrappedData, cards: List<WrappedCard>, start: Int, onClose: () -> Unit): UiModel<WrappedStoryState, WrappedStoryActions> {
    val context = LocalContext.current
    val app = context.attic
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(start.coerceIn(0, cards.lastIndex)) }
    var held by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val progress = remember(page) { Animatable(0f) }
    val card = cards[page]

    // The share sheet covers the app, so the story waits while it's open.
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val away = !lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    LaunchedEffect(page, held, busy, away) {
        if (held || busy || away) return@LaunchedEffect
        val remaining = ((1f - progress.value) * CARD_MS).toInt()
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        if (page < cards.lastIndex) page++
    }

    fun export(save: Boolean, capture: suspend () -> Bitmap) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val bitmap = capture()
                val name = "attic-wrapped-${card.fileName}-${System.currentTimeMillis() / 1000}"
                if (save) {
                    app.messages.tryEmit(if (ShareImage.save(context, bitmap, name)) "Saved to Pictures/Attic" else "Couldn't save the image")
                } else {
                    ShareImage.share(context, bitmap, name)
                }
            } finally {
                busy = false
            }
        }
    }

    val state = WrappedStoryState(data, cards, page, progress = { progress.value })
    val actions = WrappedStoryActions(
        setHeld = { held = it },
        tap = { back ->
            if (back) {
                if (page > 0) page--
            } else if (page < cards.lastIndex) {
                page++
            } else {
                onClose()
            }
        },
        close = onClose,
        save = { capture -> export(save = true, capture) },
        share = { capture -> export(save = false, capture) },
    )
    return UiModel(state, actions)
}

private const val CARD_MS = 7_000
