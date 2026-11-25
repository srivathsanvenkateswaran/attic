package app.attic.player.playback.eq

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The equaliser's ten octave bands. The centres are exact octaves of 1 kHz, so each band sits an octave
 * from its neighbours and the interaction maths in [EqDesign] works on an even grid; the labels are the
 * rounded figures people expect to read under the sliders.
 */
object EqBands {
    const val COUNT = 10

    val frequenciesHz: List<Double> = listOf(31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

    val labels: List<String> = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    const val MIN_DB = -12f
    const val MAX_DB = 12f

    /** The slider step. Presets sit on it too, so nudging a preset's slider never leaves the grid. */
    const val STEP_DB = 0.5f
}

/**
 * Ten slider gains in dB, one per [EqBands] band. Immutable and compared by value, so it can key maps and
 * be handed between threads freely. Negative zero is stored as zero, so a curve that went down and back
 * up still equals [FLAT].
 */
class EqCurve private constructor(private val gains: FloatArray) {
    constructor(gainsDb: List<Float>) : this(gainsDb.toFloatArray().also { require(it.size == EqBands.COUNT) { "An EqCurve has ${EqBands.COUNT} bands, not ${it.size}" } })

    init {
        for (i in gains.indices) gains[i] = gains[i] + 0f
    }

    val gainsDb: List<Float> get() = gains.toList()

    operator fun get(band: Int): Float = gains[band]

    /** True when every band is at 0 dB: the equaliser then copies audio straight through. */
    val isFlat: Boolean get() = gains.all { it == 0f }

    /** The same curve with every gain inside [EqBands.MIN_DB]..[EqBands.MAX_DB]; NaN and infinities become 0 dB. */
    fun coerced(): EqCurve {
        if (gains.all { it.isFinite() && it in EqBands.MIN_DB..EqBands.MAX_DB }) return this
        return EqCurve(FloatArray(EqBands.COUNT) { if (gains[it].isFinite()) gains[it].coerceIn(EqBands.MIN_DB, EqBands.MAX_DB) else 0f })
    }

    /** A copy with one band changed. */
    fun with(band: Int, gainDb: Float): EqCurve = EqCurve(gains.copyOf().also { it[band] = gainDb })

    /** Whether every band is within [toleranceDb] of [other]'s. */
    fun matches(other: EqCurve, toleranceDb: Float): Boolean = gains.indices.all { abs(gains[it] - other.gains[it]) <= toleranceDb }

    /** Compact text for storage, such as `"0,1.5,-3,0,0,0,0,0,2.25,6"`. Gains keep two decimals at most. */
    fun encode(): String = gains.joinToString(",") { format(it) }

    override fun equals(other: Any?): Boolean = other is EqCurve && gains.contentEquals(other.gains)

    override fun hashCode(): Int = gains.contentHashCode()

    override fun toString(): String = "EqCurve(${encode()})"

    companion object {
        val FLAT = EqCurve(FloatArray(EqBands.COUNT))

        fun of(vararg gainsDb: Float): EqCurve {
            require(gainsDb.size == EqBands.COUNT) { "An EqCurve has ${EqBands.COUNT} bands, not ${gainsDb.size}" }
            return EqCurve(gainsDb.copyOf())
        }

        /**
         * Reads what [encode] wrote. Anything that is not ten comma-separated finite numbers gives null, so a
         * damaged row falls back to no equaliser instead of crashing playback. Values out of range are clamped.
         */
        fun decode(text: String?): EqCurve? {
            val parts = text?.split(',') ?: return null
            if (parts.size != EqBands.COUNT) return null
            val values = FloatArray(EqBands.COUNT)
            for (i in parts.indices) {
                val v = parts[i].trim().toFloatOrNull() ?: return null
                if (!v.isFinite()) return null
                values[i] = v
            }
            return EqCurve(values).coerced()
        }

        private fun format(value: Float): String {
            val hundredths = (value * 100f).roundToInt()
            if (hundredths % 100 == 0) return (hundredths / 100).toString()
            return String.format(Locale.ROOT, "%.2f", hundredths / 100.0).trimEnd('0')
        }
    }
}
