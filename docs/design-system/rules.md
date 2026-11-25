# Rules

How a screen is built in both styles, what things are called, and the accessibility and performance
rules this project has measured its way to. Values are in [tokens.md](tokens.md) and components in
[components.md](components.md).

Contents: [adding a screen](#adding-a-screen-in-both-styles) · [naming and copy](#naming-and-copy) ·
[accessibility](#accessibility) · [performance](#performance) · [before you open a pull request](#before-you-open-a-pull-request)

## Adding a screen in both styles

FigCraft: [`Attic Expressive.figcraft`][fx] › Screens and › Flow overview;
[`Attic Comfort.figcraft`][fc] › Screens and › Flow overview

Attic is drawn in Expressive or in Comfort, and a user can switch between them in Settings while
the app is running. Everything that is not drawing is written once, in
[`ui/model/`][model-dir], and each style draws the same state its own way. That folder is the seam.

### How the pieces fit

| Piece | Where | What it does |
|---|---|---|
| The style setting | `attic.settings.uiStyle`, a [`UiStyle`][uistyle] (`Expressive` or `Comfort`) | Holds the user's choice |
| `LocalUiStyle` and `AtticTheme` | Provided once in [`MainActivity`][main-activity]; defined in [`ui/Theme.kt`][theme] | `LocalUiStyle` says which style to draw. `AtticTheme(mode, style)` sets `MaterialTheme` to that style's colours, type and shapes, and provides `LocalIsDark`, `LocalComfortColors`, `LocalComfortType` and `LocalExpressiveType` |
| The model | `remember…Model(…)` in [`ui/model/`][model-dir], returning [`UiModel<State, Actions>`][uimodel] | Reads the app's data and builds the state to show and the actions the drawing may ask for. It knows nothing about how anything looks |
| The screen function | One small file in `ui/`, such as [`HomeScreen.kt`][home-screen] | Calls the model, then calls the current style's drawing |
| The drawing | `Expressive…` in `ui/expressive/`, `Comfort…` in `ui/comfort/` | Takes `(state, actions)` and draws with its own kit |
| The shell | [`MainShell`][approot-shell] in `ui/AppRoot.kt`, then `ExpressiveShell` or `ComfortShell` | Places the destinations, the mini player and the overlays |

Home shows the whole pattern. [`HomeModel.kt`][home-model] defines `HomeState`, `HomeActions` and
`rememberHomeModel`. The screen function is ten lines:

```kotlin
@Composable
fun HomeScreen(library: Library) {
    val (state, actions) = rememberHomeModel(library)
    when (LocalUiStyle.current) {
        UiStyle.Expressive -> ExpressiveHome(state, actions)
        UiStyle.Comfort -> ComfortHome(state, actions)
    }
}
```

[`ExpressiveHome(state, actions)`][x-home] and [`ComfortHome(state, actions)`][c-home] then lay the
same sections out differently: a carousel and connected buttons in one, a hero title and grouped
cards in the other.

### Steps

1. **Write the model.** Add `ui/model/<Name>Model.kt` with a state class, an actions class and a
   `remember<Name>Model(…)` function that returns `UiModel(state, actions)`.
   - Mark the state `@Immutable` and build it from the app's flows with `collectAsStateWithLifecycle`.
   - Actions are plain lambdas. Navigation goes through `LocalUi.current` ([`UiActions`][approot-ui]);
     a link in state is a [`Destination`][uimodel], opened with `ui.open(destination)`.
   - Anything slow is done off the main thread, and the model only collects the result. `HomeModel`
     does this for Instant mixes, which [`RadioController.refreshSpeedDial`][radio-speed-dial]
     picks on `Dispatchers.Default`.
   - Playback position is not state. A drawing that needs it polls a
     [`PlaybackClock`][uimodel] at the rate it needs ([`rememberPosition`][components]).
2. **Draw it twice.** Add `ui/expressive/Expressive<Name>.kt` and `ui/comfort/Comfort<Name>.kt`, each
   a composable taking `(state, actions)`. Use only that style's kit
   ([components.md](components.md)), and take every label from `strings.xml`.
3. **Add the screen function** in `ui/<Name>Screen.kt`, switching on `LocalUiStyle.current` as above.
   The `when` has no `else`, so adding a third style would fail to compile until every screen
   handles it.
4. **Register the route** in [`AppNavHost`][approot-nav] and add an `open…` function to `UiActions`.
5. **Leave room for the dock.** Pad a scrolling screen's end with `LocalBottomPadding.current`.
6. **Draw the frames.** Add the screen and its states to the Screens page of both FigCraft files,
   Light and Dark, following ["Proposing a design change"][design-readme] in `design/README.md`, and
   update [`design/COVERAGE.md`][coverage].
7. **Look at it.** Run it on a phone in both styles and both themes. `design/README.md` describes
   the JVM reference renders, which need no phone.

### What keeps the seam clean

- **A model never imports a style.** No file in `ui/model/` imports `ui.expressive` or `ui.comfort`.
- **A drawing never reaches the app.** No file in `ui/expressive/` or `ui/comfort/` reads
  `LocalContext.current.attic` or `LocalUi.current`. Everything arrives through `state` and
  `actions`, which is also what lets the kit gallery and the reference renders draw a screen with
  made-up data.
- **Kit components take plain data and lambdas**, never a model class.
- **State that must survive a style switch lives outside the drawing.** `LibraryScreen` opens the
  playlist name dialog itself, after the `when`, so the name typed so far is kept when the style
  changes ([`LibraryScreen.kt`][library-screen]). The shell's destinations, mini player and overlays
  are `movableContentOf`, created once in `MainShell` and handed to the style's shell as
  [`ShellSlots`][approot-slots], so a live switch moves them with their state.
- **Sheets and dialogs follow the same split.** Their models are in
  [`SheetModels.kt`][sheet-models], their entry points in [`ui/Sheets.kt`][sheets], their drawings in
  `ExpressiveSheets.kt` and `ComfortSheets.kt`.
- **Overlays close before the back stack pops.** A screen with its own `BackHandler` disables it
  while `LocalOverlayCovered.current` is true ([`AppRoot.kt`][approot-ui]).

## Naming and copy

FigCraft: [`Attic Expressive.figcraft`][fx] › Screens; [`Attic Comfort.figcraft`][fc] › Screens.
Every frame there uses the names below.

### The names things have

[`BRIEF.md`][brief] fixed one vocabulary for both styles, replacing the names the first version of
the app had borrowed from a streaming product. Use the right-hand column everywhere a user reads.

| Not this | This | Why |
|---|---|---|
| "Your Library" | "Library" | |
| "Liked Songs" | "Favourites" | |
| A plus in a circle that turns into a check | A heart, outline to filled | The plus-to-check was the most recognisable borrowed control |
| "Add to liked songs" / "Remove from liked songs" | "Add to favourites" / "Remove from favourites" | |
| "Wrapped", "Your {year} Wrapped" | "Recap", "Your {year} Recap" | |
| "Play your story" | "Play recap" | |
| "Downloaded" (the filter that shows all songs) | "Songs" | Every song is a local file; nothing is downloaded |
| A downloaded badge on every row | Removed | Decoration with no state behind it |
| "What do you want to listen to?" | "Search your music" | |
| "Start browsing" | "Browse" | |
| "Speed dial" | "Instant mixes" | A tap starts radio from that song; the name should say so |
| "Popular" / "Popular releases" (artist) | "Top songs" / "Albums" | Offline, "popular" means most played by you |
| "Your top artists" | "Top artists" | |
| "Show lyrics" | "Open lyrics" | |
| "Let's find something for your playlist" | "Add songs to this playlist" | |
| "Give your playlist a name" | "Name this playlist" | |

Some class and field names in `ui/model/` still carry the old words (`WrappedModel`,
`HomeState.speedDial`, `Destination.Liked`). They are internal. The strings a user sees are the
ones above.

### The two styles' names

The styles are called **Expressive** and **Comfort**, in the app, in documents and in commit
messages. Never name either after another company's product or design language, and never name
another music product in anything a user sees. The mock folders under
`docs/design/2026-11-21-ui-directions/` have internal names from the round that produced them; quote
them only as file paths.

### Strings

Every label comes from [`res/values/strings.xml`][strings] through `stringResource` or
`pluralStringResource`. Do not write a literal in a drawing.

- Names are `<surface>_<what>`: `home_instant_mixes`, `library_filter_songs`, `queue_up_next`.
- Shared words have no prefix: `play`, `pause`, `previous`, `next`, `back`, `settings`, `cancel`,
  `create`, `rename`, `close`, `favourites`, `favourite_add`, `favourite_remove`.
- Content descriptions end in `_cd`: `now_playing_close_cd`, `more_options_for_cd`.
- Counts are plurals with the number passed as `%1$s`, formatted with grouping by the caller:
  `pluralStringResource(R.plurals.song_count, n, NumberFormat.getIntegerInstance().format(n))`.
- Durations are `duration_hours_minutes` or `duration_minutes`, joined to a count by
  `songs_and_duration`.

### Writing copy

`BRIEF.md`'s writing rule for the mocks applies to the app's copy too: plain and specific, saying
what the thing does, with no marketing adjectives. The kits fix the shape of the shortest texts:

- An empty state is a title and one sentence, with an optional action (`EmptyState` in both kits).
- A message is one line of text with no action (`ExpressiveSnackbar`, Comfort's `Toast`).

### Demo content

Mocks, FigCraft frames, screenshots and tests use the invented demo library in
`app/src/storeShots` (artists, albums, songs, lyrics and covers). Never a real album cover, a real
artist or real lyrics.

## Accessibility

FigCraft: [`Attic Expressive.figcraft`][fx] › Colour and › Components / Actions;
[`Attic Comfort.figcraft`][fc] › Colour and › Components / Actions. Both files have an
`Accessibility` annotation category for notes on a frame.

### Contrast

Text on a cover-coloured surface holds 4.5:1 whatever the cover is, and unit tests check it. A new
text-on-surface pairing on a player surface must be one of the tested pairs, or be added to them.

**Expressive.** [`ArtSchemeTest.textRolesReachAaContrastOnTheirContainers`][art-test] builds the
cover scheme for 223 seeds (a hue sweep at the strengths `coverTint` produces, greys, white, black
and loud primaries), in dark and light, and requires 4.5:1 for every pair in
[`ArtScheme.textPairs`][art-pairs]:

| Content | On |
|---|---|
| onPrimary, onSecondary, onTertiary | primary, secondary, tertiary |
| onPrimaryContainer, onSecondaryContainer, onTertiaryContainer | their containers |
| onSurface | surface, surfaceBright, every surfaceContainer step, and secondaryContainer |
| onSurfaceVariant | surface, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest |
| primary (as text or an icon) | surface, surfaceContainer, surfaceContainerHigh |
| inverseOnSurface | inverseSurface |

It works because each role has a fixed tone, and contrast between two colours depends on their
tones alone. The same test file checks that every role lands on its tone.

Outside the player, Expressive draws in the wallpaper's scheme, whose contrast is the system's.
`onSurfaceVariant` on `surfaceContainerLow` (a sheet) and `outline` used as text are not in the
tested pairs.

**Comfort.** [`ComfortTintTest`][tint-test] requires 4.5:1 for every pair in
[`ComfortTint.textPairs`][tint-pairs], first for the plain palette in both themes
(`theUntintedPaletteKeepsTextAtAa`), then for 1,145 seeds, dark and light, tinted and washed
(`textRolesHoldAaForEveryCoverInBothThemes`). The seeds are the sixteen fixture tints, a hue sweep
at 20 strengths, primaries, greys and 400 random colours.

| Content | On |
|---|---|
| on, on2, accentInk | ground, card, raised |
| on, on2 | fill |
| on | fill2, accentSoft, miniBase, miniFill |
| onAccent | accent |
| onAccentSoft | accentSoft |
| miniInk2 | miniBase, miniFill |

`on3` is the exception. It is below 4.5:1 in the plain palette by design (4.3:1 on black, 2.8:1 on
white), and `quietInkIsNeverWeakerThanTheKitsOwn` only holds tinting to never making it worse. Use
`on3` for hints, placeholders and trailing glyphs, never for text someone has to read.

Rules that follow from the pairs:

- In Comfort's light theme, amber as text or an icon is `accentInk`. `accent` on white is a fill
  colour, not an ink.
- On Comfort's mini player the second line is `miniInk2`, not `on2`, which falls to 3.6:1 over a
  cover-coloured fill.
- Titles stay in `on` (Comfort) or `onSurface` (Expressive), neither of which a cover can weaken.

### Touch targets

The floor is 48dp. The kits meet it in different ways:

| Control | Target | How |
|---|---|---|
| Expressive icon button, toggle, heart, small play control | 48dp around a 40 or 36dp container | `minimumInteractiveComponentSize()` in [`Buttons.kt`][x-buttons] and [`PlayControls.kt`][x-play] |
| Expressive chips (32dp tall) | 48dp | Material's `FilterChip` |
| Expressive row handles and more buttons | 48dp | [`RowDragHandle`][x-lists], `RowMoreButton` |
| Comfort icon button, tonal button, drag handle | 48dp (54dp for a large tonal button) | `Modifier.size(48.dp)` in [`Buttons.kt`][c-buttons] and [`Rows.kt`][c-rows] |
| Comfort buttons | 48dp or more, including the 36dp-tall small button | Material's `Button` inside `ComfortButton` keeps its minimum touch size |
| Comfort sub-tabs | 48dp tall | [`SubTabs`][c-bars] |
| Rows | 64dp or more (52dp for a Comfort sheet row, 50dp for a menu row) | The whole row is the target |

Some controls are drawn smaller than 48dp and do not extend their target. In Comfort a filter
chip is 36dp tall, a tab in the dock 44dp and the small play button 44dp. In Expressive a connected
segment is 40dp tall (`expressive/KIT.md` says 48dp; the code gives the segment itself no larger
target). They follow the mocks. Do not add new ones below 48dp, and prefer
`minimumInteractiveComponentSize()` on anything new.

### TalkBack

- **Describe the button, not the icon.** [`SymbolIcon`][x-symbols] and `ComfortIcon` take a
  `contentDescription` only for an icon that stands alone. The glyph's own text is cleared from the
  semantics tree, so an icon inside a button says nothing and the button speaks.
- **Every icon-only button has a description.** `ExpressiveIconButton`, `ExpressiveToggleButton`,
  `ComfortIconButton` and `TonalIconButton` take `contentDescription` as their second argument.
- **Kit components that know what they are describe themselves** from the shared strings:

  | Component | Says |
  |---|---|
  | `PlayButton` (both) | "Play" / "Pause" |
  | `HeartButton` (both) | "Add to favourites" / "Remove from favourites" |
  | `RowMoreButton`, `SongMoreButton` | "More options for {title}" |
  | `RowDragHandle`, `SongDragHandle` | "Drag {title} to reorder" |
  | `RowAddToggle`, `SongAddButton` | "Add {title}" / "Remove {title}" |
  | The search field's clear button | `search_clear_cd` |
  | `ExpressiveExtendedFab` | Its label, because Material's extended FAB leaves its button node with no text |
  | `FavouritesPill` | Its `description` ("Favourites") in place of the bare count |

- **Give a control the role it has.** Buttons are `Role.Button`; an Expressive toggle is
  `Role.Checkbox`; a switch is `Role.Switch`; radios and connected segments are `Role.RadioButton`
  inside `selectableGroup()`; Comfort's tabs and sub-tabs are `Role.Tab`.
- **Say the state.** Comfort's `TonalIconButton` has a `stateDescription` parameter for a toggle
  whose description does not say whether it is on.
- **In a settings row, the row is the control.** `SetSwitchRow` and `SetRadioRow` put the toggle on
  the row and pass the inner switch or radio a null handler, so TalkBack reads one item.
- **Titles are headings.** Comfort's `Hero`, `Bar` and the permission title set `heading()`.
- **Progress is exposed.** Progress bars set `progressBarRangeInfo`; both seek bars also take
  `setProgress`, so they can be adjusted without dragging.
- **A decorative picture has no description.** Covers pass `null`; the row or tile they sit in
  carries the title.

Recap cards are the one fixed-size drawing: a 360 × 640 canvas at font scale 1, so the saved image
matches the story. Everything else uses `sp` and follows the system font size.

## Performance

FigCraft: [`Attic Expressive.figcraft`][fx] › Elevation and motion;
[`Attic Comfort.figcraft`][fc] › Elevation and motion. The design files cannot show cost. Their
`Reference render differs` annotations mark where the JVM renders, which draw no shadows, differ
from a phone.

Each rule below was learned from a measurement on this project. The commit is the evidence.

### Judge speed on release builds

Compose runs far slower in a debug build, so a debug build tells you nothing about scrolling or
start-up. The figures in the [README's Performance section][readme] were measured with release
builds on one phone, as medians over repeated runs (20 cold starts and 6 scrolls of the full song
list per style): 2.2% janky frames in Expressive and 2.5% in Comfort, and a cold start of 0.25 s
and 0.24 s (commit `6d44948`). Measure the same way before and after a change, and say in the
commit message what was measured.

### No path clip on anything that moves

A clip to a rounded rectangle is cheap. A clip to any other outline is a path clip, and the
RenderThread stencils it again on every frame the content moves, for every draw inside it.

- Comfort's covers, mosaics and icon boxes each clipped to a superellipse. Its lists took about 3 ms
  more GPU time a frame than Expressive's and missed more frames: 3.1% janky in Library › Songs and
  3.0% on Home, against 2.2% and 2.0% before either style existed (commit `8907a66`).
- The cost grows with the number of points in the path. With every superellipse drawn at 720
  points, scrolling Library › Songs spent a quarter of its RenderThread time in convexity checks and
  stencil operations. [`SuperellipseShape.samplesFor`][c-shapes] now gives an outline only as many
  points as its size needs to stay within 0.1px of the curve.

So: do not `Modifier.clip(…)` a list item, a tile or anything animated to a superellipse, a cookie
or any other `Outline.Generic` shape. Fill the shape, or bake it into the picture (next rule).

### Bake the superellipse into covers

[`SuperellipseMask`][c-mask] in `ui/comfort/CoverMask.kt` is a Coil transformation that crops a
cover to the exact size it is drawn at and cuts the shape into the bitmap once, when the picture
loads. Coil's memory cache keeps the result, keyed by the mask and the size, so a cover in a
scrolling list is one plain image with no clip. Comfort's `Cover` and `Mosaic` do this by
themselves; a mosaic bakes each quarter's part of the outline.

- **The edge is the clip's own.** [`superellipseCoverage`][c-mask] samples each edge px at the four
  standard 4× multisample positions, which is how the GPU drew the clip, so a baked cover lands on
  the same edge values as the clipped one it replaced. `CoverMaskTest.coverageMatchesTheGpuClip`
  pins it.
- **Keep baked covers as ordinary bitmaps.** As hardware bitmaps each cover was a buffer of its
  own that the GPU driver looked up on every bind: texture draws took 18% of RenderThread time
  scrolling Home, against 7% as ordinary bitmaps, and janky frames fell from 2.6% to 2.4%
  (commit `e883dce`).
- **Icon boxes fill the shape** from a cached alpha mask instead of clipping to it
  ([`Artwork.kt`][c-artwork]).
- **Anything scaled as a layer keeps the clip**, because a baked edge is drawn at one size. Now
  Playing's paused cover and the Recap cards turn baking off with `LocalBakedCovers`.

### Leave undrawn what the dock covers

Below its 36dp fade ([`DockFade`][c-dock-fade]) Comfort's dock is opaque ground, so nothing a
destination draws there can be seen. [`ComfortShell`][c-shell-clip] clips the destinations to the
line where the fade ends. What reaches the screen is unchanged, and a scrolled frame of
Library › Songs took about 0.8 ms less GPU time on the test phone (commit `1b5d9a8`).

### Nothing slow on the main thread

- **Typefaces.** Every axis setting of a variable font is its own Android typeface, and making one
  costs a few milliseconds. Compose keeps only the last 16 it resolved, and the styles use more, so
  the same typefaces were made again and again on the main thread: about 32 ms of Expressive's
  start-up. Fonts now load through [`variableFont()`][variable-fonts], which keeps every typeface it
  makes, and `AtticApp` warms the current style's type scale and common icons on a background
  thread (commit `93ca88e`). Build a new font family with `variableFont`, not `Font(resId, …)`.
- **The widget.** An `APPWIDGET_UPDATE` reaches the main thread at every cold start, ahead of the
  first frame. The widget read its `SharedPreferences` there and held the thread for about 2.8 ms;
  it now reads them on the IO dispatcher (commit `25ffc15`).
- **Cover colours.** `rememberArtColor` loads the thumbnail and runs Palette on `Dispatchers.IO`
  and caches the result ([`Art.kt`][art]).
- **Instant mixes** are picked on `Dispatchers.Default`
  ([`RadioController.refreshSpeedDial`][radio-speed-dial]); [`HomeModel.kt`][home-model] only
  collects the list.
- **Widget bitmaps.** Hand `RemoteViews` an immutable bitmap, and only when the state changed. A
  mutable one is copied into shared memory for every layout it is set on, 17 to 25 MB at launch
  (commit `bde4e37`; the note is in [`PlayerWidget.kt`][widget]).

### Keep the first frame bare

The app's first frame is the loading screen. Expressive drew its morphing loading indicator there:
about 25 ms to compose, plus a path stencil and shader compiles, all before the first frame reached
the screen. On the test phone the library is ready about 90 ms later, so the indicator only
flashed. Both styles now show the bare ground and add the indicator only if loading is still going
after 400 ms ([`rememberLoadingIndicatorShown`][components]); Expressive's first frame fell from
85 ms to 33 ms (commit `ac1ae82`).

### Ship the baseline profile

[`app/src/main/baseline-prof.txt`][baseline] lists all of the app's own code, so it is compiled
ahead of time on install. The libraries ship rules for their own code, but nothing covered the
app's: every Attic method ran interpreted on its first calls at each cold start. With the profile,
activity creation fell from 76.5 ms to 62.8 ms and the first frame from 38 ms to 31 ms, in 6 traced
cold starts of Expressive (commit `4546f13`). The rule covers the whole `app.attic.player` package,
so new code under it is included without an edit.

### Read moving values when drawing

Playback position is not pushed as state. A progress composable takes its fraction as a lambda
(`progress = { fraction }`) and reads it in the draw phase, and
[`rememberPosition`][components] polls the [`PlaybackClock`][uimodel] only while it is told the
screen is active. A seek bar that moves therefore redraws itself without recomposing the screen
around it.

## Before you open a pull request

- The change is drawn in both styles, in light and dark.
- Each drawing uses only its own kit and takes `(state, actions)`.
- Every label is a string resource, with the names in [the table above](#the-names-things-have).
- Every icon-only button has a `contentDescription`, and new targets are at least 48dp.
- A new text-on-surface pairing on a player surface is in `ArtScheme.textPairs` or
  `ComfortTint.textPairs`.
- Nothing that scrolls or animates is clipped to a path.
- If a token changed: `scripts/design_tokens.sh` has been run and [tokens.md](tokens.md) updated
  ([README.md](README.md#keeping-the-docs-in-step-with-the-code)).
- If the look changed: the FigCraft frames are updated, as `design/README.md` describes.
- A claim about speed comes with a release-build measurement.

[fx]: ../../design/Attic%20Expressive.figcraft
[fc]: ../../design/Attic%20Comfort.figcraft
[design-readme]: ../../design/README.md#proposing-a-design-change
[coverage]: ../../design/COVERAGE.md
[brief]: ../design/2026-11-21-ui-directions/BRIEF.md
[readme]: ../../README.md#performance
[baseline]: ../../app/src/main/baseline-prof.txt
[strings]: ../../app/src/main/res/values/strings.xml
[uistyle]: ../../app/src/main/java/app/attic/player/data/AppSettings.kt#L12
[main-activity]: ../../app/src/main/java/app/attic/player/MainActivity.kt#L46
[theme]: ../../app/src/main/java/app/attic/player/ui/Theme.kt#L26
[model-dir]: ../../app/src/main/java/app/attic/player/ui/model/
[uimodel]: ../../app/src/main/java/app/attic/player/ui/model/UiModel.kt
[home-model]: ../../app/src/main/java/app/attic/player/ui/model/HomeModel.kt
[sheet-models]: ../../app/src/main/java/app/attic/player/ui/model/SheetModels.kt
[home-screen]: ../../app/src/main/java/app/attic/player/ui/HomeScreen.kt
[library-screen]: ../../app/src/main/java/app/attic/player/ui/LibraryScreen.kt
[sheets]: ../../app/src/main/java/app/attic/player/ui/Sheets.kt
[components]: ../../app/src/main/java/app/attic/player/ui/Components.kt
[art]: ../../app/src/main/java/app/attic/player/ui/Art.kt#L112
[variable-fonts]: ../../app/src/main/java/app/attic/player/ui/VariableFonts.kt#L36
[radio-speed-dial]: ../../app/src/main/java/app/attic/player/radio/RadioController.kt#L68
[approot-ui]: ../../app/src/main/java/app/attic/player/ui/AppRoot.kt#L46
[approot-slots]: ../../app/src/main/java/app/attic/player/ui/AppRoot.kt#L108
[approot-shell]: ../../app/src/main/java/app/attic/player/ui/AppRoot.kt#L159
[approot-nav]: ../../app/src/main/java/app/attic/player/ui/AppRoot.kt#L220
[x-home]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveHome.kt#L49
[x-buttons]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt
[x-play]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt
[x-lists]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L302
[x-symbols]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt#L178
[art-test]: ../../app/src/test/java/app/attic/player/ui/expressive/ArtSchemeTest.kt
[art-pairs]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L149
[c-home]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortHome.kt#L51
[c-buttons]: ../../app/src/main/java/app/attic/player/ui/comfort/Buttons.kt
[c-rows]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt
[c-bars]: ../../app/src/main/java/app/attic/player/ui/comfort/Bars.kt#L175
[c-shapes]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L97
[c-dock-fade]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt#L52
[c-shell-clip]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShell.kt#L106
[c-mask]: ../../app/src/main/java/app/attic/player/ui/comfort/CoverMask.kt#L39
[c-artwork]: ../../app/src/main/java/app/attic/player/ui/comfort/Artwork.kt#L289
[tint-test]: ../../app/src/test/java/app/attic/player/ui/comfort/ComfortTintTest.kt
[tint-pairs]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L108
[widget]: ../../app/src/main/java/app/attic/player/widget/PlayerWidget.kt#L223
