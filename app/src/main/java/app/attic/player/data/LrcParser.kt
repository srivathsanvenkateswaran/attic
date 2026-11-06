package app.attic.player.data

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val stamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /** Reads "[00:27.68] words" lines, including several stamps on one line. Blank lines are instrumental gaps. */
    fun parse(lrc: String): List<LyricLine> =
        lrc.lineSequence().flatMap { raw ->
            val stamps = stamp.findAll(raw).toList()
            val text = raw.substring(stamps.lastOrNull()?.range?.last?.plus(1) ?: 0).trim()
            stamps.map { m ->
                val (min, sec, frac) = m.destructured
                val fracMs = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                LyricLine(min.toLong() * 60_000 + sec.toLong() * 1000 + fracMs, text)
            }.asSequence()
        }.sortedBy { it.timeMs }.toList()

    /** The line being sung at [positionMs], or -1 before the first line. */
    fun lineAt(lines: List<LyricLine>, positionMs: Long): Int {
        var low = 0
        var high = lines.size - 1
        var found = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }
}

/** One candidate from the lyrics service. */
data class LyricsCandidate(
    val trackName: String,
    val artistName: String,
    val durationSec: Double,
    val synced: String?,
    val plain: String?,
    val instrumental: Boolean,
)

object LyricsMatcher {
    private const val MAX_DURATION_GAP_SEC = 12.0

    /**
     * Search results can be other songs with similar words, so length has to agree. YouTube rips run
     * a little long, which is why the gap isn't tighter. Synced lyrics beat plain, then closest length.
     */
    fun best(candidates: List<LyricsCandidate>, durationMs: Long): LyricsCandidate? {
        val seconds = durationMs / 1000.0
        return candidates
            .filter { !it.synced.isNullOrBlank() || !it.plain.isNullOrBlank() || it.instrumental }
            .filter { kotlin.math.abs(it.durationSec - seconds) <= MAX_DURATION_GAP_SEC }
            .sortedWith(
                compareBy<LyricsCandidate> { it.synced.isNullOrBlank() }
                    .thenBy { kotlin.math.abs(it.durationSec - seconds) },
            )
            .firstOrNull()
    }
}
