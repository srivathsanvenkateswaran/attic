# Attic design files

Attic's two UI styles are drawn here as [FigCraft](https://github.com/storytold/figcraft) design
files. They are the reference for how each screen looks, and the place to propose a change to it.

| File | Style | Code |
|---|---|---|
| `Attic Expressive.figcraft` | Expressive: colour from the phone's wallpaper and the album art, wavy progress, connected button groups, cookie shapes | `app/src/main/java/app/attic/player/ui/expressive/` |
| `Attic Comfort.figcraft` | Comfort: superellipse cards, the amber Lamplight accent, a bottom dock, the `Tinted(seed, wash)` colours from the album art | `app/src/main/java/app/attic/player/ui/comfort/` |

Both are version 1.0.0 and match the code on `ui-redesign` at 4fe4dab. The code wins: if a file and
the app disagree, the app is right and the file is out of date.

## What each file holds

The pages are the same in both files:

- **Cover** and **Changelog**: name, version, status, last update and what changed.
- **Foundations**:
  - **Colour:** variables with Light and Dark modes. Expressive's base scheme is an example wallpaper scheme. Its cover scheme is one `Color` mode per demo album, following the hue rule in `ArtScheme`. Comfort's `Tinted` tokens hold the values `Tinted.kt` computes for the example cover, with the rule in each description.
  - **Type:** text styles in the real fonts.
  - **Spacing, radii and shapes:** number variables, plus the cookie and superellipse shapes as vector components traced from the code's maths.
  - **Elevation and motion:** effect styles and the transition specs.
  - **Icons:** the 90 Material Symbols Rounded glyphs the app ships, as components.
- **Components** (Actions, Lists and cards, Navigation, Playback, Overlays, States, Recap, System): main components with variants and properties, auto layout and bound colours. Every page ends with a Dark preview.
- **Screens**: every screen and state, 411 × 914, in Light and Dark, grouped in one section per area, with the prototype wired. [COVERAGE.md](COVERAGE.md) is the checklist.
- **Flow overview**: the user-flow diagram, with labelled arrows.
- **System surfaces**: the widget sizes on a home screen, the notification shade, the lock screen, the splash and the launcher icon with its monochrome layer.

## Opening the files

1. Install FigCraft. It is free and open source (MIT or Apache 2.0): download it from
   [github.com/storytold/figcraft/releases](https://github.com/storytold/figcraft/releases) or
   [getartcraft.com/apps/figcraft](https://getartcraft.com/apps/figcraft). The files were made with
   0.18.1.
2. Install the app's fonts from `app/src/main/res/font/`: `google_sans_flex.ttf`, `onest.ttf` and
   `material_symbols_rounded.ttf`. Without them FigCraft draws text in a fallback font and lists
   the missing families.
3. Open the `.figcraft` file. Present the prototype from the Screens page; its flow starting points
   are named after the flows (First run, Play an album, Search, Settings, Recap story, Sleep timer,
   Share a song, Backup and restore and others).

### Figma

There is no `.fig` copy here, because FigCraft 0.18.1's `.fig` export doesn't carry this design
faithfully. Exported and opened again in FigCraft on 2026-11-23:

- **Expressive:** 100 of 103 Light screens differ from the original. Sheets lose their rows, and the track menu shows only an empty placeholder. Scaled Recap cards lose their scale. Every text style drops its width and roundness axes.
- **Comfort:** 40 of 96 Light screens differ, mostly in cover images inside nested components and in grid content.

FigCraft's export reported:

- variable and conditional prototype actions were left out (the theme switch in the prototype)
- Developer Mode annotations, statuses, resources, measurements and code links were left out
- text decoration looks and variable font settings were left out
- list spacing, hanging punctuation, leading trim and OpenType features were left out
- slot properties were left out, and slot layers were written as plain layers
- instance layers that don't come from their main component were left out
- preferred values of instance swap properties were left out
- text styles applied to part of a text were written as their values
- prototype device settings were left out
- spring animations other than Gentle were written as ease out
- variables' publishing, code syntax and inheritance settings were left out

To make an experimental `.fig` anyway (for example with a newer FigCraft), run
`python3 -m figbuild.figexport "../Attic Comfort.figcraft" /tmp/attic-comfort.fig` from
`design/build`. It detaches instances that hold slot content before exporting, and `--flat` also
detaches every instance on the screen pages. Open the result in FigCraft and compare it with the
original before sharing it.

## How the designs map to code

- **Screens.** Every screen frame is named `Area / State · Light` or `· Dark`. Its `Source` annotation names the Compose file and composable, for example `ui/expressive/ExpressiveHome.kt · ExpressiveHome`. Screens that match the code are marked ready for development.
- **Annotations.** Where the code differs from the HTML mocks in `docs/design/2026-11-21-ui-directions/`, a `Code differs from mock` annotation says what the code does. A `Reference render differs` annotation marks where the JVM render harness draws something differently from a phone, mainly the system-bar insets of sheets, dialogs and the Recap story.
- **Components.** Each component's description names its source file and composable. Its Code Connect snippet (Developer Mode) shows the Compose call.
- **Variables.** Each colour variable's code syntax is its Compose name, for example `MaterialTheme.colorScheme.surfaceContainerHigh` or `Comfort.colors.accent`.

| Area | Expressive (`ui/expressive/`) | Comfort (`ui/comfort/`) |
|---|---|---|
| Home | `ExpressiveHome.kt`, `ExpressiveShell.kt` | `ComfortHome.kt`, `ComfortShell.kt` |
| Search | `ExpressiveSearch.kt` | `ComfortSearch.kt` |
| Library | `ExpressiveLibrary.kt` | `ComfortLibrary.kt` |
| Album, artist, playlist, folder, Favourites | `ExpressiveCollections.kt` | `ComfortCollections.kt` |
| Now Playing | `ExpressiveNowPlaying.kt` | `ComfortNowPlaying.kt`, `NowPlayingParts.kt` |
| Queue, Lyrics | `ExpressiveQueue.kt`, `ExpressiveLyrics.kt` | `ComfortQueue.kt`, `ComfortLyrics.kt` |
| Sheets, dialogs, messages | `ExpressiveSheets.kt`, `Surfaces.kt` | `ComfortSheets.kt`, `Overlays.kt` |
| Settings, backup and restore, licences | `ExpressiveSettings.kt`, `ExpressiveLicences.kt` | `ComfortSettings.kt`, `ComfortLicences.kt` |
| Recap, its story and share images | `ExpressiveRecap.kt`, `ExpressiveRecapStory.kt`, `ExpressiveRecapCards.kt` | `ComfortRecap.kt`, `ComfortRecapStory.kt`, `ComfortRecapCards.kt` |
| First run | `ExpressiveOnboarding.kt` | `ComfortOnboarding.kt` |
| Permission, loading, empty | `ExpressiveStates.kt` | `ComfortStates.kt` |
| Widget, notification, splash, icon | `PlayerWidget.kt`, `PlaybackService.kt`, `res/layout/widget_*`, `res/mipmap-*` | the same |

Each style's `KIT.md`, next to its code, explains the kit classes the components mirror.

## Proposing a design change

1. **Open an issue** describing the change, with a sketch or a screenshot of the edited frame. Agree on it before editing the file.
2. **Edit the `.figcraft` file in FigCraft.** Change the component, not a screen's copy of it, so every screen follows. Keep colours on variables and text on text styles.
3. **Mark work in progress.** A change that isn't in the code yet is a proposal. Give the frame an annotation in the `Proposed` category saying what changes, and clear its ready-for-development status. When the code lands, remove the annotation and mark it ready again.
4. **Update the Cover and Changelog.** Add a Changelog entry and bump the version on the Cover: patch for fixes, minor for new screens or components, major for a restyle.
5. **Open one pull request per change.** A `.figcraft` file can't be merged line by line, so keep changes small and say in the description which pages and frames changed. Attach PNG exports of each changed frame, before and after, and the matching render if the code has changed too.
6. **Check against the app.** If the change goes with code, render the references (below) and compare.

### Reference renders

`scripts/store_shots/design_refs.sh` renders every screen and state of both styles, Light and Dark,
on the JVM from the demo library into `/tmp/attic-dev/design-refs/`. It needs no phone. Its
`INDEX.md` lists the keys. Screen frames use the same keys in their names on the Screens page.

To compare a frame with its render, export the frame as PNG at 2.625× and run:

```sh
cd design/build
python3 -m figbuild.compare /tmp/attic-dev/design-refs/comfort-light-home.png home.png out.png
```

This writes the reference, the design, an overlay and a difference map side by side, and prints a
score (0 means identical).

## Naming rules

- **Pages:** in the order above, sentence case.
- **Components:** `Category/Name` in title case (`Button`, `List row/Song`, `Sheet/Track menu`).
  - Variant properties are title case with title-case values: `State=Pressed`, `Size=Medium`, `Selected=True`. Booleans are `True` and `False`.
  - Other properties are named for what they change, in sentence case: `Label`, `Show subtitle`, `Leading icon`.
  - Theme is never a variant. Light and Dark come from variable modes.
- **Layers:** say what the layer is (`Cover`, `Title`, `Trailing icon`), never `Frame 12` or `Rectangle`.
- **Screens:** `Area / State · Light` and `· Dark`, for example `Search / Results · Dark`. A frame showing a whole scrolled page ends in `(full length)`.
- **Variables:** kebab case in slash groups mirroring the code (`surface/container-high`), with the code name as code syntax.
- **Text styles:** Material role names (`Title/Large`), plus the style's own groups, which the Type page documents.
- **Content:** only the invented demo library in `app/src/storeShots` (artists, albums, songs, lyrics and covers). Never real album art, real artists or real lyrics.
- **Copy:** from `res/values/strings.xml`, using the names in the BRIEF naming table: Library, Favourites, Recap, Instant mixes. User-facing text never names another company's product.

## Known differences from the app

- **Text width:** FigCraft sets Onest and Google Sans Flex slightly wider than Android does, about 1–2.5 %. Long paragraphs (lyrics, the licence text) can wrap a word earlier.
- **Recap cards:** scaled Recap cards (story thumbnails, share images, the first-run look previews) are drawn as scaled instances, as the code scales them. Their inner texts lose their text-style links.
- **Text ranges:** FigCraft can't bind a colour variable to part of a text. A few bold spans keep the paragraph's colour; the component descriptions say where.
- **Comfort's Recap Summary card:** on ui-redesign at 4fe4dab it throws on render (`ComfortRecapCards.kt`, `FitText` inside a `Row` with `IntrinsicSize.Min`). The file draws it from the code, with a `Known issue` annotation.

## How these files were made

Version 1.0.0 was generated from the code by the Python scripts in [`build/`](build/README.md),
which drive FigCraft's own automation. From now on the `.figcraft` files are the source: edit them
in FigCraft. Re-running the scripts would replace any hand edits.
