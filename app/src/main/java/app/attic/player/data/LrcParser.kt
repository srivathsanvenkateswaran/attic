package app.attic.player.data

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val stamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /** `[offset:+250]`: milliseconds to move every line earlier, or later when negative. */
    private val offsetTag = Regex("""^\s*\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

    /** Word timings from enhanced LRC, `<00:12.34>`, which the app doesn't show. */
    private val wordStamp = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    /**
     * Reads "[00:27.68] words" lines, including several stamps on one line. Blank lines are
     * instrumental gaps. ID tags such as `[ar:…]` and `[length:…]` are skipped, and `[offset:…]`
     * shifts every line.
     */
    fun parse(lrc: String): List<LyricLine> {
        val offsetMs = lrc.lineSequence().firstNotNullOfOrNull { offsetTag.find(it) }?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return lrc.lineSequence().flatMap { raw ->
            val stamps = leadingStamps(raw)
            val text = raw.substring(stamps.lastOrNull()?.range?.last?.plus(1) ?: 0).replace(wordStamp, "").trim()
            stamps.map { m ->
                val (min, sec, frac) = m.destructured
                val fracMs = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                val at = min.toLong() * 60_000 + sec.toLong() * 1000 + fracMs - offsetMs
                LyricLine(at.coerceAtLeast(0), text)
            }.asSequence()
        }.sortedBy { it.timeMs }.toList()
    }

    /** An ID tag on its own line: `[ar:Someone]`, `[length: 03:20]`, `[#comment]`. */
    private val idTag = Regex("""^\s*\[(?:[a-zA-Z]+:[^\]]*|#[^\]]*)]\s*$""")

    /** Untimed LRC text without its ID tags, or null when nothing else is left. */
    fun plainText(text: String): String? =
        text.lineSequence().filterNot { idTag.matches(it) }.joinToString("\n").trim().takeIf { it.isNotEmpty() }

    /** True when [text] has at least one timed line, so it should be shown as synced lyrics. */
    fun isSynced(text: String): Boolean = text.lineSequence().any { leadingStamps(it).isNotEmpty() }

    /** The stamps at the start of a line, before its words. A stamp after the words doesn't time the line. */
    private fun leadingStamps(line: String): List<MatchResult> {
        val found = ArrayList<MatchResult>(1)
        var at = 0
        while (true) {
            while (at < line.length && line[at] == ' ') at++
            val m = stamp.matchAt(line, at) ?: break
            found += m
            at = m.range.last + 1
        }
        return found
    }

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
