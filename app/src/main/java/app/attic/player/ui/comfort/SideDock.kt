package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The side dock's width, and how far it sits from the window's start edge, the status bar and the gesture area. */
internal val SideDockWidth = 80.dp
internal val SideDockGap = 12.dp

/** Where a destination's column starts: 8dp after the dock. Its cards add the phone's 16, so they start 24 after it. */
internal val SideDockContentStart = SideDockGap + SideDockWidth + 8.dp

/**
 * The dock on medium and expanded windows (docs/design/2026-11-25-tablet/SPEC.md, the shell): the
 * bottom dock's floating card turned on its side, 80dp wide on the raised surface with the floating
 * shadow and the edge ring. The mark (30dp) sits 20dp from its top; the tabs follow 20dp under it,
 * each an icon over its label in a 64 × 56 cell, 4dp apart, the selected one on the accent-soft fill
 * the bottom dock's tabs use. The song playing is not in it: the player bar or the floating mini
 * player shows it. Give it its height; the shell makes it as tall as the window allows.
 */
@Composable
fun ComfortSideDock(items: List<TabItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Column(
        modifier
            .width(SideDockWidth)
            .fillMaxHeight()
            .floatingShadow(ComfortShapes.Floating)
            .clip(ComfortShapes.Floating)
            .background(colors.raised)
            .padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 16.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComfortMark(size = 30.dp)
        // 16 under the mark, with the column's 4.
        Spacer(Modifier.height(12.dp))
        items.forEachIndexed { index, item ->
            SideDockTab(item, index == selectedIndex) { onSelect(index) }
        }
    }
}

/** One tab of the side dock: the icon over its 12/15 label, 2dp apart, centred in a 64 × 56 cell. */
@Composable
private fun SideDockTab(item: TabItem, selected: Boolean, onClick: () -> Unit) {
    val colors = Comfort.colors
    val ink = if (selected) colors.onAccentSoft else colors.on2
    Column(
        Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(ComfortShapes.Control)
            .background(if (selected) colors.accentSoft else Color.Transparent)
            // A selectable's ripple draws the hover and the keyboard focus too; Tab and Enter reach it.
            .selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        ComfortIcon(item.symbol, filled = selected, tint = ink)
        Text(
            item.label,
            style = onest(12f, 15f, if (selected) 700 else 500),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
