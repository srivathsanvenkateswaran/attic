import Foundation

public enum RepeatMode: Int, CaseIterable, Codable, Sendable {
    case off, all, one

    /// Off, then all, then one, then off.
    public var next: RepeatMode {
        switch self {
        case .off: .all
        case .all: .one
        case .one: .off
        }
    }
}

/// The play queue as pure data: the songs in the order they were given, the order they play in
/// (shuffled or not) and where playback is. No AVFoundation here, so it is unit-tested on its own.
public struct PlayQueue: Equatable, Sendable {
    /// The songs in the order they were given (the collection's order).
    public private(set) var original: [Track]
    /// The play order, as indices into [original]. Identity when not shuffled.
    public private(set) var order: [Int]
    /// The position in [order] of the current song. 0 when the queue is empty.
    public private(set) var position: Int
    public private(set) var isShuffled: Bool

    public static let empty = PlayQueue(tracks: [], startAt: 0, shuffled: false)

    /// `startAt` is an index into `tracks`. With `shuffled`, that song plays first and the rest are shuffled.
    public init(tracks: [Track], startAt: Int, shuffled: Bool) {
        var generator = SystemRandomNumberGenerator()
        self.init(tracks: tracks, startAt: startAt, shuffled: shuffled, using: &generator)
    }

    /// Same as above, with a supplied generator so tests can fix the shuffle.
    public init(tracks: [Track], startAt: Int, shuffled: Bool, using generator: inout some RandomNumberGenerator) {
        original = tracks
        order = Array(tracks.indices)
        position = tracks.isEmpty ? 0 : min(max(startAt, 0), tracks.count - 1)
        isShuffled = false
        if shuffled { setShuffle(true, using: &generator) }
    }

    /// Every song in play order.
    public var tracks: [Track] { order.map { original[$0] } }
    public var current: Track? { order.indices.contains(position) ? original[order[position]] : nil }
    public var isEmpty: Bool { original.isEmpty }
    /// The songs after the current one, in play order.
    public var upNext: [Track] { Array(tracks.dropFirst(position + 1)) }

    /// On: the current song stays current and first; everything else is shuffled after it.
    /// Off: the original order comes back with the current song still current.
    public mutating func setShuffle(_ on: Bool) {
        var generator = SystemRandomNumberGenerator()
        setShuffle(on, using: &generator)
    }

    /// Same as above, with a supplied generator so tests can fix the shuffle.
    public mutating func setShuffle(_ on: Bool, using generator: inout some RandomNumberGenerator) {
        guard !original.isEmpty else {
            isShuffled = on
            return
        }
        let current = order[position]
        if on {
            var rest = order.filter { $0 != current }
            rest.shuffle(using: &generator)
            order = [current] + rest
            position = 0
        } else {
            order = Array(original.indices)
            position = current
        }
        isShuffled = on
    }

    /// Where playback goes after the current song ends on its own. Nil means stop.
    /// `.one` repeats the current position; `.all` wraps to 0 after the last.
    public func positionAfterEnd(repeat mode: RepeatMode) -> Int? {
        guard !order.isEmpty else { return nil }
        switch mode {
        case .one: return position
        case .all: return (position + 1) % order.count
        case .off: return position + 1 < order.count ? position + 1 : nil
        }
    }

    /// Where the next button goes. Like `.all` for wrapping, but `.one` still advances.
    public func positionForNext(repeat mode: RepeatMode) -> Int? {
        guard !order.isEmpty else { return nil }
        switch mode {
        case .off: return position + 1 < order.count ? position + 1 : nil
        case .all, .one: return (position + 1) % order.count
        }
    }

    /// Where the previous button goes (the caller restarts the song instead when it is past 3 s).
    public func positionForPrevious(repeat mode: RepeatMode) -> Int? {
        guard !order.isEmpty else { return nil }
        if position > 0 { return position - 1 }
        return mode == .all ? order.count - 1 : nil
    }

    /// Makes the song at play-order `position` current.
    public mutating func jump(to position: Int) {
        guard order.indices.contains(position) else { return }
        self.position = position
    }

    /// Moves the song at play-order index `from` to play-order index `to`. The current song stays current.
    public mutating func move(from: Int, to: Int) {
        guard order.indices.contains(from), order.indices.contains(to), from != to else { return }
        let current = order[position]
        order.insert(order.remove(at: from), at: to)
        position = order.firstIndex(of: current) ?? position
        if !isShuffled {
            // Keep the collection order in step so turning shuffle off preserves the move.
            original.insert(original.remove(at: from), at: to)
            order = Array(original.indices)
        }
    }

    /// Removes the song at play-order index. Removing the current song makes the next one current
    /// (or the previous one if it was last).
    public mutating func remove(at position: Int) {
        guard order.indices.contains(position) else { return }
        let removed = order[position]
        original.remove(at: removed)
        order.remove(at: position)
        order = order.map { $0 > removed ? $0 - 1 : $0 }
        if order.isEmpty {
            self.position = 0
        } else if position < self.position {
            self.position -= 1
        } else if position == self.position {
            self.position = min(self.position, order.count - 1)
        }
    }

    /// Inserts songs right after the current one.
    public mutating func playNext(_ tracks: [Track]) {
        guard !tracks.isEmpty else { return }
        if !isShuffled || original.isEmpty {
            if original.isEmpty {
                original = tracks
                order = Array(original.indices)
                position = 0
            } else {
                let insertAt = order[position] + 1
                original.insert(contentsOf: tracks, at: insertAt)
                order = Array(original.indices)
            }
        } else {
            let first = original.count
            let indices = Array(first..<(first + tracks.count))
            original.append(contentsOf: tracks)
            order.insert(contentsOf: indices, at: position + 1)
        }
    }

    /// Adds songs at the end of the play order.
    public mutating func append(_ tracks: [Track]) {
        guard !tracks.isEmpty else { return }
        let first = original.count
        let indices = Array(first..<(first + tracks.count))
        original.append(contentsOf: tracks)
        order.append(contentsOf: indices)
    }

    /// Keeps the current song and everything before it; drops everything after it.
    public mutating func clearUpNext() {
        guard position + 1 < order.count else { return }
        let dropped = Set(order[(position + 1)...])
        var kept: [Track] = []
        var newIndex: [Int: Int] = [:]
        for (index, track) in original.enumerated() where !dropped.contains(index) {
            newIndex[index] = kept.count
            kept.append(track)
        }
        original = kept
        order = order[...position].map { newIndex[$0]! }
    }
}
