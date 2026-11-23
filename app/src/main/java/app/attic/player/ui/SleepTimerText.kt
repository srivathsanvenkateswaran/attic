package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.attic.player.R
import app.attic.player.playback.SleepChoice
import app.attic.player.ui.model.SleepTimerMenuState

/** A sleep timer choice as both styles list it. */
@Composable
fun sleepChoiceLabel(choice: SleepChoice): String = stringResource(
    when (choice) {
        SleepChoice.Minutes15 -> R.string.sleep_timer_15
        SleepChoice.Minutes30 -> R.string.sleep_timer_30
        SleepChoice.Minutes45 -> R.string.sleep_timer_45
        SleepChoice.Minutes60 -> R.string.sleep_timer_60
        SleepChoice.EndOfSong -> R.string.sleep_timer_end_of_song
        SleepChoice.Off -> R.string.sleep_timer_off
    },
)

/** What the timer is doing, for beside or under "Sleep timer". Null while it is off. */
@Composable
fun sleepTimerSummary(state: SleepTimerMenuState): String? {
    val remaining = state.remainingMs
    return when {
        state.choice == SleepChoice.Off -> null
        state.choice == SleepChoice.EndOfSong -> stringResource(R.string.sleep_timer_stops_after_song)
        remaining == null -> null
        remaining < 60_000L -> stringResource(R.string.sleep_timer_under_a_minute)
        else -> stringResource(R.string.sleep_timer_minutes_left, ((remaining + 59_999L) / 60_000L).toInt())
    }
}
