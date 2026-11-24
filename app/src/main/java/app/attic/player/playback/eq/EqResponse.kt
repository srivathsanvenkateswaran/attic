package app.attic.player.playback.eq

import kotlin.math.PI
import kotlin.math.cos

/**
 * The equaliser's real frequency response for drawing under the sliders: what the filters do after
 * compensation, not a line joining the knobs, so the screen shows the dips and overshoots the ear hears.
 * [gainsDb] leaves out the pre-gain, which is in [preGainDb], so the drawing sits on 0 dB where the
 * curve is flat.
 *
 * Cheap enough for every slider move: about 20 µs for 128 points on a desktop JVM (EqResponseTest
 * prints the figure); a phone core is a few times slower, still far inside one frame.
 */
class EqResponse(val frequenciesHz: FloatArray, val gainsDb: FloatArray, val preGainDb: Float) {
    companion object {
        const val DEFAULT_POINTS = 128

        fun of(curve: EqCurve, sampleRate: Int = 48_000, points: Int = DEFAULT_POINTS, fromHz: Double = 20.0, toHz: Double = 20_000.0): EqResponse {
            require(points >= 2) { "Need at least two points" }
            val design = EqDesign.forRate(sampleRate)
            val filterGains = design.filterGainsDb(curve)
            val frequencies = FloatArray(points)
            val gains = FloatArray(points)
            val cascade = design.cascade(filterGains)
            for (i in 0 until points) {
                val f = EqDesign.logSpaced(fromHz, toHz, i, points)
                frequencies[i] = f.toFloat()
                gains[i] = cascade.dbAt(cos(2 * PI * f / sampleRate)).toFloat()
            }
            return EqResponse(frequencies, gains, design.preGainDb(filterGains).toFloat())
        }
    }
}
