package app.attic.player.playback

import androidx.media3.session.SessionCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session moved from a plain media session to a library session for the car. These check that the
 * commands the notification, lock screen, widget and app send are still accepted and routed.
 */
class AtticCommandsTest {
    @Test
    fun `each custom action routes to its handler`() {
        assertEquals(AtticCommands.Route.Shuffle, AtticCommands.route("app.attic.player.SHUFFLE"))
        assertEquals(AtticCommands.Route.Like, AtticCommands.route("app.attic.player.LIKE"))
        assertEquals(AtticCommands.Route.SleepTimer, AtticCommands.route("app.attic.player.SLEEP_TIMER"))
        assertEquals(AtticCommands.Route.SmartShuffleStart, AtticCommands.route("app.attic.player.SMART_SHUFFLE"))
        assertEquals(AtticCommands.Route.SmartShuffleStop, AtticCommands.route("app.attic.player.SMART_SHUFFLE_OFF"))
        assertNull(AtticCommands.route("app.attic.player.SOMETHING_ELSE"))
    }

    @Test
    fun `the notification buttons and the app's sleep timer send the routed actions`() {
        assertEquals(AtticCommands.Route.Shuffle, AtticCommands.route(AtticCommands.SHUFFLE_COMMAND.customAction))
        assertEquals(AtticCommands.Route.Like, AtticCommands.route(AtticCommands.LIKE_COMMAND.customAction))
        assertEquals(AtticCommands.Route.SleepTimer, AtticCommands.route(SleepTimerSession.COMMAND.customAction))
        assertEquals(AtticCommands.Route.SmartShuffleStart, AtticCommands.route(SmartShuffleSession.START.customAction))
        assertEquals(AtticCommands.Route.SmartShuffleStop, AtticCommands.route(SmartShuffleSession.STOP.customAction))
    }

    @Test
    fun `controllers get the custom commands plus every library command`() {
        val available = AtticCommands.available()
        // Compared by action: a custom command's equals needs TextUtils, which is a stub off the device.
        val actions = available.commands.map { it.customAction }
        assertTrue(AtticCommands.SHUFFLE in actions)
        assertTrue(AtticCommands.LIKE in actions)
        assertTrue(SleepTimerSession.ACTION in actions)
        assertTrue(SmartShuffleSession.START_ACTION in actions)
        assertTrue(SmartShuffleSession.STOP_ACTION in actions)
        for (code in listOf(
            SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM,
            SessionCommand.COMMAND_CODE_LIBRARY_SEARCH,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT,
            SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE,
        )) {
            assertTrue("library command $code", available.contains(code))
        }
    }
}
