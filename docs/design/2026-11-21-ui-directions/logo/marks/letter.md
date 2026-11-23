# Letter marks

Monograms for Attic built on the letter A. Every mark is drawn on the 108 × 108 adaptive-icon
canvas, centred on (54, 54), inside the 66-diameter safe circle. The plain files are one colour
(`fill="currentColor"`, filled paths only, no strokes, transforms or clip-paths), so each converts
to a VectorDrawable path-for-path. The `-color` files are full-bleed launcher icons in three flat
colours.

All coordinates below are in canvas units. Weights are measured perpendicular to the stroke unless
stated otherwise.

The "could be mistaken for" notes come from memory of other marks. No trademark or image search
was run, so treat them as a list of things to check, not as clearance.

Scratch render of everything here: `/tmp/attic-mocks/logo-letter.png`
(page: `/tmp/attic-mocks/logo-letter.html`, generator: `/tmp/attic-mocks/logo-letter/build.py`).

## letter-1-side-a — Side a

Files: `letter-1-side-a.svg`, `letter-1-side-a-color.svg`

**Idea.** A lowercase double-storey a whose bowl is a record: a solid disc with a spindle hole,
standing free of the letter. The stem and hook curve over it the way a tonearm sits over a
platter. The name on the wordmark is lowercase, and "side a" is what is printed on the first side
of a record or a tape.

**Construction.**

- Record: circle of radius 17 centred (46.5, 62). Spindle hole of radius 4.2 on the same centre.
- Arm weight 8.5 throughout. Stem from x 67 to 75.5, running from y 41.5 down to the baseline at
  y 79. The record also bottoms out at y 79.
- Gap between record and stem: 3.5.
- Hook: the upper half of an ellipse centred (54.75, 41.5). Centreline radii 16.5 × 10, so the
  outer edge is 20.75 × 14.25 and the inner edge 12.25 × 5.75. The terminal is cut flat on
  y 41.5, from x 34 to 42.5, and clears the record by 4.
- Bounding box 46 × 51.75 (x 29.5 to 75.5, y 27.25 to 79). The bowl takes 34 of the 51.75 height,
  about two thirds, which is the proportion of an ordinary grotesque a. The farthest point from
  the centre is the stem's foot, at 32.97.
- Colour: paper `#F2E8D5` background, record `#1B1714`, arm `#E4472B`. A light, warm tile is rare
  among music apps, which are mostly black or saturated.

**Could be mistaken for.** An @ sign at a glance. Audacy, a radio app, also uses a lowercase a
as its mark; put the two side by side before committing. Readers of Thai or Georgian may see a letter of
their own (ล, მ). Beats made the same kind of move with a b that is a headphone; the shapes do
not resemble each other.

**At 24px.** The best of the set. The disc is the largest solid in any of the marks, the hole is
about 3 px across inside a launcher mask and about 2 px on the full 108 canvas at 24 px. The
3.5 gap between record and stem is the first thing to close, and when it does the result is
still a readable bold a.

## letter-2-hold — Hold

Files: `letter-2-hold.svg`, `letter-2-hold-color.svg`

**Idea.** A capital A with the crossbar dropped and a dot under the ridge. A peak over a dot is
the short fermata in music notation, the sign that tells a player to hold the note; an attic is
where you hold on to things. The two rafters are also a pause symbol leaned together into a roof,
and in the colour version the dot is the bulb left on in the attic.

**Construction.**

- Rafters: an equilateral triangle with circumradius 32 on the canvas centre. Apex (54, 22), feet
  on y 70, outer corners at x 26.29 and 81.71. Both rafters are at 60°.
- Rafter weight 9, which is 10.39 measured along the baseline. Inner apex at (54, 40). Feet cut
  flat on the baseline; all corners sharp.
- Dot: radius 7.25 centred (54, 63.5). That position leaves exactly 4.5 between the dot and each
  rafter. Its bottom is at y 70.75, a 0.75 overshoot below the feet.
- Colour: night `#1E2340` background, rafters `#F3EBDD`, dot `#F5A623`.

**Could be mistaken for.** A pendant lamp, a tent with someone inside, a pair of compasses. The
Arch Linux logo is a pointed arch with a small notch at its base, and the Android Auto mark is a
bare upward chevron; the dot and the flat feet separate this from both, but avoid a blue version.
It is the plainest of the set: an A with a dot could belong to several brands, and only people
who read music will see the fermata unprompted.

**At 24px.** Holds completely. Nothing in it is thinner than 9 units or closer than 4.5, so it is
a clean chevron and dot down to a status-bar size.

## letter-3-loft — Loft

Files: `letter-3-loft.svg`, `letter-3-loft-color.svg`

**Idea.** A heavy capital A with one groove cut into it, the way a groove is cut into a record.
The groove follows the outline all the way round, and the one piece it leaves whole is the
triangle under the ridge: the attic. The inline cut is also the lettering of 1970s record
sleeves.

**Construction.**

- 2:1 grid: every diagonal rises 2 for each 1 across (63.43°). Apex (54, 22), baseline y 73.2,
  feet from x 28.4 to 79.6. The letter is 51.2 wide and 51.2 tall and its three corners sit on
  the radius-32 circle.
- The stroke is 14.4 wide measured horizontally, split 5.4 / 3.6 / 5.4 into strip, groove, strip.
  Perpendicular to the leg that is 4.83 / 3.22 / 4.83, a 3 : 2 : 3 ratio.
- Outer piece: the two rafters, one strip wide, joined at the apex.
- Attic: solid triangle (54, 40), (61.6, 55.2), (46.4, 55.2), 15.2 wide and 15.2 tall.
- Crossbar from y 58.4 to 63.2 (4.8 thick), separated from the attic by a 3.2 groove. It is one
  piece with the two inner leg strips.
- Opening between the legs: 12.4 wide under the bar, 22.4 wide at the baseline.
- Colour: mustard `#E3A72B` background, rafters and frame `#2A1B12`, attic `#A9361C`.

**Could be mistaken for.** An A-frame cabin or a tent, so an outdoor brand. AIMP, a music player,
uses a triangular A. It is the closest of the four to the other designers' roofline territory.

**At 24px.** The weakest small. Inside a launcher mask the groove is about 1 px and still shows
as a hairline; on the full canvas at 24 px it drops to about 0.7 px and the mark turns into a grey,
slightly soft A. The silhouette survives, the idea does not. It is at its best from 48 px up.

## letter-4-tie-beam — Tie beam

Files: `letter-4-tie-beam.svg`, `letter-4-tie-beam-color.svg`

**Idea.** In a roof truss the horizontal member that ties the two rafters together is the tie
beam, and in an A it is the crossbar. Here it is a seek bar: thick where the track has played, a
round thumb, thin for what is left. It runs past both rafters so it reads as a control laid
across the letter and not as a crossbar with a lump on it.

**Construction.**

- Rafters: the same equilateral triangle as Hold (apex (54, 22), baseline y 70, feet x 26.29 to
  81.71, both at 60°), weight 8, which is 9.24 along the baseline.
- Beam centred on y 60, from x 26.29 to 81.71, so its ends are plumb above the outer corners of
  the feet. Both ends are rounded.
- Played part: 6 thick, from the left end to the thumb. Remaining part: 3 thick, from the thumb
  to the right end.
- Thumb: radius 6 at (56.5, 60), which is 54.5% along the beam. It clears the right rafter by
  4.2, and 9.2 of played bar shows between the left rafter and the thumb.
- Colour: pine `#16332B` background, rafters and the remaining track `#F1E9D8`, played track and
  thumb `#FF7F57`.

**Could be mistaken for.** A struck-through A (Ⱥ) or the austral sign (₳). A drafting compass
with its adjusting wheel, which is why the thumb sits off-centre. In one colour the played and
remaining halves are hard to tell apart; the idea depends on the two-colour version.

**At 24px.** Legible as an A with a bar through it. The thumb is about 3 px across and the
difference between the 6 and 3 thicknesses is gone. It shares its skeleton with Hold, and Hold is the
cleaner mark at this size.

## Wordmark

File: `letter-wordmark.svg` (viewBox `13.5 0 220 108`, one colour, all paths)

Side a followed by "attic" in **Figtree** SemiBold (variable font instanced at wght 600). Figtree
is on Google Fonts under the SIL Open Font License, and the app already bundles it at
`app/src/main/res/font/figtree.ttf`; the outlines were taken from that file. Its a is
double-storey, like the mark.

- The mark is unchanged from `letter-1-side-a.svg`, at the same coordinates.
- Text size 62, so the x-height is 31 against the mark's 51.75. Baseline y 79, shared with the
  mark. Tracking −0.3. The text starts 15 to the right of the mark's stem.
- Stem weight of the text is 6.9 against the mark's 8.5, so the mark stays the heavier element.

The scratch render also shows a second arrangement that is not shipped as a file: the mark used
as the first letter, with "ttic" in Figtree at wght 470 and size 103.5 so the x-height equals the
mark's height and the stems match (9.3 against 8.5). It reads as one word with a record in it and
avoids the doubled a of the lockup. I would use the lockup next to an app icon tile, and the
one-word version wherever the name stands alone, such as a splash screen or the README header.

## Explored and dropped

- **Level-meter bars forming the A.** Five stepped pills with the centre one tallest read as a
  raised middle finger. Slicing a solid A into vertical bars read as a rocket.
- **Slanted crossbar**, after the archaic Attic alpha. A crisp, sporty A with a story about the
  name and nothing about music.
- **Wavy crossbar.** Reads as water, not sound.
- **LED matrix A.** A pixel-font letter; with the vertical runs merged it reads as an H.
- **Lowercase a with a gable for a hook.** The current house-with-a-hole mark again.
- **Seek bar kept between the rafters.** The thumb fuses with the legs into a lump; running the
  beam past the rafters (Tie beam) fixed it.

## Ranking

1. **Side a.** Ship this one. It reads as the first letter of the name and as a record in the
   same glance, without explanation. It is the only mark here that does not look like a roof or
   a home button, it keeps its idea at 24 px, and it leads straight into the lowercase wordmark.
2. **Hold.** The most robust shape and the best story for anyone who reads notation, but the
   least ownable silhouette.
3. **Loft.** The most handsome at launcher size and above, and the one with the most character
   in colour. It loses its groove at small sizes and leans toward the roofline territory.
4. **Tie beam.** The only one that says "player" outright, and only in two colours. It is a
   sibling of Hold; the two should not both go forward.
