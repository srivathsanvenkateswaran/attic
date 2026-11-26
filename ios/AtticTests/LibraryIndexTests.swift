import AtticCore
import XCTest

final class LibraryIndexTests: XCTestCase {
    private let rootID = UUID(uuidString: "00000000-0000-0000-0000-00000000CAFE")!
    private let added = Date(timeIntervalSince1970: 1_700_000_000)

    func file(_ path: String, _ name: String) -> ScannedFile {
        ScannedFile(
            rootID: rootID, relativePath: path, fileName: name,
            url: URL(fileURLWithPath: "/tmp/attic-tests/" + path + name),
            modified: Date(timeIntervalSince1970: 1_600_000_000), size: 1
        )
    }

    private func makeTrack(_ path: String, _ name: String, _ tags: RawTags) -> Track {
        LibraryIndex.makeTrack(file: file(path, name), tags: tags, dateAdded: added)
    }

    func testTitleFallsBackToTheFileNameWithoutItsTrackNumber() {
        let noTags = RawTags()
        XCTAssertEqual(makeTrack("Music/", "01 - Intro.mp3", noTags).title, "Intro")
        XCTAssertEqual(makeTrack("Music/", "07 Song.flac", noTags).title, "Song")
        XCTAssertEqual(makeTrack("Music/", "3. Song.mp3", noTags).title, "Song")
        XCTAssertEqual(makeTrack("Music/", "1999.mp3", noTags).title, "1999")
        XCTAssertEqual(makeTrack("Music/", "01 - Intro.mp3", RawTags(title: "  Handy Name  ")).title, "Handy Name")
    }

    func testTagsAreTrimmedAndBlankBecomesNil() {
        let track = makeTrack(
            "Music/", "a.mp3",
            RawTags(title: "  Song  ", artist: "  Me  ", album: "  LP  ", genre: "  Rock  ")
        )
        XCTAssertEqual(track.title, "Song")
        XCTAssertEqual(track.artist, "Me")
        XCTAssertEqual(track.album, "LP")
        XCTAssertEqual(track.genre, "Rock")
        let blank = makeTrack("Music/", "b.mp3", RawTags(title: "   ", album: "   "))
        XCTAssertEqual(blank.title, "b")
        XCTAssertNil(blank.album)
        XCTAssertNil(blank.albumKey)
    }

    func testArtistFallsBackToAlbumArtistThenUnknown() {
        XCTAssertEqual(makeTrack("Music/", "a.mp3", RawTags()).artist, "Unknown artist")
        XCTAssertEqual(makeTrack("Music/", "a.mp3", RawTags(albumArtist: "The Band")).artist, "The Band")
        XCTAssertEqual(makeTrack("Music/", "a.mp3", RawTags(artist: "Solo", albumArtist: "The Band")).artist, "Solo")
        let featured = makeTrack("Music/", "a.mp3", RawTags(artist: "Alpha feat. Beta"))
        XCTAssertEqual(featured.artistKeys, ["alpha", "beta"])
        let repeated = makeTrack("Music/", "a.mp3", RawTags(artist: "Alpha; alpha; Beta"))
        XCTAssertEqual(repeated.artistKeys, ["alpha", "beta"])
    }

    func testSplitArtists() {
        XCTAssertEqual(LibraryIndex.splitArtists("A feat. B"), ["A", "B"])
        XCTAssertEqual(LibraryIndex.splitArtists("A; B"), ["A", "B"])
        XCTAssertEqual(LibraryIndex.splitArtists("A / B"), ["A", "B"])
        XCTAssertEqual(LibraryIndex.splitArtists("A featuring B"), ["A", "B"])
        XCTAssertEqual(LibraryIndex.splitArtists("Simon & Garfunkel"), ["Simon & Garfunkel"])
        XCTAssertEqual(LibraryIndex.splitArtists("Tyler, The Creator"), ["Tyler, The Creator"])
    }

    func testAlbumWithoutAlbumArtistInOneFolderIsOneVariousArtistsAlbum() {
        let one = makeTrack("Music/Hits/", "1.mp3", RawTags(title: "One", artist: "Alpha", album: "Hits"))
        let two = makeTrack("Music/Hits/", "2.mp3", RawTags(title: "Two", artist: "Beta", album: "Hits"))
        let library = LibraryIndex.build([two, one])
        XCTAssertEqual(library.albums.count, 1)
        let album = library.albums[0]
        XCTAssertEqual(album.name, "Hits")
        XCTAssertEqual(album.artist, "Various artists")
        XCTAssertNil(album.artistKey)
        XCTAssertEqual(album.tracks.map(\.title), ["One", "Two"])
    }

    func testSameAlbumNameInTwoFoldersWithoutAlbumArtistMakesTwoAlbums() {
        let a = makeTrack("Music/A/", "x.mp3", RawTags(title: "One", artist: "Alpha", album: "Hits"))
        let b = makeTrack("Music/B/", "x.mp3", RawTags(title: "Two", artist: "Beta", album: "Hits"))
        let library = LibraryIndex.build([a, b])
        XCTAssertEqual(library.albums.count, 2)
        XCTAssertEqual(Set(library.albums.map(\.name)), ["Hits"])
    }

    func testAlbumArtistTagGroupsOneAlbumAcrossFolders() {
        let a = makeTrack(
            "Music/A/", "x.mp3",
            RawTags(title: "One", artist: "Alpha", album: "Hits", albumArtist: "X")
        )
        let b = makeTrack(
            "Music/B/", "x.mp3",
            RawTags(title: "Two", artist: "Beta", album: "Hits", albumArtist: "X")
        )
        let library = LibraryIndex.build([a, b])
        XCTAssertEqual(library.albums.count, 1)
        XCTAssertEqual(library.albums[0].artist, "X")
        XCTAssertEqual(library.albums[0].artistKey, "x")
    }

    func testAlbumOrdersTracksByDiscThenTrackNumber() {
        let two = makeTrack(
            "Music/", "b.mp3",
            RawTags(title: "Bravo", artist: "Alpha", album: "Record", trackNumber: 2, discNumber: 1, year: 2001)
        )
        let three = makeTrack(
            "Music/", "c.mp3",
            RawTags(title: "Charlie", artist: "Alpha", album: "Record", trackNumber: 1, discNumber: 2, year: 2003)
        )
        let one = makeTrack(
            "Music/", "a.mp3",
            RawTags(title: "Alpha", artist: "Alpha", album: "Record", trackNumber: 1, discNumber: 1, year: 2001)
        )
        let library = LibraryIndex.build([two, three, one])
        XCTAssertEqual(library.albums.count, 1)
        XCTAssertEqual(library.albums[0].tracks.map(\.title), ["Alpha", "Bravo", "Charlie"])
        XCTAssertEqual(library.albums[0].year, 2003)
    }

    func testFeaturedArtistAppearsUnderEveryCreditWithDisplayNamesKept() {
        let feat = makeTrack("Music/", "1.mp3", RawTags(title: "One", artist: "Alpha feat. Beta", album: "Singles"))
        let beta = makeTrack("Music/", "2.mp3", RawTags(title: "Two", artist: "Beta", album: "Singles"))
        let library = LibraryIndex.build([feat, beta])
        XCTAssertEqual(library.artists.map(\.name), ["Alpha", "Beta"])
        XCTAssertEqual(library.artists.first { $0.name == "Alpha" }?.tracks.map(\.title), ["One"])
        XCTAssertEqual(library.artists.first { $0.name == "Beta" }?.tracks.map(\.title), ["One", "Two"])
        XCTAssertEqual(Set(library.artistByKey.keys), ["alpha", "beta"])
    }

    func testFoldersGroupByRelativePathAndSort() {
        let beta = makeTrack("Music/Beta/", "2.mp3", RawTags(title: "Two", artist: "Alpha"))
        let one = makeTrack("Music/Alpha/", "a.mp3", RawTags(title: "One", artist: "Alpha"))
        let two = makeTrack("Music/Alpha/", "b.mp3", RawTags(title: "Second", artist: "Alpha"))
        let library = LibraryIndex.build([beta, one, two])
        XCTAssertEqual(library.folders.map(\.path), ["Music/Alpha/", "Music/Beta/"])
        XCTAssertEqual(library.folders.map(\.name), ["Alpha", "Beta"])
        XCTAssertEqual(library.folders[0].tracks.map(\.fileName), ["a.mp3", "b.mp3"])
    }

    func testTracksSortByTitleThenArtist() {
        let zed = makeTrack("Music/", "z.mp3", RawTags(title: "2 Song", artist: "Zed"))
        let late = makeTrack("Music/", "a.mp3", RawTags(title: "10 Song", artist: "Abe"))
        let early = makeTrack("Music/", "b.mp3", RawTags(title: "2 Song", artist: "Abe"))
        let library = LibraryIndex.build([zed, late, early])
        XCTAssertEqual(library.tracks.map(\.title), ["2 Song", "2 Song", "10 Song"])
        XCTAssertEqual(library.tracks.map(\.artist), ["Abe", "Zed", "Abe"])
    }

    func testEveryItemIsInItsIndex() {
        let tracks = [
            makeTrack("Music/One/", "a.mp3", RawTags(title: "A Song", artist: "Alpha", album: "First", trackNumber: 1)),
            makeTrack(
                "Music/One/", "b.mp3",
                RawTags(title: "B Song", artist: "Alpha feat. Gamma", album: "First", trackNumber: 2)
            ),
            makeTrack(
                "Music/Two/", "c.mp3",
                RawTags(title: "C Song", artist: "Beta", album: "Second", albumArtist: "X")
            ),
        ]
        let library = LibraryIndex.build(tracks)
        XCTAssertEqual(Set(library.trackByKey.keys), Set(library.tracks.map(\.key)))
        XCTAssertEqual(Set(library.albumByKey.keys), Set(library.albums.map(\.key)))
        XCTAssertEqual(Set(library.artistByKey.keys), Set(library.artists.map(\.key)))
        XCTAssertEqual(Set(library.folderByPath.keys), Set(library.folders.map(\.path)))
        XCTAssertEqual(library.albumByKey[library.albums[0].key]?.name, library.albums[0].name)
        XCTAssertEqual(library.folderByPath["Music/One/"]?.tracks.count, 2)
        XCTAssertEqual(library.tracks.count, 3)
        XCTAssertEqual(library.artists.count, 3)
    }

    func testCopiesFileAndDateFields() {
        let scanned = file("Music/", "a.mp3")
        let track = LibraryIndex.makeTrack(file: scanned, tags: RawTags(), dateAdded: added)
        XCTAssertEqual(track.key, scanned.key)
        XCTAssertEqual(track.relativePath, scanned.relativePath)
        XCTAssertEqual(track.modified, scanned.modified)
        XCTAssertEqual(track.dateAdded, added)
        XCTAssertEqual(track.folder, "Music")
    }
}
