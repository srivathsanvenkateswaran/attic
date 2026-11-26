import Foundation

/// An open-source notice shown on the licences screen. Names of software and licences are proper
/// nouns, so they stay as written.
public struct OpenSourceNotice: Identifiable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let licence: String
    /// File names in the bundled Licences folder.
    public let files: [String]
}

public enum OpenSourceNotices {
    public static let all: [OpenSourceNotice] = [
        OpenSourceNotice(id: "attic", name: "Attic", licence: "GNU General Public License v3.0", files: ["LICENSE-attic.txt"]),
        OpenSourceNotice(id: "google-sans-flex", name: "Google Sans Flex", licence: "SIL Open Font License 1.1", files: ["OFL-google-sans-flex.txt", "fonts-NOTICE.txt"]),
        OpenSourceNotice(id: "material-symbols", name: "Material Symbols", licence: "Apache License 2.0", files: ["fonts-NOTICE.txt", "LICENSE-material-symbols.txt"]),
    ]

    /// The text of a bundled licence file, or nil if it is missing.
    public static func text(of file: String) -> String? {
        guard let url = Bundle.module.url(forResource: file, withExtension: nil, subdirectory: "Licences") else { return nil }
        return try? String(contentsOf: url, encoding: .utf8)
    }
}
