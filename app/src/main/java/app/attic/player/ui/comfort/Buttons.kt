package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.expressive.Symbol

/** `.btn` fills: `.filled` (the one main action on a screen), `.tonal`, `.plain` (neutral), `.text`. */
enum class ButtonStyle { Filled, Tonal, Plain, Text }

/** `.btn` heights: `.sm` 36, default 48, `.lg` 56. */
enum class ButtonSize(val height: Dp, val padding: Dp, val fontSize: Float) {
    Small(36.dp, 16.dp, 14f),
    Medium(48.dp, 22.dp, 15.5f),
    Large(56.dp, 28.dp, 17f),
}

/**
 * `.btn`: a pill. [leading] is the icon that goes first (`<span class="ms">` inside the button).
 * [block] is `.block` (full width), [danger] `.danger`, `enabled = false` is `.off` (38% opacity).
 * Material's Button underneath, so it has its ripple, minimum touch size and semantics.
 */
@Composable
fun ComfortButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    size: ButtonSize = ButtonSize.Medium,
    leading: Symbol? = null,
    leadingFilled: Boolean = false,
    block: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = Comfort.colors
    val (container, ink) = when (style) {
        ButtonStyle.Filled -> colors.accent to colors.onAccent
        ButtonStyle.Tonal -> colors.accentSoft to colors.onAccentSoft
        ButtonStyle.Plain -> colors.fill to colors.on
        ButtonStyle.Text -> Color.Transparent to colors.accentInk
    }
    val content = if (danger) colors.danger else ink
    val horizontal = if (style == ButtonStyle.Text) 12.dp else size.padding
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ComfortShapes.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        contentPadding = PaddingValues(horizontal = horizontal),
        modifier = modifier
            .then(if (block) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        if (leading != null) {
            ComfortIcon(leading, filled = leadingFilled, tint = content, modifier = Modifier.offset(x = (-4).dp))
            Box(Modifier.size(4.dp))
        }
        Text(text, style = onest(size.fontSize, 20f, 600), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * `.ib`: a bare icon button with a 48dp target. [dim] is `.dim` (secondary ink); `enabled = false` is
 * `.off`. [tint] overrides the ink, e.g. to keep a row's trailing icon in `--on-3`.
 */
@Composable
fun ComfortIconButton(
    symbol: Symbol,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
    filled: Boolean = false,
    enabled: Boolean = true,
    iconSize: Dp = 24.dp,
    tint: Color = if (dim) Comfort.colors.on2 else LocalContentColor.current,
) {
    Box(
        modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        ComfortIcon(symbol, size = iconSize, filled = filled, tint = tint)
    }
}

/**
 * `.ib.tonal`: the icon in a superellipse, the shape of a quick-panel toggle. [checked] is `.on`
 * (accent fill, on-accent ink); [large] is `.lg` (54dp). With [onCheckedChange] null it is a plain
 * tonal button ([onClick]) rather than a toggle.
 */
@Composable
fun TonalIconButton(
    symbol: Symbol,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    large: Boolean = false,
    filled: Boolean = checked,
    stateDescription: String? = null,
) {
    val colors = Comfort.colors
    val side = if (large) 54.dp else 48.dp
    Box(
        modifier
            .size(side)
            .clip(ComfortShapes.Squircle)
            .background(if (checked) colors.accent else colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                if (stateDescription != null) this.stateDescription = stateDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        ComfortIcon(symbol, filled = filled, tint = if (checked) colors.onAccent else colors.on)
    }
}

/**
 * `.ib.heart`: the favourite. Off it is an outline in the ink around it; on it is filled in the accent.
 * With [tonal] it is the deck's `.ib.tonal.lg.heart` (lit: accent fill). Describes itself: "Add to
 * favourites" / "Remove from favourites".
 */
@Composable
fun HeartButton(
    favourite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    large: Boolean = tonal,
    tint: Color = LocalContentColor.current,
) {
    val description = stringResource(if (favourite) R.string.favourite_remove else R.string.favourite_add)
    if (tonal) {
        TonalIconButton(Symbol.Favorite, description, onToggle, modifier, checked = favourite, large = large)
    } else {
        ComfortIconButton(
            Symbol.Favorite,
            description,
            onToggle,
            modifier,
            filled = favourite,
            tint = if (favourite) Comfort.colors.accent else tint,
        )
    }
}

/** `.play` sizes: 76 (`.play`), 56 (`.md`), 44 (`.sm`). */
enum class PlaySize(val side: Dp, val glyph: Dp) {
    Large(76.dp, 42.dp),
    Medium(56.dp, 32.dp),
    Small(44.dp, 26.dp),
}

/**
 * `.play`: the accent in a superellipse with a filled play or pause glyph (weight 400). Describes
 * itself "Play" / "Pause".
 */
@Composable
fun PlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: PlaySize = PlaySize.Large) {
    val colors = Comfort.colors
    val description = stringResource(if (playing) R.string.pause else R.string.play)
    Box(
        modifier
            .size(size.side)
            .clip(ComfortShapes.Squircle)
            .background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ComfortIcon(if (playing) Symbol.Pause else Symbol.PlayArrow, size = size.glyph, filled = true, weight = 400, tint = colors.onAccent)
    }
}

/**
 * The Play button of a collection's action row: filled, "Play" with `play_arrow`, or "Pause" with
 * `pause` while this collection is playing. Give it `Modifier.weight(1f)` in [CollectionActions].
 */
@Composable
fun CollectionPlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ComfortButton(
        text = stringResource(if (playing) R.string.pause else R.string.play),
        onClick = onClick,
        modifier = modifier,
        leading = if (playing) Symbol.Pause else Symbol.PlayArrow,
        leadingFilled = true,
    )
}

/** `.c-actions`: Play, then tonal toggles (shuffle, add to playlist; a playlist adds add songs and reorder). */
@Composable
fun CollectionActions(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Tap and long-press on a row or tile, with the default ripple. Nothing when both are null. */
internal fun Modifier.rowClick(onClick: (() -> Unit)?, onLongClick: (() -> Unit)? = null): Modifier =
    if (onClick == null && onLongClick == null) this
    else this.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
