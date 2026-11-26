import Foundation
import UIKit

// The contracts between modules. AtticLibrary and AtticPlayback implement them; AtticUI's
// AppModel composes them. Code against these, not against another module's concrete types.

// MARK: - Library

/// Keeps the picked folders as security-scoped bookmarks and opens their scopes.
@MainActor
public protocol FolderAccessing: AnyObject {
    /// The saved roots, in the order they were added.
    var roots: [LibraryRoot] { get }
    /// Makes a bookmark for a folder URL from the Files picker (`.fileImporter` with `.folder`),
    /// saves it and returns the new root. Adding a folder already present returns the existing root.
    func addFolder(_ url: URL) throws -> LibraryRoot
    func removeFolder(id: UUID)
    /// Resolves a root's bookmark, starts its security scope (once; it stays open for the app's
    /// life) and refreshes a stale bookmark. Nil if the folder is gone.
    func resolve(_ root: LibraryRoot) -> URL?
    /// The file URL of a song, with its root's scope open. Nil if the root is gone.
    func fileURL(for track: Track) -> URL?
}

/// Walks a root folder for audio files.
public protocol FolderScanning: Sendable {
    /// Every audio file under `url` (recursively; mp3, m4a, aac, flac, wav, aif/aiff, caf, alac in m4a),
    /// skipping hidden files and packages. `url` is the root's resolved URL with its scope open.
    func scan(root: LibraryRoot, at url: URL) async -> [ScannedFile]
}

/// Reads tags and artwork with AVFoundation.
public protocol MetadataReading: Sendable {
    func tags(for url: URL) async -> RawTags
    /// The embedded cover (or a cover.jpg / folder.jpg / front.jpg / cover.png next to the file).
    func artworkData(for url: URL) async -> Data?
}

/// Covers for display, decoded and downsampled off the main thread and cached in memory and on disk.
public protocol ArtworkProviding: Sendable {
    /// `maxPixel` is the longest side in pixels. Nil if the song has no cover.
    func image(for track: Track, maxPixel: Int) async -> UIImage?
}

/// The SwiftData store: songs from the last scan and every piece of user data, keyed by song key.
@MainActor
public protocol LibraryStoring: AnyObject {
    // Songs
    func loadTracks() -> [Track]
    /// Replaces the stored songs with `tracks` (insert new keys, update changed ones, delete missing).
    func saveTracks(_ tracks: [Track])

    // Folders
    func loadRoots() -> [LibraryRoot]
    func saveRoots(_ roots: [LibraryRoot])

    // Likes (Favourites)
    func likedKeys() -> [String]   // most recently liked first
    func setLiked(_ liked: Bool, key: String)

    // Plays
    func recordPlay(key: String, at date: Date)
    func playCounts() -> [String: Int]
    /// Distinct keys, most recently played first.
    func recentKeys(limit: Int) -> [String]

    // Playlists
    func playlists() -> [Playlist]   // newest first
    @discardableResult func createPlaylist(name: String) -> Playlist
    func renamePlaylist(id: UUID, name: String)
    func deletePlaylist(id: UUID)
    func addToPlaylist(id: UUID, keys: [String])
    func removeFromPlaylist(id: UUID, at index: Int)
    func movePlaylistEntry(id: UUID, from: Int, to: Int)

    // Lyrics cache
    func lyrics(key: String) -> LyricsDocument?
    func saveLyrics(_ lyrics: LyricsDocument, key: String)
}

/// Looks up lyrics on LRCLIB, the app's only network call.
public protocol LyricsFetching: Sendable {
    /// Sends only title, artist, album and duration. Returns `.notFound` for a 404.
    /// Throws on network errors so the caller does not cache a failure.
    func fetch(title: String, artist: String, album: String?, durationSec: Int) async throws -> LyricsDocument
}

// MARK: - Playback

/// What the player shows. `queue` is in play order (shuffled order when shuffle is on).
public struct PlayerState: Equatable, Sendable {
    public var queue: [Track]
    public var currentIndex: Int
    public var isPlaying: Bool
    public var shuffle: Bool
    public var repeatMode: RepeatMode
    /// What the queue plays from ("Favourites", an album or playlist name). Nil means the whole library.
    public var contextName: String?
    public var durationMs: Int64

    public init(
        queue: [Track] = [], currentIndex: Int = 0, isPlaying: Bool = false, shuffle: Bool = false,
        repeatMode: RepeatMode = .off, contextName: String? = nil, durationMs: Int64 = 0
    ) {
        self.queue = queue
        self.currentIndex = currentIndex
        self.isPlaying = isPlaying
        self.shuffle = shuffle
        self.repeatMode = repeatMode
        self.contextName = contextName
        self.durationMs = durationMs
    }

    public var current: Track? { queue.indices.contains(currentIndex) ? queue[currentIndex] : nil }
    public var upNext: [Track] { Array(queue.dropFirst(currentIndex + 1)) }
}

/// The audio engine. Implementations are `@Observable` classes, so SwiftUI redraws when `state` changes.
@MainActor
public protocol PlaybackControlling: AnyObject {
    var state: PlayerState { get }
    /// Where the current song is. Not part of `state`; a view polls it (TimelineView) at the rate it needs.
    func positionMs() -> Int64

    /// Replaces the queue and starts playing `tracks[startAt]`. `shuffle` nil keeps the current shuffle setting.
    func play(_ tracks: [Track], startAt: Int, context: String?, shuffle: Bool?)
    func togglePlayPause()
    func pause()
    func resume()
    func next()
    /// Restarts the song when it is past 3 seconds, otherwise goes to the previous one.
    func previous()
    func seek(toMs ms: Int64)
    func toggleShuffle()
    func cycleRepeat()
    /// `queueIndex` is a play-order index into `state.queue`.
    func jumpTo(_ queueIndex: Int)
    func remove(_ queueIndex: Int)
    func move(from: Int, to: Int)
    func clearUpNext()
    func playNext(_ tracks: [Track])
    func addToQueue(_ tracks: [Track])

    /// Called on the main actor each time a song starts (or restarts by repeat-one), for play counts and recents.
    var onTrackStarted: ((Track) -> Void)? { get set }
}

/// Mirrors the player on the lock screen and in Control Centre, and routes their buttons back.
@MainActor
public protocol NowPlayingPublishing: AnyObject {
    /// Registers MPRemoteCommandCenter handlers (play, pause, toggle, next, previous, changePlaybackPosition)
    /// that call `player`, and keeps MPNowPlayingInfoCenter current from then on by observing
    /// `player.state` (withObservationTracking) and refreshing elapsed time on play, pause and seek.
    func attach(to player: PlaybackControlling, artwork: ArtworkProviding)
    /// Call after any state change or seek.
    func update()
}
