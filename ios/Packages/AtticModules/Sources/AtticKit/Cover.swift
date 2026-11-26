import SwiftUI

/// The outline a cover is cut to.
public enum CoverStyle: Equatable, Sendable {
    /// A rounded square whose corner follows its size: albums, playlists, folders, songs.
    case cover
    /// The nine-lobed cookie: artists.
    case artist
    case circle
    /// No rounding, for a cover inside something that clips it.
    case square
    /// A rounded square with this corner, where the size rule does not apply (the mini player's 16).
    case rounded(CGFloat)

    var shape: AnyShape {
        switch self {
        case .cover: AnyShape(CoverShape())
        case .artist: AnyShape(CookieShape())
        case .circle: AnyShape(Circle())
        case .square: AnyShape(Rectangle())
        case .rounded(let radius): AnyShape(RoundedRectangle(cornerRadius: radius, style: .circular))
        }
    }
}

/// A cover: a square picture, cropped to fill. Give `size` for a fixed cover; leave it nil to fill the
/// width it is given. With no image it shows `fallback` on the highest container.
public struct Cover: View {
    @Environment(\.atticScheme) private var scheme
    private let image: Image?
    private let size: CGFloat?
    private let style: CoverStyle
    private let fallback: AtticSymbol

    public init(_ image: Image?, size: CGFloat? = nil, shape: CoverStyle = .cover, fallback: AtticSymbol = .musicNote) {
        self.image = image
        self.size = size
        self.style = shape
        self.fallback = fallback
    }

    public var body: some View {
        CoverBox(size: size, style: style, background: scheme.surfaceContainerHighest) { side in
            if let image {
                image.resizable().scaledToFill().frame(width: side, height: side).clipped()
            } else {
                SymbolIcon(fallback, size: side * 0.46).foregroundStyle(scheme.onSurfaceVariant)
            }
        }
    }
}

/// A folder or playlist: four covers in a 2×2 grid when there are four or more, else the first one,
/// else the placeholder. Pass covers from different albums.
public struct Mosaic: View {
    @Environment(\.atticScheme) private var scheme
    private let images: [Image]
    private let size: CGFloat?
    private let style: CoverStyle

    public init(_ images: [Image], size: CGFloat? = nil, shape: CoverStyle = .cover) {
        self.images = images
        self.size = size
        self.style = shape
    }

    public var body: some View {
        if images.count < 4 {
            Cover(images.first, size: size, shape: style, fallback: .queueMusic)
        } else {
            CoverBox(size: size, style: style, background: scheme.surfaceContainerHighest) { side in
                let half = side / 2
                VStack(spacing: 0) {
                    ForEach(0..<2, id: \.self) { row in
                        HStack(spacing: 0) {
                            ForEach(0..<2, id: \.self) { column in
                                images[row * 2 + column].resizable().scaledToFill().frame(width: half, height: half).clipped()
                            }
                        }
                    }
                }
            }
        }
    }
}

/// Favourites: a filled heart on the tertiary container, the one place that colour is spent.
public struct FavouritesArt: View {
    @Environment(\.atticScheme) private var scheme
    private let size: CGFloat?
    private let style: CoverStyle

    public init(size: CGFloat? = nil, shape: CoverStyle = .cover) {
        self.size = size
        self.style = shape
    }

    public var body: some View {
        CoverBox(size: size, style: style, background: scheme.tertiaryContainer) { side in
            SymbolIcon(.favorite, size: side * 0.5, filled: true).foregroundStyle(scheme.onTertiaryContainer)
        }
    }
}

/// The square every cover is drawn in: fixed, or as wide as it is given.
private struct CoverBox<Content: View>: View {
    let size: CGFloat?
    let style: CoverStyle
    let background: Color
    @ViewBuilder let content: (CGFloat) -> Content

    var body: some View {
        Group {
            if let size {
                box.frame(width: size, height: size)
            } else {
                box.aspectRatio(1, contentMode: .fit)
            }
        }
        .accessibilityHidden(true)
    }

    private var box: some View {
        GeometryReader { geometry in
            let side = min(geometry.size.width, geometry.size.height)
            ZStack {
                background
                content(side)
            }
            .frame(width: side, height: side)
            .clipShape(style.shape)
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
    }
}
