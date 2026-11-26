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
public enum LRCParser {
    private static let stamp = try! NSRegularExpression(pattern: #"\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]"#)
    private static let wordStamp = try! NSRegularExpression(pattern: #"<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>"#)
    private static let offsetTag = try! NSRegularExpression(
        pattern: #"\[offset:\s*([+-]?\d+)\s*\]"#, options: .caseInsensitive
    )

    /// Every timed line, sorted by time. Handles several stamps on one line ("[00:12.00][01:30.50]words"),
    /// two- or three-digit fractions, "[mm:ss]" without a fraction, an "[offset:+/-ms]" tag (applied
    /// to every line; positive offset makes lines earlier), and ignores other tags ([ar:], [ti:], [length:]...).
    /// Blank-text lines are kept (they mark instrumental gaps) with text "".
    public static func parse(_ text: String) -> [LyricLine] {
        var offsetMs: Int64 = 0
        let whole = NSRange(location: 0, length: (text as NSString).length)
        if let match = offsetTag.firstMatch(in: text, range: whole),
           let group = Range(match.range(at: 1), in: text),
           let value = Int64(text[group].trimmingCharacters(in: .whitespaces)) {
            offsetMs = value
        }

        var parsed: [LyricLine] = []
        for rawLine in text.split(whereSeparator: \.isNewline) {
            let line = String(rawLine)
            let ns = line as NSString
            var stamps: [(timeMs: Int64, end: Int)] = []
            var at = 0

            // Only stamps at the start of a line time it; stamps inside the words do not.
            while at < ns.length {
                while at < ns.length && ns.character(at: at) == 0x20 { at += 1 }
                let rest = NSRange(location: at, length: ns.length - at)
                guard let match = stamp.firstMatch(in: line, range: rest), match.range.location == at else { break }
                let minutes = Int(ns.substring(with: match.range(at: 1))) ?? 0
                let seconds = Int(ns.substring(with: match.range(at: 2))) ?? 0
                var fractionMs: Int64 = 0
                if match.range(at: 3).location != NSNotFound {
                    let fraction = ns.substring(with: match.range(at: 3))
                    fractionMs = Int64((fraction + "000").prefix(3)) ?? 0
                }
                let timeMs = Int64(minutes) * 60_000 + Int64(seconds) * 1_000 + fractionMs - offsetMs
                let end = match.range.location + match.range.length
                stamps.append((max(0, timeMs), end))
                at = end
            }
            guard let last = stamps.last else { continue }

            let words = ns.substring(from: last.end)
            let wordRange = NSRange(location: 0, length: (words as NSString).length)
            let withoutWordStamps = wordStamp.stringByReplacingMatches(in: words, range: wordRange, withTemplate: "")
            let lyric = withoutWordStamps.trimmingCharacters(in: .whitespacesAndNewlines)
            for item in stamps {
                parsed.append(LyricLine(timeMs: item.timeMs, text: lyric))
            }
        }

        // A plain sort is not stable; the index keeps stamps at the same time in source order.
        return parsed.enumerated()
            .sorted { a, b in
                a.element.timeMs == b.element.timeMs ? a.offset < b.offset : a.element.timeMs < b.element.timeMs
            }
            .map(\.element)
    }

    /// The index of the line being sung at `positionMs`: the last line whose time is <= position.
    /// Nil before the first line or for an empty list.
    public static func activeIndex(in lines: [LyricLine], at positionMs: Int64) -> Int? {
        var low = 0
        var high = lines.count - 1
        var found: Int?
        while low <= high {
            let mid = low + (high - low) / 2
            if lines[mid].timeMs <= positionMs {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }
}
