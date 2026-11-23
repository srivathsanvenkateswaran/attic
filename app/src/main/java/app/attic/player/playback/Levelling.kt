package app.attic.player.playback

import kotlin.math.log10
import kotlin.math.pow

/** A measured song: integrated loudness in LUFS and sample peak, 1.0 at full scale. */
data class MeasuredLevel(val lufs: Double, val peak: Double?)

/** Where a song sits for levelling: its album and number, so an album played in order is levelled as one. */
data class LevelTrack(val key: String, val albumKey: String?, val trackNumber: Int?)

/**
 * The arithmetic of volume levelling, free of the player so it can be tested. Every song is brought
 * to [TARGET_LUFS], about where streaming services play, but never so far up that its loudest sample
 * would pass [PEAK_CEILING_DB]: a quiet song with a loud peak stays a little quiet rather than clip.
 */
object Levelling {
    const val TARGET_LUFS = -14f

    /** ReplayGain 2 and the tags written to it aim at -18 LUFS. */
    const val REPLAYGAIN_REFERENCE_LUFS = -18f
    const val MAX_BOOST_DB = 12f
    const val MAX_CUT_DB = 24f

    /** Peaks stay 1 dB under full scale, which leaves room for the phone's own resampling. */
    const val PEAK_CEILING_DB = -1f

    /** Gain for a song measured at [lufs], kept under the ceiling for [peak]. */
    fun forLoudness(lufs: Double, peak: Double?): Float = clamp((TARGET_LUFS - lufs).toFloat(), peak?.toFloat())

    /** Gain for a ReplayGain value, which is relative to -18 LUFS, moved to Attic's target. */
    fun forReplayGain(gainDb: Float, peak: Float?): Float = clamp(gainDb + (TARGET_LUFS - REPLAYGAIN_REFERENCE_LUFS), peak)

    /**
     * Holds a gain inside [-MAX_CUT_DB, MAX_BOOST_DB], then lowers it until [peak] would land at
     * [PEAK_CEILING_DB]. With no peak known, only cuts are allowed.
     */
    fun clamp(gainDb: Float, peak: Float?): Float {
        val ranged = gainDb.coerceIn(-MAX_CUT_DB, MAX_BOOST_DB)
        val headroom = if (peak != null && peak > 0f) PEAK_CEILING_DB - 20 * log10(peak) else 0f
        return minOf(ranged, headroom)
    }

    fun linear(gainDb: Float): Float = 10f.pow(gainDb / 20f)

    /**
     * One loudness for an album, from its songs' loudness weighted by length: the level of the album
     * played through, so the songs keep the balance the album was mastered with.
     */
    fun albumLevel(songs: List<Pair<MeasuredLevel, Long>>): MeasuredLevel? {
        val total = songs.sumOf { it.second.coerceAtLeast(1) }
        if (songs.isEmpty() || total <= 0) return null
        val power = songs.sumOf { (level, ms) -> 10.0.pow(level.lufs / 10) * ms.coerceAtLeast(1) } / total
        val peaks = songs.map { it.first.peak }
        return MeasuredLevel(10 * log10(power), if (peaks.any { it == null }) null else peaks.maxOf { it!! })
    }

    /**
     * The gain for [current], or null to leave it at unity. Tags win over measurements, as they are
     * what the file's owner chose. When the song runs on from or into the next one on its album, the
     * album's gain is used, so the songs keep their balance and no level step lands between them.
     */
    fun gainDb(
        previous: LevelTrack?,
        current: LevelTrack,
        next: LevelTrack?,
        tags: TagGain?,
        measured: (String) -> MeasuredLevel?,
        albumMeasured: (String) -> MeasuredLevel?,
    ): Float? {
        val inAlbum = AlbumRun.follows(previous, current) || AlbumRun.follows(current, next)
        if (tags != null) {
            if (inAlbum && tags.albumGainDb != null) return forReplayGain(tags.albumGainDb, tags.albumPeak ?: tags.trackPeak)
            if (tags.trackGainDb != null) return forReplayGain(tags.trackGainDb, tags.trackPeak)
        }
        if (inAlbum && current.albumKey != null) {
            albumMeasured(current.albumKey)?.let { return forLoudness(it.lufs, it.peak) }
        }
        return measured(current.key)?.let { forLoudness(it.lufs, it.peak) }
    }
}

/** Whether two songs are an album played in order, which keeps them gapless and levels them as one. */
object AlbumRun {
    fun follows(before: LevelTrack?, after: LevelTrack?): Boolean {
        if (before == null || after == null) return false
        val album = before.albumKey ?: return false
        val number = before.trackNumber ?: return false
        return album == after.albumKey && after.trackNumber == number + 1
    }
}
