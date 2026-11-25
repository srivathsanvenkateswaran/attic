package app.attic.player.playback

import app.attic.player.analysis.BeatTracker
import app.attic.player.analysis.EnergyCurve
import app.attic.player.analysis.SongTransitions
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** What happens between two songs. Saved as its name. */
enum class TransitionMode {
    /** The next song starts when this one ends. */
    Off,

    /** A fixed fade over the crossfade length. */
    Crossfade,

    /** Planned from each song's analysis, falling back to [Crossfade] for songs without it. */
    Smart,
}

/**
 * One planned move from the song playing into the next. [fadeStartMs] is in the outgoing song, where
 * the tail takes it over; [incomingStartMs] is where the main player enters the next song. Over
 * [fadeMs] the two cross on equal-power curves.
 */
data class Transition(
    val fadeStartMs: Long,
    val fadeMs: Long,
    val incomingStartMs: Long,
    val kind: Kind,
    val beatAligned: Boolean = false,
    val outBpm: Double? = null,
    val inBpm: Double? = null,
    /** The worst the two beat grids drift apart over the overlap, from the gap between their tempos. */
    val expectedDriftMs: Long = 0,
) {
    enum class Kind {
        /** The Crossfade setting: the last [fadeMs] of the song. */
        Fixed,

        /** Planned from both songs' analysis, or the outgoing one's. */
        Smart,

        /** Smart, but the outgoing song has no usable analysis, so it fades like [Fixed]. */
        SmartFallback,
    }
}

/**
 * Plans transitions, kept free of the players so it can be tested. Smart planning searches overlaps of
 * a few fixed lengths and start points around the outgoing song's outro, scoring each by how the two
 * songs' level curves mix (see [smart]), then lines the beats up at the overlap's middle when the
 * tempos allow. It is arithmetic on a few hundred bytes, well under a millisecond.
 */
object TransitionPlanner {
    /** The crossfade lengths offered, in seconds. */
    val lengthChoices = listOf(2, 4, 6, 8, 10, 12)
    const val DEFAULT_LENGTH_SECONDS = 6

    /** Positions measured from a decode this far from the player's length aren't trusted for that song. */
    const val MAX_LENGTH_MISMATCH_MS = 1_500L

    private val LENGTHS_MS = longArrayOf(1_000, 1_500, 2_000, 3_000, 4_000, 6_000, 8_000, 10_000, 12_000)
    private const val STEP_MS = 500L
    private const val SAMPLE_MS = 250L
    private const val EARLIEST_BEFORE_OUTRO_MS = 4_000L
    private const val ENTRY_MARGIN_MS = 20L

    /** Under this mixed level (12 dB below the songs' reference) the listener is hearing dead air. */
    private val DEAD_POWER = 10.0.pow(-12.0 / 10)
    private const val W_CLASH = 2.0
    private const val W_LOST = 3.0
    private const val W_DEAD = 1.5

    /** How long after the outgoing song's end every candidate is scored, so each is judged on the same stretch. */
    private const val AFTER_END_MS = 6_000L

    /** Tempos further apart than this, after folding half and double time, are never lined up. */
    const val MAX_TEMPO_GAP = 0.06

    /** The most the beats may drift apart at either end of an aligned overlap. */
    const val DRIFT_BUDGET_MS = 40.0
    const val MIN_ALIGNED_MS = 1_500L

    /** An aligned overlap may cost this much more than the best unaligned one and still be chosen. */
    private const val ALIGNED_COST_SLACK = 1.3
    private const val ALIGNED_COST_EXTRA = 0.2

    /**
     * Null for no transition: the mode is off, there is no next song, repeat-one is on, the sleep timer
     * stops at the end of this song, or the two are consecutive songs of one album, which stay gapless.
     */
    fun plan(
        mode: TransitionMode,
        lengthSeconds: Int,
        current: LevelTrack?,
        next: LevelTrack?,
        currentDurationMs: Long,
        nextDurationMs: Long?,
        repeatOne: Boolean,
        stopsAfterThisSong: Boolean,
        outgoing: SongTransitions?,
        incoming: SongTransitions?,
    ): Transition? {
        if (mode == TransitionMode.Off) return null
        val fixed = CrossfadePlan.fadeMs(lengthSeconds, current, next, currentDurationMs, nextDurationMs, repeatOne, stopsAfterThisSong)
        if (mode == TransitionMode.Crossfade) {
            return if (fixed <= 0) null else Transition(currentDurationMs - fixed, fixed, 0, Transition.Kind.Fixed)
        }
        // Smart turns off for the same reasons a fixed fade does; only its length differs.
        if (next == null || repeatOne || stopsAfterThisSong || currentDurationMs <= 0 || AlbumRun.follows(current, next)) return null
        val out = outgoing?.takeIf { trusted(it, currentDurationMs) }
        val into = incoming?.takeIf { nextDurationMs == null || nextDurationMs <= 0 || trusted(it, nextDurationMs) }
        if (out == null) {
            return if (fixed <= 0) null else Transition(currentDurationMs - fixed, fixed, 0, Transition.Kind.SmartFallback)
        }
        return smart(out, into, currentDurationMs, nextDurationMs)
    }

    private fun trusted(t: SongTransitions, durationMs: Long) = abs(t.decodedMs - durationMs) <= MAX_LENGTH_MISMATCH_MS

    /**
     * The overlap with the lowest cost. Every candidate is scored over the same stretch of listening, from
     * the earliest start considered to a few seconds after the outgoing song's end, with the two level
     * curves mixed through the equal-power ramps:
     * - clash: both songs loud at once, the power of whichever is quieter at each moment;
     * - lost: music the fade takes away, from the end of the outgoing song and the start of the incoming
     *   one, and all of the outgoing song after the overlap;
     * - dead air: what the listener hears (the outgoing tail before the overlap, the mix, then the incoming
     *   song) more than 12 dB under the reference.
     * Ties go to the shorter overlap, then the later start.
     */
    private fun smart(out: SongTransitions, into: SongTransitions?, durationMs: Long, nextDurationMs: Long?): Transition? {
        val end = min(out.audioEndMs, durationMs)
        val entry = into?.let { max(0, it.audioStartMs - ENTRY_MARGIN_MS) } ?: 0L
        val nextLength = nextDurationMs?.takeIf { it > 0 }
        val maxLength = listOfNotNull(durationMs / 3, nextLength?.let { it / 3 }, nextLength?.let { it - entry }).min()
        val earliest = max(out.audioStartMs, out.outroStartMs - EARLIEST_BEFORE_OUTRO_MS)
        // The scoring grid, laid so the song's end falls on it.
        val windowStart = end - ((end - min(earliest, end - LENGTHS_MS[0]) + SAMPLE_MS - 1) / SAMPLE_MS) * SAMPLE_MS
        val samples = ((end + AFTER_END_MS - windowStart) / SAMPLE_MS).toInt()
        val outPower = DoubleArray(samples) { outgoingPower(out, windowStart + it * SAMPLE_MS + SAMPLE_MS / 2) }
        val inPower = DoubleArray(samples + 1) { incomingPower(into, entry + it * SAMPLE_MS + SAMPLE_MS / 2) }
        val scorer = Scorer(outPower, inPower, ((end - windowStart) / SAMPLE_MS).toInt())

        fun search(longest: Long): Candidate? {
            var best: Candidate? = null
            for (length in LENGTHS_MS) {
                if (length > longest) break
                val latest = end - length
                if (latest < out.audioStartMs) break
                val steps = (length / SAMPLE_MS).toInt()
                val gainOut = DoubleArray(steps) { cos((it + 0.5) / steps * PI / 2).let { c -> c * c } }
                var start = latest - ((latest - max(windowStart, min(earliest, latest))) / STEP_MS) * STEP_MS
                while (start <= latest) {
                    val cost = scorer.cost(((start - windowStart) / SAMPLE_MS).toInt(), gainOut)
                    val candidate = Candidate(start, length, cost)
                    if (best == null || candidate.beats(best)) best = candidate
                    start += STEP_MS
                }
            }
            return best
        }

        val chosen = search(maxLength) ?: return null
        val plain = Transition(chosen.start, chosen.length, entry, Transition.Kind.Smart, outBpm = out.outro?.bpm, inBpm = into?.intro?.bpm)
        // Lining the beats up limits the overlap to what the tempo gap allows. The best overlap within that
        // limit is used if it costs little more than the best overall; otherwise the beats stay as they fall.
        val budget = alignableLength(out, into) ?: return plain
        if (budget >= chosen.length) return align(plain, out, into, end, nextLength) ?: plain
        val within = search(min(maxLength, budget)) ?: return plain
        if (within.cost > chosen.cost * ALIGNED_COST_SLACK + ALIGNED_COST_EXTRA) return plain
        val shorter = plain.copy(fadeStartMs = within.start, fadeMs = within.length)
        return align(shorter, out, into, end, nextLength) ?: plain
    }

    /** The longest overlap whose beats stay within [DRIFT_BUDGET_MS] once lined up, or null if they can't be. */
    private fun alignableLength(out: SongTransitions, into: SongTransitions?): Long? {
        val ratio = tempoRatio(out.outro, into?.intro) ?: return null
        val gap = abs(1 - ratio)
        if (gap > MAX_TEMPO_GAP) return null
        val length = if (gap < 1e-6) Long.MAX_VALUE else (2 * DRIFT_BUDGET_MS / gap).toLong()
        return length.takeIf { it >= MIN_ALIGNED_MS }
    }

    private class Candidate(val start: Long, val length: Long, val cost: Double) {
        fun beats(other: Candidate): Boolean = when {
            cost < other.cost - 1e-9 -> true
            cost > other.cost + 1e-9 -> false
            length != other.length -> length < other.length
            else -> start > other.start
        }
    }

    /**
     * Scores candidates on a 250 ms grid. [outPower] is the outgoing song's power at each grid step (none
     * after [endIndex]); [inPower] the incoming song's at each step after it starts.
     */
    private class Scorer(private val outPower: DoubleArray, private val inPower: DoubleArray, private val endIndex: Int) {
        fun cost(startIndex: Int, gainOut: DoubleArray): Double {
            val dt = SAMPLE_MS / 1000.0
            val steps = gainOut.size
            var clash = 0.0
            var lost = 0.0
            var dead = 0.0
            for (i in outPower.indices) {
                val po = outPower[i]
                val k = i - startIndex
                when {
                    k < 0 -> dead += deficit(po)
                    k < steps -> {
                        val go = gainOut[k]
                        val gi = 1 - go
                        val pi = inPower[k]
                        clash += min(po * go, pi * gi)
                        lost += po * gi + pi * go
                        dead += deficit(po * go + pi * gi)
                    }
                    else -> {
                        if (i < endIndex) lost += po
                        dead += deficit(inPower[min(k, inPower.size - 1)])
                    }
                }
            }
            return (W_CLASH * clash + W_LOST * lost + W_DEAD * dead) * dt
        }

        private fun deficit(power: Double) = if (power >= DEAD_POWER) 0.0 else (DEAD_POWER - power) / DEAD_POWER
    }

    /** The outgoing song's power relative to its reference at [t]: full before its last 30 s, nothing after its end. */
    private fun outgoingPower(out: SongTransitions, t: Long): Double {
        if (t >= out.audioEndMs) return 0.0
        val curveStart = out.audioEndMs - EnergyCurve.BLOCKS * EnergyCurve.BLOCK_MS
        if (t < curveStart) return 1.0
        return power(out.outroEnergy, ((t - curveStart) / EnergyCurve.BLOCK_MS).toInt())
    }

    /** The incoming song's power at its own position [u]. Without analysis it is taken as full from the start. */
    private fun incomingPower(into: SongTransitions?, u: Long): Double {
        if (into == null) return 1.0
        if (u < into.audioStartMs) return 0.0
        val block = ((u - into.audioStartMs) / EnergyCurve.BLOCK_MS).toInt()
        if (block >= EnergyCurve.BLOCKS) return 1.0
        return power(into.introEnergy, block)
    }

    private fun power(curve: ByteArray, block: Int): Double {
        if (block !in curve.indices) return 1.0
        return 10.0.pow(EnergyCurve.decode(curve[block]) / 10)
    }

    /** The tempo ratio after folding half and double time into 0.5..2 around 1, or null if both aren't confident. */
    fun tempoRatio(outBeat: SongTransitions.Beat?, inBeat: SongTransitions.Beat?): Double? {
        if (outBeat == null || inBeat == null) return null
        if (outBeat.confidence < BeatTracker.MIN_CONFIDENCE || inBeat.confidence < BeatTracker.MIN_CONFIDENCE) return null
        var ratio = inBeat.bpm / outBeat.bpm
        while (ratio > 1.5) ratio /= 2
        while (ratio < 0.75) ratio *= 2
        return ratio
    }

    /**
     * Moves the overlap so a beat of each song falls on its middle: each end moves by at most half a beat.
     * Null when the tempos don't allow it or the overlap is longer than the drift budget.
     */
    private fun align(plan: Transition, out: SongTransitions, into: SongTransitions?, end: Long, nextLength: Long?): Transition? {
        val outBeat = out.outro ?: return null
        val inBeat = into?.intro ?: return null
        val ratio = tempoRatio(outBeat, inBeat) ?: return null
        val gap = abs(1 - ratio)
        if (gap > MAX_TEMPO_GAP) return null
        val budgetLength = if (gap < 1e-6) Long.MAX_VALUE else (2 * DRIFT_BUDGET_MS / gap).toLong()
        val length = plan.fadeMs
        if (length > budgetLength || length < MIN_ALIGNED_MS) return null
        val half = length / 2.0

        val outPeriod = outBeat.periodMs
        val inPeriod = inBeat.periodMs
        val middleOut = plan.fadeStartMs + plan.fadeMs / 2.0
        val middleIn = plan.incomingStartMs + plan.fadeMs / 2.0
        var beatOut = outBeat.beatMs + Math.round((middleOut - outBeat.beatMs) / outPeriod) * outPeriod
        var beatIn = inBeat.beatMs + Math.round((middleIn - inBeat.beatMs) / inPeriod) * inPeriod
        while (beatOut + half > end) beatOut -= outPeriod
        while (beatIn - half < 0) beatIn += inPeriod
        val fadeStart = floor(beatOut - half).toLong()
        val entry = floor(beatIn - half).toLong()
        if (fadeStart < out.audioStartMs || fadeStart + length > end) return null
        if (nextLength != null && entry + length > nextLength) return null
        return plan.copy(
            fadeStartMs = fadeStart,
            fadeMs = length,
            incomingStartMs = entry,
            beatAligned = true,
            expectedDriftMs = (half * gap).roundToLong(),
        )
    }
}
