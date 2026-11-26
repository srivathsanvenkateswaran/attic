import SwiftUI

/// A tile: a picture with a title and subtitle under it, in a shelf or grid. `art` draws the picture (a
/// `Cover`, `Mosaic` or `FavouritesArt` with no size, so it fills the tile). `isArtist` centres the
/// text under a cookie. `isCurrent` marks the song that is playing: the title turns primary with the
/// wave before it. `width: nil` fills the cell of a grid.
public struct Tile<Art: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let subtitle: String?
    private let width: CGFloat?
    private let isArtist: Bool
    private let isCurrent: Bool
    private let isPlaying: Bool
    private let titleLines: Int
    private let action: () -> Void
    private let art: Art

    public init(
        title: String,
        subtitle: String? = nil,
        width: CGFloat? = 148,
        isArtist: Bool = false,
        isCurrent: Bool = false,
        isPlaying: Bool = false,
        titleLines: Int = 1,
        action: @escaping () -> Void,
        @ViewBuilder art: () -> Art
    ) {
        self.title = title
        self.subtitle = subtitle
        self.width = width
        self.isArtist = isArtist
        self.isCurrent = isCurrent
        self.isPlaying = isPlaying
        self.titleLines = titleLines
        self.action = action
        self.art = art()
    }

    public var body: some View {
        Button(action: action) {
            VStack(alignment: isArtist ? .center : .leading, spacing: 0) {
                art
                HStack(spacing: 6) {
                    if isCurrent {
                        NowPlayingWave(isPlaying: isPlaying).foregroundStyle(scheme.primary)
                    }
                    Text(title)
                        .atticFont(.titleSmall.tracking(0))
                        .foregroundStyle(isCurrent ? scheme.primary : scheme.onSurface)
                        .lineLimit(titleLines)
                        .multilineTextAlignment(isArtist ? .center : .leading)
                }
                .padding(.top, 8)
                if let subtitle {
                    Text(subtitle)
                        .atticFont(.bodySmall)
                        .foregroundStyle(scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .frame(width: width, alignment: isArtist ? .center : .leading)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: isArtist ? .center : .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(subtitle.map { "\(title), \($0)" } ?? title)
        .accessibilityAddTraits(isCurrent ? [.isButton, .isSelected] : .isButton)
    }
}

/// A shelf: one row of tiles that runs off the trailing edge, 12 apart, 16 in.
public struct Shelf<Content: View>: View {
    private let spacing: CGFloat
    private let content: Content

    public init(spacing: CGFloat = AtticSpacing.shelfGap, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    public var body: some View {
        ScrollView(.horizontal) {
            LazyHStack(alignment: .top, spacing: spacing) { content }
                .padding(.horizontal, AtticSpacing.screenMargin)
        }
        .scrollIndicators(.hidden)
    }
}

/// A grid of tiles: `columns` across, 12 between columns and 20 (16 with three columns) between rows,
/// 16 at the sides. Give each tile `width: nil`.
public struct TileGrid<Item: Identifiable, Cell: View>: View {
    private let items: [Item]
    private let columns: Int
    private let cell: (Item) -> Cell

    public init(_ items: [Item], columns: Int = 2, @ViewBuilder cell: @escaping (Item) -> Cell) {
        self.items = items
        self.columns = max(1, columns)
        self.cell = cell
    }

    public var body: some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: AtticSpacing.shelfGap, alignment: .top), count: columns),
            spacing: columns >= 3 ? 16 : AtticSpacing.gridRowGap
        ) {
            ForEach(items) { item in cell(item) }
        }
        .padding(.horizontal, AtticSpacing.screenMargin)
    }
}

/// Favourites pinned beside Home's covers: a 64pt-wide pill on the tertiary container with the heart
/// and the song count. It reads "Favourites, 42 songs".
public struct FavouritesPill: View {
    @Environment(\.atticScheme) private var scheme
    private let count: String
    private let unit: String
    private let height: CGFloat
    private let action: () -> Void

    public init(count: String, unit: String, height: CGFloat = 200, action: @escaping () -> Void) {
        self.count = count
        self.unit = unit
        self.height = height
        self.action = action
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: AtticRadius.extraLargeIncreased, style: .circular)
        Button(action: action) {
            VStack(spacing: 0) {
                SymbolIcon(.favorite, size: 28, filled: true)
                Spacer(minLength: 0)
                // A count of four figures or more is set narrower, so it stays inside the pill.
                Text(count).atticFont(.titleLargeEmphasized.lineHeight(24).narrowed(count.count > 3 ? 60 : 100)).lineLimit(1).fixedSize()
                Text(unit).atticFont(.labelSmall).lineLimit(1)
            }
            .padding(.top, 18)
            .padding(.bottom, 16)
            .padding(.horizontal, 4)
            .frame(width: 64, height: height)
            .foregroundStyle(scheme.onTertiaryContainer)
            .background(scheme.tertiaryContainer, in: shape)
            .contentShape(shape)
        }
        .buttonStyle(PressStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(AtticKitStrings.favourites), \(count) \(unit)")
        .accessibilityAddTraits(.isButton)
    }
}

/// The caption under one of Home's big covers: its title over its subtitle.
public struct HeroCaption: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let subtitle: String

    public init(title: String, subtitle: String) {
        self.title = title
        self.subtitle = subtitle
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title).atticFont(.titleMedium.weight(.semibold).lineHeight(22)).foregroundStyle(scheme.onSurface).lineLimit(1)
            Text(subtitle).atticFont(.bodySmall).foregroundStyle(scheme.onSurfaceVariant).lineLimit(1)
        }
        .padding(.horizontal, 4)
        .padding(.top, 10)
    }
}

/// One of Home's big covers: a 200pt square with 28pt corners and its caption under it.
public struct HeroTile<Art: View>: View {
    private let title: String
    private let subtitle: String
    private let size: CGFloat
    private let action: () -> Void
    private let art: Art

    public init(title: String, subtitle: String, size: CGFloat = 200, action: @escaping () -> Void, @ViewBuilder art: () -> Art) {
        self.title = title
        self.subtitle = subtitle
        self.size = size
        self.action = action
        self.art = art()
    }

    public var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                art
                    .frame(width: size, height: size)
                    .clipShape(RoundedRectangle(cornerRadius: AtticRadius.extraLarge, style: .circular))
                HeroCaption(title: title, subtitle: subtitle)
            }
            .frame(width: size, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title), \(subtitle)")
        .accessibilityAddTraits(.isButton)
    }
}

/// A card under the player or on a page: the surface container, 28pt corners, 20 padding, and an
/// optional small heading with a symbol ("Lyrics").
public struct InfoCard<Content: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String?
    private let symbol: AtticSymbol?
    private let content: Content

    public init(title: String? = nil, symbol: AtticSymbol? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.symbol = symbol
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let title {
                HStack(spacing: 8) {
                    if let symbol { SymbolIcon(symbol, size: 20) }
                    Text(title).atticFont(.labelLarge.weight(.semibold))
                }
                .foregroundStyle(scheme.onSurfaceVariant)
                .padding(.bottom, 14)
                .accessibilityAddTraits(.isHeader)
            }
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(scheme.surfaceContainer, in: RoundedRectangle(cornerRadius: AtticRadius.extraLarge, style: .circular))
    }
}
