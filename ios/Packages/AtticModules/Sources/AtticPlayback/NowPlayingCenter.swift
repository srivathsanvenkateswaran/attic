import AtticCore
import Foundation
import MediaPlayer
import Observation
import UIKit

/// Mirrors the player on the lock screen and in Control Centre, routing their buttons back.
@MainActor
public final class NowPlayingCenter: NowPlayingPublishing {
    private weak var player: PlaybackControlling?
    private var artworkProvider: ArtworkProviding?
    private var artworkTask: Task<Void, Never>?

    private var lastSongKey: String?
    private var artwork: MPMediaItemArtwork?

    private struct Visible: Equatable {
        let key: String?
        let isPlaying: Bool
        let durationMs: Int64
        let queueIndex: Int
    }
    private var lastVisible: Visible?

    private var tokens: [(MPRemoteCommand, Any)] = []

    public init() {}

    public func attach(to player: PlaybackControlling, artwork: ArtworkProviding) {
        // Drop handlers registered for a previous player.
        for (command, token) in tokens {
            command.removeTarget(token)
        }
        tokens = []

        self.player = player
        self.artworkProvider = artwork
        self.lastSongKey = nil
        self.artwork = nil
        self.lastVisible = nil
        artworkTask?.cancel()
        artworkTask = nil

        let center = MPRemoteCommandCenter.shared()
        register(center.playCommand) { [weak self] in
            self?.handle(.play)
        }
        register(center.pauseCommand) { [weak self] in
            self?.handle(.pause)
        }
        register(center.togglePlayPauseCommand) { [weak self] in
            self?.handle(.toggle)
        }
        register(center.nextTrackCommand) { [weak self] in
            self?.handle(.next)
        }
        register(center.previousTrackCommand) { [weak self] in
            self?.handle(.previous)
        }
        registerPosition(center.changePlaybackPositionCommand)

        for command in [
            center.skipForwardCommand, center.skipBackwardCommand, center.seekForwardCommand,
            center.seekBackwardCommand, center.likeCommand, center.dislikeCommand,
            center.bookmarkCommand, center.changeRepeatModeCommand, center.changeShuffleModeCommand,
        ] {
            command.isEnabled = false
        }

        observe()
        update()
    }

    // MARK: - Remote commands

    private func register(_ command: MPRemoteCommand, action: @escaping () -> Void) {
        let token = command.addTarget { [weak self] _ in
            let status = MainActor.assumeIsolated { () -> MPRemoteCommandHandlerStatus in
                guard let self, self.player?.state.current != nil else {
                    return .noActionableNowPlayingItem
                }
                action()
                return .success
            }
            return status
        }
        tokens.append((command, token))
    }

    private func registerPosition(_ command: MPRemoteCommand) {
        let token = command.addTarget { [weak self] event in
            let status = MainActor.assumeIsolated { () -> MPRemoteCommandHandlerStatus in
                guard let self, let player = self.player, player.state.current != nil else {
                    return .noActionableNowPlayingItem
                }
                if let e = event as? MPChangePlaybackPositionCommandEvent {
                    self.handle(.seek(ms: Int64(e.positionTime * 1000)))
                }
                return .success
            }
            return status
        }
        tokens.append((command, token))
    }

    // MARK: - Observation

    private func observe() {
        guard let player else { return }
        withObservationTracking {
            _ = player.state
        } onChange: {
            Task { @MainActor [weak self] in
                self?.update(force: false)
                self?.observe()
            }
        }
    }

    // MARK: - Update

    /// Forced, because callers use it after a seek, which leaves the visible state unchanged.
    public func update() {
        update(force: true)
    }

    private func update(force: Bool) {
        guard let player else { return }
        let state = player.state

        guard let track = state.current else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            MPNowPlayingInfoCenter.default().playbackState = .stopped
            lastSongKey = nil
            artwork = nil
            artworkTask?.cancel()
            artworkTask = nil
            lastVisible = nil
            return
        }

        let visible = Visible(
            key: track.key, isPlaying: state.isPlaying, durationMs: state.durationMs,
            queueIndex: state.currentIndex
        )
        if !force && lastVisible == visible {
            return
        }
        lastVisible = visible

        // Artwork: reload only when the song changes.
        if lastSongKey != track.key {
            lastSongKey = track.key
            artwork = nil
            artworkTask?.cancel()
            let currentKey = track.key
            let provider = artworkProvider
            let trackCopy = track
            artworkTask = Task { [weak self] in
                guard let provider else { return }
                let image = await provider.image(for: trackCopy, maxPixel: 600)
                await MainActor.run {
                    guard let self else { return }
                    guard self.player?.state.current?.key == currentKey else { return }
                    if let image {
                        self.artwork = Self.makeArtwork(image)
                    }
                    self.update(force: true)
                }
            }
        }

        var info: [String: Any] = [:]
        info[MPMediaItemPropertyTitle] = track.title
        info[MPMediaItemPropertyArtist] = track.artist
        if let album = track.album {
            info[MPMediaItemPropertyAlbumTitle] = album
        }
        info[MPMediaItemPropertyPlaybackDuration] = Double(state.durationMs) / 1000.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = Double(player.positionMs()) / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = state.isPlaying ? 1.0 : 0.0
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0
        info[MPNowPlayingInfoPropertyMediaType] = MPNowPlayingInfoMediaType.audio.rawValue
        info[MPNowPlayingInfoPropertyPlaybackQueueIndex] = state.currentIndex
        info[MPNowPlayingInfoPropertyPlaybackQueueCount] = state.queue.count
        if let artwork {
            info[MPMediaItemPropertyArtwork] = artwork
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
        MPNowPlayingInfoCenter.default().playbackState = state.isPlaying ? .playing : .paused
    }

    /// Nonisolated, so the request handler MediaPlayer calls off the main thread isn't main-actor isolated.
    private nonisolated static func makeArtwork(_ image: UIImage) -> MPMediaItemArtwork {
        MPMediaItemArtwork(boundsSize: image.size) { _ in image }
    }

    // MARK: - Commands (testable)

    public enum Command: Sendable {
        case play, pause, toggle, next, previous, seek(ms: Int64)
    }

    internal func handle(_ command: Command) {
        guard let player, player.state.current != nil else { return }
        switch command {
        case .play: player.resume()
        case .pause: player.pause()
        case .toggle: player.togglePlayPause()
        case .next: player.next()
        case .previous: player.previous()
        case .seek(let ms):
            player.seek(toMs: ms)
            update(force: true)
        }
    }
}