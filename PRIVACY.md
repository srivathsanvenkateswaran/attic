# Attic privacy policy

Effective 23 November 2026. Applies to Attic for Android, package `app.attic.player`, version 1.0
and later.

Attic is a music player for the audio files already on your phone. It has no account, no ads, no
analytics and no crash reporting. The developer receives no data from the app.

## What Attic reads on your phone

- **Your music.** With the audio permission you grant, Attic reads the files in the `Music/Offline`
  folder through Android's media library: their tags (title, artist, album, duration), embedded
  cover art and the audio itself.
- **A language label file, if you add one.** Attic reads `language-labels.tsv` from its own folder
  under `Android/data/app.attic.player/files/` if you put one there.

## What Attic stores on your phone

All of this stays in the app's private storage on the device:

- favourites and playlists
- a listening log of plays, skips, finishes and likes, used for radio, instant mixes and Recap
- the saved play queue and your settings
- lyrics you have downloaded
- the results of song analysis, which turns each song's sound into a list of numbers so radio can
  find similar songs, and any language you set for a song

Song analysis runs on the phone. No audio, and no analysis result, leaves the device.

Attic opts out of Android's cloud backup, so none of this is copied to Google's servers. When you
move to a new phone, Android's device-to-device transfer may copy it directly from the old phone
to the new one. Uninstalling Attic, or clearing its storage in Android settings, deletes all of it. Your
music files are never changed or deleted, and neither are Recap images you saved to `Pictures`.

## What leaves your phone: lyrics lookups

Attic connects to the internet for one thing: downloading lyrics from
[LRCLIB](https://lrclib.net), a free lyrics service with no account. It does this only when you
tap to download lyrics for a song, or start a download for the whole library in Settings.

For each song, Attic sends an HTTPS request to `lrclib.net` containing:

- the song's title
- the first artist listed for the song
- the album name, if the song has one
- the song's length in whole seconds

If that finds no match, Attic sends a second request with a search query made of the title and
the first artist. Each request also carries the header `User-Agent: Attic/1.0 (personal offline
music player)`.

No file, cover art, account, device identifier or listening history is sent. As with any web
request, LRCLIB sees your IP address. LRCLIB is run independently of Attic and its own terms apply
to what it does with requests.

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
  library.
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
