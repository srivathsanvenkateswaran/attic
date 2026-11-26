import AtticCore
import Foundation

/// What Settings shows. Each folder carries the number of songs found under it.
struct SettingsState {
    struct Folder: Identifiable {
        let id: UUID
        let name: String
        let songCount: Int
    }

    let themeMode: ThemeMode
    let folders: [Folder]
    let isScanning: Bool
    let songCount: Int
    let version: String

    /// The themes in the order the group shows them.
    let themeChoices: [ThemeMode]
    let themeLabels: [String]

    var themeIndex: Int { themeChoices.firstIndex(of: themeMode) ?? 0 }
}

struct SettingsActions {
    let setTheme: (ThemeMode) -> Void
    let addFolder: (URL) -> Void
    let removeFolder: (UUID) -> Void
    let rescan: () -> Void
    let openLicences: () -> Void
    let back: () -> Void
}

@MainActor
func makeSettingsModel(app: AppModel, router: Router) -> (state: SettingsState, actions: SettingsActions) {
    let tracks = app.library.library.tracks
    let folders = app.library.roots.map { root in
        SettingsState.Folder(
            id: root.id,
            name: root.name,
            songCount: tracks.filter { $0.rootID == root.id }.count
        )
    }
    let state = SettingsState(
        themeMode: app.themeMode,
        folders: folders,
        isScanning: app.library.isScanning,
        songCount: tracks.count,
        version: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
        themeChoices: ThemeMode.allCases,
        themeLabels: [S.settingsThemeSystem, S.settingsThemeLight, S.settingsThemeDark]
    )
    let actions = SettingsActions(
        setTheme: { app.themeMode = $0 },
        addFolder: { url in Task { try? await app.library.addFolder(url) } },
        removeFolder: { id in Task { await app.library.removeFolder(id: id) } },
        rescan: { Task { await app.library.rescan() } },
        openLicences: { router.push(.licences) },
        back: { router.pop() }
    )
    return (state, actions)
}
