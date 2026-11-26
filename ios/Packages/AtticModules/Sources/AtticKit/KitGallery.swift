import SwiftUI
import UIKit

/// Every token and component of the kit on one scrolling page, for review. The switch at the top flips
/// light and dark. Launch arguments, read from the defaults: `-kitGalleryTheme dark|light|system` and
/// `-kitGallerySection N` (0 scheme, 1 from art, 2 type, 3 shapes, 4 icons, 5 buttons, 6 chips and
/// search, 7 song rows, 8 collection rows, 9 tiles, 10 play controls, 11 bars, 12 player surfaces,
/// 13 settings, 14 states, 15 sheets and lyrics).
public struct KitGallery: View {
    @State private var mode: AtticThemeMode
    private let initialSection: Int?

    public init(mode: AtticThemeMode? = nil, initialSection: Int? = nil) {
        let defaults = UserDefaults.standard
        let stored = defaults.string(forKey: "kitGalleryTheme").flatMap(AtticThemeMode.init(rawValue:))
        _mode = State(initialValue: mode ?? stored ?? .system)
        self.initialSection = initialSection ?? (defaults.object(forKey: "kitGallerySection") as? Int)
            ?? defaults.string(forKey: "kitGallerySection").flatMap(Int.init)
    }

    public var body: some View {
        GalleryPage(mode: $mode, initialSection: initialSection)
            .atticTheme(mode)
    }
}

private struct GalleryPage: View {
    @Environment(\.atticScheme) private var scheme
    @Binding var mode: AtticThemeMode
    let initialSection: Int?

    @State private var scrolled: CGFloat = 0
    @State private var tab = 0
    @State private var filter = 0
    @State private var chips: Set<Int> = [1]
    @State private var query = ""
    @State private var isPlaying = true
    @State private var isLiked = true
    @State private var shuffle = true
    @State private var repeatMode = AtticRepeatMode.all
    @State private var position = 78.0
    @State private var toggle = true
    @State private var choice = 0
    @State private var snack: String?
    @State private var seedIndex = 0

    private static let titles = [
        "Scheme", "From the cover", "Type", "Shapes", "Icons", "Buttons", "Chips and search", "Song rows",
        "Collection rows", "Tiles", "Play controls", "Bars", "Player surfaces", "Settings", "States", "Sheets and lyrics",
    ]

    var body: some View {
        VStack(spacing: 0) {
            RootAppBar("Kit gallery", scrolled: scrolled) {
                AtticMarkIcon(size: 32 - 8 * scrolled).foregroundStyle(scheme.primary)
            } actions: {
                AtticIconButton(scheme.isDark ? .lightMode : .nightsStay, label: scheme.isDark ? "Light" : "Dark") {
                    mode = scheme.isDark ? .light : .dark
                }
            }
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(Self.titles.indices, id: \.self) { index in
                            VStack(alignment: .leading, spacing: 0) {
                                SectionHeader("\(index)  \(Self.titles[index])")
                                section(index)
                            }
                            .id(index)
                        }
                    }
                    .padding(.bottom, 120)
                }
                .onScrollFraction { scrolled = $0 }
                .task {
                    if let initialSection, Self.titles.indices.contains(initialSection) {
                        try? await Task.sleep(for: .milliseconds(300))
                        proxy.scrollTo(initialSection, anchor: .top)
                    }
                }
            }
        }
        .snackbar($snack)
    }

    @ViewBuilder
    private func section(_ index: Int) -> some View {
        switch index {
        case 0: schemeSection
        case 1: fromArtSection
        case 2: typeSection
        case 3: shapesSection
        case 4: iconsSection
        case 5: buttonsSection
        case 6: chipsSection
        case 7: songRowsSection
        case 8: collectionRowsSection
        case 9: tilesSection
        case 10: playControlsSection
        case 11: barsSection
        case 12: playerSection
        case 13: settingsSection
        case 14: statesSection
        default: sheetsSection
        }
    }

    // MARK: Tokens

    private var schemeSection: some View {
        let roles: [(String, Color, Color)] = [
            ("primary", scheme.primary, scheme.onPrimary),
            ("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer),
            ("secondary", scheme.secondary, scheme.onSecondary),
            ("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer),
            ("tertiary", scheme.tertiary, scheme.onTertiary),
            ("tertiaryContainer", scheme.tertiaryContainer, scheme.onTertiaryContainer),
            ("error", scheme.error, scheme.onError),
            ("errorContainer", scheme.errorContainer, scheme.onErrorContainer),
            ("surface", scheme.surface, scheme.onSurface),
            ("surfaceContainerLowest", scheme.surfaceContainerLowest, scheme.onSurface),
            ("surfaceContainerLow", scheme.surfaceContainerLow, scheme.onSurface),
            ("surfaceContainer", scheme.surfaceContainer, scheme.onSurface),
            ("surfaceContainerHigh", scheme.surfaceContainerHigh, scheme.onSurfaceVariant),
            ("surfaceContainerHighest", scheme.surfaceContainerHighest, scheme.onSurfaceVariant),
            ("inverseSurface", scheme.inverseSurface, scheme.inverseOnSurface),
            ("outline", scheme.outline, scheme.surface),
            ("outlineVariant", scheme.outlineVariant, scheme.surface),
        ]
        return SwatchGrid(roles: roles)
    }

    private var fromArtSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(GallerySamples.seeds.indices, id: \.self) { index in
                        Button {
                            seedIndex = index
                        } label: {
                            Cover(GallerySamples.cover(index), size: 56)
                                .overlay {
                                    if index == seedIndex { CoverShape().strokeBorder(scheme.primary, lineWidth: 3) }
                                }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Cover \(index + 1)")
                    }
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            FromArtSample(isPlaying: $isPlaying, isLiked: $isLiked, cover: GallerySamples.cover(seedIndex))
                .fromArt(seed: GallerySamples.seeds[seedIndex])
        }
    }

    private var typeSection: some View {
        let styles: [(String, AtticTextStyle)] = [
            ("displayLarge", .displayLarge), ("displayMedium", .displayMedium), ("displaySmall", .displaySmall),
            ("headlineLarge", .headlineLarge), ("headlineMedium", .headlineMedium), ("headlineSmall", .headlineSmall),
            ("titleLarge", .titleLarge), ("titleMedium", .titleMedium), ("titleSmall", .titleSmall),
            ("bodyLarge", .bodyLarge), ("bodyMedium", .bodyMedium), ("bodySmall", .bodySmall),
            ("labelLarge", .labelLarge), ("labelMedium", .labelMedium), ("labelSmall", .labelSmall),
        ]
        return VStack(alignment: .leading, spacing: 10) {
            ForEach(styles, id: \.0) { name, style in
                VStack(alignment: .leading, spacing: 0) {
                    Text(name).atticFont(.labelSmall).foregroundStyle(scheme.onSurfaceVariant)
                    HStack(alignment: .firstTextBaseline, spacing: 16) {
                        Text("Attic").atticFont(style).lineLimit(1)
                        Text("Attic").atticFont(style.emphasized(style.size >= 22 ? 700 : 600)).lineLimit(1)
                    }
                }
            }
            Text("Narrowing").atticFont(.labelSmall).foregroundStyle(scheme.onSurfaceVariant)
            NarrowingText("A very long song title that narrows before it truncates", style: .headlineLargeEmphasized.tracking(-0.3))
            Text("Tabular 1:18  3:52  11:11").atticFont(.labelMedium.tabular())
        }
        .padding(.horizontal, 16)
    }

    private var shapesSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            ScrollView(.horizontal) {
                HStack(alignment: .bottom, spacing: 12) {
                    ForEach([40, 48, 64, 96, 148], id: \.self) { size in
                        Cover(GallerySamples.cover(0), size: CGFloat(size))
                    }
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            HStack(spacing: 12) {
                Cover(GallerySamples.cover(2), size: 72, shape: .artist)
                Cover(nil, size: 72, shape: .artist, fallback: .person)
                CloverShape().fill(scheme.secondaryContainer).frame(width: 72, height: 72)
                FavouritesArt(size: 72)
                Mosaic((0..<4).map(GallerySamples.cover), size: 72)
                Cover(nil, size: 72)
            }
            .padding(.horizontal, 16)
            HStack(spacing: 24) {
                LoadingIndicator()
                LoadingIndicator(contained: true)
                AtticMarkIcon(size: 48).foregroundStyle(scheme.primary)
                AtticWordmark()
            }
            .padding(.horizontal, 16)
        }
    }

    private var iconsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 8), spacing: 12) {
                ForEach(AtticSymbol.allCases, id: \.self) { symbol in
                    SymbolIcon(symbol).foregroundStyle(scheme.onSurface)
                }
            }
            HStack(spacing: 12) {
                ForEach([18, 20, 24, 28, 32, 36, 40], id: \.self) { size in
                    SymbolIcon(.favorite, size: CGFloat(size), filled: true).foregroundStyle(scheme.primary)
                }
            }
            HStack(spacing: 12) {
                ForEach([AtticSymbol.home, .libraryMusic, .favorite, .playArrow, .settings], id: \.self) { symbol in
                    SymbolIcon(symbol).foregroundStyle(scheme.onSurfaceVariant)
                    SymbolIcon(symbol, filled: true).foregroundStyle(scheme.primary)
                }
            }
        }
        .padding(.horizontal, 16)
    }

    // MARK: Components

    private var buttonsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ButtonRow {
                AtticButton("Filled") { snack = "Filled" }
                AtticButton("Tonal", variant: .tonal) { snack = "Tonal" }
                AtticButton("Outlined", variant: .outlined) {}
                AtticButton("Text", variant: .text) {}
            }
            ButtonRow {
                AtticButton("Play", size: .medium, leading: .playArrow) {}
                AtticButton("Pause", size: .medium, leading: .pause, squared: true) {}
            }
            ButtonRow {
                AtticButton("Add songs", variant: .tonal, leading: .add) {}
                AtticButton("Reorder songs", variant: .outlined, leading: .swapVert) {}
            }
            AtticButton("Shuffle all songs", size: .medium, leading: .shuffle, fillsWidth: true) {}
            ButtonRow {
                AtticButton("Delete playlist", variant: .text, tint: scheme.error) {}
                AtticButton("Disabled") {}.disabled(true)
            }
            HStack(spacing: 4) {
                AtticIconButton(.search, label: "Search") {}
                AtticIconButton(.settings, label: "Settings", variant: .filled) {}
                AtticIconButton(.playlistAdd, label: "Add", variant: .tonal) {}
                AtticIconButton(.share, label: "Share", variant: .outlined) {}
                AtticIconButton(.playlistAdd, label: "Add", variant: .tonal, medium: true) {}
                AtticIconButton(.shuffle, label: "Shuffle", variant: .tonal, medium: true, width: .wide) {}
            }
            HStack(spacing: 4) {
                AtticToggleButton(.shuffle, isOn: shuffle, label: "Shuffle") { shuffle.toggle() }
                AtticToggleButton(.shuffle, isOn: !shuffle, label: "Shuffle") { shuffle.toggle() }
                AtticToggleButton(.shuffle, isOn: shuffle, label: "Shuffle", medium: true, tonalWhenOff: true) { shuffle.toggle() }
                ShuffleToggle(isOn: shuffle) { shuffle.toggle() }
                RepeatToggle(mode: repeatMode) {
                    repeatMode = repeatMode == .off ? .all : (repeatMode == .all ? .one : .off)
                }
                HeartButton(isFavourite: isLiked) { isLiked.toggle() }
            }
            HStack(spacing: 12) {
                ExtendedFab("New playlist", symbol: .add) {}
                ExtendedFab("New playlist", symbol: .add, expanded: false) {}
            }
            SplitButton("Title", leading: .sort, menuLabel: "Sort songs by") {} menu: {
                Button("Title") {}
                Button("Artist") {}
                Button("Recently added") {}
            }
        }
        .padding(.horizontal, 16)
    }

    private var chipsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ConnectedButtonGroup(["All", "Albums", "Artists", "Folders"], selection: filter) { filter = $0 }
            ChipRow {
                ForEach(Array(["Playlists", "Albums", "Artists", "Songs"].enumerated()), id: \.offset) { index, label in
                    FilterChip(label, isSelected: chips.contains(index)) {
                        if chips.contains(index) { chips.remove(index) } else { chips = [index] }
                    }
                }
            }
            SearchField(text: $query, placeholder: "Search your music").padding(.horizontal, 16)
            SearchField(text: .constant("paper"), placeholder: "Search your music", onBack: {}).padding(.horizontal, 16)
        }
    }

    private var songRowsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ListGroup {
                SongRow(title: "Lights Over the Water", subtitle: "Mira Okafor", lead: .cover(GallerySamples.cover(0)), isCurrent: true, isPlaying: isPlaying, action: { isPlaying.toggle() }) {
                    RowMoreMenu(title: "Lights Over the Water") { Button("Play next") {} }
                }
                SongRow(title: "Porch Light", subtitle: "Mira Okafor", lead: .cover(GallerySamples.cover(1)), isLiked: true, action: {}) {
                    RowMoreMenu(title: "Porch Light") { Button("Play next") {} }
                }
                SongRow(title: "A song with no cover and a title long enough to truncate", subtitle: "Unknown artist", lead: .cover(nil), action: {}) {
                    RowDragHandle()
                }
            }
            ListGroup {
                SongRow(title: "Lights Over the Water", subtitle: "Mira Okafor", lead: .number(1), isCurrent: true, isPlaying: isPlaying, action: {}) {
                    RowMoreButton(title: "Lights Over the Water") {}
                }
                SongRow(title: "Porch Light", subtitle: "Mira Okafor", lead: .number(2), action: {}) {
                    RowMoreButton(title: "Porch Light") {}
                }
                SongRow(title: "Moth Letters", subtitle: "Mira Okafor", lead: .rankedCover(3, GallerySamples.cover(2)), action: {}) {
                    RowCount("187 plays")
                }
            }
            ListLabel("Up next, 2 songs")
            VStack(spacing: 0) {
                SongRow(title: "Second Bus Home", subtitle: "Mira Okafor", lead: .cover(GallerySamples.cover(3))) { RowDragHandle() }
                    .groupCell(index: 0, count: 2)
                SongRow(title: "Being dragged", subtitle: "Mira Okafor", lead: .cover(GallerySamples.cover(4))) { RowDragHandle() }
                    .groupCell(index: 1, count: 2, lifted: true)
            }
        }
    }

    private var collectionRowsSection: some View {
        ListGroup {
            CollectionRow(title: "Favourites", subtitle: "Playlist, 42 songs", action: {}) { FavouritesArt() }
            CollectionRow(title: "Long drive", subtitle: "Playlist, 25 songs", action: {}) { Mosaic((0..<4).map(GallerySamples.cover)) }
            CollectionRow(title: "Mira Okafor", subtitle: "Artist", action: {}) { Cover(GallerySamples.cover(2), shape: .artist) }
            CollectionRow(title: "Paper Lanterns", subtitle: "Album, Mira Okafor", isSelected: true, action: {}) { Cover(GallerySamples.cover(0)) }
        }
    }

    private var tilesSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: 8) {
                    FavouritesPill(count: "42", unit: "songs") {}
                    ForEach(0..<3, id: \.self) { index in
                        HeroTile(title: GallerySamples.albums[index], subtitle: "Mira Okafor, 2025", action: {}) {
                            Cover(GallerySamples.cover(index), shape: .square)
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            Shelf {
                ForEach(0..<5, id: \.self) { index in
                    Tile(title: GallerySamples.albums[index], subtitle: "Mira Okafor", isCurrent: index == 0, isPlaying: isPlaying, action: {}) {
                        Cover(GallerySamples.cover(index))
                    }
                }
            }
            Shelf {
                ForEach(0..<5, id: \.self) { index in
                    Tile(title: GallerySamples.artists[index], width: 112, isArtist: true, action: {}) {
                        Cover(GallerySamples.cover(index), shape: .artist)
                    }
                }
            }
            TileGrid(GallerySamples.gridItems, columns: 3) { item in
                Tile(title: item.title, subtitle: "8 songs", width: nil, action: {}) { Cover(GallerySamples.cover(item.id)) }
            }
            InfoCard(title: "Lyrics", symbol: .lyrics) {
                LyricLine("carry what we couldn't say")
                LyricLine("lights over the water", state: .upcoming)
            }
            .padding(.horizontal, 16)
        }
    }

    private var playControlsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                PlayButton(isPlaying: isPlaying, progress: position / 232) { isPlaying.toggle() }
                PlayButton(isPlaying: isPlaying) { isPlaying.toggle() }
                PlayButton(isPlaying: isPlaying, size: .large) { isPlaying.toggle() }
                PlayButton(isPlaying: isPlaying, size: .large, tonal: true) { isPlaying.toggle() }
                NowPlayingWave(isPlaying: isPlaying).foregroundStyle(scheme.primary)
            }
            ProgressSlider(position: $position, duration: 232, isPlaying: isPlaying, label: "Seek in Lights Over the Water")
            Transport(isPlaying: isPlaying, onPrevious: {}, onPlayPause: { isPlaying.toggle() }, onNext: {})
            ToolsRow {
                ShuffleToggle(isOn: shuffle) { shuffle.toggle() }
                RepeatToggle(mode: repeatMode) {
                    repeatMode = repeatMode == .off ? .all : (repeatMode == .all ? .one : .off)
                }
                HeartButton(isFavourite: isLiked) { isLiked.toggle() }
                AtticIconButton(.lyrics, label: "Lyrics") {}
                AtticIconButton(.queueMusic, label: "Open queue") {}
            }
            LinearProgress(position / 232)
        }
        .padding(.horizontal, 24)
    }

    private var barsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            RootAppBar("Library") {
                AtticIconButton(.search, label: "Search") {}
                AtticIconButton(.gridView, label: "Show as grid") {}
                AtticIconButton(.settings, label: "Settings") {}
            }
            RootAppBar("Library", scrolled: 1) {
                AtticIconButton(.settings, label: "Settings") {}
            }
            DetailAppBar("Paper Lanterns", onBack: {}) {
                AtticIconButton(.moreVert, label: "Playlist options") {}
            }
            DetailAppBar("Paper Lanterns", surface: .scrolled, onBack: {})
            ZStack {
                GallerySamples.cover(0).resizable().scaledToFill().frame(height: 64).clipped()
                DetailAppBar("Paper Lanterns", surface: .overArt, onBack: {}) {
                    AtticIconButton(.moreVert, label: "Playlist options") {}
                }
            }
            AtticTabBar(
                items: [
                    AtticTabItem(title: "Home", symbol: .home),
                    AtticTabItem(title: "Search", symbol: .search),
                    AtticTabItem(title: "Library", symbol: .libraryMusic),
                ],
                selection: $tab
            )
        }
    }

    private var playerSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            MiniPlayerBar(
                title: "Lights Over the Water", artist: "Mira Okafor", cover: GallerySamples.cover(0),
                isPlaying: isPlaying, isLiked: isLiked, progress: position / 232,
                onOpen: { snack = "Opens the player" }, onPlayPause: { isPlaying.toggle() }, onNext: {}, onToggleLike: { isLiked.toggle() }
            )
            .fromArt(seed: GallerySamples.seeds[0])
            .padding(.horizontal, 12)
            CollectionHeader(
                title: "Paper Lanterns", byline: "Mira Okafor", bylineImage: GallerySamples.cover(0), onByline: {},
                meta: ["Album • 2025", "8 songs, 30 min"], isPlaying: isPlaying, shuffleOn: shuffle,
                onPlay: { isPlaying.toggle() }, onShuffle: { shuffle.toggle() }, onAdd: {}
            ) {
                Cover(GallerySamples.cover(0))
            }
            .fromArt(seed: GallerySamples.seeds[0])
            CollectionHeader(
                title: "Mira Okafor", isArtist: true, meta: ["24 songs"], isEnabled: true,
                onPlay: {}, onShuffle: {}, onAdd: {}
            ) {
                Cover(GallerySamples.cover(2), shape: .artist)
            }
            .fromArt(seed: GallerySamples.seeds[2])
            CollectionHeader(title: "Favourites", meta: ["42 songs, 2 hr 41 min"], onPlay: {}, onShuffle: {}) {
                FavouritesArt()
            }
        }
    }

    private var settingsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            LargeTitle("Settings")
            SettingsSection("Appearance")
            ListGroup {
                ConnectedButtonGroup(["Match iPhone", "Light", "Dark"], selection: choice, onGroupCell: true) { choice = $0 }
                    .padding(.vertical, 8)
            }
            SettingsSection("Music folders")
            ListGroup {
                SettingsRow(title: "Music", summary: "1,204 songs", symbol: .folder) {
                    AtticIconButton(.close, label: "Remove Music") {}
                }
                SettingsNavigationRow(title: "Add a folder", symbol: .add) {}
                SettingsToggleRow(title: "A switch", summary: "With a line that says what it does.", isOn: $toggle)
                SettingsChoiceRow(title: "A choice", isSelected: true) {}
                SettingsNote("Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent.")
            }
        }
    }

    private var statesSection: some View {
        VStack(spacing: 0) {
            EmptyState(title: "Songs you like will appear here", body: "Tap the heart on a song to save it.", symbol: .favorite)
            EmptyState(title: "Nothing up next", symbol: .queueMusic, clover: true)
            EmptyState(title: "No songs in your music folders", body: "Copy music files into a folder you have added, and they will show up here.", symbol: .musicOff, clover: true) {
                AtticButton("Add a folder", variant: .tonal, leading: .add) {}
            }
            LoadingState("Finding your songs…").frame(height: 160)
            Snackbar("Added to queue").padding(.horizontal, 16)
        }
    }

    private var sheetsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(spacing: 0) {
                SheetHandle()
                SheetHeader(title: "Lights Over the Water", subtitle: "Mira Okafor") { Cover(GallerySamples.cover(0)) }
                SheetDivider()
                SheetRow(.playlistPlay, "Play next") {}
                SheetRow(.queueMusic, "Add to queue") {}
                SheetRow(.check, "Chosen", isSelected: true) {}
                SheetTitle("Add to playlist")
                ListGroup {
                    CollectionRow(title: "Long drive", subtitle: "25 songs", action: {}) { Mosaic((0..<4).map(GallerySamples.cover)) }
                    CollectionRow(title: "Focus", subtitle: "39 songs", action: {}) { Cover(GallerySamples.cover(3)) }
                }
                .groupCellColor(scheme.surfaceContainerHigh)
            }
            .padding(.bottom, 32)
            .background(scheme.surfaceContainerLow, in: UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28, style: .circular))
            VStack(alignment: .leading, spacing: 0) {
                SyncedLyricLine("the porch light hums", state: .sung)
                SyncedLyricLine("carry what we couldn't say", state: .current)
                SyncedLyricLine("lights over the water", state: .upcoming)
            }
            .padding(24)
            .background(scheme.surface)
            .fromArt(seed: GallerySamples.seeds[0])
        }
    }
}

private struct SwatchGrid: View {
    let roles: [(String, Color, Color)]

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
            ForEach(roles, id: \.0) { name, color, ink in
                Text(name)
                    .atticFont(.labelMedium)
                    .foregroundStyle(ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(color, in: RoundedRectangle(cornerRadius: AtticRadius.medium, style: .circular))
                    .overlay(RoundedRectangle(cornerRadius: AtticRadius.medium, style: .circular).strokeBorder(.gray.opacity(0.25), lineWidth: 0.5))
            }
        }
        .padding(.horizontal, 16)
    }
}

/// A slice of Now Playing in a cover's scheme.
private struct FromArtSample: View {
    @Environment(\.atticScheme) private var scheme
    @Binding var isPlaying: Bool
    @Binding var isLiked: Bool
    let cover: Image

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Cover(cover, size: 72)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Lights Over the Water").atticFont(.titleLargeEmphasized).lineLimit(1)
                    Text("Mira Okafor").atticFont(.bodyLarge).foregroundStyle(scheme.onSurfaceVariant)
                }
            }
            Transport(isPlaying: isPlaying, onPrevious: {}, onPlayPause: { isPlaying.toggle() }, onNext: {})
            ToolsRow {
                ShuffleToggle(isOn: true) {}
                RepeatToggle(mode: .off) {}
                HeartButton(isFavourite: isLiked) { isLiked.toggle() }
                AtticIconButton(.queueMusic, label: "Open queue") {}
            }
            SwatchGrid(roles: [
                ("primary", scheme.primary, scheme.onPrimary),
                ("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer),
                ("surfaceContainer", scheme.surfaceContainer, scheme.onSurface),
                ("tertiaryContainer", scheme.tertiaryContainer, scheme.onTertiaryContainer),
            ])
            .padding(.horizontal, -16)
        }
        .padding(16)
        .background(scheme.surface, in: RoundedRectangle(cornerRadius: AtticRadius.extraLarge, style: .circular))
        .padding(.horizontal, 16)
    }
}

/// Covers drawn in code, so the gallery needs no pictures.
private enum GallerySamples {
    struct GridItemSample: Identifiable {
        let id: Int
        let title: String
    }

    static let seeds: [UIColor] = [
        UIColor(atticRgb: 0xF07858), UIColor(atticRgb: 0x2B2A6B), UIColor(atticRgb: 0xD8402F),
        UIColor(atticRgb: 0x2E9E7A), UIColor(atticRgb: 0xE6B93A), UIColor(atticRgb: 0x9B4FC4),
    ]
    static let albums = ["Paper Lanterns", "Night Atlas", "Patang", "Glasshouse", "Second Bus Home", "Platform Ettu"]
    static let artists = ["Mira Okafor", "Ilan Reyes", "Devika Rao", "Tomas Lind", "Anouk Vale", "Kei Morita"]
    static let gridItems = (0..<6).map { GridItemSample(id: $0, title: albums[$0]) }

    private static let images: [Image] = seeds.map { seed in
        let size = CGSize(width: 240, height: 240)
        let image = UIGraphicsImageRenderer(size: size).image { context in
            let cg = context.cgContext
            let hct = Hct(seed)
            let top = UIColor(atticRgb: Hct(hue: hct.hue, chroma: 60, tone: 70).rgb)
            let bottom = UIColor(atticRgb: Hct(hue: hct.hue + 40, chroma: 50, tone: 25).rgb)
            let sun = UIColor(atticRgb: Hct(hue: hct.hue + 60, chroma: 60, tone: 85).rgb)
            let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [top.cgColor, bottom.cgColor] as CFArray, locations: [0, 1])!
            cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 0, y: size.height), options: [])
            sun.setFill()
            cg.fillEllipse(in: CGRect(x: 60, y: 50, width: 120, height: 120))
            bottom.setFill()
            cg.fill(CGRect(x: 0, y: 150, width: size.width, height: 90))
        }
        return Image(uiImage: image)
    }

    static func cover(_ index: Int) -> Image {
        images[((index % images.count) + images.count) % images.count]
    }
}

#Preview("Kit gallery, light") {
    KitGallery(mode: .light)
}

#Preview("Kit gallery, dark") {
    KitGallery(mode: .dark)
}
