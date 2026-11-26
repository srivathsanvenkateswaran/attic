import SwiftUI

/// The header block of an album, artist or playlist: the surface container with 32pt bottom corners,
/// running up to the top edge. The title leads (45pt for an artist), then an optional byline, the meta
/// lines, and the 208pt cover with the play cluster beside it: Play, and under it shuffle and add to
/// playlist.
///
/// Wrap it in `.fromArt(seed:)` for a collection with a cover. The detail bar floats over its top 64
/// points, so the block starts its text 68 below the status bar.
///
/// Put it first in a scroll view that keeps the top safe area. The block's fill runs on above the
/// header, under the status bar and into the gap a pull-down opens; the scroll view must not ignore the
/// top safe area, or the title starts under the bar.
public struct CollectionHeader<CoverArt: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let isArtist: Bool
    private let byline: String?
    private let bylineImage: Image?
    private let onByline: (() -> Void)?
    private let meta: [String]
    private let name: String
    private let isPlaying: Bool
    private let shuffleOn: Bool
    private let isEnabled: Bool
    private let onPlay: () -> Void
    private let onShuffle: () -> Void
    private let onAdd: (() -> Void)?
    private let cover: CoverArt

    /// - Parameters:
    ///   - byline: an album's artist, drawn with a small cookie and opening the artist with `onByline`.
    ///   - meta: lines under the title: "Album • 2025", "8 songs, 30 min".
    ///   - name: the collection's name, for the buttons' labels ("Shuffle Paper Lanterns").
    ///   - isPlaying: this collection is what is playing, so Play reads Pause and squares off.
    ///   - onAdd: add to a playlist; nil leaves the button out.
    public init(
        title: String,
        isArtist: Bool = false,
        byline: String? = nil,
        bylineImage: Image? = nil,
        onByline: (() -> Void)? = nil,
        meta: [String] = [],
        name: String? = nil,
        isPlaying: Bool = false,
        shuffleOn: Bool = false,
        isEnabled: Bool = true,
        onPlay: @escaping () -> Void,
        onShuffle: @escaping () -> Void,
        onAdd: (() -> Void)? = nil,
        @ViewBuilder cover: () -> CoverArt
    ) {
        self.title = title
        self.isArtist = isArtist
        self.byline = byline
        self.bylineImage = bylineImage
        self.onByline = onByline
        self.meta = meta
        self.name = name ?? title
        self.isPlaying = isPlaying
        self.shuffleOn = shuffleOn
        self.isEnabled = isEnabled
        self.onPlay = onPlay
        self.onShuffle = onShuffle
        self.onAdd = onAdd
        self.cover = cover()
    }

    /// The side of the cover.
    public static var coverSize: CGFloat { 208 }

    /// How far the fill runs above the header: past the status bar and any pull-down.
    private static var fillAbove: CGFloat { 1000 }

    public var body: some View {
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: 0,
            bottomLeadingRadius: AtticRadius.extraLargeIncreased,
            bottomTrailingRadius: AtticRadius.extraLargeIncreased,
            topTrailingRadius: 0,
            style: .circular
        )
        VStack(alignment: .leading, spacing: 0) {
            titleText
            if let byline { bylineRow(byline) }
            ForEach(meta, id: \.self) { line in
                Text(line)
                    .atticFont(.bodyMedium)
                    .foregroundStyle(scheme.onSurfaceVariant)
                    .padding(.top, 6)
            }
            HStack(spacing: 12) {
                cover.frame(width: Self.coverSize, height: Self.coverSize)
                cluster
            }
            .frame(height: Self.coverSize)
            .padding(.top, 18)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 68, leading: AtticSpacing.screenMargin, bottom: 20, trailing: AtticSpacing.screenMargin))
        .background(scheme.surfaceContainer.clipShape(shape).padding(.top, -Self.fillAbove))
    }

    private var titleText: some View {
        let style: AtticTextStyle = isArtist
            ? .displayMediumEmphasized.lineHeight(48).tracking(-0.8)
            : .headlineLargeEmphasized.lineHeight(36).tracking(-0.4)
        return ViewThatFits(in: .horizontal) {
            Text(title).atticFont(style).lineLimit(1)
            Text(title).atticFont(style.narrowed(75)).lineLimit(3).fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(scheme.onSurface)
        .accessibilityAddTraits(.isHeader)
    }

    private func bylineRow(_ byline: String) -> some View {
        let row = HStack(spacing: 8) {
            Cover(bylineImage, size: 28, shape: .artist, fallback: .person)
            Text(byline)
                .atticFont(.bodyLarge.weight(.semibold))
                .foregroundStyle(onByline != nil ? scheme.primary : scheme.onSurface)
                .lineLimit(1)
        }
        .padding(.top, 10)
        .contentShape(Rectangle())
        return Group {
            if let onByline {
                Button(action: onByline) { row }.buttonStyle(PressStyle(pressedOpacity: 0.7))
            } else {
                row
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(byline)
        .accessibilityAddTraits(onByline != nil ? .isButton : [])
    }

    private var cluster: some View {
        VStack(spacing: 8) {
            playButton
            HStack(spacing: 8) {
                AtticToggleButton(.shuffle, isOn: shuffleOn, label: AtticKitStrings.shuffleNamed(name), medium: true, width: .fill, tonalWhenOff: true, action: onShuffle)
                if let onAdd {
                    AtticIconButton(.playlistAdd, label: AtticKitStrings.addNamedToPlaylist(name), variant: .tonal, medium: true, width: .fill, action: onAdd)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .opacity(isEnabled ? 1 : 0.38)
        .disabled(!isEnabled)
    }

    /// Glyph over label, 48pt corners while idle and 28 (the cover's) while this collection plays.
    private var playButton: some View {
        let shape = RoundedRectangle(cornerRadius: isPlaying ? AtticRadius.extraLarge : AtticRadius.extraExtraLarge, style: .circular)
        return Button(action: onPlay) {
            VStack(spacing: 4) {
                ZStack {
                    SymbolIcon(.pause, size: 40, filled: true).opacity(isPlaying ? 1 : 0)
                    SymbolIcon(.playArrow, size: 40, filled: true).opacity(isPlaying ? 0 : 1)
                }
                Text(isPlaying ? AtticKitStrings.pause : AtticKitStrings.play)
                    .atticFont(.bodyLarge.weight(.semibold))
                    .lineLimit(1)
            }
            .foregroundStyle(scheme.onPrimary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(scheme.primary, in: shape)
            .contentShape(shape)
            .animation(AtticMotion.fastSpatial, value: isPlaying)
        }
        .buttonStyle(PressStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isPlaying ? AtticKitStrings.pause : AtticKitStrings.play)
        .accessibilityAddTraits(.isButton)
    }
}
