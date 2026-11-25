package app.attic.player.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/**
 * Applies each song's levelling gain to the decoded audio, on ExoPlayer's playback thread.
 *
 * Since Media3 1.11 the sink flushes its processors at every new playlist item with the item's period
 * in [AudioProcessor.StreamMetadata], after the previous item's audio has drained (androidx/media#418).
 * So the gain changes on the first sample of the new song, gapless transitions included, and a seek
 * looks the gain up again. The tags arrive a step earlier, through [LevellingAudioSink], keyed by the
 * same period.
 *
 * Cost: one multiply and a clamp per sample, on buffers the sink was copying anyway; well under 1% of
 * one core at 48 kHz stereo. At unity it copies the buffer straight through.
 */
@OptIn(UnstableApi::class)
class LevellingAudioProcessor(private val source: GainSource) : BaseAudioProcessor() {
    /** The gain for the item a period belongs to, or null for unity. Called on the playback thread. */
    fun interface GainSource {
        fun gainDb(timeline: Timeline, periodUid: Any?, tags: TagGain?): Float?
    }

    /** Whether "Volume levelling" is on. Read for every buffer, so switching it ramps in at once. */
    @Volatile
    var enabled: Boolean = true

    /**
     * A gain to use instead of looking one up. The crossfade's tail player plays the end of a song the
     * main player was already levelling, so it takes that gain rather than working it out alone.
     */
    @Volatile
    var fixedGainDb: Float? = null

    /** The gain the item now passing through asked for, before [enabled] is applied. */
    @Volatile
    var streamGainDb: Float = 0f
        private set

    private val tagsByPeriod = object : LinkedHashMap<Any, TagGain?>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, TagGain?>?) = size > REMEMBERED_PERIODS
    }

    private var applied = 1f

    /** The sink is being configured for the period [periodUid], whose format carries [metadata]. */
    fun noteStream(periodUid: Any?, metadata: Metadata?) {
        if (periodUid == null) return
        synchronized(tagsByPeriod) { tagsByPeriod[periodUid] = ReplayGainTags.fromMetadata(metadata) }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            // Anything else passes through untouched: the sink converts to 16-bit before this runs unless float output is on.
            else -> AudioFormat.NOT_SET
        }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val tags = streamMetadata.periodUid?.let { synchronized(tagsByPeriod) { tagsByPeriod[it] } }
        streamGainDb = fixedGainDb ?: runCatching { source.gainDb(streamMetadata.timeline, streamMetadata.periodUid, tags) }.getOrNull() ?: 0f
        // A new song starts at its own level straight away; only a change mid-song is ramped.
        applied = target()
    }

    override fun onReset() {
        applied = 1f
        streamGainDb = 0f
        synchronized(tagsByPeriod) { tagsByPeriod.clear() }
    }

    private fun target(): Float = if (enabled || fixedGainDb != null) Levelling.linear(streamGainDb) else 1f

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val output = replaceOutputBuffer(size)
        val target = target()
        if (applied == 1f && target == 1f) {
            output.put(inputBuffer)
            output.flip()
            return
        }
        val channels = inputAudioFormat.channelCount
        val frames = size / inputAudioFormat.bytesPerFrame
        val step = (target - applied) / RAMP_FRAMES
        var gain = applied
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> for (frame in 0 until frames) {
                if (gain != target) gain = approach(gain, target, step)
                for (c in 0 until channels) {
                    val v = (inputBuffer.getShort() * gain).toInt()
                    output.putShort(v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                }
            }
            else -> for (frame in 0 until frames) {
                if (gain != target) gain = approach(gain, target, step)
                for (c in 0 until channels) output.putFloat((inputBuffer.getFloat() * gain).coerceIn(-1f, 1f))
            }
        }
        applied = gain
        output.flip()
    }

    private fun approach(gain: Float, target: Float, step: Float): Float {
        val next = gain + step
        return if ((step > 0 && next >= target) || (step < 0 && next <= target)) target else next
    }

    private companion object {
        /** About 20 ms at 48 kHz: long enough not to click, short enough to sound immediate. */
        const val RAMP_FRAMES = 1024f
        const val REMEMBERED_PERIODS = 8
    }
}

/** Hands each new item's tags to the levelling processor as the sink is configured for it. */
@OptIn(UnstableApi::class)
class LevellingAudioSink(sink: AudioSink, private val levelling: LevellingAudioProcessor) : ForwardingAudioSink(sink) {
    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        levelling.noteStream(audioSinkConfig.mediaPeriodId?.periodUid, audioSinkConfig.format.metadata)
        super.configure(audioSinkConfig)
    }
}

/**
 * ExoPlayer's renderers with [levelling] and then [equaliser] at the end of the audio chain. Levelling
 * goes first so the equaliser's limiter is the last thing to touch the samples: a song levelled up
 * and then boosted by the equaliser is still kept from clipping.
 */
@OptIn(UnstableApi::class)
class LevellingRenderersFactory(
    context: Context,
    private val levelling: LevellingAudioProcessor,
    private val equaliser: AudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean): AudioSink =
        LevellingAudioSink(
            DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                .setAudioProcessors(arrayOf(levelling, equaliser))
                .build(),
            levelling,
        )
}
