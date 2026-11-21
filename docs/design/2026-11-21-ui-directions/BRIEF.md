# UI directions round, 2026-11-21

## The ask

> Right now, the UI is a rip off of Spotify. While it is good to use, I don't think we can
> publish it without inviting copyright strikes or something. [...] It should follow Google's
> Material standards and it should look like as if we build an offline music player for Google
> Pixel Devices, with Pixel UI Experience. [...] first extract how the current UI is, and prepare
> HTML Mock for the current spotify style UI, and then prepare HTML Mocks for the new Pixel Style
> UI you're proposing. Also propose another style of UI, like the OneUI style of UI and Apple
> Music Style of UI.

Nothing in the app changes in this round. It produces mocks to choose from.

## The four folders

| Folder | What it is |
|---|---|
| `spotify/` | The UI that ships today, redrawn faithfully from the code. The baseline, tagged `today`. |
| `pixel/` | Proposal. Material 3 Expressive as Google ships it on Pixel. The owner's stated preference. |
| `oneui/` | Proposal. Samsung One UI conventions. |
| `apple/` | Proposal. Apple Music conventions, on Android. |

`CURRENT-UI.md` in this folder is the extraction of the current app: every screen, state, label,
measurement and token. **Section 4 of it is the completeness contract**: a proposal keeps every
capability listed there and adds none of the ones under "Not present today".

## What a proposal may and may not borrow

The reason for this round is that the current app copies one product closely enough to be
mistaken for it. A proposal that copies a different product that closely has the same problem.
So each proposal takes a platform's **design language** (its layout grammar, component set,
type treatment, shape and motion rules, the way it uses colour) and applies it to Attic. It
does not reproduce Apple Music, Samsung Music or YouTube Music screen for screen, and it does
not use their logos, their names for things, their proprietary fonts or their proprietary icon
sets. Someone who uses that platform should feel at home; nobody should think it is that
company's app.

- Fonts: only families under an open licence that an Android app can bundle (OFL, Apache).
  Load them from Google Fonts. Not SF Pro, not Samsung One or One UI Sans, not Circular.
- Icons: inline SVG you draw, or an openly licensed set (Material Symbols is Apache 2.0).
  Not SF Symbols, not Samsung's icon set.
- The mark is being redesigned in this round too (`logo/`); the owner called the current one
  "really bad". Until one is chosen, a sheet that needs the mark uses the current house
  outline from `docs/logo.svg` as a stand-in, recoloured. Design the container and placement,
  not the glyph, and do not build an identity around the house shape.
- `spotify/` is exempt from all of this. Its job is to show what exists.

## Names the three proposals share

Part of what makes the current app read as Spotify is vocabulary (`CURRENT-UI.md` section 5).
The three proposals use this one replacement set so the owner decides naming once, separately
from style. `spotify/` keeps the current strings.

| Today | In the proposals | Why |
|---|---|---|
| "Your Library" (tab and title) | "Library" | |
| "Liked Songs" | "Favourites" | |
| Like control: plus in a circle, turning into a green check | A heart, outline to filled | The plus-to-check is the single most Spotify-specific control |
| "Add to liked songs" / "Remove from liked songs" | "Add to favourites" / "Remove from favourites" | |
| "Wrapped", "Your {year} Wrapped" | "Recap", "Your {year} Recap" | |
| "Play your story" | "Play recap" | |
| "Downloaded" (library filter that shows all songs) | "Songs" | Every song is a local file; nothing is "downloaded" |
| Green downloaded badge on every row, "Downloaded" label in Now Playing | Removed | Decoration with no state behind it |
| "What do you want to listen to?" | "Search your music" | |
| "Start browsing" | "Browse" | |
| "Speed dial" | "Instant mixes" | Tap starts radio from that song; the name should say so |
| "Popular" / "Popular releases" (artist) | "Top songs" / "Albums" | Offline, "popular" means most played by you |
| "Your top artists" | "Top artists" | |
| "Show lyrics" | "Open lyrics" | |
| "Let's find something for your playlist" | "Add songs to this playlist" | |
| "Give your playlist a name" | "Name this playlist" | |

Everything not in this table keeps its current string ("Home", "Search", "Recently played",
"Start listening", "Start radio", "Play next", "Add to queue", "Go to album", "About the
artist", "Credits", every settings label, every snackbar and empty-state message).

## Sheets

Every folder has the same eleven files with the same names, and each sheet has at least the
frames listed, in this order. That is what lets the index put the same screen from four styles
next to each other. Extra frames are welcome after the required ones.

| File | Required frames |
|---|---|
| `00-direction.html` | Not a screen. Palette (dark and light) as swatches, type scale specimen, shape scale, icon sample, how album-art colour is used, and the core components as strips: buttons, chips, song row, tab bar, mini player, seek bar. Prose: what is borrowed from the platform, what is Attic's own, what the signature element is. For `spotify/`: the current tokens, documented. |
| `01-home.html` | A. Home top, "All", dark, with mini player and tab bar. B. The whole Home page at full length (`.phone.long`). C. One other filter selected ("Albums"). D. Frame A in light. |
| `02-search.html` | A. Browse (empty query). B. Results for "dhur" mixing an artist, albums, a playlist and songs. C. No results. D. Frame B in light. |
| `03-library.html` | A. List, no filter. B. Grid view. C. The all-songs filter with its sort control and "Shuffle all songs". D. Create-playlist dialog over the library. |
| `04-collections.html` | A. Album. B. Artist. C. User playlist. D. Playlist in reorder mode. E. Favourites. F. Album scrolled, top bar collapsed. G. Album in light. |
| `05-now-playing.html` | A. Now Playing, playing, dark. B. Scrolled at full length: lyric line, lyrics card, "About the artist", "Credits". C. Paused, radio context, shuffle on, repeat-one. D. Frame A in light. E. Mini player states as strips: playing, paused, long title, light. |
| `06-queue-lyrics.html` | A. Queue. B. Queue during radio. C. Queue with nothing up next. D. Full-screen synced lyrics. E. Lyrics card states as strips: not downloaded, downloading, plain lyrics, instrumental, not found. |
| `07-sheets.html` | A. Track menu. B. Playlist picker. C. Language picker. D. Add songs. E. Playlist name dialog and delete dialog. F. Playlist overflow menu and a snackbar. |
| `08-settings-states.html` | A. Settings, dark. B. Settings, light, with analysis running. C. Permission screen. D. Loading and empty library. E. A missing item ("is gone"). |
| `09-recap.html` | A. Recap page top, "This year". B. The whole page at full length. C. Story: at least five of the ten card types, as 9:16 frames. D. The save and share controls on a story card. E. The Home entry card, normal and the late-December variant. |
| `10-system.html` | A. The widget in its three sizes on a home-screen backdrop. B. Media notification and lock screen. C. Analysis notification. D. Launcher icon, colour and themed monochrome. |

## Content

Use the library below in every folder. Covers are in `shared/covers.css`; open
`shared/fixtures.html` to see them. A cover is `<div class="cv art-revenge"></div>`, sized by
its width. Putting `art-revenge` on any ancestor exposes `--tint`, `--tint-deep` and
`--tint-soft` for surfaces that take their colour from the artwork.

**Now playing, everywhere:** "Jaan Se Guzarte Hain" by Khan Saab, Shashwat Sachdev, from
*Dhurandhar: The Revenge* (`art-revenge`), at 1:47 of 5:31, played from that album.

| Album | Artist | Year | Class | Songs |
|---|---|---|---|---|
| Dhurandhar: The Revenge | Shashwat Sachdev | 2026 | `art-revenge` | Aari Aari (Bombay Rockers, Khan Saab, Jasmine Sandlas, Reble) 3:42 · Main Aur Tu (Jasmine Sandlas, Reble, Shashwat Sachdev) 4:05 · Jaan Se Guzarte Hain (Khan Saab, Shashwat Sachdev) 5:31 · Aakhri Ishq (Jubin Nautiyal) 4:18 · Vaari Jaavan (Jyoti Nooran, Jasmine Sandlas, Reble) 3:56 · Phir Se (Arijit Singh) 4:44. 6 songs, 26 min |
| Dhurandhar | Shashwat Sachdev | 2025 | `art-dhurandhar` | Dhurandhar Title Track, Ishq Jalakar, Gehra Hua, Shararat |
| Yeh Jawaani Hai Deewani | Pritam | 2013 | `art-yjhd` | Badtameez Dil, Kabira, Ilahi, Balam Pichkari |
| Premam | Rajesh Murugesan | 2015 | `art-premam` | Malare (Vijay Yesudas), Aluva Puzha, Rockaankuthu |
| Rockstar | A. R. Rahman | 2011 | `art-rockstar` | Kun Faya Kun (A. R. Rahman, Javed Ali, Mohit Chauhan), Sadda Haq, Nadaan Parindey, Tum Ho |
| Viva la Vida | Coldplay | 2008 | `art-viva` | Viva La Vida, Violet Hill, Lost!, Strawberry Swing |
| Lootera | Amit Trivedi | 2013 | `art-lootera` | Monta Re (Amitabh Bhattacharya & Swanand Kirkire), Sawaar Loon, Zinda |
| Raanjhanaa | A. R. Rahman | 2013 | `art-raanjhanaa` | Raanjhanaa, Tum Tak, Banarasiya |
| Anbe Aaruyire | A. R. Rahman | 2005 | `art-anbe` | Aaruyire, Mayilirage, Varugiraai |
| Om Shanti Om | Vishal-Shekhar | 2007 | `art-oso` | Ajab Si, Main Agar Kahoon, Deewangi Deewangi |
| Udd Gaye (Single) | Ritviz | 2017 | `art-uddgaye` | Udd Gaye 3:02 |
| Manmarziyaan | Amit Trivedi | 2018 | `art-manmarziyaan` | Naina Da Kya Kasoor (Amit Trivedi & Raftaar), Daryaa, Grey Walaa Shade |
| GHOST! (Single) | phonk.me, KIIXSHI | 2023 | `art-ghost` | GHOST! 2:08 |
| A Head Full of Dreams | Coldplay | 2015 | `art-dreams` | Hymn for the Weekend, Adventure of a Lifetime, Everglow |
| Jodhaa Akbar | A. R. Rahman | 2008 | `art-jodhaa` | Khwaja Mere Khwaja, Jashn-E-Bahaaraa, In Lamhon Ke Daaman Mein |
| Troubadour | K'NAAN | 2009 | `art-troubadour` | Wavin' Flag, Take a Minute, Fatima |

- **Artists** (image is one of their covers, cropped round or to the style's artist shape):
  A. R. Rahman (`art-anbe`, 14 songs, 1 hr 13 min), Shashwat Sachdev (`art-revenge`), Coldplay
  (`art-viva`), Amit Trivedi (`art-lootera`), Pritam (`art-yjhd`), Ritviz (`art-uddgaye`),
  K'NAAN (`art-troubadour`), Khan Saab (`art-revenge`), Vijay Yesudas (`art-premam`).
- **Folders** (read-only playlists, cover is a `.mosaic` of four covers): English, 24 songs
  (viva, dreams, troubadour, ghost) · Hindi, 46 songs (oso, yjhd, lootera, rockstar) · Tamil,
  19 songs (anbe, premam, raanjhanaa, jodhaa) · Phonk, 38 songs (ghost, rockstar, dhurandhar,
  uddgaye).
- **User playlists:** "Late drive", 12 songs · "Rahman, slow", 9 songs.
- **Favourites:** 31 songs. Library total: 127 songs.
- **Recap, this year:** 18,460 minutes · 6,212 plays · 412 songs · 96 artists · 251 days with
  music · longest streak 34 days. Top songs: Jaan Se Guzarte Hain 214 plays, Kun Faya Kun 187,
  Malare 163, Viva La Vida 151, Udd Gaye 140. Top artists: A. R. Rahman 3,120 min, Shashwat
  Sachdev 2,480, Coldplay 1,905, Amit Trivedi 1,310, Ritviz 870. Languages: Hindi 46%, Tamil
  27%, English 18%, Malayalam 6%, No vocals 3%. Listener type "Night owl", busiest hour 10 pm.
  Log began 3 January 2026.
- **Lyrics:** write original placeholder lines. Do not reproduce any real song's lyrics.
- The date on screen is Saturday 21 November 2026, 9:41.

## Authoring rules

Modelled on the Tatak mock library (`/Users/srivathsanv/Documents/Personal/Tatak/docs/design/`:
see `2026-09-04-premium/index.html` and `library/screens/2026-09-04-premium-0*.html` for the
shape of a sheet).

**Files.** In your style folder: `kit.css` (tokens and every reusable component), `KIT.md`
(the class reference for whoever builds the next sheets), and the eleven sheets. A sheet links
`../shared/page.css`, `../shared/covers.css` and `kit.css`, in that order, and keeps only
sheet-specific rules in its own `<style>`. Do not edit anything in `shared/`.

**Page shape.** `.intro` (an `h1`, a `.date` line "2026-11-21 · UI directions · {style} ·
sheet NN", one or two paragraphs saying what the sheet settles), then one or more `.rowset`s
of `.col`s, then a `.spec` block. A `.col` is `.tags`, `.cap`, `.why`, then the `.phone`.

- `.cap` names the frame ("A · Home, top"). `.why` says what to look at in it, in one to three
  sentences: the decision, not a description of the picture.
- Tags: `today` on every `spotify/` frame; `proposal` on the others; add `dark` / `light`, and
  `state` for a variant.
- `.spec` ends the sheet: what changed from today and why, what was kept, and any open
  question the owner has to answer. Proposals name the specific Spotify cue each change removes.

**Frames.** `.phone` is 390 × 844 CSS px, one px per dp. Use `.phone.long` for a page shown at
full scroll length. Start each phone with the status bar (`.sbar`, markup in
`shared/fixtures.html`) and end it with the gesture pill (`.gpill`); a frame is an Android
phone in every folder. Components shown alone go in a `.strip`.

**Scope your CSS.** Every selector in `kit.css` starts with `.phone` or `.strip` (or a class
that only appears inside them), so the kit cannot restyle the review page. Dark is the default;
light is `.phone.light` / `.strip.light`. Define colours as custom properties on those two
selectors and never hard-code a hex in a component rule.

**Fidelity.** These are static pictures of real screens: real labels from `CURRENT-UI.md`
(through the naming table above), the fixture library, plausible numbers. No lorem ipsum, no
"Song title" placeholders, no features the app does not have. No JavaScript, no external
images. Touch targets are at least 48dp.

**Writing.** Captions and spec prose are plain and specific. Say what the design does and why.
No marketing adjectives, no "N things to know" openers, no recap paragraph restating the sheet.

**Check your work by looking at it.** Render every sheet and read the PNG before calling it
done:

```
/tmp/attic-mocks/shot.sh <style>/01-home.html /tmp/attic-mocks/<style>-01.png 1720 1100
```

Arguments are width and height of the viewport; raise the height for long pages, or narrow the
width to 860 to wrap the frames two per row and see them larger. Look for clipped or
overflowing text, rows that collide with the mini player or tab bar, icons that failed to
load, low contrast, misaligned baselines, and anything that still looks like the app you are
supposed to be moving away from. Fix and re-render until the sheet is clean.
