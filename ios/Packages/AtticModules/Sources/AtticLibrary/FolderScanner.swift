import AtticCore
import Foundation

/// Walks a picked folder for audio files, off the main actor.
public struct FolderScanner: FolderScanning {
    private static let audioExtensions: Set<String> = [
        "mp3", "m4a", "aac", "flac", "wav", "aif", "aiff", "caf", "alac", "m4b",
    ]

    public init() {}

    public func scan(root: LibraryRoot, at url: URL) async -> [ScannedFile] {
        let rootID = root.id
        let extensions = Self.audioExtensions
        return await Task.detached(priority: .userInitiated) { () -> [ScannedFile] in
            let keys: [URLResourceKey] = [.isRegularFileKey, .contentModificationDateKey, .fileSizeKey]
            guard let walk = FileManager.default.enumerator(
                at: url,
                includingPropertiesForKeys: keys,
                options: [.skipsHiddenFiles, .skipsPackageDescendants]
            ) else { return [] }
            var found: [ScannedFile] = []
            for case let fileURL as URL in walk {
                guard extensions.contains(fileURL.pathExtension.lowercased()) else { continue }
                let values = try? fileURL.resourceValues(forKeys: Set(keys))
                guard values?.isRegularFile == true else { continue }
                guard let relativePath = TrackKey.relativePath(of: fileURL, under: url) else { continue }
                found.append(
                    ScannedFile(
                        rootID: rootID,
                        relativePath: relativePath,
                        fileName: fileURL.lastPathComponent,
                        url: fileURL,
                        modified: values?.contentModificationDate ?? .distantPast,
                        size: Int64(values?.fileSize ?? 0)
                    )
                )
            }
            return found.sorted { $0.key < $1.key }
        }.value
    }
}
