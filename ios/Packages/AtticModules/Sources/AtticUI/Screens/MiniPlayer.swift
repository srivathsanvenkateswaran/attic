import AtticCore
import AtticKit
import SwiftUI

/// The mini player in the dock. The shell adds the 12pt side and 8pt top padding.
struct MiniPlayer: View {
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    var body: some View {
        let model = makeMiniPlayerModel(app: app, router: router)
        let state = model.state
        let actions = model.actions
        ArtworkImage(track: state.track, maxPixel: 160) { cover in
            // The ring moves twice a second; only this timeline view redraws.
            TimelineView(.periodic(from: .now, by: 0.5)) { _ in
                MiniPlayerBar(
                    title: state.title,
                    artist: state.artist,
                    cover: cover,
                    isPlaying: state.isPlaying,
                    isLiked: state.isLiked,
                    progress: miniPlayerProgress(positionMs: app.player.positionMs(), durationMs: state.durationMs),
                    onOpen: actions.open,
                    onPlayPause: actions.playPause,
                    onNext: actions.next,
                    onToggleLike: actions.toggleLike
                )
            }
        }
        .fromCover(state.track)
    }
}
