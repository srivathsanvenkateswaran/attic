package app.attic.player.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * When a crossfade happens and how long it lasts, kept free of the players so it can be tested.
 * [CrossfadeController] asks [fadeMs] for the song now playing, then [step] for what to do at the
 * current position.
 */
object CrossfadePlan {
    /** The lengths offered in Settings, in seconds. 0 is off. */
    val choices = listOf(0, 2, 4, 6, 8, 10, 12)

    /** The tail player starts this long before the fade, to line up with the main one while silent. */
    const val ARM_LEAD_MS = 5_000L

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

    fun step(positionMs: Long, durationMs: Long, fadeMs: Long): Step {
        val untilFade = durationMs - fadeMs - positionMs
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
 * jump about, so it takes the median of several, and corrects the seek it will use next time by
 * however far the last one landed out: a seek lands late by roughly the time the decoder and the
 * audio output take to start, and that varies by phone and file.
 */
class TailAlignment(
    private val toleranceMs: Long = 20,
    private val readingsNeeded: Int = 5,
) {
    /** How far ahead of the main player's position to seek the tail. Learned as it goes. */
    var leadMs: Long = 80
        private set

    private val readings = ArrayList<Long>()

    sealed interface Verdict {
        data object NeedMore : Verdict

        data object Aligned : Verdict

        /** Seek the tail again, [leadMs] ahead of the main player. */
        data object Reseek : Verdict
    }

    fun seekTarget(mainPositionMs: Long): Long = mainPositionMs + leadMs

    /** Starts a new set of readings after a seek. */
    fun reset() = readings.clear()

    /** [differenceMs] is the tail's position minus the main player's, read at the same moment. */
    fun record(differenceMs: Long): Verdict {
        readings += differenceMs
        if (readings.size < readingsNeeded) return Verdict.NeedMore
        val median = readings.sorted()[readings.size / 2]
        readings.clear()
        if (abs(median) <= toleranceMs) return Verdict.Aligned
        leadMs = (leadMs - median).coerceIn(-MAX_LEAD_MS, MAX_LEAD_MS)
        return Verdict.Reseek
    }

    private companion object {
        const val MAX_LEAD_MS = 2_000L
    }
}
