import AtticCore
import XCTest

final class TrackKeyTests: XCTestCase {
    func testKeyIsRelativePathPlusFileName() {
        XCTAssertEqual(TrackKey.make(relativePath: "Music/Artist/", fileName: "01 Song.mp3"), "Music/Artist/01 Song.mp3")
        XCTAssertEqual(TrackKey.make(relativePath: "Music/Artist", fileName: "a.flac"), "Music/Artist/a.flac")
    }

    func testRelativePathStartsWithTheRootName() {
        let root = URL(fileURLWithPath: "/tmp/x/Music")
        let file = URL(fileURLWithPath: "/tmp/x/Music/A/B/c.mp3")
        XCTAssertEqual(TrackKey.relativePath(of: file, under: root), "Music/A/B/")
        XCTAssertEqual(TrackKey.relativePath(of: URL(fileURLWithPath: "/tmp/x/Music/c.mp3"), under: root), "Music/")
        XCTAssertNil(TrackKey.relativePath(of: URL(fileURLWithPath: "/tmp/y/c.mp3"), under: root))
    }
}
