package app.attic.player.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands

/**
 * What a controller may ask of the session: every standard player and library command (the library
 * ones are what let a car browse and search), plus Attic's own shuffle, favourite, sleep timer and
 * smart shuffle.
 */
object AtticCommands {
    const val SHUFFLE = "app.attic.player.SHUFFLE"
    const val LIKE = "app.attic.player.LIKE"

    // New bundles rather than Bundle.EMPTY, which is null where the unit tests run. Commands compare by action alone.
    val SHUFFLE_COMMAND = SessionCommand(SHUFFLE, Bundle())
    val LIKE_COMMAND = SessionCommand(LIKE, Bundle())

    enum class Route { Shuffle, Like, SleepTimer, SmartShuffleStart, SmartShuffleStop }

    /** The handler for a custom command's action, or null for one the session doesn't know. */
    fun route(action: String): Route? = when (action) {
        SHUFFLE -> Route.Shuffle
        LIKE -> Route.Like
        SleepTimerSession.ACTION -> Route.SleepTimer
        SmartShuffleSession.START_ACTION -> Route.SmartShuffleStart
        SmartShuffleSession.STOP_ACTION -> Route.SmartShuffleStop
        else -> null
    }

    @OptIn(UnstableApi::class)
    fun available(): SessionCommands =
        MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(SHUFFLE_COMMAND)
            .add(LIKE_COMMAND)
            .add(SleepTimerSession.COMMAND)
            .add(SmartShuffleSession.START)
            .add(SmartShuffleSession.STOP)
            .build()
}
