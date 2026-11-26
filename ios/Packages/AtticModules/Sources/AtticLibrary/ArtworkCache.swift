import AtticCore
import CryptoKit
import ImageIO
import UIKit

/// Covers for display: decoded and downsampled off the main thread, then cached in memory and on
/// disk. `locate` gives a song's file URL (FolderAccessing.fileURL), called on the main actor.
public final class ArtworkCache: ArtworkProviding, @unchecked Sendable {
    private let reader: MetadataReading
    private let locate: @MainActor (Track) -> URL?
    private let memory = NSCache<NSString, UIImage>()
    private let lock = NSLock()
    /// One decode per cache key, so a twelve-song album decodes its cover once, not twelve times.
    private var inFlight: [String: Task<UIImage?, Never>] = [:]

    public init(reader: MetadataReading, locate: @escaping @MainActor (Track) -> URL?) {
        self.reader = reader
        self.locate = locate
    }

    public func image(for track: Track, maxPixel: Int) async -> UIImage? {
        guard track.hasArtwork else { return nil }
        // Songs of one album share a cover, so one album's key covers all its songs.
        let key = "\(track.albumKey ?? track.key)#\(maxPixel)"
        if let cached = memory.object(forKey: key as NSString) { return cached }

        lock.lock()
        let task: Task<UIImage?, Never>
        if let started = inFlight[key] {
            task = started
            lock.unlock()
        } else {
            task = Task { await self.load(key: key, track: track, maxPixel: maxPixel) }
            inFlight[key] = task
            lock.unlock()
        }
        let image = await task.value
        lock.lock()
        inFlight[key] = nil
        lock.unlock()
        return image
    }

    private func load(key: String, track: Track, maxPixel: Int) async -> UIImage? {
        if let cached = memory.object(forKey: key as NSString) { return cached }
        let reader = self.reader
        let locate = self.locate
        guard let image = await Task.detached { () -> UIImage? in
            let file = ArtworkCache.diskFile(for: key)
            if let stored = try? Data(contentsOf: file), let decoded = UIImage(data: stored) {
                return decoded
            }
            guard let url = await locate(track), let data = await reader.artworkData(for: url) else { return nil }
            guard let thumbnail = ArtworkCache.thumbnail(from: data, maxPixel: maxPixel) else { return nil }
            // Written before the song is shown, so the next launch reads it instead of decoding.
            if let jpeg = thumbnail.jpegData(compressionQuality: 0.85) {
                try? FileManager.default.createDirectory(
                    at: file.deletingLastPathComponent(), withIntermediateDirectories: true
                )
                try? jpeg.write(to: file)
            }
            return thumbnail
        }.value else { return nil }
        memory.setObject(image, forKey: key as NSString)
        return image
    }

    private static func diskFile(for key: String) -> URL {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let name = SHA256.hash(data: Data(key.utf8)).map { String(format: "%02x", $0) }.joined()
        return caches
            .appendingPathComponent("Artwork", isDirectory: true)
            .appendingPathComponent(name + ".jpg")
    }

    private static func thumbnail(from data: Data, maxPixel: Int) -> UIImage? {
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixel,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
        else { return nil }
        return UIImage(cgImage: image)
    }
}
