package app.attic.player.analysis

import ai.onnxruntime.OnnxTensor
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
import java.nio.FloatBuffer
import kotlin.math.sqrt

/** Turns one audio file into what radio needs: a sound embedding and a melody fingerprint. */
class SongAnalyzer(private val context: Context) : Closeable {
    class Result(val embedding: FloatArray, val chroma: ByteArray)

    private val environment = quietEnvironment()
    private val session: OrtSession = environment.createSession(
        context.assets.open(MODEL_ASSET).use { it.readBytes() },
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
        return Result(embed(patches), Chroma.frames(pcm))
    }

    private fun embed(patches: List<FloatArray>): FloatArray {
        val sum = FloatArray(EMBEDDING_SIZE)
        patches.chunked(1).forEach { batch ->
            val values = FloatBuffer.allocate(batch.size * batch[0].size)
            batch.forEach(values::put)
            values.rewind()
            val shape = longArrayOf(batch.size.toLong(), MelSpectrogram.PATCH_FRAMES.toLong(), MelSpectrogram.BANDS.toLong())
            OnnxTensor.createTensor(environment, values, shape).use { tensor ->
                session.run(mapOf(MODEL_INPUT to tensor), setOf(MODEL_OUTPUT)).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val rows = result[0].value as Array<FloatArray>
                    for (row in rows) for (i in row.indices) sum[i] += row[i]
                }
            }
        }
        return normalized(sum)
    }

    private fun normalized(vector: FloatArray): FloatArray {
        var norm = 0f
        for (value in vector) norm += value * value
        norm = sqrt(norm)
        if (norm > 0) for (i in vector.indices) vector[i] /= norm
        return vector
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

    override fun close() = session.close()

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

        private const val MODEL_ASSET = "discogs-effnet-bsdynamic-1.onnx"
        private const val MODEL_INPUT = "melspectrogram"
        private const val MODEL_OUTPUT = "embeddings"
        private const val MODEL_THREADS = 2
        private const val EMBEDDING_SIZE = 1280
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
