package app.attic.player.analysis

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.system.Os
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Turns one audio file into what radio and genres need: a sound embedding, a melody fingerprint and its top styles. */
class SongAnalyzer(private val context: Context) : Closeable {
    class Result(val embedding: FloatArray, val chroma: ByteArray, val styles: List<StyleScore>)

    private val model = EffnetModel(
        quietEnvironment(),
        context.assets.open(EffnetModel.ASSET).use { it.readBytes() },
        OrtSession.SessionOptions().apply {
            addXnnpack(mapOf("intra_op_num_threads" to MODEL_THREADS.toString()))
            setIntraOpNumThreads(1)
        },
    )
    private val mel = MelSpectrogram()

    /** Null when the file is too short or can't be decoded. */
    fun analyze(uri: Uri): Result? {
        val pcm = decode(uri) ?: return null
        val patches = mel.patches(pcm)
        if (patches.isEmpty()) return null
        val output = model.run(patches)
        return Result(output.embedding, Chroma.frames(pcm), output.styles)
    }

    private fun decode(uri: Uri): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            return drain(extractor, codec)
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun drain(extractor: MediaExtractor, codec: MediaCodec): FloatArray {
        val out = GrowingFloats()
        val info = MediaCodec.BufferInfo()
        var resampler: LinearResampler? = null
        var channels = 1
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        var inputDone = false
        var outputDone = false
        while (!outputDone && out.size < MAX_SAMPLES) {
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
                    resampler = LinearResampler(format.getInteger(MediaFormat.KEY_SAMPLE_RATE), TARGET_RATE)
                    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                }
                index >= 0 -> {
                    if (info.size > 0 && resampler != null) {
                        val buffer = codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val samples = PcmBuffer.samples(buffer, encoding) ?: throw IllegalStateException("Unknown PCM encoding $encoding")
                        out.add(resampler.push(PcmBuffer.toMono(samples, channels)))
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        return out.toArray()
    }

    override fun close() = model.close()

    private class GrowingFloats {
        private var values = FloatArray(16_000 * 60 * 5)
        var size = 0
            private set

        fun add(more: FloatArray) {
            if (size + more.size > values.size) values = values.copyOf(maxOf(values.size * 2, size + more.size))
            more.copyInto(values, size)
            size += more.size
        }

        fun toArray(): FloatArray = values.copyOf(size)
    }

    companion object {
        /**
         * ONNX Runtime's Android build sends usage events, with the phone's device ID, to Microsoft unless
         * told not to (Privacy.md in its repository). Analysis promises that nothing leaves the phone, so
         * this switches that off before the runtime first loads. The manifest also removes the runtime's
         * uploader, so nothing could send the events anyway.
         */
        private fun quietEnvironment(): OrtEnvironment {
            Os.setenv("ORT_DISABLE_TELEMETRY", "1", true)
            return OrtEnvironment.getEnvironment()
        }

        /** Raise this if the model or its input changes, so every song gets analysed again. */
        const val MODEL_VERSION = 1

        private const val MODEL_THREADS = 2
        const val EMBEDDING_SIZE = 1280

        /** How many Discogs styles the model scores: the length of its `activations` output. */
        const val STYLE_COUNT = 400
        private const val TARGET_RATE = 16_000
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val MAX_SAMPLES = TARGET_RATE * 60 * 12 // A long mix is judged by its first twelve minutes.

        fun toBytes(vector: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            vector.forEach(buffer::putFloat)
            return buffer.array()
        }

        fun toFloats(bytes: ByteArray): FloatArray {
            val floats = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            return FloatArray(floats.remaining()) { floats.get(it) }
        }
    }
}
