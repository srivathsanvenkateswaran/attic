import AtticCore
import AtticKit
import Observation
import SwiftUI
import UniformTypeIdentifiers

/// Home: the hero covers, shelves built from what you play, and a filter for albums, artists and folders.
/// Specified in ios/DESIGN.md ("Home").
struct HomeScreen: View {

    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme

    @State private var filter = HomeFilter.all
    /// Keys of the Start listening songs, redrawn only when the library's song count changes.
    @State private var startKeys: [String] = []
    @State private var scroll = HomeScroll()
    @State private var showImporter = false

    var body: some View {
        let (state, actions) = makeHomeModel(
            app: app, router: router, filter: filter, startKeys: startKeys, setFilter: { filter = $0 }
        )
        VStack(spacing: 0) {
            HomeBar(scroll: scroll, openSettings: actions.openSettings)
            if state.isLoading {
                LoadingState()
            } else if state.isEmpty {
                emptyState(actions)
            } else {
                list(state, actions)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(scheme.surface.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .onChange(of: app.library.library.tracks.count, initial: true) {
            startKeys = pickStartListening(from: app.library.library)
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.folder]) { result in
            if case .success(let url) = result { actions.addFolder(url) }
        }
    }

    // MARK: Empty

    private func emptyState(_ actions: HomeActions) -> some View {
        ScrollView {
            EmptyState(title: S.homeEmptyTitle, body: S.homeEmptyBody, symbol: .musicOff, clover: true) {
                AtticButton(S.settingsFoldersAdd, variant: .tonal) { showImporter = true }
            }
            .padding(.top, 64 - 48) // DESIGN.md, Home: 64pt under the bar; the state's own padding is 48
        }
        .refreshable { await actions.rescan() }
        .onScrollFraction { scroll.fraction = $0 }
    }

    // MARK: Library

    private func list(_ state: HomeState, _ actions: HomeActions) -> some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ConnectedButtonGroup(
                    [S.homeFilterAll, S.homeFilterAlbums, S.homeFilterArtists, S.homeFilterFolders],
                    selection: state.filter.rawValue
                ) { actions.setFilter(HomeFilter(rawValue: $0) ?? .all) }
                switch state.filter {
                case .all: allSections(state, actions)
                case .albums: albumGrid(state, actions)
                case .artists: artistGrid(state, actions)
                case .folders: folderGrid(state, actions)
                }
            }
        }
        .scrollIndicators(.hidden)
        .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
        .refreshable { await actions.rescan() }
        .onScrollFraction { scroll.fraction = $0 }
    }

    @ViewBuilder
    private func allSections(_ state: HomeState, _ actions: HomeActions) -> some View {
        hero(state, actions)
        ForEach(state.shelves) { shelf in
            SectionHeader(shelf.title)
            Shelf {
                ForEach(Array(shelf.tracks.enumerated()), id: \.element.key) { index, track in
                    songTile(track, state) { actions.playShelf(shelf, index) }
                }
            }
        }
        if !state.startListening.isEmpty {
            SectionHeader(S.homeStartListening, overline: S.homeStartListeningOverline) {
                PlayButton(isPlaying: false, size: .large) { actions.playStartListening(0) }
            }
            ListGroup {
                ForEach(Array(state.startListening.enumerated()), id: \.element.key) { index, track in
                    songRow(track, state) { actions.playStartListening(index) }
                }
            }
        }
        if !state.folders.isEmpty {
            SectionHeader(S.homeYourFolders)
            Shelf {
                ForEach(state.folders) { folderTile($0, width: 148, actions) }
            }
        }
        if !state.albums.isEmpty {
            SectionHeader(S.homeAlbums)
            Shelf {
                ForEach(state.albums) { albumTile($0, width: 148, actions) }
            }
        }
        if !state.topArtists.isEmpty {
            SectionHeader(S.homeTopArtists)
            Shelf {
                ForEach(state.topArtists) { artistTile($0, width: 112, actions) }
            }
        }
    }

    // MARK: Hero

    @ViewBuilder
    private func hero(_ state: HomeState, _ actions: HomeActions) -> some View {
        if state.favouriteCount != nil || !state.heroAlbums.isEmpty {
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: AtticSpacing.s) {
                    if let count = state.favouriteCount {
                        FavouritesPill(
                            count: S.grouped(count), unit: S.homeFavouritesUnit(count: count), action: actions.openFavourites
                        )
                    }
                    ForEach(state.heroAlbums) { album in
                        HeroTile(title: album.name, subtitle: album.heroSubtitle) {
                            actions.openAlbum(album.id)
                        } art: {
                            ArtworkImage(track: album.art, maxPixel: 600) { Cover($0, shape: .square) }
                        }
                    }
                }
                .padding(.horizontal, AtticSpacing.screenMargin)
            }
            .scrollIndicators(.hidden)
            .padding(.top, AtticSpacing.m)
        }
    }

    // MARK: Filtered grids

    private func albumGrid(_ state: HomeState, _ actions: HomeActions) -> some View {
        TileGrid(state.albums) { albumTile($0, width: nil, actions) }
            .padding(.top, AtticSpacing.l)
    }

    private func artistGrid(_ state: HomeState, _ actions: HomeActions) -> some View {
        TileGrid(state.topArtists, columns: 3) { artistTile($0, width: nil, actions) }
            .padding(.top, AtticSpacing.l)
    }

    private func folderGrid(_ state: HomeState, _ actions: HomeActions) -> some View {
        TileGrid(state.folders) { folderTile($0, width: nil, actions) }
            .padding(.top, AtticSpacing.l)
    }

    // MARK: Tiles and rows

    private func songTile(_ track: Track, _ state: HomeState, action: @escaping () -> Void) -> some View {
        let isCurrent = track.key == state.currentKey
        return Tile(
            title: track.title, subtitle: track.artist, width: 112,
            isCurrent: isCurrent, isPlaying: isCurrent && state.isPlaying, action: action
        ) {
            ArtworkImage(track: track, maxPixel: 336) { Cover($0) }
        }
    }

    private func songRow(_ track: Track, _ state: HomeState, action: @escaping () -> Void) -> some View {
        let isCurrent = track.key == state.currentKey
        return ArtworkImage(track: track, maxPixel: 144) { image in
            SongRow(
                title: track.title, subtitle: track.artist, lead: .cover(image),
                isCurrent: isCurrent, isPlaying: isCurrent && state.isPlaying,
                isLiked: state.likedKeys.contains(track.key), action: action
            ) {
                RowMoreMenu(title: track.title) { SongMenuItems(track: track) }
            }
            .contextMenu { SongMenuItems(track: track) }
        }
    }

    private func albumTile(_ album: HomeAlbum, width: CGFloat?, _ actions: HomeActions) -> some View {
        Tile(title: album.name, subtitle: album.artist, width: width) {
            actions.openAlbum(album.id)
        } art: {
            ArtworkImage(track: album.art, maxPixel: width == nil ? 525 : 444) { Cover($0) }
        }
    }

    private func artistTile(_ artist: HomeArtist, width: CGFloat?, _ actions: HomeActions) -> some View {
        Tile(title: artist.name, width: width, isArtist: true) {
            actions.openArtist(artist.id)
        } art: {
            ArtworkImage(track: artist.art, maxPixel: 336) { Cover($0, shape: .artist, fallback: .person) }
        }
    }

    private func folderTile(_ folder: HomeFolder, width: CGFloat?, _ actions: HomeActions) -> some View {
        Tile(title: folder.name, subtitle: folder.songs, width: width) {
            actions.openFolder(folder.id)
        } art: {
            FolderMosaic(covers: folder.covers)
        }
    }
}

// MARK: Pieces

/// What the bar needs from the scroll, kept out of the screen's state so scrolling redraws only the bar.
@MainActor
@Observable
private final class HomeScroll {
    var fraction: CGFloat = 0
}

private struct HomeBar: View {
    let scroll: HomeScroll
    let openSettings: () -> Void
    @Environment(\.atticScheme) private var scheme

    var body: some View {
        RootAppBar(S.appName, scrolled: scroll.fraction) {
            AtticMarkIcon(size: 32 - 8 * scroll.fraction)
                .foregroundStyle(scheme.primary)
                .accessibilityHidden(true)
        } actions: {
            AtticIconButton(.settings, label: S.settings, action: openSettings)
        }
    }
}

/// A folder's cover: four covers from different albums, loaded together so the grid doesn't change
/// shape while they arrive.
private struct FolderMosaic: View {
    let covers: [Track]
    @Environment(AppModel.self) private var app
    @State private var images: [Image] = []

    var body: some View {
        Mosaic(images)
            .task(id: covers.map(\.key)) {
                var loaded: [Image] = []
                for track in covers {
                    if let image = await app.artwork.image(for: track, maxPixel: 240) { loaded.append(Image(uiImage: image)) }
                }
                images = loaded
            }
    }
}
