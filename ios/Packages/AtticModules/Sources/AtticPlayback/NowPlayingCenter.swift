import AtticCore
import Foundation
import MediaPlayer

/// MPNowPlayingInfoCenter and MPRemoteCommandCenter.
/// STUB: implemented by the "now playing and remote commands" task.
@MainActor
public final class NowPlayingCenter: NowPlayingPublishing {
    public init() {}

    public func attach(to player: PlaybackControlling, artwork: ArtworkProviding) {}

    public func update() {}
}
