import SwiftUI

/// The three play controls: 36pt in a progress ring, 56pt beside a list, and the wide one in Now Playing.
public enum PlayButtonSize: Sendable {
    case small, large, extraLarge
}

/// The play control: round while paused, squared off while playing. The corner and, for the wide size,
/// the width move on the fast spatial spring; the glyph cross-fades on fast effects.
///
/// - `.small`: 36pt in a 52pt box. Give it `progress` (0 to 1) for the ring, where there is no progress
///   slider: the mini player.
/// - `.large`: 56pt, e.g. a section's "play all".
/// - `.extraLarge`: 152×80 paused, 128×80 playing. Now Playing.
///
/// `tonal` puts it on the secondary container. It reads "Play" or "Pause".
public struct PlayButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.isEnabled) private var isEnabled
    private let isPlaying: Bool
    private let size: PlayButtonSize
    private let tonal: Bool
    private let progress: Double?
    private let action: () -> Void

    public init(
        isPlaying: Bool,
        size: PlayButtonSize = .small,
        tonal: Bool = false,
        progress: Double? = nil,
        action: @escaping () -> Void
    ) {
        self.isPlaying = isPlaying
        self.size = size
        self.tonal = tonal
        self.progress = progress
        self.action = action
    }

    public var body: some View {
        let metrics = Metrics(size)
        let width = isPlaying ? metrics.playingWidth : metrics.pausedWidth
        let corner = isPlaying ? metrics.playingCorner : metrics.height / 2
        let shape = RoundedRectangle(cornerRadius: corner, style: .circular)
        Button(action: action) {
            ZStack {
                SymbolIcon(.pause, size: metrics.glyph, filled: true).opacity(isPlaying ? 1 : 0)
                SymbolIcon(.playArrow, size: metrics.glyph, filled: true).opacity(isPlaying ? 0 : 1)
            }
            .foregroundStyle(tonal ? scheme.onSecondaryContainer : scheme.onPrimary)
            .frame(width: width, height: metrics.height)
            .background(tonal ? scheme.secondaryContainer : scheme.primary, in: shape)
            .animation(AtticMotion.fastSpatial, value: isPlaying)
            .frame(width: metrics.box, height: metrics.box)
            .background {
                if let progress, size == .small {
                    ProgressRing(progress: progress)
                }
            }
            .frame(minWidth: AtticSpacing.touchTarget, minHeight: AtticSpacing.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .opacity(isEnabled ? 1 : 0.38)
        .accessibilityLabel(isPlaying ? AtticKitStrings.pause : AtticKitStrings.play)
    }

    private struct Metrics {
        let pausedWidth: CGFloat
        let playingWidth: CGFloat
        let height: CGFloat
        let playingCorner: CGFloat
        let glyph: CGFloat
        /// The box the control sits in; the small one leaves room for the ring.
        let box: CGFloat?

        init(_ size: PlayButtonSize) {
            switch size {
            case .small: (pausedWidth, playingWidth, height, playingCorner, glyph, box) = (36, 36, 36, 11, 22, 52)
            case .large: (pausedWidth, playingWidth, height, playingCorner, glyph, box) = (56, 56, 56, 18, 32, nil)
            case .extraLarge: (pausedWidth, playingWidth, height, playingCorner, glyph, box) = (152, 128, 80, 28, 44, nil)
            }
        }
    }
}

/// The ring round a small play control: elapsed in primary, the rest in outline variant, a gap at each end.
private struct ProgressRing: View {
    @Environment(\.atticScheme) private var scheme
    let progress: Double

    var body: some View {
        let fraction = max(0, min(1, progress))
        let gap = 14.0 / 360.0
        let style = StrokeStyle(lineWidth: 3, lineCap: .round)
        ZStack {
            if fraction > 0.5 / 360 {
                Circle().trim(from: 0, to: fraction).stroke(scheme.primary, style: style)
            }
            let offStart = fraction > 0.5 / 360 ? fraction + gap : gap
            let offEnd = 1 - gap
            if offEnd > offStart {
                Circle().trim(from: offStart, to: offEnd).stroke(scheme.outlineVariant, style: style)
            }
        }
        .rotationEffect(.degrees(-90))
        .padding(2.5)
    }
}

/// Previous and next in Now Playing: a 76×80 tonal pill with a 36pt filled glyph.
public struct SkipButton: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.isEnabled) private var isEnabled
    private let symbol: AtticSymbol
    private let label: String
    private let action: () -> Void

    public init(_ symbol: AtticSymbol, label: String, action: @escaping () -> Void) {
        self.symbol = symbol
        self.label = label
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            SymbolIcon(symbol, size: 36, filled: true)
                .foregroundStyle(scheme.onSecondaryContainer)
                .frame(width: 76, height: 80)
                .background(scheme.secondaryContainer, in: Capsule())
                .contentShape(Capsule())
        }
        .buttonStyle(PressStyle())
        .opacity(isEnabled ? 1 : 0.38)
        .accessibilityLabel(label)
    }
}

/// Now Playing's transport: previous, the wide play control, next, 8 apart and centred.
public struct Transport: View {
    private let isPlaying: Bool
    private let onPrevious: () -> Void
    private let onPlayPause: () -> Void
    private let onNext: () -> Void

    public init(isPlaying: Bool, onPrevious: @escaping () -> Void, onPlayPause: @escaping () -> Void, onNext: @escaping () -> Void) {
        self.isPlaying = isPlaying
        self.onPrevious = onPrevious
        self.onPlayPause = onPlayPause
        self.onNext = onNext
    }

    public var body: some View {
        HStack(spacing: 8) {
            SkipButton(.skipPrevious, label: AtticKitStrings.previous, action: onPrevious)
            PlayButton(isPlaying: isPlaying, size: .extraLarge, action: onPlayPause)
            SkipButton(.skipNext, label: AtticKitStrings.next, action: onNext)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Shuffle, as a toggle: "Turn shuffle on" / "Turn shuffle off".
public struct ShuffleToggle: View {
    private let isOn: Bool
    private let action: () -> Void

    public init(isOn: Bool, action: @escaping () -> Void) {
        self.isOn = isOn
        self.action = action
    }

    public var body: some View {
        AtticToggleButton(.shuffle, isOn: isOn, label: isOn ? AtticKitStrings.shuffleOff : AtticKitStrings.shuffleOn, action: action)
    }
}

public enum AtticRepeatMode: Sendable {
    case off, all, one
}

/// Repeat, as a toggle that cycles off, all, one. Its label says what a tap will do.
public struct RepeatToggle: View {
    private let mode: AtticRepeatMode
    private let action: () -> Void

    public init(mode: AtticRepeatMode, action: @escaping () -> Void) {
        self.mode = mode
        self.action = action
    }

    public var body: some View {
        let label = switch mode {
        case .off: AtticKitStrings.repeatOff
        case .all: AtticKitStrings.repeatAll
        case .one: AtticKitStrings.repeatOne
        }
        AtticToggleButton(mode == .one ? .repeatOne : .repeat, isOn: mode != .off, label: label, action: action)
    }
}

/// Now Playing's tools: one 56pt pill on the surface container, its buttons spread from edge to edge.
public struct ToolsRow<Content: View>: View {
    @Environment(\.atticScheme) private var scheme
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        Group(subviews: content) { subviews in
            HStack(spacing: 0) {
                ForEach(subviews.indices, id: \.self) { index in
                    if index > 0 { Spacer(minLength: 0) }
                    subviews[index]
                }
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 56)
        .frame(maxWidth: .infinity)
        .background(scheme.surfaceContainer, in: Capsule())
    }
}

// MARK: - Waves

/// A phase from 0 to 1 that goes round once every `period` seconds.
func wavePhase(at date: Date, period: Double = AtticMotion.wavePeriod) -> Double {
    let t = date.timeIntervalSinceReferenceDate / period
    return t - t.rounded(.down)
}

/// A sine wave from `start` to `end` along the middle of its box. Phase moves it towards the end. The
/// amplitude animates, so the wave can settle flat.
struct WaveLine: Shape {
    var amplitude: CGFloat
    var phase: Double
    var wavelength: CGFloat
    var start: CGFloat
    var end: CGFloat?

    var animatableData: CGFloat {
        get { amplitude }
        set { amplitude = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let endX = end ?? rect.width
        guard endX > start else { return path }
        var x = start
        var first = true
        while x <= endX {
            let y = rect.midY + amplitude * CGFloat(sin(2 * Double.pi * (Double((x - start) / wavelength) - phase)))
            if first { path.move(to: CGPoint(x: x, y: y)) } else { path.addLine(to: CGPoint(x: x, y: y)) }
            first = false
            x += 1.5
        }
        if abs(amplitude) < 0.01 { path.addLine(to: CGPoint(x: endX, y: rect.midY)) }
        return path
    }
}

/// The "now playing" mark: a short wave that travels while playing and lies flat when paused. It sits
/// over the current row's cover or in place of its number. 20×10, in the foreground style.
public struct NowPlayingWave: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let isPlaying: Bool

    public init(isPlaying: Bool) {
        self.isPlaying = isPlaying
    }

    public var body: some View {
        let moving = isPlaying && !reduceMotion
        TimelineView(.animation(paused: !moving)) { context in
            WaveLine(amplitude: moving ? 2 : 0, phase: wavePhase(at: context.date), wavelength: 10, start: 1.2, end: 18.8)
                .stroke(style: StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round))
                .animation(AtticMotion.defaultSpatial, value: moving)
        }
        .frame(width: 20, height: 10)
        .accessibilityHidden(true)
    }
}

// MARK: - Progress and loading

/// Linear progress: the done part in primary, a 4pt gap, the rest on the secondary container. 4pt tall.
public struct LinearProgress: View {
    @Environment(\.atticScheme) private var scheme
    private let fraction: Double

    public init(_ fraction: Double) {
        self.fraction = max(0, min(1, fraction))
    }

    public var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let done = width * fraction
            ZStack(alignment: .leading) {
                if done > 0 {
                    Capsule().fill(scheme.primary).frame(width: max(4, done))
                }
                let restStart = done > 0 ? max(4, done) + 4 : 0
                if width - restStart > 0 {
                    Capsule().fill(scheme.secondaryContainer).frame(width: width - restStart).offset(x: restStart)
                }
            }
        }
        .frame(height: 4)
        .accessibilityElement()
        .accessibilityValue(Text(fraction, format: .percent.precision(.fractionLength(0))))
    }
}

/// The loading indicator: one shape that morphs through seven while it turns, for waits of a few
/// seconds (the library scan, downloading lyrics). `contained` puts it in a primary-container circle.
public struct LoadingIndicator: View {
    @Environment(\.atticScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let contained: Bool
    private let size: CGFloat

    public init(contained: Bool = false, size: CGFloat = 48) {
        self.contained = contained
        self.size = size
    }

    public var body: some View {
        let active = size * (contained ? 30.0 / 48.0 : 38.0 / 48.0)
        TimelineView(.animation) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            shape(at: t)
                .fill(contained ? scheme.onPrimaryContainer : scheme.primary)
                .frame(width: active, height: active)
        }
        .frame(width: size, height: size)
        .background {
            if contained { Circle().fill(scheme.primaryContainer) }
        }
        .accessibilityElement()
        .accessibilityLabel(AtticKitStrings.loading)
    }

    private func shape(at t: Double) -> RadialShape {
        let shapes = RadialShape.loader
        if reduceMotion { return shapes[1].rotated(by: t * 0.8) }
        let step = AtticMotion.loaderStep
        let index = Int((t / step).rounded(.down))
        let within = t / step - (t / step).rounded(.down)
        let morph = Self.ease.value(at: min(1, within / 0.7))
        let from = shapes[index % shapes.count]
        let to = shapes[(index + 1) % shapes.count]
        let rotation = t * 1.6 + Double(index) * (.pi / 4) + morph * (.pi / 4)
        return RadialShape.blend(from, to, morph).rotated(by: rotation)
    }

    /// Fast out, slow in.
    private static let ease = UnitCurve.bezier(startControlPoint: UnitPoint(x: 0.4, y: 0), endControlPoint: UnitPoint(x: 0.2, y: 1))
}
