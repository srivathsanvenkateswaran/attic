package app.attic.player.home

import app.attic.player.playback.ListenTracker

/**
 * The three Home shelves built from your own listening: "On repeat", "Haven't heard in a while" and
 * "Newly added". Pure arithmetic over rows already read from the database, with the time passed in,
 * so it runs off the main thread and can be tested with a fixed clock.
 */
object HomeShelves {
    /** A song in the library, with when its file arrived. */
    class Song(val key: String, val dateAddedSec: Long)

    /** The `history` row: every play, and the last. */
    class Played(val key: String, val lastPlayedAt: Long, val playCount: Int)

    /** One `listening_events` row from the last [ON_REPEAT_DAYS] days. */
    class Listen(val key: String, val startedAt: Long, val outcome: String)

    /** Song keys per shelf, best first. A shelf with fewer than [MIN_SONGS] is empty, so it hides. */
    data class Result(
        val onRepeat: List<String> = emptyList(),
        val notLately: List<String> = emptyList(),
        val newlyAdded: List<String> = emptyList(),
    )

    const val MIN_SONGS = 4
    const val MAX_SONGS = 20
    const val ON_REPEAT_DAYS = 30
    const val NOT_LATELY_DAYS = 60
    const val NEW_DAYS = 30

    /** Plays that make a song "often played" for "Haven't heard in a while" when it isn't a favourite. */
    const val OFTEN_PLAYED = 3

    /** An "On repeat" song was heard at least this often in the window. */
    const val REPEAT_PLAYS = 2

    private const val DAY_MS = 86_400_000L

    /** Favourites count as this many plays when ranking songs you haven't heard lately. */
    private const val LIKE_WORTH = 3

    fun build(
        songs: List<Song>,
        played: List<Played>,
        recentListens: List<Listen>,
        liked: Map<String, Long>,
        nowMs: Long,
    ): Result {
        val inLibrary = songs.associateBy { it.key }
        return Result(
            onRepeat = onRepeat(recentListens, inLibrary.keys, nowMs),
            notLately = notLately(played, recentListens, liked, inLibrary.keys, nowMs),
            newlyAdded = newlyAdded(songs, nowMs),
        )
    }

    /**
     * Your most-played songs of the last 30 days. A play is a listen that wasn't skipped or swiped
     * away; a song needs two to count as on repeat. Ties go to the one heard most recently.
     */
    fun onRepeat(listens: List<Listen>, library: Set<String>, nowMs: Long): List<String> {
        val since = nowMs - ON_REPEAT_DAYS * DAY_MS
        val counted = listens.filter {
            it.startedAt in since..nowMs && it.key in library &&
                it.outcome != ListenTracker.SKIPPED && it.outcome != ListenTracker.REMOVED
        }
        val plays = counted.groupingBy { it.key }.eachCount()
        val latest = counted.groupBy { it.key }.mapValues { (_, rows) -> rows.maxOf { it.startedAt } }
        return plays.filterValues { it >= REPEAT_PLAYS }.keys
            .sortedWith(compareByDescending<String> { plays.getValue(it) }.thenByDescending { latest.getValue(it) }.thenBy { it })
            .take(MAX_SONGS)
            .orHidden()
    }

    /**
     * Favourites and often-played songs with nothing heard for 60 days or more. A favourite you have
     * never played counts from the day you liked it. Ranked by plays, a favourite worth three.
     */
    fun notLately(
        played: List<Played>,
        recentListens: List<Listen>,
        liked: Map<String, Long>,
        library: Set<String>,
        nowMs: Long,
    ): List<String> {
        val cutoff = nowMs - NOT_LATELY_DAYS * DAY_MS
        val history = played.associateBy { it.key }
        val heardRecently = recentListens.filter { it.startedAt > cutoff }.map { it.key }.toSet()
        val candidates = (history.keys + liked.keys).filter { it in library && it !in heardRecently }
        fun plays(key: String) = history[key]?.playCount ?: 0
        fun score(key: String) = plays(key) + if (key in liked) LIKE_WORTH else 0
        return candidates
            .filter { key ->
                val lastHeard = history[key]?.lastPlayedAt ?: liked.getValue(key)
                (key in liked || plays(key) >= OFTEN_PLAYED) && lastHeard <= cutoff
            }
            .sortedWith(compareByDescending<String> { score(it) }.thenBy { history[it]?.lastPlayedAt ?: liked[it] ?: 0L }.thenBy { it })
            .take(MAX_SONGS)
            .orHidden()
    }

    /**
     * Files that arrived in the last 30 days, newest first. Empty when every song in the library is
     * that new (a library copied over in one go), since then the shelf would only repeat the library.
     */
    fun newlyAdded(songs: List<Song>, nowMs: Long): List<String> {
        val sinceSec = (nowMs - NEW_DAYS * DAY_MS) / 1000
        val fresh = songs.filter { it.dateAddedSec >= sinceSec && it.dateAddedSec * 1000 <= nowMs }
        if (fresh.size == songs.size) return emptyList()
        return fresh
            .sortedWith(compareByDescending<Song> { it.dateAddedSec }.thenBy { it.key })
            .take(MAX_SONGS)
            .map { it.key }
            .orHidden()
    }

    private fun List<String>.orHidden(): List<String> = if (size >= MIN_SONGS) this else emptyList()
}
