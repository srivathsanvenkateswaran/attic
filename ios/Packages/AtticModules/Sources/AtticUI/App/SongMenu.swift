import AtticCore
import AtticKit
import SwiftUI

/// The items of a song's menu, for `RowMoreMenu(title:) { SongMenuItems(track:) }` and for
/// `.contextMenu { SongMenuItems(track:) }` on the same row. `playlistID` adds "Remove from playlist".
struct SongMenuItems: View {
    let track: Track
    var playlistID: UUID? = nil
    var playlistIndex: Int? = nil
    @Environment(AppModel.self) private var app
    @Environment(Router.self) private var router

    var body: some View {
        Button { app.player.playNext([track]); router.show(S.messagePlayingNext) } label: {
            Text(S.menuPlayNext)
        }
        Button { app.player.addToQueue([track]); router.show(S.messageAddedToQueue) } label: {
            Text(S.menuAddToQueue)
        }
        Button { router.addingToPlaylist = [track] } label: { Text(S.menuAddToPlaylist) }
        Button { app.toggleLike(track) } label: {
            Text(app.isLiked(track) ? S.favouriteRemove : S.favouriteAdd)
        }
        if let albumKey = track.albumKey, app.library.library.albumByKey[albumKey] != nil {
            Button { router.open(.album(key: albumKey)) } label: { Text(S.menuGoToAlbum) }
        }
        if let artistKey = track.artistKeys.first, app.library.library.artistByKey[artistKey] != nil {
            Button { router.open(.artist(key: artistKey)) } label: { Text(S.menuGoToArtist) }
        }
        if let playlistID, let playlistIndex {
            Button(role: .destructive) { app.removeFromPlaylist(playlistID, at: playlistIndex) } label: {
                Text(S.menuRemoveFromPlaylist)
            }
        }
    }
}
