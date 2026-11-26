import AtticCore
import AtticKit
import SwiftUI

struct SearchScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme

    @State private var query = ""
    @State private var debounced = ""
    @State private var results = SearchResults.empty
    @State private var scrolled: CGFloat = 0
    @FocusState private var fieldFocused: Bool

    var body: some View {
        let model = makeSearchModel(
            app: app,
            router: router,
            query: query,
            isDocked: fieldFocused || !query.isEmpty,
            results: results,
            clear: {
                query = ""
                results = .empty
                fieldFocused = false
            }
        )
        let search = model.actions.search

        VStack(spacing: 0) {
            if model.state.showsBrowse {
                RootAppBar(S.searchTitle, scrolled: scrolled) {
                    AtticIconButton(.settings, label: S.settings) { model.actions.openSettings() }
                }
            } else {
                // DESIGN.md, Search: the docked field sits 8pt under the status bar.
                Spacer(minLength: 0).frame(height: AtticSpacing.s)
            }

            SearchField(
                text: $query,
                placeholder: S.searchPlaceholder,
                focus: $fieldFocused,
                onBack: model.state.showsBrowse ? nil : model.actions.clear
            )
            .padding(.horizontal, AtticSpacing.l)

            if model.state.showsBrowse {
                browse(model.state, model.actions)
            } else if model.state.showsResults {
                found(model.state, model.actions)
            } else if model.state.showsEmptyState {
                EmptyState(title: S.searchNoResultsTitle(query), body: S.searchNoResultsBody, symbol: .searchOff)
            }
        }
        .background(scheme.surface.ignoresSafeArea(edges: .top))
        .toolbar(.hidden, for: .navigationBar)
        .task(id: query) {
            try? await Task.sleep(for: .milliseconds(150))
            debounced = query
        }
        .task(id: debounced) {
            results = debounced.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .empty : await search(debounced)
        }
    }

    @ViewBuilder
    private func browse(_ state: SearchState, _ actions: SearchActions) -> some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                SectionHeader(S.searchBrowse)
                ListLabel(S.homeAlbums)
                Shelf {
                    ForEach(state.browseAlbums) { album in
                        ArtworkImage(track: album.tracks.first, maxPixel: 240) { image in
                            Tile(
                                title: album.name, subtitle: album.artist, width: 120,
                                action: { actions.open(.album(key: album.key)) }
                            ) {
                                Cover(image)
                            }
                        }
                    }
                }
                ListLabel(S.libraryFilterArtists)
                Shelf {
                    ForEach(state.browseArtists) { artist in
                        ArtworkImage(track: artist.tracks.first, maxPixel: 240) { image in
                            Tile(
                                title: artist.name, subtitle: Format.songs(artist.tracks.count), width: 120,
                                isArtist: true, action: { actions.open(.artist(key: artist.key)) }
                            ) {
                                Cover(image, shape: .artist)
                            }
                        }
                    }
                }
            }
        }
        .onScrollFraction { scrolled = $0 }
        .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
    }

    @ViewBuilder
    private func found(_ state: SearchState, _ actions: SearchActions) -> some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(Array(state.results.collections.enumerated()), id: \.element.id) { index, hit in
                    ArtworkImage(track: hit.coverTrack, maxPixel: 112) { image in
                        CollectionRow(title: hit.title, subtitle: hit.subtitle, action: { actions.open(hit.destination) }) {
                            switch hit.kind {
                            case .artist: Cover(image, shape: .artist)
                            case .album: Cover(image)
                            case .playlist: Mosaic(image.map { [$0] } ?? [])
                            }
                        }
                        .groupCell(index: index, count: state.results.collections.count)
                    }
                }
                if !state.results.songs.isEmpty {
                    ListLabel(S.libraryFilterSongs)
                    ForEach(Array(state.results.songs.enumerated()), id: \.element.id) { index, hit in
                        ArtworkImage(track: hit.track, maxPixel: 96) { image in
                            SongRow(
                                title: hit.track.title,
                                subtitle: hit.track.artist,
                                lead: .cover(image),
                                isCurrent: state.currentKey == hit.track.key,
                                isPlaying: state.isPlaying,
                                isLiked: hit.isLiked,
                                action: { actions.play(hit.track) }
                            ) {
                                RowMoreMenu(title: hit.track.title) { SongMenuItems(track: hit.track) }
                            }
                            .contextMenu { SongMenuItems(track: hit.track) }
                            .groupCell(index: index, count: state.results.songs.count)
                        }
                    }
                }
            }
        }
        .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
    }
}
