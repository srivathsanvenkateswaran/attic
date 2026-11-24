package app.attic.player.playback.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import app.attic.player.playback.LevellingAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CPU cost on the JVM running the tests (not a phone; ART on a phone core is several times slower, so
 * read the % column as a lower bound and the ratios between rows as the useful part). 60 s of 48 kHz
 * stereo 16-bit noise per run, after a warm-up of the same length.
 */
class EqBenchmarkTest {
    private val rate = 48_000
    private val seconds = 60

    private fun noise(): ByteArray {
        val r = Random(11)
        val b = ByteBuffer.allocate(rate * seconds * 4).order(ByteOrder.nativeOrder())
        while (b.hasRemaining()) b.putShort((r.nextInt(-12_000, 12_000)).toShort())
        return b.array()
    }

    /** Returns nanoseconds per buffer. */
    private fun time(processor: BaseAudioProcessor, data: ByteArray, bufferBytes: Int, beforeBuffer: () -> Unit): Double {
        val input = ByteBuffer.allocateDirect(bufferBytes).order(ByteOrder.nativeOrder())
        val buffers = data.size / bufferBytes
        var sink = 0
        val start = System.nanoTime()
        for (i in 0 until buffers) {
            input.clear()
            input.put(data, i * bufferBytes, bufferBytes)
            input.flip()
            beforeBuffer()
            processor.queueInput(input)
            val out = processor.output
            sink += out.get(out.limit() / 2)
        }
        val elapsed = System.nanoTime() - start
        check(sink != Int.MIN_VALUE)
        return elapsed.toDouble() / buffers
    }

    @Test
    fun costPerBuffer() {
        val data = noise()
        val curve = EqCurve.of(5f, -3f, 4f, -2f, 1.5f, -6f, 3f, -1f, 6f, 2f)
        val other = EqCurve.of(-2f, 4f, -3f, 6f, -1f, 2f, -4f, 5f, -2f, 3f)
        val lines = ArrayList<String>()
        lines += String.format(Locale.ROOT, "%-26s %8s %12s %10s %10s", "case", "buffer", "ns/buffer", "ns/frame", "% of core")
        for (bufferBytes in intArrayOf(4096, 16384)) {
            val frames = bufferBytes / 4
            fun row(name: String, nsPerBuffer: Double) {
                val realTimeNs = frames * 1e9 / rate
                lines += String.format(Locale.ROOT, "%-26s %8d %12.0f %10.2f %10.3f", name, bufferBytes, nsPerBuffer, nsPerBuffer / frames, 100 * nsPerBuffer / realTimeNs)
            }
            // Each case builds a fresh processor and the hook run before every buffer.
            val cases = listOf<Pair<String, () -> Pair<BaseAudioProcessor, () -> Unit>>>(
                "eq flat" to { eq(FakeEqSource("A" to EqCurve.FLAT)) to {} },
                "eq 10-band curve" to { eq(FakeEqSource("A" to curve)) to {} },
                "eq glide every buffer" to {
                    val source = FakeEqSource("A" to curve)
                    var flip = false
                    eq(source) to { flip = !flip; source.set("A", if (flip) other else curve) }
                },
                "levelling -6 dB" to { levelling() to {} },
            )
            for ((name, make) in cases) {
                val (warm, warmHook) = make()
                time(warm, data, bufferBytes, warmHook)
                val (p, hook) = make()
                val ns = time(p, data, bufferBytes, hook)
                row(name, ns)
                assertTrue("$name took $ns ns per buffer", ns < frames * 1e9 / rate * 0.5)
            }
        }
        EqBench.report("bench", lines)
    }

    private fun eq(source: FakeEqSource): BaseAudioProcessor = EqualiserAudioProcessor(source).apply {
        configure(AudioFormat(rate, 2, C.ENCODING_PCM_16BIT))
        flush(item("A"))
    }

    private fun levelling(): BaseAudioProcessor = LevellingAudioProcessor { _, _, _ -> -6f }.apply {
        configure(AudioFormat(rate, 2, C.ENCODING_PCM_16BIT))
        flush(AudioProcessor.StreamMetadata.DEFAULT)
    }
}
