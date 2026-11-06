package app.attic.player.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserPlaylist(
    val id: Long,
    val name: String,
    val trackKeys: List<String>,
)

/** Liked songs, playlists and play history, all stored on the device. */
class UserData(
    private val dao: MusicDao,
    private val scope: CoroutineScope,
) {
    /** Newest first. */
    val likedKeys: StateFlow<List<String>> =
        dao.likedKeys().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val likedSet: StateFlow<Set<String>> =
        likedKeys.map { it.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    val playlists: StateFlow<List<UserPlaylist>> =
        combine(dao.playlists(), dao.playlistTracks()) { lists, entries ->
            val byPlaylist = entries.groupBy { it.playlistId }
            lists.map { p ->
                UserPlaylist(p.id, p.name, byPlaylist[p.id].orEmpty().map { it.trackKey })
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Most recent first. */
    val recentKeys: Flow<List<String>> = dao.history().map { rows -> rows.map { it.trackKey } }

    fun toggleLike(key: String) {
        scope.launch {
            if (key in likedSet.value) dao.unlike(key) else dao.like(LikedEntity(key, System.currentTimeMillis()))
        }
    }

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    fun renamePlaylist(id: Long, name: String) {
        scope.launch { dao.renamePlaylist(id, name.trim()) }
    }

    fun deletePlaylist(id: Long) {
        scope.launch { dao.deletePlaylist(id) }
    }

    /** Returns how many of [keys] were new to the playlist. A song only appears once per playlist. */
    suspend fun addToPlaylist(id: Long, keys: List<String>): Int {
        var position = dao.maxPosition(id)
        var added = 0
        for (key in keys) {
            if (dao.insertPlaylistTrack(PlaylistTrackEntity(id, key, position + 1)) != -1L) {
                position++
                added++
            }
        }
        return added
    }

    fun removeFromPlaylist(id: Long, key: String) {
        scope.launch { dao.removePlaylistTrack(id, key) }
    }

    fun reorderPlaylist(id: Long, keys: List<String>) {
        scope.launch { dao.reorderPlaylist(id, keys) }
    }
}
