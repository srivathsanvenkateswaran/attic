import SwiftUI
import UIKit

/// An empty state: a picture on the secondary container holding a symbol, the title in the emphasised
/// headline, the body, and an optional action. `clover` draws the picture as the 128pt clover, for
/// "nothing here yet". A nil `symbol` leaves the picture out; a nil `body` leaves the title alone.
public struct EmptyState<Action: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let message: String?
    private let symbol: AtticSymbol?
    private let clover: Bool
    private let action: Action

    public init(
        title: String,
        body: String? = nil,
        symbol: AtticSymbol? = nil,
        clover: Bool = false,
        @ViewBuilder action: () -> Action
    ) {
        self.title = title
        self.message = body
        self.symbol = symbol
        self.clover = clover
        self.action = action()
    }

    public var body: some View {
        VStack(spacing: 0) {
            if let symbol {
                let side: CGFloat = clover ? 128 : 112
                SymbolIcon(symbol, size: 48)
                    .foregroundStyle(scheme.onSecondaryContainer)
                    .frame(width: side, height: side)
                    .background(
                        scheme.secondaryContainer,
                        in: clover ? AnyShape(CloverShape()) : AnyShape(RoundedRectangle(cornerRadius: 36, style: .circular))
                    )
                    .padding(.bottom, 24)
            }
            VStack(spacing: 8) {
                Text(title)
                    .atticFont(.headlineSmallEmphasized)
                    .foregroundStyle(scheme.onSurface)
                if let message {
                    Text(message)
                        .atticFont(.bodyMedium)
                        .foregroundStyle(scheme.onSurfaceVariant)
                }
            }
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .combine)
            if Action.self != EmptyView.self {
                action.padding(.top, 24)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
        .padding(.vertical, 48)
    }
}

public extension EmptyState where Action == EmptyView {
    init(title: String, body: String? = nil, symbol: AtticSymbol? = nil, clover: Bool = false) {
        self.init(title: title, body: body, symbol: symbol, clover: clover) { EmptyView() }
    }
}

/// A loading state: the loading indicator centred in the room it is given, with an optional line under
/// it. It waits 400 ms before it shows, so a quick load never flashes it.
public struct LoadingState: View {
    @Environment(\.atticScheme) private var scheme
    @State private var shown = false
    private let label: String?

    public init(_ label: String? = nil) {
        self.label = label
    }

    public var body: some View {
        VStack(spacing: 16) {
            LoadingIndicator()
            if let label {
                Text(label)
                    .atticFont(.bodyLarge)
                    .foregroundStyle(scheme.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
        }
        .opacity(shown ? 1 : 0)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label ?? AtticKitStrings.loading)
        .task {
            try? await Task.sleep(for: .seconds(AtticMotion.loadingDelay))
            withAnimation(AtticMotion.defaultEffects) { shown = true }
        }
    }
}

/// The snackbar: the inverse surface, 8pt corners, text only.
public struct Snackbar: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String

    public init(_ text: String) {
        self.text = text
    }

    public var body: some View {
        Text(text)
            .atticFont(.bodyMedium)
            .foregroundStyle(scheme.inverseOnSurface)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(scheme.inverseSurface, in: RoundedRectangle(cornerRadius: AtticRadius.small, style: .circular))
    }
}

private struct SnackbarModifier: ViewModifier {
    @Binding var message: String?
    let bottomInset: CGFloat

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let message {
                    Snackbar(message)
                        .padding(.horizontal, 16)
                        .padding(.bottom, bottomInset + 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .id(message)
                        .task(id: message) {
                            UIAccessibility.post(notification: .announcement, argument: message)
                            try? await Task.sleep(for: .seconds(AtticMotion.snackbarDuration))
                            if !Task.isCancelled { self.message = nil }
                        }
                }
            }
            .animation(AtticMotion.defaultSpatial, value: message)
    }
}

public extension View {
    /// Shows a snackbar over this view's bottom edge while `message` is set: 16 above `bottomInset`
    /// (the dock's height), for 3 seconds, then sets `message` back to nil. It is announced to VoiceOver.
    func snackbar(_ message: Binding<String?>, bottomInset: CGFloat = 0) -> some View {
        modifier(SnackbarModifier(message: message, bottomInset: bottomInset))
    }
}

// MARK: - Sheets

/// A sheet's drag handle: 32×4 in outline, 16 above and 12 below.
public struct SheetHandle: View {
    @Environment(\.atticScheme) private var scheme

    public init() {}

    public var body: some View {
        Capsule()
            .fill(scheme.outline)
            .frame(width: 32, height: 4)
            .padding(.top, 16)
            .padding(.bottom, 12)
            .frame(maxWidth: .infinity)
            .accessibilityHidden(true)
    }
}

/// A sheet's title when it is not about one song: "Add to playlist".
public struct SheetTitle: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String

    public init(_ text: String) {
        self.text = text
    }

    public var body: some View {
        Text(text)
            .atticFont(.headlineSmallEmphasized)
            .foregroundStyle(scheme.onSurface)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 4, leading: 24, bottom: 12, trailing: 24))
            .accessibilityAddTraits(.isHeader)
    }
}

/// A sheet's song header: a 56pt picture, then title over subtitle.
public struct SheetHeader<Art: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let subtitle: String
    private let art: Art

    public init(title: String, subtitle: String, @ViewBuilder art: () -> Art) {
        self.title = title
        self.subtitle = subtitle
        self.art = art()
    }

    public var body: some View {
        HStack(spacing: 16) {
            art.frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 0) {
                Text(title).atticFont(.bodyLarge.weight(.semibold).lineHeight(22)).foregroundStyle(scheme.onSurface).lineLimit(1)
                Text(subtitle).atticFont(.bodyMedium).foregroundStyle(scheme.onSurfaceVariant).lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 4, leading: 24, bottom: 12, trailing: 24))
        .accessibilityElement(children: .combine)
    }
}

/// A sheet row: a 24pt symbol and a label, at least 56 tall. `isSelected` is the current choice in a
/// picker: primary and heavier. `tint` recolours both, e.g. `scheme.error` for a destructive row.
public struct SheetRow: View {
    @Environment(\.atticScheme) private var scheme
    private let symbol: AtticSymbol
    private let label: String
    private let isSelected: Bool
    private let tint: Color?
    private let action: () -> Void

    public init(_ symbol: AtticSymbol, _ label: String, isSelected: Bool = false, tint: Color? = nil, action: @escaping () -> Void) {
        self.symbol = symbol
        self.label = label
        self.isSelected = isSelected
        self.tint = tint
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                SymbolIcon(symbol, filled: isSelected)
                    .foregroundStyle(tint ?? (isSelected ? scheme.primary : scheme.onSurfaceVariant))
                Text(label)
                    .atticFont(.bodyLarge.weight(isSelected ? .semibold : .regular))
                    .foregroundStyle(tint ?? (isSelected ? scheme.primary : scheme.onSurface))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 24)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.6))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// The rule under a sheet header.
public struct SheetDivider: View {
    @Environment(\.atticScheme) private var scheme

    public init() {}

    public var body: some View {
        Rectangle()
            .fill(scheme.outlineVariant)
            .frame(height: 1)
            .padding(.horizontal, 24)
            .padding(.vertical, 8)
            .accessibilityHidden(true)
    }
}

// MARK: - Lyrics

/// Where a lyric line stands against playback.
public enum LyricLineState: Sendable {
    case sung, current, upcoming
}

/// One line of synced lyrics on the lyrics screen. The current line is 34/40, bold and rounded, in
/// primary; the others 24/32 medium, sung lines in outline and those to come in on-surface-variant.
/// Tapping it calls `action`, for seeking to the line.
public struct SyncedLyricLine: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String
    private let state: LyricLineState
    private let action: () -> Void

    public init(_ text: String, state: LyricLineState, action: @escaping () -> Void = {}) {
        self.text = text
        self.state = state
        self.action = action
    }

    public var body: some View {
        let current = state == .current
        let style: AtticTextStyle = current
            ? AtticTextStyle(size: 34, lineHeight: 40, weight: 700, tracking: -0.3, isRound: true, relativeTo: .title1)
            : AtticTextStyle(size: 24, lineHeight: 32, weight: 500, relativeTo: .title2)
        let color = switch state {
        case .sung: scheme.outline
        case .current: scheme.primary
        case .upcoming: scheme.onSurfaceVariant
        }
        Button(action: action) {
            Text(text)
                .atticFont(style)
                .foregroundStyle(color)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, current ? 4 : 0)
                .padding(.bottom, current ? 20 : 14)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .animation(AtticMotion.defaultEffects, value: state)
        .accessibilityAddTraits(current ? .isSelected : [])
    }
}

/// One line of lyrics in a card: 22/28 emphasised. The current line and plain text are on-surface; a
/// line to come is outline.
public struct LyricLine: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String
    private let state: LyricLineState
    private let maxLines: Int?

    public init(_ text: String, state: LyricLineState = .current, maxLines: Int? = nil) {
        self.text = text
        self.state = state
        self.maxLines = maxLines
    }

    public var body: some View {
        let color = switch state {
        case .sung: scheme.onSurfaceVariant
        case .current: scheme.onSurface
        case .upcoming: scheme.outline
        }
        Text(text)
            .atticFont(.titleLargeEmphasized)
            .foregroundStyle(color)
            .lineLimit(maxLines)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 8)
    }
}
