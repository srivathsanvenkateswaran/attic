package app.attic.player.data.backup

import app.attic.player.data.HistoryEntity
import app.attic.player.data.LikedEntity
import app.attic.player.data.ListeningEventEntity
import app.attic.player.data.LyricsEntity
import app.attic.player.data.PlaylistEntity
import app.attic.player.data.SavedQueueEntity
import app.attic.player.data.SongLanguageEntity
import app.attic.player.radio.LanguageImport

/** What is on this phone already, as far as a restore needs to know. */
class ExistingData(
    val likedKeys: Set<String> = emptySet(),
    val playlists: List<ExistingPlaylist> = emptyList(),
    val history: Map<String, HistoryEntity> = emptyMap(),
    /** Each listen as [listenId]. */
    val listenIds: Set<String> = emptySet(),
    /** Songs with lyrics found. A checked miss doesn't count, so restored lyrics replace it. */
    val lyricsFoundKeys: Set<String> = emptySet(),
    /** Song key to the language's source, "manual" or "imported". */
    val languageSources: Map<String, String> = emptyMap(),
    val hasSavedQueue: Boolean = false,
)

class ExistingPlaylist(val id: Long, val name: String, val keys: List<String>)

/** A playlist the restore creates, with its songs in order. */
class NewPlaylist(val entity: PlaylistEntity, val keys: List<String>)

/** What a restore added, for the message afterwards. */
data class RestoreReport(
    val likesAdded: Int,
    val playlistsAdded: Int,
    /** Playlists that already existed here by the same name and gained songs. */
    val playlistsMerged: Int,
    val listensAdded: Int,
    val lyricsAdded: Int,
    /** Songs the backup mentions that are in this phone's library, by path or by file name. */
    val songsMatched: Int,
    /** Songs the backup mentions that aren't here. Their rows are kept and come back with the files. */
    val songsMissing: Int,
)

/** Every row a restore writes. Nothing here deletes. */
class RestorePlan(
    val likes: List<LikedEntity>,
    val newPlaylists: List<NewPlaylist>,
    /** Existing playlist ID to the songs appended to its end. */
    val playlistAdditions: Map<Long, List<String>>,
    /** Inserted or replaced whole: a merged row carries the larger of each value. */
    val history: List<HistoryEntity>,
    val listens: List<ListeningEventEntity>,
    val lyrics: List<LyricsEntity>,
    val languages: List<SongLanguageEntity>,
    val queue: SavedQueueEntity?,
    val report: RestoreReport,
)

/**
 * Merges a backup into what is here without removing anything. Duplicates are recognised by key, so
 * restoring the same file twice adds nothing the second time:
 * - a favourite by song
 * - a playlist by name, ignoring case and surrounding spaces; the backup's songs it lacks go on its end
 * - a listen by song and start time
 * - a play-count row by song, keeping the later "last played" and the larger count
 * - lyrics by song, filling only songs that have none
 * - a language by song, where a manual choice beats an imported one and otherwise this phone's stays
 * - the saved queue only when this phone has none
 */
object RestorePlanner {
    fun plan(backup: Backup, existing: ExistingData, libraryKeys: Set<String>): RestorePlan {
        val songs = backup.songs()
        val keys = SongMatcher.match(songs, libraryKeys)
        fun key(song: String) = keys[song] ?: song

        val likes = LinkedHashMap<String, LikedEntity>()
        for (like in backup.likes) {
            val k = key(like.song)
            if (k in existing.likedKeys) continue
            val earlier = likes[k]
            if (earlier == null || like.likedAt < earlier.likedAt) likes[k] = LikedEntity(k, like.likedAt)
        }

        // Playlists: merge by name into one here, or into one this restore already created.
        val byName = HashMap<String, Target>()
        existing.playlists.forEach { byName.putIfAbsent(normalise(it.name), Target.Existing(it.id, it.keys.toMutableSet())) }
        val created = ArrayList<Target.Created>()
        val additions = LinkedHashMap<Long, MutableList<String>>()
        for (playlist in backup.playlists) {
            val name = playlist.name.trim()
            if (name.isEmpty()) continue
            val mapped = playlist.songs.map(::key)
            when (val target = byName[normalise(name)]) {
                null -> {
                    val fresh = Target.Created(PlaylistEntity(name = name, createdAt = playlist.createdAt), LinkedHashSet(mapped))
                    created += fresh
                    byName[normalise(name)] = fresh
                }
                is Target.Created -> target.keys.addAll(mapped)
                is Target.Existing -> mapped.forEach { k ->
                    if (target.keys.add(k)) additions.getOrPut(target.id) { ArrayList() }.add(k)
                }
            }
        }

        val history = LinkedHashMap<String, HistoryEntity>()
        for (play in backup.plays) {
            val k = key(play.song)
            val base = history[k] ?: existing.history[k]
            val merged = if (base == null) {
                HistoryEntity(k, play.lastPlayedAt, play.playCount)
            } else {
                HistoryEntity(k, maxOf(base.lastPlayedAt, play.lastPlayedAt), maxOf(base.playCount, play.playCount))
            }
            if (merged != existing.history[k]) history[k] = merged
        }

        val seenListens = HashSet(existing.listenIds)
        val listens = backup.listens.mapNotNull { listen ->
            val k = key(listen.song)
            if (!seenListens.add(listenId(k, listen.startedAt))) return@mapNotNull null
            ListeningEventEntity(
                trackKey = k,
                startedAt = listen.startedAt,
                listenedMs = listen.listenedMs,
                durationMs = listen.durationMs,
                outcome = listen.outcome,
                contextName = listen.context,
                radioCross = listen.radioCross,
            )
        }

        val lyrics = LinkedHashMap<String, LyricsEntity>()
        for (entry in backup.lyrics) {
            val k = key(entry.song)
            if (k in existing.lyricsFoundKeys || k in lyrics) continue
            if (entry.synced == null && entry.plain == null && !entry.instrumental) continue
            lyrics[k] = LyricsEntity(k, entry.synced, entry.plain, entry.instrumental, found = true, fetchedAt = entry.fetchedAt)
        }

        val languages = LinkedHashMap<String, SongLanguageEntity>()
        for (entry in backup.languages) {
            val k = key(entry.song)
            val here = existing.languageSources[k]
            val wins = here == null || (entry.source == LanguageImport.SOURCE_MANUAL && here != LanguageImport.SOURCE_MANUAL)
            if (wins && k !in languages) languages[k] = SongLanguageEntity(k, entry.language, entry.source)
        }

        val queue = backup.queue?.takeIf { !existing.hasSavedQueue && it.songs.isNotEmpty() }?.let { q ->
            SavedQueueEntity(
                queueKeys = q.songs.joinToString("\n", transform = ::key),
                index = q.index,
                positionMs = q.positionMs,
                repeatMode = q.repeatMode,
                shuffle = q.shuffle,
                baseKeys = q.baseOrder?.joinToString("\n", transform = ::key),
                contextName = q.context,
                radioSeed = q.radioSeed?.let(::key),
            )
        }

        val matched = songs.count { keys[it] != null }
        return RestorePlan(
            likes = likes.values.toList(),
            newPlaylists = created.map { NewPlaylist(it.entity, it.keys.toList()) },
            playlistAdditions = additions,
            history = history.values.toList(),
            listens = listens,
            lyrics = lyrics.values.toList(),
            languages = languages.values.toList(),
            queue = queue,
            report = RestoreReport(
                likesAdded = likes.size,
                playlistsAdded = created.size,
                playlistsMerged = additions.size,
                listensAdded = listens.size,
                lyricsAdded = lyrics.size,
                songsMatched = matched,
                songsMissing = songs.size - matched,
            ),
        )
    }

    /** How a listen is told apart from another: the same song started at the same millisecond is the same listen. */
    fun listenId(key: String, startedAt: Long): String = "$startedAt\u0000$key"

    private fun normalise(name: String) = name.trim().lowercase()

    private sealed interface Target {
        class Existing(val id: Long, val keys: MutableSet<String>) : Target
        class Created(val entity: PlaylistEntity, val keys: LinkedHashSet<String>) : Target
    }
}

/**
 * Finds each backed-up song in this phone's library. A key is the file's path from the top of its
 * volume, so a song copied to the same folder matches exactly. When the folders differ (the music
 * sits in `Music/` here and sat in `Music/Offline/` there), a song still matches by file name, but
 * only when exactly one file in the library has that name and exactly one song in the backup does,
 * so two different "01 Intro.mp3" are never confused.
 */
object SongMatcher {
    /** Backup key to library key, for the songs that are here. */
    fun match(songs: Set<String>, libraryKeys: Set<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val libraryByName = libraryKeys.groupBy(::fileName)
        val backupNameCounts = songs.groupingBy(::fileName).eachCount()
        for (song in songs) {
            if (song in libraryKeys) {
                result[song] = song
                continue
            }
            val name = fileName(song)
            val candidates = libraryByName[name] ?: continue
            if (candidates.size == 1 && backupNameCounts[name] == 1) result[song] = candidates.single()
        }
        return result
    }

    private fun fileName(key: String) = key.substringAfterLast('/')
}
