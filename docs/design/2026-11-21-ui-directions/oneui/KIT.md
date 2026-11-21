# One UI kit: class reference

`kit.css` holds every token and component. You cannot edit it. If something is missing, put
the rule in your sheet's own `<style>` (prefixed with `.phone` or `.strip`) and say so in your
report so it can be folded in.

`00-direction.html` renders every component below in dark and light. Open it first.
`01-home.html` and `05-now-playing.html` are the finished sheets to copy structure from.

## Page head

```html
<!doctype html>
<meta charset="utf-8">
<title>Attic · One UI · NN Name</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="../shared/page.css">
<link rel="stylesheet" href="../shared/covers.css">
<link rel="stylesheet" href="kit.css">
```

`kit.css` imports Onest and Material Symbols Rounded itself. Add no other font link.

## The rules, short

**Shape**
- Every image and every icon container is a superellipse, drawn by a mask class on the element:
  `.sq` up to 96dp, `.sq-lg` from 100dp, `.sq-soft` (rounder) for artists. Never `border-radius`
  on a `.cv`, and never `.cv.round`.
- Rectangles have three radii: 30 for things that float (dock, deck, sheet, dialog, action
  bar), 26 for groups, 18 for a control nested in a card. Buttons and chips are pills.
- The only circles are the radio, the switch knob and the spinner.

**Colour**
- Dark: black ground, `--card` one step up, `--raised` for anything floating. Light: plaster
  ground, white cards. No card has a shadow; only floating pieces do.
- One accent, Lamplight amber. It marks selection, the one filled button on a screen, switches
  and progress. Titles and row text stay in `--on`. A current song is never amber text.
- In light, amber as text is `--accent-ink` (Brass). Use the tokens, never a hex.
- Album-art colour: put `tinted` on a container that has, or sits inside, an `art-<key>`.
  The accent tokens inside it become the cover's colour. Add `wash` to lean the surfaces too.
  Amber does not appear inside a tinted container.

**Layout**
- Root screens (Home, Search, Library, Settings, Recap) open with `.hero`: a centred large
  title in an otherwise empty upper third. Scrolled, it becomes `.bar`.
- A collection is an object (art with its name under it, in a tray, shelf or grid). A song is
  a row in a `.group`. A collection inside a list is a `.row`.
- Rows sit in a `.group` and are split by hairlines the kit draws for you. The label for a
  group goes above it, outside, as `.label`. No gaps between rows.
- Side margins: groups 16, labels and shelves on the ground 24 to 28, Now Playing text 30.

**Type**: Onest. 40/46 bold (hero), 26/32 bold (song and collection titles), 20/26 bold (bar,
sheet and dialog titles), 16/22 medium (row titles), 15/21 (body), 13.5/18 (second lines),
14/18 semibold (labels). The component classes set all of these; you should not need a
`font-size`.

## Frame

```html
<div class="phone">                       <!-- dark. Light: class="phone light" -->
  <div class="sbar">…copy from 01-home.html…</div>
  <div class="screen pad-dock">
    …content, in normal flow…
  </div>
  <div class="dock">…</div>
  <div class="gpill"></div>
</div>
```

- `.screen` is 800dp tall and clips. In `.phone.long` it grows with its content.
- Bottom padding: `.pad-dock` (dock with mini player), `.pad-tabs` (dock, tabs only),
  `.pad-bar` (action bar). None for full-screen overlays.
- **Status bar and gesture pill** take `--on` from the phone, so they are light on dark and
  dark on light with no extra class. On a `.tinted.wash` phone they follow the washed ink. A
  `.scrim` covers and dims them. For a screen that is always dark (the Recap story), use a
  `.phone` without `light`.
- Floating pieces (`.dock`, `.actionbar`, `.sheet`, `.dialog`, `.menu`, `.toast`, `.scrim`,
  `.kbd`) are direct children of `.phone`, after `.screen`. Inside a `.strip` they drop into
  normal flow by themselves.
- A component shown alone: `<div class="strip" style="width:390px">…</div>`, or
  `class="strip light"`.

## Icons and the mark

```html
<span class="ms">shuffle</span>           <!-- Material Symbols Rounded, weight 300, 24dp -->
<span class="ms fill">favorite</span>     <!-- filled -->
<span class="ms s18">chevron_right</span> <!-- sizes: s18 s20 s28 s32 s40 -->
```

Names in use: `home search library_music settings favorite more_vert chevron_right arrow_back
expand_more play_arrow pause skip_previous skip_next shuffle repeat repeat_one queue_music
sensors` (radio) `lyrics playlist_add playlist_play add check close drag_handle swap_vert sort
grid_view view_list edit delete language album person do_not_disturb_on` (remove) `download
ios_share folder_open music_note graphic_eq`.

The mark, coloured by `currentColor` (amber by default):

```html
<svg class="mark" viewBox="20 21 68 66" aria-hidden="true"><path fill="currentColor" stroke="currentColor" stroke-width="5" stroke-linejoin="round" fill-rule="evenodd" d="M54,25L84,53L84,79Q84,83 80,83L28,83Q24,83 24,79L24,53ZM42.5,60a11.5,11.5 0 1,0 23,0a11.5,11.5 0 1,0 -23,0Z"/></svg>
```

## Artwork

```html
<div class="cv art-rockstar sq w56"></div>          <!-- album, in a row -->
<div class="cv art-rockstar sq w48 plain"></div>    <!-- 48dp and under: drop the printed title -->
<div class="cv art-anbe sq-soft w56"></div>         <!-- artist -->
<div class="cv art-viva sq-lg w132"></div>          <!-- 100dp and over -->
<div class="mosaic sq w56"><div class="cv art-oso"></div><div class="cv art-yjhd"></div><div class="cv art-lootera"></div><div class="cv art-rockstar"></div></div>
<div class="ibox sq w56"><span class="ms fill">favorite</span></div>   <!-- Favourites -->
<div class="ibox sq neutral w56"><span class="ms">music_note</span></div> <!-- no artwork -->
```

Widths: `w32 w40 w44 w48 w56 w64 w72 w88 w96 w112 w132 w148 w196`, or `style="width:…"`.
`.ibox` variants: default (tonal amber), `.neutral`, `.solid`. The icon scales with the box.

## App bars

Expanded large title, with the row that closes the viewing area:

```html
<header class="hero">                     <!-- .hero.short: 150dp instead of 228 -->
  <h1>Library</h1>
  <p>127 songs on this phone</p>          <!-- optional -->
</header>
<div class="tools">
  <div class="subtabs">…</div>            <!-- optional, left -->
  <button class="ib" aria-label="Search your library"><span class="ms">search</span></button>
  <button class="ib" aria-label="Settings"><span class="ms">settings</span></button>
</div>
```

Only Home puts the mark above the `h1`. Collapsed (root) and detail:

```html
<div class="bar"><h2>Library</h2><button class="ib"><span class="ms">settings</span></button></div>

<div class="bar lift">                    <!-- .lift: content has scrolled under it -->
  <button class="ib" aria-label="Back"><span class="ms">arrow_back</span></button>
  <h2>Dhurandhar: The Revenge</h2>
  <button class="ib" aria-label="Playlist options"><span class="ms">more_vert</span></button>
</div>
```

At the top of a detail screen the bar is the same with an empty `<h2></h2>`. Overlays (Queue,
Lyrics) use `expand_more` in place of the back arrow. A text button fits at the end
(`<button class="btn text">Clear</button>`). `.bar.count` colours the title for a mode.

## Sub-tabs and chips

```html
<div class="subtabs">                     <!-- exactly one always selected: Home filters, Recap periods -->
  <a class="subtab on">This year</a><a class="subtab">All time</a>
</div>
<div class="subtabs lift">…</div>         <!-- pinned under a collapsed bar -->

<div class="chips">                       <!-- a filter that can be switched off: Library -->
  <a class="chip">Playlists</a>
  <a class="chip on"><span class="ms">check</span>Albums</a>
</div>
```

## Search field

```html
<div class="search"><span class="ms">search</span><span class="q"><span class="ph">Search your music</span></span></div>
<div class="search"><span class="ms">search</span><span class="q">dhur<i class="caret"></i></span>
  <button class="ib dim" aria-label="Clear search"><span class="ms">close</span></button></div>
```

## Label, group, rows

```html
<div class="label">Start listening <small>Picked at random from your songs</small></div>
<div class="label">Top songs <span class="end">…</span></div>     <!-- something at the right -->
<div class="group">
  <a class="row">                         <!-- 72dp. .row.sm 64dp, .row.lg 92dp with a bold title -->
    <div class="cv art-rockstar sq w56 plain"></div>
    <div class="txt"><b>Rockstar</b><span>Album, A. R. Rahman</span></div>
    <span class="ms">chevron_right</span> <!-- optional -->
  </a>
  <a class="row"><div class="cv art-anbe sq-soft w56"></div><div class="txt"><b>A. R. Rahman</b><span>Artist</span></div></a>
  <a class="row"><div class="mosaic sq w56">…</div><div class="txt"><b>Hindi</b><span>Playlist, 46 songs</span></div></a>
  <a class="row"><div class="ibox sq w56"><span class="ms fill">favorite</span></div><div class="txt"><b>Favourites</b><span>Playlist, 31 songs</span></div></a>
</div>
<p class="note">Small print under a group.</p>
```

- `.label.tight` when the label is the first thing under a bar.
- `.txt` takes `<em>` (small line above), `<b>` (title), `<span>` (line below). All one line
  with an ellipsis.
- A trailing value: `<span class="val">3,120 min</span>`.
- A non-row block inside a group (a shelf, a grid): wrap it in `<div class="inset">`.
- `.group.pad` pads a group that holds a grid instead of rows.
- Two groups in a row get 12dp between them.

## Song row

```html
<div class="song">
  <div class="lead"><div class="cv art-viva sq w48 plain"></div></div>
  <div class="txt"><b>Viva La Vida</b><span>Coldplay</span></div>
  <button class="ib" aria-label="More options for Viva La Vida"><span class="ms">more_vert</span></button>
</div>
```

| Variant | Change |
|---|---|
| Current song | `class="song now"`, and add `<span class="bars"><i></i><i></i><i></i></span>` inside `.lead` after the cover. Paused: `class="bars still"`. |
| No cover, numbered (album) | `class="song numbered"`, lead is `<span class="num">3</span>`. Current: put the `.bars` inside `.num` in place of the number. |
| No cover, no number | `class="song bare"`, no lead. |
| Rank and play count (Recap) | `class="song ranked"`, `<span class="rank">1</span>` before `.lead`, and `<span class="meta">214 plays</span>` as the last child. |
| Duration | `<span class="meta">4:05</span>` before the button. |
| Drag handle (queue, reorder) | button holds `drag_handle`. A row being dragged: `class="song lifted"`. |
| Add / added (Add songs) | `<button class="ib add">` with `add`; `<button class="ib added">` with `check`. |
| Starts radio (Instant mixes) | trailing `<span class="ib"><span class="ms">sensors</span></span>`. |

Queue row swiped aside:

```html
<div class="swipe"><span class="under">Remove</span><div class="song">…</div></div>
```

## Tiles, shelves, grids

```html
<div class="shelf">                                    <!-- scrolls sideways; clip is automatic -->
  <a class="tile" style="--w:132px"><div class="cv art-yjhd sq-lg"></div><b>Yeh Jawaani Hai Deewani</b><span>Pritam</span></a>
  <a class="tile center" style="--w:92px"><div class="cv art-viva sq-soft"></div><b>Coldplay</b></a>   <!-- artist -->
</div>

<div class="grid3">…tiles, no --w…</div>              <!-- also .grid2 .grid4 -->

<div class="group pad"><div class="grid3 tray">       <!-- tray: fixed 80dp art, centred names -->
  <a class="tile center"><div class="cv art-premam sq"></div><b class="two">Premam</b></a>
</div></div>
<div class="group pad"><div class="grid4 tray">…64dp art…</div></div>
```

- `b.two` lets a name wrap to two lines.
- Current song on a tile: `class="tile now"`, cover wrapped as
  `<div class="lead"><div class="cv …"></div><span class="bars">…</span></div>`.
- Cards that page sideways (Instant mixes): `<div class="pager"><div class="group">…</div>…</div>`.

Browse tile (Search, empty field), in a `.grid2`:

```html
<a class="browse"><div class="mosaic sq w56">…</div><div><b>Hindi</b><span>46 songs</span></div></a>
```

Add `tinted wash art-<key>` to a browse tile if it should take a trace of its cover.

## Collection and artist headers

```html
<div class="c-head">
  <div class="cv art-revenge sq-lg w196"></div>
  <h1>Dhurandhar: The Revenge</h1>
  <a class="by">Shashwat Sachdev<span class="ms s18">chevron_right</span></a>   <!-- album only -->
  <p class="meta">Album • 2026</p>
</div>
<div class="c-actions">
  <button class="btn filled"><span class="ms fill">play_arrow</span>Play</button>
  <button class="ib tonal lg" aria-label="Shuffle …"><span class="ms">shuffle</span></button>      <!-- add .on when shuffled and playing -->
  <button class="ib tonal lg" aria-label="Add … to a playlist"><span class="ms">playlist_add</span></button>
</div>

<div class="c-head person">
  <div class="cv art-anbe sq-soft w148"></div>
  <h1>A. R. Rahman</h1>
  <p class="meta">14 songs, 1 hr 13 min</p>
</div>
```

A playlist adds two more `.ib.tonal.lg` (`add`, `swap_vert`); the Play button shrinks to fit.
While this collection is playing the button reads "Pause" with `pause`.

## Buttons

```html
<button class="btn filled">Allow access</button>      <!-- the one main action -->
<button class="btn tonal">Open lyrics</button>
<button class="btn plain">Shuffle</button>            <!-- neutral fill -->
<button class="btn text">Download lyrics for all songs</button>
```

Modifiers: `.lg` (56dp), `.sm` (36dp), `.block` (full width), `.danger`, `.off` (disabled).
An icon goes first, inside the button.

```html
<button class="ib"><span class="ms">more_vert</span></button>           <!-- 48dp target. .dim, .off -->
<button class="ib tonal"><span class="ms">shuffle</span></button>       <!-- in a superellipse. .lg = 54dp, .on = lit -->
<button class="ib heart"><span class="ms">favorite</span></button>      <!-- favourite, off -->
<button class="ib heart on"><span class="ms">favorite</span></button>   <!-- on: filled, accent -->
<button class="play"><span class="ms">play_arrow</span></button>        <!-- 76dp. .md 56, .sm 44 -->
```

## Now Playing

The phone is `class="phone art-revenge tinted wash"`.

```html
<div class="screen">
  <div class="np-page">                                <!-- one screen tall -->
    <div class="np-head">
      <button class="ib" aria-label="Close player"><span class="ms">expand_more</span></button>
      <div class="txt"><em>Playing from</em><b>Dhurandhar: The Revenge</b></div>
      <button class="ib" aria-label="More options for …"><span class="ms">more_vert</span></button>
    </div>
    <div class="np-cover"><div class="cv art-revenge sq-lg"></div></div>   <!-- .np-cover.paused -->
    <div class="np-title"><h1>Jaan Se Guzarte Hain</h1><p class="by">Khan Saab, Shashwat Sachdev</p></div>
    <div class="np-lyric"><span class="ms s18">lyrics</span><span>The window lamp stays lit</span></div>
    <div class="deck">
      <div class="seek" style="--p:.323"><span class="at">1:47</span><span class="end">5:31</span></div>
      <div class="transport">
        <button class="tp"><span class="ms">skip_previous</span></button>
        <button class="play"><span class="ms">pause</span></button>
        <button class="tp"><span class="ms">skip_next</span></button>
      </div>
      <div class="toggles">
        <button class="ib tonal lg"><span class="ms">shuffle</span></button>          <!-- .on -->
        <button class="ib tonal lg"><span class="ms">repeat</span></button>           <!-- .on; repeat_one -->
        <button class="ib tonal lg heart"><span class="ms">favorite</span></button>   <!-- .on -->
        <button class="ib tonal lg"><span class="ms">queue_music</span></button>
      </div>
    </div>
  </div>
  …lyrics card, groups…
</div>
```

`--p` is progress from 0 to 1 everywhere (`.seek`, `.mini`, `.progress`). The current song is
at 1:47 of 5:31, `--p:.323`.

Lyrics card (states are yours to lay out; these are the parts):

```html
<div class="lyrics-card">
  <div class="hd">Lyrics</div>
  <p class="ln cur">The window lamp stays lit</p>      <!-- current line -->
  <p class="ln">We cross on borrowed time</p>          <!-- lines to come -->
  <button class="btn tonal sm">Open lyrics</button>
</div>
```

Plain lyrics: one `<p class="ln clip">` (clamped to four lines). Messages: a bare `<p>`.
Downloading: `<span class="spinner sm"></span>`. "Download lyrics" is `btn filled sm`.

Artist card and credits are groups under labels:

```html
<div class="label">About the artist</div>
<div class="group"><a class="row lg"><div class="cv art-revenge sq-soft w64"></div>
  <div class="txt"><b>Khan Saab</b><span>3 songs in your library</span></div><span class="ms">chevron_right</span></a></div>
<div class="label">Credits</div>
<div class="group">
  <div class="kv"><em>Performed by</em><b>Khan Saab, Shashwat Sachdev</b></div>
  <div class="kv"><em>Album</em><b>Dhurandhar: The Revenge</b></div>
</div>
```

Compact header once scrolled: `<div class="mini top" style="--p:.323">` with a `.txt`, the
heart and the play button, and no cover.

## Dock: mini player and tab bar

```html
<div class="dock">
  <div class="mini tinted art-revenge" style="--p:.323">
    <div class="cv art-revenge sq w44 plain"></div>
    <div class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div>
    <button class="ib heart" aria-label="Add to favourites"><span class="ms">favorite</span></button>
    <button class="ib" aria-label="Pause"><span class="ms fill">pause</span></button>
  </div>
  <nav class="tabs">
    <a class="tab on"><span class="ms">home</span>Home</a>
    <a class="tab"><span class="ms">search</span>Search</a>
    <a class="tab"><span class="ms">library_music</span>Library</a>
  </nav>
</div>
```

- Mini player states: playing (`pause`), paused (`play_arrow`), favourite (`heart on`), long
  text (ellipsis is automatic), light (the phone's `light`). The fill is `--p`.
- No song queued: leave the `.mini` out and use `.pad-tabs` on the screen.
- The mini player is on every destination, including Settings and Recap. Full-screen overlays
  (Now Playing, Queue, Lyrics, the Recap story) have no dock.
- A `.mini` on its own (a widget, a notification row) rounds all four corners.

## Sheets, dialogs, menu, toast

```html
<div class="scrim"></div>
<div class="sheet">
  <div class="grab"></div>
  <div class="sheet-head"><div class="cv art-revenge sq w48 plain"></div>
    <div class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div></div>
  <!-- or: <div class="sheet-title">Add to playlist</div> -->
  <div class="sheet-row"><span class="ms">sensors</span>Start radio</div>
  <div class="sheet-row"><span class="ms">language</span>Language: Hindi<span class="val">…</span></div>
  <div class="sheet-row on"><span class="ms">language</span>Hindi<span class="ms end">check</span></div>
  <div class="sheet-row danger"><span class="ms">do_not_disturb_on</span>Remove from this playlist</div>
</div>
```

`.song` rows and a `.search` field work inside a sheet. For a tall sheet set `style="top:68px"`.

```html
<div class="scrim"></div>
<div class="dialog">                                   <!-- .above-kbd lifts it over .kbd -->
  <h3>Delete Late drive?</h3>
  <p>The playlist goes away. The songs stay in your library.</p>
  <div class="dialog-btns"><button class="btn text">Cancel</button><button class="btn text danger">Delete playlist</button></div>
</div>

<div class="field"><label>Playlist name</label><div class="q">Rahman, slow<i class="caret"></i></div></div>
<div class="kbd">…30 × <i></i>, then <i class="w2"></i><i class="sp"></i><i class="w2"></i><i></i>…</div>   <!-- keyboard stand-in -->

<div class="menu" style="top:92px;right:12px">        <!-- place it beside its trigger -->
  <div class="menu-row"><span class="ms">edit</span>Rename</div>
  <div class="menu-row danger"><span class="ms">delete</span>Delete playlist</div>
</div>

<div class="toast">Added to Late drive</div>           <!-- above the dock. .over-tabs, .low (over an overlay) -->
```

A disabled confirm is `btn text off`. Toasts are text only, one at a time.

## Controls

```html
<span class="switch"></span>  <span class="switch on"></span>
<span class="radio"></span>   <span class="radio on"></span>
<div class="progress" style="--p:.66"></div>           <!-- .thick: 16dp -->
<span class="spinner"></span>                          <!-- .sm -->
```

## Settings

```html
<div class="label">Appearance</div>
<div class="group">
  <div class="set-row lead-ctl"><span class="radio on"></span><div class="txt"><b>Dark</b></div></div>
</div>
<div class="label">Song radio</div>
<div class="group">
  <div class="set-row"><div class="txt"><b>Autoplay</b><span>When a queue reaches its last song, radio keeps going with similar songs.</span></div><span class="switch on"></span></div>
  <div class="set-block">A paragraph inside the card.
    <div class="progress" style="--p:.66"></div>
    <small>Analysing songs: 84 of 127</small>
    <button class="btn text">Analyse now without a charger</button>
  </div>
</div>
```

Radios lead the row, switches trail it. Text in a `.set-row` wraps.

## Stats, empty state

```html
<div class="group"><div class="stats">
  <div class="stat"><b>18,460</b><span>Minutes</span></div>
  <div class="stat"><b>6,212</b><span>Plays</span></div>
</div></div>

<div class="empty">                                    <!-- .empty.fill centres it in the screen -->
  <div class="ibox sq neutral"><span class="ms">search</span></div>
  <h3>No results for "zzz"</h3>
  <p>Check the spelling, or try fewer words.</p>
  <button class="btn filled">…</button>                <!-- optional -->
</div>
```

## Bottom action bar, index scrubber

```html
<div class="actionbar">                                <!-- replaces the dock in a mode -->
  <a class="act"><span class="ms">add</span>Add songs</a>
  <a class="act go"><span class="ms">check</span>Done</a>      <!-- .go accent, .danger, .off -->
</div>

<div class="index" style="--top:150px"><span>A</span><span class="on">K</span>…</div>
<div class="index-pop" style="top:300px">K</div>
```

Give the list `margin-right:28px` when an index sits beside it.

## Album-art colour, in full

| On the element | Effect |
|---|---|
| `art-<key>` | supplies `--tint` (and draws the cover, if the element is a `.cv`) |
| `tinted` | `--accent`, `--on-accent`, `--accent-ink`, `--accent-soft`, `--on-accent-soft` and the mini player's two fills become the cover's colour |
| `tinted wash` | also `--ground`, `--card`, `--raised`, `--fill`, `--fill-2`, `--line`, `--on-2`, `--on-3` |

Both work on a `.phone`, a `.strip`, or anything inside one, in dark and light. A nested
element that should paint itself with the washed surface takes `bg-ground` or `bg-card`. A cover
inside a tinted container keeps its own artwork and does not change the container's colour.

```html
<div class="phone art-revenge tinted wash">…full-screen lyrics…</div>
<div class="strip art-viva tinted wash" style="width:340px">…a widget: use .bg-card, .mini, .play, .progress…</div>
```

Spend the colour on progress and on-state: fills, the play button, lit toggles, the current
line. Leave titles in `--on`.

## Names not to reuse inside a phone

`page.css` and `covers.css` own these: `intro date tags tag rowset col cap why phone long sbar
sys gpill strip spec sw cv round plain mosaic`. `light` is the theme switch.
