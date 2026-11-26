import AtticCore
import AtticKit
import SwiftUI
import UniformTypeIdentifiers

/// Settings, laid out as in ios/DESIGN.md.
struct SettingsScreen: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    @State private var collapsed = false
    @State private var showsImporter = false
    @State private var folderToRemove: SettingsState.Folder?

    var body: some View {
        let model = makeSettingsModel(app: app, router: router)
        let state = model.state
        let actions = model.actions

        return VStack(spacing: 0) {
            DetailAppBar(
                collapsed ? S.settingsTitle : "",
                surface: collapsed ? .scrolled : .plain,
                onBack: actions.back
            )
            ScrollView {
                LazyVStack(spacing: 0) {
                    LargeTitle(S.settingsTitle)

                    SettingsSection(S.settingsAppearance)
                    ListGroup {
                        ConnectedButtonGroup(
                            state.themeLabels,
                            selection: state.themeIndex,
                            onGroupCell: true
                        ) { index in
                            actions.setTheme(state.themeChoices[index])
                        }
                        .padding(.vertical, 8)
                    }

                    SettingsSection(S.settingsFolders)
                    ListGroup {
                        ForEach(state.folders) { folder in
                            SettingsRow(
                                title: folder.name,
                                summary: Format.songs(folder.songCount),
                                symbol: .folder
                            ) {
                                AtticIconButton(.close, label: S.settingsFoldersRemoveCd(folder.name)) {
                                    if state.folders.count == 1 {
                                        folderToRemove = folder
                                    } else {
                                        actions.removeFolder(folder.id)
                                    }
                                }
                            }
                        }
                        SettingsNavigationRow(title: S.settingsFoldersAdd, symbol: .add) {
                            showsImporter = true
                        }
                        SettingsNavigationRow(
                            title: S.settingsFoldersRescan,
                            symbol: .refresh,
                            showsChevron: !state.isScanning,
                            action: actions.rescan
                        )
                        .overlay(alignment: .trailing) {
                            if state.isScanning {
                                LoadingIndicator(size: 24)
                                    .padding(.trailing, AtticSpacing.m)
                                    .allowsHitTesting(false)
                            }
                        }
                    }

                    SettingsSection(S.settingsLyrics)
                    ListGroup {
                        SettingsNote(S.settingsLyricsPrivacy)
                    }

                    SettingsSection(S.settingsAbout)
                    ListGroup {
                        SettingsRow(
                            title: "Attic",
                            summary: S.settingsAboutBody(Format.songs(state.songCount)),
                            symbol: .info
                        )
                        SettingsRow(title: S.settingsVersion, summary: state.version)
                        SettingsNavigationRow(title: S.settingsLicences, symbol: .openInNew, action: actions.openLicences)
                    }
                }
            }
            .onScrollOffset { offset in
                // The large title's 44pt line has gone (DESIGN.md, Settings).
                collapsed = offset > 44
            }
            .contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)
        }
        .toolbar(.hidden, for: .navigationBar)
        .fileImporter(isPresented: $showsImporter, allowedContentTypes: [.folder]) { result in
            if case .success(let url) = result { actions.addFolder(url) }
        }
        .confirmationDialog(
            S.settingsFoldersRemoveTitle(folderToRemove?.name ?? ""),
            isPresented: Binding(
                get: { folderToRemove != nil },
                set: { if !$0 { folderToRemove = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(S.settingsFoldersRemoveCd(folderToRemove?.name ?? ""), role: .destructive) {
                if let folderToRemove { actions.removeFolder(folderToRemove.id) }
                self.folderToRemove = nil
            }
            Button(S.cancel, role: .cancel) {}
        } message: {
            Text(S.settingsFoldersRemoveBody)
        }
    }
}
