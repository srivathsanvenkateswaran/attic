package app.attic.player.analysis

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import android.system.Os
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Turns one audio file into what radio, genres, volume levelling and smart transitions need: a sound
 * embedding, a melody fingerprint, its top styles, its loudness and its transition points.
 */
class SongAnalyzer(private val context: Context) : Closeable {
    /** [norm] is the embedding's length before it was made unit length ([EffnetModel.Output.norm]). */
    class Result(val embedding: FloatArray, val chroma: ByteArray, val styles: List<StyleScore>, val norm: Float)

    private val model = EffnetModel(
        quietEnvironment(),
        context.assets.open(EffnetModel.ASSET).use { it.readBytes() },
        OrtSession.SessionOptions().apply {
            addXnnpack(mapOf("intra_op_num_threads" to MODEL_THREADS.toString()))
            setIntraOpNumThreads(1)
        },
    )
    private val mel = MelSpectrogram()

    /**
     * The sound embedding, melody fingerprint and top styles (null when the file is too short or can't be
     * decoded), and the file's loudness and transition points, measured in the same decode.
     */
    fun analyze(uri: Uri): Analysis {
        val decoded = decode(uri) ?: return Analysis(null, null, null)
        val patches = mel.patches(decoded.mono)
        val sound = if (patches.isEmpty()) {
            null
        } else {
            val output = model.run(patches)
            Result(output.embedding, Chroma.frames(decoded.mono), output.styles, output.norm)
        }
        return Analysis(sound, decoded.measured.loudness, decoded.measured.transitions, decoded.measured.transitionNanos)
    }

    class Analysis(
        val sound: Result?,
        val loudness: SongLoudness?,
        val transitions: SongTransitions?,
        /** Time spent in [TransitionMeter], for the log. */
        val transitionNanos: Long = 0,
    )

    private class Decoded(val mono: FloatArray, val measured: PcmDecoder.Measured)

    /** Mono 16 kHz audio of the first twelve minutes for the model, and loudness and transitions over the whole file. */
    private fun decode(uri: Uri): Decoded? {
        val out = GrowingFloats()
        val measured = PcmDecoder.measure(context, uri) { mono -> if (out.size < MAX_SAMPLES) out.add(mono) } ?: return null
        return Decoded(out.toArray(), measured)
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
        private const val MAX_SAMPLES = TransitionMeter.SAMPLE_RATE * 60 * 12 // A long mix is judged by its first twelve minutes.

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
