import AtticCore
import Foundation

/// Security-scoped bookmarks for the picked folders, saved through a [LibraryStoring].
/// STUB: implemented by the "folder bookmarks and scanning" task.
@MainActor
public final class FolderAccess: FolderAccessing {
    private let store: LibraryStoring
    public private(set) var roots: [LibraryRoot]

    public init(store: LibraryStoring) {
        self.store = store
        roots = store.loadRoots()
    }

    public func addFolder(_ url: URL) throws -> LibraryRoot {
        let root = LibraryRoot(name: url.lastPathComponent, bookmark: Data())
        roots.append(root)
        store.saveRoots(roots)
        return root
    }

    public func removeFolder(id: UUID) {
        roots.removeAll { $0.id == id }
        store.saveRoots(roots)
    }

    public func resolve(_ root: LibraryRoot) -> URL? { nil }

    public func fileURL(for track: Track) -> URL? { nil }
}
