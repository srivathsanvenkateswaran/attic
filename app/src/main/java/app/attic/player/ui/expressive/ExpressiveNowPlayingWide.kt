package app.attic.player.ui.expressive

import androidx.compose.runtime.Composable
import app.attic.player.ui.model.LyricsScreenActions
import app.attic.player.ui.model.LyricsScreenState
import app.attic.player.ui.model.NowPlayingActions
import app.attic.player.ui.model.NowPlayingState
import app.attic.player.ui.model.QueueActions
import app.attic.player.ui.model.QueueState

/**
 * Now Playing beside its queue or lyrics, for an expanded window wider than it is tall
 * (docs/design/2026-11-25-tablet/SPEC.md, Now Playing; the design file's Tablet page, Now Playing).
 * The panel opens on [app.attic.player.ui.adaptive.LocalInitialPlayerPanel].
 */
@Composable
fun ExpressiveNowPlayingWide(
    state: NowPlayingState,
    actions: NowPlayingActions,
    queue: QueueState,
    queueActions: QueueActions,
    lyrics: LyricsScreenState,
    lyricsActions: LyricsScreenActions,
) {
    // Placeholder until the side-by-side layout lands: the phone layout.
    ExpressiveNowPlaying(state, actions)
}
