import AtticCore
import Foundation

/// What the mini player shows.
struct MiniPlayerState {
    var track: Track?
    var title: String
    var artist: String
    var isPlaying: Bool
    var isLiked: Bool
    var durationMs: Int64
}

struct MiniPlayerActions {
    var open: () -> Void
    var playPause: () -> Void
    var next: () -> Void
    var toggleLike: () -> Void
}

@MainActor
func makeMiniPlayerModel(app: AppModel, router: Router) -> (state: MiniPlayerState, actions: MiniPlayerActions) {
    let player = app.player.state
    let track = player.current
    let state = MiniPlayerState(
        track: track,
        title: track?.title ?? "",
        artist: track?.artist ?? "",
        isPlaying: player.isPlaying,
        isLiked: app.isLiked(track),
        durationMs: player.durationMs
    )
    let actions = MiniPlayerActions(
        open: { router.showNowPlaying = true },
        playPause: { app.player.togglePlayPause() },
        next: { app.player.next() },
        toggleLike: { if let track { app.toggleLike(track) } }
    )
    return (state, actions)
}

/// The ring's fill, 0 to 1.
func miniPlayerProgress(positionMs: Int64, durationMs: Int64) -> Double {
    guard durationMs > 0 else { return 0 }
    return min(1, max(0, Double(positionMs) / Double(durationMs)))
}
