import AtticCore
import AtticKit
import Foundation

/// Which collection a screen shows. Album, playlist, folder and Favourites share one model and one view.
enum CollectionKind: Hashable {
    case album(key: String)
    case playlist(id: UUID)
    case folder(path: String)
    case favourites
}

struct CollectionState {
    var kind: CollectionKind
    var title: String
    /// An album's artist.
    var byline: String?
    /// The song whose cover goes beside the byline.
    var bylineTrack: Track?
    /// Lines under the title.
    var meta: [String]
    var tracks: [Track]
    /// For each shown song, its place among the playlist's entries. They differ when an entry's file is
    /// gone from the library, because gone entries are kept but not shown.
    var entryIndices: [Int]
    /// The album, playlist or folder is no longer in the library.
    var isGone: Bool
    /// The collection is missing while a scan runs, so it may still turn up.
    var isLoading: Bool
    var goneTitle: String
    var goneSymbol: AtticSymbol
    /// The songs a mosaic draws: up to four with art, from different albums first.
    var coverTracks: [Track]
    /// An album's cover: its first song.
    var coverTrack: Track?
    /// The song whose colours the header takes. Nil keeps the app scheme (Favourites, no art).
    var seedTrack: Track?
    /// This collection is what is playing, and it is playing.
    var isPlayingThis: Bool
    var isShuffleOn: Bool
    var currentKey: String?
    /// Something is playing, in this collection or not.
    var isPlaying: Bool
    /// A user playlist: it can be renamed, deleted and reordered.
    var isEditable: Bool
    var playlistID: UUID?
    var isEmpty: Bool
}

struct CollectionActions {
    var back: () -> Void
    var play: (_ index: Int) -> Void
    var playOrPause: () -> Void
    var shuffle: () -> Void
    var addToPlaylist: () -> Void
    /// Nil when the album's artist is not in the library.
    var openByline: (() -> Void)?
    var rename: (_ name: String) -> Void
    var delete: () -> Void
    /// Moves a shown song to another place among the shown songs (`to` is its place afterwards).
    var move: (_ from: Int, _ to: Int) -> Void
    var addSongs: () -> Void
}

@MainActor
func makeCollectionModel(
    app: AppModel, router: Router, kind: CollectionKind
) -> (state: CollectionState, actions: CollectionActions) {
    let library = app.library.library
    let player = app.player.state

    var title = ""
    var byline: String?
    var bylineTrack: Track?
    var artistKey: String?
    var meta: [String] = []
    var tracks: [Track] = []
    var entryIndices: [Int] = []
    var found = true
    var goneTitle = S.collectionPlaylistGone
    var goneSymbol = AtticSymbol.queueMusic
    var coverTrack: Track?
    var playlistID: UUID?

    switch kind {
    case .album(let key):
        goneTitle = S.collectionAlbumGone
        goneSymbol = .album
        if let album = library.albumByKey[key] {
            title = album.name
            tracks = album.tracks
            byline = album.artist
            if let artist = album.artistKey.flatMap({ library.artistByKey[$0] }) {
                artistKey = artist.key
                bylineTrack = artist.tracks.first(where: \.hasArtwork) ?? artist.tracks.first
            } else {
                bylineTrack = album.tracks.first
            }
            coverTrack = album.tracks.first
            let kindName = album.tracks.count == 1 ? S.albumKindSingle : S.albumKindAlbum
            meta = [
                album.year.map { S.albumKindYear(kindName, String($0)) } ?? kindName,
                Format.songsAndLength(tracks),
            ]
        } else {
            found = false
        }
    case .playlist(let id):
        if let list = app.playlists.first(where: { $0.id == id }) {
            playlistID = id
            title = list.name
            for (index, key) in list.trackKeys.enumerated() {
                if let track = library.trackByKey[key] {
                    tracks.append(track)
                    entryIndices.append(index)
                }
            }
            meta = [
                S.libraryPlaylistSubtitle(count: tracks.count, S.grouped(tracks.count)),
                Format.total(ms: tracks.reduce(0) { $0 + $1.durationMs }),
            ]
        } else {
            found = false
        }
    case .folder(let path):
        if let folder = library.folderByPath[path] {
            title = folder.name
            tracks = folder.tracks
            meta = [S.folderSubtitle, Format.songsAndLength(tracks)]
        } else {
            found = false
        }
    case .favourites:
        title = S.favourites
        tracks = app.favourites
        meta = [Format.songsAndLength(tracks)]
    }
    if entryIndices.isEmpty { entryIndices = Array(tracks.indices) }

    let isThis = found && app.isPlayingFrom(title)
    let isPlayingThis = isThis && player.isPlaying
    let withArt = tracks.filter(\.hasArtwork)
    let seedTrack: Track? = if case .favourites = kind { nil } else { withArt.first }
    let isEditable = playlistID != nil

    let state = CollectionState(
        kind: kind,
        title: title,
        byline: byline,
        bylineTrack: bylineTrack,
        meta: meta,
        tracks: tracks,
        entryIndices: entryIndices,
        isGone: !found,
        isLoading: !found && app.library.isScanning,
        goneTitle: goneTitle,
        goneSymbol: goneSymbol,
        coverTracks: mosaicTracks(withArt),
        coverTrack: coverTrack,
        seedTrack: seedTrack,
        isPlayingThis: isPlayingThis,
        isShuffleOn: isThis && player.shuffle,
        currentKey: player.current?.key,
        isPlaying: player.isPlaying,
        isEditable: isEditable,
        playlistID: playlistID,
        isEmpty: tracks.isEmpty
    )

    let actions = CollectionActions(
        back: { router.pop() },
        play: { index in app.play(tracks, startAt: index, context: title) },
        playOrPause: {
            // A paused collection resumes; anything else starts from the top.
            if isThis { app.player.togglePlayPause() } else { app.play(tracks, startAt: 0, context: title, shuffle: false) }
        },
        shuffle: { app.shuffle(tracks, context: title) },
        addToPlaylist: { router.addingToPlaylist = tracks },
        openByline: artistKey.map { key in { router.open(.artist(key: key)) } },
        rename: { name in
            let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let playlistID, !trimmed.isEmpty else { return }
            app.renamePlaylist(playlistID, name: trimmed)
        },
        delete: {
            guard let playlistID else { return }
            app.deletePlaylist(playlistID)
            router.pop()
        },
        move: { from, to in
            guard let playlistID, from != to,
                  entryIndices.indices.contains(from), entryIndices.indices.contains(to) else { return }
            app.movePlaylistEntry(playlistID, from: entryIndices[from], to: entryIndices[to])
        },
        addSongs: { router.tab = .search }
    )
    return (state, actions)
}

/// Up to four covers for a mosaic: one song per album first, then more from the same albums.
private func mosaicTracks(_ withArt: [Track]) -> [Track] {
    var seen = Set<String>()
    var picked: [Track] = []
    for track in withArt where picked.count < 4 {
        if seen.insert(track.albumKey ?? track.key).inserted { picked.append(track) }
    }
    if picked.count < 4 {
        let have = Set(picked.map(\.key))
        for track in withArt where picked.count < 4 && !have.contains(track.key) { picked.append(track) }
    }
    return picked
}
