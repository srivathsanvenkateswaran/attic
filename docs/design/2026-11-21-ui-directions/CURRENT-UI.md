# Attic: the current UI

Written 2026-11-21 from the working tree on branch `ui-redesign`, including the uncommitted Wrapped, radio and analysis work. This describes what exists. It proposes nothing.

**Conventions**

- Kotlin references are relative to `app/src/main/java/app/attic/player/`, written `ui/HomeScreen.kt:121`. Resource references are relative to `app/src/main/res/`.
- Strings in "double quotes" are copied from the code. `{braces}` mark a value filled in at runtime.
- Colour role names (`surfaceContainer`, `onSurfaceVariant`, `primary`) are the Material 3 roles defined in `ui/Theme.kt`. Section 3 gives their hex values. Type style names (`titleMedium`) map to the sp sizes in the same section.
- "Tint" always means the colour extracted from a cover. Section 3.6 explains how it is computed.
- The app is portrait-only (`AndroidManifest.xml:31`), draws edge to edge under transparent system bars (`MainActivity.kt:29-33`), and targets phones. There is no tablet or landscape layout.

**Screenshots and where they disagree with the code**

`screenshots/*.png` and `docs/screenshots/*.jpg` were taken on 6 November, before the uncommitted work. `sheet1.png`, `sheet2.png` and `sheet3.png` are contact sheets of other screenshots, not pictures of bottom sheets. Differences, with the code as the authority:

| Screenshot | Shows | Code today |
|---|---|---|
| `home.png`, `docs/screenshots/home.jpg`, `light_home.jpg` | No Wrapped card, no "Speed dial" grid | Both exist on Home (`ui/HomeScreen.kt:130-153`) |
| `light_mode_emulator.png` (Settings panel) | Sections Appearance, Lyrics, About | A "Song radio" section sits between Lyrics and About (`ui/SettingsScreen.kt:118-160`) |
| `light_album.jpg`, `light_mode_emulator.png` (album panel) | Control row has only a small green badge on the left; back arrow has no round scrim | Control row has a cover thumbnail, an add-to-playlist button and a 24dp badge (`ui/CollectionScreens.kt:203-212`); back arrow sits on a black 35% circle until the bar collapses (`ui/CollectionScreens.kt:91`). The dark `album.png` matches the code. |
| `library.png` | First visible row is an artist | With no filter the list starts with Liked Songs, then user playlists, then folders, then artists (`ui/LibraryScreen.kt:121`). The screenshot may simply be scrolled. |

No screenshot exists for: Search in dark mode, Queue, any bottom sheet, any dialog, Playlist, Folder, Liked Songs, Library grid view, the Downloaded filter, Settings in dark mode, Wrapped, the Wrapped story and its cards, the permission screen, or the small and wide widget sizes. Those are described from code alone.

---

## 1. App map

```
Launch
├─ System splash: #121212 with the green Attic mark (values/themes.xml:12-16)
├─ First run .................... once per install: Welcome, Pick your look, Access, Your library (2.18)
├─ Permission screen ............ after the first run, shown while audio access is missing
├─ Loading ...................... centred spinner until the library scan finishes
└─ Main shell (ui/AppRoot.kt:169)
   │
   ├─ Tab bar, always visible in the shell:  Home | Search | Your Library
   ├─ Mini player, floats above the tab bar whenever the queue has a current song
   │
   ├─ Destinations (drawn under the mini player and tab bar, which stay visible)
   │  ├─ home ................... tab root, start destination
   │  ├─ search ................. tab root
   │  ├─ library ................ tab root
   │  ├─ album/{key}
   │  ├─ artist/{key}
   │  ├─ folder/{name} .......... read-only playlist made from a music folder
   │  ├─ genre/{key} ............ songs of one genre, from tags and analysis (added 2026-11-23)
   │  ├─ mood/{id} .............. one mood's mix for the day (added 2026-11-25, section 2.9b)
   │  ├─ interests .............. Moods and interests, full screen over the shell (added 2026-11-25)
   │  ├─ playlist/{id} .......... user playlist
   │  ├─ liked .................. Liked Songs
   │  ├─ settings
   │  └─ wrapped
   │     └─ Wrapped story ....... full-screen dialog over everything
   │
   ├─ Full-screen overlays (cover the destinations, mini player and tab bar)
   │  └─ Now Playing ............ opened by tapping the mini player
   │     ├─ Queue ............... opened by the queue icon in Now Playing
   │     └─ Lyrics .............. opened by "Show lyrics" in the lyrics card
   │
   ├─ Bottom sheets (modal windows, above everything else)
   │  ├─ Track menu
   │  │  ├─ → Playlist picker ("Add to playlist")
   │  │  └─ → Language picker ("Song language")
   │  ├─ Playlist picker ........ also opened directly for a whole collection
   │  └─ Add songs .............. from a user playlist
   │
   ├─ Dialogs and menus
   │  ├─ Playlist name dialog ... three titles: create, new, rename
   │  ├─ Delete playlist dialog
   │  └─ Playlist overflow dropdown: Rename / Delete playlist
   │
   └─ Snackbar .................. one message at a time, above the overlays, below sheets

Outside the app
├─ Home-screen widget (three sizes)
├─ Media notification and lock-screen player (drawn by Android)
├─ "Analysing your songs" progress notification
├─ System share sheet (Wrapped cards)
└─ System app-permission settings (from the first run's Access step and the permission screen)
```

### 1.1 Entry points

| Destination | Reached from |
|---|---|
| Album | Home shortcut tile; Home "Albums" shelf; Home "More from" shelf; Search browse tile; Search result; Library row or tile; Artist "Popular releases"; track menu "Go to album"; widget album tile |
| Artist | Home "Your top artists"; Home "More from" header; Search result; Library row or tile; artist line on an album; artist name in Now Playing; "About the artist" card; track menu "Go to artist"; Wrapped "Top artists" row |
| Folder | Home shortcut tile; Home "Your folders"; Search browse tile; Search result; Library row or tile |
| Playlist | Library row or tile; automatically after creating one with the Library "+" |
| Liked Songs | Home shortcut tile (always the first tile); Library row or tile |
| Settings | The round Attic-mark button at the top left of Home, Search and Library |
| Wrapped | The Wrapped card on Home. There is no other way in. |
| Search tab | Tab bar; search icon in the Library header |
| Now Playing | Tap anywhere on the mini player except its two buttons |
| Queue | Queue icon at the bottom right of Now Playing |
| Lyrics (full screen) | "Show lyrics" pill in the lyrics card |
| Track menu | Kebab on any track row; long-press on any track row; long-press on a Speed dial tile; long-press on a Wrapped "Top songs" row; kebab in the Now Playing header; long-press on a queue row |
| Sleep timer | "Sleep timer" in the track menu, only when it was opened from the kebab in the Now Playing header (added 2026-11-23, section 2.13) |
| Equaliser sheet | "Equaliser" in the track menu opened from the Now Playing kebab, or the equaliser pill in the Now Playing header (added 2026-11-25, section 2.13a) |
| Equaliser page | Settings > Playback > "Equaliser" (added 2026-11-25, section 2.15) |
| Now Playing, on a file from another app | Opening an audio file with Attic from a file manager or downloads list (added 2026-11-23, section 2.21) |
| Smart shuffle | "Smart shuffle" in the more menu of a playlist or Favourites (added 2026-11-23, section 2.9) |
| Android Auto | The car's app launcher, or "play X on Attic" said to the assistant (added 2026-11-23, section 2.22) |

### 1.2 Navigation behaviour

- **Tabs** (`ui/AppRoot.kt:240-250`). Selecting another tab pops to Home, saving and restoring each tab's state. Tapping the tab you are already on pops back to that tab's root. Album, artist and the other detail screens are pushed onto one shared stack, and the tab highlight stays on whichever tab root was visited last (in `album.png` Home is highlighted, in `artist.png` Search is).
- **Opening any destination closes every overlay** (`ui/AppRoot.kt:90-93`). Tapping the artist name in Now Playing, or "Go to album" from a menu opened over the Queue, closes Queue, Lyrics and Now Playing, then navigates.
- **System back** (`ui/AppRoot.kt:282-288`) closes Lyrics, then Queue, then Now Playing, then pops the navigation stack. Sheets, dialogs and the Wrapped story close on back.
- **Swipe down** on Now Playing closes it (section 2.10).
- **Transitions** are in section 3.8.
- A widget album tile opens the app straight on that album (`ui/AppRoot.kt:208-214`).
- The last queue is restored on launch without playing (`playback/PlaybackService.kt:228-237`), so the mini player is normally present, paused, as soon as the app opens.

---

## 2. Screens and overlays

### 2.0 Shared building blocks

Defined once here and referred to by name below.

**Tap feedback (`pressable`, `ui/Motion.kt:31-54`).** Nearly every tappable element scales down while pressed and springs back. There is no ripple. Exceptions that use the stock Material ripple: `IconButton`s, `TextButton`s, `Button`s, and the rows inside bottom sheets (`ui/Sheets.kt:164`).

**Track row (`ui/Components.kt:136-196`).** The standard song row.
- Padding: start 16, end 4, top and bottom 7.
- Cover 50dp, radius 4, then a 12dp gap. Album screens omit the cover.
- Line 1: title, `titleMedium`, one line, ellipsis. On the current song the title is `primary` green and is preceded by the playing bars and a 6dp gap.
- Line 2 (2dp below): the 13dp downloaded badge, 5dp gap, artist in `bodyMedium` `onSurfaceVariant`, one line.
- Trailing: kebab (`MoreVert`, `onSurfaceVariant`, description "More options for {title}"). Some screens replace it with a drag handle or an add/check button.
- Tap plays. Long-press opens the track menu. Press scale 0.985.
- Rows show no duration and no track number.

**Swipe to queue (added 2026-11-23, `ui/SwipeToQueue.kt`).** In both styles a song row in a list can be dragged towards the end of the line (right, in a left-to-right language). The row follows the finger to 30% of its width, then resists (35% of the finger's movement) and stops at half its width. Letting go past 30%, or flicking at 1,000dp a second or faster past 12%, adds the song to the end of the queue, shows "Added to queue" and gives the confirm haptic; the row springs back either way. Behind it: Expressive shows `surfaceContainerHighest` with the queue glyph and "Add to queue", turning `primaryContainer` once letting go would queue, and settles on its fast spatial spring; Comfort shows `--fill` with the glyph in brass, turning to the amber bed, and settles without overshoot. Rows that have it: album, artist, folder, genre, Favourites and playlist songs (not in reorder mode), Library's songs, Search results and Home's "Start listening". Rows that don't: the queue (swipe removes there), Instant mixes, Recap and reorder mode.

**Haptics (added 2026-11-23, `ui/Haptics.kt`).** Light taps from the platform's own feedback constants, through the screen's view with no flags, so the phone's touch-feedback setting turns them off. Like and play: toggle-on. Unlike and pause: toggle-off. Picking up a row to reorder (queue, playlist): drag-start; letting go: gesture-end. A swipe that queues a song and setting a sleep timer: confirm. Changing the style in Settings or on the first run: segment tick. Android 13 has no toggle, drag-start or segment constant, so it gets clock-tick and long-press instead. No custom vibration patterns.

**Playing bars (`ui/Components.kt:91-114`).** A 14dp canvas with three rounded bars, each a quarter of the width, in `primary`. While playing they bounce between 25% and 100% height on 420, 560 and 340 ms cycles. While paused they sit still at 30%.

**Downloaded badge (`ui/Components.kt:117-133`).** A `primary` green circle holding a down arrow in the background colour. 13dp in rows and the mini player, 24dp in collection control rows. It is decoration: every song is a local file, so every row carries it and it never changes state. Nothing happens on tap.

**Like button (`ui/Motion.kt:57-80`).** Not liked: `AddCircleOutline` in the current content colour, description "Add to liked songs". Liked: `CheckCircle` in `primary`, description "Remove from liked songs". Default icon size 26dp. On change it snaps to 60% scale and springs back. The track menu shows the same state with heart icons instead (section 2.13).

**Play circle (`ui/Components.kt:199-231`).** A filled circle with a play or pause glyph at 58% of its diameter. Default 56dp, `primary` fill, `onPrimary` glyph (black in dark mode, white in light). The glyph swaps with a scale-and-fade (in 140 ms from 70%, out 100 ms). Press scale 0.92. Descriptions "Play" / "Pause".

**Cover art (`ui/Art.kt:144-172`).** The embedded cover, cropped to fill. Behind it is a `surfaceContainerHighest` box with a `MusicNote` icon in `onSurfaceVariant`, which shows when the file has no art. Covers crossfade in over 120 ms (`AtticApp.kt:63`). Shapes: `CoverShape` is 4dp radius, `BigCoverShape` is 8dp (`ui/Components.kt:58-59`).

**Mosaic cover (`ui/Components.kt:255-275`).** For folders and playlists. If the songs come from four or more different albums it is a 2×2 grid of four covers with no gaps, clipped to the outer shape. With fewer than four it is one cover, or the note placeholder when empty.

**Liked tile (`ui/HomeScreen.kt:381-389`).** A linear gradient from `#4A17E0` (violet) to `#C4EFD9` (pale mint) with a white `Favorite` heart at 46% of the tile.

**Result row (`ui/SearchScreen.kt:231-259`).** Padding 16 horizontal, 7 vertical. A 56dp cover (4dp radius, or a circle for artists), 12dp gap, title in `titleMedium`, subtitle in `bodyMedium` `onSurfaceVariant`, both one line. It has no trailing control, and a tap opens the item.

**Shelf card (`ui/HomeScreen.kt:405-431`).** 160dp wide. Square cover with 8dp radius, 8dp gap, title `titleSmall` one line (green when highlighted), subtitle `bodySmall` `onSurfaceVariant` up to two lines. Shelves scroll horizontally with 16dp side padding and 14dp between cards.

**Pill chip (`ui/HomeScreen.kt:459-474`).** Fully rounded, padding 14 horizontal and 8 vertical, label in `bodyMedium`. Selected: `primary` fill, `onPrimary` text. Unselected: `surfaceContainerHighest` fill, `onSurface` text. Press scale 0.94.

**Settings button (`ui/HomeScreen.kt:443-457`).** A 36dp `surfaceContainerHighest` circle holding the green Attic mark at 18dp. Description "Settings". It occupies the place where a streaming app shows a profile picture. Press scale 0.9.

**Section title (`ui/Components.kt:245-252`).** `titleLarge`, padding start and end 16, top 26, bottom 12.

**Empty message (`ui/Components.kt:277-297`).** A centred column, padding 32 horizontal and 48 vertical, 8dp between lines: title in `titleLarge`, body in `bodyMedium` `onSurfaceVariant`, optional action below.

**Back button (`ui/Components.kt:233-243`).** `ArrowBack` in `onSurface`, description "Back", with an optional round scrim behind it.

**Bottom padding.** Every scrolling screen pads its end by the tab bar (58dp) plus the mini player (64dp when present) plus the system navigation inset plus 12dp (`ui/AppRoot.kt:226-229`), so the last row clears the floating bars. Content scrolls underneath both bars.

---

### 2.1 Tab bar

Purpose: switch between the three root screens. `ui/AppRoot.kt:355-387`.

- Height 58dp above the system navigation inset. Three equal columns.
- Each tab is an icon at 25dp, a 3dp gap, and a label in `labelSmall`.

| Label | Unselected icon | Selected icon |
|---|---|---|
| "Home" | `Outlined.Home` | `Rounded.Home` |
| "Search" | `Rounded.Search` | `Rounded.Search` (same) |
| "Your Library" | `Outlined.LibraryBooks` | `Rounded.LibraryBooks` |

- Selected icon and label are `onSurface`. Unselected are `onSurfaceVariant`. The colour animates over 140 ms. The accent green is not used here.
- The bar has no solid background. It is a vertical gradient of the background colour: 72% opaque at the top, 97% at 55% of the height, 100% at the bottom (`ui/AppRoot.kt:361`). Content shows through and fades out behind it.
- Press scale 0.9.

### 2.2 Mini player

Purpose: show and control the current song from any screen, and open Now Playing. `ui/NowPlaying.kt:116-206`.

- Present whenever the queue has a current song. Absent only when nothing has ever been queued or the queue is empty. Slides up and fades in over 240 ms, out over 180 ms.
- A floating rounded bar: 8dp from each screen edge, radius 6dp, sitting directly above the tab bar with a 4dp gap below it.
- **Background is the current cover's tint**, animated over 400 ms when the song changes. Text and icons are white in both light and dark themes, because the tint is always dark.
- Inner padding: start 8, top 8, bottom 6, end 2.
- Left to right:
  - Cover 40dp, radius 4.
  - 10dp gap.
  - Title, `titleSmall`, white, one line. Below it: 13dp downloaded badge, 4dp gap, artist in `bodySmall` at white 78%. A long artist line scrolls as a marquee after a 2.5 s delay.
  - Like button, 26dp, white outline plus or green check.
  - Play or pause icon, 30dp, white. Descriptions "Play" / "Pause".
- Along the bottom edge, inset 8dp each side: a 2dp progress line, white on white 25%, pill-ended. Updates every 500 ms while playing.
- **Tap** the bar: opens Now Playing. Press scale 0.985.
- **Swipe the title sideways**: the text follows the finger. Past 28% of its width, left goes to the next song and right goes to the previous. The text then slides back over 220 ms (`ui/NowPlaying.kt:143-163`).
- It cannot be dismissed.

### 2.3 Home

Purpose: quick ways back into music, then browse shelves. `ui/HomeScreen.kt`.

One vertically scrolling list. Top to bottom with the "All" filter:

1. **Header row.** Below the status bar, padding start 16, top and bottom 8. The settings button, then a horizontally scrolling row of chips 8dp apart: "All", "Albums", "Artists", "Folders". Exactly one is always selected. Default "All". Tapping the selected chip does nothing.

2. **Shortcut grid** (`ui/HomeScreen.kt:119-129`, `339-378`). Two columns, up to six tiles, 8dp gaps, 16dp side padding, 8dp above.
   - Each tile is 56dp tall, radius 4, `surfaceContainer` fill. A 56dp square image sits flush against the left edge with square corners. The title follows in `titleSmall`, up to two lines, 10dp side padding.
   - The first tile is always "Liked Songs" with the liked tile image.
   - The rest come from recently played songs: the song's album if the album has more than one song, otherwise its folder, otherwise its single-song album. Remaining slots are filled with the albums that have the most songs (`ui/HomeScreen.kt:254-271`).
   - Album tiles show the album cover. Folder tiles show a mosaic.
   - Tap opens Liked Songs, the album or the folder.

3. **Wrapped card** (`ui/HomeScreen.kt:276-308`). Shown only when the listening log has at least one entry and this year's recap is not empty.
   - 16dp side margin, 16dp above. Radius 10. Fixed fill `#2A0E3D` (plum) in both themes. Inner padding 14.
   - Left: a 64dp mosaic of the year's top songs, radius 4. 14dp gap.
   - Headline in `titleLarge`, colour `#FFB627` (marigold), one line: "Your {year} so far", or "Your {year} Wrapped" from 24 December.
   - Body in `bodyMedium` at white 85%, up to two lines: "{minutes} minutes. See your top songs and cards to share."
   - Tap opens Wrapped. Press scale 0.98.

4. **"Speed dial"** (`ui/HomeScreen.kt:133-153`, `312-336`). Shown when radio has chosen songs.
   - Section title "Speed dial".
   - A 3-column grid of up to nine square tiles, 8dp gaps, 16dp side padding. Radius 6.
   - Each tile is the cover full-bleed, under a vertical gradient from transparent at 45% to black 80% at the bottom, with the song title bottom-left in `labelLarge`, white, up to two lines, 8dp padding. The title turns `primary` green on the current song.
   - **Tap starts radio from that song. Long-press opens the track menu.**
   - Contents: up to four favourites (most played, with liked songs weighted up), then unplayed songs that sound like what you play. At most two per artist and one per album. The set changes once a day (`radio/SpeedDial.kt`, `radio/RadioController.kt:66-88`).

5. **"Recently played"** (`ui/HomeScreen.kt:154-169`). Shown when there is history. Section title, then a shelf of up to 12 shelf cards: cover, song title, artist. The current song's title is green. Tap plays the recents list from that song, with context "Recently played".

6. **"Start listening"** (`ui/HomeScreen.kt:170-191`). An overline "Picked at random from your songs" in `bodyMedium` `onSurfaceVariant` (26dp above), then "Start listening" in `titleLarge` (2dp above, 8dp below). Four track rows chosen at random. Tap plays those four from that row, context "Start listening".

7. **"Your folders"** (`ui/HomeScreen.kt:194-203`). Section title, then a shelf of folder cards: 160dp wide, mosaic with 8dp radius, folder name in `titleSmall`, "{n} songs" in `bodySmall` `onSurfaceVariant`. Tap opens the folder.

8. **"Albums"** (`ui/HomeScreen.kt:205-214`). Section title, then a shelf of the 14 albums with the most songs: cover, album name, album artist. Tap opens the album.

9. **"More from" spotlight** (`ui/HomeScreen.kt:216-238`). Shown when one of the 14 biggest artists has two or more albums. A tappable header row (padding 16 sides, 26 top, 12 bottom): a 52dp circular cover, 12dp gap, "More from" in `bodySmall` `onSurfaceVariant` above the artist name in `titleLarge`. Tap opens the artist. Below it, a shelf of that artist's albums with the year as subtitle, or "Album" when the year is unknown.

10. **"Your top artists"** (`ui/HomeScreen.kt:240-249`). Section title, then a shelf of the 14 artists with the most songs. Each is a 130dp circular cover with the name centred below in `titleSmall`, one line. Tap opens the artist.

**Filter variants**

| Chip | What remains under the header row |
|---|---|
| "All" | Everything above |
| "Albums" | Only the "Albums" shelf |
| "Artists" | Only "Your top artists" |
| "Folders" | Only "Your folders" |

**Added 2026-11-23, in the Expressive and Comfort styles** (state in `ui/model/HomeModel.kt`; worked out in `home/HomeFeed.kt`, `home/HomeShelves.kt` and `home/TasteMixes.kt`; drawn in `ui/expressive/ExpressiveHome.kt` and `ui/comfort/ComfortHome.kt`). Four sections built from your own listening, under "All" only. They use the proposals' names (BRIEF.md), so "Speed dial" is "Instant mixes" there.

- **"Mixes"**, straight after "Instant mixes", with the overline "Songs you play, grouped by sound" (Expressive's section overline, Comfort's small label). A shelf of three to five tiles: a mosaic of the mix's covers, its name on up to two lines, and "{n} songs". Tap plays the mix from its first song, in its own order with shuffle off, under the mix's name.
  - How a mix is made: the played and liked songs that analysis has reached are grouped by their sound embeddings (k-means; three groups, one more per 20 such songs, up to five). A group needs four songs to become a mix. Each mix is up to 25 songs: two thirds from the group, and every third song an unplayed or rarely played song that sounds like the group. Songs you mostly skip are left out, and no song is in two mixes.
  - Name: "Mix: {artist}, {artist} and more" after the two artists with the most plays in the group (a favourite counts as three plays), "Mix: {artist} and more" with one, "Mix {n}" when the songs have no artist tags. An artist who already leads an earlier mix gives way to the next one, and a name that would repeat becomes "Mix {n}".
  - The mixes change once a day, at local midnight, and stay the same all day, across launches. With fewer than 12 played or liked songs analysed there are none and the section is absent; it appears the same day once analysis reaches enough.
  - Instant mixes are unchanged: nine single songs, each starting radio. The mixes are fixed lists, a different thing, so they sit beside Instant mixes rather than replacing them.
- **"On repeat"**, after "Recently played": your most played songs of the last 30 days, at least two plays each, most first. A skip (under half the song) or a song swiped out of the queue is not a play.
- **"Haven't heard in a while"**: favourites, and songs played three times or more, with no play in the last 60 days, most played first (a favourite counts as three plays). A favourite you never played counts from the day you liked it.
- **"Newly added"**: songs whose files arrived in the last 30 days, newest first, by the date MediaStore gives. Absent when every song in the library is that new, as on a phone whose music was copied over in one go.
- These three are shelves of up to 20 songs drawn like "Recently played": cover, title, artist, and the playing song marked. Tap plays the shelf from that song, under "On repeat", "Haven't heard in a while" or "Newly added". Each is absent with fewer than four songs.
- **"Moods"** (added 2026-11-25; `home/MoodMixes.kt`, docs/features/mood-mixes.md), straight after "Mixes", with the overline "Your songs, sorted by how they feel". A shelf of one tile per mood that has a mix today: the mood's name, "{n} songs", its one line ("Slow and easy", "Bright and cheerful", "Fast and driving", "Mostly instrumental, steady", "Made for dancing", "Sad and reflective", "Tender and warm") and a mosaic of four covers. Expressive's tile is a 200 by 108 tonal container in the first cover's hue; Comfort's is a 200 by 133 card. Tapping opens the mood's page (section 2.9b). With moods picked, only those moods show, in the picked order; with none, every mood that has a mix, in the order Chill, Happy, Energetic, Focus, Party, Melancholy, Romantic.
  - Until picks are saved or "Not now" is tapped, a full-width card comes first under the heading: "What do you like to listen to?", "Pick moods, genres and languages, and the Moods shelf follows them.", a filled "Choose" (opens Moods and interests) and a text "Not now" (hides the card for good).
  - The mixes change once a day, and again the same day when the picks change or more songs get mood scores. Listening during the day doesn't change them.
- Order under "All": shortcuts, Recap, Instant mixes, Mixes, Moods, Recently played, On repeat, Haven't heard in a while, Newly added, Start listening, folders, Albums, More from, Top artists.

**States**

- **Empty library** (`ui/HomeScreen.kt:90-98`): no header, no chips. A centred empty message, title "No songs on this phone", body "Copy music files onto this phone, in Music or any other folder, and they will show up here." The tab bar is still present.
- **No history yet**: the shortcut grid is "Liked Songs" plus the five biggest albums, and "Recently played" is absent. This is what `home.png` shows.
- **No listening log**: no Wrapped card.
- **Radio has not run yet**: no "Speed dial".
- **Fewer than four songs for a shelf, or fewer than 12 played or liked songs analysed**: that shelf, or "Mixes", is absent (added 2026-11-23).
- **Fewer than eight songs with mood scores**: "Moods" and its card are absent (added 2026-11-25). Scores come from stored embeddings, so they appear on the first Home refresh after analysis has run.
- **Light theme**: tiles and chips use the light greys. The Wrapped card and Speed dial tiles keep their fixed colours.

Artists have no photos. Every "artist picture" in the app is the cover of one of that artist's songs.

### 2.4 Search

Purpose: find songs, artists, albums and folders, or browse when the field is empty. `ui/SearchScreen.kt`.

- **Header** (padding start 16, top 10, bottom 12): settings button, 12dp gap, "Search" in `headlineSmall`.
- **Search field** (`ui/SearchScreen.kt:118-136`, `182-197`). Full width with 16dp side margins, radius 6, single line, no underline.
  - Colours are inverted against the theme: the field is `inverseSurface` (white in dark mode, `#121212` in light), text and icons are `inverseOnSurface`, the placeholder is `outline` grey, the cursor is `primary`.
  - Placeholder: "What do you want to listen to?"
  - Leading `Search` icon. A trailing `Close` icon appears once there is text, description "Clear search", and empties the field.
  - Results update on every keystroke. There is no submit step, no search history and no suggestions.
- The screen resizes above the keyboard.

**Empty field: browse grid** (`ui/SearchScreen.kt:139-153`, `199-228`)

- "Start browsing" in `titleMedium`, 18dp above, 2dp below.
- A 2-column grid, 10dp gaps, 16dp side padding.
- Each tile is 92dp tall, radius 6, filled with one of eight fixed colours used in rotation: `#8D67AB`, `#E13300`, `#1E3264`, `#148A08`, `#BA5D07`, `#503750`, `#0D73EC`, `#AF2896`.
- Title top-left in `titleMedium` bold, white, up to two lines, padding 12 with 44dp kept clear on the right.
- A 68dp cover with 4dp radius sits in the bottom-right corner, rotated 25° and pushed 14dp right and 8dp down so the tile clips it.
- Tiles are every folder first, then the eight albums with the most songs. Tap opens the folder or album.
- The tile colours are fixed. They are not taken from the art.

**With a query: results list** (`ui/SearchScreen.kt:155-177`)

One flat list with no section headings, in this order:
1. Up to 5 artists: result row with a circular cover, subtitle "Artist".
2. Every matching folder: result row, subtitle "Playlist".
3. Every matching song: track row. Tap plays all matching songs from that row, context "Search results".
4. Up to 10 albums: result row, subtitle "Album, {artist}".

Matching ignores case and accents. Names that start with the query rank first, then names that contain it, then songs whose artist or album contains it and albums whose artist contains it (`ui/SearchScreen.kt:70-89`). User playlists are not searched.

**No matches**: empty message, title `No results for "{query}"`, body "Check the spelling, or try fewer words."

**Moods (added 2026-11-25).** Browse has "Moods" between the folders and "Genres", in the same tiles, one per mood that has a mix today, picked or not. A query also matches up to three mood names, labelled "Mood" (Expressive: a result row with a mosaic; Comfort: a tile in the results shelf).

**Genres (added 2026-11-23).** In both styles Browse has "Genres" after the folders, in the folders' tiles: Expressive's tonal tiles in each genre's cover hue with a mosaic and "{n} songs", Comfort's neutral cards. Biggest genre first; a genre with fewer than five songs is not shown. A query also matches up to three genre names, labelled "Genre" (Expressive: a result row with a mosaic; Comfort: a tile in the results shelf). Section 2.9a has the genre page and where genres come from.

### 2.5 Your Library

Purpose: everything you have, as a list or grid, with a full song list behind one filter. `ui/LibraryScreen.kt`.

- **Header** (padding start 16, end 8, top 8): settings button, 12dp gap, "Your Library" in `headlineSmall`, then two icon buttons:
  - `Search` at 26dp, description "Search your library". It switches to the Search tab. It does not filter this screen.
  - `Add` at 28dp, description "Create playlist". Opens the playlist name dialog titled "Give your playlist a name". Confirming creates the playlist and opens it.
- **Filter chips** (scrolling row, 16dp side padding, 10dp vertical, 8dp apart): "Playlists", "Albums", "Artists", "Downloaded". None is selected by default. Tapping the selected chip clears it.

**Default and the first three filters: collection list**

- **Sort row** (padding start 16, end 8): `Sort` icon at 18dp, 8dp gap, "Recents" in `labelLarge`, then a view toggle on the right at 20dp. The toggle shows `GridView` with description "Show as grid" in list mode, and `ViewList` with "Show as list" in grid mode. "Recents" is a fixed label. Tapping it does nothing.
- **List mode** (default): result rows with a 64dp cover.
- **Grid mode**: three columns, 14dp horizontal and 16dp vertical gaps, 16dp side padding. Each tile is a square cover, 6dp gap, title in `titleSmall` up to two lines, subtitle in `bodySmall` `onSurfaceVariant` one line. Artist tiles are circular and centre their text. Others align left.
- Cover per item type: Liked Songs uses the liked tile with 4dp radius, artists use a circle, everything else uses a mosaic (albums pass one cover, so they show that cover).

| Item | Title | Subtitle |
|---|---|---|
| Liked Songs | "Liked Songs" | "Playlist, {n} songs" |
| User playlist | its name | "Playlist, {n} songs" |
| Folder | its name | "Playlist, {n} songs" |
| Artist | its name | "Artist" |
| Album | its name | "Album, {artist}" |

| Filter | Contents, in order |
|---|---|
| none | Liked Songs, user playlists, folders, the 40 most recently played artists, all albums |
| "Playlists" | Liked Songs, user playlists, folders |
| "Albums" | all albums |
| "Artists" | all artists |

Albums and artists are ordered by how recently one of their songs was played, then alphabetically (`ui/LibraryScreen.kt:107-115`). Folders and user playlists look identical here and are both labelled "Playlist".

**"Downloaded" filter: every song** (`ui/LibraryScreen.kt:151-184`)

- The sort row is replaced by a header row (padding start 16, end 8, bottom 4): `Sort` icon 18dp, 8dp gap, "{sort}, {n} songs" in `labelLarge`, and a `Shuffle` icon button on the right, description "Shuffle all songs".
- Tapping the sort label cycles "Title" → "Artist" → "Recently added" → "Title". The label reads for example "Title, 279 songs".
- Shuffle plays all songs shuffled from a random one, context "All songs".
- Below: one track row per song. Tap plays the whole list from that row, context "All songs".
- The grid toggle is not available in this filter.

### 2.6 Collection screens: shared frame

Album, Artist, Folder, Playlist and Liked Songs share one frame. `ui/CollectionScreens.kt:68-245`.

**Collapsing top bar** (`ui/CollectionScreens.kt:76-103`). Drawn over the list, under the status bar, 4dp side padding.
- At the top of the list: transparent background. The back button sits on a black 35% circle so it reads over any cover. The title is invisible.
- Once the header has scrolled about 520 pixels, or past the first list item: the bar background becomes the cover tint over 180 ms, the scrim disappears, and the collection title appears in `titleMedium`, one line.
- Playlist screens add a kebab on the right.

**Gradient header** (`ui/CollectionScreens.kt:128-162`). Used by all except Artist.
- **A vertical gradient from the cover tint at the top to the background colour at the bottom**, behind the whole header block.
- 52dp of space below the status bar, then the cover centred with 18dp below it.
- Title in `headlineSmall`, up to three lines, 16dp side padding.
- 6dp, then a subtitle line that varies by screen.
- An optional detail line in `bodySmall` `onSurfaceVariant`, 4dp above.

**Control row** (`ui/CollectionScreens.kt:189-231`). Directly under every header. Padding start 12, end 16, top and bottom 6. Left to right:
1. A 34×44dp thumbnail of the first song's cover, radius 4. Decorative.
2. `AddCircleOutline` at 26dp in `onSurfaceVariant`, description "Add {name} to a playlist". Opens the playlist picker for every song in the collection. Disabled when the collection is empty.
3. The downloaded badge at 24dp. Decorative.
4. Extra buttons on playlists only (section 2.9).
5. Flexible space.
6. `Shuffle` at 28dp, description "Shuffle {name}". Starts the collection shuffled from a random song. It is `primary` green while shuffle is on and this collection is the one playing, and `onSurfaceVariant` otherwise. Disabled when empty.
7. 8dp gap.
8. The play circle, 56dp, `primary`. If this collection is already the one playing it toggles play and pause and shows the pause glyph while playing. Otherwise it plays from the first song with shuffle turned off.

"This collection is playing" means the player's context name equals the collection name and the current song belongs to it (`ui/CollectionScreens.kt:195`).

**Missing item** (`ui/CollectionScreens.kt:238-245`). If the album, artist or playlist no longer exists: a back button and an empty message, "{This album | This artist | This playlist} is gone" / "It may have been removed from this phone."

**Light theme.** The header tint is mixed at 40% over white, so the gradient is a pale wash and the title stays dark (`ui/CollectionScreens.kt:69-73`).

### 2.7 Album

Purpose: one album's songs in track order. `ui/CollectionScreens.kt:247-291`.

- Cover: 62% of the screen width, square, radius 4.
- Title: album name.
- Subtitle: the artist line. A 24dp circular cover of one of the artist's songs, 8dp gap, artist name in `titleSmall`. Tap opens the artist when the album has a known artist.
- Detail: "Album" or, for a one-song album, "Single", followed by " • {year}" when known. Example: "Album • 2025".
- Control row.
- Track rows **without covers**. Tap plays the album from that song, context is the album name.
- Footer in `titleSmall`, 16dp padding: "{n} songs • {duration}", for example "17 songs • 1 hr 2 min" or "4 songs • 18 min".

### 2.8 Artist

Purpose: an artist's songs and albums. `ui/CollectionScreens.kt:293-377`.

- **Hero image** instead of the gradient header: full width, aspect ratio 1.05 (slightly wider than tall), square corners, showing the cover of the artist's first song. Over it, a gradient from transparent at the midpoint to black 72% at the bottom. The artist name sits bottom-left in a 44sp bold display style (line height 48), white, up to two lines, padding 16 horizontal and 8 vertical. The image runs under the status bar.
- 12dp, then "{n} songs, {duration}" in `bodyMedium` `onSurfaceVariant`, for example "14 songs, 1 hr 13 min".
- Control row.
- **"Popular"** section title. The first five songs, each prefixed with its rank 1 to 5 in `bodyMedium` `onSurfaceVariant` (16dp from the edge, 16dp wide), followed by a normal track row with cover.
- **"Popular releases"** section title, shown when the artist has albums. One result row per album: cover, name, "{year} • Album" or "{year} • Single" (year omitted when unknown). Tap opens the album.
- **"All songs"** section title, shown when the artist has more than five songs. The remaining songs as track rows.
- Playing any song queues the artist's whole ordered list, context is the artist name.

"Popular" has no play counts behind it. The order is most recently played first, then alphabetical (`ui/CollectionScreens.kt:302-306`).

### 2.9 Folder, Liked Songs and Playlist

All three use the gradient header and control row from 2.6, followed by track rows with covers.

**Folder** (`ui/CollectionScreens.kt:379-409`). A folder at the top of the music library that isn't an artist's, shown as a read-only playlist. A folder is an artist's when more than half of its tagged songs credit the folder's name as artist, album artist or composer, or when none of its songs has those tags (`data/LibraryScope.kt`). In the owner's library the playlists are English, Hindi, Tamil and Phonk; in another library they might be Bollywood, Workout or Snaptube.
- Cover: mosaic, 62% width. Title: folder name.
- Subtitle: "Playlist from a folder on this phone" in `bodyMedium` `onSurfaceVariant`.
- Detail: "{n} songs, {duration}".
- No rename, delete, add or reorder.

**Liked Songs** (`ui/CollectionScreens.kt:411-443`).
- Cover: the liked tile, 50% width, square, radius 4. Title: "Liked Songs".
- Subtitle: "{n} songs, {duration}". No detail line.
- Songs are newest-liked first.
- The header tint comes from the first liked song, or falls back to `#333333`.
- Empty: "Songs you like will appear here" / "Tap the plus on a song to save it." The control row is still shown. Its add and shuffle buttons are disabled and its play button does nothing.

**Playlist** (`ui/CollectionScreens.kt:445-592`).
- Cover: mosaic of its songs, 62% width. Title: playlist name. Subtitle: "{n} songs, {duration}". No detail line.
- **Top bar kebab**, description "Playlist options", opens a dropdown menu:
  - `Edit` icon, "Rename" → playlist name dialog titled "Rename playlist", confirm "Rename", prefilled with the current name.
  - `Delete` icon, "Delete playlist" → delete dialog.
- **Extra control-row buttons**, after the downloaded badge:
  - `Add`, description "Add songs" → Add songs sheet.
  - `SwapVert`, description "Reorder songs". Toggles reorder mode and becomes `Check`, "Done reordering". Disabled with fewer than two songs.
- **Reorder mode**: each row's kebab becomes a `DragHandle` (description "Drag {title} to reorder"). Dragging the handle moves the row. The dragged row gets a `surfaceContainerHighest` background. Tapping a row does not play in this mode. Long-press still opens the menu. The new order is saved when the drag ends.
- The track menu opened from a playlist row has one extra entry, "Remove from this playlist".
- Empty: "Let's find something for your playlist" / "Tap the plus above to add songs from your library."
- A song can appear only once in a playlist.

**Smart shuffle** (added 2026-11-23, in the Expressive and Comfort styles; `ui/model/CollectionModels.kt`, `playback/SmartShuffle.kt`, `playback/PhysicalShuffle.kt`, `radio/RadioController.kt`). Playlists and Favourites only.

- Where: the first row of the more menu in the bar, "Smart shuffle" with the `shuffle_on` glyph. The mocks have no place for it beside Shuffle, so it went in the menu. Favourites gains a more button for it, described "Favourites options". The row, and Favourites' button, show only when the collection has songs. While smart shuffle is on for this collection the row is ticked (Expressive: the glyph filled in `primary` and a trailing tick; Comfort: the row selected, with its tick), and TalkBack reads it as on.
- On: radio picks one song for every two in the collection, at most 100, from the rest of the library. Its seed is the collection's average sound. It keeps to the collection's languages, prefers songs that aren't among the last 30 played, and never picks a song from the collection or another version of one. The collection is shuffled and a pick goes after every second song. If this collection is the one playing, the song playing carries on and the rest follows it; otherwise the queue is replaced. It plays under the collection's name, so "Playing from" and the collection's Play and Shuffle controls still show it as playing, with shuffle on.
- When nothing fits, for example before any analysis: the snackbar "Smart shuffle has nothing to add yet, so this is a plain shuffle", and the collection shuffles as the Shuffle button does.
- Off (the ticked row again): every pick leaves the queue except one that is playing, which finishes; shuffle stays on. Turning shuffle off anywhere also takes the picks out and puts the collection back in order. Playing anything else ends smart shuffle.
- The playlist or Favourites itself never changes.

### 2.9a Genre (added 2026-11-23)

`ui/model/CollectionModels.kt` (`rememberGenreModel`), drawn by `ExpressiveGenre` and `ComfortGenre`. Opened from Search (section 2.4).

- **Where genres come from** (`data/Genres.kt`). A song's genres are its genre tag's, when it has one. A tag holding several separates them with ";"; a "/" is part of the name, so "Funk / Soul" is one genre. YouTube category names count as no tag. A song without a tag takes genres from analysis: the model scores 400 Discogs styles, each named "Genre---Style", and Attic keeps a song's five strongest. The song goes in the genre whose styles add up most, and also in any other that reaches 60% of that. "Non-Music" is never used, and a song whose best style scores under 0.05 gets no genre. Names match with case, accents and punctuation folded, so a "Hip-Hop" tag and the model's "Hip Hop" are one genre, shown as most of its songs spell it. The list is built off the main thread and again as analysis stores more styles.
- **Layout.** The folder's (section 2.9): mosaic of the genre's songs, the genre name, "Genre, from tags and analysis", then "{n} songs, {duration}". Then "Albums" as a shelf, when any album has at least two songs and at least half its songs in the genre, most-matching first; then "Songs" (with no albums, just the songs).
- **Controls.** Play / pause and shuffle as on any collection, plus radio (`sensors` glyph, description "Start radio from {genre}"). Expressive puts radio in the cluster where add to playlist sits elsewhere, and moves add to playlist to the bar; Comfort adds radio to its row of buttons, between shuffle and add to playlist.
- **Genre radio.** Context "Radio · {genre}". It opens with the song nearest the middle of the genre's sound (the normalised mean of its analysed songs' embeddings) and keeps steering towards that middle instead of the first song. With none of the genre's songs analysed yet it shows "Radio can start from this genre once its songs are analysed" and plays nothing.
- **Gone.** A genre that no longer has five songs: "This genre is gone".

### 2.9b Mood (added 2026-11-25)

`ui/model/MoodModels.kt` (`rememberMoodModel`), drawn by `ExpressiveMood` and `ComfortMood` (`ui/expressive/ExpressiveMoods.kt`, `ui/comfort/ComfortMoods.kt`). Opened from Home's Moods shelf and Search.

- **Layout.** The genre page's: mosaic of the mix's covers, the mood's name, "Mood mix · {n} songs, {duration}", then "Picked from how your songs sound and what you play. New each day." Then the songs, in the mix's order.
- **Controls.** Play / pause and shuffle, plus Mood radio (`sensors` glyph, description "Mood radio"), where the genre page has its radio. No add to playlist: the mix changes daily. The mix plays under "{mood} mood mix".
- **Mood radio.** Context "Radio · {mood}". It opens with the song that fits the mood nearest the middle of the day's mix, keeps steering toward that middle, picks only songs that fit the mood and keeps to the picked languages. When a mood mix runs out, autoplay carries on the same way, leaving out the mix's songs. Before any song has mood scores it shows "Mood radio can start once songs have been analysed".
- **No mix.** Opened before the day's mixes are built, it builds them and shows nothing meanwhile; a mood with no mix then shows "This mood has no mix today".

**Moods and interests** (`rememberInterestsModel`, drawn by `ExpressiveInterests` and `ComfortInterests`). Full screen in a window over the shell, as the first-run pages are: no tabs, dock or mini player. The large title "Moods and interests", "The Moods shelf on Home follows what you pick here. It all stays on this phone.", then three groups of filter chips: "Moods you like" (the seven moods), "Genres" (the library's genres, biggest first, up to 24; "Genres show up once your songs have genre tags or have been analysed." when there are none) and "Languages" (those the library has, from Tamil, Hindi, Telugu, Malayalam, Kannada, English and No vocals). "Save" is pinned at the bottom; it saves, rebuilds the mixes and goes back. Back leaves without saving. Expressive's group titles are in `primary`; Comfort puts each group's chips on a card under its label.

### 2.10 Now Playing

Purpose: the full player. `ui/NowPlaying.kt:210-600`.

A full-screen overlay that slides up over everything. **It is always rendered with the dark theme, even when the app is in light mode** (`ui/NowPlaying.kt:211-216`), because it sits on the cover's colour.

**Background.** A vertical gradient over the first screen-height: the cover tint at the top, black 72% composited over the tint at 72% of the way down, and `#121212` at the bottom (`ui/NowPlaying.kt:279`). The tint animates over 600 ms when the song changes.

**The page scrolls vertically.** The first "page" is exactly one screen tall and holds the player. More cards sit below it.

Player page, 24dp side padding, top to bottom:

1. **Header** (4dp above):
   - `KeyboardArrowDown` at 30dp, left, description "Close player". Closes the overlay.
   - Centred: "Playing from" in `labelSmall` at white 70%, and under it the context name in `titleSmall`, one line. The context is the name of whatever started playback: an album, artist, folder or playlist name, "Liked Songs", "Recently played", "Start listening", "Search results", "All songs", "Your top songs", or "Radio · {song title}". When there is none it reads "Your library".
   - `MoreVert`, right, description "More options for {title}". Opens the track menu.
2. Flexible space.
3. **Cover pager** (`ui/NowPlaying.kt:359-393`). The cover fills the width as a square with 8dp radius. **Swipe sideways to change song**: each page is one queue entry, 16dp apart, and settling on a page jumps to that song without changing whether it is playing. **While paused the current cover shrinks by 14dp on every side**, animated over 260 ms, and grows back on play.
4. Flexible space.
5. **Current lyric line** (`ui/LyricsScreen.kt:157-172`). Only when the song has synced lyrics saved and the current line is not blank: that line in `titleSmall`, one line, 12dp below. Visible in `lyrics_card.png` above the title.
6. **Title row**: song title in `headlineSmall`, one line. 2dp below, the artist in `bodyLarge` at white 70%, one line, scrolling as a marquee after 2.5 s when too long. Tapping the artist opens the artist screen and closes the player, when that artist is in the library. On the right, the like button at 28dp.
7. 12dp, then the **seek bar** (`ui/NowPlaying.kt:471-516`). A 28dp-tall touch area with a 3dp round-capped line: white 25% for the track, white for the elapsed part, and a round white handle of 6dp radius. **While being dragged the elapsed part and handle turn `primary` green and the handle grows to 8dp** over 120 ms. Tap anywhere to jump. Drag to scrub, with the seek applied on release. Description "Seek in {title}".
8. Under it: elapsed time on the left and total duration on the right, both `labelMedium` at white 70%, format `m:ss`. It shows the total, not time remaining.
9. 4dp, then the **transport row**, spaced edge to edge:
   - Shuffle toggle.
   - `SkipPrevious` at 40dp in a 56dp button, description "Previous".
   - Play circle at 66dp, **white fill with a black glyph**.
   - `SkipNext` at 40dp in a 56dp button, description "Next".
   - Repeat toggle.
10. **Bottom row** (6dp above, 4dp below): on the left the 13dp downloaded badge, 6dp gap, and "Downloaded" in `labelMedium` `primary` green. On the right `QueueMusic`, description "Open queue".

**Shuffle and repeat toggles** (`ui/NowPlaying.kt:395-439`). Off: white icon. On: `primary` green icon with a 4dp green dot 16dp below its centre. The colour animates over 160 ms.

| Control | State | Icon | Description |
|---|---|---|---|
| Shuffle | off | `Shuffle`, white | "Turn shuffle on" |
| Shuffle | on | `Shuffle`, green + dot | "Turn shuffle off" |
| Repeat | off | `Repeat`, white | "Repeat off, tap to repeat all" |
| Repeat | all | `Repeat`, green + dot | "Repeat all, tap to repeat one" |
| Repeat | one | `RepeatOne`, green + dot | "Repeat one, tap to turn off" |

Repeat cycles off → all → one → off. Turning shuffle on reorders the real queue around the current song, and turning it off restores the original order (`playback/PhysicalShuffle.kt:8-12`), so the Queue screen always shows what will play.

**Below the player page**, 16dp side padding, 14dp between cards, 24dp at the end:

- **Lyrics card** (section 2.12).
- **"About the artist" card** (`ui/NowPlaying.kt:549-575`). Shown when the song's artist exists in the library. Radius 10, fill `#282828`. The top is the artist's cover image at full card width, aspect ratio 1.35, with "About the artist" in `titleSmall` white over its top-left corner (16dp padding). Below, with 16dp padding: the artist name in `titleLarge` and "{n} songs in your library" in `bodyMedium` at white 70%. The whole card opens the artist screen. Press scale 0.985.
- **"Credits" card** (`ui/NowPlaying.kt:577-600`). Radius 10, fill `#282828`, 16dp padding, 10dp between entries. "Credits" in `titleMedium`, then entries with the value in `bodyLarge` above its label in `bodySmall` at white 70%: "Performed by" with the artist, "Composer" with the composer tag when there is one, "Album" with the album when known, "Year" with the year when known, "Genre" with the genre tag when there is one. A genre that is a YouTube upload category ("Music", "People & Blogs", "Entertainment" and the like) counts as none. Not tappable.

**Sticky header** (`ui/NowPlaying.kt:518-545`). Once the page has scrolled past 55% of a screen-height, a bar fades in at the top (160 ms in, 120 ms out). Background is black 55% over the tint. It holds the title in `titleMedium`, the artist in `bodySmall` at white 70%, the like button, a 30dp play or pause icon, and a 2dp full-width progress line (white on white 20%).

**Dismiss by dragging** (`ui/NowPlaying.kt:228-263`). At the top of the page, pulling down drags the whole player with the finger. Released past 160dp it closes. Released sooner it springs back. Anywhere else, vertical drags scroll the page.

**States**

- Paused: pause glyphs become play glyphs, the cover is inset, the playing bars elsewhere freeze.
- Liked: green check in place of the plus.
- Radio active: there is no dedicated indicator. The only sign is the "Playing from" line reading "Radio · {seed song title}", and a queue that refills itself.
- No art: the note placeholder at 96dp in the pager, and the gradient uses the `#333333` fallback tint.
- Queue becomes empty: the overlay disappears.

### 2.11 Queue

Purpose: see, reorder and prune what plays next. `ui/QueueScreen.kt`.

A full-screen overlay over Now Playing, sliding up over 240 ms. Unlike Now Playing and Lyrics it follows the app theme, so it is light in light mode. Solid background colour, no tint.

- **Header** (4dp side padding): `KeyboardArrowDown`, description "Close queue"; "Queue" in `titleLarge`; and a "Clear" text button on the right, shown only when something is up next. "Clear" removes everything after the current song.
- **"Now playing"** small heading (`titleSmall`, `onSurfaceVariant`, padding 16 sides, 16 top, 4 bottom), then the current song as a track row: green title, playing bars, kebab. Tapping the row does nothing.
- **Up-next heading**: "Up next, {n} songs" (or "Up next, 1 song"), or "Nothing up next" when empty.
- **Up-next rows**: track rows with covers, where the kebab is replaced by a `DragHandle` (description "Drag {title} to reorder").
  - **Tap**: jump to that song and play.
  - **Drag the handle**: reorder. The dragged row gets a `surfaceContainerHighest` background.
  - **Swipe the row left or right**: removes it. The strip revealed behind is `surfaceContainerHighest` with "Remove" in `labelLarge` at the right end.
  - **Long-press**: track menu.
- Songs already played are not listed. There is no history section.
- The same song can be queued more than once.

**Radio behaviour visible here** (`playback/PlaybackService.kt:348-399`): while a radio session is active the service keeps ten songs up next, adding only to the end. Swiping one song out tells radio "less like this". "Clear" stops radio from refilling that queue.

**Smart shuffle picks** (added 2026-11-23, both styles): a song smart shuffle added (section 2.9) has the radio glyph (`sensors`, 20dp) before its drag handle, or before the more button on the "Now playing" row, described "Added by smart shuffle". Expressive draws it in `primary`, Comfort in the row's trailing ink. The marks are saved with the queue and come back after a restart.

### 2.12 Lyrics: card, inline line and full screen

`ui/LyricsScreen.kt`. A song's lyrics come from the phone first, with no network: a synced `.lrc` file with the song's name in its folder, then synced lyrics in the file's tags (ID3 `SYLT`, or LRC text in `USLT` or MP4 `©lyr`), then plain lyrics in the tags, then an untimed `.lrc` file (`data/LyricsPriority.kt`). They are read when the library loads and again when a song is shown, and they replace anything downloaded. Songs without them can be downloaded from LRCLIB. Both kinds are stored on the phone and look the same: nothing in the UI says where lyrics came from. A song's lyrics can be synced (timed lines), plain (untimed text), marked instrumental, or not found.

**Lyrics card** (inside Now Playing, `ui/LyricsScreen.kt:90-155`)

- Radius 10. **Fill is white 10% composited over the cover tint**, so it reads as a lighter shade of the player's colour. 16dp padding, 10dp between elements.
- "Lyrics" in `titleSmall`.

| State | Card body |
|---|---|
| Synced lyrics saved | Up to four lines starting at the line being sung. 22sp bold, line height 28, up to two lines each. The current line is white, the following ones are white 55%. A blank timed line shows "♪". The preview advances with playback. Then the "Show lyrics" pill. |
| Plain lyrics only | The text in 22sp bold, cut off after four lines with an ellipsis. Then the "Show lyrics" pill. |
| Instrumental | "This song is instrumental." in `bodyLarge`. No button. |
| None in the song's files, never downloaded | "Download once and they stay on this phone." in `bodyMedium` at white 80%, then a "Download lyrics" button. |
| Downloaded, none found | "No lyrics were found for this song." then the same "Download lyrics" button, which retries. |
| Downloading | A white circular progress indicator in place of the button. |

- "Show lyrics" is a white fully rounded pill, padding 16 horizontal and 8 vertical, label in `labelLarge` black, 4dp extra space above. Press scale 0.94. It opens the full-screen lyrics.
- "Download lyrics" is a standard Material filled button: `primary` green, black label, pill shape.
- Download results that produce a snackbar: "No lyrics found for {title}" and "Connect to the internet to download lyrics".

**Inline current line**: item 5 in section 2.10.

**Full-screen lyrics** (`ui/LyricsScreen.kt:176-254`)

A full-screen overlay sliding up over 260 ms, always dark-themed. **Background is the solid cover tint**, animated over 600 ms on song change. No gradient.

- **Header**: `KeyboardArrowDown` on the left, description "Close lyrics". Centred, the song title in `titleSmall` and the artist in `bodySmall` at white 75%. Nothing on the right.
- **Body**
  - Synced: a scrolling list, padding 20 horizontal and 16 vertical, 16dp between lines. Each line is 26sp bold with 33sp line height and wraps freely. **Lines up to and including the current one are white. Lines not yet sung are black at 75%.** The colour change animates over 240 ms. The list auto-scrolls so the current line sits about third from the top. **Tapping a line seeks to it.** Blank lines show "♪".
  - Plain: one block of text, 24sp bold with 33sp line height, 20dp padding, scrolls freely, no highlighting.
  - None, or instrumental: a centred "Download lyrics" button.
- **Footer**: a centred 56dp play circle, white with a black glyph, padding 16 horizontal and 8 vertical. There is no seek bar and no skip control here.

### 2.13 Bottom sheets

Purpose: per-song actions, choosing a playlist, correcting a song's language, and filling a playlist.

All are Material 3 modal bottom sheets with the stock drag handle, stock top-corner rounding and stock scrim, opened fully expanded, with a `surfaceContainer` background (`#282828` dark, `#F0F0F0` light). `ui/Sheets.kt`.

**Sheet row** (`ui/Sheets.kt:159-172`): full width, padding 20 horizontal and 13 vertical, a 24dp icon in `onSurfaceVariant`, 16dp gap, label in `bodyLarge`. Uses the stock ripple.

**Track menu** (`ui/Sheets.kt:73-157`)

- Header (padding 20 horizontal, 8 vertical): 48dp cover with 4dp radius, 12dp gap, title in `titleMedium` and artist in `bodyMedium` `onSurfaceVariant`, one line each.
- A divider in `outlineVariant` with 6dp above and below.
- Rows, in order:

| Icon | Label | Result |
|---|---|---|
| `Radio` | "Start radio" | Plays this song and starts a radio session seeded from it. Closes the sheet. |
| `FavoriteBorder` / `Favorite` | "Add to liked songs" / "Remove from liked songs" | Toggles like. Closes the sheet. |
| `PlaylistPlay` | "Play next" | Inserts after the current song. Snackbar "Playing next". |
| `QueueMusic` | "Add to queue" | Appends to the queue. Snackbar "Added to queue". |
| `PlaylistAdd` | "Add to playlist" | Replaces this sheet with the playlist picker. |
| `Language` | "Language: {name}" | Replaces this sheet with the language picker. |
| `RemoveCircleOutline` | "Remove from this playlist" | Only when opened from a user playlist row. |
| `Album` | "Go to album" | Only when the song's album is in the library. Closes overlays and opens it. |
| `PersonOutline` | "Go to artist" | Only when the song's first artist is in the library. |

`{name}` is one of "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English", "No vocals", or "Unknown" when the app cannot tell.

The like state appears as a heart in this menu and as a plus or check everywhere else.

**Added 2026-11-23, in the Expressive and Comfort styles only** (`ui/model/SheetModels.kt`, `ui/expressive/ExpressiveSheets.kt`, `ui/comfort/ComfortSheets.kt`). These use the proposals' names (BRIEF.md), so the favourite row reads "Add to favourites" / "Remove from favourites".

- **"Share"** (`share` icon), on every song in the library. Opens the system share sheet with the song's audio file, sent as its MediaStore `content://` URI with a read grant (`ui/SongShare.kt`). Expressive puts it last in the group with "Go to album" and "Go to artist"; Comfort puts it after "Go to artist", before "Remove from this playlist". If no app can take it: snackbar "Couldn't share this song".
- **"Sleep timer"** (`bedtime` icon), only in the menu opened from the Now Playing kebab. While a timer runs it shows what is left: "{n} min left" (minutes rounded up), "Less than a minute left", or "After this song". Off shows nothing.
  - Expressive: a one-row group of its own straight under the song header, its icon filled in `primary` while a timer runs and the status as the row's second line. Tapping it replaces the menu with a sheet titled "Sleep timer", the status under the title, and one single-choice group with radio buttons, like the language picker.
  - Comfort: the first row under the quick actions, with the status at its end, then `unfold_more`. Tapping it opens a pop-over on the row, like "Language": the caption "Sleep timer", the choices, and a tick in the accent on the current one. Tapping outside closes only the pop-over.
  - Choices, in order: "15 minutes", "30 minutes", "45 minutes", "1 hour", "End of this song", "Off". A choice sets the timer, closes the menu and shows a snackbar (section 2.17).
  - The timer runs in the playback service, so it carries on with the app closed (`playback/SleepTimer.kt`, `playback/PlaybackService.kt`). A countdown fades the volume out over its last 10 seconds, then pauses. "End of this song" fades over the song's last 10 seconds and pauses when the song ends. Either way playback is paused, not stopped, and the volume is back to full for the next play. A countdown that runs out while nothing is playing switches itself off and does not pause music started after that.
- **"Equaliser"** (`equalizer` icon), added 2026-11-25, only in the menu opened from the Now Playing kebab, next to "Sleep timer". Its state reads "Off", the default's preset name ("Bass boost", "Custom", a saved preset's name), "This song · {preset}" or "This album · {preset}". Tapping it replaces the menu with the equaliser sheet (section 2.13a).
  - Expressive: the second cell of the sleep timer's group, the state as its second line.
  - Comfort: the row after "Sleep timer", the state at its end in accent ink, then a chevron.
- **A song opened from another app that is not in the library** (section 2.21) gets a shorter menu: the song header, then "Sleep timer" and "Equaliser" when opened from Now Playing, and nothing else. Radio, the queue actions, favourites, playlists, language, "Go to" and "Share" all need a library song.

### 2.13a Equaliser sheet (added 2026-11-25)

Attic's own ten-band equaliser for the song that is playing. State and actions: `ui/model/EqualiserModel.kt`; drawing: `ui/expressive/ExpressiveEqualiser.kt` and `BandGraph.kt`, `ui/comfort/ComfortEqualiser.kt`; mocks: sheet 11 in `pixel/` and `oneui/`, though for Comfort the Equaliser section of `design/Attic Comfort.figcraft` (1.2.0) is now the reference. Over Now Playing it takes the cover's colours, as the track menu does.

**Which setting it changes.** A song plays with its own setting, else its album's, else the default. The sheet always edits the one the song is playing with, and the change is heard at once. The first thing under the title says which: "For all songs" over "Every song without its own setting", "For this album" over the album's name, or "For this song only" over the song's title. Expressive marks an own setting by squaring off the lead glyph and filling it in `primary`, and fills the whole piece with `primaryContainer` (its two lines in `onPrimaryContainer`), so the scope is seen from the bands while dragging; the colours cross-fade when the scope changes. Comfort puts the cover on an `accentSoft` bed.

Top to bottom:
- "Equaliser" with the switch. Off, the line "Off. Choose a preset or move a band to turn it on." shows under the title, the chips dim and the bands lose their colour; any chip or band movement turns it on. The line belongs to the switch as the title does, so a second tap where the switch was turns it back on. Off means nothing is applied, the default or any song's.
- **Preset chips**, one scrolling row: "Custom" and "Save as preset" while the bands match no preset, then the user's saved presets, then Flat, Bass boost, Bass reducer, Treble boost, Vocal, Loudness, Acoustic, Electronic, Hip-hop, Classical, Spoken word. The row opens with the selected chip second, the one before it whole at the start. In Expressive the selected chip is filled with `primary` (`onPrimary` label and tick) while the equaliser is on, and tonal (`secondaryContainer`) while it is off. A saved preset's chip, when selected, ends in a more glyph; tapping it again or long-pressing any saved chip offers "Rename" and "Delete preset".
- **Bands**: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k, each −12 to +12 dB. Drag up or down in a band's column (1 dB steps); a tap does nothing, a double tap sets 0. While dragging, the value shows in a bubble ("+4 dB"). The real response of the filters is drawn with the bands on one octave axis. Expressive draws each band as a bar from the 0 dB line over the response; Comfort draws ten capsule sliders with knobs on a `fill` card, with the response, its tinted area down to 0 dB and the +12, 0 and −12 rules behind them at the knobs' scale, so the line runs through every knob. Its action rows start 4dp under that card, with no hairline over the first.
- **Actions**: what is offered depends on the setting the song plays with; Expressive draws them as one row of buttons with one caption under it, Comfort as sheet rows, the removal row with its consequence as a second line. With the default: "Save for this song" and "Save for this album" (or the first alone when the song has no album), and the note "Saved settings come back whenever the song plays." With its album's: "Save for this song" and "Use default", with "Removes this album's setting" under "Use default". With its own: "Use default", or "Use album setting" when it sits over its album's, across the width with "Removes this song's setting". Saving or removing changes the scope piece in place; the sheet stays open. In Expressive the block is 82dp in every case at the default font size, so nothing above it moves when the scope changes. Saving from the default puts the default back as it was when the sheet opened, so edits made before "Save for this song" don't change every other song.
- **Dialogs**: "Save as preset" and "Rename preset" with a "Preset name" field ("Another preset has this name" blocks a duplicate); "Delete {name}?" with "Songs and albums saved with it keep their sound and show as Custom."

**Now Playing indicator.** When the equaliser is on and the song plays with its own or its album's setting, a small pill with the `equalizer` glyph and "This song" or "This album" sits in the header beside the kebab. Tapping it opens the sheet.

**Language picker** (`ui/Sheets.kt:174-194`)

- Title "Song language" in `titleLarge`, padding 20 horizontal and 8 vertical.
- One row per choice, in this order: "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English", "No vocals". The current one shows a `Check` icon, the others a `Language` icon.
- Tapping a choice saves it and closes. Dismissing changes nothing. A manual choice is never overwritten by the app's own guess.
- The language decides which songs radio stays with and feeds the Wrapped language breakdown.

**Playlist picker** (`ui/Sheets.kt:196-251`)

- Title "Add to playlist" in `titleLarge`.
- First row: `Add` icon, "New playlist". Opens the playlist name dialog titled "New playlist". On confirm the playlist is created and the songs are added to it.
- One row per user playlist: "{name} ({n} songs)". The icon is `Check` when the playlist already holds every song being added, otherwise `QueueMusic`.
- Tapping a playlist adds the songs, closes the sheet and shows "Added to {name}", or "Already in {name}" when none were new.
- Opened for one song from the track menu, or for a whole album, artist, folder, playlist or Liked Songs from the add button in a control row.

**Add songs** (`ui/Sheets.kt:298-374`)

- Takes 92% of the screen height.
- Title "Add songs" in `titleLarge`, padding 16 horizontal and 8 vertical.
- A filled text field, radius 8, no underline, stock Material colours, leading `Search` icon, placeholder "Find a song". Filters by title and artist as you type.
- Every library song as a track row with cover. The kebab is replaced by `Add` in `onSurface` (description "Add {title}") or, when the song is already in the playlist, `Check` in `primary` (description "Remove {title}").
- Tapping the row or its button toggles membership immediately. The sheet stays open. Long-press does nothing here.

### 2.14 Dialogs and menus

Purpose: name, rename and delete user playlists.

Material 3 alert dialogs with a `surfaceContainer` background and stock shape. `ui/Sheets.kt:253-296`.

**Playlist name dialog**

- Title in `titleLarge`. An outlined text field labelled "Playlist name", focused on open so the keyboard appears.
- Buttons: "Cancel" and a confirm text button that is disabled while the field is blank.

| Opened from | Title | Confirm | Prefilled |
|---|---|---|---|
| Library "+" | "Give your playlist a name" | "Create" | empty |
| Playlist picker "New playlist" | "New playlist" | "Create" | empty |
| Playlist kebab "Rename" | "Rename playlist" | "Rename" | current name |

**Delete playlist dialog**

- Title "Delete {name}?"
- Body "The playlist goes away. The songs stay in your library."
- Buttons "Cancel" and "Delete playlist". Confirming returns to the previous screen.

**Playlist overflow dropdown**: a stock Material dropdown anchored to the kebab with "Rename" (`Edit` icon) and "Delete playlist" (`Delete` icon).

### 2.15 Settings

Purpose: theme, lyrics download, radio options, backup. `ui/SettingsScreen.kt`. A plain scrolling page. The mini player and tab bar remain visible.

- **Header** (4dp side padding): back button, 4dp gap, "Settings" in `titleLarge`.

- **"Appearance"** section title. Three rows (padding 16 horizontal, 4 vertical), each a label in `bodyLarge` with a Material radio button at the right: "Match phone", "Dark", "Light". Tapping the row or the button selects it and the theme changes immediately. The default on a fresh install is "Dark" (`data/AppSettings.kt:18`).

- **"Playback"** section title, added 2026-11-23 after this extraction, in both styles between Appearance (Comfort: Style) and Lyrics (`ui/expressive/ExpressiveSettings.kt`, `ui/comfort/ComfortSettings.kt`; state and actions in `ui/model/SettingsModel.kt`).
  - "Volume levelling" over "Plays songs at a similar loudness, from their ReplayGain tags or the level measured during analysis. Songs not measured yet play as they are." with a switch. Default on. Expressive heads it with `volume_up` in a tonal circle.
  - "Crossfade", its length shown as "Off" or "{n} seconds" (Expressive: the supporting line; Comfort: the row's value in accent ink), over "Songs from one album played in order still run straight into each other." Tapping opens the kit's menu of lengths: "Off", "2 seconds", "4 seconds", "6 seconds", "8 seconds", "10 seconds", "12 seconds", the current one ticked (Comfort's pop-over, captioned "Crossfade", opens over its row like the track menu's sleep timer). Default "Off". Expressive heads it with `transition_fade`.
  - "Equaliser" (changed 2026-11-25), always shown. Its state is "Off" or the default's preset name, and when songs or albums have their own setting it adds "With their own setting: 4 songs, 2 albums" (Comfort shows "Ten bands and presets. A song or album can keep its own setting." when none do). Tapping opens the Equaliser page: "Use equaliser" with a switch; "Default" with the same chips, bands and response as the sheet, always editing the default; "Albums with their own setting" and "Songs with their own setting", each row with its preset name and a remove button (no confirmation); "Songs and albums with their own setting" with "None yet. Open Equaliser from the menu on Now Playing and choose “Save for this song”." when there are none; and "Phone equaliser" over "Opens the equaliser on this phone." when the phone has one, which opens the system's sound-effects panel or an installed equaliser app on Attic's audio session ("Couldn't open the equaliser" if that fails).
  - Behind them (`playback/`): the equaliser is Attic's own filter chain after levelling (`playback/eq/`), so it sounds the same on every phone; levelling brings songs to about -14 LUFS without raising a peak past -1 dBFS, and an album played in order shares one gain. Crossfade never fades between consecutive songs of one album, with repeat-one, or when the sleep timer is set to "End of this song". The analysis status line counts a song once its loudness is measured as well (see "Song radio" below).

- **"Lyrics"** section title.
  - "{saved} of {total} songs have lyrics saved on this phone. Saved lyrics work offline." in `bodyMedium` `onSurfaceVariant`. {saved} counts lyrics read from the songs' files as well as downloaded ones.
  - 4dp, then "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent." in `bodySmall` `onSurfaceVariant`.
  - Idle: a text button "Download lyrics for all songs". A run reads the songs' files first and skips every song that already has lyrics, so songs with lyrics in their files never go online.
  - Running: 12dp, a linear progress bar across the width (16dp side margins), then "Checked {done} of {total}, found {found}" in `bodySmall`. The button is hidden.
  - After a run with failures, above the button in `bodySmall`: "{n} songs couldn't be checked. Download again to retry them." or, when it gave up after five failures in a row, "Stopped because the connection failed. Check your internet, then download again to continue."

- **"Moods"** section title (added 2026-11-25), then one row: "Moods and interests" over the picks ("Chill, Focus, Romantic · 3 genres · Tamil, English", or "Nothing picked yet"), with a chevron; it opens Moods and interests. Expressive leads it with `partly_cloudy_day`.
- **"Song radio"** section title.
  - A row (padding 16 sides, 12 below): "Autoplay" in `bodyLarge` over "When a queue reaches its last song, radio keeps going with similar songs." in `bodySmall` `onSurfaceVariant`, with a Material switch at the right. Tapping the row or the switch toggles it. Default on.
  - "Radio finds similar songs by listening to your files. This happens on this phone, and nothing is uploaded." in `bodyMedium` `onSurfaceVariant`.
  - 12dp, then a linear progress bar showing analysed songs over total songs. It is always visible. Since 2026-11-23 a song counts once its styles (section 2.9a) and its loudness (the Playback section below) are stored as well, so a library analysed before then shows as part done until the next charging run has added them. A song missing both gets them from one decode; a song missing only its loudness gets a quicker decode with no model run, and those go first.
  - A status line in `bodySmall`, 6dp above, one of:
    - "Analysing songs: {done} of {total}" while analysis is running
    - "All {total} songs analysed."
    - "{done} of {total} songs analysed. The rest continue next time the phone charges."
  - A text button "Analyse now without a charger", shown only when analysis is not running and songs remain.

- **"Backup"** section title, added 2026-11-23 after this extraction, in both styles (`ui/expressive/ExpressiveSettings.kt`, `ui/comfort/ComfortSettings.kt`; state and actions in `ui/model/SettingsModel.kt` and `ui/model/BackupController.kt`). `docs/backup.md` covers the format and merge rules.
  - "Back up to a file" over "Saves favourites, playlists, listening history, saved lyrics and settings in one file. Keep it somewhere other than this phone." Tapping opens the system's save picker with the name `attic-backup-{yyyy-mm-dd}.json`. While it writes, the supporting line reads "Saving the backup…". Afterwards a snackbar says "Backup saved" or "Couldn't save the backup".
  - "Restore from a file" over "Adds a backup to what is on this phone. Nothing here is deleted." Tapping opens the system's open picker. While it reads and merges, "Restoring the backup…". Both rows are disabled while either runs. Expressive heads them with `file_save` and `file_open` in tonal circles; Comfort draws them as plain rows.
  - A note: "Song analysis isn't in the file, so a new phone analyses the songs again while it charges. If backup is on in the phone's settings, Android also keeps a copy, encrypted with your screen lock."
  - After a file is picked, a dialog "Restore this backup?" with "Made {date}, with {n} favourites, {n} playlists, {n} plays and lyrics for {n} songs. They are added to what is on this phone, and nothing here is deleted. Theme, style and autoplay change to the backup's." and "Cancel" / "Restore".
  - Then "Backup restored" with "Added {n} favourites, {n} playlists, {n} plays and lyrics for {n} songs." (only what was new), "Songs were also added to {n} playlists that already had the same names." when a playlist merged, and "{matched} of {total} songs in the backup are on this phone." When some are missing, it adds "The rest keep their place in your favourites, playlists and history, and show up when their files are added to this phone." A restore that adds nothing says "Everything in this backup was already on this phone." One button, "Close".
  - Or "Couldn't restore" with one of these bodies and "Close":
    - "This file isn't an Attic backup."
    - "This backup was made by a newer version of Attic. Update Attic, then try again."
    - "This backup is damaged and can't be read. Nothing was changed."
    - "This file is too large to be an Attic backup."
    - "The file couldn't be opened."
    - "Something went wrong while restoring, so nothing was changed. Try again."
  - The state lives with the app, so leaving Settings or switching style mid-restore keeps the dialog.

- **"About"** section title. "Attic plays your own music files, from any folder. It has no account and no streaming. {n} songs on this phone." in `bodyMedium` `onSurfaceVariant`.

Progress bars, radio buttons, the switch and text buttons use stock Material 3 components coloured by the theme's `primary`. No colours are overridden.

### 2.16 Wrapped

Purpose: a recap of listening over a period, as an in-app page, a tappable story, and shareable images. `ui/WrappedScreen.kt`, `ui/WrappedCards.kt`, `wrapped/Recap.kt`.

It is available all year. A listen counts as a play after 30 seconds, or when the song finishes.

#### Wrapped page

A scrolling page. The mini player and tab bar remain visible.

- **Header** (4dp side padding): back button, 4dp gap, "Wrapped" in `titleLarge`.
- **Period chips** (padding 16 horizontal, 8 vertical, 8dp apart): "This week", "This month", "This year", "All time". One is always selected. Default "This year".
- **Loading**: a centred spinner with 48dp padding.
- **Empty period**: empty message, title "Nothing played {this week | this month | this year | all time} yet", body "Any song you play for 30 seconds or more counts. Your top songs, artists and listening clock build up from there."

With data, top to bottom:

1. **Story banner** (`ui/WrappedScreen.kt:208-237`). 16dp side margin, 8dp vertical. Radius 12, fixed fill `#2A0E3D`, 20dp padding.
   - Headline in `displaySmall` extra-bold, `#FFB627`. The text depends on the period (table below).
   - 6dp, then "{minutes} minutes of music, told in {n} cards you can share." in `bodyMedium` at white 85%.
   - 16dp, then a fully rounded pill filled `#FF5D8F`: a `PlayArrow` icon and "Play your story" in `labelLarge`, both in `#2A0E3D`. Padding start 12, end 18, vertical 9.
   - Tapping anywhere on the banner opens the story at the first card. Press scale 0.98.

2. **Stat tiles** (`ui/WrappedScreen.kt:152-167`). A 2×3 grid, 8dp gaps, 16dp side padding. Each tile has radius 8, `surfaceContainer` fill, padding 14 horizontal and 12 vertical, the value in `headlineSmall` and the label below in `bodySmall` `onSurfaceVariant`.

   | Left | Right |
   |---|---|
   | "Minutes" | "Plays" |
   | "Songs" | "Artists" |
   | "Days with music" | "Longest streak" (value "1 day" or "{n} days") |

3. **"Share a card"** section title, then a horizontal row of card thumbnails: each 118dp wide at 9:16, radius 8, 10dp apart. Tapping one opens the story at that card.

4. **"Top songs"** (`ui/WrappedScreen.kt:252-294`). The section title with a 56dp green play circle at the right. Up to 10 rows (padding 16 horizontal, 6 vertical): rank in `titleMedium` `onSurfaceVariant` (30dp wide), 48dp cover with 4dp radius, 12dp gap, title in `titleMedium` (green on the current song) over artist in `bodyMedium` `onSurfaceVariant`, and at the right "{n} plays" or "1 play" in `bodySmall`. Tap plays the list from that song, context "Your top songs". Long-press opens the track menu. The play circle plays from the top, or toggles pause when this list is already playing.

5. **"Top artists"** (`ui/WrappedScreen.kt:296-317`). Up to 10 rows in the same layout with a 48dp circular cover, the name in `titleMedium`, and "{n} min" at the right. Tap opens the artist. Each artist gets a cover no higher-ranked artist already uses.

6. **"Languages"** (`ui/WrappedScreen.kt:319-345`). Shown when any listened song has a known language. A horizontal stacked bar 18dp tall with fully rounded ends, one segment per language sized by share and coloured per language. 12dp below, a legend row per language: a 10dp dot, 10dp gap, the language name in `bodyLarge`, and "{n}%" in `bodyMedium` `onSurfaceVariant`.

7. **"When you listen"** (`ui/WrappedScreen.kt:347-367`). A 132dp listening clock at the left, 18dp gap, then the listener type in `titleLarge` and, 4dp below, "{type line} Your busiest hour starts at {hour}." in `bodyMedium` `onSurfaceVariant`. The hour reads "midnight", "noon", "7 am" or "9 pm".

8. **Footnote** in `bodySmall` `onSurfaceVariant`, 28dp above: "Counted from Attic's listening log, which began on {d MMMM yyyy}. A song counts as a play after 30 seconds."

**Listening clock** (`ui/WrappedCards.kt:454-471`). Twenty-four round-capped bars radiating from a centre ring, midnight at the top, going clockwise. Each bar's length is that hour's listening relative to the busiest hour. Every hour has a faint track bar behind it. On the page the bars are `primary` on `surfaceContainerHighest`.

**Listener types** (`wrapped/Recap.kt:82-99`)

| Title | Line |
|---|---|
| "Night owl" | "Most of your listening happens after dark." |
| "Early riser" | "You start the day with music." |
| "Daytime listener" | "Music keeps your days going." |
| "Evening unwinder" | "Your evenings have a soundtrack." |

**Period wording** (`ui/WrappedCards.kt:605-618`, `260-265`)

| Period | Headline (banner, Home card, summary card) | Period label (card footer) | Intro card line |
|---|---|---|---|
| This week | "Your week in music" | "Week of {d MMM}" (Monday) | "Your week in music" |
| This month | "Your {Month} in music" | "{Month} {year}" | "Your month in music" |
| This year | "Your {year} so far"; from 24 Dec "Your {year} Wrapped" | "{year} so far"; from 24 Dec "{year}" | "Your year so far" |
| All time | "Everything you've played" | "All time" | "Everything you've played" |

#### Wrapped story

`ui/WrappedScreen.kt:371-486`. A full-screen dialog on solid black, covering the mini player and tab bar.

- **Top**: one progress segment per card, padding 12 horizontal and 10 vertical, 4dp apart, each 3dp tall with rounded ends, white on white 30%. Passed cards are full, the current one fills linearly.
- **Middle**: the current card, centred, 9:16, radius 10, as large as fits with 12dp side margins.
- **Bottom** (padding 16 horizontal, 12 vertical): pill buttons, each an icon at 20dp, 6dp gap and a `labelLarge` label, padding start 14, end 18, vertical 10. Press scale 0.94.
  - Left: `Close`, "Close". White 14% fill, white content.
  - Right: `Download`, "Save". White 14% fill, white content.
  - Far right: `IosShare`, "Share". White fill, black content.
- **Timing**: each card stays 7 seconds and then advances. The last card stays.
- **Tap the left third** of the card: previous card. **Tap elsewhere**: next card, or close on the last card.
- **Press and hold**: pauses the timer until released.
- The timer also pauses while an image is being exported and while the app is behind the share sheet.
- **Save** writes the current card to Pictures/Attic. Snackbar "Saved to Pictures/Attic" or "Couldn't save the image".
- **Share** hands the current card to the system share sheet as a PNG.
- Back, or "Close", dismisses.

#### Wrapped cards

`ui/WrappedCards.kt`. Every card is laid out on a fixed 360×640dp canvas and scaled, so the story, the thumbnails and the exported image are identical. Cards ignore the phone's font-size setting. **Card colours are fixed and do not follow the app theme or the cover art.**

Card palette: Marigold `#FFB627`, Plum `#2A0E3D`, Pink `#FF5D8F`, Cobalt `#2B59FF`, Mint `#9FF0C9`, white, black.

Card type: Figtree. Display text is extra-bold with −0.04em tracking and a line height of 0.92 of the size.

**Footer on every card** (`ui/WrappedCards.kt:196-217`): padding 24 horizontal, 20 vertical. A 22dp black circle holding the green Attic mark at 12dp, 8dp gap, "Attic" in bold 14sp, and at the right the period label in medium 13sp at 75% opacity. Both in the card's ink colour.

Cards appear in this order. A card is skipped when its data is missing.

| # | Card | Shown when | Background / ink |
|---|---|---|---|
| 1 | Intro | always | Plum / white |
| 2 | Minutes | always | Cobalt / white |
| 3 | Top song | at least 1 top song | Marigold / Plum |
| 4 | Top songs | at least 3 top songs | white / Plum |
| 5 | Top artist | at least 1 top artist | Pink / Plum |
| 6 | Top artists | at least 3 top artists | Plum / white |
| 7 | Languages | any known language | Mint / Plum |
| 8 | Listening clock | always | black / white |
| 9 | Highlights | any listening day | Cobalt / white |
| 10 | Summary | always | Plum / white |

1. **Intro.** Top-left, 24dp side padding, 56dp from the top: the intro line in 54sp display, white. 18dp below: "{minutes} minutes across {songs} songs. Here's what you played." in medium 18sp at white 85%. At the bottom-left, one huge Marigold word that runs off the card edge: the four-digit year at 148sp, the first three letters of the month at 190sp, "Week" at 148sp, or "All" at 190sp.

2. **Minutes.** A Mint circle, radius 62% of the card width, centred just outside the top-right corner. From 210dp down: "You listened for" (semibold 17sp), the minute count in 116sp display (92sp when it has more than five characters), "minutes" in 44sp display Mint, then 24dp below "That's about {n} hours across {d} days." or, under an hour, "Just getting started across {d} days." in medium 18sp ("hour" and "day" in the singular for 1). In the story the number counts up from zero over 1.6 s.

3. **Top song.** "Your top song" label. 22dp below, the cover at 86% width, square, radius 4, **rotated −4°**. 30dp below, the title in 40sp display, up to three lines. The artist in semibold 18sp at 80%. A Plum pill with Marigold bold 15sp text: "Played once" or "Played {n} times".

4. **Top songs.** Headline on three lines, "Songs you / kept coming / back to", in 46sp display. 30dp below, five ranked rows 16dp apart: rank in 40sp display Pink (38dp wide), a 58dp cover with 3dp radius, title in bold 18sp up to two lines over artist in medium 14sp at 70%, and "{n}×" in bold 15sp Pink at the right.

5. **Top artist.** A 330dp circular cover placed at the top-right and pushed 70dp off the right edge, 60dp from the top. Bottom-left, 84dp above the bottom: "Your top artist" label, the name in 64sp display (50sp when longer than 14 characters), up to three lines, then "{minutes} minutes over {n} plays. Number one of the {k} artists you played." in medium 17sp.

6. **Top artists.** Headline on two lines, "The artists / on repeat", in 50sp display Mint. Five ranked rows as in card 4, with circular covers, Mint ranks, white names and "{n} minutes" as the detail. No trailing value.

7. **Languages.** "What you listened in" label. The top language's share as "{n}%" in 128sp display, its name in 48sp display. 30dp below, up to five horizontal bands 10dp apart: each 34dp tall with a square left end and a fully rounded right end, width 12% plus 62% × share of the card width, coloured per language, followed by "{Language} {n}%" in bold 15sp.

8. **Listening clock.** "Your listening clock" label in Marigold. The clock at full width, Marigold bars on `#2A2A2A` tracks. The listener type in 52sp display white. "{type line} Your busiest hour starts at {hour}." in medium 17sp at white 80%.

9. **Highlights.** "The good bits" in 52sp display Marigold. Up to four items 26dp apart, each a value in 44sp display white over a line in medium 17sp at white 85%. Candidates in order:
   - "{n} min" / "on {Weekday d Month}, your biggest day"
   - "{n}×" / "{song title} in a single day" (a top song played three or more times in one day)
   - "{n} days" / "in a row with music" (streak of two or more)
   - "{n}" / "song played for the first time" or "songs played for the first time"
   - "{n}%" / "of your listening picked by radio" (5% or more)

10. **Summary.** A 176dp cover of the top song with 4dp radius, and beside it the headline in 34sp display Marigold. 34dp below, two columns: "Top artists" and "Top songs", each a medium 14sp label at 70% over up to five numbered names in bold 17sp ("1  {name}"). 34dp below, two stats: "Minutes" and "Mostly in" (the top language, or the listener type when no language is known), value in 40sp display. 40dp below, a cassette ornament: a Pink reel, a Pink 50% bar 6dp tall, a Mint reel. Each reel is a 34dp ring with three spokes.

**Language colours** (`ui/WrappedCards.kt:119-130`), shared by the page and the cards:

| Language | Colour |
|---|---|
| Tamil | `#FFB627` |
| Hindi | `#FF5D8F` |
| English | `#2B59FF` |
| Telugu | `#7A3CFF` |
| Malayalam | `#00A86B` |
| Kannada | `#FF7A1A` |
| No vocals | `#2A0E3D` |
| anything else | `#8C8C8C` |

#### Share image

`ui/ShareImage.kt`. The card on screen is captured and scaled to **1080×1920 PNG**, the story size Instagram and WhatsApp status show uncropped. The file is named `attic-wrapped-{card}-{timestamp}.png`, where `{card}` is one of `intro`, `minutes`, `top-song`, `top-songs`, `top-artist`, `top-artists`, `languages`, `listening-clock`, `highlights`, `summary`. The image contains only the card: no progress bars or buttons.

### 2.17 Snackbar messages

Purpose: short confirmations and playback or radio errors.

`ui/AppRoot.kt:200-205`, `290-302`. A stock Material snackbar in inverse colours (white with black text in dark mode, `#121212` with white text in light). Text only, no action button. A new message replaces the one showing. It sits just above the mini player and tab bar, or 48dp from the bottom when a full-screen overlay is open, and draws above those overlays.

| Message | Trigger |
|---|---|
| "Playing next" | Track menu "Play next" |
| "Added to queue" | Track menu "Add to queue" |
| "Added to {playlist}" | Songs added to a playlist |
| "Already in {playlist}" | Every chosen song was already there |
| "No lyrics found for {title}" | Single-song lyrics download found nothing |
| "Connect to the internet to download lyrics" | Single-song lyrics download failed |
| "Saved to Pictures/Attic" | Wrapped card saved |
| "Couldn't save the image" | Wrapped card save failed |
| "Radio couldn't read this song" | Radio could not analyse its seed song |
| "Radio is using {n} of {total} songs until analysis finishes" | Radio started before the whole library was analysed |
| "Added to queue" | A song row swiped to queue it (added 2026-11-23, section 2.0) |
| "Radio can start from this genre once its songs are analysed" | Genre radio with none of the genre's songs analysed (added 2026-11-23) |
| "Mood radio can start once songs have been analysed" | Mood radio with no song scored for moods yet (added 2026-11-25) |
| "Skipped {title} because it can't be played" | A queued file failed to play and was dropped |
| "Music will pause in {n} minutes" | Sleep timer set to 15, 30, 45 or 60 minutes (added 2026-11-23) |
| "Music will pause after this song" | Sleep timer set to "End of this song" (added 2026-11-23) |
| "Sleep timer off" | Sleep timer set to "Off" (added 2026-11-23) |
| "Couldn't share this song" | "Share" found no app to take the file (added 2026-11-23) |
| "Couldn't open this file" | Attic was asked to open something it can't play, such as a web link, or reading it failed (added 2026-11-23) |
| "Backup saved" | Settings "Back up to a file" finished (added 2026-11-23) |
| "Couldn't save the backup" | Writing the backup file failed (added 2026-11-23) |
| "Smart shuffle has nothing to add yet, so this is a plain shuffle" | Smart shuffle found no song in the library that fits the collection, so it shuffled the collection alone (added 2026-11-23) |

Liking a song, removing from a playlist, clearing the queue, and renaming or deleting a playlist show no message.

### 2.18 First run, permission, loading and empty states

**First run** (`ui/AppRoot.kt` `FirstRun`, state in `ui/model/OnboardingModel.kt`, drawn by `ui/expressive/ExpressiveOnboarding.kt` and `ui/comfort/ComfortOnboarding.kt`). Shown once per install, instead of the whole app, before the shell. Four steps. Each style draws them from its own kit: a bar with a back button (from step 2) and four step pips at the right, the step's content, and one full-width filled button at the bottom edge. Expressive start-aligns an emphasised headline as its permission screen did; Comfort centres a 30/36 bold title. Steps slide in from the side.

| Step | Shows | Button |
|---|---|---|
| 1. Welcome | Expressive: a 300dp surface-container card holding an arch with the Headroom mark, a clover with a folder and a pill with an aeroplane. Comfort: the mark in a 112dp tonal superellipse. Then "Welcome to Attic" and "Attic plays the music files on this phone. There's no account, and everything works offline." | "Get started" |
| 2. Pick your look | "Pick your look", "You can change it any time in Settings." Two selectable cards side by side, "Expressive" and "Comfort", each with a radio and a live picture of that style's Home title, filters ("All", "Albums", "Artists") and tab bar, drawn in that style's own theme at 42% size. The selected card is ringed in the accent. Then "Theme": "Match phone", "Dark", "Light", as Settings draws them (a connected group in Expressive, radio rows in a card in Comfort). Selecting either switches the app at once, through the same settings as Settings > Appearance, so the first run itself redraws. The style and theme start on the phone's default style and the app's default theme ("Dark"). | "Continue" |
| 3. Access | "Let Attic find your music", then two rows in a group, each an icon, a title and a reason: "Music and audio", "Required. Attic needs it to find the songs on this phone."; "Notifications", "Optional. Shows the playback controls in the notification shade and on the lock screen." A granted row ends in a tick and "Allowed". Notifications not yet asked ends in a tonal "Allow", which asks for notifications alone; refused, it ends in "Off" and nothing waits on it. Refused audio ends in "Off" in the error colour, and the screen adds "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions." | "Allow access" (the system prompt for audio only); once refused, "Open app permissions" (the system settings page for the app); once granted, "Continue" |
| 4. Your library | While the scan runs: the loading indicator (Expressive, contained) or the spinner (Comfort) over "Finding your songs…". Then "Found {n} songs" and an "Optional" group: a switch "Download lyrics for all songs" with "Downloading sends each song's title, artist, album and length to lrclib.net. Your music files are never sent.", off by default; and "Radio analyses your songs while the phone charges. It all happens on this phone." with a text button "Song radio settings". With no songs: the empty-library state ("No songs on this phone", "Copy music files onto this phone, in Music or any other folder, and they will show up here.") and "Connect the phone to a computer and copy music into the Music folder. Songs show up here as they arrive." The count updates as files arrive. | "Start listening" |

- "Start listening" ends the first run and opens Home. If the lyrics switch is on and there are songs, it starts the same whole-library download as Settings. It works during the scan too, which skips the last step; the loading spinner shows until the scan ends. "Song radio settings" ends the first run the same way and opens Settings.
- Back goes to the previous step. Back on Welcome leaves the app.
- The step, whether each permission was asked, and the lyrics switch are saved state, so rotation and process death return to the same step.
- **Who sees it** (`data/Onboarding.kt`). The decision is made once and saved as `onboarding_done` in settings.xml. With no saved answer, the install counts as already onboarded when settings.xml held any value at launch, or the database holds any like, playlist, play, saved queue, lyrics, analysis or language row. An install from before the first run existed may have data and no settings.xml, so the database check is what keeps it out. A first run that has not finished saves `false`, so picking a look (which writes settings) and then losing the process still resumes the first run.
- Debug builds show the steps with sample state: `adb shell am start -n app.attic.player/.debug.KitGalleryActivity --es screen onboarding --ei step 2 --es style comfort` (extras in `OnboardingGallery.kt`).

**Permission screen** (`ui/expressive/ExpressiveStates.kt`, `ui/comfort/ComfortStates.kt`). After the first run, shown instead of the whole app if audio access is missing, for example after it was turned off in system settings. Title "Attic plays the music on this phone", one sentence, and one full-width button at the bottom edge. Expressive heads it with a 320dp card of shapes (an arch with the mark, a clover with a note that turns grey and crossed out once refused, a pill with a bell) and start-aligns the text; Comfort centres the mark in a 96dp tonal superellipse above centred text.

| State | Body | Button |
|---|---|---|
| Not asked yet | "Allow audio access so it can find your songs in any folder. Notifications show the playback controls." | "Allow access" (opens the system permission prompts for audio and notifications) |
| Asked and refused | "Audio access is off, so Attic can't see your songs. Turn on Music and audio in the app's permissions." | "Open app permissions" (opens the system settings page for the app) |

**Loading** (`ExpressiveLoading`, `ComfortLoading`). A centred progress indicator on the background colour while a scan runs after the first run, on each cold start. No text. The first scan on a new install is shown by the first run's last step instead.

**Splash**. Android's system splash: `#121212` with the green Attic mark.

**Every empty and error string**

| Where | Title | Body |
|---|---|---|
| Home, empty library | "No songs on this phone" | "Copy music files onto this phone, in Music or any other folder, and they will show up here." |
| Search, no matches | `No results for "{query}"` | "Check the spelling, or try fewer words." |
| Liked Songs, empty | "Songs you like will appear here" | "Tap the plus on a song to save it." |
| Playlist, empty | "Let's find something for your playlist" | "Tap the plus above to add songs from your library." |
| Album / artist / playlist missing | "{This album \| This artist \| This playlist} is gone" | "It may have been removed from this phone." |
| Wrapped, empty period | "Nothing played {period} yet" | "Any song you play for 30 seconds or more counts. Your top songs, artists and listening clock build up from there." |
| Queue, nothing next | heading "Nothing up next" | none |

Search and Library have no dedicated empty-library layout. With no songs their grids and lists are simply empty.

### 2.19 Home-screen widget

`widget/PlayerWidget.kt`, `layout/widget_small.xml`, `layout/widget_wide.xml`, `layout/widget_large.xml`. One widget that changes layout as it is resized. Default 4×2 cells, minimum 110×50dp, resizable both ways (`xml/widget_player.xml`). Picker description: "Your song and controls, with your recent albums. Resize it for a smaller player."

Common to all sizes:
- **The background is the current cover's tint**, with 24dp corners. Fallback `#333333`.
- Text uses the system font, not Figtree.
- With no cover the art slot shows `#333333` with the green Attic mark.
- Tapping anywhere that is not a button opens the app.
- With no song: title "Attic", second line "Tap to open".
- The widget keeps the last song across reboots and shows it paused.

| Size | Layout |
|---|---|
| **Small**, about 2×2 | The cover fills the widget. A 44dp round button in the bottom-right corner, 8dp from the edges, black 80% with a white play or pause glyph. |
| **Wide**, about 4×1 | A 72dp-wide cover filling the height at the left. 12dp gap. Title in bold 15sp white over artist in 13sp `#B3B3B3`, one line each. Three 44dp buttons at the right with white glyphs on no background: previous, play/pause, next. |
| **Large**, 4×2 and up | Top area (padding 14 horizontal, 10 vertical): an 84dp cover with 6dp radius, 14dp gap, title in bold 16sp white, artist in 13sp at white 80%, then a control row 4dp below: previous (44dp, white glyph), play/pause (a 38dp white circle with a black glyph, 12dp side margins), next (44dp). A 22dp white Attic mark in the top-right corner. Bottom strip: a black 15% band, padding 10 horizontal and 8 vertical, holding five album tiles of equal width, 44dp tall, 6dp radius, 4dp side margins. |

The five tiles are the most recently played albums, topped up with the albums that have the most songs. Tapping one opens that album in the app.

Button descriptions: "Play or pause", "Next", "Previous", "Open album".

`widget.png` and `docs/screenshots/widget.jpg` show the large size.

### 2.20 Media notification and lock screen

Purpose: control playback with the app closed or the phone locked.

`playback/PlaybackService.kt:118-136`. Android draws this, not the app. The app supplies:
- Title, artist and album of the current song. The player reads the cover from the file. Since 2026-11-23 each song also carries a cover URI from `playback/ArtProvider.kt`, for car screens; the card uses it only until the file's own picture has been read.
- The status-bar icon: the Attic mark in white (`drawable/media3_notification_small_icon.xml`).
- Standard transport: previous, play/pause, next, and the seek position.
- Two extra buttons: **shuffle** ("Turn shuffle on" / "Turn shuffle off") and **like** (plus-in-circle "Add to liked songs" / filled check-in-circle "Remove from liked songs"). Android Auto shows the same two on its now-playing screen.
- Tapping the notification opens the app.

In `notification.png` and `lock_screen.png` this renders as the system media card: cover as the background, the Attic mark top-left, a "This phone" output chip, title and artist, a rounded play/pause button, and a bottom row of previous, seek line, next, shuffle and like.

No notification appears on launch. It appears once playback starts.

The sleep timer (section 2.13) adds nothing to the notification or the lock screen. Nor does a crossfade: the card moves to the next song when the fade starts, a few seconds before the outgoing song ends.

**Analysis notification** (`analysis/AnalysisWorker.kt:105-128`). While radio analysis runs: an ongoing silent notification on the "Song analysis" channel with an equaliser-bars icon, title "Analysing your songs", text "{done} of {total}", a determinate progress bar, and one action, "Stop". Tapping it opens the app.

### 2.21 Opening a file from another app

Added 2026-11-23. Purpose: play an audio file from a file manager, a downloads list or any app that offers "Open with".

`AndroidManifest.xml` lists Attic for `ACTION_VIEW` on `audio/*` (and `application/ogg`, `application/x-flac`, which some file managers use) from `content://` and `file://` URIs. `playback/OpenWith.kt` handles the intent and `data/OpenWithResolver.kt` works out which song it is.

- **The file is in the library.** Attic finds it by its MediaStore id (MediaStore URIs, the media documents provider, `msf:` ids from the downloads list) or by its path on a volume (`file://`, `raw:` downloads, the storage documents provider, and file managers whose URIs contain the real path). It plays in its album from that song, or among all songs ("All songs") when it has no album, which is what tapping it in the app does. This replaces the queue.
- **The file is not in the library** (a ringtone, a voice note, a file in a folder the library leaves out, or a URI that gives neither an id nor a path). It is played once, from the URI: it goes in right after the current song and starts, so the rest of the queue and its "Playing from" stay as they were and carry on after it. Opening the same file again while it plays restarts it. Its title, artist and album come from its tags, else from the file name without the extension, else "Audio file". It has no cover. It is never saved: not to the library, the saved queue, listening history, Recap, radio, favourites or playlists. If Attic is closed while it plays, the saved queue resumes on the next song.
- In both cases Now Playing opens on the song.
- A URI that isn't `content://` or `file://`, or one that fails to read, shows the snackbar "Couldn't open this file". Nothing crashes.
- A relaunch from Recents or a recreated screen does not play the file again.

### 2.22 Android Auto

Added 2026-11-23. Purpose: play the library from a car screen. The car draws everything from the playback service's browse tree (`playback/BrowseTree.kt`, `playback/CarBrowser.kt`); Attic has no car layouts of its own. `docs/play/ANDROID-AUTO.md` has the guideline check and the head-unit test steps.

- **Tabs**, each with a white outline icon (`drawable/ic_auto_*.xml`): "Home", "Playlists", "Albums", "Artists". A car that allows five tabs also gets "Favourites" between Home and Playlists; Android Auto allows four.
- **Home**: "Shuffle all songs" with the song count, then the Instant mixes under an "Instant mixes" heading (song title over artist, with its cover; tapping one starts radio from it), then up to 12 songs under "Recently played".
- **Playlists**: "Favourites" with its count (when it isn't a tab), your playlists that have songs, then folder playlists, each with "{n} songs" and the first song's cover.
- **Albums**: a grid sorted by name, album over artist. **Artists**: a grid sorted by name, with "{n} songs". Both open a list of songs.
- **Songs** are rows of title over artist (over album, on an artist's list), with covers. Tapping one plays its list from there under the app's own "Playing from" name: the album or artist name, the playlist or folder name, "Favourites", "Recently played", "Search results" or "All songs".
- **Search** groups results under "Songs", "Artists", "Albums" and "Playlists".
- **Voice**: "Play X on Attic" plays a matching playlist, artist, album or song; "play music" carries on with the queue. When nothing matches, the car shows "Couldn't find “{query}” in your music". The same phrase said to the phone opens Attic and plays, and the miss shows as a snackbar.
- **No audio access**: the car shows "Attic can't see your music yet. When it's safe, open Attic on your phone and allow access to music." Cars that run Android themselves add an "Open Attic" button.

---

## 3. Visual tokens

### 3.1 Colour scheme

`ui/Theme.kt:63-108`. Roles not listed are left at Material 3's baseline defaults and are not used directly by the app's own layouts.

| Role | Dark | Light | Typical use |
|---|---|---|---|
| `primary`, `secondary` | `#1ED760` | `#12A84A` | Accent: selected chip, play circle, current-song title, playing bars, downloaded badge, liked check, active shuffle/repeat, Material buttons and controls |
| `onPrimary`, `onSecondary` | `#000000` | `#FFFFFF` | Glyph on the play circle, text on a selected chip |
| `background`, `surface` | `#121212` | `#FFFFFF` | Every screen background |
| `onBackground`, `onSurface` | `#FFFFFF` | `#121212` | Primary text and icons |
| `surfaceVariant` | `#282828` | `#EDEDED` | (Material component internals) |
| `onSurfaceVariant` | `#B3B3B3` | `#5E5E5E` | Secondary text, unselected tabs, row kebabs, sheet icons |
| `surfaceContainerLowest` | `#000000` | `#FFFFFF` | |
| `surfaceContainerLow` | `#181818` | `#F7F7F7` | |
| `surfaceContainer` | `#282828` | `#F0F0F0` | Shortcut tiles, stat tiles, sheets, dialogs |
| `surfaceContainerHigh` | `#2A2A2A` | `#EBEBEB` | |
| `surfaceContainerHighest` | `#333333` | `#E2E2E2` | Unselected chips, settings button, cover placeholder, dragged row, swipe-to-remove strip, clock tracks |
| `outline` | `#727272` | `#8A8A8A` | Search placeholder |
| `outlineVariant` | `#333333` | `#E2E2E2` | Sheet divider |
| `inverseSurface` | `#FFFFFF` | `#121212` | Search field, snackbar |
| `inverseOnSurface` | `#000000` | `#FFFFFF` | Search field text, snackbar text |

**Fixed colours outside the scheme**

| Colour | Use |
|---|---|
| `#4A17E0` → `#C4EFD9` | Liked Songs tile gradient |
| `#282828` | "About the artist" and "Credits" cards in Now Playing |
| `#333333` | Fallback tint when a cover has no usable colour; widget placeholder and tile background |
| `#8D67AB` `#E13300` `#1E3264` `#148A08` `#BA5D07` `#503750` `#0D73EC` `#AF2896` | Search browse tiles |
| `#2A0E3D` `#FFB627` `#FF5D8F` `#2B59FF` `#9FF0C9` | Wrapped palette (Plum, Marigold, Pink, Cobalt, Mint) |
| white at 10 / 20 / 25 / 30 / 55 / 70 / 75 / 78 / 80 / 85% | Text and lines over tinted surfaces |
| black at 35 / 55 / 72 / 75 / 80% | Scrims and unsung lyric lines |
| `#1ED760` on `#121212` | Launcher icon, splash, `docs/logo.svg` |

**Theme setting.** "Match phone", "Dark" (default) or "Light". Now Playing, full-screen Lyrics, the mini player, the widget, Wrapped cards, the Wrapped banner and Home card, and Speed dial tile text do not change with the theme.

**What changes in light mode** (see `light_home.jpg`, `light_album.jpg`, `light_mode_emulator.png`)
- Backgrounds are white, text is `#121212`, greys come from the light column above.
- The accent darkens to `#12A84A`, and glyphs on green (play circle, selected chip text) become white.
- The arrow inside the downloaded badge becomes white.
- The search field flips to `#121212` with white text.
- Collection header gradients become a pale wash of the cover colour, with dark titles.
- Queue, Settings, the Wrapped page, sheets, dialogs and the snackbar follow the light scheme.
- The artist hero keeps its white name over a dark fade.
- Status-bar and navigation-bar icons turn dark.

### 3.2 Type

Font: **Figtree**, one variable font file (`font/figtree.ttf`), used at weights 400, 500, 600, 700 and 800. `ui/Theme.kt:24-61`.

Headings (`displaySmall`, `headlineSmall`, `titleLarge`) are bold with −0.02em letter spacing. All other styles have default spacing.

| Style | Size / line height (sp) | Weight | Used for |
|---|---|---|---|
| `displaySmall` | 36 / 40 | Bold | Wrapped banner headline (set extra-bold). Base for the artist hero name, overridden to 44 / 48. |
| `headlineSmall` | 26 / 31 | Bold | "Search", "Your Library", collection titles, Now Playing song title, stat tile values, permission title |
| `titleLarge` | 22 / 27 | Bold | Section titles, "Settings" / "Wrapped" / "Queue" headers, sheet and dialog titles, empty-message titles, Wrapped Home-card headline, artist name in spotlight and About card, listener type |
| `titleMedium` | 16 / 21 | Medium | Song titles in rows, result-row titles, collapsed bar title, "Start browsing", browse tile title (set bold), sticky header title, "Credits", Wrapped rank numbers |
| `titleSmall` | 14 / 18 | SemiBold | Shortcut tile titles, shelf and grid card titles, artist names under circles, mini player title, "Playing from" context, album artist line, album footer, "Lyrics" and "About the artist" labels, inline lyric line, lyrics header title, queue headings |
| `bodyLarge` | 16 / 22 | Regular | Now Playing artist, sheet rows, settings option labels, credit values, permission body, language legend |
| `bodyMedium` | 14 / 19 | Regular | Row artists and subtitles, chips, empty-message bodies, settings paragraphs, collection subtitles, "Picked at random from your songs" |
| `bodySmall` | 12 / 16 | Regular | Shelf subtitles, collection detail line, mini player artist, settings small print, stat tile labels, play counts, "More from", credit labels |
| `labelLarge` | 14 / 18 | SemiBold | Speed dial titles, "Recents" and sort labels, "Show lyrics", "Remove", story buttons, "Play your story", Material button labels |
| `labelMedium` | 12 / 15 | Medium | Seek bar times, "Downloaded" |
| `labelSmall` | 11 / 14 | Medium | Tab bar labels, "Playing from" |

Sizes set directly, outside the scale:
- Lyrics card lines: 22 / 28 bold.
- Full-screen synced lyrics: 26 / 33 bold. Plain lyrics: 24 / 33 bold.
- Wrapped cards: display sizes 34, 40, 44, 46, 48, 50, 52, 54, 64, 92, 116, 128, 148 and 190sp extra-bold; text sizes 13, 14, 15, 17 and 18sp.
- Widget: 16 / 15sp bold titles and 13sp artists in the system font.

### 3.3 Shapes

| Radius | Used on |
|---|---|
| 0 | Artist hero image; images inside shortcut tiles and mosaics |
| 3dp | Covers inside Wrapped ranked rows |
| 4dp | Covers in rows, the mini player, sheets and collection headers (`CoverShape`); shortcut tiles; Liked tile; Wrapped card covers |
| 6dp | Mini player; search field; browse tiles; Speed dial tiles; widget cover and album tiles |
| 8dp | Shelf card and folder card covers, Now Playing cover (`BigCoverShape`); stat tiles; Wrapped thumbnails; Add-songs search field |
| 10dp | Now Playing cards (Lyrics, About the artist, Credits); Wrapped Home card; story card |
| 12dp | Wrapped story banner |
| 24dp | Widget background |
| full circle / pill | Chips, play circle, settings button, artist images, downloaded badge, progress lines, "Show lyrics", story buttons, "Play your story", language bar |
| Material default | Bottom sheets, dialogs, dropdown, snackbar, Material buttons |

Squares hold album art and circles hold artists throughout.

### 3.4 Elevation and surfaces

The app's own layouts are flat. There are no shadows, borders, outlines or dividers between rows. Separation comes from spacing, from the grey `surfaceContainer` steps, and from tinted fills. The only divider is the one under the track menu header. Bottom sheets, dialogs, the dropdown and the snackbar carry whatever elevation and scrim Material 3 gives them by default.

### 3.5 Icons

All icons are Material Icons, **Rounded** style, at 24dp unless noted. The two exceptions are the unselected Home and Library tab icons, which are **Outlined**.

| Icon | Where |
|---|---|
| `Home` (Outlined / Rounded) | Home tab |
| `Search` | Search tab, search field, Library header, Add-songs field |
| `LibraryBooks` (Outlined / Rounded) | Your Library tab |
| `MusicNote` | Placeholder for missing covers |
| `ArrowBack` | Back button |
| `KeyboardArrowDown` | Close Now Playing, Queue, Lyrics |
| `MoreVert` | Row kebab, Now Playing header, playlist options |
| `PlayArrow` / `Pause` | Play circle, mini player, sticky header, "Play your story" |
| `SkipPrevious` / `SkipNext` | Now Playing transport |
| `Shuffle` | Now Playing, collection control row, "Shuffle all songs" |
| `Repeat` / `RepeatOne` | Now Playing |
| `QueueMusic` | Open queue, "Add to queue", playlist rows in the picker |
| `AddCircleOutline` / `CheckCircle` | Like button; collection "add to a playlist" |
| `Favorite` | Liked tile heart; liked state in the track menu |
| `FavoriteBorder` | Not-liked state in the track menu |
| `ArrowDownward` | Inside the downloaded badge |
| `Add` | Create playlist, "Add songs", "New playlist", add in Add-songs sheet |
| `Check` | Done reordering, already-in-playlist, added in Add-songs sheet, current language |
| `Close` | Clear search, story "Close" |
| `Sort` | Library sort row |
| `GridView` / `ViewList` | Library view toggle |
| `SwapVert` | Reorder playlist |
| `DragHandle` | Reorder handle in the queue and playlists |
| `Edit` / `Delete` | Playlist dropdown |
| `Radio` | "Start radio" |
| `PlaylistPlay` | "Play next" |
| `PlaylistAdd` | "Add to playlist" |
| `Language` | "Language: …" and language choices |
| `RemoveCircleOutline` | "Remove from this playlist" |
| `Album` | "Go to album" |
| `PersonOutline` | "Go to artist" |
| `Download` / `IosShare` | Story "Save" / "Share" |

**The Attic mark** (`drawable/ic_launcher_foreground.xml`): a gabled house outline with a round window cut out, in `#1ED760`. Used on the settings button, the permission screen, Wrapped card footers, the widget, the status-bar icon (white) and the launcher icon (on `#121212`, with a monochrome variant).

### 3.6 Colour taken from album art

**How the tint is computed** (`ui/Art.kt:99-141`)

1. Load the cover at 96px.
2. Run AndroidX Palette and take the vibrant swatch, or the muted swatch, or the dominant swatch, in that order.
3. Convert to HSL. Cap saturation at 0.62. Clamp lightness to between 0.20 and 0.30. Keep the hue.
4. Cache per song. With no cover or no swatch, use `#333333`.

The result is always a dark, moderately saturated colour, so white text stays readable on it.

**Where the tint is used**

| Surface | Treatment |
|---|---|
| Mini player | Solid tint fill. Animates 400 ms. |
| Collection header | Vertical gradient, tint → background. In light mode the tint is first mixed at 40% over white. |
| Collection top bar, once collapsed | Solid tint fill (same light-mode softening). Animates 180 ms. |
| Now Playing background | Vertical gradient: tint at 0, black 72% over tint at 0.72, `#121212` at 1. Animates 600 ms. |
| Now Playing sticky header | Black 55% over tint. |
| Lyrics card | White 10% over tint. |
| Full-screen lyrics | Solid tint. Animates 600 ms. |
| Widget | Solid tint background. |

**Fixed scrims over art, not derived from it**

| Surface | Scrim |
|---|---|
| Artist hero | Transparent at 50% → black 72% at the bottom |
| Speed dial tile | Transparent at 45% → black 80% at the bottom |
| Back button over a header | Black 35% circle |
| Widget album strip | Black 15% band |
| Small widget play button | Black 80% circle |

The collection that supplies the tint is the first song of the album, artist, folder or playlist, or the first liked song.

### 3.7 Layout measures that recur

- Screen side margin: 16dp. Now Playing uses 24dp. Sheets use 20dp.
- Row vertical padding: 7dp. Wrapped list rows: 6dp.
- Cover sizes: 40 (mini player), 48 (sheet header, Wrapped rows), 50 (track row), 56 (result row, shortcut tile), 64 (library list row, Wrapped Home card), 130 (artist circle), 160 (shelf card), 62% of width (collection header), full width less 48dp (Now Playing).
- Grids: shortcuts 2 columns; browse tiles 2 columns; Speed dial 3 columns; library grid 3 columns; stat tiles 2 columns.
- Gaps: 8dp between chips, shortcut tiles, Speed dial tiles and stat tiles; 10dp between browse tiles; 14dp between shelf cards and between Now Playing cards.
- Section title spacing: 26dp above, 12dp below.
- Tab bar 58dp. Mini player slot 64dp. Play circles 56dp (collections, lyrics, Wrapped) and 66dp (Now Playing).
- Minimum touch targets are Material's 48dp icon buttons, except the 36dp settings button and the row-level press areas.

### 3.8 Motion

`ui/Motion.kt` holds the press and like springs. Other timings sit next to the elements they animate.

**Press feedback** (`ui/Motion.kt:31-54`). Scale to the target while pressed, spring back on release. Spring: medium-low stiffness, medium bounce (damping ratio 0.5). No ripple.

| Press scale | Elements |
|---|---|
| 0.985 | Track rows, result rows, mini player, settings rows, About-the-artist card, Wrapped list rows |
| 0.98 | Wrapped Home card and story banner, lyric lines |
| 0.97 (default) | Shortcut tiles, shelf cards, folder cards, artist circles, browse tiles, Speed dial tiles, grid tiles, artist line, sort label, card thumbnails |
| 0.94 | Chips, "Show lyrics", story buttons |
| 0.92 | Play circle |
| 0.9 | Tabs, settings button |

**Like pop** (`ui/Motion.kt:57-66`). On change the icon snaps to 60% and springs to 100% with damping ratio 0.4 and medium stiffness, so it overshoots.

**Navigation** (`ui/AppRoot.kt:319-327`)

| Transition | Spec |
|---|---|
| Tab → tab | Fade in 120 ms, fade out 120 ms |
| Open a detail screen | Slide in from the right by one sixth of the width over 220 ms plus fade in 180 ms. The screen underneath fades out in 120 ms. |
| Back from a detail screen | Slide out to the right by one sixth over 200 ms plus fade out 160 ms. The screen underneath fades in over 160 ms. |

**Overlays** (`ui/AppRoot.kt:233-280`)

| Element | In | Out |
|---|---|---|
| Mini player | Slide up + fade, 240 ms | Slide down + fade, 180 ms |
| Now Playing | Slide up from the bottom edge, 260 ms | Slide down, 220 ms |
| Queue | Slide up, 240 ms | Slide down, 200 ms |
| Lyrics | Slide up, 260 ms | Slide down, 220 ms |
| Now Playing sticky header | Fade 160 ms | Fade 120 ms |

All of these use Compose's default easing for `tween` (fast-out, slow-in). Bottom sheets, dialogs and the dropdown use Material's stock motion.

**Other animated properties**

| What | Spec |
|---|---|
| Tab icon and label colour | 140 ms |
| Collection top bar background | 180 ms |
| Mini player tint | 400 ms |
| Now Playing and Lyrics tint | 600 ms |
| Shuffle / repeat colour | 160 ms |
| Play ↔ pause glyph | New glyph scales from 70% and fades in over 140 ms; old one scales to 70% and fades out over 100 ms |
| Now Playing cover inset while paused | 0 ↔ 14dp over 260 ms |
| Seek handle while dragging | 6 ↔ 8dp radius over 120 ms |
| Playing bars | 420 / 560 / 340 ms, reversing, forever |
| Lyric line colour | 240 ms |
| Lyrics auto-scroll | Animated scroll to two lines above the current line |
| Mini player swipe return | 220 ms |
| Now Playing drag-to-dismiss | Follows the finger; default spring back |
| Artist marquee | Starts after 2.5 s |
| Cover images | 120 ms crossfade on load |
| Wrapped story progress | Linear, 7 s per card |
| Wrapped minutes count-up | 1.6 s, fast-out slow-in |
| Collapsed bar title | Appears instantly (no fade) |

---

## 4. Feature list the redesign must keep

Every user-facing capability and control in the current app. Exact names are in quotes.

**App structure**
- [ ] Root tabs "Home", "Search", "Your Library"
- [ ] Re-tapping the current tab returns to its root
- [ ] Persistent mini player on every destination, including Settings and Wrapped
- [ ] Settings entry on each tab root
- [ ] Portrait phone layout, edge to edge
- [ ] Themes: "Match phone", "Dark", "Light"
- [ ] Opening a destination from inside the player closes the player
- [ ] Light haptics on like, play / pause, reorder pick-up and drop, swipe to queue, sleep timer set and style change, following the system setting (added 2026-11-23)

**Library content types**
- [ ] Songs, albums (including one-song albums labelled "Single"), artists
- [ ] Folders shown as read-only playlists ("Playlist from a folder on this phone")
- [ ] User playlists
- [ ] "Liked Songs"
- [ ] Artist images are covers of the artist's songs, because there are no artist photos

**Home**
- [ ] Filter chips "All", "Albums", "Artists", "Folders"
- [ ] Shortcut grid of up to six items, "Liked Songs" always first
- [ ] Wrapped entry card with headline and minutes
- [ ] "Speed dial": up to nine songs, tap starts radio, long-press opens the menu
- [ ] "Recently played" shelf (up to 12 songs)
- [ ] "Mixes": three to five taste mixes named after their artists, the same all day, rebuilt daily (added 2026-11-23)
- [ ] "On repeat", "Haven't heard in a while" and "Newly added" shelves, each absent below four songs (added 2026-11-23)
- [ ] "Start listening": four random songs, labelled "Picked at random from your songs"
- [ ] "Your folders" shelf
- [ ] "Albums" shelf (14 biggest)
- [ ] "More from {artist}" spotlight with that artist's albums
- [ ] "Your top artists" shelf (14 biggest)
- [ ] Empty-library message

**Search**
- [ ] Live search across songs, artists, albums and folders, ignoring case and accents
- [ ] Placeholder "What do you want to listen to?" and a clear button
- [ ] Browse tiles for every folder and the eight biggest albums under "Start browsing"
- [ ] Result types labelled "Artist", "Playlist", "Album, {artist}"
- [ ] "Genres" in Browse, genres of five songs or more, and genre names in results labelled "Genre" (added 2026-11-23)
- [ ] No-results message

**Your Library**
- [ ] Filter chips "Playlists", "Albums", "Artists", "Downloaded", each toggling off
- [ ] List view and 3-column grid view toggle
- [ ] "Recents" ordering
- [ ] Shortcut to Search
- [ ] Create playlist
- [ ] "Downloaded" = all songs, with sort cycling "Title", "Artist", "Recently added"
- [ ] "Shuffle all songs"

**Collection screens (album, artist, folder, playlist, Liked Songs)**
- [ ] Header with cover, title, subtitle, and collapse-to-title on scroll
- [ ] Play / pause for the collection
- [ ] Shuffle play for the collection, with an "on" state
- [ ] Add the whole collection to a playlist
- [ ] Song count and total duration
- [ ] Album: artist link, "Album" / "Single" and year, tracks in track order, footer summary
- [ ] Artist: "Popular" top five with ranks, "Popular releases", "All songs"
- [ ] Liked Songs: newest first, empty state
- [ ] Playlist: add songs, reorder by drag, rename, delete with confirmation, remove a song, empty state
- [ ] Smart shuffle on playlists and Favourites, from the more menu, ticked while on (added 2026-11-23)
- [ ] "is gone" state for missing items
- [ ] Genre: songs, albums mostly in the genre, play, shuffle, genre radio, add to playlist (added 2026-11-23)
- [ ] Mood: the day's mix, play, shuffle, Mood radio (added 2026-11-25)
- [ ] "Moods" on Home and in Browse, mood names in results labelled "Mood", the picks card, and Moods and interests from Home and Settings (added 2026-11-25)

**Song rows and the track menu**
- [ ] Current-song highlight and animated playing indicator
- [ ] Tap to play in context, long-press or kebab for the menu
- [ ] Swipe a row towards the end to add the song to the queue, outside the queue and reorder mode (added 2026-11-23)
- [ ] "Start radio"
- [ ] "Add to liked songs" / "Remove from liked songs"
- [ ] "Play next"
- [ ] "Add to queue"
- [ ] "Add to playlist", including "New playlist" from the picker
- [ ] "Language: {name}" with choices "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada", "English", "No vocals", and "Unknown" as the unset state
- [ ] "Remove from this playlist" (in playlists)
- [ ] "Go to album"
- [ ] "Go to artist"
- [ ] "Share" (added 2026-11-23)
- [ ] "Sleep timer" from Now Playing's menu: 15, 30, 45 and 60 minutes, "End of this song", "Off"; time left shown on the row; fade, then pause (added 2026-11-23)

**Mini player**
- [ ] Cover, title, artist, progress
- [ ] Like toggle
- [ ] Play / pause
- [ ] Swipe to next / previous
- [ ] Tap to open Now Playing

**Now Playing**
- [ ] "Playing from" with the context name
- [ ] Close button and swipe-down to dismiss
- [ ] Track menu access
- [ ] Large cover, swipe sideways to change song
- [ ] Paused state visible on the cover
- [ ] Title, artist (tap opens artist), like toggle
- [ ] Seek bar: tap and drag, elapsed and total time
- [ ] Shuffle on / off
- [ ] Previous, play / pause, next
- [ ] Repeat off / all / one
- [ ] Open queue
- [ ] Inline current lyric line
- [ ] Lyrics card with every state in section 2.12
- [ ] "About the artist" card linking to the artist
- [ ] "Credits": "Performed by", "Album", "Year"
- [ ] Compact header with like and play / pause once scrolled

**Queue**
- [ ] "Now playing" and "Up next, {n} songs" / "Nothing up next"
- [ ] Tap to jump to a song
- [ ] Drag to reorder
- [ ] Swipe to remove
- [ ] "Clear"
- [ ] Track menu from queue rows
- [ ] Shuffle reflected as the real order
- [ ] Smart shuffle picks marked in the queue (added 2026-11-23)

**Lyrics**
- [ ] Lyrics read from `.lrc` files beside songs and from the songs' tags, with no source shown
- [ ] "Download lyrics" per song, with a busy state
- [ ] Synced lyrics that follow playback, auto-scroll, and seek on tap
- [ ] Sung vs upcoming line distinction
- [ ] Plain (unsynced) lyrics
- [ ] Instrumental and not-found states
- [ ] Full-screen lyrics with play / pause
- [ ] "Download lyrics for all songs" with progress, found count and failure messages
- [ ] Count of songs with saved lyrics
- [ ] Privacy note about lrclib.net

**Radio**
- [ ] Start radio from any song, and from Speed dial
- [ ] Radio context shown as "Radio · {song title}"
- [ ] Radio from a genre, shown as "Radio · {genre}" (added 2026-11-23)
- [ ] Mood radio, shown as "Radio · {mood}", and autoplay after a mood mix continuing in that mood (added 2026-11-25)
- [ ] Self-refilling queue of ten upcoming songs
- [ ] Removing an upcoming song steers radio; "Clear" stops it
- [ ] "Autoplay" setting (default on)
- [ ] Analysis progress and status in Settings
- [ ] "Analyse now without a charger"
- [ ] On-device privacy note
- [ ] Per-song language correction

**Wrapped**
- [ ] Periods "This week", "This month", "This year", "All time"
- [ ] Story banner with "Play your story"
- [ ] Stats: "Minutes", "Plays", "Songs", "Artists", "Days with music", "Longest streak"
- [ ] "Share a card" thumbnails
- [ ] "Top songs" (up to 10) with play counts and play-all
- [ ] "Top artists" (up to 10) with minutes
- [ ] "Languages" breakdown
- [ ] "When you listen": 24-hour clock, listener type, busiest hour
- [ ] Log-start footnote
- [ ] Story: auto-advance, tap back / forward, hold to pause, progress segments
- [ ] Card types: intro, minutes, top song, top songs, top artist, top artists, languages, listening clock, highlights, summary
- [ ] "Save" to Pictures/Attic and "Share" as a 1080×1920 image
- [ ] Attic mark and period label on every shared image
- [ ] Loading and empty-period states

**Settings**
- [ ] "Appearance", "Playback", "Lyrics", "Moods", "Song radio", "Backup", "About" sections with the controls above
- [ ] "Volume levelling" switch (default on), "Crossfade" length (default off) (added 2026-11-23)
- [ ] Equaliser (added 2026-11-25): switch (default off), ten bands, eleven presets, saved presets with rename and delete, a setting per song and per album (song, then album, then default), the sheet from Now Playing's menu and its header pill, the Settings page with the lists of own settings, and "Phone equaliser" when the phone has one
- [ ] "Back up to a file" and "Restore from a file", with the restore confirmation, result and problem dialogs
- [ ] Song count in "About"

**Messages and system states**
- [ ] Every snackbar message in section 2.17
- [ ] Permission screen in both states
- [ ] Loading state
- [ ] Every empty state in section 2.18

**Outside the app**
- [ ] Resizable widget in three layouts, tinted from the cover, with five album shortcuts at the largest size
- [ ] Media notification and lock-screen controls with shuffle and like
- [ ] Analysis progress notification with "Stop"
- [ ] Launcher icon with a monochrome variant
- [ ] "Open with" for audio files: library songs play in context, other files play once and are kept nowhere (added 2026-11-23)
- [ ] Android Auto: browse tabs with white icons, covers, search, "play X" by voice, and the shuffle and like buttons (added 2026-11-23, section 2.22). A restyle changes the tab icons and the shuffle and like glyphs only.

**Not present today.** Listed so a restyle does not add controls that have nothing behind them: playback speed, volume slider, output or cast picker, per-song download or delete, tag editing, liking an album or artist, playlist cover or description editing, search history, sort options for collections, time-remaining display, queue history, accounts.

---

## 5. What makes it read as Spotify

The cues a redesign has to remove or replace, with where each lives.

**Stated intent**
- `README.md:8` calls it "A Spotify-style player".
- `ui/Theme.kt:87` comments "Spotify has no light mode to copy, so this mirrors the dark roles."
- `ui/HomeScreen.kt:442` describes the settings button as sitting "in the spot where a streaming app shows your profile".

**Colour**
- Accent green `#1ED760`: `ui/Theme.kt:63`, `values/themes.xml:4`, `drawable/ic_launcher_foreground.xml:9`, `docs/logo.svg`, and the README badges.
- The neutral ramp `#121212` / `#181818` / `#282828` / `#B3B3B3`: `ui/Theme.kt:70-82`, `values/themes.xml:3`.
- Green as the only accent, on a near-black flat UI with white text and no elevation.
- Browse-tile colours such as `#8D67AB`, `#E13300`, `#1E3264`, `#148A08`: `ui/SearchScreen.kt:91-94`.
- The violet-to-mint Liked Songs gradient with a white heart: `ui/HomeScreen.kt:381-389`.

**Type**
- Figtree, a geometric sans, with bold tight-tracked headings: `ui/Theme.kt:24-44`.

**Navigation and naming**
- The three tabs "Home", "Search", "Your Library" with icon-over-label and a books icon for the library: `ui/AppRoot.kt:112-116`.
- A tab bar with no solid fill that fades content out behind it: `ui/AppRoot.kt:361`.
- "Your Library" as a screen title: `ui/LibraryScreen.kt:134`.
- "Liked Songs": `ui/HomeScreen.kt:345`, `ui/LibraryScreen.kt:99`, `ui/CollectionScreens.kt:419`.
- "Wrapped", "Your {year} Wrapped" and "Play your story": `ui/WrappedScreen.kt:113`, `ui/WrappedCards.kt:616`, `ui/WrappedScreen.kt:234`.
- "Song radio", "Start radio" and "Autoplay", with Autoplay's description: `ui/SettingsScreen.kt:118-132`, `ui/Sheets.kt:116`.
- "Downloaded" as a library filter and a green label: `ui/LibraryScreen.kt:55`, `ui/NowPlaying.kt:318`.
- "Playing from" above the context name: `ui/NowPlaying.kt:345`.
- "What do you want to listen to?": `ui/SearchScreen.kt:121`.
- "Start browsing": `ui/SearchScreen.kt:147`.
- "Recents" with a sort glyph: `ui/LibraryScreen.kt:192-194`.
- "Popular" and "Popular releases": `ui/CollectionScreens.kt:331`, `352`.
- "Recently played", "Your top artists", "More from": `ui/HomeScreen.kt:156`, `242`, `228`.
- "About the artist" and "Credits": `ui/NowPlaying.kt:564`, `587`.
- "Show lyrics": `ui/LyricsScreen.kt:151`.
- "Let's find something for your playlist": `ui/CollectionScreens.kt:529`.
- "Give your playlist a name": `ui/LibraryScreen.kt:225`.
- "Go to album", "Go to artist", "Add to queue", "Add to playlist": `ui/Sheets.kt:130-150`.
- "Playlist, {n} songs", "Album, {artist}", "Artist" as row subtitles: `ui/LibraryScreen.kt:99-114`.

**Home**
- A round avatar-style button top-left followed by a row of pill filter chips, the selected one green: `ui/HomeScreen.kt:102-116`, `459-474`.
- The 2-column grid of short 56dp tiles with a square image flush left and a two-line title: `ui/HomeScreen.kt:121-128`, `357-377`.
- Horizontal shelves of 160dp cards under bold section titles: `ui/HomeScreen.kt:405-431`.
- Circular artist images in a shelf: `ui/HomeScreen.kt:433-440`.
- A small circular artist image with "More from" above the name: `ui/HomeScreen.kt:218-231`.

**Search**
- A white search field on a dark screen: `ui/SearchScreen.kt:182-197`.
- Coloured category tiles with a cover tucked into the corner at an angle: `ui/SearchScreen.kt:199-228`.

**Library**
- Title row with search and plus icons, chips beneath, then a sort label with a grid toggle: `ui/LibraryScreen.kt:126-202`.
- Rows mixing playlists, artists (circular) and albums with a type prefix in the subtitle.

**Collection screens**
- A header gradient from the cover's colour into the background, behind a centred cover: `ui/CollectionScreens.kt:136-141`.
- A top bar that takes the cover colour and reveals the title on scroll: `ui/CollectionScreens.kt:76-103`.
- The control row: small cover thumbnail, plus-in-circle, green download arrow on the left; shuffle and a large green play circle on the right: `ui/CollectionScreens.kt:189-231`.
- The artist hero: full-bleed image with the name in very large bold white type over a dark fade: `ui/CollectionScreens.kt:310-327`.
- Numbered "Popular" rows: `ui/CollectionScreens.kt:333-349`.
- A small circular artist image beside the album's artist name: `ui/CollectionScreens.kt:165-177`.
- "Album • 2025" detail line with a bullet separator: `ui/CollectionScreens.kt:269`.

**Rows and indicators**
- A green circular down-arrow before every artist name: `ui/Components.kt:116-133`, `173`.
- The current song's title in green with small animated equaliser bars: `ui/Components.kt:90-114`, `159-166`.
- The like control as an outlined plus-in-circle that becomes a filled green check: `ui/Motion.kt:56-80`.
- Trailing vertical kebab on every row.
- 4dp cover corners: `ui/Components.kt:58`.

**Mini player**
- A floating rounded bar above the tab bar, filled with the cover's colour, with a thin progress line along its bottom edge and plus and play icons at the right: `ui/NowPlaying.kt:128-204`.

**Now Playing**
- Header of down-chevron, "Playing from" plus context, and kebab: `ui/NowPlaying.kt:337-357`.
- Background gradient from the cover colour to near-black: `ui/NowPlaying.kt:279`.
- Title and artist left-aligned with the plus-in-circle at the right: `ui/NowPlaying.kt:290-304`.
- A thin white seek line with a small round handle and times below at each end: `ui/NowPlaying.kt:471-516`.
- The transport row: shuffle, previous, a large white play circle with a black glyph, next, repeat, with active toggles green and marked by a dot underneath: `ui/NowPlaying.kt:395-439`.
- Scrolling down to a coloured lyrics card with a white "Show lyrics" pill, then "About the artist" over an image, then "Credits": `ui/NowPlaying.kt:327-331`, `ui/LyricsScreen.kt:90-155`.

**Lyrics**
- Full screen filled with the cover colour, large bold lines, white for sung and dark for upcoming: `ui/LyricsScreen.kt:59-60`, `176-254`.

**Track menu**
- A dark bottom sheet with a cover-title-artist header, a divider, and icon-plus-label rows ending in "Go to album" and "Go to artist": `ui/Sheets.kt:98-156`.

**Wrapped**
- The name, the December switch to "Your {year} Wrapped", a tap-through story with segmented progress bars and 7-second cards, bold flat-colour 9:16 cards made for sharing, a top-five summary card, and a listener-type label: `ui/WrappedScreen.kt:369-470`, `ui/WrappedCards.kt:82-109`, `532-567`.

**Outside the app**
- The plus-in-circle and check-in-circle like icons in the media notification: `playback/PlaybackService.kt:129`.
- A widget filled with the cover colour with a white play circle: `layout/widget_large.xml:85-92`, `widget/PlayerWidget.kt:165`.
- The launcher icon: a green glyph on `#121212`: `mipmap-anydpi-v26/ic_launcher.xml`, `drawable/ic_launcher_foreground.xml`.
