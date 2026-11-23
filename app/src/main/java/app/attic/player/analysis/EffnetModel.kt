package app.attic.player.analysis

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Discogs-EffNet on a song's mel patches. One run gives both outputs: `embeddings` (1,280 numbers per
 * patch, the sound radio compares) and `activations` (a 0-to-1 score per Discogs style, per patch).
 * In the graph the scores are one dense layer and a sigmoid on the embedding, so asking for them as well
 * adds almost nothing to the run. No Android here, so the JVM tests run the real model.
 */
class EffnetModel(
    private val environment: OrtEnvironment,
    modelBytes: ByteArray,
    options: OrtSession.SessionOptions = OrtSession.SessionOptions(),
) : Closeable {
    private val session: OrtSession = environment.createSession(modelBytes, options)

    /** [embedding] has length 1. [styles] is empty when the run didn't ask for them. */
    class Output(val embedding: FloatArray, val styles: List<StyleScore>)

    fun run(patches: List<FloatArray>, withStyles: Boolean = true): Output {
        val sum = FloatArray(SongAnalyzer.EMBEDDING_SIZE)
        val styles = StyleAverager()
        val outputs = if (withStyles) setOf(EMBEDDINGS, ACTIVATIONS) else setOf(EMBEDDINGS)
        // One patch per run: batching gave no speed-up on the phone and holds more memory.
        for (patch in patches) {
            val shape = longArrayOf(1, MelSpectrogram.PATCH_FRAMES.toLong(), MelSpectrogram.BANDS.toLong())
            OnnxTensor.createTensor(environment, FloatBuffer.wrap(patch), shape).use { tensor ->
                session.run(mapOf(INPUT to tensor), outputs).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val embeddings = result.get(EMBEDDINGS).get().value as Array<FloatArray>
                    for (row in embeddings) for (i in row.indices) sum[i] += row[i]
                    if (withStyles) {
                        @Suppress("UNCHECKED_CAST")
                        val activations = result.get(ACTIVATIONS).get().value as Array<FloatArray>
                        activations.forEach(styles::add)
                    }
                }
            }
        }
        return Output(normalized(sum), if (withStyles) styles.top() else emptyList())
    }

    private fun normalized(vector: FloatArray): FloatArray {
        var norm = 0f
        for (value in vector) norm += value * value
        norm = sqrt(norm)
        if (norm > 0) for (i in vector.indices) vector[i] /= norm
        return vector
    }

    override fun close() = session.close()

    companion object {
        const val ASSET = "discogs-effnet-bsdynamic-1.onnx"
        private const val INPUT = "melspectrogram"
        private const val EMBEDDINGS = "embeddings"
        private const val ACTIVATIONS = "activations"
    }
}
