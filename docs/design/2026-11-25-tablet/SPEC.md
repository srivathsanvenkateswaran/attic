# Attic on tablets: layout spec

2026-11-25 · branch `feature/tablet-layouts`

The rules both styles follow on medium and expanded windows. The FigCraft `Tablet` pages draw them and
the Compose code implements them; where the two disagree once the code lands, the code wins and the
design file is updated. Phones (compact width) keep every layout they have today.

The target device is a Xiaomi Pad 6: 2880 × 1800 px, which is about 1440 × 900 dp in landscape and
900 × 1440 dp in portrait at its default density (to be confirmed on the device). It is used in both
orientations, mostly landscape, and HyperOS lets the app run in split screen and floating windows, so
any width between about 300 and 1440 dp can occur and can change while the app runs.

## Window classes

Width decides the layout. The breakpoints are Material's window size classes:

| Class | Width | Pad 6 case |
|---|---|---|
| Compact | under 600 dp | phones; a narrow floating window |
| Medium | 600 to 839 dp | half of the Pad in split screen (about 700 dp) |
| Expanded | 840 dp and wider | the Pad full screen, either orientation |

Height matters in two places only: Now Playing and full-screen lyrics go side by side when the window
is expanded **and** wider than it is tall, and a window under 480 dp tall keeps the compact shell
whatever its width.

Panes decide their own layout from the width they are given, not from the window class. An album
opened beside the Library list lays itself out for the width of that pane.

## Orientation

- Large screens (smallest width 600 dp or more): no orientation lock. The activity follows the device
  and survives rotation and resizing without losing the current screen, the overlay that is open, the
  scroll position or playback.
- Phones (smallest width under 600 dp): portrait, set at runtime rather than in the manifest. Every
  phone screen is drawn for a tall window; a phone in landscape is about 411 dp tall, too short for
  Now Playing's cover, seek bar and controls, and nobody has asked for it. Android ignores the
  request in multi-window, where the compact layouts already cope with short windows.

## Frame sizes on the Tablet page

| Frame | Size (dp) | Stands for |
|---|---|---|
| Expanded landscape | 1440 × 900 | the Pad 6 held sideways |
| Expanded portrait | 900 × 1440 | the Pad 6 held upright |
| Medium | 700 × 900 | half of the Pad in split screen |

Each frame starts with a status bar across its full width (24 dp on a tablet) and ends with the
gesture handle (20 dp), both from the style's system-bar components, stretched.

## The shell

| | Compact | Medium | Expanded |
|---|---|---|---|
| Navigation | bottom bar (Expressive), dock (Comfort) | rail at the start edge | rail at the start edge |
| Song playing | mini player above the bar | mini player floating at the bottom of the content area, at most 560 dp wide, centred | player bar along the bottom of the content area |
| Now Playing, queue, lyrics | full-screen overlays | full-screen overlays | full-screen overlays, laid out side by side in landscape |

**Expressive rail** (`ui/expressive/ExpressiveRail.kt` · `ExpressiveNavigationRail`): Material 3
Expressive's collapsed navigation rail, 96 dp wide, on `surface` (the page colour, no divider). The
Headroom mark sits 44 dp below the status bar; the three destinations (Home, Search, Library) follow
below it, each a 56 × 32 dp indicator pill (`secondaryContainer` when selected) holding the icon, with
the label under it in Label/Medium, 4 dp gap, items 12 dp apart. Selected icons are filled.

**Comfort side dock** (`ui/comfort/SideDock.kt` · `ComfortSideDock`): the dock's floating card turned
on its side. It is 80 dp wide and sits 12 dp in from the start edge, 12 dp below the status bar and
12 dp above the gesture area, on the dock's surface with its superellipse corners. The Comfort mark is
at its top; the tabs are stacked under it, each an icon over its label in a 64 × 56 dp cell, the
selected one on the amber selection fill the bottom dock uses.

**Expressive player bar** (`ui/expressive/ExpressivePlayerBar.kt` · `ExpressivePlayerBar`): 80 dp
tall on `surfaceContainer`, spanning the content area (not the rail), above the gesture inset, tinted
from the cover like the mini player. Left: 56 dp cover, title over artist (up to 320 dp). Centre:
previous, play/pause (the mini player's play button with its progress ring, at 48 dp) and next, with
a thin seek bar and the two times under them (480 dp wide). Right: heart, queue, lyrics. Tapping the
cover or the titles opens Now Playing.

**Comfort player bar** (`ui/comfort/ComfortPlayerBar.kt` · `ComfortPlayerBar`): a floating card 76 dp
tall, 12 dp from the content's edges and above the gesture area, with the mini player's colours and
its played-so-far fill. The same three groups as Expressive's bar, drawn with Comfort's controls.

The shell itself stays in `ExpressiveShell.kt` / `ComfortShell.kt`, which pick the wide arrangement
from `ui/adaptive/AdaptiveLayout.kt`. Destinations receive the space the bar or dock takes as
`LocalBottomPadding`, as they do now, and the rail's width is simply not theirs.

## Screens

Readable width: a column of text, rows or settings is never wider than 720 dp. Where a screen would
otherwise stretch one column across a wide pane, it centres the column at that width or splits the
width into columns or panes, as each screen says below.

**Home** (`ExpressiveHome.kt` / `ComfortHome.kt`). Shelves that scroll sideways keep scrolling and
simply show more items. Everything that is a single full-width block on a phone sits in a grid:
- the Recap card and the Instant mixes sit side by side on expanded widths;
- Instant mixes and Mixes take three columns on medium, four on expanded;
- album, artist and folder grids (the filter chips) use adaptive columns, a cell at least 160 dp wide;
- Expressive's hero carousel shows its large item at up to 360 dp, with more items after it;
- Comfort's shortcut grid takes six columns on expanded, four on medium.

**Library** (`ExpressiveLibrary.kt` / `ComfortLibrary.kt`, with `ui/LibraryPanes.kt` ·
`LibraryListDetail`). The grid view uses adaptive columns, a cell at least 160 dp wide. When the window
is at least 1200 dp wide (the Pad in landscape) the Library is list and detail: the list in a 400 dp
pane at the start, and the album, artist or playlist it opened in the pane beside it, the first item
selected on arrival. Narrower windows open the detail as its own screen, as phones do, and list rows
sit in adaptive columns at least 360 dp wide.

**Search** (`ExpressiveSearch.kt` / `ComfortSearch.kt`). Browse tiles in adaptive columns, a cell at
least 180 dp wide. Results keep their sections; song and album rows sit in adaptive columns at least
360 dp wide; the search field is at most 720 dp wide.

**Album, artist, playlist, folder, Favourites, genre** (`ui/expressive/ExpressiveCollectionPanes.kt` ·
`CollectionPanes`, `ui/comfort/ComfortCollectionPanes.kt` · `ComfortCollectionPanes`).
- Pane 760 dp or wider: two panes. The start pane (360 dp, 320 dp below 1000 dp) holds the header and
  does not scroll unless it has to: cover up to 296 dp, title, artist, the details line, and the play,
  shuffle and add buttons. The end pane holds the songs, scrolling, with the top bar's back button and
  menu above them. An artist's end pane holds Top songs, then Albums in adaptive columns.
- Pane narrower than 760 dp: one column, the header as a row (cover 200 dp at the start, title,
  details and buttons beside it) instead of stacked, then the songs.

**Now Playing** (`ui/expressive/ExpressiveNowPlayingWide.kt` · `ExpressiveNowPlayingWide`,
`ui/comfort/ComfortNowPlayingWide.kt` · `ComfortNowPlayingWide`).
- Expanded and landscape: two halves. The start half is the player: the top bar ("Playing from", close,
  menu), the cover as large as fits (up to 440 dp), the title block, the seek bar, the transport and the
  tools row. The end half is a panel with a two-way switch, Up next and Lyrics: Up next is the queue
  list (drag to reorder, swipe to remove, as in the queue screen); Lyrics is the synced lyrics, with
  the button that opens them full screen.
- Expanded portrait and medium: the phone layout in a centred column at most 560 dp wide, the cover up
  to 480 dp.

**Full-screen lyrics** (`ExpressiveLyrics.kt` / `ComfortLyrics.kt`). Expanded and landscape: a 360 dp
start pane with the cover, title, artist, seek bar and play controls; the lyrics in the rest, in a
column at most 640 dp wide. Otherwise a centred column at most 640 dp wide.

**Queue** (`ExpressiveQueue.kt` / `ComfortQueue.kt`). A centred column at most 720 dp wide.

**Settings, backup and restore, licences** (`ExpressiveSettings.kt` / `ComfortSettings.kt` and the
licence screens). A centred column at most 720 dp wide; the top bar spans the window.

**Sheets and dialogs** (`ExpressiveSheets.kt` / `ComfortSheets.kt`, `Surfaces.kt` / `Overlays.kt`).
Bottom sheets are at most 640 dp wide and centred, as Material specifies. Dialogs are at most 560 dp
wide. Menus keep their phone width.

**Recap** (`ExpressiveRecap.kt` / `ComfortRecap.kt`). The page is a centred column at most 720 dp wide.
The story keeps a phone screen's proportions (411 by 914), as tall as the window allows, centred on the
scrim.

**First run** (`ExpressiveOnboarding.kt` / `ComfortOnboarding.kt`). A centred column at most 560 dp
wide; the two look cards stay side by side.

## Input

Rows, tiles and buttons show the hover state their ripple already draws for a mouse. Keyboard focus
moves through the rail, the content and the player bar in that order. Space plays or pauses when no
text field has focus.

## Frames to draw, per style, each in Light and Dark

| Screen | Expanded landscape | Expanded portrait | Medium |
|---|---|---|---|
| Home | yes | yes | yes |
| Library (landscape: list and detail with an album open) | yes | yes | yes |
| Search, results | yes | yes | yes |
| Album | yes | yes | yes |
| Artist | yes | | |
| Playlist | yes | | |
| Now Playing with Up next | yes | yes (column) | yes (column) |
| Now Playing with Lyrics | yes | | |
| Full-screen lyrics | yes | | |
| Settings | yes | yes | |
| Track menu sheet over the album | yes | | |
| New playlist dialog over the Library | yes | | |
| First run, Choose a look | yes | yes | |

The demo library and the BRIEF naming table apply as on the phone frames. Every frame carries a
`Source` annotation naming the Compose file and composable above.
