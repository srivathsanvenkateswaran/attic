import SwiftUI

/// A settings section heading: primary, 24 above, 8 below, 32 in from the edges.
public struct SettingsSection: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String

    public init(_ title: String) {
        self.title = title
    }

    public var body: some View {
        Text(title)
            .atticFont(.labelLarge.weight(.semibold))
            .foregroundStyle(scheme.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 24, leading: 32, bottom: 8, trailing: 32))
            .accessibilityAddTraits(.isHeader)
    }
}

/// A settings row: an optional symbol in a 40pt tonal circle, a title over an optional summary, and a
/// trailing control. At least 64 tall. Put it in a `ListGroup`.
public struct SettingsRow<Trailing: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let summary: String?
    private let symbol: AtticSymbol?
    private let trailing: Trailing

    public init(title: String, summary: String? = nil, symbol: AtticSymbol? = nil, @ViewBuilder trailing: () -> Trailing) {
        self.title = title
        self.summary = summary
        self.symbol = symbol
        self.trailing = trailing()
    }

    public var body: some View {
        HStack(alignment: .center, spacing: 16) {
            if let symbol {
                SymbolIcon(symbol, size: 20)
                    .foregroundStyle(scheme.onSecondaryContainer)
                    .frame(width: 40, height: 40)
                    .background(scheme.secondaryContainer, in: Circle())
            }
            VStack(alignment: .leading, spacing: 0) {
                Text(title).atticFont(.bodyLarge.weight(.medium)).foregroundStyle(scheme.onSurface)
                if let summary, !summary.isEmpty {
                    Text(summary).atticFont(.bodyMedium).foregroundStyle(scheme.onSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            trailing
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: AtticSpacing.rowMinHeight)
        .contentShape(Rectangle())
    }
}

public extension SettingsRow where Trailing == EmptyView {
    init(title: String, summary: String? = nil, symbol: AtticSymbol? = nil) {
        self.init(title: title, summary: summary, symbol: symbol) { EmptyView() }
    }
}

/// A settings row with a switch. Tapping anywhere on the row toggles it.
public struct SettingsToggleRow: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let summary: String?
    private let symbol: AtticSymbol?
    @Binding private var isOn: Bool

    public init(title: String, summary: String? = nil, symbol: AtticSymbol? = nil, isOn: Binding<Bool>) {
        self.title = title
        self.summary = summary
        self.symbol = symbol
        _isOn = isOn
    }

    public var body: some View {
        Button {
            isOn.toggle()
        } label: {
            SettingsRow(title: title, summary: summary, symbol: symbol) {
                Toggle("", isOn: $isOn).labelsHidden().tint(scheme.primary).allowsHitTesting(false)
            }
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(summary.map { "\(title), \($0)" } ?? title)
        .accessibilityValue(isOn ? "1" : "0")
        .accessibilityAddTraits(.isToggle)
    }
}

/// One of a set of choices: the row ends in a tick while it is the one chosen.
public struct SettingsChoiceRow: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let summary: String?
    private let isSelected: Bool
    private let action: () -> Void

    public init(title: String, summary: String? = nil, isSelected: Bool, action: @escaping () -> Void) {
        self.title = title
        self.summary = summary
        self.isSelected = isSelected
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            SettingsRow(title: title, summary: summary) {
                SymbolIcon(.check).foregroundStyle(scheme.primary).opacity(isSelected ? 1 : 0)
            }
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(summary.map { "\(title), \($0)" } ?? title)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

/// A settings row that opens something or does something: it ends in a chevron unless `showsChevron` is false.
public struct SettingsNavigationRow: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let summary: String?
    private let symbol: AtticSymbol?
    private let showsChevron: Bool
    private let action: () -> Void

    public init(title: String, summary: String? = nil, symbol: AtticSymbol? = nil, showsChevron: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.summary = summary
        self.symbol = symbol
        self.showsChevron = showsChevron
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            SettingsRow(title: title, summary: summary, symbol: symbol) {
                if showsChevron {
                    SymbolIcon(.chevronRight).foregroundStyle(scheme.onSurfaceVariant)
                }
            }
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(summary.map { "\(title), \($0)" } ?? title)
        .accessibilityAddTraits(.isButton)
    }
}

/// A paragraph in a settings group.
public struct SettingsNote: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String

    public init(_ text: String) {
        self.text = text
    }

    public var body: some View {
        Text(text)
            .atticFont(.bodyMedium)
            .foregroundStyle(scheme.onSurfaceVariant)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
    }
}

/// The large title a page under Settings opens with: 36pt emphasised, 16 at the sides, 8 above.
public struct LargeTitle: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String
    private let bottom: CGFloat

    public init(_ text: String, bottom: CGFloat = 0) {
        self.text = text
        self.bottom = bottom
    }

    public var body: some View {
        Text(text)
            .atticFont(.displaySmallEmphasized.tracking(-0.4))
            .foregroundStyle(scheme.onSurface)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 8, leading: AtticSpacing.screenMargin, bottom: bottom, trailing: AtticSpacing.screenMargin))
            .accessibilityAddTraits(.isHeader)
    }
}
