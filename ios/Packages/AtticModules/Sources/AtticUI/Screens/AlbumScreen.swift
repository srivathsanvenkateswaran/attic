import SwiftUI

/// An album: its cover and artist, then its songs in track order.
struct AlbumScreen: View {
    let key: String

    var body: some View {
        CollectionScreen(kind: .album(key: key))
    }
}
