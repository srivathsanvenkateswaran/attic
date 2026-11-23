package app.attic.player.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LevellingAudioProcessorTest {
    private fun processor(gainDb: Float?, encoding: Int = C.ENCODING_PCM_16BIT): LevellingAudioProcessor =
        LevellingAudioProcessor { _, _, _ -> gainDb }.apply {
            configure(AudioFormat(48_000, 2, encoding))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }

    private fun shorts(vararg values: Int): ByteBuffer =
        ByteBuffer.allocateDirect(values.size * 2).order(ByteOrder.nativeOrder()).apply {
            values.forEach { putShort(it.toShort()) }
            flip()
        }

    private fun LevellingAudioProcessor.run(input: ByteBuffer): ShortArray {
        queueInput(input)
        val out = getOutput().asShortBuffer()
        return ShortArray(out.remaining()) { out.get(it) }
    }

    @Test
    fun appliesTheSongsGain() {
        val out = processor(-6.0206f).run(shorts(10_000, -10_000))
        assertEquals(5_000, out[0].toInt())
        assertEquals(-5_000, out[1].toInt())
    }

    @Test
    fun clampsInsteadOfWrappingAround() {
        val out = processor(6f).run(shorts(30_000, -30_000))
        assertEquals(Short.MAX_VALUE, out[0])
        assertEquals(Short.MIN_VALUE, out[1])
    }

    @Test
    fun unmeasuredSongPassesThroughUnchanged() {
        val out = processor(null).run(shorts(1234, -4321))
        assertEquals(1234, out[0].toInt())
        assertEquals(-4321, out[1].toInt())
    }

    @Test
    fun switchingOffRampsBackToUnity() {
        val p = processor(-6.0206f)
        p.enabled = false
        val frames = 4096
        val out = p.run(shorts(*IntArray(frames * 2) { 10_000 }))
        // Starts at the old gain, glides, and ends at full level within the ramp.
        assertEquals(5_000.0, out[0].toDouble(), 10.0)
        assertEquals(10_000, out[out.size - 1].toInt())
    }

    @Test
    fun fixedGainOverridesTheLookupEvenWhenSwitchedOff() {
        val p = LevellingAudioProcessor { _, _, _ -> 0f }
        p.fixedGainDb = -6.0206f
        p.enabled = false
        p.configure(AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        p.flush(AudioProcessor.StreamMetadata.DEFAULT)
        assertEquals(5_000, p.run(shorts(10_000))[0].toInt())
    }

    @Test
    fun floatSamplesAreLevelledAndClamped() {
        val p = processor(6.0206f, C.ENCODING_PCM_FLOAT)
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).apply {
            putFloat(0.25f)
            putFloat(0.75f)
            flip()
        }
        p.queueInput(input)
        val out = p.getOutput().asFloatBuffer()
        assertEquals(0.5f, out.get(0), 1e-4f)
        assertEquals(1f, out.get(1), 0f)
    }

    @Test
    fun otherEncodingsAreLeftAlone() {
        val p = LevellingAudioProcessor { _, _, _ -> -6f }
        p.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_24BIT))
        assertFalse(p.isActive)
    }
}
