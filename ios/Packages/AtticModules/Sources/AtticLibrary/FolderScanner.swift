import AtticCore
import Foundation

/// STUB: implemented by the "folder bookmarks and scanning" task.
public struct FolderScanner: FolderScanning {
    public init() {}

    public func scan(root: LibraryRoot, at url: URL) async -> [ScannedFile] { [] }
}
