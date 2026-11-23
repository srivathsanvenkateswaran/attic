package app.attic.player.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** How smart shuffle crosses the session boundary, as [SleepTimerSession] does for the timer. */
object SmartShuffleSession {
    const val START_ACTION = "app.attic.player.SMART_SHUFFLE"
    const val STOP_ACTION = "app.attic.player.SMART_SHUFFLE_OFF"

    // New bundles rather than Bundle.EMPTY, which is null where the unit tests run (see AtticCommands).
    /** Starts it: the collection in its own order, the picks, and the name it plays under. */
    val START = SessionCommand(START_ACTION, Bundle())

    /** Ends it and takes the picks out of the queue. Shuffle stays on. */
    val STOP = SessionCommand(STOP_ACTION, Bundle())

    private const val BASE = "smart_base"
    private const val PICKS = "smart_picks"
    private const val CONTEXT = "smart_context"
    private const val KEEP_CURRENT = "smart_keep_current"

    fun args(baseKeys: List<String>, pickKeys: List<String>, contextName: String, keepCurrent: Boolean): Bundle = Bundle().apply {
        putStringArrayList(BASE, ArrayList(baseKeys))
        putStringArrayList(PICKS, ArrayList(pickKeys))
        putString(CONTEXT, contextName)
        putBoolean(KEEP_CURRENT, keepCurrent)
    }

    class Request(val baseKeys: List<String>, val pickKeys: List<String>, val contextName: String?, val keepCurrent: Boolean)

    fun requestFrom(args: Bundle) = Request(
        baseKeys = args.getStringArrayList(BASE).orEmpty(),
        pickKeys = args.getStringArrayList(PICKS).orEmpty(),
        contextName = args.getString(CONTEXT),
        keepCurrent = args.getBoolean(KEEP_CURRENT, false),
    )

    /** The picks still marked, published in the session extras beside the sleep timer. */
    fun putPicks(extras: Bundle, picks: Set<String>) {
        if (picks.isNotEmpty()) extras.putStringArrayList(PICKS, ArrayList(picks))
    }

    fun picksFrom(extras: Bundle): Set<String> = extras.getStringArrayList(PICKS)?.toSet().orEmpty()
}
