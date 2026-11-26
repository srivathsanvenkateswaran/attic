import AtticCore
import Foundation

/// Security-scoped bookmarks for the picked folders, saved through a [LibraryStoring].
@MainActor
public final class FolderAccess: FolderAccessing {
    private let store: LibraryStoring
    public private(set) var roots: [LibraryRoot]
    /// Roots whose security scope is started. Each starts once and stays open for the app's life,
    /// so a scan or a playback does not pay for the scope again.
    private var open: [UUID: URL] = [:]

    public init(store: LibraryStoring) {
        self.store = store
        roots = store.loadRoots()
    }

    public func addFolder(_ url: URL) throws -> LibraryRoot {
        let path = url.standardizedFileURL.path
        if let existing = roots.first(where: { resolve($0)?.standardizedFileURL.path == path }) {
            return existing
        }
        _ = url.startAccessingSecurityScopedResource()
        let data = try url.bookmarkData(
            options: .minimalBookmark, includingResourceValuesForKeys: nil, relativeTo: nil
        )
        let root = LibraryRoot(name: url.lastPathComponent, bookmark: data)
        roots.append(root)
        store.saveRoots(roots)
        open[root.id] = url
        return root
    }

    public func removeFolder(id: UUID) {
        if let url = open.removeValue(forKey: id) {
            url.stopAccessingSecurityScopedResource()
        }
        roots.removeAll { $0.id == id }
        store.saveRoots(roots)
    }

    public func resolve(_ root: LibraryRoot) -> URL? {
        if let url = open[root.id] { return url }
        var stale = false
        guard let url = try? URL(
            resolvingBookmarkData: root.bookmark, options: [], relativeTo: nil, bookmarkDataIsStale: &stale
        ) else { return nil }
        // False for a URL that is not security scoped, such as a temp folder in a test.
        _ = url.startAccessingSecurityScopedResource()
        if stale,
           let fresh = try? url.bookmarkData(
               options: .minimalBookmark, includingResourceValuesForKeys: nil, relativeTo: nil
           ),
           let index = roots.firstIndex(where: { $0.id == root.id }) {
            roots[index].bookmark = fresh
            store.saveRoots(roots)
        }
        open[root.id] = url
        return url
    }

    public func fileURL(for track: Track) -> URL? {
        guard let root = roots.first(where: { $0.id == track.rootID }), let url = resolve(root) else { return nil }
        var file = url
        // The first component of the relative path is the root's own name, already in the URL.
        let parts = track.relativePath.split(separator: "/").map(String.init)
        for part in parts.dropFirst() {
            file.append(path: part, directoryHint: .isDirectory)
        }
        return file.appending(path: track.fileName, directoryHint: .notDirectory)
    }
}
