package app.attic.player.analysis

/** How far song analysis has got, in the words Settings shows. */
data class AnalysisProgress(val done: Int, val total: Int, val running: Boolean) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total

    val canStart: Boolean get() = !running && done < total

    val status: String get() = when {
        running -> "Analysing songs: $done of $total"
        done >= total -> "All $total songs analysed."
        else -> "$done of $total songs analysed. The rest continue next time the phone charges."
    }
}
