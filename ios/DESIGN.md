# Attic for iOS: design

The iOS app is a SwiftUI port of the Android app's Expressive style, for iPhone on iOS 26. This page
maps every Expressive token to SwiftUI and says how each component and screen is built. The Android
kit (`app/src/main/java/app/attic/player/ui/expressive/`, `docs/design-system/`) is the source; where
this page and the Android kit disagree on a number, this page is right for iOS and the difference is
listed under [Differences from Android](#differences-from-android).

All sizes are points. One Android dp is one point.

Contents: [rules](#rules) · [colour](#colour) · [type](#type) · [shapes](#shapes) ·
[spacing](#spacing) · [motion](#motion) · [icons](#icons) · [app structure](#app-structure) ·
[components](#components) · [screens](#screens) · [copy](#copy) · [accessibility](#accessibility) ·
[differences from Android](#differences-from-android)

## Rules

- Screens live in `AtticUI` and draw only with `AtticKit`. A screen never writes a colour, a font, a
  corner radius, a spring or a spacing number that has a token. If the kit lacks something, add it to
  the kit.
- Kit components take plain values (`String`, `Image?`, `Double`, kit enums) and closures. They never
  import `AtticCore` or read a singleton.
- Every colour comes from `@Environment(\.atticScheme)`. Never use `Color.primary`, `.secondary`,
  `Color(.systemBackground)` or an asset colour.
- Every text uses `.atticFont(...)`. Never use `.font(.body)` or a system font.
- Every icon is a `SymbolIcon`. Never use `Image(systemName:)`.
- The system navigation bar and tab bar are hidden. The kit's own bars are drawn in their place.
- Never write the name of another company's product in code, strings or docs.

## Colour

### The app scheme

Android takes the app scheme from the wallpaper. iOS has no such scheme, so the app scheme is fixed:
the blue scheme the store screenshots are drawn in (seed `#2A4174`, the scheme's light primary).
`AtticScheme.app(dark:)` returns it.

| Role | Light | Dark |
|---|---|---|
| `primary` | `#2A4174` | `#B7CAFF` |
| `onPrimary` | `#FFFFFF` | `#00143B` |
| `primaryContainer` | `#5E73A9` | `#7A90C8` |
| `onPrimaryContainer` | `#FFFFFF` | `#000000` |
| `inversePrimary` | `#B7CAFF` | `#2A4174` |
| `secondary` | `#3C4255` | `#C4CAE1` |
| `onSecondary` | `#FFFFFF` | `#0F1626` |
| `secondaryContainer` | `#6E7488` | `#8A90A5` |
| `onSecondaryContainer` | `#FFFFFF` | `#000000` |
| `tertiary` | `#553A55` | `#E4BFE2` |
| `onTertiary` | `#FFFFFF` | `#240D26` |
| `tertiaryContainer` | `#8A6A89` | `#A886A6` |
| `onTertiaryContainer` | `#FFFFFF` | `#000000` |
| `error` | `#B3261E` | `#F2B8B5` |
| `onError` | `#FFFFFF` | `#601410` |
| `errorContainer` | `#F9DEDC` | `#8C1D18` |
| `onErrorContainer` | `#410E0B` | `#F9DEDC` |
| `surface` | `#FAF8FF` | `#121318` |
| `onSurface` | `#1A1B20` | `#FCFAFF` |
| `onSurfaceVariant` | `#40434B` | `#C9CAD4` |
| `surfaceDim` | `#DAD9E0` | `#121318` |
| `surfaceBright` | `#FAF8FF` | `#38393F` |
| `surfaceContainerLowest` | `#FFFFFF` | `#0C0E13` |
| `surfaceContainerLow` | `#F4F3FA` | `#1A1B20` |
| `surfaceContainer` | `#EEEDF4` | `#1E1F25` |
| `surfaceContainerHigh` | `#E8E7EF` | `#282A2F` |
| `surfaceContainerHighest` | `#E2E2E9` | `#33343A` |
| `outline` | `#5D5F67` | `#A1A2AC` |
| `outlineVariant` | `#797A83` | `#81838C` |
| `inverseSurface` | `#121318` | `#FAF8FF` |
| `inverseOnSurface` | `#FCFAFF` | `#1A1B20` |
| `scrim` | `#000000` | `#000000` |

### What each role is for

| Role | Used for |
|---|---|
| `surface` | Screens |
| `surfaceContainerLow` | Sheets |
| `surfaceContainer` | Group cells, the tab bar, a scrolled app bar, info cards, Now Playing's tools row |
| `surfaceContainerHigh` | Pills, the search field, the mini player, unselected segments, group cells inside a sheet |
| `surfaceContainerHighest` | A row being dragged, a cover with no art, unselected segments on a group cell |
| `primary` / `onPrimary` | The one filled action on a screen, toggles that are on, the selected segment, the play control, the progress wave and handle |
| `primaryContainer` / `onPrimaryContainer` | The "New playlist" button, the contained loading indicator |
| `secondaryContainer` / `onSecondaryContainer` | The tab indicator, a selected chip, the playing row, skip buttons, tonal buttons, empty-state art |
| `tertiaryContainer` / `onTertiaryContainer` | Favourites and nothing else |
| `onSurfaceVariant` | Second lines, standard icon buttons, trailing glyphs |
| `outline` | The sheet handle, lyric lines already sung |
| `outlineVariant` | Outlined button and chip borders, dividers, the unplayed part of the progress track |
| `error` / `errorContainer` | A destructive button; what a swipe to remove uncovers |
| `inverseSurface` / `inverseOnSurface` | The snackbar |

### Colour from the cover

`AtticScheme.from(seed: UIColor, dark: Bool)` builds every role from a cover's colour. It is a Swift
port of Android's `ArtScheme` and `Hct`: only the seed's HCT hue is read, and each role has a fixed
tone (CIE L\*) and chroma (CAM16), so a dull cover and a loud one give the same contrast. Tertiary
uses the hue turned 60°. Chroma drops until the colour fits sRGB; tone is kept. The error roles and
`scrim` come from the app scheme.

| Role | Palette | Dark tone | Dark chroma | Light tone | Light chroma |
|---|---|--:|--:|--:|--:|
| primary | seed hue | 83.8 | 34.6 | 38.5 | 43.6 |
| onPrimary | seed hue | 14.2 | 25.0 | 98.8 | 4.5 |
| primaryContainer | seed hue | 32.7 | 33.8 | 84.9 | 28.4 |
| onPrimaryContainer | seed hue | 91.9 | 18.1 | 22.3 | 35.0 |
| inversePrimary | seed hue | 39.7 | 42.1 | 83.8 | 34.6 |
| secondary | seed hue | 81.4 | 20.5 | 37.4 | 23.0 |
| onSecondary | seed hue | 15.3 | 17.6 | 98.8 | 4.5 |
| secondaryContainer | seed hue | 25.8 | 23.1 | 87.8 | 18.6 |
| onSecondaryContainer | seed hue | 89.6 | 16.7 | 24.6 | 23.1 |
| tertiary | hue + 60° | 91.9 | 21.0 | 35.0 | 35.5 |
| onTertiary | hue + 60° | 28.1 | 32.2 | 98.8 | 4.5 |
| tertiaryContainer | hue + 60° | 88.4 | 26.1 | 88.4 | 26.1 |
| onTertiaryContainer | hue + 60° | 23.4 | 32.2 | 23.4 | 32.2 |
| surface | seed hue | 5.7 | 13.6 | 96.3 | 8.5 |
| surfaceDim | seed hue | 5.7 | 13.6 | 87.2 | 13.0 |
| surfaceBright | seed hue | 25.8 | 17.6 | 96.3 | 8.5 |
| surfaceContainerLowest | seed hue | 2.5 | 11.4 | 99.4 | 3.6 |
| surfaceContainerLow | seed hue | 8.9 | 15.2 | 94.2 | 10.2 |
| surfaceContainer | seed hue | 12.4 | 16.8 | 92.2 | 12.2 |
| surfaceContainerHigh | seed hue | 15.9 | 18.3 | 90.1 | 13.7 |
| surfaceContainerHighest | seed hue | 20.0 | 19.5 | 87.8 | 15.3 |
| onSurface | seed hue | 93.0 | 9.2 | 9.5 | 13.7 |
| inverseSurface | seed hue | 93.0 | 9.2 | 14.2 | 13.7 |
| inverseOnSurface | seed hue | 18.8 | 13.7 | 94.2 | 9.2 |
| onSurfaceVariant | seed hue | 79.1 | 15.0 | 30.4 | 17.5 |
| outline | seed hue | 58.2 | 15.3 | 51.3 | 17.3 |
| outlineVariant | seed hue | 30.4 | 17.5 | 79.1 | 16.9 |

Check: seed `#F07858` in dark gives `primary` `#FFC3B4`, `secondaryContainer` `#5A3328` and `surface`
`#200E09`, within one step per channel.

Cover colour is used on player surfaces only: the mini player, Now Playing, the queue, lyrics, and
the header block of an album, artist or playlist. The list under a header stays on the app scheme.

### Using the scheme in SwiftUI

```swift
// Once, at the root of the app. Registers the fonts, follows light and dark, sets the screen colour.
RootView().atticTheme(.system)            // AtticThemeMode: .system, .light, .dark

// In any view.
@Environment(\.atticScheme) private var scheme
Text("Library").foregroundStyle(scheme.onSurface)

// A player surface. nil keeps the scheme around it (a song with no art).
NowPlayingScreen().fromArt(seed: coverColour)     // UIColor?
```

- `.atticTheme(_:)` sets `\.atticScheme` to `AtticScheme.app(dark:)`, applies
  `.preferredColorScheme` for `.light` and `.dark`, and sets `scheme.onSurface` as the default
  foreground and `bodyLarge` as the default font.
- `.fromArt(seed:)` replaces `\.atticScheme` for its content with `AtticScheme.from(seed:dark:)`,
  following the colour scheme around it. When the seed changes, every role moves on
  `AtticMotion.slowEffects`.
- The seed is the screen's business: `AtticUI` works out one colour per cover (the average colour of
  the cover scaled to 1×1 is enough) and caches it by song key.

## Type

One variable font, Google Sans Flex (`Resources/Fonts/google_sans_flex.ttf`), with weight 300 to 800,
width 50 to 100, roundness (`ROND`) 0 to 100 and optical size 12 to 48. `AtticKit.registerFonts()`
registers it; `.atticTheme` calls that for you.

```swift
Text(title).atticFont(.titleLarge)
Text(title).atticFont(.bodyLarge.weight(.medium))     // a row title
Text(time).atticFont(.labelMedium.tabular())          // figures that must not jiggle
Text(title).atticFont(.headlineLargeEmphasized.tracking(-0.3))
```

`.atticFont` sets the font, tracking and line height, and scales all three with Dynamic Type
(`UIFontMetrics` for the text style in the last column).

| Style | Size/line | Weight | Tracking | Optical size | Round | Scales as |
|---|---|--:|--:|--:|---|---|
| `displayLarge` | 57/64 | 400 | -0.25 | 45 | no | `.largeTitle` |
| `displayMedium` | 45/52 | 400 | -0.5 | 45 | no | `.largeTitle` |
| `displaySmall` | 36/44 | 400 | -0.3 | 36 | no | `.largeTitle` |
| `headlineLarge` | 32/40 | 400 | 0 | 36 | no | `.title1` |
| `headlineMedium` | 28/36 | 400 | 0 | 28 | no | `.title1` |
| `headlineSmall` | 24/32 | 400 | 0 | 22 | no | `.title2` |
| `titleLarge` | 22/28 | 400 | 0 | 22 | no | `.title2` |
| `titleMedium` | 16/24 | 500 | 0.1 | 16 | no | `.headline` |
| `titleSmall` | 14/20 | 500 | 0.1 | 14 | no | `.subheadline` |
| `bodyLarge` | 16/24 | 400 | 0 | 16 | no | `.body` |
| `bodyMedium` | 14/20 | 400 | 0 | 14 | no | `.callout` |
| `bodySmall` | 12/16 | 400 | 0 | 12 | no | `.footnote` |
| `labelLarge` | 14/20 | 500 | 0 | 14 | no | `.subheadline` |
| `labelMedium` | 12/16 | 500 | 0 | 12 | no | `.footnote` |
| `labelSmall` | 11/16 | 500 | 0 | 12 | no | `.caption2` |

Each style has an emphasised partner, heavier and fully rounded: `displayLargeEmphasized` and so on.
Display, headline and title-large partners are weight 700. `titleMediumEmphasized`,
`titleSmallEmphasized`, `bodyLargeEmphasized`, `bodyMediumEmphasized`, `labelLargeEmphasized` and
`labelMediumEmphasized` are 600. `bodySmallEmphasized` and `labelSmallEmphasized` are 700. Size, line
height, tracking and optical size are the plain style's.

| Need | Call |
|---|---|
| Another weight | `.weight(.medium)`: `.light` 300, `.regular` 400, `.medium` 500, `.semibold` 600, `.bold` 700, `.heavy` 800 |
| A narrower setting | `.narrowed(75)`, `.narrowed(50)` (the `wdth` axis) |
| Tabular figures | `.tabular()` |
| Another size | `.sized(18, line: 24)`; the optical size follows the size |
| Other tracking | `.tracking(-0.4)` |
| Secondary text | `.foregroundStyle(scheme.onSurfaceVariant)` |
| A title that narrows before it truncates | `NarrowingText(text, style:)`: tries width 100, 75, then 50, then truncates |

Optical size by font size: 12 up to 12, 14 up to 14, 16 up to 18, 22 up to 24, 28 up to 30, 36 up to
40, 45 above.

## Shapes

| Token | Value | Used for |
|---|--:|---|
| `AtticRadius.extraSmall` | 4 | The inner corners of a group's rows |
| `AtticRadius.small` | 8 | Covers up to 48, chips, the snackbar |
| `AtticRadius.medium` | 12 | Covers up to 64, a 40pt toggle or button that is on |
| `AtticRadius.large` | 16 | Covers up to 128, a 56pt toggle or button that is on, the mini player's cover, "New playlist" |
| `AtticRadius.largeIncreased` | 20 | A group's outer corners, a row being dragged, covers up to 180 |
| `AtticRadius.extraLarge` | 28 | Covers above 180, sheets, info cards, the wide play control while playing |
| `AtticRadius.extraLargeIncreased` | 32 | The bottom corners of a collection's header block |
| `AtticRadius.extraExtraLarge` | 48 | A collection's play button while idle; Now Playing's paused cover |

| Rule | In code |
|---|---|
| Buttons, bars and pills are fully round | `Capsule()` |
| A cover's radius follows its size | `AtticRadius.cover(size)`: 8 up to 48, 12 up to 64, 16 up to 128, 20 up to 180, 28 above. `CoverShape()` measures itself |
| A list is a group | `GroupCellShape(index:count:)`: 20 on the group's outer corners, 4 inside; rows 2 apart |
| Squared off means on | `AtticRadius.onCorner(height:)`: 11 up to 36, 12 up to 40, 16 up to 56, 28 above |
| Artists are the nine-lobed cookie, never a circle | `CookieShape()` |
| "Nothing here yet" | `CloverShape()`, the 128pt picture of an empty state |
| The loading indicator | Morphs through burst, cookie, pentagon, pill, sunny, four-leaf and oval |

All corners use `RoundedRectangle(cornerRadius:style: .circular)`, so radii match Android.

Expressive separates surfaces by tone, not shadow. Only a row being dragged casts one:
`.shadow(color: .black.opacity(0.3), radius: 6, y: 3)`.

## Spacing

Named in `AtticSpacing`. Use the names; they are `CGFloat`.

| Token | Value | What |
|---|--:|---|
| `screenMargin` | 16 | Groups, shelves, grids, chips and section headers from the screen edge |
| `playerMargin` | 24 | Now Playing's and lyrics' side margin |
| `groupGap` | 2 | Between rows of a group |
| `groupSpacing` | 12 | Between two groups |
| `rowMinHeight` | 64 | A song row, a settings row |
| `collectionRowMinHeight` | 72 | A collection row (56pt lead) |
| `rowPadding` | leading 12, trailing 4, top 8, bottom 8 | Inside a song or collection row; 12 between lead, text and trailing |
| `shelfGap` | 12 | Between tiles in a shelf, between grid columns |
| `gridRowGap` | 20 | Between grid rows (16 with three columns) |
| `chipGap` | 8 | Between chips, between buttons in a row |
| `segmentGap` | 2 | Between connected segments |
| `appBarHeight` | 64 | A detail bar, a collapsed root bar |
| `rootBarHeight` | 72 | A root bar at rest |
| `tabBarHeight` | 64 | The tab bar, above the home indicator |
| `miniPlayerHeight` | 64 | |
| `miniPlayerInset` | 12 | The mini player from the screen edges |
| `miniPlayerGap` | 8 | Between the mini player and the tab bar |
| `dockClearance` | 24 | Under the last item of a scrolling screen, above the dock |
| `touchTarget` | 48 | The least tappable size |
| `xxs` `xs` `s` `m` `l` `xl` `xxl` `xxxl` `huge` | 2 4 8 12 16 20 24 32 48 | Steps for anything else |

Fixed paddings of single components (a section header's 28 above and 12 below, a list label's 20 and
8) are inside the components. A screen does not repeat them.

## Motion

Android's springs are `spring(dampingRatio, stiffness)` with mass 1. The same spring in SwiftUI is
`.spring(response: 2π / √stiffness, dampingFraction: dampingRatio)`, which equals
`.interpolatingSpring(mass: 1, stiffness: stiffness, damping: 2 · dampingRatio · √stiffness)`.

| Token | Android | `response` | `dampingFraction` | (stiffness, damping) | Used for |
|---|---|--:|--:|---|---|
| `AtticMotion.fastSpatial` | 0.6 / 800 | 0.22214 | 0.6 | 800, 33.941 | Corner morphs (play control, toggles, segments), the heart's pop, a paused cover stepping back |
| `AtticMotion.defaultSpatial` | 0.8 / 380 | 0.32232 | 0.8 | 380, 31.190 | Moves across the screen: the mini player sliding in, the wave's amplitude |
| `AtticMotion.slowSpatial` | 0.8 / 200 | 0.44429 | 0.8 | 200, 22.627 | Larger moves; unused so far |
| `AtticMotion.fastEffects` | 1 / 3800 | 0.10193 | 1 | 3800, 123.288 | Colour and alpha that switch at once: the play glyph, toggle colours |
| `AtticMotion.defaultEffects` | 1 / 1600 | 0.15708 | 1 | 1600, 80 | Most fades: an app bar taking its colour, a row's fill |
| `AtticMotion.slowEffects` | 1 / 800 | 0.22214 | 1 | 800, 56.569 | `.fromArt`: the scheme moving to a new cover |
| `AtticMotion.overlay` | 1 / 380 | 0.32232 | 1 | 380, 38.987 | Anything that rises from the bottom, so no gap opens under it |

Spatial springs (position, size, shape) overshoot. Effects springs (colour, alpha) do not. Use
`withAnimation(AtticMotion.fastSpatial)` or `.animation(AtticMotion.defaultEffects, value:)`; never
`.easeInOut` or a duration.

Other motion:

| What moves | Spec |
|---|---|
| The progress wave and the playing mark | One cycle every 1000 ms while playing; the amplitude goes to 0 on `defaultSpatial` when paused |
| The loading indicator | Morphs to the next shape every 650 ms, the morph taking the first 70% of each step, while turning |
| A tab changing | The new tab fades in on `defaultEffects`; no slide |
| Pushing a detail screen | The system push |
| Now Playing, the queue, lyrics | The system's full-screen cover and sheet |
| Reduce Motion on | The wave lies flat, the loading indicator stays a turning cookie, the heart does not pop |

## Icons

Material Symbols Rounded, bundled as `Resources/Fonts/material_symbols_rounded.ttf`: a subset of 92
glyphs with the fill (0 to 1), weight (300 to 700) and optical size (20 to 48) axes kept. The subset
has no ligatures, so each glyph is addressed by codepoint.

```swift
SymbolIcon(.queueMusic)                                   // 24pt, outline, weight 400
SymbolIcon(.favorite, size: 20, filled: true)
    .foregroundStyle(scheme.primary)
```

- `AtticSymbol` is an enum named after the glyph in camel case: `play_arrow` is `.playArrow`,
  `more_vert` is `.moreVert`. `AtticSymbol.named("queue_music")` looks one up.
- Weight is 400. `filled: true` is the selected state (the current tab, a favourite, a toggle that
  is on) and the transport glyphs (play, pause, skip).
- Sizes in the kit: 18, 20, 24, 28, 32, 36, 40. The optical size is 20 below 24pt, 24 below 40pt and
  40 from there. Icons do not scale with Dynamic Type.
- `SymbolIcon` is hidden from VoiceOver. Label the button, not the icon.
- A glyph that is not in the enum is not in the font. Adding one means re-subsetting the font
  (`third_party/fonts-NOTICE.txt`); ask before doing that, and pick from the list below first.

The glyphs: add, airplanemode_active, alarm, album, arrow_back, arrow_forward, autoplay, backspace,
bedtime, bluetooth, calendar_month, call, chat_bubble, check, chevron_right, clear_all, close, delete,
do_not_disturb_on, download, drag_handle, edit, edit_note, equalizer, error, expand_less, expand_more,
favorite, file_open, file_save, flashlight_on, folder, folder_open, graphic_eq, grid_view, home,
image, info, ios_share, keyboard_arrow_down, keyboard_arrow_up, language, library_music, light_mode,
local_fire_department, lock, lyrics, map, mic, more_vert, music_note, music_off, new_releases,
nights_stay, notifications, open_in_full, open_in_new, palette, partly_cloudy_day, pause, person,
photo_camera, play_arrow, playlist_add, playlist_play, queue_music, refresh, repeat, repeat_one,
schedule, screen_rotation, search, search_off, sensors, settings, share, shift, shuffle, shuffle_on,
skip_next, skip_previous, smartphone, sort, swap_vert, today, transition_fade, unfold_more, view_list,
volume_up, wb_sunny, wb_twilight, wifi.

The mark is `AtticMarkIcon(size:)`: one even-odd path, 36 wide by 51.5 tall, drawn `size` tall in the
foreground colour. `AtticWordmark` is the mark at 32pt in `primary`, 10pt, then "Attic".

## App structure

```
RootView                          .atticTheme(mode)
└─ Shell (ZStack)
   ├─ three NavigationStacks      Home, Search, Library; all alive, the selected one shown
   │   .safeAreaInset(edge: .bottom, spacing: 0) { Dock }
   ├─ Snackbar                    above the dock
   └─ .fullScreenCover            Now Playing
        └─ .sheet                 Queue, Lyrics
```

### Tabs

- Three tabs: Home (`.home`), Search (`.search`), Library (`.libraryMusic`). The shell keeps one
  `NavigationStack` per tab in a `ZStack` and shows the selected one (`opacity` 1 or 0,
  `allowsHitTesting`), so each tab keeps its place. Do not use `TabView`.
- Tapping the selected tab pops its stack to the root.
- Each stack hides the system bar on every screen: `.toolbar(.hidden, for: .navigationBar)`. Screens
  draw `RootAppBar` or `DetailAppBar` as the first thing in their `safeAreaInset(edge: .top)` or at
  the top of a `VStack(spacing: 0)`.
- Pushes: Album, Artist, Playlist (a user playlist or a folder), Favourites, Settings, Open-source
  licences, one licence's text. Use `navigationDestination(for:)` with one route enum per stack.
- The back swipe from the left edge must keep working with the system bar hidden. If it does not,
  `AtticUI` re-enables `interactivePopGestureRecognizer` once, in the shell.

### The dock

The dock is the tab bar with the mini player above it, placed with
`.safeAreaInset(edge: .bottom, spacing: 0)` on the `ZStack` of stacks, so every scroll view inside
ends above it.

```
┌──────────────────────────────────────────┐
│  12 │      mini player, 64 tall      │ 12 │   only while a song is queued
│      8                                    │
├──────────────────────────────────────────┤
│  tab bar: 64 tall, surfaceContainer       │
│  home indicator inset, surfaceContainer   │
└──────────────────────────────────────────┘
```

- `AtticTabBar` is 64pt tall and fills the home indicator's inset below it with `surfaceContainer`
  (`.ignoresSafeArea(edges: .bottom)` on its background only).
- `MiniPlayerBar` is 64pt tall, 12pt from each side, 8pt above the tab bar. It appears with
  `.transition(.move(edge: .bottom).combined(with: .opacity))` on `AtticMotion.defaultSpatial` when a
  song is first queued. Nothing is drawn behind it; the screen scrolls under it.
- A scrolling screen adds `AtticSpacing.dockClearance` (24) under its last item:
  `.contentMargins(.bottom, AtticSpacing.dockClearance, for: .scrollContent)`.
- Library's "New playlist" button sits 12pt above the dock, 16pt from the trailing edge.
- While the keyboard is up the dock is hidden (`.ignoresSafeArea(.keyboard)` off; hide the dock when
  a search field has focus), so the results use the whole screen.
- The snackbar sits 16pt above the dock and 16pt from each side.

### Overlays

- **Now Playing** is a `.fullScreenCover` on the shell, opened by tapping the mini player. Its content
  is wrapped in `.fromArt(seed:)` and fills the screen with `scheme.surface`. Give the cover
  `.presentationBackground(.clear)`; the page follows a downward drag that starts on the header or
  the cover, and closes when the drag passes 160pt or ends faster than 800pt/s, else returns on
  `AtticMotion.defaultSpatial`. The close button does the same.
- **Queue** and **Lyrics** are `.sheet`s from Now Playing with `.presentationDetents([.large])`,
  `.presentationDragIndicator(.hidden)` (the screen draws `SheetHandle`),
  `.presentationCornerRadius(AtticRadius.extraLarge)` and `.presentationBackground(scheme.surface)`.
  Both are inside `.fromArt(seed:)`; a sheet does not inherit the environment override, so apply it
  again on the sheet's content.
- A song's menu is a system `Menu` on the row's more button and the same items as a `.contextMenu`
  on the row. Naming a playlist is a system `.alert` with a text field. Deleting a playlist is a
  `.confirmationDialog`.

### Safe areas and Dynamic Type

- Bars take the status bar themselves: their background runs to the top edge
  (`.ignoresSafeArea(edges: .top)` on the background only), their content sits below the inset.
- Content never sits under the home indicator: the tab bar, Now Playing's tools row and a sheet's
  last row all stay above it.
- Landscape is not supported on iPhone; the app is portrait only.
- Text scales with Dynamic Type. Rows have a minimum height, not a fixed one, so they grow. The tab
  bar, the mini player, the transport and app bars keep their heights; the app caps Dynamic Type at
  `.accessibility2` at the root (`.dynamicTypeSize(...DynamicTypeSize.accessibility2)`), and text
  inside fixed-height pieces is one line that truncates.

## Components

Everything is `public` in `AtticKit`. Every component reads `\.atticScheme` itself. Labels in the
"VoiceOver" column are built in unless a parameter is named.

### Covers

| Component | Spec | VoiceOver |
|---|---|---|
| `Cover(_ image: Image?, size: CGFloat? = nil, shape: CoverStyle = .cover, fallback: AtticSymbol = .musicNote)` | A square. `size: nil` fills the width it is given. The image fills and crops. With no image: `surfaceContainerHighest` with the fallback glyph at 46% of the width in `onSurfaceVariant`. `CoverStyle`: `.cover` (radius by size), `.artist` (cookie), `.circle`, `.square` (no rounding), `.rounded(CGFloat)` | Hidden |
| `Mosaic(_ images: [Image], size: CGFloat? = nil)` | Four covers in a 2×2 grid when there are four or more, else the first, else the placeholder | Hidden |
| `FavouritesArt(size: CGFloat? = nil)` | `tertiaryContainer` with a filled heart at 50% of the width in `onTertiaryContainer` | Hidden |

### Rows and groups

| Component | Spec | VoiceOver |
|---|---|---|
| `ListGroup { rows }` | Puts each child in a cell: `GroupCellShape`, `surfaceContainer` (`surfaceContainerHigh` inside a sheet, set with `.groupCellColor(_:)`), 2pt apart, 16pt from the screen edges | |
| `.groupCell(index:count:)` | The same for one row of a lazy list: use it on each row of a `LazyVStack(spacing: 0)` | |
| `SongRow(title:subtitle:lead:isCurrent:isPlaying:isLiked:action:trailing:)` | 64pt minimum. Padding 12 / 4 / 8 / 8, 12 between parts. Lead: `.cover(Image?)` 48pt, `.number(Int)` 28pt wide in `labelLarge` tabular `onSurfaceVariant`, `.rankedCover(Int, Image?)`, `.none`. Title `bodyLarge` weight 500, line 22, one line. Subtitle `bodyMedium` `onSurfaceVariant`, one line; an empty subtitle is left out. `isLiked` draws a 16pt filled heart in `primary` after the text. `isCurrent`: the row fills with `secondaryContainer` on `defaultEffects`, the title is weight 700, title and subtitle are `onSecondaryContainer`, and the wave is drawn over the cover (on `secondaryContainer` at 80%) or in place of the number | One element: "title, subtitle"; adds "Playing" when current; the trailing control is its own element |
| `RowMoreMenu(title:) { menu items }` | A 48pt target with `.moreVert` in `onSurfaceVariant` that opens a system `Menu` | "More options for {title}" |
| `RowMoreButton(title:action:)` | The same glyph as a plain button | "More options for {title}" |
| `RowDragHandle(title:)` | 48pt, `.dragHandle` in `onSurfaceVariant`; decoration for a row the list can reorder | Hidden |
| `CollectionRow(title:subtitle:action:lead:trailing:)` | 72pt minimum, a 56pt lead (a `Cover`, `Mosaic`, `FavouritesArt`), same padding and text as a song row | One element: "title, subtitle" |
| `SectionHeader(_ title:, overline: String? = nil) { trailing }` | Title `titleLargeEmphasized`; overline `bodyMedium` `onSurfaceVariant` 2pt above it. 28 above, 12 below, 16 at the sides | A header |
| `ListLabel(_ text:)` | `labelLarge` weight 600 `onSurfaceVariant`; 20 above, 8 below, 32 at the sides | A header |

### Tiles

| Component | Spec | VoiceOver |
|---|---|---|
| `Tile(title:subtitle:width:isArtist:isCurrent:isPlaying:action:art:)` | The art fills the width (148 by default; `nil` fills the cell), 8pt, title `titleSmall` tracking 0, subtitle `bodySmall` `onSurfaceVariant`, one line each. `isArtist` centres the text. `isCurrent` turns the title `primary` with the wave before it | One button: "title, subtitle" |
| `Shelf { tiles }` | A horizontal scroll, 12 between tiles, 16 at the edges, no indicator | |
| `TileGrid(items, columns: 2) { tile }` | 12 between columns; 20 between rows, 16 with three columns; 16 at the sides | |
| `FavouritesPill(count:unit:height:action:)` | 64 wide, 200 tall, 32pt corners, `tertiaryContainer`; a 28pt filled heart 18 from the top; the count in `titleLargeEmphasized` (line 24) over the unit in `labelSmall`, 16 from the bottom; all `onTertiaryContainer` | "Favourites, {count} {unit}" |

### Buttons

| Component | Spec | VoiceOver |
|---|---|---|
| `AtticButton(_ title:, variant:, size:, leading:, trailing:, squared:, tint:, action:)` | `variant`: `.filled` (`primary`), `.tonal` (`secondaryContainer`), `.outlined` (1pt `outlineVariant`, `onSurfaceVariant` text), `.text` (`primary` text, or `tint`). `size`: `.small` 40pt tall, 16 side padding, label `labelLarge` weight 600, 20pt icon; `.medium` 56pt, 24 side padding, label 16/24 weight 600, 24pt icon. 8 between icon and label; the padding on an icon's side is 4 less. A capsule; `squared` gives it 12 (small) or 16 (medium) corners on `fastSpatial`. Disabled: 38% opacity. Pressed: 92% opacity | The title |
| `AtticIconButton(_ symbol:, label:, variant:, medium:, width:, filled:, tint:, action:)` | A 40pt circle in a 48pt target; 56pt with `medium`. `variant`: `.standard` (no container, `onSurfaceVariant`), `.filled`, `.tonal`, `.outlined`. `width`: `.narrow` (32, or 48 medium), `.standard`, `.wide` (52, or 72 medium), `.fill` (takes the width it is given). 24pt glyph | `label` |
| `AtticToggleButton(_ symbol:, isOn:, label:, medium:, width:, tonalWhenOff:, action:)` | Round while off. On: `primary` container, `onPrimary` filled glyph, corners `onCorner(height)`. The corner moves on `fastSpatial`, the colours on `fastEffects` | `label`; trait "selected" while on |
| `HeartButton(isFavourite:action:)` | A 40pt target in 48, no container. Outline in `onSurfaceVariant`, or filled in `primary`. Pops from 0.8 to 1 on `fastSpatial` when it changes. A light impact haptic when turned on | "Add to favourites" / "Remove from favourites" |
| `ShuffleToggle(isOn:action:)` | An `AtticToggleButton` with `.shuffle` | "Turn shuffle on" / "Turn shuffle off" |
| `RepeatToggle(mode:action:)` | `AtticRepeatMode`: `.off`, `.all`, `.one`. `.repeat`, or `.repeatOne` for `.one`; on unless `.off` | "Repeat off, tap to repeat all" / "Repeat all, tap to repeat one" / "Repeat one, tap to turn off" |
| `ExtendedFab(_ title:, symbol:, expanded:, action:)` | 56pt tall, 16pt corners, `primaryContainer`; a 24pt glyph, 12pt, the title in `titleMedium` weight 600; 16 leading, 20 trailing. Not `expanded`: a 56pt square with the glyph | The title |
| `SplitButton(_ title:, leading:, menuLabel:, action:) { menu items }` | Two 40pt halves 2 apart on `secondaryContainer`: the action (outer corners 20, inner 4, padding 16 and 12, a 20pt glyph, `labelLarge` weight 600) and a `Menu` half with `.keyboardArrowDown` at 20pt | The title; `menuLabel` |
| `ButtonRow { buttons }` | Buttons 8 apart | |

### Play controls

| Component | Spec | VoiceOver |
|---|---|---|
| `PlayButton(isPlaying:size:tonal:progress:action:)` | Round while paused, squared off while playing. `.small`: 36pt in a 52pt box, corner 11, 22pt glyph; with `progress` (0 to 1) a ring is drawn round it: 3pt stroke 2.5 in from the box, played part `primary`, the rest `outlineVariant`, a 14° gap at each end. `.large`: 56pt, corner 18, 32pt glyph. `.extraLarge`: 152×80 paused, 128×80 playing, corner 28, 44pt glyph. `primary` with `onPrimary`; `tonal` uses `secondaryContainer`. Width and corner move on `fastSpatial`; the glyph cross-fades on `fastEffects` | "Play" / "Pause" |
| `SkipButton(_ symbol:, label:, action:)` | 76×80 capsule, `secondaryContainer`, a 36pt filled glyph in `onSecondaryContainer` | `label` |
| `Transport(isPlaying:onPrevious:onPlayPause:onNext:)` | Previous, the extra-large play control, next; 8 apart, centred | "Previous", "Play" / "Pause", "Next" |
| `NowPlayingWave(isPlaying:)` | 20×10: a sine wave, wavelength 10, amplitude 2, stroke 2.4 with round caps, in the foreground colour (`primary` by default); flat when paused | Hidden |
| `ProgressSlider(position:duration:isPlaying:label:wavy:showsTimes:onEditingChanged:)` | See below | `label`, value "{elapsed} of {total}", adjustable in steps of 5% |
| `ToolsRow { buttons }` | A 56pt capsule on `surfaceContainer`, 12 side padding, its children spaced evenly from edge to edge | |
| `LinearProgress(_ fraction:)` | A 4pt line: the done part `primary`, a 4pt gap, the rest `secondaryContainer` | Progress |
| `LoadingIndicator(contained:size:)` | 48pt; the morphing shape in `primary`, 38pt across. `contained`: a `primaryContainer` circle with the shape 30pt across in `onPrimaryContainer` | "Loading" |

**`ProgressSlider`.** `position` is a `Binding<Double>` in seconds and `duration` is seconds. 44pt
tall, plus the times row when `showsTimes`.

- The track is 4pt thick. The handle is a 4×28 bar with 2pt corners in `primary`, with 6pt clear on
  each side. The played part is a wave in `primary` (wavelength 32, amplitude 3.5, round caps) that
  travels one cycle a second while `isPlaying`; it is flat while paused, while dragging, with
  `wavy: false` and with Reduce Motion. The rest of the track is `outlineVariant` with round caps.
- The handle's centre is at `2 + 6 + 2 + (width − 4 − 12 − 4) × fraction`.
- A tap jumps; a drag scrubs. While the finger is down the binding is written on every move and
  `onEditingChanged(true)` has been called; on release the binding has the final value and
  `onEditingChanged(false)` is called. The screen seeks the player on `false` and ignores the
  player's clock while editing.
- The times row is `labelMedium` tabular in `onSurfaceVariant`: elapsed at the leading edge, the
  length at the trailing edge, as `m:ss` (or `h:mm:ss`).

### Navigation

| Component | Spec | VoiceOver |
|---|---|---|
| `AtticTabBar(items:selection:onReselect:)` | 64pt on `surfaceContainer`. Each `AtticTabItem(title:symbol:)` is an equal column: a 56×32 capsule (clear, or `secondaryContainer` when selected, fading on `fastEffects`) holding the 24pt glyph (`onSurfaceVariant`; filled and `onSecondaryContainer` when selected), 4pt, the label in `labelMedium` (weight 500 `onSurfaceVariant`; weight 700 `onSurface` when selected). The whole column is the target | The title; traits "tab", "selected" |
| `RootAppBar(_ title:, scrolled:, leading:, actions:)` | One row, 16 leading, 8 trailing. At rest: 72pt tall, the title in `displaySmallEmphasized` tracking -0.4, clear. As `scrolled` goes 0 to 1 the row shrinks to 64, the title to `titleLargeEmphasized`, and past 0.5 the bar takes `surfaceContainer` up to the top edge, on `defaultEffects`. `leading` sits 10pt before the title (Home's mark) | The title is a header |
| `DetailAppBar(_ title:, surface:, onBack:, actions:)` | 64pt, 4 side padding: a back button (`.arrowBack`), the title in `titleLarge` weight 500 (4 side padding, one line), actions. `surface`: `.plain` (clear), `.scrolled` (`surfaceContainer` to the top edge), `.overArt` (clear, no title, each button on a `surface` circle at 62% with `onSurface` glyphs) | "Back"; the title is a header |
| `ScrollOffsetReader` / `.onScrollFraction(_:)` | Reports how far a scroll view has moved from its top as 0 to 1 over the first 120pt, for the two bars | |
| `SearchField(text:placeholder:onSubmit:onBack:)` | A 56pt capsule on `surfaceContainerHigh`: `.search` in `onSurface` (16 leading, 12 after), the text in `bodyLarge`, the placeholder in `onSurfaceVariant`, a clear button (`.close`) once there is text. With `onBack` a back button replaces the glyph. The cursor is `primary`. Return key: search | "Clear search" |
| `ChipRow { chips }` | A horizontal scroll of chips 8 apart, 16 at the sides | |
| `FilterChip(_ label:, isSelected:, action:)` | 32pt tall in a 48pt target, 8pt corners, `labelLarge`. Off: a 1pt `outlineVariant` border, `onSurfaceVariant` text, 16 side padding. On: `secondaryContainer`, `onSecondaryContainer`, an 18pt tick before the label (8 leading, 8 after the tick, 16 trailing) | The label; trait "selected" |
| `ConnectedButtonGroup(_ options:, selection:, onGroupCell:, action:)` | A one-of-N switch. Segments 40pt tall, 2 apart; 16 at the sides, 4 above and below. The group's ends are 20pt corners, inner corners 8. Selected: `primary`, `onPrimary`, an 18pt tick 6pt before the label, weight 600, all four corners 20, 15% wider. Others: `surfaceContainerHigh` (`surfaceContainerHighest` with `onGroupCell`), `onSurfaceVariant`, weight 500. Corners and width move on `fastSpatial`, colours on `fastEffects` | Each label; trait "selected" |

### Player surfaces

| Component | Spec | VoiceOver |
|---|---|---|
| `MiniPlayerBar(title:artist:cover:isPlaying:isLiked:progress:onOpen:onPlayPause:onNext:onToggleLike:)` | A 64pt capsule on `surfaceContainerHigh`, 8 leading, 6 trailing: a 48pt cover with 16pt corners, 12pt, the title in `labelLarge` weight 600 over the artist in `bodySmall` `onSurfaceVariant` (one line each), a `HeartButton`, a small `PlayButton` whose ring shows `progress`, and a standard icon button with `.skipNext` (filled). Tapping anywhere else calls `onOpen`. Swiping the text left calls `onNext`. The screen wraps it in `.fromArt(seed:)` | The text is one button: "{title}, {artist}", hint "Opens the player"; then "Add to favourites", "Play" / "Pause", "Next" |
| `CollectionHeader(title:isArtist:byline:meta:isPlaying:shuffleOn:isEnabled:name:onPlay:onShuffle:onAdd:cover:)` | A block on `surfaceContainer` with 32pt bottom corners, running to the top edge. Padding 16 at the sides, 68 below the status bar (the bar floats over it), 20 at the bottom. The title: `headlineLargeEmphasized`, line 36, tracking -0.4, up to three lines, narrowed to 75 if one line does not fit; for an artist `displayMediumEmphasized`, line 48, tracking -0.8. `byline` (an album's artist): 10pt above, a 28pt cookie, 8pt, the name in `bodyLarge` weight 600 `primary`, a button. Each `meta` line: `bodyMedium` `onSurfaceVariant`, 6pt above. Then, 18pt down, a 208pt row: the 208pt cover, 12pt, and the play cluster filling the rest: a play button that fills the cluster (`primary`, corners 48 idle and 28 playing on `fastSpatial`, a 40pt filled glyph over "Play" or "Pause" in `bodyLarge` weight 600), 8pt, a row of a shuffle toggle and an add-to-playlist button, both medium, tonal, `.fill`, 8 apart. Not `isEnabled`: the cluster is 38% and inert | "Play" / "Pause"; "Shuffle {name}"; "Add {name} to a playlist" |
| `InfoCard(title:symbol:) { content }` | `surfaceContainer`, 28pt corners, 20 padding; a heading of a 20pt glyph, 8pt, `labelLarge` weight 600, both `onSurfaceVariant`, 14 above the content | The heading is a header |
| `SyncedLyricLine(_ text:, state:, action:)` | `LyricLineState`: `.sung` (`outline`), `.current` (`primary`), `.upcoming` (`onSurfaceVariant`). Current: 34/40 weight 700 rounded, tracking -0.3, 4 above and 20 below. Others: 24/32 weight 500, 14 below. Size and colour move on `defaultSpatial` and `defaultEffects`. Leading aligned, wraps | The text; a button |

### Sheets, settings and states

| Component | Spec | VoiceOver |
|---|---|---|
| `SheetHandle()` | 32×4 capsule in `outline`, centred, 16 above and 12 below | Hidden |
| `SheetTitle(_ text:)` | `headlineSmallEmphasized`; 24 at the sides, 4 above, 12 below | A header |
| `SheetHeader(title:subtitle:) { art }` | A 56pt cover, 16pt, title `bodyLarge` weight 600 over subtitle `bodyMedium` `onSurfaceVariant`; 24 at the sides, 4 above, 12 below | One element |
| `SheetRow(_ symbol:, _ label:, isSelected:, tint:, action:)` | 56pt minimum, 24 at the sides: a 24pt glyph in `onSurfaceVariant`, 16pt, the label in `bodyLarge`. Selected: both `primary`, weight 600 | The label |
| `SheetDivider()` | 1pt `outlineVariant`; 24 at the sides, 8 above and below | Hidden |
| `SettingsSection(_ title:)` | `labelLarge` weight 600 in `primary`; 24 above, 8 below, 32 at the sides | A header |
| `SettingsRow(title:summary:symbol:) { trailing }` | 64pt minimum; 16 at the sides, 12 above and below, 16 between parts. With a symbol: a 40pt `secondaryContainer` circle holding the 20pt glyph in `onSecondaryContainer`. Title `bodyLarge` weight 500; summary `bodyMedium` `onSurfaceVariant`, wraps | One element |
| `SettingsToggleRow(title:summary:symbol:isOn:)` | A `SettingsRow` ending in a system `Toggle` tinted `primary`; the whole row toggles | A switch |
| `SettingsChoiceRow(title:summary:isSelected:action:)` | A `SettingsRow` ending in a 24pt tick in `primary` when selected | Trait "selected" |
| `SettingsNavigationRow(title:summary:symbol:action:)` | A `SettingsRow` ending in `.chevronRight` in `onSurfaceVariant` | A button |
| `SettingsNote(_ text:)` | `bodyMedium` `onSurfaceVariant`, 16 padding, in a group cell | |
| `EmptyState(title:body:symbol:clover:) { action }` | Centred; 32 at the sides, 48 above and below. A 112pt square with 36pt corners (or the 128pt clover) on `secondaryContainer` holding a 48pt glyph in `onSecondaryContainer`; 24pt; the title in `headlineSmallEmphasized`; 8pt; the body in `bodyMedium` `onSurfaceVariant`; 24pt; the action. All centred text | One element, then the action |
| `LoadingState(_ label: String? = nil)` | A `LoadingIndicator` centred in the space it is given, with the label 16pt under it in `bodyLarge` `onSurfaceVariant`. It appears after 400 ms, so a quick load never flashes it | "Loading", or the label |
| `Snackbar(_ text:)` | `inverseSurface`, 8pt corners, 16 side padding, 14 above and below, `bodyMedium` in `inverseOnSurface`, as wide as the screen less 32 | Announced when shown |
| `.snackbar(_ message: Binding<String?>, bottomInset:)` | Shows a `Snackbar` over the view's bottom edge, `bottomInset` + 16 up; it rises and fades in on `defaultSpatial`, stays 3 s, then sets the binding to nil | |

### The gallery

`KitGallery()` shows every token and component in the current theme, with a switch at the top for
light and dark. The app opens it in place of the shell when launched with `-kitGallery YES`.

## Screens

Each screen is a `VStack(spacing: 0)` of a bar and a `ScrollView` with a `LazyVStack(spacing: 0)`
unless it says otherwise. "Group" means rows with `.groupCell(index:count:)`, or a `ListGroup`.
String names are the Android names in `app/src/main/res/values/strings.xml`; [Copy](#copy) lists the
ones that are new or changed for iOS.

### First run

Shown until a music folder has been chosen. No bars; `surface`; content in a column with 24 at the
sides. Three steps, moved between on `AtticMotion.defaultSpatial` with a cross-fade.

1. **Welcome.** Centred vertically: `AtticMarkIcon(size: 72)` in `primary`; 32pt;
   `onboarding_welcome_title` in `headlineLargeEmphasized` tracking -0.3; 14pt;
   `onboarding_welcome_body` in `bodyLarge` `onSurfaceVariant`. Pinned to the bottom, 24 above the
   home indicator: `AtticButton(onboarding_get_started, size: .medium)` at full width.
2. **Choose a music folder.** From the top, 32 below the status bar: `onboarding_step` ("Step 1 of
   2") in `labelLarge` `primary`; 8pt; `onboarding_access_title` in `headlineLargeEmphasized`; 14pt;
   `ios_folder_body` in `bodyLarge` `onSurfaceVariant`; 24pt; a `ListGroup` of one
   `SettingsRow(title: ios_folder_row_title, summary: ios_folder_row_body, symbol: .folderOpen)`
   whose trailing is `AtticButton(ios_folder_choose, variant: .tonal)`. Once a folder is chosen the
   summary is the folder's name and the trailing is a 24pt tick in `primary`. Pinned to the bottom:
   `AtticButton(onboarding_continue, size: .medium)` at full width, disabled until a folder is
   chosen. "Choose" opens `.fileImporter(allowedContentTypes: [.folder])`.
3. **Finding your songs.** "Step 2 of 2". While scanning: `LoadingState(onboarding_scanning)`.
   After: `onboarding_found` ("Found 127 songs") in `headlineLargeEmphasized`, and
   `AtticButton(onboarding_start, size: .medium)` pinned to the bottom. With no songs:
   `EmptyState(title: home_empty_title, body: home_empty_body, symbol: .musicOff, clover: true)` with
   a tonal `ios_folder_choose_another` button, and `onboarding_start` still offered.

### Home

- `RootAppBar("Attic", scrolled:, leading: AtticMarkIcon(size: 32 → 24 as it scrolls) in primary)`
  with one action: `AtticIconButton(.settings, label: settings)`.
- `ConnectedButtonGroup([home_filter_all, home_filter_albums, home_filter_artists,
  home_filter_folders])`.
- **All:**
  1. 12pt, then the hero row, 16 from the leading edge: `FavouritesPill` (only when there are
     favourites; opens Favourites) and, 8pt after it, a horizontal scroll of the biggest albums as
     200pt square covers with 28pt corners, 8 apart, each with a caption 10pt under it: the album in
     `titleMedium` weight 600 (line 22) over `home_hero_album_subtitle` ("Mira Okafor, 2025") in
     `bodySmall` `onSurfaceVariant`. A cover opens its album.
  2. Song shelves, each a `SectionHeader` and a `Shelf` of `Tile(width: 112)` with the song's cover,
     title and artist, shown only when it has songs: `home_recently_played`, `home_on_repeat`,
     `home_not_lately`, `home_newly_added`. Tapping a tile plays the shelf from that song. The tile of
     the song that is playing is `isCurrent`.
  3. `SectionHeader(home_start_listening, overline: home_start_listening_overline)` with a large
     `PlayButton` as its trailing (plays them all), then a group of up to eight random songs as
     `SongRow`s with `RowMoreMenu`.
  4. `SectionHeader(home_your_folders)` and a `Shelf` of `Tile(width: 148)` with a `Mosaic`, the
     folder's name and `song_count`.
  5. `SectionHeader(home_albums)` and a `Shelf` of album `Tile`s (title, artist).
  6. `SectionHeader(home_top_artists)` and a `Shelf` of `Tile(width: 112, isArtist: true)` with a
     cookie cover and the name.
- **Albums**, **Artists**, **Folders:** 16pt, then a `TileGrid` (two columns; three for artists) of
  the same tiles with `width: nil`.
- **Empty library:** under the bar, 64pt down: `EmptyState(title: home_empty_title, body:
  home_empty_body, symbol: .musicOff, clover: true)` with a tonal `settings_folders_add` button.
- **Loading:** `LoadingState()` under the bar.

### Library

- `RootAppBar(library_title, scrolled:)` with `AtticIconButton(.search, label: library_search_cd)`
  (switches to the Search tab), a grid or list toggle (`.gridView` / `.viewList`, labels
  `library_show_grid_cd` / `library_show_list_cd`) and `AtticIconButton(.settings, label: settings)`.
- `ChipRow` of four `FilterChip`s: `library_filter_playlists`, `library_filter_albums`,
  `library_filter_artists`, `library_filter_songs`. Tapping the selected chip clears it. 12pt under
  the chips. The chips stay pinned under the bar and take its colour when scrolled.
- **No filter, list:** one group: Favourites first (`CollectionRow` with `FavouritesArt`, subtitle
  `library_playlist_subtitle`), then playlists and folders (`Mosaic`, `library_playlist_subtitle`).
  Then `ListLabel(library_filter_artists)` and a group of artists (cookie cover,
  `library_artist_subtitle`), then `ListLabel(library_filter_albums)` and a group of albums (cover,
  `library_album_subtitle`).
- **Playlists**, **Albums**, **Artists:** that one group alone.
- **Grid:** a three-column `LazyVGrid`, 12 between columns, 16 between rows, 16 at the sides, of
  `Tile(width: nil)`.
- **Songs:** `AtticButton(library_shuffle_all_cd, size: .medium, leading: .shuffle)` at full width
  (16 at the sides, 4 above); then a row (32 leading, 16 trailing, 12 above, 8 below) of
  `song_count` in `labelLarge` weight 600 `onSurfaceVariant` and a `SplitButton(sort name, leading:
  .sort, menuLabel: library_sort_menu_cd)` whose action cycles the sort and whose menu lists
  `library_sort_title`, `library_sort_artist`, `library_sort_recently_added` with a tick on the
  current one; then one group of every song as `SongRow(lead: .cover)` with `RowMoreMenu`.
- `ExtendedFab(playlist_picker_new, symbol: .add)` at the bottom trailing corner, 16 from the edge
  and 12 above the dock, on the no-filter and Playlists views; `expanded` only at the top of the
  list. It opens the name alert (`dialog_new_playlist_title`, field `dialog_playlist_name_label`,
  `cancel`, `create`).
- **Empty:** as Home's empty library.

### Album

- A `ZStack(alignment: .top)`: the scroll view, and over it `DetailAppBar(album name, surface:
  .overArt until the header's title has scrolled under the bar, then .scrolled)`. No actions.
- The scroll view starts at the top edge (`.ignoresSafeArea(edges: .top)`):
  1. `CollectionHeader(title: album, byline: artist (opens the artist), meta: [album_kind_year
     ("Album • 2025"), songs_and_duration ("8 songs, 30 min")], cover:)` inside `.fromArt(seed:)`.
     The bar is inside the same `.fromArt`.
  2. 12pt, then one group of the songs as `SongRow(lead: .number(n))` with the artist as subtitle and
     `RowMoreMenu`. Tapping plays the album from that song.
- Play plays the album from its first song, or pauses when this album is what is playing. Shuffle
  plays it shuffled. Add opens the playlist picker sheet.

### Artist

As Album, with `CollectionHeader(title: name, isArtist: true, meta: [song_count], cover: cookie)`:
pass `Cover(image, shape: .artist)` as the cover. Under it:

1. `SectionHeader(artist_top_songs)` and a group of up to five `SongRow(lead: .rankedCover(rank,
   cover))`, only when there is play history.
2. `SectionHeader(artist_albums)` and a `Shelf` of album `Tile`s with `artist_album_year_kind`
   ("2025 • Album") as subtitle.
3. `SectionHeader(artist_all_songs)` and a group of every song (`lead: .cover`, subtitle the album).

### Playlist

As Album, with `meta: [library_playlist_subtitle or folder_subtitle, songs_and_duration]` and a
`Mosaic` cover. For a user playlist the bar has one action, `AtticIconButton(.moreVert, label:
collection_options_cd)` as a `Menu` of `playlist_rename` and `playlist_delete` (destructive). Under
the header, 12 above and below, 16 at the sides: `ButtonRow` of `AtticButton(playlist_add_songs_cd,
variant: .tonal, leading: .add)` (filled when the playlist is empty) and
`AtticButton(playlist_reorder_cd, variant: .outlined, leading: .swapVert)`. Then one group of
`SongRow(lead: .cover)`. A folder has neither the menu nor the two buttons.

- **Empty:** `EmptyState(playlist_empty_title, playlist_empty_body)` with no picture.
- **Reorder:** the bar becomes a close button (`.close`, `close`), `playlist_reorder_cd` as the
  title and a filled `AtticIconButton(.check, label: playlist_reorder_done_cd)`. The list is a
  `List` as in the queue, rows ending in `RowDragHandle`.

### Favourites

As Playlist for a folder, with `CollectionHeader(title: favourites, meta: [songs_and_duration],
cover: FavouritesArt())` on the app scheme (no `.fromArt`). **Empty:**
`EmptyState(favourites_empty_title, favourites_empty_body, symbol: .favorite)`.

### Search

- With no text and no focus: `RootAppBar(search_title, scrolled:)` with the settings action, then
  `SearchField(text:, placeholder: search_placeholder)` with 16 at the sides, then
  `SectionHeader(search_browse)`, `ListLabel(home_albums)` and a `Shelf` of album tiles
  (`width: 120`), `ListLabel(library_filter_artists)` and a `Shelf` of artist tiles.
- With focus or text: the bar goes; the field sits 8pt under the status bar with `onBack` set
  (clears the text and drops focus). Under it, 12pt down:
  - collections first, in one group of `CollectionRow`s: artists (`search_result_artist`), albums
    (`search_result_album`), playlists (`search_result_playlist`);
  - then `ListLabel(library_filter_songs)` and a group of `SongRow`s.
  - **No results:** `EmptyState(search_no_results_title, search_no_results_body, symbol:
    .searchOff)`.
- Search is on every keystroke, after 150 ms without typing.

### Mini player

`MiniPlayerBar` in the dock, inside `.fromArt(seed:)`. The ring shows the position, updated twice a
second. See [the dock](#the-dock).

### Now Playing

One page that does not scroll, on `scheme.surface` from `.fromArt(seed:)`. A `VStack(spacing: 0)`:

1. **Header**, 64pt, 8 at the sides, 4 between parts: `AtticIconButton(.keyboardArrowDown, label:
   now_playing_close_cd)`; a column of `now_playing_playing_from` in `labelMedium`
   `onSurfaceVariant` over a row of the context's glyph (18pt, `primary`; album `.album`, artist
   `.person`, folder `.folder`, playlist `.playlistPlay`, favourites `.favorite`), 6pt, and its name
   in `titleSmall` weight 600 tracking 0; then `RowMoreMenu(title:)`.
2. **Cover**, 4pt down, 24 at the sides: a square `Cover`, as wide as the screen less 48 and never
   taller than the room left. Playing: 28pt corners at full size. Paused: scaled to 86% with corners
   that read as 48, on `AtticMotion.fastSpatial`. Swiping it left or right skips to the next or
   previous song.
3. A flexible gap, at least 16.
4. **Title block**, 24 at the sides: the line being sung when the song has synced lyrics (a 20pt
   row: `.lyrics` at 18pt, 8pt, the line in `titleSmall` weight 600, both `primary`; 10pt under it);
   the title as `NarrowingText` in `headlineLargeEmphasized` tracking -0.3; 2pt; the artist in
   `bodyLarge` `onSurfaceVariant` (opens the artist).
5. **Progress**, 10pt down, 24 at the sides: `ProgressSlider(position:, duration:, isPlaying:, label:
   now_playing_seek_cd)`.
6. **Transport**, 22pt down: `Transport(...)`.
7. **Tools**, 20pt down, 24 at the sides, 16 above the home indicator: `ToolsRow` of
   `ShuffleToggle`, `RepeatToggle`, `HeartButton`, `AtticIconButton(.lyrics, label: lyrics_title)`
   and `AtticIconButton(.queueMusic, label: now_playing_open_queue_cd)`.

On a phone shorter than 700pt the gap in step 3 goes to 8 and the cover shrinks to fit.

### Queue

A sheet from Now Playing on the cover's scheme.

1. `SheetHandle`.
2. A 64pt bar, 8 at the sides: `AtticIconButton(.keyboardArrowDown, label: queue_close_cd)`, then
   `queue_title` in `titleLargeEmphasized` over the context row (a 16pt glyph in `primary`, 6pt, the
   name in `labelMedium` `onSurfaceVariant`).
3. A `List` (`.listStyle(.plain)`, `.scrollContentBackground(.hidden)`, no separators, row insets 0,
   clear row backgrounds):
   - `ListLabel(queue_now_playing)`, then the current song as a one-row group: `SongRow(isCurrent:
     true)` with `RowMoreMenu`. It does not move or swipe.
   - A row of `ListLabel(queue_up_next)` ("Up next, 9 songs") and, 20 from the trailing edge,
     `AtticButton(queue_clear, variant: .text)`.
   - The songs up next, each `SongRow(lead: .cover)` with `.groupCell(index:count:)` and
     `RowDragHandle` as trailing. Tapping jumps to it. `.onMove` reorders (press and hold, then
     drag); `.swipeActions(edge: .trailing, allowsFullSwipe: true)` has one destructive button,
     `queue_remove` with `.delete`, tinted `scheme.error`.
   - **Nothing up next:** `EmptyState(queue_nothing_up_next, body: nil, symbol: .queueMusic, clover:
     true)`, 48pt down.
4. 40pt under the last row.

### Lyrics

A sheet from Now Playing on the cover's scheme, on `scheme.surface`.

1. A top bar on `surfaceContainer`: `SheetHandle`, then a 64pt row (8 leading, 16 trailing):
   `AtticIconButton(.keyboardArrowDown, label: lyrics_close_cd)`, the title in `titleMedium` weight
   600 (line 22) over the artist in `bodySmall` `onSurfaceVariant`.
2. The lines, 24 at the sides, 22pt under the bar:
   - **Synced:** a `ScrollView` of `SyncedLyricLine`s. The current line is kept a third of the way
     down, scrolled to on `AtticMotion.defaultSpatial` when the line changes, unless the person
     scrolled in the last 3 s. A blank line shows `lyric_blank_line` ("♪"). Tapping a line seeks to
     it.
   - **Plain:** paragraphs in `headlineSmall` weight 500, 28 apart.
   - **None:** 120pt down, centred: a 128pt clover on `secondaryContainer` with `.lyrics` at 48pt;
     24pt; `lyrics_not_downloaded`, `lyrics_not_found` or `lyrics_instrumental` in `bodyLarge`
     `onSurfaceVariant`; 24pt; `AtticButton(lyrics_download, leading: .download)` (`.refresh` after a
     failed try), or a `LoadingIndicator` while it downloads.
3. A bottom bar on `surfaceContainer`, 88pt above the home indicator, 24 leading, 20 trailing, 12
   between parts: the elapsed time, a `ProgressSlider(showsTimes: false)` that fills the row, the
   length (both `labelMedium` tabular `onSurfaceVariant`), and a large `PlayButton` 8pt further on.

### Settings

Pushed from any root bar. `DetailAppBar(settings_title, surface: .plain, then .scrolled once the
large title has gone)`; the bar's title is hidden until then. The page opens with `settings_title`
in `displaySmallEmphasized` tracking -0.4 (16 at the sides, 8 above).

1. `SettingsSection(settings_appearance)`; a `ListGroup` of one cell holding
   `ConnectedButtonGroup([settings_theme_system, settings_theme_light, settings_theme_dark],
   onGroupCell: true)` with 8 above and below.
2. `SettingsSection(settings_folders)`; a `ListGroup` with a `SettingsRow(title: folder name,
   summary: song_count, symbol: .folder)` per folder, ending in `AtticIconButton(.close, label:
   settings_folders_remove_cd)`, then `SettingsNavigationRow(title: settings_folders_add, symbol:
   .add)`, which opens the folder picker, and `SettingsNavigationRow(title: settings_folders_rescan,
   symbol: .refresh)`. Removing the last folder asks first (`.confirmationDialog`).
3. `SettingsSection(settings_lyrics)`; a `ListGroup` of `SettingsNote(settings_lyrics_privacy)`.
4. `SettingsSection(settings_about)`; a `ListGroup` of `SettingsRow(title: "Attic", summary:
   settings_about_body, symbol: .info)` (the summary's `%1$s` is `song_count`), `SettingsRow(title:
   settings_version, summary: the version)` and `SettingsNavigationRow(title: settings_licences,
   symbol: .openInNew)`.

Left out, as they are Android's: Style, volume levelling, transitions, the equaliser, song radio,
moods and backup.

### Open-source licences

- **The list:** `DetailAppBar(settings_licences)` and the large title as on Settings, then one
  `ListGroup` of `SettingsNavigationRow(title: the work, summary: its licence's name)`: Google Sans
  Flex (SIL Open Font License 1.1), Material Symbols (Apache License 2.0), Attic (GNU General Public
  License v3.0), and any Swift package the app links.
- **One licence:** `DetailAppBar(the work)`; the text in `bodyMedium` `onSurfaceVariant`, 16 at the
  sides, 6 above and below each paragraph, selectable. `LoadingState()` in a 200pt box while it
  loads.

### Empty and loading states

| Where | State |
|---|---|
| Any list while its data loads | `LoadingState()` filling the space under the bar |
| First run, scanning | `LoadingState(onboarding_scanning)` |
| Home, Library with no songs | `EmptyState(home_empty_title, home_empty_body, symbol: .musicOff, clover: true)` and a tonal "Add a folder" button |
| Favourites | `EmptyState(favourites_empty_title, favourites_empty_body, symbol: .favorite)` |
| A playlist with no songs | `EmptyState(playlist_empty_title, playlist_empty_body)` |
| Search with no matches | `EmptyState(search_no_results_title, search_no_results_body, symbol: .searchOff)` |
| The queue with nothing up next | `EmptyState(queue_nothing_up_next, body: nil, symbol: .queueMusic, clover: true)` |
| An album, artist or playlist that has gone | `EmptyState(collection_album_gone / collection_artist_gone / collection_playlist_gone, collection_gone_body, symbol: .album / .person / .queueMusic)` with a tonal `back` button (`leading: .arrowBack`), 72pt under the bar |

Snackbars: `message_playing_next`, `message_added_to_queue`, `message_added_to_playlist`.

### Recap

A stretch. If built, it is a card on Home under the hero row (`primaryContainer`, 28pt corners, 24
leading, 20 on the other sides: `home_recap_title_so_far` in `headlineSmallEmphasized` over
`home_recap_body` in `bodyMedium`, a 72pt `Mosaic` at the trailing edge) that opens a page of top
songs and top artists as ranked rows. The share cards and the story are not part of this pass.

## Copy

Use the Android strings as they are, with the Android names as keys in the string catalogue. Counts
are plurals with the number grouped ("1,204 songs"). These are the strings iOS adds or changes,
because Android's speak of permissions or of the phone's storage:

| Key | English |
|---|---|
| `ios_folder_body` | Attic plays the music files in a folder you choose, in Files or iCloud Drive. Nothing is copied or uploaded. |
| `ios_folder_row_title` | Music folder |
| `ios_folder_row_body` | Required. Attic plays the songs in this folder and the folders inside it. |
| `ios_folder_choose` | Choose |
| `ios_folder_choose_another` | Choose another folder |
| `home_empty_title` | No songs in your music folders |
| `home_empty_body` | Copy music files into a folder you have added, and they will show up here. |
| `settings_folders` | Music folders |
| `settings_folders_add` | Add a folder |
| `settings_folders_rescan` | Look for new songs |
| `settings_folders_remove_cd` | Remove %@ |
| `settings_folders_remove_title` | Remove %@? |
| `settings_folders_remove_body` | Its songs leave your library. The files stay where they are. |
| `settings_version` | Version |
| `settings_about_body` | Attic plays your own music files, from any folder. It has no account and no streaming. %@ in your library. |
| `collection_gone_body` | It may have been removed from your music folders. |
| `lyrics_not_downloaded` | Download once and they stay on this iPhone. |
| `settings_theme_system` | Match iPhone |
| `mini_player_open_hint` | Opens the player |
| `loading` | Loading |

The kit's own labels (Play, Pause, Previous, Next, Back, Clear search, Add to favourites, Remove
from favourites, More options for %@, the shuffle and repeat labels, Loading, Opens the player) are
in `AtticKitStrings`, a set of static variables the app may replace at launch to localise them.

## Accessibility

- Every control has a label; the tables above give each one. A row is one element that reads its
  title and subtitle; its trailing control is a second element.
- Headers (`SectionHeader`, `ListLabel`, `SettingsSection`, bar titles) carry the header trait.
- Toggles carry the selected trait and labels that say what a tap will do.
- The progress slider is adjustable: a swipe up or down moves 5% of the song and calls
  `onEditingChanged(false)`.
- Decoration is hidden: covers, the wave, glyphs inside buttons, the sheet handle.
- The least target is 48pt. A 40pt or 32pt control sits in a 48pt target.
- Text pairs meet 4.5:1 in both themes on the app scheme and on every cover scheme.
- With Reduce Motion on, the wave is flat, the loading indicator does not morph and the heart does
  not pop. Springs stay, as they move nothing across the screen.

## Differences from Android

| What | Android | iOS | Why |
|---|---|---|---|
| The app scheme | The wallpaper's | Fixed: the scheme the store screenshots show | iOS has no wallpaper scheme |
| The playing row's text and wave | Title in `onSurface`, wave in `primary` | Title, subtitle and wave in `onSecondaryContainer` | On the fixed scheme the Android colours fall to 3.2:1 on `secondaryContainer` |
| Navigation | A bottom bar or a rail, the kit's own transitions | A bottom tab bar, the system push | Platform habit; iPhone only |
| Home | A carousel whose items change width; instant mixes, mixes, moods, Recap | A plain horizontal scroll of covers; none of those | Out of scope for this pass |
| Now Playing | A pager with the next cover's sliver; lyrics, artist and credits cards below | One cover, swipe to skip; no cards; a lyrics button in the tools row | Scope; lyrics open from the button |
| Queue, lyrics | Overlays that rise from the bottom | System sheets | A swipe down closes them for free |
| Queue rows | A handle drags; a swipe either way removes | Press and hold drags; a swipe towards the leading edge removes | `List`'s own reordering and swipe actions |
| Menus, dialogs, switch | Material's, in the kit's colours | The system's `Menu`, alert, confirmation dialog and `Toggle` | Platform habit |
| Song row | No favourite mark | An optional 16pt heart after the text | Asked for in the iOS brief |
| First run | Asks for audio and notification access, offers a style | Asks for a music folder | iOS reads files only from a folder the person picks; one style |
| Swipe to queue | A song row swipes towards the end to queue | Not built; "Add to queue" is in the row's menu | Scope |
| Icons | 92 glyphs by codepoint | The same font and codepoints | |
| Tablet layouts | Rail, panes, player bar | None | iPhone first |
