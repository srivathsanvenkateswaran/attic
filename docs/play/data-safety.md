# Data safety form answers

These answers come from the code at the commit that added this file. The definitions quoted are from [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469). Read every network call again before changing an answer.

## What leaves the phone

The app has one network call: `LyricsRepository.get()` (`app/src/main/java/app/attic/player/data/LyricsRepository.kt`).
- It sends an HTTPS GET to `https://lrclib.net/api/get` or `/api/search`, with the song's artist, title and album and its length in seconds. These come from the tags of a file in the user's library.
- It also sends a User-Agent of `Attic/<version> (https://github.com/srivathsanvenkateswaran/attic)`. No identifier, account or device information is sent.
- It runs only when the user taps Download on a song's lyrics or "Download lyrics for all songs" in Settings. Settings states what is sent next to that button.
- LRCLIB is a third-party service. Attic's developer runs no server and receives nothing.

Everything else stays on the phone:
- Playback reads local files through MediaStore. Media3 only plays `content://` URIs from the library.
- Song analysis runs the bundled ONNX model on the device and stores embeddings in Room.
- ONNX Runtime's telemetry uploader is removed from the manifest, and `ORT_DISABLE_TELEMETRY=1` is set before the runtime loads (`SongAnalyzer.kt`).
- Coil is included without a network module, so it only loads cover art from local files.
- There are no analytics, crash reporting, ads or account SDKs.
- Sharing a Recap card hands an image to the Android share sheet. The user picks the destination, and the app transmits nothing itself.
- "Back up to a file" in Settings writes a JSON file through the system file picker (`ACTION_CREATE_DOCUMENT`) to a place the user chooses, and "Restore from a file" reads one the user chooses. The app opens no connection for either. If the user picks a cloud folder, the provider's app does the upload.

### Android's backup

The manifest sets `allowBackup="true"` with `res/xml/data_extraction_rules.xml`. The app makes none of these transfers: Android does, on its own schedule.
- **Cloud backup** sends `files/backup/attic-backup.json.gz` (favourites, playlists, play counts, the listening log, found lyrics, song languages and the saved queue) and `shared_prefs/settings.xml` to the user's Google account. The database, and with it song analysis, is not sent.
- `<cloud-backup disableIfNoEncryptionCapabilities="true">` means nothing is sent unless the backup can be end-to-end encrypted. Android: "Setting this constraint stops backups from being sent to the cloud if the user's device cannot support encryption" ([Auto Backup](https://developer.android.com/identity/data/autobackup)), and "The backup is end-to-end encrypted on devices running Android 9 or higher using the device's PIN, pattern, or password" (same page).
- **Device-to-device transfer** copies the app's data from the old phone to the new one. Android: "D2D transfers aren't sent to the server" (same page).
- The developer can't read either. Neither reaches a server the developer runs, and there is none.

Why this isn't declared as collection:
- No Play page names Android's backup or device transfer. This is a reading of the form's rules, not a ruling Play has published.
- Play's exemptions include data that is "unreadable by you or anyone other than the sender and recipient as a result of end-to-end encryption" ([Data safety](https://support.google.com/googleplay/android-developer/answer/10787469), "Data collection"). Because of the encryption constraint, cloud backup only ever sends data in that state.
- The same page's FAQ takes the case "My app enables users to upload their data directly to Google Drive or Dropbox for backup or storage." Its answer says that if "your app never collects or accesses the data in question, then your app does not need to declare the collection of this data", with a condition on whose terms govern the upload (read it on the page). The file export fits: the user picks the destination and the app sends nothing.
- Device transfer goes phone to phone, with no server involved.

If Play's guidance ever names Android's backup, or `disableIfNoEncryptionCapabilities` is removed, revisit the "Files and docs" and "App activity" answers below. A cloud copy without end-to-end encryption would hold the listening log, which is app activity.

## Answers

**Does your app collect or share any of the required user data types?** Yes.
- "Collect" means "transmitting data from your app off a user's device". A lyrics request transmits the song's tags off the device, even though it goes to LRCLIB and not to the developer.
- The form has no exemption from collection for user-initiated actions, only for data processed on the device.

**Is all of the user data collected by your app encrypted in transit?** Yes.
- The only endpoint is `https://lrclib.net`.
- With `targetSdk` 36, Android blocks cleartext HTTP by default, and the manifest sets neither `usesCleartextTraffic` nor a network security config.

**Which methods of account creation does your app support?** None. The app has no accounts.

**Do you provide a way for users to request that their data is deleted?** No.
- The developer holds no user data.
- Lyrics and analysis results are stored only on the phone, and uninstalling or clearing the app's storage removes them.
- A copy in Android's backup lives in the user's Google account, end-to-end encrypted, and the developer can't reach it. A backup file the user saved is theirs.
- What LRCLIB keeps from a request is outside the app's control, and the privacy policy says so.

### Data types

One type is collected.

**Files and docs: collected, not shared.**
- Play defines it as "a user's files or documents, or information about their files or documents such as file names". The title, artist, album and length read from a song file's tags are information about the user's files.
- "Music files" ("a user's music files") doesn't fit, because the files themselves are never sent.

The form then asks about this type:

| Question | Answer | Why |
|---|---|---|
| Collected or shared? | Collected | It leaves the device. |
| Shared? | No | Transfers to a third party need not be disclosed as sharing when they are "based on a specific user-initiated action, where the user reasonably expects the data to be shared". Each lyrics request follows a tap on Download, and Settings says the details go to lrclib.net. |
| Processed ephemerally? | No | Ephemeral means "retained for no longer than necessary to service the specific request in real-time". That depends on LRCLIB's servers, which the developer can't vouch for. |
| Required or optional? | Optional | Lyrics are never downloaded unless the user asks. The rest of the app works without them. |
| Purpose | App functionality | Only to find the song's lyrics. |

Every other data type: not collected, not shared. That covers location, personal info, financial info, health, messages, photos and videos, audio, contacts, calendar, app activity, web browsing, app info and performance, and device or other IDs.

- The app makes no use of the IP address that LRCLIB sees with each request, and derives no location from it. The form's location note covers location "inferred, such as via IP address", and Attic infers none.
- Device or other IDs were the type at risk: ONNX Runtime's uploader would have sent `ANDROID_ID` to Microsoft if its provider were left in the manifest.

## If something changes

Revisit these answers before shipping any of these:
- a network library
- an SDK
- crash reporting
- a new endpoint
- a feature that sends anything automatically rather than on a tap
- a change to `res/xml/data_extraction_rules.xml`, above all removing `disableIfNoEncryptionCapabilities` or adding the database to `<cloud-backup>`

The same applies to removing the `tools:node="remove"` entry for `ai.onnxruntime.TelemetryInitializer` in `AndroidManifest.xml`.
