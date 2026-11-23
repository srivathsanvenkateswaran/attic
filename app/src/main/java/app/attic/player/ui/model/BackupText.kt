package app.attic.player.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.attic.player.R
import app.attic.player.data.backup.RestoreReport
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

// The restore dialogs' sentences, built once here so both styles say the same thing.

/** "Made 23 November 2026, with 31 favourites, 2 playlists, 6,212 plays and lyrics for 112 songs. …" */
@Composable
fun restoreConfirmBody(summary: BackupSummary): String {
    val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date(summary.createdAt))
    return stringResource(
        R.string.restore_confirm_body,
        date,
        counted(R.plurals.backup_favourites, summary.likes),
        counted(R.plurals.backup_playlists, summary.playlists),
        counted(R.plurals.backup_plays, summary.plays),
        counted(R.plurals.backup_lyrics, summary.lyrics),
    )
}

/** What the restore added, then how many of the backup's songs are on this phone. */
@Composable
fun restoreResultBody(report: RestoreReport): String {
    val added = report.likesAdded + report.playlistsAdded + report.playlistsMerged + report.listensAdded + report.lyricsAdded
    val parts = ArrayList<String>()
    if (added == 0) {
        parts += stringResource(R.string.restore_done_nothing)
    } else {
        parts += stringResource(
            R.string.restore_done_added,
            counted(R.plurals.backup_favourites, report.likesAdded),
            counted(R.plurals.backup_playlists, report.playlistsAdded),
            counted(R.plurals.backup_plays, report.listensAdded),
            counted(R.plurals.backup_lyrics, report.lyricsAdded),
        )
        if (report.playlistsMerged > 0) parts += counted(R.plurals.restore_done_merged, report.playlistsMerged)
    }
    val total = report.songsMatched + report.songsMissing
    if (total > 0) {
        val format = NumberFormat.getIntegerInstance()
        parts += pluralStringResource(R.plurals.restore_done_matched, total, format.format(report.songsMatched), format.format(total))
        if (report.songsMissing > 0) parts += stringResource(R.string.restore_done_missing)
    }
    return parts.joinToString(" ")
}

@Composable
fun restoreProblemBody(problem: RestoreProblem): String = stringResource(
    when (problem) {
        RestoreProblem.NotABackup -> R.string.restore_problem_not_backup
        RestoreProblem.TooNew -> R.string.restore_problem_too_new
        RestoreProblem.Damaged -> R.string.restore_problem_damaged
        RestoreProblem.TooLarge -> R.string.restore_problem_too_large
        RestoreProblem.CouldNotOpen -> R.string.restore_problem_open
        RestoreProblem.Failed -> R.string.restore_problem_failed
    },
)

@Composable
private fun counted(id: Int, count: Int): String =
    pluralStringResource(id, count, NumberFormat.getIntegerInstance().format(count))
