# Expressive kit (Compose)

The Compose side of `docs/design/2026-11-21-ui-directions/pixel/` (KIT.md, kit.css,
00-direction.html). Screens built in Expressive draw only with what is here, plus stock Material 3
where this file says so. Everything is in package `app.attic.player.ui.expressive`. Components take
plain data and lambdas; none of them reach app singletons.

Material 3 in this BOM (2026.09.00) is **1.4.0**. It keeps the Expressive APIs internal
(MaterialExpressiveTheme, MotionScheme, the emphasised type styles, flexible app bars) and does not
ship ButtonGroup, ToggleButton, SplitButton, FloatingToolbar, LoadingIndicator, the wavy progress
indicators or MaterialShapes at all. Those are drawn here. Used from Material 1.4: Button family,
FloatingActionButton, FilterChip, ShortNavigationBar, HorizontalMultiBrowseCarousel
(`@ExperimentalMaterial3Api`, opted in inside `HeroCarousel`), ModalBottomSheet, AlertDialog,
DropdownMenu, Snackbar, Switch, RadioButton, LinearProgressIndicator (with `gapSize`),
CircularProgressIndicator, SwipeToDismissBox, OutlinedTextField. No caller needs an opt-in.
Material 3 1.5.0 (BOM 2026.11.01) makes most of the drawn ones public; swapping is local to the
functions below.

## Theme

`AtticTheme(mode, UiStyle.Expressive)` (ui/Theme.kt) sets up:

- **Colour**: the wallpaper scheme, `dynamicDarkColorScheme` / `dynamicLightColorScheme`, dark or
  light from ThemeMode. minSdk 33, so there is no fallback scheme. Use roles as kit.css names them:
  screens `surface`, group cells `surfaceContainer`, pills and the search bar `surfaceContainerHigh`,
  the one filled action and selected states `primary`, the nav indicator / selected chip / playing
  row `secondaryContainer`, Favourites `tertiaryContainer` and nothing else.
- **Type**: `MaterialTheme.typography` is the kit's scale in Google Sans Flex. The emphasised
  partners (`.em`) are `Expressive.type.titleLargeEmphasized` etc. (all 15).
- **Shape**: `MaterialTheme.shapes` is the kit's 4/8/12/16/28 scale. The rest is `ExpressiveShapes`.
- **Motion**: `ExpressiveMotion.fastSpatial()` (0.6 / 800), `defaultSpatial()` (0.8 / 380),
  `slowSpatial()` (0.8 / 200), `fastEffects()` (1 / 3800), `defaultEffects()` (1 / 1600),
  `slowEffects()` (1 / 800). Spatial for position, size and shape; effects for colour and alpha.

`Expressive.type`, `Expressive.motion`, `Expressive.shapes` reach the tokens MaterialTheme has no
slot for.

### Unbuilt surfaces and the shell

Every Expressive (and Comfort) branch that still calls Classic drawing is wrapped in
`ClassicSurface { }` (ui/classic/ClassicTheme.kt), which provides Classic's colours, type and shapes.
When you build a surface, replace `ClassicSurface { ClassicX(state, actions) }` with your drawing.

The shell is still Classic, so AppRoot calls `ClassicShell(state, actions, slots.inStyleSurface())`:
each slot (the NavHost, mini player, Now Playing, queue, lyrics) is drawn in `StyleSurface { }`,
which restores the style's own theme. A screen you build therefore gets the Expressive theme even
inside the Classic shell. Whoever builds the Expressive shell drops both wrappers there.

## Colour from the cover: `FromArt`

```kotlin
val seed = rememberArtColor(track.mediaId, fallback = Color.Unspecified)   // ui/Art.kt
FromArt(seed) {            // every Material role inside is rebuilt from the cover
    MiniPlayerPill(...)    // uses MaterialTheme.colorScheme as usual
}
FromCover(track.mediaId) { ... }      // the same, looking the tint up itself
val scheme = rememberArtScheme(seed)  // the scheme without providing it, not animated
```

- Follows light and dark (`dark = LocalIsDark.current` by default). Animates every role on
  `slowEffects` (about 600 ms) when the seed changes. `Color.Unspecified` keeps the scheme around it.
- Use on player surfaces only: mini player, Now Playing, queue, full-screen lyrics, collection and
  artist headers (`.chead`), browse tiles (`BrowseTile` does it itself). The list under a header stays
  on the app scheme.
- How it maps the kit rule (`ArtScheme`): kit.css builds each role as `oklch(L C h)` with fixed L and
  C per role and h from the tint. Each role here keeps that L as an HCT tone and that C as HCT chroma
  (median over hues), with hue = the cover's HCT hue. Tertiary is the hue + 60°, as Material's tonal
  spot does (the CSS could not rotate). The CSS's 10% mix of the tint's own tone and chroma is left
  out so contrast never depends on the cover. Error roles come from the app scheme. Unit tests check
  every text pair at 4.5:1 or more for 223 seeds in both themes.

## Type helpers

| Kit | Compose |
|---|---|
| `.t-display` … `.t-label-s` | `MaterialTheme.typography.displayMedium` … `labelSmall` (sizes per kit; display L 57/64 also there) |
| `.em` | `Expressive.type.<style>Emphasized`, or `style.emphasized()` on any kit style |
| `.w75`, `.w50` | `style.narrowed(75)`, `style.narrowed(50)` |
| title narrows before it truncates | `NarrowingText(text, modifier, style, color, maxLines = 1)`: measures at width 100, 75, 50, then ellipsizes |
| `.dim` | `color = MaterialTheme.colorScheme.onSurfaceVariant` |
| tabular figures | `style.copy(fontFeatureSettings = "tnum")` |

Google Sans Flex is bundled once (res/font/google_sans_flex.ttf, weight 300–800, width 50–100,
ROND 0–100, optical size 12–48). `GoogleSansFlex.family(Axes(opticalSize, round, width))` gives a
FontFamily for other combinations; each style already sets an optical size near its own size.

## Shape helpers

| Kit | Compose |
|---|---|
| `--r-xs` … `--r-xxl` | `ExpressiveShapes.ExtraSmall` (4) `Small` (8) `Medium` (12) `Large` (16) `LargeIncreased` (20) `ExtraLarge` (28) `ExtraLargeIncreased` (32) `ExtraExtraLarge` (48); `Pill` |
| cover radius by size | `CoverShape` (measures itself), `ExpressiveShapes.cover(size)`, `coverRadius(size)` |
| `.cookie` | `CookieShape` (the kit's polygon, Material's Cookie9Sided) |
| squared off means on | `ExpressiveShapes.onCorner(height)` |
| group cell | `ExpressiveShapes.groupItemShape(index, count)`, `GroupGap` 2dp, `GroupSpacing` 12dp |

## Icons

`SymbolIcon(Symbol.QueueMusic, size = 24.dp, filled = false, tint = LocalContentColor.current)`.
Material Symbols Rounded, subset to the 85 names the pixel/ and oneui/ sheets use; `Symbol` is an
enum named after the ligature in CamelCase (`queue_music` → `Symbol.QueueMusic`,
`Symbol.named("queue_music")`). Filled is the selected state and the transport glyphs. Sizes 18–40.
`weight = 300` for Comfort. A name not in the enum is not in the font: add it (see the enum's doc
and third_party/fonts-NOTICE.txt). Describe the button, not the icon; pass `contentDescription` only
for a standalone icon. The mark: `AtticMarkIcon(size, tint)`, `AtticWordmark("Attic", style)`.

## Components, by kit class

| Kit class | Compose | Notes |
|---|---|---|
| `.cv` | `Cover(art, modifier, size = null, shape = CoverShape)` | `art = Art.of(mediaId)`; size null fills the width as a square |
| `.cv.cookie` | `ArtistCover(art, modifier, size)` | |
| `.mosaic` | `Mosaic(arts, modifier, size)` | 2×2 at four or more covers; pass covers from different albums (`mosaicIds`) |
| `.fav-art` | `FavouritesArt(modifier, size)` | |
| `.no-art` | `NoArt(modifier, size)` | `Cover(null)` shows the same |
| `.btn` (filled/tonal/outlined/text, `.m`, `.sq`, `.off`) | `ExpressiveButton(text, onClick, modifier, variant, size = Small/Medium, leading, trailing, squared, enabled)` | `squared = true` while a Play button reads Pause |
| `.ib` (`.filled` `.tonal` `.outlined` `.narrow` `.wide` `.m`) | `ExpressiveIconButton(symbol, contentDescription, onClick, modifier, variant, medium, width, filled, tint)` | 48dp target |
| `.ib.on` | `ExpressiveToggleButton(symbol, checked, onCheckedChange, contentDescription, medium, width, tonalWhenOff)` | morphs round → 12/16 corners, fills primary |
| `.ib.heart` | `HeartButton(favourite, onToggle)` | describes itself ("Add to favourites" / "Remove from favourites") |
| `.btn-group` | `ButtonGroup(modifier, fill) { … Modifier.groupItem(interactionSource, weight) }` | pressed item widens 15%; with `fill` the others narrow |
| `.fab`, `.fab.ext` | `ExpressiveFab(symbol, contentDescription, onClick, primary)`, `ExpressiveExtendedFab(text, symbol, onClick)` | |
| `.split` | `SplitButton(text, onClick, onMenuClick, menuContentDescription, leading)` | |
| `.toolbar` (`.vibrant`, `.floating`) | `FloatingToolbar(modifier, vibrant, floating) { icon buttons }` | Now Playing's tools row |
| `.pp`, `.pp.l`, `.pp.xl`, `.pp.ring`, `.pp.tonal` | `PlayButton(playing, onClick, modifier, size = Small/Large/ExtraLarge, tonal, progress = { fraction })` | ring only when `progress` is given; describes itself "Play"/"Pause" |
| `.skip`, `.transport` | `SkipButton(symbol, contentDescription, onClick)`, `Transport(playing, onPrevious, onPlayPause, onNext)` | |
| `.wave` / `.wave.still` | `NowPlayingWave(playing)` | 20×10dp |
| `.seek`, `.seek.paused`, `.seek-times` | `SeekBar(progress, playing, onSeek, contentDescription)`, `SeekTimes(elapsed, total)` | tap or drag; `onSeek(fraction)` on release; flat while paused or dragging |
| `.nav` | `ExpressiveNavigationBar(items: List<NavItem>, selectedIndex, onSelect)` | Material ShortNavigationBar; pads for the gesture bar itself |
| `.appbar.root` (+ `.scrolled`) | `RootAppBar(title, modifier, scrolled = 0f..1f, titleLeading, actions)` | `scrolled = listState.scrolledFraction()`; Home passes the mark as `titleLeading`; settings is the last action |
| `.appbar`, `.scrolled`, `.over` | `DetailAppBar(title, onBack, modifier, surface = Plain/Scrolled/OverArt, actions)` | OverArt hides the title and puts scrims behind the buttons; inside `FromArt` for a collection |
| `.searchbar` | `SearchField(query, onQueryChange, placeholder, modifier, onSearch)` | clear button appears with text |
| `.chips` / `.chip` | `ChipRow { ExpressiveFilterChip(label, selected, onClick) }` | Library's filters |
| `.seg` | `ConnectedButtonGroup(options, selectedIndex, onSelect)` | Home's filter, Recap's period |
| `.sec` | `SectionHeader(title, modifier, overline, trailing)` | |
| `.list-label` | `ListLabel(text)` | |
| `.group` | `ListGroup(count) { index -> … }`; lazy: `groupItems(list, key) { index, item -> … }`; any modifier: `Modifier.groupCell(index, count, color)` | sheets raise cells via `LocalGroupCellColor` (ExpressiveSheet sets it) |
| `.row` (cover, number, rank + cover, `.now`, `.lifted`) | `SongRow(title, subtitle, lead = RowLead.Cover/Number/RankedCover/None, current, playing, lifted, onClick, onLongClick, trailing)` | the wave goes over the cover or replaces the number on the current song |
| row trailing | `RowMoreButton(title, onClick)`, `RowDragHandle(title, modifier)`, `RowAddToggle(title, added, onToggle)`, `RowCount(text)` | describe themselves from the shared copy |
| `.swipe` | `SwipeToRemove(onRemove) { SongRow(…) }` | |
| `.row.coll` | `CollectionRow(title, subtitle, lead = { Cover(…, size = 56.dp) }, onClick)` | |
| `.tile`, `.tile.s`, `.tile.artist`, `.tile.now` | `Tile(title, onClick, art = { m -> Cover(art, m) }, subtitle, width = 148.dp, artist, now, playing, onLongClick)` | |
| `.shelf` | `Shelf { item { Tile(…) } }` | LazyRow, 12dp gaps |
| `.grid`, `.grid.c3` | `TileGrid(items, columns = 2) { Tile(…, width = null) }` | not lazy |
| `.hero` | `HeroCarousel(count, leading = { FavouritesPill(…) }, caption = { HeroCaption(…) }, onItemClick) { index -> Cover(…, Modifier.fillMaxSize(), shape = RectangleShape) }` | Material multi-browse carousel |
| `.hero-fav` | `FavouritesPill(count, unit, description, onClick)` | |
| `.mix`, `.mixes` | `InstantMixPill(title, art, onClick, onLongClick, now)`, `InstantMixes(items, rows = 3) { InstantMixPill(…) }` | |
| `.recap-card` | `RecapCard(title, body, onClick) { m -> Mosaic(…, m) }` | |
| `.spot` | `Spotlight(overline, title, art, onOpen) { Shelf { … } }` | |
| `.browse` | `BrowseTile(title, seed, onClick) { m -> Cover(…, m) }` | takes the cover's scheme itself |
| `.stat` | `StatTile(value, label, modifier, accent)` | |
| `.sheet`, `.handle`, `.sheet-head`, `.sheet-title`, `.divider`, `.sheet-row` | `ExpressiveSheet(onDismiss) { SheetHeader(title, subtitle) { m -> Cover(…, m) }; SheetDivider(); SheetRow(symbol, label, onClick, selected) }`, `SheetTitle(text)` | opens fully expanded |
| `.dialog`, `.field` | `ExpressiveDialog(title, onDismiss, confirmButton, dismissButton, body) { DialogTextField(value, onValueChange, label) }` | buttons are `ExpressiveButton(variant = Text)` |
| `.menu`, `.menu-item` | `ExpressiveMenu(expanded, onDismiss) { ExpressiveMenuItem(symbol, label, onClick, danger) }` | |
| `.snack` | `SnackbarHost(state) { ExpressiveSnackbar(it) }`; `StaticSnackbar(text)` for previews | |
| `.empty`, `.empty-art` | `EmptyState(title, body, symbol or art = { AtticMarkIcon(size = 56.dp) }, action)` | |
| `.set-sec`, `.set-row`, `.set-note` | `SettingsSection(title)`, `SetRow(title, summary, leading, trailing)`, `SetSwitchRow`, `SetRadioRow`, `SetNote(text) { …; SetNoteStatus(text) }` | put rows in a `ListGroup` |
| `.switch`, `.radio` | `ExpressiveSwitch(checked, onCheckedChange)`, `ExpressiveRadio(selected, onClick)` | Material's |
| `.progress`, `.progress.wavy` | `LinearProgress { fraction }`, `WavyLinearProgress({ fraction }, running)` | wavy for long running jobs (analysis) |
| `.spinner`, `.spinner.busy`, `.spinner.s` | `CircularProgress(progress = null or { fraction }, small)` | |
| `.loader`, `.loader.contained` | `LoadingIndicator(contained)` | library scan, downloading lyrics |
| `.np-card`, `.kv` | `InfoCard(title, symbol) { … }`, `KeyValue(label, value)` | lyrics and credits cards |
| `.lyrics p` (`.past`, `.now`, `.plain`, `.l`) | `LyricLine(text, LyricState.Sung/Current/Upcoming/Plain, large)` | |

Not in the kit, as in pixel/KIT.md: the mini player, Now Playing, the collection header block and
Recap's charts are screen work, composed from the parts above (the gallery shows a mini player and
both headers built that way).

## Shared copy (strings)

`res/values/strings.xml` holds every label in CURRENT-UI.md section 2, with BRIEF.md's naming table
applied. Use `stringResource` / `pluralStringResource`; do not write literals in Expressive drawing.

- Names are `<surface>_<what>`: `home_instant_mixes`, `library_filter_songs`, `queue_up_next`.
  Content descriptions end in `_cd`: `now_playing_close_cd`, `more_options_for_cd`.
- Shared words have no prefix: `play`, `pause`, `previous`, `next`, `back`, `settings`, `cancel`,
  `create`, `rename`, `close`, `favourites`, `favourite_add`, `favourite_remove`.
- Counts are plurals with the number as `%1$s`, formatted by you with grouping:
  `pluralStringResource(R.plurals.song_count, n, NumberFormat.getIntegerInstance().format(n))`.
- Durations: `duration_hours_minutes` / `duration_minutes`, joined by `songs_and_duration`.
- Context names under "Playing from" are `context_*`; the model still produces Classic's literal
  context strings ("Liked Songs"), so map them when you draw.
- The style picker's three strings (`settings_style*`) are the only copy not in CURRENT-UI.md.
- Wording deliberately kept from today although the mocks differ: library subtitles stay
  "Playlist, 46 songs" / "Artist"; the album detail stays "Album • 2025". The Favourites empty body
  says "Tap the heart on a song to save it." because the plus became a heart.

## Gallery

A debug-only activity lays the kit out like 00-direction.html's strips, with covers drawn in code
and a FromArt section over six sample cover tints:

```
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es theme dark
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es theme light --ei section 6
```

`theme` is dark, light or system; `section` scrolls to a strip (0 header, 1 scheme, 2 FromArt,
3 type, 4 shape, 5 icons, 6 buttons, 7 chips and search, 8 song rows, 9 collection rows, 10 tiles,
11 nav, 12 seek and transport, 13 app bars, 14 art cards and queue, 15 headers, 16 settings and
progress, 17 sheet, dialog, menu, snackbar, empty). The button in its header flips the theme. It is
in `app/src/debug/`, so release builds do not contain it. It does not touch the app's settings.

## Known differences from the mock

- Optical size: the font is set at a fixed optical size per text style (12, 14, 16, 22, 28, 36 or
  45), where the browser follows the exact size. Ranges were trimmed to keep the font at 540 KB.
- The art scheme drops the CSS's 10% mix of the tint's own tone and chroma, and rotates tertiary
  60° where the CSS keeps the cover's hue. Art surfaces are therefore a touch more even across
  covers than the sheets show.
- The seek wave starts with a round cap, not the CSS's square end. The progress ring is drawn for
  the real position, not the sheet's fixed 1:47.
- The loading indicator morphs through approximations of Material's seven shapes (burst and cookie
  are exact; pentagon, pill, sunny, four-leaf and oval are drawn here).
- The dialog, sheet, menu and switch are Material's own components with the kit's colours, shapes
  and type, so spacing inside them is Material's (24dp dialog padding, 48dp menu items).
- Chips and connected-group segments are 32 and 40dp tall with 48dp touch targets, as the mock
  notes.
