import SwiftUI

/// The corner scale, and the rules that pick a corner from a size.
///
/// - Covers are rounded squares whose radius follows their size (`cover(_:)`, `CoverShape`).
/// - Artists are the nine-lobed cookie (`CookieShape`), never a circle.
/// - Buttons, bars and pills are fully round (`Capsule`).
/// - Squared off means on: `onCorner(height:)` is the corner a control takes when it is on.
/// - A list is a group: rows 2 apart, outer corners 20, inner corners 4 (`GroupCellShape`).
public enum AtticRadius {
    public static let extraSmall: CGFloat = 4
    public static let small: CGFloat = 8
    public static let medium: CGFloat = 12
    public static let large: CGFloat = 16
    public static let largeIncreased: CGFloat = 20
    public static let extraLarge: CGFloat = 28
    public static let extraLargeIncreased: CGFloat = 32
    public static let extraExtraLarge: CGFloat = 48

    /// A cover's corner for its size: 8 up to 48, 12 up to 64, 16 up to 128, 20 up to 180, 28 above.
    public static func cover(_ size: CGFloat) -> CGFloat {
        switch size {
        case ...48: small
        case ...64: medium
        case ...128: large
        case ...180: largeIncreased
        default: extraLarge
        }
    }

    /// The corner a control takes when it is on: 11 up to 36 tall, 12 up to 40, 16 up to 56, 28 above.
    public static func onCorner(height: CGFloat) -> CGFloat {
        switch height {
        case ...36: 11
        case ...40: medium
        case ...56: large
        default: extraLarge
        }
    }
}

/// A rounded square whose corner follows the size it is drawn at, by `AtticRadius.cover(_:)`.
public struct CoverShape: Shape {
    public init() {}

    public func path(in rect: CGRect) -> Path {
        let radius = AtticRadius.cover(min(rect.width, rect.height))
        return Path(roundedRect: rect, cornerRadius: radius, style: .circular)
    }
}

/// The shape of row `index` of `count` in a group: 20 on the group's outer corners, 4 inside. `lifted`
/// rounds all four to 20, for a row being dragged.
public struct GroupCellShape: Shape {
    public var index: Int
    public var count: Int
    public var lifted: Bool

    public init(index: Int, count: Int, lifted: Bool = false) {
        self.index = index
        self.count = count
        self.lifted = lifted
    }

    public func path(in rect: CGRect) -> Path {
        let top = lifted || index == 0 ? AtticRadius.largeIncreased : AtticRadius.extraSmall
        let bottom = lifted || index >= count - 1 ? AtticRadius.largeIncreased : AtticRadius.extraSmall
        return UnevenRoundedRectangle(
            topLeadingRadius: top,
            bottomLeadingRadius: bottom,
            bottomTrailingRadius: bottom,
            topTrailingRadius: top,
            style: .circular
        ).path(in: rect)
    }
}

/// A closed shape described by its radius at each of 180 angles round the centre. Shapes in this form
/// blend into each other by blending radii, which is how the loading indicator morphs.
public struct RadialShape: Shape, Sendable {
    static let samples = 180

    var radii: [Double]
    /// A turn round the centre, in radians.
    public var rotation: Double

    init(radii: [Double], rotation: Double = 0) {
        self.radii = radii
        self.rotation = rotation
    }

    public func path(in rect: CGRect) -> Path {
        var path = Path()
        let cx = rect.midX
        let cy = rect.midY
        let rx = rect.width / 2
        let ry = rect.height / 2
        for i in 0..<radii.count {
            let angle = 2 * Double.pi * Double(i) / Double(radii.count) + rotation
            let point = CGPoint(x: cx + cos(angle) * radii[i] * rx, y: cy + sin(angle) * radii[i] * ry)
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        path.closeSubpath()
        return path
    }

    /// The shape with its outline turned by `radians`.
    public func rotated(by radians: Double) -> RadialShape {
        RadialShape(radii: radii, rotation: rotation + radians)
    }

    /// Blends two shapes: 0 is `from`, 1 is `to`.
    static func blend(_ from: RadialShape, _ to: RadialShape, _ fraction: Double) -> RadialShape {
        RadialShape(radii: zip(from.radii, to.radii).map { $0 + ($1 - $0) * fraction })
    }

    /// From points in percent of the box, star-shaped round the centre.
    static func polygon(_ points: [(Double, Double)]) -> RadialShape {
        let polar = points
            .map { (x: $0.0 / 50 - 1, y: $0.1 / 50 - 1) }
            .map { p -> (angle: Double, radius: Double) in
                var angle = atan2(p.y, p.x)
                if angle < 0 { angle += 2 * .pi }
                return (angle, (p.x * p.x + p.y * p.y).squareRoot())
            }
            .sorted { $0.angle < $1.angle }
        let full = 2 * Double.pi
        let radii = (0..<samples).map { i -> Double in
            let angle = full * Double(i) / Double(samples)
            let after = polar.firstIndex { $0.angle >= angle } ?? 0
            let before = (after - 1 + polar.count) % polar.count
            let a0 = polar[before], a1 = polar[after]
            let span = ((a1.angle - a0.angle) + full).truncatingRemainder(dividingBy: full)
            let into = ((angle - a0.angle) + full).truncatingRemainder(dividingBy: full)
            return span == 0 ? a0.radius : a0.radius + (a1.radius - a0.radius) * (into / span)
        }
        return RadialShape(radii: radii)
    }

    /// From a function of angle (radians) to radius, scaled so its widest point touches the box.
    static func function(_ radius: (Double) -> Double) -> RadialShape {
        let raw = (0..<samples).map { radius(2 * Double.pi * Double($0) / Double(samples)) }
        let widest = raw.max() ?? 1
        return RadialShape(radii: raw.map { $0 / widest })
    }

    static let cookie = polygon(cookiePoints)
    static let clover = polygon(cloverPoints)
    static let burst = polygon(burstPoints)

    /// The shapes the loading indicator morphs through: burst, cookie, pentagon, pill, sunny, four-leaf, oval.
    static let loader: [RadialShape] = {
        func lobed(_ lobes: Double, _ depth: Double) -> RadialShape {
            function { 1 - depth * (1 - cos(lobes * $0)) / 2 }
        }
        func roundedPolygon(_ sides: Double, _ softness: Double) -> RadialShape {
            function { a in
                let sector = 2 * Double.pi / sides
                let turned = (a + .pi / 2).truncatingRemainder(dividingBy: sector)
                let local = (turned + sector).truncatingRemainder(dividingBy: sector) - sector / 2
                let polygon = cos(sector / 2) / cos(local)
                return polygon * (1 - softness) + softness * cos(sector / 2) * 1.08
            }
        }
        // A stadium 1 wide and 0.62 tall, set on the diagonal.
        let pill = function { a in
            let t = a - .pi / 4
            let x = cos(t), y = sin(t)
            let half = 0.62
            let straight = 1 - half
            var low = 0.0, high = 2.0
            for _ in 0..<30 {
                let r = (low + high) / 2
                let px = abs(x * r), py = abs(y * r)
                let dx = max(0, px - straight)
                let inside = px <= straight ? py <= half : dx * dx + py * py <= half * half
                if inside { low = r } else { high = r }
            }
            return low
        }
        let oval = function { a in 1 / ((cos(a) * cos(a)) + (sin(a) / 0.82) * (sin(a) / 0.82)).squareRoot() }
        return [burst, cookie, roundedPolygon(5, 0.35), pill, lobed(8, 0.10), lobed(4, 0.22), oval]
    }()
}

/// The nine-lobed cookie: an artist's picture at every size. Never a circle.
public struct CookieShape: Shape {
    public init() {}
    public func path(in rect: CGRect) -> Path { RadialShape.cookie.path(in: rect) }
}

/// The four-lobed clover: the picture of "nothing here yet".
public struct CloverShape: Shape {
    public init() {}
    public func path(in rect: CGRect) -> Path { RadialShape.clover.path(in: rect) }
}

/// The Attic mark: a speaker cabinet cut to the slope of a roof, with the woofer (its dust cap left
/// solid) and the tweeter as holes. 36 wide by 51.5 tall; fill it with `FillStyle(eoFill: true)`.
public struct AtticMarkShape: Shape {
    public static let aspectRatio: CGFloat = 36 / 51.5

    public init() {}

    public func path(in rect: CGRect) -> Path {
        var path = Path()
        // Drawn on the source's 108-unit canvas, where the glyph spans x 35.2 to 71.2 and y 27 to 78.5.
        path.move(to: CGPoint(x: 35.2, y: 78.5))
        path.addLine(to: CGPoint(x: 35.2, y: 47.78))
        path.addLine(to: CGPoint(x: 71.2, y: 27))
        path.addLine(to: CGPoint(x: 71.2, y: 78.5))
        path.closeSubpath()
        path.addEllipse(in: CGRect(x: 41.2, y: 48.5, width: 24, height: 24))
        path.addEllipse(in: CGRect(x: 48.4, y: 55.7, width: 9.6, height: 9.6))
        path.addEllipse(in: CGRect(x: 57.2, y: 38.2, width: 7.6, height: 7.6))
        let scale = min(rect.width / 36, rect.height / 51.5)
        let transform = CGAffineTransform(translationX: rect.midX - 18 * scale, y: rect.midY - 25.75 * scale)
            .scaledBy(x: scale, y: scale)
            .translatedBy(x: -35.2, y: -27)
        return path.applying(transform)
    }
}

/// The mark, `size` tall and about 0.7 of that wide, in the foreground style.
public struct AtticMarkIcon: View {
    private let size: CGFloat

    public init(size: CGFloat = 24) {
        self.size = size
    }

    public var body: some View {
        AtticMarkShape()
            .fill(style: FillStyle(eoFill: true))
            .frame(width: size * AtticMarkShape.aspectRatio, height: size)
            .accessibilityHidden(true)
    }
}

/// The wordmark on Home's bar: the mark in primary, then "Attic" in `style`.
public struct AtticWordmark: View {
    @Environment(\.atticScheme) private var scheme
    private let text: String
    private let style: AtticTextStyle
    private let markSize: CGFloat

    public init(_ text: String = "Attic", style: AtticTextStyle = .displaySmallEmphasized.tracking(-0.4), markSize: CGFloat = 32) {
        self.text = text
        self.style = style
        self.markSize = markSize
    }

    public var body: some View {
        HStack(spacing: 10) {
            AtticMarkIcon(size: markSize).foregroundStyle(scheme.primary)
            Text(text).atticFont(style).lineLimit(1)
        }
        .accessibilityElement(children: .combine)
    }
}
