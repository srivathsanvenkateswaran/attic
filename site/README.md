# Attic website

The landing page and privacy policy, served by GitHub Pages from this folder by
`.github/workflows/pages.yml`. Plain HTML and CSS, and one small script, `theme.js`. No cookies,
no requests to other servers. The one font, Onest, is self-hosted under the SIL Open Font License
(`fonts/OFL-onest.txt`).

The style viewer under "Two styles, one app" runs on radio buttons. Style runs on the CSS `:has()`
selector and sets the style of every screenshot on the page. Screen changes the viewer's phone only.
Theme (Match device, Light or Dark) sets the theme of every screenshot and of the page's own
colours.

Every screenshot is one `<picture>` per style, whose dark sources carry
`media="(prefers-color-scheme: dark)"`, so the page follows the device's theme by itself.
`theme.js` rewrites that query on every element that has it when Light or Dark is picked, and sets
`data-theme` on `<html>`, which `style.css` reads for the page's colours. The browser then picks
the other file from the same `srcset`, so only one theme's files load. A new screenshot needs the
same markup as the others: dark sources first, with exactly that `media` value.

Without the script, the Theme buttons stay hidden and everything follows the device's theme.
Browsers without `:has()` hide all the controls and show Expressive throughout, with Home in the
viewer.

Remembering the visitor's Style and Theme across visits is off. To turn it on, set
`REMEMBER_CHOICES` at the top of `theme.js` to `true`: the choices are then kept in the browser's
localStorage, under `attic-site-choices`. That is not a cookie, but it is stored on the visitor's
device, so check the footer line and the privacy policy still read right.

## Turning it on

1. Merge this folder and the workflow into `main`.
2. On GitHub, open the repository's Settings, then Pages. Under "Build and deployment", set Source
   to "GitHub Actions".
3. Open the Actions tab, pick the Pages workflow and click "Run workflow", or push any change under
   `site/` to `main`.

The site is then at `https://srivathsanvenkateswaran.github.io/attic/` and the privacy policy at
`https://srivathsanvenkateswaran.github.io/attic/privacy.html`. That second address goes into Play
Console as the privacy policy URL.

## Once the Play listing is live

In `index.html`, the hero has a dashed "Google Play: coming soon" `<span id="play-slot">`, with a
comment above it. Replace the span with Google's official badge from
https://play.google.com/intl/en_us/badges/, unmodified, saved under `img/` and linked to the
listing:

```html
<a class="play-badge" href="https://play.google.com/store/apps/details?id=app.attic.player">
  <img src="img/google-play-badge.png" alt="Get it on Google Play" width="646" height="250">
</a>
```

`.play-badge` in `style.css` sizes it to the buttons' height. Use the width and height of the file
you download. Then move the badge before "Download the APK" if Play should be the first choice, and
drop "coming soon" from anywhere else it appears.

## Images

`img/screens/` holds the phone screenshots, each as a 360 px and a 720 px WebP and a 360 px PNG
fallback, and every screen is rendered in both styles, light and dark. The tablet section's
screenshot (`tablet-*`) is a landscape 10-inch render, as a 720 px and a 1440 px WebP and a 720 px
PNG. `img/og.png` (1200 × 630),
`img/apple-touch-icon.png` and `img/icon-32.png` are drawn from the Headroom mark. All of them come
from one script:

```bash
scripts/store_shots/run.sh                  # raw 1080 x 2400 captures of the demo library
scripts/store_shots/tablet_refs.sh '*[TenInch-*]'   # the 2560 x 1600 tablet renders
python3 scripts/render_site_images.py       # needs Pillow and cwebp (brew install webp)
```

The screenshots use a demo library with invented artists and generated covers. Never put a real
library or real album covers on the site.

## Editing the privacy policy

`PRIVACY.md` is the source. After changing it, run `python3 scripts/render_privacy_page.py` and
commit both files. The Pages workflow fails if `privacy.html` is out of date. The page's header and
footer live in that script's `TEMPLATE`.
