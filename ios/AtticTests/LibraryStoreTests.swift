import AtticCore
import AtticLibrary
import Foundation
import XCTest

@MainActor
final class LibraryStoreTests: XCTestCase {
    private func makeTrack(fileName: String, title: String? = nil) -> Track {
        let file = ScannedFile(
            rootID: UUID(),
            relativePath: "Music/Artist/Album/",
            fileName: fileName,
            url: URL(fileURLWithPath: "/tmp/Attic/Music/Artist/Album/\(fileName)"),
            modified: Date(timeIntervalSince1970: 1_000),
            size: 42
        )
        let tags = RawTags(title: title, artist: "Artist", album: "Album", durationMs: 180_000)
        return LibraryIndex.makeTrack(file: file, tags: tags, dateAdded: Date(timeIntervalSince1970: 500))
    }

    func testSaveTracksRoundTripsAndPrunes() {
        let store = LibraryStore(inMemory: true)
        let a = makeTrack(fileName: "a.mp3")
        let b = makeTrack(fileName: "b.mp3")
        let c = makeTrack(fileName: "c.mp3")

        store.saveTracks([a, b, c])
        XCTAssertEqual(Set(store.loadTracks()), Set([a, b, c]))

        var retitled = a
        retitled.title = "Changed"
        store.saveTracks([retitled, b])

        let loaded = store.loadTracks()
        XCTAssertEqual(loaded.count, 2)
        XCTAssertEqual(loaded.first { $0.key == a.key }?.title, "Changed")
        XCTAssertNil(loaded.first { $0.key == c.key })
    }

    func testRootsRoundTripInOrder() {
        let store = LibraryStore(inMemory: true)
        let music = LibraryRoot(name: "Music", bookmark: Data([1, 2, 3]))
        let audiobooks = LibraryRoot(name: "Audiobooks", bookmark: Data([4, 5]))

        store.saveRoots([music, audiobooks])
        XCTAssertEqual(store.loadRoots(), [music, audiobooks])

        store.saveRoots([audiobooks, music])
        XCTAssertEqual(store.loadRoots(), [audiobooks, music])
    }

    func testLikesAreMostRecentFirst() {
        let store = LibraryStore(inMemory: true)
        store.setLiked(true, key: "a")
        store.setLiked(true, key: "b")
        XCTAssertEqual(store.likedKeys(), ["b", "a"])

        store.setLiked(false, key: "a")
        XCTAssertEqual(store.likedKeys(), ["b"])
    }

    func testPlaysCountAndOrder() {
        let store = LibraryStore(inMemory: true)
        let start = Date(timeIntervalSince1970: 1_000)
        store.recordPlay(key: "a", at: start)
        store.recordPlay(key: "a", at: start.addingTimeInterval(10))
        store.recordPlay(key: "b", at: start.addingTimeInterval(20))

        XCTAssertEqual(store.playCounts(), ["a": 2, "b": 1])
        XCTAssertEqual(store.recentKeys(limit: 10), ["b", "a"])
        XCTAssertEqual(store.recentKeys(limit: 1), ["b"])
    }

    func testPlaylists() {
        let store = LibraryStore(inMemory: true)
        let road = store.createPlaylist(name: "  Road  ")
        let gym = store.createPlaylist(name: "Gym")

        XCTAssertEqual(store.playlists().map(\.name), ["Gym", "Road"])
        XCTAssertEqual(store.playlists().first { $0.id == road.id }?.name, "Road")

        store.addToPlaylist(id: road.id, keys: ["x", "y", "z"])
        XCTAssertEqual(store.playlists().first { $0.id == road.id }?.trackKeys, ["x", "y", "z"])

        store.movePlaylistEntry(id: road.id, from: 0, to: 2)
        XCTAssertEqual(store.playlists().first { $0.id == road.id }?.trackKeys, ["y", "z", "x"])

        store.removeFromPlaylist(id: road.id, at: 1)
        XCTAssertEqual(store.playlists().first { $0.id == road.id }?.trackKeys, ["y", "x"])

        store.renamePlaylist(id: road.id, name: "Trip")
        XCTAssertEqual(store.playlists().first { $0.id == road.id }?.name, "Trip")

        store.deletePlaylist(id: gym.id)
        XCTAssertEqual(store.playlists().map(\.id), [road.id])
    }

    func testLyricsRoundTrip() {
        let store = LibraryStore(inMemory: true)
        let synced = LyricsDocument.synced([LyricLine(timeMs: 1_000, text: "hi")])

        store.saveLyrics(synced, key: "a")
        XCTAssertEqual(store.lyrics(key: "a"), synced)

        store.saveLyrics(.plain(["new"]), key: "a")
        XCTAssertEqual(store.lyrics(key: "a"), .plain(["new"]))

        store.saveLyrics(.notFound, key: "b")
        XCTAssertEqual(store.lyrics(key: "b"), LyricsDocument.notFound)
        XCTAssertNil(store.lyrics(key: "missing"))
    }

    func testLargeSaveCompletes() {
        let store = LibraryStore(inMemory: true)
        let tracks = (0..<5_000).map { index in
            makeTrack(fileName: String(format: "%05d.mp3", index), title: "Song \(index)")
        }

        store.saveTracks(tracks)
        XCTAssertEqual(store.loadTracks().count, 5_000)

        store.saveTracks(tracks)
        XCTAssertEqual(store.loadTracks().count, 5_000)
    }
}
