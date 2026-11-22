package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.RoundRect
import app.attic.player.R
import app.attic.player.ui.expressive.Art
import app.attic.player.ui.expressive.Symbol
import kotlin.math.roundToInt

/** One tab of the dock. */
data class TabItem(val label: String, val symbol: Symbol)

/**
 * `.dock`: one floating card (radius 30, raised, with the floating shadow), 12dp in from the edges,
 * holding the [mini] player as its first row and the [tabs] as its second. With no song queued pass
 * `mini = null` and it is a capsule tab bar. Content fades out behind it (`linear-gradient` to the
 * ground over 36dp above the card). It pads for the gesture bar: 22dp from the bottom edge, or the
 * navigation inset plus 6 when that is taller. Place it at the bottom of the screen, over the content.
 */
@Composable
fun Dock(
    modifier: Modifier = Modifier,
    mini: (@Composable () -> Unit)? = null,
    tabs: @Composable () -> Unit,
) {
    val colors = Comfort.colors
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom = max(22.dp, navigation + 6.dp)
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 1f to colors.ground, startY = 0f, endY = with(androidx.compose.ui.platform.LocalDensity.current) { 36.dp.toPx() }))
            .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = bottom),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .floatingShadow(ComfortShapes.Floating)
                .clip(ComfortShapes.Floating)
                .background(colors.raised),
        ) {
            if (mini != null) mini()
            tabs()
        }
    }
}

/**
 * `.tabs`: icon beside label, the selected one in a tonal capsule with a filled icon. 60dp.
 */
@Composable
fun TabBar(items: List<TabItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Row(
        modifier.fillMaxWidth().height(60.dp).padding(8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val ink = if (selected) colors.onAccentSoft else colors.on2
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(ComfortShapes.Pill)
                    .background(if (selected) colors.accentSoft else Color.Transparent)
                    .selectable(selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComfortIcon(item.symbol, filled = selected, tint = ink)
                Text(item.label, style = onest(14f, 18f, if (selected) 700 else 500), color = ink, maxLines = 1)
            }
        }
    }
}

/** Where a mini player sits: the dock's first row, alone (widget, notification), or the bar across a scrolled Now Playing. */
enum class MiniPlayerPlacement { Dock, Alone, Top }

/**
 * `.mini`: the current song as a 64dp row whose played part ([progress], 0 to 1) is filled. There is no
 * progress line. Cover (44dp), title and artist, the heart, then play or pause. Tap opens Now Playing;
 * swiping the text sideways past 28% of its width skips ([onSwipeNext], [onSwipePrevious]).
 *
 * The mini player takes its colour from [seed] itself (`.mini.tinted`): its ground and fill become the
 * cover's. [MiniPlayerPlacement.Top] is `.mini.top`: 60dp, square corners, no cover.
 */
@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    art: Art?,
    progress: () -> Float,
    playing: Boolean,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seed: Color = Color.Unspecified,
    placement: MiniPlayerPlacement = MiniPlayerPlacement.Dock,
    onSwipeNext: (() -> Unit)? = null,
    onSwipePrevious: (() -> Unit)? = null,
) {
    Tinted(seed) {
        val colors = Comfort.colors
        val shape: Shape = when (placement) {
            MiniPlayerPlacement.Dock -> RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            MiniPlayerPlacement.Alone -> ComfortShapes.Floating
            MiniPlayerPlacement.Top -> androidx.compose.ui.graphics.RectangleShape
        }
        Row(
            modifier
                .fillMaxWidth()
                .height(if (placement == MiniPlayerPlacement.Top) 60.dp else 64.dp)
                .clip(shape)
                .background(colors.miniBase)
                .drawBehind {
                    val width = size.width * progress().coerceIn(0f, 1f)
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
                .rowClick(onClick)
                .padding(start = if (placement == MiniPlayerPlacement.Top) 20.dp else 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (placement != MiniPlayerPlacement.Top) Cover(art, size = 44.dp)
            SwipeText(onSwipeNext, onSwipePrevious, Modifier.weight(1f)) {
                RowText(
                    title,
                    subtitle = artist,
                    titleStyle = onest(15f, 20f, 600),
                    subtitleStyle = onest(13f, 17f),
                    subtitleColor = colors.miniInk2,
                )
            }
            CompositionLocalProvider(LocalContentColor provides colors.on) {
                HeartButton(favourite, onToggleFavourite)
                ComfortIconButton(
                    if (playing) Symbol.Pause else Symbol.PlayArrow,
                    stringResource(if (playing) R.string.pause else R.string.play),
                    onPlayPause,
                    Modifier.offset(x = (-6).dp),
                    filled = true,
                    iconSize = 30.dp,
                )
            }
        }
    }
}

@Composable
private fun SwipeText(
    onNext: (() -> Unit)?,
    onPrevious: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (onNext == null && onPrevious == null) {
        Box(modifier) { content() }
        return
    }
    var offset by remember { mutableFloatStateOf(0f) }
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    Box(
        modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = size.width * 0.28f
                        if (offset <= -threshold) next?.invoke() else if (offset >= threshold) previous?.invoke()
                        offset = 0f
                    },
                    onDragCancel = { offset = 0f },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        offset += delta
                    },
                )
            }
            .offset { IntOffset(offset.roundToInt(), 0) },
    ) { content() }
}
