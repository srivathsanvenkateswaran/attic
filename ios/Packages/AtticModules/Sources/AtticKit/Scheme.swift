import SwiftUI
import UIKit

/// Light, dark, or whatever the phone is set to.
public enum AtticThemeMode: String, CaseIterable, Sendable {
    case system, light, dark
}

/// Every colour role of the Expressive style. Read it with `@Environment(\.atticScheme)`.
///
/// `AtticScheme.app(dark:)` is the app's fixed scheme. `AtticScheme.from(seed:dark:)` builds one from a
/// cover, for player surfaces. What each role is for is in ios/DESIGN.md, "Colour".
public struct AtticScheme: Equatable, Sendable {
    public var primary: Color
    public var onPrimary: Color
    public var primaryContainer: Color
    public var onPrimaryContainer: Color
    public var inversePrimary: Color
    public var secondary: Color
    public var onSecondary: Color
    public var secondaryContainer: Color
    public var onSecondaryContainer: Color
    public var tertiary: Color
    public var onTertiary: Color
    public var tertiaryContainer: Color
    public var onTertiaryContainer: Color
    public var error: Color
    public var onError: Color
    public var errorContainer: Color
    public var onErrorContainer: Color
    public var surface: Color
    public var onSurface: Color
    public var onSurfaceVariant: Color
    public var surfaceDim: Color
    public var surfaceBright: Color
    public var surfaceContainerLowest: Color
    public var surfaceContainerLow: Color
    public var surfaceContainer: Color
    public var surfaceContainerHigh: Color
    public var surfaceContainerHighest: Color
    public var outline: Color
    public var outlineVariant: Color
    public var inverseSurface: Color
    public var inverseOnSurface: Color
    public var scrim: Color
    /// Whether this is a dark scheme.
    public var isDark: Bool

    /// The seed of the app scheme: its light primary.
    public static let defaultSeed = UIColor(atticRgb: 0x2A4174)

    /// The app's scheme: the blue one the store screenshots are drawn in.
    public static func app(dark: Bool) -> AtticScheme { dark ? appDark : appLight }

    /// The scheme a player surface takes from its cover. Only the seed's hue is read; every role has a
    /// fixed tone and chroma, so contrast never depends on the cover. Error roles come from the app scheme.
    public static func from(seed: UIColor, dark: Bool) -> AtticScheme {
        from(seedRgb: seed.atticRgb, dark: dark)
    }

    /// The same, from `0xRRGGBB`.
    public static func from(seedRgb: UInt32, dark: Bool) -> AtticScheme {
        let key = UInt64(seedRgb & 0xFFFFFF) | (dark ? 1 << 32 : 0)
        if let cached = ArtSchemeCache.shared.value(for: key) { return cached }
        let hue = Hct(rgb: seedRgb).hue
        var scheme = app(dark: dark)
        for role in dark ? ArtRoles.dark : ArtRoles.light {
            let roleHue = role.tertiary ? (hue + 60).truncatingRemainder(dividingBy: 360) : hue
            scheme[keyPath: role.path] = Color(atticRgb: Hct(hue: roleHue, chroma: role.chroma, tone: role.tone).rgb)
        }
        ArtSchemeCache.shared.set(scheme, for: key)
        return scheme
    }

    private static let appLight = AtticScheme(
        primary: Color(atticRgb: 0x2A4174),
        onPrimary: Color(atticRgb: 0xFFFFFF),
        primaryContainer: Color(atticRgb: 0x5E73A9),
        onPrimaryContainer: Color(atticRgb: 0xFFFFFF),
        inversePrimary: Color(atticRgb: 0xB7CAFF),
        secondary: Color(atticRgb: 0x3C4255),
        onSecondary: Color(atticRgb: 0xFFFFFF),
        secondaryContainer: Color(atticRgb: 0x6E7488),
        onSecondaryContainer: Color(atticRgb: 0xFFFFFF),
        tertiary: Color(atticRgb: 0x553A55),
        onTertiary: Color(atticRgb: 0xFFFFFF),
        tertiaryContainer: Color(atticRgb: 0x8A6A89),
        onTertiaryContainer: Color(atticRgb: 0xFFFFFF),
        error: Color(atticRgb: 0xB3261E),
        onError: Color(atticRgb: 0xFFFFFF),
        errorContainer: Color(atticRgb: 0xF9DEDC),
        onErrorContainer: Color(atticRgb: 0x410E0B),
        surface: Color(atticRgb: 0xFAF8FF),
        onSurface: Color(atticRgb: 0x1A1B20),
        onSurfaceVariant: Color(atticRgb: 0x40434B),
        surfaceDim: Color(atticRgb: 0xDAD9E0),
        surfaceBright: Color(atticRgb: 0xFAF8FF),
        surfaceContainerLowest: Color(atticRgb: 0xFFFFFF),
        surfaceContainerLow: Color(atticRgb: 0xF4F3FA),
        surfaceContainer: Color(atticRgb: 0xEEEDF4),
        surfaceContainerHigh: Color(atticRgb: 0xE8E7EF),
        surfaceContainerHighest: Color(atticRgb: 0xE2E2E9),
        outline: Color(atticRgb: 0x5D5F67),
        outlineVariant: Color(atticRgb: 0x797A83),
        inverseSurface: Color(atticRgb: 0x121318),
        inverseOnSurface: Color(atticRgb: 0xFCFAFF),
        scrim: Color(atticRgb: 0x000000),
        isDark: false
    )

    private static let appDark = AtticScheme(
        primary: Color(atticRgb: 0xB7CAFF),
        onPrimary: Color(atticRgb: 0x00143B),
        primaryContainer: Color(atticRgb: 0x7A90C8),
        onPrimaryContainer: Color(atticRgb: 0x000000),
        inversePrimary: Color(atticRgb: 0x2A4174),
        secondary: Color(atticRgb: 0xC4CAE1),
        onSecondary: Color(atticRgb: 0x0F1626),
        secondaryContainer: Color(atticRgb: 0x8A90A5),
        onSecondaryContainer: Color(atticRgb: 0x000000),
        tertiary: Color(atticRgb: 0xE4BFE2),
        onTertiary: Color(atticRgb: 0x240D26),
        // Android's dark scheme takes the fixed tertiary roles here (ExpressiveTheme.kt), so Favourites
        // is the same light-on-mauve in both themes. The wallpaper values (#A886A6, #000000) draw a black heart.
        tertiaryContainer: Color(atticRgb: 0x8A6A89),
        onTertiaryContainer: Color(atticRgb: 0xFFFFFF),
        error: Color(atticRgb: 0xF2B8B5),
        onError: Color(atticRgb: 0x601410),
        errorContainer: Color(atticRgb: 0x8C1D18),
        onErrorContainer: Color(atticRgb: 0xF9DEDC),
        surface: Color(atticRgb: 0x121318),
        onSurface: Color(atticRgb: 0xFCFAFF),
        onSurfaceVariant: Color(atticRgb: 0xC9CAD4),
        surfaceDim: Color(atticRgb: 0x121318),
        surfaceBright: Color(atticRgb: 0x38393F),
        surfaceContainerLowest: Color(atticRgb: 0x0C0E13),
        surfaceContainerLow: Color(atticRgb: 0x1A1B20),
        surfaceContainer: Color(atticRgb: 0x1E1F25),
        surfaceContainerHigh: Color(atticRgb: 0x282A2F),
        surfaceContainerHighest: Color(atticRgb: 0x33343A),
        outline: Color(atticRgb: 0xA1A2AC),
        outlineVariant: Color(atticRgb: 0x81838C),
        inverseSurface: Color(atticRgb: 0xFAF8FF),
        inverseOnSurface: Color(atticRgb: 0x1A1B20),
        scrim: Color(atticRgb: 0x000000),
        isDark: true
    )
}

public extension Color {
    /// An opaque sRGB colour from `0xRRGGBB`. For the kit's tables; screens read colours from the scheme.
    init(atticRgb rgb: UInt32) {
        self.init(
            .sRGB,
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255,
            opacity: 1
        )
    }
}

/// One role of the cover scheme: which colour it sets, its tone and chroma, and whether it uses the hue turned 60°.
private struct ArtRole {
    let path: WritableKeyPath<AtticScheme, Color>
    let tone: Double
    let chroma: Double
    var tertiary = false
}

/// The Android kit's `ArtScheme` tables: tone (CIE L*) and chroma (CAM16) per role, per theme.
private enum ArtRoles {
    static let dark: [ArtRole] = [
        ArtRole(path: \.primary, tone: 83.8, chroma: 34.6),
        ArtRole(path: \.onPrimary, tone: 14.2, chroma: 25.0),
        ArtRole(path: \.primaryContainer, tone: 32.7, chroma: 33.8),
        ArtRole(path: \.onPrimaryContainer, tone: 91.9, chroma: 18.1),
        ArtRole(path: \.inversePrimary, tone: 39.7, chroma: 42.1),
        ArtRole(path: \.secondary, tone: 81.4, chroma: 20.5),
        ArtRole(path: \.onSecondary, tone: 15.3, chroma: 17.6),
        ArtRole(path: \.secondaryContainer, tone: 25.8, chroma: 23.1),
        ArtRole(path: \.onSecondaryContainer, tone: 89.6, chroma: 16.7),
        ArtRole(path: \.tertiary, tone: 91.9, chroma: 21.0, tertiary: true),
        ArtRole(path: \.onTertiary, tone: 28.1, chroma: 32.2, tertiary: true),
        ArtRole(path: \.tertiaryContainer, tone: 88.4, chroma: 26.1, tertiary: true),
        ArtRole(path: \.onTertiaryContainer, tone: 23.4, chroma: 32.2, tertiary: true),
        ArtRole(path: \.surface, tone: 5.7, chroma: 13.6),
        ArtRole(path: \.surfaceDim, tone: 5.7, chroma: 13.6),
        ArtRole(path: \.surfaceBright, tone: 25.8, chroma: 17.6),
        ArtRole(path: \.surfaceContainerLowest, tone: 2.5, chroma: 11.4),
        ArtRole(path: \.surfaceContainerLow, tone: 8.9, chroma: 15.2),
        ArtRole(path: \.surfaceContainer, tone: 12.4, chroma: 16.8),
        ArtRole(path: \.surfaceContainerHigh, tone: 15.9, chroma: 18.3),
        ArtRole(path: \.surfaceContainerHighest, tone: 20.0, chroma: 19.5),
        ArtRole(path: \.onSurface, tone: 93.0, chroma: 9.2),
        ArtRole(path: \.inverseSurface, tone: 93.0, chroma: 9.2),
        ArtRole(path: \.inverseOnSurface, tone: 18.8, chroma: 13.7),
        ArtRole(path: \.onSurfaceVariant, tone: 79.1, chroma: 15.0),
        ArtRole(path: \.outline, tone: 58.2, chroma: 15.3),
        ArtRole(path: \.outlineVariant, tone: 30.4, chroma: 17.5),
    ]

    static let light: [ArtRole] = [
        ArtRole(path: \.primary, tone: 38.5, chroma: 43.6),
        ArtRole(path: \.onPrimary, tone: 98.8, chroma: 4.5),
        ArtRole(path: \.primaryContainer, tone: 84.9, chroma: 28.4),
        ArtRole(path: \.onPrimaryContainer, tone: 22.3, chroma: 35.0),
        ArtRole(path: \.inversePrimary, tone: 83.8, chroma: 34.6),
        ArtRole(path: \.secondary, tone: 37.4, chroma: 23.0),
        ArtRole(path: \.onSecondary, tone: 98.8, chroma: 4.5),
        ArtRole(path: \.secondaryContainer, tone: 87.8, chroma: 18.6),
        ArtRole(path: \.onSecondaryContainer, tone: 24.6, chroma: 23.1),
        ArtRole(path: \.tertiary, tone: 35.0, chroma: 35.5, tertiary: true),
        ArtRole(path: \.onTertiary, tone: 98.8, chroma: 4.5, tertiary: true),
        ArtRole(path: \.tertiaryContainer, tone: 88.4, chroma: 26.1, tertiary: true),
        ArtRole(path: \.onTertiaryContainer, tone: 23.4, chroma: 32.2, tertiary: true),
        ArtRole(path: \.surface, tone: 96.3, chroma: 8.5),
        ArtRole(path: \.surfaceDim, tone: 87.2, chroma: 13.0),
        ArtRole(path: \.surfaceBright, tone: 96.3, chroma: 8.5),
        ArtRole(path: \.surfaceContainerLowest, tone: 99.4, chroma: 3.6),
        ArtRole(path: \.surfaceContainerLow, tone: 94.2, chroma: 10.2),
        ArtRole(path: \.surfaceContainer, tone: 92.2, chroma: 12.2),
        ArtRole(path: \.surfaceContainerHigh, tone: 90.1, chroma: 13.7),
        ArtRole(path: \.surfaceContainerHighest, tone: 87.8, chroma: 15.3),
        ArtRole(path: \.onSurface, tone: 9.5, chroma: 13.7),
        ArtRole(path: \.inverseSurface, tone: 14.2, chroma: 13.7),
        ArtRole(path: \.inverseOnSurface, tone: 94.2, chroma: 9.2),
        ArtRole(path: \.onSurfaceVariant, tone: 30.4, chroma: 17.5),
        ArtRole(path: \.outline, tone: 51.3, chroma: 17.3),
        ArtRole(path: \.outlineVariant, tone: 79.1, chroma: 16.9),
    ]
}

/// Cover schemes already worked out, by seed and theme. A scheme is 27 colour solves, so a list of
/// headers should not redo them on every pass.
private final class ArtSchemeCache: @unchecked Sendable {
    static let shared = ArtSchemeCache()
    private var values: [UInt64: AtticScheme] = [:]
    private let lock = NSLock()

    func value(for key: UInt64) -> AtticScheme? {
        lock.lock()
        defer { lock.unlock() }
        return values[key]
    }

    func set(_ scheme: AtticScheme, for key: UInt64) {
        lock.lock()
        defer { lock.unlock() }
        if values.count > 256 { values.removeAll() }
        values[key] = scheme
    }
}

// MARK: - Environment

public extension EnvironmentValues {
    /// The colour scheme in force: the app's, or a cover's inside `.fromArt(seed:)`.
    @Entry var atticScheme: AtticScheme = .app(dark: false)
}

private struct AtticThemeModifier: ViewModifier {
    let mode: AtticThemeMode
    @Environment(\.colorScheme) private var systemScheme

    func body(content: Content) -> some View {
        let dark = mode == .dark || (mode == .system && systemScheme == .dark)
        let scheme = AtticScheme.app(dark: dark)
        content
            .atticFont(.bodyLarge)
            .foregroundStyle(scheme.onSurface)
            .tint(scheme.primary)
            .background(scheme.surface.ignoresSafeArea())
            .environment(\.atticScheme, scheme)
            .preferredColorScheme(mode == .system ? nil : (dark ? .dark : .light))
    }
}

private struct FromArtModifier: ViewModifier {
    let seed: UIColor?
    @Environment(\.atticScheme) private var around

    func body(content: Content) -> some View {
        let rgb = seed?.atticRgb
        let scheme = rgb.map { AtticScheme.from(seedRgb: $0, dark: around.isDark) } ?? around
        content
            .foregroundStyle(scheme.onSurface)
            .tint(scheme.primary)
            .environment(\.atticScheme, scheme)
            .animation(AtticMotion.slowEffects, value: rgb)
    }
}

public extension View {
    /// Sets up the Expressive theme for the app: registers the fonts, provides the app scheme for light or
    /// dark, and sets the default text colour, font and screen colour. Apply once, at the root.
    func atticTheme(_ mode: AtticThemeMode = .system) -> some View {
        AtticKit.registerFonts()
        return modifier(AtticThemeModifier(mode: mode))
    }

    /// Draws the content in the scheme of a cover's colour, following light and dark. A new seed moves every
    /// role on the slow effects spring. `nil` keeps the scheme around it. For player surfaces only.
    func fromArt(seed: UIColor?) -> some View {
        modifier(FromArtModifier(seed: seed))
    }
}
