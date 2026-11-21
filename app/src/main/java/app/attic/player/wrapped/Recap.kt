package app.attic.player.wrapped

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** One logged listen, as the recap reads it. */
class Listen(
    val trackKey: String,
    val startedAt: Long,
    val listenedMs: Long,
    val outcome: String,
    val contextName: String?,
)

/** What the recap needs to know about a song. [language] is a code from LanguageLabels, or null. */
class SongInfo(
    val artistKeys: List<String>,
    val language: String?,
)

enum class RecapPeriod(val label: String) {
    Week("This week"),
    Month("This month"),
    Year("This year"),
    AllTime("All time"),
    ;

    /** Start inclusive, end exclusive, in epoch milliseconds. All time starts at zero. */
    fun range(now: Long, zone: ZoneId): LongRange {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = when (this) {
            Week -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Month -> today.withDayOfMonth(1)
            Year -> today.withDayOfYear(1)
            AllTime -> return 0L until Long.MAX_VALUE
        }
        return start.atStartOfDay(zone).toInstant().toEpochMilli() until Long.MAX_VALUE
    }
}

class RankedSong(val key: String, val plays: Int, val listenedMs: Long)

class RankedArtist(val key: String, val plays: Int, val listenedMs: Long)

class LanguageShare(val language: String, val listenedMs: Long, val share: Float)

class DayTotal(val date: LocalDate, val listenedMs: Long)

class RepeatDay(val key: String, val date: LocalDate, val plays: Int)

class Recap(
    val listenedMs: Long,
    val plays: Int,
    val songCount: Int,
    val artistCount: Int,
    val topSongs: List<RankedSong>,
    val topArtists: List<RankedArtist>,
    val languages: List<LanguageShare>,
    /** Listening time by the hour it started in, midnight first. */
    val hours: LongArray,
    val daysListened: Int,
    val longestStreak: Int,
    val biggestDay: DayTotal?,
    val onRepeat: RepeatDay?,
    /** Songs whose first play ever falls inside this period. */
    val discoveries: Int,
    /** Share of listening time that radio chose. */
    val radioShare: Float,
    val firstListen: Listen?,
    /** When the listening log begins, which is as far back as any recap can see. */
    val loggedSince: Long?,
) {
    val minutes: Long get() = listenedMs / 60_000
    val isEmpty: Boolean get() = plays == 0
    val listenerType: ListenerType get() = ListenerType.from(hours)
}

/** When in the day someone mostly listens, from the three-hour stretch with the most listening. */
enum class ListenerType(val title: String, val line: String) {
    NightOwl("Night owl", "Most of your listening happens after dark."),
    EarlyRiser("Early riser", "You start the day with music."),
    Daytime("Daytime listener", "Music keeps your days going."),
    Evening("Evening unwinder", "Your evenings have a soundtrack."),
    ;

    companion object {
        fun from(hours: LongArray): ListenerType {
            val peak = (0 until 24).maxByOrNull { h -> hours[h] + hours[(h + 1) % 24] + hours[(h + 2) % 24] } ?: 0
            return when ((peak + 1) % 24) {
                in 5..9 -> EarlyRiser
                in 10..16 -> Daytime
                in 17..21 -> Evening
                else -> NightOwl
            }
        }
    }
}

object RecapBuilder {
    /** A listen this long or longer counts as a play, the threshold streaming services use. */
    const val PLAY_MS = 30_000L
    private const val FINISHED = "finished"
    private const val RADIO_PREFIX = "Radio · "
    private const val TOP = 10
    private const val REPEAT_MIN = 3

    /** [all] is the whole log, so discoveries can tell first plays from returning ones. */
    fun build(all: List<Listen>, range: LongRange, songs: Map<String, SongInfo>, zone: ZoneId): Recap {
        val inRange = all.filter { it.startedAt in range && it.listenedMs > 0 }.sortedBy { it.startedAt }
        val played = inRange.filter(::isPlay)
        val listenedMs = inRange.sumOf { it.listenedMs }

        val topSongs = played.groupBy { it.trackKey }
            .map { (key, list) -> RankedSong(key, list.size, list.sumOf { it.listenedMs }) }
            .sortedWith(compareByDescending<RankedSong> { it.plays }.thenByDescending { it.listenedMs })

        val artistTime = HashMap<String, Long>()
        val artistPlays = HashMap<String, Int>()
        for (listen in inRange) {
            val keys = songs[listen.trackKey]?.artistKeys.orEmpty()
            for (key in keys) {
                artistTime[key] = (artistTime[key] ?: 0) + listen.listenedMs
                if (isPlay(listen)) artistPlays[key] = (artistPlays[key] ?: 0) + 1
            }
        }
        val topArtists = artistTime.entries
            .filter { (artistPlays[it.key] ?: 0) > 0 }
            .map { RankedArtist(it.key, artistPlays[it.key] ?: 0, it.value) }
            .sortedWith(compareByDescending<RankedArtist> { it.listenedMs }.thenByDescending { it.plays })

        val languageTime = inRange
            .mapNotNull { listen -> songs[listen.trackKey]?.language?.let { it to listen.listenedMs } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }
        val knownMs = languageTime.values.sum()
        val languages = languageTime.entries
            .sortedByDescending { it.value }
            .map { LanguageShare(it.key, it.value, if (knownMs == 0L) 0f else it.value.toFloat() / knownMs) }

        val hours = LongArray(24)
        val byDay = HashMap<LocalDate, Long>()
        for (listen in inRange) {
            val time = Instant.ofEpochMilli(listen.startedAt).atZone(zone)
            hours[time.hour] += listen.listenedMs
            byDay[time.toLocalDate()] = (byDay[time.toLocalDate()] ?: 0) + listen.listenedMs
        }
        val days = byDay.keys.sorted()

        val onRepeat = played
            .groupBy { it.trackKey to Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .map { (keyDay, list) -> RepeatDay(keyDay.first, keyDay.second, list.size) }
            .filter { it.plays >= REPEAT_MIN }
            .maxWithOrNull(compareBy<RepeatDay> { it.plays }.thenBy { it.date })

        val firstPlays = HashMap<String, Long>()
        for (listen in all) {
            if (!isPlay(listen)) continue
            val seen = firstPlays[listen.trackKey]
            if (seen == null || listen.startedAt < seen) firstPlays[listen.trackKey] = listen.startedAt
        }

        val radioMs = inRange.filter { it.contextName?.startsWith(RADIO_PREFIX) == true }.sumOf { it.listenedMs }

        return Recap(
            listenedMs = listenedMs,
            plays = played.size,
            songCount = topSongs.size,
            artistCount = topArtists.size,
            topSongs = topSongs.take(TOP),
            topArtists = topArtists.take(TOP),
            languages = languages,
            hours = hours,
            daysListened = days.size,
            longestStreak = longestStreak(days),
            biggestDay = byDay.maxByOrNull { it.value }?.let { DayTotal(it.key, it.value) },
            onRepeat = onRepeat,
            discoveries = firstPlays.values.count { it in range },
            radioShare = if (listenedMs == 0L) 0f else radioMs.toFloat() / listenedMs,
            firstListen = played.firstOrNull(),
            loggedSince = all.minOfOrNull { it.startedAt },
        )
    }

    private fun isPlay(listen: Listen) = listen.listenedMs >= PLAY_MS || listen.outcome == FINISHED

    /** The most days in a row with any listening. [days] must be sorted. */
    internal fun longestStreak(days: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
