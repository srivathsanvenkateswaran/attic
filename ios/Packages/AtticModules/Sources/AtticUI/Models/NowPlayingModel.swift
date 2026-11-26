import AtticCore
import AtticKit
import Foundation

/// What a queue's context name names, for the glyph in Now Playing's header.
enum ContextKind: Equatable {
    case album, artist, folder, playlist, favourites, other
}

/// Works out what kind of collection `name` is: the player keeps only the name. A name the song
/// itself belongs to wins (its album, one of its artists, its folder), then the user's playlists.
func contextKindOf(name: String?, track: Track?, library: Library, playlists: [String]) -> ContextKind {
    guard let name else { return .other }
    if name == S.favourites { return .favourites }
    if let track {
        let album = track.albumKey.flatMap { library.albumByKey[$0]?.name } ?? track.album
        if album == name { return .album }
        if track.artistKeys.contains(where: { library.artistByKey[$0]?.name == name }) { return .artist }
        if track.folder == name { return .folder }
    }
    if playlists.contains(name) { return .playlist }
    return .other
}

struct NowPlayingState {
    /// Nil once nothing is queued; the screen draws nothing then.
    let track: Track?
    /// The text under "Playing from": the context's name, or "Your library" when there is none.
    let contextName: String
    let contextKind: ContextKind
    let isPlaying: Bool
    let shuffle: Bool
    let repeatMode: AtticRepeatMode
    let isLiked: Bool
    let durationMs: Int64
    /// The song's synced lyrics, empty when it has none (or they are not loaded yet).
    let syncedLines: [AtticCore.LyricLine]
    /// The song's first artist that is in the library, for the artist button.
    let artistKey: String?

    var hasSyncedLyrics: Bool { !syncedLines.isEmpty }

    /// The line being sung at `positionMs`, nil before the first line or when it is blank.
    func lyricLine(atMs positionMs: Int64) -> String? {
        guard let index = LRCParser.activeIndex(in: syncedLines, at: positionMs) else { return nil }
        let text = syncedLines[index].text
        return text.isEmpty ? nil : text
    }
}

struct NowPlayingActions {
    let close: () -> Void
    let openQueue: () -> Void
    let openLyrics: () -> Void
    let toggleLike: () -> Void
    let togglePlayPause: () -> Void
    let previous: () -> Void
    let next: () -> Void
    let toggleShuffle: () -> Void
    let cycleRepeat: () -> Void
    let seek: (Int64) -> Void
    /// Nil when the song's artist is not in the library.
    let openArtist: (() -> Void)?
    /// Where the song is now, in ms; a view polls it.
    let positionMs: () -> Int64
}

/// `lyrics` is the current song's lyrics as the screen loaded them (`app.lyrics(for:)`).
@MainActor
func makeNowPlayingModel(
    app: AppModel, router: Router, lyrics: LyricsDocument?
) -> (state: NowPlayingState, actions: NowPlayingActions) {
    let player = app.player.state
    let track = player.current
    let library = app.library.library

    let kind = contextKindOf(
        name: player.contextName, track: track, library: library, playlists: app.playlists.map(\.name)
    )
    let contextName: String
    if let name = player.contextName {
        contextName = kind == .favourites ? S.contextFavourites : name
    } else {
        contextName = S.contextYourLibrary
    }

    var lines: [AtticCore.LyricLine] = []
    if case .synced(let synced)? = lyrics { lines = synced }

    let artistKey = track?.artistKeys.first { library.artistByKey[$0] != nil }
    let repeatMode: AtticRepeatMode = switch player.repeatMode {
    case .off: .off
    case .all: .all
    case .one: .one
    }

    let state = NowPlayingState(
        track: track,
        contextName: contextName,
        contextKind: kind,
        isPlaying: player.isPlaying,
        shuffle: player.shuffle,
        repeatMode: repeatMode,
        isLiked: app.isLiked(track),
        durationMs: player.durationMs > 0 ? player.durationMs : (track?.durationMs ?? 0),
        syncedLines: lines,
        artistKey: artistKey
    )
    let actions = NowPlayingActions(
        close: { router.showNowPlaying = false },
        openQueue: { router.showQueue = true },
        openLyrics: { router.showLyrics = true },
        toggleLike: { if let track { app.toggleLike(track) } },
        togglePlayPause: { app.player.togglePlayPause() },
        previous: { app.player.previous() },
        next: { app.player.next() },
        toggleShuffle: { app.player.toggleShuffle() },
        cycleRepeat: { app.player.cycleRepeat() },
        seek: { app.seek(toMs: $0) },
        openArtist: artistKey.map { key in { router.open(.artist(key: key)) } },
        positionMs: { app.player.positionMs() }
    )
    return (state, actions)
}
