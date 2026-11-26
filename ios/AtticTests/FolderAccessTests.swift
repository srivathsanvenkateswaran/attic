import AtticCore
import AtticLibrary
import XCTest

@MainActor
final class FolderAccessTests: XCTestCase {
    /// A temp "Music" folder with one song, removed when the test ends.
    private func makeMusicFolder() throws -> URL {
        let dir = FileManager.default.temporaryDirectory
            .appending(path: UUID().uuidString, directoryHint: .isDirectory)
        let music = dir.appending(path: "Music", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: music, withIntermediateDirectories: true)
        try Data("x".utf8).write(to: music.appending(path: "1.mp3"))
        addTeardownBlock { try? FileManager.default.removeItem(at: dir) }
        return music
    }

    func testAddFolderSavesOneRoot() throws {
        let access = FolderAccess(store: LibraryStore(inMemory: true))
        let root = try access.addFolder(try makeMusicFolder())
        XCTAssertEqual(root.name, "Music")
        XCTAssertEqual(access.roots.count, 1)
        XCTAssertNotNil(access.resolve(root))
    }

    func testAddingTheSameFolderTwiceKeepsOneRoot() throws {
        let access = FolderAccess(store: LibraryStore(inMemory: true))
        let music = try makeMusicFolder()
        let first = try access.addFolder(music)
        let second = try access.addFolder(music)
        XCTAssertEqual(second.id, first.id)
        XCTAssertEqual(access.roots.count, 1)
    }

    func testFileURLForATrackPointsAtTheSong() async throws {
        let access = FolderAccess(store: LibraryStore(inMemory: true))
        let music = try makeMusicFolder()
        let root = try access.addFolder(music)
        let scanned = await FolderScanner().scan(root: root, at: music)
        let file = try XCTUnwrap(scanned.first)
        let track = LibraryIndex.makeTrack(file: file, tags: RawTags(), dateAdded: Date())
        let url = try XCTUnwrap(access.fileURL(for: track))
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
        XCTAssertEqual(
            url.resolvingSymlinksInPath().path,
            music.appending(path: "1.mp3").resolvingSymlinksInPath().path
        )
    }

    func testASecondAccessSeesTheSavedRoot() throws {
        let store = LibraryStore(inMemory: true)
        let root = try FolderAccess(store: store).addFolder(try makeMusicFolder())
        let access = FolderAccess(store: store)
        XCTAssertEqual(access.roots.map(\.id), [root.id])
        XCTAssertNotNil(access.resolve(root))
    }

    func testRemoveFolderEmptiesRoots() throws {
        let access = FolderAccess(store: LibraryStore(inMemory: true))
        let root = try access.addFolder(try makeMusicFolder())
        access.removeFolder(id: root.id)
        XCTAssertTrue(access.roots.isEmpty)
    }
}
