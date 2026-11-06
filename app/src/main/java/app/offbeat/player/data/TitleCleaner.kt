package app.offbeat.player.data

import java.text.Normalizer

/** Text cleanup for tags and filenames. Display only: the files themselves are never rewritten. */
object TitleCleaner {
    private val youtubeId = Regex("""\s*\[[A-Za-z0-9_-]{11}]\s*$""")
    private val extension = Regex("""\.[A-Za-z0-9]{2,4}$""")
    private val leadingTrackNumber = Regex("""^(\d{1,3})[\s._-]+(?=\S)""")

    /** Bracketed upload noise. "(The Official 2010 FIFA World Cup Song)" names the song, so it stays. */
    private val bracketNoise = Regex(
        """\s*[(\[]\s*(?:(?:the\s+)?official\b[^)\]]*\b(?:video|audio|lyrics?|visuali[sz]er)\b[^)\]]*""" +
            """|lyrics?(?:\s+video)?|audio|video|explicit|hd|hq|4k)\s*[)\]]""",
        RegexOption.IGNORE_CASE,
    )
    private val trailingNoise = Regex(
        """(?:\s+-)?\s+(?:official\s+)?(?:music\s+)?(?:video\s+song|video|lyric|lyrics|hd|hq)\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val featuring = Regex("""\b(?:ft|feat)\.?\s+([^()\[\]]+)""", RegexOption.IGNORE_CASE)
    private val bracketed = Regex("""[(\[][^)\]]*[)\]]""")
    private val creditSplit = Regex("""\s*,\s*|\s+&\s+|\s+(?:feat\.?|ft\.?)\s+""", RegexOption.IGNORE_CASE)
    private val ripCreditSplit = Regex("""\s*,\s*|\s+&\s+|\s+x\s+""", RegexOption.IGNORE_CASE)

    /** Removes the trailing "[kTpyO8iwlSY]" that YouTube downloads carry. "[Remix]" is too short to match. */
    fun stripYoutubeId(text: String): String = text.replace(youtubeId, "").trim()

    /** Filename to title: drops the extension, the YouTube ID and a leading "05 " track number. */
    fun titleFromFilename(displayName: String): String {
        val stem = stripYoutubeId(displayName.replace(extension, ""))
        return stem.replace(leadingTrackNumber, "").ifBlank { stem }
    }

    fun trackNumberFromFilename(displayName: String): Int? =
        leadingTrackNumber.find(displayName)?.groupValues?.get(1)?.toIntOrNull()

    /** Folds case, accents and punctuation, so "A.R. RAHMAN" and "A. R. Rahman" compare equal. */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            for (ch in decomposed) {
                if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
                if (ch.isLetterOrDigit()) append(ch.lowercaseChar())
            }
        }
    }

    /** Folds case and accents like [normalize] but keeps word breaks, for substring search. */
    fun searchable(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            for (ch in decomposed) {
                if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
                append(if (ch.isLetterOrDigit()) ch.lowercaseChar() else ' ')
            }
        }.replace(Regex("""\s+"""), " ").trim()
    }

    fun splitCredits(artist: String, isRip: Boolean = false): List<String> =
        artist.split(if (isRip) ripCreditSplit else creditSplit)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** "Vishal-Shekhar, K.K., Vishal-Shekhar" reads as "Vishal-Shekhar, K.K.". Untouched if nothing repeats. */
    fun dedupeCredits(artist: String): String {
        val credits = splitCredits(artist)
        val distinct = credits.distinctBy(::normalize)
        return if (distinct.size == credits.size) artist else distinct.joinToString(", ")
    }

    /** "OneRepublic - I Ain't Worried" on a properly tagged file: the artist is already known. */
    fun stripArtistPrefix(title: String, artistNames: List<String>): String {
        val parts = title.split(" - ", limit = 2)
        if (parts.size != 2) return title
        val left = normalize(parts[0])
        return if (artistNames.any { normalize(it) == left }) parts[1].trim() else title
    }

    data class Rip(val title: String, val artist: String?, val credits: List<String>)

    /**
     * Files with no album tag are YouTube rips. Their artist tag is often the uploader's channel
     * ("DopeMusic") and the real artist is inside the title ("Coldplay - Hymn For The Weekend (Lyrics)").
     */
    fun parseRip(rawTitle: String, tagArtist: String?): Rip {
        val tag = tagArtist?.trim()?.takeIf { it.isNotEmpty() }
        val cleaned = rawTitle.replace('｜', '|').replace(bracketNoise, "").trim()
        val hasPipe = '|' in cleaned
        val segments = cleaned.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        var head = segments.firstOrNull() ?: cleaned
        if (head.endsWith(" Songs", ignoreCase = true) && segments.size > 1) head = segments[1]
        head = stripTrailingNoise(head)

        val dash = head.split(" - ", limit = 2)
        if (dash.size < 2) return Rip(head, tag, emptyList())

        val left = dash[0].trim()
        val right = stripTrailingNoise(dash[1].trim())
        val tagInLeft = tag != null && normalize(left).contains(normalize(tag))
        // A name in brackets is a remix credit ("Celestial (Steerner Remix)"), not the performer.
        val tagInRight = tag != null && normalize(right.replace(bracketed, "")).contains(normalize(tag))

        return when {
            // "Maryan - Sonapareeya Video | Dhanush": the left side is a film, not a performer.
            hasPipe && !tagInLeft -> Rip(right, tag, emptyList())
            // "Nada Nada - Jmilton": the uploader is named on the right, so the song is on the left.
            tagInRight && !tagInLeft -> Rip(left, tag, ripCredits(tag, left))
            // "BATIDAO FUNK (SUPER SLOWED) - ZXKAI x Rezcaze": artists don't carry parentheticals.
            '(' in left -> Rip(left, right, ripCredits(right, left))
            else -> Rip(right, left, ripCredits(left, right))
        }
    }

    private fun ripCredits(artist: String, title: String): List<String> {
        val featured = featuring.find(title)?.groupValues?.get(1)?.trim()
        return (splitCredits(artist, isRip = true) + featured?.let { splitCredits(it, isRip = true) }.orEmpty())
    }

    private fun stripTrailingNoise(text: String): String {
        var result = text.trim()
        while (true) {
            val next = result.replace(trailingNoise, "").trim()
            if (next == result || next.isEmpty()) return result
            result = next
        }
    }
}
