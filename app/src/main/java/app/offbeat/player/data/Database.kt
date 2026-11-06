package app.offbeat.player.data

import android.content.Context
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
)

@Dao
interface MusicDao {
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

    @Transaction
    suspend fun reorderPlaylist(id: Long, keys: List<String>) {
        clearPlaylist(id)
        insertPlaylistTracks(keys.mapIndexed { i, key -> PlaylistTrackEntity(id, key, i) })
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
}

@Database(
    entities = [
        LikedEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        HistoryEntity::class,
        SavedQueueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OffbeatDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        fun create(context: Context): OffbeatDatabase =
            Room.databaseBuilder(context, OffbeatDatabase::class.java, "offbeat.db").build()
    }
}
