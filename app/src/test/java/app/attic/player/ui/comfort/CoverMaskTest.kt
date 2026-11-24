package app.attic.player.ui.comfort

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A baked cover lands where Coil would have drawn the picture under a clip. */
class CoverMaskTest {

    @Test
    fun squareSourceScalesToTheBox() {
        // A 48dp row cover at 420dpi is 126px; MediaStore thumbnails come back larger.
        assertEquals(IntRect(0, 0, 126, 126), SuperellipseMask.cropRect(160, 160, 126, 126))
        assertEquals(IntRect(0, 0, 126, 126), SuperellipseMask.cropRect(126, 126, 126, 126))
    }

    @Test
    fun wideSourceIsCentredAndCutAtTheSides() {
        // 1280x720 into 126: scale 0.175, 224x126, offset (126 - 224) / 2 = -49.
        assertEquals(IntRect(-49, 0, 175, 126), SuperellipseMask.cropRect(1280, 720, 126, 126))
    }

    @Test
    fun tallSourceIsCentredAndCutTopAndBottom() {
        // 300x301 into 147: scale 0.49, 147x147.49 -> 147x147, no offset.
        assertEquals(IntRect(0, 0, 147, 147), SuperellipseMask.cropRect(300, 301, 147, 147))
        // 100x151 into 73x74: scale 0.73, 73x110.23 -> 73x110, offset (74 - 110) / 2 = -18.
        assertEquals(IntRect(0, -18, 73, 92), SuperellipseMask.cropRect(100, 151, 73, 74))
    }

    @Test
    fun halfPixelOffsetsRoundAsCompose() {
        // 127x126 into 126: scale 1, offset (126 - 127) / 2 = -0.5, which Compose rounds up to 0.
        assertEquals(IntRect(0, 0, 127, 126), SuperellipseMask.cropRect(127, 126, 126, 126))
    }

    @Test
    fun onlyTheSuperellipsesBake() {
        assertTrue(SuperellipseMask.canBake(ArtShape))
        assertTrue(SuperellipseMask.canBake(ComfortShapes.SquircleSoft))
        assertFalse(SuperellipseMask.canBake(ComfortShapes.Group))
    }

    @Test
    fun cacheKeysTellMasksApart() {
        val density = 2.625f
        val art = SuperellipseMask(ArtShape, density)
        assertEquals(art.cacheKey, SuperellipseMask(ArtShape, density).cacheKey)
        assertNotEquals(art.cacheKey, SuperellipseMask(ComfortShapes.SquircleSoft, density).cacheKey)
        assertNotEquals(art.cacheKey, SuperellipseMask(ArtShape, 3f).cacheKey)
        assertNotEquals(
            SuperellipseMask(ArtShape, density, MosaicTile(147, 147, 0, 0)).cacheKey,
            SuperellipseMask(ArtShape, density, MosaicTile(147, 147, 1, 0)).cacheKey,
        )
    }

    @Test
    fun coverageMatchesTheGpuClip() {
        // Home's Favourites box on the test phone: 210px, `.sq`. In a screenshot of the clipped box,
        // 428 edge pixels are partly covered, each by a quarter step.
        val alpha = superellipseCoverage(ComfortShapes.Squircle, 210, 210)
        val values = alpha.map { it.toInt() and 0xFF }
        assertEquals(setOf(0, 64, 128, 191, 255), values.toSet())
        assertEquals(428, values.count { it in 1..254 })
        // Full at the centre, empty in the corner, full at the middle of each edge.
        assertEquals(255, values[105 * 210 + 105])
        assertEquals(0, values[0])
        assertEquals(255, values[105 * 210 + 0])
    }
}
