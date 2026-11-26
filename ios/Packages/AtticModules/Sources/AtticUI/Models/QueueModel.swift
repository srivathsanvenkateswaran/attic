import AtticCore
import AtticKit
import Foundation

/// A song after the current one. The same song can be queued twice, so the id carries its occurrence.
struct UpNextRow: Identifiable {
    let id: String
    let track: Track
    /// A play-order index into the player's queue; it stays valid while rows move around it.
    let queueIndex: Int
}

enum QueueContextKind: Sendable {
    case favourites, album, artist, playlist, folder, other
}

struct QueueState {
    let current: Track?
    let isPlaying: Bool
    let contextName: String?
    let contextKind: QueueContextKind
    let upNext: [UpNextRow]
    /// What the bar shows under "Queue". A nil context plays from the whole library.
    let contextLabel: String
    let contextSymbol: AtticSymbol?
    let upNextLabel: String
}

struct QueueActions {
    let close: () -> Void
    let clearUpNext: () -> Void
    let jumpTo: (Int) -> Void
    let remove: (Int) -> Void
    /// The first argument is a queue index; the second is a position in the up-next list.
    let move: (Int, Int) -> Void
}

/// What the player only keeps as a name: a collection the current song belongs to wins, then a user playlist.
private func queueContextKind(
    name: String?, current: Track?, library: Library, playlistNames: [String]
) -> QueueContextKind {
    guard let name else { return .other }
    if name == S.favourites { return .favourites }
    if let current {
        if current.album == name { return .album }
        if current.artist == name || current.artistKeys.contains(where: { library.artistByKey[$0]?.name == name }) {
            return .artist
        }
        if current.folder == name { return .folder }
    }
    if playlistNames.contains(name) { return .playlist }
    return .other
}

@MainActor
func makeQueueModel(app: AppModel, router: Router) -> (state: QueueState, actions: QueueActions) {
    let player = app.player.state
    let current = player.current
    let upNextCount = max(0, player.queue.count - player.currentIndex - 1)

    var seen: [String: Int] = [:]
    var upNext: [UpNextRow] = []
    upNext.reserveCapacity(upNextCount)
    for (index, track) in player.queue.enumerated() {
        let occurrence = (seen[track.key] ?? 0) + 1
        seen[track.key] = occurrence
        guard index > player.currentIndex else { continue }
        upNext.append(UpNextRow(id: "\(track.key)#\(occurrence)", track: track, queueIndex: index))
    }

    let contextName = player.contextName
    let kind = queueContextKind(
        name: contextName,
        current: current,
        library: app.library.library,
        playlistNames: app.playlists.map(\.name)
    )
    let contextSymbol: AtticSymbol? = switch kind {
    case .favourites: .favorite
    case .album: .album
    case .artist: .person
    case .playlist: .playlistPlay
    case .folder: .folder
    case .other: nil
    }

    let state = QueueState(
        current: current,
        isPlaying: player.isPlaying,
        contextName: contextName,
        contextKind: kind,
        upNext: upNext,
        contextLabel: contextName ?? S.contextYourLibrary,
        contextSymbol: contextSymbol,
        upNextLabel: S.queueUpNext(count: upNext.count, S.grouped(upNext.count))
    )

    let currentIndex = player.currentIndex
    let actions = QueueActions(
        close: { router.showQueue = false },
        clearUpNext: { app.player.clearUpNext() },
        jumpTo: { index in app.player.jumpTo(index) },
        remove: { index in app.player.remove(index) },
        move: { from, position in
            let target = currentIndex + 1 + position
            guard from >= 0, from != target else { return }
            app.player.move(from: from, to: target)
        }
    )

    return (state, actions)
}
