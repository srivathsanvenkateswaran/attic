import AtticCore
import Foundation

/// The SwiftData store.
/// STUB: implemented by the "SwiftData store" task (in-memory placeholder until then).
@MainActor
public final class LibraryStore: LibraryStoring {
    private var tracks: [Track] = []
    private var roots: [LibraryRoot] = []
    private var liked: [String] = []
    private var plays: [(String, Date)] = []
    private var lists: [Playlist] = []
    private var lyricsByKey: [String: LyricsDocument] = [:]

    /// `inMemory` is for tests and previews.
    public init(inMemory: Bool = false) {}

    public func loadTracks() -> [Track] { tracks }
    public func saveTracks(_ tracks: [Track]) { self.tracks = tracks }
    public func loadRoots() -> [LibraryRoot] { roots }
    public func saveRoots(_ roots: [LibraryRoot]) { self.roots = roots }
    public func likedKeys() -> [String] { liked }
    public func setLiked(_ isLiked: Bool, key: String) {
        liked.removeAll { $0 == key }
        if isLiked { liked.insert(key, at: 0) }
    }
    public func recordPlay(key: String, at date: Date) { plays.append((key, date)) }
    public func playCounts() -> [String: Int] { plays.reduce(into: [:]) { $0[$1.0, default: 0] += 1 } }
    public func recentKeys(limit: Int) -> [String] {
        var seen = Set<String>()
        return plays.reversed().map(\.0).filter { seen.insert($0).inserted }.prefix(limit).map { $0 }
    }
    public func playlists() -> [Playlist] { lists }
    public func createPlaylist(name: String) -> Playlist {
        let list = Playlist(name: name)
        lists.insert(list, at: 0)
        return list
    }
    public func renamePlaylist(id: UUID, name: String) {}
    public func deletePlaylist(id: UUID) { lists.removeAll { $0.id == id } }
    public func addToPlaylist(id: UUID, keys: [String]) {}
    public func removeFromPlaylist(id: UUID, at index: Int) {}
    public func movePlaylistEntry(id: UUID, from: Int, to: Int) {}
    public func lyrics(key: String) -> LyricsDocument? { lyricsByKey[key] }
    public func saveLyrics(_ lyrics: LyricsDocument, key: String) { lyricsByKey[key] = lyrics }
}
