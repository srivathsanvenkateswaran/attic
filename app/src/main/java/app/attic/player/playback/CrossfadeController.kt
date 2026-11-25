package app.attic.player.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.attic.player.analysis.SongTransitions
import app.attic.player.playback.eq.EqSource
import app.attic.player.playback.eq.EqualiserAudioProcessor

/**
 * Crossfades from one song into the next, at the point and for the length [TransitionPlanner] chose:
 * the last few seconds for the Crossfade setting, or around the outgoing song's outro for Smart, entering
 * the next song past its leading silence and, when the tempos allow, with a beat of each on the
 * overlap's middle. Media3 can't overlap two items in one player, so a second,
 * bare ExoPlayer, the tail, plays the end of the outgoing song while the main player moves on:
 *
 * 1. Eight seconds before the fade, the tail opens the song the main player is on, silent, and lines
 *    its position up with the main player's to within 20 ms ([TailAlignment]).
 * 2. When the fade is due, the tail is turned up and the main player moves to the next item, at the
 *    planned entry point, at volume 0, so the outgoing song carries on from the tail without a break.
 * 3. Over the fade the tail goes down and the main player comes up on equal-power curves, both
 *    following the main player's position in the new song. Then the tail stops, even if the outgoing
 *    song had more to play.
 *
 * The main player's first audio in the new song can land a few tens of ms late. Early in each fade the
 * controller measures how far the two songs' positions have slipped against the plan (the join error,
 * logged under `AtticTransition`), and enters the next beat-aligned song that much further in.
 *
 * The main player is the only one the media session, notification, widget and queue ever see; to
 * them a crossfade is an early move to the next item. Each player has its own equaliser reading the
 * same settings, and each resolves the song it is playing, so both songs keep their own curves. If the tail can't line up in time, the song
 * just plays to its end, gapless, as it would with crossfade off.
 *
 * A fade is cut (the tail stops, the main player goes to full volume) by a pause from anywhere, a
 * seek or skip, a change that takes away the song being faded in, an error, or the setting being
 * turned off. Everything here runs on the main thread: a 50 ms check while lining up and a 30 ms
 * volume step while fading, each a couple of position reads and a volume set.
 */
@OptIn(UnstableApi::class)
class CrossfadeController(
    private val context: Context,
    private val main: ExoPlayer,
    private val mainLevelling: LevellingAudioProcessor,
    /**
     * The equaliser's settings. The tail gets its own equaliser reading them, so the outgoing song
     * keeps its own curve while the incoming one plays with its own.
     */
    private val equaliser: EqSource,
    private val mode: () -> TransitionMode,
    private val lengthSeconds: () -> Int,
    /** True while the sleep timer is set to stop at the end of the song, which a fade would skip past. */
    private val stopsAfterThisSong: () -> Boolean,
    private val trackFor: (String) -> LevelTrack?,
    private val transitionsFor: (String) -> SongTransitions?,
    /** The planned start of a Smart transition out of the song [mediaId], or null; for Now Playing's mark. */
    private val onSmartPlan: (mediaId: String?, fadeStartMs: Long?) -> Unit = { _, _ -> },
) {
    private data class Plan(val currentKey: String, val nextKey: String, val transition: Transition, val durationMs: Long)

    private sealed interface State {
        data object Idle : State

        data class Waiting(val plan: Plan) : State

        data class Aligning(
            val plan: Plan,
            var settleUntil: Long,
            var aligned: Boolean = false,
            val armedAt: Long = SystemClock.elapsedRealtime(),
            var seeks: Int = 0,
        ) : State

        data class Fading(val plan: Plan, val startedAt: Long, val joinReadings: MutableList<Long> = ArrayList()) : State
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tailLevelling = LevellingAudioProcessor { _, _, _ -> null }
    private val tailEqualiser = EqualiserAudioProcessor(equaliser)
    private var tail: ExoPlayer? = null
    private val alignment = TailAlignment()
    private var state: State = State.Idle
    private var masterVolume = 1f
    private var mainRamp = 1f
    private var tailRamp = 0f

    /** How far past the planned entry point to seek a beat-aligned song, learned from each fade's join error. */
    private var entryLeadMs = 0L
    private var publishedPlan: Pair<String?, Long?> = null to null

    /** True only while the main player is being moved to the next song for a fade. */
    var isOwnTransition: Boolean = false
        private set

    private val evaluate = Runnable { evaluate() }
    private val alignTick = Runnable { alignTick() }
    private val fadeTick = Runnable { fadeTick() }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isOwnTransition) return
            if (state is State.Fading) cut("a move to another song")
            replan()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (isOwnTransition) return
            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                // A seek or skip by the listener. Mid-fade it ends the fade; before one, the tail has to line up again.
                cut("a seek")
                replan()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) cut("a pause") else replan()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val fading = state as? State.Fading
            if (fading != null) {
                if (main.currentMediaItem?.mediaId != fading.plan.nextKey) cut("a queue change")
                return
            }
            replan()
        }

        override fun onRepeatModeChanged(repeatMode: Int) = replan()

        override fun onPlayerError(error: PlaybackException) = cut("an error")
    }

    init {
        main.addListener(listener)
    }

    /** The sleep timer's fade, applied on top of the crossfade's own volumes. */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume
        applyVolumes()
    }

    /** The setting or the sleep timer changed. */
    fun settingsChanged() {
        if (state is State.Fading && mode() == TransitionMode.Off) cut("the setting turned off")
        replan()
    }

    fun release() {
        cancel()
        pending = null
        planning.shutdownNow()
        main.removeListener(listener)
        tail?.release()
        tail = null
    }

    /** Works out whether this song fades into the next, and when to start getting ready. */
    /**
     * Works out whether this song fades into the next, and when to start getting ready. The inputs are
     * read here on the main thread; the planning itself (a fraction of a millisecond on a fast phone,
     * several in a debug build) runs on [planning], and its result comes back here. A result is kept
     * against its inputs, so the replans a radio refill or a timeline change set off cost nothing.
     */
    private fun replan() {
        if (state is State.Fading) return
        val inputs = inputsNow()
        if (inputs == null) {
            pending = null
            apply(null, null)
            return
        }
        val known = planned
        if (known != null && known.first == inputs) {
            apply(inputs, known.second)
            return
        }
        if (inputs == pending) return
        // Until the new plan arrives, nothing is armed for an old one.
        if (state !is State.Fading) cancel()
        pending = inputs
        if (planning.isShutdown) return
        planning.execute {
            val started = System.nanoTime()
            val transition = TransitionPlanner.plan(
                mode = inputs.mode,
                lengthSeconds = inputs.lengthSeconds,
                current = inputs.current,
                next = inputs.next,
                currentDurationMs = inputs.durationMs,
                nextDurationMs = inputs.nextDurationMs,
                repeatOne = inputs.repeatOne,
                stopsAfterThisSong = inputs.stopsAfterThisSong,
                outgoing = inputs.outgoing,
                incoming = inputs.incoming,
            )
            val micros = (System.nanoTime() - started) / 1_000
            handler.post {
                if (pending != inputs) return@post
                pending = null
                planned = inputs to transition
                if (transition != null) {
                    Log.i(TAG, "Plan ${describe(transition)}, planned in $micros µs off the main thread: ${inputs.currentKey} → ${inputs.nextKey}")
                }
                if (state !is State.Fading) apply(inputs, transition)
            }
        }
    }

    /** Puts a plan in place: publishes it and waits for its moment, or clears everything when there is none. */
    private fun apply(inputs: PlanInputs?, transition: Transition?) {
        val nextKey = inputs?.nextKey
        val plan = if (inputs == null || transition == null || nextKey == null) null else Plan(inputs.currentKey, nextKey, transition, inputs.durationMs)
        publish(plan)
        if (plan == null) {
            if (lastLogged != null && inputs != null) Log.i(TAG, "No transition out of ${inputs.currentKey}")
            lastLogged = null
            cancel()
            return
        }
        lastLogged = plan
        val aligning = state as? State.Aligning
        if (aligning != null && aligning.plan == plan) return
        val waiting = state as? State.Waiting
        if (waiting != null && waiting.plan == plan) return
        cancel()
        state = State.Waiting(plan)
        evaluate()
    }

    /** Everything a plan depends on, read from the player and settings on the main thread. */
    private data class PlanInputs(
        val currentKey: String,
        val nextKey: String?,
        val mode: TransitionMode,
        val lengthSeconds: Int,
        val current: LevelTrack,
        val next: LevelTrack?,
        val durationMs: Long,
        val nextDurationMs: Long?,
        val repeatOne: Boolean,
        val stopsAfterThisSong: Boolean,
        val outgoing: SongTransitions?,
        val incoming: SongTransitions?,
    )

    private fun inputsNow(): PlanInputs? {
        val item = main.currentMediaItem ?: return null
        if (!main.playWhenReady) return null
        val nextIndex = main.nextMediaItemIndex
        val next = if (nextIndex == C.INDEX_UNSET) null else main.getMediaItemAt(nextIndex)
        val duration = main.duration.takeIf { it != C.TIME_UNSET } ?: item.mediaMetadata.durationMs ?: return null
        return PlanInputs(
            currentKey = item.mediaId,
            nextKey = next?.mediaId,
            mode = mode(),
            lengthSeconds = lengthSeconds(),
            current = levelTrack(item.mediaId),
            next = next?.let { levelTrack(it.mediaId) },
            durationMs = duration,
            nextDurationMs = next?.mediaMetadata?.durationMs,
            repeatOne = main.repeatMode == Player.REPEAT_MODE_ONE,
            stopsAfterThisSong = stopsAfterThisSong(),
            outgoing = transitionsFor(item.mediaId),
            incoming = next?.let { transitionsFor(it.mediaId) },
        )
    }

    private val planning = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "AtticTransitionPlanner").apply { isDaemon = true }
    }
    private var pending: PlanInputs? = null
    private var planned: Pair<PlanInputs, Transition?>? = null
    private var lastLogged: Plan? = null

    private fun describe(t: Transition): String = buildString {
        append("${t.kind} fade ${t.fadeMs} ms at ${t.fadeStartMs} ms, enters at ${t.incomingStartMs} ms")
        append(", bpm ${t.outBpm?.let { "%.2f".format(it) } ?: "-"} → ${t.inBpm?.let { "%.2f".format(it) } ?: "-"}")
        append(if (t.beatAligned) ", beats aligned, drift up to ${t.expectedDriftMs} ms" else ", beats not aligned")
    }

    private fun publish(plan: Plan?) {
        val smart = plan?.transition?.takeIf { it.kind == Transition.Kind.Smart }
        // The point where the seek bar moves to the next song, which the mark stands for.
        val now = (if (smart == null) null else plan.currentKey) to smart?.let(::joinAt)
        if (now == publishedPlan) return
        publishedPlan = now
        onSmartPlan(now.first, now.second)
    }

    private fun levelTrack(key: String): LevelTrack = trackFor(key) ?: LevelTrack(key, null, null)

    private fun evaluate() {
        val waiting = state as? State.Waiting ?: return
        if (main.currentMediaItem?.mediaId != waiting.plan.currentKey) {
            replan()
            return
        }
        when (val step = CrossfadePlan.step(main.currentPosition, joinAt(waiting.plan.transition))) {
            is CrossfadePlan.Step.Wait -> handler.postDelayed(evaluate, step.ms)
            CrossfadePlan.Step.Arm -> arm(waiting.plan)
            CrossfadePlan.Step.TooLate -> state = State.Idle
        }
    }

    private fun arm(plan: Plan) {
        val item = main.currentMediaItem ?: return
        val tail = tailPlayer()
        // The tail plays the end of a song the main player is already levelling, so it takes that gain.
        tailLevelling.fixedGainDb = if (mainLevelling.enabled) mainLevelling.streamGainDb else 0f
        tailRamp = 0f
        applyVolumes()
        tail.setMediaItem(item)
        tail.prepare()
        val aligning = State.Aligning(plan, settleUntil = 0)
        state = aligning
        seekTail(aligning)
        tail.play()
        handler.post(alignTick)
    }

    private fun seekTail(aligning: State.Aligning) {
        aligning.seeks++
        alignment.reset()
        tail?.seekTo(alignment.seekTarget(main.currentPosition))
        aligning.settleUntil = SystemClock.elapsedRealtime() + SETTLE_MS
    }

    private fun alignTick() {
        val aligning = state as? State.Aligning ?: return
        val tail = tail ?: return
        val fadeStart = joinAt(aligning.plan.transition)
        val position = main.currentPosition
        if (position >= fadeStart) {
            if (aligning.aligned) {
                startFade(aligning.plan)
            } else {
                Log.i(
                    TAG,
                    "Couldn't line the tail up in ${SystemClock.elapsedRealtime() - aligning.armedAt} ms (${aligning.seeks} seeks, " +
                        "tail ${if (tail.isPlaying) "playing" else "state ${tail.playbackState}"}, lead ${alignment.leadMs} ms), " +
                        "so the song ends as it would without a transition: ${aligning.plan.currentKey}",
                )
                cancel()
            }
            return
        }
        if (!aligning.aligned && tail.isPlaying && SystemClock.elapsedRealtime() >= aligning.settleUntil) {
            when (alignment.record(tail.currentPosition - main.currentPosition)) {
                TailAlignment.Verdict.Aligned -> {
                    aligning.aligned = true
                    Log.i(TAG, "Tail lined up in ${SystemClock.elapsedRealtime() - aligning.armedAt} ms after ${aligning.seeks} seeks, lead ${alignment.leadMs} ms")
                }
                TailAlignment.Verdict.Reseek -> {
                    Log.i(TAG, "Tail ${alignment.lastMedianMs} ms out, seeking it again with lead ${alignment.leadMs} ms")
                    seekTail(aligning)
                }
                TailAlignment.Verdict.NeedMore -> Unit
            }
        }
        handler.postDelayed(alignTick, (fadeStart - position).coerceIn(1, ALIGN_TICK_MS))
    }

    private fun startFade(plan: Plan) {
        val nextIndex = main.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || main.getMediaItemAt(nextIndex).mediaId != plan.nextKey) {
            cancel()
            return
        }
        state = State.Fading(plan, SystemClock.elapsedRealtime())
        // The tail takes over the outgoing song at full level as the main player leaves it.
        tailRamp = 1f
        mainRamp = 0f
        applyVolumes()
        val t = plan.transition
        isOwnTransition = true
        try {
            main.seekTo(nextIndex, seekTarget(t))
        } finally {
            isOwnTransition = false
        }
        handler.post(fadeTick)
    }

    /**
     * Where in the outgoing song the main player moves to the next one. A seek into the middle of the next
     * file makes the main player buffer for a few hundred ms, so when the next song's leading silence is
     * short the move happens that much earlier, to the next song's start, which ExoPlayer has already
     * loaded: the tail plays on at full level over the silence and the overlap is the planned one. For
     * beat-aligned songs the learned [entryLeadMs] moves it earlier still, to make up the main player's
     * usual lateness.
     */
    private fun joinAt(t: Transition): Long =
        if (joinsAtStart(t)) t.fadeStartMs - t.incomingStartMs - (if (t.beatAligned) entryLeadMs else 0L) else t.fadeStartMs

    private fun seekTarget(t: Transition): Long = when {
        joinsAtStart(t) -> 0L
        else -> t.incomingStartMs + if (t.beatAligned) entryLeadMs else 0L
    }

    private fun joinsAtStart(t: Transition) = t.incomingStartMs in 1..MAX_SILENCE_TO_PLAY_MS

    private fun fadeTick() {
        val fading = state as? State.Fading ?: return
        val tail = tail
        val t = fading.plan.transition
        val progress = (main.currentPosition - t.incomingStartMs).toFloat() / t.fadeMs
        if (tail == null || progress >= 1f || tail.playbackState == Player.STATE_ENDED) {
            finishFade()
            return
        }
        measureJoin(fading, tail)
        tailRamp = CrossfadePlan.outgoingVolume(progress)
        mainRamp = CrossfadePlan.incomingVolume(progress)
        applyVolumes()
        handler.postDelayed(fadeTick, FADE_TICK_MS)
    }

    /**
     * Between [JOIN_FROM_MS] and [JOIN_UNTIL_MS] into a fade, reads how far each player has got since the
     * join against the plan. Positive means the outgoing song is ahead: the incoming song started late.
     */
    private fun measureJoin(fading: State.Fading, tail: ExoPlayer) {
        val since = SystemClock.elapsedRealtime() - fading.startedAt
        if (since < JOIN_FROM_MS || since > JOIN_UNTIL_MS || fading.joinReadings.size >= JOIN_READINGS) return
        if (!main.isPlaying || !tail.isPlaying) return
        val t = fading.plan.transition
        fading.joinReadings += (tail.currentPosition - t.fadeStartMs) - (main.currentPosition - t.incomingStartMs)
        if (fading.joinReadings.size < JOIN_READINGS) return
        val error = fading.joinReadings.sorted()[JOIN_READINGS / 2]
        val usedLead = if (t.beatAligned) entryLeadMs else 0L
        // The error already includes the lead this fade used, so the next lead is that plus the error.
        entryLeadMs = (usedLead + error).coerceIn(-MAX_ENTRY_LEAD_MS, MAX_ENTRY_LEAD_MS)
        Log.i(
            TAG,
            "Join error $error ms (${if (t.beatAligned) "beats aligned" else "not aligned"}, entry lead was $usedLead ms, " +
                "next $entryLeadMs ms): ${fading.plan.currentKey} → ${fading.plan.nextKey}",
        )
    }

    private fun finishFade() {
        (state as? State.Fading)?.let { Log.i(TAG, "Fade finished after ${SystemClock.elapsedRealtime() - it.startedAt} ms: ${it.plan.nextKey}") }
        cancel()
        replan()
    }

    /** Stops a fade halfway: the tail goes, the new song plays at full volume. */
    private fun cut(why: String) {
        val fading = state as? State.Fading
        if (fading != null) {
            Log.i(TAG, "Cut the fade by $why, ${SystemClock.elapsedRealtime() - fading.startedAt} ms in: ${fading.plan.currentKey} → ${fading.plan.nextKey}")
        }
        cancel()
    }

    private fun cancel() {
        handler.removeCallbacks(evaluate)
        handler.removeCallbacks(alignTick)
        handler.removeCallbacks(fadeTick)
        tail?.let {
            it.stop()
            it.clearMediaItems()
        }
        state = State.Idle
        tailRamp = 0f
        mainRamp = 1f
        applyVolumes()
    }

    private fun applyVolumes() {
        main.volume = masterVolume * mainRamp
        tail?.volume = masterVolume * tailRamp
    }

    /**
     * Built once and kept, so a fade costs no player construction. It never asks for audio focus (the
     * main player holds it) and shares the main player's audio session, so an equaliser attached to
     * Attic shapes both.
     */
    private fun tailPlayer(): ExoPlayer = tail ?: ExoPlayer.Builder(context, LevellingRenderersFactory(context, tailLevelling, tailEqualiser))
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ false,
        )
        .build()
        .also { player ->
            player.setAudioSessionId(main.audioSessionId)
            player.volume = 0f
            player.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) = cancel()
                },
            )
            tail = player
        }

    private companion object {
        /** A seek's first position readings are unreliable; wait this long before trusting them. */
        const val SETTLE_MS = 400L
        const val ALIGN_TICK_MS = 50L
        const val FADE_TICK_MS = 30L
        const val TAG = "AtticTransition"
        const val JOIN_FROM_MS = 250L
        const val JOIN_UNTIL_MS = 1_200L
        const val JOIN_READINGS = 5
        const val MAX_ENTRY_LEAD_MS = 300L

        /** Leading silence up to this long is played (under the tail) rather than seeked past. */
        const val MAX_SILENCE_TO_PLAY_MS = 3_000L
    }
}
