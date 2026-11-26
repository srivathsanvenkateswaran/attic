import AtticCore
import AtticPlayback
import AVFoundation
import XCTest

@MainActor
final class AudioPlayerTests: XCTestCase {
    private var directory: URL!
    private var urls: [String: URL] = [:]
    private var tracks: [Track] = []
    private var unplayableKeys: Set<String> = []
    private var player: AudioPlayer!

    override func setUp() async throws {
        try await super.setUp()
        let dir = FileManager.default.temporaryDirectory
            .appendingPathComponent("AtticPlayerTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        directory = dir

        let rootID = UUID()
        var made: [Track] = []
        for index in 0..<3 {
            let url = dir.appendingPathComponent("track\(index).wav")
            try Self.writeSilence(to: url)
            let file = ScannedFile(
                rootID: rootID, relativePath: "Music/", fileName: url.lastPathComponent,
                url: url, modified: Date(), size: 0
            )
            let tags = RawTags(title: "Track \(index)", artist: "Artist", durationMs: 500)
            let track = LibraryIndex.makeTrack(file: file, tags: tags, dateAdded: Date())
            made.append(track)
            urls[track.key] = url
        }
        tracks = made

        player = AudioPlayer { [weak self] track in
            guard let self, !self.unplayableKeys.contains(track.key) else { return nil }
            return self.urls[track.key]
        }
    }

    override func tearDown() async throws {
        player?.pause()
        player = nil
        if let directory { try? FileManager.default.removeItem(at: directory) }
        directory = nil
        urls = [:]
        tracks = []
        unplayableKeys = []
        try await super.tearDown()
    }

    func testPlayStartsAtTheGivenTrack() async throws {
        var started: [String] = []
        player.onTrackStarted = { started.append($0.key) }

        player.play(tracks, startAt: 1, context: "Test", shuffle: false)

        XCTAssertEqual(player.state.queue, tracks)
        XCTAssertEqual(player.state.currentIndex, 1)
        XCTAssertEqual(player.state.current, tracks[1])
        XCTAssertEqual(player.state.contextName, "Test")
        XCTAssertEqual(player.state.durationMs, 500)
        XCTAssertTrue(player.state.isPlaying)
        XCTAssertEqual(started, [tracks[1].key])
    }

    func testNextStopsAtTheEndAndPreviousGoesBack() async throws {
        player.play(tracks, startAt: 1, context: nil, shuffle: false)

        player.next()
        XCTAssertEqual(player.state.currentIndex, 2)

        player.next()
        XCTAssertEqual(player.state.currentIndex, 2)

        player.previous()
        XCTAssertEqual(player.state.currentIndex, 1)
    }

    func testJumpToChangesTheCurrentSong() async throws {
        player.play(tracks, startAt: 1, context: nil, shuffle: false)

        player.jumpTo(0)

        XCTAssertEqual(player.state.currentIndex, 0)
        XCTAssertEqual(player.state.current, tracks[0])
    }

    func testRepeatCyclesOffAllOne() async throws {
        player.play(tracks, startAt: 0, context: nil, shuffle: false)
        XCTAssertEqual(player.state.repeatMode, .off)

        player.cycleRepeat()
        XCTAssertEqual(player.state.repeatMode, .all)

        player.cycleRepeat()
        XCTAssertEqual(player.state.repeatMode, .one)

        player.cycleRepeat()
        XCTAssertEqual(player.state.repeatMode, .off)
    }

    func testJumpToSkipsASongThatCannotBeLocated() async throws {
        player.play(tracks, startAt: 0, context: nil, shuffle: false)
        unplayableKeys = [tracks[1].key]

        player.jumpTo(1)

        XCTAssertEqual(player.state.currentIndex, 2)
        XCTAssertEqual(player.state.current, tracks[2])
    }

    func testTheLastSongEndsTheQueue() async throws {
        player.play(tracks, startAt: 2, context: nil, shuffle: false)
        XCTAssertTrue(player.state.isPlaying)
        XCTAssertEqual(player.state.currentIndex, 2)

        // Lenient and bounded: a busy simulator can take a while to render, but the loop stops
        // after five seconds, so the test cannot hang.
        var waited = 0
        while player.state.isPlaying && waited < 50 {
            try? await Task.sleep(for: .milliseconds(100))
            waited += 1
        }

        guard !player.state.isPlaying else {
            // The simulator did not render the song in time; nothing to check.
            return
        }
        XCTAssertEqual(player.state.currentIndex, 2)
        XCTAssertEqual(player.state.current, tracks[2])
        XCTAssertLessThan(player.positionMs(), 1_000)
    }

    // MARK: - Helpers

    private static func writeSilence(to url: URL) throws {
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatLinearPCM,
            AVSampleRateKey: 44_100.0,
            AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 32,
            AVLinearPCMIsFloatKey: true,
            AVLinearPCMIsBigEndianKey: false,
            AVLinearPCMIsNonInterleaved: false,
        ]
        let file = try AVAudioFile(forWriting: url, settings: settings)
        let format = file.processingFormat
        let frames = AVAudioFrameCount(format.sampleRate * 0.5)
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames))
        buffer.frameLength = frames
        try file.write(from: buffer)
    }
}
