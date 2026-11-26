import AtticCore
import AtticKit
import SwiftUI

/// The "Add to playlist" sheet, shown by the shell while `router.addingToPlaylist` is set.
struct PlaylistPickerSheet: View {
    let tracks: [Track]
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router
    @Environment(\.atticScheme) private var scheme
    @State private var naming = false
    @State private var name = ""

    var body: some View {
        VStack(spacing: 0) {
            SheetHandle()
            SheetTitle(S.playlistPickerTitle)
            ScrollView {
                LazyVStack(spacing: 0) {
                    SheetRow(.add, S.playlistPickerNew) { naming = true }
                    ForEach(app.playlists) { list in
                        SheetRow(.playlistPlay, list.name) { add(to: list) }
                    }
                }
            }
        }
        .background(scheme.surfaceContainerLow)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.hidden)
        .alert(S.dialogNewPlaylistTitle, isPresented: $naming) {
            TextField(S.dialogPlaylistNameLabel, text: $name)
            Button(S.cancel, role: .cancel) { name = "" }
            Button(S.create) {
                let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                name = ""
                guard !trimmed.isEmpty else { return }
                add(to: app.createPlaylist(name: trimmed))
            }
        }
    }

    private func add(to list: Playlist) {
        app.addToPlaylist(list.id, tracks: tracks)
        router.addingToPlaylist = nil
        router.show(S.messageAddedToPlaylist(list.name))
    }
}
