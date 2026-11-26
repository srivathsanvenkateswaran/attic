import AtticCore
import AtticKit
import SwiftUI

/// Album, playlist, folder and Favourites: one header block over one group of songs. The four screens
/// in AlbumScreen.swift and its neighbours only say which collection to show.
struct CollectionScreen: View {
    let kind: CollectionKind
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme
    /// The header's title has scrolled under the bar.
    @State private var titleUnderBar = false
    @State private var reordering = false
    @State private var renaming = false
    @State private var newName = ""
    @State private var confirmingDelete = false

    /// How far the header scrolls before its title is under the bar (68 below the bar's top, DESIGN.md).
    private static let titleUnderBarOffset: CGFloat = 40

    var body: some View {
        let model = makeCollectionModel(app: app, router: router, kind: kind)
        let state = model.state
        let actions = model.actions
        Group {
            if state.isLoading {
                loadingView(actions)
            } else if state.isGone {
                goneView(state, actions)
            } else if reordering && state.isEditable {
                reorderView(state, actions)
            } else {
                collectionView(state, actions)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(scheme.surface.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .alert(S.dialogRenamePlaylistTitle, isPresented: $renaming) {
            TextField(S.dialogPlaylistNameLabel, text: $newName)
            Button(S.cancel, role: .cancel) {}
            Button(S.save) { actions.rename(newName) }
        }
        .confirmationDialog(
            S.dialogDeletePlaylistTitle(state.title), isPresented: $confirmingDelete, titleVisibility: .visible
        ) {
            Button(S.dialogDeletePlaylistConfirm, role: .destructive) { actions.delete() }
        } message: {
            Text(S.dialogDeletePlaylistBody)
        }
        .onChange(of: state.isEditable) { _, editable in if !editable { reordering = false } }
    }

    // MARK: States

    private func loadingView(_ actions: CollectionActions) -> some View {
        VStack(spacing: 0) {
            DetailAppBar("", onBack: actions.back)
            LoadingState()
        }
    }

    private func goneView(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        VStack(spacing: 0) {
            DetailAppBar("", onBack: actions.back)
            EmptyState(title: state.goneTitle, body: S.collectionGoneBody, symbol: state.goneSymbol) {
                AtticButton(S.back, variant: .tonal, leading: .arrowBack, action: actions.back)
            }
            .padding(.top, 72) // DESIGN.md, Empty and loading states
            Spacer(minLength: 0)
        }
    }

    // MARK: The collection

    private func collectionView(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        ZStack(alignment: .top) {
            ScrollView {
                LazyVStack(spacing: 0) {
                    header(state, actions)
                    if state.isEditable {
                        playlistButtons(state, actions)
                    } else {
                        Color.clear.frame(height: AtticSpacing.groupSpacing)
                    }
                    if state.isEmpty {
                        emptyState(state)
                    } else {
                        ForEach(Array(state.tracks.enumerated()), id: \.offset) { index, track in
                            songRow(index, track, state, actions)
                        }
                    }
                }
            }
            .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
            .onScrollGeometryChange(for: Bool.self) { geometry in
                geometry.contentOffset.y + geometry.contentInsets.top > Self.titleUnderBarOffset
            } action: { _, under in
                titleUnderBar = under
            }
            bar(state, actions)
                .fromCover(state.seedTrack)
        }
    }

    @ViewBuilder
    private func bar(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        let surface: AppBarSurface = titleUnderBar ? .scrolled : .overArt
        if state.isEditable {
            DetailAppBar(state.title, surface: surface, onBack: actions.back) {
                optionsMenu(state)
            }
        } else {
            DetailAppBar(state.title, surface: surface, onBack: actions.back)
        }
    }

    private func optionsMenu(_ state: CollectionState) -> some View {
        Menu {
            Button {
                newName = state.title
                renaming = true
            } label: { Text(S.playlistRename) }
            Button(role: .destructive) { confirmingDelete = true } label: { Text(S.playlistDelete) }
        } label: {
            SymbolIcon(.moreVert)
                .foregroundStyle(titleUnderBar ? scheme.onSurfaceVariant : scheme.onSurface) // the bar's own tint is kit-internal
                .frame(width: AtticSpacing.touchTarget, height: AtticSpacing.touchTarget)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(S.collectionOptionsCd)
    }

    private func header(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        ArtworkImage(track: state.bylineTrack, maxPixel: 96) { bylineImage in
            CollectionHeader(
                title: state.title,
                byline: state.byline,
                bylineImage: bylineImage,
                onByline: actions.openByline,
                meta: state.meta,
                isPlaying: state.isPlayingThis,
                shuffleOn: state.isShuffleOn,
                isEnabled: !state.isEmpty,
                onPlay: actions.playOrPause,
                onShuffle: actions.shuffle,
                onAdd: actions.addToPlaylist
            ) {
                headerCover(state)
            }
        }
        .fromCover(state.seedTrack)
    }

    @ViewBuilder
    private func headerCover(_ state: CollectionState) -> some View {
        switch state.kind {
        case .album:
            ArtworkImage(track: state.coverTrack, maxPixel: 480) { image in Cover(image) }
        case .playlist, .folder:
            MosaicArt(tracks: state.coverTracks)
        case .favourites:
            FavouritesArt()
        }
    }

    private func playlistButtons(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        ButtonRow {
            AtticButton(
                S.playlistAddSongsCd, variant: state.isEmpty ? .filled : .tonal, leading: .add,
                action: actions.addSongs
            )
            if !state.isEmpty {
                AtticButton(S.playlistReorderCd, variant: .outlined, leading: .swapVert) {
                    withAnimation(AtticMotion.defaultEffects) { reordering = true }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, AtticSpacing.screenMargin)
        .padding(.vertical, AtticSpacing.groupSpacing)
    }

    @ViewBuilder
    private func emptyState(_ state: CollectionState) -> some View {
        if case .favourites = state.kind {
            EmptyState(title: S.favouritesEmptyTitle, body: S.favouritesEmptyBody, symbol: .favorite)
        } else {
            EmptyState(title: S.playlistEmptyTitle, body: S.playlistEmptyBody)
        }
    }

    // MARK: Songs

    @ViewBuilder
    private func songRow(_ index: Int, _ track: Track, _ state: CollectionState, _ actions: CollectionActions) -> some View {
        if case .album = state.kind {
            songRowContent(index, track, state, actions, image: nil)
        } else {
            ArtworkImage(track: track, maxPixel: 144) { image in
                songRowContent(index, track, state, actions, image: image)
            }
        }
    }

    private func songRowContent(
        _ index: Int, _ track: Track, _ state: CollectionState, _ actions: CollectionActions, image: Image?
    ) -> some View {
        let isCurrent = state.currentKey == track.key
        let lead: SongRowLead
        if case .album = state.kind {
            lead = .number(track.trackNumber ?? index + 1)
        } else {
            lead = .cover(image)
        }
        let menu = SongMenuItems(track: track, playlistID: state.playlistID, playlistIndex: state.entryIndices[index])
        return SongRow(
            title: track.title,
            subtitle: track.artist,
            lead: lead,
            isCurrent: isCurrent,
            isPlaying: isCurrent && state.isPlaying,
            isLiked: app.isLiked(track),
            action: { actions.play(index) }
        ) {
            RowMoreMenu(title: track.title) { menu }
        }
        .contextMenu { menu }
        .groupCell(index: index, count: state.tracks.count)
    }

    // MARK: Reorder

    private func reorderView(_ state: CollectionState, _ actions: CollectionActions) -> some View {
        VStack(spacing: 0) {
            DetailAppBar(
                S.playlistReorderCd, surface: .plain, backSymbol: .close, backLabel: S.close,
                onBack: { withAnimation(AtticMotion.defaultEffects) { reordering = false } }
            ) {
                AtticIconButton(.check, label: S.playlistReorderDoneCd, variant: .filled) {
                    withAnimation(AtticMotion.defaultEffects) { reordering = false }
                }
            }
            List {
                ForEach(Array(state.tracks.enumerated()), id: \.offset) { index, track in
                    reorderRow(index, track, state)
                        .listRowInsets(EdgeInsets())
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
                .onMove { source, destination in
                    guard let from = source.first else { return }
                    // `destination` is the gap the row is dropped before; the model wants its final place.
                    actions.move(from, destination > from ? destination - 1 : destination)
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .environment(\.defaultMinListRowHeight, 0)
            .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
        }
    }

    private func reorderRow(_ index: Int, _ track: Track, _ state: CollectionState) -> some View {
        ArtworkImage(track: track, maxPixel: 144) { image in
            let isCurrent = state.currentKey == track.key
            SongRow(
                title: track.title,
                subtitle: track.artist,
                lead: .cover(image),
                isCurrent: isCurrent,
                isPlaying: isCurrent && state.isPlaying,
                isLiked: app.isLiked(track)
            ) {
                RowDragHandle()
            }
            .groupCell(index: index, count: state.tracks.count)
        }
    }
}

/// A playlist's or folder's cover: four covers from its songs, loaded together so the grid does not
/// rebuild as each one arrives.
private struct MosaicArt: View {
    let tracks: [Track]
    @Environment(AppModel.self) private var app
    @State private var images: [Image] = []

    var body: some View {
        Mosaic(images)
            .task(id: tracks.map(\.key)) {
                let artwork = app.artwork
                let loaded = await withTaskGroup(of: (Int, UIImage?).self) { group in
                    for (index, track) in tracks.enumerated() {
                        group.addTask { (index, await artwork.image(for: track, maxPixel: 240)) }
                    }
                    var found: [(Int, UIImage)] = []
                    for await (index, image) in group {
                        if let image { found.append((index, image)) }
                    }
                    return found.sorted { $0.0 < $1.0 }.map { Image(uiImage: $0.1) }
                }
                images = loaded
            }
    }
}
