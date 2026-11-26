import Foundation

/// The notices on the Open-source licences list. Names of works and licences are proper nouns.
struct LicencesState {
    let notices: [OpenSourceNotice]
}

struct LicencesActions {
    let back: () -> Void
    let open: (String) -> Void
}

@MainActor
func makeLicencesModel(router: Router) -> (state: LicencesState, actions: LicencesActions) {
    let state = LicencesState(notices: OpenSourceNotices.all)
    let actions = LicencesActions(
        back: { router.pop() },
        open: { router.push(.licence(id: $0)) }
    )
    return (state, actions)
}

/// The notice whose text is on screen, and its paragraphs once they have loaded.
struct LicenceState {
    let notice: OpenSourceNotice
}

struct LicenceActions {
    let back: () -> Void
    /// Reads the bundled files off the main thread and returns one string per paragraph.
    let load: () async -> [String]
}

@MainActor
func makeLicenceModel(id: String, router: Router) -> (state: LicenceState, actions: LicenceActions) {
    let notice = OpenSourceNotices.all.first { $0.id == id } ?? OpenSourceNotices.all[0]
    let load: () async -> [String] = {
        let files = notice.files
        return await Task.detached(priority: .userInitiated) {
            files.flatMap { file in
                OpenSourceNotices.text(of: file).map(licenceParagraphs) ?? []
            }
        }.value
    }
    let actions = LicenceActions(back: { router.pop() }, load: load)
    return (state: LicenceState(notice: notice), actions: actions)
}

/// Splits a licence at its blank lines. Wrapped prose becomes one line per paragraph; blocks whose
/// line breaks carry meaning (lists, rules, indented text) stay as laid out.
func licenceParagraphs(_ text: String) -> [String] {
    let normalized = text.replacingOccurrences(of: "\r\n", with: "\n")
    var blocks: [[String]] = []
    var current: [String] = []
    for line in normalized.split(separator: "\n", omittingEmptySubsequences: false) {
        let trimmed = String(line).trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty {
            if !current.isEmpty { blocks.append(current); current = [] }
        } else {
            current.append(String(line).replacingOccurrences(of: "\\s+$", with: "", options: .regularExpression))
        }
    }
    if !current.isEmpty { blocks.append(current) }
    return blocks.compactMap { block in
        guard block.contains(where: { !$0.trimmingCharacters(in: .whitespaces).isEmpty }) else { return nil }
        let laidOut = block.dropFirst().contains { $0.hasPrefix(" ") || $0.hasPrefix("\t") }
            || block.contains { line in
                let trimmed = line.trimmingCharacters(in: .whitespaces)
                return trimmed.hasPrefix("- ") || trimmed.hasPrefix("* ") || isRule(trimmed)
            }
        return laidOut ? block.joined(separator: "\n") : block.map { $0.trimmingCharacters(in: .whitespaces) }.joined(separator: " ")
    }
}

private func isRule(_ line: String) -> Bool {
    line.count >= 3 && line.allSatisfy { $0 == "=" || $0 == "-" || $0 == "_" || $0 == "*" }
}
