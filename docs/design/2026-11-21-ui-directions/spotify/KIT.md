# spotify/ kit

`kit.css` is the current Attic UI as CSS: the tokens from `ui/Theme.kt` and one class per
composable in `app/src/main/java/app/attic/player/ui/`. This folder is the baseline, tagged
`today`. Draw what the code does. If a screen is awkward, it stays awkward.

Every component below is rendered on `00-direction.html`. Open that sheet next to this file.

## Setting up a sheet

```html
<link rel="stylesheet" href="../shared/page.css">
<link rel="stylesheet" href="../shared/covers.css">
<link rel="stylesheet" href="kit.css">
```

`kit.css` imports Figtree and both icon fonts itself. A sheet needs no font `<link>`.

Keep sheet-only CSS in the sheet's `<style>`, and give sheet-only classes names the kit does
not use. Inside a component the kit uses short child names (`.t` title, `.s` subtitle, `.tx`
text column, `.r` row, `.p` progress, `.lbl`, `.v`, `.ln`, `.b`). A bare `.lbl { … }` or
`.t { … }` in a sheet will restyle them.

## Frame

```html
<div class="phone">                      <!-- add: light · long -->
  <div class="sbar">…</div>              <!-- copy from shared/fixtures.html -->
  <div class="screen">                   <!-- add: bleed · no-mini · no-dock · theme-dark · art-<key> -->
    …screen content…
  </div>
  <div class="dock">                     <!-- omit on full-screen overlays -->
    <div class="mini art-revenge">…</div>
    <div class="tabbar">…</div>
  </div>
  <div class="gpill"></div>
</div>
```

| Class | Effect |
|---|---|
| `.phone.light` / `.strip.light` | Light scheme. |
| `.phone.long` | Page at full scroll length. `.screen` then gets the app's bottom padding: 58 + 64 + 24 + 12. |
| `.screen` | The app surface. Pads 44px for the status bar. Clipped to 844px unless the phone is `.long`. |
| `.screen.bleed` | No top padding: content runs under the status bar (collection screens, Now Playing). |
| `.screen.no-mini` | In a long phone, bottom padding without the mini player (nothing queued). |
| `.screen.no-dock` | In a long phone, no bottom padding (Now Playing, Queue, Lyrics, permission screen). |
| `.theme-dark` | Restores the dark scheme inside a light phone. Now Playing and full-screen Lyrics are always dark. |
| `.dock` | Pins the mini player and tab bar to the bottom, over the content. |

**Status bar colour.** The kit positions `.sbar` over the screen and colours it and `.gpill`
with the theme's content colour: white in dark, `#121212` in light. That holds on every
screen, including Now Playing in a light phone, where the icons are dark on the dark tint.
The app sets the system-bar style per theme, not per screen (`MainActivity.kt:29-33`). Do not
override it.

**Strips.** A component on its own goes in `<div class="strip">`. Strips take the same tokens
and `.light`. Three things to know:

- Components that reserve the status bar (`.topbar`, `.chead`, `.np-sticky`) read `--sb`.
  In a strip set `style="--sb:0px"`.
- Absolutely positioned components (`.scrim`, `.sheet`, `.dialog`, `.menu`, `.snack`, `.dock`)
  need the strip to have a height. Set it in the sheet's style.
- A strip showing Now Playing parts needs `theme-dark` and an `art-<key>` class, and its own
  dark background, because those parts are white-on-tint.

## Tokens

Colours are custom properties on `.phone` / `.strip`. Never write a hex in a sheet rule when
a token exists.

| Token | Dark | Light | Role in `Theme.kt` |
|---|---|---|---|
| `--primary` | `#1ED760` | `#12A84A` | primary, secondary |
| `--on-primary` | `#000000` | `#FFFFFF` | onPrimary, onSecondary |
| `--background` | `#121212` | `#FFFFFF` | background, surface |
| `--on-surface` | `#FFFFFF` | `#121212` | onBackground, onSurface |
| `--on-surface-variant` | `#B3B3B3` | `#5E5E5E` | onSurfaceVariant |
| `--surface-variant` | `#282828` | `#EDEDED` | surfaceVariant |
| `--sc-lowest` | `#000000` | `#FFFFFF` | surfaceContainerLowest |
| `--sc-low` | `#181818` | `#F7F7F7` | surfaceContainerLow |
| `--sc` | `#282828` | `#F0F0F0` | surfaceContainer |
| `--sc-high` | `#2A2A2A` | `#EBEBEB` | surfaceContainerHigh |
| `--sc-highest` | `#333333` | `#E2E2E2` | surfaceContainerHighest |
| `--outline` | `#727272` | `#8A8A8A` | outline |
| `--outline-variant` | `#333333` | `#E2E2E2` | outlineVariant |
| `--inverse-surface` | `#FFFFFF` | `#121212` | inverseSurface |
| `--inverse-on-surface` | `#000000` | `#FFFFFF` | inverseOnSurface |
| `--secondary-container` | `#4A4458` | `#E8DEF8` | not set by the app; Material baseline. Progress track only. |

Fixed in both themes: `--white`, `--black`, `--mark` (`#1ED760`), `--tint-fallback` (`#333333`),
`--np-card` (`#282828`), `--liked-a` / `--liked-b`, the Wrapped palette `--plum` `--marigold`
`--pink` `--cobalt` `--mint`, `--browse-1` … `--browse-8`, and `--lang-tamil` `--lang-hindi`
`--lang-english` `--lang-telugu` `--lang-malayalam` `--lang-kannada` `--lang-novocals`
`--lang-other`.

Alphas the code uses: `--w10 --w14 --w20 --w25 --w30 --w55 --w70 --w75 --w78 --w80 --w85`
(white) and `--k15 --k32 --k35 --k55 --k60 --k72 --k75 --k80` (black).

Measures: `--sb` 44px status bar, `--nav` 24px gesture inset, `--tab-h` 58px, `--mini-h` 64px.

### Type

| Class | Token | Size / line | Weight |
|---|---|---|---|
| `.t-display-s` | `--f-display-s` | 36 / 40 | 700, −0.02em |
| `.t-headline` | `--f-headline` | 26 / 31 | 700, −0.02em |
| `.t-title-l` | `--f-title-l` | 22 / 27 | 700, −0.02em |
| `.t-title-m` | `--f-title-m` | 16 / 21 | 500 |
| `.t-title-s` | `--f-title-s` | 14 / 18 | 600 |
| `.t-body-l` | `--f-body-l` | 16 / 22 | 400 |
| `.t-body-m` | `--f-body-m` | 14 / 19 | 400 |
| `.t-body-s` | `--f-body-s` | 12 / 16 | 400 |
| `.t-label-l` | `--f-label-l` | 14 / 18 | 600 |
| `.t-label-m` | `--f-label-m` | 12 / 15 | 500 |
| `.t-label-s` | `--f-label-s` | 11 / 14 | 500 |

In sheet CSS: `font: var(--f-title-l); letter-spacing: var(--tight);` (the tracking only on the
three heading styles). Helpers: `.c-variant`, `.c-primary`, `.l1` (one line, ellipsis), `.l2`
(two-line clamp), `.grow` (flex spacer).

### Tint

`covers.css` sets `--tint` on any element with an `art-<key>` class. The kit derives:

- `--art`: the app's tint. Hue kept, saturation capped at 62%, lightness clamped to 20–30%.
  `#333333` when there is no `art-` class.
- `--art-header`: `--art` in dark, `--art` at 40% over white in light. Collection headers and
  collapsed bars only.

Put `art-<key>` on the surface that takes the colour, or on its `.screen`:
`<div class="mini art-revenge">`, `<div class="screen bleed art-revenge">`.

Blends used by the app, for sheet CSS:

| Surface | CSS |
|---|---|
| Mini player, full-screen lyrics, widget | `var(--art)` |
| Collection header | `linear-gradient(180deg, var(--art-header), var(--background))` |
| Collapsed top bar | `var(--art-header)` |
| Now Playing page | `linear-gradient(180deg, var(--art) 0, color-mix(in srgb, var(--black) 72%, var(--art)) 72%, var(--background) 100%)` |
| Now Playing compact header | `color-mix(in srgb, var(--black) 55%, var(--art))` |
| Lyrics card | `color-mix(in srgb, var(--white) 10%, var(--art))` |

## Icons

Material Icons Rounded by ligature name: `<i class="ic">shuffle</i>`. Add `o` for Outlined, used
only by the unselected Home and Your Library tabs. Default 24px; size classes `s13 s16 s18 s20
s25 s26 s28 s30 s40`, or `style="--s:22px"`.

Names in use: `home search library_books music_note arrow_back keyboard_arrow_down more_vert
play_arrow pause skip_previous skip_next shuffle repeat repeat_one queue_music
add_circle_outline check_circle favorite favorite_border arrow_downward add check close sort
grid_view view_list swap_vert drag_handle edit delete radio playlist_play playlist_add language
remove_circle_outline album person_outline download ios_share`.

After rendering, check that no icon shows as a word.

```html
<span class="ib"><i class="ic">more_vert</i></span>          <!-- 48dp icon button -->
<span class="ib s56"><i class="ic s40">skip_next</i></span>  <!-- 56dp target -->
<span class="ib shade"><i class="ic">arrow_back</i></span>   <!-- black 35% disc behind it, over art -->
<span class="ib off"><i class="ic">shuffle</i></span>        <!-- disabled -->
```

The Attic mark (18px by default; `--s` to resize; `.white` for the widget):

```html
<svg class="mark" viewBox="24 24 60 60"><path fill-rule="evenodd" d="M54,25L84,53L84,79Q84,83 80,83L28,83Q24,83 24,79L24,53ZM44,60a10,10 0 1,0 20,0a10,10 0 1,0 -20,0Z"/></svg>
```

Its fill is `#1ED760` in both themes. It does not take the light scheme's darker green.

## Covers

From `shared/covers.css`. The kit sets width and radius wherever a cover sits inside a
component, so write the bare element:

```html
<div class="cv art-revenge"></div>                 <!-- album cover -->
<div class="cv art-revenge plain"></div>           <!-- no title text: use at 40px and under, and for artist images -->
<div class="cv art-anbe round"></div>              <!-- artist -->
<div class="mosaic"><div class="cv art-viva"></div><div class="cv art-dreams"></div><div class="cv art-troubadour"></div><div class="cv art-ghost"></div></div>
<div class="liked"><i class="ic">favorite</i></div>      <!-- Liked Songs tile -->
<div class="noart"><i class="ic">music_note</i></div>    <!-- file with no art -->
```

## Small parts

```html
<span class="dl"></span>                 <!-- downloaded badge, 13dp. On every song row. -->
<span class="dl lg"></span>              <!-- 24dp, collection control row -->

<span class="eq"><i></i><i></i><i></i></span>          <!-- playing bars -->
<span class="eq paused"><i></i><i></i><i></i></span>   <!-- held at 30% while paused -->

<span class="ib like"></span>            <!-- not liked: plus in a circle, current colour -->
<span class="ib like on"></span>         <!-- liked: green check in a circle -->

<span class="playc"></span>              <!-- 56dp green play circle -->
<span class="playc pause"></span>
<span class="playc white s66 pause"></span>   <!-- Now Playing: 66dp, white, black glyph -->
<span class="playc white pause"></span>       <!-- full-screen lyrics: 56dp white -->
```

The like glyph is 26dp. Now Playing's title block sets it to 28dp itself.

## Tab bar and mini player

```html
<div class="dock">
  <div class="mini art-revenge">
    <div class="r">
      <div class="cv art-revenge plain"></div>
      <div class="tx"><div class="t">Jaan Se Guzarte Hain</div><div class="s"><span class="dl"></span>Khan Saab, Shashwat Sachdev</div></div>
      <span class="ib like"></span>
      <span class="ib"><i class="ic s30">pause</i></span>
    </div>
    <div class="p" style="--p:32.3%"><i></i></div>
  </div>
  <div class="tabbar">
    <div class="tab on"><i class="ic">home</i><span>Home</span></div>
    <div class="tab"><i class="ic">search</i><span>Search</span></div>
    <div class="tab"><i class="ic o">library_books</i><span>Your Library</span></div>
  </div>
</div>
```

- Mini player states: `pause` ↔ `play_arrow` for playing and paused; `like on` for liked; a long
  title gets an ellipsis; a long artist line is a marquee, so it is clipped with none. Text and
  icons are white in both themes. The bar has no dismissed state. It is absent only when nothing
  is queued: leave it out and add `no-mini` to the screen.
- Tabs: the selected tab uses the Rounded glyph and `.on`. Unselected Home and Your Library
  use `o`. Search has one glyph. A detail screen keeps the tab it was opened from selected.
- 1:47 of 5:31 is `--p:32.3%`.

## Headers

```html
<!-- Home: settings button, then chips -->
<div class="home-head">
  <span class="setbtn"><svg class="mark" …></svg></span>
  <div class="chips"><span class="chip on">All</span><span class="chip">Albums</span><span class="chip">Artists</span><span class="chip">Folders</span></div>
</div>

<!-- Search -->
<div class="head"><span class="setbtn">…</span><h1>Search</h1></div>

<!-- Your Library -->
<div class="head lib"><span class="setbtn">…</span><h1>Your Library</h1>
  <span class="ib"><i class="ic s26">search</i></span><span class="ib"><i class="ic s28">add</i></span></div>
<div class="chips lib"><span class="chip">Playlists</span><span class="chip">Albums</span><span class="chip">Artists</span><span class="chip">Downloaded</span></div>

<!-- Settings, Wrapped -->
<div class="backbar"><span class="ib"><i class="ic">arrow_back</i></span><h1>Settings</h1></div>
<div class="chips wrapped">…</div>       <!-- Wrapped period chips -->

<!-- Queue -->
<div class="backbar q"><span class="ib"><i class="ic">keyboard_arrow_down</i></span><h1>Queue</h1><span class="btn text">Clear</span></div>
```

Library chips have no default selection. Home and Wrapped always have one.

## Section titles

```html
<h2 class="sec">Recently played</h2>

<p class="sec-over">Picked at random from your songs</p>
<h2 class="sec">Start listening</h2>                    <!-- tightens itself after .sec-over -->

<div class="sec-row"><h2 class="sec">Top songs</h2><span class="playc"></span></div>

<h3 class="subhead">Up next, 6 songs</h3>               <!-- Queue's small grey heading -->
```

## Home

```html
<div class="shortcuts">
  <div class="shortcut"><div class="liked"><i class="ic">favorite</i></div><div class="t">Liked Songs</div></div>
  <div class="shortcut"><div class="cv art-revenge"></div><div class="t">Dhurandhar: The Revenge</div></div>
  <div class="shortcut"><div class="mosaic">…</div><div class="t">Phonk</div></div>
</div>

<div class="wrapcard">
  <div class="mosaic">…</div>
  <div class="tx"><div class="t">Your 2026 so far</div><div class="s">18,460 minutes. See your top songs and cards to share.</div></div>
</div>

<div class="dial">
  <div class="dial-tile cur"><div class="cv art-revenge"></div><div class="t">Jaan Se Guzarte Hain</div></div>
  <div class="dial-tile"><div class="cv art-rockstar"></div><div class="t">Kun Faya Kun</div></div>
</div>

<div class="shelf">
  <div class="card cur"><div class="cv art-revenge"></div><div class="t">Jaan Se Guzarte Hain</div><div class="s">Khan Saab, Shashwat Sachdev</div></div>
  <div class="card"><div class="mosaic">…</div><div class="t">Hindi</div><div class="s">46 songs</div></div>
  <div class="card round"><div class="cv art-anbe round"></div><div class="t">A. R. Rahman</div></div>
</div>

<div class="spot"><div class="cv art-anbe round"></div><div><div class="s">More from</div><div class="t">A. R. Rahman</div></div></div>
```

`.cur` turns a card or dial title green for the current song. Shelves clip at the phone edge;
give them four or more cards so the cut shows. The Wrapped card reads "Your 2026 so far" on
the round's date and "Your 2026 Wrapped" from 24 December.

## Rows

```html
<!-- track row -->
<div class="row">
  <div class="cv art-rockstar"></div>
  <div class="tx"><div class="t">Kun Faya Kun</div><div class="s"><span class="dl"></span>A. R. Rahman, Javed Ali, Mohit Chauhan</div></div>
  <span class="ib"><i class="ic">more_vert</i></span>
</div>

<!-- current song: green title and bars (add "paused" to .eq when paused) -->
<div class="row cur">
  <div class="cv art-revenge"></div>
  <div class="tx"><div class="t"><span class="eq"><i></i><i></i><i></i></span>Jaan Se Guzarte Hain</div><div class="s"><span class="dl"></span>Khan Saab, Shashwat Sachdev</div></div>
  <span class="ib"><i class="ic">more_vert</i></span>
</div>
```

| Variant | Change |
|---|---|
| Album screen | Leave out the cover element. |
| Numbered (artist "Popular") | Wrap: `<div class="ranked"><span class="rk">1</span><div class="row">…</div></div>` |
| Drag handle (queue, playlist reorder) | Trailing `<span class="ib plain"><i class="ic">drag_handle</i></span>` |
| Being dragged | `.row.drag` |
| Add songs: not in playlist | Trailing `<span class="ib plain"><i class="ic">add</i></span>` |
| Add songs: in playlist | Trailing `<span class="ib ok"><i class="ic">check</i></span>` |
| Swiped to remove (queue) | Wrap: `<div class="swipe" style="--dx:-140px"><div class="row">…</div></div>` |

Rows show no duration and no track number. Every row has the badge.

```html
<!-- Wrapped "Top songs": rank, cover, text, value. For "Top artists" use cv round and drop .s -->
<div class="toprow cur"><span class="rk">1</span><div class="cv art-revenge plain"></div>
  <div class="tx"><div class="t">Jaan Se Guzarte Hain</div><div class="s">Khan Saab, Shashwat Sachdev</div></div><span class="v">214 plays</span></div>

<!-- result row: 56dp cover, no trailing control -->
<div class="res"><div class="cv art-viva round"></div><div class="tx"><div class="t">Coldplay</div><div class="s">Artist</div></div></div>

<!-- Library list row: the same with a 64dp cover -->
<div class="res lib"><div class="liked"><i class="ic">favorite</i></div><div class="tx"><div class="t">Liked Songs</div><div class="s">Playlist, 31 songs</div></div></div>
```

## Search

```html
<div class="field"><i class="ic">search</i><span class="v ph">What do you want to listen to?</span></div>

<div class="field"><i class="ic">search</i><span class="v">dhur<i class="caret"></i></span><span class="ib"><i class="ic">close</i></span></div>

<p class="browse-title">Start browsing</p>
<div class="browse">
  <div class="btile"><div class="t">English</div><div class="cv art-viva"></div></div>
  <div class="btile c2"><div class="t">Hindi</div><div class="cv art-oso"></div></div>
</div>

<div class="field stock"><i class="ic">search</i><span class="v ph">Find a song</span></div>   <!-- Add-songs sheet -->
```

Browse colours rotate `btile`, `c2` … `c8`, then start again. Folders come first, then the
eight biggest albums. The search field is inverse: white in dark, `#121212` in light.

## Your Library

```html
<div class="sortrow"><i class="ic s18">sort</i><span class="lbl">Recents</span><span class="ib"><i class="ic s20">grid_view</i></span></div>

<!-- "Downloaded" filter -->
<div class="sortrow songs"><i class="ic s18">sort</i><span class="lbl">Title, 127 songs</span><span class="ib"><i class="ic">shuffle</i></span></div>

<div class="libgrid">
  <div class="gtile"><div class="liked"><i class="ic">favorite</i></div><div class="t">Liked Songs</div><div class="s">Playlist, 31 songs</div></div>
  <div class="gtile round"><div class="cv art-anbe round"></div><div class="t">A. R. Rahman</div><div class="s">Artist</div></div>
  <div class="gtile"><div class="cv art-lootera"></div><div class="t">Lootera</div><div class="s">Album, Amit Trivedi</div></div>
</div>
```

In grid mode the toggle glyph is `view_list`.

## Collection screens

```html
<div class="screen bleed art-revenge">
  <div class="topbar"><span class="ib shade"><i class="ic">arrow_back</i></span><span class="t">Dhurandhar: The Revenge</span></div>

  <div class="chead">
    <div class="cv art-revenge"></div>                        <!-- or .mosaic, or .liked (50% wide) -->
    <h1 class="ttl">Dhurandhar: The Revenge</h1>
    <div class="by"><div class="cv art-revenge round plain"></div>Shashwat Sachdev</div>   <!-- album -->
    <p class="det">Album • 2026</p>
  </div>

  <div class="ctrl">
    <span class="thumb"><div class="cv art-revenge plain"></div></span>
    <span class="ib"><i class="ic s26">add_circle_outline</i></span>
    <span class="dl lg"></span>
    <span class="grow"></span>
    <span class="ib"><i class="ic s28">shuffle</i></span>
    <span class="playc"></span>
  </div>
  …rows…
  <p class="foot">6 songs • 26 min</p>                        <!-- album only -->
</div>
```

| Need | Markup |
|---|---|
| Collapsed bar | `<div class="topbar collapsed">` with a plain `.ib` back arrow (no `shade`). The title shows. |
| Playlist bar | Add `<span class="ib"><i class="ic">more_vert</i></span>` after the title. |
| Folder, playlist, Liked Songs subtitle | Replace `.by` with `<p class="csub">Playlist from your Music/Offline folder</p>`. |
| Playlist control-row extras | After the badge: `<span class="ib plain"><i class="ic">add</i></span><span class="ib plain"><i class="ic">swap_vert</i></span>` (`check` while reordering). |
| Shuffle on for this collection | `<span class="ib on">` |
| This collection playing | `<span class="playc pause">` |
| Empty collection | `off` on the add and shuffle buttons. |
| Artist | No `.chead`. Use the hero below, then `.ctrl`. |

```html
<div class="hero"><div class="cv art-anbe plain"></div><h1>A. R. Rahman</h1></div>
<p class="csub">14 songs, 1 hr 13 min</p>
```

In light the header is a pale wash and the title is dark. The hero keeps white text.

## Now Playing

```html
<div class="screen bleed no-dock theme-dark art-revenge">
  <div class="np-page">
    <div class="np-head">
      <span class="ib"><i class="ic s30">keyboard_arrow_down</i></span>
      <div class="ctx"><small>Playing from</small><b>Dhurandhar: The Revenge</b></div>
      <span class="ib"><i class="ic">more_vert</i></span>
    </div>
    <div class="grow"></div>
    <div class="np-cover"><div class="cv art-revenge"></div></div>        <!-- add "paused" for the 14dp inset -->
    <div class="grow"></div>
    <p class="np-lyric">The line being sung</p>                          <!-- only with synced lyrics -->
    <div class="np-title">
      <div class="tx"><h1>Jaan Se Guzarte Hain</h1><p>Khan Saab, Shashwat Sachdev</p></div>
      <span class="ib like"></span>
    </div>
    <div class="seek" style="--p:32.3%"><i></i></div>                    <!-- add "scrub" while dragged -->
    <div class="times"><span>1:47</span><span>5:31</span></div>
    <div class="transport">
      <span class="ib tog"><i class="ic">shuffle</i></span>              <!-- "tog on": green with a dot -->
      <span class="ib s56"><i class="ic s40">skip_previous</i></span>
      <span class="playc white s66 pause"></span>
      <span class="ib s56"><i class="ic s40">skip_next</i></span>
      <span class="ib tog"><i class="ic">repeat</i></span>               <!-- repeat one: "tog on" + repeat_one -->
    </div>
    <div class="np-foot"><span class="dl"></span>Downloaded<span class="ib"><i class="ic">queue_music</i></span></div>
  </div>
  <div class="np-cards">…cards…</div>
</div>
```

Cards under the player:

```html
<div class="lyr-card">
  <h3>Lyrics</h3>
  <p class="ln now">Current line</p><p class="ln">Next line</p><p class="ln">…</p><p class="ln">…</p>
  <span class="pill">Show lyrics</span>
</div>

<div class="about">
  <div class="img"><div class="cv art-revenge plain"></div><span>About the artist</span></div>
  <div class="b"><b>Khan Saab</b><span>2 songs in your library</span></div>
</div>

<div class="credits">
  <h3>Credits</h3>
  <div><b>Khan Saab, Shashwat Sachdev</b><span>Performed by</span></div>
  <div><b>Dhurandhar: The Revenge</b><span>Album</span></div>
  <div><b>2026</b><span>Year</span></div>
</div>
```

Other lyrics-card bodies, in place of the `.ln` lines and pill:

| State | Body |
|---|---|
| Plain lyrics | `<p class="plain">…text…</p>` (cut at four lines) then the pill |
| Never downloaded | `<p class="msg">Download once and they stay on this phone.</p><span class="btn">Download lyrics</span>` |
| None found | `<p class="msg">No lyrics were found for this song.</p><span class="btn">Download lyrics</span>` |
| Downloading | The message, then `<span class="spin white"></span>` in place of the button |
| Instrumental | `<p class="msg big">This song is instrumental.</p>` |

Compact header shown after scrolling 55% of a screen:

```html
<div class="np-sticky">
  <div class="r">
    <div class="tx"><div class="t">Jaan Se Guzarte Hain</div><div class="s">Khan Saab, Shashwat Sachdev</div></div>
    <span class="ib like"></span><span class="ib"><i class="ic s30">pause</i></span>
  </div>
  <div class="p"><i></i></div>
</div>
```

Radio has no indicator. Its only sign is the context line "Radio · {song title}".

## Queue

Queue follows the app theme. It is a plain `.screen.no-dock` with `.backbar.q`, `.subhead`
headings, the current song as `.row.cur` with a kebab, and up-next rows with drag handles (see
Rows). "Clear" is present only when something is up next. With nothing next the second heading
reads "Nothing up next" and no rows follow.

## Full-screen lyrics

```html
<div class="screen theme-dark art-revenge lyr-screen">
  <div class="lyr-head"><span class="ib"><i class="ic">keyboard_arrow_down</i></span>
    <div class="ctx"><b>Jaan Se Guzarte Hain</b><small>Khan Saab, Shashwat Sachdev</small></div></div>
  <div class="lyr-body">
    <p class="ln sung">Sung line</p><p class="ln sung">Current line</p><p class="ln">Upcoming line</p>
  </div>
  <div class="lyr-foot"><span class="playc white pause"></span></div>
</div>
```

Plain lyrics: `<p class="plain">…</p>` inside `.lyr-body`. No lyrics or instrumental:
`<div class="lyr-body center"><span class="btn">Download lyrics</span></div>`. A blank timed
line is "♪".

## Sheets, dialogs, menu, snackbar

Place these after `.dock`, inside `.phone`.

```html
<div class="scrim"></div>
<div class="sheet">
  <div class="sheet-head"><div class="cv art-revenge plain"></div>
    <div class="tx"><div class="t">Jaan Se Guzarte Hain</div><div class="s">Khan Saab, Shashwat Sachdev</div></div></div>
  <div class="divider"></div>
  <div class="srow"><i class="ic">radio</i>Start radio</div>
  <div class="srow"><i class="ic">favorite_border</i>Add to liked songs</div>
</div>

<!-- picker: title, then rows -->
<div class="scrim"></div>
<div class="sheet flush"><h2 class="sheet-title">Add to playlist</h2><div class="srow"><i class="ic">add</i>New playlist</div>…</div>

<!-- Add songs: 92% tall -->
<div class="scrim"></div>
<div class="sheet tall"><h2 class="sheet-title">Add songs</h2><div class="field stock">…</div>…rows…</div>
```

Track-menu icons in order: `radio`, `favorite_border` / `favorite`, `playlist_play`,
`queue_music`, `playlist_add`, `language`, `remove_circle_outline`, `album`, `person_outline`.

```html
<div class="scrim dialog"></div>
<div class="dialog">
  <h2>Give your playlist a name</h2>
  <div class="ofield"><label>Playlist name</label>Late drive<i class="caret"></i></div>
  <div class="acts"><span class="btn text">Cancel</span><span class="btn text">Create</span></div>
</div>

<div class="dialog">
  <h2>Delete Late drive?</h2>
  <p>The playlist goes away. The songs stay in your library.</p>
  <div class="acts"><span class="btn text">Cancel</span><span class="btn text">Delete playlist</span></div>
</div>

<div class="menu" style="right:8px;top:48px">
  <div class="mi"><i class="ic">edit</i>Rename</div>
  <div class="mi"><i class="ic">delete</i>Delete playlist</div>
</div>

<div class="snack">Added to Late drive</div>
```

The confirm button is `btn text off` while the name field is blank. The snackbar sits above
the mini player and tab bar; `.snack.no-mini` when no mini player is shown, `.snack.over` when a
full-screen overlay is open. It has no action button.

## Controls

```html
<span class="btn">Download lyrics</span>          <!-- filled -->
<span class="btn text">Clear</span>               <!-- text -->
<span class="btn text off">Create</span>          <!-- disabled text -->
<span class="pill">Show lyrics</span>             <!-- white pill, dark surfaces only -->

<span class="switch on"></span>  <span class="switch"></span>
<span class="radio on"></span>   <span class="radio"></span>

<div class="prog" style="--p:66%"><i></i></div>

<span class="spin"></span>                                        <!-- .white on tinted surfaces -->
<div class="center fill"><span class="spin"></span></div>         <!-- loading screen -->
<div class="center pad"><span class="spin"></span></div>          <!-- Wrapped loading -->
```

## Settings

```html
<h2 class="sec">Appearance</h2>
<div class="opt"><span class="lbl">Match phone</span><span class="radio"></span></div>
<div class="opt"><span class="lbl">Dark</span><span class="radio on"></span></div>

<h2 class="sec">Song radio</h2>
<div class="setrow">
  <div class="tx"><div class="t">Autoplay</div><div class="s">When a queue reaches its last song, radio keeps going with similar songs.</div></div>
  <span class="switch on"></span>
</div>
<p class="para">Radio finds similar songs by listening to your files. …</p>
<div style="height:12px"></div>
<div class="prog" style="--p:66%"><i></i></div>
<p class="para status">84 of 127 songs analysed. The rest continue next time the phone charges.</p>
<div class="setbtn-row"><span class="btn text">Analyse now without a charger</span></div>

<p class="para small">Downloading sends each song's title, artist, album and length to lrclib.net. …</p>
```

Spacing between paragraphs is set in the code case by case (4dp between the two Lyrics
paragraphs, 12dp before a progress bar). Add it with a spacer or margin in the sheet.

## Empty state, stat tiles

```html
<div class="empty"><h2>No results for "dhurx"</h2><p>Check the spelling, or try fewer words.</p></div>

<div class="stats">
  <div class="stat"><b>18,460</b><span>Minutes</span></div>
  <div class="stat"><b>6,212</b><span>Plays</span></div>
</div>
```

The empty state centres its lines as boxes. A line that wraps fills the width and is
left-aligned, because the code sets no text alignment. The permission screen is different: it
centres its text explicitly, so style that one in the sheet.

## Wrapped page and story chrome

```html
<div class="banner">
  <h2>Your 2026 so far</h2>
  <p>18,460 minutes of music, told in 10 cards you can share.</p>
  <span class="go"><i class="ic">play_arrow</i>Play your story</span>
</div>

<div class="langbar"><i class="lg-hindi" style="flex:46"></i><i class="lg-tamil" style="flex:27"></i>…</div>
<div class="langrow"><i class="lg-hindi"></i><span>Hindi</span><em>46%</em></div>

<div class="segs"><i class="done"></i><i class="now" style="--p:40%"></i><i></i></div>
<span class="spill"><i class="ic">close</i>Close</span>
<span class="spill"><i class="ic">download</i>Save</span>
<span class="spill solid"><i class="ic">ios_share</i>Share</span>
```

Language classes: `lg-tamil lg-hindi lg-english lg-telugu lg-malayalam lg-kannada lg-novocals
lg-other`.

## Not in the kit

Build these in the sheet's own `<style>`, with the tokens above:

- The ten Wrapped story cards, the listening clock and the card thumbnails (`ui/WrappedCards.kt`).
  They sit on a fixed 360 × 640 canvas with their own display sizes.
- The widget in its three sizes, the media notification, the lock screen, the analysis
  notification and the launcher icon. Android draws most of these, and the widget uses the
  system font.
- The permission screen (72dp mark, centred `headlineSmall` title, centred `bodyLarge` body,
  one `.btn`).

## Guessed, not measured

- `--secondary-container` and the progress bar's gap and stop dot: Material 3 defaults for the
  library version in use. No screenshot shows a progress bar.
- Dialog width (328px) and the 60% dim behind dialogs: platform defaults.
- Sheet handle, 28dp sheet corners, 32% sheet scrim, dropdown and snackbar shadows: Material 3
  defaults.
- `.eq` and `.spin` are one frame of an animation.
