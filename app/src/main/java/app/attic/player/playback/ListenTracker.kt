package app.attic.player.playback

/** Measures how long each song was actually heard, for the listening log. */
class ListenTracker(private val clock: () -> Long = System::currentTimeMillis) {
    data class Listen(
        val key: String,
        val startedAt: Long,
        val listenedMs: Long,
        val durationMs: Long,
        val outcome: String,
    )

    private var key: String? = null
    private var durationMs = 0L
    private var startedAt = 0L
    private var listenedMs = 0L
    private var playingSince: Long? = null

    fun start(key: String, durationMs: Long) {
        this.key = key
        this.durationMs = durationMs
        startedAt = clock()
        listenedMs = 0
        playingSince = null
    }

    fun playing(isPlaying: Boolean) {
        val since = playingSince
        when {
            isPlaying && since == null -> playingSince = clock()
            !isPlaying && since != null -> {
                listenedMs += clock() - since
                playingSince = null
            }
        }
    }

    /** The finished listen, or null if nothing was heard. [automatic] means the song ended by itself. */
    fun finish(automatic: Boolean): Listen? {
        playing(false)
        val finishedKey = key ?: return null
        key = null
        if (listenedMs <= 0) return null
        val outcome = when {
            automatic || listenedMs >= durationMs * FINISHED_SHARE -> FINISHED
            listenedMs < durationMs * SKIPPED_SHARE -> SKIPPED
            else -> PARTIAL
        }
        return Listen(finishedKey, startedAt, listenedMs, durationMs, outcome)
    }

    companion object {
        const val FINISHED = "finished"
        const val PARTIAL = "partial"
        const val SKIPPED = "skipped"
        const val REMOVED = "removed"
        private const val FINISHED_SHARE = 0.9
        private const val SKIPPED_SHARE = 0.5
    }
}
