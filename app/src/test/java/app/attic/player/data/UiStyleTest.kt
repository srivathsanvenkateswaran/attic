package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UiStyleTest {
    @Test
    fun samsungPhonesDefaultToComfort() {
        assertEquals(UiStyle.Comfort, UiStyle.defaultFor("samsung"))
        assertEquals(UiStyle.Comfort, UiStyle.defaultFor("Samsung"))
        assertEquals(UiStyle.Comfort, UiStyle.defaultFor("SAMSUNG"))
    }

    @Test
    fun otherPhonesDefaultToExpressive() {
        assertEquals(UiStyle.Expressive, UiStyle.defaultFor("Google"))
        assertEquals(UiStyle.Expressive, UiStyle.defaultFor("OnePlus"))
        assertEquals(UiStyle.Expressive, UiStyle.defaultFor(""))
    }

    @Test
    fun aSavedChoiceWinsOverTheDefault() {
        assertEquals(UiStyle.Expressive, UiStyle.resolve("Expressive", "samsung"))
        assertEquals(UiStyle.Comfort, UiStyle.resolve("Comfort", "Google"))
    }

    @Test
    fun noOrUnknownSavedChoiceFallsBackToTheDefault() {
        assertEquals(UiStyle.Comfort, UiStyle.resolve(null, "samsung"))
        assertEquals(UiStyle.Expressive, UiStyle.resolve(null, "Google"))
        assertEquals(UiStyle.Expressive, UiStyle.resolve("Retro", "Google"))
        assertEquals(UiStyle.Comfort, UiStyle.resolve("Retro", "samsung"))
        assertEquals(UiStyle.Expressive, UiStyle.resolve("", "Google"))
    }

    @Test
    fun theRemovedClassicStyleFallsBackToTheDefault() {
        assertEquals(UiStyle.Expressive, UiStyle.resolve("Classic", "Google"))
        assertEquals(UiStyle.Comfort, UiStyle.resolve("Classic", "samsung"))
    }

    @Test
    fun everyStyleIsOffered() {
        assertEquals(listOf(UiStyle.Expressive, UiStyle.Comfort), UiStyle.choices)
        assertEquals(UiStyle.entries.toList(), UiStyle.choices)
    }
}
