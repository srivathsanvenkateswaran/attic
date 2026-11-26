import AtticCore
import SwiftUI
import UIKit

/// Loads a song's cover through AppModel.artwork and hands it to `content`, nil while loading or when
/// the song has none. Screens wrap kit components (which take `Image?`) with it:
/// `ArtworkImage(track: t, maxPixel: 160) { image in CoverArt(image: image, ...) }`.
struct ArtworkImage<Content: View>: View {
    let track: Track?
    var maxPixel: Int = 300
    @ViewBuilder let content: (Image?) -> Content
    @Environment(AppModel.self) private var app
    @State private var image: UIImage?

    var body: some View {
        content(image.map { Image(uiImage: $0) })
            .task(id: track?.key) {
                image = nil
                guard let track, track.hasArtwork else { return }
                image = await app.artwork.image(for: track, maxPixel: maxPixel)
            }
    }
}
