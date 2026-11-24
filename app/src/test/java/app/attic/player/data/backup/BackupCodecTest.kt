package app.attic.player.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val full = Backup(
        createdAt = 1_795_000_000_000,
        appVersion = "1.0.0",
        settings = BackupSettings(
            theme = "Light",
            uiStyle = "Comfort",
            autoplay = false,
            equaliserOn = true,
            equaliserDefault = "6,4,0,0,-2,0,-1,-5,5,1",
        ),
        likes = listOf(BackupLike("Music/Hindi/Kabira.mp3", 1_790_000_000_000)),
        playlists = listOf(
            BackupPlaylist("Late drive", 1_780_000_000_000, listOf("Music/Hindi/Kabira.mp3", "Music/English/Viva La Vida.mp3")),
            BackupPlaylist("Empty", 1_781_000_000_000, emptyList()),
        ),
        plays = listOf(BackupPlay("Music/Hindi/Kabira.mp3", 1_794_000_000_000, 214)),
        listens = listOf(
            BackupListen("Music/Hindi/Kabira.mp3", 1_794_000_000_000, 180_000, 240_000, "finished", "Late drive", false),
            BackupListen("Music/English/Viva La Vida.mp3", 1_794_000_300_000, 9_000, 242_000, "skipped", null, true),
        ),
        lyrics = listOf(
            BackupLyrics("Music/Hindi/Kabira.mp3", "[00:01.00] Pehli line \"quoted\"\n[00:05.00] दूसरी", null, false, 1_793_000_000_000),
            BackupLyrics("Music/Phonk/GHOST!.mp3", null, null, true, 1_793_000_000_001),
        ),
        languages = listOf(BackupLanguage("Music/Hindi/Kabira.mp3", "hi", "manual")),
        queue = BackupQueue(
            songs = listOf("Music/Hindi/Kabira.mp3", "Music/English/Viva La Vida.mp3"),
            index = 1,
            positionMs = 107_000,
            repeatMode = 2,
            shuffle = true,
            baseOrder = listOf("Music/English/Viva La Vida.mp3", "Music/Hindi/Kabira.mp3"),
            context = "Late drive",
            radioSeed = null,
        ),
        equaliserSongs = listOf(BackupEqualiser("Music/Hindi/Kabira.mp3", "-1.5,-3,-3,1.5,3.75,3.75,3,1.5,0,-1.5", 1_794_100_000_000)),
        equaliserAlbums = listOf(BackupEqualiser("monsoon tapes|kavya", "5.5,4.25,3.5,2.5,1.25,0,0,0,0,0", 1_794_200_000_000)),
        equaliserPresets = listOf(BackupEqualiserPreset("Car \"loud\"", "3,3,0,0,0,0,0,0,2,2", 1_794_300_000_000)),
    )

    @Test
    fun roundTripKeepsEverything() {
        assertEquals(full, BackupCodec.decode(BackupCodec.encode(full)))
    }

    @Test
    fun gzippedCopyReadsTheSame() {
        assertEquals(full, BackupCodec.decode(BackupCodec.gzip(BackupCodec.encode(full))))
    }

    @Test
    fun plainBytesWithByteOrderMarkRead() {
        val bytes = ("\uFEFF" + BackupCodec.encode(full)).toByteArray(Charsets.UTF_8)
        assertEquals(full, BackupCodec.decode(bytes))
    }

    @Test
    fun emptyBackupRoundTrips() {
        val empty = Backup(createdAt = 1, appVersion = null, settings = null)
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test
    fun fileStartsWithFormatAndVersion() {
        val text = BackupCodec.encode(full)
        assertTrue(text.contains("\"format\":\"attic-backup\""))
        assertTrue(text.contains("\"schemaVersion\":2"))
    }

    @Test
    fun missingSectionsAndUnknownFieldsAreFine() {
        val backup = BackupCodec.decode(
            """{"format":"attic-backup","schemaVersion":1,"createdAt":5,"addedLater":{"x":1},
               "likes":[{"song":"a.mp3","likedAt":2,"rating":5}]}""",
        )
        assertEquals(listOf(BackupLike("a.mp3", 2)), backup.likes)
        assertTrue(backup.playlists.isEmpty())
        assertNull(backup.queue)
        assertNull(backup.settings)
    }

    @Test
    fun versionOneBackupStillReadsWithNoEqualiser() {
        val backup = BackupCodec.decode(
            """{"format":"attic-backup","schemaVersion":1,"createdAt":5,"settings":{"theme":"Dark","autoplay":true},
               "likes":[{"song":"a.mp3","likedAt":2}]}""",
        )
        assertEquals(listOf(BackupLike("a.mp3", 2)), backup.likes)
        assertEquals(BackupSettings(theme = "Dark", uiStyle = null, autoplay = true), backup.settings)
        assertTrue(backup.equaliserSongs.isEmpty())
        assertTrue(backup.equaliserAlbums.isEmpty())
        assertTrue(backup.equaliserPresets.isEmpty())
    }

    @Test
    fun equaliserSongsCountAmongTheBackupsSongs() {
        assertTrue("Music/Hindi/Kabira.mp3" in full.songs())
        assertTrue("monsoon tapes|kavya" !in full.songs())
    }

    @Test
    fun newerSchemaIsRefused() {
        assertReason(BackupReadException.Reason.TooNew, """{"format":"attic-backup","schemaVersion":3,"createdAt":5}""")
    }

    @Test
    fun badSchemaNumberIsDamaged() {
        assertReason(BackupReadException.Reason.Damaged, """{"format":"attic-backup","schemaVersion":0,"createdAt":5}""")
        assertReason(BackupReadException.Reason.Damaged, """{"format":"attic-backup","createdAt":5}""")
    }

    @Test
    fun otherJsonIsNotABackup() {
        assertReason(BackupReadException.Reason.NotABackup, """{"playlists":[]}""")
        assertReason(BackupReadException.Reason.NotABackup, """[1,2,3]""")
        assertReason(BackupReadException.Reason.NotABackup, "ID3\u0003 not json at all")
        assertReason(BackupReadException.Reason.NotABackup, "")
    }

    @Test
    fun truncatedFileIsDamaged() {
        val text = BackupCodec.encode(full)
        assertReason(BackupReadException.Reason.Damaged, text.substring(0, text.length / 2))
    }

    @Test
    fun wrongTypesAreDamaged() {
        assertReason(
            BackupReadException.Reason.Damaged,
            """{"format":"attic-backup","schemaVersion":1,"createdAt":5,"likes":[{"song":7,"likedAt":2}]}""",
        )
        assertReason(
            BackupReadException.Reason.Damaged,
            """{"format":"attic-backup","schemaVersion":1,"createdAt":5,"likes":{"song":"a"}}""",
        )
        assertReason(
            BackupReadException.Reason.Damaged,
            """{"format":"attic-backup","schemaVersion":1,"createdAt":"yesterday"}""",
        )
    }

    @Test
    fun brokenGzipIsDamaged() {
        val zipped = BackupCodec.gzip(BackupCodec.encode(full))
        try {
            BackupCodec.decode(zipped.copyOf(zipped.size / 2))
            fail("Expected a read error")
        } catch (e: BackupReadException) {
            assertEquals(BackupReadException.Reason.Damaged, e.reason)
        }
    }

    @Test
    fun oversizedFileIsRefused() {
        try {
            BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES + 1))
            fail("Expected a read error")
        } catch (e: BackupReadException) {
            assertEquals(BackupReadException.Reason.TooLarge, e.reason)
        }
    }

    private fun assertReason(reason: BackupReadException.Reason, text: String) {
        try {
            BackupCodec.decode(text)
            fail("Expected $reason for $text")
        } catch (e: BackupReadException) {
            assertEquals(text, reason, e.reason)
        }
    }
}
