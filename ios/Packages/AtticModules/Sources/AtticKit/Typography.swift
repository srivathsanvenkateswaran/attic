import CoreText
import SwiftUI
import UIKit

/// The weights Google Sans Flex is set at.
public enum AtticFontWeight: Int, Sendable {
    case light = 300, regular = 400, medium = 500, semibold = 600, bold = 700, heavy = 800
}

/// One style of the Expressive type scale, in Google Sans Flex. Apply it with `.atticFont(_:)`, which
/// sets the font, tracking and line height and scales them with Dynamic Type.
///
/// The plain styles are `displayLarge` … `labelSmall`; each has an emphasised partner (heavier and fully
/// rounded), `displayLargeEmphasized` and so on. Change one for a single use with `weight(_:)`,
/// `narrowed(_:)`, `tabular()`, `sized(_:line:)` and `tracking(_:)`.
public struct AtticTextStyle: Equatable, Hashable, Sendable {
    public var size: CGFloat
    public var lineHeight: CGFloat
    public var weight: Int
    public var tracking: CGFloat
    public var isRound: Bool
    /// The width axis, 50 to 100.
    public var width: Int
    public var isTabular: Bool
    /// The system text style whose Dynamic Type curve this style follows.
    public var relativeTo: UIFont.TextStyle

    public init(
        size: CGFloat,
        lineHeight: CGFloat,
        weight: Int = 400,
        tracking: CGFloat = 0,
        isRound: Bool = false,
        width: Int = 100,
        isTabular: Bool = false,
        relativeTo: UIFont.TextStyle = .body
    ) {
        self.size = size
        self.lineHeight = lineHeight
        self.weight = weight
        self.tracking = tracking
        self.isRound = isRound
        self.width = width
        self.isTabular = isTabular
        self.relativeTo = relativeTo
    }

    // MARK: The scale

    public static let displayLarge = AtticTextStyle(size: 57, lineHeight: 64, weight: 400, tracking: -0.25, relativeTo: .largeTitle)
    public static let displayMedium = AtticTextStyle(size: 45, lineHeight: 52, weight: 400, tracking: -0.5, relativeTo: .largeTitle)
    public static let displaySmall = AtticTextStyle(size: 36, lineHeight: 44, weight: 400, tracking: -0.3, relativeTo: .largeTitle)
    public static let headlineLarge = AtticTextStyle(size: 32, lineHeight: 40, weight: 400, relativeTo: .title1)
    public static let headlineMedium = AtticTextStyle(size: 28, lineHeight: 36, weight: 400, relativeTo: .title1)
    public static let headlineSmall = AtticTextStyle(size: 24, lineHeight: 32, weight: 400, relativeTo: .title2)
    public static let titleLarge = AtticTextStyle(size: 22, lineHeight: 28, weight: 400, relativeTo: .title2)
    public static let titleMedium = AtticTextStyle(size: 16, lineHeight: 24, weight: 500, tracking: 0.1, relativeTo: .headline)
    public static let titleSmall = AtticTextStyle(size: 14, lineHeight: 20, weight: 500, tracking: 0.1, relativeTo: .subheadline)
    public static let bodyLarge = AtticTextStyle(size: 16, lineHeight: 24, weight: 400, relativeTo: .body)
    public static let bodyMedium = AtticTextStyle(size: 14, lineHeight: 20, weight: 400, relativeTo: .callout)
    public static let bodySmall = AtticTextStyle(size: 12, lineHeight: 16, weight: 400, relativeTo: .footnote)
    public static let labelLarge = AtticTextStyle(size: 14, lineHeight: 20, weight: 500, relativeTo: .subheadline)
    public static let labelMedium = AtticTextStyle(size: 12, lineHeight: 16, weight: 500, relativeTo: .footnote)
    public static let labelSmall = AtticTextStyle(size: 11, lineHeight: 16, weight: 500, relativeTo: .caption2)

    public static let displayLargeEmphasized = displayLarge.emphasized(700)
    public static let displayMediumEmphasized = displayMedium.emphasized(700)
    public static let displaySmallEmphasized = displaySmall.emphasized(700)
    public static let headlineLargeEmphasized = headlineLarge.emphasized(700)
    public static let headlineMediumEmphasized = headlineMedium.emphasized(700)
    public static let headlineSmallEmphasized = headlineSmall.emphasized(700)
    public static let titleLargeEmphasized = titleLarge.emphasized(700)
    public static let titleMediumEmphasized = titleMedium.emphasized(600)
    public static let titleSmallEmphasized = titleSmall.emphasized(600)
    public static let bodyLargeEmphasized = bodyLarge.emphasized(600)
    public static let bodyMediumEmphasized = bodyMedium.emphasized(600)
    public static let bodySmallEmphasized = bodySmall.emphasized(700)
    public static let labelLargeEmphasized = labelLarge.emphasized(600)
    public static let labelMediumEmphasized = labelMedium.emphasized(600)
    public static let labelSmallEmphasized = labelSmall.emphasized(700)

    // MARK: Changes for one use

    /// The emphasised partner of any style: heavier and fully rounded.
    public func emphasized(_ weight: Int = 700) -> AtticTextStyle {
        var copy = self
        copy.weight = weight
        copy.isRound = true
        return copy
    }

    public func weight(_ weight: AtticFontWeight) -> AtticTextStyle {
        var copy = self
        copy.weight = weight.rawValue
        return copy
    }

    /// A narrower setting: 75 or 50 on the width axis.
    public func narrowed(_ width: Int) -> AtticTextStyle {
        var copy = self
        copy.width = max(50, min(100, width))
        return copy
    }

    /// Figures that all take the same width, for times and counts that change in place.
    public func tabular() -> AtticTextStyle {
        var copy = self
        copy.isTabular = true
        return copy
    }

    public func sized(_ size: CGFloat, line: CGFloat) -> AtticTextStyle {
        var copy = self
        copy.size = size
        copy.lineHeight = line
        return copy
    }

    public func tracking(_ tracking: CGFloat) -> AtticTextStyle {
        var copy = self
        copy.tracking = tracking
        return copy
    }

    public func lineHeight(_ line: CGFloat) -> AtticTextStyle {
        var copy = self
        copy.lineHeight = line
        return copy
    }

    public func rounded(_ round: Bool = true) -> AtticTextStyle {
        var copy = self
        copy.isRound = round
        return copy
    }

    // MARK: Fonts

    /// The optical sizes in use, so a few settings cover every style.
    public static func opticalSize(for size: CGFloat) -> CGFloat {
        switch size {
        case ...12: 12
        case ...14: 14
        case ...18: 16
        case ...24: 22
        case ...30: 28
        case ...40: 36
        default: 45
        }
    }

    /// How much Dynamic Type enlarges this style at `dynamicTypeSize`: 1 at the default size.
    public func scale(for dynamicTypeSize: DynamicTypeSize) -> CGFloat {
        let traits = UITraitCollection(preferredContentSizeCategory: UIContentSizeCategory(dynamicTypeSize))
        return UIFontMetrics(forTextStyle: relativeTo).scaledValue(for: 100, compatibleWith: traits) / 100
    }

    /// The font at this style's size times `scale`.
    public func uiFont(scale: CGFloat = 1) -> UIFont {
        FontCache.shared.font(for: self, scale: scale)
    }

    /// The SwiftUI font at this style's size times `scale`. Prefer `.atticFont(_:)`, which also sets tracking and line height.
    public func font(scale: CGFloat = 1) -> Font {
        Font(uiFont(scale: scale) as CTFont)
    }

    fileprivate func makeFont(scale: CGFloat) -> UIFont {
        let pointSize = (size * scale * 2).rounded() / 2
        guard let base = BundledFont.text.descriptor else {
            return UIFont.systemFont(ofSize: pointSize, weight: weight >= 600 ? .bold : .regular)
        }
        let variations: [NSNumber: NSNumber] = [
            axisTag("wght"): NSNumber(value: weight),
            axisTag("wdth"): NSNumber(value: width),
            axisTag("ROND"): NSNumber(value: isRound ? 100 : 0),
            axisTag("opsz"): NSNumber(value: Double(Self.opticalSize(for: size))),
        ]
        var attributes: [CFString: Any] = [kCTFontVariationAttribute: variations]
        if isTabular {
            attributes[kCTFontFeatureSettingsAttribute] = [[kCTFontOpenTypeFeatureTag: "tnum", kCTFontOpenTypeFeatureValue: 1]]
        }
        let descriptor = CTFontDescriptorCreateCopyWithAttributes(base, attributes as CFDictionary)
        return CTFontCreateWithFontDescriptor(descriptor, pointSize, nil) as UIFont
    }
}

private final class FontCache: @unchecked Sendable {
    static let shared = FontCache()

    private struct Key: Hashable {
        let style: AtticTextStyle
        let scale: CGFloat
    }

    private var fonts: [Key: UIFont] = [:]
    private let lock = NSLock()

    func font(for style: AtticTextStyle, scale: CGFloat) -> UIFont {
        let key = Key(style: style, scale: scale)
        lock.lock()
        defer { lock.unlock() }
        if let font = fonts[key] { return font }
        let font = style.makeFont(scale: scale)
        fonts[key] = font
        return font
    }
}

private struct AtticFontModifier: ViewModifier {
    let style: AtticTextStyle
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    func body(content: Content) -> some View {
        let scale = style.scale(for: dynamicTypeSize)
        content
            .font(style.font(scale: scale))
            .tracking(style.tracking * scale)
            .lineHeight(.exact(points: (style.lineHeight * scale).rounded()))
    }
}

public extension View {
    /// Sets text in a style of the Expressive scale: font, tracking and line height, scaled with Dynamic Type.
    func atticFont(_ style: AtticTextStyle) -> some View {
        modifier(AtticFontModifier(style: style))
    }
}

/// One line of text that narrows before it truncates: it is set at width 100, then 75, then 50, and
/// only then ends in an ellipsis. For titles beside something that must not move (Now Playing's title).
public struct NarrowingText: View {
    private let text: String
    private let style: AtticTextStyle

    public init(_ text: String, style: AtticTextStyle) {
        self.text = text
        self.style = style
    }

    public var body: some View {
        ViewThatFits(in: .horizontal) {
            line(100)
            line(75)
            line(50).truncationMode(.tail)
        }
    }

    private func line(_ width: Int) -> some View {
        Text(text)
            .atticFont(style.narrowed(min(width, style.width)))
            .lineLimit(1)
    }
}
