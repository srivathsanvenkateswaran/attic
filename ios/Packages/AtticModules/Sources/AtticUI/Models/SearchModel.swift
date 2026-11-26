import AtticCore
import Foundation

// MARK: - Plain state

/// One collection in the results group. The cover comes from a song in the collection.
struct SearchCollectionHit: Identifiable, Sendable {
    enum Kind: Sendable { case artist, album, playlist }

    let id: String
    let title: String
    let subtitle: String
    let kind: Kind
    let destination: Destination
    let coverTrack: Track?
}

struct SearchSongHit: Identifiable, Sendable {
    var id: String { track.key }
    let track: Track
    let isLiked: Bool
}

struct SearchResults: Sendable {
    var collections: [SearchCollectionHit] = []
    var songs: [SearchSongHit] = []

    static let empty = SearchResults()

    var isEmpty: Bool { collections.isEmpty && songs.isEmpty }
}

struct SearchState: Sendable {
    let query: String
    let isDocked: Bool
    let browseAlbums: [Album]
    let browseArtists: [Artist]
    let results: SearchResults
    let currentKey: String?
    let isPlaying: Bool

    var showsBrowse: Bool { !isDocked }
    var showsResults: Bool { isDocked && !query.isEmpty && !results.isEmpty }
    var showsEmptyState: Bool { isDocked && !query.isEmpty && results.isEmpty }
}

struct SearchActions {
    /// Matches off the main thread and returns to the caller on the main actor.
    let search: @MainActor (String) async -> SearchResults
    let open: (Destination) -> Void
    let play: (Track) -> Void
    let clear: () -> Void
    let openSettings: () -> Void
}

// MARK: - Matching

private enum SearchMatching {
    static func find(
        tracks: [Track],
        artists: [Artist],
        albums: [Album],
        playlists: [Playlist],
        query: String
    ) -> SearchResults {
        let normalized = TrackKey.normalize(query)
        guard !normalized.isEmpty else { return .empty }
        let words = normalized.split(separator: " ").map(String.init)
        let trackByKey = Dictionary(tracks.map { ($0.key, $0) }, uniquingKeysWith: { first, _ in first })

        let foundTracks = ranked(tracks, words: words, normalized: normalized) { track in
            (track.title, [track.artist, track.album ?? ""])
        }
        let foundArtists = ranked(artists, words: words, normalized: normalized) { ($0.name, []) }
        let foundAlbums = ranked(albums, words: words, normalized: normalized) { ($0.name, [$0.artist]) }
        let foundPlaylists = ranked(playlists, words: words, normalized: normalized) { ($0.name, []) }

        var collections: [SearchCollectionHit] = []
        collections += foundArtists.prefix(10).map { artist in
            SearchCollectionHit(
                id: "artist:\(artist.key)", title: artist.name, subtitle: S.searchResultArtist,
                kind: .artist, destination: .artist(key: artist.key), coverTrack: artist.tracks.first
            )
        }
        collections += foundAlbums.prefix(10).map { album in
            SearchCollectionHit(
                id: "album:\(album.key)", title: album.name, subtitle: S.searchResultAlbum(album.artist),
                kind: .album, destination: .album(key: album.key), coverTrack: album.tracks.first
            )
        }
        collections += foundPlaylists.prefix(10).map { playlist in
            SearchCollectionHit(
                id: "playlist:\(playlist.id.uuidString)", title: playlist.name, subtitle: S.searchResultPlaylist,
                kind: .playlist, destination: .playlist(id: playlist.id),
                coverTrack: playlist.trackKeys.first.flatMap { trackByKey[$0] }
            )
        }

        return SearchResults(
            collections: collections,
            songs: foundTracks.prefix(100).map { SearchSongHit(track: $0, isLiked: false) }
        )
    }

    /// Exact name, then a name that starts with the query, then library order.
    private static func ranked<T>(
        _ items: [T],
        words: [String],
        normalized: String,
        fields: (T) -> (String, [String])
    ) -> [T] {
        var hits: [(rank: Int, index: Int, item: T)] = []
        for (index, item) in items.enumerated() {
            let (name, secondary) = fields(item)
            let primary = TrackKey.normalize(name)
            let keys = [primary] + secondary.map(TrackKey.normalize)
            guard matches(words: words, keys: keys, primary: primary, normalized: normalized) else { continue }
            let rank = primary == normalized ? 0 : (primary.hasPrefix(normalized) ? 1 : 2)
            hits.append((rank, index, item))
        }
        return hits
            .sorted { $0.rank != $1.rank ? $0.rank < $1.rank : $0.index < $1.index }
            .map(\.item)
    }

    /// Every query word starts a word in one of the fields, or the whole query runs through the name.
    private static func matches(words: [String], keys: [String], primary: String, normalized: String) -> Bool {
        let fieldWords = keys.map { $0.split(separator: " ").map(String.init) }
        let allWordsMatch = words.allSatisfy { word in
            fieldWords.contains { $0.contains { $0.hasPrefix(word) } }
        }
        return allWordsMatch || primary.contains(normalized)
    }
}

/// The biggest collections for Browse, keeping library order when counts tie.
private func mostSongs<T>(_ items: [T], count: (T) -> Int, limit: Int) -> [T] {
    items.enumerated()
        .sorted { lhs, rhs in
            let a = count(lhs.element), b = count(rhs.element)
            return a != b ? a > b : lhs.offset < rhs.offset
        }
        .prefix(limit)
        .map(\.element)
}

// MARK: - Model

@MainActor
func makeSearchModel(
    app: AppModel,
    router: Router,
    query: String,
    isDocked: Bool,
    results: SearchResults,
    clear: @escaping () -> Void
) -> (state: SearchState, actions: SearchActions) {
    let library = app.library.library
    let state = SearchState(
        query: query,
        isDocked: isDocked,
        browseAlbums: mostSongs(library.albums, count: { $0.tracks.count }, limit: 20),
        browseArtists: mostSongs(library.artists, count: { $0.tracks.count }, limit: 20),
        results: SearchResults(
            collections: results.collections,
            songs: results.songs.map { SearchSongHit(track: $0.track, isLiked: app.likedSet.contains($0.track.key)) }
        ),
        currentKey: app.player.state.current?.key,
        isPlaying: app.player.state.isPlaying
    )

    let search: @MainActor (String) async -> SearchResults = { text in
        let tracks = app.library.library.tracks
        let artists = app.library.library.artists
        let albums = app.library.library.albums
        let playlists = app.playlists
        return await Task.detached(priority: .userInitiated) {
            SearchMatching.find(tracks: tracks, artists: artists, albums: albums, playlists: playlists, query: text)
        }.value
    }

    let actions = SearchActions(
        search: search,
        open: { router.open($0) },
        play: { track in
            let songs = results.songs.map(\.track)
            app.play(songs, startAt: songs.firstIndex(of: track) ?? 0, context: nil)
        },
        clear: clear,
        openSettings: { router.push(.settings) }
    )
    return (state, actions)
}
