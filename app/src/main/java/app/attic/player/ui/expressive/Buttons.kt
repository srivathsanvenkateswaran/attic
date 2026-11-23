package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import kotlin.math.roundToInt

enum class ButtonVariant { Filled, Tonal, Outlined, Text }

/** `.btn` is 40dp tall; `.btn.m` is 56. */
enum class ButtonSize(val height: Dp) { Small(40.dp), Medium(56.dp) }

/**
 * A labelled button (`.btn`): filled, tonal, outlined or text, in two heights, fully round. [squared]
 * is "squared off means on": a collection's Play button squares off while it reads Pause.
 * [leading] / [trailing] put a symbol before or after the label; [leadingFilled] fills it (play glyphs are filled).
 * [contentColor] recolours a text button, e.g. `error` for a destructive confirm ("Delete playlist").
 */
@Composable
fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    size: ButtonSize = ButtonSize.Small,
    leading: Symbol? = null,
    leadingFilled: Boolean = true,
    trailing: Symbol? = null,
    squared: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    contentColor: Color = Color.Unspecified,
) {
    val medium = size == ButtonSize.Medium
    val corner by animateDpAsState(
        if (squared) (if (medium) ExpressiveShapes.Large else ExpressiveShapes.Medium) else size.height / 2,
        ExpressiveMotion.fastSpatial(),
        label = "buttonCorner",
    )
    val shape = RoundedCornerShape(corner)
    val side = when {
        variant == ButtonVariant.Text -> 12.dp
        medium -> 24.dp
        else -> 16.dp
    }
    val padding = PaddingValues(start = if (leading != null) side - 4.dp else side, end = if (trailing != null) side - 4.dp else side)
    val iconSize = if (medium) 24.dp else 20.dp
    val labelStyle = MaterialTheme.typography.labelLarge.let {
        if (medium) it.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold) else it.copy(fontWeight = FontWeight.SemiBold)
    }
    val sized = modifier.height(size.height)
    val content: @Composable RowScope.() -> Unit = {
        if (leading != null) {
            SymbolIcon(leading, size = iconSize, filled = leadingFilled)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = labelStyle, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            SymbolIcon(trailing, size = iconSize)
        }
    }
    when (variant) {
        ButtonVariant.Filled -> Button(onClick, sized, enabled, shape, contentPadding = padding, interactionSource = interactionSource, content = content)
        ButtonVariant.Tonal -> FilledTonalButton(onClick, sized, enabled, shape, contentPadding = padding, interactionSource = interactionSource, content = content)
        ButtonVariant.Outlined -> OutlinedButton(
            onClick, sized, enabled, shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = padding,
            interactionSource = interactionSource,
            content = content,
        )
        ButtonVariant.Text -> TextButton(
            onClick, sized, enabled, shape,
            colors = if (contentColor.isSpecified) ButtonDefaults.textButtonColors(contentColor = contentColor) else ButtonDefaults.textButtonColors(),
            contentPadding = padding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

enum class IconButtonVariant { Standard, Filled, Tonal, Outlined }

/** The colour of a standard icon button's glyph. onSurfaceVariant unless a container (a toolbar) says otherwise. */
val LocalStandardIconTint = staticCompositionLocalOf { Color.Unspecified }

/** `.ib.narrow`, `.ib`, `.ib.wide`. */
enum class IconButtonWidth { Narrow, Default, Wide }

private fun iconButtonSize(medium: Boolean, width: IconButtonWidth): Pair<Dp, Dp> {
    val height = if (medium) 56.dp else 40.dp
    val w = when (width) {
        IconButtonWidth.Narrow -> if (medium) 48.dp else 32.dp
        IconButtonWidth.Default -> height
        IconButtonWidth.Wide -> if (medium) 72.dp else 52.dp
    }
    return w to height
}

/**
 * An icon button (`.ib`): a 40dp container in a 48dp touch target, or 56dp with [medium] (`.ib.m`).
 * Standard has no container; filled, tonal and outlined do. [width] gives the narrow and wide forms.
 */
@Composable
fun ExpressiveIconButton(
    symbol: Symbol,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.Standard,
    medium: Boolean = false,
    width: IconButtonWidth = IconButtonWidth.Default,
    filled: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    iconSize: Dp = 24.dp,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (w, h) = iconButtonSize(medium, width)
    val container = when (variant) {
        IconButtonVariant.Filled -> colors.primary
        IconButtonVariant.Tonal -> colors.secondaryContainer
        else -> Color.Transparent
    }
    val content = when {
        tint != Color.Unspecified -> tint
        variant == IconButtonVariant.Filled -> colors.onPrimary
        variant == IconButtonVariant.Tonal -> colors.onSecondaryContainer
        else -> LocalStandardIconTint.current.takeOrElse { colors.onSurfaceVariant }
    }
    val shape = ExpressiveShapes.Pill
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(w, h)
            .clip(shape)
            .background(container)
            .then(if (variant == IconButtonVariant.Outlined) Modifier.border(1.dp, colors.outlineVariant, shape) else Modifier)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(symbol, size = iconSize, filled = filled, tint = if (enabled) content else content.copy(alpha = 0.38f))
    }
}

/**
 * A toggle (`.ib.on`): round while off, squared off and filled with primary while on, with the
 * glyph filled. The corner morphs on the fast spatial spring, the colour on fast effects. [tonalWhenOff]
 * gives it the tonal container while off, as in a collection header (`.ib.m.tonal`).
 */
@Composable
fun ExpressiveToggleButton(
    symbol: Symbol,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    medium: Boolean = false,
    width: IconButtonWidth = IconButtonWidth.Default,
    tonalWhenOff: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val (w, h) = iconButtonSize(medium, width)
    val corner by animateDpAsState(if (checked) ExpressiveShapes.onCorner(h) else h / 2, ExpressiveMotion.fastSpatial(), label = "toggleCorner")
    val container by animateColorAsState(
        when {
            checked -> colors.primary
            tonalWhenOff -> colors.secondaryContainer
            else -> colors.primary.copy(alpha = 0f)
        },
        ExpressiveMotion.fastEffects(),
        label = "toggleContainer",
    )
    val content by animateColorAsState(
        when {
            checked -> colors.onPrimary
            tonalWhenOff -> colors.onSecondaryContainer
            else -> LocalStandardIconTint.current.takeOrElse { colors.onSurfaceVariant }
        },
        ExpressiveMotion.fastEffects(),
        label = "toggleContent",
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(w, h)
            .clip(shape)
            .background(container)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = null,
                indication = ripple(),
                onValueChange = onCheckedChange,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(symbol, filled = checked, tint = if (enabled) content else content.copy(alpha = 0.38f))
    }
}

/**
 * The favourite heart (`.ib.heart`): an outline, then filled in primary, with no container. It pops
 * from 0.8 to 1 on the fast spatial spring when it changes. The description comes from the shared copy.
 */
@Composable
fun HeartButton(favourite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val scale = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(favourite) }
    LaunchedEffect(favourite) {
        if (shown != favourite) {
            shown = favourite
            scale.snapTo(0.8f)
            scale.animateTo(1f, ExpressiveMotion.fastSpatial())
        }
    }
    val description = stringResource(if (favourite) R.string.favourite_remove else R.string.favourite_add)
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(ExpressiveShapes.Pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(
            Symbol.Favorite,
            filled = favourite,
            tint = if (favourite) colors.primary else LocalStandardIconTint.current.takeOrElse { colors.onSurfaceVariant },
            modifier = Modifier.scale(scale.value),
        )
    }
}

/** What a [ButtonGroup] child can ask for. */
class ButtonGroupScope internal constructor(private val rowScope: RowScope, private val fill: Boolean) {
    /**
     * Marks a child of the group. While it is pressed it widens by 15%; in a [ButtonGroup] that fills
     * its width ([weight] set), its neighbours narrow to make room. Pass the same [interactionSource]
     * to the button.
     */
    @Composable
    fun Modifier.groupItem(interactionSource: MutableInteractionSource, weight: Float = 1f): Modifier {
        val pressed by interactionSource.collectIsPressedAsState()
        val factor by animateFloatAsState(if (pressed) 1.15f else 1f, ExpressiveMotion.fastSpatial(), label = "groupItem")
        return if (fill) {
            with(rowScope) { this@groupItem.weight(weight * factor) }
        } else {
            this.layout { measurable, constraints ->
                val natural = measurable.maxIntrinsicWidth(constraints.maxHeight)
                val width = (natural * factor).roundToInt().coerceAtMost(constraints.maxWidth)
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
    }
}

/**
 * A button group (`.btn-group`): buttons 8dp apart that react to each other. With [fill] the group
 * spans its width and the children share it by weight.
 */
@Composable
fun ButtonGroup(
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    spacing: Dp = 8.dp,
    content: @Composable ButtonGroupScope.() -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        ButtonGroupScope(this, fill).content()
    }
}

/** A FAB (`.fab`): 56dp, 16dp corners, primary container, the one surface besides menus that casts a shadow. */
@Composable
fun ExpressiveFab(symbol: Symbol, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(ExpressiveShapes.Large),
        containerColor = if (primary) colors.primary else colors.primaryContainer,
        contentColor = if (primary) colors.onPrimary else colors.onPrimaryContainer,
    ) { SymbolIcon(symbol) }
}

/** An extended FAB (`.fab.ext`): a symbol and a label. Not [expanded], it shrinks to the symbol (a list has scrolled). */
@Composable
fun ExpressiveExtendedFab(text: String, symbol: Symbol, onClick: () -> Unit, modifier: Modifier = Modifier, expanded: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        modifier = modifier,
        shape = RoundedCornerShape(ExpressiveShapes.Large),
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        icon = { SymbolIcon(symbol) },
        text = { Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1) },
    )
}

/**
 * A split button (`.split`): an action and its menu, 2dp apart, outer corners round and inner ones 4,
 * on the tonal container. The menu half shows `keyboard_arrow_down`.
 */
@Composable
fun SplitButton(
    text: String,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuContentDescription: String,
    modifier: Modifier = Modifier,
    leading: Symbol? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.height(40.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            color = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
        ) {
            Row(Modifier.padding(start = 16.dp, end = 12.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    SymbolIcon(leading, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            }
        }
        Surface(
            onClick = onMenuClick,
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            color = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            modifier = Modifier.semantics { contentDescription = menuContentDescription },
        ) {
            Box(Modifier.padding(start = 10.dp, end = 12.dp).height(40.dp), contentAlignment = Alignment.Center) {
                SymbolIcon(Symbol.KeyboardArrowDown, size = 20.dp)
            }
        }
    }
}

/**
 * A floating toolbar (`.toolbar`): one 64dp pill holding a few icon buttons. [vibrant] puts it on the
 * primary container; [floating] gives it the shadow it needs over content.
 */
@Composable
fun FloatingToolbar(
    modifier: Modifier = Modifier,
    vibrant: Boolean = false,
    floating: Boolean = false,
    height: Dp = 64.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (vibrant) colors.primaryContainer else colors.surfaceContainerHigh
    val contentColor = if (vibrant) colors.onPrimaryContainer else colors.onSurfaceVariant
    Row(
        modifier
            .then(if (floating) Modifier.shadow(6.dp, ExpressiveShapes.Pill) else Modifier)
            .clip(ExpressiveShapes.Pill)
            .background(container)
            .height(height)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, LocalStandardIconTint provides contentColor) { content() }
    }
}
