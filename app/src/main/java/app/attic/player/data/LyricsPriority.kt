package app.attic.player.data

/** Where a song's saved lyrics came from. Stored in [LyricsEntity.source]. */
enum class LyricsSource(val id: String) {
    Lrclib("lrclib"),

    /** A `.lrc` file with the song's name, in the song's folder. */
    Sidecar("sidecar"),

    /** The audio file's own tags. */
    Embedded("embedded"),
    ;

    companion object {
        fun of(id: String): LyricsSource = entries.firstOrNull { it.id == id } ?: Lrclib
    }
}

/** Lyrics found on the phone, before they are saved. */
data class LocalLyrics(val synced: String?, val plain: String?, val source: LyricsSource)

/** What to do with a song's saved lyrics row after looking at its files. */
sealed interface LyricsUpdate {
    data object Keep : LyricsUpdate
    data class Save(val lyrics: LocalLyrics) : LyricsUpdate

    /** The file that gave the saved lyrics no longer has them. */
    data object Remove : LyricsUpdate
}

/**
 * The order lyrics are taken in:
 * 1. a synced `.lrc` next to the song,
 * 2. synced lyrics in the file's tags (`SYLT`, or LRC text in `USLT` / `©lyr`),
 * 3. plain lyrics in the file's tags (`USLT` / `©lyr`),
 * 4. a `.lrc` next to the song with no timestamps,
 * 5. lyrics already downloaded from LRCLIB,
 * 6. a new LRCLIB download, which only happens when the user asks.
 */
object LyricsPriority {
    fun local(sidecar: String?, embedded: EmbeddedLyrics): LocalLyrics? {
        val sidecarText = sidecar?.removePrefix("\uFEFF")?.replace("\r\n", "\n")?.replace('\r', '\n')?.trim()?.takeIf { it.isNotBlank() }
        return when {
            sidecarText != null && LrcParser.parse(sidecarText).isNotEmpty() -> LocalLyrics(sidecarText, null, LyricsSource.Sidecar)
            embedded.synced != null -> LocalLyrics(embedded.synced, embedded.plain, LyricsSource.Embedded)
            embedded.plain != null -> LocalLyrics(null, embedded.plain, LyricsSource.Embedded)
            sidecarText != null && !LrcParser.isSynced(sidecarText) ->
                LrcParser.plainText(sidecarText)?.let { LocalLyrics(null, it, LyricsSource.Sidecar) }
            else -> null
        }
    }

    /** Local lyrics replace anything saved; saved LRCLIB lyrics stay when there are none. */
    fun update(local: LocalLyrics?, saved: LyricsEntity?): LyricsUpdate = when {
        local != null ->
            if (saved != null && saved.found && LyricsSource.of(saved.source) == local.source &&
                saved.synced == local.synced && saved.plain == local.plain
            ) {
                LyricsUpdate.Keep
            } else {
                LyricsUpdate.Save(local)
            }
        saved != null && LyricsSource.of(saved.source) != LyricsSource.Lrclib -> LyricsUpdate.Remove
        else -> LyricsUpdate.Keep
    }
}
