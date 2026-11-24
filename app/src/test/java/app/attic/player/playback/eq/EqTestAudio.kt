package app.attic.player.playback.eq

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** An [EqSource] keyed by period uid, with a version tests bump by hand. */
internal class FakeEqSource(vararg curves: Pair<Any, EqCurve?>) : EqSource {
    val curves = hashMapOf(*curves)
    var calls = 0

    @Volatile
    override var version: Long = 0

    override fun curveFor(timeline: Timeline, periodUid: Any?): EqCurve? {
        calls++
        return curves[periodUid]
    }

    fun set(periodUid: Any, curve: EqCurve?) {
        curves[periodUid] = curve
        version++
    }
}

internal fun item(periodUid: Any): AudioProcessor.StreamMetadata =
    AudioProcessor.StreamMetadata.Builder().setTimeline(Timeline.EMPTY).setPeriodUid(periodUid).build()

internal fun EqualiserAudioProcessor.start(rate: Int, channels: Int, encoding: Int, periodUid: Any): EqualiserAudioProcessor {
    configure(AudioFormat(rate, channels, encoding))
    flush(item(periodUid))
    return this
}

/** Feeds [samples] (interleaved, as shorts) in buffers of [bufferBytes] and returns everything that came out. */
internal fun EqualiserAudioProcessor.runShorts(samples: ShortArray, bufferBytes: Int = 4096, beforeBuffer: (Int) -> Unit = {}): ShortArray {
    val out = ShortArray(samples.size)
    var at = 0
    var index = 0
    while (at < samples.size) {
        val n = minOf(bufferBytes / 2, samples.size - at)
        beforeBuffer(index++)
        val input = ByteBuffer.allocateDirect(n * 2).order(ByteOrder.nativeOrder())
        input.asShortBuffer().put(samples, at, n)
        queueInput(input)
        check(!input.hasRemaining())
        val o = getOutput().order(ByteOrder.nativeOrder()).asShortBuffer()
        o.get(out, at, o.remaining())
        at += n
    }
    return out
}

internal fun EqualiserAudioProcessor.runFloats(samples: FloatArray, bufferBytes: Int = 4096, beforeBuffer: (Int) -> Unit = {}): FloatArray {
    val out = FloatArray(samples.size)
    var at = 0
    var index = 0
    while (at < samples.size) {
        val n = minOf(bufferBytes / 4, samples.size - at)
        beforeBuffer(index++)
        val input = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(samples, at, n)
        queueInput(input)
        val o = getOutput().order(ByteOrder.nativeOrder()).asFloatBuffer()
        o.get(out, at, o.remaining())
        at += n
    }
    return out
}

/** A sine of [amplitude] (1 = full scale) on every channel. */
internal fun sine(hz: Double, amplitude: Double, frames: Int, channels: Int = 2, rate: Int = 48_000): DoubleArray =
    DoubleArray(frames * channels) { amplitude * sin(2 * PI * hz * (it / channels) / rate) }

internal fun DoubleArray.toShorts(): ShortArray = ShortArray(size) { (this[it] * 32767).toInt().coerceIn(-32768, 32767).toShort() }

internal fun DoubleArray.toFloats(): FloatArray = FloatArray(size) { this[it].toFloat() }

internal val PCM16 = C.ENCODING_PCM_16BIT
internal val PCM_FLOAT = C.ENCODING_PCM_FLOAT
