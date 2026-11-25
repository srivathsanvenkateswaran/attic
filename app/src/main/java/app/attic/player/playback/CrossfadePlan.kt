package app.attic.player.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The fixed crossfade's length and the timing every transition shares, kept free of the players so it
 * can be tested. [TransitionPlanner] uses [fadeMs] for the Crossfade setting and for Smart without
 * analysis; [CrossfadeController] asks [step] what to do at the current position.
 */
object CrossfadePlan {
    /**
     * The tail player starts this long before the fade, to line up with the main one while silent: a
     * second for positions to settle after its seek, 750 ms of readings, a nudge of up to five seconds
     * ([TailAlignment.Verdict.Nudge]), then a second at normal speed and another set of readings.
     */
    const val ARM_LEAD_MS = 10_000L

    /** With less than this left before the fade there isn't time to line up, so the song just ends. */
    const val MIN_ARM_MS = 1_500L

    /** A fade shorter than this sounds like a glitch rather than a blend. */
    const val MIN_FADE_MS = 1_000L

    /**
     * The fade into the next song, in ms, or 0 for none. None when the setting is off, there is no
     * next song, repeat-one is on, the sleep timer stops at the end of this song, or the two are
     * consecutive songs of one album, which stay gapless. A fade is never more than a third of
     * either song, so a short interlude isn't swallowed by it.
     */
    fun fadeMs(
        settingSeconds: Int,
        current: LevelTrack?,
        next: LevelTrack?,
        currentDurationMs: Long,
        nextDurationMs: Long?,
        repeatOne: Boolean,
        stopsAfterThisSong: Boolean,
    ): Long {
        if (settingSeconds <= 0 || next == null || repeatOne || stopsAfterThisSong) return 0
        if (currentDurationMs <= 0) return 0
        if (AlbumRun.follows(current, next)) return 0
        var fade = settingSeconds * 1000L
        fade = minOf(fade, currentDurationMs / 3)
        if (nextDurationMs != null && nextDurationMs > 0) fade = minOf(fade, nextDurationMs / 3)
        return if (fade < MIN_FADE_MS) 0 else fade
    }

    sealed interface Step {
        /** Nothing to do for [ms]; then ask again. */
        data class Wait(val ms: Long) : Step

        /** Start the tail player now. */
        data object Arm : Step

        /** The fade is due, or nearly, with no time to line up: let the song end as it would. */
        data object TooLate : Step
    }

    /** What to do at [positionMs] for a fade that starts at [fadeStartMs]. */
    fun step(positionMs: Long, fadeStartMs: Long): Step {
        val untilFade = fadeStartMs - positionMs
        return when {
            untilFade < MIN_ARM_MS -> Step.TooLate
            untilFade > ARM_LEAD_MS -> Step.Wait(untilFade - ARM_LEAD_MS)
            else -> Step.Arm
        }
    }

    /** Equal-power curves for two unrelated songs: the outgoing one at [progress] 0..1. */
    fun outgoingVolume(progress: Float): Float = cos(progress.coerceIn(0f, 1f) * PI / 2).toFloat()

    fun incomingVolume(progress: Float): Float = sin(progress.coerceIn(0f, 1f) * PI / 2).toFloat()
}

/**
 * Lines the tail player up with the main one. Position readings from a player that has just started
 * jump about, so it takes the median of several. A seek can't do the fine work: on the owner's phone
 * the same seek landed anywhere from 140 to 450 ms late, as the decoder and the audio output took more
 * or less time to start. So the tail is seeked once, close, and then played slightly slow or fast,
 * while it is still silent, until it is level with the main player ([Verdict.Nudge]). It is back at
 * normal speed well before anyone hears it. Each first reading after a seek also corrects the lead
 * the next seek uses.
 */
class TailAlignment(
    private val toleranceMs: Long = 20,
    /**
     * On the owner's phone, readings taken 50 ms apart flipped between the true offset and outliers
     * about 270 ms to one side, and up to a third of them were outliers. A median of fifteen (750 ms of
     * readings) follows the true offset; a median of five often landed on the outliers.
     */
    private val readingsNeeded: Int = 15,
) {
    /** How far ahead of the main player's position to seek the tail. Learned as it goes. */
    var leadMs: Long = 80
        private set

    /** The median of the last full set of readings, for the log. */
    var lastMedianMs: Long = 0
        private set

    private val readings = ArrayList<Long>()
    private var afterSeek = true

    sealed interface Verdict {
        data object NeedMore : Verdict

        data object Aligned : Verdict

        /** Too far out to nudge: seek the tail again, [leadMs] ahead of the main player. */
        data object Reseek : Verdict

        /** Play the silent tail at [speed] for [forMs], then at normal speed, then measure again. */
        data class Nudge(val speed: Float, val forMs: Long) : Verdict
    }

    fun seekTarget(mainPositionMs: Long): Long = mainPositionMs + leadMs

    /** Starts a new set of readings after a seek. */
    fun reset() {
        readings.clear()
        afterSeek = true
    }

    /** Starts a new set of readings after a nudge, with no seek in between. */
    fun remeasure() {
        readings.clear()
        afterSeek = false
    }

    /** [differenceMs] is the tail's position minus the main player's, read at the same moment. */
    fun record(differenceMs: Long): Verdict {
        readings += differenceMs
        if (readings.size < readingsNeeded) return Verdict.NeedMore
        val median = readings.sorted()[readings.size / 2]
        lastMedianMs = median
        readings.clear()
        // Only a reading straight after a seek says how far a seek lands out.
        if (afterSeek) leadMs = (leadMs - median).coerceIn(-MAX_LEAD_MS, MAX_LEAD_MS)
        afterSeek = false
        return when {
            abs(median) <= toleranceMs -> Verdict.Aligned
            abs(median) > MAX_NUDGE_MS -> Verdict.Reseek
            // A tail ahead (positive) plays slow to let the main player catch up; one behind plays fast.
            else -> Verdict.Nudge(
                speed = if (median > 0) 1f - NUDGE_RATE else 1f + NUDGE_RATE,
                forMs = (abs(median) / NUDGE_RATE).toLong(),
            )
        }
    }

    companion object {
        private const val MAX_LEAD_MS = 2_000L

        /** How much faster or slower than normal the silent tail plays while it catches up. */
        const val NUDGE_RATE = 0.08f

        /** Further out than this a nudge would take more than five seconds, so the tail seeks instead. */
        const val MAX_NUDGE_MS = 400L
    }
}
