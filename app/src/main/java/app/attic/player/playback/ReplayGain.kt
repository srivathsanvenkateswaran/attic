package app.attic.player.playback

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.extractor.mp3.Mp3InfoReplayGain
import kotlin.math.log10

/**
 * Gains a file's tags ask for, in dB against ReplayGain 2's reference of -18 LUFS. Peaks are linear,
 * 1.0 at full scale, and null when the tags don't give one.
 */
data class TagGain(
    val trackGainDb: Float?,
    val trackPeak: Float?,
    val albumGainDb: Float?,
    val albumPeak: Float?,
) {
    val isEmpty: Boolean get() = trackGainDb == null && albumGainDb == null
}

/**
 * Reads volume-levelling tags from the names and values a file carries. Taggers write the same thing
 * several ways; in order of trust:
 *
 * - ReplayGain: ID3 `TXXX:REPLAYGAIN_TRACK_GAIN`, the MP4 atom `----:com.apple.iTunes:replaygain_track_gain`,
 *   or the Vorbis comment `REPLAYGAIN_TRACK_GAIN`, with `_PEAK` and `ALBUM_` companions.
 * - `R128_TRACK_GAIN` / `R128_ALBUM_GAIN` in Opus files: Q7.8 dB against -23 LUFS.
 * - iTunes Sound Check, `iTunNORM`: ten hex words, the first two the loudness of each channel.
 * - The ReplayGain field LAME writes into an MP3's Info header (see [fromMetadata]).
 */
object ReplayGainTags {
    /** [fields] are tag name to value; names are matched without regard to case. Null when nothing usable is there. */
    fun parse(fields: List<Pair<String, String>>): TagGain? {
        val byName = HashMap<String, String>()
        for ((name, value) in fields) byName.putIfAbsent(name.trim().lowercase(), value)

        val replayGain = TagGain(
            trackGainDb = byName["replaygain_track_gain"]?.let(::decibels),
            trackPeak = byName["replaygain_track_peak"]?.let(::peak),
            albumGainDb = byName["replaygain_album_gain"]?.let(::decibels),
            albumPeak = byName["replaygain_album_peak"]?.let(::peak),
        )
        if (!replayGain.isEmpty) return replayGain

        val r128 = TagGain(
            trackGainDb = byName["r128_track_gain"]?.let(::r128),
            trackPeak = null,
            albumGainDb = byName["r128_album_gain"]?.let(::r128),
            albumPeak = null,
        )
        if (!r128.isEmpty) return r128

        return byName["itunnorm"]?.let(::soundCheck)
    }

    /** "-6.48 dB", "+1.2dB", "-6,48 dB". */
    internal fun decibels(value: String): Float? =
        NUMBER.find(value)?.value?.replace(',', '.')?.toFloatOrNull()?.takeIf { it.isFinite() && it in -60f..60f }

    private fun peak(value: String): Float? =
        NUMBER.find(value)?.value?.replace(',', '.')?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f && it < 100f }

    /** Opus output gain is Q7.8 against -23 LUFS; ReplayGain's reference is 5 dB louder. */
    private fun r128(value: String): Float? = value.trim().toIntOrNull()?.let { it / 256f + R128_TO_REPLAYGAIN_DB }

    /**
     * Sound Check: the first two words are each channel's level in thousandths, so the gain is
     * -10·log10(max/1000); words seven and eight are the channel peaks out of 32768. Apple's reference
     * is close enough to ReplayGain's that players treat the two alike.
     */
    internal fun soundCheck(value: String): TagGain? {
        val words = value.trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull(16) }
        if (words.size < 10) return null
        val level = maxOf(words[0], words[1])
        if (level <= 0) return null
        val gain = (-10 * log10(level / 1000.0)).toFloat().takeIf { it in -60f..60f } ?: return null
        val peak = maxOf(words[6], words[7]).takeIf { it > 0 }?.let { it / 32768f }
        return TagGain(gain, peak, null, null)
    }

    /**
     * The tag fields in a Media3 [Metadata], as the extractors hand them over: ID3 TXXX and COMM frames,
     * MP4 freeform atoms (which arrive as [InternalFrame]) and Vorbis comments. Falls back to LAME's
     * Info-header gain when no tag says anything.
     */
    @OptIn(UnstableApi::class)
    fun fromMetadata(metadata: Metadata?): TagGain? {
        if (metadata == null) return null
        val fields = ArrayList<Pair<String, String>>()
        var lame: Mp3InfoReplayGain? = null
        for (i in 0 until metadata.length()) {
            when (val entry = metadata[i]) {
                is TextInformationFrame -> if (entry.id == "TXXX" && entry.description != null) {
                    entry.values.firstOrNull()?.let { fields += entry.description!! to it }
                }
                is CommentFrame -> fields += entry.description to entry.text
                is InternalFrame -> fields += entry.description to entry.text
                is VorbisComment -> fields += entry.key to entry.value
                is Mp3InfoReplayGain -> lame = entry
            }
        }
        return parse(fields) ?: lame?.let(::fromLame)
    }

    /** LAME writes a "radio" (track) gain and the peak; its reference sits close to ReplayGain's. */
    @OptIn(UnstableApi::class)
    private fun fromLame(info: Mp3InfoReplayGain): TagGain? {
        val track = listOfNotNull(info.field1, info.field2).firstOrNull { it.name == Mp3InfoReplayGain.GainField.NAME_RADIO }
            ?: return null
        val album = listOfNotNull(info.field1, info.field2).firstOrNull { it.name == Mp3InfoReplayGain.GainField.NAME_AUDIOPHILE }
        return TagGain(track.gain, info.peak.takeIf { it > 0f }, album?.gain, null)
    }

    private val NUMBER = Regex("[-+]?\\d+(?:[.,]\\d+)?")
    private const val R128_TO_REPLAYGAIN_DB = 5f
}
