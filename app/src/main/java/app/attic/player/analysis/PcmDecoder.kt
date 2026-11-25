package app.attic.player.analysis

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

/** Decodes a file's first audio track with the phone's own codec, for analysis and loudness. */
object PcmDecoder {
    private const val CODEC_TIMEOUT_US = 10_000L

    /** Stops after this much audio, so a DJ mix of several hours can't hold analysis up. */
    private const val MAX_SECONDS = 45 * 60L

    /**
     * Hands [onSamples] the decoded audio as interleaved floats in -1..1, after [onFormat] has given
     * its sample rate and channel count. [onSamples] returns false to stop early. False when the file
     * couldn't be decoded at all.
     */
    fun decode(
        context: Context,
        uri: Uri,
        onFormat: (sampleRate: Int, channels: Int) -> Unit,
        onSamples: (FloatArray) -> Boolean,
    ): Boolean {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            drain(extractor, codec, onFormat, onSamples)
            return true
        } catch (_: Exception) {
            return false
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun drain(
        extractor: MediaExtractor,
        codec: MediaCodec,
        onFormat: (Int, Int) -> Unit,
        onSamples: (FloatArray) -> Boolean,
    ) {
        val info = MediaCodec.BufferInfo()
        var channels = 0
        var frameLimit = Long.MAX_VALUE
        var frames = 0L
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        var inputDone = false
        while (frames < frameLimit) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(0)
                if (index >= 0) {
                    val read = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                    if (read < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, read, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val index = codec.dequeueOutputBuffer(info, if (inputDone) CODEC_TIMEOUT_US else 0)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    frameLimit = rate * MAX_SECONDS
                    onFormat(rate, channels)
                }
                index >= 0 -> {
                    var keepGoing = true
                    if (info.size > 0 && channels > 0) {
                        val buffer = codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val samples = PcmBuffer.samples(buffer, encoding) ?: throw IllegalStateException("Unknown PCM encoding $encoding")
                        frames += samples.size / channels
                        keepGoing = onSamples(samples)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (!keepGoing || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /** What one decode measures besides the model's input. [transitionNanos] is the time [TransitionMeter] took. */
    class Measured(val loudness: SongLoudness?, val transitions: SongTransitions?, val transitionNanos: Long)

    /**
     * Loudness at the file's own rate and channels, and the transition points from a 16 kHz mono copy,
     * which [mono] also receives (the model's input). Null when the file can't be decoded. Without a
     * [mono] consumer this is the quick decode for a song analysed before Attic measured these.
     */
    fun measure(context: Context, uri: Uri, mono: ((FloatArray) -> Unit)? = null): Measured? {
        var loudness: LoudnessMeter? = null
        var resampler: LinearResampler? = null
        val transitions = TransitionMeter()
        var channels = 1
        var transitionNanos = 0L
        val decoded = decode(
            context,
            uri,
            onFormat = { rate, count ->
                channels = count
                loudness = LoudnessMeter(rate, count)
                resampler = LinearResampler(rate, TransitionMeter.SAMPLE_RATE)
            },
            onSamples = { samples ->
                loudness?.add(samples)
                val resampled = resampler!!.push(PcmBuffer.toMono(samples, channels))
                val started = System.nanoTime()
                transitions.add(resampled)
                transitionNanos += System.nanoTime() - started
                mono?.invoke(resampled)
                true
            },
        )
        if (!decoded) return null
        val started = System.nanoTime()
        val result = transitions.result()
        transitionNanos += System.nanoTime() - started
        return Measured(loudness?.result(), result, transitionNanos)
    }
}
