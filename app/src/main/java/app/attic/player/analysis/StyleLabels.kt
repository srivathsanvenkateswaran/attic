package app.attic.player.analysis

import org.json.JSONObject

/**
 * The 400 Discogs styles the model scores, in the order of its `activations` output. They come from
 * Essentia's metadata file for the model, bundled unchanged as [ASSET]. Each label is "Genre---Style",
 * such as "Pop---Bollywood"; the part before the separator is one of Discogs' 15 top-level genres.
 */
class StyleLabels(val labels: List<String>) {
    init {
        require(labels.size == SongAnalyzer.STYLE_COUNT) { "Expected ${SongAnalyzer.STYLE_COUNT} labels, got ${labels.size}" }
    }

    /** "Pop" for "Pop---Bollywood". A label without the separator is its own genre. */
    fun genre(index: Int): String = labels[index].substringBefore(SEPARATOR)

    /** "Bollywood" for "Pop---Bollywood". */
    fun style(index: Int): String = labels[index].substringAfter(SEPARATOR)

    companion object {
        const val ASSET = "discogs-effnet-bsdynamic-1.json"
        const val SEPARATOR = "---"

        /** Reads Essentia's metadata JSON: the labels are its `classes` array. */
        fun parse(json: String): StyleLabels {
            val classes = JSONObject(json).getJSONArray("classes")
            return StyleLabels(List(classes.length()) { classes.getString(it) })
        }
    }
}
