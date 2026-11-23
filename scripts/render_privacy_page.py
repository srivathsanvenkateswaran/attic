#!/usr/bin/env python3
"""Writes site/privacy.html from PRIVACY.md, so the policy has one source.

    python3 -m pip install markdown   # once
    python3 scripts/render_privacy_page.py          # rewrite the page
    python3 scripts/render_privacy_page.py --check  # exit 1 if the page is out of date

Edit PRIVACY.md, run this, and commit both files together.
"""

import argparse
import html
import re
import sys
from pathlib import Path

import markdown

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "PRIVACY.md"
TARGET = ROOT / "site/privacy.html"
REPO = "https://github.com/srivathsanvenkateswaran/attic/blob/main/"

TEMPLATE = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{title}</title>
  <meta name="description" content="What Attic reads, stores and sends. Attic has no account, analytics or ads; lyrics lookups go to lrclib.net.">
  <meta name="theme-color" content="#c6533a">
  <link rel="icon" href="img/icon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <!-- Generated from PRIVACY.md by scripts/render_privacy_page.py. Edit that file, not this one. -->
  <div class="top slim">
    <header class="wrap bar">
      <a class="brand" href="./" aria-label="Attic home">
        <svg viewBox="0 0 108 108" aria-hidden="true"><rect width="108" height="108" rx="24" fill="#F5EDDD"/><path fill="#C6533A" d="M35.2,78.5L35.2,47.78L71.2,27L71.2,78.5Z"/><path fill="#F5EDDD" fill-rule="evenodd" d="M41.2,60.5a12,12 0 1,0 24,0a12,12 0 1,0 -24,0ZM48.4,60.5a4.8,4.8 0 1,0 9.6,0a4.8,4.8 0 1,0 -9.6,0ZM57.2,42a3.8,3.8 0 1,0 7.6,0a3.8,3.8 0 1,0 -7.6,0Z"/></svg>
        Attic
      </a>
      <nav aria-label="Site">
        <a href="./">Home</a>
        <a href="https://github.com/srivathsanvenkateswaran/attic">GitHub</a>
      </nav>
    </header>
  </div>
  <main class="wrap">
    <div class="doc">
{body}
    </div>
  </main>
  <footer class="wrap">
    <p>The source of this page is <a href="{repo}PRIVACY.md">PRIVACY.md</a>, and its history lists every change.</p>
  </footer>
</body>
</html>
"""


def render() -> str:
    text = SOURCE.read_text(encoding="utf-8")
    # Repository-relative links point at GitHub once the page is served from Pages.
    text = re.sub(r"\]\((?!https?://|#)([^)]+)\)", lambda m: f"]({REPO}{m.group(1)})", text)
    body = markdown.markdown(text, extensions=["tables"], output_format="html")
    title_match = re.search(r"<h1>(.*?)</h1>", body)
    title = html.unescape(title_match.group(1)) if title_match else "Privacy policy"
    # The first paragraph is the effective date.
    body = body.replace("<p>Effective", '<p class="effective">Effective', 1)
    body = "\n".join("      " + line if line else line for line in body.splitlines())
    return TEMPLATE.format(title=f"{html.escape(title)}", body=body, repo=REPO)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="fail if site/privacy.html is out of date")
    args = parser.parse_args()
    page = render()
    if args.check:
        current = TARGET.read_text(encoding="utf-8") if TARGET.exists() else ""
        if current != page:
            print("site/privacy.html is out of date; run scripts/render_privacy_page.py", file=sys.stderr)
            return 1
        return 0
    TARGET.write_text(page, encoding="utf-8")
    print(f"wrote {TARGET}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
