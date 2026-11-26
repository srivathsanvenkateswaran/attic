import Foundation
import SwiftData

/// The first stored schema. Every row that holds user data is keyed by song key, never by a system
/// ID, so likes, plays, playlists and lyrics survive a rescan.
enum AtticSchemaV1: VersionedSchema {
    static let versionIdentifier = Schema.Version(1, 0, 0)

    static var models: [any PersistentModel.Type] {
        [SongRecord.self, RootRecord.self, LikeRecord.self, PlayStat.self, PlaylistRecord.self, LyricsRecord.self]
    }

    /// A song as `JSONEncoder` writes a [Track]. Stored whole because a rescan replaces the row.
    @Model
    final class SongRecord {
        @Attribute(.unique) var key: String
        var payload: Data

        init(key: String, payload: Data) {
            self.key = key
            self.payload = payload
        }
    }

    @Model
    final class RootRecord {
        @Attribute(.unique) var id: UUID
        var name: String
        var bookmark: Data
        var order: Int

        init(id: UUID, name: String, bookmark: Data, order: Int) {
            self.id = id
            self.name = name
            self.bookmark = bookmark
            self.order = order
        }
    }

    @Model
    final class LikeRecord {
        @Attribute(.unique) var key: String
        var likedAt: Date

        init(key: String, likedAt: Date) {
            self.key = key
            self.likedAt = likedAt
        }
    }

    @Model
    final class PlayStat {
        @Attribute(.unique) var key: String
        var count: Int
        var lastPlayedAt: Date

        init(key: String, count: Int, lastPlayedAt: Date) {
            self.key = key
            self.count = count
            self.lastPlayedAt = lastPlayedAt
        }
    }

    @Model
    final class PlaylistRecord {
        @Attribute(.unique) var id: UUID
        var name: String
        var createdAt: Date
        var trackKeys: [String]

        init(id: UUID, name: String, createdAt: Date, trackKeys: [String]) {
            self.id = id
            self.name = name
            self.createdAt = createdAt
            self.trackKeys = trackKeys
        }
    }

    @Model
    final class LyricsRecord {
        @Attribute(.unique) var key: String
        var payload: Data

        init(key: String, payload: Data) {
            self.key = key
            self.payload = payload
        }
    }
}

// Short names for the store and its tests. A later schema gets its own nested classes.
typealias SongRecord = AtticSchemaV1.SongRecord
typealias RootRecord = AtticSchemaV1.RootRecord
typealias LikeRecord = AtticSchemaV1.LikeRecord
typealias PlayStat = AtticSchemaV1.PlayStat
typealias PlaylistRecord = AtticSchemaV1.PlaylistRecord
typealias LyricsRecord = AtticSchemaV1.LyricsRecord

/// The migration plan. A schema change adds `AtticSchemaV2` with its own models, appends it to
/// `schemas`, and adds a `MigrationStage` between the two versions to `stages`.
enum AtticMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] { [AtticSchemaV1.self] }
    static var stages: [MigrationStage] { [] }
}
