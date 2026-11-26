import AtticCore
import Foundation

enum HomeFilter: Int, CaseIterable {
    case all, albums, artists, folders
}

/// A shelf of songs: its title is also what the queue says it plays from.
struct HomeSongShelf: Identifiable {
    let title: String
    let tracks: [Track]
    var id: String { title }
}

struct HomeAlbum: Identifiable {
    let id: String
    let name: String
    let artist: String
    /// "Mira Okafor, 2025", for a hero tile.
    let heroSubtitle: String
    /// The song whose cover stands for the album.
    let art: Track?
}

struct HomeArtist: Identifiable {
    let id: String
    let name: String
    let art: Track?
}

struct HomeFolder: Identifiable {
    /// The folder's relative path.
    let id: String
    let name: String
    let songs: String
    /// Up to four songs from different albums, for the mosaic.
    let covers: [Track]
}

struct HomeState {
    var isEmpty: Bool
    /// Scanning with nothing found yet.
    var isLoading: Bool
    var filter: HomeFilter
    /// Nil when there are no favourites.
    var favouriteCount: Int?
    var heroAlbums: [HomeAlbum]
    var shelves: [HomeSongShelf]
    var startListening: [Track]
    var folders: [HomeFolder]
    var albums: [HomeAlbum]
    var topArtists: [HomeArtist]
    var likedKeys: Set<String>
    var currentKey: String?
    var isPlaying: Bool
}

struct HomeActions {
    var setFilter: (HomeFilter) -> Void
    var openSettings: () -> Void
    var openFavourites: () -> Void
    var openAlbum: (String) -> Void
    var openArtist: (String) -> Void
    var openFolder: (String) -> Void
    /// Plays a shelf from the song at an index.
    var playShelf: (HomeSongShelf, Int) -> Void
    /// Plays Start listening from the song at an index (0 for the play button).
    var playStartListening: (Int) -> Void
    var addFolder: (URL) -> Void
    var rescan: () async -> Void
}

enum HomeLimits {
    static let shelf = 12
    static let hero = 8
    static let startListening = 8
    static let onRepeatMinPlays = 3
    static let notLatelyMinPlays = 2
    /// A song played within this many recent plays is not "not lately".
    static let notLatelyWindow = 50
}

/// The keys of up to eight random songs. The screen keeps them until the song count changes, so a
/// redraw doesn't reshuffle the list.
func pickStartListening(from library: Library) -> [String] {
    library.tracks.shuffled().prefix(HomeLimits.startListening).map(\.key)
}

@MainActor
func makeHomeModel(
    app: AppModel,
    router: Router,
    filter: HomeFilter,
    startKeys: [String],
    setFilter: @escaping (HomeFilter) -> Void
) -> (state: HomeState, actions: HomeActions) {
    let library = app.library.library
    let counts = app.playCounts
    let recent = uniqueKeys(app.recentKeys)

    let heroAlbums = Array(
        library.albums.sorted { ($0.tracks.count, $1.name) > ($1.tracks.count, $0.name) }.prefix(HomeLimits.hero)
    ).map(album)

    var shelves: [HomeSongShelf] = []
    func add(_ title: String, _ tracks: [Track]) {
        if !tracks.isEmpty { shelves.append(HomeSongShelf(title: title, tracks: tracks)) }
    }
    add(S.homeRecentlyPlayed, recent.compactMap { library.trackByKey[$0] }.prefix(HomeLimits.shelf).map { $0 })
    add(S.homeOnRepeat, mostPlayed(library.tracks, counts: counts) { $0 >= HomeLimits.onRepeatMinPlays })
    let recentWindow = Set(recent.prefix(HomeLimits.notLatelyWindow))
    add(S.homeNotLately, mostPlayed(library.tracks, counts: counts, excluding: recentWindow) { $0 >= HomeLimits.notLatelyMinPlays })
    add(S.homeNewlyAdded, library.tracks.sorted { $0.dateAdded > $1.dateAdded }.prefix(HomeLimits.shelf).map { $0 })

    // Albums played most recently first, then the rest by name.
    let recentRank = Dictionary(recent.enumerated().map { ($1, $0) }, uniquingKeysWith: { a, _ in a })
    func rank(_ album: Album) -> Int { album.tracks.compactMap { recentRank[$0.key] }.min() ?? Int.max }
    let albums = library.albums
        .sorted { (rank($0), $0.name) < (rank($1), $1.name) }
        .prefix(HomeLimits.shelf).map(album)

    func plays(_ artist: Artist) -> Int { artist.tracks.reduce(0) { $0 + (counts[$1.key] ?? 0) } }
    let topArtists = library.artists
        .sorted { (plays($1), $1.tracks.count, $0.name) < (plays($0), $0.tracks.count, $1.name) }
        .prefix(HomeLimits.shelf)
        .map { HomeArtist(id: $0.key, name: $0.name, art: cover(of: $0.tracks)) }

    let folders = library.folders
        .sorted { ($0.tracks.count, $1.name) > ($1.tracks.count, $0.name) }
        .prefix(HomeLimits.shelf)
        .map { HomeFolder(id: $0.path, name: $0.name, songs: Format.songs($0.tracks.count), covers: mosaicCovers($0.tracks)) }

    let favourites = app.favourites.count
    let state = HomeState(
        isEmpty: library.isEmpty,
        isLoading: library.isEmpty && app.library.isScanning,
        filter: filter,
        favouriteCount: favourites > 0 ? favourites : nil,
        heroAlbums: heroAlbums,
        shelves: shelves,
        startListening: startKeys.compactMap { library.trackByKey[$0] },
        folders: folders,
        albums: Array(albums),
        topArtists: topArtists,
        likedKeys: app.likedSet,
        currentKey: app.player.state.current?.key,
        isPlaying: app.player.state.isPlaying
    )
    let startTracks = state.startListening
    let actions = HomeActions(
        setFilter: setFilter,
        openSettings: { router.push(.settings) },
        openFavourites: { router.open(.favourites) },
        openAlbum: { router.open(.album(key: $0)) },
        openArtist: { router.open(.artist(key: $0)) },
        openFolder: { router.open(.folder(path: $0)) },
        playShelf: { shelf, index in app.play(shelf.tracks, startAt: index, context: shelf.title) },
        playStartListening: { index in app.play(startTracks, startAt: index, context: S.homeStartListening) },
        addFolder: { url in Task { try? await app.library.addFolder(url) } },
        rescan: { await app.library.rescan() }
    )
    return (state, actions)
}

// MARK: Helpers

private func uniqueKeys(_ keys: [String]) -> [String] {
    var seen = Set<String>()
    return keys.filter { seen.insert($0).inserted }
}

/// Songs played at least as often as `qualifies` asks, most played first (ties by title).
private func mostPlayed(
    _ tracks: [Track], counts: [String: Int], excluding: Set<String> = [], qualifies: (Int) -> Bool
) -> [Track] {
    tracks
        .filter { qualifies(counts[$0.key] ?? 0) && !excluding.contains($0.key) }
        .sorted {
            let a = counts[$0.key] ?? 0, b = counts[$1.key] ?? 0
            return a != b ? a > b : $0.title.localizedStandardCompare($1.title) == .orderedAscending
        }
        .prefix(HomeLimits.shelf).map { $0 }
}

private func cover(of tracks: [Track]) -> Track? {
    tracks.first(where: \.hasArtwork) ?? tracks.first
}

private func album(_ album: Album) -> HomeAlbum {
    HomeAlbum(
        id: album.key, name: album.name, artist: album.artist,
        heroSubtitle: S.homeHeroAlbumSubtitle(album.artist, album.year.map(String.init) ?? S.homeAlbumNoYear),
        art: cover(of: album.tracks)
    )
}

/// Up to four songs with a cover, each from a different album.
private func mosaicCovers(_ tracks: [Track]) -> [Track] {
    var seen = Set<String>()
    var picked: [Track] = []
    for track in tracks where track.hasArtwork {
        guard seen.insert(track.albumKey ?? track.key).inserted else { continue }
        picked.append(track)
        if picked.count == 4 { break }
    }
    return picked
}
