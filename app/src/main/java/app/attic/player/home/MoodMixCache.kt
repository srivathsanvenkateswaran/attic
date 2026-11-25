package app.attic.player.home

/**
 * The day's mood mixes as saved between launches. [inputs] fingerprints what they were built from
 * apart from the listening log: the picks, the scoring version and how many songs had scores. New
 * picks, or a backfill that scores more songs, build them again the same day; listening doesn't.
 */
data class MoodMixCache(val day: Long, val inputs: String, val mixes: List<MoodMixes.Mix>) {
    fun isCurrent(today: Long, inputsNow: String): Boolean = day == today && inputs == inputsNow

    fun encode(): String = buildString {
        append(VERSION).append(FIELD).append(day).append(FIELD).append(inputs)
        for (mix in mixes) {
            append('\n').append(mix.mood.id).append(FIELD).append(mix.keys.joinToString(ITEM.toString()))
        }
    }

    companion object {
        private const val VERSION = "1"
        private const val FIELD = '\u001E'
        private const val ITEM = '\u001F'

        /** The fingerprint [isCurrent] compares. Order within the picks matters only for moods, as Home shows them in that order. */
        fun inputs(picks: MoodMixes.Picks, scoringVersion: Int, scoredSongs: Int): String =
            listOf(
                scoringVersion.toString(),
                scoredSongs.toString(),
                picks.moods.joinToString(",") { it.id },
                picks.genres.sorted().joinToString(","),
                picks.languages.sorted().joinToString(","),
            ).joinToString("|")

        /** Null for anything this version didn't write, so the mixes are built again. */
        fun decode(text: String): MoodMixCache? {
            val lines = text.split('\n')
            val head = lines.first().split(FIELD)
            if (head.size != 3 || head[0] != VERSION) return null
            val day = head[1].toLongOrNull() ?: return null
            val mixes = lines.drop(1).map { line ->
                val parts = line.split(FIELD)
                if (parts.size != 2) return null
                val mood = Mood.fromId(parts[0]) ?: return null
                MoodMixes.Mix(mood, if (parts[1].isEmpty()) emptyList() else parts[1].split(ITEM))
            }
            return MoodMixCache(day, head[2], mixes)
        }
    }
}
