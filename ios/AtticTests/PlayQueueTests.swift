import AtticCore
import XCTest

final class PlayQueueTests: XCTestCase {
    private struct SeededGenerator: RandomNumberGenerator {
        var state: UInt64

        mutating func next() -> UInt64 {
            state = state &* 6364136223846793005 &+ 1442695040888963407
            return state
        }
    }

    private func generator(_ seed: UInt64 = 42) -> SeededGenerator {
        SeededGenerator(state: seed)
    }

    private func tracks(from names: [String]) -> [Track] {
        let rootID = UUID()
        let date = Date(timeIntervalSince1970: 0)
        return names.map { name in
            let file = ScannedFile(
                rootID: rootID, relativePath: "Music/", fileName: "\(name).mp3",
                url: URL(fileURLWithPath: "/tmp/Music/\(name).mp3"), modified: date, size: 0
            )
            return LibraryIndex.makeTrack(file: file, tags: RawTags(title: name), dateAdded: date)
        }
    }

    private func makeTracks(_ count: Int = 5) -> [Track] {
        tracks(from: (0..<count).map(String.init))
    }

    private func keys<S: Sequence>(_ tracks: S) -> [String] where S.Element == Track {
        tracks.map(\.key)
    }

    private func assertInvariants(_ queue: PlayQueue, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(
            queue.order.sorted(), Array(queue.original.indices),
            "order is always a permutation of original indices", file: file, line: line
        )
        XCTAssertEqual(
            queue.tracks.count, queue.original.count,
            "tracks and original have the same count", file: file, line: line
        )
        if queue.original.isEmpty {
            XCTAssertEqual(queue.position, 0, "an empty queue sits at 0", file: file, line: line)
        } else {
            XCTAssertTrue(queue.order.indices.contains(queue.position), "position is in range", file: file, line: line)
        }
    }

    func testInitNotShuffledStartsAtTheGivenSong() {
        let all = makeTracks()
        var rng = generator()
        let queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        assertInvariants(queue)
        XCTAssertFalse(queue.isShuffled)
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(keys(queue.tracks), keys(all))
        XCTAssertEqual(keys(queue.upNext), keys(all[3...]))
    }

    func testInitShuffledPutsTheStartSongFirst() {
        let all = makeTracks()
        var rng = generator()
        let queue = PlayQueue(tracks: all, startAt: 3, shuffled: true, using: &rng)
        assertInvariants(queue)
        XCTAssertTrue(queue.isShuffled)
        XCTAssertEqual(queue.current?.key, all[3].key)
        XCTAssertEqual(queue.position, 0)
        XCTAssertEqual(queue.tracks.first?.key, all[3].key)
        XCTAssertEqual(Set(keys(queue.tracks)), Set(keys(all)))
    }

    func testInitClampsStartAt() {
        let all = makeTracks()
        var rng = generator()
        let below = PlayQueue(tracks: all, startAt: -4, shuffled: false, using: &rng)
        assertInvariants(below)
        XCTAssertEqual(below.current?.key, all[0].key)
        let above = PlayQueue(tracks: all, startAt: 99, shuffled: false, using: &rng)
        assertInvariants(above)
        XCTAssertEqual(above.current?.key, all[4].key)
    }

    func testShuffleOnThenOffKeepsTheCurrentSong() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        assertInvariants(queue)
        queue.setShuffle(true, using: &rng)
        assertInvariants(queue)
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(queue.position, 0)
        XCTAssertEqual(queue.tracks.first?.key, all[2].key)
        XCTAssertEqual(Set(keys(queue.tracks)), Set(keys(all)))
        queue.setShuffle(false, using: &rng)
        assertInvariants(queue)
        XCTAssertFalse(queue.isShuffled)
        XCTAssertEqual(keys(queue.tracks), keys(all))
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(queue.position, 2)
    }

    func testReshuffleKeepsTheSongAtTheCurrentPositionFirst() {
        let all = makeTracks()
        var rng = generator(7)
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: true, using: &rng)
        queue.jump(to: 3)
        assertInvariants(queue)
        let currentKey = queue.current?.key
        queue.setShuffle(true, using: &rng)
        assertInvariants(queue)
        XCTAssertEqual(queue.position, 0)
        XCTAssertEqual(queue.current?.key, currentKey)
        XCTAssertEqual(queue.tracks.first?.key, currentKey)
        XCTAssertEqual(Set(keys(queue.tracks)), Set(keys(all)))
    }

    func testShuffleOnAnEmptyQueueOnlySetsTheFlag() {
        var rng = generator()
        var queue = PlayQueue.empty
        queue.setShuffle(true, using: &rng)
        assertInvariants(queue)
        XCTAssertTrue(queue.isShuffled)
        XCTAssertTrue(queue.order.isEmpty)
        XCTAssertEqual(queue.position, 0)
        queue.setShuffle(false, using: &rng)
        assertInvariants(queue)
        XCTAssertFalse(queue.isShuffled)
    }

    func testPositionAfterEnd() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: false, using: &rng)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .off), 1)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .all), 1)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .one), 0)

        queue.jump(to: 2)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .off), 3)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .all), 3)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .one), 2)

        queue.jump(to: 4)
        assertInvariants(queue)
        XCTAssertNil(queue.positionAfterEnd(repeat: .off))
        XCTAssertEqual(queue.positionAfterEnd(repeat: .all), 0)
        XCTAssertEqual(queue.positionAfterEnd(repeat: .one), 4)

        let empty = PlayQueue.empty
        assertInvariants(empty)
        XCTAssertNil(empty.positionAfterEnd(repeat: .off))
        XCTAssertNil(empty.positionAfterEnd(repeat: .all))
        XCTAssertNil(empty.positionAfterEnd(repeat: .one))
    }

    func testPositionForNext() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: false, using: &rng)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionForNext(repeat: .off), 1)
        XCTAssertEqual(queue.positionForNext(repeat: .all), 1)
        XCTAssertEqual(queue.positionForNext(repeat: .one), 1)

        queue.jump(to: 2)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionForNext(repeat: .off), 3)
        XCTAssertEqual(queue.positionForNext(repeat: .all), 3)
        XCTAssertEqual(queue.positionForNext(repeat: .one), 3)

        queue.jump(to: 4)
        assertInvariants(queue)
        XCTAssertNil(queue.positionForNext(repeat: .off))
        XCTAssertEqual(queue.positionForNext(repeat: .all), 0)
        XCTAssertEqual(queue.positionForNext(repeat: .one), 0)

        let empty = PlayQueue.empty
        assertInvariants(empty)
        XCTAssertNil(empty.positionForNext(repeat: .off))
        XCTAssertNil(empty.positionForNext(repeat: .all))
        XCTAssertNil(empty.positionForNext(repeat: .one))
    }

    func testPositionForPrevious() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: false, using: &rng)
        assertInvariants(queue)
        XCTAssertNil(queue.positionForPrevious(repeat: .off))
        XCTAssertEqual(queue.positionForPrevious(repeat: .all), 4)
        XCTAssertNil(queue.positionForPrevious(repeat: .one))

        queue.jump(to: 2)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionForPrevious(repeat: .off), 1)
        XCTAssertEqual(queue.positionForPrevious(repeat: .all), 1)
        XCTAssertEqual(queue.positionForPrevious(repeat: .one), 1)

        queue.jump(to: 4)
        assertInvariants(queue)
        XCTAssertEqual(queue.positionForPrevious(repeat: .off), 3)
        XCTAssertEqual(queue.positionForPrevious(repeat: .all), 3)
        XCTAssertEqual(queue.positionForPrevious(repeat: .one), 3)

        let empty = PlayQueue.empty
        assertInvariants(empty)
        XCTAssertNil(empty.positionForPrevious(repeat: .off))
        XCTAssertNil(empty.positionForPrevious(repeat: .all))
        XCTAssertNil(empty.positionForPrevious(repeat: .one))
    }

    func testJumpIgnoresOutOfRange() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: false, using: &rng)
        queue.jump(to: 2)
        assertInvariants(queue)
        XCTAssertEqual(queue.current?.key, all[2].key)
        queue.jump(to: -1)
        assertInvariants(queue)
        queue.jump(to: 5)
        assertInvariants(queue)
        XCTAssertEqual(queue.position, 2)
    }

    func testMoveNotShuffledAlsoMovesTheCollection() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: false, using: &rng)
        assertInvariants(queue)
        queue.move(from: 3, to: 0)
        assertInvariants(queue)
        let expected = [all[3], all[0], all[1], all[2], all[4]]
        XCTAssertEqual(keys(queue.tracks), keys(expected))
        XCTAssertEqual(queue.current?.key, all[1].key)
        XCTAssertEqual(queue.position, 2)

        queue.setShuffle(false, using: &rng)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys(expected))
        XCTAssertEqual(queue.current?.key, all[1].key)
        XCTAssertEqual(queue.position, 2)
    }

    func testMoveOfCurrentSongTakesItsPositionWithIt() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: false, using: &rng)
        queue.move(from: 1, to: 3)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[2], all[3], all[1], all[4]]))
        XCTAssertEqual(queue.current?.key, all[1].key)
        XCTAssertEqual(queue.position, 3)

        queue.move(from: 2, to: 2)
        assertInvariants(queue)
        queue.move(from: -1, to: 0)
        assertInvariants(queue)
        queue.move(from: 0, to: 99)
        assertInvariants(queue)
        XCTAssertEqual(queue.position, 3)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[2], all[3], all[1], all[4]]))
    }

    func testMoveShuffledKeepsTheCurrentSongAndTheCollection() {
        let all = makeTracks()
        var rng = generator(11)
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: true, using: &rng)
        queue.jump(to: 2)
        assertInvariants(queue)
        let originalKeys = keys(queue.original)
        let currentKey = queue.current?.key
        let before = queue.order

        queue.move(from: 0, to: 4)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.original), originalKeys)
        XCTAssertEqual(queue.current?.key, currentKey)
        XCTAssertEqual(queue.order, [before[1], before[2], before[3], before[4], before[0]])
        XCTAssertEqual(queue.position, 1)
    }

    func testRemoveBeforeCurrent() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        queue.remove(at: 0)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[1], all[2], all[3], all[4]]))
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(queue.position, 1)
    }

    func testRemoveCurrentSlidesToTheNextSong() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        queue.remove(at: 2)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[1], all[3], all[4]]))
        XCTAssertEqual(queue.current?.key, all[3].key)
        XCTAssertEqual(queue.position, 2)
    }

    func testRemoveAfterCurrentLeavesPositionAlone() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: false, using: &rng)
        queue.remove(at: 3)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[1], all[2], all[4]]))
        XCTAssertEqual(queue.current?.key, all[1].key)
        XCTAssertEqual(queue.position, 1)
    }

    func testRemoveLastCurrentFallsBack() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 4, shuffled: false, using: &rng)
        queue.remove(at: 4)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[1], all[2], all[3]]))
        XCTAssertEqual(queue.current?.key, all[3].key)
        XCTAssertEqual(queue.position, 3)
    }

    func testRemoveDownToEmpty() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        while !queue.isEmpty {
            queue.remove(at: queue.position)
            assertInvariants(queue)
        }
        XCTAssertTrue(queue.tracks.isEmpty)
        XCTAssertEqual(queue.position, 0)
        XCTAssertNil(queue.current)
        queue.remove(at: 0)
        assertInvariants(queue)
    }

    func testRemoveShuffledReindexesTheOrder() {
        let all = makeTracks()
        var rng = generator(3)
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: true, using: &rng)
        queue.jump(to: 1)
        assertInvariants(queue)
        let currentKey = queue.current?.key
        let removedKey = queue.tracks[3].key

        queue.remove(at: 3)
        assertInvariants(queue)
        XCTAssertEqual(queue.current?.key, currentKey)
        XCTAssertEqual(Set(keys(queue.tracks)).count, 4)
        XCTAssertFalse(Set(keys(queue.tracks)).contains(removedKey))
    }

    func testPlayNextNotShuffledGoesAfterTheCurrentSong() {
        let all = makeTracks()
        let newTracks = tracks(from: ["new"])
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: false, using: &rng)
        assertInvariants(queue)
        queue.playNext(newTracks)
        assertInvariants(queue)
        XCTAssertEqual(
            keys(queue.tracks), keys([all[0], all[1], newTracks[0], all[2], all[3], all[4]])
        )
        XCTAssertEqual(queue.current?.key, all[1].key)
        XCTAssertEqual(queue.position, 1)
    }

    func testPlayNextShuffledLandsAfterTheCurrentPosition() {
        let all = makeTracks()
        let newTracks = tracks(from: ["newA", "newB"])
        var rng = generator(9)
        var queue = PlayQueue(tracks: all, startAt: 1, shuffled: true, using: &rng)
        queue.jump(to: 2)
        assertInvariants(queue)
        let currentKey = queue.current?.key

        queue.playNext(newTracks)
        assertInvariants(queue)
        XCTAssertEqual(queue.current?.key, currentKey)
        XCTAssertEqual(queue.position, 2)
        XCTAssertEqual(keys(queue.tracks).dropFirst(3).prefix(2), ["Music/newA.mp3", "Music/newB.mp3"])
        XCTAssertEqual(Set(keys(queue.tracks)).count, 7)
    }

    func testPlayNextOnAnEmptyQueueBecomesCurrent() {
        var rng = generator()
        var queue = PlayQueue.empty
        let newTracks = tracks(from: ["a", "b"])
        queue.playNext(newTracks)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), ["Music/a.mp3", "Music/b.mp3"])
        XCTAssertEqual(queue.current?.key, newTracks[0].key)
        XCTAssertEqual(queue.position, 0)
    }

    func testAppendAddsToTheEndOfThePlayOrder() {
        let all = makeTracks()
        let more = tracks(from: ["a", "b"])
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        assertInvariants(queue)
        queue.append(more)
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys(all + more))
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(queue.position, 2)

        var shuffledRng = generator(5)
        var shuffled = PlayQueue(tracks: all, startAt: 1, shuffled: true, using: &shuffledRng)
        let before = shuffled.order
        assertInvariants(shuffled)
        shuffled.append(more)
        assertInvariants(shuffled)
        XCTAssertEqual(Array(shuffled.order.dropLast(2)), before)
        XCTAssertEqual(Array(shuffled.order.suffix(2)), [5, 6])
        XCTAssertEqual(shuffled.tracks.last?.key, more[1].key)
    }

    func testClearUpNextKeepsTheCurrentSongAndEverythingBeforeIt() {
        let all = makeTracks()
        var rng = generator()
        var queue = PlayQueue(tracks: all, startAt: 2, shuffled: false, using: &rng)
        assertInvariants(queue)
        queue.clearUpNext()
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[1], all[2]]))
        XCTAssertEqual(queue.current?.key, all[2].key)
        XCTAssertEqual(queue.position, 2)
        XCTAssertTrue(queue.upNext.isEmpty)

        queue.clearUpNext()
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), keys([all[0], all[1], all[2]]))
    }

    func testClearUpNextShuffledDropsOnlyWhatFollowsTheCurrentSong() {
        let all = makeTracks()
        var rng = generator(13)
        var queue = PlayQueue(tracks: all, startAt: 0, shuffled: true, using: &rng)
        queue.jump(to: 1)
        assertInvariants(queue)
        let kept = keys(queue.tracks.prefix(2))
        let currentKey = queue.current?.key

        queue.clearUpNext()
        assertInvariants(queue)
        XCTAssertEqual(keys(queue.tracks), kept)
        XCTAssertEqual(queue.current?.key, currentKey)
        XCTAssertEqual(queue.position, 1)
    }
}
