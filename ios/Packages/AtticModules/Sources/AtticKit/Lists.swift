import SwiftUI

public extension EnvironmentValues {
    /// The colour a group's cells take here. nil is the surface container; a sheet raises it.
    @Entry var atticGroupCellColor: Color? = nil
}

private struct GroupCellModifier: ViewModifier {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.atticGroupCellColor) private var cellColor
    let index: Int
    let count: Int
    let lifted: Bool
    let margin: CGFloat

    func body(content: Content) -> some View {
        let shape = GroupCellShape(index: index, count: count, lifted: lifted)
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(lifted ? scheme.surfaceContainerHighest : (cellColor ?? scheme.surfaceContainer))
            .clipShape(shape)
            .shadow(color: .black.opacity(lifted ? 0.3 : 0), radius: lifted ? 6 : 0, y: lifted ? 3 : 0)
            .padding(.horizontal, margin)
            .padding(.top, index == 0 ? 0 : AtticSpacing.groupGap)
            .animation(AtticMotion.fastSpatial, value: lifted)
    }
}

public extension View {
    /// One cell of a group: cut to its place in the group (outer corners 20, inner 4), filled with the
    /// cell colour, 2 below the cell above, 16 in from the screen edges. Use it on each row of a lazy
    /// list; `ListGroup` applies it for you. `lifted` is a row being dragged.
    func groupCell(index: Int, count: Int, lifted: Bool = false, margin: CGFloat = AtticSpacing.screenMargin) -> some View {
        modifier(GroupCellModifier(index: index, count: count, lifted: lifted, margin: margin))
    }

    /// Sets the colour group cells take inside this view: `surfaceContainerHigh` inside a sheet.
    func groupCellColor(_ color: Color?) -> some View {
        environment(\.atticGroupCellColor, color)
    }
}

/// A list group: each child is one cell, 2 apart, the group's outer corners 20 and the inner ones 4.
/// Every list sits in one. Two groups in a row are `AtticSpacing.groupSpacing` apart. For a long list
/// use `.groupCell(index:count:)` on the rows of a `LazyVStack(spacing: 0)` instead.
public struct ListGroup<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        Group(subviews: content) { subviews in
            VStack(spacing: 0) {
                ForEach(subviews.indices, id: \.self) { index in
                    subviews[index].groupCell(index: index, count: subviews.count)
                }
            }
        }
    }
}

/// What leads a song row.
public enum SongRowLead {
    /// A 48pt cover.
    case cover(Image?)
    /// A track number in place of a cover (album tracks). The wave replaces it on the current song.
    case number(Int)
    /// A rank before a cover (an artist's top songs).
    case rankedCover(Int, Image?)
    case none
}

/// A song row: lead, title over subtitle, trailing control. At least 64 tall. The current song gets the
/// secondary container fill, a heavier title, and the wave over its cover or in place of its number,
/// moving while `isPlaying`. `isLiked` draws a small heart after the text. Put it in a `ListGroup` or
/// give it `.groupCell(index:count:)`.
///
/// `action` is a tap on the row. `trailing` is its own control: `RowMoreMenu`, `RowMoreButton`,
/// `RowDragHandle`.
public struct SongRow<Trailing: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let subtitle: String
    private let lead: SongRowLead
    private let isCurrent: Bool
    private let isPlaying: Bool
    private let isLiked: Bool
    private let action: (() -> Void)?
    private let trailing: Trailing

    public init(
        title: String,
        subtitle: String = "",
        lead: SongRowLead = .none,
        isCurrent: Bool = false,
        isPlaying: Bool = false,
        isLiked: Bool = false,
        action: (() -> Void)? = nil,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.lead = lead
        self.isCurrent = isCurrent
        self.isPlaying = isPlaying
        self.isLiked = isLiked
        self.action = action
        self.trailing = trailing()
    }

    public var body: some View {
        HStack(spacing: 0) {
            if let action {
                Button(action: action) { main }.buttonStyle(PressStyle(pressedOpacity: 0.6))
            } else {
                main
            }
            trailing
        }
        .padding(AtticSpacing.rowPadding)
        .frame(minHeight: AtticSpacing.rowMinHeight)
        .background(isCurrent ? scheme.secondaryContainer : scheme.secondaryContainer.opacity(0))
        .animation(AtticMotion.defaultEffects, value: isCurrent)
    }

    private var main: some View {
        HStack(spacing: AtticSpacing.rowGap) {
            leadView
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .atticFont(.bodyLarge.weight(isCurrent ? .bold : .medium).lineHeight(22))
                    .foregroundStyle(isCurrent ? scheme.onSecondaryContainer : scheme.onSurface)
                    .lineLimit(1)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .atticFont(.bodyMedium)
                        .foregroundStyle(isCurrent ? scheme.onSecondaryContainer : scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if isLiked {
                SymbolIcon(.favorite, size: 16, filled: true)
                    .foregroundStyle(isCurrent ? scheme.onSecondaryContainer : scheme.primary)
                    .padding(.trailing, 8)
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
        .accessibilityAddTraits(isCurrent ? .isSelected : [])
    }

    private var accessibilityText: String {
        var parts = [title]
        if !subtitle.isEmpty { parts.append(subtitle) }
        if isCurrent { parts.append(AtticKitStrings.playing) }
        if isLiked { parts.append(AtticKitStrings.favourites) }
        return parts.joined(separator: ", ")
    }

    @ViewBuilder private var leadView: some View {
        switch lead {
        case .cover(let image):
            coverLead(image)
        case .number(let number):
            ZStack {
                if isCurrent {
                    NowPlayingWave(isPlaying: isPlaying).foregroundStyle(scheme.onSecondaryContainer)
                } else {
                    Text(String(number))
                        .atticFont(.labelLarge.tabular())
                        .foregroundStyle(scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .frame(width: 28)
        case .rankedCover(let rank, let image):
            Text(String(rank))
                .atticFont(.labelLarge.weight(.semibold).tabular())
                .foregroundStyle(isCurrent ? scheme.onSecondaryContainer : scheme.onSurfaceVariant)
                .lineLimit(1)
                .frame(width: 18)
            coverLead(image)
        case .none:
            EmptyView()
        }
    }

    private func coverLead(_ image: Image?) -> some View {
        Cover(image, size: 48)
            .overlay {
                if isCurrent {
                    ZStack {
                        CoverShape().fill(scheme.secondaryContainer.opacity(0.8))
                        NowPlayingWave(isPlaying: isPlaying).foregroundStyle(scheme.onSecondaryContainer)
                    }
                }
            }
    }
}

public extension SongRow where Trailing == EmptyView {
    /// A song row with nothing at its end.
    init(
        title: String,
        subtitle: String = "",
        lead: SongRowLead = .none,
        isCurrent: Bool = false,
        isPlaying: Bool = false,
        isLiked: Bool = false,
        action: (() -> Void)? = nil
    ) {
        self.init(title: title, subtitle: subtitle, lead: lead, isCurrent: isCurrent, isPlaying: isPlaying, isLiked: isLiked, action: action) {
            EmptyView()
        }
    }
}

/// The three dots at the end of a row, opening a system menu of `content`: "More options for {title}".
public struct RowMoreMenu<Content: View>: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.atticStandardIconTint) private var standardTint
    private let title: String
    private let content: Content

    public init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    public var body: some View {
        Menu {
            content
        } label: {
            SymbolIcon(.moreVert)
                .foregroundStyle(standardTint ?? scheme.onSurfaceVariant)
                .frame(width: AtticSpacing.touchTarget, height: AtticSpacing.touchTarget)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(AtticKitStrings.moreOptionsFor(title))
    }
}

/// The three dots at the end of a row as a plain button, for a row whose options open some other way.
public struct RowMoreButton: View {
    private let title: String
    private let action: () -> Void

    public init(title: String, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    public var body: some View {
        AtticIconButton(.moreVert, label: AtticKitStrings.moreOptionsFor(title), action: action)
    }
}

/// The handle at the end of a row the list can reorder. It is decoration: the list does the moving.
public struct RowDragHandle: View {
    @Environment(\.atticScheme) private var scheme

    public init() {}

    public var body: some View {
        SymbolIcon(.dragHandle)
            .foregroundStyle(scheme.onSurfaceVariant)
            .frame(width: AtticSpacing.touchTarget, height: AtticSpacing.touchTarget)
    }
}

/// A figure at the end of a row: "187 plays".
public struct RowCount: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String

    public init(_ text: String) {
        self.text = text
    }

    public var body: some View {
        Text(text)
            .atticFont(.labelMedium.tabular())
            .foregroundStyle(scheme.onSurfaceVariant)
            .lineLimit(1)
            .padding(.trailing, 12)
    }
}

/// A collection row: a 56pt lead whose shape says what it is (a cover, a mosaic, a cookie, the
/// Favourites heart), then title and subtitle. At least 72 tall. `isSelected` gives it the playing
/// row's colours.
public struct CollectionRow<Lead: View, Trailing: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let subtitle: String
    private let isSelected: Bool
    private let action: (() -> Void)?
    private let lead: Lead
    private let trailing: Trailing

    public init(
        title: String,
        subtitle: String = "",
        isSelected: Bool = false,
        action: (() -> Void)? = nil,
        @ViewBuilder lead: () -> Lead,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.isSelected = isSelected
        self.action = action
        self.lead = lead()
        self.trailing = trailing()
    }

    public var body: some View {
        HStack(spacing: 0) {
            if let action {
                Button(action: action) { main }.buttonStyle(PressStyle(pressedOpacity: 0.6))
            } else {
                main
            }
            trailing
        }
        .padding(AtticSpacing.rowPadding)
        .padding(.trailing, Trailing.self == EmptyView.self ? 8 : 0)
        .frame(minHeight: AtticSpacing.collectionRowMinHeight)
        .background(isSelected ? scheme.secondaryContainer : scheme.secondaryContainer.opacity(0))
    }

    private var main: some View {
        HStack(spacing: AtticSpacing.rowGap) {
            lead.frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .atticFont(.bodyLarge.weight(.medium).lineHeight(22))
                    .foregroundStyle(isSelected ? scheme.onSecondaryContainer : scheme.onSurface)
                    .lineLimit(1)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .atticFont(.bodyMedium)
                        .foregroundStyle(isSelected ? scheme.onSecondaryContainer : scheme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(subtitle.isEmpty ? title : "\(title), \(subtitle)")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

public extension CollectionRow where Trailing == EmptyView {
    /// A collection row with nothing at its end.
    init(
        title: String,
        subtitle: String = "",
        isSelected: Bool = false,
        action: (() -> Void)? = nil,
        @ViewBuilder lead: () -> Lead
    ) {
        self.init(title: title, subtitle: subtitle, isSelected: isSelected, action: action, lead: lead) { EmptyView() }
    }
}

/// A section header: the title in the emphasised title style, an optional overline above it ("Picked at
/// random from your songs"), and an optional trailing control such as a 56pt play button. 28 above,
/// 12 below, 16 at the sides.
public struct SectionHeader<Trailing: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let title: String
    private let overline: String?
    private let trailing: Trailing

    public init(_ title: String, overline: String? = nil, @ViewBuilder trailing: () -> Trailing) {
        self.title = title
        self.overline = overline
        self.trailing = trailing()
    }

    public var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                if let overline {
                    Text(overline).atticFont(.bodyMedium).foregroundStyle(scheme.onSurfaceVariant)
                }
                Text(title).atticFont(.titleLargeEmphasized).foregroundStyle(scheme.onSurface)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
            trailing
        }
        .padding(EdgeInsets(top: 28, leading: AtticSpacing.screenMargin, bottom: 12, trailing: AtticSpacing.screenMargin))
    }
}

public extension SectionHeader where Trailing == EmptyView {
    init(_ title: String, overline: String? = nil) {
        self.init(title, overline: overline) { EmptyView() }
    }
}

/// A small heading directly above a group: "Up next, 9 songs". 20 above, 8 below, 32 at the sides.
public struct ListLabel: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String

    public init(_ text: String) {
        self.text = text
    }

    public var body: some View {
        Text(text)
            .atticFont(.labelLarge.weight(.semibold))
            .foregroundStyle(scheme.onSurfaceVariant)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 20, leading: 32, bottom: 8, trailing: 32))
            .accessibilityAddTraits(.isHeader)
    }
}
