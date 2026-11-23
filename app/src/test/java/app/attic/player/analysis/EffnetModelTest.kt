package app.attic.player.analysis

import ai.onnxruntime.OrtEnvironment
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The bundled model, run on the JVM over the tones AudioDspTest checks against Essentia, made a minute
 * long. Checks what the style output adds, in results and in time.
 */
class EffnetModelTest {
    @Test
    fun stylesAreFiveScoresHighestFirst() {
        val styles = model.run(patches).styles
        assertEquals(StyleCodec.TOP, styles.size)
        assertEquals(styles.sortedByDescending { it.score }, styles)
        styles.forEach { assertTrue(it.score in 0f..1f) }
        assertEquals(styles.size, styles.map { it.label }.distinct().size)
    }

    @Test
    fun askingForStylesLeavesTheEmbeddingAlone() {
        val with = model.run(patches, withStyles = true)
        val without = model.run(patches, withStyles = false)
        assertArrayEquals(without.embedding, with.embedding, 0f)
        assertTrue(without.styles.isEmpty())
        assertEquals(1f, sqrt(with.embedding.sumOf { (it * it).toDouble() }).toFloat(), 1e-4f)
    }

    /**
     * Times each way on the same patches, alternating so drift in the machine hits both alike. The
     * style head is one 1,280 by 400 layer behind a network of millions of operations, so the bound
     * is loose: anything near it would mean the run is doing something it shouldn't.
     */
    @Test
    fun stylesAddLittleTime() {
        val withMs = ArrayList<Double>()
        val withoutMs = ArrayList<Double>()
        repeat(ROUNDS) {
            withoutMs += timeMs { model.run(patches, withStyles = false) }
            withMs += timeMs { model.run(patches, withStyles = true) }
        }
        val with = withMs.sorted()[ROUNDS / 2]
        val without = withoutMs.sorted()[ROUNDS / 2]
        println(
            "EffnetModel on ${patches.size} patches (a minute of audio, mel %.1f ms), median of $ROUNDS: embeddings %.1f ms, embeddings and styles %.1f ms (%+.1f%%)"
                .format(melMs, without, with, 100 * (with - without) / without),
        )
        assertTrue("Styles took %.1f ms against %.1f ms".format(with, without), with <= without * 1.15 + 20)
    }

    private inline fun timeMs(block: () -> Unit): Double {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1e6
    }

    companion object {
        private const val ROUNDS = 7
        private lateinit var model: EffnetModel
        private lateinit var patches: List<FloatArray>
        private var melMs = 0.0

        @JvmStatic
        @BeforeClass
        fun load() {
            model = EffnetModel(OrtEnvironment.getEnvironment(), File("src/main/assets/${EffnetModel.ASSET}").readBytes())
            // The tones from AudioDspTest, with a slow swell so the patches differ.
            val signal = FloatArray(16_000 * 60) { i ->
                val t = i / 16_000.0
                val swell = 0.6 + 0.4 * sin(2 * PI * 0.05 * t)
                (swell * (0.3 * sin(2 * PI * 440 * t) + 0.2 * sin(2 * PI * 1234.5 * t) + 0.1 * sin(2 * PI * 90 * t))).toFloat()
            }
            val melStart = System.nanoTime()
            patches = MelSpectrogram().patches(signal)
            melMs = (System.nanoTime() - melStart) / 1e6
            model.run(patches) // Warm up.
        }

        @JvmStatic
        @AfterClass
        fun close() = model.close()
    }
}
