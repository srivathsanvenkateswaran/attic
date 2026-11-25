package app.attic.player.analysis

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * A song's mood scores: what Essentia's classifier heads say about its embedding, one number per
 * output, in a fixed order. The first eight are single heads ([HAPPY] to [ENGAGEMENT]); the 56 after
 * [THEME_START] are the moodtheme head's tags, in [THEME_TAGS] order. A missing value is NaN: a song
 * scored without its embedding's length has no tags and no engagement (docs/features/mood-mixes.md).
 */
object MoodSignals {
    const val HAPPY = 0
    const val SAD = 1
    const val RELAXED = 2
    const val AGGRESSIVE = 3
    const val PARTY = 4
    const val DANCEABLE = 5
    const val INSTRUMENTAL = 6
    const val ENGAGEMENT = 7
    const val THEME_START = 8

    /** The moodtheme head's tags, in the order of its output (its metadata file's `classes`). */
    val THEME_TAGS = listOf(
        "action", "adventure", "advertising", "background", "ballad", "calm", "children", "christmas", "commercial", "cool",
        "corporate", "dark", "deep", "documentary", "drama", "dramatic", "dream", "emotional", "energetic", "epic", "fast",
        "film", "fun", "funny", "game", "groovy", "happy", "heavy", "holiday", "hopeful", "inspiring", "love", "meditative",
        "melancholic", "melodic", "motivational", "movie", "nature", "party", "positive", "powerful", "relaxing", "retro",
        "romantic", "sad", "sexy", "slow", "soft", "soundscape", "space", "sport", "summer", "trailer", "travel", "upbeat",
        "uplifting",
    )

    val SIZE = THEME_START + THEME_TAGS.size

    /** The signal index of a moodtheme tag. */
    fun tag(name: String): Int {
        val index = THEME_TAGS.indexOf(name)
        require(index >= 0) { "No moodtheme tag $name" }
        return THEME_START + index
    }

    private const val MISSING = 0xFFFF
    private const val SCALE = 65_534f

    /** Two bytes a value, little-endian, as a fraction of 1; 0xFFFF for a missing one. */
    fun encode(signals: FloatArray): ByteArray {
        require(signals.size == SIZE) { "Expected $SIZE signals, got ${signals.size}" }
        val buffer = ByteBuffer.allocate(SIZE * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (value in signals) {
            buffer.putShort((if (value.isNaN()) MISSING else (value.coerceIn(0f, 1f) * SCALE).roundToInt()).toShort())
        }
        return buffer.array()
    }

    /** Always [SIZE] values; a short blob leaves the rest missing. */
    fun decode(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(SIZE) { i ->
            if (i >= bytes.size / 2) return@FloatArray Float.NaN
            val raw = buffer.short.toInt() and 0xFFFF
            if (raw == MISSING) Float.NaN else raw / SCALE
        }
    }
}
