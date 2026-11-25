package app.attic.player.analysis

import app.attic.player.data.MusicDao
import app.attic.player.data.SongAnalysisEntity
import app.attic.player.data.SongLoudnessEntity
import app.attic.player.data.SongStylesEntity
import app.attic.player.data.SongTransitionsEntity

/** Raise this if the loudness measure changes, so every song is measured again. */
const val LOUDNESS_VERSION = 1

/** Raise this if [TransitionMeter] or [KeyEstimate] changes, so every song is measured again. */
const val TRANSITIONS_VERSION = 1

/** What a song still needs from the analysis worker. */
enum class AnalysisNeed {
    /** A decode and a model run: the sound, styles, loudness and transitions all come from that one decode. */
    Full,

    /** Only the loudness or the transitions are missing: a decode with no model run, a second or two. */
    Measure,
}

/** Which songs the analysis worker still has to visit, and in what order. Kept apart so a JVM test can check it. */
object AnalysisPlan {
    /**
     * Null when nothing is left. [finished] is the model's finished songs ([MusicDao.finishedKeysNow]:
     * read with styles, or unreadable); [measured] the songs whose loudness is saved; [transitioned] the
     * songs whose transitions are saved.
     */
    fun need(key: String, finished: Set<String>, measured: Set<String>, transitioned: Set<String>): AnalysisNeed? = when {
        key !in finished -> AnalysisNeed.Full
        key !in measured || key !in transitioned -> AnalysisNeed.Measure
        else -> null
    }

    /**
     * Every key still to do, with what it needs, in this order: songs missing their loudness (it changes
     * how they sound), songs radio can't use yet ([analyzed] doesn't hold them), songs missing only their
     * transitions (a quick decode each), then songs analysed before styles or embedding lengths were
     * stored. Within each, songs you played most recently ([recent], newest at 0) come first. A song
     * needing the model gets everything else from the same decode, never a second one.
     */
    fun todo(
        keys: List<String>,
        finished: Set<String>,
        analyzed: Set<String>,
        measured: Set<String>,
        transitioned: Set<String>,
        recent: Map<String, Int>,
    ): List<Pair<String, AnalysisNeed>> =
        keys.mapNotNull { key -> need(key, finished, measured, transitioned)?.let { key to it } }
            .sortedWith(
                compareBy(
                    { (key, need) ->
                        when {
                            need == AnalysisNeed.Measure && key !in measured -> 0
                            need == AnalysisNeed.Full && key !in analyzed -> 1
                            need == AnalysisNeed.Measure -> 2
                            else -> 3
                        }
                    },
                    { (key, _) -> recent[key] ?: Int.MAX_VALUE },
                ),
            )
}

/**
 * Saves what one decode learned: the sound for radio, the styles for genres, the loudness for volume
 * levelling and the transition points, in one transaction. A song that already had an embedding
 * ([hadEmbedding]: analysed before styles or the embedding's length were stored) keeps it if its file
 * can't be read now; it gets an empty styles row and an unknown (0) length instead, so it isn't tried again.
 */
suspend fun MusicDao.saveAnalysis(
    key: String,
    analysis: SongAnalyzer.Analysis,
    hadEmbedding: Boolean = false,
    now: Long = System.currentTimeMillis(),
) {
    val sound = analysis.sound
    val keepOld = sound == null && hadEmbedding
    saveSongAnalysis(
        analysis = if (keepOld) {
            null
        } else {
            SongAnalysisEntity(
                trackKey = key,
                modelVersion = SongAnalyzer.MODEL_VERSION,
                embedding = sound?.embedding?.let(SongAnalyzer::toBytes),
                chroma = sound?.chroma,
                analyzedAt = now,
                embeddingNorm = sound?.norm,
            )
        },
        styles = when {
            sound != null -> SongStylesEntity(key, SongAnalyzer.MODEL_VERSION, StyleCodec.encode(sound.styles))
            // A song that kept its styles and only lacked its length keeps them.
            keepOld && !hasStyles(key, SongAnalyzer.MODEL_VERSION) -> SongStylesEntity(key, SongAnalyzer.MODEL_VERSION, ByteArray(0))
            else -> null
        },
        loudness = loudnessEntity(key, analysis.loudness, now),
        transitions = transitionsEntity(key, analysis.transitions, KeyEstimate.of(sound?.chroma), now),
    )
    if (keepOld) markNormUnknown(key)
}

/**
 * What a decode with no model run measured, for a song analysed before Attic measured these. The key
 * comes from the chroma already stored. A null [measured] (the file couldn't be decoded) records that,
 * so it isn't tried on every run.
 */
suspend fun MusicDao.saveMeasured(key: String, measured: PcmDecoder.Measured?, now: Long = System.currentTimeMillis()) {
    saveMeasured(
        loudnessEntity(key, measured?.loudness, now),
        transitionsEntity(key, measured?.transitions, KeyEstimate.of(chroma(key)), now),
    )
}

internal fun transitionsEntity(key: String, t: SongTransitions?, songKey: KeyEstimate.Key?, now: Long) = SongTransitionsEntity(
    trackKey = key,
    version = TRANSITIONS_VERSION,
    decodedMs = t?.decodedMs,
    audioStartMs = t?.audioStartMs,
    audioEndMs = t?.audioEndMs,
    outroStartMs = t?.outroStartMs,
    introBpm = t?.intro?.bpm,
    introBeatMs = t?.intro?.beatMs,
    introBeatConfidence = t?.intro?.confidence,
    outroBpm = t?.outro?.bpm,
    outroBeatMs = t?.outro?.beatMs,
    outroBeatConfidence = t?.outro?.confidence,
    energy = t?.energy,
    key = if (t == null) null else songKey?.key,
    keyConfidence = if (t == null) null else songKey?.confidence,
    introEnergy = t?.introEnergy,
    outroEnergy = t?.outroEnergy,
    measuredAt = now,
)

private fun loudnessEntity(key: String, loudness: SongLoudness?, now: Long) = SongLoudnessEntity(
    trackKey = key,
    version = LOUDNESS_VERSION,
    integratedLufs = loudness?.integratedLufs,
    samplePeak = loudness?.samplePeak,
    measuredAt = now,
)
