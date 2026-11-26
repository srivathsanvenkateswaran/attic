# Attic for iOS

A SwiftUI port of the Android app, in its Expressive style. Work in progress; see the status below.

## Build and run

Needs Xcode 27 with the iOS 26 simulator runtime, and xcodegen (`brew install xcodegen`).

```sh
ios/scripts/build.sh          # generates Attic.xcodeproj, builds, runs the unit tests
ios/scripts/build.sh build    # build only
```

The project file is generated from `project.yml` and is not in git. DerivedData goes to `ios/.build`.

## Layout

| Path | Holds |
|---|---|
| `project.yml` | The app target (Attic, `app.attic.ios`, iOS 26.0) and the AtticTests bundle |
| `Attic/` | The app entry point |
| `AtticTests/` | Unit tests |
| `Packages/AtticModules/Sources/AtticCore` | Models, library keys, the play queue, LRC parsing, the service protocols (`Services.swift`) |
| `.../AtticLibrary` | Folder bookmarks, scanning, tags and artwork, the SwiftData store, the LRCLIB client, `LibraryService` |
| `.../AtticPlayback` | `AudioPlayer` (AVQueuePlayer), `NowPlayingCenter` (lock screen and Control Centre) |
| `.../AtticKit` | Expressive tokens and components, see `DESIGN.md` |
| `.../AtticUI` | `AppModel`, `Router`, the shell and the screens |
