package app.attic.player.analysis

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Where a song really starts and ends, how its level moves near both ends, and its beat there: what a
 * smart transition plans with. Times are milliseconds from the start of the decoded audio. A null beat
 * means no steady beat was found in that window (an ambient intro, a spoken outro, a fade-out).
 */
data class SongTransitions(
    val decodedMs: Long,
    val audioStartMs: Long,
    val audioEndMs: Long,
    /** After this the song only gets quieter. Equal to [audioEndMs] for a cold ending. */
    val outroStartMs: Long,
    val intro: Beat?,
    val outro: Beat?,
    /** Mean onset strength over the song, 0..1: how busy it is, whatever its loudness. */
    val energy: Float,
    /** Levels relative to the song's reference, two per second, 30 s forward from [audioStartMs]. See [EnergyCurve]. */
    val introEnergy: ByteArray,
    /** The same for the 30 s that end at [audioEndMs], oldest first. */
    val outroEnergy: ByteArray,
) {
    /** A beat grid: [bpm], and one beat at [beatMs]; the others are `beatMs + k × 60000 / bpm`. */
    data class Beat(val bpm: Double, val beatMs: Long, val confidence: Float) {
        val periodMs: Double get() = 60_000.0 / bpm
    }

    override fun equals(other: Any?): Boolean = other is SongTransitions && other.decodedMs == decodedMs &&
        other.audioStartMs == audioStartMs && other.audioEndMs == audioEndMs && other.outroStartMs == outroStartMs &&
        other.intro == intro && other.outro == outro && other.energy == energy &&
        other.introEnergy.contentEquals(introEnergy) && other.outroEnergy.contentEquals(outroEnergy)

    override fun hashCode(): Int = (decodedMs * 31 + audioStartMs * 17 + audioEndMs).hashCode()
}

/** Level curves stored a byte per half second: 0 is the song's reference level, each step 0.5 dB below it. */
object EnergyCurve {
    const val BLOCK_MS = 500L
    const val BLOCKS = 60

    fun encode(relativeDb: Double): Byte = (-relativeDb * 2).roundToInt().coerceIn(0, 255).toByte()

    fun decode(value: Byte): Double = -(value.toInt() and 0xFF) / 2.0
}

/**
 * Measures [SongTransitions] from 16 kHz mono audio, fed in decoder-sized pieces. It keeps two numbers
 * per 10 ms hop, the level and the onset strength (positive spectral flux over 24 log-spaced bands of a
 * 512-point frame), for the first and last 90 s only, plus a histogram of half-second levels and running
 * sums for the whole song. Memory stays at about 60 KB whatever the song's length.
 */
class TransitionMeter {
    private val spectrum = PowerSpectrum(FRAME)
    private val window = FloatArray(FRAME) { (0.5 - 0.5 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
    private val windowed = FloatArray(FRAME)
    private val power = DoubleArray(FRAME / 2 + 1)
    private val bands = DoubleArray(BAND_EDGES.size - 1)
    private val previousBands = DoubleArray(BAND_EDGES.size - 1)
    private val history = FloatArray(FRAME)
    private var historyFilled = 0
    private var hopFilled = 0
    private var hopSquares = 0.0

    private val headLevel = FloatArray(KEEP_FRAMES)
    private val headFlux = FloatArray(KEEP_FRAMES)
    private val tailLevel = FloatArray(KEEP_FRAMES)
    private val tailFlux = FloatArray(KEEP_FRAMES)
    private var frames = 0

    private val blockHistogram = IntArray(HISTOGRAM_BINS)
    private var blockSquares = 0.0
    private var blockFrames = 0
    private var fluxSum = 0.0
    private var fluxMean = 0.0
    private var fluxFrames = 0

    /** Mono samples in -1..1 at 16 kHz. */
    fun add(samples: FloatArray) {
        for (x in samples) {
            history[historyFilled % FRAME] = x
            historyFilled++
            hopSquares += x * x
            if (++hopFilled == HOP) {
                closeHop()
                hopFilled = 0
                hopSquares = 0.0
            }
        }
    }

    private fun closeHop() {
        val meanSquare = hopSquares / HOP
        val level = (10 * log10(meanSquare + 1e-12)).toFloat()
        val flux = flux()
        val index = frames
        if (index < KEEP_FRAMES) {
            headLevel[index] = level
            headFlux[index] = flux
        }
        tailLevel[index % KEEP_FRAMES] = level
        tailFlux[index % KEEP_FRAMES] = flux
        frames++
        blockSquares += meanSquare
        if (++blockFrames == BLOCK_FRAMES) {
            val db = 10 * log10(blockSquares / BLOCK_FRAMES + 1e-12)
            blockHistogram[histogramBin(db)]++
            blockSquares = 0.0
            blockFrames = 0
        }
        // Onset strength above its running half-second mean: spikes count, steady sound doesn't.
        fluxMean += (flux - fluxMean) / ENERGY_SMOOTH_FRAMES
        if (level > ABSOLUTE_SILENCE_DB) {
            fluxSum += max(0.0, flux - fluxMean)
            fluxFrames++
        }
    }

    /** Positive change in log band power since the last hop, summed over bands. */
    private fun flux(): Float {
        if (historyFilled < FRAME) return 0f
        val start = historyFilled % FRAME
        for (i in 0 until FRAME) windowed[i] = history[(start + i) % FRAME] * window[i]
        spectrum.compute(windowed, power)
        var flux = 0.0
        for (b in bands.indices) {
            var sum = 0.0
            for (bin in BAND_EDGES[b] until BAND_EDGES[b + 1]) sum += power[bin]
            val value = ln(1 + LOG_GAIN * sum)
            val rise = value - previousBands[b]
            if (rise > 0) flux += rise
            previousBands[b] = value
            bands[b] = value
        }
        return flux.toFloat()
    }

    /** Null when the song was all silence or too short to say anything about. */
    fun result(): SongTransitions? {
        if (frames < MIN_FRAMES) return null
        val reference = referenceDb() ?: return null
        val threshold = max(ABSOLUTE_SILENCE_DB, reference - RELATIVE_SILENCE_DB)
        val start = (0 until min(frames, KEEP_FRAMES)).firstOrNull { level(it) > threshold } ?: return null
        val tailFirst = max(0, frames - KEEP_FRAMES)
        val end = (frames - 1 downTo tailFirst).firstOrNull { level(it) > threshold }?.plus(1) ?: return null
        if (end <= start) return null
        val outroStart = outroStart(start, end, reference)
        val intro = beat(start, min(end, start + BEAT_WINDOW_FRAMES), anchor = start)
        val outroFrom = max(start, max(tailFirst, outroStart - OUTRO_BEFORE_FRAMES))
        val outroTo = min(end, outroStart + OUTRO_AFTER_FRAMES)
        val outro = if (outroTo - outroFrom >= MIN_BEAT_FRAMES) beat(outroFrom, outroTo, anchor = outroStart) else null
        val meanFlux = if (fluxFrames == 0) 0.0 else fluxSum / fluxFrames
        return SongTransitions(
            decodedMs = frames * HOP_MS,
            audioStartMs = start * HOP_MS,
            audioEndMs = end * HOP_MS,
            outroStartMs = outroStart * HOP_MS,
            intro = intro,
            outro = outro,
            energy = (meanFlux / ENERGY_FULL_SCALE).coerceIn(0.0, 1.0).toFloat(),
            introEnergy = curve(start, forward = true, reference),
            outroEnergy = curve(end, forward = false, reference),
        )
    }

    /** Whether frame [i]'s numbers were kept (the first or last 90 s). */
    private fun kept(i: Int) = i in 0 until frames && (i < KEEP_FRAMES || i >= frames - KEEP_FRAMES)

    private fun level(i: Int): Float = if (i < KEEP_FRAMES) headLevel[i] else tailLevel[i % KEEP_FRAMES]

    private fun flux(i: Int): Float = if (i < KEEP_FRAMES) headFlux[i] else tailFlux[i % KEEP_FRAMES]

    /** The song's "full" level: the 90th percentile of its half-second levels, ignoring silence. */
    private fun referenceDb(): Double? {
        var total = 0
        for (bin in histogramBin(ABSOLUTE_SILENCE_DB) + 1 until HISTOGRAM_BINS) total += blockHistogram[bin]
        if (total == 0) return null
        val wanted = (total * 0.9).roundToInt().coerceAtLeast(1)
        var seen = 0
        for (bin in histogramBin(ABSOLUTE_SILENCE_DB) + 1 until HISTOGRAM_BINS) {
            seen += blockHistogram[bin]
            if (seen >= wanted) return HISTOGRAM_FLOOR_DB + (bin + 0.5) * HISTOGRAM_STEP_DB
        }
        return null
    }

    /** Mean power in dB of the frames in [from, to) that were kept; null if none were. */
    private fun meanDb(from: Int, to: Int): Double? {
        var sum = 0.0
        var count = 0
        for (i in max(0, from) until min(frames, to)) {
            if (!kept(i)) continue
            sum += 10.0.pow(level(i) / 10.0)
            count++
        }
        return if (count == 0) null else 10 * log10(sum / count + 1e-12)
    }

    /**
     * Scanning back from [end] in half-second blocks, each judged over the two seconds around it: the
     * end of the last block within [OUTRO_DROP_DB] of [reference].
     */
    private fun outroStart(start: Int, end: Int, reference: Double): Int {
        val earliest = max(start, max(0, frames - KEEP_FRAMES))
        var blockEnd = end
        while (blockEnd - BLOCK_FRAMES >= earliest) {
            val middle = blockEnd - BLOCK_FRAMES / 2
            val level = meanDb(middle - SMOOTH_FRAMES / 2, middle + SMOOTH_FRAMES / 2) ?: break
            if (level >= reference - OUTRO_DROP_DB) return startOfDecline(blockEnd, earliest, level).coerceAtMost(end)
            blockEnd -= BLOCK_FRAMES
        }
        return max(earliest, blockEnd)
    }

    /**
     * A fade-out crosses [OUTRO_DROP_DB] part way down, so from there this keeps walking back while the
     * level still rises going backwards, to where the decline began.
     */
    private fun startOfDecline(from: Int, earliest: Int, levelAtFrom: Double): Int {
        var blockEnd = from
        var level = levelAtFrom
        var flat = 0
        while (blockEnd - 2 * BLOCK_FRAMES >= earliest) {
            val middle = blockEnd - BLOCK_FRAMES - BLOCK_FRAMES / 2
            val earlier = meanDb(middle - SMOOTH_FRAMES / 2, middle + SMOOTH_FRAMES / 2) ?: break
            if (earlier >= level + DECLINE_STEP_DB) {
                flat = 0
            } else if (++flat >= 2) {
                break
            }
            if (earlier > level) level = earlier
            blockEnd -= BLOCK_FRAMES
        }
        return blockEnd + flat * BLOCK_FRAMES
    }

    private fun curve(from: Int, forward: Boolean, reference: Double): ByteArray = ByteArray(EnergyCurve.BLOCKS) { b ->
        val blockStart = if (forward) from + b * BLOCK_FRAMES else from - (EnergyCurve.BLOCKS - b) * BLOCK_FRAMES
        val db = meanDb(blockStart, blockStart + BLOCK_FRAMES) ?: (reference - 127.5)
        EnergyCurve.encode(db - reference)
    }

    /** Tempo and beat phase over frames [from, to), with the reported beat the one nearest [anchor]. */
    private fun beat(from: Int, to: Int, anchor: Int): SongTransitions.Beat? {
        if (to - from < MIN_BEAT_FRAMES) return null
        val onsets = FloatArray(to - from) { if (kept(from + it)) flux(from + it) else 0f }
        return BeatTracker.track(onsets)?.let { grid ->
            val anchorFrame = (anchor - from).toDouble()
            val k = Math.round((anchorFrame - grid.firstBeat) / grid.period)
            val beatFrame = grid.firstBeat + k * grid.period
            SongTransitions.Beat(
                bpm = 60_000.0 / (grid.period * HOP_MS),
                beatMs = ((from + beatFrame) * HOP_MS + ONSET_LATENCY_MS).roundToLong(),
                confidence = grid.confidence,
            )
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val FRAME = 512
        const val HOP = 160
        const val HOP_MS = 10L
        private const val KEEP_FRAMES = 9_000 // 90 s
        private const val BLOCK_FRAMES = 50 // 0.5 s
        private const val SMOOTH_FRAMES = 200 // 2 s
        private const val MIN_FRAMES = 100
        private const val BEAT_WINDOW_FRAMES = 2_000 // 20 s
        private const val MIN_BEAT_FRAMES = 600 // 6 s
        private const val OUTRO_BEFORE_FRAMES = 1_500
        private const val OUTRO_AFTER_FRAMES = 500
        private const val ABSOLUTE_SILENCE_DB = -60.0
        private const val RELATIVE_SILENCE_DB = 45.0
        const val OUTRO_DROP_DB = 8.0
        private const val DECLINE_STEP_DB = 0.3
        private const val LOG_GAIN = 1_000.0

        /** Mean onset strength per hop that counts as fully busy. See TransitionMeterTest for what fixtures read. */
        private const val ENERGY_FULL_SCALE = 6.0
        private const val ENERGY_SMOOTH_FRAMES = 25.0

        /**
         * A Hann-windowed frame's flux peaks once an onset is well inside it, about this long after the
         * frame's start. Measured on click tracks in TransitionMeterTest; both songs share it, so it
         * cancels in beat alignment and only matters for absolute positions.
         */
        private const val ONSET_LATENCY_MS = 0.0

        private const val HISTOGRAM_FLOOR_DB = -120.0
        private const val HISTOGRAM_STEP_DB = 0.5
        private const val HISTOGRAM_BINS = 260

        private fun histogramBin(db: Double): Int =
            floor((db - HISTOGRAM_FLOOR_DB) / HISTOGRAM_STEP_DB).toInt().coerceIn(0, HISTOGRAM_BINS - 1)

        /** 24 bands, log-spaced from 40 Hz to 8 kHz, as FFT bin edges at 31.25 Hz a bin. */
        private val BAND_EDGES: IntArray = run {
            val edges = (0..24).map { i ->
                val hz = 40.0 * (8_000.0 / 40.0).pow(i / 24.0)
                (hz / (SAMPLE_RATE.toDouble() / FRAME)).roundToInt().coerceIn(1, FRAME / 2 + 1)
            }.toMutableList()
            for (i in 1 until edges.size) if (edges[i] <= edges[i - 1]) edges[i] = edges[i - 1] + 1
            edges.toIntArray()
        }
    }
}

/**
 * Tempo and beat phase from an onset-strength envelope at 100 frames a second: autocorrelation weighted
 * toward 120 BPM, a comb for the phase, then a straight line through the picked beats, which pins the
 * period well below 0.5 % over 20 s.
 */
object BeatTracker {
    /** [period] and [firstBeat] in frames, [firstBeat] counted from the envelope's start. */
    class Grid(val period: Double, val firstBeat: Double, val confidence: Float)

    const val MIN_CONFIDENCE = 0.35f
    private const val MIN_LAG = 30 // 200 BPM
    private const val MAX_LAG = 100 // 60 BPM
    private const val PRIOR_BPM = 120.0
    private const val PRIOR_OCTAVES = 1.0
    private const val FRAMES_PER_MINUTE = 6_000.0
    private const val MIN_SALIENCE = 2.5
    private const val SALIENCE_RANGE = 3.0

    fun track(raw: FloatArray): Grid? {
        val salience = salience(raw)
        val onsets = normalised(raw) ?: return null
        val n = onsets.size
        if (n < MAX_LAG * 3) return null
        val ac = DoubleArray(2 * MAX_LAG + 2)
        for (lag in 0 until ac.size) {
            if (lag >= n) break
            var sum = 0.0
            for (t in 0 until n - lag) sum += onsets[t] * onsets[t + lag]
            ac[lag] = sum / (n - lag)
        }
        if (ac[0] <= 0) return null
        val score = DoubleArray(MAX_LAG + 2)
        for (lag in MIN_LAG - 1..MAX_LAG + 1) {
            val bpm = FRAMES_PER_MINUTE / lag
            val octaves = ln(bpm / PRIOR_BPM) / ln(2.0)
            val prior = exp(-0.5 * (octaves / PRIOR_OCTAVES) * (octaves / PRIOR_OCTAVES))
            score[lag] = prior * (ac[lag] + 0.5 * ac[min(2 * lag, ac.size - 1)])
        }
        var best = MIN_LAG
        for (lag in MIN_LAG..MAX_LAG) if (score[lag] > score[best]) best = lag
        val (a, b, c) = Triple(score[best - 1], score[best], score[best + 1])
        val denominator = a - 2 * b + c
        var period = best + if (denominator < 0) (0.5 * (a - c) / denominator).coerceIn(-0.5, 0.5) else 0.0

        // Phase: the offset whose comb collects the most onset strength.
        var phase = 0.0
        var phaseScore = -1.0
        var offset = 0.0
        while (offset < period) {
            var sum = 0.0
            var t = offset
            while (t < n) {
                sum += onsets[t.roundToInt().coerceAtMost(n - 1)]
                t += period
            }
            if (sum > phaseScore) {
                phaseScore = sum
                phase = offset
            }
            offset += 1.0
        }

        // Refine twice: pick the strongest onset near each expected beat, then fit a line through them.
        var first = phase
        repeat(2) {
            val ks = ArrayList<Double>()
            val ts = ArrayList<Double>()
            var k = 0
            while (true) {
                val expected = first + k * period
                if (expected >= n) break
                val reach = 0.15 * period
                val lo = max(0, (expected - reach).roundToInt())
                val hi = min(n - 1, (expected + reach).roundToInt())
                var at = -1
                for (i in lo..hi) if (at < 0 || onsets[i] > onsets[at]) at = i
                if (at >= 0 && onsets[at] > 0.3) {
                    ks += k.toDouble()
                    ts += peak(onsets, at)
                }
                k++
            }
            if (ks.size < 4) return null
            val meanK = ks.average()
            val meanT = ts.average()
            var sxy = 0.0
            var sxx = 0.0
            for (i in ks.indices) {
                sxy += (ks[i] - meanK) * (ts[i] - meanT)
                sxx += (ks[i] - meanK) * (ks[i] - meanK)
            }
            if (sxx <= 0) return null
            period = sxy / sxx
            first = meanT - period * meanK
        }
        if (period < MIN_LAG * 0.9 || period > MAX_LAG * 1.1) return null

        // How periodic the envelope is at that tempo, against how much of it is on the beats.
        val periodicity = interpolated(ac, period) / ac[0]
        var onBeat = 0.0
        var beats = 0
        var t = first
        while (t < n) {
            if (t >= 0) {
                onBeat += onsets[t.roundToInt().coerceIn(0, n - 1)]
                beats++
            }
            t += period
        }
        val mean = onsets.average()
        val contrast = if (beats == 0 || mean <= 0) 0.0 else (onBeat / beats) / mean
        val shape = 0.5 * (periodicity / 0.5).coerceIn(0.0, 1.0) + 0.5 * ((contrast - 1) / 4).coerceIn(0.0, 1.0)
        // A beat needs onsets that stand out: steady sound only ever flickers a little.
        val confidence = (shape * ((salience - MIN_SALIENCE) / SALIENCE_RANGE).coerceIn(0.0, 1.0)).toFloat()
        while (first < 0) first += period
        return Grid(period, first, confidence)
    }

    /** How far the envelope's peaks stand above its typical value: its 99th percentile over its median. */
    private fun salience(raw: FloatArray): Double {
        if (raw.isEmpty()) return 0.0
        val sorted = raw.sortedArray()
        val median = sorted[sorted.size / 2].toDouble()
        val top = sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)].toDouble()
        return top / (median + 1e-6)
    }

    /** The envelope minus its local mean over half a second, clipped at zero, scaled so its peaks are near 1. */
    private fun normalised(raw: FloatArray): DoubleArray? {
        val n = raw.size
        if (n == 0) return null
        val half = 25
        val prefix = DoubleArray(n + 1)
        for (i in 0 until n) prefix[i + 1] = prefix[i] + raw[i]
        val out = DoubleArray(n) { i ->
            val lo = max(0, i - half)
            val hi = min(n, i + half + 1)
            max(0.0, raw[i] - (prefix[hi] - prefix[lo]) / (hi - lo))
        }
        val sorted = out.sortedArray()
        val scale = sorted[(n * 0.99).toInt().coerceAtMost(n - 1)]
        if (scale <= 1e-9) return null
        for (i in out.indices) out[i] = min(out[i] / scale, 2.0)
        return out
    }

    /** Sub-frame position of the peak at [i], by a parabola through its neighbours. */
    private fun peak(values: DoubleArray, i: Int): Double {
        if (i <= 0 || i >= values.size - 1) return i.toDouble()
        val a = values[i - 1]
        val b = values[i]
        val c = values[i + 1]
        val denominator = a - 2 * b + c
        return if (denominator < 0) i + (0.5 * (a - c) / denominator).coerceIn(-0.5, 0.5) else i.toDouble()
    }

    private fun interpolated(values: DoubleArray, at: Double): Double {
        val i = floor(at).toInt().coerceIn(0, values.size - 2)
        val f = at - i
        return values[i] * (1 - f) + values[i + 1] * f
    }
}

/**
 * A song's key from the chroma analysis stores: the frames averaged and matched against the 24 rotations
 * of the Krumhansl–Kessler profiles. Keys 0–11 are C major to B major, 12–23 C minor to B minor.
 */
object KeyEstimate {
    class Key(val key: Int, val confidence: Float)

    private val MAJOR = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
    private val MINOR = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

    /** Null when there is no chroma or it is flat. Chroma comes as 12 unsigned bytes a frame. */
    fun of(chroma: ByteArray?): Key? {
        if (chroma == null || chroma.size < 12) return null
        val mean = DoubleArray(12)
        val frames = chroma.size / 12
        for (f in 0 until frames) for (c in 0 until 12) mean[c] += (chroma[f * 12 + c].toInt() and 0xFF).toDouble()
        if (mean.all { it == mean[0] }) return null
        val scores = DoubleArray(24) { key ->
            val profile = if (key < 12) MAJOR else MINOR
            correlation(mean) { c -> profile[Math.floorMod(c - key % 12, 12)] }
        }
        val order = scores.indices.sortedByDescending { scores[it] }
        val best = order[0]
        return Key(best, (scores[best] - scores[order[1]]).toFloat().coerceIn(0f, 1f))
    }

    private fun correlation(x: DoubleArray, y: (Int) -> Double): Double {
        val ys = DoubleArray(12) { y(it) }
        val mx = x.average()
        val my = ys.average()
        var sxy = 0.0
        var sxx = 0.0
        var syy = 0.0
        for (i in 0 until 12) {
            sxy += (x[i] - mx) * (ys[i] - my)
            sxx += (x[i] - mx) * (x[i] - mx)
            syy += (ys[i] - my) * (ys[i] - my)
        }
        return if (sxx <= 0 || syy <= 0) 0.0 else sxy / sqrt(sxx * syy)
    }

    /** Steps apart on the Camelot wheel (0–6), counting a relative major/minor pair as one step. */
    fun camelotDistance(a: Int, b: Int): Int {
        fun position(key: Int): Pair<Int, Boolean> {
            val minor = key >= 12
            // Relative minor shares the major's position: A minor (21) sits with C major (0).
            val root = if (minor) (key - 12 + 3) % 12 else key
            return Pair((root * 7) % 12, minor)
        }
        val (pa, ma) = position(a)
        val (pb, mb) = position(b)
        val around = abs(pa - pb).let { min(it, 12 - it) }
        return around + if (ma != mb) 1 else 0
    }
}
