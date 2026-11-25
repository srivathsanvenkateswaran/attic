#!/usr/bin/env python3
"""Frames the tablet renders as Google Play's 7-inch and 10-inch tablet screenshots.

Reads the store-size renders that tablet_refs.sh writes (2560 x 1600 and 2048 x 1280, both landscape)
and makes 16:9 images, a captioned tablet on cream or terracotta, as frame.py does for phones:

  sevenInchScreenshots/  six 1920 x 1080 images
  tenInchScreenshots/    six 2560 x 1440 images

    scripts/store_shots/tablet_refs.sh '*[TenInch-*]'
    scripts/store_shots/tablet_refs.sh '*[SevenInch-*]'
    scripts/store_shots/run.sh      # once, for the brand marks beside the captions
    python3 scripts/store_shots/frame_tablets.py [--renders /tmp/attic-dev/tablet-renders]
        [--shots /tmp/attic-dev/store-shots] [--out fastlane/metadata/android/en-US/images]
"""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw

from frame import ROOT, caption, colours, font, mark, place

# Each image: the render (<style>-<theme>-<key>), the ground colour, and the caption in two lines.
# Dark screens sit on terracotta and light ones on cream, and the styles take turns, as on the phone set.
TEN = [
    ("expressive-dark-home", "terracotta", "A rail at the side, the player along the bottom", "Turn the tablet either way and Attic keeps your place."),
    ("comfort-light-library_albums", "cream", "The Library beside the album you open", "Pick an album and its songs appear next to the list."),
    ("expressive-dark-now_playing", "terracotta", "Now Playing beside the queue", "See what plays next without leaving the song."),
    ("comfort-dark-now_playing_lyrics", "terracotta", "Synced lyrics beside the player", "Download them once, then read them offline."),
    ("expressive-light-album", "cream", "Albums in two panes", "The cover and buttons stay put while the songs scroll."),
    ("comfort-light-search_results", "cream", "Search results in columns", "Artists, albums and songs from your own files."),
]

# At the smaller size Comfort's Now Playing shortens the title and Expressive's album pane clips its
# Pause button, so those two screens come from the other style.
SEVEN = [
    ("expressive-dark-home", "terracotta", "A rail at the side, the player along the bottom", "Turn the tablet either way and Attic keeps your place."),
    ("comfort-light-library_albums", "cream", "Your whole library at a glance", "Albums, artists and playlists in two columns."),
    ("expressive-dark-now_playing", "terracotta", "Now Playing beside the queue", "See what plays next without leaving the song."),
    ("comfort-dark-album", "terracotta", "Albums in two panes", "The cover and buttons stay put while the songs scroll."),
    ("expressive-light-now_playing_lyrics", "cream", "Synced lyrics beside the player", "Download them once, then read them offline."),
    ("comfort-light-search_results", "cream", "Search results in columns", "Artists, albums and songs from your own files."),
]

SIZES = [
    # Folder, render size key, image size, the set.
    ("tenInchScreenshots", "ten-inch", (2560, 1440), TEN),
    ("sevenInchScreenshots", "seven-inch", (1920, 1080), SEVEN),
]


def tablet(shot, height):
    """The screenshot in a thin dark bezel with rounded corners, scaled to [height], on transparent."""
    bezel = max(8, height // 48)
    screen_h = height - 2 * bezel
    screen_w = round(shot.width * screen_h / shot.height)
    radius = round(screen_h * 0.035)
    screen = shot.convert("RGB").resize((screen_w, screen_h), Image.LANCZOS)
    out = Image.new("RGBA", (screen_w + 2 * bezel, height), (0, 0, 0, 0))
    ImageDraw.Draw(out).rounded_rectangle((0, 0, out.width - 1, out.height - 1), radius + bezel, fill=(18, 18, 20, 255))
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius, fill=255)
    out.paste(screen, (bezel, bezel), mask)
    return out


def play_image(shots, render, size, ground, title, body):
    w, h = size
    s = w / 2560
    bg, ink, soft = colours(ground)
    canvas = Image.new("RGBA", size, bg + (255,))
    draw = ImageDraw.Draw(canvas)
    title_face, body_face = font(round(64 * s), 700), font(round(38 * s), 400)
    # The mark, then the two caption lines beside it, centred as one group.
    text_w = round(max(draw.textlength(title, font=title_face), draw.textlength(body, font=body_face)))
    mark_px = round(96 * s)
    gap = round(36 * s)
    left = (w - (mark_px + gap + text_w)) // 2
    top = round(52 * s)
    canvas.alpha_composite(mark(shots, ground, mark_px), (left, top + round(10 * s)))
    end = caption(draw, [title, body], [title_face, body_face], [ink, soft], left + mark_px + gap, top, text_w + 2, "left")
    device_top = round(end) + round(28 * s)
    device = tablet(Image.open(render), h - round(48 * s) - device_top)
    place(canvas, device, (w - device.width) // 2, device_top)
    return canvas.convert("RGB")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--renders", type=Path, default=Path("/tmp/attic-dev/tablet-renders"))
    parser.add_argument("--shots", type=Path, default=Path("/tmp/attic-dev/store-shots"), help="where run.sh left raw/mark-*.png")
    parser.add_argument("--out", type=Path, default=ROOT / "fastlane/metadata/android/en-US/images")
    args = parser.parse_args()
    for folder, key, size, shots in SIZES:
        target = args.out / folder
        target.mkdir(parents=True, exist_ok=True)
        for old in target.glob("*.png"):
            old.unlink()
        for i, (name, ground, title, body) in enumerate(shots, 1):
            render = args.renders / f"tablet-{key}-{name}.png"
            image = play_image(args.shots, render, size, ground, title, body)
            image.save(target / f"{i:02d}-{name.replace('_', '-')}.png", optimize=True)
        print(f"{folder}: {target}")


if __name__ == "__main__":
    main()
