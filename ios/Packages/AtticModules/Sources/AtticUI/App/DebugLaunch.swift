#if DEBUG
import AtticCore
import Foundation

/// Launch arguments for running the app without touching it, in the simulator and for screenshots.
/// Debug builds only.
///
///   -atticFolder <path>   add this folder if none is saved yet (instead of the Files picker)
///   -atticPlay <n>        after the scan, play the library from song n (title order)
///   -atticScreen <name>   then open: nowPlaying, queue, lyrics, library, search, settings,
///                         licences, album, artist, favourites
///   -atticTheme <mode>    system, light or dark
@MainActor
enum DebugLaunch {
    static func run(app: AppModel, router: Router) async {
        let defaults = UserDefaults.standard
        if let mode = defaults.string(forKey: "atticTheme").flatMap(ThemeMode.init(rawValue:)) {
            app.themeMode = mode
        }
        if let path = defaults.string(forKey: "atticFolder"), app.library.roots.isEmpty {
            try? await app.library.addFolder(URL(fileURLWithPath: path, isDirectory: true))
        }
        let library = app.library.library
        if let start = defaults.string(forKey: "atticPlay").flatMap(Int.init), !library.tracks.isEmpty {
            app.play(library.tracks, startAt: min(start, library.tracks.count - 1), context: nil)
        }
        guard let screen = defaults.string(forKey: "atticScreen") else { return }
        try? await Task.sleep(for: .milliseconds(600))
        switch screen {
        case "nowPlaying": router.showNowPlaying = true
        case "queue", "lyrics":
            router.showNowPlaying = true
            try? await Task.sleep(for: .milliseconds(800))
            if screen == "queue" { router.showQueue = true } else { router.showLyrics = true }
        case "library": router.tab = .library
        case "search": router.tab = .search
        case "settings": router.push(.settings)
        case "licences": router.push(.settings); router.push(.licences)
        case "album": if let album = library.albums.first { router.push(.album(key: album.key)) }
        case "artist": if let artist = library.artists.first { router.push(.artist(key: artist.key)) }
        case "favourites": router.push(.favourites)
        default: break
        }
    }
}
#endif
