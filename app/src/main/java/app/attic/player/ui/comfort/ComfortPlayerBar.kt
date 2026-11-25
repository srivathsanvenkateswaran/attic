package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.MiniPlayerActions
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.rememberPosition

/** The player bar's card height. */
internal val PlayerBarHeight = 76.dp

/** What the player bar opens beyond the mini player's actions: the queue and the lyrics, over everything. */
@Immutable
internal class PlayerBarActions(val openQueue: () -> Unit, val openLyrics: () -> Unit)

/**
 * Set by the wide shell when the song shows as the player bar (an expanded window). The mini player
 * slot reads it and draws [ComfortPlayerBar] instead of the mini player; null everywhere else.
 */
internal val LocalPlayerBar = staticCompositionLocalOf<PlayerBarActions?> { null }

/**
 * The playing song on an expanded window (docs/design/2026-11-25-tablet/SPEC.md, the shell), in place
 * of the dock's mini player: a floating card 76dp tall with the floating shadow and the edge ring,
 * tinted from the cover as the mini player is, its played part filled (mini-fill over mini-base). That
 * fill is its progress, so there is no seek line. Start: the cover (52dp), title over artist; tapping
 * them opens Now Playing. Centre: the elapsed time, previous, play or pause and next (30, 36 and 30dp
 * glyphs in 48dp targets), the length. End: favourite, queue, lyrics. The start and end groups share
 * the width equally, so the centre group sits in the middle of the card.
 */
@Composable
internal fun ComfortPlayerBar(state: MiniPlayerState, actions: MiniPlayerActions, bar: PlayerBarActions, modifier: Modifier = Modifier) {
    val track = state.track ?: return
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified, deepen = false)
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 500)
    val duration = track.durationMs
    Tinted(seed) {
        val colors = Comfort.colors
        Row(
            modifier
                .fillMaxWidth()
                .height(PlayerBarHeight)
                .floatingShadow(ComfortShapes.Floating)
                .clip(ComfortShapes.Floating)
                .background(colors.miniBase)
                .drawBehind {
                    val played = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    val width = size.width * played
                    if (width > 0f) {
                        val r = 22.dp.toPx()
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = 0f, top = 0f, right = width, bottom = size.height,
                                    topLeftCornerRadius = CornerRadius.Zero,
                                    topRightCornerRadius = CornerRadius(r),
                                    bottomRightCornerRadius = CornerRadius(r),
                                    bottomLeftCornerRadius = CornerRadius.Zero,
                                ),
                            )
                        }
                        drawPath(path, colors.miniFill)
                    }
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.on) {
                Row(
                    Modifier.weight(1f).clip(ComfortShapes.Control).rowClick(actions.open),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Cover(Art.of(track.mediaId), size = 52.dp)
                    RowText(
                        track.title,
                        Modifier.weight(1f),
                        subtitle = track.artist,
                        titleStyle = onest(15f, 20f, 600),
                        subtitleStyle = onest(13f, 17f),
                        subtitleColor = colors.miniInk2,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(formatDuration(position), style = onest(14f, 18f, 600).tabular(), color = colors.on)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComfortIconButton(Symbol.SkipPrevious, stringResource(R.string.previous), actions.previous, filled = true, iconSize = 30.dp)
                        ComfortIconButton(
                            if (state.isPlaying) Symbol.Pause else Symbol.PlayArrow,
                            stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                            actions.togglePlayPause,
                            filled = true,
                            iconSize = 36.dp,
                        )
                        ComfortIconButton(Symbol.SkipNext, stringResource(R.string.next), actions.next, filled = true, iconSize = 30.dp)
                    }
                    Text(formatDuration(duration), style = onest(14f, 18f, 600).tabular(), color = colors.miniInk2)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    HeartButton(state.liked, actions.toggleLike)
                    ComfortIconButton(Symbol.QueueMusic, stringResource(R.string.now_playing_open_queue_cd), bar.openQueue)
                    ComfortIconButton(Symbol.Lyrics, stringResource(R.string.lyrics_open), bar.openLyrics)
                }
            }
        }
    }
}
