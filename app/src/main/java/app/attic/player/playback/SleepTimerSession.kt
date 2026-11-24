package app.attic.player.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/**
 * How the sleep timer crosses the session boundary. The UI sets it with [COMMAND]; the service
 * publishes its state in the session extras, which every connected controller receives.
 */
object SleepTimerSession {
    val COMMAND = SessionCommand("app.attic.player.SLEEP_TIMER", Bundle.EMPTY)
    private const val MINUTES = "sleep_minutes"
    private const val ENDS_AT = "sleep_ends_at"

    fun args(choice: SleepChoice): Bundle = Bundle().apply { putInt(MINUTES, choice.minutes) }

    fun choiceFrom(args: Bundle): SleepChoice = SleepChoice.of(args.getInt(MINUTES, 0))

    fun extras(status: SleepTimerStatus): Bundle = Bundle().apply {
        putInt(MINUTES, status.choice.minutes)
        status.endsAtElapsedMs?.let { putLong(ENDS_AT, it) }
    }

    fun statusFrom(extras: Bundle): SleepTimerStatus = SleepTimerStatus(
        choice = SleepChoice.of(extras.getInt(MINUTES, 0)),
        endsAtElapsedMs = if (extras.containsKey(ENDS_AT)) extras.getLong(ENDS_AT) else null,
    )
}
