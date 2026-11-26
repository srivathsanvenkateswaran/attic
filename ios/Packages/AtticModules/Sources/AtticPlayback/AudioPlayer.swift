import AtticCore
import AVFoundation
import Foundation
import Observation

/// The audio engine: AVQueuePlayer, the audio session, interruptions and route changes.
/// STUB: implemented by the "playback engine" task. Keep the public API.
@MainActor
@Observable
public final class AudioPlayer: PlaybackControlling {
    public private(set) var state = PlayerState()
    @ObservationIgnored public var onTrackStarted: ((Track) -> Void)?
    @ObservationIgnored private let locate: @MainActor (Track) -> URL?
    @ObservationIgnored private var queue = PlayQueue.empty

    /// `locate` gives a song's file URL with its folder's scope open (FolderAccessing.fileURL).
    public init(locate: @escaping @MainActor (Track) -> URL?) {
        self.locate = locate
    }

    public func positionMs() -> Int64 { 0 }

    public func play(_ tracks: [Track], startAt: Int, context: String?, shuffle: Bool?) {
        queue = PlayQueue(tracks: tracks, startAt: startAt, shuffled: shuffle ?? state.shuffle)
        state.queue = queue.tracks
        state.currentIndex = queue.position
        state.contextName = context
        state.shuffle = queue.isShuffled
        state.isPlaying = true
        if let current = queue.current { onTrackStarted?(current) }
    }

    public func togglePlayPause() { state.isPlaying.toggle() }
    public func pause() { state.isPlaying = false }
    public func resume() { state.isPlaying = !state.queue.isEmpty }
    public func next() {}
    public func previous() {}
    public func seek(toMs ms: Int64) {}
    public func toggleShuffle() {}
    public func cycleRepeat() { state.repeatMode = state.repeatMode.next }
    public func jumpTo(_ queueIndex: Int) {}
    public func remove(_ queueIndex: Int) {}
    public func move(from: Int, to: Int) {}
    public func clearUpNext() {}
    public func playNext(_ tracks: [Track]) {}
    public func addToQueue(_ tracks: [Track]) {}
}
