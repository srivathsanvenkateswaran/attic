# Components

What to reach for in each style, with the composable's name and file. Sizes, colours and timings
are in [tokens.md](tokens.md); how a screen is put together is in [rules.md](rules.md).

Each style draws only with its own kit. Expressive's is package `app.attic.player.ui.expressive`
and Comfort's is `app.attic.player.ui.comfort`. Several names exist in both (`SongRow`, `Cover`,
`Tile`, `PlayButton`, `SearchField`), so a file imports one package and never both. Comfort borrows
the [`Art`][x-cover] type and the [`Symbol`][x-symbols] enum from the Expressive package.

Every kit component takes plain data and lambdas. None of them reads an app singleton.

For the full argument lists, read each kit's reference: [`expressive/KIT.md`][x-kit] and
[`comfort/KIT.md`][c-kit]. To see a component on a phone, open the debug-only kit gallery
(`KitGalleryActivity`; both KIT.md files give the `adb` commands).

Contents: [buttons](#buttons) · [play controls](#play-controls) ·
[lists, rows and groups](#lists-rows-and-groups) · [covers, tiles and shelves](#covers-tiles-and-shelves) ·
[navigation bar and dock](#the-navigation-bar-expressive-and-the-dock-comfort) ·
[top bars](#top-bars) · [mini player](#mini-player) · [Now Playing](#now-playing) ·
[progress](#progress) · [switches and radios](#switches-and-radios) ·
[chips and one-of-N choices](#chips-and-one-of-n-choices) · [search field](#search-field) ·
[sheets](#sheets) · [menus](#menus) · [dialogs](#dialogs) · [messages](#snackbar-expressive-and-toast-comfort) ·
[empty, loading and permission states](#empty-loading-and-permission-states) ·
[settings rows](#settings-rows) · [Recap](#recap) · [widget](#widget) ·
[where KIT.md differs](#where-kitmd-differs-from-the-code)

## Buttons

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions;
[`Attic Comfort.figcraft`][fc] › Components / Actions

| Need | Expressive ([`Buttons.kt`][x-buttons]) | Comfort ([`Buttons.kt`][c-buttons]) |
|---|---|---|
| A labelled button | `ExpressiveButton(text, onClick, variant = ButtonVariant.Filled / Tonal / Outlined / Text, size = ButtonSize.Small / Medium)`: 40 or 56dp tall, fully round | `ComfortButton(text, onClick, style = ButtonStyle.Filled / Tonal / Plain / Text, size = ButtonSize.Small / Medium / Large)`: 36, 48 or 56dp tall, a pill |
| A destructive button | `ExpressiveButton(variant = ButtonVariant.Text, contentColor = colorScheme.error)` | `ComfortButton(danger = true)` |
| Full width | `Modifier.fillMaxWidth()` or a weight in a `ButtonGroup(fill = true)` | `block = true` |
| An icon button | `ExpressiveIconButton(symbol, contentDescription, onClick, variant, medium, width)`: a 40dp container in a 48dp target | `ComfortIconButton(symbol, contentDescription, onClick, dim, filled)`: bare, 48dp target |
| A toggle | `ExpressiveToggleButton(symbol, checked, onCheckedChange, contentDescription)`: round while off, squared off and `primary` while on | `TonalIconButton(symbol, contentDescription, onClick, checked, large)`: a superellipse, `accent` while on |
| Favourite | `HeartButton(favourite, onToggle)` | `HeartButton(favourite, onToggle, tonal, large)` |
| Buttons that react to each other | `ButtonGroup { … Modifier.groupItem(interactionSource) }` | none |
| A floating action | `ExpressiveFab(symbol, contentDescription, onClick)`, `ExpressiveExtendedFab(text, symbol, onClick, expanded)` | none; the main action is the one filled `ComfortButton` |
| An action with a menu | `SplitButton(text, onClick, onMenuClick, menuContentDescription)` | none |
| A row of tools | `FloatingToolbar { icon buttons }`: a 64dp pill. In the kit and the gallery; no screen uses it yet | `Toggles { … }` on Now Playing ([`NowPlayingParts.kt`][c-np]) |
| A collection's Play row | Screen work in [`ExpressiveCollections.kt`][x-collections]: a play button with the glyph over its label (48dp corners while idle, 28dp while this collection plays) and medium toggles beside it | `CollectionActions { CollectionPlayButton(playing, onClick, Modifier.weight(1f)); TonalIconButton(…, large = true) }` |

Both `HeartButton`s describe themselves ("Add to favourites" / "Remove from favourites").

Do:

```kotlin
// Expressive: the one filled action; it squares off while it reads Pause.
ExpressiveButton(stringResource(if (playing) R.string.pause else R.string.play), onPlay, squared = playing)

// Comfort: the one filled button on the screen.
ComfortButton(stringResource(R.string.play), onPlay, leading = Symbol.PlayArrow, leadingFilled = true)
```

Don't:

```kotlin
// Material's Button directly: it skips the kit's heights, corner morph and colours.
Button(onClick = onPlay) { Text("Play") }

// Two filled buttons side by side in Comfort. Amber marks the one main action.
ComfortButton("Play", onPlay); ComfortButton("Shuffle", onShuffle)
```

## Play controls

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions and › Components / Playback;
[`Attic Comfort.figcraft`][fc] › Components / Actions and › Components / Playback

| Need | Expressive ([`PlayControls.kt`][x-play]) | Comfort |
|---|---|---|
| Play or pause | `PlayButton(playing, onClick, size = PlaySize.Small / Large / ExtraLarge, tonal, progress = { fraction })`. Round while paused, squared off while playing. `Small` is 36dp in a 52dp box; `Large` 56dp; `ExtraLarge` 152 × 80dp paused and 128 × 80dp playing | `PlayButton(playing, onClick, size = PlaySize.Large / Medium / Small)` in [`Buttons.kt`][c-buttons]: an accent superellipse, 76, 56 or 44dp |
| Progress where there is no seek bar | Pass `progress` to the small `PlayButton` for its ring | The mini player's fill (below) |
| Previous and next | `SkipButton(symbol, contentDescription, onClick)`: a 76 × 80dp tonal pill | `SkipButton(symbol, contentDescription, onClick)` in [`NowPlayingParts.kt`][c-np]: a 64dp target, 42dp glyph |
| The three together | `Transport(playing, onPrevious, onPlayPause, onNext)` | `Transport(playing, onPrevious, onPlayPause, onNext)` |
| "This is playing" on a row or tile | `NowPlayingWave(playing)`: a 20 × 10dp wave, flat when paused | `PlayingBars(playing)` in [`Rows.kt`][c-rows]: three bars that bounce, at rest when paused |

Both `PlayButton`s describe themselves "Play" / "Pause".

## Lists, rows and groups

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Lists and cards;
[`Attic Comfort.figcraft`][fc] › Components / Lists and cards

Every list sits in a group, and each style draws a group its own way: Expressive's rows are separate
cells 2dp apart, with 20dp corners on the group's outside and 4dp inside; Comfort's is one card with
26dp corners whose rows are split by hairlines.

| Need | Expressive ([`Lists.kt`][x-lists]) | Comfort ([`Groups.kt`][c-groups], [`Rows.kt`][c-rows]) |
|---|---|---|
| A short group | `ListGroup(count) { index -> … }` | `Group { item { … }; item { … } }` |
| A long group in a `LazyColumn` | `groupItems(list, key) { index, item -> … }` | `groupItems(list, key) { index, item -> … }` |
| One cell placed by hand | `Modifier.groupCell(index, count, color)` | `GroupCell(index, count) { … }` |
| A song | `SongRow(title, subtitle, lead = RowLead.Cover(art) / Number(n) / RankedCover(rank, art) / RankedArtist(rank, art) / None, current, playing, onClick, onLongClick, trailing)` | `SongRow(title, subtitle, lead = SongLead.Cover(art) / Artist(art) / Number(n) / None, current, playing, rank, meta, onClick, onLongClick, trailing)` |
| An album, artist, playlist or folder | `CollectionRow(title, subtitle, lead = { Cover(…, size = 56.dp) }, onClick)` | `CollectionRow(title, subtitle, lead = { m -> Cover(art, m) }, onClick, size = RowSize.Small / Medium / Large, value, chevron)` |
| What ends a song row | `RowMoreButton`, `RowDragHandle`, `RowAddToggle`, `RowCount` | `SongMoreButton`, `SongDragHandle`, `SongAddButton`, `SongRadioMark` |
| The current song | `current = true`: the `secondaryContainer` fill, a heavier title, the wave over the cover or in place of the number | `current = true`: a tonal bed, a bold title, the bars over the dimmed cover. The title's colour does not change |
| Swipe to remove (the queue) | `SwipeToRemove(onRemove, resetKey) { SongRow(…) }` | `SwipeToRemove(onRemove, resetKey = …) { SongRow(…) }` |
| A row being dragged | `Modifier.liftableCell(index, count, lifted)` with `SongRow(lifted = …)` | `SongRow(lifted = true)` |
| A heading above a group | `SectionHeader(title, overline, trailing)`, or `ListLabel(text)` for a small one ([`Navigation.kt`][x-nav]) | `Label(text, small, tight, end = { … })` |
| Small print under a group | `SetNote(text)` in settings ([`Surfaces.kt`][x-surfaces]) | `Note(text)` |
| A label over a value (Credits) | `KeyValue(label, value)` in [`Surfaces.kt`][x-surfaces] | `KeyValue(label, value)` |
| Rows split by hairlines without a card | none | `Hairlined { item { … } }` (a sheet's rows) |
| A shelf or grid inside a group | Put it in a cell | `Inset { … }` |

In Comfort, rows draw their own hairline. `Group`, `Hairlined` and `groupItems` tell each row
whether one goes above it (`LocalHairlineAbove`), and `LocalComfortContainer` tells chips, the
search field and song rows whether they sit on the ground, a card, a sheet or a dialog. A hand-made
card sets it with `OnCard { … }`.

Do:

```kotlin
// Expressive
LazyColumn {
    item { ListLabel(stringResource(R.string.queue_up_next)) }
    groupItems(songs, key = { it.key }) { _, song ->
        SongRow(song.title, song.artist, RowLead.Cover(Art.of(song.mediaId)), onClick = { play(song) })
    }
}

// Comfort
LazyColumn {
    item { Label(stringResource(R.string.queue_up_next)) }
    groupItems(songs, key = { it.key }) { _, song ->
        SongRow(song.title, song.artist, lead = SongLead.Cover(Art.of(song.mediaId)), onClick = { play(song) })
    }
}
```

Don't:

```kotlin
// A bare row outside a group: it has no cell (Expressive) and no card or hairline (Comfort).
LazyColumn { items(songs) { SongRow(it.title, it.artist, RowLead.Cover(Art.of(it.mediaId))) } }

// A divider between Expressive rows. The 2dp gap is the separator.
HorizontalDivider()

// Amber text on Comfort's current row. The bed and the bold title mark it.
Text(song.title, color = Comfort.colors.accent)
```

## Covers, tiles and shelves

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Lists and cards;
[`Attic Comfort.figcraft`][fc] › Components / Lists and cards

Both styles take the same picture type: `Art.of(mediaId)` or `Art.Picture(bitmap)`
([`Cover.kt`][x-cover]). A shape says what a thing is.

| Need | Expressive ([`Cover.kt`][x-cover], [`Tiles.kt`][x-tiles]) | Comfort ([`Artwork.kt`][c-artwork], [`Tiles.kt`][c-tiles]) |
|---|---|---|
| An album, playlist or folder cover | `Cover(art, modifier, size)`: a rounded square whose radius follows its size | `Cover(art, modifier, size, dimmed)`: a superellipse, the larger curve from 100dp |
| An artist | `ArtistCover(art, modifier, size)`: the nine-lobed cookie | `ArtistCover(art, modifier, size)`: the rounder superellipse |
| A folder or playlist of several albums | `Mosaic(arts, modifier, size)`: 2 × 2 with four or more covers | `Mosaic(arts, modifier, size)` |
| Favourites | `FavouritesArt(modifier, size)`: a filled heart on `tertiaryContainer` | `FavouritesArt(modifier, size)`: the tonal icon box with a filled heart |
| No cover | `NoArt(modifier, size)`; `Cover(null)` shows the same | `NoArt(modifier, size)`: the neutral icon box with a note |
| An icon in a container | none | `IconBox(symbol, modifier, size, style = IconBoxStyle.Tonal / Neutral / Solid)` |
| A tile | `Tile(title, onClick, art = { m -> Cover(art, m) }, subtitle, width = 148.dp, artist, now, playing)` | `Tile(title, onClick, art = { m -> Cover(art, m) }, subtitle, width = 132.dp, centered, now, playing)` |
| Tiles that scroll sideways | `Shelf { item { Tile(…) } }` | `Shelf { item { Tile(…) } }` |
| Tiles in columns | `TileGrid(items, columns = 2) { Tile(…, width = null) }` | `TileGrid(count, columns, tray) { i -> Tile(…, width = null) }` |
| Home's lead | `HeroCarousel(count, leading = { FavouritesPill(…) }, caption = { HeroCaption(…) }) { index -> … }` | `Hero(title, mark = true)` then shelves and groups |
| Instant mixes | `InstantMixes(items) { InstantMixPill(title, art, onClick, onLongClick, now) }` | `Pager(pageCount) { page -> item { SongRow(…, trailing = { SongRadioMark() }) } }` |
| Search's browse tiles | `BrowseTile(title, seed, onClick) { m -> Cover(…, m) }`: takes the cover's scheme itself | `BrowseTile(title, onClick, art = { m -> … }, seed = …)`: a seed washes the card |
| A collection's header | Screen work in [`ExpressiveCollections.kt`][x-collections], inside `FromArt` | `CollectionHeader(title, art, byline, meta, person, artSize)` |
| A figure over its label | `StatTile(value, label, accent)` | `Stats(listOf(value to label, …))` in a `Group` ([`Controls.kt`][c-controls]) |
| Home's Recap card | `RecapCard(title, body, onClick) { m -> Mosaic(…, m) }` | Screen work in [`ComfortHome.kt`][c-home] |

Do:

```kotlin
// A mosaic of covers from different albums (ui/Components.kt).
Mosaic(mosaicIds(tracks).map { Art.of(it) }, size = 56.dp)
```

Don't:

```kotlin
// A circle for an artist, in either style.
Cover(art, shape = CircleShape)

// A rounded rectangle on Comfort artwork.
Cover(art, shape = RoundedCornerShape(12.dp))

// Clipping a Comfort cover yourself. Cover bakes the shape into the picture; see rules.md.
AsyncImage(model, null, Modifier.clip(ComfortShapes.Squircle))
```

## The navigation bar (Expressive) and the dock (Comfort)

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Navigation;
[`Attic Comfort.figcraft`][fc] › Components / Navigation

Both shells put Home, Search and Library at the bottom and let destinations scroll under it. A
screen never draws the bar itself; the shell does, and tells the screen how much room to leave with
`LocalBottomPadding` ([`ui/Components.kt`][components]).

| | Expressive | Comfort |
|---|---|---|
| The shell | [`ExpressiveShell(state, actions, slots)`][x-shell] | [`ComfortShell(state, actions, slots)`][c-shell] |
| The bar | `ExpressiveNavigationBar(items: List<NavItem>, selectedIndex, onSelect)` in [`Navigation.kt`][x-nav]: Material's short navigation bar on `surfaceContainer`, a pill indicator in `secondaryContainer`, the selected icon filled | `Dock(mini = { MiniPlayer(…) }) { TabBar(items, selectedIndex, onSelect) }` in [`Dock.kt`][c-dock]: one floating card with 30dp corners, the mini player as its first row and the tabs as its second |
| With nothing queued | The bar alone | `mini = null`: the dock is a capsule tab bar |
| The mini player | Floats 8dp above the bar, 12dp in from the edges | Is the dock's first row |
| With the keyboard up | The mini player rides above the keyboard; the bar stays under it | The dock stays where it is, under the keyboard |
| A mode that replaces the bar | none | `ActionBar { ActionBarItem(symbol, label, onClick, go, danger) }` in [`Controls.kt`][c-controls] (reorder, selection) |

Do:

```kotlin
LazyColumn(contentPadding = PaddingValues(bottom = LocalBottomPadding.current)) { … }
```

Don't:

```kotlin
// A fixed number for the dock. Its height changes with the mini player and the gesture inset.
LazyColumn(contentPadding = PaddingValues(bottom = 176.dp)) { … }
```

## Top bars

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Navigation;
[`Attic Comfort.figcraft`][fc] › Components / Navigation

| Need | Expressive ([`Navigation.kt`][x-nav]) | Comfort ([`Bars.kt`][c-bars], [`ComfortRoot.kt`][c-root]) |
|---|---|---|
| A tab root's title | `RootAppBar(title, scrolled = listState.scrolledFraction(), titleLeading, actions)`: a 36/44 title that shrinks to 22/28 as the list scrolls | `Hero(title, subtitle, short, mark)` as the list's first item, then `Tools(leading = { SubTabs(…) }) { icon buttons }`. `RootPage` swaps in the 56dp `Bar` once the hero has scrolled away |
| A detail screen's bar | `DetailAppBar(title, onBack, surface = AppBarSurface.Plain / Scrolled / OverArt, actions)` | `Bar(title, onBack, lift, accentTitle) { actions }`; `title = ""` at the top of a detail screen |
| Over a collection header | `AppBarSurface.OverArt`: the title is hidden and the buttons get scrims | A plain bar that lifts and shows the title once the header has scrolled under |
| An overlay's bar (queue, lyrics) | Screen work in [`ExpressiveQueue.kt`][x-queue] and [`ExpressiveLyrics.kt`][x-lyrics], inside `FromCover` | `Bar(backSymbol = Symbol.ExpandMore, backDescription = …)` |

Settings is the last action on a root bar in both styles. A tab root takes the status bar inset
itself; neither shell adds one.

## Mini player

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Navigation;
[`Attic Comfort.figcraft`][fc] › Components / Navigation

| | Expressive | Comfort |
|---|---|---|
| Composable | [`ExpressiveMiniPlayer(state, actions)`][x-shell] | [`ComfortMiniPlayer(state, actions)`][c-shell], which draws the kit's `MiniPlayer(…)` from [`Dock.kt`][c-dock] |
| Shape | A 64dp pill | A 64dp row; the dock's top corners |
| Colour | `FromArt(seed)`: the cover scheme's `surfaceContainerHigh` | `seed`: the cover's `miniBase`, with `miniFill` over the played part |
| Progress | The ring around the play control | The fill itself; there is no progress line |
| Content | A 48dp cover, title over artist, the heart, the play control | A 44dp cover, title and artist, the heart, play or pause |
| Gestures | Tap opens Now Playing; swiping the text sideways past 28% of its width skips | the same |
| Other placements | none | `MiniPlayerPlacement.Alone`, and `Top` (60dp, square corners, no cover) across a scrolled Now Playing |

Both are driven by one model, `rememberMiniPlayerModel` ([`ui/NowPlaying.kt`][now-playing]).

## Now Playing

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Playback and › Screens;
[`Attic Comfort.figcraft`][fc] › Components / Playback and › Screens

Now Playing is a screen composed from kit parts, drawn in the cover's colours.

| Part | Expressive ([`ExpressiveNowPlaying.kt`][x-np]) | Comfort ([`ComfortNowPlaying.kt`][c-npscreen], parts in [`NowPlayingParts.kt`][c-np]) |
|---|---|---|
| Colour | `FromArt(seed) { … }` around the whole screen | `Tinted(seed, wash = true) { … }` around the whole screen |
| Header | Screen work: close, "Playing from" and the context, more | `NowPlayingHeader(context, onClose, onMore, moreDescription)` |
| Cover | A pager of covers with 28dp corners; paused, it steps back to 86% and rounds to 48dp | `NowPlayingCover(art, paused)`: a large superellipse 30dp in from the edges; paused, it draws at 86% |
| Title | 32sp emphasised, narrowing before it truncates (`NarrowingText`) | `NowPlayingTitle(title, artist, onArtistClick)`: 26/32 bold |
| The line being sung | In the title block | `NowPlayingLyric(line, onClick)` |
| Seek | `SeekBar(progress, playing, onSeek, contentDescription, onScrub)` and `SeekTimes(elapsed, total)` | `Deck { SeekBar(progress = { f }, elapsed, total, onSeek, onScrub = …) … }` |
| Transport | `Transport(…)`: skip pills and the wide play control | `Transport(…)` inside the `Deck` |
| Shuffle, repeat, favourite, queue | A row of `ExpressiveToggleButton`s and icon buttons under the transport | `Toggles { TonalIconButton(…, large = true); HeartButton(…, tonal = true) }` inside the `Deck` |
| Lyrics card | `InfoCard(title, symbol) { LyricLine(text, LyricState.…) }` in [`Surfaces.kt`][x-surfaces] | `LyricsCard { LyricsLine(text, current, plain); LyricsMessage(text) }` |
| Credits | `InfoCard { KeyValue(label, value) }` | `Group { item { KeyValue(label, value) } }` |

The queue and full-screen lyrics are overlays of their own: [`ExpressiveQueue.kt`][x-queue] and
[`ExpressiveLyrics.kt`][x-lyrics]; [`ComfortQueue.kt`][c-queue] and [`ComfortLyrics.kt`][c-lyrics].
In Comfort both are `Tinted(wash = true)`. Dragging Now Playing down 160dp closes it in both styles.

## Progress

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions and › Components / Playback;
[`Attic Comfort.figcraft`][fc] › Components / Actions and › Components / Playback

Expressive's progress is a wave while something is running. Comfort's is a thick capsule that
fills; progress is never a thin line there.

| Need | Expressive ([`PlayControls.kt`][x-play]) | Comfort |
|---|---|---|
| The seek bar | `SeekBar(…)`: a wavy active track while playing, flat when paused or dragged, a 4 × 28dp handle, 44dp tall | `SeekBar(…)` in [`NowPlayingParts.kt`][c-np]: a 48dp capsule whose fill is the played part, with the elapsed time inside the fill. The fill is never narrower than 64dp |
| A long job (song analysis, a lyrics download) | `WavyLinearProgress({ fraction }, running)` | `ComfortProgress(progress = { f }, thick)` in [`Controls.kt`][c-controls]: 10dp, or 16dp when `thick` |
| Plain determinate progress | `LinearProgress { fraction }` | `ComfortProgress(…)` |
| A short wait | `LoadingIndicator(contained)`: one shape morphing while it turns | `Spinner(small)`: a 36dp ring, 22dp when `small` |
| A circular indicator | `CircularProgress(progress = null or { fraction }, small)` | `Spinner(…)` |
| Recap's story | `StoryProgress(progress = { fraction }, stops, running)` | `ComfortProgress(…)` with "4 of 10" beside it |

Every progress composable takes its fraction as a lambda, so the value is read when the bar is
drawn and a moving bar does not recompose its screen.

Do:

```kotlin
WavyLinearProgress({ state.analysed / state.total.toFloat() }, running = state.running)   // Expressive
ComfortProgress(progress = { state.analysed / state.total.toFloat() })                    // Comfort
```

Don't:

```kotlin
// Material's indicator in Comfort: a thin line the kit does not use.
LinearProgressIndicator(progress = { fraction })
```

## Switches and radios

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions;
[`Attic Comfort.figcraft`][fc] › Components / Actions

| Need | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Controls.kt`][c-controls]) |
|---|---|---|
| A switch | `ExpressiveSwitch(checked, onCheckedChange)`: Material's, with a tick in the thumb when on and a cross when off | `ComfortSwitch(checked, onCheckedChange)`: drawn here, a 52 × 30dp track with a 24dp knob |
| A radio | `ExpressiveRadio(selected, onClick)`: Material's | `ComfortRadio(selected, onClick)`: drawn here, a 24dp ring with a 12dp dot |
| In a settings row | `SetSwitchRow(title, checked, onCheckedChange, summary)`, `SetRadioRow(title, selected, onSelect, summary)` | `SetSwitchRow(title, checked, onCheckedChange, summary = …)`, `SetRadioRow(title, selected, onClick, summary = …)` |

In a row, the whole row is the control: pass `null` for the switch's or radio's own handler, as the
`Set…Row` helpers do, so TalkBack reads one item.

## Chips and one-of-N choices

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions and › Components / Navigation;
[`Attic Comfort.figcraft`][fc] › Components / Actions and › Components / Navigation

A chip is a filter that can be switched off again. A choice that always has exactly one option
selected is a different component.

| Need | Expressive ([`Navigation.kt`][x-nav]) | Comfort ([`Bars.kt`][c-bars]) |
|---|---|---|
| Filters that can be cleared (Library) | `ChipRow { ExpressiveFilterChip(label, selected, onClick) }`: 32dp, 8dp corners, tonal with a tick while on | `ChipRow { FilterChip(label, selected, onClick) }`: a 36dp pill, tonal with a tick while on |
| One of N (Home's filter, Recap's period, the theme) | `ConnectedButtonGroup(options, selectedIndex, onSelect)`: segments 40dp tall and 2dp apart; the selected one widens, goes fully round and fills with `primary` | `SubTabs(options, selectedIndex, onSelect, lift)`: the selected one is bold with a short accent underline |

On a group cell, pass `unselectedColor = surfaceContainerHighest` to `ConnectedButtonGroup`. A
Comfort chip or search field on a card, sheet or dialog takes `fill` by itself.

## Search field

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Navigation;
[`Attic Comfort.figcraft`][fc] › Components / Actions

| | Expressive ([`Navigation.kt`][x-nav]) | Comfort ([`Bars.kt`][c-bars]) |
|---|---|---|
| The field | `SearchField(query, onQueryChange, placeholder, onSearch, onBack)`: a 56dp pill on `surfaceContainerHigh` | `SearchField(query, onQueryChange, placeholder, onSearch, focusRequester)`: a 50dp capsule in the card colour |
| The docked search view's bar | Pass `onBack`: a back button replaces the glyph | none |
| Something that opens search | none | `SearchFieldButton(placeholder, onClick)` |

Both show a clear button once there is text, described by `search_clear_cd`. The placeholder is
"Search your music".

## Sheets

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Overlays;
[`Attic Comfort.figcraft`][fc] › Components / Overlays

Both are Material's `ModalBottomSheet` in the kit's colours and shape, opened fully expanded.

| Need | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Overlays.kt`][c-overlays]) |
|---|---|---|
| The sheet | `ExpressiveSheet(onDismiss, bottomPadding) { … }`: `surfaceContainerLow`, 28dp top corners, a 32 × 4dp handle | `ComfortSheet(onDismiss) { … }`: `raised`, 30dp top corners, a 40 × 4dp grab handle |
| A header about one song | `SheetHeader(title, subtitle) { m -> Cover(…, m) }` | `SheetHeader(title, subtitle, art = { m -> Cover(art, m) })` |
| A title | `SheetTitle(text)` | `SheetTitle(text, subtitle = …)` |
| A row | `SheetRow(symbol, label, onClick, selected)`, 56dp | `Hairlined { item { SheetRow(symbol, label, onClick, value, selected, danger, end) } }`, 52dp |
| A rule under the header | `SheetDivider()` | none; the hairlines do it |
| A group inside | `ListGroup`; its cells take `surfaceContainerHigh` by themselves | `SongRow`s, which indent to the sheet's 24dp margin by themselves |

The sheets the app opens are built from these in [`ExpressiveSheets.kt`][x-sheets] and
[`ComfortSheets.kt`][c-sheets] (the track menu with its sleep timer choices, the playlist picker,
the language picker, Add songs), and chosen per style by the entry points in [`ui/Sheets.kt`][sheets]: `TrackMenuSheet`,
`PlaylistPickerSheet`, `AddSongsSheet`. A new sheet follows the same split.

## Menus

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Overlays;
[`Attic Comfort.figcraft`][fc] › Components / Overlays

Both are Material's `DropdownMenu`.

| Need | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Overlays.kt`][c-overlays]) |
|---|---|---|
| The menu | `ExpressiveMenu(expanded, onDismiss) { … }`: `surfaceContainer`, 16dp corners | `ComfortMenu(expanded, onDismiss, onSheet) { … }`: `raised`, 22dp corners, the `edge` ring |
| An item | `ExpressiveMenuItem(symbol, label, onClick, danger)` | `MenuRow(symbol, label, onClick, selected, danger, check)` |
| A caption | none | `MenuCaption(text)` |
| The current choice in a list | `symbol = Symbol.Check` on the current item and `null` on the others; a null symbol keeps its space (Library's sort menu) | `selected = true, check = true`: accent ink, semibold, and a trailing tick (the language and sleep timer menus) |

In Comfort, anchor the menu with a `Box` around its trigger.

## Dialogs

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Overlays;
[`Attic Comfort.figcraft`][fc] › Components / Overlays

| | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Overlays.kt`][c-overlays]) |
|---|---|---|
| The dialog | `ExpressiveDialog(title, onDismiss, confirmButton, dismissButton, body) { … }`: Material's `AlertDialog`, centred, on `surfaceContainerHigh` with 28dp corners | `ComfortDialog(title, onDismiss, confirmText, onConfirm, dismissText, body, confirmEnabled, danger) { … }`: drawn here, floating at the bottom, 12dp in from the edges, riding above the keyboard |
| Buttons | `ExpressiveButton(variant = ButtonVariant.Text)` | Two text buttons split by a thin divider; `dismissText = null` draws the confirm button alone |
| A text field | `DialogTextField(value, onValueChange, label)`: outlined | `DialogField(value, onValueChange, label, placeholder)`: a label over the text, underlined in the accent |
| A destructive confirm | `contentColor = colorScheme.error` on the confirm button | `danger = true` |

The app's dialogs are `PlaylistNameDialog` and `DeletePlaylistDialog` in [`ui/Sheets.kt`][sheets].
A dialog that holds what the user has typed is opened from the shared screen function, outside the
style's drawing, so the text survives a style switch ([`LibraryScreen.kt`][library-screen]).

## Snackbar (Expressive) and toast (Comfort)

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Overlays;
[`Attic Comfort.figcraft`][fc] › Components / Overlays

Both shells own one `SnackbarHostState` and place the message; a screen asks for a message through
its model's actions and never hosts one itself.

| | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Overlays.kt`][c-overlays]) |
|---|---|---|
| Composable | `SnackbarHost(state) { ExpressiveSnackbar(it) }` | `ToastHost(state, modifier)` |
| Looks like | `inverseSurface`, 8dp corners, text only | One line in a capsule on `toast`, with the small shadow; no action |
| Where | 16dp above the dock; low while an overlay is open | Just above the dock; low while an overlay is open |
| Over the player | Drawn inside `FromCover`, so it takes the cover scheme's inverse roles | The kit's `toast` colours |
| For previews | `StaticSnackbar(text)` | `Toast(text)` |

## Empty, loading and permission states

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / States;
[`Attic Comfort.figcraft`][fc] › Components / States

| Need | Expressive | Comfort |
|---|---|---|
| Nothing to show | `EmptyState(title, body, symbol or art, action, clover)` in [`Surfaces.kt`][x-surfaces]: a 112dp rounded square on `secondaryContainer`, or the 128dp clover for the empty library | `EmptyState(title, body, symbol, art, fill, action)` in [`Controls.kt`][c-controls]: a 76dp neutral icon box, a title and one sentence |
| A title alone ("Nothing up next") | `body = null` | `body = null` |
| The first library scan | [`ExpressiveLoading()`][x-states]: the loading indicator on the surface | [`ComfortLoading()`][c-states]: a spinner on the ground |
| Audio access | [`ExpressivePermission(state, actions)`][x-states] | [`ComfortPermission(state, actions)`][c-states] |
| The first run | [`ExpressiveOnboarding.kt`][x-onboarding] | [`ComfortOnboarding.kt`][c-onboarding] |
| A missing album, artist or playlist | In [`ExpressiveCollections.kt`][x-collections] | In [`ComfortCollections.kt`][c-collections] |

Both loading screens show nothing but the ground for their first 400 ms
([`rememberLoadingIndicatorShown`][components]), so a normal start never flashes an indicator.
[rules.md](rules.md#performance) has the measurement behind it.

Do:

```kotlin
if (rememberLoadingIndicatorShown()) LoadingIndicator()   // Expressive
if (rememberLoadingIndicatorShown()) Spinner()            // Comfort
```

## Settings rows

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Lists and cards;
[`Attic Comfort.figcraft`][fc] › Components / Lists and cards

| Need | Expressive ([`Surfaces.kt`][x-surfaces]) | Comfort ([`Controls.kt`][c-controls]) |
|---|---|---|
| A section heading | `SettingsSection(title)` | `Label(text)` |
| A row | `SetRow(title, summary, leading, trailing)` in a `ListGroup` cell, 64dp minimum | `SetRow(title, summary, value, onClick, leading, trailing)` in a `Group`, 60dp minimum |
| A paragraph, progress or a button in the group | `SetNote(text) { …; SetNoteStatus(text) }` | `SetBlock(afterRow) { SetBlockText(…); ComfortProgress(…, SetBlockProgressPadding); SetBlockSmall(…) }` |

## Recap

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Recap;
[`Attic Comfort.figcraft`][fc] › Components / Recap

Recap's page, story and cards are screen work built from the kits:
[`ExpressiveRecap.kt`][x-recap], `ExpressiveRecapStory.kt` and `ExpressiveRecapCards.kt`;
[`ComfortRecap.kt`][c-recap], `ComfortRecapStory.kt` and `ComfortRecapCards.kt`. A card is a fixed
360 × 640 drawing at font scale 1, so the story, its thumbnails and the saved 1080 × 1920 image
match. Expressive cards use the dark cover scheme in both themes. Comfort cards are always
`ComfortDark`, and the top song, top artist and summary cards are washed with their cover.

## Widget

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / System and › System surfaces;
[`Attic Comfort.figcraft`][fc] › Components / System and › System surfaces

The home-screen widget is not Compose. [`PlayerWidget`][widget] is an `AppWidgetProvider` that
fills `RemoteViews` layouts, a small, a wide and a large one for each style:

| Size | Shows | Expressive layout | Comfort layout |
|---|---|---|---|
| Small (from 110 × 110dp) | The cover and play | `widget_expressive_small.xml` | `widget_comfort_small.xml` |
| Wide (from 180 × 50dp) | One row: cover, title, controls | `widget_expressive_wide.xml` | `widget_comfort_wide.xml` |
| Large (from 250 × 180dp; 250 × 197dp in Comfort) | The song, controls and five album shortcuts | `widget_expressive_large.xml` | `widget_comfort_large.xml` |

The launcher picks the largest layout that fits. Views are styled in
[`widget_styles.xml`][widget-styles] and coloured from the cover by [`WidgetColors`][widget-colors]
with each style's own rule; [tokens.md](tokens.md#widget-and-window-colours) has the values.
Changing the style in Settings redraws every widget.

Rules that are particular to the widget:

- Comfort's superellipse is cut into the cover bitmap, because a `RemoteViews` image cannot be
  clipped to a path. Expressive uses the views' rounded outlines.
- Hand `RemoteViews` an immutable bitmap. A mutable one is copied into shared memory for every
  layout it is set on, at every redraw ([`PlayerWidget.kt`][widget], the note on its cut covers).
- Read the saved song off the main thread; see [rules.md](rules.md#performance).

## Where KIT.md differs from the code

FigCraft: [`Attic Expressive.figcraft`][fx] › Components / Actions, whose notes agree with the code
on the toolbar and the segments.

- `expressive/KIT.md` calls `FloatingToolbar` "Now Playing's tools row". No screen calls it; Now
  Playing draws a row of toggle and icon buttons.
- `expressive/KIT.md` says connected segments have 48dp touch targets. A segment's target is its own
  40dp height ([`ConnectedButtonGroup`][x-nav]).
- `expressive/KIT.md`'s `FromArt` example wraps `MiniPlayerPill(...)`. No composable has that name;
  the mini player is [`ExpressiveMiniPlayer`][x-shell].

Differences in values are listed in
[tokens.md](tokens.md#where-kitmd-the-mocks-and-figcraft-differ-from-the-code).

[fx]: ../../design/Attic%20Expressive.figcraft
[fc]: ../../design/Attic%20Comfort.figcraft
[x-kit]: ../../app/src/main/java/app/attic/player/ui/expressive/KIT.md
[c-kit]: ../../app/src/main/java/app/attic/player/ui/comfort/KIT.md
[x-buttons]: ../../app/src/main/java/app/attic/player/ui/expressive/Buttons.kt
[x-play]: ../../app/src/main/java/app/attic/player/ui/expressive/PlayControls.kt
[x-lists]: ../../app/src/main/java/app/attic/player/ui/expressive/Lists.kt
[x-nav]: ../../app/src/main/java/app/attic/player/ui/expressive/Navigation.kt
[x-surfaces]: ../../app/src/main/java/app/attic/player/ui/expressive/Surfaces.kt
[x-cover]: ../../app/src/main/java/app/attic/player/ui/expressive/Cover.kt
[x-symbols]: ../../app/src/main/java/app/attic/player/ui/expressive/Symbols.kt
[x-tiles]: ../../app/src/main/java/app/attic/player/ui/expressive/Tiles.kt
[x-shell]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveShell.kt
[x-np]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveNowPlaying.kt
[x-queue]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveQueue.kt
[x-lyrics]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveLyrics.kt
[x-sheets]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveSheets.kt
[x-states]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveStates.kt
[x-onboarding]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveOnboarding.kt
[x-collections]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveCollections.kt
[x-recap]: ../../app/src/main/java/app/attic/player/ui/expressive/ExpressiveRecap.kt
[c-buttons]: ../../app/src/main/java/app/attic/player/ui/comfort/Buttons.kt
[c-bars]: ../../app/src/main/java/app/attic/player/ui/comfort/Bars.kt
[c-controls]: ../../app/src/main/java/app/attic/player/ui/comfort/Controls.kt
[c-rows]: ../../app/src/main/java/app/attic/player/ui/comfort/Rows.kt
[c-groups]: ../../app/src/main/java/app/attic/player/ui/comfort/Groups.kt
[c-tiles]: ../../app/src/main/java/app/attic/player/ui/comfort/Tiles.kt
[c-artwork]: ../../app/src/main/java/app/attic/player/ui/comfort/Artwork.kt
[c-dock]: ../../app/src/main/java/app/attic/player/ui/comfort/Dock.kt
[c-np]: ../../app/src/main/java/app/attic/player/ui/comfort/NowPlayingParts.kt
[c-npscreen]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortNowPlaying.kt
[c-overlays]: ../../app/src/main/java/app/attic/player/ui/comfort/Overlays.kt
[c-shell]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortShell.kt
[c-root]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortRoot.kt
[c-home]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortHome.kt
[c-queue]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortQueue.kt
[c-lyrics]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortLyrics.kt
[c-sheets]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortSheets.kt
[c-states]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortStates.kt
[c-onboarding]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortOnboarding.kt
[c-collections]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortCollections.kt
[c-recap]: ../../app/src/main/java/app/attic/player/ui/comfort/ComfortRecap.kt
[components]: ../../app/src/main/java/app/attic/player/ui/Components.kt
[sheets]: ../../app/src/main/java/app/attic/player/ui/Sheets.kt
[now-playing]: ../../app/src/main/java/app/attic/player/ui/NowPlaying.kt
[library-screen]: ../../app/src/main/java/app/attic/player/ui/LibraryScreen.kt
[widget]: ../../app/src/main/java/app/attic/player/widget/PlayerWidget.kt
[widget-colors]: ../../app/src/main/java/app/attic/player/widget/WidgetColors.kt
[widget-styles]: ../../app/src/main/res/values/widget_styles.xml
