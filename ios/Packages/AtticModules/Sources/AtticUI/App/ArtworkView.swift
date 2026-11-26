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

/// The colour a player surface takes from a song's cover: the average of a tiny downsample,
/// which is enough because `AtticScheme.from(seed:)` reads only its hue.
enum ArtSeed {
    static func color(of image: UIImage) -> UIColor? {
        guard let cg = image.cgImage else { return nil }
        var pixel = [UInt8](repeating: 0, count: 4)
        guard let context = CGContext(
            data: &pixel, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }
        context.interpolationQuality = .medium
        context.draw(cg, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        return UIColor(red: CGFloat(pixel[0]) / 255, green: CGFloat(pixel[1]) / 255, blue: CGFloat(pixel[2]) / 255, alpha: 1)
    }
}

/// Applies `.fromArt(seed:)` with the seed of `track`'s cover, loaded in the background.
/// Nil track or no cover keeps the app scheme.
struct FromCover: ViewModifier {
    let track: Track?
    @Environment(AppModel.self) private var app
    @State private var seed: UIColor?

    func body(content: Content) -> some View {
        content
            .fromArt(seed: seed)
            .task(id: track?.key) {
                guard let track, track.hasArtwork,
                      let image = await app.artwork.image(for: track, maxPixel: 64) else { seed = nil; return }
                seed = ArtSeed.color(of: image)
            }
    }
}

extension View {
    /// Player surfaces only (mini player, Now Playing, queue, lyrics, collection headers).
    func fromCover(_ track: Track?) -> some View { modifier(FromCover(track: track)) }
}
