import SwiftUI
import UIKit

public enum AtticButtonVariant: Sendable {
    case filled, tonal, outlined, text
}

public enum AtticButtonSize: Sendable {
    /// 40 tall.
    case small
    /// 56 tall.
    case medium

    var height: CGFloat { self == .medium ? 56 : 40 }
}

/// What a pressed control looks like: a touch dimmer, back on the fast effects spring.
struct PressStyle: ButtonStyle {
    var pressedOpacity: Double = 0.92

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? pressedOpacity : 1)
            .animation(AtticMotion.fastEffects, value: configuration.isPressed)
    }
}

/// A labelled button: filled, tonal, outlined or text, in two heights, fully round. `squared` is
/// "squared off means on". `tint` recolours a text button, e.g. `scheme.error` for "Delete playlist".
/// `fillsWidth` stretches it across the width it is given.
public struct AtticButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.isEnabled) private var isEnabled
    private let title: String
    private let variant: AtticButtonVariant
    private let size: AtticButtonSize
    private let leading: AtticSymbol?
    private let leadingFilled: Bool
    private let trailing: AtticSymbol?
    private let squared: Bool
    private let tint: Color?
    private let fillsWidth: Bool
    private let action: () -> Void

    public init(
        _ title: String,
        variant: AtticButtonVariant = .filled,
        size: AtticButtonSize = .small,
        leading: AtticSymbol? = nil,
        leadingFilled: Bool = true,
        trailing: AtticSymbol? = nil,
        squared: Bool = false,
        tint: Color? = nil,
        fillsWidth: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.variant = variant
        self.size = size
        self.leading = leading
        self.leadingFilled = leadingFilled
        self.trailing = trailing
        self.squared = squared
        self.tint = tint
        self.fillsWidth = fillsWidth
        self.action = action
    }

    public var body: some View {
        let medium = size == .medium
        let side: CGFloat = variant == .text ? 12 : (medium ? 24 : 16)
        let iconSize: CGFloat = medium ? 24 : 20
        let corner = squared ? (medium ? AtticRadius.large : AtticRadius.medium) : size.height / 2
        let shape = RoundedRectangle(cornerRadius: corner, style: .circular)
        let labelStyle: AtticTextStyle = medium
            ? .labelLarge.sized(16, line: 24).weight(.semibold)
            : .labelLarge.weight(.semibold)
        Button(action: action) {
            HStack(spacing: 8) {
                if let leading { SymbolIcon(leading, size: iconSize, filled: leadingFilled) }
                Text(title).atticFont(labelStyle).lineLimit(1)
                if let trailing { SymbolIcon(trailing, size: iconSize) }
            }
            .padding(.leading, leading != nil ? side - 4 : side)
            .padding(.trailing, trailing != nil ? side - 4 : side)
            .frame(maxWidth: fillsWidth ? .infinity : nil)
            .frame(height: size.height)
            .foregroundStyle(foreground)
            .background(background, in: shape)
            .overlay {
                if variant == .outlined { shape.strokeBorder(scheme.outlineVariant, lineWidth: 1) }
            }
            .contentShape(shape)
            .frame(minHeight: AtticSpacing.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .opacity(isEnabled ? 1 : 0.38)
        .animation(AtticMotion.fastSpatial, value: squared)
    }

    private var background: Color {
        switch variant {
        case .filled: scheme.primary
        case .tonal: scheme.secondaryContainer
        case .outlined, .text: .clear
        }
    }

    private var foreground: Color {
        if let tint { return tint }
        return switch variant {
        case .filled: scheme.onPrimary
        case .tonal: scheme.onSecondaryContainer
        case .outlined: scheme.onSurfaceVariant
        case .text: scheme.primary
        }
    }
}

public enum AtticIconButtonVariant: Sendable {
    /// No container.
    case standard
    case filled, tonal, outlined
}

public enum AtticIconButtonWidth: Sendable {
    case narrow, standard, wide
    /// As wide as the room it is given, for buttons that share a row.
    case fill
}

private func iconButtonSize(medium: Bool, width: AtticIconButtonWidth) -> (width: CGFloat?, height: CGFloat) {
    let height: CGFloat = medium ? 56 : 40
    switch width {
    case .narrow: return (medium ? 48 : 32, height)
    case .standard: return (height, height)
    case .wide: return (medium ? 72 : 52, height)
    case .fill: return (nil, height)
    }
}

extension EnvironmentValues {
    /// The colour of a standard icon button's glyph, when a container (a bar over art) sets one.
    @Entry var atticStandardIconTint: Color? = nil
}

/// An icon button: a 40pt container in a 48pt target, or 56pt with `medium`. `label` is what VoiceOver reads.
public struct AtticIconButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.atticStandardIconTint) private var standardTint
    private let symbol: AtticSymbol
    private let label: String
    private let variant: AtticIconButtonVariant
    private let medium: Bool
    private let width: AtticIconButtonWidth
    private let filled: Bool
    private let tint: Color?
    private let iconSize: CGFloat
    private let action: () -> Void

    public init(
        _ symbol: AtticSymbol,
        label: String,
        variant: AtticIconButtonVariant = .standard,
        medium: Bool = false,
        width: AtticIconButtonWidth = .standard,
        filled: Bool = false,
        tint: Color? = nil,
        iconSize: CGFloat = 24,
        action: @escaping () -> Void
    ) {
        self.symbol = symbol
        self.label = label
        self.variant = variant
        self.medium = medium
        self.width = width
        self.filled = filled
        self.tint = tint
        self.iconSize = iconSize
        self.action = action
    }

    public var body: some View {
        let size = iconButtonSize(medium: medium, width: width)
        Button(action: action) {
            IconButtonFace(
                symbol: symbol,
                iconSize: iconSize,
                filled: filled,
                container: container,
                content: content,
                border: variant == .outlined ? scheme.outlineVariant : nil,
                corner: size.height / 2,
                width: size.width,
                height: size.height
            )
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .opacity(isEnabled ? 1 : 0.38)
        .accessibilityLabel(label)
    }

    private var container: Color {
        switch variant {
        case .filled: scheme.primary
        case .tonal: scheme.secondaryContainer
        case .standard, .outlined: .clear
        }
    }

    private var content: Color {
        if let tint { return tint }
        return switch variant {
        case .filled: scheme.onPrimary
        case .tonal: scheme.onSecondaryContainer
        case .standard, .outlined: standardTint ?? scheme.onSurfaceVariant
        }
    }
}

/// The drawing every icon button shares: a container, a glyph, and a 48pt target round them.
struct IconButtonFace: View {
    let symbol: AtticSymbol
    var iconSize: CGFloat = 24
    let filled: Bool
    let container: Color
    let content: Color
    var border: Color?
    let corner: CGFloat
    let width: CGFloat?
    let height: CGFloat

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: corner, style: .circular)
        SymbolIcon(symbol, size: iconSize, filled: filled)
            .foregroundStyle(content)
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil)
            .background(container, in: shape)
            .overlay {
                if let border { shape.strokeBorder(border, lineWidth: 1) }
            }
            .frame(minWidth: AtticSpacing.touchTarget, minHeight: AtticSpacing.touchTarget)
            .contentShape(Rectangle())
    }
}

/// A toggle: round while off, squared off and filled with primary while on, the glyph filled. The corner
/// moves on the fast spatial spring, the colours on fast effects. `tonalWhenOff` gives it the tonal
/// container while off, as in a collection header.
public struct AtticToggleButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.atticStandardIconTint) private var standardTint
    private let symbol: AtticSymbol
    private let isOn: Bool
    private let label: String
    private let medium: Bool
    private let width: AtticIconButtonWidth
    private let tonalWhenOff: Bool
    private let action: () -> Void

    public init(
        _ symbol: AtticSymbol,
        isOn: Bool,
        label: String,
        medium: Bool = false,
        width: AtticIconButtonWidth = .standard,
        tonalWhenOff: Bool = false,
        action: @escaping () -> Void
    ) {
        self.symbol = symbol
        self.isOn = isOn
        self.label = label
        self.medium = medium
        self.width = width
        self.tonalWhenOff = tonalWhenOff
        self.action = action
    }

    public var body: some View {
        let size = iconButtonSize(medium: medium, width: width)
        let corner = isOn ? AtticRadius.onCorner(height: size.height) : size.height / 2
        let container = isOn ? scheme.primary : (tonalWhenOff ? scheme.secondaryContainer : scheme.primary.opacity(0))
        let content = isOn ? scheme.onPrimary : (tonalWhenOff ? scheme.onSecondaryContainer : (standardTint ?? scheme.onSurfaceVariant))
        Button(action: action) {
            IconButtonFace(
                symbol: symbol,
                filled: isOn,
                container: container,
                content: content,
                corner: corner,
                width: size.width,
                height: size.height
            )
            .animation(AtticMotion.fastSpatial, value: corner)
            .animation(AtticMotion.fastEffects, value: isOn)
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .opacity(isEnabled ? 1 : 0.38)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

/// The favourite heart: an outline, then filled in primary, with no container. It pops from 0.8 to 1
/// on the fast spatial spring when it changes, and reads "Add to favourites" or "Remove from favourites".
public struct HeartButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.atticStandardIconTint) private var standardTint
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var scale: CGFloat = 1
    private let isFavourite: Bool
    private let action: () -> Void

    public init(isFavourite: Bool, action: @escaping () -> Void) {
        self.isFavourite = isFavourite
        self.action = action
    }

    public var body: some View {
        Button {
            if !isFavourite { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
            action()
        } label: {
            SymbolIcon(.favorite, filled: isFavourite)
                .foregroundStyle(isFavourite ? scheme.primary : (standardTint ?? scheme.onSurfaceVariant))
                .scaleEffect(scale)
                .frame(width: AtticSpacing.touchTarget, height: AtticSpacing.touchTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityLabel(isFavourite ? AtticKitStrings.favouriteRemove : AtticKitStrings.favouriteAdd)
        .onChange(of: isFavourite) {
            guard !reduceMotion else { return }
            var instant = Transaction()
            instant.disablesAnimations = true
            withTransaction(instant) { scale = 0.8 }
            withAnimation(AtticMotion.fastSpatial) { scale = 1 }
        }
    }
}

/// The extended action button at the foot of a list ("New playlist"): a glyph and a label on the primary
/// container. Not `expanded`, it shrinks to the glyph, for a list that has scrolled.
public struct ExtendedFab: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let symbol: AtticSymbol
    private let expanded: Bool
    private let action: () -> Void

    public init(_ title: String, symbol: AtticSymbol, expanded: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.symbol = symbol
        self.expanded = expanded
        self.action = action
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: AtticRadius.large, style: .circular)
        Button(action: action) {
            HStack(spacing: 12) {
                SymbolIcon(symbol)
                if expanded {
                    Text(title).atticFont(.titleMedium.weight(.semibold)).lineLimit(1)
                        .transition(.opacity.combined(with: .scale(scale: 0.8, anchor: .leading)))
                }
            }
            .padding(.leading, 16)
            .padding(.trailing, expanded ? 20 : 16)
            .frame(height: 56)
            .foregroundStyle(scheme.onPrimaryContainer)
            .background(scheme.primaryContainer, in: shape)
            .contentShape(shape)
        }
        .buttonStyle(PressStyle())
        .animation(AtticMotion.fastSpatial, value: expanded)
        .accessibilityLabel(title)
    }
}

/// A split button: an action and its menu, 2 apart, on the tonal container. The outer corners are round
/// and the inner ones 4. The menu half shows a down arrow and opens `menu`.
public struct SplitButton<MenuContent: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let leading: AtticSymbol?
    private let menuLabel: String
    private let action: () -> Void
    private let menu: MenuContent

    public init(
        _ title: String,
        leading: AtticSymbol? = nil,
        menuLabel: String,
        action: @escaping () -> Void,
        @ViewBuilder menu: () -> MenuContent
    ) {
        self.title = title
        self.leading = leading
        self.menuLabel = menuLabel
        self.action = action
        self.menu = menu()
    }

    public var body: some View {
        let start = UnevenRoundedRectangle(topLeadingRadius: 20, bottomLeadingRadius: 20, bottomTrailingRadius: 4, topTrailingRadius: 4, style: .circular)
        let end = UnevenRoundedRectangle(topLeadingRadius: 4, bottomLeadingRadius: 4, bottomTrailingRadius: 20, topTrailingRadius: 20, style: .circular)
        HStack(spacing: 2) {
            Button(action: action) {
                HStack(spacing: 8) {
                    if let leading { SymbolIcon(leading, size: 20) }
                    Text(title).atticFont(.labelLarge.weight(.semibold)).lineLimit(1)
                }
                .padding(.leading, 16)
                .padding(.trailing, 12)
                .frame(height: 40)
                .foregroundStyle(scheme.onSecondaryContainer)
                .background(scheme.secondaryContainer, in: start)
                .contentShape(start)
            }
            .buttonStyle(PressStyle())
            Menu {
                menu
            } label: {
                SymbolIcon(.keyboardArrowDown, size: 20)
                    .padding(.leading, 10)
                    .padding(.trailing, 12)
                    .frame(height: 40)
                    .foregroundStyle(scheme.onSecondaryContainer)
                    .background(scheme.secondaryContainer, in: end)
                    .contentShape(end)
            }
            .accessibilityLabel(menuLabel)
        }
    }
}

/// Buttons in a row, 8 apart.
public struct ButtonRow<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        HStack(spacing: AtticSpacing.chipGap) { content }
    }
}
