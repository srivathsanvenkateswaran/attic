package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The navigation rail (SPEC.md, the shell): Material 3 Expressive's collapsed rail, [RailWidth] wide on
 * the page colour with no divider, in place of the navigation bar on medium and expanded windows. The
 * Headroom mark (32dp, primary) sits 20dp under the status bar, centred on the top bar's title line;
 * the destinations follow 28dp below it (each item has Material's 6dp above its pill), each a 56 x 32 indicator pill (secondary container when
 * selected, the icon filled) over its label in Label/Medium, 4dp apart, items 12dp apart.
 *
 * The items are tabs in one selectable group: Tab moves to the selected one and the arrow keys along
 * the rest, Enter or Space selects. Focus and hover show on the pill, as Material's rail draws them.
 */
@Composable
fun ExpressiveNavigationRail(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AtticMarkIcon(size = 32.dp, tint = colors.primary)
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEachIndexed { index, item ->
                RailItem(item, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
private fun RailItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val indicator by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f),
        ExpressiveMotion.fastEffects(),
        label = "railIndicator",
    )
    Column(
        Modifier
            .width(RailWidth)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp, 32.dp)
                .clip(ExpressiveShapes.Pill)
                .background(indicator)
                .indication(interaction, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(item.symbol, filled = selected, tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
