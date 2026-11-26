import Foundation

/// The metadata blocks at the head of a FLAC file. AVFoundation returns no tags for .flac on iOS,
/// so the reader walks these blocks itself: only as far as the last-block flag, never the audio.
public struct FlacTags: Sendable {
    /// Vorbis comments keyed by their upper-cased name, e.g. "TITLE". Values are trimmed.
    public var comments: [String: String]
    /// The first PICTURE block with data, if the file carries one.
    public var artworkData: Data?
    /// From STREAMINFO's total samples, 0 when the block didn't say.
    public var durationMs: Int64

    public init(comments: [String: String] = [:], artworkData: Data? = nil, durationMs: Int64 = 0) {
        self.comments = comments
        self.artworkData = artworkData
        self.durationMs = durationMs
    }

    /// Reads a file's metadata blocks one at a time. Nil if it isn't a FLAC file.
    public static func read(from url: URL) -> FlacTags? {
        guard let handle = try? FileHandle(forReadingFrom: url) else { return nil }
        defer { try? handle.close() }
        return parse { count in
            guard let data = try? handle.read(upToCount: count), data.count == count else { return nil }
            return data
        }
    }

    /// Parses a complete FLAC head, marker included. Nil if the marker isn't "fLaC".
    public static func parse(_ data: Data) -> FlacTags? {
        var index = data.startIndex
        return parse { count in
            guard index < data.endIndex else { return nil }
            let end = min(index + count, data.endIndex)
            defer { index = end }
            return data.subdata(in: index..<end)
        }
    }

    private static func parse(read: (Int) -> Data?) -> FlacTags? {
        guard let marker = read(4), marker == Data("fLaC".utf8) else { return nil }
        var tags = FlacTags()
        while let header = read(4), header.count == 4 {
            let first = header[header.startIndex]
            let lastBlock = (first & 0x80) != 0
            let type = Int(first & 0x7F)
            let at = header.startIndex
            let length = (Int(header[at + 1]) << 16) | (Int(header[at + 2]) << 8) | Int(header[at + 3])
            guard length >= 0, let body = read(length), body.count == length else { break }
            switch type {
            case 0 where tags.durationMs == 0:
                tags.durationMs = streamInfoDurationMs(body)
            case 4:
                for (key, value) in comments(body) where tags.comments[key] == nil {
                    tags.comments[key] = value
                }
            case 6 where tags.artworkData == nil:
                tags.artworkData = pictureData(body)
            default:
                break
            }
            if lastBlock { break }
        }
        return tags
    }

    /// STREAMINFO: 20 bits of sample rate, 3 of channels, 5 of sample size, 36 of total samples,
    /// all starting at byte 10.
    private static func streamInfoDurationMs(_ body: Data) -> Int64 {
        let bytes = [UInt8](body)
        guard bytes.count >= 18 else { return 0 }
        let sampleRate = (Int(bytes[10]) << 12) | (Int(bytes[11]) << 4) | (Int(bytes[12]) >> 4)
        guard sampleRate > 0 else { return 0 }
        let totalSamples = (UInt64(bytes[13] & 0x0F) << 32) | (UInt64(bytes[14]) << 24)
            | (UInt64(bytes[15]) << 16) | (UInt64(bytes[16]) << 8) | UInt64(bytes[17])
        guard totalSamples > 0 else { return 0 }
        return Int64((Double(totalSamples) * 1000 / Double(sampleRate)).rounded())
    }

    /// A VORBIS_COMMENT block: little-endian lengths, then "KEY=value" strings.
    private static func comments(_ body: Data) -> [String: String] {
        var result: [String: String] = [:]
        let bytes = [UInt8](body)
        var at = 0
        func takeCount() -> Int? {
            guard at + 4 <= bytes.count else { return nil }
            defer { at += 4 }
            return Int(bytes[at]) | Int(bytes[at + 1]) << 8 | Int(bytes[at + 2]) << 16 | Int(bytes[at + 3]) << 24
        }
        guard let vendorLength = takeCount(), vendorLength >= 0, at + vendorLength <= bytes.count else { return [:] }
        at += vendorLength
        guard let count = takeCount(), count >= 0 else { return [:] }
        for _ in 0..<count {
            guard let length = takeCount(), length >= 0, at + length <= bytes.count else { break }
            let value = String(decoding: bytes[at..<(at + length)], as: UTF8.self)
            at += length
            guard let split = value.firstIndex(of: "=") else { continue }
            var key = value[..<split].trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            // Writers spell it both ways; look it up under one name.
            if key == "ALBUM ARTIST" { key = "ALBUMARTIST" }
            let text = value[value.index(after: split)...].trimmingCharacters(in: .whitespacesAndNewlines)
            guard !key.isEmpty, !text.isEmpty, result[key] == nil else { continue }
            result[key] = text
        }
        return result
    }

    /// A PICTURE block: type, mime, description, four sizes, then the image bytes.
    private static func pictureData(_ body: Data) -> Data? {
        let bytes = [UInt8](body)
        var at = 4
        func takeLength() -> Int? {
            guard at + 4 <= bytes.count else { return nil }
            defer { at += 4 }
            return Int(bytes[at]) << 24 | Int(bytes[at + 1]) << 16 | Int(bytes[at + 2]) << 8 | Int(bytes[at + 3])
        }
        guard let mimeLength = takeLength(), mimeLength >= 0, at + mimeLength <= bytes.count else { return nil }
        at += mimeLength
        guard let descriptionLength = takeLength(), descriptionLength >= 0, at + descriptionLength <= bytes.count else { return nil }
        at += descriptionLength
        // Width, height, depth, colour count.
        guard (0..<4).allSatisfy({ _ in takeLength() != nil }) else { return nil }
        guard let imageLength = takeLength(), imageLength > 0, at + imageLength <= bytes.count else { return nil }
        return Data(bytes[at..<(at + imageLength)])
    }
}
