import AtticCore
@testable import AtticPlayback
import MediaPlayer
import UIKit
import XCTest

@MainActor
final class NowPlayingCenterTests: XCTestCase {
    /// The first call into the system's now-playing service can stall for over a minute on a busy
    /// simulator. Pay for it here, outside every test's timeout.
    override class func setUp() {
        super.setUp()
        MainActor.assumeIsolated {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        }
    }

    private func makeTrack(title: String, artist: String, key: String? = nil) -> Track {
        Track(
            key: key ?? title, rootID: UUID(), relativePath: "r/", fileName: "f.mp3",
            title: title, artist: artist, artistKeys: [], album: "Album", albumKey: nil,
            albumArtist: nil, folder: nil, trackNumber: nil, discNumber: nil, durationMs: 1000,
            year: nil, dateAdded: Date(), genre: nil, composer: nil, hasArtwork: true, modified: Date()
        )
    }

    private func makePlayer(track: Track) -> FakePlayer {
        let p = FakePlayer()
        p.state = PlayerState(queue: [track], currentIndex: 0, isPlaying: true, durationMs: 1000)
        return p
    }

    func testAttachShowsTitle() {
        let center = NowPlayingCenter()
        let track = makeTrack(title: "Song", artist: "Band")
        let player = makePlayer(track: track)
        center.attach(to: player, artwork: FakeArtwork())
        XCTAssertEqual(
            MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyTitle] as? String,
            "Song"
        )
    }

    func testStateChangeUpdatesTitle() async {
        let center = NowPlayingCenter()
        let track = makeTrack(title: "Song", artist: "Band")
        let player = makePlayer(track: track)
        center.attach(to: player, artwork: FakeArtwork())

        let newTrack = makeTrack(title: "Song2", artist: "Band2", key: "k2")
        player.state = PlayerState(queue: [newTrack], currentIndex: 0, isPlaying: true, durationMs: 2000)

        let exp = expectation(description: "title updated")
        Task { @MainActor in
            for _ in 0..<600 {
                if MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyTitle] as? String == "Song2" {
                    exp.fulfill()
                    return
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
        await fulfillment(of: [exp], timeout: 30.0)
        // The center and the player are held weakly by their own callbacks; keep both alive until here.
        withExtendedLifetime((center, player)) {}
    }

    func testClearQueueSetsNil() {
        let center = NowPlayingCenter()
        let track = makeTrack(title: "Song", artist: "Band")
        let player = makePlayer(track: track)
        center.attach(to: player, artwork: FakeArtwork())
        player.state = PlayerState()
        center.update()
        XCTAssertNil(MPNowPlayingInfoCenter.default().nowPlayingInfo)
    }

    func testArtworkAppears() async {
        let center = NowPlayingCenter()
        let track = makeTrack(title: "Song", artist: "Band")
        let player = makePlayer(track: track)
        center.attach(to: player, artwork: FakeArtwork())

        let exp = expectation(description: "artwork")
        Task { @MainActor in
            for _ in 0..<600 {
                if center.artwork != nil {
                    exp.fulfill()
                    return
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
        await fulfillment(of: [exp], timeout: 30.0)
        // The center and the player are held weakly by their own callbacks; keep both alive until here.
        withExtendedLifetime((center, player)) {}
    }

    func testHandleRecordsCalls() {
        let center = NowPlayingCenter()
        let track = makeTrack(title: "Song", artist: "Band")
        let player = makePlayer(track: track)
        center.attach(to: player, artwork: FakeArtwork())
        player.recorded.removeAll()

        center.handle(.play)
        center.handle(.pause)
        center.handle(.toggle)
        center.handle(.next)
        center.handle(.previous)
        center.handle(.seek(ms: 1234))

        XCTAssertEqual(player.recorded, ["resume", "pause", "toggle", "next", "previous", "seek:1234"])
    }
}

@Observable
@MainActor
final class FakePlayer: PlaybackControlling {
    var state: PlayerState = PlayerState()
    var recorded: [String] = []
    var position: Int64 = 0

    func positionMs() -> Int64 { position }

    func play(_ tracks: [Track], startAt: Int, context: String?, shuffle: Bool?) {}
    func togglePlayPause() { recorded.append("toggle") }
    func pause() { recorded.append("pause") }
    func resume() { recorded.append("resume") }
    func next() { recorded.append("next") }
    func previous() { recorded.append("previous") }
    func seek(toMs ms: Int64) { recorded.append("seek:\(ms)") }
    func toggleShuffle() {}
    func cycleRepeat() {}
    func jumpTo(_ queueIndex: Int) {}
    func remove(_ queueIndex: Int) {}
    func move(from: Int, to: Int) {}
    func clearUpNext() {}
    func playNext(_ tracks: [Track]) {}
    func addToQueue(_ tracks: [Track]) {}

    var onTrackStarted: ((Track) -> Void)?
}

struct FakeArtwork: ArtworkProviding {
    func image(for track: Track, maxPixel: Int) async -> UIImage? {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 2, height: 2))
        return renderer.image { _ in }
    }
}
