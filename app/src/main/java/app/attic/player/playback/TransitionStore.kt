package app.attic.player.playback

import app.attic.player.analysis.SongTransitions
import app.attic.player.analysis.TRANSITIONS_VERSION
import app.attic.player.data.MusicDao
import app.attic.player.data.SongTransitionsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Every song's transition points, kept as one immutable map the playback thread reads without waiting,
 * as [LevelStore] does for loudness. It is rebuilt off the main thread when analysis saves a song, and
 * [changed] runs (on [scope]'s thread) after each rebuild, so a song measured while it plays gets a smart
 * plan for its ending. About 200 bytes a song.
 */
class TransitionStore(dao: MusicDao, scope: CoroutineScope, private val changed: () -> Unit) {
    @Volatile
    private var songs: Map<String, SongTransitions> = emptyMap()

    init {
        scope.launch {
            dao.transitions(TRANSITIONS_VERSION)
                .map { rows -> rows.mapNotNull { row -> row.toTransitions()?.let { row.trackKey to it } }.toMap() }
                .flowOn(Dispatchers.Default)
                .collect {
                    songs = it
                    changed()
                }
        }
    }

    operator fun get(key: String): SongTransitions? = songs[key]

    companion object {
        /** Null for a row that records a file that couldn't be measured. */
        fun SongTransitionsEntity.toTransitions(): SongTransitions? {
            val decoded = decodedMs ?: return null
            return SongTransitions(
                decodedMs = decoded,
                audioStartMs = audioStartMs ?: return null,
                audioEndMs = audioEndMs ?: return null,
                outroStartMs = outroStartMs ?: return null,
                intro = beat(introBpm, introBeatMs, introBeatConfidence),
                outro = beat(outroBpm, outroBeatMs, outroBeatConfidence),
                energy = energy ?: 0f,
                introEnergy = introEnergy ?: return null,
                outroEnergy = outroEnergy ?: return null,
            )
        }

        private fun beat(bpm: Double?, at: Long?, confidence: Float?): SongTransitions.Beat? =
            if (bpm == null || at == null || confidence == null) null else SongTransitions.Beat(bpm, at, confidence)
    }
}
