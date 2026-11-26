import SwiftUI

/// The progress slider: a wavy played part while playing, flat when paused or while dragging; a 4×28
/// bar handle with 6pt clear either side; the rest of the track in outline variant. Tap to jump, drag
/// to scrub.
///
/// `position` and `duration` are seconds. While a finger is down the binding is written on every move,
/// after `onEditingChanged(true)`. On release the binding holds the final value and
/// `onEditingChanged(false)` is called: seek the player then, and ignore its clock while editing.
///
/// 44pt tall, plus the times under it (elapsed, then the length) unless `showsTimes` is false.
public struct ProgressSlider: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Binding private var position: Double
    @State private var isDragging = false
    private let duration: Double
    private let isPlaying: Bool
    private let label: String
    private let wavy: Bool
    private let showsTimes: Bool
    private let onEditingChanged: (Bool) -> Void

    public init(
        position: Binding<Double>,
        duration: Double,
        isPlaying: Bool,
        label: String,
        wavy: Bool = true,
        showsTimes: Bool = true,
        onEditingChanged: @escaping (Bool) -> Void = { _ in }
    ) {
        _position = position
        self.duration = duration
        self.isPlaying = isPlaying
        self.label = label
        self.wavy = wavy
        self.showsTimes = showsTimes
        self.onEditingChanged = onEditingChanged
    }

    private var fraction: Double {
        duration > 0 ? max(0, min(1, position / duration)) : 0
    }

    public var body: some View {
        VStack(spacing: 0) {
            track
            if showsTimes {
                HStack {
                    Text(AtticFormat.duration(position))
                    Spacer(minLength: 8)
                    Text(AtticFormat.duration(duration))
                }
                .atticFont(.labelMedium.tabular())
                .foregroundStyle(scheme.onSurfaceVariant)
                .lineLimit(1)
                .accessibilityHidden(true)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(AtticKitStrings.positionOfLength(AtticFormat.duration(position), AtticFormat.duration(duration)))
        .accessibilityAdjustableAction { direction in
            guard duration > 0 else { return }
            let step = duration * 0.05
            onEditingChanged(true)
            switch direction {
            case .increment: position = min(duration, position + step)
            case .decrement: position = max(0, position - step)
            @unknown default: break
            }
            onEditingChanged(false)
        }
    }

    private var track: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let handleX = Self.handleX(width: width, fraction: fraction)
            let waving = wavy && isPlaying && !isDragging && !reduceMotion
            let restStart = handleX + Self.handle / 2 + Self.gap
            ZStack(alignment: .topLeading) {
                TimelineView(.animation(paused: !waving)) { context in
                    WaveLine(
                        amplitude: waving ? 3.5 : 0,
                        phase: wavePhase(at: context.date),
                        wavelength: 32,
                        start: Self.thickness / 2,
                        end: handleX - Self.handle / 2 - Self.gap
                    )
                    .stroke(scheme.primary, style: StrokeStyle(lineWidth: Self.thickness, lineCap: .round, lineJoin: .round))
                    .animation(AtticMotion.defaultSpatial, value: waving)
                }
                if restStart < width - Self.thickness / 2 {
                    Path { path in
                        path.move(to: CGPoint(x: restStart, y: geometry.size.height / 2))
                        path.addLine(to: CGPoint(x: width - Self.thickness / 2, y: geometry.size.height / 2))
                    }
                    .stroke(scheme.outlineVariant, style: StrokeStyle(lineWidth: Self.thickness, lineCap: .round))
                }
                RoundedRectangle(cornerRadius: Self.handle / 2, style: .circular)
                    .fill(scheme.primary)
                    .frame(width: Self.handle, height: 28)
                    .position(x: handleX, y: geometry.size.height / 2)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        guard duration > 0 else { return }
                        if !isDragging {
                            isDragging = true
                            onEditingChanged(true)
                        }
                        position = Self.fraction(x: value.location.x, width: width) * duration
                    }
                    .onEnded { value in
                        guard duration > 0 else { return }
                        position = Self.fraction(x: value.location.x, width: width) * duration
                        isDragging = false
                        onEditingChanged(false)
                    }
            )
        }
        .frame(height: 44)
    }

    private static let thickness: CGFloat = 4
    private static let handle: CGFloat = 4
    private static let gap: CGFloat = 6

    /// Where the handle's centre is for a fraction: it travels between the track's rounded ends, less its own gap.
    private static func handleX(width: CGFloat, fraction: Double) -> CGFloat {
        let lead = thickness / 2 + gap + handle / 2
        return lead + max(0, width - thickness - 2 * gap - handle) * CGFloat(fraction)
    }

    /// The inverse of `handleX`, so the handle sits under the finger.
    private static func fraction(x: CGFloat, width: CGFloat) -> Double {
        let lead = thickness / 2 + gap + handle / 2
        let travel = width - thickness - 2 * gap - handle
        guard travel > 0 else { return 0 }
        return Double(max(0, min(1, (x - lead) / travel)))
    }
}
