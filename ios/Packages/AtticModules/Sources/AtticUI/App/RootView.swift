import AtticCore
import AtticKit
import SwiftUI

/// The app's root: onboarding until a folder is picked, then the tabs with the mini player docked
/// above the tab bar, and Now Playing, the queue and lyrics over them.
/// The shell's look is specified in ios/DESIGN.md ("Shell").
public struct RootView: View {
    @State private var app: AppModel
    @State private var router = Router()

    public init(app: AppModel) {
        _app = State(initialValue: app)
    }

    public var body: some View {
        Group {
            if app.library.roots.isEmpty {
                OnboardingScreen()
            } else {
                AppShell()
            }
        }
        .environment(app)
        .environment(router)
        .preferredColorScheme(app.themeMode.colorScheme)
        .task { await app.start() }
    }
}

extension ThemeMode {
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

/// STUB: the shell task restyles this to DESIGN.md.
struct AppShell: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    var body: some View {
        @Bindable var router = router
        TabView(selection: $router.tab) {
            Tab("Home", systemImage: "house", value: AppTab.home) {
                NavigationStack(path: $router.homePath) {
                    HomeScreen().withRoutes()
                }
            }
            Tab("Search", systemImage: "magnifyingglass", value: AppTab.search) {
                NavigationStack(path: $router.searchPath) {
                    SearchScreen().withRoutes()
                }
            }
            Tab("Library", systemImage: "music.note.list", value: AppTab.library) {
                NavigationStack(path: $router.libraryPath) {
                    LibraryScreen().withRoutes()
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if app.player.state.current != nil {
                MiniPlayer()
            }
        }
        .fullScreenCover(isPresented: $router.showNowPlaying) {
            NowPlayingScreen()
                .sheet(isPresented: $router.showQueue) { QueueScreen() }
                .sheet(isPresented: $router.showLyrics) { LyricsScreen() }
        }
    }
}

extension View {
    /// Every pushed screen, for a tab's NavigationStack.
    func withRoutes() -> some View {
        navigationDestination(for: Route.self) { route in
            switch route {
            case .album(let key): AlbumScreen(key: key)
            case .artist(let key): ArtistScreen(key: key)
            case .folder(let path): FolderScreen(path: path)
            case .playlist(let id): PlaylistScreen(id: id)
            case .favourites: FavouritesScreen()
            case .settings: SettingsScreen()
            case .licences: LicencesScreen()
            case .licence(let id): LicenceScreen(id: id)
            }
        }
    }
}
