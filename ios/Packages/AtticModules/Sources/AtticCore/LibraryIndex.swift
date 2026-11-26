import Foundation

/// Turns scanned files and their tags into songs, and songs into a grouped [Library].
public enum LibraryIndex {
    /// Builds one song from a file and its tags. Title falls back to the file name without its
    /// extension; artist falls back to "Unknown artist". `dateAdded` is when this key was first seen
    /// (pass the stored value on a rescan, or now).
    public static func makeTrack(file: ScannedFile, tags: RawTags, dateAdded: Date) -> Track {
        let stem = (file.fileName as NSString).deletingPathExtension
        let artist = trimmed(tags.artist) ?? trimmed(tags.albumArtist) ?? "Unknown artist"
        let album = trimmed(tags.album)
        let albumArtist = trimmed(tags.albumArtist)
        // An album with no album-artist tag groups by folder, so a compilation filed in one folder
        // stays one album while the same name in another folder stays separate.
        let albumKey = album.map {
            TrackKey.normalize($0) + "|" + (albumArtist.map(TrackKey.normalize) ?? file.relativePath)
        }
        return Track(
            key: file.key, rootID: file.rootID, relativePath: file.relativePath, fileName: file.fileName,
            title: trimmed(tags.title) ?? fileTitle(stem: stem), artist: artist,
            artistKeys: deduped(splitArtists(artist).map(TrackKey.normalize)),
            album: album, albumKey: albumKey, albumArtist: albumArtist,
            folder: file.relativePath.split(separator: "/").last.map(String.init),
            trackNumber: tags.trackNumber, discNumber: tags.discNumber, durationMs: tags.durationMs,
            year: tags.year, dateAdded: dateAdded, genre: trimmed(tags.genre),
            composer: trimmed(tags.composer), hasArtwork: tags.hasArtwork, modified: file.modified
        )
    }

    /// Groups songs into albums, artists and folders, and sorts everything.
    public static func build(_ tracks: [Track]) -> Library {
        let sorted = tracks.sorted(by: trackOrder)

        var library = Library.empty
        library.tracks = sorted
        library.trackByKey = Dictionary(sorted.map { ($0.key, $0) }, uniquingKeysWith: { a, _ in a })

        var groupTracks: [String: [Track]] = [:]
        var groupOrder: [String] = []
        for track in sorted {
            guard let key = track.albumKey else { continue }
            if groupTracks[key] == nil { groupOrder.append(key) }
            groupTracks[key, default: []].append(track)
        }
        var albums: [Album] = []
        var albumByKey: [String: Album] = [:]
        for key in groupOrder {
            let album = makeAlbum(key: key, group: groupTracks[key] ?? [])
            albums.append(album)
            albumByKey[key] = album
        }
        albums.sort(by: albumOrder)

        // An artist's display name is the credit as tagged; the first spelling seen wins, while
        // keys stay normalized.
        var artistNames: [String: String] = [:]
        var tracksByArtist: [String: [Track]] = [:]
        for track in sorted {
            for credit in splitArtists(track.artist) {
                let key = TrackKey.normalize(credit)
                if artistNames[key] == nil { artistNames[key] = credit }
            }
            for key in track.artistKeys {
                tracksByArtist[key, default: []].append(track)
            }
        }
        // Collected from the albums rather than by scanning every track per artist.
        var albumKeysByArtist: [String: Set<String>] = [:]
        for album in albums {
            for track in album.tracks {
                for key in track.artistKeys {
                    albumKeysByArtist[key, default: []].insert(album.key)
                }
            }
        }
        var artists: [Artist] = []
        var artistByKey: [String: Artist] = [:]
        for (key, list) in tracksByArtist {
            let ownAlbums = (albumKeysByArtist[key] ?? []).compactMap { albumByKey[$0] }
                .sorted(by: albumYearOrder)
            let artist = Artist(
                key: key, name: artistNames[key] ?? key,
                tracks: list.sorted(by: artistTrackOrder), albums: ownAlbums
            )
            artists.append(artist)
            artistByKey[key] = artist
        }
        artists.sort(by: artistOrder)

        var tracksByPath: [String: [Track]] = [:]
        var pathOrder: [String] = []
        for track in sorted {
            if tracksByPath[track.relativePath] == nil { pathOrder.append(track.relativePath) }
            tracksByPath[track.relativePath, default: []].append(track)
        }
        var folders: [Folder] = []
        var folderByPath: [String: Folder] = [:]
        for path in pathOrder {
            let list = (tracksByPath[path] ?? []).sorted(by: fileNameOrder)
            let folder = Folder(name: list.first?.folder ?? path, path: path, tracks: list)
            folders.append(folder)
            folderByPath[path] = folder
        }
        folders.sort(by: folderOrder)

        library.albums = albums
        library.artists = artists
        library.folders = folders
        library.albumByKey = albumByKey
        library.artistByKey = artistByKey
        library.folderByPath = folderByPath
        return library
    }

    /// Splits a tag's artist credits on the separators tags use. "&" and "," are not separators:
    /// "Simon & Garfunkel" and "Tyler, The Creator" are single names.
    public static func splitArtists(_ s: String) -> [String] {
        let replaced = creditSeparators.stringByReplacingMatches(
            in: s, options: [], range: NSRange(s.startIndex..<s.endIndex, in: s), withTemplate: "\u{0}"
        )
        let parts = replaced.components(separatedBy: "\u{0}")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? [s] : parts
    }

    private static let creditSeparators = try! NSRegularExpression(
        pattern: #";| / | featuring | feat\. | feat | ft\. "#, options: [.caseInsensitive]
    )

    /// A file name's title is its stem minus a leading track number ("01 - Intro" -> "Intro").
    private static func fileTitle(stem: String) -> String {
        guard let range = stem.range(of: #"^\d{1,3}\s*[-._)]?\s+"#, options: .regularExpression) else {
            return stem
        }
        let rest = String(stem[range.upperBound...]).trimmingCharacters(in: .whitespacesAndNewlines)
        return rest.isEmpty ? stem : rest
    }

    private static func makeAlbum(key: String, group: [Track]) -> Album {
        let albumArtist = group.compactMap(\.albumArtist).first
        let firstArtist = group.first?.artist
        let sharedArtist = group.allSatisfy({ $0.artist == firstArtist }) ? firstArtist : nil
        let artist: String
        let artistKey: String?
        if let albumArtist {
            artist = albumArtist
            artistKey = TrackKey.normalize(albumArtist)
        } else if let sharedArtist {
            artist = sharedArtist
            artistKey = group.first?.artistKeys.first
        } else {
            artist = "Various artists"
            artistKey = nil
        }
        return Album(
            key: key, name: group.compactMap(\.album).first ?? key, artist: artist, artistKey: artistKey,
            tracks: group.sorted(by: albumTrackOrder), year: group.compactMap(\.year).max()
        )
    }

    private static func trimmed(_ text: String?) -> String? {
        guard let text else { return nil }
        let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    private static func deduped(_ keys: [String]) -> [String] {
        var seen = Set<String>()
        return keys.filter { seen.insert($0).inserted }
    }

    private static func trackOrder(_ a: Track, _ b: Track) -> Bool {
        let c = a.title.localizedStandardCompare(b.title)
        if c != .orderedSame { return c == .orderedAscending }
        let byArtist = a.artist.localizedStandardCompare(b.artist)
        if byArtist != .orderedSame { return byArtist == .orderedAscending }
        return a.key < b.key
    }

    private static func albumOrder(_ a: Album, _ b: Album) -> Bool {
        let c = a.name.localizedStandardCompare(b.name)
        if c != .orderedSame { return c == .orderedAscending }
        let byArtist = a.artist.localizedStandardCompare(b.artist)
        if byArtist != .orderedSame { return byArtist == .orderedAscending }
        return a.key < b.key
    }

    private static func albumTrackOrder(_ a: Track, _ b: Track) -> Bool {
        let aDisc = a.discNumber ?? 1
        let bDisc = b.discNumber ?? 1
        if aDisc != bDisc { return aDisc < bDisc }
        let aNumber = a.trackNumber ?? Int.max
        let bNumber = b.trackNumber ?? Int.max
        if aNumber != bNumber { return aNumber < bNumber }
        return a.title.localizedStandardCompare(b.title) == .orderedAscending
    }

    private static func artistTrackOrder(_ a: Track, _ b: Track) -> Bool {
        let c = (a.album ?? "").localizedStandardCompare(b.album ?? "")
        if c != .orderedSame { return c == .orderedAscending }
        return albumTrackOrder(a, b)
    }

    private static func albumYearOrder(_ a: Album, _ b: Album) -> Bool {
        if let aYear = a.year, let bYear = b.year, aYear != bYear { return aYear > bYear }
        if a.year != nil, b.year == nil { return true }
        if a.year == nil, b.year != nil { return false }
        return albumOrder(a, b)
    }

    private static func artistOrder(_ a: Artist, _ b: Artist) -> Bool {
        let c = a.name.localizedStandardCompare(b.name)
        if c != .orderedSame { return c == .orderedAscending }
        return a.key < b.key
    }

    private static func fileNameOrder(_ a: Track, _ b: Track) -> Bool {
        a.fileName.localizedStandardCompare(b.fileName) == .orderedAscending
    }

    private static func folderOrder(_ a: Folder, _ b: Folder) -> Bool {
        let c = a.name.localizedStandardCompare(b.name)
        if c != .orderedSame { return c == .orderedAscending }
        return a.path.localizedStandardCompare(b.path) == .orderedAscending
    }
}
