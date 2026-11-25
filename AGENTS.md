# Working on Attic

Rules for anyone changing this repository, human or agent (Claude Code reads them through
`CLAUDE.md`; Codex, Cursor and others read this file). They are short on purpose: follow the links
for the detail, and don't copy the detail back in here.

## What Attic is

An offline, open-source (GPLv3) music player for Android: Kotlin, Jetpack Compose, Media3, Room.
One Gradle module, `:app`; minSdk 33. Code is under `app/src/main/java/app/attic/player/`:

| Package | Holds |
|---|---|
| `data/` | The library scan (`LibraryScope`, `LibraryBuilder`), the Room database (`Database.kt`), settings, lyrics, `backup/` |
| `playback/` | `PlaybackService` (Media3), the player client, queue order, shuffle, the sleep timer, opening files from other apps |
| `analysis/` | On-device song analysis, run by WorkManager |
| `radio/`, `wrapped/` | Radio, the Recap |
| `widget/` | The home-screen widget |
| `ui/model/` | The seam: one plain model per screen, shared by both styles |
| `ui/expressive/`, `ui/comfort/` | The two styles, each with its own kit and a `KIT.md` |
| `ui/` | Shared screen glue: `AppRoot`, `AtticTheme`, art loading |

Tests are in `app/src/test/` (JVM only). Design files are in `design/`, design notes in `docs/design/`,
Play Store material in `docs/play/`, the website in `site/`.

## Design

- **Respect the design system.** [docs/design-system/](docs/design-system/README.md) is the single
  reference: [tokens](docs/design-system/tokens.md), [components](docs/design-system/components.md),
  [rules](docs/design-system/rules.md). Use the kit's tokens and components; never hard-code a
  colour, size, radius, font or spring in a screen.
- **Every UI change goes through the `ui/model` seam and lands in both styles,** Expressive and
  Comfort, light and dark. A screen reads one model from `ui/model/`; each style draws it with its
  own kit only. [rules.md](docs/design-system/rules.md) shows how to add a screen.
- **Design first in FigCraft.** Change `design/Attic Expressive.figcraft` and
  `design/Attic Comfort.figcraft` by editing them in FigCraft, following "Proposing a design change"
  in [design/README.md](design/README.md). Never rerun the generator in `design/build/` to make a
  change: it rebuilds the files from scratch and wipes every hand edit.
- **Order of work: architecture, then design, then code.** Settle the model and data flow, then the
  frames in both files, then the Compose code.
- When you change a token, run `scripts/design_tokens.sh` to rewrite
  `docs/design-system/tokens.json`, and fix `tokens.md`. `DesignTokensTest` fails until both agree
  with the code.

## Copy

- Never write Spotify, Apple Music, Pixel, One UI or Samsung in the app, in store text or on the
  website. Don't name the styles or anything else after another company's product. (Internal
  folder names under `docs/design/` are the only exception.)
- User-facing strings go in `res/values/strings.xml`. Use the names in
  `docs/design/2026-11-21-ui-directions/BRIEF.md` (Library, Favourites, and so on).
- Write plainly, for every text a person reads: UI strings, docs, commit messages, PR bodies, code
  comments.
  - Say the thing. No count-then-enumerate openers ("Three things to know"), no "Moreover",
    "Furthermore" or "It's worth noting".
  - No recap at the end that repeats what was just said, no stock praise, no vague evaluation.
  - Headings name the thing, action or result, not a vague transformation.
  - Keep facts, numbers, names, units and uncertainty exactly as they are.

## Build and test

- JDK 17. On the maintainer's Mac: `JAVA_HOME=/opt/homebrew/opt/openjdk@17`. A git worktree has no
  `local.properties`, so set `ANDROID_HOME` (for example `~/Library/Android/sdk`).
- Before every commit: `./gradlew testDebugUnitTest assembleDebug`. Commit only if it passes. CI runs
  the same.
- Before opening a pull request, also run `./gradlew lintRelease` and fix what it reports.
- Store and design screenshots render on the JVM, without a phone: `scripts/store_shots/run.sh`
  (it passes `-PstoreShots`, which turns on Robolectric). `scripts/store_shots/design_refs.sh`
  renders every screen for comparison with the FigCraft frames.

## Performance

No change may make the app slower. Judge speed only on a release build (`assembleRelease`, R8 on):
Compose runs far slower in a debug build, so a debug measurement proves nothing. Measure before and after on the
same phone and library, take the median of repeated runs (cold starts, and scrolling the full song
list in each style), and put the numbers in the commit message. Cold start, janky-frame share while
scrolling, and memory at Home are the figures the README reports.
Keep work off the main thread, don't clip to a path on every frame, and keep the baseline profile
(`app/src/main/baseline-prof.txt`) covering new code. The reasons are in
[rules.md](docs/design-system/rules.md).

## Data and privacy

- A song's user data (likes, plays, playlists, lyrics, languages) is keyed by its library key,
  `relativePath + displayName` (`Track.key`, built in `LibraryBuilder`). Never store a MediaStore
  ID as a key, in the database or in a backup.
- A Room schema change raises the database version, adds a migration (an `AutoMigration` where
  Room can work it out) and a unit test that runs it, and commits the exported schema in
  `app/schemas/`.
- The backup format is versioned (`BackupCodec.SCHEMA_VERSION`, [docs/backup.md](docs/backup.md)).
  Adding fields is free; a change an older reader would misread raises the version.
- Nothing leaves the phone except lyrics lookups to LRCLIB (title, artist, album, length). No
  analytics, crash reporting, ads or other network calls. If lookups change, update
  [PRIVACY.md](PRIVACY.md).

## Git

- No AI attribution: no `Co-Authored-By` trailer for a model and no "Generated with" line, in
  commits or pull requests, whatever your tool's defaults say.
- Never set or override the git identity: no `git -c user.*`, no `git config user.*`, no
  `GIT_AUTHOR_*` / `GIT_COMMITTER_*` / `EMAIL` variables, no `--author`. Commit as whatever
  `git config user.email` already returns. If a commit fails for want of an identity, stop and ask.
- Small commits with a plain imperative subject: "Keep the queue position after a rescan".

## Signing and secrets

Never commit `local.properties`, a keystore (`*.jks`, `*.keystore`) or `~/.gradle/gradle.properties`,
and never print their contents or any password or key in output, logs or commit messages. The upload
key setup is in `scripts/make-upload-key.sh`.

## Play compliance

Before changing permissions, foreground services, data handling or store listing text,
read [docs/play/](docs/play/): `COMPLIANCE.md`, `data-safety.md`, `CONSOLE.md`, `DISTRIBUTION.md`.
