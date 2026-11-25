#!/usr/bin/env python3
"""Frames the raw store screenshots for Google Play and Product Hunt, and writes the README images.

Reads the 1080 x 2400 captures that StoreShots (app/src/storeShots) writes, and makes:

  play/         eight 1080 x 1920 images, a captioned phone on cream or terracotta
  producthunt/  four 1270 x 760 gallery images, two phones beside a caption
  readme/       eight light-mode screens, 540 x 1200 JPEG, for docs/screenshots

Captions use Google Sans Flex from app/src/main/res/font, the font the Expressive style draws in.

    python3 scripts/store_shots/frame.py [--shots /tmp/attic-dev/store-shots] [--readme docs/screenshots]
"""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FONT = ROOT / "app/src/main/res/font/google_sans_flex.ttf"

TERRACOTTA = (0xC6, 0x53, 0x3A)
CREAM = (0xF5, 0xED, 0xDD)
INK = (0x2B, 0x1D, 0x17)
INK_SOFT = (0x7A, 0x4A, 0x3C)

# Each Play image: the raw shot(s), the ground colour, and the caption in two lines.
PLAY = [
    (["expressive-dark-home"], "terracotta", "Plays the music already on your phone", "No account, no streaming, no ads."),
    (["comfort-light-album"], "cream", "Album pages tinted from the cover", "Built from the tags in your files."),
    (["expressive-dark-now-playing"], "terracotta", "Shuffle, repeat and favourites on one screen", "The line being sung sits under the title."),
    (["comfort-dark-lyrics"], "terracotta", "Synced lyrics that follow the song", "Download them once, then read them offline."),
    (["expressive-light-queue"], "cream", "Drag to reorder the queue", "Swipe a song away to remove it."),
    (["comfort-light-home-mixes"], "cream", "Mixes made from what you play", "Grouped by how the songs sound, new each day."),
    (["expressive-light-home", "comfort-light-home"], "cream", "Two styles to pick from", "Expressive and Comfort, switched in Settings."),
    (["comfort-dark-recap"], "terracotta", "A recap of your year", "Made on the phone from what you played."),
]

PRODUCT_HUNT = [
    (["expressive-dark-home", "comfort-dark-home"], "terracotta", "Two styles to pick from",
     "Expressive and Comfort. Switch between them in Settings without losing your place."),
    (["expressive-light-now-playing", "expressive-light-lyrics"], "cream", "Lyrics that follow the song",
     "The line being sung is highlighted. Download lyrics once, then they work offline."),
    (["comfort-light-album", "comfort-light-artist"], "cream", "Pages built from your own files",
     "Albums and artists come from your tags, coloured to match each cover."),
    (["expressive-dark-recap", "comfort-dark-recap"], "terracotta", "Your year, counted on the phone",
     "Minutes, top songs and streaks from a listening log that stays on the device."),
]

README = [
    ("expressive-light-home", "expressive-home"),
    ("expressive-light-album", "expressive-album"),
    ("expressive-light-artist", "expressive-artist"),
    ("expressive-light-now-playing", "expressive-now-playing"),
    ("comfort-light-home", "comfort-home"),
    ("comfort-light-album", "comfort-album"),
    ("comfort-light-artist", "comfort-artist"),
    ("comfort-light-now-playing", "comfort-now-playing"),
]


def font(size, weight):
    f = ImageFont.truetype(str(FONT), size)
    # Optical size, width, weight, roundness, in the font's own axis order.
    f.set_variation_by_axes([min(48, max(12, size // 2)), 100, weight, 0])
    return f


def colours(ground):
    if ground == "terracotta":
        return TERRACOTTA, CREAM, CREAM
    return CREAM, INK, INK_SOFT


def wrap(draw, text, face, width):
    """Greedy line breaks, then evened out so a second line is never a lone word."""
    words = text.split()
    lines, line = [], ""
    for word in words:
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=face) <= width or not line:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    if len(lines) == 2:
        splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
        fits = [pair for pair in splits if all(draw.textlength(l, font=face) <= width for l in pair)]
        lines = list(min(fits, key=lambda pair: max(draw.textlength(l, font=face) for l in pair)))
    return lines


def phone(shot, height):
    """The screenshot in a thin dark bezel with rounded corners, scaled to [height], on transparent."""
    bezel = max(6, height // 110)
    screen_h = height - 2 * bezel
    screen_w = round(shot.width * screen_h / shot.height)
    radius = round(screen_w * 0.085)
    screen = shot.convert("RGB").resize((screen_w, screen_h), Image.LANCZOS)
    out = Image.new("RGBA", (screen_w + 2 * bezel, height), (0, 0, 0, 0))
    ImageDraw.Draw(out).rounded_rectangle((0, 0, out.width - 1, out.height - 1), radius + bezel, fill=(18, 18, 20, 255))
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius, fill=255)
    out.paste(screen, (bezel, bezel), mask)
    return out


def place(canvas, device, x, y):
    """Pastes [device] with a soft shadow under it."""
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    alpha = device.split()[3].point(lambda a: a * 0.35)
    shadow.paste((0, 0, 0, 255), (x, y + device.height // 60), alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(device.height // 45))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(device, (x, y))


def mark(shots, ground, size):
    name = "mark-on-terracotta" if ground == "terracotta" else "mark-on-cream"
    return Image.open(shots / "raw" / f"{name}.png").convert("RGBA").resize((size, size), Image.LANCZOS)


def caption(draw, lines, faces, colours_, x, y, width, align):
    for text, face, colour, gap in zip(lines, faces, colours_, (14, 0)):
        for line in wrap(draw, text, face, width):
            w = draw.textlength(line, font=face)
            lx = x + (width - w) / 2 if align == "center" else x
            draw.text((lx, y), line, font=face, fill=colour)
            y += face.size * 1.22
        y += gap + face.size * 0.3
    return y


def play_image(shots, names, ground, title, body):
    bg, ink, soft = colours(ground)
    canvas = Image.new("RGBA", (1080, 1920), bg + (255,))
    draw = ImageDraw.Draw(canvas)
    canvas.alpha_composite(mark(shots, ground, 84), (498, 84))
    end = caption(draw, [title, body], [font(64, 700), font(38, 400)], [ink, soft], 80, 200, 920, "center")
    top = round(end) + 40
    raws = [Image.open(shots / "raw" / f"{n}.png") for n in names]
    if len(raws) == 1:
        device = phone(raws[0], 1920 - 56 - top)
        place(canvas, device, (1080 - device.width) // 2, top)
    else:
        # Two phones overlap a little, the second lower and in front.
        height = 1180
        back, front = (phone(r, height) for r in raws)
        place(canvas, back, 24, top + 10)
        place(canvas, front, 1080 - 24 - front.width, min(top + 100, 1920 - 40 - height))
    return canvas.convert("RGB")


def hunt_image(shots, names, ground, title, body):
    bg, ink, soft = colours(ground)
    canvas = Image.new("RGBA", (1270, 760), bg + (255,))
    draw = ImageDraw.Draw(canvas)
    canvas.alpha_composite(mark(shots, ground, 64), (72, 150))
    caption(draw, [title, body], [font(50, 700), font(28, 400)], [ink, soft], 72, 240, 470, "left")
    left, right = (phone(Image.open(shots / "raw" / f"{n}.png"), 650) for n in names)
    right_x = 1270 - 56 - right.width
    place(canvas, right, right_x, 55)
    place(canvas, left, right_x - 28 - left.width, 55)
    return canvas.convert("RGB")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--shots", type=Path, default=Path("/tmp/attic-dev/store-shots"))
    parser.add_argument("--readme", type=Path, help="Also copy the README images here, e.g. docs/screenshots")
    args = parser.parse_args()
    shots = args.shots

    play = shots / "play"
    play.mkdir(parents=True, exist_ok=True)
    for i, (names, ground, title, body) in enumerate(PLAY, 1):
        play_image(shots, names, ground, title, body).save(play / f"{i:02d}-{names[0]}.png", optimize=True)

    hunt = shots / "producthunt"
    hunt.mkdir(parents=True, exist_ok=True)
    for i, (names, ground, title, body) in enumerate(PRODUCT_HUNT, 1):
        hunt_image(shots, names, ground, title, body).save(hunt / f"{i:02d}-{title.lower().replace(' ', '-').replace(',', '')}.png", optimize=True)

    readme = shots / "readme"
    readme.mkdir(parents=True, exist_ok=True)
    targets = [readme] + ([args.readme] if args.readme else [])
    for raw, name in README:
        image = Image.open(shots / "raw" / f"{raw}.png").convert("RGB").resize((540, 1200), Image.LANCZOS)
        for target in targets:
            target.mkdir(parents=True, exist_ok=True)
            image.save(target / f"{name}.jpg", quality=84, optimize=True, progressive=True)

    print(f"Play: {play}\nProduct Hunt: {hunt}\nREADME: {', '.join(str(t) for t in targets)}")


if __name__ == "__main__":
    main()
