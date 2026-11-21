# Architecture marks

Marks for Attic drawn from the roof space itself: the gable, the rafters, the pitch of the
roofline, the round window. Each one is also a piece of audio equipment or notation. None has
walls, a door or a floor plan, which is what separates them from the house outline in
`docs/logo.svg`.

Every mark sits on the 108 × 108 adaptive-icon canvas inside the safe circle of radius 33 around
(54, 54). The plain files are one `<path>` with `fill="currentColor"` and the non-zero rule: no
strokes, transforms, gradients or clip-paths, only `M`, `L`, `A` and `Z`, so each pastes into a
VectorDrawable `pathData` as it is. The `-color` files are full-bleed launcher icons in flat
colours. Their palettes were picked to differ from the four in `letter.md`, so the two sets can be
told apart on one sheet.

Coordinates are canvas units. "Square" means measured perpendicular to a slope; "plumb" means a
vertical cut. Sizes at 24 px are given both inside a launcher mask, where the central 72 units
fill the icon (1 unit = 0.33 px), and on the bare canvas (1 unit = 0.22 px).

The "could be mistaken for" notes are from memory. No trademark or image search was run.

Scratch render: `/tmp/attic-mocks/logo-arch.png` (page `/tmp/attic-mocks/logo-arch.html`,
generator `/tmp/attic-mocks/logo-arch/build.py`, extent check `measure.py` beside it).

## arch-1-rafters — Rafters

Files: `arch-1-rafters.svg`, `arch-1-rafters-color.svg`

**Idea.** A gable built from five upright bars whose tops are cut on the roof pitch. As
architecture it is a run of rafters, or the glazing bars of an A-frame window. As sound it is a
level meter, or a rank of organ pipes in a loft. The roof is not drawn; the cut tops are the only
thing that makes it one.

**Construction.**

- Bars 9 wide, 4 apart, so they repeat every 13. Left edges at x 23.5, 36.5, 49.5, 62.5 and 75.5;
  the set is 61 wide and symmetric about x 54.
- All five stand on y 66.
- Every top lies on y = 29.5 + |x − 54|: a 45° pitch (12-in-12) with the ridge at (54, 29.5).
- That gives the outer bars a height of 6 at the eave rising to 15, the second pair 19 rising to
  28, and the centre bar 32 at its shoulders and 36.5 at the ridge. The centre bar is the only
  one with a point.
- Straight segments only: 5 polygons, 21 vertices. Overall 61 × 36.5.
- The centre of area is at (54.0, 53.7), so the mark is centred by weight and its bounding box
  sits 6.25 high. The farthest points from the centre are the two outer bottom corners, at 32.8.
- Colour: aubergine `#2A1A3E` background; bars `#EE8A5E` at the eaves, `#F7BE84` next, `#FCEBD2`
  at the ridge, lightest where a lamp under the ridge would be.

**Could be mistaken for.** A bar chart, so an analytics or fitness app, or a skyline with one
tower. The Adidas mountain is three slanted bars in a triangle; this is five upright ones, but it
is the nearest well-known shape. Among music apps, Deezer's old mark was columns of stacked
segments and Audiomack's is an orange waveform on black, which is why the colour version is not
orange on black and the bars are flat-footed, not mirrored about a centre line. People who do not
see a roof will still see sound, which is the better half to keep.

**At 24px.** Bars are 3 px and gaps 1.3 px inside a mask: all five read, and so does the pitch.
On the bare canvas the gaps drop to 0.9 px and go grey without closing. At 16 px it is a ribbed
triangle. There is no detail to lose, since the mark has nothing in it but the bars.

## arch-2-roofbeam — Roof beam

Files: `arch-2-roofbeam.svg`, `arch-2-roofbeam-color.svg`

**Idea.** Two beamed notes whose beam is bent into a gable. "Beam" and "pitch" are words that
roofs and notation share, and the mark is that overlap drawn once: the stems are the posts, the
beam is the roof.

**Construction.**

- 45° throughout. Stems 6 wide, at x 41 to 47 and x 72 to 78.
- Beam: ridge at (59.5, 24.5), falling at 45° to the outer faces of the stems at y 43. It is 9
  deep measured plumb, 6.36 square. Inner ridge at (59.5, 33.5). Both ends are plumb cuts, flush
  with the stems.
- Note heads: ellipses 21 × 15.5, tilted 20° with the right side up, centred (36.78, 68) and
  (67.78, 68). Each head's widest point touches its stem's right face, and the stem ends at that
  height (y 66.42) so the two meet on a tangent.
- Both heads hang to the left of their stems, as written notes do. That keeps it a pair of notes
  and not a symmetric house. The cost is a mark that cannot be centred both ways: the centre of
  area is at (55.8, 56.1) and the centre of the 51.2 × 51.6 bounding box at (52.3, 50.3), with the
  canvas centre between them.
- Farthest point: the lower left of the left head, at 32.5.
- In the one-colour path the stems run up inside the beam and rely on the non-zero rule to merge.
  Do not switch the path to even-odd.
- Colour: pale sky `#CFE1EE` background, notes `#17233F`, roof `#D2552E`. The roof is drawn over
  the stems, so it sits on them.

**Could be mistaken for.** Apple Music, whose icon is a white beamed pair on red. The gable and a
palette with no red or pink background keep it apart, but it is the same genre of mark, and
Samsung Music also uses a note. It is also the closest of the four to a house: a roof on two
posts. The note heads and the missing floor are what stop it, and a careless redraw that centred
the heads under the stems would turn it into a home icon with feet.

**At 24px.** Reads as a pair of notes with a pointed beam. The heads are about 7 px wide inside a
mask and 4.5 px on the bare canvas, and they are the heaviest thing in the mark at every size.
The stems are 2 px and 1.3 px. At 16 px the gable flattens toward an ordinary beam and the roof
half of the idea is the part that goes.

## arch-3-eave — Eave

Files: `arch-3-eave.svg`, `arch-3-eave-color.svg`

**Idea.** A play triangle tucked under a roof. The roof is lopsided: a short slope, a ridge, and
a long slope that runs parallel to the triangle's top edge and past its tip. A symmetric gable
over a play button is what property-video companies use, and it reads as "home". The long slope
gives this one a direction, the same one the triangle points in.

**Construction.**

- 30° throughout: every edge is at 30°, 150° or plumb.
- Play triangle: equilateral, edge 34, vertices (45.78, 45.08), (75.22, 62.08), (45.78, 79.08).
- Roof: 8 thick square (9.24 plumb), and 6 clear of the triangle's top edge, square.
- Ridge at (45.78, 28.92), directly above the triangle's upright edge.
- Long slope: falls 30° to the right as far as x 84.22, which is 9 past the triangle's tip.
- Short slope: falls 30° to the left for a run of 22, to x 23.78.
- Both roof ends are plumb cuts. Two polygons, 9 vertices.
- Bounding box 60.4 × 50.2, centred on (54, 54); centre of area at (54.8, 52.0). Farthest point:
  the top of the short slope's end, at 32.7.
- Colour: cobalt `#2B44C7` background, roof `#FFCB47`, triangle `#F6EEDD`.

**Could be mistaken for.** A play button in a cap, which is close enough to the intent. A
real-estate or home-cinema logo, if someone straightens the roof. A paper-plane "send" icon at a
glance, since both are a triangle with a fold line. Google Play is a multicoloured triangle and
Poweramp a triangle on black; neither has a roof, but a play triangle is the most used shape in
the category and this mark does not escape that.

**At 24px.** It holds: two solids with a 2 px gap inside a mask and 1.3 px on the bare canvas.
The short slope is the first part to soften, and at 16 px it is a play triangle with a tick over
it.

## arch-4-eject — Eject

Files: `arch-4-eject.svg`, `arch-4-eject-color.svg`

**Idea.** An attic in section is a triangle above a line: the roof space, and the ceiling of the
floor below. That is also the eject key from a tape deck or a CD player, a control that only
exists for music you own a copy of. The round window in the gable makes the triangle a room.

**Construction.**

- Gable: base 54 wide on y 59.4 (x 27 to 81), apex (54, 27), height 32.4. Rise to half-span is
  6 : 5, a pitch of 50.2°.
- Window: radius 7.5, centred on the triangle's incentre at (54, 46.75). The inradius is 12.65,
  so the wall between the window and each of the three sides is the same 5.15.
- Floor: a bar 54 × 7 from y 65.4 to 72.4, 6 below the gable.
- Overall 54 × 45.4. The window is wound against the triangle, so the single path fills correctly
  under either rule.
- Centre of area at (54.0, 56.0); the bounding box sits 4.3 high. Farthest points: the bar's
  bottom corners, at 32.7.
- Colour: pale mint `#BFE0D8` background, gable and floor `#0E3B43`. The window is cut through to
  the background.

**Could be mistaken for.** The eject key, since it is one. On a player that could be read as
"remove" or "stop", which is the wrong first message. The Eye of Providence, a pyramid with a
round eye, or a warning triangle. It is also a cousin of `letter-2-hold`, a peak over a dot, and the
two should not both go forward. Of the four it has the least of its own: it is a system glyph
with a hole in it.

**At 24px.** Holds better than the other three: it is three solids, a window 5 px across inside a
mask and 3.3 px on the bare canvas, and a 2 px gap above the floor. At 16 px the window is a grey
dot and the rest is unchanged.

## Explored and dropped

- **Record under a gable.** A ring under a chevron is an eye under a brow, or a doorbell camera.
  Made large, with highlights so it reads as vinyl, it becomes a round head in a conical hat.
- **Gable cut from a record**, with grooves and a label. The Eye of Providence, exactly.
- **Fanlight**, a half-round window with radiating bars, alone or set in a gable. A handsome
  sunrise that says nothing about music.
- **Rafters receding to a window.** Nested chevrons over a dot are an upside-down Wi-Fi symbol, a
  bad sign for an offline player.
- **Stacked rooflines**, three level lines each with a gable in it. Good at 192 px; at 24 px it is
  three wavy stripes, and at any size it is a sergeant's insignia.
- **Headphones with a gable for a band.** A house outline with thick walls: the home button.
- **Symmetric roof over a play triangle.** Clean and generic; Eave is what it turned into.
- **Shaft of light from a skylight** cut through a solid gable. A mountain with a piece missing.
- **Round window with glazing bars as guitar strings.** A barred window.
- **The triangle as the percussion instrument**, an open outline with a gap. Reads as a letter A.

## Ranking

1. **Rafters.** Ship this one. It is the only mark here that is neither a borrowed glyph nor a
   house: the note, the play triangle and the eject key all belong to everyone, and five bars cut
   on a 45° line do not. It survives as a silhouette, loses nothing at 24 px, and the bars give
   the app something to animate, since the same five can be the now-playing indicator.
2. **Roof beam.** The one a stranger understands fastest, music and a roof in one glance, and the
   warmest in colour. It ranks second because a pair of notes is the most common mark in the
   category and because it is one careless redraw away from a house.
3. **Eave.** Says "player" outright and holds at every size. Its roof is its own, but most of
   its area is a play triangle, and that can never be owned.
4. **Eject.** The strongest silhouette and the best fit to the word "attic", with the weakest
   claim to being a logo. A good monochrome notification glyph, a poor first impression.
