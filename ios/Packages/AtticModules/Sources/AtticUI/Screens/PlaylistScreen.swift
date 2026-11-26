import Foundation
import SwiftUI

/// One of the person's playlists.
struct PlaylistScreen: View {
    let id: UUID

    var body: some View {
        CollectionScreen(kind: .playlist(id: id))
    }
}
