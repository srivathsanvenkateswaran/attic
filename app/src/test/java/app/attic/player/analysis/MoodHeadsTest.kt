package app.attic.player.analysis

import ai.onnxruntime.OrtEnvironment
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The bundled heads on the JVM, against outputs Python's onnxruntime gave for the same vector. */
class MoodHeadsTest {
    private val vector: FloatArray = FloatArray(SongAnalyzer.EMBEDDING_SIZE) { i -> (sin(i * 0.37) + 0.3 * cos(i * 0.011)).toFloat() }.let { v ->
        val n = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
        FloatArray(v.size) { v[it] / n }
    }

    @Test
    fun withTheLengthEveryHeadMatchesTheReference() {
        val out = heads.score(listOf(vector), listOf(4f)).single()
        val expected = mapOf(
            MoodSignals.HAPPY to 0.54449, MoodSignals.SAD to 0.51050, MoodSignals.RELAXED to 0.70224, MoodSignals.AGGRESSIVE to 0.45612,
            MoodSignals.PARTY to 0.59448, MoodSignals.DANCEABLE to 0.47532, MoodSignals.INSTRUMENTAL to 0.89908, MoodSignals.ENGAGEMENT to 0.29397,
            MoodSignals.tag("romantic") to 0.41280,
        )
        for ((signal, value) in expected) assertEquals("signal $signal", value.toFloat(), out[signal], 1e-4f)
        assertTrue(out.none { it.isNaN() })
        assertTrue(out.all { it in 0f..1f })
    }

    @Test
    fun withoutTheLengthOnlyTheSingleHeadsRunAtTheFixedScale() {
        for (unknown in listOf<Float?>(null, 0f)) {
            val out = heads.score(listOf(vector), listOf(unknown)).single()
            assertEquals(0.53222f, out[MoodSignals.HAPPY], 1e-4f)
            assertEquals(0.84190f, out[MoodSignals.INSTRUMENTAL], 1e-4f)
            assertTrue(out[MoodSignals.ENGAGEMENT].isNaN())
            for (i in MoodSignals.THEME_START until MoodSignals.SIZE) assertTrue(out[i].isNaN())
        }
    }

    @Test
    fun aBatchGivesWhatSingleRunsGive() {
        val other = FloatArray(vector.size) { vector[(it * 7) % vector.size] }
        val batch = heads.score(listOf(vector, other, vector), listOf(4f, null, 2.5f))
        val singles = listOf(heads.score(listOf(vector), listOf(4f)), heads.score(listOf(other), listOf(null)), heads.score(listOf(vector), listOf(2.5f))).map { it.single() }
        for (i in batch.indices) assertArrayEquals(singles[i], batch[i], 1e-5f)
        assertTrue(heads.score(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun theKeptColumnsAreTheClassesTheMetadataNames() {
        for (head in MoodHeads.HEADS) {
            val name = head.file.substringBefore("-discogs")
            val classes = JSONObject(File("src/main/assets/${MoodHeads.DIR}/${head.file.removeSuffix(".onnx")}.json").readText()).getJSONArray("classes")
            if (name == "mtg_jamendo_moodtheme") {
                assertEquals(MoodSignals.THEME_TAGS, List(classes.length()) { classes.getString(it) })
            } else {
                assertEquals(name, MoodHeads.KEPT_CLASSES.getValue(name), classes.getString(head.outputs.single().first))
            }
        }
    }

    @Test
    fun signalsRoundTripWithMissingValues() {
        val signals = FloatArray(MoodSignals.SIZE) { if (it % 5 == 0) Float.NaN else it / MoodSignals.SIZE.toFloat() }
        val bytes = MoodSignals.encode(signals)
        assertEquals(MoodSignals.SIZE * 2, bytes.size)
        val back = MoodSignals.decode(bytes)
        for (i in signals.indices) {
            if (signals[i].isNaN()) assertTrue(back[i].isNaN()) else assertEquals(signals[i], back[i], 1f / 65_534)
        }
        // A short blob leaves the rest missing.
        val short = MoodSignals.decode(bytes.copyOf(4))
        assertEquals(signals[1], short[1], 1f / 65_534)
        assertTrue(short[2].isNaN())
    }

    companion object {
        private lateinit var heads: MoodHeads

        @JvmStatic
        @BeforeClass
        fun load() {
            heads = MoodHeads(OrtEnvironment.getEnvironment()) { path -> File("src/main/assets/$path").readBytes() }
        }

        @JvmStatic
        @AfterClass
        fun close() = heads.close()
    }
}
