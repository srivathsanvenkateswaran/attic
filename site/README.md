# Attic website

The landing page and privacy policy, served by GitHub Pages from this folder by
`.github/workflows/pages.yml`. Plain HTML and CSS: no scripts, no cookies, no requests to other
servers. The one font, Onest, is self-hosted under the SIL Open Font License (`fonts/OFL-onest.txt`).

The style viewer under "Two styles, one app" runs on radio buttons and the CSS `:has()` selector.
Browsers without `:has()` hide its controls and show Home in the visitor's theme.

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
fallback. `img/og.png` (1200 × 630), `img/apple-touch-icon.png` and `img/icon-32.png` are drawn from
the Headroom mark. All of them come from one script:

```bash
scripts/store_shots/run.sh                  # raw 1080 x 2400 captures of the demo library
python3 scripts/render_site_images.py       # needs Pillow and cwebp (brew install webp)
```

The screenshots use a demo library with invented artists and generated covers. Never put a real
library or real album covers on the site.

## Editing the privacy policy

`PRIVACY.md` is the source. After changing it, run `python3 scripts/render_privacy_page.py` and
commit both files. The Pages workflow fails if `privacy.html` is out of date. The page's header and
footer live in that script's `TEMPLATE`.
