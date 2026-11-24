package app.attic.player.data

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.metadata.id3.Id3Decoder
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.text.SubtitleParser
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Reads the tags of an audio file with the extractors Media3 already ships for playback, without
 * decoding any audio. Only the bytes the tags need are read: the audio between them is skipped.
 */
@OptIn(UnstableApi::class)
object TagReader {
    /** A tag this big is almost all cover art; reading it to find lyrics isn't worth the memory. */
    private const val MAX_ID3_BYTES = 16 shl 20

    /** Enough extractor steps to reach the tags of any sane file, and a stop for a broken one. */
    private const val MAX_STEPS = 10_000

    private val MP4_EXTENSIONS = setOf("m4a", "mp4", "m4b", "m4p", "aac", "alac", "3gp")
    private val FLAC_EXTENSIONS = setOf("flac")
    private val OGG_EXTENSIONS = setOf("ogg", "oga", "opus")

    fun metadata(channel: FileChannel, displayName: String): List<Metadata.Entry> {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return when {
            extension in MP4_EXTENSIONS ->
                extract(Mp4Extractor(SubtitleParser.Factory.UNSUPPORTED, Mp4Extractor.FLAG_OMIT_TRACK_SAMPLE_TABLE or Mp4Extractor.FLAG_DISABLE_ARTWORK_METADATA), channel)
            extension in FLAC_EXTENSIONS -> extract(FlacExtractor(), channel)
            extension in OGG_EXTENSIONS -> extract(OggExtractor(), channel)
            else -> id3(channel)
        }
    }

    /** The ID3v2 tag at the start of the file, decoding only the frames that can hold lyrics. */
    private fun id3(channel: FileChannel): List<Metadata.Entry> {
        val header = ByteArray(Id3Decoder.ID3_HEADER_LENGTH)
        if (readAt(channel, 0, header, header.size) < header.size) return emptyList()
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return emptyList()
        val size = (header[6].toInt() and 0x7F shl 21) or (header[7].toInt() and 0x7F shl 14) or
            (header[8].toInt() and 0x7F shl 7) or (header[9].toInt() and 0x7F)
        val hasFooter = header[5].toInt() and 0x10 != 0
        val total = Id3Decoder.ID3_HEADER_LENGTH + size + if (hasFooter) Id3Decoder.ID3_HEADER_LENGTH else 0
        if (total > MAX_ID3_BYTES) return emptyList()
        val tag = ByteArray(total)
        val read = readAt(channel, 0, tag, total)
        val predicate = Id3Decoder.FramePredicate { _, id0, id1, id2, id3 ->
            val id = buildString { for (c in intArrayOf(id0, id1, id2, id3)) if (c != 0) append(c.toChar()) }
            id in EmbeddedLyricsReader.ID3_FRAMES
        }
        val metadata = Id3Decoder(predicate).decode(tag, read) ?: return emptyList()
        return List(metadata.length()) { metadata[it] }
    }

    /** Runs [extractor] until every track it found has a format, then returns the formats' metadata. */
    private fun extract(extractor: Extractor, channel: FileChannel): List<Metadata.Entry> {
        val output = FormatCollector()
        val length = channel.size()
        try {
            if (!extractor.sniff(DefaultExtractorInput(ChannelReader(channel, 0), 0, length))) return emptyList()
            extractor.init(output)
            val seek = PositionHolder()
            var position = 0L
            var steps = 0
            while (!output.done && steps < MAX_STEPS) {
                val input = DefaultExtractorInput(ChannelReader(channel, position), position, length)
                var result = Extractor.RESULT_CONTINUE
                while (result == Extractor.RESULT_CONTINUE && !output.done && steps++ < MAX_STEPS) {
                    result = extractor.read(input, seek)
                }
                when (result) {
                    Extractor.RESULT_SEEK -> position = seek.position
                    Extractor.RESULT_END_OF_INPUT -> break
                }
            }
        } finally {
            extractor.release()
        }
        return output.entries()
    }

    private fun readAt(channel: FileChannel, position: Long, target: ByteArray, length: Int): Int {
        val buffer = ByteBuffer.wrap(target, 0, length)
        var at = position
        while (buffer.hasRemaining()) {
            val n = channel.read(buffer, at)
            if (n < 0) break
            at += n
        }
        return buffer.position()
    }

    /** Positional reads, so two inputs over one channel can't disturb each other. */
    private class ChannelReader(private val channel: FileChannel, private var position: Long) : DataReader {
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            val n = channel.read(ByteBuffer.wrap(buffer, offset, length), position)
            if (n < 0) return C.RESULT_END_OF_INPUT
            position += n
            return n
        }
    }

    private class FormatCollector : ExtractorOutput {
        private val tracks = ArrayList<FormatTrack>()
        private var ended = false

        val done: Boolean get() = ended && tracks.all { it.format != null }

        fun entries(): List<Metadata.Entry> = tracks.flatMap { track ->
            val metadata = track.format?.metadata ?: return@flatMap emptyList()
            List(metadata.length()) { metadata[it] }
        }

        override fun track(id: Int, type: Int): TrackOutput = FormatTrack().also { tracks += it }

        override fun endTracks() {
            ended = true
        }

        override fun seekMap(seekMap: SeekMap) = Unit
    }

    /** Keeps the format and throws the audio away. */
    private class FormatTrack : TrackOutput {
        var format: Format? = null
        private val scratch = ByteArray(4096)

        override fun format(format: Format) {
            this.format = format
        }

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
            val n = input.read(scratch, 0, minOf(length, scratch.size))
            if (n == C.RESULT_END_OF_INPUT) {
                if (allowEndOfInput) return C.RESULT_END_OF_INPUT
                throw IOException("File ended inside a sample")
            }
            return n
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
            data.skipBytes(length)
        }

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) = Unit
    }
}
