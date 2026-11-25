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
  row `secondaryContainer`, Favourites `tertiaryContainer` and nothing else. In dark the app scheme's
  tertiary container is the wallpaper's `tertiaryFixed` (ink `onTertiaryFixedVariant`), so Favourites stays
  light as kit.css draws it in both themes.
- **Type**: `MaterialTheme.typography` is the kit's scale in Google Sans Flex. The emphasised
  partners (`.em`) are `Expressive.type.titleLargeEmphasized` etc. (all 15).
- **Shape**: `MaterialTheme.shapes` is the kit's 4/8/12/16/28 scale. The rest is `ExpressiveShapes`.
- **Motion**: `ExpressiveMotion.fastSpatial()` (0.6 / 800), `defaultSpatial()` (0.8 / 380),
  `slowSpatial()` (0.8 / 200), `fastEffects()` (1 / 3800), `defaultEffects()` (1 / 1600),
  `slowEffects()` (1 / 800). Spatial for position, size and shape; effects for colour and alpha.

`Expressive.type`, `Expressive.motion`, `Expressive.shapes` reach the tokens MaterialTheme has no
slot for.

### The shell

AppRoot calls `ExpressiveShell(state, actions, slots)` with no wrappers, so every slot is drawn in
the Expressive theme.

### The shell, for screens inside it (ExpressiveShell.kt)

- Destinations fill the screen and scroll under the dock: the navigation bar, with the mini player
  8dp above it and 12dp in from the edges while a song is queued. Pad a scrolling screen's end by
  `LocalBottomPadding.current` (ui/Components.kt): the dock's height plus 24dp, the kit's
  `.page.clear-dock`. A FAB sits 12dp above the dock: `padding(bottom = LocalBottomPadding.current - 12.dp)`.
- A tab root takes the status bar itself (`RootAppBar`, `DetailAppBar` do); the shell adds no top inset.
- Destinations move with `ExpressiveNavTransitions`: tab roots fade through, detail screens use the
  shared X axis. Now Playing, the queue and lyrics rise from the bottom over the dock.
- While the keyboard is up the mini player rides above it and the navigation bar stays under it.
- The snackbar (`ExpressiveSnackbar`) sits 16dp above the dock, or near the bottom edge while an
  overlay is open, and draws above the overlays.
- `LocalOverlayCovered` (ui/AppRoot.kt) is true while Now Playing, the queue or lyrics is up. A screen with
  its own `BackHandler` (Search's docked bar) disables it then, so back closes the overlay first.
- System bar icons follow the theme (MainActivity). Art surfaces take the same light or dark as the
  theme, so the icons stay readable over them without a change per surface.

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
| `--shape-clover` (sheets 06, 08) | `CloverShape` (Material's Clover4Leaf), for "nothing here yet" |
| `--sunny`, `--puff` (sheet 09) | `SunnyShape` (Material's Sunny, the play-count badge), `PuffShape` (a soft square inset 6.5%) |

## Icons

`SymbolIcon(Symbol.QueueMusic, size = 24.dp, filled = false, tint = LocalContentColor.current)`.
Material Symbols Rounded, subset to the 88 names the pixel/ and oneui/ sheets use, plus `file_save` and
`file_open` for Settings' backup rows and `transition_fade`, `schedule` and `equalizer` for its Playback rows
(Transitions, Crossfade length, Equaliser); `Symbol` is an
enum named after the ligature in CamelCase (`queue_music` → `Symbol.QueueMusic`,
`Symbol.named("queue_music")`). Filled is the selected state and the transport glyphs. Sizes 18–40.
`weight = 300` for Comfort. A name not in the enum is not in the font: add it (see the enum's doc
and third_party/fonts-NOTICE.txt). Describe the button, not the icon; pass `contentDescription` only
for a standalone icon. The mark: `AtticMarkIcon(size, tint)`, `AtticWordmark("Attic", style)`. It is
"Headroom" (logo/marks/sound-2-headroom.svg, the owner's choice), not the sheets' house stand-in: one
even-odd path cropped to its bounding box, drawn [size] tall and about 0.7 of that wide, in the tint
you give it. The launcher icon, its monochrome layer and the notification icon still use the old
house until they are redrawn.

## Components, by kit class

| Kit class | Compose | Notes |
|---|---|---|
| `.cv` | `Cover(art, modifier, size = null, shape = CoverShape)` | `art = Art.of(mediaId)`; size null fills the width as a square |
| `.cv.cookie` | `ArtistCover(art, modifier, size)` | |
| `.mosaic` | `Mosaic(arts, modifier, size)` | 2×2 at four or more covers; pass covers from different albums (`mosaicIds`) |
| `.fav-art` | `FavouritesArt(modifier, size)` | |
| `.no-art` | `NoArt(modifier, size)` | `Cover(null)` shows the same |
| `.btn` (filled/tonal/outlined/text, `.m`, `.sq`, `.off`, `.danger`) | `ExpressiveButton(text, onClick, modifier, variant, size = Small/Medium, leading, trailing, squared, enabled, contentColor)` | `squared = true` while a Play button reads Pause; `contentColor = colorScheme.error` for a destructive text button ("Delete playlist") |
| `.ib` (`.filled` `.tonal` `.outlined` `.narrow` `.wide` `.m`) | `ExpressiveIconButton(symbol, contentDescription, onClick, modifier, variant, medium, width, filled, tint)` | 48dp target |
| `.ib.on` | `ExpressiveToggleButton(symbol, checked, onCheckedChange, contentDescription, medium, width, tonalWhenOff)` | morphs round → 12/16 corners, fills primary |
| `.ib.heart` | `HeartButton(favourite, onToggle)` | describes itself ("Add to favourites" / "Remove from favourites") |
| `.btn-group` | `ButtonGroup(modifier, fill) { … Modifier.groupItem(interactionSource, weight) }` | pressed item widens 15%; with `fill` the others narrow |
| `.fab`, `.fab.ext` | `ExpressiveFab(symbol, contentDescription, onClick, primary)`, `ExpressiveExtendedFab(text, symbol, onClick, expanded)` | `expanded = false` shrinks it to the symbol once a list scrolls (Library) |
| `.split` | `SplitButton(text, onClick, onMenuClick, menuContentDescription, leading)` | |
| `.toolbar` (`.vibrant`, `.floating`) | `FloatingToolbar(modifier, vibrant, floating) { icon buttons }` | Now Playing's tools row |
| `.pp`, `.pp.l`, `.pp.xl`, `.pp.ring`, `.pp.tonal` | `PlayButton(playing, onClick, modifier, size = Small/Large/ExtraLarge, tonal, progress = { fraction })` | ring only when `progress` is given; describes itself "Play"/"Pause" |
| `.skip`, `.transport` | `SkipButton(symbol, contentDescription, onClick)`, `Transport(playing, onPrevious, onPlayPause, onNext)` | |
| `.wave` / `.wave.still` | `NowPlayingWave(playing)` | 20×10dp |
| `.seek`, `.seek.paused`, `.seek-times` | `SeekBar(progress, playing, onSeek, contentDescription, onScrub, mark)`, `SeekTimes(elapsed, total)` | tap or drag; `onSeek(fraction)` on release; `onScrub(fraction)` while dragging and `onScrub(null)` on release, so the elapsed time can follow the finger; flat while paused or dragging; `mark` (a fraction, or null) draws a 4dp `onSurfaceVariant` dot on the rest of the track where a Smart transition starts, at the handle's own mapping, only while the handle is more than 2dp short of it (the rule `StoryProgress` uses for its stops) |
| `.story .seek` + `.st-stops` | `StoryProgress(progress = { fraction }, stops, running)` | the seek bar's drawing, not draggable, with a 4dp dot at each stop still ahead; flat while not running (the story held) |
| `.nav` | `ExpressiveNavigationBar(items: List<NavItem>, selectedIndex, onSelect)` | Material ShortNavigationBar; pads for the gesture bar itself |
| `.appbar.root` (+ `.scrolled`) | `RootAppBar(title, modifier, scrolled = 0f..1f, titleLeading, actions)` | `scrolled = listState.scrolledFraction()`; Home passes the mark as `titleLeading`; settings is the last action |
| `.appbar`, `.scrolled`, `.over` | `DetailAppBar(title, onBack, modifier, surface = Plain/Scrolled/OverArt, actions)` | OverArt hides the title and puts scrims behind the buttons; inside `FromArt` for a collection |
| `.searchbar`, `.searchbar.docked` | `SearchField(query, onQueryChange, placeholder, modifier, onSearch, onBack)` | clear button appears with text; `onBack` makes it the docked search view's bar, a back button in place of the glyph |
| `.chips` / `.chip` | `ChipRow { ExpressiveFilterChip(label, selected, onClick) }` | Library's filters |
| `.seg`, `.seg.four` | `ConnectedButtonGroup(options, selectedIndex, onSelect, modifier, unselectedColor, labelWidth, selectedWeight)` | Home's filter, Recap's period, Settings' theme and style; on a group cell pass `surfaceContainerHighest`; Recap's four periods pass `labelWidth = 86, selectedWeight = 1.3f` |
| `.sec` | `SectionHeader(title, modifier, overline, trailing)` | |
| `.list-label` | `ListLabel(text)` | |
| `.group` | `ListGroup(count) { index -> … }`; lazy: `groupItems(list, key) { index, item -> … }`; any modifier: `Modifier.groupCell(index, count, color)` | sheets raise cells via `LocalGroupCellColor` (ExpressiveSheet sets it) |
| `.row` (cover, number, rank + cover, rank + cookie, `.now`, `.lifted`) | `SongRow(title, subtitle, lead = RowLead.Cover/Number/RankedCover/RankedArtist/None, current, playing, lifted, onClick, onLongClick, onSwipeToQueue, trailing)` | the wave goes over the cover or replaces the number on the current song; an empty `subtitle` leaves the title alone (Recap's top artists); `onSwipeToQueue` wraps the row in `SwipeToQueueCell` |
| row trailing | `RowMoreButton(title, onClick)`, `RowDragHandle(title, modifier)`, `RowAddToggle(title, added, onToggle)`, `RowCount(text)`, `RowSmartPickMark()` | describe themselves from the shared copy; the smart-pick mark (radio glyph in primary, 20dp) goes before the handle on a queue row smart shuffle added |
| `.swipe` | `SwipeToRemove(onRemove, resetKey) { SongRow(…) }` | the swipe starts over when `resetKey` changes; the queue passes its up-next list, because a row's key passes to the song's next copy after a removal |
| swipe to queue (no mock) | `SwipeToQueueCell(onQueue) { … }`, or `SongRow(onSwipeToQueue = …)` | towards the end only; uncovers `surfaceContainerHighest` with `queue_music` and "Add to queue", `primaryContainer` once letting go would queue; settles on `fastSpatial`. Thresholds and the confirm haptic are shared (`ui/SwipeToQueue.kt`). Never inside the queue, whose rows swipe to remove |
| `.row.lifted` in a group | `Modifier.liftableCell(index, count, lifted)` on a lazy item, with `SongRow(lifted = …)` | a group cell that can be dragged: in its place while it rests; all four corners 20 and an unclipped shadow while held. The queue and a playlist's reorder mode. The handle's `onDragStarted` gives `Haptic.PickUp` and `onDragStopped` gives `Haptic.Drop` (`ui/Haptics.kt`) |
| `.row.coll` | `CollectionRow(title, subtitle, lead = { Cover(…, size = 56.dp) }, onClick)` | `onClick = null` leaves the row inert, for a list whose only action is a button in `trailing` (the equaliser's albums) |
| `.tile`, `.tile.s`, `.tile.artist`, `.tile.now` | `Tile(title, onClick, art = { m -> Cover(art, m) }, subtitle, width = 148.dp, artist, now, playing, onLongClick, titleLines = 1)` | `titleLines = 2` for a name that needs it (Home's taste mixes) |
| `.shelf` | `Shelf { item { Tile(…) } }` | LazyRow, 12dp gaps |
| `.grid`, `.grid.c3` | `TileGrid(items, columns = 2) { Tile(…, width = null) }` | not lazy |
| `.hero` | `HeroCarousel(count, leading = { FavouritesPill(…) }, caption = { HeroCaption(…) }, onItemClick) { index -> Cover(…, Modifier.fillMaxSize(), shape = RectangleShape) }` | Material multi-browse carousel |
| `.hero-fav` | `FavouritesPill(count, unit, description, onClick)` | |
| `.mix`, `.mixes` | `InstantMixPill(title, art, onClick, onLongClick, now)`, `InstantMixes(items, rows = 3) { InstantMixPill(…) }` | |
| `.recap-card` | `RecapCard(title, body, onClick) { m -> Mosaic(…, m) }` | |
| `.spot` | `Spotlight(overline, title, art, onOpen) { Shelf { … } }` | |
| `.browse`, `.browse.folder` | `BrowseTile(title, seed, onClick, modifier, subtitle) { m -> Cover(…, m) }` | takes the cover's scheme itself; with `subtitle` it is Search's 88dp folder tile (sheet 02) with a 64dp picture |
| Mood tile, Mood picks card (design) | `MoodTile(tile, onClick)`, `MoodPicksCard(onChoose, onDismiss)` in `ExpressiveMoods.kt` | Home's Moods shelf: `BrowseTile`'s tonal container at 200 by 108 in the first cover's hue, 56dp mosaic at the end; the card is `surfaceContainerHigh` with a filled and a text button |
| `.stat` | `StatTile(value, label, modifier, accent)` | |
| `.sheet`, `.handle`, `.sheet-head`, `.sheet-title`, `.divider`, `.sheet-row` | `ExpressiveSheet(onDismiss, bottomPadding) { SheetHeader(title, subtitle) { m -> Cover(…, m) }; SheetDivider(); SheetRow(symbol, label, onClick, selected) }`, `SheetTitle(text)`, `SheetSubtitle(text, maxLines)` | opens fully expanded; a sheet whose list scrolls to its foot passes `bottomPadding = 0.dp`; the subtitle is the line pulled up under a title (`.sheet-sub`) |
| `.dialog`, `.field` | `ExpressiveDialog(title, onDismiss, confirmButton, dismissButton, body) { DialogTextField(value, onValueChange, label) }` | buttons are `ExpressiveButton(variant = Text)`; the field also takes a `TextFieldValue` (prefilled, cursor at the end), with `error` for the line under it and `onDone` for the keyboard's done key |
| `.menu`, `.menu-item` | `ExpressiveMenu(expanded, onDismiss, offset) { ExpressiveMenuItem(symbol, label, onClick, danger, checked) }` | anchored to the layout it is declared in, moved by `offset`; a null `symbol` keeps its space, so a list of choices ticks only the current one; a non-null `checked` makes an on/off item (smart shuffle): filled primary symbol and a trailing tick while on, read as a toggle |
| `.snack` | `SnackbarHost(state) { ExpressiveSnackbar(it) }`; `StaticSnackbar(text)` for previews | over the player the shell draws it inside `FromCover`, so it takes the cover scheme's inverse roles (07 J) |
| `.empty`, `.empty-art` | `EmptyState(title, body, symbol or art = { AtticMarkIcon(size = 56.dp) }, action)` | `clover = true` draws the 128dp clover (`.empty-art.clover`), the empty library; `body = null` for a title alone ("Nothing up next") |
| `.set-sec`, `.set-row`, `.set-note` | `SettingsSection(title)`, `SetRow(title, summary, leading, trailing)`, `SetSwitchRow`, `SetRadioRow`, `SetNote(text) { …; SetNoteStatus(text) }` | put rows in a `ListGroup` |
| `.set-row .set-ic` (sheet 08) | `SetIconRow(symbol, modifier, center, trailing) { RowTitle(…); SupportingText(…); Status(…) }` | Settings' own row, a 40dp tonal circle before the text; internal to the package (ExpressiveSettings.kt), shared with the equaliser page |
| `.set-title` + `.appbar` | `LargeTitlePage(title, listState, onBack) { LazyColumn { item { LargeTitle(title, bottom) } … } }` | a page under Settings whose 36sp title collapses into the bar (licences, equaliser); internal, in ExpressiveLicences.kt |
| `.switch`, `.radio` | `ExpressiveSwitch(checked, onCheckedChange)`, `ExpressiveRadio(selected, onClick)` | Material's |
| `.progress`, `.progress.wavy` | `LinearProgress { fraction }`, `WavyLinearProgress({ fraction }, running)` | wavy for long running jobs (analysis) |
| `.spinner`, `.spinner.busy`, `.spinner.s` | `CircularProgress(progress = null or { fraction }, small)` | |
| `.loader`, `.loader.contained` | `LoadingIndicator(contained)` | library scan, downloading lyrics |
| `.np-card`, `.kv` | `InfoCard(title, symbol) { … }`, `KeyValue(label, value)` | lyrics and credits cards |
| `.lyrics p` (`.past`, `.now`, `.plain`, `.l`) | `LyricLine(text, LyricState.Sung/Current/Upcoming/Plain, large, maxLines)` | `maxLines` ends in an ellipsis (the card's preview: two per synced line, four for plain text) |

## Sheets and Recap (sheets 07 and 09)

Built from the parts above, in `ExpressiveSheets.kt`, `ExpressiveRecap.kt`, `ExpressiveRecapStory.kt`
and `ExpressiveRecapCards.kt`. Pieces other screens can use:

- `languageName(code)`: a language code ("ta", "none", null) as the shared copy, "Unknown" when unset.
- `ExpressiveRecapCard(card, data, modifier, layer, animate)`: one Recap card at any width. The
  drawing is a fixed 360×640 canvas at font scale 1, so the story, thumbnails and the saved
  1080×1920 image match. Its colours are `ArtScheme.colorScheme(seed, dark = true)` of the cover
  `recapCardCover(card, data)` names (a card about one song or artist takes that cover; the rest are
  dealt from the top songs), in both themes and never the wallpaper. A song with no art uses a fixed
  fallback seed, Material's baseline purple. `rememberRecapCardSeed` gives the seed, which the
  story screen passes to `FromArt` so the screen takes the card's hue.
- `ListeningDial(hours, type, ground, modifier, labelSize, glyph)`: the 24-bar clock on a 288-unit
  drawing (hub radius 34, bars to 100, stroke 8.5), "12 am / 6 am / 12 pm / 6 pm" and the busiest
  hour labelled, the listener type's glyph (`listenerSymbol`) in a secondary-container hub. Bars are
  `primary` mixed 58% into `ground`; the busiest is `primary`.
- `recapPeriodLabel(data)`, `recapHeadline(data)`, `listenerTitle(type)`, `recapClockLine(type, hours)`:
  Recap's wording from the shared copy, including the switch from 24 December ("2026", "Your 2026
  Recap").
- `FitText(text, style)`: one line that shrinks its font to fit instead of wrapping (big figures).
  `cardText(size, line, weight, width, round)`: Google Sans Flex at any size on the width (50–100)
  and ROND axes; the mock's 25% and 151% widths and 900 weight are clamped to the bundled ranges.

Not in the kit, as in pixel/KIT.md: the mini player, Now Playing, the collection header block and
Recap's charts are screen work, composed from the parts above (the gallery shows a mini player and
both headers built that way). The collection header follows sheet 04's `.chead.tf` arrangement
(title, byline and meta, then the cover with the `.ccluster` beside it), drawn in
ExpressiveCollections.kt with the collapsing bar and the missing-item state; the full-screen lyric
lines (sheet 06 frame D: current 34/40 in primary, others 24/32 medium, sung in outline) are drawn in
ExpressiveLyrics.kt, because they differ from the kit's `LyricLine` as the sheet's open question says.
`contextSymbol(kind)` and `contextLabel(name, kind)` (ExpressiveNowPlaying.kt) give the glyph and the
shared name for what is playing; the queue's bar uses them too. The shell and the mini player are `ExpressiveShell` and
`ExpressiveMiniPlayer` (ExpressiveShell.kt); the `.dock` and `.nav` placement is there.

## Equaliser (sheet 11)

`ExpressiveEqualiser.kt` holds the sheet over Now Playing (`ExpressiveEqualiserSheet`) and Settings >
Playback > Equaliser (`ExpressiveEqualiserPage`); `BandGraph.kt` holds the drawing. Both screens are the
same two group cells, so anything else that edits a curve can reuse them:

| Sheet 11 class | Compose | Notes |
|---|---|---|
| `.eq-scope`, `.eq-scope.own` | `EqualiserScopeCell(scope, detail)` | which setting the bands change. Default: a round `secondaryContainer` lead on the group's cell colour. A song's or album's own: the lead at 12dp corners in `primary`, the glyph filled ("squared off means on"), and the whole cell `primaryContainer` with both lines `onPrimaryContainer`, drawn behind the row inside the group cell's clip; the corner moves on `fastSpatial`, the colours on `defaultEffects`, and only when the scope changes. One semantics node, a polite live region. `scopeSymbol(scope)` is the glyph each scope goes by everywhere: `LibraryMusic`, `Album`, `MusicNote` |
| `.eq-cell` | `EqualiserEditor(state, actions)` | the preset chips over the band graph, and the preset dialogs (save, rename, delete) from `state.dialog`. Chips are in 48dp targets, so the cell pads 4dp above them and pulls the graph 8dp up (`Modifier.pullUp`) |
| `.eq-cell .chips` | inside `EqualiserEditor` | a `LazyRow` of `ExpressiveFilterChip`s read as radio buttons, the selected one filled with `primary` (`onPrimary` ink) while on and tonal while off, cross-faded on `defaultEffects` (`ExpressiveFilterChip`'s `selectedContainerColor` / `selectedContentColor`; Library's chips keep the tonal default): "Custom" and "Save as preset" while the bands match no preset, then saved presets, then the built-in ones. A saved preset is a chip drawn here to the same measure, because it takes a long press and ends in `MoreVert` while selected; its menu is `ExpressiveMenu(offset = DpOffset(0.dp, (-4).dp))`. The row opens with the chip before the selected one first, 4dp further on past the start so nothing of the chip before that shows, and scrolls to the selected chip when the selection changes. Off: 50% alpha |
| `.eq-plot`, `.eq-freqs` | `BandGraph(curve, on, onBand, modifier)` | one `drawWithCache` canvas, 236dp tall at font scale 1: 20dp of room, the 192dp plot (8dp a decibel, 0 dB in the middle), 8dp, the frequency labels; a 30dp gutter for "+12 / 0 / −12" and 10dp after the last band. Always left to right |
| `.eq-acts`, `.eq-undo`, `.eq-help` | `EqualiserActions` in ExpressiveEqualiser.kt | one block, 82dp in every scope at font scale 1 (16 + 40 + 10 + 16), so the sheet does not move when a setting is saved or removed: a `ButtonGroup(fill = true)` of small buttons (filled save for the song, tonal save for the album, outlined removal) and one centred `bodySmall` caption 10dp under it. The removal's consequence is its caption, under that button alone when a save sits beside it; the button reads it as "label. consequence" and the caption is left out of semantics |
| `.eq-on` | `EqualiserIndicator` in ExpressiveNowPlaying.kt | Now Playing's header pill: 32dp, `surfaceContainerHigh`, the glyph in `primary`; fades on `defaultEffects`, and composes nothing while the song plays with the default |

How `BandGraph` keeps a drag cheap, which anything added to it has to keep:

- The gains on screen are one `FloatArray` in a `mutableStateOf(…, neverEqualPolicy())`, changed in
  place and read only in the draw block. A drag invalidates drawing, never composition. So do the
  colours (`rememberUpdatedState` of the scheme) and the on/off blend.
- The response path is rebuilt in the draw cache only when the gains differ from the ones last drawn
  (96 samples of `EqualiserBands.response`). The labels are measured once per size.
- `onBand` is called at most once a frame (`snapshotFlow` + `conflate`, then a frame's wait), and once
  more as the finger lifts. Whole decibels only, so the model changes at most 24 times across the range.
- A new `curve` from the model is eased to over 200 ms by one animation; while a finger is down the
  finger wins. A double tap and a screen reader's step go through the model and come back the same way.
- Touch: one `pointerInput` picks the band from x and follows vertical movement from where the bar is.
  A tap does nothing. Ten empty boxes over the columns carry the slider semantics ("31 Hz", "+4 dB",
  steps of 1 dB, "Set to 0 dB").

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
- Context names under "Playing from" are `context_*`. The model hands over the name the queue was
  saved with ("Favourites", or "Liked Songs" in an older saved queue), so map it by `ContextKind`.
- The style picker's three strings (`settings_style*`) are the only copy not in CURRENT-UI.md. Two
  format strings join existing values the way sheets 07 and 09 draw them: `add_songs_subtitle`
  ("Late drive, 14 songs") and `story_position` ("5 of 10", which replaced the progress segments).
- The playlist name dialog's callers pass `dialog_*_title` and `create` / `rename` already, so the
  dialog draws them as they come.
- Wording deliberately kept from today although the mocks differ: library subtitles stay
  "Playlist, 46 songs" / "Artist"; the album detail stays "Album • 2025". The Favourites empty body
  says "Tap the heart on a song to save it." because the plus became a heart.

## Tablet (medium and expanded windows)

docs/design/2026-11-25-tablet/SPEC.md is the rulebook; the design file's Tablet page draws it. Each
screen reads `LocalWindowLayout` once at its top and keeps its phone path as it was, so a compact
window composes nothing new. The shared sizes and helpers are in `ExpressiveAdaptive.kt`.

**How the shell switches.** `ExpressiveShell` hands a window whose `nav` is `Rail` to
`ExpressiveWideShell` (ExpressiveWideShell.kt): the rail at the start edge, the destinations beside
it, and at the bottom of the content area the player bar (`player == Bar`, expanded) or the phone's
mini player floating at most 576dp wide and centred, 12dp above the gesture area, over a band of
`surface` behind the handle (`player == Floating`, medium). Now Playing, the queue and lyrics still
cover the whole window; the snackbar sits 16dp above the bar or mini player. Destinations get the
room the bar or mini player takes, plus 24dp, as `LocalBottomPadding`. The movable `ShellSlots` are
the same in both arrangements, so a resize keeps their state. The mini player slot draws the player
bar itself when `player == Bar` (`ExpressiveMiniPlayer`); the shell provides the queue and lyrics
buttons' actions through `LocalPlayerBarLinks`.

| Component | File | Dimensions |
|---|---|---|
| `ExpressiveNavigationRail` | ExpressiveRail.kt | 96dp on `surface`, no divider. Headroom mark 32dp in `primary`, 20dp under the status bar; the first item 34dp below it (28 to the item, whose pill sits 6 into it). Items are tabs in a selectable group: a 56 x 32 pill (`secondaryContainer`, icon filled, when selected), label in labelMedium 4dp under, 12dp between items. Focus and hover show on the pill. |
| `ExpressivePlayerBar` | ExpressivePlayerBar.kt | An 80dp row on the cover scheme's `surfaceContainer`, top corners 28dp, its surface running on behind the gesture inset. Start: 56dp cover, title (bodyLarge SemiBold) over artist, at most 320dp; tapping them opens Now Playing. Centre (36% of the bar, 320 to 480dp): previous, a 48dp play control with no ring (round paused, 16dp corners playing), next, over the elapsed time, `WavyLinearProgress` and the length. End: heart, queue, lyrics. |
| `CollectionPanes` | ExpressiveCollectionPanes.kt | Lays a collection out for its pane's width (`BoxWithConstraints`, tablets only). 760dp and wider: a 360dp start pane (320 below 1000) with the header as a card (cover scheme `surfaceContainer`, 32dp corners, 16dp padding, cover as wide as the card, or narrower and centred on a short window so the card ends above the player bar (`CoverToFit`); play/shuffle/add in one row of medium buttons, Play on a row of its own above the other two when the row leaves it too little room for its label), the songs beside it. 600 to 760: one column with the header as a row (cover 200dp, titles over the buttons beside it). Narrower: the phone layout. Beside the Library's list there is no bar; the menu moves to the end of the header's buttons. |
| `ExpressiveNowPlayingWide` | ExpressiveNowPlayingWide.kt | Two halves on an expanded landscape window. Start: the phone's header, the cover (from the height, on the 8dp grid, at most 440) with the next cover's 72dp after it, in a column never narrower than the transport (320dp; the cover then starts at the column's start), title block (no sung line under 840dp tall), seek bar, transport, tools. End: a 320dp connected button group (Up next, Lyrics; `rememberSaveable`, opening on `LocalInitialPlayerPanel`), the full-screen button at the end of its row with Lyrics on, then `QueueList` or `LyricsBody`, faded at their top and bottom edges. |

**Screens.** The page margin is 24dp: destinations add `WideExtraMargin` (8dp) to the 16dp their
components carry, and shelves run on through the end margin to the window's edge (`Modifier.bleed`).
Lists of rows sit in columns at least 360dp wide, each column its own group filled top to bottom
(`columnGroupItems`); grids of tiles in cells at least 160dp wide. Home: no mark in the bar, the
filter at most 512dp with its margins, the carousel 200dp tall around 260dp items under 840dp of
window height and 280 around 360 above it (`coverSlot` makes a cover fill a slot wider than tall),
the Recap card beside Instant mixes on expanded (40% of the row, at most 480dp), the mixes in three
columns, Start listening in two columns on expanded. Library: in the list pane the open row takes
`CollectionRow(selected = true)`. Search: the field at most 720dp, Browse tiles in cells of 180dp.
Queue, Settings, licences, Recap and a playlist's reorder mode: a centred column at most 720dp
(`readableInset`), the bar spanning the window. Upright Now Playing: the phone's player in a
centred 576dp column, the cover at most 480dp, its gaps kept instead of spread. Full-screen lyrics:
on an expanded landscape window a 360dp start pane with one card (close, cover, title, seek bar,
play controls) and the lines in a centred 640dp column; otherwise a centred 640dp column. Recap
story: a phone's 411:914 proportions, as tall as the window. First run: a centred 560dp column.
Sheets keep Material's 640dp; dialogs Material's 560, the New playlist dialog 400.

**Input.** A song row opens its menu on a right click on medium and expanded windows
(`onSecondaryClick`, in `SongRow`). The rail items, the player bar's song area and buttons, and the
panel switch draw a ripple, which also draws focus and hover. Home's carousel items and Favourites
pill have no indication on any window; that predates the tablet work.

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

The first run (ExpressiveOnboarding.kt, ComfortOnboarding.kt) opens with `--es screen onboarding`,
in the style given by `--es style`. Extras: `step` 0 to 3, `audio` and `notifications` (`notasked`,
`granted`, `refused`), `songs` (-1 still scanning, 0 empty, or a count; default 127). Continue,
back, the look and theme work; Allow grants at once. Nothing is saved and no permission is asked.

```
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es screen onboarding --ei step 1
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es screen onboarding --es style comfort --ei step 3 --ei songs 0
```

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
- The equaliser's response is the engine's (`EqualiserBands.response`), which passes through the tops
  of the bars; sheet 11 draws a stand-in with a bump per band. The frequency labels shrink to fit their
  column at a large font size, which the sheet does not show.
