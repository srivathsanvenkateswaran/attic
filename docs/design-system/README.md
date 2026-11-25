# Attic design system

Attic is an offline music player for Android, drawn in either of its styles, **Expressive** and
**Comfort**. A user picks one on first run and can switch in Settings while the app is running.
These pages describe both: the values each is built from, the components to use, and the rules for
adding to them.

| Page | What it answers |
|---|---|
| [tokens.md](tokens.md) | What is the value? Colour roles in light and dark, colour taken from album art, type, icons, spacing, shapes, elevation, motion, the brand mark, the widget's colours and the website's palette. Ends with a list of where other sources disagree with the code |
| [components.md](components.md) | What do I use? For each kind of control, the composable in each style and its file, with short do and don't examples |
| [rules.md](rules.md) | How do I add to it? Building a screen in both styles, naming and copy, accessibility, performance |
| `tokens.json` | The same token values as data. Written by `scripts/design_tokens.sh`; never edited by hand |

## The two styles

| | Expressive | Comfort |
|---|---|---|
| Colour | The phone's wallpaper scheme; no fixed palette | A fixed palette on a black or plaster ground, with one accent, amber |
| Colour from album art | `FromArt(seed)`: a whole Material scheme rebuilt from the cover's hue | `Tinted(seed, wash)`: the accent, and with `wash` the surfaces, moved toward the cover |
| Font | Google Sans Flex, with emphasised styles that are heavier and fully rounded | Onest |
| Icons | Material Symbols Rounded at weight 400 | The same font at weight 300 |
| Covers | Rounded squares whose radius follows their size | Superellipses |
| Artists | A nine-lobed cookie | A rounder superellipse |
| Lists | Separate cells 2dp apart, 20dp corners on the group's outside | One card with 26dp corners, rows split by hairlines |
| The bottom of the screen | A navigation bar with the mini player floating above it | A floating dock holding the mini player and the tabs |
| Progress | A wave while something is running | A thick capsule that fills |
| On means | Squared off and filled with `primary` | Filled with the accent |
| Motion | Springs; position and shape overshoot | Short tweens; nothing overshoots |
| Separating surfaces | Tone | A card on the ground; shadows only on things that float |

## Where each style lives

| | Expressive | Comfort |
|---|---|---|
| Code | [`ui/expressive/`][x-dir] | [`ui/comfort/`][c-dir] |
| Kit reference, beside the code | [`expressive/KIT.md`][x-kit] | [`comfort/KIT.md`][c-kit] |
| Theme entry | [`AtticTheme(mode, UiStyle.Expressive)`][theme] | [`AtticTheme(mode, UiStyle.Comfort)`][theme] |
| Token objects | `MaterialTheme`, plus `Expressive.type`, `Expressive.motion`, `Expressive.shapes` ([`ExpressiveTheme.kt`][x-theme]) | `Comfort.colors`, `Comfort.type`, `Comfort.shapes` ([`ComfortTheme.kt`][c-theme]) |
| Design file | [`design/Attic Expressive.figcraft`][fx] | [`design/Attic Comfort.figcraft`][fc] |
| Mocks from the round that chose it | `docs/design/2026-11-21-ui-directions/pixel/` | `docs/design/2026-11-21-ui-directions/oneui/` |

Shared by both, in [`ui/`][ui-dir]:

| File | Holds |
|---|---|
| [`Theme.kt`][theme] | `AtticTheme`, `LocalUiStyle`, `LocalIsDark` |
| [`model/`][model-dir] | Every screen's state and actions, written once for both styles |
| [`Art.kt`][art] | Loading covers, and the cover colour both styles start from |
| [`VariableFonts.kt`][fonts] | `variableFont`, which both styles' fonts load through |
| [`Components.kt`][components] | `LocalBottomPadding`, the loading-indicator delay, small helpers |
| [`expressive/Cover.kt`][x-cover], [`expressive/Symbols.kt`][x-symbols], [`expressive/Mark.kt`][x-mark] | The `Art` type, the `Symbol` enum and the mark's vector, which Comfort borrows |

The mock folders keep the internal names of the directions round. In anything a user reads, the
styles are Expressive and Comfort and nothing else.

## How these pages relate to the code, KIT.md, FigCraft and the mocks

The code is the truth. Where any other source disagrees with it, the code is right and the other
source is out of date.

| Source | What it is for | When to read it |
|---|---|---|
| The Kotlin code | Every value and every component | Always the final word |
| These pages | Both styles side by side: the values, what to use, the rules. Part of [tokens.md](tokens.md) is checked against the code by a test | To find a value, to choose a component, before adding a screen |
| `KIT.md`, one per style | The full reference for one kit: each mock class and the composable that draws it, every argument, the debug gallery's commands, known differences from the mock | When working inside one style and you need a component's exact arguments |
| The FigCraft files | How every screen and state looks, in Light and Dark, with components, variables and a wired prototype | To see a screen, and to propose a change to how something looks ([`design/README.md`][design-readme]) |
| The HTML mocks and their `kit.css` | The record of the round in which the styles were designed. The kits' class names (`.row`, `.sheet`, `--r-float`) come from here | For history, and to understand a class name in a doc comment |
| [`BRIEF.md`][brief], [`CURRENT-UI.md`][current-ui] | The naming table, and every screen, state and label the app has | When writing copy or checking that a screen keeps a capability |

Every section of the three pages names the FigCraft page that draws it, written as
"FigCraft: `Attic Comfort.figcraft` › Colour". The pages are the same in both files:

| FigCraft page | Documented in |
|---|---|
| Cover, Changelog | [`design/README.md`][design-readme] |
| Colour | [tokens.md](tokens.md#colour-in-expressive), [tokens.md](tokens.md#colour-in-comfort) |
| Type | [tokens.md](tokens.md#type) |
| Spacing, radii and shapes | [tokens.md](tokens.md#spacing), [tokens.md](tokens.md#shapes-and-radii) |
| Elevation and motion | [tokens.md](tokens.md#elevation), [tokens.md](tokens.md#motion) |
| Icons | [tokens.md](tokens.md#icons) |
| Components / Actions | [components.md](components.md#buttons), [progress](components.md#progress), [switches and radios](components.md#switches-and-radios), [chips](components.md#chips-and-one-of-n-choices) |
| Components / Lists and cards | [components.md](components.md#lists-rows-and-groups), [covers, tiles and shelves](components.md#covers-tiles-and-shelves), [settings rows](components.md#settings-rows) |
| Components / Navigation | [components.md](components.md#the-navigation-bar-expressive-and-the-dock-comfort), [top bars](components.md#top-bars), [mini player](components.md#mini-player), [search field](components.md#search-field) |
| Components / Playback | [components.md](components.md#play-controls), [Now Playing](components.md#now-playing) |
| Components / Overlays | [components.md](components.md#sheets), [menus](components.md#menus), [dialogs](components.md#dialogs), [messages](components.md#snackbar-expressive-and-toast-comfort) |
| Components / States | [components.md](components.md#empty-loading-and-permission-states) |
| Components / Recap | [components.md](components.md#recap) |
| Components / System, System surfaces | [components.md](components.md#widget), [tokens.md](tokens.md#brand-mark), [tokens.md](tokens.md#widget-and-window-colours) |
| Screens, Flow overview | [rules.md](rules.md#adding-a-screen-in-both-styles) |

## Keeping the docs in step with the code

[`DesignTokensTest`][test] runs with the app's unit tests and fails when these pages drift from the
code. It reads the values from the Kotlin objects themselves ([`DesignTokens.kt`][test-tokens]), so
nothing is typed twice.

| Check | What fails it |
|---|---|
| `tokensMdMatchesCode` | A checked row in [tokens.md](tokens.md) is missing, appears twice, or does not contain the code's value; a Comfort colour row gives light before dark; a row names a token the code no longer has |
| `tokensJsonMatchesCode` | `tokens.json` is not exactly what the code produces |
| `tokenListsCoverEveryProperty` | A token was added to `ComfortColors`, either type scale, either shapes object or `ExpressiveMotion` and is not yet listed for checking |
| `colourResourcesFollowComfortPalette` | A `comfort_…` colour in `res/values/colors.xml` or `res/values-night/colors.xml` no longer matches `Comfort.colors` |

### Which rows are checked

A checked row is a table row in tokens.md whose first cell is one of these expressions in
backticks. Each must start exactly one row.

| First cell | Covers | The row must contain |
|---|---|---|
| `Comfort.colors.<name>` | Every `ComfortColors` property except `dark` | The dark value, then the light value, each as `` `#RRGGBB` `` in capitals, or `` `#RRGGBBAA` `` when alpha is below 1 |
| `Comfort.type.<name>` | Every style in `ComfortTypography` | Size and line as `40/46`, the weight as `700`, and the tracking as `-0.025em` unless it is zero |
| `Expressive.type.<name>` | Every style in `ExpressiveTypography`, emphasised ones included | Size and line as `57/64`, the weight as `400`, and the tracking as `-0.25sp` unless it is zero |
| `ComfortShapes.<name>` | The four radii, `LargeArtFrom` and the three superellipses | A radius as `30dp`; a superellipse's exponent as `n = 4` |
| `ExpressiveShapes.<name>` | The eight corners, `GroupGap` and `GroupSpacing` | The value as `4dp` |
| `Expressive.motion.<name>` | The six springs | The damping ratio and the stiffness as plain numbers, `0.6` and `800` |
| `TintSpec` | Comfort's tint animation | `450 ms` |

Numbers are written as the code writes them, without a trailing `.0`. A value must stand alone in
its cell: `700` is not found inside `1700`.

Every other table and all the prose are not checked. They are kept right by reading the code when
it changes, so each value links to the file that defines it.

### After changing a token

1. Change the Kotlin.
2. Run `scripts/design_tokens.sh`. It rewrites `tokens.json` from the code, then runs
   `DesignTokensTest`, which lists every row of tokens.md that is now wrong. It needs JDK 17.
3. Fix those rows, and any unchecked table or sentence nearby that mentions the old value.
4. If you added a token, add it to the lists in [`DesignTokens.kt`][test-tokens] and give it a row.
5. Update the style's `KIT.md` if it mentions the value.
6. Update the FigCraft variable, text style or effect style, following
   ["Proposing a design change"][design-change].

To run the check alone:

```sh
./gradlew :app:testDebugUnitTest --tests 'app.attic.player.ui.DesignTokensTest'
```

### After changing a component

1. Update the component's row in [components.md](components.md) if its name, file or purpose
   changed.
2. Update the style's `KIT.md`, which lists every argument.
3. Update the component in the FigCraft file, not a screen's copy of it.
4. If you found a place where `KIT.md`, a mock or a FigCraft file disagrees with the code and you
   are not fixing it now, add it to
   [the list in tokens.md](tokens.md#where-kitmd-the-mocks-and-figcraft-differ-from-the-code).

[fx]: ../../design/Attic%20Expressive.figcraft
[fc]: ../../design/Attic%20Comfort.figcraft
[design-readme]: ../../design/README.md
[design-change]: ../../design/README.md#proposing-a-design-change
[brief]: ../design/2026-11-21-ui-directions/BRIEF.md
[current-ui]: ../design/2026-11-21-ui-directions/CURRENT-UI.md
[test]: ../../app/src/test/java/app/attic/player/ui/DesignTokensTest.kt
[test-tokens]: ../../app/src/test/java/app/attic/player/ui/DesignTokens.kt
[ui-dir]: ../../app/src/main/java/app/attic/player/ui/
[x-dir]: ../../app/src/main/java/app/attic/player/ui/expressive/
[c-dir]: ../../app/src/main/java/app/attic/player/ui/comfort/
[model-dir]: ../../app/src/main/java/app/attic/player/ui/model/
[x-kit]: ../../app/src/main/java/app/attic/player/ui/expressive/KIT.md
[c-kit]: ../../app/src/main/java/app/attic/player/ui/comfort/KIT.md
[theme]: ../../app/src/main/java/app/attic/player/ui/Theme.kt#L32
[x-theme]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveTheme.kt#L35
[c-theme]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortTheme.kt#L17
[art]: ../../app/src/main/java/app/attic/player/ui/Art.kt
[fonts]: ../../app/src/main/java/app/attic/player/ui/VariableFonts.kt
[components]: ../../app/src/main/java/app/attic/player/ui/Components.kt
[x-cover]: ../../app/src/main/java/app/attic/player/ui/expressive/Cover.kt
[x-symbols]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt
[x-mark]: ../../app/src/main/java/app/attic/player/ui/expressive/Mark.kt
