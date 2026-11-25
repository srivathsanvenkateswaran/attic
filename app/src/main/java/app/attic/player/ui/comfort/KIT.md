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
| `.song` | `SongRow(title, subtitle, modifier, lead = SongLead.Cover(art)/Artist(art)/Number(n)/None, onClick, onLongClick, current, playing, rank, meta, lifted, onSwipeToQueue, trailing = { … })` | `.now` = `current`; `.numbered` = `Number`; `.bare` = `None`; `.ranked` = `rank`; `.meta` = `meta`; `.lifted` = `lifted`; `onSwipeToQueue` wraps the row in `SwipeToQueueCell` |
| song trailing | `SongMoreButton(title, onClick)`, `SongDragHandle(title, modifier)`, `SongAddButton(title, added, onToggle)`, `SongRadioMark()`, `SongSmartPickMark()` | describe themselves from the shared strings; pass the reorder library's handle modifier to `SongDragHandle`, giving `Haptic.PickUp` in its `onDragStarted` and `Haptic.Drop` in `onDragStopped` (`ui/Haptics.kt`); the smart-pick mark (radio glyph, 20dp) goes before the handle on a queue row smart shuffle added |
| `.swipe`, `.swipe.r` | `SwipeToRemove(onRemove, modifier, resetKey) { SongRow(…) }` | either way; `do_not_disturb_on` and "Remove" in danger on fill, on the side it uncovers |
| swipe to queue (no mock) | `SwipeToQueueCell(onQueue) { … }`, or `SongRow(onSwipeToQueue = …)` | towards the end only; uncovers fill with `queue_music` in accentInk and "Add to queue" in on, turning to accentSoft / onAccentSoft once letting go would queue; settles on a critically damped spring (1 / 500), no overshoot. Thresholds and the confirm haptic are shared (`ui/SwipeToQueue.kt`). Never inside the queue, whose rows swipe to remove |
| `.bars`, `.bars.still` | `PlayingBars(playing, modifier, size = 16.dp, barWidth, gap, color)` | bounces while playing (Attic's choice; the mock is still) |
| `.tile`, `.tile.center`, `b.two`, `.tile.now` | `Tile(title, onClick, art = { m -> Cover(art, m) }, modifier, subtitle, width = 132.dp, centered, twoLines, artSize, now, playing, onLongClick, subtitleLines = 1)` | `width = null` in a grid; `subtitleLines = 2` lets the second line wrap (Search's result shelf) |
| `.shelf` | `Shelf { item { Tile(…) } }` | LazyRow, 14dp gaps, 24dp edges (16 in a group) |
| `.grid2/3/4`, `.tray` | `TileGrid(count, columns, modifier, tray) { i -> Tile(…, width = null) }`; tray tiles: `artSize = trayArt(columns), centered = true` | not lazy; put a tray in `Group(pad = true)` |
| `.pager` | `Pager(pageCount) { page -> item { SongRow(…) } }` | 312dp cards, 10dp apart |
| `.browse` (`.tinted.wash`) | `BrowseTile(title, onClick, art = { m -> Mosaic(…, m) }, modifier, subtitle, seed)` | art gets 56dp; a seed washes the card |
| Mood tile, Mood picks card (design) | `MoodTile(tile, onClick)`, `MoodPicksCard(onChoose, onDismiss)` in `ComfortMoods.kt` | Home's Moods shelf: the browse card at 200 by 133, mosaic top left, count top right; the card is `--card` with a filled and a text button |
| `.c-head`, `.c-head.person` | `CollectionHeader(title, art = { m -> Cover(art, m) }, modifier, byline, onBylineClick, meta, person, artSize)` | art gets 196dp (148 for a person) unless `artSize`; sheet 04 draws 176 |
| `.c-actions` | `CollectionActions { CollectionPlayButton(playing, onClick, Modifier.weight(1f), enabled); TonalIconButton(…, large = true) }` | Play reads Pause while this collection plays; `enabled = false` is `.off` |
| `.btn` (`.filled .tonal .plain .text`, `.lg .sm .block .danger .off`) | `ComfortButton(text, onClick, modifier, style, size = Small/Medium/Large, leading, leadingFilled, block, danger, enabled)` | |
| `.ib` (`.dim`, `.off`) | `ComfortIconButton(symbol, contentDescription, onClick, modifier, dim, filled, enabled, iconSize, tint)` | 48dp target |
| `.ib.tonal` (`.lg`, `.on`, `.off`) | `TonalIconButton(symbol, contentDescription, onClick, modifier, checked, large, filled, stateDescription, enabled, container)` | superellipse; checked = accent; `container` replaces `--fill` at rest (`--fill-2` on a dark sheet, `--card` on light ground) |
| `.ib.heart`, `.ib.tonal.lg.heart` | `HeartButton(favourite, onToggle, modifier, tonal, large)` | describes itself |
| `.play` (`.md`, `.sm`) | `PlayButton(playing, onClick, modifier, size = Large/Medium/Small)` | 76 / 56 / 44; describes itself |
| `.np-head` | `NowPlayingHeader(context, onClose, onMore, moreDescription, modifier, overline, insets, trailing)` | `trailing` sits before the more button and the context name gives up its width to it (the equaliser's capsule) |
| `.eq-on` | `EqualiserIndicator(scope, onClick)` | Now Playing's header, while the song plays with its own or its album's equaliser setting; nothing with `scope = null`; fades over 200 ms |
| `.np-cover`, `.paused` | `NowPlayingCover(art, paused, modifier)` | paused draws at 86%, animated |
| `.np-title` | `NowPlayingTitle(title, artist, modifier, onArtistClick)` | |
| `.np-lyric` | `NowPlayingLyric(line, modifier, onClick)` | |
| `.deck` | `Deck { SeekBar(…); Transport(…); Toggles { … } }` | |
| `.seek` | `SeekBar(progress = { f }, elapsed, total, onSeek, modifier, onScrub, contentDescription)` | tap or drag; `onScrub(fraction)` while dragging, `null` on release; fill never under 64dp; no Smart transition mark (the design file's notch was reviewed and not built: under the total time it said little) |
| `.transport`, `.tp` | `Transport(playing, onPrevious, onPlayPause, onNext)`, `SkipButton(symbol, cd, onClick)` | |
| `.toggles` | `Toggles { TonalIconButton(…, large = true); HeartButton(…, tonal = true) }` | shuffle, repeat, favourite, queue |
| `.lyrics-card`, `.ln`, `.ln.cur`, `.ln.clip`, message | `LyricsCard(modifier, title) { LyricsLine(text, current, plain); LyricsMessage(text) }` | give a button or `Spinner(small = true)` inside `Modifier.padding(top = 14.dp)` |
| `.dock` | `Dock(modifier, mini = { MiniPlayer(…) }, insets) { TabBar(…) }` | `mini = null` with nothing queued; place at the bottom over content |
| `.mini` (`.top`, alone) | `MiniPlayer(title, artist, art, progress, playing, favourite, onToggleFavourite, onPlayPause, onClick, modifier, seed, placement = Dock/Alone/Top, onSwipeNext, onSwipePrevious, trailing)` | tints itself from `seed`; fill = progress; swipe text past 28% to skip; `trailing` replaces heart and play (lyrics: `PlayButton(size = Small)`) |
| `.tabs`, `.tab.on` | `TabBar(items = listOf(TabItem(label, symbol)), selectedIndex, onSelect)` | |
| `.sheet`, `.grab` | `ComfortSheet(onDismiss, modifier, sheetState) { … }`; `StaticSheet { … }` for previews | opens expanded |
| `.sheet-head`, `.sheet-title` (`small`) | `SheetHeader(title, subtitle, art = { m -> Cover(art, m) })`, `SheetTitle(text, modifier, subtitle)` | |
| `.sheet-row` (`.on`, `.danger`, `.val`, `.end.go`, `.two`) | `Hairlined { item { SheetRow(symbol, label, onClick, modifier, value, selected, danger, end, summary) } }` | `end`: a quiet trailing glyph (`chevron_right`, `unfold_more`); `summary`: a second line saying what the row leads to, in a 64dp row ("Use default" / "Removes this song's setting") |
| `.dialog`, `.dialog-btns`, `.above-kbd` | `ComfortDialog(title, onDismiss, confirmText, onConfirm, dismissText, modifier, body, confirmEnabled, danger) { DialogField(…) }`; `DialogCard(…)` for previews | rides above the keyboard by itself; `dismissText = null` draws the confirm button alone, for a result to acknowledge (the restore result) |
| `.field` | `DialogField(value, onValueChange, label, modifier, placeholder, focusRequester, onDone)` | a prefilled value opens with the cursor at its end |
| `.menu`, `.menu.on-sheet`, `.menu-cap`, `.menu-row` | `ComfortMenu(expanded, onDismiss, modifier, offset, onSheet) { MenuCaption(text); MenuRow(symbol, label, onClick, modifier, selected, danger, check) }`; `StaticMenu` for previews | anchor: a Box around the trigger; `onSheet` takes `--fill`; `check` ends the selected row in a tick; an on/off row (smart shuffle) is `selected = on, check = true` with `Modifier.semantics { toggleableState = … }` |
| `.toast` | `ToastHost(snackbarHostState, modifier)`; `Toast(text)` | place 180dp up with the mini player, 116 with tabs alone, 56 over an overlay |
| `.switch` | `ComfortSwitch(checked, onCheckedChange, modifier, enabled)` | `onCheckedChange = null` when the row toggles |
| `.radio` | `ComfortRadio(selected, onClick, modifier)` | |
| `.progress`, `.thick` | `ComfortProgress(progress = { f }, modifier, thick)` | |
| `.spinner`, `.sm` | `Spinner(modifier, small)` | turns |
| `.set-row`, `.lead-ctl`, `.val` | `SetRow(title, modifier, summary, value, onClick, leading, trailing)`, `SetSwitchRow(title, checked, onCheckedChange, modifier, summary, enabled)`, `SetRadioRow(title, selected, onClick, modifier, summary)` | put in a `Group`; a row whose value is one of a few choices (Settings' Transitions and Crossfade length) is `PickerRow` in ComfortSettings.kt: a `SetRow` with the choice as its value, opening a `ComfortMenu` captioned with the row's title over the row, toward the value |
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
| Equaliser sheet and Settings > Playback > Equaliser | ComfortEqualiser.kt | sheet 11; see "The equaliser" below |
| Recap page | ComfortRecap.kt | hero with the back arrow pulled into its foot (`.tools.lead`); sub-tabs pin under the bar when scrolled; `.playrow`, `.stats`, `.langs`, `.when` |
| Recap cards | ComfortRecapCards.kt | 360 by 640, always `ComfortDark` (`RecapCardColors`); top song, top artist and summary washed by their cover; ranked rows on beds in each cover's `--mini-fill`; `ListeningClock`, `languageSteps` (accent minus .13 / .26 / .38 L in OKLCH, "No vocals" `--on-3`) |
| Recap story | ComfortRecapStory.kt | full-screen window, dark, washed with the card; one deck: `ComfortProgress` with "4 of 10", Save (plain) and Share (filled); the app's messages as a `ToastHost` above the deck |

Shared helpers there: `songCount`, `totalLength`, `songsAndLength` (copy), `SystemBarsFor` (a
full-screen window's dim and bar icons), `rememberProgress` (how far a song has played).

## The equaliser (sheet 11)

ComfortEqualiser.kt draws the Equaliser section of `design/Attic Comfort.figcraft` (1.2.0) from
`ui/model/EqualiserModel.kt`; oneui/11-equaliser.html is the earlier mock the class names come from. Its pieces are
private to the file, since nothing else draws an equaliser; `EqualiserIndicator` is the one the kit
table lists. Entry points: `ComfortEqualiserSheet(state, actions)` (from the track menu's "Equaliser"
row and from the indicator; over the player the caller washes it with the cover) and
`ComfortEqualiserPage(state, actions)` (Settings > Playback > Equaliser, a `RootPage`).

| Mock class | Piece | Notes |
|---|---|---|
| `.eq-title`, `.eq-off` | `EqualiserTitleRow`, the off line | the whole row is the switch; the line under it folds in and out over 200 ms |
| `.eq-scope` (`.own`) | `EqualiserScopeHead` | default: neutral 48dp icon box; a song's or album's own setting: its cover on an `--accent-soft` bed, second line in `--on-accent-soft`; one live region for TalkBack |
| `.chips` here | `PresetChips`, `PresetChip` | single choice (`Role.RadioButton` in a `selectableGroup`), unlike `FilterChip`; "Custom" and "Save as preset" lead while no preset matches; a chosen preset of the listener's own ends in `more_vert` and opens Rename / Delete preset on a second tap or a long press; scrolls its selection into view; half strength while the equaliser is off |
| `.eq-bands`, `.eq-freqs` | `BandSliders` | a `--fill` card (radius 18; 30dp in at the start for the dB labels, 10dp at the end) holding one drawing and one `pointerInput`. The drawing has the dashed rules at +12 and -12 dB, the 0 dB line and their labels, the tracks, the engine's real response to 14 dB either way with an `--accent` area down to 0 dB, then the knobs, all at the knobs' 6.5dp to a decibel, so the line runs through every knob; off, the line is flat in `--on-3`. The bands: relative vertical drag in a band's column snapping to 1 dB, double tap to 0 dB, a tap alone does nothing; knob 20dp (26 while dragged, with the value in a `--toast` bubble); a tick on reaching or passing 0 dB; ten `BandSemantics` sliders over the columns |
| `.eq-rows`, `.sheet-row.two`, `.eq-note` | `EqualiserActionRows` | `SheetRow`s 4dp under the plot's card, with no hairline over the first; the removal row carries `summary` |
| `.eq-card` | `DefaultCard` | the editor in a `Group` on the page: chips 12dp in, the plot's card 8dp in on three sides |
| "with their own setting" rows | `OwnAlbumRow`, `OwnSongRow` | `CollectionRow` / `SongRow` that do nothing themselves; the preset and a dim close button in `trailing`; rows leave with `animateItem()` |
| dialogs | `PresetDialog` | `ComfortDialog` + `DialogField`; "Another preset has this name" under the field in `--danger` |

How it stays cheap, which is a rule for anything added to it:

- The gains being drawn live in `BandsState.gains`, a `FloatArray` in a state that only the plot's draw
  block reads. A drag redraws the plot and recomposes nothing in it. The response path is
  rebuilt inside `drawWithCache`'s draw block, and only when the gains' version or the on/off fraction
  changed.
- The model hands over a new state for every band change. The sheet and the page pass state down as
  `State` holders, values compared by content (`remember(list) { list }`) and lambdas that capture only
  those holders, so the sheet itself, the chips, the scope head, the action rows and the lists are
  skipped; what recomposes each frame of a drag is the editor's shell and one empty semantics box.
- `setBand` goes to the model once a frame at most (`snapshotFlow` + `conflate`, then a frame's wait),
  and once more when the finger lifts.
- The knob's shadow is one radial-gradient disc built with the drawing's cache and drawn under each
  knob. No blur, no layer.
- Everything that moves is a 200 ms or 100 ms tween started by an event. Nothing loops, and nothing
  runs once the sheet or the page has left the screen.

## Tablet (medium and expanded windows)

The rules are docs/design/2026-11-25-tablet/SPEC.md; the design file's Tablet page draws them. Every
branch reads `LocalWindowLayout` (ui/adaptive/AdaptiveLayout.kt) once, at the top of a screen, and a
compact window takes the phone code path unchanged.

**How the shell switches.** `ComfortShell` hands over to `ComfortWideShell` (ComfortShellWide.kt) when
`LocalWindowLayout.current.nav == Rail`: a window at least 600dp wide and 480dp tall. The same
`ShellSlots` are placed in either, so a resize keeps the NavHost and the overlays. The destination's
column runs from 8dp after the side dock (`SideDockContentStart`, 100dp) to 8dp before the end
edge, so the phone's 16dp group margins put cards 24dp from the dock and from the edge. The song
floats over the content's bottom on the dock's 36dp fade: `player == Bar` (expanded) sets
`LocalPlayerBar`, and the mini player slot then draws `ComfortPlayerBar` instead of the mini player;
`player == Floating` (medium) puts the phone's mini player in the dock's raised card, at most
`MiniPlayerMaxWidthDp` (576) wide and centred. Destinations get the player's room as
`LocalBottomPadding`. Overlays still cover the whole window; the toast sits above the player.

| Part | Call | Size |
|---|---|---|
| Side dock | `ComfortSideDock(items, selectedIndex, onSelect, modifier)` (SideDock.kt) | 80dp wide, 12 from the start edge, the status bar and the gesture area; raised, radius 30, floating shadow and edge; the mark (30) 20 from its top, 16 above the tabs; tabs 64 × 56, icon over a 12/15 label 2dp apart, 4 apart, the selected one on accent-soft with the control radius |
| Player bar | `ComfortPlayerBar(state, actions, bar)` (ComfortPlayerBar.kt) | 76dp card, radius 30, floating shadow and edge, tinted from the cover, its played part filled (mini-fill over mini-base) as its only progress; cover 52, title over artist; elapsed, previous, play or pause, next (30, 36, 30dp glyphs in 48 targets, 8 apart), length; heart, queue, lyrics. The start and end groups share the width, so the transport sits in the middle |
| Now Playing's side panel switch | `SubTabs([Up next, Lyrics])` at the top of the end half (ComfortNowPlayingWide.kt) | under the status bar in a 60dp row; Lyrics adds the open-full-screen button at the row's end |

| Screen | File | On a tablet |
|---|---|---|
| Home | ComfortHome.kt | hero without the mark (the dock has it), short (150) under 840dp tall; shortcut and folder trays six across, art 112 (expanded) or 72 (medium); Instant mixes in three cards (two on medium) filled top to bottom; Start listening in two cards on expanded; filter grids in columns ≥ 160dp |
| Library | ComfortLibrary.kt, ComfortLibraryPanes.kt | rows in cards ≥ 360dp side by side, grids in columns ≥ 160; beside the open item (`LocalListDetailSelection`) the list pane takes the bar from the start, then the chips, the open row on `selectedBed()` (`selectedTileBed()` in the grid) |
| Search | ComfortSearch.kt | field at most 720dp at the start, Browse in columns ≥ 180, songs in cards ≥ 360 |
| Album, artist, playlist, folder, genre, Favourites | ComfortCollectionPanes.kt | `collectionLayout(pane width)` from a `BoxWithConstraints` (non-compact only): two panes from 760dp (header pane 360, or 320 under 1000; cover up to 296, an artist 200), the header as a row from 600 (cover 200), else the phone screen (`LocalStackedCollection`); songs in `columnGroupItems` cards ≥ 360 filled top to bottom; an artist's and a genre's albums as a grid; no back beside the Library's list (`LocalDetailPane`) |
| Now Playing | ComfortNowPlayingWide.kt, ComfortNowPlaying.kt | sideways: the player in the start half (cover as large as the height leaves, ≤ 440; no sung line) beside Up next (the queue's own `QueuePage`) or Lyrics (`SyncedLyrics`); upright: the phone page in a centred column, cover ≤ 480, the cover and title halfway between header and deck |
| Full-screen lyrics | ComfortLyricsWide.kt, ComfortLyrics.kt | sideways: a 360dp start pane (bar, cover 264, title, seek and play in one card) and the lines in a column ≤ 640; upright: the lines in a centred column ≤ 640 |
| Queue, Settings, licences, Recap page | ComfortQueue.kt, ComfortSettings.kt, ComfortLicences.kt, ComfortRecap.kt | the list in a centred column of cards ≤ 720 (`readableColumnInset()`); the bar spans the window |
| Recap story | ComfortRecapStory.kt | a phone's proportions (411 by 914), as tall as the window allows, centred on the scrim |
| Dialogs | Overlays.kt `ComfortDialog` | centred in the window, ≤ 560dp, 400 with a field; sheets keep Material's 640 maximum |
| First run | ComfortOnboarding.kt | a centred column ≤ 560; sideways, the look step puts the cards beside the Theme list in an 840dp block |

Song rows open the track menu on a right click (`onSecondaryClick`, non-compact windows only). Every
tablet part is a ripple clickable or selectable, so it shows hover and keyboard focus and takes Tab and
Enter; the side dock comes first in the tree (dock, content, player), drawn above the content by
`zIndex` so its shadow falls on it.
