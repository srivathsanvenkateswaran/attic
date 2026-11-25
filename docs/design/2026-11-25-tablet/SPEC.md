# Attic on tablets: layout spec

2026-11-25 · branch `feature/tablet-layouts`

The rules both styles follow on medium and expanded windows. The FigCraft `Tablet` pages draw them and
the Compose code implements them; where the two disagree once the code lands, the code wins and the
design file is updated. Phones (compact width) keep every layout they have today.

The target device is a Xiaomi Pad 6: 2880 × 1800 px at 400 dpi (density 2.5, from its stock
`ro.sf.lcd_density`; to be confirmed with `wm density` on the device), so **1152 × 720 dp in landscape
and 720 × 1152 dp in portrait**. Landscape is an expanded width; portrait is a medium width. Its last
update is HyperOS 2 on Android 14. It is used in both orientations, mostly landscape, and HyperOS lets
the app run in split screen (each half about 570 dp wide in landscape, a compact width) and in floating
windows, so any width from about 300 dp up can occur and can change while the app runs. A smaller
"display size" setting, or a bigger tablet, gives 1440 × 900 dp and more.

## Window classes

Width decides the layout. The breakpoints are Material's window size classes:

| Class | Width | Pad 6 case |
|---|---|---|
| Compact | under 600 dp | phones; a narrow floating window; half of the Pad in split screen (about 570 dp) |
| Medium | 600 to 839 dp | the Pad upright (720 dp) |
| Expanded | 840 dp and wider | the Pad sideways (1152 dp); bigger tablets |

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
| Landscape | 1152 × 720 | the Pad 6 held sideways, at its stock density |
| Portrait | 720 × 1152 | the Pad 6 held upright (a medium width) |
| Large landscape | 1440 × 900 | the Pad 6 at a smaller display size, or a bigger tablet |

Each frame starts with a status bar across its full width (24 dp on a tablet) and ends with the
gesture handle (20 dp), both from the style's system-bar components, stretched.

The screen margin is 24 dp on medium and expanded windows. The phone components carry 16 dp, so a
destination adds 8 dp at each side. Shelves that scroll sideways start at the margin. Expressive's run on
to the window's end edge; Comfort's stop where the page's column does, 8 dp before it.

## The shell

| | Compact | Medium | Expanded |
|---|---|---|---|
| Navigation | bottom bar (Expressive), dock (Comfort) | rail at the start edge | rail at the start edge |
| Song playing | mini player above the bar | mini player floating at the bottom of the content area, at most 576 dp wide, centred | player bar along the bottom of the content area |
| Now Playing, queue, lyrics | full-screen overlays | full-screen overlays | full-screen overlays, laid out side by side in landscape |

**Expressive rail** (`ui/expressive/ExpressiveRail.kt` · `ExpressiveNavigationRail`): Material 3
Expressive's collapsed navigation rail, 96 dp wide, on `surface` (the page colour, no divider). The
Headroom mark (32 dp) sits 20 dp below the status bar, centred on the top bar's title line; the three
destinations (Home, Search, Library) follow 28 dp below it, each a 56 × 32 dp indicator pill
(`secondaryContainer` when selected) holding the icon, with the label under it in Label/Medium, 4 dp
gap, items 12 dp apart. Selected icons are filled. The rail carries the mark, so Home's top bar is the
title and settings without it.

**Comfort side dock** (`ui/comfort/SideDock.kt` · `ComfortSideDock`): the dock's floating card turned
on its side. It is 80 dp wide and sits 12 dp in from the start edge, 12 dp below the status bar and
12 dp above the gesture area, on the dock's surface with its superellipse corners. The Comfort mark is
at its top; the tabs are stacked under it, each an icon over its label in a 64 × 56 dp cell, the
selected one on the amber selection fill the bottom dock uses. A destination's cards start 24 dp
after the dock (116 dp from the window's start edge) and end 24 dp before the end edge. The dock
carries the mark, so Home's hero is the title alone; the hero is the short one (150 dp) in a window
under 840 dp tall.

**Expressive player bar** (`ui/expressive/ExpressivePlayerBar.kt` · `ExpressivePlayerBar`): an 80 dp
row on `surfaceContainer`, spanning the content area (not the rail), tinted from the cover like the
mini player. Its top corners are 28 dp and its surface runs on behind the 20 dp gesture inset. Left:
56 dp cover, title over artist (up to 320 dp). Centre: previous, play/pause (the large play button at
48 dp, without the progress ring, since the seek bar under it shows progress) and next, with the
elapsed time, a wavy progress bar and the length under them. Right: heart, queue, lyrics. The centre
group is 36 % of the bar's width, at least 320 and at most 480 dp (384 dp on the Pad sideways), and
the left and right groups share the rest equally, so the title keeps its room on a 1056 dp bar.
Tapping the cover or the titles opens Now Playing.

**Comfort player bar** (`ui/comfort/ComfortPlayerBar.kt` · `ComfortPlayerBar`): a floating card 76 dp
tall, in line with the content's cards (24 dp from the window's end edge) and 12 dp above the gesture
area, with the mini player's colours and its played-so-far fill. Left: cover, title over artist.
Centre: the elapsed time, previous, play/pause, next, the length. Right: heart, queue, lyrics. It has
no separate seek line: the played-so-far fill across the card is its progress, and a second one at
another scale read as a mistake.

On medium widths the mini player is the phone's, at most 576 dp wide (so on the Pad upright it lines up
with the 24 dp margins of the 624 dp content area), centred in the content area,
12 dp above the gesture inset. Expressive draws the gesture inset in the page colour so the page does
not show under the handle; Comfort puts the mini player in its dock card over the dock's fade.

The shell itself stays in `ExpressiveShell.kt` / `ComfortShell.kt`, which pick the wide arrangement
from `ui/adaptive/AdaptiveLayout.kt`. Destinations receive the space the bar or dock takes as
`LocalBottomPadding`, as they do now, and the rail's width is simply not theirs.

## Screens

Readable width: a column of text, rows or settings is never wider than 720 dp. Where a screen would
otherwise stretch one column across a wide pane, it centres the column at that width or splits the
width into columns or panes, as each screen says below.

**Home** (`ExpressiveHome.kt` / `ComfortHome.kt`). Shelves that scroll sideways keep scrolling and
simply show more items. Everything that is a single full-width block on a phone sits in a grid:
- Expressive: the Recap card and the Instant mixes sit side by side on expanded widths, the card
  taking 40 % of the row and at most 480 dp, as tall as the mixes beside it. Comfort keeps the Recap
  entry as a full-width row: its instant mixes are song rows, and beside a card their columns fall
  under 280 dp;
- Instant mixes take three equal columns (Expressive on medium and expanded; Comfort three on
  expanded and two on medium, where three truncate the titles). Four columns truncate "Platform Ettu"
  at 1152 dp. Comfort's columns are cards of song rows, filled top to bottom. Mixes stays a shelf, as
  the code builds it;
- Start listening takes two columns on expanded widths, one on medium;
- album, artist and folder grids (the filter chips) use adaptive columns, a cell at least 160 dp wide;
- Expressive's filter is at most 480 dp wide, at the start, and its "More from" block at most 720 dp;
- Expressive's hero carousel is 200 dp tall with a preferred item width of 260 dp in a window under
  840 dp tall (the Pad sideways, where a taller carousel pushes the Recap row under the player bar),
  and 280 dp tall with a preferred item width of 360 dp in a taller one;
- Comfort's shortcut tray takes six columns on expanded and on medium (four would leave a second row
  of two), its art 112 dp on expanded and 72 dp on medium.

**Library** (`ExpressiveLibrary.kt` / `ComfortLibrary.kt`, with `ui/LibraryPanes.kt` ·
`LibraryListDetail`). The grid view uses adaptive columns, a cell at least 160 dp wide. When the window
is expanded and wider than it is tall (the Pad in landscape), and the content area beside the rail or
dock is at least 1000 dp wide, the Library is list and detail: the list
in a 400 dp pane at the start, and the album, artist or playlist it opened in the pane beside it, the
first item selected on arrival. The pane is 400 rather than 360 dp because the filter chips need
372 dp with one of them on. The selected row takes the colours a playing song's row has; in Comfort's
grid view the open tile sits on the same bed, reaching 6 dp past the tile. The detail
pane is about 650 dp wide on the Pad, so the detail takes its header-row layout; at 1440 dp it is wide
enough for two panes. The detail has no back button, since the list is beside it. A narrower
expanded window (a 7-inch tablet sideways, about 960 by 600 dp) would leave the detail under 600 dp,
where it takes the phone's screen with its back button, so there the Library is the list alone, in
columns, and opens items as their own screens. Comfort's list pane
uses the collapsed bar instead of the hero, which would leave four rows at 720 dp. Narrower or
upright windows open the detail as its own screen, as phones do, and list rows sit in adaptive columns
at least 360 dp wide, each column its own group, filled top to bottom.

**Search** (`ExpressiveSearch.kt` / `ComfortSearch.kt`). Browse tiles in adaptive columns, a cell at
least 180 dp wide. Results keep their sections; song and album rows sit in adaptive columns at least
360 dp wide; the search field is at most 720 dp wide.

**Album, artist, playlist, folder, Favourites, genre** (`ui/expressive/ExpressiveCollectionPanes.kt` ·
`CollectionPanes`, `ui/comfort/ComfortCollectionPanes.kt` · `ComfortCollectionPanes`).
- Pane 760 dp or wider: two panes. The start pane (360 dp, 320 dp below 1000 dp) holds the header and
  does not scroll unless it has to: cover up to 296 dp, title, artist, the details line, and the play,
  shuffle and add buttons in one row. On a short window the cover gets smaller (to 120 dp at least,
  centred in Expressive's card) so the buttons stay above the player bar; where the row leaves Play
  too little room for its label (Expressive's card on a 7-inch tablet), Play takes a row of its own
  above shuffle and add. The end pane holds the songs, scrolling, in adaptive columns at
  least 360 dp wide (two columns at 1440 dp), filled top to bottom. An artist's end pane holds Top
  songs, then Albums in adaptive columns. A playlist's Add songs and Reorder songs sit above its songs.
  Expressive draws the header as a card on the cover's scheme (32 dp corners, hugging its content,
  the cover 288 dp in a 360 dp pane) and puts back and the menu above the songs; beside the Library's
  list, where the detail has no bar, a playlist's or Favourites' menu button sits at the end of the
  header's buttons. Comfort's bar spans the pane with back at the start edge, because back above the
  songs sat mid-screen. It never shows the collection's name, since the header stays in view.
- Pane narrower than 760 dp: one column, the header as a row (cover 200 dp at the start, title,
  details and buttons beside it) instead of stacked, then the songs.

**Now Playing** (`ui/expressive/ExpressiveNowPlayingWide.kt` · `ExpressiveNowPlayingWide`,
`ui/comfort/ComfortNowPlayingWide.kt` · `ComfortNowPlayingWide`).
- Expanded and landscape: two halves. The start half is the player: the top bar ("Playing from", close,
  menu), the cover as large as fits (about 280 dp on the Pad's 720 dp height, up to 440 dp), the title block, the seek bar, the transport and the
  tools row. The player's column is never narrower than its transport, so on a short window
  (960 by 600 dp) the cover shrinks but previous, play and next keep their size and the title its
  room; the cover then starts at the column's start in Expressive and stays centred in Comfort.
  At 720 dp tall the line being sung is left out from under the title and the gaps are
  tighter, so the tools row ends above the gesture area; Comfort leaves that line out of the wide
  layout altogether, since Lyrics is one tap away. The end half has a two-way switch at its top, Up
  next and Lyrics, and under it the queue as the queue screen lays it out (drag to reorder, swipe to
  remove) or the synced lyrics, with the button that opens them full screen at the end of the switch's
  row. The end half sits on the page, not on a card.
- Otherwise (the Pad upright, medium widths, expanded portrait on bigger tablets): the phone layout in
  a centred column at most 576 dp wide, the cover up to 480 dp.

**Full-screen lyrics** (`ExpressiveLyrics.kt` / `ComfortLyrics.kt`). Expanded and landscape: a 360 dp
start pane with the cover, title, artist, seek bar and play controls, drawn as one card; the lyrics in
the rest, in a column at most 640 dp wide. Otherwise a centred column at most 640 dp wide.

**Queue** (`ExpressiveQueue.kt` / `ComfortQueue.kt`). A centred column at most 720 dp wide.

**Settings, backup and restore, licences** (`ExpressiveSettings.kt` / `ComfortSettings.kt` and the
licence screens). A centred column at most 720 dp wide; the top bar spans the window.

**Sheets and dialogs** (`ExpressiveSheets.kt` / `ComfortSheets.kt`, `Surfaces.kt` / `Overlays.kt`).
Bottom sheets are at most 640 dp wide and centred, as Material specifies. Dialogs are at most 560 dp
wide and centred in the window; a dialog with one field, such as New playlist, is 400 dp wide.
Comfort anchors its dialogs at the bottom on a phone and centres them here. Menus keep their phone
width.

**Recap** (`ExpressiveRecap.kt` / `ComfortRecap.kt`). The page is a centred column at most 720 dp wide.
The story keeps a phone screen's proportions (411 by 914), as tall as the window allows, centred on the
scrim.

**First run** (`ExpressiveOnboarding.kt` / `ComfortOnboarding.kt`). A centred column at most 560 dp
wide; the two look cards stay side by side. Comfort's Theme group is a list of three rows, so on the
Pad sideways (720 dp tall) the look cards and the Theme group sit side by side in a block 840 dp wide
and Continue keeps the 560 dp width.

## Input

Rows, tiles and buttons show the hover state their ripple already draws for a mouse. Keyboard focus
moves through the rail, the content and the player bar in that order. Space plays or pauses when no
text field has focus.

## Frames to draw, per style, each in Light and Dark

| Screen | Landscape 1152 × 720 | Portrait 720 × 1152 | Large landscape 1440 × 900 |
|---|---|---|---|
| Home | yes | yes | yes, and the whole page (full length) |
| Library (landscape: list and detail with an album open) | yes | yes | yes |
| Search, results | yes | yes | |
| Album | yes | yes | yes |
| Artist | yes | | |
| Playlist | yes | | |
| Now Playing with Up next | yes | yes (column) | yes |
| Now Playing with Lyrics | yes | | |
| Full-screen lyrics | yes | | |
| Settings | yes | yes | |
| Track menu sheet over the album | yes | | |
| New playlist dialog over the Library | yes | | |
| First run, Choose a look | yes | yes | |

The demo library and the BRIEF naming table apply as on the phone frames. Every frame carries a
`Source` annotation naming the Compose file and composable above.
