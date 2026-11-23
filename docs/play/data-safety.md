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

The same applies to removing the `tools:node="remove"` entry for `ai.onnxruntime.TelemetryInitializer` in `AndroidManifest.xml`.
