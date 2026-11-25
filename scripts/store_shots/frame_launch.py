#!/usr/bin/env python3
"""Frames the launch images: the Product Hunt gallery, the LinkedIn carousel and the X and Mastodon images.

Reads the renders launch_shots.sh leaves under /tmp/attic-dev/launch and writes, under --out:

  product-hunt/  thumbnail.png (480 x 480) and eight 1270 x 760 gallery images
  linkedin/      seven 1080 x 1350 slides, carousel.pdf holding them, and tester-recruitment.png
  social/        three 1600 x 900 images for the X thread and the Mastodon post
  press/         the screens the press kit asks for, as rendered, with no frame

Phones, tablets, shadows and caption type come from frame.py and frame_tablets.py, so these sit beside
the Play screenshots as one family. The file names on the tag-cleanup card are set in the Mac's
monospace font, or DejaVu Sans Mono elsewhere.

    scripts/store_shots/launch_shots.sh --out ~/attic-launch-kit/assets
    python3 scripts/store_shots/frame_launch.py --out ~/attic-launch-kit/assets   # frames again, without rendering
"""

import argparse
import shutil
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from frame import CREAM, INK, INK_SOFT, ROOT, TERRACOTTA, colours, font, phone, place, wrap
from frame_tablets import tablet

sys.path.insert(0, str(ROOT / "scripts"))
from render_store_images import draw_mark, icon  # noqa: E402

REPO = "github.com/srivathsanvenkateswaran/attic"
BEZEL = (18, 18, 20)
MONO = ["/System/Library/Fonts/Menlo.ttc", "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"]

# The downloads on the tag-cleanup card: the file's name, then its artist tag. They are the four songs
# that show at the top of Kavya Raman's page in the demo library, named the ways TitleCleaner untangles:
# a video ID after the title, an uploader in the artist tag with the artist in the title, and a track number.
MESSY = [
    ("Iravu Rail [q7Lm2XvB9dE].mp3", "KAVYA RAMAN"),
    ("Kavya Raman - Saalai Ooram (Lyrics).mp3", "Thinnai Tunes"),
    ("kavya raman - Thinnai Pechu (Official Video).mp3", "Thinnai Tunes"),
    ("04 Neer Kudam.m4a", "Kavya Raman"),
]

# Product Hunt's gallery: what stands beside the caption, the ground, and the caption. A caption of two
# sentences is set as the family's title and body lines.
HUNT = [
    ("now-playing", "terracotta", "Your own music files.", "Two styles, no account."),
    ("mixes", "cream", "Daily mixes and mood mixes, worked out on your phone.", None),
    ("radio", "cream", "Radio from any song, in the language you're listening to.", None),
    ("transitions", "cream", "Smart transitions start where a song winds down.", None),
    ("equaliser", "cream", "An equaliser that remembers each song.", None),
    ("tags", "cream", "Messy downloads, tidied on screen.", "Your files stay untouched."),
    ("tablet", "cream", "Two panes on a tablet, in both styles.", None),
    ("open-source", "terracotta", "Open source.", REPO),
]

LINKEDIN = [
    ("attic", "terracotta", "Attic", "A music player for the songs already on your phone."),
    ("tags", "cream", "Your files stay untouched.", None),
    ("mixes", "cream", "Radio, mixes and moods, worked out on the phone.", None),
    ("equaliser", "cream", "An equaliser that remembers each song.", None),
    ("now-playing", "cream", "Two styles.", "Switch in Settings."),
    ("tablet", "cream", "Two panes on a tablet.", None),
    ("open-source", "terracotta", "Open source, GPLv3", "Feedback welcome"),
]


class Sources:
    """The three folders of renders, by the names their harnesses give them."""

    def __init__(self, renders):
        self.shots, self.refs, self.tablets = renders / "store-shots/raw", renders / "design-refs", renders / "tablet-renders"

    def shot(self, name):
        return Image.open(self.shots / f"{name}.png")

    def ref(self, name):
        return Image.open(self.refs / f"{name}.png")

    def tablet(self, name):
        return Image.open(self.tablets / f"tablet-ten-inch-{name}.png")


def mark_tile(size):
    """The colour mark as the app icon: on its terracotta ground with rounded corners. For cream grounds."""
    tile = icon(size)
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 0.22, fill=255)
    tile.putalpha(mask.resize((size, size), Image.LANCZOS))
    return tile


def mark_bare(size):
    """The colour mark with no ground of its own, framed as the icon frames it. For terracotta grounds."""
    big = size * 4
    image = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    scale = big / 76.0
    draw_mark(ImageDraw.Draw(image), (-16.0 * scale, -16.0 * scale), scale)
    return image.resize((size, size), Image.LANCZOS)


def brand_mark(ground, size):
    return mark_bare(round(size * 1.3)) if ground == "terracotta" else mark_tile(size)


def paste_mark(canvas, ground, size, x, y):
    """Puts the mark with its visible square at [x], [y]: the bare mark is drawn larger, as its cabinet is inset."""
    image = brand_mark(ground, size)
    inset = (image.width - size) // 2
    canvas.alpha_composite(image, (round(x) - inset, round(y) - inset))


def text_block(draw, parts, width):
    """Lays out (text, face, colour, gap after) parts as wrapped lines; returns the lines and the block's height."""
    lines, y = [], 0.0
    for text, face, colour, gap in parts:
        for line in wrap(draw, text, face, width):
            lines.append((line, face, colour, y))
            y += face.size * 1.2
        y += gap
    return lines, y - parts[-1][3]


def draw_block(draw, lines, x, y, width=None):
    """Draws the lines from text_block at [x], [y], centred in [width] when it is given."""
    for line, face, colour, dy in lines:
        lx = x + (width - draw.textlength(line, font=face)) / 2 if width else x
        draw.text((lx, y + dy), line, font=face, fill=colour)


def loupe(canvas, shot, centre, crop, at, diameter):
    """A round magnifier: [crop] pixels of [shot] around [centre], drawn [diameter] wide, centred on [at]."""
    cx, cy = centre
    piece = shot.convert("RGB").crop((cx - crop // 2, cy - crop // 2, cx + crop // 2, cy + crop // 2))
    ring = max(4, diameter // 36)
    big = diameter * 4
    disc = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    ImageDraw.Draw(disc).ellipse((0, 0, big - 1, big - 1), fill=BEZEL + (255,))
    inner = piece.resize((big - 8 * ring, big - 8 * ring), Image.LANCZOS)
    mask = Image.new("L", inner.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, inner.width - 1, inner.height - 1), fill=255)
    disc.paste(inner, (4 * ring, 4 * ring), mask)
    place(canvas, disc.resize((diameter, diameter), Image.LANCZOS), at[0] - diameter // 2, at[1] - diameter // 2)


def mono(size):
    for path in MONO:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    raise SystemExit("No monospace font found; add one to MONO.")


def wrap_name(draw, text, face, width):
    """Breaks a file name at its spaces, and inside a word only when the word alone overflows the line."""
    lines, line = [], ""
    for word in text.split(" "):
        trial = f"{line} {word}" if line else word
        if draw.textlength(trial, font=face) <= width:
            line = trial
            continue
        if line:
            lines.append(line)
        line = word
        while draw.textlength(line, font=face) > width:
            cut = next(i for i in range(len(line), 0, -1) if draw.textlength(line[:i], font=face) <= width)
            lines.append(line[:cut])
            line = line[cut:]
    return lines + [line]


def file_card(width, height, size):
    """A dark panel listing the MESSY downloads, file names in monospace at [size] px, on transparent."""
    s = 4
    card = Image.new("RGBA", (width * s, height * s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(card)
    radius = round(width * 0.07) * s
    draw.rounded_rectangle((0, 0, width * s - 1, height * s - 1), radius, fill=INK + (255,))
    pad = round(size * 1.5) * s
    name_face, label_face, head_face = mono(size * s), font(round(size * 0.86) * s, 400), font(round(size * 0.92) * s, 600)
    dim, faint = (0xC9, 0xB7, 0xA6), (0x5A, 0x48, 0x3E)
    inner = width * s - 2 * pad

    head_h = round(size * 3.2) * s
    draw.text((pad, pad * 0.9), "Download", font=head_face, fill=CREAM)
    count = f"{len(MESSY)} files"
    draw.text((width * s - pad - draw.textlength(count, font=label_face), pad * 0.92), count, font=label_face, fill=dim)
    draw.line((pad, head_h, width * s - pad, head_h), fill=faint, width=s)

    rows = [(wrap_name(draw, name, name_face, inner), artist) for name, artist in MESSY]
    line_h, label_h = name_face.size * 1.32, label_face.size * 1.5
    heights = [len(lines) * line_h + label_h for lines, _ in rows]
    gap = (height * s - head_h - pad * 0.7 - sum(heights)) / len(rows)
    y = head_h + gap / 2
    for (lines, artist), h in zip(rows, heights):
        for line in lines:
            draw.text((pad, y), line, font=name_face, fill=CREAM)
            y += line_h
        draw.text((pad, y + label_face.size * 0.2), f"Artist tag: {artist}", font=label_face, fill=dim)
        y += label_h + gap
        if (lines, artist) != rows[-1]:
            draw.line((pad, y - gap / 2, width * s - pad, y - gap / 2), fill=faint, width=s)
    return card.resize((width, height), Image.LANCZOS)


def arrow(canvas, x, y, radius):
    """A terracotta disc with an arrow pointing right, centred on [x], [y]: files in, tidy page out."""
    s = 4
    r = radius * s
    disc = Image.new("RGBA", (2 * r, 2 * r), (0, 0, 0, 0))
    draw = ImageDraw.Draw(disc)
    draw.ellipse((0, 0, 2 * r - 1, 2 * r - 1), fill=TERRACOTTA + (255,), outline=CREAM + (255,), width=round(r * 0.12))
    w = round(r * 0.16)
    draw.line((r * 0.55, r, r * 1.4, r), fill=CREAM, width=w)
    draw.line((r * 1.02, r * 0.62, r * 1.42, r, r * 1.02, r * 1.38), fill=CREAM, width=w, joint="curve")
    canvas.alpha_composite(disc.resize((2 * radius, 2 * radius), Image.LANCZOS), (x - radius, y - radius))


def new_canvas(size, ground):
    bg, ink, soft = colours(ground)
    canvas = Image.new("RGBA", size, bg + (255,))
    return canvas, ImageDraw.Draw(canvas), ink, soft


# Product Hunt

HUNT_SIZE = (1270, 760)
HUNT_PHONE = 650  # as frame.py's hunt_image: two phones 650 tall, 55 from the top, 56 from the right edge
HUNT_LEFT, HUNT_RIGHT, HUNT_GAP = 72, 56, 28


def hunt_caption(canvas, draw, ground, title, body, width):
    """The mark over the caption, left-aligned at the left margin and centred top to bottom."""
    _, ink, soft = colours(ground)
    parts = [(title, font(50, 700), ink, 22)]
    if body:
        parts.append((body, font(30, 400), soft, 0))
    lines, height = text_block(draw, parts, width)
    mark_px, mark_gap = 64, 34
    top = (HUNT_SIZE[1] - (mark_px + mark_gap + height)) // 2
    paste_mark(canvas, ground, mark_px, HUNT_LEFT, top)
    draw_block(draw, lines, HUNT_LEFT, top + mark_px + mark_gap)


# The details the loupes enlarge, in the 1080 x 2400 renders: which phone (0 left, 1 right), the centre and
# width of the piece shown, and how far the loupe sits from that centre in the framed image, so it covers
# less of what it is about.
START_RADIO = (0, (170, 1232), 300, (-52, 14))  # Comfort's track menu, the first of its four buttons
SMART_DOT = (1, (920, 1694), 250, (0, 46))  # the end of Expressive's seek bar; the dot is at 948, 1678
SAVE_FOR_SONG = (0, (345, 2072), 450, (0, 4))  # Comfort's equaliser sheet, the row under the bands
SONG_PILL = (1, (790, 185), 330, (0, 40))  # Comfort's Now Playing header


def hunt_pair(src, key):
    """The two phones for a gallery image, and the details its loupes show."""
    if key == "now-playing":
        return src.shot("expressive-dark-now-playing"), src.shot("comfort-dark-now-playing"), []
    if key == "mixes":
        return src.shot("expressive-light-home-mixes-moods"), src.shot("expressive-light-mood"), []
    if key == "radio":
        return src.ref("comfort-light-sheet_track_menu_now_playing"), src.ref("comfort-light-queue_radio"), [START_RADIO]
    if key == "transitions":
        return src.shot("expressive-light-settings-playback"), src.ref("expressive-light-now_playing_transition_mark"), [SMART_DOT]
    if key == "equaliser":
        return src.ref("comfort-light-sheet_equaliser"), src.ref("comfort-light-now_playing_equaliser"), [SAVE_FOR_SONG, SONG_PILL]
    raise KeyError(key)


def hunt_image(src, key, ground, title, body):
    canvas, draw, ink, soft = new_canvas(HUNT_SIZE, ground)
    w, h = HUNT_SIZE
    top = (h - HUNT_PHONE) // 2
    if key == "open-source":
        return closing(canvas, draw, ink, 190, title, 84, 40, "GPLv3", 30, (36, 12, 40))
    if key == "tablet":
        # One tablet in each style, the second lower and in front, as the Play set overlaps two phones.
        back = tablet(src.tablet("expressive-light-library_albums"), 392)
        front = tablet(src.tablet("comfort-light-library_albums"), 392)
        right = w - HUNT_RIGHT
        place(canvas, back, right - back.width, top)
        front_x = right - back.width - 86
        place(canvas, front, front_x, top + HUNT_PHONE - front.height)
        hunt_caption(canvas, draw, ground, title, body, front_x - 36 - HUNT_LEFT)
        return canvas.convert("RGB")
    if key == "tags":
        right = phone(src.shot("expressive-light-artist"), HUNT_PHONE)
        right_x = w - HUNT_RIGHT - right.width
        card = file_card(332, HUNT_PHONE, 15)
        card_x = right_x - HUNT_GAP - card.width
        place(canvas, card, card_x, top)
        place(canvas, right, right_x, top)
        arrow(canvas, right_x - HUNT_GAP // 2, h // 2, 24)
        hunt_caption(canvas, draw, ground, title, body, card_x - 40 - HUNT_LEFT)
        return canvas.convert("RGB")
    first, second, details = hunt_pair(src, key)
    left, right = phone(first, HUNT_PHONE), phone(second, HUNT_PHONE)
    right_x = w - HUNT_RIGHT - right.width
    left_x = right_x - HUNT_GAP - left.width
    place(canvas, left, left_x, top)
    place(canvas, right, right_x, top)
    bezel, diameter = 6, 176
    for which, (cx, cy), crop, (dx, dy) in details:
        shot, x = ((first, left_x), (second, right_x))[which]
        scale = (left.width - 2 * bezel) / shot.width
        # Beside the detail it enlarges, and never nearer the image's edge than 20 px.
        at_x = min(x + bezel + round(cx * scale) + dx, w - 20 - diameter // 2)
        loupe(canvas, shot, (cx, cy), crop, (at_x, top + bezel + round(cy * scale) + dy), diameter)
    hunt_caption(canvas, draw, ground, title, body, left_x - 44 - HUNT_LEFT)
    return canvas.convert("RGB")


def pill_height(size):
    return round(size * 1.94)


def pill(canvas, text, size, y, ink):
    """[text] at [size] px in an outlined pill, centred across the canvas with its top at [y]."""
    s = 4
    face = font(size * s, 600)
    tw = ImageDraw.Draw(canvas).textlength(text, font=face)
    w, h = round(tw + 1.5 * size * s), pill_height(size) * s
    image = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, w - 1, h - 1), h / 2, outline=ink + (255,), width=round(size * s / 13))
    draw.text((w / 2, h / 2), text, font=face, fill=ink + (255,), anchor="mm")
    small = image.resize((w // s, h // s), Image.LANCZOS)
    canvas.alpha_composite(small, ((canvas.width - small.width) // 2, round(y)))


def closing(canvas, draw, ink, mark_px, title, title_px, url_px, tag, tag_px, gaps):
    """The mark, a title, the repo's address and a pill, centred both ways. [gaps] are the three spaces between them."""
    w, h = canvas.size
    title_face, url_face = font(title_px, 700), font(url_px, 500)
    height = mark_px + gaps[0] + title_px * 1.2 + gaps[1] + url_px * 1.2 + gaps[2] + pill_height(tag_px)
    y = (h - height) / 2
    paste_mark(canvas, "terracotta", mark_px, (w - mark_px) // 2, y)
    y += mark_px + gaps[0]
    draw.text(((w - draw.textlength(title, font=title_face)) / 2, y), title, font=title_face, fill=ink)
    y += title_px * 1.2 + gaps[1]
    draw.text(((w - draw.textlength(REPO, font=url_face)) / 2, y), REPO, font=url_face, fill=ink)
    pill(canvas, tag, tag_px, y + url_px * 1.2 + gaps[2], ink)
    return canvas.convert("RGB")


def thumbnail(size=480):
    """sound-2-headroom-color.svg, full bleed: the whole 108-unit canvas, as the Product Hunt spec asks."""
    big = size * 4
    image = Image.new("RGB", (big, big), TERRACOTTA)
    draw_mark(ImageDraw.Draw(image), (0, 0), big / 108.0)
    return image.resize((size, size), Image.LANCZOS)


# LinkedIn

SLIDE = (1080, 1350)
SLIDE_TOP, SLIDE_BOTTOM, SLIDE_SIDE = 286, 56, 64


def slide_caption(canvas, draw, ground, title, body):
    """The mark beside the caption, centred as one group in the band above the screens, as on the tablet set."""
    _, ink, soft = colours(ground)
    mark_px, gap = 104, 34
    parts = [(title, font(60, 700), ink, 14)]
    if body:
        parts.append((body, font(38, 400), soft, 0))
    lines, height = text_block(draw, parts, SLIDE[0] - 2 * SLIDE_SIDE - mark_px - gap)
    text_w = round(max(draw.textlength(line, font=face) for line, face, _, _ in lines))
    left = (SLIDE[0] - (mark_px + gap + text_w)) // 2
    band = SLIDE_TOP - 16
    paste_mark(canvas, ground, mark_px, left, (band - mark_px) // 2 + 8)
    draw_block(draw, lines, left + mark_px + gap, (band - height) / 2 + 2)


def slide_image(src, key, ground, title, body):
    canvas, draw, ink, soft = new_canvas(SLIDE, ground)
    w, h = SLIDE
    if key == "attic":
        mark_px, name_face, line_face = 330, font(190, 700), font(52, 400)
        top = 262
        paste_mark(canvas, ground, mark_px, (w - mark_px) // 2, top)
        y = top + mark_px + 56
        draw.text(((w - draw.textlength(title, font=name_face)) / 2, y), title, font=name_face, fill=ink)
        lines, _ = text_block(draw, [(body, line_face, ink, 0)], 760)
        draw_block(draw, lines, (w - 760) // 2, y + name_face.size * 1.2 + 44, 760)
        return canvas.convert("RGB")
    if key == "open-source":
        return closing(canvas, draw, ink, 250, title, 92, 42, body, 40, (60, 24, 72))
    slide_caption(canvas, draw, ground, title, body)
    height = h - SLIDE_TOP - SLIDE_BOTTOM
    if key == "tablet":
        # The same screen light and dark, the dark one lower and in front, to fill the upright slide.
        back = tablet(src.tablet("comfort-light-library_albums"), 520)
        front = tablet(src.tablet("comfort-dark-library_albums"), 520)
        place(canvas, back, SLIDE_SIDE - 8, SLIDE_TOP + 8)
        place(canvas, front, w - SLIDE_SIDE + 8 - front.width, h - SLIDE_BOTTOM - 8 - front.height)
        return canvas.convert("RGB")
    if key == "tags":
        device = phone(src.shot("expressive-light-artist"), height)
        gap = 40
        card = file_card(w - 2 * 44 - gap - device.width, height, 21)
        place(canvas, card, 44, SLIDE_TOP)
        place(canvas, device, 44 + card.width + gap, SLIDE_TOP)
        arrow(canvas, 44 + card.width + gap // 2, SLIDE_TOP + height // 2, 34)
        return canvas.convert("RGB")
    if key == "now-playing":
        left = phone(src.shot("expressive-light-now-playing"), height)
        right = phone(src.shot("comfort-light-now-playing"), height)
        gap = 32
        x = (w - left.width - gap - right.width) // 2
        place(canvas, left, x, SLIDE_TOP)
        place(canvas, right, x + left.width + gap, SLIDE_TOP)
        return canvas.convert("RGB")
    shot = {"mixes": src.shot("expressive-light-home-mixes-moods"), "equaliser": src.ref("comfort-light-sheet_equaliser")}[key]
    device = phone(shot, height)
    place(canvas, device, (w - device.width) // 2, SLIDE_TOP)
    return canvas.convert("RGB")


def tester_image(src):
    """For the tester post: Home in Expressive beside the mark."""
    canvas, draw, ink, soft = new_canvas(SLIDE, "terracotta")
    w, h = SLIDE
    device = phone(src.shot("expressive-dark-home"), 1150)
    x = w - 72 - device.width
    place(canvas, device, x, (h - device.height) // 2)
    mark_px = 250
    paste_mark(canvas, "terracotta", mark_px, (x - mark_px) // 2, (h - mark_px) // 2)
    return canvas.convert("RGB")


# X and Mastodon

WIDE = (1600, 900)
WIDE_PHONE = 780


def wide_image(src, key, ground):
    """No caption, as the posts carry the words: the mark and the name, then the screens."""
    canvas, draw, ink, soft = new_canvas(WIDE, ground)
    w, h = WIDE
    top = (h - WIDE_PHONE) // 2
    if key == "now-playing":
        screens = [phone(src.shot("expressive-dark-now-playing"), WIDE_PHONE), phone(src.shot("comfort-dark-now-playing"), WIDE_PHONE)]
    elif key == "home":
        screens = [phone(src.shot("expressive-light-home-mixes-moods"), WIDE_PHONE)]
    else:
        screens = [file_card(520, WIDE_PHONE, 20), phone(src.shot("expressive-light-artist"), WIDE_PHONE)]
    gap = 44
    mark_px, name_face = 150, font(112, 700)
    name_w = round(draw.textlength("Attic", font=name_face))
    brand_w = max(mark_px, name_w)
    brand_gap = 120 if len(screens) == 1 else 96
    group = brand_w + brand_gap + sum(s.width for s in screens) + gap * (len(screens) - 1)
    x = (w - group) // 2
    brand_h = mark_px + 30 + round(name_face.size * 1.1)
    by = (h - brand_h) // 2
    paste_mark(canvas, ground, mark_px, x + (brand_w - mark_px) // 2, by)
    draw.text((x + (brand_w - name_w) / 2, by + mark_px + 30), "Attic", font=name_face, fill=ink)
    x += brand_w + brand_gap
    spots = []
    for screen in screens:
        spots.append(x)
        place(canvas, screen, x, top)
        x += screen.width + gap
    if key == "tags":
        arrow(canvas, spots[1] - gap // 2, h // 2, 32)
    return canvas.convert("RGB")


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--renders", type=Path, default=Path("/tmp/attic-dev/launch"), help="where launch_shots.sh left the renders")
    parser.add_argument("--out", type=Path, required=True, help="the folder to write product-hunt/, linkedin/, social/ and press/ into")
    args = parser.parse_args()
    src, out = Sources(args.renders), args.out.expanduser()

    written = [save(thumbnail(), out / "product-hunt/thumbnail.png")]
    for i, (key, ground, title, body) in enumerate(HUNT, 1):
        written.append(save(hunt_image(src, key, ground, title, body), out / f"product-hunt/gallery-{i:02d}-{key}.png"))

    slides = []
    for i, (key, ground, title, body) in enumerate(LINKEDIN, 1):
        slides.append(slide_image(src, key, ground, title, body))
        written.append(save(slides[-1], out / f"linkedin/slide-{i:02d}-{key}.png"))
    pdf = out / "linkedin/carousel.pdf"
    # 1080 x 1350 px at 144 dpi is a 7.5 x 9.375 inch page. Pillow stores the pages as JPEG, so at a high quality.
    slides[0].save(pdf, save_all=True, append_images=slides[1:], resolution=144.0, quality=95, subsampling=0)
    written.append(pdf)
    written.append(save(tester_image(src), out / "linkedin/tester-recruitment.png"))

    for name, key, ground in [("x-1-now-playing", "now-playing", "terracotta"), ("x-2-home-mixes-moods", "home", "cream"), ("x-4-tag-cleanup", "tags", "cream")]:
        written.append(save(wide_image(src, key, ground), out / f"social/{name}.png"))

    # The press kit's screens, unframed: the equaliser sheet, Transitions, and the tablet in each style.
    press = [
        (src.refs / "expressive-light-sheet_equaliser.png", "phone-expressive-equaliser-sheet.png"),
        (src.refs / "comfort-light-sheet_equaliser.png", "phone-comfort-equaliser-sheet.png"),
        (src.shots / "expressive-light-settings-playback.png", "phone-expressive-settings-transitions.png"),
        (src.shots / "comfort-light-settings-playback.png", "phone-comfort-settings-transitions.png"),
    ] + [
        (src.tablets / f"tablet-ten-inch-{style}-light-{screen}.png", f"tablet-{style}-{name}.png")
        for style in ("expressive", "comfort")
        for screen, name in (("library_albums", "library-album"), ("now_playing", "now-playing-queue"))
    ]
    for source, name in press:
        (out / "press").mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, out / "press" / name)
        written.append(out / "press" / name)

    for path in written:
        size = "x".join(map(str, Image.open(path).size)) if path.suffix == ".png" else f"{len(slides)} pages"
        print(f"{path.relative_to(out)}  {size}  {path.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
