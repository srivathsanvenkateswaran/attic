import AtticCore
import AtticLibrary
import XCTest

final class FolderScannerTests: XCTestCase {
    private var tempDir: URL!
    private var music: URL!

    override func setUpWithError() throws {
        tempDir = FileManager.default.temporaryDirectory
            .appending(path: UUID().uuidString, directoryHint: .isDirectory)
        music = tempDir.appending(path: "Music", directoryHint: .isDirectory)
        for name in ["A/1.mp3", "A/B/2.flac", "C/3.M4A", ".hidden.mp3", "A/notes.txt", "A/cover.jpg"] {
            let file = music.appending(path: name)
            try FileManager.default.createDirectory(
                at: file.deletingLastPathComponent(), withIntermediateDirectories: true
            )
            try Data("x".utf8).write(to: file)
        }
    }

    override func tearDownWithError() throws {
        if let tempDir { try? FileManager.default.removeItem(at: tempDir) }
        tempDir = nil
        music = nil
    }

    func testScanFindsOnlyAudioFiles() async {
        let files = await FolderScanner().scan(root: LibraryRoot(name: "Music", bookmark: Data()), at: music)
        XCTAssertEqual(files.map(\.key), ["Music/A/1.mp3", "Music/A/B/2.flac", "Music/C/3.M4A"])
        XCTAssertEqual(files.map(\.relativePath), ["Music/A/", "Music/A/B/", "Music/C/"])
        XCTAssertEqual(files.map(\.fileName), ["1.mp3", "2.flac", "3.M4A"])
    }
}
