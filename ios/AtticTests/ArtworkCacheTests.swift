import AVFoundation
import AtticCore
import AtticLibrary
import UIKit
import XCTest

final class ArtworkCacheTests: XCTestCase {
    private func makeFolder() throws -> URL {
        let folder = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
            .appendingPathComponent("ArtworkCacheTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }

    /// One second of silence, 44.1 kHz mono, plus a cover next to it.
    private func makeSong(in folder: URL) throws -> URL {
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

        try TestCover.write(named: "Cover.PNG", in: folder)
        return url
    }

    private func makeTrack(file: URL, hasArtwork: Bool) -> Track {
        let scanned = ScannedFile(
            rootID: UUID(), relativePath: "Music/Artist/", fileName: "01 Song.wav",
            url: file, modified: Date(), size: 1
        )
        return LibraryIndex.makeTrack(file: scanned, tags: RawTags(hasArtwork: hasArtwork), dateAdded: Date())
    }

    func testImageIsDownsampledToMaxPixel() async throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = try makeSong(in: folder)
        let track = makeTrack(file: url, hasArtwork: true)
        let cache = ArtworkCache(reader: MetadataReader(), locate: { _ in url })
        let fetched = await cache.image(for: track, maxPixel: 2)
        let image = try XCTUnwrap(fetched)
        let longestSide = max(image.size.width * image.scale, image.size.height * image.scale)
        XCTAssertLessThanOrEqual(longestSide, 2)
    }

    func testSongWithoutArtworkHasNoImage() async throws {
        let folder = try makeFolder()
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = try makeSong(in: folder)
        let track = makeTrack(file: url, hasArtwork: false)
        let cache = ArtworkCache(reader: MetadataReader(), locate: { _ in url })

        let image = await cache.image(for: track, maxPixel: 64)
        XCTAssertNil(image)
    }
}
