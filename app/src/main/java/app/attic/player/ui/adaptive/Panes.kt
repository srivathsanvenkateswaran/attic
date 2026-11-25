package app.attic.player.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Which row of the Library's list is open in the pane beside it (ui/LibraryPanes.kt). */
@Immutable
data class ListDetailSelection(val itemId: String)

/** Set inside the Library's list pane while it shows list and detail; null everywhere else. */
val LocalListDetailSelection = compositionLocalOf<ListDetailSelection?> { null }

/** True inside the detail pane beside the Library's list: the screen leaves out its back button. */
val LocalDetailPane = staticCompositionLocalOf { false }

/** What the end half of Now Playing shows when it sits beside the player (SPEC.md, Now Playing). */
enum class PlayerPanel { UpNext, Lyrics }

/** The panel Now Playing opens on. Up next in the app; the design renders set Lyrics to draw that state. */
val LocalInitialPlayerPanel = staticCompositionLocalOf { PlayerPanel.UpNext }
