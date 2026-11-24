package app.attic.player.ui.comfort

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.ui.expressive.Symbol

/**
 * `.switch`: a 52×30 pill track (`--fill-2` off, accent on) with a 24dp white knob that carries the
 * small shadow. Drawn here: Material's Switch has a 16dp knob when off and an outlined track, which the
 * kit does not. Pass [onCheckedChange] null when the whole row toggles it.
 */
@Composable
fun ComfortSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = Comfort.colors
    val track by animateColorAsState(if (checked) colors.accent else colors.fill2, tween(160), label = "track")
    val knob by animateDpAsState(if (checked) 25.dp else 3.dp, tween(160), label = "knob")
    Box(
        modifier
            .then(
                if (onCheckedChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                else Modifier,
            )
            .alpha(if (enabled) 1f else 0.38f)
            .size(width = 52.dp, height = 30.dp)
            .clip(ComfortShapes.Pill)
            .background(track),
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 3.dp)
                .size(24.dp)
                .floatingShadow(ComfortShapes.Circle, small = true, edge = false)
                .clip(ComfortShapes.Circle)
                .background(colors.knob),
        )
    }
}

/**
 * `.radio`: a 24dp ring in `--on-3`, accent with a 12dp dot when [selected]. Drawn here for the kit's
 * 24dp size (Material's is 20). Pass [onClick] null when the whole row selects it.
 */
@Composable
fun ComfortRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    val ring by animateColorAsState(if (selected) colors.accent else colors.on3, tween(160), label = "ring")
    Box(
        modifier
            .then(if (onClick != null) Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick) else Modifier)
            .size(24.dp)
            .border(2.dp, ring, ComfortShapes.Circle),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(12.dp).clip(ComfortShapes.Circle).background(colors.accent))
    }
}

/**
 * `.progress`: a 10dp capsule (16 with [thick]) that fills with the accent. Progress is never a thin
 * line in this kit.
 */
@Composable
fun ComfortProgress(progress: () -> Float, modifier: Modifier = Modifier, thick: Boolean = false) {
    val colors = Comfort.colors
    val height = if (thick) 16.dp else 10.dp
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(ComfortShapes.Pill)
            .background(colors.fill2)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f) }
            .drawBehind {
                val fraction = progress().coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRoundRect(colors.accent, size = Size(size.width * fraction, size.height), cornerRadius = CornerRadius(size.height / 2))
                }
            },
    )
}

/**
 * `.spinner`: a 36dp ring (22 with [small]) in `--fill-2`, half of it in the accent, turning. One of the
 * kit's three circles.
 */
@Composable
fun Spinner(modifier: Modifier = Modifier, small: Boolean = false) {
    val colors = Comfort.colors
    val side = if (small) 22.dp else 36.dp
    val stroke = if (small) 3.dp else 4.dp
    val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "turn",
    )
    Canvas(modifier.size(side).rotate(28f + turn).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val width = stroke.toPx()
        val inset = width / 2
        val arcSize = Size(size.width - width, size.height - width)
        drawArc(colors.fill2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width))
        drawArc(colors.accent, 225f, 180f, false, Offset(inset, inset), arcSize, style = Stroke(width, cap = StrokeCap.Butt))
    }
}

/**
 * `.set-row`: a settings row. [title] (16.5/22) and [summary] (13.5/19 in `--on-2`) wrap. [leading] is
 * a radio (`.lead-ctl`); [trailing] a switch, a [value] in accent ink, or a chevron. Use
 * [SetSwitchRow] and [SetRadioRow] for the common two.
 */
@Composable
fun SetRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    clickModifier: Modifier = Modifier.rowClick(onClick),
) {
    val colors = Comfort.colors
    Row(
        modifier
            .fillMaxWidth()
            .hairlineAbove(LocalHairlineAbove.current, if (leading != null) 60.dp else 20.dp)
            .then(clickModifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = onest(16.5f, 22f, 500), color = colors.on)
            if (summary != null) Text(summary, style = onest(13.5f, 19f), color = colors.on2, modifier = Modifier.padding(top = 2.dp))
        }
        if (value != null) Text(value, style = onest(14f, 18f), color = colors.accentInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke(this)
    }
}

/** A settings row with a trailing switch; the whole row toggles it. */
@Composable
fun SetSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, summary: String? = null, enabled: Boolean = true) {
    SetRow(
        title,
        modifier,
        summary = summary,
        clickModifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { ComfortSwitch(checked, null, enabled = enabled) },
    )
}

/** A settings row led by a radio; the whole row selects it. */
@Composable
fun SetRadioRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, summary: String? = null) {
    SetRow(
        title,
        modifier,
        summary = summary,
        clickModifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        leading = { ComfortRadio(selected, null) },
    )
}

/**
 * `.set-block`: a paragraph, a progress bar or a button inside a settings card. [afterRow] trims the
 * top padding when it follows a [SetRow] (`.set-row + .set-block`). Use [SetBlockText],
 * [SetBlockSmall], [ComfortProgress] and a `ButtonStyle.Text` button inside.
 */
@Composable
fun SetBlock(modifier: Modifier = Modifier, afterRow: Boolean = false, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = if (afterRow) 2.dp else 14.dp, bottom = 16.dp),
        content = content,
    )
}

/** Body text in a [SetBlock]: 14/20 in `--on-2`. */
@Composable
fun SetBlockText(text: String, modifier: Modifier = Modifier) =
    Text(text, style = onest(14f, 20f), color = Comfort.colors.on2, modifier = modifier)

/** Small print in a [SetBlock] (`small`): 12.5/17 in `--on-3`, 6dp below what precedes it. */
@Composable
fun SetBlockSmall(text: String, modifier: Modifier = Modifier) =
    Text(text, style = onest(12.5f, 17f), color = Comfort.colors.on3, modifier = modifier.padding(top = 6.dp))

/** The spacing the kit gives a progress bar inside a [SetBlock] (14 above, 8 below). */
val SetBlockProgressPadding = Modifier.padding(top = 14.dp, bottom = 8.dp)

/**
 * `.stats`: stat cells two across in one card, split by hairlines: a 26/32 bold tabular figure over a
 * 13/18 label. Put it in a [Group] item.
 */
@Composable
fun Stats(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Column(modifier.fillMaxWidth()) {
        items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                    .then(
                        if (rowIndex > 0) Modifier.drawBehind { drawRect(colors.line, size = Size(size.width, 1.dp.toPx())) } else Modifier,
                    ),
            ) {
                row.forEachIndexed { column, (value, label) ->
                    if (column == 1) Box(Modifier.width(1.dp).fillMaxSize().background(colors.line))
                    Column(Modifier.weight(1f).padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 16.dp)) {
                        Text(value, style = Comfort.type.stat, color = colors.on, maxLines = 1)
                        Text(label, style = onest(13f, 18f), color = colors.on2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) {
                    Box(Modifier.width(1.dp).fillMaxSize().background(colors.line))
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * `.empty`: an icon box (76dp, neutral), a title (20/26 bold) and one sentence, with an optional
 * [action] button 24dp below. [fill] centres it in the space it is given (`.empty.fill`). [art]
 * replaces the icon box (e.g. `{ MarkBox(size = 76.dp) }`).
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    symbol: Symbol = Symbol.MusicNote,
    art: (@Composable () -> Unit)? = null,
    fill: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = Comfort.colors
    Column(
        modifier.then(if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (fill) Arrangement.Center else Arrangement.Top,
    ) {
        if (art != null) art() else IconBox(symbol, size = 76.dp, style = IconBoxStyle.Neutral)
        Text(
            title,
            style = Comfort.type.barTitle,
            color = colors.on,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 22.dp),
        )
        if (body != null) {
            Text(body, style = Comfort.type.body, color = colors.on2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        if (action != null) Box(Modifier.padding(top = 24.dp)) { action() }
    }
}

/**
 * `.actionbar`: replaces the dock in a mode (reorder, selection): a floating card 68dp tall, 12dp in
 * from the edges and 22dp up, holding [ActionBarItem]s.
 */
@Composable
fun ActionBar(
    modifier: Modifier = Modifier,
    insets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = Comfort.colors
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .padding(start = 12.dp, end = 12.dp, bottom = 22.dp)
            .height(68.dp)
            .floatingShadow(ComfortShapes.Floating)
            .clip(ComfortShapes.Floating)
            .background(colors.raised)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** `.act`: icon over a 12/15 label. [go] is the accent action ("Done"); [danger]; `enabled = false` is `.off`. */
@Composable
fun RowScope.ActionBarItem(
    symbol: Symbol,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    go: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = Comfort.colors
    val ink = when {
        danger -> colors.danger
        go -> colors.accentInk
        else -> colors.on
    }
    Column(
        modifier
            .weight(1f)
            .height(56.dp)
            .clip(ComfortShapes.Control)
            .alpha(if (enabled) 1f else 0.38f)
            .then(if (enabled) Modifier.rowClick(onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        ComfortIcon(symbol, tint = ink)
        Text(label, style = onest(12f, 15f, if (go) 700 else 500), color = ink, maxLines = 1)
    }
}

/**
 * `.index`: the letter scrubber beside a long alphabetical list (22dp wide). [current] is lit in an
 * accent capsule. Tapping or dragging calls [onSelect] with the letter's index, and while the finger
 * is down an [IndexPop] shows the letter beside the list ([onScrub] gives the index, or null on release,
 * so the screen can place the pop where it wants; [IndexScrubberWithPop] does it for you).
 */
@Composable
fun IndexScrubber(
    letters: List<String>,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onScrub: (Int?) -> Unit = {},
) {
    val colors = Comfort.colors
    val select by rememberUpdatedState(onSelect)
    val scrub by rememberUpdatedState(onScrub)
    Column(
        modifier
            .width(22.dp)
            .pointerInput(letters.size) {
                fun index(y: Float) = ((y / size.height) * letters.size).toInt().coerceIn(0, letters.size - 1)
                detectVerticalDragGestures(
                    onDragStart = { offset -> index(offset.y).let { select(it); scrub(it) } },
                    onDragEnd = { scrub(null) },
                    onDragCancel = { scrub(null) },
                    onVerticalDrag = { change, _ -> index(change.position.y).let { select(it); scrub(it) } },
                )
            }
            .pointerInput(letters.size) {
                detectTapGestures { offset -> select(((offset.y / size.height) * letters.size).toInt().coerceIn(0, letters.size - 1)) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { index, letter ->
            val on = index == current
            Box(
                Modifier
                    .height(15.5.dp)
                    .then(if (on) Modifier.size(18.dp).clip(ComfortShapes.Pill).background(colors.accent) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = onest(10.5f, 15.5f, 600), color = if (on) colors.onAccent else colors.on3)
            }
        }
    }
}

/** `.index-pop`: the letter under the thumb, 56dp, in an accent superellipse. */
@Composable
fun IndexPop(letter: String, modifier: Modifier = Modifier) {
    val colors = Comfort.colors
    Box(modifier.size(56.dp).background(colors.accent, ComfortShapes.Squircle), contentAlignment = Alignment.Center) {
        Text(letter, style = onest(26f, 32f, 700), color = colors.onAccent)
    }
}

/**
 * [IndexScrubber] with its [IndexPop] 34dp to its left, level with the letter being touched. Place it
 * at the end of the list's box, starting [top] from the top.
 */
@Composable
fun IndexScrubberWithPop(letters: List<String>, current: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, top: Dp = 150.dp) {
    var scrubbing by remember { mutableIntStateOf(-1) }
    Box(modifier.padding(top = top)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.width(67.dp)) {
                if (scrubbing >= 0) IndexPop(letters[scrubbing], Modifier.offset(y = 15.5.dp * scrubbing + 7.75.dp - 28.dp))
            }
            IndexScrubber(letters, current, onSelect, onScrub = { scrubbing = it ?: -1 }, modifier = Modifier.padding(end = 1.dp))
        }
    }
}
