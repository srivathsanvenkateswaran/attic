package app.attic.player.data

/** Works out a song's language from what the library already says. Null means it can't tell. */
object LanguageLabels {
    /** Fits any session: an instrumental, or a genre where the words aren't the point. */
    const val NO_VOCALS = "none"

    /** Offered when correcting a song by hand, in the order shown. */
    val CHOICES = listOf(
        "ta" to "Tamil",
        "hi" to "Hindi",
        "te" to "Telugu",
        "ml" to "Malayalam",
        "kn" to "Kannada",
        "en" to "English",
        NO_VOCALS to "No vocals",
    )

    private val FOLDERS = mapOf("Tamil" to "ta", "Hindi" to "hi", "English" to "en", "Phonk" to NO_VOCALS)
    private val ALBUM_WORDS = Regex("""\((Tamil|Hindi|Telugu|Malayalam|Kannada)\)""", RegexOption.IGNORE_CASE)
    private val ALBUM_CODES = mapOf("tamil" to "ta", "hindi" to "hi", "telugu" to "te", "malayalam" to "ml", "kannada" to "kn")

    fun displayName(code: String?): String =
        CHOICES.firstOrNull { it.first == code }?.second ?: "Unknown"

    /** A stored label comes from your own correction or an imported file, so it beats any guess. */
    fun resolve(folder: String?, album: String?, stored: String?, lyrics: String?, instrumental: Boolean): String? {
        stored?.let { return it }
        FOLDERS[folder]?.let { return it }
        album?.let { name ->
            ALBUM_WORDS.find(name)?.let { return ALBUM_CODES[it.groupValues[1].lowercase()] }
        }
        if (instrumental) return NO_VOCALS
        return lyrics?.let(::fromScript)
    }

    /** Romanized lyrics stay unknown, because Tamil and Hindi look alike in Latin letters. */
    private fun fromScript(text: String): String? {
        val tamil = text.count { it in '஀'..'௿' }
        val hindi = text.count { it in 'ऀ'..'ॿ' }
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return when {
            tamil > latin && tamil >= hindi -> "ta"
            hindi > latin -> "hi"
            else -> null
        }
    }

    /** One song per line, `path<TAB>language`. Anything else is ignored. */
    fun parseImport(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.split('\t') }
            .filter { it.size == 2 && it[0].isNotBlank() && it[1].isNotBlank() }
            .associate { it[0] to it[1].trim() }
}
