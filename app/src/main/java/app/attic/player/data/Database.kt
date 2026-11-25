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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    /** A [LyricsSource] ID. Lyrics read from the song's files replace downloaded ones. */
    @ColumnInfo(defaultValue = "lrclib") val source: String = LyricsSource.Lrclib.id,
)

/**
 * When a song's files were last read for lyrics, so unchanged files aren't read again. Times are
 * MediaStore's DATE_MODIFIED in seconds; a null [sidecarModifiedSec] means there was no `.lrc` file.
 */
@Entity(tableName = "local_lyrics_checks")
data class LocalLyricsCheckEntity(
    @PrimaryKey val trackKey: String,
    val audioModifiedSec: Long,
    val sidecarModifiedSec: Long?,
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

/**
 * A song's five strongest Discogs styles from the analysis model, as [app.attic.player.analysis.StyleCodec]
 * stores them. An empty [top] records a song whose file couldn't be read again, so it isn't retried.
 */
@Entity(tableName = "song_styles")
class SongStylesEntity(
    @PrimaryKey val trackKey: String,
    val modelVersion: Int,
    val top: ByteArray,
)

/**
 * A song's loudness, measured from the decoded audio during analysis. A null [integratedLufs] records a
 * file that couldn't be decoded or never rose above silence; it plays at unity gain and isn't measured
 * again until [version] changes. [samplePeak] is 1.0 at full scale.
 */
@Entity(tableName = "song_loudness")
data class SongLoudnessEntity(
    @PrimaryKey val trackKey: String,
    val version: Int,
    val integratedLufs: Double?,
    val samplePeak: Double?,
    val measuredAt: Long,
)

/** A language from your own correction or an imported file. Everything else is worked out on load. */
@Entity(tableName = "song_language")
data class SongLanguageEntity(
    @PrimaryKey val trackKey: String,
    val language: String,
    val source: String,
)

/**
 * Where a song really starts and ends, its level and beat near both ends, its key and how busy it is:
 * what a smart transition and radio's ordering read ([app.attic.player.analysis.SongTransitions]). A null
 * [decodedMs] records a file that couldn't be measured; it isn't tried again until [version] changes.
 * Times are milliseconds from the start of the decoded audio. [key] is 0–11 major, 12–23 minor.
 *
 * Carried verbatim from `feature/smart-transitions`, which owns version 6, so the equaliser's version 7
 * builds on the same schema the owner's phone already has. When the two branches meet, theirs wins.
 */
@Entity(tableName = "song_transitions")
class SongTransitionsEntity(
    @PrimaryKey val trackKey: String,
    val version: Int,
    val decodedMs: Long?,
    val audioStartMs: Long?,
    val audioEndMs: Long?,
    val outroStartMs: Long?,
    val introBpm: Double?,
    val introBeatMs: Long?,
    val introBeatConfidence: Float?,
    val outroBpm: Double?,
    val outroBeatMs: Long?,
    val outroBeatConfidence: Float?,
    val energy: Float?,
    val key: Int?,
    val keyConfidence: Float?,
    val introEnergy: ByteArray?,
    val outroEnergy: ByteArray?,
    val measuredAt: Long,
)

/**
 * An equaliser curve saved for one song or one album. [scope] is [EQ_SCOPE_SONG], with a song's
 * [Track.key] as [key], or [EQ_SCOPE_ALBUM], with an album's [Track.albumKey]. [gains] is the curve as
 * [app.attic.player.playback.eq.EqCurve.encode] writes it, one value in dB per band.
 */
@Entity(tableName = "eq_settings", primaryKeys = ["scope", "key"])
data class EqSettingEntity(
    val scope: String,
    val key: String,
    val gains: String,
    val savedAt: Long,
)

const val EQ_SCOPE_SONG = "song"
const val EQ_SCOPE_ALBUM = "album"

/** A curve saved under a name of your own, offered beside the built-in presets. */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gains: String,
    val createdAt: Long,
)

class EmbeddingRow(val trackKey: String, val embedding: ByteArray)

class StylesRow(val trackKey: String, val top: ByteArray)

class LyricsTextRow(val trackKey: String, val plain: String?, val synced: String?, val instrumental: Boolean)

class OutcomeCount(val trackKey: String, val outcome: String, val total: Int)

class CrossCount(val outcome: String, val total: Int)

class PlayCount(val trackKey: String, val playCount: Int)

class EventStart(val trackKey: String, val startedAt: Long)

class RecentListen(val trackKey: String, val startedAt: Long, val outcome: String)

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

    @Query("SELECT * FROM lyrics WHERE trackKey = :key")
    suspend fun lyricsNow(key: String): LyricsEntity?

    @Query("SELECT * FROM lyrics")
    suspend fun allLyrics(): List<LyricsEntity>

    @Query("DELETE FROM lyrics WHERE trackKey = :key")
    suspend fun deleteLyrics(key: String)

    @Query("SELECT * FROM local_lyrics_checks")
    suspend fun localLyricsChecks(): List<LocalLyricsCheckEntity>

    @Query("SELECT * FROM local_lyrics_checks WHERE trackKey = :key")
    suspend fun localLyricsCheck(key: String): LocalLyricsCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalLyricsCheck(entity: LocalLyricsCheckEntity)


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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStyles(entity: SongStylesEntity)

    /**
     * What one decode gave, together, so a song is never left with its analysis and not its styles. A
     * null part is left as it was (see [app.attic.player.analysis.saveAnalysis]).
     */
    @Transaction
    suspend fun saveSongAnalysis(analysis: SongAnalysisEntity?, styles: SongStylesEntity?, loudness: SongLoudnessEntity?) {
        if (analysis != null) saveAnalysis(analysis)
        if (styles != null) saveStyles(styles)
        if (loudness != null) saveLoudness(loudness)
    }

    @Query("SELECT trackKey, top FROM song_styles WHERE modelVersion = :version")
    fun styles(version: Int): Flow<List<StylesRow>>

    /**
     * Songs analysis has finished with: read with their styles, or found unreadable. A song analysed
     * before styles were stored is not finished until its styles are.
     */
    @Query(
        "SELECT a.trackKey FROM song_analysis a LEFT JOIN song_styles s ON s.trackKey = a.trackKey AND s.modelVersion = :version " +
            "WHERE a.modelVersion = :version AND (a.embedding IS NULL OR s.trackKey IS NOT NULL)",
    )
    fun finishedKeys(version: Int): Flow<List<String>>

    @Query(
        "SELECT a.trackKey FROM song_analysis a LEFT JOIN song_styles s ON s.trackKey = a.trackKey AND s.modelVersion = :version " +
            "WHERE a.modelVersion = :version AND (a.embedding IS NULL OR s.trackKey IS NOT NULL)",
    )
    suspend fun finishedKeysNow(version: Int): List<String>

    @Query("SELECT chroma FROM song_analysis WHERE trackKey = :key")
    suspend fun chroma(key: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLoudness(entity: SongLoudnessEntity)

    @Query("SELECT trackKey FROM song_loudness WHERE version = :version")
    suspend fun measuredKeysNow(version: Int): List<String>

    @Query("SELECT trackKey FROM song_loudness WHERE version = :version")
    fun measuredKeys(version: Int): Flow<List<String>>

    /**
     * Songs analysis has nothing left to do for: finished as [finishedKeys] says, and with their loudness
     * saved. This is the count Settings shows as analysed.
     */
    @Query(
        "SELECT a.trackKey FROM song_analysis a " +
            "LEFT JOIN song_styles s ON s.trackKey = a.trackKey AND s.modelVersion = :modelVersion " +
            "JOIN song_loudness l ON l.trackKey = a.trackKey AND l.version = :loudnessVersion " +
            "WHERE a.modelVersion = :modelVersion AND (a.embedding IS NULL OR s.trackKey IS NOT NULL)",
    )
    fun completeKeys(modelVersion: Int, loudnessVersion: Int): Flow<List<String>>

    @Query("SELECT * FROM song_loudness WHERE version = :version AND integratedLufs IS NOT NULL")
    fun loudness(version: Int): Flow<List<SongLoudnessEntity>>

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

    /** Listens from [since] on, for Home's "On repeat". Uses the startedAt index. */
    @Query("SELECT trackKey, startedAt, outcome FROM listening_events WHERE startedAt >= :since AND listenedMs > 0")
    suspend fun listensSince(since: Long): List<RecentListen>

    @Query("SELECT * FROM eq_settings")
    fun eqSettings(): Flow<List<EqSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqSetting(entity: EqSettingEntity)

    @Query("DELETE FROM eq_settings WHERE scope = :scope AND `key` = :key")
    suspend fun deleteEqSetting(scope: String, key: String)

    @Query("SELECT * FROM eq_presets ORDER BY createdAt, id")
    fun eqPresets(): Flow<List<EqPresetEntity>>

    @Insert
    suspend fun insertEqPreset(entity: EqPresetEntity): Long

    @Query("UPDATE eq_presets SET name = :name WHERE id = :id")
    suspend fun renameEqPreset(id: Long, name: String)

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun deleteEqPreset(id: Long)

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

    @Query("SELECT * FROM eq_settings ORDER BY scope, `key`")
    suspend fun allEqSettings(): List<EqSettingEntity>

    @Query("SELECT * FROM eq_presets ORDER BY createdAt, id")
    suspend fun allEqPresets(): List<EqPresetEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEqSettingsIfAbsent(entities: List<EqSettingEntity>)

    @Insert
    suspend fun insertEqPresets(entities: List<EqPresetEntity>)

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

    /** True when anything has been saved: likes, playlists, plays, a queue, lyrics, analysis, languages or equaliser settings. */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM liked) OR EXISTS(SELECT 1 FROM playlists) OR EXISTS(SELECT 1 FROM history) " +
            "OR EXISTS(SELECT 1 FROM saved_queue) OR EXISTS(SELECT 1 FROM lyrics) OR EXISTS(SELECT 1 FROM listening_events) " +
            "OR EXISTS(SELECT 1 FROM song_analysis) OR EXISTS(SELECT 1 FROM song_language) " +
            "OR EXISTS(SELECT 1 FROM eq_settings) OR EXISTS(SELECT 1 FROM eq_presets)",
    )
    suspend fun hasUserData(): Boolean
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
        LocalLyricsCheckEntity::class,
        SongStylesEntity::class,
        SongLoudnessEntity::class,
        SongTransitionsEntity::class,
        EqSettingEntity::class,
        EqPresetEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AtticDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao

    companion object {
        fun create(context: Context): AtticDatabase =
            Room.databaseBuilder(context, AtticDatabase::class.java, "attic.db")
                .addMigrations(
                    DatabaseMigrations.FROM_3_TO_4,
                    DatabaseMigrations.FROM_4_TO_5,
                    DatabaseMigrations.FROM_5_TO_6,
                    DatabaseMigrations.FROM_6_TO_7,
                )
                .build()
    }
}

/**
 * Hand-written migrations. Their SQL is kept apart from the [Migration] so the JVM test
 * (DatabaseMigrationTest) can run it on plain SQLite against the exported schemas in `app/schemas`.
 */
object DatabaseMigrations {
    /** Adds `song_styles`. Songs analysed before it get their styles from the next analysis run. */
    val SQL_3_TO_4 = listOf(
        "CREATE TABLE IF NOT EXISTS `song_styles` (`trackKey` TEXT NOT NULL, `modelVersion` INTEGER NOT NULL, " +
            "`top` BLOB NOT NULL, PRIMARY KEY(`trackKey`))",
    )

    /** Adds `song_loudness`. Nothing existing changes, so every saved row carries over. */
    val SQL_4_TO_5 = listOf(
        "CREATE TABLE IF NOT EXISTS `song_loudness` (`trackKey` TEXT NOT NULL, `version` INTEGER NOT NULL, " +
            "`integratedLufs` REAL, `samplePeak` REAL, `measuredAt` INTEGER NOT NULL, PRIMARY KEY(`trackKey`))",
    )

    /**
     * Adds `song_transitions` for smart transitions and radio's ordering. Nothing existing changes. Songs
     * analysed before it get their row from a decode on the next charging run.
     *
     * Version 6 belongs to `feature/smart-transitions`; this is its migration, word for word, so a phone
     * on that branch's version 6 and one coming from 5 end up with the same database here.
     */
    val SQL_5_TO_6 = listOf(
        "CREATE TABLE IF NOT EXISTS `song_transitions` (`trackKey` TEXT NOT NULL, `version` INTEGER NOT NULL, " +
            "`decodedMs` INTEGER, `audioStartMs` INTEGER, `audioEndMs` INTEGER, `outroStartMs` INTEGER, " +
            "`introBpm` REAL, `introBeatMs` INTEGER, `introBeatConfidence` REAL, " +
            "`outroBpm` REAL, `outroBeatMs` INTEGER, `outroBeatConfidence` REAL, " +
            "`energy` REAL, `key` INTEGER, `keyConfidence` REAL, `introEnergy` BLOB, `outroEnergy` BLOB, " +
            "`measuredAt` INTEGER NOT NULL, PRIMARY KEY(`trackKey`))",
    )

    /** Adds `eq_settings` and `eq_presets` for the equaliser. Nothing existing changes. */
    val SQL_6_TO_7 = listOf(
        "CREATE TABLE IF NOT EXISTS `eq_settings` (`scope` TEXT NOT NULL, `key` TEXT NOT NULL, `gains` TEXT NOT NULL, " +
            "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`scope`, `key`))",
        "CREATE TABLE IF NOT EXISTS `eq_presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
            "`gains` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
    )

    val FROM_3_TO_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) = SQL_3_TO_4.forEach(db::execSQL)
    }

    val FROM_4_TO_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) = SQL_4_TO_5.forEach(db::execSQL)
    }

    val FROM_5_TO_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) = SQL_5_TO_6.forEach(db::execSQL)
    }

    val FROM_6_TO_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) = SQL_6_TO_7.forEach(db::execSQL)
    }
}
