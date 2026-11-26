import AtticCore
import Foundation

/// STUB: implemented by the "LRCLIB and LRC" task.
public struct LRCLibClient: LyricsFetching {
    public init(session: URLSession = .shared) {}

    public func fetch(title: String, artist: String, album: String?, durationSec: Int) async throws -> LyricsDocument {
        .notFound
    }
}
