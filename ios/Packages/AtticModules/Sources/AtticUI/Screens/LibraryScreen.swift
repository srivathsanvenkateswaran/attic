import AtticCore
import AtticKit
import SwiftUI
import UniformTypeIdentifiers

struct LibraryScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme

    @State private var filter: LibraryFilter?
    @AppStorage("library.grid") private var grid = false
    @AppStorage("library.sort") private var sort: SongSort = .title
    @State private var creating = false
    @State private var newPlaylistName = ""
    @State private var showingFolderPicker = false
    @State private var scrolled: CGFloat = 0

    // DESIGN.md, Components: a collection row's lead is 56pt.
    private let leadSize: CGFloat = 56

    var body: some View {
        let model = makeLibraryModel(
            app: app, router: router, filter: $filter, grid: $grid, sort: $sort, creating: $creating
        )
        let state = model.state
        let actions = model.actions

        VStack(spacing: 0) {
            RootAppBar(S.libraryTitle, scrolled: scrolled) {
                AtticIconButton(.search, label: S.librarySearchCd, action: actions.openSearch)
                AtticIconButton(
                    state.grid ? .viewList : .gridView,
                    label: state.grid ? S.libraryShowListCd : S.libraryShowGridCd,
                    action: actions.toggleGrid
                )
                .disabled(state.filter == .songs)
                AtticIconButton(.settings, label: S.settings, action: actions.openSettings)
            }

            ChipRow {
                ForEach(LibraryFilter.allCases, id: \.self) { option in
                    FilterChip(option.title, isSelected: state.filter == option) {
                        actions.toggleFilter(option)
                    }
                }
            }
            .padding(.bottom, AtticSpacing.m)
            .background(scrolled > 0.5 ? scheme.surfaceContainer : scheme.surface.opacity(0))

            if state.isEmpty {
                EmptyState(
                    title: S.homeEmptyTitle,
                    body: S.homeEmptyBody,
                    symbol: .musicOff,
                    clover: true
                ) {
                    AtticButton(S.settingsFoldersAdd, variant: .tonal, leading: .add) {
                        showingFolderPicker = true
                    }
                }
            } else {
                ScrollView {
                    Group {
                        if state.filter == .songs {
                            songList(state, actions)
                        } else if state.grid {
                            collectionGrid(state, actions)
                        } else {
                            collectionList(state, actions)
                        }
                    }
                }
                .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
                .onScrollFraction { scrolled = $0 }
                .refreshable { await app.library.rescan() }
            }
        }
        .background(scheme.surface)
        .toolbar(.hidden, for: .navigationBar)
        .overlay(alignment: .bottomTrailing) {
            if state.filter == nil || state.filter == .playlists {
                ExtendedFab(S.playlistPickerNew, symbol: .add, expanded: scrolled < 0.5) {
                    actions.startCreating()
                }
                // DESIGN.md, The dock: 12 above the dock, 16 from the trailing edge.
                .padding(.trailing, AtticSpacing.screenMargin)
                .padding(.bottom, AtticSpacing.m)
            }
        }
        .alert(S.dialogNewPlaylistTitle, isPresented: $creating) {
            TextField(S.dialogPlaylistNameLabel, text: $newPlaylistName)
            Button(S.cancel, role: .cancel) { newPlaylistName = "" }
            Button(S.create) {
                let name = newPlaylistName.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { return }
                actions.create(name)
                newPlaylistName = ""
            }
        }
        .fileImporter(isPresented: $showingFolderPicker, allowedContentTypes: [.folder]) { result in
            guard case .success(let url) = result else { return }
            Task { try? await app.library.addFolder(url) }
        }
    }

    @ViewBuilder
    private func collectionList(_ state: LibraryState, _ actions: LibraryActions) -> some View {
        LazyVStack(spacing: 0) {
            ForEach(state.sections) { section in
                if let label = section.label {
                    ListLabel(label)
                }
                ForEach(Array(section.items.enumerated()), id: \.element.id) { index, item in
                    CollectionRow(title: item.title, subtitle: item.subtitle, action: { actions.open(item) }) {
                        itemArt(item, size: leadSize)
                    }
                    .groupCell(index: index, count: section.items.count)
                }
            }
        }
    }

    @ViewBuilder
    private func collectionGrid(_ state: LibraryState, _ actions: LibraryActions) -> some View {
        LazyVStack(spacing: 0) {
            ForEach(state.sections) { section in
                if let label = section.label {
                    ListLabel(label)
                }
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: AtticSpacing.shelfGap), count: 3),
                    spacing: AtticSpacing.l
                ) {
                    ForEach(section.items) { item in
                        Tile(
                            title: item.title,
                            subtitle: item.subtitle,
                            width: nil,
                            isArtist: item.kind == .artist,
                            action: { actions.open(item) }
                        ) {
                            itemArt(item, size: nil)
                        }
                    }
                }
                .padding(.horizontal, AtticSpacing.screenMargin)
            }
        }
    }

    @ViewBuilder
    private func songList(_ state: LibraryState, _ actions: LibraryActions) -> some View {
        VStack(spacing: 0) {
            AtticButton(
                S.libraryShuffleAllCd, size: .medium, leading: .shuffle, fillsWidth: true,
                action: actions.shuffleAll
            )
            .padding(.horizontal, AtticSpacing.screenMargin)
            .padding(.top, AtticSpacing.xs)

            HStack(spacing: AtticSpacing.s) {
                Text(S.songCount(count: state.songs.count, S.grouped(state.songs.count)))
                    .atticFont(.labelLarge.weight(.semibold))
                    .foregroundStyle(scheme.onSurfaceVariant)
                Spacer(minLength: 0)
                SplitButton(
                    state.sort.title,
                    leading: .sort,
                    menuLabel: S.librarySortMenuCd,
                    action: actions.cycleSort
                ) {
                    ForEach(SongSort.allCases, id: \.self) { option in
                        Button { actions.setSort(option) } label: {
                            HStack {
                                Text(option.title)
                                if option == state.sort { SymbolIcon(.check, size: 18) }
                            }
                        }
                    }
                }
            }
            // DESIGN.md, Library: the count row starts 32pt from the leading edge.
            .padding(.leading, 32)
            .padding(.trailing, AtticSpacing.screenMargin)
            .padding(.top, AtticSpacing.m)
            .padding(.bottom, AtticSpacing.s)

            LazyVStack(spacing: 0) {
                ForEach(Array(state.songs.enumerated()), id: \.element.key) { index, track in
                    ArtworkImage(track: track, maxPixel: 144) { image in
                        SongRow(
                            title: track.title,
                            subtitle: track.artist,
                            lead: .cover(image),
                            isCurrent: state.currentKey == track.key,
                            isPlaying: state.isPlaying,
                            isLiked: state.likedKeys.contains(track.key),
                            action: { actions.play(track) }
                        ) {
                            RowMoreMenu(title: track.title) { SongMenuItems(track: track) }
                        }
                    }
                    .contextMenu { SongMenuItems(track: track) }
                    .groupCell(index: index, count: state.songs.count)
                }
            }
        }
    }

    @ViewBuilder
    private func itemArt(_ item: LibraryItem, size: CGFloat?) -> some View {
        switch item.kind {
        case .favourites:
            FavouritesArt(size: size)
        case .playlist, .folder:
            MosaicCovers(tracks: item.artTracks, size: size)
        case .artist:
            ArtworkImage(track: item.artTracks.first, maxPixel: 144) { image in
                Cover(image, size: size, shape: .artist, fallback: .person)
            }
        case .album:
            ArtworkImage(track: item.artTracks.first, maxPixel: 144) { image in
                Cover(image, size: size)
            }
        }
    }
}

/// Loads up to four covers with ArtworkImage, then draws them through the kit's Mosaic.
private struct MosaicCovers: View {
    let tracks: [Track]
    var size: CGFloat?
    @State private var images: [Image?] = []

    var body: some View {
        Mosaic(images.compactMap { $0 }, size: size)
            .background {
                ZStack {
                    ForEach(Array(tracks.prefix(4).enumerated()), id: \.offset) { index, track in
                        ArtworkImage(track: track, maxPixel: 144) { image in
                            Color.clear
                                .onAppear { store(image, at: index) }
                                .onChange(of: image) { _, newValue in store(newValue, at: index) }
                        }
                    }
                }
                .frame(width: 0, height: 0)
            }
    }

    private func store(_ image: Image?, at index: Int) {
        if images.count != min(4, tracks.count) {
            images = Array(repeating: nil, count: min(4, tracks.count))
        }
        guard images.indices.contains(index) else { return }
        images[index] = image
    }
}
