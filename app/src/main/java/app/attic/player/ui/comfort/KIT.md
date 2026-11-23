# Comfort kit (Compose)

The Compose side of `docs/design/2026-11-21-ui-directions/oneui/` (KIT.md, kit.css,
00-direction.html). Screens built in Comfort draw only with what is here. Everything is in package
`app.attic.player.ui.comfort`, except two shared pieces it borrows from `ui/expressive/`: the `Art`
type (`Art.of(mediaId)`, `Art.Picture(bitmap)`, Cover.kt) and the `Symbol` enum (Symbols.kt).
Components take plain data and lambdas; none of them reach app singletons. Comfort is never called
"One UI" or "Samsung" in anything a user sees.

Material 3 in this BOM is 1.4.0. Used from Material: Button (inside `ComfortButton`),
ModalBottomSheet (`ComfortSheet`), DropdownMenu (`ComfortMenu`), SnackbarHost (`ToastHost`),
SwipeToDismissBox (`SwipeToRemove`). Drawn here because Material's differ from the kit: the switch
(Material's knob is 16dp when off and its track is outlined), the radio (Material's is 20dp, the
kit's 24), the dialog (Material's AlertDialog centres itself and right-aligns its buttons; the kit's
floats at the bottom with two split buttons), progress (a 10dp capsule, not a line), the spinner.

## Theme

`AtticTheme(mode, UiStyle.Comfort)` (ui/Theme.kt) sets up:

- **Colour**: the kit's fixed palette, dark or light from ThemeMode. No wallpaper colour. Read the
  tokens with `Comfort.colors` (a `ComfortColors`, from `LocalComfortColors`), named as kit.css
  names them: `ground card raised fill fill2 line edge on on2 on3 accent onAccent accentInk
  accentSoft onAccentSoft danger onArt knob toast onToast scrim shadow shadowSmall miniBase miniFill`,
  plus `miniInk2` (below). `ComfortDark` / `ComfortLight` are the two palettes.
- `MaterialTheme.colorScheme` is the same tokens (`ComfortColors.toColorScheme()`), so stock
  components inside follow: primary = accent, onPrimary = onAccent, primaryContainer and
  secondaryContainer = accentSoft, surface = ground, surfaceContainer(Low) = card,
  surfaceContainerHigh = raised, surfaceContainerHighest = fill, onSurfaceVariant = on2,
  outline = on3, outlineVariant = line, inverseSurface = toast, error = danger. There is one accent:
  secondary and tertiary repeat accentInk. Surface tint is transparent.
- **Type**: Onest. `Comfort.type` (`ComfortTypography`), also `MaterialTheme.typography` (mapping
  in ComfortType.kt).
- **Shape**: `MaterialTheme.shapes` is 8 / 18 / 18 / 26 / 30. The rest is `ComfortShapes`.
- Rules from the kit that the tokens cannot enforce: amber marks selection, the one filled button
  on a screen, switches and progress. Titles and row text stay in `on`. A current song is never amber
  text. In light, amber as text or an icon is `accentInk` (Brass), never `accent`.

## Album-art colour: `Tinted`

```kotlin
val seed = rememberArtColor(track.mediaId, Color.Unspecified, deepen = false)   // ui/Art.kt
Tinted(seed) { … }               // .tinted: accent tokens and the mini player's fills
Tinted(seed, wash = true) { … }  // .tinted.wash: also the surfaces and the quieter inks
TintedCover(mediaId, wash = true) { … }   // the same, looking the swatch up itself
```

- Pass the cover's **swatch** (`deepen = false`), not the deepened tint Expressive uses:
  the kit keeps the cover's chroma, so a dulled seed gives a dull player.
- How it maps the kit rule (`ComfortTint`, Tinted.kt): kit.css writes each token as
  `oklch(from var(--tint) L calc(c*k) h)`. `ComfortTint.apply` converts the seed to OKLCH (Oklch.kt)
  and, per token, keeps its hue, multiplies its chroma by k and sets lightness L, with the table
  copied from kit.css section 2 for dark and light. Light `.tinted` sets onAccent to plain white,
  as the CSS does. Out of sRGB gamut, chroma is reduced at the same L and h (CSS Color 4's gamut
  mapping minus its JND clip), so a token's lightness, and with it contrast, never moves.
- `.tinted` moves accent, onAccent, accentInk, accentSoft, onAccentSoft, miniBase, miniFill,
  miniInk2. `wash` also moves ground, card, raised, fill, fill2, line, on2, on3. `on` never changes.
  Tokens the rule does not name keep the value around them, so a `Tinted` inside a washed one keeps
  the washed surfaces, as nested CSS classes would.
- `MaterialTheme.colorScheme` is rebuilt from the tinted tokens, so Material components inside
  follow too. Amber does not appear inside.
- Every token animates over 450 ms when the seed changes. `Color.Unspecified` (no art) keeps the
  palette around it, which is amber.
- Where to use it, from the sheets: Now Playing, the queue and full-screen lyrics are
  `Tinted(wash = true)` on the whole screen. The mini player tints itself (`MiniPlayer(seed = …)`).
  A browse tile takes `BrowseTile(seed = …)`. Widgets: `wash = true`. Spend the colour on progress
  and on-state; leave titles in `on`.
- A nested piece that should paint itself with the washed surface: `ComfortSurface(card = true)`
  (`.bg-card`) or `ComfortSurface()` (`.bg-ground`).
- `miniInk2` is Attic's one addition to the token set: the mini player's second line. kit.css draws
  it in `--on-2`, which falls to 3.6:1 over a cover-coloured fill. It is `on2` untinted and
  `oklch(.82, c*.3, h)` / `oklch(.45, c*.3, h)` tinted.
- Unit tests (ComfortTintTest): every text role on its background holds 4.5:1 for 1,145 seeds
  (the 16 fixture tints, a hue sweep at 20 strengths, primaries, greys, 400 random colours), dark and
  light, tinted and washed. `on3` (hints, placeholders, trailing icons) is below 4.5:1 in the kit's
  own palette (4.3:1 on black, 2.8:1 on white); the test holds it to never being weaker than that.

## Type

| Kit | Compose (`Comfort.type.…`) | Material slot |
|---|---|---|
| `.hero h1` 40/46 700 | `hero` | displayLarge/Medium |
| `.c-head h1`, `.np-title h1` 26/32 700 | `title` | headlineMedium |
| `.c-head.person h1` 30/36 700 | `personTitle` | headlineLarge |
| `.bar h2`, sheet, dialog, empty titles 20/26 700 | `barTitle` | titleLarge |
| `.txt b` 16/22 500 | `rowTitle` | titleMedium |
| `.row.lg .txt b` 18/24 700 | `rowTitleLarge` | |
| body 15/21 | `body` | bodyLarge |
| `.txt span` 13.5/18 | `sub` | bodyMedium |
| `.txt em`, tile second lines 12.5/16 | `caption` | bodySmall |
| `.label` 14/18 600 | `label` | titleSmall |
| `.btn` 15.5/20 600 | `button` | labelLarge |
| `.stat b` 26/32 700 tabular | `stat` | |
| `.lyrics-card .ln` 22/28 700 | `lyric` | headlineSmall |

`onest(size, line, weight, tracking)` builds any other style (tracking in em, as the CSS writes it);
`style.tabular()` is `font-variant-numeric:tabular-nums`. Styles centre text in the line box as CSS
does. Onest is bundled once (res/font/onest.ttf, 65 KB, weight 400–800, Latin); the family pins
each weight on the axis, so `FontWeight(600)` is a real semibold. Other scripts fall back to the
system font (the direction sheet's open question).

## Shape

| Kit | Compose |
|---|---|
| `.sq` (up to 96dp), `.sq-lg` (from 100dp) | `ArtShape`: picks by the size it is drawn at. `ComfortShapes.Squircle` (n = 4), `SquircleLarge` (n = 5), `ComfortShapes.art(size)` |
| `.sq-soft` (artists) | `ComfortShapes.SquircleSoft` (n = 2.6) |
| `--r-float` 30 | `ComfortShapes.Floating`, `FloatingTop` (sheets), `FloatRadius` |
| `--r-group` 26 | `ComfortShapes.Group`, `GroupRadius` |
| `--r-ctl` 18 | `ComfortShapes.Control`, `ControlRadius` |
| menu 22 | `ComfortShapes.Menu` |
| pills (buttons, chips) | `ComfortShapes.Pill` |
| radio, knob, spinner | `ComfortShapes.Circle`, nothing else |

The superellipses are `SuperellipseShape(exponent)`: `|x|^n + |y|^n = 1` traced at 720 points. kit.css
samples the same curves (`x = cos(t)^(2/n)`) at 72 or 88 points; ComfortShapesTest checks every CSS
point lies on the Compose outline. Never `RoundedCornerShape` on artwork; never a circle for an artist.

## Icons and the mark

`ComfortIcon(Symbol.QueueMusic, size = 24.dp, filled = false, tint = LocalContentColor.current)`:
the shared Material Symbols Rounded subset (res/font/material_symbols_rounded.ttf, which keeps the
wght 300–700, FILL and opsz axes) at **weight 300**, as the kit draws it. `filled` is `.ms.fill`:
the selected tab, a favourite that is on, play and pause. The transport and play glyphs are weight
400, as kit.css sets them (`PlayButton`, `SkipButton` do it). Every name oneui/KIT.md lists is in
`Symbol`; one that is not is not in the font (see Symbols.kt to add one).

The mark is "Headroom" (ui/expressive/Mark.kt, shared): `ComfortMark(size, tint = accent)` (24dp;
30 above Home's hero), `MarkBox(size, style)` (the mark at half an icon box). It takes the accent,
so inside `Tinted` it is the cover's colour.

## Components, by kit class

Rows read `LocalHairlineAbove` and draw their own hairline at their `--sep`; `Group`, `Hairlined`
and `groupItems` set it. `LocalComfortContainer` (Ground, Card, Sheet, Dialog) tells chips and the
search field to take `fill` on a card, and song rows to indent in a sheet; `Group`, `GroupCell`,
`ComfortSheet`, `DialogCard` and `OnCard { }` set it.

| Kit class | Compose | Notes |
|---|---|---|
| `.cv.sq`, `.cv.sq-lg` | `Cover(art, modifier, size = null, shape = ArtShape, dimmed = false)` | size null fills the width as a square; no art shows the neutral note box; `dimmed` is the current song's brightness(.4) |
| `.cv.sq-soft` | `ArtistCover(art, modifier, size)` | |
| `.mosaic` | `Mosaic(arts, modifier, size)` | 2×2 at four or more |
| `.ibox` (`.neutral`, `.solid`) | `IconBox(symbol, modifier, size, style = Tonal/Neutral/Solid, filled)`, `FavouritesArt(m, size)`, `NoArt(m, size)`, `MarkBox` | icon at 46%; neutral on a dark sheet is `--fill-2` |
| `.hero`, `.hero.short` | `Hero(title, modifier, subtitle, short, mark, insets = statusBars)` | pads for the status bar itself; `mark` on Home only |
| `.tools` | `Tools(modifier, leading = { SubTabs(…) }) { icon buttons }` | settings last |
| `.bar`, `.bar.lift`, `.bar.count` | `Bar(title, modifier, onBack, backSymbol, backDescription, lift, accentTitle, insets) { actions }` | `title = ""` at the top of a detail screen; `backSymbol = ExpandMore` on overlays; describe the back button with `backDescription` (default "Back") |
| `.subtabs`, `.subtabs.lift` | `SubTabs(options, selectedIndex, onSelect, modifier, lift)` | exactly one selected; Home filters, Recap periods |
| `.chips`, `.chip` | `ChipRow { FilterChip(label, selected, onClick) }` | Library's switch-off-able filters |
| `.search` | `SearchField(query, onQueryChange, placeholder, modifier, onSearch, focusRequester)`; `SearchFieldButton(placeholder, onClick)` | clear button appears with text |
| `.label` (`small`, `.end`, `.tight`) | `Label(text, modifier, small, tight, end = { … })` | |
| `.note` | `Note(text)` | |
| `.group`, `.group.pad`, `.group + .group` | `Group(modifier, pad, margin = 16.dp) { item { … } }`; `GroupSpacing` (12dp) | lazy: `LazyListScope.groupItems(list, key) { i, x -> … }` or `GroupCell(index, count) { … }` |
| hairline-split rows with no card (sheet rows) | `Hairlined { item { … } }` | |
| `.inset` | `Inset { … }` | a shelf or grid inside a group |
| `.row`, `.row.sm`, `.row.lg`, `.val`, chevron | `CollectionRow(title, subtitle, lead = { m -> Cover(art, m) }, onClick, modifier, size = Small/Medium/Large, overline, value, chevron, onLongClick, trailing)` | lead gets the art size in its modifier; in a sheet it pads 24 / 18 (`.sheet .row`) |
| `.txt` (`em`, `b`, `span`) | `RowText(title, modifier, subtitle, overline, titleStyle, …, wrap)` | |
| `.kv` | `KeyValue(label, value)` | Credits |
| `.song` | `SongRow(title, subtitle, modifier, lead = SongLead.Cover(art)/Artist(art)/Number(n)/None, onClick, onLongClick, current, playing, rank, meta, lifted, trailing = { … })` | `.now` = `current`; `.numbered` = `Number`; `.bare` = `None`; `.ranked` = `rank`; `.meta` = `meta`; `.lifted` = `lifted` |
| song trailing | `SongMoreButton(title, onClick)`, `SongDragHandle(title, modifier)`, `SongAddButton(title, added, onToggle)`, `SongRadioMark()` | describe themselves from the shared strings; pass the reorder library's handle modifier to `SongDragHandle` |
| `.swipe`, `.swipe.r` | `SwipeToRemove(onRemove, modifier, resetKey) { SongRow(…) }` | either way; `do_not_disturb_on` and "Remove" in danger on fill, on the side it uncovers |
| `.bars`, `.bars.still` | `PlayingBars(playing, modifier, size = 16.dp, barWidth, gap, color)` | bounces while playing (Attic's choice; the mock is still) |
| `.tile`, `.tile.center`, `b.two`, `.tile.now` | `Tile(title, onClick, art = { m -> Cover(art, m) }, modifier, subtitle, width = 132.dp, centered, twoLines, artSize, now, playing, onLongClick, subtitleLines = 1)` | `width = null` in a grid; `subtitleLines = 2` lets the second line wrap (Search's result shelf) |
| `.shelf` | `Shelf { item { Tile(…) } }` | LazyRow, 14dp gaps, 24dp edges (16 in a group) |
| `.grid2/3/4`, `.tray` | `TileGrid(count, columns, modifier, tray) { i -> Tile(…, width = null) }`; tray tiles: `artSize = trayArt(columns), centered = true` | not lazy; put a tray in `Group(pad = true)` |
| `.pager` | `Pager(pageCount) { page -> item { SongRow(…) } }` | 312dp cards, 10dp apart |
| `.browse` (`.tinted.wash`) | `BrowseTile(title, onClick, art = { m -> Mosaic(…, m) }, modifier, subtitle, seed)` | art gets 56dp; a seed washes the card |
| `.c-head`, `.c-head.person` | `CollectionHeader(title, art = { m -> Cover(art, m) }, modifier, byline, onBylineClick, meta, person, artSize)` | art gets 196dp (148 for a person) unless `artSize`; sheet 04 draws 176 |
| `.c-actions` | `CollectionActions { CollectionPlayButton(playing, onClick, Modifier.weight(1f), enabled); TonalIconButton(…, large = true) }` | Play reads Pause while this collection plays; `enabled = false` is `.off` |
| `.btn` (`.filled .tonal .plain .text`, `.lg .sm .block .danger .off`) | `ComfortButton(text, onClick, modifier, style, size = Small/Medium/Large, leading, leadingFilled, block, danger, enabled)` | |
| `.ib` (`.dim`, `.off`) | `ComfortIconButton(symbol, contentDescription, onClick, modifier, dim, filled, enabled, iconSize, tint)` | 48dp target |
| `.ib.tonal` (`.lg`, `.on`, `.off`) | `TonalIconButton(symbol, contentDescription, onClick, modifier, checked, large, filled, stateDescription, enabled, container)` | superellipse; checked = accent; `container` replaces `--fill` at rest (`--fill-2` on a dark sheet, `--card` on light ground) |
| `.ib.heart`, `.ib.tonal.lg.heart` | `HeartButton(favourite, onToggle, modifier, tonal, large)` | describes itself |
| `.play` (`.md`, `.sm`) | `PlayButton(playing, onClick, modifier, size = Large/Medium/Small)` | 76 / 56 / 44; describes itself |
| `.np-head` | `NowPlayingHeader(context, onClose, onMore, moreDescription, modifier, overline, insets)` | |
| `.np-cover`, `.paused` | `NowPlayingCover(art, paused, modifier)` | paused draws at 86%, animated |
| `.np-title` | `NowPlayingTitle(title, artist, modifier, onArtistClick)` | |
| `.np-lyric` | `NowPlayingLyric(line, modifier, onClick)` | |
| `.deck` | `Deck { SeekBar(…); Transport(…); Toggles { … } }` | |
| `.seek` | `SeekBar(progress = { f }, elapsed, total, onSeek, modifier, onScrub, contentDescription)` | tap or drag; `onScrub(fraction)` while dragging, `null` on release; fill never under 64dp |
| `.transport`, `.tp` | `Transport(playing, onPrevious, onPlayPause, onNext)`, `SkipButton(symbol, cd, onClick)` | |
| `.toggles` | `Toggles { TonalIconButton(…, large = true); HeartButton(…, tonal = true) }` | shuffle, repeat, favourite, queue |
| `.lyrics-card`, `.ln`, `.ln.cur`, `.ln.clip`, message | `LyricsCard(modifier, title) { LyricsLine(text, current, plain); LyricsMessage(text) }` | give a button or `Spinner(small = true)` inside `Modifier.padding(top = 14.dp)` |
| `.dock` | `Dock(modifier, mini = { MiniPlayer(…) }, insets) { TabBar(…) }` | `mini = null` with nothing queued; place at the bottom over content |
| `.mini` (`.top`, alone) | `MiniPlayer(title, artist, art, progress, playing, favourite, onToggleFavourite, onPlayPause, onClick, modifier, seed, placement = Dock/Alone/Top, onSwipeNext, onSwipePrevious, trailing)` | tints itself from `seed`; fill = progress; swipe text past 28% to skip; `trailing` replaces heart and play (lyrics: `PlayButton(size = Small)`) |
| `.tabs`, `.tab.on` | `TabBar(items = listOf(TabItem(label, symbol)), selectedIndex, onSelect)` | |
| `.sheet`, `.grab` | `ComfortSheet(onDismiss, modifier, sheetState) { … }`; `StaticSheet { … }` for previews | opens expanded |
| `.sheet-head`, `.sheet-title` (`small`) | `SheetHeader(title, subtitle, art = { m -> Cover(art, m) })`, `SheetTitle(text, modifier, subtitle)` | |
| `.sheet-row` (`.on`, `.danger`, `.val`, `.end.go`) | `Hairlined { item { SheetRow(symbol, label, onClick, modifier, value, selected, danger, end) } }` | `end`: a quiet trailing glyph (`chevron_right`, `unfold_more`) |
| `.dialog`, `.dialog-btns`, `.above-kbd` | `ComfortDialog(title, onDismiss, confirmText, onConfirm, dismissText, modifier, body, confirmEnabled, danger) { DialogField(…) }`; `DialogCard(…)` for previews | rides above the keyboard by itself; `dismissText = null` draws the confirm button alone, for a result to acknowledge (the restore result) |
| `.field` | `DialogField(value, onValueChange, label, modifier, placeholder, focusRequester, onDone)` | a prefilled value opens with the cursor at its end |
| `.menu`, `.menu.on-sheet`, `.menu-cap`, `.menu-row` | `ComfortMenu(expanded, onDismiss, modifier, offset, onSheet) { MenuCaption(text); MenuRow(symbol, label, onClick, modifier, selected, danger, check) }`; `StaticMenu` for previews | anchor: a Box around the trigger; `onSheet` takes `--fill`; `check` ends the selected row in a tick |
| `.toast` | `ToastHost(snackbarHostState, modifier)`; `Toast(text)` | place 180dp up with the mini player, 116 with tabs alone, 56 over an overlay |
| `.switch` | `ComfortSwitch(checked, onCheckedChange, modifier, enabled)` | `onCheckedChange = null` when the row toggles |
| `.radio` | `ComfortRadio(selected, onClick, modifier)` | |
| `.progress`, `.thick` | `ComfortProgress(progress = { f }, modifier, thick)` | |
| `.spinner`, `.sm` | `Spinner(modifier, small)` | turns |
| `.set-row`, `.lead-ctl`, `.val` | `SetRow(title, modifier, summary, value, onClick, leading, trailing)`, `SetSwitchRow(title, checked, onCheckedChange, modifier, summary, enabled)`, `SetRadioRow(title, selected, onClick, modifier, summary)` | put in a `Group` |
| `.set-block` | `SetBlock(afterRow) { SetBlockText(…); ComfortProgress(…, SetBlockProgressPadding); SetBlockSmall(…); ComfortButton(style = Text, Modifier.padding(top = 12.dp).offset(x = (-12).dp)) }` | |
| `.stats`, `.stat` | `Group { item { Stats(listOf(value to label, …)) } }` | |
| `.empty`, `.empty.fill` | `EmptyState(title, modifier, body, symbol, art, fill, action)` | |
| `.actionbar`, `.act` (`.go .danger .off`) | `ActionBar(modifier, insets) { ActionBarItem(symbol, label, onClick, go, danger, enabled) }` | replaces the dock in a mode |
| `.index`, `.index-pop` | `IndexScrubber(letters, current, onSelect, modifier, onScrub)`, `IndexPop(letter)`, or `IndexScrubberWithPop(letters, current, onSelect, modifier, top)` | give the list 28dp end padding |
| `.bg-ground`, `.bg-card` | `ComfortSurface(modifier, card, shape) { … }` | |
| floating shadow | `Modifier.floatingShadow(shape, small, edge)` | dock, sheet, dialog, menu, action bar; `small` for a lifted row, toast, knob |

Not in the kit, as in oneui/KIT.md: the screens themselves. The Comfort shell (ComfortShell.kt: the
dock over the content, `LocalBottomPadding`, the overlays and the toast), the collapsing hero-to-bar
of a root screen (ComfortRoot.kt: `RootPage`, `rememberCollapsed`, `rememberPinned`, `LeadTools`) and
Now Playing's page are screen work, composed from the parts above; the gallery shows each part on
its own. `ComfortPadding.Dock/Tabs/
ActionBar` are the mock's `.pad-dock` (176), `.pad-tabs` and `.pad-bar` (112) if a screen needs them
before a shell exists.

## Shared copy

Use the strings in `res/values/strings.xml` (see ui/expressive/KIT.md "Shared copy"). The kit's own
components already describe themselves with them: `play`, `pause`, `previous`, `next`, `back`,
`favourite_add` / `favourite_remove`, `more_options_for_cd`, `drag_to_reorder_cd`,
`add_songs_add_cd` / `add_songs_remove_cd`, `search_clear_cd`, `now_playing_close_cd`,
`now_playing_playing_from`, `now_playing_seek_cd`, `queue_remove`, `lyrics_title`.

## Gallery

The debug-only kit gallery (app/src/debug) has the Comfort strips, laid out like
oneui/00-direction.html, in ComfortGallery.kt:

```
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es style comfort --es theme dark
adb -s <serial> shell am start -n app.attic.player/.debug.KitGalleryActivity --es style comfort --es theme light --ei section 5
```

`section`: 0 header, 1 palette, 2 type, 3 shape, 4 icons and the mark, 5 album-art colour (Tinted),
6 album-art colour (Tinted, wash), 7 buttons, 8 hero, tools, sub-tabs, chips, search, 9 song rows,
10 collection rows and tiles, 11 dock, 12 Now Playing deck, 13 Now Playing page, lyrics card,
credits, 14 collection header, 15 artist header and bars, 16 settings and controls, 17 stats and
empty state, 18 menu, dialogs, toast, action bar, 19 bottom sheet, 20 index scrubber. The button in
the header flips the theme. It does not touch the app's settings. Without `--es style comfort` it
opens the Expressive kit.

The first run in Comfort: add `--es screen onboarding` (extras in ui/expressive/KIT.md, "Gallery").

## Known differences from the mock

- `miniInk2`: the mini player's second line is one step lighter (dark) or darker (light) than
  `--on-2` when tinted, so it holds 4.5:1 over the cover-coloured fill. The mock uses `--on-2`.
- Gamut mapping: where `oklch(L, c*k, h)` falls outside sRGB the browser clips each channel; here
  chroma is reduced at the same lightness and hue. Vivid covers come out a little less saturated
  than Chrome renders them, and their contrast is exactly the kit's.
- Tinted takes the Palette swatch as `--tint`. The mocks' tints are hand-picked mid tones; a
  cover whose vibrant swatch is very pale or very dark still gets the kit's lightness per token, but
  its chroma (and so how colourful the player is) follows the swatch.
- 05-now-playing.html's spec describes an older wash (mixes of `--tint-deep` / `--tint-soft`); this
  follows kit.css, which replaced it with the OKLCH rule.
- Shadows use Compose's `dropShadow` with the CSS values (blur 36 / offset 12 dark, 28 / 8 light);
  Android's blur is close to, not identical with, Chrome's.
- `.bars` animate while playing (the mock is a still picture).
- The spinner turns; the mock's is a still arc at 28°.
- The menu is Material's DropdownMenu: 8dp vertical padding inside (kit 6) and Material's own
  enter animation. Its shadow is an elevation shadow, not the CSS blur.
- The sheet is Material's ModalBottomSheet with the kit's colours, radius and grab handle; it opens
  fully expanded, and drag-to-dismiss is Material's.
- The dialog lives in a platform Dialog window with its dim turned off and the kit's scrim drawn
  instead.
- Optical sizes: the icon font is set at fixed optical sizes 20 / 24 / 40 by size, where the browser
  follows the exact size.
- Every Comfort surface now draws Comfort: the shell, the mini player, Home, Search, Library,
  Settings, Now Playing, the first run, the permission screen and loading, plus the collection screens, the queue,
  full-screen lyrics, the sheets, dialogs and menus, and Recap (page, story, cards; tables below).

## Screens drawn from the kit (sheets 04, 06, 07, 09)

| Surface | File | Notes |
|---|---|---|
| Album, artist, folder, Favourites, playlist, "is gone" | ComfortCollections.kt | a plain bar that lifts and shows the title once it has scrolled under; header art 176dp; Play row; songs in one card; playlist tools (`.pl-tools`) at the head of the card; reorder mode in its own full-screen window so the action bar can replace the dock |
| Queue | ComfortQueue.kt | `Tinted(wash = true)`; the current song as the mini player's filled row (`.q-now`); "Clear" as a raised pill over a fade (`.q-foot`) |
| Full-screen lyrics | ComfortLyrics.kt | `Tinted(wash = true)`; lines on the ground, the current one on an accent-soft bed (`.ly`); the mini player's row alone at the foot (`.dock.solo`) |
| Track menu, pickers, Add songs, dialogs | ComfortSheets.kt | quick actions as tonal toggles on `--fill-2` (`.quick .qa`); the language picker as a pop-over on its row; over the player the caller washes them with the playing cover |
| Recap page | ComfortRecap.kt | hero with the back arrow pulled into its foot (`.tools.lead`); sub-tabs pin under the bar when scrolled; `.playrow`, `.stats`, `.langs`, `.when` |
| Recap cards | ComfortRecapCards.kt | 360 by 640, always `ComfortDark` (`RecapCardColors`); top song, top artist and summary washed by their cover; ranked rows on beds in each cover's `--mini-fill`; `ListeningClock`, `languageSteps` (accent minus .13 / .26 / .38 L in OKLCH, "No vocals" `--on-3`) |
| Recap story | ComfortRecapStory.kt | full-screen window, dark, washed with the card; one deck: `ComfortProgress` with "4 of 10", Save (plain) and Share (filled); the app's messages as a `ToastHost` above the deck |

Shared helpers there: `songCount`, `totalLength`, `songsAndLength` (copy), `SystemBarsFor` (a
full-screen window's dim and bar icons), `rememberProgress` (how far a song has played).
