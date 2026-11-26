import AtticCore
import Foundation

/// STUB: implemented by the "metadata and artwork" task.
public struct MetadataReader: MetadataReading {
    public init() {}

    public func tags(for url: URL) async -> RawTags { RawTags() }

    public func artworkData(for url: URL) async -> Data? { nil }
}
