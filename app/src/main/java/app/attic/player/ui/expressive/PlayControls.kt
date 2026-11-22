package app.attic.player.ui.expressive

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.attic.player.R
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** The three play controls: 36dp in a progress ring, 56dp beside a list, and the wide one in Now Playing. */
enum class PlaySize { Small, Large, ExtraLarge }

/**
 * The play control (`.pp`): round while paused, squared off while playing. The corner and, for the
 * wide size, the width morph on the fast spatial spring; the glyph cross-fades on fast effects.
 *
 * - [PlaySize.Small]: 36dp in a 52dp box. Give it [progress] for the ring (`.pp.ring`), for places with
 *   no seek bar: mini player, compact header, widget.
 * - [PlaySize.Large]: 56dp (`.pp.l`), e.g. a section's "play all".
 * - [PlaySize.ExtraLarge]: 152×80 paused, 128×80 playing (`.pp.xl`), Now Playing.
 *
 * [tonal] puts it on the secondary container (`.pp.tonal`).
 */
@Composable
fun PlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PlaySize = PlaySize.Small,
    tonal: Boolean = false,
    progress: (() -> Float)? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (tonal) colors.secondaryContainer else colors.primary
    val content = if (tonal) colors.onSecondaryContainer else colors.onPrimary
    val (pausedWidth, playingWidth, height) = when (size) {
        PlaySize.Small -> Triple(36.dp, 36.dp, 36.dp)
        PlaySize.Large -> Triple(56.dp, 56.dp, 56.dp)
        PlaySize.ExtraLarge -> Triple(152.dp, 128.dp, 80.dp)
    }
    val playingCorner = when (size) {
        PlaySize.Small -> 11.dp
        PlaySize.Large -> 18.dp
        PlaySize.ExtraLarge -> 28.dp
    }
    val glyph = when (size) {
        PlaySize.Small -> 22.dp
        PlaySize.Large -> 32.dp
        PlaySize.ExtraLarge -> 44.dp
    }
    val width by animateDpAsState(if (playing) playingWidth else pausedWidth, ExpressiveMotion.fastSpatial(), label = "ppWidth")
    val corner by animateDpAsState(if (playing) playingCorner else height / 2, ExpressiveMotion.fastSpatial(), label = "ppCorner")
    val description = stringResource(if (playing) R.string.pause else R.string.play)
    val box = if (size == PlaySize.Small) 52.dp else null
    val outline = colors.outlineVariant
    val ring = colors.primary
    Box(
        modifier
            .then(if (box != null) Modifier.size(box) else Modifier)
            .then(
                if (progress != null) {
                    Modifier.drawProgressRing(progress, ring, outline)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(corner)
        Box(
            Modifier
                .minimumInteractiveComponentSize()
                .size(width, height)
                .clip(shape)
                .background(container)
                .clickable(enabled = enabled, role = Role.Button, indication = ripple(), interactionSource = null, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(playing, animationSpec = ExpressiveMotion.fastEffects(), label = "ppGlyph") { isPlaying ->
                SymbolIcon(if (isPlaying) Symbol.Pause else Symbol.PlayArrow, size = glyph, filled = true, tint = content)
            }
        }
    }
}

/** The ring around a small play control: elapsed in primary, the rest in outline variant, a gap at each end. */
private fun Modifier.drawProgressRing(progress: () -> Float, on: Color, off: Color): Modifier =
    drawBehind {
        val stroke = 3.dp.toPx()
        val inset = 2.5.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val gap = 14f
        val sweep = (progress().coerceIn(0f, 1f) * 360f)
        val style = Stroke(stroke, cap = StrokeCap.Round)
        if (sweep > 0.5f) drawArc(on, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
        val offStart = if (sweep > 0.5f) -90f + sweep + gap else -90f + gap
        val offSweep = 270f - gap - offStart
        if (offSweep > 0f) drawArc(off, offStart, offSweep, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
    }

/** Previous and next in Now Playing (`.skip`): a 76×80 tonal pill with a 36dp filled glyph. */
@Composable
fun SkipButton(symbol: Symbol, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(76.dp, 80.dp)
            .clip(ExpressiveShapes.Pill)
            .background(colors.secondaryContainer)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(symbol, size = 36.dp, filled = true, tint = colors.onSecondaryContainer)
    }
}

/** Now Playing's transport (`.transport`): previous, the wide play control, next, as one button group. */
@Composable
fun Transport(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        SkipButton(Symbol.SkipPrevious, stringResource(R.string.previous), onPrevious)
        PlayButton(playing, onPlayPause, size = PlaySize.ExtraLarge)
        SkipButton(Symbol.SkipNext, stringResource(R.string.next), onNext)
    }
}

/** A phase from 0 to 1 that advances one cycle per [periodMs] while [running], frame by frame, and holds when not. */
@Composable
internal fun rememberWavePhase(running: Boolean, periodMs: Long = 1000): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last >= 0) phase.floatValue = (phase.floatValue + (now - last) / 1_000_000f / periodMs) % 1f
                last = now
            }
        }
    }
    return phase
}

/** Draws a sine wave from [startX] to [endX] around [centerY]. Phase moves it towards the end. */
internal fun DrawScope.drawWave(
    color: Color,
    startX: Float,
    endX: Float,
    centerY: Float,
    wavelength: Float,
    amplitude: Float,
    phase: Float,
    stroke: Float,
    path: Path = Path(),
) {
    if (endX <= startX) return
    path.reset()
    val step = 1.5f
    var x = startX
    var first = true
    while (x <= endX) {
        val y = centerY + amplitude * sin(2f * PI.toFloat() * ((x - startX) / wavelength - phase))
        if (first) path.moveTo(x, y) else path.lineTo(x, y)
        first = false
        x += step
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
}

/**
 * The "now playing" mark (`.wave`): a short wave that travels while [playing] and lies flat when
 * paused (`.wave.still`). It sits over the current row's cover or in place of its number.
 */
@Composable
fun NowPlayingWave(playing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val amplitude by animateFloatAsState(if (playing) 1f else 0f, ExpressiveMotion.defaultSpatial(), label = "waveAmp")
    val phase by rememberWavePhase(playing)
    Canvas(modifier.size(20.dp, 10.dp)) {
        val stroke = 2.4.dp.toPx()
        drawWave(
            color,
            startX = stroke / 2,
            endX = size.width - stroke / 2,
            centerY = size.height / 2,
            wavelength = 10.dp.toPx(),
            amplitude = 2.dp.toPx() * amplitude,
            phase = phase,
            stroke = stroke,
        )
    }
}

/**
 * The seek bar (`.seek`): a wavy active track while [playing], flat when paused or while dragging; a
 * 4×28dp bar handle with 6dp clear either side; the rest of the track in outline variant. Tap to jump,
 * drag to scrub; [onSeek] gets the fraction on release. [onScrub] hears the fraction under the finger
 * while dragging and null once it lifts, so the elapsed time can follow the drag. 44dp tall.
 */
@Composable
fun SeekBar(
    progress: Float,
    playing: Boolean,
    onSeek: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onScrub: (Float?) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) dragFraction else progress.coerceIn(0f, 1f)
    val waving = playing && !dragging
    val amplitude by animateFloatAsState(if (waving) 1f else 0f, ExpressiveMotion.defaultSpatial(), label = "seekAmp")
    val phase by rememberWavePhase(waving)
    val path = remember { Path() }
    val seek by rememberUpdatedState(onSeek)
    val scrub by rememberUpdatedState(onScrub)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                setProgress { target ->
                    seek(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> seek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        scrub(dragFraction)
                    },
                    onDragEnd = {
                        seek(dragFraction)
                        dragging = false
                        scrub(null)
                    },
                    onDragCancel = {
                        dragging = false
                        scrub(null)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        scrub(dragFraction)
                    },
                )
            },
    ) {
        val track = 4.dp.toPx()
        val thumbWidth = 4.dp.toPx()
        val gap = 6.dp.toPx()
        val cy = size.height / 2
        val thumbX = (track / 2 + gap + thumbWidth / 2) + (size.width - track - 2 * gap - thumbWidth) * shown
        drawWave(
            colors.primary,
            startX = track / 2,
            endX = thumbX - thumbWidth / 2 - gap,
            centerY = cy,
            wavelength = 32.dp.toPx(),
            amplitude = 3.5.dp.toPx() * amplitude,
            phase = phase,
            stroke = track,
            path = path,
        )
        val offStart = thumbX + thumbWidth / 2 + gap
        if (offStart < size.width - track / 2) {
            drawLine(colors.outlineVariant, Offset(offStart, cy), Offset(size.width - track / 2, cy), track, StrokeCap.Round)
        }
        drawRoundRect(
            colors.primary,
            topLeft = Offset(thumbX - thumbWidth / 2, cy - 14.dp.toPx()),
            size = Size(thumbWidth, 28.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbWidth / 2),
        )
    }
}

/**
 * The seek bar's drawing as a progress line that cannot be dragged (Recap's story, sheet 09 D): the
 * wave up to [progress], the bar handle, the rest of the track, and a 4dp stop at each fraction in
 * [stops] still ahead of the handle. The wave travels while [running] and lies flat when not (held).
 */
@Composable
fun StoryProgress(progress: () -> Float, stops: List<Float>, running: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val amplitude by animateFloatAsState(if (running) 1f else 0f, ExpressiveMotion.defaultSpatial(), label = "storyAmp")
    val phase by rememberWavePhase(running)
    val path = remember { Path() }
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        val shown = progress().coerceIn(0f, 1f)
        val track = 4.dp.toPx()
        val thumbWidth = 4.dp.toPx()
        val gap = 6.dp.toPx()
        val cy = size.height / 2
        val thumbX = (track / 2 + gap + thumbWidth / 2) + (size.width - track - 2 * gap - thumbWidth) * shown
        drawWave(
            colors.primary,
            startX = track / 2,
            endX = thumbX - thumbWidth / 2 - gap,
            centerY = cy,
            wavelength = 32.dp.toPx(),
            amplitude = 3.5.dp.toPx() * amplitude,
            phase = phase,
            stroke = track,
            path = path,
        )
        val offStart = thumbX + thumbWidth / 2 + gap
        if (offStart < size.width - track / 2) {
            drawLine(colors.outlineVariant, Offset(offStart, cy), Offset(size.width - track / 2, cy), track, StrokeCap.Round)
        }
        val dot = 2.dp.toPx()
        stops.forEach { stop ->
            val x = size.width * stop
            if (x > offStart + dot) drawCircle(colors.onSurfaceVariant, dot, Offset(x, cy))
        }
        drawRoundRect(
            colors.primary,
            topLeft = Offset(thumbX - thumbWidth / 2, cy - 14.dp.toPx()),
            size = Size(thumbWidth, 28.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbWidth / 2),
        )
    }
}

/** Elapsed and total time under a seek bar (`.seek-times`), in tabular figures. */
@Composable
fun SeekTimes(elapsed: String, total: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(elapsed, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(total, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Linear progress (`.progress`): active part, a 4dp gap, the track and its stop dot. Material's own. */
@Composable
fun LinearProgress(progress: () -> Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(progress = progress, modifier = modifier.fillMaxWidth(), gapSize = 4.dp)
}

/**
 * Wavy linear progress (`.progress.wavy`), for a long job that is running, such as song analysis.
 * The active part is a moving wave; the track and stop dot are as in [LinearProgress]. 10dp tall.
 */
@Composable
fun WavyLinearProgress(progress: () -> Float, modifier: Modifier = Modifier, running: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val phase by rememberWavePhase(running)
    val amplitude by animateFloatAsState(if (running) 1f else 0f, ExpressiveMotion.defaultSpatial(), label = "wavyAmp")
    val path = remember { Path() }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f) },
    ) {
        val p = progress().coerceIn(0f, 1f)
        val stroke = 2.4.dp.toPx()
        val track = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val cy = size.height / 2
        val activeEnd = size.width * p
        drawWave(colors.primary, stroke / 2, activeEnd - stroke / 2, cy, 10.dp.toPx(), 2.dp.toPx() * amplitude, phase, stroke, path)
        val trackStart = if (p > 0f) activeEnd + gap else 0f
        if (trackStart < size.width) {
            drawLine(colors.secondaryContainer, Offset(trackStart + track / 2, cy), Offset(size.width - track / 2, cy), track, StrokeCap.Round)
        }
        drawCircle(colors.primary, radius = 2.dp.toPx(), center = Offset(size.width - 2.dp.toPx(), cy))
    }
}

/** Circular progress (`.spinner`): determinate with [progress], indeterminate without. 40dp, or 24 with [small]. */
@Composable
fun CircularProgress(modifier: Modifier = Modifier, progress: (() -> Float)? = null, small: Boolean = false) {
    val size = if (small) 24.dp else 40.dp
    if (progress != null) {
        CircularProgressIndicator(progress = progress, modifier = modifier.size(size), gapSize = 4.dp)
    } else {
        CircularProgressIndicator(modifier = modifier.size(size))
    }
}

/**
 * The loading indicator (`.loader`): one shape that morphs through Material's set while it turns,
 * for waits under about five seconds (the library scan, downloading lyrics). 48dp. [contained] puts it
 * in a primary-container circle.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, contained: Boolean = false, size: Dp = 48.dp, contentDescription: String? = null) {
    val colors = MaterialTheme.colorScheme
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last >= 0) time.floatValue += (now - last) / 1_000_000f
                last = now
            }
        }
    }
    val shapes = LoaderShapes
    val radii = remember { FloatArray(RadialShape.SAMPLES) }
    val path = remember { Path() }
    Canvas(
        modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        if (contained) drawCircle(colors.primaryContainer)
        val step = 650f
        val t = time.floatValue
        val index = floor(t / step).toInt()
        val within = (t % step) / step
        val morph = FastOutSlowInEasing.transform((within / 0.7f).coerceAtMost(1f))
        val from = shapes[index % shapes.size]
        val to = shapes[(index + 1) % shapes.size]
        RadialShape.lerp(from, to, morph, radii)
        val shape = RadialShape(radii)
        val active = if (contained) 30.dp.toPx() else 38.dp.toPx()
        val rotation = (t / 1000f) * 1.6f + index * (PI.toFloat() / 4f) + morph * (PI.toFloat() / 4f)
        val origin = Offset((this.size.width - active) / 2, (this.size.height - active) / 2)
        translateTo(origin) {
            shape.path(Size(active, active), rotation, path)
            drawPath(path, if (contained) colors.onPrimaryContainer else colors.primary)
        }
    }
}

private inline fun DrawScope.translateTo(offset: Offset, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(offset.x, offset.y)
    block()
    drawContext.transform.translate(-offset.x, -offset.y)
}
