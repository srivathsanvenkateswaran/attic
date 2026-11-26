import AtticCore
import Foundation
import SwiftData

/// The SwiftData store: songs from the last scan, the picked folders, and every piece of user data,
/// all keyed by song key.
@MainActor
public final class LibraryStore: LibraryStoring {
    private let container: ModelContainer
    private let context: ModelContext
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    /// `inMemory` is for tests and previews.
    public init(inMemory: Bool = false) {
        let schema = Schema(versionedSchema: AtticSchemaV1.self)
        let configuration = ModelConfiguration("Attic", isStoredInMemoryOnly: inMemory)
        let container: ModelContainer
        if let saved = try? ModelContainer(
            for: schema, migrationPlan: AtticMigrationPlan.self, configurations: configuration
        ) {
            container = saved
        } else {
            // The app must still open when the saved store cannot be read (a corrupt file, no disk
            // space). Losing saved data beats refusing to launch.
            container = try! ModelContainer(
                for: schema, migrationPlan: AtticMigrationPlan.self,
                configurations: ModelConfiguration("Attic", isStoredInMemoryOnly: true)
            )
        }
        // The context does not keep its container alive, and a fetch on a context whose container
        // is gone traps. The store owns it for as long as the app runs.
        self.container = container
        context = container.mainContext
        // Saves happen once per write below, not after each inserted row.
        context.autosaveEnabled = false
    }

    // MARK: - Songs

    public func loadTracks() -> [Track] {
        let records = (try? context.fetch(FetchDescriptor<SongRecord>())) ?? []
        return records.compactMap { try? decoder.decode(Track.self, from: $0.payload) }
    }

    public func saveTracks(_ tracks: [Track]) {
        // One fetch for the whole library, so a 10,000 song rescan does not query per row.
        let existing = (try? context.fetch(FetchDescriptor<SongRecord>())) ?? []
        var recordsByKey = Dictionary(existing.map { ($0.key, $0) }, uniquingKeysWith: { first, _ in first })
        var seen = Set<String>()
        for track in tracks where seen.insert(track.key).inserted {
            guard let payload = try? encoder.encode(track) else { continue }
            if let record = recordsByKey.removeValue(forKey: track.key) {
                if record.payload != payload { record.payload = payload }
            } else {
                context.insert(SongRecord(key: track.key, payload: payload))
            }
        }
        for stale in recordsByKey.values { context.delete(stale) }
        try? context.save()
    }

    // MARK: - Folders

    public func loadRoots() -> [LibraryRoot] {
        let records = (try? context.fetch(FetchDescriptor<RootRecord>(sortBy: [SortDescriptor(\.order)]))) ?? []
        return records.map { LibraryRoot(id: $0.id, name: $0.name, bookmark: $0.bookmark) }
    }

    public func saveRoots(_ roots: [LibraryRoot]) {
        let existing = (try? context.fetch(FetchDescriptor<RootRecord>())) ?? []
        var recordsByID = Dictionary(existing.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        for (index, root) in roots.enumerated() {
            if let record = recordsByID.removeValue(forKey: root.id) {
                record.name = root.name
                record.bookmark = root.bookmark
                record.order = index
            } else {
                context.insert(RootRecord(id: root.id, name: root.name, bookmark: root.bookmark, order: index))
            }
        }
        for stale in recordsByID.values { context.delete(stale) }
        try? context.save()
    }

    // MARK: - Likes

    public func likedKeys() -> [String] {
        // The key breaks a tie between two likes saved in the same instant.
        let descriptor = FetchDescriptor<LikeRecord>(
            sortBy: [SortDescriptor(\.likedAt, order: .reverse), SortDescriptor(\.key, order: .reverse)]
        )
        return ((try? context.fetch(descriptor)) ?? []).map(\.key)
    }

    public func setLiked(_ liked: Bool, key: String) {
        if liked {
            if let record = try? likeRecord(key: key) {
                record.likedAt = Date()
            } else {
                context.insert(LikeRecord(key: key, likedAt: Date()))
            }
        } else if let record = try? likeRecord(key: key) {
            context.delete(record)
        }
        try? context.save()
    }

    private func likeRecord(key: String) throws -> LikeRecord? {
        var descriptor = FetchDescriptor<LikeRecord>(predicate: #Predicate { $0.key == key })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    // MARK: - Plays

    public func recordPlay(key: String, at date: Date) {
        if let stat = try? playStat(key: key) {
            stat.count += 1
            stat.lastPlayedAt = date
        } else {
            context.insert(PlayStat(key: key, count: 1, lastPlayedAt: date))
        }
        try? context.save()
    }

    public func playCounts() -> [String: Int] {
        let stats = (try? context.fetch(FetchDescriptor<PlayStat>())) ?? []
        return Dictionary(stats.map { ($0.key, $0.count) }, uniquingKeysWith: { first, _ in first })
    }

    public func recentKeys(limit: Int) -> [String] {
        guard limit > 0 else { return [] }
        var descriptor = FetchDescriptor<PlayStat>(
            sortBy: [SortDescriptor(\.lastPlayedAt, order: .reverse), SortDescriptor(\.key, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return ((try? context.fetch(descriptor)) ?? []).map(\.key)
    }

    private func playStat(key: String) throws -> PlayStat? {
        var descriptor = FetchDescriptor<PlayStat>(predicate: #Predicate { $0.key == key })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    // MARK: - Playlists

    public func playlists() -> [Playlist] {
        let descriptor = FetchDescriptor<PlaylistRecord>(
            sortBy: [SortDescriptor(\.createdAt, order: .reverse), SortDescriptor(\.name)]
        )
        return ((try? context.fetch(descriptor)) ?? []).map {
            Playlist(id: $0.id, name: $0.name, trackKeys: $0.trackKeys, createdAt: $0.createdAt)
        }
    }

    @discardableResult
    public func createPlaylist(name: String) -> Playlist {
        let playlist = Playlist(name: name.trimmingCharacters(in: .whitespacesAndNewlines))
        context.insert(
            PlaylistRecord(id: playlist.id, name: playlist.name, createdAt: playlist.createdAt, trackKeys: [])
        )
        try? context.save()
        return playlist
    }

    public func renamePlaylist(id: UUID, name: String) {
        guard let record = try? playlistRecord(id: id) else { return }
        record.name = name
        try? context.save()
    }

    public func deletePlaylist(id: UUID) {
        guard let record = try? playlistRecord(id: id) else { return }
        context.delete(record)
        try? context.save()
    }

    public func addToPlaylist(id: UUID, keys: [String]) {
        guard let record = try? playlistRecord(id: id) else { return }
        record.trackKeys.append(contentsOf: keys)
        try? context.save()
    }

    public func removeFromPlaylist(id: UUID, at index: Int) {
        guard let record = try? playlistRecord(id: id) else { return }
        guard record.trackKeys.indices.contains(index) else { return }
        record.trackKeys.remove(at: index)
        try? context.save()
    }

    public func movePlaylistEntry(id: UUID, from: Int, to: Int) {
        guard let record = try? playlistRecord(id: id) else { return }
        guard record.trackKeys.indices.contains(from) else { return }
        let key = record.trackKeys.remove(at: from)
        let target = min(max(to, 0), record.trackKeys.count)
        record.trackKeys.insert(key, at: target)
        try? context.save()
    }

    private func playlistRecord(id: UUID) throws -> PlaylistRecord? {
        var descriptor = FetchDescriptor<PlaylistRecord>(predicate: #Predicate { $0.id == id })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }

    // MARK: - Lyrics

    public func lyrics(key: String) -> LyricsDocument? {
        guard let record = try? lyricsRecord(key: key) else { return nil }
        return try? decoder.decode(LyricsDocument.self, from: record.payload)
    }

    public func saveLyrics(_ lyrics: LyricsDocument, key: String) {
        guard let payload = try? encoder.encode(lyrics) else { return }
        if let record = try? lyricsRecord(key: key) {
            record.payload = payload
        } else {
            context.insert(LyricsRecord(key: key, payload: payload))
        }
        try? context.save()
    }

    private func lyricsRecord(key: String) throws -> LyricsRecord? {
        var descriptor = FetchDescriptor<LyricsRecord>(predicate: #Predicate { $0.key == key })
        descriptor.fetchLimit = 1
        return try context.fetch(descriptor).first
    }
}
