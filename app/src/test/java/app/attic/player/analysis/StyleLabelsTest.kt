package app.attic.player.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Reads the label list exactly as the app bundles it. */
class StyleLabelsTest {
    private val labels = StyleLabels.parse(File("src/main/assets/${StyleLabels.ASSET}").readText())

    @Test
    fun hasOneLabelPerModelOutput() {
        assertEquals(SongAnalyzer.STYLE_COUNT, labels.labels.size)
    }

    @Test
    fun splitsGenreFromStyle() {
        assertEquals("Blues---Boogie Woogie", labels.labels[0])
        assertEquals("Blues", labels.genre(0))
        assertEquals("Boogie Woogie", labels.style(0))
        val bollywood = labels.labels.indexOf("Pop---Bollywood")
        assertEquals("Pop", labels.genre(bollywood))
        assertEquals("Bollywood", labels.style(bollywood))
    }

    @Test
    fun keepsCommasAndSlashesInsideGenreNames() {
        val genres = labels.labels.indices.map(labels::genre).toSet()
        assertTrue("Folk, World, & Country" in genres)
        assertTrue("Funk / Soul" in genres)
        assertEquals(15, genres.size)
    }

    @Test
    fun everyLabelHasGenreAndStyle() {
        labels.labels.forEach { assertTrue(it, it.contains(StyleLabels.SEPARATOR)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAListOfTheWrongLength() {
        StyleLabels.parse("""{"classes": ["Pop---Ballad"]}""")
    }
}
