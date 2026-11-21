package app.attic.player.wrapped

import app.attic.player.AtticApp
import app.attic.player.data.LanguageLabels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Reads the listening log and the library into a [Recap] for one period. */
class WrappedRepository(private val app: AtticApp) {
    fun listenCount(): Flow<Int> = app.database.dao().listenCount()

    suspend fun recap(period: RecapPeriod, now: Long = System.currentTimeMillis()): Recap = withContext(Dispatchers.Default) {
        val dao = app.database.dao()
        val library = app.library.await()
        val zone = ZoneId.systemDefault()
        val listens = dao.listens()
            .filter { it.trackKey in library.trackByKey }
            .map { Listen(it.trackKey, it.startedAt, it.listenedMs, it.outcome, it.contextName) }
        val stored = dao.storedLanguages().associate { it.trackKey to it.language }
        val lyrics = dao.lyricsTexts().associateBy { it.trackKey }
        val songs = listens.map { it.trackKey }.distinct().mapNotNull { key ->
            val track = library.trackByKey[key] ?: return@mapNotNull null
            val text = lyrics[key]
            key to SongInfo(
                artistKeys = track.artistKeys.filter { it in library.artistByKey },
                language = LanguageLabels.resolve(track.folder, track.album, stored[key], text?.plain ?: text?.synced, text?.instrumental == true),
            )
        }.toMap()
        RecapBuilder.build(listens, period.range(now, zone), songs, zone)
    }
}
