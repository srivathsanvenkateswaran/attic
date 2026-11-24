package app.attic.player.data

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.nio.charset.Charset

/** Lyrics stored inside an audio file's tags. [synced] is LRC text, ready for [LrcParser]. */
data class EmbeddedLyrics(val synced: String?, val plain: String?) {
    companion object {
        val None = EmbeddedLyrics(null, null)
    }
}

/**
 * Picks lyrics out of the metadata Media3 reads from a file.
 *
 * - ID3 (MP3): `SYLT` is synced. `USLT` is synced when its text is an LRC document, plain otherwise.
 *   Media3 leaves both as raw [BinaryFrame]s, so their bodies are decoded here. A `TXXX` frame
 *   described "LYRICS" or "USLT" (what ffmpeg writes) counts like `USLT`.
 * - MP4 (M4A): Media3 turns `©lyr` into a [TextInformationFrame] with the ID `USLT`.
 * - Vorbis comments (FLAC, Ogg, Opus): `LYRICS` or `UNSYNCEDLYRICS`.
 */
@OptIn(UnstableApi::class)
object EmbeddedLyricsReader {
    /** Frame IDs worth decoding; v2.2 used three-letter IDs. */
    val ID3_FRAMES = setOf("USLT", "SYLT", "TXXX", "ULT", "SLT", "TXX")

    private val TEXT_KEYS = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "USLT")

    fun from(entries: List<Metadata.Entry>): EmbeddedLyrics {
        var sylt: String? = null
        val texts = ArrayList<String>()
        for (entry in entries) {
            when (entry) {
                is BinaryFrame -> when (entry.id) {
                    "SYLT", "SLT" -> if (sylt == null) sylt = Id3LyricsFrames.sylt(entry.data)
                    "USLT", "ULT" -> Id3LyricsFrames.uslt(entry.data)?.let(texts::add)
                }
                is TextInformationFrame -> when {
                    entry.id == "USLT" -> texts += entry.values.joinToString("\n")
                    entry.id.startsWith("TXX") && entry.description?.uppercase() in TEXT_KEYS ->
                        texts += entry.values.joinToString("\n")
                }
                is VorbisComment -> if (entry.key.uppercase() in TEXT_KEYS) texts += entry.value
                else -> Unit
            }
        }
        val cleaned = texts.map(::clean).filter { it.isNotEmpty() }
        val synced = sylt?.takeIf { LrcParser.parse(it).isNotEmpty() } ?: cleaned.firstOrNull(LrcParser::isSynced)
        val plain = cleaned.firstOrNull { !LrcParser.isSynced(it) }
        return EmbeddedLyrics(synced, plain)
    }

    /** Old Mac taggers end lines with a bare carriage return. */
    private fun clean(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n').trim('\u0000', ' ', '\n', '\t')
}

/** Decodes the bodies of ID3 `USLT` and `SYLT` frames (ID3v2.4 sections 4.8 and 4.9). */
object Id3LyricsFrames {
    private const val LATIN_1 = 0
    private const val UTF_16 = 1
    private const val UTF_16_BE = 2
    private const val UTF_8 = 3

    /** Timestamps in milliseconds. The other format, MPEG frames, can't be turned into time here. */
    private const val TIMESTAMPS_MS = 2

    /** Encoding, language, description, then the lyrics. */
    fun uslt(data: ByteArray): String? {
        if (data.size < 4) return null
        val encoding = data[0].toInt()
        val descriptionEnd = terminator(data, 4, encoding)
        val start = descriptionEnd + terminatorSize(encoding)
        if (start > data.size) return null
        return decode(data, start, data.size, encoding).trimEnd('\u0000').takeIf { it.isNotBlank() }
    }

    /**
     * Encoding, language, timestamp format, content type, description, then pairs of text and a
     * 4-byte time. Returned as LRC. Most taggers start each line's text with a line break; when any
     * entry has one, entries without one carry on the line before (word-by-word karaoke). Otherwise
     * each entry is a line of its own.
     */
    fun sylt(data: ByteArray): String? {
        if (data.size < 6) return null
        val encoding = data[0].toInt()
        if (data[4].toInt() != TIMESTAMPS_MS) return null
        var at = terminator(data, 6, encoding) + terminatorSize(encoding)
        val entries = ArrayList<Pair<String, Long>>()
        var bigEndian = true
        while (at < data.size) {
            val end = terminator(data, at, encoding)
            if (end + terminatorSize(encoding) + 4 > data.size) break
            var from = at
            if (encoding == UTF_16 && end - at >= 2) {
                val b0 = data[at].toInt() and 0xFF
                val b1 = data[at + 1].toInt() and 0xFF
                if (b0 == 0xFF && b1 == 0xFE) { bigEndian = false; from += 2 }
                if (b0 == 0xFE && b1 == 0xFF) { bigEndian = true; from += 2 }
            }
            val charset = if (encoding == UTF_16) (if (bigEndian) Charsets.UTF_16BE else Charsets.UTF_16LE) else charset(encoding)
            val text = String(data, from, end - from, charset)
            val timeAt = end + terminatorSize(encoding)
            val time = ((data[timeAt].toLong() and 0xFF) shl 24) or ((data[timeAt + 1].toLong() and 0xFF) shl 16) or
                ((data[timeAt + 2].toLong() and 0xFF) shl 8) or (data[timeAt + 3].toLong() and 0xFF)
            entries += text to time
            at = timeAt + 4
        }
        if (entries.isEmpty()) return null
        val breaksMarkLines = entries.any { (text, _) -> text.contains('\n') || text.contains('\r') }
        val lines = ArrayList<Pair<Long, StringBuilder>>()
        for ((text, time) in entries) {
            val startsLine = !breaksMarkLines || lines.isEmpty() || text.startsWith('\n') || text.startsWith('\r')
            val words = text.replace("\r", "").replace("\n", if (breaksMarkLines) "" else " ")
            if (startsLine) lines += time to StringBuilder(words.trimStart()) else lines.last().second.append(words)
        }
        return lines.joinToString("\n") { (time, words) -> stamp(time) + words.toString().trim() }
    }

    fun stamp(ms: Long): String = "[%02d:%02d.%03d]".format(ms / 60_000, ms / 1000 % 60, ms % 1000)

    private fun charset(encoding: Int): Charset = when (encoding) {
        UTF_16 -> Charsets.UTF_16
        UTF_16_BE -> Charsets.UTF_16BE
        UTF_8 -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }

    private fun terminatorSize(encoding: Int) = if (encoding == UTF_16 || encoding == UTF_16_BE) 2 else 1

    /** Index of the string terminator starting the search at [from], or the end of [data]. */
    private fun terminator(data: ByteArray, from: Int, encoding: Int): Int {
        if (terminatorSize(encoding) == 1) {
            for (i in from until data.size) if (data[i].toInt() == 0) return i
            return data.size
        }
        var i = from
        while (i + 1 < data.size) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) return i
            i += 2
        }
        return data.size
    }

    private fun decode(data: ByteArray, from: Int, to: Int, encoding: Int): String =
        if (from >= to) "" else String(data, from, to - from, charset(encoding))
}
