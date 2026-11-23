#!/usr/bin/env python3
"""After DesignRefs has rendered: trims the whole-page renders, copies the brand sources and the
Tinted rules, writes covers/README.txt and INDEX.md. Usage: design_refs_post.py <out> <repo root>."""
import json
import re
import shutil
import sys
from pathlib import Path

from PIL import Image

out = Path(sys.argv[1])
root = Path(sys.argv[2])
STYLES = ["expressive", "comfort"]
THEMES = ["light", "dark"]

# Pages sized to the screen (Now Playing) come as frames at known scrolls; stitch each into one image,
# taking every later frame's rows between its pinned header and the gesture bar.
for spec in sorted(out.glob("*.frames.json")):
    name = spec.name[: -len(".frames.json")]
    meta = json.loads(spec.read_text())
    scrolls, top, bottom = meta["scrolls"], meta["top"], meta["bottom"]
    frames = [Image.open(out / f"{name}.frame{k}.png").convert("RGB") for k in range(len(scrolls))]
    w, h = frames[0].size
    page = Image.new("RGB", (w, scrolls[-1] + h))
    page.paste(frames[0].crop((0, 0, w, h - bottom)), (0, 0))
    covered = h - bottom
    for s, frame in zip(scrolls[1:], frames[1:]):
        start = max(covered, s + top)
        end = s + h - bottom
        if end > start:
            page.paste(frame.crop((0, start - s, w, end - s)), (0, start))
            covered = end
    page.paste(frames[-1].crop((0, h - bottom, w, h)), (0, covered))
    page = page.crop((0, 0, w, covered + bottom))
    page.save(out / f"{name}.png", optimize=True)
    for k in range(len(scrolls)):
        (out / f"{name}.frame{k}.png").unlink()
    spec.unlink()

# Whole-page renders are drawn on a 12000px phone. Keep the page down to its last row plus some
# ground, then the dock band from the bottom of the image.
DOCK_BAND = 600
for f in sorted(out.glob("*_full.png")):
    if f.name.endswith("now_playing_full.png"):
        continue
    im = Image.open(f).convert("RGB")
    w, h = im.size
    if h <= 2400:
        continue
    ground = im.getpixel((2, h - DOCK_BAND - 1))
    px = im.load()
    last = 0
    for y in range(h - DOCK_BAND - 1, 0, -1):
        row = [px[x, y] for x in range(0, w, 6)]
        if any(max(abs(a - b) for a, b in zip(p, ground)) > 6 for p in row):
            last = y
            break
    cut = min(h - DOCK_BAND, last + 160)
    trimmed = Image.new("RGB", (w, cut + DOCK_BAND))
    trimmed.paste(im.crop((0, 0, w, cut)), (0, 0))
    trimmed.paste(im.crop((0, h - DOCK_BAND, w, h)), (0, cut))
    trimmed.save(f, optimize=True)

# Brand sources.
brand = out / "brand"
brand.mkdir(exist_ok=True)
for src in [
    root / "docs/design/2026-11-21-ui-directions/logo/marks/sound-2-headroom-color.svg",
    root / "docs/design/2026-11-21-ui-directions/logo/marks/sound-2-headroom.svg",
    root / "app/src/main/res/drawable/ic_launcher_foreground.xml",
    root / "app/src/main/res/drawable/ic_launcher_monochrome.xml",
    root / "app/src/main/res/drawable/ic_mark.xml",
    root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
]:
    if src.exists():
        shutil.copy(src, brand / src.name)

colors = out / "colors"
if colors.exists():
    shutil.copy(root / "scripts/store_shots/comfort-tinted-rules.md", colors / "comfort-tinted-rules.md")

# covers/README.txt, from DemoLibrary.kt and the colour data.
lib = (root / "app/src/storeShots/kotlin/app/attic/player/storeshots/DemoLibrary.kt").read_text()
specs = re.findall(r'AlbumSpec\(\s*"([^"]+)",\s*"([^"]+)",\s*(\d+),\s*"[^"]+",\s*(\d+),', lib)
covers_kt = (root / "app/src/storeShots/kotlin/app/attic/player/storeshots/DemoCovers.kt").read_text()
drawings = re.search(r"when \(index\) \{(.*?)\n        \}", covers_kt, re.S).group(1)
drawing = {int(i): n for i, n in re.findall(r"(\d+) -> (\w+)\(", drawings)}
drawing.setdefault(11, re.search(r"else -> (\w+)\(", drawings).group(1))
covers = out / "covers"
if covers.exists():
    lines = [
        "Attic demo covers, drawn in code by app/src/storeShots/kotlin/app/attic/player/storeshots/DemoCovers.kt (no real artwork).",
        "Each file is 720x720 JPEG at quality 90, downscaled from the 1000px original; full/ holds the 1000px lossless PNGs.",
        "File names are <cover index>-<album slug>. The cover index is DemoCovers' index, not the album's place in the library.",
        "Seed swatch is the cover colour ui/Art.kt coverSwatch finds (Palette vibrant, else muted, else dominant).",
        "",
        f"{'file':34}{'album':22}{'artist':24}{'year':6}{'drawing':21}seed swatch",
    ]
    rows = []
    for artist, album, year, cover in specs:
        slug = re.sub(r"[^a-z0-9]+", "-", album.lower()).strip("-")
        seed = ""
        cj = colors / f"comfort-tinted-{cover}.json"
        if cj.exists():
            seed = json.loads(cj.read_text())["seed"]["hex"]
        rows.append((int(cover), f"{cover}-{slug}.jpg".ljust(34) + album.ljust(22) + artist.ljust(24) + year.ljust(6) + drawing.get(int(cover), "").ljust(21) + seed))
    lines += [r for _, r in sorted(rows)]
    lines += [
        "",
        'Now playing: every Now Playing, lyrics, queue and mini-player render shows "Lights Over the Water" (track 1 of 8)',
        'by Mira Okafor, from "Paper Lanterns" (2025): cover 0-paper-lanterns.jpg. It is shown playing at 1:18.5 of 3:52',
        "(DemoLibrary.POSITION_MS = 78500). The queue goes on with the rest of Paper Lanterns, then Glasshouse Summer 1-2 and Iravu Rail 1-2.",
        "",
        "Brand (../brand/):",
        "  mark-*.png               the Headroom mark (ui/expressive/Mark.kt AtticMarkIcon) at 512px: on cream #F5EDDD, on",
        "                           terracotta #C6533A, and on transparent in terracotta, cream and black. The store shots' BrandMarks",
        "                           test draws the same mark at 96dp.",
        "  sound-2-headroom*.svg    the vector sources, from docs/design/2026-11-21-ui-directions/logo/marks/",
        "  ic_launcher*.xml, ic_mark.xml   the Android drawables. The adaptive icon's background is headroom_ground #C6533A,",
        "                           the cabinet #F5EDDD and the cone #2A211C (app/src/main/res/values/colors.xml).",
    ]
    (covers / "README.txt").write_text("\n".join(lines) + "\n")

# INDEX.md
INDEX_NOTES = Path(root / "scripts/store_shots/design_refs_index.md").read_text()
pngs = sorted(p.name for p in out.glob("*.png"))
keys = []
for name in pngs:
    m = re.match(r"(expressive|comfort)-(light|dark)-(.+)\.png$", name)
    if m and m.group(3) not in keys:
        keys.append(m.group(3))
have = {(m.group(1), m.group(2), m.group(3)) for m in (re.match(r"(expressive|comfort)-(light|dark)-(.+)\.png$", n) for n in pngs) if m}
table = ["| key | expressive light | expressive dark | comfort light | comfort dark | size |", "|---|---|---|---|---|---|"]
for k in keys:
    cells = ["yes" if (s, t, k) in have else "**missing**" for s in STYLES for t in THEMES]
    size = ""
    for s in STYLES:
        f = out / f"{s}-light-{k}.png"
        if f.exists():
            w, h = Image.open(f).size
            size = f"{w}x{h}"
            break
    table.append(f"| `{k}` | " + " | ".join(cells) + f" | {size} |")
total = sum(p.stat().st_size for p in out.rglob("*") if p.is_file())
index = INDEX_NOTES.replace("{{TABLE}}", "\n".join(table)).replace("{{COUNT}}", str(len(pngs))).replace(
    "{{SIZE}}", f"{total / 1e6:.0f} MB"
)
(out / "INDEX.md").write_text(index)
print(f"{len(pngs)} renders, {len(keys)} keys, {total / 1e6:.0f} MB in {out}")
