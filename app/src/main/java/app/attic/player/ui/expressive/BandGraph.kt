package app.attic.player.ui.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.attic.player.R
import app.attic.player.playback.eq.EqCurve
import app.attic.player.ui.Haptic
import app.attic.player.ui.model.EqualiserBands
import app.attic.player.ui.rememberHaptics
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

private val PlotHeight = 192.dp

/** Room above and below the plot for a finger at ±12 dB; the value bubble is drawn in the room above. */
private val PlotRoom = 20.dp

/** The gutter that holds "+12", "0" and "−12", and the margin after the last band. */
private val PlotStart = 30.dp
private val PlotEnd = 10.dp
private val DbHeight = 8.dp

/** The response may pass ±12 dB where neighbouring boosts add up; it is drawn up to here. */
private const val ResponseLimitDb = 14.5f
private const val ResponsePoints = 96
private const val SettleMillis = 200
private const val PressMillis = 100

/** One band's new value on its way to the engine. */
private data class BandChange(val band: Int, val gainDb: Float)

/**
 * What the graph draws and what the finger is doing. Everything the drawing needs is snapshot state
 * read in the draw phase only, so a drag redraws the canvas and recomposes nothing.
 */
@Stable
private class BandGraphState(curve: EqCurve) {
    /**
     * The gains on screen. One array, changed in place and assigned back to itself: with
     * [neverEqualPolicy] that is a write, so the canvas redraws without an allocation per frame.
     */
    var gains by mutableStateOf(curve.toGains(), neverEqualPolicy())
        private set

    /** The band that is drawn wide with its value above it, or -1. It outlives the drag by the time [press] takes to fall. */
    var held by mutableIntStateOf(-1)
        private set

    /** 0 at rest, 1 while a band is dragged: the bar's extra width and the bubble's opacity. */
    val press = Animatable(0f)

    /** The change the engine has not been told yet. Collected at most once a frame. */
    var outgoing by mutableStateOf<BandChange?>(null)
        private set

    private var sent: BandChange? = null
    private var dragging = false
    private var settling: Job? = null

    /** The curve the model last gave, which the drawing returns to when a drag ends on anything else. */
    var latest: EqCurve = curve

    fun shows(curve: EqCurve): Boolean = (0 until EqualiserBands.COUNT).all { gains[it] == curve[it] }

    /** A finger has started moving [band]. Returns the gain it moves from. */
    fun begin(band: Int): Float {
        // A preset still easing in is finished at once, so the drag starts from where the band will be.
        settling?.let {
            it.cancel()
            latest.copyInto(gains)
            gains = gains
        }
        dragging = true
        held = band
        outgoing = null
        sent = null
        return gains[band]
    }

    fun move(band: Int, gainDb: Float) {
        gains[band] = gainDb
        gains = gains
        outgoing = BandChange(band, gainDb)
    }

    /**
     * The finger is up. Returns the curve to ease to when the model's moved under the drag (the next
     * song started, say), keeping the dragged band where the finger left it; null when nothing did.
     */
    fun end(band: Int): FloatArray? {
        dragging = false
        val target = latest.toGains().also { it[band] = gains[band] }
        return target.takeUnless { it.contentEquals(gains) }
    }

    /** The bar has narrowed again; unless another drag began meanwhile, nothing is held. */
    fun release(band: Int) {
        if (!dragging && held == band) held = -1
    }

    /** Tells the engine the change it has not heard, if there is one. */
    fun flush(send: (Int, Float) -> Unit) {
        val change = outgoing ?: return
        if (change == sent) return
        sent = change
        send(change.band, change.gainDb)
    }

    /** The model's curve changed: ease every band to it, unless a finger is on the graph or it is what is drawn. */
    suspend fun follow(curve: EqCurve) {
        latest = curve
        if (dragging || shows(curve)) return
        settle(curve.toGains())
    }

    /** Eases every band to [target] over 200 ms, one animation driving all ten; the response follows, drawn from the same array. */
    suspend fun settle(target: FloatArray) = coroutineScope {
        settling?.cancel()
        settling = coroutineContext.job
        val from = gains.copyOf()
        try {
            animate(0f, 1f, animationSpec = tween(SettleMillis)) { fraction, _ ->
                for (i in from.indices) gains[i] = from[i] + (target[i] - from[i]) * fraction
                gains = gains
            }
        } finally {
            if (settling === coroutineContext.job) settling = null
        }
    }
}

private fun EqCurve.toGains(): FloatArray = FloatArray(EqualiserBands.COUNT) { this[it] }

private fun EqCurve.copyInto(gains: FloatArray) {
    for (i in gains.indices) gains[i] = this[i]
}

/**
 * Where a band lands [deltaDb] from [from]: on whole decibels, clamped to the range. A band resting
 * between two (a preset's 2.5) stays put until the finger has gone half a decibel, so it never jumps
 * when the drag begins.
 */
private fun snapped(from: Float, deltaDb: Float): Float =
    if (abs(deltaDb) < 0.5f) from else (from + deltaDb).roundToInt().toFloat().coerceIn(EqualiserBands.MIN_DB, EqualiserBands.MAX_DB)

/** True when going from [from] to [to] reaches or passes 0 dB. Leaving 0 is not a crossing. */
private fun crossesZero(from: Float, to: Float): Boolean = (from > 0f && to <= 0f) || (from < 0f && to >= 0f)

/**
 * The band graph (frames B to E, K2): ten bars on a shared 0 dB line, which are the sliders, with the
 * response the engine reports as a soft shape behind them. One canvas draws the tracks, the line, the
 * response, the bars, the axis and frequency labels and the value bubble.
 *
 * Drag up or down anywhere in a band's column and its bar follows from where it is, a decibel at a
 * time, with a tick on reaching 0 dB. A tap does nothing, so a resting thumb can't change the sound; a
 * double tap sets the band to 0. While a band is dragged its bar widens from 14 to 18dp and its value
 * rides above it.
 *
 * Nothing here recomposes during a drag: the gains are state read only while drawing, the response is
 * recomputed in the draw cache when they change, and [onBand] hears at most one change a frame. A new
 * [curve] from the model (a preset, the next song) is eased to over 200 ms. Off ([on] false), the bars
 * lose the accent and the response is hidden, since nothing is applied.
 *
 * For a screen reader each band is a slider of its own, "31 Hz" at "+4 dB", stepping by a decibel.
 */
@Composable
fun BandGraph(curve: EqCurve, on: Boolean, onBand: (band: Int, gainDb: Float) -> Unit, modifier: Modifier = Modifier) {
    val graph = remember { BandGraphState(curve) }
    val send by rememberUpdatedState(onBand)
    val haptics = rememberHaptics()
    LaunchedEffect(curve) { graph.follow(curve) }
    LaunchedEffect(graph) {
        snapshotFlow { graph.outgoing }.conflate().collect {
            graph.flush(send)
            // Nothing more goes out until the next frame; a change made meanwhile replaces this one.
            withFrameNanos { }
        }
    }

    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val live by animateFloatAsState(if (on) 1f else 0f, ExpressiveMotion.defaultEffects(), label = "graphLive")
    val measurer = rememberTextMeasurer()
    val context = LocalContext.current
    // Remembered, so the draw cache below is rebuilt for a new size or font and not for every new curve.
    val typography = MaterialTheme.typography
    val labelStyle = typography.labelSmall
    val axisStyle = remember(typography) { typography.labelSmall.copy(fontFeatureSettings = "tnum") }
    val bubbleStyle = remember(typography) { typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum") }
    val axis = remember { listOf(EqualiserBands.MAX_DB, 0f, EqualiserBands.MIN_DB).map { EqualiserBands.formatDb(it) } }
    // The labels' line is 16sp, so at a larger font size the canvas grows with them.
    val labelLine = with(LocalDensity.current) { max(16.dp.toPx(), 16.sp.toPx()).toDp() }

    // Set through the model, which sends the curve back, so the bar eases to its place like a preset does.
    val set: (Int, Float) -> Unit = remember(graph) {
        { band, gainDb ->
            if (crossesZero(graph.gains[band], gainDb)) haptics.perform(Haptic.Tick)
            send(band, gainDb)
        }
    }

    // Frequency runs low to high from the left in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier
                .fillMaxWidth()
                .height(PlotRoom + PlotHeight + 8.dp + labelLine)
                .drawWithCache {
                    val left = PlotStart.toPx()
                    val top = PlotRoom.toPx()
                    val width = size.width - left - PlotEnd.toPx()
                    val height = PlotHeight.toPx()
                    val zero = top + height / 2
                    val perDb = DbHeight.toPx()
                    val column = width / EqualiserBands.COUNT

                    val axisLabels = axis.map { measurer.measure(it, axisStyle) }
                    // A frequency label wider than its column (a large font size) is set smaller, to fit.
                    val widest = EqualiserBands.labels.maxOf { measurer.measure(it, labelStyle).size.width }
                    val fit = min(1f, (column - 2.dp.toPx()) / widest)
                    val frequencyStyle = if (fit < 1f) labelStyle.copy(fontSize = labelStyle.fontSize * fit) else labelStyle
                    val frequencyLabels = EqualiserBands.labels.map { measurer.measure(it, frequencyStyle) }

                    val line = Path()
                    val area = Path()
                    val drawn = FloatArray(EqualiserBands.COUNT) { Float.NaN }
                    var bubbleGain = Float.NaN
                    var bubbleText: TextLayoutResult? = null

                    onDrawBehind {
                        val gains = graph.gains
                        if (!gains.contentEquals(drawn)) {
                            gains.copyInto(drawn)
                            responsePath(EqualiserBands.response(gains, ResponsePoints), left, width, zero, perDb, line, area)
                        }
                        val ink = colors
                        val strength = live

                        for (band in 0 until EqualiserBands.COUNT) {
                            val x = left + column * (band + 0.5f)
                            drawLine(ink.outlineVariant, Offset(x, top), Offset(x, top + height), 2.dp.toPx(), StrokeCap.Round)
                        }
                        drawLine(ink.outline, Offset(left, zero), Offset(left + width, zero), 1.dp.toPx())

                        if (strength > 0f) {
                            drawPath(area, ink.primary, alpha = 0.16f * strength)
                            drawPath(line, ink.primary, alpha = 0.8f * strength, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }

                        val bar = lerp(ink.outline, ink.primary, strength)
                        val held = graph.held
                        val press = graph.press.value
                        for (band in 0 until EqualiserBands.COUNT) {
                            val x = left + column * (band + 0.5f)
                            val grown = if (band == held) press else 0f
                            val length = abs(gains[band]) * perDb
                            if (length >= 3.dp.toPx()) {
                                val half = (7.dp + 2.dp * grown).toPx()
                                val up = gains[band] > 0f
                                drawRoundRect(bar, Offset(x - half, if (up) zero - length else zero), Size(2 * half, length), CornerRadius(min(half, length / 2)))
                                // The end on the 0 dB line is squarer than the end that moves.
                                if (length > half) drawRoundRect(bar, Offset(x - half, if (up) zero - half else zero), Size(2 * half, half), CornerRadius(2.dp.toPx()))
                            } else {
                                // Too short to read as a bar: a handle on the value, so a band at 0 can still be seen and taken.
                                val half = (8.dp + 2.dp * grown).toPx()
                                drawRoundRect(bar, Offset(x - half, zero - gains[band] * perDb - 3.dp.toPx()), Size(2 * half, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                            }
                        }

                        val labelAlpha = 0.6f + 0.4f * strength
                        axisLabels.forEachIndexed { index, label ->
                            val y = top + height * index / 2
                            drawText(label, ink.onSurfaceVariant, Offset(left - 6.dp.toPx() - label.size.width, y - label.size.height / 2f), alpha = labelAlpha)
                        }
                        val labelsTop = top + height + 8.dp.toPx()
                        frequencyLabels.forEachIndexed { band, label ->
                            val x = left + column * (band + 0.5f)
                            drawText(label, ink.onSurfaceVariant, Offset(x - label.size.width / 2f, labelsTop + (labelLine.toPx() - label.size.height) / 2f), alpha = labelAlpha)
                        }

                        if (held >= 0 && press > 0f) {
                            val gain = gains[held]
                            var text = bubbleText
                            if (text == null || gain != bubbleGain) {
                                text = measurer.measure(context.getString(R.string.equaliser_db, EqualiserBands.formatDb(gain)), bubbleStyle)
                                bubbleGain = gain
                                bubbleText = text
                            }
                            val bubble = Size(max(58.dp.toPx(), text.size.width + 20.dp.toPx()), 26.dp.toPx())
                            // 8dp above the bar's upper end, which for a cut is the 0 dB line. Kept inside the cell at the first and last band.
                            val x = (left + column * (held + 0.5f) - bubble.width / 2).coerceIn(4.dp.toPx(), size.width - 4.dp.toPx() - bubble.width)
                            val y = zero - max(gain, 0f) * perDb - 8.dp.toPx() - bubble.height
                            drawRoundRect(ink.inverseSurface, Offset(x, y), bubble, CornerRadius(bubble.height / 2), alpha = press)
                            drawText(text, ink.inverseOnSurface, Offset(x + (bubble.width - text.size.width) / 2, y + (bubble.height - text.size.height) / 2), alpha = press)
                        }
                    }
                }
                .pointerInput(graph) {
                    val left = PlotStart.toPx()
                    val right = PlotEnd.toPx()
                    val perDb = DbHeight.toPx()
                    var tappedBand = -1
                    var tappedAt = 0L
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val band = ((down.position.x - left) / ((size.width - left - right) / EqualiserBands.COUNT)).toInt().coerceIn(0, EqualiserBands.COUNT - 1)
                            var travel = 0f
                            val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                change.consume()
                                travel = overSlop
                            }
                            if (drag == null) {
                                // Lifted without moving, or the gesture went elsewhere. Lifted twice on one band is the double tap.
                                val tap = currentEvent.changes.firstOrNull { it.id == down.id }?.takeIf {
                                    !it.pressed && !it.isConsumed && (it.position - down.position).getDistance() <= viewConfiguration.touchSlop
                                }
                                when {
                                    tap == null -> tappedBand = -1
                                    tappedBand == band && tap.uptimeMillis - tappedAt <= viewConfiguration.doubleTapTimeoutMillis -> {
                                        tappedBand = -1
                                        set(band, 0f)
                                    }
                                    else -> {
                                        tappedBand = band
                                        tappedAt = tap.uptimeMillis
                                    }
                                }
                                return@awaitEachGesture
                            }
                            tappedBand = -1
                            val from = graph.begin(band)
                            launch { graph.press.animateTo(1f, tween(PressMillis)) }
                            var value = from
                            fun track() {
                                val next = snapped(from, -travel / perDb)
                                if (next == value) return
                                if (crossesZero(value, next)) haptics.perform(Haptic.Tick)
                                value = next
                                graph.move(band, next)
                            }
                            track()
                            verticalDrag(drag.id) { change ->
                                travel += change.positionChange().y
                                change.consume()
                                track()
                            }
                            // The last value goes out now, not a frame later, in case the sheet is on its way out.
                            graph.flush(send)
                            val stale = graph.end(band)
                            launch {
                                graph.press.animateTo(0f, tween(PressMillis))
                                graph.release(band)
                            }
                            if (stale != null) launch { graph.settle(stale) }
                        }
                    }
                },
        ) {
            Row(Modifier.padding(start = PlotStart, end = PlotEnd).fillMaxWidth().height(PlotRoom * 2 + PlotHeight)) {
                for (band in 0 until EqualiserBands.COUNT) {
                    BandSlider(band, curve[band], set, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/**
 * Builds the response curve through [response], sampled evenly across the plot, into [line], and the
 * shape between it and the 0 dB line into [area]. The samples are joined through their midpoints, which
 * rounds the corners 96 straight pieces would show.
 */
private fun responsePath(response: FloatArray, left: Float, width: Float, zero: Float, perDb: Float, line: Path, area: Path) {
    val last = response.lastIndex
    fun x(index: Int) = left + width * index / last
    fun y(index: Int) = zero - response[index].coerceIn(-ResponseLimitDb, ResponseLimitDb) * perDb
    line.rewind()
    line.moveTo(x(0), y(0))
    for (index in 1 until last) {
        line.quadraticTo(x(index), y(index), (x(index) + x(index + 1)) / 2, (y(index) + y(index + 1)) / 2)
    }
    line.lineTo(x(last), y(last))
    area.rewind()
    area.addPath(line)
    area.lineTo(left + width, zero)
    area.lineTo(left, zero)
    area.close()
}

/**
 * One band for a screen reader: an empty box over the band's column that reads "31 Hz, +4 dB" and
 * steps by a decibel, with "Set to 0 dB" as an action. Touch is the canvas's, not this box's.
 */
@Composable
private fun BandSlider(band: Int, gainDb: Float, set: (Int, Float) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val name = remember(band, context) { EqualiserBands.describe(context, band) }
    val value = stringResource(R.string.equaliser_db, EqualiserBands.formatDb(gainDb))
    val reset = stringResource(R.string.equaliser_band_reset)
    Box(
        modifier.semantics {
            contentDescription = name
            stateDescription = value
            progressBarRangeInfo = ProgressBarRangeInfo(gainDb, EqualiserBands.MIN_DB..EqualiserBands.MAX_DB, steps = 23)
            setProgress { wanted ->
                val next = wanted.roundToInt().toFloat().coerceIn(EqualiserBands.MIN_DB, EqualiserBands.MAX_DB)
                if (next == gainDb) {
                    false
                } else {
                    set(band, next)
                    true
                }
            }
            customActions = listOf(
                CustomAccessibilityAction(reset) {
                    set(band, 0f)
                    true
                },
            )
        },
    )
}
