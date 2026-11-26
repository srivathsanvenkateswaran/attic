import AtticCore
import XCTest

final class LRCParserTests: XCTestCase {
    func testBasicLines() {
        let text = "[00:01.00]Hello\r\n[00:02.50]World"
        XCTAssertEqual(LRCParser.parse(text), [
            LyricLine(timeMs: 1_000, text: "Hello"),
            LyricLine(timeMs: 2_500, text: "World"),
        ])
    }

    func testTwoAndThreeDigitFractions() {
        let text = "[00:01.2]tenths\n[00:01.25]hundredths\n[00:01.250]milliseconds"
        XCTAssertEqual(LRCParser.parse(text), [
            LyricLine(timeMs: 1_200, text: "tenths"),
            LyricLine(timeMs: 1_250, text: "hundredths"),
            LyricLine(timeMs: 1_250, text: "milliseconds"),
        ])
    }

    func testColonFractionAndOneToThreeDigitMinutes() {
        let text = "[00:01:50]colon\n[1:02]one digit minute\n[123:04.50]three digit minute"
        XCTAssertEqual(LRCParser.parse(text), [
            LyricLine(timeMs: 1_500, text: "colon"),
            LyricLine(timeMs: 62_000, text: "one digit minute"),
            LyricLine(timeMs: 7_384_500, text: "three digit minute"),
        ])
    }

    func testSeveralStampsOnOneLine() {
        let lines = LRCParser.parse("[00:12.00][01:30.50]words")
        XCTAssertEqual(lines, [
            LyricLine(timeMs: 12_000, text: "words"),
            LyricLine(timeMs: 90_500, text: "words"),
        ])
    }

    func testOffsetMakesLinesEarlier() {
        let text = "[ar:Someone]\n[offset:+500]\n[00:02.00]Hi"
        XCTAssertEqual(LRCParser.parse(text), [LyricLine(timeMs: 1_500, text: "Hi")])
    }

    func testNegativeOffsetMakesLinesLater() {
        let text = "[offset:-250]\n[00:02.00]Hi"
        XCTAssertEqual(LRCParser.parse(text), [LyricLine(timeMs: 2_250, text: "Hi")])
    }

    func testOffsetClampsAtZero() {
        let text = "[offset:+5000]\n[00:01.00]Hi"
        XCTAssertEqual(LRCParser.parse(text), [LyricLine(timeMs: 0, text: "Hi")])
    }

    func testMetadataTagsAreIgnored() {
        let text = """
        [ar:Artist]
        [ti:Title]
        [al:Album]
        [length:03:20]
        [by:someone]
        untimed words
        [00:03.00]words
        """
        XCTAssertEqual(LRCParser.parse(text), [LyricLine(timeMs: 3_000, text: "words")])
    }

    func testBlankTextIsKept() {
        XCTAssertEqual(LRCParser.parse("[00:05.00]   "), [LyricLine(timeMs: 5_000, text: "")])
    }

    func testWordStampsAreStripped() {
        let text = "[00:04.00]<00:04.10>He<00:04.50>llo"
        XCTAssertEqual(LRCParser.parse(text), [LyricLine(timeMs: 4_000, text: "Hello")])
    }

    func testOutOfOrderInputIsSortedAndEqualTimesKeepSourceOrder() {
        let text = "[00:05.00]late\n[00:02.00]first\n[00:02.00]second\n[00:01.00]early"
        XCTAssertEqual(LRCParser.parse(text), [
            LyricLine(timeMs: 1_000, text: "early"),
            LyricLine(timeMs: 2_000, text: "first"),
            LyricLine(timeMs: 2_000, text: "second"),
            LyricLine(timeMs: 5_000, text: "late"),
        ])
    }

    func testActiveIndex() {
        let lines = [
            LyricLine(timeMs: 1_000, text: "a"),
            LyricLine(timeMs: 2_500, text: "b"),
            LyricLine(timeMs: 5_000, text: "c"),
        ]
        XCTAssertNil(LRCParser.activeIndex(in: lines, at: 999))
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 1_000), 0)
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 2_499), 0)
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 2_500), 1)
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 4_999), 1)
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 5_000), 2)
        XCTAssertEqual(LRCParser.activeIndex(in: lines, at: 9_999), 2)
    }

    func testActiveIndexOnEmptyList() {
        XCTAssertNil(LRCParser.activeIndex(in: [], at: 0))
        XCTAssertNil(LRCParser.activeIndex(in: [], at: 10_000))
    }
}
