package app.attic.player.playback.eq

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Where the equaliser gets each song's curve. */
interface EqSource {
    /** Goes up whenever any answer below may have changed (a slider moved, a song saved). Read once per buffer. */
    val version: Long

    /** The curve for the item a period belongs to, or null to leave it untouched. Called on the playback thread; must not block. */
    fun curveFor(timeline: Timeline, periodUid: Any?): EqCurve?
}

/**
 * Where the equaliser's diagnostics go. [enabled] is asked once per flush, never per buffer. The default
 * writes to logcat under [TAG] when `adb shell setprop log.tag.AtticEq DEBUG` is set; unit tests get
 * nothing, as android.util.Log is a stub there.
 */
interface EqLogger {
    val enabled: Boolean

    fun log(message: String)

    object Android : EqLogger {
        override val enabled: Boolean
            get() = try {
                Log.isLoggable(TAG, Log.DEBUG)
            } catch (_: Throwable) {
                false
            }

        override fun log(message: String) {
            try {
                Log.d(TAG, message)
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        const val TAG = "AtticEq"
    }
}

/**
 * A ten-band graphic equaliser on ExoPlayer's playback thread, with a curve per song.
 *
 * Per-song switching works like [app.attic.player.playback.LevellingAudioProcessor]: since Media3 1.11
 * the sink flushes its processors at each new playlist item with the item's period, after the previous
 * item has drained, so [onFlush] looks the new curve up and applies it from the first sample, gapless
 * transitions included, with the filters' memory cleared. A change mid-song (a slider dragged, a preset
 * picked) is seen through [EqSource.version] once per buffer and glides over [GLIDE_SECONDS]: the filter
 * gains move in a straight line in dB, coefficients are recomputed every [GLIDE_BLOCK_FRAMES] frames, and
 * the filters keep their memory, so there is no click and no zipper noise.
 *
 * Precision. Filters run as transposed direct form II with double coefficients and state. In float, the
 * 31 Hz filter at 48 kHz has its poles within 0.003 of the unit circle and its state words cancel each
 * other; measured error was about -58 dBFS RMS at +12 dB (EqualiserAudioProcessorTest prints it), far
 * above 16-bit's -96 dBFS floor. In double it is below -150 dBFS. On ARM64, scalar double arithmetic
 * runs at the same rate as float, so precision costs nothing measurable.
 *
 * Headroom. A boost would push full-scale audio over, so a pre-gain takes back the curve's peak response
 * (see [EqDesign.HEADROOM_COMPENSATION]), and a peak limiter catches what that misses: filters can raise
 * a waveform's peaks through phase alone. The limiter is linked across channels, attacks instantly (the
 * gain drops on the very sample that would cross [CEILING_DB]) and releases over [RELEASE_SECONDS]. It
 * normally does nothing at all.
 *
 * Cost (EqBenchmarkTest): a flat or missing curve copies the buffer straight through; bands whose filter
 * gain is 0 dB are skipped.
 *
 * It belongs after levelling in the chain, so its limiter is the last stage before the sink and no later
 * gain can push audio back over the ceiling.
 */
@OptIn(UnstableApi::class)
class EqualiserAudioProcessor(private val source: EqSource, private val logger: EqLogger = EqLogger.Android) : BaseAudioProcessor() {
    /** The curve now applied or being glided to, or null when the equaliser is leaving audio alone. */
    @Volatile
    var appliedCurve: EqCurve? = null
        private set

    /** The pre-gain for [appliedCurve], in dB: zero or negative. */
    @Volatile
    var preGainDb: Float = 0f
        private set

    private var timeline: Timeline = Timeline.EMPTY
    private var periodUid: Any? = null
    private var seenVersion = Long.MIN_VALUE
    private var target: EqCurve? = null

    private var design: EqDesign? = null
    private var sampleRate = 0
    private var channels = 0

    /** Filter gains in dB now, and the glide's ends. */
    private val gains = DoubleArray(EqBands.COUNT)
    private val glideFrom = DoubleArray(EqBands.COUNT)
    private val glideTo = DoubleArray(EqBands.COUNT)
    private var preNowDb = 0.0
    private var preFromDb = 0.0
    private var preToDb = 0.0
    private var glideFrames = 0
    private var glideDone = 0

    /** b0, b1, b2, a1, a2 per band. */
    private val coeffs = DoubleArray(5 * EqBands.COUNT)

    /** Whether a band does anything: a gain other than 0 dB, or memory still ringing out after one. */
    private val live = BooleanArray(EqBands.COUNT)

    /** Two state words per band per channel: index (band * channels + channel) * 2. */
    private var state = DoubleArray(0)

    private var limiterGain = 1.0
    private var release = 0.0

    private var work = DoubleArray(0)
    private var shorts = ShortArray(0)
    private var floats = FloatArray(0)

    private var debug = false
    private var stats: Stats? = null

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            // Anything else passes through untouched: the sink converts to 16-bit before this runs unless float output is on.
            else -> AudioFormat.NOT_SET
        }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        if (!isActive) return
        debug = logger.enabled
        sampleRate = inputAudioFormat.sampleRate
        channels = inputAudioFormat.channelCount
        design = EqDesign.forRate(sampleRate)
        val words = EqBands.COUNT * channels * 2
        if (state.size != words) state = DoubleArray(words) else state.fill(0.0)
        limiterGain = 1.0
        release = exp(-1.0 / (RELEASE_SECONDS * sampleRate))
        timeline = streamMetadata.timeline
        periodUid = streamMetadata.periodUid
        seenVersion = source.version
        // A new song (or a seek) starts on its own curve at once; only a change mid-song glides.
        jumpTo(resolve())
        stats = if (debug) Stats(sampleRate) else null
        if (debug) logger.log("item ${streamMetadata.periodUid}: ${describe()}")
    }

    override fun onReset() {
        design = null
        target = null
        appliedCurve = null
        preGainDb = 0f
        timeline = Timeline.EMPTY
        periodUid = null
        seenVersion = Long.MIN_VALUE
        gains.fill(0.0)
        live.fill(false)
        glideFrames = 0
        preNowDb = 0.0
        state = DoubleArray(0)
        limiterGain = 1.0
        stats = null
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val design = design
        if (design != null) {
            val version = source.version
            if (version != seenVersion) {
                seenVersion = version
                val curve = resolve()
                if (curve != target) {
                    startGlide(curve)
                    if (debug) logger.log("change: ${describe()}")
                }
            }
        }
        val output = replaceOutputBuffer(size)
        val frames = size / inputAudioFormat.bytesPerFrame
        val float = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (design == null || isIdentity()) {
            stats?.let { s ->
                read(inputBuffer.duplicate().order(inputBuffer.order()), frames, float)
                s.add(work, frames, channels, input = true)
                s.add(work, frames, channels, input = false)
                s.maybeLog(logger)
            }
            output.put(inputBuffer)
            output.flip()
            return
        }
        read(inputBuffer, frames, float)
        stats?.add(work, frames, channels, input = true)
        var frame = 0
        while (frame < frames) {
            val n: Int
            val preStart = linear(preNowDb)
            if (glideDone < glideFrames) {
                n = min(GLIDE_BLOCK_FRAMES, frames - frame)
                stepGlide(design, n)
            } else {
                n = frames - frame
            }
            filter(frame, n)
            limit(frame, n, preStart, linear(preNowDb))
            frame += n
        }
        settleTails()
        stats?.let {
            it.add(work, frames, channels, input = false)
            it.maybeLog(logger)
        }
        write(output, frames, float)
    }

    private fun resolve(): EqCurve? = runCatching { source.curveFor(timeline, periodUid) }.getOrNull()?.coerced()?.takeUnless { it.isFlat }

    private fun jumpTo(curve: EqCurve?) {
        val design = design ?: return
        target = curve
        val filterGains = if (curve == null) DoubleArray(EqBands.COUNT) else design.filterGainsDb(curve)
        filterGains.copyInto(gains)
        filterGains.copyInto(glideTo)
        preNowDb = design.preGainDb(filterGains)
        preToDb = preNowDb
        glideFrames = 0
        glideDone = 0
        for (b in 0 until EqBands.COUNT) {
            design.coefficients(b, gains[b], coeffs, 5 * b)
            live[b] = gains[b] != 0.0
        }
        appliedCurve = curve
        preGainDb = preNowDb.toFloat()
    }

    private fun startGlide(curve: EqCurve?) {
        val design = design ?: return
        target = curve
        val filterGains = if (curve == null) DoubleArray(EqBands.COUNT) else design.filterGainsDb(curve)
        gains.copyInto(glideFrom)
        filterGains.copyInto(glideTo)
        preFromDb = preNowDb
        preToDb = design.preGainDb(filterGains)
        glideFrames = max(1, (GLIDE_SECONDS * sampleRate).toInt())
        glideDone = 0
        appliedCurve = curve
        preGainDb = preToDb.toFloat()
    }

    /** Moves the glide on by [frames] and sets the coefficients for the block that follows. */
    private fun stepGlide(design: EqDesign, frames: Int) {
        glideDone = min(glideFrames, glideDone + frames)
        val p = glideDone.toDouble() / glideFrames
        val done = glideDone >= glideFrames
        for (b in 0 until EqBands.COUNT) {
            if (glideFrom[b] == glideTo[b] && gains[b] == glideTo[b]) continue
            gains[b] = if (done) glideTo[b] else glideFrom[b] + (glideTo[b] - glideFrom[b]) * p
            design.coefficients(b, gains[b], coeffs, 5 * b)
            if (gains[b] != 0.0) live[b] = true
        }
        preNowDb = if (done) preToDb else preFromDb + (preToDb - preFromDb) * p
    }

    private fun isIdentity(): Boolean {
        if (glideDone < glideFrames || preNowDb != 0.0 || limiterGain != 1.0) return false
        for (b in 0 until EqBands.COUNT) if (live[b]) return false
        return true
    }

    private fun filter(start: Int, n: Int) {
        for (b in 0 until EqBands.COUNT) {
            if (!live[b]) continue
            if (channels == 2) filterStereo(b, start, n) else filterAny(b, start, n)
        }
    }

    /** Both channels in one pass: two independent chains the CPU can overlap. */
    private fun filterStereo(b: Int, start: Int, n: Int) {
        val k = 5 * b
        val b0 = coeffs[k]; val b1 = coeffs[k + 1]; val b2 = coeffs[k + 2]; val a1 = coeffs[k + 3]; val a2 = coeffs[k + 4]
        val s = b * 4
        var l1 = state[s]; var l2 = state[s + 1]; var r1 = state[s + 2]; var r2 = state[s + 3]
        val x = work
        var i = start * 2
        val end = (start + n) * 2
        while (i < end) {
            val xl = x[i]
            val yl = b0 * xl + l1
            l1 = b1 * xl - a1 * yl + l2
            l2 = b2 * xl - a2 * yl
            x[i] = yl
            val xr = x[i + 1]
            val yr = b0 * xr + r1
            r1 = b1 * xr - a1 * yr + r2
            r2 = b2 * xr - a2 * yr
            x[i + 1] = yr
            i += 2
        }
        state[s] = l1; state[s + 1] = l2; state[s + 2] = r1; state[s + 3] = r2
    }

    private fun filterAny(b: Int, start: Int, n: Int) {
        val k = 5 * b
        val b0 = coeffs[k]; val b1 = coeffs[k + 1]; val b2 = coeffs[k + 2]; val a1 = coeffs[k + 3]; val a2 = coeffs[k + 4]
        val x = work
        for (c in 0 until channels) {
            val s = (b * channels + c) * 2
            var s1 = state[s]; var s2 = state[s + 1]
            var i = start * channels + c
            val end = (start + n) * channels
            while (i < end) {
                val xi = x[i]
                val y = b0 * xi + s1
                s1 = b1 * xi - a1 * y + s2
                s2 = b2 * xi - a2 * y
                x[i] = y
                i += channels
            }
            state[s] = s1; state[s + 1] = s2
        }
    }

    /** Pre-gain, ramped from [preStart] to [preEnd] across the block, then the peak limiter. */
    private fun limit(start: Int, n: Int, preStart: Double, preEnd: Double) {
        val x = work
        val step = (preEnd - preStart) / n
        var pre = preStart
        var g = limiterGain
        val ceiling = CEILING
        for (f in start until start + n) {
            pre += step
            val base = f * channels
            var peak = 0.0
            for (c in 0 until channels) {
                val v = abs(x[base + c]) * pre
                if (v > peak) peak = v
            }
            if (g != 1.0) {
                g = 1.0 - (1.0 - g) * release
                if (g > LIMITER_SNAP) g = 1.0
            }
            if (peak * g > ceiling) g = ceiling / peak
            val m = pre * g
            if (m != 1.0) for (c in 0 until channels) x[base + c] *= m
        }
        limiterGain = g
    }

    /**
     * Clears memory too small to matter: it keeps denormals out of the filters during silence, and lets a
     * band that glided to 0 dB stop being processed once its ringing has died away.
     */
    private fun settleTails() {
        for (b in 0 until EqBands.COUNT) {
            if (!live[b]) continue
            val from = b * channels * 2
            val until = from + channels * 2
            var biggest = 0.0
            for (i in from until until) {
                val v = abs(state[i])
                if (v < DENORMAL_GUARD) state[i] = 0.0
                if (v > biggest) biggest = v
            }
            if (gains[b] == 0.0 && glideDone >= glideFrames && biggest < TAIL_SILENCE) {
                for (i in from until until) state[i] = 0.0
                live[b] = false
            }
        }
    }

    private fun read(input: ByteBuffer, frames: Int, float: Boolean) {
        val samples = frames * channels
        if (work.size < samples) work = DoubleArray(samples)
        if (float) {
            if (floats.size < samples) floats = FloatArray(samples)
            input.asFloatBuffer().get(floats, 0, samples)
            for (i in 0 until samples) work[i] = floats[i].toDouble()
        } else {
            if (shorts.size < samples) shorts = ShortArray(samples)
            input.asShortBuffer().get(shorts, 0, samples)
            for (i in 0 until samples) work[i] = shorts[i] * SHORT_SCALE
        }
        input.position(input.position() + samples * (if (float) 4 else 2))
    }

    private fun write(output: ByteBuffer, frames: Int, float: Boolean) {
        val samples = frames * channels
        if (float) {
            for (i in 0 until samples) floats[i] = work[i].toFloat()
            output.asFloatBuffer().put(floats, 0, samples)
            output.position(samples * 4)
        } else {
            for (i in 0 until samples) {
                val v = work[i] * 32768.0
                val r = if (v >= 0) (v + 0.5).toInt() else (v - 0.5).toInt()
                shorts[i] = r.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            output.asShortBuffer().put(shorts, 0, samples)
            output.position(samples * 2)
        }
        output.flip()
    }

    private fun describe(): String {
        val curve = target
        return if (curve == null) "flat (pass-through)" else "curve ${curve.encode()}, filters ${gains.joinToString(",") { String.format(Locale.ROOT, "%.1f", it) }}, pre-gain ${String.format(Locale.ROOT, "%.2f", preToDb)} dB"
    }

    /**
     * Once a second, input and output RMS in dBFS, over the whole band and below about 177 Hz (the edge
     * between the 125 and 250 Hz bands), on the channels' mean. Only built when the debug log is on.
     */
    private class Stats(private val sampleRate: Int) {
        private val lowPass = Biquad.lowPass(177.0, 0.7071, sampleRate)
        private val inLow = DoubleArray(2)
        private val outLow = DoubleArray(2)
        private var inAll = 0.0
        private var inBass = 0.0
        private var outAll = 0.0
        private var outBass = 0.0
        private var inFrames = 0
        private var outFrames = 0

        fun add(x: DoubleArray, frames: Int, channels: Int, input: Boolean) {
            val s = if (input) inLow else outLow
            var all = 0.0
            var bass = 0.0
            for (f in 0 until frames) {
                var m = 0.0
                for (c in 0 until channels) m += x[f * channels + c]
                m /= channels
                val y = lowPass.b0 * m + s[0]
                s[0] = lowPass.b1 * m - lowPass.a1 * y + s[1]
                s[1] = lowPass.b2 * m - lowPass.a2 * y
                all += m * m
                bass += y * y
            }
            if (input) {
                inAll += all; inBass += bass; inFrames += frames
            } else {
                outAll += all; outBass += bass; outFrames += frames
            }
        }

        fun maybeLog(logger: EqLogger) {
            if (outFrames < sampleRate) return
            fun db(sum: Double, n: Int) = String.format(Locale.ROOT, "%.1f", 10 * log10(max(sum / max(n, 1), 1e-20)))
            logger.log("rms in ${db(inAll, inFrames)} dBFS (bass ${db(inBass, inFrames)}), out ${db(outAll, outFrames)} dBFS (bass ${db(outBass, outFrames)})")
            inAll = 0.0; inBass = 0.0; outAll = 0.0; outBass = 0.0; inFrames = 0; outFrames = 0
        }
    }

    companion object {
        /** Peaks stay 1 dB under full scale, like levelling's ceiling, leaving room for the phone's resampler. */
        const val CEILING_DB = -1.0
        val CEILING = 10.0.pow(CEILING_DB / 20)

        /** The limiter's release time constant. */
        const val RELEASE_SECONDS = 0.08

        /** How long a mid-song change takes. Long enough to sound like a slider, short enough to feel live. */
        const val GLIDE_SECONDS = 0.08
        const val GLIDE_BLOCK_FRAMES = 64

        private const val SHORT_SCALE = 1.0 / 32768.0
        private const val LIMITER_SNAP = 1.0 - 1e-7
        private const val DENORMAL_GUARD = 1e-25
        private const val TAIL_SILENCE = 1e-12

        private fun linear(db: Double): Double = if (db == 0.0) 1.0 else 10.0.pow(db / 20)
    }
}
