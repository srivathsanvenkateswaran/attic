# Google Play readiness

Checked on 2026-11-23. **Done** means the repository already meets the requirement. **Owner** means it needs something only the developer account holder can do. The Play Console steps are listed in order at the end.

## Build

| Requirement | Status | Evidence |
|---|---|---|
| App bundle | Done | `./gradlew bundleRelease` writes `app/build/outputs/bundle/release/app-release.aab` (38.7 MB). |
| Signed with an upload key | Owner | `app/build.gradle.kts` signs release builds with the key named by `ATTIC_UPLOAD_STORE_FILE`, `ATTIC_UPLOAD_STORE_PASSWORD`, `ATTIC_UPLOAD_KEY_ALIAS` and `ATTIC_UPLOAD_KEY_PASSWORD`. Without them it signs with the debug key and prints a warning, and Play rejects a debug-signed bundle. Run `scripts/make-upload-key.sh` and add the four lines it prints to `~/.gradle/gradle.properties`. |
| Play App Signing | Owner | New apps use it by default. Accept it at the first upload, and keep the upload keystore and its password backed up off this machine. |
| Version | Done | `gradle.properties`: `attic.versionName=1.0.0`, `attic.versionCode=1`. Raise `attic.versionCode` by one for every upload, because Play refuses a code it has already seen. It is not derived from git, so F-Droid builds stay reproducible. |
| Target API level | Done | `targetSdk = 36`. New apps and updates have had to target API 36 since 31 August 2026 ([target API requirements](https://developer.android.com/google/play/requirements/target-sdk)). No API 37 deadline has been announced. |
| 16 KB page size | Done | `zipalign -c -P 16 -v 4` passes on the release APK. Every LOAD segment of `libonnxruntime.so`, `libonnxruntime4j_jni.so` and `libandroidx.graphics.path.so` is aligned to `0x4000` (`llvm-readelf -lW`). The libraries are stored uncompressed, and `extractNativeLibs` is false. Play requires 16 KB support for apps that target Android 15 or later and ship native code ([page sizes](https://developer.android.com/guide/practices/page-sizes)). |
| 64-bit | Done | The only ABI is `arm64-v8a` (`abiFilters`), so there is no 32-bit code. The app doesn't install on x86_64 Chromebooks or on 32-bit-only phones. Adding ABIs is a product choice: Play's per-ABI split APKs would keep each download the same size. |
| R8 keeps what reflection needs | Done | `mapping.txt` keeps these names unchanged: `AtticDatabase_Impl` and WorkManager's `WorkDatabase_Impl` (Room), `AnalysisWorker` and its constructor (WorkManager's consumer rule), `PlaybackService` and `PlayerWidget` (manifest), and the `ai.onnxruntime` classes (`proguard-rules.pro`). Both `assembleRelease` and `bundleRelease` succeed. |
| Lint | Done | `./gradlew lintRelease` reports 0 errors and 52 warnings. The warnings are unused resources, KTX and Compose style notes, newer dependency versions and the ChromeOS ABI note. `DataExtractionRules` no longer appears, because the manifest now names `res/xml/data_extraction_rules.xml`. `app/lint.xml` gives the reason for each false positive it silences. |
| Debug-only components | Done | `KitGalleryActivity`, `WidgetHostActivity` and `SystemSurfacesReceiver` are declared in `app/src/debug/AndroidManifest.xml` only and are absent from the release manifest. |

## Manifest and permissions

The merged release manifest requests `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`, `WAKE_LOCK`, `INTERNET` and `RECEIVE_BOOT_COMPLETED`, plus androidx.core's signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

- None of these needs a Play permission declaration form, apart from the foreground service types below. There is no location, SMS, call log, `QUERY_ALL_PACKAGES`, all-files access or exact alarm.
- `ACCESS_NETWORK_STATE` is added by Media3 and ONNX Runtime. It is removed with `tools:node="remove"` because nothing uses it.
- `RECEIVE_BOOT_COMPLETED` comes from WorkManager, which uses it to restore scheduled analysis after a reboot. It is kept.
- Backup and restore add no permission. "Back up to a file" and "Restore from a file" go through the system file picker. Android's backup is on (`allowBackup="true"`). Its cloud half carries only an end-to-end-encrypted JSON copy and the settings file, and never the database (`docs/backup.md`).
- `READ_MEDIA_AUDIO` and `POST_NOTIFICATIONS` are requested together from the first screen, after text that says what each one is for. If the user refuses, the screen links to the app's permission settings (`RootModel.kt`).
- ONNX Runtime 1.31 adds `ai.onnxruntime.TelemetryInitializer`. At launch it starts Microsoft's 1DS telemetry uploader, which reads `ANDROID_ID`, the device class, locale and time zone. ONNX Runtime's `Privacy.md` says telemetry is on by default in official Android builds and keeps the platform device IDs there. The manifest removes the provider, and `SongAnalyzer` sets `ORT_DISABLE_TELEMETRY=1` before the runtime loads. Without that fix, the Data safety form would have to declare device IDs shared with Microsoft.

## Foreground services

Play asks for a declaration per foreground service type: a description, a use case, the user impact if the task is deferred or interrupted, and a video link ([Play FGS requirements](https://support.google.com/googleplay/android-developer/answer/13392821)). Attic uses two types.

**Song analysis stays `dataSync`.** It does not move to `mediaProcessing`.
- Android's list for `dataSync` includes "Local file processing" ([service types](https://developer.android.com/develop/background-work/services/fgs/service-types)).
- Play offers `dataSync` a use case called "Local processing: Other", described as "Use for specifically user-initiated work and not for regular system or server-initiated tasks", for example encryption or resizing.
- Play's only `mediaProcessing` use case is "Media transcoding". Its video must show "converting or transcoding a video or audio file to a different format". Analysis converts nothing.
- `mediaProcessing` also exists only from API 35, and `minSdk` is 33.
- To fit "user-initiated", `AnalysisWorker` now becomes a foreground service only on a run the user starts with "Analyse now without a charger". A run that waited for the charger stays an ordinary WorkManager job and shows the same notification.
- On Android 15 and later, `dataSync` gets 6 hours per 24 ([timeout](https://developer.android.com/develop/background-work/services/fgs/timeout)). WorkManager 2.12's `SystemForegroundService` implements both `onTimeout` overloads and stops the work. Progress is saved after every song, so a stopped run loses nothing.
- Android 16 makes jobs started from a foreground service count against their normal quotas ([FGS changes](https://developer.android.com/develop/background-work/services/fgs/changes)). That affects only how fast the background chunks restart.

Declaration text to paste into Play Console, under App content, Foreground service permissions:

> **Type:** Data sync. **Use case:** Local processing: Other.
>
> **Description:** Attic plays music files stored on the phone. Its song radio feature finds similar songs by analysing each audio file on the device with a bundled machine-learning model. The analysis decodes the file and computes a sound fingerprint, which is saved in the app's local database. Nothing is uploaded. The user starts a run from Settings > Song radio > "Analyse now without a charger". While it runs, a notification shows "Analysing your songs" with progress and a Stop button. Analysis takes about 27 seconds per song, so a few hundred songs take a few hours, and the foreground service keeps that work going when the user leaves the app. Runs that start automatically while the phone charges do not use a foreground service.
>
> **If the task is deferred:** Radio and autoplay can't suggest similar songs for songs that are not analysed yet. Playback is unaffected. Analysis also runs while the phone charges.
>
> **If the task is interrupted:** Results are saved after each song, so analysis resumes where it stopped. Nothing is lost.
>
> **Video:** [link]

> **Type:** Media playback.
>
> **Description:** Attic plays the user's local music files. A Media3 MediaSessionService keeps playing when the app is in the background, with the media notification, lock-screen controls, headset and Bluetooth buttons, and the home-screen widget.
>
> **If the task is deferred:** Music the user started doesn't play.
>
> **If the task is interrupted:** Playback stops mid-song, and the user has to reopen the app to resume.
>
> **Video:** [link]

What each video needs to show:
- **Data sync:** open Settings, scroll to Song radio, tap "Analyse now without a charger", pull down the notification shade to show the progress notification, go to the home screen, and come back to show the count has moved on.
- **Media playback:** play a song, leave the app, and control playback from the notification and the lock screen.

Upload each video to YouTube as unlisted.

## Policy

| Requirement | Status | Evidence |
|---|---|---|
| Data safety form | Owner | Every app must complete it, including apps that collect nothing ([Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)). Answers, with the code behind each one, are in [`data-safety.md`](data-safety.md). It explains why Android's backup and the backup file aren't declared as collection. No Play page addresses Android's backup directly, so that answer rests on the end-to-end-encryption exemption. |
| Privacy policy | Owner | All apps need a privacy policy URL in Play Console ([User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)). The policy is [`PRIVACY.md`](../../PRIVACY.md) at the repository root. `site/privacy.html` is generated from it and published by GitHub Pages at `https://srivathsanvenkateswaran.github.io/attic/privacy.html`, which is the URL to enter. In the app, Settings already states what the lyrics download sends and that analysis uploads nothing. |
| Closed test before production | Owner | Personal developer accounts created after 13 November 2023 must run a closed test with at least 12 testers opted in for 14 consecutive days before applying for production access ([testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)). |
| Target audience | Owner | Choose 13 and over, or ages 13–15 and older. If the audience includes children under 13, the Families policy applies ([target audience](https://support.google.com/googleplay/android-developer/answer/9867159)). Attic isn't designed for children, and choosing 18+ would hide it from teenagers for no reason. Store graphics that look aimed at children can still get a 13+ app reviewed under Families rules. |
| Content rating | Owner | Every app needs an IARC rating, and apps without one are removed ([content ratings](https://support.google.com/googleplay/android-developer/answer/9898843)). Accurate answers: no violence, sexual content, gambling, purchases or ads, no user-to-user interaction, and no sharing of location. Lyrics for the user's songs come from LRCLIB unfiltered, so they can contain strong language, and [`CONSOLE.md`](CONSOLE.md) answers Yes to profanity for that reason. |
| App access | Owner | No login is needed. Tell reviewers to put any music files on the phone, for example in `Music/` or `Download/`. The library is empty until then. |
| Ads | Owner | Declare "No ads". The code contains no ad SDK. |
| Store listing metadata | Owner | Title of 30 characters or fewer. No emoji, no ranking or price claims such as "#1" or "free", and no keyword lists ([metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)). Keep other brands out of the listing: the design notes compare the styles to Spotify, Pixel and One UI, and those names are not in the app. The README screenshots show commercial album covers, so use covers you have rights to for store screenshots. |
| User-visible trademarks | Done | Searching `res/` and the strings in code for Spotify, Wrapped, Pixel, One UI and Samsung finds them only in comments, the internal route `"wrapped"` and class names. The shipped font notice named the `pixel/` and `oneui/` mock folders and now says "Expressive and Comfort". |
| Android developer verification | Done | Play registers the apps it distributes automatically ([developer verification](https://developer.android.com/developer-verification)). Sideloaded and F-Droid builds are separate ([open-source registration](https://developer.android.com/developer-verification/guides/open-source-app-registration)). |

## Licences

| Component | Licence | Status |
|---|---|---|
| Attic | GPL-3.0-or-later | Done: `LICENSE` ships as `assets/licences/LICENSE-attic.txt` |
| Google Sans Flex, Onest | SIL OFL 1.1 | Done: the OFL texts and `fonts-NOTICE.txt` ship in `assets/licences/` |
| Material Symbols, AndroidX, Kotlin, kotlinx.coroutines, Guava, Coil, Okio, Reorderable | Apache 2.0 | Done: the licence text and `libraries-NOTICE.txt` ship in `assets/licences/` |
| ONNX Runtime 1.31.1 | MIT, plus its third-party notices | Done: the MIT text and `ThirdPartyNotices.txt`, taken from the 1.31.1 jar on Maven Central, ship in `assets/licences/` |
| Discogs-EffNet model | CC BY-NC-SA 4.0 or BY-NC-ND 4.0 (sources disagree) | Owner decision, below |

Settings > About > Open-source licences lists each entry and opens its text, in both styles.

### The Discogs-EffNet model

The Essentia sources don't agree on the licence:
- Essentia's [models page](https://essentia.upf.edu/models.html) says "All the models created by the MTG are licensed under CC BY-NC-SA 4.0", also "available under proprietary license upon request".
- The [`LICENSE`](https://essentia.upf.edu/models/LICENSE) file in the model directory names CC BY-NC-ND 4.0 and then links to the BY-NC-SA legal code.

Either licence allows non-commercial use with attribution. Attic ships the file unmodified, so the NoDerivatives term isn't triggered, and the ShareAlike term would only bind an adapted model.

**(a) Distribution on Play of a free app.**
- CC defines NonCommercial as "not primarily intended for or directed towards commercial advantage or monetary compensation" ([BY-NC-SA 4.0 legal code](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode.en)).
- CC says the test doesn't depend on the type of user and that it can't advise on edge cases ([CC FAQ](https://creativecommons.org/faq/)).
- Attic is free, with no ads, purchases or accounts. Play charges no service fee on free apps ([Developer Distribution Agreement](https://play.google/developer-distribution-agreement.html) §3.7).
- Distributing it this way is defensible as non-commercial, but CC gives no ruling on it.
- The Distribution Agreement makes the developer warrant the right to distribute third-party material (§11.2). Adding ads, a paid tier or donations-for-features would break the NC term.

**(b) Combining it with GPLv3 code.**
- The FSF lists CC's NC licences as non-free because they restrict selling copies ([licence list](https://www.gnu.org/licenses/license-list.html)). They are not GPL-compatible.
- The model is a data file that the app loads at run time; no GPL code is compiled into it. The GPL treats separate works distributed together as an aggregate and doesn't extend to them ([GPL FAQ, mere aggregation](https://www.gnu.org/licenses/gpl-faq.en.html#MereAggregation); GPLv3 §5).
- The FSF has not ruled on ML weights specifically.
- The consequence: anyone can redistribute or sell Attic's code under the GPL, but not the model, so a redistributed APK that includes the model stays non-commercial.

**(c) F-Droid.**
- The NonFreeAssets anti-feature covers assets "under a license that restricts commercial usage or making derivative works", naming CC NC and ND ([Anti-Features](https://f-droid.org/docs/Anti-Features/)).
- The Inclusion Policy lets non-functional assets such as artwork and fonts use NC licences if they allow redistribution ([Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/)). It doesn't say how a functional asset like a model is treated.
- Expect the NonFreeAssets label at minimum, and possibly a request for a build without the model.

**Choices for the owner:**
- Ask MTG (mtg-info@upf.edu) to confirm in writing which licence applies and that a free, ad-free Play listing counts as non-commercial.
- Or ask MTG for a licence to ship the model with an open-source app.
- Or plan a build flavour without the model (radio off) for F-Droid.

## F-Droid notes

- F-Droid's `UpdateCheckData` can read `attic.versionCode` and `attic.versionName` from `gradle.properties`.
- Without the upload key, `assembleRelease` signs with the debug key. F-Droid signs builds itself, so its recipe will need an unsigned release, for example a `prebuild` step that removes the debug fallback.

## Owner actions, in the order they block release

1. Run `scripts/make-upload-key.sh`, add its four lines to `~/.gradle/gradle.properties`, raise `attic.versionCode` if needed, and run `./gradlew bundleRelease`. Back up the keystore and its password.
2. In Play Console, create the app: name "Attic", free, app rather than game. Opt in to Play App Signing.
3. Turn on GitHub Pages so `PRIVACY.md` is published at `https://srivathsanvenkateswaran.github.io/attic/privacy.html` (see `site/README.md`).
4. Under App content, fill in these forms. Play won't send any release, including a closed test, to review until they are complete:
   - privacy policy URL: `https://srivathsanvenkateswaran.github.io/attic/privacy.html`
   - ads: none
   - app access, with the note about putting music files on the phone
   - content rating questionnaire
   - target audience: 13+
   - Data safety, from `data-safety.md`
   - foreground service declarations, from the text above, after recording the two videos
   - the government, financial, health and news declarations: all no
5. Write the store listing: short and full description, a 512 × 512 icon, a 1024 × 500 feature graphic, and phone screenshots that use covers you have rights to.
6. Upload the AAB to a closed testing track. Opt in at least 12 testers and keep them for 14 consecutive days (personal accounts created after 13 November 2023).
7. Apply for production access, then promote the release.
8. Decide on the model licence before production, from the choices above.
