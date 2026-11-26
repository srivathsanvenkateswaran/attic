import AtticCore
import AVFoundation
import Foundation
import Observation

/// The audio engine: AVQueuePlayer, the audio session, interruptions and route changes.
///
/// `PlayQueue` is the source of truth for order and position; the player only holds the current
/// song and the one after it, so songs run into each other without a gap.
@MainActor
@Observable
public final class AudioPlayer: PlaybackControlling {
    public private(set) var state = PlayerState()
    @ObservationIgnored public var onTrackStarted: ((Track) -> Void)?

    @ObservationIgnored private let locate: @MainActor (Track) -> URL?
    @ObservationIgnored private var queue = PlayQueue.empty
    @ObservationIgnored private let player = AVQueuePlayer()
    /// Which queue position each player item was made for.
    @ObservationIgnored private var itemPositions: [ObjectIdentifier: Int] = [:]
    /// The preloaded item that plays after the current one.
    @ObservationIgnored private var follower: ObjectIdentifier?
    @ObservationIgnored private var kvoTokens: [NSKeyValueObservation] = []
    /// Notification observers, removed when the bag goes away.
    @ObservationIgnored private let notifications = NotificationBag()
    @ObservationIgnored private var sessionActive = false
    /// True while `load` rearranges the player, so the observations it causes are ignored.
    @ObservationIgnored private var isLoading = false

    /// `locate` gives a song's file URL with its folder's scope open (FolderAccessing.fileURL).
    public init(locate: @escaping @MainActor (Track) -> URL?) {
        self.locate = locate
        player.actionAtItemEnd = .advance
        observePlayer()
        observeSession()
    }

    // MARK: - State

    public func positionMs() -> Int64 {
        let seconds = player.currentTime().seconds
        guard seconds.isFinite else { return 0 }
        return Int64(seconds * 1000)
    }

    private func syncState() {
        var next = state
        next.queue = queue.tracks
        next.currentIndex = queue.position
        next.shuffle = queue.isShuffled
        next.durationMs = queue.current?.durationMs ?? 0
        if next != state { state = next }
    }

    private func setPlaying(_ playing: Bool) {
        if state.isPlaying != playing { state.isPlaying = playing }
    }

    // MARK: - Loading

    /// Makes `position` current and fills the player with it and the song after it.
    private func load(position: Int, play shouldPlay: Bool) {
        isLoading = true
        defer { isLoading = false }

        queue.jump(to: position)
        player.removeAllItems()
        itemPositions.removeAll()
        follower = nil

        let tracks = queue.tracks
        var index = queue.position
        var current: AVPlayerItem?
        while tracks.indices.contains(index) {
            if let url = locate(tracks[index]) {
                current = AVPlayerItem(url: url)
                break
            }
            index += 1
        }

        guard let item = current else {
            // Nothing in this queue can be played; stop rather than sit on a dead item.
            setPlaying(false)
            syncState()
            return
        }

        if index != queue.position { queue.jump(to: index) }
        itemPositions[ObjectIdentifier(item)] = index
        player.insert(item, after: nil)
        insertFollower(after: item)

        if shouldPlay {
            activateSession()
            player.play()
        }
        setPlaying(shouldPlay)
        syncState()
        if let track = queue.current { onTrackStarted?(track) }
    }

    /// Preloads the song that plays after `item`, if there is one.
    private func insertFollower(after item: AVPlayerItem) {
        guard let position = queue.positionAfterEnd(repeat: state.repeatMode) else { return }
        let tracks = queue.tracks
        guard tracks.indices.contains(position), let url = locate(tracks[position]) else { return }
        let next = AVPlayerItem(url: url)
        itemPositions[ObjectIdentifier(next)] = position
        follower = ObjectIdentifier(next)
        player.insert(next, after: item)
    }

    /// Drops whatever was preloaded and preloads again after the current song.
    private func rebuildFollower() {
        guard let current = player.currentItem else { return }
        itemPositions[ObjectIdentifier(current)] = queue.position
        let items = player.items()
        if let index = items.firstIndex(of: current) {
            for item in items.dropFirst(index + 1) {
                player.remove(item)
                itemPositions.removeValue(forKey: ObjectIdentifier(item))
            }
        }
        follower = nil
        insertFollower(after: current)
    }

    private func clearPlayer() {
        player.removeAllItems()
        itemPositions.removeAll()
        follower = nil
        setPlaying(false)
    }

    // MARK: - Transport

    public func play(_ tracks: [Track], startAt: Int, context: String?, shuffle: Bool?) {
        guard !tracks.isEmpty else {
            queue = .empty
            clearPlayer()
            state.contextName = context
            syncState()
            return
        }
        queue = PlayQueue(tracks: tracks, startAt: startAt, shuffled: shuffle ?? state.shuffle)
        state.contextName = context
        load(position: queue.position, play: true)
    }

    public func togglePlayPause() {
        if state.isPlaying { pause() } else { resume() }
    }

    public func pause() {
        player.pause()
        setPlaying(false)
    }

    public func resume() {
        activateSession()
        if player.currentItem == nil {
            guard !queue.isEmpty else { return }
            load(position: queue.position, play: true)
        } else {
            player.play()
            setPlaying(true)
        }
    }

    public func next() {
        guard let position = queue.positionForNext(repeat: state.repeatMode) else { return }
        load(position: position, play: true)
    }

    public func previous() {
        if positionMs() > 3000 {
            seek(toMs: 0)
            return
        }
        guard let position = queue.positionForPrevious(repeat: state.repeatMode) else {
            seek(toMs: 0)
            return
        }
        load(position: position, play: true)
    }

    public func seek(toMs ms: Int64) {
        player.seek(
            to: CMTime(value: max(ms, 0), timescale: 1000),
            toleranceBefore: .zero,
            toleranceAfter: .zero
        )
    }

    public func toggleShuffle() {
        queue.setShuffle(!queue.isShuffled)
        syncState()
        rebuildFollower()
    }

    public func cycleRepeat() {
        state.repeatMode = state.repeatMode.next
        rebuildFollower()
    }

    public func jumpTo(_ queueIndex: Int) {
        guard queue.tracks.indices.contains(queueIndex) else { return }
        load(position: queueIndex, play: true)
    }

    public func remove(_ queueIndex: Int) {
        guard queue.tracks.indices.contains(queueIndex) else { return }
        let wasCurrent = queueIndex == queue.position
        let wasPlaying = state.isPlaying
        queue.remove(at: queueIndex)
        if queue.isEmpty {
            clearPlayer()
            syncState()
        } else if wasCurrent {
            load(position: queue.position, play: wasPlaying)
        } else {
            syncState()
            rebuildFollower()
        }
    }

    public func move(from: Int, to: Int) {
        guard queue.tracks.indices.contains(from), queue.tracks.indices.contains(to) else { return }
        queue.move(from: from, to: to)
        syncState()
        rebuildFollower()
    }

    public func clearUpNext() {
        queue.clearUpNext()
        syncState()
        rebuildFollower()
    }

    public func playNext(_ tracks: [Track]) {
        guard !tracks.isEmpty else { return }
        guard !queue.isEmpty else {
            play(tracks, startAt: 0, context: state.contextName, shuffle: state.shuffle)
            return
        }
        queue.playNext(tracks)
        syncState()
        rebuildFollower()
    }

    public func addToQueue(_ tracks: [Track]) {
        guard !tracks.isEmpty else { return }
        guard !queue.isEmpty else {
            play(tracks, startAt: 0, context: state.contextName, shuffle: state.shuffle)
            return
        }
        queue.append(tracks)
        syncState()
        rebuildFollower()
    }

    // MARK: - Audio session

    private func activateSession() {
        guard !sessionActive else { return }
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default, policy: .longFormAudio)
            try session.setActive(true)
            sessionActive = true
        } catch {
            // Playback still works without an active session; try again on the next play.
        }
    }

    // MARK: - Observation

    private func observePlayer() {
        kvoTokens.append(player.observe(\.currentItem, options: [.new]) { [weak self] _, _ in
            Task { @MainActor in self?.currentItemDidChange() }
        })
        kvoTokens.append(player.observe(\.timeControlStatus, options: [.new]) { [weak self] _, _ in
            Task { @MainActor in self?.timeControlDidChange() }
        })
    }

    /// The player moved on by itself, to the preloaded song or off the end of the queue.
    private func currentItemDidChange() {
        guard let item = player.currentItem else {
            setPlaying(false)
            // With repeat off the last song stays ready at its start.
            if !queue.isEmpty { load(position: queue.position, play: false) }
            return
        }
        let id = ObjectIdentifier(item)
        guard id == follower, let position = itemPositions[id] else { return }
        follower = nil
        queue.jump(to: position)
        syncState()
        if let track = queue.current { onTrackStarted?(track) }
        insertFollower(after: item)
    }

    private func timeControlDidChange() {
        guard !isLoading else { return }
        switch player.timeControlStatus {
        case .playing, .waitingToPlayAtSpecifiedRate:
            setPlaying(true)
        case .paused:
            setPlaying(false)
        @unknown default:
            break
        }
    }

    private func observeSession() {
        let center = NotificationCenter.default
        let session = AVAudioSession.sharedInstance()
        notifications.tokens.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification, object: session, queue: .main
            ) { [weak self] note in
                MainActor.assumeIsolated { self?.handleInterruption(note) }
            }
        )
        notifications.tokens.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification, object: session, queue: .main
            ) { [weak self] note in
                MainActor.assumeIsolated { self?.handleRouteChange(note) }
            }
        )
        notifications.tokens.append(
            center.addObserver(
                forName: AVPlayerItem.failedToPlayToEndTimeNotification, object: nil, queue: .main
            ) { [weak self] note in
                MainActor.assumeIsolated { self?.handleItemFailure(note) }
            }
        )
    }

    private func handleInterruption(_ note: Notification) {
        guard let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: raw) else { return }
        switch type {
        case .began:
            setPlaying(false)
        case .ended:
            let optionsRaw = note.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            let options = AVAudioSession.InterruptionOptions(rawValue: optionsRaw)
            guard options.contains(.shouldResume) else { return }
            activateSession()
            player.play()
            setPlaying(true)
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ note: Notification) {
        guard let raw = note.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
              AVAudioSession.RouteChangeReason(rawValue: raw) == .oldDeviceUnavailable else { return }
        pause()
    }

    private func handleItemFailure(_ note: Notification) {
        guard let item = note.object as? AVPlayerItem, item === player.currentItem else { return }
        next()
    }
}

/// Removes the notification observers from `deinit`, which cannot touch main-actor state.
private final class NotificationBag {
    var tokens: [NSObjectProtocol] = []

    deinit {
        for token in tokens { NotificationCenter.default.removeObserver(token) }
    }
}
