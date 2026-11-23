package app.attic.player.playback

import androidx.annotation.OptIn
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import app.attic.player.analysis.LOUDNESS_VERSION
import app.attic.player.data.Library
import app.attic.player.data.MusicDao
import app.attic.player.data.SongLoudnessEntity
import app.attic.player.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * The measured loudness of every song and album, kept as one immutable snapshot the playback thread
 * can read without waiting. It is rebuilt off the main thread when analysis saves a song or the
 * library changes.
 */
@OptIn(UnstableApi::class)
class LevelStore(dao: MusicDao, private val library: StateFlow<Library?>, scope: CoroutineScope) : LevellingAudioProcessor.GainSource {
    private class Snapshot(val songs: Map<String, MeasuredLevel>, val albums: Map<String, MeasuredLevel>)

    @Volatile
    private var snapshot = Snapshot(emptyMap(), emptyMap())

    init {
        scope.launch {
            combine(dao.loudness(LOUDNESS_VERSION), library) { rows, lib -> build(rows, lib) }
                .flowOn(Dispatchers.Default)
                .collect { snapshot = it }
        }
    }

    private fun build(rows: List<SongLoudnessEntity>, library: Library?): Snapshot {
        val songs = HashMap<String, MeasuredLevel>(rows.size)
        for (row in rows) row.integratedLufs?.let { songs[row.trackKey] = MeasuredLevel(it, row.samplePeak) }
        // An album gets one level only once every song on it is measured; until then each song keeps its own.
        val albums = HashMap<String, MeasuredLevel>()
        library?.tracks?.filter { it.albumKey != null }?.groupBy { it.albumKey!! }?.forEach { (album, tracks) ->
            val levels = tracks.map { track -> (songs[track.key] ?: return@forEach) to track.durationMs }
            Levelling.albumLevel(levels)?.let { albums[album] = it }
        }
        return Snapshot(songs, albums)
    }

    override fun gainDb(timeline: Timeline, periodUid: Any?, tags: TagGain?): Float? {
        val tracks = library.value?.trackByKey
        val window = Timeline.Window()
        val index = if (periodUid == null || timeline.isEmpty) -1 else timeline.getPeriodByUid(periodUid, Timeline.Period()).windowIndex
        fun at(i: Int): LevelTrack? {
            if (i !in 0 until timeline.windowCount) return null
            val key = timeline.getWindow(i, window).mediaItem.mediaId
            return tracks?.get(key)?.toLevelTrack() ?: LevelTrack(key, null, null)
        }
        val current = at(index) ?: LevelTrack("", null, null)
        val shot = snapshot
        return Levelling.gainDb(at(index - 1), current, at(index + 1), tags, shot.songs::get, shot.albums::get)
    }

    private fun Track.toLevelTrack() = LevelTrack(key, albumKey, trackNumber)
}
