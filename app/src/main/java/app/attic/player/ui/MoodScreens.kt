package app.attic.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import app.attic.player.data.Library
import app.attic.player.data.UiStyle
import app.attic.player.ui.comfort.ComfortInterests
import app.attic.player.ui.comfort.ComfortMood
import app.attic.player.ui.expressive.ExpressiveInterests
import app.attic.player.ui.expressive.ExpressiveMood
import app.attic.player.ui.model.rememberInterestsModel
import app.attic.player.ui.model.rememberMoodModel

/** A mood's page: its mix for the day, Play and Mood radio (docs/features/mood-mixes.md). */
@Composable
fun MoodScreen(library: Library, id: String) {
    val (state, actions) = rememberMoodModel(library, id)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveMood(state, actions)
        UiStyle.Comfort -> ComfortMood(state, actions)
    }
}

/**
 * Moods and interests: the moods, genres and languages the mood mixes follow. Drawn full screen, as the
 * first-run pages are, so Save stays at the bottom with no tabs, dock or mini player over it: a window
 * of its own above the shell, which back closes like any page.
 */
@Composable
fun InterestsScreen(library: Library) {
    val (state, actions) = rememberInterestsModel(library)
    val dark = LocalIsDark.current
    Dialog(onDismissRequest = { actions.back() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                it.setDimAmount(0f)
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        // Its own window: nothing of the shell's (tabs, dock, mini player) sits under its foot.
        CompositionLocalProvider(LocalBottomPadding provides 0.dp) {
            when (LocalUiStyle.current) {
                UiStyle.Expressive -> ExpressiveInterests(state, actions)
                UiStyle.Comfort -> ComfortInterests(state, actions)
            }
        }
    }
}
