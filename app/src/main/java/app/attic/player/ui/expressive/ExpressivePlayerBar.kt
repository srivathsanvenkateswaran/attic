package app.attic.player.ui.expressive

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.ui.adaptive.LocalWindowLayout
import app.attic.player.ui.formatDuration
import app.attic.player.ui.model.MiniPlayerActions
import app.attic.player.ui.model.MiniPlayerState
import app.attic.player.ui.rememberArtColor
import app.attic.player.ui.rememberPosition

/** The overlays the player bar opens that the mini player has no buttons for. The shell provides them. */
class PlayerBarLinks(val openQueue: () -> Unit, val openLyrics: () -> Unit)

/** Set by [ExpressiveShell] around the player bar on an expanded window; null elsewhere. */
val LocalPlayerBarLinks = staticCompositionLocalOf<PlayerBarLinks?> { null }

/** The player bar's row, above the gesture area its surface runs on behind. */
val PlayerBarHeight: Dp = 80.dp

/**
 * The player bar (SPEC.md, the shell): the playing song on an expanded window, in place of the mini
 * player. An 80dp row on the cover scheme's surface container spanning the content area, its top
 * corners 28dp, its surface running on behind the gesture inset. Start: the 56dp cover and the title
 * over the artist (at most 320dp); tapping either opens Now Playing. Centre: previous, play or pause
 * (48dp, no ring, since the progress line under it shows how far the song has played) and next, over
 * the elapsed time, the wavy progress line and the length. End: favourite, queue and lyrics. The
 * centre group is 36% of the bar, 320 to 480dp; the start and end groups share the rest equally, so
 * the controls sit in the middle of the bar.
 */
@Composable
fun ExpressivePlayerBar(state: MiniPlayerState, actions: MiniPlayerActions) {
    val track = state.track ?: return
    val links = LocalPlayerBarLinks.current
    val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified)
    val barWidth = LocalWindowLayout.current.contentWidthDp
    val controls = (barWidth * 0.36f / 8f).let { Math.round(it) * 8f }.coerceIn(320f, 480f).dp
    val position = rememberPosition(state.clock, track.key, state.isPlaying, 500)
    val progress = if (track.durationMs > 0) (position.toFloat() / track.durationMs).coerceIn(0f, 1f) else 0f

    FromArt(seed) {
        val colors = MaterialTheme.colorScheme
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = ExpressiveShapes.ExtraLarge, topEnd = ExpressiveShapes.ExtraLarge))
                .background(colors.surfaceContainer)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Row(
                Modifier.fillMaxWidth().height(PlayerBarHeight).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ExpressiveShapes.Large))
                        .clickable(role = Role.Button, onClick = actions.open),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(Art.of(track.mediaId), size = 56.dp, shape = RoundedCornerShape(ExpressiveShapes.Large))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.widthIn(max = 320.dp)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(Modifier.width(controls), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveIconButton(Symbol.SkipPrevious, stringResource(R.string.previous), actions.previous, filled = true)
                        BarPlayButton(state.isPlaying, actions.togglePlayPause)
                        ExpressiveIconButton(Symbol.SkipNext, stringResource(R.string.next), actions.next, filled = true)
                    }
                    val figures = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDuration(position), style = figures, color = colors.onSurfaceVariant)
                        WavyLinearProgress({ progress }, Modifier.weight(1f), running = state.isPlaying)
                        Text(formatDuration(track.durationMs), style = figures, color = colors.onSurfaceVariant)
                    }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    HeartButton(state.liked, actions.toggleLike)
                    if (links != null) {
                        ExpressiveIconButton(Symbol.QueueMusic, stringResource(R.string.now_playing_open_queue_cd), links.openQueue)
                        ExpressiveIconButton(Symbol.Lyrics, stringResource(R.string.lyrics_open), links.openLyrics)
                    }
                }
            }
        }
    }
}

/** The bar's play control: 48dp, round while paused and squared to 16dp while playing, no ring. */
@Composable
private fun BarPlayButton(playing: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (playing) ExpressiveShapes.Large else 24.dp, ExpressiveMotion.fastSpatial(), label = "barPlayCorner")
    val description = stringResource(if (playing) R.string.pause else R.string.play)
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .clip(RoundedCornerShape(corner))
            .background(colors.primary)
            .clickable(role = Role.Button, indication = ripple(), interactionSource = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(playing, animationSpec = ExpressiveMotion.fastEffects(), label = "barPlayGlyph") { isPlaying ->
            SymbolIcon(if (isPlaying) Symbol.Pause else Symbol.PlayArrow, size = 28.dp, filled = true, tint = colors.onPrimary)
        }
    }
}
