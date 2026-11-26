# Attic for iOS

A SwiftUI port of the Android app, in its Expressive style only. It plays music files from folders
you pick in Files, offline, with no account. The one network call is the LRCLIB lyrics lookup
(title, artist, album and length), the same as on Android.

## Build and run

Needs Xcode 27 with the iOS 26 simulator runtime, and xcodegen (`brew install xcodegen`). Simulator
only: the project has no signing team.

```sh
ios/scripts/build.sh          # generates Attic.xcodeproj, builds, runs the unit tests
ios/scripts/build.sh build    # build only
```

The project file is generated from `project.yml` and is not in git. DerivedData goes to
`ios/.build`. The script runs at most three builds at once on one Mac (`ATTIC_GATE=1` adds a fourth
lane for the merge gate) and uses one simulator, "iPhone Air": builds run in parallel, test runs take
turns on it.

To run it, install `ios/.build/Build/Products/Debug-iphonesimulator/Attic.app` on a booted
simulator and launch `app.attic.ios`. On first run it asks for a music folder through the Files
picker; put music in the simulator's Files app (On My iPhone) first.

Debug builds take launch arguments that skip the picker and open a screen, for screenshots
(`App/DebugLaunch.swift`):

```sh
xcrun simctl launch "iPhone Air" app.attic.ios -atticFolder /path/to/Music -atticPlay 5 \
  -atticScreen nowPlaying -atticTheme dark
```

`-kitGallery` shows every kit component instead of the app.

## Layout

| Path | Holds |
|---|---|
| `project.yml` | The app target (Attic, `app.attic.ios`, iOS 26.0) and the AtticTests bundle |
| `DESIGN.md` | The Expressive tokens, components and screens for iOS |
| `Attic/` | The app entry point |
| `AtticTests/` | Unit tests |
| `Packages/AtticModules/Sources/AtticCore` | Models, library keys, the play queue, LRC parsing, the service protocols (`Services.swift`) |
| `.../AtticLibrary` | Folder bookmarks, scanning, tags and artwork, the SwiftData store, the LRCLIB client, `LibraryService` |
| `.../AtticPlayback` | `AudioPlayer` (AVQueuePlayer), `NowPlayingCenter` (lock screen and Control Centre) |
| `.../AtticKit` | Expressive tokens and components |
| `.../AtticUI` | `AppModel`, `Router`, the shell, one model per screen (`Models/`) and the screens (`Screens/`) |
| `scripts/` | `build.sh`, and `strings.py`, which writes `AtticUI/App/Strings.swift` from the Android strings plus `strings-ios.txt` |

A song's saved data (likes, plays, playlists, lyrics) is keyed by its relative path plus file name,
the same rule as Android's `Track.key`. The relative path starts with the picked folder's name, so
picking a folder called `Music` gives the same keys as Android's `Music/` files.

## What works

- Picking one or more folders in Files, kept as security-scoped bookmarks. Scanning mp3, m4a, aac,
  flac, wav, aif and alac files, with tags and covers from AVFoundation plus a small FLAC reader
  (Vorbis comments, embedded pictures) and cover.jpg-style sidecar images. A rescan on launch, on
  pull to refresh and from Settings.
- SwiftData storage for the library and every piece of user data (schema `AtticSchemaV1`, with a
  migration plan to add versions to).
- Playback with AVQueuePlayer: a queue with shuffle and repeat, the next song preloaded so songs run
  into each other without a gap, background audio, interruptions and unplugged headphones,
  the lock screen and Control Centre (title, artist, album, cover, position, and play, pause, next,
  previous and seek).
- Screens: first run, Home, Library (playlists, albums, artists, songs, grid or list, sorting),
  Album, Artist, Playlist, Folder, Favourites, Search, the mini player, Now Playing (colours from
  the cover), Queue (reorder, swipe to remove, clear), Lyrics (synced lines from LRCLIB), Settings
  (theme, music folders, about) and the open-source licences.
- Playlists (create, add, reorder, rename, delete), favourites, play counts and recently played.
- Light and dark, Dynamic Type up to accessibility size 2, VoiceOver labels.

## What doesn't, yet

- The Files picker on first run and in Settings was not driven in the simulator (nothing here can
  tap); the folder was added with the `-atticFolder` launch argument, which calls the same code.
- Checked only in the simulator. The lock screen itself was not looked at: this Mac has no
  Simulator app, so it was verified through the system's media service log (Attic registered as
  the now-playing app, with title, cover and a moving position in the background) and unit tests.
- No iPad layout, Recap, song analysis, mood mixes, radio, equaliser, widgets, CarPlay or backup.
- The iPhone strings that differ from Android are in `scripts/strings-ios.txt` and need a read.
- No UI tests; the unit tests cover keys, grouping, the queue, the store, the scanner, tags, LRC
  parsing, the LRCLIB client, the engine and the lock-screen bridge.
