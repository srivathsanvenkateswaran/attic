package app.attic.player.playback

import android.app.SearchManager
import android.content.Intent
import android.provider.MediaStore
import app.attic.player.AtticApp

/**
 * "Play X on Attic" said to the phone's assistant arrives as MEDIA_PLAY_FROM_SEARCH. The query and the
 * assistant's hints go to the playback service, which resolves them as it does a request from a car.
 */
object PlayFromSearch {
    fun handle(app: AtticApp, intent: Intent?): Boolean {
        if (intent?.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return false
        app.player.playFromSearch(intent.getStringExtra(SearchManager.QUERY).orEmpty(), intent.extras)
        return true
    }
}
