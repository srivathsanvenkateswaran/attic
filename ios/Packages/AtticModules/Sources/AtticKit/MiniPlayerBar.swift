import SwiftUI

/// The mini player: a 64pt pill on the high container with the cover, the title over the artist, the
/// heart, the play control in its progress ring, and next. Tapping the cover or the text opens the
/// player; swiping the text towards the leading edge skips to the next song.
///
/// It draws in whatever scheme is around it, so the screen wraps it in `.fromArt(seed:)` and pads it
/// 12 from the edges and 8 above the tab bar.
public struct MiniPlayerBar: View {
    @Environment(\.atticScheme) private var scheme
    @State private var drag: CGFloat = 0
    private let title: String
    private let artist: String
    private let cover: Image?
    private let isPlaying: Bool
    private let isLiked: Bool
    private let progress: Double
    private let onOpen: () -> Void
    private let onPlayPause: () -> Void
    private let onNext: () -> Void
    private let onToggleLike: () -> Void

    public init(
        title: String,
        artist: String,
        cover: Image?,
        isPlaying: Bool,
        isLiked: Bool,
        progress: Double,
        onOpen: @escaping () -> Void,
        onPlayPause: @escaping () -> Void,
        onNext: @escaping () -> Void,
        onToggleLike: @escaping () -> Void
    ) {
        self.title = title
        self.artist = artist
        self.cover = cover
        self.isPlaying = isPlaying
        self.isLiked = isLiked
        self.progress = progress
        self.onOpen = onOpen
        self.onPlayPause = onPlayPause
        self.onNext = onNext
        self.onToggleLike = onToggleLike
    }

    public var body: some View {
        HStack(spacing: 0) {
            Button(action: onOpen) {
                HStack(spacing: 12) {
                    Cover(cover, size: 48, shape: .rounded(AtticRadius.large))
                    VStack(alignment: .leading, spacing: 0) {
                        Text(title)
                            .atticFont(.labelLarge.weight(.semibold))
                            .foregroundStyle(scheme.onSurface)
                            .lineLimit(1)
                        Text(artist)
                            .atticFont(.bodySmall)
                            .foregroundStyle(scheme.onSurfaceVariant)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .offset(x: drag)
                }
                .frame(maxHeight: .infinity)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressStyle(pressedOpacity: 0.7))
            .simultaneousGesture(swipe)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(title), \(artist)")
            .accessibilityHint(AtticKitStrings.openPlayerHint)
            .accessibilityAddTraits(.isButton)
            HeartButton(isFavourite: isLiked, action: onToggleLike)
            PlayButton(isPlaying: isPlaying, progress: progress, action: onPlayPause)
            AtticIconButton(.skipNext, label: AtticKitStrings.next, width: .narrow, filled: true, action: onNext)
                .frame(width: 40)
        }
        .padding(.leading, 8)
        .padding(.trailing, 6)
        .frame(height: AtticSpacing.miniPlayerHeight)
        .background(scheme.surfaceContainerHigh, in: Capsule())
        .contentShape(Capsule())
    }

    /// A sideways drag on the text: past 28% of 200 points it skips, and the text springs back either way.
    private var swipe: some Gesture {
        DragGesture(minimumDistance: 16)
            .onChanged { value in
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                drag = min(0, value.translation.width)
            }
            .onEnded { value in
                if value.translation.width < -56 { onNext() }
                withAnimation(AtticMotion.defaultSpatial) { drag = 0 }
            }
    }
}
