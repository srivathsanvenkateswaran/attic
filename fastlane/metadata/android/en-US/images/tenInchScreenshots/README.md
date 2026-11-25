# Tablet screenshots

This folder and `../sevenInchScreenshots/` hold the tablet screenshots, six each. They are rendered on
the JVM from the demo library (invented artists, generated covers) and framed by
`scripts/store_shots/frame_tablets.py`:

```bash
scripts/store_shots/tablet_refs.sh '*[TenInch-*]'     # 1280 × 800 dp, 2560 × 1600 px
scripts/store_shots/tablet_refs.sh '*[SevenInch-*]'   # 1024 × 640 dp, 2048 × 1280 px
scripts/store_shots/run.sh                            # once, for the brand marks
python3 scripts/store_shots/frame_tablets.py
```

## What Play accepts

- Up to 8 screenshots per device type, PNG or JPEG (24-bit, no alpha), up to 8 MB each.
- Each side between 320 and 3840 px, the long side no more than 2.3 times the short side.
- For tablets and Chromebooks, at least 4 screenshots, 16:9 in landscape or 9:16 in portrait.
- To be eligible for recommendation placements, landscape shots of at least 1920 × 1080.

Source: [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151),
read 2026-11-25. The page also asks that text outside the app's own screen be kept out, because Play
can crop it on some layouts. These images keep the phone set's caption above the tablet; if Play's
preview crops a caption, drop it in `frame_tablets.py` and render again.

## The sets

Both sizes are 16:9: 2560 × 1440 for 10-inch and 1920 × 1080 for 7-inch. As on the phone set, the
styles take turns, dark screens sit on terracotta and light ones on cream.

| # | 10-inch | 7-inch |
|---|---|---|
| 1 | Home, Expressive, dark | Home, Expressive, dark |
| 2 | Library beside an open album, Comfort, light | Library albums in two columns, Comfort, light |
| 3 | Now Playing beside the queue, Expressive, dark | Now Playing beside the queue, Expressive, dark |
| 4 | Now Playing beside lyrics, Comfort, dark | Album in two panes, Comfort, dark |
| 5 | Album in two panes, Expressive, light | Now Playing beside lyrics, Expressive, light |
| 6 | Search results, Comfort, light | Search results, Comfort, light |

The 7-inch set is rendered at 1024 × 640 dp rather than the more common 960 × 600 dp, and swaps two
screens, because of layout faults at that size: Comfort's Now Playing shortens the song title
("Lights Ove…") and squeezes its buttons, Expressive's Now Playing clips its Next button at 960 dp, and
Expressive's album pane clips its Pause button up to about 1066 dp wide.
