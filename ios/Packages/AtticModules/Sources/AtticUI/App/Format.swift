import AtticCore
import Foundation

/// Counts and lengths in the forms the strings expect.
enum Format {
    /// "1 song", "1,204 songs".
    static func songs(_ n: Int) -> String { S.songCount(count: n, S.grouped(n)) }

    /// A total length: "1 hr 2 min", "18 min".
    static func total(ms: Int64) -> String {
        let minutes = Int((ms + 30_000) / 60_000)
        return minutes >= 60 ? S.durationHoursMinutes(minutes / 60, minutes % 60) : S.durationMinutes(minutes)
    }

    /// "8 songs, 30 min".
    static func songsAndLength(_ tracks: [Track]) -> String {
        S.songsAndDuration(songs(tracks.count), total(ms: tracks.reduce(0) { $0 + $1.durationMs }))
    }

    /// A clock position: "3:05", "1:02:09".
    static func clock(ms: Int64) -> String {
        let seconds = max(0, Int(ms / 1000))
        let h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%d:%02d", m, s)
    }
}
