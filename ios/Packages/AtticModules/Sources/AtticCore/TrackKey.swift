import Foundation

/// Library keys. A song's key is its relative path plus its file name, the same rule as Android's
/// `Track.key` (`relativePath + displayName`), so saved data survives a rescan and matches across phones.
public enum TrackKey {
    /// `relativePath` must start with the root folder's name and end in "/" ("Music/Artist/").
    /// A path without the trailing slash gets one.
    public static func make(relativePath: String, fileName: String) -> String {
        let dir = relativePath.hasSuffix("/") || relativePath.isEmpty ? relativePath : relativePath + "/"
        return dir + fileName
    }

    /// The relative path of a file at `fileURL` under the picked folder `rootURL`, starting with the
    /// root's own name and ending in "/". Returns nil if the file is not under the root.
    /// Example: root ".../Music", file ".../Music/A/b.mp3" gives "Music/A/".
    public static func relativePath(of fileURL: URL, under rootURL: URL) -> String? {
        let root = rootURL.standardizedFileURL.resolvingSymlinksInPath().pathComponents
        let file = fileURL.standardizedFileURL.resolvingSymlinksInPath().pathComponents
        guard file.count > root.count, Array(file.prefix(root.count)) == root, let rootName = root.last else { return nil }
        let middle = file[root.count..<(file.count - 1)]
        return ([rootName] + middle).joined(separator: "/") + "/"
    }

    /// Lowercased, diacritics folded, whitespace collapsed, trimmed. Used for album and artist keys
    /// and for search.
    public static func normalize(_ text: String) -> String {
        let folded = text.folding(options: [.caseInsensitive, .diacriticInsensitive, .widthInsensitive], locale: nil)
        return folded.split(whereSeparator: \.isWhitespace).joined(separator: " ")
    }
}
