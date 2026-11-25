package app.attic.player.playback.eq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqCurveTest {
    @Test
    fun bandsAreTenOctaves() {
        assertEquals(EqBands.COUNT, EqBands.frequenciesHz.size)
        assertEquals(EqBands.COUNT, EqBands.labels.size)
        for (i in 1 until EqBands.COUNT) assertEquals(2.0, EqBands.frequenciesHz[i] / EqBands.frequenciesHz[i - 1], 0.0)
        assertEquals("1k", EqBands.labels[5])
    }

    @Test
    fun equalityIsByValue() {
        val a = EqCurve.of(0f, 1.5f, -3f, 0f, 0f, 0f, 0f, 0f, 0f, 6f)
        val b = EqCurve(listOf(0f, 1.5f, -3f, 0f, 0f, 0f, 0f, 0f, 0f, 6f))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.with(0, 0.5f))
        assertEquals(EqCurve.FLAT, EqCurve.of(-0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    }

    @Test
    fun flatness() {
        assertTrue(EqCurve.FLAT.isFlat)
        assertFalse(EqCurve.FLAT.with(9, 0.5f).isFlat)
    }

    @Test
    fun encodesCompactlyAndRoundTrips() {
        val c = EqCurve.of(0f, 1.5f, -3f, 0f, 0f, 0f, 0f, 0f, 2.25f, 12f)
        assertEquals("0,1.5,-3,0,0,0,0,0,2.25,12", c.encode())
        assertEquals(c, EqCurve.decode(c.encode()))
        assertEquals(EqCurve.FLAT, EqCurve.decode(EqCurve.FLAT.encode()))
        assertEquals(EqCurve.of(-0.5f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), EqCurve.decode(" -0.5 ,0,0,0,0,0,0,0,0,0"))
    }

    @Test
    fun badTextDecodesToNull() {
        assertNull(EqCurve.decode(null))
        assertNull(EqCurve.decode(""))
        assertNull(EqCurve.decode("0,0,0"))
        assertNull(EqCurve.decode("0,0,0,0,0,0,0,0,0,0,0"))
        assertNull(EqCurve.decode("0,0,0,0,x,0,0,0,0,0"))
        assertNull(EqCurve.decode("0,0,0,0,NaN,0,0,0,0,0"))
        assertNull(EqCurve.decode("0,0,0,0,Infinity,0,0,0,0,0"))
        assertNull(EqCurve.decode("0,0,0,0,,0,0,0,0,0"))
    }

    @Test
    fun outOfRangeIsClamped() {
        assertEquals(EqCurve.of(12f, -12f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), EqCurve.decode("40,-99,0,0,0,0,0,0,0,0"))
        assertEquals(EqCurve.of(0f, 12f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), EqCurve.of(Float.NaN, 13f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f).coerced())
        val inRange = EqCurve.of(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        assertTrue(inRange.coerced() === inRange)
    }
}
