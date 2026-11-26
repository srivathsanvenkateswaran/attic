import AtticCore
import AtticLibrary
import AtticPlayback
import Foundation
import Observation
import UIKit

/// Everything the screens read and do, in one place. Screen models (Models/) are built from it,
/// like the `remember…Model` functions on Android; a screen never reaches the services directly.
@MainActor
@Observable
public final class AppModel {
    public let library: LibraryService
    public let player: AudioPlayer
    @ObservationIgnored public let store: LibraryStoring
    @ObservationIgnored public let artwork: ArtworkProviding
    @ObservationIgnored public let lyricsFetcher: LyricsFetching
    @ObservationIgnored public let nowPlaying: NowPlayingPublishing

    /// Song keys, most recently liked first.
    public private(set) var likedKeys: [String] = []
    public private(set) var likedSet: Set<String> = []
    public private(set) var playlists: [Playlist] = []
    public private(set) var recentKeys: [String] = []
    public private(set) var playCounts: [String: Int] = [:]
    public var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: "themeMode") }
    }

    public init(inMemory: Bool = false) {
        let store = LibraryStore(inMemory: inMemory)
        let folders = FolderAccess(store: store)
        let reader = MetadataReader()
        self.store = store
        library = LibraryService(store: store, folders: folders, scanner: FolderScanner(), reader: reader)
        player = AudioPlayer(locate: { folders.fileURL(for: $0) })
        artwork = ArtworkCache(reader: reader, locate: { folders.fileURL(for: $0) })
        lyricsFetcher = LRCLibClient()
        nowPlaying = NowPlayingCenter()
        themeMode = ThemeMode(rawValue: UserDefaults.standard.string(forKey: "themeMode") ?? "") ?? .system
        reloadUserData()
        player.onTrackStarted = { [weak self] track in
            self?.store.recordPlay(key: track.key, at: Date())
            self?.reloadPlays()
        }
        nowPlaying.attach(to: player, artwork: artwork)
    }

    /// Rescans on launch.
    public func start() async {
        await library.rescan()
    }

    // MARK: Playback

    /// Plays `tracks` from `startAt`. `context` is what Now Playing says it plays from
    /// (an album, playlist or folder name, or `S.favourites`); nil for the whole library.
    public func play(_ tracks: [Track], startAt: Int = 0, context: String?, shuffle: Bool? = nil) {
        guard !tracks.isEmpty else { return }
        player.play(tracks, startAt: startAt, context: context, shuffle: shuffle)
    }

    /// Plays `tracks` shuffled, from a random song.
    public func shuffle(_ tracks: [Track], context: String?) {
        guard !tracks.isEmpty else { return }
        player.play(tracks, startAt: Int.random(in: 0..<tracks.count), context: context, shuffle: true)
    }

    /// Seeks and tells the lock screen, whose elapsed time would otherwise drift.
    public func seek(toMs ms: Int64) {
        player.seek(toMs: ms)
        nowPlaying.update()
    }

    /// True when `context` is what the player is playing from.
    public func isPlayingFrom(_ context: String) -> Bool {
        player.state.contextName == context && player.state.current != nil
    }

    // MARK: User data

    public func isLiked(_ track: Track?) -> Bool { track.map { likedSet.contains($0.key) } ?? false }

    public func toggleLike(_ track: Track) {
        store.setLiked(!likedSet.contains(track.key), key: track.key)
        reloadUserData()
    }

    @discardableResult
    public func createPlaylist(name: String) -> Playlist {
        let list = store.createPlaylist(name: name)
        reloadUserData()
        return list
    }

    public func renamePlaylist(_ id: UUID, name: String) { store.renamePlaylist(id: id, name: name); reloadUserData() }
    public func deletePlaylist(_ id: UUID) { store.deletePlaylist(id: id); reloadUserData() }
    public func addToPlaylist(_ id: UUID, tracks: [Track]) { store.addToPlaylist(id: id, keys: tracks.map(\.key)); reloadUserData() }
    public func removeFromPlaylist(_ id: UUID, at index: Int) { store.removeFromPlaylist(id: id, at: index); reloadUserData() }
    public func movePlaylistEntry(_ id: UUID, from: Int, to: Int) { store.movePlaylistEntry(id: id, from: from, to: to); reloadUserData() }

    /// The songs of a playlist that are in the library, in playlist order.
    public func tracks(of playlist: Playlist) -> [Track] {
        playlist.trackKeys.compactMap { library.library.trackByKey[$0] }
    }

    public var favourites: [Track] { likedKeys.compactMap { library.library.trackByKey[$0] } }

    public func reloadUserData() {
        likedKeys = store.likedKeys()
        likedSet = Set(likedKeys)
        playlists = store.playlists()
        reloadPlays()
    }

    private func reloadPlays() {
        recentKeys = store.recentKeys(limit: 200)
        playCounts = store.playCounts()
    }

    // MARK: Lyrics

    /// Cached lyrics, or a lookup on LRCLIB that is then cached. Nil on a network error.
    public func lyrics(for track: Track) async -> LyricsDocument? {
        if let saved = store.lyrics(key: track.key) { return saved }
        do {
            let found = try await lyricsFetcher.fetch(
                title: track.title, artist: track.artist, album: track.album,
                durationSec: Int(track.durationMs / 1000)
            )
            store.saveLyrics(found, key: track.key)
            return found
        } catch {
            return nil
        }
    }
}
