package app.attic.player.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Crossfades from one song into the next. Media3 can't overlap two items in one player, so a second,
 * bare ExoPlayer, the tail, plays the end of the outgoing song while the main player moves on:
 *
 * 1. Five seconds before the fade, the tail opens the song the main player is on, silent, and lines
 *    its position up with the main player's to within 20 ms ([TailAlignment]).
 * 2. When the fade is due, the tail is turned up and the main player skips to the next item at
 *    volume 0, so the outgoing song carries on from the tail without a break.
 * 3. Over the fade the tail goes down and the main player comes up on equal-power curves, both
 *    following the main player's position in the new song. Then the tail stops.
 *
 * The main player is the only one the media session, notification, widget and queue ever see; to
 * them a crossfade is an early move to the next item. If the tail can't line up in time, the song
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
    private val settingSeconds: () -> Int,
    /** True while the sleep timer is set to stop at the end of the song, which a fade would skip past. */
    private val stopsAfterThisSong: () -> Boolean,
    private val trackFor: (String) -> LevelTrack?,
) {
    private data class Plan(val currentKey: String, val nextKey: String, val fadeMs: Long, val durationMs: Long)

    private sealed interface State {
        data object Idle : State

        data class Waiting(val plan: Plan) : State

        data class Aligning(val plan: Plan, var settleUntil: Long, var aligned: Boolean = false) : State

        data class Fading(val plan: Plan) : State
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tailLevelling = LevellingAudioProcessor { _, _, _ -> null }
    private var tail: ExoPlayer? = null
    private val alignment = TailAlignment()
    private var state: State = State.Idle
    private var masterVolume = 1f
    private var mainRamp = 1f
    private var tailRamp = 0f

    /** True only while the main player is being moved to the next song for a fade. */
    var isOwnTransition: Boolean = false
        private set

    private val evaluate = Runnable { evaluate() }
    private val alignTick = Runnable { alignTick() }
    private val fadeTick = Runnable { fadeTick() }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isOwnTransition) return
            if (state is State.Fading) cut()
            replan()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (isOwnTransition) return
            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                // A seek or skip by the listener. Mid-fade it ends the fade; before one, the tail has to line up again.
                cancel()
                replan()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) cancel() else replan()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val fading = state as? State.Fading
            if (fading != null) {
                if (main.currentMediaItem?.mediaId != fading.plan.nextKey) cut()
                return
            }
            replan()
        }

        override fun onRepeatModeChanged(repeatMode: Int) = replan()

        override fun onPlayerError(error: PlaybackException) = cancel()
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
        if (state is State.Fading && settingSeconds() <= 0) cut()
        replan()
    }

    fun release() {
        cancel()
        main.removeListener(listener)
        tail?.release()
        tail = null
    }

    /** Works out whether this song fades into the next, and when to start getting ready. */
    private fun replan() {
        if (state is State.Fading) return
        val plan = planNow()
        if (plan == null) {
            cancel()
            return
        }
        val aligning = state as? State.Aligning
        if (aligning != null && aligning.plan == plan) return
        cancel()
        state = State.Waiting(plan)
        evaluate()
    }

    private fun planNow(): Plan? {
        val item = main.currentMediaItem ?: return null
        if (!main.playWhenReady) return null
        val nextIndex = main.nextMediaItemIndex
        val next = if (nextIndex == C.INDEX_UNSET) null else main.getMediaItemAt(nextIndex)
        val duration = main.duration.takeIf { it != C.TIME_UNSET } ?: item.mediaMetadata.durationMs ?: return null
        val fade = CrossfadePlan.fadeMs(
            settingSeconds = settingSeconds(),
            current = levelTrack(item.mediaId),
            next = next?.let { levelTrack(it.mediaId) },
            currentDurationMs = duration,
            nextDurationMs = next?.mediaMetadata?.durationMs,
            repeatOne = main.repeatMode == Player.REPEAT_MODE_ONE,
            stopsAfterThisSong = stopsAfterThisSong(),
        )
        if (fade <= 0 || next == null) return null
        return Plan(item.mediaId, next.mediaId, fade, duration)
    }

    private fun levelTrack(key: String): LevelTrack = trackFor(key) ?: LevelTrack(key, null, null)

    private fun evaluate() {
        val waiting = state as? State.Waiting ?: return
        if (main.currentMediaItem?.mediaId != waiting.plan.currentKey) {
            replan()
            return
        }
        when (val step = CrossfadePlan.step(main.currentPosition, waiting.plan.durationMs, waiting.plan.fadeMs)) {
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
        alignment.reset()
        tail?.seekTo(alignment.seekTarget(main.currentPosition))
        aligning.settleUntil = SystemClock.elapsedRealtime() + SETTLE_MS
    }

    private fun alignTick() {
        val aligning = state as? State.Aligning ?: return
        val tail = tail ?: return
        val fadeStart = aligning.plan.durationMs - aligning.plan.fadeMs
        val position = main.currentPosition
        if (position >= fadeStart) {
            if (aligning.aligned) startFade(aligning.plan) else cancel()
            return
        }
        if (!aligning.aligned && tail.isPlaying && SystemClock.elapsedRealtime() >= aligning.settleUntil) {
            when (alignment.record(tail.currentPosition - main.currentPosition)) {
                TailAlignment.Verdict.Aligned -> aligning.aligned = true
                TailAlignment.Verdict.Reseek -> seekTail(aligning)
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
        state = State.Fading(plan)
        // The tail takes over the outgoing song at full level as the main player leaves it.
        tailRamp = 1f
        mainRamp = 0f
        applyVolumes()
        isOwnTransition = true
        try {
            main.seekTo(nextIndex, 0)
        } finally {
            isOwnTransition = false
        }
        handler.post(fadeTick)
    }

    private fun fadeTick() {
        val fading = state as? State.Fading ?: return
        val tail = tail
        val progress = main.currentPosition.toFloat() / fading.plan.fadeMs
        if (tail == null || progress >= 1f || tail.playbackState == Player.STATE_ENDED) {
            finishFade()
            return
        }
        tailRamp = CrossfadePlan.outgoingVolume(progress)
        mainRamp = CrossfadePlan.incomingVolume(progress)
        applyVolumes()
        handler.postDelayed(fadeTick, FADE_TICK_MS)
    }

    private fun finishFade() {
        cancel()
        replan()
    }

    /** Stops a fade halfway: the tail goes, the new song plays at full volume. */
    private fun cut() = cancel()

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
    private fun tailPlayer(): ExoPlayer = tail ?: ExoPlayer.Builder(context, LevellingRenderersFactory(context, tailLevelling))
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
    }
}
