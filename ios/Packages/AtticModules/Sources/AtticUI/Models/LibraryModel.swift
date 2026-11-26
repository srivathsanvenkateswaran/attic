import AtticCore
import SwiftUI

/// The four chips above the library. Songs lists every song; the others show collections.
enum LibraryFilter: String, CaseIterable, Sendable {
    case playlists, albums, artists, songs

    var title: String {
        switch self {
        case .playlists: S.libraryFilterPlaylists
        case .albums: S.libraryFilterAlbums
        case .artists: S.libraryFilterArtists
        case .songs: S.libraryFilterSongs
        }
    }
}

/// The order of the Songs list. The raw value is what the screen persists.
enum SongSort: String, CaseIterable, Sendable {
    case title, artist, recentlyAdded

    var next: SongSort {
        switch self {
        case .title: .artist
        case .artist: .recentlyAdded
        case .recentlyAdded: .title
        }
    }

    var title: String {
        switch self {
        case .title: S.librarySortTitle
        case .artist: S.librarySortArtist
        case .recentlyAdded: S.librarySortRecentlyAdded
        }
    }
}

enum LibraryItemKind: Sendable {
    case favourites, playlist, folder, artist, album
}

/// One row or tile. The art tracks carry the covers: one for a cover, up to four for a mosaic.
struct LibraryItem: Identifiable, Sendable {
    let id: String
    let title: String
    let subtitle: String
    let kind: LibraryItemKind
    let destination: Destination
    let artTracks: [Track]
    let songCount: Int
}

struct LibrarySection: Identifiable, Sendable {
    let id: String
    /// The label above the group, if it has one.
    let label: String?
    let items: [LibraryItem]
}

struct LibraryState: Sendable {
    let filter: LibraryFilter?
    let sort: SongSort
    let grid: Bool
    let isCreating: Bool
    let songs: [Track]
    let sections: [LibrarySection]
    let currentKey: String?
    let isPlaying: Bool
    let likedKeys: Set<String>
    let isEmpty: Bool
}

struct LibraryActions {
    let toggleFilter: (LibraryFilter) -> Void
    let cycleSort: () -> Void
    let setSort: (SongSort) -> Void
    let toggleGrid: () -> Void
    let openSettings: () -> Void
    let openSearch: () -> Void
    let open: (LibraryItem) -> Void
    let startCreating: () -> Void
    let dismissCreating: () -> Void
    /// Creates the playlist and opens it.
    let create: (String) -> Void
    let shuffleAll: () -> Void
    let play: (Track) -> Void
}

/// The bindings are the screen's `@State` and `@AppStorage`, so the choices survive a restart
/// while the sorting and grouping stay here.
@MainActor
func makeLibraryModel(
    app: AppModel,
    router: Router,
    filter: Binding<LibraryFilter?>,
    grid: Binding<Bool>,
    sort: Binding<SongSort>,
    creating: Binding<Bool>
) -> (state: LibraryState, actions: LibraryActions) {
    let library = app.library.library
    let tracks = library.tracks
    let currentFilter = filter.wrappedValue
    let currentSort = sort.wrappedValue

    let ordered: [Track]
    switch currentSort {
    case .title:
        ordered = tracks
    case .artist:
        ordered = tracks.sorted { lhs, rhs in
            let byArtist = lhs.artist.localizedCaseInsensitiveCompare(rhs.artist)
            if byArtist != .orderedSame { return byArtist == .orderedAscending }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }
    case .recentlyAdded:
        ordered = tracks.sorted { $0.dateAdded > $1.dateAdded }
    }

    // Most recently played first, then the name for anything never played.
    var rank: [String: Int] = [:]
    for (index, key) in app.recentKeys.enumerated() where rank[key] == nil {
        rank[key] = index
    }
    func recency(_ tracks: [Track]) -> Int {
        tracks.map { rank[$0.key] ?? Int.max }.min() ?? Int.max
    }
    func byRecency(_ lhs: (String, Int), _ rhs: (String, Int)) -> Bool {
        if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }
        return lhs.0.localizedCaseInsensitiveCompare(rhs.0) == .orderedAscending
    }

    // Covers from different albums, so a mosaic does not repeat one picture four times.
    func mosaicTracks(_ tracks: [Track]) -> [Track] {
        var seen: Set<String> = []
        var chosen: [Track] = []
        for track in tracks {
            guard seen.insert(track.albumKey ?? track.key).inserted else { continue }
            chosen.append(track)
            if chosen.count == 4 { break }
        }
        return chosen
    }

    func playlistSubtitle(_ count: Int) -> String {
        S.libraryPlaylistSubtitle(count: count, Format.songs(count))
    }

    let likedCount = app.favourites.count
    let favourites = LibraryItem(
        id: "liked",
        title: S.favourites,
        subtitle: playlistSubtitle(likedCount),
        kind: .favourites,
        destination: .favourites,
        artTracks: [],
        songCount: likedCount
    )

    let playlists = app.playlists.map { playlist in
        let tracks = app.tracks(of: playlist)
        return LibraryItem(
            id: "p\(playlist.id.uuidString)",
            title: playlist.name,
            subtitle: playlistSubtitle(tracks.count),
            kind: .playlist,
            destination: .playlist(id: playlist.id),
            artTracks: mosaicTracks(tracks),
            songCount: tracks.count
        )
    }

    let folders = library.folders.map { folder in
        LibraryItem(
            id: "f\(folder.path)",
            title: folder.name,
            subtitle: S.folderSubtitle,
            kind: .folder,
            destination: .folder(path: folder.path),
            artTracks: mosaicTracks(folder.tracks),
            songCount: folder.tracks.count
        )
    }

    let albumItems = library.albums.sorted { byRecency(($0.name, recency($0.tracks)), ($1.name, recency($1.tracks))) }
        .map { album in
            LibraryItem(
                id: "a\(album.key)",
                title: album.name,
                subtitle: S.libraryAlbumSubtitle(album.artist),
                kind: .album,
                destination: .album(key: album.key),
                artTracks: Array(album.tracks.prefix(1)),
                songCount: album.tracks.count
            )
        }

    let artistItems = library.artists.sorted { byRecency(($0.name, recency($0.tracks)), ($1.name, recency($1.tracks))) }
        .map { artist in
            LibraryItem(
                id: "r\(artist.key)",
                title: artist.name,
                subtitle: S.libraryArtistSubtitle,
                kind: .artist,
                destination: .artist(key: artist.key),
                artTracks: Array(artist.tracks.prefix(1)),
                songCount: artist.tracks.count
            )
        }

    let sections: [LibrarySection]
    switch currentFilter {
    case .playlists:
        sections = [LibrarySection(id: "collections", label: nil, items: [favourites] + playlists + folders)]
    case .albums:
        sections = [LibrarySection(id: "albums", label: nil, items: albumItems)]
    case .artists:
        sections = [LibrarySection(id: "artists", label: nil, items: artistItems)]
    case .songs:
        sections = []
    case nil:
        sections = [
            LibrarySection(id: "collections", label: nil, items: [favourites] + playlists + folders),
            LibrarySection(id: "artists", label: S.libraryFilterArtists, items: artistItems),
            LibrarySection(id: "albums", label: S.libraryFilterAlbums, items: albumItems),
        ].filter { !$0.items.isEmpty }
    }

    let state = LibraryState(
        filter: currentFilter,
        sort: currentSort,
        grid: grid.wrappedValue,
        isCreating: creating.wrappedValue,
        songs: ordered,
        sections: sections,
        currentKey: app.player.state.current?.key,
        isPlaying: app.player.state.isPlaying,
        likedKeys: app.likedSet,
        isEmpty: library.isEmpty
    )

    let actions = LibraryActions(
        toggleFilter: { option in filter.wrappedValue = filter.wrappedValue == option ? nil : option },
        cycleSort: { sort.wrappedValue = sort.wrappedValue.next },
        setSort: { sort.wrappedValue = $0 },
        toggleGrid: { grid.wrappedValue.toggle() },
        openSettings: { router.push(.settings) },
        openSearch: { router.tab = .search },
        open: { router.open($0.destination) },
        startCreating: { creating.wrappedValue = true },
        dismissCreating: { creating.wrappedValue = false },
        create: { name in
            creating.wrappedValue = false
            let playlist = app.createPlaylist(name: name)
            router.open(.playlist(id: playlist.id))
        },
        shuffleAll: { app.shuffle(tracks, context: nil) },
        play: { track in
            guard let index = ordered.firstIndex(of: track) else { return }
            app.play(ordered, startAt: index, context: nil)
        }
    )

    return (state, actions)
}
