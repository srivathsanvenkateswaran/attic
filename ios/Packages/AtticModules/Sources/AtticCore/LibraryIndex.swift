import Foundation

/// Turns scanned files and their tags into songs, and songs into a grouped [Library].
/// STUB: implemented by the "library grouping" task. Keep these signatures.
public enum LibraryIndex {
    /// Builds one song from a file and its tags. Title falls back to the file name without its
    /// extension; artist falls back to "Unknown artist". `dateAdded` is when this key was first seen
    /// (pass the stored value on a rescan, or now).
    public static func makeTrack(file: ScannedFile, tags: RawTags, dateAdded: Date) -> Track {
        let stem = (file.fileName as NSString).deletingPathExtension
        let artist = tags.artist ?? "Unknown artist"
        return Track(
            key: file.key, rootID: file.rootID, relativePath: file.relativePath, fileName: file.fileName,
            title: tags.title ?? stem, artist: artist, artistKeys: [TrackKey.normalize(artist)],
            album: tags.album, albumKey: tags.album.map { TrackKey.normalize($0) }, albumArtist: tags.albumArtist,
            folder: file.relativePath.split(separator: "/").last.map(String.init), trackNumber: tags.trackNumber,
            discNumber: tags.discNumber, durationMs: tags.durationMs, year: tags.year, dateAdded: dateAdded,
            genre: tags.genre, composer: tags.composer, hasArtwork: tags.hasArtwork, modified: file.modified
        )
    }

    /// Groups songs into albums, artists and folders, and sorts everything.
    public static func build(_ tracks: [Track]) -> Library {
        let sorted = tracks.sorted { $0.title.localizedStandardCompare($1.title) == .orderedAscending }
        var library = Library.empty
        library.tracks = sorted
        library.trackByKey = Dictionary(sorted.map { ($0.key, $0) }, uniquingKeysWith: { a, _ in a })
        return library
    }
}
