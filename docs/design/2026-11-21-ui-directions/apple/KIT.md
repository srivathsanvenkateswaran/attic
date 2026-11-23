# apple/ kit reference

`kit.css` holds the tokens and every shared component for this folder. You cannot edit it. Put
anything a single sheet needs in that sheet's `<style>`, written as `.phone .x{…}` or
`.strip .x{…}`; kit rules carry the specificity of one class, so a sheet rule always wins.

`00-direction.html` shows every component below, rendered. Open it next to this file.

## Sheet skeleton

```html
<!doctype html>
<meta charset="utf-8">
<title>Attic · Apple direction · 03 Library</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="../shared/page.css">
<link rel="stylesheet" href="../shared/covers.css">
<link rel="stylesheet" href="kit.css">
<style>/* sheet-only rules */</style>
```

`kit.css` imports Instrument Sans itself. No JavaScript, no images.

## Frame

```html
<div class="phone">                 <!-- add: light · long · lit art-<key> · np -->
  <div class="sbar">…</div>         <!-- copy from shared/fixtures.html -->
  <div class="page">…content…</div>
  <div class="dock">…</div>         <!-- mini player + tab bar; leave out on overlays -->
  <div class="gpill"></div>
</div>
```

| Class | Does |
|---|---|
| `.phone.light`, `.strip.light` | Light tokens. Dark is the default. |
| `.tone-dark`, `.tone-light` | Force a theme on a subtree (a dark widget on a light wallpaper). |
| `.page` | The content layer. Starts under the status bar, clipped by the frame, runs under the dock. |
| `.page.grouped` | Grouped-list background (Settings). Light theme drops to `#F2F0EC`. |
| `.phone.long` | Full scroll length. `.page` grows and leaves 176dp clear for the dock at the end. |
| `.scrolled` + `style="--y:320px"` | Wrap the content to show it scrolled by that much. Keep pinned bars outside it. |
| `.sys-light`, `.sys-dark` | On `.sbar` or `.gpill`: force white or dark glyphs. Normally not needed; both follow the surface's ink, including on `.lit`. |
| `.strip`, `.strip.bare` | A component on its own. 16dp padding, or none. Set a height when it holds absolute children (dock, sheet, alert). |

Side margin is `--gut` (20dp). Touch targets are 48dp or more. Controls drawn smaller
(`.chip` 36, `.seg` 40, `.btn.sm` 36) take the rest as invisible slop, so leave 6dp of space
around them.

## Colour rules

- Chrome is Soot or Paper, ink, and one accent. **Ember (`--accent`) marks things that respond
  to a tap** and nothing else: text buttons, tinted icons, the selected tab, a filled heart, a
  switch that is on, the check on a chosen option. Never colour a title with it.
- At most one `.btn.fill` per screen. Its partner is `.btn.tint`.
- The only gradients are the ones made from a cover (`.lit`, `.bloom`, `.art.glow`, `.wash`).
- Never write a hex in a component. Read tokens: `--bg --card --card-2 --ink --ink-2 --ink-3
  --line --fill --fill-strong --accent --accent-ink --accent-wash --danger --danger-wash`.
- Text helpers: `.dim` (secondary), `.faint` (tertiary), `.acc` (Ember), `.tn` (tabular figures).
- Type helpers: `.t-large` 34/40 · `.t-title` 22/27 · `.t-head` 17/22 · `.t-body` 16/21 ·
  `.t-sub` 14/18 · `.t-foot` 13/17 · `.t-cap` 11/13.
- No capitals for headings, no small label above a heading. A heading may have one explanatory
  line under it (`.sec p`).

## Material rules

- `.glass` is thin: tab bar, mini player, round buttons. `.glass.thick` is for sheets, menus,
  alerts and toasts.
- Glass floats clear of the screen edges and **must have content behind it**. In a strip, put
  rows in an absolutely positioned layer under it.
- No borders. Shadows only under covers and floating glass.

## Artwork colour

Needs an `art-<key>` class from `shared/covers.css` on the same element or an ancestor.

| Class | Use |
|---|---|
| `.lit` | The surface becomes a blurred field built from that cover. Ink, hairlines, fills and the accent switch to match: white on a dark field in dark theme, the cover's dark shade on a pale field in light theme. Every kit component placed inside adapts. |
| `.lit.deep` | The dark field in both themes. Use for the widget, the Recap card, browse tiles. |
| `.bloom` | On glass: artwork light from the leading edge. Tokens unchanged. Used by `.mini`. |
| `.art.glow` | Wrapper for a header cover; its colour falls on the page under it. |
| `.wash` | A faint radial halo behind a header. Prefer `.art.glow`; never a top-to-bottom gradient. |

```html
<div class="phone np lit art-revenge">…</div>             <!-- whole screen -->
<a class="recap lit deep art-revenge">…</a>               <!-- a card -->
<div class="art glow art-revenge"><div class="cv art-revenge"></div></div>
```

## Icons

`<i class="ic ic-heart"></i>`. Takes `currentColor`. 24dp by default; `style="--sz:20px"` to resize.

```
home home-fill mark search search-fill library library-fill chev chev-left chev-down pull
arrow-left more heart heart-fill play pause next prev shuffle repeat repeat-one queue lyrics
plus check close settings radio play-next add-queue add-playlist playlist language remove
album artist drag sort grid rows trash pencil save share folder note bars bars-off clock stop info
```

`mark` is the Attic logo as a glyph. `pull` is the close handle on full-screen overlays. `save`
and `share` are the Recap story actions. `rows` / `grid` are the Library view toggle.

## Pictures

```html
<span class="pic"><div class="cv art-viva"></div></span>
<span class="pic round"><div class="cv art-anbe"></div></span>        <!-- artist -->
<span class="pic"><div class="mosaic"><div class="cv art-viva"></div>…×4</div></span>
<span class="pic fav"></span>                                         <!-- Favourites -->
<span class="pic blank"></span>                                       <!-- no artwork -->
```

`.pic` takes its width from the component it sits in. Cover lettering is hidden in rows, the
mini player and sheet headers, and kept in shelves and grids.

## Navigation

```html
<!-- root screen: large title -->
<header class="lt">
  <h1>Library</h1>
  <span class="trail">
    <button class="ibtn acc" aria-label="Search your library"><i class="ic ic-search"></i></button>
    <button class="ibtn round" aria-label="Settings"><i class="ic ic-settings"></i></button>
  </span>
</header>

<!-- detail screen at the top: round glass buttons over the header, no title -->
<header class="nav over">
  <span class="lead"><button class="ibtn round" aria-label="Back"><i class="ic ic-chev-left"></i></button></span>
  <span class="ttl"></span>
  <span class="trail"><button class="ibtn round" aria-label="Playlist options"><i class="ic ic-more"></i></button></span>
</header>

<!-- collapsed: title on a blurred edge, content passing under -->
<header class="nav pinned">
  <span class="lead"><button class="ibtn" aria-label="Back"><i class="ic ic-chev-left"></i></button></span>
  <span class="ttl">Dhurandhar: The Revenge</span>
  <span class="trail"><button class="ibtn" aria-label="More"><i class="ic ic-more"></i></button></span>
</header>
```

`.nav` alone is a 52dp bar in the flow (Settings, Recap, Queue: back or `chev-down`, title, an
optional `.btn.plain` such as "Clear"). `.nav.over` and `.nav.pinned` are absolute, cover the
status area, and go outside `.scrolled`. Follow a `.nav` with `.lt` when a pushed screen wants
a large title.

## Dock: tab bar and mini player

```html
<div class="dock">
  <div class="mini glass bloom art-revenge">
    <span class="pic"><div class="cv art-revenge"></div></span>
    <span class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></span>
    <button class="heart" aria-label="Add to favourites"></button>
    <button class="ring" style="--v:.323" aria-label="Pause"><i class="ic ic-pause"></i></button>
  </div>
  <nav class="tabbar glass">
    <a class="tab on"><i class="ic ic-home-fill"></i>Home</a>
    <a class="tab"><i class="ic ic-search"></i>Search</a>
    <a class="tab"><i class="ic ic-library"></i>Library</a>
  </nav>
</div>
```

- Selected tab: `.on` and the `-fill` icon. Now playing everywhere is 1:47 of 5:31, `--v:.323`.
- Paused: `ic-play` in the ring. Favourite: `.heart.on`. Long title: `.txt.run` (drawn mid-scroll,
  fading at the trailing end).
- The dock stays on Settings and Recap. It is absent on Now Playing, Queue, Lyrics, the Recap
  story and the permission screen.
- A toast sits above it: `.toast` defaults to 162dp from the bottom; `.toast.low` for overlays.

## Filters and fields

```html
<div class="seg"><span class="on">All</span><span>Albums</span><span>Artists</span><span>Folders</span></div>

<div class="chips"><span class="chip on">Playlists</span><span class="chip">Albums</span></div>

<div class="search"><i class="ic ic-search"></i><span class="q"><span class="ph">Search your music</span></span></div>
<div class="search"><i class="ic ic-search"></i><span class="q">dhur<i class="caret"></i></span>
  <button class="clear" aria-label="Clear search"></button></div>

<label class="field-label">Playlist name</label>
<div class="field focus">Rahman, slow<i class="caret"></i></div>
<div class="field"><span class="ph">Playlist name</span></div>

<div class="listbar">
  <button class="btn plain"><i class="ic ic-sort"></i>Title, 127 songs</button>
  <button class="ibtn acc" aria-label="Shuffle all songs"><i class="ic ic-shuffle"></i></button>
</div>
```

`.seg` is for a choice that always has a value (Home filter, Recap period). `.chips` is for
filters that can all be off (Library). Neither uses the accent.

## Section header

```html
<div class="sec"><div><h2>Start listening</h2><p>Picked at random from your songs</p></div></div>
<div class="sec"><div><h2>More from A. R. Rahman<i class="ic ic-chev"></i></h2></div></div>   <!-- links -->
<div class="sec"><div><h2>Top songs</h2></div><button class="btn sm tint"><i class="ic ic-play"></i>Play</button></div>
<div class="sec sm"><div><h2>Up next, 9 songs</h2></div></div>                                <!-- quiet -->
```

## Rows

Rows draw a hairline from the text edge to the trailing edge; the last row in its parent has
none. Add `.nosep` to drop one. Wrap runs of rows in any block (`<div class="list">`).

```html
<!-- song -->
<div class="row">
  <span class="pic"><div class="cv art-viva"></div></span>
  <span class="txt"><b>Viva La Vida</b><span>Coldplay</span></span>
  <button class="ibtn" aria-label="More options for Viva La Vida"><i class="ic ic-more"></i></button>
</div>

<!-- playing: add .now (and .paused). The cover dims under three bars; the title is not coloured -->
<div class="row now">…</div>

<!-- numbered, no cover (album). .now swaps the number for the bars -->
<div class="row num"><span class="n">3</span><span class="txt"><b>…</b><span>…</span></span><button class="ibtn">…</button></div>

<!-- rank + cover + trailing count (Recap top songs; artist "Top songs" without the count) -->
<div class="row rank"><span class="n">1</span><span class="pic">…</span><span class="txt">…</span><span class="end">214 plays</span></div>

<!-- drag handle (queue, playlist reorder); .lift is the row in the hand -->
<div class="row">… <button class="ibtn" aria-label="Drag Viva La Vida to reorder"><i class="ic ic-drag"></i></button></div>
<div class="row lift">…</div>

<!-- swiped to remove (queue) -->
<div class="swipe"><span class="lbl">Remove</span><div class="row">…</div></div>

<!-- add / added (Add songs sheet) -->
<div class="row">… <button class="tick" aria-label="Add Aaruyire"></button></div>
<div class="row">… <button class="tick on" aria-label="Remove Aaruyire"></button></div>

<!-- album / artist / playlist / folder: taller, chevron, no menu -->
<a class="row lg link"><span class="pic round"><div class="cv art-anbe"></div></span>
  <span class="txt"><b>A. R. Rahman</b><span>Artist</span></span></a>

<!-- category: Ember icon, label, optional count, chevron -->
<a class="cat"><i class="ic ic-album"></i><span class="lbl">Albums</span><span class="val">16</span></a>
<!-- the same with a 44dp picture in place of the icon -->
<a class="cat tall"><span class="pic"><div class="mosaic">…</div></span><span class="lbl">Hindi</span><span class="val">46 songs</span></a>
```

`.end` holds any trailing text (a duration, a count, a small icon). A standalone playing mark
is `<i class="bars"></i>` / `<i class="bars paused"></i>`.

## Shelves, grids, tiles

```html
<div class="shelf">            <!-- 148dp covers. .sm 108dp, .lg 164dp, or style="--w:120px" -->
  <a class="card"><span class="pic"><div class="cv art-viva"></div></span><b>Viva la Vida</b><span>Coldplay</span></a>
  <a class="card round"><span class="pic"><div class="cv art-anbe"></div></span><b>A. R. Rahman</b></a>
  <a class="card now">…</a>     <!-- playing mark on the cover -->
</div>

<div class="grid">…cards…</div>        <!-- two columns ("Recently added", Home › Albums) -->
<div class="grid c3">…cards…</div>     <!-- three columns (Library grid view, Home shortcuts) -->

<div class="pager">                    <!-- columns of rows that page sideways; the next column shows at the edge -->
  <div class="colm">…three .row…</div><div class="colm">…</div>
</div>

<div class="browse">                   <!-- Search tiles -->
  <a class="tile lit deep art-viva"><b>English</b><span class="pic"><div class="mosaic">…</div></span></a>
</div>
```

## Collection and artist headers

```html
<div class="hero">
  <div class="art glow art-revenge"><div class="cv art-revenge"></div></div>
  <h1>Dhurandhar: The Revenge</h1>
  <a class="by">Shashwat Sachdev</a>           <!-- Ember: it links to the artist -->
  <p class="meta">Album · 2026</p>
</div>
<div class="actions">
  <button class="btn fill"><i class="ic ic-play"></i>Play</button>
  <button class="btn tint"><i class="ic ic-shuffle"></i>Shuffle</button>
  <button class="btn gray icon" aria-label="Add Dhurandhar: The Revenge to a playlist"><i class="ic ic-add-playlist"></i></button>
</div>
<p class="foot">6 songs, 26 min</p>

<div class="hero artist">                       <!-- round crop, name as a large title -->
  <div class="art glow art-anbe"><div class="cv plain art-anbe"></div></div>
  <h1>A. R. Rahman</h1>
  <p class="meta">14 songs, 1 hr 13 min</p>
</div>
```

- `.hero` leaves 60dp at the top for `.nav.over`. For a mosaic, put one of its art classes on
  `.art` so the glow has a source, and a `.mosaic` inside.
- When this collection is the one playing, "Play" becomes `ic-pause` + "Pause". Shuffle on for
  this collection: swap `.tint` for `.fill` on Shuffle and make Play `.tint`.
- Playlist extras ("Add songs", "Reorder songs" / "Done reordering") are more `.btn.gray.icon`
  buttons in the same row, or `.btn.sm` chips under it.

## Buttons and small controls

```html
<button class="btn fill">Allow access</button>
<button class="btn tint">Download lyrics</button>
<button class="btn gray">Cancel</button>
<button class="btn plain">Clear</button>            <button class="btn plain danger">Delete playlist</button>
<button class="btn sm tint"><i class="ic ic-play"></i>Play recap</button>
<button class="btn fill off">Create</button>        <!-- disabled -->
<button class="btn on-art">Open lyrics</button>     <!-- white, for .lit surfaces; pair with .sm -->
<button class="btn fill block">…</button>           <!-- full width -->

<button class="ibtn"><i class="ic ic-more"></i></button>           <!-- 48dp icon button: .acc, .dim -->
<button class="ibtn round"><i class="ic ic-chev-left"></i></button> <!-- 38dp glass circle inside it -->

<button class="heart" aria-label="Add to favourites"></button>
<button class="heart on" aria-label="Remove from favourites"></button>

<i class="switch"></i>  <i class="switch on"></i>
<div class="prog" style="--v:.66"></div>
<i class="spin"></i>                                 <!-- style="--sz:22px" to resize -->
```

## Now Playing

```html
<div class="phone np lit art-revenge">
  <div class="sbar">…</div>
  <div class="page">
    <div class="np-page">                      <!-- the player: one screen tall -->
      <header class="np-top">
        <button class="ibtn" aria-label="Close player"><i class="ic ic-pull"></i></button>
        <p class="ctx">Playing from <b>Dhurandhar: The Revenge</b></p>
      </header>
      <div class="np-art"><div class="cv art-revenge"></div></div>      <!-- .paused shrinks it -->
      <p class="np-line"><i class="ic ic-lyrics"></i><span>I carry what the night forgot</span></p>
      <div class="np-title">
        <span class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></span>
        <button class="heart"></button>
        <button class="ibtn"><i class="ic ic-more"></i></button>
      </div>
      <div class="scrub" style="--v:.323">     <!-- .drag while scrubbing -->
        <div class="bar"></div><div class="times"><span>1:47</span><span>5:31</span></div>
      </div>
      <div class="transport">
        <button aria-label="Previous"><i class="ic ic-prev"></i></button>
        <button class="main" aria-label="Pause"><i class="ic ic-pause"></i></button>
        <button aria-label="Next"><i class="ic ic-next"></i></button>
      </div>
      <div class="np-tools">
        <button class="tool"><i class="ic ic-shuffle"></i></button>     <!-- .on = lozenge -->
        <button class="tool"><i class="ic ic-queue"></i></button>
        <button class="tool on"><i class="ic ic-repeat-one"></i></button>
      </div>
    </div>
    …cards…
  </div>
  <div class="gpill"></div>
</div>
```

Keep an empty `<p class="np-line"></p>` when there is no synced line, so the layout holds.
Radio context reads `Playing from <b>Radio · Jaan Se Guzarte Hain</b>`.

Cards under the player, and the compact header:

```html
<section class="np-card">
  <h3>Lyrics</h3>
  <div class="lyr"><span class="cur">line being sung</span><span>next</span><span>next</span><span>next</span></div>
  <button class="btn sm on-art">Open lyrics</button>
</section>
<!-- other lyric states use the same card:
     plain      <div class="lyr plain">text, cut at four lines</div> + the button
     none yet   <p>Download once and they stay on this phone.</p><button class="btn sm on-art">Download lyrics</button>
     busy       the same <p>, then <i class="spin" style="--sz:26px;margin-top:16px"></i>
     not found  <p>No lyrics were found for this song.</p> + the button
     instrumental <p>This song is instrumental.</p> -->

<a class="np-card about">
  <span class="img"><h3>About the artist</h3><div class="cv art-revenge"></div></span>
  <span class="body"><span class="txt"><b>Khan Saab</b><span>3 songs in your library</span></span><i class="ic ic-chev"></i></span>
</a>

<section class="np-card credits">
  <h3>Credits</h3>
  <dl><div><dt>Performed by</dt><dd>Khan Saab, Shashwat Sachdev</dd></div><div><dt>Album</dt><dd>…</dd></div></dl>
</section>

<header class="np-bar">                         <!-- sibling of .page, after it -->
  <span class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></span>
  <button class="heart"></button>
  <button class="ring" style="--v:.323" aria-label="Pause"><i class="ic ic-pause"></i></button>
</header>
```

Full-screen lyrics, on a `.phone.lit` frame:

```html
<span class="ll sung">a line already sung</span>     <!-- sharp, half strength -->
<span class="ll cur">the line being sung</span>      <!-- white -->
<span class="ll">the next line</span>                <!-- dim, slightly blurred -->
<span class="ll far">later lines</span>              <!-- dimmer, more blur -->
<span class="ll plain">unsynced text</span>          <!-- no highlighting -->
```

The Queue follows the app theme (no `.lit`): `.nav` with `ic-chev-down`, the title and
`.btn.plain` "Clear"; `.sec.sm` headings; a `.row.now` without a handle; `.row` with `ic-drag`.

## Sheets, menus, alerts, toast

```html
<div class="scrim"></div>
<div class="sheet glass thick">                 <!-- attached to the bottom edge; set top:… for a tall one -->
  <div class="sheet-head"><span class="pic"><div class="cv art-revenge"></div></span>
    <span class="txt"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></span></div>
  <button class="mrow"><span class="lbl">Start radio</span><i class="ic ic-radio"></i></button>
  <button class="mrow"><span class="lbl">Language</span><span class="val">Hindi</span><i class="ic ic-language"></i></button>
  <button class="mrow danger"><span class="lbl">Remove from this playlist</span><i class="ic ic-remove"></i></button>
</div>

<div class="sheet glass thick">
  <div class="sheet-title">Song language</div>  <!-- picker title; may hold a trailing .btn.plain -->
  <button class="mrow opt on"><span class="lbl">Hindi</span></button>    <!-- .opt.on shows the check -->
  <button class="mrow opt"><span class="lbl">Tamil</span></button>
</div>

<div class="menu glass thick" style="right:12px;top:92px">    <!-- pop-over; position it yourself -->
  <button class="mrow"><span class="lbl">Rename</span><i class="ic ic-pencil"></i></button>
  <button class="mrow danger"><span class="lbl">Delete playlist</span><i class="ic ic-trash"></i></button>
</div>
<!-- <div class="msep"></div> between groups of menu rows -->

<div class="scrim"></div>
<div class="alert glass thick">
  <h2>Delete Late drive?</h2>
  <p>The playlist goes away. The songs stay in your library.</p>
  <div class="btns"><button class="btn gray">Cancel</button><button class="btn fill danger">Delete playlist</button></div>
</div>
<!-- name dialog: <h2>, .field-label, .field.focus, .btns with .btn.fill (add .off while blank) -->

<div class="toast glass thick">Added to queue</div>
```

Labels lead and icons trail in every menu row. `.mrow.acc` colours a row Ember ("New playlist").

## Settings

```html
<div class="page grouped">
  <header class="nav">…back…<span class="ttl"></span><span class="trail"></span></header>
  <header class="lt"><h1>Settings</h1></header>

  <p class="group-head">Appearance</p>
  <div class="group">
    <div class="cell opt"><span class="txt">Match phone</span></div>
    <div class="cell opt on"><span class="txt">Dark</span></div>
  </div>

  <p class="group-head">Song radio</p>
  <div class="group">
    <div class="cell"><span class="txt">Autoplay<span>When a queue reaches its last song, radio keeps going with similar songs.</span></span><i class="switch on"></i></div>
    <div class="cell stack"><div class="prog" style="--v:.66"></div><p>Analysing songs: 84 of 127</p></div>
    <button class="cell act">Analyse now without a charger</button>
  </div>
  <p class="group-foot">Radio finds similar songs by listening to your files. …</p>
</div>
```

`.cell .val` is a trailing value. A leading `.ic` in a cell is Ember.

## Empty state, stats, Recap entry

```html
<div class="empty">                       <!-- .mid centres it in the frame -->
  <i class="ic ic-search"></i>
  <h2>No results for "dhurr"</h2>
  <p>Check the spelling, or try fewer words.</p>
  <button class="btn tint">Browse</button>      <!-- only when there is somewhere to go -->
</div>

<div class="stats">
  <div class="stat"><b>18,460</b><span>Minutes</span></div><div class="stat"><b>6,212</b><span>Plays</span></div>
</div>

<a class="recap lit deep art-revenge">
  <span class="pic"><div class="mosaic">…</div></span>
  <span class="txt"><b>Your 2026 so far</b><span>18,460 minutes. See your top songs and cards to share.</span></span>
  <i class="ic ic-chev"></i>
</a>
```

For the permission screen use `.empty.mid` with `<i class="ic ic-mark" style="--sz:72px;color:var(--accent)">`
and a `.btn.fill`. Loading is a centred `.spin`.

## Names

Use the shared table in `BRIEF.md`. In this folder also: the empty Favourites body reads "Tap
the heart on a song to save it." (today: "Tap the plus…"), and metadata joins with " · " or a
comma, never "•".
