package app.attic.player.analysis

import android.media.AudioFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** The expected numbers come from Essentia, the library EffNet was trained against. */
class AudioDspTest {
    private val signal = FloatArray(32_000) { i ->
        val t = i / 16_000.0
        (0.3 * sin(2 * PI * 440 * t) + 0.2 * sin(2 * PI * 1234.5 * t) + 0.1 * sin(2 * PI * 90 * t)).toFloat()
    }

    private fun resource(name: String): List<FloatArray> =
        javaClass.getResourceAsStream("/analysis/$name")!!.bufferedReader().readLines()
            .map { line -> line.trim().split(' ').map(String::toFloat).toFloatArray() }

    @Test
    fun melFramesMatchEssentia() {
        val expected = resource("mel_expected.txt")
        val mel = MelSpectrogram()
        listOf(0, 1, 50, 123).forEachIndexed { row, frame ->
            assertArrayEquals(expected[row], mel.frame(signal, frame * 256), 0.002f)
        }
    }

    @Test
    fun patchesAreEvery248FramesAnd128FramesLong() {
        val patches = MelSpectrogram().patches(FloatArray(16_000 * 10))
        // 10 s is 624 usable frames, so patches start at frames 0, 248 and 496.
        assertEquals(3, patches.size)
        assertEquals(128 * 96, patches[0].size)
    }

    @Test
    fun audioShorterThanOnePatchGivesNoPatches() {
        assertEquals(0, MelSpectrogram().patches(FloatArray(1000)).size)
    }

    @Test
    fun chromaMatchesThePrototype() {
        val expected = resource("chroma_expected.txt")
        val chroma = Chroma.frames(signal)
        assertEquals(expected.size, chroma.size / 12)
        expected.forEachIndexed { row, values ->
            assertArrayEquals(values, FloatArray(12) { (chroma[row * 12 + it].toInt() and 0xFF) / 255f }, 0.006f)
        }
    }

    @Test
    fun resamplingKeepsThePitchOfATone() {
        val input = FloatArray(44_100) { sin(2 * PI * 440 * it / 44_100.0).toFloat() }
        val resampled = LinearResampler(44_100, 16_000).let { it.push(input) }
        assertEquals(16_000f, resampled.size.toFloat(), 2f)
        val zeroCrossings = (1 until resampled.size).count { (resampled[it - 1] < 0) != (resampled[it] < 0) }
        assertEquals(880f, zeroCrossings.toFloat(), 4f)
    }

    @Test
    fun resamplingAcrossBuffersMatchesOneBuffer() {
        val input = FloatArray(4_410) { sin(2 * PI * 300 * it / 44_100.0).toFloat() }
        val whole = LinearResampler(44_100, 16_000).push(input)
        val split = LinearResampler(44_100, 16_000).let { r ->
            r.push(input.copyOfRange(0, 1_000)) + r.push(input.copyOfRange(1_000, input.size))
        }
        assertArrayEquals(whole, split, 1e-5f)
    }

    @Test
    fun sixteenBitSamplesAreRead() {
        val bytes = ByteBuffer.wrap(byteArrayOf(0, 0x40, 0, 0xC0.toByte())).order(ByteOrder.LITTLE_ENDIAN)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), PcmBuffer.samples(bytes, AudioFormat.ENCODING_PCM_16BIT), 1e-4f)
    }

    @Test
    fun twentyFourBitSamplesAreRead() {
        val bytes = ByteBuffer.wrap(byteArrayOf(0, 0, 0x40, 0, 0, 0xC0.toByte())).order(ByteOrder.LITTLE_ENDIAN)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), PcmBuffer.samples(bytes, AudioFormat.ENCODING_PCM_24BIT_PACKED), 1e-4f)
    }

    @Test
    fun thirtyTwoBitSamplesAreRead() {
        val bytes = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0x40, 0, 0, 0, 0xC0.toByte())).order(ByteOrder.LITTLE_ENDIAN)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), PcmBuffer.samples(bytes, AudioFormat.ENCODING_PCM_32BIT), 1e-4f)
    }

    @Test
    fun floatSamplesAreRead() {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(0.25f).putFloat(-1f)
        bytes.rewind()
        assertArrayEquals(floatArrayOf(0.25f, -1f), PcmBuffer.samples(bytes, AudioFormat.ENCODING_PCM_FLOAT), 1e-6f)
    }

    @Test
    fun anUnreadableSampleFormatIsRefusedInsteadOfMisread() {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(null, PcmBuffer.samples(bytes, 9999))
    }

    @Test
    fun stereoIsAveragedToMono() {
        assertArrayEquals(floatArrayOf(0.5f, -0.25f), PcmBuffer.toMono(floatArrayOf(1f, 0f, -0.5f, 0f), 2), 1e-6f)
    }
}
