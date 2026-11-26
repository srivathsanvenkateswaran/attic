import CoreText
import Foundation

/// The Expressive kit for iOS. Tokens and components are specified in ios/DESIGN.md.
public enum AtticKit {
    /// Registers Google Sans Flex and Material Symbols Rounded from the package bundle. Safe to call more
    /// than once; `.atticTheme` calls it for you.
    public static func registerFonts() {
        _ = registered
    }

    private static let registered: Bool = {
        for file in [BundledFont.text, BundledFont.symbols] {
            guard let url = file.url else { continue }
            // Already registered is not a failure worth reporting.
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
        return true
    }()
}

/// The two font files in the bundle, and their descriptors with the variation axes left free.
enum BundledFont: String {
    case text = "google_sans_flex"
    case symbols = "material_symbols_rounded"

    var url: URL? {
        Bundle.module.url(forResource: rawValue, withExtension: "ttf")
            ?? Bundle.module.url(forResource: rawValue, withExtension: "ttf", subdirectory: "Fonts")
    }

    /// The file's own descriptor, so no font is ever looked up by a guessed name.
    var descriptor: CTFontDescriptor? {
        FontDescriptors.shared.descriptor(for: self)
    }
}

private final class FontDescriptors: @unchecked Sendable {
    static let shared = FontDescriptors()
    private var cache: [BundledFont: CTFontDescriptor] = [:]
    private let lock = NSLock()

    func descriptor(for font: BundledFont) -> CTFontDescriptor? {
        lock.lock()
        defer { lock.unlock() }
        if let cached = cache[font] { return cached }
        AtticKit.registerFonts()
        guard let url = font.url,
              let all = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
              let first = all.first
        else { return nil }
        cache[font] = first
        return first
    }
}

/// A four-letter OpenType axis tag as the number Core Text keys variations by.
func axisTag(_ tag: String) -> NSNumber {
    NSNumber(value: tag.utf8.reduce(UInt32(0)) { ($0 << 8) | UInt32($1) })
}
