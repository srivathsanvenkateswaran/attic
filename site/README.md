# Attic website

The landing page and privacy policy, served by GitHub Pages from this folder by
`.github/workflows/pages.yml`. Plain HTML and CSS: no scripts, no cookies, no requests to other
servers. The one font, Onest, is self-hosted under the SIL Open Font License (`fonts/OFL-onest.txt`).

## Turning it on

1. Merge this folder and the workflow into `main`.
2. On GitHub, open the repository's Settings, then Pages. Under "Build and deployment", set Source
   to "GitHub Actions".
3. Open the Actions tab, pick the Pages workflow and click "Run workflow", or push any change under
   `site/` to `main`.

The site is then at `https://srivathsanvenkateswaran.github.io/attic/` and the privacy policy at
`https://srivathsanvenkateswaran.github.io/attic/privacy.html`. That second address goes into Play
Console as the privacy policy URL.

## Before announcing it

- **Screenshots.** `index.html` has two phone frames with placeholders. Save one Expressive and one
  Comfort screenshot as `img/expressive.png` and `img/comfort.png` (raw 1080×2400 captures are
  fine), then swap each placeholder `<div>` for the `<img>` tag in the comment next to it.
- **Play badge.** "Coming to Google Play" stands in for the badge. Once the listing is live, replace
  it with Google's official badge from https://play.google.com/intl/en_us/badges/, unmodified,
  linking to `https://play.google.com/store/apps/details?id=app.attic.player`.
- **Licence link.** The footer links to `LICENSE` on `main`, which only exists once the GPL licence
  commit is merged there.

## Editing the privacy policy

`PRIVACY.md` is the source. After changing it, run `python3 scripts/render_privacy_page.py` and
commit both files. The Pages workflow fails if `privacy.html` is out of date.
