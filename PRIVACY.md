# Attic privacy policy

Effective 25 November 2026. Applies to Attic for Android, package `app.attic.player`, version 1.0
and later.

Attic is a music player for the audio files already on your phone. It has no account, no ads, no
analytics and no crash reporting. The developer receives no data from the app.

## What Attic reads on your phone

- **Your music.** With the audio permission you grant, Attic reads the songs Android's media
  library lists, in any folder on the phone or its SD card: their tags (title, artist, album,
  duration), embedded cover art, any lyrics in the tags, and the audio itself. It leaves out
  ringtones, notification and alarm sounds, recordings, podcasts, audiobooks and voice notes.
- **Lyrics files beside your songs.** With the same permission, Attic reads `.lrc` lyrics files
  that have a song's name and sit in its folder. Android lists these with your music.
- **A language label file, if you add one.** Attic reads `language-labels.tsv` from its own folder
  under `Android/data/app.attic.player/files/` if you put one there.

## What Attic stores on your phone

All of this stays in the app's private storage on the device:

- favourites and playlists
- a listening log of plays, skips, finishes and likes, used for radio, the mixes and shelves on
  Home, and Recap
- the saved play queue and your settings
- lyrics you have downloaded, and lyrics read from your music and `.lrc` files
- the results of song analysis: a list of numbers describing each song's sound, which radio uses
  to find similar songs, the musical styles the model rates likeliest for it, and its loudness
- any language you set for a song

Song analysis runs on the phone. No audio, and no analysis result, leaves the device.

Uninstalling Attic, or clearing its storage in Android settings, deletes all of it from the phone.
Your music files are never changed or deleted, and neither are Recap images you saved to
`Pictures`. Copies you made with the backups below are not deleted.

## Backups

**Android's backup.** If backup is turned on in your phone's settings, Android copies two things
to your Google account, usually once a day:

- a backup file Attic keeps in its private storage, with your favourites, playlists, listening log,
  downloaded lyrics, the languages you set and the saved queue. Each song is named by its file's
  path on the phone, such as `Music/Hindi/Kabira.mp3`.
- your settings: theme, style and autoplay

Attic tells Android to send this only when the phone can encrypt it end to end, which needs
Android 9 or later and a screen lock. Neither Google nor the developer can read it. Song analysis
results are not sent; a phone restored from the backup analyses its songs again. When Attic is
installed on a new phone signed in to the same account, Android restores the copy and Attic adds it
to that phone's data. The developer receives nothing. Turning off backup in Android's settings
stops it, and the copy already in your Google account is handled by Android and your account, not
by Attic.

When you move to a new phone by cable or a direct phone-to-phone transfer, Android copies Attic's
data, song analysis included, straight from the old phone to the new one, without going through a
server.

**Backup files you make.** Settings > Backup > "Back up to a file" saves the same data, as a JSON
file, to a place you choose in Android's file picker. If you choose a cloud folder, that service
stores it under its own terms. Attic doesn't upload the file anywhere itself. The file is not
encrypted, so keep it somewhere private: it contains your listening history and the names of your
song files. "Restore from a file" reads a file you choose and adds it to what is on the phone.

## What leaves your phone: lyrics lookups

Attic connects to the internet for one thing: downloading lyrics from
[LRCLIB](https://lrclib.net), a free lyrics service with no account. It does this only when you
tap to download lyrics for a song, or start a download for the whole library in Settings. Songs
whose files already have lyrics are never looked up.

For each song, Attic sends an HTTPS request to `lrclib.net` containing:

- the song's title
- the first artist listed for the song
- the album name, if the song has one
- the song's length in whole seconds

If that finds no match, Attic sends a second request with a search query made of the title and
the first artist. Each request also carries the header
`User-Agent: Attic/<version> (https://github.com/srivathsanvenkateswaran/attic)`, which gives the
app's version number and the address of its source code.

No file, cover art, account, device identifier or listening history is sent. As with any web
request, LRCLIB sees your IP address. LRCLIB is run independently of Attic and its own terms apply
to what it does with requests.

## Cars, watches and other apps on the phone

**Android Auto and media controls.** When Android Auto, a watch or another app connects to
Attic's playback to control it, Attic gives that app the titles, artists, albums and covers of
the songs it asks for: the lists a car browses or searches, and the songs around the one playing.
Attic lets an app read a cover only after it has connected in this way. What that app does with
them, such as showing them on a car's screen, is up to it and its own terms.

**Voice requests.** When you ask the phone's assistant to play something on Attic, the assistant
hands Attic the words it heard, and Attic looks for them in your library on the phone. The
assistant handles your voice under its own terms.

**Equaliser apps.** While it plays, Attic tells equaliser apps on the phone which audio session
to adjust. That message carries the session's number and Attic's package name, nothing else.

None of this goes to the internet from Attic.

## Sharing and saving Recap cards

When you tap Share on a Recap card, Attic renders the card as an image and hands it to the app
you pick in Android's share sheet. When you tap Save, it writes the image to `Pictures/Attic` on
your phone. Nothing is sent anywhere unless you choose to share it.

## Permissions

| Permission | Used for |
|---|---|
| `READ_MEDIA_AUDIO` | Reading your music |
| `POST_NOTIFICATIONS` | Playback controls, and progress while songs are analysed |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | Playing with the screen off |
| `FOREGROUND_SERVICE_DATA_SYNC` | Keeping song analysis running with its progress notification |
| `INTERNET` | Lyrics downloads only |
| `RECEIVE_BOOT_COMPLETED` | Added by WorkManager, the Android library that schedules song analysis, so scheduled work survives a restart |

## What Attic does not do

- No account or sign-in.
- No analytics, telemetry or crash reporting. Attic includes no analytics or crash reporting
  library. ONNX Runtime, the library that runs song analysis, can report usage to Microsoft; Attic
  turns that off, so it sends nothing.
- No ads and no advertising identifier.
- No location, contacts, camera, microphone or other personal data.
- No selling or sharing of data with anyone, because the developer collects none.

## Children

Attic is not directed at children under 13 and collects no personal information from anyone.

## Changes

Changes to this policy are published at this address and in `PRIVACY.md` in the source
repository, whose history shows every earlier version. The effective date at the top changes with
each revision.

## Contact

Open an issue at
[github.com/srivathsanvenkateswaran/attic/issues](https://github.com/srivathsanvenkateswaran/attic/issues).
For a security problem, follow [SECURITY.md](SECURITY.md) instead of filing a public issue.
