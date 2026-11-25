package app.attic.player.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualiserBandsTest {
    @Test
    fun valuesCarryTheirSignAndATrueMinus() {
        assertEquals("+4", EqualiserBands.formatDb(4f))
        assertEquals("\u22122.5", EqualiserBands.formatDb(-2.5f))
        assertEquals("0", EqualiserBands.formatDb(0f))
        assertEquals("0", EqualiserBands.formatDb(-0.1f))
        assertEquals("+12", EqualiserBands.formatDb(12f))
    }

    @Test
    fun eachBandSitsInTheMiddleOfItsTenth() {
        assertEquals(0.05f, EqualiserBands.centre(0), 1e-6f)
        assertEquals(0.95f, EqualiserBands.centre(9), 1e-6f)
    }

    @Test
    fun theResponseSitsOnZeroWhenFlatAndPeaksAtABoostedBand() {
        val flat = EqualiserBands.response(FloatArray(EqualiserBands.COUNT))
        assertEquals(0f, flat.maxOf { kotlin.math.abs(it) }, 1e-3f)
        val gains = FloatArray(EqualiserBands.COUNT).also { it[5] = 6f }
        val response = EqualiserBands.response(gains, points = 100)
        // Point 55 is the middle of the 1 kHz band's tenth on the drawing.
        assertEquals(6f, response[55], 0.3f)
    }
}
