import AtticCore
import AtticLibrary
import XCTest

private final class StubProtocol: URLProtocol {
    private static let lock = NSLock()
    private static var responder: ((URLRequest) -> (status: Int, body: String))?
    private static var recorded: [URLRequest] = []

    static func stub(_ responder: @escaping (URLRequest) -> (status: Int, body: String)) {
        lock.lock()
        self.responder = responder
        recorded = []
        lock.unlock()
    }

    static func clear() {
        lock.lock()
        responder = nil
        recorded = []
        lock.unlock()
    }

    static var requests: [URLRequest] {
        lock.lock()
        defer { lock.unlock() }
        return recorded
    }

    override class func canInit(with request: URLRequest) -> Bool { true }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        Self.lock.lock()
        Self.recorded.append(request)
        let responder = Self.responder
        Self.lock.unlock()

        let (status, body) = responder?(request) ?? (500, "{}")
        let response = HTTPURLResponse(
            url: request.url!, statusCode: status, httpVersion: nil,
            headerFields: ["Content-Type": "application/json"]
        )!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

final class LRCLibClientTests: XCTestCase {
    override func setUp() {
        super.setUp()
        StubProtocol.clear()
    }

    override func tearDown() {
        StubProtocol.clear()
        super.tearDown()
    }

    private func makeClient() -> LRCLibClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        return LRCLibClient(session: URLSession(configuration: configuration))
    }

    private func json(_ object: Any) -> String {
        let data = try! JSONSerialization.data(withJSONObject: object)
        return String(decoding: data, as: UTF8.self)
    }

    private func record(
        duration: Double = 183, synced: String? = nil, plain: String? = nil, instrumental: Bool = false
    ) -> [String: Any] {
        [
            "id": 1,
            "trackName": "Song",
            "artistName": "Artist",
            "albumName": "Album",
            "duration": duration,
            "instrumental": instrumental,
            "plainLyrics": plain ?? NSNull(),
            "syncedLyrics": synced ?? NSNull(),
        ]
    }

    func testGetURLUsesCutArtistAndDurationWithoutAlbum() async throws {
        StubProtocol.stub { _ in (200, self.json(self.record(synced: "[00:01.00]Hi"))) }
        let client = makeClient()

        _ = try await client.fetch(
            title: "Song Name", artist: "First, Second & Third", album: nil, durationSec: 183
        )

        let request = try XCTUnwrap(StubProtocol.requests.first)
        let url = try XCTUnwrap(request.url)
        XCTAssertEqual(url.scheme, "https")
        XCTAssertEqual(url.host, "lrclib.net")
        XCTAssertEqual(url.path, "/api/get")
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let items = Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })
        XCTAssertEqual(items["artist_name"], "First")
        XCTAssertEqual(items["track_name"], "Song Name")
        XCTAssertEqual(items["duration"], "183")
        XCTAssertNil(items["album_name"])
        XCTAssertEqual(
            request.value(forHTTPHeaderField: "User-Agent"),
            "Attic-iOS/1.0 (https://github.com/srivathsanvenkateswaran/attic)"
        )
        XCTAssertEqual(request.timeoutInterval, 12)
    }

    func testGetURLCutsAmpersandAndIncludesAlbum() async throws {
        StubProtocol.stub { _ in (200, self.json(self.record(synced: "[00:01.00]Hi"))) }
        let client = makeClient()

        _ = try await client.fetch(title: "Song", artist: "A & B", album: "The Album", durationSec: 200)

        let url = try XCTUnwrap(StubProtocol.requests.first?.url)
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        let items = Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })
        XCTAssertEqual(items["artist_name"], "A")
        XCTAssertEqual(items["album_name"], "The Album")
    }

    func testSyncedLyricsFromGet() async throws {
        StubProtocol.stub { _ in (200, self.json(self.record(synced: "[00:01.00]Hello\n[00:02.50]World"))) }
        let client = makeClient()

        let document = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)

        XCTAssertEqual(document, .synced([
            LyricLine(timeMs: 1_000, text: "Hello"),
            LyricLine(timeMs: 2_500, text: "World"),
        ]))
    }

    func testInstrumentalFromGet() async throws {
        StubProtocol.stub { _ in
            (200, self.json(self.record(synced: "[00:01.00]ignored", instrumental: true)))
        }
        let client = makeClient()

        let document = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)

        XCTAssertEqual(document, .instrumental)
    }

    func testPlainLyricsFromGet() async throws {
        StubProtocol.stub { _ in (200, self.json(self.record(plain: "first line\nsecond line"))) }
        let client = makeClient()

        let document = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)

        XCTAssertEqual(document, .plain(["first line", "second line"]))
    }

    func testSearchPicksRecordWithinThreeSeconds() async throws {
        StubProtocol.stub { request in
            if request.url?.path == "/api/search" {
                let far = self.record(duration: 200, plain: "wrong song")
                let close = self.record(duration: 184, synced: "[00:01.00]right song")
                return (200, self.json([far, close]))
            }
            return (404, "{}")
        }
        let client = makeClient()

        let document = try await client.fetch(
            title: "Song", artist: "A, B & C", album: nil, durationSec: 183
        )

        XCTAssertEqual(document, .synced([LyricLine(timeMs: 1_000, text: "right song")]))
        XCTAssertEqual(StubProtocol.requests.count, 2)
        let searchURL = try XCTUnwrap(StubProtocol.requests[1].url)
        XCTAssertEqual(searchURL.path, "/api/search")
        let components = try XCTUnwrap(URLComponents(url: searchURL, resolvingAgainstBaseURL: false))
        let items = Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })
        XCTAssertEqual(items["q"], "Song A")
    }

    func testSearchIgnoresRecordsOutsideTenSeconds() async throws {
        StubProtocol.stub { request in
            if request.url?.path == "/api/search" {
                return (200, self.json([self.record(duration: 250, synced: "[00:01.00]too far")]))
            }
            return (404, "{}")
        }
        let client = makeClient()

        let document = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)

        XCTAssertEqual(document, .notFound)
    }

    func testNotFoundWhenBothRequestsReturn404() async throws {
        StubProtocol.stub { _ in (404, "{}") }
        let client = makeClient()

        let document = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)

        XCTAssertEqual(document, .notFound)
        XCTAssertEqual(StubProtocol.requests.count, 2)
    }

    func testServerErrorThrows() async throws {
        StubProtocol.stub { _ in (500, "{}") }
        let client = makeClient()

        do {
            _ = try await client.fetch(title: "Song", artist: "Artist", album: nil, durationSec: 183)
            XCTFail("Expected a network error for status 500")
        } catch {
            XCTAssertEqual(StubProtocol.requests.count, 1)
        }
    }
}
