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
///
/// STUB: implemented by the "queue order" task. Keep every signature.
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
        original = tracks
        order = Array(tracks.indices)
        position = tracks.isEmpty ? 0 : min(max(startAt, 0), tracks.count - 1)
        isShuffled = false
        if shuffled { setShuffle(true) }
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
        isShuffled = on
    }

    /// Where playback goes after the current song ends on its own. Nil means stop.
    /// `.one` repeats the current position; `.all` wraps to 0 after the last.
    public func positionAfterEnd(repeat mode: RepeatMode) -> Int? {
        position + 1 < order.count ? position + 1 : nil
    }

    /// Where the next button goes. Like `.all` for wrapping, but `.one` still advances.
    public func positionForNext(repeat mode: RepeatMode) -> Int? {
        position + 1 < order.count ? position + 1 : nil
    }

    /// Where the previous button goes (the caller restarts the song instead when it is past 3 s).
    public func positionForPrevious(repeat mode: RepeatMode) -> Int? {
        position > 0 ? position - 1 : nil
    }

    /// Makes the song at play-order `position` current.
    public mutating func jump(to position: Int) {
        guard order.indices.contains(position) else { return }
        self.position = position
    }

    /// Moves the song at play-order index `from` to play-order index `to`. The current song stays current.
    public mutating func move(from: Int, to: Int) {}

    /// Removes the song at play-order index. Removing the current song makes the next one current
    /// (or the previous one if it was last).
    public mutating func remove(at position: Int) {}

    /// Inserts songs right after the current one.
    public mutating func playNext(_ tracks: [Track]) {}

    /// Adds songs at the end of the play order.
    public mutating func append(_ tracks: [Track]) {}

    /// Keeps the current song and everything before it; drops everything after it.
    public mutating func clearUpNext() {}
}
