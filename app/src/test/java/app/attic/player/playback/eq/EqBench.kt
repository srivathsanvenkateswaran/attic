package app.attic.player.playback.eq

import java.io.File

/** Prints measurement lines with a fixed prefix (grep `ATTIC-EQ` in the test report) and keeps them in /tmp/attic-eq/bench.txt. */
internal object EqBench {
    const val PREFIX = "ATTIC-EQ"
    private val file = File("/tmp/attic-eq/bench.txt")

    @Synchronized
    fun report(section: String, lines: List<String>) {
        val text = lines.joinToString("\n") { "$PREFIX $section | $it" }
        println(text)
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText("$text\n")
        }
    }
}
