#!/usr/bin/env python3
"""Renders the store icon and feature graphic from the Headroom mark.

    python3 scripts/render_store_images.py

Writes fastlane/metadata/android/en-US/images/icon.png (512x512, RGBA, square corners, since
Play applies its own mask) and featureGraphic.png (1024x500, RGB, since Play rejects alpha there).

Needs only Pillow. The mark is drawn from the geometry in
docs/design/2026-11-21-ui-directions/logo/marks/sound-2-headroom-color.svg rather than rasterised
from the file, so no SVG library is required. If that SVG changes, change MARK below to match.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "fastlane/metadata/android/en-US/images"
FONT = ROOT / "app/src/main/res/font/google_sans_flex.ttf"

TERRACOTTA = (0xC6, 0x53, 0x3A)
CREAM = (0xF5, 0xED, 0xDD)
INK = (0x2A, 0x21, 0x1C)

# Headroom, in the SVG's 108-unit canvas.
MARK = {
    "cabinet": [(35.2, 78.5), (35.2, 47.78), (71.2, 27.0), (71.2, 78.5)],
    "woofer": ((53.2, 60.5), 12.0),
    "cap": ((53.2, 60.5), 4.8),
    "tweeter": ((61.0, 42.0), 3.8),
}

# Everything is drawn this many times larger, then scaled down, to anti-alias the edges.
SUPERSAMPLE = 4


def draw_mark(draw: ImageDraw.ImageDraw, origin: tuple[float, float], scale: float) -> None:
    """Draws the cabinet, woofer ring and tweeter with canvas unit (0, 0) at origin."""
    ox, oy = origin

    def pt(x: float, y: float) -> tuple[float, float]:
        return ox + x * scale, oy + y * scale

    def circle(centre: tuple[float, float], radius: float, fill: tuple[int, int, int]) -> None:
        cx, cy = pt(*centre)
        r = radius * scale
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)

    draw.polygon([pt(x, y) for x, y in MARK["cabinet"]], fill=CREAM)
    # The SVG cuts these as even-odd holes; painting them in order gives the same picture.
    circle(*MARK["woofer"], INK)
    circle(*MARK["cap"], CREAM)
    circle(*MARK["tweeter"], INK)


def icon(size: int = 512) -> Image.Image:
    """The launcher icon's colourway, framed on the central 76 of 108 units.

    The launcher shows the central 72 units; four more give the mark a little room once Play
    rounds the corners.
    """
    view_from, view_size = 16.0, 76.0
    big = size * SUPERSAMPLE
    image = Image.new("RGBA", (big, big), TERRACOTTA + (255,))
    scale = big / view_size
    draw_mark(ImageDraw.Draw(image), (-view_from * scale, -view_from * scale), scale)
    return image.resize((size, size), Image.Resampling.LANCZOS)


def font(size: int, weight: int) -> ImageFont.FreeTypeFont:
    face = ImageFont.truetype(str(FONT), size)
    # Axes: optical size, width, weight, roundness.
    face.set_variation_by_axes([min(48, max(12, size // SUPERSAMPLE)), 100, weight, 0])
    return face


def feature_graphic() -> Image.Image:
    width, height = 1024, 500
    s = SUPERSAMPLE
    image = Image.new("RGB", (width * s, height * s), CREAM)
    draw = ImageDraw.Draw(image)

    # The icon as a tile on the left, with Play-like rounded corners since this is a picture of it.
    tile = 300 * s
    tile_x, tile_y = 96 * s, (height * s - tile) // 2
    tile_image = icon(tile // s).resize((tile, tile), Image.Resampling.LANCZOS)
    mask = Image.new("L", (tile, tile), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, tile - 1, tile - 1], radius=tile * 0.22, fill=255)
    image.paste(tile_image.convert("RGB"), (tile_x, tile_y), mask)

    text_x = tile_x + tile + 64 * s
    name = font(132 * s, 700)
    line = font(34 * s, 500)
    small = font(28 * s, 400)

    name_top = 128 * s
    draw.text((text_x, name_top), "Attic", font=name, fill=INK)
    draw.text((text_x, name_top + 168 * s), "Plays the MP3s already", font=line, fill=INK)
    draw.text((text_x, name_top + 212 * s), "on your phone.", font=line, fill=INK)
    draw.text((text_x, name_top + 276 * s), "No account, no streaming, no ads.", font=small, fill=TERRACOTTA)

    return image.resize((width, height), Image.Resampling.LANCZOS)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    icon().save(OUT / "icon.png", optimize=True)
    feature_graphic().save(OUT / "featureGraphic.png", optimize=True)
    for name in ("icon.png", "featureGraphic.png"):
        with Image.open(OUT / name) as written:
            print(f"{OUT / name}: {written.size[0]}x{written.size[1]} {written.mode}")


if __name__ == "__main__":
    main()
