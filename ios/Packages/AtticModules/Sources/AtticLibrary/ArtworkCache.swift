import AtticCore
import UIKit

/// STUB: implemented by the "metadata and artwork" task.
/// `locate` gives a song's file URL (FolderAccessing.fileURL), called on the main actor.
public final class ArtworkCache: ArtworkProviding, @unchecked Sendable {
    private let reader: MetadataReading
    private let locate: @MainActor (Track) -> URL?

    public init(reader: MetadataReading, locate: @escaping @MainActor (Track) -> URL?) {
        self.reader = reader
        self.locate = locate
    }

    public func image(for track: Track, maxPixel: Int) async -> UIImage? { nil }
}
