import SwiftUI

/// A row of chips that scrolls sideways, 8 apart with 16 at the sides.
public struct ChipRow<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: AtticSpacing.chipGap) { content }
                .padding(.horizontal, AtticSpacing.screenMargin)
        }
        .scrollIndicators(.hidden)
    }
}

/// A filter chip: 32 tall in a 48pt target, 8pt corners, an outline while off; tonal with a tick while
/// on. Filters that can be cleared are chips; a one-of-N switch is a `ConnectedButtonGroup`.
public struct FilterChip: View {
    @Environment(\.atticScheme) private var scheme
    private let label: String
    private let isSelected: Bool
    private let action: () -> Void

    public init(_ label: String, isSelected: Bool, action: @escaping () -> Void) {
        self.label = label
        self.isSelected = isSelected
        self.action = action
    }

    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: AtticRadius.small, style: .circular)
        Button(action: action) {
            HStack(spacing: 8) {
                if isSelected { SymbolIcon(.check, size: 18) }
                Text(label).atticFont(.labelLarge).lineLimit(1)
            }
            .padding(.leading, isSelected ? 8 : 16)
            .padding(.trailing, 16)
            .frame(height: 32)
            .foregroundStyle(isSelected ? scheme.onSecondaryContainer : scheme.onSurfaceVariant)
            .background(isSelected ? scheme.secondaryContainer : scheme.secondaryContainer.opacity(0), in: shape)
            .overlay {
                if !isSelected { shape.strokeBorder(scheme.outlineVariant, lineWidth: 1) }
            }
            .frame(minHeight: AtticSpacing.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle(pressedOpacity: 0.7))
        .animation(AtticMotion.fastEffects, value: isSelected)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// A connected button group: a one-of-N switch that always has one selected, such as Home's filter or
/// Settings' theme. Segments are 40 tall and 2 apart; the group's ends are round and the inner corners
/// 8. The selected segment widens a little, goes fully round, fills with primary and shows a tick. On a
/// group cell pass `onGroupCell: true`, so the unselected segments stand out from the cell.
public struct ConnectedButtonGroup: View {
    @Environment(\.atticScheme) private var scheme
    private let options: [String]
    private let selection: Int
    private let onGroupCell: Bool
    private let action: (Int) -> Void

    public init(_ options: [String], selection: Int, onGroupCell: Bool = false, action: @escaping (Int) -> Void) {
        self.options = options
        self.selection = selection
        self.onGroupCell = onGroupCell
        self.action = action
    }

    public var body: some View {
        WeightedRow(spacing: AtticSpacing.segmentGap) {
            ForEach(options.indices, id: \.self) { index in
                segment(index)
            }
        }
        .frame(height: 40)
        .padding(.horizontal, AtticSpacing.screenMargin)
        .padding(.vertical, 4)
        .animation(AtticMotion.fastSpatial, value: selection)
    }

    private func segment(_ index: Int) -> some View {
        let selected = index == selection
        let start: CGFloat = selected || index == 0 ? 20 : 8
        let end: CGFloat = selected || index == options.count - 1 ? 20 : 8
        let shape = UnevenRoundedRectangle(topLeadingRadius: start, bottomLeadingRadius: start, bottomTrailingRadius: end, topTrailingRadius: end, style: .circular)
        return Button {
            action(index)
        } label: {
            HStack(spacing: 6) {
                if selected { SymbolIcon(.check, size: 18) }
                Text(options[index])
                    .atticFont(.labelLarge.weight(selected ? .semibold : .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .padding(.horizontal, 4)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .foregroundStyle(selected ? scheme.onPrimary : scheme.onSurfaceVariant)
            .background(selected ? scheme.primary : (onGroupCell ? scheme.surfaceContainerHighest : scheme.surfaceContainerHigh), in: shape)
            .contentShape(shape)
        }
        .buttonStyle(PressStyle())
        .layoutValue(key: SegmentWeight.self, value: selected ? 1.15 : 1)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct SegmentWeight: LayoutValueKey {
    static let defaultValue: CGFloat = 1
}

/// A row whose children share its width by weight.
private struct WeightedRow: Layout {
    var spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let height = proposal.height ?? subviews.map { $0.sizeThatFits(.unspecified).height }.max() ?? 0
        let width = proposal.width ?? subviews.reduce(0) { $0 + $1.sizeThatFits(.unspecified).width } + spacing * CGFloat(max(0, subviews.count - 1))
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard !subviews.isEmpty else { return }
        let weights = subviews.map { $0[SegmentWeight.self] }
        let total = weights.reduce(0, +)
        let available = bounds.width - spacing * CGFloat(subviews.count - 1)
        var x = bounds.minX
        for (index, subview) in subviews.enumerated() {
            let width = available * weights[index] / total
            subview.place(at: CGPoint(x: x, y: bounds.minY), anchor: .topLeading, proposal: ProposedViewSize(width: width, height: bounds.height))
            x += width + spacing
        }
    }
}
