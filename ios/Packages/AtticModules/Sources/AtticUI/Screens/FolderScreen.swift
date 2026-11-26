import SwiftUI

/// The songs directly in one folder, shown as a playlist.
struct FolderScreen: View {
    let path: String

    var body: some View {
        CollectionScreen(kind: .folder(path: path))
    }
}
