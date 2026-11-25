package app.attic.player.playback.eq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {
    @Test
    fun idsAreTheStoredOnes() {
        assertEquals(
            listOf("flat", "bass_boost", "bass_reducer", "treble_boost", "vocal", "loudness", "acoustic", "electronic", "hip_hop", "classical", "spoken_word"),
            EqPresets.all.map { it.id },
        )
    }

    @Test
    fun curvesAreInRangeAndOnTheSliderGrid() {
        for (p in EqPresets.all) for (g in p.curve.gainsDb) {
            assertTrue("${p.id} $g", g in EqBands.MIN_DB..EqBands.MAX_DB)
            assertEquals("${p.id} $g", 0f, (g / EqBands.STEP_DB) % 1f, 0f)
        }
        assertTrue(EqPresets.byId(EqPresets.FLAT)!!.curve.isFlat)
        assertEquals(EqCurve.of(5.5f, 4.5f, 3.5f, 2.5f, 1.5f, 0f, 0f, 0f, 0f, 0f), EqPresets.byId(EqPresets.BASS_BOOST)!!.curve)
    }

    @Test
    fun builtInsAreDistinct() {
        assertEquals(EqPresets.all.size, EqPresets.all.map { it.curve }.toSet().size)
    }

    @Test
    fun matchFindsBuiltInsFirstThenUserPresetsThenNothing() {
        val bass = EqPresets.byId(EqPresets.BASS_BOOST)!!
        val nearly = EqCurve.of(*FloatArray(EqBands.COUNT) { bass.curve[it] + 0.04f })
        assertSame(bass, EqPresets.match(nearly))
        val mine = EqPreset("user:1", EqCurve.of(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        val shadow = EqPreset("user:2", bass.curve)
        assertSame(bass, EqPresets.match(bass.curve, listOf(shadow)))
        assertSame(mine, EqPresets.match(EqCurve.of(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), listOf(mine)))
        assertNull(EqPresets.match(EqCurve.of(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.5f), listOf(mine)))
        assertNull(EqPresets.match(bass.curve.with(0, bass.curve[0] + 0.1f)))
    }
}
