import AVFoundation
import AtticCore
import AtticLibrary
import UIKit
import XCTest

final class MetadataReaderTests: XCTestCase {
    private func makeFolder() throws -> URL {
        let folder = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
            .appendingPathComponent("MetadataReaderTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }

    /// One second of silence, 44.1 kHz mono.
    private func writeSilentWAV(in folder: URL) throws -> URL {
        let url = folder.appendingPathComponent("silent.wav")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatLinearPCM),
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16,
            AVLinearPCMIsBigEndianKey: false,
            AVLinearPCMIsFloatKey: false,
            AVLinearPCMIsNonInterleaved: false,
        ]
        let file = try AVAudioFile(
            forWriting: url, settings: settings, commonFormat: .pcmFormatInt16, interleaved: true
        )
        let format = try XCTUnwrap(AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: 44_100, channels: 1, interleaved: true))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 44_100))
        buffer.frameLength = 44_100
        if let samples = buffer.int16ChannelData?[0] {
            samples.update(repeating: 0, count: 44_100)
        }
        try file.write(from: buffer)
        return url
    }

    func testSilentWAVHasDurationAndNoTags() async throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = try writeSilentWAV(in: folder)

        let tags = await MetadataReader().tags(for: url)
        XCTAssertTrue((950...1050).contains(tags.durationMs), "durationMs is \(tags.durationMs)")
        XCTAssertNil(tags.title)
        XCTAssertNil(tags.artist)
        XCTAssertNil(tags.trackNumber)
        XCTAssertFalse(tags.hasArtwork)

        let artwork = await MetadataReader().artworkData(for: url)
        XCTAssertNil(artwork)
    }

    func testCoverNextToTheFileIsArtwork() async throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = try writeSilentWAV(in: folder)
        try TestCover.write(named: "Cover.PNG", in: folder)

        let tags = await MetadataReader().tags(for: url)
        XCTAssertTrue(tags.hasArtwork)

        let artwork = await MetadataReader().artworkData(for: url)
        XCTAssertNotNil(artwork)
    }

    func testFlacFileGivesTagsWithoutAVFoundation() async throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = folder.appendingPathComponent("song.flac")
        try Self.flacHead().write(to: url)

        let tags = await MetadataReader().tags(for: url)
        XCTAssertEqual(tags.title, "Song")
        XCTAssertEqual(tags.artist, "Band")
        XCTAssertEqual(tags.trackNumber, 3)
        XCTAssertEqual(tags.year, 2019)
        XCTAssertEqual(tags.durationMs, 1000)
    }

    func testFlacTagsReadStreamInfoAndVorbisComment() throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = folder.appendingPathComponent("song.flac")
        try Self.flacHead().write(to: url)

        let tags = try XCTUnwrap(FlacTags.read(from: url))
        XCTAssertEqual(tags.comments["TITLE"], "Song")
        XCTAssertEqual(tags.comments["ARTIST"], "Band")
        XCTAssertEqual(tags.comments["TRACKNUMBER"], "3/9")
        XCTAssertEqual(tags.comments["DATE"], "2019-04-01")
        XCTAssertEqual(tags.durationMs, 1000)
        XCTAssertNil(tags.artworkData)
    }

    /// "fLaC", a STREAMINFO block of one second at 44.1 kHz, then a last VORBIS_COMMENT block.
    private func musicFile(_ path: String) throws -> URL {
        let url = URL(fileURLWithPath: "/tmp/attic-ios-music/Music/" + path)
        try XCTSkipUnless(FileManager.default.fileExists(atPath: url.path), "generated test music is missing")
        return url
    }

    func testM4AFileWithITunesTags() async throws {
        let tags = await MetadataReader().tags(for: try musicFile("Mira Okafor/Paper Gardens/02 Green Ink.m4a"))
        XCTAssertEqual(tags.title, "Green Ink")
        XCTAssertEqual(tags.artist, "Mira Okafor")
        XCTAssertEqual(tags.album, "Paper Gardens")
        XCTAssertEqual(tags.trackNumber, 2)
        XCTAssertEqual(tags.year, 2021)
        XCTAssertTrue(tags.hasArtwork)
        XCTAssertGreaterThan(tags.durationMs, 0)
    }

    func testFlacFileWithVorbisCommentsAndSidecar() async throws {
        let url = try musicFile("The Ferns/Weather Report/01 Drizzle.flac")
        let tags = await MetadataReader().tags(for: url)
        XCTAssertEqual(tags.title, "Drizzle")
        XCTAssertEqual(tags.artist, "The Ferns")
        XCTAssertEqual(tags.trackNumber, 1)
        XCTAssertEqual(tags.year, 2019)
        XCTAssertTrue(tags.hasArtwork)
        let artwork = await MetadataReader().artworkData(for: url)
        XCTAssertNotNil(artwork)
    }

    func testMP3FileWithID3Tags() async throws {
        let url = try musicFile("Harbour Lights/Low Tide/01 Salt Air.mp3")
        let tags = await MetadataReader().tags(for: url)
        XCTAssertEqual(tags.title, "Salt Air")
        XCTAssertEqual(tags.album, "Low Tide")
        XCTAssertTrue(tags.hasArtwork)
        let artwork = await MetadataReader().artworkData(for: url)
        XCTAssertNotNil(artwork)
    }

    private static func flacHead() -> Data {
        var bytes: [UInt8] = [0x66, 0x4C, 0x61, 0x43]

        var streamInfo = [UInt8](repeating: 0, count: 34)
        let word: UInt64 = (UInt64(44_100) << 44) | (UInt64(15) << 36) | UInt64(44_100)
        for at in 0..<8 {
            streamInfo[10 + at] = UInt8((word >> (56 - 8 * at)) & 0xFF)
        }
        bytes += [0x00, 0x00, 0x00, 0x22] + streamInfo

        let entries = ["TITLE=Song", "ARTIST=Band", "TRACKNUMBER=3/9", "DATE=2019-04-01"]
        var comment = littleEndian(8) + Array("libattic".utf8)
        comment += littleEndian(UInt32(entries.count))
        for entry in entries {
            comment += littleEndian(UInt32(entry.utf8.count)) + Array(entry.utf8)
        }
        bytes += [0x84] + bigEndian24(UInt32(comment.count)) + comment
        return Data(bytes)
    }

    private static func littleEndian(_ value: UInt32) -> [UInt8] {
        [UInt8(value & 0xFF), UInt8((value >> 8) & 0xFF), UInt8((value >> 16) & 0xFF), UInt8((value >> 24) & 0xFF)]
    }

    /// A block header carries its length in 24 bits, not 32.
    private static func bigEndian24(_ value: UInt32) -> [UInt8] {
        [UInt8((value >> 16) & 0xFF), UInt8((value >> 8) & 0xFF), UInt8(value & 0xFF)]
    }
}

/// A 4x4 red PNG built with CoreGraphics; UIGraphicsImageRenderer stalls off the main thread here.
enum TestCover {
    static func write(named name: String, in folder: URL) throws {
        let space = CGColorSpaceCreateDeviceRGB()
        let context = try XCTUnwrap(CGContext(
            data: nil, width: 4, height: 4, bitsPerComponent: 8, bytesPerRow: 0, space: space,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ))
        context.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 4, height: 4))
        let image = try XCTUnwrap(context.makeImage())
        try XCTUnwrap(UIImage(cgImage: image).pngData()).write(to: folder.appendingPathComponent(name))
    }
}
