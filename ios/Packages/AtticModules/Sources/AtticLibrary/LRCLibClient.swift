import AtticCore
import Foundation

/// Looks up lyrics on LRCLIB. The only network call the app makes.
public struct LRCLibClient: LyricsFetching {
    private static let base = "https://lrclib.net/api"
    private static let userAgent = "Attic-iOS/1.0 (https://github.com/srivathsanvenkateswaran/attic)"

    private enum FetchError: Error {
        case badURL
        case badStatus(Int)
        case noData
    }

    private struct Record: Decodable {
        let id: Int?
        let trackName: String?
        let artistName: String?
        let albumName: String?
        let duration: Double?
        let instrumental: Bool?
        let plainLyrics: String?
        let syncedLyrics: String?

        var hasSyncedLyrics: Bool { Self.hasText(syncedLyrics) }
        var hasPlainLyrics: Bool { Self.hasText(plainLyrics) }

        private static func hasText(_ value: String?) -> Bool {
            guard let value else { return false }
            return !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func fetch(title: String, artist: String, album: String?, durationSec: Int) async throws -> LyricsDocument {
        let artistQuery = Self.queryArtist(artist)
        if let record = try await exact(title: title, artist: artistQuery, album: album, durationSec: durationSec) {
            return Self.document(for: record)
        }
        return try await searched(title: title, artist: artistQuery, durationSec: durationSec)
    }

    private static func queryArtist(_ artist: String) -> String {
        var name = artist
        if let comma = name.firstIndex(of: ",") { name = String(name[name.startIndex..<comma]) }
        if let ampersand = name.range(of: " & ") { name = String(name[name.startIndex..<ampersand.lowerBound]) }
        return name
    }

    private func exact(title: String, artist: String, album: String?, durationSec: Int) async throws -> Record? {
        guard let url = URL(string: Self.base + "/get") else { throw FetchError.badURL }
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        var items = [
            URLQueryItem(name: "artist_name", value: artist),
            URLQueryItem(name: "track_name", value: title),
        ]
        if let album { items.append(URLQueryItem(name: "album_name", value: album)) }
        items.append(URLQueryItem(name: "duration", value: String(durationSec)))
        components.queryItems = items
        guard let requestURL = components.url else { throw FetchError.badURL }

        let (data, status) = try await send(Self.request(for: requestURL))
        switch status {
        case 200:
            return try JSONDecoder().decode(Record.self, from: data)
        case 404:
            return nil
        default:
            throw FetchError.badStatus(status)
        }
    }

    private func searched(title: String, artist: String, durationSec: Int) async throws -> LyricsDocument {
        guard let url = URL(string: Self.base + "/search") else { throw FetchError.badURL }
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "q", value: "\(title) \(artist)")]
        guard let requestURL = components.url else { throw FetchError.badURL }

        let (data, status) = try await send(Self.request(for: requestURL))
        switch status {
        case 200:
            let records = try JSONDecoder().decode([Record].self, from: data)
            guard let best = Self.best(records, for: durationSec) else { return .notFound }
            return Self.document(for: best)
        case 404:
            return .notFound
        default:
            throw FetchError.badStatus(status)
        }
    }

    /// Length has to agree: search results are often other songs with the same words.
    private static func best(_ records: [Record], for durationSec: Int) -> Record? {
        let candidates = records.compactMap { record -> (record: Record, gap: Double)? in
            guard let duration = record.duration else { return nil }
            let gap = abs(duration - Double(durationSec))
            guard gap <= 10 else { return nil }
            return (record, gap)
        }
        return candidates.min { a, b in
            let aClose = a.gap <= 3
            let bClose = b.gap <= 3
            if aClose != bClose { return aClose }
            if a.record.hasSyncedLyrics != b.record.hasSyncedLyrics { return a.record.hasSyncedLyrics }
            if a.record.hasPlainLyrics != b.record.hasPlainLyrics { return a.record.hasPlainLyrics }
            return a.gap < b.gap
        }?.record
    }

    private static func document(for record: Record) -> LyricsDocument {
        if record.instrumental == true { return .instrumental }
        if let synced = record.syncedLyrics, record.hasSyncedLyrics {
            let lines = LRCParser.parse(synced)
            if !lines.isEmpty { return .synced(lines) }
        }
        if let plain = record.plainLyrics, record.hasPlainLyrics {
            return .plain(plain.split(whereSeparator: \.isNewline).map(String.init))
        }
        return .notFound
    }

    private static func request(for url: URL) -> URLRequest {
        var request = URLRequest(url: url)
        request.timeoutInterval = 12
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        return request
    }

    private func send(_ request: URLRequest) async throws -> (Data, Int) {
        try await withCheckedThrowingContinuation { continuation in
            let task = session.dataTask(with: request) { data, response, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let data {
                    let status = (response as? HTTPURLResponse)?.statusCode ?? 0
                    continuation.resume(returning: (data, status))
                } else {
                    continuation.resume(throwing: FetchError.noData)
                }
            }
            task.resume()
        }
    }
}
