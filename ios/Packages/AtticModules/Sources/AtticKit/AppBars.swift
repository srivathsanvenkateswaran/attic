import SwiftUI

/// One destination in the tab bar.
public struct AtticTabItem: Identifiable, Equatable, Sendable {
    public let title: String
    public let symbol: AtticSymbol
    public var id: String { title }

    public init(title: String, symbol: AtticSymbol) {
        self.title = title
        self.symbol = symbol
    }
}

/// The tab bar: 64 tall on the surface container, which also fills the home indicator's inset under it.
/// The selected tab has a pill indicator in the secondary container, its glyph filled and its label
/// heavier. Tapping the selected tab calls `onReselect`, for popping to the root.
public struct AtticTabBar: View {
    @Environment(\.atticScheme) private var scheme
    private let items: [AtticTabItem]
    @Binding private var selection: Int
    private let onReselect: (Int) -> Void

    public init(items: [AtticTabItem], selection: Binding<Int>, onReselect: @escaping (Int) -> Void = { _ in }) {
        self.items = items
        _selection = selection
        self.onReselect = onReselect
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(items.indices, id: \.self) { index in
                tab(index)
            }
        }
        .frame(height: AtticSpacing.tabBarHeight)
        .background(scheme.surfaceContainer.ignoresSafeArea(edges: .bottom))
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isTabBar)
    }

    private func tab(_ index: Int) -> some View {
        let selected = index == selection
        let item = items[index]
        return Button {
            if selected { onReselect(index) } else { selection = index }
        } label: {
            VStack(spacing: 4) {
                SymbolIcon(item.symbol, filled: selected)
                    .foregroundStyle(selected ? scheme.onSecondaryContainer : scheme.onSurfaceVariant)
                    .frame(width: 56, height: 32)
                    .background(selected ? scheme.secondaryContainer : scheme.secondaryContainer.opacity(0), in: Capsule())
                Text(item.title)
                    .atticFont(.labelMedium.weight(selected ? .bold : .medium))
                    .foregroundStyle(selected ? scheme.onSurface : scheme.onSurfaceVariant)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
            .animation(AtticMotion.fastEffects, value: selected)
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .accessibilityLabel(item.title)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}

private func lerp(_ from: CGFloat, _ to: CGFloat, _ fraction: CGFloat) -> CGFloat {
    from + (to - from) * fraction
}

/// A tab root's bar: one row, a large rounded title and trailing actions, settings last. As `scrolled`
/// goes from 0 to 1 the title shrinks from 36 to 22 and the bar takes the surface container colour up
/// to the top edge. `leading` sits before the title (Home's mark).
public struct RootAppBar<Leading: View, Actions: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let scrolled: CGFloat
    private let leading: Leading
    private let actions: Actions

    public init(
        _ title: String,
        scrolled: CGFloat = 0,
        @ViewBuilder leading: () -> Leading,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.scrolled = max(0, min(1, scrolled))
        self.leading = leading()
        self.actions = actions()
    }

    public var body: some View {
        let style = AtticTextStyle.displaySmallEmphasized
            .sized(lerp(36, 22, scrolled), line: lerp(44, 28, scrolled))
            .tracking(lerp(-0.4, 0, scrolled))
        HStack(spacing: 0) {
            HStack(spacing: Leading.self == EmptyView.self ? 0 : 10) {
                leading
                Text(title)
                    .atticFont(style)
                    .foregroundStyle(scheme.onSurface)
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            actions
        }
        .padding(.leading, AtticSpacing.screenMargin)
        .padding(.trailing, 8)
        .frame(height: lerp(AtticSpacing.rootBarHeight, AtticSpacing.appBarHeight, scrolled))
        .background(
            scheme.surfaceContainer
                .opacity(scrolled > 0.5 ? 1 : 0)
                .ignoresSafeArea(edges: .top)
                .animation(AtticMotion.defaultEffects, value: scrolled > 0.5)
        )
    }
}

public extension RootAppBar where Leading == EmptyView {
    init(_ title: String, scrolled: CGFloat = 0, @ViewBuilder actions: () -> Actions) {
        self.init(title, scrolled: scrolled, leading: { EmptyView() }, actions: actions)
    }
}

public extension RootAppBar where Leading == EmptyView, Actions == EmptyView {
    init(_ title: String, scrolled: CGFloat = 0) {
        self.init(title, scrolled: scrolled, leading: { EmptyView() }, actions: { EmptyView() })
    }
}

/// The colours a detail bar sits in.
public enum AppBarSurface: Sendable {
    /// Clear, with the title.
    case plain
    /// The surface container up to the top edge, with the title.
    case scrolled
    /// Clear with no title, each button on a scrim: floating over a header block.
    case overArt
}

/// A detail bar: back, a 22pt title, actions. 64 tall.
public struct DetailAppBar<Actions: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let surface: AppBarSurface
    private let backSymbol: AtticSymbol
    private let backLabel: String
    private let onBack: () -> Void
    private let actions: Actions

    public init(
        _ title: String,
        surface: AppBarSurface = .plain,
        backSymbol: AtticSymbol = .arrowBack,
        backLabel: String? = nil,
        onBack: @escaping () -> Void,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.surface = surface
        self.backSymbol = backSymbol
        self.backLabel = backLabel ?? AtticKitStrings.back
        self.onBack = onBack
        self.actions = actions()
    }

    public var body: some View {
        let over = surface == .overArt
        HStack(spacing: 0) {
            scrim(over) {
                AtticIconButton(backSymbol, label: backLabel, tint: over ? scheme.onSurface : nil, action: onBack)
            }
            Text(title)
                .atticFont(.titleLarge.weight(.medium))
                .foregroundStyle(scheme.onSurface)
                .lineLimit(1)
                .padding(.horizontal, 4)
                .frame(maxWidth: .infinity, alignment: .leading)
                .opacity(over ? 0 : 1)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHidden(over)
            if Actions.self != EmptyView.self {
                scrim(over) { HStack(spacing: 0) { actions } }
            }
        }
        .padding(.horizontal, 4)
        .frame(height: AtticSpacing.appBarHeight)
        .background(
            scheme.surfaceContainer
                .opacity(surface == .scrolled ? 1 : 0)
                .ignoresSafeArea(edges: .top)
        )
        .animation(AtticMotion.defaultEffects, value: surface)
    }

    @ViewBuilder
    private func scrim<Content: View>(_ over: Bool, @ViewBuilder content: () -> Content) -> some View {
        if over {
            content()
                .environment(\.atticStandardIconTint, scheme.onSurface)
                .background(scheme.surface.opacity(0.62), in: Capsule().inset(by: 4))
        } else {
            content()
        }
    }
}

public extension DetailAppBar where Actions == EmptyView {
    init(
        _ title: String,
        surface: AppBarSurface = .plain,
        backSymbol: AtticSymbol = .arrowBack,
        backLabel: String? = nil,
        onBack: @escaping () -> Void
    ) {
        self.init(title, surface: surface, backSymbol: backSymbol, backLabel: backLabel, onBack: onBack) { EmptyView() }
    }
}

public extension View {
    /// Reports how far a scroll view has moved from its top, 0 to 1 over the first 120 points, for the
    /// app bars. Apply it to the `ScrollView` or `List`.
    func onScrollFraction(range: CGFloat = AtticSpacing.barScrollRange, _ action: @escaping (CGFloat) -> Void) -> some View {
        onScrollGeometryChange(for: CGFloat.self) { geometry in
            let offset = geometry.contentOffset.y + geometry.contentInsets.top
            return max(0, min(1, offset / range))
        } action: { _, fraction in
            action(fraction)
        }
    }

    /// Reports how far a scroll view has moved from its top, in points.
    func onScrollOffset(_ action: @escaping (CGFloat) -> Void) -> some View {
        onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            action(offset)
        }
    }
}

/// The search field: a 56pt pill on the high container with the search glyph, the text, and a clear
/// button once there is text. With `onBack` a back button leads in place of the glyph, for the field
/// while it has focus. Pass `focus` to drive or read the keyboard focus.
public struct SearchField: View {
    @Environment(\.atticScheme) private var scheme
    @Binding private var text: String
    @FocusState private var ownFocus: Bool
    private let placeholder: String
    private let focus: FocusState<Bool>.Binding?
    private let onSubmit: () -> Void
    private let onBack: (() -> Void)?

    public init(
        text: Binding<String>,
        placeholder: String,
        focus: FocusState<Bool>.Binding? = nil,
        onSubmit: @escaping () -> Void = {},
        onBack: (() -> Void)? = nil
    ) {
        _text = text
        self.placeholder = placeholder
        self.focus = focus
        self.onSubmit = onSubmit
        self.onBack = onBack
    }

    public var body: some View {
        HStack(spacing: 0) {
            if let onBack {
                AtticIconButton(.arrowBack, label: AtticKitStrings.back, tint: scheme.onSurface, action: onBack)
                    .padding(.trailing, 4)
            } else {
                SymbolIcon(.search)
                    .foregroundStyle(scheme.onSurface)
                    .padding(.trailing, 12)
            }
            TextField(
                "",
                text: $text,
                prompt: Text(placeholder).foregroundStyle(scheme.onSurfaceVariant)
            )
            .atticFont(.bodyLarge)
            .foregroundStyle(scheme.onSurface)
            .tint(scheme.primary)
            .submitLabel(.search)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .focused(focus ?? $ownFocus)
            .onSubmit(onSubmit)
            .frame(maxWidth: .infinity)
            .accessibilityLabel(placeholder)
            if text.isEmpty {
                Spacer().frame(width: 12)
            } else {
                AtticIconButton(.close, label: AtticKitStrings.clearSearch, tint: scheme.onSurfaceVariant) { text = "" }
            }
        }
        .padding(.leading, onBack != nil ? 4 : 16)
        .padding(.trailing, 4)
        .frame(height: 56)
        .background(scheme.surfaceContainerHigh, in: Capsule())
        .contentShape(Capsule())
        .onTapGesture {
            if let focus { focus.wrappedValue = true } else { ownFocus = true }
        }
    }
}
