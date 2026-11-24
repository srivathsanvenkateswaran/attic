# Phone screenshots

Rendered from a demo library by `scripts/store_shots/run.sh`, which also frames them. Invented artists and generated covers, so no copyrighted artwork appears in the listing.

## What Play accepts

- 2 to 8 phone screenshots, PNG or JPEG, up to 8 MB each, with no alpha channel.
- Each side between 320 and 3840 px, and the long side no more than 2.3 times the short side.
- To be eligible for recommendation placements, at least four screenshots in 9:16 portrait, at
  least 1080×1920. Missing this doesn't block the listing.

Source: [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151),
read 2026-11-23. The brief for this folder said the limit was twice the short side; Play's page now
says 2.3 times.

A raw capture from the test phone is 1080×2400 (20:9, a ratio of 2.22), so Play would accept it
for upload, but it isn't 9:16 and wouldn't count towards the four. `scripts/frame_screenshots.py`
places each capture whole on a 1080×1920 (9:16) card under a caption:

```bash
# captions.tsv: one "file name<TAB>caption" per line
python3 scripts/frame_screenshots.py --captions captions.tsv \
    --out fastlane/metadata/android/en-US/images/phoneScreenshots/ raw/*.png
```

Files upload in name order, so prefix them `1-`, `2-` and so on.

## Suggested set

Use at least four, showing both styles in light and dark:

1. Home, Expressive, dark
2. Album, Expressive, light
3. Now Playing with synced lyrics
4. Queue during radio
5. Home, Comfort, dark
6. Library, Comfort, light
7. Lock screen controls and the home-screen widget
8. Recap

## Before committing a shot

- Use a library you have the right to show. Cover art belongs to its owners, so prefer albums with
  free licences or your own artwork, and keep artist names and covers you don't control out of the
  frame where you can.
- No personal notifications, account names or other apps in the status bar.
