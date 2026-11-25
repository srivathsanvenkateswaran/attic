package app.attic.player.ui.model

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import app.attic.player.ui.LocalUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One entry on the Open-source licences page: a part of Attic, the licence it comes under, and the files
 * in assets/licences/ that hold its notice and licence text. The build copies those from third_party/.
 */
@Immutable
class OpenSourceNotice(val id: String, val name: String, val licence: String, val files: List<String>)

/** Names of software and licences are proper nouns, so they stay as written rather than in strings.xml. */
val OpenSourceNotices = listOf(
    OpenSourceNotice("attic", "Attic", "GNU General Public License v3.0", listOf("LICENSE-attic.txt")),
    OpenSourceNotice(
        "discogs-effnet",
        "Discogs-EffNet model and mood classifiers",
        "Creative Commons BY-NC-SA 4.0",
        listOf("discogs-effnet-NOTICE.txt"),
    ),
    OpenSourceNotice("onnxruntime", "ONNX Runtime", "MIT License", listOf("onnxruntime-LICENSE.txt", "onnxruntime-ThirdPartyNotices.txt")),
    OpenSourceNotice("google-sans-flex", "Google Sans Flex", "SIL Open Font License 1.1", listOf("OFL-google-sans-flex.txt", "fonts-NOTICE.txt")),
    OpenSourceNotice("onest", "Onest", "SIL Open Font License 1.1", listOf("OFL-onest.txt", "fonts-NOTICE.txt")),
    OpenSourceNotice("material-symbols", "Material Symbols", "Apache License 2.0", listOf("fonts-NOTICE.txt", "LICENSE-material-symbols.txt")),
    OpenSourceNotice("libraries", "Jetpack, Kotlin and other libraries", "Apache License 2.0", listOf("libraries-NOTICE.txt", "LICENSE-material-symbols.txt")),
)

class LicencesActions(val back: () -> Unit, val open: (OpenSourceNotice) -> Unit)

@Composable
fun rememberLicencesModel(): UiModel<List<OpenSourceNotice>, LicencesActions> {
    val ui = LocalUi.current
    return UiModel(OpenSourceNotices, LicencesActions(back = { ui.back() }, open = { ui.openLicence(it.id) }))
}

/** One notice's text, as paragraphs. [paragraphs] is null while the files load. */
@Immutable
class LicenceState(val notice: OpenSourceNotice, val paragraphs: List<String>?)

@Composable
fun rememberLicenceModel(id: String): UiModel<LicenceState, () -> Unit> {
    val context = LocalContext.current
    val ui = LocalUi.current
    val notice = OpenSourceNotices.firstOrNull { it.id == id } ?: OpenSourceNotices.first()
    val paragraphs by produceState<List<String>?>(null, notice) { value = loadParagraphs(context, notice) }
    return UiModel(LicenceState(notice, paragraphs)) { ui.back() }
}

private suspend fun loadParagraphs(context: Context, notice: OpenSourceNotice): List<String> = withContext(Dispatchers.IO) {
    notice.files.flatMap { file ->
        licenceParagraphs(context.assets.open("licences/$file").bufferedReader().use { it.readText() })
    }
}

/**
 * Splits a licence file at its blank lines. The files are wrapped at about 80 columns, which a phone
 * can't fit, so plain prose is joined back into one line per paragraph. A block with an indented or
 * bulleted line is kept as laid out, since its line breaks carry meaning.
 */
fun licenceParagraphs(text: String): List<String> =
    text.replace("\r\n", "\n")
        .split(Regex("\n\\s*\n"))
        .map { block -> block.trim('\n').lines().map(String::trimEnd) }
        .filter { lines -> lines.any(String::isNotBlank) }
        .map { lines ->
            val laidOut = lines.drop(1).any { it.startsWith(" ") || it.startsWith("\t") } ||
                lines.any { it.trimStart().startsWith("- ") || it.trimStart().startsWith("* ") || it.isRule() }
            if (laidOut) lines.joinToString("\n") else lines.joinToString(" ") { it.trim() }
        }

/** An underline or divider drawn with = - _ or *, which has to stay on a line of its own. */
private fun String.isRule(): Boolean = length >= 3 && all { it == '=' || it == '-' || it == '_' || it == '*' }
