import CoreText
import SwiftUI
import UIKit

/// The glyphs in the bundled Material Symbols Rounded subset (Apache 2.0), named after the glyph in
/// camel case: `play_arrow` is `.playArrow`. The raw value is the glyph's own name. The subset has no
/// ligatures, so each glyph is drawn by its codepoint.
///
/// A glyph that is not here is not in the font. Adding one means re-running the subset command in
/// third_party/fonts-NOTICE.txt with its codepoint added.
public enum AtticSymbol: String, CaseIterable, Sendable {
    case add = "add"
    case airplanemodeActive = "airplanemode_active"
    case alarm = "alarm"
    case album = "album"
    case arrowBack = "arrow_back"
    case arrowForward = "arrow_forward"
    case autoplay = "autoplay"
    case backspace = "backspace"
    case bedtime = "bedtime"
    case bluetooth = "bluetooth"
    case calendarMonth = "calendar_month"
    case call = "call"
    case chatBubble = "chat_bubble"
    case check = "check"
    case chevronRight = "chevron_right"
    case clearAll = "clear_all"
    case close = "close"
    case delete = "delete"
    case doNotDisturbOn = "do_not_disturb_on"
    case download = "download"
    case dragHandle = "drag_handle"
    case edit = "edit"
    case editNote = "edit_note"
    case equalizer = "equalizer"
    case error = "error"
    case expandLess = "expand_less"
    case expandMore = "expand_more"
    case favorite = "favorite"
    case fileOpen = "file_open"
    case fileSave = "file_save"
    case flashlightOn = "flashlight_on"
    case folder = "folder"
    case folderOpen = "folder_open"
    case graphicEq = "graphic_eq"
    case gridView = "grid_view"
    case home = "home"
    case image = "image"
    case info = "info"
    case iosShare = "ios_share"
    case keyboardArrowDown = "keyboard_arrow_down"
    case keyboardArrowUp = "keyboard_arrow_up"
    case language = "language"
    case libraryMusic = "library_music"
    case lightMode = "light_mode"
    case localFireDepartment = "local_fire_department"
    case lock = "lock"
    case lyrics = "lyrics"
    case map = "map"
    case mic = "mic"
    case moreVert = "more_vert"
    case musicNote = "music_note"
    case musicOff = "music_off"
    case newReleases = "new_releases"
    case nightsStay = "nights_stay"
    case notifications = "notifications"
    case openInFull = "open_in_full"
    case openInNew = "open_in_new"
    case palette = "palette"
    case partlyCloudyDay = "partly_cloudy_day"
    case pause = "pause"
    case person = "person"
    case photoCamera = "photo_camera"
    case playArrow = "play_arrow"
    case playlistAdd = "playlist_add"
    case playlistPlay = "playlist_play"
    case queueMusic = "queue_music"
    case refresh = "refresh"
    case `repeat` = "repeat"
    case repeatOne = "repeat_one"
    case schedule = "schedule"
    case screenRotation = "screen_rotation"
    case search = "search"
    case searchOff = "search_off"
    case sensors = "sensors"
    case settings = "settings"
    case share = "share"
    case shift = "shift"
    case shuffle = "shuffle"
    case shuffleOn = "shuffle_on"
    case skipNext = "skip_next"
    case skipPrevious = "skip_previous"
    case smartphone = "smartphone"
    case sort = "sort"
    case swapVert = "swap_vert"
    case today = "today"
    case transitionFade = "transition_fade"
    case unfoldMore = "unfold_more"
    case viewList = "view_list"
    case volumeUp = "volume_up"
    case wbSunny = "wb_sunny"
    case wbTwilight = "wb_twilight"
    case wifi = "wifi"

    /// The symbol with this glyph name, e.g. "queue_music".
    public static func named(_ name: String) -> AtticSymbol? { AtticSymbol(rawValue: name) }

    /// The glyph's codepoint in the font.
    public var codepoint: UInt32 {
        switch self {
        case .add: 0xE145
        case .airplanemodeActive: 0xE53D
        case .alarm: 0xE855
        case .album: 0xE019
        case .arrowBack: 0xE5C4
        case .arrowForward: 0xE5C8
        case .autoplay: 0xF6B5
        case .backspace: 0xE14A
        case .bedtime: 0xF159
        case .bluetooth: 0xE1A7
        case .calendarMonth: 0xEBCC
        case .call: 0xF0D4
        case .chatBubble: 0xE0CB
        case .check: 0xE668
        case .chevronRight: 0xE5CC
        case .clearAll: 0xE0B8
        case .close: 0xE5CD
        case .delete: 0xE92E
        case .doNotDisturbOn: 0xF08F
        case .download: 0xF090
        case .dragHandle: 0xE25D
        case .edit: 0xF097
        case .editNote: 0xE745
        case .equalizer: 0xE01D
        case .error: 0xF8B6
        case .expandLess: 0xE5CE
        case .expandMore: 0xE5CF
        case .favorite: 0xE87E
        case .fileOpen: 0xEAF3
        case .fileSave: 0xF17F
        case .flashlightOn: 0xF00B
        case .folder: 0xE2C7
        case .folderOpen: 0xE2C8
        case .graphicEq: 0xE1B8
        case .gridView: 0xE9B0
        case .home: 0xE9B2
        case .image: 0xE3F4
        case .info: 0xE88E
        case .iosShare: 0xE6B8
        case .keyboardArrowDown: 0xE313
        case .keyboardArrowUp: 0xE316
        case .language: 0xEA07
        case .libraryMusic: 0xE030
        case .lightMode: 0xE518
        case .localFireDepartment: 0xEF55
        case .lock: 0xE899
        case .lyrics: 0xEC0B
        case .map: 0xE55B
        case .mic: 0xE31D
        case .moreVert: 0xE5D4
        case .musicNote: 0xE405
        case .musicOff: 0xE440
        case .newReleases: 0xEF76
        case .nightsStay: 0xF174
        case .notifications: 0xE7F5
        case .openInFull: 0xF1CE
        case .openInNew: 0xE89E
        case .palette: 0xE40A
        case .partlyCloudyDay: 0xF172
        case .pause: 0xE034
        case .person: 0xF0D3
        case .photoCamera: 0xE412
        case .playArrow: 0xE037
        case .playlistAdd: 0xE03B
        case .playlistPlay: 0xE05F
        case .queueMusic: 0xE03D
        case .refresh: 0xE5D5
        case .`repeat`: 0xE040
        case .repeatOne: 0xE041
        case .schedule: 0xEFD6
        case .screenRotation: 0xF2D5
        case .search: 0xEF7A
        case .searchOff: 0xEA76
        case .sensors: 0xE51E
        case .settings: 0xE8B8
        case .share: 0xE80D
        case .shift: 0xE5F2
        case .shuffle: 0xE043
        case .shuffleOn: 0xE9E1
        case .skipNext: 0xE044
        case .skipPrevious: 0xE045
        case .smartphone: 0xE7BA
        case .sort: 0xE164
        case .swapVert: 0xE8D5
        case .today: 0xE8DF
        case .transitionFade: 0xF50C
        case .unfoldMore: 0xE5D7
        case .viewList: 0xE8EF
        case .volumeUp: 0xE050
        case .wbSunny: 0xE430
        case .wbTwilight: 0xE1C6
        case .wifi: 0xE63E
        }
    }
}

/// A Material Symbols Rounded icon, drawn in the foreground style. `filled` is the selected state (the
/// current tab, a favourite, a toggle that is on) and the transport glyphs. Sizes in the kit are 18, 20,
/// 24 (the default), 28, 32, 36 and 40. Icons do not scale with Dynamic Type.
///
/// It is hidden from VoiceOver: label the button it sits in.
public struct SymbolIcon: View {
    private let symbol: AtticSymbol
    private let size: CGFloat
    private let filled: Bool
    private let weight: Int

    public init(_ symbol: AtticSymbol, size: CGFloat = 24, filled: Bool = false, weight: Int = 400) {
        self.symbol = symbol
        self.size = size
        self.filled = filled
        self.weight = weight
    }

    public var body: some View {
        SymbolShape(symbol: symbol, filled: filled, weight: weight)
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// A symbol's outline, filling the square it is given. Use it where a `Shape` is needed; otherwise `SymbolIcon`.
public struct SymbolShape: Shape {
    public var symbol: AtticSymbol
    public var filled: Bool
    public var weight: Int

    public init(symbol: AtticSymbol, filled: Bool = false, weight: Int = 400) {
        self.symbol = symbol
        self.filled = filled
        self.weight = weight
    }

    public func path(in rect: CGRect) -> Path {
        let side = min(rect.width, rect.height)
        guard side > 0, let outline = SymbolOutlines.shared.outline(symbol, filled: filled, weight: weight, opticalSize: Self.opticalSize(for: side)) else {
            return Path()
        }
        // The outline is on a one-point em square with y up, the baseline at the square's bottom edge.
        let transform = CGAffineTransform(translationX: rect.midX - side / 2, y: rect.midY + side / 2).scaledBy(x: side, y: -side)
        return Path(outline).applying(transform)
    }

    /// The optical sizes the outlines are cut at; a size in between uses the one below.
    static func opticalSize(for size: CGFloat) -> Int {
        size < 24 ? 20 : (size < 40 ? 24 : 40)
    }
}

/// Glyph outlines on a one-point em square, by symbol and axis setting.
private final class SymbolOutlines: @unchecked Sendable {
    static let shared = SymbolOutlines()

    private struct Key: Hashable {
        let symbol: AtticSymbol
        let filled: Bool
        let weight: Int
        let opticalSize: Int
    }

    private struct FontKey: Hashable {
        let filled: Bool
        let weight: Int
        let opticalSize: Int
    }

    private var outlines: [Key: CGPath] = [:]
    private var fonts: [FontKey: CTFont] = [:]
    private let lock = NSLock()
    /// Outlines are taken from the font at this size and scaled down, so hinting never rounds them.
    private let referenceSize: CGFloat = 96

    func outline(_ symbol: AtticSymbol, filled: Bool, weight: Int, opticalSize: Int) -> CGPath? {
        let key = Key(symbol: symbol, filled: filled, weight: weight, opticalSize: opticalSize)
        lock.lock()
        defer { lock.unlock() }
        if let path = outlines[key] { return path }
        guard let font = font(filled: filled, weight: weight, opticalSize: opticalSize) else { return nil }
        var characters = Array(String(UnicodeScalar(symbol.codepoint) ?? " ").utf16)
        var glyphs = [CGGlyph](repeating: 0, count: characters.count)
        guard CTFontGetGlyphsForCharacters(font, &characters, &glyphs, characters.count), glyphs[0] != 0 else { return nil }
        var toUnit = CGAffineTransform(scaleX: 1 / referenceSize, y: 1 / referenceSize)
        guard let path = CTFontCreatePathForGlyph(font, glyphs[0], &toUnit) else { return nil }
        outlines[key] = path
        return path
    }

    private func font(filled: Bool, weight: Int, opticalSize: Int) -> CTFont? {
        let key = FontKey(filled: filled, weight: weight, opticalSize: opticalSize)
        if let font = fonts[key] { return font }
        guard let base = BundledFont.symbols.descriptor else { return nil }
        let variations: [NSNumber: NSNumber] = [
            axisTag("FILL"): NSNumber(value: filled ? 1 : 0),
            axisTag("wght"): NSNumber(value: weight),
            axisTag("opsz"): NSNumber(value: opticalSize),
        ]
        let descriptor = CTFontDescriptorCreateCopyWithAttributes(base, [kCTFontVariationAttribute: variations] as CFDictionary)
        let font = CTFontCreateWithFontDescriptor(descriptor, referenceSize, nil)
        fonts[key] = font
        return font
    }
}
