# pixel/kit.css reference

Material 3 Expressive as Pixel ships it, applied to Attic. `00-direction.html` shows every
component below as a strip; open it next to this file. `01-home.html` and
`05-now-playing.html` are the worked examples.

A sheet links `../shared/page.css`, `../shared/covers.css`, `kit.css`, in that order. The kit
imports its own fonts (Google Sans Flex, Roboto Flex as fallback, Material Symbols Rounded), so
a sheet needs no font link. Add these two lines to the head so the fonts connect early:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
```

Do not edit `kit.css`. Sheet-specific rules go in the sheet's `<style>` and use the role
variables, never a hex.

## The rules, short

**Colour.** The app scheme and the art scheme share one set of role names.

- The **app scheme** is the wallpaper's. It is on `.phone` / `.strip` by default (dark) and
  with `.light`. Screens are `--surface`; a list row is `--surface-container`; pills and the
  search bar are `--surface-container-high`. `--primary` is the one filled action on a screen
  and selected states. `--secondary-container` is the nav indicator, a selected chip and the row
  that is playing. `--tertiary-container` is Favourites and nothing else.
- The **art scheme** is the cover's. Add `.from-art` to any element that has, or sits inside,
  an `.art-<key>` class and every role is rebuilt from that cover. Use it on player surfaces
  only: mini player, Now Playing, queue, full-screen lyrics, collection and artist headers,
  browse tiles, the widget, the media notification. Recap story cards should also use it (or
  fixed roles of your own), because a shared image must not depend on the viewer's wallpaper.
- Separate surfaces by container role, not by shadow. Only `.menu`, `.snack`, `.fab` and a
  `.row.lifted` have a shadow. No gradients, and no text over artwork.

**Shape.**

- Albums, playlists and folders are rounded squares. Radius by size: 8 up to 48dp, 12 up to 64,
  16 up to 128, 20 up to 180, 28 above. The kit sets these inside its components; set
  `border-radius:var(--r-…)` yourself for a one-off.
- Artists are the cookie: add `.cookie` to the `.cv`. Do not use `.cv.round` from `covers.css`.
- Buttons, bars and pills are fully round.
- **Squared off means on.** The play control while playing, a selected toggle (`.ib.on`).
- A list is always a `.group`: rows 2dp apart, outer corners 20, inner corners 4.

**Type.** Emphasised (`.em`: heavier, fully rounded) is for the one line a block is about. A
title that does not fit narrows before it truncates: add `.w75`, then `.w50`.

**Playing.** A wavy line and a squared-off control. Paused: a flat line and a round control.
Apply both together: `.seek` + `.pp.playing` + `.wave`, or `.seek.paused` + `.pp` + `.wave.still`.

**Names.** Use the brief's table: Library, Favourites, Recap, Instant mixes, "Search your
music", "Top songs", "Open lyrics".

## Tokens

| Group | Variables |
|---|---|
| Accent roles | `--primary` `--on-primary` `--primary-container` `--on-primary-container`, and the same four for `secondary`, `tertiary`, `error` |
| Surfaces | `--surface` `--surface-dim` `--surface-bright` `--surface-container-lowest` `-low` (none) `-high` `-highest` |
| Content and lines | `--on-surface` `--on-surface-variant` `--outline` `--outline-variant` |
| Inverse | `--inverse-surface` `--inverse-on-surface` `--inverse-primary` `--scrim` |
| Corners | `--r-xs` 4 · `--r-s` 8 · `--r-m` 12 · `--r-l` 16 · `--r-li` 20 · `--r-xl` 28 · `--r-xli` 32 · `--r-xxl` 48 · `--r-full` |
| Shapes | `--shape-cookie` `--shape-burst` (clip-path polygons) |
| Other | `--font` · `--elev-2` `--elev-3` (shadows) · `--wave` `--wave-s` (mask images) |

Type helpers: `.t-display` 45/52 · `.t-display-s` 36/44 · `.t-headline-l` 32/40 · `.t-headline`
28/36 · `.t-headline-s` 24/32 · `.t-title-l` 22/28 · `.t-title` 16/24 · `.t-title-s` 14/20 ·
`.t-body-l` 16/24 · `.t-body` 14/20 · `.t-body-s` 12/16 · `.t-label-l` 14/20 · `.t-label` 12/16 ·
`.t-label-s` 11/16. Modifiers: `.em` emphasised, `.dim` (`on-surface-variant`), `.clip1` one line
with an ellipsis, `.w75` / `.w50` narrower.

## Frame skeleton and the status bar

```html
<div class="phone">                      <!-- add .light; add .from-art art-revenge for a player screen -->
  <div class="sbar">…</div>              <!-- markup from shared/fixtures.html -->
  <div class="page clear-dock">          <!-- clear-dock: room for nav + mini; clear-nav: nav only -->
    …
  </div>
  <div class="dock">
    <div class="mini from-art art-revenge">…</div>
    <nav class="nav">…</nav>
  </div>
  <div class="gpill"></div>
</div>
```

**Status-bar colouring.** The status bar and gesture pill inherit `color` from the phone, so they
follow the theme and, on a `.from-art` phone, the cover. A surface that should run under the
status bar adds `.under-sbar` (`.chead`, `.np-sticky` and `.appbar.scrolled` already do this
themselves). Nothing else is needed.

For `.phone.long`, the dock still sits at the bottom of the frame.

## Icons, the mark, the wave

```html
<span class="ms">home</span>             <!-- Material Symbols Rounded, by ligature name -->
<span class="ms fill">favorite</span>    <!-- filled = selected -->
<span class="ms s18">check</span>        <!-- sizes: s18 s20 (24 default) s28 s32 s40 -->

<svg class="mark" viewBox="20 20 68 68"><path fill-rule="evenodd" d="M54,25L84,53L84,79Q84,83 80,83L28,83Q24,83 24,79L24,53ZM44,60a10,10 0 1,0 20,0a10,10 0 1,0 -20,0Z"/></svg>
<!-- the Attic mark; 24dp, drawn in currentColor; set width/height and color on it or its parent -->

<i class="wave"></i>                     <!-- playing; .wave.still when paused -->
```

If an icon name renders as a word, the name is wrong; check it on fonts.google.com/icons.

## Covers

```html
<div class="cv art-revenge"></div>                 <!-- album; width sets the size -->
<div class="cv plain art-revenge"></div>           <!-- without the drawn title, for 64dp and below -->
<div class="cv plain cookie art-anbe"></div>       <!-- artist -->
<div class="mosaic"><div class="cv art-oso"></div><div class="cv art-yjhd"></div><div class="cv art-lootera"></div><div class="cv art-rockstar"></div></div>
<div class="fav-art"><span class="ms fill">favorite</span></div>   <!-- Favourites -->
<div class="no-art"><span class="ms">music_note</span></div>       <!-- a file with no cover -->
```

## Buttons

```html
<button class="btn filled"><span class="ms fill">play_arrow</span>Play</button>
<button class="btn tonal">Open lyrics</button>
<button class="btn outlined">Download lyrics</button>
<button class="btn text">Cancel</button>
<button class="btn text off">Create</button>            <!-- .off = disabled -->
<button class="btn filled m">…</button>                  <!-- .m = 56dp; .wide fills the row; .sq = squared -->

<button class="ib"><span class="ms">more_vert</span></button>          <!-- icon button, 48 target -->
<button class="ib tonal">…</button>  <button class="ib filled">…</button>  <button class="ib outlined">…</button>
<button class="ib m tonal">…</button>                    <!-- 56dp; add .narrow or .wide -->
<button class="ib on"><span class="ms">shuffle</span></button>          <!-- a toggle that is on -->

<button class="ib heart"><span class="ms">favorite</span></button>      <!-- favourite, off -->
<button class="ib heart on"><span class="ms">favorite</span></button>   <!-- favourite, on -->

<div class="btn-group">…buttons…</div>
<button class="fab"><span class="ms">add</span></button>
<button class="fab ext"><span class="ms">add</span>New playlist</button>
<span class="split"><span><span class="ms">sort</span>Title</span><span><span class="ms">keyboard_arrow_down</span></span></span>
<div class="toolbar">…icon buttons…</div>                <!-- .vibrant, .floating -->
```

## Play control

```html
<button class="pp"><span class="ms">play_arrow</span></button>               <!-- paused, 36dp -->
<button class="pp playing"><span class="ms">pause</span></button>            <!-- playing -->
<button class="pp ring playing"><span class="ms">pause</span></button>       <!-- with the progress ring -->
<button class="pp l playing">…</button>                                      <!-- 56dp -->
<button class="pp xl playing">…</button>                                     <!-- Now Playing: 128×80, 152×80 when paused -->
```

`.ring` is for places with no seek bar: mini player, compact header, widget, notification. The
ring is drawn for 1:47 of 5:31 and cannot show another position. `.pp.tonal` is a quieter fill.

## Navigation bar and mini player

```html
<nav class="nav">
  <a class="nav-item on"><span class="ind"><span class="ms">home</span></span>Home</a>
  <a class="nav-item"><span class="ind"><span class="ms">search</span></span>Search</a>
  <a class="nav-item"><span class="ind"><span class="ms">library_music</span></span>Library</a>
</nav>

<div class="mini from-art art-revenge">
  <div class="cv plain art-revenge"></div>
  <div class="mini-meta"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div>
  <button class="ib heart"><span class="ms">favorite</span></button>
  <button class="pp ring playing"><span class="ms">pause</span></button>
</div>
```

Mini player states: playing (`.pp.ring.playing`, `pause`), paused (`.pp.ring`, `play_arrow`),
favourite (`.ib.heart.on`), a long artist line (`<span class="marquee">`; the title ellipsizes
on its own), light (inherits from the frame). The mini player is on every destination,
including Settings and Recap, 8dp above the nav bar. It is absent only when the queue is empty.

## App bars

```html
<header class="appbar root">                                  <!-- tab root -->
  <h1 class="title">Library</h1>                              <!-- Home puts the mark before "Attic" -->
  <button class="ib"><span class="ms">add</span></button>
  <button class="ib"><span class="ms">settings</span></button> <!-- settings is the last action on every root -->
</header>

<header class="appbar">                                       <!-- detail -->
  <button class="ib"><span class="ms">arrow_back</span></button>
  <h1 class="title">Settings</h1>
</header>

<header class="appbar scrolled">…</header>                    <!-- after scrolling: container colour, runs under the status bar -->
<header class="appbar root scrolled">…</header>               <!-- root, collapsed: 22sp title -->
<div class="appbar over">…</div>                              <!-- over artwork or a header block; buttons get a scrim -->
```

A collection screen that has scrolled past its header uses `.appbar.scrolled.from-art` with the
collection's `art-<key>` and its title (frame F of sheet 04).

## Search bar, chips, connected group, section header

```html
<div class="searchbar"><span class="ms">search</span><span class="q">Search your music</span></div>
<div class="searchbar"><span class="ms">search</span><span class="q typed">dhur</span><button class="ib"><span class="ms">close</span></button></div>

<div class="chips">                                           <!-- filters that can be cleared -->
  <a class="chip">Playlists</a>
  <a class="chip on"><span class="ms">check</span>Albums</a>
</div>

<div class="seg">                                             <!-- one of N, always one selected -->
  <a class="seg-item on"><span class="ms">check</span>All</a>
  <a class="seg-item">Albums</a><a class="seg-item">Artists</a><a class="seg-item">Folders</a>
</div>

<div class="sec"><div><h3>Recently played</h3></div></div>
<div class="sec"><div><p class="over">Picked at random from your songs</p><h3>Start listening</h3></div></div>
<div class="sec"><div><h3>Top songs</h3></div><button class="pp l"><span class="ms">play_arrow</span></button></div>   <!-- trailing control -->
<p class="list-label">Up next, 9 songs</p>                    <!-- small heading directly above a group -->
```

Home's filter and Recap's period switch are `.seg`. Library's filters are `.chips`.

## Carousel, shelf, tile, grid

```html
<div class="hero">                                            <!-- Home: Favourites pinned + covers + one caption -->
  <a class="hero-fav"><span class="ms fill s28">favorite</span><span><b>31</b><small>songs</small></span></a>
  <div class="hero-main">
    <div class="hero-items">
      <a class="hero-item lg"><div class="cv art-revenge"></div></a>
      <a class="hero-item md"><div class="cv plain art-dhurandhar"></div></a>
      <a class="hero-item sm"><div class="cv plain art-yjhd"></div></a>
    </div>
    <a class="hero-cap"><b>Dhurandhar: The Revenge</b><span>Shashwat Sachdev, 2026</span></a>
  </div>
</div>

<div class="shelf">                                           <!-- one row, runs off the right edge -->
  <a class="tile"><div class="cv art-viva"></div><b>Viva la Vida</b><span>Coldplay</span></a>
  <a class="tile s" style="--w:112px">…</a>                   <!-- .s = smaller radius, for tiles under 128dp -->
  <a class="tile artist" style="--w:112px"><div class="cv plain cookie art-anbe"></div><b>A. R. Rahman</b></a>
  <a class="tile now">…<b><i class="wave"></i>Jaan Se Guzarte Hain</b>…</a>   <!-- the song that is playing -->
</div>

<div class="grid">…tiles…</div>                               <!-- two columns; .grid.c3 = three -->
```

Default tile width is 148dp; set `--w` to change it. A tile holds a `.cv`, a `.mosaic`, a
`.fav-art` or a `.no-art`.

## Instant-mix pills, Recap card, spotlight, browse tile, stat tile

```html
<div class="mixes">
  <div class="mix-row"><a class="mix"><div class="cv art-rockstar"></div>Kun Faya Kun</a>…</div>
  <div class="mix-row">…</div><div class="mix-row">…</div>
</div>                                                        <!-- .mix.now = the seed of the radio that is playing -->

<a class="recap-card">
  <div><h3>Your 2026 so far</h3><p>18,460 minutes. See your top songs and cards to share.</p></div>
  <div class="mosaic">…</div>
</a>

<section class="spot">
  <a class="spot-head"><div class="cv plain cookie art-anbe"></div><div><small>More from</small><b>A. R. Rahman</b></div><button class="ib"><span class="ms">arrow_forward</span></button></a>
  <div class="shelf">…tiles…</div>
</section>

<div class="browse-grid">
  <a class="browse from-art art-viva"><b>English</b><div class="mosaic">…</div></a>
  <a class="browse from-art art-revenge"><b>Dhurandhar: The Revenge</b><div class="cv plain art-revenge"></div></a>
</div>

<div class="stats">
  <div class="stat accent"><b>18,460</b><span>Minutes</span></div>
  <div class="stat"><b>6,212</b><span>Plays</span></div>
</div>
```

## Lists

```html
<div class="group">
  <!-- song with cover -->
  <div class="row"><span class="lead"><div class="cv plain art-viva"></div></span>
    <div class="row-text"><b>Viva La Vida</b><span>Coldplay</span></div>
    <button class="ib"><span class="ms">more_vert</span></button></div>

  <!-- the song that is playing: .now, and the wave over the cover (.wave.still when paused) -->
  <div class="row now"><span class="lead"><div class="cv plain art-revenge"></div><i class="wave"></i></span>
    <div class="row-text"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div>
    <button class="ib"><span class="ms">more_vert</span></button></div>

  <!-- numbered, no cover (album tracks) -->
  <div class="row"><span class="lead num">4</span><div class="row-text">…</div>…</div>
  <!-- numbered and playing: the wave replaces the number -->
  <div class="row now"><span class="lead num"><i class="wave"></i></span>…</div>

  <!-- rank + cover + play count (artist "Top songs", Recap) -->
  <div class="row"><span class="rank">2</span><span class="lead"><div class="cv plain art-rockstar"></div></span>
    <div class="row-text">…</div><span class="count">187 plays</span></div>

  <!-- trailing variants -->
  … <button class="ib"><span class="ms">drag_handle</span></button>      <!-- reorder -->
  … <button class="ib"><span class="ms">add</span></button>              <!-- add to playlist -->
  … <button class="ib on"><span class="ms">check</span></button>         <!-- already added -->
</div>

<div class="row lifted">…</div>                               <!-- being dragged -->
<div class="swipe"><div class="row">…</div><span class="swipe-label"><span class="ms">delete</span>Remove</span></div>   <!-- mid-swipe -->

<!-- collection rows: the leading shape says what kind it is -->
<div class="group">
  <a class="row coll"><span class="lead"><div class="fav-art"><span class="ms fill">favorite</span></div></span><div class="row-text"><b>Favourites</b><span>31 songs</span></div></a>
  <a class="row coll"><span class="lead"><div class="mosaic">…</div></span><div class="row-text"><b>Hindi</b><span>Folder, 46 songs</span></div></a>
  <a class="row coll"><span class="lead"><div class="cv plain cookie art-anbe"></div></span><div class="row-text"><b>A. R. Rahman</b><span>14 songs</span></div></a>
  <a class="row coll"><span class="lead"><div class="cv plain art-revenge"></div></span><div class="row-text"><b>Dhurandhar: The Revenge</b><span>Shashwat Sachdev</span></div></a>
</div>
```

A **queue row** is a `.row` with a `drag_handle`; the current song is a `.row.now` in its own
group under `<p class="list-label">Now playing</p>`. Put the queue on a `.phone.from-art`.

Any element can be a group child and gets the cell colour and corners: a `.row`, a `.set-row`,
a `.set-note`, a `.swipe`. Two groups in a row are 12dp apart.

## Collection and artist header

```html
<header class="chead from-art art-revenge">                   <!-- first thing after .sbar; runs under it -->
  <div class="appbar"><button class="ib"><span class="ms">arrow_back</span></button><span class="title"></span><button class="ib"><span class="ms">more_vert</span></button></div>
  <div class="cv art-revenge"></div>                          <!-- or .mosaic, or .fav-art -->
  <h1>Dhurandhar: The Revenge</h1>
  <a class="byline"><div class="cv plain cookie art-revenge"></div>Shashwat Sachdev</a>
  <p class="meta">Album, 2026. 6 songs, 26 min</p>
  <div class="cactions">
    <button class="btn filled m"><span class="ms fill">play_arrow</span>Play</button>
    <button class="ib m tonal"><span class="ms">shuffle</span></button>       <!-- .on while shuffling this collection -->
    <button class="ib m tonal"><span class="ms">playlist_add</span></button>
  </div>
</header>

<header class="chead artist from-art art-anbe">               <!-- artist: cookie, display-size name, no byline -->
  <div class="appbar">…</div>
  <div class="cv plain cookie art-anbe"></div>
  <h1>A. R. Rahman</h1>
  <p class="meta">14 songs, 1 hr 13 min</p>
  <div class="cactions">…</div>
</header>
```

While the collection is the one playing, the button reads "Pause" with the `pause` glyph and
adds `.sq` (squared off means on).
Playlists add `ib m tonal` buttons for add songs (`add`) and reorder (`swap_vert`, `check` when
done) to `.cactions`. The list under the header is on the app scheme, not the cover's.

## Now Playing

The frame is `<div class="phone from-art art-revenge">`.

```html
<header class="np-top">
  <button class="ib"><span class="ms">keyboard_arrow_down</span></button>
  <div class="np-ctx"><small>Playing from</small><b><span class="ms">album</span>Dhurandhar: The Revenge</b></div>
  <button class="ib"><span class="ms">more_vert</span></button>
</header>                                                     <!-- context glyphs: album, person, folder, playlist_play, favorite, sensors (radio) -->

<div class="np-pager">                                        <!-- .paused: cover steps back and rounds off -->
  <div class="np-cover"><div class="cv art-revenge"></div></div>
  <div class="np-next"><div class="cv plain art-revenge"></div></div>   <!-- next queue entry -->
</div>

<div class="np-info">
  <p class="np-lyric"><span class="ms s18">lyrics</span><span>I carry the lamp you left alight</span></p>   <!-- only with synced lyrics -->
  <h1 class="np-title">Jaan Se Guzarte Hain</h1>              <!-- add .w75 / .w50 if it does not fit -->
  <a class="np-artist">Khan Saab, Shashwat Sachdev</a>
</div>

<div class="seek" style="--p:32.3%"><i class="seek-on"></i><i class="seek-thumb"></i><i class="seek-off"></i></div>   <!-- .seek.paused = flat -->
<div class="seek-times"><span>1:47</span><span>5:31</span></div>

<div class="transport">
  <button class="skip"><span class="ms">skip_previous</span></button>
  <button class="pp xl playing"><span class="ms">pause</span></button>
  <button class="skip"><span class="ms">skip_next</span></button>
</div>

<div class="np-tools">
  <button class="ib"><span class="ms">shuffle</span></button>           <!-- .on when shuffle is on -->
  <button class="ib"><span class="ms">repeat</span></button>            <!-- .on for repeat all; repeat_one + .on for repeat one -->
  <button class="ib heart"><span class="ms">favorite</span></button>
  <button class="ib"><span class="ms">queue_music</span></button>
</div>
```

Cards under the player, and the compact header:

```html
<section class="np-card">                                     <!-- lyrics card -->
  <h4><span class="ms">lyrics</span>Lyrics</h4>
  <div class="lyrics"><p class="now">current line</p><p>next line</p><p>♪</p></div>
  <button class="btn tonal">Open lyrics<span class="ms">open_in_full</span></button>
</section>
<!-- other lyrics-card bodies: <p class="note">Download once and they stay on this phone.</p> + <button class="btn filled">Download lyrics</button>;
     <span class="loader"></span> while downloading; <div class="lyrics plain">…</div> for unsynced text;
     <p class="note">This song is instrumental.</p> -->

<a class="np-card artist-card">
  <div class="cv plain cookie art-revenge"></div>
  <div><small>About the artist</small><b>Khan Saab</b><span>2 songs in your library</span></div>
  <span class="ms">arrow_forward</span>
</a>

<section class="np-card">                                     <!-- credits -->
  <h4>Credits</h4>
  <div class="kv"><small>Performed by</small><b>Khan Saab, Shashwat Sachdev</b></div>
  <div class="kv"><small>Album</small><b>Dhurandhar: The Revenge</b></div>
</section>

<header class="np-sticky">                                    <!-- once scrolled past the player -->
  <div class="mini-meta"><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div>
  <button class="ib heart"><span class="ms">favorite</span></button>
  <button class="pp ring playing"><span class="ms">pause</span></button>
</header>
```

Full-screen lyrics: `<div class="lyrics l">` with `<p class="past">` for sung lines,
`<p class="now">` for the current one and plain `<p>` for those to come.

## Sheet, dialog, menu, snackbar

These are absolutely positioned inside the phone. Put `.scrim` before a sheet or dialog.

```html
<div class="scrim"></div>
<div class="sheet">
  <div class="handle"></div>
  <div class="sheet-head"><div class="cv plain art-revenge"></div><div><b>Jaan Se Guzarte Hain</b><span>Khan Saab, Shashwat Sachdev</span></div></div>
  <div class="divider"></div>
  <a class="sheet-row"><span class="ms">sensors</span><span>Start radio</span></a>
  <a class="sheet-row on"><span class="ms">check</span><span>Hindi</span></a>      <!-- the current choice in a picker -->
</div>
<!-- a sheet with a title instead of a song: <h2 class="sheet-title">Add to playlist</h2> -->
<!-- a sheet may hold a .searchbar and a .group of rows (Add songs) -->

<div class="scrim"></div>
<div class="dialog">
  <h2>Name this playlist</h2>
  <p>Body text, when there is any.</p>
  <div class="field focus"><label>Playlist name</label><span class="val">Late drive</span></div>
  <div class="dialog-actions"><button class="btn text">Cancel</button><button class="btn text">Create</button></div>
</div>

<div class="menu" style="right:8px;top:96px">
  <a class="menu-item"><span class="ms">edit</span>Rename</a>
  <a class="menu-item"><span class="ms">delete</span>Delete playlist</a>      <!-- .danger for red -->
</div>

<div class="snack">Added to Late drive</div>      <!-- sits above the dock; .low when a full-screen overlay is open; .static in a strip -->
```

## Switch, radio, progress, loading

```html
<span class="switch on"><span class="ms">check</span></span>
<span class="switch"><span class="ms">close</span></span>
<span class="radio on"></span>   <span class="radio"></span>

<div class="progress" style="--p:42%"></div>            <!-- linear -->
<div class="progress wavy" style="--p:62%"></div>       <!-- linear, wavy: a long job that is running -->
<span class="spinner" style="--p:62"></span>            <!-- circular, determinate (percent, no unit) -->
<span class="spinner busy"></span>                      <!-- circular, indeterminate; .s = 24dp -->
<span class="loader"></span>                            <!-- loading indicator; .contained adds the circle -->
```

Use `.loader` for the library scan and for "downloading lyrics". Use `.progress` where the
current app shows a linear bar.

## Settings

```html
<p class="set-sec">Song radio</p>
<div class="group">
  <div class="set-row"><div><b>Autoplay</b><span>When a queue reaches its last song, radio keeps going with similar songs.</span></div><span class="switch on"><span class="ms">check</span></span></div>
  <div class="set-row"><div><b>Dark</b></div><span class="radio on"></span></div>      <!-- radio option -->
  <div class="set-note">Paragraph of explanation.
    <div class="progress wavy" style="--p:62%"></div>
    <small>Analysing songs: 79 of 127</small>
    <button class="btn text">Analyse now without a charger</button></div>
</div>
```

A `.set-row` may start with a `<span class="ms">` icon.

## Empty state

```html
<div class="empty">
  <div class="empty-art"><span class="ms">search_off</span></div>    <!-- or the .mark -->
  <h2>No results for “dhurx”</h2>
  <p>Check the spelling, or try fewer words.</p>
  <button class="btn filled">Allow access</button>                   <!-- optional -->
</div>
```

## Not in the kit

Write these in the sheet that needs them, from role variables: the Recap listening clock,
language bar and story cards; the widget layouts; the notification and lock-screen frames; the
launcher icon. The widget should be a `.from-art` container with 28dp corners holding a `.cv`
and a `.pp.ring`; its album shortcuts are `.cv.plain` at 12dp radius.

## Known limits

- The art scheme keeps the cover's hue for tertiary roles (CSS `color-mix()` cannot rotate a
  hue). In the app, the scheme comes from material-color-utilities and tertiary is shifted.
- The progress ring on `.pp.ring` is one fixed drawing.
- `.seek`'s wave starts with a square end at the left edge.
- Chips are 32dp tall and connected-group segments 40dp, the Material sizes; their touch
  targets extend to 48dp, which the mock does not draw.
