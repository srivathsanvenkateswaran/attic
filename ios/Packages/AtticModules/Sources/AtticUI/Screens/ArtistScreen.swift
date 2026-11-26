import AtticCore
import AtticKit
import SwiftUI

/// An artist: the header block with a cookie cover, the most played songs, their albums, every song.
struct ArtistScreen: View {
    let key: String
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @State private var barSurface: AppBarSurface = .overArt

    var body: some View {
        let (state, actions) = makeArtistModel(app: app, router: router, key: key)
        Group {
            if state.isGone {
                gone(actions)
            } else {
                content(state, actions)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: Gone

    private func gone(_ actions: ArtistActions) -> some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                // 72pt under the bar (DESIGN.md, Empty and loading states).
                Spacer().frame(height: AtticSpacing.appBarHeight + 72)
                EmptyState(title: S.collectionArtistGone, body: S.collectionGoneBody, symbol: .person) {
                    AtticButton(S.back, variant: .tonal, leading: .arrowBack, action: actions.back)
                }
                Spacer(minLength: 0)
            }
            DetailAppBar(S.collectionArtistGone, surface: .plain, onBack: actions.back)
        }
    }

    // MARK: Artist

    private func content(_ state: ArtistState, _ actions: ArtistActions) -> some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    CollectionHeader(
                        title: state.name,
                        isArtist: true,
                        meta: [state.songCountText],
                        isPlaying: state.isPlayingThis,
                        isEnabled: !state.allSongs.isEmpty,
                        onPlay: actions.playPause,
                        onShuffle: actions.shuffle,
                        onAdd: actions.addToPlaylist
                    ) {
                        ArtworkImage(track: state.coverTrack, maxPixel: 480) { image in
                            Cover(image, shape: .artist, fallback: .person)
                        }
                    }
                    .fromCover(state.coverTrack)

                    if !state.topSongs.isEmpty {
                        SectionHeader(S.artistTopSongs)
                        ListGroup {
                            ForEach(Array(state.topSongs.enumerated()), id: \.element.key) { index, track in
                                ArtworkImage(track: track, maxPixel: 160) { image in
                                    songRow(track, lead: .rankedCover(index + 1, image), subtitle: track.album ?? "", state: state) {
                                        actions.playTop(index)
                                    }
                                }
                            }
                        }
                    }

                    if !state.albums.isEmpty {
                        SectionHeader(S.artistAlbums)
                        Shelf {
                            ForEach(state.albums) { album in
                                ArtworkImage(track: album.coverTrack, maxPixel: 300) { image in
                                    Tile(title: album.name, subtitle: album.subtitle, action: { actions.openAlbum(album.key) }) {
                                        Cover(image)
                                    }
                                }
                            }
                        }
                    }

                    SectionHeader(S.artistAllSongs)
                    LazyVStack(spacing: 0) {
                        ForEach(Array(state.allSongs.enumerated()), id: \.element.key) { index, track in
                            ArtworkImage(track: track, maxPixel: 160) { image in
                                songRow(track, lead: .cover(image), subtitle: track.album ?? "", state: state) {
                                    actions.playAll(index)
                                }
                            }
                            .groupCell(index: index, count: state.allSongs.count)
                        }
                    }

                    Color.clear.frame(height: AtticSpacing.dockClearance)
                }
            }
            // The header's text starts 68pt below the status bar, so the scroll view keeps the top
            // safe area as an inset; the header block paints behind the status bar itself.
            .onScrollFraction { fraction in
                let surface: AppBarSurface = fraction >= 1 ? .scrolled : .overArt
                if surface != barSurface { barSurface = surface }
            }

            DetailAppBar(state.name, surface: barSurface, onBack: actions.back)
                .fromCover(state.coverTrack)
        }
    }

    private func songRow(
        _ track: Track, lead: SongRowLead, subtitle: String, state: ArtistState, action: @escaping () -> Void
    ) -> some View {
        SongRow(
            title: track.title,
            subtitle: subtitle,
            lead: lead,
            isCurrent: state.currentKey == track.key,
            isPlaying: state.isPlaying,
            isLiked: state.likedSet.contains(track.key),
            action: action
        ) {
            RowMoreMenu(title: track.title) { SongMenuItems(track: track) }
        }
        .contextMenu { SongMenuItems(track: track) }
    }
}
