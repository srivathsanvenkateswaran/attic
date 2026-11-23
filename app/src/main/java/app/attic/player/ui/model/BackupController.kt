package app.attic.player.ui.model

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import app.attic.player.AtticApp
import app.attic.player.R
import app.attic.player.data.backup.Backup
import app.attic.player.data.backup.BackupReadException
import app.attic.player.data.backup.RestoreReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

/** Why a restore didn't happen. */
enum class RestoreProblem { NotABackup, TooNew, Damaged, TooLarge, CouldNotOpen, Failed }

/** What a backup file holds, for the question before restoring it. */
@Immutable
class BackupSummary(val createdAt: Long, val likes: Int, val playlists: Int, val plays: Int, val lyrics: Int)

/** Where backing up or restoring has got to. Lives with the app, so leaving Settings or switching style keeps it. */
sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Saving : BackupStatus
    data object Reading : BackupStatus
    class Confirming(val backup: Backup, val summary: BackupSummary) : BackupStatus
    data object Restoring : BackupStatus
    class Restored(val report: RestoreReport) : BackupStatus
    class Failed(val problem: RestoreProblem) : BackupStatus
}

/** Runs Settings' "Back up to a file" and "Restore from a file" against the files the system picker returns. */
class BackupController(private val app: AtticApp) {
    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    fun save(uri: Uri) {
        if (_status.value != BackupStatus.Idle) return
        _status.value = BackupStatus.Saving
        app.scope.launch {
            val ok = try {
                withContext(Dispatchers.IO) {
                    // "wt" truncates, so choosing an older, larger backup to overwrite leaves no tail behind.
                    app.contentResolver.openOutputStream(uri, "wt")?.use { app.backup.writeTo(it) } != null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't save the backup", e)
                false
            }
            _status.value = BackupStatus.Idle
            app.messages.tryEmit(app.getString(if (ok) R.string.backup_saved else R.string.backup_save_failed))
        }
    }

    fun open(uri: Uri) {
        if (_status.value != BackupStatus.Idle) return
        _status.value = BackupStatus.Reading
        app.scope.launch {
            _status.value = try {
                val backup = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { app.backup.readFrom(it) }
                        ?: throw FileNotFoundException(uri.toString())
                }
                BackupStatus.Confirming(
                    backup,
                    BackupSummary(
                        createdAt = backup.createdAt,
                        likes = backup.likes.size,
                        playlists = backup.playlists.size,
                        plays = backup.listens.count { it.listenedMs > 0 },
                        lyrics = backup.lyrics.size,
                    ),
                )
            } catch (e: BackupReadException) {
                Log.w(TAG, "Not a usable backup", e)
                BackupStatus.Failed(
                    when (e.reason) {
                        BackupReadException.Reason.NotABackup -> RestoreProblem.NotABackup
                        BackupReadException.Reason.TooNew -> RestoreProblem.TooNew
                        BackupReadException.Reason.Damaged -> RestoreProblem.Damaged
                        BackupReadException.Reason.TooLarge -> RestoreProblem.TooLarge
                    },
                )
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't open the backup", e)
                BackupStatus.Failed(RestoreProblem.CouldNotOpen)
            } catch (e: SecurityException) {
                Log.w(TAG, "Couldn't open the backup", e)
                BackupStatus.Failed(RestoreProblem.CouldNotOpen)
            }
        }
    }

    fun confirm() {
        val confirming = _status.value as? BackupStatus.Confirming ?: return
        _status.value = BackupStatus.Restoring
        app.scope.launch {
            _status.value = try {
                BackupStatus.Restored(app.backup.restore(confirming.backup, applySettings = true))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Restore failed", e)
                BackupStatus.Failed(RestoreProblem.Failed)
            }
        }
    }

    /** Closes the question or the result. Ignored while work is running. */
    fun dismiss() {
        when (_status.value) {
            is BackupStatus.Confirming, is BackupStatus.Restored, is BackupStatus.Failed -> _status.value = BackupStatus.Idle
            else -> Unit
        }
    }

    private companion object {
        const val TAG = "Backup"
    }
}
