package app.attic.player.playback.eq

import kotlin.math.roundToInt

/** A named curve. Built-in presets have the stable [id]s below; the UI looks their names up by id. */
data class EqPreset(val id: String, val curve: EqCurve)

/**
 * The built-in presets: the graphic-equaliser presets that have shipped with iTunes and the Mac's Music
 * app for years, so their shapes are ones people know by name. The values are the ones in that app's
 * preset file (its bands sit at 32, 64, 125 Hz … 16 kHz, the same octaves as [EqBands]), rounded to
 * the nearest [EqBands.STEP_DB] so every preset sits on the slider grid. Bass boost, Bass reducer,
 * Treble boost and Vocal are its "Increase Bass", "Reduce Bass", "Increase Treble" and "Increase
 * Vocals"; Spoken word is its "Spoken Word". Ids are stored in the
 * database, so they never change.
 */
object EqPresets {
    const val FLAT = "flat"
    const val BASS_BOOST = "bass_boost"
    const val BASS_REDUCER = "bass_reducer"
    const val TREBLE_BOOST = "treble_boost"
    const val VOCAL = "vocal"
    const val LOUDNESS = "loudness"
    const val ACOUSTIC = "acoustic"
    const val ELECTRONIC = "electronic"
    const val HIP_HOP = "hip_hop"
    const val CLASSICAL = "classical"
    const val SPOKEN_WORD = "spoken_word"

    /** The match tolerance: well under the slider step, above float noise from storage. */
    const val MATCH_TOLERANCE_DB = 0.05f

    val all: List<EqPreset> = listOf(
        // Bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k.
        preset(FLAT, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preset(BASS_BOOST, 5.5f, 4.25f, 3.5f, 2.5f, 1.25f, 0f, 0f, 0f, 0f, 0f),
        preset(BASS_REDUCER, -5.5f, -4.25f, -3.5f, -2.5f, -1.25f, 0f, 0f, 0f, 0f, 0f),
        preset(TREBLE_BOOST, 0f, 0f, 0f, 0f, 0f, 1.25f, 2.5f, 3.5f, 4.25f, 5.5f),
        preset(VOCAL, -1.5f, -3f, -3f, 1.5f, 3.75f, 3.75f, 3f, 1.5f, 0f, -1.5f),
        preset(LOUDNESS, 6f, 4f, 0f, 0f, -2f, 0f, -1f, -5f, 5f, 1f),
        preset(ACOUSTIC, 5f, 4.9f, 3.95f, 1.05f, 2.15f, 1.75f, 3.5f, 4.1f, 3.55f, 2.15f),
        preset(ELECTRONIC, 4.25f, 3.8f, 1.2f, 0f, -2.15f, 2.25f, 0.85f, 1.25f, 3.95f, 4.8f),
        preset(HIP_HOP, 5f, 4.25f, 1.5f, 3f, -1f, -1f, 1.5f, -0.5f, 2f, 3f),
        preset(CLASSICAL, 4.75f, 3.75f, 3f, 2.5f, -1.5f, -1.5f, 0f, 2.25f, 3.25f, 3.75f),
        preset(SPOKEN_WORD, -3.46f, -0.47f, 0f, 0.69f, 3.46f, 4.61f, 4.84f, 4.28f, 2.54f, 0f),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): EqPreset? = byId[id]

    /**
     * The preset [curve] is, built-ins first and then [userPresets] in order, or null when it is a custom
     * curve. Each band must be within [MATCH_TOLERANCE_DB].
     */
    fun match(curve: EqCurve, userPresets: List<EqPreset> = emptyList()): EqPreset? =
        all.firstOrNull { it.curve.matches(curve, MATCH_TOLERANCE_DB) } ?: userPresets.firstOrNull { it.curve.matches(curve, MATCH_TOLERANCE_DB) }

    private fun preset(id: String, vararg gainsDb: Float) = EqPreset(id, EqCurve.of(*FloatArray(gainsDb.size) { onGrid(gainsDb[it]) }))

    private fun onGrid(gainDb: Float): Float = (gainDb / EqBands.STEP_DB).roundToInt() * EqBands.STEP_DB
}
