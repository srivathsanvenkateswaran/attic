# Android Auto

Written 2026-11-23. Attic shows up in Android Auto as a media app. The car draws every screen. Attic supplies a browse tree, search, voice requests and the now-playing controls from its playback service. Nothing here has run on a head unit yet. The [Desktop Head Unit steps](#test-on-the-desktop-head-unit) below are the next check, and the guideline table marks what that test has to confirm.

## What the car shows

```
root
├── Home        Shuffle all songs; "Instant mixes" (each starts radio); "Recently played" (12 songs)
├── Favourites  songs, newest first                       (only when the car allows five tabs)
├── Playlists   Favourites (when there are four tabs), your playlists, folder playlists → songs
├── Albums      grid of albums, by name → songs
└── Artists     grid of artists, by name → songs, most recently played first
```

- **Tabs.** Android Auto asks for at most four tabs, and the car media docs say to assume four when a car sends no limit ([content hierarchy](https://developer.android.com/training/cars/media/create-media-browser/content-hierarchy)). Favourites then becomes the first row of Playlists. A car that allows five gets it as its own tab. Tab icons are white Material Symbols vectors (`res/drawable/ic_auto_*.xml`).
- **Layout.** Albums and Artists are grids. Every list of songs, and Playlists, is a list. Home and search results use group headings. The root sets these as content-style hints in `playback/CarBrowser.kt`. The manifest has no content-style setting; the car reads the hints from the browse tree.
- **Covers.** The car takes artwork only as a `content://` or `android.resource://` URI ([media artwork](https://developer.android.com/training/cars/media/create-media-browser/media-artwork)). `playback/ArtProvider.kt` cuts a 480 px JPEG from the song's file the first time a cover is asked for, and keeps it until the next launch. The provider is not exported. The session grants read access, URI by URI, with `grantUriPermission(controller.packageName, uri, FLAG_GRANT_READ_URI_PERMISSION)` (`playback/ArtGrants.kt`), to the controllers connected to it:
  - a browse or search result's covers, to the browser that asked, before the result is returned (`onGetLibraryRoot`, `onGetChildren`, `onGetItem`, `onGetSearchResult`);
  - the covers of the playing song and the 10 after it, to every connected controller, when a controller connects, when the song changes and when the queue changes. That covers the car's now-playing screen and queue, and the system's media controls.

  Attic's own package is never granted anything: the app, the widget and Media3's notification load covers in Attic's process, where the provider is readable without a grant (the notification goes through the session's `BitmapLoader`, which runs there). Media3's placeholder name for a controller it can't identify gets nothing. Grants last until the phone restarts, and each package and URI is granted once per launch.
- **Playing a row.** A song plays its whole list from that song, under the same "Playing from" name the app's own screen uses. History, Recap, radio continuation and the saved queue treat it like the same list played in the app. An Instant mix starts radio from its song, as the Home tile does. "Shuffle all songs" plays every song shuffled.
- **Search.** The car's search button looks through songs, artists, albums, your playlists and folder playlists. It ignores case and accents. A song found by search plays the search results from that song, as in the app.
- **Voice.** "Play X on Attic" tries a playlist or folder name, an artist, an album, then a song title, preferring an exact name to a partial one. "X by Y" narrows to that artist, and "favourites" or "liked songs" plays Favourites. A spoken song plays alone, and radio carries on from it when the queue ends. "Play music on Attic" carries on with the queue that is loaded, then the saved queue, then every song shuffled. When nothing matches, the car shows "Couldn't find "X" in your music" and the queue stays as it was. The same resolver handles "play X on Attic" said to the phone, through `MEDIA_PLAY_FROM_SEARCH`.
- **Without audio access** the car shows "Attic can't see your music yet. When it's safe, open Attic on your phone and allow access to music." This message is guideline `VI-1`.

The tree is built in `playback/BrowseTree.kt` and voice requests in `playback/VoiceSearch.kt`. Both are plain Kotlin with JVM tests (`BrowseTreeTest`, `VoiceSearchTest`).

## Car app quality guidelines

From [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality), last updated 2026-09-14. A media app must meet the "Car optimized" tier to be accepted on Play. The table lists the guidelines that apply to a media app on a phone running Android Auto. **Meets** means the code does what the guideline asks. **Check on DHU** means the code is written for it but no head unit has confirmed it.

| ID | Requirement | Attic |
|---|---|---|
| PC-1 | No features outside the app types meant for cars | Meets. The car gets music browsing and playback only. |
| EP-1 | Works as expected or as described in the Play listing | Meets in code. **Owner:** the store listing doesn't mention Android Auto yet. |
| EP-2 | Relaunching restores state as closely as possible | Meets. The service puts the saved queue back, paused, when it starts. Check on DHU. |
| SA-1 | No animated elements | Meets. The car draws every screen. |
| AD-1 | No text ads beyond advertiser or product name | Meets. No ads. |
| IU-1 | No images apart from album art and drawer icons | Meets. Covers and tab icons only. |
| VI-1 | Asks the user to look at the phone only when it's safe | Meets. The permission message says "When it's safe". |
| ST-1 | No automatically scrolling text | Meets. The car draws all text. |
| VC-1 | Supports Gemini and Google Assistant voice commands | Check on DHU. Voice requests are resolved in `onSetMediaItems` and `onAddMediaItems`. The session accepts every controller, as the docs require for assistants. |
| DR-1 | App buttons respond within 2 s | Check on DHU. Shuffle and heart act at once in the service. |
| DR-2 | Launches within 10 s | Check on DHU. A cold start scans MediaStore before the first list shows. The scan is quick on the owner's 740 songs; a much larger library hasn't been timed. |
| DR-3 | Content loads within 10 s | Check on DHU. Home re-reads Instant mixes each time it opens. |
| VD-1 | Icons and colours meet Android Auto contrast | Meets. Every icon is a white vector the car tints. |
| VD-2 | White icon sets the system colours | Meets. Tab icons and the shuffle and heart icons are white. |
| VD-3 | Colours the system can adjust for readability | Meets, unchecked on a head unit. Android Auto styles playback controls with the `colorAccent` of the style named by `com.google.android.gms.car.application.theme` ([configure manifest](https://developer.android.com/training/cars/media/configure-manifest)). That is `Theme.Attic.Car` in `res/values/themes.xml`: `android:colorAccent` and `android:colorPrimary` set to Headroom terracotta `#C6533A`. No activity uses that style, so neither in-app style changes. |
| GB-1 | Disabled elements don't work | Meets. The session offers only the commands it handles. |
| NA-1 | No ads in notifications | Meets. |
| IN-1 | Notifications only when relevant to the driver | Meets. The only other notification is song analysis, which runs while charging or after "Analyse now" in Settings. |
| MA-1 | No autoplay on start or without a user action | Meets. Restoring the queue never prepares or plays it. Check on DHU after a force stop. |

Rows that don't apply: `EP-4`, `PE-1` and `DL-2` are for Android Automotive OS, which Attic doesn't target. Others cover video, games, browsers or templated media apps.

### Gaps and risks

- **Nothing has run on a head unit.** Every "Check on DHU" row is open until the steps below pass.
- **VD-3 colours.** Set, but not yet seen on the DHU. Step 9's playback controls should show terracotta, or a colour the car adjusted from it for contrast.
- **The listing.** EP-1 judges the app against its Play listing, which says nothing about cars yet.
- **Cover grants on a head unit.** The provider is no longer exported, so a car shows covers only through the grants above. Nothing has checked them on the DHU yet: step 3's "Instant mixes" covers, step 5's album grid and step 9's now-playing cover are the test. A car that loads covers for queue items more than 10 ahead would show those without art.
- **Updates wait for the car review.** The car review blocks every later release of the app until it passes ([distribute](https://developer.android.com/training/cars/distribute)). The FAQ on the quality page says the same.
- **The permission message uses Media3's "authentication expired" error.** Media3 copies only that code and the parental-control code to the playback state the car reads. The resolution button under it appears only on cars that run Android themselves ([Android Automotive OS](https://developer.android.com/training/cars/media/automotive-os)).

## Play Console

From [distribute](https://developer.android.com/training/cars/distribute), last updated 2026-10-06, and [Play Console help](https://support.google.com/googleplay/android-developer/answer/13295490).

1. The manifest already carries `com.google.android.gms.car.application`, pointing at `res/xml/automotive_app_desc.xml` (`<uses name="media"/>`) ([media apps for Auto](https://developer.android.com/training/cars/media/auto)).
2. **Owner:** in Play Console, open Advanced settings, then Form factors, then "Add form factor", and choose Android Auto.
3. **Owner:** release the bundle to a testing track. Android Auto has no track of its own; it ships inside the phone app.
4. Review depends on the track:

   | Track | Car review |
   |---|---|
   | Internal app sharing, internal testing | None |
   | Closed testing | Runs, doesn't block |
   | Open testing, production | Blocks the release |

   No review time is published. The result arrives by email.
5. **Owner:** mention Android Auto in the store listing before the review, for EP-1.

The current Developer Program Policy has no Android Auto section ([policy](https://support.google.com/googleplay/android-developer/answer/17190352)). The changes this year are in the guidelines: `VC-1` now names Gemini (May 2026), and `EP-4` asks Automotive OS apps for screenshots (August 2026).

## Test on the Desktop Head Unit

Run these against the phone over adb from a Mac. The serial below is the owner's phone.

**Set up once**

1. In Android Studio, open SDK Manager, then SDK Tools, and install "Android Auto Desktop Head Unit Emulator". It installs to `~/Library/Android/sdk/extras/google/auto/`. Then run `chmod +x ~/Library/Android/sdk/extras/google/auto/desktop-head-unit` ([DHU](https://developer.android.com/training/cars/testing/dhu)).
2. On the phone, update Android Auto from Play. Open Settings > Apps > See all apps > Android Auto > Advanced > Additional settings in the app. Tap "Version and permission info" ten times and accept "Allow development settings?" ([testing](https://developer.android.com/training/cars/testing)).
3. In Android Auto's developer settings, turn on "Unknown sources". Android Auto needs it for apps that weren't installed from Play. The docs don't say whether the DHU does, so turn it on anyway.

**Each session**

4. Install the build: `ANDROID_SERIAL=QWBMGMS8BYBI99KJ JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew installDebug`.
5. On the phone, in Android Auto, open the overflow menu and tap "Start head unit server".
6. `adb -s QWBMGMS8BYBI99KJ forward tcp:5277 tcp:5277`
7. `cd ~/Library/Android/sdk/extras/google/auto && ./desktop-head-unit`. Add `-c` with a file from `config/` for a different screen size.
8. In another terminal, keep `adb -s QWBMGMS8BYBI99KJ logcat -s PlaybackService MediaSessionImpl MediaSessionLegacyStub AndroidRuntime` open.

**What to check, with the result to expect**

| # | Do | Expect |
|---|---|---|
| 1 | Open the app launcher on the DHU | Attic is listed with its icon |
| 2 | Open Attic | Four tabs: Home, Playlists, Albums, Artists, each with a white icon. Nothing starts playing (MA-1) |
| 3 | Home | "Shuffle all songs", then an "Instant mixes" heading with covers, then "Recently played" |
| 4 | Playlists | Favourites first with its song count, then your playlists, then folder playlists |
| 5 | Albums, then an album, then its second song | Album covers in a grid; songs in a list; the album plays from that song. On the phone, Now Playing shows the album as "Playing from" |
| 6 | Artists, then an artist | Grid of artists; the artist's songs, most recently played first |
| 7 | Home, then an Instant mix | That song plays, and the DHU queue fills with up to ten songs of radio |
| 8 | Search icon, type "par" | Results grouped under Songs, Artists, Albums and Playlists headings |
| 9 | Now playing screen | Cover, title, artist, and the shuffle and heart buttons. Heart adds the song to Favourites (check on the phone); shuffle changes its icon and reorders Up next |
| 10 | Voice. First run `say -o /tmp/play.wav --file-format=WAVE --data-format=LEI16@16000 "Play Parachutes on Attic"` (check the format against `afinfo voice/pause.wav` first), then type `mic play /tmp/play.wav` in the DHU console. Or type `mic begin` and speak | The album plays. Try an artist, a song, "my favourites" and "music" too. "Music" carries on with the loaded queue |
| 11 | Voice: "Play zzqx on Attic" | The DHU shows "Couldn't find "zzqx" in your music". The queue is unchanged |
| 12 | `restrict all` in the console, then browse | Browsing and playback still work with the driving restrictions on. Then `restrict none` |
| 13 | `night`, then `day` | Icons stay legible on both |
| 14 | `adb -s QWBMGMS8BYBI99KJ shell am force-stop app.attic.player`, then open Attic on the DHU | Tabs load within 10 s (DR-2, DR-3). The last song shows paused and doesn't start by itself (EP-2, MA-1) |
| 15 | `adb -s QWBMGMS8BYBI99KJ shell pm revoke app.attic.player android.permission.READ_MEDIA_AUDIO`, then open Attic on the DHU | The "When it's safe, open Attic on your phone" message. Restore with `pm grant` and the same arguments |
| 16 | Play to the end of an album with autoplay on | Radio continues from the last song |

**Then on the phone, for regressions**

- The notification and lock screen show the cover, and their shuffle and heart buttons work.
- The widget updates with the song and its play state.
- After `am force-stop`, a headset or Bluetooth play button resumes the saved queue.
- The sleep timer counts down and fades out.
- "Open with" from a file manager still plays the file.
- Search in the app finds the same songs as before.
