package app.attic.player.playback

/** The sleep timer's choices, in the order the menu lists them. [minutes] is what the session command carries. */
enum class SleepChoice(val minutes: Int) {
    Minutes15(15),
    Minutes30(30),
    Minutes45(45),
    Minutes60(60),
    EndOfSong(END_OF_SONG),
    Off(0),
    ;

    companion object {
        fun of(minutes: Int): SleepChoice = entries.firstOrNull { it.minutes == minutes } ?: Off
    }
}

private const val END_OF_SONG = -1

/**
 * What the UI shows of the timer. [endsAtElapsedMs] is on the `SystemClock.elapsedRealtime` clock and
 * is set only for a countdown.
 */
data class SleepTimerStatus(val choice: SleepChoice = SleepChoice.Off, val endsAtElapsedMs: Long? = null) {
    fun remainingMs(nowElapsedMs: Long): Long? = endsAtElapsedMs?.let { (it - nowElapsedMs).coerceAtLeast(0) }
}

/**
 * The sleep timer's arithmetic, kept free of the player so it can be tested with a fake [clock].
 * [PlaybackService] calls [step] on the delay it returns, sets the volume it returns, and pauses when
 * told to. The last [FADE_MS] fade the volume down to nothing, so the music doesn't stop mid-phrase.
 */
class SleepTimer(private val clock: () -> Long) {
    sealed interface Step {
        /** Keep playing at [volume], and call [step] again in [nextCheckMs]. */
        data class Play(val volume: Float, val nextCheckMs: Long) : Step

        /** Time is up: pause and put the volume back. The timer has switched itself off. */
        data object Pause : Step

        /** The timer is off. Nothing to do until it is set again. */
        data object Idle : Step
    }

    var choice: SleepChoice = SleepChoice.Off
        private set

    private var endsAt: Long? = null

    val status: SleepTimerStatus get() = SleepTimerStatus(choice, endsAt)

    fun set(choice: SleepChoice) {
        this.choice = choice
        endsAt = if (choice.minutes > 0) clock() + choice.minutes * 60_000L else null
    }

    /**
     * [songRemainingMs] is how much of the current song is left, null when its length is unknown. It
     * only matters for "End of this song", which fades on the song's own last seconds.
     */
    fun step(songRemainingMs: Long?): Step {
        val end = endsAt
        return when {
            choice == SleepChoice.Off -> Step.Idle
            choice == SleepChoice.EndOfSong -> {
                // The pause itself comes from the player stopping at the end of the song (songEnded).
                if (songRemainingMs == null) {
                    Step.Play(1f, SONG_CHECK_MS)
                } else {
                    Step.Play(volumeFor(songRemainingMs), if (songRemainingMs <= FADE_MS) FADE_TICK_MS else SONG_CHECK_MS)
                }
            }
            end == null -> Step.Idle
            else -> {
                val remaining = end - clock()
                if (remaining <= 0) {
                    set(SleepChoice.Off)
                    Step.Pause
                } else if (remaining <= FADE_MS) {
                    Step.Play(volumeFor(remaining), FADE_TICK_MS)
                } else {
                    // Wake again when the fade is due; capped so a late wake-up corrects itself soon.
                    Step.Play(1f, (remaining - FADE_MS).coerceAtMost(MAX_WAIT_MS))
                }
            }
        }
    }

    /**
     * The player stopped at the end of a song. True when that is the timer's doing ("End of this
     * song"), so the caller should keep it paused and put the volume back. The timer switches off.
     */
    fun songEnded(): Boolean {
        if (choice != SleepChoice.EndOfSong) return false
        set(SleepChoice.Off)
        return true
    }

    /**
     * Playback started again. A countdown that ran out while nothing was playing switches off here
     * rather than pausing the music the person has just started. True when it switched off.
     */
    fun resumed(): Boolean {
        val end = endsAt ?: return false
        if (clock() < end) return false
        set(SleepChoice.Off)
        return true
    }

    companion object {
        const val FADE_MS = 10_000L
        const val FADE_TICK_MS = 100L
        const val SONG_CHECK_MS = 500L
        const val MAX_WAIT_MS = 30_000L

        fun volumeFor(remainingMs: Long): Float = (remainingMs.toFloat() / FADE_MS).coerceIn(0f, 1f)
    }
}
