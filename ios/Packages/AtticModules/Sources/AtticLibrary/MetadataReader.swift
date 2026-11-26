import AtticCore
import AVFoundation
import Foundation

/// Reads tags and artwork from a file. AVFoundation does the work for most formats; .flac on iOS
/// is the exception, so those files are also read block by block with [FlacTags].
public struct MetadataReader: MetadataReading {
    /// Covers we accept from the file's own folder, in the order we look for them.
    private static let sidecarNames = [
        "cover.jpg", "folder.jpg", "front.jpg", "cover.png", "folder.png", "front.png", "album.jpg",
    ]

    public init() {}

    public func tags(for url: URL) async -> RawTags {
        let asset = AVURLAsset(url: url)
        var items: [AVMetadataItem] = []
        var durationMs: Int64 = 0

        // A file that won't open still gets whatever the rest of the reading finds.
        do {
            let (duration, common) = try await asset.load(.duration, .commonMetadata)
            if duration.seconds.isFinite, duration.seconds > 0 {
                durationMs = Int64((duration.seconds * 1000).rounded())
            }
            items = common
        } catch {}
        do {
            for format in try await asset.load(.availableMetadataFormats) {
                if let formatItems = try? await asset.loadMetadata(for: format) {
                    items.append(contentsOf: formatItems)
                }
            }
        } catch {}

        var tags = RawTags()
        tags.durationMs = durationMs
        tags.title = await firstText(in: items) { $0.commonKey == .commonKeyTitle }
        tags.artist = await firstText(in: items) { $0.commonKey == .commonKeyArtist }
        tags.album = await firstText(in: items) { $0.commonKey == .commonKeyAlbumName }
        tags.albumArtist = await firstText(in: items) {
            $0.identifier == .id3MetadataBand || $0.identifier == .iTunesMetadataAlbumArtist
        }
        tags.trackNumber = await trackNumber(in: items)
        tags.discNumber = await discNumber(in: items)
        tags.year = await year(in: items)
        tags.genre = await genre(in: items)
        tags.composer = await firstText(in: items) {
            $0.identifier == .id3MetadataComposer || $0.identifier == .iTunesMetadataComposer
                || $0.identifier == .quickTimeMetadataComposer
        }
        tags.hasArtwork = await hasArtwork(in: items)

        if url.pathExtension.lowercased() == "flac" {
            fillFlac(&tags, at: url)
        }
        if !tags.hasArtwork, sidecar(at: url) != nil {
            tags.hasArtwork = true
        }
        return tags
    }

    public func artworkData(for url: URL) async -> Data? {
        if let embedded = await embeddedArtwork(for: url) {
            return embedded
        }
        return sidecar(at: url).flatMap { try? Data(contentsOf: $0) }
    }

    // MARK: - AVFoundation items

    private func firstText(in items: [AVMetadataItem], _ matches: (AVMetadataItem) -> Bool) async -> String? {
        for item in items where matches(item) {
            guard let raw = try? await item.load(.stringValue) else { continue }
            let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            if !text.isEmpty { return text }
        }
        return nil
    }

    private func trackNumber(in items: [AVMetadataItem]) async -> Int? {
        if let id3 = await firstNumber(in: items, matching: { $0.identifier == .id3MetadataTrackNumber }) {
            return id3
        }
        return await firstAtomNumber(in: items, matching: { $0.identifier == .iTunesMetadataTrackNumber })
    }

    private func discNumber(in items: [AVMetadataItem]) async -> Int? {
        if let id3 = await firstNumber(in: items, matching: { $0.identifier == .id3MetadataPartOfASet }) {
            return id3
        }
        return await firstAtomNumber(in: items, matching: { $0.identifier == .iTunesMetadataDiscNumber })
    }

    private func firstNumber(in items: [AVMetadataItem], matching: (AVMetadataItem) -> Bool) async -> Int? {
        for item in items where matching(item) {
            guard let raw = try? await item.load(.stringValue) else { continue }
            if let value = Self.number(in: raw) { return value }
        }
        return nil
    }

    /// The 16-bit big-endian count at bytes 2 and 3 of an iTunes trkn / disk atom.
    private func firstAtomNumber(in items: [AVMetadataItem], matching: (AVMetadataItem) -> Bool) async -> Int? {
        for item in items where matching(item) {
            guard let data = try? await item.load(.dataValue), data.count >= 4 else { continue }
            let bytes = [UInt8](data)
            let value = (Int(bytes[2]) << 8) | Int(bytes[3])
            if value > 0 { return value }
        }
        return nil
    }

    private func year(in items: [AVMetadataItem]) async -> Int? {
        let identifiers: [AVMetadataIdentifier] = [.id3MetadataRecordingTime, .id3MetadataYear, .iTunesMetadataReleaseDate]
        for item in items {
            let byIdentifier = item.identifier.map { identifiers.contains($0) } ?? false
            guard byIdentifier || item.commonKey == .commonKeyCreationDate else { continue }
            guard let raw = try? await item.load(.stringValue) else { continue }
            if let value = Self.year(in: raw) { return value }
        }
        return nil
    }

    private func genre(in items: [AVMetadataItem]) async -> String? {
        let identifiers: [AVMetadataIdentifier] = [.id3MetadataContentType, .iTunesMetadataUserGenre, .quickTimeMetadataGenre]
        for item in items {
            let byIdentifier = item.identifier.map { identifiers.contains($0) } ?? false
            guard byIdentifier || item.commonKey == .commonKeyType else { continue }
            guard let raw = try? await item.load(.stringValue) else { continue }
            if let value = Self.genre(in: raw) { return value }
        }
        return nil
    }

    private func hasArtwork(in items: [AVMetadataItem]) async -> Bool {
        for item in items where item.commonKey == .commonKeyArtwork {
            if let data = try? await item.load(.dataValue), !data.isEmpty { return true }
        }
        return false
    }

    // MARK: - Values

    /// "3/12" and "3" both mean track 3.
    private static func number(in text: String) -> Int? {
        let head = text.components(separatedBy: "/").first ?? text
        guard let value = Int(head.trimmingCharacters(in: .whitespacesAndNewlines)) else { return nil }
        return value > 0 ? value : nil
    }

    /// The first four-digit run that reads as a year, so "2019-04-01" gives 2019.
    private static func year(in text: String) -> Int? {
        let characters = Array(text)
        guard characters.count >= 4 else { return nil }
        for at in 0...(characters.count - 4) {
            let run = String(characters[at..<(at + 4)])
            guard run.allSatisfy({ $0.isASCII && $0.isNumber }) else { continue }
            if let value = Int(run), (1000...2999).contains(value) { return value }
        }
        return nil
    }

    /// ID3 content type can open with a genre number, "(17)Rock"; the number alone has no name,
    /// so a value that is only one is dropped.
    private static func genre(in text: String) -> String? {
        var value = text.trimmingCharacters(in: .whitespacesAndNewlines)
        while let prefix = value.range(of: "^\\([0-9]+\\)", options: .regularExpression) {
            value = value[prefix.upperBound...].trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return value.isEmpty ? nil : value
    }

    // MARK: - FLAC

    private func fillFlac(_ tags: inout RawTags, at url: URL) {
        guard let flac = FlacTags.read(from: url) else { return }
        let comments = flac.comments
        if tags.title == nil { tags.title = comments["TITLE"] }
        if tags.artist == nil { tags.artist = comments["ARTIST"] }
        if tags.album == nil { tags.album = comments["ALBUM"] }
        if tags.albumArtist == nil { tags.albumArtist = comments["ALBUMARTIST"] }
        if tags.trackNumber == nil { tags.trackNumber = comments["TRACKNUMBER"].flatMap { Self.number(in: $0) } }
        if tags.discNumber == nil { tags.discNumber = comments["DISCNUMBER"].flatMap { Self.number(in: $0) } }
        if tags.year == nil {
            tags.year = comments["DATE"].flatMap { Self.year(in: $0) } ?? comments["YEAR"].flatMap { Self.year(in: $0) }
        }
        if tags.genre == nil { tags.genre = comments["GENRE"].flatMap { Self.genre(in: $0) } }
        if tags.composer == nil { tags.composer = comments["COMPOSER"] }
        if tags.durationMs == 0 { tags.durationMs = flac.durationMs }
        if !tags.hasArtwork { tags.hasArtwork = flac.artworkData != nil }
    }

    // MARK: - Artwork

    /// The cover inside the file: a PICTURE block for FLAC, otherwise the common artwork item.
    private func embeddedArtwork(for url: URL) async -> Data? {
        if url.pathExtension.lowercased() == "flac", let picture = FlacTags.read(from: url)?.artworkData {
            return picture
        }
        let items = (try? await AVURLAsset(url: url).load(.commonMetadata)) ?? []
        for item in items where item.commonKey == .commonKeyArtwork {
            if let data = try? await item.load(.dataValue), !data.isEmpty { return data }
        }
        return nil
    }

    /// The cover image sitting in the file's own folder, if there is one.
    private func sidecar(at url: URL) -> URL? {
        let folder = url.deletingLastPathComponent()
        guard let names = try? FileManager.default.contentsOfDirectory(atPath: folder.path) else { return nil }
        for wanted in Self.sidecarNames {
            if let match = names.first(where: { $0.lowercased() == wanted }) {
                return folder.appendingPathComponent(match)
            }
        }
        return nil
    }
}
