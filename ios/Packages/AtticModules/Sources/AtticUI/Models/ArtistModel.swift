import AtticCore
import SwiftUI

/// An album under its artist: the cover song and the "2025 • Album" line.
struct ArtistAlbum: Identifiable {
    let key: String
    let name: String
    let subtitle: String
    let coverTrack: Track?
    var id: String { key }
}

struct ArtistState {
    /// The artist is no longer in the library.
    let isGone: Bool
    let name: String
    let songCountText: String
    /// Up to five most played songs; empty hides the section.
    let topSongs: [Track]
    let albums: [ArtistAlbum]
    let allSongs: [Track]
    let coverTrack: Track?
    let currentKey: String?
    let isPlaying: Bool
    /// This artist is what the player is playing from.
    let isPlayingThis: Bool
    let likedSet: Set<String>
}

struct ArtistActions {
    let back: () -> Void
    let playAll: (Int) -> Void
    let playTop: (Int) -> Void
    let playPause: () -> Void
    let shuffle: () -> Void
    let addToPlaylist: () -> Void
    let openAlbum: (String) -> Void
}

@MainActor
func makeArtistModel(app: AppModel, router: Router, key: String) -> (state: ArtistState, actions: ArtistActions) {
    let artist = app.library.library.artistByKey[key]
    let name = artist?.name ?? ""
    let allSongs = artist?.tracks ?? []
    let counts = app.playCounts
    // Ties keep library order, so the list doesn't shuffle between redraws.
    let topSongs = allSongs.enumerated()
        .filter { counts[$0.element.key, default: 0] > 0 }
        .sorted {
            let a = counts[$0.element.key, default: 0], b = counts[$1.element.key, default: 0]
            return a != b ? a > b : $0.offset < $1.offset
        }
        .prefix(5)
        .map(\.element)
    let albums = (artist?.albums ?? []).map { album in
        let kind = album.tracks.count == 1 ? S.albumKindSingle : S.albumKindAlbum
        let subtitle = album.year.map { S.artistAlbumYearKind(String($0), kind) } ?? kind
        return ArtistAlbum(
            key: album.key, name: album.name, subtitle: subtitle,
            coverTrack: album.tracks.first { $0.hasArtwork } ?? album.tracks.first
        )
    }
    let player = app.player
    let state = ArtistState(
        isGone: artist == nil,
        name: name,
        songCountText: Format.songs(allSongs.count),
        topSongs: topSongs,
        albums: albums,
        allSongs: allSongs,
        coverTrack: allSongs.first { $0.hasArtwork },
        currentKey: player.state.current?.key,
        isPlaying: player.state.isPlaying,
        isPlayingThis: app.isPlayingFrom(name),
        likedSet: app.likedSet
    )
    let actions = ArtistActions(
        back: { router.pop() },
        playAll: { index in app.play(allSongs, startAt: index, context: name) },
        playTop: { index in app.play(topSongs, startAt: index, context: name) },
        playPause: {
            if app.isPlayingFrom(name) {
                player.togglePlayPause()
            } else {
                app.play(allSongs, startAt: 0, context: name)
            }
        },
        shuffle: { app.shuffle(allSongs, context: name) },
        addToPlaylist: { router.addingToPlaylist = allSongs },
        openAlbum: { albumKey in router.open(.album(key: albumKey)) }
    )
    return (state, actions)
}
