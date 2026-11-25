# Design tokens

Every value on this page is read from the Kotlin or resource file linked beside it. When a value
here and the code disagree, the code is right and this page is out of date.

Rows whose first cell is a code expression in backticks, such as `Comfort.colors.ground`, are
compared with the code by [`DesignTokensTest`][test] on every test run. Change the code first, then
the row. [README.md](README.md#keeping-the-docs-in-step-with-the-code) has the steps.

Contents: [colour in Expressive](#colour-in-expressive) · [colour in Comfort](#colour-in-comfort) ·
[brand mark](#brand-mark) · [type](#type) · [icons](#icons) · [spacing](#spacing) ·
[shapes and radii](#shapes-and-radii) · [elevation](#elevation) · [motion](#motion) ·
[widget and window colours](#widget-and-window-colours) · [website palette](#website-palette) ·
[where other sources differ](#where-kitmd-the-mocks-and-figcraft-differ-from-the-code)

## Colour in Expressive

FigCraft: [`Attic Expressive.figcraft`][fx] › Colour

### The app scheme comes from the wallpaper

Expressive has no fixed palette. [`expressiveColorScheme(dark)`][xtheme-scheme] returns
`dynamicDarkColorScheme` or `dynamicLightColorScheme`, so every role follows the phone's wallpaper.
`minSdk` is 33, so there is no fallback scheme. The one change to the system's scheme is in dark:
`tertiaryContainer` is replaced by `tertiaryFixed` and `onTertiaryContainer` by
`onTertiaryFixedVariant`, so the Favourites container stays light in both themes.

Because the values move with the wallpaper, this page lists what each role is for, not a hex. The
`Wallpaper` variable collection in the FigCraft file holds one example scheme (the one the JVM
reference renders draw); it is an illustration, not a token set.

| Role | Used for | Where |
|---|---|---|
| `surface` | Screens | [`AtticTheme`][theme] |
| `surfaceContainerLow` | Bottom sheets | [`ExpressiveSheet`][xsurfaces] |
| `surfaceContainer` | Group cells, the navigation bar, menus, info cards | [`groupCellColor`][xlists-cellcolor], [`ExpressiveNavigationBar`][xnav] |
| `surfaceContainerHigh` | Pills, the search bar, the mini player, dialogs, group cells inside a sheet | [`SearchField`][xnav-search], [`ExpressiveMiniPlayer`][xshell-mini] |
| `surfaceContainerHighest` | A lifted row, unselected segments on a group cell | [`SongRow`][xlists-songrow], [`ConnectedButtonGroup`][xnav-seg] |
| `primary` / `onPrimary` | The one filled action on a screen, toggles that are on, the selected segment, the play control, the seek wave | [`Buttons.kt`][xbuttons], [`PlayControls.kt`][xplay] |
| `primaryContainer` | FABs, the Recap card on Home | [`ExpressiveFab`][xbuttons-fab], [`RecapCard`][xtiles] |
| `secondaryContainer` | The navigation indicator, a selected chip, the playing row, skip buttons, empty-state art | [`Navigation.kt`][xnav], [`Lists.kt`][xlists] |
| `tertiaryContainer` | Favourites and nothing else | [`FavouritesArt`][xcover], [`FavouritesPill`][xtiles] |
| `onSurfaceVariant` | Second lines, standard icon buttons, trailing glyphs | [`Lists.kt`][xlists] |
| `outline` | The sheet handle, lyric lines still to come | [`SheetHandle`][xsurfaces], [`LyricLine`][xsurfaces] |
| `outlineVariant` | Outlined button borders, sheet dividers, the unplayed part of the seek track | [`ExpressiveButton`][xbuttons], [`SeekBar`][xplay] |
| `error` / `errorContainer` | A destructive text button or menu item; what a swipe to remove uncovers | [`ExpressiveMenuItem`][xsurfaces], [`SwipeToRemove`][xlists] |
| `inverseSurface` | The snackbar | [`ExpressiveSnackbar`][xsurfaces] |

### Cover colour in Expressive

[`FromArt(seed)`][fromart] rebuilds every Material role from a cover and provides the result as
`MaterialTheme.colorScheme` for its content. [`FromCover(mediaId)`][fromart-cover] looks the seed up
itself, and [`rememberArtScheme(seed)`][fromart-remember] returns the scheme without providing it.

- **The seed** is `rememberArtColor(mediaId, fallback = Color.Unspecified)` from [`Art.kt`][art-remember]:
  the cover's Palette swatch (vibrant, else muted, else dominant), then deepened by
  [`deepenTint`][art-deepen], which caps HSL saturation at 0.62 and holds lightness between 0.2 and
  0.3. `Color.Unspecified` (a song with no art) keeps the scheme around it.
- **The scheme** is [`ArtScheme`][artscheme]. Only the seed's HCT hue is read. Each role has a fixed
  tone and chroma (the table below), so a dull cover and a loud one give the same contrast. Primary,
  secondary and the neutrals take the cover's hue; tertiary takes the hue turned 60°
  ([`paletteHue`][artscheme-hue]). HCT is implemented in [`Hct.kt`][hct]; chroma drops until the
  colour fits sRGB and tone is kept.
- **Roles the table does not define** (the error roles) come from the scheme around `FromArt`.
  `background` is `surface`, `surfaceVariant` is `surfaceContainerHighest` and `surfaceTint` is
  `primary` ([`colorScheme`][artscheme-scheme]).
- **A new seed** animates every role on [`ExpressiveMotion.slowEffects()`](#motion)
  ([`animateColorScheme`][fromart-animate]).
- **Where it is used:** the mini player, Now Playing, the queue, full-screen lyrics, collection and
  artist headers, and browse tiles. A list under a collection header stays on the app scheme. Recap
  cards use `ArtScheme.colorScheme(seed, dark = true)` in both themes.

Tone is CIE L\* (0 to 100) and chroma is CAM16 chroma, both from [`ArtScheme.kt`][artscheme-dark].

| Role | Palette | Dark tone | Dark chroma | Light tone | Light chroma |
|---|---|--:|--:|--:|--:|
| primary | Primary | 83.8 | 34.6 | 38.5 | 43.6 |
| onPrimary | Primary | 14.2 | 25.0 | 98.8 | 4.5 |
| primaryContainer | Primary | 32.7 | 33.8 | 84.9 | 28.4 |
| onPrimaryContainer | Primary | 91.9 | 18.1 | 22.3 | 35.0 |
| inversePrimary | Primary | 39.7 | 42.1 | 83.8 | 34.6 |
| secondary | Secondary | 81.4 | 20.5 | 37.4 | 23.0 |
| onSecondary | Secondary | 15.3 | 17.6 | 98.8 | 4.5 |
| secondaryContainer | Secondary | 25.8 | 23.1 | 87.8 | 18.6 |
| onSecondaryContainer | Secondary | 89.6 | 16.7 | 24.6 | 23.1 |
| tertiary | Tertiary (hue + 60°) | 91.9 | 21.0 | 35.0 | 35.5 |
| onTertiary | Tertiary | 28.1 | 32.2 | 98.8 | 4.5 |
| tertiaryContainer | Tertiary | 88.4 | 26.1 | 88.4 | 26.1 |
| onTertiaryContainer | Tertiary | 23.4 | 32.2 | 23.4 | 32.2 |
| surface | Neutral | 5.7 | 13.6 | 96.3 | 8.5 |
| surfaceDim | Neutral | 5.7 | 13.6 | 87.2 | 13.0 |
| surfaceBright | Neutral | 25.8 | 17.6 | 96.3 | 8.5 |
| surfaceContainerLowest | Neutral | 2.5 | 11.4 | 99.4 | 3.6 |
| surfaceContainerLow | Neutral | 8.9 | 15.2 | 94.2 | 10.2 |
| surfaceContainer | Neutral | 12.4 | 16.8 | 92.2 | 12.2 |
| surfaceContainerHigh | Neutral | 15.9 | 18.3 | 90.1 | 13.7 |
| surfaceContainerHighest | Neutral | 20.0 | 19.5 | 87.8 | 15.3 |
| onSurface | Neutral | 93.0 | 9.2 | 9.5 | 13.7 |
| inverseSurface | Neutral | 93.0 | 9.2 | 14.2 | 13.7 |
| inverseOnSurface | Neutral | 18.8 | 13.7 | 94.2 | 9.2 |
| onSurfaceVariant | NeutralVariant | 79.1 | 15.0 | 30.4 | 17.5 |
| outline | NeutralVariant | 58.2 | 15.3 | 51.3 | 17.3 |
| outlineVariant | NeutralVariant | 30.4 | 17.5 | 79.1 | 16.9 |

[`ArtSchemeTest`][artscheme-test] holds every pair in [`ArtScheme.textPairs`][artscheme-pairs] to
4.5:1 or more for 223 seeds in both themes. [rules.md](rules.md#contrast) lists the pairs.

In the FigCraft file the `Cover` collection holds the scheme for the example cover, the `Art`
collection one scheme per demo album, and the `Color` collection has a mode for each, so a frame
switches cover by switching mode.

## Colour in Comfort

FigCraft: [`Attic Comfort.figcraft`][fc] › Colour

### The palette

Comfort has a fixed palette: [`ComfortDark`][cc-dark] and [`ComfortLight`][cc-light], both
[`ComfortColors`][cc]. Read it with `Comfort.colors` inside a Comfort theme. There is one accent,
amber, and no wallpaper colour. A value with eight hex digits ends in its alpha byte.

| Token | Dark | Light | What it is for |
|---|---|---|---|
| `Comfort.colors.ground` | `#000000` | `#F2F1EE` | Night / Plaster: the screen |
| `Comfort.colors.card` | `#171716` | `#FFFFFF` | Loft / Paper: groups, cards, the deck |
| `Comfort.colors.raised` | `#232221` | `#FFFFFF` | Rafter: things that float (dock, sheets, dialogs, menus) |
| `Comfort.colors.fill` | `#2C2B29` | `#EEECE8` | Slate / Putty: control tracks, tonal icon buttons, chips on a card |
| `Comfort.colors.fill2` | `#3B3936` | `#DCD9D3` | The switch-off track, the progress track, the grab handle |
| `Comfort.colors.line` | `#2A2928` | `#EBE9E4` | Seam: inset hairlines |
| `Comfort.colors.edge` | `#FFFFFF12` | `#281E0A0F` | A 1dp ring around floating pieces, drawn with their shadow |
| `Comfort.colors.on` | `#F5F4F1` | `#191817` | Chalk / Ink: text and icons |
| `Comfort.colors.on2` | `#AAA7A2` | `#67645F` | Dust: secondary text |
| `Comfort.colors.on3` | `#73706B` | `#9C9993` | Ash: hints, placeholders, trailing icons. Below 4.5:1 by design; never body text |
| `Comfort.colors.accent` | `#FFB347` | `#F2A024` | Lamplight: fills, the one filled button, switches, progress, selection |
| `Comfort.colors.onAccent` | `#2A1800` | `#2A1800` | Soot: ink on the accent |
| `Comfort.colors.accentInk` | `#FFC063` | `#985600` | The accent as text or an icon on a neutral surface (Brass in light) |
| `Comfort.colors.accentSoft` | `#3A2A13` | `#FBE8C9` | The tonal container: selected tab, current row, tonal button |
| `Comfort.colors.onAccentSoft` | `#FFCD85` | `#7A4500` | Ink on the tonal container |
| `Comfort.colors.danger` | `#FF7A6B` | `#CF3A2A` | Ember: delete, remove |
| `Comfort.colors.onArt` | `#FFFFFF` | `#FFFFFF` | Marks drawn on artwork (the playing bars over a dimmed cover) |
| `Comfort.colors.knob` | `#FFFFFF` | `#FFFFFF` | The switch knob |
| `Comfort.colors.toast` | `#3B3936` | `#3D3B38` | The toast's capsule |
| `Comfort.colors.onToast` | `#F5F4F1` | `#FFFFFF` | Text on the toast |
| `Comfort.colors.scrim` | `#0000009E` | `#18140C66` | Behind sheets and dialogs |
| `Comfort.colors.shadow` | `#000000B3` | `#3C2D1424` | The floating shadow's colour (see [elevation](#elevation-in-comfort)) |
| `Comfort.colors.shadowSmall` | `#00000073` | `#3C2D1429` | The small shadow's colour |
| `Comfort.colors.miniBase` | `#232221` | `#FFFFFF` | The mini player's ground |
| `Comfort.colors.miniFill` | `#2C2B29` | `#EEECE8` | The mini player's played part |
| `Comfort.colors.miniInk2` | `#AAA7A2` | `#67645F` | The mini player's second line |

`miniInk2` is the one token the mocks do not have. `kit.css` draws the mini player's second line in
`--on-2`, which falls to 3.6:1 over a cover-coloured fill ([`ComfortColors`][cc] class note).

Rules the tokens cannot enforce, from [`comfort/KIT.md`][ckit]:

- Amber marks selection, the one filled button on a screen, switches and progress.
- Titles and row text stay in `on`. A current song is never amber text.
- In light, amber as text or an icon is `accentInk`, never `accent`.

In the FigCraft file the `Palette` collection holds these values with Light and Dark modes (the
accent tokens sit in its `lamplight/` group), and each variable's code syntax is its Compose name.

### How stock Material components get the palette

[`ComfortColors.toColorScheme()`][cc-scheme] maps the tokens onto a Material `ColorScheme`, so a
stock sheet, menu or text field inside a Comfort theme draws in the kit's colours.

| Material role | Comfort token |
|---|---|
| primary, onPrimary | accent, onAccent |
| primaryContainer, secondaryContainer, tertiaryContainer | accentSoft |
| onPrimaryContainer, onSecondaryContainer, onTertiaryContainer | onAccentSoft |
| secondary, tertiary, inversePrimary | accentInk |
| onSecondary, onTertiary | ground |
| background, surface, surfaceDim, surfaceContainerLowest | ground |
| surfaceContainerLow, surfaceContainer | card |
| surfaceContainerHigh, surfaceBright | raised |
| surfaceContainerHighest, surfaceVariant | fill |
| onBackground, onSurface | on |
| onSurfaceVariant | on2 |
| outline | on3 |
| outlineVariant | line |
| inverseSurface, inverseOnSurface | toast, onToast |
| error | danger |
| onError | `#2A0A06` dark, white light |
| errorContainer, onErrorContainer | `#3D1A16` and `#FFB4A9` dark; `#FBE0DC` and `#8C1D12` light |
| scrim | scrim |
| surfaceTint | transparent, so nothing tints by elevation |

### Cover colour in Comfort

[`Tinted(seed, wash)`][tinted] replaces some tokens with ones computed from a cover, and rebuilds
`MaterialTheme.colorScheme` from the result. [`TintedCover(mediaId, wash)`][tinted-cover] looks the
seed up itself.

- **The seed** is `rememberArtColor(mediaId, Color.Unspecified, deepen = false)`: the cover's
  Palette swatch as found ([`coverSwatch`][art-swatch]), not the deepened tint Expressive uses. The
  rule below keeps the cover's chroma, so a dulled seed gives a dull player. `Color.Unspecified`
  keeps the palette around it, which is amber.
- **The rule** is [`ComfortTint.apply`][tint-apply]. The seed is converted to OKLCH
  ([`Oklch.kt`][oklch]). Each token keeps the seed's hue, multiplies its chroma by a fixed factor
  and sets its own lightness. Only hue and chroma are read from the seed; its lightness is ignored.
  This is `kit.css`'s `oklch(from var(--tint) L calc(c*k) h)`.
- **Out of gamut**, chroma is reduced at the same lightness and hue until the colour fits sRGB
  ([`Oklch.toArgb`][oklch-toargb]), so the lightness, and with it the contrast, never moves.
- **`Tinted(seed)`** moves the accent tokens and the mini player's tokens. **`wash = true`** also
  moves the surfaces and the quieter inks, `on2` and `on3`. `on`, `edge`, `danger`, `onArt`, `knob`, `toast`,
  `onToast`, `scrim` and the shadows never change.
- **Nesting:** a token the rule does not name keeps the value around it, so a `Tinted` inside a
  washed one keeps the washed surfaces.
- **A new seed** animates every moved token over 450 ms ([`TintSpec`](#motion-in-comfort)).
- **Where it is used:** Now Playing, the queue and full-screen lyrics are `Tinted(wash = true)` on
  the whole screen. The mini player tints itself from its `seed`. A browse tile with a seed is
  washed. The widget's card is washed. Sheets opened over the player are washed by their caller.
  Recap cards are always dark; the top song, top artist and summary cards are washed with their
  cover.

Lightness is OKLCH lightness (0 to 1); "× chroma" is the factor on the seed's chroma. Values from
[`Tinted.kt`][tint-tables].

| Token | Moved by | Dark lightness | Dark × chroma | Light lightness | Light × chroma |
|---|---|--:|--:|--:|--:|
| accent | `Tinted` | 0.76 | 1.0 | 0.47 | 1.0 |
| onAccent | `Tinted` | 0.2 | 0.55 | plain white | n/a |
| accentInk | `Tinted` | 0.82 | 0.85 | 0.43 | 1.0 |
| accentSoft | `Tinted` | 0.31 | 0.6 | 0.9 | 0.4 |
| onAccentSoft | `Tinted` | 0.89 | 0.5 | 0.36 | 1.0 |
| miniBase | `Tinted` | 0.25 | 0.5 | 0.955 | 0.16 |
| miniFill | `Tinted` | 0.4 | 0.9 | 0.87 | 0.42 |
| miniInk2 | `Tinted` | 0.82 | 0.3 | 0.45 | 0.3 |
| ground | `wash` | 0.115 | 0.28 | 0.93 | 0.2 |
| card | `wash` | 0.215 | 0.34 | 0.978 | 0.08 |
| raised | `wash` | 0.26 | 0.34 | 0.985 | 0.06 |
| fill | `wash` | 0.3 | 0.4 | 0.905 | 0.26 |
| fill2 | `wash` | 0.38 | 0.45 | 0.84 | 0.34 |
| line | `wash` | 0.28 | 0.36 | 0.915 | 0.14 |
| on2 | `wash` | 0.77 | 0.3 | 0.45 | 0.3 |
| on3 | `wash` | 0.57 | 0.26 | 0.62 | 0.26 |

[`ComfortTintTest`][tint-test] holds every pair in [`ComfortTint.textPairs`][tint-pairs] to 4.5:1
for 1,145 seeds, dark and light, tinted and washed. [rules.md](rules.md#contrast) lists the pairs
and the `on3` exception.

In the FigCraft file the `Tinted` collection holds the values `Tinted.kt` computes for the example
cover (seed `#F07858`), and the `Color` collection has the modes Base, Tinted and Tinted wash,
which is how a frame switches between them.

## Brand mark

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / System and › System surfaces;
[`Attic Comfort.figcraft`][fc] › Components / System and › System surfaces. Both files hold the
mark's colours in a `Brand` variable collection (`headroom/ground`, `headroom/cabinet`,
`headroom/cone`).

The mark is "Headroom": a speaker cabinet cut to the slope of a roof, with the woofer (its dust cap
left solid) and the tweeter as holes. The source drawings are
[`sound-2-headroom.svg`][logo-mono] (one colour) and [`sound-2-headroom-color.svg`][logo-color], on
a 108-unit canvas.

| Colour | Hex | Resource | Where |
|---|---|---|---|
| Terracotta, the ground | `#C6533A` | `@color/headroom_ground` | The launcher icon's background layer |
| Cream, the cabinet | `#F5EDDD` | `@color/headroom_cabinet` | The launcher icon's foreground |
| Dark, the cone and tweeter | `#2A211C` | `@color/headroom_cone` | The launcher icon's foreground |

These resources are in [`res/values/colors.xml`][colors]. The launcher icon is one adaptive icon
for both styles ([`ic_launcher.xml`][launcher]), with a monochrome layer for themed icons.

Inside the app the mark is never drawn in the brand colours. [`AtticMarkVector`][mark] is one
even-odd path cropped to the glyph's bounding box, 36 wide by 51.5 tall, drawn in a colour from the
scheme:

| Style | Composable | Size | Colour |
|---|---|---|---|
| Expressive | [`AtticMarkIcon(size, tint)`][mark-icon] | `size` tall and about 0.7 of that wide; 24dp by default | `LocalContentColor` unless given |
| Expressive | [`AtticWordmark(text, style)`][mark-wordmark] | 32dp mark, 10dp gap, then "Attic"; Home's bar shrinks the mark to 24dp once scrolled | `primary` |
| Comfort | [`ComfortMark(size, tint)`][cart-mark] | 24dp; 30dp above Home's hero title | `accent`, so the cover's colour inside `Tinted` |
| Comfort | [`MarkBox(size, style)`][cart-markbox] | The mark at half the width of an icon box | The icon box's ink |

The widget's corner mark is [`ic_mark.xml`][ic-mark], tinted where used.

## Type

FigCraft: [`Attic Expressive.figcraft`][fx] › Type; [`Attic Comfort.figcraft`][fc] › Type

### Type in Expressive: Google Sans Flex

One variable font, `res/font/google_sans_flex.ttf`, with the axes below ([`GoogleSansFlex`][xtype-font]):

| Axis | Range in the bundled font | How the scale uses it |
|---|---|---|
| Weight (`wght`) | 300 to 800 | Per style, below. Families are built at 300, 400, 500, 600, 700 and 800 |
| Width (`wdth`) | 50 to 100 | 100. [`narrowed(75)`][xtype-narrowed] and `narrowed(50)` set a title narrower |
| Roundness (`ROND`) | 0 to 100 | 0 for the plain styles, 100 for the emphasised ones |
| Optical size (`opsz`) | 12 to 48 | Fixed per style from its size, because Android does not apply it automatically |

[`opticalSizeFor`][xtype-opsz] picks the optical size: 12 up to 12sp, 14 up to 14sp, 16 up to 18sp,
22 up to 24sp, 28 up to 30sp, 36 up to 40sp and 45 above.

The plain styles are `MaterialTheme.typography`. Each has an emphasised partner, heavier and fully
rounded, read as `Expressive.type.titleLargeEmphasized` and so on. Size and line height are in sp.
Values from [`ExpressiveTypography`][xtype-scale].

| Style | Size/line | Weight | Tracking | Optical size | Round | FigCraft text style |
|---|---|--:|---|--:|---|---|
| `Expressive.type.displayLarge` | 57/64 | 400 | -0.25sp | 45 | no | `Display/Large` |
| `Expressive.type.displayMedium` | 45/52 | 400 | -0.5sp | 45 | no | `Display/Medium` |
| `Expressive.type.displaySmall` | 36/44 | 400 | -0.3sp | 36 | no | `Display/Small` |
| `Expressive.type.headlineLarge` | 32/40 | 400 | 0 | 36 | no | `Headline/Large` |
| `Expressive.type.headlineMedium` | 28/36 | 400 | 0 | 28 | no | `Headline/Medium` |
| `Expressive.type.headlineSmall` | 24/32 | 400 | 0 | 22 | no | `Headline/Small` |
| `Expressive.type.titleLarge` | 22/28 | 400 | 0 | 22 | no | `Title/Large` |
| `Expressive.type.titleMedium` | 16/24 | 500 | 0.1sp | 16 | no | `Title/Medium` |
| `Expressive.type.titleSmall` | 14/20 | 500 | 0.1sp | 14 | no | `Title/Small` |
| `Expressive.type.bodyLarge` | 16/24 | 400 | 0 | 16 | no | `Body/Large` |
| `Expressive.type.bodyMedium` | 14/20 | 400 | 0 | 14 | no | `Body/Medium` |
| `Expressive.type.bodySmall` | 12/16 | 400 | 0 | 12 | no | `Body/Small` |
| `Expressive.type.labelLarge` | 14/20 | 500 | 0 | 14 | no | `Label/Large` |
| `Expressive.type.labelMedium` | 12/16 | 500 | 0 | 12 | no | `Label/Medium` |
| `Expressive.type.labelSmall` | 11/16 | 500 | 0 | 12 | no | `Label/Small` |
| `Expressive.type.displayLargeEmphasized` | 57/64 | 700 | -0.25sp | 45 | yes | `Display/Large Emphasized` |
| `Expressive.type.displayMediumEmphasized` | 45/52 | 700 | -0.5sp | 45 | yes | `Display/Medium Emphasized` |
| `Expressive.type.displaySmallEmphasized` | 36/44 | 700 | -0.3sp | 36 | yes | `Display/Small Emphasized` |
| `Expressive.type.headlineLargeEmphasized` | 32/40 | 700 | 0 | 36 | yes | `Headline/Large Emphasized` |
| `Expressive.type.headlineMediumEmphasized` | 28/36 | 700 | 0 | 28 | yes | `Headline/Medium Emphasized` |
| `Expressive.type.headlineSmallEmphasized` | 24/32 | 700 | 0 | 22 | yes | `Headline/Small Emphasized` |
| `Expressive.type.titleLargeEmphasized` | 22/28 | 700 | 0 | 22 | yes | `Title/Large Emphasized` |
| `Expressive.type.titleMediumEmphasized` | 16/24 | 600 | 0.1sp | 16 | yes | `Title/Medium Emphasized` |
| `Expressive.type.titleSmallEmphasized` | 14/20 | 600 | 0.1sp | 14 | yes | `Title/Small Emphasized` |
| `Expressive.type.bodyLargeEmphasized` | 16/24 | 600 | 0 | 16 | yes | `Body/Large Emphasized` |
| `Expressive.type.bodyMediumEmphasized` | 14/20 | 600 | 0 | 14 | yes | `Body/Medium Emphasized` |
| `Expressive.type.bodySmallEmphasized` | 12/16 | 700 | 0 | 12 | yes | `Body/Small Emphasized` |
| `Expressive.type.labelLargeEmphasized` | 14/20 | 600 | 0 | 14 | yes | `Label/Large Emphasized` |
| `Expressive.type.labelMediumEmphasized` | 12/16 | 600 | 0 | 12 | yes | `Label/Medium Emphasized` |
| `Expressive.type.labelSmallEmphasized` | 11/16 | 700 | 0 | 12 | yes | `Label/Small Emphasized` |

Helpers in [`Type.kt`][xtype]:

| Need | Call |
|---|---|
| The emphasised partner of any kit style | [`style.emphasized()`][xtype-emphasized], bold unless given a weight |
| A narrower setting | [`style.narrowed(75)`][xtype-narrowed], `style.narrowed(50)` |
| A title that narrows before it truncates | [`NarrowingText(text, style = …)`][xtype-narrowing]: measures at width 100, 75, then 50, then ellipsizes |
| Secondary text | `color = MaterialTheme.colorScheme.onSurfaceVariant` |
| Tabular figures | `style.copy(fontFeatureSettings = "tnum")` |

Components override a style where the design needs it (a row title is `bodyLarge` at weight 500).
The FigCraft file records those as `Kit/…` text styles; in code they are `copy(…)` calls beside the
component.

### Type in Comfort: Onest

One variable font, `res/font/onest.ttf`, weight 400 to 800, Latin. [`Onest`][ctype-font] pins the
weight axis at 400, 500, 600, 700 and 800, so `FontWeight(600)` is a true semibold. Other scripts
fall back to the system font.

Size and line height are in sp. Tracking is in em, as `kit.css` writes it; the code multiplies it
by the size ([`onest()`][ctype-onest]). Every style centres its text in the line box, as CSS does
(`CssLineHeight`). Values from [`ComfortTypography`][ctype-scale].

| Style | Size/line | Weight | Tracking | Mock class | Material slot | FigCraft text style |
|---|---|--:|---|---|---|---|
| `Comfort.type.hero` | 40/46 | 700 | -0.025em | `.hero h1`, the expanded large title | displayLarge, displayMedium | `Scale/Hero` |
| `Comfort.type.title` | 26/32 | 700 | -0.02em | `.c-head h1`, `.np-title h1`: song and collection titles | headlineMedium | `Scale/Title` |
| `Comfort.type.personTitle` | 30/36 | 700 | -0.02em | `.c-head.person h1`: an artist's name | displaySmall, headlineLarge | `Scale/Person title` |
| `Comfort.type.barTitle` | 20/26 | 700 | -0.015em | `.bar h2`, sheet, dialog and empty-state titles | titleLarge | `Scale/Bar title` |
| `Comfort.type.rowTitle` | 16/22 | 500 | 0 | `.txt b`: row titles | titleMedium | `Scale/Row title` |
| `Comfort.type.rowTitleLarge` | 18/24 | 700 | -0.01em | `.row.lg .txt b` | none | `Scale/Row title large` |
| `Comfort.type.body` | 15/21 | 400 | 0 | Dialog and empty-state text | bodyLarge | `Scale/Body` |
| `Comfort.type.sub` | 13.5/18 | 400 | 0 | `.txt span`: a row's second line | bodyMedium | `Scale/Sub` |
| `Comfort.type.caption` | 12.5/16 | 400 | 0 | `.txt em`, tile second lines | bodySmall | `Scale/Caption` |
| `Comfort.type.label` | 14/18 | 600 | 0 | `.label`, `.lyrics-card .hd`: section labels | titleSmall | `Scale/Label` |
| `Comfort.type.button` | 15.5/20 | 600 | 0 | `.btn` | labelLarge | `Scale/Button` |
| `Comfort.type.stat` | 26/32 | 700 | -0.02em | `.stat b`, with tabular figures | none | `Scale/Stat` |
| `Comfort.type.lyric` | 22/28 | 700 | -0.015em | `.lyrics-card .ln` | headlineSmall | `Scale/Lyric` |

The Material slots `labelMedium` and `labelSmall` have no named style of their own. `labelMedium`
is `onest(14f, 18f, 500)` and `labelSmall` is `onest(12f, 15f, 500)`
([`ComfortTypography.material`][ctype-material]).

`onest(size, line, weight, tracking)` builds any other style, and
[`style.tabular()`][ctype-tabular] turns on tabular figures. Components set many sizes of their own
this way (a settings title is 16.5/22, a tab label 14/18). The FigCraft file records those as
`Component/…` text styles.

## Icons

FigCraft: [`Attic Expressive.figcraft`][fx] › Icons; [`Attic Comfort.figcraft`][fc] › Icons

Both styles draw Material Symbols Rounded from one bundled subset,
`res/font/material_symbols_rounded.ttf`, with the FILL (0 to 1), weight (300 to 700) and optical
size (20 to 48) axes kept. [`Symbol`][symbols-enum] is an enum of the 90 glyphs in the font, each
addressed by codepoint. A name that is not in the enum is not in the font; the enum's doc says how
to add one.

| | Expressive | Comfort |
|---|---|---|
| Composable | [`SymbolIcon(symbol, size, filled, tint, weight)`][symbols-icon] | [`ComfortIcon(symbol, size, filled, tint, weight)`][ctheme-icon] |
| Weight | 400 | 300 ([`ComfortIconWeight`][ctheme-weight]); the play, pause and skip glyphs are 400 |
| Default size | 24dp | 24dp |
| Sizes in the kit | 18, 20, 24, 28, 32, 36, 40dp | 18, 20, 24, 28, 32, 40dp |
| Filled | The selected state and the transport glyphs | The selected tab, a favourite that is on, play and pause |

The font is set at optical size 20 below 24dp, 24 below 40dp and 40 from there
([`opticalSizeFor`][symbols-opsz]).

## Spacing

FigCraft: [`Attic Expressive.figcraft`][fx] › Spacing, radii and shapes;
[`Attic Comfort.figcraft`][fc] › Spacing, radii and shapes

There is no central spacing object in the code. Spacing is written inline, in dp, in each
component. Some values have names because more than one file reads them:
`ExpressiveShapes.GroupGap` and `ExpressiveShapes.GroupSpacing` in Expressive; `GroupMargin`,
`GroupSpacing`, `ComfortPadding` and `SetBlockProgressPadding` in Comfort; and `LocalBottomPadding`,
which both shells provide. The tables below list the values the kits use and where each is written,
so a new screen can match them.

The FigCraft files do have spacing variables (a `Dimension` collection with `space/…` steps and, in
Comfort, aliases such as `space/group-margin`). Those names belong to the design files only.

### Spacing in Expressive

| What | Value | Where it is written |
|---|---|---|
| Screen margin | 16dp | [`Modifier.groupCell`][xlists-cell] (`horizontalMargin`), [`Shelf`][xtiles-shelf], [`TileGrid`][xtiles-grid], [`ChipRow`][xnav-chips], [`SectionHeader`][xnav-section] |
| Now Playing's side margin | 24dp | [`SideMargin`][xnp-margin] |
| Between rows of a group | 2dp | `ExpressiveShapes.GroupGap` (see [shapes](#shapes-in-expressive)) |
| Between two groups | 12dp | `ExpressiveShapes.GroupSpacing` |
| Song row | 64dp minimum; 12dp start, 4dp end, 8dp top and bottom; 12dp between lead, text and trailing | [`SongRow`][xlists-songrow] |
| Collection row | 72dp minimum, 56dp lead, same padding as a song row | [`CollectionRow`][xlists-collrow] |
| Section header | 28dp above, 12dp below, 16dp at the sides | [`SectionHeader`][xnav-section] |
| List label | 20dp above, 8dp below, 32dp at the sides | [`ListLabel`][xnav-listlabel] |
| Shelf | 12dp between tiles, 16dp at the edges | [`Shelf`][xtiles-shelf] |
| Tile grid | 12dp between columns; 20dp between rows, 16dp with three columns | [`TileGrid`][xtiles-grid] |
| Chips | 8dp apart | [`ChipRow`][xnav-chips] |
| Connected segments | 2dp apart, 16dp at the sides, 4dp above and below | [`ConnectedButtonGroup`][xnav-seg] |
| Button group | 8dp apart | [`ButtonGroup`][xbuttons-group] |
| Sheet | 24dp side padding on rows and headers; the handle has 16dp above and 12dp below; 32dp under the last row | [`ExpressiveSheet`][xsurfaces], [`SheetHandle`][xsurfaces], [`SheetRow`][xsurfaces] |
| Settings row | 64dp minimum; 16dp at the sides, 12dp top and bottom; 16dp between parts | [`SetRow`][xsurfaces] |
| Settings section heading | 24dp above, 8dp below, 32dp at the sides | [`SettingsSection`][xsurfaces] |
| Empty state | 32dp at the sides, 48dp above and below | [`EmptyState`][xsurfaces] |
| Mini player in the dock | 64dp tall, 12dp in from the edges, 8dp above the navigation bar | [`ExpressiveShell.kt`][xshell-sizes] |
| Room a scrolling screen leaves for the dock | The navigation bar's height, plus the mini player and its gap when a song is queued, plus 24dp | `LocalBottomPadding`, set in [`ExpressiveShell`][xshell] |
| A FAB above the dock | 12dp: `padding(bottom = LocalBottomPadding.current - 12.dp)` | [`ExpressiveShell`][xshell] doc |
| Snackbar | 16dp above the dock, or 24dp above the system navigation inset while an overlay is open | [`ExpressiveShell`][xshell] |

### Spacing in Comfort

| What | Value | Where it is written |
|---|---|---|
| A group's margin from the screen edges | 16dp | [`GroupMargin`][cgroups-margin] |
| Between two groups | 12dp | [`GroupSpacing`][cgroups-spacing] |
| Section label | 28dp at the sides; 24dp above (14dp when `tight`), 9dp below | [`Label`][cgroups-label] |
| Note under a group | 28dp at the sides, 10dp above | [`Note`][cgroups] |
| Padded group (a grid inside a card) | 14dp at the sides, 18dp above, 16dp below | [`Group(pad = true)`][cgroups-group] |
| Collection row | 72dp minimum with 56dp art (64 with 48 for `Small`, 92 with 64 for `Large`); 16dp start, 10dp end, 8dp top and bottom; 14dp between parts | [`RowSize`][crows-size], [`CollectionRow`][crows-collrow] |
| Song row | 64dp minimum, 48dp cover; 16dp start, 4dp end, 8dp top and bottom; 14dp between parts | [`SongRow`][crows-songrow] |
| Rows inside a sheet | 24dp start (the sheet's margin) | [`SongRow`][crows-songrow], [`CollectionRow`][crows-collrow] |
| Hairline between rows | Starts where the row's text starts (78dp for a song row, 58 numbered, 112 ranked, 20 bare; 78, 86 or 94 for a collection row by size) and ends 20dp from the end edge | [`hairlineAbove`][cgroups-hairline] |
| Shelf | 14dp between tiles; 24dp at the edges, 16dp inside a group | [`Shelf`][ctiles-shelf] |
| Pager | 312dp cards, 10dp apart, 16dp at the edges | [`Pager`][ctiles-pager] |
| Hero | 228dp tall (150 when `short`), 32dp at the sides | [`Hero`][cbars-hero] |
| Tools row | 50dp tall; 24dp start, 8dp end | [`Tools`][cbars-tools] |
| Collapsed bar | 56dp tall, 8dp at the sides | [`Bar`][cbars-bar] |
| Dock | 12dp in from the edges; the card starts 28dp into a 36dp fade to the ground ([`DockFade`][cdock-fade]); 22dp from the bottom edge, or the navigation inset plus 6dp when that is taller | [`Dock`][cdock] |
| Tab bar | 60dp tall with 8dp padding; tabs 44dp tall and 4dp apart | [`TabBar`][cdock-tabs] |
| Now Playing's cover and title | 30dp in from the edges | [`NowPlayingCover`][cnp], [`NowPlayingTitle`][cnp] |
| Deck | 16dp margin, 14dp padding; transport buttons 26dp apart | [`Deck`][cnp], [`Transport`][cnp] |
| Sheet | 24dp side margin; rows 52dp minimum with 18dp between icon and label; 30dp under the last row | [`ComfortSheet`][coverlays], [`SheetRow`][coverlays] |
| Dialog | 12dp in from the edges, 22dp above the bottom; 24dp padding | [`ComfortDialog`][coverlays], [`DialogCard`][coverlays] |
| Settings row | 60dp minimum; 20dp at the sides, 11dp top and bottom; 16dp between parts | [`SetRow`][ccontrols] |
| Empty state | 40dp at the sides, 48dp above and below | [`EmptyState`][ccontrols] |
| Action bar | 68dp tall, 12dp in from the edges, 22dp up | [`ActionBar`][ccontrols] |
| Room a scrolling screen leaves for the dock | The dock's measured height, its fade included (176dp with the mini player and 112dp without, until it is measured) | `LocalBottomPadding`, set in [`ComfortShell`][cshell]; [`ComfortPadding`][cgroups-padding] holds the two fixed values |
| Toast | 6dp above the dock's height, or 32dp above the system navigation inset while an overlay is open | [`ComfortShell`][cshell] |

## Shapes and radii

FigCraft: [`Attic Expressive.figcraft`][fx] › Spacing, radii and shapes;
[`Attic Comfort.figcraft`][fc] › Spacing, radii and shapes

### Shapes in Expressive

The corner scale and the group distances are in [`ExpressiveShapes`][xshapes].

| Token | Value | Mock variable | Material slot | Used for |
|---|--:|---|---|---|
| `ExpressiveShapes.ExtraSmall` | 4dp | `--r-xs` | extraSmall | The inner corners of a group's rows |
| `ExpressiveShapes.Small` | 8dp | `--r-s` | small | Covers up to 48dp, chips, the snackbar |
| `ExpressiveShapes.Medium` | 12dp | `--r-m` | medium | Covers up to 64dp, a 40dp toggle or button that is on |
| `ExpressiveShapes.Large` | 16dp | `--r-l` | large | Covers up to 128dp, FABs, menus, a 56dp toggle or button that is on, the mini player's cover |
| `ExpressiveShapes.LargeIncreased` | 20dp | `--r-li` | none | The outer corners of a group, a lifted row, covers up to 180dp |
| `ExpressiveShapes.ExtraLarge` | 28dp | `--r-xl` | extraLarge | Covers above 180dp, sheets, dialogs, info cards, the wide play control while playing |
| `ExpressiveShapes.ExtraLargeIncreased` | 32dp | `--r-xli` | none | The bottom corners of a collection's header block |
| `ExpressiveShapes.ExtraExtraLarge` | 48dp | `--r-xxl` | none | A collection's play button while idle (28dp while it plays); Now Playing's paused cover uses the same 48dp |
| `ExpressiveShapes.GroupGap` | 2dp | `.group` gap | none | The gap between rows of a group |
| `ExpressiveShapes.GroupSpacing` | 12dp | `.group + .group` | none | The space between two groups |

Other shape rules in [`Shapes.kt`][xshapes]:

| Rule | In code | Values |
|---|---|---|
| Buttons, bars and pills are fully round | `ExpressiveShapes.Pill` | `CircleShape` |
| A cover's radius follows its size | [`coverRadius(size)`][xshapes-cover], `cover(size)`, and [`CoverShape`][xshapes-covershape], which measures itself | 8dp up to 48dp, 12 up to 64, 16 up to 128, 20 up to 180, 28 above |
| A list is a group | [`groupItemShape(index, count)`][xshapes-group] | 20dp on the group's outer corners, 4dp inside |
| Squared off means on | [`onCorner(height)`][xshapes-oncorner], used by `ExpressiveToggleButton` | 11dp up to a 36dp control, 12 up to 40, 16 up to 56, 28 above |
| The play control's corner while playing | Set in [`PlayButton`][xplay-button] itself | 11dp (36dp control), 18dp (56dp), 28dp (the wide one); half the height while paused |
| A labelled button that is on (`squared`) | Set in [`ExpressiveButton`][xbuttons-button] itself | 12dp at 40dp tall, 16dp at 56dp |
| Artists are the nine-lobed cookie, never a circle | [`CookieShape`][xshapes-cookie] | Material's Cookie9Sided, from the mock's polygon |
| "Nothing here yet" | [`CloverShape`][xshapes-clover] | Material's Clover4Leaf; the empty library's 128dp picture |
| Recap's play-count badge | [`SunnyShape`][xshapes-sunny] | Material's Sunny, eight shallow lobes |
| A soft square on Recap's intro card | [`PuffShape`][xshapes-puff] | Inset 6.5 percent from its box |
| The loader | [`BurstShape`][xshapes-burst], [`LoaderShapes`][xshapes-loader] | Seven shapes the loading indicator morphs through |

The cookie, clover, sunny, puff and burst are [`RadialShape`][xshapes-radial]s: a radius at each of
180 angles, which is what lets the loading indicator blend one into the next.

### Shapes in Comfort

Everything is in [`ComfortShapes`][cshapes].

| Token | Value | Mock variable | Used for |
|---|--:|---|---|
| `ComfortShapes.FloatRadius` | 30dp | `--r-float` | Things that float: the dock, the deck, sheets (top corners), dialogs, the action bar. Shapes `Floating`, `FloatingTop` |
| `ComfortShapes.GroupRadius` | 26dp | `--r-group` | Groups and cards. Shape `Group` |
| `ComfortShapes.ControlRadius` | 18dp | `--r-ctl` | A control nested in a card: the seek bar, the current row's bed, a lifted row. Shape `Control` |
| `ComfortShapes.MenuRadius` | 22dp | `.menu` | The pop-over menu. Shape `Menu` |
| `ComfortShapes.LargeArtFrom` | 100dp | `.sq-lg` | The size from which artwork takes the larger superellipse |

Every image and icon container is a superellipse, `|x|^n + |y|^n = 1` stretched to its box
([`SuperellipseShape`][cshapes-super]). Never a rounded rectangle on artwork, and never a circle for
an artist.

| Shape | Exponent | Mock class | Used for |
|---|---|---|---|
| `ComfortShapes.Squircle` | n = 4 | `.sq` | Covers, mosaics, icon boxes and tonal buttons below 100dp; the play button |
| `ComfortShapes.SquircleLarge` | n = 5 | `.sq-lg` | Covers from 100dp |
| `ComfortShapes.SquircleSoft` | n = 2.6 | `.sq-soft` | Artists, at every size |

Other shape rules:

| Rule | In code |
|---|---|
| Artwork picks its superellipse by the size it is drawn at | [`ArtShape`][cshapes-art], or `ComfortShapes.art(size)` for a known size |
| Buttons and chips are pills | `ComfortShapes.Pill`, `RoundedCornerShape(50)` |
| The only circles are the radio, the switch knob and the spinner | `ComfortShapes.Circle` |
| Stock Material components | [`ComfortShapes.material`][cshapes-material]: 8dp, 18dp, 18dp, 26dp, 30dp for extraSmall to extraLarge |

The superellipse is traced at 720 points, and drawn with only as many as its size needs to stay
within 0.1px of the curve ([`samplesFor`][cshapes-samples]): 64 for a 48dp row cover.
[`ComfortShapesTest`][cshapes-test] checks that every point `kit.css` lists lies on the Compose
outline. [rules.md](rules.md#performance) explains why covers do not clip to these shapes.

## Elevation

FigCraft: [`Attic Expressive.figcraft`][fx] › Elevation and motion;
[`Attic Comfort.figcraft`][fc] › Elevation and motion

### Elevation in Comfort

Floating pieces carry a blurred drop shadow and a 1dp ring; cards never take one.
[`Modifier.floatingShadow(shape, small, edge)`][cgroups-shadow] draws both. The colours are the
`shadow`, `shadowSmall` and `edge` tokens in [the palette](#the-palette).

| Shadow | Dark | Light | Ring | Used for | FigCraft effect style |
|---|---|---|---|---|---|
| Floating (`small = false`) | blur 36dp, 12dp down, in `shadow` | blur 28dp, 8dp down, in `shadow` | 1dp in `edge` | The dock, sheets, dialogs, the action bar | `Shadow/Floating` |
| Small (`small = true`) | blur 14dp, 4dp down, in `shadowSmall` | blur 12dp, 3dp down, in `shadowSmall` | none unless `edge = true` | A lifted row, the toast, the switch knob | `Shadow/Small` |

The menu is Material's `DropdownMenu` with a 12dp elevation shadow and a 1dp `edge` border
([`ComfortMenu`][coverlays]), not the blurred shadow. Surface tint is transparent and
`tonalElevation` is 0 wherever the kit sets it, so no surface changes colour by elevation.

### Elevation in Expressive

Expressive separates surfaces by tone, not shadow: a screen is `surface`, a sheet
`surfaceContainerLow`, a group cell `surfaceContainer`, a pill `surfaceContainerHigh`
([the role table](#the-app-scheme-comes-from-the-wallpaper)). These pieces cast a shadow:

| Piece | Shadow | Where | FigCraft effect style |
|---|---|---|---|
| Menu | `shadowElevation = 3.dp`, `tonalElevation = 0.dp` | [`ExpressiveMenu`][xsurfaces] | `Elevation/Level 2` |
| A row being dragged | `Modifier.shadow(6.dp)` | [`liftableCell`][xlists-lift], [`SongRow(lifted)`][xlists-songrow] | `Elevation/Level 3` |
| Floating toolbar | `Modifier.shadow(6.dp)` when `floating` | [`FloatingToolbar`][xbuttons-toolbar] | `Elevation/Level 3` |
| FAB, extended FAB, snackbar | Material's own | [`ExpressiveFab`][xbuttons-fab], [`ExpressiveSnackbar`][xsurfaces] | `Elevation/Level 3` |

## Motion

FigCraft: [`Attic Expressive.figcraft`][fx] › Elevation and motion;
[`Attic Comfort.figcraft`][fc] › Elevation and motion

### Motion in Expressive

Expressive moves on the springs in [`ExpressiveMotion`][xmotion]. Spatial springs (position, size,
shape) overshoot; effects springs (colour, alpha) do not.

| Spring | Damping ratio | Stiffness | Used for |
|---|--:|--:|---|
| `Expressive.motion.fastSpatial` | 0.6 | 800 | Corner morphs (play control, toggles, segments, a lifted cell), the heart's pop, a pressed button widening |
| `Expressive.motion.defaultSpatial` | 0.8 | 380 | Moves across the screen: the mini player sliding in, a detail screen's slide, the seek wave's amplitude |
| `Expressive.motion.slowSpatial` | 0.8 | 200 | Defined for larger moves; no screen uses it yet |
| `Expressive.motion.fastEffects` | 1 | 3800 | Colour and alpha that should switch at once: the play glyph's cross-fade, toggle colours, exits |
| `Expressive.motion.defaultEffects` | 1 | 1600 | Most fades: app bars taking their container colour, row fills, the hero caption |
| `Expressive.motion.slowEffects` | 1 | 800 | `FromArt`: every role of the cover scheme moving to a new cover |

Other motion written in the Expressive code:

| What moves | Spec | Where |
|---|---|---|
| A tab root appearing | Fades in on `defaultEffects` while growing from 92% on `defaultSpatial`; the old one fades out on `fastEffects` | [`ExpressiveNavTransitions`][xshell-nav] |
| A detail screen appearing | Slides in a tenth of the width from the end on `defaultSpatial` while fading in; back reverses it | [`ExpressiveNavTransitions`][xshell-nav] |
| Now Playing, the queue, lyrics | Rise from the bottom on `spring(dampingRatio = 1f, stiffness = 380f)`, with no overshoot so no gap opens under them | [`overlaySpring`][xshell-overlay] |
| The mini player | Slides up on `defaultSpatial` and fades in on `defaultEffects` when a song is queued | [`ExpressiveShell`][xshell] |
| The seek wave and the playing wave | One cycle every 1000 ms while playing; flat when paused | [`rememberWavePhase`][xplay-phase] |
| The loading indicator | Morphs to the next shape every 650 ms, the morph taking the first 70% of each step | [`LoadingIndicator`][xplay-loader] |
| A long artist line in the mini player | Starts scrolling after 2500 ms | [`ExpressiveShell.kt`][xshell-marquee] |

### Motion in Comfort

Comfort moves on short tweens and does not overshoot.

| Animation | Duration | What it animates | Where |
|---|---|---|---|
| `TintSpec` | 450 ms | Every token `Tinted` moves, when the cover changes | [`Tinted.kt`][tint-spec] |

Other motion written in the Comfort code:

| What moves | Spec | Where |
|---|---|---|
| Switch track and knob, radio ring | `tween(160)` | [`Controls.kt`][ccontrols] |
| Now Playing's cover stepping back to 86% when paused | `tween(260)` | [`NowPlayingCover`][cnp] |
| A tab root appearing | Fades in over 220 ms after a 60 ms delay; the old one fades out over 120 ms | [`ComfortNavTransitions`][cshell-nav] |
| A detail screen appearing | Slides in an eighth of the width over 300 ms while fading in over 220 ms | [`ComfortNavTransitions`][cshell-nav] |
| Going back from a detail screen | Slides out over 260 ms while fading out over 180 ms | [`ComfortNavTransitions`][cshell-nav] |
| The mini player row in the dock | Expands and fades in over 240 ms; shrinks and fades out over 180 ms | [`ComfortShell`][cshell] |
| Now Playing, the queue, lyrics | Rise on `spring(dampingRatio = 1f, stiffness = 380f)`, the same as Expressive | [`overlaySpring`][cshell-overlay] |
| The collapsed bar taking over from the hero | Fades over 150 ms | [`RootPage`][croot] |
| Full-screen lyrics scrolling to the next line | 240 ms | [`ComfortLyrics.kt`][clyrics] |
| The spinner | One turn every 900 ms, linear | [`Spinner`][ccontrols] |
| The playing bars | Each bar bounces between 26% and full height with its own period (420, 560 and 340 ms), 120 ms apart | [`PlayingBars`][crows-bars] |

### Motion both styles share

| What | Value | Where |
|---|---|---|
| A loading screen holds its indicator back | 400 ms | [`rememberLoadingIndicatorShown`][components-loading] |
| A cover fading in once loaded | 120 ms cross-fade | [`AtticApp.newImageLoader`][app-loader] |
| Recap's figures counting up | 1600 ms, `FastOutSlowInEasing` | [`ExpressiveRecapCards.kt`][xrecap], [`ComfortRecapCards.kt`][crecap] |
| Mini player swipe to skip | Past 28% of the text's width | [`ExpressiveShell.kt`][xshell-marquee], [`Dock.kt`][cdock-swipe] |

## Widget and window colours

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / System and › System surfaces;
[`Attic Comfort.figcraft`][fc] › Components / System and › System surfaces

The home-screen widget is drawn with `RemoteViews`, so it cannot read the Compose tokens. Its
colours are computed from the cover once a song has one, and come from resources before that.

- **Once a song has a cover**, [`WidgetColors`][widget-colors] computes both themes with the app's
  own maths. Expressive takes `ArtScheme.roles(tint, dark)` and uses `surfaceContainerHigh` for the
  container, `onSurface` and `onSurfaceVariant` for text, `primary` for play and `secondaryContainer`
  for skip. Comfort takes `ComfortTint.apply(base, swatch, wash = true)` and uses `card`, `on`,
  `on2`, `accent` and `line`. With no cover Expressive uses the wallpaper scheme and Comfort the
  plain palette.
- **Before that** (the picker preview and the first frame), the layouts use colour resources in
  [`res/values/colors.xml`][colors] and [`res/values-night/colors.xml`][colors-night]. Expressive's
  are system tones (`@android:color/system_neutral1_100` and so on). Comfort's are hex copies of the
  palette: `comfort_widget_card`, `comfort_widget_on`, `comfort_widget_on_2`,
  `comfort_widget_accent`, `comfort_widget_on_accent`, `comfort_widget_fill`, `comfort_widget_line`.
  `DesignTokensTest` fails if one of them stops matching `Comfort.colors`.

The same files hold the window colour behind the splash screen and the first frame:
`expressive_window` (the system's neutral, tone 99 in light and 10 in dark) and `comfort_window`
(the palette's `ground`).

Other widget values, from [`widget_styles.xml`][widget-styles], the layouts and
[`PlayerWidget.kt`][widget-sizes]:

| | Expressive | Comfort |
|---|---|---|
| Container corners | The system's widget radius (`system_app_widget_background_radius`) | 30dp, the float radius |
| Font | The system font | Onest (`@font/onest_widget`) |
| Title | Bold | Bold, 16sp on a 21sp line |
| Artist line | 2dp below the title | 13sp on a 17sp line, 3dp below the title |
| Play | A 36dp button in a 52dp box, round while paused and squared off while playing | A 44dp superellipse in the accent |
| Skip | A 40dp tonal fill in a 48dp target | A bare glyph in a 44dp target |
| Album shortcuts | Five covers of equal width with 12dp corners | Five 56dp covers cut to the superellipse |
| Smallest size of each layout | 110 × 110dp, 180 × 50dp, 250 × 180dp | 110 × 110dp, 180 × 50dp, 250 × 197dp |

## Website palette

The website in `site/` is not part of the app and does not use either style. It is drawn in the
brand mark's colours, in Onest, and follows the browser's light or dark setting
(`color-scheme: light dark`). Its variables are at the top of [`site/style.css`][site].

The brand colours are the [brand mark's](#brand-mark) values and do not change with the scheme:

| Variable | Hex | Used for |
|---|---|---|
| `--terracotta` | `#C6533A` | The hero's ground in light, and the dot that starts a list item |
| `--cream` | `#F5EDDD` | Text on the hero and on the privacy band, the hero's mark, and the fill of the solid hero button and the skip link |
| `--dark` | `#2A211C` | Text on cream: the solid hero button and the skip link |

Every other colour has a light and a dark value:

| Variable | Light | Dark | Used for |
|---|---|---|---|
| `--hero` | `--terracotta` | `#8F3B29` | The header and hero ground |
| `--page` | `#FBF7EF` | `--dark` | The page background |
| `--band` | `--cream` | `#33291F` | The Features and FAQ sections, and the style viewer's unselected buttons |
| `--ink` | `--dark` | `--cream` | Text, and the 3px focus outline |
| `--ink-2` | `#5A4A40` | `#CDBFAE` | Secondary text |
| `--link` | `#A2402B` | `#F0A08A` | Links. In light, `#C6533A` is too light for small text on cream |
| `--rule` | `rgba(42, 33, 28, 0.16)` | `rgba(245, 237, 221, 0.16)` | Hairlines and borders |
| `--code` | `rgba(42, 33, 28, 0.07)` | `rgba(245, 237, 221, 0.09)` | The background of inline code |
| `--deep` | `--dark` | `#1D1613` | The privacy band |
| `--bezel` | `#1B1512` | `#0F0C0A` | The phone frame around a screenshot |

Colours written where they are used, the same in both schemes:

| Hex | Used for |
|---|---|
| `#F0A08A` | Links on the privacy band |
| `#E3D6C3` | Secondary text on the privacy band |
| `#E98A72` | The marker beside "Expressive" in the style viewer |
| `#F2A93B` | The marker beside "Comfort" in the style viewer. It is not a kit token: Comfort's accent is `#F2A024` in light and `#FFB347` in dark |

Body text is `400 1.0625rem/1.6 "Onest"` (the size is `--step-0`). The page's `theme-color` is
`#C6533A` in light and `#8F3B29` in dark ([`site/index.html`][site-index]).

## Where KIT.md, the mocks and FigCraft differ from the code

This page follows the code in every case below. None of these sources was changed.

| Subject | The code | The other source |
|---|---|---|
| Launcher and notification icons | Headroom: [`ic_launcher_foreground.xml`][launcher-fg], the monochrome layer and the notification icon were redrawn in commit `0da73c1` | [`expressive/KIT.md`][xkit] ("Icons") still says they use the old house mark until they are redrawn |
| Expressive line heights | Each style sets one: 57/64, 45/52 and so on | The FigCraft text styles `Display/Large` to `Label/Small` and their emphasised partners have line height Auto; each description gives the code's value |
| How long a new cover takes in Expressive | `slowEffects`, a spring with damping 1 and stiffness 800 | `FromArt.kt`'s doc comment and `expressive/KIT.md` call it "about 600 ms"; FigCraft's Elevation and motion page puts the same spring within 1% of its target by about 235 ms |
| Expressive's app scheme | The wallpaper's, so no fixed values | `pixel/kit.css` uses one fixed scheme from seed `#3F6FD8`; FigCraft's `Wallpaper` collection holds a different example |
| Expressive's cover scheme | Fixed tone and chroma per role; tertiary's hue turned 60° | `pixel/kit.css` also mixes in a tenth of the tint's own tone and chroma, and keeps the cover's hue for tertiary. `expressive/KIT.md` records the difference |
| Display large | `displayLarge` is 57/64 | `pixel/kit.css` has no such class; its largest is `.t-display`, 45/52 |
| `onCorner` for a 56dp control | [`onCorner(56.dp)`][xshapes-oncorner] returns 16dp. `PlayButton` sets its own 18dp | The function's doc comment lists "a 56dp one 18" for the play control, which is `PlayButton`'s value, not the function's |
| Comfort's `miniInk2` | A token of its own, one step lighter (dark) or darker (light) than `on2` when tinted | `oneui/kit.css` has no such variable and draws the line in `--on-2`. `comfort/KIT.md` records the difference |
| Comfort's collection header art | Every collection screen passes 176dp ([`HeaderArt`][ccoll-art]) | `CollectionHeader`'s default is 196dp (148 for a person), and FigCraft's `size/art-header` variable is 196. `comfort/KIT.md` mentions both |
| Comfort's credits label | `KeyValue`'s label is 12.5/17 | `ComfortType.kt`'s doc comment lists `.kv em` under `caption`, which is 12.5/16. `oneui/kit.css` has 12.5/17 |
| Comfort's widget text | 16sp on a 21sp line and 13sp on a 17sp line ([`widget_styles.xml`][widget-styles]) | FigCraft's `Widget/Title` is 16/20 and `Widget/Artist` is 13/16 |
| Comfort's toast position | Placed from the dock's measured height | `oneui/kit.css` and `comfort/KIT.md` give fixed distances from the bottom: 180, 116 and 56dp |
| Comfort's out-of-gamut colours | Chroma is reduced at the same lightness and hue | A browser clips each channel, so the mocks' vivid covers are a little more saturated. `comfort/KIT.md` records the difference |
| Motion variables | Springs in `ExpressiveMotion`; tweens written where they are used in Comfort | The Comfort file has a `Motion` variable collection (`duration/tint` and others). The Expressive file has none and writes the springs on its Elevation and motion page |
| Spacing variables | None; spacing is written inline | Both FigCraft files have a `Dimension` collection with `space/…` variables, and the Expressive file `layout/…` ones |
| `AtticMark` in `ui/Components.kt` | Draws the launcher foreground scaled up; nothing calls it | Its comment describes the old mark's roofline. Use `AtticMarkIcon` or `ComfortMark` |

[test]: ../../app/src/test/java/app/attic/player/ui/DesignTokensTest.kt
[fx]: ../../design/Attic%20Expressive.figcraft
[fc]: ../../design/Attic%20Comfort.figcraft
[xkit]: ../../app/src/main/java/app/attic/player/ui/expressive/KIT.md
[ckit]: ../../app/src/main/java/app/attic/player/ui/comfort/KIT.md
[theme]: ../../app/src/main/java/app/attic/player/ui/Theme.kt#L32
[art-remember]: ../../app/src/main/java/app/attic/player/ui/Art.kt#L112
[art-swatch]: ../../app/src/main/java/app/attic/player/ui/Art.kt#L95
[art-deepen]: ../../app/src/main/java/app/attic/player/ui/Art.kt#L131
[components-loading]: ../../app/src/main/java/app/attic/player/ui/Components.kt#L73
[app-loader]: ../../app/src/main/java/app/attic/player/AtticApp.kt#L109
[xtheme-scheme]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveTheme.kt#L52
[xmotion]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveTheme.kt#L25
[fromart]: ../../app/src/main/java/app/attic/player/ui/expressive/FromArt.kt#L31
[fromart-cover]: ../../app/src/main/java/app/attic/player/ui/expressive/FromArt.kt#L46
[fromart-remember]: ../../app/src/main/java/app/attic/player/ui/expressive/FromArt.kt#L53
[fromart-animate]: ../../app/src/main/java/app/attic/player/ui/expressive/FromArt.kt#L60
[artscheme]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L20
[artscheme-dark]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L30
[artscheme-hue]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L91
[artscheme-scheme]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L110
[artscheme-pairs]: ../../app/src/main/java/app/attic/player/ui/expressive/ArtScheme.kt#L149
[artscheme-test]: ../../app/src/test/java/app/attic/player/ui/expressive/ArtSchemeTest.kt
[hct]: ../../app/src/main/java/app/attic/player/ui/expressive/Hct.kt
[xshapes]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L30
[xshapes-cover]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L57
[xshapes-group]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L69
[xshapes-oncorner]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L79
[xshapes-covershape]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L88
[xshapes-radial]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L100
[xshapes-cookie]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L158
[xshapes-clover]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L177
[xshapes-sunny]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L194
[xshapes-puff]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L215
[xshapes-burst]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L233
[xshapes-loader]: ../../app/src/main/java/app/attic/player/ui/expressive/Shapes.kt#L247
[xtype]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt
[xtype-font]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L33
[xtype-opsz]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L62
[xtype-scale]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L88
[xtype-emphasized]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L151
[xtype-narrowed]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L157
[xtype-narrowing]: ../../app/src/main/java/app/attic/player/ui/expressive/Type.kt#L167
[symbols-enum]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt#L37
[symbols-opsz]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt#L164
[symbols-icon]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt#L178
[mark]: ../../app/src/main/java/app/attic/player/ui/expressive/Mark.kt#L31
[mark-icon]: ../../app/src/main/java/app/attic/player/ui/expressive/Mark.kt#L54
[mark-wordmark]: ../../app/src/main/java/app/attic/player/ui/expressive/Mark.kt#L60
[xbuttons]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt
[xbuttons-button]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt#L78
[xbuttons-group]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt#L338
[xbuttons-fab]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt#L351
[xbuttons-toolbar]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt#L430
[xlists]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt
[xlists-cell]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L57
[xlists-lift]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L68
[xlists-cellcolor]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L84
[xlists-songrow]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L143
[xlists-collrow]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt#L315
[xnav]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L67
[xnav-search]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L224
[xnav-chips]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L301
[xnav-seg]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L314
[xnav-section]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L366
[xnav-listlabel]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt#L392
[xplay]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt
[xplay-button]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt#L75
[xplay-phase]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt#L189
[xplay-loader]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt#L457
[xsurfaces]: ../../app/src/main/java/app/attic/player/ui/expressive/Surfaces.kt
[xcover]: ../../app/src/main/java/app/attic/player/ui/expressive/Cover.kt#L79
[xtiles]: ../../app/src/main/java/app/attic/player/ui/expressive/Tiles.kt
[xtiles-shelf]: ../../app/src/main/java/app/attic/player/ui/expressive/Tiles.kt#L101
[xtiles-grid]: ../../app/src/main/java/app/attic/player/ui/expressive/Tiles.kt#L115
[xshell]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L118
[xshell-nav]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L79
[xshell-overlay]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L99
[xshell-sizes]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L101
[xshell-mini]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L223
[xshell-marquee]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt#L289
[xnp-margin]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveNowPlaying.kt#L117
[xrecap]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveRecapCards.kt#L419
[cc]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortColors.kt#L19
[cc-dark]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortColors.kt#L70
[cc-light]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortColors.kt#L101
[cc-scheme]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortColors.kt#L142
[ctheme-weight]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortTheme.kt#L30
[ctheme-icon]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortTheme.kt#L38
[tinted]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L148
[tinted-cover]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L159
[tint-tables]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L29
[tint-apply]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L79
[tint-pairs]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L108
[tint-spec]: ../../app/src/main/java/app/attic/player/ui/comfort/Tinted.kt#L129
[tint-test]: ../../app/src/test/java/app/attic/player/ui/comfort/ComfortTintTest.kt
[oklch]: ../../app/src/main/java/app/attic/player/ui/comfort/Oklch.kt
[oklch-toargb]: ../../app/src/main/java/app/attic/player/ui/comfort/Oklch.kt#L38
[ctype-font]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortType.kt#L20
[ctype-onest]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortType.kt#L33
[ctype-tabular]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortType.kt#L43
[ctype-scale]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortType.kt#L50
[ctype-material]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortType.kt#L79
[cshapes-super]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L37
[cshapes-samples]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L97
[cshapes-art]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L111
[cshapes]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L133
[cshapes-material]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShapes.kt#L160
[cshapes-test]: ../../app/src/test/java/app/attic/player/ui/comfort/ComfortShapesTest.kt
[cart-mark]: ../../app/src/main/java/app/attic/player/ui/comfort/Artwork.kt#L250
[cart-markbox]: ../../app/src/main/java/app/attic/player/ui/comfort/Artwork.kt#L255
[cgroups]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt
[cgroups-margin]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L85
[cgroups-spacing]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L88
[cgroups-group]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L106
[cgroups-hairline]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L189
[cgroups-shadow]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L206
[cgroups-label]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L226
[cgroups-padding]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt#L283
[crows-size]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt#L81
[crows-collrow]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt#L94
[crows-songrow]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt#L184
[crows-bars]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt#L328
[ctiles-shelf]: ../../app/src/main/java/app/attic/player/ui/comfort/Tiles.kt#L108
[ctiles-pager]: ../../app/src/main/java/app/attic/player/ui/comfort/Tiles.kt#L167
[cbars-hero]: ../../app/src/main/java/app/attic/player/ui/comfort/Bars.kt#L61
[cbars-tools]: ../../app/src/main/java/app/attic/player/ui/comfort/Bars.kt#L106
[cbars-bar]: ../../app/src/main/java/app/attic/player/ui/comfort/Bars.kt#L131
[cdock]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt#L65
[cdock-tabs]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt#L97
[cdock-fade]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt#L52
[cdock-swipe]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt#L235
[cnp]: ../../app/src/main/java/app/attic/player/ui/comfort/NowPlayingParts.kt
[coverlays]: ../../app/src/main/java/app/attic/player/ui/comfort/Overlays.kt
[ccontrols]: ../../app/src/main/java/app/attic/player/ui/comfort/Controls.kt
[cshell]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShell.kt#L92
[cshell-nav]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShell.kt#L58
[cshell-overlay]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShell.kt#L78
[croot]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortRoot.kt#L84
[clyrics]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortLyrics.kt#L202
[crecap]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortRecapCards.kt#L270
[ccoll-art]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortCollections.kt#L91
[widget-colors]: ../../app/src/main/java/app/attic/player/widget/WidgetColors.kt#L33
[widget-sizes]: ../../app/src/main/java/app/attic/player/widget/PlayerWidget.kt#L116
[widget-styles]: ../../app/src/main/res/values/widget_styles.xml
[colors]: ../../app/src/main/res/values/colors.xml
[colors-night]: ../../app/src/main/res/values-night/colors.xml
[launcher]: ../../app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
[launcher-fg]: ../../app/src/main/res/drawable/ic_launcher_foreground.xml
[ic-mark]: ../../app/src/main/res/drawable/ic_mark.xml
[logo-mono]: ../design/2026-11-21-ui-directions/logo/marks/sound-2-headroom.svg
[logo-color]: ../design/2026-11-21-ui-directions/logo/marks/sound-2-headroom-color.svg
[site]: ../../site/style.css
[site-index]: ../../site/index.html
