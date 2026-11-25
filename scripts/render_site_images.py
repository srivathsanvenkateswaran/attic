#!/usr/bin/env python3
"""Writes the website's images from the raw store shots and the Headroom mark.

    scripts/store_shots/run.sh                  # once, makes the raw 1080 x 2400 captures
    python3 scripts/render_site_images.py       # write the site's images

Each screen in site/img/screens/ becomes a 360 px and a 720 px WebP, plus a 360 px PNG for
browsers without WebP. It also draws the icons (icon-32.png, apple-touch-icon.png) and the
1200 x 630 sharing image (og.png). The raw captures use a demo library with invented artists and
generated covers.

Needs Pillow and cwebp (brew install webp).
"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
IMG = ROOT / "site/img"
TARGET = IMG / "screens"
FONT = ROOT / "site/fonts/onest.ttf"
DEFAULT_RAW = Path(os.environ.get("STORE_SHOTS_DIR", "/tmp/attic-dev/store-shots")) / "raw"

THEMES = ("light", "dark")
# Screens shown on the page, by the name the store-shot test gives them. Every one is rendered in
# both styles, because the page's Style switch shows one style throughout. The first four fill the
# style viewer; the rest illustrate one feature or section each.
STYLES = ("expressive", "comfort")
SCREENS = (
    "home", "now-playing", "album", "artist",
    "library", "queue", "home-mixes", "lyrics", "recap", "settings-playback", "equaliser",
)
WIDTHS = (360, 720)


def webp(src: Image.Image, width: int, out: Path) -> None:
    height = round(src.height * width / src.width)
    with tempfile.TemporaryDirectory() as tmp:
        png = Path(tmp) / "in.png"
        src.resize((width, height), Image.LANCZOS).save(png)
        subprocess.run(["cwebp", "-quiet", "-q", "78", "-m", "6", "-sharp_yuv", str(png), "-o", str(out)], check=True)


def png(src: Image.Image, width: int, out: Path) -> None:
    height = round(src.height * width / src.width)
    small = src.resize((width, height), Image.LANCZOS)
    # 256 colours keeps the fallback small; only browsers without WebP ever load it.
    small.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(out, optimize=True)


TERRACOTTA = (0xC6, 0x53, 0x3A)
CREAM = (0xF5, 0xED, 0xDD)
DARK = (0x2A, 0x21, 0x1C)
BEZEL = (0x1B, 0x15, 0x12)


def draw_mark(draw: ImageDraw.ImageDraw, x: float, y: float, scale: float) -> None:
    """The Headroom mark in its launcher colours, from sound-2-headroom-color.svg.

    (x, y) is where the 108-unit canvas's origin lands; scale is pixels per unit."""

    def p(u: float, v: float) -> tuple:
        return (x + u * scale, y + v * scale)

    def disc(cu: float, cv: float, r: float, fill: tuple) -> None:
        (a, b), (c, d) = p(cu - r, cv - r), p(cu + r, cv + r)
        draw.ellipse((a, b, c, d), fill=fill)

    draw.polygon([p(35.2, 78.5), p(35.2, 47.78), p(71.2, 27), p(71.2, 78.5)], fill=CREAM)
    disc(53.2, 60.5, 12, DARK)    # the speaker ring
    disc(53.2, 60.5, 4.8, CREAM)  # its cone
    disc(61, 42, 3.8, DARK)       # the tweeter


def icon(size: int, radius: float, out: Path) -> None:
    ss = 8  # draw large, then shrink, for smooth edges
    big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.rounded_rectangle((0, 0, size * ss - 1, size * ss - 1), radius=radius * size * ss, fill=TERRACOTTA)
    draw_mark(d, 0, 0, size * ss / 108)
    big.resize((size, size), Image.LANCZOS).save(out, optimize=True)


def font(px: int, weight: int) -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONT), px)
    f.set_variation_by_axes([weight])
    return f


def phone(shot: Path, width: int) -> Image.Image:
    """A screenshot in a plain bezel, matching the site's CSS frame."""
    ss = 2
    w = width * ss
    pad = round(w * 0.024)
    screen_w = w - 2 * pad
    screen = Image.open(shot).convert("RGB").resize((screen_w, round(screen_w * 2400 / 1080)), Image.LANCZOS)
    h = screen.height + 2 * pad
    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, w - 1, h - 1), radius=round(w * 0.12), fill=BEZEL)
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), radius=round(screen_w * 0.096), fill=255)
    frame.paste(screen, (pad, pad), mask)
    return frame.resize((width, round(h / ss)), Image.LANCZOS)


def og(raw: Path, out: Path) -> None:
    W, H = 1200, 630
    ss = 2
    big = Image.new("RGB", (W * ss, H * ss), TERRACOTTA)
    d = ImageDraw.Draw(big)
    # The mark, cropped to its own bounds: 36 x 51.5 units.
    unit = 2.2 * ss
    draw_mark(d, 72 * ss - 35.2 * unit, 64 * ss - 27 * unit, unit)
    d.text((72 * ss, 286 * ss), "Attic", font=font(92 * ss, 800), fill=CREAM, anchor="ls")
    lines = ["A music player for the songs", "already on your phone."]
    for i, line in enumerate(lines):
        d.text((72 * ss, (366 + i * 48) * ss), line, font=font(38 * ss, 600), fill=CREAM, anchor="ls")
    small = font(25 * ss, 450)
    for i, line in enumerate(["No account, no streaming, no ads.", "Free and open source, for Android."]):
        d.text((72 * ss, (494 + i * 36) * ss), line, font=small, fill=CREAM, anchor="ls")
    img = big.resize((W, H), Image.LANCZOS)
    # Two phones, one per style; the second runs off the bottom edge.
    left = phone(raw / "expressive-light-now-playing.png", 236)
    right = phone(raw / "comfort-dark-home.png", 236)
    img.paste(left, (676, 70), left)
    img.paste(right, (930, 118), right)
    label = font(21, 600)
    dl = ImageDraw.Draw(img)
    dl.text((676 + 118, 70 - 18), "Expressive", font=label, fill=CREAM, anchor="ms")
    dl.text((930 + 118, 118 - 18), "Comfort", font=label, fill=CREAM, anchor="ms")
    img.save(out, optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--raw", type=Path, default=DEFAULT_RAW, help=f"raw captures (default {DEFAULT_RAW})")
    args = parser.parse_args()
    if not shutil.which("cwebp"):
        print("cwebp not found; brew install webp", file=sys.stderr)
        return 1
    TARGET.mkdir(parents=True, exist_ok=True)
    for style in STYLES:
        for theme in THEMES:
            for screen in SCREENS:
                name = f"{style}-{theme}-{screen}"
                source = args.raw / f"{name}.png"
                if not source.exists():
                    print(f"missing {source}", file=sys.stderr)
                    return 1
                image = Image.open(source).convert("RGB")
                for width in WIDTHS:
                    webp(image, width, TARGET / f"{name}-{width}.webp")
                png(image, WIDTHS[0], TARGET / f"{name}-{WIDTHS[0]}.png")
                print(f"wrote {name}")
    icon(32, 0.22, IMG / "icon-32.png")
    icon(180, 0, IMG / "apple-touch-icon.png")  # iOS rounds the corners itself
    og(args.raw, IMG / "og.png")
    print("wrote icons and og.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
