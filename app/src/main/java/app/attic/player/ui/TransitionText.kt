package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.attic.player.R
import app.attic.player.playback.TransitionMode

/** "Off", "Crossfade" or "Smart": the Transitions row's value and its picker's rows, in both styles. */
@Composable
fun transitionLabel(mode: TransitionMode): String = stringResource(
    when (mode) {
        TransitionMode.Off -> R.string.settings_crossfade_off
        TransitionMode.Crossfade -> R.string.settings_crossfade
        TransitionMode.Smart -> R.string.settings_transitions_smart
    },
)

/** What the chosen mode does; Crossfade and Smart add that album runs stay gapless. */
@Composable
fun transitionBody(mode: TransitionMode): String = when (mode) {
    TransitionMode.Off -> stringResource(R.string.settings_transitions_off_body)
    TransitionMode.Crossfade -> stringResource(R.string.settings_transitions_crossfade_body) + " " + stringResource(R.string.settings_crossfade_body)
    TransitionMode.Smart -> stringResource(R.string.settings_transitions_smart_body) + " " + stringResource(R.string.settings_crossfade_body)
}

/** "6 seconds". */
@Composable
fun crossfadeLengthLabel(seconds: Int): String = pluralStringResource(R.plurals.settings_crossfade_seconds, seconds, seconds)

/** Under "Crossfade length", for Smart only. */
@Composable
fun crossfadeLengthBody(mode: TransitionMode): String? =
    if (mode == TransitionMode.Smart) stringResource(R.string.settings_crossfade_length_smart_body) else null
