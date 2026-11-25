package app.attic.player.data.backup

import app.attic.player.data.EQ_SCOPE_ALBUM
import app.attic.player.data.EQ_SCOPE_SONG
import app.attic.player.data.HistoryEntity
import app.attic.player.data.LikedEntity
import app.attic.player.data.PlaylistOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorePlannerTest {
    private val kabira = "Music/Offline/Hindi/Kabira.mp3"
    private val viva = "Music/Offline/English/Viva La Vida.mp3"
    private val gone = "Music/Offline/Tamil/Aaruyire.mp3"

    private fun backup(
        likes: List<BackupLike> = emptyList(),
        playlists: List<BackupPlaylist> = emptyList(),
        plays: List<BackupPlay> = emptyList(),
        listens: List<BackupListen> = emptyList(),
        lyrics: List<BackupLyrics> = emptyList(),
        languages: List<BackupLanguage> = emptyList(),
        queue: BackupQueue? = null,
    ) = Backup(1, "1.0.0", null, likes, playlists, plays, listens, lyrics, languages, queue)

    private fun listen(song: String, at: Long) = BackupListen(song, at, 60_000, 200_000, "finished", null, false)

    @Test
    fun intoAnEmptyPhoneEverythingIsAdded() {
        val plan = RestorePlanner.plan(
            backup(
                likes = listOf(BackupLike(kabira, 10), BackupLike(gone, 11)),
                playlists = listOf(BackupPlaylist("Late drive", 5, listOf(viva, kabira, gone))),
                listens = listOf(listen(kabira, 100), listen(gone, 200)),
            ),
            ExistingData(),
            setOf(kabira, viva),
        )
        assertEquals(listOf(LikedEntity(kabira, 10), LikedEntity(gone, 11)), plan.likes)
        assertEquals(listOf(viva, kabira, gone), plan.newPlaylists.single().keys)
        assertEquals("Late drive", plan.newPlaylists.single().entity.name)
        assertEquals(2, plan.listens.size)
        assertEquals(RestoreReport(2, 1, 0, 2, 0, songsMatched = 2, songsMissing = 1), plan.report)
    }

    @Test
    fun restoringTwiceAddsNothing() {
        val file = backup(
            likes = listOf(BackupLike(kabira, 10)),
            playlists = listOf(BackupPlaylist("Late drive", 5, listOf(viva, kabira))),
            plays = listOf(BackupPlay(kabira, 300, 4)),
            listens = listOf(listen(kabira, 100)),
            lyrics = listOf(BackupLyrics(kabira, null, "words", false, 9)),
            languages = listOf(BackupLanguage(kabira, "hi", "manual")),
        )
        val afterFirst = ExistingData(
            likedKeys = setOf(kabira),
            playlists = listOf(ExistingPlaylist(1, "Late drive", listOf(viva, kabira))),
            history = mapOf(kabira to HistoryEntity(kabira, 300, 4)),
            listenIds = setOf(RestorePlanner.listenId(kabira, 100)),
            lyricsFoundKeys = setOf(kabira),
            languageSources = mapOf(kabira to "manual"),
        )
        val plan = RestorePlanner.plan(file, afterFirst, setOf(kabira, viva))
        assertTrue(plan.likes.isEmpty())
        assertTrue(plan.newPlaylists.isEmpty())
        assertTrue(plan.playlistAdditions.isEmpty())
        assertTrue(plan.history.isEmpty())
        assertTrue(plan.listens.isEmpty())
        assertTrue(plan.lyrics.isEmpty())
        assertTrue(plan.languages.isEmpty())
    }

    @Test
    fun playlistWithSameNameGainsOnlyMissingSongs() {
        val plan = RestorePlanner.plan(
            backup(playlists = listOf(BackupPlaylist("  late DRIVE ", 5, listOf(viva, kabira, gone)))),
            ExistingData(playlists = listOf(ExistingPlaylist(7, "Late drive", listOf(kabira)))),
            setOf(kabira, viva),
        )
        assertTrue(plan.newPlaylists.isEmpty())
        assertEquals(mapOf(7L to listOf(viva, gone)), plan.playlistAdditions)
        assertEquals(1, plan.report.playlistsMerged)
    }

    @Test
    fun twoBackupPlaylistsWithOneNameBecomeOne() {
        val plan = RestorePlanner.plan(
            backup(playlists = listOf(BackupPlaylist("Mix", 1, listOf(kabira)), BackupPlaylist("mix", 2, listOf(viva, kabira)))),
            ExistingData(),
            setOf(kabira, viva),
        )
        assertEquals(listOf(kabira, viva), plan.newPlaylists.single().keys)
    }

    @Test
    fun historyKeepsLaterTimeAndLargerCount() {
        val plan = RestorePlanner.plan(
            backup(plays = listOf(BackupPlay(kabira, 100, 40), BackupPlay(viva, 900, 1))),
            ExistingData(history = mapOf(kabira to HistoryEntity(kabira, 500, 3), viva to HistoryEntity(viva, 50, 6))),
            setOf(kabira, viva),
        )
        assertEquals(listOf(HistoryEntity(kabira, 500, 40), HistoryEntity(viva, 900, 6)), plan.history)
    }

    @Test
    fun lyricsReplaceACheckedMissButNotFoundLyrics() {
        val plan = RestorePlanner.plan(
            backup(
                lyrics = listOf(
                    BackupLyrics(kabira, null, "backup words", false, 1),
                    BackupLyrics(viva, "[00:01.00] line", null, false, 2),
                ),
            ),
            ExistingData(lyricsFoundKeys = setOf(kabira)),
            setOf(kabira, viva),
        )
        assertEquals(listOf(viva), plan.lyrics.map { it.trackKey })
        assertTrue(plan.lyrics.single().found)
    }

    @Test
    fun manualLanguageBeatsImportedOnly() {
        val plan = RestorePlanner.plan(
            backup(languages = listOf(BackupLanguage(kabira, "hi", "manual"), BackupLanguage(viva, "en", "imported"))),
            ExistingData(languageSources = mapOf(kabira to "imported", viva to "manual")),
            setOf(kabira, viva),
        )
        assertEquals(listOf(kabira), plan.languages.map { it.trackKey })
    }

    @Test
    fun queueOnlyWhenNoneIsSaved() {
        val queue = BackupQueue(listOf(kabira, viva), 1, 5_000, 0, false, null, "Late drive", null)
        assertEquals("$kabira\n$viva", RestorePlanner.plan(backup(queue = queue), ExistingData(), setOf(kabira)).queue?.queueKeys)
        assertNull(RestorePlanner.plan(backup(queue = queue), ExistingData(hasSavedQueue = true), setOf(kabira)).queue)
    }

    @Test
    fun songInAnotherFolderMatchesByUniqueFileName() {
        val here = "Music/Hindi/Kabira.mp3"
        val plan = RestorePlanner.plan(
            backup(likes = listOf(BackupLike(kabira, 1)), listens = listOf(listen(kabira, 5))),
            ExistingData(),
            setOf(here, viva),
        )
        assertEquals(listOf(here), plan.likes.map { it.trackKey })
        assertEquals(here, plan.listens.single().trackKey)
        assertEquals(1, plan.report.songsMatched)
        assertEquals(0, plan.report.songsMissing)
    }

    @Test
    fun ambiguousFileNamesStayUnmatched() {
        // Two files named Intro.mp3 here: neither is guessed.
        val twoHere = SongMatcher.match(setOf("Old/A/Intro.mp3"), setOf("New/A/Intro.mp3", "New/B/Intro.mp3"))
        assertTrue(twoHere.isEmpty())
        // Two in the backup: neither is guessed either.
        val twoThere = SongMatcher.match(setOf("Old/A/Intro.mp3", "Old/B/Intro.mp3"), setOf("New/Intro.mp3"))
        assertTrue(twoThere.isEmpty())
    }

    @Test
    fun exactPathWinsOverFileName() {
        val matched = SongMatcher.match(setOf(kabira), setOf(kabira, "Elsewhere/Kabira.mp3"))
        assertEquals(mapOf(kabira to kabira), matched)
    }

    @Test
    fun earliestLikeOfADuplicateIsKept() {
        val plan = RestorePlanner.plan(
            backup(likes = listOf(BackupLike(kabira, 50), BackupLike(kabira, 20))),
            ExistingData(),
            setOf(kabira),
        )
        assertEquals(listOf(LikedEntity(kabira, 20)), plan.likes)
    }

    @Test
    fun reorderKeepsSongsTheScreenDidNotShow() {
        assertEquals(listOf(viva, kabira, gone), PlaylistOrder.reordered(listOf(kabira, gone, viva), listOf(viva, kabira)))
    }

    @Test
    fun equaliserCurvesFillOnlySongsAndAlbumsWithoutOne() {
        val backup = Backup(
            createdAt = 1,
            appVersion = null,
            settings = null,
            equaliserSongs = listOf(
                BackupEqualiser("Music/Kabira.mp3", "1,1,1,1,1,1,1,1,1,1", 10),
                BackupEqualiser(viva, "2,2,2,2,2,2,2,2,2,2", 11),
            ),
            equaliserAlbums = listOf(
                BackupEqualiser("monsoon tapes|kavya", "3,3,3,3,3,3,3,3,3,3", 12),
                BackupEqualiser("night bus|", "4,4,4,4,4,4,4,4,4,4", 13),
            ),
        )
        val plan = RestorePlanner.plan(
            backup,
            ExistingData(equaliserTargets = setOf(EQ_SCOPE_SONG to viva, EQ_SCOPE_ALBUM to "night bus|")),
            setOf(kabira, viva),
        )
        // The song moved folders and is found by its file name; the ones set here stay as they are.
        assertEquals(
            listOf(EQ_SCOPE_SONG to kabira, EQ_SCOPE_ALBUM to "monsoon tapes|kavya"),
            plan.equaliser.map { it.scope to it.key },
        )
        assertEquals("1,1,1,1,1,1,1,1,1,1", plan.equaliser.first().gains)
    }

    @Test
    fun equaliserPresetsMergeByName() {
        val backup = Backup(
            createdAt = 1,
            appVersion = null,
            settings = null,
            equaliserPresets = listOf(
                BackupEqualiserPreset(" car ", "1,1,1,1,1,1,1,1,1,1", 1),
                BackupEqualiserPreset("Night", "2,2,2,2,2,2,2,2,2,2", 2),
                BackupEqualiserPreset("night", "3,3,3,3,3,3,3,3,3,3", 3),
                BackupEqualiserPreset("  ", "4,4,4,4,4,4,4,4,4,4", 4),
            ),
        )
        val plan = RestorePlanner.plan(backup, ExistingData(equaliserPresetNames = setOf("Car")), emptySet())
        assertEquals(listOf("Night"), plan.equaliserPresets.map { it.name })
        assertEquals("2,2,2,2,2,2,2,2,2,2", plan.equaliserPresets.single().gains)
    }
}
