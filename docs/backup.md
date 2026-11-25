# Backup and restore

Attic keeps a user's data in two ways: a file the user saves from Settings, and Android's own
backup. Both use the same format, and a restore from either merges into what is already on the
phone.

## The file

"Back up to a file" writes `attic-backup-YYYY-MM-DD.json` through the system file picker
(`ACTION_CREATE_DOCUMENT`), and "Restore from a file" reads one (`ACTION_OPEN_DOCUMENT`). No storage
permission is involved. The code is in `app/src/main/java/app/attic/player/data/backup/`.

UTF-8 JSON. The reader also takes the gzipped form that Android's backup carries:

```json
{
  "format": "attic-backup",
  "schemaVersion": 3,
  "createdAt": 1795392000000,
  "appVersion": "1.0.0",
  "settings": { "theme": "Dark", "uiStyle": "Comfort", "autoplay": true,
                "equaliserOn": true, "equaliserDefault": "0,0,0,0,0,0,0,0,0,0" },
  "likes": [{ "song": "Music/Hindi/Kabira.mp3", "likedAt": 1790000000000 }],
  "playlists": [{ "name": "Late drive", "createdAt": 1780000000000, "songs": ["Music/Hindi/Kabira.mp3"] }],
  "plays": [{ "song": "Music/Hindi/Kabira.mp3", "lastPlayedAt": 1794000000000, "playCount": 214 }],
  "listens": [{ "song": "Music/Hindi/Kabira.mp3", "startedAt": 1794000000000, "listenedMs": 180000,
                "durationMs": 240000, "outcome": "finished", "context": "Late drive", "radioCross": false }],
  "lyrics": [{ "song": "Music/Hindi/Kabira.mp3", "synced": "[00:01.00] …", "plain": null,
               "instrumental": false, "fetchedAt": 1793000000000 }],
  "languages": [{ "song": "Music/Hindi/Kabira.mp3", "language": "hi", "source": "manual" }],
  "queue": { "songs": ["…"], "index": 0, "positionMs": 107000, "repeatMode": 0, "shuffle": false,
             "baseOrder": null, "context": "Late drive", "radioSeed": null },
  "equaliser": {
    "songs": [{ "song": "Music/Hindi/Kabira.mp3", "gains": "-1.5,-3,-3,1.5,3.75,3.75,3,1.5,0,-1.5", "savedAt": 1794100000000 }],
    "albums": [{ "album": "monsoon tapes|kavya", "gains": "5.5,4.25,3.5,2.5,1.25,0,0,0,0,0", "savedAt": 1794200000000 }],
    "presets": [{ "name": "Car", "gains": "3,3,0,0,0,0,0,0,2,2", "createdAt": 1794300000000 }]
  },
  "interests": { "moods": ["chill", "focus"], "genres": ["hip hop"], "languages": ["ta", "en"],
                 "updatedAt": 1795392000000 }
}
```

- A song is its library key: the file's path relative to its volume, `RELATIVE_PATH + DISPLAY_NAME`, which is what the database stores. MediaStore IDs are never written.
- Times are milliseconds since the epoch.
- An equaliser curve is ten gains in dB, comma separated, for the bands at 31 Hz to 16 kHz. An album is its library key, the album's name and artist as the library files them.

**Versions.**
- A reader accepts any `schemaVersion` up to its own and ignores fields it doesn't know. A missing list counts as empty, so a later version can add fields and sections without a new number.
- A change an older reader would misread, such as a renamed field or a changed meaning, raises the number. An older Attic then refuses the file with "made by a newer version of Attic" instead of guessing.
- Version 2 added the `equaliser` section and the two equaliser settings. A version 1 reader would have skipped them without a word and restored everything else, losing every song's curve, so the number went up and an older Attic refuses the file. Version 1 files still read.
- Version 3 added `interests`, the moods, genres and languages picked for mood mixes (docs/features/mood-mixes.md). A version 2 reader would skip them without a word and restore the rest, so the number went up again. Version 1 and 2 files still read, with no picks.
- The format has its own types (`Backup.kt`), separate from the Room entities, so a database migration doesn't change the file.

**What a restore does.** Nothing is deleted. Duplicates are matched by key, so restoring the same file twice adds nothing the second time (`RestorePlanner`):

| Data | Merge rule |
|---|---|
| Favourites | Added if not already a favourite. A song liked twice in the file keeps the earlier time. |
| Playlists | Matched by name, ignoring case and spaces at the ends. An existing playlist gains the songs it lacks, at its end. Otherwise the playlist is created. |
| Listening log | A listen is the same if it has the same song and start time. |
| Play counts | Keeps the later "last played" and the larger count, so a repeat restore doesn't double them. |
| Lyrics | Fill songs with no lyrics, and replace a checked miss. Lyrics already found stay. |
| Languages | Added where there is none. A manual choice in the file replaces an imported one. |
| Saved queue | Only if the phone has none. |
| Equaliser curves | Fill songs and albums that have none on the phone. A song is matched as below. |
| Equaliser presets | Matched by name, ignoring case and spaces at the ends. A preset already on the phone stays as it is. |
| Mood-mix picks | Only if the phone has never had any. Picks saved here, even saved as none, stay. |
| Settings | Theme, style, autoplay, the equaliser switch and its default curve come from the file. The confirmation says so. A restore from Android's backup leaves them alone, because Android restores the settings file itself. |

The whole merge is one database transaction, so a failure changes nothing.

**Matching songs.**
- A song in the file matches a song on the phone by its path.
- If the music sits in another folder on the new phone (`Music/` instead of `Music/Offline/`), a song still matches by file name. This needs exactly one file with that name on the phone and exactly one in the backup, so two different `01 Intro.mp3` are never mixed up.
- Rows for songs that match neither way are kept under the old path. Nothing in the database ties a favourite, playlist entry or listen to a file that exists, so a row reattaches as soon as a file appears at that path.
- The result dialog says how many of the backup's songs are on the phone.
- Screens already show only songs that are on the phone. Two places now cope with kept rows. The Favourites count counts only songs that are here. Reordering a playlist keeps the songs it didn't show after the ones it did, where before it dropped them.

**Damaged and foreign files.** The reader reports one of four reasons, and the dialog explains each. It checks every field's type, so a file with a field of the wrong type is refused instead of half-read.

| Reason | When |
|---|---|
| Not a backup | Not JSON, or JSON without `"format": "attic-backup"` |
| Too new | The schema version is above this version's |
| Damaged | The file is cut short, a field has the wrong type, or a gzip stream is broken |
| Too large | The file is over 64 MB |

## What is left out, and why

- **Song analysis** (`song_analysis`) is the one big table. Each song holds a 1,280-float embedding (5 KB) and a melody fingerprint of up to 2,400 × 12 bytes (29 KB), so about 34 KB a song: 34 MB for 1,000 songs, and about 45 MB as base64 in JSON. Everything else is usually a few megabytes.
  - Recomputing takes about 27 s a song, roughly 7.5 hours for 1,000 songs. That happens unattended while the phone charges, and "Analyse now" starts it at once.
  - The embedding describes the audio, not the path. A file at the same path on the new phone may be a different encode, and the model version may have changed, so the new phone's own analysis is the safer answer.
  - Radio works on the analysed part of the library in the meantime and says so.
  - Device-to-device transfer copies the database as it is, so a phone moved that way keeps its analysis.
- **Lyrics checked and not found** are not saved, so the new phone can look again.
- **Caches** (shared Recap images) and the **widget's last song** are not user data.

## Android's backup

`AndroidManifest.xml` sets `allowBackup="true"` and `dataExtractionRules="@xml/data_extraction_rules"`.

**Cloud backup** carries `files/backup/attic-backup.json.gz` and `shared_prefs/settings.xml` only.
- The database is not sent. Song analysis can take it past the 25 MB that Android allows: "Every app can allocate up to 25 MB of backup data per app user", and "If the amount of data is over 25 MB, the system calls `onQuotaExceeded()` and doesn't back up data to the cloud" ([Auto Backup](https://developer.android.com/identity/data/autobackup)). One large library would otherwise lose the whole backup, settings included.
- Copying a live SQLite file in WAL mode also risks an inconsistent copy. The JSON copy is written whole and then renamed into place.
- `MainActivity.onStop` refreshes the copy, through `BackupRepository.saveSnapshot`. Android backs up when the phone is idle, at most once a day, so the copy it takes is the one from the last time the app was left. A copy whose content hasn't changed isn't rewritten.
- If the gzipped copy passes 20 MB, it is written without lyrics, which can be downloaded again. If it is still too big, the last copy is kept.
- `disableIfNoEncryptionCapabilities="true"`: nothing goes to the cloud unless it can be end-to-end encrypted. Android: "The backup is end-to-end encrypted on devices running Android 9 or higher using the device's PIN, pattern, or password." That makes the data unreadable to Google and to the developer, which is what the Data safety answers rely on (`docs/play/data-safety.md`).
- When Android restores the app on a new phone, the copy arrives before the first launch ("The restore operation occurs after the APK is installed but before the app is available to be launched by the user"). `BackupRepository.restoreSnapshotOnce` merges it once audio access is granted, so songs in other folders can be matched by name.
- A marker in `noBackupFilesDir` records that the check has been done. Android never backs that directory up, so every new install checks once.
- Until the check is done, the copy is not rewritten, so an empty new install can't overwrite the backup it came with.

**Device-to-device transfer** copies everything except:
- WorkManager's database, because the app schedules analysis again on launch
- the widget's last song, because its MediaStore ID means another file on the new phone
- the JSON copy, because the database it came from travels anyway

Caches and `no_backup` are never copied.

**`fullBackupContent`** is not set. It is the rule format for Android 11 and lower. With `minSdk` 33, every phone that can install Attic reads `dataExtractionRules` instead ("For devices running Android 12 or higher, apps targeting API level 31+ use the `data-extraction-rules` format", same page). A second copy of the rules would be untested and could drift.

**Privacy.** Before this change, `allowBackup="false"` stopped cloud backup. On Android 12 and later it does not stop device-to-device transfer on every phone ("doesn't disable device-to-device transfers for the app" on some manufacturers' devices, same page), so the old privacy policy already allowed for that. Now:
- the cloud receives the user's data, encrypted end to end, in their own Google account
- song analysis stays on the phone
- the user can turn it off in Android's settings

## Checking on a phone

These need a device. None has been run.

1. Back up to a file, to the Downloads folder and to a cloud folder. Open the file and check it is readable JSON with the expected counts.
2. Restore it on the same phone. The result should say nothing new was added.
3. Clear the app's storage, grant audio access, and restore. Favourites, playlists, Recap and lyrics should come back, along with the theme and style.
4. Move a few files to another folder, then restore. They should match by file name.
5. Delete a few files, restore, put them back. They should reattach to favourites and playlists.
6. Pick an MP3, a truncated copy of a backup, and a backup with `"schemaVersion": 4`. Each should show its own message and change nothing.
7. Run the cloud backup by hand and restore it:
   ```
   adb shell bmgr backupnow app.attic.player
   adb uninstall app.attic.player
   adb install app-debug.apk
   ```
   Before the first launch, `files/backup/attic-backup.json.gz` should exist. After audio access is granted, the data should be back. Check `adb logcat -s Backup`.
8. Check `adb shell dumpsys backup` and the copy's size with a large library.
9. Do a device-to-device transfer, or `adb shell bmgr` with the D2D transport if one is available. Analysis should arrive with the database, and the widget should start blank.
10. In both styles, check the Backup rows, the busy text, the confirmation and result dialogs, and the one-button Comfort dialog, in light and dark.
