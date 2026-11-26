import SwiftUI

/// The songs the person has liked, newest first.
struct FavouritesScreen: View {
    var body: some View {
        CollectionScreen(kind: .favourites)
    }
}
