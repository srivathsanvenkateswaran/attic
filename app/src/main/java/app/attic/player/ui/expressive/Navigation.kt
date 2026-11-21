package app.attic.player.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import app.attic.player.R

/** One destination in the navigation bar. */
@Immutable
data class NavItem(val label: String, val symbol: Symbol)

/**
 * The navigation bar (`.nav`): Material's short navigation bar on surface container, a pill
 * indicator in secondary container, the selected icon filled and its label heavier.
 */
@Composable
fun ExpressiveNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
) {
    val colors = MaterialTheme.colorScheme
    ShortNavigationBar(
        modifier = modifier,
        containerColor = colors.surfaceContainer,
        contentColor = colors.onSurfaceVariant,
        windowInsets = windowInsets,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(index) },
                icon = { SymbolIcon(item.symbol, filled = selected) },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                        maxLines = 1,
                    )
                },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSecondaryContainer,
                    selectedTextColor = colors.onSurface,
                    selectedIndicatorColor = colors.secondaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

/** How far a list has scrolled away from its top, 0 to 1 over the first [range] pixels. For app bars. */
@Composable
fun LazyListState.scrolledFraction(range: Float = 120f): Float {
    val fraction by remember(this) {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / range).coerceIn(0f, 1f)
        }
    }
    return fraction
}

/**
 * A tab root's app bar (`.appbar.root`): one row, a large rounded title (36/44) and trailing actions,
 * settings last. As [scrolled] goes from 0 to 1 the title shrinks to 22/28 and the bar takes the
 * surface container colour, running under the status bar. [titleLeading] holds Home's mark.
 */
@Composable
fun RootAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrolled: Float = 0f,
    titleLeading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val type = Expressive.type
    val fraction by animateFloatAsState(scrolled, ExpressiveMotion.defaultEffects(), label = "rootBar")
    val container by animateColorAsState(
        if (scrolled > 0.5f) colors.surfaceContainer else colors.surface.copy(alpha = 0f),
        ExpressiveMotion.defaultEffects(),
        label = "rootBarColor",
    )
    val big = type.displaySmallEmphasized.copy(letterSpacing = (-0.4).sp)
    val small = type.titleLargeEmphasized
    val style = TextStyle(
        fontFamily = big.fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = lerp(big.fontSize, small.fontSize, fraction),
        lineHeight = lerp(big.lineHeight, small.lineHeight, fraction),
        letterSpacing = lerp(big.letterSpacing, 0.sp, fraction),
    )
    Box(modifier.fillMaxWidth().background(container).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(lerp(72.dp, 64.dp, fraction)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (titleLeading != null) {
                    titleLeading()
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

/** The colours an app bar sits in. [OverArt] floats over a header block or image, giving its buttons a scrim. */
enum class AppBarSurface { Plain, Scrolled, OverArt }

/**
 * A detail app bar (`.appbar`): back, a 22/28 title, actions. [surface] Scrolled gives it the
 * container colour under the status bar (`.appbar.scrolled`); OverArt leaves it transparent with
 * scrims behind the buttons (`.appbar.over`) and hides the title until the header has scrolled away.
 */
@Composable
fun DetailAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    surface: AppBarSurface = AppBarSurface.Plain,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (surface == AppBarSurface.Scrolled) colors.surfaceContainer else colors.surfaceContainer.copy(alpha = 0f),
        ExpressiveMotion.defaultEffects(),
        label = "detailBar",
    )
    val over = surface == AppBarSurface.OverArt
    Box(modifier.fillMaxWidth().background(container).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OverArtScrim(over) {
                ExpressiveIconButton(Symbol.ArrowBack, stringResource(R.string.back), onBack, tint = if (over) colors.onSurface else Color.Unspecified)
            }
            Text(
                if (over) "" else title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            OverArtScrim(over) { Row(content = actions) }
        }
    }
}

@Composable
private fun OverArtScrim(over: Boolean, content: @Composable () -> Unit) {
    if (!over) return content()
    val scrim = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    CompositionLocalProvider(LocalStandardIconTint provides MaterialTheme.colorScheme.onSurface) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.padding(4.dp).clip(ExpressiveShapes.Pill).background(scrim)) { content() }
        }
    }
}

/**
 * The search bar (`.searchbar`): a 56dp pill on surface container high with the search glyph, the
 * query, and a clear button once there is text. [placeholder] is "Search your music".
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth().height(56.dp),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(ExpressiveShapes.Pill).background(colors.surfaceContainerHigh)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(Symbol.Search, tint = colors.onSurface)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, style = textStyle.copy(color = colors.onSurfaceVariant), maxLines = 1)
                    field()
                }
                if (query.isNotEmpty()) {
                    ExpressiveIconButton(Symbol.Close, stringResource(R.string.search_clear_cd), { onQueryChange("") }, tint = colors.onSurfaceVariant)
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
        },
    )
}

/**
 * A filter chip (`.chip`): 32dp, 8dp corners, an outline while off; tonal with a tick while on.
 * Filters that can be cleared are chips; a one-of-N switch is a [ConnectedButtonGroup].
 */
@Composable
fun ExpressiveFilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        leadingIcon = if (selected) ({ SymbolIcon(Symbol.Check, size = 18.dp) }) else null,
        shape = RoundedCornerShape(ExpressiveShapes.Small),
        colors = FilterChipDefaults.filterChipColors(
            labelColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.secondaryContainer,
            selectedLabelColor = colors.onSecondaryContainer,
            selectedLeadingIconColor = colors.onSecondaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outlineVariant,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

/** A row of chips (`.chips`), 8dp apart with 16dp side padding. */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/**
 * A connected button group (`.seg`): a one-of-N switch that always has one selected, such as Home's
 * filter or Recap's period. Segments are 40dp tall and 2dp apart; the group's ends are round and the
 * inner corners 8. The selected segment widens a little, goes fully round, fills with primary and
 * shows a tick.
 */
@Composable
fun ConnectedButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val weight by animateFloatAsState(if (selected) 1.15f else 1f, ExpressiveMotion.fastSpatial(), label = "segWeight")
            val start by animateDpAsState(if (selected || index == 0) 20.dp else 8.dp, ExpressiveMotion.fastSpatial(), label = "segStart")
            val end by animateDpAsState(if (selected || index == options.lastIndex) 20.dp else 8.dp, ExpressiveMotion.fastSpatial(), label = "segEnd")
            val container by animateColorAsState(if (selected) colors.primary else colors.surfaceContainerHigh, ExpressiveMotion.fastEffects(), label = "segColor")
            val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurfaceVariant, ExpressiveMotion.fastEffects(), label = "segContent")
            val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
            Row(
                Modifier
                    .weight(weight)
                    .height(40.dp)
                    .clip(shape)
                    .background(container)
                    .selectable(selected = selected, role = Role.RadioButton, interactionSource = null, indication = ripple()) { onSelect(index) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides content) {
                    if (selected) {
                        SymbolIcon(Symbol.Check, size = 18.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * A section header (`.sec`): the section title in the emphasised title style, an optional overline
 * above it ("Picked at random from your songs"), and an optional trailing control such as a 56dp play
 * button. 28dp above, 12 below, 16 at the sides.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
            }
            Text(title, style = Expressive.type.titleLargeEmphasized)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.padding(vertical = 0.dp)) { trailing() }
        }
    }
}

/** A small heading directly above a group (`.list-label`): "Up next, 9 songs". */
@Composable
fun ListLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}
