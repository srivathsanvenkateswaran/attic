#!/usr/bin/env python3
"""Frames raw phone screenshots for the Play listing.

A 1080x2400 capture is 20:9. Play accepts that for upload (its limit is a long side 2.3 times the
short side), but only 9:16 portrait shots of at least 1080x1920 count towards the four Play wants
before it features an app in recommendations. This puts each capture, whole and with rounded
corners, on a 1080x1920 card under a caption, on a terracotta or cream ground.

    python3 scripts/frame_screenshots.py --out framed/ \\
        --caption "Albums tinted from their covers" home.png

    # One caption per file: a TSV of file name, tab, caption.
    python3 scripts/frame_screenshots.py --out framed/ --captions captions.tsv shots/*.png

Grounds alternate terracotta, cream, terracotta... unless --ground picks one. Output files keep
their input names and are written as RGB PNGs, since Play rejects alpha in screenshots.
Needs only Pillow.
"""

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FONT = ROOT / "app/src/main/res/font/google_sans_flex.ttf"

TERRACOTTA = (0xC6, 0x53, 0x3A)
CREAM = (0xF5, 0xED, 0xDD)
INK = (0x2A, 0x21, 0x1C)

CARD_W, CARD_H = 1080, 1920
CAPTION_SIZE = 64
CAPTION_LINE = 80
CAPTION_MAX_LINES = 2
CAPTION_WIDTH = 920
SCREEN_TOP = 380
SCREEN_BOTTOM_MARGIN = 72
BEZEL = 14
CORNER = 44


def caption_font() -> ImageFont.FreeTypeFont:
    face = ImageFont.truetype(str(FONT), CAPTION_SIZE)
    face.set_variation_by_axes([48, 100, 600, 0])  # optical size, width, weight, roundness
    return face


def wrap(text: str, face: ImageFont.FreeTypeFont, width: int) -> list[str]:
    lines: list[str] = []
    for word in text.split():
        trial = f"{lines[-1]} {word}" if lines else word
        if lines and face.getlength(trial) <= width:
            lines[-1] = trial
        else:
            lines.append(word)
    if len(lines) > CAPTION_MAX_LINES:
        raise ValueError(f"caption needs {len(lines)} lines, at most {CAPTION_MAX_LINES} fit: {text!r}")
    for line in lines:
        if face.getlength(line) > width:
            raise ValueError(f"caption word too wide to fit: {line!r}")
    return lines


def frame(shot: Image.Image, caption: str, ground: tuple[int, int, int]) -> Image.Image:
    ink = CREAM if ground == TERRACOTTA else INK
    card = Image.new("RGB", (CARD_W, CARD_H), ground)

    face = caption_font()
    draw = ImageDraw.Draw(card)
    lines = wrap(caption, face, CAPTION_WIDTH)
    # Centre the caption's lines in the band above the screen.
    first_baseline = (SCREEN_TOP - len(lines) * CAPTION_LINE) / 2 + CAPTION_LINE * 0.75
    for i, line in enumerate(lines):
        draw.text((CARD_W / 2, first_baseline + i * CAPTION_LINE), line, font=face, fill=ink, anchor="ms")

    # Scale the whole screen to fit the space under the caption, keeping its proportions.
    screen_h = CARD_H - SCREEN_TOP - SCREEN_BOTTOM_MARGIN - 2 * BEZEL
    screen_w = round(shot.width * screen_h / shot.height)
    screen = shot.convert("RGB").resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    body_w, body_h = screen_w + 2 * BEZEL, screen_h + 2 * BEZEL
    left, top = (CARD_W - body_w) // 2, SCREEN_TOP

    shadow = Image.new("L", (CARD_W, CARD_H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle(
        [left, top + 16, left + body_w, top + body_h + 16], radius=CORNER + BEZEL, fill=90
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(24))
    card = Image.composite(Image.new("RGB", card.size, (0, 0, 0)), card, shadow)

    ImageDraw.Draw(card).rounded_rectangle(
        [left, top, left + body_w - 1, top + body_h - 1], radius=CORNER + BEZEL, fill=INK
    )
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, screen_w - 1, screen_h - 1], radius=CORNER, fill=255)
    card.paste(screen, (left + BEZEL, top + BEZEL), mask)
    return card


def read_captions(path: Path) -> dict[str, str]:
    captions = {}
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip() or raw.startswith("#"):
            continue
        name, sep, text = raw.partition("\t")
        if not sep or not text.strip():
            raise ValueError(f"{path}:{number}: expected 'file name<TAB>caption'")
        captions[name.strip()] = text.strip()
    return captions


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("inputs", nargs="+", type=Path, help="raw screenshots, portrait PNG or JPEG")
    parser.add_argument("--out", required=True, type=Path, help="directory for the framed PNGs")
    parser.add_argument("--caption", help="caption for every input without one in --captions")
    parser.add_argument("--captions", type=Path, help="TSV of file name, tab, caption")
    parser.add_argument("--ground", choices=["alternate", "terracotta", "cream"], default="alternate")
    args = parser.parse_args()

    captions = read_captions(args.captions) if args.captions else {}
    grounds = {"terracotta": [TERRACOTTA], "cream": [CREAM], "alternate": [TERRACOTTA, CREAM]}[args.ground]
    args.out.mkdir(parents=True, exist_ok=True)

    if not 2 <= len(args.inputs) <= 8:
        print(f"note: Play takes 2 to 8 phone screenshots; got {len(args.inputs)}", file=sys.stderr)

    for index, path in enumerate(args.inputs):
        caption = captions.get(path.name, args.caption)
        if not caption:
            parser.error(f"no caption for {path.name}; pass --caption or list it in --captions")
        with Image.open(path) as shot:
            if shot.width >= shot.height:
                parser.error(f"{path.name} is {shot.width}x{shot.height}; expected a portrait capture")
            if (shot.width, shot.height) != (1080, 2400):
                print(f"note: {path.name} is {shot.width}x{shot.height}, not 1080x2400; scaling it anyway",
                      file=sys.stderr)
            try:
                card = frame(shot, caption, grounds[index % len(grounds)])
            except ValueError as problem:
                parser.error(f"{path.name}: {problem}")
        target = args.out / (path.stem + ".png")
        card.save(target, optimize=True)
        print(f"{target}: {card.width}x{card.height}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
