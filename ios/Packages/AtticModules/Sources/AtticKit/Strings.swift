import Foundation

/// The labels the kit's own controls read out. They are English by default; an app that is localised
/// sets them once at launch, before any view is drawn.
public enum AtticKitStrings {
    nonisolated(unsafe) public static var play = "Play"
    nonisolated(unsafe) public static var pause = "Pause"
    nonisolated(unsafe) public static var previous = "Previous"
    nonisolated(unsafe) public static var next = "Next"
    nonisolated(unsafe) public static var back = "Back"
    nonisolated(unsafe) public static var clearSearch = "Clear search"
    nonisolated(unsafe) public static var favouriteAdd = "Add to favourites"
    nonisolated(unsafe) public static var favouriteRemove = "Remove from favourites"
    nonisolated(unsafe) public static var favourites = "Favourites"
    nonisolated(unsafe) public static var shuffleOn = "Turn shuffle on"
    nonisolated(unsafe) public static var shuffleOff = "Turn shuffle off"
    nonisolated(unsafe) public static var repeatOff = "Repeat off, tap to repeat all"
    nonisolated(unsafe) public static var repeatAll = "Repeat all, tap to repeat one"
    nonisolated(unsafe) public static var repeatOne = "Repeat one, tap to turn off"
    nonisolated(unsafe) public static var loading = "Loading"
    nonisolated(unsafe) public static var playing = "Playing"
    nonisolated(unsafe) public static var openPlayerHint = "Opens the player"
    /// "More options for {title}".
    nonisolated(unsafe) public static var moreOptionsFor: (String) -> String = { "More options for \($0)" }
    /// "Shuffle {name}".
    nonisolated(unsafe) public static var shuffleNamed: (String) -> String = { "Shuffle \($0)" }
    /// "Add {name} to a playlist".
    nonisolated(unsafe) public static var addNamedToPlaylist: (String) -> String = { "Add \($0) to a playlist" }
    /// "1:18 of 3:52".
    nonisolated(unsafe) public static var positionOfLength: (String, String) -> String = { "\($0) of \($1)" }
}

/// Text the kit formats the same way everywhere.
public enum AtticFormat {
    /// A position or length in seconds as "m:ss", or "h:mm:ss" from an hour.
    public static func duration(_ seconds: Double) -> String {
        let total = Int(max(0, seconds.isFinite ? seconds : 0).rounded(.down))
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%d:%02d", m, s)
    }
}
