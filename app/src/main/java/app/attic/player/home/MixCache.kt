package app.attic.player.home

/**
 * The day's taste mixes as saved between launches, so they are worked out once a day rather than on
 * every start. [seeds] is how many played or liked analysed songs they were built from; a day that
 * had too few is built again once more are analysed.
 */
data class MixCache(val day: Long, val seeds: Int, val mixes: List<TasteMixes.Mix>) {
    /** True when today's mixes still stand: same day, and not an empty answer that more analysis could change. */
    fun isCurrent(today: Long, seedsNow: Int): Boolean = day == today && (mixes.isNotEmpty() || seedsNow == seeds)

    fun encode(): String = buildString {
        append(VERSION).append(FIELD).append(day).append(FIELD).append(seeds)
        for (mix in mixes) {
            append('\n')
            append(mix.artistKeys.joinToString(ITEM.toString()))
            append(FIELD)
            append(mix.keys.joinToString(ITEM.toString()))
        }
    }

    companion object {
        private const val VERSION = "1"

        /** Separators no file path or artist name contains. */
        private const val FIELD = '\u001E'
        private const val ITEM = '\u001F'

        /** Null for anything this version didn't write, so the mixes are simply built again. */
        fun decode(text: String): MixCache? {
            val lines = text.split('\n')
            val head = lines.first().split(FIELD)
            if (head.size != 3 || head[0] != VERSION) return null
            val day = head[1].toLongOrNull() ?: return null
            val seeds = head[2].toIntOrNull() ?: return null
            val mixes = lines.drop(1).map { line ->
                val parts = line.split(FIELD)
                if (parts.size != 2) return null
                TasteMixes.Mix(keys = parts[1].items(), artistKeys = parts[0].items())
            }
            return MixCache(day, seeds, mixes)
        }

        private fun String.items(): List<String> = if (isEmpty()) emptyList() else split(ITEM)
    }
}
