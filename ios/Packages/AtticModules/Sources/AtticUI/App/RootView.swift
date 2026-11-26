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
        .atticTheme(app.themeMode.kitMode)
        .dynamicTypeSize(...DynamicTypeSize.accessibility2)
        .task { await app.start() }
    }
}

extension ThemeMode {
    var kitMode: AtticThemeMode {
        switch self {
        case .system: .system
        case .light: .light
        case .dark: .dark
        }
    }
}

/// The three tab stacks, the dock under them, and the overlays above.
struct AppShell: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @State private var keyboardUp = false
    @State private var dockHeight: CGFloat = 0

    var body: some View {
        @Bindable var router = router
        ZStack {
            stack(.home, path: $router.homePath) { HomeScreen() }
            stack(.search, path: $router.searchPath) { SearchScreen() }
            stack(.library, path: $router.libraryPath) { LibraryScreen() }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            dock
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { dockHeight = $0 }
        }
        .snackbar($router.message, bottomInset: keyboardUp ? 0 : dockHeight)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            keyboardUp = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            keyboardUp = false
        }
        .fullScreenCover(isPresented: $router.showNowPlaying) {
            NowPlayingScreen()
                .sheet(isPresented: $router.showQueue) {
                    QueueScreen().overlays(app: app, router: router, mode: app.themeMode)
                }
                .sheet(isPresented: $router.showLyrics) {
                    LyricsScreen().overlays(app: app, router: router, mode: app.themeMode)
                }
                .overlays(app: app, router: router, mode: app.themeMode)
                .presentationBackground(.clear)
        }
        .sheet(isPresented: pickerShown) {
            PlaylistPickerSheet(tracks: router.addingToPlaylist ?? [])
                .overlays(app: app, router: router, mode: app.themeMode)
        }
    }

    private var pickerShown: Binding<Bool> {
        Binding(
            get: { router.addingToPlaylist != nil },
            set: { if !$0 { router.addingToPlaylist = nil } }
        )
    }

    private var tabIndex: Binding<Int> {
        Binding(
            get: { AppTab.allCases.firstIndex(of: router.tab) ?? 0 },
            set: { router.tab = AppTab.allCases[$0] }
        )
    }

    private var tabItems: [AtticTabItem] {
        [
            AtticTabItem(title: S.tabHome, symbol: .home),
            AtticTabItem(title: S.tabSearch, symbol: .search),
            AtticTabItem(title: S.tabLibrary, symbol: .libraryMusic),
        ]
    }

    /// The tab bar with the mini player above it. Hidden while the keyboard is up.
    @ViewBuilder private var dock: some View {
        if !keyboardUp {
            VStack(spacing: 0) {
                if app.player.state.current != nil {
                    MiniPlayer()
                        .padding(.horizontal, 12) // DESIGN.md, "The dock"
                        .padding(.bottom, 8)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                AtticTabBar(items: tabItems, selection: tabIndex, onReselect: { _ in router.popToRoot() })
            }
            .animation(AtticMotion.defaultSpatial, value: app.player.state.current != nil)
        }
    }

    /// One tab's stack. All three stay alive so each keeps its place; only the selected one is seen.
    private func stack<Root: View>(_ tab: AppTab, path: Binding<[Route]>, @ViewBuilder root: () -> Root) -> some View {
        let selected = router.tab == tab
        return NavigationStack(path: path) {
            root()
                .toolbar(.hidden, for: .navigationBar)
                .withRoutes()
        }
        .opacity(selected ? 1 : 0)
        .allowsHitTesting(selected)
        .accessibilityHidden(!selected)
    }
}

private extension View {
    /// Environment and theme for a cover or sheet, which SwiftUI presents outside the shell's tree.
    func overlays(app: AppModel, router: Router, mode: ThemeMode) -> some View {
        environment(app).environment(router).atticTheme(mode.kitMode)
    }
}

// The system bar is hidden, which disables the edge swipe. This puts it back, and only while a
// stack has something to pop.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ g: UIGestureRecognizer) -> Bool { viewControllers.count > 1 }
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
