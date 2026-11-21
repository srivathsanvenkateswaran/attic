package app.attic.player.wrapped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class RecapBuilderTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val all = 0L until Long.MAX_VALUE

    private fun at(day: Int, hour: Int, month: Int = 11): Long =
        LocalDateTime.of(2026, month, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun listen(key: String, startedAt: Long, ms: Long = 200_000, outcome: String = "finished", context: String? = "English") =
        Listen(key, startedAt, ms, outcome, context)

    private val songs = mapOf(
        "a" to SongInfo(listOf("ed"), "en"),
        "b" to SongInfo(listOf("ed", "steerner"), "en"),
        "c" to SongInfo(listOf("anirudh"), "ta"),
        "d" to SongInfo(emptyList(), null),
    )

    @Test
    fun ranksSongsByPlaysThenTime() {
        val log = listOf(
            listen("a", at(1, 10)),
            listen("a", at(1, 11)),
            listen("b", at(1, 12), ms = 400_000),
            listen("c", at(1, 13), ms = 500_000),
            listen("c", at(1, 14), ms = 500_000),
        )
        val recap = RecapBuilder.build(log, all, songs, zone)
        assertEquals(listOf("c", "a", "b"), recap.topSongs.map { it.key })
        assertEquals(5, recap.plays)
        assertEquals(3, recap.songCount)
    }

    @Test
    fun shortSkipsAddTimeButNotPlays() {
        val log = listOf(
            listen("a", at(1, 10)),
            listen("b", at(1, 11), ms = 5_000, outcome = "skipped"),
        )
        val recap = RecapBuilder.build(log, all, songs, zone)
        assertEquals(1, recap.plays)
        assertEquals(205_000, recap.listenedMs)
        assertEquals(listOf("a"), recap.topSongs.map { it.key })
    }

    @Test
    fun creditsEveryArtistOnASong() {
        val log = listOf(listen("b", at(1, 10)), listen("a", at(1, 11)))
        val recap = RecapBuilder.build(log, all, songs, zone)
        assertEquals("ed", recap.topArtists.first().key)
        assertEquals(2, recap.topArtists.first().plays)
        assertTrue(recap.topArtists.any { it.key == "steerner" })
    }

    @Test
    fun languageSharesLeaveOutUnknownSongs() {
        val log = listOf(
            listen("a", at(1, 10), ms = 300_000),
            listen("c", at(1, 11), ms = 100_000),
            listen("d", at(1, 12), ms = 900_000),
        )
        val recap = RecapBuilder.build(log, all, songs, zone)
        assertEquals(listOf("en", "ta"), recap.languages.map { it.language })
        assertEquals(0.75f, recap.languages.first().share, 0.001f)
    }

    @Test
    fun periodOnlyCountsListensInside() {
        val log = listOf(listen("a", at(20, 10, month = 10)), listen("b", at(3, 10)))
        val range = RecapPeriod.Month.range(at(5, 9), zone)
        val recap = RecapBuilder.build(log, range, songs, zone)
        assertEquals(listOf("b"), recap.topSongs.map { it.key })
    }

    @Test
    fun weekStartsOnMonday() {
        // 11 November 2026 is a Wednesday.
        val range = RecapPeriod.Week.range(at(11, 15), zone)
        assertEquals(at(9, 0), range.first)
    }

    @Test
    fun discoveriesAreFirstPlaysInThePeriod() {
        val log = listOf(
            listen("a", at(20, 10, month = 10)),
            listen("a", at(2, 10)),
            listen("b", at(3, 10)),
            listen("c", at(4, 10)),
        )
        val range = RecapPeriod.Month.range(at(5, 9), zone)
        assertEquals(2, RecapBuilder.build(log, range, songs, zone).discoveries)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val days = listOf(1, 2, 3, 5, 6).map { LocalDate.of(2026, 11, it) }
        assertEquals(3, RecapBuilder.longestStreak(days))
    }

    @Test
    fun onRepeatNeedsThreePlaysInADay() {
        val twice = listOf(listen("a", at(1, 10)), listen("a", at(1, 11)))
        assertNull(RecapBuilder.build(twice, all, songs, zone).onRepeat)
        val thrice = twice + listen("a", at(1, 12))
        assertEquals(3, RecapBuilder.build(thrice, all, songs, zone).onRepeat?.plays)
    }

    @Test
    fun radioShareCountsRadioContexts() {
        val log = listOf(listen("a", at(1, 10), context = "Radio · Celestial"), listen("b", at(1, 11)))
        assertEquals(0.5f, RecapBuilder.build(log, all, songs, zone).radioShare, 0.001f)
    }

    @Test
    fun lateNightListeningIsANightOwl() {
        val log = listOf(listen("a", at(1, 23)), listen("b", at(2, 0)), listen("c", at(2, 1)), listen("a", at(2, 14), ms = 40_000))
        assertEquals(ListenerType.NightOwl, RecapBuilder.build(log, all, songs, zone).listenerType)
    }
}
