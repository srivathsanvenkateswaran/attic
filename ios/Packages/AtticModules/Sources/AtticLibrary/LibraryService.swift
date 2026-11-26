import AtticCore
import Foundation
import Observation

/// Owns the library: the picked folders, the scan and the grouped [Library]. AtticUI reads
/// `library` and `isScanning`; everything else goes through the protocols it was built with.
@MainActor
@Observable
public final class LibraryService {
    public private(set) var library: Library = .empty
    public private(set) var roots: [LibraryRoot] = []
    public private(set) var isScanning = false
    /// Files found so far in the running scan, for onboarding's "Found N songs".
    public private(set) var scannedCount = 0

    @ObservationIgnored public let store: LibraryStoring
    @ObservationIgnored public let folders: FolderAccessing
    @ObservationIgnored private let scanner: FolderScanning
    @ObservationIgnored private let reader: MetadataReading
    @ObservationIgnored private var scanTask: Task<Void, Never>?

    public init(store: LibraryStoring, folders: FolderAccessing, scanner: FolderScanning, reader: MetadataReading) {
        self.store = store
        self.folders = folders
        self.scanner = scanner
        self.reader = reader
        roots = folders.roots
        library = LibraryIndex.build(store.loadTracks())
    }

    /// Adds a folder from the Files picker and rescans.
    public func addFolder(_ url: URL) async throws {
        _ = try folders.addFolder(url)
        roots = folders.roots
        await rescan()
    }

    public func removeFolder(id: UUID) async {
        folders.removeFolder(id: id)
        roots = folders.roots
        await rescan()
    }

    /// Scans every root. Unchanged files (same key and modification date) keep their stored tags;
    /// new or changed files are read again. A song whose file is gone is dropped from the library
    /// (its likes, plays and playlist entries stay in the store).
    public func rescan() async {
        if let scanTask { return await scanTask.value }
        let task = Task { await self.runScan() }
        scanTask = task
        await task.value
        scanTask = nil
    }

    private func runScan() async {
        isScanning = true
        scannedCount = 0
        defer { isScanning = false }
        let previous = Dictionary(store.loadTracks().map { ($0.key, $0) }, uniquingKeysWith: { a, _ in a })
        var found: [Track] = []
        for root in folders.roots {
            guard let url = folders.resolve(root) else { continue }
            let files = await scanner.scan(root: root, at: url)
            for file in files {
                if let old = previous[file.key], old.modified == file.modified, old.rootID == file.rootID {
                    found.append(old)
                } else {
                    let tags = await reader.tags(for: file.url)
                    found.append(LibraryIndex.makeTrack(file: file, tags: tags, dateAdded: previous[file.key]?.dateAdded ?? Date()))
                }
                scannedCount += 1
            }
        }
        // Two roots can hold the same relative path; the first one wins, as on Android.
        var seen = Set<String>()
        found = found.filter { seen.insert($0.key).inserted }
        store.saveTracks(found)
        library = LibraryIndex.build(found)
    }
}
