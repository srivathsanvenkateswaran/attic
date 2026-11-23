package app.attic.player.playback

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect

/**
 * Attic has no equaliser of its own. It hands its audio session to whichever one the phone has: the
 * system's sound effects panel or an installed equaliser app. Apps that apply effects to every player
 * listen for the open and close broadcasts and attach to the session.
 */
object Equaliser {
    /** The intent that opens an equaliser panel for [sessionId]. */
    fun panelIntent(context: Context, sessionId: Int): Intent =
        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)

    /** Whether anything on the phone opens that panel. Needs the `<queries>` entry in the manifest. */
    fun isAvailable(context: Context): Boolean =
        context.packageManager.resolveActivity(
            Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL),
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null

    /** Tells equaliser apps a music session has opened, so they attach before anything plays. */
    fun announceOpen(context: Context, sessionId: Int) = context.sendBroadcast(session(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, context, sessionId))

    fun announceClose(context: Context, sessionId: Int) = context.sendBroadcast(session(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, context, sessionId))

    private fun session(action: String, context: Context, sessionId: Int) =
        Intent(action)
            .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
}
