package app.attic.player.analysis

import android.media.AudioFormat
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Power spectrum of a real signal, using a radix-2 FFT with its tables computed once. */
internal class PowerSpectrum(private val size: Int) {
    private val cosTable = DoubleArray(size / 2) { cos(2 * PI * it / size) }
    private val sinTable = DoubleArray(size / 2) { sin(2 * PI * it / size) }
    private val reversed = IntArray(size).also { table ->
        val bits = log2(size.toDouble()).roundToInt()
        for (i in 0 until size) table[i] = Integer.reverse(i) ushr (32 - bits)
    }
    private val re = DoubleArray(size)
    private val im = DoubleArray(size)

    /** Returns size/2 + 1 values, written into [out]. */
    fun compute(windowed: FloatArray, out: DoubleArray) {
        for (i in 0 until size) {
            re[i] = windowed[reversed[i]].toDouble()
            im[i] = 0.0
        }
        var span = 2
        while (span <= size) {
            val half = span / 2
            val step = size / span
            var start = 0
            while (start < size) {
                for (k in 0 until half) {
                    val wr = cosTable[k * step]
                    val wi = -sinTable[k * step]
                    val a = start + k
                    val b = a + half
                    val tr = wr * re[b] - wi * im[b]
                    val ti = wr * im[b] + wi * re[b]
                    re[b] = re[a] - tr
                    im[b] = im[a] - ti
                    re[a] += tr
                    im[a] += ti
                }
                start += span
            }
            span *= 2
        }
        for (i in 0..size / 2) out[i] = re[i] * re[i] + im[i] * im[i]
    }
}

/**
 * The input EffNet expects, matching Essentia's TensorflowInputMusiCNN: 16 kHz audio cut into frames
 * of 512 samples every 256, turned into 96 mel bands, then compressed with log10(1 + 10000x).
 */
class MelSpectrogram {
    private val spectrum = PowerSpectrum(FRAME)
    private val window = FloatArray(FRAME) { (0.5 - 0.5 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
    private val windowed = FloatArray(FRAME)
    private val power = DoubleArray(FRAME / 2 + 1)
    private val bandStart = IntArray(BANDS)
    private val bandWeights = arrayOfNulls<DoubleArray>(BANDS)

    init {
        val points = DoubleArray(BANDS + 2) { fromMel(toMel(0.0) + it * (toMel(8000.0) - toMel(0.0)) / (BANDS + 1)) }
        val hzPerBin = 8000.0 / (FRAME / 2)
        for (band in 0 until BANDS) {
            val low = points[band]
            val mid = points[band + 1]
            val high = points[band + 2]
            val first = (low / hzPerBin).roundToInt()
            val last = (high / hzPerBin).roundToInt()
            bandStart[band] = first
            bandWeights[band] = DoubleArray(last - first + 1) { i ->
                val hz = (first + i) * hzPerBin
                val rise = when {
                    hz >= low && hz < mid -> (hz - low) / (mid - low)
                    hz >= mid && hz < high -> (high - hz) / (high - mid)
                    else -> 0.0
                }
                rise * 2 / (high - low)
            }
        }
    }

    /** Fills 96 values for the frame at [start]. */
    fun frame(pcm: FloatArray, start: Int, out: FloatArray = FloatArray(BANDS), outOffset: Int = 0): FloatArray {
        for (i in 0 until FRAME) windowed[i] = pcm[start + i] * window[i]
        spectrum.compute(windowed, power)
        for (band in 0 until BANDS) {
            val weights = bandWeights[band]!!
            val first = bandStart[band]
            var sum = 0.0
            for (i in weights.indices) sum += weights[i] * power[first + i]
            out[outOffset + band] = log10(1 + 10_000 * sum).toFloat()
        }
        return out
    }

    /**
     * Patches of 128 frames, taking every fourth of the positions EffNet's authors use. Tested on 30
     * songs, the averaged result stays above 0.995 cosine of the full version at a quarter of the work.
     */
    fun patches(pcm: FloatArray): List<FloatArray> {
        val frames = if (pcm.size < FRAME) 0 else (pcm.size - FRAME) / HOP + 1
        return buildList {
            var first = 0
            while (first + PATCH_FRAMES <= frames) {
                val patch = FloatArray(PATCH_FRAMES * BANDS)
                for (f in 0 until PATCH_FRAMES) frame(pcm, (first + f) * HOP, patch, f * BANDS)
                add(patch)
                first += PATCH_STEP_FRAMES
            }
        }
    }

    private fun toMel(hz: Double): Double =
        if (hz < 1000) 3 * hz / 200 else 15 + 27 * log10(hz / 1000) / log10(6.4)

    private fun fromMel(mel: Double): Double =
        if (mel < 15) 200 * mel / 3 else 1000 * 6.4.pow((mel - 15) / 27)

    companion object {
        const val FRAME = 512
        const val HOP = 256
        const val BANDS = 96
        const val PATCH_FRAMES = 128
        private const val PATCH_STEP_FRAMES = 62 * 4
    }
}

/** Twelve pitch-class strengths, four times a second, stored as bytes. This is what melody matching reads. */
object Chroma {
    private const val FRAME = 4096
    private const val HOP = 4000
    private const val SAMPLE_RATE = 16_000
    private const val MAX_FRAMES = 2400 // Ten minutes, enough to recognise a song inside a long mix.

    fun frames(pcm: FloatArray): ByteArray {
        val count = if (pcm.size <= FRAME) 0 else ((pcm.size - FRAME) / HOP).coerceAtMost(MAX_FRAMES)
        val out = ByteArray(count * 12)
        if (count == 0) return out
        val spectrum = PowerSpectrum(FRAME)
        val window = FloatArray(FRAME) { (0.5 - 0.5 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
        val binHz = SAMPLE_RATE.toDouble() / FRAME
        val firstBin = kotlin.math.ceil(100 / binHz).toInt()
        val lastBin = (2000 / binHz).toInt()
        val pitchClass = IntArray(lastBin - firstBin + 1) { i ->
            Math.floorMod((12 * log2((firstBin + i) * binHz / 440.0)).roundToInt(), 12)
        }
        val windowed = FloatArray(FRAME)
        val power = DoubleArray(FRAME / 2 + 1)
        val classes = DoubleArray(12)
        for (f in 0 until count) {
            for (i in 0 until FRAME) windowed[i] = pcm[f * HOP + i] * window[i]
            spectrum.compute(windowed, power)
            classes.fill(0.0)
            for (i in pitchClass.indices) classes[pitchClass[i]] += power[firstBin + i]
            var norm = 0.0
            for (c in 0 until 12) {
                classes[c] = sqrt(classes[c])
                norm += classes[c] * classes[c]
            }
            norm = sqrt(norm) + 1e-9
            for (c in 0 until 12) out[f * 12 + c] = (classes[c] / norm * 255).roundToInt().toByte()
        }
        return out
    }
}

/** Linear interpolation, the same resampling the reference embeddings used, and safe across decoder buffers. */
class LinearResampler(inputRate: Int, outputRate: Int) {
    private val step = inputRate.toDouble() / outputRate
    private var position = 0.0
    private var previous = 0f

    fun push(input: FloatArray): FloatArray {
        if (input.isEmpty()) return input
        val capacity = (input.size / step).toInt() + 2
        val out = FloatArray(capacity)
        var written = 0
        // position counts from the sample before this buffer, which sits at -1.
        while (position < input.size - 1 && written < capacity) {
            val index = kotlin.math.floor(position).toInt()
            val fraction = (position - index).toFloat()
            val from = if (index < 0) previous else input[index]
            out[written++] = from + (input[index + 1] - from) * fraction
            position += step
        }
        previous = input.last()
        position -= input.size
        return out.copyOf(written)
    }
}

object PcmBuffer {
    /** Reads a decoder's output as floats. Null for a format this doesn't know, rather than misreading it. */
    fun samples(buffer: ByteBuffer, encoding: Int): FloatArray? = when (encoding) {
        AudioFormat.ENCODING_PCM_16BIT -> buffer.asShortBuffer().let { s -> FloatArray(s.remaining()) { s.get(it) / 32768f } }
        AudioFormat.ENCODING_PCM_32BIT -> buffer.asIntBuffer().let { s -> FloatArray(s.remaining()) { s.get(it) / 2147483648f } }
        AudioFormat.ENCODING_PCM_FLOAT -> buffer.asFloatBuffer().let { s -> FloatArray(s.remaining()) { s.get(it) } }
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
            val start = buffer.position()
            FloatArray(buffer.remaining() / 3) { i ->
                val at = start + i * 3
                val value = (buffer.get(at).toInt() and 0xFF) or
                    ((buffer.get(at + 1).toInt() and 0xFF) shl 8) or
                    (buffer.get(at + 2).toInt() shl 16)
                value / 8388608f
            }
        }
        else -> null
    }

    fun toMono(interleaved: FloatArray, channels: Int): FloatArray {
        if (channels <= 1) return interleaved
        return FloatArray(interleaved.size / channels) { frame ->
            var sum = 0f
            for (c in 0 until channels) sum += interleaved[frame * channels + c]
            sum / channels
        }
    }
}
