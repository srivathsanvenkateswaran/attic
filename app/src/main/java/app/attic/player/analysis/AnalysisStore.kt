package app.attic.player.analysis

import app.attic.player.data.MusicDao
import app.attic.player.data.SongAnalysisEntity
import app.attic.player.data.SongLoudnessEntity
import app.attic.player.data.SongStylesEntity

/** Raise this if the loudness measure changes, so every song is measured again. */
const val LOUDNESS_VERSION = 1

/** What a song still needs from the analysis worker. */
enum class AnalysisNeed {
    /** A decode and a model run: the sound, styles and loudness all come from that one decode. */
    Full,

    /** Only the loudness is missing: a decode with no model run, a second or two. */
    Loudness,
}

/** Which songs the analysis worker still has to visit, and in what order. Kept apart so a JVM test can check it. */
object AnalysisPlan {
    /**
     * Null when nothing is left. [finished] is the model's finished songs ([MusicDao.finishedKeysNow]:
     * read with styles, or unreadable); [measured] the songs whose loudness is saved.
     */
    fun need(key: String, finished: Set<String>, measured: Set<String>): AnalysisNeed? = when {
        key !in finished -> AnalysisNeed.Full
        key !in measured -> AnalysisNeed.Loudness
        else -> null
    }

    /**
     * Every key still to do, with what it needs. Songs that only need their loudness go first, as they are
     * quick; then songs radio can't use yet ([analyzed] doesn't hold them); then songs analysed before
     * styles were stored. Within each, songs you played most recently ([recent], newest at 0) come first.
     * A song missing both styles and loudness gets one full decode for both, never a second for loudness.
     */
    fun todo(
        keys: List<String>,
        finished: Set<String>,
        analyzed: Set<String>,
        measured: Set<String>,
        recent: Map<String, Int>,
    ): List<Pair<String, AnalysisNeed>> =
        keys.mapNotNull { key -> need(key, finished, measured)?.let { key to it } }
            .sortedWith(
                compareBy(
                    { (key, need) ->
                        when {
                            need == AnalysisNeed.Loudness -> 0
                            key !in analyzed -> 1
                            else -> 2
                        }
                    },
                    { (key, _) -> recent[key] ?: Int.MAX_VALUE },
                ),
            )
}

/**
 * Saves what one decode learned: the sound for radio, the styles for genres and the loudness for volume
 * levelling, in one transaction. A song that already had an embedding ([hadEmbedding]: analysed before
 * styles were stored) keeps it if its file can't be read now; it gets an empty styles row instead, so
 * it isn't tried again.
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
            )
        },
        styles = when {
            sound != null -> SongStylesEntity(key, SongAnalyzer.MODEL_VERSION, StyleCodec.encode(sound.styles))
            keepOld -> SongStylesEntity(key, SongAnalyzer.MODEL_VERSION, ByteArray(0))
            else -> null
        },
        loudness = loudnessEntity(key, analysis.loudness, now),
    )
}

/** A null [loudness] records a file that couldn't be measured, so it isn't tried on every run. */
suspend fun MusicDao.saveLoudness(key: String, loudness: SongLoudness?, now: Long = System.currentTimeMillis()) {
    saveLoudness(loudnessEntity(key, loudness, now))
}

private fun loudnessEntity(key: String, loudness: SongLoudness?, now: Long) = SongLoudnessEntity(
    trackKey = key,
    version = LOUDNESS_VERSION,
    integratedLufs = loudness?.integratedLufs,
    samplePeak = loudness?.samplePeak,
    measuredAt = now,
)
