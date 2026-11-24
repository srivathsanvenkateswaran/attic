package app.attic.player.playback.eq

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The equaliser's ten filters at one sample rate, and the solve that turns slider positions into filter
 * gains.
 *
 * Filter choice. Bands 31 Hz to 8 kHz are peaking filters at Q 0.8 (about 1.7 octaves wide at the half-gain
 * points). The textbook octave Q of 1.41 leaves dips of about 2 dB between bands with every slider at +12
 * even after compensation; Q 0.8 leaves at most 0.73 dB, so a broad boost sounds like one smooth lift rather than ten
 * bumps. Wider filters overlap more, which the compensation below absorbs. The 31 Hz band stays a peaking
 * filter rather than a low shelf: a shelf would lift everything down to DC, spending headroom (and small
 * speakers' excursion) on rumble nobody hears, while the peaking filter is back to +2.7 dB by 10 Hz.
 *
 * The 16 kHz band is a high shelf with its corner at 11.3 kHz, the geometric midpoint of 8 and 16 kHz.
 * A peaking filter there is squeezed against Nyquist by the bilinear transform: at 44.1 kHz it left a
 * 2.2 dB hole at 11.3 kHz with all sliders at +12 and fell to +2.4 dB by 20 kHz, so "treble" meant
 * different things at 44.1 and 48 kHz. The shelf keeps the top octave lifted to Nyquist at both rates.
 *
 * Compensation. Neighbouring filters add up: ten bands at +6 dB give far more than +6 dB. Following
 * Välimäki and Liski, "Accurate Cascade Graphic Equalizer" (IEEE SPL, 2017), each filter's dB response is
 * treated as roughly proportional to its gain. An interaction matrix holds every filter's response, per dB
 * of a [PROTOTYPE_GAIN_DB] prototype, at the ten centres and the nine midpoints between them; a weighted
 * least-squares solve finds the filter gains whose summed responses meet the sliders at the centres and
 * the straight line between them at the midpoints. The response shape changes with gain, so the solve is
 * repeated [ITERATIONS] times with the matrix rebuilt from the previous answer's real filters. Midpoints
 * weigh [MIDPOINT_WEIGHT] of a centre, so the sliders are met first and the in-between is kept smooth.
 * Measured: worst centre error 0.07 dB over flat, all +6, all +12, alternating ±12, bass boost, smile and
 * random curves at 44.1 and 48 kHz (see EqDesignTest). It runs only when a curve changes, in a few
 * microseconds.
 *
 * Bands too close to Nyquist for the rate (above 0.45 fs, only below 36 kHz) are left out.
 */
class EqDesign private constructor(val sampleRate: Int) {
    private val cosW0 = DoubleArray(EqBands.COUNT)
    private val sinW0 = DoubleArray(EqBands.COUNT)

    /** Which bands this rate can carry. */
    internal val active = BooleanArray(EqBands.COUNT)

    /** The points the solve looks at: band centres, then midpoints, both below 0.475 fs. */
    private val pointCos: DoubleArray

    /** For each point, its place on the band axis: 0 at 31 Hz, 1 at 62 Hz, 0.5 between them. */
    private val pointPosition: DoubleArray
    private val pointWeight: DoubleArray

    /** prototype[p][b]: band b's response at point p per dB of prototype gain. */
    private val prototype: Array<DoubleArray>

    /** Cosines of the grid [peakDb] searches: 20 Hz to the top of the band, log-spaced. */
    private val peakGridCos: DoubleArray

    init {
        for (b in 0 until EqBands.COUNT) {
            val f = designFrequency(b)
            cosW0[b] = cos(2 * PI * f / sampleRate)
            sinW0[b] = sin(2 * PI * f / sampleRate)
            active[b] = f < MAX_DESIGN_FRACTION * sampleRate
        }
        val positions = ArrayList<Double>()
        val weights = ArrayList<Double>()
        for (b in 0 until EqBands.COUNT) {
            positions += b.toDouble()
            weights += 1.0
            if (b < EqBands.COUNT - 1) {
                positions += b + 0.5
                weights += MIDPOINT_WEIGHT
            }
        }
        val keep = positions.indices.filter { frequencyAt(positions[it]) < MAX_POINT_FRACTION * sampleRate }
        pointPosition = DoubleArray(keep.size) { positions[keep[it]] }
        pointWeight = DoubleArray(keep.size) { weights[keep[it]] }
        pointCos = DoubleArray(keep.size) { cos(2 * PI * frequencyAt(pointPosition[it]) / sampleRate) }
        prototype = Array(keep.size) { p -> DoubleArray(EqBands.COUNT) { b -> if (active[b]) bandDb(b, PROTOTYPE_GAIN_DB, pointCos[p]) / PROTOTYPE_GAIN_DB else 0.0 } }
        val top = min(PEAK_GRID_TOP_HZ, PEAK_GRID_TOP_FRACTION * sampleRate)
        peakGridCos = DoubleArray(PEAK_GRID_POINTS) { cos(2 * PI * logSpaced(PEAK_GRID_BOTTOM_HZ, top, it, PEAK_GRID_POINTS) / sampleRate) }
    }

    /** Writes band [band]'s coefficients at [gainDb] into [out] at [at] (b0, b1, b2, a1, a2). Allocation-free. */
    internal fun coefficients(band: Int, gainDb: Double, out: DoubleArray, at: Int) {
        if (band == TREBLE_BAND) {
            Rbj.highShelf(cosW0[band], sinW0[band], SHELF_SLOPE, gainDb, out, at)
        } else {
            Rbj.peaking(cosW0[band], sinW0[band] / (2 * PEAKING_Q), gainDb, out, at)
        }
    }

    fun filter(band: Int, gainDb: Double): Biquad {
        val c = DoubleArray(5)
        coefficients(band, gainDb, c, 0)
        return Biquad(c[0], c[1], c[2], c[3], c[4])
    }

    /** The filter gains, in dB per band, that make the cascade meet [curve]'s sliders. Zero for a flat curve. */
    fun filterGainsDb(curve: EqCurve): DoubleArray {
        val gains = DoubleArray(EqBands.COUNT)
        if (curve.isFlat) return gains
        val targets = DoubleArray(pointPosition.size) { sliderAt(curve, pointPosition[it]) }
        val matrix = Array(pointPosition.size) { prototype[it].copyOf() }
        solveInto(matrix, targets, gains)
        val scratch = DoubleArray(5)
        repeat(ITERATIONS) {
            for (b in 0 until EqBands.COUNT) {
                if (!active[b] || abs(gains[b]) < MIN_ITERATION_GAIN_DB) {
                    for (p in matrix.indices) matrix[p][b] = prototype[p][b]
                    continue
                }
                coefficients(b, gains[b], scratch, 0)
                for (p in matrix.indices) matrix[p][b] = dbOf(scratch, pointCos[p]) / gains[b]
            }
            solveInto(matrix, targets, gains)
        }
        for (b in gains.indices) gains[b] = if (active[b]) gains[b].coerceIn(-MAX_FILTER_DB, MAX_FILTER_DB) else 0.0
        return gains
    }

    /** The cascade's response in dB at [frequencyHz] with filter gains [filterGainsDb]. */
    fun responseDb(filterGainsDb: DoubleArray, frequencyHz: Double): Double = cascade(filterGainsDb).dbAt(cos(2 * PI * frequencyHz / sampleRate))

    /** The filters for [filterGainsDb] with their coefficients worked out once, for evaluating at many frequencies. */
    internal fun cascade(filterGainsDb: DoubleArray): Cascade {
        val coeffs = DoubleArray(5 * EqBands.COUNT)
        var n = 0
        for (b in 0 until EqBands.COUNT) {
            if (!active[b] || filterGainsDb[b] == 0.0) continue
            coefficients(b, filterGainsDb[b], coeffs, 5 * n)
            n++
        }
        return Cascade(coeffs, n)
    }

    internal class Cascade(private val coeffs: DoubleArray, private val count: Int) {
        fun powerAt(cosW: Double): Double {
            var power = 1.0
            for (i in 0 until count) power *= magnitudeSquared(coeffs, cosW, 5 * i)
            return power
        }

        fun dbAt(cosW: Double): Double = 10 * log10(max(powerAt(cosW), 1e-30))
    }

    /** The highest point of the cascade's response, 20 Hz to 20 kHz (or near Nyquist below 44.1 kHz). */
    fun peakDb(filterGainsDb: DoubleArray): Double {
        if (filterGainsDb.all { it == 0.0 }) return 0.0
        val cascade = cascade(filterGainsDb)
        var best = 0.0
        for (c in peakGridCos) best = max(best, cascade.powerAt(c))
        return 10 * log10(max(best, 1e-30))
    }

    /** The gain put in front of the filters so a boost does not push full-scale audio over. Zero or negative. */
    fun preGainDb(filterGainsDb: DoubleArray): Double {
        val boost = max(0.0, peakDb(filterGainsDb))
        return if (boost == 0.0) 0.0 else -HEADROOM_COMPENSATION * boost
    }

    private fun bandDb(band: Int, gainDb: Double, cosW: Double): Double {
        val c = DoubleArray(5)
        coefficients(band, gainDb, c, 0)
        return dbOf(c, cosW)
    }

    /**
     * Weighted least squares through the normal equations, over the active bands only. Ten unknowns and at
     * most nineteen points, so Gaussian elimination with partial pivoting is plenty, in double.
     */
    private fun solveInto(matrix: Array<DoubleArray>, targets: DoubleArray, gains: DoubleArray) {
        val bands = (0 until EqBands.COUNT).filter { active[it] }
        val n = bands.size
        val a = Array(n) { DoubleArray(n + 1) }
        for (p in matrix.indices) {
            val w2 = pointWeight[p] * pointWeight[p]
            for (i in 0 until n) {
                val mi = matrix[p][bands[i]] * w2
                for (j in 0 until n) a[i][j] += mi * matrix[p][bands[j]]
                a[i][n] += mi * targets[p]
            }
        }
        for (col in 0 until n) {
            var pivot = col
            for (r in col + 1 until n) if (abs(a[r][col]) > abs(a[pivot][col])) pivot = r
            val tmp = a[col]; a[col] = a[pivot]; a[pivot] = tmp
            val d = a[col][col]
            if (abs(d) < 1e-12) continue
            for (r in col + 1 until n) {
                val f = a[r][col] / d
                if (f == 0.0) continue
                for (k in col..n) a[r][k] -= f * a[col][k]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var s = a[i][n]
            for (k in i + 1 until n) s -= a[i][k] * x[k]
            x[i] = if (abs(a[i][i]) < 1e-12) 0.0 else s / a[i][i]
        }
        gains.fill(0.0)
        for (i in 0 until n) gains[bands[i]] = x[i]
    }

    companion object {
        const val PEAKING_Q = 0.8
        const val TREBLE_BAND = 9

        /** Geometric midpoint of 8 and 16 kHz. */
        val TREBLE_SHELF_HZ = sqrt(8000.0 * 16000.0)

        /** The cookbook's shelf slope S; 1 is as steep as it goes without a bump. */
        const val SHELF_SLOPE = 1.0

        const val PROTOTYPE_GAIN_DB = 17.0
        const val ITERATIONS = 2
        const val MIDPOINT_WEIGHT = 0.25

        /**
         * How much of the curve's peak boost the pre-gain takes back: all of it. EqHeadroomTest runs the
         * presets over pink noise levelled to -1 dBFS peaks and over a saturated loud master peaking at
         * -0.1 dBFS (levelling off). Full: the limiter never acts. Three quarters: 1.4 dB louder, limiter on
         * up to 1.5% of a loud master's samples (0.16 dB at most). Half: on 85% of them, 1.5 dB deep, which
         * is pumping. The limiter has no look-ahead, so each time it acts it bends the waveform; keeping it a
         * safety net is worth the 3 to 5 dB a boost costs in level. A pure cut is never made louder.
         */
        const val HEADROOM_COMPENSATION = 1.0

        /** A guard on what the solve may ask of one filter; alternating ±12 dB sliders need about ±31. */
        const val MAX_FILTER_DB = 40.0

        private const val MIN_ITERATION_GAIN_DB = 0.01
        private const val MAX_DESIGN_FRACTION = 0.45
        private const val MAX_POINT_FRACTION = 0.475
        private const val PEAK_GRID_POINTS = 160
        private const val PEAK_GRID_BOTTOM_HZ = 20.0
        private const val PEAK_GRID_TOP_HZ = 20_000.0
        private const val PEAK_GRID_TOP_FRACTION = 0.48

        private val cache = ConcurrentHashMap<Int, EqDesign>()

        /** The design for [sampleRate], built once per rate and shared; it holds no mutable state. */
        fun forRate(sampleRate: Int): EqDesign {
            require(sampleRate > 0) { "Sample rate $sampleRate" }
            return cache.getOrPut(sampleRate) { EqDesign(sampleRate) }
        }

        private fun designFrequency(band: Int): Double = if (band == TREBLE_BAND) TREBLE_SHELF_HZ else EqBands.frequenciesHz[band]

        /** The frequency at a place on the band axis; bands are octaves, so it is 31.25 · 2^position. */
        private fun frequencyAt(position: Double): Double = EqBands.frequenciesHz[0] * Math.pow(2.0, position)

        /** The slider line at a place on the band axis: the slider itself at a centre, the mean of two between. */
        private fun sliderAt(curve: EqCurve, position: Double): Double {
            val low = position.toInt()
            val frac = position - low
            if (frac == 0.0 || low >= EqBands.COUNT - 1) return curve[low].toDouble()
            return curve[low] * (1 - frac) + curve[low + 1] * frac
        }

        internal fun logSpaced(from: Double, to: Double, index: Int, count: Int): Double =
            if (count == 1) from else exp(ln(from) + (ln(to) - ln(from)) * index / (count - 1))

        internal fun magnitudeSquared(c: DoubleArray, cosW: Double, at: Int = 0): Double {
            val b0 = c[at]; val b1 = c[at + 1]; val b2 = c[at + 2]; val a1 = c[at + 3]; val a2 = c[at + 4]
            val cos2W = 2 * cosW * cosW - 1
            val num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * cosW + 2 * b0 * b2 * cos2W
            val den = 1 + a1 * a1 + a2 * a2 + 2 * (a1 + a1 * a2) * cosW + 2 * a2 * cos2W
            return num / den
        }

        private fun dbOf(c: DoubleArray, cosW: Double): Double = 10 * log10(max(magnitudeSquared(c, cosW), 1e-30))
    }
}
