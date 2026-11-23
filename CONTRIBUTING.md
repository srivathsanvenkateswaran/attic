# Contributing to Attic

Bug reports, fixes and small features are welcome. Attic was built around one phone and one
library, so reports from other phones and other libraries are especially useful.

## Reporting a bug

Open an issue with the bug template. Say which phone and Android version you have, which style
(Expressive or Comfort) and theme you were using, and what you did before it went wrong. A
screenshot or a screen recording helps more than a description.

For a security problem, don't open an issue; follow [SECURITY.md](SECURITY.md).

## Building

You need JDK 17 and the Android SDK with platform 37.

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # release APK, for checking speed
```

Compose runs far slower in a debug build, so judge scrolling and start-up on a release build. The
release build only targets 64-bit ARM (`arm64-v8a`), because the song analysis runtime ships as
native code.

Attic reads music from every folder Android's media library indexes.
[`LibraryScope.kt`](app/src/main/java/app/attic/player/data/LibraryScope.kt) decides which files
are songs and where each library root starts. The four folder names that become playlists
(`English`, `Hindi`, `Tamil`, `Phonk`) are hard-coded in
[`LibraryBuilder.kt`](app/src/main/java/app/attic/player/data/LibraryBuilder.kt).

## Making a change

- Open an issue before a large change, so we can agree on the approach first.
- Keep a pull request to one change. Commits are small, with a plain imperative subject:
  "Keep the queue position after a rescan", not "Fixed stuff".
- Add or update unit tests in `app/src/test/` when you change logic outside the UI.
- Run `./gradlew :app:testDebugUnitTest assembleDebug` before you push. CI runs the same.
- A change that affects what you see needs it in both styles, Expressive and Comfort, in light
  and dark. Each style draws only from its own kit; read `KIT.md` in
  `app/src/main/java/app/attic/player/ui/expressive/` or `.../ui/comfort/` first. Put before and
  after screenshots in the pull request.
- User-facing text goes in `res/values/strings.xml`.

## Design files

Both styles are drawn as FigCraft files in [`design/`](design/README.md): every screen and state
in light and dark, the components and the tokens. Open them in FigCraft, which is free and open
source. To propose a change to how something looks, follow "Proposing a design change" in
[`design/README.md`](design/README.md). A code change that alters the look should update the
matching frames in the same pull request.

## Names and look

Attic has two styles, called **Expressive** and **Comfort**. Don't name them, or anything else in
the app, after another company's product or platform, and don't copy another app's screens,
icons, fonts or wording. Fonts and icons must be under an open licence an Android app can bundle
(OFL or Apache 2.0).

## Privacy

Attic's promise is that playback and song analysis never touch the network, and that lyrics
lookups send only a song's title, artist, album and length to lrclib.net. A change that sends
anything else anywhere, adds a network call, or adds an analytics, crash reporting or ads library
won't be merged. If you change what lyrics lookups send, update [PRIVACY.md](PRIVACY.md) in the
same pull request and run `python3 scripts/render_privacy_page.py`.

## Licence

Attic is licensed under the [GNU General Public License v3.0 or later](LICENSE). By sending a pull
request you agree that your contribution is licensed under the same terms. Third-party assets keep
their own licences, listed in the README; don't add one whose licence forbids redistribution.

## Code of conduct

Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).
