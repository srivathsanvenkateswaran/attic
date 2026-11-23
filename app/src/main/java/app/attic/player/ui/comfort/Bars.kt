package app.attic.player.ui.comfort

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.attic.player.R
import app.attic.player.ui.expressive.Symbol

/**
 * `.hero`: the expanded large title of a root screen (Home, Search, Library, Settings, Recap), centred
 * in an otherwise empty upper third, 228dp tall ([short]: 150). [subtitle] is the optional line under
 * it; [mark] puts the Attic mark above the title (Home only). It pads for the status bar itself
 * ([insets]). Follow it with [Tools]; once scrolled, show [Bar] instead.
 */
@Composable
fun Hero(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    short: Boolean = false,
    mark: Boolean = false,
    insets: WindowInsets = WindowInsets.statusBars,
) {
    Column(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .height(if (short) 150.dp else 228.dp)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (mark) {
            ComfortMark(size = 30.dp)
            Spacer(Modifier.height(14.dp))
        }
        Text(
            title,
            style = Comfort.type.hero,
            color = Comfort.colors.on,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = onest(15f, 20f),
                color = Comfort.colors.on2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * `.tools`: the 50dp row that closes the viewing area: [leading] on the left (usually [SubTabs]),
 * [actions] on the right (icon buttons; settings last).
 */
@Composable
fun Tools(
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(50.dp).padding(start = 24.dp, end = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Row(Modifier.weight(1f).offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically, content = leading)
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

/**
 * `.bar`: the collapsed bar (root screens, once scrolled) and the detail bar. [onBack] adds the back
 * button ([backSymbol] `expand_more` for overlays: Queue, Lyrics). [lift] is `.bar.lift`: content has
 * scrolled under it, so it takes the ground and a hairline. [title] empty at the top of a detail
 * screen. [accentTitle] is `.bar.count` ("2 selected"). Pads for the status bar ([insets]).
 */
@Composable
fun Bar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backSymbol: Symbol = Symbol.ArrowBack,
    backDescription: String = stringResource(R.string.back),
    lift: Boolean = false,
    accentTitle: Boolean = false,
    insets: WindowInsets = WindowInsets.statusBars,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = Comfort.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (lift) Modifier.background(colors.ground).drawBehind {
                    drawRect(colors.line, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                } else Modifier,
            )
            .windowInsetsPadding(insets)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) ComfortIconButton(backSymbol, backDescription, onBack)
        Text(
            title,
            style = Comfort.type.barTitle,
            color = if (accentTitle) colors.accentInk else colors.on,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 16.dp).semantics { heading() },
        )
        CompositionLocalProvider(LocalContentColor provides colors.on) { actions() }
    }
}

/**
 * `.subtabs`: a choice where exactly one option is always selected (Home's filters, Recap's periods).
 * The selected one is bold `--on` with a short accent underline. [lift] is `.subtabs.lift`, pinned
 * under a collapsed bar. Put it in [Tools]' leading slot, or alone.
 */
@Composable
fun SubTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lift: Boolean = false,
) {
    val colors = Comfort.colors
    Row(
        modifier
            .then(
                if (lift) Modifier.fillMaxWidth().background(colors.ground).drawBehind {
                    drawRect(colors.line, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                }.padding(horizontal = 14.dp) else Modifier,
            )
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .height(48.dp)
                    .selectable(selected, role = Role.Tab, onClick = { if (!selected) onSelect(index) })
                    .drawBehind {
                        if (selected) {
                            val inset = 10.dp.toPx()
                            drawRoundRect(
                                colors.accent,
                                topLeft = Offset(inset, size.height - 7.dp.toPx() - 3.dp.toPx()),
                                size = Size(size.width - inset * 2, 3.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = onest(15f, 20f, if (selected) 700 else 500),
                    color = if (selected) colors.on else colors.on2,
                    maxLines = 1,
                )
            }
        }
    }
}

/** `.chips`: a sideways-scrolling row of [FilterChip]s, 8dp apart. */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * `.chip`: a filter that can be switched off again (Library). Selected: tonal with a tick. On a card,
 * sheet or dialog the unselected chip takes `--fill` by itself.
 */
@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val onCard = LocalComfortContainer.current != ComfortContainer.Ground
    Row(
        modifier
            .height(36.dp)
            .clip(ComfortShapes.Pill)
            .background(if (selected) colors.accentSoft else if (onCard) colors.fill else colors.card)
            .selectable(selected, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) ComfortIcon(Symbol.Check, size = 18.dp, tint = colors.onAccentSoft, modifier = Modifier.offset(x = (-4).dp))
        Text(
            label,
            style = onest(14f, 18f, if (selected) 600 else 500),
            color = if (selected) colors.onAccentSoft else colors.on,
            maxLines = 1,
            modifier = if (selected) Modifier.offset(x = (-4).dp) else Modifier,
        )
    }
}

/**
 * `.search`: a card-coloured capsule (fill on a card or in a sheet) with the search glyph, the query
 * and, once there is text, a clear button. The caret is the accent.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
) {
    val colors = Comfort.colors
    val onCard = LocalComfortContainer.current != ComfortContainer.Ground
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clip(ComfortShapes.Pill)
            .background(if (onCard) colors.fill else colors.card)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComfortIcon(Symbol.Search, tint = colors.on2)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                // The field itself carries the placeholder as its label, so this copy is hidden from TalkBack.
                Text(
                    placeholder,
                    style = onest(16f, 22f),
                    color = colors.on3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = onest(16f, 22f).copy(color = colors.on),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = placeholder }
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
        }
        if (query.isNotEmpty()) {
            ComfortIconButton(Symbol.Close, stringResource(R.string.search_clear_cd), { onQueryChange("") }, dim = true)
        } else {
            Spacer(Modifier.width(4.dp))
        }
    }
}

/** A clickable stand-in for [SearchField] that opens search elsewhere (Library's search glyph target). */
@Composable
fun SearchFieldButton(placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val onCard = LocalComfortContainer.current != ComfortContainer.Ground
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clip(ComfortShapes.Pill)
            .background(if (onCard) colors.fill else colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComfortIcon(Symbol.Search, tint = colors.on2)
        Text(placeholder, style = onest(16f, 22f), color = colors.on3, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

