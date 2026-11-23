package app.attic.player.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LicenceTextTest {
    @Test
    fun rejoinsHardWrappedProse() {
        val text = "Permission is hereby granted, free of charge,\nto any person obtaining a copy.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\".\n"
        assertEquals(
            listOf("Permission is hereby granted, free of charge, to any person obtaining a copy.", "THE SOFTWARE IS PROVIDED \"AS IS\"."),
            licenceParagraphs(text),
        )
    }

    @Test
    fun keepsIndentedAndListedBlocksAsLaidOut() {
        val text = "Modified with fontTools:\n    python3 -m fontTools.subset inst.ttf\n\n- one\n- two\r\n"
        assertEquals(
            listOf("Modified with fontTools:\n    python3 -m fontTools.subset inst.ttf", "- one\n- two"),
            licenceParagraphs(text),
        )
    }

    @Test
    fun keepsAnUnderlinedHeadingOnTwoLines() {
        assertEquals(listOf("Fonts\n====="), licenceParagraphs("Fonts\n=====\n"))
    }

    @Test
    fun collapsesRunsOfBlankLines() {
        assertEquals(listOf("A", "B"), licenceParagraphs("\n\nA\n\n\n\nB\n\n"))
    }

    @Test
    fun everyNoticeFileIsNamedOnce() {
        val ids = OpenSourceNotices.map { it.id }
        assertEquals(ids.distinct(), ids)
    }
}
