package app.attic.player.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** All track references below use [Track.key], the file's path, so they outlive a MediaStore rebuild. */
@Entity(tableName = "liked")
data class LikedEntity(
    @PrimaryKey val trackKey: String,
    val likedAt: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackKey"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackKey: String,
    val position: Int,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val trackKey: String,
    val lastPlayedAt: Long,
    val playCount: Int,
)

/** One row: the queue as it stood, restored on the next launch. Keys are newline separated. */
@Entity(tableName = "saved_queue")
data class SavedQueueEntity(
    @PrimaryKey val id: Int = 1,
    val queueKeys: String,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean,
    val baseKeys: String?,
    val contextName: String?,
    @ColumnInfo(defaultValue = "NULL") val radioSeed: String? = null,
)

/** A row with [found] false records a checked miss, so bulk downloads don't keep asking. */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackKey: String,
    val synced: String?,
    val plain: String?,
    val instrumental: Boolean,
    val found: Boolean,
    val fetchedAt: Long,
)

/** One row per listen, kept for good: radio learns from it and it can answer "what did I play this year?". */
@Entity(tableName = "listening_events", indices = [Index("trackKey"), Index("startedAt")])
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackKey: String,
    val startedAt: Long,
    val listenedMs: Long,
    val durationMs: Long,
    val outcome: String,
    val contextName: String?,
    val radioCross: Boolean,
)

/** What analysis learned from a file. A null [embedding] records a file that couldn't be read. */
@Entity(tableName = "song_analysis")
class SongAnalysisEntity(
    @PrimaryKey val trackKey: String,
    val modelVersion: Int,
    val embedding: ByteArray?,
    val chroma: ByteArray?,
    val analyzedAt: Long,
)

/** A language from your own correction or an imported file. Everything else is worked out on load. */
@Entity(tableName = "song_language")
data class SongLanguageEntity(
    @PrimaryKey val trackKey: String,
    val language: String,
    val source: String,
)

class EmbeddingRow(val trackKey: String, val embedding: ByteArray)

class LyricsTextRow(val trackKey: String, val plain: String?, val synced: String?, val instrumental: Boolean)

class OutcomeCount(val trackKey: String, val outcome: String, val total: Int)

class CrossCount(val outcome: String, val total: Int)

class PlayCount(val trackKey: String, val playCount: Int)

class EventStart(val trackKey: String, val startedAt: Long)

class ListenRow(val trackKey: String, val startedAt: Long, val listenedMs: Long, val outcome: String, val contextName: String?)

@Dao
interface MusicDao {
    @Query("SELECT * FROM lyrics WHERE trackKey = :key")
    fun lyrics(key: String): Flow<LyricsEntity?>

    @Query("SELECT trackKey FROM lyrics WHERE found = 1")
    fun lyricsFoundKeys(): Flow<List<String>>

    @Query("SELECT trackKey FROM lyrics")
    suspend fun lyricsCheckedKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entity: LyricsEntity)


    @Query("SELECT trackKey FROM liked ORDER BY likedAt DESC")
    fun likedKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun like(entity: LikedEntity)

    @Query("DELETE FROM liked WHERE trackKey = :key")
    suspend fun unlike(key: String)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position")
    fun playlistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(entity: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackKey = :key")
    suspend fun removePlaylistTrack(id: Long, key: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun clearPlaylist(id: Long)

    @Insert
    suspend fun insertPlaylistTracks(entities: List<PlaylistTrackEntity>)

    @Query("SELECT trackKey FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    suspend fun playlistKeys(id: Long): List<String>

    /**
     * Puts [keys] in this order. The screen only shows songs that are on the phone, so songs it
     * didn't pass (a restored song whose file isn't here yet) keep their order after them.
     */
    @Transaction
    suspend fun reorderPlaylist(id: Long, keys: List<String>) {
        val order = PlaylistOrder.reordered(playlistKeys(id), keys)
        clearPlaylist(id)
        insertPlaylistTracks(order.mapIndexed { i, key -> PlaylistTrackEntity(id, key, i) })
    }

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT 200")
    fun history(): Flow<List<HistoryEntity>>

    @Query(
        "INSERT INTO history (trackKey, lastPlayedAt, playCount) VALUES (:key, :at, 1) " +
            "ON CONFLICT(trackKey) DO UPDATE SET lastPlayedAt = :at, playCount = playCount + 1",
    )
    suspend fun recordPlay(key: String, at: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(entity: SavedQueueEntity)

    @Query("SELECT * FROM saved_queue WHERE id = 1")
    suspend fun savedQueue(): SavedQueueEntity?

    @Insert
    suspend fun insertEvent(entity: ListeningEventEntity)

    @Query("SELECT trackKey, outcome, COUNT(*) AS total FROM listening_events GROUP BY trackKey, outcome")
    suspend fun outcomeCounts(): List<OutcomeCount>

    @Query("SELECT outcome, COUNT(*) AS total FROM listening_events WHERE radioCross = 1 GROUP BY outcome")
    suspend fun crossCounts(): List<CrossCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnalysis(entity: SongAnalysisEntity)

    @Query("SELECT trackKey FROM song_analysis WHERE modelVersion = :version")
    fun analyzedKeys(version: Int): Flow<List<String>>

    @Query("SELECT trackKey FROM song_analysis WHERE modelVersion = :version")
    suspend fun analyzedKeysNow(version: Int): List<String>

    @Query("SELECT trackKey, embedding FROM song_analysis WHERE modelVersion = :version AND embedding IS NOT NULL")
    suspend fun embeddings(version: Int): List<EmbeddingRow>

    @Query("SELECT chroma FROM song_analysis WHERE trackKey = :key")
    suspend fun chroma(key: String): ByteArray?

    @Query("SELECT * FROM song_language")
    suspend fun storedLanguages(): List<SongLanguageEntity>

    @Query("SELECT language FROM song_language WHERE trackKey = :key")
    fun storedLanguage(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLanguage(entity: SongLanguageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLanguages(entities: List<SongLanguageEntity>)

    @Query("SELECT trackKey, plain, synced, instrumental FROM lyrics WHERE found = 1")
    suspend fun lyricsTexts(): List<LyricsTextRow>

    @Query("SELECT trackKey, playCount FROM history")
    suspend fun playCounts(): List<PlayCount>

    @Query("SELECT trackKey FROM history ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun recentKeys(limit: Int): List<String>

    @Query("SELECT trackKey, startedAt, listenedMs, outcome, contextName FROM listening_events WHERE listenedMs > 0 ORDER BY startedAt")
    suspend fun listens(): List<ListenRow>

    @Query("SELECT COUNT(*) FROM listening_events WHERE listenedMs > 0")
    fun listenCount(): Flow<Int>

    // Backup and restore read whole tables and write in bulk.

    @Query("SELECT * FROM liked ORDER BY likedAt")
    suspend fun allLiked(): List<LikedEntity>

    @Query("SELECT * FROM playlists ORDER BY createdAt, id")
    suspend fun allPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position")
    suspend fun allPlaylistTracks(): List<PlaylistTrackEntity>

    @Query("SELECT * FROM history")
    suspend fun allHistory(): List<HistoryEntity>

    @Query("SELECT * FROM listening_events ORDER BY startedAt, id")
    suspend fun allEvents(): List<ListeningEventEntity>

    @Query("SELECT trackKey, startedAt FROM listening_events")
    suspend fun eventStarts(): List<EventStart>

    @Query("SELECT * FROM lyrics WHERE found = 1")
    suspend fun allFoundLyrics(): List<LyricsEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLiked(entities: List<LikedEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTracksIfAbsent(entities: List<PlaylistTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(entities: List<HistoryEntity>)

    @Insert
    suspend fun insertEvents(entities: List<ListeningEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entities: List<LyricsEntity>)
}

/** Keeps songs a reorder didn't mention, in their old order, after the ones it did. */
object PlaylistOrder {
    fun reordered(current: List<String>, shown: List<String>): List<String> {
        val placed = shown.distinct()
        val set = placed.toSet()
        return placed + current.filter { it !in set }
    }
}

@Database(
    entities = [
        LikedEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        HistoryEntity::class,
        SavedQueueEntity::class,
        LyricsEntity::class,
        ListeningEventEntity::class,
        SongAnalysisEntity::class,
        SongLanguageEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AtticDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        fun create(context: Context): AtticDatabase =
            Room.databaseBuilder(context, AtticDatabase::class.java, "attic.db").build()
    }
}
