# Sound marks

Marks for Attic drawn from what is kept in an attic and from the sound itself: a record, its
sleeve, a speaker, a tape deck's keys. Each one has to carry the roof as well as the music,
because a bare disc, a note or a pair of headphones belongs to every player on the store.

Every mark sits on the 108 × 108 adaptive-icon canvas inside the safe circle of radius 33 around
(54, 54). The plain files are one `<path>` with `fill="currentColor"`: straight lines and circular
arcs only, no strokes, transforms, gradients or clip-paths. Deck and Lid fill the same under
either rule. Headroom and Corner cut their holes with `fill-rule="evenodd"`, which a
VectorDrawable needs as `android:fillType="evenOdd"`. The `-color` files are full-bleed launcher
icons in three flat colours, background included.

Coordinates are canvas units. "Square" means measured perpendicular to an edge. Sizes at 24 px
are given on the bare canvas (1 unit = 0.22 px) and inside a launcher mask, where the central 72
units fill the icon (1 unit = 0.33 px). Centres of area were measured from a 432 px render.

The "could be mistaken for" notes are from memory. No trademark or image search was run.

Scratch render: `/tmp/attic-mocks/logo-sound.png` (page `/tmp/attic-mocks/logo-sound.html`,
generator `/tmp/attic-mocks/logo-sound-work/build.py`).

## Where this set meets the other two

`arch-4-eject` uses the same tape-deck key as Deck below. The two were drawn independently and
differ in what sits under the roof: a round window there, half a record here. On one sheet they
are one idea with two drawings, and only one should go forward. `letter-2-hold`, a peak over a
dot, is a third relative.

This set also had a gable of five record spines cut on a 45° line. `arch-1-rafters` is the same
picture, so it was taken out and replaced with Lid.

## sound-1-deck — Deck

Files: `sound-1-deck.svg`, `sound-1-deck-color.svg`

**Idea.** The eject key on a tape deck is a triangle over a bar, which is also an attic in
section: the roof space and the floor it stands on. Half a record stands on that floor, under the
ridge. The key comes from machines that play music you own a copy of.

**Construction.**

- Roof: a right isosceles triangle, 45° pitch. Ridge at (54, 30.12), eaves at (26.5, 57.62) and
  (81.5, 57.62): 55 wide, 27.5 high.
- One module, m = 27.5 ÷ 4√2 = 4.861. The midpoint of the roof's base is 4m from each slope,
  square, and everything else is counted in m.
- Half record, centred on that midpoint: spindle hole of radius m, vinyl from m to 2m (9.72),
  clear space from 2m to 3m (14.58). The roof is therefore m thick where the arch comes closest
  to each slope.
- Floor: m below the roof and 2m thick (y 62.48 to 72.20), the same 55 wide.
- Overall 55 × 42.08. Three semicircular arcs and straight lines; no subpath overlaps another.
- Centre of area at (54.0, 57.4); the bounding box is centred on y 51.2, so the canvas centre
  falls between the two. Farthest points: the floor's bottom corners, at 32.98.
- Colour: cream `#F2E9D8` ground; roof, vinyl and floor `#23201D`; a brick `#C4472D` half-disc of
  radius m fills the spindle hole as the label. The vinyl stays dark on purpose: in a warm colour
  the arcs turn into a sunrise.

**Could be mistaken for.** The eject key, since it is one, and on a phone that can mean "remove"
or "unmount". VLC's cone is also a triangle on a base used by a media player; this one is wide
and 45° where the cone is steep, carries concentric arcs and not cross-bands, and is not orange,
but the family resemblance is there. A rainbow or sunrise under a roof. A desk bell or a cloche
on a tray.

**At 24px.** The roof and the floor are untouched. One module is 1.1 px on the bare canvas and
1.6 px inside a mask, so the hole, the vinyl and the clear band are each about a pixel: they
survive as a light arch with a darker band in it, visible in the magnified strip of the scratch
render. A status-bar cut should drop the vinyl and keep a plain arch of radius 2m.

## sound-2-headroom — Headroom

Files: `sound-2-headroom.svg`, `sound-2-headroom-color.svg`

**Idea.** Furniture in an attic gets cut to the slope of the roof. This is a speaker cabinet
built that way, tall on one side and short on the other. Headroom is the height left under a
sloping ceiling, and in audio it is the margin left above the loudest peak.

**Construction.**

- Cabinet: 36 wide (x 35.2 to 71.2), standing on y 78.5. The top is one cut at 30°: the tall
  side is 51.5 high (top at y 27) and the short side 30.72 (top at y 47.78).
- Woofer: a hole of radius 12 centred on (53.2, 60.5), on the cabinet's centreline, 6 clear of
  both sides and of the base. A dust cap of radius 4.8 is left solid in the middle, so the cone
  shows as a ring 7.2 wide.
- Tweeter: a hole of radius 3.8 at (61, 42), under the high side where the slope leaves room for
  it. It clears the slope by 4.1 square, the woofer by 4.3 and the tall side by 6.4.
- One path under the even-odd rule: outline, woofer, cap, tweeter.
- Bounding box centred on (53.2, 52.75), centre of area at (55.0, 57.1). Farthest point: the top
  corner of the tall side, at 32.0.
- Colour: terracotta `#C6533A` ground, cabinet `#F5EDDD`, cone and tweeter `#2A211C` with the cap
  left in the cabinet colour.

**Could be mistaken for.** A bookshelf speaker, which is the intent. A washing machine or a
camera, as any box with a round hole can be. A milk carton, a SIM card or a file icon, because of
the cut corner. In white on black the tweeter becomes an eye and the mark a one-eyed creature.
The slope is the weak half: without the name beside it, a stranger sees a speaker with a slanted
top and not a roof.

**At 24px.** The steadiest of the four. The woofer is 5.3 px across on the bare canvas and 8 px
inside a mask, with its cap still a separate dot; the tweeter is 1.7 px and 2.5 px. At 16 px the
tweeter greys out and it is still a speaker.

## sound-3-lid — Lid

Files: `sound-3-lid.svg`, `sound-3-lid-color.svg`

**Idea.** The house from the old icon, opened: the roof is hinged at one eave like the lid of a
music box, and it will not shut because a record is standing in the way. The attic is the gap
under the lifted roof, and the music is what is in it.

**Construction.**

- Box: 45 × 21 (x 32 to 77, y 55.5 to 76.5), bottom corners radius 2.
- Roof: an isosceles triangle 52 wide and 19.5 high. Each half is a 3-4-5 triangle (rise 19.5,
  half-span 26, rafter 32.5), a pitch of 36.87°. Shut, it would overhang the box by 3.5 a side.
- Hinge at the roof's left eave, (28.5, 52), 3.5 above the box. The roof is turned 22.5° about
  it, which puts the ridge at (45.06, 24.03) and the free eave at (76.54, 32.10).
- Record: a half-disc of radius 10.5 with its flat edge on the hinge line y 52 and a spindle
  half-hole of radius 2.8. Its centre is 36.58 from the hinge, at (65.08, 52), the one place where
  the underside of the roof clears it by 3.5 (36.58 × sin 22.5° = 14 = 10.5 + 3.5).
- Every gap is 3.5: box to roof, box to record, record to roof.
- Bounding box 48.5 × 52.5 centred on (52.75, 50.3); centre of area at (54.1, 54.7). Farthest
  point: the box's bottom right corner, at 32.2.
- Colour: slate `#1D2B3F` ground, box and record `#F3EBDC`, roof `#E7B23C`.

**Could be mistaken for.** A jack-in-the-box, or someone in a pointed hat looking out of a
crate. A gift box. A grand piano with its lid up, seen from the side. A play triangle resting on
a box. The record was first drawn as a ring, and a ring above a box is a padlock, so it is a
solid half-disc now. It is still a house in two pieces: set the roof down level and it is the
silhouette of `docs/logo.svg`, so the tilt has to survive every redraw.

**At 24px.** The weakest of the four on the bare canvas. The record is 4.7 px wide, its hole is
gone and the 3.5 gaps are 0.8 px, so what is left is a box with a wedge over it and a smudge
between them. Inside a mask the record is 7 px, the gaps 1.2 px, and it reads.

## sound-4-corner — Corner

Files: `sound-4-corner.svg`, `sound-4-corner-color.svg`

**Idea.** A record sliding out of its sleeve, with the sleeve stood on one corner so that the
corner is a ridge and its two upper edges are a 45° roof. The record leaves along the slope, the
way a sleeve lets it.

**Construction.**

- Sleeve: a square of side 34 turned 45°, centred on (49.2, 49.2). Ridge corner at (49.2, 25.16);
  the others at (73.24, 49.2), (49.2, 73.24) and (25.16, 49.2).
- Record: radius 15, a 30 disc for a 34 sleeve. Centre (66.59, 66.59), which is 24.6 from the
  sleeve's centre along the diagonal and 7.6 past its lower right edge, so about three quarters
  of the disc is out.
- The visible part of the record is cut parallel to that edge and 3.7 clear of it.
- Groove: a clear ring from radius 7.4 to 10.6, leaving a label of radius 7.4 and an outer band
  4.4 wide. Spindle hole radius 2.6, 1.3 clear of the cut.
- One path under the even-odd rule.
- Bounding box 56.4 square centred on (53.4, 53.4); centre of area at (53.8, 53.8). Farthest
  point: the record's edge on the diagonal, at 32.8.
- Colour: teal `#1D4C49` ground, sleeve `#EFE5D1`, record `#E8873A`.

**Could be mistaken for.** The stock "album" glyph, tilted. A pencil eraser, a marker tip or a
price tag at small sizes. Git's mark is a red-orange diamond and Tidal's is built from diamonds,
which is why the sleeve is neither red nor black. The roof is the problem: nobody sees one unless
told, and without it this is a record in a sleeve, which any player could use.

**At 24px.** A diamond with a round bite beside it. The sleeve is 7.6 px on a side on the bare
canvas and 11.3 px inside a mask. The groove is 0.7 px and disappears, the spindle hole is a
single grey pixel, and the gap between sleeve and record holds at 0.8 px and 1.2 px.

## Explored and dropped

- **Five record spines cut on a 45° line.** Finished, then removed as a duplicate of
  `arch-1-rafters`.
- **A cassette with a gabled top edge.** Two reels side by side are eyes, and the head well
  below them is a mouth. It is a ghost, or an owl.
- **A record under a chevron or a solid gable.** A head in a hat. `arch.md` reached the same
  verdict.
- **A record with a roofline cut through it.** A ladybird, or a Poké Ball.
- **Headphones whose band is a gable.** Legible as headphones under a roof at 150 px. At 28 px it
  is the outline of a home icon, and beside the letter set it is one more A.
- **A triangle of stacked records.** Horizontal bands across a triangle are the VLC cone.
- **Level bars under a roof tilted off them.** A property-market chart.

## Ranking

1. **Deck.** Both halves, the roof and the record, can be read without the name beside it, and
   it is the only symmetric mark of the four. The roof and floor keep their shape down to 24 px,
   and the half record gives the eject key something of its own, which is what `arch.md` finds
   missing in `arch-4-eject`.
   Its risk is the key's own meaning.
2. **Headroom.** A speaker needs no explaining, and it is the steadiest at 24 px. It ranks
   second because the attic is in the name more than in the drawing.
3. **Lid.** It answers the old house icon directly, since it is that icon with the roof lifted,
   and it is the only one with a joke in it. It is also three pieces, it goes soft on the bare
   24 px canvas, and it depends on a 22.5° tilt that a careless redraw will lose.
4. **Corner.** In colour it is the plainest icon of the set, two shapes and one gap. It has the
   weakest link to the word "attic".

Ship Deck, after one check: show the colour icon to a few people with no context and ask what the
app does. If most of them say "eject" or "remove the SD card", ship Headroom.
