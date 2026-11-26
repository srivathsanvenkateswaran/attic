import Foundation

public struct LyricLine: Hashable, Codable, Sendable {
    public var timeMs: Int64
    public var text: String

    public init(timeMs: Int64, text: String) {
        self.timeMs = timeMs
        self.text = text
    }
}

/// A song's lyrics as saved and shown.
public enum LyricsDocument: Hashable, Codable, Sendable {
    /// Time-stamped lines, sorted by time.
    case synced([LyricLine])
    /// Lines without times.
    case plain([String])
    /// LRCLIB says the song has no words.
    case instrumental
    /// Looked up and not found. Saved so the lookup is not repeated on every play.
    case notFound
}

/// Parses LRC text ("[mm:ss.xx] words").
/// STUB: implemented by the "LRCLIB and LRC" task. Keep these signatures.
public enum LRCParser {
    /// Every timed line, sorted by time. Handles several stamps on one line ("[00:12.00][01:30.50]words"),
    /// two- or three-digit fractions, "[mm:ss]" without a fraction, an "[offset:+/-ms]" tag (applied
    /// to every line; positive offset makes lines earlier), and ignores other tags ([ar:], [ti:], [length:]...).
    /// Blank-text lines are kept (they mark instrumental gaps) with text "".
    public static func parse(_ text: String) -> [LyricLine] { [] }

    /// The index of the line being sung at `positionMs`: the last line whose time is <= position.
    /// Nil before the first line or for an empty list.
    public static func activeIndex(in lines: [LyricLine], at positionMs: Int64) -> Int? { nil }
}
