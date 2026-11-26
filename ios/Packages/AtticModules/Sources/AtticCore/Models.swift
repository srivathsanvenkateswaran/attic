import Foundation

/// A folder the user picked in the Files picker. Songs under it are keyed relative to its parent,
/// so a song's key starts with this folder's own name, like Android's `relativePath` starts with "Music/".
public struct LibraryRoot: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    /// The folder's own name, such as "Music". It is the first component of every key under it.
    public var name: String
    /// A security-scoped bookmark to the folder (`URL.bookmarkData()`).
    public var bookmark: Data

    public init(id: UUID = UUID(), name: String, bookmark: Data) {
        self.id = id
        self.name = name
        self.bookmark = bookmark
    }
}

/// One audio file found by a scan, before its tags are read.
public struct ScannedFile: Hashable, Sendable {
    public let rootID: UUID
    /// The directory the file is in, starting with the root's name and ending in "/", e.g. "Music/Artist/Album/".
    public let relativePath: String
    /// The file name with its extension, e.g. "01 Intro.flac".
    public let fileName: String
    /// Where the file is now. Only valid while the root's security scope is open.
    public let url: URL
    public let modified: Date
    public let size: Int64

    public init(rootID: UUID, relativePath: String, fileName: String, url: URL, modified: Date, size: Int64) {
        self.rootID = rootID
        self.relativePath = relativePath
        self.fileName = fileName
        self.url = url
        self.modified = modified
        self.size = size
    }

    /// The library key, the same rule as Android's `Track.key`.
    public var key: String { TrackKey.make(relativePath: relativePath, fileName: fileName) }
}

/// A file's tags as AVFoundation reads them. Nil means the tag was absent.
public struct RawTags: Hashable, Sendable {
    public var title: String?
    public var artist: String?
    public var album: String?
    public var albumArtist: String?
    public var trackNumber: Int?
    public var discNumber: Int?
    public var year: Int?
    public var genre: String?
    public var composer: String?
    public var durationMs: Int64
    public var hasArtwork: Bool

    public init(
        title: String? = nil, artist: String? = nil, album: String? = nil, albumArtist: String? = nil,
        trackNumber: Int? = nil, discNumber: Int? = nil, year: Int? = nil, genre: String? = nil,
        composer: String? = nil, durationMs: Int64 = 0, hasArtwork: Bool = false
    ) {
        self.title = title
        self.artist = artist
        self.album = album
        self.albumArtist = albumArtist
        self.trackNumber = trackNumber
        self.discNumber = discNumber
        self.year = year
        self.genre = genre
        self.composer = composer
        self.durationMs = durationMs
        self.hasArtwork = hasArtwork
    }
}

/// A song in the library. Saved data (likes, plays, playlists, lyrics) refers to it by [key].
public struct Track: Identifiable, Hashable, Codable, Sendable {
    public var id: String { key }
    /// `relativePath + fileName`. Never a system ID.
    public let key: String
    public let rootID: UUID
    public let relativePath: String
    public let fileName: String
    public var title: String
    /// The artist as tagged, for display ("A & B").
    public var artist: String
    /// Every credited artist's key; the first is the primary artist.
    public var artistKeys: [String]
    public var album: String?
    public var albumKey: String?
    public var albumArtist: String?
    /// The name of the directory the file is in.
    public var folder: String?
    public var trackNumber: Int?
    public var discNumber: Int?
    public var durationMs: Int64
    public var year: Int?
    public var dateAdded: Date
    public var genre: String?
    public var composer: String?
    public var hasArtwork: Bool
    /// The file's modification date at the last scan, so a rescan can skip unchanged files.
    public var modified: Date

    public init(
        key: String, rootID: UUID, relativePath: String, fileName: String, title: String, artist: String,
        artistKeys: [String], album: String?, albumKey: String?, albumArtist: String?, folder: String?,
        trackNumber: Int?, discNumber: Int?, durationMs: Int64, year: Int?, dateAdded: Date,
        genre: String?, composer: String?, hasArtwork: Bool, modified: Date
    ) {
        self.key = key
        self.rootID = rootID
        self.relativePath = relativePath
        self.fileName = fileName
        self.title = title
        self.artist = artist
        self.artistKeys = artistKeys
        self.album = album
        self.albumKey = albumKey
        self.albumArtist = albumArtist
        self.folder = folder
        self.trackNumber = trackNumber
        self.discNumber = discNumber
        self.durationMs = durationMs
        self.year = year
        self.dateAdded = dateAdded
        self.genre = genre
        self.composer = composer
        self.hasArtwork = hasArtwork
        self.modified = modified
    }
}

public struct Album: Identifiable, Hashable, Sendable {
    public var id: String { key }
    public let key: String
    public var name: String
    public var artist: String
    public var artistKey: String?
    /// In disc, then track-number order.
    public var tracks: [Track]
    public var year: Int?

    public init(key: String, name: String, artist: String, artistKey: String?, tracks: [Track], year: Int?) {
        self.key = key
        self.name = name
        self.artist = artist
        self.artistKey = artistKey
        self.tracks = tracks
        self.year = year
    }
}

public struct Artist: Identifiable, Hashable, Sendable {
    public var id: String { key }
    public let key: String
    public var name: String
    public var tracks: [Track]
    public var albums: [Album]

    public init(key: String, name: String, tracks: [Track], albums: [Album]) {
        self.key = key
        self.name = name
        self.tracks = tracks
        self.albums = albums
    }
}

/// The songs directly in one directory, which Android shows as a folder playlist.
public struct Folder: Identifiable, Hashable, Sendable {
    public var id: String { path }
    /// The directory's own name.
    public var name: String
    /// Its relative path, e.g. "Music/Artist/Album/".
    public var path: String
    public var tracks: [Track]

    public init(name: String, path: String, tracks: [Track]) {
        self.name = name
        self.path = path
        self.tracks = tracks
    }
}

/// The whole library, grouped. Built by `LibraryIndex.build(_:)`.
public struct Library: Sendable {
    /// Every song, sorted by title (case- and diacritic-insensitive).
    public var tracks: [Track]
    /// Sorted by name.
    public var albums: [Album]
    /// Sorted by name.
    public var artists: [Artist]
    /// Sorted by name.
    public var folders: [Folder]
    public var trackByKey: [String: Track]
    public var albumByKey: [String: Album]
    public var artistByKey: [String: Artist]
    public var folderByPath: [String: Folder]

    public init(
        tracks: [Track], albums: [Album], artists: [Artist], folders: [Folder],
        trackByKey: [String: Track], albumByKey: [String: Album], artistByKey: [String: Artist],
        folderByPath: [String: Folder]
    ) {
        self.tracks = tracks
        self.albums = albums
        self.artists = artists
        self.folders = folders
        self.trackByKey = trackByKey
        self.albumByKey = albumByKey
        self.artistByKey = artistByKey
        self.folderByPath = folderByPath
    }

    public static let empty = Library(
        tracks: [], albums: [], artists: [], folders: [], trackByKey: [:], albumByKey: [:],
        artistByKey: [:], folderByPath: [:]
    )

    public var isEmpty: Bool { tracks.isEmpty }
}

public struct Playlist: Identifiable, Hashable, Codable, Sendable {
    public let id: UUID
    public var name: String
    /// Song keys in playlist order. A key whose file is gone is kept, not shown.
    public var trackKeys: [String]
    public var createdAt: Date

    public init(id: UUID = UUID(), name: String, trackKeys: [String] = [], createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.trackKeys = trackKeys
        self.createdAt = createdAt
    }
}

/// Somewhere a tile or row leads, so state can describe a link without holding navigation.
public enum Destination: Hashable, Sendable {
    case album(key: String)
    case artist(key: String)
    case folder(path: String)
    case playlist(id: UUID)
    case favourites
}

public enum ThemeMode: String, CaseIterable, Codable, Sendable {
    case system, light, dark
}
