package app.attic.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/** Fixtures come from scripts/make_lyrics_fixtures.py. */
class LocalLyricsTest {
    companion object {
        /**
         * Media3's ParsableByteArray compares Build.FINGERPRINT with a string the first time it runs,
         * and the stub Android jar leaves it null. The field is static final, so only Unsafe can set it.
         */
        @JvmStatic
        @BeforeClass
        fun giveBuildAFingerprint() {
            val field = android.os.Build::class.java.getField("FINGERPRINT")
            if (field.get(null) != null) return
            val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
            val unsafe = unsafeField.get(null) as sun.misc.Unsafe
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), "jvm-unit-test")
        }
    }

    private fun fixture(name: String): File =
        File(requireNotNull(javaClass.classLoader?.getResource("lyrics/$name")) { "missing fixture $name" }.toURI())

    private fun embedded(name: String): EmbeddedLyrics =
        RandomAccessFile(fixture(name), "r").use { EmbeddedLyricsReader.from(TagReader.metadata(it.channel, name)) }

    // LRC parsing

    @Test
    fun `offset tag moves every line earlier, or later when negative`() {
        assertEquals(listOf(500L, 1500L), LrcParser.parse("[offset:+500]\n[00:01.00]a\n[00:02.00]b").map { it.timeMs })
        assertEquals(listOf(1250L), LrcParser.parse("[offset:-250]\n[00:01.00]a").map { it.timeMs })
        // A line can't start before the song does.
        assertEquals(listOf(0L), LrcParser.parse("[offset:5000]\n[00:01.00]a").map { it.timeMs })
    }

    @Test
    fun `several stamps on one line make one line each, in time order`() {
        val lines = LrcParser.parse("[00:30.00][00:10.00]Chorus\n[00:20.00]Verse")
        assertEquals(listOf(10_000L, 20_000L, 30_000L), lines.map { it.timeMs })
        assertEquals(listOf("Chorus", "Verse", "Chorus"), lines.map { it.text })
    }

    @Test
    fun `ID tags and word timings are left out of the text`() {
        val lrc = "[ti:Song]\n[ar:Someone]\n[al:Album]\n[by:me]\n[length: 03:20]\n[re:tool]\n[00:01.50]<00:01.50>Hello <00:02.00>there"
        val lines = LrcParser.parse(lrc)
        assertEquals(listOf(LyricLine(1500, "Hello there")), lines)
    }

    @Test
    fun `text counts as synced only with a leading stamp`() {
        assertTrue(LrcParser.isSynced("[ar:x]\n[00:01.00]words"))
        assertFalse(LrcParser.isSynced("[Chorus]\nwords"))
        assertFalse(LrcParser.isSynced("[ar:Someone]\n[ti:Song]"))
        assertFalse(LrcParser.isSynced("words at [00:01.00]"))
    }

    // ID3 extraction

    @Test
    fun `reads plain UTF-16 USLT from ID3v2_3`() {
        assertEquals(EmbeddedLyrics(null, "Plain first\nPlain second — ünïcode"), embedded("uslt_plain_v23.mp3"))
    }

    @Test
    fun `LRC inside USLT counts as synced`() {
        val lyrics = embedded("uslt_lrc_v24.mp3")
        assertNull(lyrics.plain)
        assertEquals(listOf(LyricLine(1000, "First line"), LyricLine(2500, "Second line")), LrcParser.parse(lyrics.synced!!))
    }

    @Test
    fun `SYLT becomes LRC and beats USLT, which stays as the plain text`() {
        val lyrics = embedded("sylt_v23.mp3")
        assertEquals("[00:01.000]Hello\n[00:02.500]world again", lyrics.synced)
        assertEquals("Plain words", lyrics.plain)
    }

    @Test
    fun `reads ffmpeg's TXXX lyrics frame`() {
        assertEquals(EmbeddedLyrics(null, "Txxx first\nTxxx second"), embedded("txxx.mp3"))
    }

    @Test
    fun `a file without lyrics has none`() {
        assertEquals(EmbeddedLyrics.None, embedded("no_lyrics.mp3"))
    }

    @Test
    fun `SYLT entries without line breaks are a line each, and frame timestamps are refused`() {
        val body = byteArrayOf(0, 'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 2, 1, 0) +
            "One".toByteArray() + byteArrayOf(0, 0, 0, 0, 100) + "Two".toByteArray() + byteArrayOf(0, 0, 0, 0x03, 0xE8.toByte())
        assertEquals("[00:00.100]One\n[00:01.000]Two", Id3LyricsFrames.sylt(body))
        val frames = body.copyOf().also { it[4] = 1 }
        assertNull(Id3LyricsFrames.sylt(frames))
    }

    // MP4 extraction

    @Test
    fun `reads synced lyrics from an M4A whose moov comes after the audio`() {
        val lyrics = embedded("lyr_moov_last.m4a")
        assertEquals(listOf(3200L, 4000L), LrcParser.parse(lyrics.synced!!).map { it.timeMs })
    }

    @Test
    fun `reads plain lyrics from a faststart M4A`() {
        assertEquals(EmbeddedLyrics(null, "Plain mp4 words"), embedded("lyr_faststart.m4a"))
    }

    // Vorbis comments

    @Test
    fun `reads the LYRICS comment from FLAC and Opus`() {
        assertEquals(listOf(LyricLine(500, "Flac line")), LrcParser.parse(embedded("lyrics.flac").synced!!))
        assertEquals(EmbeddedLyrics(null, "Opus words"), embedded("lyrics.opus"))
    }

    // Priority

    private val syncedSidecar = "[00:01.00]From the lrc file"
    private val syncedTag = EmbeddedLyrics("[00:05.00]From the tag", "Plain from the tag")
    private val plainTag = EmbeddedLyrics(null, "Plain from the tag")

    @Test
    fun `a synced lrc file beats everything in the tags`() {
        assertEquals(LocalLyrics(syncedSidecar, null, LyricsSource.Sidecar), LyricsPriority.local(syncedSidecar, syncedTag))
    }

    @Test
    fun `synced tags beat plain tags, which beat an untimed lrc file`() {
        assertEquals(LocalLyrics(syncedTag.synced, syncedTag.plain, LyricsSource.Embedded), LyricsPriority.local(null, syncedTag))
        assertEquals(LocalLyrics(syncedTag.synced, syncedTag.plain, LyricsSource.Embedded), LyricsPriority.local("just words", syncedTag))
        assertEquals(LocalLyrics(null, plainTag.plain, LyricsSource.Embedded), LyricsPriority.local("just words", plainTag))
        assertEquals(LocalLyrics(null, "just words", LyricsSource.Sidecar), LyricsPriority.local("\uFEFFjust words\r\n", EmbeddedLyrics.None))
        assertNull(LyricsPriority.local(" \n", EmbeddedLyrics.None))
        assertNull(LyricsPriority.local("[ar:Someone]", EmbeddedLyrics.None))
    }

    private fun saved(source: LyricsSource, synced: String? = null, plain: String? = "p", found: Boolean = true) =
        LyricsEntity("k", synced, plain, instrumental = false, found = found, fetchedAt = 0, source = source.id)

    @Test
    fun `local lyrics replace downloaded ones, and downloaded ones stay when there are none`() {
        val local = LocalLyrics(syncedSidecar, null, LyricsSource.Sidecar)
        assertEquals(LyricsUpdate.Save(local), LyricsPriority.update(local, saved(LyricsSource.Lrclib)))
        assertEquals(LyricsUpdate.Save(local), LyricsPriority.update(local, saved(LyricsSource.Lrclib, found = false)))
        assertEquals(LyricsUpdate.Save(local), LyricsPriority.update(local, null))
        assertEquals(LyricsUpdate.Keep, LyricsPriority.update(null, saved(LyricsSource.Lrclib)))
        assertEquals(LyricsUpdate.Keep, LyricsPriority.update(null, null))
    }

    @Test
    fun `lyrics from a file are removed when the file loses them, and kept when unchanged`() {
        assertEquals(LyricsUpdate.Remove, LyricsPriority.update(null, saved(LyricsSource.Sidecar)))
        assertEquals(LyricsUpdate.Remove, LyricsPriority.update(null, saved(LyricsSource.Embedded)))
        val same = LocalLyrics(null, "p", LyricsSource.Embedded)
        assertEquals(LyricsUpdate.Keep, LyricsPriority.update(same, saved(LyricsSource.Embedded)))
        val moved = LocalLyrics(syncedSidecar, null, LyricsSource.Sidecar)
        assertEquals(LyricsUpdate.Save(moved), LyricsPriority.update(moved, saved(LyricsSource.Embedded)))
    }
}
