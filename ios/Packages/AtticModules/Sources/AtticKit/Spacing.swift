import SwiftUI

/// The distances the kit and the screens share, in points. Paddings that belong to one component
/// (a section header's 28 above and 12 below) are inside that component.
public enum AtticSpacing {
    // Steps for anything without a name of its own.
    public static let xxs: CGFloat = 2
    public static let xs: CGFloat = 4
    public static let s: CGFloat = 8
    public static let m: CGFloat = 12
    public static let l: CGFloat = 16
    public static let xl: CGFloat = 20
    public static let xxl: CGFloat = 24
    public static let xxxl: CGFloat = 32
    public static let huge: CGFloat = 48

    /// Groups, shelves, grids, chips and section headers from the screen edge.
    public static let screenMargin: CGFloat = 16
    /// Now Playing's and lyrics' side margin.
    public static let playerMargin: CGFloat = 24
    /// Between rows of a group.
    public static let groupGap: CGFloat = 2
    /// Between two groups.
    public static let groupSpacing: CGFloat = 12
    /// A song row and a settings row are at least this tall.
    public static let rowMinHeight: CGFloat = 64
    /// A collection row, with its 56pt lead, is at least this tall.
    public static let collectionRowMinHeight: CGFloat = 72
    /// Inside a song or collection row.
    public static let rowPadding = EdgeInsets(top: 8, leading: 12, bottom: 8, trailing: 4)
    /// Between a row's lead, text and trailing control.
    public static let rowGap: CGFloat = 12
    /// Between tiles in a shelf, and between grid columns.
    public static let shelfGap: CGFloat = 12
    /// Between grid rows; 16 with three columns.
    public static let gridRowGap: CGFloat = 20
    /// Between chips, and between buttons in a row.
    public static let chipGap: CGFloat = 8
    /// Between connected segments.
    public static let segmentGap: CGFloat = 2
    /// A detail bar, and a root bar once scrolled.
    public static let appBarHeight: CGFloat = 64
    /// A root bar at rest.
    public static let rootBarHeight: CGFloat = 72
    /// The tab bar, above the home indicator.
    public static let tabBarHeight: CGFloat = 64
    public static let miniPlayerHeight: CGFloat = 64
    /// The mini player from the screen edges.
    public static let miniPlayerInset: CGFloat = 12
    /// Between the mini player and the tab bar.
    public static let miniPlayerGap: CGFloat = 8
    /// Under the last item of a scrolling screen, above the dock.
    public static let dockClearance: CGFloat = 24
    /// The least tappable size.
    public static let touchTarget: CGFloat = 48
    /// How far a list scrolls before a bar has fully taken its scrolled look.
    public static let barScrollRange: CGFloat = 120
}
